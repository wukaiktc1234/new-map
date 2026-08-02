/**
 * 员工管理API
 * 对应后端: /v1/employees (EmployeeController)
 *
 * 说明：
 * - 所有方法对接真实后端 EmployeeController，已移除全部 Mock 数据
 * - 保留原方法名以兼容现有调用方（useEmployee.ts / HREmployee.vue / HRContract.vue）
 * - 后端实体字段 name 对应前端类型 Employee.employeeName，在 API 边界处做字段映射
 */
import { get, post, put, del } from '../request'
import type { Employee, EmployeeStatistics, EmployeeBasicInfo, EmployeeTransferData } from '../../types/hr'

/** 后端员工接口基础路径 */
const EMPLOYEE_BASE = '/v1/employees'

/** 分页查询参数 */
export interface EmployeeListParams {
  /** 当前页码（从1开始） */
  current?: number
  /** 每页条数 */
  size?: number
}

/** 分页查询结果（对应后端 /page 返回结构） */
export interface EmployeePageResult {
  records: Employee[]
  total: number
  current: number
  size: number
}

/**
 * 将后端返回的员工数据适配为前端 Employee 类型
 * 后端实体字段 name → 前端类型 employeeName，其余字段一致
 */
function normalizeEmployee(raw: Employee): Employee {
  if (raw && raw.employeeName == null) {
    const backendName = raw.name
    if (typeof backendName === 'string') {
      raw.employeeName = backendName
    }
  }
  return raw
}

/** 批量适配员工列表 */
function normalizeEmployeeList(list: Employee[] | null | undefined): Employee[] {
  return (list || []).map(normalizeEmployee)
}

/**
 * 将前端员工数据转换为后端创建/更新 DTO
 * 前端字段 employeeName → 后端字段 name
 */
function toBackendPayload(data: Partial<Employee>): Record<string, unknown> {
  const payload: Record<string, unknown> = { ...data }
  if (data.employeeName !== undefined) {
    payload.name = data.employeeName
    delete payload.employeeName
  }
  return payload
}

