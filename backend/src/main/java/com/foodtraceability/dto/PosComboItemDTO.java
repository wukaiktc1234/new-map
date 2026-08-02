package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * POS套餐项DTO
 * 用于收银终端套餐内菜品项数据传输
 */
@Schema(description = "POS套餐项DTO")
public class PosComboItemDTO {

    @Schema(description = "食材ID")
    private String foodId;

    @Schema(description = "食材名称")
    private String foodName;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "价格")
    private BigDecimal price;

    public PosComboItemDTO() {
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
