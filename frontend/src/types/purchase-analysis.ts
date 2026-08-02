/**
 * 采购数据分析相关类型定义
 *
 * 与 PurchaseReport 系列类型相对应，用于"分析视角"页面。
 * 复用 PurchaseReport 系列 VO（后端 PurchaseAnalysisService 直接返回这些 VO），
 * 因此前端也复用 types/purchase-report.ts 中的对应数据项类型。
 *
 * 本文件仅定义：
 * - 查询表单类型（PurchaseAnalysisQueryForm）
 * - 为可读性重新导出的别名类型（PurchaseAnalysisTrendItem / SupplierItem / CategoryItem / Summary）
 */

import type {
  PurchaseReportMonthlyItem,
  PurchaseReportSupplierItem,
  PurchaseReportCategoryItem,
  PurchaseReportSummary,
} from './purchase-report'

/**
 * 采购分析查询参数（仅支持日期范围过滤）
 */
export interface PurchaseAnalysisQueryForm {
  /** 开始日期（yyyy-MM-dd，可选） */
  startDate?: string;
  /** 结束日期（yyyy-MM-dd，可选） */
  endDate?: string;
}

/**
 * 采购趋势分析数据项（按月份聚合）
 * 复用 PurchaseReportMonthlyItem
 */
export type PurchaseAnalysisTrendItem = PurchaseReportMonthlyItem;

/**
 * 供应商采购占比分析数据项
 * 复用 PurchaseReportSupplierItem
 */
export type PurchaseAnalysisSupplierItem = PurchaseReportSupplierItem;

/**
 * 品类采购分布分析数据项
 * 复用 PurchaseReportCategoryItem
 */
export type PurchaseAnalysisCategoryItem = PurchaseReportCategoryItem;

/**
 * 综合汇总指标
 * 复用 PurchaseReportSummary
 */
export type PurchaseAnalysisSummary = PurchaseReportSummary;
