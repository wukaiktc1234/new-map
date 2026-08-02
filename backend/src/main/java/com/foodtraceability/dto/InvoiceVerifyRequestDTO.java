package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发票验真请求DTO
 */
public class InvoiceVerifyRequestDTO {
    private String invoiceCode;
    private String invoiceNo;
    private String issueDate;
    private String verifyValue;
    private String invoiceType;
    private String buyerTaxNo;
    private String sellerTaxNo;
    private BigDecimal amountWithoutTax;
    private BigDecimal totalAmount;
    private String checkCode;


    public static class InvoiceVerifyRequestDTOBuilder {
        private String invoiceCode;
        private String invoiceNo;
        private String issueDate;
        private String verifyValue;
        private String invoiceType;
        private String buyerTaxNo;
        private String sellerTaxNo;
        private BigDecimal amountWithoutTax;
        private BigDecimal totalAmount;
        private String checkCode;

        InvoiceVerifyRequestDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder invoiceCode(final String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder invoiceNo(final String invoiceNo) {
            this.invoiceNo = invoiceNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder issueDate(final String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder verifyValue(final String verifyValue) {
            this.verifyValue = verifyValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder invoiceType(final String invoiceType) {
            this.invoiceType = invoiceType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder buyerTaxNo(final String buyerTaxNo) {
            this.buyerTaxNo = buyerTaxNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder sellerTaxNo(final String sellerTaxNo) {
            this.sellerTaxNo = sellerTaxNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder amountWithoutTax(final BigDecimal amountWithoutTax) {
            this.amountWithoutTax = amountWithoutTax;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder totalAmount(final BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder checkCode(final String checkCode) {
            this.checkCode = checkCode;
            return this;
        }

        public InvoiceVerifyRequestDTO build() {
            return new InvoiceVerifyRequestDTO(this.invoiceCode, this.invoiceNo, this.issueDate, this.verifyValue, this.invoiceType, this.buyerTaxNo, this.sellerTaxNo, this.amountWithoutTax, this.totalAmount, this.checkCode);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder(invoiceCode=" + this.invoiceCode + ", invoiceNo=" + this.invoiceNo + ", issueDate=" + this.issueDate + ", verifyValue=" + this.verifyValue + ", invoiceType=" + this.invoiceType + ", buyerTaxNo=" + this.buyerTaxNo + ", sellerTaxNo=" + this.sellerTaxNo + ", amountWithoutTax=" + this.amountWithoutTax + ", totalAmount=" + this.totalAmount + ", checkCode=" + this.checkCode + ")";
        }
    }

    public static InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder builder() {
        return new InvoiceVerifyRequestDTO.InvoiceVerifyRequestDTOBuilder();
    }

    public String getInvoiceCode() {
        return this.invoiceCode;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public String getIssueDate() {
        return this.issueDate;
    }

    public String getVerifyValue() {
        return this.verifyValue;
    }

    public String getInvoiceType() {
        return this.invoiceType;
    }

    public String getBuyerTaxNo() {
        return this.buyerTaxNo;
    }

    public String getSellerTaxNo() {
        return this.sellerTaxNo;
    }

    public BigDecimal getAmountWithoutTax() {
        return this.amountWithoutTax;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getCheckCode() {
        return this.checkCode;
    }

    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setIssueDate(final String issueDate) {
        this.issueDate = issueDate;
    }

    public void setVerifyValue(final String verifyValue) {
        this.verifyValue = verifyValue;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public void setBuyerTaxNo(final String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    public void setSellerTaxNo(final String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    public void setAmountWithoutTax(final BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvoiceVerifyRequestDTO)) return false;
        final InvoiceVerifyRequestDTO other = (InvoiceVerifyRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$invoiceCode = this.getInvoiceCode();
        final java.lang.Object other$invoiceCode = other.getInvoiceCode();
        if (this$invoiceCode == null ? other$invoiceCode != null : !this$invoiceCode.equals(other$invoiceCode)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$verifyValue = this.getVerifyValue();
        final java.lang.Object other$verifyValue = other.getVerifyValue();
        if (this$verifyValue == null ? other$verifyValue != null : !this$verifyValue.equals(other$verifyValue)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$buyerTaxNo = this.getBuyerTaxNo();
        final java.lang.Object other$buyerTaxNo = other.getBuyerTaxNo();
        if (this$buyerTaxNo == null ? other$buyerTaxNo != null : !this$buyerTaxNo.equals(other$buyerTaxNo)) return false;
        final java.lang.Object this$sellerTaxNo = this.getSellerTaxNo();
        final java.lang.Object other$sellerTaxNo = other.getSellerTaxNo();
        if (this$sellerTaxNo == null ? other$sellerTaxNo != null : !this$sellerTaxNo.equals(other$sellerTaxNo)) return false;
        final java.lang.Object this$amountWithoutTax = this.getAmountWithoutTax();
        final java.lang.Object other$amountWithoutTax = other.getAmountWithoutTax();
        if (this$amountWithoutTax == null ? other$amountWithoutTax != null : !this$amountWithoutTax.equals(other$amountWithoutTax)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvoiceVerifyRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $verifyValue = this.getVerifyValue();
        result = result * PRIME + ($verifyValue == null ? 43 : $verifyValue.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $sellerTaxNo = this.getSellerTaxNo();
        result = result * PRIME + ($sellerTaxNo == null ? 43 : $sellerTaxNo.hashCode());
        final java.lang.Object $amountWithoutTax = this.getAmountWithoutTax();
        result = result * PRIME + ($amountWithoutTax == null ? 43 : $amountWithoutTax.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvoiceVerifyRequestDTO(invoiceCode=" + this.getInvoiceCode() + ", invoiceNo=" + this.getInvoiceNo() + ", issueDate=" + this.getIssueDate() + ", verifyValue=" + this.getVerifyValue() + ", invoiceType=" + this.getInvoiceType() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", totalAmount=" + this.getTotalAmount() + ", checkCode=" + this.getCheckCode() + ")";
    }

    public InvoiceVerifyRequestDTO() {
    }

    public InvoiceVerifyRequestDTO(final String invoiceCode, final String invoiceNo, final String issueDate, final String verifyValue, final String invoiceType, final String buyerTaxNo, final String sellerTaxNo, final BigDecimal amountWithoutTax, final BigDecimal totalAmount, final String checkCode) {
        this.invoiceCode = invoiceCode;
        this.invoiceNo = invoiceNo;
        this.issueDate = issueDate;
        this.verifyValue = verifyValue;
        this.invoiceType = invoiceType;
        this.buyerTaxNo = buyerTaxNo;
        this.sellerTaxNo = sellerTaxNo;
        this.amountWithoutTax = amountWithoutTax;
        this.totalAmount = totalAmount;
        this.checkCode = checkCode;
    }
}
