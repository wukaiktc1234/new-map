package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 发票二维码信息DTO
 * 
 * 从PDF发票二维码中提取的信息
 */
public class InvoiceQrInfoDTO {
    /**
     * 发票代码
     */
    private String invoiceCode;
    /**
     * 发票号码
     */
    private String invoiceNo;
    /**
     * 开票日期
     */
    private String issueDate;
    /**
     * 校验码（后6位）
     */
    private String checkCode;
    /**
     * 金额（不含税）
     */
    private BigDecimal amountWithoutTax;
    /**
     * 价税合计
     */
    private BigDecimal totalAmount;
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    /**
     * 购买方名称
     */
    private String buyerName;
    /**
     * 购买方税号
     */
    private String buyerTaxNo;
    /**
     * 销售方名称
     */
    private String sellerName;
    /**
     * 销售方税号
     */
    private String sellerTaxNo;
    /**
     * 发票类型
     * 01:增值税专用发票
     * 04:增值税普通发票
     * 10:增值税电子普通发票
     * 11:增值税电子专用发票
     */
    private String invoiceType;
    /**
     * 发票类型名称
     */
    private String invoiceTypeName;
    /**
     * 二维码原始内容
     */
    private String rawContent;
    /**
     * 是否解析成功
     */
    private boolean success;
    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建成功结果
     */
    public static InvoiceQrInfoDTO success(String rawContent) {
        return InvoiceQrInfoDTO.builder().success(true).rawContent(rawContent).build();
    }

    /**
     * 创建失败结果
     */
    public static InvoiceQrInfoDTO failed(String errorMessage) {
        return InvoiceQrInfoDTO.builder().success(false).errorMessage(errorMessage).build();
    }


    public static class InvoiceQrInfoDTOBuilder {
        private String invoiceCode;
        private String invoiceNo;
        private String issueDate;
        private String checkCode;
        private BigDecimal amountWithoutTax;
        private BigDecimal totalAmount;
        private BigDecimal taxAmount;
        private String buyerName;
        private String buyerTaxNo;
        private String sellerName;
        private String sellerTaxNo;
        private String invoiceType;
        private String invoiceTypeName;
        private String rawContent;
        private boolean success;
        private String errorMessage;

        InvoiceQrInfoDTOBuilder() {
        }

        /**
         * 发票代码
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder invoiceCode(final String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        /**
         * 发票号码
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder invoiceNo(final String invoiceNo) {
            this.invoiceNo = invoiceNo;
            return this;
        }

        /**
         * 开票日期
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder issueDate(final String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * 校验码（后6位）
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder checkCode(final String checkCode) {
            this.checkCode = checkCode;
            return this;
        }

        /**
         * 金额（不含税）
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder amountWithoutTax(final BigDecimal amountWithoutTax) {
            this.amountWithoutTax = amountWithoutTax;
            return this;
        }

        /**
         * 价税合计
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder totalAmount(final BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /**
         * 税额
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder taxAmount(final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        /**
         * 购买方名称
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder buyerName(final String buyerName) {
            this.buyerName = buyerName;
            return this;
        }

        /**
         * 购买方税号
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder buyerTaxNo(final String buyerTaxNo) {
            this.buyerTaxNo = buyerTaxNo;
            return this;
        }

        /**
         * 销售方名称
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder sellerName(final String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        /**
         * 销售方税号
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder sellerTaxNo(final String sellerTaxNo) {
            this.sellerTaxNo = sellerTaxNo;
            return this;
        }

        /**
         * 发票类型
         * 01:增值税专用发票
         * 04:增值税普通发票
         * 10:增值税电子普通发票
         * 11:增值税电子专用发票
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder invoiceType(final String invoiceType) {
            this.invoiceType = invoiceType;
            return this;
        }

        /**
         * 发票类型名称
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder invoiceTypeName(final String invoiceTypeName) {
            this.invoiceTypeName = invoiceTypeName;
            return this;
        }

        /**
         * 二维码原始内容
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder rawContent(final String rawContent) {
            this.rawContent = rawContent;
            return this;
        }

        /**
         * 是否解析成功
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder success(final boolean success) {
            this.success = success;
            return this;
        }

        /**
         * 错误信息
         * @return {@code this}.
         */
        public InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public InvoiceQrInfoDTO build() {
            return new InvoiceQrInfoDTO(this.invoiceCode, this.invoiceNo, this.issueDate, this.checkCode, this.amountWithoutTax, this.totalAmount, this.taxAmount, this.buyerName, this.buyerTaxNo, this.sellerName, this.sellerTaxNo, this.invoiceType, this.invoiceTypeName, this.rawContent, this.success, this.errorMessage);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder(invoiceCode=" + this.invoiceCode + ", invoiceNo=" + this.invoiceNo + ", issueDate=" + this.issueDate + ", checkCode=" + this.checkCode + ", amountWithoutTax=" + this.amountWithoutTax + ", totalAmount=" + this.totalAmount + ", taxAmount=" + this.taxAmount + ", buyerName=" + this.buyerName + ", buyerTaxNo=" + this.buyerTaxNo + ", sellerName=" + this.sellerName + ", sellerTaxNo=" + this.sellerTaxNo + ", invoiceType=" + this.invoiceType + ", invoiceTypeName=" + this.invoiceTypeName + ", rawContent=" + this.rawContent + ", success=" + this.success + ", errorMessage=" + this.errorMessage + ")";
        }
    }

