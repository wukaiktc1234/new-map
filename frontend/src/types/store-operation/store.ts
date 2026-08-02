import type { PageResponse, PageParams } from './common'

/** 门店运营状态 */
export type StoreStatus = 'running' | 'paused' | 'closed'

/** 后端门店数据结构（金额为分，状态为数字编码） */
export interface StoreBackend {
  storeId: string
  storeName: string
  address: string
  phone: string
  status: number
  openTime: string
  closeTime: string
  managerId: string
  managerName: string
  todayRevenue: number
  orderCount: number
  dineInCount: number
  takeoutCount: number
  pickupCount?: number
  tableUsageRate: string
  onDutyStaff: number
}

/** 门店基本信息 */
export interface StoreBasicInfo {
  storeId: string
  storeName: string
  address: string
  phone: string
  status: StoreStatus
  openTime: string
  closeTime: string
  managerId: string
  managerName: string
}

/** 门店列表项（含今日运营数据） */
export interface StoreListItem extends StoreBasicInfo {
  todayRevenue: number
  orderCount: number
  dineInCount: number
  takeoutCount: number
  pickupCount: number
  tableUsageRate: string
  onDutyStaff: number
}

/** 门店统计概览 */
export interface StoreOverviewStats {
  activeStores: number
  todayRevenue: number
  todayOrderCount: number
  onDutyStaff: number
}

/** 图表数据 - 营收对比（柱状图） */
export interface RevenueChartData {
  stores: string[]
  revenues: number[]
}

/** 图表数据 - 订单来源分布（饼图） */
export interface OrderSourceData {
  dineIn: number
  takeout: number
  pickup: number
}

/** 图表数据 - 翻台率趋势（折线图） */
export interface TableUsageTrendData {
  dates: string[]
  rates: number[]
}

/** 完整图表数据集合 */
export interface StoreChartData {
  revenueComparison: RevenueChartData
  orderSourceDistribution: OrderSourceData
  tableUsageTrend: TableUsageTrendData
}

/** 门店查询参数 */
export interface StoreListParams extends PageParams {
  status?: StoreStatus
  keyword?: string
}
