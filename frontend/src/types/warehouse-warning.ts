/**
 * 库存预警类型定义
 * 对齐后端: InventoryWarningRule + InventoryWarningRecord实体
 */

/** 预警类型（前端语义化） */
export type WarningType = 'low_stock' | 'high_stock' | 'expiring_soon' | 'expired'

/** 通知方式（前端语义化） */
export type NotifyMethod = 'email' | 'message' | 'sms' | 'all'

/** 预警规则信息（对应后端 InventoryWarningRule） */
export interface InventoryWarningRuleInfo {
  ruleId: string
  ruleName: string
  warningType: WarningType
  warningTypeName: string
  materialId: string
  materialName: string
  warehouseId: string
  warehouseName: string
  threshold: number
  unit: string
  enabled: number
  notifyMethod: NotifyMethod
  notifyMethodName: string
  notifyUsers: string
  remark: string
  createTime: string
  updateTime: string
}

/** 预警记录信息（对应后端 InventoryWarningRecord / InventoryWarning） */
export interface InventoryWarningRecordInfo {
  warningId: string
  warningType: WarningType
  warningTypeName: string
  materialId: string
  materialName: string
  warehouseId: string
  warehouseName: string
  currentStock: number
  threshold: number
  unit: string
  status: number
  statusName: string
  handler: string
  handlerName: string
  handleTime: string
  handleRemark: string
  createTime: string
  updateTime: string
}

/** 预警规则查询参数 */
export interface WarningRuleQueryForm {
  ruleName?: string
  warningType?: WarningType
  enabled?: number
  page?: number
  size?: number
}

/** 预警规则创建表单 */
export interface WarningRuleCreateForm {
  ruleName: string
  warningType: WarningType
  materialId?: string
  warehouseId?: string
  threshold: number
  notifyMethod: NotifyMethod
  notifyUsers?: string
  remark?: string
}

/** 预警记录查询参数 */
export interface WarningRecordQueryForm {
  warningType?: WarningType
  status?: number
  warehouseId?: string
  materialName?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

/** 预警处理表单 */
export interface WarningHandleForm {
  handleRemark: string
}

/** AI模型类型 */
export type AIModelType = 'rule-engine' | 'ai-model-a' | 'ai-model-b'

/** AI模型配置 */
export interface AIModelConfig {
  modelId: string
  modelName: string
  modelType: 'local' | 'api'
  endpoint: string
  apiKey: string
  status: 'active' | 'inactive'
  description: string
  lastSyncTime: string
}

/** 模型建议结果 */
export interface ModelSuggestionResult {
  modelId: string
  modelName: string
  suggestedQuantity: number
  confidence: 'high' | 'medium' | 'low'
  reasoning: string
}

/** 采购建议项 */
export interface PurchaseSuggestionItem {
  materialId: string
  materialName: string
  specification: string
  unit: string
  currentStock: number
  safetyStock: number
  suggestedQuantity: number
  estimatedCost: string
  supplierName: string
  /** 各模型建议结果 */
  modelResults?: ModelSuggestionResult[]
}
