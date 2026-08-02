package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 审批记录实体类
 * 对应数据库表：approval_record
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@TableName("approval_record")
@Schema(description = "审批记录实体")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "审批记录ID")
    private Long id;
    @Schema(description = "档案ID")
    private Long archiveId;
    @Schema(description = "步骤序号（1,2,3...）")
    @TableField("step_number")
    private Integer stepNumber;
    @Schema(description = "步骤名称（如：HR形式审查、直接主管实质审查）")
    @TableField("step_name")
    private String stepName;
    @Schema(description = "审批类型：FORMAL-形式审查, SUBSTANTIVE-实质审查")
    @TableField("review_type")
    private String reviewType;
    @Schema(description = "审批人ID")
    private Long reviewerId;
    @Schema(description = "审批人姓名")
    private String reviewerName;
    @Schema(description = "审批动作：APPROVE-通过, REJECT-拒绝")
    private String action;
    @Schema(description = "审批意见")
    private String comment;
    @Schema(description = "审批时间")
    @TableField("action_time")
    private LocalDateTime actionTime;
    @Schema(description = "审批状态：PENDING-待审批, APPROVED-已通过, REJECTED-已拒绝")
    private String status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    // 审批动作常量
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    // 审批状态常量
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public ApprovalRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public Integer getStepNumber() {
        return this.stepNumber;
    }

    public String getStepName() {
        return this.stepName;
    }

    public String getReviewType() {
        return this.reviewType;
    }

    public Long getReviewerId() {
        return this.reviewerId;
    }

    public String getReviewerName() {
        return this.reviewerName;
    }

    public String getAction() {
        return this.action;
    }

    public String getComment() {
        return this.comment;
    }

    public LocalDateTime getActionTime() {
        return this.actionTime;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setStepNumber(final Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public void setStepName(final String stepName) {
        this.stepName = stepName;
    }

    public void setReviewType(final String reviewType) {
        this.reviewType = reviewType;
    }

    public void setReviewerId(final Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public void setReviewerName(final String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public void setActionTime(final LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ApprovalRecord)) return false;
        final ApprovalRecord other = (ApprovalRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$stepNumber = this.getStepNumber();
        final java.lang.Object other$stepNumber = other.getStepNumber();
        if (this$stepNumber == null ? other$stepNumber != null : !this$stepNumber.equals(other$stepNumber)) return false;
        final java.lang.Object this$reviewerId = this.getReviewerId();
        final java.lang.Object other$reviewerId = other.getReviewerId();
        if (this$reviewerId == null ? other$reviewerId != null : !this$reviewerId.equals(other$reviewerId)) return false;
        final java.lang.Object this$stepName = this.getStepName();
        final java.lang.Object other$stepName = other.getStepName();
        if (this$stepName == null ? other$stepName != null : !this$stepName.equals(other$stepName)) return false;
        final java.lang.Object this$reviewType = this.getReviewType();
        final java.lang.Object other$reviewType = other.getReviewType();
        if (this$reviewType == null ? other$reviewType != null : !this$reviewType.equals(other$reviewType)) return false;
        final java.lang.Object this$reviewerName = this.getReviewerName();
        final java.lang.Object other$reviewerName = other.getReviewerName();
        if (this$reviewerName == null ? other$reviewerName != null : !this$reviewerName.equals(other$reviewerName)) return false;
        final java.lang.Object this$action = this.getAction();
        final java.lang.Object other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) return false;
        final java.lang.Object this$comment = this.getComment();
        final java.lang.Object other$comment = other.getComment();
        if (this$comment == null ? other$comment != null : !this$comment.equals(other$comment)) return false;
        final java.lang.Object this$actionTime = this.getActionTime();
        final java.lang.Object other$actionTime = other.getActionTime();
        if (this$actionTime == null ? other$actionTime != null : !this$actionTime.equals(other$actionTime)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ApprovalRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $stepNumber = this.getStepNumber();
        result = result * PRIME + ($stepNumber == null ? 43 : $stepNumber.hashCode());
        final java.lang.Object $reviewerId = this.getReviewerId();
        result = result * PRIME + ($reviewerId == null ? 43 : $reviewerId.hashCode());
        final java.lang.Object $stepName = this.getStepName();
        result = result * PRIME + ($stepName == null ? 43 : $stepName.hashCode());
        final java.lang.Object $reviewType = this.getReviewType();
        result = result * PRIME + ($reviewType == null ? 43 : $reviewType.hashCode());
        final java.lang.Object $reviewerName = this.getReviewerName();
        result = result * PRIME + ($reviewerName == null ? 43 : $reviewerName.hashCode());
        final java.lang.Object $action = this.getAction();
        result = result * PRIME + ($action == null ? 43 : $action.hashCode());
        final java.lang.Object $comment = this.getComment();
        result = result * PRIME + ($comment == null ? 43 : $comment.hashCode());
        final java.lang.Object $actionTime = this.getActionTime();
        result = result * PRIME + ($actionTime == null ? 43 : $actionTime.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ApprovalRecord(id=" + this.getId() + ", archiveId=" + this.getArchiveId() + ", stepNumber=" + this.getStepNumber() + ", stepName=" + this.getStepName() + ", reviewType=" + this.getReviewType() + ", reviewerId=" + this.getReviewerId() + ", reviewerName=" + this.getReviewerName() + ", action=" + this.getAction() + ", comment=" + this.getComment() + ", actionTime=" + this.getActionTime() + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
