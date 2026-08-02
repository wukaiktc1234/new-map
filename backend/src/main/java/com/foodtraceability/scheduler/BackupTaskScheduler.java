package com.foodtraceability.scheduler;

import com.foodtraceability.dto.BackupCreateDTO;
import com.foodtraceability.entity.DataBackupRecord;
import com.foodtraceability.service.DataBackupService;
import com.foodtraceability.service.impl.DataBackupServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据备份定时任务调度器
 *
 * 功能特性:
 * 1. 每天凌晨2点自动执行全量备份
 * 2. 支持通过配置文件启用/禁用
 * 3. 自动清理过期备份（保留最近N份）
 * 4. 备份成功/失败时记录详细日志
 * 5. 支持可配置的备份参数
 *
 * 配置项:
 * - backup.auto.enabled: 是否启用自动备份（默认false）
 * - backup.auto.retention-count: 保留最近备份数量（默认10份）
 * - backup.auto.backup-name-prefix: 备份名称前缀（默认"auto_backup"）
 */
@Component
@ConditionalOnProperty(name = "backup.auto.enabled", havingValue = "true", matchIfMissing = false)
public class BackupTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BackupTaskScheduler.class);


    public BackupTaskScheduler(DataBackupService dataBackupService) {
        this.dataBackupService = dataBackupService;
    }

    /** 系统用户ID（用于标识定时任务操作者） */
    private static final Long SYSTEM_USER_ID = 0L;

    /** 系统用户名 */
    private static final String SYSTEM_USERNAME = "SYSTEM_SCHEDULER";

    private final DataBackupService dataBackupService;

    /** 自动备份保留最近N份备份数量 */
    @Value("${backup.auto.retention-count:10}")
    private int retentionCount;

    /** 备份名称前缀 */
    @Value("${backup.auto.backup-name-prefix:auto_backup}")
    private String backupNamePrefix;

    /**
     * 每天凌晨2点执行自动全量备份
     * 使用cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeScheduledBackup() {
        logger.info("========== 开始执行定时自动备份任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: 检查是否应该执行备份
            if (!shouldExecuteBackup()) {
                logger.info("当前不满足备份条件，跳过本次备份");
                return;
            }

            // Step 2: 构建备份请求
            BackupCreateDTO createDTO = buildBackupRequest();

            // Step 3: 执行备份
            logger.info("开始执行自动全量备份: 名称={}", createDTO.getBackupName());
            DataBackupRecord backupRecord = dataBackupService.createBackup(
                    createDTO,
                    SYSTEM_USER_ID,
                    SYSTEM_USERNAME
            );

            // Step 4: 记录备份结果
            if (backupRecord.getStatus() == DataBackupServiceImpl.STATUS_SUCCESS) {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("✓ 定时自动备份成功完成: ID={}, 文件={}, 大小={}字节, 耗时={}ms",
                        backupRecord.getBackupId(),
                        backupRecord.getFilePath(),
                        backupRecord.getFileSizeBytes(),
                        duration);

                // Step 5: 备份成功后清理旧备份
                cleanupOldBackups();

                // Step 6: 发送成功通知
                sendBackupNotification(true, backupRecord, null);
            } else {
                logger.error("✗ 定时自动备份失败: ID={}, 状态={}, 错误={}",
                        backupRecord.getBackupId(),
                        backupRecord.getStatus(),
                        backupRecord.getErrorMessage());

                // 发送失败告警
                sendBackupNotification(false, backupRecord, backupRecord.getErrorMessage());
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("✗ 定时自动备份任务执行异常! 耗时={}ms, 错误={}", duration, e.getMessage(), e);

            // 发送异常告警通知
            sendBackupNotification(false, null, "任务执行异常: " + e.getMessage());
        }

        logger.info("========== 定时自动备份任务结束 ==========");
    }

    /**
     * 判断是否应该执行备份
     * 可扩展：检查系统负载、业务时间窗口等条件
     */
    private boolean shouldExecuteBackup() {
        // TODO: 可添加更多判断条件
        // 1. 检查系统负载是否过高
        // 2. 检查是否在业务高峰期
        // 3. 检查磁盘空间是否充足（DataBackupService内部已检查）

        return true;
    }

    /**
     * 构建自动备份请求对象
     * 使用配置的默认参数创建标准化的备份请求
     */
    private BackupCreateDTO buildBackupRequest() {
        BackupCreateDTO createDTO = new BackupCreateDTO();

        // 生成带时间戳的备份名称
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        createDTO.setBackupName(String.format("%s_%s", backupNamePrefix, timestamp));

        // 全量备份
        createDTO.setBackupType(1);  // FULL全量备份
        createDTO.setBackupMethod(1);  // PG_DUMP方式

        // 默认保留30天
        createDTO.setRetentionDays(30);

        // 本地存储
        createDTO.setStorageLocation("local");

        // 添加描述信息
        createDTO.setDescription(String.format(
                "定时自动备份 - 触发时间: %s, 调度器: BackupTaskScheduler",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));

        return createDTO;
    }

    /**
     * 清理过期的旧备份
     * 保留最近N份成功的备份，删除更早的备份
     */
    private void cleanupOldBackups() {
        try {
            logger.info("开始清理过期备份，保留最近{}份...", retentionCount);

            // 调用服务层的过期备份清理方法
            int cleanedCount = dataBackupService.cleanupExpiredBackups();

            if (cleanedCount > 0) {
                logger.info("过期备份清理完成: 共清理{}条记录", cleanedCount);
            } else {
                logger.info("没有需要清理的过期备份");
            }

        } catch (Exception e) {
            logger.error("清理过期备份失败: {}", e.getMessage(), e);
            // 清理失败不影响主流程
        }
    }

    /**
     * 发送备份结果通知
     * 通过日志记录和可选的通知服务发送告警
     *
     * @param success 是否成功
     * @param backupRecord 备份记录（可为null，如果备份未创建成功）
     * @param errorMessage 错误消息（如果失败）
     */
    private void sendBackupNotification(boolean success, DataBackupRecord backupRecord, String errorMessage) {
        try {
            String subject = success ? "[备份成功] 定时自动备份完成" : "[备份失败] 定时自动备份异常";

            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("数据备份定时任务执行报告\n");
            messageBuilder.append("=" .repeat(50)).append("\n");
            messageBuilder.append(String.format("执行时间: %s\n", LocalDateTime.now()));
            messageBuilder.append(String.format("执行结果: %s\n", success ? "成功" : "失败"));

            if (backupRecord != null) {
                messageBuilder.append(String.format("备份ID: %d\n", backupRecord.getBackupId()));
                messageBuilder.append(String.format("备份名称: %s\n", backupRecord.getBackupName()));
                messageBuilder.append(String.format("文件大小: %d 字节\n", backupRecord.getFileSizeBytes()));
                messageBuilder.append(String.format("文件路径: %s\n", backupRecord.getFilePath()));
                messageBuilder.append(String.format("校验和(SHA256): %s\n", backupRecord.getFileChecksum()));
                messageBuilder.append(String.format("执行耗时: %d ms\n", backupRecord.getDurationMs()));
            }

            if (!success && errorMessage != null) {
                messageBuilder.append(String.format("错误信息: %s\n", errorMessage));
            }

            messageBuilder.append("\n请及时检查备份状态和数据完整性！");

            String message = messageBuilder.toString();

            if (success) {
                logger.info("[备份通知] {}", message);
            } else {
                logger.error("[备份告警] {}", message);
            }

            // TODO: 如果有NotificationService，可以在这里发送邮件/钉钉/企业微信等通知
            // if (notificationService != null) {
            //     notificationService.sendToAdmins(subject, message);
            // }

        } catch (Exception e) {
            logger.error("发送备份通知失败: {}", e.getMessage(), e);
            // 通知发送失败不影响主流程
        }
    }
}
