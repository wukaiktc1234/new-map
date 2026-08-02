package com.foodtraceability.dto;

import java.util.List;

/**
 * 权限同步结果
 */
public class PermissionSyncResult {
    private Integer added;
    private Integer updated;
    private Integer deleted;
    private Integer unchanged;
    private List<String> addedPermissions;
    private List<String> updatedPermissions;
    private List<String> deletedPermissions;

    public PermissionSyncResult() {
    }

    public Integer getAdded() {
        return this.added;
    }

    public Integer getUpdated() {
        return this.updated;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public Integer getUnchanged() {
        return this.unchanged;
    }

    public List<String> getAddedPermissions() {
        return this.addedPermissions;
    }

    public List<String> getUpdatedPermissions() {
        return this.updatedPermissions;
    }

    public List<String> getDeletedPermissions() {
        return this.deletedPermissions;
    }

    public void setAdded(final Integer added) {
        this.added = added;
    }

    public void setUpdated(final Integer updated) {
        this.updated = updated;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setUnchanged(final Integer unchanged) {
        this.unchanged = unchanged;
    }

    public void setAddedPermissions(final List<String> addedPermissions) {
        this.addedPermissions = addedPermissions;
    }

    public void setUpdatedPermissions(final List<String> updatedPermissions) {
        this.updatedPermissions = updatedPermissions;
    }

    public void setDeletedPermissions(final List<String> deletedPermissions) {
        this.deletedPermissions = deletedPermissions;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PermissionSyncResult)) return false;
        final PermissionSyncResult other = (PermissionSyncResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$added = this.getAdded();
        final java.lang.Object other$added = other.getAdded();
        if (this$added == null ? other$added != null : !this$added.equals(other$added)) return false;
        final java.lang.Object this$updated = this.getUpdated();
        final java.lang.Object other$updated = other.getUpdated();
        if (this$updated == null ? other$updated != null : !this$updated.equals(other$updated)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$unchanged = this.getUnchanged();
        final java.lang.Object other$unchanged = other.getUnchanged();
        if (this$unchanged == null ? other$unchanged != null : !this$unchanged.equals(other$unchanged)) return false;
        final java.lang.Object this$addedPermissions = this.getAddedPermissions();
        final java.lang.Object other$addedPermissions = other.getAddedPermissions();
        if (this$addedPermissions == null ? other$addedPermissions != null : !this$addedPermissions.equals(other$addedPermissions)) return false;
        final java.lang.Object this$updatedPermissions = this.getUpdatedPermissions();
        final java.lang.Object other$updatedPermissions = other.getUpdatedPermissions();
        if (this$updatedPermissions == null ? other$updatedPermissions != null : !this$updatedPermissions.equals(other$updatedPermissions)) return false;
        final java.lang.Object this$deletedPermissions = this.getDeletedPermissions();
        final java.lang.Object other$deletedPermissions = other.getDeletedPermissions();
        if (this$deletedPermissions == null ? other$deletedPermissions != null : !this$deletedPermissions.equals(other$deletedPermissions)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PermissionSyncResult;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $added = this.getAdded();
        result = result * PRIME + ($added == null ? 43 : $added.hashCode());
        final java.lang.Object $updated = this.getUpdated();
        result = result * PRIME + ($updated == null ? 43 : $updated.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $unchanged = this.getUnchanged();
        result = result * PRIME + ($unchanged == null ? 43 : $unchanged.hashCode());
        final java.lang.Object $addedPermissions = this.getAddedPermissions();
        result = result * PRIME + ($addedPermissions == null ? 43 : $addedPermissions.hashCode());
        final java.lang.Object $updatedPermissions = this.getUpdatedPermissions();
        result = result * PRIME + ($updatedPermissions == null ? 43 : $updatedPermissions.hashCode());
        final java.lang.Object $deletedPermissions = this.getDeletedPermissions();
        result = result * PRIME + ($deletedPermissions == null ? 43 : $deletedPermissions.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PermissionSyncResult(added=" + this.getAdded() + ", updated=" + this.getUpdated() + ", deleted=" + this.getDeleted() + ", unchanged=" + this.getUnchanged() + ", addedPermissions=" + this.getAddedPermissions() + ", updatedPermissions=" + this.getUpdatedPermissions() + ", deletedPermissions=" + this.getDeletedPermissions() + ")";
    }
}
