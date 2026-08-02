package com.foodtraceability.dto.finance;

/**
 * 财务统计概览VO
 *
 * <p>Sprint 3.1 P0 F-002：用于 FinanceStatisticsController 返回当月财务概览，
 * 含当月收入/支出/利润（Long，分）+ 环比变化率（Double）。
 * 金额字段全部 Long（分），无 BigDecimal。</p>
 */
public class FinanceStatisticsVO {

    /** 当月收入（单位：分） */
    private Long monthlyIncome;

    /** 当月支出（单位：分） */
    private Long monthlyExpense;

    /** 当月利润（单位：分，= 收入 - 支出） */
    private Long monthlyProfit;

    /** 收入环比变化率（百分比，正值表示增长，负值表示下降） */
    private Double monthlyIncomeChange;

    /** 支出环比变化率（百分比） */
    private Double monthlyExpenseChange;

    /** 利润环比变化率（百分比） */
    private Double monthlyProfitChange;

    /** 上月收入（单位：分） */
    private Long lastMonthIncome;

    /** 上月支出（单位：分） */
    private Long lastMonthExpense;

    /** 上月利润（单位：分） */
    private Long lastMonthProfit;

    /** 统计周期（如 "2026-06"） */
    private String period;

    public FinanceStatisticsVO() {
    }

    public Long getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Long monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public Long getMonthlyExpense() {
        return monthlyExpense;
    }

    public void setMonthlyExpense(Long monthlyExpense) {
        this.monthlyExpense = monthlyExpense;
    }

    public Long getMonthlyProfit() {
        return monthlyProfit;
    }

    public void setMonthlyProfit(Long monthlyProfit) {
        this.monthlyProfit = monthlyProfit;
    }

    public Double getMonthlyIncomeChange() {
        return monthlyIncomeChange;
    }

    public void setMonthlyIncomeChange(Double monthlyIncomeChange) {
        this.monthlyIncomeChange = monthlyIncomeChange;
    }

    public Double getMonthlyExpenseChange() {
        return monthlyExpenseChange;
    }

    public void setMonthlyExpenseChange(Double monthlyExpenseChange) {
        this.monthlyExpenseChange = monthlyExpenseChange;
    }

    public Double getMonthlyProfitChange() {
        return monthlyProfitChange;
    }

    public void setMonthlyProfitChange(Double monthlyProfitChange) {
        this.monthlyProfitChange = monthlyProfitChange;
    }

    public Long getLastMonthIncome() {
        return lastMonthIncome;
    }

    public void setLastMonthIncome(Long lastMonthIncome) {
        this.lastMonthIncome = lastMonthIncome;
    }

    public Long getLastMonthExpense() {
        return lastMonthExpense;
    }

    public void setLastMonthExpense(Long lastMonthExpense) {
        this.lastMonthExpense = lastMonthExpense;
    }

    public Long getLastMonthProfit() {
        return lastMonthProfit;
    }

    public void setLastMonthProfit(Long lastMonthProfit) {
        this.lastMonthProfit = lastMonthProfit;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
