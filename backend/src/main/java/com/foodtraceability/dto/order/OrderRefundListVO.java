package com.foodtraceability.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 退款列表VO
 * 用于退款申请分页查询的展示对象
 */
@Schema(description = "退款列表展示对象")
public class OrderRefundListVO {

    /** 退款ID */
    @Schema(description = "退款ID")
    private Long refundId;

    /** 退款单号 */
    @Schema(description = "退款单号")
    private String refundNo;

    /** 订单ID */
    @Schema(description = "订单ID")
    private String orderId;

    /** 订单编号 */
    @Schema(description = "订单编号")
    private String orderCode;

    /** 退款金额（分） */
    @Schema(description = "退款金额（分）")
    private Long refundAmount;

    /** 退款原因 */
    @Schema(description = "退款原因")
    private String refundReason;

    /** 退款状态: 1待审核 2已通过 3已拒绝 4已执行 */
    @Schema(description = "退款状态: 1待审核 2已通过 3已拒绝 4已执行")
    private Integer refundStatus;

    /** 退款状态名称 */
    @Schema(description = "退款状态名称")
    private String refundStatusName;

    /** 申请人姓名 */
    @Schema(description = "申请人姓名")
    private String applyUserName;

    /** 审批人姓名 */
    @Schema(description = "审批人姓名")
    private String approveUserName;

    /** 拒绝原因 */
    @Schema(description = "拒绝原因")
    private String rejectReason;

    /** 申请时间 */
    @Schema(description = "申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 完成时间 */
    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    // Getter和Setter方法
    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public String getRefundStatusName() { return refundStatusName; }
    public void setRefundStatusName(String refundStatusName) { this.refundStatusName = refundStatusName; }
    public String getApplyUserName() { return applyUserName; }
    public void setApplyUserName(String applyUserName) { this.applyUserName = applyUserName; }
    public String getApproveUserName() { return approveUserName; }
    public void setApproveUserName(String approveUserName) { this.approveUserName = approveUserName; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }
}
