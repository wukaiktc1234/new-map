package com.foodtraceability.dto;

/**
 * 从订单创建待取餐记录请求DTO
 */
public class CallNumberCreateFromOrderRequest {
    private String orderId;
    private String orderNumber;
    private String tableNumber;
    private String orderType;
    private Integer itemCount;

    public CallNumberCreateFromOrderRequest() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getOrderType() {
        return this.orderType;
    }

    public Integer getItemCount() {
        return this.itemCount;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setOrderType(final String orderType) {
        this.orderType = orderType;
    }

    public void setItemCount(final Integer itemCount) {
        this.itemCount = itemCount;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CallNumberCreateFromOrderRequest)) return false;
        final CallNumberCreateFromOrderRequest other = (CallNumberCreateFromOrderRequest) o;
        if (!other.canEqual(this)) return false;
        final Object this$itemCount = this.getItemCount();
        final Object other$itemCount = other.getItemCount();
        if (this$itemCount == null ? other$itemCount != null : !this$itemCount.equals(other$itemCount)) return false;
        final Object this$orderId = this.getOrderId();
        final Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final Object this$orderNumber = this.getOrderNumber();
        final Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final Object this$tableNumber = this.getTableNumber();
        final Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final Object this$orderType = this.getOrderType();
        final Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CallNumberCreateFromOrderRequest;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $itemCount = this.getItemCount();
        result = result * PRIME + ($itemCount == null ? 43 : $itemCount.hashCode());
        final Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "CallNumberCreateFromOrderRequest(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", tableNumber=" + this.getTableNumber() + ", orderType=" + this.getOrderType() + ", itemCount=" + this.getItemCount() + ")";
    }
}
