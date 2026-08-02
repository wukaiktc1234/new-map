package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应付账款VO
 */
public class PayableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应付ID */
    private Long payableId;

    /** 应付编号 */
    private String payableNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 关联采购单ID */
    private Long purchaseOrderId;

    /** 关联采购入库单ID */
    private Long stockinId;

    /** 关联采购入库单号 */
    private String stockinNo;

    /** 关联采购订单号 */
    private String orderNo;

    /** 原始应付金额（单位：分） */
    private Long originalAmount;

    /** 原始应付金额（元，用于显示） */
    private String originalAmountDisplay;

    /** 已付金额（单位：分） */
    private Long paidAmount;

    /** 已付金额（元，用于显示） */
    private String paidAmountDisplay;

    /** 余额（单位：分） */
    private Long balanceAmount;

    /** 余额（元，用于显示） */
    private String balanceAmountDisplay;

    /** 到期日 */
    private LocalDate dueDate;

    /** 账期天数 */
    private Integer paymentTerm;

    /** 状态 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 关联原正向应付单ID（红字单使用） */
    private Long relatedInvoiceId;

    /** 单据类型：blue(蓝字正向) / red(红字退货) */
    private String invoiceType;

    /** 创建时间 */
    private java.time.LocalDateTime createTime;

    // getter和setter方法（简洁格式）
    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }
    public String getPayableNo() { return payableNo; }
    public void setPayableNo(String payableNo) { this.payableNo = payableNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public Long getStockinId() { return stockinId; }
    public void setStockinId(Long stockinId) { this.stockinId = stockinId; }
    public String getStockinNo() { return stockinNo; }
    public void setStockinNo(String stockinNo) { this.stockinNo = stockinNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Long originalAmount) { this.originalAmount = originalAmount; }
    public String getOriginalAmountDisplay() { return originalAmountDisplay; }
    public void setOriginalAmountDisplay(String originalAmountDisplay) { this.originalAmountDisplay = originalAmountDisplay; }
    public Long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
    public String getPaidAmountDisplay() { return paidAmountDisplay; }
    public void setPaidAmountDisplay(String paidAmountDisplay) { this.paidAmountDisplay = paidAmountDisplay; }
    public Long getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(Long balanceAmount) { this.balanceAmount = balanceAmount; }
    public String getBalanceAmountDisplay() { return balanceAmountDisplay; }
    public void setBalanceAmountDisplay(String balanceAmountDisplay) { this.balanceAmountDisplay = balanceAmountDisplay; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getPaymentTerm() { return paymentTerm; }
    public void setPaymentTerm(Integer paymentTerm) { this.paymentTerm = paymentTerm; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Long getRelatedInvoiceId() { return relatedInvoiceId; }
    public void setRelatedInvoiceId(Long relatedInvoiceId) { this.relatedInvoiceId = relatedInvoiceId; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}
