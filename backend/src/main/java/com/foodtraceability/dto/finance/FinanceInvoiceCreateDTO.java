package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 发票创建DTO
 */
@Schema(description = "发票创建请求")
public class FinanceInvoiceCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "发票代码")
    @Size(max = 32, message = "发票代码长度不能超过32个字符")
    private String invoiceCode;

    @NotBlank(message = "发票号码不能为空")
    @Size(max = 32, message = "发票号码长度不能超过32个字符")
    @Schema(description = "发票号码")
    private String invoiceNo;

    @NotNull(message = "发票类型不能为空")
    @Min(value = 1, message = "发票类型无效")
    @Max(value = 4, message = "发票类型无效")
    @Schema(description = "发票类型: 1专票 2普票 3电子 4其他")
    private Integer invoiceType;

    @NotNull(message = "发票类别不能为空")
    @Min(value = 1, message = "发票类别无效")
    @Max(value = 2, message = "发票类别无效")
    @Schema(description = "发票类别: 1进项 2销项")
    private Integer invoiceCategory;

    @Schema(description = "购买方名称")
    @Size(max = 200, message = "购买方名称长度不能超过200个字符")
    private String buyerName;

    @Schema(description = "购买方税号")
    @Size(max = 50, message = "购买方税号长度不能超过50个字符")
    private String buyerTaxNo;

    @Schema(description = "销售方名称")
    @Size(max = 200, message = "销售方名称长度不能超过200个字符")
    private String sellerName;

    @Schema(description = "销售方税号")
    @Size(max = 50, message = "销售方税号长度不能超过50个字符")
    private String sellerTaxNo;

    @Schema(description = "不含税金额（分）")
    @Min(value = 0, message = "不含税金额不能为负数")
    private Long totalAmount;

    @Schema(description = "税额（分）")
    @Min(value = 0, message = "税额不能为负数")
    private Long taxAmount;

    @Schema(description = "价税合计（分）")
    @Min(value = 0, message = "价税合计不能为负数")
    private Long totalAmountWithTax;

    @Schema(description = "开票日期")
    @Size(max = 20, message = "开票日期长度不能超过20个字符")
    private String invoiceDate;

    @Schema(description = "收到日期")
    @Size(max = 20, message = "收到日期长度不能超过20个字符")
    private String receiveDate;

    @Schema(description = "发票扫描件路径")
    @Size(max = 500, message = "发票扫描件路径长度不能超过500个字符")
    private String imageUrl;

    @Schema(description = "关联的收支记录ID")
    private Long relatedRecordId;

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
    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
    public String getReceiveDate() { return receiveDate; }
    public void setReceiveDate(String receiveDate) { this.receiveDate = receiveDate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getRelatedRecordId() { return relatedRecordId; }
    public void setRelatedRecordId(Long relatedRecordId) { this.relatedRecordId = relatedRecordId; }
}
