/**
 * 采购申请 API + DataConverter
 * 对应后端: /v1/purchase/requests
 *
 * DataConverter 处理内容：
 * - 采购申请的 status 为字符串（语义字符串），前后端一致，无需转换
 * - total_amount 为 Long（分），前端 number（元），需分↔元转换
 *   - 分→元：fen / 100（toFrontend 时使用）
 *   - 元→分：Math.round(yuan * 100)（toCreateDTO/toUpdateDTO 时使用，但当前后端在 Service 层计算 totalAmount，前端不发送）
 * - items 中的 estimatedPrice/subtotalAmount 仍为 BigDecimal（元），不在本转换范围
 * - requestId/itemId 均为 String（UUID），无需 number↔string 转换
 * - 后端 DTO 未返回 updateBy/deletedBy/deletedTime，由 converter 填充默认空串
 * - 后端 LocalDateTime 序列化为 ISO 字符串，前端直接使用
 */
import { get, post, put, del } from '../request'

import type {
  PurchaseRequest,
  PurchaseRequestInfo,
  PurchaseRequestItem,
  PurchaseRequestCreateParams,
  PurchaseRequestUpdateParams,
  PurchaseRequestQueryParams,
  PurchaseRequestApproveParams,
} from '@/types/purchase-request'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的采购申请明细 DTO */
interface PurchaseRequestItemBackend {
  itemId: string | null
  requestId: string | null
  foodId: string | null
  foodName: string | null
  foodCode: string | null
  specification: string | null
  quantity: number | null        // BigDecimal 序列化为 number
  unit: string | null
  estimatedPrice: number | null  // BigDecimal 序列化为 number（元）
  subtotalAmount: number | null   // BigDecimal 序列化为 number（元）
  remark: string | null
  plannedReceiverType: string | null
  plannedStoreId: string | null
  plannedWarehouseId: number | string | null
}

