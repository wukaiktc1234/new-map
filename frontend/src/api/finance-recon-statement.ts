/**
 * 调节表（S3–S9）API 封装（P3-3）。
 *
 * 以后端实际代码为准（controller/finance/ReconciliationController.java）：
 * - GET  /v1/finance/recon/statements?status&periodStart&periodEnd&accountId&current&size  列表（IPage）
 * - GET  /v1/finance/recon/statements/{statementId}                                       详情（含 items）
 * - POST /v1/finance/recon/statements                                                     生成/重算
 * - POST /v1/finance/recon/statements/{id}/submit                                         提交（V5：调节后两侧必须相等）
 * - POST /v1/finance/recon/statements/{id}/approve                                        审核（V8：四眼 + 禁止出纳）
 * - POST /v1/finance/recon/statements/{id}/items/{itemId}/resolve-ambiguity               处置同额歧义（BL-6）
 * - GET  /v1/finance/recon/statements/{statementId}/export                                CSV 导出（Blob）
 *
 * 注意：后端**没有** `/reject` 端点（原始需求里提到过），因此本模块不提供 reject，
 *      状态机为 DRAFT → SUBMITTED → APPROVED（见 ReconciliationStatementServiceImpl）。
 */
import { get, post } from '@/api/request'

/** 未达账项明细（对应后端 ReconciliationItemVO，字段为 public field 序列化） */
export interface ReconciliationItemVO {
  itemId: number
  /** A 企业已收银行未收 / B 企业已付银行未付 / C 银行已收企业未收 / D 银行已付企业未付 */
  category: string
  categoryName?: string | null
  flowId?: number | null
  transactionId?: number | null
  /** 分 */
  amount?: number | null
  businessDate?: string | null
  periodStart?: string | null
  periodEnd?: string | null
  ownerUserId?: number | null
  ownerName?: string | null
  reason?: string | null
  clearedFlag?: boolean | null
  /** 是否处于未解决的同额歧义（reason 以 AMBIGUOUS: 开头） */
  ambiguous?: boolean | null
}

/** 调节表视图（对应后端 ReconciliationStatementVO；金额单位：分） */
export interface ReconciliationStatementVO {
  statementId: number
  statementNo: string
  accountId?: number | null
  storeId?: number | null
  periodStart?: string | null
  periodEnd?: string | null
  /** 企业账面余额（分） */
  bookBalance?: number | null
  /** 对账单余额（分） */
  bankBalance?: number | null
  /** A 企业已收银行未收（分） */
  unclearedInCredit?: number | null
  /** B 企业已付银行未付（分） */
  unclearedInDebit?: number | null
  /** C 银行已收企业未收（分） */
  unclearedOutCredit?: number | null
  /** D 银行已付企业未付（分） */
  unclearedOutDebit?: number | null
  /** 企业调节后余额 = book + C − D（分），接口原值 */
  adjustedBookBalance?: number | null
  /** 银行调节后余额 = bank + A − B（分），接口原值 */
  adjustedBankBalance?: number | null
  isBalanced?: boolean | null
  status?: string | null
  submittedBy?: number | null
  submittedAt?: string | null
  approvedBy?: number | null
  approvedAt?: string | null
  remark?: string | null
  createUserId?: number | null
  createTime?: string | null
  /** 未解决的同额歧义条数（>0 时 isBalanced 恒 false，禁止 submit/approve） */
  ambiguousItemCount?: number | null
  items?: ReconciliationItemVO[] | null
}

/** 列表查询参数 */
export interface StatementQuery {
  status?: string | null
  /** yyyy-MM-dd */
  periodStart?: string | null
  /** yyyy-MM-dd */
  periodEnd?: string | null
  accountId?: number | null
  current?: number
  size?: number
}

/** 生成/重算入参（对应 StatementCreateRequest） */
export interface StatementCreatePayload {
  accountId: number
  /** yyyy-MM-dd */
  periodStart: string
  /** yyyy-MM-dd */
  periodEnd: string
  /** 对账单余额（分） */
  bankStatementBalanceFen?: number | null
  remark?: string | null
}

