/**
 * 采购收货 API + DataConverter
 * 对应后端: /v1/purchase/stockins
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串（后端 status 0~2 + qualityCheckResult 1~3 ↔ 前端 'pending'/'inspecting'/'qualified'/'unqualified'/'partial'/'completed'）
 * - 入库类型数字↔收货方式字符串（后端 stockinType 1~3 ↔ 前端 'warehouse_direct'/'store_direct'/'batch'）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 字段名映射（后端 stockinCode ↔ 前端 stockinNo、后端 qualityRemark ↔ 前端 remark、后端 qualityCheckUserId ↔ 前端 inspectorId、后端 warehouseId ↔ 前端 deliveryWarehouse 等）
 * - 后端无字段时前端返回默认值（orderNo/supplierName/inspectorName/deliveryStore/receiverName 等）
 */
import { get, post, put, del } from '../request'
// 金额转换统一委托给 utils/money（fenToYuanNumber 别名 fenToYuan 保留 2 位小数精度）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  PurchaseStockinInfo,
  PurchaseStockinItem,
  PurchaseStockinQueryForm,
  PurchaseStockinFormData,
  PurchaseStockinStatus,
  DeliveryMethod,
} from '@/types/purchase-stockin'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的入库明细实体 */
interface PurchaseStockinItemBackend {
  stockinItemId: number
  stockinId: number
  orderItemId: number | null
  materialId: number | null
  materialName: string | null
  batchNo: string | null
  productionDate: string | null   // yyyy-MM-dd
  expiryDate: string | null        // yyyy-MM-dd
  actualQuantity: number | null    // BigDecimal
  unit: string | null
  unitPrice: number | null          // 分
  amount: number | null             // 分
  locationId: number | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
}

