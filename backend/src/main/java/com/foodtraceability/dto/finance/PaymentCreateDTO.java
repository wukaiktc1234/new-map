package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 付款登记DTO
 * 用于登记向供应商的付款，触发四账联动
 */
@Schema(description = "付款登记请求")
public class PaymentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联应付账款ID", required = true)
    @NotNull(message = "应付账款ID不能为空")
    private Long payableId;

    @Schema(description = "付款金额（单位：分）", required = true)
    @NotNull(message = "付款金额不能为空")
    @Positive(message = "付款金额必须大于0")
    private Long paymentAmount;

    @Schema(description = "付款方式：bank_transfer-银行转账 cash-现金 check-支票", required = true)
    @NotNull(message = "付款方式不能为空")
    private String paymentMethod;

    @Schema(description = "付款银行账户ID（银行转账/支票时必填）", required = true)
    @NotNull(message = "付款银行账户不能为空")
    private Long bankAccountId;

    @Schema(description = "付款日期", required = true)
    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public Long getPayableId() {
        return payableId;
    }

    public void setPayableId(Long payableId) {
        this.payableId = payableId;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
