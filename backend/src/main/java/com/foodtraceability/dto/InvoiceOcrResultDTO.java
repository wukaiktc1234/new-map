package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发票OCR识别结果DTO
 * 
 * 包含从发票图片中识别出的所有关键信息
 */
public class InvoiceOcrResultDTO {
    private boolean success;
    private String errorMessage;
    private String engineName;
    private String invoiceCode;
    private String invoiceNo;
    private String invoiceType;
    private String invoiceTypeName;
    private String machineNo;
    private String issueDate;
    private String checkCode;
    private String buyerName;
    private String buyerTaxNo;
    private String buyerAddressPhone;
    private String buyerBankAccount;
    private String sellerName;
    private String sellerTaxNo;
    private String sellerAddressPhone;
    private String sellerBankAccount;
    private BigDecimal amountWithoutTax;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String goodsName;
    private String goodsSpec;
    private String goodsUnit;
    private String goodsQuantity;
    private String goodsPrice;
    private List<InvoiceGoodsItemDTO> goodsItems;
    private String payee;
    private String checker;
    private String issuer;
    private String amountInWords;
    private String remarks;
    private String rawText;
    private Double confidence;
    private Double processingTimeMs;

    public static InvoiceOcrResultDTO success(String engineName) {
        InvoiceOcrResultDTO dto = new InvoiceOcrResultDTO();
        dto.setSuccess(true);
        dto.setEngineName(engineName);
        return dto;
    }

    public static InvoiceOcrResultDTO failed(String errorMessage) {
        InvoiceOcrResultDTO dto = new InvoiceOcrResultDTO();
        dto.setSuccess(false);
        dto.setErrorMessage(errorMessage);
        return dto;
    }

    public boolean hasBasicInfo() {
        return invoiceCode != null && invoiceNo != null && issueDate != null && (checkCode != null || totalAmount != null);
    }

    public boolean hasCompleteInfo() {
        return hasBasicInfo() && buyerName != null && buyerTaxNo != null && sellerName != null && sellerTaxNo != null && amountWithoutTax != null && taxAmount != null && totalAmount != null;
    }


    public static class InvoiceOcrResultDTOBuilder {
        private boolean success;
        private String errorMessage;
        private String engineName;
        private String invoiceCode;
        private String invoiceNo;
        private String invoiceType;
        private String invoiceTypeName;
        private String machineNo;
        private String issueDate;
        private String checkCode;
        private String buyerName;
        private String buyerTaxNo;
        private String buyerAddressPhone;
        private String buyerBankAccount;
        private String sellerName;
        private String sellerTaxNo;
        private String sellerAddressPhone;
        private String sellerBankAccount;
        private BigDecimal amountWithoutTax;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String goodsName;
        private String goodsSpec;
        private String goodsUnit;
        private String goodsQuantity;
        private String goodsPrice;
        private List<InvoiceGoodsItemDTO> goodsItems;
        private String payee;
        private String checker;
        private String issuer;
        private String amountInWords;
        private String remarks;
        private String rawText;
        private Double confidence;
        private Double processingTimeMs;

        InvoiceOcrResultDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder success(final boolean success) {
            this.success = success;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder engineName(final String engineName) {
            this.engineName = engineName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder invoiceCode(final String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder invoiceNo(final String invoiceNo) {
            this.invoiceNo = invoiceNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder invoiceType(final String invoiceType) {
            this.invoiceType = invoiceType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder invoiceTypeName(final String invoiceTypeName) {
            this.invoiceTypeName = invoiceTypeName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder machineNo(final String machineNo) {
            this.machineNo = machineNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder issueDate(final String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder checkCode(final String checkCode) {
            this.checkCode = checkCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder buyerName(final String buyerName) {
            this.buyerName = buyerName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder buyerTaxNo(final String buyerTaxNo) {
            this.buyerTaxNo = buyerTaxNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder buyerAddressPhone(final String buyerAddressPhone) {
            this.buyerAddressPhone = buyerAddressPhone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder buyerBankAccount(final String buyerBankAccount) {
            this.buyerBankAccount = buyerBankAccount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder sellerName(final String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder sellerTaxNo(final String sellerTaxNo) {
            this.sellerTaxNo = sellerTaxNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder sellerAddressPhone(final String sellerAddressPhone) {
            this.sellerAddressPhone = sellerAddressPhone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder sellerBankAccount(final String sellerBankAccount) {
            this.sellerBankAccount = sellerBankAccount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder amountWithoutTax(final BigDecimal amountWithoutTax) {
            this.amountWithoutTax = amountWithoutTax;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder taxRate(final BigDecimal taxRate) {
            this.taxRate = taxRate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder taxAmount(final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder totalAmount(final BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder goodsName(final String goodsName) {
            this.goodsName = goodsName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder goodsSpec(final String goodsSpec) {
            this.goodsSpec = goodsSpec;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder goodsUnit(final String goodsUnit) {
            this.goodsUnit = goodsUnit;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder goodsQuantity(final String goodsQuantity) {
            this.goodsQuantity = goodsQuantity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder goodsPrice(final String goodsPrice) {
            this.goodsPrice = goodsPrice;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder goodsItems(final List<InvoiceGoodsItemDTO> goodsItems) {
            this.goodsItems = goodsItems;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder payee(final String payee) {
            this.payee = payee;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder checker(final String checker) {
            this.checker = checker;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder issuer(final String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder amountInWords(final String amountInWords) {
            this.amountInWords = amountInWords;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder remarks(final String remarks) {
            this.remarks = remarks;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder rawText(final String rawText) {
            this.rawText = rawText;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder confidence(final Double confidence) {
            this.confidence = confidence;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder processingTimeMs(final Double processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public InvoiceOcrResultDTO build() {
            return new InvoiceOcrResultDTO(this.success, this.errorMessage, this.engineName, this.invoiceCode, this.invoiceNo, this.invoiceType, this.invoiceTypeName, this.machineNo, this.issueDate, this.checkCode, this.buyerName, this.buyerTaxNo, this.buyerAddressPhone, this.buyerBankAccount, this.sellerName, this.sellerTaxNo, this.sellerAddressPhone, this.sellerBankAccount, this.amountWithoutTax, this.taxRate, this.taxAmount, this.totalAmount, this.goodsName, this.goodsSpec, this.goodsUnit, this.goodsQuantity, this.goodsPrice, this.goodsItems, this.payee, this.checker, this.issuer, this.amountInWords, this.remarks, this.rawText, this.confidence, this.processingTimeMs);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder(success=" + this.success + ", errorMessage=" + this.errorMessage + ", engineName=" + this.engineName + ", invoiceCode=" + this.invoiceCode + ", invoiceNo=" + this.invoiceNo + ", invoiceType=" + this.invoiceType + ", invoiceTypeName=" + this.invoiceTypeName + ", machineNo=" + this.machineNo + ", issueDate=" + this.issueDate + ", checkCode=" + this.checkCode + ", buyerName=" + this.buyerName + ", buyerTaxNo=" + this.buyerTaxNo + ", buyerAddressPhone=" + this.buyerAddressPhone + ", buyerBankAccount=" + this.buyerBankAccount + ", sellerName=" + this.sellerName + ", sellerTaxNo=" + this.sellerTaxNo + ", sellerAddressPhone=" + this.sellerAddressPhone + ", sellerBankAccount=" + this.sellerBankAccount + ", amountWithoutTax=" + this.amountWithoutTax + ", taxRate=" + this.taxRate + ", taxAmount=" + this.taxAmount + ", totalAmount=" + this.totalAmount + ", goodsName=" + this.goodsName + ", goodsSpec=" + this.goodsSpec + ", goodsUnit=" + this.goodsUnit + ", goodsQuantity=" + this.goodsQuantity + ", goodsPrice=" + this.goodsPrice + ", goodsItems=" + this.goodsItems + ", payee=" + this.payee + ", checker=" + this.checker + ", issuer=" + this.issuer + ", amountInWords=" + this.amountInWords + ", remarks=" + this.remarks + ", rawText=" + this.rawText + ", confidence=" + this.confidence + ", processingTimeMs=" + this.processingTimeMs + ")";
        }
    }

    public static InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder builder() {
        return new InvoiceOcrResultDTO.InvoiceOcrResultDTOBuilder();
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getEngineName() {
        return this.engineName;
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

    public String getMachineNo() {
        return this.machineNo;
    }

    public String getIssueDate() {
        return this.issueDate;
    }

    public String getCheckCode() {
        return this.checkCode;
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

    public BigDecimal getTaxRate() {
        return this.taxRate;
    }

    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getGoodsName() {
        return this.goodsName;
    }

    public String getGoodsSpec() {
        return this.goodsSpec;
    }

    public String getGoodsUnit() {
        return this.goodsUnit;
    }

    public String getGoodsQuantity() {
        return this.goodsQuantity;
    }

    public String getGoodsPrice() {
        return this.goodsPrice;
    }

    public List<InvoiceGoodsItemDTO> getGoodsItems() {
        return this.goodsItems;
    }

    public String getPayee() {
        return this.payee;
    }

    public String getChecker() {
        return this.checker;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public String getAmountInWords() {
        return this.amountInWords;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public String getRawText() {
        return this.rawText;
    }

    public Double getConfidence() {
        return this.confidence;
    }

    public Double getProcessingTimeMs() {
        return this.processingTimeMs;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setEngineName(final String engineName) {
        this.engineName = engineName;
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

    public void setMachineNo(final String machineNo) {
        this.machineNo = machineNo;
    }

    public void setIssueDate(final String issueDate) {
        this.issueDate = issueDate;
    }

    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
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

    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setGoodsName(final String goodsName) {
        this.goodsName = goodsName;
    }

    public void setGoodsSpec(final String goodsSpec) {
        this.goodsSpec = goodsSpec;
    }

    public void setGoodsUnit(final String goodsUnit) {
        this.goodsUnit = goodsUnit;
    }

    public void setGoodsQuantity(final String goodsQuantity) {
        this.goodsQuantity = goodsQuantity;
    }

    public void setGoodsPrice(final String goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public void setGoodsItems(final List<InvoiceGoodsItemDTO> goodsItems) {
        this.goodsItems = goodsItems;
    }

    public void setPayee(final String payee) {
        this.payee = payee;
    }

    public void setChecker(final String checker) {
        this.checker = checker;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public void setAmountInWords(final String amountInWords) {
        this.amountInWords = amountInWords;
    }

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    public void setRawText(final String rawText) {
        this.rawText = rawText;
    }

    public void setConfidence(final Double confidence) {
        this.confidence = confidence;
    }

    public void setProcessingTimeMs(final Double processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvoiceOcrResultDTO)) return false;
        final InvoiceOcrResultDTO other = (InvoiceOcrResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isSuccess() != other.isSuccess()) return false;
        final java.lang.Object this$confidence = this.getConfidence();
        final java.lang.Object other$confidence = other.getConfidence();
        if (this$confidence == null ? other$confidence != null : !this$confidence.equals(other$confidence)) return false;
        final java.lang.Object this$processingTimeMs = this.getProcessingTimeMs();
        final java.lang.Object other$processingTimeMs = other.getProcessingTimeMs();
        if (this$processingTimeMs == null ? other$processingTimeMs != null : !this$processingTimeMs.equals(other$processingTimeMs)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$engineName = this.getEngineName();
        final java.lang.Object other$engineName = other.getEngineName();
        if (this$engineName == null ? other$engineName != null : !this$engineName.equals(other$engineName)) return false;
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
        final java.lang.Object this$machineNo = this.getMachineNo();
        final java.lang.Object other$machineNo = other.getMachineNo();
        if (this$machineNo == null ? other$machineNo != null : !this$machineNo.equals(other$machineNo)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
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
        final java.lang.Object this$taxRate = this.getTaxRate();
        final java.lang.Object other$taxRate = other.getTaxRate();
        if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$goodsName = this.getGoodsName();
        final java.lang.Object other$goodsName = other.getGoodsName();
        if (this$goodsName == null ? other$goodsName != null : !this$goodsName.equals(other$goodsName)) return false;
        final java.lang.Object this$goodsSpec = this.getGoodsSpec();
        final java.lang.Object other$goodsSpec = other.getGoodsSpec();
        if (this$goodsSpec == null ? other$goodsSpec != null : !this$goodsSpec.equals(other$goodsSpec)) return false;
        final java.lang.Object this$goodsUnit = this.getGoodsUnit();
        final java.lang.Object other$goodsUnit = other.getGoodsUnit();
        if (this$goodsUnit == null ? other$goodsUnit != null : !this$goodsUnit.equals(other$goodsUnit)) return false;
        final java.lang.Object this$goodsQuantity = this.getGoodsQuantity();
        final java.lang.Object other$goodsQuantity = other.getGoodsQuantity();
        if (this$goodsQuantity == null ? other$goodsQuantity != null : !this$goodsQuantity.equals(other$goodsQuantity)) return false;
        final java.lang.Object this$goodsPrice = this.getGoodsPrice();
        final java.lang.Object other$goodsPrice = other.getGoodsPrice();
        if (this$goodsPrice == null ? other$goodsPrice != null : !this$goodsPrice.equals(other$goodsPrice)) return false;
        final java.lang.Object this$goodsItems = this.getGoodsItems();
        final java.lang.Object other$goodsItems = other.getGoodsItems();
        if (this$goodsItems == null ? other$goodsItems != null : !this$goodsItems.equals(other$goodsItems)) return false;
        final java.lang.Object this$payee = this.getPayee();
        final java.lang.Object other$payee = other.getPayee();
        if (this$payee == null ? other$payee != null : !this$payee.equals(other$payee)) return false;
        final java.lang.Object this$checker = this.getChecker();
        final java.lang.Object other$checker = other.getChecker();
        if (this$checker == null ? other$checker != null : !this$checker.equals(other$checker)) return false;
        final java.lang.Object this$issuer = this.getIssuer();
        final java.lang.Object other$issuer = other.getIssuer();
        if (this$issuer == null ? other$issuer != null : !this$issuer.equals(other$issuer)) return false;
        final java.lang.Object this$amountInWords = this.getAmountInWords();
        final java.lang.Object other$amountInWords = other.getAmountInWords();
        if (this$amountInWords == null ? other$amountInWords != null : !this$amountInWords.equals(other$amountInWords)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        final java.lang.Object this$rawText = this.getRawText();
        final java.lang.Object other$rawText = other.getRawText();
        if (this$rawText == null ? other$rawText != null : !this$rawText.equals(other$rawText)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvoiceOcrResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSuccess() ? 79 : 97);
        final java.lang.Object $confidence = this.getConfidence();
        result = result * PRIME + ($confidence == null ? 43 : $confidence.hashCode());
        final java.lang.Object $processingTimeMs = this.getProcessingTimeMs();
        result = result * PRIME + ($processingTimeMs == null ? 43 : $processingTimeMs.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $engineName = this.getEngineName();
        result = result * PRIME + ($engineName == null ? 43 : $engineName.hashCode());
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $invoiceTypeName = this.getInvoiceTypeName();
        result = result * PRIME + ($invoiceTypeName == null ? 43 : $invoiceTypeName.hashCode());
        final java.lang.Object $machineNo = this.getMachineNo();
        result = result * PRIME + ($machineNo == null ? 43 : $machineNo.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
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
        final java.lang.Object $taxRate = this.getTaxRate();
        result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $goodsName = this.getGoodsName();
        result = result * PRIME + ($goodsName == null ? 43 : $goodsName.hashCode());
        final java.lang.Object $goodsSpec = this.getGoodsSpec();
        result = result * PRIME + ($goodsSpec == null ? 43 : $goodsSpec.hashCode());
        final java.lang.Object $goodsUnit = this.getGoodsUnit();
        result = result * PRIME + ($goodsUnit == null ? 43 : $goodsUnit.hashCode());
        final java.lang.Object $goodsQuantity = this.getGoodsQuantity();
        result = result * PRIME + ($goodsQuantity == null ? 43 : $goodsQuantity.hashCode());
        final java.lang.Object $goodsPrice = this.getGoodsPrice();
        result = result * PRIME + ($goodsPrice == null ? 43 : $goodsPrice.hashCode());
        final java.lang.Object $goodsItems = this.getGoodsItems();
        result = result * PRIME + ($goodsItems == null ? 43 : $goodsItems.hashCode());
        final java.lang.Object $payee = this.getPayee();
        result = result * PRIME + ($payee == null ? 43 : $payee.hashCode());
        final java.lang.Object $checker = this.getChecker();
        result = result * PRIME + ($checker == null ? 43 : $checker.hashCode());
        final java.lang.Object $issuer = this.getIssuer();
        result = result * PRIME + ($issuer == null ? 43 : $issuer.hashCode());
        final java.lang.Object $amountInWords = this.getAmountInWords();
        result = result * PRIME + ($amountInWords == null ? 43 : $amountInWords.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        final java.lang.Object $rawText = this.getRawText();
        result = result * PRIME + ($rawText == null ? 43 : $rawText.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvoiceOcrResultDTO(success=" + this.isSuccess() + ", errorMessage=" + this.getErrorMessage() + ", engineName=" + this.getEngineName() + ", invoiceCode=" + this.getInvoiceCode() + ", invoiceNo=" + this.getInvoiceNo() + ", invoiceType=" + this.getInvoiceType() + ", invoiceTypeName=" + this.getInvoiceTypeName() + ", machineNo=" + this.getMachineNo() + ", issueDate=" + this.getIssueDate() + ", checkCode=" + this.getCheckCode() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", buyerAddressPhone=" + this.getBuyerAddressPhone() + ", buyerBankAccount=" + this.getBuyerBankAccount() + ", sellerName=" + this.getSellerName() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", sellerAddressPhone=" + this.getSellerAddressPhone() + ", sellerBankAccount=" + this.getSellerBankAccount() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", totalAmount=" + this.getTotalAmount() + ", goodsName=" + this.getGoodsName() + ", goodsSpec=" + this.getGoodsSpec() + ", goodsUnit=" + this.getGoodsUnit() + ", goodsQuantity=" + this.getGoodsQuantity() + ", goodsPrice=" + this.getGoodsPrice() + ", goodsItems=" + this.getGoodsItems() + ", payee=" + this.getPayee() + ", checker=" + this.getChecker() + ", issuer=" + this.getIssuer() + ", amountInWords=" + this.getAmountInWords() + ", remarks=" + this.getRemarks() + ", rawText=" + this.getRawText() + ", confidence=" + this.getConfidence() + ", processingTimeMs=" + this.getProcessingTimeMs() + ")";
    }

    public InvoiceOcrResultDTO() {
    }

    public InvoiceOcrResultDTO(final boolean success, final String errorMessage, final String engineName, final String invoiceCode, final String invoiceNo, final String invoiceType, final String invoiceTypeName, final String machineNo, final String issueDate, final String checkCode, final String buyerName, final String buyerTaxNo, final String buyerAddressPhone, final String buyerBankAccount, final String sellerName, final String sellerTaxNo, final String sellerAddressPhone, final String sellerBankAccount, final BigDecimal amountWithoutTax, final BigDecimal taxRate, final BigDecimal taxAmount, final BigDecimal totalAmount, final String goodsName, final String goodsSpec, final String goodsUnit, final String goodsQuantity, final String goodsPrice, final List<InvoiceGoodsItemDTO> goodsItems, final String payee, final String checker, final String issuer, final String amountInWords, final String remarks, final String rawText, final Double confidence, final Double processingTimeMs) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.engineName = engineName;
        this.invoiceCode = invoiceCode;
        this.invoiceNo = invoiceNo;
        this.invoiceType = invoiceType;
        this.invoiceTypeName = invoiceTypeName;
        this.machineNo = machineNo;
        this.issueDate = issueDate;
        this.checkCode = checkCode;
        this.buyerName = buyerName;
        this.buyerTaxNo = buyerTaxNo;
        this.buyerAddressPhone = buyerAddressPhone;
        this.buyerBankAccount = buyerBankAccount;
        this.sellerName = sellerName;
        this.sellerTaxNo = sellerTaxNo;
        this.sellerAddressPhone = sellerAddressPhone;
        this.sellerBankAccount = sellerBankAccount;
        this.amountWithoutTax = amountWithoutTax;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.goodsName = goodsName;
        this.goodsSpec = goodsSpec;
        this.goodsUnit = goodsUnit;
        this.goodsQuantity = goodsQuantity;
        this.goodsPrice = goodsPrice;
        this.goodsItems = goodsItems;
        this.payee = payee;
        this.checker = checker;
        this.issuer = issuer;
        this.amountInWords = amountInWords;
        this.remarks = remarks;
        this.rawText = rawText;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
    }
}
