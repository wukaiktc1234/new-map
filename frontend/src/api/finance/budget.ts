/**
 * 预算API
 * 对应后端: /v1/finance/budgets
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost 等）
 * - 内置 DataConverter 处理：
 *   - 预算状态数字↔字符串（后端 1~4 ↔ 前端 'draft'/'approved'/'executing'/'closed'）
 *   - 金额字段 budgetAmount/actualAmount 分↔元
 */
import { get, post, put } from '../request'
import { BudgetDataConverter, BudgetStatusMap } from './converters'
import type {
  FinanceBudget,
  FinanceBudgetFormData,
  FinanceBudgetQueryForm,
  UpdateActualAmountDTO,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 year → 后端 budgetYear
 * - 前端 status 字符串 → 后端 status 数字
 */
function mapQueryParams(params?: FinanceBudgetQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, year, budgetYear, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  // 前端兼容 year 字段，映射到后端 budgetYear
  if (budgetYear !== undefined) {
    result.budgetYear = budgetYear
  } else if (year !== undefined) {
    result.budgetYear = year
  }
  if (status !== undefined && typeof status === 'string') {
    result.status = BudgetStatusMap.toBackend[status]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface BudgetPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const budgetApi = {
  /**
   * 分页查询预算列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/金额）
   */
  async getList(params?: FinanceBudgetQueryForm): Promise<PageResponse<FinanceBudget>> {
    const query = mapQueryParams(params)
    const res = await get<BudgetPageBackend | null>('/v1/finance/budgets', query)
    const records = (res?.records || []).map(item =>
      BudgetDataConverter.toFrontend(item) as unknown as FinanceBudget
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
   * 根据ID获取预算详情
   * @param id - 预算ID
   * @returns 预算详情（已转换状态/金额）
   */
  async getById(id: string): Promise<FinanceBudget> {
    const res = await get<Record<string, unknown>>(`/v1/finance/budgets/${id}`)
    return BudgetDataConverter.toFrontend(res) as unknown as FinanceBudget
  },

  /**
   * 创建预算
   * @param data - 预算表单数据（前端语义字符串，金额为元）
   * @returns 创建后的预算（已转换状态/金额）
   */
  async create(data: FinanceBudgetFormData): Promise<FinanceBudget> {
    const dto = BudgetDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/budgets', dto)
    return BudgetDataConverter.toFrontend(res) as unknown as FinanceBudget
  },

  /**
   * 更新预算
   * @param id - 预算ID
   * @param data - 预算表单数据（前端语义字符串，金额为元）
   * @returns 更新后的预算（已转换状态/金额）
   */
  async update(id: string, data: FinanceBudgetFormData): Promise<FinanceBudget> {
    const dto = BudgetDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/budgets/${id}`, dto)
    return BudgetDataConverter.toFrontend(res) as unknown as FinanceBudget
  },

  /**
   * 更新预算实际金额
   * 后端使用 @RequestParam Long actualAmount（URL query），非 JSON body
   * actualAmount 单位为分（后端），前端调用前需自行将元转为分传入
   *
   * @param id - 预算ID
   * @param data - 实际金额数据（actualAmount 单位为分）
   * @returns 更新后的预算（已转换状态/金额）
   */
  async updateActualAmount(id: string, data: UpdateActualAmountDTO): Promise<FinanceBudget> {
    const res = await put<Record<string, unknown>>(
      `/v1/finance/budgets/${id}/actual`,
      null,
      { params: { actualAmount: data.actualAmount } }
    )
    return BudgetDataConverter.toFrontend(res) as unknown as FinanceBudget
  },
}

export default budgetApi
