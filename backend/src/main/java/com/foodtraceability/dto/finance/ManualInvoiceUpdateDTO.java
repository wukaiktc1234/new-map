package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 手动发票更新DTO
 */
@Schema(description = "手动发票更新信息")
public class ManualInvoiceUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 20, message = "发票代码长度不能超过20字符")
    @Schema(description = "发票代码")
    private String invoiceCode;

    @Size(max = 20, message = "发票号码长度不能超过20字符")
    @Schema(description = "发票号码")
    private String invoiceNo;

    @Positive(message = "总金额必须大于0")
    @Schema(description = "价税合计（分）")
    private Long totalAmount;

    @PositiveOrZero(message = "不含税金额不能为负数")
    @Schema(description = "不含税金额（分）")
    private Long amountWithoutTax;

    @PositiveOrZero(message = "税额不能为负数")
    @Schema(description = "税额（分）")
    private Long taxAmount;

    @Size(max = 500, message = "备注长度不能超过500字符")
    @Schema(description = "备注")
    private String remark;

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public Long getAmountWithoutTax() { return amountWithoutTax; }
    public void setAmountWithoutTax(Long amountWithoutTax) { this.amountWithoutTax = amountWithoutTax; }
    public Long getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Long taxAmount) { this.taxAmount = taxAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
