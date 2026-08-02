/**
 * 会员管理API
 * 对应后端: /v1/members
 */
import { get, post, put } from '../request'
import type { MemberInfo, MemberQueryForm, MemberCreateForm, MemberUpdateForm, MemberStatsOverview, MemberConsumeRecord, MemberRechargeRecord, MemberStatus } from '@/types/member'
import { memberConverter } from './converters'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 * 同时转换 status 字段（前端语义字符串 → 后端数字编码）
 * gender 保持字符串（后端接受字符串）
 */
function mapQueryParams(params?: MemberQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = memberConverter.toBackendStatus(status as MemberStatus)
  }
  return result
}

/** 后端 RechargeRecordVO 字段（金额已为元字符串） */
interface BackendRechargeRecordVO {
  recordId: string
  recordNo: string
  memberId: string
  memberName?: string
  memberPhone?: string
  planName?: string
  rechargeAmount?: string
  bonusAmount?: string
  paymentMethod?: string
  paymentStatus?: string
  createdAt?: string
}

/** 后端充值记录 VO → 前端 MemberRechargeRecord */
function mapRechargeVOToMemberRecord(vo: BackendRechargeRecordVO): MemberRechargeRecord {
  // 后端 paymentStatus: pending/success/failed/refunded/partial_refunded
  // 前端 RechargePaymentStatus: pending/paid/refunded/failed
  const statusMap: Record<string, string> = {
    pending: 'pending',
    success: 'paid',
    failed: 'failed',
    refunded: 'refunded',
    partial_refunded: 'refunded',
  }
  return {
    recordId: vo.recordId,
    recordNo: vo.recordNo,
    memberId: vo.memberId,
    memberPhone: vo.memberPhone || '',
    planName: vo.planName || '',
    rechargeAmount: vo.rechargeAmount || '0.00',
    bonusAmount: vo.bonusAmount || '0.00',
    paymentMethod: vo.paymentMethod || '',
    paymentStatus: (vo.paymentStatus ? statusMap[vo.paymentStatus] : 'pending') as MemberRechargeRecord['paymentStatus'],
    operateUserName: '',
    createTime: vo.createdAt || '',
    remark: '',
  }
}

export const memberApi = {
  /** 分页查询会员列表 */
  async getList(params?: MemberQueryForm): Promise<{ records: MemberInfo[]; total: number }> {
    return await get<{ records: MemberInfo[]; total: number }>('/v1/members', mapQueryParams(params))
  },

  /** 获取会员详情 */
  async getById(id: string): Promise<MemberInfo | null> {
    return await get<MemberInfo | null>(`/v1/members/${id}`)
  },

  /** 创建会员 */
  async create(data: MemberCreateForm): Promise<MemberInfo | null> {
    return await post<MemberInfo | null>('/v1/members', data)
  },

  /** 更新会员信息 */
  async update(id: string, data: MemberUpdateForm): Promise<MemberInfo | null> {
    return await put<MemberInfo | null>(`/v1/members/${id}`, data)
  },

  /** 冻结会员 */
  async freeze(id: string): Promise<void> {
    await put(`/v1/members/${id}/freeze`)
  },

  /** 解冻会员 */
  async unfreeze(id: string): Promise<void> {
    await put(`/v1/members/${id}/unfreeze`)
  },

  /** 拉黑会员 */
  async blacklist(id: string): Promise<void> {
    await put(`/v1/members/${id}/blacklist`)
  },

  /** 获取会员统计概览 */
  async getStatsOverview(): Promise<MemberStatsOverview | null> {
    return await get<MemberStatsOverview | null>('/v1/members/stats/overview')
  },

  /** 获取会员消费记录 */
  async getConsumeRecords(memberId: string): Promise<MemberConsumeRecord[]> {
    // 已知风险：后端会员消费记录模块尚未实现（无对应 Controller/Service/Entity）
    // 返回空数组避免 UI 报错，待后端消费记录模块开发完成后对接
    console.warn('[memberApi] 会员消费记录后端端点未实现，返回空数组: memberId=', memberId)
    return []
  },

  /** 获取会员储值记录 */
  async getRechargeRecords(memberId: string): Promise<MemberRechargeRecord[]> {
    // 后端无 /v1/members/{id}/recharge-records 端点，改用 /v1/recharge-records?memberId=xxx
    const res = await get<{ records: BackendRechargeRecordVO[]; total: number }>(
      '/v1/recharge-records',
      { memberId, page: 1, size: 1000 },
    )
    return (res?.records || []).map(mapRechargeVOToMemberRecord)
  },

  /** 更新用户偏好 */
  async updatePreferences(id: string, preferences: Record<string, unknown>): Promise<void> {
    await put(`/v1/members/${id}/preferences`, preferences)
  },
}
