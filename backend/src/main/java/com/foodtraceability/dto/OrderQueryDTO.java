package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * POS订单查询DTO
 */
@Schema(description = "POS订单查询DTO")
public class OrderQueryDTO {
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "取餐号")
    private String pickupNumber;
    @Schema(description = "取餐码（外卖使用）")
    private String pickupCode;
    @Schema(description = "创建时间")
    private String createTime;
    @Schema(description = "项目数量")
    private Integer itemCount;
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @Schema(description = "支付方式")
    private String paymentMethod;
    @Schema(description = "交易流水号")
    private String transactionId;
    @Schema(description = "状态")
    private String status;

    public OrderQueryDTO() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getPickupNumber() {
        return this.pickupNumber;
    }

    public String getPickupCode() {
        return this.pickupCode;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public Integer getItemCount() {
        return this.itemCount;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getStatus() {
        return this.status;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setPickupNumber(final String pickupNumber) {
        this.pickupNumber = pickupNumber;
    }

    public void setPickupCode(final String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public void setCreateTime(final String createTime) {
        this.createTime = createTime;
    }

    public void setItemCount(final Integer itemCount) {
        this.itemCount = itemCount;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderQueryDTO)) return false;
        final OrderQueryDTO other = (OrderQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$itemCount = this.getItemCount();
        final java.lang.Object other$itemCount = other.getItemCount();
        if (this$itemCount == null ? other$itemCount != null : !this$itemCount.equals(other$itemCount)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$pickupNumber = this.getPickupNumber();
        final java.lang.Object other$pickupNumber = other.getPickupNumber();
        if (this$pickupNumber == null ? other$pickupNumber != null : !this$pickupNumber.equals(other$pickupNumber)) return false;
        final java.lang.Object this$pickupCode = this.getPickupCode();
        final java.lang.Object other$pickupCode = other.getPickupCode();
        if (this$pickupCode == null ? other$pickupCode != null : !this$pickupCode.equals(other$pickupCode)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$transactionId = this.getTransactionId();
        final java.lang.Object other$transactionId = other.getTransactionId();
        if (this$transactionId == null ? other$transactionId != null : !this$transactionId.equals(other$transactionId)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $itemCount = this.getItemCount();
        result = result * PRIME + ($itemCount == null ? 43 : $itemCount.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $pickupNumber = this.getPickupNumber();
        result = result * PRIME + ($pickupNumber == null ? 43 : $pickupNumber.hashCode());
        final java.lang.Object $pickupCode = this.getPickupCode();
        result = result * PRIME + ($pickupCode == null ? 43 : $pickupCode.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $transactionId = this.getTransactionId();
        result = result * PRIME + ($transactionId == null ? 43 : $transactionId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderQueryDTO(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", pickupNumber=" + this.getPickupNumber() + ", pickupCode=" + this.getPickupCode() + ", createTime=" + this.getCreateTime() + ", itemCount=" + this.getItemCount() + ", totalAmount=" + this.getTotalAmount() + ", paymentMethod=" + this.getPaymentMethod() + ", transactionId=" + this.getTransactionId() + ", status=" + this.getStatus() + ")";
    }
}
