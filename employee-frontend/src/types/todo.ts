/**
 * 待办事项类型定义
 * 用于消息中心待办模块的数据结构
 */

/** 待办操作类型 */
export type TodoActionType = 'confirm' | 'approve' | 'complete'

/** 待办来源模块 */
export type TodoSourceModule = 'approval' | 'training' | 'onboarding' | 'contract'

/** 待办优先级 */
export type TodoPriority = 'high' | 'medium' | 'low'

/** 可操作的待办项(分区1) */
export interface ActionableTodo {
  id: string
  /** 来源模块标识 */
  sourceModule: string
  title: string
  description?: string
  /** 紧急程度 */
  urgency?: TodoPriority
  /** 创建时间（ISO格式） */
  createdAt?: string
  createTime?: string
  /** 截止时间（ISO格式） */
  deadline?: string
  /** 关联业务ID */
  businessId?: string
  /** 兼容旧字段：优先级 */
  priority?: TodoPriority
  /** 兼容旧字段：类型 */
  type?: TodoSourceModule
  /** 兼容旧字段：操作类型 */
  actionType?: TodoActionType
  /** 兼容旧字段：跳转链接 */
  targetUrl?: string
}

/** 申请进展项(分区2) */
export interface ProgressTodo {
  id: string
  /** 来源模块标识 */
  sourceModule?: string
  title: string
  status?: string
  statusText?: string
  /** 进度百分比（0-100） */
  progress?: number
  /** 进度描述文本 */
  progressLabel?: string
  /** 创建时间（ISO格式） */
  createdAt?: string
  /** 兼容旧字段：类型 */
  type?: string
  /** 兼容旧字段：跳转链接 */
  targetUrl?: string
}

/** 通知项(分区3) */
export interface TodoNotification {
  id: string
  title: string
  content: string
  time?: string
  /** 通知类型（info/warning等） */
  type?: string
  /** 创建时间（ISO格式） */
  createTime?: string
  isRead: boolean
  /** 兼容旧字段：跳转链接 */
  targetUrl?: string
}