/** 后端返回的入库单实体（Controller 直接返回实体） */
interface PurchaseStockinBackend {
  stockinId: number
  stockinCode: string | null
  orderNo: string | null
  supplierName: string | null
  orderId: number | null
  supplierId: number | null
  warehouseId: number | null
  stockinDate: string | null        // yyyy-MM-dd
  stockinType: number | null         // 1/2/3
  totalQuantity: number | null       // BigDecimal
  totalAmount: number | null         // 分
  qualityCheckResult: number | null  // 1/2/3
  qualityCheckUserId: number | null
  qualityCheckTime: string | null   // yyyy-MM-dd HH:mm:ss
  qualityRemark: string | null
  status: number | null              // 0/1/2
  createUserId: number | null
  createTime: string | null
  updateTime: string | null
  deleted: number
  items?: PurchaseStockinItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface PurchaseStockinPageBackend {
  records: PurchaseStockinBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端 status：0待入库 1已入库 2部分入库
// 后端 qualityCheckResult：1合格 2不合格 3待检
// 前端 6 个状态合并表示，由 status + qualityCheckResult 联合推断
// ============================================================

/** 状态字符串 → 后端 status（用于查询参数）
 * inspecting/qualified/unqualified 都属于 status=0 待入库
 */
const STATUS_TO_BACKEND: Record<PurchaseStockinStatus, number> = {
  pending: 0,
  inspecting: 0,
  qualified: 0,
  unqualified: 0,
  partial: 2,
  completed: 1,
}

/** 入库类型数字 → 前端 deliveryMethod 字符串
 * 后端 stockinType：1正常入库 2退货入库 3赠品入库
 * 前端 deliveryMethod：warehouse_direct/store_direct/batch
 * 语义并不严格对应，仅作粗略映射
 */
const STOCKIN_TYPE_TO_FRONTEND: Record<number, DeliveryMethod> = {
  1: 'warehouse_direct',
  2: 'store_direct',
  3: 'batch',
}

/** 前端 deliveryMethod 字符串 → 后端入库类型数字 */
const DELIVERY_METHOD_TO_BACKEND: Record<DeliveryMethod, number> = {
  warehouse_direct: 1,
  store_direct: 2,
  batch: 3,
}

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money，见文件顶部 import）
// ============================================================

/** 后端 status + qualityCheckResult → 前端状态字符串 */
function toFrontendStatus(
  status: number | null | undefined,
  qcResult: number | null | undefined,
): PurchaseStockinStatus {
  if (status === 1) return 'completed'       // 已入库 → 已完成
  if (status === 2) return 'partial'           // 部分入库 → 部分合格
  // status === 0 待入库
  if (qcResult === 1) return 'qualified'        // 合格
  if (qcResult === 2) return 'unqualified'      // 不合格
  if (qcResult === 3) return 'inspecting'       // 待检（开始检验后）
  return 'pending'                              // 默认待检验
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购收货数据转换器（API 层，处理后端实体 ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 purchaseStockinConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 stockin.ts 内部使用，不通过 index.ts 导出。
 */
export const purchaseStockinConverter = {
  /** 后端明细实体 → 前端 PurchaseStockinItem */
  toItemFrontend(backend: PurchaseStockinItemBackend): PurchaseStockinItem {
    return {
      stockinItemId: backend.stockinItemId != null ? String(backend.stockinItemId) : undefined,
      orderItemId: backend.orderItemId != null ? String(backend.orderItemId) : undefined,
      materialId: backend.materialId != null ? String(backend.materialId) : '',
      materialName: backend.materialName ?? '',
      // 后端 unitPrice 为分，保留原始值；unitPrice 字段保持后端原值以兼容历史调用
      unitPrice: backend.unitPrice != null ? Number(backend.unitPrice) : undefined,
      unitPriceFen: backend.unitPrice != null ? Number(backend.unitPrice) : undefined,
      quantity: backend.actualQuantity != null ? Number(backend.actualQuantity) : 0,
      // 后端明细无合格/不合格数量字段，前端表单编辑时由用户填写
      qualifiedQuantity: 0,
      unqualifiedQuantity: 0,
      temperature: null,
      shelfLife: null,
      productionDate: backend.productionDate ?? undefined,
      batchNo: backend.batchNo ?? undefined,
      remark: backend.remark ?? '',
    }
  },

  /** 后端实体 → 前端 PurchaseStockinInfo */
  toFrontend(backend: PurchaseStockinBackend): PurchaseStockinInfo {
    return {
      stockinId: String(backend.stockinId),
      stockinNo: backend.stockinCode ?? '',
      orderId: backend.orderId != null ? String(backend.orderId) : '',
      orderNo: backend.orderNo ?? '',
      supplierId: backend.supplierId != null ? String(backend.supplierId) : '',
      supplierName: backend.supplierName ?? '',
      deliveryMethod: backend.stockinType != null
        ? (STOCKIN_TYPE_TO_FRONTEND[backend.stockinType] ?? 'warehouse_direct')
        : 'warehouse_direct',
      // 后端 warehouseId 数字 → 前端 deliveryWarehouse 字符串
      deliveryWarehouse: backend.warehouseId != null ? String(backend.warehouseId) : undefined,
      // 后端实体无 deliveryStore
      deliveryStore: undefined,
      // 后端实体无 receiverName
      receiverName: undefined,
      totalAmount: fenToYuan(backend.totalAmount),
      totalQuantity: backend.totalQuantity != null ? Number(backend.totalQuantity) : 0,
      stockinDate: backend.stockinDate ?? '',
      inspectorId: backend.qualityCheckUserId != null ? String(backend.qualityCheckUserId) : '',
      // 后端实体无 inspectorName
      inspectorName: '',
      status: toFrontendStatus(backend.status, backend.qualityCheckResult),
      // 后端无明细级合格计数，仅用质检结果推断汇总
      qualifiedCount: backend.qualityCheckResult === 1 ? 1 : 0,
      unqualifiedCount: backend.qualityCheckResult === 2 ? 1 : 0,
      // 入库明细列表
      items: (backend.items ?? []).map(purchaseStockinConverter.toItemFrontend),
      // 后端实体用 qualityRemark 存储备注
      remark: backend.qualityRemark ?? '',
      appearanceResult: (backend as unknown as Record<string, unknown>).appearanceResult as string | undefined,
      odorResult: (backend as unknown as Record<string, unknown>).odorResult as string | undefined,
      temperature: Number((backend as unknown as Record<string, unknown>).temperature) || null,
      humidity: Number((backend as unknown as Record<string, unknown>).humidity) || null,
      sampleQuantity: Number((backend as unknown as Record<string, unknown>).sampleQuantity) || null,
      sampleRate: Number((backend as unknown as Record<string, unknown>).sampleRate) || null,
      unqualifiedType: (backend as unknown as Record<string, unknown>).unqualifiedType as string | undefined,
      disposalOpinion: (backend as unknown as Record<string, unknown>).disposalOpinion as string | undefined,
      documentCheck: (backend as unknown as Record<string, unknown>).documentCheck as string | undefined,
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseStockinBackend[] | null | undefined): PurchaseStockinInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseStockinConverter.toFrontend)
  },

  /** 前端表单数据 → 后端 CreateDTO 格式 */
  toCreateDTO(form: PurchaseStockinFormData): Record<string, unknown> {
    const items = (form.items ?? []).map(item => ({
      // 前端从 PurchaseOrderItemInfo.id（即后端 itemId）保存到 orderItemId
      orderItemId: item.orderItemId ? Number(item.orderItemId) : 0,
      materialId: item.materialId ? Number(item.materialId) : null,
      materialName: item.materialName,
      batchNo: null,
      productionDate: item.productionDate || null,
      expiryDate: null,
      actualQuantity: item.quantity,
      // 单位与单价必须从采购订单明细继承，禁止前端随意修改
      unit: item.unit || '个',
      unitPrice: item.unitPrice != null ? yuanToFen(item.unitPrice) : 0,
      locationId: null,
      remark: item.remark ?? null,
    }))
    return {
      orderId: form.orderId ? Number(form.orderId) : null,
      // 前端 deliveryWarehouse 是仓库 ID 字符串，转换为数字；无值时不再默认主仓库，避免掩盖上游未带入的问题
      warehouseId: form.deliveryWarehouse ? Number(form.deliveryWarehouse) : undefined,
      stockinDate: form.stockinDate || null,
      stockinType: DELIVERY_METHOD_TO_BACKEND[form.deliveryMethod] ?? 1,
      items,
      // 创建备注与质检备注职责分离，创建时取表单 remark，质检时取 qualityRemark
      remark: form.remark ?? '',
    }
  },

  /** 状态字符串 → 后端 status（用于查询参数） */
  toBackendStatus(status: PurchaseStockinStatus): number {
    return STATUS_TO_BACKEND[status] ?? 0
  },

  /** 质检结果字符串 → 后端 qualityCheckResult */
  toBackendQcResult(result: 'qualified' | 'unqualified' | 'partial'): number {
    if (result === 'qualified') return 1
    if (result === 'unqualified') return 2
    return 3  // partial → 待检
  },

  /** 格式化金额（元） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return yuan.toFixed(2)
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 采购收货查询参数（传给后端）
 */
interface PurchaseStockinQueryParams {
  current: number
  size: number
  orderId?: number
  status?: number
  [key: string]: unknown
}

export const purchaseStockinApi = {
  /**
   * 分页查询入库单列表
   * @param params 查询参数（含分页）
   */
  async getList(
    params: PurchaseStockinQueryForm & { page?: number; size?: number },
  ): Promise<{ records: PurchaseStockinInfo[]; total: number }> {
    const query: PurchaseStockinQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    // 状态转换
    if (params.status) query.status = purchaseStockinConverter.toBackendStatus(params.status)
    // 入库单号、采购订单号、供应商名称支持后端模糊查询
    if (params.stockinNo) query.stockinNo = params.stockinNo
    if (params.orderNo) query.orderNo = params.orderNo
    if (params.supplierName) query.supplierName = params.supplierName

    const res = await get<PurchaseStockinPageBackend | null>('/v1/purchase/stockins', query)
    const records = purchaseStockinConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询入库单详情（含明细）
   * @param id 入库单ID
   */
  async getById(id: string): Promise<PurchaseStockinInfo | null> {
    const res = await get<PurchaseStockinBackend | null>(`/v1/purchase/stockins/${id}`)
    return res ? purchaseStockinConverter.toFrontend(res) : null
  },

  /**
   * 创建入库单
   * @param data 表单数据
   */
  async create(data: PurchaseStockinFormData): Promise<PurchaseStockinInfo> {
    const dto = purchaseStockinConverter.toCreateDTO(data)
    const res = await post<PurchaseStockinBackend>('/v1/purchase/stockins', dto)
    return purchaseStockinConverter.toFrontend(res)
  },

  /**
   * 更新入库单（仅 qualityRemark/status/stockinType/stockinDate 可更新）
   * @param id 入库单ID
   * @param data 更新数据
   */
  async update(
    id: string,
    data: Partial<PurchaseStockinFormData> & {
      status?: PurchaseStockinStatus
      qualityRemark?: string
    },
  ): Promise<PurchaseStockinInfo> {
    const payload: Record<string, unknown> = {}
    if (data.qualityRemark !== undefined) payload.qualityRemark = data.qualityRemark
    if (data.status !== undefined) payload.status = purchaseStockinConverter.toBackendStatus(data.status)
    if (data.deliveryMethod !== undefined) {
      payload.stockinType = DELIVERY_METHOD_TO_BACKEND[data.deliveryMethod]
    }
    if (data.stockinDate !== undefined) payload.stockinDate = data.stockinDate
    const res = await put<PurchaseStockinBackend>(`/v1/purchase/stockins/${id}`, payload)
    return purchaseStockinConverter.toFrontend(res)
  },

  /**
   * 删除入库单
   * @param id 入库单ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/stockins/${id}`)
  },

  /**
   * 开始检验（将质检结果置为待检 3）
   * @param id 入库单ID
   */
  async startInspection(id: string): Promise<void> {
    await put<void>(`/v1/purchase/stockins/${id}/start-inspection`)
  },

  /**
   * 提交质检结果
   * @param id 入库单ID
   * @param data 质检表单数据
   */
  async submitInspection(
    id: string,
    data: {
      result: 'qualified' | 'unqualified' | 'partial'
      qualityRemark?: string
      appearanceResult?: string
      odorResult?: string
      temperature?: number | null
      humidity?: number | null
      sampleQuantity?: number | null
      sampleRate?: number | null
      unqualifiedType?: string
      disposalOpinion?: string
      documentCheck?: string
    },
  ): Promise<void> {
    const payload: Record<string, unknown> = {
      qualityCheckResult: purchaseStockinConverter.toBackendQcResult(data.result),
      qualityRemark: data.qualityRemark ?? null,
      appearanceResult: data.appearanceResult ?? null,
      odorResult: data.odorResult ?? null,
      temperature: data.temperature ?? null,
      humidity: data.humidity ?? null,
      sampleQuantity: data.sampleQuantity ?? null,
      sampleRate: data.sampleRate ?? null,
      unqualifiedType: data.unqualifiedType ?? null,
      disposalOpinion: data.disposalOpinion ?? null,
      documentCheck: data.documentCheck ?? null,
    }
    await put<void>(`/v1/purchase/stockins/${id}/quality-check`, payload)
  },

  /**
   * 确认入库（完成收货）
   * @param id 入库单ID
   */
  async complete(id: string): Promise<void> {
    await put<void>(`/v1/purchase/stockins/${id}/confirm`)
  },
}

export default purchaseStockinApi
