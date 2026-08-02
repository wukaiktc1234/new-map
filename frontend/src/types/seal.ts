/**
 * 电子签章管理类型定义
 * 对应后端: /v1/seals
 */

/** 印章类型 */
export type SealType = 'official' | 'finance' | 'contract' | 'legal' | 'custom'

/** 印章状态 */
export type SealStatus = 'active' | 'inactive' | 'revoked'

/** 印章使用场景 */
export type SealScene = 'hr_contract' | 'purchase_contract' | 'electronic_contract'

/** 印章信息 */
export interface SealInfo {
  /** 印章ID */
  sealId: string
  /** 印章名称 */
  sealName: string
  /** 印章类型 */
  sealType: SealType
  /** 印章图片（Base64） */
  sealImage: string
  /** 状态 */
  status: SealStatus
  /** 保管人 */
  keeper: string
  /** 授权使用人ID列表 */
  authorizedUsers: string[]
  /** 授权使用场景 */
  authorizedScenes: SealScene[]
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
  /** 创建人 */
  createBy: string
  /** 备注 */
  remark?: string
}

/** 印章使用记录 */
export interface SealUsageLog {
  /** 记录ID */
  logId: string
  /** 印章ID */
  sealId: string
  /** 印章名称（冗余，便于展示） */
  sealName: string
  /** 业务类型 */
  businessType: SealScene
  /** 业务ID（合同ID） */
  businessId: string
  /** 业务编号（合同编号，便于展示） */
  businessNo: string
  /** 操作人 */
  operator: string
  /** 操作时间 */
  operateTime: string
  /** IP地址 */
  ipAddress: string
  /** 备注 */
  remark?: string
}

/** 印章表单数据（新增/编辑） */
export interface SealFormData {
  sealName: string
  sealType: SealType
  sealImage: string
  keeper: string
  authorizedUsers: string[]
  authorizedScenes: SealScene[]
  remark?: string
}

/** 印章查询表单 */
export interface SealQueryForm {
  keyword?: string
  sealType?: SealType | null
  status?: SealStatus | null
  page?: number
  size?: number
}

/** 印章使用记录查询表单 */
export interface SealUsageLogQueryForm {
  sealId?: string
  businessType?: SealScene | null
  page?: number
  size?: number
}

/** 印章类型选项 */
export const SealTypeOptions = [
  { label: '公章', value: 'official' as const },
  { label: '财务专用章', value: 'finance' as const },
  { label: '合同专用章', value: 'contract' as const },
  { label: '法人章', value: 'legal' as const },
  { label: '自定义', value: 'custom' as const },
]

/** 印章状态选项 */
export const SealStatusOptions = [
  { label: '启用', value: 'active' as const },
  { label: '停用', value: 'inactive' as const },
  { label: '作废', value: 'revoked' as const },
]

/** 印章使用场景选项 */
export const SealSceneOptions = [
  { label: '人事合同', value: 'hr_contract' as const },
  { label: '采购合同', value: 'purchase_contract' as const },
  { label: '电子合同', value: 'electronic_contract' as const },
]

/** 印章类型标签映射 */
export const SealTypeLabelMap: Record<SealType, string> = {
  official: '公章',
  finance: '财务专用章',
  contract: '合同专用章',
  legal: '法人章',
  custom: '自定义',
}

/** 印章状态标签映射 */
export const SealStatusLabelMap: Record<SealStatus, string> = {
  active: '启用',
  inactive: '停用',
  revoked: '作废',
}

/** 印章使用场景标签映射 */
export const SealSceneLabelMap: Record<SealScene, string> = {
  hr_contract: '人事合同',
  purchase_contract: '采购合同',
  electronic_contract: '电子合同',
}
