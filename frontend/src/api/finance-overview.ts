/**
 * 总部资金总览（P3-4）只读接口封装。
 *
 * 契约（后端 fund-500 并行实现）：
 * GET /v1/finance/overview/daily?date=YYYY-MM-DD
 * {
 *   date, dataScope, storeId,
 *   stores: [{ storeId, storeName, cashAccountId, balanceFen, netChangeFen, negative, abnormal }],
 *   channels: { internalSaleFen, externalReceivedFen, diffFen, unexplainedCount,
 *               runs: [{ runId, accountId, internalAmount, externalAmount, differenceCount }] },
 *   statements: { openCount, byStatus: [{ status, count }], oldestPendingDays,
 *                 oldest: { statementId, status, submittedBy, submittedAt, daysPending } }
 * }
 *
 * 金额单位：分。前端**只展示接口原值**，不做任何重算/求和。
 */
import { get } from '@/api/request'

/** 门店现金户行 */
export interface FundOverviewStoreVO {
  storeId: number | null
  storeName: string | null
  cashAccountId: number | null
  /** 分：当前余额 */
  balanceFen: number | null
  /** 分：当日净变动 */
  netChangeFen: number | null
  /** 余额为负（透支/异常） */
  negative: boolean | null
  /** 是否异常（后端判定，前端不自行推断） */
  abnormal: boolean | null
}

/** 渠道跑批行 */
export interface FundOverviewChannelRunVO {
  runId: number | null
  accountId: number | null
  /** 分 */
  internalAmount: number | null
  /** 分 */
  externalAmount: number | null
  differenceCount: number | null
}

/** 渠道到账 vs SALE（内部销售） */
export interface FundOverviewChannelsVO {
  /** 分 */
  internalSaleFen: number | null
  /** 分 */
  externalReceivedFen: number | null
  /** 分 */
  diffFen: number | null
  unexplainedCount: number | null
  runs: FundOverviewChannelRunVO[]
}

/** 调节表状态分布 */
export interface FundOverviewStatementStatusVO {
  status: string | null
  count: number | null
}

/** 最久未关闭的调节表 */
export interface FundOverviewOldestVO {
  statementId: number | null
  status: string | null
  submittedBy: number | null
  submittedAt: string | null
  daysPending: number | null
}

/** 未关闭调节表汇总 */
export interface FundOverviewStatementsVO {
  openCount: number | null
  byStatus: FundOverviewStatementStatusVO[]
  oldestPendingDays: number | null
  oldest: FundOverviewOldestVO | null
}

/** 资金总览视图 */
export interface FundOverviewVO {
  date: string | null
  dataScope: string | null
  storeId: number | null
  stores: FundOverviewStoreVO[]
  channels: FundOverviewChannelsVO | null
  statements: FundOverviewStatementsVO | null
}

/** 接口错误（区分 403，用于「无权查看资金总览」文案） */
export class OverviewApiError extends Error {
  readonly forbidden: boolean

  constructor(message: string, forbidden = false) {
    super(message)
    this.name = 'OverviewApiError'
    this.forbidden = forbidden
  }
}

function toOverviewError(error: unknown): OverviewApiError {
  const message = error instanceof Error ? error.message : String(error)
  return new OverviewApiError(message, /403|没有权限|无权|无访问权限|禁止/.test(message))
}

function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null
}

function bool(value: unknown): boolean | null {
  return typeof value === 'boolean' ? value : null
}

function str(value: unknown): string | null {
  return typeof value === 'string' ? value : null
}

/**
 * 总部资金总览（单日）。
 * @param date yyyy-MM-dd
 */
export async function getFundOverviewDaily(date: string): Promise<FundOverviewVO> {
  try {
    const data = await get<FundOverviewVO | null>('/v1/finance/overview/daily', { date })
    if (!data) throw new OverviewApiError('资金总览未返回数据')

    const stores = Array.isArray(data.stores)
      ? data.stores.map((row) => ({
          storeId: num(row?.storeId),
          storeName: str(row?.storeName),
          cashAccountId: num(row?.cashAccountId),
          balanceFen: num(row?.balanceFen),
          netChangeFen: num(row?.netChangeFen),
          negative: bool(row?.negative),
          abnormal: bool(row?.abnormal),
        }))
      : []

    const channels = data.channels
      ? {
          internalSaleFen: num(data.channels.internalSaleFen),
          externalReceivedFen: num(data.channels.externalReceivedFen),
          diffFen: num(data.channels.diffFen),
          unexplainedCount: num(data.channels.unexplainedCount),
          runs: Array.isArray(data.channels.runs)
            ? data.channels.runs.map((run) => ({
                runId: num(run?.runId),
                accountId: num(run?.accountId),
                internalAmount: num(run?.internalAmount),
                externalAmount: num(run?.externalAmount),
                differenceCount: num(run?.differenceCount),
              }))
            : [],
        }
      : null

    const statements = data.statements
      ? {
          openCount: num(data.statements.openCount),
          byStatus: Array.isArray(data.statements.byStatus)
            ? data.statements.byStatus.map((row) => ({
                status: str(row?.status),
                count: num(row?.count),
              }))
            : [],
          oldestPendingDays: num(data.statements.oldestPendingDays),
          oldest: data.statements.oldest
            ? {
                statementId: num(data.statements.oldest.statementId),
                status: str(data.statements.oldest.status),
                submittedBy: num(data.statements.oldest.submittedBy),
                submittedAt: str(data.statements.oldest.submittedAt),
                daysPending: num(data.statements.oldest.daysPending),
              }
            : null,
        }
      : null

    return {
      date: str(data.date),
      dataScope: str(data.dataScope),
      storeId: num(data.storeId),
      stores,
      channels,
      statements,
    }
  } catch (error) {
    throw error instanceof OverviewApiError ? error : toOverviewError(error)
  }
}

export const financeOverviewApi = { getFundOverviewDaily }

export default financeOverviewApi
