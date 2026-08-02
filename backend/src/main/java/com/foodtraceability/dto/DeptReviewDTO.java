package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 部门审核 DTO（pending_review → pending_final/rejected）
 */
@Schema(description = "部门审核DTO")
public class DeptReviewDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核通过（true）或退回（false）", required = true)
    private Boolean approved;

    @Schema(description = "审核意见")
    private String comment;

    @Schema(description = "退回原因（approved 为 false 时必填）")
    private String rejectReason;

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
