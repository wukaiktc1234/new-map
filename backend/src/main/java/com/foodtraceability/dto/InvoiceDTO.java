package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 发票DTO
 * @author example
 * @since 2025-12-05
 */
public class InvoiceDTO {
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 客户名称
     */
    private String customerName;
    /**
     * 客户类型：PERSON(个人)、COMPANY(企业)
     */
    private String customerType;
    /**
     * 纳税人识别号
     */
    private String taxpayerId;
    /**
     * 发票类型：VAT_GENERAL(增值税普通发票)、VAT_SPECIAL(增值税专用发票)、ELECTRONIC(电子发票)
     */
    private String invoiceType;
    /**
     * 商品名称
     */
    private String goodsName;
    /**
     * 商品数量
     */
    private Integer quantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 不含税金额
     */
    private BigDecimal taxExcludedAmount;
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    /**
     * 价税合计
     */
    private BigDecimal totalAmount;
    /**
     * 发票状态：DRAFT(草稿)、ISSUED(已开具)、VOIDED(已作废)、RED_ISSUED(已红冲)
     */
    private String invoiceStatus;
    /**
     * 备注
     */
    private String remark;

    public InvoiceDTO() {
    }

    /**
     * 订单ID
     */
    public Long getOrderId() {
        return this.orderId;
    }

    /**
     * 订单编号
     */
    public String getOrderNo() {
        return this.orderNo;
    }

    /**
     * 客户名称
     */
    public String getCustomerName() {
        return this.customerName;
    }

    /**
     * 客户类型：PERSON(个人)、COMPANY(企业)
     */
    public String getCustomerType() {
        return this.customerType;
    }

    /**
     * 纳税人识别号
     */
    public String getTaxpayerId() {
        return this.taxpayerId;
    }

    /**
     * 发票类型：VAT_GENERAL(增值税普通发票)、VAT_SPECIAL(增值税专用发票)、ELECTRONIC(电子发票)
     */
    public String getInvoiceType() {
        return this.invoiceType;
    }

    /**
     * 商品名称
     */
    public String getGoodsName() {
        return this.goodsName;
    }

    /**
     * 商品数量
     */
    public Integer getQuantity() {
        return this.quantity;
    }

    /**
     * 单价
     */
    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    /**
     * 税率
     */
    public BigDecimal getTaxRate() {
        return this.taxRate;
    }

    /**
     * 不含税金额
     */
    public BigDecimal getTaxExcludedAmount() {
        return this.taxExcludedAmount;
    }

    /**
     * 税额
     */
    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    /**
     * 价税合计
     */
    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    /**
     * 发票状态：DRAFT(草稿)、ISSUED(已开具)、VOIDED(已作废)、RED_ISSUED(已红冲)
     */
    public String getInvoiceStatus() {
        return this.invoiceStatus;
    }

