/**
 * OCR 日期字段自我纠错
 *
 * 针对喷码/印刷日期的 OCR 常见错误：
 * 1. 字符混淆修正：O→0、I/1→1、B→8、S→5、G→6、Z→2、T→7 等
 * 2. 合法日期校验：月 01-12、日按月份天数、年份合理范围
 * 3. 交叉验证推算：生产日期 + 保质期（天/月）→ 保质期至
 */

/** 混淆字符 → 数字映射（OCR 喷码常见误识） */
const CHAR_TO_DIGIT: Record<string, string> = {
  O: '0', o: '0', D: '0', Q: '0',
  I: '1', l: '1',
  Z: '2',
  S: '5',
  G: '6',
  T: '7',
  B: '8',
  ' ': '',
}

/** 可参与纠错的字符集合（数字 + 常见混淆字母） */
const DIGIT_OR_CONFUSABLE = '0123456789OoDI1lTSGZBz'

function isLeapYear(y: number): boolean {
  return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0
}

function daysInMonth(y: number, m: number): number {
  const table = [31, isLeapYear(y) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  return table[m - 1] ?? 31
}

/** 校验 YYYY-MM-DD 是否合法且年份在合理范围 */
export function isValidDateStr(s: string, minYear = 2000, maxYear = 2035): boolean {
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!m) return false
  const y = Number(m[1])
  const mo = Number(m[2])
  const d = Number(m[3])
  if (y < minYear || y > maxYear) return false
  if (mo < 1 || mo > 12) return false
  if (d < 1 || d > daysInMonth(y, mo)) return false
  return true
}

/** 规范化日期片段（把混淆字符转数字，返回 null 表示无法规范） */
function normalizePart(part: string): string | null {
  let out = ''
  for (const ch of part) {
    if (ch >= '0' && ch <= '9') {
      out += ch
      continue
    }
    if (ch in CHAR_TO_DIGIT) {
      out += CHAR_TO_DIGIT[ch]
      continue
    }
    return null
  }
  return out
}

/** 在关键词后方找日期候选串（含混淆字符），逐字符规范化后校验合法 */
function extractDateNear(text: string, keywords: string[]): string | null {
  const t = text || ''
  const d = DIGIT_OR_CONFUSABLE
  const yearPat = `[${d}]{4}`
  const datePat = `(${yearPat})[-/.年]([${d}]{1,2})[-/.月]([${d}]{1,2})日?`
  for (const kw of keywords) {
    const re = new RegExp(`${kw}[^\\dOoDI1lTSGZBz]{0,12}?${datePat}`)
    const m = t.match(re)
    if (!m) continue
    const y = normalizePart(m[1])
    const mo = normalizePart(m[2])
    const day = normalizePart(m[3])
    if (!y || !mo || !day) continue
    const candidate = `${y}-${mo.padStart(2, '0')}-${day.padStart(2, '0')}`
    if (isValidDateStr(candidate)) return candidate
  }
  return null
}

/** 全文任意位置找合法日期（含混淆修正） */
function extractAnyDate(text: string): string | null {
  const t = text || ''
  const d = DIGIT_OR_CONFUSABLE
  const re = new RegExp(`([${d}]{4})[-/.年]([${d}]{1,2})[-/.月]([${d}]{1,2})日?`, 'g')
  let m: RegExpExecArray | null
  while ((m = re.exec(t)) !== null) {
    const y = normalizePart(m[1])
    const mo = normalizePart(m[2])
    const day = normalizePart(m[3])
    if (!y || !mo || !day) continue
    const candidate = `${y}-${mo.padStart(2, '0')}-${day.padStart(2, '0')}`
    if (isValidDateStr(candidate)) return candidate
  }
  return null
}

/**
 * 从 OCR 文本提取并纠错日期
 * @param text OCR 原始文本
 * @param keywords 关键词（生产日期/保质期至等），优先关键词附近
 * @param opts 年份范围约束
 */
export function extractCorrectedDate(
  text: string,
  keywords: string[],
  opts: { minYear?: number; maxYear?: number } = {},
): string | null {
  const minYear = opts.minYear ?? 2000
  const maxYear = opts.maxYear ?? 2035
  const near = extractDateNear(text, keywords)
  if (near && isValidDateStr(near, minYear, maxYear)) return near
  const any = extractAnyDate(text)
  if (any && isValidDateStr(any, minYear, maxYear)) return any
  return null
}

/** 提取保质期：如「保质期：180天」「保质期6个月」 */
export function extractShelfLife(text: string): { value: number; unit: 'day' | 'month' } | null {
  const t = text || ''
  const m = t.match(/保质期\s*[:：]?\s*(\d{1,4})\s*(天|日|个月|月)/)
  if (m) {
    const value = Number(m[1])
    if (!Number.isNaN(value) && value > 0) {
      return { value, unit: m[2] === '个月' || m[2] === '月' ? 'month' : 'day' }
    }
  }
  const en = t.match(/Shelf\s*Life\s*[:：]?\s*(\d{1,4})\s*(d|D|M|m)/)
  if (en) {
    const value = Number(en[1])
    if (!Number.isNaN(value) && value > 0) {
      return { value, unit: en[2] === 'M' || en[2] === 'm' ? 'month' : 'day' }
    }
  }
  return null
}

/** 日期加减天数/月数，返回 YYYY-MM-DD */
export function addDuration(dateStr: string, value: number, unit: 'day' | 'month'): string {
  const [y, mo, d] = dateStr.split('-').map(Number)
  const date = new Date(y, mo - 1, d)
  if (unit === 'month') {
    date.setMonth(date.getMonth() + value)
  } else {
    date.setDate(date.getDate() + value)
  }
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 两个日期相差天数（dateStr2 - dateStr1） */
export function diffDays(dateStr1: string, dateStr2: string): number {
  const [y1, m1, d1] = dateStr1.split('-').map(Number)
  const [y2, m2, d2] = dateStr2.split('-').map(Number)
  const t1 = new Date(y1, m1 - 1, d1).getTime()
  const t2 = new Date(y2, m2 - 1, d2).getTime()
  return Math.round((t2 - t1) / 86400000)
}
