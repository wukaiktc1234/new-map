/**
 * 库存管理 API + DataConverter
 * 对应后端: /v1/inventory
 *
 * DataConverter 处理内容：
 * - 库存状态数字↔字符串（后端 1~4 ↔ 前端 'normal'/'warning'/'expired'/'frozen'）
 * - 库存类型数字↔字符串（后端 1~4 ↔ 前端 'raw_material'/'semi_finished'/'finished'/'packaging'）
 * - 变动类型数字↔字符串（后端 1~8 ↔ 前端 'purchase_in'/'sale_out'/...）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 string 元，2位小数）
 */
import { get, post } from '../request'

import type {
  InventoryInfo,
  InventoryQueryForm,
  InventoryIncreaseForm,
  InventoryLockForm,
  InventoryDeductForm,
  InventoryStatus,
  InventoryType,
  TransactionType,
} from '@/types/warehouse-inventory'
import { inventoryConverter } from './converters'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的库存 VO */
interface InventoryBackend {
  inventoryId: number
  materialId: string
  materialName: string
  specification: string | null
  unit: string | null
  warehouseId: number
  warehouseName: string | null
  locationId: number | null
  locationName: string | null
  quantity: number
  lockedQuantity: number | null
  availableQuantity: number | null
  batchNo: string | null
  productionDate: string | null  // yyyy-MM-dd
  expiryDate: string | null       // yyyy-MM-dd
  unitCost: number                 // 单位：分
  totalCost: number                // 单位：分
  minSafeQty: number | null
  maxStockQty: number | null
  status: number                   // 1=正常 2=预警 3=过期 4=冻结
  inventoryType: number            // 1=原料 2=半成品 3=成品 4=包装材料
  createTime: string | null
  updateTime: string | null
}

/** 后端 IPage 分页响应 */
interface InventoryPageBackend {
  records: InventoryBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 内部 DataConverter（API 层）
// ============================================================

/**
 * 库存数据转换器（API 层）
 *
 * 复用 converters.ts 导出的 inventoryConverter，补充：
 * - 后端 VO → 前端 Info 转换（含 ID number→string、金额分→元）
 * - 前端表单 → 后端 DTO 转换（含 金额元→分、状态/类型字符串→数字）
 */
const inventoryApiConverter = {
  /** 后端 VO → 前端 InventoryInfo */
  toFrontend(backend: InventoryBackend): InventoryInfo {
    const status: InventoryStatus = inventoryConverter.toFrontendStatus(backend.status)
    const inventoryType: InventoryType = inventoryConverter.toFrontendType(backend.inventoryType)
    return {
      inventoryId: String(backend.inventoryId),
      materialId: backend.materialId,
      materialName: backend.materialName,
      specification: backend.specification ?? '',
      unit: backend.unit ?? '',
      warehouseId: String(backend.warehouseId),
      locationId: backend.locationId != null ? String(backend.locationId) : '',
      quantity: backend.quantity,
      lockedQuantity: backend.lockedQuantity ?? 0,
      availableQuantity: backend.availableQuantity ?? backend.quantity,
      batchNo: backend.batchNo ?? '',
      productionDate: backend.productionDate ?? '',
      expiryDate: backend.expiryDate ?? '',
      // 后端以分为单位，前端以元为单位（保留 2 位小数字符串）
      unitCost: inventoryConverter.toYuan(backend.unitCost),
      totalCost: inventoryConverter.toYuan(backend.totalCost),
      minSafeQty: backend.minSafeQty ?? 0,
      maxStockQty: backend.maxStockQty ?? 0,
      status,
      inventoryType,
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryBackend[] | null | undefined): InventoryInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryApiConverter.toFrontend)
  },

  /** 前端 InventoryIncreaseForm → 后端 CreateDTO */
  toIncreaseDTO(form: InventoryIncreaseForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {
      materialId: form.materialId,
      warehouseId: Number(form.warehouseId),
      quantity: form.quantity,
    }
    if (form.locationId) dto.locationId = Number(form.locationId)
    // 前端 unitCost 为元字符串，后端为分（整数）
    if (form.unitCost !== undefined) dto.unitCost = inventoryConverter.toFen(form.unitCost)
    // 前端 transactionType 已是数字，直接传递；若为字符串则转换
    if (form.transactionType !== undefined) {
      dto.transactionType = typeof form.transactionType === 'number'
        ? form.transactionType
        : inventoryConverter.toBackendTransactionType(form.transactionType as TransactionType)
    }
    if (form.batchNo) dto.batchNo = form.batchNo
    if (form.referenceNo) dto.referenceNo = form.referenceNo
    if (form.referenceType) dto.referenceType = form.referenceType
    if (form.remark) dto.remark = form.remark
    return dto
  },

