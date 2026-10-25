/**
 * 门店对账 + 渠道手续费（P3-2）只读接口封装。
 *
 * 契约（后端 fund-500 并行实现，前端按此对接）：
 * - GET /v1/finance/store-recon/daily?storeId&date
 *     → { storeId, businessDate, expectedAmount, actualAmount, differenceAmount,
 *         slipId, slipNo, slipStatus, remark }        // 金额单位：分
 * - GET /v1/finance/recon/channel-fee-summary?runId&from&to
 *     → { groups: [{ sourceType, diffCount, feeTotalFen, diffAmountFen }],
 *         total:  { diffCount, feeTotalFen, diffAmountFen } }
 *
 * 说明：金额一律保持「分」的整数，展示层再除 100。
 */
import { get } from '@/api/request'

/** 门店当日对账（金额单位：分） */
export interface StoreReconDailyVO {
  storeId: number | null
  businessDate: string | null
  /** 分 */
  expectedAmount: number | null
  /** 分 */
  actualAmount: number | null
  /** 分：differenceAmount = actualAmount − expectedAmount */
  differenceAmount: number | null
  slipId: number | null
  slipNo: string | null
  slipStatus: string | null
  remark: string | null
}

/** 渠道手续费分组（金额单位：分） */
export interface ChannelFeeGroupVO {
  sourceType: string | null
  diffCount: number | null
  /** 分 */
  feeTotalFen: number | null
  /** 分 */
  diffAmountFen: number | null
}

/** 渠道手续费汇总 */
export interface ChannelFeeSummaryVO {
  groups: ChannelFeeGroupVO[]
  total: ChannelFeeGroupVO | null
}

/**
 * 接口错误（带 HTTP 语义分类）。
 *
 * 403 需要与普通错误区分：店长访问其他门店时后端返回 403，
 * 页面要显示「无权访问其他门店数据」，而不是空白或通用报错。
 */
export class ReconApiError extends Error {
  /** 是否为 403（无权限） */
  readonly forbidden: boolean
  /** 是否为 404（无数据/未跑批） */
  readonly notFound: boolean

  constructor(message: string, forbidden = false, notFound = false) {
    super(message)
    this.name = 'ReconApiError'
    this.forbidden = forbidden
    this.notFound = notFound
  }
}

/** 从 request.ts 抛出的错误里判定 403 / 404（其 message 为后端中文提示） */
function toReconError(error: unknown): ReconApiError {
  const message = error instanceof Error ? error.message : String(error)
  const forbidden = /403|没有权限|无权|无访问权限|禁止/.test(message)
  const notFound = /404|不存在|未找到/.test(message)
  return new ReconApiError(message, forbidden, notFound)
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

/**
 * 门店当日对账。
 * @param storeId 门店ID（店长留空则由后端按当前登录账号归属门店兜底）
 * @param date    yyyy-MM-dd
 */
export async function getStoreReconDaily(params: {
  storeId?: number | string | null
  date: string
}): Promise<StoreReconDailyVO> {
  try {
    const data = await get<StoreReconDailyVO | null>('/v1/finance/store-recon/daily', compactParams({
      storeId: params.storeId,
      date: params.date,
    }))
    if (!data) {
      throw new ReconApiError('门店对账未返回数据', false, true)
    }
    return {
      storeId: data.storeId ?? null,
      businessDate: data.businessDate ?? null,
      expectedAmount: typeof data.expectedAmount === 'number' ? data.expectedAmount : null,
      actualAmount: typeof data.actualAmount === 'number' ? data.actualAmount : null,
      differenceAmount: typeof data.differenceAmount === 'number' ? data.differenceAmount : null,
      slipId: data.slipId ?? null,
      slipNo: data.slipNo ?? null,
      slipStatus: data.slipStatus ?? null,
      remark: data.remark ?? null,
    }
  } catch (error) {
    throw error instanceof ReconApiError ? error : toReconError(error)
  }
}

/** 渠道手续费汇总（runId 为空时按 from/to 时间窗统计） */
export async function getChannelFeeSummary(params: {
  runId?: number | null
  from?: string | null
  to?: string | null
} = {}): Promise<ChannelFeeSummaryVO> {
  try {
    const data = await get<ChannelFeeSummaryVO | null>('/v1/finance/recon/channel-fee-summary', compactParams({
      runId: params.runId,
      from: params.from,
      to: params.to,
    }))
    const groups = Array.isArray(data?.groups) ? data!.groups : []
    return {
      groups: groups.map((group) => ({
        sourceType: group.sourceType ?? null,
        diffCount: typeof group.diffCount === 'number' ? group.diffCount : null,
        feeTotalFen: typeof group.feeTotalFen === 'number' ? group.feeTotalFen : null,
        diffAmountFen: typeof group.diffAmountFen === 'number' ? group.diffAmountFen : null,
      })),
      total: data?.total
        ? {
            sourceType: data.total.sourceType ?? null,
            diffCount: typeof data.total.diffCount === 'number' ? data.total.diffCount : null,
            feeTotalFen: typeof data.total.feeTotalFen === 'number' ? data.total.feeTotalFen : null,
            diffAmountFen: typeof data.total.diffAmountFen === 'number' ? data.total.diffAmountFen : null,
          }
        : null,
    }
  } catch (error) {
    throw error instanceof ReconApiError ? error : toReconError(error)
  }
}

export const financeStoreReconApi = {
  getStoreReconDaily,
  getChannelFeeSummary,
}

export default financeStoreReconApi
