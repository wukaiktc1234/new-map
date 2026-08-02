package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款单展示VO
 * 金额单位：分（前端通过DataConverter转换为元）
 */
@Schema(description = "付款单展示对象")
public class PaymentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "付款单ID")
    private Long paymentId;

    @Schema(description = "付款单号")
    private String paymentNo;

    @Schema(description = "关联应付账款ID")
    private Long payableId;

    @Schema(description = "关联应付编号")
    private String payableNo;

    @Schema(description = "关联采购入库单ID")
    private Long stockinId;

    @Schema(description = "关联采购入库单号")
    private String stockinNo;

    @Schema(description = "关联采购订单号")
    private String orderNo;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "付款金额（分）")
    private Long paymentAmount;

    @Schema(description = "付款金额显示（元）")
    private String paymentAmountDisplay;

    @Schema(description = "付款方式")
    private String paymentMethod;

    @Schema(description = "付款方式名称")
    private String paymentMethodName;

    @Schema(description = "付款银行账户ID")
    private Long bankAccountId;

    @Schema(description = "付款账户名称")
    private String bankAccountName;

    @Schema(description = "付款日期")
    private LocalDate paymentDate;

    @Schema(description = "关联资金流水ID")
    private Long fundFlowId;

    @Schema(description = "关联流水号")
    private String fundFlowNo;

    @Schema(description = "关联会计凭证ID")
    private Long voucherId;

    @Schema(description = "关联凭证号")
    private String voucherNo;

    @Schema(description = "状态：1-已确认 2-已作废")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "作废时间")
    private LocalDateTime voidTime;

    @Schema(description = "作废备注")
    private String voidRemark;

    @Schema(description = "作废后新关联资金流水ID")
    private Long voidFundFlowId;

    @Schema(description = "作废后新关联流水号")
    private String voidFundFlowNo;

    @Schema(description = "作废后新关联会计凭证ID")
    private Long voidVoucherId;

    @Schema(description = "作废后新关联凭证号")
    private String voidVoucherNo;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getPayableId() {
        return payableId;
    }

    public void setPayableId(Long payableId) {
        this.payableId = payableId;
    }

    public String getPayableNo() {
        return payableNo;
    }

    public void setPayableNo(String payableNo) {
        this.payableNo = payableNo;
    }

    public Long getStockinId() {
        return stockinId;
    }

    public void setStockinId(Long stockinId) {
        this.stockinId = stockinId;
    }

    public String getStockinNo() {
        return stockinNo;
    }

    public void setStockinNo(String stockinNo) {
        this.stockinNo = stockinNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentAmountDisplay() {
        return paymentAmountDisplay;
    }

    public void setPaymentAmountDisplay(String paymentAmountDisplay) {
        this.paymentAmountDisplay = paymentAmountDisplay;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Long getFundFlowId() {
        return fundFlowId;
    }

    public void setFundFlowId(Long fundFlowId) {
        this.fundFlowId = fundFlowId;
    }

    public String getFundFlowNo() {
        return fundFlowNo;
    }

    public void setFundFlowNo(String fundFlowNo) {
        this.fundFlowNo = fundFlowNo;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getVoidTime() {
        return voidTime;
    }

    public void setVoidTime(LocalDateTime voidTime) {
        this.voidTime = voidTime;
    }

    public String getVoidRemark() {
        return voidRemark;
    }

    public void setVoidRemark(String voidRemark) {
        this.voidRemark = voidRemark;
    }

    public Long getVoidFundFlowId() {
        return voidFundFlowId;
    }

    public void setVoidFundFlowId(Long voidFundFlowId) {
        this.voidFundFlowId = voidFundFlowId;
    }

    public String getVoidFundFlowNo() {
        return voidFundFlowNo;
    }

    public void setVoidFundFlowNo(String voidFundFlowNo) {
        this.voidFundFlowNo = voidFundFlowNo;
    }

    public Long getVoidVoucherId() {
        return voidVoucherId;
    }

    public void setVoidVoucherId(Long voidVoucherId) {
        this.voidVoucherId = voidVoucherId;
    }

    public String getVoidVoucherNo() {
        return voidVoucherNo;
    }

    public void setVoidVoucherNo(String voidVoucherNo) {
        this.voidVoucherNo = voidVoucherNo;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
