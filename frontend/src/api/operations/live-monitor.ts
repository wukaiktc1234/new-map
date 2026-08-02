/**
 * 实时监控 API
 * 对应后端: LiveMonitorController (/v1/operations/live-monitor)
 *
 * 后端返回 Map<String, Object> / List<Map<String, Object>>，结构灵活，
 * 前端按约定字段读取并做防御性处理。
 *
 * 后端端点：
 * - GET /v1/operations/live-monitor/overview  实时监控总览
 * - GET /v1/operations/live-monitor/stores    门店实时状态列表
 * - GET /v1/operations/live-monitor/trend     趋势数据（参数 dimension: 24h/7d/30d）
 * - GET /v1/operations/live-monitor/alerts    实时告警列表
 */
import { get } from '@/api/request'

// ============================================================
// 类型定义（与 LiveMonitor.vue 内部类型保持一致）
// ============================================================

/** 门店营业状态 */
export type StoreStatus = 'active' | 'inactive'

/** 门店实时记录 */
export interface StoreRecord {
  storeName: string
  todayRevenue: number
  yesterdayRevenue: number
  orderCount: number
  dineIn: number
  takeaway: number
  selfPickup: number
  turnoverRate: number
  staffOnDuty: number
  status: StoreStatus
}

/** 实时监控总览数据 */
export interface LiveMonitorOverview {
  storeCount: number
  onlineStores: number
  todayOrders: number
  todayRevenue: number
  totalStaff: number
  avgTurnover: number
}

/** 时间维度 */
export type TrendDimension = '24h' | '7d' | '30d'

/** 趋势数据 */
export interface TrendData {
  dimension: TrendDimension
  dates: string[]
  revenue: number[]
  orders: number[]
}

/** 告警级别 */
export type AlertLevel = 'error' | 'warning' | 'info'

/** 实时告警条目 */
export interface AlertItem {
  id: string
  level: AlertLevel
  storeName: string
  message: string
  time: string
}

// ============================================================
// 后端响应接口（Map<String, Object> 的约定结构）
// ============================================================

interface OverviewBackend {
  storeCount?: number
  onlineStores?: number
  todayOrders?: number
  todayRevenue?: number
  totalStaff?: number
  avgTurnover?: number
}

interface StoreBackend {
  storeName?: string
  name?: string
  todayRevenue?: number
  revenue?: number
  yesterdayRevenue?: number
  orderCount?: number
  orders?: number
  dineIn?: number
  takeaway?: number
  selfPickup?: number
  turnoverRate?: number
  staffOnDuty?: number
  status?: string
}

interface TrendBackend {
  dimension?: string
  dates?: string[]
  revenue?: number[]
  orders?: number[]
}

interface AlertBackend {
  id?: string | number
  level?: string
  storeName?: string
  message?: string
  time?: string
}

// ============================================================
// 字段适配器
// ============================================================

/** 后端门店数据 → 前端 StoreRecord（兼容字段名差异） */
function adaptStore(raw: StoreBackend): StoreRecord {
  const status = raw.status as StoreStatus
  return {
    storeName: raw.storeName || raw.name || '未知门店',
    todayRevenue: Number(raw.todayRevenue ?? raw.revenue ?? 0),
    yesterdayRevenue: Number(raw.yesterdayRevenue ?? 0),
    orderCount: Number(raw.orderCount ?? raw.orders ?? 0),
    dineIn: Number(raw.dineIn ?? 0),
    takeaway: Number(raw.takeaway ?? 0),
    selfPickup: Number(raw.selfPickup ?? 0),
    turnoverRate: Number(raw.turnoverRate ?? 0),
    staffOnDuty: Number(raw.staffOnDuty ?? 0),
    status: status === 'inactive' ? 'inactive' : 'active',
  }
}

/** 后端总览 → 前端 LiveMonitorOverview（防御性读取） */
function adaptOverview(raw: OverviewBackend | null | undefined): LiveMonitorOverview {
  return {
    storeCount: Number(raw?.storeCount ?? 0),
    onlineStores: Number(raw?.onlineStores ?? 0),
    todayOrders: Number(raw?.todayOrders ?? 0),
    todayRevenue: Number(raw?.todayRevenue ?? 0),
    totalStaff: Number(raw?.totalStaff ?? 0),
    avgTurnover: Number(raw?.avgTurnover ?? 0),
  }
}

/** 后端趋势数据 → 前端 TrendData（防御性读取） */
function adaptTrend(raw: TrendBackend | null | undefined, dimension: TrendDimension): TrendData {
  const dim = (raw?.dimension as TrendDimension) || dimension
  return {
    dimension: dim,
    dates: Array.isArray(raw?.dates) ? raw.dates.map(String) : [],
    revenue: Array.isArray(raw?.revenue) ? raw.revenue.map(Number) : [],
    orders: Array.isArray(raw?.orders) ? raw.orders.map(Number) : [],
  }
}

/** 后端告警 → 前端 AlertItem（防御性读取） */
function adaptAlert(raw: AlertBackend): AlertItem {
  const level = raw.level as AlertLevel
  return {
    id: String(raw.id ?? ''),
    level: ['error', 'warning', 'info'].includes(level) ? level : 'info',
    storeName: raw.storeName || '未知门店',
    message: raw.message || '',
    time: raw.time || '',
  }
}

// ============================================================
// API 实现
// ============================================================

export const liveMonitorApi = {
  /**
   * 获取实时监控总览数据
   * GET /v1/operations/live-monitor/overview
   */
  async getOverview(): Promise<LiveMonitorOverview> {
    const res = await get<OverviewBackend>('/v1/operations/live-monitor/overview')
    return adaptOverview(res)
  },

  /**
   * 获取门店实时状态列表
   * GET /v1/operations/live-monitor/stores
   */
  async getStores(): Promise<StoreRecord[]> {
    const res = await get<StoreBackend[]>('/v1/operations/live-monitor/stores')
    return Array.isArray(res) ? res.map(adaptStore) : []
  },

  /**
   * 获取趋势数据
   * GET /v1/operations/live-monitor/trend?dimension=24h|7d|30d
   * @param dimension 时间维度：24h / 7d / 30d
   */
  async getTrend(dimension: TrendDimension = '24h'): Promise<TrendData> {
    const res = await get<TrendBackend>('/v1/operations/live-monitor/trend', { dimension })
    return adaptTrend(res, dimension)
  },

  /**
   * 获取实时告警列表
   * GET /v1/operations/live-monitor/alerts
   */
  async getAlerts(): Promise<AlertItem[]> {
    const res = await get<AlertBackend[]>('/v1/operations/live-monitor/alerts')
    return Array.isArray(res) ? res.map(adaptAlert) : []
  },
}

export default liveMonitorApi
