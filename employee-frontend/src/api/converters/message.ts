/**
 * 消息转换器
 * 负责消息/通知数据在后端与前端之间的转换
 * 遵循规范：禁止在组件中直接做状态映射
 *
 * 转换规则：
 * - category: 根据type字段推断消息分类
 * - isRead: 数字布尔转换
 * - 时间: ISO格式化为相对时间/展示格式
 */

// ============================================================
// 类型定义
// ============================================================

/** 消息分类 */
export type MessageCategory =
  | 'system'
  | 'approval'
  | 'schedule'
  | 'reminder'
  | 'announcement'

/** 消息项 */
export interface MessageItem {
  /** 消息ID */
  id: string
  /** 标题 */
  title: string
  /** 内容摘要 */
  content: string
  /** 消息分类 */
  category: MessageCategory
  /** 是否已读 */
  isRead: boolean
  /** 优先级 */
  priority: 'high' | 'normal' | 'low'
  /** 发送者名称 */
  senderName: string
  /** 关联业务ID */
  businessId: string
  /** 关联业务类型 */
  businessType: string
  /** 创建时间（ISO） */
  createTime: string
  /** 展示用时间（已格式化） */
  displayTime: string
  /** 关联路由 */
  route: string
}

// ============================================================
// 分类映射
// ============================================================

/** 业务类型 -> 消息分类映射 */
const BUSINESS_TYPE_CATEGORY_MAP: Record<string, MessageCategory> = {
  APPROVAL: 'approval',
  SCHEDULE_CHANGE: 'schedule',
  SCHEDULE_SWAP: 'schedule',
  HEALTH_CERT_EXPIRE: 'reminder',
  CONTRACT_EXPIRE: 'reminder',
  SALARY_RELEASE: 'announcement',
  SYSTEM: 'system',
}

/** 默认优先级映射（按分类） */
const CATEGORY_PRIORITY_MAP: Record<MessageCategory, 'high' | 'normal' | 'low'> = {
  system: 'normal',
  approval: 'high',
  schedule: 'normal',
  reminder: 'high',
  announcement: 'low',
}

/**
 * 格式化时间为展示用的相对时间/简短格式
 * @param isoString - ISO时间字符串
 * @returns 格式化后的展示时间
 */
function formatDisplayTime(isoString: string): string {
  if (!isoString) return ''

  const now = new Date()
  const target = new Date(isoString)
  const diffMs = now.getTime() - target.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  // 1分钟内
  if (diffMin < 1) return '刚刚'
  // 1小时内
  if (diffHour < 1) return `${diffMin}分钟前`
  // 今天内
  if (diffDay < 1) return `${diffHour}小时前`
  // 7天内
  if (diffDay < 7) return `${diffDay}天前`
  // 超过7天显示日期
  const y = target.getFullYear()
  const m = String(target.getMonth() + 1).padStart(2, '0')
  const d = String(target.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// ============================================================
// 导出转换器对象
// ============================================================

export const messageDataConverter = {
  // ----------------------------------------------------------
  // 分类推断
  // ----------------------------------------------------------

  /**
   * 根据消息类型/业务类型推断消息分类
   * @param type - 后端消息类型或业务类型
   * @returns 消息分类
   */
  categoryFromType(type: string): MessageCategory {
    if (!type) return 'system'
    const upperType = type.toUpperCase()
    return BUSINESS_TYPE_CATEGORY_MAP[upperType] ?? 'system'
  },

  // ----------------------------------------------------------
  // 消息项转换
  // ----------------------------------------------------------

  /**
   * 后端原始消息数据转前端 MessageItem
   * 处理isRead数字布尔转换、时间格式化、分类推断
   * @param raw - 后端原始消息数据
   * @returns 前端 MessageItem 对象
   */
  toMessageItem(raw: Record<string, unknown>): MessageItem {
    // isRead: 后端可能传数字 0/1 或布尔
    const rawIsRead = raw.isRead
    const isRead = typeof rawIsRead === 'boolean'
      ? rawIsRead
      : Number(rawIsRead) === 1

    // 根据 businessType 或 type 推断分类
    const businessType = (raw.businessType as string) || (raw.type as string) || ''
    const category = (raw.category as MessageCategory) || this.categoryFromType(businessType)

    return {
      id: String(raw.id ?? ''),
      title: (raw.title as string) || '',
      content: (raw.content as string) || (raw.summary as string) || '',
      category,
      isRead,
      priority: (raw.priority as 'high' | 'normal' | 'low') || CATEGORY_PRIORITY_MAP[category] || 'normal',
      senderName: (raw.senderName as string) || (raw.sender as string) || '系统',
      businessId: (raw.businessId as string) || '',
      businessType,
      createTime: (raw.createTime as string) || (raw.createdAt as string) || '',
      displayTime: formatDisplayTime((raw.createTime as string) || ''),
      route: (raw.route as string) || (raw.actionLink as string) || '',
    }
  },

  /**
   * 批量转换为消息列表
   * @param list - 后端原始消息列表
   * @returns 前端 MessageItem 数组
   */
  toMessageItemList(list: Record<string, unknown>[]): MessageItem[] {
    return list.map(item => this.toMessageItem(item))
  },

  // ----------------------------------------------------------
  // 工具方法
  // ----------------------------------------------------------

  /**
   * 获取分类对应的优先级
   * @param category - 消息分类
   * @returns 优先级
   */
  getCategoryPriority(category: MessageCategory): 'high' | 'normal' | 'low' {
    return CATEGORY_PRIORITY_MAP[category] || 'normal'
  },

  /**
   * 获取分类中文标签
   * @param category - 消息分类
   * @returns 中文标签
   */
  getCategoryLabel(category: MessageCategory): string {
    const labelMap: Record<MessageCategory, string> = {
      system: '系统消息',
      approval: '审批通知',
      schedule: '排班通知',
      reminder: '提醒事项',
      announcement: '公告通知',
    }
    return labelMap[category] || category
  },
}
