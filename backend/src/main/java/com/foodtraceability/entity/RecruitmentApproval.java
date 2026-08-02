package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 招聘审批记录表实体
 * 对应数据库表 recruitment_approvals
 * 用于记录招聘流程中的审批环节，包含面试评估、薪资建议、审批决策等完整信息
 */
@TableName("recruitment_approvals")
@Schema(description = "招聘审批记录实体")
public class RecruitmentApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批记录ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("approval_id")
    @Schema(description = "审批记录ID")
    private String approvalId;

    /** 关联的招聘岗位ID（外键） */
    @TableField("post_id")
    @Schema(description = "关联招聘岗位ID")
    private String postId;

    /** 应聘者姓名 */
    @TableField("applicant_name")
    @Schema(description = "应聘者姓名")
    private String applicantName;

    /** 联系电话 */
    @TableField("phone")
    @Schema(description = "联系电话")
    private String phone;

    /** 建议薪资（单位：分/月，符合项目金额规范） */
    @TableField("proposed_salary")
    @Schema(description = "建议薪资（分/月）")
    private Long proposedSalary;

    /** 面试评分（1-100分） */
    @TableField("interview_score")
    @Schema(description = "面试评分（1-100）")
    private Integer interviewScore;

    /** 面试官ID */
    @TableField("interviewer_id")
    @Schema(description = "面试官ID")
    private String interviewerId;

    /** 面试官评语 */
    @TableField("interviewer_comment")
    @Schema(description = "面试官评语")
    private String interviewerComment;

    /** 当前审批层级（1=初审 2=复审 3=终审） */
    @TableField("current_level")
    @Schema(description = "当前审批层级：1=初审 2=复审 3=终审")
    private Short currentLevel;

    /** 当前审批人ID */
    @TableField("approver_id")
    @Schema(description = "当前审批人ID")
    private String approverId;

    /** 审批状态（pending/approved/rejected/withdrawn/hired） */
    @TableField("approval_status")
    @Schema(description = "审批状态：pending/approved/rejected/withdrawn/hired")
    private String approvalStatus;

    /** 驳回原因 */
    @TableField("rejection_reason")
    @Schema(description = "驳回原因")
    private String rejectionReason;

    /** 审批通过时间 */
    @TableField("approved_at")
    @Schema(description = "审批通过时间")
    private LocalDateTime approvedAt;

    /** 最终决策意见 */
    @TableField("final_decision")
    @Schema(description = "最终决策意见")
    private String finalDecision;

    /** 入职时间 */
    @TableField("hired_at")
    @Schema(description = "入职时间")
    private LocalDateTime hiredAt;

    /** 入职后关联的员工ID */
    @TableField("employee_id")
    @Schema(description = "关联员工ID")
    private String employeeId;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter 方法 ====================

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getProposedSalary() {
        return proposedSalary;
    }

    public void setProposedSalary(Long proposedSalary) {
        this.proposedSalary = proposedSalary;
    }

    public Integer getInterviewScore() {
        return interviewScore;
    }

    public void setInterviewScore(Integer interviewScore) {
        this.interviewScore = interviewScore;
    }

    public String getInterviewerId() {
        return interviewerId;
    }

    public void setInterviewerId(String interviewerId) {
        this.interviewerId = interviewerId;
    }

    public String getInterviewerComment() {
        return interviewerComment;
    }

    public void setInterviewerComment(String interviewerComment) {
        this.interviewerComment = interviewerComment;
    }

    public Short getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Short currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public LocalDateTime getHiredAt() {
        return hiredAt;
    }

    public void setHiredAt(LocalDateTime hiredAt) {
        this.hiredAt = hiredAt;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
