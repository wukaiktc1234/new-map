package com.foodtraceability.dto;

/**
 * 取餐请求DTO
 */
public class CallNumberPickupRequest {
    private String orderId;

    public CallNumberPickupRequest() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CallNumberPickupRequest)) return false;
        final CallNumberPickupRequest other = (CallNumberPickupRequest) o;
        if (!other.canEqual(this)) return false;
        final Object this$orderId = this.getOrderId();
        final Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CallNumberPickupRequest;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "CallNumberPickupRequest(orderId=" + this.getOrderId() + ")";
    }
}
