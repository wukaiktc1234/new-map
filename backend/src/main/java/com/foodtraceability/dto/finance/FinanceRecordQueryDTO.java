package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 收支记录查询DTO
 */
public class FinanceRecordQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录编号（模糊查询） */
    private String recordNo;

    /** 收支类型：1收入 2支出 3转账 */
    private Integer recordType;

    /** 收支类别 */
    private Integer recordCategory;

    /** 支付方式 */
    private Integer paymentMethod;

    /** 对方名称（模糊查询） */
    private String counterpartyName;

    /** 业务日期起 */
    private LocalDate startDate;

    /** 业务日期止 */
    private LocalDate endDate;

    /** 审批状态：0待审批 1已审批 2已驳回 */
    private Integer approvalStatus;

    /** 当前页码 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 20;

    // getter和setter方法
    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
    public Integer getRecordType() { return recordType; }
    public void setRecordType(Integer recordType) { this.recordType = recordType; }
    public Integer getRecordCategory() { return recordCategory; }
    public void setRecordCategory(Integer recordCategory) { this.recordCategory = recordCategory; }
    public Integer getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
