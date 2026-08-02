/**
 * 运营总览 API
 * 对应后端: OperationsDashboardController (/v1/operations/dashboard)
 *
 * 对接策略：
 * - 所有方法调用真实后端 API，错误由 request 拦截器统一处理
 * - 后端返回 Map<String, Object>，结构灵活，前端按约定字段读取并做防御性处理
 *
 * 后端端点：
 * - GET /v1/operations/dashboard/stats  多店监控统计卡片（含 KPI、健康度、事件、预警摘要）
 * - GET /v1/operations/dashboard/stores 门店绩效列表（分页）
 */
import { get } from '@/api/request'

// ============================================================
// 类型定义（与 OperationsDashboard.vue 内部类型保持一致）
// ============================================================

/** 健康度指标 */
export interface HealthMetric {
  label: string
  value: number
  target: number
  unit: string
  status: 'good' | 'warning' | 'error'
}

/** 时间线事件 */
export interface TimelineEvent {
  date: string
  title: string
  type: 'milestone' | 'alert' | 'info' | 'success'
  description?: string
}

/** 预警摘要 */
export interface AlertSummary {
  id: string
  level: 'error' | 'warning' | 'info'
  storeName: string
  message: string
  time: string
}

/** 门店快照状态 */
export type StoreStatus = 'normal' | 'inactive' | 'warning'

/** 门店快照数据 */
export interface StoreSnapshot {
  name: string
  revenue: number
  orders: number
  status: StoreStatus
}

/** 看板统计数据（KPI + 健康度 + 事件 + 预警摘要） */
export interface DashboardStats {
  todayRevenue: number
  dayOverDayChange: number
  todayOrders: number
  activeStores: number
  avgTurnover: number
  targetProgress: number
  healthMetrics: HealthMetric[]
  weekEvents: TimelineEvent[]
  alertSummaries: AlertSummary[]
}

/** 门店绩效分页结果 */
export interface StorePageResult {
  records: StoreSnapshot[]
  total: number
  current?: number
  size?: number
}

// ============================================================
// 后端响应接口（Map<String, Object> 的约定结构）
// ============================================================

interface DashboardStatsBackend {
  // 后端实际返回字段（OperationsDashboardDataServiceImpl.getDashboardStats）
  todayRevenue?: number | string  // 后端返回元为单位的字符串（如 "1234.56"）
  todayOrderCount?: number        // 后端字段名为 todayOrderCount（非 todayOrders）
  activeStores?: number
  avgTableTurnover?: number | string  // 后端字段名为 avgTableTurnover（非 avgTurnover）
  onDutyStaff?: number
  customerSatisfaction?: number
  queryDate?: string
  hasFullAccess?: boolean
  // 以下字段后端暂未实现，预留兼容
  dayOverDayChange?: number
  todayOrders?: number            // 兼容旧字段名
  avgTurnover?: number | string   // 兼容旧字段名
  targetProgress?: number
  healthMetrics?: HealthMetric[]
  weekEvents?: TimelineEvent[]
  alertSummaries?: AlertSummary[]
}

interface StoreBackend {
  name?: string
  storeName?: string
  revenue?: number | string
  totalRevenue?: number | string  // 后端 getStorePerformance 返回 totalRevenue
  orders?: number
  orderCount?: number             // 后端返回 orderCount
  todayOrders?: number
  todayRevenue?: number | string
  status?: number | string        // 后端返回数字（1营业/0停业/2警告）
}

interface PageResult<T> {
  records?: T[]
  total?: number
  current?: number
  size?: number
}

// ============================================================
// 字段适配器
// ============================================================

/** 后端门店状态（数字）→ 前端语义化状态 */
function adaptStoreStatus(status: number | string | undefined): StoreStatus {
  // 后端 stores_new.status：1=营业中 0=停业 2=警告/其他
  // 前端 StoreStatus：'normal' | 'inactive' | 'warning'
  if (status === 'normal' || status === 'inactive' || status === 'warning') {
    return status as StoreStatus
  }
  const numStatus = Number(status)
  if (numStatus === 1) return 'normal'
  if (numStatus === 0) return 'inactive'
  if (numStatus === 2) return 'warning'
  return 'normal'
}

/** 后端门店数据 → 前端 StoreSnapshot（兼容字段名差异） */
function adaptStore(raw: StoreBackend): StoreSnapshot {
  return {
    name: raw.name || raw.storeName || '未知门店',
    // 后端返回 totalRevenue（元字符串）或 revenue（数字）
    revenue: Number(raw.revenue ?? raw.totalRevenue ?? raw.todayRevenue ?? 0),
    // 后端返回 orderCount 或 orders
    orders: Number(raw.orders ?? raw.orderCount ?? raw.todayOrders ?? 0),
    status: adaptStoreStatus(raw.status),
  }
}

/** 后端看板统计 → 前端 DashboardStats（防御性读取 + 字段名兼容） */
function adaptStats(raw: DashboardStatsBackend | null | undefined): DashboardStats {
  return {
    // 后端 todayRevenue 返回字符串（如 "1234.56"），需转为数字
    todayRevenue: Number(raw?.todayRevenue ?? 0),
    dayOverDayChange: Number(raw?.dayOverDayChange ?? 0),
    // 后端字段名为 todayOrderCount，兼容旧字段名 todayOrders
    todayOrders: Number(raw?.todayOrders ?? raw?.todayOrderCount ?? 0),
    activeStores: Number(raw?.activeStores ?? 0),
    // 后端字段名为 avgTableTurnover（字符串如 "2.50"），兼容旧字段名 avgTurnover
    avgTurnover: Number(raw?.avgTurnover ?? raw?.avgTableTurnover ?? 0),
    targetProgress: Number(raw?.targetProgress ?? 0),
    // 后端暂未实现以下字段，使用空数组兜底
    healthMetrics: Array.isArray(raw?.healthMetrics) ? raw.healthMetrics : [],
    weekEvents: Array.isArray(raw?.weekEvents) ? raw.weekEvents : [],
    alertSummaries: Array.isArray(raw?.alertSummaries) ? raw.alertSummaries : [],
  }
}

// ============================================================
// API 实现
// ============================================================

export const dashboardApi = {
  /**
   * 获取多店监控统计卡片数据
   * GET /v1/operations/dashboard/stats
   * 包含 KPI、健康度指标、本周事件、预警摘要
   */
  async getDashboardStats(): Promise<DashboardStats> {
    const res = await get<DashboardStatsBackend>('/v1/operations/dashboard/stats')
    return adaptStats(res)
  },

  /**
   * 获取门店绩效列表
   * GET /v1/operations/dashboard/stores?page&size
   * @param page 页码（默认 1）
   * @param size 每页大小（默认 10）
   */
  async getStoreList(page = 1, size = 100): Promise<StorePageResult> {
    const res = await get<PageResult<StoreBackend>>('/v1/operations/dashboard/stores', { page, size })
    const records = (res?.records || []).map(adaptStore)
    return {
      records,
      total: res?.total || records.length,
      current: res?.current || page,
      size: res?.size || size,
    }
  },
}

export default dashboardApi
