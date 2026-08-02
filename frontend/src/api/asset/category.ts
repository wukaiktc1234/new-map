/**
 * 资产分类管理API
 * 对应后端: /v1/asset/categories (AssetCategoryController)
 *
 * 后端接口：
 * - GET /v1/asset/categories/page      分页查询分类列表
 * - GET /v1/asset/categories/{id}      获取分类详情
 * - POST /v1/asset/categories          创建分类
 * - PUT /v1/asset/categories/{id}      更新分类
 * - DELETE /v1/asset/categories/{id}   删除分类（逻辑删除）
 * - GET /v1/asset/categories/tree      获取分类树形结构
 */
import { get, post, put, del } from '../request'
import type { AssetCategory } from '../../types/asset'

// ============================================================
// 后端字段类型定义（AssetCategoryNew）
// ============================================================

/** 后端分类实体原始结构 */
interface CategoryBackendVO {
  categoryId?: number
  categoryCode?: string
  categoryName?: string
  parentId?: number
  depreciationMethod?: number
  usefulLifeYears?: number
  residualRate?: number
  sortOrder?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 后端分页响应 */
interface CategoryPageBackend {
  records: CategoryBackendVO[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
}

// ============================================================
// 数据转换：后端实体 → 前端 AssetCategory
// ============================================================

/**
 * 后端 VO → 前端 AssetCategory 转换
 * 处理字段名映射、ID 类型转换、折旧年限单位转换
 * - usefulLifeYears（年）→ usefulLifeMonthsOverride（月）
 * - residualRate（0.05）→ salvageRateOverride（5，百分比）
 * - parentId=0 表示顶级分类，转换为 null
 */
function toCategory(vo: CategoryBackendVO): AssetCategory {
  const parentId = vo.parentId != null && vo.parentId !== 0
    ? String(vo.parentId)
    : null
  return {
    id: String(vo.categoryId ?? ''),
    parentId,
    code: vo.categoryCode || '',
    name: vo.categoryName || '',
    sortOrder: vo.sortOrder ?? 0,
    usefulLifeMonthsOverride: vo.usefulLifeYears != null ? vo.usefulLifeYears * 12 : undefined,
    salvageRateOverride: vo.residualRate != null ? vo.residualRate * 100 : undefined,
    assetCount: 0,
    totalValue: 0,
    monthlyDepreciation: 0,
    createTime: vo.createTime || '',
  }
}

/**
 * 前端 AssetCategory → 后端创建/更新 DTO
 */
function toBackendDTO(data: Partial<AssetCategory>): Record<string, unknown> {
  const dto: Record<string, unknown> = {}
  if (data.code !== undefined) dto.categoryCode = data.code
  if (data.name !== undefined) dto.categoryName = data.name
  if (data.parentId !== undefined) {
    dto.parentId = data.parentId ? Number(data.parentId) : 0
  }
  if (data.sortOrder !== undefined) dto.sortOrder = data.sortOrder
  if (data.usefulLifeMonthsOverride !== undefined) {
    dto.usefulLifeYears = Math.floor(data.usefulLifeMonthsOverride / 12)
  }
  if (data.salvageRateOverride !== undefined) {
    dto.residualRate = data.salvageRateOverride / 100
  }
  return dto
}

/**
 * 将扁平列表构建为树形结构
 * 根据 parentId 字段组装父子关系
 */
function buildTree(list: AssetCategory[]): AssetCategory[] {
  const map = new Map<string, AssetCategory>()
  const roots: AssetCategory[] = []

  // 第一遍：建立 id → node 映射
  for (const item of list) {
    map.set(item.id, { ...item, children: [] })
  }

  // 第二遍：根据 parentId 组装树
  for (const item of list) {
    const node = map.get(item.id)!
    if (item.parentId && map.has(item.parentId)) {
      const parent = map.get(item.parentId)!
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  }

  return roots
}

// ============================================================
// API 实现
// ============================================================

export const categoryApi = {
  /**
   * 获取分类列表（扁平化）
   * 后端路径：GET /v1/asset/categories/page
   */
  async getList(): Promise<AssetCategory[]> {
    const res = await get<CategoryPageBackend | null>('/v1/asset/categories/page', {
      page: 1,
      size: 1000,
    })
    const records = (res?.records || []).map(toCategory)
    return records
  },

  /**
   * 获取分类树结构
   * 后端路径：GET /v1/asset/categories/tree
   * 注：后端返回扁平列表，前端根据 parentId 构建树形结构
   */
  async getTree(): Promise<AssetCategory[]> {
    const res = await get<CategoryBackendVO[] | null>('/v1/asset/categories/tree')
    const list = (res || []).map(toCategory)
    return buildTree(list)
  },

  /**
   * 根据ID获取分类详情
   * 后端路径：GET /v1/asset/categories/{id}
   */
  async getById(id: string): Promise<AssetCategory | null> {
    try {
      const res = await get<CategoryBackendVO>(`/v1/asset/categories/${id}`)
      return res ? toCategory(res) : null
    } catch {
      return null
    }
  },

  /**
   * 创建分类
   * 后端路径：POST /v1/asset/categories
   */
  async create(data: Partial<AssetCategory>): Promise<AssetCategory> {
    const dto = toBackendDTO(data)
    const res = await post<CategoryBackendVO>('/v1/asset/categories', dto)
    return toCategory(res)
  },

  /**
   * 更新分类
   * 后端路径：PUT /v1/asset/categories/{id}
   */
  async update(id: string, data: Partial<AssetCategory>): Promise<AssetCategory | null> {
    const dto = toBackendDTO(data)
    const res = await put<CategoryBackendVO>(`/v1/asset/categories/${id}`, dto)
    return res ? toCategory(res) : null
  },

  /**
   * 删除分类（逻辑删除）
   * 后端路径：DELETE /v1/asset/categories/{id}
   */
  async delete(id: string): Promise<boolean> {
    await del<void>(`/v1/asset/categories/${id}`)
    return true
  },
}
