package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@TableName("system_config")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Boolean encrypted;
    private Long createdBy;
    @TableField("create_time")
    private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField("update_time")
    private LocalDateTime updatedAt;
    private Integer deleted;

    public SystemConfig() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public String getConfigValue() {
        return this.configValue;
    }

    public String getConfigType() {
        return this.configType;
    }

    public String getDescription() {
        return this.description;
    }

    public Boolean getEncrypted() {
        return this.encrypted;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setConfigKey(final String configKey) {
        this.configKey = configKey;
    }

    public void setConfigValue(final String configValue) {
        this.configValue = configValue;
    }

    public void setConfigType(final String configType) {
        this.configType = configType;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setEncrypted(final Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedBy(final Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SystemConfig)) return false;
        final SystemConfig other = (SystemConfig) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$encrypted = this.getEncrypted();
        final java.lang.Object other$encrypted = other.getEncrypted();
        if (this$encrypted == null ? other$encrypted != null : !this$encrypted.equals(other$encrypted)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$configKey = this.getConfigKey();
        final java.lang.Object other$configKey = other.getConfigKey();
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey)) return false;
        final java.lang.Object this$configValue = this.getConfigValue();
        final java.lang.Object other$configValue = other.getConfigValue();
        if (this$configValue == null ? other$configValue != null : !this$configValue.equals(other$configValue)) return false;
        final java.lang.Object this$configType = this.getConfigType();
        final java.lang.Object other$configType = other.getConfigType();
        if (this$configType == null ? other$configType != null : !this$configType.equals(other$configType)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SystemConfig;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $encrypted = this.getEncrypted();
        result = result * PRIME + ($encrypted == null ? 43 : $encrypted.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $configKey = this.getConfigKey();
        result = result * PRIME + ($configKey == null ? 43 : $configKey.hashCode());
        final java.lang.Object $configValue = this.getConfigValue();
        result = result * PRIME + ($configValue == null ? 43 : $configValue.hashCode());
        final java.lang.Object $configType = this.getConfigType();
        result = result * PRIME + ($configType == null ? 43 : $configType.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SystemConfig(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", configKey=" + this.getConfigKey() + ", configValue=" + this.getConfigValue() + ", configType=" + this.getConfigType() + ", description=" + this.getDescription() + ", encrypted=" + this.getEncrypted() + ", createdBy=" + this.getCreatedBy() + ", createdAt=" + this.getCreatedAt() + ", updatedBy=" + this.getUpdatedBy() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
