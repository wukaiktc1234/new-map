/**
 * 库存统计API
 * 对应后端: /v1/inventory/stats
 */
import { get } from '@/api/request'
import { inventoryStatsConverter } from './converters'
import type {
  InventoryStatsOverview,
  InventoryTrendItem,
  InventoryCategoryStatItem,
  ConsumptionTrendItem,
  WarningTrendItem,
  WarningLevelDistribution,
  StatsTrendQueryForm,
} from '@/types/warehouse-stats'

// ============================================================
// 后端类型定义（金额字段后端为分，前端为元字符串）
// ============================================================

/** 统计概览后端类型（totalValue 为分） */
interface InventoryStatsOverviewBackend {
  totalItems: number
  totalQuantity: number
  totalValue: number
  warningCount: number
  outOfStockCount: number
  expiringCount: number
  warehouseCount: number
  turnoverRate: number
}

/** 库存趋势后端类型（totalValue 为分） */
interface InventoryTrendItemBackend {
  date: string
  totalQuantity: number
  totalValue: number
  inQuantity: number
  outQuantity: number
}

/** 分类统计后端类型（value 为分） */
interface InventoryCategoryStatItemBackend {
  categoryName: string
  quantity: number
  value: number
  percentage: number
}

/** 消耗趋势后端类型（amount 为分） */
interface ConsumptionTrendItemBackend {
  date: string
  quantity: number
  amount: number
}

// ============================================================
// 数据转换函数
// ============================================================

/** 将前端趋势查询参数映射为后端参数名（startDate→startTime, endDate→endTime, granularity→type） */
function mapTrendParams(params?: StatsTrendQueryForm): Record<string, unknown> {
  if (!params) return {}
  const backendParams: Record<string, unknown> = {}
  if (params.startDate) backendParams.startTime = params.startDate
  if (params.endDate) backendParams.endTime = params.endDate
  if (params.granularity) backendParams.type = params.granularity
  if (params.warehouseId) backendParams.warehouseId = params.warehouseId
  return backendParams
}

/** 后端统计概览 → 前端（totalValue: 分→元字符串） */
function toFrontendOverview(backend: InventoryStatsOverviewBackend): InventoryStatsOverview {
  return {
    totalItems: backend.totalItems,
    totalQuantity: backend.totalQuantity,
    totalValue: inventoryStatsConverter.toYuan(backend.totalValue).toFixed(2),
    warningCount: backend.warningCount,
    outOfStockCount: backend.outOfStockCount,
    expiringCount: backend.expiringCount,
    warehouseCount: backend.warehouseCount,
    turnoverRate: backend.turnoverRate,
  }
}

/** 后端库存趋势 → 前端（totalValue: 分→元字符串） */
function toFrontendTrendItem(backend: InventoryTrendItemBackend): InventoryTrendItem {
  return {
    date: backend.date,
    totalQuantity: backend.totalQuantity,
    totalValue: inventoryStatsConverter.toYuan(backend.totalValue).toFixed(2),
    inQuantity: backend.inQuantity,
    outQuantity: backend.outQuantity,
  }
}

/** 后端分类统计 → 前端（value: 分→元字符串） */
function toFrontendCategoryStat(backend: InventoryCategoryStatItemBackend): InventoryCategoryStatItem {
  return {
    categoryName: backend.categoryName,
    quantity: backend.quantity,
    value: inventoryStatsConverter.toYuan(backend.value).toFixed(2),
    percentage: backend.percentage,
  }
}

/** 后端消耗趋势 → 前端（amount: 分→元字符串） */
function toFrontendConsumptionTrend(backend: ConsumptionTrendItemBackend): ConsumptionTrendItem {
  return {
    date: backend.date,
    quantity: backend.quantity,
    amount: inventoryStatsConverter.toYuan(backend.amount).toFixed(2),
  }
}

// ============================================================
// API 定义
// ============================================================

export const inventoryStatsApi = {
  /** 获取统计概览 */
  async getOverview(): Promise<InventoryStatsOverview> {
    const res = await get<InventoryStatsOverviewBackend>('/v1/inventory/stats/overview')
    return toFrontendOverview(res)
  },

  /** 获取库存趋势 */
  async getTrend(params?: StatsTrendQueryForm): Promise<InventoryTrendItem[]> {
    const res = await get<InventoryTrendItemBackend[]>('/v1/inventory/stats/trend', mapTrendParams(params))
    return (res || []).map(toFrontendTrendItem)
  },

  /** 获取分类统计 */
  async getCategoryStats(warehouseId?: string): Promise<InventoryCategoryStatItem[]> {
    const res = await get<InventoryCategoryStatItemBackend[]>('/v1/inventory/stats/category', { warehouseId })
    return (res || []).map(toFrontendCategoryStat)
  },

  /** 获取消耗趋势 */
  async getConsumptionTrend(params?: StatsTrendQueryForm): Promise<ConsumptionTrendItem[]> {
    const res = await get<ConsumptionTrendItemBackend[]>('/v1/inventory/stats/consumption-trend', mapTrendParams(params))
    return (res || []).map(toFrontendConsumptionTrend)
  },

  /** 获取同比环比数据 */
  async getComparison(): Promise<Record<string, unknown>> {
    return await get<Record<string, unknown>>('/v1/inventory/stats/comparison')
  },

  /** 获取预警趋势 */
  async getWarningTrend(params?: StatsTrendQueryForm): Promise<WarningTrendItem[]> {
    const res = await get<WarningTrendItem[]>('/v1/inventory/stats/warning-trend', mapTrendParams(params))
    return res || []
  },

  /** 获取预警级别分布 */
  async getWarningLevelDistribution(): Promise<WarningLevelDistribution> {
    const res = await get<WarningLevelDistribution>('/v1/inventory/stats/warning-level-distribution')
    return res
  },

  /** 获取预警状态分布 */
  async getWarningStatusDistribution(): Promise<Record<string, unknown>> {
    return await get<Record<string, unknown>>('/v1/inventory/stats/warning-status-distribution')
  },
}
