package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算VO
 */
public class BudgetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预算ID */
    private Long budgetId;

    /** 预算年份 */
    private Integer budgetYear;

    /** 预算月份 */
    private Integer budgetMonth;

    /** 预算类型 */
    private Integer budgetType;

    /** 预算类型名称 */
    private String budgetTypeName;

    /** 细分科目/类别ID */
    private Integer categoryId;

    /** 科目/类别名称 */
    private String categoryName;

    /** 预算金额（单位：分） */
    private Long budgetAmount;

    /** 预算金额（元，用于显示） */
    private String budgetAmountDisplay;

    /** 实际金额（单位：分） */
    private Long actualAmount;

    /** 实际金额（元，用于显示） */
    private String actualAmountDisplay;

    /** 差异=预算-实际（单位：分） */
    private Long variance;

    /** 差异（元，用于显示） */
    private String varianceDisplay;

    /** 差异率% */
    private BigDecimal varianceRate;

    /** 执行率% */
    private BigDecimal executionRate;

    /** 责任部门ID */
    private Long responsibleDeptId;

    /** 责任部门名称 */
    private String responsibleDeptName;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    // getter和setter方法（简洁格式）
    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }
    public Integer getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(Integer budgetMonth) { this.budgetMonth = budgetMonth; }
    public Integer getBudgetType() { return budgetType; }
    public void setBudgetType(Integer budgetType) { this.budgetType = budgetType; }
    public String getBudgetTypeName() { return budgetTypeName; }
    public void setBudgetTypeName(String budgetTypeName) { this.budgetTypeName = budgetTypeName; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Long budgetAmount) { this.budgetAmount = budgetAmount; }
    public String getBudgetAmountDisplay() { return budgetAmountDisplay; }
    public void setBudgetAmountDisplay(String budgetAmountDisplay) { this.budgetAmountDisplay = budgetAmountDisplay; }
    public Long getActualAmount() { return actualAmount; }
    public void setActualAmount(Long actualAmount) { this.actualAmount = actualAmount; }
    public String getActualAmountDisplay() { return actualAmountDisplay; }
    public void setActualAmountDisplay(String actualAmountDisplay) { this.actualAmountDisplay = actualAmountDisplay; }
    public Long getVariance() { return variance; }
    public void setVariance(Long variance) { this.variance = variance; }
    public String getVarianceDisplay() { return varianceDisplay; }
    public void setVarianceDisplay(String varianceDisplay) { this.varianceDisplay = varianceDisplay; }
    public BigDecimal getVarianceRate() { return varianceRate; }
    public void setVarianceRate(BigDecimal varianceRate) { this.varianceRate = varianceRate; }
    public BigDecimal getExecutionRate() { return executionRate; }
    public void setExecutionRate(BigDecimal executionRate) { this.executionRate = executionRate; }
    public Long getResponsibleDeptId() { return responsibleDeptId; }
    public void setResponsibleDeptId(Long responsibleDeptId) { this.responsibleDeptId = responsibleDeptId; }
    public String getResponsibleDeptName() { return responsibleDeptName; }
    public void setResponsibleDeptName(String responsibleDeptName) { this.responsibleDeptName = responsibleDeptName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
