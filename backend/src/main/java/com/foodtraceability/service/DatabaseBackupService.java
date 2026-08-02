package com.foodtraceability.service;

import com.foodtraceability.common.Result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份服务接口
 */
public interface DatabaseBackupService {

    /**
     * 执行手动备份
     *
     * @param backupName 备份名称（可选）
     * @param description 备份描述（可选）
     * @param operator 操作人
     * @return 备份结果
     */
    Result<String> manualBackup(String backupName, String description, String operator);

    /**
     * 执行自动备份
     *
     * @return 备份是否成功
     */
    boolean autoBackup();

    /**
     * 恢复数据库到指定备份
     *
     * @param backupId 备份ID
     * @param operator 操作人
     * @return 恢复结果
     */
    Result<Void> restoreBackup(String backupId, String operator);

    /**
     * 删除备份文件
     *
     * @param backupId 备份ID
     * @return 删除结果
     */
    Result<Void> deleteBackup(String backupId);

    /**
     * 获取备份列表
     *
     * @return 备份列表
     */
    Result<List<BackupInfo>> getBackupList();

    /**
     * 清理过期备份
     *
     * @param retainDays 保留天数
     * @return 清理结果
     */
    Result<Void> cleanupOldBackups(int retainDays);

    /**
     * 获取备份存储路径
     *
     * @return 备份存储路径
     */
    String getBackupStoragePath();

    /**
     * 上传备份到云存储
     *
     * @param backupId 备份ID
     * @return 上传结果
     */
    Result<Map<String, Object>> uploadBackupToCloud(String backupId);

    /**
     * 从云存储下载备份
     *
     * @param backupId 备份ID
     * @return 下载结果
     */
    Result<String> downloadBackupFromCloud(String backupId);

    /**
     * 检查外键约束
     *
     * @return 检查结果
     */
    Result<Map<String, Object>> checkForeignKeyConstraints();

    /**
     * 检查字符集和排序规则
     *
     * @return 检查结果
     */
    Result<Map<String, Object>> checkCharsetAndCollation();

    /**
     * 检查数据类型和长度
     *
     * @return 检查结果
     */
    Result<Map<String, Object>> checkDataTypesAndLength();

    /**
     * 验证备份完整性
     *
     * @param backupId 备份ID
     * @return 验证结果
     */
    Result<Map<String, Object>> verifyBackupIntegrity(String backupId);

    /**
     * 备份信息
     */
    class BackupInfo {
        private String backupId;
        private String backupName;
        private String fileName;
        private String filePath;
        private long fileSize;
        private String fileSizeFormatted;
        private LocalDateTime backupTime;
        private String backupType; // MANUAL-手动, AUTO-自动
        private String description;
        private String operator;
        private String status; // SUCCESS-成功, FAILED-失败
        private String errorMessage;

        // Getters and Setters
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }

        public String getBackupName() { return backupName; }
        public void setBackupName(String backupName) { this.backupName = backupName; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public String getFileSizeFormatted() { return fileSizeFormatted; }
        public void setFileSizeFormatted(String fileSizeFormatted) { this.fileSizeFormatted = fileSizeFormatted; }

        public LocalDateTime getBackupTime() { return backupTime; }
        public void setBackupTime(LocalDateTime backupTime) { this.backupTime = backupTime; }

        public String getBackupType() { return backupType; }
        public void setBackupType(String backupType) { this.backupType = backupType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
