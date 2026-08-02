package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    IPage<AuditLog> selectAuditLogPage(
        Page<AuditLog> page,
        @Param("userId") String userId,
        @Param("username") String username,
        @Param("operationType") String operationType,
        @Param("module") String module,
        @Param("riskLevel") String riskLevel,
        @Param("ip") String ip,
        @Param("sensitiveFlag") Integer sensitiveFlag,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 归档指定日期之前的审计日志（INSERT 到 audit_log_archive 表）。
     * 注意：审计日志仅允许 INSERT，禁止 UPDATE 和 DELETE（会计法/网络安全法要求），
     * 因此归档操作只做 INSERT 拷贝，不修改原表数据。
     */
    int archiveLogsBeforeDate(@Param("date") LocalDateTime date);

    long countByRiskLevel(@Param("riskLevel") String riskLevel,
                           @Param("startTime") LocalDateTime startTime);
}
