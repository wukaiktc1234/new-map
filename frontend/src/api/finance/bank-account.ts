/**
 * 银行账户API
 * 对应后端: /v1/finance/bank-accounts
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get/post/put/del（替代 silentGet/silentPost/silentPut/silentDel）
 * - 内置 DataConverter 处理：
 *   - 账户状态数字↔字符串（后端 1~3 ↔ 前端 'active'/'frozen'/'closed'）
 *   - 账户类型数字↔字符串（后端 1~5 ↔ 前端 'basic'/'general'/'payroll'/'reserve'/'other'）
 *   - 金额字段 balance/initialBalance 分↔元
 */
import { get, post, put, del } from '../request'
import { BankAccountDataConverter, BankAccountStatusMap, BankAccountTypeMap } from './converters'
import type {
  FinanceBankAccount,
  FinanceBankAccountFormData,
  FinanceBankAccountQueryForm,
  UpdateBalanceDTO,
  PageResponse,
} from '@/types/finance'

/**
 * 映射分页查询参数：
 * - 前端 page/size → 后端 current/size
 * - 前端 status 字符串 → 后端 status 数字
 * - 前端 accountType 字符串 → 后端 accountType 数字
 */
function mapQueryParams(params?: FinanceBankAccountQueryForm): Record<string, unknown> {
  if (!params) return {}
  const { page, size, status, accountType, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (size !== undefined) result.size = size
  if (status !== undefined && typeof status === 'string') {
    result.status = BankAccountStatusMap.toBackend[status]
  }
  if (accountType !== undefined && typeof accountType === 'string') {
    result.accountType = BankAccountTypeMap.toBackend[accountType]
  }
  return result
}

/** 后端 IPage 分页响应 */
interface BankAccountPageBackend {
  records: Record<string, unknown>[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const bankAccountApi = {
  /**
   * 分页查询银行账户列表
   * @param params - 查询参数
   * @returns 分页数据（已转换状态/类型/金额）
   */
  async getList(params?: FinanceBankAccountQueryForm): Promise<PageResponse<FinanceBankAccount>> {
    const query = mapQueryParams(params)
    const res = await get<BankAccountPageBackend | null>('/v1/finance/bank-accounts', query)
    const records = (res?.records || []).map(item =>
      BankAccountDataConverter.toFrontend(item) as unknown as FinanceBankAccount
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
   * 根据ID获取银行账户详情
   * @param id - 账户ID
   * @returns 账户详情（已转换状态/类型/金额）
   */
  async getById(id: string): Promise<FinanceBankAccount> {
    const res = await get<Record<string, unknown>>(`/v1/finance/bank-accounts/${id}`)
    return BankAccountDataConverter.toFrontend(res) as unknown as FinanceBankAccount
  },

  /**
   * 创建银行账户
   * @param data - 账户表单数据（前端语义字符串，金额为元）
   * @returns 创建后的账户（已转换状态/类型/金额）
   */
  async create(data: FinanceBankAccountFormData): Promise<FinanceBankAccount> {
    const dto = BankAccountDataConverter.toCreateDTO(data as unknown as Record<string, unknown>)
    const res = await post<Record<string, unknown>>('/v1/finance/bank-accounts', dto)
    return BankAccountDataConverter.toFrontend(res) as unknown as FinanceBankAccount
  },

  /**
   * 更新银行账户
   * @param id - 账户ID
   * @param data - 账户表单数据（前端语义字符串，金额为元）
   * @returns 更新后的账户（已转换状态/类型/金额）
   */
  async update(id: string, data: FinanceBankAccountFormData): Promise<FinanceBankAccount> {
    const dto = BankAccountDataConverter.toUpdateDTO(data as unknown as Record<string, unknown>)
    const res = await put<Record<string, unknown>>(`/v1/finance/bank-accounts/${id}`, dto)
    return BankAccountDataConverter.toFrontend(res) as unknown as FinanceBankAccount
  },

  /**
   * 删除银行账户
   * @param id - 账户ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/finance/bank-accounts/${id}`)
  },

  /**
   * 更新账户余额
   * 后端使用 @RequestParam Long newBalance（URL query），非 JSON body
   * balance 单位为分（后端），前端调用前需自行将元转为分传入
   *
   * @param id - 账户ID
   * @param data - 余额数据（balance 单位为分）
   * @returns 更新后的账户（已转换状态/类型/金额）
   */
  async updateBalance(id: string, data: UpdateBalanceDTO): Promise<FinanceBankAccount> {
    const res = await put<Record<string, unknown>>(
      `/v1/finance/bank-accounts/${id}/balance`,
      null,
      { params: { newBalance: data.balance } }
    )
    return BankAccountDataConverter.toFrontend(res) as unknown as FinanceBankAccount
  },
}

export default bankAccountApi
