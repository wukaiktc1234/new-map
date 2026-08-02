package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 退款审批DTO
 * 对应前端 PUT /v1/refunds/{refundId}/approve
 */
@Schema(description = "退款审批请求")
public class RefundApproveDTO {

    @Schema(description = "是否同意退款", example = "true")
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Schema(description = "审批意见", example = "同意退款")
    private String comment;

    // ==================== Getter & Setter ====================

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
