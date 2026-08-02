package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 套餐明细/配料实体类
 * 定义套餐包含的菜品及其配置
 */
@TableName("combo_ingredients")
@Schema(description = "套餐明细/配料实体")
public class ComboIngredientNew {

    /** 配料ID，主键自增 */
    @TableId(value = "ingredient_id", type = IdType.AUTO)
    @Schema(description = "配料ID", example = "1")
    private Long ingredientId;

    /** 关联套餐ID */
    @TableField("combo_id")
    @Schema(description = "关联套餐ID", example = "1")
    private Long comboId;

    /** 关联菜品ID */
    @TableField("food_id")
    @Schema(description = "关联菜品ID", example = "5")
    private Long foodId;

    /** 数量 */
    @TableField("quantity")
    @Schema(description = "数量", example = "1")
    private Integer quantity;

    /** 单位 */
    @TableField("unit")
    @Schema(description = "单位", example = "份")
    private String unit;

    /** 是否必选 */
    @TableField("is_required")
    @Schema(description = "是否必选")
    private Boolean isRequired;

    /** 最多可选几样（当isRequired为false时有效） */
    @TableField("max_select")
    @Schema(description = "最多可选几样", example = "2")
    private Integer maxSelect;

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

    // Getter方法
    public Long getIngredientId() { return ingredientId; }
    public Long getComboId() { return comboId; }
    public Long getFoodId() { return foodId; }
    public Integer getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Boolean getIsRequired() { return isRequired; }
    public Integer getMaxSelect() { return maxSelect; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public void setComboId(Long comboId) { this.comboId = comboId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
