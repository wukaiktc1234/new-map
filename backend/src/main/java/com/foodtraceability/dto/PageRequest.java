package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分页请求基类
 */
public class PageRequest {
    @Schema(description = "页码，从1开始", example = "1")
    private int page = 1;
    @Schema(description = "每页条数", example = "10")
    private int size = 10;

    public int getPage() {
        return Math.max(1, page);
    }

    public int getSize() {
        return Math.max(1, Math.min(100, size));
    }

    public PageRequest() {
    }

    public void setPage(final int page) {
        this.page = page;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PageRequest)) return false;
        final PageRequest other = (PageRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getPage() != other.getPage()) return false;
        if (this.getSize() != other.getSize()) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PageRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getPage();
        result = result * PRIME + this.getSize();
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PageRequest(page=" + this.getPage() + ", size=" + this.getSize() + ")";
    }
}
