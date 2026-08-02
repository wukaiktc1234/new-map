package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 数据恢复日志实体
 * 对应数据库表 backup_restore_log
 * 用于记录所有数据恢复操作的详细日志信息
 */
@TableName("backup_restore_log")
@Schema(description = "数据恢复日志实体")
public class BackupRestoreLog {
    /**
     * 恢复日志主键
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "恢复日志主键ID")
    private Long logId;
    /**
     * 关联的备份记录ID
     */
    @TableField("backup_id")
    @Schema(description = "关联的备份记录ID")
    private Long backupId;
    /**
     * 关联的备份名称
     */
    @TableField("backup_name")
    @Schema(description = "关联的备份名称")
    private String backupName;
    /**
     * 恢复状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *           3=FAILED失败 4=ROLLED_BACK已回滚
     */
    @TableField("restore_status")
    @Schema(description = "恢复状态: 0=待处理 1=进行中 2=成功 3=失败 4=已回滚")
    private Integer restoreStatus;
    /**
     * 恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
     */
    @TableField("restore_mode")
    @Schema(description = "恢复模式: 1=全覆盖 2=选择性 3=预览")
    private Integer restoreMode;
    /**
     * 目标表列表(选择性恢复时使用，JSON数组)
     */
    @TableField("target_tables")
    @Schema(description = "目标表列表(JSON数组)")
    private String targetTables;
    /**
     * 恢复前自动快照路径
     */
    @TableField("pre_restore_snapshot")
    @Schema(description = "恢复前自动快照路径")
    private String preRestoreSnapshot;
    /**
     * 错误信息
     */
    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;
    /**
     * 执行耗时(毫秒)
     */
    @TableField("duration_ms")
    @Schema(description = "执行耗时(毫秒)")
    private Long durationMs;
    /**
     * 确认人用户名
     */
    @TableField("confirmed_by")
    @Schema(description = "确认人用户名")
    private String confirmedBy;
    /**
     * 确认时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("confirm_time")
    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;
    /**
     * 回滚SQL语句(如果生成)
     */
    @TableField("rollback_sql")
    @Schema(description = "回滚SQL语句")
    private String rollbackSql;
    /**
     * 操作用户ID
     */
    @TableField("create_user_id")
    @Schema(description = "操作用户ID")
    private Long createUserId;
    /**
     * 操作用户名
     */
    @TableField("create_username")
    @Schema(description = "操作用户名")
    private String createUsername;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("finish_time")
    @Schema(description = "完成时间")
    private LocalDateTime finishTime;
    /**
     * 逻辑删除标记: 0未删除 1已删除
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记: 0未删除 1已删除")
    private Integer deleted;

    public BackupRestoreLog() {
    }

    /**
     * 恢复日志主键
     */
    public Long getLogId() {
        return this.logId;
    }

    /**
     * 关联的备份记录ID
     */
    public Long getBackupId() {
        return this.backupId;
    }

    /**
     * 关联的备份名称
     */
    public String getBackupName() {
        return this.backupName;
    }

    /**
     * 恢复状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *           3=FAILED失败 4=ROLLED_BACK已回滚
     */
    public Integer getRestoreStatus() {
        return this.restoreStatus;
    }

    /**
     * 恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
     */
    public Integer getRestoreMode() {
        return this.restoreMode;
    }

    /**
     * 目标表列表(选择性恢复时使用，JSON数组)
     */
    public String getTargetTables() {
        return this.targetTables;
    }

    /**
     * 恢复前自动快照路径
     */
    public String getPreRestoreSnapshot() {
        return this.preRestoreSnapshot;
    }

    /**
     * 错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * 执行耗时(毫秒)
     */
    public Long getDurationMs() {
        return this.durationMs;
    }

    /**
     * 确认人用户名
     */
    public String getConfirmedBy() {
        return this.confirmedBy;
    }

    /**
     * 确认时间
     */
    public LocalDateTime getConfirmTime() {
        return this.confirmTime;
    }

    /**
     * 回滚SQL语句(如果生成)
     */
    public String getRollbackSql() {
        return this.rollbackSql;
    }

    /**
     * 操作用户ID
     */
    public Long getCreateUserId() {
        return this.createUserId;
    }

