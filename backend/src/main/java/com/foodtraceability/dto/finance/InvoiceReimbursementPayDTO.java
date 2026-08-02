package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 发票报销付款DTO
 *
 * <p>Sprint 3.1 P0 F-001：用于标记报销单已付款，
 * 含 paymentVoucherNo（付款凭证号）+ paymentDate（付款日期）。</p>
 */
public class InvoiceReimbursementPayDTO {

    /** 付款凭证号（必填） */
    @NotBlank(message = "付款凭证号不能为空")
    @Size(max = 32, message = "付款凭证号长度不能超过32个字符")
    private String paymentVoucherNo;

    /** 付款日期（必填） */
    @NotNull(message = "付款日期不能为空")
    private LocalDate paymentDate;

    public InvoiceReimbursementPayDTO() {
    }

    public String getPaymentVoucherNo() {
        return paymentVoucherNo;
    }

    public void setPaymentVoucherNo(String paymentVoucherNo) {
        this.paymentVoucherNo = paymentVoucherNo;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}
