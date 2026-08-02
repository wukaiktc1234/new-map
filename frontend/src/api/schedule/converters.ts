/**
 * 排班管理模块 - 数据转换器
 * 负责后端数据与前端展示数据之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 *
 * 状态字段使用语义化字符串（符合项目规范 §24 前端语义化状态）
 */

import type {
  SchedulePlan,
  SchedulePlanStatus,
  SyncStatus,
  SwapRequest,
  SwapRequestStatus,
  SwapEntryInfo,
  ShiftTypeCode,
  NotificationLog,
  SendStatus,
  NotificationChannel,
} from '@/types/schedule'

// ============================================================
// 一、排班计划状态映射
// ============================================================

/** 后端状态值 → 前端语义化状态 */
const PLAN_STATUS_MAP: Record<string, SchedulePlanStatus> = {
  '0': 'draft',
  '1': 'published',
  '2': 'executing',
  '3': 'archived',
}

/** 前端语义化状态 → 中文标签 */
const PLAN_STATUS_LABEL_MAP: Record<SchedulePlanStatus, string> = {
  draft: '草稿',
  published: '已发布',
  executing: '执行中',
  archived: '已归档',
}

/** 同步状态值 → 前端语义化状态 */
const SYNC_STATUS_MAP: Record<string, SyncStatus> = {
  '0': 'not_synced',
  '1': 'synced',
  '2': 'failed',
}

/** 同步状态中文标签 */
const SYNC_STATUS_LABEL_MAP: Record<SyncStatus, string> = {
  not_synced: '未同步',
  synced: '已同步',
  failed: '同步失败',
}

// ============================================================
// 二、换班请求状态映射
// ============================================================

/** 后端状态值 → 前端语义化状态 */
const SWAP_STATUS_MAP: Record<string, SwapRequestStatus> = {
  '0': 'pending',
  '1': 'approved',
  '2': 'rejected',
  '3': 'cancelled',
}

/** 前端语义化状态 → 中文标签 */
const SWAP_STATUS_LABEL_MAP: Record<SwapRequestStatus, string> = {
  pending: '待审批',
  approved: '已通过',
  rejected: '已驳回',
  cancelled: '已取消',
}

// ============================================================
// 三、通知发送状态映射
// ============================================================

/** 后端状态值 → 前端语义化状态 */
const SEND_STATUS_MAP: Record<string, SendStatus> = {
  '0': 'pending',
  '1': 'sending',
  '2': 'success',
  '3': 'failed',
}

/** 发送状态中文标签 */
const SEND_STATUS_LABEL_MAP: Record<SendStatus, string> = {
  pending: '待发送',
  sending: '发送中',
  success: '发送成功',
  failed: '发送失败',
}

/** 通知渠道映射 */
const CHANNEL_MAP: Record<string, NotificationChannel> = {
  '0': 'in_app',
  '1': 'sms',
  '2': 'email',
  '3': 'wechat_work',
}

/** 通知渠道中文标签 */
const CHANNEL_LABEL_MAP: Record<NotificationChannel, string> = {
  in_app: '站内信',
  sms: '短信',
  email: '邮件',
  wechat_work: '企业微信',
}

// ============================================================
// 四、班次类型代码映射
// ============================================================

/** 班次类型代码中文标签 */
const SHIFT_TYPE_LABEL_MAP: Record<ShiftTypeCode, string> = {
  morning: '早班',
  noon: '午班',
  evening: '晚班',
  night_off: '休息',
}

// ============================================================
// 五、排班计划转换器
// ============================================================

/** 后端返回的排班计划原始数据 */
interface SchedulePlanBackend {
  planId: string
  planName: string
  storeId: string
  storeName: string
  startDate: string
  endDate: string
  status: string | number
  version: number
  templateId?: string
  templateName?: string
  employeeCount: number
  totalWorkHours: number
  publisherId?: string
  publisherName?: string
  publishTime?: string
  attendanceSyncStatus: string | number
  attendanceSyncTime?: string
  conflictCheckStatus: string | number
  conflictCheckTime?: string
  createTime: string
  updateTime: string
}

