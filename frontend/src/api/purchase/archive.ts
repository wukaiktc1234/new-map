/**
 * 商品档案 API + DataConverter
 * 对应后端: /v1/purchase/archives
 *
 * DataConverter 处理内容：
 * - 金额分↔元、状态数字↔字符串、ID number↔string
 */
import { get, post, put, del } from '../request'
// 金额转换统一委托给 utils/money：
// - fenToYuanNumber（别名 fenToYuan）：分→元 number，保留 2 位小数精度
// - yuanToFen：标准元转分
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  MaterialArchiveInfo,
  MaterialArchiveQueryForm,
  MaterialArchiveFormData,
  MaterialArchiveStatus,
} from '@/types/purchase-archive'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的商品档案 VO */
interface MaterialArchiveBackend {
  materialId: number
  materialCode: string
  materialName: string
  categoryId: number | null
  categoryName: string | null
  unit: string
  spec: string | null
  /** 参考价（分） */
  referencePrice: number
  barcode: string | null
  origin: string | null
  shelfLife: string | null
  storageCondition: string | null
  departmentId: number | null
  supplierId: number | null
  supplierName: string | null
  /** 状态：1启用 0停用 */
  status: number
  statusName: string
  remark: string | null
  createTime: string
  updateTime: string
}

/** 后端分页响应 */
interface MaterialArchivePageBackend {
  records: MaterialArchiveBackend[] | null
  total: number
  current: number
  size: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// ============================================================

const STATUS_TO_FRONTEND: Record<number, MaterialArchiveStatus> = {
  1: 'active',
  0: 'inactive',
}

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money，见文件顶部 import）
// ============================================================

// ============================================================
// DataConverter
// ============================================================

/**
 * 商品档案数据转换器
 * - toFrontend: 后端 VO → 前端 Info（金额分→元，状态数字→字符串，ID number→string）
 * - toCreateDTO: 前端 Form → 后端 CreateDTO（金额元→分，状态字符串→数字，ID string→number）
 * - toUpdateDTO: 前端 Form → 后端 UpdateDTO（同 CreateDTO 但部分字段可选）
 */
export const materialArchiveConverter = {
  /** 后端 VO → 前端 Info */
  toFrontend(backend: MaterialArchiveBackend): MaterialArchiveInfo {
    return {
      materialId: String(backend.materialId),
      materialCode: backend.materialCode,
      materialName: backend.materialName,
      categoryId: backend.categoryId != null ? String(backend.categoryId) : '',
      categoryName: backend.categoryName ?? '',
      unit: backend.unit,
      spec: backend.spec ?? '',
      referencePrice: fenToYuan(backend.referencePrice),
      barcode: backend.barcode ?? '',
      origin: backend.origin ?? '',
      shelfLife: backend.shelfLife ?? '',
      storageCondition: backend.storageCondition ?? '',
      departmentId: backend.departmentId != null ? String(backend.departmentId) : '',
      supplierId: backend.supplierId != null ? String(backend.supplierId) : '',
      supplierName: backend.supplierName ?? '',
      status: STATUS_TO_FRONTEND[backend.status] ?? 'inactive',
      remark: backend.remark ?? '',
      createTime: backend.createTime,
      updateTime: backend.updateTime,
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: MaterialArchiveBackend[] | null | undefined): MaterialArchiveInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(materialArchiveConverter.toFrontend)
  },

  /** 前端 Form → 后端 CreateDTO */
  toCreateDTO(form: MaterialArchiveFormData): Record<string, unknown> {
    return {
      materialName: form.materialName,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      unit: form.unit,
      spec: form.spec || null,
      referencePrice: yuanToFen(form.referencePrice),
      barcode: form.barcode || null,
      origin: form.origin || null,
      shelfLife: form.shelfLife || null,
      storageCondition: form.storageCondition || null,
      departmentId: form.departmentId ? Number(form.departmentId) : null,
      supplierId: form.supplierId ? Number(form.supplierId) : null,
      remark: form.remark || null,
    }
  },

  /** 前端 Form → 后端 UpdateDTO（部分更新） */
  toUpdateDTO(form: Partial<MaterialArchiveFormData>): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.materialName !== undefined) dto.materialName = form.materialName
    if (form.categoryId !== undefined) dto.categoryId = form.categoryId ? Number(form.categoryId) : null
    if (form.unit !== undefined) dto.unit = form.unit
    if (form.spec !== undefined) dto.spec = form.spec || null
    if (form.referencePrice !== undefined) dto.referencePrice = yuanToFen(form.referencePrice)
    if (form.barcode !== undefined) dto.barcode = form.barcode || null
    if (form.origin !== undefined) dto.origin = form.origin || null
    if (form.shelfLife !== undefined) dto.shelfLife = form.shelfLife || null
    if (form.storageCondition !== undefined) dto.storageCondition = form.storageCondition || null
    if (form.departmentId !== undefined) dto.departmentId = form.departmentId ? Number(form.departmentId) : null
    if (form.supplierId !== undefined) dto.supplierId = form.supplierId ? Number(form.supplierId) : null
    if (form.remark !== undefined) dto.remark = form.remark || null
    return dto
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 商品档案查询参数（传给后端）
 */
type ArchiveQueryParams = {
  page: number
  size: number
  status?: string
  keyword?: string
  categoryId?: string
  departmentId?: string
  categoryName?: string
}

export const materialArchiveApi = {
  /**
   * 分页查询商品档案列表
   * @param params 查询参数（含分页）
   */
  async getList(params: MaterialArchiveQueryForm & { page?: number; size?: number }): Promise<{ records: MaterialArchiveInfo[]; total: number }> {
    const query: ArchiveQueryParams = {
      page: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.status) query.status = params.status
    if (params.keyword) query.keyword = params.keyword
    if (params.categoryId) query.categoryId = params.categoryId
    if (params.departmentId) query.departmentId = params.departmentId
    if (params.categoryName) query.categoryName = params.categoryName

    const res = await get<MaterialArchivePageBackend | null>('/v1/purchase/archives', query)
    const records = materialArchiveConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询商品档案详情
   * @param id 商品ID
   */
  async getById(id: string): Promise<MaterialArchiveInfo | null> {
    const res = await get<MaterialArchiveBackend | null>(`/v1/purchase/archives/${id}`)
    return res ? materialArchiveConverter.toFrontend(res) : null
  },

  /**
   * 创建商品档案
   * @param form 表单数据
   */
  async create(form: MaterialArchiveFormData): Promise<MaterialArchiveInfo> {
    const dto = materialArchiveConverter.toCreateDTO(form)
    const res = await post<MaterialArchiveBackend>('/v1/purchase/archives', dto)
    return materialArchiveConverter.toFrontend(res)
  },

  /**
   * 更新商品档案（部分更新）
   * @param id 商品ID
   * @param form 表单数据（部分字段）
   */
  async update(id: string, form: Partial<MaterialArchiveFormData>): Promise<MaterialArchiveInfo> {
    const dto = materialArchiveConverter.toUpdateDTO(form)
    const res = await put<MaterialArchiveBackend>(`/v1/purchase/archives/${id}`, dto)
    return materialArchiveConverter.toFrontend(res)
  },

  /**
   * 删除商品档案（逻辑删除）
   * @param id 商品ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/archives/${id}`)
  },

  /**
   * 更新商品状态（启用/停用）
   * @param id 商品ID
   * @param status 目标状态（前端语义化字符串）
   */
  async updateStatus(id: string, status: MaterialArchiveStatus): Promise<void> {
    // 后端用 query string 传 status，值传前端字符串（Controller 内部转换为 1/0）
    await put<void>(`/v1/purchase/archives/${id}/status?status=${status}`)
  },
}

export default materialArchiveApi
