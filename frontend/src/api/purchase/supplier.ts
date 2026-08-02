/**
 * 供应商档案 API + DataConverter
 * 对应后端: /v1/suppliers
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串、ID number↔string
 * - 后端 VO 字段少于前端类型，缺失字段返回默认值（保持前端类型完整性）
 */
import { get, post, put, del } from '../request'

import type {
  SupplierInfo,
  SupplierQueryForm,
  SupplierFormData,
  SupplierStatus,
  SupplierType,
  SupplierLevel,
  SettlementMethod,
} from '@/types/purchase-supplier'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的供应商 VO（字段少于前端 SupplierInfo） */
interface SupplierBackend {
  supplierId: number
  supplierCode: string
  supplierName: string
  contactPerson: string | null
  phone: string | null
  address: string | null
  licenseNo: string | null
  licenseExpiry: string | null
  bankName: string | null
  bankAccount: string | null
  taxNo: string | null
  settlementMethod: string | null
  paymentTerms: number | null
  category: string | null
  status: number
  statusDesc: string | null
  creditLevel: string | null
  rating: number | null
  remark: string | null
  createTime: string | null
}

/** 后端 IPage 分页响应 */
interface SupplierPageBackend {
  records: SupplierBackend[] | null
  total: number
  current: number
  size: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端状态：1合作中 0停用 2黑名单
// 前端状态：'active' 'inactive' 'frozen' 'eliminated'
// 注：frozen 和 eliminated 都映射到 2（黑名单）
// ============================================================

const STATUS_TO_FRONTEND: Record<number, SupplierStatus> = {
  1: 'active',
  0: 'inactive',
  2: 'frozen',
}

const STATUS_TO_BACKEND: Record<SupplierStatus, number> = {
  active: 1,
  inactive: 0,
  frozen: 2,
  eliminated: 2,
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 供应商数据转换器
 * - toFrontend: 后端 VO → 前端 Info（状态数字→字符串，ID number→string）
 * - toCreateDTO: 前端 Form → 后端 CreateDTO
 * - toUpdateDTO: 前端 Form → 后端 UpdateDTO（部分更新）
 */
export const supplierConverter = {
  /** 后端 VO → 前端 Info（缺失字段返回默认值） */
  toFrontend(backend: SupplierBackend): SupplierInfo {
    return {
      supplierId: String(backend.supplierId),
      supplierCode: backend.supplierCode,
      supplierName: backend.supplierName,
      // 后端 VO 不包含的字段，用默认值填充
      shortName: '',
      unifiedSocialCode: '',
      supplierType: 'raw_material' as SupplierType,
      supplierLevel: 'qualified' as SupplierLevel,
      industry: '',
      registeredCapital: 0,
      establishDate: '',
      contactPerson: backend.contactPerson ?? '',
      contactPhone: backend.phone ?? '',
      email: '',
      address: backend.address ?? '',
      bankName: backend.bankName ?? '',
      bankAccount: backend.bankAccount ?? '',
      settlementMethod: (backend.settlementMethod as SettlementMethod) ?? 'monthly',
      paymentTerms: backend.paymentTerms ?? 30,
      supplyCategories: [],
      mainCategories: backend.category ?? '',
      minOrderQuantity: 0,
      deliveryArea: '',
      businessScope: '',
      overallScore: backend.rating != null ? Number(backend.rating) : 0,
      qualityScore: 0,
      deliveryScore: 0,
      priceScore: 0,
      serviceScore: 0,
      evaluationGrade: backend.creditLevel as 'A' | 'B' | 'C' | 'D' ?? 'D',
      lastEvaluationDate: '',
      status: STATUS_TO_FRONTEND[backend.status] ?? 'inactive',
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: '',
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: SupplierBackend[] | null | undefined): SupplierInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(supplierConverter.toFrontend)
  },

  /** 前端 Form → 后端 CreateDTO */
  toCreateDTO(form: SupplierFormData): Record<string, unknown> {
    const dto: Record<string, unknown> = {
      supplierName: form.supplierName,
      contactPerson: form.contactPerson,
      phone: form.contactPhone,
      address: form.address ?? null,
      remark: form.remark ?? null,
    }
    // 以下字段后端 CreateDTO 中可能存在但前端表单不一定填写
    if (form.bankName !== undefined) dto.bankName = form.bankName || null
    if (form.bankAccount !== undefined) dto.bankAccount = form.bankAccount || null
    if (form.settlementMethod !== undefined) dto.settlementMethod = form.settlementMethod
    if (form.paymentTerms !== undefined) dto.paymentTerms = form.paymentTerms
    return dto
  },

  /** 前端 Form → 后端 UpdateDTO（部分更新） */
  toUpdateDTO(form: Partial<SupplierFormData>): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.supplierName !== undefined) dto.supplierName = form.supplierName
    if (form.contactPerson !== undefined) dto.contactPerson = form.contactPerson
    if (form.contactPhone !== undefined) dto.phone = form.contactPhone
    if (form.address !== undefined) dto.address = form.address || null
    if (form.bankName !== undefined) dto.bankName = form.bankName || null
    if (form.bankAccount !== undefined) dto.bankAccount = form.bankAccount || null
    if (form.settlementMethod !== undefined) dto.settlementMethod = form.settlementMethod
    if (form.paymentTerms !== undefined) dto.paymentTerms = form.paymentTerms
    if (form.remark !== undefined) dto.remark = form.remark || null
    return dto
  },

