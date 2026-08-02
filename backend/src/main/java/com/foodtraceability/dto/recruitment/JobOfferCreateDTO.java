package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 录用Offer创建DTO
 *
 * <p>HR 创建录用 Offer,创建后状态为 pending,发送时才触发 offer.sent 事件。</p>
 *
 * <p>薪资字段使用 Long(单位:分),前端在边界处完成元↔分转换。</p>
 */
@Schema(description = "录用Offer创建DTO")
public class JobOfferCreateDTO {

    @NotBlank(message = "招聘需求ID不能为空")
    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)", example = "1789012345678901234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String requirementId;

    @NotBlank(message = "简历ID不能为空")
    @Schema(description = "简历ID(VARCHAR(32)雪花)", example = "1789012345678901234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String resumeId;

    @Schema(description = "面试ID(VARCHAR(32)雪花,可空)", example = "1789012345678901234")
    private String interviewId;

    @NotBlank(message = "候选人姓名不能为空")
    @Size(max = 100, message = "候选人姓名长度不能超过100个字符")
    @Schema(description = "候选人姓名", example = "张三",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String candidateName;

    @Size(max = 100, message = "候选人邮箱长度不能超过100个字符")
    @Schema(description = "候选人邮箱(发送Offer邮件用)", example = "zhangsan@example.com")
    private String candidateEmail;

    @NotNull(message = "岗位ID不能为空")
    @Schema(description = "岗位ID", example = "2001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long positionId;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 100, message = "岗位名称长度不能超过100个字符")
    @Schema(description = "岗位名称", example = "服务员",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String positionName;

    @Schema(description = "门店ID(可空,HR通用岗位无门店)", example = "1001")
    private Long storeId;

    @Size(max = 100, message = "门店名称长度不能超过100个字符")
    @Schema(description = "门店名称", example = "上海人民广场店")
    private String storeName;

    @NotNull(message = "拟定薪资不能为空")
    @Min(value = 1, message = "拟定薪资必须大于0")
    @Schema(description = "拟定薪资(单位:分)", example = "800000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long proposedSalary;

    @NotNull(message = "试用期月数不能为空")
    @Min(value = 0, message = "试用期月数最小为0")
    @Max(value = 6, message = "试用期月数最大为6")
    @Schema(description = "试用期月数(0-6,默认3)", example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer probationMonths;

    @Min(value = 0, message = "试用期薪资必须大于等于0")
    @Schema(description = "试用期薪资(单位:分,可空)", example = "640000")
    private Long probationSalary;

    @Schema(description = "预期入职日期", example = "2026-07-01")
    private LocalDate entryDate;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

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
}
