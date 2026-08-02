package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 科目映射规则VO
 * 展示自动凭证的科目映射规则详情
 */
public class AccountMappingRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 事件类型 */
    private String eventType;

    /** 事件类型名称 */
    private String eventTypeName;

    /** 借方科目编码 */
    private String debitAccountCode;

    /** 借方科目名称 */
    private String debitAccountName;

    /** 贷方科目编码 */
    private String creditAccountCode;

    /** 贷方科目名称 */
    private String creditAccountName;

    /** 税率(%) */
    private BigDecimal taxRate;

    /** 成本中心 */
    private String costCenter;

    /** 摘要模板 */
    private String descriptionTemplate;

    /** 是否启用 */
    private Boolean enabled;

    /** 优先级 */
    private Integer priority;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

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

    public String getEventTypeName() {
        return eventTypeName;
    }

    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    public String getDebitAccountCode() {
        return debitAccountCode;
    }

    public void setDebitAccountCode(String debitAccountCode) {
        this.debitAccountCode = debitAccountCode;
    }

    public String getDebitAccountName() {
        return debitAccountName;
    }

    public void setDebitAccountName(String debitAccountName) {
        this.debitAccountName = debitAccountName;
    }

    public String getCreditAccountCode() {
        return creditAccountCode;
    }

    public void setCreditAccountCode(String creditAccountCode) {
        this.creditAccountCode = creditAccountCode;
    }

    public String getCreditAccountName() {
        return creditAccountName;
    }

    public void setCreditAccountName(String creditAccountName) {
        this.creditAccountName = creditAccountName;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
