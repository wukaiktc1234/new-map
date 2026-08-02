package com.foodtraceability.dto.recruitment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 名额追加申请DTO
 *
 * <p>门店店长申请追加招聘名额时使用。</p>
 */
@Schema(description = "名额追加申请DTO")
public class QuotaAdjustmentDTO {

    @NotNull(message = "追加数量不能为空")
    @Min(value = 1, message = "追加数量必须大于0")
    @Schema(description = "追加数量", example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer additionalCount;

    @NotBlank(message = "追加原因不能为空")
    @Size(max = 500, message = "追加原因长度不能超过500个字符")
    @Schema(description = "追加原因", example = "周末客流增加",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    public Integer getAdditionalCount() {
        return additionalCount;
    }

    public void setAdditionalCount(Integer additionalCount) {
        this.additionalCount = additionalCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