const employeeApi = {
  /**
   * 获取员工分页列表
   * GET /v1/employees/page
   * @param params - 分页参数
   */
  async getList(params: EmployeeListParams = {}): Promise<EmployeePageResult> {
    const res = await get<EmployeePageResult>(`${EMPLOYEE_BASE}/page`, {
      current: params.current ?? 1,
      size: params.size ?? 10,
    })
    return {
      records: normalizeEmployeeList(res?.records),
      total: res?.total ?? 0,
      current: res?.current ?? params.current ?? 1,
      size: res?.size ?? params.size ?? 10,
    }
  },

  /**
   * 获取员工列表（兼容旧调用，返回数组）
   * 内部调用分页接口并提取 records
   */
  async getEmployees(): Promise<Employee[]> {
    const res = await get<EmployeePageResult>(`${EMPLOYEE_BASE}/page`, {
      current: 1,
      size: 1000,
    })
    return normalizeEmployeeList(res?.records)
  },

  /**
   * 根据ID获取员工详情
   * GET /v1/employees/{id}
   */
  async getEmployeeById(id: string): Promise<Employee> {
    const res = await get<Employee>(`${EMPLOYEE_BASE}/${id}`)
    return normalizeEmployee(res)
  },

  /**
   * 根据部门ID获取员工列表
   * GET /v1/employees/department/{departmentId}
   */
  async getEmployeesByDepartmentId(departmentId: string): Promise<Employee[]> {
    const res = await get<Employee[]>(`${EMPLOYEE_BASE}/department/${departmentId}`)
    return normalizeEmployeeList(res)
  },

  /**
   * 根据职位ID获取员工列表
   * GET /v1/employees/position/{positionId}
   */
  async getEmployeesByPosition(positionId: string): Promise<Employee[]> {
    const res = await get<Employee[]>(`${EMPLOYEE_BASE}/position/${positionId}`)
    return normalizeEmployeeList(res)
  },

  /**
   * 创建员工
   * POST /v1/employees
   */
  async createEmployee(employee: Omit<Employee, 'id'>): Promise<Employee> {
    const res = await post<Employee>(EMPLOYEE_BASE, toBackendPayload(employee))
    return normalizeEmployee(res)
  },

  /**
   * 更新员工
   * PUT /v1/employees/{id}
   */
  async updateEmployee(id: string, employee: Partial<Employee>): Promise<Employee> {
    const res = await put<Employee>(`${EMPLOYEE_BASE}/${id}`, toBackendPayload(employee))
    return normalizeEmployee(res)
  },

  /**
   * 删除员工
   * DELETE /v1/employees/{id}
   */
  async deleteEmployee(id: string): Promise<void> {
    await del<void>(`${EMPLOYEE_BASE}/${id}`)
  },

  /**
   * 批量更新员工部门
   * PUT /v1/employees/batch/department
   * 后端使用 @RequestParam 接收 employeeIds 与 departmentId
   */
  async batchUpdateDepartment(employeeIds: string[], departmentId: string): Promise<void> {
    await put<void>(`${EMPLOYEE_BASE}/batch/department`, undefined, {
      params: { employeeIds, departmentId },
    })
  },

  /**
   * 批量更新员工职位
   * PUT /v1/employees/batch/position
   * 后端使用 @RequestParam 接收 employeeIds 与 positionId
   */
  async batchUpdatePosition(employeeIds: string[], positionId: string): Promise<void> {
    await put<void>(`${EMPLOYEE_BASE}/batch/position`, undefined, {
      params: { employeeIds, positionId },
    })
  },

  /**
   * 员工人事变动（调岗/调部门）
   * PUT /v1/employees/{id}/transfer
   */
  async transferEmployee(id: string, data: EmployeeTransferData): Promise<Employee> {
    const res = await put<Employee>(`${EMPLOYEE_BASE}/${id}/transfer`, {
      newDepartmentId: data.newDepartmentId,
      newPositionId: data.newPositionId,
      transferDate: data.transferDate,
      reason: data.reason,
    })
    return normalizeEmployee(res)
  },

  /**
   * 获取员工总数
   * GET /v1/employees/count
   */
  async getCount(): Promise<number> {
    const res = await get<{ count: number }>(`${EMPLOYEE_BASE}/count`)
    return res?.count ?? 0
  },

  /**
   * 获取部门员工数量
   * GET /v1/employees/count/department/{departmentId}
   */
  async getEmployeeCountByDepartmentId(departmentId: string): Promise<number> {
    const res = await get<number>(`${EMPLOYEE_BASE}/count/department/${departmentId}`)
    return res ?? 0
  },

  /**
   * 获取职位员工数量
   * GET /v1/employees/count/position/{positionId}
   */
  async getEmployeeCountByPosition(positionId: string): Promise<number> {
    const res = await get<number>(`${EMPLOYEE_BASE}/count/position/${positionId}`)
    return res ?? 0
  },

  /**
   * 批量获取员工基本信息
   * POST /v1/employees/batch/basic-info
   * 后端使用 @RequestBody 接收员工ID列表，返回以员工ID为key的映射
   */
  async getBatchBasicInfo(employeeIds: string[]): Promise<Record<string, EmployeeBasicInfo>> {
    return post<Record<string, EmployeeBasicInfo>>(
      `${EMPLOYEE_BASE}/batch/basic-info`,
      employeeIds
    )
  },

  /**
   * 获取员工全局统计
   * GET /v1/employees/statistics
   * 统计数据不受列表筛选条件影响，反映全量员工状态
   */
  async getStatistics(): Promise<EmployeeStatistics> {
    const res = await get<{ total: number; statusDistribution: { active: number; inactive: number; probation: number } }>('/v1/employees/statistics')
    return {
      active: res?.statusDistribution?.active ?? 0,
      probation: res?.statusDistribution?.probation ?? 0,
      inactive: res?.statusDistribution?.inactive ?? 0,
      newThisMonth: 0,
      overAge: 0,
      total: res?.total ?? 0,
    }
  },
}

export default employeeApi
