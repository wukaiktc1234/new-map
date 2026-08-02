/**
 * 临期预警 API
 * 对应后端: ExpiryAlertController (/v1/expiry-alerts)
 *
 * 对接策略：
 * - 所有方法直接调用真实后端 API，无 Mock 降级
 * - 使用 @/api/request 导出的 get/post 辅助函数（内部基于 request 实例）
 * - GET 请求直接传 params 对象（不嵌套），符合项目 API 规范
 * - 响应数据由拦截器自动提取，前端直接使用返回值（不访问 .data）
 */
import { get, post } from '@/api/request'
import type { PageResponse } from '@/types'
import type {
  ExpiryAlert,
  ExpiryAlertQuery,
  ExpiryDashboard,
  ExpiryStatistics,
} from '@/types/traceability'

export const expiryAlertApi = {
  /**
   * 获取临期预警看板
   * GET /v1/expiry-alerts/dashboard
   */
  getDashboard(): Promise<ExpiryDashboard> {
    return get<ExpiryDashboard>('/v1/expiry-alerts/dashboard')
  },

  /**
   * 查询即将过期列表（黄色/绿色预警）
   * GET /v1/expiry-alerts/expiring-soon
   */
  getExpiringSoon(params: ExpiryAlertQuery): Promise<PageResponse<ExpiryAlert>> {
    return get<PageResponse<ExpiryAlert>>('/v1/expiry-alerts/expiring-soon', params as Record<string, unknown>)
  },

  /**
   * 查询已过期列表（红色预警）
   * GET /v1/expiry-alerts/expired
   */
  getExpired(params: ExpiryAlertQuery): Promise<PageResponse<ExpiryAlert>> {
    return get<PageResponse<ExpiryAlert>>('/v1/expiry-alerts/expired', params as Record<string, unknown>)
  },

  /**
   * 报损处理
   * POST /v1/expiry-alerts/{traceCodeId}/scrap
   * 后端 @RequestParam(required=false) String remark，通过 query 传递
   * 食品安全关键操作：禁止假成功，后端不可用时抛异常
   */
  scrap(traceCodeId: number, remark?: string): Promise<boolean> {
    const params = remark ? { remark } : undefined
    return post<boolean>(`/v1/expiry-alerts/${encodeURIComponent(traceCodeId)}/scrap`, undefined, { params })
  },

  /**
   * 退货处理
   * POST /v1/expiry-alerts/{traceCodeId}/return
   * 后端 @RequestParam(required=false) String remark，通过 query 传递
   * 食品安全关键操作：禁止假成功，后端不可用时抛异常
   */
  returnGoods(traceCodeId: number, remark?: string): Promise<boolean> {
    const params = remark ? { remark } : undefined
    return post<boolean>(`/v1/expiry-alerts/${encodeURIComponent(traceCodeId)}/return`, undefined, { params })
  },

  /**
   * 获取临期预警统计
   * GET /v1/expiry-alerts/statistics
   */
  getStatistics(): Promise<ExpiryStatistics> {
    return get<ExpiryStatistics>('/v1/expiry-alerts/statistics')
  },
}

export default expiryAlertApi
