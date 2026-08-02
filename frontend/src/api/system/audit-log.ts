/**
 * 操作审计日志 API
 * 对应后端: /v1/audit-logs (AuditLogController)
 *
 * 用于查询系统操作审计日志，支持按用户/操作类型/模块/风险等级/IP/时间区间筛选
 */
import { get, del, post } from '../request'

/** 操作类型枚举 */
export type AuditOperationType =
  | 'LOGIN' | 'LOGOUT' | 'CREATE' | 'UPDATE' | 'DELETE'
  | 'EXPORT' | 'VIEW' | 'AUTH' | 'SYSTEM' | 'SECURITY'

/** 风险等级枚举 */
export type AuditRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

/** 响应状态枚举 */
export type AuditResponseStatus = 'SUCCESS' | 'FAILED' | 'PARTIAL'

/** 审计日志记录 */
export interface AuditLogItem {
  id: number
  tenantId: string | null
  requestId: string | null
  userId: string | null
  username: string | null
  operation: string | null
  module: string | null
  operationType: AuditOperationType | string
  ip: string | null
  userAgent: string | null
  requestUrl: string | null
  requestMethod: string | null
  requestParams: string | null
  responseStatus: AuditResponseStatus | string
  responseBody: string | null
  errorMessage: string | null
  executionTime: number | null
  businessKey: string | null
  riskLevel: AuditRiskLevel | string
  sensitiveFlag: number | null
  sessionId: string | null
  createdAt: string
}

/** 分页响应 */
export interface AuditLogPageResponse {
  records: AuditLogItem[]
  total: number
  current: number
  size: number
  pages: number
}

/** 查询参数 */
export interface AuditLogQueryParams {
  current?: number
  size?: number
  userId?: string
  username?: string
  operationType?: AuditOperationType | ''
  module?: string
  riskLevel?: AuditRiskLevel | ''
  ip?: string
  sensitiveFlag?: number | ''
  startTime?: string
  endTime?: string
}

/** 统计信息 */
export interface AuditLogStatistics {
  total: number
  highRisk: number
  criticalRisk: number
  failedOperations: number
  sensitiveOperations: number
  topUsers?: Array<{ username: string; count: number }>
  topModules?: Array<{ module: string; count: number }>
  trend?: Array<{ date: string; count: number }>
}

/** 操作类型选项 */
export const AUDIT_OPERATION_TYPE_OPTIONS: Array<{ value: AuditOperationType; label: string }> = [
  { value: 'LOGIN', label: '登录' },
  { value: 'LOGOUT', label: '登出' },
  { value: 'CREATE', label: '创建' },
  { value: 'UPDATE', label: '更新' },
  { value: 'DELETE', label: '删除' },
  { value: 'EXPORT', label: '导出' },
  { value: 'VIEW', label: '查看' },
  { value: 'AUTH', label: '认证' },
  { value: 'SYSTEM', label: '系统' },
  { value: 'SECURITY', label: '安全' },
]

/** 风险等级选项 */
export const AUDIT_RISK_LEVEL_OPTIONS: Array<{ value: AuditRiskLevel; label: string; tagType: string }> = [
  { value: 'LOW', label: '低', tagType: 'info' },
  { value: 'MEDIUM', label: '中', tagType: 'warning' },
  { value: 'HIGH', label: '高', tagType: 'danger' },
  { value: 'CRITICAL', label: '严重', tagType: 'danger' },
]

export const auditLogApi = {
  /**
   * 分页查询审计日志
   */
  async getList(params: AuditLogQueryParams = {}): Promise<AuditLogPageResponse> {
    return await get<AuditLogPageResponse>('/v1/audit-logs', params as Record<string, unknown>) || {
      records: [],
      total: 0,
      current: 1,
      size: 20,
      pages: 0,
    }
  },

  /**
   * 获取统计信息
   */
  async getStatistics(params: { startDate?: string; endDate?: string } = {}): Promise<AuditLogStatistics> {
    return await get<AuditLogStatistics>('/v1/audit-logs/statistics', params as Record<string, unknown>) || {
      total: 0,
      highRisk: 0,
      criticalRisk: 0,
      failedOperations: 0,
      sensitiveOperations: 0,
    }
  },

  /**
   * 导出审计日志（CSV，触发浏览器下载）
   * 使用 request 实例的 responseType: 'blob' 能力获取二进制数据
   */
  async export(params: Omit<AuditLogQueryParams, 'current' | 'size'> = {}): Promise<Blob | null> {
    const filteredParams = Object.entries(params)
      .filter(([, v]) => v !== '' && v !== undefined && v !== null)
      .reduce<Record<string, unknown>>((acc, [k, v]) => { acc[k] = v; return acc }, {})
    try {
      return await get<Blob>('/v1/audit-logs/export', filteredParams, { responseType: 'blob' })
    } catch {
      return null
    }
  },

  /**
   * 清理过期审计日志（已废弃）。
   * 审计日志仅允许 INSERT，禁止 UPDATE 和 DELETE（会计法/网络安全法要求）。
   * 此方法保留仅为前端兼容，后端已不再执行任何删除操作。
   * 如需管理历史日志，请使用 archive() 方法（归档不删除原表数据）。
   */
  async cleanExpired(_retentionDays = 180): Promise<{ cleaned: number }> {
    // 审计日志不可删除（法规要求），此方法已废弃，不再调用后端接口
    return { cleaned: 0 }
  },

  /**
   * 归档历史日志（仅 ADMIN）
   */
  async archive(retentionDays = 90): Promise<{ archived: number }> {
    return await post<{ archived: number }>('/v1/audit-logs/archive', undefined, {
      params: { retentionDays },
    } as Record<string, unknown>) || { archived: 0 }
  },
}

export default auditLogApi
