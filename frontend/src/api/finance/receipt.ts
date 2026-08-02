/**
 * 收款单API
 * 对应后端: /v1/finance/receipts
 * 四账联动：应收账款 + 银行账户 + 资金流水 + 会计凭证
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback（含 buildMockReceipt 辅助函数），后端故障直接抛错
 * - 改用命名导出 get/post（替代 silentGet/silentPost）
 * - 收款金额 receiptAmount 后端单位为分，前端调用前需自行将元转为分传入
 */
import { get, post } from '../request'
import type { FinanceReceipt, ReceiptFormData } from '@/types/finance'

export const receiptApi = {
  /**
   * 登记收款（四账联动）
   * 一个事务内完成：
   * 1. 创建收款单
   * 2. 更新应收账款余额、状态和最后收款日期
   * 3. 创建资金流水（联动银行账户余额增加）
   * 4. 生成收款会计凭证（借：银行存款 / 贷：应收账款）
   * @param data - 收款登记表单（receiptAmount 单位为分）
   * @returns 收款单（含关联的流水ID和凭证ID）
   */
  async register(data: ReceiptFormData): Promise<FinanceReceipt> {
    return await post<FinanceReceipt>('/v1/finance/receipts', data as unknown as Record<string, unknown>)
  },

  /**
   * 获取收款单详情
   * @param id - 收款单ID
   * @returns 收款单详情
   */
  async getById(id: string): Promise<FinanceReceipt> {
    return await get<FinanceReceipt>(`/v1/finance/receipts/${id}`)
  },

  /**
   * 查询某笔应收账款的所有收款记录
   * @param receivableId - 应收账款ID
   * @returns 收款记录列表
   */
  async getHistoryByReceivableId(receivableId: string): Promise<FinanceReceipt[]> {
    const res = await get<FinanceReceipt[] | null>(`/v1/finance/receipts/by-receivable/${receivableId}`)
    return res ?? []
  },
}

export default receiptApi
