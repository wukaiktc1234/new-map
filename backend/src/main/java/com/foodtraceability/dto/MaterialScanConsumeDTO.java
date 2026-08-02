package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 原料扫码消耗DTO
 */
@Schema(description = "原料扫码消耗DTO")
public class MaterialScanConsumeDTO {
    @Schema(description = "原料追溯码")
    private String materialTraceCode;
    @Schema(description = "后厨订单ID")
    private String kitchenOrderId;
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "菜品ID")
    private String dishId;
    @Schema(description = "菜品名称")
    private String dishName;
    @Schema(description = "消耗数量")
    private BigDecimal consumeQuantity;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "操作人姓名")
    private String operatorName;
    @Schema(description = "扫码设备")
    private String scanDevice;
    @Schema(description = "门店ID")
    private Long storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "备注")
    private String remark;

    public MaterialScanConsumeDTO() {
    }

    public String getMaterialTraceCode() {
        return this.materialTraceCode;
    }

    public String getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public BigDecimal getConsumeQuantity() {
        return this.consumeQuantity;
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

    public String getRemark() {
        return this.remark;
    }

    public void setMaterialTraceCode(final String materialTraceCode) {
        this.materialTraceCode = materialTraceCode;
    }

    public void setKitchenOrderId(final String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setConsumeQuantity(final BigDecimal consumeQuantity) {
        this.consumeQuantity = consumeQuantity;
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

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MaterialScanConsumeDTO)) return false;
        final MaterialScanConsumeDTO other = (MaterialScanConsumeDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$materialTraceCode = this.getMaterialTraceCode();
        final java.lang.Object other$materialTraceCode = other.getMaterialTraceCode();
        if (this$materialTraceCode == null ? other$materialTraceCode != null : !this$materialTraceCode.equals(other$materialTraceCode)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$consumeQuantity = this.getConsumeQuantity();
        final java.lang.Object other$consumeQuantity = other.getConsumeQuantity();
        if (this$consumeQuantity == null ? other$consumeQuantity != null : !this$consumeQuantity.equals(other$consumeQuantity)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$scanDevice = this.getScanDevice();
        final java.lang.Object other$scanDevice = other.getScanDevice();
        if (this$scanDevice == null ? other$scanDevice != null : !this$scanDevice.equals(other$scanDevice)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MaterialScanConsumeDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $materialTraceCode = this.getMaterialTraceCode();
        result = result * PRIME + ($materialTraceCode == null ? 43 : $materialTraceCode.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $consumeQuantity = this.getConsumeQuantity();
        result = result * PRIME + ($consumeQuantity == null ? 43 : $consumeQuantity.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $scanDevice = this.getScanDevice();
        result = result * PRIME + ($scanDevice == null ? 43 : $scanDevice.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "MaterialScanConsumeDTO(materialTraceCode=" + this.getMaterialTraceCode() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderId=" + this.getOrderId() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", consumeQuantity=" + this.getConsumeQuantity() + ", operatorId=" + this.getOperatorId() + ", operatorName=" + this.getOperatorName() + ", scanDevice=" + this.getScanDevice() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", remark=" + this.getRemark() + ")";
    }
}
