package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 系统角色实体类
 */
@TableName("sys_roles")
@Schema(description = "系统角色表")
public class SysRole {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "角色ID")
    private Long id;
    @TableField("role_code")
    @Schema(description = "角色编码")
    private String roleCode;
    @TableField("role_name")
    @Schema(description = "角色名称")
    private String roleName;
    @TableField("role_desc")
    @Schema(description = "角色描述")
    private String roleDesc;
    @TableField("data_scope")
    @Schema(description = "数据范围：all-全部/department-部门/store-门店/self-本人/custom-自定义")
    private String dataScope;
    @TableField("custom_data_scope")
    @Schema(description = "自定义数据范围配置（JSON格式）")
    private String customDataScope;
    @TableField("is_system")
    @Schema(description = "是否系统角色：0-否，1-是")
    private Integer isSystem;
    @TableField("is_enabled")
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer isEnabled;
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public SysRole() {
    }

    public Long getId() {
        return this.id;
    }

    public String getRoleCode() {
        return this.roleCode;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public String getRoleDesc() {
        return this.roleDesc;
    }

    public String getDataScope() {
        return this.dataScope;
    }

    public String getCustomDataScope() {
        return this.customDataScope;
    }

    public Integer getIsSystem() {
        return this.isSystem;
    }

    public Integer getIsEnabled() {
        return this.isEnabled;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setRoleCode(final String roleCode) {
        this.roleCode = roleCode;
    }

    public void setRoleName(final String roleName) {
        this.roleName = roleName;
    }

    public void setRoleDesc(final String roleDesc) {
        this.roleDesc = roleDesc;
    }

    public void setDataScope(final String dataScope) {
        this.dataScope = dataScope;
    }

    public void setCustomDataScope(final String customDataScope) {
        this.customDataScope = customDataScope;
    }

    public void setIsSystem(final Integer isSystem) {
        this.isSystem = isSystem;
    }

    public void setIsEnabled(final Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
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
        if (!(o instanceof SysRole)) return false;
        final SysRole other = (SysRole) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$isSystem = this.getIsSystem();
        final java.lang.Object other$isSystem = other.getIsSystem();
        if (this$isSystem == null ? other$isSystem != null : !this$isSystem.equals(other$isSystem)) return false;
        final java.lang.Object this$isEnabled = this.getIsEnabled();
        final java.lang.Object other$isEnabled = other.getIsEnabled();
        if (this$isEnabled == null ? other$isEnabled != null : !this$isEnabled.equals(other$isEnabled)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$roleCode = this.getRoleCode();
        final java.lang.Object other$roleCode = other.getRoleCode();
        if (this$roleCode == null ? other$roleCode != null : !this$roleCode.equals(other$roleCode)) return false;
        final java.lang.Object this$roleName = this.getRoleName();
        final java.lang.Object other$roleName = other.getRoleName();
        if (this$roleName == null ? other$roleName != null : !this$roleName.equals(other$roleName)) return false;
        final java.lang.Object this$roleDesc = this.getRoleDesc();
        final java.lang.Object other$roleDesc = other.getRoleDesc();
        if (this$roleDesc == null ? other$roleDesc != null : !this$roleDesc.equals(other$roleDesc)) return false;
        final java.lang.Object this$dataScope = this.getDataScope();
        final java.lang.Object other$dataScope = other.getDataScope();
        if (this$dataScope == null ? other$dataScope != null : !this$dataScope.equals(other$dataScope)) return false;
        final java.lang.Object this$customDataScope = this.getCustomDataScope();
        final java.lang.Object other$customDataScope = other.getCustomDataScope();
        if (this$customDataScope == null ? other$customDataScope != null : !this$customDataScope.equals(other$customDataScope)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysRole;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $isSystem = this.getIsSystem();
        result = result * PRIME + ($isSystem == null ? 43 : $isSystem.hashCode());
        final java.lang.Object $isEnabled = this.getIsEnabled();
        result = result * PRIME + ($isEnabled == null ? 43 : $isEnabled.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $roleCode = this.getRoleCode();
        result = result * PRIME + ($roleCode == null ? 43 : $roleCode.hashCode());
        final java.lang.Object $roleName = this.getRoleName();
        result = result * PRIME + ($roleName == null ? 43 : $roleName.hashCode());
        final java.lang.Object $roleDesc = this.getRoleDesc();
        result = result * PRIME + ($roleDesc == null ? 43 : $roleDesc.hashCode());
        final java.lang.Object $dataScope = this.getDataScope();
        result = result * PRIME + ($dataScope == null ? 43 : $dataScope.hashCode());
        final java.lang.Object $customDataScope = this.getCustomDataScope();
        result = result * PRIME + ($customDataScope == null ? 43 : $customDataScope.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysRole(id=" + this.getId() + ", roleCode=" + this.getRoleCode() + ", roleName=" + this.getRoleName() + ", roleDesc=" + this.getRoleDesc() + ", dataScope=" + this.getDataScope() + ", customDataScope=" + this.getCustomDataScope() + ", isSystem=" + this.getIsSystem() + ", isEnabled=" + this.getIsEnabled() + ", sortOrder=" + this.getSortOrder() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
