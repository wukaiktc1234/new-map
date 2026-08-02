package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLoss;

/**
 * 报损单服务接口
 * 定义报损管理相关的业务方法
 */
public interface InventoryLossService {
    
    /**
     * 创建报损单
     */
    InventoryLoss createInventoryLoss(InventoryLoss inventoryLoss);
    
    /**
     * 更新报损单
     */
    InventoryLoss updateInventoryLoss(Long id, InventoryLoss inventoryLoss);
    
    /**
     * 根据ID获取报损单
     */
    InventoryLoss getInventoryLossById(Long id);
    
    /**
     * 根据ID删除报损单
     */
    void deleteInventoryLoss(Long id);
    
    /**
     * 分页查询报损单列表
     */
    IPage<InventoryLoss> getInventoryLossPage(Page<InventoryLoss> page, String lossNo, Long warehouseId, String status, String applyTimeStart, String applyTimeEnd);
    
    /**
     * 审批报损单
     */
    InventoryLoss approveInventoryLoss(Long id, String status, String remark);
    
    /**
     * 处理报损
     */
    InventoryLoss processInventoryLoss(Long id);
}