/** 后端返回的采购申请 DTO（Controller 返回 PurchaseRequestDTO） */
interface PurchaseRequestBackend {
  requestId: string | null
  requestNo: string | null
  title: string | null
  requestType: string | null
  departmentId: string | null
  departmentName: string | null
  applicantId: string | null
  applicantName: string | null
  totalAmount: number | null       // Long 分（后端），前端转为元
  status: string | null             // 语义字符串：draft/pending/approved/rejected/completed/cancelled
  priority: string | null
  expectedDate: string | null       // yyyy-MM-dd
  description: string | null
  rejectReason: string | null
  /** 驳回次数（A1：达到 3 次后禁止重新提交，需管理员重置） */
  rejectCount: number | null
  approvedBy: string | null
  approvedTime: string | null       // yyyy-MM-dd'T'HH:mm:ss
  budgetId: string | null
  budgetStatus: string | null
  createTime: string | null         // yyyy-MM-dd'T'HH:mm:ss
  createBy: string | null
  updateTime: string | null         // yyyy-MM-dd'T'HH:mm:ss
  // 注意：后端 DTO 当前未返回 updateBy/deletedBy/deletedTime，由 converter 填充默认空串
  items?: PurchaseRequestItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface PurchaseRequestPageBackend {
  records: PurchaseRequestBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

/** 后端统计响应 */
interface PurchaseRequestStatisticsBackend {
  draft?: number
  pending?: number
  approved?: number
  rejected?: number
  completed?: number
  cancelled?: number
  [key: string]: unknown
}

/** 后端待处理数量响应 */
interface PurchaseRequestPendingCountBackend {
  count: number
  [key: string]: unknown
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购申请数据转换器（API 层，处理后端 DTO ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 purchaseRequestConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 request.ts 内部使用，不通过 index.ts 导出。
 *
 * 转换说明：
 * - status：后端为语义字符串（draft/pending/approved/rejected/completed/cancelled），前端同为字符串，无需转换
 * - totalAmount：后端为 Long（分），前端为 number（元），需分↔元转换（分→元：/ 100）
 * - ID：后端为 String（UUID），前端为 string，无需转换
 * - 审计字段：后端 DTO 未返回 updateBy/deletedBy/deletedTime，由 converter 填充默认空串
 */
export const purchaseRequestConverter = {
  /** 后端明细 DTO → 前端 PurchaseRequestItem */
  toItemFrontend(backend: PurchaseRequestItemBackend): PurchaseRequestItem {
    return {
      itemId: backend.itemId ?? '',
      requestId: backend.requestId ?? '',
      materialId: backend.foodId ?? '',
      materialName: backend.foodName ?? '',
      materialCode: backend.foodCode ?? '',
      specification: backend.specification ?? '',
      quantity: backend.quantity != null ? Number(backend.quantity) : 0,
      unit: backend.unit ?? '',
      estimatedPrice: backend.estimatedPrice != null ? Number(backend.estimatedPrice) : 0,
      subtotalAmount: backend.subtotalAmount != null ? Number(backend.subtotalAmount) : 0,
      remark: backend.remark ?? '',
      plannedReceiverType: (backend.plannedReceiverType as 'STORE' | 'WAREHOUSE') ?? undefined,
      plannedStoreId: backend.plannedStoreId ?? undefined,
      plannedWarehouseId: backend.plannedWarehouseId != null ? String(backend.plannedWarehouseId) : undefined,
    }
  },

  /** 后端 DTO → 前端 PurchaseRequest */
  toFrontend(backend: PurchaseRequestBackend): PurchaseRequest {
    return {
      requestId: backend.requestId ?? '',
      requestNo: backend.requestNo ?? '',
      title: backend.title ?? '',
      requestType: backend.requestType ?? 'routine',
      departmentId: backend.departmentId ?? '',
      departmentName: backend.departmentName ?? '',
      createBy: backend.applicantId ?? '',
      createByName: backend.applicantName ?? '',
      totalAmount: backend.totalAmount != null ? Number(backend.totalAmount) / 100 : 0,
      status: backend.status ?? 'draft',
      priority: backend.priority ?? 'normal',
      expectedDate: backend.expectedDate ?? '',
      description: backend.description ?? '',
      rejectReason: backend.rejectReason ?? '',
      rejectCount: backend.rejectCount ?? 0,
      approvedByName: backend.approvedBy ?? '',
      approvedTime: backend.approvedTime ?? '',
      budgetId: backend.budgetId ?? '',
      budgetStatus: backend.budgetStatus ?? '',
      createTime: backend.createTime ?? '',
      // 后端 DTO 当前未返回，填充默认空串
      updateBy: '',
      updateTime: backend.updateTime ?? '',
      deletedBy: '',
      deletedTime: '',
      items: (backend.items ?? []).map(purchaseRequestConverter.toItemFrontend),
    }
  },

  /** 后端 DTO 列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseRequestBackend[] | null | undefined): PurchaseRequestInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseRequestConverter.toFrontend)
  },

  /** 前端表单数据 → 后端 CreateDTO 格式 */
  toCreateDTO(form: PurchaseRequestCreateParams): Record<string, unknown> {
    const items = (form.items ?? []).map(item => ({
      foodId: item.materialId ?? null,
      foodName: item.materialName,
      foodCode: item.materialCode ?? null,
      specification: item.specification ?? null,
      quantity: item.quantity ?? 0,
      unit: item.unit ?? null,
      estimatedPrice: item.estimatedPrice ?? 0,
      remark: item.remark ?? null,
      plannedReceiverType: item.plannedReceiverType ?? null,
      plannedStoreId: item.plannedStoreId ?? null,
      plannedWarehouseId: item.plannedWarehouseId != null ? Number(item.plannedWarehouseId) : null,
    }))
    return {
      title: form.title,
      requestType: form.requestType ?? null,
      departmentId: form.departmentId ?? null,
      departmentName: form.departmentName ?? null,
      applicantId: form.createBy ?? null,
      applicantName: form.createByName ?? null,
      priority: form.priority ?? null,
      expectedDate: form.expectedDate ?? null,
      description: form.description ?? null,
      budgetId: form.budgetId ?? null,
      items,
    }
  },

  /** 前端更新表单 → 后端 UpdateDTO 格式 */
  toUpdateDTO(form: PurchaseRequestUpdateParams): Record<string, unknown> {
    const items = (form.items ?? []).map(item => ({
      itemId: item.itemId ?? null,
      foodId: item.materialId ?? null,
      foodName: item.materialName,
      foodCode: item.materialCode ?? null,
      specification: item.specification ?? null,
      quantity: item.quantity ?? 0,
      unit: item.unit ?? null,
      estimatedPrice: item.estimatedPrice ?? 0,
      remark: item.remark ?? null,
      plannedReceiverType: item.plannedReceiverType ?? null,
      plannedStoreId: item.plannedStoreId ?? null,
      plannedWarehouseId: item.plannedWarehouseId != null ? Number(item.plannedWarehouseId) : null,
    }))
    return {
      requestId: form.requestId,
      title: form.title ?? null,
      requestType: form.requestType ?? null,
      departmentId: form.departmentId ?? null,
      departmentName: form.departmentName ?? null,
      priority: form.priority ?? null,
      expectedDate: form.expectedDate ?? null,
      description: form.description ?? null,
      budgetId: form.budgetId ?? null,
      items,
    }
  },

  /** 格式化金额（元） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return yuan.toFixed(2)
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 采购申请查询参数（传给后端）
 * 后端 Controller 接收：page/size/requestNo/status/departmentId/applicantId/departmentName/applicantName/startDate/endDate
 */
interface PurchaseRequestQueryParamsBackend {
  page: number
  size: number
  requestNo?: string
  status?: string
  departmentId?: string
  applicantId?: string
  departmentName?: string
  applicantName?: string
  startDate?: string
  endDate?: string
  [key: string]: unknown
}

export const purchaseRequestApi = {
  /**
   * 分页查询采购申请列表
   * @param params 查询参数（含分页）
   */
  async getList(
    params: PurchaseRequestQueryParams & { page?: number; size?: number },
  ): Promise<{ records: PurchaseRequest[]; total: number }> {
    const query: PurchaseRequestQueryParamsBackend = {
      page: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.requestNo) query.requestNo = params.requestNo
    if (params.status) query.status = params.status
    if (params.departmentId) query.departmentId = params.departmentId
  if (params.createBy) query.applicantId = params.createBy
  if (params.departmentName) query.departmentName = params.departmentName
  if (params.createByName) query.applicantName = params.createByName
  if (params.startDate) query.startDate = params.startDate
  if (params.endDate) query.endDate = params.endDate

    const res = await get<PurchaseRequestPageBackend | null>('/v1/purchase/requests/page', query)
    const records = purchaseRequestConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询采购申请详情（含明细）
   * @param id 申请ID
   */
  async getById(id: string): Promise<PurchaseRequest | null> {
    const res = await get<PurchaseRequestBackend | null>(`/v1/purchase/requests/${id}`)
    return res ? purchaseRequestConverter.toFrontend(res) : null
  },

  /**
   * 创建采购申请
   * @param data 表单数据
   */
  async create(data: PurchaseRequestCreateParams): Promise<PurchaseRequest> {
    const dto = purchaseRequestConverter.toCreateDTO(data)
    const res = await post<PurchaseRequestBackend>('/v1/purchase/requests', dto)
    return purchaseRequestConverter.toFrontend(res)
  },

  /**
   * 更新采购申请（仅 draft/rejected 状态可更新）
   * @param id 申请ID
   * @param data 表单数据
   */
  async update(id: string, data: PurchaseRequestUpdateParams): Promise<PurchaseRequest> {
    const payload = { ...data, requestId: id }
    const dto = purchaseRequestConverter.toUpdateDTO(payload)
    const res = await put<PurchaseRequestBackend>(`/v1/purchase/requests/${id}`, dto)
    return purchaseRequestConverter.toFrontend(res)
  },

  /**
   * 删除采购申请（逻辑删除，仅 draft 状态可删除）
   * @param id 申请ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/requests/${id}`)
  },

  /**
   * 提交审批（draft/rejected → pending）
   * @param id 申请ID
   */
  async submit(id: string): Promise<void> {
    await post<void>(`/v1/purchase/requests/${id}/submit`)
  },

  /**
   * 审批采购申请（pending → approved/rejected）
   * 后端 RequestBody: { status, remark }
   * @param id 申请ID
   * @param params 审批参数
   */
  async approve(id: string, params: PurchaseRequestApproveParams): Promise<void> {
    await post<void>(`/v1/purchase/requests/${id}/approve`, {
      status: params.status,
      remark: params.remark,
      approvedBy: params.approvedByName,
    })
  },

  /**
   * 根据采购申请生成采购订单（approved → completed）
   * @param id 申请ID
   */
  async generateOrder(id: string): Promise<PurchaseRequest | null> {
    const res = await post<PurchaseRequestBackend | null>(`/v1/purchase/requests/${id}/generate-order`)
    return res ? purchaseRequestConverter.toFrontend(res) : null
  },

  /**
   * 重置驳回次数（A1：管理员解锁被驳回 3 次限制重新提交的申请）
   * @param id 申请ID
   */
  async resetRejectCount(id: string): Promise<void> {
    await post<void>(`/v1/purchase/requests/${id}/reset-reject`)
  },

  /**
   * 获取采购申请统计
   * 后端返回各状态数量：draft/pending/approved/rejected/completed/cancelled
   */
  async getStatistics(): Promise<{
    draft: number
    pending: number
    approved: number
    rejected: number
    completed: number
    cancelled: number
    total: number
  }> {
    const res = await get<PurchaseRequestStatisticsBackend | null>('/v1/purchase/requests/statistics')
    const draft = res?.draft ?? 0
    const pending = res?.pending ?? 0
    const approved = res?.approved ?? 0
    const rejected = res?.rejected ?? 0
    const completed = res?.completed ?? 0
    const cancelled = res?.cancelled ?? 0
    return {
      draft,
      pending,
      approved,
      rejected,
      completed,
      cancelled,
      total: draft + pending + approved + rejected + completed + cancelled,
    }
  },

  /**
   * 获取待处理采购请求数量
   * 后端返回 { count: number }
   */
  async getPendingCount(): Promise<{ count: number }> {
    const res = await get<PurchaseRequestPendingCountBackend | null>('/v1/purchase/requests/status/pending')
    return { count: res?.count ?? 0 }
  },

  /**
   * 按状态查询采购申请列表
   * @param status 状态字符串（draft/pending/approved/rejected/completed/cancelled）
   */
  async getByStatus(status: string): Promise<PurchaseRequestInfo[]> {
    const res = await get<PurchaseRequestBackend[] | null>(`/v1/purchase/requests/status/${status}`)
    return purchaseRequestConverter.toFrontendList(res)
  },
}

export default purchaseRequestApi
