package com.foodtraceability.security.model;

import java.util.List;

public class UserPermissionInfo {
    private String userId;
    private String username;
    private List<String> roles;
    private List<String> permissions;
    private String storeId;
    private String departmentId;
    private Boolean isAdmin;
    private Long cacheTime;

    UserPermissionInfo(final String userId, final String username, final List<String> roles, final List<String> permissions, final String storeId, final String departmentId, final Boolean isAdmin, final Long cacheTime) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.permissions = permissions;
        this.storeId = storeId;
        this.departmentId = departmentId;
        this.isAdmin = isAdmin;
        this.cacheTime = cacheTime;
    }


    public static class UserPermissionInfoBuilder {
        private String userId;
        private String username;
        private List<String> roles;
        private List<String> permissions;
        private String storeId;
        private String departmentId;
        private Boolean isAdmin;
        private Long cacheTime;

        UserPermissionInfoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder roles(final List<String> roles) {
            this.roles = roles;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder permissions(final List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder storeId(final String storeId) {
            this.storeId = storeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder departmentId(final String departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder isAdmin(final Boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UserPermissionInfo.UserPermissionInfoBuilder cacheTime(final Long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }

        public UserPermissionInfo build() {
            return new UserPermissionInfo(this.userId, this.username, this.roles, this.permissions, this.storeId, this.departmentId, this.isAdmin, this.cacheTime);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "UserPermissionInfo.UserPermissionInfoBuilder(userId=" + this.userId + ", username=" + this.username + ", roles=" + this.roles + ", permissions=" + this.permissions + ", storeId=" + this.storeId + ", departmentId=" + this.departmentId + ", isAdmin=" + this.isAdmin + ", cacheTime=" + this.cacheTime + ")";
        }
    }

    public static UserPermissionInfo.UserPermissionInfoBuilder builder() {
        return new UserPermissionInfo.UserPermissionInfoBuilder();
    }

    public String getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public List<String> getPermissions() {
        return this.permissions;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getDepartmentId() {
        return this.departmentId;
    }

    public Boolean getIsAdmin() {
        return this.isAdmin;
    }

    public Long getCacheTime() {
        return this.cacheTime;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setRoles(final List<String> roles) {
        this.roles = roles;
    }

    public void setPermissions(final List<String> permissions) {
        this.permissions = permissions;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    public void setIsAdmin(final Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setCacheTime(final Long cacheTime) {
        this.cacheTime = cacheTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof UserPermissionInfo)) return false;
        final UserPermissionInfo other = (UserPermissionInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$isAdmin = this.getIsAdmin();
        final java.lang.Object other$isAdmin = other.getIsAdmin();
        if (this$isAdmin == null ? other$isAdmin != null : !this$isAdmin.equals(other$isAdmin)) return false;
        final java.lang.Object this$cacheTime = this.getCacheTime();
        final java.lang.Object other$cacheTime = other.getCacheTime();
        if (this$cacheTime == null ? other$cacheTime != null : !this$cacheTime.equals(other$cacheTime)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$roles = this.getRoles();
        final java.lang.Object other$roles = other.getRoles();
        if (this$roles == null ? other$roles != null : !this$roles.equals(other$roles)) return false;
        final java.lang.Object this$permissions = this.getPermissions();
        final java.lang.Object other$permissions = other.getPermissions();
        if (this$permissions == null ? other$permissions != null : !this$permissions.equals(other$permissions)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UserPermissionInfo;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $isAdmin = this.getIsAdmin();
        result = result * PRIME + ($isAdmin == null ? 43 : $isAdmin.hashCode());
        final java.lang.Object $cacheTime = this.getCacheTime();
        result = result * PRIME + ($cacheTime == null ? 43 : $cacheTime.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $roles = this.getRoles();
        result = result * PRIME + ($roles == null ? 43 : $roles.hashCode());
        final java.lang.Object $permissions = this.getPermissions();
        result = result * PRIME + ($permissions == null ? 43 : $permissions.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "UserPermissionInfo(userId=" + this.getUserId() + ", username=" + this.getUsername() + ", roles=" + this.getRoles() + ", permissions=" + this.getPermissions() + ", storeId=" + this.getStoreId() + ", departmentId=" + this.getDepartmentId() + ", isAdmin=" + this.getIsAdmin() + ", cacheTime=" + this.getCacheTime() + ")";
    }
}
