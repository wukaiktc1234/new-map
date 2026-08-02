/**
 * 采购订单 API + DataConverter
 * 对应后端: /v1/purchase/orders
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串（后端 orderStatus 0~5 ↔ 前端语义化字符串）
 * - 付款状态数字↔字符串（后端 paymentStatus 0~2 ↔ 前端语义化字符串）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 日期 LocalDate/LocalDateTime ↔ string
 * - 字段名映射（后端 orderCode ↔ 前端 orderNo、后端 approvalRemark ↔ 前端 rejectReason 等）
 */
import { get, post, put, del } from '../request'
// 金额转换统一委托给 utils/money（fenToYuanNumber 别名 fenToYuan 保留 2 位小数精度）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  PurchaseOrderInfo,
  PurchaseOrderItemInfo,
  PurchaseOrderQueryForm,
  PurchaseOrderStatus,
  PaymentStatus,
} from '@/types/purchase-order'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的采购订单明细实体 */
interface PurchaseOrderItemBackend {
  itemId: number
  orderId: number
  materialId: number | null
  materialName: string
  specification: string | null
  unit: string
  quantity: number
  unitPrice: number  // 单位：分
  amount: number     // 单位：分
  taxRate: number | null
  receivedQuantity: number | null
  remark: string | null
  plannedReceiverType: string | null
  plannedStoreId: string | null
  plannedWarehouseId: number | null
  createTime: string | null
  updateTime: string | null
}

