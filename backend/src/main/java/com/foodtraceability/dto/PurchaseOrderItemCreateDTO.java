package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 采购订单明细创建DTO
 * 用于接收创建采购订单明细的请求参数
 */
@Schema(description = "采购订单明细创建DTO")
public class PurchaseOrderItemCreateDTO {
    @NotBlank(message = "商品ID不能为空")
    @Schema(description = "商品ID", required = true, example = "FOOD001")
    private String foodId;
    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", required = true, example = "优质大米")
    private String foodName;
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @Schema(description = "数量", required = true, example = "100")
    private Integer quantity;
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.0", message = "单价必须大于等于0")
    @Schema(description = "单价", required = true, example = "5.50")
    private BigDecimal unitPrice;
    @Schema(description = "规格", example = "25kg/袋")
    private String specification;

    /**
     * 计算小计金额
     * @return 小计金额
     */
    public BigDecimal calculateSubtotalAmount() {
        if (quantity != null && unitPrice != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    public PurchaseOrderItemCreateDTO() {
    }

    public String getFoodId() {
        return this.foodId;
    }

    public String getFoodName() {
        return this.foodName;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public String getSpecification() {
        return this.specification;
    }

    public void setFoodId(final String foodId) {
        this.foodId = foodId;
    }

    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseOrderItemCreateDTO)) return false;
        final PurchaseOrderItemCreateDTO other = (PurchaseOrderItemCreateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$specification = this.getSpecification();
        final java.lang.Object other$specification = other.getSpecification();
        if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseOrderItemCreateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $specification = this.getSpecification();
        result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseOrderItemCreateDTO(foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", specification=" + this.getSpecification() + ")";
    }
}
