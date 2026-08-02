import request from './request'
import type { NoticeItem, NoticeType } from '@/types/notice'
import type { PaginationParams } from '@/types/common'

export const noticeApi = {
  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getList(params: PaginationParams & { type?: NoticeType; isRead?: boolean }): Promise<{ records: NoticeItem[]; total: number }> {
    return request.get('/v1/employee/notices', params as unknown as Record<string, unknown>) as any
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getById(id: string): Promise<NoticeItem> {
    return request.get<NoticeItem>(`/v1/employee/notices/${id}`) as any
  },

  markAsRead(id: string): Promise<void> {
    return request.post(`/v1/employee/notices/${id}/read`)
  },

  markAllRead(): Promise<void> {
    return request.post('/v1/employee/notices/read-all')
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getUnreadCount(): Promise<number> {
    return request.get<number>('/v1/employee/notices/unread-count') as any
  },
}
