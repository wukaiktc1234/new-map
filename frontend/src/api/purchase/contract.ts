/**
 * 采购合同 API + DataConverter
 * 对应后端: /v1/purchase/contracts
 *
 * DataConverter 处理内容：
 * - 状态字符串↔字符串（后端 draft/signed/terminated ↔ 前端 draft/pending/active/expired/terminated）
 * - ID number↔string（后端 id ↔ 前端 contractId，后端 createdBy ↔ 前端 createBy）
 * - 金额 元↔元（后端 BigDecimal DECIMAL(12,2) ↔ 前端 number，本模块金额未采用"分"约定）
 * - 字段名映射（后端 contractAmount ↔ 前端 totalAmount）
 * - 后端无字段时前端返回默认值（paymentTerms/deliveryTerms/contractContent/归档字段等返回空）
 */
import { get, post, put, del } from '../request'

import type {
  PurchaseContractInfo,
  PurchaseContractStatus,
  PurchaseContractQueryForm,
  PurchaseContractFormData,
} from '@/types/purchase-contract'

// 为兼容视图层扩展表单类型（含模板/正文/附件等前端独有字段）
export type { PurchaseContractFormData }
export interface PurchaseContractFormFull extends PurchaseContractFormData {
  contractContent?: string
  templateId?: string
  customTemplateName?: string
  attachments?: unknown[]
}

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的采购合同实体（Controller 直接返回实体） */
interface PurchaseContractBackend {
  id: number
  contractNo: string | null
  contractName: string | null
  supplierId: number | null
  supplierName: string | null
  contractType: string | null
  contractAmount: number | null   // 元（DECIMAL(12,2)）
  startDate: string | null        // ISO 时间戳
  endDate: string | null          // ISO 时间戳
  status: string | null           // draft/signed/terminated
  signatory: string | null
  signDate: string | null         // ISO 时间戳
  attachmentUrl: string | null
  remark: string | null
  createdBy: number | null
  createTime: string | null
  updatedBy: number | null
  updateTime: string | null
  deleted: number
}

/** 后端 IPage 分页响应 */
interface PurchaseContractPageBackend {
  records: PurchaseContractBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端字符串 ↔ 前端字符串）
// 后端 status：draft 草稿 / signed 已签署 / terminated 已终止
// 前端 PurchaseContractStatus：draft/pending/active/expired/terminated
// 后端无 pending/expired 状态，前端 active 对应后端 signed
// ============================================================

/** 后端 status → 前端状态字符串 */
const STATUS_TO_FRONTEND: Record<string, PurchaseContractStatus> = {
  draft: 'draft',
  signed: 'active',
  terminated: 'terminated',
  pending: 'pending',
  expired: 'expired',
}

/** 前端状态字符串 → 后端 status（用于查询参数） */
const STATUS_TO_BACKEND: Record<PurchaseContractStatus, string> = {
  draft: 'draft',
  pending: 'pending',
  active: 'signed',
  expired: 'expired',
  terminated: 'terminated',
}

// ============================================================
// 金额转换（元 ↔ 元）
// 本模块金额为 DECIMAL(12,2) 元，与前端 number 元直接对应，无需分/元换算
// ============================================================

function toYuan(value: number | null | undefined): number {
  if (value == null) return 0
  return Number(value)
}

/** 将日期字符串归一化为后端 LocalDateTime 可解析的 ISO 格式 */
function toBackendDateTime(dateStr: string | null | undefined): string | null {
  if (!dateStr) return null
  // 前端表单日期为 YYYY-MM-DD，补全为 ISO datetime
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
    return `${dateStr}T00:00:00`
  }
  return dateStr
}

