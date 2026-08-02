/**
 * 客户分析API
 * 对应后端: /v1/analytics/customer
 *
 * 使用标准 get 请求，错误由 request 拦截器统一处理。
 */
import { get } from '../request'

export const customerAnalysisApi = {
  /** 获取客户画像分析 */
  getProfile(params?: Record<string, unknown>) {
    return get<Record<string, unknown>>('/v1/analytics/customer/profile', params)
  },

  /** 获取客户分群 */
  getSegments(params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/analytics/customer/segments', params)
  },

  /** 获取客户价值分析 */
  getValueAnalysis(params?: Record<string, unknown>) {
    return get<Record<string, unknown>>('/v1/analytics/customer/value', params)
  },

  /** 获取流失预警 */
  getChurnPrediction(params?: Record<string, unknown>) {
    return get<unknown[]>('/v1/analytics/customer/churn', params)
  },

  /** 获取复购分析 */
  getRepurchaseAnalysis(params?: Record<string, unknown>) {
    return get<Record<string, unknown>>('/v1/analytics/customer/repurchase', params)
  },
}
