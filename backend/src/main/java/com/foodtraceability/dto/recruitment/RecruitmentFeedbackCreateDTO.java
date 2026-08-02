package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 招聘反馈创建DTO
 *
 * <p>HR 对门店提报的招聘需求提交反馈(approve/feedback/reject)。</p>
 */
@Schema(description = "招聘反馈创建DTO")
public class RecruitmentFeedbackCreateDTO {

    @NotBlank(message = "招聘需求ID不能为空")
    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)", example = "1789012345678901234",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String requirementId;

    @NotBlank(message = "反馈类型不能为空")
    @Pattern(regexp = "^(approve|feedback|reject)$",
            message = "反馈类型必须为approve、feedback或reject")
    @Schema(description = "反馈类型(approve=通过,feedback=需修改,reject=驳回)", example = "approve",
            allowableValues = {"approve", "feedback", "reject"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String feedbackType;

    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容", example = "需求已审核通过,请开始招聘",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
