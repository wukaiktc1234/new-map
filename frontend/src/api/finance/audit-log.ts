/**
 * 财务审计日志API
 * 对应后端: /v1/finance/audit-logs
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get（替代 silentGet）
 */
import { get } from '../request'
import type {
  FinanceAuditLog,
  FinanceAuditLogQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 */
function mapQueryParams(params?: FinanceAuditLogQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  return result
}

/** 后端 IPage 分页响应 */
interface AuditLogPageBackend {
  records: FinanceAuditLog[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const auditLogApi = {
  /**
   * 分页查询财务审计日志列表
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(params?: FinanceAuditLogQueryForm): Promise<PageResponse<FinanceAuditLog>> {
    const query = mapQueryParams(params)
    const res = await get<AuditLogPageBackend | null>('/v1/finance/audit-logs', query)
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取审计日志详情
   * @param id - 日志ID
   * @returns 日志详情
   */
  async getById(id: string): Promise<FinanceAuditLog> {
    return await get<FinanceAuditLog>(`/v1/finance/audit-logs/${id}`)
  },
}

export default auditLogApi
