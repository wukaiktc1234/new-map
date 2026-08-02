package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 菜单实体类
 * 用于管理系统菜单信息，支持层级关系
 */
@TableName("menus")
@Schema(description = "菜单实体")
public class Menu {
    /**
     * 菜单ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "菜单ID", example = "1234567890")
    private String menuId;
    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称", example = "食品管理")
    private String menuName;
    /**
     * 菜单编码（唯一）
     */
    @Schema(description = "菜单编码（唯一）", example = "MENU001")
    private String menuCode;
    /**
     * 父菜单ID
     */
    @Schema(description = "父菜单ID", example = "1234567890")
    private String parentId;
    /**
     * 菜单层级
     */
    @Schema(description = "菜单层级", example = "1")
    private Integer level;
    /**
     * 菜单路径（用于快速查询层级关系）
     */
    @Schema(description = "菜单路径", example = "/1234567890/")
    private String path;
    /**
     * 菜单URL
     */
    @Schema(description = "菜单URL", example = "/food")
    private String url;
    /**
     * 菜单图标
     */
    @Schema(description = "菜单图标", example = "el-icon-food")
    private String icon;
    /**
     * 菜单排序号
     */
    @Schema(description = "菜单排序号", example = "1")
    private Integer sortOrder;
    /**
     * 菜单状态（active: 正常, inactive: 禁用）
     */
    @Schema(description = "菜单状态（active: 正常, inactive: 禁用）", example = "active")
    private String status;
    /**
     * 菜单类型（directory: 目录, menu: 菜单, button: 按钮）
     */
    @Schema(description = "菜单类型（directory: 目录, menu: 菜单, button: 按钮）", example = "menu")
    private String menuType;
    /**
     * 权限编码
     */
    @Schema(description = "权限编码", example = "food:view")
    private String permissionCode;
    /**
     * 组件路径
     */
    @Schema(description = "组件路径", example = "FoodManagement")
    private String component;
    /**
     * 是否可见
     */
    @Schema(description = "是否可见", example = "true")
    private Boolean visible;
    /**
     * 是否缓存
     */
    @Schema(description = "是否缓存", example = "false")
    private Boolean keepAlive;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人", example = "system")
    private String createdBy;
    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人", example = "admin")
    private String updatedBy;
    /**
     * 逻辑删除标记
     */
    @TableLogic
    @Schema(description = "逻辑删除标记", example = "false")
    private Boolean deleted;
    /**
     * 版本号（乐观锁）
     */
    @Version
    @Schema(description = "版本号（乐观锁）", example = "1")
    private Integer version;
    /**
     * 扩展字段（JSON格式存储）
     */
    @Schema(description = "扩展字段（JSON格式存储）", example = "{\"customField\": \"value\"}")
    private String extData;

    public Menu() {
    }

    /**
     * 菜单ID
     */
    public String getMenuId() {
        return this.menuId;
    }

    /**
     * 菜单名称
     */
    public String getMenuName() {
        return this.menuName;
    }

    /**
     * 菜单编码（唯一）
     */
    public String getMenuCode() {
        return this.menuCode;
    }

    /**
     * 父菜单ID
     */
    public String getParentId() {
        return this.parentId;
    }

    /**
     * 菜单层级
     */
    public Integer getLevel() {
        return this.level;
    }

    /**
     * 菜单路径（用于快速查询层级关系）
     */
    public String getPath() {
        return this.path;
    }

    /**
     * 菜单URL
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * 菜单图标
     */
    public String getIcon() {
        return this.icon;
    }

    /**
     * 菜单排序号
     */
    public Integer getSortOrder() {
        return this.sortOrder;
    }

    /**
     * 菜单状态（active: 正常, inactive: 禁用）
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * 菜单类型（directory: 目录, menu: 菜单, button: 按钮）
     */
    public String getMenuType() {
        return this.menuType;
    }

    /**
     * 权限编码
     */
    public String getPermissionCode() {
        return this.permissionCode;
    }

