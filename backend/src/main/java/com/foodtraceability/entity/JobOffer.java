package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 录用Offer实体类
 * 对应表: job_offers（Ch3.3 录用 Offer 持久化）
 * 主键: offer_id BIGINT GENERATED ALWAYS AS IDENTITY（自增）
 * requirement_id / resume_id / interview_id 类型: VARCHAR(32)（雪花算法，Sprint 1.5 已迁移）
 * 薪资字段: Long（单位:分，遵循 project_rules.md 金额存储规范）
 */
@TableName("job_offers")
@Schema(description = "录用Offer实体")
public class JobOffer {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID(自增)")
    private Long offerId;

    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)")
    private String requirementId;

    @Schema(description = "简历ID(VARCHAR(32)雪花)")
    private String resumeId;

    @Schema(description = "面试ID(VARCHAR(32)雪花,可空)")
    private String interviewId;

    @Schema(description = "候选人姓名")
    private String candidateName;

    @Schema(description = "候选人邮箱(发送Offer邮件用)")
    private String candidateEmail;

    @Schema(description = "岗位ID")
    private Long positionId;

    @Schema(description = "岗位名称(冗余)")
    private String positionName;

    @Schema(description = "门店ID(可空)")
    private Long storeId;

    @Schema(description = "门店名称(冗余)")
    private String storeName;

    @Schema(description = "拟定薪资(单位:分)")
    private Long proposedSalary;

    @Schema(description = "试用期月数(0-6,默认3)")
    private Integer probationMonths;

    @Schema(description = "试用期薪资(单位:分,可空)")
    private Long probationSalary;

    @Schema(description = "状态(pending/sent/accepted/onboarded/rejected/withdrawn)")
    private String status;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "接受时间")
    private LocalDateTime acceptTime;

    @Schema(description = "拒绝时间")
    private LocalDateTime rejectTime;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "预期入职日期")
    private LocalDate entryDate;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除:0=未删除 1=已删除")
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
