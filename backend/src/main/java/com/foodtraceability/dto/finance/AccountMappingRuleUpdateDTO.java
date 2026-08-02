package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 科目映射规则更新DTO
 * 所有字段可选，仅更新传入的字段
 */
public class AccountMappingRuleUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则名称 */
    private String ruleName;

    /** 事件类型 */
    private String eventType;

    /** 借方科目编码 */
    private String debitAccountCode;

    /** 贷方科目编码 */
    private String creditAccountCode;

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 成本中心 */
    private String costCenter;

    /** 摘要模板 */
    private String descriptionTemplate;

    /** 优先级 */
    private Integer priority;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDebitAccountCode() {
        return debitAccountCode;
    }

    public void setDebitAccountCode(String debitAccountCode) {
        this.debitAccountCode = debitAccountCode;
    }

    public String getCreditAccountCode() {
        return creditAccountCode;
    }

    public void setCreditAccountCode(String creditAccountCode) {
        this.creditAccountCode = creditAccountCode;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public void setDescriptionTemplate(String descriptionTemplate) {
        this.descriptionTemplate = descriptionTemplate;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
