package com.foodtraceability.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象（VO）
 * 用于前端展示的完整订单信息
 */
@Schema(description = "订单视图对象")
public class OrderVO {

    /** 订单ID */
    @Schema(description = "订单ID")
    private String orderId;

    /** 订单编号 */
    @Schema(description = "订单编号")
    private String orderCode;

    /** 订单类型 */
    @Schema(description = "订单类型: 1堂食 2外卖 3自提 4打包")
    private Integer orderType;

    /** 订单类型名称 */
    @Schema(description = "订单类型名称")
    private String orderTypeName;

    /** 订单来源 */
    @Schema(description = "订单来源")
    private Integer orderSource;

    /** 门店ID（用于POS终端订单关联门店） */
    @Schema(description = "门店ID")
    private String storeId;

    /** 门店名称 */
    @Schema(description = "门店名称")
    private String storeName;

    /** 会员ID */
    @Schema(description = "会员ID")
    private Long customerId;

    /** 顾客姓名 */
    @Schema(description = "顾客姓名")
    private String customerName;

    /** 顾客电话 */
    @Schema(description = "顾客电话")
    private String customerPhone;

    /** 桌台ID */
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 桌台名称 */
    @Schema(description = "桌台名称")
    private String tableName;

    /** 用餐人数 */
    @Schema(description = "用餐人数")
    private Integer diningPeopleCount;

    /** 订单状态 */
    @Schema(description = "订单状态")
    private Integer orderStatus;

    /** 订单状态名称 */
    @Schema(description = "订单状态名称")
    private String orderStatusName;

    /** 支付状态 */
    @Schema(description = "支付状态")
    private Integer paymentStatus;

    /** 支付状态名称 */
    @Schema(description = "支付状态名称")
    private String paymentStatusName;

    /** 支付方式名称（用于POS终端订单展示） */
    @Schema(description = "支付方式名称")
    private String paymentMethodName;

    /** 订单总金额（分） */
    @Schema(description = "订单总金额（分）")
    private Long totalAmount;

    /** 折扣优惠（分） */
    @Schema(description = "折扣优惠（分）")
    private Long discountAmount;

    /** 优惠券抵扣（分） */
    @Schema(description = "优惠券抵扣（分）")
    private Long couponAmount;

    /** 积分抵扣（分） */
    @Schema(description = "积分抵扣（分）")
    private Long pointsAmount;

    /** 配送费（分） */
    @Schema(description = "配送费（分）")
    private Long deliveryFee;

    /** 打包费（分） */
    @Schema(description = "打包费（分）")
    private Long packagingFee;

    /** 实付金额（分） */
    @Schema(description = "实付金额（分）")
    private Long finalAmount;

    /** 已付金额（分） */
    @Schema(description = "已付金额（分）")
    private Long paidAmount;

    /** 已退金额（分） */
    @Schema(description = "已退金额（分）")
    private Long refundAmount;

    /** 获得积分 */
    @Schema(description = "获得积分")
    private Integer pointsEarned;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 取消原因 */
    @Schema(description = "取消原因")
    private String cancelReason;

    /** 外卖配送地址 */
    @Schema(description = "外卖配送地址")
    private String deliveryAddress;

    /** 期望送达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "期望送达时间")
    private LocalDateTime expectedTime;

    /** 实际送达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际送达时间")
    private LocalDateTime actualDeliveryTime;

    /** 收银员ID */
    @Schema(description = "收银员ID")
    private Long cashierUserId;

    /** 收银员姓名 */
    @Schema(description = "收银员姓名")
    private String cashierUserName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 订单明细列表 */
    @Schema(description = "订单明细列表")
    private List<OrderItemVO> items;

    /** 支付记录列表 */
    @Schema(description = "支付记录列表")
    private List<OrderPaymentRecordVO> payments;

    /** 退款记录列表 */
    @Schema(description = "退款记录列表")
    private List<OrderRefundRecordVO> refunds;

