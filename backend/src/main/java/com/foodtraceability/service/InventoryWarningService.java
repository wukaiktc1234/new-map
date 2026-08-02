package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryWarning;

import java.util.List;

/**
 * 库存预警服务接口
 * 定义库存预警相关的业务方法
 */
public interface InventoryWarningService {
    
    /**
     * 创建库存预警
     */
    InventoryWarning createWarning(InventoryWarning warning);
    
    /**
     * 更新库存预警
     */
    InventoryWarning updateWarning(Long id, InventoryWarning warning);
    
    /**
     * 根据ID获取库存预警
     */
    InventoryWarning getWarningById(Long id);
    
    /**
     * 根据ID删除库存预警
     */
    void deleteWarning(Long id);
    
    /**
     * 分页查询库存预警列表
     */
    IPage<InventoryWarning> getWarningPage(Page<InventoryWarning> page, Long warehouseId, Integer warningLevel, Integer status);
    
    /**
     * 处理库存预警
     */
    InventoryWarning handleWarning(Long id, Integer status, String handler, String handleRemark);
    
    /**
     * 根据产品ID和仓库ID获取库存预警
     */
    InventoryWarning getWarningByProductAndWarehouse(Long productId, Long warehouseId);
    
    /**
     * 批量生成库存预警
     */
    void batchGenerateWarnings(List<InventoryWarning> warnings);
    
    /**
     * 自动生成库存预警
     */
    void autoGenerateWarnings();
    
    /**
     * 生成采购建议
     */
    Object generatePurchaseSuggestions();
    
    /**
     * 创建采购单
     */
    Object createPurchaseOrder(Object purchaseData);
}