/** 将后端 ISO 时间戳截取为前端展示用的日期/时间字符串 */
function toFrontendDate(value: string | null | undefined): string {
  if (!value) return ''
  return value
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 采购合同数据转换器（API 层，处理后端实体 ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 purchaseContractConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 contract.ts 内部使用，不通过 index.ts 导出。
 */
export const purchaseContractDataConverter = {
  /** 后端实体 → 前端 PurchaseContractInfo */
  toFrontend(backend: PurchaseContractBackend): PurchaseContractInfo {
    return {
      contractId: String(backend.id),
      contractNo: backend.contractNo ?? '',
      contractName: backend.contractName ?? '',
      supplierId: backend.supplierId != null ? String(backend.supplierId) : '',
      supplierName: backend.supplierName ?? '',
      totalAmount: toYuan(backend.contractAmount),
      startDate: toFrontendDate(backend.startDate),
      endDate: toFrontendDate(backend.endDate),
      signDate: toFrontendDate(backend.signDate),
      status: STATUS_TO_FRONTEND[backend.status ?? ''] ?? 'draft',
      // 后端无 paymentTerms/deliveryTerms 字段
      paymentTerms: '',
      deliveryTerms: '',
      remark: backend.remark ?? '',
      // 后端无 contractContent 字段
      contractContent: '',
      // 后端无归档相关字段
      archived: false,
      archiveTime: '',
      archivedBy: '',
      archiveLocation: '',
      viewCount: 0,
      lastViewTime: '',
      customTemplateName: '',
      createBy: backend.createdBy != null ? String(backend.createdBy) : '',
      createTime: toFrontendDate(backend.createTime),
      updateTime: toFrontendDate(backend.updateTime),
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: PurchaseContractBackend[] | null | undefined): PurchaseContractInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(purchaseContractDataConverter.toFrontend)
  },

  /** 前端表单数据 → 后端实体格式（Create/Update） */
  toBackendDTO(form: Partial<PurchaseContractFormFull>): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.contractName !== undefined) dto.contractName = form.contractName
    if (form.supplierId !== undefined) dto.supplierId = form.supplierId ? Number(form.supplierId) : null
    if (form.supplierName !== undefined) dto.supplierName = form.supplierName ?? null
    if (form.totalAmount !== undefined) dto.contractAmount = form.totalAmount
    if (form.startDate !== undefined) dto.startDate = toBackendDateTime(form.startDate)
    if (form.endDate !== undefined) dto.endDate = toBackendDateTime(form.endDate)
    if (form.remark !== undefined) dto.remark = form.remark ?? null
    return dto
  },

  /** 前端状态字符串 → 后端 status（用于查询参数） */
  toBackendStatus(status: PurchaseContractStatus): string {
    return STATUS_TO_BACKEND[status] ?? status
  },

  /** 格式化金额（元，带两位小数） */
  formatYuan(yuan: number | null | undefined): string {
    if (yuan == null) return '0.00'
    return yuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 采购合同查询参数（传给后端）
 */
interface PurchaseContractQueryParams {
  page: number
  pageSize: number
  contractNo?: string
  supplierId?: number
  status?: string
  startDate?: string
  endDate?: string
  [key: string]: unknown
}

export const purchaseContractApi = {
  /**
   * 分页查询采购合同列表
   * @param params 查询参数（含分页）
   */
  async getList(
    params: PurchaseContractQueryForm & { page?: number; size?: number },
  ): Promise<{ records: PurchaseContractInfo[]; total: number }> {
    const query: PurchaseContractQueryParams = {
      page: params.page ?? 1,
      pageSize: params.size ?? 10,
    }
    if (params.contractNo) query.contractNo = params.contractNo
    if (params.supplierId) query.supplierId = Number(params.supplierId)
    if (params.status) query.status = purchaseContractDataConverter.toBackendStatus(params.status)
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate

    const res = await get<PurchaseContractPageBackend | null>('/v1/purchase/contracts/page', query)
    const records = purchaseContractDataConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询采购合同详情
   * @param id 合同ID
   */
  async getById(id: string): Promise<PurchaseContractInfo | null> {
    const res = await get<PurchaseContractBackend | null>(`/v1/purchase/contracts/${id}`)
    return res ? purchaseContractDataConverter.toFrontend(res) : null
  },

  /**
   * 创建采购合同
   * @param data 表单数据
   */
  async create(data: PurchaseContractFormFull): Promise<PurchaseContractInfo> {
    const dto = purchaseContractDataConverter.toBackendDTO(data)
    const res = await post<PurchaseContractBackend>('/v1/purchase/contracts', dto)
    return purchaseContractDataConverter.toFrontend(res)
  },

  /**
   * 更新采购合同
   * @param id 合同ID
   * @param data 表单数据
   */
  async update(id: string, data: Partial<PurchaseContractFormFull>): Promise<PurchaseContractInfo> {
    const dto = purchaseContractDataConverter.toBackendDTO(data)
    const res = await put<PurchaseContractBackend>(`/v1/purchase/contracts/${id}`, dto)
    return purchaseContractDataConverter.toFrontend(res)
  },

  /**
   * 删除采购合同
   * @param id 合同ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/contracts/${id}`)
  },

  /**
   * 签署采购合同（草稿 → 已签署）
   * @param id 合同ID
   * @param signatory 签署人
   */
  async sign(id: string, signatory: string): Promise<void> {
    await post<void>(`/v1/purchase/contracts/${id}/sign`, undefined, {
      params: { signatory },
    })
  },

  /**
   * 终止采购合同（生效中 → 已终止）
   * @param id 合同ID
   * @param data 含终止原因
   */
  async terminate(id: string, data: { reason: string }): Promise<void> {
    await post<void>(`/v1/purchase/contracts/${id}/terminate`, undefined, {
      params: { reason: data.reason },
    })
  },

  /**
   * 获取合同附件列表（后端无对应端点，返回空数组占位）
   * @param _id 合同ID
   */
  async getAttachments(_id: string): Promise<ContractAttachment[]> {
    return []
  },

  /**
   * 获取合同变更记录（后端无对应端点，返回空数组占位）
   * @param _id 合同ID
   */
  async getChanges(_id: string): Promise<ContractChangeRecord[]> {
    return []
  },

  /**
   * 获取关联订单（后端无对应端点，返回空数组占位）
   * @param _id 合同ID
   */
  async getRelatedOrders(_id: string): Promise<RelatedOrder[]> {
    return []
  },
}

/** 合同附件类型 */
export interface ContractAttachment {
  name: string
  size: string
  uploadTime: string
  uploader: string
}

/** 合同变更记录类型 */
export interface ContractChangeRecord {
  changeId: string
  changeDate: string
  changeType: string
  description: string
  operator: string
  status: string
}

/** 关联订单类型 */
export interface RelatedOrder {
  orderNo: string
  orderDate: string
  totalAmount: number
  status: string
}

export default purchaseContractApi
