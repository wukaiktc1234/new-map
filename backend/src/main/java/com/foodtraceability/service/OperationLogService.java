package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.OperationLogEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLogEntity> {

    /**
     * 记录操作日志
     */
    void logOperation(OperationLogEntity operationLog);

    /**
     * 根据条件查询操作日志
     */
    List<OperationLogEntity> queryLogs(String operationModule, String operationType, String operatorName,
                                 LocalDateTime startTime, LocalDateTime endTime);
}