    /**
     * 备注
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 订单ID
     */
    public void setOrderId(final Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 订单编号
     */
    public void setOrderNo(final String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 客户名称
     */
    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    /**
     * 客户类型：PERSON(个人)、COMPANY(企业)
     */
    public void setCustomerType(final String customerType) {
        this.customerType = customerType;
    }

    /**
     * 纳税人识别号
     */
    public void setTaxpayerId(final String taxpayerId) {
        this.taxpayerId = taxpayerId;
    }

    /**
     * 发票类型：VAT_GENERAL(增值税普通发票)、VAT_SPECIAL(增值税专用发票)、ELECTRONIC(电子发票)
     */
    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    /**
     * 商品名称
     */
    public void setGoodsName(final String goodsName) {
        this.goodsName = goodsName;
    }

    /**
     * 商品数量
     */
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 单价
     */
    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * 税率
     */
    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * 不含税金额
     */
    public void setTaxExcludedAmount(final BigDecimal taxExcludedAmount) {
        this.taxExcludedAmount = taxExcludedAmount;
    }

    /**
     * 税额
     */
    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * 价税合计
     */
    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 发票状态：DRAFT(草稿)、ISSUED(已开具)、VOIDED(已作废)、RED_ISSUED(已红冲)
     */
    public void setInvoiceStatus(final String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    /**
     * 备注
     */
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvoiceDTO)) return false;
        final InvoiceDTO other = (InvoiceDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$orderNo = this.getOrderNo();
        final java.lang.Object other$orderNo = other.getOrderNo();
        if (this$orderNo == null ? other$orderNo != null : !this$orderNo.equals(other$orderNo)) return false;
        final java.lang.Object this$customerName = this.getCustomerName();
        final java.lang.Object other$customerName = other.getCustomerName();
        if (this$customerName == null ? other$customerName != null : !this$customerName.equals(other$customerName)) return false;
        final java.lang.Object this$customerType = this.getCustomerType();
        final java.lang.Object other$customerType = other.getCustomerType();
        if (this$customerType == null ? other$customerType != null : !this$customerType.equals(other$customerType)) return false;
        final java.lang.Object this$taxpayerId = this.getTaxpayerId();
        final java.lang.Object other$taxpayerId = other.getTaxpayerId();
        if (this$taxpayerId == null ? other$taxpayerId != null : !this$taxpayerId.equals(other$taxpayerId)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$goodsName = this.getGoodsName();
        final java.lang.Object other$goodsName = other.getGoodsName();
        if (this$goodsName == null ? other$goodsName != null : !this$goodsName.equals(other$goodsName)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$taxRate = this.getTaxRate();
        final java.lang.Object other$taxRate = other.getTaxRate();
        if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
        final java.lang.Object this$taxExcludedAmount = this.getTaxExcludedAmount();
        final java.lang.Object other$taxExcludedAmount = other.getTaxExcludedAmount();
        if (this$taxExcludedAmount == null ? other$taxExcludedAmount != null : !this$taxExcludedAmount.equals(other$taxExcludedAmount)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$invoiceStatus = this.getInvoiceStatus();
        final java.lang.Object other$invoiceStatus = other.getInvoiceStatus();
        if (this$invoiceStatus == null ? other$invoiceStatus != null : !this$invoiceStatus.equals(other$invoiceStatus)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvoiceDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $orderNo = this.getOrderNo();
        result = result * PRIME + ($orderNo == null ? 43 : $orderNo.hashCode());
        final java.lang.Object $customerName = this.getCustomerName();
        result = result * PRIME + ($customerName == null ? 43 : $customerName.hashCode());
        final java.lang.Object $customerType = this.getCustomerType();
        result = result * PRIME + ($customerType == null ? 43 : $customerType.hashCode());
        final java.lang.Object $taxpayerId = this.getTaxpayerId();
        result = result * PRIME + ($taxpayerId == null ? 43 : $taxpayerId.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $goodsName = this.getGoodsName();
        result = result * PRIME + ($goodsName == null ? 43 : $goodsName.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $taxRate = this.getTaxRate();
        result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
        final java.lang.Object $taxExcludedAmount = this.getTaxExcludedAmount();
        result = result * PRIME + ($taxExcludedAmount == null ? 43 : $taxExcludedAmount.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $invoiceStatus = this.getInvoiceStatus();
        result = result * PRIME + ($invoiceStatus == null ? 43 : $invoiceStatus.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvoiceDTO(orderId=" + this.getOrderId() + ", orderNo=" + this.getOrderNo() + ", customerName=" + this.getCustomerName() + ", customerType=" + this.getCustomerType() + ", taxpayerId=" + this.getTaxpayerId() + ", invoiceType=" + this.getInvoiceType() + ", goodsName=" + this.getGoodsName() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", taxRate=" + this.getTaxRate() + ", taxExcludedAmount=" + this.getTaxExcludedAmount() + ", taxAmount=" + this.getTaxAmount() + ", totalAmount=" + this.getTotalAmount() + ", invoiceStatus=" + this.getInvoiceStatus() + ", remark=" + this.getRemark() + ")";
    }
}
