package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 数据备份创建请求DTO
 * 用于接收创建备份操作的请求参数
 */
@Schema(description = "数据备份创建请求")
public class BackupCreateDTO {
    /**
     * 备份名称（必填，长度限制）
     */
    @NotBlank(message = "备份名称不能为空")
    @Schema(description = "备份名称", example = "daily_full_backup_20260404", requiredMode = Schema.RequiredMode.REQUIRED)
    private String backupName;
    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     * 默认为全量备份
     */
    @NotNull(message = "备份类型不能为空")
    @Min(value = 1, message = "备份类型值无效")
    @Max(value = 3, message = "备份类型值无效")
    @Schema(description = "备份类型: 1=全量 2=增量 3=仅结构", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer backupType;
    /**
     * 备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
     * 默认使用PG_DUMP
     */
    @Min(value = 1, message = "备份方式值无效")
    @Max(value = 3, message = "备份方式值无效")
    @Schema(description = "备份方式: 1=PG_DUMP 2=PG_RESTORE 3=自定义", example = "1")
    private Integer backupMethod = 1;
    /**
     * 需要包含的表列表（可选，为空则备份全部表）
     */
    @Schema(description = "需要包含的表名列表（为空则备份全部表）")
    private List<String> tablesIncluded;
    /**
     * 存储位置: local本地/s3/oss对象存储
     * 默认为local
     */
    @Schema(description = "存储位置: local/s3/oss", example = "local")
    private String storageLocation = "local";
    /**
     * 保留天数（默认30天，最小1天，最大365天）
     */
    @Min(value = 1, message = "保留天数最少1天")
    @Max(value = 365, message = "保留天数最多365天")
    @Schema(description = "保留天数(1-365)", example = "30")
    private Integer retentionDays = 30;
    /**
     * 是否压缩备份文件
     */
    @Schema(description = "是否压缩备份文件", example = "true")
    private Boolean compressed = true;
    /**
     * 备份描述信息
     */
    @Schema(description = "备份描述信息", example = "每日例行全量备份")
    private String description;

    public BackupCreateDTO() {
    }

    /**
     * 备份名称（必填，长度限制）
     */
    public String getBackupName() {
        return this.backupName;
    }

    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     * 默认为全量备份
     */
    public Integer getBackupType() {
        return this.backupType;
    }

    /**
     * 备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
     * 默认使用PG_DUMP
     */
    public Integer getBackupMethod() {
        return this.backupMethod;
    }

    /**
     * 需要包含的表列表（可选，为空则备份全部表）
     */
    public List<String> getTablesIncluded() {
        return this.tablesIncluded;
    }

    /**
     * 存储位置: local本地/s3/oss对象存储
     * 默认为local
     */
    public String getStorageLocation() {
        return this.storageLocation;
    }

    /**
     * 保留天数（默认30天，最小1天，最大365天）
     */
    public Integer getRetentionDays() {
        return this.retentionDays;
    }

    /**
     * 是否压缩备份文件
     */
    public Boolean getCompressed() {
        return this.compressed;
    }

    /**
     * 备份描述信息
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 备份名称（必填，长度限制）
     */
    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     * 默认为全量备份
     */
    public void setBackupType(final Integer backupType) {
        this.backupType = backupType;
    }

    /**
     * 备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
     * 默认使用PG_DUMP
     */
    public void setBackupMethod(final Integer backupMethod) {
        this.backupMethod = backupMethod;
    }

    /**
     * 需要包含的表列表（可选，为空则备份全部表）
     */
    public void setTablesIncluded(final List<String> tablesIncluded) {
        this.tablesIncluded = tablesIncluded;
    }

    /**
     * 存储位置: local本地/s3/oss对象存储
     * 默认为local
     */
    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    /**
     * 保留天数（默认30天，最小1天，最大365天）
     */
    public void setRetentionDays(final Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    /**
     * 是否压缩备份文件
     */
    public void setCompressed(final Boolean compressed) {
        this.compressed = compressed;
    }

    /**
     * 备份描述信息
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BackupCreateDTO)) return false;
        final BackupCreateDTO other = (BackupCreateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$backupType = this.getBackupType();
        final java.lang.Object other$backupType = other.getBackupType();
        if (this$backupType == null ? other$backupType != null : !this$backupType.equals(other$backupType)) return false;
        final java.lang.Object this$backupMethod = this.getBackupMethod();
        final java.lang.Object other$backupMethod = other.getBackupMethod();
        if (this$backupMethod == null ? other$backupMethod != null : !this$backupMethod.equals(other$backupMethod)) return false;
        final java.lang.Object this$retentionDays = this.getRetentionDays();
        final java.lang.Object other$retentionDays = other.getRetentionDays();
        if (this$retentionDays == null ? other$retentionDays != null : !this$retentionDays.equals(other$retentionDays)) return false;
        final java.lang.Object this$compressed = this.getCompressed();
        final java.lang.Object other$compressed = other.getCompressed();
        if (this$compressed == null ? other$compressed != null : !this$compressed.equals(other$compressed)) return false;
        final java.lang.Object this$backupName = this.getBackupName();
        final java.lang.Object other$backupName = other.getBackupName();
        if (this$backupName == null ? other$backupName != null : !this$backupName.equals(other$backupName)) return false;
        final java.lang.Object this$tablesIncluded = this.getTablesIncluded();
        final java.lang.Object other$tablesIncluded = other.getTablesIncluded();
        if (this$tablesIncluded == null ? other$tablesIncluded != null : !this$tablesIncluded.equals(other$tablesIncluded)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BackupCreateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $backupType = this.getBackupType();
        result = result * PRIME + ($backupType == null ? 43 : $backupType.hashCode());
        final java.lang.Object $backupMethod = this.getBackupMethod();
        result = result * PRIME + ($backupMethod == null ? 43 : $backupMethod.hashCode());
        final java.lang.Object $retentionDays = this.getRetentionDays();
        result = result * PRIME + ($retentionDays == null ? 43 : $retentionDays.hashCode());
        final java.lang.Object $compressed = this.getCompressed();
        result = result * PRIME + ($compressed == null ? 43 : $compressed.hashCode());
        final java.lang.Object $backupName = this.getBackupName();
        result = result * PRIME + ($backupName == null ? 43 : $backupName.hashCode());
        final java.lang.Object $tablesIncluded = this.getTablesIncluded();
        result = result * PRIME + ($tablesIncluded == null ? 43 : $tablesIncluded.hashCode());
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BackupCreateDTO(backupName=" + this.getBackupName() + ", backupType=" + this.getBackupType() + ", backupMethod=" + this.getBackupMethod() + ", tablesIncluded=" + this.getTablesIncluded() + ", storageLocation=" + this.getStorageLocation() + ", retentionDays=" + this.getRetentionDays() + ", compressed=" + this.getCompressed() + ", description=" + this.getDescription() + ")";
    }
}
