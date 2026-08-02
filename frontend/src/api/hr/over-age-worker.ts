/**
 * 超龄劳动者管理API
 * 对应后端: /v1/over-age-workers
 */
import { get, post } from '../request'
import type {
  OverAgeWorkerInfo,
  OverAgeWorkerFormData,
  OverAgeWorkerQueryParams,
} from '../../types/hr/over-age-worker'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// ============================================================
// Mock 数据
// ============================================================

let mockOverAgeWorkerList: OverAgeWorkerInfo[] = [
  {
    id: 'OAW001',
    employeeId: 'EMP016',
    employeeName: '陈师傅',
    retirementDate: '2024-12-31',
    reemploymentDate: '2025-01-15',
    agreementNo: 'LW-2025-001',
    agreementStartDate: '2025-01-15',
    agreementEndDate: '2026-01-14',
    workInjuryInsuranceNo: 'GSBX-2025-001',
    workInjuryInsuranceExpiry: '2026-01-14',
    healthCheckExpiry: '2026-06-30',
    healthCheckResult: 'qualified',
    remark: '资深厨师，返聘负责特色菜品研发',
    createTime: '2025-01-10 09:00:00',
    updateTime: '2026-05-20 10:00:00',
  },
  {
    id: 'OAW002',
    employeeId: 'EMP017',
    employeeName: '刘阿姨',
    retirementDate: '2023-06-30',
    reemploymentDate: '2023-07-15',
    agreementNo: 'LW-2025-002',
    agreementStartDate: '2025-07-15',
    agreementEndDate: '2026-07-14',
    workInjuryInsuranceNo: 'GSBX-2025-002',
    workInjuryInsuranceExpiry: '2026-07-14',
    healthCheckExpiry: '2026-08-15',
    healthCheckResult: 'restricted',
    restrictedPositions: ['后厨热菜', '油炸岗位'],
    remark: '限制高温岗位，安排面点及凉菜岗位',
    createTime: '2023-07-10 09:00:00',
    updateTime: '2026-04-15 11:00:00',
  },
  {
    id: 'OAW003',
    employeeId: 'EMP018',
    employeeName: '赵师傅',
    retirementDate: '2025-03-31',
    reemploymentDate: '2025-04-15',
    agreementNo: 'LW-2025-003',
    agreementStartDate: '2025-04-15',
    agreementEndDate: '2026-04-14',
    workInjuryInsuranceNo: 'GSBX-2025-003',
    workInjuryInsuranceExpiry: '2026-04-14',
    healthCheckExpiry: '2026-03-31',
    healthCheckResult: 'unqualified',
    remark: '体检不合格，需重新体检或终止返聘',
    createTime: '2025-04-10 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
]

function mapQueryParams(params?: OverAgeWorkerQueryParams): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, keyword, healthCheckResult } = params as Record<string, unknown>
  const result: Record<string, unknown> = {}
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  if (keyword !== undefined) result.keyword = keyword
  if (healthCheckResult !== undefined) result.healthCheckResult = healthCheckResult
  return result
}

export const overAgeWorkerApi = {
  /** 分页查询超龄劳动者列表 */
  async getList(params?: OverAgeWorkerQueryParams): Promise<{ records: OverAgeWorkerInfo[]; total: number }> {
    return await get<{ records: OverAgeWorkerInfo[]; total: number }>('/v1/over-age-workers/page', mapQueryParams(params))
  },

  /** 根据ID获取超龄劳动者详情 */
  async getById(id: string): Promise<OverAgeWorkerInfo> {
    return await get<OverAgeWorkerInfo>('/v1/over-age-workers/' + id)
  },

  /** 创建超龄劳动者记录 */
  async create(data: OverAgeWorkerFormData): Promise<OverAgeWorkerInfo> {
    return await post<OverAgeWorkerInfo>('/v1/over-age-workers', data)
  },

  /** 更新超龄劳动者记录 */
  async update(id: string, data: Partial<OverAgeWorkerFormData>): Promise<OverAgeWorkerInfo> {
    return await post<OverAgeWorkerInfo>('/v1/over-age-workers/' + id, data)
  },

  /** 删除超龄劳动者记录 */
  async delete(id: string): Promise<void> {
    await post('/v1/over-age-workers/' + id + '/delete', {})
  },

  /** 获取合规预警 */
  async getComplianceWarnings(): Promise<OverAgeWorkerInfo[]> {
    return await get<OverAgeWorkerInfo[]>('/v1/over-age-workers/compliance-warnings')
  },
}
