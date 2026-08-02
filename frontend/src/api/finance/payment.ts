/**
 * 付款单API
 * 对应后端: /v1/finance/payments
 * 四账联动：应付账款 + 银行账户 + 资金流水 + 会计凭证
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback（含 buildMockPayment 辅助函数），后端故障直接抛错
 * - 改用命名导出 get/post（替代 silentGet/silentPost）
 * - 付款金额 paymentAmount 后端单位为分，前端调用前需自行将元转为分传入
 */
import { get, post } from '../request'
import type { FinancePayment, PaymentFormData, PaymentVoidFormData } from '@/types/finance'

export const paymentApi = {
  /**
   * 登记付款（四账联动）
   * 一个事务内完成：
   * 1. 创建付款单
   * 2. 更新应付账款余额和状态
   * 3. 创建资金流水（联动银行账户余额扣减）
   * 4. 生成付款会计凭证（借：应付账款 / 贷：银行存款）
   * @param data - 付款登记表单（paymentAmount 单位为分）
   * @returns 付款单（含关联的流水ID和凭证ID）
   */
  async register(data: PaymentFormData): Promise<FinancePayment> {
    return await post<FinancePayment>('/v1/finance/payments', data as unknown as Record<string, unknown>)
  },

  /**
   * 获取付款单详情
   * @param id - 付款单ID
   * @returns 付款单详情
   */
  async getById(id: string): Promise<FinancePayment> {
    return await get<FinancePayment>(`/v1/finance/payments/${id}`)
  },

  /**
   * 查询某笔应付账款的所有付款记录
   * @param payableId - 应付账款ID
   * @returns 付款记录列表
   */
  async getHistoryByPayableId(payableId: string): Promise<FinancePayment[]> {
    const res = await get<FinancePayment[] | null>(`/v1/finance/payments/by-payable/${payableId}`)
    return res ?? []
  },

  /**
   * 作废付款单（四账联动回滚）
   * @param id - 付款单ID
   * @param data - 作废表单
   * @returns 作废后的付款单
   */
  async voidPayment(id: string, data: PaymentVoidFormData): Promise<FinancePayment> {
    return await post<FinancePayment>(`/v1/finance/payments/${id}/void`, data as unknown as Record<string, unknown>)
  },
}

export default paymentApi
