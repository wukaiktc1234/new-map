/**
 * 采购数据分析 API + DataConverter
 * 对应后端: /v1/purchase/analysis
 *
 * 设计说明（2026-06-29）：
 * - 后端 PurchaseAnalysisService 复用 PurchaseReport 系列 VO，
 *   因此前端也复用 purchaseReportConverter 完成数据转换（金额分↔元、ID 数字↔字符串、null 安全降级）
 * - 移除所有 silentGet 调用与硬编码 Mock fallback
 * - 仅按日期范围聚合，不支持 supplierId/category 过滤
 */
import { get } from '../request'
import { purchaseReportConverter } from './report'

import type {
  PurchaseAnalysisTrendItem,
  PurchaseAnalysisSupplierItem,
  PurchaseAnalysisCategoryItem,
  PurchaseAnalysisSummary,
  PurchaseAnalysisQueryForm,
} from '@/types/purchase-analysis'

// 后端原始类型直接复用 report.ts 中的同名 Backend 类型（已导出）
import type {
  PurchaseReportSummaryBackend,
  PurchaseReportMonthlyItemBackend,
  PurchaseReportSupplierItemBackend,
  PurchaseReportCategoryItemBackend,
} from './report'

// ============================================================
// DataConverter（复用 purchaseReportConverter，提供语义化别名）
// ============================================================

export const purchaseAnalysisConverter = {
  /** 趋势项转换：后端 VO（分）→ 前端类型（元） */
  toTrendItem: purchaseReportConverter.toMonthlyItem,

  /** 供应商项转换 */
  toSupplierItem: purchaseReportConverter.toSupplierItem,

  /** 品类项转换 */
  toCategoryItem: purchaseReportConverter.toCategoryItem,

  /** 汇总转换 */
  toSummary: purchaseReportConverter.toSummary,

  /** 格式化金额（元，带千分位与两位小数） */
  formatYuan: purchaseReportConverter.formatYuan,

  /** 格式化百分比（保留一位小数） */
  formatPercent: purchaseReportConverter.formatPercent,
}

// ============================================================
// API
// ============================================================

export const purchaseAnalysisApi = {
  /** 采购趋势分析（按月份聚合） */
  async getTrend(params?: PurchaseAnalysisQueryForm): Promise<PurchaseAnalysisTrendItem[]> {
    const res = await get<PurchaseReportMonthlyItemBackend[] | null>('/v1/purchase/analysis/trend', {
      startDate: params?.startDate,
      endDate: params?.endDate,
    });
    return (res ?? []).map(purchaseAnalysisConverter.toTrendItem);
  },

  /** 供应商采购占比分析 */
  async getSupplier(params?: PurchaseAnalysisQueryForm): Promise<PurchaseAnalysisSupplierItem[]> {
    const res = await get<PurchaseReportSupplierItemBackend[] | null>('/v1/purchase/analysis/supplier', {
      startDate: params?.startDate,
      endDate: params?.endDate,
    });
    return (res ?? []).map(purchaseAnalysisConverter.toSupplierItem);
  },

  /** 品类采购分布分析 */
  async getCategory(params?: PurchaseAnalysisQueryForm): Promise<PurchaseAnalysisCategoryItem[]> {
    const res = await get<PurchaseReportCategoryItemBackend[] | null>('/v1/purchase/analysis/category', {
      startDate: params?.startDate,
      endDate: params?.endDate,
    });
    return (res ?? []).map(purchaseAnalysisConverter.toCategoryItem);
  },

  /** 综合汇总指标 */
  async getSummary(params?: PurchaseAnalysisQueryForm): Promise<PurchaseAnalysisSummary> {
    const res = await get<PurchaseReportSummaryBackend | null>('/v1/purchase/analysis/summary', {
      startDate: params?.startDate,
      endDate: params?.endDate,
    });
    return purchaseAnalysisConverter.toSummary(res ?? {} as PurchaseReportSummaryBackend);
  },
}

export default purchaseAnalysisApi
