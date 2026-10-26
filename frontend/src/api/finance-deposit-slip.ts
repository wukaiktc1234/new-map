/**
 * 现金缴款单 API（N19-3b 消费端）
 *
 * 后端（backend/.../controller/finance/DepositSlipController.java）：
 * - GET  /v1/finance/deposit-slips                  列表（IPage<DepositSlipVO>）
 * - GET  /v1/finance/deposit-slips/{slipId}         详情
 * - POST /v1/finance/deposit-slips/{slipId}/confirm-attribution  人工确认退款归属班次
 *        body: { refundRecordId, shiftId, reason } → 返回重算后的 DepositSlipVO
 *
 * N19-3b 新增消费字段（后端已就绪）：
 * - refundComponentFen      退款构成（分）
 * - differenceNetFen        净值 = actual − (expected − refundComponent)（分）
 * - missingRefundTimeCount  退款 refund_time 为空、未计入本单的笔数
 *
 * 金额一律「分」，展示层 `formatFen`。
 */
import { get, post, silentGet } from '@/api/request'

/** 缴款单视图（字段与后端 DepositSlipVO 对应） */
export interface DepositSlipVO {
  slipId: number
  slipNo: string
  businessDate: string | null
  storeId: number | null
  shiftId: number | null
  cashierEmployeeId: number | null
  cashierName: string | null
  collectorUserId: number | null
  collectorName: string | null
  /** 分 */
  expectedAmount: number | null
  /** 分 */
  actualAmount: number | null
  /** 分：应交 − 实收（正=短款，负=长款），仅记录在单据上 */
  differenceAmount: number | null
  /** 分：本次存行中属于退款/冲回的金额（N19-2） */
  refundComponentFen: number | null
  /** 分：净值 = actual − (expected − refundComponent)（N19-2） */
  differenceNetFen: number | null
  /**
   * 后端口径的四项提示计数（**前端不再自算**，只消费后端值）：
   * - stillOpenCount：班次窗口未结束的数量（A39 口径）
   * - staleCount：本单窗口内存在 refund_time > slip.create_time 的退款数量（A39 口径）
   * - legacyNullCount：历史单据缺少 N19-2 新字段的数量
   * - missingRefundTimeCount：退款 refund_time 为空、未计入本单的笔数
   * 字段缺失或为 0 → 不显示对应 chip。
   */
  stillOpenCount: number | null
  staleCount: number | null
  legacyNullCount: number | null
  missingRefundTimeCount: number | null
  cashAccountId: number | null
  bankAccountId: number | null
  depositBankTxnId: number | null
  depositRefNo: string | null
  status: string | null
  submittedAt: string | null
  receivedAt: string | null
  depositedAt: string | null
  verifiedAt: string | null
  closedAt: string | null
  reopenOf: number | null
  remark: string | null
  createUserId: number | null
  createdBy: string | null
  createTime: string | null
  updateTime: string | null
  /**
   * 待确认归属的退款（后端若在详情里内联返回则直接用；
   * 字段名容错：pendingRefunds / boundaryRefunds / pendingAttributionRefunds / refundsPending）
   */
  pendingRefunds?: PendingAttributionRefundVO[] | null
  boundaryRefunds?: PendingAttributionRefundVO[] | null
  pendingAttributionRefunds?: PendingAttributionRefundVO[] | null
  refundsPending?: PendingAttributionRefundVO[] | null
}

/** 候选班次 */
export interface ShiftCandidateVO {
  shiftId: number
  /** 展示名（上一班次 / 本班次 / 下一班次 或 具体时间窗文案） */
  label?: string | null
  windowStart?: string | null
  windowEnd?: string | null
}

/** 待确认归属的退款行 */
export interface PendingAttributionRefundVO {
  refundRecordId: number
  refundNo?: string | null
  orderNumber?: string | null
  /** 分 */
  amountFen: number | null
  refundTime?: string | null
  /** 候选班次（上一/本/下一） */
  candidateShifts?: ShiftCandidateVO[] | null
}

/** 列表查询 */
export interface DepositSlipQuery {
  status?: string | null
  storeId?: number | null
  shiftId?: number | null
  businessDateFrom?: string | null
  businessDateTo?: string | null
  current?: number
  size?: number
}

/** 四项提示计数（后端口径） */
export interface DepositSlipHints {
  stillOpenCount: number | null
  staleCount: number | null
  legacyNullCount: number | null
  missingRefundTimeCount: number | null
}

export interface DepositSlipPage {
  records: DepositSlipVO[]
  total: number
  current: number
  size: number
  /** 列表级汇总计数（后端若在分页根节点返回则优先使用；缺失时逐单字段求和） */
  summary: DepositSlipHints | null
}

/** 归属确认入参（与后端 Map body 对齐：refundRecordId / shiftId / reason） */
export interface ConfirmAttributionPayload {
  refundRecordId: number
  shiftId: number
  reason: string
}

function compactParams(params: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (typeof value === 'string' && value.trim() === '') return
    result[key] = value
  })
  return result
}