  /** 前端 InventoryLockForm → 后端 LockDTO */
  toLockDTO(form: InventoryLockForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {
      inventoryId: Number(form.inventoryId),
      quantity: form.quantity,
    }
    if (form.referenceNo) dto.referenceNo = form.referenceNo
    if (form.referenceType) dto.referenceType = form.referenceType
    return dto
  },

  /** 前端 InventoryDeductForm → 后端 DeductDTO */
  toDeductDTO(form: InventoryDeductForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {
      inventoryId: Number(form.inventoryId),
      quantity: form.quantity,
    }
    if (form.referenceNo) dto.referenceNo = form.referenceNo
    if (form.referenceType) dto.referenceType = form.referenceType
    if (form.transactionType !== undefined) {
      dto.transactionType = typeof form.transactionType === 'number'
        ? form.transactionType
        : inventoryConverter.toBackendTransactionType(form.transactionType as TransactionType)
    }
    return dto
  },
}

// ============================================================
// API 实现
// ============================================================

/** 库存查询参数（传给后端） */
interface InventoryQueryParams {
  current: number
  size: number
  warehouseId?: number
  materialId?: string
  materialName?: string
  batchNo?: string
  status?: number
  lowStockOnly?: boolean
  expiringSoon?: boolean
  [key: string]: unknown
}

/** 库存分页响应 */
interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export const inventoryApi = {
  /**
   * 分页查询库存列表
   * @param params 查询参数（含分页）
   */
  async getList(params?: InventoryQueryForm): Promise<PageResult<InventoryInfo>> {
    const query: InventoryQueryParams = {
      current: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    if (params?.warehouseId) query.warehouseId = Number(params.warehouseId)
    if (params?.materialId) query.materialId = params.materialId
    if (params?.materialName) query.materialName = params.materialName
    if (params?.batchNo) query.batchNo = params.batchNo
    if (params?.status) query.status = inventoryConverter.toBackendStatus(params.status)
    if (params?.lowStockOnly) query.lowStockOnly = params.lowStockOnly
    if (params?.expiringSoon) query.expiringSoon = params.expiringSoon

    const res = await get<InventoryPageBackend | null>('/v1/inventory', query)
    const records = inventoryApiConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? query.current
    const size = res?.size ?? query.size
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 获取库存详情
   * @param inventoryId 库存ID
   */
  async getById(inventoryId: string): Promise<InventoryInfo> {
    const res = await get<InventoryBackend>(`/v1/inventory/${inventoryId}`)
    return inventoryApiConverter.toFrontend(res)
  },

  /**
   * 根据库位ID获取库存列表
   * @param locationId 库位ID
   */
  async getByLocationId(locationId: string): Promise<InventoryInfo[]> {
    const res = await get<InventoryBackend[] | null>(`/v1/inventory/location/${locationId}`)
    return inventoryApiConverter.toFrontendList(res)
  },

  /**
   * 获取可用库存数量
   * @param inventoryId 库存ID
   */
  async getAvailableQuantity(inventoryId: string): Promise<number> {
    const res = await get<number>(`/v1/inventory/${inventoryId}/available`)
    return Number(res) || 0
  },

  /**
   * 锁定库存
   * @param data 锁定表单
   */
  async lock(data: InventoryLockForm): Promise<void> {
    const dto = inventoryApiConverter.toLockDTO(data)
    await post<void>('/v1/inventory/lock', dto)
  },

  /**
   * 解锁库存
   * @param inventoryId 库存ID
   * @param quantity 解锁数量
   */
  async unlock(inventoryId: string, quantity: number): Promise<void> {
    // 后端通过 @RequestParam 接收 quantity，使用 query string 传递
    await post<void>(`/v1/inventory/${inventoryId}/unlock?quantity=${quantity}`)
  },

  /**
   * 扣减库存
   * @param data 扣减表单
   */
  async deduct(data: InventoryDeductForm): Promise<void> {
    const dto = inventoryApiConverter.toDeductDTO(data)
    await post<void>('/v1/inventory/deduct', dto)
  },

  /**
   * 增加库存（入库）
   * @param data 增加表单
   */
  async increase(data: InventoryIncreaseForm): Promise<void> {
    const dto = inventoryApiConverter.toIncreaseDTO(data)
    await post<void>('/v1/inventory/increase', dto)
  },

  /**
   * 获取低库存列表
   * @param warehouseId 仓库ID（可选）
   */
  async getLowStockList(warehouseId?: string): Promise<InventoryInfo[]> {
    const params: Record<string, unknown> = {}
    if (warehouseId) params.warehouseId = Number(warehouseId)
    const res = await get<InventoryBackend[] | null>('/v1/inventory/low-stock', params)
    return inventoryApiConverter.toFrontendList(res)
  },
}

export default inventoryApi
