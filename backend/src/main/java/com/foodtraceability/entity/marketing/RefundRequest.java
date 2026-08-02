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
 * 退款申请实体类
 * 退款审批流：申请 → 审批 → 执行
 * 退款仅退本金，赠送按配置处理（清零或按比例扣减）
 *
 * 状态（status）：pending/approved/rejected/executed/cancelled
 */
@TableName("refund_requests")
public class RefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 退款ID */
    @TableId(value = "refund_id", type = IdType.ASSIGN_ID)
    private String refundId;

    /** 关联充值记录ID */
    @TableField("recharge_record_id")
    private String rechargeRecordId;

    /** 充值流水号 */
    @TableField("recharge_record_no")
    private String rechargeRecordNo;

    /** 会员ID */
    @TableField("member_id")
    private String memberId;

    /** 会员姓名 */
    @TableField("member_name")
    private String memberName;

    /** 会员手机号 */
    @TableField("member_phone")
    private String memberPhone;

    /** 申请退款金额（分） */
    @TableField("requested_amount")
    private Long requestedAmount;

    /** 可退本金（分，系统计算） */
    @TableField("refundable_principal")
    private Long refundablePrincipal;

    /** 实际退款金额（分） */
    @TableField("actual_refund_amount")
    private Long actualRefundAmount;

    /** 赠送金额处理：clear-清零 proportional-按比例扣减 */
    @TableField("bonus_handling")
    private String bonusHandling;

    /** 退款状态 */
    @TableField("status")
    private String status;

    /** 申请人 */
    @TableField("applicant")
    private String applicant;

    /** 申请时间 */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /** 审批人 */
    @TableField("approver")
    private String approver;

    /** 审批时间 */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /** 审批意见 */
    @TableField("approve_comment")
    private String approveComment;

    /** 退款执行人 */
    @TableField("executor")
    private String executor;

    /** 退款执行时间 */
    @TableField("execute_time")
    private LocalDateTime executeTime;

    /** 退款原因 */
    @TableField("refund_reason")
    private String refundReason;

    /** 退款方式（原路返回） */
    @TableField("refund_method")
    private String refundMethod;

    /** 备注 */
    @TableField("remark")
    private String remark;

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
    /** 状态：待审批 */
    public static final String STATUS_PENDING = "pending";
    /** 状态：已批准 */
    public static final String STATUS_APPROVED = "approved";
    /** 状态：已拒绝 */
    public static final String STATUS_REJECTED = "rejected";
    /** 状态：已执行 */
    public static final String STATUS_EXECUTED = "executed";
    /** 状态：已取消 */
    public static final String STATUS_CANCELLED = "cancelled";
    /** 赠送处理：清零 */
    public static final String BONUS_CLEAR = "clear";
    /** 赠送处理：按比例扣减 */
    public static final String BONUS_PROPORTIONAL = "proportional";

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

    public Long getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(Long requestedAmount) { this.requestedAmount = requestedAmount; }

    public Long getRefundablePrincipal() { return refundablePrincipal; }
    public void setRefundablePrincipal(Long refundablePrincipal) { this.refundablePrincipal = refundablePrincipal; }

    public Long getActualRefundAmount() { return actualRefundAmount; }
    public void setActualRefundAmount(Long actualRefundAmount) { this.actualRefundAmount = actualRefundAmount; }

    public String getBonusHandling() { return bonusHandling; }
    public void setBonusHandling(String bonusHandling) { this.bonusHandling = bonusHandling; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }

    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public String getApproveComment() { return approveComment; }
    public void setApproveComment(String approveComment) { this.approveComment = approveComment; }

    public String getExecutor() { return executor; }
    public void setExecutor(String executor) { this.executor = executor; }

    public LocalDateTime getExecuteTime() { return executeTime; }
    public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getRefundMethod() { return refundMethod; }
    public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