    public static InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder builder() {
        return new InvoiceQrInfoDTO.InvoiceQrInfoDTOBuilder();
    }

    /**
     * 发票代码
     */
    public String getInvoiceCode() {
        return this.invoiceCode;
    }

    /**
     * 发票号码
     */
    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    /**
     * 开票日期
     */
    public String getIssueDate() {
        return this.issueDate;
    }

    /**
     * 校验码（后6位）
     */
    public String getCheckCode() {
        return this.checkCode;
    }

    /**
     * 金额（不含税）
     */
    public BigDecimal getAmountWithoutTax() {
        return this.amountWithoutTax;
    }

    /**
     * 价税合计
     */
    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    /**
     * 税额
     */
    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    /**
     * 购买方名称
     */
    public String getBuyerName() {
        return this.buyerName;
    }

    /**
     * 购买方税号
     */
    public String getBuyerTaxNo() {
        return this.buyerTaxNo;
    }

    /**
     * 销售方名称
     */
    public String getSellerName() {
        return this.sellerName;
    }

    /**
     * 销售方税号
     */
    public String getSellerTaxNo() {
        return this.sellerTaxNo;
    }

    /**
     * 发票类型
     * 01:增值税专用发票
     * 04:增值税普通发票
     * 10:增值税电子普通发票
     * 11:增值税电子专用发票
     */
    public String getInvoiceType() {
        return this.invoiceType;
    }

    /**
     * 发票类型名称
     */
    public String getInvoiceTypeName() {
        return this.invoiceTypeName;
    }

    /**
     * 二维码原始内容
     */
    public String getRawContent() {
        return this.rawContent;
    }

    /**
     * 是否解析成功
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * 错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * 发票代码
     */
    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    /**
     * 发票号码
     */
    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    /**
     * 开票日期
     */
    public void setIssueDate(final String issueDate) {
        this.issueDate = issueDate;
    }

