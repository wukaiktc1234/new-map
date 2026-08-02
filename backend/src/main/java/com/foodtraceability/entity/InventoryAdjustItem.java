package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存调整单明细实体类
 * 记录每个物料的调整前数量、调整数量、调整后数量等
 */
@TableName("inventory_adjust_item")
public class InventoryAdjustItem {

    /**
     * 明细ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long adjustItemId;

    /**
     * 调整单ID
     */
    @TableField("adjust_id")
    private Long adjustId;

    /**
     * 物料ID
     */
    @TableField("material_id")
    private String materialId;

    /**
     * 物料名称（冗余字段）
     */
    @TableField("material_name")
    private String materialName;

    /**
     * 规格型号
     */
    @TableField("specification")
    private String specification;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 调整前数量
     */
    @TableField("before_quantity")
    private BigDecimal beforeQuantity;

    /**
     * 调整数量（正数为增加，负数为减少）
     */
    @TableField("adjust_quantity")
    private BigDecimal adjustQuantity;

    /**
     * 调整后数量
     */
    @TableField("after_quantity")
    private BigDecimal afterQuantity;

    /**
     * 单价（分）
     */
    @TableField("unit_cost")
    private Long unitCost;

    /**
     * 调整金额（分）
     */
    @TableField("adjust_amount")
    private Long adjustAmount;

    /**
     * 批次号
     */
    @TableField("batch_no")
    private String batchNo;

    /**
     * 调整原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getAdjustItemId() {
        return adjustItemId;
    }

    public void setAdjustItemId(Long adjustItemId) {
        this.adjustItemId = adjustItemId;
    }

    public Long getAdjustId() {
        return adjustId;
    }

    public void setAdjustId(Long adjustId) {
        this.adjustId = adjustId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
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

    public BigDecimal getBeforeQuantity() {
        return beforeQuantity;
    }

    public void setBeforeQuantity(BigDecimal beforeQuantity) {
        this.beforeQuantity = beforeQuantity;
    }

    public BigDecimal getAdjustQuantity() {
        return adjustQuantity;
    }

    public void setAdjustQuantity(BigDecimal adjustQuantity) {
        this.adjustQuantity = adjustQuantity;
    }

    public BigDecimal getAfterQuantity() {
        return afterQuantity;
    }

    public void setAfterQuantity(BigDecimal afterQuantity) {
        this.afterQuantity = afterQuantity;
    }

    public Long getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Long unitCost) {
        this.unitCost = unitCost;
    }

    public Long getAdjustAmount() {
        return adjustAmount;
    }

    public void setAdjustAmount(Long adjustAmount) {
        this.adjustAmount = adjustAmount;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
