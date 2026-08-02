package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("sys_config_history")
@Schema(description = "系统配置变更历史实体")
public class SysConfigHistory {
    @TableId(type = IdType.AUTO)
    @Schema(description = "历史记录ID")
    private Long historyId;
    @Schema(description = "关联配置ID")
    private Long configId;
    @Schema(description = "配置键")
    private String configKey;
    @Schema(description = "变更前值")
    private String oldValue;
    @Schema(description = "变更后值")
    private String newValue;
    @Schema(description = "变更类型: CREATE/UPDATE/RESET/ENABLE/DISABLE")
    private String changeType;
    @Schema(description = "变更原因")
    private String changeReason;
    @Schema(description = "操作人ID")
    private String operatorId;
    @Schema(description = "操作人姓名")
    private String operatorName;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public SysConfigHistory() {
    }

    public Long getHistoryId() {
        return this.historyId;
    }

    public Long getConfigId() {
        return this.configId;
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    public String getChangeType() {
        return this.changeType;
    }

    public String getChangeReason() {
        return this.changeReason;
    }

    public String getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setHistoryId(final Long historyId) {
        this.historyId = historyId;
    }

    public void setConfigId(final Long configId) {
        this.configId = configId;
    }

    public void setConfigKey(final String configKey) {
        this.configKey = configKey;
    }

    public void setOldValue(final String oldValue) {
        this.oldValue = oldValue;
    }

    public void setNewValue(final String newValue) {
        this.newValue = newValue;
    }

    public void setChangeType(final String changeType) {
        this.changeType = changeType;
    }

    public void setChangeReason(final String changeReason) {
        this.changeReason = changeReason;
    }

    public void setOperatorId(final String operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SysConfigHistory)) return false;
        final SysConfigHistory other = (SysConfigHistory) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$historyId = this.getHistoryId();
        final java.lang.Object other$historyId = other.getHistoryId();
        if (this$historyId == null ? other$historyId != null : !this$historyId.equals(other$historyId)) return false;
        final java.lang.Object this$configId = this.getConfigId();
        final java.lang.Object other$configId = other.getConfigId();
        if (this$configId == null ? other$configId != null : !this$configId.equals(other$configId)) return false;
        final java.lang.Object this$configKey = this.getConfigKey();
        final java.lang.Object other$configKey = other.getConfigKey();
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey)) return false;
        final java.lang.Object this$oldValue = this.getOldValue();
        final java.lang.Object other$oldValue = other.getOldValue();
        if (this$oldValue == null ? other$oldValue != null : !this$oldValue.equals(other$oldValue)) return false;
        final java.lang.Object this$newValue = this.getNewValue();
        final java.lang.Object other$newValue = other.getNewValue();
        if (this$newValue == null ? other$newValue != null : !this$newValue.equals(other$newValue)) return false;
        final java.lang.Object this$changeType = this.getChangeType();
        final java.lang.Object other$changeType = other.getChangeType();
        if (this$changeType == null ? other$changeType != null : !this$changeType.equals(other$changeType)) return false;
        final java.lang.Object this$changeReason = this.getChangeReason();
        final java.lang.Object other$changeReason = other.getChangeReason();
        if (this$changeReason == null ? other$changeReason != null : !this$changeReason.equals(other$changeReason)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysConfigHistory;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $historyId = this.getHistoryId();
        result = result * PRIME + ($historyId == null ? 43 : $historyId.hashCode());
        final java.lang.Object $configId = this.getConfigId();
        result = result * PRIME + ($configId == null ? 43 : $configId.hashCode());
        final java.lang.Object $configKey = this.getConfigKey();
        result = result * PRIME + ($configKey == null ? 43 : $configKey.hashCode());
        final java.lang.Object $oldValue = this.getOldValue();
        result = result * PRIME + ($oldValue == null ? 43 : $oldValue.hashCode());
        final java.lang.Object $newValue = this.getNewValue();
        result = result * PRIME + ($newValue == null ? 43 : $newValue.hashCode());
        final java.lang.Object $changeType = this.getChangeType();
        result = result * PRIME + ($changeType == null ? 43 : $changeType.hashCode());
        final java.lang.Object $changeReason = this.getChangeReason();
        result = result * PRIME + ($changeReason == null ? 43 : $changeReason.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysConfigHistory(historyId=" + this.getHistoryId() + ", configId=" + this.getConfigId() + ", configKey=" + this.getConfigKey() + ", oldValue=" + this.getOldValue() + ", newValue=" + this.getNewValue() + ", changeType=" + this.getChangeType() + ", changeReason=" + this.getChangeReason() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
