package com.foodtraceability.service.dto;

import java.time.LocalDateTime;

/**
 * 简化的菜品响应DTO
 * @author example
 * @since 2026-01-08
 */
public class SimpleFoodResponse {
    /**
     * 菜品ID
     */
    private String foodId;
    /**
     * 菜品编码
     */
    private String foodCode;
    /**
     * 菜品名称
     */
    private String foodName;
    /**
     * 菜品分类
     */
    private String foodCategory;
    /**
     * 菜品状态
     */
    private Integer foodStatus;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public SimpleFoodResponse() {
    }

    /**
     * 菜品ID
     */
    public String getFoodId() {
        return this.foodId;
    }

    /**
     * 菜品编码
     */
    public String getFoodCode() {
        return this.foodCode;
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
     * 菜品状态
     */
    public Integer getFoodStatus() {
        return this.foodStatus;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 菜品ID
     */
    public void setFoodId(final String foodId) {
        this.foodId = foodId;
    }

    /**
     * 菜品编码
     */
    public void setFoodCode(final String foodCode) {
        this.foodCode = foodCode;
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
     * 菜品状态
     */
    public void setFoodStatus(final Integer foodStatus) {
        this.foodStatus = foodStatus;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SimpleFoodResponse)) return false;
        final SimpleFoodResponse other = (SimpleFoodResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$foodStatus = this.getFoodStatus();
        final java.lang.Object other$foodStatus = other.getFoodStatus();
        if (this$foodStatus == null ? other$foodStatus != null : !this$foodStatus.equals(other$foodStatus)) return false;
        final java.lang.Object this$foodId = this.getFoodId();
        final java.lang.Object other$foodId = other.getFoodId();
        if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
        final java.lang.Object this$foodCode = this.getFoodCode();
        final java.lang.Object other$foodCode = other.getFoodCode();
        if (this$foodCode == null ? other$foodCode != null : !this$foodCode.equals(other$foodCode)) return false;
        final java.lang.Object this$foodName = this.getFoodName();
        final java.lang.Object other$foodName = other.getFoodName();
        if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
        final java.lang.Object this$foodCategory = this.getFoodCategory();
        final java.lang.Object other$foodCategory = other.getFoodCategory();
        if (this$foodCategory == null ? other$foodCategory != null : !this$foodCategory.equals(other$foodCategory)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SimpleFoodResponse;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $foodStatus = this.getFoodStatus();
        result = result * PRIME + ($foodStatus == null ? 43 : $foodStatus.hashCode());
        final java.lang.Object $foodId = this.getFoodId();
        result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
        final java.lang.Object $foodCode = this.getFoodCode();
        result = result * PRIME + ($foodCode == null ? 43 : $foodCode.hashCode());
        final java.lang.Object $foodName = this.getFoodName();
        result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
        final java.lang.Object $foodCategory = this.getFoodCategory();
        result = result * PRIME + ($foodCategory == null ? 43 : $foodCategory.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SimpleFoodResponse(foodId=" + this.getFoodId() + ", foodCode=" + this.getFoodCode() + ", foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodStatus=" + this.getFoodStatus() + ", createTime=" + this.getCreateTime() + ")";
    }
}
