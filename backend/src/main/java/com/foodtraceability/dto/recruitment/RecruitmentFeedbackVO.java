package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 招聘反馈视图对象VO
 */
@Schema(description = "招聘反馈视图对象")
public class RecruitmentFeedbackVO {

    @Schema(description = "反馈ID", example = "1")
    private Long feedbackId;

    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)", example = "1789012345678901234")
    private String requirementId;

    @Schema(description = "反馈类型(approve/feedback/reject)", example = "approve")
    private String feedbackType;

    @Schema(description = "反馈内容", example = "需求已审核通过")
    private String content;

    @Schema(description = "审核人ID(HR)", example = "100")
    private Long reviewerId;

    @Schema(description = "审核人姓名", example = "李HR")
    private String reviewerName;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

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

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
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
