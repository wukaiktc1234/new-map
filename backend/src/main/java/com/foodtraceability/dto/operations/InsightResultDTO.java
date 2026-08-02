package com.foodtraceability.dto.operations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 智能洞察结果DTO
 * 智能洞察规则匹配后的输出
 */
public class InsightResultDTO {

    /**
     * 洞察项列表
     */
    private List<InsightItem> insights;

    /**
     * 按严重程度分组统计
     */
    private Map<String, Integer> severityStats;

    /**
     * 洞察项
     */
    public static class InsightItem {

        /**
         * 洞察类型：1异常提醒 2增长亮点 3行动建议 4趋势预警
         */
        private Integer insightType;

        /**
         * 严重程度：1提示 2警告 3严重
         */
        private Integer severity;

        /**
         * 消息内容
         */
        private String message;

        /**
         * 关联指标key
         */
        private String metricKey;

        /**
         * 指标名称
         */
        private String metricName;

        /**
         * 当前值（元或数量）
         */
        private String currentValue;

        /**
         * 对比值
         */
        private String compareValue;

        /**
         * 差异百分比
         */
        private BigDecimal diffPercent;

        /**
         * 规则ID
         */
        private Long ruleId;

        /**
         * 优先级
         */
        private Integer priority;

        // ========== Getter & Setter ==========

        public Integer getInsightType() {
            return insightType;
        }

        public void setInsightType(Integer insightType) {
            this.insightType = insightType;
        }

        public Integer getSeverity() {
            return severity;
        }

        public void setSeverity(Integer severity) {
            this.severity = severity;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

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

        public String getCompareValue() {
            return compareValue;
        }

        public void setCompareValue(String compareValue) {
            this.compareValue = compareValue;
        }

        public BigDecimal getDiffPercent() {
            return diffPercent;
        }

        public void setDiffPercent(BigDecimal diffPercent) {
            this.diffPercent = diffPercent;
        }

        public Long getRuleId() {
            return ruleId;
        }

        public void setRuleId(Long ruleId) {
            this.ruleId = ruleId;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }

    // ========== Getter & Setter ==========

    public List<InsightItem> getInsights() {
        return insights;
    }

    public void setInsights(List<InsightItem> insights) {
        this.insights = insights;
    }

    public Map<String, Integer> getSeverityStats() {
        return severityStats;
    }

    public void setSeverityStats(Map<String, Integer> severityStats) {
        this.severityStats = severityStats;
    }
}
