/**
 * 采购结算 API + DataConverter
 * 对应后端: /v1/purchase/settlements
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串（后端 status 0~4 ↔ 前端 'pending'/'partial'/'finance_reviewing'/'completed'/'overdue'）
 * - 发票状态数字↔字符串（后端 invoiceStatus 0~2 ↔ 未开票/已开票/已收票）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 字段名映射（后端 createUserId ↔ 前端 createBy 等）
 */
import { get, post, put, del } from '../request'
// 金额转换统一委托给 utils/money（fenToYuanNumber 别名 fenToYuan 保留 2 位小数精度）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  PurchaseSettlementInfo,
  PurchaseSettlementQueryForm,
  PurchaseSettlementFormData,
  PurchaseSettlementStatus,
} from '@/types/purchase-settlement'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的结算单实体（Controller 直接返回实体） */
interface PurchaseSettlementBackend {
  settlementId: number
  settlementNo: string | null
  orderId: number | null
  orderNo: string | null
  supplierId: number | null
  supplierName: string | null
  totalAmount: number | null      // 单位：分
  paidAmount: number | null        // 单位：分
  unpaidAmount: number | null      // 单位：分
  dueDate: string | null           // yyyy-MM-dd
  status: number | null            // 0/1/2/3/4
  paymentMethod: string | null
  invoiceNo: string | null
  invoiceStatus: number | null     // 0/1/2
  remark: string | null
  createUserId: number | null
  createTime: string | null        // yyyy-MM-dd HH:mm:ss
  updateTime: string | null         // yyyy-MM-dd HH:mm:ss
  deleted: number
}

