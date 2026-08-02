package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单明细实体类
 * 用于管理采购订单中的商品明细信息
 */
@TableName("purchase_order_items")
@Schema(description = "采购订单明细实体")
public class PurchaseOrderItem {

    /**
     * 明细主键ID（自增）
     */
    @TableId(value = "item_id", type = IdType.AUTO)
    @Schema(description = "明细主键ID", example = "1")
    private Long itemId;

    /**
     * 关联的采购订单ID
     */
    @TableField("order_id")
    @Schema(description = "采购订单ID", example = "1")
    private Long orderId;

    /**
     * 明细ID字符串（对外唯一标识）
     */
    @TableField("item_id_str")
    @Schema(description = "明细ID字符串")
    private String itemIdStr;

    /**
     * 物料ID（关联物料主数据）
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
     * 规格
     */
    @TableField("specification")
    @Schema(description = "规格", example = "大号/5kg装")
    private String specification;

    /**
     * 单位
     */
    @TableField("unit")
    @Schema(description = "单位", example = "斤")
    private String unit;

    /**
     * 采购数量
     */
    @TableField("quantity")
    @Schema(description = "采购数量", example = "100.000")
    private BigDecimal quantity;

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
    @Schema(description = "金额（分）", example = "30000")
    private Long amount;

    /**
     * 税率
     */
    @TableField("tax_rate")
    @Schema(description = "税率", example = "0.13")
    private BigDecimal taxRate;

    /**
     * 已收货数量
     */
    @TableField("received_quantity")
    @Schema(description = "已收货数量", example = "50.000")
    private BigDecimal receivedQuantity;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 计划收货方类型
     */
    @TableField("planned_receiver_type")
    @Schema(description = "计划收货方类型", example = "STORE")
    private String plannedReceiverType;

    /**
     * 计划收货门店ID
     */
    @TableField("planned_store_id")
    @Schema(description = "计划收货门店ID", example = "STORE001")
    private String plannedStoreId;

    /**
     * 计划收货仓库ID
     */
    @TableField("planned_warehouse_id")
    @Schema(description = "计划收货仓库ID", example = "1")
    private Long plannedWarehouseId;

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

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getItemIdStr() {
        return itemIdStr;
    }

    public void setItemIdStr(String itemIdStr) {
        this.itemIdStr = itemIdStr;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPlannedReceiverType() {
        return plannedReceiverType;
    }

    public void setPlannedReceiverType(String plannedReceiverType) {
        this.plannedReceiverType = plannedReceiverType;
    }

    public String getPlannedStoreId() {
        return plannedStoreId;
    }

    public void setPlannedStoreId(String plannedStoreId) {
        this.plannedStoreId = plannedStoreId;
    }

    public Long getPlannedWarehouseId() {
        return plannedWarehouseId;
    }

    public void setPlannedWarehouseId(Long plannedWarehouseId) {
        this.plannedWarehouseId = plannedWarehouseId;
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
