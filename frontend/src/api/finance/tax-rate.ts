/**
 * 税率配置API
 * 对应后端: /v1/finance/tax-rate-configs
 *
 * 【设计说明】
 * - 后端 TaxRateConfigController 6 个端点：create/update/delete/getDetail/getPage/getEffectiveRate
 * - getEffectiveRate 后端要求 @RequestParam Integer taxType + @RequestParam Integer taxpayerType（两个必填参数）
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 * - 内置税种转换：后端数字 ↔ 前端语义字符串
 */
import { get, post, put, del } from '../request'
import { TaxTypeMap } from './converters'
import type {
  TaxRateConfig,
  TaxRateConfigFormData,
  TaxRateConfigQueryForm,
  TaxType,
  PageResponse,
} from '@/types/finance'

/** 后端数据 → 前端展示（税种数字→字符串） */
function toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...backend }
  if (result.taxType !== undefined && result.taxType !== null) {
    result.taxType = TaxTypeMap.toFrontend[result.taxType as number] ?? result.taxType
  }
  return result
}

/** 前端表单 → 后端 DTO（税种字符串→数字） */
function toDTO(form: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...form }
  if (typeof result.taxType === 'string') {
    result.taxType = TaxTypeMap.toBackend[result.taxType] ?? result.taxType
  }
  return result
}

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 taxType 字符串 → 后端 taxType 数字
 * - 前端 effective → 后端 isActive
 */
function mapQueryParams(params?: TaxRateConfigQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, taxType, effective, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (taxType !== undefined && typeof taxType === 'string') {
    result.taxType = TaxTypeMap.toBackend[taxType]
  }
  if (effective !== undefined) {
    result.isActive = effective
  }
  return result
}

/** 后端 IPage 分页响应 */
interface TaxRatePageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const taxRateApi = {
  /**
   * 分页查询税率配置列表
   * @param params - 查询参数
   * @returns 分页数据（已转换税种）
   */
  async getList(params?: TaxRateConfigQueryForm): Promise<PageResponse<TaxRateConfig>> {
    const query = mapQueryParams(params)
    const res = await get<TaxRatePageBackend | null>('/v1/finance/tax-rate-configs', query)
    const records = (res?.records || []).map(item =>
      toFrontend(item) as unknown as TaxRateConfig
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
   * 根据ID获取税率配置详情
   * 对应后端 GET /v1/finance/tax-rate-configs/{id}
   * @param id - 配置ID
   * @returns 配置详情（已转换税种）
   */
  async getById(id: string): Promise<TaxRateConfig> {
    const res = await get<Record<string, unknown>>(`/v1/finance/tax-rate-configs/${id}`)
    return toFrontend(res) as unknown as TaxRateConfig
  },

  /**
   * 获取当前有效税率
   * 后端 GET /v1/finance/tax-rate-configs/effective?taxType={number}&taxpayerType={number}
   * 后端要求 @RequestParam Integer taxType + @RequestParam Integer taxpayerType（均必填）
   *
   * @param taxType - 税种（前端字符串，会自动转后端数字）
   * @param taxpayerType - 纳税人类型：1-一般纳税人 2-小规模纳税人
   * @returns 有效税率配置（已转换税种）
   */
  async getEffectiveRate(taxType: TaxType, taxpayerType: number): Promise<TaxRateConfig> {
    const res = await get<Record<string, unknown>>(
      '/v1/finance/tax-rate-configs/effective',
      { taxType: TaxTypeMap.toBackend[taxType], taxpayerType }
    )
    return toFrontend(res) as unknown as TaxRateConfig
  },

  /**
   * 创建税率配置
   * @param data - 税率配置表单数据（前端语义字符串）
   * @returns 创建后的税率配置（已转换税种）
   */
  async create(data: TaxRateConfigFormData): Promise<TaxRateConfig> {
    const dto = toDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/tax-rate-configs', dto)
    return toFrontend(res) as unknown as TaxRateConfig
  },

  /**
   * 更新税率配置
   * @param id - 配置ID
   * @param data - 税率配置表单数据（前端语义字符串）
   * @returns 更新后的税率配置（已转换税种）
   */
  async update(id: string, data: TaxRateConfigFormData): Promise<TaxRateConfig> {
    const dto = toDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/tax-rate-configs/${id}`, dto)
    return toFrontend(res) as unknown as TaxRateConfig
  },

  /**
   * 删除税率配置（逻辑删除）
   * @param id - 配置ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/tax-rate-configs/${id}`)
  },
}

export default taxRateApi
