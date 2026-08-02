/**
 * 通知日志 API
 * 对应后端: /v1/schedule/notifications
 */
import { get, post } from '@/api/request'
import { notificationLogConverter } from './converters'
import type {
  NotificationChannel,
  SendStatus,
} from '@/types/schedule'
import type { IPage } from '@/types/pagination'

/** 后端返回的通知日志数据（原始格式） */
interface NotificationLogBackend {
  logId: string
  businessType: string
  businessId: string
  title: string
  content: string
  receiverIds: string | number[]
  receiverNames: string | string[]
  channel: string | number
  sendStatus: string | number
  sendTime?: string
  retryCount: number
  errorMessage?: string
  createTime: string
}

/** 通知统计信息 */
interface NotificationStats {
  totalSent: number
  successCount: number
  failedCount: number
  pendingCount: number
  byChannel: Record<NotificationChannel, number>
}

/** 分页响应类型 */
type NotificationPageResponse = IPage<NotificationLogBackend>

export const notificationApi = {
  /**
   * 获取通知日志列表（分页）
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(
    params: {
      page?: number
      size?: number
      channel?: NotificationChannel | ''
      sendStatus?: SendStatus | ''
      businessType?: string
    }
  ): Promise<NotificationPageResponse> {
    const res = await get<NotificationPageResponse>(
      '/v1/schedule/notifications',
      params
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: (res.records || []).map((item: NotificationLogBackend) =>
        notificationLogConverter.toFrontend(item)
      ),
    }
  },

  /**
   * 获取通知统计信息
   * @returns 统计数据
   */
  async getStats(): Promise<NotificationStats> {
    return get<NotificationStats>('/v1/schedule/notifications/stats')
  },

  /**
   * 重发失败的通知
   * @param id - 日志ID
   */
  async resend(id: string): Promise<void> {
    await post(`/v1/schedule/notifications/${id}/resend`)
  },
}

export default notificationApi
