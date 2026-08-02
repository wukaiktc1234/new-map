/**
 * 仓储管理模块数据转换器
 * 负责后端数据与前端展示数据之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 */

import type { WarehouseType, WarehouseStatus } from '@/types/warehouse'
import type { InventoryStatus, InventoryType, TransactionType } from '@/types/warehouse-inventory'
import type { LocationType, LocationStatus } from '@/types/warehouse-location'
import type { CheckType, CheckStatus } from '@/types/warehouse-check'
import type { TransferStatus } from '@/types/warehouse-transfer'
import type { WarningType, NotifyMethod } from '@/types/warehouse-warning'
import type { LossType, LossStatus } from '@/types/warehouse-loss'
import type { OutboundType, OutboundStatus } from '@/types/warehouse-outbound'
import type { AdjustType, AdjustStatus } from '@/types/warehouse-adjust'
import type { UrgencyLevel, ConfidenceLevel, SuggestionStatus } from '@/types/warehouse-smart-restock'
import { fenToYuan, yuanToFen, fenToYuanNumber } from '@/utils/money'

// ============================================================
// 仓库类型转换
// ============================================================

/** 仓库类型 → 中文标签 */
const warehouseTypeLabelMap: Record<WarehouseType, string> = {
  main: '主仓',
  cold: '冷库',
  freeze: '冻库',
  normal: '常温库',
}

/** 仓库类型 → StatusTag status */
const warehouseTypeStatusMap: Record<WarehouseType, string> = {
  main: 'primary',
  cold: 'info',
  freeze: 'warning',
  normal: 'default',
}

/** 仓库状态 → StatusTag status */
const warehouseStatusMap: Record<WarehouseStatus, string> = {
  active: 'success',
  inactive: 'info',
}

/** 仓库状态 → 中文标签 */
const warehouseStatusLabelMap: Record<WarehouseStatus, string> = {
  active: '启用',
  inactive: '停用',
}

/** 后端仓库类型数值 → 前端语义化字符串 */
const warehouseTypeBackendMap: Record<number, WarehouseType> = {
  1: 'main',
  2: 'cold',
  3: 'freeze',
  4: 'normal',
}

/** 前端语义化字符串 → 后端数值 */
const warehouseTypeFrontendMap: Record<WarehouseType, number> = {
  main: 1,
  cold: 2,
  freeze: 3,
  normal: 4,
}

/** 后端状态数值 → 前端语义化字符串 */
const warehouseStatusBackendMap: Record<number, WarehouseStatus> = {
  1: 'active',
  0: 'inactive',
}

/** 前端语义化字符串 → 后端数值 */
const warehouseStatusFrontendMap: Record<WarehouseStatus, number> = {
  active: 1,
  inactive: 0,
}

// ============================================================
// 库存状态转换
// ============================================================

/** 库存状态 → StatusTag status */
const inventoryStatusMap: Record<InventoryStatus, string> = {
  normal: 'success',
  warning: 'warning',
  expired: 'danger',
  frozen: 'info',
}

/** 库存状态 → 中文标签 */
const inventoryStatusLabelMap: Record<InventoryStatus, string> = {
  normal: '正常',
  warning: '预警',
  expired: '过期',
  frozen: '冻结',
}

/** 后端数值 → 前端字符串 */
const inventoryStatusBackendMap: Record<number, InventoryStatus> = {
  1: 'normal',
  2: 'warning',
  3: 'expired',
  4: 'frozen',
}

/** 前端字符串 → 后端数值 */
const inventoryStatusFrontendMap: Record<InventoryStatus, number> = {
  normal: 1,
  warning: 2,
  expired: 3,
  frozen: 4,
}

/** 库存类型 → 中文标签 */
const inventoryTypeLabelMap: Record<InventoryType, string> = {
  raw_material: '原料',
  semi_finished: '半成品',
  finished: '成品',
  packaging: '包装材料',
}

/** 库存类型 → StatusTag status */
const inventoryTypeStatusMap: Record<InventoryType, string> = {
  raw_material: 'primary',
  semi_finished: 'warning',
  finished: 'success',
  packaging: 'info',
}

/** 后端数值 → 前端字符串 */
const inventoryTypeBackendMap: Record<number, InventoryType> = {
  1: 'raw_material',
  2: 'semi_finished',
  3: 'finished',
  4: 'packaging',
}

