/**
 * 金额转换统一工具
 *
 * 后端和数据库以「分」（整数）为单位；
 * 前端以「元」（字符串）为单位。
 *
 * 强制规范：
 *   1. 禁止在组件内直接做 *100 或 /100 金额转换
 *   2. 禁止使用 (fen / 100).toFixed(2) 这种二次转换
 *   3. 所有金额转换必须走本文件或各模块 DataConverter
 *
 * @module utils/money
 */

/**
 * 分 → 元（字符串）
 * 用于显示场景，避免浮点精度问题。
 *
 * @param fen 金额（分），可空
 * @returns 形如 "100.50" 的字符串；fen 为 null/NaN 时返回 "0.00"
 *
 * @example
 * fenToYuan(10050)        // "100.50"
 * fenToYuan(null)         // "0.00"
 * fenToYuan(undefined)    // "0.00"
 * fenToYuan(NaN)          // "0.00"
 */
export function fenToYuan(fen: number | null | undefined): string {
  if (fen == null || Number.isNaN(fen)) return '0.00'
  return (fen / 100).toFixed(2)
}

/**
 * 分 → 元（数字）
 * 用于需要参与计算的场景（如求和、比较、图表）。
 *
 * @param fen 金额（分），可空
 * @returns 元数值；fen 为 null/NaN 时返回 0
 *
 * @example
 * fenToYuanNumber(10050)   // 100.5
 * fenToYuanNumber(null)    // 0
 */
export function fenToYuanNumber(fen: number | null | undefined): number {
  if (fen == null || Number.isNaN(fen)) return 0
  return Number((fen / 100).toFixed(2))
}

/**
 * 元 → 分（整数）
 * 使用 Math.round 修正浮点精度问题（0.1 + 0.2 ≠ 0.3）。
 *
 * @param yuan 金额（元），数字或字符串，可空
 * @returns 分（整数）；yuan 为 null/空串/NaN 时返回 0
 *
 * @example
 * yuanToFen(100.50)        // 10050
 * yuanToFen('100.50')     // 10050
 * yuanToFen(0.1 + 0.2)    // 30（而非 30.000000000000004）
 */
export function yuanToFen(yuan: string | number | null | undefined): number {
  if (yuan == null || yuan === '') return 0
  const num = typeof yuan === 'string' ? parseFloat(yuan) : yuan
  if (Number.isNaN(num)) return 0
  return Math.round(num * 100)
}

/**
 * 分 → 元并格式化为带千分位的货币字符串（带 ¥ 前缀）
 * 用于 StatCard、表格金额列等最终展示场景。
 *
 * @param fen 金额（分），可空
 * @returns 形如 "¥1,234.56" 的字符串
 *
 * @example
 * formatFenToYuan(123456)  // "¥1,234.56"
 * formatFenToYuan(null)    // "¥0.00"
 */
export function formatFenToYuan(fen: number | null | undefined): string {
  const yuan = fenToYuanNumber(fen)
  return `¥${yuan.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

/**
 * 分 → 元字符串（带千分位，不含货币符号）
 * 用于需要纯数字带千分位的展示场景（如表格列、统计卡）。
 *
 * @param fen 金额（分），可空
 * @returns 形如 "1,234.56" 的字符串
 *
 * @example
 * fenToYuanDisplay(123456)  // "1,234.56"
 * fenToYuanDisplay(null)    // "0.00"
 */
export function fenToYuanDisplay(fen: number | null | undefined): string {
  const yuan = fenToYuanNumber(fen)
  return yuan.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

/**
 * 分 → 万元（字符串，保留 2 位小数）
 * 用于大额数据展示（如财务报表、预算汇总）。
 *
 * @param fen 金额（分），可空
 * @returns 形如 "12.34" 的字符串（单位：万元）
 *
 * @example
 * fenToWan(1234567)   // "12.35"
 * fenToWan(null)      // "0.00"
 */
export function fenToWan(fen: number | null | undefined): string {
  if (fen == null || Number.isNaN(fen)) return '0.00'
  return (fen / 1000000).toFixed(2)
}
