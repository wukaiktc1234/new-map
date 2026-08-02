/**
 * 财务报表API
 * 对应后端: /v1/finance/reports
 *
 * 【设计说明】
 * - 后端 ReportController 7 个端点：
 *   1. GET /profit-statement?startDate&endDate (yyyy-MM-dd)
 *   2. GET /income-expense-summary?startDate&endDate (yyyy-MM-dd)
 *   3. GET /receivable-statistics (无参数)
 *   4. GET /payable-statistics (无参数)
 *   5. GET /aging-analysis (无参数)
 *   6. GET /cost-structure?period (YYYY-MM)
 *   7. GET /budget-execution?year&type (type 可选)
 * - 前端 getBalanceSheet/getCashFlowStatement 后端无对应端点（保留接口待后端补全）
 *
 * 【改造说明（2026-06-30）】
 * - 移除所有 Mock fallback，后端故障直接抛错
 * - 改用命名导出 get（替代 silentGet）
 * - 报表金额字段单位为分（与后端一致），View 层通过 fenToYuan 转换展示
 *   （保持与现有视图层调用契约一致，避免双重转换）
 *
 * 【修复说明（2026-07-03）】
 * - 补全账龄分析及其他报表端点（5.7.9/13 账龄分析路径错位修复）
 */
import { get } from '../request'
import type {
  FinanceReportQueryForm,
  ProfitStatementData,
  BalanceSheetData,
  CashFlowStatementData,
} from '@/types/finance'

/**
 * 映射报表查询参数：
 * - 前端 startDate/endDate 字符串原样传递（确保格式为 YYYY-MM-DD）
 * - 后端使用 @DateTimeFormat(pattern = "yyyy-MM-dd") 解析
 */
function mapReportParams(params: FinanceReportQueryForm): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  if (params.startDate) result.startDate = params.startDate
  if (params.endDate) result.endDate = params.endDate
  return result
}

export const reportApi = {
  /**
   * 获取利润表
   * 后端 GET /v1/finance/reports/profit-statement?startDate&endDate
   * 后端使用 @DateTimeFormat(pattern = "yyyy-MM-dd")，需确保前端传 YYYY-MM-DD 格式
   *
   * @param params - 报表查询参数
   * @returns 利润表数据（金额单位为分，View 层通过 fenToYuan 转换）
   */
  async getProfitStatement(params: FinanceReportQueryForm): Promise<ProfitStatementData> {
    const res = await get<ProfitStatementData | null>(
      '/v1/finance/reports/profit-statement',
      mapReportParams(params)
    )
    return (res ?? {}) as ProfitStatementData
  },

  /**
   * 获取资产负债表
   * 【TODO P1】后端 ReportController 无 /balance-sheet 端点，当前调用会 404
   * 待后端补全后启用真实数据返回
   *
   * @param params - 报表查询参数
   * @returns 资产负债表数据（金额单位为分，View 层通过 fenToYuan 转换）
   */
  async getBalanceSheet(params: FinanceReportQueryForm): Promise<BalanceSheetData> {
    const res = await get<BalanceSheetData | null>(
      '/v1/finance/reports/balance-sheet',
      mapReportParams(params)
    )
    return (res ?? {}) as BalanceSheetData
  },

  /**
   * 获取现金流量表
   * 【TODO P1】后端 ReportController 无 /cash-flow 端点，当前调用会 404
   * 待后端补全后启用真实数据返回
   *
   * @param params - 报表查询参数
   * @returns 现金流量表数据（金额单位为分，View 层通过 fenToYuan 转换）
   */
  async getCashFlowStatement(params: FinanceReportQueryForm): Promise<CashFlowStatementData> {
    const res = await get<CashFlowStatementData | null>(
      '/v1/finance/reports/cash-flow',
      mapReportParams(params)
    )
    return (res ?? {}) as CashFlowStatementData
  },

  /**
   * 获取收支明细汇总
   * 后端 GET /v1/finance/reports/income-expense-summary?startDate&endDate
   *
   * @param params - 报表查询参数
   * @returns 收支汇总数据（金额单位为分）
   */
  async getIncomeExpenseSummary(params: FinanceReportQueryForm): Promise<Record<string, unknown>> {
    const res = await get<Record<string, unknown> | null>(
      '/v1/finance/reports/income-expense-summary',
      mapReportParams(params)
    )
    return res ?? {}
  },

  /**
   * 获取应收账款统计
   * 后端 GET /v1/finance/reports/receivable-statistics
   *
   * @returns 应收账款统计数据（金额单位为分）
   */
  async getReceivableStatistics(): Promise<Record<string, unknown>> {
    const res = await get<Record<string, unknown> | null>('/v1/finance/reports/receivable-statistics')
    return res ?? {}
  },

  /**
   * 获取应付账款统计
   * 后端 GET /v1/finance/reports/payable-statistics
   *
   * @returns 应付账款统计数据（金额单位为分）
   */
  async getPayableStatistics(): Promise<Record<string, unknown>> {
    const res = await get<Record<string, unknown> | null>('/v1/finance/reports/payable-statistics')
    return res ?? {}
  },

  /**
   * 获取账龄分析
   * 后端 GET /v1/finance/reports/aging-analysis
   *
   * @returns 账龄分析数据列表（各账龄段的应收账款分布）
   */
  async getAgingAnalysis(): Promise<Record<string, unknown>[]> {
    const res = await get<Record<string, unknown>[] | null>('/v1/finance/reports/aging-analysis')
    return res ?? []
  },

  /**
   * 获取成本结构分析
   * 后端 GET /v1/finance/reports/cost-structure?period
   *
   * @param period - 期间（YYYY-MM）
   * @returns 成本结构数据（金额单位为分）
   */
  async getCostStructure(period: string): Promise<Record<string, unknown>> {
    const res = await get<Record<string, unknown> | null>(
      '/v1/finance/reports/cost-structure',
      { period }
    )
    return res ?? {}
  },

  /**
   * 获取预算执行情况
   * 后端 GET /v1/finance/reports/budget-execution?year&type
   *
   * @param year - 年度
   * @param type - 类型（可选）
   * @returns 预算执行数据（金额单位为分）
   */
  async getBudgetExecution(year: number, type?: number): Promise<Record<string, unknown>> {
    const params: Record<string, unknown> = { year }
    if (type !== undefined) params.type = type
    const res = await get<Record<string, unknown> | null>(
      '/v1/finance/reports/budget-execution',
      params
    )
    return res ?? {}
  },
}

export default reportApi
