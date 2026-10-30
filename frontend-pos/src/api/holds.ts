import request from '@/api/request'
import { fenToYuan, yuanToFen } from '@/utils/shiftStatus'

/**
 * POS 挂单 API（后端 2026-10-30 上线，flyway 019）
 *
 * 端点（servlet.context-path=/api，request 实例 baseURL 已是 /api，这里只写 /v1/...）：
 *   POST   /v1/pos/holds              body {cartData,totalAmountFen,terminalId,storeId?,clientHoldId?}
 *                                     header Idempotency-Key 或 body.clientHoldId（后端 body 优先）
 *   GET    /v1/pos/holds?terminalId=  → {list:[{id,orderNo,cartData,totalAmountFen,createTime,expireAt}], total}
 *   DELETE /v1/pos/holds/{id}?terminalId=   取消（CAS，已处理 → 409）
 *   POST   /v1/pos/holds/{id}/checkout?terminalId= 取出结账（CAS，已处理 → 409）
 *
 * ⚠ 实测修正（2026-10-30）：checkout / cancel **必须带 query 参数 terminalId**，
 *   放 body 里后端不认（会回「缺少必填参数：terminalId」）。
 *
 * ⚠ 金额单位铁律：后端 total_amount_fen 是**分**（bigint），前端购物车 totalAmount 是**元**。
 *   发请求前 yuanToFen()，解析响应时 fenToYuan()；两个函数来自 utils/shiftStatus.ts（含非数字守卫）。
 */

/** 后端返回的挂单（cartData 可能是 JSON 字符串） */
export interface BackendHold {
  id: number | string
  orderNo: string
  cartData: string | Record<string, unknown>
  totalAmountFen: number
  createTime: string
  expireAt: string
}

export interface HoldListResult {
  list: BackendHold[]
  total: number
}

export interface CreateHoldParams {
  cartData: Record<string, unknown>
  /** 前端购物车金额（**元**） */
  totalAmountYuan: number
  terminalId: string
  storeId?: number
  clientHoldId?: string
}

export interface CreateHoldResult {
  id: number | string
  orderNo: string
  expireAt: string
  createTime: string
}

/** cartData 可能是后端序列化后的字符串，统一转对象（解析失败给空对象，不让整个列表挂掉） */
export function parseCartData(raw: BackendHold['cartData']): Record<string, unknown> {
  if (!raw) return {}
  if (typeof raw === 'object') return raw as Record<string, unknown>
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {}
  } catch {
    return {}
  }
}

export const holdsApi = {
  /** 列表（后端按 employee_id + terminal_id 双条件隔离，必须带 terminalId） */
  async list(terminalId: string): Promise<HoldListResult> {
    const res = (await request.get('/v1/pos/holds', {
      params: { terminalId },
    })) as unknown as HoldListResult | null
    const list = Array.isArray(res?.list) ? res!.list : []
    return { list, total: typeof res?.total === 'number' ? res!.total : list.length }
  },

  /** 新建挂单：金额元→分 */
  async create(params: CreateHoldParams): Promise<CreateHoldResult> {
    const body: Record<string, unknown> = {
      cartData: params.cartData,
      totalAmountFen: yuanToFen(params.totalAmountYuan),
      terminalId: params.terminalId,
    }
    if (typeof params.storeId === 'number') body.storeId = params.storeId
    if (params.clientHoldId) body.clientHoldId = params.clientHoldId

    const headers: Record<string, string> = {}
    if (params.clientHoldId) headers['Idempotency-Key'] = params.clientHoldId

    return (await request.post('/v1/pos/holds', body, { headers })) as unknown as CreateHoldResult
  },

  /** 取消（CAS：已处理 → 业务码/HTTP 409） */
  async cancel(id: number | string, terminalId: string): Promise<void> {
    await request.delete(`/v1/pos/holds/${encodeURIComponent(String(id))}`, {
      params: { terminalId },
    })
  },

  /** 取出结账（CAS：已处理 → 业务码/HTTP 409） */
  async checkout(id: number | string, terminalId: string): Promise<Record<string, unknown> | null> {
    return (await request.post(
      `/v1/pos/holds/${encodeURIComponent(String(id))}/checkout`,
      {},
      { params: { terminalId } },
    )) as unknown as Record<string, unknown> | null
  },
}

/** 前端展示用的金额换算（统一走 fenToYuan，禁止裸 /100） */
export function holdAmountYuan(hold: BackendHold): number {
  return fenToYuan(hold.totalAmountFen)
}

export default holdsApi
