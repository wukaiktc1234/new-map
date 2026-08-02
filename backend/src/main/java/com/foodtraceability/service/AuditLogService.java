package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.AuditLog;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AuditLogService {

    void saveAsync(AuditLog auditLog);

    void saveBatchAsync(List<AuditLog> auditLogs);

    IPage<AuditLog> getAuditLogPage(
        Page<AuditLog> page,
        String userId,
        String username,
        String operationType,
        String module,
        String riskLevel,
        String ip,
        Integer sensitiveFlag,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    void exportToCsv(Map<String, Object> params, HttpServletResponse response) throws Exception;

    long countToday();

    long countByRiskLevel(String riskLevel, LocalDateTime startTime);

    Map<String, Object> getStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 归档历史审计日志。
     * 注意：审计日志仅允许 INSERT，禁止 UPDATE 和 DELETE（会计法/网络安全法要求）。
     * 归档操作仅将历史日志拷贝到 audit_log_archive 表，不修改也不删除原表数据。
     */
    int archiveLogs(int retentionDays);

    void flush();
}
