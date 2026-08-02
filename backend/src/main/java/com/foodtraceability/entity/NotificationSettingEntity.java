package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 通知设置实体类
 * 存储SMTP等通知通道的配置信息
 * 敏感信息(如密码)使用Jasypt加密存储
 */
@TableName("notification_setting")
@Schema(description = "通知设置实体")
public class NotificationSettingEntity {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long settingId;
    @TableField("setting_key")
    @Schema(description = "设置键(唯一)")
    private String settingKey;
    @TableField("setting_value")
    @Schema(description = "设置值")
    private String settingValue;
    @TableField("setting_group")
    @Schema(description = "设置分组")
    private String settingGroup;
    @TableField("description")
    @Schema(description = "描述说明")
    private String description;
    /**
     * 是否加密存储: 0=否 1=是
     */
    @TableField("is_encrypted")
    @Schema(description = "是否加密存储:0=否 1=是")
    private Integer isEncrypted;
    /**
     * 状态: 0=DISABLED 1=ENABLED
     */
    @TableField("status")
    @Schema(description = "状态:0=DISABLED 1=ENABLED")
    private Integer status;
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记:0=未删除 1=已删除")
    private Integer deleted;
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public NotificationSettingEntity() {
    }

    public Long getSettingId() {
        return this.settingId;
    }

    public String getSettingKey() {
        return this.settingKey;
    }

    public String getSettingValue() {
        return this.settingValue;
    }

    public String getSettingGroup() {
        return this.settingGroup;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * 是否加密存储: 0=否 1=是
     */
    public Integer getIsEncrypted() {
        return this.isEncrypted;
    }

    /**
     * 状态: 0=DISABLED 1=ENABLED
     */
    public Integer getStatus() {
        return this.status;
    }

    public Long getVersion() {
        return this.version;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setSettingId(final Long settingId) {
        this.settingId = settingId;
    }

    public void setSettingKey(final String settingKey) {
        this.settingKey = settingKey;
    }

    public void setSettingValue(final String settingValue) {
        this.settingValue = settingValue;
    }

    public void setSettingGroup(final String settingGroup) {
        this.settingGroup = settingGroup;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 是否加密存储: 0=否 1=是
     */
    public void setIsEncrypted(final Integer isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    /**
     * 状态: 0=DISABLED 1=ENABLED
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof NotificationSettingEntity)) return false;
        final NotificationSettingEntity other = (NotificationSettingEntity) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$settingId = this.getSettingId();
        final java.lang.Object other$settingId = other.getSettingId();
        if (this$settingId == null ? other$settingId != null : !this$settingId.equals(other$settingId)) return false;
        final java.lang.Object this$isEncrypted = this.getIsEncrypted();
        final java.lang.Object other$isEncrypted = other.getIsEncrypted();
        if (this$isEncrypted == null ? other$isEncrypted != null : !this$isEncrypted.equals(other$isEncrypted)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$settingKey = this.getSettingKey();
        final java.lang.Object other$settingKey = other.getSettingKey();
        if (this$settingKey == null ? other$settingKey != null : !this$settingKey.equals(other$settingKey)) return false;
        final java.lang.Object this$settingValue = this.getSettingValue();
        final java.lang.Object other$settingValue = other.getSettingValue();
        if (this$settingValue == null ? other$settingValue != null : !this$settingValue.equals(other$settingValue)) return false;
        final java.lang.Object this$settingGroup = this.getSettingGroup();
        final java.lang.Object other$settingGroup = other.getSettingGroup();
        if (this$settingGroup == null ? other$settingGroup != null : !this$settingGroup.equals(other$settingGroup)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof NotificationSettingEntity;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $settingId = this.getSettingId();
        result = result * PRIME + ($settingId == null ? 43 : $settingId.hashCode());
        final java.lang.Object $isEncrypted = this.getIsEncrypted();
        result = result * PRIME + ($isEncrypted == null ? 43 : $isEncrypted.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $settingKey = this.getSettingKey();
        result = result * PRIME + ($settingKey == null ? 43 : $settingKey.hashCode());
        final java.lang.Object $settingValue = this.getSettingValue();
        result = result * PRIME + ($settingValue == null ? 43 : $settingValue.hashCode());
        final java.lang.Object $settingGroup = this.getSettingGroup();
        result = result * PRIME + ($settingGroup == null ? 43 : $settingGroup.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "NotificationSettingEntity(settingId=" + this.getSettingId() + ", settingKey=" + this.getSettingKey() + ", settingValue=" + this.getSettingValue() + ", settingGroup=" + this.getSettingGroup() + ", description=" + this.getDescription() + ", isEncrypted=" + this.getIsEncrypted() + ", status=" + this.getStatus() + ", version=" + this.getVersion() + ", deleted=" + this.getDeleted() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
