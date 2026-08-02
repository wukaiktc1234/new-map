package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 招聘审批创建DTO
 * 用于提交录用申请，包含应聘者基本信息、面试评估和薪资建议
 */
@Schema(description = "招聘审批创建DTO")
public class RecruitmentApprovalCreateDTO {

    /** 关联的招聘岗位ID */
    @NotBlank(message = "招聘岗位ID不能为空")
    @Schema(description = "招聘岗位ID", example = "post001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String postId;

    /** 应聘者姓名 */
    @NotBlank(message = "应聘者姓名不能为空")
    @Size(max = 100, message = "应聘者姓名长度不能超过100个字符")
    @Schema(description = "应聘者姓名", example = "赵六", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applicantName;

    /**
     * 联系电话
     * 支持中国大陆手机号格式
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "联系电话", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    /**
     * 建议薪资（单位：元/月）
     * 前端传入元，后端转换为分存储到数据库
     * 范围：0-100000元/月
     */
    @Min(value = 0, message = "建议薪资不能小于0")
    @Max(value = 100000, message = "建议薪资不能超过100000")
    @Schema(description = "建议薪资（单位：元/月）", example = "8000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String proposedSalary;

    /**
     * 面试评分（1-100分）
     * 综合评估应聘者的能力、经验、态度等维度
     */
    @Min(value = 1, message = "面试评分不能小于1")
    @Max(value = 100, message = "面试评分不能大于100")
    @Schema(description = "面试评分（1-100）", example = "85", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer interviewScore;

    /**
     * 面试官评语
     * 要求详细说明面试情况、优缺点分析、录用建议等
     */
    @NotBlank(message = "面试官评语不能为空")
    @Size(min = 20, max = 1000, message = "面试官评语长度必须在20-1000个字符之间")
    @Schema(description = "面试官评语（20-1000字）", example = "该应聘者有5年餐饮行业经验，熟悉前厅管理流程...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String interviewerComment;

    /**
     * 面试官ID
     */
    @Schema(description = "面试官ID", example = "interviewer001")
    private String interviewerId;

    /**
     * 岗位编码
     */
    @Schema(description = "岗位编码", example = "chef_head")
    private String positionCode;

    // ==================== Getter & Setter ====================

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

    public String getProposedSalary() {
        return proposedSalary;
    }

    public void setProposedSalary(String proposedSalary) {
        this.proposedSalary = proposedSalary;
    }

    public Integer getInterviewScore() {
        return interviewScore;
    }

    public void setInterviewScore(Integer interviewScore) {
        this.interviewScore = interviewScore;
    }

    public String getInterviewerComment() {
        return interviewerComment;
    }

    public void setInterviewerComment(String interviewerComment) {
        this.interviewerComment = interviewerComment;
    }

    public String getInterviewerId() {
        return interviewerId;
    }

    public void setInterviewerId(String interviewerId) {
        this.interviewerId = interviewerId;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }
}
