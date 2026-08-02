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


    public LossOutboundService(LossOutboundMapper lossOutboundMapper, ProductMapper productMapper, WarehouseMapper warehouseMapper, InventoryMapper inventoryMapper) {
        this.lossOutboundMapper = lossOutboundMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryMapper = inventoryMapper;
    }

    private final LossOutboundMapper lossOutboundMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final InventoryMapper inventoryMapper;

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

        updateInventory(lossOutbound.getProductId(), lossOutbound.getWarehouseId(), -lossOutbound.getLossQuantity());

        lossOutbound.setStatus("approved");
        lossOutbound.setApproverId(approverId);
        lossOutbound.setApproverName(approverName);
        lossOutbound.setApproveTime(LocalDateTime.now());
        lossOutbound.setUpdatedAt(LocalDateTime.now());

        lossOutboundMapper.updateById(lossOutbound);

        log.info("审核报损单成功: lossNo={}, approver={}", lossOutbound.getLossNo(), approverName);

        return lossOutbound;
    }

    private void updateInventory(Long productId, Long warehouseId, Integer quantity) {
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getProductId, productId)
                .eq(Inventory::getWarehouseId, warehouseId);

        Inventory inventory = inventoryMapper.selectOne(queryWrapper);

        if (inventory == null) {
            if (quantity < 0) {
                throw new RuntimeException("库存不足，无法扣减");
            }
            inventory = new Inventory();
            inventory.setProductId(productId);
            inventory.setWarehouseId(warehouseId);
            inventory.setCurrentStock(BigDecimal.valueOf(quantity));
            inventory.setSafetyStock(BigDecimal.ZERO);
            inventory.setCreateTime(LocalDateTime.now());
            inventory.setUpdateTime(LocalDateTime.now());
            inventory.setDeleted(0);
            inventoryMapper.insert(inventory);
            log.info("创建库存记录: productId={}, warehouseId={}, stock={}", productId, warehouseId, quantity);
        } else {
            BigDecimal oldStock = inventory.getCurrentStock();
            BigDecimal newStock = oldStock.add(BigDecimal.valueOf(quantity));
            if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("库存不足，当前库存: " + oldStock);
            }
            inventory.setCurrentStock(newStock);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryMapper.updateById(inventory);
            log.info("更新库存: productId={}, warehouseId={}, oldStock={}, change={}, newStock={}",
                    productId, warehouseId, oldStock, quantity, newStock);
        }
    }

    private String generateLossNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "BS" + dateStr + randomStr;
    }
}
