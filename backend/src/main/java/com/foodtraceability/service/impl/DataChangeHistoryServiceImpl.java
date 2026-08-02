package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DataChangeHistory;
import com.foodtraceability.mapper.DataChangeHistoryMapper;
import com.foodtraceability.service.DataChangeHistoryService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 数据变更历史Service实现类
 */
@Service
public class DataChangeHistoryServiceImpl extends ServiceImpl<DataChangeHistoryMapper, DataChangeHistory> implements DataChangeHistoryService {

    @Override
    public List<DataChangeHistory> getHistoryByRecordId(String recordId, String tableName) {
        LambdaQueryWrapper<DataChangeHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DataChangeHistory::getRecordId, recordId)
                    .eq(DataChangeHistory::getTableName, tableName)
                    .orderByDesc(DataChangeHistory::getOperationTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<DataChangeHistory> getHistoryByTableName(String tableName) {
        LambdaQueryWrapper<DataChangeHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DataChangeHistory::getTableName, tableName)
                    .orderByDesc(DataChangeHistory::getOperationTime);
        return this.list(queryWrapper);
    }

    @Override
    public boolean saveHistory(DataChangeHistory dataChangeHistory) {
        return this.save(dataChangeHistory);
    }
}