    /**
     * 校验码（后6位）
     */
    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
    }

    /**
     * 金额（不含税）
     */
    public void setAmountWithoutTax(final BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    /**
     * 价税合计
     */
    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 税额
     */
    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * 购买方名称
     */
    public void setBuyerName(final String buyerName) {
        this.buyerName = buyerName;
    }

    /**
     * 购买方税号
     */
    public void setBuyerTaxNo(final String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    /**
     * 销售方名称
     */
    public void setSellerName(final String sellerName) {
        this.sellerName = sellerName;
    }

    /**
     * 销售方税号
     */
    public void setSellerTaxNo(final String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    /**
     * 发票类型
     * 01:增值税专用发票
     * 04:增值税普通发票
     * 10:增值税电子普通发票
     * 11:增值税电子专用发票
     */
    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    /**
     * 发票类型名称
     */
    public void setInvoiceTypeName(final String invoiceTypeName) {
        this.invoiceTypeName = invoiceTypeName;
    }

    /**
     * 二维码原始内容
     */
    public void setRawContent(final String rawContent) {
        this.rawContent = rawContent;
    }

    /**
     * 是否解析成功
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvoiceQrInfoDTO)) return false;
        final InvoiceQrInfoDTO other = (InvoiceQrInfoDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isSuccess() != other.isSuccess()) return false;
        final java.lang.Object this$invoiceCode = this.getInvoiceCode();
        final java.lang.Object other$invoiceCode = other.getInvoiceCode();
        if (this$invoiceCode == null ? other$invoiceCode != null : !this$invoiceCode.equals(other$invoiceCode)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
        final java.lang.Object this$amountWithoutTax = this.getAmountWithoutTax();
        final java.lang.Object other$amountWithoutTax = other.getAmountWithoutTax();
        if (this$amountWithoutTax == null ? other$amountWithoutTax != null : !this$amountWithoutTax.equals(other$amountWithoutTax)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$buyerName = this.getBuyerName();
        final java.lang.Object other$buyerName = other.getBuyerName();
        if (this$buyerName == null ? other$buyerName != null : !this$buyerName.equals(other$buyerName)) return false;
        final java.lang.Object this$buyerTaxNo = this.getBuyerTaxNo();
        final java.lang.Object other$buyerTaxNo = other.getBuyerTaxNo();
        if (this$buyerTaxNo == null ? other$buyerTaxNo != null : !this$buyerTaxNo.equals(other$buyerTaxNo)) return false;
        final java.lang.Object this$sellerName = this.getSellerName();
        final java.lang.Object other$sellerName = other.getSellerName();
        if (this$sellerName == null ? other$sellerName != null : !this$sellerName.equals(other$sellerName)) return false;
        final java.lang.Object this$sellerTaxNo = this.getSellerTaxNo();
        final java.lang.Object other$sellerTaxNo = other.getSellerTaxNo();
        if (this$sellerTaxNo == null ? other$sellerTaxNo != null : !this$sellerTaxNo.equals(other$sellerTaxNo)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$invoiceTypeName = this.getInvoiceTypeName();
        final java.lang.Object other$invoiceTypeName = other.getInvoiceTypeName();
        if (this$invoiceTypeName == null ? other$invoiceTypeName != null : !this$invoiceTypeName.equals(other$invoiceTypeName)) return false;
        final java.lang.Object this$rawContent = this.getRawContent();
        final java.lang.Object other$rawContent = other.getRawContent();
        if (this$rawContent == null ? other$rawContent != null : !this$rawContent.equals(other$rawContent)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvoiceQrInfoDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSuccess() ? 79 : 97);
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
        final java.lang.Object $amountWithoutTax = this.getAmountWithoutTax();
        result = result * PRIME + ($amountWithoutTax == null ? 43 : $amountWithoutTax.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $buyerName = this.getBuyerName();
        result = result * PRIME + ($buyerName == null ? 43 : $buyerName.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $sellerName = this.getSellerName();
        result = result * PRIME + ($sellerName == null ? 43 : $sellerName.hashCode());
        final java.lang.Object $sellerTaxNo = this.getSellerTaxNo();
        result = result * PRIME + ($sellerTaxNo == null ? 43 : $sellerTaxNo.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $invoiceTypeName = this.getInvoiceTypeName();
        result = result * PRIME + ($invoiceTypeName == null ? 43 : $invoiceTypeName.hashCode());
        final java.lang.Object $rawContent = this.getRawContent();
        result = result * PRIME + ($rawContent == null ? 43 : $rawContent.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvoiceQrInfoDTO(invoiceCode=" + this.getInvoiceCode() + ", invoiceNo=" + this.getInvoiceNo() + ", issueDate=" + this.getIssueDate() + ", checkCode=" + this.getCheckCode() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", totalAmount=" + this.getTotalAmount() + ", taxAmount=" + this.getTaxAmount() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", sellerName=" + this.getSellerName() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", invoiceType=" + this.getInvoiceType() + ", invoiceTypeName=" + this.getInvoiceTypeName() + ", rawContent=" + this.getRawContent() + ", success=" + this.isSuccess() + ", errorMessage=" + this.getErrorMessage() + ")";
    }

    public InvoiceQrInfoDTO() {
    }

    /**
     * Creates a new {@code InvoiceQrInfoDTO} instance.
     *
     * @param invoiceCode 发票代码
     * @param invoiceNo 发票号码
     * @param issueDate 开票日期
     * @param checkCode 校验码（后6位）
     * @param amountWithoutTax 金额（不含税）
     * @param totalAmount 价税合计
     * @param taxAmount 税额
     * @param buyerName 购买方名称
     * @param buyerTaxNo 购买方税号
     * @param sellerName 销售方名称
     * @param sellerTaxNo 销售方税号
     * @param invoiceType 发票类型
     * 01:增值税专用发票
     * 04:增值税普通发票
     * 10:增值税电子普通发票
     * 11:增值税电子专用发票
     * @param invoiceTypeName 发票类型名称
     * @param rawContent 二维码原始内容
     * @param success 是否解析成功
     * @param errorMessage 错误信息
     */
    public InvoiceQrInfoDTO(final String invoiceCode, final String invoiceNo, final String issueDate, final String checkCode, final BigDecimal amountWithoutTax, final BigDecimal totalAmount, final BigDecimal taxAmount, final String buyerName, final String buyerTaxNo, final String sellerName, final String sellerTaxNo, final String invoiceType, final String invoiceTypeName, final String rawContent, final boolean success, final String errorMessage) {
        this.invoiceCode = invoiceCode;
        this.invoiceNo = invoiceNo;
        this.issueDate = issueDate;
        this.checkCode = checkCode;
        this.amountWithoutTax = amountWithoutTax;
        this.totalAmount = totalAmount;
        this.taxAmount = taxAmount;
        this.buyerName = buyerName;
        this.buyerTaxNo = buyerTaxNo;
        this.sellerName = sellerName;
        this.sellerTaxNo = sellerTaxNo;
        this.invoiceType = invoiceType;
        this.invoiceTypeName = invoiceTypeName;
        this.rawContent = rawContent;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