    /**
     * 操作用户名
     */
    public String getCreateUsername() {
        return this.createUsername;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 完成时间
     */
    public LocalDateTime getFinishTime() {
        return this.finishTime;
    }

    /**
     * 逻辑删除标记: 0未删除 1已删除
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 恢复日志主键
     */
    public void setLogId(final Long logId) {
        this.logId = logId;
    }

    /**
     * 关联的备份记录ID
     */
    public void setBackupId(final Long backupId) {
        this.backupId = backupId;
    }

    /**
     * 关联的备份名称
     */
    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

    /**
     * 恢复状态: 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功
     *           3=FAILED失败 4=ROLLED_BACK已回滚
     */
    public void setRestoreStatus(final Integer restoreStatus) {
        this.restoreStatus = restoreStatus;
    }

    /**
     * 恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
     */
    public void setRestoreMode(final Integer restoreMode) {
        this.restoreMode = restoreMode;
    }

    /**
     * 目标表列表(选择性恢复时使用，JSON数组)
     */
    public void setTargetTables(final String targetTables) {
        this.targetTables = targetTables;
    }

    /**
     * 恢复前自动快照路径
     */
    public void setPreRestoreSnapshot(final String preRestoreSnapshot) {
        this.preRestoreSnapshot = preRestoreSnapshot;
    }

    /**
     * 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 执行耗时(毫秒)
     */
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 确认人用户名
     */
    public void setConfirmedBy(final String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    /**
     * 确认时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setConfirmTime(final LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    /**
     * 回滚SQL语句(如果生成)
     */
    public void setRollbackSql(final String rollbackSql) {
        this.rollbackSql = rollbackSql;
    }

    /**
     * 操作用户ID
     */
    public void setCreateUserId(final Long createUserId) {
        this.createUserId = createUserId;
    }

    /**
     * 操作用户名
     */
    public void setCreateUsername(final String createUsername) {
        this.createUsername = createUsername;
    }

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setFinishTime(final LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * 逻辑删除标记: 0未删除 1已删除
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BackupRestoreLog)) return false;
        final BackupRestoreLog other = (BackupRestoreLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$logId = this.getLogId();
        final java.lang.Object other$logId = other.getLogId();
        if (this$logId == null ? other$logId != null : !this$logId.equals(other$logId)) return false;
        final java.lang.Object this$backupId = this.getBackupId();
        final java.lang.Object other$backupId = other.getBackupId();
        if (this$backupId == null ? other$backupId != null : !this$backupId.equals(other$backupId)) return false;
        final java.lang.Object this$restoreStatus = this.getRestoreStatus();
        final java.lang.Object other$restoreStatus = other.getRestoreStatus();
        if (this$restoreStatus == null ? other$restoreStatus != null : !this$restoreStatus.equals(other$restoreStatus)) return false;
        final java.lang.Object this$restoreMode = this.getRestoreMode();
        final java.lang.Object other$restoreMode = other.getRestoreMode();
        if (this$restoreMode == null ? other$restoreMode != null : !this$restoreMode.equals(other$restoreMode)) return false;
        final java.lang.Object this$durationMs = this.getDurationMs();
        final java.lang.Object other$durationMs = other.getDurationMs();
        if (this$durationMs == null ? other$durationMs != null : !this$durationMs.equals(other$durationMs)) return false;
        final java.lang.Object this$createUserId = this.getCreateUserId();
        final java.lang.Object other$createUserId = other.getCreateUserId();
        if (this$createUserId == null ? other$createUserId != null : !this$createUserId.equals(other$createUserId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$backupName = this.getBackupName();
        final java.lang.Object other$backupName = other.getBackupName();
        if (this$backupName == null ? other$backupName != null : !this$backupName.equals(other$backupName)) return false;
        final java.lang.Object this$targetTables = this.getTargetTables();
        final java.lang.Object other$targetTables = other.getTargetTables();
        if (this$targetTables == null ? other$targetTables != null : !this$targetTables.equals(other$targetTables)) return false;
        final java.lang.Object this$preRestoreSnapshot = this.getPreRestoreSnapshot();
        final java.lang.Object other$preRestoreSnapshot = other.getPreRestoreSnapshot();
        if (this$preRestoreSnapshot == null ? other$preRestoreSnapshot != null : !this$preRestoreSnapshot.equals(other$preRestoreSnapshot)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$confirmedBy = this.getConfirmedBy();
        final java.lang.Object other$confirmedBy = other.getConfirmedBy();
        if (this$confirmedBy == null ? other$confirmedBy != null : !this$confirmedBy.equals(other$confirmedBy)) return false;
        final java.lang.Object this$confirmTime = this.getConfirmTime();
        final java.lang.Object other$confirmTime = other.getConfirmTime();
        if (this$confirmTime == null ? other$confirmTime != null : !this$confirmTime.equals(other$confirmTime)) return false;
        final java.lang.Object this$rollbackSql = this.getRollbackSql();
        final java.lang.Object other$rollbackSql = other.getRollbackSql();
        if (this$rollbackSql == null ? other$rollbackSql != null : !this$rollbackSql.equals(other$rollbackSql)) return false;
        final java.lang.Object this$createUsername = this.getCreateUsername();
        final java.lang.Object other$createUsername = other.getCreateUsername();
        if (this$createUsername == null ? other$createUsername != null : !this$createUsername.equals(other$createUsername)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$finishTime = this.getFinishTime();
        final java.lang.Object other$finishTime = other.getFinishTime();
        if (this$finishTime == null ? other$finishTime != null : !this$finishTime.equals(other$finishTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BackupRestoreLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $logId = this.getLogId();
        result = result * PRIME + ($logId == null ? 43 : $logId.hashCode());
        final java.lang.Object $backupId = this.getBackupId();
        result = result * PRIME + ($backupId == null ? 43 : $backupId.hashCode());
        final java.lang.Object $restoreStatus = this.getRestoreStatus();
        result = result * PRIME + ($restoreStatus == null ? 43 : $restoreStatus.hashCode());
        final java.lang.Object $restoreMode = this.getRestoreMode();
        result = result * PRIME + ($restoreMode == null ? 43 : $restoreMode.hashCode());
        final java.lang.Object $durationMs = this.getDurationMs();
        result = result * PRIME + ($durationMs == null ? 43 : $durationMs.hashCode());
        final java.lang.Object $createUserId = this.getCreateUserId();
        result = result * PRIME + ($createUserId == null ? 43 : $createUserId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $backupName = this.getBackupName();
        result = result * PRIME + ($backupName == null ? 43 : $backupName.hashCode());
        final java.lang.Object $targetTables = this.getTargetTables();
        result = result * PRIME + ($targetTables == null ? 43 : $targetTables.hashCode());
        final java.lang.Object $preRestoreSnapshot = this.getPreRestoreSnapshot();
        result = result * PRIME + ($preRestoreSnapshot == null ? 43 : $preRestoreSnapshot.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $confirmedBy = this.getConfirmedBy();
        result = result * PRIME + ($confirmedBy == null ? 43 : $confirmedBy.hashCode());
        final java.lang.Object $confirmTime = this.getConfirmTime();
        result = result * PRIME + ($confirmTime == null ? 43 : $confirmTime.hashCode());
        final java.lang.Object $rollbackSql = this.getRollbackSql();
        result = result * PRIME + ($rollbackSql == null ? 43 : $rollbackSql.hashCode());
        final java.lang.Object $createUsername = this.getCreateUsername();
        result = result * PRIME + ($createUsername == null ? 43 : $createUsername.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $finishTime = this.getFinishTime();
        result = result * PRIME + ($finishTime == null ? 43 : $finishTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BackupRestoreLog(logId=" + this.getLogId() + ", backupId=" + this.getBackupId() + ", backupName=" + this.getBackupName() + ", restoreStatus=" + this.getRestoreStatus() + ", restoreMode=" + this.getRestoreMode() + ", targetTables=" + this.getTargetTables() + ", preRestoreSnapshot=" + this.getPreRestoreSnapshot() + ", errorMessage=" + this.getErrorMessage() + ", durationMs=" + this.getDurationMs() + ", confirmedBy=" + this.getConfirmedBy() + ", confirmTime=" + this.getConfirmTime() + ", rollbackSql=" + this.getRollbackSql() + ", createUserId=" + this.getCreateUserId() + ", createUsername=" + this.getCreateUsername() + ", createTime=" + this.getCreateTime() + ", finishTime=" + this.getFinishTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
