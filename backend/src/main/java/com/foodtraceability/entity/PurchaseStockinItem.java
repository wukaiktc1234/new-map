package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购入库明细实体类
 * 用于管理入库单中的商品明细信息，包含批次追溯信息
 */
@TableName("purchase_stockin_items")
@Schema(description = "采购入库明细实体")
public class PurchaseStockinItem {

    /**
     * 入库明细主键ID（自增）
     */
    @TableId(value = "stockin_item_id", type = IdType.AUTO)
    @Schema(description = "入库明细主键ID", example = "1")
    private Long stockinItemId;

    /**
     * 关联的入库单ID
     */
    @TableField("stockin_id")
    @Schema(description = "入库单ID", example = "1")
    private Long stockinId;

    /**
     * 关联的订单明细ID
     */
    @TableField("order_item_id")
    @Schema(description = "订单明细ID", example = "10")
    private Long orderItemId;

    /**
     * 物料ID
     */
    @TableField("material_id")
    @Schema(description = "物料ID", example = "100")
    private Long materialId;

    /**
     * 物料名称
     */
    @TableField("material_name")
    @Schema(description = "物料名称", example = "土豆")
    private String materialName;

    /**
     * 批次号
     */
    @TableField("batch_no")
    @Schema(description = "批次号", example = "B20260425001")
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
     * 实收数量
     */
    @TableField("actual_quantity")
    @Schema(description = "实收数量", example = "50.000")
    private BigDecimal actualQuantity;

    /**
     * 单位
     */
    @TableField("unit")
    @Schema(description = "单位", example = "斤")
    private String unit;

    /**
     * 入库单价（单位：分）
     */
    @TableField("unit_price")
    @Schema(description = "入库单价（分）", example = "300")
    private Long unitPrice;

    /**
     * 金额（单位：分）
     */
    @TableField("amount")
    @Schema(description = "金额（分）", example = "15000")
    private Long amount;

    /**
     * 库位ID
     */
    @TableField("location_id")
    @Schema(description = "库位ID", example = "5")
    private Long locationId;

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

    public Long getStockinItemId() {
        return stockinItemId;
    }

    public void setStockinItemId(Long stockinItemId) {
        this.stockinItemId = stockinItemId;
    }

    public Long getStockinId() {
        return stockinId;
    }

    public void setStockinId(Long stockinId) {
        this.stockinId = stockinId;
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

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
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
