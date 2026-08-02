package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;

@TableName("role_permissions")
public class RolePermission implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long permissionId;
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public RolePermission() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getRoleId() {
        return this.roleId;
    }

    public Long getPermissionId() {
        return this.permissionId;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setRoleId(final Long roleId) {
        this.roleId = roleId;
    }

    public void setPermissionId(final Long permissionId) {
        this.permissionId = permissionId;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RolePermission)) return false;
        final RolePermission other = (RolePermission) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$roleId = this.getRoleId();
        final java.lang.Object other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !this$roleId.equals(other$roleId)) return false;
        final java.lang.Object this$permissionId = this.getPermissionId();
        final java.lang.Object other$permissionId = other.getPermissionId();
        if (this$permissionId == null ? other$permissionId != null : !this$permissionId.equals(other$permissionId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RolePermission;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $roleId = this.getRoleId();
        result = result * PRIME + ($roleId == null ? 43 : $roleId.hashCode());
        final java.lang.Object $permissionId = this.getPermissionId();
        result = result * PRIME + ($permissionId == null ? 43 : $permissionId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RolePermission(id=" + this.getId() + ", roleId=" + this.getRoleId() + ", permissionId=" + this.getPermissionId() + ", deleted=" + this.getDeleted() + ")";
    }
}
