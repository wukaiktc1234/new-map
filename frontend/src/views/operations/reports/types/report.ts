/**
 * 经营报表模块类型定义（管理层视角 - 精简版）
 */

// ============================================================
// 基础枚举
// ============================================================

export type ReportTab = 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly' | 'profit'
export type CompareType = 'none' | 'mom' | 'yoy'
export type ChannelType = 'all' | 'dine_in' | 'takeout' | 'self_pickup'

/** 报表类型（1=日报 2=周报 3=月报 4=季报 5=年报 6=利润分析） */
export type ReportType = 1 | 2 | 3 | 4 | 5 | 6

// ============================================================
// 查询参数
// ============================================================

export interface OperationsReportQueryParams {
  startDate: string
  endDate: string
  storeIds?: string[]
  channel?: ChannelType
  compareType?: CompareType
}

// ============================================================
// KPI卡片数据
// ============================================================

export interface KpiCardData {
  key: string
  label: string
  value: string | number
  unit?: string
  yoy?: number
  mom?: number
  targetAchievement?: number
  colorType?: 'primary' | 'success' | 'warning' | 'error' | 'info'
}

export interface KpiSummary {
  period: string
  reportType: number
  kpiCards: Array<{
    metricKey: string
    metricName: string
    currentValue: string
    currentValueFen: number
    yoyChange: number
    momChange: number
    targetAchievement: number
    unit: string
    isCurrency: boolean
  }>
}

// ============================================================
// 门店对比数据
// ============================================================

export interface StoreComparisonItem {
  storeId: string
  storeName: string
  revenue: number
  revenueYoy: number
  revenueMom: number
  grossRate: number
  avgCheck: number
  avgCheckYoy: number
  orderCount: number
  perCapitaEfficiency: number
  perAreaEfficiency: number
  trend: number[]
}

export interface StoreComparisonData {
  stores: StoreComparisonItem[]
  summary: {
    totalRevenue: number
    totalStores: number
    avgGrossRate: number
    abnormalStoreCount: number
  }
}

// ============================================================
// 图表数据
// ============================================================

export interface TrendDataPoint {
  date: string
  revenue: number
  grossRate: number
}

export interface ChartData {
  trendData?: TrendDataPoint[]
  storeRank?: Array<{ name: string; value: number }>
  [key: string]: unknown
}

// ============================================================
// 导出任务
// ============================================================

export interface ExportTaskResponse {
  taskId: number
  taskNo: string
  status: number
}

export interface ExportTaskStatus {
  taskId: number
  taskNo: string
  status: number
  fileName?: string
  fileSize?: number
  rowCount?: number
  errorMessage?: string
  completedTime?: string
}
