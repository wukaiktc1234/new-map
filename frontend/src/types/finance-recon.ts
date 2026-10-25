/**
 * 财务对账工作台（P3-1）类型定义
 *
 * 与后端 DTO 逐字对应：
 * - ReconRunVO        backend/.../dto/finance/recon/ReconRunVO.java
 * - ReconDifferenceVO backend/.../dto/finance/recon/ReconDifferenceVO.java
 * - ReconRunCreateResultVO / ReconResolveDTO / ReconRunQueryDTO / ReconDifferenceQueryDTO
 *
 * 【金额单位】全部为「分」（integer）。展示时统一走 `formatFen` 转元保留 2 位；
 * **任何比较/求和必须使用原始分值**，不要用除法后的浮点值做断言。
 */

/** 跑批状态：RUNNING / FINALIZED / FAILED */
export type ReconRunStatus = 'RUNNING' | 'FINALIZED' | 'FAILED'

/** 差异状态 */
export type ReconDifferenceStatus =
  | 'OPEN'
  | 'ASSIGNED'
  | 'RESOLVING'
  | 'CLOSED'
  | 'EXEMPT'
  | 'PENDING_P2_REVERSE'

/** 差异类型 */
export type ReconDifferenceType =
  | 'AMOUNT_DIFF'
  | 'TIME_DIFF'
  | 'ONE_SIDED_INTERNAL'
  | 'ONE_SIDED_EXTERNAL'
  | 'DUPLICATE'
  | 'CHANNEL_FEE'
  | 'AMBIGUOUS'

/** 处理决议：WRITE_OFF（核销）/ EXEMPT（豁免）/ REVERSE（冲正，P1 仅挂 PENDING_P2_REVERSE） */
export type ReconResolution = 'WRITE_OFF' | 'EXEMPT' | 'REVERSE'

/** IPage 分页响应（MyBatis-Plus） */
export interface ReconPageResult<T> {
  records: T[] | null
  total: number
  current: number
  size: number
  pages?: number
}

/** 跑批视图（R1 结果 / R2 列表 / R3 详情）——字段与 ReconRunVO 逐字对应 */
export interface ReconRunVO {
  runId: number
  runKey: string
  accountId: number
  accountName: string | null
  storeId: number | null
  /** yyyy-MM-dd */
  businessDate: string
  status: ReconRunStatus | string
  internalCount: number | null
  /** 分 */
  internalAmount: number | null
  externalCount: number | null
  /** 分 */
  externalAmount: number | null
  matchedCount: number | null
  /** 分 */
  matchedAmount: number | null
  /** 勾稽率（百分数，两位小数；internalAmount=0 时为 null） */
  matchRate: number | null
  /** 差异数（AC-46a：摘要卡必须直接取该字段，不得前端数列表长度） */
  differenceCount: number | null
  /** 重建自哪个 run（P1 未启用，可能为 null） */
  rebuildOf: number | null
  startedAt: string | null
  finishedAt: string | null
  operatorId: number | null
  createTime: string | null
  updateTime?: string | null
}

/** R1 返回体：{ reused, run } */
export interface ReconRunCreateResult {
  reused: boolean
  run: ReconRunVO
}

/** 差异池视图（R8 列表 / R9 与 R10 的返回）——字段与 ReconDifferenceVO 逐字对应 */
export interface ReconDifferenceVO {
  differenceId: number
  runId: number
  differenceType: ReconDifferenceType | string
  internalRef: string | null
  externalRef: string | null
  /** 分 */
  amountInternal: number | null
  /** 分 */
  amountExternal: number | null
  /** 分：diff_amount = amount_external − amount_internal */
  diffAmount: number | null
  status: ReconDifferenceStatus | string
  resolution: ReconResolution | string | null
  resolutionRemark: string | null
  reverseFlowNo: string | null
  assigneeId: number | null
  assignedAt: string | null
  resolutionBy: number | null
  resolutionTime: string | null
  reopenOf: number | null
  createTime: string | null
  updateTime?: string | null
}

/** R1 请求体（force=true 强制重建） */
export interface ReconRunCreatePayload {
  accountId: number
  /** yyyy-MM-dd */
  businessDate: string
  force?: boolean
}

/** R2 查询参数 */
export interface ReconRunQuery {
  accountId?: number | null
  /** yyyy-MM-dd */
  businessDate?: string | null
  status?: ReconRunStatus | string | null
  current?: number
  size?: number
}

/** R8 查询参数（from/to 为可选时间窗，按 create_time 半开区间 [from, to)） */
export interface ReconDifferenceQuery {
  status?: ReconDifferenceStatus | string | null
  type?: ReconDifferenceType | string | null
  runId?: number | null
  assigneeId?: number | null
  /** ISO-8601 本地时间，如 2026-10-01T00:00:00 */
  from?: string | null
  to?: string | null
  current?: number
  size?: number
}

/**
 * R10 请求体。
 *
 * 【契约校正】后端 ReconResolveDTO 的字段是 `resolution` + **`remark`**（@NotBlank），
 * 不是 `resolutionRemark`（后者只是 VO 里回显的字段名）。因此提交时把用户填写的「原因」
 * 放在 `remark` 上；`resolutionRemark` 仅用于读回展示。
 */
export interface ReconResolvePayload {
  resolution: ReconResolution
  remark: string
  /** 仅 REVERSE 预留（P2 回填真实反向流水号） */
  reverseFlowNo?: string
}

/** 允许的决议值（AC-48：必须三者之一） */
export const RECON_RESOLUTIONS: readonly ReconResolution[] = ['WRITE_OFF', 'EXEMPT', 'REVERSE'] as const

/** 决议中文名 */
export const RECON_RESOLUTION_LABELS: Record<ReconResolution, string> = {
  WRITE_OFF: '核销（只记决议）',
  EXEMPT: '豁免（不计入差异）',
  REVERSE: '冲正（P2 执行）',
}

/** 差异状态中文名 */
export const RECON_STATUS_LABELS: Record<string, string> = {
  OPEN: '待认领',
  ASSIGNED: '已认领',
  RESOLVING: '处理中',
  CLOSED: '已核销',
  EXEMPT: '已豁免',
  PENDING_P2_REVERSE: '待冲正(P2)',
}

/** 差异类型中文名 */
export const RECON_TYPE_LABELS: Record<string, string> = {
  AMOUNT_DIFF: '金额差异',
  TIME_DIFF: '时间差异',
  ONE_SIDED_INTERNAL: '单边-内部',
  ONE_SIDED_EXTERNAL: '单边-外部',
  DUPLICATE: '重复入账',
  CHANNEL_FEE: '渠道手续费',
  AMBIGUOUS: '存疑',
}

/** 分 → 元字符串（保留 2 位）；null/undefined 视为 0.00 */
export function formatFen(fen: number | null | undefined): string {
  const value = typeof fen === 'number' && Number.isFinite(fen) ? fen : 0
  return (value / 100).toFixed(2)
}

/**
 * 求和「分」值（AC-46b 用的聚合工具）。
 * 全程使用整数分值累加，避免浮点误差；缺值按 0 计。
 */
export function sumFen(list: Array<{ diffAmount?: number | null }>): number {
  return list.reduce((sum, item) => sum + (typeof item.diffAmount === 'number' ? item.diffAmount : 0), 0)
}
