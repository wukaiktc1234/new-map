/**
 * 采购管理模块数据转换器
 * 负责后端数据与前端展示数据之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 */

import type { PurchaseOrderStatus, PaymentStatus } from '@/types/purchase-order'
import type { MaterialArchiveStatus } from '@/types/purchase-archive'
import type { MaterialCategoryStatus } from '@/types/purchase-category'
import type { PurchasePlanStatus } from '@/types/purchase-plan'
import type { PurchaseStockinStatus, DeliveryMethod } from '@/types/purchase-stockin'
import type { PurchaseContractInfo } from '@/types/purchase-contract'
import type { ElectronicContractStatus } from '@/types/purchase-electronic-contract'
import type { PurchaseSettlementStatus } from '@/types/purchase-settlement'
import type { MaterialRequestStatus } from '@/types/material-request'
import type {
  SupplierType,
  SupplierLevel,
  SupplierStatus,
  SettlementMethod,
} from '@/types/purchase-supplier'
import type { PurchaseReturnStatus, PurchaseReturnRefundMethod } from '@/types/purchase-return'
import { fenToYuan, yuanToFen } from '@/utils/money'

// ============================================================
// 采购订单状态转换
// ============================================================

/** 订单状态 → StatusTag status */
const orderStatusMap: Record<PurchaseOrderStatus, string> = {
  draft: 'default',
  pending: 'warning',
  approved: 'primary',
  ordered: 'orange',
  shipped: 'info',
  received: 'success',
  rejected: 'danger',
  partial_received: 'probation',
  completed: 'active',
  terminated: 'danger',
  cancelled: 'inactive',
}

/** 订单状态 → 中文标签 */
const orderStatusLabelMap: Record<PurchaseOrderStatus, string> = {
  draft: '草稿',
  pending: '待审批',
  approved: '已审批',
  ordered: '已下单',
  shipped: '已发货',
  received: '已收货',
  rejected: '已拒绝',
  partial_received: '部分到货',
  completed: '已完成',
  terminated: '已终止',
  cancelled: '已取消',
}

/** 付款状态 → StatusTag status */
const paymentStatusMap: Record<PaymentStatus, string> = {
  unpaid: 'danger',
  partial: 'warning',
  paid: 'success',
}

/** 付款状态 → 中文标签 */
const paymentStatusLabelMap: Record<PaymentStatus, string> = {
  unpaid: '未付款',
  partial: '部分付款',
  paid: '已付款',
}

// ============================================================
// 商品档案状态转换
// ============================================================

/** 商品档案状态 → StatusTag status */
const archiveStatusMap: Record<MaterialArchiveStatus, string> = {
  active: 'success',
  inactive: 'info',
}

/** 商品档案状态 → 中文标签 */
const archiveStatusLabelMap: Record<MaterialArchiveStatus, string> = {
  active: '启用',
  inactive: '停用',
}

// ============================================================
// 商品分类状态转换
// ============================================================

/** 商品分类状态 → StatusTag status */
const categoryStatusMap: Record<MaterialCategoryStatus, string> = {
  active: 'success',
  inactive: 'info',
}

/** 商品分类状态 → 中文标签 */
const categoryStatusLabelMap: Record<MaterialCategoryStatus, string> = {
  active: '启用',
  inactive: '停用',
}

// ============================================================
// 采购计划状态转换
// ============================================================

/** 计划状态 → StatusTag status */
const planStatusMap: Record<PurchasePlanStatus, string> = {
  draft: 'default',
  pending: 'warning',
  approved: 'success',
  executing: 'info',
  completed: 'success',
  rejected: 'danger',
}

/** 计划状态 → 中文标签 */
const planStatusLabelMap: Record<PurchasePlanStatus, string> = {
  draft: '草稿',
  pending: '待审批',
  approved: '已审批',
  executing: '执行中',
  completed: '已完成',
  rejected: '已拒绝',
}

