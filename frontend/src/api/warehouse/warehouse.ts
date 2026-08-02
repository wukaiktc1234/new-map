/**
 * 仓库管理 API + DataConverter
 * 对应后端: /v1/warehouses
 *
 * DataConverter 处理内容：
 * - 仓库状态数字↔字符串（后端 0/1 ↔ 前端 'active'/'inactive'）
 * - 仓库类型数字↔字符串（后端 1~4 ↔ 前端 'main'/'cold'/'freeze'/'normal'）
 * - ID number↔string（避免大数精度丢失）
 * - 容量字段 capacity 为数值，无金额转换
 */
import { get, post, put, del } from '../request'

import type {
  WarehouseInfo,
  WarehouseQueryForm,
  WarehouseCreateForm,
  WarehouseUpdateForm,
  WarehouseType,
  WarehouseStatus,
} from '@/types/warehouse'
import { warehouseConverter } from './converters'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的仓库 VO */
interface WarehouseBackend {
  warehouseId: number
  warehouseCode: string | null
  warehouseName: string
  warehouseType: number  // 1=主仓 2=冷库 3=冻库 4=常温库
  address: string | null
  managerId: string | null
  managerName: string | null
  phone: string | null
  capacity: number
  usedCapacity: number | null
  capacityUsageRate: number | null
  status: number  // 0=停用 1=启用
  remark: string | null
  createTime: string | null
  updateTime: string | null
}

/** 后端 IPage 分页响应 */
interface WarehousePageBackend {
  records: WarehouseBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 内部 DataConverter（API 层）
// ============================================================

/**
 * 仓库数据转换器（API 层）
 *
 * 注意：与 converters.ts 中的 warehouseConverter（UI 层，用于 StatusTag 显示）同名同实例。
 * 本文件直接复用 converters.ts 导出的 warehouseConverter，避免重复实现。
 * 这里仅补充前端表单→后端 DTO 的转换逻辑，以及后端 VO→前端 Info 的转换。
 */
const warehouseApiConverter = {
  /** 后端 VO → 前端 WarehouseInfo */
  toFrontend(backend: WarehouseBackend): WarehouseInfo {
    const status: WarehouseStatus = warehouseConverter.toFrontendStatus(backend.status)
    const warehouseType: WarehouseType = warehouseConverter.toFrontendType(backend.warehouseType)
    return {
      warehouseId: String(backend.warehouseId),
      warehouseCode: backend.warehouseCode ?? '',
      warehouseName: backend.warehouseName,
      warehouseType,
      warehouseTypeName: warehouseConverter.toTypeLabel(warehouseType),
      address: backend.address ?? '',
      managerId: backend.managerId ?? '',
      managerName: backend.managerName ?? '',
      phone: backend.phone ?? '',
      capacity: backend.capacity ?? 0,
      usedCapacity: backend.usedCapacity ?? 0,
      capacityUsageRate: backend.capacityUsageRate ?? 0,
      status,
      statusName: warehouseConverter.toStatusLabel(status),
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: WarehouseBackend[] | null | undefined): WarehouseInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(warehouseApiConverter.toFrontend)
  },

  /** 前端 CreateForm → 后端 CreateDTO */
  toCreateDTO(form: WarehouseCreateForm): Record<string, unknown> {
    return {
      warehouseCode: form.warehouseCode,
      warehouseName: form.warehouseName,
      warehouseType: warehouseConverter.toBackendType(form.warehouseType),
      address: form.address ?? null,
      managerId: form.managerId || null,
      phone: form.phone || null,
      capacity: form.capacity ?? 0,
      remark: form.remark ?? null,
    }
  },

  /** 前端 UpdateForm → 后端 UpdateDTO */
  toUpdateDTO(form: WarehouseUpdateForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.warehouseName !== undefined) dto.warehouseName = form.warehouseName
    if (form.warehouseType !== undefined) dto.warehouseType = warehouseConverter.toBackendType(form.warehouseType)
    if (form.address !== undefined) dto.address = form.address
    if (form.managerId !== undefined) dto.managerId = form.managerId || null
    if (form.phone !== undefined) dto.phone = form.phone || null
    if (form.capacity !== undefined) dto.capacity = form.capacity
    if (form.remark !== undefined) dto.remark = form.remark
    return dto
  },
}

// ============================================================
// API 实现
// ============================================================

/** 仓库查询参数（传给后端） */
interface WarehouseQueryParams {
  current: number
  size: number
  warehouseName?: string
  warehouseCode?: string
  warehouseType?: number
  status?: number
  [key: string]: unknown
}

/** 仓库分页响应 */
interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export const warehouseApi = {
  /**
   * 分页查询仓库列表
   * @param params 查询参数（含分页）
   */
  async getList(params?: WarehouseQueryForm): Promise<PageResult<WarehouseInfo>> {
    const query: WarehouseQueryParams = {
      current: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    if (params?.warehouseName) query.warehouseName = params.warehouseName
    if (params?.warehouseCode) query.warehouseCode = params.warehouseCode
    if (params?.warehouseType) query.warehouseType = warehouseConverter.toBackendType(params.warehouseType)
    if (params?.status) query.status = warehouseConverter.toBackendStatus(params.status)

    const res = await get<WarehousePageBackend | null>('/v1/warehouses', query)
    const records = warehouseApiConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? query.current
    const size = res?.size ?? query.size
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 获取仓库详情
   * @param warehouseId 仓库ID
   */
  async getById(warehouseId: string): Promise<WarehouseInfo> {
    const res = await get<WarehouseBackend>(`/v1/warehouses/${warehouseId}`)
    return warehouseApiConverter.toFrontend(res)
  },

  /**
   * 创建仓库
   * @param data 创建表单
   */
  async create(data: WarehouseCreateForm): Promise<WarehouseInfo> {
    const dto = warehouseApiConverter.toCreateDTO(data)
    const res = await post<WarehouseBackend>('/v1/warehouses', dto)
    return warehouseApiConverter.toFrontend(res)
  },

  /**
   * 更新仓库
   * @param warehouseId 仓库ID
   * @param data 更新表单
   */
  async update(warehouseId: string, data: WarehouseUpdateForm): Promise<WarehouseInfo> {
    const dto = warehouseApiConverter.toUpdateDTO(data)
    const res = await put<WarehouseBackend>(`/v1/warehouses/${warehouseId}`, dto)
    return warehouseApiConverter.toFrontend(res)
  },

  /**
   * 删除仓库（后端为逻辑删除）
   * @param warehouseId 仓库ID
   */
  async delete(warehouseId: string): Promise<void> {
    await del<void>(`/v1/warehouses/${warehouseId}`)
  },

  /**
   * 获取启用中的仓库列表
   */
  async getActiveList(): Promise<WarehouseInfo[]> {
    const res = await get<WarehouseBackend[] | null>('/v1/warehouses/active')
    return warehouseApiConverter.toFrontendList(res)
  },

  /**
   * 切换仓库状态
   * @param warehouseId 仓库ID
   * @param status 后端状态数值（0=停用 1=启用）
   */
  async toggleStatus(warehouseId: string, status: number): Promise<void> {
    // 后端通过 @RequestParam 接收 status，使用 query string 传递
    await put<void>(`/v1/warehouses/${warehouseId}/status?status=${status}`)
  },
}

export default warehouseApi