/** 后端 IPage 分页响应 */
interface PurchaseSettlementPageBackend {
  records: PurchaseSettlementBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端 status：0待结算 1部分结算 2财务审核中 3已完成 4已逾期
// 后端 invoiceStatus：0未开票 1已开票 2已收票
// ============================================================

const STATUS_TO_FRONTEND: Record<number, PurchaseSettlementStatus> = {
  0: 'pending',
  1: 'partial',
  2: 'finance_reviewing',
  3: 'completed',
  4: 'overdue',
}

const STATUS_TO_BACKEND: Record<PurchaseSettlementStatus, number> = {
  pending: 0,
  partial: 1,
  finance_reviewing: 2,
  completed: 3,
  overdue: 4,
}

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money，见文件顶部 import）
// ============================================================

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购结算数据转换器（API 层，处理后端实体 ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 purchaseSettlementConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 settlement.ts 内部使用，不通过 index.ts 导出。
 */
export const purchaseSettlementConverter = {
  /** 后端实体 → 前端 PurchaseSettlementInfo */
  toFrontend(backend: PurchaseSettlementBackend): PurchaseSettlementInfo {
    return {
      settlementId: String(backend.settlementId),
      settlementNo: backend.settlementNo ?? '',
      orderId: backend.orderId != null ? String(backend.orderId) : '',
      orderNo: backend.orderNo ?? '',
      supplierId: backend.supplierId != null ? String(backend.supplierId) : '',
      supplierName: backend.supplierName ?? '',
      totalAmount: fenToYuan(backend.totalAmount),
      paidAmount: fenToYuan(backend.paidAmount),
      unpaidAmount: fenToYuan(backend.unpaidAmount),
      dueDate: backend.dueDate ?? '',
      status: backend.status != null
        ? (STATUS_TO_FRONTEND[backend.status] ?? 'pending')
        : 'pending',
      paymentMethod: backend.paymentMethod ?? '',
      remark: backend.remark ?? '',
      // 后端 createUserId 数字 → 前端 createBy 字符串
      createBy: backend.createUserId != null ? String(backend.createUserId) : '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseSettlementBackend[] | null | undefined): PurchaseSettlementInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseSettlementConverter.toFrontend)
  },

  /** 前端表单数据 → 后端 CreateDTO 格式
   *  支持携带 orderNo/supplierId/supplierName 等冗余字段（与表单 reactive 对象一致）
   */
  toCreateDTO(form: PurchaseSettlementFormData & {
    orderNo?: string
    supplierId?: string
    supplierName?: string
  }): Record<string, unknown> {
    return {
      orderId: form.orderId ? Number(form.orderId) : null,
      orderNo: form.orderNo ?? null,
      supplierId: form.supplierId ? Number(form.supplierId) : null,
      supplierName: form.supplierName ?? null,
      totalAmount: yuanToFen(form.totalAmount),
      dueDate: form.dueDate || null,
      paymentMethod: form.paymentMethod ?? null,
      remark: form.remark ?? null,
    }
  },

  /** 状态字符串 → 后端 status（用于查询参数） */
  toBackendStatus(status: PurchaseSettlementStatus): number {
    return STATUS_TO_BACKEND[status] ?? 0
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
 * 采购结算查询参数（传给后端）
 * 添加 [key: string]: unknown 索引签名以适配动态字段
 */
interface PurchaseSettlementQueryParams {
  current: number
  size: number
  settlementNo?: string
  orderNo?: string
  supplierName?: string
  supplierId?: number
  status?: number
  invoiceStatus?: number
  startDate?: string
  endDate?: string
  keyword?: string
  [key: string]: unknown
}

export const purchaseSettlementApi = {
  /**
   * 分页查询结算单列表
   * @param params 查询参数（含分页）
   */
  async getList(
    params: PurchaseSettlementQueryForm & { page?: number; size?: number },
  ): Promise<{ records: PurchaseSettlementInfo[]; total: number }> {
    const query: PurchaseSettlementQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.settlementNo) query.settlementNo = params.settlementNo
    if (params.supplierId) query.supplierId = Number(params.supplierId)
    if (params.status) query.status = purchaseSettlementConverter.toBackendStatus(params.status)
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate
    if (params.keyword) query.keyword = params.keyword

    const res = await get<PurchaseSettlementPageBackend | null>(
      '/v1/purchase/settlements',
      query,
    )
    const records = purchaseSettlementConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询结算单详情
   * @param id 结算单ID
   */
  async getById(id: string): Promise<PurchaseSettlementInfo | null> {
    const res = await get<PurchaseSettlementBackend | null>(
      `/v1/purchase/settlements/${id}`,
    )
    return res ? purchaseSettlementConverter.toFrontend(res) : null
  },

  /**
   * 创建结算单
   * @param data 表单数据（支持携带 orderNo/supplierId/supplierName 冗余字段）
   */
  async create(data: PurchaseSettlementFormData & {
    orderNo?: string
    supplierId?: string
    supplierName?: string
  }): Promise<PurchaseSettlementInfo> {
    const dto = purchaseSettlementConverter.toCreateDTO(data)
    const res = await post<PurchaseSettlementBackend>('/v1/purchase/settlements', dto)
    return purchaseSettlementConverter.toFrontend(res)
  },

  /**
   * 更新结算单
   * @param id 结算单ID
   * @param data 更新数据
   */
  async update(
    id: string,
    data: Partial<PurchaseSettlementFormData>,
  ): Promise<PurchaseSettlementInfo> {
    const payload: Record<string, unknown> = {}
    if (data.totalAmount !== undefined) payload.totalAmount = yuanToFen(data.totalAmount)
    if (data.dueDate !== undefined) payload.dueDate = data.dueDate || null
    if (data.paymentMethod !== undefined) payload.paymentMethod = data.paymentMethod ?? null
    if (data.remark !== undefined) payload.remark = data.remark ?? null
    const res = await put<PurchaseSettlementBackend>(
      `/v1/purchase/settlements/${id}`,
      payload,
    )
    return purchaseSettlementConverter.toFrontend(res)
  },

  /**
   * 删除结算单（仅 pending 状态可删）
   * @param id 结算单ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/settlements/${id}`)
  },

  /**
   * 结算操作（付款/标记完成）
   * @param id 结算单ID
   * @param params.action 'pay' 付款 | 'complete' 标记完成
   * @param params.voucherNo 付款凭证号（action=pay 时必填）
   */
  async settle(
    id: string,
    params: { action: 'pay' | 'complete'; voucherNo?: string },
  ): Promise<void> {
    const query: Record<string, unknown> = { action: params.action }
    if (params.voucherNo) query.voucherNo = params.voucherNo
    await post<void>(
      `/v1/purchase/settlements/${id}/settle`,
      undefined,
      { params: query },
    )
  },

  /**
   * 申请发票
   * @param id 结算单ID
   */
  async applyInvoice(id: string): Promise<void> {
    await post<void>(`/v1/purchase/settlements/${id}/invoice/apply`)
  },

  /**
   * 确认收票
   * @param id 结算单ID
   * @param invoiceNo 发票号
   */
  async receiveInvoice(id: string, invoiceNo: string): Promise<void> {
    const query: Record<string, unknown> = { invoiceNo }
    await post<void>(
      `/v1/purchase/settlements/${id}/invoice/receive`,
      undefined,
      { params: query },
    )
  },
}

export default purchaseSettlementApi
