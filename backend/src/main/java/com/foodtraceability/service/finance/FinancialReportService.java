package com.foodtraceability.service.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 财务报表Service接口
 * 提供利润表、收支明细、应收应付统计等报表数据
 */
public interface FinancialReportService {

    /**
     * 利润表数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 利润表数据
     */
    Map<String, Object> getProfitStatement(LocalDate startDate, LocalDate endDate);

    /**
     * 收支明细汇总
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收支汇总数据
     */
    Map<String, Object> getIncomeExpenseSummary(LocalDate startDate, LocalDate endDate);

    /**
     * 应收账款统计
     * @return 统计数据（总额、已收、余额等）
     */
    Map<String, Object> getReceivableStatistics();

    /**
     * 应付账款统计
     * @return 统计数据（总额、已付、余额等）
     */
    Map<String, Object> getPayableStatistics();

    /**
     * 账龄分析
     * @return 各账龄段的金额统计
     */
    List<Map<String, Object>> getAgingAnalysis();

    /**
     * 成本结构分析
     * @param period 期间
     * @return 各成本类型的占比
     */
    Map<String, Object> getCostStructure(String period);

    /**
     * 预算执行情况
     * @param year 年份
     * @param type 类型（收入/成本/费用）
     * @return 预算vs实际对比
     */
    Map<String, Object> getBudgetExecution(Integer year, Integer type);
}
