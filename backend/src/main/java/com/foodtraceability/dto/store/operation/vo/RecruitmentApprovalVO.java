package com.foodtraceability.dto.store.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 招聘审批视图对象VO
 * 用于返回给前端的招聘审批记录信息
 * 包含完整的审批流程信息和历史记录
 */
@Schema(description = "招聘审批视图对象")
public class RecruitmentApprovalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础字段 ====================

    /** 审批记录ID */
    @Schema(description = "审批记录ID", example = "1234567890123456789")
    private String approvalId;

    /** 关联的招聘岗位ID */
    @Schema(description = "招聘岗位ID", example = "post001")
    private String postId;

    /** 岗位名称（关联查询） */
    @Schema(description = "岗位名称", example = "厨师长")
    private String postName;

    /** 应聘者姓名 */
    @Schema(description = "应聘者姓名", example = "赵六")
    private String applicantName;

    /** 联系电话（脱敏显示：138****8000） */
    @Schema(description = "联系电话（脱敏）", example = "138****8000")
    private String phone;

    /**
     * 建议薪资（单位：元/月）
     * 由DataConverter将数据库中的分转换为元用于前端显示
     */
    @Schema(description = "建议薪资（元/月）", example = "8000.00")
    private String proposedSalary;

    /** 面试评分（1-100分） */
    @Schema(description = "面试评分（1-100）", example = "85")
    private Integer interviewScore;

    /** 面试官ID */
    @Schema(description = "面试官ID", example = "interviewer001")
    private String interviewerId;

    /** 面试官姓名（关联查询） */
    @Schema(description = "面试官姓名", example = "孙七")
    private String interviewerName;

    /** 当前审批层级（1=初审 2=复审 3=终审） */
    @Schema(description = "当前审批层级：1=初审 2=复审 3=终审", example = "2")
    private Short currentLevel;

    /** 当前审批层级名称 */
    @Schema(description = "当前审批层级名称", example = "复审")
    private String currentLevelName;

    /** 审批状态（pending/approved/rejected/withdrawn/hired） */
    @Schema(description = "审批状态", example = "pending")
    private String approvalStatus;

    /** 审批状态名称 */
    @Schema(description = "审批状态名称", example = "待审批")
    private String approvalStatusName;

    /** 驳回原因 */
    @Schema(description = "驳回原因", example = "该应聘者缺乏相关岗位的工作经验")
    private String rejectionReason;

    /** 审批通过时间 */
    @Schema(description = "审批通过时间", example = "2026-05-11T16:30:00")
    private LocalDateTime approvedAt;

    /** 最终决策意见 */
    @Schema(description = "最终决策意见", example = "同意录用，薪资按建议执行")
    private String finalDecision;

    /** 入职时间 */
    @Schema(description = "入职时间", example = "2026-05-15T09:00:00")
    private LocalDateTime hiredAt;

    /** 入职后关联的员工ID */
    @Schema(description = "关联员工ID", example = "emp001")
    private String employeeId;

    /** 创建时间 */
    @Schema(description = "创建时间", example = "2026-05-10T14:00:00")
    private LocalDateTime createTime;

    // ==================== 扩展字段 ====================

    /**
     * 审批历史记录列表
     * 记录完整的审批流转过程，包括每次审批的时间、操作人、意见等
     * 用于前端展示审批时间线
     */
    @Schema(description = "审批历史记录")
    private List<ApprovalHistoryItem> approvalHistory;

    // ==================== 内部类：审批历史项 ====================

    /**
     * 审批历史项
     * 用于记录每一次审批操作的详细信息
     */
    @Schema(description = "审批历史项")
    public static class ApprovalHistoryItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 历史记录ID */
        @Schema(description = "历史记录ID", example = "hist001")
        private String historyId;

        /** 操作类型（submit/approve/reject/withdraw） */
        @Schema(description = "操作类型", example = "approve")
        private String action;

        /** 操作类型名称 */
        @Schema(description = "操作类型名称", example = "审批通过")
        private String actionName;

        /** 操作人ID */
        @Schema(description = "操作人ID", example = "user001")
        private String operatorId;

        /** 操作人姓名 */
        @Schema(description = "操作人姓名", example = "张三")
        private String operatorName;

        /** 操作时的审批层级 */
        @Schema(description = "审批层级", example = "1")
        private Short level;

        /** 操作意见/备注 */
        @Schema(description = "操作意见", example = "同意，候选人符合岗位要求")
        private String comment;

        /** 操作时间 */
        @Schema(description = "操作时间", example = "2026-05-11T10:30:00")
        private LocalDateTime operateTime;

        // Getter和Setter
        public String getHistoryId() { return historyId; }
        public void setHistoryId(String historyId) { this.historyId = historyId; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getActionName() { return actionName; }
        public void setActionName(String actionName) { this.actionName = actionName; }

        public String getOperatorId() { return operatorId; }
        public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

        public String getOperatorName() { return operatorName; }
        public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

        public Short getLevel() { return level; }
        public void setLevel(Short level) { this.level = level; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public LocalDateTime getOperateTime() { return operateTime; }
        public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
    }

    // ==================== Getter & Setter ====================

    public String getApprovalId() { return approvalId; }
    public void setApprovalId(String approvalId) { this.approvalId = approvalId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProposedSalary() { return proposedSalary; }
    public void setProposedSalary(String proposedSalary) { this.proposedSalary = proposedSalary; }

    public Integer getInterviewScore() { return interviewScore; }
    public void setInterviewScore(Integer interviewScore) { this.interviewScore = interviewScore; }

    public String getInterviewerId() { return interviewerId; }
    public void setInterviewerId(String interviewerId) { this.interviewerId = interviewerId; }

    public String getInterviewerName() { return interviewerName; }
    public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }

    public Short getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Short currentLevel) { this.currentLevel = currentLevel; }

    public String getCurrentLevelName() { return currentLevelName; }
    public void setCurrentLevelName(String currentLevelName) { this.currentLevelName = currentLevelName; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getApprovalStatusName() { return approvalStatusName; }
    public void setApprovalStatusName(String approvalStatusName) { this.approvalStatusName = approvalStatusName; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getFinalDecision() { return finalDecision; }
    public void setFinalDecision(String finalDecision) { this.finalDecision = finalDecision; }

    public LocalDateTime getHiredAt() { return hiredAt; }
    public void setHiredAt(LocalDateTime hiredAt) { this.hiredAt = hiredAt; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getApproverId() { return null; }
    public void setApproverId(String approverId) { }

    public List<ApprovalHistoryItem> getApprovalHistory() { return approvalHistory; }
    public void setApprovalHistory(List<ApprovalHistoryItem> approvalHistory) { this.approvalHistory = approvalHistory; }
}
