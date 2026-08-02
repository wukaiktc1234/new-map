package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票管理实体类
 * 管理进项发票和销项发票，支持OCR识别和认证抵扣
 */
@TableName("finance_invoices")
public class FinanceInvoice implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发票ID */
    @TableId(value = "invoice_id", type = IdType.AUTO)
    private Long invoiceId;

    /** 发票代码 */
    @TableField("invoice_code")
    private String invoiceCode;

    /** 发票号码 */
    @TableField("invoice_no")
    private String invoiceNo;

    /**
     * 发票类型
     * 1-增值税专票 2-增值税普票 3-电子发票 4-其他
     */
    @TableField("invoice_type")
    private Integer invoiceType;

    /**
     * 发票类别
     * 1-进项发票 2-销项发票
     */
    @TableField("invoice_category")
    private Integer invoiceCategory;

    /** 购买方名称 */
    @TableField("buyer_name")
    private String buyerName;

    /** 购买方税号 */
    @TableField("buyer_tax_no")
    private String buyerTaxNo;

    /** 销售方名称 */
    @TableField("seller_name")
    private String sellerName;

    /** 销售方税号 */
    @TableField("seller_tax_no")
    private String sellerTaxNo;

    /** 不含税金额（单位：分） */
    @TableField("total_amount")
    private Long totalAmount;

    /** 税额（单位：分） */
    @TableField("tax_amount")
    private Long taxAmount;

    /** 价税合计（单位：分） */
    @TableField("total_amount_with_tax")
    private Long totalAmountWithTax;

    /** 开票日期 */
    @TableField("invoice_date")
    private LocalDate invoiceDate;

    /** 收到日期 */
    @TableField("receive_date")
    private LocalDate receiveDate;

    /**
     * 发票状态
     * 0-待认证 1-已认证 2-已抵扣 3-异常 4-已红冲
     */
    @TableField("invoice_status")
    private Integer invoiceStatus;

    /** 验证结果 */
    @TableField("verification_result")
    private String verificationResult;

    /** 发票扫描件/电子件路径 */
    @TableField("image_url")
    private String imageUrl;

    /** OCR识别结果（JSON格式） */
    @TableField("ocr_result")
    private String ocrResult;

    /** 关联的收支记录ID */
    @TableField("related_record_id")
    private Long relatedRecordId;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== Getter & Setter ==========

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

    public String getOcrResult() { return ocrResult; }
    public void setOcrResult(String ocrResult) { this.ocrResult = ocrResult; }

    public Long getRelatedRecordId() { return relatedRecordId; }
    public void setRelatedRecordId(Long relatedRecordId) { this.relatedRecordId = relatedRecordId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
