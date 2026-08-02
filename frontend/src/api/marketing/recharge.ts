/**
 * 储值管理API
 * 对应后端: /v1/recharge-plans, /v1/recharge-records, /v1/refunds, /v1/recharge-settings, /v1/recharge-stats, /v1/recharge-finance, /v1/anomaly-alerts
 *
 * 设计原则：
 * 1. 本金/赠送余额分离（核心设计，影响退款、过期、会计）
 * 2. 所有风控参数可配置（不硬编码任何业务规则）
 * 3. 消费扣减顺序：先扣赠送余额，再扣本金余额
 * 4. 退款仅退本金，赠送不退
 * 5. 预留财务系统对接接口
 */
import { get, post, put, del } from '../request'
import type { PageResponse } from '@/types'
import type {
  RechargePlanInfo,
  RechargePlanForm,
  RechargePlanQueryForm,
  RechargeRecordInfo,
  RechargeRecordQueryForm,
  RechargePaymentMethod,
  RefundRequestInfo,
  RefundRequestStatus,
  RechargeSystemSettings,
  RechargeStatsOverview,
  RechargeFinanceLog,
  FinanceLogType,
  AnomalyAlert,
} from '@/types/member-recharge'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 * 储值模块状态字段无数字编码映射，保持原值
 */
function mapQueryParams(params?: Record<string, unknown>): Record<string, unknown> {
  if (!params) return {}
  const { page, size, ...rest } = params
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  return result
}

// ============================================================
// API - 充值方案管理
// ============================================================

export const rechargePlanApi = {
  /** 获取充值方案列表 */
  async getList(params?: RechargePlanQueryForm): Promise<RechargePlanInfo[]> {
    return await get<RechargePlanInfo[]>('/v1/recharge-plans', mapQueryParams(params as unknown as Record<string, unknown>))
  },

  /** 获取充值方案详情 */
  async getById(id: string): Promise<RechargePlanInfo | null> {
    return await get<RechargePlanInfo | null>(`/v1/recharge-plans/${id}`)
  },

  /** 创建充值方案 */
  async create(data: RechargePlanForm): Promise<RechargePlanInfo | null> {
    return await post<RechargePlanInfo | null>('/v1/recharge-plans', data)
  },

  /** 更新充值方案 */
  async update(id: string, data: RechargePlanForm): Promise<RechargePlanInfo | null> {
    return await put<RechargePlanInfo | null>(`/v1/recharge-plans/${id}`, data)
  },

  /** 删除充值方案 */
  async delete(id: string): Promise<void> {
    await del(`/v1/recharge-plans/${id}`)
  },

  /** 切换方案状态 */
  async toggleStatus(id: string): Promise<void> {
    await put(`/v1/recharge-plans/${id}/toggle-status`)
  },
}

// ============================================================
// API - 充值记录管理
// ============================================================

export const rechargeRecordApi = {
  /** 分页获取充值记录 */
  async getPage(params: RechargeRecordQueryForm & { page: number; size: number }): Promise<PageResponse<RechargeRecordInfo>> {
    return await get<PageResponse<RechargeRecordInfo>>('/v1/recharge-records', mapQueryParams(params as unknown as Record<string, unknown>))
  },

  /** 获取充值记录详情 */
  async getById(id: string): Promise<RechargeRecordInfo | null> {
    return await get<RechargeRecordInfo | null>(`/v1/recharge-records/${id}`)
  },

  /** 为会员充值（会员详情页调用） */
  async recharge(memberId: string, planId: string, paymentMethod: RechargePaymentMethod): Promise<RechargeRecordInfo | null> {
    return await post<RechargeRecordInfo | null>('/v1/recharge-records/recharge', { memberId, planId, paymentMethod })
  },

  /** 导出充值记录 */
  async export(params: RechargeRecordQueryForm): Promise<void> {
    await post('/v1/recharge-records/export', params)
  },
}

// ============================================================
// API - 退款管理
// ============================================================

