package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.mapper.OperationLogMapper;
import com.foodtraceability.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现类
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLogEntity> implements OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void logOperation(OperationLogEntity operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            logger.error("记录操作日志失败: {}", e.getMessage());
        }
    }

    @Override
    public List<OperationLogEntity> queryLogs(String operationModule, String operationType, String operatorName,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        return operationLogMapper.selectByConditions(operationModule, operationType, operatorName, startTime, endTime);
    }
}
