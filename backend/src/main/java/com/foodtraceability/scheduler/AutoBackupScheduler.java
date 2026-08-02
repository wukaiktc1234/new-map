package com.foodtraceability.scheduler;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.DatabaseBackupService;
import com.foodtraceability.service.SysSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 自动备份定时任务
 */
@Component
public class AutoBackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoBackupScheduler.class);


    public AutoBackupScheduler(DatabaseBackupService databaseBackupService, SysSettingService sysSettingService) {
        this.databaseBackupService = databaseBackupService;
        this.sysSettingService = sysSettingService;
    }

    private final DatabaseBackupService databaseBackupService;

    private final SysSettingService sysSettingService;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 每分钟检查一次是否需要执行自动备份
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndExecuteBackup() {
        try {
            log.debug("检查自动备份配置...");

            // 获取备份配置
            Result<String> autoBackupResult = sysSettingService.getSettingValue("auto_backup", "global", null);
            Result<String> backupFrequencyResult = sysSettingService.getSettingValue("backup_frequency", "global", null);
            Result<String> backupTimeResult = sysSettingService.getSettingValue("backup_time", "global", null);

            String autoBackup = autoBackupResult.isSuccess() ? autoBackupResult.getData() : null;
            String backupFrequency = backupFrequencyResult.isSuccess() ? backupFrequencyResult.getData() : null;
            String backupTime = backupTimeResult.isSuccess() ? backupTimeResult.getData() : null;

            log.debug("备份配置: autoBackup={}, frequency={}, time={}", autoBackup, backupFrequency, backupTime);

            // 检查是否开启自动备份
            if (!"true".equalsIgnoreCase(autoBackup)) {
                return;
            }

            // 检查备份频率和时间
            if (backupFrequency == null || backupTime == null) {
                log.warn("备份配置不完整: frequency={}, time={}", backupFrequency, backupTime);
                return;
            }

            // 解析备份时间
            LocalTime scheduledTime = LocalTime.parse(backupTime, timeFormatter);
            LocalTime now = LocalTime.now();

            // 检查是否到达备份时间（允许1分钟的误差）
            if (!isTimeToBackup(now, scheduledTime)) {
                return;
            }

            // 检查备份频率
            if (!shouldBackupToday(backupFrequency)) {
                log.debug("今天不需要备份，频率: {}", backupFrequency);
                return;
            }

            // 执行备份
            log.info("开始执行定时自动备份，频率: {}, 时间: {}", backupFrequency, backupTime);
            boolean success = databaseBackupService.autoBackup();

            if (success) {
                log.info("定时自动备份执行成功");
            } else {
                log.error("定时自动备份执行失败");
            }

        } catch (Exception e) {
            log.error("自动备份任务执行异常", e);
        }
    }

    /**
     * 每天凌晨清理过期备份
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldBackups() {
        try {
            log.info("开始清理过期备份...");

            // 默认保留30天
            int retainDays = 30;

            // 尝试从配置中读取保留天数
            try {
                Result<String> retainDaysResult = sysSettingService.getSettingValue("backup_retain_days", "global", null);
                String retainDaysStr = retainDaysResult.isSuccess() ? retainDaysResult.getData() : null;
                if (retainDaysStr != null && !retainDaysStr.isEmpty()) {
                    retainDays = Integer.parseInt(retainDaysStr);
                }
            } catch (Exception e) {
                log.warn("读取备份保留天数配置失败，使用默认值30天");
            }

            databaseBackupService.cleanupOldBackups(retainDays);
            log.info("过期备份清理完成");

        } catch (Exception e) {
            log.error("清理过期备份异常", e);
        }
    }

    /**
     * 检查是否到达备份时间
     */
    private boolean isTimeToBackup(LocalTime now, LocalTime scheduledTime) {
        // 允许1分钟的误差
        int nowMinutes = now.getHour() * 60 + now.getMinute();
        int scheduledMinutes = scheduledTime.getHour() * 60 + scheduledTime.getMinute();

        return Math.abs(nowMinutes - scheduledMinutes) <= 1;
    }

    /**
     * 根据频率判断今天是否需要备份
     */
    private boolean shouldBackupToday(String frequency) {
        LocalDateTime now = LocalDateTime.now();

        return switch (frequency.toLowerCase()) {
            case "daily" -> true; // 每天
            case "weekly" -> now.getDayOfWeek().getValue() == 1; // 每周一
            case "monthly" -> now.getDayOfMonth() == 1; // 每月1号
            default -> false;
        };
    }
}
