package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 手动输入发票信息DTO
 * 
 * 用于OCR识别失败或用户手动输入发票信息
 */
@Schema(description = "手动输入发票信息")
public class ManualInvoiceInputDTO {
    @NotBlank(message = "发票类型不能为空")
    @Pattern(regexp = "^(invoice|vat_special|vat_general|full_electronic)$", message = "发票类型不正确，必须是invoice、vat_special、vat_general或full_electronic")
    @Schema(description = "发票类型: invoice-全电发票, vat_special-增值税专用发票, vat_general-增值税普通发票", required = true)
    private String invoiceType;
    @NotNull(message = "开票日期不能为空")
    @PastOrPresent(message = "开票日期不能晚于当前日期")
    @Schema(description = "开票日期", required = true)
    private LocalDate issueDate;
    @Pattern(regexp = "^[0-9A-Z]{0,20}$", message = "发票代码格式不正确")
    @Schema(description = "发票代码")
    private String invoiceCode;
    @NotBlank(message = "发票号码不能为空")
    @Pattern(regexp = "^[0-9A-Z]{8,20}$", message = "发票号码格式不正确，应为8-20位数字或大写字母")
    @Schema(description = "发票号码", required = true)
    private String invoiceNo;
    @Pattern(regexp = "^[0-9A-Z]{0,20}$", message = "校验码格式不正确")
    @Schema(description = "校验码(后6位)")
    private String checkCode;
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    @Digits(integer = 15, fraction = 2, message = "金额格式不正确，最多15位整数2位小数")
    @Schema(description = "价税合计(总金额)", required = true)
    private BigDecimal totalAmount;
    @Positive(message = "不含税金额必须大于0")
    @Digits(integer = 15, fraction = 2, message = "不含税金额格式不正确")
    @Schema(description = "不含税金额")
    private BigDecimal amountWithoutTax;
    @PositiveOrZero(message = "税额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "税额格式不正确")
    @Schema(description = "税额")
    private BigDecimal taxAmount;
    @Valid
    @Schema(description = "购买方信息")
    private PartyInfo buyer;
    @Valid
    @Schema(description = "销售方信息")
    private PartyInfo seller;
    @Valid
    @Size(max = 50, message = "明细条目不能超过50条")
    @Schema(description = "货物/服务明细")
    private List<InvoiceItemInput> items;
    @Size(max = 500, message = "备注长度不能超过500字符")
    @Schema(description = "备注")
    private String remark;
    @Size(max = 50, message = "收款人长度不能超过50字符")
    @Schema(description = "收款人")
    private String payee;
    @Size(max = 50, message = "复核人长度不能超过50字符")
    @Schema(description = "复核人")
    private String reviewer;
    @Size(max = 200, message = "原始文件名长度不能超过200字符")
    @Schema(description = "原始文件名")
    private String originalFileName;
    @Size(max = 200, message = "手动输入原因长度不能超过200字符")
    @Schema(description = "手动输入原因")
    private String manualInputReason;


    /**
     * 购买方/销售方信息
     */
    public static class PartyInfo {
        @Size(max = 100, message = "名称长度不能超过100字符")
        @Schema(description = "名称")
        private String name;
        @Pattern(regexp = "^[0-9A-Z]{0,20}$", message = "纳税人识别号格式不正确")
        @Schema(description = "纳税人识别号")
        private String taxNo;
        @Size(max = 200, message = "地址电话长度不能超过200字符")
        @Schema(description = "地址电话")
        private String addressPhone;
        @Size(max = 200, message = "开户行及账号长度不能超过200字符")
        @Schema(description = "开户行及账号")
        private String bankAccount;


        public static class PartyInfoBuilder {
            private String name;
            private String taxNo;
            private String addressPhone;
            private String bankAccount;

            PartyInfoBuilder() {
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder name(final String name) {
                this.name = name;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder taxNo(final String taxNo) {
                this.taxNo = taxNo;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder addressPhone(final String addressPhone) {
                this.addressPhone = addressPhone;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder bankAccount(final String bankAccount) {
                this.bankAccount = bankAccount;
                return this;
            }

            public ManualInvoiceInputDTO.PartyInfo build() {
                return new ManualInvoiceInputDTO.PartyInfo(this.name, this.taxNo, this.addressPhone, this.bankAccount);
            }

            @java.lang.Override
            public java.lang.String toString() {
                return "ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder(name=" + this.name + ", taxNo=" + this.taxNo + ", addressPhone=" + this.addressPhone + ", bankAccount=" + this.bankAccount + ")";
            }
        }

        public static ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder builder() {
            return new ManualInvoiceInputDTO.PartyInfo.PartyInfoBuilder();
        }

        public String getName() {
            return this.name;
        }

        public String getTaxNo() {
            return this.taxNo;
        }

        public String getAddressPhone() {
            return this.addressPhone;
        }

        public String getBankAccount() {
            return this.bankAccount;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setTaxNo(final String taxNo) {
            this.taxNo = taxNo;
        }

        public void setAddressPhone(final String addressPhone) {
            this.addressPhone = addressPhone;
        }

        public void setBankAccount(final String bankAccount) {
            this.bankAccount = bankAccount;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof ManualInvoiceInputDTO.PartyInfo)) return false;
            final ManualInvoiceInputDTO.PartyInfo other = (ManualInvoiceInputDTO.PartyInfo) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final java.lang.Object this$taxNo = this.getTaxNo();
            final java.lang.Object other$taxNo = other.getTaxNo();
            if (this$taxNo == null ? other$taxNo != null : !this$taxNo.equals(other$taxNo)) return false;
            final java.lang.Object this$addressPhone = this.getAddressPhone();
            final java.lang.Object other$addressPhone = other.getAddressPhone();
            if (this$addressPhone == null ? other$addressPhone != null : !this$addressPhone.equals(other$addressPhone)) return false;
            final java.lang.Object this$bankAccount = this.getBankAccount();
            final java.lang.Object other$bankAccount = other.getBankAccount();
            if (this$bankAccount == null ? other$bankAccount != null : !this$bankAccount.equals(other$bankAccount)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ManualInvoiceInputDTO.PartyInfo;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $taxNo = this.getTaxNo();
            result = result * PRIME + ($taxNo == null ? 43 : $taxNo.hashCode());
            final java.lang.Object $addressPhone = this.getAddressPhone();
            result = result * PRIME + ($addressPhone == null ? 43 : $addressPhone.hashCode());
            final java.lang.Object $bankAccount = this.getBankAccount();
            result = result * PRIME + ($bankAccount == null ? 43 : $bankAccount.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "ManualInvoiceInputDTO.PartyInfo(name=" + this.getName() + ", taxNo=" + this.getTaxNo() + ", addressPhone=" + this.getAddressPhone() + ", bankAccount=" + this.getBankAccount() + ")";
        }

        public PartyInfo() {
        }

        public PartyInfo(final String name, final String taxNo, final String addressPhone, final String bankAccount) {
            this.name = name;
            this.taxNo = taxNo;
            this.addressPhone = addressPhone;
            this.bankAccount = bankAccount;
        }
    }


    /**
     * 货物/服务明细
     */
    public static class InvoiceItemInput {
        @Size(max = 100, message = "货物/服务名称长度不能超过100字符")
        @Schema(description = "货物/服务名称")
        private String name;
        @Size(max = 50, message = "规格型号长度不能超过50字符")
        @Schema(description = "规格型号")
        private String specification;
        @Size(max = 20, message = "单位长度不能超过20字符")
        @Schema(description = "单位")
        private String unit;
        @Positive(message = "数量必须大于0")
        @Digits(integer = 10, fraction = 4, message = "数量格式不正确")
        @Schema(description = "数量")
        private BigDecimal quantity;
        @Positive(message = "单价必须大于0")
        @Digits(integer = 15, fraction = 4, message = "单价格式不正确")
        @Schema(description = "单价")
        private BigDecimal unitPrice;
        @Positive(message = "金额必须大于0")
        @Digits(integer = 15, fraction = 2, message = "金额格式不正确")
        @Schema(description = "金额")
        private BigDecimal amount;
        @Positive(message = "税率必须大于0")
        @Digits(integer = 3, fraction = 2, message = "税率格式不正确")
        @Schema(description = "税率")
        private BigDecimal taxRate;
        @PositiveOrZero(message = "税额不能为负数")
        @Digits(integer = 15, fraction = 2, message = "税额格式不正确")
        @Schema(description = "税额")
        private BigDecimal taxAmount;
        @Size(max = 30, message = "税收分类编码长度不能超过30字符")
        @Schema(description = "税收分类编码")
        private String taxClassificationCode;


        public static class InvoiceItemInputBuilder {
            private String name;
            private String specification;
            private String unit;
            private BigDecimal quantity;
            private BigDecimal unitPrice;
            private BigDecimal amount;
            private BigDecimal taxRate;
            private BigDecimal taxAmount;
            private String taxClassificationCode;

            InvoiceItemInputBuilder() {
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder name(final String name) {
                this.name = name;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder specification(final String specification) {
                this.specification = specification;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder unit(final String unit) {
                this.unit = unit;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder quantity(final BigDecimal quantity) {
                this.quantity = quantity;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder unitPrice(final BigDecimal unitPrice) {
                this.unitPrice = unitPrice;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder amount(final BigDecimal amount) {
                this.amount = amount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder taxRate(final BigDecimal taxRate) {
                this.taxRate = taxRate;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder taxAmount(final BigDecimal taxAmount) {
                this.taxAmount = taxAmount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            public ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder taxClassificationCode(final String taxClassificationCode) {
                this.taxClassificationCode = taxClassificationCode;
                return this;
            }

            public ManualInvoiceInputDTO.InvoiceItemInput build() {
                return new ManualInvoiceInputDTO.InvoiceItemInput(this.name, this.specification, this.unit, this.quantity, this.unitPrice, this.amount, this.taxRate, this.taxAmount, this.taxClassificationCode);
            }

            @java.lang.Override
            public java.lang.String toString() {
                return "ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder(name=" + this.name + ", specification=" + this.specification + ", unit=" + this.unit + ", quantity=" + this.quantity + ", unitPrice=" + this.unitPrice + ", amount=" + this.amount + ", taxRate=" + this.taxRate + ", taxAmount=" + this.taxAmount + ", taxClassificationCode=" + this.taxClassificationCode + ")";
            }
        }

        public static ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder builder() {
            return new ManualInvoiceInputDTO.InvoiceItemInput.InvoiceItemInputBuilder();
        }

        public String getName() {
            return this.name;
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

        public String getTaxClassificationCode() {
            return this.taxClassificationCode;
        }

        public void setName(final String name) {
            this.name = name;
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

        public void setTaxClassificationCode(final String taxClassificationCode) {
            this.taxClassificationCode = taxClassificationCode;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof ManualInvoiceInputDTO.InvoiceItemInput)) return false;
            final ManualInvoiceInputDTO.InvoiceItemInput other = (ManualInvoiceInputDTO.InvoiceItemInput) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
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
            final java.lang.Object this$taxClassificationCode = this.getTaxClassificationCode();
            final java.lang.Object other$taxClassificationCode = other.getTaxClassificationCode();
            if (this$taxClassificationCode == null ? other$taxClassificationCode != null : !this$taxClassificationCode.equals(other$taxClassificationCode)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ManualInvoiceInputDTO.InvoiceItemInput;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
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
            final java.lang.Object $taxClassificationCode = this.getTaxClassificationCode();
            result = result * PRIME + ($taxClassificationCode == null ? 43 : $taxClassificationCode.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "ManualInvoiceInputDTO.InvoiceItemInput(name=" + this.getName() + ", specification=" + this.getSpecification() + ", unit=" + this.getUnit() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", amount=" + this.getAmount() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", taxClassificationCode=" + this.getTaxClassificationCode() + ")";
        }

        public InvoiceItemInput() {
        }

        public InvoiceItemInput(final String name, final String specification, final String unit, final BigDecimal quantity, final BigDecimal unitPrice, final BigDecimal amount, final BigDecimal taxRate, final BigDecimal taxAmount, final String taxClassificationCode) {
            this.name = name;
            this.specification = specification;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.taxRate = taxRate;
            this.taxAmount = taxAmount;
            this.taxClassificationCode = taxClassificationCode;
        }
    }


    public static class ManualInvoiceInputDTOBuilder {
        private String invoiceType;
        private LocalDate issueDate;
        private String invoiceCode;
        private String invoiceNo;
        private String checkCode;
        private BigDecimal totalAmount;
        private BigDecimal amountWithoutTax;
        private BigDecimal taxAmount;
        private PartyInfo buyer;
        private PartyInfo seller;
        private List<InvoiceItemInput> items;
        private String remark;
        private String payee;
        private String reviewer;
        private String originalFileName;
        private String manualInputReason;

        ManualInvoiceInputDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder invoiceType(final String invoiceType) {
            this.invoiceType = invoiceType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder issueDate(final LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder invoiceCode(final String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder invoiceNo(final String invoiceNo) {
            this.invoiceNo = invoiceNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder checkCode(final String checkCode) {
            this.checkCode = checkCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder totalAmount(final BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder amountWithoutTax(final BigDecimal amountWithoutTax) {
            this.amountWithoutTax = amountWithoutTax;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder taxAmount(final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder buyer(final PartyInfo buyer) {
            this.buyer = buyer;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder seller(final PartyInfo seller) {
            this.seller = seller;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder items(final List<InvoiceItemInput> items) {
            this.items = items;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder payee(final String payee) {
            this.payee = payee;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder reviewer(final String reviewer) {
            this.reviewer = reviewer;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder originalFileName(final String originalFileName) {
            this.originalFileName = originalFileName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder manualInputReason(final String manualInputReason) {
            this.manualInputReason = manualInputReason;
            return this;
        }

        public ManualInvoiceInputDTO build() {
            return new ManualInvoiceInputDTO(this.invoiceType, this.issueDate, this.invoiceCode, this.invoiceNo, this.checkCode, this.totalAmount, this.amountWithoutTax, this.taxAmount, this.buyer, this.seller, this.items, this.remark, this.payee, this.reviewer, this.originalFileName, this.manualInputReason);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder(invoiceType=" + this.invoiceType + ", issueDate=" + this.issueDate + ", invoiceCode=" + this.invoiceCode + ", invoiceNo=" + this.invoiceNo + ", checkCode=" + this.checkCode + ", totalAmount=" + this.totalAmount + ", amountWithoutTax=" + this.amountWithoutTax + ", taxAmount=" + this.taxAmount + ", buyer=" + this.buyer + ", seller=" + this.seller + ", items=" + this.items + ", remark=" + this.remark + ", payee=" + this.payee + ", reviewer=" + this.reviewer + ", originalFileName=" + this.originalFileName + ", manualInputReason=" + this.manualInputReason + ")";
        }
    }

    public static ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder builder() {
        return new ManualInvoiceInputDTO.ManualInvoiceInputDTOBuilder();
    }

    public String getInvoiceType() {
        return this.invoiceType;
    }

    public LocalDate getIssueDate() {
        return this.issueDate;
    }

    public String getInvoiceCode() {
        return this.invoiceCode;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public String getCheckCode() {
        return this.checkCode;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public BigDecimal getAmountWithoutTax() {
        return this.amountWithoutTax;
    }

    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    public PartyInfo getBuyer() {
        return this.buyer;
    }

    public PartyInfo getSeller() {
        return this.seller;
    }

    public List<InvoiceItemInput> getItems() {
        return this.items;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getPayee() {
        return this.payee;
    }

    public String getReviewer() {
        return this.reviewer;
    }

    public String getOriginalFileName() {
        return this.originalFileName;
    }

    public String getManualInputReason() {
        return this.manualInputReason;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setAmountWithoutTax(final BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setBuyer(final PartyInfo buyer) {
        this.buyer = buyer;
    }

    public void setSeller(final PartyInfo seller) {
        this.seller = seller;
    }

    public void setItems(final List<InvoiceItemInput> items) {
        this.items = items;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setPayee(final String payee) {
        this.payee = payee;
    }

    public void setReviewer(final String reviewer) {
        this.reviewer = reviewer;
    }

    public void setOriginalFileName(final String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setManualInputReason(final String manualInputReason) {
        this.manualInputReason = manualInputReason;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ManualInvoiceInputDTO)) return false;
        final ManualInvoiceInputDTO other = (ManualInvoiceInputDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$invoiceCode = this.getInvoiceCode();
        final java.lang.Object other$invoiceCode = other.getInvoiceCode();
        if (this$invoiceCode == null ? other$invoiceCode != null : !this$invoiceCode.equals(other$invoiceCode)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$amountWithoutTax = this.getAmountWithoutTax();
        final java.lang.Object other$amountWithoutTax = other.getAmountWithoutTax();
        if (this$amountWithoutTax == null ? other$amountWithoutTax != null : !this$amountWithoutTax.equals(other$amountWithoutTax)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$buyer = this.getBuyer();
        final java.lang.Object other$buyer = other.getBuyer();
        if (this$buyer == null ? other$buyer != null : !this$buyer.equals(other$buyer)) return false;
        final java.lang.Object this$seller = this.getSeller();
        final java.lang.Object other$seller = other.getSeller();
        if (this$seller == null ? other$seller != null : !this$seller.equals(other$seller)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$payee = this.getPayee();
        final java.lang.Object other$payee = other.getPayee();
        if (this$payee == null ? other$payee != null : !this$payee.equals(other$payee)) return false;
        final java.lang.Object this$reviewer = this.getReviewer();
        final java.lang.Object other$reviewer = other.getReviewer();
        if (this$reviewer == null ? other$reviewer != null : !this$reviewer.equals(other$reviewer)) return false;
        final java.lang.Object this$originalFileName = this.getOriginalFileName();
        final java.lang.Object other$originalFileName = other.getOriginalFileName();
        if (this$originalFileName == null ? other$originalFileName != null : !this$originalFileName.equals(other$originalFileName)) return false;
        final java.lang.Object this$manualInputReason = this.getManualInputReason();
        final java.lang.Object other$manualInputReason = other.getManualInputReason();
        if (this$manualInputReason == null ? other$manualInputReason != null : !this$manualInputReason.equals(other$manualInputReason)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ManualInvoiceInputDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $amountWithoutTax = this.getAmountWithoutTax();
        result = result * PRIME + ($amountWithoutTax == null ? 43 : $amountWithoutTax.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $buyer = this.getBuyer();
        result = result * PRIME + ($buyer == null ? 43 : $buyer.hashCode());
        final java.lang.Object $seller = this.getSeller();
        result = result * PRIME + ($seller == null ? 43 : $seller.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $payee = this.getPayee();
        result = result * PRIME + ($payee == null ? 43 : $payee.hashCode());
        final java.lang.Object $reviewer = this.getReviewer();
        result = result * PRIME + ($reviewer == null ? 43 : $reviewer.hashCode());
        final java.lang.Object $originalFileName = this.getOriginalFileName();
        result = result * PRIME + ($originalFileName == null ? 43 : $originalFileName.hashCode());
        final java.lang.Object $manualInputReason = this.getManualInputReason();
        result = result * PRIME + ($manualInputReason == null ? 43 : $manualInputReason.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ManualInvoiceInputDTO(invoiceType=" + this.getInvoiceType() + ", issueDate=" + this.getIssueDate() + ", invoiceCode=" + this.getInvoiceCode() + ", invoiceNo=" + this.getInvoiceNo() + ", checkCode=" + this.getCheckCode() + ", totalAmount=" + this.getTotalAmount() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", taxAmount=" + this.getTaxAmount() + ", buyer=" + this.getBuyer() + ", seller=" + this.getSeller() + ", items=" + this.getItems() + ", remark=" + this.getRemark() + ", payee=" + this.getPayee() + ", reviewer=" + this.getReviewer() + ", originalFileName=" + this.getOriginalFileName() + ", manualInputReason=" + this.getManualInputReason() + ")";
    }

    public ManualInvoiceInputDTO() {
    }

    public ManualInvoiceInputDTO(final String invoiceType, final LocalDate issueDate, final String invoiceCode, final String invoiceNo, final String checkCode, final BigDecimal totalAmount, final BigDecimal amountWithoutTax, final BigDecimal taxAmount, final PartyInfo buyer, final PartyInfo seller, final List<InvoiceItemInput> items, final String remark, final String payee, final String reviewer, final String originalFileName, final String manualInputReason) {
        this.invoiceType = invoiceType;
        this.issueDate = issueDate;
        this.invoiceCode = invoiceCode;
        this.invoiceNo = invoiceNo;
        this.checkCode = checkCode;
        this.totalAmount = totalAmount;
        this.amountWithoutTax = amountWithoutTax;
        this.taxAmount = taxAmount;
        this.buyer = buyer;
        this.seller = seller;
        this.items = items;
        this.remark = remark;
        this.payee = payee;
        this.reviewer = reviewer;
        this.originalFileName = originalFileName;
        this.manualInputReason = manualInputReason;
    }
}
