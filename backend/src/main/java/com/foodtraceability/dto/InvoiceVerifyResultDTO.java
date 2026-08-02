package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票验真结果DTO
 */
public class InvoiceVerifyResultDTO {
    private boolean success;
    private String message;
    private String verifyCode;
    private String invoiceCode;
    private String invoiceNo;
    private String invoiceType;
    private String invoiceTypeName;
    private String issueDate;
    private String buyerName;
    private String buyerTaxNo;
    private String buyerAddressPhone;
    private String buyerBankAccount;
    private String sellerName;
    private String sellerTaxNo;
    private String sellerAddressPhone;
    private String sellerBankAccount;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String checkCode;
    private String machineCode;
    private String remark;
    private String payee;
    private String checker;
    private String invalidFlag;
    private Integer invoiceStatus;
    private String invoiceStatusDescription;
    private int verifyCount;
    private LocalDateTime verifyTime;
    private Integer status;
    private String errorMessage;
    private String statusDescription;
    private String pdfUrl;
    private String ofdUrl;
    private List<InvoiceItemDTO> items;
    private String rawResponse;
    private String verifySource;
    private boolean taxPlatformVerified;

    public static InvoiceVerifyResultDTO success() {
        InvoiceVerifyResultDTO result = new InvoiceVerifyResultDTO();
        result.setSuccess(true);
        result.setMessage("发票验真成功");
        result.setStatus(1);
        result.setStatusDescription("验真成功");
        result.setVerifyTime(LocalDateTime.now());
        return result;
    }

    public static InvoiceVerifyResultDTO failed(String message) {
        InvoiceVerifyResultDTO result = new InvoiceVerifyResultDTO();
        result.setSuccess(false);
        result.setMessage(message);
        result.setErrorMessage(message);
        result.setStatus(2);
        result.setStatusDescription("验真失败");
        result.setVerifyTime(LocalDateTime.now());
        return result;
    }


    public static class InvoiceItemDTO {
        private String productName;
        private String specification;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private String taxCode;


        public static class InvoiceItemDTOBuilder {
            private String productName;
            private String specification;
            private String unit;
            private BigDecimal quantity;
            private BigDecimal unitPrice;
            private BigDecimal amount;
            private BigDecimal taxRate;
            private BigDecimal taxAmount;
            private String taxCode;

