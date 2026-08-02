package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 银行账户VO
 * 账号脱敏显示，仅通过accountNumberMasked字段返回脱敏后的账号
 */
@Schema(description = "银行账户信息")
public class BankAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户ID")
    private Long accountId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "脱敏账号（保留后4位）")
    private String accountNumberMasked;

    @Schema(description = "账户类型：1-基本户 2-一般户 3-备用金 4-其他")
    private Integer accountType;

    @Schema(description = "余额（单位：分）")
    private Long balance;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    @Schema(description = "开户日期")
    private LocalDate openDate;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
