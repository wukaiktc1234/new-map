package com.foodtraceability.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventId;
    private String kitchenOrderId;
    private String orderId;
    private String orderNumber;
    private Integer orderType;
    private String tableNumber;
    private BigDecimal totalAmount;
    private Integer totalDishes;
    private List<OrderItemInfo> orderItems;
    private LocalDateTime createTime;
    private LocalDateTime eventTime;


    public static class OrderItemInfo implements Serializable {
        private String id;
        private String name;
        private Integer quantity;
        private BigDecimal price;
        private String dishType;

        public OrderItemInfo() {
        }

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public Integer getQuantity() {
            return this.quantity;
        }

        public BigDecimal getPrice() {
            return this.price;
        }

        public String getDishType() {
            return this.dishType;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }

        public void setDishType(final String dishType) {
            this.dishType = dishType;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrderCreatedEvent.OrderItemInfo)) return false;
            final OrderCreatedEvent.OrderItemInfo other = (OrderCreatedEvent.OrderItemInfo) o;
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
            final java.lang.Object this$dishType = this.getDishType();
            final java.lang.Object other$dishType = other.getDishType();
            if (this$dishType == null ? other$dishType != null : !this$dishType.equals(other$dishType)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrderCreatedEvent.OrderItemInfo;
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
            final java.lang.Object $dishType = this.getDishType();
            result = result * PRIME + ($dishType == null ? 43 : $dishType.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrderCreatedEvent.OrderItemInfo(id=" + this.getId() + ", name=" + this.getName() + ", quantity=" + this.getQuantity() + ", price=" + this.getPrice() + ", dishType=" + this.getDishType() + ")";
        }
    }

    public OrderCreatedEvent() {
        this.eventTime = LocalDateTime.now();
    }

    public String getEventId() {
        return this.eventId;
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

    public Integer getOrderType() {
        return this.orderType;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public Integer getTotalDishes() {
        return this.totalDishes;
    }

    public List<OrderItemInfo> getOrderItems() {
        return this.orderItems;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getEventTime() {
        return this.eventTime;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
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

    public void setOrderType(final Integer orderType) {
        this.orderType = orderType;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setTotalDishes(final Integer totalDishes) {
        this.totalDishes = totalDishes;
    }

    public void setOrderItems(final List<OrderItemInfo> orderItems) {
        this.orderItems = orderItems;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setEventTime(final LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderCreatedEvent)) return false;
        final OrderCreatedEvent other = (OrderCreatedEvent) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$totalDishes = this.getTotalDishes();
        final java.lang.Object other$totalDishes = other.getTotalDishes();
        if (this$totalDishes == null ? other$totalDishes != null : !this$totalDishes.equals(other$totalDishes)) return false;
        final java.lang.Object this$eventId = this.getEventId();
        final java.lang.Object other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$orderItems = this.getOrderItems();
        final java.lang.Object other$orderItems = other.getOrderItems();
        if (this$orderItems == null ? other$orderItems != null : !this$orderItems.equals(other$orderItems)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$eventTime = this.getEventTime();
        final java.lang.Object other$eventTime = other.getEventTime();
        if (this$eventTime == null ? other$eventTime != null : !this$eventTime.equals(other$eventTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderCreatedEvent;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $totalDishes = this.getTotalDishes();
        result = result * PRIME + ($totalDishes == null ? 43 : $totalDishes.hashCode());
        final java.lang.Object $eventId = this.getEventId();
        result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $orderItems = this.getOrderItems();
        result = result * PRIME + ($orderItems == null ? 43 : $orderItems.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $eventTime = this.getEventTime();
        result = result * PRIME + ($eventTime == null ? 43 : $eventTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderCreatedEvent(eventId=" + this.getEventId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", tableNumber=" + this.getTableNumber() + ", totalAmount=" + this.getTotalAmount() + ", totalDishes=" + this.getTotalDishes() + ", orderItems=" + this.getOrderItems() + ", createTime=" + this.getCreateTime() + ", eventTime=" + this.getEventTime() + ")";
    }
}
