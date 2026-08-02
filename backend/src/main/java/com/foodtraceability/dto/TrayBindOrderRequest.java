package com.foodtraceability.dto;

/**
 * 托盘绑定订单请求DTO
 */
public class TrayBindOrderRequest {
    private String trayCode;
    private String orderId;
    private Long kitchenOrderId;

    public TrayBindOrderRequest() {
    }

    public String getTrayCode() {
        return this.trayCode;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public Long getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public void setTrayCode(final String trayCode) {
        this.trayCode = trayCode;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setKitchenOrderId(final Long kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof TrayBindOrderRequest)) return false;
        final TrayBindOrderRequest other = (TrayBindOrderRequest) o;
        if (!other.canEqual(this)) return false;
        final Object this$kitchenOrderId = this.getKitchenOrderId();
        final Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final Object this$trayCode = this.getTrayCode();
        final Object other$trayCode = other.getTrayCode();
        if (this$trayCode == null ? other$trayCode != null : !this$trayCode.equals(other$trayCode)) return false;
        final Object this$orderId = this.getOrderId();
        final Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof TrayBindOrderRequest;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final Object $trayCode = this.getTrayCode();
        result = result * PRIME + ($trayCode == null ? 43 : $trayCode.hashCode());
        final Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "TrayBindOrderRequest(trayCode=" + this.getTrayCode() + ", orderId=" + this.getOrderId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ")";
    }
}
