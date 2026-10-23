/**
 * useSummary - 合计行汇总逻辑（core 层，P1-UI-CORE-005）
 * ============================================================
 *
 * 【定位】
 * DataTable show-summary / summary-method 的金额合计与格式化统一入口。
 * 金额口径**强制复用** `@/utils/money` 既有 Converter
 * （fenToYuanNumber / fenToYuanDisplay / yuanToFen），**禁止页面各自实现
 * 千分位格式化**（§9.2 批次红线 ④，来源 `production-remediation-task-board.md` §9.2）。
 *
 * 【使用方式】
 * ```ts
 * import { createAmountSummaryMethod } from '@/composables/useSummary'
 *
 * // 行内金额为「元」（已被 DataConverter 转换）时：
 * const summaryMethod = createAmountSummaryMethod({ amountProps: ['income', 'expense'], unit: 'yuan' })
 * // 行内金额为「分」（后端原始）时：
 * const summaryMethod = createAmountSummaryMethod({ amountProps: ['debitTotal'], unit: 'fen' })
 *
 * <DataTable :show-summary="true" :summary-method="summaryMethod" ... />
 * ```
 *
 * 【契约】
 * - createAmountSummaryMethod 返回 Element Plus summary-method 函数
 *   （({ columns, data }) => (string | number)[]）：首列显示 label（默认「合计」），
 *   amountProps 列显示千分位合计，其余列空白。
 * - 不传 summary-method 给 DataTable 时使用 core 默认方法（首列「合计」、其余空白，
 *   不做无声明数值求和），金额合计必须显式使用本 composable。
 */

import { fenToYuanDisplay, fenToYuanNumber, yuanToFen } from '@/utils/money'

/**
 * 金额合计：按行累加指定字段并格式化为千分位字符串（2 位小数）。
 * 口径与既有 Converter 同源：
 * - unit='fen'：分 → 元（fenToYuanNumber / fenToYuanDisplay）
 * - unit='yuan'：元 → 分 → 元（yuanToFen 修正浮点后走同一格式化路径）
 *
 * @param data 表格行数据
 * @param prop 金额字段名
 * @param unit 行内金额单位：'fen'（后端原始分）| 'yuan'（已被 DataConverter 转为元），默认 'yuan'
 * @returns 形如 "1,234.56" 的字符串；无有效数值时 "0.00"
 */
export function summarizeAmount(
  data: any[],
  prop: string,
  unit: 'fen' | 'yuan' = 'yuan',
): string {
  let total = 0
  for (const row of data) {
    const value = row?.[prop]
    if (value == null || value === '' || Number.isNaN(Number(value))) continue
    total += Number(value)
  }
  if (unit === 'fen') {
    return fenToYuanDisplay(total)
  }
  return fenToYuanDisplay(yuanToFen(total))
}

/** 合计行配置（createAmountSummaryMethod 入参） */
export interface AmountSummaryConfig {
  /** 需要合计的金额列 prop 列表 */
  amountProps: string[]
  /** 金额列单位：'fen'（分，后端原始）| 'yuan'（元，已转换），默认 'yuan' */
  unit?: 'fen' | 'yuan'
  /** 首列表头文案，默认「合计」 */
  label?: string
}

/**
 * 生成 Element Plus summary-method 函数：
 * 首列显示 label（默认「合计」），amountProps 列显示千分位合计（summarizeAmount），其余列空白。
 * 页面仅声明「哪些列合计 + 单位」，格式化实现全部在本 composable（core 层）。
 */
export function createAmountSummaryMethod(config: AmountSummaryConfig) {
  const { amountProps, unit = 'yuan', label = '合计' } = config
  return ({ columns, data }: { columns: any[]; data: any[] }): (string | number)[] => {
    return columns.map((col, index) => {
      const prop = col.property as string | undefined
      if (index === 0) return label
      if (prop && amountProps.includes(prop)) return summarizeAmount(data, prop, unit)
      return ''
    })
  }
}