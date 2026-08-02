/**
 * 商品分类 API + DataConverter
 * 对应后端: /v1/material-categories
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串、ID number↔string
 * - 与 types/purchase-category.ts 类型定义对齐
 */
import { get, post, put, del } from '../request'

import type {
  MaterialCategoryInfo,
  MaterialCategoryQueryForm,
  MaterialCategoryFormData,
  MaterialCategoryStatus,
} from '@/types/purchase-category'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的商品分类 VO */
interface MaterialCategoryBackend {
  categoryId: number
  categoryCode: string
  categoryName: string
  parentId: number | null
  parentName: string | null
  remark: string | null
  sortOrder: number
  status: number
  statusName: string
  materialCount: number
  createTime: string
  updateTime: string
}

/** 后端分页响应 */
interface MaterialCategoryPageBackend {
  records: MaterialCategoryBackend[] | null
  total: number
  current: number
  size: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// ============================================================

const STATUS_TO_FRONTEND: Record<number, MaterialCategoryStatus> = {
  1: 'active',
  0: 'inactive',
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 商品分类数据转换器
 * - toFrontend: 后端 VO → 前端 Info（状态数字→字符串，ID number→string）
 * - toCreateDTO: 前端 Form → 后端 CreateDTO（ID string→number）
 * - toUpdateDTO: 前端 Form → 后端 UpdateDTO（部分更新）
 */
export const materialCategoryConverter = {
  /** 后端 VO → 前端 Info */
  toFrontend(backend: MaterialCategoryBackend): MaterialCategoryInfo {
    return {
      categoryId: String(backend.categoryId),
      categoryCode: backend.categoryCode,
      categoryName: backend.categoryName,
      parentId: backend.parentId != null ? String(backend.parentId) : '',
      parentName: backend.parentName ?? '',
      sortOrder: backend.sortOrder,
      materialCount: backend.materialCount ?? 0,
      status: STATUS_TO_FRONTEND[backend.status] ?? 'inactive',
      remark: backend.remark ?? '',
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: MaterialCategoryBackend[] | null | undefined): MaterialCategoryInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(materialCategoryConverter.toFrontend)
  },

  /** 前端 Form → 后端 CreateDTO */
  toCreateDTO(form: MaterialCategoryFormData): Record<string, unknown> {
    return {
      categoryCode: form.categoryCode,
      categoryName: form.categoryName,
      parentId: form.parentId ? Number(form.parentId) : null,
      description: form.remark || null,
      sortOrder: form.sortOrder ?? 0,
    }
  },

  /** 前端 Form → 后端 UpdateDTO（部分更新） */
  toUpdateDTO(form: Partial<MaterialCategoryFormData>): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.categoryName !== undefined) dto.categoryName = form.categoryName
    if (form.parentId !== undefined) dto.parentId = form.parentId ? Number(form.parentId) : null
    if (form.remark !== undefined) dto.description = form.remark || null
    if (form.sortOrder !== undefined) dto.sortOrder = form.sortOrder
    return dto
  },

  /** 状态字符串 → 后端数字（用于状态切换） */
  toBackendStatus(status: MaterialCategoryStatus): number {
    return status === 'active' ? 1 : 0
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 商品分类查询参数（传给后端）
 */
interface CategoryQueryParams {
  page: number
  size: number
  status?: string
  keyword?: string
  [key: string]: unknown
}

export const materialCategoryApi = {
  /**
   * 分页查询商品分类列表
   * @param params 查询参数（含分页）
   */
  async getList(params: MaterialCategoryQueryForm & { page?: number; size?: number }): Promise<{ records: MaterialCategoryInfo[]; total: number }> {
    const query: CategoryQueryParams = {
      page: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.status) query.status = params.status
    if (params.keyword) query.keyword = params.keyword

    const res = await get<MaterialCategoryPageBackend | null>('/v1/material-categories/page', query)
    const records = materialCategoryConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询商品分类详情
   * @param id 分类ID
   */
  async getById(id: string): Promise<MaterialCategoryInfo | null> {
    const res = await get<MaterialCategoryBackend | null>(`/v1/material-categories/${id}`)
    return res ? materialCategoryConverter.toFrontend(res) : null
  },

  /**
   * 创建商品分类
   * @param form 表单数据
   */
  async create(form: MaterialCategoryFormData): Promise<MaterialCategoryInfo> {
    const dto = materialCategoryConverter.toCreateDTO(form)
    const res = await post<MaterialCategoryBackend>('/v1/material-categories', dto)
    return materialCategoryConverter.toFrontend(res)
  },

  /**
   * 更新商品分类（部分更新）
   * @param id 分类ID
   * @param form 表单数据（部分字段）
   */
  async update(id: string, form: Partial<MaterialCategoryFormData>): Promise<MaterialCategoryInfo> {
    const dto = materialCategoryConverter.toUpdateDTO(form)
    const res = await put<MaterialCategoryBackend>(`/v1/material-categories/${id}`, dto)
    return materialCategoryConverter.toFrontend(res)
  },

  /**
   * 删除商品分类（逻辑删除）
   * @param id 分类ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/material-categories/${id}`)
  },

  /**
   * 更新分类状态（启用/停用）
   * @param id 分类ID
   * @param status 目标状态（前端语义化字符串）
   */
  async updateStatus(id: string, status: MaterialCategoryStatus): Promise<void> {
    await put<void>(`/v1/material-categories/${id}/status?status=${status}`)
  },

  /**
   * 获取所有启用的分类列表（下拉选项用）
   */
  async getEnabledList(): Promise<MaterialCategoryInfo[]> {
    const res = await get<MaterialCategoryBackend[] | null>('/v1/material-categories')
    return materialCategoryConverter.toFrontendList(res)
  },

  /**
   * 获取所有分类列表（包含禁用的）
   */
  async getAllList(): Promise<MaterialCategoryInfo[]> {
    const res = await get<MaterialCategoryBackend[] | null>('/v1/material-categories/all')
    return materialCategoryConverter.toFrontendList(res)
  },
}

export default materialCategoryApi