            InvoiceItemDTOBuilder() {
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder productName(final String productName) {
                this.productName = productName;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder specification(final String specification) {
                this.specification = specification;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder unit(final String unit) {
                this.unit = unit;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder quantity(final BigDecimal quantity) {
                this.quantity = quantity;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder unitPrice(final BigDecimal unitPrice) {
                this.unitPrice = unitPrice;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder amount(final BigDecimal amount) {
                this.amount = amount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder taxRate(final BigDecimal taxRate) {
                this.taxRate = taxRate;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder taxAmount(final BigDecimal taxAmount) {
                this.taxAmount = taxAmount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder taxCode(final String taxCode) {
                this.taxCode = taxCode;
                return this;
            }

            public InvoiceVerifyResultDTO.InvoiceItemDTO build() {
                return new InvoiceVerifyResultDTO.InvoiceItemDTO(this.productName, this.specification, this.unit, this.quantity, this.unitPrice, this.amount, this.taxRate, this.taxAmount, this.taxCode);
            }

            @java.lang.Override
            public java.lang.String toString() {
                return "InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder(productName=" + this.productName + ", specification=" + this.specification + ", unit=" + this.unit + ", quantity=" + this.quantity + ", unitPrice=" + this.unitPrice + ", amount=" + this.amount + ", taxRate=" + this.taxRate + ", taxAmount=" + this.taxAmount + ", taxCode=" + this.taxCode + ")";
            }
        }

        public static InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder builder() {
            return new InvoiceVerifyResultDTO.InvoiceItemDTO.InvoiceItemDTOBuilder();
        }

        public String getProductName() {
            return this.productName;
        }

        public String getSpecification() {
            return this.specification;
        }

        public String getUnit() {
            return this.unit;
        }

        public BigDecimal getQuantity() {
            return this.quantity;
        }

        public BigDecimal getUnitPrice() {
            return this.unitPrice;
        }

        public BigDecimal getAmount() {
            return this.amount;
        }

        public BigDecimal getTaxRate() {
            return this.taxRate;
        }

        public BigDecimal getTaxAmount() {
            return this.taxAmount;
        }

        public String getTaxCode() {
            return this.taxCode;
        }

        public void setProductName(final String productName) {
            this.productName = productName;
        }

        public void setSpecification(final String specification) {
            this.specification = specification;
        }

        public void setUnit(final String unit) {
            this.unit = unit;
        }

        public void setQuantity(final BigDecimal quantity) {
            this.quantity = quantity;
        }

        public void setUnitPrice(final BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public void setAmount(final BigDecimal amount) {
            this.amount = amount;
        }

        public void setTaxRate(final BigDecimal taxRate) {
            this.taxRate = taxRate;
        }

        public void setTaxAmount(final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
        }

        public void setTaxCode(final String taxCode) {
            this.taxCode = taxCode;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof InvoiceVerifyResultDTO.InvoiceItemDTO)) return false;
            final InvoiceVerifyResultDTO.InvoiceItemDTO other = (InvoiceVerifyResultDTO.InvoiceItemDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$productName = this.getProductName();
            final java.lang.Object other$productName = other.getProductName();
            if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
            final java.lang.Object this$specification = this.getSpecification();
            final java.lang.Object other$specification = other.getSpecification();
            if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
            final java.lang.Object this$unit = this.getUnit();
            final java.lang.Object other$unit = other.getUnit();
            if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$unitPrice = this.getUnitPrice();
            final java.lang.Object other$unitPrice = other.getUnitPrice();
            if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
            final java.lang.Object this$amount = this.getAmount();
            final java.lang.Object other$amount = other.getAmount();
            if (this$amount == null ? other$amount != null : !this$amount.equals(other$amount)) return false;
            final java.lang.Object this$taxRate = this.getTaxRate();
            final java.lang.Object other$taxRate = other.getTaxRate();
            if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
            final java.lang.Object this$taxAmount = this.getTaxAmount();
            final java.lang.Object other$taxAmount = other.getTaxAmount();
            if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
            final java.lang.Object this$taxCode = this.getTaxCode();
            final java.lang.Object other$taxCode = other.getTaxCode();
            if (this$taxCode == null ? other$taxCode != null : !this$taxCode.equals(other$taxCode)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof InvoiceVerifyResultDTO.InvoiceItemDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $productName = this.getProductName();
            result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
            final java.lang.Object $specification = this.getSpecification();
            result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
            final java.lang.Object $unit = this.getUnit();
            result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $unitPrice = this.getUnitPrice();
            result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
            final java.lang.Object $amount = this.getAmount();
            result = result * PRIME + ($amount == null ? 43 : $amount.hashCode());
            final java.lang.Object $taxRate = this.getTaxRate();
            result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
            final java.lang.Object $taxAmount = this.getTaxAmount();
            result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
            final java.lang.Object $taxCode = this.getTaxCode();
            result = result * PRIME + ($taxCode == null ? 43 : $taxCode.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InvoiceVerifyResultDTO.InvoiceItemDTO(productName=" + this.getProductName() + ", specification=" + this.getSpecification() + ", unit=" + this.getUnit() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", amount=" + this.getAmount() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", taxCode=" + this.getTaxCode() + ")";
        }

        public InvoiceItemDTO() {
        }

        public InvoiceItemDTO(final String productName, final String specification, final String unit, final BigDecimal quantity, final BigDecimal unitPrice, final BigDecimal amount, final BigDecimal taxRate, final BigDecimal taxAmount, final String taxCode) {
            this.productName = productName;
            this.specification = specification;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.taxRate = taxRate;
            this.taxAmount = taxAmount;
            this.taxCode = taxCode;
        }
    }


    public static class InvoiceVerifyResultDTOBuilder {
        private boolean success;
        private String message;
        private String verifyCode;
        private String invoiceCode;
        private String invoiceNo;
        private String invoiceType;
        private String invoiceTypeName;
        private String issueDate;
        private String buyerName;
        private String buyerTaxNo;
        private String buyerAddressPhone;
        private String buyerBankAccount;
        private String sellerName;
        private String sellerTaxNo;
        private String sellerAddressPhone;
        private String sellerBankAccount;
        private BigDecimal amountWithoutTax;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String checkCode;
        private String machineCode;
        private String remark;
        private String payee;
        private String checker;
        private String invalidFlag;
        private Integer invoiceStatus;
        private String invoiceStatusDescription;
        private int verifyCount;
        private LocalDateTime verifyTime;
        private Integer status;
        private String errorMessage;
        private String statusDescription;
        private String pdfUrl;
        private String ofdUrl;
        private List<InvoiceItemDTO> items;
        private String rawResponse;
        private String verifySource;
        private boolean taxPlatformVerified;

        InvoiceVerifyResultDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder success(final boolean success) {
            this.success = success;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder verifyCode(final String verifyCode) {
            this.verifyCode = verifyCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invoiceCode(final String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invoiceNo(final String invoiceNo) {
            this.invoiceNo = invoiceNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invoiceType(final String invoiceType) {
            this.invoiceType = invoiceType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invoiceTypeName(final String invoiceTypeName) {
            this.invoiceTypeName = invoiceTypeName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder issueDate(final String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder buyerName(final String buyerName) {
            this.buyerName = buyerName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder buyerTaxNo(final String buyerTaxNo) {
            this.buyerTaxNo = buyerTaxNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder buyerAddressPhone(final String buyerAddressPhone) {
            this.buyerAddressPhone = buyerAddressPhone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder buyerBankAccount(final String buyerBankAccount) {
            this.buyerBankAccount = buyerBankAccount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder sellerName(final String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder sellerTaxNo(final String sellerTaxNo) {
            this.sellerTaxNo = sellerTaxNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder sellerAddressPhone(final String sellerAddressPhone) {
            this.sellerAddressPhone = sellerAddressPhone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder sellerBankAccount(final String sellerBankAccount) {
            this.sellerBankAccount = sellerBankAccount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder amountWithoutTax(final BigDecimal amountWithoutTax) {
            this.amountWithoutTax = amountWithoutTax;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder taxAmount(final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder totalAmount(final BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder checkCode(final String checkCode) {
            this.checkCode = checkCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder machineCode(final String machineCode) {
            this.machineCode = machineCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder payee(final String payee) {
            this.payee = payee;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder checker(final String checker) {
            this.checker = checker;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invalidFlag(final String invalidFlag) {
            this.invalidFlag = invalidFlag;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invoiceStatus(final Integer invoiceStatus) {
            this.invoiceStatus = invoiceStatus;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder invoiceStatusDescription(final String invoiceStatusDescription) {
            this.invoiceStatusDescription = invoiceStatusDescription;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder verifyCount(final int verifyCount) {
            this.verifyCount = verifyCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder verifyTime(final LocalDateTime verifyTime) {
            this.verifyTime = verifyTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder statusDescription(final String statusDescription) {
            this.statusDescription = statusDescription;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder pdfUrl(final String pdfUrl) {
            this.pdfUrl = pdfUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder ofdUrl(final String ofdUrl) {
            this.ofdUrl = ofdUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder items(final List<InvoiceItemDTO> items) {
            this.items = items;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder rawResponse(final String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder verifySource(final String verifySource) {
            this.verifySource = verifySource;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder taxPlatformVerified(final boolean taxPlatformVerified) {
            this.taxPlatformVerified = taxPlatformVerified;
            return this;
        }

        public InvoiceVerifyResultDTO build() {
            return new InvoiceVerifyResultDTO(this.success, this.message, this.verifyCode, this.invoiceCode, this.invoiceNo, this.invoiceType, this.invoiceTypeName, this.issueDate, this.buyerName, this.buyerTaxNo, this.buyerAddressPhone, this.buyerBankAccount, this.sellerName, this.sellerTaxNo, this.sellerAddressPhone, this.sellerBankAccount, this.amountWithoutTax, this.taxAmount, this.totalAmount, this.checkCode, this.machineCode, this.remark, this.payee, this.checker, this.invalidFlag, this.invoiceStatus, this.invoiceStatusDescription, this.verifyCount, this.verifyTime, this.status, this.errorMessage, this.statusDescription, this.pdfUrl, this.ofdUrl, this.items, this.rawResponse, this.verifySource, this.taxPlatformVerified);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder(success=" + this.success + ", message=" + this.message + ", verifyCode=" + this.verifyCode + ", invoiceCode=" + this.invoiceCode + ", invoiceNo=" + this.invoiceNo + ", invoiceType=" + this.invoiceType + ", invoiceTypeName=" + this.invoiceTypeName + ", issueDate=" + this.issueDate + ", buyerName=" + this.buyerName + ", buyerTaxNo=" + this.buyerTaxNo + ", buyerAddressPhone=" + this.buyerAddressPhone + ", buyerBankAccount=" + this.buyerBankAccount + ", sellerName=" + this.sellerName + ", sellerTaxNo=" + this.sellerTaxNo + ", sellerAddressPhone=" + this.sellerAddressPhone + ", sellerBankAccount=" + this.sellerBankAccount + ", amountWithoutTax=" + this.amountWithoutTax + ", taxAmount=" + this.taxAmount + ", totalAmount=" + this.totalAmount + ", checkCode=" + this.checkCode + ", machineCode=" + this.machineCode + ", remark=" + this.remark + ", payee=" + this.payee + ", checker=" + this.checker + ", invalidFlag=" + this.invalidFlag + ", invoiceStatus=" + this.invoiceStatus + ", invoiceStatusDescription=" + this.invoiceStatusDescription + ", verifyCount=" + this.verifyCount + ", verifyTime=" + this.verifyTime + ", status=" + this.status + ", errorMessage=" + this.errorMessage + ", statusDescription=" + this.statusDescription + ", pdfUrl=" + this.pdfUrl + ", ofdUrl=" + this.ofdUrl + ", items=" + this.items + ", rawResponse=" + this.rawResponse + ", verifySource=" + this.verifySource + ", taxPlatformVerified=" + this.taxPlatformVerified + ")";
        }
    }

    public static InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder builder() {
        return new InvoiceVerifyResultDTO.InvoiceVerifyResultDTOBuilder();
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getVerifyCode() {
        return this.verifyCode;
    }

    public String getInvoiceCode() {
        return this.invoiceCode;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public String getInvoiceType() {
        return this.invoiceType;
    }

    public String getInvoiceTypeName() {
        return this.invoiceTypeName;
    }

    public String getIssueDate() {
        return this.issueDate;
    }

    public String getBuyerName() {
        return this.buyerName;
    }

    public String getBuyerTaxNo() {
        return this.buyerTaxNo;
    }

    public String getBuyerAddressPhone() {
        return this.buyerAddressPhone;
    }

    public String getBuyerBankAccount() {
        return this.buyerBankAccount;
    }

    public String getSellerName() {
        return this.sellerName;
    }

    public String getSellerTaxNo() {
        return this.sellerTaxNo;
    }

    public String getSellerAddressPhone() {
        return this.sellerAddressPhone;
    }

    public String getSellerBankAccount() {
        return this.sellerBankAccount;
    }

    public BigDecimal getAmountWithoutTax() {
        return this.amountWithoutTax;
    }

    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getCheckCode() {
        return this.checkCode;
    }

    public String getMachineCode() {
        return this.machineCode;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getPayee() {
        return this.payee;
    }

    public String getChecker() {
        return this.checker;
    }

    public String getInvalidFlag() {
        return this.invalidFlag;
    }

    public Integer getInvoiceStatus() {
        return this.invoiceStatus;
    }

    public String getInvoiceStatusDescription() {
        return this.invoiceStatusDescription;
    }

    public int getVerifyCount() {
        return this.verifyCount;
    }

    public LocalDateTime getVerifyTime() {
        return this.verifyTime;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getStatusDescription() {
        return this.statusDescription;
    }

    public String getPdfUrl() {
        return this.pdfUrl;
    }

    public String getOfdUrl() {
        return this.ofdUrl;
    }

    public List<InvoiceItemDTO> getItems() {
        return this.items;
    }

    public String getRawResponse() {
        return this.rawResponse;
    }

    public String getVerifySource() {
        return this.verifySource;
    }

    public boolean isTaxPlatformVerified() {
        return this.taxPlatformVerified;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setVerifyCode(final String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public void setInvoiceTypeName(final String invoiceTypeName) {
        this.invoiceTypeName = invoiceTypeName;
    }

    public void setIssueDate(final String issueDate) {
        this.issueDate = issueDate;
    }

    public void setBuyerName(final String buyerName) {
        this.buyerName = buyerName;
    }

    public void setBuyerTaxNo(final String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    public void setBuyerAddressPhone(final String buyerAddressPhone) {
        this.buyerAddressPhone = buyerAddressPhone;
    }

    public void setBuyerBankAccount(final String buyerBankAccount) {
        this.buyerBankAccount = buyerBankAccount;
    }

    public void setSellerName(final String sellerName) {
        this.sellerName = sellerName;
    }

    public void setSellerTaxNo(final String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    public void setSellerAddressPhone(final String sellerAddressPhone) {
        this.sellerAddressPhone = sellerAddressPhone;
    }

    public void setSellerBankAccount(final String sellerBankAccount) {
        this.sellerBankAccount = sellerBankAccount;
    }

    public void setAmountWithoutTax(final BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
    }

    public void setMachineCode(final String machineCode) {
        this.machineCode = machineCode;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setPayee(final String payee) {
        this.payee = payee;
    }

    public void setChecker(final String checker) {
        this.checker = checker;
    }

    public void setInvalidFlag(final String invalidFlag) {
        this.invalidFlag = invalidFlag;
    }

    public void setInvoiceStatus(final Integer invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public void setInvoiceStatusDescription(final String invoiceStatusDescription) {
        this.invoiceStatusDescription = invoiceStatusDescription;
    }

    public void setVerifyCount(final int verifyCount) {
        this.verifyCount = verifyCount;
    }

    public void setVerifyTime(final LocalDateTime verifyTime) {
        this.verifyTime = verifyTime;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setStatusDescription(final String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public void setPdfUrl(final String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public void setOfdUrl(final String ofdUrl) {
        this.ofdUrl = ofdUrl;
    }

    public void setItems(final List<InvoiceItemDTO> items) {
        this.items = items;
    }

    public void setRawResponse(final String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public void setVerifySource(final String verifySource) {
        this.verifySource = verifySource;
    }

    public void setTaxPlatformVerified(final boolean taxPlatformVerified) {
        this.taxPlatformVerified = taxPlatformVerified;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvoiceVerifyResultDTO)) return false;
        final InvoiceVerifyResultDTO other = (InvoiceVerifyResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isSuccess() != other.isSuccess()) return false;
        if (this.getVerifyCount() != other.getVerifyCount()) return false;
        if (this.isTaxPlatformVerified() != other.isTaxPlatformVerified()) return false;
        final java.lang.Object this$invoiceStatus = this.getInvoiceStatus();
        final java.lang.Object other$invoiceStatus = other.getInvoiceStatus();
        if (this$invoiceStatus == null ? other$invoiceStatus != null : !this$invoiceStatus.equals(other$invoiceStatus)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$verifyCode = this.getVerifyCode();
        final java.lang.Object other$verifyCode = other.getVerifyCode();
        if (this$verifyCode == null ? other$verifyCode != null : !this$verifyCode.equals(other$verifyCode)) return false;
        final java.lang.Object this$invoiceCode = this.getInvoiceCode();
        final java.lang.Object other$invoiceCode = other.getInvoiceCode();
        if (this$invoiceCode == null ? other$invoiceCode != null : !this$invoiceCode.equals(other$invoiceCode)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$invoiceTypeName = this.getInvoiceTypeName();
        final java.lang.Object other$invoiceTypeName = other.getInvoiceTypeName();
        if (this$invoiceTypeName == null ? other$invoiceTypeName != null : !this$invoiceTypeName.equals(other$invoiceTypeName)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$buyerName = this.getBuyerName();
        final java.lang.Object other$buyerName = other.getBuyerName();
        if (this$buyerName == null ? other$buyerName != null : !this$buyerName.equals(other$buyerName)) return false;
        final java.lang.Object this$buyerTaxNo = this.getBuyerTaxNo();
        final java.lang.Object other$buyerTaxNo = other.getBuyerTaxNo();
        if (this$buyerTaxNo == null ? other$buyerTaxNo != null : !this$buyerTaxNo.equals(other$buyerTaxNo)) return false;
        final java.lang.Object this$buyerAddressPhone = this.getBuyerAddressPhone();
        final java.lang.Object other$buyerAddressPhone = other.getBuyerAddressPhone();
        if (this$buyerAddressPhone == null ? other$buyerAddressPhone != null : !this$buyerAddressPhone.equals(other$buyerAddressPhone)) return false;
        final java.lang.Object this$buyerBankAccount = this.getBuyerBankAccount();
        final java.lang.Object other$buyerBankAccount = other.getBuyerBankAccount();
        if (this$buyerBankAccount == null ? other$buyerBankAccount != null : !this$buyerBankAccount.equals(other$buyerBankAccount)) return false;
        final java.lang.Object this$sellerName = this.getSellerName();
        final java.lang.Object other$sellerName = other.getSellerName();
        if (this$sellerName == null ? other$sellerName != null : !this$sellerName.equals(other$sellerName)) return false;
        final java.lang.Object this$sellerTaxNo = this.getSellerTaxNo();
        final java.lang.Object other$sellerTaxNo = other.getSellerTaxNo();
        if (this$sellerTaxNo == null ? other$sellerTaxNo != null : !this$sellerTaxNo.equals(other$sellerTaxNo)) return false;
        final java.lang.Object this$sellerAddressPhone = this.getSellerAddressPhone();
        final java.lang.Object other$sellerAddressPhone = other.getSellerAddressPhone();
        if (this$sellerAddressPhone == null ? other$sellerAddressPhone != null : !this$sellerAddressPhone.equals(other$sellerAddressPhone)) return false;
        final java.lang.Object this$sellerBankAccount = this.getSellerBankAccount();
        final java.lang.Object other$sellerBankAccount = other.getSellerBankAccount();
        if (this$sellerBankAccount == null ? other$sellerBankAccount != null : !this$sellerBankAccount.equals(other$sellerBankAccount)) return false;
        final java.lang.Object this$amountWithoutTax = this.getAmountWithoutTax();
        final java.lang.Object other$amountWithoutTax = other.getAmountWithoutTax();
        if (this$amountWithoutTax == null ? other$amountWithoutTax != null : !this$amountWithoutTax.equals(other$amountWithoutTax)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
        final java.lang.Object this$machineCode = this.getMachineCode();
        final java.lang.Object other$machineCode = other.getMachineCode();
        if (this$machineCode == null ? other$machineCode != null : !this$machineCode.equals(other$machineCode)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$payee = this.getPayee();
        final java.lang.Object other$payee = other.getPayee();
        if (this$payee == null ? other$payee != null : !this$payee.equals(other$payee)) return false;
        final java.lang.Object this$checker = this.getChecker();
        final java.lang.Object other$checker = other.getChecker();
        if (this$checker == null ? other$checker != null : !this$checker.equals(other$checker)) return false;
        final java.lang.Object this$invalidFlag = this.getInvalidFlag();
        final java.lang.Object other$invalidFlag = other.getInvalidFlag();
        if (this$invalidFlag == null ? other$invalidFlag != null : !this$invalidFlag.equals(other$invalidFlag)) return false;
        final java.lang.Object this$invoiceStatusDescription = this.getInvoiceStatusDescription();
        final java.lang.Object other$invoiceStatusDescription = other.getInvoiceStatusDescription();
        if (this$invoiceStatusDescription == null ? other$invoiceStatusDescription != null : !this$invoiceStatusDescription.equals(other$invoiceStatusDescription)) return false;
        final java.lang.Object this$verifyTime = this.getVerifyTime();
        final java.lang.Object other$verifyTime = other.getVerifyTime();
        if (this$verifyTime == null ? other$verifyTime != null : !this$verifyTime.equals(other$verifyTime)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$statusDescription = this.getStatusDescription();
        final java.lang.Object other$statusDescription = other.getStatusDescription();
        if (this$statusDescription == null ? other$statusDescription != null : !this$statusDescription.equals(other$statusDescription)) return false;
        final java.lang.Object this$pdfUrl = this.getPdfUrl();
        final java.lang.Object other$pdfUrl = other.getPdfUrl();
        if (this$pdfUrl == null ? other$pdfUrl != null : !this$pdfUrl.equals(other$pdfUrl)) return false;
        final java.lang.Object this$ofdUrl = this.getOfdUrl();
        final java.lang.Object other$ofdUrl = other.getOfdUrl();
        if (this$ofdUrl == null ? other$ofdUrl != null : !this$ofdUrl.equals(other$ofdUrl)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        final java.lang.Object this$rawResponse = this.getRawResponse();
        final java.lang.Object other$rawResponse = other.getRawResponse();
        if (this$rawResponse == null ? other$rawResponse != null : !this$rawResponse.equals(other$rawResponse)) return false;
        final java.lang.Object this$verifySource = this.getVerifySource();
        final java.lang.Object other$verifySource = other.getVerifySource();
        if (this$verifySource == null ? other$verifySource != null : !this$verifySource.equals(other$verifySource)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvoiceVerifyResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSuccess() ? 79 : 97);
        result = result * PRIME + this.getVerifyCount();
        result = result * PRIME + (this.isTaxPlatformVerified() ? 79 : 97);
        final java.lang.Object $invoiceStatus = this.getInvoiceStatus();
        result = result * PRIME + ($invoiceStatus == null ? 43 : $invoiceStatus.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $verifyCode = this.getVerifyCode();
        result = result * PRIME + ($verifyCode == null ? 43 : $verifyCode.hashCode());
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $invoiceTypeName = this.getInvoiceTypeName();
        result = result * PRIME + ($invoiceTypeName == null ? 43 : $invoiceTypeName.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $buyerName = this.getBuyerName();
        result = result * PRIME + ($buyerName == null ? 43 : $buyerName.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $buyerAddressPhone = this.getBuyerAddressPhone();
        result = result * PRIME + ($buyerAddressPhone == null ? 43 : $buyerAddressPhone.hashCode());
        final java.lang.Object $buyerBankAccount = this.getBuyerBankAccount();
        result = result * PRIME + ($buyerBankAccount == null ? 43 : $buyerBankAccount.hashCode());
        final java.lang.Object $sellerName = this.getSellerName();
        result = result * PRIME + ($sellerName == null ? 43 : $sellerName.hashCode());
        final java.lang.Object $sellerTaxNo = this.getSellerTaxNo();
        result = result * PRIME + ($sellerTaxNo == null ? 43 : $sellerTaxNo.hashCode());
        final java.lang.Object $sellerAddressPhone = this.getSellerAddressPhone();
        result = result * PRIME + ($sellerAddressPhone == null ? 43 : $sellerAddressPhone.hashCode());
        final java.lang.Object $sellerBankAccount = this.getSellerBankAccount();
        result = result * PRIME + ($sellerBankAccount == null ? 43 : $sellerBankAccount.hashCode());
        final java.lang.Object $amountWithoutTax = this.getAmountWithoutTax();
        result = result * PRIME + ($amountWithoutTax == null ? 43 : $amountWithoutTax.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
        final java.lang.Object $machineCode = this.getMachineCode();
        result = result * PRIME + ($machineCode == null ? 43 : $machineCode.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $payee = this.getPayee();
        result = result * PRIME + ($payee == null ? 43 : $payee.hashCode());
        final java.lang.Object $checker = this.getChecker();
        result = result * PRIME + ($checker == null ? 43 : $checker.hashCode());
        final java.lang.Object $invalidFlag = this.getInvalidFlag();
        result = result * PRIME + ($invalidFlag == null ? 43 : $invalidFlag.hashCode());
        final java.lang.Object $invoiceStatusDescription = this.getInvoiceStatusDescription();
        result = result * PRIME + ($invoiceStatusDescription == null ? 43 : $invoiceStatusDescription.hashCode());
        final java.lang.Object $verifyTime = this.getVerifyTime();
        result = result * PRIME + ($verifyTime == null ? 43 : $verifyTime.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $statusDescription = this.getStatusDescription();
        result = result * PRIME + ($statusDescription == null ? 43 : $statusDescription.hashCode());
        final java.lang.Object $pdfUrl = this.getPdfUrl();
        result = result * PRIME + ($pdfUrl == null ? 43 : $pdfUrl.hashCode());
        final java.lang.Object $ofdUrl = this.getOfdUrl();
        result = result * PRIME + ($ofdUrl == null ? 43 : $ofdUrl.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        final java.lang.Object $rawResponse = this.getRawResponse();
        result = result * PRIME + ($rawResponse == null ? 43 : $rawResponse.hashCode());
        final java.lang.Object $verifySource = this.getVerifySource();
        result = result * PRIME + ($verifySource == null ? 43 : $verifySource.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvoiceVerifyResultDTO(success=" + this.isSuccess() + ", message=" + this.getMessage() + ", verifyCode=" + this.getVerifyCode() + ", invoiceCode=" + this.getInvoiceCode() + ", invoiceNo=" + this.getInvoiceNo() + ", invoiceType=" + this.getInvoiceType() + ", invoiceTypeName=" + this.getInvoiceTypeName() + ", issueDate=" + this.getIssueDate() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", buyerAddressPhone=" + this.getBuyerAddressPhone() + ", buyerBankAccount=" + this.getBuyerBankAccount() + ", sellerName=" + this.getSellerName() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", sellerAddressPhone=" + this.getSellerAddressPhone() + ", sellerBankAccount=" + this.getSellerBankAccount() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", taxAmount=" + this.getTaxAmount() + ", totalAmount=" + this.getTotalAmount() + ", checkCode=" + this.getCheckCode() + ", machineCode=" + this.getMachineCode() + ", remark=" + this.getRemark() + ", payee=" + this.getPayee() + ", checker=" + this.getChecker() + ", invalidFlag=" + this.getInvalidFlag() + ", invoiceStatus=" + this.getInvoiceStatus() + ", invoiceStatusDescription=" + this.getInvoiceStatusDescription() + ", verifyCount=" + this.getVerifyCount() + ", verifyTime=" + this.getVerifyTime() + ", status=" + this.getStatus() + ", errorMessage=" + this.getErrorMessage() + ", statusDescription=" + this.getStatusDescription() + ", pdfUrl=" + this.getPdfUrl() + ", ofdUrl=" + this.getOfdUrl() + ", items=" + this.getItems() + ", rawResponse=" + this.getRawResponse() + ", verifySource=" + this.getVerifySource() + ", taxPlatformVerified=" + this.isTaxPlatformVerified() + ")";
    }

    public InvoiceVerifyResultDTO() {
    }

    public InvoiceVerifyResultDTO(final boolean success, final String message, final String verifyCode, final String invoiceCode, final String invoiceNo, final String invoiceType, final String invoiceTypeName, final String issueDate, final String buyerName, final String buyerTaxNo, final String buyerAddressPhone, final String buyerBankAccount, final String sellerName, final String sellerTaxNo, final String sellerAddressPhone, final String sellerBankAccount, final BigDecimal amountWithoutTax, final BigDecimal taxAmount, final BigDecimal totalAmount, final String checkCode, final String machineCode, final String remark, final String payee, final String checker, final String invalidFlag, final Integer invoiceStatus, final String invoiceStatusDescription, final int verifyCount, final LocalDateTime verifyTime, final Integer status, final String errorMessage, final String statusDescription, final String pdfUrl, final String ofdUrl, final List<InvoiceItemDTO> items, final String rawResponse, final String verifySource, final boolean taxPlatformVerified) {
        this.success = success;
        this.message = message;
        this.verifyCode = verifyCode;
        this.invoiceCode = invoiceCode;
        this.invoiceNo = invoiceNo;
        this.invoiceType = invoiceType;
        this.invoiceTypeName = invoiceTypeName;
        this.issueDate = issueDate;
        this.buyerName = buyerName;
        this.buyerTaxNo = buyerTaxNo;
        this.buyerAddressPhone = buyerAddressPhone;
        this.buyerBankAccount = buyerBankAccount;
        this.sellerName = sellerName;
        this.sellerTaxNo = sellerTaxNo;
        this.sellerAddressPhone = sellerAddressPhone;
        this.sellerBankAccount = sellerBankAccount;
        this.amountWithoutTax = amountWithoutTax;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.checkCode = checkCode;
        this.machineCode = machineCode;
        this.remark = remark;
        this.payee = payee;
        this.checker = checker;
        this.invalidFlag = invalidFlag;
        this.invoiceStatus = invoiceStatus;
        this.invoiceStatusDescription = invoiceStatusDescription;
        this.verifyCount = verifyCount;
        this.verifyTime = verifyTime;
        this.status = status;
        this.errorMessage = errorMessage;
        this.statusDescription = statusDescription;
        this.pdfUrl = pdfUrl;
        this.ofdUrl = ofdUrl;
        this.items = items;
        this.rawResponse = rawResponse;
        this.verifySource = verifySource;
        this.taxPlatformVerified = taxPlatformVerified;
    }
}
