package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款单实体类
 * 记录每次向供应商付款的明细，支持四账联动（应付账款+银行账户+资金流水+会计凭证）
 * 付款单号格式：FK + yyyyMMdd + 4位序号
 */
@TableName("payment")
public class Payment extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 付款单ID */
    @TableId(type = IdType.AUTO)
    private Long paymentId;

    /** 付款单号 */
    private String paymentNo;

    /** 关联应付账款ID */
    private Long payableId;

    /** 关联应付编号（冗余） */
    private String payableNo;

    /** 关联采购入库单ID */
    private Long stockinId;

    /** 关联采购入库单号（冗余） */
    private String stockinNo;

    /** 关联采购订单号（冗余） */
    private String orderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 付款金额（分） */
    private Long paymentAmount;

    /**
     * 付款方式
     * bank_transfer-银行转账 cash-现金 check-支票
     */
    private String paymentMethod;

    /** 付款银行账户ID */
    private Long bankAccountId;

    /** 付款账户名称（冗余） */
    private String bankAccountName;

    /** 付款日期 */
    private LocalDate paymentDate;

    /** 关联资金流水ID */
    private Long fundFlowId;

    /** 关联流水号（冗余，便于付款历史直接展示） */
    private String fundFlowNo;

    /** 关联会计凭证ID */
    private Long voucherId;

    /** 关联凭证号（冗余，便于付款历史直接展示） */
    private String voucherNo;

    /**
     * 状态
     * 1-已确认 2-已作废
     */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 作废时间 */
    private LocalDateTime voidTime;

    /** 作废备注 */
    private String voidRemark;

    /** 作废后新关联资金流水ID */
    private Long voidFundFlowId;

    /** 作废后新关联流水号 */
    private String voidFundFlowNo;

    /** 作废后新关联会计凭证ID */
    private Long voidVoucherId;

    /** 作废后新关联凭证号 */
    private String voidVoucherNo;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人姓名 */
    private String createUserName;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", paymentNo='" + paymentNo + '\'' +
                ", payableId=" + payableId +
                ", payableNo='" + payableNo + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", paymentAmount=" + paymentAmount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", bankAccountId=" + bankAccountId +
                ", paymentDate=" + paymentDate +
                ", fundFlowId=" + fundFlowId +
                ", voucherId=" + voucherId +
                ", status=" + status +
                '}';
    }
}
