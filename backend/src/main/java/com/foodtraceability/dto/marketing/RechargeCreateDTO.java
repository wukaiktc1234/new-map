package com.foodtraceability.dto.marketing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 充值请求DTO
 */
public class RechargeCreateDTO {

    /** 会员ID */
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    /** 充值方案ID（NULL表示自定义金额） */
    private Long planId;

    /** 自定义充值金额（分），当planId为空时必填 */
    @Positive(message = "充值金额必须大于0")
    private Long rechargeAmount;

    /**
     * 支付方式
     * 1微信 2支付宝 3现金 4银行卡 5余额支付
     */
    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod;

    /** 第三方支付流水号 */
    private String transactionNo;

    /** 操作员ID */
    private Long operateUserId;

    /** 操作员姓名 */
    private String operateUserName;

    /** 备注 */
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getRechargeAmount() {
        return rechargeAmount;
    }

    public void setRechargeAmount(Long rechargeAmount) {
        this.rechargeAmount = rechargeAmount;
    }

    public Integer getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Integer paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public Long getOperateUserId() {
        return operateUserId;
    }

    public void setOperateUserId(Long operateUserId) {
        this.operateUserId = operateUserId;
    }

    public String getOperateUserName() {
        return operateUserName;
    }

    public void setOperateUserName(String operateUserName) {
        this.operateUserName = operateUserName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
