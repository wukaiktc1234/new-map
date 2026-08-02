/**
 * 税务计算与申报API
 * 对应后端: /v1/finance/tax
 *
 * 【设计说明】
 * - 后端 TaxCalculationController 6 个端点：
 *   1. GET /calculate/{taxType} 计算税款
 *   2. GET /returns 申报表列表
 *   3. GET /returns/{id} 申报表详情
 *   4. POST /returns/generate 生成申报表
 *   5. POST /bureau/submit 提交电子税务局
 *   6. GET /bureau/status/{submissionId} 查询电子税务局状态
 * - 电子税务局接口为占位实现，返回"待对接"提示
 *
 * 【改造说明（2026-06-30）】
 * - 改用命名导出 get/post（替代 silentGet/silentPost）
 */
import { get, post } from '../request'

export const taxCalculationApi = {
  /**
   * 计算税款
   * 后端 GET /v1/finance/tax/calculate/{taxType}
   *
   * @param taxType - 税种：VAT（增值税）、INCOME_TAX（所得税）
   * @param params - 查询参数（storeId, year, month）
   * @returns 税款计算结果
   */
  calculate(taxType: string, params: { storeId?: string; year: string; month: string }): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>(`/v1/finance/tax/calculate/${taxType}`, params as Record<string, unknown>)
  },

  /**
   * 获取纳税申报表列表
   * 后端 GET /v1/finance/tax/returns
   *
   * @param params - 查询参数（storeId?, taxType?）
   * @returns 申报表列表
   */
  getReturns(params: Record<string, unknown>): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>('/v1/finance/tax/returns', params)
  },

  /**
   * 获取申报表详情
   * 后端 GET /v1/finance/tax/returns/{id}
   *
   * @param id - 申报表ID
   * @returns 申报表详情
   */
  getReturnDetail(id: string): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>(`/v1/finance/tax/returns/${id}`)
  },

  /**
   * 生成纳税申报表
   * 后端 POST /v1/finance/tax/returns/generate
   *
   * @param data - 生成请求（taxType, period, totalTax）
   * @returns 生成的申报表
   */
  generateReturn(data: { taxType: string; period: string; totalTax: number }): Promise<Record<string, unknown>> {
    return post<Record<string, unknown>>('/v1/finance/tax/returns/generate', data)
  },

  /**
   * 提交到电子税务局
   * 后端 POST /v1/finance/tax/bureau/submit
   *
   * @param data - 提交数据（申报表信息）
   * @returns 提交结果
   */
  submitToBureau(data: Record<string, unknown>): Promise<Record<string, unknown>> {
    return post<Record<string, unknown>>('/v1/finance/tax/bureau/submit', data)
  },

  /**
   * 查询电子税务局状态
   * 后端 GET /v1/finance/tax/bureau/status/{submissionId}
   *
   * @param submissionId - 电子税务局提交单号
   * @returns 申报状态
   */
  getBureauStatus(submissionId: string): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>(`/v1/finance/tax/bureau/status/${submissionId}`)
  },
}

export default taxCalculationApi
