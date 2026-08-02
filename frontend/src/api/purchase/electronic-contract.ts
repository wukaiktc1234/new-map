/**
 * 电子合同 API + DataConverter
 * 对应后端: /v1/purchase/electronic-contracts
 *
 * DataConverter 处理内容：
 * - 状态字符串↔字符串（后端 draft/pending_sign/signed/cancelled ↔ 前端同名字符串，可直接对应）
 * - ID number↔string（后端 id ↔ 前端 eContractId，后端 createdBy ↔ 前端 createBy）
 * - 字段名映射（后端 contractNo ↔ 前端 eContractNo）
 * - 后端无字段时前端返回默认值（totalAmount/partyA/signMethod/startDate/contractContent/归档字段等返回空/0）
 */
import { get, post, put, del } from '../request'

import type {
  ElectronicContractInfo,
  ElectronicContractStatus,
  ElectronicContractQueryForm,
  ElectronicContractFormData,
} from '@/types/purchase-electronic-contract'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的电子合同实体（Controller 直接返回实体） */
interface ElectronicContractBackend {
  id: number
  contractNo: string | null
  contractName: string | null
  supplierId: number | null
  supplierName: string | null
  contractType: string | null
  status: string | null            // draft/pending_sign/signed/expired/cancelled
  signUrl: string | null
  viewUrl: string | null
  signDate: string | null          // ISO 时间戳
  expireDate: string | null        // ISO 时间戳
  remark: string | null
  createdBy: number | null
  createTime: string | null
  updatedBy: number | null
  updateTime: string | null
  deleted: number
}

/** 后端 IPage 分页响应 */
interface ElectronicContractPageBackend {
  records: ElectronicContractBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端字符串 ↔ 前端字符串）
// 后端 status：draft/pending_sign/signed/expired/cancelled，与前端 ElectronicContractStatus 完全一致
// ============================================================

/** 后端 status → 前端状态字符串 */
const STATUS_TO_FRONTEND: Record<string, ElectronicContractStatus> = {
  draft: 'draft',
  pending_sign: 'pending_sign',
  signed: 'signed',
  expired: 'expired',
  cancelled: 'cancelled',
}

// ============================================================
// 工具函数
// ============================================================

/** 将日期字符串归一化为后端 LocalDateTime 可解析的 ISO 格式 */
function toBackendDateTime(dateStr: string | null | undefined): string | null {
  if (!dateStr) return null
  // 前端表单日期为 YYYY-MM-DD，补全为 ISO datetime
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
    return `${dateStr}T00:00:00`
  }
  return dateStr
}

/** 将后端 ISO 时间戳透传为前端展示用的日期/时间字符串 */
function toFrontendDate(value: string | null | undefined): string {
  if (!value) return ''
  return value
}

// ============================================================
// DataConverter
// ============================================================

/**
 * 电子合同数据转换器（API 层，处理后端实体 ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 electronicContractConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 electronic-contract.ts 内部使用，不通过 index.ts 导出。
 */
