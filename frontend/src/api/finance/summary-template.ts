/**
 * 摘要模板API
 * 对应后端: /v1/finance/summary-templates
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 */
import { get, post, put, del } from '../request'
import type {
  SummaryTemplate,
  SummaryTemplateFormData,
  SummaryTemplateQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 */
function mapQueryParams(params?: SummaryTemplateQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  return result
}

/** 后端 IPage 分页响应 */
interface SummaryTemplatePageBackend {
  records: SummaryTemplate[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const summaryTemplateApi = {
  /**
   * 分页查询摘要模板列表
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(params?: SummaryTemplateQueryForm): Promise<PageResponse<SummaryTemplate>> {
    const query = mapQueryParams(params)
    const res = await get<SummaryTemplatePageBackend | null>('/v1/finance/summary-templates', query)
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 搜索摘要模板
   * @param params - 查询参数
   * @returns 模板列表
   */
  async search(params?: SummaryTemplateQueryForm): Promise<SummaryTemplate[]> {
    const query = mapQueryParams(params)
    const res = await get<SummaryTemplate[] | null>('/v1/finance/summary-templates/search', query)
    return res ?? []
  },

  /**
   * 创建摘要模板
   * @param data - 模板表单数据
   * @returns 创建后的模板
   */
  async create(data: SummaryTemplateFormData): Promise<SummaryTemplate> {
    return await post<SummaryTemplate>('/v1/finance/summary-templates', data)
  },

  /**
   * 更新摘要模板
   * @param id - 模板ID
   * @param data - 模板表单数据
   * @returns 更新后的模板
   */
  async update(id: string, data: SummaryTemplateFormData): Promise<SummaryTemplate> {
    return await put<SummaryTemplate>(`/v1/finance/summary-templates/${id}`, data)
  },

  /**
   * 删除摘要模板
   * @param id - 模板ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/summary-templates/${id}`)
  },

  /**
   * 增加模板使用次数
   * @param id - 模板ID
   * @returns 更新后的模板
   */
  async incrementUsage(id: string): Promise<SummaryTemplate> {
    return await put<SummaryTemplate>(`/v1/finance/summary-templates/${id}/increment-usage`)
  },
}

export default summaryTemplateApi