export const schedulePlanConverter = {
  /**
   * 将后端数据转换为前端格式（单条）
   * @param backend - 后端原始数据
   * @returns 前端展示数据
   */
  toFrontend(backend: SchedulePlanBackend): SchedulePlan {
    const rawStatus = String(backend.status ?? '0')
    const rawSyncStatus = String(
      backend.attendanceSyncStatus ?? '0'
    )
    const rawConflictStatus = String(
      backend.conflictCheckStatus ?? '0'
    )

    return {
      ...backend,
      status:
        PLAN_STATUS_MAP[rawStatus] ??
        ('draft' as SchedulePlanStatus),
      attendanceSyncStatus:
        SYNC_STATUS_MAP[rawSyncStatus] ??
        ('not_synced' as SyncStatus),
      conflictCheckStatus:
        SYNC_STATUS_MAP[rawConflictStatus] ??
        ('not_synced' as SyncStatus),
    }
  },

  /**
   * 批量将后端数据转换为前端格式
   * @param list - 后端原始数据列表
   * @returns 前端展示数据列表
   */
  toFrontendList(list: SchedulePlanBackend[]): SchedulePlan[] {
    return (list || []).map((item) => this.toFrontend(item))
  },

  /**
   * 确保状态值为有效的语义化字符串
   * 用于处理未知或异常的状态值
   * @param value - 原始状态值
   * @param fallback - 默认值（可选，默认'draft'）
   * @returns 有效的状态值
   */
  ensureValidStatus(
    value: unknown,
    fallback: SchedulePlanStatus = 'draft'
  ): SchedulePlanStatus {
    if (
      value &&
      typeof value === 'string' &&
      ['draft', 'published', 'executing', 'archived'].includes(value)
    ) {
      return value as SchedulePlanStatus
    }
    // 尝试从数字映射
    const mapped =
      PLAN_STATUS_MAP[String(value ?? '')]
    return mapped ?? fallback
  },

  /**
   * 确保同步状态值为有效的语义化字符串
   * @param value - 原始状态值
   * @param fallback - 默认值（可选，默认'not_synced'）
   * @returns 有效的同步状态值
   */
  ensureValidSyncStatus(
    value: unknown,
    fallback: SyncStatus = 'not_synced'
  ): SyncStatus {
    if (
      value &&
      typeof value === 'string' &&
      ['not_synced', 'synced', 'failed'].includes(value)
    ) {
      return value as SyncStatus
    }
    const mapped = SYNC_STATUS_MAP[String(value ?? '')]
    return mapped ?? fallback
  },

  /** 获取状态中文标签 */
  getStatusLabel(status: SchedulePlanStatus): string {
    return PLAN_STATUS_LABEL_MAP[status] ?? status
  },

  /** 获取同步状态中文标签 */
  getSyncStatusLabel(status: SyncStatus): string {
    return SYNC_STATUS_LABEL_MAP[status] ?? status
  },
}

// ============================================================
// 六、换班请求转换器
// ============================================================

/** 后端返回的换班请求原始数据 */
interface SwapRequestBackend {
  requestId: string
  planId: string
  planName: string
  status: string | number
  initiatorId: string
  initiatorName: string
  initiatorEntry: Record<string, unknown>
  targetEmployeeId: string
  targetEmployeeName: string
  targetEntry: Record<string, unknown>
  reason?: string
  createTime: string
  approverId?: string
  approverName?: string
  approveTime?: string
  approveComment?: string
}

