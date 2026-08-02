export type NoticeType = 'announcement' | 'activity' | 'training' | 'system' | 'warning'

export const NOTICE_TYPE_LABEL_MAP: Record<NoticeType, string> = {
  announcement: '公告',
  activity: '活动',
  training: '培训',
  system: '系统',
  warning: '提醒',
}

export const NOTICE_TYPE_ICON_MAP: Record<NoticeType, string> = {
  announcement: '📢',
  activity: '🎉',
  training: '📚',
  system: '⚙️',
  warning: '⚠️',
}

export interface NoticeItem {
  id: string
  title: string
  summary: string
  type: NoticeType
  content?: string
  isRead: boolean
  isPinned: boolean
  publishedAt: string
  attachments?: string[]
  authorName: string
}
