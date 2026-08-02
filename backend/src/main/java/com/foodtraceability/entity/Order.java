// 订单实体类
package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 用于管理系统订单信息
 */
@TableName("orders_legacy")
@Schema(description = "POS历史订单实体（已迁移至独立表，与新订单核心系统隔离）")
public class Order {
    /**
     * 订单ID，主键
     */
    @TableId(value = "ORDER_ID", type = IdType.ASSIGN_ID)
    @Schema(description = "订单ID", example = "O20241201001")
    private String orderId;
    /**
     * 用户ID
     */
    @TableField("USER_ID")
    @Schema(description = "用户ID", example = "U001")
    private String userId;
    /**
     * 订单编号
     */
    @TableField("ORDER_NUMBER")
    @Schema(description = "订单编号", example = "ORD202412010001")
    private String orderNumber;
    /**
     * 订单类型（0-堂食，1-外卖，2-自提）
     */
    @TableField("ORDER_TYPE")
    @Schema(description = "订单类型（0-堂食，1-外卖，2-自提）", example = "1")
    private Integer orderType;
    /**
     * 订单状态（0-待支付，1-已支付，2-待配送，3-配送中，4-已完成，5-已取消，6-退款中，7-已退款）
     */
    @TableField("ORDER_STATUS")
    @Schema(description = "订单状态（0-待支付，1-已支付，2-待配送，3-配送中，4-已完成，5-已取消，6-退款中，7-已退款）", example = "1")
    private Integer orderStatus;
    /**
     * 订单金额（元）
     */
    @TableField("ORDER_AMOUNT")
    @Schema(description = "订单金额（元）", example = "128.50")
    private BigDecimal orderAmount;
    /**
     * 优惠金额（元）
     */
    @TableField("DISCOUNT_AMOUNT")
    @Schema(description = "优惠金额（元）", example = "10.00")
    private BigDecimal discountAmount;
    /**
     * 实际支付金额（元）
     */
    @TableField("ACTUAL_AMOUNT")
    @Schema(description = "实际支付金额（元）", example = "118.50")
    private BigDecimal actualAmount;
    /**
     * 支付方式（0-微信支付，1-支付宝，2-现金，3-银行卡）
     */
    @TableField("PAYMENT_METHOD")
    @Schema(description = "支付方式（0-微信支付，1-支付宝，2-现金，3-银行卡）", example = "0")
    private Integer paymentMethod;
    /**
     * 交易单号（线上支付时生成）
     */
    @TableField("transaction_id")
    @Schema(description = "交易单号", example = "WX20240218123456789")
    private String transactionId;
    /**
     * 支付时间
     */
    @TableField("PAYMENT_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间", example = "2024-12-01 12:30:00")
    private LocalDateTime paymentTime;
    /**
     * 配送地址
     */
    @TableField("DELIVERY_ADDRESS")
    @Schema(description = "配送地址", example = "北京市朝阳区建国路88号")
    private String deliveryAddress;
    /**
     * 联系人姓名
     */
    @TableField("CONTACT_NAME")
    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;
    /**
     * 联系人电话
     */
    @TableField("CONTACT_PHONE")
    @Schema(description = "联系人电话", example = "13800138000")
    private String contactPhone;
    /**
     * 备注信息
     */
    @TableField("REMARKS")
    @Schema(description = "备注信息", example = "不要辣，多放香菜")
    private String remarks;
    /**
     * 幂等性键，用于防止重复提交订单
     */
    @TableField("IDEMPOTENCY_KEY")
    @Schema(description = "幂等性键", example = "UUID-unique-key")
    private String idempotencyKey;
    /**
     * 订单来源（0-APP，1-小程序，2-公众号，3-网页，4-POS终端）
     */
    @TableField("ORDER_SOURCE")
    @Schema(description = "订单来源（0-APP，1-小程序，2-公众号，3-网页，4-POS终端）", example = "4")
    private Integer orderSource;
    /**
     * 商家ID
     */
    @TableField("MERCHANT_ID")
    @Schema(description = "商家ID", example = "M001")
    private String merchantId;
    /**
     * 门店ID（关联 stores 表，POS端订单所属门店）
     */
    @TableField("STORE_ID")
    @Schema(description = "门店ID", example = "S001")
    private String storeId;
    /**
     * 门店名称（冗余存储，避免频繁关联查询）
     */
    @TableField("STORE_NAME")
    @Schema(description = "门店名称", example = "中心旗舰店")
    private String storeName;
    /**
     * 配送员ID
     */
    @TableField("DELIVERYMAN_ID")
    @Schema(description = "配送员ID", example = "D001")
    private String deliverymanId;
    /**
     * 预计送达时间
     */
    @TableField("ESTIMATED_DELIVERY_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预计送达时间", example = "2024-12-01 13:30:00")
    private LocalDateTime estimatedDeliveryTime;
    /**
     * 实际送达时间
     */
    @TableField("ACTUAL_DELIVERY_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际送达时间", example = "2024-12-01 13:25:00")
    private LocalDateTime actualDeliveryTime;
    /**
     * 取消原因
     */
    @TableField("CANCEL_REASON")
    @Schema(description = "取消原因", example = "用户取消")
    private String cancelReason;
    /**
     * 退款原因
     */
    @TableField("REFUND_REASON")
    @Schema(description = "退款原因", example = "商品质量问题")
    private String refundReason;
    /**
     * 退款金额
     */
    @TableField("REFUND_AMOUNT")
    @Schema(description = "退款金额（元）", example = "118.50")
    private BigDecimal refundAmount;
    /**
     * 退款时间
     */
    @TableField("REFUND_TIME")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "退款时间", example = "2024-12-01 13:40:00")
    private LocalDateTime refundTime;
    /**
     * 创建时间
     */
    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    /**
     * 创建人
     */
    @TableField(value = "CREATE_BY", fill = FieldFill.INSERT)
    @Schema(description = "创建人", example = "system")
    private String createBy;
    /**
     * 更新人
     */
    @TableField(value = "UPDATE_BY", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("DELETED")
    @Schema(description = "删除标记", example = "0")
    private Integer deleted;

    // Getter and Setter methods
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public Integer getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Integer paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(Integer orderSource) {
        this.orderSource = orderSource;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getDeliverymanId() {
        return deliverymanId;
    }

    public void setDeliverymanId(String deliverymanId) {
        this.deliverymanId = deliverymanId;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public LocalDateTime getActualDeliveryTime() {
        return actualDeliveryTime;
    }

    public void setActualDeliveryTime(LocalDateTime actualDeliveryTime) {
        this.actualDeliveryTime = actualDeliveryTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
