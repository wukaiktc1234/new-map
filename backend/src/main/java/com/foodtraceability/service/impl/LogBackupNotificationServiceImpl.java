package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.BackupNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志备份通知服务实现
 * 通过日志记录备份通知（基础实现，可扩展为邮件、短信等）
 */
@Service
public class LogBackupNotificationServiceImpl implements BackupNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogBackupNotificationServiceImpl.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result<Void> sendBackupSuccessNotification(String backupId, String backupName, String fileSize, long duration) {
        log.info("========================================");
        log.info("【备份成功通知】");
        log.info("备份ID: {}", backupId);
        log.info("备份名称: {}", backupName);
        log.info("文件大小: {}", fileSize);
        log.info("备份耗时: {} 秒", duration);
        log.info("备份时间: {}", LocalDateTime.now().format(formatter));
        log.info("========================================");
        return Result.success();
    }

    @Override
    public Result<Void> sendBackupFailureNotification(String backupId, String backupName, String errorMessage) {
        log.error("========================================");
        log.error("【备份失败通知】");
        log.error("备份ID: {}", backupId);
        log.error("备份名称: {}", backupName);
        log.error("错误信息: {}", errorMessage);
        log.error("失败时间: {}", LocalDateTime.now().format(formatter));
        log.error("========================================");
        return Result.success();
    }

    @Override
    public Result<Void> sendRestoreSuccessNotification(String backupId, String backupName, String operator) {
        log.info("========================================");
        log.info("【恢复成功通知】");
        log.info("备份ID: {}", backupId);
        log.info("备份名称: {}", backupName);
        log.info("操作人: {}", operator);
        log.info("恢复时间: {}", LocalDateTime.now().format(formatter));
        log.info("========================================");
        return Result.success();
    }

    @Override
    public Result<Void> sendRestoreFailureNotification(String backupId, String backupName, String operator, String errorMessage) {
        log.error("========================================");
        log.error("【恢复失败通知】");
        log.error("备份ID: {}", backupId);
        log.error("备份名称: {}", backupName);
        log.error("操作人: {}", operator);
        log.error("错误信息: {}", errorMessage);
        log.error("失败时间: {}", LocalDateTime.now().format(formatter));
        log.error("========================================");
        return Result.success();
    }

    @Override
    public Result<Void> sendStorageWarning(long usedSpace, long totalSpace, double usagePercent) {
        String usedFormatted = formatFileSize(usedSpace);
        String totalFormatted = formatFileSize(totalSpace);

        log.warn("========================================");
        log.warn("【存储空间警告】");
        log.warn("已用空间: {} / {}", usedFormatted, totalFormatted);
        log.warn("使用率: {:.2f}%", usagePercent);
        log.warn("警告时间: {}", LocalDateTime.now().format(formatter));

        if (usagePercent >= 90) {
            log.warn("警告级别: 严重 - 存储空间即将耗尽，请立即清理！");
        } else if (usagePercent >= 80) {
            log.warn("警告级别: 高 - 存储空间不足，建议尽快清理！");
        } else if (usagePercent >= 70) {
            log.warn("警告级别: 中 - 存储空间使用率较高，请注意监控！");
        }

        log.warn("========================================");
        return Result.success();
    }

    @Override
    public Result<Void> sendDataConsistencyReport(String checkType, int issues, int totalChecks) {
        log.info("========================================");
        log.info("【数据一致性检查报告】");
        log.info("检查类型: {}", checkType);
        log.info("总检查项: {}", totalChecks);
        log.info("发现问题: {}", issues);
        log.info("检查时间: {}", LocalDateTime.now().format(formatter));

        if (issues == 0) {
            log.info("检查结果: 通过 - 未发现数据一致性问题");
        } else if (issues < 5) {
            log.warn("检查结果: 警告 - 发现 {} 个潜在问题，建议关注", issues);
        } else {
            log.error("检查结果: 失败 - 发现 {} 个问题，需要立即处理！", issues);
        }

        log.info("========================================");
        return Result.success();
    }

    @Override
    public Result<Boolean> isAvailable() {
        return Result.success(true);
    }

    @Override
    public String getNotificationType() {
        return "LOG";
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
