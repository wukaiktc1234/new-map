/**
 * 临期预警类型定义
 *
 * 对齐后端 ExpiryAlertController (/v1/expiry-alerts) 的 ExpiryAlertVO。
 * 预警等级、处理状态使用大写枚举字符串，与后端保持一致。
 */

/** 预警等级（对齐后端枚举） */
export type AlertLevel = 'RED' | 'YELLOW' | 'GREEN'

/** 处理状态（对齐后端枚举） */
export type ExpiryHandlingStatus = 'PENDING' | 'SCRAPPED' | 'RETURNED' | 'RESOLVED'

/** 追溯类型（对齐后端枚举） */
export type TraceType = 'MATERIAL' | 'FOOD'

/** 临期预警记录（对齐后端 ExpiryAlertVO） */
export interface ExpiryAlert {
  /** 预警ID */
  alertId: number
  /** 追溯码ID */
  traceCodeId: number
  /** 追溯码 */
  traceCode: string
  /** 追溯类型: MATERIAL/FOOD */
  traceType: TraceType
  /** 追溯类型中文名 */
  traceTypeName: string
  /** 物料ID */
  materialId?: string
  /** 物料名称 */
  materialName?: string
  /** 批次号 */
  batchNo?: string
  /** 供应商ID */
  supplierId?: number
  /** 供应商名称 */
  supplierName?: string
  /** 生产日期（yyyy-MM-dd） */
  productionDate?: string
  /** 过期日期（yyyy-MM-dd） */
  expiryDate: string
  /** 剩余天数（可为负数，负数表示已过期） */
  remainingDays: number
  /** 预警等级: RED/YELLOW/GREEN */
  alertLevel: AlertLevel
  /** 预警等级中文名 */
  alertLevelName: string
  /** 处理状态: PENDING/SCRAPPED/RETURNED/RESOLVED */
  handlingStatus: ExpiryHandlingStatus
  /** 处理状态中文名 */
  handlingStatusName: string
  /** 处理动作 */
  handlingAction?: string
  /** 处理人ID */
  handledById?: number
  /** 处理人姓名 */
  handledByName?: string
  /** 处理时间 */
  handledTime?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 临期预警查询参数 */
export interface ExpiryAlertQuery {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 追溯码 */
  traceCode?: string
  /** 追溯类型 */
  traceType?: TraceType
  /** 物料名称 */
  materialName?: string
  /** 批次号 */
  batchNo?: string
  /** 供应商ID */
  supplierId?: number
  /** 预警等级 */
  alertLevel?: AlertLevel
  /** 处理状态 */
  handlingStatus?: ExpiryHandlingStatus
  /** 过期日期起始 */
  expiryDateStart?: string
  /** 过期日期结束 */
  expiryDateEnd?: string
}

/** 临期预警看板（对齐后端 ExpiryDashboardVO） */
export interface ExpiryDashboard {
  /** 红色预警数（已过期） */
  redCount: number
  /** 黄色预警数（7天内） */
  yellowCount: number
  /** 绿色预警数（7天外） */
  greenCount: number
  /** 待处理总数 */
  totalPending: number
  /** 已报损总数 */
  totalScrapped: number
  /** 已退货总数 */
  totalReturned: number
  /** 最近预警列表 */
  recentAlerts: ExpiryAlert[]
}

/** 临期预警统计 */
export interface ExpiryStatistics {
  /** 预警总数 */
  totalAlerts: number
  /** 红色预警数 */
  redAlerts: number
  /** 黄色预警数 */
  yellowAlerts: number
  /** 绿色预警数 */
  greenAlerts: number
  /** 待处理数 */
  pendingCount: number
  /** 已报损数 */
  scrappedCount: number
  /** 已退货数 */
  returnedCount: number
  /** 已处理数 */
  resolvedCount: number
}

/** 预警等级映射 */
export const AlertLevelMap: Record<AlertLevel, { label: string; status: string }> = {
  RED: { label: '红色预警', status: 'error' },
  YELLOW: { label: '黄色预警', status: 'warning' },
  GREEN: { label: '绿色预警', status: 'success' },
}

/** 处理状态映射 */
export const ExpiryHandlingStatusMap: Record<ExpiryHandlingStatus, { label: string; status: string }> = {
  PENDING: { label: '待处理', status: 'warning' },
  SCRAPPED: { label: '已报损', status: 'error' },
  RETURNED: { label: '已退货', status: 'info' },
  RESOLVED: { label: '已处理', status: 'success' },
}
