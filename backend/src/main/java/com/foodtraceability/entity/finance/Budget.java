package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预算实体类
 * 管理企业年度/月度预算，支持预算执行情况对比和预警
 */
@TableName("budgets")
public class Budget extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预算ID */
    @TableId(type = IdType.AUTO)
    private Long budgetId;

    /** 预算年份 */
    private Integer budgetYear;

    /** 预算月份（1-12），NULL表示年度预算 */
    private Integer budgetMonth;

    /**
     * 预算类型
     * 1-收入预算 2-成本预算 3-费用预算 4-利润预算 5-现金流预算
     */
    private Integer budgetType;

    /** 细分科目/类别ID */
    private Integer categoryId;

    /** 预算金额（单位：分） */
    private Long budgetAmount;

    /** 实际金额（单位：分） */
    private Long actualAmount;

    /** 差异=预算-实际（单位：分） */
    private Long variance;

    /** 差异率% */
    private BigDecimal varianceRate;

    /** 责任部门ID */
    private Long responsibleDeptId;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(Integer budgetYear) {
        this.budgetYear = budgetYear;
    }

    public Integer getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(Integer budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public Integer getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(Integer budgetType) {
        this.budgetType = budgetType;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Long getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(Long budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public Long getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(Long actualAmount) {
        this.actualAmount = actualAmount;
    }

    public Long getVariance() {
        return variance;
    }

    public void setVariance(Long variance) {
        this.variance = variance;
    }

    public BigDecimal getVarianceRate() {
        return varianceRate;
    }

    public void setVarianceRate(BigDecimal varianceRate) {
        this.varianceRate = varianceRate;
    }

    public Long getResponsibleDeptId() {
        return responsibleDeptId;
    }

    public void setResponsibleDeptId(Long responsibleDeptId) {
        this.responsibleDeptId = responsibleDeptId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
