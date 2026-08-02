package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("v_inventory_summary")
@Schema(description = "库存汇总视图实体")
public class InventorySummary {
    @Schema(description = "产品ID")
    private Long productId;
    @Schema(description = "产品名称")
    private String productName;
    @Schema(description = "计量单位")
    private String unit;
    @Schema(description = "成本单价")
    private BigDecimal costPrice;
    @Schema(description = "仓库库存总量")
    private Integer warehouseStock;
    @Schema(description = "门店库存总量")
    private Integer storeStock;
    @Schema(description = "总库存量")
    private Integer totalStock;
    @Schema(description = "总库存价值")
    private BigDecimal totalValue;
    @Schema(description = "涉及门店数量")
    private Integer storeCount;

    public InventorySummary() {
    }

    public Long getProductId() {
        return this.productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    public Integer getWarehouseStock() {
        return this.warehouseStock;
    }

    public Integer getStoreStock() {
        return this.storeStock;
    }

    public Integer getTotalStock() {
        return this.totalStock;
    }

    public BigDecimal getTotalValue() {
        return this.totalValue;
    }

    public Integer getStoreCount() {
        return this.storeCount;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setCostPrice(final BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setWarehouseStock(final Integer warehouseStock) {
        this.warehouseStock = warehouseStock;
    }

    public void setStoreStock(final Integer storeStock) {
        this.storeStock = storeStock;
    }

    public void setTotalStock(final Integer totalStock) {
        this.totalStock = totalStock;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public void setStoreCount(final Integer storeCount) {
        this.storeCount = storeCount;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InventorySummary)) return false;
        final InventorySummary other = (InventorySummary) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$warehouseStock = this.getWarehouseStock();
        final java.lang.Object other$warehouseStock = other.getWarehouseStock();
        if (this$warehouseStock == null ? other$warehouseStock != null : !this$warehouseStock.equals(other$warehouseStock)) return false;
        final java.lang.Object this$storeStock = this.getStoreStock();
        final java.lang.Object other$storeStock = other.getStoreStock();
        if (this$storeStock == null ? other$storeStock != null : !this$storeStock.equals(other$storeStock)) return false;
        final java.lang.Object this$totalStock = this.getTotalStock();
        final java.lang.Object other$totalStock = other.getTotalStock();
        if (this$totalStock == null ? other$totalStock != null : !this$totalStock.equals(other$totalStock)) return false;
        final java.lang.Object this$storeCount = this.getStoreCount();
        final java.lang.Object other$storeCount = other.getStoreCount();
        if (this$storeCount == null ? other$storeCount != null : !this$storeCount.equals(other$storeCount)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$costPrice = this.getCostPrice();
        final java.lang.Object other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !this$costPrice.equals(other$costPrice)) return false;
        final java.lang.Object this$totalValue = this.getTotalValue();
        final java.lang.Object other$totalValue = other.getTotalValue();
        if (this$totalValue == null ? other$totalValue != null : !this$totalValue.equals(other$totalValue)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InventorySummary;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $warehouseStock = this.getWarehouseStock();
        result = result * PRIME + ($warehouseStock == null ? 43 : $warehouseStock.hashCode());
        final java.lang.Object $storeStock = this.getStoreStock();
        result = result * PRIME + ($storeStock == null ? 43 : $storeStock.hashCode());
        final java.lang.Object $totalStock = this.getTotalStock();
        result = result * PRIME + ($totalStock == null ? 43 : $totalStock.hashCode());
        final java.lang.Object $storeCount = this.getStoreCount();
        result = result * PRIME + ($storeCount == null ? 43 : $storeCount.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $costPrice = this.getCostPrice();
        result = result * PRIME + ($costPrice == null ? 43 : $costPrice.hashCode());
        final java.lang.Object $totalValue = this.getTotalValue();
        result = result * PRIME + ($totalValue == null ? 43 : $totalValue.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InventorySummary(productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", unit=" + this.getUnit() + ", costPrice=" + this.getCostPrice() + ", warehouseStock=" + this.getWarehouseStock() + ", storeStock=" + this.getStoreStock() + ", totalStock=" + this.getTotalStock() + ", totalValue=" + this.getTotalValue() + ", storeCount=" + this.getStoreCount() + ")";
    }
}
