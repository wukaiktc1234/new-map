package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 权限实体类
 * 对应数据库表：permissions
 */
@TableName("permissions")
@Schema(description = "权限实体")
public class Permission {
    /**
     * 权限ID
     * 注：PostgreSQL 真实表 permissions 主键为 permission_id（V1.0.0.100__init_postgresql.sql）。
     */
    @TableId(type = IdType.AUTO, value = "permission_id")
    @Schema(description = "权限ID", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 权限编码
     */
    @TableField("permission_code")
    @Schema(description = "权限编码", example = "PERMISSION_VIEW")
    private String permissionCode;
    /**
     * 权限名称
     */
    @TableField("permission_name")
    @Schema(description = "权限名称", example = "查看权限")
    private String permissionName;
    /**
     * 权限类型（1-菜单，2-按钮，3-接口）
     */
    @TableField("permission_type")
    @Schema(description = "权限类型（1-菜单，2-按钮，3-接口）", example = "1")
    private Integer permissionType;
    /**
     * 所属模块
     */
    @TableField("module")
    @Schema(description = "所属模块", example = "system")
    private String module;
    /**
     * 父权限ID
     */
    @TableField("parent_id")
    @Schema(description = "父权限ID", example = "0")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    /**
     * 权限级别
     */
    @TableField("level")
    @Schema(description = "权限级别", example = "1")
    private Integer level;
    /**
     * 排序号
     */
    @TableField("sort_order")
    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;
    /**
     * 状态（1: 启用, 0: 禁用）
     */
    @TableField("status")
    @Schema(description = "状态（1-启用，0-禁用）", example = "1")
    private Integer status;
    /**
     * 图标
     */
    @TableField("icon")
    @Schema(description = "图标", example = "el-icon-setting")
    private String icon;
    /**
     * 路径（用于前端路由）
     */
    @TableField("path")
    @Schema(description = "路径", example = "/system/permission")
    private String path;
    /**
     * 组件路径（用于前端组件）
     */
    @TableField("component")
    @Schema(description = "组件路径", example = "system/permission/index")
    private String component;
    /**
     * 重定向
     */
    @TableField("redirect")
    @Schema(description = "重定向", example = "/system/permission/list")
    private String redirect;
    /**
     * 是否隐藏：1-是，0-否
     */
    @TableField("is_hidden")
    @Schema(description = "是否隐藏（1-是，0-否）", example = "0")
    private Integer isHidden;
    /**
     * 是否缓存：1-是，0-否
     */
    @TableField("is_cache")
    @Schema(description = "是否缓存（1-是，0-否）", example = "1")
    private Integer isCache;
    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;
    /**
     * 创建人ID
     */
    @TableField("created_by")
    @Schema(description = "创建人ID", example = "1")
    private Long createdBy;
    /**
     * 更新人ID
     */
    @TableField("updated_by")
    @Schema(description = "更新人ID", example = "1")
    private Long updatedBy;
    /**
     * 子权限列表（树形结构）
     */
    @TableField(exist = false)
    private List<Permission> children;

    public Permission() {
    }

    /**
     * 权限ID
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 权限编码
     */
    public String getPermissionCode() {
        return this.permissionCode;
    }

    /**
     * 权限名称
     */
    public String getPermissionName() {
        return this.permissionName;
    }

    /**
     * 权限类型（1-菜单，2-按钮，3-接口）
     */
    public Integer getPermissionType() {
        return this.permissionType;
    }

    /**
     * 所属模块
     */
    public String getModule() {
        return this.module;
    }

    /**
     * 父权限ID
     */
    public Long getParentId() {
        return this.parentId;
    }

    /**
     * 权限级别
     */
    public Integer getLevel() {
        return this.level;
    }

    /**
     * 排序号
     */
    public Integer getSortOrder() {
        return this.sortOrder;
    }

    /**
     * 状态（1: 启用, 0: 禁用）
     */
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 图标
     */
    public String getIcon() {
        return this.icon;
    }

    /**
     * 路径（用于前端路由）
     */
    public String getPath() {
        return this.path;
    }

    /**
     * 组件路径（用于前端组件）
     */
    public String getComponent() {
        return this.component;
    }

    /**
     * 重定向
     */
    public String getRedirect() {
        return this.redirect;
    }

    /**
     * 是否隐藏：1-是，0-否
     */
    public Integer getIsHidden() {
        return this.isHidden;
    }

    /**
     * 是否缓存：1-是，0-否
     */
    public Integer getIsCache() {
        return this.isCache;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 逻辑删除标记
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 创建人ID
     */
    public Long getCreatedBy() {
        return this.createdBy;
    }

    /**
     * 更新人ID
     */
    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    /**
     * 子权限列表（树形结构）
     */
    public List<Permission> getChildren() {
        return this.children;
    }

    /**
     * 权限ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 权限编码
     */
    public void setPermissionCode(final String permissionCode) {
        this.permissionCode = permissionCode;
    }

    /**
     * 权限名称
     */
    public void setPermissionName(final String permissionName) {
        this.permissionName = permissionName;
    }

    /**
     * 权限类型（1-菜单，2-按钮，3-接口）
     */
    public void setPermissionType(final Integer permissionType) {
        this.permissionType = permissionType;
    }

    /**
     * 所属模块
     */
    public void setModule(final String module) {
        this.module = module;
    }

    /**
     * 父权限ID
     */
    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 权限级别
     */
    public void setLevel(final Integer level) {
        this.level = level;
    }

    /**
     * 排序号
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 状态（1: 启用, 0: 禁用）
     */
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 图标
     */
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    /**
     * 路径（用于前端路由）
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * 组件路径（用于前端组件）
     */
    public void setComponent(final String component) {
        this.component = component;
    }

    /**
     * 重定向
     */
    public void setRedirect(final String redirect) {
        this.redirect = redirect;
    }

    /**
     * 是否隐藏：1-是，0-否
     */
    public void setIsHidden(final Integer isHidden) {
        this.isHidden = isHidden;
    }

    /**
     * 是否缓存：1-是，0-否
     */
    public void setIsCache(final Integer isCache) {
        this.isCache = isCache;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 更新时间
     */
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 逻辑删除标记
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 创建人ID
     */
    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 更新人ID
     */
    public void setUpdatedBy(final Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 子权限列表（树形结构）
     */
    public void setChildren(final List<Permission> children) {
        this.children = children;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Permission)) return false;
        final Permission other = (Permission) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$permissionType = this.getPermissionType();
        final java.lang.Object other$permissionType = other.getPermissionType();
        if (this$permissionType == null ? other$permissionType != null : !this$permissionType.equals(other$permissionType)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$isHidden = this.getIsHidden();
        final java.lang.Object other$isHidden = other.getIsHidden();
        if (this$isHidden == null ? other$isHidden != null : !this$isHidden.equals(other$isHidden)) return false;
        final java.lang.Object this$isCache = this.getIsCache();
        final java.lang.Object other$isCache = other.getIsCache();
        if (this$isCache == null ? other$isCache != null : !this$isCache.equals(other$isCache)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$permissionCode = this.getPermissionCode();
        final java.lang.Object other$permissionCode = other.getPermissionCode();
        if (this$permissionCode == null ? other$permissionCode != null : !this$permissionCode.equals(other$permissionCode)) return false;
        final java.lang.Object this$permissionName = this.getPermissionName();
        final java.lang.Object other$permissionName = other.getPermissionName();
        if (this$permissionName == null ? other$permissionName != null : !this$permissionName.equals(other$permissionName)) return false;
        final java.lang.Object this$module = this.getModule();
        final java.lang.Object other$module = other.getModule();
        if (this$module == null ? other$module != null : !this$module.equals(other$module)) return false;
        final java.lang.Object this$icon = this.getIcon();
        final java.lang.Object other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon)) return false;
        final java.lang.Object this$path = this.getPath();
        final java.lang.Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final java.lang.Object this$component = this.getComponent();
        final java.lang.Object other$component = other.getComponent();
        if (this$component == null ? other$component != null : !this$component.equals(other$component)) return false;
        final java.lang.Object this$redirect = this.getRedirect();
        final java.lang.Object other$redirect = other.getRedirect();
        if (this$redirect == null ? other$redirect != null : !this$redirect.equals(other$redirect)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Permission;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $permissionType = this.getPermissionType();
        result = result * PRIME + ($permissionType == null ? 43 : $permissionType.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $isHidden = this.getIsHidden();
        result = result * PRIME + ($isHidden == null ? 43 : $isHidden.hashCode());
        final java.lang.Object $isCache = this.getIsCache();
        result = result * PRIME + ($isCache == null ? 43 : $isCache.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $permissionCode = this.getPermissionCode();
        result = result * PRIME + ($permissionCode == null ? 43 : $permissionCode.hashCode());
        final java.lang.Object $permissionName = this.getPermissionName();
        result = result * PRIME + ($permissionName == null ? 43 : $permissionName.hashCode());
        final java.lang.Object $module = this.getModule();
        result = result * PRIME + ($module == null ? 43 : $module.hashCode());
        final java.lang.Object $icon = this.getIcon();
        result = result * PRIME + ($icon == null ? 43 : $icon.hashCode());
        final java.lang.Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final java.lang.Object $component = this.getComponent();
        result = result * PRIME + ($component == null ? 43 : $component.hashCode());
        final java.lang.Object $redirect = this.getRedirect();
        result = result * PRIME + ($redirect == null ? 43 : $redirect.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Permission(id=" + this.getId() + ", permissionCode=" + this.getPermissionCode() + ", permissionName=" + this.getPermissionName() + ", permissionType=" + this.getPermissionType() + ", module=" + this.getModule() + ", parentId=" + this.getParentId() + ", level=" + this.getLevel() + ", sortOrder=" + this.getSortOrder() + ", status=" + this.getStatus() + ", icon=" + this.getIcon() + ", path=" + this.getPath() + ", component=" + this.getComponent() + ", redirect=" + this.getRedirect() + ", isHidden=" + this.getIsHidden() + ", isCache=" + this.getIsCache() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ", children=" + this.getChildren() + ")";
    }
}
