/**
 * 消息中心 API
 * 提供消息查询、已读标记等接口
 * 遵循规范：使用 request 实例，参数直接传递
 */

import request from './request'
import type { MessageItem } from '@/api/converters/message'

/** 分页响应通用类型 */
interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export const messageApi = {
  /**
   * 获取消息列表（分页）
   * @param params - 查询参数
   * @returns 消息分页数据
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getMessages(params: {
    category?: string
    page?: number
    size?: number
  }): Promise<PageResponse<MessageItem>> {
    return request.get<PageResponse<MessageItem>>('/v1/messages', params as Record<string, unknown>) as any
  },

  /**
   * 标记单条消息为已读
   * @param id - 消息ID
   */
  markRead(id: string): Promise<void> {
    return request.put(`/v1/messages/${id}/read`)
  },

  /**
   * 标记所有消息为已读
   */
  markAllRead(): Promise<void> {
    return request.put('/v1/messages/read-all')
  },
}
