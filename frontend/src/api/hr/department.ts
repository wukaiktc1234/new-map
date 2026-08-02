/**
 * 部门管理API
 * 对接后端: /v1/departments
 *
 * 注意：本文件早期版本曾包含 mockDepartmentTree 等硬编码降级数据，
 * 已于本次重构中移除。所有方法均调用真实后端 API，失败时由调用方
 * （如 useDepartmentOptions composable）统一处理降级逻辑。
 */
import { get, post, put, del } from '../request'
import type { DepartmentDTO, Department } from '../../types/hr'

export const departmentApi = {
  /** 获取部门列表（扁平化） */
  async getDepartments(): Promise<DepartmentDTO[]> {
    return await get<DepartmentDTO[]>('/v1/departments/page')
  },

  /** 获取部门树结构 */
  async getDepartmentTree(): Promise<DepartmentDTO[]> {
    return await get<DepartmentDTO[]>('/v1/departments/tree')
  },

  /** 根据ID获取部门详情 */
  async getDepartment(id: string): Promise<DepartmentDTO> {
    return await get<DepartmentDTO>('/v1/departments/' + id)
  },

  /** 新增部门 */
  async addDepartment(dept: Department): Promise<DepartmentDTO> {
    return await post<DepartmentDTO>('/v1/departments', dept)
  },

  /** 更新部门 */
  async updateDepartment(id: string, dept: Partial<Department>): Promise<DepartmentDTO> {
    return await put<DepartmentDTO>('/v1/departments/' + id, dept)
  },

  /** 删除部门 */
  async deleteDepartment(id: string): Promise<void> {
    await del('/v1/departments/' + id)
  },

  /** 更新部门状态（启用/禁用，级联子部门） */
  async updateDepartmentStatus(id: string, status: 'active' | 'inactive'): Promise<void> {
    await put('/v1/departments/' + id + '/status/cascade', { status })
  },

  /** 更新部门排序 */
  async updateSortOrder(id: string, sortOrder: number): Promise<void> {
    await put('/v1/departments/' + id + '/sort', { sortOrder })
  },
}
