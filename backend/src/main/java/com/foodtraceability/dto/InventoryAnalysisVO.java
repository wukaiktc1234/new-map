package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存分析视图对象
 * 用于返回库存分析的详细数据给前端
 */
public class InventoryAnalysisVO {

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 报表编号
     */
    private String reportNo;

    /**
     * 报表日期
     */
    private String reportDate;

    /**
     * SKU总数
     */
    private Integer totalSkuCount;

    /**
     * 库存总值（元）
     */
    private String totalInventoryValue;

    /**
     * 周转率%
     */
    private BigDecimal turnoverRate;

    /**
     * 缺货SKU数
     */
    private Integer outOfStockCount;

    /**
     * 积压SKU数
     */
    private Integer overstockCount;

    /**
     * 报损金额（元）
     */
    private String wasteAmount;

    /**
     * 预警次数
     */
    private Integer warningCount;

    /**
     * 分类库存分析列表
     */
    private List<Map<String, Object>> categoryAnalysis;

    /**
     * 周转率排名列表
     */
    private List<Map<String, Object>> turnoverRanking;

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

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public Integer getTotalSkuCount() {
        return totalSkuCount;
    }

    public void setTotalSkuCount(Integer totalSkuCount) {
        this.totalSkuCount = totalSkuCount;
    }

    public String getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(String totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public BigDecimal getTurnoverRate() {
        return turnoverRate;
    }

    public void setTurnoverRate(BigDecimal turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    public Integer getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(Integer outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public Integer getOverstockCount() {
        return overstockCount;
    }

    public void setOverstockCount(Integer overstockCount) {
        this.overstockCount = overstockCount;
    }

    public String getWasteAmount() {
        return wasteAmount;
    }

    public void setWasteAmount(String wasteAmount) {
        this.wasteAmount = wasteAmount;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public List<Map<String, Object>> getCategoryAnalysis() {
        return categoryAnalysis;
    }

    public void setCategoryAnalysis(List<Map<String, Object>> categoryAnalysis) {
        this.categoryAnalysis = categoryAnalysis;
    }

    public List<Map<String, Object>> getTurnoverRanking() {
        return turnoverRanking;
    }

    public void setTurnoverRanking(List<Map<String, Object>> turnoverRanking) {
        this.turnoverRanking = turnoverRanking;
    }

    public String getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(String generateTime) {
        this.generateTime = generateTime;
    }
}
