/**
 * 公司初始化相关类型定义
 */

/** 公司初始化表单数据 */
export interface CompanyInitFormData {
  companyName: string
  companyCode?: string
  legalPerson?: string
  contactPhone?: string
  address?: string
  initVersion?: string
  remark?: string
}

/** 公司初始化记录 */
export interface CompanyInitRecord {
  recordId: number
  companyName: string
  companyCode: string
  legalPerson: string
  contactPhone: string
  address: string
  status: string
  initVersion: string
  remark: string
  createTime: string
  updateTime: string
}

/** 系统初始化状态 */
export interface SystemInitStatus {
  step: string
  isCompleted: boolean
  completedAt?: string
  createdAt?: string
  updatedAt?: string
}
