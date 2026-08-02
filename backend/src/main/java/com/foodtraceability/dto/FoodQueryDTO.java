package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商品查询DTO
 * 用于商品快速选择的查询参数
 */
@Schema(description = "商品查询DTO")
public class FoodQueryDTO {
    @Schema(description = "页码", example = "1")
    private Integer page = 1;
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
    @Schema(description = "商品分类ID", example = "CAT001")
    private String categoryId;
    @Schema(description = "关键字（商品名称或编码）", example = "大米")
    private String keyword;
    @Schema(description = "商品状态（1-上架，0-下架）", example = "1")
    private Integer status;

    public FoodQueryDTO() {
    }

    public Integer getPage() {
        return this.page;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public String getKeyword() {
        return this.keyword;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setPage(final Integer page) {
        this.page = page;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }

    public void setKeyword(final String keyword) {
        this.keyword = keyword;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FoodQueryDTO)) return false;
        final FoodQueryDTO other = (FoodQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$page = this.getPage();
        final java.lang.Object other$page = other.getPage();
        if (this$page == null ? other$page != null : !this$page.equals(other$page)) return false;
        final java.lang.Object this$pageSize = this.getPageSize();
        final java.lang.Object other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !this$pageSize.equals(other$pageSize)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$categoryId = this.getCategoryId();
        final java.lang.Object other$categoryId = other.getCategoryId();
        if (this$categoryId == null ? other$categoryId != null : !this$categoryId.equals(other$categoryId)) return false;
        final java.lang.Object this$keyword = this.getKeyword();
        final java.lang.Object other$keyword = other.getKeyword();
        if (this$keyword == null ? other$keyword != null : !this$keyword.equals(other$keyword)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoodQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $page = this.getPage();
        result = result * PRIME + ($page == null ? 43 : $page.hashCode());
        final java.lang.Object $pageSize = this.getPageSize();
        result = result * PRIME + ($pageSize == null ? 43 : $pageSize.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $categoryId = this.getCategoryId();
        result = result * PRIME + ($categoryId == null ? 43 : $categoryId.hashCode());
        final java.lang.Object $keyword = this.getKeyword();
        result = result * PRIME + ($keyword == null ? 43 : $keyword.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FoodQueryDTO(page=" + this.getPage() + ", pageSize=" + this.getPageSize() + ", categoryId=" + this.getCategoryId() + ", keyword=" + this.getKeyword() + ", status=" + this.getStatus() + ")";
    }
}
