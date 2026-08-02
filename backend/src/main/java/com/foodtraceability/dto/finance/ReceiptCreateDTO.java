package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 收款登记DTO
 * 用于登记向客户的收款，触发四账联动
 */
@Schema(description = "收款登记请求")
public class ReceiptCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联应收账款ID", required = true)
    @NotNull(message = "应收账款ID不能为空")
    private Long receivableId;

    @Schema(description = "收款金额（单位：分）", required = true)
    @NotNull(message = "收款金额不能为空")
    @Positive(message = "收款金额必须大于0")
    private Long receiptAmount;

    @Schema(description = "收款方式：bank_transfer-银行转账 cash-现金 check-支票", required = true)
    @NotNull(message = "收款方式不能为空")
    private String receiptMethod;

    @Schema(description = "收款银行账户ID（银行转账/支票时必填）", required = true)
    @NotNull(message = "收款银行账户不能为空")
    private Long bankAccountId;

    @Schema(description = "收款日期", required = true)
    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public Long getReceiptAmount() {
        return receiptAmount;
    }

    public void setReceiptAmount(Long receiptAmount) {
        this.receiptAmount = receiptAmount;
    }

    public String getReceiptMethod() {
        return receiptMethod;
    }

    public void setReceiptMethod(String receiptMethod) {
        this.receiptMethod = receiptMethod;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
