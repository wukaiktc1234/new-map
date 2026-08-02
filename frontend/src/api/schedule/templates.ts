/**
 * 排班模板管理 API
 * 对应后端: /v1/schedule/templates
 */
import { get, post, put, del } from '@/api/request'
import type {
  ScheduleTemplate,
  TemplateStatus,
} from '@/types/schedule'
import type { IPage } from '@/types/pagination'

/** 后端返回的模板数据（原始格式） */
interface ScheduleTemplateBackend {
  templateId: string
  templateName: string
  description?: string
  storeId: string
  demandMatrix: Record<string, unknown>
  enabledRuleIds: string[]
  isDefault: boolean
  status: string | number
  useCount: number
  createTime: string
  updateTime: string
}

/** 分页响应类型 */
type TemplatePageResponse = IPage<ScheduleTemplateBackend>

export const templateApi = {
  /**
   * 获取模板列表（分页）
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(
    params: { page?: number; size?: number; keyword?: string; status?: TemplateStatus | '' }
  ): Promise<TemplatePageResponse> {
    const res = await get<TemplatePageResponse>(
      '/v1/schedule/templates',
      params as Record<string, unknown>
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return res
  },

  /**
   * 根据ID获取模板详情
   * @param id - 模板ID
   * @returns 模板详情
   */
  async getById(id: string): Promise<ScheduleTemplate> {
    return get<ScheduleTemplate>(`/v1/schedule/templates/${id}`)
  },

  /**
   * 创建排班模板
   * @param data - 模板数据
   * @returns 创建的模板
   */
  async create(data: Partial<ScheduleTemplate>): Promise<ScheduleTemplate> {
    return post<ScheduleTemplate>('/v1/schedule/templates', data)
  },

  /**
   * 更新排班模板
   * @param id - 模板ID
   * @param data - 模板数据
   * @returns 更新后的模板
   */
  async update(
    id: string,
    data: Partial<ScheduleTemplate>
  ): Promise<ScheduleTemplate> {
    return put<ScheduleTemplate>(
      `/v1/schedule/templates/${id}`,
      data
    )
  },

  /**
   * 删除排班模板
   * @param id - 模板ID
   */
  async delete(id: string): Promise<void> {
    await del(`/v1/schedule/templates/${id}`)
  },

  /**
   * 复制模板
   * @param id - 源模板ID
   * @param newName - 新模板名称
   * @returns 复制后的模板
   */
  async copy(
    id: string,
    newName: string
  ): Promise<ScheduleTemplate> {
    return post<ScheduleTemplate>(
      `/v1/schedule/templates/${id}/copy`,
      { newName }
    )
  },

  /**
   * 切换模板状态（启用/停用）
   * 后端为切换式调用，无需传递目标状态
   * @param id - 模板ID
   */
  async toggleStatus(id: string): Promise<void> {
    await put(`/v1/schedule/templates/${id}/toggle-status`)
  },

  /**
   * 设置默认模板
   * @param id - 模板ID
   */
  async setDefault(id: string): Promise<void> {
    await put(`/v1/schedule/templates/${id}/set-default`)
  },
}

export default templateApi
