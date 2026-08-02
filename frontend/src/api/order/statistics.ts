/**
 * 订单统计 API
 *
 * 对应后端：
 * - OrderTrendsController (/v1/orders/trends)：订单趋势、按日期分组统计
 * - SalesAnalysisController (/v1/analytics/sales)：销售分析（热销菜品、时段分布等）
 *
 * 使用标准 get 请求，错误由 request 拦截器统一处理。
 *
 * 说明：
 * - 订单来源分布（堂食/外卖/自提/打包）复用 getTrend() 返回的 orderTypeDistribution 字段，
 *   该字段由后端 OrderNewServiceImpl.getOrderTrends 统计返回，无需独立 API。
 * - 热销菜品排行对接 /v1/analytics/sales/top-foods（后端 TODO，返回空数组，实现后自动生效）。
 * - 时段分布对接 /v1/analytics/sales/hourly-distribution（后端已实现基础结构，当前全 0）。
 */
import { get } from '@/api/request'
import { orderDataConverter } from './converters'
import type {
  DailyStats,
  DailyStatsBackend,
  OrderTrendData,
  TopSellingFood,
  HourlyDistribution,
} from '@/types/order'

export interface TrendQueryForm {
  startDate?: string
  endDate?: string
  /** 粒度：day/week/month */
  granularity?: string
}

export interface DailyStatsQueryForm {
  startDate?: string
  endDate?: string
  storeName?: string
}

/** 热销菜品查询参数 */
export interface TopFoodsQueryForm {
  /** 返回数量上限（默认 10） */
  limit?: number
  /** 开始日期（yyyy-MM-dd） */
  startDate?: string
  /** 结束日期（yyyy-MM-dd） */
  endDate?: string
}

export const orderStatisticsApi = {
  /**
   * 获取订单趋势
   * GET /v1/orders/trends
   * 返回：Map<String, Object>（timeSeries、summary、comparison、trendAnalysis、orderTypeDistribution）
   *
   * orderTypeDistribution 字段为订单类型占比（堂食/外卖/自提/打包 → 百分比数字），
   * 可直接用于订单来源分布饼图。
   */
  async getTrend(params?: TrendQueryForm): Promise<OrderTrendData> {
    const query: Record<string, unknown> = {}
    if (params?.startDate) query.startDate = params.startDate
    if (params?.endDate) query.endDate = params.endDate
    if (params?.granularity) query.granularity = params.granularity
    const res = await get<OrderTrendData>('/v1/orders/trends', query)
    return (res ?? {}) as OrderTrendData
  },

  /**
   * 按日期分组统计
   * GET /v1/orders/trends/daily
   * 转换：后端金额（分）→ 前端金额（元/字符串）
   */
  async getDailyStats(params?: DailyStatsQueryForm): Promise<DailyStats[]> {
    const query: Record<string, unknown> = {}
    if (params?.startDate) query.startDate = params.startDate
    if (params?.endDate) query.endDate = params.endDate
    if (params?.storeName) query.storeName = params.storeName
    const res = await get<DailyStatsBackend[]>('/v1/orders/trends/daily', query)
    if (!res || !Array.isArray(res)) return []
    return res.map((item: DailyStatsBackend) => orderDataConverter.dailyStatsToFrontend(item))
  },

  /**
   * 获取热销菜品 TOP N
   * GET /v1/analytics/sales/top-foods?limit=&startDate=&endDate=
   *
   * 后端 SalesAnalysisServiceImpl.getTopSellingFoods 当前为 TODO，返回空数组。
   * 后端实现后图表自动显示真实数据。
   *
   * 注意：后端返回 List<Map<String, Object>>，字段名基于业务约定
   *      （foodName/totalQuantity/totalAmount），实现后可能需要调整映射。
   *
   * @returns 热销菜品列表（按销量降序）
   */
  async getProductStats(params?: TopFoodsQueryForm): Promise<TopSellingFood[]> {
    const query: Record<string, unknown> = {
      limit: params?.limit ?? 10,
    }
    if (params?.startDate) query.startDate = params.startDate
    if (params?.endDate) query.endDate = params.endDate
    const res = await get<TopSellingFood[]>('/v1/analytics/sales/top-foods', query)
    if (!res || !Array.isArray(res)) return []
    return res
  },

  /**
   * 获取时段分布
   * GET /v1/analytics/sales/hourly-distribution?date=
   *
   * 后端返回 Map<String, Object>：{ hourlyData: [{ hour, orderCount, amount }], date }
   * 后端已实现基础结构（24 小时框架），当前 orderCount/amount 全为 0，待接入真实统计。
   *
   * @param date 日期（yyyy-MM-dd），为空时后端默认今天
   * @returns 时段分布数据（含 24 小时数据列表）
   */
  async getHourStats(date?: string): Promise<HourlyDistribution> {
    const query: Record<string, unknown> = {}
    if (date) query.date = date
    const res = await get<HourlyDistribution>('/v1/analytics/sales/hourly-distribution', query)
    return (res ?? {}) as HourlyDistribution
  },
}

export default orderStatisticsApi
