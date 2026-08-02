/**
 * 采购到货单 API + DataConverter
 * 对应后端: /v1/purchase/arrivals
 *
 * DataConverter 处理内容：
 * - 到货单状态数字↔字符串（后端 0~4 ↔ 前端 'pending'/'receiving'/'partial_received'/'received'/'closed'）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 日期 LocalDate/LocalDateTime ↔ string
 */
import { get, post, put } from '../request'
import { fenToYuanNumber as fenToYuan, yuanToFen } from '@/utils/money'

import type {
  PurchaseArrivalInfo,
  PurchaseArrivalItem,
  PurchaseArrivalQueryForm,
  PurchaseArrivalFormData,
  PurchaseArrivalUpdateForm,
  PurchaseArrivalCloseForm,
  PurchaseArrivalStatus,
  ReceiverType,
  ShipmentStatus,
  TransportMode,
  VehicleType,
} from '@/types/purchase-arrival'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的到货单明细实体 */
interface PurchaseArrivalItemBackend {
  arrivalItemId: number
  arrivalId: number
  orderItemId: number | null
  materialId: number | null
  materialName: string | null
  specification: string | null
  unit: string | null
  expectedQuantity: number | null
  receivedQuantity: number | null
  unitPrice: number | null
  amount: number | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
}

