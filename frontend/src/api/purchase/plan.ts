/**
 * 采购计划 API + DataConverter
 * 对应后端: /v1/purchase/plans
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串（后端 0~5 ↔ 前端 'draft'/'pending'/'approved'/'executing'/'completed'/'rejected'）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 日期 LocalDate/LocalDateTime ↔ string
 */
import { get, post, put, del } from '../request'
// 金额转换统一委托给 utils/money（fenToYuanNumber 别名 fenToYuan 保留 2 位小数精度）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'
import { getToken } from '@/utils/auth'

import type {
  PurchasePlanInfo,
  PurchasePlanItem,
  PurchasePlanQueryForm,
  PurchasePlanFormData,
  PurchasePlanStatus,
} from '@/types/purchase-plan'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的采购计划明细 VO */
interface PurchasePlanItemBackend {
  itemId: number
  materialId: string | null
  materialName: string
  specification: string | null
  quantity: number
  unit: string
  estimatedPrice: number  // 单位：分
  isTempMaterial: number  // 0否 1是
  supplierId: number | null
  supplierName: string | null
  remark: string | null
}

/** 后端返回的采购计划 VO */
interface PurchasePlanBackend {
  planId: number
  planNo: string
  planDate: string  // yyyy-MM-dd
  departmentId: number | null
  departmentName: string | null
  totalAmount: number  // 单位：分
  itemCount: number
  createBy: number | null
  createByName: string | null
  status: number  // 0~5
  remark: string | null
  approvedByName: string | null
  approveTime: string | null  // yyyy-MM-dd HH:mm:ss
  rejectReason: string | null
  createTime: string | null
  updateTime: string | null
  items?: PurchasePlanItemBackend[] | null  // 详情接口返回
}

