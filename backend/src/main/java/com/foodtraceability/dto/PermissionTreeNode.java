package com.foodtraceability.dto;

import java.util.List;

/**
 * 权限树节点DTO
 */
public class PermissionTreeNode {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private Integer permissionType;
    private String module;
    private Long parentId;
    private Integer sortOrder;
    private Boolean checked;
    private Boolean disabled;
    private List<PermissionTreeNode> children;

    public PermissionTreeNode() {
    }

    public Long getId() {
        return this.id;
    }

    public String getPermissionCode() {
        return this.permissionCode;
    }

    public String getPermissionName() {
        return this.permissionName;
    }

    public Integer getPermissionType() {
        return this.permissionType;
    }

    public String getModule() {
        return this.module;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public Boolean getChecked() {
        return this.checked;
    }

    public Boolean getDisabled() {
        return this.disabled;
    }

    public List<PermissionTreeNode> getChildren() {
        return this.children;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setPermissionCode(final String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public void setPermissionName(final String permissionName) {
        this.permissionName = permissionName;
    }

    public void setPermissionType(final Integer permissionType) {
        this.permissionType = permissionType;
    }

    public void setModule(final String module) {
        this.module = module;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setChecked(final Boolean checked) {
        this.checked = checked;
    }

    public void setDisabled(final Boolean disabled) {
        this.disabled = disabled;
    }

    public void setChildren(final List<PermissionTreeNode> children) {
        this.children = children;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionTreeNode)) return false;
        final PermissionTreeNode other = (PermissionTreeNode) o;
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
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$checked = this.getChecked();
        final java.lang.Object other$checked = other.getChecked();
        if (this$checked == null ? other$checked != null : !this$checked.equals(other$checked)) return false;
        final java.lang.Object this$disabled = this.getDisabled();
        final java.lang.Object other$disabled = other.getDisabled();
        if (this$disabled == null ? other$disabled != null : !this$disabled.equals(other$disabled)) return false;
        final java.lang.Object this$permissionCode = this.getPermissionCode();
        final java.lang.Object other$permissionCode = other.getPermissionCode();
        if (this$permissionCode == null ? other$permissionCode != null : !this$permissionCode.equals(other$permissionCode)) return false;
        final java.lang.Object this$permissionName = this.getPermissionName();
        final java.lang.Object other$permissionName = other.getPermissionName();
        if (this$permissionName == null ? other$permissionName != null : !this$permissionName.equals(other$permissionName)) return false;
        final java.lang.Object this$module = this.getModule();
        final java.lang.Object other$module = other.getModule();
        if (this$module == null ? other$module != null : !this$module.equals(other$module)) return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PermissionTreeNode;
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
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $checked = this.getChecked();
        result = result * PRIME + ($checked == null ? 43 : $checked.hashCode());
        final java.lang.Object $disabled = this.getDisabled();
        result = result * PRIME + ($disabled == null ? 43 : $disabled.hashCode());
        final java.lang.Object $permissionCode = this.getPermissionCode();
        result = result * PRIME + ($permissionCode == null ? 43 : $permissionCode.hashCode());
        final java.lang.Object $permissionName = this.getPermissionName();
        result = result * PRIME + ($permissionName == null ? 43 : $permissionName.hashCode());
        final java.lang.Object $module = this.getModule();
        result = result * PRIME + ($module == null ? 43 : $module.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PermissionTreeNode(id=" + this.getId() + ", permissionCode=" + this.getPermissionCode() + ", permissionName=" + this.getPermissionName() + ", permissionType=" + this.getPermissionType() + ", module=" + this.getModule() + ", parentId=" + this.getParentId() + ", sortOrder=" + this.getSortOrder() + ", checked=" + this.getChecked() + ", disabled=" + this.getDisabled() + ", children=" + this.getChildren() + ")";
    }
}
