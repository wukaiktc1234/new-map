package com.foodtraceability.dto.finance.deposit;

import jakarta.validation.constraints.NotNull;

/** SL6 存行入参：关联外部到账流水 bank_transactions.transaction_id（E3-8/E3-9 用）。 */
public class DepositToBankDTO {

    @NotNull(message = "bankTransactionId 不能为空")
    private Long bankTransactionId;

    private String depositRefNo;

    private String remark;

    public Long getBankTransactionId() { return bankTransactionId; }
    public void setBankTransactionId(Long bankTransactionId) { this.bankTransactionId = bankTransactionId; }

    public String getDepositRefNo() { return depositRefNo; }
    public void setDepositRefNo(String depositRefNo) { this.depositRefNo = depositRefNo; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
