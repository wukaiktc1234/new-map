/**
 * 站内通知 API（顶栏通知面板）
 * 对应后端: NotificationController (/v1/notification)
 *
 * 端点：
 * - GET  /v1/notification/notifications                    当前用户通知列表（分页）
 * - GET  /v1/notification/notifications/unread-count      未读数量
 * - PUT  /v1/notification/notifications/{id}/read         标记单条已读
 * - PUT  /v1/notification/notifications/read-all          全部标记已读
 *
 * 说明：P1-MOCK-010 接入。后端实体 Notification 字段：
 * id/title/content/type/businessType/isRead(0/1)/priority/createTime 等。
 */
import { get, put } from './request'

/** 后端通知实体（JSON 形态） */
export interface NotificationBackend {
  id?: number
  notificationId?: number
  userId?: number
  title?: string
  content?: string
  type?: string
  businessId?: number
  businessType?: string
  /** 0 未读 / 1 已读 */
  isRead?: number
  priority?: string
  senderId?: number
  senderName?: string
  createTime?: string
  readTime?: string
  [key: string]: unknown
}

/** 前端通知展示模型 */
export interface SiteNotification {
  id: string
  title: string
  content: string
  /** 相对展示时间（由后端 createTime 格式化） */
  timeText: string
  read: boolean
  createTime: string
}

/** 分页响应 */
interface NotificationPageBackend {
  records?: NotificationBackend[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
  [key: string]: unknown
}

/** 后端时间字符串（ISO 或 yyyy-MM-dd HH:mm:ss）转前端展示时间 */
function formatNotificationTime(iso?: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toFrontend(raw: NotificationBackend): SiteNotification {
  const id = raw.notificationId != null ? String(raw.notificationId) : (raw.id != null ? String(raw.id) : '')
  return {
    id,
    title: raw.title ?? '',
    content: raw.content ?? '',
    timeText: formatNotificationTime(raw.createTime),
    read: raw.isRead === 1,
    createTime: raw.createTime ?? '',
  }
}

export const siteNotificationApi = {
  /** 获取当前用户通知列表（最多 20 条） */
  async getMyNotifications(): Promise<SiteNotification[]> {
    const res = await get<NotificationPageBackend | NotificationBackend[] | null>(
      '/v1/notification/notifications',
      { page: 1, size: 20 },
    )
    if (Array.isArray(res)) {
      return (res || []).map(toFrontend)
    }
    return (res?.records || []).map(toFrontend)
  },

  /** 获取未读通知数量 */
  async getUnreadCount(): Promise<number> {
    const res = await get<{ count?: number } | null>('/v1/notification/notifications/unread-count')
    return res?.count ?? 0
  },

  /** 标记单条通知已读 */
  async markAsRead(id: string): Promise<void> {
    await put<void>(`/v1/notification/notifications/${encodeURIComponent(id)}/read`)
  },

  /** 全部标记已读 */
  async markAllAsRead(): Promise<void> {
    await put<void>('/v1/notification/notifications/read-all')
  },
}

export default siteNotificationApi
