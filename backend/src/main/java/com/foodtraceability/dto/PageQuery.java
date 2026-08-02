package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分页查询基类
 */
@Schema(description = "分页查询基类")
public class PageQuery {
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;
    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;

    public Integer getCurrent() {
        return current != null && current > 0 ? current : 1;
    }

    public Integer getSize() {
        return size != null && size > 0 && size <= 100 ? size : 10;
    }

    public long getOffset() {
        return (long) (getCurrent() - 1) * getSize();
    }

    public PageQuery() {
    }

    public void setCurrent(final Integer current) {
        this.current = current;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PageQuery)) return false;
        final PageQuery other = (PageQuery) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$current = this.getCurrent();
        final java.lang.Object other$current = other.getCurrent();
        if (this$current == null ? other$current != null : !this$current.equals(other$current)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PageQuery;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $current = this.getCurrent();
        result = result * PRIME + ($current == null ? 43 : $current.hashCode());
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PageQuery(current=" + this.getCurrent() + ", size=" + this.getSize() + ")";
    }
}
