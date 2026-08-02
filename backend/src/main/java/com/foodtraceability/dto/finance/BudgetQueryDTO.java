package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 预算查询DTO
 */
public class BudgetQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预算年份 */
    private Integer budgetYear;

    /** 预算月份 */
    private Integer budgetMonth;

    /**
     * 预算类型
     * 1-收入预算 2-成本预算 3-费用预算 4-利润预算 5-现金流预算
     */
    private Integer budgetType;

    /** 细分科目/类别ID */
    private Integer categoryId;

    /** 责任部门ID */
    private Long responsibleDeptId;

    /** 当前页码 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 20;

    // getter和setter方法
    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }
    public Integer getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(Integer budgetMonth) { this.budgetMonth = budgetMonth; }
    public Integer getBudgetType() { return budgetType; }
    public void setBudgetType(Integer budgetType) { this.budgetType = budgetType; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Long getResponsibleDeptId() { return responsibleDeptId; }
    public void setResponsibleDeptId(Long responsibleDeptId) { this.responsibleDeptId = responsibleDeptId; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
