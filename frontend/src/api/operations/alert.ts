/**
 * 告警管理 API
 * 对应后端: AlertController (/v1/alerts)
 *
 * 对接策略：
 * - 所有方法调用真实后端 API，错误由 request 拦截器统一处理
 * - 后端 AlertEntity 字段（severity/status）与前端 AlertItem 字段（level/status）存在差异，需做字段映射
 *
 * 字段映射说明（P1）：
 * - 前端 level(error/warning/info) ↔ 后端 severity(ERROR/WARNING/INFO)
 * - 前端 status(pending/processing/resolved/ignored) ↔ 后端 status(ACTIVE/ACKNOWLEDGED/RESOLVED)
 *   注：后端无 ignored 状态，前端 ignored 仅作本地状态保留（不向后端同步）
 */
import { get, put, del } from '@/api/request'

// ============================================================
// 类型定义
// ============================================================

/** 前端告警级别 */
export type AlertLevel = 'error' | 'warning' | 'info'

/** 前端告警状态（含本地态 ignored） */
export type AlertStatus = 'pending' | 'processing' | 'resolved' | 'ignored'

/** 前端告警业务类型 */
export type AlertType =
  | 'revenue'
  | 'staff'
  | 'device'
  | 'inventory'
  | 'certificate'
  | 'food_safety'
  | 'complaint'

/** 前端告警数据结构（与视图 AlertItem 保持一致） */
export interface AlertItem {
  id: string
  level: AlertLevel
  type: AlertType
  storeName: string
  message: string
  source: string
  triggerTime: string
  ruleName: string
  affectedScope: string
  status: AlertStatus
  processHistory?: Array<{ time: string; action: string; operator: string }>
}

/** 告警查询参数 */
export interface AlertQuery {
  page?: number
  size?: number
  /** 前端 level（API 内部转换为后端 severity） */
  level?: AlertLevel | ''
  /** 前端 status（API 内部转换为后端 status） */
  status?: AlertStatus | ''
  /** 告警来源 */
  source?: string
}

/** 告警统计 */
export interface AlertStatistics {
  activeCount: number
  acknowledgedCount: number
  resolvedCount: number
  errorCount: number
  warningCount: number
  infoCount: number
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
  pages?: number
}

// ============================================================
// 字段映射常量
// ============================================================

/** 后端 severity → 前端 level */
const SEVERITY_TO_LEVEL: Record<string, AlertLevel> = {
  ERROR: 'error',
  WARNING: 'warning',
  INFO: 'info',
}

/** 前端 level → 后端 severity */
const LEVEL_TO_SEVERITY: Record<AlertLevel, string> = {
  error: 'ERROR',
  warning: 'WARNING',
  info: 'INFO',
}

/** 后端 status → 前端 status */
const BACKEND_STATUS_TO_FRONTEND: Record<string, AlertStatus> = {
  ACTIVE: 'pending',
  ACKNOWLEDGED: 'processing',
  RESOLVED: 'resolved',
}

/** 前端 status → 后端 status（ignored 无后端对应，不参与转换） */
const FRONTEND_STATUS_TO_BACKEND: Record<Exclude<AlertStatus, 'ignored'>, string> = {
  pending: 'ACTIVE',
  processing: 'ACKNOWLEDGED',
  resolved: 'RESOLVED',
}

// ============================================================
// 后端实体接口（AlertEntity 的 JSON 形态）
// ============================================================

interface AlertBackend {
  alertId?: number
  name?: string
  description?: string
  severity?: string
  status?: string
  value?: number | null
  threshold?: number | null
  source?: string
  sourceType?: string
  sourceId?: string
  alertTime?: string
  resolveTime?: string | null
  acknowledgeTime?: string | null
  acknowledgedBy?: string
  resolvedBy?: string
  resolveDescription?: string
  assignedTo?: number | null
  createTime?: string
  updateTime?: string
}

// ============================================================
// 字段适配器
// ============================================================

/**
 * 根据 sourceType/source 推断前端业务类型
 * 后端 AlertEntity 无显式 type 字段，从 sourceType 间接推断
 */
function inferAlertType(sourceType?: string, source?: string): AlertType {
  const text = `${sourceType || ''} ${source || ''}`.toLowerCase()
  if (text.includes('revenue') || text.includes('营收') || text.includes('订单')) return 'revenue'
  if (text.includes('staff') || text.includes('员工') || text.includes('排班')) return 'staff'
  if (text.includes('device') || text.includes('设备') || text.includes('收银')) return 'device'
  if (text.includes('inventory') || text.includes('库存')) return 'inventory'
  if (text.includes('certificate') || text.includes('证照') || text.includes('证件')) return 'certificate'
  if (text.includes('food_safety') || text.includes('食品')) return 'food_safety'
  if (text.includes('complaint') || text.includes('客诉')) return 'complaint'
  return 'revenue'
}

