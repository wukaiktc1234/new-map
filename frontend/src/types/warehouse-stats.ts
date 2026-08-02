/**
 * 库存统计与分析类型定义
 * 对齐后端: InventoryStats + InventoryAnalysis相关实体
 */

/** 统计趋势查询参数 */
export interface StatsTrendQueryForm {
  startDate?: string
  endDate?: string
  warehouseId?: string
  granularity?: 'day' | 'week' | 'month'
}

/** 统计概览 */
export interface InventoryStatsOverview {
  totalItems: number
  totalQuantity: number
  totalValue: string
  warningCount: number
  outOfStockCount: number
  expiringCount: number
  warehouseCount: number
  turnoverRate: number
}

/** 库存趋势项 */
export interface InventoryTrendItem {
  date: string
  totalQuantity: number
  totalValue: string
  inQuantity: number
  outQuantity: number
}

/** 分类统计项 */
export interface InventoryCategoryStatItem {
  categoryName: string
  quantity: number
  value: string
  percentage: number
}

/** 消耗趋势项 */
export interface ConsumptionTrendItem {
  date: string
  quantity: number
  amount: string
}

/** 预警趋势项 */
export interface WarningTrendItem {
  date: string
  warningCount: number
  handledCount: number
  unhandledCount: number
}

/** 预警级别分布 */
export interface WarningLevelDistribution {
  lowStock: number
  highStock: number
  expiringSoon: number
  expired: number
}

/** 分析概览 */
export interface InventoryAnalysisOverview {
  totalValue: string
  turnoverRate: number
  wasteRate: number
  stockoutRate: number
  avgInventoryDays: number
  abcARatio: number
  abcBRatio: number
  abcCRatio: number
}

/** 分析报表 */
export interface InventoryAnalysisReport {
  reportId: string
  reportDate: string
  totalValue: string
  turnoverRate: number
  wasteRate: number
  stockoutRate: number
  summary: string
  createTime: string
}

/** 分析报表查询参数 */
export interface InventoryAnalysisQueryForm {
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

/** 周转率分析项 */
export interface TurnoverAnalysisItem {
  materialId: string
  materialName: string
  category: string
  turnoverRate: number
  avgInventoryDays: number
  salesQuantity: number
  avgInventory: number
}

/** ABC分类项 */
export interface ABCClassificationItem {
  materialId: string
  materialName: string
  category: 'A' | 'B' | 'C'
  totalValue: string
  valuePercentage: number
  quantityPercentage: number
}

/** 报损分析项 */
export interface WasteAnalysisItem {
  materialId: string
  materialName: string
  lossType: string
  lossQuantity: number
  lossAmount: string
  lossRate: number
}

/** 缺货风险项 */
export interface StockoutRiskItem {
  materialId: string
  materialName: string
  currentStock: number
  safetyStock: number
  avgDailyConsumption: number
  daysUntilStockout: number
  riskLevel: 'high' | 'medium' | 'low'
}

/**
 * 统计卡片颜色类型（从 @/types/stat 统一导出，避免重复定义）
 *
 * 严格对齐 StatCard 组件 Props.colorType 的可选值，
 * 用于 computed statistics 数组中 colorType 字段的类型断言，
 * 替代 `as const`（字面量类型推断导致 StatCard Props 类型不兼容）。
 */
export type { StatColorType } from '@/types/stat'

/**
 * 仓库成本分析项（库存报表 Tab3：成本分析）
 *
 * 用于 InventoryReport.vue 的成本分析数据展示，
 * 由仓库维度的库存金额、占比、品项数、周转率构成。
 */
export interface WarehouseCostItem {
  /** 仓库名称 */
  warehouseName: string
  /** 库存金额（元，字符串格式，含千分位） */
  totalValue: string
  /** 占比（百分比，0-100） */
  percentage: number
  /** 品项数 */
  itemCount: number
  /** 平均周转率 */
  turnoverRate: number
}
