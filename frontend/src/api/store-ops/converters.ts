import type {
  StoreListItem,
  StoreBackend,
  StoreStatus,
  DailySettlement,
  SettlementBackend,
  SettlementStatus,
  PendingTask,
  TaskBackend,
  Certificate,
  CertificateBackend,
  DiningTable,
  DiningTableBackend,
  DiningTableFormData,
  CallNumberQueue,
  CallNumberQueueBackend,
} from '@/types/store-operation'
import { fenToYuanNumber, yuanToFen } from '@/utils/money'

/**
 * 门店数据转换器
 * 处理门店相关数据的前后端格式映射
 */
export const storeDataConverter = {
  /**
   * 后端 → 前端（API响应 → 页面显示）
   */
  toFrontend(backend: StoreBackend): StoreListItem {
    return {
      storeId: backend.storeId,
      storeName: backend.storeName,
      address: backend.address,
      phone: backend.phone,
      status: this.convertStatus(backend.status),
      openTime: backend.openTime,
      closeTime: backend.closeTime,
      managerId: backend.managerId,
      managerName: backend.managerName,
      todayRevenue: this.fenToYuan(backend.todayRevenue),
      orderCount: backend.orderCount,
      dineInCount: backend.dineInCount,
      takeoutCount: backend.takeoutCount,
      pickupCount: backend.pickupCount ?? 0,
      tableUsageRate: backend.tableUsageRate,
      onDutyStaff: backend.onDutyStaff,
    }
  },

  /**
   * 前端 → 后端（表单提交 → API请求）
   */
  toBackend(frontend: Partial<StoreListItem>): Partial<StoreBackend> {
    return {
      ...frontend,
      status: this.convertToStatusNum(frontend.status!),
      todayRevenue: this.yuanToFen(frontend.todayRevenue!),
    }
  },

  /** 状态码转换：后端数字 → 前端字符串 */
  convertStatus(statusCode: number): StoreStatus {
    const map: Record<number, StoreStatus> = { 1: 'running', 0: 'paused', 2: 'closed' }
    return map[statusCode] ?? 'paused'
  },

  /** 状态码转换：前端字符串 → 后端数字 */
  convertToStatusNum(status: string): number {
    const map: Record<string, number> = { running: 1, paused: 0, closed: 2 }
    return map[status] ?? 0
  },

  /** 分转元（后端分 → 前端元，返回 number） */
  fenToYuan(fen: number | null | undefined): number {
    return fenToYuanNumber(fen)
  },

  /** 元转分（前端元 → 后端分） */
  yuanToFen(yuan: string | number | null | undefined): number {
    return yuanToFen(yuan)
  },
}

/**
 * 对账数据转换器
 * 处理日结对账数据的格式映射和金额转换
 */
