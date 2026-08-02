package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@TableName("interviews")
@Schema(description = "面试记录实体")
public class Interview {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "面试编号")
    private String interviewCode;

    @Schema(description = "简历ID")
    private String resumeId;

    @Schema(description = "招聘需求ID")
    private String requirementId;

    @Schema(description = "面试轮次（first：初试，second：复试，final：终试）")
    private String interviewRound;

    @Schema(description = "面试类型（online：线上，offline：线下）")
    private String interviewType;

    @Schema(description = "面试时间")
    private LocalDateTime interviewDate;

    @Schema(description = "面试地点")
    private String interviewLocation;

    @Schema(description = "面试官ID")
    private String interviewerId;

    @Schema(description = "面试官姓名")
    private String interviewerName;

    @Schema(description = "状态（scheduled：已安排，completed：已完成，cancelled：已取消）")
    private String status;

    @Schema(description = "面试结果（pass：通过，fail：未通过，pending：待定）")
    private String result;

    @Schema(description = "面试分数")
    private Integer score;

    @Schema(description = "面试反馈")
    private String feedback;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "门店面试官ID(用户ID,字符串兼容)")
    private String storeInterviewerId;

    @Schema(description = "门店面试官姓名")
    private String storeInterviewerName;

    @Schema(description = "门店面评内容")
    private String storeEvaluation;

    @Schema(description = "门店面评提交时间")
    private LocalDateTime storeEvaluationTime;

    @Schema(description = "门店面评评分(1-5)")
    private Integer storeEvaluationScore;

    @Schema(description = "人事面试官ID")
    private String hrInterviewerId;

    @Schema(description = "人事面试官姓名")
    private String hrInterviewerName;

    @Schema(description = "人事面评内容")
    private String hrEvaluation;

    @Schema(description = "人事面评提交时间")
    private LocalDateTime hrEvaluationTime;

    @Schema(description = "人事面评评分")
    private Integer hrEvaluationScore;

    @Schema(description = "学历核验结果：pending/pass/fail")
    private String educationVerification;

    @Schema(description = "学历核验备注")
    private String educationVerificationRemark;

    @Schema(description = "背调结果：pending/pass/fail")
    private String backgroundCheckResult;

    @Schema(description = "背调备注")
    private String backgroundCheckRemark;

    @Schema(description = "人事面谈状态：pending/scheduled/completed/pass/fail")
    private String hrInterviewStatus;

    @Schema(description = "创建人ID")
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // Getter and Setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInterviewCode() {
        return interviewCode;
    }

    public void setInterviewCode(String interviewCode) {
        this.interviewCode = interviewCode;
    }

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getInterviewRound() {
        return interviewRound;
    }

    public void setInterviewRound(String interviewRound) {
        this.interviewRound = interviewRound;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public LocalDateTime getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDateTime interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getInterviewLocation() {
        return interviewLocation;
    }

    public void setInterviewLocation(String interviewLocation) {
        this.interviewLocation = interviewLocation;
    }

    public String getInterviewerId() {
        return interviewerId;
    }

    public void setInterviewerId(String interviewerId) {
        this.interviewerId = interviewerId;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStoreInterviewerId() {
        return storeInterviewerId;
    }

    public void setStoreInterviewerId(String storeInterviewerId) {
        this.storeInterviewerId = storeInterviewerId;
    }

    public String getStoreInterviewerName() {
        return storeInterviewerName;
    }

    public void setStoreInterviewerName(String storeInterviewerName) {
        this.storeInterviewerName = storeInterviewerName;
    }

    public String getStoreEvaluation() {
        return storeEvaluation;
    }

    public void setStoreEvaluation(String storeEvaluation) {
        this.storeEvaluation = storeEvaluation;
    }

    public LocalDateTime getStoreEvaluationTime() {
        return storeEvaluationTime;
    }

    public void setStoreEvaluationTime(LocalDateTime storeEvaluationTime) {
        this.storeEvaluationTime = storeEvaluationTime;
    }

    public Integer getStoreEvaluationScore() {
        return storeEvaluationScore;
    }

    public void setStoreEvaluationScore(Integer storeEvaluationScore) {
        this.storeEvaluationScore = storeEvaluationScore;
    }

    public String getHrInterviewerId() {
        return hrInterviewerId;
    }

    public void setHrInterviewerId(String hrInterviewerId) {
        this.hrInterviewerId = hrInterviewerId;
    }

    public String getHrInterviewerName() {
        return hrInterviewerName;
    }

    public void setHrInterviewerName(String hrInterviewerName) {
        this.hrInterviewerName = hrInterviewerName;
    }

    public String getHrEvaluation() {
        return hrEvaluation;
    }

    public void setHrEvaluation(String hrEvaluation) {
        this.hrEvaluation = hrEvaluation;
    }

    public LocalDateTime getHrEvaluationTime() {
        return hrEvaluationTime;
    }

    public void setHrEvaluationTime(LocalDateTime hrEvaluationTime) {
        this.hrEvaluationTime = hrEvaluationTime;
    }

    public Integer getHrEvaluationScore() {
        return hrEvaluationScore;
    }

    public void setHrEvaluationScore(Integer hrEvaluationScore) {
        this.hrEvaluationScore = hrEvaluationScore;
    }

    public String getEducationVerification() {
        return educationVerification;
    }

    public void setEducationVerification(String educationVerification) {
        this.educationVerification = educationVerification;
    }

    public String getEducationVerificationRemark() {
        return educationVerificationRemark;
    }

    public void setEducationVerificationRemark(String educationVerificationRemark) {
        this.educationVerificationRemark = educationVerificationRemark;
    }

    public String getBackgroundCheckResult() {
        return backgroundCheckResult;
    }

    public void setBackgroundCheckResult(String backgroundCheckResult) {
        this.backgroundCheckResult = backgroundCheckResult;
    }

    public String getBackgroundCheckRemark() {
        return backgroundCheckRemark;
    }

    public void setBackgroundCheckRemark(String backgroundCheckRemark) {
        this.backgroundCheckRemark = backgroundCheckRemark;
    }

    public String getHrInterviewStatus() {
        return hrInterviewStatus;
    }

    public void setHrInterviewStatus(String hrInterviewStatus) {
        this.hrInterviewStatus = hrInterviewStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
