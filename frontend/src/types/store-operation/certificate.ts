import type { PageResponse, ExportParams } from './common'

/** 证件类型 */
export type CertType =
  | 'business_license'
  | 'catering_license'
  | 'hygiene_license'
  | 'safety_license'
  | 'health_certificate'
  | 'pollution_license'
  | 'other'

/** 证件状态 */
export type CertStatus = 'active' | 'expiring' | 'expired' | 'revoked'

/** 预警级别（前端显示用） */
export type AlertLevel = 'normal' | 'warning' | 'danger'

/** 证件信息 */
export interface Certificate {
  certificateId: string
  certName: string
  certType: CertType
  certNumber?: string
  holderType: 'company' | 'employee'
  holderId: string
  holderName: string
  issueDate: string
  expiryDate: string
  daysLeft: number
  status: CertStatus
  fileUrl?: string
  alertLevel: AlertLevel
  issuer?: string
  createTime: string
  updateTime: string
}

/** 后端证件数据结构（类型为数字编码，状态为数字编码） */
export interface CertificateBackend {
  certificateId: string
  certName: string
  certType: number
  certNumber?: string
  holderType: number
  holderId: string
  holderName: string
  issueDate: string
  expiryDate: string
  status: number
  fileUrl?: string
  issuer?: string
  createTime: string
  updateTime: string
}

/** 新增证件DTO */
export interface CreateCertificateDTO {
  certName: string
  certType: CertType
  certNumber?: string
  holderType: 'company' | 'employee'
  holderId: string
  holderName: string
  issueDate: string
  expiryDate: string
  fileUrl?: string
  issuer?: string
  remark?: string
}

/** 更新证书DTO */
export interface UpdateCertificateDTO extends Partial<CreateCertificateDTO> {
  /** 停用标记（'inactive' 表示停用操作，非 CertStatus 业务状态） */
  status?: 'inactive'
}

/** 证件查询参数 */
export interface CertificateQueryParams {
  page: number
  size: number
  certType?: CertType
  status?: CertStatus
  holderKeyword?: string
  expiryRange?: [string, string]
}

/** 证件导出参数 */
export interface CertificateExportParams extends ExportParams {
  certType?: CertType
  status?: CertStatus
  includeExpiringOnly?: boolean
}

/** 续期记录 */
export interface CertificateRenewalRecord {
  recordId: string
  certificateId: string
  certName: string
  oldExpiryDate: string
  newExpiryDate: string
  oldCertNumber?: string
  newCertNumber?: string
  renewCost?: number
  remark?: string
  newFileUrl?: string
  renewTime: string
  operatorName: string
}

/** 续期表单数据（前端提交用） */
export interface CertificateRenewalDTO {
  certificateId: string
  newExpiryDate: string
  newCertNumber?: string
  renewCost?: number
  remark?: string
  newFile?: File
}

/** 续期费用报销状态（完整审批链：待报销→已提交→店长已批→财务已审→已核销） */
export type ExpenseStatus = 'unreimbursed' | 'submitted' | 'manager_approved' | 'finance_approved' | 'reimbursed'

/** 续期费用记录（P0阶段 - 证照续期后续联动） */
export interface CertificateExpenseRecord {
  /** 费用记录ID */
  expenseId: string
  /** 关联的证件ID */
  certificateId: string
  /** 关联的证件名称 */
  certName: string
  /** 续期记录ID（关联到具体哪次续期） */
  renewalRecordId: string
  /** 费用金额（元） */
  amount: number
  /** 报销状态 */
  expenseStatus: ExpenseStatus
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 创建费用记录DTO */
export interface CreateExpenseDTO {
  certificateId: string
  certName: string
  renewalRecordId: string
  amount: number
  remark?: string
}

/** 更新费用状态DTO */
export interface UpdateExpenseStatusDTO {
  expenseId: string
  expenseStatus: ExpenseStatus
}

/** 费用审批操作日志 */
export interface ExpenseAuditLog {
  /** 日志ID */
  logId: string
  /** 关联的费用记录ID */
  expenseId: string
  /** 操作类型 */
  action: 'submit' | 'approve' | 'reject' | 'finance_approve' | 'write_off'
  /** 操作人角色 */
  operatorRole: 'staff' | 'store_manager' | 'finance'
  /** 操作人姓名 */
  operatorName: string
  /** 审批意见 */
  remark: string
  /** 操作时间 */
  operateTime: string
  /** 操作前状态 */
  fromStatus: ExpenseStatus
  /** 操作后状态 */
  toStatus: ExpenseStatus
}
