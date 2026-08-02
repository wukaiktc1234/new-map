package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 菜品-配料关联DTO
 * 
 * @author demo
 * @since 2025-12-29
 */
@Schema(description = "菜品配料关联信息")
public class DishIngredientDTO {
    @Schema(description = "关联ID")
    private Long id;
    @Schema(description = "菜品ID")
    private Long dishId;
    @Schema(description = "菜品名称")
    private String dishName;
    @Schema(description = "配料ID")
    private String ingredientId;
    @Schema(description = "配料名称")
    private String ingredientName;
    @Schema(description = "配料编码")
    private String ingredientCode;
    @Schema(description = "用量")
    private BigDecimal quantity;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "配料成本价（元）")
    private BigDecimal costPrice;
    @Schema(description = "总成本（元）")
    private BigDecimal totalCost;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    @Schema(description = "创建人")
    private String createBy;
    @Schema(description = "更新人")
    private String updateBy;

    public DishIngredientDTO() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
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

    public BigDecimal getCostPrice() {
        return this.costPrice;
    }

    public BigDecimal getTotalCost() {
        return this.totalCost;
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

    public void setId(final Long id) {
        this.id = id;
    }

    public void setDishId(final Long dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
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

    public void setCostPrice(final BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setTotalCost(final BigDecimal totalCost) {
        this.totalCost = totalCost;
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

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DishIngredientDTO)) return false;
        final DishIngredientDTO other = (DishIngredientDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
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
        return other instanceof DishIngredientDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
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
        return "DishIngredientDTO(id=" + this.getId() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", ingredientId=" + this.getIngredientId() + ", ingredientName=" + this.getIngredientName() + ", ingredientCode=" + this.getIngredientCode() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", costPrice=" + this.getCostPrice() + ", totalCost=" + this.getTotalCost() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ")";
    }
}
