package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Offer拒绝DTO
 *
 * <p>候选人拒绝 Offer 时提交拒绝原因(可选)。</p>
 */
@Schema(description = "Offer拒绝DTO")
public class OfferRejectDTO {

    @Size(max = 500, message = "拒绝原因长度不能超过500个字符")
    @Schema(description = "拒绝原因", example = "已接受其他offer")
    private String rejectReason;

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
