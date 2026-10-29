import request from '@/api/request'

/**
 * 交班异常单 & 班次统计 API
 *
 * 路径分两组（后端已定：不提供 /v1/pos/shift/exceptions/** 别名，避免两套入口漂移）：
 *   只读/扫码（BASE）：
 *     GET  /v1/pos/shift/stats?terminalId=             → ShiftStats（账目唯一真相）
 *     GET  /v1/pos/shift/exceptions?terminalId=&tab=   → ExceptionOrder[]
 *     POST /v1/pos/shift/exceptions/{orderId}/scan     → 生成扫码支付（幂等，返回 payNo + 二维码）
 *     GET  /v1/pos/shift/scan/{payNo}                  → 扫码支付状态
 *   写操作（WRITE_BASE，后端已实现）：
 *     POST /v1/pos/exceptions/{orderId}/void           → 作废（幂等）
 *     POST /v1/pos/exceptions/{orderId}/resolve        → 补收款（现金，幂等）
 *
 * 写接口入参（后端契约）：void → { reason, note? }；resolve → { payType, amountFen, cashReceivedFen? }
 * 响应统一含 replayed:boolean（首次 false、幂等重放 true），前端据此可跳过重复提示。
 * 所有写接口都带 Idempotency-Key 头：网络重试 / 用户连点都只会在后端落一次。
 */
const BASE = '/v1/pos/shift'
/** 写操作（作废/补收款）走已实现的 /v1/pos/exceptions，不加 /shift 前缀 */
const WRITE_BASE = '/v1/pos/exceptions'

export type ExceptionType = 'unpaid' | 'cancelled' | 'no_txn' | 'amount_mismatch' | 'other'
export type ExceptionTab = 'pending' | 'processed'

export interface ShiftStats {
  /** 现金收入（后端口径，前端不再自减） */
  cashIn: number
  /** 现金退款 */
  cashRefund: number
  /** 应上交现金 */
  handoverCash: number
  /** 钱箱应有（含备用金） */
  drawerExpected: number
  /** 备用金 */
  openingCash: number
  /** 待处理异常单数量：交班按钮的唯一开关（口径=待处理的异常单） */
  pendingExceptionCount: number
  /**
   * 历史异常单总数（后端新字段）：口径=订单明细里被判为异常、不计入营业额的订单。
   * 与 pendingExceptionCount **不同义**，只做说明，不参与交班门禁。
   */
  exceptionOrderTotal?: number
  /** 净营业额 */
  netRevenue: number
  matched: number
  refunded: number
  /** 统计生成时间，用于判断是否过期 */
  generatedAt?: string
}

export interface ExceptionOrder {
  orderId: string
  orderNumber: string
  createTime: string
  amount: number
  paymentMethod?: string
  type: ExceptionType
  typeLabel: string
  employeeName?: string
  /** 已处理态才有：voided / resolved / resolved_scan */
  handledAs?: 'voided' | 'resolved' | 'resolved_scan'
  handledAt?: string
  handledBy?: string
}

export interface ScanPayment {
  payNo: string
  /** 二维码内容（前端渲染成图） */
  qrContent: string
  /** 过期时间戳（ms） */
  expiresAt: number
}

export interface ScanStatus {
  status: 'pending' | 'paid' | 'closed' | 'expired'
  paidAt?: string
}

/** 写操作返回：后端契约含 replayed（首次 false / 幂等重放 true） */
export interface ShiftWriteResult {
  replayed?: boolean
  [key: string]: unknown
}

const idempotencyHeader = (key: string) => ({ headers: { 'Idempotency-Key': key } })

export const shiftApi = {
  getStats(terminalId: string): Promise<ShiftStats | null> {
    return request.get(`${BASE}/stats`, { params: { terminalId } }) as unknown as Promise<ShiftStats | null>
  },

  listExceptions(terminalId: string, tab: ExceptionTab): Promise<ExceptionOrder[]> {
    return request.get(`${BASE}/exceptions`, {
      params: { terminalId, tab }
    }) as unknown as Promise<ExceptionOrder[]>
  },

  voidOrder(orderId: string, payload: { reason: string; note?: string }, idempotencyKey: string) {
    return request.post<ShiftWriteResult>(
      `${WRITE_BASE}/${orderId}/void`,
      // ⚠ 后端从**请求体**读 idempotencyKey（PosExceptionController.voidOrder → body.get("idempotencyKey")），
      //   只发 Header 会被判「idempotencyKey 必填」。Header 同时保留，便于网关层去重。
      { ...payload, idempotencyKey },
      idempotencyHeader(idempotencyKey),
    ) as unknown as Promise<ShiftWriteResult>
  },

  resolveCash(
    orderId: string,
    payload: { payType: 'CASH'; amountFen: number; cashReceivedFen?: number },
    idempotencyKey: string,
  ) {
    return request.post<ShiftWriteResult>(
      `${WRITE_BASE}/${orderId}/resolve`,
      // ⚠ 同上：idempotencyKey 必须在 body 里（只发 Header → 400 idempotencyKey 必填）
      { ...payload, idempotencyKey },
      idempotencyHeader(idempotencyKey),
    ) as unknown as Promise<ShiftWriteResult>
  },

  createScanPayment(orderId: string, idempotencyKey: string): Promise<ScanPayment> {
    return request.post(`${BASE}/exceptions/${orderId}/scan`, {}, idempotencyHeader(idempotencyKey)) as unknown as Promise<ScanPayment>
  },

  queryScanStatus(payNo: string): Promise<ScanStatus> {
    return request.get(`${BASE}/scan/${payNo}`) as unknown as Promise<ScanStatus>
  }
}

export default shiftApi