/** 前端字符串 → 后端数值 */
const inventoryTypeFrontendMap: Record<InventoryType, number> = {
  raw_material: 1,
  semi_finished: 2,
  finished: 3,
  packaging: 4,
}

// ============================================================
// 变动类型转换
// ============================================================

/** 变动类型 → 中文标签 */
const transactionTypeLabelMap: Record<TransactionType, string> = {
  purchase_in: '采购入库',
  sale_out: '销售出库',
  transfer_out: '调拨出',
  transfer_in: '调拨入',
  check_gain: '盘点盈',
  check_loss: '盘点亏',
  damage: '报损',
  return: '退货',
}

/** 变动类型 → StatusTag status */
const transactionTypeStatusMap: Record<TransactionType, string> = {
  purchase_in: 'success',
  sale_out: 'primary',
  transfer_out: 'warning',
  transfer_in: 'info',
  check_gain: 'success',
  check_loss: 'danger',
  damage: 'danger',
  return: 'warning',
}

/** 后端数值 → 前端字符串 */
const transactionTypeBackendMap: Record<number, TransactionType> = {
  1: 'purchase_in',
  2: 'sale_out',
  3: 'transfer_out',
  4: 'transfer_in',
  5: 'check_gain',
  6: 'check_loss',
  7: 'damage',
  8: 'return',
}

/** 前端字符串 → 后端数值 */
const transactionTypeFrontendMap: Record<TransactionType, number> = {
  purchase_in: 1,
  sale_out: 2,
  transfer_out: 3,
  transfer_in: 4,
  check_gain: 5,
  check_loss: 6,
  damage: 7,
  return: 8,
}

// ============================================================
// 库位类型转换
// ============================================================

/** 库位类型 → 中文标签 */
const locationTypeLabelMap: Record<LocationType, string> = {
  shelf: '货架',
  floor: '地面',
  cold_storage: '冷藏区',
  freezer: '冷冻区',
}

/** 库位类型 → StatusTag status */
const locationTypeStatusMap: Record<LocationType, string> = {
  shelf: 'default',
  floor: 'info',
  cold_storage: 'primary',
  freezer: 'warning',
}

/** 后端数值 → 前端字符串 */
const locationTypeBackendMap: Record<number, LocationType> = {
  1: 'shelf',
  2: 'floor',
  3: 'cold_storage',
  4: 'freezer',
}

/** 前端字符串 → 后端数值 */
const locationTypeFrontendMap: Record<LocationType, number> = {
  shelf: 1,
  floor: 2,
  cold_storage: 3,
  freezer: 4,
}

/** 库位状态 → StatusTag status */
const locationStatusMap: Record<LocationStatus, string> = {
  active: 'success',
  inactive: 'info',
}

/** 库位状态 → 中文标签 */
const locationStatusLabelMap: Record<LocationStatus, string> = {
  active: '启用',
  inactive: '停用',
}

// ============================================================
// 盘点类型/状态转换
// ============================================================

/** 盘点类型 → 中文标签 */
const checkTypeLabelMap: Record<CheckType, string> = {
  blind: '盲盘',
  open: '明盘',
  cycle: '循环盘点',
}

/** 盘点状态 → StatusTag status */
const checkStatusMap: Record<CheckStatus, string> = {
  pending: 'default',
  checking: 'warning',
  approved: 'info',
  completed: 'success',
  cancelled: 'inactive',
}

/** 盘点状态 → 中文标签 */
const checkStatusLabelMap: Record<CheckStatus, string> = {
  pending: '待盘点',
  checking: '盘点中',
  approved: '已审核',
  completed: '已完成',
  cancelled: '已取消',
}

/** 后端数值 → 前端字符串 */
const checkTypeBackendMap: Record<number, CheckType> = {
  1: 'blind',
  2: 'open',
  3: 'cycle',
}

const checkStatusBackendMap: Record<number, CheckStatus> = {
  0: 'pending',
  1: 'checking',
  2: 'approved',
  3: 'completed',
  4: 'cancelled',
}

/** 前端字符串 → 后端数值 */
const checkTypeFrontendMap: Record<CheckType, number> = {
  blind: 1,
  open: 2,
  cycle: 3,
}

const checkStatusFrontendMap: Record<CheckStatus, number> = {
  pending: 0,
  checking: 1,
  approved: 2,
  completed: 3,
  cancelled: 4,
}

