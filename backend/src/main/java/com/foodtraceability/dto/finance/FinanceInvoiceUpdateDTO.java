package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 发票更新DTO
 *
 * <p>Sprint 3.1 P0 F-003/F-004：用于更新发票信息，仅 draft 状态的发票可更新。</p>
 *
 * <p>金额字段为 Long（分），无 BigDecimal。</p>
 */
@Schema(description = "发票更新请求")
public class FinanceInvoiceUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 发票ID（必填，由路径参数传入） */
    @NotNull(message = "发票ID不能为空")
    @Schema(description = "发票ID")
    private Long invoiceId;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Size(max = 30, message = "发票号码长度不能超过30个字符")
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

    @Schema(description = "开票日期（yyyy-MM-dd）")
    private String invoiceDate;

    @Schema(description = "收到日期（yyyy-MM-dd）")
    private String receiveDate;

    @Schema(description = "发票扫描件路径")
    private String imageUrl;

    @Schema(description = "关联的收支记录ID")
    private Long relatedRecordId;

    public FinanceInvoiceUpdateDTO() {
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Integer getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(Integer invoiceType) {
        this.invoiceType = invoiceType;
    }

    public Integer getInvoiceCategory() {
        return invoiceCategory;
    }

    public void setInvoiceCategory(Integer invoiceCategory) {
        this.invoiceCategory = invoiceCategory;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerTaxNo() {
        return buyerTaxNo;
    }

    public void setBuyerTaxNo(String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerTaxNo() {
        return sellerTaxNo;
    }

    public void setSellerTaxNo(String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Long taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Long getTotalAmountWithTax() {
        return totalAmountWithTax;
    }

    public void setTotalAmountWithTax(Long totalAmountWithTax) {
        this.totalAmountWithTax = totalAmountWithTax;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(String receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getRelatedRecordId() {
        return relatedRecordId;
    }

    public void setRelatedRecordId(Long relatedRecordId) {
        this.relatedRecordId = relatedRecordId;
    }
}
