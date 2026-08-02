/**
 * 采购结算相关类型定义
 * 对应后端实体: PurchaseSettlement (purchase_settlement)
 */

/**
 * 采购结算状态
 */
export type PurchaseSettlementStatus =
  | 'pending'  // 待付款
  | 'partial'  // 部分付款
  | 'finance_reviewing' // 财务审核中
  | 'completed' // 已完成
  | 'overdue';  // 已逾期

/**
 * 采购结算信息
 */
export interface PurchaseSettlementInfo {
  /** 结算ID */
  settlementId: string;
  /** 结算编号 */
  settlementNo: string;
  /** 采购订单ID */
  orderId: string;
  /** 采购订单编号 */
  orderNo: string;
  /** 供应商ID */
  supplierId: string;
  /** 供应商名称 */
  supplierName: string;
  /** 总金额（元） */
  totalAmount: number;
  /** 已付金额（元） */
  paidAmount: number;
  /** 未付金额（元） */
  unpaidAmount: number;
  /** 到期日期 */
  dueDate: string;
  /** 状态 */
  status: PurchaseSettlementStatus;
  /** 付款方式 */
  paymentMethod: string;
  /** 备注 */
  remark: string;
  /** 创建人 */
  createBy: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 采购结算查询参数
 */
export interface PurchaseSettlementQueryForm {
  /** 结算编号 */
  settlementNo?: string;
  /** 状态 */
  status?: PurchaseSettlementStatus | null;
  /** 供应商ID */
  supplierId?: string;
  /** 开始日期 */
  startDate?: string;
  /** 结束日期 */
  endDate?: string;
  /** 搜索关键词 */
  keyword?: string;
}

/**
 * 采购结算表单数据（新建/编辑）
 */
export interface PurchaseSettlementFormData {
  /** 采购订单ID */
  orderId: string;
  /** 总金额（元） */
  totalAmount: number;
  /** 到期日期 */
  dueDate: string;
  /** 付款方式 */
  paymentMethod?: string;
  /** 备注 */
  remark?: string;
}

/** 采购结算状态选项 */
export const PurchaseSettlementStatusOptions = [
  { label: '待付款', value: 'pending' },
  { label: '部分付款', value: 'partial' },
  { label: '财务审核中', value: 'finance_reviewing' },
  { label: '已完成', value: 'completed' },
  { label: '已逾期', value: 'overdue' },
] as const;
