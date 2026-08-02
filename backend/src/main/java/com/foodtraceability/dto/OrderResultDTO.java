package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * POS订单创建结果DTO
 */
@Schema(description = "POS订单创建结果DTO")
public class OrderResultDTO {
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "取餐号")
    private String pickupNumber;
    @Schema(description = "取餐码（外卖使用）")
    private String pickupCode;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "订单类型")
    private String orderType;
    @Schema(description = "桌号")
    private String tableNumber;
    @Schema(description = "订单金额")
    private BigDecimal totalAmount;
    @Schema(description = "创建时间")
    private String createTime;
    @Schema(description = "订单来源(0-APP/1-支付宝/2-现金/3-银行卡/4-POS终端)")
    private Integer orderSource;
    @Schema(description = "订单项列表")
    private List<OrderItemInfo> orderItems;
    @Schema(description = "扫码支付二维码URL（仅微信/支付宝返回，前端需弹窗展示）")
    private String qrCodeUrl;
    @Schema(description = "扫码支付交易号（用于确认支付）")
    private String qrTransactionId;
    @Schema(description = "二维码过期时间（毫秒时间戳，仅微信/支付宝返回）")
    private Long qrExpiresAt;


    @Schema(description = "订单项信息")
    public static class OrderItemInfo {
        @Schema(description = "商品ID")
        private String id;
        @Schema(description = "商品名称")
        private String name;
        @Schema(description = "单价")
        private BigDecimal price;
        @Schema(description = "数量")
        private Integer quantity;

        public OrderItemInfo() {
        }

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public BigDecimal getPrice() {
            return this.price;
        }

        public Integer getQuantity() {
            return this.quantity;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrderResultDTO.OrderItemInfo)) return false;
            final OrderResultDTO.OrderItemInfo other = (OrderResultDTO.OrderItemInfo) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$id = this.getId();
            final java.lang.Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final java.lang.Object this$price = this.getPrice();
            final java.lang.Object other$price = other.getPrice();
            if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrderResultDTO.OrderItemInfo;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $price = this.getPrice();
            result = result * PRIME + ($price == null ? 43 : $price.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrderResultDTO.OrderItemInfo(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ", quantity=" + this.getQuantity() + ")";
        }
    }

    public OrderResultDTO() {
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

    public String getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }

    public String getOrderType() {
        return this.orderType;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public List<OrderItemInfo> getOrderItems() {
        return this.orderItems;
    }

    public void setOrderItems(final List<OrderItemInfo> orderItems) {
        this.orderItems = orderItems;
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

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setOrderType(final String orderType) {
        this.orderType = orderType;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCreateTime(final String createTime) {
        this.createTime = createTime;
    }

    public Integer getOrderSource() {
        return this.orderSource;
    }

    public void setOrderSource(final Integer orderSource) {
        this.orderSource = orderSource;
    }

    public String getQrCodeUrl() {
        return this.qrCodeUrl;
    }

    public void setQrCodeUrl(final String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getQrTransactionId() {
        return this.qrTransactionId;
    }

    public void setQrTransactionId(final String qrTransactionId) {
        this.qrTransactionId = qrTransactionId;
    }

    public Long getQrExpiresAt() {
        return this.qrExpiresAt;
    }

    public void setQrExpiresAt(final Long qrExpiresAt) {
        this.qrExpiresAt = qrExpiresAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderResultDTO)) return false;
        final OrderResultDTO other = (OrderResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
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
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$orderItems = this.getOrderItems();
        final java.lang.Object other$orderItems = other.getOrderItems();
        if (this$orderItems == null ? other$orderItems != null : !this$orderItems.equals(other$orderItems)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $pickupNumber = this.getPickupNumber();
        result = result * PRIME + ($pickupNumber == null ? 43 : $pickupNumber.hashCode());
        final java.lang.Object $pickupCode = this.getPickupCode();
        result = result * PRIME + ($pickupCode == null ? 43 : $pickupCode.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $orderItems = this.getOrderItems();
        result = result * PRIME + ($orderItems == null ? 43 : $orderItems.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderResultDTO(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", pickupNumber=" + this.getPickupNumber() + ", pickupCode=" + this.getPickupCode() + ", status=" + this.getStatus() + ", message=" + this.getMessage() + ", orderType=" + this.getOrderType() + ", tableNumber=" + this.getTableNumber() + ", totalAmount=" + this.getTotalAmount() + ", createTime=" + this.getCreateTime() + ", orderItems=" + this.getOrderItems() + ")";
    }
}
