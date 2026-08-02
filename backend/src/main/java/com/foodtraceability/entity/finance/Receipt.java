package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 收款单实体类
 * 记录每次向客户收款的明细，支持四账联动（应收账款+银行账户+资金流水+会计凭证）
 * 收款单号格式：SK + yyyyMMdd + 4位序号
 */
@TableName("receipt")
public class Receipt extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收款单ID */
    @TableId(type = IdType.AUTO)
    private Long receiptId;

    /** 收款单号 */
    private String receiptNo;

    /** 关联应收账款ID */
    private Long receivableId;

    /** 关联应收编号（冗余） */
    private String receivableNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 收款金额（分） */
    private Long receiptAmount;

    /**
     * 收款方式
     * bank_transfer-银行转账 cash-现金 check-支票
     */
    private String receiptMethod;

    /** 收款银行账户ID */
    private Long bankAccountId;

    /** 收款账户名称（冗余） */
    private String bankAccountName;

    /** 收款日期 */
    private LocalDate receiptDate;

    /** 关联资金流水ID */
    private Long fundFlowId;

    /** 关联会计凭证ID */
    private Long voucherId;

    /**
     * 状态
     * 1-已确认 2-已作废
     */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人姓名 */
    private String createUserName;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public String getReceivableNo() {
        return receivableNo;
    }

    public void setReceivableNo(String receivableNo) {
        this.receivableNo = receivableNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getReceiptAmount() {
        return receiptAmount;
    }

    public void setReceiptAmount(Long receiptAmount) {
        this.receiptAmount = receiptAmount;
    }

    public String getReceiptMethod() {
        return receiptMethod;
    }

    public void setReceiptMethod(String receiptMethod) {
        this.receiptMethod = receiptMethod;
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

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public Long getFundFlowId() {
        return fundFlowId;
    }

    public void setFundFlowId(Long fundFlowId) {
        this.fundFlowId = fundFlowId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
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
        return "Receipt{" +
                "receiptId=" + receiptId +
                ", receiptNo='" + receiptNo + '\'' +
                ", receivableId=" + receivableId +
                ", receivableNo='" + receivableNo + '\'' +
                ", customerName='" + customerName + '\'' +
                ", receiptAmount=" + receiptAmount +
                ", receiptMethod='" + receiptMethod + '\'' +
                ", bankAccountId=" + bankAccountId +
                ", receiptDate=" + receiptDate +
                ", fundFlowId=" + fundFlowId +
                ", voucherId=" + voucherId +
                ", status=" + status +
                '}';
    }
}
