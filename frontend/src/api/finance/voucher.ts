/**
 * 凭证API
 * 对应后端: /v1/finance/vouchers
 *
 * 【设计说明】后端 VoucherController 无 DELETE 端点（凭证不允许物理删除），
 * 前端 delete 方法实际调用 void 端点（POST /v1/finance/vouchers/{id}/void）实现"作废"。
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost 等）
 * - 内置 DataConverter 处理：
 *   - 凭证状态数字↔字符串（后端 1~4 ↔ 前端 'draft'/'audited'/'posted'/'cancelled'）
 *   - 凭证类型数字↔字符串（后端 1~4 ↔ 前端 'receipt'/'payment'/'transfer'/'general'）
 *   - 多字段金额分↔元（debitTotal/creditTotal/debitAmount/creditAmount）
 */
import { get, post, put } from '../request'
import { VoucherDataConverter, VoucherStatusMap, VoucherTypeMap } from './converters'
import type {
  FinanceVoucherNew,
  VoucherFormData,
  VoucherQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 status 数字
 * - 前端 voucherType 字符串 → 后端 voucherType 数字
 */
function mapQueryParams(params?: VoucherQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, voucherType, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = VoucherStatusMap.toBackend[status]
  }
  if (voucherType !== undefined && typeof voucherType === 'string') {
    result.voucherType = VoucherTypeMap.toBackend[voucherType]
  }
  return result
}

/** 后端 IPage 分页响应（records 为原始后端对象，状态/类型/金额为数字） */
interface VoucherPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const voucherApi = {
  /**
   * 分页查询凭证列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型/金额）
   */
  async getList(params?: VoucherQueryForm): Promise<PageResponse<FinanceVoucherNew>> {
    const query = mapQueryParams(params)
    const res = await get<VoucherPageBackend | null>('/v1/finance/vouchers', query)
    const records = (res?.records || []).map(item =>
      VoucherDataConverter.toFrontend(item) as unknown as FinanceVoucherNew
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
   * 根据ID获取凭证详情
   * @param id - 凭证ID
   * @returns 凭证详情（已转换状态/类型/金额）
   */
  async getById(id: string): Promise<FinanceVoucherNew> {
    const res = await get<Record<string, unknown>>(`/v1/finance/vouchers/${id}`)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 创建凭证
   * @param data - 凭证表单数据（前端语义字符串，金额为元）
   * @returns 创建后的凭证（已转换状态/类型/金额）
   */
  async create(data: VoucherFormData): Promise<FinanceVoucherNew> {
    const dto = VoucherDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/vouchers', dto)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 更新凭证
   * @param id - 凭证ID
   * @param data - 凭证表单数据（前端语义字符串，金额为元）
   * @returns 更新后的凭证（已转换状态/类型/金额）
   */
  async update(id: string, data: VoucherFormData): Promise<FinanceVoucherNew> {
    const dto = VoucherDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/vouchers/${id}`, dto)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 作废凭证（替代删除）
   * 后端无 DELETE 端点，调用 POST /v1/finance/vouchers/{id}/void 实现作废
   * @param id - 凭证ID
   */
  async delete(id: string): Promise<void> {
    await post<void>(`/v1/finance/vouchers/${id}/void`)
  },

  /**
   * 审核凭证（草稿 → 已审核）
   * 对应后端 POST /v1/finance/vouchers/{id}/approve
   * @param id - 凭证ID
   * @returns 审核后的凭证（已转换状态/类型/金额）
   */
  async approve(id: string): Promise<FinanceVoucherNew> {
    const res = await post<Record<string, unknown>>(`/v1/finance/vouchers/${id}/approve`)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 过账凭证（已审核 → 已过账）
   * 对应后端 POST /v1/finance/vouchers/{id}/post
   * @param id - 凭证ID
   * @returns 过账后的凭证（已转换状态/类型/金额）
   */
  async post(id: string): Promise<FinanceVoucherNew> {
    const res = await post<Record<string, unknown>>(`/v1/finance/vouchers/${id}/post`)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 反审核凭证（已审核 → 草稿）
   * 对应后端 POST /v1/finance/vouchers/{id}/unapprove
   * @param id - 凭证ID
   * @returns 反审核后的凭证（已转换状态/类型/金额）
   */
  async unapprove(id: string): Promise<FinanceVoucherNew> {
    const res = await post<Record<string, unknown>>(`/v1/finance/vouchers/${id}/unapprove`)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 反过账凭证（已过账 → 已审核）
   * 对应后端 POST /v1/finance/vouchers/{id}/unpost
   * @param id - 凭证ID
   * @returns 反过账后的凭证（已转换状态/类型/金额）
   */
  async unpost(id: string): Promise<FinanceVoucherNew> {
    const res = await post<Record<string, unknown>>(`/v1/finance/vouchers/${id}/unpost`)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },

  /**
   * 作废凭证（草稿/已审核 → 已作废）
   * 对应后端 POST /v1/finance/vouchers/{id}/void
   * @param id - 凭证ID
   * @returns 作废后的凭证（已转换状态/类型/金额）
   */
  async void(id: string): Promise<FinanceVoucherNew> {
    const res = await post<Record<string, unknown>>(`/v1/finance/vouchers/${id}/void`)
    return VoucherDataConverter.toFrontend(res) as unknown as FinanceVoucherNew
  },
}

export default voucherApi
