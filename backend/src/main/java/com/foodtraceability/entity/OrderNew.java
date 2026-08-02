package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 订单主表实体类
 * 系统最核心的实体，管理所有订单信息
 *
 * 订单状态机:
 * 0待确认 -> 1已确认(制作中) -> 2已完成 -> 3已取消
 *          |-> 4部分退款    |-> 5全额退款
 * 特殊状态: 6待评价
 */
@TableName("orders")
@Schema(description = "订单主表实体")
public class OrderNew {

    /** 订单ID */
    @TableId(value = "order_id")
    @Schema(description = "订单ID", example = "O1783341970544")
    private String orderId;

    /** 订单编号，唯一 */
    @TableField("order_code")
    @Schema(description = "订单编号", example = "ORD20260425001")
    private String orderCode;

    /** 订单类型：1堂食 2外卖 3自提 4打包 */
    @TableField("order_type")
    @Schema(description = "订单类型: 1堂食 2外卖 3自提 4打包", example = "1")
    private Integer orderType;

    /** 订单来源：1收银台 2小程序 3第三方平台 */
    @TableField("order_source")
    @Schema(description = "订单来源: 1收银台 2小程序 3第三方平台", example = "1")
    private Integer orderSource;

    /** 门店ID（关联 stores_new.store_id），NULL 表示历史订单 */
    @TableField("store_id")
    @Schema(description = "门店ID", example = "1")
    private Long storeId;

    /** 会员ID（NULL表示非会员） */
    @TableField("customer_id")
    @Schema(description = "会员ID")
    private Long customerId;

    /** 顾客姓名（外卖必填） */
    @TableField("customer_name")
    @Schema(description = "顾客姓名", example = "张三")
    private String customerName;

    /** 顾客电话 */
    @TableField("customer_phone")
    @Schema(description = "顾客电话", example = "13800138000")
    private String customerPhone;

    /** 桌台ID（堂食必填） */
    @TableField("table_id")
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 桌台名称（冗余方便查询） */
    @TableField("table_name")
    @Schema(description = "桌台名称", example = "A01桌")
    private String tableName;

    /** 用餐人数 */
    @TableField("dining_people_count")
    @Schema(description = "用餐人数", example = "4")
    private Integer diningPeopleCount;

    /**
     * 订单状态：
     * 0待确认 1已确认(制作中) 2已完成 3已取消
     * 4部分退款 5全额退款 6待评价
     */
    @TableField("order_status")
    @Schema(description = "订单状态: 0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款 6待评价", example = "0")
    private Integer orderStatus;

    /**
     * 支付状态：
     * 0未支付 1部分支付 2已支付 3已退款
     */
    @TableField("payment_status")
    @Schema(description = "支付状态: 0未支付 1部分支付 2已支付 3已退款", example = "0")
    private Integer paymentStatus;

    /** 订单总金额（分） */
    @TableField("total_amount")
    @Schema(description = "订单总金额（分）", example = "12800")
    private Long totalAmount;

    /** 折扣优惠（分） */
    @TableField("discount_amount")
    @Schema(description = "折扣优惠（分）", example = "1000")
    private Long discountAmount;

    /** 优惠券抵扣（分） */
    @TableField("coupon_amount")
    @Schema(description = "优惠券抵扣（分）", example = "500")
    private Long couponAmount;

    /** 积分抵扣（分） */
    @TableField("points_amount")
    @Schema(description = "积分抵扣（分）", example = "200")
    private Long pointsAmount;

    /** 配送费（分） */
    @TableField("delivery_fee")
    @Schema(description = "配送费（分）", example = "500")
    private Long deliveryFee;

    /** 打包费（分） */
    @TableField("packaging_fee")
    @Schema(description = "打包费（分）", example = "100")
    private Long packagingFee;

    /** 实付金额（分） */
    @TableField("final_amount")
    @Schema(description = "实付金额（分）", example = "11200")
    private Long finalAmount;

    /** 已付金额（分） */
    @TableField("paid_amount")
    @Schema(description = "已付金额（分）", example = "0")
    private Long paidAmount;

    /** 已退金额（分） */
    @TableField("refund_amount")
    @Schema(description = "已退金额（分）", example = "0")
    private Long refundAmount;

    /** 获得积分 */
    @TableField("points_earned")
    @Schema(description = "获得积分", example = "112")
    private Integer pointsEarned;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 取消原因 */
    @TableField("cancel_reason")
    @Schema(description = "取消原因")
    private String cancelReason;

    /** 外卖配送地址 */
    @TableField("delivery_address")
    @Schema(description = "外卖配送地址")
    private String deliveryAddress;

    /** 期望送达时间（外卖） */
    @TableField("expected_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "期望送达时间")
    private LocalDateTime expectedTime;

    /** 实际送达时间 */
    @TableField("actual_delivery_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际送达时间")
    private LocalDateTime actualDeliveryTime;

    /** 收银员ID */
    @TableField("cashier_user_id")
    @Schema(description = "收银员ID")
    private Long cashierUserId;

    /** 创建人ID */
    @TableField("create_user_id")
    @Schema(description = "创建人ID")
    private Long createUserId;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public String getOrderId() { return orderId; }
    public String getOrderCode() { return orderCode; }
    public Integer getOrderType() { return orderType; }
    public Integer getOrderSource() { return orderSource; }
    public Long getStoreId() { return storeId; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public Long getTableId() { return tableId; }
    public String getTableName() { return tableName; }
    public Integer getDiningPeopleCount() { return diningPeopleCount; }
    public Integer getOrderStatus() { return orderStatus; }
    public Integer getPaymentStatus() { return paymentStatus; }
    public Long getTotalAmount() { return totalAmount; }
    public Long getDiscountAmount() { return discountAmount; }
    public Long getCouponAmount() { return couponAmount; }
    public Long getPointsAmount() { return pointsAmount; }
    public Long getDeliveryFee() { return deliveryFee; }
    public Long getPackagingFee() { return packagingFee; }
    public Long getFinalAmount() { return finalAmount; }
    public Long getPaidAmount() { return paidAmount; }
    public Long getRefundAmount() { return refundAmount; }
    public Integer getPointsEarned() { return pointsEarned; }
    public String getRemark() { return remark; }
    public String getCancelReason() { return cancelReason; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public LocalDateTime getExpectedTime() { return expectedTime; }
    public LocalDateTime getActualDeliveryTime() { return actualDeliveryTime; }
    public Long getCashierUserId() { return cashierUserId; }
    public Long getCreateUserId() { return createUserId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public void setOrderType(Integer orderType) { this.orderType = orderType; }
    public void setOrderSource(Integer orderSource) { this.orderSource = orderSource; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public void setDiningPeopleCount(Integer diningPeopleCount) { this.diningPeopleCount = diningPeopleCount; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public void setPaymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
    public void setCouponAmount(Long couponAmount) { this.couponAmount = couponAmount; }
    public void setPointsAmount(Long pointsAmount) { this.pointsAmount = pointsAmount; }
    public void setDeliveryFee(Long deliveryFee) { this.deliveryFee = deliveryFee; }
    public void setPackagingFee(Long packagingFee) { this.packagingFee = packagingFee; }
    public void setFinalAmount(Long finalAmount) { this.finalAmount = finalAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public void setExpectedTime(LocalDateTime expectedTime) { this.expectedTime = expectedTime; }
    public void setActualDeliveryTime(LocalDateTime actualDeliveryTime) { this.actualDeliveryTime = actualDeliveryTime; }
    public void setCashierUserId(Long cashierUserId) { this.cashierUserId = cashierUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