    /**
     * 组件路径
     */
    public String getComponent() {
        return this.component;
    }

    /**
     * 是否可见
     */
    public Boolean getVisible() {
        return this.visible;
    }

    /**
     * 是否缓存
     */
    public Boolean getKeepAlive() {
        return this.keepAlive;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedTime() {
        return this.createdTime;
    }

    /**
     * 更新时间
     */
    public LocalDateTime getUpdatedTime() {
        return this.updatedTime;
    }

    /**
     * 创建人
     */
    public String getCreatedBy() {
        return this.createdBy;
    }

    /**
     * 更新人
     */
    public String getUpdatedBy() {
        return this.updatedBy;
    }

    /**
     * 逻辑删除标记
     */
    public Boolean getDeleted() {
        return this.deleted;
    }

    /**
     * 版本号（乐观锁）
     */
    public Integer getVersion() {
        return this.version;
    }

    /**
     * 扩展字段（JSON格式存储）
     */
    public String getExtData() {
        return this.extData;
    }

    /**
     * 菜单ID
     */
    public void setMenuId(final String menuId) {
        this.menuId = menuId;
    }

    /**
     * 菜单名称
     */
    public void setMenuName(final String menuName) {
        this.menuName = menuName;
    }

    /**
     * 菜单编码（唯一）
     */
    public void setMenuCode(final String menuCode) {
        this.menuCode = menuCode;
    }

    /**
     * 父菜单ID
     */
    public void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    /**
     * 菜单层级
     */
    public void setLevel(final Integer level) {
        this.level = level;
    }

    /**
     * 菜单路径（用于快速查询层级关系）
     */
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * 菜单URL
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * 菜单图标
     */
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    /**
     * 菜单排序号
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 菜单状态（active: 正常, inactive: 禁用）
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 菜单类型（directory: 目录, menu: 菜单, button: 按钮）
     */
    public void setMenuType(final String menuType) {
        this.menuType = menuType;
    }

    /**
     * 权限编码
     */
    public void setPermissionCode(final String permissionCode) {
        this.permissionCode = permissionCode;
    }

    /**
     * 组件路径
     */
    public void setComponent(final String component) {
        this.component = component;
    }

    /**
     * 是否可见
     */
    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }

