/**
 * 消息类型定义
 * 用于消息中心的消息数据结构
 */

/** 消息分类 */
export type MessageCategory = 'system' | 'announcement' | 'mention'

/** 消息项 */
export interface MessageItem {
  id: string
  category: MessageCategory
  title: string
  content: string
  time: string
  isRead: boolean
  type?: string
  targetId?: string
}
