/**
 * 排班计划管理 API
 * 对应后端: /v1/schedule/plans
 */
import { get, post, put, del } from '@/api/request'
import { schedulePlanConverter } from './converters'
import type {
  SchedulePlan,
  SchedulePlanFormData,
  SchedulePlanQueryForm,
} from '@/types/schedule'
import type { IPage } from '@/types/pagination'

/** 后端返回的排班计划数据（原始格式） */
interface SchedulePlanBackend {
  planId: string
  planName: string
  storeId: string
  storeName: string
  startDate: string
  endDate: string
  status: string | number
  version: number
  templateId?: string
  templateName?: string
  employeeCount: number
  totalWorkHours: number
  publisherId?: string
  publisherName?: string
  publishTime?: string
  attendanceSyncStatus: string | number
  attendanceSyncTime?: string
  conflictCheckStatus: string | number
  conflictCheckTime?: string
  createTime: string
  updateTime: string
}

/** 计划统计信息 */
interface SchedulePlanStats {
  totalPlans: number
  draftCount: number
  publishedCount: number
  executingCount: number
  archivedCount: number
  thisWeekPlans: number
}

/** 分页响应类型（后端原始格式） */
type PlanPageResponseBackend = IPage<SchedulePlanBackend>

/** 分页响应类型（前端转换后格式） */
type PlanPageResponse = IPage<SchedulePlan>

export const schedulePlanApi = {
  /**
   * 获取排班计划列表（分页）
   * @param params - 查询参数
   * @returns 分页数据（records 已转换为前端类型）
   */
  async getList(params: SchedulePlanQueryForm): Promise<PlanPageResponse> {
    const res = await get<PlanPageResponseBackend>(
      '/v1/schedule/plans',
      params
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: (res.records || []).map((item: SchedulePlanBackend) =>
        schedulePlanConverter.toFrontend(item)
      ),
    }
  },

  /**
   * 根据ID获取排班计划详情
   * @param id - 计划ID
   * @returns 计划详情
   */
  async getById(id: string): Promise<SchedulePlan> {
    const res = await get<SchedulePlanBackend>(
      `/v1/schedule/plans/${id}`
    )
    return schedulePlanConverter.toFrontend(res)
  },

  /**
   * 创建排班计划
   * @param data - 表单数据
   * @returns 创建的计划
   */
  async create(data: SchedulePlanFormData): Promise<SchedulePlan> {
    const res = await post<SchedulePlanBackend>(
      '/v1/schedule/plans',
      data
    )
    return schedulePlanConverter.toFrontend(res)
  },

  /**
   * 更新排班计划
   * @param id - 计划ID
   * @param data - 表单数据
   * @returns 更新后的计划
   */
  async update(
    id: string,
    data: SchedulePlanFormData
  ): Promise<SchedulePlan> {
    const res = await put<SchedulePlanBackend>(
      `/v1/schedule/plans/${id}`,
      data
    )
    return schedulePlanConverter.toFrontend(res)
  },

  /**
   * 删除排班计划
   * @param id - 计划ID
   */
  async delete(id: string): Promise<void> {
    await del(`/v1/schedule/plans/${id}`)
  },

  /**
   * 发布排班计划
   * @param id - 计划ID
   */
  async publish(id: string): Promise<void> {
    await put(`/v1/schedule/plans/${id}/publish`)
  },

  /**
   * 撤回已发布的排班计划
   * @param id - 计划ID
   */
  async withdraw(id: string): Promise<void> {
    // 后端 @RequestBody required=true，需发送空对象作为请求体避免 400
    await put(`/v1/schedule/plans/${id}/withdraw`, {})
  },

  /**
   * 获取排班计划统计信息
   * @returns 统计数据
   */
  async getStats(): Promise<SchedulePlanStats> {
    return get<SchedulePlanStats>('/v1/schedule/plans/stats')
  },
}

export default schedulePlanApi