export const swapRequestConverter = {
  /**
   * 将后端数据转换为前端格式（单条）
   * @param backend - 后端原始数据
   * @returns 前端展示数据
   */
  toFrontend(backend: SwapRequestBackend): SwapRequest {
    const rawStatus = String(backend.status ?? '0')

    return {
      requestId: backend.requestId,
      planId: backend.planId,
      planName: backend.planName,
      status:
        SWAP_STATUS_MAP[rawStatus] ??
        ('pending' as SwapRequestStatus),
      initiatorId: backend.initiatorId,
      initiatorName: backend.initiatorName,
      initiatorEntry: this.convertEntryInfo(
        backend.initiatorEntry
      ),
      targetEmployeeId: backend.targetEmployeeId,
      targetEmployeeName: backend.targetEmployeeName,
      targetEntry: this.convertEntryInfo(backend.targetEntry),
      reason: backend.reason,
      createTime: backend.createTime,
      approverId: backend.approverId,
      approverName: backend.approverName,
      approveTime: backend.approveTime,
      approveComment: backend.approveComment,
    }
  },

  /**
   * 批量将后端数据转换为前端格式
   * @param list - 后端原始数据列表
   * @returns 前端展示数据列表
   */
  toFrontendList(list: SwapRequestBackend[]): SwapRequest[] {
    return (list || []).map((item) => this.toFrontend(item))
  },

  /**
   * 确保换班请求状态值为有效的语义化字符串
   * @param value - 原始状态值
   * @param fallback - 默认值（可选，默认'pending'）
   * @returns 有效的状态值
   */
  ensureValidStatus(
    value: unknown,
    fallback: SwapRequestStatus = 'pending'
  ): SwapRequestStatus {
    if (
      value &&
      typeof value === 'string' &&
      ['pending', 'approved', 'rejected', 'cancelled'].includes(value)
    ) {
      return value as SwapRequestStatus
    }
    const mapped = SWAP_STATUS_MAP[String(value ?? '')]
    return mapped ?? fallback
  },

  /**
   * 转换换班条目信息
   * 处理可能为JSON字符串或对象的情况
   * @param data - 条目数据（可能是对象或JSON字符串）
   * @returns 标准化的条目信息
   */
  convertEntryInfo(data: Record<string, unknown>): SwapEntryInfo {
    return {
      entryId: String(data.entryId ?? ''),
      date: String(data.date ?? ''),
      shiftType: (data.shiftCode ?? data.shiftType ??
        'morning') as ShiftTypeCode,
      shiftName: String(data.shiftName ?? ''),
      startTime: String(data.startTime ?? ''),
      endTime: String(data.endTime ?? ''),
    }
  },

  /**
   * 解析JSON数组字段
   * 处理后端返回的可能是JSON字符串的数组字段
   * @param value - 原始值（可能是数组、JSON字符串或其他）
   * @returns 解析后的数组
   */
  parseJsonArray<T>(value: unknown): T[] {
    if (Array.isArray(value)) {
      return value as T[]
    }
    if (typeof value === 'string' && value.trim()) {
      try {
        return JSON.parse(value) as T[]
      } catch {
        console.warn('[SwapConverter] JSON解析失败:', value)
        return []
      }
    }
    return []
  },

  /** 获取状态中文标签 */
  getStatusLabel(status: SwapRequestStatus): string {
    return SWAP_STATUS_LABEL_MAP[status] ?? status
  },
}

// ============================================================
// 七、通知日志转换器
// ============================================================

/** 后端返回的通知日志原始数据 */
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

export const notificationLogConverter = {
  /**
   * 将后端数据转换为前端格式（单条）
   * @param backend - 后端原始数据
   * @returns 前端展示数据
   */
  toFrontend(backend: NotificationLogBackend): NotificationLog {
    const rawStatus = String(backend.sendStatus ?? '0')
    const rawChannel = String(backend.channel ?? '0')

    return {
      logId: backend.logId,
      businessType: backend.businessType,
      businessId: backend.businessId,
      title: backend.title,
      content: backend.content,
      receiverIds: swapRequestConverter.parseJsonArray<number>(
        backend.receiverIds
      ),
      receiverNames: swapRequestConverter.parseJsonArray<string>(
        backend.receiverNames
      ),
      channel:
        CHANNEL_MAP[rawChannel] ??
        ('in_app' as NotificationChannel),
      sendStatus:
        SEND_STATUS_MAP[rawStatus] ??
        ('pending' as SendStatus),
      sendTime: backend.sendTime,
      retryCount: backend.retryCount ?? 0,
      errorMessage: backend.errorMessage,
      createTime: backend.createTime,
    }
  },

  /**
   * 确保发送状态值为有效的语义化字符串
   * @param value - 原始状态值
   * @param fallback - 默认值（可选，默认'pending'）
   * @returns 有效的状态值
   */
  ensureValidSendStatus(
    value: unknown,
    fallback: SendStatus = 'pending'
  ): SendStatus {
    if (
      value &&
      typeof value === 'string' &&
      ['pending', 'sending', 'success', 'failed'].includes(value)
    ) {
      return value as SendStatus
    }
    const mapped = SEND_STATUS_MAP[String(value ?? '')]
    return mapped ?? fallback
  },

  /**
   * 解析JSON数组字段
   * 处理后端返回的可能是JSON字符串的数组字段
   * @param value - 原始值（可能是数组、JSON字符串或其他）
   * @returns 解析后的数组
   */
  parseJsonArray<T>(value: unknown): T[] {
    return swapRequestConverter.parseJsonArray<T>(value)
  },

  /** 获取发送状态中文标签 */
  getSendStatusLabel(status: SendStatus): string {
    return SEND_STATUS_LABEL_MAP[status] ?? status
  },

  /** 获取通知渠道中文标签 */
  getChannelLabel(channel: NotificationChannel): string {
    return CHANNEL_LABEL_MAP[channel] ?? channel
  },
}

// ============================================================
// 八、通用工具函数导出
// ============================================================

/**
 * 获取班次类型中文标签
 * @param code - 班次类型代码
 * @returns 中文名称
 */
export function getShiftTypeLabel(code: ShiftTypeCode): string {
  return SHIFT_TYPE_LABEL_MAP[code] ?? code
}
