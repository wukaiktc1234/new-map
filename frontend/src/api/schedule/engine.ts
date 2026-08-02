/**
 * 排班引擎 API
 * 对应后端: /v1/schedule/engine
 */
import { get, post } from '@/api/request'
import type {
  ScheduleTemplate,
  ScheduleEntry,
  GenerateScheduleFormData,
} from '@/types/schedule'

/** 不可用时间段 */
interface UnavailablePeriod {
  /** 员工ID */
  employeeId: string
  /** 日期 (YYYY-MM-DD) */
  date: string
  /** 开始时间 (HH:mm) */
  startTime: string
  /** 结束时间 (HH:mm) */
  endTime: string
  /** 原因 */
  reason: string
}

export const engineApi = {
  /**
   * 触发自动排班生成（仅预览，不保存）
   * @param data - 生成参数
   * @returns 生成的排班条目列表
   */
  async generate(data: GenerateScheduleFormData): Promise<ScheduleEntry[]> {
    const res = await post<ScheduleEntry[]>(
      '/v1/schedule/engine/generate',
      data
    )
    return res || []
  },

  /**
   * 触发自动排班生成并保存
   * @param data - 生成参数
   * @returns 保存后的排班条目列表
   */
  async generateAndSave(
    data: GenerateScheduleFormData
  ): Promise<ScheduleEntry[]> {
    const res = await post<ScheduleEntry[]>(
      '/v1/schedule/engine/generate-and-save',
      data
    )
    return res || []
  },

  /**
   * 获取可用的排班模板列表
   * @returns 模板列表
   */
  async getTemplates(): Promise<ScheduleTemplate[]> {
    const res = await get<ScheduleTemplate[]>(
      '/v1/schedule/engine/templates'
    )
    return res || []
  },

  /**
   * 获取员工不可用时间段
   * @param planId - 计划ID
   * @param employeeIds - 员工ID列表（可选，不传则返回所有员工）
   * @returns 不可用时间段列表
   */
  async getUnavailable(
    planId: string,
    employeeIds?: string[]
  ): Promise<UnavailablePeriod[]> {
    const params = { planId, employeeIds } as Record<string, unknown>
    const res = await get<UnavailablePeriod[]>(
      '/v1/schedule/engine/unavailable',
      params
    )
    return res || []
  },
}

export default engineApi
