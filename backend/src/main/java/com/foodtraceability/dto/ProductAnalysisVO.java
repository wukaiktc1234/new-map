package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 产品分析视图对象
 * 用于返回产品分析的详细数据给前端
 */
public class ProductAnalysisVO {

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 报表编号
     */
    private String reportNo;

    /**
     * 统计周期开始日期
     */
    private String periodStart;

    /**
     * 统计周期结束日期
     */
    private String periodEnd;

    /**
     * 在售菜品数
     */
    private Integer totalFoodsCount;

    /**
     * 套餐数
     */
    private Integer totalCombosCount;

    /**
     * 毛利总额（元）
     */
    private String grossProfitTotal;

    /**
     * 毛利率%
     */
    private BigDecimal grossProfitRate;

    /**
     * 菜品毛利贡献TOP10列表
     */
    private List<Map<String, Object>> foodContributionTop10;

    /**
     * 套餐受欢迎度排名列表
     */
    private List<Map<String, Object>> comboPopularity;

    /**
     * 新品表现数据
     */
    private Map<String, Object> newFoodPerformance;

    /**
     * 价格敏感度数据
     */
    private Map<String, Object> priceSensitivityData;

    /**
     * 生成时间
     */
    private String generateTime;

    // getter和setter方法
    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getTotalFoodsCount() {
        return totalFoodsCount;
    }

    public void setTotalFoodsCount(Integer totalFoodsCount) {
        this.totalFoodsCount = totalFoodsCount;
    }

    public Integer getTotalCombosCount() {
        return totalCombosCount;
    }

    public void setTotalCombosCount(Integer totalCombosCount) {
        this.totalCombosCount = totalCombosCount;
    }

    public String getGrossProfitTotal() {
        return grossProfitTotal;
    }

    public void setGrossProfitTotal(String grossProfitTotal) {
        this.grossProfitTotal = grossProfitTotal;
    }

    public BigDecimal getGrossProfitRate() {
        return grossProfitRate;
    }

    public void setGrossProfitRate(BigDecimal grossProfitRate) {
        this.grossProfitRate = grossProfitRate;
    }

    public List<Map<String, Object>> getFoodContributionTop10() {
        return foodContributionTop10;
    }

    public void setFoodContributionTop10(List<Map<String, Object>> foodContributionTop10) {
        this.foodContributionTop10 = foodContributionTop10;
    }

    public List<Map<String, Object>> getComboPopularity() {
        return comboPopularity;
    }

    public void setComboPopularity(List<Map<String, Object>> comboPopularity) {
        this.comboPopularity = comboPopularity;
    }

    public Map<String, Object> getNewFoodPerformance() {
        return newFoodPerformance;
    }

    public void setNewFoodPerformance(Map<String, Object> newFoodPerformance) {
        this.newFoodPerformance = newFoodPerformance;
    }

    public Map<String, Object> getPriceSensitivityData() {
        return priceSensitivityData;
    }

    public void setPriceSensitivityData(Map<String, Object> priceSensitivityData) {
        this.priceSensitivityData = priceSensitivityData;
    }

    public String getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(String generateTime) {
        this.generateTime = generateTime;
    }
}
