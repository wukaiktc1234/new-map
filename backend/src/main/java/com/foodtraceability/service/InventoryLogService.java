package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLog;
import java.util.Map;

/**
 * 库存日志服务接口
 * S5：写后门已关闭（宪法 §III.7 / 禁区 7：inventory_log 为只读历史表，无 create/update/delete 方法）
 * 定义库存日志管理相关的业务方法
 */
public interface InventoryLogService {
    
    /**
     * 根据ID获取库存日志
     */
    InventoryLog getInventoryLogById(Long id);
    
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
