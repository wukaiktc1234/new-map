package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 充值记录实体类
 * 记录会员的所有充值交易流水
 */
@TableName("recharge_record")
public class RechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("record_id")
    private Long recordId;

    /** 充值流水号（唯一） */
    @TableField("record_no")
    private String recordNo;

    /** 会员ID */
    @TableField("member_id")
    private Long memberId;

    /** 充值方案ID（NULL表示自定义金额） */
    @TableField("plan_id")
    private Long planId;

    /** 实充金额（分） */
    @TableField("recharge_amount")
    private Long rechargeAmount;

    /** 赠送金额（分） */
    @TableField("bonus_amount")
    private Long bonusAmount;

    /** 赠送积分 */
    @TableField("bonus_points")
    private Integer bonusPoints;

    /**
     * 支付方式
     * 1微信 2支付宝 3现金 4银行卡 5余额支付
     */
    @TableField("payment_method")
    private Integer paymentMethod;

    /**
     * 支付状态
     * 0待支付 1已支付 2已退款 3支付失败
     */
    @TableField("payment_status")
    private Integer paymentStatus;

    /** 第三方支付流水号 */
    @TableField("transaction_no")
    private String transactionNo;

    /** 支付完成时间 */
    @TableField("paid_time")
    private LocalDateTime paidTime;

    /** 退款时间 */
    @TableField("refund_time")
    private LocalDateTime refundTime;

    /** 退款金额（分） */
    @TableField("refund_amount")
    private Long refundAmount;

    /** 操作员ID */
    @TableField("operate_user_id")
    private Long operateUserId;

    /** 操作员姓名 */
    @TableField("operate_user_name")
    private String operateUserName;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0未删除，1已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 非持久化字段 ====================

    /** 会员手机号 */
    @TableField(exist = false)
    private String memberPhone;

    /** 会员昵称 */
    @TableField(exist = false)
    private String memberNickname;

    /** 方案名称 */
    @TableField(exist = false)
    private String planName;

    // ==================== Getter & Setter ====================

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

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

    public Long getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(Long bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public Integer getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(Integer bonusPoints) {
        this.bonusPoints = bonusPoints;
    }

    public Integer getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Integer paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public LocalDateTime getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(LocalDateTime paidTime) {
        this.paidTime = paidTime;
    }

    public LocalDateTime getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(LocalDateTime refundTime) {
        this.refundTime = refundTime;
    }

    public Long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
    }

    public String getMemberNickname() {
        return memberNickname;
    }

    public void setMemberNickname(String memberNickname) {
        this.memberNickname = memberNickname;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }
}
