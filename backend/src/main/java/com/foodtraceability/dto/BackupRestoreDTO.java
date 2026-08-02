package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 数据恢复请求DTO
 * 用于接收数据恢复操作的请求参数
 * 包含二次密码确认字段以确保高爆炸半径操作的安全性
 */
@Schema(description = "数据恢复请求")
public class BackupRestoreDTO {
    /**
     * 关联的备份记录ID（必填）
     */
    @NotNull(message = "备份ID不能为空")
    @Schema(description = "关联的备份记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long backupId;
    /**
     * 恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
     * 默认为全覆盖模式（高风险操作）
     */
    @NotNull(message = "恢复模式不能为空")
    @Min(value = 1, message = "恢复模式值无效")
    @Max(value = 3, message = "恢复模式值无效")
    @Schema(description = "恢复模式: 1=全覆盖 2=选择性 3=预览", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer restoreMode;
    /**
     * 目标表列表（选择性恢复时必填）
     */
    @Schema(description = "目标表列表（选择性恢复时必填）")
    private List<String> targetTables;
    /**
     * 二次确认密码（高爆炸半径操作必须）
     * 用户需要输入当前登录密码以确认操作意图
     */
    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "二次确认密码（用于验证操作者身份）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;
    /**
     * 操作原因说明（建议填写）
     */
    @NotBlank(message = "操作原因不能为空")
    @Schema(description = "操作原因说明", example = "修复数据错误：订单金额计算异常", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
    /**
     * 是否在恢复前创建快照（默认开启）
     */
    @Schema(description = "是否在恢复前创建自动快照", example = "true")
    private Boolean createPreSnapshot = true;
    /**
     * 是否通知所有管理员（默认开启）
     */
    @Schema(description = "是否通知所有管理员", example = "true")
    private Boolean notifyAdmins = true;

    public BackupRestoreDTO() {
    }

    /**
     * 关联的备份记录ID（必填）
     */
    public Long getBackupId() {
        return this.backupId;
    }

    /**
     * 恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
     * 默认为全覆盖模式（高风险操作）
     */
    public Integer getRestoreMode() {
        return this.restoreMode;
    }

    /**
     * 目标表列表（选择性恢复时必填）
     */
    public List<String> getTargetTables() {
        return this.targetTables;
    }

    /**
     * 二次确认密码（高爆炸半径操作必须）
     * 用户需要输入当前登录密码以确认操作意图
     */
    public String getConfirmPassword() {
        return this.confirmPassword;
    }

    /**
     * 操作原因说明（建议填写）
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * 是否在恢复前创建快照（默认开启）
     */
    public Boolean getCreatePreSnapshot() {
        return this.createPreSnapshot;
    }

    /**
     * 是否通知所有管理员（默认开启）
     */
    public Boolean getNotifyAdmins() {
        return this.notifyAdmins;
    }

    /**
     * 关联的备份记录ID（必填）
     */
    public void setBackupId(final Long backupId) {
        this.backupId = backupId;
    }

    /**
     * 恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
     * 默认为全覆盖模式（高风险操作）
     */
    public void setRestoreMode(final Integer restoreMode) {
        this.restoreMode = restoreMode;
    }

    /**
     * 目标表列表（选择性恢复时必填）
     */
    public void setTargetTables(final List<String> targetTables) {
        this.targetTables = targetTables;
    }

    /**
     * 二次确认密码（高爆炸半径操作必须）
     * 用户需要输入当前登录密码以确认操作意图
     */
    public void setConfirmPassword(final String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    /**
     * 操作原因说明（建议填写）
     */
    public void setReason(final String reason) {
        this.reason = reason;
    }

    /**
     * 是否在恢复前创建快照（默认开启）
     */
    public void setCreatePreSnapshot(final Boolean createPreSnapshot) {
        this.createPreSnapshot = createPreSnapshot;
    }

    /**
     * 是否通知所有管理员（默认开启）
     */
    public void setNotifyAdmins(final Boolean notifyAdmins) {
        this.notifyAdmins = notifyAdmins;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BackupRestoreDTO)) return false;
        final BackupRestoreDTO other = (BackupRestoreDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$backupId = this.getBackupId();
        final java.lang.Object other$backupId = other.getBackupId();
        if (this$backupId == null ? other$backupId != null : !this$backupId.equals(other$backupId)) return false;
        final java.lang.Object this$restoreMode = this.getRestoreMode();
        final java.lang.Object other$restoreMode = other.getRestoreMode();
        if (this$restoreMode == null ? other$restoreMode != null : !this$restoreMode.equals(other$restoreMode)) return false;
        final java.lang.Object this$createPreSnapshot = this.getCreatePreSnapshot();
        final java.lang.Object other$createPreSnapshot = other.getCreatePreSnapshot();
        if (this$createPreSnapshot == null ? other$createPreSnapshot != null : !this$createPreSnapshot.equals(other$createPreSnapshot)) return false;
        final java.lang.Object this$notifyAdmins = this.getNotifyAdmins();
        final java.lang.Object other$notifyAdmins = other.getNotifyAdmins();
        if (this$notifyAdmins == null ? other$notifyAdmins != null : !this$notifyAdmins.equals(other$notifyAdmins)) return false;
        final java.lang.Object this$targetTables = this.getTargetTables();
        final java.lang.Object other$targetTables = other.getTargetTables();
        if (this$targetTables == null ? other$targetTables != null : !this$targetTables.equals(other$targetTables)) return false;
        final java.lang.Object this$confirmPassword = this.getConfirmPassword();
        final java.lang.Object other$confirmPassword = other.getConfirmPassword();
        if (this$confirmPassword == null ? other$confirmPassword != null : !this$confirmPassword.equals(other$confirmPassword)) return false;
        final java.lang.Object this$reason = this.getReason();
        final java.lang.Object other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BackupRestoreDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $backupId = this.getBackupId();
        result = result * PRIME + ($backupId == null ? 43 : $backupId.hashCode());
        final java.lang.Object $restoreMode = this.getRestoreMode();
        result = result * PRIME + ($restoreMode == null ? 43 : $restoreMode.hashCode());
        final java.lang.Object $createPreSnapshot = this.getCreatePreSnapshot();
        result = result * PRIME + ($createPreSnapshot == null ? 43 : $createPreSnapshot.hashCode());
        final java.lang.Object $notifyAdmins = this.getNotifyAdmins();
        result = result * PRIME + ($notifyAdmins == null ? 43 : $notifyAdmins.hashCode());
        final java.lang.Object $targetTables = this.getTargetTables();
        result = result * PRIME + ($targetTables == null ? 43 : $targetTables.hashCode());
        final java.lang.Object $confirmPassword = this.getConfirmPassword();
        result = result * PRIME + ($confirmPassword == null ? 43 : $confirmPassword.hashCode());
        final java.lang.Object $reason = this.getReason();
        result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BackupRestoreDTO(backupId=" + this.getBackupId() + ", restoreMode=" + this.getRestoreMode() + ", targetTables=" + this.getTargetTables() + ", confirmPassword=" + this.getConfirmPassword() + ", reason=" + this.getReason() + ", createPreSnapshot=" + this.getCreatePreSnapshot() + ", notifyAdmins=" + this.getNotifyAdmins() + ")";
    }
}