/** IPage 分页响应 */
export interface StatementPageResult {
  records: ReconciliationStatementVO[]
  total: number
  current: number
  size: number
  pages?: number
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

/** S4 列表 */
export async function listStatements(query: StatementQuery = {}): Promise<StatementPageResult> {
  const params = compactParams({
    status: query.status,
    periodStart: query.periodStart,
    periodEnd: query.periodEnd,
    accountId: query.accountId,
    current: query.current ?? 1,
    size: query.size ?? 20,
  })
  const data = await get<StatementPageResult | null>('/v1/finance/recon/statements', params)
  return {
    records: data?.records ?? [],
    total: data?.total ?? 0,
    current: data?.current ?? Number(params.current ?? 1),
    size: data?.size ?? Number(params.size ?? 20),
    pages: data?.pages ?? 0,
  }
}

/** S5 详情（含四类未达账项 items） */
export async function getStatement(statementId: number): Promise<ReconciliationStatementVO> {
  const data = await get<ReconciliationStatementVO | null>(
    `/v1/finance/recon/statements/${encodeURIComponent(String(statementId))}`,
  )
  if (!data) throw new Error('调节表详情为空')
  return data
}

/** S3 生成/重算 */
export async function generateStatement(payload: StatementCreatePayload): Promise<ReconciliationStatementVO> {
  const data = await post<ReconciliationStatementVO | null>('/v1/finance/recon/statements', compactParams({
    accountId: payload.accountId,
    periodStart: payload.periodStart,
    periodEnd: payload.periodEnd,
    bankStatementBalanceFen: payload.bankStatementBalanceFen,
    remark: payload.remark,
  }))
  if (!data) throw new Error('生成调节表未返回数据')
  return data
}

/** S6 提交（后端 V5 门禁：调节后两侧不相等 → 报错） */
export async function submitStatement(statementId: number): Promise<ReconciliationStatementVO> {
  const data = await post<ReconciliationStatementVO | null>(
    `/v1/finance/recon/statements/${encodeURIComponent(String(statementId))}/submit`,
  )
  if (!data) throw new Error('提交后未返回调节表')
  return data
}

/**
 * S7 审核（后端 V8：四眼原则 + 禁止出纳）。
 *
 * 前端按 submittedBy / 角色做禁用前置，但**后端 409/错误仍是最终依据**：
 * 调用失败时不吞错，把后端文案原样抛给页面展示。
 */
export async function approveStatement(statementId: number): Promise<ReconciliationStatementVO> {
  const data = await post<ReconciliationStatementVO | null>(
    `/v1/finance/recon/statements/${encodeURIComponent(String(statementId))}/approve`,
  )
  if (!data) throw new Error('审核后未返回调节表')
  return data
}

/** BL-6：处置同额歧义（decision = A|C 或 B|D） */
export async function resolveStatementAmbiguity(
  statementId: number,
  itemId: number,
  payload: { decision: string; targetRef?: string | null; reason?: string | null },
): Promise<ReconciliationStatementVO> {
  const data = await post<ReconciliationStatementVO | null>(
    `/v1/finance/recon/statements/${encodeURIComponent(String(statementId))}/items/${encodeURIComponent(String(itemId))}/resolve-ambiguity`,
    compactParams({ decision: payload.decision, targetRef: payload.targetRef, reason: payload.reason }),
  )
  if (!data) throw new Error('歧义处置未返回调节表')
  return data
}

/** S9 导出 CSV（Blob；文件名由后端 Content-Disposition 给出，这里回退 statement-{id}.csv） */
export async function exportStatementCsv(statementId: number): Promise<Blob> {
  return get<Blob>(
    `/v1/finance/recon/statements/${encodeURIComponent(String(statementId))}/export`,
    undefined,
    { responseType: 'blob' },
  )
}

export const financeStatementApi = {
  listStatements,
  getStatement,
  generateStatement,
  submitStatement,
  approveStatement,
  resolveStatementAmbiguity,
  exportStatementCsv,
}

export default financeStatementApi
