package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 数据备份记录实体
 * 对应数据库表 data_backup_record
 * 用于记录所有数据库备份操作的详细信息
 */
@TableName("data_backup_record")
@Schema(description = "数据备份记录实体")
public class DataBackupRecord {
    /**
     * 备份记录主键
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "备份记录主键ID")
    private Long backupId;
    /**
     * 备份名称
     */
    @TableField("backup_name")
    @Schema(description = "备份名称")
    private String backupName;
    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     */
    @TableField("backup_type")
    @Schema(description = "备份类型: 1=全量 2=增量 3=仅结构")
    private Integer backupType;
    /**
     * 备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
     */
    @TableField("backup_method")
    @Schema(description = "备份方式: 1=PG_DUMP 2=PG_RESTORE 3=自定义")
    private Integer backupMethod;
    /**
     * 备份文件存储路径
     */
    @TableField("file_path")
    @Schema(description = "备份文件存储路径")
    private String filePath;
    /**
     * 备份文件大小(字节)
     */
    @TableField("file_size_bytes")
    @Schema(description = "备份文件大小(字节)")
    private Long fileSizeBytes;
    /**
     * SHA256文件校验和
     */
    @TableField("file_checksum")
    @Schema(description = "SHA256文件校验和")
    private String fileChecksum;
    /**
     * 状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *       3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
     */
    @TableField("status")
    @Schema(description = "状态: 0=待处理 1=进行中 2=成功 3=失败 4=已过期 5=删除中")
    private Integer status;
    /**
     * 包含的表列表(JSON数组格式)
     */
    @TableField("tables_included")
    @Schema(description = "包含的表列表(JSON数组)")
    private String tablesIncluded;
    /**
     * 影响行数
     */
    @TableField("rows_affected")
    @Schema(description = "影响行数")
    private Long rowsAffected;
    /**
     * 执行耗时(毫秒)
     */
    @TableField("duration_ms")
    @Schema(description = "执行耗时(毫秒)")
    private Long durationMs;
    /**
     * 错误信息
     */
    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;
    /**
     * 触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统
     */
    @TableField("triggered_by")
    @Schema(description = "触发方式: MANUAL/SCHEDULED/SYSTEM")
    private String triggeredBy;
    /**
     * 触发用户ID
     */
    @TableField("trigger_user_id")
    @Schema(description = "触发用户ID")
    private Long triggerUserId;
    /**
     * 触发用户名
     */
    @TableField("trigger_username")
    @Schema(description = "触发用户名")
    private String triggerUsername;
    /**
     * 存储位置: local本地/s3/oss对象存储
     */
    @TableField("storage_location")
    @Schema(description = "存储位置: local/s3/oss")
    private String storageLocation;
    /**
     * 保留天数
     */
    @TableField("retention_days")
    @Schema(description = "保留天数")
    private Integer retentionDays;
    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("expires_at")
    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;
    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;
    /**
     * 逻辑删除标记: 0未删除 1已删除
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记: 0未删除 1已删除")
    private Integer deleted;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public DataBackupRecord() {
    }

    /**
     * 备份记录主键
     */
    public Long getBackupId() {
        return this.backupId;
    }

    /**
     * 备份名称
     */
    public String getBackupName() {
        return this.backupName;
    }

    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     */
    public Integer getBackupType() {
        return this.backupType;
    }

    /**
     * 备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
     */
    public Integer getBackupMethod() {
        return this.backupMethod;
    }

    /**
     * 备份文件存储路径
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * 备份文件大小(字节)
     */
    public Long getFileSizeBytes() {
        return this.fileSizeBytes;
    }

    /**
     * SHA256文件校验和
     */
    public String getFileChecksum() {
        return this.fileChecksum;
    }

    /**
     * 状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *       3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 包含的表列表(JSON数组格式)
     */
    public String getTablesIncluded() {
        return this.tablesIncluded;
    }

    /**
     * 影响行数
     */
    public Long getRowsAffected() {
        return this.rowsAffected;
    }

    /**
     * 执行耗时(毫秒)
     */
    public Long getDurationMs() {
        return this.durationMs;
    }

    /**
     * 错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * 触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统
     */
    public String getTriggeredBy() {
        return this.triggeredBy;
    }

    /**
     * 触发用户ID
     */
    public Long getTriggerUserId() {
        return this.triggerUserId;
    }

    /**
     * 触发用户名
     */
    public String getTriggerUsername() {
        return this.triggerUsername;
    }

    /**
     * 存储位置: local本地/s3/oss对象存储
     */
    public String getStorageLocation() {
        return this.storageLocation;
    }

