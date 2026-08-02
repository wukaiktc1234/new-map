package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 银行账户创建DTO
 */
@Schema(description = "银行账户创建请求")
public class BankAccountCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户名称", required = true)
    @NotBlank(message = "账户名称不能为空")
    @Size(max = 100, message = "账户名称长度不能超过100位")
    private String accountName;

    @Schema(description = "银行名称")
    @Size(max = 100, message = "银行名称长度不能超过100位")
    private String bankName;

    @Schema(description = "账号", required = true)
    @NotBlank(message = "账号不能为空")
    @Size(max = 50, message = "账号长度不能超过50位")
    private String accountNumber;

    @Schema(description = "账户类型：1-基本户 2-一般户 3-备用金 4-其他", required = true)
    @NotNull(message = "账户类型不能为空")
    private Integer accountType;

    @Schema(description = "开户日期")
    private LocalDate openDate;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
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
}
