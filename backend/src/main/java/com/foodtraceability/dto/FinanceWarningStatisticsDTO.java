package com.foodtraceability.dto;

/**
 * 财务风险预警统计DTO
 * 用于封装财务风险预警的统计数据
 */
public class FinanceWarningStatisticsDTO {
    
    /**
     * 总预警数量
     */
    private long totalCount;
    
    /**
     * 未处理预警数量
     */
    private long unhandledCount;
    
    /**
     * 处理中预警数量
     */
    private long handlingCount;
    
    /**
     * 已解决预警数量
     */
    private long resolvedCount;
    
    /**
     * 预算超支预警数量
     */
    private long budgetOverCount;
    
    /**
     * 应收账款逾期预警数量
     */
    private long arOverdueCount;
    
    /**
     * 库存积压预警数量
     */
    private long inventoryOverCount;
    
    /**
     * 现金流风险预警数量
     */
    private long cashFlowRiskCount;
    
    /**
     * 税务风险预警数量
     */
    private long taxRiskCount;
    
    // getter和setter方法
    public long getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
    
    public long getUnhandledCount() {
        return unhandledCount;
    }
    
    public void setUnhandledCount(long unhandledCount) {
        this.unhandledCount = unhandledCount;
    }
    
    public long getHandlingCount() {
        return handlingCount;
    }
    
    public void setHandlingCount(long handlingCount) {
        this.handlingCount = handlingCount;
    }
    
    public long getResolvedCount() {
        return resolvedCount;
    }
    
    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }
    
    public long getBudgetOverCount() {
        return budgetOverCount;
    }
    
    public void setBudgetOverCount(long budgetOverCount) {
        this.budgetOverCount = budgetOverCount;
    }
    
    public long getArOverdueCount() {
        return arOverdueCount;
    }
    
    public void setArOverdueCount(long arOverdueCount) {
        this.arOverdueCount = arOverdueCount;
    }
    
    public long getInventoryOverCount() {
        return inventoryOverCount;
    }
    
    public void setInventoryOverCount(long inventoryOverCount) {
        this.inventoryOverCount = inventoryOverCount;
    }
    
    public long getCashFlowRiskCount() {
        return cashFlowRiskCount;
    }
    
    public void setCashFlowRiskCount(long cashFlowRiskCount) {
        this.cashFlowRiskCount = cashFlowRiskCount;
    }
    
    public long getTaxRiskCount() {
        return taxRiskCount;
    }
    
    public void setTaxRiskCount(long taxRiskCount) {
        this.taxRiskCount = taxRiskCount;
    }
}