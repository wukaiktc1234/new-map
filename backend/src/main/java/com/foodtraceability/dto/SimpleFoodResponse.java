package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜品响应DTO（简化版）
 * 用于餐厅菜品管理返回
 */
@Schema(description = "菜品响应（简化版）")
public class SimpleFoodResponse {
    @Schema(description = "菜品ID")
    private String foodId;
    @Schema(description = "菜品编码，格式为D+00000，例如：D00001")
    private String foodCode;
    @Schema(description = "菜品名称")
    private String foodName;
    @Schema(description = "菜品分类")
    private String foodCategory;
    @Schema(description = "菜品描述")
    private String foodDescription;
    @Schema(description = "菜品状态（0-下架，1-上架）")
    private Integer foodStatus;
    @Schema(description = "价格（元）")
    private java.math.BigDecimal price;
    @Schema(description = "菜品图片URL")
    private String foodImageUrl;
    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;
    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;

    public SimpleFoodResponse() {
    }

    public String getFoodId() {
        return this.foodId;
    }

    public String getFoodCode() {
        return this.foodCode;
    }

    public String getFoodName() {
        return this.foodName;
    }

    public String getFoodCategory() {
        return this.foodCategory;
    }

    public String getFoodDescription() {
        return this.foodDescription;
    }

    public Integer getFoodStatus() {
        return this.foodStatus;
    }

    public java.math.BigDecimal getPrice() {
        return this.price;
    }

    public String getFoodImageUrl() {
        return this.foodImageUrl;
    }

    public java.time.LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public java.time.LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setFoodId(final String foodId) {
        this.foodId = foodId;
    }

    public void setFoodCode(final String foodCode) {
        this.foodCode = foodCode;
    }

    public void setFoodName(final String foodName) {
        this.foodName = foodName;
    }

    public void setFoodCategory(final String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public void setFoodDescription(final String foodDescription) {
        this.foodDescription = foodDescription;
    }

    public void setFoodStatus(final Integer foodStatus) {
        this.foodStatus = foodStatus;
    }

    public void setPrice(final java.math.BigDecimal price) {
        this.price = price;
    }

    public void setFoodImageUrl(final String foodImageUrl) {
        this.foodImageUrl = foodImageUrl;
    }

    public void setCreateTime(final java.time.LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final java.time.LocalDateTime updateTime) {
        this.updateTime = updateTime;
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
        final java.lang.Object this$foodDescription = this.getFoodDescription();
        final java.lang.Object other$foodDescription = other.getFoodDescription();
        if (this$foodDescription == null ? other$foodDescription != null : !this$foodDescription.equals(other$foodDescription)) return false;
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$foodImageUrl = this.getFoodImageUrl();
        final java.lang.Object other$foodImageUrl = other.getFoodImageUrl();
        if (this$foodImageUrl == null ? other$foodImageUrl != null : !this$foodImageUrl.equals(other$foodImageUrl)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
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
        final java.lang.Object $foodDescription = this.getFoodDescription();
        result = result * PRIME + ($foodDescription == null ? 43 : $foodDescription.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $foodImageUrl = this.getFoodImageUrl();
        result = result * PRIME + ($foodImageUrl == null ? 43 : $foodImageUrl.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SimpleFoodResponse(foodId=" + this.getFoodId() + ", foodCode=" + this.getFoodCode() + ", foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodDescription=" + this.getFoodDescription() + ", foodStatus=" + this.getFoodStatus() + ", price=" + this.getPrice() + ", foodImageUrl=" + this.getFoodImageUrl() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
