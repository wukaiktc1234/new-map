/**
 * 订单查询/管理 API
 * 对应后端: OrderNewController (/v1/orders)
 *
 * 使用标准 get/post 请求：
 * - 错误由 request 拦截器统一处理
 * - GET 请求直接传 params 对象，禁止嵌套
 *
 * 注意：本文件还包含 POS 订单支付接口（pay），
 * 该接口对应后端 PosOrderController (/v1/pos/orders)，使用 post 发送。
 */
import { get, post } from '@/api/request'
import { orderDataConverter } from './converters'
import type {
  OrderBackend,
  Order,
  OrderQueryForm,
  OrderDetail,
  OrderStats,
  OrderStatsBackend,
  PageResponse,
  OrderStatusValue,
  PayRequest,
  PayResponse,
} from '@/types/order'

export const orderApi = {
  /**
   * 分页查询订单
   * GET /v1/orders
   * 转换：前端查询参数（语义化状态）→ 后端编码；后端响应（分/数字）→ 前端（元/字符串）
   */
  async getList(params: OrderQueryForm): Promise<PageResponse<Order>> {
    const backendParams: Record<string, unknown> = {
      page: params.page || 1,
      size: params.size || 10,
    }
    if (params.orderCode) backendParams.orderCode = params.orderCode
    if (params.orderType != null) backendParams.orderType = params.orderType
    if (params.orderStatus) {
      backendParams.orderStatus = orderDataConverter.orderStatusToBackendCode(
        params.orderStatus as OrderStatusValue
      )
    }
    // TODO: paymentStatus 应改为语义化字符串（unpaid/partial/paid/refunded），通过 DataConverter 转换
    if (params.paymentStatus != null) backendParams.paymentStatus = params.paymentStatus
    if (params.customerName) backendParams.customerName = params.customerName
    if (params.customerPhone) backendParams.customerPhone = params.customerPhone
    if (params.startTime) backendParams.startTime = params.startTime
    if (params.endTime) backendParams.endTime = params.endTime
    if (params.minAmount) backendParams.minAmount = orderDataConverter.yuanToFen(params.minAmount)
    if (params.maxAmount) backendParams.maxAmount = orderDataConverter.yuanToFen(params.maxAmount)

    const res = await get<PageResponse<OrderBackend>>('/v1/orders', backendParams)
    return {
      records: orderDataConverter.toFrontendList(res?.records),
      total: res?.total ?? 0,
      current: res?.current ?? (params.page || 1),
      size: res?.size ?? (params.size || 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 分页查询POS终端订单
   * GET /v1/orders/pos
   * 查询POS收银端创建的订单（orders_legacy 表），返回格式与 getList 一致。
   * 解决：POS收银端订单无法在管理端订单中心查看的问题。
   */
  async getPosList(params: OrderQueryForm): Promise<PageResponse<Order>> {
    const backendParams: Record<string, unknown> = {
      page: params.page || 1,
      size: params.size || 10,
    }
    if (params.orderCode) backendParams.orderCode = params.orderCode
    if (params.orderType != null) backendParams.orderType = params.orderType
    if (params.orderStatus) {
      backendParams.orderStatus = orderDataConverter.orderStatusToBackendCode(
        params.orderStatus as OrderStatusValue
      )
    }
    if (params.customerName) backendParams.customerName = params.customerName
    if (params.startTime) backendParams.startTime = params.startTime
    if (params.endTime) backendParams.endTime = params.endTime
    if (params.minAmount) backendParams.minAmount = orderDataConverter.yuanToFen(params.minAmount)
    if (params.maxAmount) backendParams.maxAmount = orderDataConverter.yuanToFen(params.maxAmount)
    if (params.storeId) backendParams.storeId = params.storeId

    const res = await get<PageResponse<OrderBackend>>('/v1/orders/pos', backendParams)
    return {
      records: orderDataConverter.toFrontendList(res?.records),
      total: res?.total ?? 0,
      current: res?.current ?? (params.page || 1),
      size: res?.size ?? (params.size || 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 获取订单详情
   * GET /v1/orders/{orderId}
   * 返回：OrderDetail（含 items、payments、refunds 列表）
   */
  async getById(orderId: number): Promise<OrderDetail> {
    const res = await get<OrderBackend>(`/v1/orders/${orderId}`)
    return orderDataConverter.toFrontend(res)
  },

  /**
   * 获取POS终端订单详情（含菜品明细）
   * GET /v1/orders/pos/{orderNumber}
   * 用于管理端订单中心查看POS收银端订单的完整明细（数据追溯）。
   * 后端会查询 orders_legacy + order_items_legacy 表，返回格式与 getById 一致。
   *
   * @param orderNumber 订单编号（如 T20260713002），非订单ID
   */
  async getPosDetail(orderNumber: string): Promise<OrderDetail> {
    const res = await get<OrderBackend>(`/v1/orders/pos/${orderNumber}`)
    return orderDataConverter.toFrontend(res)
  },

  /**
   * 取消订单
   * POST /v1/orders/{orderId}/cancel
   * @param cancelReason - 取消原因（可选，后端为 @RequestParam）
   */
  async cancel(orderId: number, cancelReason?: string): Promise<void> {
    const options: { params?: Record<string, unknown> } = {}
    if (cancelReason) options.params = { cancelReason }
    await post(`/v1/orders/${orderId}/cancel`, undefined, options)
  },

  /**
   * 获取今日销售统计
   * GET /v1/orders/today/statistics
   * 转换：后端金额（分）→ 前端金额（元/字符串）
   */
  async getTodayStats(): Promise<OrderStats> {
    const res = await get<OrderStatsBackend>('/v1/orders/today/statistics')
    return orderDataConverter.statsToFrontend(res)
  },

  /**
   * POS 订单支付
   * POST /v1/pos/orders/order/pay
   *
   * 对应后端 PosOrderController.payOrder（与 OrderNewController 不同），
   * 返回 OrderResultDTO（响应拦截器已自动提取 data）。
   *
   * 数据转换说明：
   * - paymentMethod：前端使用语义化字符串（'wechat'/'alipay'/'cash'/'card'/'balance'），
   *   后端 PayRequestDTO 通过正则同时接受中英文，无需 DataConverter 转换
   * - amount：后端为 BigDecimal（元，非分），前端直接传 number，无需元分转换
   * - orderId：后端为 String 类型，前端传字符串
   *
   * 使用 post（非静默）：支付失败需由全局拦截器弹出错误提示。
   *
   * @param data - 支付请求参数
   */
  async pay(data: PayRequest): Promise<PayResponse> {
    return post<PayResponse>('/v1/pos/orders/order/pay', data)
  },
}

export default orderApi
