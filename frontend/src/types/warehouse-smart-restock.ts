/**
 * 智能补货建议相关类型定义
 * 用于 SmartRestock.vue 页面
 */

/** 紧急程度 */
export type UrgencyLevel = 'urgent' | 'high' | 'medium' | 'low'

/** 模型置信度 */
export type ConfidenceLevel = 'high' | 'medium' | 'low'

/** 建议状态 */
export type SuggestionStatus = 'pending' | 'accepted' | 'modified' | 'rejected' | 'converted'

/** 建议记录物料项 */
export interface SuggestionRecordItem {
  materialName: string
  quantity: number
  unitPrice: number
  amount: number
}

/** 建议记录 */
export interface SuggestionRecord {
  suggestionNo: string
  materialCount: number
  totalAmount: number
  suggestedSupplier: string
  status: SuggestionStatus
  statusName: string
  createTime: string
  purchaseRequestNo: string
  remark?: string
  items: SuggestionRecordItem[]
}
