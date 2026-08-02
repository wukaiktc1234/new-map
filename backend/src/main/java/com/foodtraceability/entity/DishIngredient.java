package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 菜品-配料关联实体类
 * 
 * @author demo
 * @since 2025-12-29
 */
@TableName("dish_ingredient")
public class DishIngredient {
    /**
     * 关联ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 菜品ID
     */
    @TableField("dish_id")
    private String dishId;
    /**
     * 菜品名称
     */
    @TableField("dish_name")
    private String dishName;
    /**
     * 配料ID
     */
    @TableField("ingredient_id")
    private String ingredientId;
    /**
     * 配料名称
     */
    @TableField("ingredient_name")
    private String ingredientName;
    /**
     * 配料编码
     */
    @TableField("ingredient_code")
    private String ingredientCode;
    /**
     * 用量
     */
    @TableField("quantity")
    private BigDecimal quantity;
    /**
     * 单位
     */
    @TableField("unit")
    private String unit;
    /**
     * 配料成本价（元）
     */
    @TableField("cost_price")
    private BigDecimal costPrice;
    /**
     * 总成本（元）
     */
    @TableField("total_cost")
    private BigDecimal totalCost;
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;
    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public DishIngredient() {
    }

    /**
     * 关联ID，主键
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 菜品ID
     */
    public String getDishId() {
        return this.dishId;
    }

    /**
     * 菜品名称
     */
    public String getDishName() {
        return this.dishName;
    }

    /**
     * 配料ID
     */
    public String getIngredientId() {
        return this.ingredientId;
    }

    /**
     * 配料名称
     */
    public String getIngredientName() {
        return this.ingredientName;
    }

    /**
     * 配料编码
     */
    public String getIngredientCode() {
        return this.ingredientCode;
    }

    /**
     * 用量
     */
    public BigDecimal getQuantity() {
        return this.quantity;
    }

    /**
     * 单位
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * 配料成本价（元）
     */
    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    /**
     * 总成本（元）
     */
    public BigDecimal getTotalCost() {
        return this.totalCost;
    }

    /**
     * 备注
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 创建时间
     */
    public Date getCreateTime() {
        return this.createTime;
    }

    /**
     * 更新时间
     */
    public Date getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 创建人
     */
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 更新人
     */
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 关联ID，主键
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 菜品ID
     */
    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    /**
     * 菜品名称
     */
    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    /**
     * 配料ID
     */
    public void setIngredientId(final String ingredientId) {
        this.ingredientId = ingredientId;
    }

    /**
     * 配料名称
     */
    public void setIngredientName(final String ingredientName) {
        this.ingredientName = ingredientName;
    }

    /**
     * 配料编码
     */
    public void setIngredientCode(final String ingredientCode) {
        this.ingredientCode = ingredientCode;
    }

    /**
     * 用量
     */
    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * 单位
     */
    public void setUnit(final String unit) {
        this.unit = unit;
    }

    /**
     * 配料成本价（元）
     */
    public void setCostPrice(final BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    /**
     * 总成本（元）
     */
    public void setTotalCost(final BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    /**
     * 备注
     */
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(final Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 创建人
     */
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    /**
     * 更新人
     */
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishIngredient)) return false;
        final DishIngredient other = (DishIngredient) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
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
        final java.lang.Object this$costPrice = this.getCostPrice();
        final java.lang.Object other$costPrice = other.getCostPrice();
        if (this$costPrice == null ? other$costPrice != null : !this$costPrice.equals(other$costPrice)) return false;
        final java.lang.Object this$totalCost = this.getTotalCost();
        final java.lang.Object other$totalCost = other.getTotalCost();
        if (this$totalCost == null ? other$totalCost != null : !this$totalCost.equals(other$totalCost)) return false;
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
        return other instanceof DishIngredient;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
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
        final java.lang.Object $costPrice = this.getCostPrice();
        result = result * PRIME + ($costPrice == null ? 43 : $costPrice.hashCode());
        final java.lang.Object $totalCost = this.getTotalCost();
        result = result * PRIME + ($totalCost == null ? 43 : $totalCost.hashCode());
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
        return "DishIngredient(id=" + this.getId() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", ingredientId=" + this.getIngredientId() + ", ingredientName=" + this.getIngredientName() + ", ingredientCode=" + this.getIngredientCode() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", costPrice=" + this.getCostPrice() + ", totalCost=" + this.getTotalCost() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
