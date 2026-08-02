package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 系统设置实体类
 */
@TableName("sys_settings")
@Schema(description = "系统设置表")
public class SysSetting {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "设置ID")
    private Long id;
    @TableField("setting_group")
    @Schema(description = "设置分组")
    private String settingGroup;
    @TableField("setting_key")
    @Schema(description = "设置键")
    private String settingKey;
    @TableField("scope_type")
    @Schema(description = "作用域：global-全局/store-门店/user-用户")
    private String scopeType;
    @TableField("scope_id")
    @Schema(description = "作用域ID")
    private String scopeId;
    @TableField("setting_value")
    @Schema(description = "设置值")
    private String settingValue;
    @TableField("value_type")
    @Schema(description = "值类型：string/number/boolean/select/multiselect/json/array/color/image/file")
    private String valueType;
    @TableField("setting_name")
    @Schema(description = "设置名称")
    private String settingName;
    @TableField("setting_desc")
    @Schema(description = "设置描述")
    private String settingDesc;
    @TableField("options")
    @Schema(description = "选项列表（JSON格式）")
    private String options;
    @TableField("validation_rules")
    @Schema(description = "验证规则（JSON格式）")
    private String validationRules;
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
    @TableField("required_permission")
    @Schema(description = "所需权限")
    private String requiredPermission;
    @TableField("is_enabled")
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer isEnabled;
    @TableField("is_system")
    @Schema(description = "是否系统配置：0-否，1-是")
    private Integer isSystem;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableField("created_by")
    @Schema(description = "创建人")
    private String createdBy;
    @TableField("updated_by")
    @Schema(description = "更新人")
    private String updatedBy;

    public SysSetting() {
    }

    public Long getId() {
        return this.id;
    }

    public String getSettingGroup() {
        return this.settingGroup;
    }

    public String getSettingKey() {
        return this.settingKey;
    }

    public String getScopeType() {
        return this.scopeType;
    }

    public String getScopeId() {
        return this.scopeId;
    }

    public String getSettingValue() {
        return this.settingValue;
    }

    public String getValueType() {
        return this.valueType;
    }

    public String getSettingName() {
        return this.settingName;
    }

    public String getSettingDesc() {
        return this.settingDesc;
    }

    public String getOptions() {
        return this.options;
    }

    public String getValidationRules() {
        return this.validationRules;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getRequiredPermission() {
        return this.requiredPermission;
    }

    public Integer getIsEnabled() {
        return this.isEnabled;
    }

    public Integer getIsSystem() {
        return this.isSystem;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setSettingGroup(final String settingGroup) {
        this.settingGroup = settingGroup;
    }

    public void setSettingKey(final String settingKey) {
        this.settingKey = settingKey;
    }

    public void setScopeType(final String scopeType) {
        this.scopeType = scopeType;
    }

    public void setScopeId(final String scopeId) {
        this.scopeId = scopeId;
    }

    public void setSettingValue(final String settingValue) {
        this.settingValue = settingValue;
    }

    public void setValueType(final String valueType) {
        this.valueType = valueType;
    }

    public void setSettingName(final String settingName) {
        this.settingName = settingName;
    }

    public void setSettingDesc(final String settingDesc) {
        this.settingDesc = settingDesc;
    }

    public void setOptions(final String options) {
        this.options = options;
    }

    public void setValidationRules(final String validationRules) {
        this.validationRules = validationRules;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setRequiredPermission(final String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public void setIsEnabled(final Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setIsSystem(final Integer isSystem) {
        this.isSystem = isSystem;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SysSetting)) return false;
        final SysSetting other = (SysSetting) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$isEnabled = this.getIsEnabled();
        final java.lang.Object other$isEnabled = other.getIsEnabled();
        if (this$isEnabled == null ? other$isEnabled != null : !this$isEnabled.equals(other$isEnabled)) return false;
        final java.lang.Object this$isSystem = this.getIsSystem();
        final java.lang.Object other$isSystem = other.getIsSystem();
        if (this$isSystem == null ? other$isSystem != null : !this$isSystem.equals(other$isSystem)) return false;
        final java.lang.Object this$settingGroup = this.getSettingGroup();
        final java.lang.Object other$settingGroup = other.getSettingGroup();
        if (this$settingGroup == null ? other$settingGroup != null : !this$settingGroup.equals(other$settingGroup)) return false;
        final java.lang.Object this$settingKey = this.getSettingKey();
        final java.lang.Object other$settingKey = other.getSettingKey();
        if (this$settingKey == null ? other$settingKey != null : !this$settingKey.equals(other$settingKey)) return false;
        final java.lang.Object this$scopeType = this.getScopeType();
        final java.lang.Object other$scopeType = other.getScopeType();
        if (this$scopeType == null ? other$scopeType != null : !this$scopeType.equals(other$scopeType)) return false;
        final java.lang.Object this$scopeId = this.getScopeId();
        final java.lang.Object other$scopeId = other.getScopeId();
        if (this$scopeId == null ? other$scopeId != null : !this$scopeId.equals(other$scopeId)) return false;
        final java.lang.Object this$settingValue = this.getSettingValue();
        final java.lang.Object other$settingValue = other.getSettingValue();
        if (this$settingValue == null ? other$settingValue != null : !this$settingValue.equals(other$settingValue)) return false;
        final java.lang.Object this$valueType = this.getValueType();
        final java.lang.Object other$valueType = other.getValueType();
        if (this$valueType == null ? other$valueType != null : !this$valueType.equals(other$valueType)) return false;
        final java.lang.Object this$settingName = this.getSettingName();
        final java.lang.Object other$settingName = other.getSettingName();
        if (this$settingName == null ? other$settingName != null : !this$settingName.equals(other$settingName)) return false;
        final java.lang.Object this$settingDesc = this.getSettingDesc();
        final java.lang.Object other$settingDesc = other.getSettingDesc();
        if (this$settingDesc == null ? other$settingDesc != null : !this$settingDesc.equals(other$settingDesc)) return false;
        final java.lang.Object this$options = this.getOptions();
        final java.lang.Object other$options = other.getOptions();
        if (this$options == null ? other$options != null : !this$options.equals(other$options)) return false;
        final java.lang.Object this$validationRules = this.getValidationRules();
        final java.lang.Object other$validationRules = other.getValidationRules();
        if (this$validationRules == null ? other$validationRules != null : !this$validationRules.equals(other$validationRules)) return false;
        final java.lang.Object this$requiredPermission = this.getRequiredPermission();
        final java.lang.Object other$requiredPermission = other.getRequiredPermission();
        if (this$requiredPermission == null ? other$requiredPermission != null : !this$requiredPermission.equals(other$requiredPermission)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysSetting;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $isEnabled = this.getIsEnabled();
        result = result * PRIME + ($isEnabled == null ? 43 : $isEnabled.hashCode());
        final java.lang.Object $isSystem = this.getIsSystem();
        result = result * PRIME + ($isSystem == null ? 43 : $isSystem.hashCode());
        final java.lang.Object $settingGroup = this.getSettingGroup();
        result = result * PRIME + ($settingGroup == null ? 43 : $settingGroup.hashCode());
        final java.lang.Object $settingKey = this.getSettingKey();
        result = result * PRIME + ($settingKey == null ? 43 : $settingKey.hashCode());
        final java.lang.Object $scopeType = this.getScopeType();
        result = result * PRIME + ($scopeType == null ? 43 : $scopeType.hashCode());
        final java.lang.Object $scopeId = this.getScopeId();
        result = result * PRIME + ($scopeId == null ? 43 : $scopeId.hashCode());
        final java.lang.Object $settingValue = this.getSettingValue();
        result = result * PRIME + ($settingValue == null ? 43 : $settingValue.hashCode());
        final java.lang.Object $valueType = this.getValueType();
        result = result * PRIME + ($valueType == null ? 43 : $valueType.hashCode());
        final java.lang.Object $settingName = this.getSettingName();
        result = result * PRIME + ($settingName == null ? 43 : $settingName.hashCode());
        final java.lang.Object $settingDesc = this.getSettingDesc();
        result = result * PRIME + ($settingDesc == null ? 43 : $settingDesc.hashCode());
        final java.lang.Object $options = this.getOptions();
        result = result * PRIME + ($options == null ? 43 : $options.hashCode());
        final java.lang.Object $validationRules = this.getValidationRules();
        result = result * PRIME + ($validationRules == null ? 43 : $validationRules.hashCode());
        final java.lang.Object $requiredPermission = this.getRequiredPermission();
        result = result * PRIME + ($requiredPermission == null ? 43 : $requiredPermission.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysSetting(id=" + this.getId() + ", settingGroup=" + this.getSettingGroup() + ", settingKey=" + this.getSettingKey() + ", scopeType=" + this.getScopeType() + ", scopeId=" + this.getScopeId() + ", settingValue=" + this.getSettingValue() + ", valueType=" + this.getValueType() + ", settingName=" + this.getSettingName() + ", settingDesc=" + this.getSettingDesc() + ", options=" + this.getOptions() + ", validationRules=" + this.getValidationRules() + ", sortOrder=" + this.getSortOrder() + ", requiredPermission=" + this.getRequiredPermission() + ", isEnabled=" + this.getIsEnabled() + ", isSystem=" + this.getIsSystem() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}
