/**
 * 资产处置管理API
 * 对应后端: /v1/asset/disposal (AssetDisposalController)
 *
 * 后端接口：
 * - GET /v1/asset/disposal/page          分页查询处置列表（返回 List<Map>）
 * - GET /v1/asset/disposal/{id}          获取处置详情（返回 Map）
 * - POST /v1/asset/disposal              创建处置申请（assetId/type/reason/handler 为 RequestParam）
 * - POST /v1/asset/disposal/{id}/approve 审批处置申请（approved/comment 为 RequestParam）
 *
 * 后端返回 Map<String, Object> 结构，字段名可能不固定，
 * 此处通过宽松类型映射转换为前端 DisposalRecord
 */
import { get, post } from '../request'
import type { RequestOptions } from '../request'
import type {
  DisposalRecord,
  DisposalCreateDTO,
  DisposalApprovalDTO,
  DisposalStatus,
  DisposalType,
  PageResponse,
} from '../../types/asset'
import { DisposalStatus as DS, DisposalType as DT } from '../../types/asset'

// ============================================================
// 后端字段类型定义（Map<String, Object>）
// ============================================================

/** 后端处置记录（Map 结构，字段名宽松） */
interface DisposalBackend {
  id?: number
  disposalId?: number
  disposalNo?: string
  assetId?: number
  assetCode?: string
  assetName?: string
  categoryName?: string
  type?: string
  disposalType?: string
  netValue?: number
  disposalAmount?: number
  gainLoss?: number
  status?: string | number
  applicantName?: string
  applicantId?: number
  approverName?: string
  approvalTime?: string
  rejectReason?: string
  reason?: string
  remark?: string
  handler?: string
  disposalDate?: string
  createTime?: string
}

// ============================================================
// 状态映射
// ============================================================

/** 后端状态 → 前端 DisposalStatus 映射 */
function toDisposalStatus(status: string | number | undefined): DisposalStatus {
  const s = String(status ?? '').toLowerCase()
  if (s === 'approved' || s === '2') return DS.APPROVED
  if (s === 'completed' || s === '3' || s === 'done') return DS.COMPLETED
  if (s === 'rejected' || s === '4') return DS.REJECTED
  return DS.PENDING
}

/** 前端 DisposalType → 后端处置类型字符串 */
function toBackendType(type: DisposalType): string {
  switch (type) {
    case DT.SALE: return 'sale'
    case DT.SCRAP: return 'scrap'
    case DT.DONATE: return 'donate'
    case DT.TRANSFER_OUT: return 'transfer_out'
    default: return 'scrap'
  }
}

/** 后端处置类型 → 前端 DisposalType */
function toDisposalType(type: string | undefined): DisposalType {
  const t = (type || '').toLowerCase()
  if (t === 'sale') return DT.SALE
  if (t === 'donate') return DT.DONATE
  if (t === 'transfer_out' || t === 'transferout') return DT.TRANSFER_OUT
  return DT.SCRAP
}

// ============================================================
// 数据转换
// ============================================================

/**
 * 后端 Map → 前端 DisposalRecord 转换
 * 处理字段名兼容、ID 类型转换、状态/类型枚举映射
 */
function toDisposalRecord(vo: DisposalBackend): DisposalRecord {
  const id = vo.id ?? vo.disposalId ?? 0
  const typeStr = vo.type || vo.disposalType
  return {
    id: String(id),
    disposalNo: vo.disposalNo || `DSP-${id}`,
    assetId: vo.assetId != null ? String(vo.assetId) : '',
    assetCode: vo.assetCode || '',
    assetName: vo.assetName || '',
    categoryName: vo.categoryName,
    disposalType: toDisposalType(typeStr),
    netValue: vo.netValue ?? 0,
    disposalAmount: vo.disposalAmount ?? 0,
    gainLoss: vo.gainLoss ?? 0,
    status: toDisposalStatus(vo.status),
    applicantId: vo.applicantId != null ? String(vo.applicantId) : undefined,
    applicantName: vo.applicantName || vo.handler,
    approverName: vo.approverName,
    approvalTime: vo.approvalTime,
    rejectReason: vo.rejectReason,
    remark: vo.remark || vo.reason,
    disposalDate: vo.disposalDate,
    createTime: vo.createTime || '',
  }
}

