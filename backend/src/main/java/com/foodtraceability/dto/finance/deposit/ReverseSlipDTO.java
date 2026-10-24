package com.foodtraceability.dto.finance.deposit;

import jakarta.validation.constraints.NotBlank;

/** SL10 冲正入参：原因必填（只新增反向流水，原流水置 status=3）。 */
public class ReverseSlipDTO {

    @NotBlank(message = "reason 不能为空")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
