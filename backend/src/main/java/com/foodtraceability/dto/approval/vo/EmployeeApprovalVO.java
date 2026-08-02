package com.foodtraceability.dto.approval.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工日常审批列表项VO
 * 用于审批列表页面的展示，包含核心摘要信息
 * 不包含完整表单数据，减少列表查询的数据传输量
 */
@Schema(description = "员工日常审批列表项VO")
public class EmployeeApprovalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础字段 ====================

    /** 审批ID */
    @Schema(description = "审批ID", example = "a1b2c3d4e5f6...")
    private String id;

    /** 审批编号 */
    @Schema(description = "审批编号", example = "AP20260301001")
    private String approvalNo;

    /**
     * 审批类型
     * leave=请假, overtime=加班, swap=换班, travel=出差, reimbursement=报销, requisition=领用
     */
    @Schema(description = "审批类型", example = "leave")
    private String type;

    /** 标题（如 "年假申请-张三"） */
    @Schema(description = "标题", example = "年假申请-张三")
    private String title;

    /**
     * 审批状态
     * pending=待审批, approved=已通过, rejected=已驳回, withdrawn=已撤回
     */
    @Schema(description = "审批状态", example = "pending")
    private String status;

    /**
     * 优先级
     * normal=普通, urgent=紧急, critical=特急
     */
    @Schema(description = "优先级", example = "normal")
    private String priority;

    // ==================== 申请人信息 ====================

    /** 申请人ID */
    @Schema(description = "申请人ID", example = "emp001")
    private String applicantId;

    /** 申请人姓名（关联查询填充） */
    @Schema(description = "申请人姓名", example = "张三")
    private String applicantName;

    /** 申请人头像URL */
    @Schema(description = "申请人头像URL", example = "/api/files/avatar/emp001.jpg")
    private String applicantAvatar;

    // ==================== 组织信息 ====================

    /** 部门名称（关联查询填充） */
    @Schema(description = "部门名称", example = "前厅部")
    private String departmentName;

    /** 职位名称（关联查询填充） */
    @Schema(description = "职位名称", example = "服务员")
    private String positionName;

    // ==================== 时间信息 ====================

    /** 提交时间 */
    @Schema(description = "提交时间", example = "2026-06-03T10:30:00")
    private LocalDateTime submitTime;

    /** 审批完成时间（通过/驳回时记录，可为null） */
    @Schema(description = "审批完成时间", example = "2026-06-03T14:20:00")
    private LocalDateTime approveTime;

    // ==================== 审批人信息 ====================

    /** 当前审批人姓名（关联查询填充） */
    @Schema(description = "当前审批人姓名", example = "王经理")
    private String currentApproverName;

    // ==================== 风险与摘要 ====================

    /**
     * 风险等级
     * null=无风险, info=提示, warning=警告, danger=危险, prohibited=禁止
     */
    @Schema(description = "风险等级", example = "info")
    private String riskLevel;

    /**
     * 表单摘要（JSON字符串）
     * 用于列表预览的核心信息快照
     */
    @Schema(description = "表单摘要（预览用）")
    private String formSummary;

    // ==================== Getter & Setter 方法 ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApprovalNo() {
        return approvalNo;
    }

    public void setApprovalNo(String approvalNo) {
        this.approvalNo = approvalNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantAvatar() {
        return applicantAvatar;
    }

    public void setApplicantAvatar(String applicantAvatar) {
        this.applicantAvatar = applicantAvatar;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public String getCurrentApproverName() {
        return currentApproverName;
    }

    public void setCurrentApproverName(String currentApproverName) {
        this.currentApproverName = currentApproverName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getFormSummary() {
        return formSummary;
    }

    public void setFormSummary(String formSummary) {
        this.formSummary = formSummary;
    }
}