// ============================================================
// 调拨状态转换
// ============================================================

/** 调拨状态 → StatusTag status */
const transferStatusMap: Record<TransferStatus, string> = {
  pending: 'default',
  shipped: 'primary',
  received: 'info',
  completed: 'success',
}

/** 调拨状态 → 中文标签 */
const transferStatusLabelMap: Record<TransferStatus, string> = {
  pending: '待调拨',
  shipped: '已发出',
  received: '已接收',
  completed: '已完成',
}

/** 后端数值 → 前端字符串 */
const transferStatusBackendMap: Record<number, TransferStatus> = {
  0: 'pending',
  1: 'shipped',
  2: 'received',
  3: 'completed',
}

/** 前端字符串 → 后端数值 */
const transferStatusFrontendMap: Record<TransferStatus, number> = {
  pending: 0,
  shipped: 1,
  received: 2,
  completed: 3,
}

// ============================================================
// 预警类型转换
// ============================================================

/** 预警类型 → 中文标签 */
const warningTypeLabelMap: Record<WarningType, string> = {
  low_stock: '低库存',
  high_stock: '高库存',
  expiring_soon: '临期',
  expired: '过期',
}

/** 预警类型 → StatusTag status */
const warningTypeStatusMap: Record<WarningType, string> = {
  low_stock: 'warning',
  high_stock: 'info',
  expiring_soon: 'warning',
  expired: 'danger',
}

/** 后端数值 → 前端字符串 */
const warningTypeBackendMap: Record<number, WarningType> = {
  1: 'low_stock',
  2: 'high_stock',
  3: 'expiring_soon',
  4: 'expired',
}

/** 前端字符串 → 后端数值 */
const warningTypeFrontendMap: Record<WarningType, number> = {
  low_stock: 1,
  high_stock: 2,
  expiring_soon: 3,
  expired: 4,
}

/** 通知方式 → 中文标签 */
const notifyMethodLabelMap: Record<NotifyMethod, string> = {
  email: '邮件',
  message: '站内信',
  sms: '短信',
  all: '全部',
}

/** 后端数值 → 前端字符串 */
const notifyMethodBackendMap: Record<number, NotifyMethod> = {
  1: 'email',
  2: 'message',
  3: 'sms',
  4: 'all',
}

/** 前端字符串 → 后端数值 */
const notifyMethodFrontendMap: Record<NotifyMethod, number> = {
  email: 1,
  message: 2,
  sms: 3,
  all: 4,
}

/** 预警记录状态（0=待处理, 1=已处理） → 中文标签 */
const warningRecordStatusLabelMap: Record<number, string> = {
  0: '待处理',
  1: '已处理',
}

/** 预警记录状态 → StatusTag status */
const warningRecordStatusMap: Record<number, string> = {
  0: 'warning',
  1: 'success',
}

// ============================================================
// 报损类型/状态转换
// ============================================================

/** 报损类型 → 中文标签 */
const lossTypeLabelMap: Record<LossType, string> = {
  expired: '过期',
  damaged: '损坏',
  lost: '丢失',
  other: '其他',
}

/** 报损类型 → StatusTag status */
const lossTypeStatusMap: Record<LossType, string> = {
  expired: 'warning',
  damaged: 'danger',
  lost: 'danger',
  other: 'info',
}

/** 报损状态 → StatusTag status */
const lossStatusMap: Record<LossStatus, string> = {
  pending: 'default',
  approved: 'info',
  processed: 'success',
}

/** 报损状态 → 中文标签 */
const lossStatusLabelMap: Record<LossStatus, string> = {
  pending: '待审核',
  approved: '已审核',
  processed: '已处理',
}

/** 后端数值 → 前端字符串 */
const lossTypeBackendMap: Record<number, LossType> = {
  1: 'expired',
  2: 'damaged',
  3: 'lost',
  4: 'other',
}

const lossStatusBackendMap: Record<number, LossStatus> = {
  0: 'pending',
  1: 'approved',
  2: 'processed',
}

/** 前端字符串 → 后端数值 */
const lossTypeFrontendMap: Record<LossType, number> = {
  expired: 1,
  damaged: 2,
  lost: 3,
  other: 4,
}

const lossStatusFrontendMap: Record<LossStatus, number> = {
  pending: 0,
  approved: 1,
  processed: 2,
}

