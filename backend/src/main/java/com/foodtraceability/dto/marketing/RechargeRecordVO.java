package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充值记录VO
 * 金额字段以元为单位（字符串），与前端契约一致
 */
@Schema(description = "充值记录信息")
public class RechargeRecordVO {

    @Schema(description = "记录ID")
    private String recordId;

    @Schema(description = "流水号")
    private String recordNo;

    @Schema(description = "会员ID")
    private String memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "会员手机号")
    private String memberPhone;

    @Schema(description = "充值方案ID")
    private String planId;

    @Schema(description = "充值方案名称")
    private String planName;

    @Schema(description = "充值金额（元）")
    private String rechargeAmount;

    @Schema(description = "本金入账金额（元）")
    private String principalAmount;

    @Schema(description = "赠送金额（元）")
    private String bonusAmount;

    @Schema(description = "赠送积分")
    private Integer bonusPoints;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "支付状态")
    private String paymentStatus;

    @Schema(description = "支付时间")
    private String paymentTime;

    @Schema(description = "交易流水号")
    private String transactionNo;

    @Schema(description = "赠送金额过期时间")
    private String bonusExpireTime;

    @Schema(description = "退款状态")
    private String refundStatus;

    @Schema(description = "退款金额（元）")
    private String refundAmount;

    @Schema(description = "退款时间")
    private String refundTime;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款审批人")
    private String refundApprover;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    // ==================== Getter & Setter ====================

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getRechargeAmount() { return rechargeAmount; }
    public void setRechargeAmount(String rechargeAmount) { this.rechargeAmount = rechargeAmount; }

    public String getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(String principalAmount) { this.principalAmount = principalAmount; }

    public String getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(String bonusAmount) { this.bonusAmount = bonusAmount; }

    public Integer getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(Integer bonusPoints) { this.bonusPoints = bonusPoints; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentTime() { return paymentTime; }
    public void setPaymentTime(String paymentTime) { this.paymentTime = paymentTime; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public String getBonusExpireTime() { return bonusExpireTime; }
    public void setBonusExpireTime(String bonusExpireTime) { this.bonusExpireTime = bonusExpireTime; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public String getRefundAmount() { return refundAmount; }
    public void setRefundAmount(String refundAmount) { this.refundAmount = refundAmount; }

    public String getRefundTime() { return refundTime; }
    public void setRefundTime(String refundTime) { this.refundTime = refundTime; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getRefundApprover() { return refundApprover; }
    public void setRefundApprover(String refundApprover) { this.refundApprover = refundApprover; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
