package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 系统权限实体类
 */
@TableName("sys_permissions")
@Schema(description = "系统权限表")
public class SysPermission {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "权限ID")
    private Long id;
    @TableField("perm_code")
    @Schema(description = "权限编码：module:resource:action")
    private String permCode;
    @TableField("perm_name")
    @Schema(description = "权限名称")
    private String permName;
    @TableField("perm_type")
    @Schema(description = "权限类型：menu-菜单/button-按钮/api-接口/data-数据")
    private String permType;
    @TableField("module_code")
    @Schema(description = "模块编码")
    private String moduleCode;
    @TableField("module_name")
    @Schema(description = "模块名称")
    private String moduleName;
    @TableField("parent_id")
    @Schema(description = "父权限ID")
    private Long parentId;
    @TableField("route_path")
    @Schema(description = "路由路径（菜单权限）")
    private String routePath;
    @TableField("component")
    @Schema(description = "组件路径（菜单权限）")
    private String component;
    @TableField("icon")
    @Schema(description = "图标（菜单权限）")
    private String icon;
    @TableField("sort_order")
    @Schema(description = "排序")
    private Integer sortOrder;
    @TableField("is_enabled")
    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer isEnabled;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public SysPermission() {
    }

    public Long getId() {
        return this.id;
    }

    public String getPermCode() {
        return this.permCode;
    }

    public String getPermName() {
        return this.permName;
    }

    public String getPermType() {
        return this.permType;
    }

    public String getModuleCode() {
        return this.moduleCode;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public String getRoutePath() {
        return this.routePath;
    }

    public String getComponent() {
        return this.component;
    }

    public String getIcon() {
        return this.icon;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Integer getIsEnabled() {
        return this.isEnabled;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setPermCode(final String permCode) {
        this.permCode = permCode;
    }

    public void setPermName(final String permName) {
        this.permName = permName;
    }

    public void setPermType(final String permType) {
        this.permType = permType;
    }

    public void setModuleCode(final String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public void setRoutePath(final String routePath) {
        this.routePath = routePath;
    }

    public void setComponent(final String component) {
        this.component = component;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setIsEnabled(final Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SysPermission)) return false;
        final SysPermission other = (SysPermission) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$isEnabled = this.getIsEnabled();
        final java.lang.Object other$isEnabled = other.getIsEnabled();
        if (this$isEnabled == null ? other$isEnabled != null : !this$isEnabled.equals(other$isEnabled)) return false;
        final java.lang.Object this$permCode = this.getPermCode();
        final java.lang.Object other$permCode = other.getPermCode();
        if (this$permCode == null ? other$permCode != null : !this$permCode.equals(other$permCode)) return false;
        final java.lang.Object this$permName = this.getPermName();
        final java.lang.Object other$permName = other.getPermName();
        if (this$permName == null ? other$permName != null : !this$permName.equals(other$permName)) return false;
        final java.lang.Object this$permType = this.getPermType();
        final java.lang.Object other$permType = other.getPermType();
        if (this$permType == null ? other$permType != null : !this$permType.equals(other$permType)) return false;
        final java.lang.Object this$moduleCode = this.getModuleCode();
        final java.lang.Object other$moduleCode = other.getModuleCode();
        if (this$moduleCode == null ? other$moduleCode != null : !this$moduleCode.equals(other$moduleCode)) return false;
        final java.lang.Object this$moduleName = this.getModuleName();
        final java.lang.Object other$moduleName = other.getModuleName();
        if (this$moduleName == null ? other$moduleName != null : !this$moduleName.equals(other$moduleName)) return false;
        final java.lang.Object this$routePath = this.getRoutePath();
        final java.lang.Object other$routePath = other.getRoutePath();
        if (this$routePath == null ? other$routePath != null : !this$routePath.equals(other$routePath)) return false;
        final java.lang.Object this$component = this.getComponent();
        final java.lang.Object other$component = other.getComponent();
        if (this$component == null ? other$component != null : !this$component.equals(other$component)) return false;
        final java.lang.Object this$icon = this.getIcon();
        final java.lang.Object other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysPermission;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $isEnabled = this.getIsEnabled();
        result = result * PRIME + ($isEnabled == null ? 43 : $isEnabled.hashCode());
        final java.lang.Object $permCode = this.getPermCode();
        result = result * PRIME + ($permCode == null ? 43 : $permCode.hashCode());
        final java.lang.Object $permName = this.getPermName();
        result = result * PRIME + ($permName == null ? 43 : $permName.hashCode());
        final java.lang.Object $permType = this.getPermType();
        result = result * PRIME + ($permType == null ? 43 : $permType.hashCode());
        final java.lang.Object $moduleCode = this.getModuleCode();
        result = result * PRIME + ($moduleCode == null ? 43 : $moduleCode.hashCode());
        final java.lang.Object $moduleName = this.getModuleName();
        result = result * PRIME + ($moduleName == null ? 43 : $moduleName.hashCode());
        final java.lang.Object $routePath = this.getRoutePath();
        result = result * PRIME + ($routePath == null ? 43 : $routePath.hashCode());
        final java.lang.Object $component = this.getComponent();
        result = result * PRIME + ($component == null ? 43 : $component.hashCode());
        final java.lang.Object $icon = this.getIcon();
        result = result * PRIME + ($icon == null ? 43 : $icon.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysPermission(id=" + this.getId() + ", permCode=" + this.getPermCode() + ", permName=" + this.getPermName() + ", permType=" + this.getPermType() + ", moduleCode=" + this.getModuleCode() + ", moduleName=" + this.getModuleName() + ", parentId=" + this.getParentId() + ", routePath=" + this.getRoutePath() + ", component=" + this.getComponent() + ", icon=" + this.getIcon() + ", sortOrder=" + this.getSortOrder() + ", isEnabled=" + this.getIsEnabled() + ", createTime=" + this.getCreateTime() + ")";
    }
}
