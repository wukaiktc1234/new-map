/**
 * AI 模型配置 - TypeScript 类型定义
 * 对应后端: /v1/ai-models (AIModelConfigController)
 */

/** 模型类型：local-本地模型 / api-API服务 */
export type AIModelType = 'local' | 'api'

/** 模型状态：active-启用 / inactive-停用 */
export type AIModelStatus = 'active' | 'inactive'

/** 置信度：high-高 / medium-中 / low-低 */
export type AIModelConfidence = 'high' | 'medium' | 'low'

/** AI 模型视图对象（后端返回，apiKey 已脱敏） */
export interface AIModelVO {
  id: number
  modelCode: string
  modelName: string
  modelType: AIModelType
  /** 服务商标识（可选，后端当前未持久化时回退显示） */
  provider?: string
  endpoint: string
  /** 已脱敏的 apiKey（如 sk-abc****） */
  apiKey: string | null
  /** 模型版本（可选） */
  modelVersion?: string
  /** 默认模型名称（可选） */
  defaultModel?: string
  /** 温度参数（可选） */
  temperature?: number
  /** 最大 token 数（可选） */
  maxTokens?: number
  /** 上下文长度（可选） */
  contextLength?: number
  /** 重试次数（可选） */
  maxRetries?: number
  /** 今日调用次数（可选统计字段） */
  todayCalls?: number
  status: AIModelStatus
  confidence: AIModelConfidence
  description: string
  lastSyncTime: string | null
  modelPath: string | null
  modelParams: string | null
  timeout: number
  createdBy: string | null
  updatedBy: string | null
  createdAt: string
  updatedAt: string
}

/** 创建模型请求 */
export interface AIModelCreateDTO {
  modelName: string
  modelType: AIModelType
  endpoint?: string
  apiKey?: string
  confidence?: AIModelConfidence
  description?: string
  modelPath?: string
  modelParams?: string
  timeout?: number
  provider?: string
  modelVersion?: string
  defaultModel?: string
  temperature?: number
  maxTokens?: number
  contextLength?: number
  maxRetries?: number
}

/** 更新模型请求（部分更新） */
export interface AIModelUpdateDTO {
  modelName?: string
  modelType?: AIModelType
  endpoint?: string
  apiKey?: string
  status?: AIModelStatus
  confidence?: AIModelConfidence
  description?: string
  modelPath?: string
  modelParams?: string
  timeout?: number
  provider?: string
  modelVersion?: string
  defaultModel?: string
  temperature?: number
  maxTokens?: number
  contextLength?: number
  maxRetries?: number
}

/** 查询参数 */
export interface AIModelQueryParams {
  page?: number
  size?: number
  modelType?: AIModelType | ''
  provider?: string
  status?: AIModelStatus | ''
  keyword?: string
}

/** 分页响应 */
export interface AIModelPageResponse {
  records: AIModelVO[]
  total: number
  current: number
  size: number
}

/** 统计信息 */
export interface AIModelStatistics {
  total: number
  active: number
  local: number
  api: number
}

/** 测试连接结果（对应后端 testConnection 返回的 Map） */
export interface AIModelTestResult {
  /** 是否连接成功 */
  success: boolean
  /** 结果消息（成功/失败原因） */
  message: string
  /** 响应耗时（毫秒） */
  responseTimeMs?: number
  /** HTTP 状态码（仅 api 类型返回） */
  httpStatus?: number
  /** 模型类型（local/api） */
  modelType?: AIModelType
  /** 模型名称 */
  modelName?: string
}

/** 模型类型选项 */
export const AI_MODEL_TYPE_OPTIONS: Array<{ value: AIModelType; label: string }> = [
  { value: 'local', label: '本地模型' },
  { value: 'api', label: 'API服务' },
]

/** 模型状态选项 */
export const AI_MODEL_STATUS_OPTIONS: Array<{ value: AIModelStatus; label: string }> = [
  { value: 'active', label: '启用' },
  { value: 'inactive', label: '停用' },
]

/** 置信度选项 */
export const AI_MODEL_CONFIDENCE_OPTIONS: Array<{ value: AIModelConfidence; label: string; percentage: string }> = [
  { value: 'high', label: '高', percentage: '90%' },
  { value: 'medium', label: '中', percentage: '60%' },
  { value: 'low', label: '低', percentage: '30%' },
]

/** 模型类型标签 */
export function aiModelTypeLabel(type: AIModelType): string {
  return type === 'local' ? '本地模型' : 'API服务'
}

/** 置信度标签 */
export function aiModelConfidenceLabel(confidence: AIModelConfidence): string {
  const item = AI_MODEL_CONFIDENCE_OPTIONS.find(o => o.value === confidence)
  return item ? item.label : confidence
}

/** 置信度百分比 */
export function aiModelConfidencePercentage(confidence: AIModelConfidence): string {
  const item = AI_MODEL_CONFIDENCE_OPTIONS.find(o => o.value === confidence)
  return item ? item.percentage : ''
}
