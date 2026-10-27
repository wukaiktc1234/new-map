package com.foodtraceability.dto.finance.deposit;

/** SL6 存行入参：关联外部到账流水 bank_transactions.transaction_id（E3-8/E3-9 用）。 */
public class DepositToBankDTO {

    /** 差异3：**可选** —— 为空表示「人工汇总模式」（未绑定到账流水，仍可存行，审计标 matched=false） */
    private Long bankTransactionId;

    /** 差异3/NEW-35：人工汇总模式下**显式指定**收款银行户（本门店 + account_type=1 + status=1） */
    private Long bankAccountId;

    private String depositRefNo;

    private String remark;

    public Long getBankTransactionId() { return bankTransactionId; }
    public void setBankTransactionId(Long bankTransactionId) { this.bankTransactionId = bankTransactionId; }

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

    public String getDepositRefNo() { return depositRefNo; }
    public void setDepositRefNo(String depositRefNo) { this.depositRefNo = depositRefNo; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
