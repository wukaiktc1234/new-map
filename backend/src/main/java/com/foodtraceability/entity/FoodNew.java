package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 菜品/食品实体类
 * 用于管理餐厅菜品信息，包括价格、库存、状态等
 */
@TableName("foods")
@Schema(description = "菜品/食品实体")
public class FoodNew {

    /** 菜品ID，主键自增 */
    @TableId(value = "food_id", type = IdType.AUTO)
    @Schema(description = "菜品ID", example = "1")
    private Long foodId;

    /** 菜品编码，唯一 */
    @TableField("food_code")
    @Schema(description = "菜品编码", example = "FD001")
    private String foodCode;

    /** 菜品名称 */
    @TableField("food_name")
    @Schema(description = "菜品名称", example = "红烧肉")
    private String foodName;

    /** 分类ID，关联food_categories表 */
    @TableField("category_id")
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /** 规格（如"大份/中份/小份"） */
    @TableField("specification")
    @Schema(description = "规格", example = "大份")
    private String specification;

    /** 单位（份/碗/杯/只） */
    @TableField("unit")
    @Schema(description = "单位", example = "份")
    private String unit;

    /** 售价（分） */
    @TableField("sale_price")
    @Schema(description = "售价（分）", example = "3800")
    private Long salePrice;

    /** 成本价（分），用于计算毛利 */
    @TableField("cost_price")
    @Schema(description = "成本价（分）", example = "1500")
    private Long costPrice;

    /** 库存数量（可选，用于限量菜品） */
    @TableField("stock")
    @Schema(description = "库存数量", example = "100")
    private Integer stock;

    /** 最低库存预警 */
    @TableField("min_stock")
    @Schema(description = "最低库存预警", example = "10")
    private Integer minStock;

    /** 菜品图片URL */
    @TableField("image_url")
    @Schema(description = "菜品图片URL")
    private String imageUrl;

    /** 菜品描述 */
    @TableField("description")
    @Schema(description = "菜品描述")
    private String description;

    /** 制作时间（分钟） */
    @TableField("cooking_time")
    @Schema(description = "制作时间（分钟）", example = "15")
    private Integer cookingTime;

    /** 状态：1在售 0停售 2售罄（遵循项目统一状态标准） */
    @TableField("status")
    @Schema(description = "状态: 1在售 0停售 2售罄", example = "1")
    private Integer status;

    /** 是否推荐 */
    @TableField("is_recommend")
    @Schema(description = "是否推荐")
    private Boolean isRecommend;

    /** 是否辣 */
    @TableField("is_spicy")
    @Schema(description = "是否辣")
    private Boolean isSpicy;

    /** 排序权重 */
    @TableField("sort_order")
    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;

    // Getter方法
    public Long getFoodId() { return foodId; }
    public String getFoodCode() { return foodCode; }
    public String getFoodName() { return foodName; }
    public Long getCategoryId() { return categoryId; }
    public String getSpecification() { return specification; }
    public String getUnit() { return unit; }
    public Long getSalePrice() { return salePrice; }
    public Long getCostPrice() { return costPrice; }
    public Integer getStock() { return stock; }
    public Integer getMinStock() { return minStock; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public Integer getCookingTime() { return cookingTime; }
    public Integer getStatus() { return status; }
    public Boolean getIsRecommend() { return isRecommend; }
    public Boolean getIsSpicy() { return isSpicy; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }
    public Integer getVersion() { return version; }

    // Setter方法
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public void setFoodCode(String foodCode) { this.foodCode = foodCode; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setSpecification(String specification) { this.specification = specification; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setSalePrice(Long salePrice) { this.salePrice = salePrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDescription(String description) { this.description = description; }
    public void setCookingTime(Integer cookingTime) { this.cookingTime = cookingTime; }
    public void setStatus(Integer status) { this.status = status; }
    public void setIsRecommend(Boolean isRecommend) { this.isRecommend = isRecommend; }
    public void setIsSpicy(Boolean isSpicy) { this.isSpicy = isSpicy; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setVersion(Integer version) { this.version = version; }
}
