/**
 * 合同智能管理类型定义
 */

/** 风险等级 */
export type RiskLevel = 'high' | 'medium' | 'low'

/** 续签推荐等级 */
export type RenewRecommendation = 'strong' | 'normal' | 'cautious' | 'not_recommended'

/** 到期合同提醒 */
export interface ExpiringContract {
  contractId: string
  contractNo: string
  employeeName: string
  department: string
  position: string
  contractType: string
  startDate: string
  endDate: string
  /** 剩余天数（负数表示已过期） */
  remainingDays: number
  /** 提醒级别 */
  alertLevel: 'expired' | 'urgent' | 'warning' | 'notice'
  /** 续签推荐 */
  recommendation: RenewRecommendation
  /** 推荐理由 */
  recommendationReason: string
}

/** 合同风险项 */
export interface ContractRisk {
  riskId: string
  riskType: 'expired_unsigned' | 'salary_anomaly' | 'missing_contract' | 'type_mismatch' | 'long_term_temp'
  riskLevel: RiskLevel
  contractId?: string
  employeeName: string
  department: string
  description: string
  detectedTime: string
  suggestion: string
}

/** 续签推荐统计 */
export interface RenewalStats {
  total: number
  strong: number
  normal: number
  cautious: number
  notRecommended: number
}

/** 到期提醒统计 */
export interface ExpiryStats {
  expired: number
  urgent: number
  warning: number
  notice: number
}

/** 合同模板类型 */
export type ContractTemplateType = 'labor' | 'service' | 'employment' | 'internship' | 'confidentiality' | 'custom'

/** 合同模板信息 */
export interface ContractTemplateInfo {
  templateId: string
  templateName: string
  templateType: ContractTemplateType
  description: string
  content: string
  /** 可自定义条款 */
  customClauses: TemplateClause[]
  status: 'active' | 'inactive'
  version: string
  updateTime: string
  createBy: string
}

/** 模板条款 */
export interface TemplateClause {
  clauseId: string
  clauseTitle: string
  clauseContent: string
  /** 是否必选 */
  required: boolean
  /** 是否可编辑 */
  editable: boolean
  /** 条款分类 */
  category: 'basic' | 'salary' | 'benefit' | 'term' | 'termination' | 'confidentiality' | 'other'
}

/** 合同模板类型选项 */
export const ContractTemplateTypeOptions = [
  { label: '劳动合同', value: 'labor' as const },
  { label: '劳务协议', value: 'service' as const },
  { label: '用工协议', value: 'employment' as const },
  { label: '实习协议', value: 'internship' as const },
  { label: '保密协议', value: 'confidentiality' as const },
  { label: '自定义', value: 'custom' as const },
]

/** 合同模板类型标签映射 */
export const ContractTemplateTypeLabelMap: Record<ContractTemplateType, string> = {
  labor: '劳动合同',
  service: '劳务协议',
  employment: '用工协议',
  internship: '实习协议',
  confidentiality: '保密协议',
  custom: '自定义',
}

/** 续签推荐标签映射 */
export const RenewRecommendationLabelMap: Record<RenewRecommendation, string> = {
  strong: '强烈推荐续签',
  normal: '建议续签',
  cautious: '谨慎续签',
  not_recommended: '不建议续签',
}

/** 风险等级标签映射 */
export const RiskLevelLabelMap: Record<RiskLevel, string> = {
  high: '高风险',
  medium: '中风险',
  low: '低风险',
}

/** 风险类型标签映射 */
export const RiskTypeLabelMap: Record<ContractRisk['riskType'], string> = {
  expired_unsigned: '到期未签',
  salary_anomaly: '薪资异常',
  missing_contract: '缺失合同',
  type_mismatch: '类型不匹配',
  long_term_temp: '长期临时',
}