    /**
     * 是否缓存
     */
    public void setKeepAlive(final Boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * 创建时间
     */
    public void setCreatedTime(final LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * 更新时间
     */
    public void setUpdatedTime(final LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    /**
     * 创建人
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 更新人
     */
    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * 逻辑删除标记
     */
    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * 版本号（乐观锁）
     */
    public void setVersion(final Integer version) {
        this.version = version;
    }

    /**
     * 扩展字段（JSON格式存储）
     */
    public void setExtData(final String extData) {
        this.extData = extData;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Menu)) return false;
        final Menu other = (Menu) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$visible = this.getVisible();
        final java.lang.Object other$visible = other.getVisible();
        if (this$visible == null ? other$visible != null : !this$visible.equals(other$visible)) return false;
        final java.lang.Object this$keepAlive = this.getKeepAlive();
        final java.lang.Object other$keepAlive = other.getKeepAlive();
        if (this$keepAlive == null ? other$keepAlive != null : !this$keepAlive.equals(other$keepAlive)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$menuId = this.getMenuId();
        final java.lang.Object other$menuId = other.getMenuId();
        if (this$menuId == null ? other$menuId != null : !this$menuId.equals(other$menuId)) return false;
        final java.lang.Object this$menuName = this.getMenuName();
        final java.lang.Object other$menuName = other.getMenuName();
        if (this$menuName == null ? other$menuName != null : !this$menuName.equals(other$menuName)) return false;
        final java.lang.Object this$menuCode = this.getMenuCode();
        final java.lang.Object other$menuCode = other.getMenuCode();
        if (this$menuCode == null ? other$menuCode != null : !this$menuCode.equals(other$menuCode)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$path = this.getPath();
        final java.lang.Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final java.lang.Object this$url = this.getUrl();
        final java.lang.Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
        final java.lang.Object this$icon = this.getIcon();
        final java.lang.Object other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$menuType = this.getMenuType();
        final java.lang.Object other$menuType = other.getMenuType();
        if (this$menuType == null ? other$menuType != null : !this$menuType.equals(other$menuType)) return false;
        final java.lang.Object this$permissionCode = this.getPermissionCode();
        final java.lang.Object other$permissionCode = other.getPermissionCode();
        if (this$permissionCode == null ? other$permissionCode != null : !this$permissionCode.equals(other$permissionCode)) return false;
        final java.lang.Object this$component = this.getComponent();
        final java.lang.Object other$component = other.getComponent();
        if (this$component == null ? other$component != null : !this$component.equals(other$component)) return false;
        final java.lang.Object this$createdTime = this.getCreatedTime();
        final java.lang.Object other$createdTime = other.getCreatedTime();
        if (this$createdTime == null ? other$createdTime != null : !this$createdTime.equals(other$createdTime)) return false;
        final java.lang.Object this$updatedTime = this.getUpdatedTime();
        final java.lang.Object other$updatedTime = other.getUpdatedTime();
        if (this$updatedTime == null ? other$updatedTime != null : !this$updatedTime.equals(other$updatedTime)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$extData = this.getExtData();
        final java.lang.Object other$extData = other.getExtData();
        if (this$extData == null ? other$extData != null : !this$extData.equals(other$extData)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Menu;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $visible = this.getVisible();
        result = result * PRIME + ($visible == null ? 43 : $visible.hashCode());
        final java.lang.Object $keepAlive = this.getKeepAlive();
        result = result * PRIME + ($keepAlive == null ? 43 : $keepAlive.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $menuId = this.getMenuId();
        result = result * PRIME + ($menuId == null ? 43 : $menuId.hashCode());
        final java.lang.Object $menuName = this.getMenuName();
        result = result * PRIME + ($menuName == null ? 43 : $menuName.hashCode());
        final java.lang.Object $menuCode = this.getMenuCode();
        result = result * PRIME + ($menuCode == null ? 43 : $menuCode.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final java.lang.Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final java.lang.Object $icon = this.getIcon();
        result = result * PRIME + ($icon == null ? 43 : $icon.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $menuType = this.getMenuType();
        result = result * PRIME + ($menuType == null ? 43 : $menuType.hashCode());
        final java.lang.Object $permissionCode = this.getPermissionCode();
        result = result * PRIME + ($permissionCode == null ? 43 : $permissionCode.hashCode());
        final java.lang.Object $component = this.getComponent();
        result = result * PRIME + ($component == null ? 43 : $component.hashCode());
        final java.lang.Object $createdTime = this.getCreatedTime();
        result = result * PRIME + ($createdTime == null ? 43 : $createdTime.hashCode());
        final java.lang.Object $updatedTime = this.getUpdatedTime();
        result = result * PRIME + ($updatedTime == null ? 43 : $updatedTime.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $extData = this.getExtData();
        result = result * PRIME + ($extData == null ? 43 : $extData.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Menu(menuId=" + this.getMenuId() + ", menuName=" + this.getMenuName() + ", menuCode=" + this.getMenuCode() + ", parentId=" + this.getParentId() + ", level=" + this.getLevel() + ", path=" + this.getPath() + ", url=" + this.getUrl() + ", icon=" + this.getIcon() + ", sortOrder=" + this.getSortOrder() + ", status=" + this.getStatus() + ", menuType=" + this.getMenuType() + ", permissionCode=" + this.getPermissionCode() + ", component=" + this.getComponent() + ", visible=" + this.getVisible() + ", keepAlive=" + this.getKeepAlive() + ", createdTime=" + this.getCreatedTime() + ", updatedTime=" + this.getUpdatedTime() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ", deleted=" + this.getDeleted() + ", version=" + this.getVersion() + ", extData=" + this.getExtData() + ")";
    }
}
