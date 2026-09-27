package com.foodtraceability.service;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.LossOutboundCreateDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.LossOutbound;
import com.foodtraceability.entity.Product;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.mapper.LossOutboundMapper;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class LossOutboundService {

    private static final Logger log = LoggerFactory.getLogger(LossOutboundService.class);


    public LossOutboundService(LossOutboundMapper lossOutboundMapper, ProductMapper productMapper, WarehouseMapper warehouseMapper, InventoryMapper inventoryMapper, InventoryService inventoryService, LocationService locationService) {
        this.lossOutboundMapper = lossOutboundMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryService = inventoryService;
        this.locationService = locationService;
    }

    private final LossOutboundMapper lossOutboundMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;
    private final LocationService locationService;

    public IPage<LossOutbound> getPage(int page, int pageSize, String lossNo, String status, String startDate, String endDate) {
        Page<LossOutbound> pageParam = new Page<>(page, pageSize);
        return lossOutboundMapper.selectPageByCondition(pageParam, lossNo, status, startDate, endDate);
    }

    public LossOutbound getById(Long id) {
        return lossOutboundMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public LossOutbound create(LossOutboundCreateDTO dto, Long operatorId, String operatorName) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }

        LossOutbound lossOutbound = new LossOutbound();
        lossOutbound.setLossNo(generateLossNo());
        lossOutbound.setProductId(dto.getProductId());
        lossOutbound.setProductName(product.getName());
        lossOutbound.setLossQuantity(dto.getLossQuantity());
        lossOutbound.setUnit(product.getUnit());
        lossOutbound.setLossReason(dto.getLossReason());
        lossOutbound.setWarehouseId(dto.getWarehouseId());
        lossOutbound.setWarehouseName(warehouse.getName());
        lossOutbound.setLossDate(dto.getLossDate());
        lossOutbound.setLossAmount(BigDecimal.valueOf(dto.getLossQuantity()).multiply(product.getCostPrice()));
        lossOutbound.setStatus("pending");
        lossOutbound.setOperatorId(operatorId);
        lossOutbound.setOperatorName(operatorName);
        lossOutbound.setRemark(dto.getRemark());
        lossOutbound.setCreatedAt(LocalDateTime.now());
        lossOutbound.setUpdatedAt(LocalDateTime.now());
        lossOutbound.setDeleted(0);

        lossOutboundMapper.insert(lossOutbound);

        log.info("创建报损单成功: lossNo={}, productId={}, quantity={}, operator={}",
                lossOutbound.getLossNo(), dto.getProductId(), dto.getLossQuantity(), operatorName);

        return lossOutbound;
    }

    @Transactional(rollbackFor = Exception.class)
    public LossOutbound approve(Long id, Long approverId, String approverName) {
        LossOutbound lossOutbound = lossOutboundMapper.selectById(id);
        if (lossOutbound == null) {
            throw new RuntimeException("报损单不存在");
        }

        if (!"pending".equals(lossOutbound.getStatus())) {
            throw new RuntimeException("报损单状态不正确，无法审核");
        }

        updateInventory(lossOutbound.getProductId(), lossOutbound.getWarehouseId(), -lossOutbound.getLossQuantity(), lossOutbound.getLossNo());

        lossOutbound.setStatus("approved");
        lossOutbound.setApproverId(approverId);
        lossOutbound.setApproverName(approverName);
        lossOutbound.setApproveTime(LocalDateTime.now());
        lossOutbound.setUpdatedAt(LocalDateTime.now());

        lossOutboundMapper.updateById(lossOutbound);

        log.info("审核报损单成功: lossNo={}, approver={}", lossOutbound.getLossNo(), approverName);

        return lossOutbound;
    }

    /**
     * M3-M4 S4c-2 收编（对照表 #2）：报损出库经统一库存服务，
     * 污染列 product_id 查询与直写旁路移除，自然获得乐观锁重试与流水。
     */
    private void updateInventory(Long productId, Long warehouseId, Integer quantity, String lossNo) {
        com.foodtraceability.entity.Location location = locationService.resolveByWarehouseId(warehouseId);
        if (location == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "仓库未映射到位置，无法执行报损库存变动：warehouseId=" + warehouseId);
        }
        Long locationId = location.getLocationId();
        String sourceRef = "报损出库 - " + lossNo;
        if (quantity < 0) {
            inventoryService.decreaseStockAtLocation(locationId, productId,
                    BigDecimal.valueOf(-quantity), "LOSS", sourceRef);
        } else {
            inventoryService.increaseStockAtLocation(locationId, productId,
                    null, BigDecimal.valueOf(quantity), null, null, "LOSS", sourceRef);
        }
    }

    private String generateLossNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "BS" + dateStr + randomStr;
    }
}
