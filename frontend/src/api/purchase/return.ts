/**
 * 采购退货 API + DataConverter
 * 对应后端：/v1/purchase/returns
 *
 * DataConverter 处理内容：
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - ID number↔string（避免大数精度丢失）
 * - 日期/时间字段透传
 */
import { get, post, put, del } from '../request'
import { fenToYuanNumber } from '@/utils/money'

import type {
  PurchaseReturn,
  PurchaseReturnInfo,
  PurchaseReturnItem,
  PurchaseReturnCreateParams,
  PurchaseReturnUpdateParams,
  PurchaseReturnQueryParams,
  PurchaseReturnApproveParams,
} from '@/types/purchase-return'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的退货明细 DTO */
interface PurchaseReturnItemBackend {
  id: number | null
  returnId: number | null
  stockinItemId: number | null
  materialId: number | null
  materialName: string | null
  specification: string | null
  unit: string | null
  quantity: number | null
  unitPrice: number | null
  totalAmount: number | null
  returnReason: string | null
  batchNo: string | null
  createTime: string | null
}

/** 后端返回的退货单 DTO */
interface PurchaseReturnBackend {
  id: number | null
  returnNo: string | null
  stockinId: number | null
  stockinNo: string | null
  orderId: number | null
  orderNo: string | null
  supplierId: number | null
  supplierName: string | null
  warehouseId: number | null
  returnDate: string | null
  totalQuantity: number | null
  totalAmount: number | null
  refundMethod: string | null
  status: string | null
  approvalRemark: string | null
  relatedPayableId: number | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
  items?: PurchaseReturnItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface PurchaseReturnPageBackend {
  records: PurchaseReturnBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// DataConverter
// ============================================================

export const purchaseReturnConverter = {
  /** 后端明细 DTO → 前端 PurchaseReturnItem */
  toItemFrontend(backend: PurchaseReturnItemBackend): PurchaseReturnItem {
    return {
      id: String(backend.id ?? ''),
      returnId: String(backend.returnId ?? ''),
      stockinItemId: String(backend.stockinItemId ?? ''),
      materialId: String(backend.materialId ?? ''),
      materialName: backend.materialName ?? '',
      specification: backend.specification ?? '',
      unit: backend.unit ?? '',
      quantity: backend.quantity != null ? Number(backend.quantity) : 0,
      unitPrice: fenToYuanNumber(backend.unitPrice),
      totalAmount: fenToYuanNumber(backend.totalAmount),
      returnReason: backend.returnReason ?? '',
      batchNo: backend.batchNo ?? '',
      createTime: backend.createTime ?? '',
    }
  },

  /** 后端 DTO → 前端 PurchaseReturn */
  toFrontend(backend: PurchaseReturnBackend): PurchaseReturn {
    return {
      id: String(backend.id ?? ''),
      returnNo: backend.returnNo ?? '',
      stockinId: String(backend.stockinId ?? ''),
      stockinNo: backend.stockinNo ?? '',
      orderId: String(backend.orderId ?? ''),
      orderNo: backend.orderNo ?? '',
      supplierId: String(backend.supplierId ?? ''),
      supplierName: backend.supplierName ?? '',
      warehouseId: String(backend.warehouseId ?? ''),
      returnDate: backend.returnDate ?? '',
      totalQuantity: backend.totalQuantity != null ? Number(backend.totalQuantity) : 0,
      totalAmount: fenToYuanNumber(backend.totalAmount),
      refundMethod: (backend.refundMethod as PurchaseReturn['refundMethod']) ?? 'offset',
      status: (backend.status as PurchaseReturn['status']) ?? 'pending',
      approvalRemark: backend.approvalRemark ?? '',
      relatedPayableId: String(backend.relatedPayableId ?? ''),
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
      items: (backend.items ?? []).map(purchaseReturnConverter.toItemFrontend),
    }
  },

  /** 后端 DTO 列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseReturnBackend[] | null | undefined): PurchaseReturnInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseReturnConverter.toFrontend)
  },

  /** 前端表单 → 后端 CreateDTO */
  toCreateDTO(form: PurchaseReturnCreateParams): Record<string, unknown> {
    return {
      stockinId: Number(form.stockinId),
      returnDate: form.returnDate ?? null,
      refundMethod: form.refundMethod ?? 'offset',
      remark: form.remark ?? null,
      items: (form.items ?? []).map(item => ({
        stockinItemId: Number(item.stockinItemId),
        materialId: Number(item.materialId),
        quantity: item.quantity,
        returnReason: item.returnReason ?? null,
        batchNo: item.batchNo ?? null,
      })),
    }
  },

  /** 前端表单 → 后端 UpdateDTO */
  toUpdateDTO(form: PurchaseReturnUpdateParams): Record<string, unknown> {
    return purchaseReturnConverter.toCreateDTO(form)
  },

  /** 格式化金额（元） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return Number(yuan).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  },
}

// ============================================================
// API 实现
// ============================================================

interface PurchaseReturnQueryParamsBackend {
  page: number
  pageSize: number
  returnNo?: string
  supplierId?: number
  stockinNo?: string
  status?: string
  startDate?: string
  endDate?: string
  [key: string]: unknown
}

export const purchaseReturnApi = {
  /**
   * 分页查询采购退货列表
   * @param params 查询参数（含分页）
   */
  async getList(
    params: PurchaseReturnQueryParams & { page?: number; size?: number },
  ): Promise<{ records: PurchaseReturn[]; total: number }> {
    const query: PurchaseReturnQueryParamsBackend = {
      page: params.page ?? 1,
      pageSize: params.size ?? 10,
    }
    if (params.returnNo) query.returnNo = params.returnNo
    if (params.supplierId) query.supplierId = Number(params.supplierId)
    if (params.stockinNo) query.stockinNo = params.stockinNo
    if (params.status) query.status = params.status
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate

    const res = await get<PurchaseReturnPageBackend | null>('/v1/purchase/returns/page', query)
    const records = purchaseReturnConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询采购退货详情（含明细）
   * @param id 退货单ID
   */
  async getById(id: string): Promise<PurchaseReturn | null> {
    const res = await get<PurchaseReturnBackend | null>(`/v1/purchase/returns/${id}`)
    return res ? purchaseReturnConverter.toFrontend(res) : null
  },

  /**
   * 创建采购退货单
   * @param data 表单数据
   */
  async create(data: PurchaseReturnCreateParams): Promise<PurchaseReturn> {
    const dto = purchaseReturnConverter.toCreateDTO(data)
    const res = await post<PurchaseReturnBackend>('/v1/purchase/returns', dto)
    return purchaseReturnConverter.toFrontend(res)
  },

  /**
   * 更新采购退货单（仅 pending 状态可更新）
   * @param id 退货单ID
   * @param data 表单数据
   */
  async update(id: string, data: PurchaseReturnUpdateParams): Promise<PurchaseReturn> {
    const payload = { ...data, returnId: id }
    const dto = purchaseReturnConverter.toUpdateDTO(payload)
    const res = await put<PurchaseReturnBackend>(`/v1/purchase/returns/${id}`, dto)
    return purchaseReturnConverter.toFrontend(res)
  },

  /**
   * 删除采购退货单（仅 pending 状态可删除）
   * @param id 退货单ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/returns/${id}`)
  },

  /**
   * 审批采购退货单（pending → approved/rejected）
   * @param id 退货单ID
   * @param params 审批参数
   */
  async approve(id: string, params: PurchaseReturnApproveParams): Promise<void> {
    await post<void>(`/v1/purchase/returns/${id}/approve`, {
      status: params.status,
      remark: params.remark ?? '',
    })
  },

  /**
   * 完成采购退货单（现金退款到账确认，approved → completed）
   * @param id 退货单ID
   */
  async complete(id: string): Promise<void> {
    await post<void>(`/v1/purchase/returns/${id}/complete`)
  },
}

export default purchaseReturnApi
