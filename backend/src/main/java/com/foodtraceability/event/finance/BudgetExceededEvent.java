package com.foodtraceability.event.finance;

import java.io.Serializable;

/**
 * 预算超支事件（F-022）
 *
 * <p>触发场景：实际支出超过预算额度时发布。
 * 不可变事件对象，所有字段使用 final 修饰。
 */
public class BudgetExceededEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预算ID */
    private final Long budgetId;
    /** 预算金额（单位：分） */
    private final Long budgetAmount;
    /** 实际金额（单位：分） */
    private final Long actualAmount;
    /** 超支比例（0.15 表示超支 15%） */
    private final Double exceedRate;

    /**
     * 构造方法
     *
     * @param budgetId     预算ID
     * @param budgetAmount 预算金额（单位：分）
     * @param actualAmount 实际金额（单位：分）
     * @param exceedRate   超支比例
     */
    public BudgetExceededEvent(Long budgetId, Long budgetAmount, Long actualAmount, Double exceedRate) {
        this.budgetId = budgetId;
        this.budgetAmount = budgetAmount;
        this.actualAmount = actualAmount;
        this.exceedRate = exceedRate;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public Long getBudgetAmount() {
        return budgetAmount;
    }

    public Long getActualAmount() {
        return actualAmount;
    }

    public Double getExceedRate() {
        return exceedRate;
    }
}
