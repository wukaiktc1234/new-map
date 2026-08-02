package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发票实体类
 * 用于存储健康证报销相关的发票信息
 */
@TableName("invoice")
public class Invoice {
    /**
     * 发票ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    /**
     * 健康证ID，外键关联健康证表
     */
    private String healthCertificateId;
    /**
     * 发票号码
     */
    private String invoiceNumber;
    /**
     * 发票金额
     */
    private BigDecimal invoiceAmount;
    /**
     * 发票日期
     */
    private LocalDate invoiceDate;
    /**
     * 发票类型
     * 增值税普通发票
     * 增值税专用发票
     * 其他发票
     */
    private String invoiceType;
    /**
     * 发票图片路径
     */
    private String invoiceImage;
    /**
     * 发票审核状态
     * pending: 待审核
     * approved: 已通过
     * rejected: 已拒绝
     */
    private String approvalStatus;
    /**
     * 审核人
     */
    private String approvedBy;
    /**
     * 审核日期
     */
    private LocalDate approvedDate;
    /**
     * 拒绝原因
     */
    private String rejectReason;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 操作时间
     */
    private LocalDate operationTime;

    public void setId(String id) {
        this.id = id;
    }

    public void setHealthCertificateId(String healthCertificateId) {
        this.healthCertificateId = healthCertificateId;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setInvoiceAmount(BigDecimal invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public void setInvoiceImage(String invoiceImage) {
        this.invoiceImage = invoiceImage;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setApprovedDate(LocalDate approvedDate) {
        this.approvedDate = approvedDate;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setOperationTime(LocalDate operationTime) {
        this.operationTime = operationTime;
    }

    public String getId() {
        return this.id;
    }

    public String getHealthCertificateId() {
        return this.healthCertificateId;
    }

    public String getInvoiceNumber() {
        return this.invoiceNumber;
    }

    public BigDecimal getInvoiceAmount() {
        return this.invoiceAmount;
    }

    public LocalDate getInvoiceDate() {
        return this.invoiceDate;
    }

    public String getInvoiceType() {
        return this.invoiceType;
    }

    public String getInvoiceImage() {
        return this.invoiceImage;
    }

    public String getApprovalStatus() {
        return this.approvalStatus;
    }

    public String getApprovedBy() {
        return this.approvedBy;
    }

    public LocalDate getApprovedDate() {
        return this.approvedDate;
    }

    public String getRejectReason() {
        return this.rejectReason;
    }

    public String getOperator() {
        return this.operator;
    }

    public LocalDate getOperationTime() {
        return this.operationTime;
    }

    public Invoice() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Invoice)) return false;
        final Invoice other = (Invoice) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$healthCertificateId = this.getHealthCertificateId();
        final java.lang.Object other$healthCertificateId = other.getHealthCertificateId();
        if (this$healthCertificateId == null ? other$healthCertificateId != null : !this$healthCertificateId.equals(other$healthCertificateId)) return false;
        final java.lang.Object this$invoiceNumber = this.getInvoiceNumber();
        final java.lang.Object other$invoiceNumber = other.getInvoiceNumber();
        if (this$invoiceNumber == null ? other$invoiceNumber != null : !this$invoiceNumber.equals(other$invoiceNumber)) return false;
        final java.lang.Object this$invoiceAmount = this.getInvoiceAmount();
        final java.lang.Object other$invoiceAmount = other.getInvoiceAmount();
        if (this$invoiceAmount == null ? other$invoiceAmount != null : !this$invoiceAmount.equals(other$invoiceAmount)) return false;
        final java.lang.Object this$invoiceDate = this.getInvoiceDate();
        final java.lang.Object other$invoiceDate = other.getInvoiceDate();
        if (this$invoiceDate == null ? other$invoiceDate != null : !this$invoiceDate.equals(other$invoiceDate)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$invoiceImage = this.getInvoiceImage();
        final java.lang.Object other$invoiceImage = other.getInvoiceImage();
        if (this$invoiceImage == null ? other$invoiceImage != null : !this$invoiceImage.equals(other$invoiceImage)) return false;
        final java.lang.Object this$approvalStatus = this.getApprovalStatus();
        final java.lang.Object other$approvalStatus = other.getApprovalStatus();
        if (this$approvalStatus == null ? other$approvalStatus != null : !this$approvalStatus.equals(other$approvalStatus)) return false;
        final java.lang.Object this$approvedBy = this.getApprovedBy();
        final java.lang.Object other$approvedBy = other.getApprovedBy();
        if (this$approvedBy == null ? other$approvedBy != null : !this$approvedBy.equals(other$approvedBy)) return false;
        final java.lang.Object this$approvedDate = this.getApprovedDate();
        final java.lang.Object other$approvedDate = other.getApprovedDate();
        if (this$approvedDate == null ? other$approvedDate != null : !this$approvedDate.equals(other$approvedDate)) return false;
        final java.lang.Object this$rejectReason = this.getRejectReason();
        final java.lang.Object other$rejectReason = other.getRejectReason();
        if (this$rejectReason == null ? other$rejectReason != null : !this$rejectReason.equals(other$rejectReason)) return false;
        final java.lang.Object this$operator = this.getOperator();
        final java.lang.Object other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) return false;
        final java.lang.Object this$operationTime = this.getOperationTime();
        final java.lang.Object other$operationTime = other.getOperationTime();
        if (this$operationTime == null ? other$operationTime != null : !this$operationTime.equals(other$operationTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Invoice;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $healthCertificateId = this.getHealthCertificateId();
        result = result * PRIME + ($healthCertificateId == null ? 43 : $healthCertificateId.hashCode());
        final java.lang.Object $invoiceNumber = this.getInvoiceNumber();
        result = result * PRIME + ($invoiceNumber == null ? 43 : $invoiceNumber.hashCode());
        final java.lang.Object $invoiceAmount = this.getInvoiceAmount();
        result = result * PRIME + ($invoiceAmount == null ? 43 : $invoiceAmount.hashCode());
        final java.lang.Object $invoiceDate = this.getInvoiceDate();
        result = result * PRIME + ($invoiceDate == null ? 43 : $invoiceDate.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $invoiceImage = this.getInvoiceImage();
        result = result * PRIME + ($invoiceImage == null ? 43 : $invoiceImage.hashCode());
        final java.lang.Object $approvalStatus = this.getApprovalStatus();
        result = result * PRIME + ($approvalStatus == null ? 43 : $approvalStatus.hashCode());
        final java.lang.Object $approvedBy = this.getApprovedBy();
        result = result * PRIME + ($approvedBy == null ? 43 : $approvedBy.hashCode());
        final java.lang.Object $approvedDate = this.getApprovedDate();
        result = result * PRIME + ($approvedDate == null ? 43 : $approvedDate.hashCode());
        final java.lang.Object $rejectReason = this.getRejectReason();
        result = result * PRIME + ($rejectReason == null ? 43 : $rejectReason.hashCode());
        final java.lang.Object $operator = this.getOperator();
        result = result * PRIME + ($operator == null ? 43 : $operator.hashCode());
        final java.lang.Object $operationTime = this.getOperationTime();
        result = result * PRIME + ($operationTime == null ? 43 : $operationTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Invoice(id=" + this.getId() + ", healthCertificateId=" + this.getHealthCertificateId() + ", invoiceNumber=" + this.getInvoiceNumber() + ", invoiceAmount=" + this.getInvoiceAmount() + ", invoiceDate=" + this.getInvoiceDate() + ", invoiceType=" + this.getInvoiceType() + ", invoiceImage=" + this.getInvoiceImage() + ", approvalStatus=" + this.getApprovalStatus() + ", approvedBy=" + this.getApprovedBy() + ", approvedDate=" + this.getApprovedDate() + ", rejectReason=" + this.getRejectReason() + ", operator=" + this.getOperator() + ", operationTime=" + this.getOperationTime() + ")";
    }
}
