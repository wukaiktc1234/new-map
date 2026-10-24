package com.foodtraceability.dto.finance.deposit;

import jakarta.validation.constraints.NotBlank;

/** SL9 驳回入参：原因必填。 */
public class RejectSlipDTO {

    @NotBlank(message = "reason 不能为空")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