export const refundApi = {
  /** 创建退款申请 */
  async create(data: { rechargeRecordId: string; requestedAmount: string; refundReason: string }): Promise<RefundRequestInfo | null> {
    return await post<RefundRequestInfo | null>('/v1/refunds', data)
  },

  /** 分页获取退款申请列表 */
  async getPage(params: { status?: RefundRequestStatus | null; page: number; size: number }): Promise<PageResponse<RefundRequestInfo>> {
    return await get<PageResponse<RefundRequestInfo>>('/v1/refunds', mapQueryParams(params as unknown as Record<string, unknown>))
  },

  /** 审批退款申请（同意/拒绝） */
  async approve(refundId: string, approved: boolean, comment: string): Promise<void> {
    await put(`/v1/refunds/${refundId}/approve`, { approved, comment })
  },

  /** 执行退款 */
  async execute(refundId: string): Promise<void> {
    await post(`/v1/refunds/${refundId}/execute`)
  },
}

// ============================================================
// API - 储值系统设置
// ============================================================

export const rechargeSettingsApi = {
  /** 获取系统设置 */
  async get(): Promise<RechargeSystemSettings | null> {
    return await get<RechargeSystemSettings | null>('/v1/recharge-settings')
  },

  /** 更新系统设置 */
  async update(data: RechargeSystemSettings): Promise<void> {
    await put('/v1/recharge-settings', data)
  },
}

// ============================================================
// API - 储值统计
// ============================================================

export const rechargeStatsApi = {
  /** 获取储值统计概览 */
  async getOverview(): Promise<RechargeStatsOverview | null> {
    return await get<RechargeStatsOverview | null>('/v1/recharge-stats/overview')
  },
}

// ============================================================
// API - 财务流水
// ============================================================

export const rechargeFinanceApi = {
  /** 分页获取财务流水 */
  async getPage(params: { financeType?: FinanceLogType | null; startTime?: string; endTime?: string; page: number; size: number }): Promise<PageResponse<RechargeFinanceLog>> {
    return await get<PageResponse<RechargeFinanceLog>>('/v1/recharge-finance', mapQueryParams(params as unknown as Record<string, unknown>))
  },

  /** 导出财务流水 */
  async export(params: { startTime?: string; endTime?: string }): Promise<void> {
    await post('/v1/recharge-finance/export', params)
  },
}

// ============================================================
// API - 异常交易告警
// ============================================================

export const anomalyAlertApi = {
  /** 分页获取异常告警列表 */
  async getPage(params: { status?: string | null; page: number; size: number }): Promise<PageResponse<AnomalyAlert>> {
    return await get<PageResponse<AnomalyAlert>>('/v1/anomaly-alerts', mapQueryParams(params as unknown as Record<string, unknown>))
  },

  /** 处理告警 */
  async handle(alertId: string, remark: string): Promise<void> {
    await put(`/v1/anomaly-alerts/${alertId}/handle`, { remark })
  },

  /** 忽略告警 */
  async ignore(alertId: string): Promise<void> {
    await put(`/v1/anomaly-alerts/${alertId}/ignore`)
  },
}

// ============================================================
// 向后兼容：统一 rechargeApi（已弃用，建议使用上述分职责API）
// ============================================================

export const rechargeApi = {
  getPlanList: rechargePlanApi.getList,
  createPlan: rechargePlanApi.create,
  updatePlan: rechargePlanApi.update,
  togglePlanStatus: rechargePlanApi.toggleStatus,
  getRecordList: (params?: RechargeRecordQueryForm & { page?: number; size?: number }) =>
    rechargeRecordApi.getPage({ ...params, page: params?.page ?? 1, size: params?.size ?? 20 }),
  getStatsOverview: rechargeStatsApi.getOverview,
  refund: (recordId: string, reason: string) =>
    refundApi.create({ rechargeRecordId: recordId, requestedAmount: '0', refundReason: reason }),
}
