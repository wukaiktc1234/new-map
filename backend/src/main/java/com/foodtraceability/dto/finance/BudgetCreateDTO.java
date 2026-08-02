package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 预算创建DTO
 */
public class BudgetCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预算年份 */
    @NotNull(message = "预算年份不能为空")
    @Min(value = 2000, message = "预算年份无效")
    @Max(value = 2100, message = "预算年份无效")
    private Integer budgetYear;

    /** 预算月份（1-12），NULL表示年度预算 */
    @Min(value = 1, message = "预算月份无效")
    @Max(value = 12, message = "预算月份无效")
    private Integer budgetMonth;

    /**
     * 预算类型
     * 1-收入预算 2-成本预算 3-费用预算 4-利润预算 5-现金流预算
     */
    @NotNull(message = "预算类型不能为空")
    @Min(value = 1, message = "预算类型无效")
    @Max(value = 5, message = "预算类型无效")
    private Integer budgetType;

    /** 细分科目/类别ID */
    private Integer categoryId;

    /** 预算金额（单位：分） */
    @NotNull(message = "预算金额不能为空")
    @Min(value = 0, message = "预算金额不能为负数")
    private Long budgetAmount;

    /** 责任部门ID */
    private Long responsibleDeptId;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    // getter和setter方法
    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }
    public Integer getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(Integer budgetMonth) { this.budgetMonth = budgetMonth; }
    public Integer getBudgetType() { return budgetType; }
    public void setBudgetType(Integer budgetType) { this.budgetType = budgetType; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Long getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Long budgetAmount) { this.budgetAmount = budgetAmount; }
    public Long getResponsibleDeptId() { return responsibleDeptId; }
    public void setResponsibleDeptId(Long responsibleDeptId) { this.responsibleDeptId = responsibleDeptId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
