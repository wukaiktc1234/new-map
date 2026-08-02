package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应DTO
 * 用于返回订单信息
 */
@Schema(description = "订单响应")
public class OrderResponse {
    @Schema(description = "订单ID", example = "O20241201001")
    private String orderId;
    @Schema(description = "用户ID", example = "U001")
    private String userId;
    @Schema(description = "订单编号", example = "ORD202412010001")
    private String orderNumber;
    @Schema(description = "订单类型（0-堂食，1-外卖，2-自提）", example = "1")
    private Integer orderType;
    @Schema(description = "订单状态（0-待支付，1-已支付，2-待配送，3-配送中，4-已完成，5-已取消，6-退款中，7-已退款）", example = "1")
    private Integer orderStatus;
    @Schema(description = "订单金额（元）", example = "128.50")
    private BigDecimal orderAmount;
    @Schema(description = "优惠金额（元）", example = "10.00")
    private BigDecimal discountAmount;
    @Schema(description = "实际支付金额（元）", example = "118.50")
    private BigDecimal actualAmount;
    @Schema(description = "支付方式（0-微信支付，1-支付宝，2-现金，3-银行卡）", example = "0")
    private Integer paymentMethod;
    @Schema(description = "支付时间", example = "2024-12-01 12:30:00")
    private LocalDateTime paymentTime;
    @Schema(description = "配送地址", example = "北京市朝阳区建国路88号")
    private String deliveryAddress;
    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;
    @Schema(description = "联系人电话", example = "13800138000")
    private String contactPhone;
    @Schema(description = "备注信息", example = "不要辣，多放香菜")
    private String remarks;
    @Schema(description = "订单来源（0-APP，1-小程序，2-公众号，3-网页）", example = "1")
    private Integer orderSource;
    @Schema(description = "商家ID", example = "M001")
    private String merchantId;
    @Schema(description = "配送员ID", example = "D001")
    private String deliverymanId;
    @Schema(description = "预计送达时间", example = "2024-12-01 13:30:00")
    private LocalDateTime estimatedDeliveryTime;
    @Schema(description = "实际送达时间", example = "2024-12-01 13:25:00")
    private LocalDateTime actualDeliveryTime;
    @Schema(description = "创建时间", example = "2024-12-01 12:25:00")
    private LocalDateTime createTime;
    @Schema(description = "更新时间", example = "2024-12-01 12:30:00")
    private LocalDateTime updateTime;
    @Schema(description = "创建人", example = "admin")
    private String createBy;
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    @Schema(description = "退款金额（元）", example = "0.00")
    private BigDecimal refundAmount;
    @Schema(description = "退款时间", example = "2024-12-01 13:40:00")
    private LocalDateTime refundTime;
    @Schema(description = "取消原因", example = "用户取消")
    private String cancelReason;
    @Schema(description = "退款原因", example = "商品质量问题")
    private String refundReason;
    @Schema(description = "订单项列表")
    private List<OrderItemResponse> orderItems;

    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }


    /**
     * 订单项响应DTO
     */
    @Schema(description = "订单项响应")
    public static class OrderItemResponse {
        @Schema(description = "订单项ID", example = "OI20241201001")
        private String orderItemId;
        @Schema(description = "产品ID", example = "F001")
        private String foodId;
        @Schema(description = "产品名称", example = "有机苹果")
        private String foodName;
        @Schema(description = "产品单价（元）", example = "12.50")
        private BigDecimal unitPrice;
        @Schema(description = "购买数量", example = "2")
        private Integer quantity;
        @Schema(description = "小计金额（元）", example = "25.00")
        private BigDecimal subtotalAmount;
        @Schema(description = "产品图片URL", example = "/uploads/food/apple001.jpg")
        private String foodImageUrl;
        @Schema(description = "产品规格", example = "200g/个")
        private String specification;

        public OrderItemResponse() {
        }

        public String getOrderItemId() {
            return this.orderItemId;
        }

        public String getFoodId() {
            return this.foodId;
        }

        public String getFoodName() {
            return this.foodName;
        }

        public BigDecimal getUnitPrice() {
            return this.unitPrice;
        }

        public Integer getQuantity() {
            return this.quantity;
        }

        public BigDecimal getSubtotalAmount() {
            return this.subtotalAmount;
        }

        public String getFoodImageUrl() {
            return this.foodImageUrl;
        }

        public String getSpecification() {
            return this.specification;
        }

        public void setOrderItemId(final String orderItemId) {
            this.orderItemId = orderItemId;
        }

        public void setFoodId(final String foodId) {
            this.foodId = foodId;
        }

        public void setFoodName(final String foodName) {
            this.foodName = foodName;
        }

        public void setUnitPrice(final BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        public void setSubtotalAmount(final BigDecimal subtotalAmount) {
            this.subtotalAmount = subtotalAmount;
        }

        public void setFoodImageUrl(final String foodImageUrl) {
            this.foodImageUrl = foodImageUrl;
        }

        public void setSpecification(final String specification) {
            this.specification = specification;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrderResponse.OrderItemResponse)) return false;
            final OrderResponse.OrderItemResponse other = (OrderResponse.OrderItemResponse) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$orderItemId = this.getOrderItemId();
            final java.lang.Object other$orderItemId = other.getOrderItemId();
            if (this$orderItemId == null ? other$orderItemId != null : !this$orderItemId.equals(other$orderItemId)) return false;
            final java.lang.Object this$foodId = this.getFoodId();
            final java.lang.Object other$foodId = other.getFoodId();
            if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
            final java.lang.Object this$foodName = this.getFoodName();
            final java.lang.Object other$foodName = other.getFoodName();
            if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
            final java.lang.Object this$unitPrice = this.getUnitPrice();
            final java.lang.Object other$unitPrice = other.getUnitPrice();
            if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
            final java.lang.Object this$subtotalAmount = this.getSubtotalAmount();
            final java.lang.Object other$subtotalAmount = other.getSubtotalAmount();
            if (this$subtotalAmount == null ? other$subtotalAmount != null : !this$subtotalAmount.equals(other$subtotalAmount)) return false;
            final java.lang.Object this$foodImageUrl = this.getFoodImageUrl();
            final java.lang.Object other$foodImageUrl = other.getFoodImageUrl();
            if (this$foodImageUrl == null ? other$foodImageUrl != null : !this$foodImageUrl.equals(other$foodImageUrl)) return false;
            final java.lang.Object this$specification = this.getSpecification();
            final java.lang.Object other$specification = other.getSpecification();
            if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrderResponse.OrderItemResponse;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $orderItemId = this.getOrderItemId();
            result = result * PRIME + ($orderItemId == null ? 43 : $orderItemId.hashCode());
            final java.lang.Object $foodId = this.getFoodId();
            result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
            final java.lang.Object $foodName = this.getFoodName();
            result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
            final java.lang.Object $unitPrice = this.getUnitPrice();
            result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
            final java.lang.Object $subtotalAmount = this.getSubtotalAmount();
            result = result * PRIME + ($subtotalAmount == null ? 43 : $subtotalAmount.hashCode());
            final java.lang.Object $foodImageUrl = this.getFoodImageUrl();
            result = result * PRIME + ($foodImageUrl == null ? 43 : $foodImageUrl.hashCode());
            final java.lang.Object $specification = this.getSpecification();
            result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrderResponse.OrderItemResponse(orderItemId=" + this.getOrderItemId() + ", foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", unitPrice=" + this.getUnitPrice() + ", quantity=" + this.getQuantity() + ", subtotalAmount=" + this.getSubtotalAmount() + ", foodImageUrl=" + this.getFoodImageUrl() + ", specification=" + this.getSpecification() + ")";
        }
    }

    public OrderResponse() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public Integer getOrderType() {
        return this.orderType;
    }

    public Integer getOrderStatus() {
        return this.orderStatus;
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

    public LocalDateTime getPaymentTime() {
        return this.paymentTime;
    }

    public String getDeliveryAddress() {
        return this.deliveryAddress;
    }

    public String getContactName() {
        return this.contactName;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public Integer getOrderSource() {
        return this.orderSource;
    }

    public String getMerchantId() {
        return this.merchantId;
    }

    public String getDeliverymanId() {
        return this.deliverymanId;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return this.estimatedDeliveryTime;
    }

    public LocalDateTime getActualDeliveryTime() {
        return this.actualDeliveryTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public BigDecimal getRefundAmount() {
        return this.refundAmount;
    }

    public LocalDateTime getRefundTime() {
        return this.refundTime;
    }

    public String getCancelReason() {
        return this.cancelReason;
    }

    public String getRefundReason() {
        return this.refundReason;
    }

    public List<OrderItemResponse> getOrderItems() {
        return this.orderItems;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderType(final Integer orderType) {
        this.orderType = orderType;
    }

    public void setOrderStatus(final Integer orderStatus) {
        this.orderStatus = orderStatus;
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

    public void setPaymentTime(final LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public void setDeliveryAddress(final String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setContactName(final String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    public void setOrderSource(final Integer orderSource) {
        this.orderSource = orderSource;
    }

    public void setMerchantId(final String merchantId) {
        this.merchantId = merchantId;
    }

    public void setDeliverymanId(final String deliverymanId) {
        this.deliverymanId = deliverymanId;
    }

    public void setEstimatedDeliveryTime(final LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public void setActualDeliveryTime(final LocalDateTime actualDeliveryTime) {
        this.actualDeliveryTime = actualDeliveryTime;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setRefundAmount(final BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public void setRefundTime(final LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }

    public void setCancelReason(final String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void setRefundReason(final String refundReason) {
        this.refundReason = refundReason;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderResponse)) return false;
        final OrderResponse other = (OrderResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$orderStatus = this.getOrderStatus();
        final java.lang.Object other$orderStatus = other.getOrderStatus();
        if (this$orderStatus == null ? other$orderStatus != null : !this$orderStatus.equals(other$orderStatus)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$orderSource = this.getOrderSource();
        final java.lang.Object other$orderSource = other.getOrderSource();
        if (this$orderSource == null ? other$orderSource != null : !this$orderSource.equals(other$orderSource)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$orderAmount = this.getOrderAmount();
        final java.lang.Object other$orderAmount = other.getOrderAmount();
        if (this$orderAmount == null ? other$orderAmount != null : !this$orderAmount.equals(other$orderAmount)) return false;
        final java.lang.Object this$discountAmount = this.getDiscountAmount();
        final java.lang.Object other$discountAmount = other.getDiscountAmount();
        if (this$discountAmount == null ? other$discountAmount != null : !this$discountAmount.equals(other$discountAmount)) return false;
        final java.lang.Object this$actualAmount = this.getActualAmount();
        final java.lang.Object other$actualAmount = other.getActualAmount();
        if (this$actualAmount == null ? other$actualAmount != null : !this$actualAmount.equals(other$actualAmount)) return false;
        final java.lang.Object this$paymentTime = this.getPaymentTime();
        final java.lang.Object other$paymentTime = other.getPaymentTime();
        if (this$paymentTime == null ? other$paymentTime != null : !this$paymentTime.equals(other$paymentTime)) return false;
        final java.lang.Object this$deliveryAddress = this.getDeliveryAddress();
        final java.lang.Object other$deliveryAddress = other.getDeliveryAddress();
        if (this$deliveryAddress == null ? other$deliveryAddress != null : !this$deliveryAddress.equals(other$deliveryAddress)) return false;
        final java.lang.Object this$contactName = this.getContactName();
        final java.lang.Object other$contactName = other.getContactName();
        if (this$contactName == null ? other$contactName != null : !this$contactName.equals(other$contactName)) return false;
        final java.lang.Object this$contactPhone = this.getContactPhone();
        final java.lang.Object other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        final java.lang.Object this$merchantId = this.getMerchantId();
        final java.lang.Object other$merchantId = other.getMerchantId();
        if (this$merchantId == null ? other$merchantId != null : !this$merchantId.equals(other$merchantId)) return false;
        final java.lang.Object this$deliverymanId = this.getDeliverymanId();
        final java.lang.Object other$deliverymanId = other.getDeliverymanId();
        if (this$deliverymanId == null ? other$deliverymanId != null : !this$deliverymanId.equals(other$deliverymanId)) return false;
        final java.lang.Object this$estimatedDeliveryTime = this.getEstimatedDeliveryTime();
        final java.lang.Object other$estimatedDeliveryTime = other.getEstimatedDeliveryTime();
        if (this$estimatedDeliveryTime == null ? other$estimatedDeliveryTime != null : !this$estimatedDeliveryTime.equals(other$estimatedDeliveryTime)) return false;
        final java.lang.Object this$actualDeliveryTime = this.getActualDeliveryTime();
        final java.lang.Object other$actualDeliveryTime = other.getActualDeliveryTime();
        if (this$actualDeliveryTime == null ? other$actualDeliveryTime != null : !this$actualDeliveryTime.equals(other$actualDeliveryTime)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        final java.lang.Object this$refundAmount = this.getRefundAmount();
        final java.lang.Object other$refundAmount = other.getRefundAmount();
        if (this$refundAmount == null ? other$refundAmount != null : !this$refundAmount.equals(other$refundAmount)) return false;
        final java.lang.Object this$refundTime = this.getRefundTime();
        final java.lang.Object other$refundTime = other.getRefundTime();
        if (this$refundTime == null ? other$refundTime != null : !this$refundTime.equals(other$refundTime)) return false;
        final java.lang.Object this$cancelReason = this.getCancelReason();
        final java.lang.Object other$cancelReason = other.getCancelReason();
        if (this$cancelReason == null ? other$cancelReason != null : !this$cancelReason.equals(other$cancelReason)) return false;
        final java.lang.Object this$refundReason = this.getRefundReason();
        final java.lang.Object other$refundReason = other.getRefundReason();
        if (this$refundReason == null ? other$refundReason != null : !this$refundReason.equals(other$refundReason)) return false;
        final java.lang.Object this$orderItems = this.getOrderItems();
        final java.lang.Object other$orderItems = other.getOrderItems();
        if (this$orderItems == null ? other$orderItems != null : !this$orderItems.equals(other$orderItems)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderResponse;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $orderStatus = this.getOrderStatus();
        result = result * PRIME + ($orderStatus == null ? 43 : $orderStatus.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $orderSource = this.getOrderSource();
        result = result * PRIME + ($orderSource == null ? 43 : $orderSource.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $orderAmount = this.getOrderAmount();
        result = result * PRIME + ($orderAmount == null ? 43 : $orderAmount.hashCode());
        final java.lang.Object $discountAmount = this.getDiscountAmount();
        result = result * PRIME + ($discountAmount == null ? 43 : $discountAmount.hashCode());
        final java.lang.Object $actualAmount = this.getActualAmount();
        result = result * PRIME + ($actualAmount == null ? 43 : $actualAmount.hashCode());
        final java.lang.Object $paymentTime = this.getPaymentTime();
        result = result * PRIME + ($paymentTime == null ? 43 : $paymentTime.hashCode());
        final java.lang.Object $deliveryAddress = this.getDeliveryAddress();
        result = result * PRIME + ($deliveryAddress == null ? 43 : $deliveryAddress.hashCode());
        final java.lang.Object $contactName = this.getContactName();
        result = result * PRIME + ($contactName == null ? 43 : $contactName.hashCode());
        final java.lang.Object $contactPhone = this.getContactPhone();
        result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        final java.lang.Object $merchantId = this.getMerchantId();
        result = result * PRIME + ($merchantId == null ? 43 : $merchantId.hashCode());
        final java.lang.Object $deliverymanId = this.getDeliverymanId();
        result = result * PRIME + ($deliverymanId == null ? 43 : $deliverymanId.hashCode());
        final java.lang.Object $estimatedDeliveryTime = this.getEstimatedDeliveryTime();
        result = result * PRIME + ($estimatedDeliveryTime == null ? 43 : $estimatedDeliveryTime.hashCode());
        final java.lang.Object $actualDeliveryTime = this.getActualDeliveryTime();
        result = result * PRIME + ($actualDeliveryTime == null ? 43 : $actualDeliveryTime.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $refundAmount = this.getRefundAmount();
        result = result * PRIME + ($refundAmount == null ? 43 : $refundAmount.hashCode());
        final java.lang.Object $refundTime = this.getRefundTime();
        result = result * PRIME + ($refundTime == null ? 43 : $refundTime.hashCode());
        final java.lang.Object $cancelReason = this.getCancelReason();
        result = result * PRIME + ($cancelReason == null ? 43 : $cancelReason.hashCode());
        final java.lang.Object $refundReason = this.getRefundReason();
        result = result * PRIME + ($refundReason == null ? 43 : $refundReason.hashCode());
        final java.lang.Object $orderItems = this.getOrderItems();
        result = result * PRIME + ($orderItems == null ? 43 : $orderItems.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderResponse(orderId=" + this.getOrderId() + ", userId=" + this.getUserId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", orderStatus=" + this.getOrderStatus() + ", orderAmount=" + this.getOrderAmount() + ", discountAmount=" + this.getDiscountAmount() + ", actualAmount=" + this.getActualAmount() + ", paymentMethod=" + this.getPaymentMethod() + ", paymentTime=" + this.getPaymentTime() + ", deliveryAddress=" + this.getDeliveryAddress() + ", contactName=" + this.getContactName() + ", contactPhone=" + this.getContactPhone() + ", remarks=" + this.getRemarks() + ", orderSource=" + this.getOrderSource() + ", merchantId=" + this.getMerchantId() + ", deliverymanId=" + this.getDeliverymanId() + ", estimatedDeliveryTime=" + this.getEstimatedDeliveryTime() + ", actualDeliveryTime=" + this.getActualDeliveryTime() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", refundAmount=" + this.getRefundAmount() + ", refundTime=" + this.getRefundTime() + ", cancelReason=" + this.getCancelReason() + ", refundReason=" + this.getRefundReason() + ", orderItems=" + this.getOrderItems() + ")";
    }
}
