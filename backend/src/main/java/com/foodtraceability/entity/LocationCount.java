package com.foodtraceability.entity;

/**
 * 操作地点统计实体类
 */
public class LocationCount {
    private String location;
    private Long count;

    public LocationCount() {
    }

    public String getLocation() {
        return this.location;
    }

    public Long getCount() {
        return this.count;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LocationCount)) return false;
        final LocationCount other = (LocationCount) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        final java.lang.Object this$location = this.getLocation();
        final java.lang.Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LocationCount;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        final java.lang.Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LocationCount(location=" + this.getLocation() + ", count=" + this.getCount() + ")";
    }
}
