/**
 * 库位管理 API + DataConverter
 * 对应后端: /v1/inventory-locations
 *
 * DataConverter 处理内容：
 * - 库位类型数字↔字符串（后端 1~4 ↔ 前端 'shelf'/'floor'/'cold_storage'/'freezer'）
 * - 库位状态数字↔字符串（后端 0/1 ↔ 前端 'inactive'/'active'，与仓库状态映射一致）
 * - ID number↔string（避免大数精度丢失）
 *
 * 注意：converters.ts 中的 locationConverter 仅提供类型/状态的 UI 层标签转换，
 * 未提供 toBackendStatus/toFrontendStatus，因此在 API 层本地实现状态映射。
 */
import { get, post, put } from '../request'

import type {
  InventoryLocationInfo,
  LocationQueryForm,
  LocationCreateForm,
  LocationUpdateForm,
  LocationType,
  LocationStatus,
} from '@/types/warehouse-location'
import { locationConverter } from './converters'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的库位 VO */
interface InventoryLocationBackend {
  locationId: number
  warehouseId: number
  warehouseName: string | null
  locationCode: string
  locationName: string
  locationType: number  // 1=货架 2=地面 3=冷藏区 4=冷冻区
  maxCapacity: number
  currentQuantity: number | null
  usageRate: number | null
  status: number  // 0=停用 1=启用
  remark: string | null
  createTime: string | null
  updateTime: string | null
}

/** 后端 IPage 分页响应 */
interface InventoryLocationPageBackend {
  records: InventoryLocationBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端 status：0=停用 1=启用（与 WarehouseStatus 映射一致）
// ============================================================

const LOCATION_STATUS_TO_FRONTEND: Record<number, LocationStatus> = {
  0: 'inactive',
  1: 'active',
}

const LOCATION_STATUS_TO_BACKEND: Record<LocationStatus, number> = {
  active: 1,
  inactive: 0,
}

// ============================================================
// 内部 DataConverter（API 层）
// ============================================================

/**
 * 库位数据转换器（API 层）
 *
 * 复用 converters.ts 导出的 locationConverter 处理类型/状态标签，本文件补充：
 * - 后端 VO → 前端 Info 转换
 * - 前端表单 → 后端 DTO 转换
 * - 库位状态字符串↔数字（converters.ts 中未提供）
 */
const locationApiConverter = {
  /** 后端 VO → 前端 InventoryLocationInfo */
  toFrontend(backend: InventoryLocationBackend): InventoryLocationInfo {
    const locationType: LocationType = locationConverter.toFrontendType(backend.locationType)
    const status: LocationStatus = LOCATION_STATUS_TO_FRONTEND[backend.status] ?? 'inactive'
    return {
      locationId: String(backend.locationId),
      warehouseId: String(backend.warehouseId),
      warehouseName: backend.warehouseName ?? '',
      locationCode: backend.locationCode,
      locationName: backend.locationName,
      locationType,
      locationTypeName: locationConverter.toTypeLabel(locationType),
      maxCapacity: backend.maxCapacity ?? 0,
      currentQuantity: backend.currentQuantity ?? 0,
      usageRate: backend.usageRate ?? 0,
      status,
      statusName: locationConverter.toStatusLabel(status),
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryLocationBackend[] | null | undefined): InventoryLocationInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(locationApiConverter.toFrontend)
  },

  /** 前端 CreateForm → 后端 CreateDTO */
  toCreateDTO(form: LocationCreateForm): Record<string, unknown> {
    return {
      warehouseId: Number(form.warehouseId),
      locationCode: form.locationCode,
      locationName: form.locationName,
      locationType: locationConverter.toBackendType(form.locationType),
      maxCapacity: form.maxCapacity ?? 0,
      remark: form.remark ?? null,
    }
  },

  /** 前端 UpdateForm → 后端 UpdateDTO */
  toUpdateDTO(form: LocationUpdateForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.locationName !== undefined) dto.locationName = form.locationName
    if (form.locationType !== undefined) dto.locationType = locationConverter.toBackendType(form.locationType)
    if (form.maxCapacity !== undefined) dto.maxCapacity = form.maxCapacity
    if (form.status !== undefined) dto.status = LOCATION_STATUS_TO_BACKEND[form.status]
    if (form.remark !== undefined) dto.remark = form.remark
    return dto
  },

  /** 库位状态字符串 → 后端数字 */
  toBackendStatus(status: LocationStatus): number {
    return LOCATION_STATUS_TO_BACKEND[status] ?? 0
  },
}

// ============================================================
// API 实现
// ============================================================

/** 库位查询参数（传给后端） */
interface LocationQueryParams {
  current: number
  size: number
  warehouseId?: number
  locationCode?: string
  locationName?: string
  locationType?: number
  status?: number
  [key: string]: unknown
}

/** 库位分页响应 */
interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export const inventoryLocationApi = {
  /**
   * 分页查询库位列表
   * @param params 查询参数（含分页）
   */
  async getList(params?: LocationQueryForm): Promise<PageResult<InventoryLocationInfo>> {
    const query: LocationQueryParams = {
      current: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    if (params?.warehouseId) query.warehouseId = Number(params.warehouseId)
    if (params?.locationCode) query.locationCode = params.locationCode
    if (params?.locationType) query.locationType = locationConverter.toBackendType(params.locationType)
    if (params?.status) query.status = locationApiConverter.toBackendStatus(params.status)

    const res = await get<InventoryLocationPageBackend | null>('/v1/inventory-locations', query)
    const records = locationApiConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? query.current
    const size = res?.size ?? query.size
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 获取库位详情
   * @param locationId 库位ID
   */
  async getById(locationId: string): Promise<InventoryLocationInfo> {
    const res = await get<InventoryLocationBackend>(`/v1/inventory-locations/${locationId}`)
    return locationApiConverter.toFrontend(res)
  },

  /**
   * 创建库位
   * @param data 创建表单
   */
  async create(data: LocationCreateForm): Promise<InventoryLocationInfo> {
    const dto = locationApiConverter.toCreateDTO(data)
    const res = await post<InventoryLocationBackend>('/v1/inventory-locations', dto)
    return locationApiConverter.toFrontend(res)
  },

  /**
   * 更新库位
   * @param locationId 库位ID
   * @param data 更新表单
   */
  async update(locationId: string, data: LocationUpdateForm): Promise<InventoryLocationInfo> {
    const dto = locationApiConverter.toUpdateDTO(data)
    const res = await put<InventoryLocationBackend>(`/v1/inventory-locations/${locationId}`, dto)
    return locationApiConverter.toFrontend(res)
  },

  /**
   * 切换库位状态
   * @param locationId 库位ID
   * @param status 后端状态数值（0=停用 1=启用）
   */
  async toggleStatus(locationId: string, status: number): Promise<void> {
    // 后端通过 @RequestParam 接收 status，使用 query string 传递
    await put<void>(`/v1/inventory-locations/${locationId}/status?status=${status}`)
  },

  /**
   * 根据仓库ID获取库位列表
   * @param warehouseId 仓库ID
   */
  async getByWarehouseId(warehouseId: string): Promise<InventoryLocationInfo[]> {
    const res = await get<InventoryLocationBackend[] | null>(`/v1/inventory-locations/by-warehouse/${warehouseId}`)
    return locationApiConverter.toFrontendList(res)
  },
}

export default inventoryLocationApi
