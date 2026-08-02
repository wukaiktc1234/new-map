package com.foodtraceability.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单退款事件
 * 触发：退款审批通过后发布
 * 监听：{@link com.foodtraceability.event.listener.OrderRefundEventListener}
 *       生成红字财务记录（退款支出）+ 关联原凭证（如有）
 *
 * <p>DF-022 修复：approveRefund 中实现实际退款记录持久化后，发布本事件
 * 触发下游财务联动（生成退款流水、红字凭证等）。</p>
 */
public class OrderRefundEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 事件ID（建议使用 UUID 字符串） */
    private String eventId;
    /** 退款记录ID */
    private Long refundId;
    /** 关联订单ID */
    private String orderId;
    /** 订单编号（便于人工排查） */
    private String orderNumber;
    /** 门店ID（用于财务核算维度） */
    private Long storeId;
    /** 退款类型：1全额退款 2部分退款 */
    private Integer refundType;
    /** 退款金额（单位：分） */
    private Long refundAmount;
    /** 退款原因 */
    private String refundReason;
    /**
     * 退款方式：
     * 1原路返回 2现金 3其他
     */
    private Integer refundMethod;
    /** 审批人ID */
    private Long approveUserId;
    /** 退款明细ID列表（部分退款时使用，匹配 OrderItemNew.itemId） */
    private List<String> refundItemIds;
    /** 支付渠道状态：0未提交 1待渠道处理 2渠道处理成功 3渠道处理失败 */
    private Integer paymentChannelStatus;
    /** 退款完成时间 */
    private LocalDateTime completeTime;
    /** 事件发布时间 */
    private LocalDateTime eventTime;

    public OrderRefundEvent() {
        this.eventTime = LocalDateTime.now();
    }

    public String getEventId() { return eventId; }
    public Long getRefundId() { return refundId; }
    public String getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public Long getStoreId() { return storeId; }
    public Integer getRefundType() { return refundType; }
    public Long getRefundAmount() { return refundAmount; }
    public String getRefundReason() { return refundReason; }
    public Integer getRefundMethod() { return refundMethod; }
    public Long getApproveUserId() { return approveUserId; }
    public List<String> getRefundItemIds() { return refundItemIds; }
    public Integer getPaymentChannelStatus() { return paymentChannelStatus; }
    public LocalDateTime getCompleteTime() { return completeTime; }
    public LocalDateTime getEventTime() { return eventTime; }

    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public void setRefundType(Integer refundType) { this.refundType = refundType; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public void setRefundMethod(Integer refundMethod) { this.refundMethod = refundMethod; }
    public void setApproveUserId(Long approveUserId) { this.approveUserId = approveUserId; }
    public void setRefundItemIds(List<String> refundItemIds) { this.refundItemIds = refundItemIds; }
    public void setPaymentChannelStatus(Integer paymentChannelStatus) { this.paymentChannelStatus = paymentChannelStatus; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