function toNum(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function normalizeSlip(raw: Record<string, unknown>): DepositSlipVO {
  const pending = (raw.pendingRefunds ?? raw.boundaryRefunds ?? raw.pendingAttributionRefunds ?? raw.refundsPending ?? null) as
    | PendingAttributionRefundVO[]
    | null
  return {
    slipId: Number(raw.slipId ?? 0),
    slipNo: String(raw.slipNo ?? ''),
    businessDate: (raw.businessDate as string) ?? null,
    storeId: toNum(raw.storeId),
    shiftId: toNum(raw.shiftId),
    cashierEmployeeId: toNum(raw.cashierEmployeeId),
    cashierName: (raw.cashierName as string) ?? null,
    collectorUserId: toNum(raw.collectorUserId),
    collectorName: (raw.collectorName as string) ?? null,
    expectedAmount: toNum(raw.expectedAmount),
    actualAmount: toNum(raw.actualAmount),
    differenceAmount: toNum(raw.differenceAmount),
    refundComponentFen: toNum(raw.refundComponentFen),
    differenceNetFen: toNum(raw.differenceNetFen),
    stillOpenCount: toNum(raw.stillOpenCount),
    staleCount: toNum(raw.staleCount),
    legacyNullCount: toNum(raw.legacyNullCount),
    missingRefundTimeCount: toNum(raw.missingRefundTimeCount),
    cashAccountId: toNum(raw.cashAccountId),
    bankAccountId: toNum(raw.bankAccountId),
    depositBankTxnId: toNum(raw.depositBankTxnId),
    depositRefNo: (raw.depositRefNo as string) ?? null,
    status: (raw.status as string) ?? null,
    submittedAt: (raw.submittedAt as string) ?? null,
    receivedAt: (raw.receivedAt as string) ?? null,
    depositedAt: (raw.depositedAt as string) ?? null,
    verifiedAt: (raw.verifiedAt as string) ?? null,
    closedAt: (raw.closedAt as string) ?? null,
    reopenOf: toNum(raw.reopenOf),
    remark: (raw.remark as string) ?? null,
    createUserId: toNum(raw.createUserId),
    createdBy: (raw.createdBy as string) ?? null,
    createTime: (raw.createTime as string) ?? null,
    updateTime: (raw.updateTime as string) ?? null,
    pendingRefunds: Array.isArray(pending) ? pending : null,
  }
}

/** 列表 */
export async function listDepositSlips(query: DepositSlipQuery = {}): Promise<DepositSlipPage> {
  const params = compactParams({
    status: query.status,
    storeId: query.storeId,
    shiftId: query.shiftId,
    businessDateFrom: query.businessDateFrom,
    businessDateTo: query.businessDateTo,
    current: query.current ?? 1,
    size: query.size ?? 20,
  })
  const data = await get<Record<string, unknown> | null>('/v1/finance/deposit-slips', params)
  const records = Array.isArray(data?.records)
    ? (data!.records as Array<Record<string, unknown>>).map(normalizeSlip)
    : []
  const summaryRaw = (data?.summary ?? data?.counters ?? data?.hints ?? null) as Record<string, unknown> | null
  const summary = summaryRaw
    ? {
        stillOpenCount: toNum(summaryRaw.stillOpenCount),
        staleCount: toNum(summaryRaw.staleCount),
        legacyNullCount: toNum(summaryRaw.legacyNullCount),
        missingRefundTimeCount: toNum(summaryRaw.missingRefundTimeCount),
      }
    : (typeof data?.stillOpenCount === 'number'
        ? {
            stillOpenCount: toNum(data.stillOpenCount),
            staleCount: toNum(data.staleCount),
            legacyNullCount: toNum(data.legacyNullCount),
            missingRefundTimeCount: toNum(data.missingRefundTimeCount),
          }
        : null)
  return {
    records,
    total: toNum(data?.total) ?? 0,
    current: toNum(data?.current) ?? Number(params.current ?? 1),
    size: toNum(data?.size) ?? Number(params.size ?? 20),
    summary,
  }
}

/** 详情 */
export async function getDepositSlip(slipId: number): Promise<DepositSlipVO> {
  const data = await get<Record<string, unknown> | null>(
    `/v1/finance/deposit-slips/${encodeURIComponent(String(slipId))}`,
  )
  if (!data) throw new Error('缴款单详情为空')
  return normalizeSlip(data)
}

/**
 * 待确认归属的退款列表。
 *
 * 契约（fund-500）：`GET /v1/finance/deposit-slips/{slipId}/pending-refunds`
 * → 200 + `pendingRefunds[]`（含 candidateShifts）；**空时返回 200 + 空数组**，页面据此隐藏区块。
 * 详情若已内联 pendingRefunds，则直接用内联数据（少一次请求）；
 * 端点暂未上线时用 silentGet 静默降级为空数组（不弹提示）。
 */
export async function listPendingAttributionRefunds(
  slipId: number,
  detail?: DepositSlipVO | null,
): Promise<PendingAttributionRefundVO[]> {
  const inline = detail?.pendingRefunds
  if (Array.isArray(inline) && inline.length > 0) return inline
  try {
    // 用 silentGet：该端点为可选能力，未提供时不要弹「资源不存在」噪音
    const data = await silentGet<unknown>(`/v1/finance/deposit-slips/${encodeURIComponent(String(slipId))}/pending-refunds`)
    if (Array.isArray(data)) return data as PendingAttributionRefundVO[]
    const records = (data as Record<string, unknown> | null)?.records
    return Array.isArray(records) ? (records as PendingAttributionRefundVO[]) : []
  } catch {
    // 端点未提供时安静降级（区块隐藏），不打扰用户
    return []
  }
}

/** 人工确认退款归属班次（成功后返回重算过的单据） */
export async function confirmAttribution(
  slipId: number,
  payload: ConfirmAttributionPayload,
): Promise<DepositSlipVO> {
  const data = await post<Record<string, unknown> | null>(
    `/v1/finance/deposit-slips/${encodeURIComponent(String(slipId))}/confirm-attribution`,
    { refundRecordId: payload.refundRecordId, shiftId: payload.shiftId, reason: payload.reason },
  )
  if (!data) throw new Error('归属确认未返回单据')
  return normalizeSlip(data)
}

export const financeDepositSlipApi = {
  listDepositSlips,
  getDepositSlip,
  listPendingAttributionRefunds,
  confirmAttribution,
}

export default financeDepositSlipApi
