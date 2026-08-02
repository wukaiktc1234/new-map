package com.foodtraceability.dto.operations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * KPI汇总DTO
 * 用于首屏快速加载核心KPI数据
 *
 * 金额字段说明：
 * - 后端和数据库以分为单位（Long整数）
 * - 前端以元为单位（浮点字符串）
 * - 通过DataConverter在API边界处完成转换
 */
public class KpiSummaryDTO {

    /**
     * KPI卡片列表
     */
    private List<KpiCardItem> kpiCards;

    /**
     * 汇总时间范围
     */
    private String period;

    /**
     * 报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析
     */
    private Integer reportType;

    /**
     * 扩展数据
     */
    private Map<String, Object> extra;

    /**
     * KPI卡片项
     */
    public static class KpiCardItem {

        /**
         * 指标key
         */
        private String metricKey;

        /**
         * 指标名称
         */
        private String metricName;

        /**
         * 当前值（元，浮点字符串，前端展示用）
         */
        private String currentValue;

        /**
         * 当前值（分，整数，后端存储用）
         */
        private Long currentValueFen;

        /**
         * 同比变化率（百分比，如5.2表示+5.2%）
         */
        private BigDecimal yoyChange;

        /**
         * 环比变化率（百分比）
         */
        private BigDecimal momChange;

        /**
         * 目标达成率（百分比，如102.4表示102.4%）
         */
        private BigDecimal targetAchievement;

        /**
         * 单位
         */
        private String unit;

        /**
         * 是否金额类型
         */
        private Boolean isCurrency;

        // ========== Getter & Setter ==========

        public String getMetricKey() {
            return metricKey;
        }

        public void setMetricKey(String metricKey) {
            this.metricKey = metricKey;
        }

        public String getMetricName() {
            return metricName;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }

        public Long getCurrentValueFen() {
            return currentValueFen;
        }

        public void setCurrentValueFen(Long currentValueFen) {
            this.currentValueFen = currentValueFen;
        }

        public BigDecimal getYoyChange() {
            return yoyChange;
        }

        public void setYoyChange(BigDecimal yoyChange) {
            this.yoyChange = yoyChange;
        }

        public BigDecimal getMomChange() {
            return momChange;
        }

        public void setMomChange(BigDecimal momChange) {
            this.momChange = momChange;
        }

        public BigDecimal getTargetAchievement() {
            return targetAchievement;
        }

        public void setTargetAchievement(BigDecimal targetAchievement) {
            this.targetAchievement = targetAchievement;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Boolean getIsCurrency() {
            return isCurrency;
        }

        public void setIsCurrency(Boolean isCurrency) {
            this.isCurrency = isCurrency;
        }
    }

    // ========== Getter & Setter ==========

    public List<KpiCardItem> getKpiCards() {
        return kpiCards;
    }

    public void setKpiCards(List<KpiCardItem> kpiCards) {
        this.kpiCards = kpiCards;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
