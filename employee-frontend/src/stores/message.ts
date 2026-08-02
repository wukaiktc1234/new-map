import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { MessageItem, MessageCategory } from '@/types/message'

/**
 * 消息中心 Pinia Store
 *
 * 管理系统通知、公告、@提及三类消息的获取、分类展示和已读状态。
 * 支持单条已读、全部已读、分类切换等操作。
 */
export const useMessageStore = defineStore('message', () => {
  // ===== State =====
  const systemNotifications = ref<MessageItem[]>([])
  const announcements = ref<MessageItem[]>([])
  const mentions = ref<MessageItem[]>([])
  const activeCategory = ref<MessageCategory>('system')
  const isLoading = ref(false)

  // ===== Computed =====

  /** 所有分类中未读消息的总数 */
  const unreadCount = computed((): number => {
    return (
      systemNotifications.value.filter((m) => !m.isRead).length +
      announcements.value.filter((m) => !m.isRead).length +
      mentions.value.filter((m) => !m.isRead).length
    )
  })

  /** 根据当前激活分类返回对应的消息列表 */
  const currentMessages = computed((): MessageItem[] => {
    switch (activeCategory.value) {
      case 'system':
        return systemNotifications.value
      case 'announcement':
        return announcements.value
      case 'mention':
        return mentions.value
      default:
        return systemNotifications.value
    }
  })

  // ===== Actions =====

  /**
   * 获取所有分类的消息数据
   * 调用消息API并按分类填充到对应的ref中
   */
  async function fetchMessages(): Promise<void> {
    isLoading.value = true
    try {
      // 动态导入API，避免循环依赖
      const { notificationApi } = await import('@/api/notification')

      const [systemRes, announcementRes, mentionRes] = await Promise.allSettled([
        notificationApi.getMessages({ category: 'system' }),
        notificationApi.getMessages({ category: 'announcement' }),
        notificationApi.getMessages({ category: 'mention' }),
      ])

      if (systemRes.status === 'fulfilled') {
        systemNotifications.value = (systemRes.value?.records || []) as unknown as MessageItem[]
      }
      if (announcementRes.status === 'fulfilled') {
        announcements.value = (announcementRes.value?.records || []) as unknown as MessageItem[]
      }
      if (mentionRes.status === 'fulfilled') {
        mentions.value = (mentionRes.value?.records || []) as unknown as MessageItem[]
      }
    } catch (error) {
      // API不可用时使用空数组，不阻断流程
      systemNotifications.value = []
      announcements.value = []
      mentions.value = []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 将指定消息标记为已读
   * 同时更新本地状态和调用后端API
   */
  async function markRead(id: string): Promise<void> {
    // 先更新本地状态（乐观更新）
    updateLocalReadStatus(id, true)

    try {
      const { notificationApi } = await import('@/api/notification')
      await notificationApi.markRead(id)
    } catch (error) {
      // API失败时回滚本地状态
      updateLocalReadStatus(id, false)
    }
  }

  /**
   * 将所有消息标记为已读
   */
  async function markAllRead(): Promise<void> {
    // 乐观更新：全部标记为已读
    const prevSystem = [...systemNotifications.value]
    const prevAnnouncement = [...announcements.value]
    const prevMention = [...mentions.value]

    markAllLocalRead()

    try {
      const { notificationApi } = await import('@/api/notification')
      await notificationApi.markAllRead()
    } catch (error) {
      // 回滚
      systemNotifications.value = prevSystem
      announcements.value = prevAnnouncement
      mentions.value = prevMention
    }
  }

  /**
   * 切换当前活跃的消息分类
   */
  function switchCategory(cat: MessageCategory): void {
    activeCategory.value = cat
  }

  // ===== 内部辅助方法 =====

  /** 更新本地某条消息的已读状态 */
  function updateLocalReadStatus(id: string, isRead: boolean): void {
    const allLists = [
      systemNotifications,
      announcements,
      mentions,
    ] as const

    for (const list of allLists) {
      const item = list.value.find((m) => m.id === id)
      if (item) {
        item.isRead = isRead
        break
      }
    }
  }

  /** 将所有列表中的消息标记为已读 */
  function markAllLocalRead(): void {
    systemNotifications.value.forEach((m) => {
      m.isRead = true
    })
    announcements.value.forEach((m) => {
      m.isRead = true
    })
    mentions.value.forEach((m) => {
      m.isRead = true
    })
  }

  return {
    // State
    systemNotifications,
    announcements,
    mentions,
    activeCategory,
    isLoading,
    // Computed
    unreadCount,
    currentMessages,
    // Actions
    fetchMessages,
    markRead,
    markAllRead,
    switchCategory,
  }
})
