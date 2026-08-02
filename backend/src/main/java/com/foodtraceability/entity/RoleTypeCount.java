package com.foodtraceability.entity;

/**
 * 角色类型统计实体类
 */
public class RoleTypeCount {
    private String roleType;
    private Long count;

    public RoleTypeCount() {
    }

    public String getRoleType() {
        return this.roleType;
    }

    public Long getCount() {
        return this.count;
    }

    public void setRoleType(final String roleType) {
        this.roleType = roleType;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RoleTypeCount)) return false;
        final RoleTypeCount other = (RoleTypeCount) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        final java.lang.Object this$roleType = this.getRoleType();
        final java.lang.Object other$roleType = other.getRoleType();
        if (this$roleType == null ? other$roleType != null : !this$roleType.equals(other$roleType)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RoleTypeCount;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        final java.lang.Object $roleType = this.getRoleType();
        result = result * PRIME + ($roleType == null ? 43 : $roleType.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RoleTypeCount(roleType=" + this.getRoleType() + ", count=" + this.getCount() + ")";
    }
}
