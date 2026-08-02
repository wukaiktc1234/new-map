package com.foodtraceability.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 简化的菜品请求DTO
 * @author example
 * @since 2026-01-08
 */
public class SimpleFoodRequest {
    /**
     * 菜品名称
     */
    @NotBlank(message = "菜品名称不能为空")
    private String foodName;
    /**
     * 菜品分类
     */
    private String foodCategory;
    /**
     * 菜品描述
     */
    private String foodDescription;
    /**
     * 菜品状态
     */
    private Integer foodStatus;

    public SimpleFoodRequest() {
    }

    /**
     * 菜品名称
     */
    public String getFoodName() {
        return this.foodName;
    }

    /**
     * 菜品分类
     */
    public String getFoodCategory() {
        return this.foodCategory;
    }

    /**
     * 菜品描述
     */
    public String getFoodDescription() {
        return this.foodDescription;
    }

    /**
     * 菜品状态
     */
    public Integer getFoodStatus() {
        return this.foodStatus;
    }

    /**
     * 菜品名称
     */
    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    /**
     * 菜品分类
     */
    public void setFoodCategory(final String foodCategory) {
        this.foodCategory = foodCategory;
    }

    /**
     * 菜品描述
     */
    public void setFoodDescription(final String foodDescription) {
        this.foodDescription = foodDescription;
    }

    /**
     * 菜品状态
     */
    public void setFoodStatus(final Integer foodStatus) {
        this.foodStatus = foodStatus;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SimpleFoodRequest)) return false;
        final SimpleFoodRequest other = (SimpleFoodRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$foodStatus = this.getFoodStatus();
        final java.lang.Object other$foodStatus = other.getFoodStatus();
        if (this$foodStatus == null ? other$foodStatus != null : !this$foodStatus.equals(other$foodStatus)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$foodCategory = this.getFoodCategory();
        final java.lang.Object other$foodCategory = other.getFoodCategory();
        if (this$foodCategory == null ? other$foodCategory != null : !this$foodCategory.equals(other$foodCategory)) return false;
        final java.lang.Object this$foodDescription = this.getFoodDescription();
        final java.lang.Object other$foodDescription = other.getFoodDescription();
        if (this$foodDescription == null ? other$foodDescription != null : !this$foodDescription.equals(other$foodDescription)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SimpleFoodRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $foodStatus = this.getFoodStatus();
        result = result * PRIME + ($foodStatus == null ? 43 : $foodStatus.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $foodCategory = this.getFoodCategory();
        result = result * PRIME + ($foodCategory == null ? 43 : $foodCategory.hashCode());
        final java.lang.Object $foodDescription = this.getFoodDescription();
        result = result * PRIME + ($foodDescription == null ? 43 : $foodDescription.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SimpleFoodRequest(foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodDescription=" + this.getFoodDescription() + ", foodStatus=" + this.getFoodStatus() + ")";
    }
}
