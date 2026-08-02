package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 手动发票VO
 *
 * <p>包含 ElectronicVoucher 的展示字段，以及关联 ElectronicInvoice 的关键字段。</p>
 */
@Schema(description = "手动发票信息")
public class ManualInvoiceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "凭证ID")
    private Long id;

    @Schema(description = "凭证编号（发票号码）")
    private String voucherNo;

    @Schema(description = "凭证类型")
    private String voucherType;

    @Schema(description = "发票类型")
    private String invoiceType;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开票日期")
    private LocalDate issueDate;

    @Schema(description = "价税合计")
    private BigDecimal totalAmount;

    @Schema(description = "不含税金额")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "购买方名称")
    private String buyerName;

    @Schema(description = "销售方名称")
    private String sellerName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：0-待处理，1-已入账，2-已归档")
    private Integer status;

    @Schema(description = "验真状态：0-未验真，1-验真通过，2-验真失败")
    private Integer verifyStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "验真时间")
    private LocalDateTime verifyTime;

    @Schema(description = "验真消息")
    private String verifyMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getAmountWithoutTax() { return amountWithoutTax; }
    public void setAmountWithoutTax(BigDecimal amountWithoutTax) { this.amountWithoutTax = amountWithoutTax; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(Integer verifyStatus) { this.verifyStatus = verifyStatus; }
    public LocalDateTime getVerifyTime() { return verifyTime; }
    public void setVerifyTime(LocalDateTime verifyTime) { this.verifyTime = verifyTime; }
    public String getVerifyMessage() { return verifyMessage; }
    public void setVerifyMessage(String verifyMessage) { this.verifyMessage = verifyMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
