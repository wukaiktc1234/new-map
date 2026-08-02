package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderDetailDTO {
    private String orderId;
    private String orderNumber;
    private String createTime;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundTime;
    private String paymentMethod;
    private String status;
    private String kitchenStatus;
    private String transactionId;
    private List<OrderItemDetail> items;
    private String pickupNumber;
    private String pickupCode;
    private Integer orderType;
    private String orderTypeText;
    private Integer orderSource;
    private String tableNumber;
    private String contactName;
    private String contactPhone;
    private String deliveryAddress;
    private String remarks;
    private String createBy;
    private String updateBy;


    public static class OrderItemDetail {
        private String id;
        private String name;
        private BigDecimal price;
        private Integer quantity;

        public OrderItemDetail() {
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
            if (!(o instanceof OrderDetailDTO.OrderItemDetail)) return false;
            final OrderDetailDTO.OrderItemDetail other = (OrderDetailDTO.OrderItemDetail) o;
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
            return other instanceof OrderDetailDTO.OrderItemDetail;
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
            return "OrderDetailDTO.OrderItemDetail(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ", quantity=" + this.getQuantity() + ")";
        }
    }

    public OrderDetailDTO() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public BigDecimal getRefundAmount() {
        return this.refundAmount;
    }

    public String getRefundReason() {
        return this.refundReason;
    }

    public String getRefundTime() {
        return this.refundTime;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getStatus() {
        return this.status;
    }

    public String getKitchenStatus() {
        return this.kitchenStatus;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public List<OrderItemDetail> getItems() {
        return this.items;
    }

    public String getPickupNumber() {
        return this.pickupNumber;
    }

    public String getPickupCode() {
        return this.pickupCode;
    }

    public Integer getOrderType() {
        return this.orderType;
    }

    public String getOrderTypeText() {
        return this.orderTypeText;
    }

    public Integer getOrderSource() {
        return this.orderSource;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getContactName() {
        return this.contactName;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public String getDeliveryAddress() {
        return this.deliveryAddress;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setCreateTime(final String createTime) {
        this.createTime = createTime;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setRefundAmount(final BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public void setRefundReason(final String refundReason) {
        this.refundReason = refundReason;
    }

    public void setRefundTime(final String refundTime) {
        this.refundTime = refundTime;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setKitchenStatus(final String kitchenStatus) {
        this.kitchenStatus = kitchenStatus;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public void setItems(final List<OrderItemDetail> items) {
        this.items = items;
    }

    public void setPickupNumber(final String pickupNumber) {
        this.pickupNumber = pickupNumber;
    }

    public void setPickupCode(final String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public void setOrderType(final Integer orderType) {
        this.orderType = orderType;
    }

    public void setOrderTypeText(final String orderTypeText) {
        this.orderTypeText = orderTypeText;
    }

    public void setOrderSource(final Integer orderSource) {
        this.orderSource = orderSource;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setContactName(final String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setDeliveryAddress(final String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderDetailDTO)) return false;
        final OrderDetailDTO other = (OrderDetailDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$orderSource = this.getOrderSource();
        final java.lang.Object other$orderSource = other.getOrderSource();
        if (this$orderSource == null ? other$orderSource != null : !this$orderSource.equals(other$orderSource)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$refundAmount = this.getRefundAmount();
        final java.lang.Object other$refundAmount = other.getRefundAmount();
        if (this$refundAmount == null ? other$refundAmount != null : !this$refundAmount.equals(other$refundAmount)) return false;
        final java.lang.Object this$refundReason = this.getRefundReason();
        final java.lang.Object other$refundReason = other.getRefundReason();
        if (this$refundReason == null ? other$refundReason != null : !this$refundReason.equals(other$refundReason)) return false;
        final java.lang.Object this$refundTime = this.getRefundTime();
        final java.lang.Object other$refundTime = other.getRefundTime();
        if (this$refundTime == null ? other$refundTime != null : !this$refundTime.equals(other$refundTime)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$kitchenStatus = this.getKitchenStatus();
        final java.lang.Object other$kitchenStatus = other.getKitchenStatus();
        if (this$kitchenStatus == null ? other$kitchenStatus != null : !this$kitchenStatus.equals(other$kitchenStatus)) return false;
        final java.lang.Object this$transactionId = this.getTransactionId();
        final java.lang.Object other$transactionId = other.getTransactionId();
        if (this$transactionId == null ? other$transactionId != null : !this$transactionId.equals(other$transactionId)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        final java.lang.Object this$pickupNumber = this.getPickupNumber();
        final java.lang.Object other$pickupNumber = other.getPickupNumber();
        if (this$pickupNumber == null ? other$pickupNumber != null : !this$pickupNumber.equals(other$pickupNumber)) return false;
        final java.lang.Object this$pickupCode = this.getPickupCode();
        final java.lang.Object other$pickupCode = other.getPickupCode();
        if (this$pickupCode == null ? other$pickupCode != null : !this$pickupCode.equals(other$pickupCode)) return false;
        final java.lang.Object this$orderTypeText = this.getOrderTypeText();
        final java.lang.Object other$orderTypeText = other.getOrderTypeText();
        if (this$orderTypeText == null ? other$orderTypeText != null : !this$orderTypeText.equals(other$orderTypeText)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$contactName = this.getContactName();
        final java.lang.Object other$contactName = other.getContactName();
        if (this$contactName == null ? other$contactName != null : !this$contactName.equals(other$contactName)) return false;
        final java.lang.Object this$contactPhone = this.getContactPhone();
        final java.lang.Object other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
        final java.lang.Object this$deliveryAddress = this.getDeliveryAddress();
        final java.lang.Object other$deliveryAddress = other.getDeliveryAddress();
        if (this$deliveryAddress == null ? other$deliveryAddress != null : !this$deliveryAddress.equals(other$deliveryAddress)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderDetailDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $orderSource = this.getOrderSource();
        result = result * PRIME + ($orderSource == null ? 43 : $orderSource.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $refundAmount = this.getRefundAmount();
        result = result * PRIME + ($refundAmount == null ? 43 : $refundAmount.hashCode());
        final java.lang.Object $refundReason = this.getRefundReason();
        result = result * PRIME + ($refundReason == null ? 43 : $refundReason.hashCode());
        final java.lang.Object $refundTime = this.getRefundTime();
        result = result * PRIME + ($refundTime == null ? 43 : $refundTime.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $kitchenStatus = this.getKitchenStatus();
        result = result * PRIME + ($kitchenStatus == null ? 43 : $kitchenStatus.hashCode());
        final java.lang.Object $transactionId = this.getTransactionId();
        result = result * PRIME + ($transactionId == null ? 43 : $transactionId.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        final java.lang.Object $pickupNumber = this.getPickupNumber();
        result = result * PRIME + ($pickupNumber == null ? 43 : $pickupNumber.hashCode());
        final java.lang.Object $pickupCode = this.getPickupCode();
        result = result * PRIME + ($pickupCode == null ? 43 : $pickupCode.hashCode());
        final java.lang.Object $orderTypeText = this.getOrderTypeText();
        result = result * PRIME + ($orderTypeText == null ? 43 : $orderTypeText.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $contactName = this.getContactName();
        result = result * PRIME + ($contactName == null ? 43 : $contactName.hashCode());
        final java.lang.Object $contactPhone = this.getContactPhone();
        result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        final java.lang.Object $deliveryAddress = this.getDeliveryAddress();
        result = result * PRIME + ($deliveryAddress == null ? 43 : $deliveryAddress.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderDetailDTO(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", createTime=" + this.getCreateTime() + ", totalAmount=" + this.getTotalAmount() + ", refundAmount=" + this.getRefundAmount() + ", refundReason=" + this.getRefundReason() + ", refundTime=" + this.getRefundTime() + ", paymentMethod=" + this.getPaymentMethod() + ", status=" + this.getStatus() + ", kitchenStatus=" + this.getKitchenStatus() + ", transactionId=" + this.getTransactionId() + ", items=" + this.getItems() + ", pickupNumber=" + this.getPickupNumber() + ", pickupCode=" + this.getPickupCode() + ", orderType=" + this.getOrderType() + ", orderTypeText=" + this.getOrderTypeText() + ", orderSource=" + this.getOrderSource() + ", tableNumber=" + this.getTableNumber() + ", contactName=" + this.getContactName() + ", contactPhone=" + this.getContactPhone() + ", deliveryAddress=" + this.getDeliveryAddress() + ", remarks=" + this.getRemarks() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ")";
    }
}
