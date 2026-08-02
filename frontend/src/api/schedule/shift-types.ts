/**
 * 班次类型管理 API
 * 对应后端: /v1/schedule/shift-types
 */
import { get, post, put } from '@/api/request'
import type { ScheduleShiftType, ShiftTypeCode } from '@/types/schedule'

/** 后端返回的班次类型数据（原始格式） */
interface ScheduleShiftTypeBackend {
  shiftTypeId: string
  shiftCode: string
  shiftName: string
  storeId: string
  startTime: string
  endTime: string
  color: string
  durationMinutes: number
  isRest: boolean
  sortOrder: number
  status: string | number
  createTime: string
  updateTime: string
}

/** 排序请求体 */
interface ReorderPayload {
  /** 排序项列表 */
  items: Array<{
    id: string
    sortOrder: number
  }>
}

export const shiftTypeApi = {
  /**
   * 获取所有班次类型（含停用，按排序顺序）
   * @param storeId - 门店ID
   * @returns 班次类型列表
   */
  async getAll(storeId: string): Promise<ScheduleShiftType[]> {
    const res = await get<ScheduleShiftTypeBackend[]>(
      '/v1/schedule/shift-types/all',
      { storeId }
    )
    return (res || []).map(this.convertToFrontend)
  },

  /**
   * 获取启用的班次类型（用于表单选择）
   * @param storeId - 门店ID
   * @returns 启用的班次类型列表
   */
  async getActive(storeId: string): Promise<ScheduleShiftType[]> {
    const res = await get<ScheduleShiftTypeBackend[]>(
      '/v1/schedule/shift-types',
      { storeId }
    )
    return (res || []).map(this.convertToFrontend)
  },

  /**
   * 根据ID获取班次类型详情
   * @param id - 班次类型ID
   * @returns 班次类型详情
   */
  async getById(id: string): Promise<ScheduleShiftType> {
    const res = await get<ScheduleShiftTypeBackend>(
      `/v1/schedule/shift-types/${id}`
    )
    return this.convertToFrontend(res)
  },

  /**
   * 创建班次类型
   * @param data - 班次类型数据
   * @returns 创建的班次类型
   */
  async create(data: Partial<ScheduleShiftType>): Promise<ScheduleShiftType> {
    const res = await post<ScheduleShiftTypeBackend>(
      '/v1/schedule/shift-types',
      data
    )
    return this.convertToFrontend(res)
  },

  /**
   * 更新班次类型
   * @param id - 班次类型ID
   * @param data - 班次类型数据
   * @returns 更新后的班次类型
   */
  async update(
    id: string,
    data: Partial<ScheduleShiftType>
  ): Promise<ScheduleShiftType> {
    const res = await put<ScheduleShiftTypeBackend>(
      `/v1/schedule/shift-types/${id}`,
      data
    )
    return this.convertToFrontend(res)
  },

  /**
   * 切换班次状态（启用/停用）
   * 后端为切换式调用，无需传递目标状态
   * @param id - 班次类型ID
   */
  async toggleStatus(id: string): Promise<void> {
    await put(`/v1/schedule/shift-types/${id}/toggle-status`)
  },

  /**
   * 批量调整班次排序
   * @param items - 排序项列表
   */
  async reorder(items: ReorderPayload['items']): Promise<void> {
    await put('/v1/schedule/shift-types/reorder', { items })
  },

  /**
   * 将后端数据转换为前端格式
   */
  convertToFrontend(backend: ScheduleShiftTypeBackend): ScheduleShiftType {
    return {
      ...backend,
      shiftCode: backend.shiftCode as ShiftTypeCode,
      status:
        backend.status === 1 || backend.status === '1'
          ? 'active'
          : 'inactive',
    }
  },
}

export default shiftTypeApi
