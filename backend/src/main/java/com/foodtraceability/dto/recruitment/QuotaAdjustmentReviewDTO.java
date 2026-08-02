package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 名额追加审核DTO
 *
 * <p>HR 审核门店提交的名额追加申请。</p>
 */
@Schema(description = "名额追加审核DTO")
public class QuotaAdjustmentReviewDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "是否通过(true=通过,false=驳回)", example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;

    @Size(max = 500, message = "审核备注长度不能超过500个字符")
    @Schema(description = "审核备注", example = "同意追加")
    private String remark;

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
