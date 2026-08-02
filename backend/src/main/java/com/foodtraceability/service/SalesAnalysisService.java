package com.foodtraceability.service;

import com.foodtraceability.dto.SalesAnalysisVO;
import com.foodtraceability.dto.SalesAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import java.util.List;
import java.util.Map;

/**
 * 销售分析服务接口
 * 提供销售数据的分析和报表生成功能
 */
public interface SalesAnalysisService {

    /**
     * 生成日报
     * @param date 日期
     * @return 销售分析报表
     */
    SalesAnalysisVO generateDailySalesReport(java.time.LocalDate date);

    /**
     * 生成周报
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 销售分析报表
     */
    SalesAnalysisVO generateWeeklySalesReport(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取销售趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param granularity 粒度（day/week/month）
     * @return 销售趋势数据
     */
    Map<String, Object> getSalesTrend(java.time.LocalDate startDate, java.time.LocalDate endDate, String granularity);

    /**
     * 获取品类销售占比
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 品类占比数据
     */
    Map<String, Object> getCategorySalesAnalysis(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取热销菜品TOP N
     * @param limit 数量限制
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 热销菜品列表
     */
    List<Map<String, Object>> getTopSellingFoods(Integer limit, java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取时段分布
     * @param date 日期
     * @return 时段分布数据
     */
    Map<String, Object> getHourlyDistribution(java.time.LocalDate date);

    /**
     * 获取渠道对比
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 渠道对比数据
     */
    Map<String, Object> getChannelComparison(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 分页查询销售报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<SalesAnalysisVO> querySalesReports(SalesAnalysisQueryDTO queryDTO);

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    SalesAnalysisVO getReportById(Long reportId);
}
