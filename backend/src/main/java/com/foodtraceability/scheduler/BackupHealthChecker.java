package com.foodtraceability.scheduler;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.DataBackupRecord;
import com.foodtraceability.mapper.DataBackupMapper;
import com.foodtraceability.service.impl.DataBackupServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 数据备份健康检查器
 *
 * 功能特性:
 * 1. 检查最后一次备份是否成功及时间间隔
 * 2. 验证备份文件的完整性（SHA256重新校验）
 * 3. 监控磁盘剩余空间使用情况
 * 4. 统计最近备份的成功率
 * 5. 提供标准化的健康报告（可集成到Spring Boot Actuator）
 *
 * 使用方式:
 * - 自动注册为Spring HealthIndicator，可通过 /actuator/health 访问
 * - 可通过 BackupHealthChecker.getHealthReport() 获取详细报告
 * - 支持自定义告警阈值配置
 *
 * 配置项:
 * - backup.health.max-backup-age-hours: 备份最大允许年龄（小时），默认24小时
 * - backup.health.min-disk-space-percent: 最小磁盘可用空间百分比，默认10%
 * - backup.health.check-recent-count: 检查的最近备份数量，默认10份
 */
@Component
public class BackupHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(BackupHealthChecker.class);


    public BackupHealthChecker(DataBackupMapper dataBackupMapper) {
        this.dataBackupMapper = dataBackupMapper;
    }

    private final DataBackupMapper dataBackupMapper;

    /** 备份存储路径 */
    @Value("${backup.storage.path:./backups}")
    private String backupStoragePath;

    /** 备份最大允许年龄（小时） */
    @Value("${backup.health.max-backup-age-hours:24}")
    private int maxBackupAgeHours;

    /** 最小磁盘可用空间百分比 */
    @Value("${backup.health.min-disk-space-percent:10.0}")
    private double minDiskSpacePercent;

    /** 检查的最近备份数量 */
    @Value("${backup.health.check-recent-count:10}")
    private int recentCheckCount;

    /**
     * Spring Boot Actuator 健康检查端点接口实现
     * 返回简化的健康状态（UP/DOWN）
     */
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Map<String, Object> healthReport = getHealthReport();
            boolean isHealthy = checkOverallHealth(healthReport);
            result.putAll(healthReport);
            result.put("status", isHealthy ? "UP" : "DOWN");
        } catch (Exception e) {
            logger.error("备份健康检查异常: {}", e.getMessage(), e);
            result.put("status", "DOWN");
            result.put("error", "健康检查执行异常: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取详细的备份健康检查报告
     * 包含所有检查项的详细信息和建议
     *
     * @return 健康报告Map，包含所有检查指标
     */
    public Map<String, Object> getHealthReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // 1. 最后一次备份状态检查
        report.putAll(checkLastBackupStatus());

        // 2. 最近备份成功率统计
        report.putAll(checkRecentBackupSuccessRate());

        // 3. 磁盘空间检查
        report.putAll(checkDiskSpace());

        // 4. 备份文件完整性抽样校验
        report.putAll(checkBackupFileIntegrity());

        // 5. 整体健康评估
        report.put("overallHealthy", checkOverallHealth(report));
        report.put("checkTime", LocalDateTime.now().toString());

        return report;
    }

    /**
     * 检查最后一次备份状态
     * 包括：是否成功、备份时间、文件大小、时间间隔等
     */
    private Map<String, Object> checkLastBackupStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("checkItem", "最后一次备份状态");

        try {
            // 查询最近的一条备份记录（按创建时间倒序）
            Page<DataBackupRecord> page = new Page<>(1, 1);
            List<DataBackupRecord> latestBackups = dataBackupMapper.selectLatestSuccessBackups(1);

            if (latestBackups == null || latestBackups.isEmpty()) {
                result.put("status", "WARNING");
                result.put("message", "没有找到任何备份记录");
                result.put("hasBackup", false);
                result.put("lastBackupTime", null);
                result.put("hoursSinceLastBackup", null);
                return result;
            }

            DataBackupRecord lastBackup = latestBackups.get(0);
            LocalDateTime lastBackupTime = lastBackup.getCreateTime();

            // 计算距离上次备份的小时数
            long hoursSinceBackup = ChronoUnit.HOURS.between(lastBackupTime, LocalDateTime.now());

            result.put("hasBackup", true);
            result.put("lastBackupId", lastBackup.getBackupId());
            result.put("lastBackupName", lastBackup.getBackupName());
            result.put("lastBackupTime", lastBackupTime != null ? lastBackupTime.toString() : null);
            result.put("lastBackupStatus", lastBackup.getStatus());
            result.put("lastBackupSize", lastBackup.getFileSizeBytes());
            result.put("lastBackupChecksum", lastBackup.getFileChecksum());
            result.put("hoursSinceLastBackup", hoursSinceBackup);

            // 判断状态
            boolean isRecentEnough = hoursSinceBackup <= maxBackupAgeHours;
            boolean isSuccess = lastBackup.getStatus() == DataBackupServiceImpl.STATUS_SUCCESS;

            if (isSuccess && isRecentEnough) {
                result.put("status", "OK");
                result.put("message", String.format("最后备份正常: %s前完成", formatDuration(hoursSinceBackup)));
            } else if (!isSuccess) {
                result.put("status", "CRITICAL");
                result.put("message", String.format("最后备份失败! 状态码=%d, 错误=%s",
                        lastBackup.getStatus(), lastBackup.getErrorMessage()));
            } else {
                result.put("status", "WARNING");
                result.put("message", String.format("备份已过期: 上次备份在%s前（超过%d小时阈值）",
                        formatDuration(hoursSinceBackup), maxBackupAgeHours));
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "检查失败: " + e.getMessage());
            logger.error("检查最后一次备份状态失败", e);
        }

        return result;
    }

    /**
     * 检查最近N次备份的成功率
     * 用于判断备份系统的稳定性
     */
    private Map<String, Object> checkRecentBackupSuccessRate() {
        Map<String, Object> result = new HashMap<>();
        result.put("checkItem", "最近备份成功率");

        try {
            List<DataBackupRecord> recentBackups = dataBackupMapper.selectLatestSuccessBackups(recentCheckCount);

            if (recentBackups == null || recentBackups.isEmpty()) {
                result.put("status", "WARNING");
                result.put("message", "没有足够的备份数据统计");
                result.put("totalBackups", 0);
                result.put("successCount", 0);
                result.put("successRate", 0.0);
                return result;
            }

            int totalCount = recentBackups.size();
            long successCount = recentBackups.stream()
                    .filter(b -> b.getStatus() == DataBackupServiceImpl.STATUS_SUCCESS)
                    .count();

            double successRate = (double) successCount / totalCount * 100;

            result.put("totalBackups", totalCount);
            result.put("successCount", successCount);
            result.put("failedCount", totalCount - successCount);
            result.put("successRate", Math.round(successRate * 100.0) / 100.0);

            // 判断成功率是否达标（要求>=90%）
            if (successRate >= 90.0) {
                result.put("status", "OK");
                result.put("message", String.format("备份系统稳定: 成功率%.1f%% (%d/%d)", successRate, successCount, totalCount));
            } else if (successRate >= 70.0) {
                result.put("status", "WARNING");
                result.put("message", String.format("备份成功率偏低: %.1f%% (%d/%d)，建议检查", successRate, successCount, totalCount));
            } else {
                result.put("status", "CRITICAL");
                result.put("message", String.format("备份系统严重异常! 成功率仅%.1f%% (%d/%d)", successRate, successCount, totalCount));
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "统计失败: " + e.getMessage());
            logger.error("检查备份成功率失败", e);
        }

        return result;
    }

    /**
     * 检查磁盘剩余空间
     * 确保有足够的空间进行后续备份操作
     */
    private Map<String, Object> checkDiskSpace() {
        Map<String, Object> result = new HashMap<>();
        result.put("checkItem", "磁盘空间使用情况");

        try {
            Path rootPath = Paths.get(backupStoragePath).toAbsolutePath().getRoot();
            if (rootPath == null) {
                result.put("status", "ERROR");
                result.put("message", "无法确定存储根路径");
                return result;
            }

            FileStore store = Files.getFileStore(rootPath);
            long totalSpace = store.getTotalSpace();
            long usableSpace = store.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            double usagePercent = (double) usedSpace / totalSpace * 100;
            double availablePercent = (double) usableSpace / totalSpace * 100;

            result.put("totalSpace", totalSpace);
            result.put("usableSpace", usableSpace);
            result.put("usedSpace", usedSpace);
            result.put("usagePercent", Math.round(usagePercent * 100.0) / 100.0);
            result.put("availablePercent", Math.round(availablePercent * 100.0) / 100.0);
            result.put("storagePath", rootPath.toString());

            // 格式化显示
            result.put("totalSpaceFormatted", formatFileSize(totalSpace));
            result.put("usableSpaceFormatted", formatFileSize(usableSpace));
            result.put("usedSpaceFormatted", formatFileSize(usedSpace));

            // 判断磁盘空间是否充足
            if (availablePercent >= minDiskSpacePercent) {
                result.put("status", "OK");
                result.put("message", String.format("磁盘空间充足: 可用%.2f%% (%s)",
                        availablePercent, formatFileSize(usableSpace)));
            } else if (availablePercent >= minDiskSpacePercent * 0.5) {
                result.put("status", "WARNING");
                result.put("message", String.format("磁盘空间偏低: 仅剩%.2f%% (%s)，建议清理",
                        availablePercent, formatFileSize(usableSpace)));
            } else {
                result.put("status", "CRITICAL");
                result.put("message", String.format("磁盘空间严重不足! 仅剩%.2f%% (%s)",
                        availablePercent, formatFileSize(usableSpace)));
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "获取磁盘信息失败: " + e.getMessage());
            logger.error("检查磁盘空间失败", e);
        }

        return result;
    }

    /**
     * 抽样检查备份文件的完整性
     * 对最近的一份成功备份进行SHA256重新校验
     */
    private Map<String, Object> checkBackupFileIntegrity() {
        Map<String, Object> result = new HashMap<>();
        result.put("checkItem", "备份文件完整性校验");

        try {
            // 获取最近一份成功的备份
            List<DataBackupRecord> latestBackups = dataBackupMapper.selectLatestSuccessBackups(1);

            if (latestBackups == null || latestBackups.isEmpty()) {
                result.put("status", "SKIPPED");
                result.put("message", "没有可校验的备份文件");
                result.put("fileChecked", false);
                return result;
            }

            DataBackupRecord backup = latestBackups.get(0);

            // 检查文件是否存在
            if (backup.getFilePath() == null || backup.getFilePath().isEmpty()) {
                result.put("status", "WARNING");
                result.put("message", "备份记录中没有文件路径");
                result.put("fileChecked", false);
                return result;
            }

            File backupFile = new File(backup.getFilePath());
            if (!backupFile.exists()) {
                result.put("status", "CRITICAL");
                result.put("message", String.format("备份文件丢失! 路径: %s", backup.getFilePath()));
                result.put("fileChecked", true);
                result.put("fileExists", false);
                return result;
            }

            // 重新计算SHA256并比对
            String currentChecksum = calculateFileSHA256(backupFile);
            String storedChecksum = backup.getFileChecksum();

            result.put("fileChecked", true);
            result.put("fileExists", true);
            result.put("filePath", backup.getFilePath());
            result.put("fileSize", backupFile.length());
            result.put("storedChecksum", storedChecksum);
            result.put("currentChecksum", currentChecksum);

            // 校验结果
            if (storedChecksum != null && storedChecksum.equalsIgnoreCase(currentChecksum)) {
                result.put("status", "OK");
                result.put("message", "文件完整性校验通过 (SHA256匹配)");
                result.put("integrityValid", true);
            } else if (storedChecksum == null || storedChecksum.isEmpty()) {
                result.put("status", "WARNING");
                result.put("message", "备份记录中未存储原始校验和，无法验证");
                result.put("integrityValid", null);
            } else {
                result.put("status", "CRITICAL");
                result.put("message", "文件完整性校验失败! SHA256不匹配，文件可能已损坏或被篡改");
                result.put("integrityValid", false);
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "文件校验过程异常: " + e.getMessage());
            result.put("fileChecked", false);
            logger.error("检查备份文件完整性失败", e);
        }

        return result;
    }

    /**
     * 根据所有检查项判断整体健康状况
     * 只有当所有关键检查都通过时才返回true
     */
    private boolean checkOverallHealth(Map<String, Object> healthReport) {
        try {
            // 检查最后一备份状态
            Map<String, Object> lastBackupStatus = (Map<String, Object>) healthReport.getOrDefault(
                    getLastBackupStatusKey(healthReport), Collections.emptyMap());
            String lastStatus = (String) lastBackupStatus.getOrDefault("status", "UNKNOWN");

            // 检查磁盘空间
            Map<String, Object> diskSpace = (Map<String, Object>) healthReport.getOrDefault(
                    getDiskSpaceKey(healthReport), Collections.emptyMap());
            String diskStatus = (String) diskSpace.getOrDefault("status", "UNKNOWN");

            // 如果任何关键检查为CRITICAL，则整体不健康
            if ("CRITICAL".equals(lastStatus) || "CRITICAL".equals(diskStatus)) {
                return false;
            }

            // 如果有ERROR，也不健康
            if ("ERROR".equals(lastStatus) || "ERROR".equals(diskStatus)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.error("判断整体健康状态失败", e);
            return false;
        }
    }

    /**
     * 查找最后一次备份状态的key（兼容不同的Map结构）
     */
    private String getLastBackupStatusKey(Map<String, Object> report) {
        for (String key : report.keySet()) {
            Object value = report.get(key);
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                if ("最后一次备份状态".equals(map.get("checkItem"))) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * 查找磁盘空间检查的key
     */
    private String getDiskSpaceKey(Map<String, Object> report) {
        for (String key : report.keySet()) {
            Object value = report.get(key);
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                if ("磁盘空间使用情况".equals(map.get("checkItem"))) {
                    return key;
                }
            }
        }
        return null;
    }

    // ==================== 工具方法 ====================

    /**
     * 计算文件的SHA256校验和
     */
    private String calculateFileSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream fis = new java.io.FileInputStream(file)) {
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 格式化时间持续时长
     */
    private String formatDuration(long hours) {
        if (hours < 24) {
            return hours + "小时";
        } else {
            long days = hours / 24;
            long remainingHours = hours % 24;
            return days + "天" + remainingHours + "小时";
        }
    }
}
