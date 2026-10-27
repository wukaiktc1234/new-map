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
  /** 分：班次备用金（= pos_shifts.opening_cash 元 × 100，差异 1） */
  reserveFen: number | null
  /** 分：计现额 = actual − reserve_fen（抽屉总额 − 备用金） */
  cashCountedFen: number | null
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
  /** 整单共享的候选班次（若后端在详情里也内联） */
  candidateShifts?: ShiftCandidateVO[] | null
}

/** 候选班次（后端：{ shiftId, label, startTime, endTime }） */
export interface ShiftCandidateVO {
  shiftId: number
  /** 展示名，如 `morning #325` */
  label?: string | null
  startTime?: string | null
  endTime?: string | null
}

/** 待确认归属的退款行 */
export interface PendingAttributionRefundVO {
  refundRecordId: number
  refundNo?: string | null
  orderNumber?: string | null
  /** 分 */
  amountFen: number | null
  refundTime?: string | null
  /** 行级候选班次（后端目前放顶层，行级为兼容保留） */
  candidateShifts?: ShiftCandidateVO[] | null
}

/**
 * 待确认归属载荷（后端真实响应结构）：
 * `{ pendingRefunds: [...], candidateShifts: [{shiftId,label,startTime,endTime}], missingRefundTimeCount: 0 }`
 * 三个字段都在 data 根节点上；candidateShifts 是**整单共享**的三选一候选，不是行级字段。
 */
