/**
 * 采购报表 API + DataConverter
 * 对应后端: /v1/purchase/reports
 *
 * DataConverter 处理内容：
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - ID number↔string（避免大数精度丢失）
 * - 比率 BigDecimal(0~1) ↔ 百分比 number(0~100)
 * - null 安全降级（后端可能返回 null）
 */
import { get } from '../request'
// 金额转换统一委托给 utils/money
// - fenToYuanNumber 别名 fenToYuan：分→元 number，保留 2 位小数精度
// - yuanToFen 别名 utilsYuanToFen：标准元转分（避免与下面的 converter 方法同名）
import { yuanToFen as utilsYuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  PurchaseReportSummary,
  PurchaseReportMonthlyItem,
  PurchaseReportSupplierItem,
  PurchaseReportCategoryItem,
  PurchaseReportQueryForm,
} from '@/types/purchase-report'

// ============================================================
// 后端原始类型定义（导出供 analysis.ts 等复用）
// ============================================================

/** 后端返回的报表汇总 VO（金额字段单位：分） */
export interface PurchaseReportSummaryBackend {
  totalPurchaseAmount: number | null;
  totalOrderCount: number | null;
  totalSettledAmount: number | null;
  totalUnsettledAmount: number | null;
  avgOrderAmount: number | null;
  supplierCount: number | null;
  pendingOrders: number | null;
  onTimeDeliveryRate: number | null;  // BigDecimal, 0~1
  qualifiedRate: number | null;         // BigDecimal, 0~1
}

/** 后端返回的月度趋势 VO（金额字段单位：分） */
export interface PurchaseReportMonthlyItemBackend {
  month: string | null;
  purchaseAmount: number | null;
  orderCount: number | null;
  settledAmount: number | null;
  supplierCount: number | null;
}

/** 后端返回的供应商维度 VO（金额字段单位：分） */
export interface PurchaseReportSupplierItemBackend {
  supplierId: number | null;
  supplierName: string | null;
  totalAmount: number | null;
  orderCount: number | null;
  onTimeRate: number | null;       // BigDecimal, 0~100
  qualifiedRate: number | null;   // BigDecimal, 0~100
}

/** 后端返回的分类维度 VO（金额字段单位：分） */
export interface PurchaseReportCategoryItemBackend {
  categoryName: string | null;
  totalAmount: number | null;
  totalQuantity: number | null;  // BigDecimal
  orderCount: number | null;
}

// ============================================================
// 数据转换工具（金额转换统一委托给 utils/money，见文件顶部 import）
// ============================================================

/** 后端 BigDecimal 比率（0~1）→ 前端百分比 number（0~100） */
function rateToPercent(rate: number | null | undefined): number {
  if (rate == null) return 0;
  const num = Number(rate);
  // 兼容后端两种返回格式：0~1（小数）或 0~100（已是百分比）
  if (num > 1) return num;
  return num * 100;
}

/** ID number → string（避免大数精度丢失） */
function idToString(id: number | null | undefined): string {
  if (id == null) return '';
  return String(id);
}

/** null 安全降级为字符串 */
function safeStr(val: string | null | undefined): string {
  return val ?? '';
}

/** null 安全降级为数字 */
function safeNum(val: number | null | undefined): number {
  return val ?? 0;
}

/** null 安全降级为 BigDecimal number */
function safeBigNum(val: number | null | undefined): number {
  return val == null ? 0 : Number(val);
}

// ============================================================
// DataConverter
// ============================================================

