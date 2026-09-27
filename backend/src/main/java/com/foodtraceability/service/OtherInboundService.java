package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.OtherInboundCreateDTO;
import com.foodtraceability.entity.OtherInbound;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.Product;
import com.foodtraceability.entity.Warehouse;
import com.foodtraceability.mapper.OtherInboundMapper;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.mapper.WarehouseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Service
public class OtherInboundService {
    
    private static final Logger log = LoggerFactory.getLogger(OtherInboundService.class);
    

    public OtherInboundService(OtherInboundMapper otherInboundMapper, InventoryMapper inventoryMapper, ProductMapper productMapper, WarehouseMapper warehouseMapper, InventoryService inventoryService, LocationService locationService) {
        this.otherInboundMapper = otherInboundMapper;
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.inventoryService = inventoryService;
        this.locationService = locationService;
    }

    private final OtherInboundMapper otherInboundMapper;
    
    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;
    private final LocationService locationService;
    
    private final ProductMapper productMapper;
    
    private final WarehouseMapper warehouseMapper;
    
    public IPage<OtherInbound> getPage(int page, int pageSize, String inboundType, String startDate, String endDate) {
        Page<OtherInbound> pageParam = new Page<>(page, pageSize);
        return otherInboundMapper.selectPageByCondition(pageParam, inboundType, startDate, endDate);
    }
    
    public OtherInbound getById(Long id) {
        return otherInboundMapper.selectById(id);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public OtherInbound create(OtherInboundCreateDTO dto, Long operatorId, String operatorName) {
        log.info("创建其他入库单，操作人：{}，商品ID：{}，数量：{}", operatorName, dto.getProductId(), dto.getQuantity());
        
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("产品不存在，ID: " + dto.getProductId());
        }
        
        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在，ID: " + dto.getWarehouseId());
        }
        
        OtherInbound inbound = new OtherInbound();
        inbound.setInboundNo(generateInboundNo());
        inbound.setInboundType(dto.getInboundType());
        inbound.setProductId(dto.getProductId());
        inbound.setProductName(product.getName());
        inbound.setQuantity(dto.getQuantity());
        inbound.setUnit(product.getUnit());
        inbound.setWarehouseId(dto.getWarehouseId());
        /* 修复：使用getWarehouseName()替代getName() */
        inbound.setWarehouseName(warehouse.getWarehouseName());
        inbound.setSourceId(dto.getSourceId());
        inbound.setReturnSource(dto.getReturnSource());
        inbound.setRemark(dto.getRemark());
        inbound.setInboundTime(LocalDateTime.now());
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setStatus(1);
        inbound.setDeleted(0);
        
        otherInboundMapper.insert(inbound);
        
        updateInventory(dto.getProductId(), dto.getWarehouseId(), dto.getQuantity(), product.getName(), warehouse.getWarehouseName(), product.getUnit());
        
        log.info("其他入库单创建成功，入库单号：{}", inbound.getInboundNo());
        
        return inbound;
    }
    
    /**
     * M3-M4 S4c-2 收编（对照表 #3）：其他入库经统一库存服务；
     * 直写旁路与 intValue() 精度截断移除，自然获得乐观锁重试与流水。
     */
    private void updateInventory(Long productId, Long warehouseId, Integer quantity, String productName, String warehouseName, String unit) {
        com.foodtraceability.entity.Location location = locationService.resolveByWarehouseId(warehouseId);
        if (location == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "仓库未映射到位置，无法执行其他入库：warehouseId=" + warehouseId);
        }
        inventoryService.increaseStockAtLocation(location.getLocationId(), productId,
                productName, BigDecimal.valueOf(quantity), unit, null, "OTHER",
                "其他入库 - " + warehouseName);
    }
    
    private String generateInboundNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "QT" + timestamp + random;
    }
}
