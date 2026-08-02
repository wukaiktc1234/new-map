package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * POS销售事件DTO
 * 用于POS堂食/外卖收款场景的会计事件实现
 * 包含支付方式、订单类型、平台佣金等特有信息
 * @author example
 * @since 2026-04-04
 */
public class PosSaleEvent implements AccountingEvent {

    /** 事件类型常量：现金收款 */
    public static final String EVENT_TYPE_CASH = "POS_SALE_CASH";
    
    /** 事件类型常量：微信收款 */
    public static final String EVENT_TYPE_WECHAT = "POS_SALE_WECHAT";
    
    /** 事件类型常量：支付宝收款 */
    public static final String EVENT_TYPE_ALIPAY = "POS_SALE_ALIPAY";
    
    /** 支付方式枚举：现金 */
    public static final String PAYMENT_CASH = "CASH";
    
    /** 支付方式枚举：微信 */
    public static final String PAYMENT_WECHAT = "WECHAT";
    
    /** 支付方式枚举：支付宝 */
    public static final String PAYMENT_ALIPAY = "ALIPAY";
    
    /** 支付方式枚举：美团 */
    public static final String PAYMENT_MEITUAN = "MEITUAN";
    
    /** 支付方式枚举：饿了么 */
    public static final String PAYMENT_ELEME = "ELEME";
    
    /** 支付方式枚举：抖音 */
    public static final String PAYMENT_DOUYIN = "DOUYIN";
    
    /** 订单类型枚举：堂食 */
    public static final String ORDER_TYPE_DINE_IN = "DINE_IN";
    
    /** 订单类型枚举：外卖 */
    public static final String ORDER_TYPE_TAKEAWAY = "TAKEAWAY";
    
    /** 订单类型枚举：团购 */
    public static final String ORDER_TYPE_GROUP_BUY = "GROUP_BUY";

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 源业务单据ID（订单ID）
     */
    private String sourceBusinessId;

    /**
     * 源业务单据编号（订单号）
     */
    private String sourceBusinessNo;

    /**
     * 事件日期（收款日期）
     */
    private LocalDate eventDate;

    /**
     * 总金额（应收金额或实收金额）
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 平台佣金金额（外卖/团购场景）
     */
    private BigDecimal platformCommission;

    /**
     * 客户姓名（可选）
     */
    private String customerName;

    /**
     * 扩展数据
     */
    private Map<String, Object> extraData = new HashMap<>();

    // 实现AccountingEvent接口方法

    @Override
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getSourceBusinessId() {
        return sourceBusinessId;
    }

    public void setSourceBusinessId(String sourceBusinessId) {
        this.sourceBusinessId = sourceBusinessId;
    }

    @Override
    public String getSourceBusinessNo() {
        return sourceBusinessNo;
    }

    public void setSourceBusinessNo(String sourceBusinessNo) {
        this.sourceBusinessNo = sourceBusinessNo;
    }

    @Override
    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public Map<String, Object> getExtraData() {
        if (extraData == null) {
            extraData = new HashMap<>();
        }
        // 将特有字段添加到扩展数据中
        extraData.put("paymentMethod", paymentMethod);
        extraData.put("orderType", orderType);
        extraData.put("platformCommission", platformCommission);
        extraData.put("customerName", customerName);
        return extraData;
    }

    // getter and setter methods for specific fields
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public void setPlatformCommission(BigDecimal platformCommission) {
        this.platformCommission = platformCommission;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    @Override
    public String toString() {
        return "PosSaleEvent{" +
            "eventType='" + eventType + '\'' +
            ", sourceBusinessId='" + sourceBusinessId + '\'' +
            ", sourceBusinessNo='" + sourceBusinessNo + '\'' +
            ", eventDate=" + eventDate +
            ", amount=" + amount +
            ", paymentMethod='" + paymentMethod + '\'' +
            ", orderType='" + orderType + '\'' +
            ", platformCommission=" + platformCommission +
            ", customerName='" + customerName + '\'' +
            '}';
    }
}