/** 后端 IPage 分页响应 */
interface PurchasePlanPageBackend {
  records: PurchasePlanBackend[] | null
  total: number
  current: number
  size: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端：0草稿 1待审批 2已审批 3执行中 4已完成 5已拒绝
// 前端：'draft' 'pending' 'approved' 'executing' 'completed' 'rejected'
// ============================================================

const STATUS_TO_FRONTEND: Record<number, PurchasePlanStatus> = {
  0: 'draft',
  1: 'pending',
  2: 'approved',
  3: 'executing',
  4: 'completed',
  5: 'rejected',
}

const STATUS_TO_BACKEND: Record<PurchasePlanStatus, number> = {
  draft: 0,
  pending: 1,
  approved: 2,
  executing: 3,
  completed: 4,
  rejected: 5,
}

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money，见文件顶部 import）
// ============================================================

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购计划数据转换器
 */
export const purchasePlanConverter = {
  /** 后端 Item VO → 前端 PurchasePlanItem */
  toItemFrontend(backend: PurchasePlanItemBackend): PurchasePlanItem {
    return {
      materialId: backend.materialId ?? '',
      materialName: backend.materialName,
      specification: backend.specification ?? undefined,
      quantity: Number(backend.quantity),
      unit: backend.unit,
      estimatedPrice: fenToYuan(backend.estimatedPrice),
      isTempMaterial: backend.isTempMaterial === 1,
      supplierId: backend.supplierId != null ? String(backend.supplierId) : undefined,
      supplierName: backend.supplierName ?? undefined,
      remark: backend.remark ?? '',
    }
  },

  /** 后端 VO → 前端 Info */
  toFrontend(backend: PurchasePlanBackend): PurchasePlanInfo {
    return {
      planId: String(backend.planId),
      planNo: backend.planNo,
      planDate: backend.planDate,
      departmentId: backend.departmentId != null ? String(backend.departmentId) : '',
      departmentName: backend.departmentName ?? '',
      totalAmount: fenToYuan(backend.totalAmount),
      itemCount: backend.itemCount,
      createBy: backend.createBy != null ? String(backend.createBy) : '',
      createByName: backend.createByName ?? '',
      status: STATUS_TO_FRONTEND[backend.status] ?? 'draft',
      remark: backend.remark ?? '',
      approvedByName: backend.approvedByName ?? '',
      approvedTime: backend.approveTime ?? '',
      rejectReason: backend.rejectReason ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 VO 列表 → 前端 Info 列表 */
  toFrontendList(list: PurchasePlanBackend[] | null | undefined): PurchasePlanInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchasePlanConverter.toFrontend)
  },

  /** 后端 VO → 前端 Info（含明细，详情接口用） */
  toFrontendWithItems(backend: PurchasePlanBackend): PurchasePlanInfo & { items: PurchasePlanItem[] } {
    const info = purchasePlanConverter.toFrontend(backend)
    return {
      ...info,
      items: (backend.items ?? []).map(purchasePlanConverter.toItemFrontend),
    }
  },

  /** 前端 Form → 后端 CreateDTO */
  toCreateDTO(form: PurchasePlanFormData): Record<string, unknown> {
    const items = form.items.map(item => ({
      materialId: item.materialId || null,
      materialName: item.materialName,
      specification: item.specification ?? null,
      quantity: item.quantity,
      unit: item.unit,
      estimatedPrice: yuanToFen(item.estimatedPrice),
      isTempMaterial: item.isTempMaterial ? 1 : 0,
      supplierId: item.supplierId ? Number(item.supplierId) : null,
      supplierName: item.supplierName ?? null,
      remark: item.remark ?? null,
    }))
    return {
      departmentId: Number(form.departmentId),
      // departmentName 由后端根据 departmentId 查询填充（PurchasePlanFormData 不含 departmentName）
      planDate: form.planDate,
      remark: form.remark ?? null,
      items,
    }
  },

  /** 前端 Form → 后端 UpdateDTO */
  toUpdateDTO(form: PurchasePlanFormData): Record<string, unknown> {
    return purchasePlanConverter.toCreateDTO(form)
  },

  /** 状态字符串 → 后端数字 */
  toBackendStatus(status: PurchasePlanStatus): number {
    return STATUS_TO_BACKEND[status] ?? 0
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 采购计划查询参数（传给后端）
 */
interface PurchasePlanQueryParams {
  current: number
  size: number
  planNo?: string
  status?: number
  departmentId?: number
  startDate?: string
  endDate?: string
  keyword?: string
  [key: string]: unknown
}

export const purchasePlanApi = {
  /**
   * 分页查询采购计划列表
   * @param params 查询参数（含分页）
   */
  async getList(params: PurchasePlanQueryForm & { page?: number; size?: number }): Promise<{ records: PurchasePlanInfo[]; total: number }> {
    const query: PurchasePlanQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.planNo) query.planNo = params.planNo
    if (params.status) query.status = purchasePlanConverter.toBackendStatus(params.status)
    if (params.departmentId) query.departmentId = Number(params.departmentId)
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate
    if (params.keyword) query.keyword = params.keyword

    const res = await get<PurchasePlanPageBackend | null>('/v1/purchase/plans', query)
    const records = purchasePlanConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询采购计划详情（含明细）
   * @param id 计划ID
   */
  async getById(id: string): Promise<(PurchasePlanInfo & { items: PurchasePlanItem[] }) | null> {
    const res = await get<PurchasePlanBackend | null>(`/v1/purchase/plans/${id}`)
    return res ? purchasePlanConverter.toFrontendWithItems(res) : null
  },

  /**
   * 创建采购计划
   * @param form 表单数据
   */
  async create(form: PurchasePlanFormData): Promise<PurchasePlanInfo & { items: PurchasePlanItem[] }> {
    const dto = purchasePlanConverter.toCreateDTO(form)
    const res = await post<PurchasePlanBackend>('/v1/purchase/plans', dto)
    return purchasePlanConverter.toFrontendWithItems(res)
  },

  /**
   * 更新采购计划（仅草稿状态可更新）
   * @param id 计划ID
   * @param form 表单数据
   */
  async update(id: string, form: PurchasePlanFormData): Promise<PurchasePlanInfo & { items: PurchasePlanItem[] }> {
    const dto = purchasePlanConverter.toUpdateDTO(form)
    const res = await put<PurchasePlanBackend>(`/v1/purchase/plans/${id}`, dto)
    return purchasePlanConverter.toFrontendWithItems(res)
  },

  /**
   * 删除采购计划（仅草稿/已拒绝状态可删除）
   * @param id 计划ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/plans/${id}`)
  },

  /**
   * 提交审批（草稿 → 待审批）
   * @param id 计划ID
   */
  async submit(id: string): Promise<void> {
    await put<void>(`/v1/purchase/plans/${id}/submit`)
  },

  /**
   * 审批通过（待审批 → 已审批）
   * @param id 计划ID
   * @param approvedBy 审批人（可选，后端默认从认证上下文获取）
   */
  async approve(id: string, approvedBy?: string): Promise<void> {
    const url = approvedBy
      ? `/v1/purchase/plans/${id}/approve?approvedBy=${encodeURIComponent(approvedBy)}`
      : `/v1/purchase/plans/${id}/approve`
    await put<void>(url)
  },

  /**
   * 审批拒绝（待审批 → 已拒绝）
   * @param id 计划ID
   * @param reason 拒绝原因
   */
  async reject(id: string, reason: string): Promise<void> {
    await put<void>(`/v1/purchase/plans/${id}/reject?reason=${encodeURIComponent(reason)}`)
  },

  /**
   * 开始执行（已审批 → 执行中）
   * @param id 计划ID
   */
  async execute(id: string): Promise<void> {
    await put<void>(`/v1/purchase/plans/${id}/execute`)
  },

  /**
   * 根据采购计划生成采购订单（草稿；幂等：已生成过则返回既有订单）
   * @param id 计划ID
   */
  async generateOrder(id: string): Promise<void> {
    await post<void>(`/v1/purchase/plans/${id}/generate-order`)
  },

  /**
   * 从库存预警生成采购计划（草稿）
   */
  async generateFromStock(): Promise<PurchasePlanInfo & { items: PurchasePlanItem[] }> {
    const res = await post<PurchasePlanBackend>('/v1/purchase/plans/generate-from-stock')
    return purchasePlanConverter.toFrontendWithItems(res)
  },

  /**
   * 导出采购计划 Excel（浏览器直接下载）
   * @param params 查询条件
   */
  async exportExcel(params: PurchasePlanQueryForm & { page?: number; size?: number }): Promise<void> {
    const query = new URLSearchParams()
    if (params.planNo) query.set('planNo', params.planNo)
    if (params.status != null && params.status !== '') query.set('status', String(params.status))
    if (params.keyword) query.set('keyword', params.keyword)
    const token = getToken()
    const resp = await fetch(`/api/v1/purchase/plans/export?${query.toString()}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
    if (!resp.ok) throw new Error(`导出失败（HTTP ${resp.status}）`)
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `采购计划导出_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  },
}

export default purchasePlanApi
