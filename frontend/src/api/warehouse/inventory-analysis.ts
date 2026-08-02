/**
 * 库存分析API
 * 对应后端: /v1/analytics/inventory
 */
import { get, post } from '@/api/request'
import { inventoryAnalysisConverter } from './converters'
import type {
  InventoryAnalysisOverview,
  InventoryAnalysisReport,
  InventoryAnalysisQueryForm,
  TurnoverAnalysisItem,
  ABCClassificationItem,
  WasteAnalysisItem,
  StockoutRiskItem,
} from '@/types/warehouse-stats'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// ============================================================
// 后端类型定义（金额字段后端为分，前端为元字符串）
// ============================================================

/** 分析概览后端类型（totalValue 为分） */
interface InventoryAnalysisOverviewBackend {
  totalValue: number
  turnoverRate: number
  wasteRate: number
  stockoutRate: number
  avgInventoryDays: number
  abcARatio: number
  abcBRatio: number
  abcCRatio: number
}

/** 分析报表后端类型（totalValue 为分） */
interface InventoryAnalysisReportBackend {
  reportId: string
  reportDate: string
  totalValue: number
  turnoverRate: number
  wasteRate: number
  stockoutRate: number
  summary: string
  createTime: string
}

/** 周转率分析后端类型 */
interface TurnoverAnalysisItemBackend {
  materialId: string
  materialName: string
  category: string
  turnoverRate: number
  avgInventoryDays: number
  salesQuantity: number
  avgInventory: number
}

/** ABC分类后端类型（totalValue 为分） */
interface ABCClassificationItemBackend {
  materialId: string
  materialName: string
  category: 'A' | 'B' | 'C'
  totalValue: number
  valuePercentage: number
  quantityPercentage: number
}

/** 报损分析后端类型（lossAmount 为分） */
interface WasteAnalysisItemBackend {
  materialId: string
  materialName: string
  lossType: string
  lossQuantity: number
  lossAmount: number
  lossRate: number
}

/** 缺货风险后端类型 */
interface StockoutRiskItemBackend {
  materialId: string
  materialName: string
  currentStock: number
  safetyStock: number
  avgDailyConsumption: number
  daysUntilStockout: number
  riskLevel: 'high' | 'medium' | 'low'
}

/** 仓库库存价值后端类型（totalValue 为分） */
interface WarehouseValueBackend {
  warehouseId: string
  warehouseName: string
  totalValue: number
}

// ============================================================
// 数据转换函数
// ============================================================

/** 后端分析概览 → 前端（totalValue: 分→元字符串） */
function toFrontendOverview(backend: InventoryAnalysisOverviewBackend): InventoryAnalysisOverview {
  return {
    totalValue: inventoryAnalysisConverter.toYuan(backend.totalValue).toFixed(2),
    turnoverRate: backend.turnoverRate,
    wasteRate: backend.wasteRate,
    stockoutRate: backend.stockoutRate,
    avgInventoryDays: backend.avgInventoryDays,
    abcARatio: backend.abcARatio,
    abcBRatio: backend.abcBRatio,
    abcCRatio: backend.abcCRatio,
  }
}

/** 后端分析报表 → 前端（totalValue: 分→元字符串） */
function toFrontendReport(backend: InventoryAnalysisReportBackend): InventoryAnalysisReport {
  return {
    reportId: backend.reportId,
    reportDate: backend.reportDate,
    totalValue: inventoryAnalysisConverter.toYuan(backend.totalValue).toFixed(2),
    turnoverRate: backend.turnoverRate,
    wasteRate: backend.wasteRate,
    stockoutRate: backend.stockoutRate,
    summary: backend.summary,
    createTime: backend.createTime,
  }
}

/** 后端ABC分类 → 前端（totalValue: 分→元字符串） */
function toFrontendABC(backend: ABCClassificationItemBackend): ABCClassificationItem {
  return {
    materialId: backend.materialId,
    materialName: backend.materialName,
    category: backend.category,
    totalValue: inventoryAnalysisConverter.toYuan(backend.totalValue).toFixed(2),
    valuePercentage: backend.valuePercentage,
    quantityPercentage: backend.quantityPercentage,
  }
}

