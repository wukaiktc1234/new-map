/**
 * 订单管理模块数据转换器
 *
 * 负责后端数据与前端展示数据之间的转换：
 * - 金额：后端分（number） ↔ 前端元（string）
 * - 状态：后端数字编码 ↔ 前端语义化字符串
 *
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 */

import { fenToYuan as formatPrice, yuanToFen } from '@/utils/money'
import type {
  OrderBackend,
  Order,
  OrderItemBackend,
  OrderItem,
  OrderPaymentBackend,
  OrderPayment,
  OrderRefundRecordBackend,
  OrderRefundRecord,
  OrderStatusValue,
  OrderStatsBackend,
  OrderStats,
  OrderRefundBackend,
  OrderRefund,
  OrderRefundStatsBackend,
  OrderRefundStats,
  RefundStatusValue,
  ReservationBackend,
  Reservation,
  ReservationStatusValue,
  DailyStatsBackend,
  DailyStats,
} from '@/types/order'

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money）
// ============================================================

/**
 * 分转元（保留2位小数）
 * @param fen - 分（后端 Long/number）
 * @returns 元字符串，两位小数
 */
// formatPrice 直接复用 utils/money 的 fenToYuan（同名语义）

/**
 * 元转分
 * @param yuan - 元字符串
 * @returns 分（整数）
 */
// yuanToFen 直接复用 utils/money 的同名函数

// ============================================================
// 订单状态映射
// 后端：0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款 6待评价
// ============================================================

const ORDER_STATUS_TO_FRONTEND: Record<number, OrderStatusValue> = {
  0: 'pending',
  1: 'confirmed',
  2: 'completed',
  3: 'cancelled',
  4: 'partial_refund',
  5: 'full_refund',
  6: 'pending_review',
}

const ORDER_STATUS_TO_BACKEND: Record<OrderStatusValue, number> = {
  pending: 0,
  confirmed: 1,
  completed: 2,
  cancelled: 3,
  partial_refund: 4,
  full_refund: 5,
  pending_review: 6,
}

/** 订单状态：后端编码 → 前端语义化字符串 */
function orderStatusToFrontend(code: number): OrderStatusValue {
  return ORDER_STATUS_TO_FRONTEND[code] ?? 'pending'
}

/** 订单状态：前端语义化字符串 → 后端编码 */
function orderStatusToBackendCode(status: OrderStatusValue): number {
  return ORDER_STATUS_TO_BACKEND[status] ?? 0
}

// ============================================================
// 退款状态映射
// 后端：0待审核 1已同意 2已拒绝 3已退款（与 order_refund_records.refund_status 注释一致）
// ============================================================

const REFUND_STATUS_TO_FRONTEND: Record<number, RefundStatusValue> = {
  0: 'pending',
  1: 'approved',
  2: 'rejected',
  3: 'executed',
}

const REFUND_STATUS_TO_BACKEND: Record<RefundStatusValue, number> = {
  pending: 0,
  approved: 1,
  rejected: 2,
  executed: 3,
}

/** 退款状态：后端编码 → 前端语义化字符串 */
function refundStatusToFrontend(code: number): RefundStatusValue {
  return REFUND_STATUS_TO_FRONTEND[code] ?? 'pending'
}

/** 退款状态：前端语义化字符串 → 后端编码 */
function refundStatusToBackendCode(status: RefundStatusValue): number {
  return REFUND_STATUS_TO_BACKEND[status] ?? 0
}

// ============================================================
// 预约状态映射
// 后端：1待确认 2已确认 3已到店 4已取消 5未到
// ============================================================

const RESERVATION_STATUS_TO_FRONTEND: Record<number, ReservationStatusValue> = {
  1: 'pending',
  2: 'confirmed',
  3: 'arrived',
  4: 'cancelled',
  5: 'no_show',
}

const RESERVATION_STATUS_TO_BACKEND: Record<ReservationStatusValue, number> = {
  pending: 1,
  confirmed: 2,
  arrived: 3,
  cancelled: 4,
  no_show: 5,
}