    // ========== 内部VO类 ==========

    /**
     * 订单明细VO
     */
    @Schema(description = "订单明细VO")
    public static class OrderItemVO {
        @Schema(description = "明细ID")
        private String itemId;
        @Schema(description = "产品类型")
        private Integer productType;
        @Schema(description = "产品类型名称")
        private String productTypeName;
        @Schema(description = "菜品ID")
        private Long foodId;
        @Schema(description = "套餐ID")
        private Long comboId;
        @Schema(description = "商品名称")
        private String productName;
        @Schema(description = "规格")
        private String specification;
        @Schema(description = "单价（分）")
        private Long unitPrice;
        @Schema(description = "数量")
        private Integer quantity;
        @Schema(description = "小计金额（分）")
        private Long amount;
        @Schema(description = "单项优惠（分）")
        private Long discountAmount;
        @Schema(description = "备注")
        private String remark;
        @Schema(description = "厨房状态")
        private Integer kitchenStatus;
        @Schema(description = "厨房状态名称")
        private String kitchenStatusName;

        // Getter和Setter
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public Integer getProductType() { return productType; }
        public void setProductType(Integer productType) { this.productType = productType; }
        public String getProductTypeName() { return productTypeName; }
        public void setProductTypeName(String productTypeName) { this.productTypeName = productTypeName; }
        public Long getFoodId() { return foodId; }
        public void setFoodId(Long foodId) { this.foodId = foodId; }
        public Long getComboId() { return comboId; }
        public void setComboId(Long comboId) { this.comboId = comboId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public Long getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public Long getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public Integer getKitchenStatus() { return kitchenStatus; }
        public void setKitchenStatus(Integer kitchenStatus) { this.kitchenStatus = kitchenStatus; }
        public String getKitchenStatusName() { return kitchenStatusName; }
        public void setKitchenStatusName(String kitchenStatusName) { this.kitchenStatusName = kitchenStatusName; }
    }

    /**
     * 支付记录VO
     */
    @Schema(description = "支付记录VO")
    public static class OrderPaymentRecordVO {
        @Schema(description = "支付记录ID")
        private Long paymentId;
        @Schema(description = "支付方式")
        private Integer paymentMethod;
        @Schema(description = "支付方式名称")
        private String paymentMethodName;
        @Schema(description = "支付金额（分）")
        private Long paymentAmount;
        @Schema(description = "交易流水号")
        private String transactionNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "支付时间")
        private LocalDateTime paymentTime;
        @Schema(description = "操作人姓名")
        private String operatorName;

        // Getter和Setter
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
        public Integer getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getPaymentMethodName() { return paymentMethodName; }
        public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
        public Long getPaymentAmount() { return paymentAmount; }
        public void setPaymentAmount(Long paymentAmount) { this.paymentAmount = paymentAmount; }
        public String getTransactionNo() { return transactionNo; }
        public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
        public LocalDateTime getPaymentTime() { return paymentTime; }
        public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
        public String getOperatorName() { return operatorName; }
        public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    }

