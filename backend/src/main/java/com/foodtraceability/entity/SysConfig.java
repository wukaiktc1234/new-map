package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("sys_config")
@Schema(description = "系统配置实体")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Long configId;
    @TableField("config_key")
    @Schema(description = "配置键(唯一)")
    private String configKey;
    @TableField("config_name")
    @Schema(description = "配置名称")
    private String configName;
    @TableField("config_group")
    @Schema(description = "配置分组")
    private String configGroup;
    @TableField("config_category")
    @Schema(description = "配置子分类")
    private String configCategory;
    @TableField("value_type")
    @Schema(description = "值类型: STRING/INTEGER/LONG/DOUBLE/BOOLEAN/JSON/TEXT")
    private String valueType;
    @TableField("config_value")
    @Schema(description = "配置值")
    private String configValue;
    @TableField("default_value")
    @Schema(description = "默认值")
    private String defaultValue;
    @TableField("is_sensitive")
    @Schema(description = "是否敏感")
    private Integer isSensitive;
    @TableField("is_encrypted")
    @Schema(description = "是否加密存储")
    private Integer isEncrypted;
    @TableField("is_enabled")
    @Schema(description = "是否启用")
    private Integer isEnabled;
    @TableField("is_readonly")
    @Schema(description = "是否只读")
    private Integer isReadonly;
    @TableField("sort_order")
    @Schema(description = "排序序号")
    private Integer sortOrder;
    @TableField("description")
    @Schema(description = "配置描述")
    private String description;
    @TableField("validation_rules")
    @Schema(description = "校验规则")
    private String validationRules;
    @TableField("options")
    @Schema(description = "可选值列表")
    private String options;
    @TableField("create_user_id")
    @Schema(description = "创建用户ID")
    private String createUserId;
    @TableField("create_username")
    @Schema(description = "创建用户名")
    private String createUsername;
    @TableField("update_user_id")
    @Schema(description = "更新用户ID")
    private String updateUserId;
    @TableField("update_username")
    @Schema(description = "更新用户名")
    private String updateUsername;
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Integer version;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;

    public SysConfig() {
    }

    public Long getConfigId() {
        return this.configId;
    }

    public String getConfigKey() {
        return this.configKey;
    }

    public String getConfigName() {
        return this.configName;
    }

    public String getConfigGroup() {
        return this.configGroup;
    }

    public String getConfigCategory() {
        return this.configCategory;
    }

    public String getValueType() {
        return this.valueType;
    }

    public String getConfigValue() {
        return this.configValue;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public Integer getIsSensitive() {
        return this.isSensitive;
    }

    public Integer getIsEncrypted() {
        return this.isEncrypted;
    }

    public Integer getIsEnabled() {
        return this.isEnabled;
    }

    public Integer getIsReadonly() {
        return this.isReadonly;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getDescription() {
        return this.description;
    }

    public String getValidationRules() {
        return this.validationRules;
    }

    public String getOptions() {
        return this.options;
    }

    public String getCreateUserId() {
        return this.createUserId;
    }

    public String getCreateUsername() {
        return this.createUsername;
    }

    public String getUpdateUserId() {
        return this.updateUserId;
    }

    public String getUpdateUsername() {
        return this.updateUsername;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }

    public void setConfigId(final Long configId) {
        this.configId = configId;
    }

    public void setConfigKey(final String configKey) {
        this.configKey = configKey;
    }

    public void setConfigName(final String configName) {
        this.configName = configName;
    }

    public void setConfigGroup(final String configGroup) {
        this.configGroup = configGroup;
    }

    public void setConfigCategory(final String configCategory) {
        this.configCategory = configCategory;
    }

    public void setValueType(final String valueType) {
        this.valueType = valueType;
    }

    public void setConfigValue(final String configValue) {
        this.configValue = configValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setIsSensitive(final Integer isSensitive) {
        this.isSensitive = isSensitive;
    }

    public void setIsEncrypted(final Integer isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public void setIsEnabled(final Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setIsReadonly(final Integer isReadonly) {
        this.isReadonly = isReadonly;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setValidationRules(final String validationRules) {
        this.validationRules = validationRules;
    }

    public void setOptions(final String options) {
        this.options = options;
    }

    public void setCreateUserId(final String createUserId) {
        this.createUserId = createUserId;
    }

    public void setCreateUsername(final String createUsername) {
        this.createUsername = createUsername;
    }

    public void setUpdateUserId(final String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public void setUpdateUsername(final String updateUsername) {
        this.updateUsername = updateUsername;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeletedAt(final LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SysConfig)) return false;
        final SysConfig other = (SysConfig) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$configId = this.getConfigId();
        final java.lang.Object other$configId = other.getConfigId();
        if (this$configId == null ? other$configId != null : !this$configId.equals(other$configId)) return false;
        final java.lang.Object this$isSensitive = this.getIsSensitive();
        final java.lang.Object other$isSensitive = other.getIsSensitive();
        if (this$isSensitive == null ? other$isSensitive != null : !this$isSensitive.equals(other$isSensitive)) return false;
        final java.lang.Object this$isEncrypted = this.getIsEncrypted();
        final java.lang.Object other$isEncrypted = other.getIsEncrypted();
        if (this$isEncrypted == null ? other$isEncrypted != null : !this$isEncrypted.equals(other$isEncrypted)) return false;
        final java.lang.Object this$isEnabled = this.getIsEnabled();
        final java.lang.Object other$isEnabled = other.getIsEnabled();
        if (this$isEnabled == null ? other$isEnabled != null : !this$isEnabled.equals(other$isEnabled)) return false;
        final java.lang.Object this$isReadonly = this.getIsReadonly();
        final java.lang.Object other$isReadonly = other.getIsReadonly();
        if (this$isReadonly == null ? other$isReadonly != null : !this$isReadonly.equals(other$isReadonly)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$configKey = this.getConfigKey();
        final java.lang.Object other$configKey = other.getConfigKey();
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey)) return false;
        final java.lang.Object this$configName = this.getConfigName();
        final java.lang.Object other$configName = other.getConfigName();
        if (this$configName == null ? other$configName != null : !this$configName.equals(other$configName)) return false;
        final java.lang.Object this$configGroup = this.getConfigGroup();
        final java.lang.Object other$configGroup = other.getConfigGroup();
        if (this$configGroup == null ? other$configGroup != null : !this$configGroup.equals(other$configGroup)) return false;
        final java.lang.Object this$configCategory = this.getConfigCategory();
        final java.lang.Object other$configCategory = other.getConfigCategory();
        if (this$configCategory == null ? other$configCategory != null : !this$configCategory.equals(other$configCategory)) return false;
        final java.lang.Object this$valueType = this.getValueType();
        final java.lang.Object other$valueType = other.getValueType();
        if (this$valueType == null ? other$valueType != null : !this$valueType.equals(other$valueType)) return false;
        final java.lang.Object this$configValue = this.getConfigValue();
        final java.lang.Object other$configValue = other.getConfigValue();
        if (this$configValue == null ? other$configValue != null : !this$configValue.equals(other$configValue)) return false;
        final java.lang.Object this$defaultValue = this.getDefaultValue();
        final java.lang.Object other$defaultValue = other.getDefaultValue();
        if (this$defaultValue == null ? other$defaultValue != null : !this$defaultValue.equals(other$defaultValue)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$validationRules = this.getValidationRules();
        final java.lang.Object other$validationRules = other.getValidationRules();
        if (this$validationRules == null ? other$validationRules != null : !this$validationRules.equals(other$validationRules)) return false;
        final java.lang.Object this$options = this.getOptions();
        final java.lang.Object other$options = other.getOptions();
        if (this$options == null ? other$options != null : !this$options.equals(other$options)) return false;
        final java.lang.Object this$createUserId = this.getCreateUserId();
        final java.lang.Object other$createUserId = other.getCreateUserId();
        if (this$createUserId == null ? other$createUserId != null : !this$createUserId.equals(other$createUserId)) return false;
        final java.lang.Object this$createUsername = this.getCreateUsername();
        final java.lang.Object other$createUsername = other.getCreateUsername();
        if (this$createUsername == null ? other$createUsername != null : !this$createUsername.equals(other$createUsername)) return false;
        final java.lang.Object this$updateUserId = this.getUpdateUserId();
        final java.lang.Object other$updateUserId = other.getUpdateUserId();
        if (this$updateUserId == null ? other$updateUserId != null : !this$updateUserId.equals(other$updateUserId)) return false;
        final java.lang.Object this$updateUsername = this.getUpdateUsername();
        final java.lang.Object other$updateUsername = other.getUpdateUsername();
        if (this$updateUsername == null ? other$updateUsername != null : !this$updateUsername.equals(other$updateUsername)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$deletedAt = this.getDeletedAt();
        final java.lang.Object other$deletedAt = other.getDeletedAt();
        if (this$deletedAt == null ? other$deletedAt != null : !this$deletedAt.equals(other$deletedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysConfig;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $configId = this.getConfigId();
        result = result * PRIME + ($configId == null ? 43 : $configId.hashCode());
        final java.lang.Object $isSensitive = this.getIsSensitive();
        result = result * PRIME + ($isSensitive == null ? 43 : $isSensitive.hashCode());
        final java.lang.Object $isEncrypted = this.getIsEncrypted();
        result = result * PRIME + ($isEncrypted == null ? 43 : $isEncrypted.hashCode());
        final java.lang.Object $isEnabled = this.getIsEnabled();
        result = result * PRIME + ($isEnabled == null ? 43 : $isEnabled.hashCode());
        final java.lang.Object $isReadonly = this.getIsReadonly();
        result = result * PRIME + ($isReadonly == null ? 43 : $isReadonly.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $configKey = this.getConfigKey();
        result = result * PRIME + ($configKey == null ? 43 : $configKey.hashCode());
        final java.lang.Object $configName = this.getConfigName();
        result = result * PRIME + ($configName == null ? 43 : $configName.hashCode());
        final java.lang.Object $configGroup = this.getConfigGroup();
        result = result * PRIME + ($configGroup == null ? 43 : $configGroup.hashCode());
        final java.lang.Object $configCategory = this.getConfigCategory();
        result = result * PRIME + ($configCategory == null ? 43 : $configCategory.hashCode());
        final java.lang.Object $valueType = this.getValueType();
        result = result * PRIME + ($valueType == null ? 43 : $valueType.hashCode());
        final java.lang.Object $configValue = this.getConfigValue();
        result = result * PRIME + ($configValue == null ? 43 : $configValue.hashCode());
        final java.lang.Object $defaultValue = this.getDefaultValue();
        result = result * PRIME + ($defaultValue == null ? 43 : $defaultValue.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $validationRules = this.getValidationRules();
        result = result * PRIME + ($validationRules == null ? 43 : $validationRules.hashCode());
        final java.lang.Object $options = this.getOptions();
        result = result * PRIME + ($options == null ? 43 : $options.hashCode());
        final java.lang.Object $createUserId = this.getCreateUserId();
        result = result * PRIME + ($createUserId == null ? 43 : $createUserId.hashCode());
        final java.lang.Object $createUsername = this.getCreateUsername();
        result = result * PRIME + ($createUsername == null ? 43 : $createUsername.hashCode());
        final java.lang.Object $updateUserId = this.getUpdateUserId();
        result = result * PRIME + ($updateUserId == null ? 43 : $updateUserId.hashCode());
        final java.lang.Object $updateUsername = this.getUpdateUsername();
        result = result * PRIME + ($updateUsername == null ? 43 : $updateUsername.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $deletedAt = this.getDeletedAt();
        result = result * PRIME + ($deletedAt == null ? 43 : $deletedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysConfig(configId=" + this.getConfigId() + ", configKey=" + this.getConfigKey() + ", configName=" + this.getConfigName() + ", configGroup=" + this.getConfigGroup() + ", configCategory=" + this.getConfigCategory() + ", valueType=" + this.getValueType() + ", configValue=" + this.getConfigValue() + ", defaultValue=" + this.getDefaultValue() + ", isSensitive=" + this.getIsSensitive() + ", isEncrypted=" + this.getIsEncrypted() + ", isEnabled=" + this.getIsEnabled() + ", isReadonly=" + this.getIsReadonly() + ", sortOrder=" + this.getSortOrder() + ", description=" + this.getDescription() + ", validationRules=" + this.getValidationRules() + ", options=" + this.getOptions() + ", createUserId=" + this.getCreateUserId() + ", createUsername=" + this.getCreateUsername() + ", updateUserId=" + this.getUpdateUserId() + ", updateUsername=" + this.getUpdateUsername() + ", version=" + this.getVersion() + ", deleted=" + this.getDeleted() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deletedAt=" + this.getDeletedAt() + ")";
    }
}