// ============================================================
// 采购收货状态转换
// ============================================================

/** 收货状态 → StatusTag status */
const stockinStatusMap: Record<PurchaseStockinStatus, string> = {
  pending: 'warning',
  inspecting: 'info',
  qualified: 'success',
  unqualified: 'danger',
  partial: 'warning',
  completed: 'success',
}

/** 收货状态 → 中文标签 */
const stockinStatusLabelMap: Record<PurchaseStockinStatus, string> = {
  pending: '待检验',
  inspecting: '检验中',
  qualified: '检验合格',
  unqualified: '检验不合格',
  partial: '部分合格',
  completed: '已完成',
}

/** 收货方式 → 中文标签 */
const deliveryMethodLabelMap: Record<DeliveryMethod, string> = {
  warehouse_direct: '直发仓库',
  store_direct: '直发门店',
  batch: '分批收货',
}

// ============================================================
// 采购合同状态转换
// ============================================================

type ContractStatus = PurchaseContractInfo['status']

/** 合同状态 → StatusTag status */
const contractStatusMap: Record<ContractStatus, string> = {
  draft: 'default',
  pending: 'warning',
  active: 'success',
  expired: 'info',
  terminated: 'danger',
}

/** 合同状态 → 中文标签 */
const contractStatusLabelMap: Record<ContractStatus, string> = {
  draft: '草稿',
  pending: '待审批',
  active: '生效中',
  expired: '已过期',
  terminated: '已终止',
}

// ============================================================
// 电子合同状态转换
// ============================================================

/** 电子合同状态 → StatusTag status */
const eContractStatusMap: Record<ElectronicContractStatus, string> = {
  draft: 'default',
  pending_sign: 'warning',
  signed: 'success',
  expired: 'info',
  cancelled: 'danger',
}

/** 电子合同状态 → 中文标签 */
const eContractStatusLabelMap: Record<ElectronicContractStatus, string> = {
  draft: '草稿',
  pending_sign: '待签署',
  signed: '已签署',
  expired: '已过期',
  cancelled: '已取消',
}

// ============================================================
// 采购申请状态转换
// ============================================================

const requestStatusMap: Record<string, string> = {
  draft: 'default',
  pending: 'warning',
  approved: 'success',
  rejected: 'danger',
  completed: 'success',
  cancelled: 'info',
}

const requestStatusLabelMap: Record<string, string> = {
  draft: '草稿',
  pending: '待审批',
  approved: '已审批',
  rejected: '已拒绝',
  completed: '已完成',
  cancelled: '已取消',
}

// ============================================================
// 采购退货状态转换
// ============================================================

/** 退货单状态 → StatusTag status */
const returnStatusMap: Record<PurchaseReturnStatus, string> = {
  pending: 'warning',
  approved: 'success',
  rejected: 'danger',
  completed: 'info',
}

/** 退货单状态 → 中文标签 */
const returnStatusLabelMap: Record<PurchaseReturnStatus, string> = {
  pending: '待审批',
  approved: '已通过',
  rejected: '已驳回',
  completed: '已完成',
}

/** 退款方式 → StatusTag status */
const refundMethodStatusMap: Record<PurchaseReturnRefundMethod, string> = {
  offset: 'primary',
  cash: 'warning',
}

/** 退款方式 → 中文标签 */
const refundMethodLabelMap: Record<PurchaseReturnRefundMethod, string> = {
  offset: '冲抵货款',
  cash: '现金退款',
}

// ============================================================
// 采购结算状态转换
// ============================================================

/** 结算状态 → StatusTag status */
const settlementStatusMap: Record<PurchaseSettlementStatus, string> = {
  pending: 'warning',
  partial: 'info',
  finance_reviewing: 'warning',
  completed: 'success',
  overdue: 'danger',
}

/** 结算状态 → 中文标签 */
const settlementStatusLabelMap: Record<PurchaseSettlementStatus, string> = {
  pending: '待结算',
  partial: '部分结算',
  finance_reviewing: '财务审核中',
  completed: '已结算',
  overdue: '已逾期',
}

