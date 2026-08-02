/**
 * 门店档案管理 API
 *
 * 对应后端 StoreNewController（/v1/stores）→ StoreNewService → stores_new 表
 *
 * 数据转换说明：
 * - storeType/status 前端使用语义化字符串，后端/数据库使用数字编码
 * - areaSize 后端 BigDecimal（平方米），前端 number
 * - businessHoursStart/End 后端 LocalTime，前端 "HH:mm:ss" 字符串
 * - openDate/licenseExpiry 后端 LocalDate，前端 "YYYY-MM-DD" 字符串
 * - createTime/updateTime 后端 LocalDateTime，前端 ISO 8601 字符串
 */
import { get, post, put, del, patch } from '../request'
import type {
  StoreArchive,
  StoreArchiveCreateForm,
  StoreArchivePage,
  StoreArchiveQuery,
  StoreArchiveStats,
  StoreArchiveUpdateForm,
  StoreCodeAvailability,
  StoreOption,
  StoreStatus,
  StoreType,
} from '@/types/store-operation/store-archive'
import {
  STORE_STATUS_FROM_BACKEND,
  STORE_STATUS_TO_BACKEND,
  STORE_TYPE_FROM_BACKEND,
  STORE_TYPE_TO_BACKEND,
} from '@/types/store-operation/store-archive'

// ============================================================
// 后端响应类型定义
// ============================================================

/** 后端门店实体（对应 StoreNew Java 实体） */
interface StoreBackend {
  storeId: number
  storeCode: string
  storeName: string
  storeType: number
  address: string
  phone: string
  businessHoursStart: string | null
  businessHoursEnd: string | null
  areaSize: number | null
  /** 所属区域标识（后端可空） */
  area: string | null
  managerId: number | null
  openDate: string | null
  status: number
  licenseNo: string | null
  licenseExpiry: string | null
  configJson: string | null
  remark: string | null
  createTime: string
  updateTime: string
}

/** 后端分页响应 */
interface StoreBackendPage {
  records: StoreBackend[]
  total: number
  current: number
  size: number
  pages: number
}

/** 后端统计响应（Controller 已按字符串键聚合，无需再次转换） */
interface StoreStatsBackend {
  total: number
  byStatus: {
    running: number
    renovating: number
    paused: number
    closed: number
  }
  byType: {
    direct: number
    franchise: number
    cooperation: number
  }
  licenseExpiringSoon: number
}

// ============================================================
// 数据转换器
// ============================================================

/**
 * 门店档案数据转换器
 * 处理前后端格式映射：状态/类型数字编码 ↔ 语义化字符串
 */