export const purchaseReportConverter = {
  /** 汇总数据转换：后端 VO（分）→ 前端类型（元） */
  toSummary(b: PurchaseReportSummaryBackend): PurchaseReportSummary {
    return {
      totalPurchaseAmount: fenToYuan(b.totalPurchaseAmount),
      totalOrderCount: safeNum(b.totalOrderCount),
      totalSettledAmount: fenToYuan(b.totalSettledAmount),
      totalUnsettledAmount: fenToYuan(b.totalUnsettledAmount),
      avgOrderAmount: fenToYuan(b.avgOrderAmount),
      supplierCount: safeNum(b.supplierCount),
      pendingOrders: safeNum(b.pendingOrders),
      onTimeDeliveryRate: rateToPercent(b.onTimeDeliveryRate),
      qualifiedRate: rateToPercent(b.qualifiedRate),
    };
  },

  /** 月度趋势项转换 */
  toMonthlyItem(b: PurchaseReportMonthlyItemBackend): PurchaseReportMonthlyItem {
    return {
      month: safeStr(b.month),
      purchaseAmount: fenToYuan(b.purchaseAmount),
      orderCount: safeNum(b.orderCount),
      settledAmount: fenToYuan(b.settledAmount),
      supplierCount: safeNum(b.supplierCount),
    };
  },

  /** 供应商维度项转换 */
  toSupplierItem(b: PurchaseReportSupplierItemBackend): PurchaseReportSupplierItem {
    return {
      supplierId: idToString(b.supplierId),
      supplierName: safeStr(b.supplierName),
      totalAmount: fenToYuan(b.totalAmount),
      orderCount: safeNum(b.orderCount),
      onTimeRate: rateToPercent(b.onTimeRate),
      qualifiedRate: rateToPercent(b.qualifiedRate),
    };
  },

  /** 分类维度项转换 */
  toCategoryItem(b: PurchaseReportCategoryItemBackend): PurchaseReportCategoryItem {
    return {
      categoryName: safeStr(b.categoryName),
      totalAmount: fenToYuan(b.totalAmount),
      totalQuantity: safeBigNum(b.totalQuantity),
      orderCount: safeNum(b.orderCount),
    };
  },

  /** 元 → 分（委托给 utils/money，用于上传参数，目前报表 API 无需上传金额，保留以备扩展） */
  yuanToFen(yuan: number | null | undefined): number {
    return utilsYuanToFen(yuan);
  },

  /** 格式化金额（元，带千分位与两位小数） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00';
    return yuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  },

  /** 格式化百分比（保留一位小数） */
  formatPercent(percent: number | null | undefined): string {
    if (percent == null) return '0.0';
    return percent.toFixed(1);
  },
}

// ============================================================
// API
// ============================================================

export const purchaseReportApi = {
  /** 获取报表汇总数据 */
  async getSummary(params?: PurchaseReportQueryForm): Promise<PurchaseReportSummary> {
    const res = await get<PurchaseReportSummaryBackend | null>('/v1/purchase/reports/summary', {
      startDate: params?.startDate,
      endDate: params?.endDate,
      supplierId: params?.supplierId,
      category: params?.category,
    });
    return purchaseReportConverter.toSummary(res ?? {} as PurchaseReportSummaryBackend);
  },

  /** 获取月度趋势数据 */
  async getMonthlyTrend(params?: PurchaseReportQueryForm): Promise<PurchaseReportMonthlyItem[]> {
    const res = await get<PurchaseReportMonthlyItemBackend[] | null>('/v1/purchase/reports/monthly-trend', {
      startDate: params?.startDate,
      endDate: params?.endDate,
      supplierId: params?.supplierId,
      category: params?.category,
    });
    return (res ?? []).map(purchaseReportConverter.toMonthlyItem);
  },

  /** 获取供应商维度数据 */
  async getSupplierReport(params?: PurchaseReportQueryForm): Promise<PurchaseReportSupplierItem[]> {
    const res = await get<PurchaseReportSupplierItemBackend[] | null>('/v1/purchase/reports/supplier', {
      startDate: params?.startDate,
      endDate: params?.endDate,
      supplierId: params?.supplierId,
      category: params?.category,
    });
    return (res ?? []).map(purchaseReportConverter.toSupplierItem);
  },

  /** 获取分类维度数据 */
  async getCategoryReport(params?: PurchaseReportQueryForm): Promise<PurchaseReportCategoryItem[]> {
    const res = await get<PurchaseReportCategoryItemBackend[] | null>('/v1/purchase/reports/category', {
      startDate: params?.startDate,
      endDate: params?.endDate,
      supplierId: params?.supplierId,
      category: params?.category,
    });
    return (res ?? []).map(purchaseReportConverter.toCategoryItem);
  },

  /** 导出报表（返回 Blob，含 UTF-8 BOM 头，Excel 兼容） */
  async exportReport(params: PurchaseReportQueryForm & { format: string }): Promise<Blob> {
    const res = await get<ArrayBuffer | Blob | unknown>('/v1/purchase/reports/export', {
      startDate: params.startDate,
      endDate: params.endDate,
      supplierId: params.supplierId,
      category: params.category,
      format: params.format,
    }, { responseType: 'blob' });
    // 后端返回 byte[]（含 UTF-8 BOM 头），axios 在 responseType=blob 时已包装为 Blob
    if (res instanceof Blob) return res;
    if (res instanceof ArrayBuffer) return new Blob([res], { type: 'text/csv;charset=utf-8;' });
    // 兜底：包装为空 Blob，避免 UI 报错
    return new Blob([], { type: 'text/csv;charset=utf-8;' });
  },
}

export default purchaseReportApi
