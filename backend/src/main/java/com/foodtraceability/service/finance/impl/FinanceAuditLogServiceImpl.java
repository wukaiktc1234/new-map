package com.foodtraceability.service.finance.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.FinanceAuditLog;
import com.foodtraceability.mapper.finance.FinanceAuditLogMapper;
import com.foodtraceability.service.finance.FinanceAuditLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 财务审计日志Service实现
 * 仅支持INSERT和SELECT，不支持UPDATE和DELETE
 */
@Service
public class FinanceAuditLogServiceImpl extends ServiceImpl<FinanceAuditLogMapper, FinanceAuditLog>
        implements FinanceAuditLogService {

    @Override
    public FinanceAuditLogVO getDetail(Long logId) {
        FinanceAuditLog entity = baseMapper.selectById(logId);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public IPage<FinanceAuditLogVO> getPage(FinanceAuditLogQueryDTO query) {
        Page<FinanceAuditLog> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<FinanceAuditLog> wrapper = new LambdaQueryWrapper<>();

        if (query.getOperatorId() != null) {
            wrapper.eq(FinanceAuditLog::getOperatorId, query.getOperatorId());
        }
        if (query.getOperationType() != null && !query.getOperationType().isBlank()) {
            wrapper.eq(FinanceAuditLog::getOperationType, query.getOperationType());
        }
        if (query.getModule() != null && !query.getModule().isBlank()) {
            wrapper.eq(FinanceAuditLog::getModule, query.getModule());
        }
        if (query.getTargetId() != null) {
            wrapper.eq(FinanceAuditLog::getTargetId, query.getTargetId());
        }
        if (query.getTargetType() != null && !query.getTargetType().isBlank()) {
            wrapper.eq(FinanceAuditLog::getTargetType, query.getTargetType());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(FinanceAuditLog::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(FinanceAuditLog::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(FinanceAuditLog::getLogId);

        IPage<FinanceAuditLog> entityPage = baseMapper.selectPage(page, wrapper);

        Page<FinanceAuditLogVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<FinanceAuditLogVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        @SuppressWarnings("unchecked")
        IPage<FinanceAuditLogVO> result = (IPage<FinanceAuditLogVO>) (IPage<?>) voPage;
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean log(FinanceAuditLog entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean log(Long operatorId, String operatorName, String operationType, String module,
                       Long targetId, String targetType, String operationDesc, String operationData, String ipAddress) {
        FinanceAuditLog entity = new FinanceAuditLog();
        entity.setOperatorId(operatorId);
        entity.setOperatorName(operatorName);
        entity.setOperationType(operationType);
        entity.setModule(module);
        entity.setTargetId(targetId);
        entity.setTargetType(targetType);
        entity.setOperationDesc(operationDesc);
        entity.setOperationData(operationData);
        entity.setIpAddress(ipAddress);
        return baseMapper.insert(entity) > 0;
    }

    /**
     * 实体转VO
     */
    private FinanceAuditLogVO convertToVO(FinanceAuditLog entity) {
        FinanceAuditLogVO vo = new FinanceAuditLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
