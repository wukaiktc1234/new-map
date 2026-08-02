package com.foodtraceability.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public class DishComboCreateDTO {

    @NotBlank(message = "套餐名称不能为空")
    private String comboName;

    private String comboCode;

    private String description;

    @NotNull(message = "套餐价格不能为空")
    @Positive(message = "套餐价格必须大于0")
    private BigDecimal price;

    private String status;

    private String imageUrl;

    private List<ComboIngredientDTO> ingredients;

    // Getters and Setters
    public String getComboName() {
        return comboName;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public String getComboCode() {
        return comboCode;
    }

    public void setComboCode(String comboCode) {
        this.comboCode = comboCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ComboIngredientDTO> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<ComboIngredientDTO> ingredients) {
        this.ingredients = ingredients;
    }
}
