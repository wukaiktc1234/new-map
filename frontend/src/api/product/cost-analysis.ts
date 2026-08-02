/**
 * 成本分析API
 * 对应后端: /v1/product-center/cost-analysis
 */
import { get } from '@/api/request'
import { productDataConverter } from './converters'
import type {
  CostAnalysisBackend,
  CostAnalysisItem,
  CostSummary,
  CostTrendData,
  SalesSummaryItem,
} from '@/types/product'

export const costAnalysisApi = {
  async getReport(params?: {
    categoryId?: number
    keyword?: string
    status?: number
    page?: number
    size?: number
  }): Promise<{ records: CostAnalysisItem[]; total: number }> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<{ records: CostAnalysisBackend[]; total: number }>(
      '/v1/product-center/cost-analysis',
      params as Record<string, unknown> | undefined
    )
    if (!res) return { records: [], total: 0 }
    return {
      records: (res.records || []).map(item => productDataConverter.costAnalysisToFrontend(item)),
      total: res.total ?? 0,
    }
  },

  async getSummary(params?: {
    categoryId?: number
    keyword?: string
  }): Promise<CostSummary> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<CostSummary>(
      '/v1/product-center/cost-analysis/summary',
      params as Record<string, unknown> | undefined
    )
    return res || {
      totalDishes: 0,
      normalCount: 0,
      warningCount: 0,
      dangerCount: 0,
      avgMarginRate: 0,
      totalCostChange: 0,
      topCostIncrease: [],
      topNegativeProfit: [],
    }
  },

  async getTrend(params?: {
    startDate?: string
    endDate?: string
    dishId?: number
  }): Promise<CostTrendData[]> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<CostTrendData[]>(
      '/v1/product-center/cost-analysis/trend',
      params as Record<string, unknown> | undefined
    )
    return res || []
  },

  async getLowProfitWarning(params?: {
    threshold?: number
    limit?: number
  }): Promise<CostAnalysisItem[]> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<CostAnalysisBackend[]>(
      '/v1/product-center/cost-analysis/low-profit-warning',
      params as Record<string, unknown> | undefined
    )
    return (res || []).map(item => productDataConverter.costAnalysisToFrontend(item))
  },

  async getProfitRateDistribution(params?: {
    categoryId?: number
  }): Promise<Record<string, number>> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<Record<string, number>>(
      '/v1/product-center/cost-analysis/profit-distribution',
      params as Record<string, unknown> | undefined
    )
    return res || {}
  },

  async getCategoryCostRanking(topN: number = 10): Promise<Array<Record<string, unknown>>> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<Array<Record<string, unknown>>>(
      '/v1/product-center/cost-analysis/category-ranking',
      { topN }
    )
    return res || []
  },

  /**
   * 获取销售数据汇总（按商品聚合的 TOP N 销售额数据）
   * 数据来源：order_items 表，order_items 为空时返回空数组
   * @param topN 前N名，默认10，最大50
   * @returns 销售汇总列表，金额单位为分
   */
  async getSalesSummary(topN: number = 10): Promise<SalesSummaryItem[]> {
    // GET 请求直接传 params 对象，禁止 { params } 嵌套
    const res = await get<SalesSummaryItem[]>(
      '/v1/product-center/cost-analysis/sales-summary',
      { topN }
    )
    return res || []
  },
}

export default costAnalysisApi
