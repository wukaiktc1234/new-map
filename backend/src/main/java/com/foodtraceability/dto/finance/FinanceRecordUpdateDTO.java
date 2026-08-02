package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 收支记录更新DTO
 */
public class FinanceRecordUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @NotNull(message = "记录ID不能为空")
    private Long recordId;

    /** 收支类型 */
    private Integer recordType;

    /** 收支类别 */
    private Integer recordCategory;

    /** 金额（单位：分） */
    private Long amount;

    /** 支付方式 */
    private Integer paymentMethod;

    /** 对应的会计科目ID */
    private Long accountSubjectId;

    /** 对方单位/个人名称 */
    private String counterpartyName;

    /** 对方类型 */
    private Integer counterpartyType;

    /** 业务发生日期 */
    private LocalDate businessDate;

    /** 备注 */
    private String remark;

    // getter和setter方法
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Integer getRecordType() { return recordType; }
    public void setRecordType(Integer recordType) { this.recordType = recordType; }
    public Integer getRecordCategory() { return recordCategory; }
    public void setRecordCategory(Integer recordCategory) { this.recordCategory = recordCategory; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Integer getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }
    public Long getAccountSubjectId() { return accountSubjectId; }
    public void setAccountSubjectId(Long accountSubjectId) { this.accountSubjectId = accountSubjectId; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public Integer getCounterpartyType() { return counterpartyType; }
    public void setCounterpartyType(Integer counterpartyType) { this.counterpartyType = counterpartyType; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