export const storeArchiveConverter = {
  /** 后端 → 前端 */
  toFrontend(backend: StoreBackend): StoreArchive {
    return {
      storeId: backend.storeId,
      storeCode: backend.storeCode,
      storeName: backend.storeName,
      storeType: STORE_TYPE_FROM_BACKEND[backend.storeType] ?? 'direct',
      address: backend.address ?? '',
      phone: backend.phone ?? '',
      businessHoursStart: backend.businessHoursStart ?? '',
      businessHoursEnd: backend.businessHoursEnd ?? '',
      areaSize: backend.areaSize ?? 0,
      area: backend.area ?? '',
      managerId: backend.managerId,
      openDate: backend.openDate ?? '',
      status: STORE_STATUS_FROM_BACKEND[backend.status] ?? 'running',
      licenseNo: backend.licenseNo ?? '',
      licenseExpiry: backend.licenseExpiry ?? '',
      configJson: backend.configJson ?? '',
      remark: backend.remark ?? '',
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  /** 前端创建表单 → 后端 DTO */
  toCreateDTO(form: StoreArchiveCreateForm): Record<string, unknown> {
    return {
      storeCode: form.storeCode,
      storeName: form.storeName,
      storeType: STORE_TYPE_TO_BACKEND[form.storeType],
      address: form.address || null,
      phone: form.phone || null,
      businessHoursStart: form.businessHoursStart || null,
      businessHoursEnd: form.businessHoursEnd || null,
      areaSize: form.areaSize || null,
      area: form.area || null,
      managerId: form.managerId,
      openDate: form.openDate || null,
      status: STORE_STATUS_TO_BACKEND[form.status],
      licenseNo: form.licenseNo || null,
      licenseExpiry: form.licenseExpiry || null,
      configJson: form.configJson || null,
      remark: form.remark || null,
    }
  },

  /** 前端更新表单 → 后端 DTO（仅包含已修改字段） */
  toUpdateDTO(form: StoreArchiveUpdateForm): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.storeName !== undefined) dto.storeName = form.storeName
    if (form.storeType !== undefined) dto.storeType = STORE_TYPE_TO_BACKEND[form.storeType]
    if (form.address !== undefined) dto.address = form.address || null
    if (form.phone !== undefined) dto.phone = form.phone || null
    if (form.businessHoursStart !== undefined) dto.businessHoursStart = form.businessHoursStart || null
    if (form.businessHoursEnd !== undefined) dto.businessHoursEnd = form.businessHoursEnd || null
    if (form.areaSize !== undefined) dto.areaSize = form.areaSize || null
    if (form.area !== undefined) dto.area = form.area || null
    if (form.managerId !== undefined) dto.managerId = form.managerId
    if (form.openDate !== undefined) dto.openDate = form.openDate || null
    if (form.status !== undefined) dto.status = STORE_STATUS_TO_BACKEND[form.status]
    if (form.licenseNo !== undefined) dto.licenseNo = form.licenseNo || null
    if (form.licenseExpiry !== undefined) dto.licenseExpiry = form.licenseExpiry || null
    if (form.configJson !== undefined) dto.configJson = form.configJson || null
    if (form.remark !== undefined) dto.remark = form.remark || null
    return dto
  },

  /** 后端统计 → 前端统计（后端已按字符串键聚合，直接透传） */
  toStatsFrontend(backend: StoreStatsBackend): StoreArchiveStats {
    return {
      total: backend.total,
      byStatus: {
        running: backend.byStatus?.running ?? 0,
        renovating: backend.byStatus?.renovating ?? 0,
        paused: backend.byStatus?.paused ?? 0,
        closed: backend.byStatus?.closed ?? 0,
      },
      byType: {
        direct: backend.byType?.direct ?? 0,
        franchise: backend.byType?.franchise ?? 0,
        cooperation: backend.byType?.cooperation ?? 0,
      },
      licenseExpiringSoon: backend.licenseExpiringSoon,
    }
  },

  /** 门店选项（轻量级转换） */
  toOption(backend: StoreBackend): StoreOption {
    return {
      storeId: backend.storeId,
      storeCode: backend.storeCode,
      storeName: backend.storeName,
    }
  },

  /** 状态 → StatusTag 组件 status 属性（使用 StatusTag 内置状态色） */
  toStatusTagStatus(status: StoreStatus): string {
    const map: Record<StoreStatus, string> = {
      running: 'success',
      renovating: 'warning',
      paused: 'paused',
      closed: 'inactive',
    }
    return map[status] ?? 'info'
  },

  /** 类型 → StatusTag 组件 status 属性 */
  toTypeTagStatus(type: StoreType): string {
    const map: Record<StoreType, string> = {
      direct: 'active',
      franchise: 'info',
      cooperation: 'pending',
    }
    return map[type] ?? 'info'
  },
}

// ============================================================
// API 接口
// ============================================================

