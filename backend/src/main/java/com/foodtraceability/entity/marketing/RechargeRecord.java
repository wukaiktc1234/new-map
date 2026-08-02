package com.foodtraceability.entity.marketing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 充值记录实体类
 * 记录会员充值流水，本金/赠送金额分离
 *
 * 支付状态（payment_status）：pending/success/failed/refunded/partial_refunded
 * 退款状态（refund_status）：none/pending/approved/rejected/refunded
 */
@TableName("recharge_records")
public class RechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(value = "record_id", type = IdType.ASSIGN_ID)
    private String recordId;

    /** 流水号 */
    @TableField("record_no")
    private String recordNo;

    /** 会员ID */
    @TableField("member_id")
    private String memberId;

    /** 会员姓名 */
    @TableField("member_name")
    private String memberName;

    /** 会员手机号 */
    @TableField("member_phone")
    private String memberPhone;

    /** 充值方案ID */
    @TableField("plan_id")
    private String planId;

    /** 充值方案名称 */
    @TableField("plan_name")
    private String planName;

    /** 充值金额（分，用户实付） */
    @TableField("recharge_amount")
    private Long rechargeAmount;

    /** 本金入账金额（分） */
    @TableField("principal_amount")
    private Long principalAmount;

    /** 赠送金额（分） */
    @TableField("bonus_amount")
    private Long bonusAmount;

    /** 赠送积分 */
    @TableField("bonus_points")
    private Integer bonusPoints;

    /** 支付方式 */
    @TableField("payment_method")
    private String paymentMethod;

    /** 支付状态 */
    @TableField("payment_status")
    private String paymentStatus;

    /** 支付时间 */
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    /** 交易流水号（第三方支付） */
    @TableField("transaction_no")
    private String transactionNo;

    /** 赠送金额过期时间 */
    @TableField("bonus_expire_time")
    private LocalDateTime bonusExpireTime;

    /** 退款状态 */
    @TableField("refund_status")
    private String refundStatus;

    /** 退款金额（分） */
    @TableField("refund_amount")
    private Long refundAmount;

    /** 退款时间 */
    @TableField("refund_time")
    private LocalDateTime refundTime;

    /** 退款原因 */
    @TableField("refund_reason")
    private String refundReason;

    /** 退款审批人 */
    @TableField("refund_approver")
    private String refundApprover;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 状态常量 ====================
    /** 支付状态：待支付 */
    public static final String PAYMENT_PENDING = "pending";
    /** 支付状态：成功 */
    public static final String PAYMENT_SUCCESS = "success";
    /** 支付状态：失败 */
    public static final String PAYMENT_FAILED = "failed";
    /** 支付状态：已退款 */
    public static final String PAYMENT_REFUNDED = "refunded";
    /** 支付状态：部分退款 */
    public static final String PAYMENT_PARTIAL_REFUNDED = "partial_refunded";
    /** 退款状态：无退款 */
    public static final String REFUND_NONE = "none";
    /** 退款状态：申请中 */
    public static final String REFUND_PENDING = "pending";
    /** 退款状态：已批准 */
    public static final String REFUND_APPROVED = "approved";
    /** 退款状态：已拒绝 */
    public static final String REFUND_REJECTED = "rejected";
    /** 退款状态：已退款 */
    public static final String REFUND_REFUNDED = "refunded";
    /** 支付方式：微信 */
    public static final String METHOD_WECHAT = "wechat";
    /** 支付方式：支付宝 */
    public static final String METHOD_ALIPAY = "alipay";
    /** 支付方式：现金 */
    public static final String METHOD_CASH = "cash";
    /** 支付方式：银行卡 */
    public static final String METHOD_BANK_CARD = "bank_card";
    /** 支付方式：余额 */
    public static final String METHOD_BALANCE = "balance";

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

    public Long getRechargeAmount() { return rechargeAmount; }
    public void setRechargeAmount(Long rechargeAmount) { this.rechargeAmount = rechargeAmount; }

    public Long getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(Long principalAmount) { this.principalAmount = principalAmount; }

    public Long getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(Long bonusAmount) { this.bonusAmount = bonusAmount; }

    public Integer getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(Integer bonusPoints) { this.bonusPoints = bonusPoints; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }

    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }

    public LocalDateTime getBonusExpireTime() { return bonusExpireTime; }
    public void setBonusExpireTime(LocalDateTime bonusExpireTime) { this.bonusExpireTime = bonusExpireTime; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }

    public Long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }

    public LocalDateTime getRefundTime() { return refundTime; }
    public void setRefundTime(LocalDateTime refundTime) { this.refundTime = refundTime; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getRefundApprover() { return refundApprover; }
    public void setRefundApprover(String refundApprover) { this.refundApprover = refundApprover; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
