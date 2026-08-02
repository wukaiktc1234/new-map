package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 销售分析视图对象
 * 用于返回销售分析的详细数据给前端
 */
public class SalesAnalysisVO {

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 报表编号
     */
    private String reportNo;

    /**
     * 报表类型
     */
    private Integer reportType;

    /**
     * 报表类型名称
     */
    private String reportTypeName;

    /**
     * 统计周期开始日期
     */
    private String periodStart;

    /**
     * 统计周期结束日期
     */
    private String periodEnd;

    /**
     * 订单总数
     */
    private Integer totalOrders;

    /**
     * 总销售额（元）
     */
    private String totalAmount;

    /**
     * 客单价（元）
     */
    private String avgOrderValue;

    /**
     * 退款单数
     */
    private Integer refundCount;

    /**
     * 退款额（元）
     */
    private String refundAmount;

    /**
     * 堂食销售额（元）
     */
    private String dineInAmount;

    /**
     * 外卖销售额（元）
     */
    private String takeoutAmount;

    /**
     * 自提销售额（元）
     */
    private String selfPickupAmount;

    /**
     * 高峰时段
     */
    private Integer peakHour;

    /**
     * TOP10菜品销量列表
     */
    private List<Map<String, Object>> topFoods;

    /**
     * 支付方式统计
     */
    private Map<String, Object> paymentMethodStats;

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

    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public String getReportTypeName() {
        return reportTypeName;
    }

    public void setReportTypeName(String reportTypeName) {
        this.reportTypeName = reportTypeName;
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

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(String avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }

    public Integer getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(Integer refundCount) {
        this.refundCount = refundCount;
    }

    public String getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(String refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getDineInAmount() {
        return dineInAmount;
    }

    public void setDineInAmount(String dineInAmount) {
        this.dineInAmount = dineInAmount;
    }

    public String getTakeoutAmount() {
        return takeoutAmount;
    }

    public void setTakeoutAmount(String takeoutAmount) {
        this.takeoutAmount = takeoutAmount;
    }

    public String getSelfPickupAmount() {
        return selfPickupAmount;
    }

    public void setSelfPickupAmount(String selfPickupAmount) {
        this.selfPickupAmount = selfPickupAmount;
    }

    public Integer getPeakHour() {
        return peakHour;
    }

    public void setPeakHour(Integer peakHour) {
        this.peakHour = peakHour;
    }

    public List<Map<String, Object>> getTopFoods() {
        return topFoods;
    }

    public void setTopFoods(List<Map<String, Object>> topFoods) {
        this.topFoods = topFoods;
    }

    public Map<String, Object> getPaymentMethodStats() {
        return paymentMethodStats;
    }

    public void setPaymentMethodStats(Map<String, Object> paymentMethodStats) {
        this.paymentMethodStats = paymentMethodStats;
    }

    public String getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(String generateTime) {
        this.generateTime = generateTime;
    }
}