export const settlementDataConverter = {
  /**
   * 后端 → 前端
   */
  toFrontend(backend: SettlementBackend): DailySettlement {
    return {
      settlementId: backend.settlementId,
      settlementDate: backend.settlementDate,
      storeId: backend.storeId,
      storeName: backend.storeName,
      totalRevenue: storeDataConverter.fenToYuan(backend.totalRevenue),
      totalCost: storeDataConverter.fenToYuan(backend.totalCost),
      netProfit: storeDataConverter.fenToYuan(backend.netProfit),
      grossProfitRate: backend.grossProfitRate,
      orderCount: backend.orderCount,
      avgOrderValue: storeDataConverter.fenToYuan(backend.avgOrderValue),
      auditorId: backend.auditorId,
      auditorName: backend.auditorName,
      status: this.convertStatus(backend.status),
      confirmTime: backend.confirmTime,
      remark: backend.remark,
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  /** 状态码转换 */
  convertStatus(statusCode: number): SettlementStatus {
    const map: Record<number, SettlementStatus> = { 0: 'pending', 1: 'approved', 2: 'rejected' }
    return map[statusCode] ?? 'pending'
  },
}

/**
 * 任务数据转换器
 * 处理待办任务的数据映射
 */
export const taskDataConverter = {
  /**
   * 后端 → 前端
   */
  toFrontend(backend: TaskBackend): PendingTask {
    return {
      taskId: backend.taskId,
      taskType: this.convertTaskType(backend.taskType),
      title: backend.title,
      description: backend.description,
      sourceModule: backend.sourceModule,
      sourceRefId: backend.sourceRefId,
      assigneeId: backend.assigneeId,
      assigneeName: backend.assigneeName,
      deadline: backend.deadline,
      priority: this.convertPriority(backend.priority),
      status: this.convertTaskStatus(backend.status),
      overdueDays: backend.overdueDays,
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  /** 任务类型映射 */
  convertTaskType(typeCode: number): PendingTask['taskType'] {
    const map: Record<number, PendingTask['taskType']> = {
      1: 'approval', 2: 'inspection', 3: 'refund',
      4: 'settlement', 5: 'certificate', 6: 'audit',
      7: 'maintenance', 99: 'other',
    }
    return map[typeCode] ?? 'other'
  },

  /** 优先级映射：后端数字 → 前端字符串 */
  convertPriority(priorityCode: number): PendingTask['priority'] {
    const map: Record<number, PendingTask['priority']> = { 3: 'high', 2: 'medium', 1: 'low' }
    return map[priorityCode] ?? 'medium'
  },

  /** 优先级映射：前端字符串 → 后端数字 */
  convertToPriorityNum(priority: PendingTask['priority']): number {
    const map: Record<string, number> = { low: 1, medium: 2, high: 3 }
    return map[priority] ?? 2
  },

  /** 状态映射 */
  convertTaskStatus(statusCode: number): PendingTask['status'] {
    const map: Record<number, PendingTask['status']> = {
      0: 'pending', 1: 'completed', 2: 'expired', 3: 'cancelled',
    }
    return map[statusCode] ?? 'unknown'
  },
}

/**
 * 证件数据转换器
 * 处理证件管理数据的格式映射，自动计算到期信息
 */
export const certificateDataConverter = {
  /**
   * 后端 → 前端
   * 自动计算 daysLeft、推导 status 和 alertLevel
   */
  toFrontend(backend: CertificateBackend): Certificate {
    const daysLeft = this.calculateDaysLeft(backend.expiryDate)

    return {
      certificateId: backend.certificateId,
      certName: backend.certName,
      certType: this.convertCertType(backend.certType),
      certNumber: backend.certNumber,
      holderType: backend.holderType === 1 ? 'company' : 'employee',
      holderId: backend.holderId,
      holderName: backend.holderName,
      issueDate: backend.issueDate,
      expiryDate: backend.expiryDate,
      daysLeft,
      status: this.deriveStatus(daysLeft, backend.status),
      fileUrl: backend.fileUrl,
      alertLevel: this.deriveAlertLevel(daysLeft),
      issuer: backend.issuer,
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  /** 计算剩余天数 */
  calculateDaysLeft(expiryDate: string): number {
    const now = new Date()
    const expiry = new Date(expiryDate)

    if (isNaN(expiry.getTime())) {
      return -9999 // 无效日期标记
    }

    const diffTime = expiry.getTime() - now.getTime()
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  },

  /** 根据剩余天数推导状态 */
  deriveStatus(daysLeft: number, _originalStatus: number): Certificate['status'] {
    if (daysLeft === -9999) return 'expired' // 无效日期视为已过期
    if (daysLeft < 0) return 'expired'
    if (daysLeft <= 30) return 'expiring'
    return 'active'
  },

  /** 推导预警级别 */
  deriveAlertLevel(daysLeft: number): Certificate['alertLevel'] {
    if (daysLeft === -9999) return 'danger' // 无效日期视为危险
    if (daysLeft < 0) return 'danger'
    if (daysLeft <= 30) return 'warning'
    return 'normal'
  },

  /** 证件类型映射 */
  convertCertType(typeCode: number): Certificate['certType'] {
    const map: Record<number, Certificate['certType']> = {
      1: 'business_license', 2: 'catering_license',
      3: 'hygiene_license', 4: 'safety_license',
      5: 'health_certificate', 6: 'pollution_license', 99: 'other',
    }
    return map[typeCode] ?? 'other'
  },
}

/**
 * 餐桌数据转换器
 * 处理餐桌管理数据的前后端格式映射
 */
export const diningTableDataConverter = {
  toFrontend(backend: DiningTableBackend): DiningTable {
    return {
      tableId: backend.tableId,
      storeId: backend.storeId,
      tableCode: backend.tableCode,
      tableName: backend.tableName,
      tableType: this.convertTableType(backend.tableType),
      seatsCount: backend.seatsCount,
      status: this.convertStatus(backend.status),
      qrCode: backend.qrCode,
      sortOrder: backend.sortOrder,
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  toBackend(frontend: Partial<DiningTableFormData>): Record<string, unknown> {
    return {
      ...frontend,
      tableType: frontend.tableType ? this.convertToTableTypeNum(frontend.tableType) : undefined,
    }
  },

  convertStatus(statusCode: number): DiningTable['status'] {
    const map: Record<number, DiningTable['status']> = {
      1: 'idle', 2: 'dining', 3: 'reserved', 4: 'maintenance', 5: 'disabled',
    }
    return map[statusCode] ?? 'idle'
  },

  convertTableType(typeCode: number): DiningTable['tableType'] {
    const map: Record<number, DiningTable['tableType']> = {
      1: 'hall', 2: 'private_room', 3: 'bar', 4: 'outdoor',
    }
    return map[typeCode] ?? 'hall'
  },

  convertToTableTypeNum(type: DiningTable['tableType']): number {
    const map: Record<string, number> = { hall: 1, private_room: 2, bar: 3, outdoor: 4 }
    return map[type] ?? 1
  },

  convertToStatusNum(status: DiningTable['status']): number {
    const map: Record<string, number> = { idle: 1, dining: 2, reserved: 3, maintenance: 4, disabled: 5 }
    return map[status] ?? 1
  },

  toStatusTagStatus(status: DiningTable['status']): string {
    const map: Record<DiningTable['status'], string> = {
      idle: 'success', dining: 'warning', reserved: 'info', maintenance: 'warning', disabled: 'default',
    }
    return map[status] ?? 'default'
  },

  toStatusLabel(status: DiningTable['status']): string {
    const map: Record<DiningTable['status'], string> = {
      idle: '空闲', dining: '用餐中', reserved: '已预约', maintenance: '维护中', disabled: '停用',
    }
    return map[status] ?? '未知'
  },

  toTableTypeLabel(type: DiningTable['tableType']): string {
    const map: Record<DiningTable['tableType'], string> = {
      hall: '大厅', private_room: '包厢', bar: '吧台', outdoor: '露台',
    }
    return map[type] ?? type
  },
}

/**
 * 叫号排队数据转换器
 * 处理叫号排队数据的前后端格式映射
 */
export const callNumberQueueDataConverter = {
  toFrontend(backend: CallNumberQueueBackend): CallNumberQueue {
    return {
      queueId: backend.queueId,
      storeId: backend.storeId,
      ticketNumber: backend.ticketNumber,
      queueType: this.convertQueueType(backend.queueType),
      peopleCount: backend.peopleCount,
      tablePreference: backend.tablePreference,
      status: this.convertStatus(backend.status),
      callTime: backend.callTime,
      calledCount: backend.calledCount,
      createTime: backend.createTime,
    }
  },

  convertStatus(statusCode: number): CallNumberQueue['status'] {
    const map: Record<number, CallNumberQueue['status']> = {
      1: 'waiting', 2: 'called', 3: 'expired', 4: 'dined', 5: 'cancelled',
    }
    return map[statusCode] ?? 'waiting'
  },

  convertQueueType(typeCode: number): CallNumberQueue['queueType'] {
    const map: Record<number, CallNumberQueue['queueType']> = {
      1: 'dine_in', 2: 'takeout', 3: 'pickup',
    }
    return map[typeCode] ?? 'dine_in'
  },

  /** 状态码转换：前端字符串 → 后端数字 */
  convertToStatusNum(status: CallNumberQueue['status']): number {
    const map: Record<string, number> = { waiting: 1, called: 2, expired: 3, dined: 4, cancelled: 5 }
    return map[status] ?? 1
  },

  /** 队列类型转换：前端字符串 → 后端数字 */
  convertToQueueTypeNum(queueType: CallNumberQueue['queueType']): number {
    const map: Record<string, number> = { dine_in: 1, takeout: 2, pickup: 3 }
    return map[queueType] ?? 1
  },

  toStatusTagStatus(status: CallNumberQueue['status']): string {
    const map: Record<CallNumberQueue['status'], string> = {
      waiting: 'warning', called: 'primary', expired: 'default', dined: 'success', cancelled: 'info',
    }
    return map[status] ?? 'default'
  },

  toStatusLabel(status: CallNumberQueue['status']): string {
    const map: Record<CallNumberQueue['status'], string> = {
      waiting: '等待中', called: '已叫号', expired: '已过号', dined: '已用餐', cancelled: '已取消',
    }
    return map[status] ?? '未知'
  },
}