/**
 * 结算发票状态映射（后端数字编码）
 * 0 未开票 | 1 已开票 | 2 已收票
 */
const settlementInvoiceStatusLabelMap: Record<number, string> = {
  0: '未开票',
  1: '已开票',
  2: '已收票',
}

// ============================================================
// 导出转换器（金额转换函数来自 utils/money）
// ============================================================

export const purchaseOrderConverter = {
  toStatusTagStatus(status: PurchaseOrderStatus): string {
    return orderStatusMap[status] || 'default'
  },
  toStatusLabel(status: PurchaseOrderStatus): string {
    return orderStatusLabelMap[status] || status
  },
  toPaymentStatusTagStatus(status: PaymentStatus): string {
    return paymentStatusMap[status] || 'default'
  },
  toPaymentStatusLabel(status: PaymentStatus): string {
    return paymentStatusLabelMap[status] || status
  },
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

export const materialArchiveConverter = {
  toStatusTagStatus(status: MaterialArchiveStatus): string {
    return archiveStatusMap[status] || 'default'
  },
  toStatusLabel(status: MaterialArchiveStatus): string {
    return archiveStatusLabelMap[status] || status
  },
}

export const materialCategoryConverter = {
  toStatusTagStatus(status: MaterialCategoryStatus): string {
    return categoryStatusMap[status] || 'default'
  },
  toStatusLabel(status: MaterialCategoryStatus): string {
    return categoryStatusLabelMap[status] || status
  },
}

export const purchasePlanConverter = {
  toStatusTagStatus(status: PurchasePlanStatus): string {
    return planStatusMap[status] || 'default'
  },
  toStatusLabel(status: PurchasePlanStatus): string {
    return planStatusLabelMap[status] || status
  },
}

export const purchaseStockinConverter = {
  toStatusTagStatus(status: PurchaseStockinStatus): string {
    return stockinStatusMap[status] || 'default'
  },
  toStatusLabel(status: PurchaseStockinStatus): string {
    return stockinStatusLabelMap[status] || status
  },
  toDeliveryMethodLabel(method: DeliveryMethod): string {
    return deliveryMethodLabelMap[method] || method
  },
}

export const purchaseContractConverter = {
  toStatusTagStatus(status: ContractStatus): string {
    return contractStatusMap[status] || 'default'
  },
  toStatusLabel(status: ContractStatus): string {
    return contractStatusLabelMap[status] || status
  },
  /** 格式化金额（元，带千分位与两位小数） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return Number(yuan).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  },
}

export const electronicContractConverter = {
  toStatusTagStatus(status: ElectronicContractStatus): string {
    return eContractStatusMap[status] || 'default'
  },
  toStatusLabel(status: ElectronicContractStatus): string {
    return eContractStatusLabelMap[status] || status
  },
  /** 格式化金额（元，带千分位与两位小数） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return Number(yuan).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  },
}

export const purchaseRequestConverter = {
  toStatusTagStatus(status: string): string {
    return requestStatusMap[status] || 'default'
  },
  toStatusLabel(status: string): string {
    return requestStatusLabelMap[status] || status
  },
  /** 格式化金额（元，带千分位与两位小数） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return yuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  },
}

// ============================================================
// 物资需求提报状态转换
// ============================================================

/** 物资需求提报状态 → StatusTag status */
const materialRequestStatusMap: Record<MaterialRequestStatus, string> = {
  draft: 'default',
  pending: 'warning',
  approved: 'success',
  rejected: 'danger',
  converted: 'info',
}

/** 物资需求提报状态 → 中文标签 */
const materialRequestStatusLabelMap: Record<MaterialRequestStatus, string> = {
  draft: '草稿',
  pending: '待审核',
  approved: '已审核',
  rejected: '已驳回',
  converted: '已转采购申请',
}

export const materialRequestConverter = {
  toStatusTagStatus(status: MaterialRequestStatus): string {
    return materialRequestStatusMap[status] || 'default'
  },
  toStatusLabel(status: MaterialRequestStatus): string {
    return materialRequestStatusLabelMap[status] || status
  },
}

export const purchaseSettlementConverter = {
  toStatusTagStatus(status: PurchaseSettlementStatus): string {
    return settlementStatusMap[status] || 'default'
  },
  toStatusLabel(status: PurchaseSettlementStatus): string {
    return settlementStatusLabelMap[status] || status
  },
  /** 发票状态 → 中文标签（后端数字编码 0未开票/1已开票/2已收票） */
  toInvoiceStatusLabel(invoiceStatus: number): string {
    return settlementInvoiceStatusLabelMap[invoiceStatus] ?? '未知'
  },
}

export const purchaseReturnConverter = {
  toStatusTagStatus(status: PurchaseReturnStatus): string {
    return returnStatusMap[status] || 'default'
  },
  toStatusLabel(status: PurchaseReturnStatus): string {
    return returnStatusLabelMap[status] || status
  },
  toRefundMethodTagStatus(method: PurchaseReturnRefundMethod): string {
    return refundMethodStatusMap[method] || 'default'
  },
  toRefundMethodLabel(method: PurchaseReturnRefundMethod): string {
    return refundMethodLabelMap[method] || method
  },
  /** 格式化金额（元，带千分位与两位小数） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return Number(yuan).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  },
}

// ============================================================
// 供应商档案转换
// ============================================================

/** 供应商类型 → 中文标签 */
const supplierTypeMap: Record<SupplierType, string> = {
  raw_material: '原材料',
  auxiliary: '辅料',
  equipment: '设备',
  service: '服务',
}

/** 供应商等级 → 中文标签 */
const supplierLevelMap: Record<SupplierLevel, string> = {
  strategic: '战略',
  qualified: '合格',
  alternative: '备选',
  temporary: '临时',
}

/** 供应商状态 → StatusTag status */
const supplierStatusMap: Record<SupplierStatus, string> = {
  active: 'success',
  inactive: 'info',
  frozen: 'warning',
  eliminated: 'danger',
}

/** 供应商状态 → 中文标签 */
const supplierStatusLabelMap: Record<SupplierStatus, string> = {
  active: '正常',
  inactive: '停用',
  frozen: '冻结',
  eliminated: '淘汰',
}

/** 供应商类型 → StatusTag status */
const supplierTypeStatusMap: Record<SupplierType, string> = {
  raw_material: 'primary',
  auxiliary: 'info',
  equipment: 'warning',
  service: 'default',
}

/** 供应商等级 → StatusTag status */
const supplierLevelStatusMap: Record<SupplierLevel, string> = {
  strategic: 'success',
  qualified: 'primary',
  alternative: 'warning',
  temporary: 'info',
}

/** 结算方式 → 中文标签 */
const settlementMethodLabelMap: Record<SettlementMethod, string> = {
  monthly: '月结',
  immediate: '现结',
  prepaid: '预付',
}

export const supplierConverter = {
  toTypeLabel(type: SupplierType): string {
    return supplierTypeMap[type] || type
  },
  toTypeStatusTagStatus(type: SupplierType): string {
    return supplierTypeStatusMap[type] || 'default'
  },
  toLevelLabel(level: SupplierLevel): string {
    return supplierLevelMap[level] || level
  },
  toLevelStatusTagStatus(level: SupplierLevel): string {
    return supplierLevelStatusMap[level] || 'default'
  },
  toSettlementMethodLabel(method: SettlementMethod): string {
    return settlementMethodLabelMap[method] || method
  },
  toStatusTagStatus(status: SupplierStatus): string {
    return supplierStatusMap[status] || 'default'
  },
  toStatusLabel(status: SupplierStatus): string {
    return supplierStatusLabelMap[status] || status
  },
}