  /** 状态字符串 → 后端数字 */
  toBackendStatus(status: SupplierStatus): number {
    return STATUS_TO_BACKEND[status] ?? 1
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 供应商查询参数（传给后端）
 */
interface SupplierQueryParams {
  current: number
  size: number
  supplierName?: string
  status?: number
  [key: string]: unknown
}

export const supplierApi = {
  /**
   * 分页查询供应商列表
   * @param params 查询参数（含分页）
   */
  async getList(params: SupplierQueryForm & { page?: number; size?: number }): Promise<{ records: SupplierInfo[]; total: number }> {
    const query: SupplierQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.keyword) query.supplierName = params.keyword
    if (params.status) query.status = supplierConverter.toBackendStatus(params.status)

    const res = await get<SupplierPageBackend | null>('/v1/suppliers/page', query)
    const records = supplierConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询供应商详情
   * @param id 供应商ID
   */
  async getById(id: string): Promise<SupplierInfo | null> {
    const res = await get<SupplierBackend | null>(`/v1/suppliers/${id}`)
    return res ? supplierConverter.toFrontend(res) : null
  },

  /**
   * 创建供应商
   * @param form 表单数据
   */
  async create(form: SupplierFormData): Promise<SupplierInfo> {
    const dto = supplierConverter.toCreateDTO(form)
    const res = await post<SupplierBackend>('/v1/suppliers', dto)
    return supplierConverter.toFrontend(res)
  },

  /**
   * 更新供应商（部分更新）
   * @param id 供应商ID
   * @param form 表单数据（部分字段）
   */
  async update(id: string, form: Partial<SupplierFormData>): Promise<SupplierInfo> {
    const dto = supplierConverter.toUpdateDTO(form)
    const res = await put<SupplierBackend>(`/v1/suppliers/${id}`, dto)
    return supplierConverter.toFrontend(res)
  },

  /**
   * 删除供应商（逻辑删除）
   * @param id 供应商ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/suppliers/${id}`)
  },

  /**
   * 更新供应商状态
   * @param id 供应商ID
   * @param status 目标状态（前端语义化字符串）
   * @param reason 操作原因（后端该接口未定义，忽略）
   */
  async updateStatus(id: string, status: SupplierStatus, reason?: string): Promise<void> {
    const backendStatus = supplierConverter.toBackendStatus(status)
    await put<void>(`/v1/suppliers/${id}/status?status=${backendStatus}`)
    void reason
  },

  /**
   * 获取启用的供应商列表（下拉选项用）
   * 后端返回的是 List<Supplier>，而非 VO（无 statusDesc 等字段）
   */
  async getEnabledList(): Promise<SupplierInfo[]> {
    const res = await get<SupplierBackend[] | null>('/v1/suppliers/list', { status: 1 })
    return supplierConverter.toFrontendList(res)
  },

  /**
   * 批量删除供应商
   * @param ids 供应商ID列表
   */
  async batchDelete(ids: string[]): Promise<void> {
    await post<void>('/v1/suppliers/batch-delete', ids)
  },

  /**
   * 获取供应商统计信息
   */
  async getStatistics(): Promise<Record<string, unknown>> {
    const res = await get<Record<string, unknown> | null>('/v1/suppliers/statistics')
    return res ?? {}
  },
}

export default supplierApi
