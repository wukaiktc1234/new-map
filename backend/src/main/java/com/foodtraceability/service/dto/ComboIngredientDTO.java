package com.foodtraceability.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class ComboIngredientDTO {
    @NotNull(message = "菜品ID不能为空")
    private String foodId;
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于0")
    private BigDecimal quantity;
    @NotNull(message = "单位不能为空")
    private String unit;

    // Getter methods
    public String getFoodId() {
        return foodId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    // Setter methods
    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ComboIngredientDTO() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ComboIngredientDTO)) return false;
        final ComboIngredientDTO other = (ComboIngredientDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ComboIngredientDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ComboIngredientDTO(foodId=" + this.getFoodId() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ")";
    }
}
