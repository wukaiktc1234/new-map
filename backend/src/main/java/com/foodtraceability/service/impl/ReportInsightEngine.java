package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.dto.operations.InsightResultDTO;
import com.foodtraceability.entity.report.ReportInsightRule;
import com.foodtraceability.mapper.ReportInsightRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 智能洞察规则引擎
 * 基于数据规则自动生成异常提醒、增长亮点、行动建议
 */
@Component
public class ReportInsightEngine {

    private static final Logger logger = LoggerFactory.getLogger(ReportInsightEngine.class);

    private final ReportInsightRuleMapper reportInsightRuleMapper;

    public ReportInsightEngine(ReportInsightRuleMapper reportInsightRuleMapper) {
        this.reportInsightRuleMapper = reportInsightRuleMapper;
    }

    /**
     * 执行规则匹配
     * @param reportScope 报表范围：1日报 2周报 3月报 4季报 5年报 6利润
     * @param metrics 指标数据Map，key为metricKey，value为当前值
     * @param compareMetrics 对比指标数据Map
     * @return 洞察结果DTO
     */
    public InsightResultDTO matchRules(Integer reportScope,
                                       Map<String, BigDecimal> metrics,
                                       Map<String, BigDecimal> compareMetrics) {
        InsightResultDTO result = new InsightResultDTO();
        List<InsightResultDTO.InsightItem> insights = new ArrayList<>();

        try {
            // 加载启用的规则
            List<ReportInsightRule> rules = loadEnabledRules(reportScope);
            logger.debug("加载到{}条规则，报表范围: {}", rules.size(), reportScope);

            for (ReportInsightRule rule : rules) {
                try {
                    InsightResultDTO.InsightItem item = evaluateRule(rule, metrics, compareMetrics);
                    if (item != null) {
                        insights.add(item);
                    }
                } catch (Exception e) {
                    logger.warn("规则匹配失败，规则ID: {}, 原因: {}", rule.getRuleId(), e.getMessage());
                }
            }

            // 按优先级排序（数字越大越优先）
            insights.sort((a, b) -> {
                int priorityCompare = b.getPriority().compareTo(a.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                // 严重级别高的排前面
                return b.getSeverity().compareTo(a.getSeverity());
            });

            result.setInsights(insights);
            result.setSeverityStats(calculateSeverityStats(insights));

            logger.info("规则匹配完成，命中{}条洞察", insights.size());
        } catch (Exception e) {
            logger.error("智能洞察规则匹配失败", e);
        }

        return result;
    }

    /**
     * 加载启用的规则
     * @param reportScope 报表范围
     * @return 启用的规则列表
     */
    private List<ReportInsightRule> loadEnabledRules(Integer reportScope) {
        QueryWrapper<ReportInsightRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_enabled", true)
                .and(wrapper -> wrapper.eq("report_scope", 0).or().eq("report_scope", reportScope))
                .orderByDesc("priority");
        return reportInsightRuleMapper.selectList(queryWrapper);
    }

    /**
     * 评估单条规则
     * @param rule 规则
     * @param metrics 当前指标
     * @param compareMetrics 对比指标
     * @return 如果规则命中则返回洞察项，否则返回null
     */
    private InsightResultDTO.InsightItem evaluateRule(ReportInsightRule rule,
                                                      Map<String, BigDecimal> metrics,
                                                      Map<String, BigDecimal> compareMetrics) {
        String metricKey = rule.getMetricKey();
        BigDecimal currentValue = metrics.get(metricKey);

        if (currentValue == null) {
            return null;
        }

        // 获取对比值
        BigDecimal compareValue = getCompareValue(rule, metricKey, compareMetrics);
        if (compareValue == null) {
            return null;
        }

        // 计算差异
        BigDecimal diff = currentValue.subtract(compareValue);
        BigDecimal diffPercent = BigDecimal.ZERO;
        if (compareValue.compareTo(BigDecimal.ZERO) != 0) {
            diffPercent = diff.divide(compareValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // 根据阈值类型选择比较值
        BigDecimal thresholdValue = rule.getThresholdValue();
        BigDecimal compareValueForRule;
        if (rule.getThresholdType() != null && rule.getThresholdType() == 2) {
            // 百分比阈值，使用差异百分比
            compareValueForRule = diffPercent;
        } else {
            // 绝对值阈值
            compareValueForRule = currentValue;
        }

        // 执行比较
        boolean isMatched = executeComparison(compareValueForRule, thresholdValue, rule.getOperator());

        if (!isMatched) {
            return null;
        }

        // 构建洞察项
        InsightResultDTO.InsightItem item = new InsightResultDTO.InsightItem();
        item.setInsightType(rule.getRuleType());
        item.setSeverity(rule.getSeverity());
        item.setMetricKey(metricKey);
        item.setMetricName(getMetricName(metricKey));
        item.setCurrentValue(currentValue.toString());
        item.setCompareValue(compareValue.toString());
        item.setDiffPercent(diffPercent);
        item.setRuleId(rule.getRuleId());
        item.setPriority(rule.getPriority());

        // 消息模板变量替换
        String message = replaceTemplateVariables(rule.getMessageTemplate(), metricKey,
                currentValue, compareValue, diffPercent);
        item.setMessage(message);

        return item;
    }

    /**
     * 获取对比值
     * @param rule 规则
     * @param metricKey 指标key
     * @param compareMetrics 对比指标Map
     * @return 对比值
     */
    private BigDecimal getCompareValue(ReportInsightRule rule, String metricKey,
                                       Map<String, BigDecimal> compareMetrics) {
        // 对比目标：1日均值 2上周同期 3上月同期 4去年同期 5目标值
        // 实际对比值由调用方传入compareMetrics，key格式为 metricKey_compareTarget
        String compareKey = metricKey + "_" + rule.getCompareTarget();
        BigDecimal value = compareMetrics.get(compareKey);
        if (value == null) {
            // 尝试直接用metricKey获取
            value = compareMetrics.get(metricKey);
        }
        return value;
    }

    /**
     * 执行比较运算
     * @param left 左值
     * @param right 右值
     * @param operator 运算符
     * @return 比较结果
     */
    private boolean executeComparison(BigDecimal left, BigDecimal right, String operator) {
        if (left == null || right == null || operator == null) {
            return false;
        }

        int compareResult = left.compareTo(right);

        return switch (operator) {
            case "<" -> compareResult < 0;
            case "<=" -> compareResult <= 0;
            case ">" -> compareResult > 0;
            case ">=" -> compareResult >= 0;
            case "==", "=" -> compareResult == 0;
            case "!=" -> compareResult != 0;
            default -> {
                logger.warn("未知的比较运算符: {}", operator);
                yield false;
            }
        };
    }

    /**
     * 替换消息模板变量
     * @param template 消息模板
     * @param metricKey 指标key
     * @param currentValue 当前值
     * @param compareValue 对比值
     * @param diffPercent 差异百分比
     * @return 替换后的消息
     */
    private String replaceTemplateVariables(String template, String metricKey,
                                            BigDecimal currentValue, BigDecimal compareValue,
                                            BigDecimal diffPercent) {
        if (template == null) {
            return "";
        }

        String result = template;
        result = result.replace("${metricName}", getMetricName(metricKey));
        result = result.replace("${currentValue}", currentValue != null ? currentValue.toString() : "N/A");
        result = result.replace("${compareValue}", compareValue != null ? compareValue.toString() : "N/A");
        result = result.replace("${diffPercent}", diffPercent != null ? diffPercent.setScale(2, RoundingMode.HALF_UP).toString() : "N/A");
        result = result.replace("${diffAbs}", currentValue != null && compareValue != null ? currentValue.subtract(compareValue).abs().toString() : "N/A");

        return result;
    }

    /**
     * 计算严重程度统计
     * @param insights 洞察列表
     * @return 严重程度统计
     */
    private Map<String, Integer> calculateSeverityStats(List<InsightResultDTO.InsightItem> insights) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("info", 0);
        stats.put("warning", 0);
        stats.put("critical", 0);

        for (InsightResultDTO.InsightItem item : insights) {
            switch (item.getSeverity()) {
                case 1 -> stats.put("info", stats.get("info") + 1);
                case 2 -> stats.put("warning", stats.get("warning") + 1);
                case 3 -> stats.put("critical", stats.get("critical") + 1);
                default -> {
                    // 忽略未知级别
                }
            }
        }

        return stats;
    }

    /**
     * 获取指标名称
     * @param metricKey 指标key
     * @return 指标名称
     */
    private String getMetricName(String metricKey) {
        return switch (metricKey) {
            case "revenue" -> "营收";
            case "order_count" -> "订单数";
            case "avg_check" -> "客单价";
            case "profit" -> "利润";
            case "gross_profit" -> "毛利润";
            case "net_profit" -> "净利润";
            case "cost" -> "成本";
            case "cost_rate" -> "成本率";
            case "refund_rate" -> "退款率";
            case "customer_count" -> "客流量";
            case "table_turnover" -> "翻台率";
            default -> metricKey;
        };
    }

    /**
     * 生成默认规则（初始化使用）
     * @return 默认规则列表
     */
    public List<ReportInsightRule> generateDefaultRules() {
        List<ReportInsightRule> rules = new ArrayList<>();

        // 营收低于日均值15%
        ReportInsightRule rule1 = new ReportInsightRule();
        rule1.setRuleName("营收低于日均值");
        rule1.setRuleType(1);
        rule1.setReportScope(0);
        rule1.setMetricKey("revenue");
        rule1.setCompareTarget(1);
        rule1.setThresholdType(2);
        rule1.setThresholdValue(new BigDecimal("-15"));
        rule1.setOperator("<");
        rule1.setSeverity(2);
        rule1.setMessageTemplate("营收低于日均值${diffPercent}%（当前${currentValue}，日均${compareValue}），请关注客流变化");
        rule1.setIsEnabled(true);
        rule1.setPriority(100);
        rules.add(rule1);

        // 订单数同比增长超过20%
        ReportInsightRule rule2 = new ReportInsightRule();
        rule2.setRuleName("订单数大幅增长");
        rule2.setRuleType(2);
        rule2.setReportScope(0);
        rule2.setMetricKey("order_count");
        rule2.setCompareTarget(4);
        rule2.setThresholdType(2);
        rule2.setThresholdValue(new BigDecimal("20"));
        rule2.setOperator(">");
        rule2.setSeverity(1);
        rule2.setMessageTemplate("订单数同比增长${diffPercent}%（当前${currentValue}，去年同期${compareValue}），表现优异");
        rule2.setIsEnabled(true);
        rule2.setPriority(80);
        rules.add(rule2);

        // 成本率超过65%
        ReportInsightRule rule3 = new ReportInsightRule();
        rule3.setRuleName("成本率过高");
        rule3.setRuleType(1);
        rule3.setReportScope(6);
        rule3.setMetricKey("cost_rate");
        rule3.setCompareTarget(5);
        rule3.setThresholdType(2);
        rule3.setThresholdValue(new BigDecimal("65"));
        rule3.setOperator(">");
        rule3.setSeverity(3);
        rule3.setMessageTemplate("成本率达到${currentValue}%（目标${compareValue}%），建议优化采购和库存管理");
        rule3.setIsEnabled(true);
        rule3.setPriority(120);
        rules.add(rule3);

        // 净利润环比下降超过10%
        ReportInsightRule rule4 = new ReportInsightRule();
        rule4.setRuleName("净利润环比下降");
        rule4.setRuleType(3);
        rule4.setReportScope(0);
        rule4.setMetricKey("net_profit");
        rule4.setCompareTarget(3);
        rule4.setThresholdType(2);
        rule4.setThresholdValue(new BigDecimal("-10"));
        rule4.setOperator("<");
        rule4.setSeverity(2);
        rule4.setMessageTemplate("净利润环比下降${diffPercent}%（当前${currentValue}，上月${compareValue}），建议检查成本结构");
        rule4.setIsEnabled(true);
        rule4.setPriority(90);
        rules.add(rule4);

        // 客单价低于目标值
        ReportInsightRule rule5 = new ReportInsightRule();
        rule5.setRuleName("客单价低于目标");
        rule5.setRuleType(3);
        rule5.setReportScope(0);
        rule5.setMetricKey("avg_check");
        rule5.setCompareTarget(5);
        rule5.setThresholdType(1);
        rule5.setThresholdValue(new BigDecimal("30"));
        rule5.setOperator("<");
        rule5.setSeverity(1);
        rule5.setMessageTemplate("客单价低于目标${diffAbs}元（当前${currentValue}，目标${compareValue}），可推荐高价值套餐");
        rule5.setIsEnabled(true);
        rule5.setPriority(60);
        rules.add(rule5);

        return rules;
    }
}
