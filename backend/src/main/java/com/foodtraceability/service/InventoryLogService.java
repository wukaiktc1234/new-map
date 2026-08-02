package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLog;
import java.util.Map;

/**
 * 库存日志服务接口
 * 定义库存日志管理相关的业务方法
 */
public interface InventoryLogService {
    
    /**
     * 创建库存日志
     */
    InventoryLog createInventoryLog(InventoryLog inventoryLog);
    
    /**
     * 根据ID获取库存日志
     */
    InventoryLog getInventoryLogById(Long id);
    
    /**
     * 更新库存日志
     */
    InventoryLog updateInventoryLog(Long id, InventoryLog inventoryLog);
    
    /**
     * 删除库存日志
     */
    void deleteInventoryLog(Long id);
    
    /**
     * 分页查询库存日志列表
     */
    IPage<InventoryLog> getInventoryLogPage(Page<InventoryLog> page, Long productId, Long warehouseId, String operationType, Long operatorId, String startTime, String endTime);
    
    /**
     * 导出库存日志
     */
    void exportInventoryLog(Long productId, Long warehouseId, String operationType, Long operatorId, String startTime, String endTime);
    
    /**
     * 获取库存消耗统计数据
     */
    Map<String, Object> getConsumptionStats(String startTime, String endTime, String category);
}