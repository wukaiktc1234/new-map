package com.foodtraceability.entity;

/**
 * 类别统计实体类
 */
public class CategoryCount {
    private String category;
    private Long count;

    public CategoryCount() {
    }

    public String getCategory() {
        return this.category;
    }

    public Long getCount() {
        return this.count;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CategoryCount)) return false;
        final CategoryCount other = (CategoryCount) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        final java.lang.Object this$category = this.getCategory();
        final java.lang.Object other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CategoryCount;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        final java.lang.Object $category = this.getCategory();
        result = result * PRIME + ($category == null ? 43 : $category.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "CategoryCount(category=" + this.getCategory() + ", count=" + this.getCount() + ")";
    }
}
