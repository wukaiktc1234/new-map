package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 数据备份查询条件DTO
 * 用于接收备份列表查询的过滤条件
 */
@Schema(description = "数据备份查询条件")
public class BackupQueryDTO {
    /**
     * 备份名称（支持模糊搜索）
     */
    @Schema(description = "备份名称（模糊匹配）")
    private String backupName;
    /**
     * 备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
     */
    @Schema(description = "备份类型: 1=全量 2=增量 3=仅结构")
    private Integer backupType;
    /**
     * 状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *       3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
     */
    @Schema(description = "状态: 0=待处理 1=进行中 2=成功 3=失败 4=已过期 5=删除中")
    private Integer status;
    /**
     * 存储位置: local/s3/oss
     */
    @Schema(description = "存储位置: local/s3/oss")
    private String storageLocation;
    /**
     * 触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统
     */
    @Schema(description = "触发方式: MANUAL/SCHEDULED/SYSTEM")
    private String triggeredBy;
    /**
     * 查询开始时间
     */
    @Schema(description = "查询开始时间")
    private LocalDateTime startTime;
    /**
     * 查询结束时间
     */
    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;
    /**
     * 当前页码（默认1）
     */
    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;
    /**
     * 每页大小（默认20，最大100）
     */
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    public BackupQueryDTO() {
    }

    /**
     * 备份名称（支持模糊搜索）
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
     * 状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *       3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 存储位置: local/s3/oss
     */
    public String getStorageLocation() {
        return this.storageLocation;
    }

    /**
     * 触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统
     */
    public String getTriggeredBy() {
        return this.triggeredBy;
    }

    /**
     * 查询开始时间
     */
    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    /**
     * 查询结束时间
     */
    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    /**
     * 当前页码（默认1）
     */
    public Integer getCurrent() {
        return this.current;
    }

    /**
     * 每页大小（默认20，最大100）
     */
    public Integer getSize() {
        return this.size;
    }

    /**
     * 备份名称（支持模糊搜索）
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
     * 状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *       3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 存储位置: local/s3/oss
     */
    public void setStorageLocation(final String storageLocation) {
        this.storageLocation = storageLocation;
    }

    /**
     * 触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统
     */
    public void setTriggeredBy(final String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    /**
     * 查询开始时间
     */
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 查询结束时间
     */
    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 当前页码（默认1）
     */
    public void setCurrent(final Integer current) {
        this.current = current;
    }

    /**
     * 每页大小（默认20，最大100）
     */
    public void setSize(final Integer size) {
        this.size = size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BackupQueryDTO)) return false;
        final BackupQueryDTO other = (BackupQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$backupType = this.getBackupType();
        final java.lang.Object other$backupType = other.getBackupType();
        if (this$backupType == null ? other$backupType != null : !this$backupType.equals(other$backupType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$current = this.getCurrent();
        final java.lang.Object other$current = other.getCurrent();
        if (this$current == null ? other$current != null : !this$current.equals(other$current)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        final java.lang.Object this$backupName = this.getBackupName();
        final java.lang.Object other$backupName = other.getBackupName();
        if (this$backupName == null ? other$backupName != null : !this$backupName.equals(other$backupName)) return false;
        final java.lang.Object this$storageLocation = this.getStorageLocation();
        final java.lang.Object other$storageLocation = other.getStorageLocation();
        if (this$storageLocation == null ? other$storageLocation != null : !this$storageLocation.equals(other$storageLocation)) return false;
        final java.lang.Object this$triggeredBy = this.getTriggeredBy();
        final java.lang.Object other$triggeredBy = other.getTriggeredBy();
        if (this$triggeredBy == null ? other$triggeredBy != null : !this$triggeredBy.equals(other$triggeredBy)) return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime)) return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BackupQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $backupType = this.getBackupType();
        result = result * PRIME + ($backupType == null ? 43 : $backupType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $current = this.getCurrent();
        result = result * PRIME + ($current == null ? 43 : $current.hashCode());
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $backupName = this.getBackupName();
        result = result * PRIME + ($backupName == null ? 43 : $backupName.hashCode());
        final java.lang.Object $storageLocation = this.getStorageLocation();
        result = result * PRIME + ($storageLocation == null ? 43 : $storageLocation.hashCode());
        final java.lang.Object $triggeredBy = this.getTriggeredBy();
        result = result * PRIME + ($triggeredBy == null ? 43 : $triggeredBy.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BackupQueryDTO(backupName=" + this.getBackupName() + ", backupType=" + this.getBackupType() + ", status=" + this.getStatus() + ", storageLocation=" + this.getStorageLocation() + ", triggeredBy=" + this.getTriggeredBy() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", current=" + this.getCurrent() + ", size=" + this.getSize() + ")";
    }
}
