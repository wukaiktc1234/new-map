/**
 * 资金流水API
 * 对应后端: /v1/finance/fund-flows
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post（替代 silentGet/silentPost）
 * - 内置 DataConverter 处理：
 *   - 流水状态数字↔字符串（后端 1~3 ↔ 前端 'pending'/'completed'/'cancelled'）
 *   - 流水类型数字↔字符串（后端 1/2 ↔ 前端 'income'/'expense'）
 *   - 金额字段 amount/balance 分↔元
 */
import { get, post } from '../request'
import { FundFlowDataConverter, FundFlowTypeMap, fenToYuanNumber } from './converters'
import type {
  FundFlow,
  FundFlowFormData,
  FundFlowQueryForm,
  FundFlowStatistics,
  PageResponse,
} from '@/types/finance'

/**
 * 剔除空值参数（''/undefined/null/NaN 一律不下发）
 *
 * 后端资金流水相关端点的 ID/数值参数都是 Long/int 类型：
 * - `@RequestParam Long accountId` 收到空串 → MethodArgumentTypeMismatchException → 400「参数类型不匹配：accountId」
 * - `@PathVariable Long accountId` 收到 'undefined'/'' → 同上
 * 因此在 API 边界统一过滤空值，避免把空串透传给后端。
 */
function compactParams(params: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (typeof value === 'string' && value.trim() === '') return
    if (typeof value === 'number' && Number.isNaN(value)) return
    result[key] = value
  })
  return result
}

/**
 * 判断 ID 是否可用于请求（非空且不是字符串化的 undefined/null/NaN）
 * 例：`${undefined}` 会得到字符串 'undefined'，这类值同样必须拦掉。
 */
function isPresentId(value: unknown): value is string | number {
  if (value === undefined || value === null) return false
  const text = String(value).trim()
  return text !== '' && text !== 'undefined' && text !== 'null' && text !== 'NaN'
}

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 bankAccountId → 后端 accountId（空值不下发）
 * - 前端 flowType 字符串 → 后端 flowDirection 数字
 */
function mapQueryParams(params?: FundFlowQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, bankAccountId, flowType, ...rest } = params as Record<string, unknown>
  // rest 中的字符串条件（flowNo/startDate/endDate/counterparty 等）统一过滤空串
  const result: Record<string, unknown> = compactParams(rest)
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (isPresentId(bankAccountId)) result.accountId = bankAccountId
  if (flowType !== undefined && typeof flowType === 'string' && flowType.trim() !== '') {
    result.flowDirection = FundFlowTypeMap.toBackend[flowType]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface FundFlowPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

/** 统计数据转换：后端金额为分，前端为元 */
function toFrontendStatistics(backend: Record<string, unknown> | null): FundFlowStatistics {
  if (!backend) {
    return { totalIncome: 0, totalExpense: 0, netAmount: 0, count: 0 }
  }
  return {
    totalIncome: fenToYuanNumber(backend.totalIncome as number),
    totalExpense: fenToYuanNumber(backend.totalExpense as number),
    netAmount: fenToYuanNumber(backend.netAmount as number),
    count: (backend.count as number) ?? 0,
  }
}

export const fundFlowApi = {
  /**
   * 分页查询资金流水列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型/金额）
   */
  async getList(params?: FundFlowQueryForm): Promise<PageResponse<FundFlow>> {
    const query = mapQueryParams(params)
    const res = await get<FundFlowPageBackend | null>('/v1/finance/fund-flows', query)
    const records = (res?.records || []).map(item =>
      FundFlowDataConverter.toFrontend(item) as unknown as FundFlow
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
   * 根据ID获取资金流水详情
   * @param id - 流水ID
   * @returns 流水详情（已转换状态/类型/金额）
   */
  async getById(id: string): Promise<FundFlow> {
    const res = await get<Record<string, unknown>>(`/v1/finance/fund-flows/${id}`)
    return FundFlowDataConverter.toFrontend(res) as unknown as FundFlow
  },

  /**
   * 创建资金流水
   * @param data - 流水表单数据（前端语义字符串，金额为元）
   * @returns 创建后的流水（已转换状态/类型/金额）
   */
  async create(data: FundFlowFormData): Promise<FundFlow> {
    const dto = FundFlowDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/fund-flows', dto)
    return FundFlowDataConverter.toFrontend(res) as unknown as FundFlow
  },

  /**
   * 按银行账户查询资金流水
   * @param accountId - 银行账户ID
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型/金额）
   */
  async getByAccountId(accountId: string, params?: FundFlowQueryForm): Promise<PageResponse<FundFlow>> {
    const fallback: PageResponse<FundFlow> = {
      records: [],
      total: 0,
      current: (params?.page as number | undefined) ?? 1,
      size: (params?.size as number | undefined) ?? 10,
      pages: 0,
    }
    // 守卫：accountId 为空时后端 @PathVariable Long 会 400「参数类型不匹配：accountId」
    if (!isPresentId(accountId)) {
      console.warn('[fundFlowApi.getByAccountId] accountId 为空，已跳过请求', { accountId })
      return fallback
    }
    const query = mapQueryParams(params)
    const res = await get<FundFlowPageBackend | null>(`/v1/finance/fund-flows/account/${encodeURIComponent(String(accountId))}`, query)
    const records = (res?.records || []).map(item =>
      FundFlowDataConverter.toFrontend(item) as unknown as FundFlow
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
   * 获取银行账户资金流水统计
   * 后端 GET /v1/finance/fund-flows/account/{accountId}/statistics
   *   accountId 为 @PathVariable Long，startDate/endDate 为 @RequestParam LocalDate（必填，yyyy-MM-dd）
   *
   * 【空守卫】accountId 为空（null/undefined/''/'undefined'）时直接返回全 0 统计且不发请求：
   * 否则 URL 会拼成 /account/undefined/statistics，后端抛
   * MethodArgumentTypeMismatchException → 400「参数类型不匹配：accountId」。
   *
   * @param accountId - 银行账户ID（允许为空，为空则不请求）
   * @param startDate - 开始日期（yyyy-MM-dd）
   * @param endDate - 结束日期（yyyy-MM-dd）
   * @returns 统计数据（金额已转为元）
   */
  async getStatisticsByAccount(
    accountId: string | number | null | undefined,
    startDate: string,
    endDate: string,
  ): Promise<FundFlowStatistics> {
    if (!isPresentId(accountId)) {
      console.warn('[fundFlowApi.getStatisticsByAccount] accountId 为空，已跳过统计请求', { accountId, startDate, endDate })
      return { totalIncome: 0, totalExpense: 0, netAmount: 0, count: 0 }
    }
    const query = compactParams({ startDate, endDate })
    const res = await get<Record<string, unknown> | null>(
      `/v1/finance/fund-flows/account/${encodeURIComponent(String(accountId))}/statistics`,
      query
    )
    return toFrontendStatistics(res ?? null)
  },
}

export default fundFlowApi
