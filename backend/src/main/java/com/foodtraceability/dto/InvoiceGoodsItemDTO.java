package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 发票商品明细DTO
 */
public class InvoiceGoodsItemDTO {
    /**
     * 行号
     */
    private Integer itemNo;
    /**
     * 货物或应税劳务、服务名称
     */
    private String goodsName;
    /**
     * 规格型号
     */
    private String specification;
    /**
     * 单位
     */
    private String unit;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 金额（不含税）
     */
    private BigDecimal amount;
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    /**
     * 是否为折扣行
     */
    private boolean discount;
    /**
     * 商品编码（税收分类编码）
     */
    private String goodsCode;


    public static class InvoiceGoodsItemDTOBuilder {
        private Integer itemNo;
        private String goodsName;
        private String specification;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private boolean discount;
        private String goodsCode;

        InvoiceGoodsItemDTOBuilder() {
        }

        /**
         * 行号
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder itemNo(final Integer itemNo) {
            this.itemNo = itemNo;
            return this;
        }

        /**
         * 货物或应税劳务、服务名称
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder goodsName(final String goodsName) {
            this.goodsName = goodsName;
            return this;
        }

        /**
         * 规格型号
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder specification(final String specification) {
            this.specification = specification;
            return this;
        }

        /**
         * 单位
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder unit(final String unit) {
            this.unit = unit;
            return this;
        }

        /**
         * 数量
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder quantity(final BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        /**
         * 单价
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder unitPrice(final BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        /**
         * 金额（不含税）
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder amount(final BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        /**
         * 税率
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder taxRate(final BigDecimal taxRate) {
            this.taxRate = taxRate;
            return this;
        }

        /**
         * 税额
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder taxAmount(final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        /**
         * 是否为折扣行
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder discount(final boolean discount) {
            this.discount = discount;
            return this;
        }

        /**
         * 商品编码（税收分类编码）
         * @return {@code this}.
         */
        public InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder goodsCode(final String goodsCode) {
            this.goodsCode = goodsCode;
            return this;
        }

        public InvoiceGoodsItemDTO build() {
            return new InvoiceGoodsItemDTO(this.itemNo, this.goodsName, this.specification, this.unit, this.quantity, this.unitPrice, this.amount, this.taxRate, this.taxAmount, this.discount, this.goodsCode);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder(itemNo=" + this.itemNo + ", goodsName=" + this.goodsName + ", specification=" + this.specification + ", unit=" + this.unit + ", quantity=" + this.quantity + ", unitPrice=" + this.unitPrice + ", amount=" + this.amount + ", taxRate=" + this.taxRate + ", taxAmount=" + this.taxAmount + ", discount=" + this.discount + ", goodsCode=" + this.goodsCode + ")";
        }
    }

    public static InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder builder() {
        return new InvoiceGoodsItemDTO.InvoiceGoodsItemDTOBuilder();
    }

    /**
     * 行号
     */
    public Integer getItemNo() {
        return this.itemNo;
    }

    /**
     * 货物或应税劳务、服务名称
     */
    public String getGoodsName() {
        return this.goodsName;
    }

    /**
     * 规格型号
     */
    public String getSpecification() {
        return this.specification;
    }

    /**
     * 单位
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * 数量
     */
    public BigDecimal getQuantity() {
        return this.quantity;
    }

    /**
     * 单价
     */
    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    /**
     * 金额（不含税）
     */
    public BigDecimal getAmount() {
        return this.amount;
    }

    /**
     * 税率
     */
    public BigDecimal getTaxRate() {
        return this.taxRate;
    }

    /**
     * 税额
     */
    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    /**
     * 是否为折扣行
     */
    public boolean isDiscount() {
        return this.discount;
    }

    /**
     * 商品编码（税收分类编码）
     */
    public String getGoodsCode() {
        return this.goodsCode;
    }

    /**
     * 行号
     */
    public void setItemNo(final Integer itemNo) {
        this.itemNo = itemNo;
    }

    /**
     * 货物或应税劳务、服务名称
     */
    public void setGoodsName(final String goodsName) {
        this.goodsName = goodsName;
    }

    /**
     * 规格型号
     */
    public void setSpecification(final String specification) {
        this.specification = specification;
    }

    /**
     * 单位
     */
    public void setUnit(final String unit) {
        this.unit = unit;
    }

    /**
     * 数量
     */
    public void setQuantity(final BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * 单价
     */
    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * 金额（不含税）
     */
    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 税率
     */
    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * 税额
     */
    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * 是否为折扣行
     */
    public void setDiscount(final boolean discount) {
        this.discount = discount;
    }

    /**
     * 商品编码（税收分类编码）
     */
    public void setGoodsCode(final String goodsCode) {
        this.goodsCode = goodsCode;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvoiceGoodsItemDTO)) return false;
        final InvoiceGoodsItemDTO other = (InvoiceGoodsItemDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isDiscount() != other.isDiscount()) return false;
        final java.lang.Object this$itemNo = this.getItemNo();
        final java.lang.Object other$itemNo = other.getItemNo();
        if (this$itemNo == null ? other$itemNo != null : !this$itemNo.equals(other$itemNo)) return false;
        final java.lang.Object this$goodsName = this.getGoodsName();
        final java.lang.Object other$goodsName = other.getGoodsName();
        if (this$goodsName == null ? other$goodsName != null : !this$goodsName.equals(other$goodsName)) return false;
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
        final java.lang.Object this$goodsCode = this.getGoodsCode();
        final java.lang.Object other$goodsCode = other.getGoodsCode();
        if (this$goodsCode == null ? other$goodsCode != null : !this$goodsCode.equals(other$goodsCode)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvoiceGoodsItemDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isDiscount() ? 79 : 97);
        final java.lang.Object $itemNo = this.getItemNo();
        result = result * PRIME + ($itemNo == null ? 43 : $itemNo.hashCode());
        final java.lang.Object $goodsName = this.getGoodsName();
        result = result * PRIME + ($goodsName == null ? 43 : $goodsName.hashCode());
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
        final java.lang.Object $goodsCode = this.getGoodsCode();
        result = result * PRIME + ($goodsCode == null ? 43 : $goodsCode.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvoiceGoodsItemDTO(itemNo=" + this.getItemNo() + ", goodsName=" + this.getGoodsName() + ", specification=" + this.getSpecification() + ", unit=" + this.getUnit() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", amount=" + this.getAmount() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", discount=" + this.isDiscount() + ", goodsCode=" + this.getGoodsCode() + ")";
    }

    public InvoiceGoodsItemDTO() {
    }

    /**
     * Creates a new {@code InvoiceGoodsItemDTO} instance.
     *
     * @param itemNo 行号
     * @param goodsName 货物或应税劳务、服务名称
     * @param specification 规格型号
     * @param unit 单位
     * @param quantity 数量
     * @param unitPrice 单价
     * @param amount 金额（不含税）
     * @param taxRate 税率
     * @param taxAmount 税额
     * @param discount 是否为折扣行
     * @param goodsCode 商品编码（税收分类编码）
     */
    public InvoiceGoodsItemDTO(final Integer itemNo, final String goodsName, final String specification, final String unit, final BigDecimal quantity, final BigDecimal unitPrice, final BigDecimal amount, final BigDecimal taxRate, final BigDecimal taxAmount, final boolean discount, final String goodsCode) {
        this.itemNo = itemNo;
        this.goodsName = goodsName;
        this.specification = specification;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.amount = amount;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.discount = discount;
        this.goodsCode = goodsCode;
    }
}
