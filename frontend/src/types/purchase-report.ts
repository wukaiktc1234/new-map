/**
 * 采购报表相关类型定义
 *
 * 包含两部分：
 * 1. 报表元信息类型（PurchaseReportType / PurchaseReportInfo / PurchaseReportQueryForm）
 *    - 用于报表列表视图的展示与查询
 * 2. 报表数据项类型（PurchaseReportSummary / PurchaseReportMonthlyItem / PurchaseReportSupplierItem / PurchaseReportCategoryItem）
 *    - 与后端 VO 一一对应，金额字段以"元"为单位（由 DataConverter 在 API 边界从"分"转换）
 */

/**
 * 采购报表类型
 */
export type PurchaseReportType =
  | 'summary'  // 汇总报表
  | 'detail'   // 明细报表
  | 'supplier' // 供应商报表
  | 'category'; // 分类报表

/**
 * 采购报表信息（用于报表列表展示）
 */
export interface PurchaseReportInfo {
  /** 报表ID */
  reportId: string;
  /** 报表类型 */
  reportType: PurchaseReportType;
  /** 报表日期 */
  reportDate: string;
  /** 订单总数 */
  totalOrders: number;
  /** 总金额（元） */
  totalAmount: number;
  /** 平均订单金额（元） */
  avgOrderAmount: number;
  /** 最大供应商 */
  topSupplier: string;
  /** 最大分类 */
  topCategory: string;
  /** 状态 */
  status: string;
  /** 创建人 */
  createBy: string;
  /** 创建时间 */
  createTime: string;
}

/**
 * 采购报表查询参数
 */
export interface PurchaseReportQueryForm {
  /** 开始日期（yyyy-MM-dd） */
  startDate?: string;
  /** 结束日期（yyyy-MM-dd） */
  endDate?: string;
  /** 供应商ID（可选） */
  supplierId?: string;
  /** 物资分类名称（可选） */
  category?: string;
}

// ============================================================
// 报表数据项类型（与后端 VO 一一对应，金额单位：元）
// ============================================================

/**
 * 采购报表汇总数据
 * 对应后端: PurchaseReportSummaryVO
 */
export interface PurchaseReportSummary {
  /** 采购总金额（元） */
  totalPurchaseAmount: number;
  /** 订单总数 */
  totalOrderCount: number;
  /** 已结算金额（元） */
  totalSettledAmount: number;
  /** 未结算金额（元） */
  totalUnsettledAmount: number;
  /** 平均订单金额（元） */
  avgOrderAmount: number;
  /** 供应商数 */
  supplierCount: number;
  /** 待处理订单数 */
  pendingOrders: number;
  /** 准时交付率（百分比，0~100） */
  onTimeDeliveryRate: number;
  /** 质检合格率（百分比，0~100） */
  qualifiedRate: number;
}

/**
 * 采购报表月度趋势数据项
 * 对应后端: PurchaseReportMonthlyItemVO
 */
export interface PurchaseReportMonthlyItem {
  /** 月份（yyyy-MM） */
  month: string;
  /** 采购金额（元） */
  purchaseAmount: number;
  /** 订单数 */
  orderCount: number;
  /** 已结算金额（元） */
  settledAmount: number;
  /** 供应商数 */
  supplierCount: number;
}

/**
 * 采购报表供应商维度数据项
 * 对应后端: PurchaseReportSupplierItemVO
 */
export interface PurchaseReportSupplierItem {
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName: string;
  /** 采购总额（元） */
  totalAmount: number;
  /** 订单数 */
  orderCount: number;
  /** 准时交付率（百分比，0~100） */
  onTimeRate: number;
  /** 质检合格率（百分比，0~100） */
  qualifiedRate: number;
}

/**
 * 采购报表分类维度数据项
 * 对应后端: PurchaseReportCategoryItemVO
 */
export interface PurchaseReportCategoryItem {
  /** 物资分类名称 */
  categoryName: string;
  /** 采购总额（元） */
  totalAmount: number;
  /** 总数量 */
  totalQuantity: number;
  /** 订单数 */
  orderCount: number;
}
