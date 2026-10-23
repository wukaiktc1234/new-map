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
import { get, post, silentGet } from '@/api/request'
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

// ============================================================
// 资金流水 → 关联 POS 订单明细穿透查询（财务资金管理详情弹窗使用）
// ============================================================
//
// 背景：fund_flows 表没有 order_id 列，后端 FundFlowVO 也没有订单号字段，
// 订单关联只存在于 remark（"POS销售入账：<orderNumber>[orderId=<orderId>]"）。
// 这里按 orderNumber/orderId 联查订单明细，多端点依次降级：
//   1. GET /v1/pos/orders/order/{orderNumber}   → OrderDetailDTO（金额已是元，仅需登录态）
//   2. GET /v1/orders/pos/{orderNumber}         → OrderVO（金额为分，需 order:view 权限）
//   3. GET /v1/orders/{orderId}                 → OrderVO（金额为分，需 order:view 权限）
// 全部失败时返回结构化原因（无权限/订单不存在/暂不可用），由调用方做优雅降级，不抛错、不弹全局提示。

/** 关联订单明细行（金额单位：元） */
export interface PosOrderTraceItem {
  name: string
  quantity: number
  /** 单价（元） */
  price: number
  /** 小计（元） */
  amount: number
}

/** 关联订单统一视图（金额单位：元） */
export interface PosOrderTrace {
  orderId: string
  orderNumber: string
  /** 下单时间（后端原样字符串，展示层自行格式化） */
  createTime: string
  /** 订单金额（元），后端未返回时为 null */
  totalAmount: number | null
  paymentMethod: string
  status: string
  tableNumber: string
  items: PosOrderTraceItem[]
  /** 数据来源：pos = PosOrderController，admin = OrderNewController */
  source: 'pos' | 'admin'
}

/** 联查结果：成功返回统一视图，失败返回可展示的原因 */
export type PosOrderTraceResult =
  | { ok: true; trace: PosOrderTrace }
  | { ok: false; reason: 'forbidden' | 'not-found' | 'unavailable'; message: string }

function toNumberOrNull(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

/** 分 → 元（保留 2 位） */
function fenToYuanValue(value: unknown): number | null {
  const num = toNumberOrNull(value)
  return num === null ? null : Math.round(num) / 100
}

/** 已是元的数值（保留 2 位） */
function yuanValue(value: unknown): number | null {
  const num = toNumberOrNull(value)
  return num === null ? null : Math.round(num * 100) / 100
}

function toText(value: unknown): string {
  if (value === null || value === undefined) return ''
  const text = String(value)
  return text === 'undefined' || text === 'null' ? '' : text
}

/** OrderDetailDTO（PosOrderController）→ 统一视图（金额已是元） */
function normalizePosOrderDetail(raw: Record<string, unknown>): PosOrderTrace {
  const rawItems = Array.isArray(raw.items) ? (raw.items as Record<string, unknown>[]) : []
  return {
    orderId: toText(raw.orderId),
    orderNumber: toText(raw.orderNumber),
    createTime: toText(raw.createTime),
    totalAmount: yuanValue(raw.totalAmount),
    paymentMethod: toText(raw.paymentMethod),
    status: toText(raw.status),
    tableNumber: toText(raw.tableNumber),
    source: 'pos',
    items: rawItems.map((item) => {
      const quantity = toNumberOrNull(item.quantity) ?? 0
      const price = yuanValue(item.price) ?? 0
      return {
        name: toText(item.name) || '未知菜品',
        quantity,
        price,
        amount: Math.round(price * quantity * 100) / 100,
      }
    }),
  }
}

/** OrderVO（OrderNewController）→ 统一视图（金额为分） */
function normalizeAdminOrder(raw: Record<string, unknown>): PosOrderTrace {
  const rawItems = Array.isArray(raw.items) ? (raw.items as Record<string, unknown>[]) : []
  const total = fenToYuanValue(raw.finalAmount) ?? fenToYuanValue(raw.totalAmount)
  return {
    orderId: toText(raw.orderId),
    orderNumber: toText(raw.orderCode) || toText(raw.orderNumber),
    createTime: toText(raw.createTime),
    totalAmount: total,
    paymentMethod: toText(raw.paymentMethodName) || toText(raw.paymentMethod),
    status: toText(raw.orderStatusName) || toText(raw.orderStatus),
    tableNumber: toText(raw.tableName),
    source: 'admin',
    items: rawItems.map((item) => {
      const quantity = toNumberOrNull(item.quantity) ?? 0
      const price = fenToYuanValue(item.unitPrice) ?? 0
      const amount = fenToYuanValue(item.amount) ?? Math.round(price * quantity * 100) / 100
      return { name: toText(item.productName) || '未知菜品', quantity, price, amount }
    }),
  }
}

/** 把接口错误文案归类为可展示的降级原因 */
function classifyOrderError(message: string): PosOrderTraceResult {
  if (/权限|403/.test(message)) {
    return { ok: false, reason: 'forbidden', message: '无权限查看该 POS 订单明细' }
  }
  if (/不存在|404/.test(message)) {
    return { ok: false, reason: 'not-found', message: '关联订单不存在（可能已归档或被清理）' }
  }
  return { ok: false, reason: 'unavailable', message: '订单明细暂不可用，请稍后重试' }
}

function errorText(error: unknown): string {
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: unknown }).message)
  }
  return ''
}

/**
 * 按订单号/订单ID 联查 POS 订单明细（多端点自动降级，绝不对调用方抛错）
 *
 * @param orderNumber 订单号（remark 解析所得，可为空）
 * @param orderId 订单ID（remark 解析所得，可为空）
 */
export async function getPosOrderTrace(
  orderNumber?: string | null,
  orderId?: string | null,
): Promise<PosOrderTraceResult> {
  const number = toText(orderNumber).trim()
  const id = toText(orderId).trim()
  if (!number && !id) {
    return { ok: false, reason: 'not-found', message: '该流水未关联 POS 订单号' }
  }

  let lastMessage = ''

  // 顺序很关键：管理端（财务中心）用户通常没有 POS 收银端点权限，
  // 先打 /v1/orders/pos/{no} 可避免产生一堆 403 console 噪音；POS 端用户再走第一条兜底。
  if (number) {
    try {
      const raw = await silentGet<Record<string, unknown>>(
        `/v1/orders/pos/${encodeURIComponent(number)}`,
      )
      if (raw && typeof raw === 'object') {
        return { ok: true, trace: normalizeAdminOrder(raw) }
      }
      lastMessage = '订单接口未返回数据'
    } catch (error) {
      lastMessage = errorText(error)
    }
  }

  if (id) {
    try {
      const raw = await silentGet<Record<string, unknown>>(
        `/v1/orders/${encodeURIComponent(id)}`,
      )
      if (raw && typeof raw === 'object') {
        return { ok: true, trace: normalizeAdminOrder(raw) }
      }
      lastMessage = lastMessage || '订单接口未返回数据'
    } catch (error) {
      lastMessage = errorText(error) || lastMessage
    }
  }

  if (number) {
    try {
      const raw = await silentGet<Record<string, unknown>>(
        `/v1/pos/orders/order/${encodeURIComponent(number)}`,
      )
      if (raw && typeof raw === 'object') {
        return { ok: true, trace: normalizePosOrderDetail(raw) }
      }
    } catch (error) {
      lastMessage = errorText(error) || lastMessage
    }
  }

  return classifyOrderError(lastMessage)
}

