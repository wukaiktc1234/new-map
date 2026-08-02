package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品配方/BOM实体类
 * 定义菜品的原料配方和成本构成
 */
@TableName("dish_recipes")
@Schema(description = "菜品配方/BOM实体")
public class DishRecipeNew {

    /** 配方ID，主键自增 */
    @TableId(value = "recipe_id", type = IdType.AUTO)
    @Schema(description = "配方ID", example = "1")
    private Long recipeId;

    /** 关联菜品ID */
    @TableField("food_id")
    @Schema(description = "关联菜品ID", example = "1")
    private Long foodId;

    /** 原料ID（关联仓储的物料表） */
    @TableField("material_id")
    @Schema(description = "原料ID", example = "100")
    private Long materialId;

    /** 原料名称（冗余存储） */
    @TableField("material_name")
    @Schema(description = "原料名称", example = "五花肉")
    private String materialName;

    /** 规格 */
    @TableField("specification")
    @Schema(description = "规格", example = "500g/块")
    private String specification;

    /** 标准用量 */
    @TableField("required_quantity")
    @Schema(description = "标准用量", example = "200")
    private BigDecimal requiredQuantity;

    /** 单位 */
    @TableField("unit")
    @Schema(description = "单位", example = "克")
    private String unit;

    /** 损耗率(%) */
    @TableField("loss_rate")
    @Schema(description = "损耗率(%)", example = "5.00")
    private BigDecimal lossRate;

    /** 原料单价（分） */
    @TableField("unit_cost")
    @Schema(description = "原料单价（分）", example = "80")
    private Long unitCost;

    /** 该项成本（分） */
    @TableField("subtotal_cost")
    @Schema(description = "该项成本（分）", example = "160")
    private Long subtotalCost;

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
    public Long getRecipeId() { return recipeId; }
    public Long getFoodId() { return foodId; }
    public Long getMaterialId() { return materialId; }
    public String getMaterialName() { return materialName; }
    public String getSpecification() { return specification; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public String getUnit() { return unit; }
    public BigDecimal getLossRate() { return lossRate; }
    public Long getUnitCost() { return unitCost; }
    public Long getSubtotalCost() { return subtotalCost; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }
    public Integer getVersion() { return version; }

    // Setter方法
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public void setSpecification(String specification) { this.specification = specification; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }
    public void setUnitCost(Long unitCost) { this.unitCost = unitCost; }
    public void setSubtotalCost(Long subtotalCost) { this.subtotalCost = subtotalCost; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setVersion(Integer version) { this.version = version; }
}