/** 后端返回的到货单实体 */
interface PurchaseArrivalBackend {
  arrivalId: number
  arrivalCode: string | null
  orderId: number | null
  orderCode: string | null
  supplierId: number | null
  supplierName: string | null
  receiverType: string | null
  storeId: string | null
  warehouseId: number | null
  shipmentStatus: string | null
  logisticsNo: string | null
  logisticsCompany: string | null
  transportMode: string | null
  vehiclePlateNo: string | null
  vehicleType: string | null
  driverName: string | null
  driverPhone: string | null
  freightAmount: number | null
  estimatedArrivalDate: string | null
  actualArrivalDate: string | null
  totalQuantity: number | null
  totalAmount: number | null
  qualityCheckResult?: number | null
  qualityCheckRemark?: string | null
  confirmTime?: string | null
  receivedQuantity: number | null
  status: number | null
  closeReason: string | null
  remark: string | null
  createUserId: number | null
  createTime: string | null
  updateTime: string | null
  deleted: number
  items?: PurchaseArrivalItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface PurchaseArrivalPageBackend {
  records: PurchaseArrivalBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// ============================================================

const STATUS_TO_FRONTEND: Record<number, PurchaseArrivalStatus> = {
  0: 'pending',
  1: 'receiving',
  2: 'partial_received',
  3: 'received',
  4: 'closed',
}

const STATUS_TO_BACKEND: Record<PurchaseArrivalStatus, number> = {
  pending: 0,
  receiving: 1,
  partial_received: 2,
  received: 3,
  closed: 4,
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购到货单数据转换器（API 层）
 */
export const purchaseArrivalConverter = {
  /** 后端明细实体 → 前端 PurchaseArrivalItem */
  toItemFrontend(backend: PurchaseArrivalItemBackend): PurchaseArrivalItem {
    return {
      arrivalItemId: String(backend.arrivalItemId),
      arrivalId: String(backend.arrivalId),
      orderItemId: backend.orderItemId != null ? String(backend.orderItemId) : '',
      materialId: backend.materialId != null ? String(backend.materialId) : '',
      materialName: backend.materialName ?? '',
      specification: backend.specification ?? undefined,
      unit: backend.unit ?? undefined,
      expectedQuantity: backend.expectedQuantity != null ? Number(backend.expectedQuantity) : 0,
      receivedQuantity: backend.receivedQuantity != null ? Number(backend.receivedQuantity) : 0,
      unitPrice: backend.unitPrice != null ? Number(backend.unitPrice) : 0,
      amount: backend.amount != null ? fenToYuan(backend.amount) : 0,
      remark: backend.remark ?? undefined,
    }
  },

  /** 后端实体 → 前端 PurchaseArrivalInfo */
  toFrontend(backend: PurchaseArrivalBackend): PurchaseArrivalInfo {
    return {
      arrivalId: String(backend.arrivalId),
      arrivalCode: backend.arrivalCode ?? '',
      orderId: backend.orderId != null ? String(backend.orderId) : '',
      orderCode: backend.orderCode ?? undefined,
      supplierId: backend.supplierId != null ? String(backend.supplierId) : undefined,
      supplierName: backend.supplierName ?? undefined,
      receiverType: (backend.receiverType as ReceiverType) ?? 'STORE',
      storeId: backend.storeId ?? undefined,
      warehouseId: backend.warehouseId != null ? String(backend.warehouseId) : undefined,
      shipmentStatus: (backend.shipmentStatus as ShipmentStatus) ?? undefined,
      logisticsNo: backend.logisticsNo ?? undefined,
      logisticsCompany: backend.logisticsCompany ?? undefined,
      transportMode: (backend.transportMode as TransportMode) ?? undefined,
      vehiclePlateNo: backend.vehiclePlateNo ?? undefined,
      vehicleType: (backend.vehicleType as VehicleType) ?? undefined,
      driverName: backend.driverName ?? undefined,
      driverPhone: backend.driverPhone ?? undefined,
      freightAmount: backend.freightAmount != null ? fenToYuan(backend.freightAmount) : 0,
      estimatedArrivalDate: backend.estimatedArrivalDate ?? undefined,
      actualArrivalDate: backend.actualArrivalDate ?? undefined,
      totalQuantity: backend.totalQuantity != null ? Number(backend.totalQuantity) : 0,
      totalAmount: backend.totalAmount != null ? fenToYuan(backend.totalAmount) : 0,
      receivedQuantity: backend.receivedQuantity != null ? Number(backend.receivedQuantity) : 0,
      status: STATUS_TO_FRONTEND[backend.status ?? 0] ?? 'pending',
      closeReason: backend.closeReason ?? undefined,
      remark: backend.remark ?? undefined,
      qualityCheckResult: backend.qualityCheckResult ?? 0,
      qualityCheckRemark: backend.qualityCheckRemark ?? undefined,
      confirmTime: backend.confirmTime ?? undefined,
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
      items: (backend.items ?? []).map(purchaseArrivalConverter.toItemFrontend),
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseArrivalBackend[] | null | undefined): PurchaseArrivalInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseArrivalConverter.toFrontend)
  },

  /** 前端创建表单 → 后端 CreateDTO */
  toCreateDTO(form: PurchaseArrivalFormData): Record<string, unknown> {
    return {
      orderId: form.orderId ? Number(form.orderId) : null,
      shipmentStatus: form.shipmentStatus ?? null,
      logisticsNo: form.logisticsNo ?? null,
      logisticsCompany: form.logisticsCompany ?? null,
      transportMode: form.transportMode ?? null,
      vehiclePlateNo: form.vehiclePlateNo ?? null,
      vehicleType: form.vehicleType ?? null,
      driverName: form.driverName ?? null,
      driverPhone: form.driverPhone ?? null,
      freightAmount: form.freightAmount != null ? yuanToFen(form.freightAmount) : null,
      estimatedArrivalDate: form.estimatedArrivalDate ?? null,
      remark: form.remark ?? null,
    }
  },

  /** 前端更新表单 → 后端 UpdateDTO */
  toUpdateDTO(form: PurchaseArrivalUpdateForm): Record<string, unknown> {
    return {
      shipmentStatus: form.shipmentStatus ?? null,
      logisticsNo: form.logisticsNo ?? null,
      logisticsCompany: form.logisticsCompany ?? null,
      transportMode: form.transportMode ?? null,
      vehiclePlateNo: form.vehiclePlateNo ?? null,
      vehicleType: form.vehicleType ?? null,
      driverName: form.driverName ?? null,
      driverPhone: form.driverPhone ?? null,
      freightAmount: form.freightAmount != null ? yuanToFen(form.freightAmount) : null,
      estimatedArrivalDate: form.estimatedArrivalDate ?? null,
      actualArrivalDate: form.actualArrivalDate ?? null,
      remark: form.remark ?? null,
    }
  },

  /** 前端关闭表单 → 后端 CloseDTO */
  toCloseDTO(form: PurchaseArrivalCloseForm): Record<string, unknown> {
    return {
      closeReason: form.closeReason,
      remark: form.remark ?? null,
    }
  },

  /** 状态字符串 → 后端数字 */
  toBackendStatus(status: PurchaseArrivalStatus): number {
    return STATUS_TO_BACKEND[status] ?? 0
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 采购到货单查询参数（传给后端）
 */
interface PurchaseArrivalQueryParams {
  current: number
  size: number
  arrivalCode?: string
  orderCode?: string
  supplierId?: number
  status?: number
  receiverType?: string
  storeId?: string
  warehouseId?: number
  overdueOnly?: boolean
  estimatedStartDate?: string
  estimatedEndDate?: string
  [key: string]: unknown
}

export const purchaseArrivalApi = {
  /**
   * 分页查询采购到货单
   * @param params 查询参数（含分页）
   */
  async getList(
    params: PurchaseArrivalQueryForm & { page?: number; size?: number },
  ): Promise<{ records: PurchaseArrivalInfo[]; total: number }> {
    const query: PurchaseArrivalQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.arrivalCode) query.arrivalCode = params.arrivalCode
    if (params.orderCode) query.orderCode = params.orderCode
    if (params.supplierId != null && params.supplierId !== '') {
      query.supplierId = Number(params.supplierId)
    }
    if (params.status) query.status = purchaseArrivalConverter.toBackendStatus(params.status)
    if (params.receiverType) query.receiverType = params.receiverType
    if (params.storeId != null && params.storeId !== '') query.storeId = params.storeId
    if (params.warehouseId != null && params.warehouseId !== '') {
      query.warehouseId = Number(params.warehouseId)
    }
    if (params.overdueOnly != null) query.overdueOnly = params.overdueOnly
    if (params.estimatedStartDate) query.estimatedStartDate = params.estimatedStartDate
    if (params.estimatedEndDate) query.estimatedEndDate = params.estimatedEndDate
    if (params.createUserId != null && params.createUserId !== '') {
      query.createUserId = Number(params.createUserId)
    }

    const res = await get<PurchaseArrivalPageBackend | null>('/v1/purchase/arrivals', query)
    const records = purchaseArrivalConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询到货单详情（含明细）
   * @param id 到货单ID
   */
  async getById(id: string): Promise<PurchaseArrivalInfo | null> {
    const res = await get<PurchaseArrivalBackend | null>(`/v1/purchase/arrivals/${id}`)
    return res ? purchaseArrivalConverter.toFrontend(res) : null
  },

  /**
   * 根据采购订单创建到货单
   * @param data 创建表单
   */
  async createFromOrder(data: PurchaseArrivalFormData): Promise<PurchaseArrivalInfo[]> {
    const dto = purchaseArrivalConverter.toCreateDTO(data)
    const res = await post<PurchaseArrivalBackend[] | null>('/v1/purchase/arrivals', dto)
    return purchaseArrivalConverter.toFrontendList(res)
  },

  /**
   * 到货单质检（1通过/2失败）
   * @param id 到货单ID
   * @param result 1通过 2失败
   * @param remark 质检备注（失败原因）
   */
  async qualityCheck(id: string, result: 1 | 2, remark?: string): Promise<PurchaseArrivalInfo> {
    const query = new URLSearchParams({ result: String(result) })
    if (remark) query.set('remark', remark)
    const res = await put<PurchaseArrivalBackend>(`/v1/purchase/arrivals/${id}/quality-check?${query.toString()}`)
    return purchaseArrivalConverter.toFrontend(res)
  },

  /**
   * 到货单确认入库：加库存（仓库+门店/unit_cost）+ 应付 + 回写订单实收 + 追溯码事件
   * @param id 到货单ID
   */
  async confirmArrival(id: string): Promise<PurchaseArrivalInfo> {
    const res = await put<PurchaseArrivalBackend>(`/v1/purchase/arrivals/${id}/confirm`)
    return purchaseArrivalConverter.toFrontend(res)
  },

  /**
   * 更新到货单物流信息
   * @param id 到货单ID
   * @param data 更新表单
   */
  async update(id: string, data: PurchaseArrivalUpdateForm): Promise<PurchaseArrivalInfo> {
    const dto = purchaseArrivalConverter.toUpdateDTO(data)
    const res = await put<PurchaseArrivalBackend>(`/v1/purchase/arrivals/${id}`, dto)
    return purchaseArrivalConverter.toFrontend(res)
  },

  /**
   * 手动关闭待收货到货单
   * @param id 到货单ID
   * @param data 关闭表单
   */
  async close(id: string, data: PurchaseArrivalCloseForm): Promise<PurchaseArrivalInfo> {
    const dto = purchaseArrivalConverter.toCloseDTO(data)
    const res = await put<PurchaseArrivalBackend>(`/v1/purchase/arrivals/${id}/close`, dto)
    return purchaseArrivalConverter.toFrontend(res)
  },
}

export default purchaseArrivalApi
