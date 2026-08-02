package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 菜品-配料关联创建请求DTO
 * 
 * @author demo
 * @since 2025-12-29
 */
@Schema(description = "菜品配料关联创建请求")
public class DishIngredientCreateRequest {
    @Schema(description = "菜品ID")
    @NotBlank(message = "菜品ID不能为空")
    private String dishId;
    @Schema(description = "配料ID")
    @NotBlank(message = "配料ID不能为空")
    private String ingredientId;
    @Schema(description = "用量")
    @NotNull(message = "用量不能为空")
    @Positive(message = "用量必须大于0")
    private BigDecimal quantity;
    @Schema(description = "单位")
    @NotBlank(message = "单位不能为空")
    private String unit;
    @Schema(description = "备注")
    private String remark;

    public DishIngredientCreateRequest() {
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getIngredientId() {
        return this.ingredientId;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setIngredientId(final String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishIngredientCreateRequest)) return false;
        final DishIngredientCreateRequest other = (DishIngredientCreateRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$ingredientId = this.getIngredientId();
        final java.lang.Object other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DishIngredientCreateRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $ingredientId = this.getIngredientId();
        result = result * PRIME + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DishIngredientCreateRequest(dishId=" + this.getDishId() + ", ingredientId=" + this.getIngredientId() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", remark=" + this.getRemark() + ")";
    }
}
