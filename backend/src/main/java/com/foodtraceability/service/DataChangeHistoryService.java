package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DataChangeHistory;
import java.util.List;

/**
 * 数据变更历史Service接口
 */
public interface DataChangeHistoryService extends IService<DataChangeHistory> {
    
    /**
     * 根据记录ID和表名查询变更历史
     * @param recordId 记录ID
     * @param tableName 表名
     * @return 变更历史列表
     */
    List<DataChangeHistory> getHistoryByRecordId(String recordId, String tableName);
    
    /**
     * 根据表名查询变更历史
     * @param tableName 表名
     * @return 变更历史列表
     */
    List<DataChangeHistory> getHistoryByTableName(String tableName);
    
    /**
     * 保存数据变更记录
     * @param dataChangeHistory 数据变更历史实体
     * @return 保存结果
     */
    boolean saveHistory(DataChangeHistory dataChangeHistory);
}