// ============================================================
// 导出转换器
// ============================================================

export const warehouseConverter = {
  toTypeLabel: (type: WarehouseType) => warehouseTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: WarehouseType) => warehouseTypeStatusMap[type] || 'default',
  toStatusLabel: (status: WarehouseStatus) => warehouseStatusLabelMap[status] || status,
  toStatusTagStatus: (status: WarehouseStatus) => warehouseStatusMap[status] || 'default',
  toBackendType: (type: WarehouseType) => warehouseTypeFrontendMap[type],
  toFrontendType: (val: number) => warehouseTypeBackendMap[val] || 'normal',
  toBackendStatus: (status: WarehouseStatus) => warehouseStatusFrontendMap[status],
  toFrontendStatus: (val: number) => warehouseStatusBackendMap[val] || 'inactive',
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

export const inventoryConverter = {
  toStatusLabel: (status: InventoryStatus) => inventoryStatusLabelMap[status] || status,
  toStatusTagStatus: (status: InventoryStatus) => inventoryStatusMap[status] || 'default',
  toBackendStatus: (status: InventoryStatus) => inventoryStatusFrontendMap[status],
  toFrontendStatus: (val: number) => inventoryStatusBackendMap[val] || 'normal',
  toTypeLabel: (type: InventoryType) => inventoryTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: InventoryType) => inventoryTypeStatusMap[type] || 'default',
  toBackendType: (type: InventoryType) => inventoryTypeFrontendMap[type],
  toFrontendType: (val: number) => inventoryTypeBackendMap[val] || 'raw_material',
  toTransactionTypeLabel: (type: TransactionType) => transactionTypeLabelMap[type] || type,
  toTransactionTypeStatusTagStatus: (type: TransactionType) => transactionTypeStatusMap[type] || 'default',
  toBackendTransactionType: (type: TransactionType) => transactionTypeFrontendMap[type],
  toFrontendTransactionType: (val: number) => transactionTypeBackendMap[val] || 'purchase_in',
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

export const locationConverter = {
  toTypeLabel: (type: LocationType) => locationTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: LocationType) => locationTypeStatusMap[type] || 'default',
  toBackendType: (type: LocationType) => locationTypeFrontendMap[type],
  toFrontendType: (val: number) => locationTypeBackendMap[val] || 'shelf',
  toStatusLabel: (status: LocationStatus) => locationStatusLabelMap[status] || status,
  toStatusTagStatus: (status: LocationStatus) => locationStatusMap[status] || 'default',
}

export const inventoryCheckConverter = {
  toTypeLabel: (type: CheckType) => checkTypeLabelMap[type] || type,
  toStatusLabel: (status: CheckStatus) => checkStatusLabelMap[status] || status,
  toStatusTagStatus: (status: CheckStatus) => checkStatusMap[status] || 'default',
  toBackendType: (type: CheckType) => checkTypeFrontendMap[type],
  toFrontendType: (val: number) => checkTypeBackendMap[val] || 'open',
  toBackendStatus: (status: CheckStatus) => checkStatusFrontendMap[status],
  toFrontendStatus: (val: number) => checkStatusBackendMap[val] || 'pending',
}

export const inventoryTransferConverter = {
  toStatusLabel: (status: TransferStatus) => transferStatusLabelMap[status] || status,
  toStatusTagStatus: (status: TransferStatus) => transferStatusMap[status] || 'default',
  toBackendStatus: (status: TransferStatus) => transferStatusFrontendMap[status],
  toFrontendStatus: (val: number) => transferStatusBackendMap[val] || 'pending',
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

export const inventoryWarningConverter = {
  toTypeLabel: (type: WarningType) => warningTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: WarningType) => warningTypeStatusMap[type] || 'default',
  toBackendType: (type: WarningType) => warningTypeFrontendMap[type],
  toFrontendType: (val: number) => warningTypeBackendMap[val] || 'low_stock',
  toNotifyMethodLabel: (method: NotifyMethod) => notifyMethodLabelMap[method] || method,
  toBackendNotifyMethod: (method: NotifyMethod) => notifyMethodFrontendMap[method],
  toFrontendNotifyMethod: (val: number) => notifyMethodBackendMap[val] || 'message',
  /** 预警记录状态（0=待处理, 1=已处理） → 中文标签 */
  toStatusLabel: (status: number) => warningRecordStatusLabelMap[status] || '待处理',
  /** 预警记录状态 → StatusTag status */
  toStatusTagStatus: (status: number) => warningRecordStatusMap[status] || 'default',
}

