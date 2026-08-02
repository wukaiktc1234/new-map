package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 招聘审批查询DTO
 * 用于招聘审批记录的筛选和分页查询
 */
@Schema(description = "招聘审批查询DTO")
public class ApprovalQueryDTO extends PageQuery {

    @Schema(description = "招聘岗位ID", example = "post001")
    private String postId;

    @Schema(description = "审批状态（pending/approved/rejected/withdrawn/hired）", example = "pending")
    private String approvalStatus;

    @Schema(description = "当前审批人ID", example = "approver001")
    private String approverId;

    /**
     * 当前审批层级：1=初审 2=复审 3=终审
     */
    @Schema(description = "当前审批层级（1=初审 2=复审 3=终审）")
    private Short currentLevel;

    @Schema(description = "应聘者姓名（模糊查询）", example = "张三")
    private String applicantName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间-开始", example = "2026-05-01 00:00:00")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间-结束", example = "2026-05-11 23:59:59")
    private LocalDateTime endDate;

    // ==================== Getter & Setter ====================

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public Short getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Short currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
