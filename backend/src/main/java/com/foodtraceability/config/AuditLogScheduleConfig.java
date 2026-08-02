package com.foodtraceability.config;

import com.foodtraceability.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditLogScheduleConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogScheduleConfig.class);


    public AuditLogScheduleConfig(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    private final AuditLogService auditLogService;

    @Value("${app.audit.log.archive-days:90}")
    private int archiveDays;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 审计日志仅允许 INSERT，禁止 UPDATE 和 DELETE（会计法/网络安全法要求）。
        // 已移除 cleanExpiredLogs 定时清理任务，仅保留归档任务（归档不删除原表数据）。
        logger.info("审计日志定时任务已启动 - 归档:{}天(原表数据永久保留,不可删除)", archiveDays);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveExpiredLogs() {
        try {
            long start = System.currentTimeMillis();
            int archived = auditLogService.archiveLogs(archiveDays);
            long duration = System.currentTimeMillis() - start;
            logger.info("[定时任务] 审计日志归档完成: 归档{}条(原表数据保留不变), 耗时{}ms", archived, duration);
        } catch (Exception e) {
            logger.error("[定时任务] 审计日志归档失败: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void flushBuffer() {
        try {
            auditLogService.flush();
        } catch (Exception e) {
            logger.debug("刷新审计日志缓冲区失败: {}", e.getMessage());
        }
    }
}
