/**
 * 供应商签署门户类型定义
 * 对应后端: /v1/supplier-portal
 */

/** 签署链接状态 */
export type SignLinkStatus = 'pending' | 'sent' | 'viewed' | 'signed' | 'rejected' | 'expired'

/** 实名认证状态 */
export type VerifyStatus = 'unverified' | 'verified' | 'failed'

/** 签署链接信息 */
export interface SignLinkInfo {
  /** 链接ID（主键） */
  linkId: string
  /** 电子合同ID */
  eContractId: string
  /** 电子合同编号 */
  eContractNo: string
  /** 合同名称 */
  contractName: string
  /** 供应商ID */
  supplierId: string
  /** 供应商名称 */
  supplierName: string
  /** 供应商联系人手机号 */
  contactPhone: string
  /** 供应商联系人邮箱 */
  contactEmail?: string
  /** 链接状态 */
  status: SignLinkStatus
  /** 链接token（用于H5访问鉴权） */
  token: string
  /** 链接过期时间 */
  expireTime: string
  /** 发送时间 */
  sendTime?: string
  /** 查看时间 */
  viewTime?: string
  /** 签署时间 */
  signTime?: string
  /** 拒绝时间 */
  rejectTime?: string
  /** 拒绝原因 */
  rejectReason?: string
  /** 实名认证信息 */
  verification?: SupplierVerification
  /** 创建时间 */
  createTime: string
  /** 创建人 */
  createBy: string
}

/** 供应商实名认证信息 */
export interface SupplierVerification {
  /** 认证状态 */
  verifyStatus: VerifyStatus
  /** 真实姓名 */
  realName: string
  /** 身份证号（脱敏） */
  idCardMasked: string
  /** 手机号 */
  phone: string
  /** 认证时间 */
  verifyTime: string
  /** 认证方式：sms/face */
  verifyMethod: 'sms' | 'face'
}

/** 生成签署链接表单 */
export interface CreateSignLinkForm {
  eContractId: string
  contactPhone: string
  contactEmail?: string
  expireDays: number
  remark?: string
}

/** 签署链接查询表单 */
export interface SignLinkQueryForm {
  keyword?: string
  status?: SignLinkStatus | null
  page?: number
  size?: number
}

/** 签署链接状态选项 */
export const SignLinkStatusOptions = [
  { label: '待发送', value: 'pending' as const },
  { label: '已发送', value: 'sent' as const },
  { label: '已查看', value: 'viewed' as const },
  { label: '已签署', value: 'signed' as const },
  { label: '已拒绝', value: 'rejected' as const },
  { label: '已过期', value: 'expired' as const },
]

/** 签署链接状态标签映射 */
export const SignLinkStatusLabelMap: Record<SignLinkStatus, string> = {
  pending: '待发送',
  sent: '已发送',
  viewed: '已查看',
  signed: '已签署',
  rejected: '已拒绝',
  expired: '已过期',
}
