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
    

    public OtherInboundService(OtherInboundMapper otherInboundMapper, InventoryMapper inventoryMapper, ProductMapper productMapper, WarehouseMapper warehouseMapper) {
        this.otherInboundMapper = otherInboundMapper;
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
    }

    private final OtherInboundMapper otherInboundMapper;
    
    private final InventoryMapper inventoryMapper;
    
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
    
    private void updateInventory(Long productId, Long warehouseId, Integer quantity, String productName, String warehouseName, String unit) {
        /* 修复：使用getMaterialId替代getProductId */
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getMaterialId, productId)
               .eq(Inventory::getWarehouseId, warehouseId)
               .eq(Inventory::getDeleted, 0);

        Inventory inventory = inventoryMapper.selectOne(wrapper);

        if (inventory == null) {
            inventory = new Inventory();
            /* 修复：使用setMaterialId替代setProductId */
            inventory.setMaterialId(productId);
            /* 修复：使用setMaterialName替代setProductName */
            inventory.setMaterialName(productName);
            inventory.setWarehouseId(warehouseId);
            /* 注意：Inventory实体没有warehouseName字段，此行可能需要移除或调整 */
            // inventory.setWarehouseName(warehouseName);
            /* 修复：使用setQuantity（BigDecimal）替代setCurrentStock（int） */
            inventory.setQuantity(BigDecimal.valueOf(quantity));
            /* 注意：Inventory实体没有safetyStock字段，此行可能需要移除或调整为minSafeQty */
            // inventory.setSafetyStock(0);
            inventory.setUnit(unit);
            inventory.setDeleted(0);
            inventoryMapper.insert(inventory);
            log.info("创建新库存记录，物料ID：{}，仓库ID：{}，数量：{}", productId, warehouseId, quantity);
        } else {
            /* 修复：使用quantity字段进行计算 */
            int currentStock = inventory.getQuantity() != null ? inventory.getQuantity().intValue() : 0;
            inventory.setQuantity(BigDecimal.valueOf(currentStock + quantity));
            inventoryMapper.updateById(inventory);
            log.info("更新库存，物料ID：{}，仓库ID：{}，增加数量：{}，当前库存：{}",
                    productId, warehouseId, quantity, inventory.getQuantity());
        }
    }
    
    private String generateInboundNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "QT" + timestamp + random;
    }
}
