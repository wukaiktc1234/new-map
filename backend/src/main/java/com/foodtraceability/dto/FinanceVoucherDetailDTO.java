package com.foodtraceability.dto;

public class FinanceVoucherDetailDTO {

    private Long id;

    private Long voucherId;

    private String accountCode;

    private String accountName;

    private String accountType;

    /**
     * 借方金额（单位：分）
     * 后端和数据库统一使用 Long 分，前端通过 DataConverter 转换为元
     */
    private Long debitAmount;

    /**
     * 贷方金额（单位：分）
     * 后端和数据库统一使用 Long 分，前端通过 DataConverter 转换为元
     */
    private Long creditAmount;

    private String summary;

    private Integer lineNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Long getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(Long debitAmount) {
        this.debitAmount = debitAmount;
    }

    public Long getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(Long creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }
}
