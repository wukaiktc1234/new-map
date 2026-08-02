package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.product.CostAnalysisQueryDTO;
import com.foodtraceability.dto.product.CostAnalysisVO;
import com.foodtraceability.dto.product.CostTrendDataVO;
import java.util.List;
import java.util.Map;

/**
 * 成本分析服务接口
 * 提供产品成本报表和盈利分析功能
 */
public interface CostAnalysisService {

    /**
     * 分页查询成本分析数据
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Page<CostAnalysisVO> queryAnalysis(CostAnalysisQueryDTO queryDto);

    /**
     * 获取成本分析汇总统计
     * @param queryDto 查询条件（使用productType等筛选）
     * @return 汇总统计数据
     */
    Map<String, Object> getSummary(CostAnalysisQueryDTO queryDto);

    /**
     * 获取毛利率分布统计
     * @param queryDto 查询条件
     * @return 分布区间统计
     */
    Map<String, Integer> getProfitRateDistribution(CostAnalysisQueryDTO queryDto);

    /**
     * 获取分类成本排名
     * @param topN 前N名
     * @return 分类成本排名列表
     */
    List<Map<String, Object>> getCategoryCostRanking(int topN);

    /**
     * 获取低毛利产品预警列表
     * @param threshold 毛利率阈值(%)
     * @param limit 数量限制
     * @return 低毛利产品列表
     */
    List<CostAnalysisVO> getLowProfitWarning(double threshold, int limit);

    /**
     * 获取成本趋势数据
     * @param startDate 开始日期（可空，格式 yyyy-MM-dd）
     * @param endDate 结束日期（可空，格式 yyyy-MM-dd）
     * @param dishId 菜品ID（可空，指定则查询单菜品趋势）
     * @return 趋势数据列表
     */
    List<CostTrendDataVO> getTrend(String startDate, String endDate, Long dishId);

    /**
     * 获取销售数据汇总（按商品聚合的 TOP N 销售额数据）
     * 数据来源：order_items 表，聚合销量与销售额
     * @param topN 前N名
     * @return 销售汇总列表，每项包含 productName/productType/salesCount/salesAmount
     */
    List<Map<String, Object>> getSalesSummary(int topN);
}
