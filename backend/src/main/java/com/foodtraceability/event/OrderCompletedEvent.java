package com.foodtraceability.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单完成事件
 * 触发：财务收入记录、成本计算、销售统计
 */
public class OrderCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventId;
    private String orderId;
    private String orderNumber;
    private Integer orderType;
    private String orderTypeName;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal actualAmount;
    private Integer paymentMethod;
    private String paymentMethodName;
    private Long storeId;
    private String storeName;
    private String tableNumber;
    private String contactName;
    private String contactPhone;
    private List<OrderItemInfo> orderItems;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal totalCost;
    private BigDecimal profit;
    private LocalDateTime orderTime;
    private LocalDateTime completeTime;
    private LocalDateTime eventTime;


    public static class OrderItemInfo implements Serializable {
        private String dishId;
        private String dishName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal cost;
        private List<String> materialTraceCodes;

        public OrderItemInfo() {
        }

        public String getDishId() {
            return this.dishId;
        }

        public String getDishName() {
            return this.dishName;
        }

        public Integer getQuantity() {
            return this.quantity;
        }

        public BigDecimal getPrice() {
            return this.price;
        }

        public BigDecimal getCost() {
            return this.cost;
        }

        public List<String> getMaterialTraceCodes() {
            return this.materialTraceCodes;
        }

        public void setDishId(final String dishId) {
            this.dishId = dishId;
        }

        public void setDishName(final String dishName) {
            this.dishName = dishName;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }

        public void setCost(final BigDecimal cost) {
            this.cost = cost;
        }

        public void setMaterialTraceCodes(final List<String> materialTraceCodes) {
            this.materialTraceCodes = materialTraceCodes;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrderCompletedEvent.OrderItemInfo)) return false;
            final OrderCompletedEvent.OrderItemInfo other = (OrderCompletedEvent.OrderItemInfo) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$dishId = this.getDishId();
            final java.lang.Object other$dishId = other.getDishId();
            if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
            final java.lang.Object this$dishName = this.getDishName();
            final java.lang.Object other$dishName = other.getDishName();
            if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
            final java.lang.Object this$price = this.getPrice();
            final java.lang.Object other$price = other.getPrice();
            if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
            final java.lang.Object this$cost = this.getCost();
            final java.lang.Object other$cost = other.getCost();
            if (this$cost == null ? other$cost != null : !this$cost.equals(other$cost)) return false;
            final java.lang.Object this$materialTraceCodes = this.getMaterialTraceCodes();
            final java.lang.Object other$materialTraceCodes = other.getMaterialTraceCodes();
            if (this$materialTraceCodes == null ? other$materialTraceCodes != null : !this$materialTraceCodes.equals(other$materialTraceCodes)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrderCompletedEvent.OrderItemInfo;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $dishId = this.getDishId();
            result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
            final java.lang.Object $dishName = this.getDishName();
            result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
            final java.lang.Object $price = this.getPrice();
            result = result * PRIME + ($price == null ? 43 : $price.hashCode());
            final java.lang.Object $cost = this.getCost();
            result = result * PRIME + ($cost == null ? 43 : $cost.hashCode());
            final java.lang.Object $materialTraceCodes = this.getMaterialTraceCodes();
            result = result * PRIME + ($materialTraceCodes == null ? 43 : $materialTraceCodes.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrderCompletedEvent.OrderItemInfo(dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", quantity=" + this.getQuantity() + ", price=" + this.getPrice() + ", cost=" + this.getCost() + ", materialTraceCodes=" + this.getMaterialTraceCodes() + ")";
        }
    }

    public OrderCompletedEvent() {
        this.eventTime = LocalDateTime.now();
    }

    public String getEventId() {
        return this.eventId;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public Integer getOrderType() {
        return this.orderType;
    }

    public String getOrderTypeName() {
        return this.orderTypeName;
    }

    public BigDecimal getOrderAmount() {
        return this.orderAmount;
    }

    public BigDecimal getDiscountAmount() {
        return this.discountAmount;
    }

    public BigDecimal getActualAmount() {
        return this.actualAmount;
    }

    public Integer getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getPaymentMethodName() {
        return this.paymentMethodName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
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

    public List<OrderItemInfo> getOrderItems() {
        return this.orderItems;
    }

    public BigDecimal getMaterialCost() {
        return this.materialCost;
    }

    public BigDecimal getLaborCost() {
        return this.laborCost;
    }

    public BigDecimal getTotalCost() {
        return this.totalCost;
    }

    public BigDecimal getProfit() {
        return this.profit;
    }

    public LocalDateTime getOrderTime() {
        return this.orderTime;
    }

    public LocalDateTime getCompleteTime() {
        return this.completeTime;
    }

    public LocalDateTime getEventTime() {
        return this.eventTime;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderType(final Integer orderType) {
        this.orderType = orderType;
    }

    public void setOrderTypeName(final String orderTypeName) {
        this.orderTypeName = orderTypeName;
    }

    public void setOrderAmount(final BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public void setDiscountAmount(final BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setActualAmount(final BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public void setPaymentMethod(final Integer paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentMethodName(final String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
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

    public void setOrderItems(final List<OrderItemInfo> orderItems) {
        this.orderItems = orderItems;
    }

    public void setMaterialCost(final BigDecimal materialCost) {
        this.materialCost = materialCost;
    }

    public void setLaborCost(final BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public void setTotalCost(final BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public void setProfit(final BigDecimal profit) {
        this.profit = profit;
    }

    public void setOrderTime(final LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public void setCompleteTime(final LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public void setEventTime(final LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderCompletedEvent)) return false;
        final OrderCompletedEvent other = (OrderCompletedEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$eventId = this.getEventId();
        final java.lang.Object other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$orderTypeName = this.getOrderTypeName();
        final java.lang.Object other$orderTypeName = other.getOrderTypeName();
        if (this$orderTypeName == null ? other$orderTypeName != null : !this$orderTypeName.equals(other$orderTypeName)) return false;
        final java.lang.Object this$orderAmount = this.getOrderAmount();
        final java.lang.Object other$orderAmount = other.getOrderAmount();
        if (this$orderAmount == null ? other$orderAmount != null : !this$orderAmount.equals(other$orderAmount)) return false;
        final java.lang.Object this$discountAmount = this.getDiscountAmount();
        final java.lang.Object other$discountAmount = other.getDiscountAmount();
        if (this$discountAmount == null ? other$discountAmount != null : !this$discountAmount.equals(other$discountAmount)) return false;
        final java.lang.Object this$actualAmount = this.getActualAmount();
        final java.lang.Object other$actualAmount = other.getActualAmount();
        if (this$actualAmount == null ? other$actualAmount != null : !this$actualAmount.equals(other$actualAmount)) return false;
        final java.lang.Object this$paymentMethodName = this.getPaymentMethodName();
        final java.lang.Object other$paymentMethodName = other.getPaymentMethodName();
        if (this$paymentMethodName == null ? other$paymentMethodName != null : !this$paymentMethodName.equals(other$paymentMethodName)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$contactName = this.getContactName();
        final java.lang.Object other$contactName = other.getContactName();
        if (this$contactName == null ? other$contactName != null : !this$contactName.equals(other$contactName)) return false;
        final java.lang.Object this$contactPhone = this.getContactPhone();
        final java.lang.Object other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
        final java.lang.Object this$orderItems = this.getOrderItems();
        final java.lang.Object other$orderItems = other.getOrderItems();
        if (this$orderItems == null ? other$orderItems != null : !this$orderItems.equals(other$orderItems)) return false;
        final java.lang.Object this$materialCost = this.getMaterialCost();
        final java.lang.Object other$materialCost = other.getMaterialCost();
        if (this$materialCost == null ? other$materialCost != null : !this$materialCost.equals(other$materialCost)) return false;
        final java.lang.Object this$laborCost = this.getLaborCost();
        final java.lang.Object other$laborCost = other.getLaborCost();
        if (this$laborCost == null ? other$laborCost != null : !this$laborCost.equals(other$laborCost)) return false;
        final java.lang.Object this$totalCost = this.getTotalCost();
        final java.lang.Object other$totalCost = other.getTotalCost();
        if (this$totalCost == null ? other$totalCost != null : !this$totalCost.equals(other$totalCost)) return false;
        final java.lang.Object this$profit = this.getProfit();
        final java.lang.Object other$profit = other.getProfit();
        if (this$profit == null ? other$profit != null : !this$profit.equals(other$profit)) return false;
        final java.lang.Object this$orderTime = this.getOrderTime();
        final java.lang.Object other$orderTime = other.getOrderTime();
        if (this$orderTime == null ? other$orderTime != null : !this$orderTime.equals(other$orderTime)) return false;
        final java.lang.Object this$completeTime = this.getCompleteTime();
        final java.lang.Object other$completeTime = other.getCompleteTime();
        if (this$completeTime == null ? other$completeTime != null : !this$completeTime.equals(other$completeTime)) return false;
        final java.lang.Object this$eventTime = this.getEventTime();
        final java.lang.Object other$eventTime = other.getEventTime();
        if (this$eventTime == null ? other$eventTime != null : !this$eventTime.equals(other$eventTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderCompletedEvent;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $eventId = this.getEventId();
        result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $orderTypeName = this.getOrderTypeName();
        result = result * PRIME + ($orderTypeName == null ? 43 : $orderTypeName.hashCode());
        final java.lang.Object $orderAmount = this.getOrderAmount();
        result = result * PRIME + ($orderAmount == null ? 43 : $orderAmount.hashCode());
        final java.lang.Object $discountAmount = this.getDiscountAmount();
        result = result * PRIME + ($discountAmount == null ? 43 : $discountAmount.hashCode());
        final java.lang.Object $actualAmount = this.getActualAmount();
        result = result * PRIME + ($actualAmount == null ? 43 : $actualAmount.hashCode());
        final java.lang.Object $paymentMethodName = this.getPaymentMethodName();
        result = result * PRIME + ($paymentMethodName == null ? 43 : $paymentMethodName.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $contactName = this.getContactName();
        result = result * PRIME + ($contactName == null ? 43 : $contactName.hashCode());
        final java.lang.Object $contactPhone = this.getContactPhone();
        result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        final java.lang.Object $orderItems = this.getOrderItems();
        result = result * PRIME + ($orderItems == null ? 43 : $orderItems.hashCode());
        final java.lang.Object $materialCost = this.getMaterialCost();
        result = result * PRIME + ($materialCost == null ? 43 : $materialCost.hashCode());
        final java.lang.Object $laborCost = this.getLaborCost();
        result = result * PRIME + ($laborCost == null ? 43 : $laborCost.hashCode());
        final java.lang.Object $totalCost = this.getTotalCost();
        result = result * PRIME + ($totalCost == null ? 43 : $totalCost.hashCode());
        final java.lang.Object $profit = this.getProfit();
        result = result * PRIME + ($profit == null ? 43 : $profit.hashCode());
        final java.lang.Object $orderTime = this.getOrderTime();
        result = result * PRIME + ($orderTime == null ? 43 : $orderTime.hashCode());
        final java.lang.Object $completeTime = this.getCompleteTime();
        result = result * PRIME + ($completeTime == null ? 43 : $completeTime.hashCode());
        final java.lang.Object $eventTime = this.getEventTime();
        result = result * PRIME + ($eventTime == null ? 43 : $eventTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderCompletedEvent(eventId=" + this.getEventId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", orderTypeName=" + this.getOrderTypeName() + ", orderAmount=" + this.getOrderAmount() + ", discountAmount=" + this.getDiscountAmount() + ", actualAmount=" + this.getActualAmount() + ", paymentMethod=" + this.getPaymentMethod() + ", paymentMethodName=" + this.getPaymentMethodName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", tableNumber=" + this.getTableNumber() + ", contactName=" + this.getContactName() + ", contactPhone=" + this.getContactPhone() + ", orderItems=" + this.getOrderItems() + ", materialCost=" + this.getMaterialCost() + ", laborCost=" + this.getLaborCost() + ", totalCost=" + this.getTotalCost() + ", profit=" + this.getProfit() + ", orderTime=" + this.getOrderTime() + ", completeTime=" + this.getCompleteTime() + ", eventTime=" + this.getEventTime() + ")";
    }
}
