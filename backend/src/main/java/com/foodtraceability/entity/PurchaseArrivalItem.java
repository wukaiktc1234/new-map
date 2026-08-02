package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购到货单明细表实体类
 * 用于管理到货单中的商品明细信息
 */
@TableName("purchase_arrival_items")
@Schema(description = "采购到货单明细表实体")
public class PurchaseArrivalItem {

    /**
     * 到货明细主键ID（自增）
     */
    @TableId(value = "arrival_item_id", type = IdType.AUTO)
    @Schema(description = "到货明细主键ID", example = "1")
    private Long arrivalItemId;

    /**
     * 关联到货单ID
     */
    @TableField("arrival_id")
    @Schema(description = "关联到货单ID", example = "1")
    private Long arrivalId;

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
     * 计划到货数量
     */
    @TableField("expected_quantity")
    @Schema(description = "计划到货数量", example = "100.000")
    private BigDecimal expectedQuantity;

    /**
     * 已确认数量
     */
    @TableField("received_quantity")
    @Schema(description = "已确认数量", example = "50.000")
    private BigDecimal receivedQuantity;

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

    public Long getArrivalItemId() {
        return arrivalItemId;
    }

    public void setArrivalItemId(Long arrivalItemId) {
        this.arrivalItemId = arrivalItemId;
    }

    public Long getArrivalId() {
        return arrivalId;
    }

    public void setArrivalId(Long arrivalId) {
        this.arrivalId = arrivalId;
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

    public BigDecimal getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(BigDecimal expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
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
