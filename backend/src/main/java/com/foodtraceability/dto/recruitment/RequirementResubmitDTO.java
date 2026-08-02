package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 招聘需求重新提交DTO
 *
 * <p>门店收到 HR "feedback"(需修改) 反馈后,修改字段并重新提交。</p>
 *
 * <p>所有字段均可选,仅更新提交的字段。</p>
 */
@Schema(description = "招聘需求重新提交DTO")
public class RequirementResubmitDTO {

    @Schema(description = "需求描述")
    private String description;

    @Schema(description = "需求明细")
    private String requirements;

    @Schema(description = "薪资范围", example = "6000-8000")
    private String salaryRange;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }
}
