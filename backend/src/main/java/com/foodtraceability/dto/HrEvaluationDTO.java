package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 人事面评提交DTO
 *
 * <p>HR 对门店初面通过的候选人进行人事面谈，提交面评、学历核验、背调结果。</p>
 */
@Schema(description = "人事面评提交DTO")
public class HrEvaluationDTO {

    @NotBlank(message = "面评内容不能为空")
    @Schema(description = "面评内容", example = "沟通能力好,符合岗位要求",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String evaluation;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 100, message = "评分最大为100")
    @Schema(description = "评分(1-100)", example = "85",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer score;

    @Schema(description = "学历核验结果：pending/pass/fail", example = "pass")
    private String educationVerification;

    @Schema(description = "学历核验备注", example = "学信网核验通过")
    private String educationRemark;

    @Schema(description = "背调结果：pending/pass/fail", example = "pass")
    private String backgroundCheck;

    @Schema(description = "背调备注", example = "无不良记录")
    private String backgroundRemark;

    @NotBlank(message = "人事面谈状态不能为空")
    @Schema(description = "人事面谈状态：pending/scheduled/completed/pass/fail", example = "pass",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getEducationVerification() {
        return educationVerification;
    }

    public void setEducationVerification(String educationVerification) {
        this.educationVerification = educationVerification;
    }

    public String getEducationRemark() {
        return educationRemark;
    }

    public void setEducationRemark(String educationRemark) {
        this.educationRemark = educationRemark;
    }

    public String getBackgroundCheck() {
        return backgroundCheck;
    }

    public void setBackgroundCheck(String backgroundCheck) {
        this.backgroundCheck = backgroundCheck;
    }

    public String getBackgroundRemark() {
        return backgroundRemark;
    }

    public void setBackgroundRemark(String backgroundRemark) {
        this.backgroundRemark = backgroundRemark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
