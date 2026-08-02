package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryWarning;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryWarningMapper;
import com.foodtraceability.service.InventoryWarningService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 库存预警服务实现类
 * 实现库存预警相关的业务方法
 */
@Service
public class InventoryWarningServiceImpl extends ServiceImpl<InventoryWarningMapper, InventoryWarning> implements InventoryWarningService {
    

    public InventoryWarningServiceImpl(InventoryWarningMapper inventoryWarningMapper, InventoryMapper inventoryMapper) {
        this.inventoryWarningMapper = inventoryWarningMapper;
        this.inventoryMapper = inventoryMapper;
    }

    private final InventoryWarningMapper inventoryWarningMapper;
    
    private final InventoryMapper inventoryMapper;
    
    @Override
    public InventoryWarning createWarning(InventoryWarning warning) {
        this.save(warning);
        return warning;
    }
    
    @Override
    public InventoryWarning updateWarning(Long id, InventoryWarning warning) {
        warning.setId(id);
        this.updateById(warning);
        return this.getById(id);
    }
    
    @Override
    public InventoryWarning getWarningById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public void deleteWarning(Long id) {
        this.removeById(id);
    }
    
    @Override
    public IPage<InventoryWarning> getWarningPage(Page<InventoryWarning> page, Long warehouseId, Integer warningLevel, Integer status) {
        return inventoryWarningMapper.selectWarningPage(page, warehouseId, warningLevel, status);
    }
    
    @Override
    public InventoryWarning handleWarning(Long id, Integer status, String handler, String handleRemark) {
        InventoryWarning warning = this.getById(id);
        if (warning != null) {
            warning.setStatus(status);
            warning.setHandler(handler);
            warning.setHandleTime(LocalDateTime.now());
            warning.setHandleRemark(handleRemark);
            this.updateById(warning);
        }
        return warning;
    }
    
    @Override
    public InventoryWarning getWarningByProductAndWarehouse(Long productId, Long warehouseId) {
        return inventoryWarningMapper.selectByProductAndWarehouse(productId, warehouseId);
    }
    
    @Override
    public void batchGenerateWarnings(List<InventoryWarning> warnings) {
        this.saveBatch(warnings);
    }
    
    @Override
    public void autoGenerateWarnings() {
        // 查询所有库存记录
        List<Inventory> inventories = inventoryMapper.selectList(null);
        
        // 遍历库存记录，生成库存预警
        for (Inventory inventory : inventories) {
            // 检查库存是否低于安全库存（BigDecimal比较必须使用compareTo）
            if (inventory.getCurrentStock().compareTo(inventory.getMinSafeQty()) < 0) {
                // 检查是否已经存在预警
                InventoryWarning existingWarning = inventoryWarningMapper.selectByProductAndWarehouse(
                        inventory.getProductId(), inventory.getWarehouseId());
                
                if (existingWarning == null) {
                    // 创建新的库存预警
                    InventoryWarning warning = new InventoryWarning();
                    warning.setInventoryId(inventory.getId());
                    warning.setProductId(inventory.getProductId());
                    warning.setProductName(inventory.getProductName());
                    warning.setWarehouseId(inventory.getWarehouseId());
                    warning.setCurrentStock(inventory.getCurrentStock().intValue()); // BigDecimal→Integer转换
                    warning.setSafeStock(inventory.getMinSafeQty().intValue()); // BigDecimal→Integer转换
                    
                    // 计算预警级别（BigDecimal必须转换为double进行除法运算）
                    double ratio = inventory.getCurrentStock().doubleValue() / inventory.getMinSafeQty().doubleValue();
                    if (ratio < 0.3) {
                        warning.setWarningLevel(3); // 高
                    } else if (ratio < 0.5) {
                        warning.setWarningLevel(2); // 中
                    } else {
                        warning.setWarningLevel(1); // 低
                    }
                    
                    warning.setWarningType(1); // 低于安全库存
                    warning.setStatus(1); // 未处理
                    warning.setCreatedAt(new Date());
                    warning.setUpdatedAt(new Date());
                    
                    this.save(warning);
                } else {
                    // 更新现有预警
                    existingWarning.setCurrentStock(inventory.getCurrentStock().intValue()); // BigDecimal→Integer转换
                    existingWarning.setSafeStock(inventory.getMinSafeQty().intValue()); // BigDecimal→Integer转换
                    
                    // 重新计算预警级别（BigDecimal必须转换为double进行除法运算）
                    double ratio = inventory.getCurrentStock().doubleValue() / inventory.getMinSafeQty().doubleValue();
                    if (ratio < 0.3) {
                        existingWarning.setWarningLevel(3); // 高
                    } else if (ratio < 0.5) {
                        existingWarning.setWarningLevel(2); // 中
                    } else {
                        existingWarning.setWarningLevel(1); // 低
                    }
                    
                    existingWarning.setUpdatedAt(new Date());
                    this.updateById(existingWarning);
                }
            }
        }
    }
    
    @Override
    public Object generatePurchaseSuggestions() {
        List<InventoryWarning> warnings = inventoryWarningMapper.selectList(null);
        
        List<InventoryWarning> unhandledWarnings = warnings.stream()
                .filter(warning -> warning.getStatus() != null && warning.getStatus() == 1)
                .toList();
        
        if (unhandledWarnings.isEmpty()) {
            // 使用实际数据库列名 current_stock（Java字段名为quantity，通过@TableField映射）
            // 注意：.apply() 是原始SQL，不会经过MyBatis-Plus的字段名转换，必须使用数据库列名
            List<Inventory> lowStockInventories = inventoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                    .isNotNull(Inventory::getQuantity)
                    .isNotNull(Inventory::getMinSafeQty)
                    .apply("current_stock < min_safe_qty")
            );

            return lowStockInventories.stream().map(inv -> {
                java.util.Map<String, Object> suggestion = new java.util.HashMap<>();
                suggestion.put("id", inv.getId());
                suggestion.put("productId", inv.getProductId());
                suggestion.put("productName", inv.getProductName());
                suggestion.put("currentStock", inv.getCurrentStock());
                suggestion.put("safeStock", inv.getMinSafeQty());
                int currentStock = inv.getCurrentStock() != null ? inv.getCurrentStock().intValue() : 0; // BigDecimal→int转换
                int safetyStock = inv.getMinSafeQty() != null ? inv.getMinSafeQty().intValue() : 0; // BigDecimal→int转换
                suggestion.put("suggestedPurchase", safetyStock * 2 - currentStock);
                suggestion.put("warehouseId", inv.getWarehouseId());
                suggestion.put("warningLevel", currentStock < safetyStock * 0.3 ? 3 :
                    (currentStock < safetyStock * 0.5 ? 2 : 1));
                return suggestion;
            }).toList();
        }
        
        return unhandledWarnings.stream().map(warning -> {
            java.util.Map<String, Object> suggestion = new java.util.HashMap<>();
            suggestion.put("id", warning.getId());
            suggestion.put("productId", warning.getProductId());
            suggestion.put("productName", warning.getProductName());
            suggestion.put("currentStock", warning.getCurrentStock());
            suggestion.put("safeStock", warning.getSafeStock());
            suggestion.put("suggestedPurchase", warning.getSafeStock() * 2 - warning.getCurrentStock());
            suggestion.put("warehouseId", warning.getWarehouseId());
            suggestion.put("warningLevel", warning.getWarningLevel());
            return suggestion;
        }).toList();
    }
    
    @Override
    public Object createPurchaseOrder(Object purchaseData) {
        // 实现创建采购单的逻辑
        // 这里只是简单返回数据，实际项目中需要调用采购服务
        return purchaseData;
    }
}