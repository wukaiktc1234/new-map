/**
 * 发票API
 * 对应后端: /v1/finance/invoices
 *
 * 【设计说明】
 * - 后端 InvoiceController 8 个端点：getPage/getDetail/create/update/delete/issue/void/redFlush
 * - 状态机：draft → issued → void / red-flushed
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 * - 内置 DataConverter 处理：
 *   - 发票状态数字↔字符串（后端 1~4 ↔ 前端 'draft'/'issued'/'cancelled'/'red_flushed'）
 *   - 发票类型数字↔字符串（后端 1~4 ↔ 前端 'special'/'normal'/'electronic'/'electronic_special'）
 *   - 金额字段 amountWithoutTax/taxAmount/totalAmount 分↔元
 */
import { get, post, put, del } from '../request'
import { InvoiceDataConverter, InvoiceStatusMap, InvoiceTypeMap } from './converters'
import type {
  FinanceInvoiceNew,
  FinanceInvoiceFormData,
  FinanceInvoiceQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 status 数字
 * - 前端 invoiceType 字符串 → 后端 invoiceType 数字
 */
function mapQueryParams(params?: FinanceInvoiceQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, invoiceType, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = InvoiceStatusMap.toBackend[status]
  }
  if (invoiceType !== undefined && typeof invoiceType === 'string') {
    result.invoiceType = InvoiceTypeMap.toBackend[invoiceType]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface InvoicePageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const invoiceApi = {
  /**
   * 分页查询发票列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型/金额）
   */
  async getList(params?: FinanceInvoiceQueryForm): Promise<PageResponse<FinanceInvoiceNew>> {
    const query = mapQueryParams(params)
    const res = await get<InvoicePageBackend | null>('/v1/finance/invoices', query)
    const records = (res?.records || []).map(item =>
      InvoiceDataConverter.toFrontend(item) as unknown as FinanceInvoiceNew
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
   * 根据ID获取发票详情
   * @param id - 发票ID
   * @returns 发票详情（已转换状态/类型/金额）
   */
  async getById(id: string): Promise<FinanceInvoiceNew> {
    const res = await get<Record<string, unknown>>(`/v1/finance/invoices/${id}`)
    return InvoiceDataConverter.toFrontend(res) as unknown as FinanceInvoiceNew
  },

  /**
   * 创建发票
   * @param data - 发票表单数据（前端语义字符串，金额为元）
   * @returns 创建后的发票（已转换状态/类型/金额）
   */
  async create(data: FinanceInvoiceFormData): Promise<FinanceInvoiceNew> {
    const dto = InvoiceDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/invoices', dto)
    return InvoiceDataConverter.toFrontend(res) as unknown as FinanceInvoiceNew
  },

  /**
   * 更新发票（仅 draft 状态可改）
   * @param id - 发票ID
   * @param data - 发票表单数据（前端语义字符串，金额为元）
   * @returns 更新后的发票（已转换状态/类型/金额）
   */
  async update(id: string, data: FinanceInvoiceFormData): Promise<FinanceInvoiceNew> {
    const dto = InvoiceDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/invoices/${id}`, dto)
    return InvoiceDataConverter.toFrontend(res) as unknown as FinanceInvoiceNew
  },

  /**
   * 删除发票（逻辑删除）
   * @param id - 发票ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/invoices/${id}`)
  },

  /**
   * 开具发票（draft → issued）
   * 对应后端 POST /v1/finance/invoices/{id}/issue
   *
   * @param id - 发票ID
   * @returns 开具后的发票（已转换状态/类型/金额）
   */
  async issue(id: string): Promise<FinanceInvoiceNew> {
    const res = await post<Record<string, unknown>>(`/v1/finance/invoices/${id}/issue`)
    return InvoiceDataConverter.toFrontend(res) as unknown as FinanceInvoiceNew
  },

  /**
   * 作废发票（issued → void）
   * 对应后端 PUT /v1/finance/invoices/{id}/void
   *
   * @param id - 发票ID
   * @returns 作废后的发票（已转换状态/类型/金额）
   */
  async void(id: string): Promise<FinanceInvoiceNew> {
    const res = await put<Record<string, unknown>>(`/v1/finance/invoices/${id}/void`)
    return InvoiceDataConverter.toFrontend(res) as unknown as FinanceInvoiceNew
  },

  /**
   * 红冲发票（issued → red-flushed）
   * 对应后端 PUT /v1/finance/invoices/{id}/red-flush
   *
   * @param id - 发票ID
   * @returns 红冲后的发票（已转换状态/类型/金额）
   */
  async redFlush(id: string): Promise<FinanceInvoiceNew> {
    const res = await put<Record<string, unknown>>(`/v1/finance/invoices/${id}/red-flush`)
    return InvoiceDataConverter.toFrontend(res) as unknown as FinanceInvoiceNew
  },
}

export default invoiceApi