    /**
     * 保留天数
     */
    public Integer getRetentionDays() {
        return this.retentionDays;
    }

    /**
     * 过期时间
     */
    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    /**
     * 乐观锁版本号
     */
    public Long getVersion() {
        return this.version;
    }

    /**
     * 逻辑删除标记: 0未删除 1已删除
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 备份记录主键
     */
    public void setBackupId(final Long backupId) {
        this.backupId = backupId;
    }

    /**
     * 备份名称
     */
    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     */
    public void setBackupType(final Integer backupType) {
        this.backupType = backupType;
    }

    /**
     * 备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
     */
    public void setBackupMethod(final Integer backupMethod) {
        this.backupMethod = backupMethod;
    }

    /**
     * 备份文件存储路径
     */
    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    /**
     * 备份文件大小(字节)
     */
    public void setFileSizeBytes(final Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    /**
     * SHA256文件校验和
     */
    public void setFileChecksum(final String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    /**
     * 状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *       3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 包含的表列表(JSON数组格式)
     */
    public void setTablesIncluded(final String tablesIncluded) {
        this.tablesIncluded = tablesIncluded;
    }

    /**
     * 影响行数
     */
    public void setRowsAffected(final Long rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    /**
     * 执行耗时(毫秒)
     */
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统
     */
    public void setTriggeredBy(final String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    /**
     * 触发用户ID
     */
    public void setTriggerUserId(final Long triggerUserId) {
        this.triggerUserId = triggerUserId;
    }

    /**
     * 触发用户名
     */
    public void setTriggerUsername(final String triggerUsername) {
        this.triggerUsername = triggerUsername;
    }

    /**
     * 存储位置: local本地/s3/oss对象存储
     */
    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    /**
     * 保留天数
     */
    public void setRetentionDays(final Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setExpiresAt(final LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 乐观锁版本号
     */
    public void setVersion(final Long version) {
        this.version = version;
    }

    /**
     * 逻辑删除标记: 0未删除 1已删除
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DataBackupRecord)) return false;
        final DataBackupRecord other = (DataBackupRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$backupId = this.getBackupId();
        final java.lang.Object other$backupId = other.getBackupId();
        if (this$backupId == null ? other$backupId != null : !this$backupId.equals(other$backupId)) return false;
        final java.lang.Object this$backupType = this.getBackupType();
        final java.lang.Object other$backupType = other.getBackupType();
        if (this$backupType == null ? other$backupType != null : !this$backupType.equals(other$backupType)) return false;
        final java.lang.Object this$backupMethod = this.getBackupMethod();
        final java.lang.Object other$backupMethod = other.getBackupMethod();
        if (this$backupMethod == null ? other$backupMethod != null : !this$backupMethod.equals(other$backupMethod)) return false;
        final java.lang.Object this$fileSizeBytes = this.getFileSizeBytes();
        final java.lang.Object other$fileSizeBytes = other.getFileSizeBytes();
        if (this$fileSizeBytes == null ? other$fileSizeBytes != null : !this$fileSizeBytes.equals(other$fileSizeBytes)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$rowsAffected = this.getRowsAffected();
        final java.lang.Object other$rowsAffected = other.getRowsAffected();
        if (this$rowsAffected == null ? other$rowsAffected != null : !this$rowsAffected.equals(other$rowsAffected)) return false;
        final java.lang.Object this$durationMs = this.getDurationMs();
        final java.lang.Object other$durationMs = other.getDurationMs();
        if (this$durationMs == null ? other$durationMs != null : !this$durationMs.equals(other$durationMs)) return false;
        final java.lang.Object this$triggerUserId = this.getTriggerUserId();
        final java.lang.Object other$triggerUserId = other.getTriggerUserId();
        if (this$triggerUserId == null ? other$triggerUserId != null : !this$triggerUserId.equals(other$triggerUserId)) return false;
        final java.lang.Object this$retentionDays = this.getRetentionDays();
        final java.lang.Object other$retentionDays = other.getRetentionDays();
        if (this$retentionDays == null ? other$retentionDays != null : !this$retentionDays.equals(other$retentionDays)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$backupName = this.getBackupName();
        final java.lang.Object other$backupName = other.getBackupName();
        if (this$backupName == null ? other$backupName != null : !this$backupName.equals(other$backupName)) return false;
        final java.lang.Object this$filePath = this.getFilePath();
        final java.lang.Object other$filePath = other.getFilePath();
        if (this$filePath == null ? other$filePath != null : !this$filePath.equals(other$filePath)) return false;
        final java.lang.Object this$fileChecksum = this.getFileChecksum();
        final java.lang.Object other$fileChecksum = other.getFileChecksum();
        if (this$fileChecksum == null ? other$fileChecksum != null : !this$fileChecksum.equals(other$fileChecksum)) return false;
        final java.lang.Object this$tablesIncluded = this.getTablesIncluded();
        final java.lang.Object other$tablesIncluded = other.getTablesIncluded();
        if (this$tablesIncluded == null ? other$tablesIncluded != null : !this$tablesIncluded.equals(other$tablesIncluded)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$triggeredBy = this.getTriggeredBy();
        final java.lang.Object other$triggeredBy = other.getTriggeredBy();
        if (this$triggeredBy == null ? other$triggeredBy != null : !this$triggeredBy.equals(other$triggeredBy)) return false;
        final java.lang.Object this$triggerUsername = this.getTriggerUsername();
        final java.lang.Object other$triggerUsername = other.getTriggerUsername();
        if (this$triggerUsername == null ? other$triggerUsername != null : !this$triggerUsername.equals(other$triggerUsername)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$expiresAt = this.getExpiresAt();
        final java.lang.Object other$expiresAt = other.getExpiresAt();
        if (this$expiresAt == null ? other$expiresAt != null : !this$expiresAt.equals(other$expiresAt)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DataBackupRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $backupId = this.getBackupId();
        result = result * PRIME + ($backupId == null ? 43 : $backupId.hashCode());
        final java.lang.Object $backupType = this.getBackupType();
        result = result * PRIME + ($backupType == null ? 43 : $backupType.hashCode());
        final java.lang.Object $backupMethod = this.getBackupMethod();
        result = result * PRIME + ($backupMethod == null ? 43 : $backupMethod.hashCode());
        final java.lang.Object $fileSizeBytes = this.getFileSizeBytes();
        result = result * PRIME + ($fileSizeBytes == null ? 43 : $fileSizeBytes.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $rowsAffected = this.getRowsAffected();
        result = result * PRIME + ($rowsAffected == null ? 43 : $rowsAffected.hashCode());
        final java.lang.Object $durationMs = this.getDurationMs();
        result = result * PRIME + ($durationMs == null ? 43 : $durationMs.hashCode());
        final java.lang.Object $triggerUserId = this.getTriggerUserId();
        result = result * PRIME + ($triggerUserId == null ? 43 : $triggerUserId.hashCode());
        final java.lang.Object $retentionDays = this.getRetentionDays();
        result = result * PRIME + ($retentionDays == null ? 43 : $retentionDays.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $backupName = this.getBackupName();
        result = result * PRIME + ($backupName == null ? 43 : $backupName.hashCode());
        final java.lang.Object $filePath = this.getFilePath();
        result = result * PRIME + ($filePath == null ? 43 : $filePath.hashCode());
        final java.lang.Object $fileChecksum = this.getFileChecksum();
        result = result * PRIME + ($fileChecksum == null ? 43 : $fileChecksum.hashCode());
        final java.lang.Object $tablesIncluded = this.getTablesIncluded();
        result = result * PRIME + ($tablesIncluded == null ? 43 : $tablesIncluded.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $triggeredBy = this.getTriggeredBy();
        result = result * PRIME + ($triggeredBy == null ? 43 : $triggeredBy.hashCode());
        final java.lang.Object $triggerUsername = this.getTriggerUsername();
        result = result * PRIME + ($triggerUsername == null ? 43 : $triggerUsername.hashCode());
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $expiresAt = this.getExpiresAt();
        result = result * PRIME + ($expiresAt == null ? 43 : $expiresAt.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DataBackupRecord(backupId=" + this.getBackupId() + ", backupName=" + this.getBackupName() + ", backupType=" + this.getBackupType() + ", backupMethod=" + this.getBackupMethod() + ", filePath=" + this.getFilePath() + ", fileSizeBytes=" + this.getFileSizeBytes() + ", fileChecksum=" + this.getFileChecksum() + ", status=" + this.getStatus() + ", tablesIncluded=" + this.getTablesIncluded() + ", rowsAffected=" + this.getRowsAffected() + ", durationMs=" + this.getDurationMs() + ", errorMessage=" + this.getErrorMessage() + ", triggeredBy=" + this.getTriggeredBy() + ", triggerUserId=" + this.getTriggerUserId() + ", triggerUsername=" + this.getTriggerUsername() + ", storageLocation=" + this.getStorageLocation() + ", retentionDays=" + this.getRetentionDays() + ", expiresAt=" + this.getExpiresAt() + ", version=" + this.getVersion() + ", deleted=" + this.getDeleted() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
