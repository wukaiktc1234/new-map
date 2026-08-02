import type { PageResponse, ExportParams } from './common'

/** 对账单状态 */
export type SettlementStatus = 'pending' | 'approved' | 'rejected'

/** 日结对账记录 */
export interface DailySettlement {
  settlementId: string
  settlementDate: string
  storeId: string
  storeName: string
  totalRevenue: number
  totalCost: number
  netProfit: number
  grossProfitRate: number
  orderCount: number
  avgOrderValue: number
  auditorId: string
  auditorName: string
  status: SettlementStatus
  confirmTime?: string
  remark?: string
  createTime: string
  updateTime: string
  /** 营业差异金额（订单金额与收款金额的差值） */
  difference?: number
  /** 退款金额（分） */
  refundAmount?: number
  /** 作废金额（分） */
  cancelledAmount?: number
  /** 支付方式明细 */
  paymentBreakdown?: Record<string, { amount: number; count: number }>
  /** 优惠抵扣总金额（分） */
  totalDiscountAmount?: number
  /** 审核时间 */
  auditTime?: string
  /** 期望营收（用于差异对比，分） */
  expectedRevenue?: number
  /** 期望成本（用于差异对比，分） */
  expectedCost?: number
  /** 期望利润（用于差异对比，分） */
  expectedProfit?: number
}

/** 后端日结对账数据结构（金额为分，状态为数字编码） */
export interface SettlementBackend {
  settlementId: string
  settlementDate: string
  storeId: string
  storeName: string
  totalRevenue: number
  totalCost: number
  netProfit: number
  grossProfitRate: number
  orderCount: number
  avgOrderValue: number
  auditorId: string
  auditorName: string
  status: number
  confirmTime?: string
  remark?: string
  createTime: string
  updateTime: string
}

/** 对账查询参数 */
export interface SettlementQueryParams {
  page: number
  size: number
  dateRange?: [string, string] | null
  status?: SettlementStatus
  storeId?: string
}

/** 确认对账请求 */
export interface ConfirmSettlementDTO {
  settlementId: string
  auditorRemark?: string
}

/** 对账导出参数 */
export interface SettlementExportParams extends ExportParams {
  storeId?: string
}