    /**
     * 退款记录VO
     */
    @Schema(description = "退款记录VO")
    public static class OrderRefundRecordVO {
        @Schema(description = "退款记录ID")
        private Long refundId;
        @Schema(description = "退款类型")
        private Integer refundType;
        @Schema(description = "退款类型名称")
        private String refundTypeName;
        @Schema(description = "退款金额（分）")
        private Long refundAmount;
        @Schema(description = "退款原因")
        private String refundReason;
        @Schema(description = "退款方式")
        private Integer refundMethod;
        @Schema(description = "退款状态")
        private Integer refundStatus;
        @Schema(description = "退款状态名称")
        private String refundStatusName;
        @Schema(description = "审批人姓名")
        private String approveUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "申请时间")
        private LocalDateTime createTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "完成时间")
        private LocalDateTime completeTime;

        // Getter和Setter
        public Long getRefundId() { return refundId; }
        public void setRefundId(Long refundId) { this.refundId = refundId; }
        public Integer getRefundType() { return refundType; }
        public void setRefundType(Integer refundType) { this.refundType = refundType; }
        public String getRefundTypeName() { return refundTypeName; }
        public void setRefundTypeName(String refundTypeName) { this.refundTypeName = refundTypeName; }
        public Long getRefundAmount() { return refundAmount; }
        public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
        public String getRefundReason() { return refundReason; }
        public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
        public Integer getRefundMethod() { return refundMethod; }
        public void setRefundMethod(Integer refundMethod) { this.refundMethod = refundMethod; }
        public Integer getRefundStatus() { return refundStatus; }
        public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
        public String getRefundStatusName() { return refundStatusName; }
        public void setRefundStatusName(String refundStatusName) { this.refundStatusName = refundStatusName; }
        public String getApproveUserName() { return approveUserName; }
        public void setApproveUserName(String approveUserName) { this.approveUserName = approveUserName; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public LocalDateTime getCompleteTime() { return completeTime; }
        public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
    }

    // OrderVO的Getter和Setter方法
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Integer getOrderType() { return orderType; }
    public void setOrderType(Integer orderType) { this.orderType = orderType; }
    public String getOrderTypeName() { return orderTypeName; }
    public void setOrderTypeName(String orderTypeName) { this.orderTypeName = orderTypeName; }
    public Integer getOrderSource() { return orderSource; }
    public void setOrderSource(Integer orderSource) { this.orderSource = orderSource; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Integer getDiningPeopleCount() { return diningPeopleCount; }
    public void setDiningPeopleCount(Integer diningPeopleCount) { this.diningPeopleCount = diningPeopleCount; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderStatusName() { return orderStatusName; }
    public void setOrderStatusName(String orderStatusName) { this.orderStatusName = orderStatusName; }
    public Integer getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentStatusName() { return paymentStatusName; }
    public void setPaymentStatusName(String paymentStatusName) { this.paymentStatusName = paymentStatusName; }

    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public Long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
    public Long getCouponAmount() { return couponAmount; }
    public void setCouponAmount(Long couponAmount) { this.couponAmount = couponAmount; }
    public Long getPointsAmount() { return pointsAmount; }
    public void setPointsAmount(Long pointsAmount) { this.pointsAmount = pointsAmount; }
    public Long getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(Long deliveryFee) { this.deliveryFee = deliveryFee; }
    public Long getPackagingFee() { return packagingFee; }
    public void setPackagingFee(Long packagingFee) { this.packagingFee = packagingFee; }
    public Long getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Long finalAmount) { this.finalAmount = finalAmount; }
    public Long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
    public Long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public LocalDateTime getExpectedTime() { return expectedTime; }
    public void setExpectedTime(LocalDateTime expectedTime) { this.expectedTime = expectedTime; }
    public LocalDateTime getActualDeliveryTime() { return actualDeliveryTime; }
    public void setActualDeliveryTime(LocalDateTime actualDeliveryTime) { this.actualDeliveryTime = actualDeliveryTime; }
    public Long getCashierUserId() { return cashierUserId; }
    public void setCashierUserId(Long cashierUserId) { this.cashierUserId = cashierUserId; }
    public String getCashierUserName() { return cashierUserName; }
    public void setCashierUserName(String cashierUserName) { this.cashierUserName = cashierUserName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<OrderItemVO> getItems() { return items; }
    public void setItems(List<OrderItemVO> items) { this.items = items; }
    public List<OrderPaymentRecordVO> getPayments() { return payments; }
    public void setPayments(List<OrderPaymentRecordVO> payments) { this.payments = payments; }
    public List<OrderRefundRecordVO> getRefunds() { return refunds; }
    public void setRefunds(List<OrderRefundRecordVO> refunds) { this.refunds = refunds; }
}
