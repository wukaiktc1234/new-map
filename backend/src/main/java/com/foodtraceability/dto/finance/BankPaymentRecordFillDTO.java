package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 银行打款记录回填请求DTO
 */
@Schema(description = "银行打款记录回填请求")
public class BankPaymentRecordFillDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "银行流水号")
    @NotBlank(message = "银行流水号不能为空")
    private String bankTransactionNo;

    @Schema(description = "银行交易时间")
    @NotNull(message = "银行交易时间不能为空")
    private LocalDateTime bankTransactionTime;

    @Schema(description = "凭证类型：receipt-银行回单 invoice-发票 other-其他")
    private String voucherType;

    @Schema(description = "凭证文件URL")
    private String voucherUrl;

    public String getBankTransactionNo() {
        return bankTransactionNo;
    }

    public void setBankTransactionNo(String bankTransactionNo) {
        this.bankTransactionNo = bankTransactionNo;
    }

    public LocalDateTime getBankTransactionTime() {
        return bankTransactionTime;
    }

    public void setBankTransactionTime(LocalDateTime bankTransactionTime) {
        this.bankTransactionTime = bankTransactionTime;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getVoucherUrl() {
        return voucherUrl;
    }

    public void setVoucherUrl(String voucherUrl) {
        this.voucherUrl = voucherUrl;
    }
}
