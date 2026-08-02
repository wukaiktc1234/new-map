/**
 * 应收账款API
 * 对应后端: /v1/finance/receivables
 *
 * 【设计说明】后端 ReceivableController 5 个端点：create/getDetail/getPage/confirmPayment/writeOff
 * 缺少 update（PUT /{id}）和 delete（DELETE /{id}）端点。
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post（替代 silentGet/silentPost）
 * - 内置 DataConverter 处理：
 *   - 状态数字↔字符串（后端 1~4 ↔ 前端 'unpaid'/'partial'/'settled'/'overdue'）
 *   - 金额字段 amount/receivedAmount/remainAmount 分↔元
 */
import { get, post } from '../request'
import { ReceivableDataConverter, ReceivablePayableStatusMap } from './converters'
import type {
  FinanceReceivable,
  FinanceReceivableFormData,
  FinanceReceivableQueryForm,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：前端 page/size → 后端 current/size
 * 同时转换 status 字段（前端语义字符串 → 后端数字编码）
 */
function mapQueryParams(params?: FinanceReceivableQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = ReceivablePayableStatusMap.toBackend[status]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface ReceivablePageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const receivableApi = {
  /**
   * 分页查询应收账款列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/金额）
   */
  async getList(params?: FinanceReceivableQueryForm): Promise<PageResponse<FinanceReceivable>> {
    const query = mapQueryParams(params)
    const res = await get<ReceivablePageBackend | null>('/v1/finance/receivables', query)
    const records = (res?.records || []).map(item =>
      ReceivableDataConverter.toFrontend(item) as unknown as FinanceReceivable
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
   * 根据ID获取应收账款详情
   * @param id - 应收账款ID
   * @returns 应收账款详情（已转换状态/金额）
   */
  async getById(id: string): Promise<FinanceReceivable> {
    const res = await get<Record<string, unknown>>(`/v1/finance/receivables/${id}`)
    return ReceivableDataConverter.toFrontend(res) as unknown as FinanceReceivable
  },

  /**
   * 创建应收账款
   * @param data - 应收账款表单数据（前端语义字符串，金额为元）
   * @returns 创建后的应收账款（已转换状态/金额）
   */
  async create(data: FinanceReceivableFormData): Promise<FinanceReceivable> {
    const dto = ReceivableDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/receivables', dto)
    return ReceivableDataConverter.toFrontend(res) as unknown as FinanceReceivable
  },

  /**
   * 确认收款（后端 POST /{id}/payment）
   * @param id - 应收账款ID
   * @param amount - 收款金额（分，前端调用前需将元转为分传入）
   */
  async confirmPayment(id: string, amount: number): Promise<void> {
    await post<void>(`/v1/finance/receivables/${id}/payment`, null, { params: { amount } })
  },

  /**
   * 核销应收账款（后端 POST /{id}/write-off）
   * @param id - 应收账款ID
   */
  async writeOff(id: string): Promise<void> {
    await post<void>(`/v1/finance/receivables/${id}/write-off`)
  },

  // 【设计说明】后端 ReceivableController 无 PUT/DELETE 端点
  // update/delete 方法已移除，编辑/删除应通过 confirmPayment/writeOff 流程
}

export default receivableApi
