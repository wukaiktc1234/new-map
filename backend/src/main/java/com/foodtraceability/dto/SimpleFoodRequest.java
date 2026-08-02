package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 菜品请求DTO（简化版）
 * 用于餐厅菜品管理，只包含必要的字段
 */
@Schema(description = "菜品请求（简化版）")
public class SimpleFoodRequest {
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称长度不能超过100个字符")
    @Schema(description = "菜品名称", example = "宫保鸡丁")
    private String foodName;
    @NotBlank(message = "菜品分类不能为空")
    @Size(max = 50, message = "菜品分类长度不能超过50个字符")
    @Schema(description = "菜品分类", example = "热菜")
    private String foodCategory;
    @Size(max = 500, message = "菜品描述长度不能超过500个字符")
    @Schema(description = "菜品描述", example = "经典川菜，麻辣鲜香")
    private String foodDescription;
    @NotNull(message = "菜品状态不能为空")
    @Min(value = 0, message = "菜品状态不能小于0")
    @Max(value = 1, message = "菜品状态不能超过1")
    @Schema(description = "菜品状态（0-下架，1-上架）", example = "1")
    private Integer foodStatus;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格不能小于0.01元")
    @DecimalMax(value = "999999.99", message = "价格不能超过999999.99元")
    @Schema(description = "价格（元）", example = "38.50")
    private java.math.BigDecimal price;
    @Size(max = 200, message = "菜品图片URL长度不能超过200个字符")
    @Schema(description = "菜品图片URL", example = "/uploads/food/gongbao-chicken.jpg")
    private String foodImageUrl;

    public String getFoodName() {
        return foodName;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public String getFoodDescription() {
        return foodDescription;
    }

    public Integer getFoodStatus() {
        return foodStatus;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public String getFoodImageUrl() {
        return foodImageUrl;
    }

    public SimpleFoodRequest() {
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
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$foodImageUrl = this.getFoodImageUrl();
        final java.lang.Object other$foodImageUrl = other.getFoodImageUrl();
        if (this$foodImageUrl == null ? other$foodImageUrl != null : !this$foodImageUrl.equals(other$foodImageUrl)) return false;
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
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $foodImageUrl = this.getFoodImageUrl();
        result = result * PRIME + ($foodImageUrl == null ? 43 : $foodImageUrl.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SimpleFoodRequest(foodName=" + this.getFoodName() + ", foodCategory=" + this.getFoodCategory() + ", foodDescription=" + this.getFoodDescription() + ", foodStatus=" + this.getFoodStatus() + ", price=" + this.getPrice() + ", foodImageUrl=" + this.getFoodImageUrl() + ")";
    }
}
