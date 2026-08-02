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
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 bankAccountId → 后端 accountId
 * - 前端 flowType 字符串 → 后端 flowDirection 数字
 */
function mapQueryParams(params?: FundFlowQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, bankAccountId, flowType, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (bankAccountId !== undefined) result.accountId = bankAccountId
  if (flowType !== undefined && typeof flowType === 'string') {
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
    const query = mapQueryParams(params)
    const res = await get<FundFlowPageBackend | null>(`/v1/finance/fund-flows/account/${accountId}`, query)
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
   * 后端 @RequestParam startDate/endDate（必填，格式 yyyy-MM-dd）
   *
   * @param accountId - 银行账户ID
   * @param startDate - 开始日期（yyyy-MM-dd）
   * @param endDate - 结束日期（yyyy-MM-dd）
   * @returns 统计数据（金额已转为元）
   */
  async getStatisticsByAccount(accountId: string, startDate: string, endDate: string): Promise<FundFlowStatistics> {
    const res = await get<Record<string, unknown> | null>(
      `/v1/finance/fund-flows/account/${accountId}/statistics`,
      { startDate, endDate }
    )
    return toFrontendStatistics(res ?? null)
  },
}

export default fundFlowApi
