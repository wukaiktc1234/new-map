/**
 * 营销分析API
 * 对应后端: /v1/marketing/analysis
 *
 * 使用标准 get 请求，错误由 request 拦截器统一处理。
 */
import { get } from '../request'

export const marketingAnalysisApi = {
  /** 获取营销活动概况 */
  getOverview(params?: Record<string, unknown>) {
    return get<Record<string, unknown>>('/v1/marketing/analysis/overview', params)
  },

  /** 获取活动效果分析 */
  getCampaignEffect(params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/marketing/analysis/campaigns', params)
  },

  /** 获取渠道分析 */
  getChannelAnalysis(params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/marketing/analysis/channels', params)
  },

  /** 获取趋势数据 */
  getTrends(params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/marketing/analysis/trends', params)
  },
}