export const electronicContractDataConverter = {
  /** 后端实体 → 前端 ElectronicContractInfo */
  toFrontend(backend: ElectronicContractBackend): ElectronicContractInfo {
    return {
      eContractId: String(backend.id),
      eContractNo: backend.contractNo ?? '',
      // 后端无关联采购合同字段，前端 contractId/contractNo 返回空
      contractId: '',
      contractNo: '',
      contractName: backend.contractName ?? '',
      // 后端无 partyA 字段
      partyA: '',
      supplierId: backend.supplierId != null ? String(backend.supplierId) : '',
      supplierName: backend.supplierName ?? '',
      // 后端无 totalAmount 字段
      totalAmount: 0,
      // 后端无 signMethod 字段
      signMethod: '',
      // 后端无 startDate 字段
      startDate: '',
      signDate: toFrontendDate(backend.signDate),
      expireDate: toFrontendDate(backend.expireDate),
      status: STATUS_TO_FRONTEND[backend.status ?? ''] ?? 'draft',
      signUrl: backend.signUrl ?? '',
      remark: backend.remark ?? '',
      // 后端无 contractContent 字段
      contractContent: '',
      // 后端无归档相关字段
      archived: false,
      archiveTime: '',
      archivedBy: '',
      archiveLocation: '',
      createBy: backend.createdBy != null ? String(backend.createdBy) : '',
      createTime: toFrontendDate(backend.createTime),
      updateTime: toFrontendDate(backend.updateTime),
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: ElectronicContractBackend[] | null | undefined): ElectronicContractInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(electronicContractDataConverter.toFrontend)
  },

  /** 前端表单数据 → 后端实体格式（Create/Update） */
  toBackendDTO(form: Partial<ElectronicContractFormData>): Record<string, unknown> {
    const dto: Record<string, unknown> = {}
    if (form.contractName !== undefined) dto.contractName = form.contractName
    // 后端 supplierId 为 Long，表单可能仅有 supplierName，supplierId 可选
    if (form.supplierName !== undefined) dto.supplierName = form.supplierName ?? null
    if (form.expireDate !== undefined) dto.expireDate = toBackendDateTime(form.expireDate)
    if (form.remark !== undefined) dto.remark = form.remark ?? null
    // 后端 create 时自动生成 contractNo，update 时保留原值，故不发送 eContractNo
    return dto
  },

  /** 前端状态字符串 → 后端 status（用于查询参数） */
  toBackendStatus(status: ElectronicContractStatus): string {
    return status
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
 * 电子合同查询参数（传给后端）
 */
interface ElectronicContractQueryParams {
  page: number
  pageSize: number
  contractNo?: string
  supplierId?: number
  status?: string
  startDate?: string
  endDate?: string
  [key: string]: unknown
}

export const electronicContractApi = {
  /**
   * 分页查询电子合同列表
   * @param params 查询参数（含分页）
   */
  async getList(
    params: ElectronicContractQueryForm & { page?: number; size?: number },
  ): Promise<{ records: ElectronicContractInfo[]; total: number }> {
    const query: ElectronicContractQueryParams = {
      page: params.page ?? 1,
      pageSize: params.size ?? 10,
    }
    // 前端 keyword 关键词映射到后端 contractNo 模糊查询
    if (params.eContractNo) query.contractNo = params.eContractNo
    else if (params.keyword) query.contractNo = params.keyword
    if (params.supplierId) query.supplierId = Number(params.supplierId)
    if (params.status) query.status = electronicContractDataConverter.toBackendStatus(params.status)
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate

    const res = await get<ElectronicContractPageBackend | null>('/v1/purchase/electronic-contracts/page', query)
    const records = electronicContractDataConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询电子合同详情
   * @param id 电子合同ID
   */
  async getById(id: string): Promise<ElectronicContractInfo | null> {
    const res = await get<ElectronicContractBackend | null>(`/v1/purchase/electronic-contracts/${id}`)
    return res ? electronicContractDataConverter.toFrontend(res) : null
  },

  /**
   * 创建电子合同
   * @param data 表单数据
   */
  async create(data: ElectronicContractFormData): Promise<ElectronicContractInfo> {
    const dto = electronicContractDataConverter.toBackendDTO(data)
    const res = await post<ElectronicContractBackend>('/v1/purchase/electronic-contracts', dto)
    return electronicContractDataConverter.toFrontend(res)
  },

  /**
   * 更新电子合同
   * @param id 电子合同ID
   * @param data 表单数据
   */
  async update(id: string, data: Partial<ElectronicContractFormData>): Promise<ElectronicContractInfo> {
    const dto = electronicContractDataConverter.toBackendDTO(data)
    const res = await put<ElectronicContractBackend>(`/v1/purchase/electronic-contracts/${id}`, dto)
    return electronicContractDataConverter.toFrontend(res)
  },

  /**
   * 删除电子合同
   * @param id 电子合同ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/electronic-contracts/${id}`)
  },

  /**
   * 发起签署（草稿 → 待签署）
   * @param id 电子合同ID
   * @param _data 签署备注（前端独有，后端 send 端点无需参数）
   */
  async initiateSigning(id: string, _data?: { remark?: string }): Promise<void> {
    await post<void>(`/v1/purchase/electronic-contracts/${id}/send`)
  },

  /**
   * 确认签署完成（待签署 → 已签署）
   * @param id 电子合同ID
   * @param _data 签署确认信息（前端独有，后端 sign 端点需 signUrl 参数，默认传空字符串）
   */
  async confirmSigned(id: string, _data?: { confirmedBy?: string; remark?: string }): Promise<void> {
    await post<void>(`/v1/purchase/electronic-contracts/${id}/sign`, undefined, {
      params: { signUrl: '' },
    })
  },

  /**
   * 作废电子合同（已签署/草稿 → 已取消）
   * @param id 电子合同ID
   * @param data 含作废原因
   */
  async invalidate(id: string, data?: { reason?: string }): Promise<void> {
    const reason = data?.reason || '用户作废'
    await post<void>(`/v1/purchase/electronic-contracts/${id}/cancel`, undefined, {
      params: { reason },
    })
  },

  /**
   * 获取签署记录（后端无对应端点，返回空数组占位）
   * @param _id 电子合同ID
   */
  async getSignLogs(_id: string): Promise<ElectronicSignLog[]> {
    return []
  },

  /**
   * 获取安全存证信息（后端无对应端点，返回空存证占位）
   * @param id 电子合同ID
   */
  async getEvidence(id: string): Promise<ElectronicEvidence> {
    return {
      eContractId: id,
      caCertificate: '',
      timestamp: '',
      blockchainAddress: '',
      evidenceHash: '',
      hasEvidence: false,
    }
  },
}

/** 电子合同签署记录类型 */
export interface ElectronicSignLog {
  logId: string
  action: 'initiate' | 'company_sign' | 'supplier_sign' | 'complete' | 'reject'
  operator: string
  operatorRole: string
  actionTime: string
  description: string
  ipAddress: string
}

/** 电子合同安全存证信息类型 */
export interface ElectronicEvidence {
  eContractId: string
  caCertificate: string
  timestamp: string
  blockchainAddress: string
  evidenceHash: string
  hasEvidence: boolean
}

export default electronicContractApi