// ============================================================
// API 实现
// ============================================================

export const disposalApi = {
  /**
   * 获取处置记录列表
   * 后端路径：GET /v1/asset/disposal/page
   * 后端返回 List<Map>（非分页结构），前端包装为 PageResponse
   */
  async getList(params?: { status?: DisposalStatus; disposalType?: DisposalType }): Promise<PageResponse<DisposalRecord>> {
    const query: Record<string, unknown> = {
      page: 1,
      size: 1000,
    }
    const res = await get<DisposalBackend[] | null>('/v1/asset/disposal/page', query)
    let records = (res || []).map(toDisposalRecord)

    // 后端不支持状态/类型筛选，前端过滤
    if (params?.status) {
      records = records.filter(r => r.status === params.status)
    }
    if (params?.disposalType) {
      records = records.filter(r => r.disposalType === params.disposalType)
    }

    return {
      records,
      total: records.length,
      current: 1,
      size: 1000,
      pages: 1,
    }
  },

  /**
   * 根据ID获取处置记录
   * 后端路径：GET /v1/asset/disposal/{id}
   */
  async getById(id: string): Promise<DisposalRecord | null> {
    try {
      const res = await get<DisposalBackend | null>(`/v1/asset/disposal/${id}`)
      return res ? toDisposalRecord(res) : null
    } catch {
      return null
    }
  },

  /**
   * 创建处置申请
   * 后端路径：POST /v1/asset/disposal
   * 后端使用 @RequestParam 接收参数，返回处置单ID（Long）
   */
  async create(data: DisposalCreateDTO): Promise<DisposalRecord> {
    const params: Record<string, unknown> = {
      assetId: data.assetId,
      type: toBackendType(data.disposalType),
      reason: data.remark || '',
    }
    const disposalId = await post<number>('/v1/asset/disposal', undefined, {
      params,
    } as Partial<RequestOptions>)
    return {
      id: String(disposalId ?? ''),
      disposalNo: `DSP-${disposalId ?? ''}`,
      assetId: data.assetId,
      assetCode: '',
      assetName: '',
      disposalType: data.disposalType,
      netValue: 0,
      disposalAmount: data.disposalAmount,
      gainLoss: -data.disposalAmount,
      status: DS.PENDING,
      disposalDate: data.disposalDate,
      remark: data.remark,
      createTime: new Date().toISOString(),
    }
  },

  /**
   * 更新处置记录
   * 后端无对应接口，抛出异常提示
   */
  async update(_id: string, _data: Partial<DisposalRecord>): Promise<DisposalRecord | null> {
    throw new Error('后端暂不支持直接更新处置记录')
  },

  /**
   * 删除处置记录
   * 后端无对应接口，抛出异常提示
   */
  async delete(_id: string): Promise<boolean> {
    throw new Error('后端暂不支持删除处置记录')
  },

  /**
   * 审批处置申请
   * 后端路径：POST /v1/asset/disposal/{id}/approve
   * 后端使用 @RequestParam 接收 approved 和 comment
   */
  async approve(data: DisposalApprovalDTO): Promise<DisposalRecord | null> {
    const params: Record<string, unknown> = {
      approved: data.approved,
      comment: data.rejectReason || '',
    }
    await post<void>(`/v1/asset/disposal/${data.id}/approve`, undefined, {
      params,
    } as Partial<RequestOptions>)
    // 审批后重新获取详情
    return await disposalApi.getById(data.id)
  },

  /**
   * 驳回处置申请
   * 后端路径：POST /v1/asset/disposal/{id}/approve（approved=false）
   */
  async reject(id: string, rejectReason: string): Promise<DisposalRecord | null> {
    const params: Record<string, unknown> = {
      approved: false,
      comment: rejectReason,
    }
    await post<void>(`/v1/asset/disposal/${id}/approve`, undefined, {
      params,
    } as Partial<RequestOptions>)
    return await disposalApi.getById(id)
  },

  /**
   * 执行处置（完成处置流程）
   * 后端无对应接口，抛出异常提示
   */
  async execute(_id: string): Promise<DisposalRecord | null> {
    throw new Error('后端暂不支持执行处置操作，请通过审批流程完成')
  },
}
