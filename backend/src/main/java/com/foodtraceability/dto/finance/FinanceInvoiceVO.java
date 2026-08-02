package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票VO
 */
@Schema(description = "发票信息")
public class FinanceInvoiceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "发票ID")
    private Long invoiceId;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票类型: 1专票 2普票 3电子 4其他")
    private Integer invoiceType;

    @Schema(description = "发票类别: 1进项 2销项")
    private Integer invoiceCategory;

    @Schema(description = "购买方名称")
    private String buyerName;

    @Schema(description = "购买方税号")
    private String buyerTaxNo;

    @Schema(description = "销售方名称")
    private String sellerName;

    @Schema(description = "销售方税号")
    private String sellerTaxNo;

    @Schema(description = "不含税金额（分）")
    private Long totalAmount;

    @Schema(description = "税额（分）")
    private Long taxAmount;

    @Schema(description = "价税合计（分）")
    private Long totalAmountWithTax;

    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "收到日期")
    private LocalDate receiveDate;

    @Schema(description = "发票状态: 0待认证 1已认证 2已抵扣 3异常 4已红冲")
    private Integer invoiceStatus;

    @Schema(description = "验证结果")
    private String verificationResult;

    @Schema(description = "发票扫描件路径")
    private String imageUrl;

    @Schema(description = "关联的收支记录ID")
    private Long relatedRecordId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public Integer getInvoiceType() { return invoiceType; }
    public void setInvoiceType(Integer invoiceType) { this.invoiceType = invoiceType; }
    public Integer getInvoiceCategory() { return invoiceCategory; }
    public void setInvoiceCategory(Integer invoiceCategory) { this.invoiceCategory = invoiceCategory; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerTaxNo() { return buyerTaxNo; }
    public void setBuyerTaxNo(String buyerTaxNo) { this.buyerTaxNo = buyerTaxNo; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSellerTaxNo() { return sellerTaxNo; }
    public void setSellerTaxNo(String sellerTaxNo) { this.sellerTaxNo = sellerTaxNo; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public Long getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Long taxAmount) { this.taxAmount = taxAmount; }
    public Long getTotalAmountWithTax() { return totalAmountWithTax; }
    public void setTotalAmountWithTax(Long totalAmountWithTax) { this.totalAmountWithTax = totalAmountWithTax; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getReceiveDate() { return receiveDate; }
    public void setReceiveDate(LocalDate receiveDate) { this.receiveDate = receiveDate; }
    public Integer getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(Integer invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public String getVerificationResult() { return verificationResult; }
    public void setVerificationResult(String verificationResult) { this.verificationResult = verificationResult; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getRelatedRecordId() { return relatedRecordId; }
    public void setRelatedRecordId(Long relatedRecordId) { this.relatedRecordId = relatedRecordId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
