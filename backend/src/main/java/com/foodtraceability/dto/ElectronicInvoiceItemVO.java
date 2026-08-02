package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 发票明细行项目VO
 */
@Schema(description = "发票明细行项目VO")
public class ElectronicInvoiceItemVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "行号")
    private Integer itemNo;
    @Schema(description = "商品编码")
    private String goodsCode;
    @Schema(description = "商品名称")
    private String goodsName;
    @Schema(description = "规格型号")
    private String specification;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "数量")
    private BigDecimal quantity;
    @Schema(description = "单价")
    private BigDecimal unitPrice;
    @Schema(description = "金额")
    private BigDecimal amount;
    @Schema(description = "税率")
    private BigDecimal taxRate;
    @Schema(description = "税额")
    private BigDecimal taxAmount;
    @Schema(description = "折扣金额")
    private BigDecimal discountAmount;
    @Schema(description = "备注")
    private String remark;

    public ElectronicInvoiceItemVO() {
    }

    public Long getId() {
        return this.id;
    }

    public Integer getItemNo() {
        return this.itemNo;
    }

    public String getGoodsCode() {
        return this.goodsCode;
    }

    public String getGoodsName() {
        return this.goodsName;
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

    public BigDecimal getDiscountAmount() {
        return this.discountAmount;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setItemNo(final Integer itemNo) {
        this.itemNo = itemNo;
    }

    public void setGoodsCode(final String goodsCode) {
        this.goodsCode = goodsCode;
    }

    public void setGoodsName(final String goodsName) {
        this.goodsName = goodsName;
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

    public void setDiscountAmount(final BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicInvoiceItemVO)) return false;
        final ElectronicInvoiceItemVO other = (ElectronicInvoiceItemVO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$itemNo = this.getItemNo();
        final java.lang.Object other$itemNo = other.getItemNo();
        if (this$itemNo == null ? other$itemNo != null : !this$itemNo.equals(other$itemNo)) return false;
        final java.lang.Object this$goodsCode = this.getGoodsCode();
        final java.lang.Object other$goodsCode = other.getGoodsCode();
        if (this$goodsCode == null ? other$goodsCode != null : !this$goodsCode.equals(other$goodsCode)) return false;
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
        final java.lang.Object this$discountAmount = this.getDiscountAmount();
        final java.lang.Object other$discountAmount = other.getDiscountAmount();
        if (this$discountAmount == null ? other$discountAmount != null : !this$discountAmount.equals(other$discountAmount)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicInvoiceItemVO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $itemNo = this.getItemNo();
        result = result * PRIME + ($itemNo == null ? 43 : $itemNo.hashCode());
        final java.lang.Object $goodsCode = this.getGoodsCode();
        result = result * PRIME + ($goodsCode == null ? 43 : $goodsCode.hashCode());
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
        final java.lang.Object $discountAmount = this.getDiscountAmount();
        result = result * PRIME + ($discountAmount == null ? 43 : $discountAmount.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicInvoiceItemVO(id=" + this.getId() + ", itemNo=" + this.getItemNo() + ", goodsCode=" + this.getGoodsCode() + ", goodsName=" + this.getGoodsName() + ", specification=" + this.getSpecification() + ", unit=" + this.getUnit() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", amount=" + this.getAmount() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", discountAmount=" + this.getDiscountAmount() + ", remark=" + this.getRemark() + ")";
    }
}
