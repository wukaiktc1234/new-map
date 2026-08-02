/**
 * 财务记录API
 * 对应后端: /v1/finance/records
 *
 * 【设计说明】后端 RecordController 5 个端点：create/update/getDetail/getPage/approve
 * 无 DELETE 端点，前端 delete 方法已移除（避免 405）。
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost 等）
 * - 内置 DataConverter 处理：
 *   - 审批状态数字↔字符串（后端 1~4 ↔ 前端 'draft'/'pending'/'approved'/'rejected'）
 *   - 记录类型数字↔字符串（后端 1/2 ↔ 前端 'income'/'expense'）
 *   - 金额字段 amount 分↔元
 */
import { get, post, put } from '../request'
import {
  FinanceRecordStatusMap,
  FinanceRecordTypeMap,
  fenToYuanNumber,
  yuanToFen,
} from './converters'
import type {
  FinanceRecordNew,
  FinanceRecordFormData,
  FinanceRecordQueryForm,
  PageResponse,
} from '@/types/finance'

/** 金额字段清单（分↔元） */
const AMOUNT_FIELDS = ['amount'] as const

/** 后端数据 → 前端展示（金额分→元，状态/类型数字→字符串） */
function toFrontend(backend: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...backend }
  AMOUNT_FIELDS.forEach(field => {
    if (result[field] !== undefined && result[field] !== null) {
      result[field] = fenToYuanNumber(result[field] as number)
    }
  })
  if (result.status !== undefined && result.status !== null) {
    result.status = FinanceRecordStatusMap.toFrontend[result.status as number] ?? result.status
  }
  if (result.recordType !== undefined && result.recordType !== null) {
    result.recordType = FinanceRecordTypeMap.toFrontend[result.recordType as number] ?? result.recordType
  }
  return result
}

/** 前端表单 → 后端 DTO（金额元→分，状态/类型字符串→数字） */
function toDTO(form: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...form }
  AMOUNT_FIELDS.forEach(field => {
    if (result[field] !== undefined && result[field] !== null) {
      result[field] = yuanToFen(result[field] as number | string)
    }
  })
  if (typeof result.status === 'string') {
    result.status = FinanceRecordStatusMap.toBackend[result.status] ?? result.status
  }
  if (typeof result.recordType === 'string') {
    result.recordType = FinanceRecordTypeMap.toBackend[result.recordType] ?? result.recordType
  }
  return result
}

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 approvalStatus 数字（0待审批 1已审批 2已驳回）
 * - 前端 recordType 字符串 → 后端 recordType 数字（1收入 2支出 3转账）
 */
function mapQueryParams(params?: FinanceRecordQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, recordType, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.approvalStatus = FinanceRecordStatusMap.toBackend[status]
  }
  if (recordType !== undefined && typeof recordType === 'string') {
    result.recordType = FinanceRecordTypeMap.toBackend[recordType]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface RecordPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const financeRecordApi = {
  /**
   * 分页查询财务记录列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型/金额）
   */
  async getList(params?: FinanceRecordQueryForm): Promise<PageResponse<FinanceRecordNew>> {
    const query = mapQueryParams(params)
    const res = await get<RecordPageBackend | null>('/v1/finance/records', query)
    const records = (res?.records || []).map(item =>
      toFrontend(item) as unknown as FinanceRecordNew
    )
    return {
      records,
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page ?? 1),
      size: res?.size ?? (params?.size ?? 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取财务记录详情
   * @param id - 记录ID
   * @returns 记录详情（已转换状态/类型/金额）
   */
  async getById(id: string): Promise<FinanceRecordNew> {
    const res = await get<Record<string, unknown>>(`/v1/finance/records/${id}`)
    return toFrontend(res) as unknown as FinanceRecordNew
  },

  /**
   * 创建财务记录
   * @param data - 记录表单数据（前端语义字符串，金额为元）
   * @returns 创建后的记录（已转换状态/类型/金额）
   */
  async create(data: FinanceRecordFormData): Promise<FinanceRecordNew> {
    const dto = toDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/records', dto)
    return toFrontend(res) as unknown as FinanceRecordNew
  },

  /**
   * 更新财务记录
   * @param id - 记录ID
   * @param data - 记录表单数据（前端语义字符串，金额为元）
   * @returns 更新后的记录（已转换状态/类型/金额）
   */
  async update(id: string, data: FinanceRecordFormData): Promise<FinanceRecordNew> {
    const dto = toDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/records/${id}`, dto)
    return toFrontend(res) as unknown as FinanceRecordNew
  },

  /**
   * 审批财务记录
   * 后端 POST /v1/finance/records/{id}/approve?approved={boolean}
   *
   * @param id - 记录ID
   * @param approved - 是否通过
   */
  async approve(id: string, approved: boolean): Promise<void> {
    await post<void>(`/v1/finance/records/${id}/approve`, null, { params: { approved } })
  },

  // 【设计说明】后端 RecordController 无 DELETE 端点
  // delete 方法已移除（原前端调用 DELETE /v1/finance/records/{id} 后端会返回 405）
}

export default financeRecordApi
