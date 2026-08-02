/**
 * 标准成本卡API
 * 对应后端: /v1/finance/standard-cost-cards
 *
 * 【设计说明】
 * - 后端 StandardCostCardController 7 个端点：create/update/delete/getDetail/getPage/updateWarningStatus/getByDishId
 * - updateWarningStatus 后端使用 @RequestParam Integer warningStatus（URL query），非 JSON body
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 * - 内置 DataConverter 处理：
 *   - 预警状态数字↔字符串（后端 1~3 ↔ 前端 'normal'/'warning'/'over'）
 *   - 金额字段 standardCost/actualCost/variance 分↔元
 */
import { get, post, put, del } from '../request'
import { StandardCostCardDataConverter, WarningStatusMap } from './converters'
import type {
  StandardCostCard,
  StandardCostCardFormData,
  StandardCostCardQueryForm,
  UpdateWarningStatusDTO,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 warningStatus 字符串 → 后端 warningStatus 数字（1正常 2预警 3超支）
 */
function mapQueryParams(params?: StandardCostCardQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, warningStatus, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (warningStatus !== undefined && typeof warningStatus === 'string') {
    result.warningStatus = WarningStatusMap.toBackend[warningStatus]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface StandardCostPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const standardCostApi = {
  /**
   * 分页查询标准成本卡列表
   * @param params - 查询参数
   * @returns 分页数据（已转换预警状态/金额）
   */
  async getList(params?: StandardCostCardQueryForm): Promise<PageResponse<StandardCostCard>> {
    const query = mapQueryParams(params)
    const res = await get<StandardCostPageBackend | null>('/v1/finance/standard-cost-cards', query)
    const records = (res?.records || []).map(item =>
      StandardCostCardDataConverter.toFrontend(item) as unknown as StandardCostCard
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
   * 根据ID获取标准成本卡详情
   * 对应后端 GET /v1/finance/standard-cost-cards/{id}
   * @param id - 成本卡ID
   * @returns 标准成本卡详情（已转换预警状态/金额）
   */
  async getById(id: string): Promise<StandardCostCard> {
    const res = await get<Record<string, unknown>>(`/v1/finance/standard-cost-cards/${id}`)
    return StandardCostCardDataConverter.toFrontend(res) as unknown as StandardCostCard
  },

  /**
   * 根据菜品ID查询标准成本卡
   * @param dishId - 菜品ID
   * @returns 标准成本卡详情（已转换预警状态/金额）
   */
  async getByDishId(dishId: string): Promise<StandardCostCard> {
    const res = await get<Record<string, unknown>>(`/v1/finance/standard-cost-cards/dish/${dishId}`)
    return StandardCostCardDataConverter.toFrontend(res) as unknown as StandardCostCard
  },

  /**
   * 创建标准成本卡
   * @param data - 标准成本卡表单数据（前端语义字符串，金额为元）
   * @returns 创建后的标准成本卡（已转换预警状态/金额）
   */
  async create(data: StandardCostCardFormData): Promise<StandardCostCard> {
    const dto = StandardCostCardDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/standard-cost-cards', dto)
    return StandardCostCardDataConverter.toFrontend(res) as unknown as StandardCostCard
  },

  /**
   * 更新标准成本卡
   * @param id - 成本卡ID
   * @param data - 标准成本卡表单数据（前端语义字符串，金额为元）
   * @returns 更新后的标准成本卡（已转换预警状态/金额）
   */
  async update(id: string, data: StandardCostCardFormData): Promise<StandardCostCard> {
    const dto = StandardCostCardDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/standard-cost-cards/${id}`, dto)
    return StandardCostCardDataConverter.toFrontend(res) as unknown as StandardCostCard
  },

  /**
   * 删除标准成本卡（逻辑删除）
   * @param id - 成本卡ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/standard-cost-cards/${id}`)
  },

  /**
   * 更新预警状态
   * 后端 PUT /v1/finance/standard-cost-cards/{id}/warning-status?warningStatus={number}
   * 后端使用 @RequestParam Integer warningStatus（URL query），非 JSON body
   *
   * @param id - 成本卡ID
   * @param data - 预警状态数据（warningStatus 为前端语义字符串）
   * @returns 更新后的标准成本卡（已转换预警状态/金额）
   */
  async updateWarningStatus(id: string, data: UpdateWarningStatusDTO): Promise<StandardCostCard> {
    const res = await put<Record<string, unknown>>(
      `/v1/finance/standard-cost-cards/${id}/warning-status`,
      null,
      { params: { warningStatus: WarningStatusMap.toBackend[data.warningStatus] } }
    )
    return StandardCostCardDataConverter.toFrontend(res) as unknown as StandardCostCard
  },
}

export default standardCostApi
