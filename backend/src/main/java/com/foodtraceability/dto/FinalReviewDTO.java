package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 店长终审 DTO（pending_final → published/rejected）
 */
@Schema(description = "店长终审DTO")
public class FinalReviewDTO {

    @NotNull(message = "终审结果不能为空")
    @Schema(description = "终审通过（true）或退回（false）", required = true)
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
