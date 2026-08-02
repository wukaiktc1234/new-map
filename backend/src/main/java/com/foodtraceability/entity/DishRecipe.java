package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 菜品配方实体类
 * 用于定义菜品的标准原料配比
 */
@TableName("dish_recipe")
@Schema(description = "菜品配方实体")
public class DishRecipe {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("recipe_id")
    @Schema(description = "配方ID", example = "DR20250101001")
    private String recipeId;
    @TableField("dish_id")
    @Schema(description = "菜品ID", example = "D001")
    private String dishId;
    @TableField("dish_name")
    @Schema(description = "菜品名称", example = "宫保鸡丁")
    private String dishName;
    @TableField("dish_code")
    @Schema(description = "菜品编码", example = "D001")
    private String dishCode;
    @TableField("ingredient_id")
    @Schema(description = "原料ID", example = "I001")
    private String ingredientId;
    @TableField("ingredient_name")
    @Schema(description = "原料名称", example = "鸡肉")
    private String ingredientName;
    @TableField("ingredient_code")
    @Schema(description = "原料编码", example = "I001")
    private String ingredientCode;
    @TableField("quantity")
    @Schema(description = "标准用量", example = "0.500")
    private BigDecimal quantity;
    @TableField("unit")
    @Schema(description = "单位", example = "kg")
    private String unit;
    @TableField("allow_variance")
    @Schema(description = "允许误差百分比", example = "10.00")
    private BigDecimal allowVariance;
    @TableField("estimated_cost")
    @Schema(description = "预估成本", example = "15.00")
    private BigDecimal estimatedCost;
    @TableField("actual_cost")
    @Schema(description = "实际成本", example = "15.50")
    private BigDecimal actualCost;
    @TableField("is_required")
    @Schema(description = "是否必需：0-可选, 1-必需", example = "1")
    private Integer isRequired;
    @TableField("sort_order")
    @Schema(description = "排序", example = "1")
    private Integer sortOrder;
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
    @TableField("create_by")
    @Schema(description = "创建人")
    private String createBy;
    @TableField("update_by")
    @Schema(description = "更新人")
    private String updateBy;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记：0-未删除, 1-已删除")
    private Integer deleted;

    public DishRecipe() {
    }

    public Long getId() {
        return this.id;
    }

    public String getRecipeId() {
        return this.recipeId;
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public String getDishCode() {
        return this.dishCode;
    }

    public String getIngredientId() {
        return this.ingredientId;
    }

    public String getIngredientName() {
        return this.ingredientName;
    }

    public String getIngredientCode() {
        return this.ingredientCode;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getAllowVariance() {
        return this.allowVariance;
    }

    public BigDecimal getEstimatedCost() {
        return this.estimatedCost;
    }

    public BigDecimal getActualCost() {
        return this.actualCost;
    }

    public Integer getIsRequired() {
        return this.isRequired;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public String getRemark() {
        return this.remark;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public Date getUpdateTime() {
        return this.updateTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setRecipeId(final String recipeId) {
        this.recipeId = recipeId;
    }

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setDishCode(final String dishCode) {
        this.dishCode = dishCode;
    }

    public void setIngredientId(final String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setIngredientName(final String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public void setIngredientCode(final String ingredientCode) {
        this.ingredientCode = ingredientCode;
    }

    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setAllowVariance(final BigDecimal allowVariance) {
        this.allowVariance = allowVariance;
    }

    public void setEstimatedCost(final BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public void setActualCost(final BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public void setIsRequired(final Integer isRequired) {
        this.isRequired = isRequired;
    }

    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishRecipe)) return false;
        final DishRecipe other = (DishRecipe) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$isRequired = this.getIsRequired();
        final java.lang.Object other$isRequired = other.getIsRequired();
        if (this$isRequired == null ? other$isRequired != null : !this$isRequired.equals(other$isRequired)) return false;
        final java.lang.Object this$sortOrder = this.getSortOrder();
        final java.lang.Object other$sortOrder = other.getSortOrder();
        if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$recipeId = this.getRecipeId();
        final java.lang.Object other$recipeId = other.getRecipeId();
        if (this$recipeId == null ? other$recipeId != null : !this$recipeId.equals(other$recipeId)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$dishCode = this.getDishCode();
        final java.lang.Object other$dishCode = other.getDishCode();
        if (this$dishCode == null ? other$dishCode != null : !this$dishCode.equals(other$dishCode)) return false;
        final java.lang.Object this$ingredientId = this.getIngredientId();
        final java.lang.Object other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) return false;
        final java.lang.Object this$ingredientName = this.getIngredientName();
        final java.lang.Object other$ingredientName = other.getIngredientName();
        if (this$ingredientName == null ? other$ingredientName != null : !this$ingredientName.equals(other$ingredientName)) return false;
        final java.lang.Object this$ingredientCode = this.getIngredientCode();
        final java.lang.Object other$ingredientCode = other.getIngredientCode();
        if (this$ingredientCode == null ? other$ingredientCode != null : !this$ingredientCode.equals(other$ingredientCode)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$allowVariance = this.getAllowVariance();
        final java.lang.Object other$allowVariance = other.getAllowVariance();
        if (this$allowVariance == null ? other$allowVariance != null : !this$allowVariance.equals(other$allowVariance)) return false;
        final java.lang.Object this$estimatedCost = this.getEstimatedCost();
        final java.lang.Object other$estimatedCost = other.getEstimatedCost();
        if (this$estimatedCost == null ? other$estimatedCost != null : !this$estimatedCost.equals(other$estimatedCost)) return false;
        final java.lang.Object this$actualCost = this.getActualCost();
        final java.lang.Object other$actualCost = other.getActualCost();
        if (this$actualCost == null ? other$actualCost != null : !this$actualCost.equals(other$actualCost)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DishRecipe;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $isRequired = this.getIsRequired();
        result = result * PRIME + ($isRequired == null ? 43 : $isRequired.hashCode());
        final java.lang.Object $sortOrder = this.getSortOrder();
        result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $recipeId = this.getRecipeId();
        result = result * PRIME + ($recipeId == null ? 43 : $recipeId.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $dishCode = this.getDishCode();
        result = result * PRIME + ($dishCode == null ? 43 : $dishCode.hashCode());
        final java.lang.Object $ingredientId = this.getIngredientId();
        result = result * PRIME + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        final java.lang.Object $ingredientName = this.getIngredientName();
        result = result * PRIME + ($ingredientName == null ? 43 : $ingredientName.hashCode());
        final java.lang.Object $ingredientCode = this.getIngredientCode();
        result = result * PRIME + ($ingredientCode == null ? 43 : $ingredientCode.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $allowVariance = this.getAllowVariance();
        result = result * PRIME + ($allowVariance == null ? 43 : $allowVariance.hashCode());
        final java.lang.Object $estimatedCost = this.getEstimatedCost();
        result = result * PRIME + ($estimatedCost == null ? 43 : $estimatedCost.hashCode());
        final java.lang.Object $actualCost = this.getActualCost();
        result = result * PRIME + ($actualCost == null ? 43 : $actualCost.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DishRecipe(id=" + this.getId() + ", recipeId=" + this.getRecipeId() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", dishCode=" + this.getDishCode() + ", ingredientId=" + this.getIngredientId() + ", ingredientName=" + this.getIngredientName() + ", ingredientCode=" + this.getIngredientCode() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", allowVariance=" + this.getAllowVariance() + ", estimatedCost=" + this.getEstimatedCost() + ", actualCost=" + this.getActualCost() + ", isRequired=" + this.getIsRequired() + ", sortOrder=" + this.getSortOrder() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
