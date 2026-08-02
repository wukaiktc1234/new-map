package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 退款申请VO
 * 金额字段以元为单位（字符串），与前端契约一致
 */
@Schema(description = "退款申请信息")
public class RefundVO {

    @Schema(description = "退款ID")
    private String refundId;

    @Schema(description = "关联充值记录ID")
    private String rechargeRecordId;

    @Schema(description = "充值流水号")
    private String rechargeRecordNo;

    @Schema(description = "会员ID")
    private String memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "会员手机号")
    private String memberPhone;

    @Schema(description = "申请退款金额（元）")
    private String requestedAmount;

    @Schema(description = "可退本金（元）")
    private String refundablePrincipal;

    @Schema(description = "实际退款金额（元）")
    private String actualRefundAmount;

    @Schema(description = "赠送金额处理: clear-清零 proportional-按比例扣减")
    private String bonusHandling;

    @Schema(description = "退款状态: pending/approved/rejected/executed/cancelled")
    private String status;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "申请时间")
    private String applyTime;

    @Schema(description = "审批人")
    private String approver;

    @Schema(description = "审批时间")
    private String approveTime;

    @Schema(description = "审批意见")
    private String approveComment;

    @Schema(description = "退款执行人")
    private String executor;

    @Schema(description = "退款执行时间")
    private String executeTime;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款方式")
    private String refundMethod;

    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }

    public String getRechargeRecordId() { return rechargeRecordId; }
    public void setRechargeRecordId(String rechargeRecordId) { this.rechargeRecordId = rechargeRecordId; }

    public String getRechargeRecordNo() { return rechargeRecordNo; }
    public void setRechargeRecordNo(String rechargeRecordNo) { this.rechargeRecordNo = rechargeRecordNo; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(String requestedAmount) { this.requestedAmount = requestedAmount; }

    public String getRefundablePrincipal() { return refundablePrincipal; }
    public void setRefundablePrincipal(String refundablePrincipal) { this.refundablePrincipal = refundablePrincipal; }

    public String getActualRefundAmount() { return actualRefundAmount; }
    public void setActualRefundAmount(String actualRefundAmount) { this.actualRefundAmount = actualRefundAmount; }

    public String getBonusHandling() { return bonusHandling; }
    public void setBonusHandling(String bonusHandling) { this.bonusHandling = bonusHandling; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }

    public String getApplyTime() { return applyTime; }
    public void setApplyTime(String applyTime) { this.applyTime = applyTime; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public String getApproveTime() { return approveTime; }
    public void setApproveTime(String approveTime) { this.approveTime = approveTime; }

    public String getApproveComment() { return approveComment; }
    public void setApproveComment(String approveComment) { this.approveComment = approveComment; }

    public String getExecutor() { return executor; }
    public void setExecutor(String executor) { this.executor = executor; }

    public String getExecuteTime() { return executeTime; }
    public void setExecuteTime(String executeTime) { this.executeTime = executeTime; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getRefundMethod() { return refundMethod; }
    public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
