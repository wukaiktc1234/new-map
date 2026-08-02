package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动记录实体类
 * 用于记录所有库存变动操作，包括入库、出库、调拨等
 */
@TableName("inventory_transactions")
public class InventoryTransaction {
    /**
     * 变动记录ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long transactionId;

    /**
     * 变动类型（1:采购入库 2:销售出库 3:调拨出 4:调拨入 5:盘点盈 6:盘点亏 7:报损 8:退货）
     */
    @TableField("transaction_type")
    private Integer transactionType;

    /**
     * 库存ID
     */
    @TableField("inventory_id")
    private Long inventoryId;

    /**
     * 物料ID
     */
    @TableField("material_id")
    private Long materialId;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 变动数量（正数为增加，负数为减少）
     */
    @TableField("quantity_change")
    private BigDecimal quantityChange;

    /**
     * 变动前数量
     */
    @TableField("before_qty")
    private BigDecimal beforeQty;

    /**
     * 变动后数量
     */
    @TableField("after_qty")
    private BigDecimal afterQty;

    /**
     * 单位成本（分）
     */
    @TableField("unit_cost")
    private Long unitCost;

    /**
     * 总成本（分）
     */
    @TableField("total_cost")
    private Long totalCost;

    /**
     * 关联单据号
     */
    @TableField("reference_no")
    private String referenceNo;

    /**
     * 关联单据类型（订单/入库单/出库单）
     */
    @TableField("reference_type")
    private String referenceType;

    /**
     * 操作人ID
     */
    @TableField("create_user_id")
    private Long createUserId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getBeforeQty() {
        return beforeQty;
    }

    public void setBeforeQty(BigDecimal beforeQty) {
        this.beforeQty = beforeQty;
    }

    public BigDecimal getAfterQty() {
        return afterQty;
    }

    public void setAfterQty(BigDecimal afterQty) {
        this.afterQty = afterQty;
    }

    public Long getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Long unitCost) {
        this.unitCost = unitCost;
    }

    public Long getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Long totalCost) {
        this.totalCost = totalCost;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
