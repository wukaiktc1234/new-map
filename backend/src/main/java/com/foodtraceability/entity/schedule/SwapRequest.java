package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 换班申请实体类
 * 存储员工换班请求和审批信息
 */
@TableName("swap_requests")
public class SwapRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "request_id", type = IdType.ASSIGN_ID)
    private String requestId;

    /** 所属排班方案ID */
    @TableField("plan_id")
    private String planId;

    /** 发起人用户ID */
    @TableField("initiator_id")
    private Long initiatorId;

    /** 发起人姓名(冗余) */
    @TableField("initiator_name")
    private String initiatorName;

    /** 发起人的排班条目ID(换出) */
    @TableField("initiator_entry_id")
    private String initiatorEntryId;

    /** 换出的日期 */
    @TableField("initiator_work_date")
    private LocalDate initiatorWorkDate;

    /** 换出的班次类型 */
    @TableField("initiator_shift_type")
    private String initiatorShiftType;

    /** 换出的班次名称 */
    @TableField("initiator_shift_name")
    private String initiatorShiftName;

    /** 换班目标员工ID */
    @TableField("target_employee_id")
    private Long targetEmployeeId;

    /** 目标员工姓名(冗余) */
    @TableField("target_employee_name")
    private String targetEmployeeName;

    /** 目标人的排班条目ID(换入) */
    @TableField("target_entry_id")
    private String targetEntryId;

    /** 换入的日期 */
    @TableField("target_work_date")
    private LocalDate targetWorkDate;

    /** 换入的班次类型 */
    @TableField("target_shift_type")
    private String targetShiftType;

    /** 换入的班次名称 */
    @TableField("target_shift_name")
    private String targetShiftName;

    /**
     * 申请状态(语义化字符串, ADR-001决策)
     * pending-待审批 approved-通过 rejected-拒绝 cancelled-已取消
     */
    @TableField("status")
    private String status;

    /** 换班原因(选填) */
    @TableField("reason")
    private String reason;

    /** 审批人用户ID */
    @TableField("approver_id")
    private Long approverId;

    /** 审批人姓名 */
    @TableField("approver_name")
    private String approverName;

    /** 审批时间 */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /** 拒绝原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 审批意见(通过时记录) */
    @TableField("approve_comment")
    private String approveComment;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 状态常量 ====================

    /** 待审批 */
    public static final String STATUS_PENDING = "pending";
    /** 已通过 */
    public static final String STATUS_APPROVED = "approved";
    /** 已拒绝 */
    public static final String STATUS_REJECTED = "rejected";
    /** 已取消 */
    public static final String STATUS_CANCELLED = "cancelled";

    // ==================== Getter & Setter ====================

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Long getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(Long initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public String getInitiatorEntryId() {
        return initiatorEntryId;
    }

    public void setInitiatorEntryId(String initiatorEntryId) {
        this.initiatorEntryId = initiatorEntryId;
    }

    public LocalDate getInitiatorWorkDate() {
        return initiatorWorkDate;
    }

    public void setInitiatorWorkDate(LocalDate initiatorWorkDate) {
        this.initiatorWorkDate = initiatorWorkDate;
    }

    public String getInitiatorShiftType() {
        return initiatorShiftType;
    }

    public void setInitiatorShiftType(String initiatorShiftType) {
        this.initiatorShiftType = initiatorShiftType;
    }

    public String getInitiatorShiftName() {
        return initiatorShiftName;
    }

    public void setInitiatorShiftName(String initiatorShiftName) {
        this.initiatorShiftName = initiatorShiftName;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(Long targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
    }

    public String getTargetEmployeeName() {
        return targetEmployeeName;
    }

    public void setTargetEmployeeName(String targetEmployeeName) {
        this.targetEmployeeName = targetEmployeeName;
    }

    public String getTargetEntryId() {
        return targetEntryId;
    }

    public void setTargetEntryId(String targetEntryId) {
        this.targetEntryId = targetEntryId;
    }

    public LocalDate getTargetWorkDate() {
        return targetWorkDate;
    }

    public void setTargetWorkDate(LocalDate targetWorkDate) {
        this.targetWorkDate = targetWorkDate;
    }

    public String getTargetShiftType() {
        return targetShiftType;
    }

    public void setTargetShiftType(String targetShiftType) {
        this.targetShiftType = targetShiftType;
    }

    public String getTargetShiftName() {
        return targetShiftName;
    }

    public void setTargetShiftName(String targetShiftName) {
        this.targetShiftName = targetShiftName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getApproveComment() {
        return approveComment;
    }

    public void setApproveComment(String approveComment) {
        this.approveComment = approveComment;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
}
