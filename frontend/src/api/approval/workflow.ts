/**
 * 审批流程API
 * 对应后端: /v1/approval/workflows
 *
 * 所有接口均调用真实后端服务，审批记录持久化到数据库。
 * 审批通过/驳回会触发 BusinessApprovalHandler 完成业务状态联动。
 */
import { get, post, put, del, silentGet } from '@/api/request'
import type {
  ApprovalWorkflow,
  ApprovalWorkflowFormData,
  ApprovalWorkflowQueryForm,
  ApprovalAuditLog,
  ApprovalCurrentNode,
  BusinessType,
  TemplateCode,
} from '@/types/approval-workflow'

/** 后端分页结果统一结构 */
interface PageResult<T> {
  total: number
  records: T[]
  current: number
  size: number
}

// ============================================================
// API 定义
// ============================================================

export const approvalWorkflowApi = {
  /** 分页查询审批流程列表 */
  async getList(params?: ApprovalWorkflowQueryForm): Promise<{ records: ApprovalWorkflow[]; total: number }> {
    const res = await get<PageResult<ApprovalWorkflow> | null>('/v1/approval/workflows/page', params as Record<string, unknown>)
    return {
      records: res?.records || [],
      total: res?.total ?? 0,
    }
  },

  /** 获取审批流程详情 */
  async getById(id: string): Promise<ApprovalWorkflow | null> {
    return get<ApprovalWorkflow | null>(`/v1/approval/workflows/${id}`)
  },

  /** 新建审批流程 */
  async create(data: ApprovalWorkflowFormData): Promise<ApprovalWorkflow> {
    return post<ApprovalWorkflow>('/v1/approval/workflows', data)
  },

  /** 更新审批流程 */
  async update(id: string, data: ApprovalWorkflowFormData): Promise<ApprovalWorkflow> {
    return put<ApprovalWorkflow>(`/v1/approval/workflows/${id}`, data)
  },

  /** 删除审批流程 */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/approval/workflows/${id}`)
  },

  /** 启用/禁用审批流程 */
  async toggleEnabled(id: string, enabled: boolean): Promise<void> {
    await put<void>(`/v1/approval/workflows/${id}/toggle-enabled`, { enabled })
  },

  /** 获取默认审批流程（根据业务类型+权限模板）— 静默加载，失败不弹提示 */
  async getDefaultWorkflow(businessType: BusinessType, templateCode: TemplateCode): Promise<ApprovalWorkflow | null> {
    return silentGet<ApprovalWorkflow | null>('/v1/approval/workflows/default', {
      businessType,
      templateCode,
    })
  },

  /** 获取当前审批节点信息 */
  async getCurrentNode(businessId: string, businessType: string): Promise<ApprovalCurrentNode | null> {
    return get<ApprovalCurrentNode | null>('/v1/approval/workflows/current-node', {
      businessId,
      businessType,
    })
  },

  /** 提交审批 */
  async submitApproval(businessType: BusinessType, businessId: string): Promise<void> {
    await post<void>('/v1/approval/workflows/submit', { businessType, businessId })
  },

  /** 审批通过 */
  async approve(params: { businessId: string; businessType: string; comment: string }): Promise<void> {
    await post<void>('/v1/approval/workflows/approve', params)
  },

  /** 审批驳回 */
  async reject(params: { businessId: string; businessType: string; comment: string }): Promise<void> {
    await post<void>('/v1/approval/workflows/reject', params)
  },

  /** 撤回审批 */
  async withdraw(businessId: string): Promise<void> {
    await post<void>('/v1/approval/workflows/withdraw', { businessId })
  },

  /** 获取审批记录 */
  async getApprovalLogs(businessId: string): Promise<ApprovalAuditLog[]> {
    return get<ApprovalAuditLog[]>(`/v1/approval/workflows/${businessId}/logs`)
  },
}

export default approvalWorkflowApi
