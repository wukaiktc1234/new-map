package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 招聘反馈实体类
 * 对应表: recruitment_feedback（Ch3.2 门店↔HR 招聘协同反馈）
 * 主键: feedback_id BIGINT GENERATED ALWAYS AS IDENTITY（自增）
 * requirement_id 类型: VARCHAR(32)（雪花算法，Sprint 1.5 已迁移）
 */
@TableName("recruitment_feedback")
@Schema(description = "招聘反馈实体")
public class RecruitmentFeedback {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID(自增)")
    private Long feedbackId;

    @Schema(description = "招聘需求ID(VARCHAR(32)雪花)")
    private String requirementId;

    @Schema(description = "反馈类型(approve/feedback/reject)")
    private String feedbackType;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "审核人ID(HR)")
    private Long reviewerId;

    @Schema(description = "审核人姓名")
    private String reviewerName;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
