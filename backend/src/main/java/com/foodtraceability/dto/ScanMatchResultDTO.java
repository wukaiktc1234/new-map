package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "扫码匹配结果DTO")
public class ScanMatchResultDTO {
    @Schema(description = "是否匹配成功")
    private Boolean success;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "追溯码")
    private String traceCode;
    @Schema(description = "原料名称")
    private String materialName;
    @Schema(description = "使用数量")
    private BigDecimal usedQuantity;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "匹配的订单ID")
    private String orderId;
    @Schema(description = "匹配的后厨订单ID")
    private String kitchenOrderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "匹配的菜品名称")
    private String dishName;
    @Schema(description = "订单状态变更")
    private String orderStatusChange;
    @Schema(description = "剩余可用数量")
    private BigDecimal remainingQuantity;
    @Schema(description = "是否触发订单制作")
    private Boolean triggeredMaking;
    @Schema(description = "是否自动完成制作")
    private Boolean triggeredComplete;
    @Schema(description = "原料完成进度")
    private String materialProgress;

    public static ScanMatchResultDTO success(String message) {
        ScanMatchResultDTO result = new ScanMatchResultDTO();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static ScanMatchResultDTO fail(String message) {
        ScanMatchResultDTO result = new ScanMatchResultDTO();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public ScanMatchResultDTO() {
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public BigDecimal getUsedQuantity() {
        return this.usedQuantity;
    }

    public String getUnit() {
        return this.unit;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getDishName() {
        return this.dishName;
    }

    public String getOrderStatusChange() {
        return this.orderStatusChange;
    }

    public BigDecimal getRemainingQuantity() {
        return this.remainingQuantity;
    }

    public Boolean getTriggeredMaking() {
        return this.triggeredMaking;
    }

    public Boolean getTriggeredComplete() {
        return this.triggeredComplete;
    }

    public String getMaterialProgress() {
        return this.materialProgress;
    }

    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setUsedQuantity(final BigDecimal usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setKitchenOrderId(final String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setOrderStatusChange(final String orderStatusChange) {
        this.orderStatusChange = orderStatusChange;
    }

    public void setRemainingQuantity(final BigDecimal remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public void setTriggeredMaking(final Boolean triggeredMaking) {
        this.triggeredMaking = triggeredMaking;
    }

    public void setTriggeredComplete(final Boolean triggeredComplete) {
        this.triggeredComplete = triggeredComplete;
    }

    public void setMaterialProgress(final String materialProgress) {
        this.materialProgress = materialProgress;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ScanMatchResultDTO)) return false;
        final ScanMatchResultDTO other = (ScanMatchResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$success = this.getSuccess();
        final java.lang.Object other$success = other.getSuccess();
        if (this$success == null ? other$success != null : !this$success.equals(other$success)) return false;
        final java.lang.Object this$triggeredMaking = this.getTriggeredMaking();
        final java.lang.Object other$triggeredMaking = other.getTriggeredMaking();
        if (this$triggeredMaking == null ? other$triggeredMaking != null : !this$triggeredMaking.equals(other$triggeredMaking)) return false;
        final java.lang.Object this$triggeredComplete = this.getTriggeredComplete();
        final java.lang.Object other$triggeredComplete = other.getTriggeredComplete();
        if (this$triggeredComplete == null ? other$triggeredComplete != null : !this$triggeredComplete.equals(other$triggeredComplete)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$usedQuantity = this.getUsedQuantity();
        final java.lang.Object other$usedQuantity = other.getUsedQuantity();
        if (this$usedQuantity == null ? other$usedQuantity != null : !this$usedQuantity.equals(other$usedQuantity)) return false;
        final java.lang.Object this$unit = this.getUnit();
        final java.lang.Object other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$orderStatusChange = this.getOrderStatusChange();
        final java.lang.Object other$orderStatusChange = other.getOrderStatusChange();
        if (this$orderStatusChange == null ? other$orderStatusChange != null : !this$orderStatusChange.equals(other$orderStatusChange)) return false;
        final java.lang.Object this$remainingQuantity = this.getRemainingQuantity();
        final java.lang.Object other$remainingQuantity = other.getRemainingQuantity();
        if (this$remainingQuantity == null ? other$remainingQuantity != null : !this$remainingQuantity.equals(other$remainingQuantity)) return false;
        final java.lang.Object this$materialProgress = this.getMaterialProgress();
        final java.lang.Object other$materialProgress = other.getMaterialProgress();
        if (this$materialProgress == null ? other$materialProgress != null : !this$materialProgress.equals(other$materialProgress)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ScanMatchResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $success = this.getSuccess();
        result = result * PRIME + ($success == null ? 43 : $success.hashCode());
        final java.lang.Object $triggeredMaking = this.getTriggeredMaking();
        result = result * PRIME + ($triggeredMaking == null ? 43 : $triggeredMaking.hashCode());
        final java.lang.Object $triggeredComplete = this.getTriggeredComplete();
        result = result * PRIME + ($triggeredComplete == null ? 43 : $triggeredComplete.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $usedQuantity = this.getUsedQuantity();
        result = result * PRIME + ($usedQuantity == null ? 43 : $usedQuantity.hashCode());
        final java.lang.Object $unit = this.getUnit();
        result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $orderStatusChange = this.getOrderStatusChange();
        result = result * PRIME + ($orderStatusChange == null ? 43 : $orderStatusChange.hashCode());
        final java.lang.Object $remainingQuantity = this.getRemainingQuantity();
        result = result * PRIME + ($remainingQuantity == null ? 43 : $remainingQuantity.hashCode());
        final java.lang.Object $materialProgress = this.getMaterialProgress();
        result = result * PRIME + ($materialProgress == null ? 43 : $materialProgress.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ScanMatchResultDTO(success=" + this.getSuccess() + ", message=" + this.getMessage() + ", traceCode=" + this.getTraceCode() + ", materialName=" + this.getMaterialName() + ", usedQuantity=" + this.getUsedQuantity() + ", unit=" + this.getUnit() + ", orderId=" + this.getOrderId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderNumber=" + this.getOrderNumber() + ", dishName=" + this.getDishName() + ", orderStatusChange=" + this.getOrderStatusChange() + ", remainingQuantity=" + this.getRemainingQuantity() + ", triggeredMaking=" + this.getTriggeredMaking() + ", triggeredComplete=" + this.getTriggeredComplete() + ", materialProgress=" + this.getMaterialProgress() + ")";
    }
}