/** 后端返回的采购订单实体（Controller 直接返回实体） */
interface PurchaseOrderBackend {
  orderId: number
  orderCode: string | null
  supplierId: number | null
  supplierName: string | null
  warehouseId: number | null
  totalAmount: number       // 单位：分
  taxAmount: number | null   // 单位：分
  discountAmount: number | null  // 单位：分
  finalAmount: number | null     // 单位：分
  orderStatus: number       // 0~6
  paymentStatus: number     // 0~2
  expectedDate: string | null   // yyyy-MM-dd
  orderDate: string | null       // yyyy-MM-dd
  storeId: number | null
  requestId: string | null
  requestNo: string | null
  contractId: string | null
  contractNo: string | null
  sourceType: string | null
  priority: string | null
  // 后端已持久化：PurchaseOrderCreateDTO / PurchaseOrderServiceImpl.createOrder 均写入
  purchaseType: string | null
  contactPerson: string | null
  contactPhone: string | null
  budgetId: string | null
  budgetStatus: string | null
  // 关联采购计划ID（可选，从计划创建订单时填入）
  planId: number | null
  // paidAmount 当前由财务付款回写触发（待 LK-FINANCE-02 修复后联动）
  paidAmount: number | null
  approvalUserId: number | null
  approvalTime: string | null   // yyyy-MM-dd HH:mm:ss
  approvalRemark: string | null
  remark: string | null
  createUserId: number | null
  createByName: string | null
  createTime: string | null     // yyyy-MM-dd HH:mm:ss
  updateTime: string | null      // yyyy-MM-dd HH:mm:ss
  // 后端实体未持久化以下字段，前端使用 ?? '' 兜底；如需展示需后端补字段
  updateBy: string | null
  deleted: number
  deletedTime: string | null
  deletedBy: string | null
  items?: PurchaseOrderItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface PurchaseOrderPageBackend {
  records: PurchaseOrderBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端 orderStatus：0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消 6已下单
// 前端 PurchaseOrderStatus 含 11 个值，后端支持 7 个
// 未支持的前端状态按业务语义就近映射：shipped→已审核、received→已完成、rejected/terminated→已取消
// ============================================================

const STATUS_TO_FRONTEND: Record<number, PurchaseOrderStatus> = {
  0: 'draft',
  1: 'pending',
  2: 'approved',
  3: 'partial_received',
  4: 'completed',
  5: 'cancelled',
  6: 'ordered',
  7: 'rejected',
}

const STATUS_TO_BACKEND: Record<PurchaseOrderStatus, number> = {
  draft: 0,
  pending: 1,
  approved: 2,
  ordered: 6,
  shipped: 2,           // 后端无此状态，映射到已审核
  received: 4,          // 后端无此状态，映射到已完成
  rejected: 7,         // 已驳回（可修改后重新提交）
  partial_received: 3,
  completed: 4,
  terminated: 5,        // 后端无此状态，映射到已取消
  cancelled: 5,
}

const PAYMENT_STATUS_TO_FRONTEND: Record<number, PaymentStatus> = {
  0: 'unpaid',
  1: 'partial',
  2: 'paid',
}

const PAYMENT_STATUS_TO_BACKEND: Record<PaymentStatus, number> = {
  unpaid: 0,
  partial: 1,
  paid: 2,
}

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money，见文件顶部 import）
// ============================================================

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购订单数据转换器（API 层，处理后端实体 ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 purchaseOrderConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 order.ts 内部使用，不通过 index.ts 导出。
 */
export const purchaseOrderConverter = {
  /** 后端明细实体 → 前端 PurchaseOrderItemInfo */
  toItemFrontend(backend: PurchaseOrderItemBackend): PurchaseOrderItemInfo {
    return {
      id: backend.itemId,
      purchaseOrderId: String(backend.orderId),
      materialId: backend.materialId != null ? String(backend.materialId) : '',
      materialName: backend.materialName,
      specification: backend.specification ?? '',
      unit: backend.unit,
      quantity: Number(backend.quantity),
      unitPrice: fenToYuan(backend.unitPrice),
      amount: fenToYuan(backend.amount),
      receivedQuantity: backend.receivedQuantity != null ? Number(backend.receivedQuantity) : 0,
      remark: backend.remark ?? '',
      plannedReceiverType: (backend.plannedReceiverType as 'STORE' | 'WAREHOUSE') ?? undefined,
      plannedStoreId: backend.plannedStoreId ?? undefined,
      plannedWarehouseId: backend.plannedWarehouseId != null ? String(backend.plannedWarehouseId) : undefined,
    }
  },

  /** 后端实体 → 前端 PurchaseOrderInfo */
  toFrontend(backend: PurchaseOrderBackend): PurchaseOrderInfo {
    return {
      purchaseOrderId: String(backend.orderId),
      orderNo: backend.orderCode ?? '',
      supplierId: backend.supplierId != null ? String(backend.supplierId) : '',
      supplierName: backend.supplierName ?? '',
      warehouseId: backend.warehouseId != null ? String(backend.warehouseId) : '',
      // 后端已持久化字段：requestId/requestNo/contractId/contractNo/sourceType/priority
      requestId: backend.requestId ?? '',
      requestNo: backend.requestNo ?? '',
      contractId: backend.contractId ?? '',
      contractNo: backend.contractNo ?? '',
      sourceType: backend.sourceType ?? '',
      priority: backend.priority ?? '',
      // 后端已持久化字段，缺失时兜底为空
      purchaseType: backend.purchaseType ?? '',
      contactPerson: backend.contactPerson ?? '',
      contactPhone: backend.contactPhone ?? '',
      budgetId: backend.budgetId ?? '',
      budgetStatus: backend.budgetStatus ?? '',
      planId: backend.planId ?? undefined,
      totalAmount: fenToYuan(backend.totalAmount),
      status: STATUS_TO_FRONTEND[backend.orderStatus] ?? 'draft',
      orderDate: backend.orderDate ?? '',
      expectedDate: backend.expectedDate ?? '',
      remark: backend.remark ?? '',
      rejectReason: backend.approvalRemark ?? '',
      paymentStatus: PAYMENT_STATUS_TO_FRONTEND[backend.paymentStatus] ?? 'unpaid',
      // paidAmount 当前仅在财务付款回写后才有值；缺失时兜底为 0
      paidAmount: backend.paidAmount != null ? fenToYuan(backend.paidAmount) : 0,
      items: (backend.items ?? []).map(purchaseOrderConverter.toItemFrontend),
      createTime: backend.createTime ?? '',
      createByName: backend.createByName ?? (backend.createUserId != null ? String(backend.createUserId) : ''),
      updateTime: backend.updateTime ?? '',
      // 后端实体未持久化以下字段，兜底为空
      updateBy: backend.updateBy ?? '',
      deletedTime: backend.deletedTime ?? '',
      deletedBy: backend.deletedBy ?? '',
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseOrderBackend[] | null | undefined): PurchaseOrderInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseOrderConverter.toFrontend)
  },

  /** 前端表单数据 → 后端 CreateDTO/UpdateDTO 格式 */
  toCreateDTO(form: Partial<PurchaseOrderInfo>): Record<string, unknown> {
    const items = (form.items ?? []).map(item => ({
      materialId: item.materialId ? Number(item.materialId) : null,
      materialName: item.materialName,
      specification: item.specification ?? null,
      unit: item.unit,
      quantity: item.quantity,
      unitPrice: yuanToFen(item.unitPrice),
      remark: item.remark ?? null,
      plannedReceiverType: item.plannedReceiverType ?? null,
      plannedStoreId: item.plannedStoreId ?? null,
      plannedWarehouseId: item.plannedWarehouseId != null ? Number(item.plannedWarehouseId) : null,
    }))
    return {
      supplierId: form.supplierId ? Number(form.supplierId) : null,
      // 后端实体有 warehouseId 但前端表单不含此字段，传 null
      warehouseId: null,
      discountAmount: 0,
      expectedDate: form.expectedDate ?? null,
      remark: form.remark ?? null,
      // 关联采购计划ID（可选，从计划创建订单时填入）
      planId: form.planId ?? null,
      items,
    }
  },

  /** 状态字符串 → 后端数字 */
  toBackendStatus(status: PurchaseOrderStatus): number {
    return STATUS_TO_BACKEND[status] ?? 0
  },

  /** 付款状态字符串 → 后端数字 */
  toBackendPaymentStatus(status: PaymentStatus): number {
    return PAYMENT_STATUS_TO_BACKEND[status] ?? 0
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 采购订单查询参数（传给后端）
 */
interface PurchaseOrderQueryParams {
  current: number
  size: number
  orderCode?: string
  requestNo?: string
  supplierId?: number
  orderStatus?: number
  paymentStatus?: number
  startDate?: string
  endDate?: string
  createUserId?: number
  [key: string]: unknown
}

export const purchaseOrderApi = {
  /**
   * 分页查询采购订单列表
   * @param params 查询参数（含分页）
   */
  async getList(params: PurchaseOrderQueryForm & { page?: number; size?: number; supplierId?: string | number }): Promise<{ records: PurchaseOrderInfo[]; total: number }> {
    const query: PurchaseOrderQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.orderNo) query.orderCode = params.orderNo
    if (params.requestNo) query.requestNo = params.requestNo
    if (params.supplierId != null && params.supplierId !== '') {
      query.supplierId = Number(params.supplierId)
    }
    if (params.status) query.orderStatus = purchaseOrderConverter.toBackendStatus(params.status)
    if (params.paymentStatus) query.paymentStatus = purchaseOrderConverter.toBackendPaymentStatus(params.paymentStatus)
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate
    if (params.createUserId != null && params.createUserId !== '') {
      query.createUserId = Number(params.createUserId)
    }

    const res = await get<PurchaseOrderPageBackend | null>('/v1/purchase/orders', query)
    const records = purchaseOrderConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询采购订单详情（含明细）
   * @param id 订单ID
   */
  async getById(id: string): Promise<PurchaseOrderInfo | null> {
    const res = await get<PurchaseOrderBackend | null>(`/v1/purchase/orders/${id}`)
    return res ? purchaseOrderConverter.toFrontend(res) : null
  },

  /**
   * 创建采购订单（草稿状态）
   * @param data 表单数据
   */
  async create(data: Partial<PurchaseOrderInfo>): Promise<PurchaseOrderInfo[]> {
    const dto = purchaseOrderConverter.toCreateDTO(data)
    const res = await post<PurchaseOrderBackend[] | null>('/v1/purchase/orders', dto)
    return purchaseOrderConverter.toFrontendList(res || [])
  },

  /**
   * 更新采购订单（仅草稿/已取消状态可更新）
   * @param id 订单ID
   * @param data 表单数据
   */
  async update(id: string, data: Partial<PurchaseOrderInfo>): Promise<PurchaseOrderInfo> {
    const dto = purchaseOrderConverter.toCreateDTO(data)
    const res = await put<PurchaseOrderBackend>(`/v1/purchase/orders/${id}`, dto)
    return purchaseOrderConverter.toFrontend(res)
  },

  /**
   * 删除采购订单
   * @param id 订单ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/orders/${id}`)
  },

  /**
   * 提交审批（草稿 → 待审核）
   * @param id 订单ID
   */
  async submit(id: string): Promise<void> {
    await put<void>(`/v1/purchase/orders/${id}/submit`)
  },

  /**
   * 审批通过（待审核 → 已审核）
   * @param id 订单ID
   * @param approvalRemark 审批备注（可选）
   */
  async approve(id: string, approvalRemark?: string): Promise<void> {
    const url = approvalRemark
      ? `/v1/purchase/orders/${id}/approve?approvalRemark=${encodeURIComponent(approvalRemark)}`
      : `/v1/purchase/orders/${id}/approve`
    await put<void>(url)
  },

  /**
   * 确认下单（已审核 → 已下单）
   * @param id 订单ID
   */
  async confirmOrder(id: string): Promise<void> {
    await put<void>(`/v1/purchase/orders/${id}/confirm`)
  },

  /**
   * 审批拒绝（待审核 → 已取消，记录拒绝原因）
   * @param id 订单ID
   * @param reason 拒绝原因
   */
  async reject(id: string, reason: string): Promise<void> {
    await put<void>(`/v1/purchase/orders/${id}/reject?reason=${encodeURIComponent(reason)}`)
  },

  /**
   * 取消订单
   * @param id 订单ID
   * @param reason 取消原因
   */
  async cancel(id: string, reason: string): Promise<void> {
    await put<void>(`/v1/purchase/orders/${id}/cancel?reason=${encodeURIComponent(reason)}`)
  },
}

export default purchaseOrderApi