export const storeArchiveApi = {
  /**
   * 分页查询门店列表
   * GET /v1/stores?current=&size=&storeName=&storeType=&status=
   */
  async getList(params: StoreArchiveQuery): Promise<StoreArchivePage> {
    const query: Record<string, unknown> = {
      current: params.current,
      size: params.size,
    }
    if (params.storeName) query.storeName = params.storeName
    if (params.storeType) query.storeType = STORE_TYPE_TO_BACKEND[params.storeType]
    if (params.status) query.status = STORE_STATUS_TO_BACKEND[params.status]

    const res = await get<StoreBackendPage | null>('/v1/stores', query)
    if (!res) {
      return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    }
    return {
      records: (res.records || []).map((r) => storeArchiveConverter.toFrontend(r)),
      total: res.total,
      current: res.current,
      size: res.size,
      pages: res.pages,
    }
  },

  /**
   * 获取门店详情
   * GET /v1/stores/{storeId}
   */
  async getById(storeId: number): Promise<StoreArchive> {
    const res = await get<StoreBackend>(`/v1/stores/${storeId}`)
    return storeArchiveConverter.toFrontend(res)
  },

  /**
   * 按编码查询门店
   * GET /v1/stores/code/{storeCode}
   */
  async getByCode(storeCode: string): Promise<StoreArchive> {
    const res = await get<StoreBackend>(`/v1/stores/code/${encodeURIComponent(storeCode)}`)
    return storeArchiveConverter.toFrontend(res)
  },

  /**
   * 创建门店
   * POST /v1/stores
   */
  async create(form: StoreArchiveCreateForm): Promise<StoreArchive> {
    const dto = storeArchiveConverter.toCreateDTO(form)
    const res = await post<StoreBackend>('/v1/stores', dto)
    return storeArchiveConverter.toFrontend(res)
  },

  /**
   * 更新门店（仅更新非空字段）
   * PUT /v1/stores/{storeId}
   */
  async update(storeId: number, form: StoreArchiveUpdateForm): Promise<StoreArchive> {
    const dto = storeArchiveConverter.toUpdateDTO(form)
    const res = await put<StoreBackend>(`/v1/stores/${storeId}`, dto)
    return storeArchiveConverter.toFrontend(res)
  },

  /**
   * 更新门店状态
   * PATCH /v1/stores/{storeId}/status?status=1
   */
  async updateStatus(storeId: number, status: StoreStatus): Promise<void> {
    await patch<void>(`/v1/stores/${storeId}/status?status=${STORE_STATUS_TO_BACKEND[status]}`)
  },

  /**
   * 逻辑删除门店
   * DELETE /v1/stores/{storeId}
   */
  async delete(storeId: number): Promise<void> {
    await del<void>(`/v1/stores/${storeId}`)
  },

  /**
   * 获取营业中门店列表（下拉选项用）
   * GET /v1/stores/active
   */
  async getActiveStores(): Promise<StoreOption[]> {
    const res = await get<StoreBackend[] | null>('/v1/stores/active')
    if (!res || !Array.isArray(res)) return []
    return res.map((r) => storeArchiveConverter.toOption(r))
  },

  /**
   * 检查门店编码可用性
   * GET /v1/stores/code-available?storeCode=&excludeId=
   */
  async checkCodeAvailable(storeCode: string, excludeId?: number): Promise<StoreCodeAvailability> {
    const params: Record<string, unknown> = { storeCode }
    if (excludeId !== undefined && excludeId !== null) {
      params.excludeId = excludeId
    }
    const res = await get<StoreCodeAvailability | null>('/v1/stores/code-available', params)
    return res ?? { available: false, storeCode }
  },

  /**
   * 获取门店统计信息
   * GET /v1/stores/stats
   */
  async getStats(): Promise<StoreArchiveStats> {
    const res = await get<StoreStatsBackend | null>('/v1/stores/stats')
    if (!res) {
      return {
        total: 0,
        byStatus: { running: 0, renovating: 0, paused: 0, closed: 0 },
        byType: { direct: 0, franchise: 0, cooperation: 0 },
        licenseExpiringSoon: 0,
      }
    }
    return storeArchiveConverter.toStatsFrontend(res)
  },
}
