/**
 * POS 销售入账流水与订单的关联解析
 *
 * 【来源】后端 PosSaleFinanceRecorder（backend/.../service/finance/PosSaleFinanceRecorder.java）
 * 在 POS 支付成功后写入资金流水，备注（fund_flows.remark）使用确定性格式：
 *
 *   `POS销售入账：<orderNumber>[orderId=<orderId>]`
 *
 * 例（DB 实测）：`POS销售入账：T20261023079[orderId=O1792759260016]`
 * 该串同时是后端幂等键（按 `[orderId=xxx]` 查重），因此格式稳定、可安全解析。
 *
 * 【为什么在前端解析】fund_flows 表没有 order_id 列，后端 FundFlowVO 也没有订单号字段，
 * 只能从 remark 反解；后续建议后端补 `orderId/orderNumber` 字段或提供
 * `GET /v1/finance/fund-flows/{flowId}/order` 关联查询（见交付说明）。
 */

/** 解析结果 */
export interface PosOrderRef {
  /** 订单号（如 T20261023079），可能为空（仅拿到 orderId 时） */
  orderNumber: string
  /** 订单ID（如 O1792759260016），可能为空（备注里没有 [orderId=...] 时） */
  orderId: string
  /** 是否命中「POS销售入账」前缀（用于文案） */
  fromPosSale: boolean
  /** 原始备注 */
  remark: string
}

/** POS 销售入账前缀（含全角/半角冒号） */
const POS_SALE_PREFIX_RE = /^\s*POS\s*销售入账\s*[：:]\s*/

/** 订单号：字母数字下划线短横线 */
const ORDER_NUMBER_RE = /([A-Za-z0-9_-]{4,64})/

/** 幂等键片段 [orderId=xxx]（允许前后空格、全角方括号） */
const ORDER_ID_TOKEN_RE = /\[\s*orderId\s*=\s*([A-Za-z0-9_-]{1,64})\s*[\]］]/

function normalizeId(value: string | null | undefined): string {
  if (value === null || value === undefined) return ''
  const text = String(value).trim()
  if (!text || text === 'undefined' || text === 'null' || text === 'NaN') return ''
  return text
}

/**
 * 从资金流水备注解析关联 POS 订单信息。
 *
 * @param remark 资金流水备注（后端 fund_flows.remark / 前端 FundFlow.summary 或 .remark）
 * @returns 解析结果；完全无法解析出订单号与 orderId 时返回 null
 */
export function parsePosOrderRemark(remark?: string | null): PosOrderRef | null {
  if (!remark) return null
  const text = String(remark)
  const idMatch = ORDER_ID_TOKEN_RE.exec(text)
  const orderId = normalizeId(idMatch?.[1])

  const isPosSale = POS_SALE_PREFIX_RE.test(text)
  let orderNumber = ''
  if (isPosSale) {
    // 前缀之后、[orderId=...] 之前的部分即订单号
    const tail = text.replace(POS_SALE_PREFIX_RE, '')
    const head = tail.split('[')[0] ?? ''
    orderNumber = normalizeId(ORDER_NUMBER_RE.exec(head)?.[1])
  }

  if (!orderNumber && !orderId) return null
  return { orderNumber, orderId, fromPosSale: isPosSale, remark: text }
}
