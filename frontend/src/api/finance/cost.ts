/**
 * 成本记录API
 * 对应后端: /v1/finance/costs
 *
 * 【设计说明】
 * - 后端 CostController 5 个端点：create/update/getDetail/getPage/summarizeByPeriod
 * - 无 DELETE 端点，前端 delete 方法已移除（避免 405）
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost 等）
 * - 内置 DataConverter 处理：
 *   - 成本类型数字↔字符串（后端 1~6 ↔ 前端 'material'/'labor'/'rent'/'energy'/'marketing'/'other'）
 *   - 金额字段 amount/budgetAmount/variance 分↔元
 */
import { get, post, put } from '../request'
import { CostDataConverter, CostTypeMap } from './converters'
import type {
  FinanceCostRecord,
  FinanceCostFormData,
  FinanceCostQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 costType 字符串 → 后端 costType 数字
 * - 前端 startDate → 后端 startPeriod（期间格式 YYYY-MM）
 * - 前端 endDate → 后端 endPeriod（期间格式 YYYY-MM）
 */
function mapQueryParams(params?: FinanceCostQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, costType, startDate, endDate, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (costType !== undefined && typeof costType === 'string') {
    result.costType = CostTypeMap.toBackend[costType]
  }
  if (startDate !== undefined) result.startPeriod = startDate
  if (endDate !== undefined) result.endPeriod = endDate
  return result
}

/** 后端 IPage 分页响应 */
interface CostPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const costApi = {
  /**
   * 分页查询成本记录列表
   * @param params - 查询参数
   * @returns 分页数据（已转换类型/金额）
   */
  async getList(params?: FinanceCostQueryForm): Promise<PageResponse<FinanceCostRecord>> {
    const query = mapQueryParams(params)
    const res = await get<CostPageBackend | null>('/v1/finance/costs', query)
    const records = (res?.records || []).map(item =>
      CostDataConverter.toFrontend(item) as unknown as FinanceCostRecord
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
   * 根据ID获取成本记录详情
   * @param id - 成本记录ID
   * @returns 成本记录详情（已转换类型/金额）
   */
  async getById(id: string): Promise<FinanceCostRecord> {
    const res = await get<Record<string, unknown>>(`/v1/finance/costs/${id}`)
    return CostDataConverter.toFrontend(res) as unknown as FinanceCostRecord
  },

  /**
   * 创建成本记录
   * @param data - 成本记录表单数据（前端语义字符串，金额为元）
   * @returns 创建后的成本记录（已转换类型/金额）
   */
  async create(data: FinanceCostFormData): Promise<FinanceCostRecord> {
    const dto = CostDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/costs', dto)
    return CostDataConverter.toFrontend(res) as unknown as FinanceCostRecord
  },

  /**
   * 更新成本记录
   * @param id - 成本记录ID
   * @param data - 成本记录表单数据（前端语义字符串，金额为元）
   * @returns 更新后的成本记录（已转换类型/金额）
   */
  async update(id: string, data: FinanceCostFormData): Promise<FinanceCostRecord> {
    const dto = CostDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/costs/${id}`, dto)
    return CostDataConverter.toFrontend(res) as unknown as FinanceCostRecord
  },

  // 【设计说明】后端 CostController 无 DELETE 端点
  // delete 方法已移除（原前端调用 DELETE /v1/finance/costs/{id} 后端会返回 405）

  // 【设计说明】后端有 GET /v1/finance/costs/summary?period={period} 端点
  // 前端未对接 summarizeByPeriod 方法（按期间汇总成本，返回 Map<Integer, Long>）
}

export default costApi
