package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import com.foodtraceability.entity.ComboIngredient;

@Schema(description = "更新套餐请求DTO")
public class DishComboUpdateDTO {
    @Schema(description = "套餐名称")
    private String comboName;
    @Positive(message = "套餐价格必须大于0")
    @Schema(description = "套餐价格")
    private BigDecimal price;
    @Schema(description = "套餐描述")
    private String description;
    @Schema(description = "套餐类型：regular-普通套餐，special-特色套餐，family-家庭套餐，business-商务套餐，set-套餐组合")
    private String comboType;
    @Schema(description = "适用人数")
    private Integer peopleCount;
    @Schema(description = "套餐状态（0-禁用，1-启用）")
    private Integer status;
    @Schema(description = "套餐成分列表")
    private List<ComboIngredient> ingredients;

    // Getter methods
    public String getComboName() {
        return comboName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getComboType() {
        return comboType;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public Integer getStatus() {
        return status;
    }

    public List<ComboIngredient> getIngredients() {
        return ingredients;
    }

    // Setter methods
    public void setComboName(String comboName) {
        this.comboName = comboName;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setComboType(String comboType) {
        this.comboType = comboType;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setIngredients(List<ComboIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public DishComboUpdateDTO() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishComboUpdateDTO)) return false;
        final DishComboUpdateDTO other = (DishComboUpdateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$peopleCount = this.getPeopleCount();
        final java.lang.Object other$peopleCount = other.getPeopleCount();
        if (this$peopleCount == null ? other$peopleCount != null : !this$peopleCount.equals(other$peopleCount)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$comboName = this.getComboName();
        final java.lang.Object other$comboName = other.getComboName();
        if (this$comboName == null ? other$comboName != null : !this$comboName.equals(other$comboName)) return false;
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$comboType = this.getComboType();
        final java.lang.Object other$comboType = other.getComboType();
        if (this$comboType == null ? other$comboType != null : !this$comboType.equals(other$comboType)) return false;
        final java.lang.Object this$ingredients = this.getIngredients();
        final java.lang.Object other$ingredients = other.getIngredients();
        if (this$ingredients == null ? other$ingredients != null : !this$ingredients.equals(other$ingredients)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DishComboUpdateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $peopleCount = this.getPeopleCount();
        result = result * PRIME + ($peopleCount == null ? 43 : $peopleCount.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $comboName = this.getComboName();
        result = result * PRIME + ($comboName == null ? 43 : $comboName.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $comboType = this.getComboType();
        result = result * PRIME + ($comboType == null ? 43 : $comboType.hashCode());
        final java.lang.Object $ingredients = this.getIngredients();
        result = result * PRIME + ($ingredients == null ? 43 : $ingredients.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DishComboUpdateDTO(comboName=" + this.getComboName() + ", price=" + this.getPrice() + ", description=" + this.getDescription() + ", comboType=" + this.getComboType() + ", peopleCount=" + this.getPeopleCount() + ", status=" + this.getStatus() + ", ingredients=" + this.getIngredients() + ")";
    }
}
