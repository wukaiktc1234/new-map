package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 银行账户更新DTO
 */
@Schema(description = "银行账户更新请求")
public class BankAccountUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户名称")
    @Size(max = 100, message = "账户名称长度不能超过100位")
    private String accountName;

    @Schema(description = "银行名称")
    @Size(max = 100, message = "银行名称长度不能超过100位")
    private String bankName;

    @Schema(description = "账户类型：1-基本户 2-一般户 3-备用金 4-其他")
    private Integer accountType;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

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

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
