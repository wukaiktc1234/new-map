/**
 * 门店库存API
 * 对应后端: /v1/store-inventory
 *
 * 归属说明：门店库存属于门店运营（store-ops）模块，故放置于 api/store-ops/ 目录。
 */
import { get, post } from '@/api/request'
import { fenToYuan } from '@/utils/money'
import type {
  StoreInventoryInfo,
  StoreInventorySummary,
  StoreInventoryQueryForm,
  StoreInventoryAdjustForm,
  StoreInventoryLogInfo,
  StoreInfo,
} from '@/types/warehouse-store'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 后端 StoreInventory 实体（响应拦截器已解包 Result.data） */
interface StoreInventoryBackend {
  id?: number
  storeId?: string
  materialId?: number
  materialName?: string
  currentStock?: number
  unit?: string
  safetyStock?: number
  maxStock?: number
  unitCost?: number
  totalCost?: number
  createTime?: string
  updateTime?: string
}

/** 后端 StoreInventoryLog 实体 */
interface StoreInventoryLogBackend {
  logId?: number
  inventoryId?: number
  storeId?: string
  storeName?: string
  productId?: number
  productName?: string
  type?: number
  beforeStock?: number
  afterStock?: number
  changeQuantity?: number
  operatorId?: number
  operatorName?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/** 后端门店库存 → 前端 StoreInventoryInfo */
function toStoreInventoryInfo(backend: StoreInventoryBackend): StoreInventoryInfo {
  const quantity = Number(backend.currentStock) || 0
  return {
    inventoryId: String(backend.id ?? ''),
    storeId: backend.storeId || '',
    storeName: '',
    materialId: String(backend.materialId ?? ''),
    materialName: backend.materialName || '',
    specification: '',
    unit: backend.unit || '',
    quantity,
    lockedQuantity: 0,
    availableQuantity: quantity,
    unitCost: fenToYuan(backend.unitCost ?? 0),
    totalCost: fenToYuan(backend.totalCost ?? 0),
    lastInTime: '',
    lastOutTime: '',
    createTime: backend.createTime || '',
    updateTime: backend.updateTime || '',
  }
}

/** 后端门店库存日志 → 前端 StoreInventoryLogInfo */
function toStoreInventoryLog(backend: StoreInventoryLogBackend): StoreInventoryLogInfo {
  return {
    logId: String(backend.logId ?? ''),
    inventoryId: String(backend.inventoryId ?? ''),
    storeId: backend.storeId || '',
    storeName: backend.storeName || '',
    materialId: String(backend.productId ?? ''),
    materialName: backend.productName || '',
    operationType: String(backend.type ?? ''),
    operationTypeName: '',
    quantity: backend.changeQuantity ?? 0,
    beforeStock: backend.beforeStock ?? 0,
    afterStock: backend.afterStock ?? 0,
    operatorId: String(backend.operatorId ?? ''),
    operatorName: backend.operatorName || '',
    remark: backend.remark || '',
    createTime: backend.createdAt || '',
  }
}

export const storeInventoryApi = {
  /** 获取门店列表 */
  async getActiveStores(): Promise<StoreInfo[]> {
    return await get<StoreInfo[]>('/v1/store-inventory/stores')
  },

  /** 分页查询门店库存 */
  async getList(params?: StoreInventoryQueryForm): Promise<PageResult<StoreInventoryInfo>> {
    // 前端参数名 → 后端参数名映射：size→pageSize, materialName→productName
    const query: Record<string, unknown> = {}
    if (params?.storeId) query.storeId = params.storeId
    if (params?.materialName) query.productName = params.materialName
    if (params?.page) query.page = params.page
    if (params?.size) query.pageSize = params.size
    const res = await get<PageResult<StoreInventoryBackend>>('/v1/store-inventory/list', query)
    return {
      records: (res?.records || []).map(toStoreInventoryInfo),
      total: res?.total ?? 0,
      current: res?.current ?? params?.page ?? 1,
      size: res?.size ?? params?.size ?? 10,
      pages: res?.pages ?? 0,
    }
  },

  /** 分页查询门店库存汇总 */
  async getSummary(params?: StoreInventoryQueryForm): Promise<PageResult<StoreInventorySummary>> {
    // 前端参数名 → 后端参数名映射：size→pageSize, materialName→productName
    const query: Record<string, unknown> = {}
    if (params?.storeId) query.storeId = params.storeId
    if (params?.materialName) query.productName = params.materialName
    if (params?.page) query.page = params.page
    if (params?.size) query.pageSize = params.size
    return await get<PageResult<StoreInventorySummary>>('/v1/store-inventory/summary', query)
  },

  /** 调整门店库存 */
  async adjust(data: StoreInventoryAdjustForm): Promise<StoreInventoryInfo | null> {
    const res = await post<StoreInventoryBackend | null>('/v1/store-inventory/adjust', null, {
      params: {
        inventoryId: data.inventoryId,
        quantity: data.quantity,
        type: data.type === 'in' ? 1 : 2,
        remark: data.remark,
      }
    })
    return res ? toStoreInventoryInfo(res) : null
  },

  /** 分页查询门店库存日志 */
  async getLogs(params?: Record<string, unknown>): Promise<PageResult<StoreInventoryLogInfo>> {
    // 前端参数名 → 后端参数名映射：size→pageSize
    // 后端 logs 接口不支持 materialName 筛选，此处不发送该参数
    const query: Record<string, unknown> = {}
    if (params?.storeId) query.storeId = params.storeId
    if (params?.page) query.page = params.page
    if (params?.size) query.pageSize = params.size
    const res = await get<PageResult<StoreInventoryLogBackend>>('/v1/store-inventory/logs', query)
    return {
      records: (res?.records || []).map(toStoreInventoryLog),
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page as number) ?? 1,
      size: res?.size ?? (params?.size as number) ?? 10,
      pages: res?.pages ?? 0,
    }
  },
}
