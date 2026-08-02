/**
 * 退款管理 API
 * 对应后端: OrderNewController (/v1/orders/refunds)
 *
 * 使用标准 get/post 请求，错误由 request 拦截器统一处理。
 */
import { get, post } from '@/api/request'
import { orderDataConverter } from './converters'
import type {
  OrderRefundBackend,
  OrderRefund,
  OrderRefundQueryForm,
  OrderRefundStats,
  OrderRefundStatsBackend,
  PageResponse,
  RefundStatusValue,
} from '@/types/order'

export const orderRefundApi = {
  /**
   * 退款列表查询
   * GET /v1/orders/refunds
   * 转换：前端查询参数（语义化状态）→ 后端编码；后端响应（分/数字）→ 前端（元/字符串）
   */
  async getList(params: OrderRefundQueryForm): Promise<PageResponse<OrderRefund>> {
    const backendParams: Record<string, unknown> = {
      page: params.page || 1,
      size: params.size || 10,
    }
    if (params.refundNo) backendParams.refundNo = params.refundNo
    if (params.orderCode) backendParams.orderCode = params.orderCode
    if (params.refundStatus) {
      backendParams.refundStatus = orderDataConverter.refundStatusToBackendCode(
        params.refundStatus as RefundStatusValue
      )
    }
    if (params.startTime) backendParams.startTime = params.startTime
    if (params.endTime) backendParams.endTime = params.endTime

    const res = await get<PageResponse<OrderRefundBackend>>('/v1/orders/refunds', backendParams)
    return {
      records: orderDataConverter.refundToFrontendList(res?.records),
      total: res?.total ?? 0,
      current: res?.current ?? (params.page || 1),
      size: res?.size ?? (params.size || 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 退款统计
   * GET /v1/orders/refunds/stats
   * 转换：后端金额（分）→ 前端金额（元/字符串）
   */
  async getStats(): Promise<OrderRefundStats> {
    const res = await get<OrderRefundStatsBackend>('/v1/orders/refunds/stats')
    return orderDataConverter.refundStatsToFrontend(res)
  },

  /**
   * 审批退款
   * POST /v1/orders/refunds/{refundId}/approve
   * @param refundId - 退款ID
   * @param approved - 是否通过（后端为 @RequestParam）
   * @param approveUserId - 审批人ID（后端为 @RequestParam）
   * @param options.body - 审核数据（调整后退款金额/审核意见）
   *
   * 注意：
   * - approved、approveUserId 走 query 参数（后端 @RequestParam）
   * - refundAmount、approveRemark 走请求体（避免 URL 暴露业务数据）
   * - refundAmount 单位为「分」（整数），由调用方通过 yuanToFen 转换
   */
  async approve(
    refundId: number,
    approved: boolean,
    approveUserId: number,
    body?: { refundAmount?: number; approveRemark?: string },
  ): Promise<void> {
    const options: { params: Record<string, unknown> } = {
      params: { approved, approveUserId },
    }
    await post(`/v1/orders/refunds/${refundId}/approve`, body, options)
  },
}

export default orderRefundApi
