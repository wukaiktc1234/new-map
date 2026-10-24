package com.foodtraceability.dto.finance.deposit;

import jakarta.validation.constraints.NotNull;

/** SL5 收款入参：实收金额（分）。短款=应交−实收，只记在单据上。 */
public class ReceiveSlipDTO {

    @NotNull(message = "receivedAmountFen 不能为空")
    private Long receivedAmountFen;

    private String remark;

    public Long getReceivedAmountFen() { return receivedAmountFen; }
    public void setReceivedAmountFen(Long receivedAmountFen) { this.receivedAmountFen = receivedAmountFen; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