/** 预约状态：后端编码 → 前端语义化字符串 */
function reservationStatusToFrontend(code: number): ReservationStatusValue {
  return RESERVATION_STATUS_TO_FRONTEND[code] ?? 'pending'
}

/** 预约状态：前端语义化字符串 → 后端编码 */
function reservationStatusToBackendCode(status: ReservationStatusValue): number {
  return RESERVATION_STATUS_TO_BACKEND[status] ?? 1
}

// ============================================================
// 订单明细 / 支付 / 退款记录转换
// ============================================================

function itemToFrontend(item: OrderItemBackend): OrderItem {
  return {
    itemId: item.itemId,
    productName: item.productName,
    unitPrice: formatPrice(item.unitPrice),
    quantity: item.quantity,
    amount: formatPrice(item.amount),
    kitchenStatusName: item.kitchenStatusName,
  }
}

function paymentToFrontend(payment: OrderPaymentBackend): OrderPayment {
  return {
    paymentMethodName: payment.paymentMethodName,
    paymentAmount: formatPrice(payment.paymentAmount),
    paymentTime: payment.paymentTime,
    operatorName: payment.operatorName,
  }
}

function refundRecordToFrontend(record: OrderRefundRecordBackend): OrderRefundRecord {
  return {
    refundId: record.refundId,
    refundAmount: formatPrice(record.refundAmount),
    refundReason: record.refundReason,
    refundStatusName: record.refundStatusName,
    approveUserName: record.approveUserName,
    createTime: record.createTime,
    completeTime: record.completeTime,
  }
}

// ============================================================
// 订单转换
// ============================================================

/** 后端订单 → 前端订单（金额分转元，状态数字转语义化字符串） */
function toFrontend(backend: OrderBackend): Order {
  return {
    orderId: backend.orderId,
    orderCode: backend.orderCode,
    orderType: backend.orderType,
    orderTypeName: backend.orderTypeName,
    storeId: backend.storeId,
    storeName: backend.storeName,
    customerId: backend.customerId,
    customerName: backend.customerName,
    customerPhone: backend.customerPhone,
    tableId: backend.tableId,
    tableName: backend.tableName,
    diningPeopleCount: backend.diningPeopleCount,
    orderStatus: orderStatusToFrontend(backend.orderStatus),
    orderStatusName: backend.orderStatusName,
    paymentStatus: backend.paymentStatus as Order['paymentStatus'],
    paymentStatusName: backend.paymentStatusName,
    totalAmount: formatPrice(backend.totalAmount),
    discountAmount: formatPrice(backend.discountAmount),
    finalAmount: formatPrice(backend.finalAmount),
    paidAmount: formatPrice(backend.paidAmount),
    refundAmount: formatPrice(backend.refundAmount),
    remark: backend.remark,
    cancelReason: backend.cancelReason,
    cashierUserName: backend.cashierUserName,
    createTime: backend.createTime,
    updateTime: backend.updateTime,
    items: backend.items?.map(itemToFrontend),
    payments: backend.payments?.map(paymentToFrontend),
    refunds: backend.refunds?.map(refundRecordToFrontend),
  }
}

/** 后端订单列表 → 前端订单列表 */
function toFrontendList(list: OrderBackend[] | null | undefined): Order[] {
  if (!list || !Array.isArray(list)) return []
  return list.map(toFrontend)
}

// ============================================================
// 今日统计转换
// ============================================================

/** 今日销售统计后端 → 前端（金额分转元） */
function statsToFrontend(backend: OrderStatsBackend | null | undefined): OrderStats {
  const totalOrders = backend?.totalOrders ?? 0
  const totalSales = backend?.totalSalesAmount ?? 0
  // 待处理订单 = 总订单 - 已完成 - 已取消
  const pendingOrders = Math.max(
    0,
    totalOrders - (backend?.completedOrders ?? 0) - (backend?.cancelledOrders ?? 0)
  )
  const avgOrderAmount = totalOrders > 0 ? totalSales / totalOrders : 0
  return {
    todayOrders: totalOrders,
    todayRevenue: formatPrice(totalSales),
    pendingOrders,
    avgOrderAmount: formatPrice(avgOrderAmount),
    completedOrders: backend?.completedOrders ?? 0,
  }
}

