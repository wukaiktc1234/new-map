package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 后厨订单创建DTO
 */
@Schema(description = "后厨订单创建DTO")
public class KitchenOrderCreateDTO {
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提")
    private Integer orderType;
    @Schema(description = "桌号（堂食时使用）")
    private String tableNumber;
    @Schema(description = "菜品列表")
    private List<DishItemDTO> dishItems;
    @Schema(description = "优先级：0-普通, 1-加急, 2-特急")
    private Integer priority;
    @Schema(description = "门店ID")
    private Long storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "备注")
    private String remark;


    @Schema(description = "菜品项DTO")
    public static class DishItemDTO {
        @Schema(description = "菜品ID")
        private String dishId;
        @Schema(description = "菜品名称")
        private String dishName;
        @Schema(description = "数量")
        private Integer quantity;
        @Schema(description = "备注")
        private String remark;

        public DishItemDTO() {
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

        public String getRemark() {
            return this.remark;
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

        public void setRemark(final String remark) {
            this.remark = remark;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof KitchenOrderCreateDTO.DishItemDTO)) return false;
            final KitchenOrderCreateDTO.DishItemDTO other = (KitchenOrderCreateDTO.DishItemDTO) o;
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
            final java.lang.Object this$remark = this.getRemark();
            final java.lang.Object other$remark = other.getRemark();
            if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof KitchenOrderCreateDTO.DishItemDTO;
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
            final java.lang.Object $remark = this.getRemark();
            result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "KitchenOrderCreateDTO.DishItemDTO(dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", quantity=" + this.getQuantity() + ", remark=" + this.getRemark() + ")";
        }
    }

    public KitchenOrderCreateDTO() {
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

    public List<DishItemDTO> getDishItems() {
        return this.dishItems;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getRemark() {
        return this.remark;
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

    public void setDishItems(final List<DishItemDTO> dishItems) {
        this.dishItems = dishItems;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof KitchenOrderCreateDTO)) return false;
        final KitchenOrderCreateDTO other = (KitchenOrderCreateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$dishItems = this.getDishItems();
        final java.lang.Object other$dishItems = other.getDishItems();
        if (this$dishItems == null ? other$dishItems != null : !this$dishItems.equals(other$dishItems)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof KitchenOrderCreateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $dishItems = this.getDishItems();
        result = result * PRIME + ($dishItems == null ? 43 : $dishItems.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "KitchenOrderCreateDTO(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", tableNumber=" + this.getTableNumber() + ", dishItems=" + this.getDishItems() + ", priority=" + this.getPriority() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", remark=" + this.getRemark() + ")";
    }
}
