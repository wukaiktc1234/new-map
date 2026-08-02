package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 录用Offer视图对象VO
 *
 * <p>薪资字段为 Long(单位:分),前端在边界处完成分↔元转换。</p>
 */
@Schema(description = "录用Offer视图对象")
public class JobOfferVO {

    @Schema(description = "Offer ID", example = "1")
    private Long offerId;

    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)", example = "1789012345678901234")
    private String requirementId;

    @Schema(description = "简历ID(VARCHAR(32)雪花)", example = "1789012345678901234")
    private String resumeId;

    @Schema(description = "面试ID(VARCHAR(32)雪花,可空)", example = "1789012345678901234")
    private String interviewId;

    @Schema(description = "候选人姓名", example = "张三")
    private String candidateName;

    @Schema(description = "候选人邮箱", example = "zhangsan@example.com")
    private String candidateEmail;

    @Schema(description = "岗位ID", example = "2001")
    private Long positionId;

    @Schema(description = "岗位名称", example = "服务员")
    private String positionName;

    @Schema(description = "门店ID(可空)", example = "1001")
    private Long storeId;

    @Schema(description = "门店名称", example = "上海人民广场店")
    private String storeName;

    @Schema(description = "拟定薪资(单位:分)", example = "800000")
    private Long proposedSalary;

    @Schema(description = "试用期月数(0-6)", example = "3")
    private Integer probationMonths;

    @Schema(description = "试用期薪资(单位:分,可空)", example = "640000")
    private Long probationSalary;

    @Schema(description = "状态(pending/sent/accepted/onboarded/rejected/withdrawn)",
            example = "pending")
    private String status;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "接受时间")
    private LocalDateTime acceptTime;

    @Schema(description = "拒绝时间")
    private LocalDateTime rejectTime;

    @Schema(description = "拒绝原因", example = "已接受其他offer")
    private String rejectReason;

    @Schema(description = "预期入职日期", example = "2026-07-01")
    private LocalDate entryDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getProposedSalary() {
        return proposedSalary;
    }

    public void setProposedSalary(Long proposedSalary) {
        this.proposedSalary = proposedSalary;
    }

    public Integer getProbationMonths() {
        return probationMonths;
    }

    public void setProbationMonths(Integer probationMonths) {
        this.probationMonths = probationMonths;
    }

    public Long getProbationSalary() {
        return probationSalary;
    }

    public void setProbationSalary(Long probationSalary) {
        this.probationSalary = probationSalary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public LocalDateTime getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(LocalDateTime acceptTime) {
        this.acceptTime = acceptTime;
    }

    public LocalDateTime getRejectTime() {
        return rejectTime;
    }

    public void setRejectTime(LocalDateTime rejectTime) {
        this.rejectTime = rejectTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
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
}
