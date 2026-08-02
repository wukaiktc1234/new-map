package com.foodtraceability.service.operations;

import java.util.List;
import java.util.Map;

/**
 * 经营分析决策看板服务接口
 * 提供门店健康度评分、门店排名、品类销售分析、AI 洞察建议、营收趋势等
 */
public interface DecisionBoardService {

    /**
     * 获取门店健康度评分
     * @return 健康度评分列表，每条含 label/score/icon
     */
    List<Map<String, Object>> getHealthScores();

    /**
     * 获取门店排名
     * @return 门店排名列表，每条含 rank/storeName/revenue/cost/profit/profitMargin/growth/healthScore
     */
    List<Map<String, Object>> getStoreRanking();

    /**
     * 获取品类销售分析
     * @return 品类销售列表，每条含 name/value/growth
     */
    List<Map<String, Object>> getCategorySales();

    /**
     * 获取 AI 洞察建议
     * @return 洞察建议列表，每条含 id/icon/title/description/severity
     */
    List<Map<String, Object>> getInsights();

    /**
     * 获取营收-成本-利润趋势
     * @param period 时间维度：7d（近7天）/ 4w（近4周）/ 6m（近6月）
     * @return 趋势数据（dates/revenue/cost/profit）
     */
    Map<String, Object> getRevenueTrend(String period);
}
