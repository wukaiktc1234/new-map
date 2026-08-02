package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 收支记录创建DTO
 */
public class FinanceRecordCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收支类型
     * 1-收入 2-支出 3-转账
     */
    @NotNull(message = "收支类型不能为空")
    @Min(value = 1, message = "收支类型无效")
    @Max(value = 3, message = "收支类型无效")
    private Integer recordType;

    /**
     * 收支类别
     * 收入: 101-销售收入 102-服务收入 103-其他收入
     * 支出: 201-采购支出 202-工资支出 203-租金支出 204-水电支出 205-其他支出
     */
    @NotNull(message = "收支类别不能为空")
    @Min(value = 100, message = "收支类别无效")
    @Max(value = 300, message = "收支类别无效")
    private Integer recordCategory;

    /** 金额（单位：分） */
    @NotNull(message = "金额不能为空")
    @Min(value = 0, message = "金额不能为负数")
    private Long amount;

    /**
     * 支付方式
     * 1-现金 2-银行存款 3-微信 4-支付宝 5-支票 6-其他
     */
    @Min(value = 1, message = "支付方式无效")
    @Max(value = 6, message = "支付方式无效")
    private Integer paymentMethod;

    /** 对应的会计科目ID */
    private Long accountSubjectId;

    /** 对方单位/个人名称 */
    @Size(max = 100, message = "对方名称长度不能超过100位")
    private String counterpartyName;

    /**
     * 对方类型
     * 1-供应商 2-客户 3-员工 4-其他
     */
    @Min(value = 1, message = "对方类型无效")
    @Max(value = 4, message = "对方类型无效")
    private Integer counterpartyType;

    /** 业务发生日期 */
    @NotNull(message = "业务发生日期不能为空")
    private LocalDate businessDate;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    // getter和setter方法
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
