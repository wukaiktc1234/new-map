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
 * 收支流水实体类
 * 记录企业的所有收入、支出和转账业务
 * 支持审批流程，可关联生成记账凭证
 */
@TableName("finance_records")
public class FinanceRecord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 记录编号，唯一标识 */
    private String recordNo;

    /**
     * 收支类型
     * 1-收入 2-支出 3-转账
     */
    private Integer recordType;

    /**
     * 收支类别
     * 收入: 101-销售收入 102-服务收入 103-其他收入
     * 支出: 201-采购支出 202-工资支出 203-租金支出 204-水电支出 205-其他支出
     */
    private Integer recordCategory;

    /** 金额（单位：分） */
    private Long amount;

    /**
     * 支付方式
     * 1-现金 2-银行存款 3-微信 4-支付宝 5-支票 6-其他
     */
    private Integer paymentMethod;

    /** 对应的会计科目ID */
    private Long accountSubjectId;

    /** 对方单位/个人名称 */
    private String counterpartyName;

    /**
     * 对方类型
     * 1-供应商 2-客户 3-员工 4-其他
     */
    private Integer counterpartyType;

    /** 业务发生日期 */
    private LocalDate businessDate;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 关联的凭证ID */
    private Long voucherId;

    /** 关联的发票ID */
    private Long invoiceId;

    /**
     * 审批状态
     * 0-待审批 1-已审批 2-已驳回
     */
    private Integer approvalStatus;

    /** 审批人ID */
    private Long approveUserId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public Integer getRecordType() {
        return recordType;
    }

    public void setRecordType(Integer recordType) {
        this.recordType = recordType;
    }

    public Integer getRecordCategory() {
        return recordCategory;
    }

    public void setRecordCategory(Integer recordCategory) {
        this.recordCategory = recordCategory;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Integer paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getAccountSubjectId() {
        return accountSubjectId;
    }

    public void setAccountSubjectId(Long accountSubjectId) {
        this.accountSubjectId = accountSubjectId;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public Integer getCounterpartyType() {
        return counterpartyType;
    }

    public void setCounterpartyType(Integer counterpartyType) {
        this.counterpartyType = counterpartyType;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Integer getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(Integer approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Long getApproveUserId() {
        return approveUserId;
    }

    public void setApproveUserId(Long approveUserId) {
        this.approveUserId = approveUserId;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
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
}
