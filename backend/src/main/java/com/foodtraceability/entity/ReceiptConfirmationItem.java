package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收货确认单明细表实体类
 * 用于管理收货确认单中的商品明细及批次信息
 */
@TableName("receipt_confirmation_items")
@Schema(description = "收货确认单明细表实体")
public class ReceiptConfirmationItem {

    /**
     * 收货确认明细主键ID（自增）
     */
    @TableId(value = "confirmation_item_id", type = IdType.AUTO)
    @Schema(description = "收货确认明细主键ID", example = "1")
    private Long confirmationItemId;

    /**
     * 关联收货确认单ID
     */
    @TableField("confirmation_id")
    @Schema(description = "关联收货确认单ID", example = "1")
    private Long confirmationId;

    /**
     * 关联到货明细ID
     */
    @TableField("arrival_item_id")
    @Schema(description = "关联到货明细ID", example = "1")
    private Long arrivalItemId;

    /**
     * 关联采购订单明细ID
     */
    @TableField("order_item_id")
    @Schema(description = "关联采购订单明细ID", example = "1")
    private Long orderItemId;

    /**
     * 物料ID
     */
    @TableField("material_id")
    @Schema(description = "物料ID", example = "1")
    private Long materialId;

    /**
     * 物料名称
     */
    @TableField("material_name")
    @Schema(description = "物料名称")
    private String materialName;

    /**
     * 规格
     */
    @TableField("specification")
    @Schema(description = "规格")
    private String specification;

    /**
     * 单位
     */
    @TableField("unit")
    @Schema(description = "单位")
    private String unit;

    /**
     * 确认收货数量
     */
    @TableField("confirmed_quantity")
    @Schema(description = "确认收货数量", example = "50.000")
    private BigDecimal confirmedQuantity;

    /**
     * 拒收数量
     */
    @TableField("rejected_quantity")
    @Schema(description = "拒收数量", example = "0.000")
    private BigDecimal rejectedQuantity;

    /**
     * 单价（单位：分）
     */
    @TableField("unit_price")
    @Schema(description = "单价（分）", example = "300")
    private Long unitPrice;

    /**
     * 金额（单位：分）
     */
    @TableField("amount")
    @Schema(description = "金额（分）", example = "15000")
    private Long amount;

    /**
     * 批次号
     */
    @TableField("batch_no")
    @Schema(description = "批次号")
    private String batchNo;

    /**
     * 生产日期
     */
    @TableField("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getConfirmationItemId() {
        return confirmationItemId;
    }

    public void setConfirmationItemId(Long confirmationItemId) {
        this.confirmationItemId = confirmationItemId;
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public Long getArrivalItemId() {
        return arrivalItemId;
    }

    public void setArrivalItemId(Long arrivalItemId) {
        this.arrivalItemId = arrivalItemId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
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

    public BigDecimal getConfirmedQuantity() {
        return confirmedQuantity;
    }

    public void setConfirmedQuantity(BigDecimal confirmedQuantity) {
        this.confirmedQuantity = confirmedQuantity;
    }

    public BigDecimal getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(BigDecimal rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
