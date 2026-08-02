/**
 * 收货确认单 API + DataConverter
 * 对应后端: /v1/receipt-confirmations
 *
 * 门店收货与库存入库共用，负责实物确认、库存增加、应付生成。
 *
 * DataConverter 处理内容：
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 日期 LocalDate/LocalDateTime ↔ string
 */
import { get, post } from './request'
import { fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  ReceiptConfirmationInfo,
  ReceiptConfirmationItem,
  ReceiptConfirmationQueryForm,
  ReceiptConfirmationFormData,
  ReceiverType,
} from '@/types/purchase-arrival'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的收货确认单明细实体 */
interface ReceiptConfirmationItemBackend {
  confirmationItemId: number
  confirmationId: number
  arrivalItemId: number | null
  orderItemId: number | null
  materialId: number | null
  materialName: string | null
  specification: string | null
  unit: string | null
  confirmedQuantity: number | null
  rejectedQuantity: number | null
  unitPrice: number | null
  amount: number | null
  batchNo: string | null
  productionDate: string | null
  expiryDate: string | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
}

/** 后端返回的收货确认单实体 */
interface ReceiptConfirmationBackend {
  confirmationId: number
  confirmationCode: string | null
  receiptSource: string | null
  arrivalId: number | null
  orderId: number | null
  receiverType: string | null
  storeId: string | null
  warehouseId: number | null
  confirmUserId: number | null
  confirmTime: string | null
  totalQuantity: number | null
  totalAmount: number | null
  qualityCheckResult: number | null
  qualityRemark: string | null
  status: number | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
  deleted: number
  items?: ReceiptConfirmationItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface ReceiptConfirmationPageBackend {
  records: ReceiptConfirmationBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 收货确认单数据转换器（API 层）
 */
export const receiptConfirmationConverter = {
  /** 后端明细实体 → 前端 ReceiptConfirmationItem */
  toItemFrontend(backend: ReceiptConfirmationItemBackend): ReceiptConfirmationItem {
    return {
      confirmationItemId: String(backend.confirmationItemId),
      confirmationId: String(backend.confirmationId),
      arrivalItemId: backend.arrivalItemId != null ? String(backend.arrivalItemId) : '',
      orderItemId: backend.orderItemId != null ? String(backend.orderItemId) : '',
      materialId: backend.materialId != null ? String(backend.materialId) : '',
      materialName: backend.materialName ?? '',
      specification: backend.specification ?? undefined,
      unit: backend.unit ?? undefined,
      confirmedQuantity: backend.confirmedQuantity != null ? Number(backend.confirmedQuantity) : 0,
      rejectedQuantity: backend.rejectedQuantity != null ? Number(backend.rejectedQuantity) : 0,
      unitPrice: backend.unitPrice != null ? Number(backend.unitPrice) : 0,
      amount: backend.amount != null ? fenToYuan(backend.amount) : 0,
      batchNo: backend.batchNo ?? undefined,
      productionDate: backend.productionDate ?? undefined,
      expiryDate: backend.expiryDate ?? undefined,
      remark: backend.remark ?? undefined,
    }
  },

  /** 后端实体 → 前端 ReceiptConfirmationInfo */
  toFrontend(backend: ReceiptConfirmationBackend): ReceiptConfirmationInfo {
    return {
      confirmationId: String(backend.confirmationId),
      confirmationCode: backend.confirmationCode ?? '',
      receiptSource: backend.receiptSource === 'direct' ? 'direct' : 'arrival',
      arrivalId: backend.arrivalId != null ? String(backend.arrivalId) : '',
      orderId: backend.orderId != null ? String(backend.orderId) : '',
      receiverType: (backend.receiverType as ReceiverType) ?? 'STORE',
      storeId: backend.storeId ?? undefined,
      warehouseId: backend.warehouseId != null ? String(backend.warehouseId) : undefined,
      confirmUserId: backend.confirmUserId != null ? String(backend.confirmUserId) : undefined,
      confirmTime: backend.confirmTime ?? '',
      totalQuantity: backend.totalQuantity != null ? Number(backend.totalQuantity) : 0,
      totalAmount: backend.totalAmount != null ? fenToYuan(backend.totalAmount) : 0,
      qualityCheckResult: backend.qualityCheckResult ?? undefined,
      qualityRemark: backend.qualityRemark ?? undefined,
      status: backend.status ?? 1,
      remark: backend.remark ?? undefined,
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
      items: (backend.items ?? []).map(receiptConfirmationConverter.toItemFrontend),
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: ReceiptConfirmationBackend[] | null | undefined): ReceiptConfirmationInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(receiptConfirmationConverter.toFrontend)
  },

  /** 前端创建表单 → 后端 CreateDTO */
  toCreateDTO(form: ReceiptConfirmationFormData): Record<string, unknown> {
    const items = (form.items ?? []).map(item => ({
      arrivalItemId: item.arrivalItemId ? Number(item.arrivalItemId) : null,
      confirmedQuantity: item.confirmedQuantity,
      rejectedQuantity: item.rejectedQuantity ?? 0,
      batchNo: item.batchNo ?? null,
      productionDate: item.productionDate ?? null,
      expiryDate: item.expiryDate ?? null,
      remark: item.remark ?? null,
    }))
    return {
      arrivalId: form.arrivalId ? Number(form.arrivalId) : null,
      items,
      qualityCheckResult: form.qualityCheckResult ?? null,
      qualityRemark: form.qualityRemark ?? null,
      remark: form.remark ?? null,
    }
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 收货确认单查询参数（传给后端）
 */
interface ReceiptConfirmationQueryParams {
  current: number
  size: number
  confirmationCode?: string
  arrivalId?: number
  orderId?: number
  receiverType?: string
  storeId?: string
  warehouseId?: number
  [key: string]: unknown
}

export const receiptConfirmationApi = {
  /**
   * 分页查询收货确认单
   * @param params 查询参数（含分页）
   */
  async getList(
    params: ReceiptConfirmationQueryForm & { page?: number; size?: number },
  ): Promise<{ records: ReceiptConfirmationInfo[]; total: number }> {
    const query: ReceiptConfirmationQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.confirmationCode) query.confirmationCode = params.confirmationCode
    if (params.arrivalId != null && params.arrivalId !== '') {
      query.arrivalId = Number(params.arrivalId)
    }
    if (params.orderId != null && params.orderId !== '') {
      query.orderId = Number(params.orderId)
    }
    if (params.receiverType) query.receiverType = params.receiverType
    if (params.storeId != null && params.storeId !== '') query.storeId = params.storeId
    if (params.warehouseId != null && params.warehouseId !== '') {
      query.warehouseId = Number(params.warehouseId)
    }

    const res = await get<ReceiptConfirmationPageBackend | null>('/v1/receipt-confirmations', query)
    const records = receiptConfirmationConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询收货确认单详情（含明细）
   * @param id 确认单ID
   */
  async getById(id: string): Promise<ReceiptConfirmationInfo | null> {
    const res = await get<ReceiptConfirmationBackend | null>(`/v1/receipt-confirmations/${id}`)
    return res ? receiptConfirmationConverter.toFrontend(res) : null
  },

  /**
   * 创建收货确认单
   * @param data 创建表单
   */
  async create(data: ReceiptConfirmationFormData): Promise<ReceiptConfirmationInfo> {
    const dto = receiptConfirmationConverter.toCreateDTO(data)
    const res = await post<ReceiptConfirmationBackend>('/v1/receipt-confirmations', dto)
    return receiptConfirmationConverter.toFrontend(res)
  },
}

export default receiptConfirmationApi
