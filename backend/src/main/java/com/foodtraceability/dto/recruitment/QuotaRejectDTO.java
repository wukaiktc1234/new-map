package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 名额拒绝DTO
 *
 * <p>门店店长拒绝接收名额时提交拒绝原因。</p>
 */
@Schema(description = "名额拒绝DTO")
public class QuotaRejectDTO {

    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 500, message = "拒绝原因长度不能超过500个字符")
    @Schema(description = "拒绝原因", example = "人员已满",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
