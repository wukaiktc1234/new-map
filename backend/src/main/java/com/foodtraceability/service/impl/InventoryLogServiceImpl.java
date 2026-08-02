package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.InventoryLog;
import com.foodtraceability.mapper.InventoryLogMapper;
import com.foodtraceability.service.InventoryLogService;
import org.springframework.stereotype.Service;

/**
 * 库存日志服务实现类
 * 实现库存日志管理相关的业务方法
 */
@Service
public class InventoryLogServiceImpl extends ServiceImpl<InventoryLogMapper, InventoryLog> implements InventoryLogService {
    

    public InventoryLogServiceImpl(InventoryLogMapper inventoryLogMapper) {
        this.inventoryLogMapper = inventoryLogMapper;
    }

    private final InventoryLogMapper inventoryLogMapper;
    
    @Override
    public InventoryLog createInventoryLog(InventoryLog inventoryLog) {
        this.save(inventoryLog);
        return inventoryLog;
    }
    
    @Override
    public InventoryLog getInventoryLogById(Long id) {
        return this.getById(id);
    }
    
    @Override
    public InventoryLog updateInventoryLog(Long id, InventoryLog inventoryLog) {
        inventoryLog.setId(id);
        this.updateById(inventoryLog);
        return this.getById(id);
    }
    
    @Override
    public void deleteInventoryLog(Long id) {
        this.removeById(id);
    }
    
    @Override
    public IPage<InventoryLog> getInventoryLogPage(Page<InventoryLog> page, Long productId, Long warehouseId, String operationType, Long operatorId, String startTime, String endTime) {
        return inventoryLogMapper.selectInventoryLogPage(page, productId, warehouseId, operationType, operatorId, startTime, endTime);
    }
    
    @Override
    public void exportInventoryLog(Long productId, Long warehouseId, String operationType, Long operatorId, String startTime, String endTime) {
        // 这里可以实现库存日志导出功能，例如导出为Excel文件
        // 目前暂时为空实现，后续可以根据需求完善
    }
    
    @Override
    public java.util.Map<String, Object> getConsumptionStats(String startTime, String endTime, String category) {
        // 查询库存消耗统计数据，operationType为"out"表示消耗
        return inventoryLogMapper.selectConsumptionStats(startTime, endTime, "out");
    }
}