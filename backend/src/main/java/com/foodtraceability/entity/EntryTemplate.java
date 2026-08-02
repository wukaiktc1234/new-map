package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分录模板实体类
 * 定义自动记账时生成的会计分录模板行
 * @author example
 * @since 2026-04-04
 */
@TableName("entry_template")
public class EntryTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    @TableField("template_name")
    private String templateName;

    /**
     * 关联的规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 行号
     */
    @TableField("line_no")
    private Integer lineNo;

    /**
     * 科目编码
     */
    @TableField("subject_code")
    private String subjectCode;

    /**
     * 借贷方向：DEBIT(借方)、CREDIT(贷方)
     */
    @TableField("direction")
    private String direction;

    /**
     * 金额表达式（支持占位符如：#{amount}、#{commission}）
     */
    @TableField("amount_expression")
    private String amountExpression;

    /**
     * 摘要模板（支持占位符）
     */
    @TableField("summary_template")
    private String summaryTemplate;

    /**
     * 是否必填
     */
    @TableField("is_required")
    private Boolean isRequired;

    /**
     * 条件表达式（可选，用于条件性生成分录）
     */
    @TableField("condition_expression")
    private String conditionExpression;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getAmountExpression() {
        return amountExpression;
    }

    public void setAmountExpression(String amountExpression) {
        this.amountExpression = amountExpression;
    }

    public String getSummaryTemplate() {
        return summaryTemplate;
    }

    public void setSummaryTemplate(String summaryTemplate) {
        this.summaryTemplate = summaryTemplate;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "EntryTemplate{" +
            "id=" + id +
            ", templateName='" + templateName + '\'' +
            ", ruleId=" + ruleId +
            ", lineNo=" + lineNo +
            ", subjectCode='" + subjectCode + '\'' +
            ", direction='" + direction + '\'' +
            ", amountExpression='" + amountExpression + '\'' +
            ", sortOrder=" + sortOrder +
            '}';
    }
}
