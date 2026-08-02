package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 权限响应DTO
 */
@Schema(description = "权限响应")
public class PermissionResponse {
    @Schema(description = "权限列表", example = "[\"user:manage\", \"product:view\"]")
    private List<String> permissions;
    @Schema(description = "角色列表", example = "[\"admin\", \"user\"]")
    private List<String> roles;

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public PermissionResponse() {
    }

    public List<String> getPermissions() {
        return this.permissions;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionResponse)) return false;
        final PermissionResponse other = (PermissionResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$permissions = this.getPermissions();
        final java.lang.Object other$permissions = other.getPermissions();
        if (this$permissions == null ? other$permissions != null : !this$permissions.equals(other$permissions)) return false;
        final java.lang.Object this$roles = this.getRoles();
        final java.lang.Object other$roles = other.getRoles();
        if (this$roles == null ? other$roles != null : !this$roles.equals(other$roles)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PermissionResponse;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $permissions = this.getPermissions();
        result = result * PRIME + ($permissions == null ? 43 : $permissions.hashCode());
        final java.lang.Object $roles = this.getRoles();
        result = result * PRIME + ($roles == null ? 43 : $roles.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PermissionResponse(permissions=" + this.getPermissions() + ", roles=" + this.getRoles() + ")";
    }
}