export const inventoryLossConverter = {
  toTypeLabel: (type: LossType) => lossTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: LossType) => lossTypeStatusMap[type] || 'default',
  toStatusLabel: (status: LossStatus) => lossStatusLabelMap[status] || status,
  toStatusTagStatus: (status: LossStatus) => lossStatusMap[status] || 'default',
  toBackendType: (type: LossType) => lossTypeFrontendMap[type],
  toFrontendType: (val: number) => lossTypeBackendMap[val] || 'other',
  toBackendStatus: (status: LossStatus) => lossStatusFrontendMap[status],
  toFrontendStatus: (val: number) => lossStatusBackendMap[val] || 'pending',
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

// ============================================================
// 出库类型转换
// ============================================================

const outboundTypeLabelMap: Record<OutboundType, string> = {
  requisition: '领料出库',
  sale: '销售出库',
  return: '退货出库',
  other: '其他出库',
}

const outboundTypeStatusMap: Record<OutboundType, string> = {
  requisition: 'info',
  sale: 'primary',
  return: 'warning',
  other: 'default',
}

const outboundStatusLabelMap: Record<OutboundStatus, string> = {
  pending: '待审批',
  approved: '已审批',
  completed: '已出库',
  rejected: '已驳回',
}

const outboundStatusMap: Record<OutboundStatus, string> = {
  pending: 'warning',
  approved: 'primary',
  completed: 'success',
  rejected: 'error',
}

export const inventoryOutboundConverter = {
  toTypeLabel: (type: OutboundType) => outboundTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: OutboundType) => outboundTypeStatusMap[type] || 'default',
  toStatusLabel: (status: OutboundStatus) => outboundStatusLabelMap[status] || status,
  toStatusTagStatus: (status: OutboundStatus) => outboundStatusMap[status] || 'default',
  /** 前端字符串 → 后端字符串（后端 type 为 String） */
  toBackendType: (type: OutboundType): string => type,
  /** 后端字符串 → 前端字符串 */
  toFrontendType: (val: string | null | undefined): OutboundType => (val as OutboundType) || 'other',
  /** 前端字符串 → 后端字符串（后端 status 为 String） */
  toBackendStatus: (status: OutboundStatus): string => status,
  /** 后端字符串 → 前端字符串 */
  toFrontendStatus: (val: string | null | undefined): OutboundStatus => (val as OutboundStatus) || 'pending',
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

// ============================================================
// 调整类型转换
// ============================================================

const adjustTypeLabelMap: Record<AdjustType, string> = {
  gain: '盘盈调整',
  loss: '盘亏调整',
  temp_loss: '温度损耗',
  weight_diff: '称重差异',
  other: '其他调整',
}

const adjustTypeStatusMap: Record<AdjustType, string> = {
  gain: 'success',
  loss: 'error',
  temp_loss: 'warning',
  weight_diff: 'info',
  other: 'default',
}

const adjustStatusLabelMap: Record<AdjustStatus, string> = {
  pending: '待审批',
  approved: '已审批',
  completed: '已完成',
  rejected: '已驳回',
}

const adjustStatusMap: Record<AdjustStatus, string> = {
  pending: 'warning',
  approved: 'primary',
  completed: 'success',
  rejected: 'error',
}

export const inventoryAdjustConverter = {
  toTypeLabel: (type: AdjustType) => adjustTypeLabelMap[type] || type,
  toTypeStatusTagStatus: (type: AdjustType) => adjustTypeStatusMap[type] || 'default',
  toStatusLabel: (status: AdjustStatus) => adjustStatusLabelMap[status] || status,
  toStatusTagStatus: (status: AdjustStatus) => adjustStatusMap[status] || 'default',
  /** 前端字符串 → 后端字符串（后端 type 为 String） */
  toBackendType: (type: AdjustType): string => type,
  /** 后端字符串 → 前端字符串 */
  toFrontendType: (val: string | null | undefined): AdjustType => (val as AdjustType) || 'other',
  /** 前端字符串 → 后端字符串（后端 status 为 String） */
  toBackendStatus: (status: AdjustStatus): string => status,
  /** 后端字符串 → 前端字符串 */
  toFrontendStatus: (val: string | null | undefined): AdjustStatus => (val as AdjustStatus) || 'pending',
  toYuan: fenToYuan,
  toFen: yuanToFen,
}

