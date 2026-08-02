package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 菜品响应DTO
 * 用于餐厅菜品管理返回，包含完整的菜品信息
 */
@Schema(description = "菜品响应")
public class FoodResponse {
    
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
    
    @Schema(description = "菜品价格")
    private BigDecimal price;
    
    @Schema(description = "成本价")
    private BigDecimal costPrice;
    
    @Schema(description = "菜品图片URL")
    private String foodImageUrl;
    
    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;

    // Getter and Setter methods
    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodCode() {
        return foodCode;
    }

    public void setFoodCode(String foodCode) {
        this.foodCode = foodCode;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(String foodCategory) {
        this.foodCategory = foodCategory;
    }

    public String getFoodDescription() {
        return foodDescription;
    }

    public void setFoodDescription(String foodDescription) {
        this.foodDescription = foodDescription;
    }

    public Integer getFoodStatus() {
        return foodStatus;
    }

    public void setFoodStatus(Integer foodStatus) {
        this.foodStatus = foodStatus;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getFoodImageUrl() {
        return foodImageUrl;
    }

    public void setFoodImageUrl(String foodImageUrl) {
        this.foodImageUrl = foodImageUrl;
    }

    public java.time.LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.time.LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public java.time.LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.time.LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}