// ============================================================
// 退款转换
// ============================================================

/** 后端退款 → 前端退款（金额分转元，状态数字转语义化字符串） */
function refundToFrontend(backend: OrderRefundBackend): OrderRefund {
  return {
    refundId: backend.refundId,
    refundNo: backend.refundNo,
    orderId: backend.orderId,
    orderCode: backend.orderCode,
    refundAmount: formatPrice(backend.refundAmount),
    refundReason: backend.refundReason,
    refundStatus: refundStatusToFrontend(backend.refundStatus),
    refundStatusName: backend.refundStatusName,
    applyUserName: backend.applyUserName,
    approveUserName: backend.approveUserName,
    rejectReason: backend.rejectReason,
    createTime: backend.createTime,
    completeTime: backend.completeTime,
  }
}

/** 后端退款列表 → 前端退款列表 */
function refundToFrontendList(list: OrderRefundBackend[] | null | undefined): OrderRefund[] {
  if (!list || !Array.isArray(list)) return []
  return list.map(refundToFrontend)
}

/** 退款统计后端 → 前端（金额分转元） */
function refundStatsToFrontend(backend: OrderRefundStatsBackend | null | undefined): OrderRefundStats {
  return {
    pendingCount: backend?.pendingCount ?? 0,
    refundedAmount: formatPrice(backend?.refundedAmount),
    refundRate: backend?.refundRate ?? 0,
    avgProcessHours: backend?.avgProcessHours ?? 0,
  }
}

// ============================================================
// 预约转换
// ============================================================

/** 后端预约 → 前端预约（金额分转元，状态数字转语义化字符串） */
function reservationToFrontend(backend: ReservationBackend): Reservation {
  return {
    reservationId: backend.reservationId,
    reservationCode: backend.reservationCode,
    customerName: backend.customerName,
    customerPhone: backend.customerPhone,
    tableId: backend.tableId,
    storeId: backend.storeId,
    reservationDate: backend.reservationDate,
    reservationTime: backend.reservationTime,
    peopleCount: backend.peopleCount,
    depositAmount: formatPrice(backend.depositAmount),
    remark: backend.remark,
    status: reservationStatusToFrontend(backend.status),
    createTime: backend.createTime,
    confirmTime: backend.confirmTime,
    arriveTime: backend.arriveTime,
    cancelTime: backend.cancelTime,
  }
}

/** 后端预约列表 → 前端预约列表 */
function reservationToFrontendList(list: ReservationBackend[] | null | undefined): Reservation[] {
  if (!list || !Array.isArray(list)) return []
  return list.map(reservationToFrontend)
}

// ============================================================
// 按日期分组统计转换
// ============================================================

/** 按日期分组统计后端 → 前端（金额分转元） */
function dailyStatsToFrontend(backend: DailyStatsBackend): DailyStats {
  return {
    date: backend.date,
    storeName: backend.storeName,
    orderCount: backend.orderCount,
    revenue: formatPrice(backend.revenue),
    cost: formatPrice(backend.cost),
    profit: formatPrice(backend.profit),
    profitRate: backend.profitRate,
  }
}

// ============================================================
// 统一导出
// ============================================================

export const orderDataConverter = {
  // 金额转换
  formatPrice,
  yuanToFen,
  // 订单状态映射
  orderStatusToFrontend,
  orderStatusToBackendCode,
  // 退款状态映射
  refundStatusToFrontend,
  refundStatusToBackendCode,
  // 预约状态映射
  reservationStatusToFrontend,
  reservationStatusToBackendCode,
  // 订单转换
  toFrontend,
  toFrontendList,
  statsToFrontend,
  // 退款转换
  refundToFrontend,
  refundToFrontendList,
  refundStatsToFrontend,
  // 预约转换
  reservationToFrontend,
  reservationToFrontendList,
  // 日期统计转换
  dailyStatsToFrontend,
}

export default orderDataConverter
