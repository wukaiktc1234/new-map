/**
 * 期末结账（会计期间）API 封装。
 *
 * 契约（fund-500 并行实现，前端按此对接）：
 * - GET  /v1/finance/periods                      期间列表（含结账状态/账户数/期初期末合计）
 * - GET  /v1/finance/periods/{code}               期间明细（逐账户 opening/income/expense/closing/笔数/校验）
 * - POST /v1/finance/periods/{code}/close         结账
 * - POST /v1/finance/periods/{code}/reopen        反结账（body { reason }）
 *
 * 金额单位：**分**。前端只展示接口原值（`data-*-fen` 属性也是原值），不重算。
 *
 * 字段兼容：本模块对常见命名差异做归一（periodCode/code、isClosed/closed、
 * closeUserId/closedBy、closeTime/closedAt、closingTotal(Fen)/closingFen…），
 * 便于后端定型后无需改页面。
 */
import { get, post } from '@/api/request'

/** 逐账户结账明细 */
export interface PeriodAccountVO {
  accountId: number | null
  accountName: string | null
  /** 分 */
  openingFen: number | null
  /** 分 */
  incomeFen: number | null
  /** 分 */
  expenseFen: number | null
  /** 分 */
  closingFen: number | null
  txnCount: number | null
  /** 试算平衡是否通过（false → 行标红） */
  trialBalancePassed: boolean | null
  /** 结账后是否仍有写入（true → 提示「结账后仍有写入」并标红） */
  dirty: boolean | null
}

/** 期间三态 */
export type PeriodStatus = 'OPEN' | 'CLOSED' | 'REOPENED'

/** 会计期间 */
export interface PeriodVO {
  /** 期间编码（yyyy-MM 或 yyyy-Qn 等，作为 url 片段） */
  periodCode: string
  periodName: string | null
  startDate: string | null
  endDate: string | null
  /** 归一后的三态：OPEN / CLOSED / REOPENED（P4-4 补：不再只看 isClosed） */
  status: PeriodStatus
  /** 是否已结账（= status === 'CLOSED'） */
  closed: boolean
  /** 是否已反结账（= status === 'REOPENED'） */
  reopened: boolean
  /** 后端原始状态值（数字或字符串，仅调试用） */
  rawStatus: string | number | null
  closedBy: number | null
  closedAt: string | null
  /** 反结账时间 / 原因（REOPENED 徽标 tooltip 用） */
  reopenAt: string | null
  reopenReason: string | null
  accountCount: number | null
  /** 分：期初合计 */
  openingTotalFen: number | null
  /** 分：期末合计 */
  closingTotalFen: number | null
  accounts: PeriodAccountVO[]
}

/** 接口错误（保留后端 message 原文，供页面 period-error 展示） */
export class PeriodApiError extends Error {
  readonly forbidden: boolean

  constructor(message: string, forbidden = false) {
    super(message)
    this.name = 'PeriodApiError'
    this.forbidden = forbidden
  }
}

