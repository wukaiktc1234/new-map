package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "顾客订单DTO")
public class CustomerOrderDTO {
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "桌台号")
    private String tableNumber;
    @Schema(description = "订单状态")
    private String status;
    @Schema(description = "订单状态码")
    private Integer orderStatus;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "总菜品数")
    private Integer totalDishes;
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @Schema(description = "订单项列表")
    private List<OrderItemDTO> items;


    @Schema(description = "订单项DTO")
    public static class OrderItemDTO {
        @Schema(description = "菜品名称")
        private String name;
        @Schema(description = "数量")
        private Integer quantity;
        @Schema(description = "单价")
        private BigDecimal price;

        public OrderItemDTO() {
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

        public void setName(final String name) {
            this.name = name;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof CustomerOrderDTO.OrderItemDTO)) return false;
            final CustomerOrderDTO.OrderItemDTO other = (CustomerOrderDTO.OrderItemDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final java.lang.Object this$price = this.getPrice();
            final java.lang.Object other$price = other.getPrice();
            if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof CustomerOrderDTO.OrderItemDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $price = this.getPrice();
            result = result * PRIME + ($price == null ? 43 : $price.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "CustomerOrderDTO.OrderItemDTO(name=" + this.getName() + ", quantity=" + this.getQuantity() + ", price=" + this.getPrice() + ")";
        }
    }

    public CustomerOrderDTO() {
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

    public String getStatus() {
        return this.status;
    }

    public Integer getOrderStatus() {
        return this.orderStatus;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Integer getTotalDishes() {
        return this.totalDishes;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public List<OrderItemDTO> getItems() {
        return this.items;
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

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setOrderStatus(final Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setTotalDishes(final Integer totalDishes) {
        this.totalDishes = totalDishes;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setItems(final List<OrderItemDTO> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CustomerOrderDTO)) return false;
        final CustomerOrderDTO other = (CustomerOrderDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderStatus = this.getOrderStatus();
        final java.lang.Object other$orderStatus = other.getOrderStatus();
        if (this$orderStatus == null ? other$orderStatus != null : !this$orderStatus.equals(other$orderStatus)) return false;
        final java.lang.Object this$totalDishes = this.getTotalDishes();
        final java.lang.Object other$totalDishes = other.getTotalDishes();
        if (this$totalDishes == null ? other$totalDishes != null : !this$totalDishes.equals(other$totalDishes)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CustomerOrderDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderStatus = this.getOrderStatus();
        result = result * PRIME + ($orderStatus == null ? 43 : $orderStatus.hashCode());
        final java.lang.Object $totalDishes = this.getTotalDishes();
        result = result * PRIME + ($totalDishes == null ? 43 : $totalDishes.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "CustomerOrderDTO(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", tableNumber=" + this.getTableNumber() + ", status=" + this.getStatus() + ", orderStatus=" + this.getOrderStatus() + ", createTime=" + this.getCreateTime() + ", totalDishes=" + this.getTotalDishes() + ", totalAmount=" + this.getTotalAmount() + ", items=" + this.getItems() + ")";
    }
}