export interface PendingAttributionPayload {
  pendingRefunds: PendingAttributionRefundVO[]
  candidateShifts: ShiftCandidateVO[]
  missingRefundTimeCount: number
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
    reserveFen: toNum(raw.reserveFen),
    cashCountedFen: toNum(raw.cashCountedFen),
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
export async function getPendingAttribution(
  slipId: number,
  detail?: DepositSlipVO | null,
): Promise<PendingAttributionPayload> {
  const empty: PendingAttributionPayload = { pendingRefunds: [], candidateShifts: [], missingRefundTimeCount: 0 }

  // 详情内联优先（少一次请求）
  const inlineRows = detail?.pendingRefunds ?? detail?.boundaryRefunds ?? detail?.pendingAttributionRefunds ?? null
  if (Array.isArray(inlineRows) && inlineRows.length > 0) {
    return {
      // 内联数据本身已是 VO 形态，直接透传（保留原字段）
      pendingRefunds: inlineRows as PendingAttributionRefundVO[],
      candidateShifts: (detail?.candidateShifts ?? []) as ShiftCandidateVO[],
      missingRefundTimeCount: detail?.missingRefundTimeCount ?? 0,
    }
  }

  try {
    // silentGet：端点暂不可用时不弹提示
    const data = await silentGet<Record<string, unknown> | null>(
      `/v1/finance/deposit-slips/${encodeURIComponent(String(slipId))}/pending-refunds`,
    )
    if (!data) return empty

    // 后端真实结构：data = { pendingRefunds, candidateShifts, missingRefundTimeCount }
    // （兼容历史上直接返回数组 / { records: [] } 的形态）
    if (Array.isArray(data)) {
      return { pendingRefunds: data.map(normalizePendingRefund), candidateShifts: [], missingRefundTimeCount: 0 }
    }
    const rowsRaw = Array.isArray(data.pendingRefunds)
      ? data.pendingRefunds
      : Array.isArray(data.records)
        ? data.records
        : []
    const shiftsRaw = Array.isArray(data.candidateShifts) ? data.candidateShifts : []
    return {
      pendingRefunds: (rowsRaw as Array<Record<string, unknown>>).map(normalizePendingRefund),
      candidateShifts: (shiftsRaw as Array<Record<string, unknown>>).map(normalizeShiftCandidate),
      missingRefundTimeCount: toNum(data.missingRefundTimeCount) ?? 0,
    }
  } catch {
    return empty
  }
}

/** 归一单条待确认退款（后端字段：refundRecordId / refundNo / amountFen / refundTime） */
function normalizePendingRefund(raw: Record<string, unknown>): PendingAttributionRefundVO {
  const rowCandidates = Array.isArray(raw.candidateShifts) ? raw.candidateShifts : []
  return {
    refundRecordId: Number(raw.refundRecordId ?? raw.id ?? 0),
    refundNo: (raw.refundNo as string) ?? (raw.refund_no as string) ?? null,
    orderNumber: (raw.orderNumber as string) ?? (raw.order_no as string) ?? null,
    amountFen: toNum(raw.amountFen) ?? toNum(raw.amount),
    refundTime: (raw.refundTime as string) ?? (raw.refund_time as string) ?? null,
    candidateShifts: (rowCandidates as Array<Record<string, unknown>>).map(normalizeShiftCandidate),
  }
}

/** 归一候选班次（后端字段：shiftId / label / startTime / endTime） */
function normalizeShiftCandidate(raw: Record<string, unknown>): ShiftCandidateVO {
  return {
    shiftId: Number(raw.shiftId ?? 0),
    label: (raw.label as string) ?? null,
    startTime: (raw.startTime as string) ?? null,
    endTime: (raw.endTime as string) ?? null,
  }
}


/* ==========================================================================
 * 差异 3：本日存行汇总（人工汇总模式）
 *
 * 端点（fund-500 落盘中）：GET /v1/finance/deposit-slips/daily-summary?date=yyyy-MM-dd
 * 返回：{ depositedSlips: [...], totalCashCountedFen, matchedFen, unmatchedFen,
 *         unmatchedCount, bankInflowFen }
 * 金额单位：分。
 * ========================================================================== */
export interface DepositDailySlipVO {
  slipId: number | null
  slipNo: string | null
  storeId: number | null
  shiftId: number | null
  /** 分：该单应存现金 */
  cashCountedFen: number | null
  /** 绑定的银行流水号（为空 = 未匹配） */
  bankFlowNo: string | null
  /** 分：该行对应的银行进账 */
  bankInflowFen: number | null
  /** 是否已绑定银行流水 */
  matched: boolean
  status: string | null
}

export interface DepositDailySummaryVO {
  date: string
  depositedSlips: DepositDailySlipVO[]
  /** 分：合计应存现金 */
  totalCashCountedFen: number | null
  /** 分：已绑银行流水合计 */
  matchedFen: number | null
  /** 分：未匹配银行流水合计 */
  unmatchedFen: number | null
  unmatchedCount: number
  /** 分：银行当日进账（多户歧义时为 null，**不是 0**） */
  bankInflowFen: number | null
  /**
   * 多户歧义：当日存行单跨多个基本户且未指定 accountId → true，此时 bankInflowFen=null。
   * 前端必须显式提示「请选择银行账户」，不得把 null 当 0 渲染。
   */
  bankInflowAccountAmbiguous: boolean
}

function normalizeDailySlip(raw: Record<string, unknown>): DepositDailySlipVO {
  const bankFlowNo = (raw.bankFlowNo as string)
    ?? (raw.depositRefNo as string)
    ?? (raw.bankTxnNo as string)
    ?? (raw.bankTransactionNo as string)
    ?? null
  const matchedRaw = raw.matched ?? raw.bound ?? raw.bankMatched
  const matched = typeof matchedRaw === 'boolean'
    ? matchedRaw
    : Boolean(bankFlowNo || raw.depositBankTxnId || raw.bankTransactionId)
  return {
    slipId: toNum(raw.slipId) ?? toNum(raw.id),
    slipNo: (raw.slipNo as string) ?? null,
    storeId: toNum(raw.storeId),
    shiftId: toNum(raw.shiftId),
    cashCountedFen: toNum(raw.cashCountedFen) ?? toNum(raw.totalCashFen) ?? toNum(raw.cashAmountFen) ?? toNum(raw.amountFen),
    bankFlowNo,
    bankInflowFen: toNum(raw.bankInflowFen) ?? toNum(raw.bankAmountFen) ?? toNum(raw.inflowFen),
    matched,
    status: (raw.status as string) ?? null,
  }
}

/** 本日存行汇总（date 默认由调用方给今天） */
export async function getDailySummary(date: string, accountId?: number | null): Promise<DepositDailySummaryVO> {
  const params: Record<string, unknown> = { date }
  if (accountId !== undefined && accountId !== null && (accountId as unknown) !== '') params.accountId = accountId
  const data = await get<Record<string, unknown> | null>('/v1/finance/deposit-slips/daily-summary', params)
  const rawRows = Array.isArray(data?.depositedSlips) ? data!.depositedSlips : []
  const rows = (rawRows as Array<Record<string, unknown>>).map(normalizeDailySlip)
  return {
    date: (data?.date as string) ?? date,
    depositedSlips: rows,
    totalCashCountedFen: toNum(data?.totalCashCountedFen),
    matchedFen: toNum(data?.matchedFen),
    unmatchedFen: toNum(data?.unmatchedFen),
    // 后端未给 unmatchedCount 时，按行上的 matched 兜底计数（仍是后端行数据）
    unmatchedCount: toNum(data?.unmatchedCount) ?? rows.filter((r) => !r.matched).length,
    bankInflowFen: toNum(data?.bankInflowFen),
    bankInflowAccountAmbiguous: Boolean(data?.bankInflowAccountAmbiguous ?? data?.bank_inflow_account_ambiguous),
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
  getPendingAttribution,
  confirmAttribution,
  getDailySummary,
}

export default financeDepositSlipApi