/** 后端报损分析 → 前端（lossAmount: 分→元字符串） */
function toFrontendWaste(backend: WasteAnalysisItemBackend): WasteAnalysisItem {
  return {
    materialId: backend.materialId,
    materialName: backend.materialName,
    lossType: backend.lossType,
    lossQuantity: backend.lossQuantity,
    lossAmount: inventoryAnalysisConverter.toYuan(backend.lossAmount).toFixed(2),
    lossRate: backend.lossRate,
  }
}

/** 后端仓库库存价值 → 前端（totalValue: 分→元字符串） */
function toFrontendWarehouseValue(backend: WarehouseValueBackend): Record<string, unknown> {
  return {
    warehouseId: backend.warehouseId,
    warehouseName: backend.warehouseName,
    totalValue: inventoryAnalysisConverter.toYuan(backend.totalValue).toFixed(2),
  }
}

// ============================================================
// API 定义
// ============================================================

export const inventoryAnalysisApi = {
  /** 获取分析概览 */
  async getOverview(): Promise<InventoryAnalysisOverview> {
    const res = await get<InventoryAnalysisOverviewBackend>('/v1/analytics/inventory/overview')
    return toFrontendOverview(res)
  },

  /** 生成分析报表 */
  async generateReport(date: string): Promise<InventoryAnalysisReport> {
    const res = await post<InventoryAnalysisReportBackend>(`/v1/analytics/inventory/report`, undefined, {
      params: { date },
    })
    return toFrontendReport(res)
  },

  /** 获取周转率分析 */
  async getTurnoverAnalysis(startDate: string, endDate: string): Promise<TurnoverAnalysisItem[]> {
    const res = await get<TurnoverAnalysisItemBackend[]>('/v1/analytics/inventory/turnover-analysis', { startDate, endDate })
    return (res || []).map(item => item as TurnoverAnalysisItem)
  },

  /** 获取ABC分类 */
  async getABCClassification(): Promise<ABCClassificationItem[]> {
    const res = await get<ABCClassificationItemBackend[]>('/v1/analytics/inventory/abc-classification')
    return (res || []).map(toFrontendABC)
  },

  /** 获取报损分析 */
  async getWasteAnalysis(startDate: string, endDate: string): Promise<WasteAnalysisItem[]> {
    const res = await get<WasteAnalysisItemBackend[]>('/v1/analytics/inventory/waste-analysis', { startDate, endDate })
    return (res || []).map(toFrontendWaste)
  },

  /** 获取缺货风险列表 */
  async getStockoutRiskList(): Promise<StockoutRiskItem[]> {
    const res = await get<StockoutRiskItemBackend[]>('/v1/analytics/inventory/stockout-risk')
    return (res || []).map(item => item as StockoutRiskItem)
  },

  /** 获取仓库库存价值 */
  async getWarehouseValue(): Promise<Record<string, unknown>[]> {
    const res = await get<WarehouseValueBackend[]>('/v1/analytics/inventory/warehouse-value')
    return (res || []).map(toFrontendWarehouseValue)
  },

  /** 查询报表列表 */
  async queryReports(params?: InventoryAnalysisQueryForm): Promise<PageResult<InventoryAnalysisReport>> {
    const res = await post<PageResult<InventoryAnalysisReportBackend>>('/v1/analytics/inventory/reports/query', params)
    return {
      records: (res?.records || []).map(toFrontendReport),
      total: res?.total || 0,
      current: res?.current || params?.page || 1,
      size: res?.size || params?.size || 10,
      pages: res?.pages || 0,
    }
  },

  /** 获取报表详情 */
  async getReportById(reportId: string): Promise<InventoryAnalysisReport> {
    const res = await get<InventoryAnalysisReportBackend>(`/v1/analytics/inventory/reports/${reportId}`)
    return toFrontendReport(res)
  },
}