// ============================================================
// 库存日志操作类型转换
// ============================================================

/** 操作类型 → 中文标签 */
const operationTypeLabelMap: Record<string, string> = {
  purchase_in: '采购入库',
  sale_out: '领用出库',
  transfer_out: '调拨出库',
  transfer_in: '调拨入库',
  check_gain: '盘点盈',
  check_loss: '盘点亏',
  damage: '报损',
  return: '退货',
}

/** 操作类型 → StatusTag status */
const operationTypeStatusMap: Record<string, string> = {
  purchase_in: 'success',
  sale_out: 'primary',
  transfer_out: 'warning',
  transfer_in: 'info',
  check_gain: 'success',
  check_loss: 'danger',
  damage: 'danger',
  return: 'warning',
}

export const inventoryLogConverter = {
  /** 操作类型 → 中文标签 */
  toOperationTypeLabel: (operationType: string): string => operationTypeLabelMap[operationType] || operationType,
  /** 操作类型 → StatusTag status */
  toOperationTypeStatusTagStatus: (operationType: string): string => operationTypeStatusMap[operationType] || 'default',
}

// ============================================================
// 库存统计转换（金额元分转换统一走 utils/money + SmartRestock 状态映射）
// ============================================================

/** 紧急程度 → 中文标签 */
const urgencyLabelMap: Record<UrgencyLevel, string> = {
  urgent: '紧急',
  high: '高',
  medium: '中',
  low: '低',
}

/** 紧急程度 → StatusTag status */
const urgencyStatusMap: Record<UrgencyLevel, string> = {
  urgent: 'danger',
  high: 'warning',
  medium: 'info',
  low: 'success',
}

/** 置信度 → 中文标签 */
const confidenceLabelMap: Record<ConfidenceLevel, string> = {
  high: '高',
  medium: '中',
  low: '低',
}

/** 置信度 → StatusTag status */
const confidenceStatusMap: Record<ConfidenceLevel, string> = {
  high: 'success',
  medium: 'warning',
  low: 'danger',
}

/** 建议状态 → 中文标签 */
const suggestionStatusLabelMap: Record<SuggestionStatus, string> = {
  pending: '待审核',
  accepted: '已采纳',
  modified: '已调整',
  rejected: '已拒绝',
  converted: '已转采购',
}

/** 建议状态 → StatusTag status */
const suggestionStatusMap: Record<SuggestionStatus, string> = {
  pending: 'warning',
  accepted: 'success',
  modified: 'info',
  rejected: 'danger',
  converted: 'primary',
}

export const inventoryStatsConverter = {
  /** 分 → 元 */
  toYuan: (fen: number | null | undefined): number => fenToYuanNumber(fen),
  /** 元 → 分 */
  toFen: (yuan: number): number => yuanToFen(yuan),
  /** 紧急程度 → 中文标签 */
  toUrgencyLabel: (level: UrgencyLevel): string => urgencyLabelMap[level] || level,
  /** 紧急程度 → StatusTag status */
  toUrgencyStatusTagStatus: (level: UrgencyLevel): string => urgencyStatusMap[level] || 'default',
  /** 置信度 → 中文标签 */
  toConfidenceLabel: (confidence: ConfidenceLevel): string => confidenceLabelMap[confidence] || confidence,
  /** 置信度 → StatusTag status */
  toConfidenceStatusTagStatus: (confidence: ConfidenceLevel): string => confidenceStatusMap[confidence] || 'default',
  /** 建议状态 → 中文标签 */
  toSuggestionStatusLabel: (status: SuggestionStatus): string => suggestionStatusLabelMap[status] || status,
  /** 建议状态 → StatusTag status */
  toSuggestionStatusTagStatus: (status: SuggestionStatus): string => suggestionStatusMap[status] || 'default',
}

// ============================================================
// 库存分析转换（金额元分转换）
// ============================================================

export const inventoryAnalysisConverter = {
  /** 分 → 元 */
  toYuan: (fen: number | null | undefined): number => fenToYuanNumber(fen),
  /** 元 → 分 */
  toFen: (yuan: number): number => yuanToFen(yuan),
}
