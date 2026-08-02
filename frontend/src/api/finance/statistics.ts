/**
 * 财务统计概览API
 * 对应后端: /v1/finance/statistics
 *
 * 【设计说明】
 * - 后端 FinanceStatisticsController 1 个端点：
 *   GET /overview   获取财务统计概览（当月收入/支出/利润+环比变化率）
 * - 金额字段为 Long（分），通过 fenToYuanNumber 转元展示
 *
 * 【改造说明】
 * - 内置金额分→元转换，返回前端可直接使用的数值
 */
import { get } from '../request'
import { fenToYuanNumber } from './converters'
import type { FinanceStatisticsQuery, FinanceStatisticsOverview } from '@/types/finance'

export const statisticsApi = {
  /**
   * 获取财务统计概览数据
   * 聚合 Receivable/Payable/FinanceVoucher/FundFlow 数据，
   * 计算当月收入/支出/利润及环比变化率
   * @param query - 查询条件（含 startDate/endDate/period）
   * @returns 财务统计概览（金额字段已转元）
   */
  async getOverview(query?: FinanceStatisticsQuery): Promise<FinanceStatisticsOverview> {
    const params: Record<string, unknown> = {}
    if (query?.startDate) params.startDate = query.startDate
    if (query?.endDate) params.endDate = query.endDate
    if (query?.period) params.period = query.period
    const res = await get<Record<string, unknown> | null>('/v1/finance/statistics/overview', params)
    if (!res) {
      return {
        monthlyIncome: 0,
        monthlyExpense: 0,
        monthlyProfit: 0,
      }
    }
    return {
      monthlyIncome: fenToYuanNumber(res.monthlyIncome as number),
      monthlyExpense: fenToYuanNumber(res.monthlyExpense as number),
      monthlyProfit: fenToYuanNumber(res.monthlyProfit as number),
      monthlyIncomeChange: res.monthlyIncomeChange as number | undefined,
      monthlyExpenseChange: res.monthlyExpenseChange as number | undefined,
      monthlyProfitChange: res.monthlyProfitChange as number | undefined,
      lastMonthIncome: fenToYuanNumber(res.lastMonthIncome as number),
      lastMonthExpense: fenToYuanNumber(res.lastMonthExpense as number),
      lastMonthProfit: fenToYuanNumber(res.lastMonthProfit as number),
      period: res.period as string | undefined,
    }
  },
}

export default statisticsApi
