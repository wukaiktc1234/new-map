package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 财务报表数据传输对象
 * 用于封装财务报表的详细数据
 */
public class FinanceReportDTO {
    
    /**
     * 报表类型
     */
    private String reportType;
    
    /**
     * 报表期间
     */
    private String reportPeriod;
    
    /**
     * 总收入
     */
    private BigDecimal totalIncome;
    
    /**
     * 总支出
     */
    private BigDecimal totalExpense;
    
    /**
     * 总利润
     */
    private BigDecimal totalProfit;
    
    /**
     * 总应纳税额
     */
    private BigDecimal totalTaxAmount;
    
    /**
     * 总已纳税额
     */
    private BigDecimal totalPaidAmount;
    
    /**
     * 总未纳税额
     */
    private BigDecimal totalUnpaidAmount;
    
    /**
     * 资产总额（资产负债表）
     */
    private BigDecimal totalAssets;
    
    /**
     * 负债总额（资产负债表）
     */
    private BigDecimal totalLiabilities;
    
    /**
     * 所有者权益总额（资产负债表）
     */
    private BigDecimal totalEquity;
    
    /**
     * 经营活动产生的现金流量（现金流量表）
     */
    private BigDecimal operatingCashFlow;
    
    /**
     * 投资活动产生的现金流量（现金流量表）
     */
    private BigDecimal investingCashFlow;
    
    /**
     * 筹资活动产生的现金流量（现金流量表）
     */
    private BigDecimal financingCashFlow;
    
    /**
     * 净现金流量（现金流量表）
     */
    private BigDecimal netCashFlow;
    
    /**
     * 报表详情列表
     */
    private List<?> details;
    
    /**
     * 报表明细列表
     */
    private java.util.List<com.foodtraceability.dto.FinanceReportItemDTO> reportDetails;
    
    /**
     * 按类别统计的收入
     */
    private Map<String, BigDecimal> incomeByCategory;
    
    /**
     * 按类别统计的支出
     */
    private Map<String, BigDecimal> expenseByCategory;
    
    /**
     * 按税种统计的税额
     */
    private Map<String, BigDecimal> taxByType;
    
    /**
     * 按纳税状态统计的税额
     */
    private Map<String, BigDecimal> taxByStatus;
    
    /**
     * 按科目类别统计的余额（资产负债表）
     */
    private Map<String, List<?>> balancesByCategory;
    
    // getter和setter方法
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public String getReportPeriod() {
        return reportPeriod;
    }
    
    public void setReportPeriod(String reportPeriod) {
        this.reportPeriod = reportPeriod;
    }
    
    public BigDecimal getTotalIncome() {
        return totalIncome;
    }
    
    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }
    
    public BigDecimal getTotalExpense() {
        return totalExpense;
    }
    
    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }
    
    public BigDecimal getTotalProfit() {
        return totalProfit;
    }
    
    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }
    
    public BigDecimal getTotalTaxAmount() {
        return totalTaxAmount;
    }
    
    public void setTotalTaxAmount(BigDecimal totalTaxAmount) {
        this.totalTaxAmount = totalTaxAmount;
    }
    
    public BigDecimal getTotalPaidAmount() {
        return totalPaidAmount;
    }
    
    public void setTotalPaidAmount(BigDecimal totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }
    
    public BigDecimal getTotalUnpaidAmount() {
        return totalUnpaidAmount;
    }
    
    public void setTotalUnpaidAmount(BigDecimal totalUnpaidAmount) {
        this.totalUnpaidAmount = totalUnpaidAmount;
    }
    
    public List<?> getDetails() {
        return details;
    }
    
    public void setDetails(List<?> details) {
        this.details = details;
    }
    
    public java.util.List<com.foodtraceability.dto.FinanceReportItemDTO> getReportDetails() {
        return reportDetails;
    }
    
    public void setReportDetails(java.util.List<com.foodtraceability.dto.FinanceReportItemDTO> reportDetails) {
        this.reportDetails = reportDetails;
    }
    
    public Map<String, BigDecimal> getIncomeByCategory() {
        return incomeByCategory;
    }
    
    public void setIncomeByCategory(Map<String, BigDecimal> incomeByCategory) {
        this.incomeByCategory = incomeByCategory;
    }
    
    public Map<String, BigDecimal> getExpenseByCategory() {
        return expenseByCategory;
    }
    
    public void setExpenseByCategory(Map<String, BigDecimal> expenseByCategory) {
        this.expenseByCategory = expenseByCategory;
    }
    
    public Map<String, BigDecimal> getTaxByType() {
        return taxByType;
    }
    
    public void setTaxByType(Map<String, BigDecimal> taxByType) {
        this.taxByType = taxByType;
    }
    
    public Map<String, BigDecimal> getTaxByStatus() {
        return taxByStatus;
    }
    
    public void setTaxByStatus(Map<String, BigDecimal> taxByStatus) {
        this.taxByStatus = taxByStatus;
    }
    
    public BigDecimal getTotalAssets() {
        return totalAssets;
    }
    
    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }
    
    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }
    
    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }
    
    public BigDecimal getTotalEquity() {
        return totalEquity;
    }
    
    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }
    
    public BigDecimal getOperatingCashFlow() {
        return operatingCashFlow;
    }
    
    public void setOperatingCashFlow(BigDecimal operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }
    
    public BigDecimal getInvestingCashFlow() {
        return investingCashFlow;
    }
    
    public void setInvestingCashFlow(BigDecimal investingCashFlow) {
        this.investingCashFlow = investingCashFlow;
    }
    
    public BigDecimal getFinancingCashFlow() {
        return financingCashFlow;
    }
    
    public void setFinancingCashFlow(BigDecimal financingCashFlow) {
        this.financingCashFlow = financingCashFlow;
    }
    
    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }
    
    public void setNetCashFlow(BigDecimal netCashFlow) {
        this.netCashFlow = netCashFlow;
    }
    
    public Map<String, List<?>> getBalancesByCategory() {
        return balancesByCategory;
    }
    
    public void setBalancesByCategory(Map<String, List<?>> balancesByCategory) {
        this.balancesByCategory = balancesByCategory;
    }
}