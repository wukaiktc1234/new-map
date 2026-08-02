package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 订单状态变更通知DTO
 * 用于WebSocket推送订单状态变化
 */
@Schema(description = "订单状态变更通知")
public class OrderStatusNotification {
    @Schema(description = "通知类型：new_order-新订单, status_change-状态变更, ready_for_pickup-准备取餐")
    private String notificationType;
    @Schema(description = "后厨订单ID")
    private String kitchenOrderId;
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提")
    private Integer orderType;
    @Schema(description = "桌号（堂食时使用）")
    private String tableNumber;
    @Schema(description = "当前状态")
    private String status;
    @Schema(description = "变更前状态")
    private String previousStatus;
    @Schema(description = "菜品总数")
    private Integer totalDishes;
    @Schema(description = "优先级：0-普通, 1-加急, 2-特急")
    private Integer priority;
    @Schema(description = "制作人员姓名")
    private String chefName;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "通知时间")
    private LocalDateTime notificationTime;


    public static class OrderStatusNotificationBuilder {
        private String notificationType;
        private String kitchenOrderId;
        private String orderId;
        private String orderNumber;
        private Integer orderType;
        private String tableNumber;
        private String status;
        private String previousStatus;
        private Integer totalDishes;
        private Integer priority;
        private String chefName;
        private String remark;
        private LocalDateTime notificationTime;

        OrderStatusNotificationBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder notificationType(final String notificationType) {
            this.notificationType = notificationType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder kitchenOrderId(final String kitchenOrderId) {
            this.kitchenOrderId = kitchenOrderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder orderId(final String orderId) {
            this.orderId = orderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder orderNumber(final String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder orderType(final Integer orderType) {
            this.orderType = orderType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder tableNumber(final String tableNumber) {
            this.tableNumber = tableNumber;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder previousStatus(final String previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder totalDishes(final Integer totalDishes) {
            this.totalDishes = totalDishes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder priority(final Integer priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder chefName(final String chefName) {
            this.chefName = chefName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public OrderStatusNotification.OrderStatusNotificationBuilder notificationTime(final LocalDateTime notificationTime) {
            this.notificationTime = notificationTime;
            return this;
        }

        public OrderStatusNotification build() {
            return new OrderStatusNotification(this.notificationType, this.kitchenOrderId, this.orderId, this.orderNumber, this.orderType, this.tableNumber, this.status, this.previousStatus, this.totalDishes, this.priority, this.chefName, this.remark, this.notificationTime);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrderStatusNotification.OrderStatusNotificationBuilder(notificationType=" + this.notificationType + ", kitchenOrderId=" + this.kitchenOrderId + ", orderId=" + this.orderId + ", orderNumber=" + this.orderNumber + ", orderType=" + this.orderType + ", tableNumber=" + this.tableNumber + ", status=" + this.status + ", previousStatus=" + this.previousStatus + ", totalDishes=" + this.totalDishes + ", priority=" + this.priority + ", chefName=" + this.chefName + ", remark=" + this.remark + ", notificationTime=" + this.notificationTime + ")";
        }
    }

    public static OrderStatusNotification.OrderStatusNotificationBuilder builder() {
        return new OrderStatusNotification.OrderStatusNotificationBuilder();
    }

    public String getNotificationType() {
        return this.notificationType;
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

    public String getStatus() {
        return this.status;
    }

    public String getPreviousStatus() {
        return this.previousStatus;
    }

    public Integer getTotalDishes() {
        return this.totalDishes;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public String getChefName() {
        return this.chefName;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getNotificationTime() {
        return this.notificationTime;
    }

    public void setNotificationType(final String notificationType) {
        this.notificationType = notificationType;
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

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setPreviousStatus(final String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public void setTotalDishes(final Integer totalDishes) {
        this.totalDishes = totalDishes;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public void setChefName(final String chefName) {
        this.chefName = chefName;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setNotificationTime(final LocalDateTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderStatusNotification)) return false;
        final OrderStatusNotification other = (OrderStatusNotification) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$totalDishes = this.getTotalDishes();
        final java.lang.Object other$totalDishes = other.getTotalDishes();
        if (this$totalDishes == null ? other$totalDishes != null : !this$totalDishes.equals(other$totalDishes)) return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$notificationType = this.getNotificationType();
        final java.lang.Object other$notificationType = other.getNotificationType();
        if (this$notificationType == null ? other$notificationType != null : !this$notificationType.equals(other$notificationType)) return false;
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
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$previousStatus = this.getPreviousStatus();
        final java.lang.Object other$previousStatus = other.getPreviousStatus();
        if (this$previousStatus == null ? other$previousStatus != null : !this$previousStatus.equals(other$previousStatus)) return false;
        final java.lang.Object this$chefName = this.getChefName();
        final java.lang.Object other$chefName = other.getChefName();
        if (this$chefName == null ? other$chefName != null : !this$chefName.equals(other$chefName)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$notificationTime = this.getNotificationTime();
        final java.lang.Object other$notificationTime = other.getNotificationTime();
        if (this$notificationTime == null ? other$notificationTime != null : !this$notificationTime.equals(other$notificationTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderStatusNotification;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $totalDishes = this.getTotalDishes();
        result = result * PRIME + ($totalDishes == null ? 43 : $totalDishes.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $notificationType = this.getNotificationType();
        result = result * PRIME + ($notificationType == null ? 43 : $notificationType.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $previousStatus = this.getPreviousStatus();
        result = result * PRIME + ($previousStatus == null ? 43 : $previousStatus.hashCode());
        final java.lang.Object $chefName = this.getChefName();
        result = result * PRIME + ($chefName == null ? 43 : $chefName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $notificationTime = this.getNotificationTime();
        result = result * PRIME + ($notificationTime == null ? 43 : $notificationTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderStatusNotification(notificationType=" + this.getNotificationType() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", tableNumber=" + this.getTableNumber() + ", status=" + this.getStatus() + ", previousStatus=" + this.getPreviousStatus() + ", totalDishes=" + this.getTotalDishes() + ", priority=" + this.getPriority() + ", chefName=" + this.getChefName() + ", remark=" + this.getRemark() + ", notificationTime=" + this.getNotificationTime() + ")";
    }

    public OrderStatusNotification() {
    }

    public OrderStatusNotification(final String notificationType, final String kitchenOrderId, final String orderId, final String orderNumber, final Integer orderType, final String tableNumber, final String status, final String previousStatus, final Integer totalDishes, final Integer priority, final String chefName, final String remark, final LocalDateTime notificationTime) {
        this.notificationType = notificationType;
        this.kitchenOrderId = kitchenOrderId;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
        this.status = status;
        this.previousStatus = previousStatus;
        this.totalDishes = totalDishes;
        this.priority = priority;
        this.chefName = chefName;
        this.remark = remark;
        this.notificationTime = notificationTime;
    }
}
