import type { PageResponse } from './common'

/** 任务类型 */
export type TaskType =
  | 'approval'
  | 'inspection'
  | 'refund'
  | 'settlement'
  | 'certificate'
  | 'audit'
  | 'maintenance'
  | 'other'

/** 任务优先级 */
export type TaskPriority = 'high' | 'medium' | 'low'

/** 任务状态 */
export type TaskStatus = 'pending' | 'completed' | 'expired' | 'cancelled'

/** 待办任务 */
export interface PendingTask {
  taskId: string
  taskType: TaskType
  title: string
  description?: string
  sourceModule: string
  sourceRefId?: string
  assigneeId: string
  assigneeName: string
  deadline: string
  priority: TaskPriority
  status: TaskStatus
  overdueDays?: number
  createTime: string
  updateTime: string
}

/** 后端任务数据结构（数字编码，用于与后端API交互） */
export interface TaskBackend {
  taskId: string
  taskType: number
  title: string
  description?: string
  sourceModule: string
  sourceRefId?: string
  assigneeId: string
  assigneeName: string
  deadline: string
  priority: number
  status: number
  overdueDays?: number
  createTime: string
  updateTime: string
}

/** 任务查询参数 */
export interface TaskQueryParams {
  page: number
  size: number
  taskType?: TaskType
  priority?: TaskPriority
  status?: TaskStatus
  assignee?: string
  keyword?: string
}

/** 标记完成请求 */
export interface CompleteTaskDTO {
  taskId: string
  completionRemark?: string
}

/** 稍后提醒请求 */
export interface SnoozeTaskRequest {
  taskId: string
  snoozeUntil: string
  reason?: string
}
