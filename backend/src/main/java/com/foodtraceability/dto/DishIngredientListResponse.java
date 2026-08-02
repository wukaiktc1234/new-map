package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 菜品配料列表响应DTO
 * 
 * @author demo
 * @since 2025-12-29
 */
@Schema(description = "菜品配料列表响应")
public class DishIngredientListResponse {
    @Schema(description = "菜品配料列表")
    private List<DishIngredientDTO> records;
    @Schema(description = "总记录数")
    private Long total;
    @Schema(description = "每页记录数")
    private Long size;
    @Schema(description = "当前页码")
    private Long current;
    @Schema(description = "总页数")
    private Long pages;
    @Schema(description = "菜品总成本价（元）")
    private java.math.BigDecimal totalCostPrice;

    public DishIngredientListResponse() {
    }

    public List<DishIngredientDTO> getRecords() {
        return this.records;
    }

    public Long getTotal() {
        return this.total;
    }

    public Long getSize() {
        return this.size;
    }

    public Long getCurrent() {
        return this.current;
    }

    public Long getPages() {
        return this.pages;
    }

    public java.math.BigDecimal getTotalCostPrice() {
        return this.totalCostPrice;
    }

    public void setRecords(final List<DishIngredientDTO> records) {
        this.records = records;
    }

    public void setTotal(final Long total) {
        this.total = total;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public void setCurrent(final Long current) {
        this.current = current;
    }

    public void setPages(final Long pages) {
        this.pages = pages;
    }

    public void setTotalCostPrice(final java.math.BigDecimal totalCostPrice) {
        this.totalCostPrice = totalCostPrice;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishIngredientListResponse)) return false;
        final DishIngredientListResponse other = (DishIngredientListResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$total = this.getTotal();
        final java.lang.Object other$total = other.getTotal();
        if (this$total == null ? other$total != null : !this$total.equals(other$total)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        final java.lang.Object this$current = this.getCurrent();
        final java.lang.Object other$current = other.getCurrent();
        if (this$current == null ? other$current != null : !this$current.equals(other$current)) return false;
        final java.lang.Object this$pages = this.getPages();
        final java.lang.Object other$pages = other.getPages();
        if (this$pages == null ? other$pages != null : !this$pages.equals(other$pages)) return false;
        final java.lang.Object this$records = this.getRecords();
        final java.lang.Object other$records = other.getRecords();
        if (this$records == null ? other$records != null : !this$records.equals(other$records)) return false;
        final java.lang.Object this$totalCostPrice = this.getTotalCostPrice();
        final java.lang.Object other$totalCostPrice = other.getTotalCostPrice();
        if (this$totalCostPrice == null ? other$totalCostPrice != null : !this$totalCostPrice.equals(other$totalCostPrice)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DishIngredientListResponse;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $total = this.getTotal();
        result = result * PRIME + ($total == null ? 43 : $total.hashCode());
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $current = this.getCurrent();
        result = result * PRIME + ($current == null ? 43 : $current.hashCode());
        final java.lang.Object $pages = this.getPages();
        result = result * PRIME + ($pages == null ? 43 : $pages.hashCode());
        final java.lang.Object $records = this.getRecords();
        result = result * PRIME + ($records == null ? 43 : $records.hashCode());
        final java.lang.Object $totalCostPrice = this.getTotalCostPrice();
        result = result * PRIME + ($totalCostPrice == null ? 43 : $totalCostPrice.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DishIngredientListResponse(records=" + this.getRecords() + ", total=" + this.getTotal() + ", size=" + this.getSize() + ", current=" + this.getCurrent() + ", pages=" + this.getPages() + ", totalCostPrice=" + this.getTotalCostPrice() + ")";
    }
}
