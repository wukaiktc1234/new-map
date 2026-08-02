/**
 * 审批流配置API
 * 对应后端: /v1/finance/approval-flow-configs
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 */
import { get, post, put, del } from '../request'
import type {
  ApprovalFlowConfig,
  ApprovalFlowConfigFormData,
  ApprovalFlowConfigQueryForm,
  ApprovalDocumentType,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 */
function mapQueryParams(params?: ApprovalFlowConfigQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  return result
}

/** 后端 IPage 分页响应 */
interface ApprovalFlowPageBackend {
  records: ApprovalFlowConfig[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const approvalFlowApi = {
  /**
   * 分页查询审批流配置列表
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(params?: ApprovalFlowConfigQueryForm): Promise<PageResponse<ApprovalFlowConfig>> {
    const query = mapQueryParams(params)
    const res = await get<ApprovalFlowPageBackend | null>('/v1/finance/approval-flow-configs', query)
    return {
      records: res?.records ?? [],
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据单据类型查询审批流配置
   * @param documentType - 单据类型
   * @returns 审批流配置详情
   */
  async getByDocumentType(documentType: ApprovalDocumentType): Promise<ApprovalFlowConfig> {
    return await get<ApprovalFlowConfig>(`/v1/finance/approval-flow-configs/by-document-type/${documentType}`)
  },

  /**
   * 创建审批流配置
   * @param data - 审批流配置表单数据
   * @returns 创建后的审批流配置
   */
  async create(data: ApprovalFlowConfigFormData): Promise<ApprovalFlowConfig> {
    return await post<ApprovalFlowConfig>('/v1/finance/approval-flow-configs', data)
  },

  /**
   * 更新审批流配置
   * @param id - 配置ID
   * @param data - 审批流配置表单数据
   * @returns 更新后的审批流配置
   */
  async update(id: string, data: ApprovalFlowConfigFormData): Promise<ApprovalFlowConfig> {
    return await put<ApprovalFlowConfig>(`/v1/finance/approval-flow-configs/${id}`, data)
  },

  /**
   * 删除审批流配置
   * @param id - 配置ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/approval-flow-configs/${id}`)
  },

  /**
   * 启用/禁用审批流配置
   * @param id - 配置ID
   * @returns 更新后的审批流配置
   */
  async toggleEnabled(id: string): Promise<ApprovalFlowConfig> {
    return await put<ApprovalFlowConfig>(`/v1/finance/approval-flow-configs/${id}/toggle-enabled`)
  },
}

export default approvalFlowApi