/** 后端实体 → 前端 AlertItem */
function adaptAlert(raw: AlertBackend): AlertItem {
  const backendStatus = raw.status || 'ACTIVE'
  const severity = raw.severity || 'INFO'
  const triggerTime = raw.alertTime || new Date().toISOString()

  // 由 acknowledge/resolve 时间构造处理历史
  const processHistory: Array<{ time: string; action: string; operator: string }> = []
  if (raw.acknowledgeTime) {
    processHistory.push({
      time: new Date(raw.acknowledgeTime).toLocaleString('zh-CN'),
      action: '标记为处理中',
      operator: raw.acknowledgedBy || '系统',
    })
  }
  if (raw.resolveTime) {
    processHistory.push({
      time: new Date(raw.resolveTime).toLocaleString('zh-CN'),
      action: raw.resolveDescription ? `已解决：${raw.resolveDescription}` : '已解决',
      operator: raw.resolvedBy || '系统',
    })
  }

  return {
    id: String(raw.alertId ?? ''),
    level: SEVERITY_TO_LEVEL[severity] || 'info',
    type: inferAlertType(raw.sourceType, raw.source),
    storeName: raw.source || '未知门店',
    message: raw.description || '',
    source: raw.source || '',
    triggerTime,
    ruleName: raw.name || '',
    affectedScope: raw.sourceType || '关联业务数据',
    status: BACKEND_STATUS_TO_FRONTEND[backendStatus] || 'pending',
    processHistory,
  }
}

// ============================================================
// API 实现
// ============================================================

export const alertApi = {
  /**
   * 分页查询告警列表
   * GET /v1/alerts?current&size&severity&status&source
   * @param query 查询参数（前端语义化字段，内部转换为后端字段）
   */
  async getAlerts(query: AlertQuery = {}): Promise<PageResult<AlertItem>> {
    const { page = 1, size = 20, level, status, source } = query
    // 前端字段映射为后端字段
    const params: Record<string, unknown> = {
      current: page,
      size,
    }
    if (level) params.severity = LEVEL_TO_SEVERITY[level as AlertLevel]
    if (status && status !== 'ignored') {
      params.status = FRONTEND_STATUS_TO_BACKEND[status as Exclude<AlertStatus, 'ignored'>]
    }
    if (source) params.source = source

    const res = await get<PageResult<AlertBackend>>(`/v1/alerts`, params)
    const records = (res?.records || []).map(adaptAlert)
    return {
      records,
      total: res?.total || records.length,
      current: res?.current || page,
      size: res?.size || size,
      pages: res?.pages,
    }
  },

  /**
   * 获取活跃告警列表
   * GET /v1/alerts/active
   */
  async getActiveAlerts(): Promise<AlertItem[]> {
    const res = await get<AlertBackend[]>('/v1/alerts/active')
    return (res || []).map(adaptAlert)
  },

  /**
   * 根据 ID 获取告警
   * GET /v1/alerts/{alertId}
   */
  async getAlertById(alertId: string): Promise<AlertItem | null> {
    const res = await get<AlertBackend>(`/v1/alerts/${encodeURIComponent(alertId)}`)
    return res ? adaptAlert(res) : null
  },

  /**
   * 获取告警统计
   * GET /v1/alerts/statistics
   */
  async getStatistics(): Promise<AlertStatistics> {
    const res = await get<AlertStatistics>('/v1/alerts/statistics')
    return {
      activeCount: res?.activeCount || 0,
      acknowledgedCount: res?.acknowledgedCount || 0,
      resolvedCount: res?.resolvedCount || 0,
      errorCount: res?.errorCount || 0,
      warningCount: res?.warningCount || 0,
      infoCount: res?.infoCount || 0,
    }
  },

  /**
   * 确认告警（前端"标记处理中"）
   * PUT /v1/alerts/{alertId}/acknowledge
   * 后端将状态从 ACTIVE → ACKNOWLEDGED
   */
  async acknowledgeAlert(alertId: string): Promise<AlertItem | null> {
    const res = await put<AlertBackend>(`/v1/alerts/${encodeURIComponent(alertId)}/acknowledge`)
    return res ? adaptAlert(res) : null
  },

  /**
   * 解决告警（前端"标记已解决"）
   * PUT /v1/alerts/{alertId}/resolve?resolveDescription=xxx
   * 后端将状态从 ACKNOWLEDGED → RESOLVED
   * @param alertId 告警 ID
   * @param resolveDescription 解决描述
   */
  async resolveAlert(alertId: string, resolveDescription: string): Promise<AlertItem | null> {
    // 后端 resolveDescription 通过 query 参数传递
    const res = await put<AlertBackend>(
      `/v1/alerts/${encodeURIComponent(alertId)}/resolve`,
      {},
      { params: { resolveDescription } }
    )
    return res ? adaptAlert(res) : null
  },

  /**
   * 删除告警（逻辑删除）
   * DELETE /v1/alerts/{alertId}
   */
  async deleteAlert(alertId: string): Promise<boolean> {
    await del<void>(`/v1/alerts/${encodeURIComponent(alertId)}`)
    return true
  },
}

export default alertApi
