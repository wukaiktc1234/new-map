/**
 * 结转模板API
 * 对应后端: /v1/finance/transfer-templates
 *
 * 【设计说明】
 * - 后端 TransferTemplateController 6 个端点：create/update/delete/getDetail/getPage/toggleEnabled
 * - toggleEnabled 后端要求 @RequestParam Boolean enabled（必填），前端调用需传参
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 */
import { get, post, put, del } from '../request'
import type {
  FinanceTransferTemplate,
  FinanceTransferTemplateFormData,
  FinanceTransferTemplateQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 */
function mapQueryParams(params?: FinanceTransferTemplateQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  return result
}

/** 后端 IPage 分页响应 */
interface TransferTemplatePageBackend {
  records: FinanceTransferTemplate[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const transferTemplateApi = {
  /**
   * 分页查询结转模板列表
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(params?: FinanceTransferTemplateQueryForm): Promise<PageResponse<FinanceTransferTemplate>> {
    const query = mapQueryParams(params)
    const res = await get<TransferTemplatePageBackend | null>('/v1/finance/transfer-templates', query)
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取结转模板详情
   * 对应后端 GET /v1/finance/transfer-templates/{id}
   * @param id - 模板ID
   * @returns 模板详情
   */
  async getById(id: string): Promise<FinanceTransferTemplate> {
    return await get<FinanceTransferTemplate>(`/v1/finance/transfer-templates/${id}`)
  },

  /**
   * 创建结转模板
   * @param data - 模板表单数据
   * @returns 创建后的模板
   */
  async create(data: FinanceTransferTemplateFormData): Promise<FinanceTransferTemplate> {
    return await post<FinanceTransferTemplate>('/v1/finance/transfer-templates', data)
  },

  /**
   * 更新结转模板
   * @param id - 模板ID
   * @param data - 模板表单数据
   * @returns 更新后的模板
   */
  async update(id: string, data: FinanceTransferTemplateFormData): Promise<FinanceTransferTemplate> {
    return await put<FinanceTransferTemplate>(`/v1/finance/transfer-templates/${id}`, data)
  },

  /**
   * 删除结转模板（逻辑删除）
   * @param id - 模板ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/transfer-templates/${id}`)
  },

  /**
   * 启用/禁用结转模板
   * 后端 PUT /v1/finance/transfer-templates/{id}/toggle-enabled?enabled={boolean}
   * 后端要求 @RequestParam Boolean enabled（必填）
   *
   * @param id - 模板ID
   * @param enabled - 是否启用（必填）
   * @returns 更新后的模板
   */
  async toggleEnabled(id: string, enabled: boolean): Promise<FinanceTransferTemplate> {
    return await put<FinanceTransferTemplate>(
      `/v1/finance/transfer-templates/${id}/toggle-enabled`,
      null,
      { params: { enabled } }
    )
  },
}

export default transferTemplateApi
