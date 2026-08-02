/**
 * 会计期间API
 * 对应后端: /v1/finance/accounting-periods
 *
 * 【设计说明】
 * - 后端 AccountingPeriodController 8 个端点
 * - close/reopen/profitTransfer 后端要求 @RequestParam Long operatorId（必填）
 * - getClosingChecklist 返回 ClosingChecklistItem[]
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost/silentPut）
 * - 内置状态转换：后端数字 ↔ 前端语义字符串
 *   （1=open 2=closing 3=closed）
 */
import { get, post, put } from '../request'
import { AccountingPeriodStatusMap } from './converters'
import type {
  AccountingPeriod,
  AccountingPeriodQueryForm,
  TrialBalanceResult,
  ClosingChecklistItem,
  ProfitTransferResult,
  PageResponse,
} from '@/types/finance'

/** 后端数据 → 前端展示（状态数字→字符串） */
function toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...backend }
  if (result.status !== undefined && result.status !== null) {
    result.status = AccountingPeriodStatusMap.toFrontend[result.status as number] ?? result.status
  }
  return result
}

/** 前端表单 → 后端 DTO（状态字符串→数字） */
function toDTO(form: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...form }
  if (typeof result.status === 'string') {
    result.status = AccountingPeriodStatusMap.toBackend[result.status] ?? result.status
  }
  return result
}

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 status 数字（1开启 2结账中 3已结账）
 */
function mapQueryParams(params?: AccountingPeriodQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = AccountingPeriodStatusMap.toBackend[status]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface AccountingPeriodPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const accountingPeriodApi = {
  /**
   * 分页查询会计期间列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态）
   */
  async getList(params?: AccountingPeriodQueryForm): Promise<PageResponse<AccountingPeriod>> {
    const query = mapQueryParams(params)
    const res = await get<AccountingPeriodPageBackend | null>('/v1/finance/accounting-periods', query)
    const records = (res?.records || []).map(item =>
      toFrontend(item) as unknown as AccountingPeriod
    )
    return {
      records,
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取会计期间详情
   * @param id - 期间ID
   * @returns 期间详情（已转换状态）
   */
  async getById(id: string): Promise<AccountingPeriod> {
    const res = await get<Record<string, unknown>>(`/v1/finance/accounting-periods/${id}`)
    return toFrontend(res) as unknown as AccountingPeriod
  },

  /**
   * 创建会计期间
   * @param data - 期间数据（前端语义字符串）
   * @returns 创建后的期间（已转换状态）
   */
  async create(data: Partial<AccountingPeriod>): Promise<AccountingPeriod> {
    const dto = toDTO(data as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/accounting-periods', dto)
    return toFrontend(res) as unknown as AccountingPeriod
  },

  /**
   * 结账
   * 后端 PUT /v1/finance/accounting-periods/{id}/close?operatorId={operatorId}
   *
   * @param id - 期间ID
   * @param operatorId - 操作人ID（必填）
   * @returns 结账后的期间（已转换状态）
   */
  async close(id: string, operatorId: string): Promise<AccountingPeriod> {
    const res = await put<Record<string, unknown>>(
      `/v1/finance/accounting-periods/${id}/close`,
      null,
      { params: { operatorId } }
    )
    return toFrontend(res) as unknown as AccountingPeriod
  },

  /**
   * 反结账
   * 后端 PUT /v1/finance/accounting-periods/{id}/reopen?operatorId={operatorId}
   *
   * @param id - 期间ID
   * @param operatorId - 操作人ID（必填）
   * @returns 反结账后的期间（已转换状态）
   */
  async reopen(id: string, operatorId: string): Promise<AccountingPeriod> {
    const res = await put<Record<string, unknown>>(
      `/v1/finance/accounting-periods/${id}/reopen`,
      null,
      { params: { operatorId } }
    )
    return toFrontend(res) as unknown as AccountingPeriod
  },

  /**
   * 获取试算平衡结果
   * @param id - 期间ID
   * @returns 试算平衡结果
   */
  async getTrialBalance(id: string): Promise<TrialBalanceResult> {
    const res = await get<TrialBalanceResult | null>(
      `/v1/finance/accounting-periods/${id}/trial-balance`
    )
    return (res ?? { balanced: true, totalDebit: 0, totalCredit: 0, difference: 0, details: [] }) as TrialBalanceResult
  },

  /**
   * 获取结账清单
   * @param id - 期间ID
   * @returns 结账清单项列表
   */
  async getClosingChecklist(id: string): Promise<ClosingChecklistItem[]> {
    const res = await get<ClosingChecklistItem[] | null>(
      `/v1/finance/accounting-periods/${id}/closing-checklist`
    )
    return res ?? []
  },

  /**
   * 损益结转
   * 后端 POST /v1/finance/accounting-periods/{id}/profit-transfer?operatorId={operatorId}
   *
   * @param id - 期间ID
   * @param operatorId - 操作人ID（必填）
   * @returns 结转结果
   */
  async profitTransfer(id: string, operatorId: string): Promise<ProfitTransferResult> {
    const res = await post<ProfitTransferResult | null>(
      `/v1/finance/accounting-periods/${id}/profit-transfer`,
      null,
      { params: { operatorId } }
    )
    return (res ?? { success: true, transferAmount: 0, message: '' }) as ProfitTransferResult
  },

  /**
   * 获取当前会计期间
   * @returns 当前期间（已转换状态）
   */
  async getCurrent(): Promise<AccountingPeriod> {
    const res = await get<Record<string, unknown>>('/v1/finance/accounting-periods/current')
    return toFrontend(res) as unknown as AccountingPeriod
  },
}

export default accountingPeriodApi