function toPeriodError(error: unknown): PeriodApiError {
  const message = error instanceof Error ? error.message : String(error)
  return new PeriodApiError(message, /403|没有权限|无权|禁止/.test(message))
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function str(value: unknown): string | null {
  if (typeof value === 'string' && value.trim() !== '') return value
  if (typeof value === 'number') return String(value)
  return null
}

function bool(value: unknown): boolean | null {
  return typeof value === 'boolean' ? value : null
}

function pick<T>(obj: Record<string, unknown>, keys: string[]): T | null {
  for (const key of keys) {
    const value = obj[key]
    if (value !== undefined && value !== null) return value as T
  }
  return null
}

function normalizeAccount(raw: Record<string, unknown>): PeriodAccountVO {
  return {
    accountId: num(pick(raw, ['accountId', 'id'])),
    accountName: str(pick(raw, ['accountName', 'name'])),
    openingFen: num(pick(raw, ['openingFen', 'openingBalanceFen', 'openingFenAmount', 'opening'])),
    incomeFen: num(pick(raw, ['incomeFen', 'income', 'inflowFen'])),
    expenseFen: num(pick(raw, ['expenseFen', 'expense', 'outflowFen'])),
    closingFen: num(pick(raw, ['closingFen', 'closingBalanceFen', 'closing'])),
    txnCount: num(pick(raw, ['txnCount', 'transactionCount', 'flowCount', 'count'])),
    trialBalancePassed: bool(pick(raw, ['trialBalancePassed', 'balanced', 'trialBalanceOk'])),
    dirty: bool(pick(raw, ['dirty', 'hasDirtyWrites', 'dirtyAfterClose'])),
  }
}

/**
 * 归一期间状态（P4-4 补）：优先用后端 `status`（OPEN / CLOSED / REOPENED），
 * 缺失时退化到 `reopened` → `isClosed` → 数字状态码，**不再只看 isClosed**。
 */
function normalizeStatus(raw: Record<string, unknown>): { status: PeriodStatus; rawStatus: string | number | null } {
  const rawStatus = pick<unknown>(raw, ['status', 'periodStatus']) as string | number | null
  const text = typeof rawStatus === 'string' ? rawStatus.toUpperCase() : ''
  if (text === 'OPEN' || text === 'CLOSED' || text === 'REOPENED') {
    return { status: text as PeriodStatus, rawStatus }
  }
  if (bool(pick<unknown>(raw, ['reopened', 'isReopened'])) === true) {
    return { status: 'REOPENED', rawStatus }
  }
  if (bool(pick<unknown>(raw, ['closed', 'isClosed'])) === true) {
    return { status: 'CLOSED', rawStatus }
  }
  if (rawStatus === 2 || String(rawStatus) === '2') {
    return { status: 'CLOSED', rawStatus }
  }
  return { status: 'OPEN', rawStatus }
}

function normalizePeriod(raw: Record<string, unknown>): PeriodVO {
  const accountsRaw = pick<unknown>(raw, ['accounts', 'accountRows', 'details'])
  const accounts = Array.isArray(accountsRaw) ? accountsRaw.map((item) => normalizeAccount(item as Record<string, unknown>)) : []
  const { status, rawStatus } = normalizeStatus(raw)
  return {
    periodCode: str(pick(raw, ['periodCode', 'code', 'period', 'period_key'])) ?? '',
    periodName: str(pick(raw, ['periodName', 'name'])),
    startDate: str(pick(raw, ['startDate', 'periodStart', 'beginDate'])),
    endDate: str(pick(raw, ['endDate', 'periodEnd', 'finishDate'])),
    status,
    closed: status === 'CLOSED',
    reopened: status === 'REOPENED',
    rawStatus,
    closedBy: num(pick(raw, ['closedBy', 'closeUserId', 'closedByUserId'])),
    closedAt: str(pick(raw, ['closedAt', 'closeTime', 'closedTime'])),
    reopenAt: str(pick(raw, ['reopenAt', 'reopenTime', 'reopenedAt'])),
    reopenReason: str(pick(raw, ['reopenReason', 'reopenedReason'])),
    accountCount: num(pick(raw, ['accountCount', 'accountsCount', 'accountNum'])) ?? (accounts.length || null),
    openingTotalFen: num(pick(raw, ['openingTotalFen', 'openingTotal', 'openingFen'])),
    closingTotalFen: num(pick(raw, ['closingTotalFen', 'closingTotal', 'closingFen'])),
    accounts,
  }
}

function normalizeList(data: unknown): PeriodVO[] {
  if (Array.isArray(data)) return data.map((item) => normalizePeriod(item as Record<string, unknown>))
  if (data && typeof data === 'object') {
    const obj = data as Record<string, unknown>
    const records = pick<unknown>(obj, ['records', 'list', 'rows', 'items'])
    if (Array.isArray(records)) return records.map((item) => normalizePeriod(item as Record<string, unknown>))
  }
  return []
}

/** 列表 */
export async function listPeriods(): Promise<PeriodVO[]> {
  try {
    const data = await get<unknown>('/v1/finance/periods')
    return normalizeList(data)
  } catch (error) {
    throw error instanceof PeriodApiError ? error : toPeriodError(error)
  }
}

/** 明细（逐账户）；失败时调用方可回退到列表行内联 accounts */
export async function getPeriodDetail(periodCode: string): Promise<PeriodVO> {
  try {
    const data = await get<Record<string, unknown> | null>(
      `/v1/finance/periods/${encodeURIComponent(periodCode)}`,
    )
    if (!data) throw new PeriodApiError('期间明细为空')
    return normalizePeriod(data)
  } catch (error) {
    throw error instanceof PeriodApiError ? error : toPeriodError(error)
  }
}

/** 结账 */
export async function closePeriod(periodCode: string): Promise<void> {
  try {
    await post<unknown>(`/v1/finance/periods/${encodeURIComponent(periodCode)}/close`)
  } catch (error) {
    // 后端 message（含 OPEN 差异条数 / 账户差额等具体原因）原样抛出，页面直接展示
    throw error instanceof PeriodApiError ? error : toPeriodError(error)
  }
}

/** 反结账（reason 必填，≥10 字由前端校验，后端二次校验） */
export async function reopenPeriod(periodCode: string, reason: string): Promise<void> {
  try {
    await post<unknown>(`/v1/finance/periods/${encodeURIComponent(periodCode)}/reopen`, { reason })
  } catch (error) {
    throw error instanceof PeriodApiError ? error : toPeriodError(error)
  }
}

export const financePeriodApi = {
  listPeriods,
  getPeriodDetail,
  closePeriod,
  reopenPeriod,
}

export default financePeriodApi
