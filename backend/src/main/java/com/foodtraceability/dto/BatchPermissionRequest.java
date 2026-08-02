package com.foodtraceability.dto;

import java.util.List;

/**
 * 批量权限请求
 */
public class BatchPermissionRequest {
    private List<Long> permissionIds;
    private Boolean merge;

    public BatchPermissionRequest() {
    }

    public List<Long> getPermissionIds() {
        return this.permissionIds;
    }

    public Boolean getMerge() {
        return this.merge;
    }

    public void setPermissionIds(final List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }

    public void setMerge(final Boolean merge) {
        this.merge = merge;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BatchPermissionRequest)) return false;
        final BatchPermissionRequest other = (BatchPermissionRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$merge = this.getMerge();
        final java.lang.Object other$merge = other.getMerge();
        if (this$merge == null ? other$merge != null : !this$merge.equals(other$merge)) return false;
        final java.lang.Object this$permissionIds = this.getPermissionIds();
        final java.lang.Object other$permissionIds = other.getPermissionIds();
        if (this$permissionIds == null ? other$permissionIds != null : !this$permissionIds.equals(other$permissionIds)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BatchPermissionRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $merge = this.getMerge();
        result = result * PRIME + ($merge == null ? 43 : $merge.hashCode());
        final java.lang.Object $permissionIds = this.getPermissionIds();
        result = result * PRIME + ($permissionIds == null ? 43 : $permissionIds.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BatchPermissionRequest(permissionIds=" + this.getPermissionIds() + ", merge=" + this.getMerge() + ")";
    }
}
