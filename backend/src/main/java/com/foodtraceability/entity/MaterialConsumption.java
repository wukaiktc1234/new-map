package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 原料消耗记录实体类
 * 用于记录后厨制作过程中原料的消耗情况
 */
@TableName("material_consumption")
@Schema(description = "原料消耗记录实体")
public class MaterialConsumption {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("consumption_id")
    @Schema(description = "消耗记录ID", example = "MC20250101001")
    private String consumptionId;
    @TableField("kitchen_order_id")
    @Schema(description = "后厨订单ID", example = "KO20250101001")
    private String kitchenOrderId;
    @TableField("order_id")
    @Schema(description = "订单ID", example = "O20250101001")
    private String orderId;
    @TableField("order_number")
    @Schema(description = "订单编号", example = "ORD202501010001")
    private String orderNumber;
    @TableField("material_trace_code_id")
    @Schema(description = "原料追溯码ID", example = "MTC20250101001")
    private String materialTraceCodeId;
    @TableField("material_trace_code")
    @Schema(description = "原料追溯码", example = "MTC20250101001ABC")
    private String materialTraceCode;
    @TableField("product_id")
    @Schema(description = "商品ID")
    private Long productId;
    @TableField("product_name")
    @Schema(description = "商品名称", example = "有机苹果")
    private String productName;
    @TableField("product_code")
    @Schema(description = "商品编码", example = "P001")
    private String productCode;
    @TableField("consume_quantity")
    @Schema(description = "消耗数量", example = "0.500")
    private BigDecimal consumeQuantity;
    @TableField("unit")
    @Schema(description = "单位", example = "kg")
    private String unit;
    @TableField("unit_cost")
    @Schema(description = "单位成本", example = "15.50")
    private BigDecimal unitCost;
    @TableField("total_cost")
    @Schema(description = "总成本", example = "7.75")
    private BigDecimal totalCost;
    @TableField("dish_id")
    @Schema(description = "菜品ID", example = "D001")
    private String dishId;
    @TableField("dish_name")
    @Schema(description = "菜品名称", example = "宫保鸡丁")
    private String dishName;
    @TableField("consume_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "消耗时间")
    private LocalDateTime consumeTime;
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private Long operatorId;
    @TableField("operator_name")
    @Schema(description = "操作人姓名", example = "李师傅")
    private String operatorName;
    @TableField("scan_device")
    @Schema(description = "扫码设备", example = "扫码枪1号")
    private String scanDevice;
    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;
    @TableField("store_name")
    @Schema(description = "门店名称", example = "总店")
    private String storeName;
    @TableField("inventory_deducted")
    @Schema(description = "库存是否已扣减：0-未扣减, 1-已扣减", example = "0")
    private Integer inventoryDeducted;
    @TableField("inventory_deduct_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "库存扣减时间")
    private LocalDateTime inventoryDeductTime;
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createBy;
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人")
    private String updateBy;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记：0-未删除, 1-已删除")
    private Integer deleted;

    public MaterialConsumption() {
    }

    public Long getId() {
        return this.id;
    }

    public String getConsumptionId() {
        return this.consumptionId;
    }

    public String getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getMaterialTraceCodeId() {
        return this.materialTraceCodeId;
    }

    public String getMaterialTraceCode() {
        return this.materialTraceCode;
    }

    public Long getProductId() {
        return this.productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public BigDecimal getConsumeQuantity() {
        return this.consumeQuantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getUnitCost() {
        return this.unitCost;
    }

    public BigDecimal getTotalCost() {
        return this.totalCost;
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public LocalDateTime getConsumeTime() {
        return this.consumeTime;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public String getScanDevice() {
        return this.scanDevice;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public Integer getInventoryDeducted() {
        return this.inventoryDeducted;
    }

    public LocalDateTime getInventoryDeductTime() {
        return this.inventoryDeductTime;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setConsumptionId(final String consumptionId) {
        this.consumptionId = consumptionId;
    }

    public void setKitchenOrderId(final String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setMaterialTraceCodeId(final String materialTraceCodeId) {
        this.materialTraceCodeId = materialTraceCodeId;
    }

    public void setMaterialTraceCode(final String materialTraceCode) {
        this.materialTraceCode = materialTraceCode;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setProductCode(final String productCode) {
        this.productCode = productCode;
    }

    public void setConsumeQuantity(final BigDecimal consumeQuantity) {
        this.consumeQuantity = consumeQuantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setUnitCost(final BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public void setTotalCost(final BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setConsumeTime(final LocalDateTime consumeTime) {
        this.consumeTime = consumeTime;
    }

    public void setOperatorId(final Long operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setScanDevice(final String scanDevice) {
        this.scanDevice = scanDevice;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setInventoryDeducted(final Integer inventoryDeducted) {
        this.inventoryDeducted = inventoryDeducted;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setInventoryDeductTime(final LocalDateTime inventoryDeductTime) {
        this.inventoryDeductTime = inventoryDeductTime;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MaterialConsumption)) return false;
        final MaterialConsumption other = (MaterialConsumption) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$inventoryDeducted = this.getInventoryDeducted();
        final java.lang.Object other$inventoryDeducted = other.getInventoryDeducted();
        if (this$inventoryDeducted == null ? other$inventoryDeducted != null : !this$inventoryDeducted.equals(other$inventoryDeducted)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$consumptionId = this.getConsumptionId();
        final java.lang.Object other$consumptionId = other.getConsumptionId();
        if (this$consumptionId == null ? other$consumptionId != null : !this$consumptionId.equals(other$consumptionId)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$materialTraceCodeId = this.getMaterialTraceCodeId();
        final java.lang.Object other$materialTraceCodeId = other.getMaterialTraceCodeId();
        if (this$materialTraceCodeId == null ? other$materialTraceCodeId != null : !this$materialTraceCodeId.equals(other$materialTraceCodeId)) return false;
        final java.lang.Object this$materialTraceCode = this.getMaterialTraceCode();
        final java.lang.Object other$materialTraceCode = other.getMaterialTraceCode();
        if (this$materialTraceCode == null ? other$materialTraceCode != null : !this$materialTraceCode.equals(other$materialTraceCode)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$productCode = this.getProductCode();
        final java.lang.Object other$productCode = other.getProductCode();
        if (this$productCode == null ? other$productCode != null : !this$productCode.equals(other$productCode)) return false;
        final java.lang.Object this$consumeQuantity = this.getConsumeQuantity();
        final java.lang.Object other$consumeQuantity = other.getConsumeQuantity();
        if (this$consumeQuantity == null ? other$consumeQuantity != null : !this$consumeQuantity.equals(other$consumeQuantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$unitCost = this.getUnitCost();
        final java.lang.Object other$unitCost = other.getUnitCost();
        if (this$unitCost == null ? other$unitCost != null : !this$unitCost.equals(other$unitCost)) return false;
        final java.lang.Object this$totalCost = this.getTotalCost();
        final java.lang.Object other$totalCost = other.getTotalCost();
        if (this$totalCost == null ? other$totalCost != null : !this$totalCost.equals(other$totalCost)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$consumeTime = this.getConsumeTime();
        final java.lang.Object other$consumeTime = other.getConsumeTime();
        if (this$consumeTime == null ? other$consumeTime != null : !this$consumeTime.equals(other$consumeTime)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$scanDevice = this.getScanDevice();
        final java.lang.Object other$scanDevice = other.getScanDevice();
        if (this$scanDevice == null ? other$scanDevice != null : !this$scanDevice.equals(other$scanDevice)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$inventoryDeductTime = this.getInventoryDeductTime();
        final java.lang.Object other$inventoryDeductTime = other.getInventoryDeductTime();
        if (this$inventoryDeductTime == null ? other$inventoryDeductTime != null : !this$inventoryDeductTime.equals(other$inventoryDeductTime)) return false;
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
        return other instanceof MaterialConsumption;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $inventoryDeducted = this.getInventoryDeducted();
        result = result * PRIME + ($inventoryDeducted == null ? 43 : $inventoryDeducted.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $consumptionId = this.getConsumptionId();
        result = result * PRIME + ($consumptionId == null ? 43 : $consumptionId.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $materialTraceCodeId = this.getMaterialTraceCodeId();
        result = result * PRIME + ($materialTraceCodeId == null ? 43 : $materialTraceCodeId.hashCode());
        final java.lang.Object $materialTraceCode = this.getMaterialTraceCode();
        result = result * PRIME + ($materialTraceCode == null ? 43 : $materialTraceCode.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $productCode = this.getProductCode();
        result = result * PRIME + ($productCode == null ? 43 : $productCode.hashCode());
        final java.lang.Object $consumeQuantity = this.getConsumeQuantity();
        result = result * PRIME + ($consumeQuantity == null ? 43 : $consumeQuantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $unitCost = this.getUnitCost();
        result = result * PRIME + ($unitCost == null ? 43 : $unitCost.hashCode());
        final java.lang.Object $totalCost = this.getTotalCost();
        result = result * PRIME + ($totalCost == null ? 43 : $totalCost.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $consumeTime = this.getConsumeTime();
        result = result * PRIME + ($consumeTime == null ? 43 : $consumeTime.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $scanDevice = this.getScanDevice();
        result = result * PRIME + ($scanDevice == null ? 43 : $scanDevice.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $inventoryDeductTime = this.getInventoryDeductTime();
        result = result * PRIME + ($inventoryDeductTime == null ? 43 : $inventoryDeductTime.hashCode());
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
        return "MaterialConsumption(id=" + this.getId() + ", consumptionId=" + this.getConsumptionId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", materialTraceCodeId=" + this.getMaterialTraceCodeId() + ", materialTraceCode=" + this.getMaterialTraceCode() + ", productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", productCode=" + this.getProductCode() + ", consumeQuantity=" + this.getConsumeQuantity() + ", unit=" + this.getUnit() + ", unitCost=" + this.getUnitCost() + ", totalCost=" + this.getTotalCost() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", consumeTime=" + this.getConsumeTime() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", scanDevice=" + this.getScanDevice() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", inventoryDeducted=" + this.getInventoryDeducted() + ", inventoryDeductTime=" + this.getInventoryDeductTime() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
