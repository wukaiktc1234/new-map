package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 菜品基本信息DTO（用于缓存和批量查询）
 */
@Schema(description = "菜品基本信息")
public class FoodBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID", example = "1")
    private Long foodId;

    @Schema(description = "菜品编码", example = "FD001")
    private String foodCode;

    @Schema(description = "菜品名称", example = "红烧肉")
    private String foodName;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "规格", example = "大份")
    private String specification;

    @Schema(description = "单位", example = "份")
    private String unit;

    @Schema(description = "售价（分）", example = "3800")
    private Long salePrice;

    @Schema(description = "状态: 1在售 2停售 3售罄", example = "1")
    private Integer status;

    @Schema(description = "菜品图片URL")
    private String imageUrl;

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public String getFoodCode() { return foodCode; }
    public void setFoodCode(String foodCode) { this.foodCode = foodCode; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getSalePrice() { return salePrice; }
    public void setSalePrice(Long salePrice) { this.salePrice = salePrice; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
