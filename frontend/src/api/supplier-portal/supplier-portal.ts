/**
 * 供应商签署门户 API
 * 对应后端: /v1/supplier-portal
 *
 * 后端端点：
 * - 管理端: GET /page, POST /links, GET /{linkId}, POST /{linkId}/send, POST /{linkId}/revoke
 * - 供应商端 H5: GET /h5/contract, POST /h5/view, POST /h5/verify, POST /h5/sign, POST /h5/reject
 */
import { get, post } from '@/api/request'
import type {
  SignLinkInfo,
  CreateSignLinkForm,
  SignLinkQueryForm,
  SupplierVerification,
} from '@/types/supplier-portal'

/** 后端 SupplierSignLink 实体原始字段 */
interface SignLinkBackend {
  linkId?: string
  eContractId?: string
  eContractNo?: string
  contractName?: string
  supplierId?: string
  supplierName?: string
  contactPhone?: string
  contactEmail?: string
  status?: string
  token?: string
  expireTime?: string
  createTime?: string
  createBy?: string
  sendTime?: string
  viewTime?: string
  signTime?: string
  rejectTime?: string
  rejectReason?: string
  verification?: SupplierVerification
}

/** 后端 → 前端字段适配（字段同名，仅做防御性拷贝） */
function adaptLink(raw: SignLinkBackend | null | undefined): SignLinkInfo | null {
  if (!raw) return null
  return {
    linkId: raw.linkId || '',
    eContractId: raw.eContractId || '',
    eContractNo: raw.eContractNo || '',
    contractName: raw.contractName || '',
    supplierId: raw.supplierId || '',
    supplierName: raw.supplierName || '',
    contactPhone: raw.contactPhone || '',
    contactEmail: raw.contactEmail || '',
    status: (raw.status as SignLinkInfo['status']) || 'pending',
    token: raw.token || '',
    expireTime: raw.expireTime || '',
    createTime: raw.createTime || '',
    createBy: raw.createBy || '',
    sendTime: raw.sendTime,
    viewTime: raw.viewTime,
    signTime: raw.signTime,
    rejectTime: raw.rejectTime,
    rejectReason: raw.rejectReason,
    verification: raw.verification,
  }
}

/** 后端分页响应 */
interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
}

export const supplierPortalApi = {
  /** 查询签署链接列表 */
  async getList(params?: SignLinkQueryForm): Promise<{ records: SignLinkInfo[]; total: number }> {
    const res = await get<PageResult<SignLinkBackend> | SignLinkBackend[]>('/v1/supplier-portal/page', {
      page: params?.page || 1,
      size: params?.size || 10,
      status: params?.status,
      keyword: params?.keyword,
    })
    const records = Array.isArray(res) ? res : (res?.records || [])
    const total = Array.isArray(res) ? res.length : (res?.total || 0)
    return {
      records: records.map(r => adaptLink(r)).filter(Boolean) as SignLinkInfo[],
      total,
    }
  },

  /** 生成签署链接 */
  async createLink(data: CreateSignLinkForm): Promise<SignLinkInfo | null> {
    const res = await post<SignLinkBackend>('/v1/supplier-portal/links', {
      eContractId: data.eContractId,
      contactPhone: data.contactPhone,
      contactEmail: data.contactEmail,
      expireDays: data.expireDays,
    })
    return adaptLink(res)
  },

  /** 发送签署链接（短信/邮件） */
  async sendLink(linkId: string, channel: 'sms' | 'email' | 'both'): Promise<SignLinkInfo | null> {
    const res = await post<SignLinkBackend>(`/v1/supplier-portal/${linkId}/send`, { channel })
    return adaptLink(res)
  },

  /** 作废签署链接 */
  async revokeLink(linkId: string): Promise<void> {
    await post<string>(`/v1/supplier-portal/${linkId}/revoke`)
  },

  /** 获取签署链接详情 */
  async getById(linkId: string): Promise<SignLinkInfo | null> {
    const res = await get<SignLinkBackend>(`/v1/supplier-portal/${linkId}`)
    return adaptLink(res)
  },

  /* ===== 供应商端 H5 API（通过 token 鉴权） ===== */

  /** 供应商通过 token 获取合同信息 */
  async getContractByToken(token: string): Promise<SignLinkInfo | null> {
    const res = await get<SignLinkBackend>('/v1/supplier-portal/h5/contract', { token })
    return adaptLink(res)
  },

  /** 供应商查看合同（更新状态为 viewed） */
  async viewContract(token: string): Promise<SignLinkInfo | null> {
    const res = await post<SignLinkBackend>('/v1/supplier-portal/h5/view', { token })
    return adaptLink(res)
  },

  /**
   * 发送实名认证验证码
   * 后端会生成随机验证码并存入缓存（10 分钟过期），暂未对接真实短信网关，
   * 验证码通过后端日志输出便于联调。
   *
   * @param token 签署链接 token
   * @param phone 手机号
   * @returns 后端返回 { sent, phone(脱敏), expireSeconds }
   */
  async sendVerifyCode(token: string, phone: string): Promise<{ sent: boolean; phone: string; expireSeconds: number }> {
    const res = await post<{ sent: boolean; phone: string; expireSeconds: number }>(
      '/v1/supplier-portal/h5/send-code',
      { token, phone },
    )
    return res
  },

  /** 供应商实名认证 */
  async verify(token: string, realName: string, idCard: string, phone: string, code: string): Promise<SupplierVerification | null> {
    const res = await post<SupplierVerification>('/v1/supplier-portal/h5/verify', {
      token,
      realName,
      idCard,
      phone,
      code,
    })
    return res
  },

  /** 供应商确认签署 */
  async signByToken(token: string, verification: SupplierVerification): Promise<SignLinkInfo | null> {
    const res = await post<SignLinkBackend>('/v1/supplier-portal/h5/sign', {
      token,
      verification,
    })
    return adaptLink(res)
  },

  /** 供应商拒绝签署 */
  async rejectByToken(token: string, reason: string): Promise<SignLinkInfo | null> {
    const res = await post<SignLinkBackend>('/v1/supplier-portal/h5/reject', { token, reason })
    return adaptLink(res)
  },
}

export default supplierPortalApi
