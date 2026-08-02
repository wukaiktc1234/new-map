package com.foodtraceability.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.util.List;

/**
 * 菜品创建请求DTO
 */
@Schema(description = "菜品创建请求")
public class FoodCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "菜品名称不能为空")
    @Schema(description = "菜品名称", example = "红烧肉", requiredMode = Schema.RequiredMode.REQUIRED)
    private String foodName;

    @Schema(description = "菜品编码（可选，系统自动生成）", example = "FD001")
    private String foodCode;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @Schema(description = "规格", example = "大份")
    private String specification;

    @Schema(description = "单位", example = "份")
    private String unit;

    @Positive(message = "售价必须大于0")
    @Schema(description = "售价（分）", example = "3800")
    private Long salePrice;

    @Schema(description = "成本价（分）", example = "1500")
    private Long costPrice;

    @Schema(description = "库存数量", example = "100")
    private Integer stock;

    @Schema(description = "最低库存预警", example = "10")
    private Integer minStock;

    @Schema(description = "菜品图片URL")
    private String imageUrl;

    @Schema(description = "菜品描述")
    private String description;

    @Schema(description = "制作时间（分钟）", example = "15")
    private Integer cookingTime;

    @Schema(description = "状态: 1在售 0停售 2售罄", example = "1")
    private Integer status;

    @Schema(description = "是否推荐")
    private Boolean isRecommend;

    @Schema(description = "是否辣")
    private Boolean isSpicy;

    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    @Schema(description = "原料明细列表")
    private List<RecipeItemDTO> recipes;

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getFoodCode() {
        return foodCode;
    }

    public void setFoodCode(String foodCode) {
        this.foodCode = foodCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Long salePrice) {
        this.salePrice = salePrice;
    }

    public Long getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Long costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = cookingTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Boolean isRecommend) {
        this.isRecommend = isRecommend;
    }

    public Boolean getIsSpicy() {
        return isSpicy;
    }

    public void setIsSpicy(Boolean isSpicy) {
        this.isSpicy = isSpicy;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<RecipeItemDTO> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<RecipeItemDTO> recipes) {
        this.recipes = recipes;
    }
}
