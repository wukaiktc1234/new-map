/**
 * 待办事项管理 API
 *
 * 数据来源策略：调用真实后端 API，失败时抛出错误由调用方处理
 * 后端路径前缀：/v1/store-management/tasks
 */
import { get, put } from '@/api/request'
import { taskDataConverter } from '@/api/store-ops/converters'
import type {
  PendingTask,
  TaskQueryParams,
  TaskBackend,
  PageResponse,
  OperationResult,
} from '@/types/store-operation'

/** 后端分页响应结构（MyBatis Plus IPage） */
interface BackendPage<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/**
 * 前端查询参数 → 后端查询参数映射
 * 前端 page → 后端 current；前端 assignee → 后端 assigneeId；
 * 前端 priority(字符串) → 后端 priority(数字)
 */
function toBackendQuery(params: TaskQueryParams): Record<string, unknown> {
  const backend: Record<string, unknown> = {
    current: params.page,
    size: params.size,
  }
  if (params.taskType) backend.taskType = params.taskType
  if (params.priority) backend.priority = taskDataConverter.convertToPriorityNum(params.priority)
  if (params.status) backend.status = params.status
  if (params.assignee) backend.assigneeId = params.assignee
  if (params.keyword) backend.keyword = params.keyword
  return backend
}

export const taskApi = {
  /**
   * 分页查询待办任务列表
   */
  async getTaskList(params: TaskQueryParams): Promise<PageResponse<PendingTask>> {
    const res = await get<BackendPage<TaskBackend>>(
      '/v1/store-management/tasks',
      toBackendQuery(params),
    )
    return {
      records: (res?.records || []).map((r) => taskDataConverter.toFrontend(r)),
      total: res?.total ?? 0,
      current: res?.current ?? params.page,
      size: res?.size ?? params.size,
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 获取任务详情
   */
  async getTaskDetail(id: string): Promise<PendingTask | null> {
    const res = await get<TaskBackend>(`/v1/store-management/tasks/${id}`)
    return res ? taskDataConverter.toFrontend(res) : null
  },

  /**
   * 完成任务
   * 失败时返回失败结果
   */
  async completeTask(taskId: string, remark?: string): Promise<OperationResult> {
    try {
      await put<void>(`/v1/store-management/tasks/${taskId}/complete`)
      return { success: true, message: '任务已完成' }
    } catch {
      return { success: false, message: '任务完成失败，请重试' }
    }
  },

  /**
   * 批量完成任务
   * 失败时返回失败结果
   */
  async batchCompleteTasks(taskIds: string[]): Promise<OperationResult> {
    try {
      await put<void>('/v1/store-management/tasks/batch-complete', taskIds)
      return { success: true, message: `已批量完成 ${taskIds.length} 个任务` }
    } catch {
      return { success: false, message: '批量完成任务失败，请重试' }
    }
  },

  /**
   * 获取未读任务数量
   */
  async getUnreadCount(): Promise<number> {
    const res = await get<number>('/v1/store-management/tasks/unread-count')
    return res ?? 0
  },

  /**
   * 获取任务联动跳转 URL
   * 失败时抛出错误（前端有本地跳转配置兜底）
   */
  async getRedirectUrl(id: string): Promise<string> {
    const res = await get<string>(`/v1/store-management/tasks/${id}/redirect-url`)
    return res ?? ''
  },
}
