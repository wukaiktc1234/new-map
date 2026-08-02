package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 退款记录实体类
 * 记录订单的每笔退款操作及审批流程
 */
@TableName("order_refund_records")
@Schema(description = "退款记录实体")
public class OrderRefundRecordNew {

    /** 退款记录ID，主键自增 */
    @TableId(value = "refund_id", type = IdType.AUTO)
    @Schema(description = "退款记录ID", example = "1")
    private Long refundId;

    /** 关联订单ID */
    @TableField("order_id")
    @Schema(description = "关联订单ID", example = "O1783341970544")
    private String orderId;

    /**
     * 退款类型：
     * 1全额退款 2部分退款
     */
    @TableField("refund_type")
    @Schema(description = "退款类型: 1全额退款 2部分退款", example = "1")
    private Integer refundType;

    /** 退款金额（分） */
    @TableField("refund_amount")
    @Schema(description = "退款金额（分）", example = "11200")
    private Long refundAmount;

    /** 退款原因 */
    @TableField("refund_reason")
    @Schema(description = "退款原因", example = "菜品质量问题")
    private String refundReason;

    /**
     * 退款方式：
     * 1原路返回 2现金 3其他
     */
    @TableField("refund_method")
    @Schema(description = "退款方式: 1原路返回 2现金 3其他", example = "1")
    private Integer refundMethod;

    /** 审批人ID */
    @TableField("approve_user_id")
    @Schema(description = "审批人ID")
    private Long approveUserId;

    /**
     * 退款状态：
     * 0待审核 1已同意 2已拒绝 3已退款
     */
    @TableField("refund_status")
    @Schema(description = "退款状态: 0待审核 1已同意 2已拒绝 3已退款", example = "0")
    private Integer refundStatus;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 完成时间 */
    @TableField("complete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    /**
     * 支付渠道状态：
     * 0未提交 1待渠道处理 2渠道处理成功 3渠道处理失败
     * 用于退款记录持久化方案：approveRefund 时标记为"待渠道处理"，
     * 后续接入真实支付 SDK 时再实现实际退款（DF-022 修复）
     */
    @TableField("payment_channel_status")
    @Schema(description = "支付渠道状态: 0未提交 1待渠道处理 2渠道处理成功 3渠道处理失败", example = "0")
    private Integer paymentChannelStatus;

    /** 支付渠道提交时间（退款提交到支付渠道的时间） */
    @TableField("payment_channel_submit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付渠道提交时间")
    private LocalDateTime paymentChannelSubmitTime;

    /**
     * 退款明细ID列表（CSV格式，部分退款时使用）
     * 存储订单明细 OrderItemNew.itemId 的列表，以英文逗号分隔
     * 用于部分退款精确回补库存（DF-024 修复）
     */
    @TableField("refund_item_ids")
    @Schema(description = "退款明细ID列表（CSV格式，部分退款时使用）")
    private String refundItemIds;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public Long getRefundId() { return refundId; }
    public String getOrderId() { return orderId; }
    public Integer getRefundType() { return refundType; }
    public Long getRefundAmount() { return refundAmount; }
    public String getRefundReason() { return refundReason; }
    public Integer getRefundMethod() { return refundMethod; }
    public Long getApproveUserId() { return approveUserId; }
    public Integer getRefundStatus() { return refundStatus; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getCompleteTime() { return completeTime; }
    public Integer getPaymentChannelStatus() { return paymentChannelStatus; }
    public LocalDateTime getPaymentChannelSubmitTime() { return paymentChannelSubmitTime; }
    public String getRefundItemIds() { return refundItemIds; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setRefundId(Long refundId) { this.refundId = refundId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setRefundType(Integer refundType) { this.refundType = refundType; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public void setRefundMethod(Integer refundMethod) { this.refundMethod = refundMethod; }
    public void setApproveUserId(Long approveUserId) { this.approveUserId = approveUserId; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
    public void setPaymentChannelStatus(Integer paymentChannelStatus) { this.paymentChannelStatus = paymentChannelStatus; }
    public void setPaymentChannelSubmitTime(LocalDateTime paymentChannelSubmitTime) { this.paymentChannelSubmitTime = paymentChannelSubmitTime; }
    public void setRefundItemIds(String refundItemIds) { this.refundItemIds = refundItemIds; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
