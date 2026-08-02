package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 税务记录DTO
 * @author example
 * @since 2025-12-05
 */
public class TaxRecordDTO {
    /**
     * 税种类型：VAT(增值税)、ENTERPRISE_INCOME_TAX(企业所得税)、PERSONAL_INCOME_TAX(个人所得税)、OTHER(其他)
     */
    private String taxType;
    /**
     * 纳税期间：如202511(2025年11月)
     */
    private String taxPeriod;
    /**
     * 应纳税所得额
     */
    private BigDecimal taxableAmount;
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 应纳税额
     */
    private BigDecimal taxAmount;
    /**
     * 已纳税额
     */
    private BigDecimal paidAmount;
    /**
     * 纳税状态：UNPAID(未缴纳)、PAID(已缴纳)、OVERDUE(逾期)
     */
    private String taxStatus;
    /**
     * 缴纳日期
     */
    private LocalDateTime paymentDate;
    /**
     * 描述
     */
    private String description;

    public String getTaxStatus() {
        return this.taxStatus;
    }

    public String getTaxPeriod() {
        return this.taxPeriod;
    }

    public String getTaxType() {
        return this.taxType;
    }

    public TaxRecordDTO() {
    }

    /**
     * 应纳税所得额
     */
    public BigDecimal getTaxableAmount() {
        return this.taxableAmount;
    }

    /**
     * 税率
     */
    public BigDecimal getTaxRate() {
        return this.taxRate;
    }

    /**
     * 应纳税额
     */
    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    /**
     * 已纳税额
     */
    public BigDecimal getPaidAmount() {
        return this.paidAmount;
    }

    /**
     * 缴纳日期
     */
    public LocalDateTime getPaymentDate() {
        return this.paymentDate;
    }

    /**
     * 描述
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 税种类型：VAT(增值税)、ENTERPRISE_INCOME_TAX(企业所得税)、PERSONAL_INCOME_TAX(个人所得税)、OTHER(其他)
     */
    public void setTaxType(final String taxType) {
        this.taxType = taxType;
    }

    /**
     * 纳税期间：如202511(2025年11月)
     */
    public void setTaxPeriod(final String taxPeriod) {
        this.taxPeriod = taxPeriod;
    }

    /**
     * 应纳税所得额
     */
    public void setTaxableAmount(final BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    /**
     * 税率
     */
    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * 应纳税额
     */
    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * 已纳税额
     */
    public void setPaidAmount(final BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    /**
     * 纳税状态：UNPAID(未缴纳)、PAID(已缴纳)、OVERDUE(逾期)
     */
    public void setTaxStatus(final String taxStatus) {
        this.taxStatus = taxStatus;
    }

    /**
     * 缴纳日期
     */
    public void setPaymentDate(final LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    /**
     * 描述
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TaxRecordDTO)) return false;
        final TaxRecordDTO other = (TaxRecordDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$taxType = this.getTaxType();
        final java.lang.Object other$taxType = other.getTaxType();
        if (this$taxType == null ? other$taxType != null : !this$taxType.equals(other$taxType)) return false;
        final java.lang.Object this$taxPeriod = this.getTaxPeriod();
        final java.lang.Object other$taxPeriod = other.getTaxPeriod();
        if (this$taxPeriod == null ? other$taxPeriod != null : !this$taxPeriod.equals(other$taxPeriod)) return false;
        final java.lang.Object this$taxableAmount = this.getTaxableAmount();
        final java.lang.Object other$taxableAmount = other.getTaxableAmount();
        if (this$taxableAmount == null ? other$taxableAmount != null : !this$taxableAmount.equals(other$taxableAmount)) return false;
        final java.lang.Object this$taxRate = this.getTaxRate();
        final java.lang.Object other$taxRate = other.getTaxRate();
        if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$paidAmount = this.getPaidAmount();
        final java.lang.Object other$paidAmount = other.getPaidAmount();
        if (this$paidAmount == null ? other$paidAmount != null : !this$paidAmount.equals(other$paidAmount)) return false;
        final java.lang.Object this$taxStatus = this.getTaxStatus();
        final java.lang.Object other$taxStatus = other.getTaxStatus();
        if (this$taxStatus == null ? other$taxStatus != null : !this$taxStatus.equals(other$taxStatus)) return false;
        final java.lang.Object this$paymentDate = this.getPaymentDate();
        final java.lang.Object other$paymentDate = other.getPaymentDate();
        if (this$paymentDate == null ? other$paymentDate != null : !this$paymentDate.equals(other$paymentDate)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TaxRecordDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $taxType = this.getTaxType();
        result = result * PRIME + ($taxType == null ? 43 : $taxType.hashCode());
        final java.lang.Object $taxPeriod = this.getTaxPeriod();
        result = result * PRIME + ($taxPeriod == null ? 43 : $taxPeriod.hashCode());
        final java.lang.Object $taxableAmount = this.getTaxableAmount();
        result = result * PRIME + ($taxableAmount == null ? 43 : $taxableAmount.hashCode());
        final java.lang.Object $taxRate = this.getTaxRate();
        result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $paidAmount = this.getPaidAmount();
        result = result * PRIME + ($paidAmount == null ? 43 : $paidAmount.hashCode());
        final java.lang.Object $taxStatus = this.getTaxStatus();
        result = result * PRIME + ($taxStatus == null ? 43 : $taxStatus.hashCode());
        final java.lang.Object $paymentDate = this.getPaymentDate();
        result = result * PRIME + ($paymentDate == null ? 43 : $paymentDate.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TaxRecordDTO(taxType=" + this.getTaxType() + ", taxPeriod=" + this.getTaxPeriod() + ", taxableAmount=" + this.getTaxableAmount() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", paidAmount=" + this.getPaidAmount() + ", taxStatus=" + this.getTaxStatus() + ", paymentDate=" + this.getPaymentDate() + ", description=" + this.getDescription() + ")";
    }
}
