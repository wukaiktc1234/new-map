/**
 * POS 端时间格式化统一入口
 *
 * 背景：后端（Jackson，server.servlet.context-path=/api）返回的 LocalDateTime
 * 往往带微秒且用 'T' 分隔，例如：
 *   "2026-10-23T12:46:54.386540"
 * 直接渲染会出现裸 ISO 串；用 toLocaleString('zh-CN') 又会变成 "2026/10/23 12:46:54"。
 * 因此 POS 端所有时间展示统一走本文件，输出固定格式：
 *   formatDateTime        -> YYYY-MM-DD HH:mm:ss
 *   formatDateTimeMinute  -> YYYY-MM-DD HH:mm
 *   formatDate            -> YYYY-MM-DD
 *   formatTime            -> HH:mm
 *   formatMonthDayTime    -> MM-DD HH:mm
 */

/** 可接受的时间入参：后端字符串 / 时间戳 / Date / 空值 */
export type DateInput = string | number | Date | null | undefined

/** 两位补零 */
const pad2 = (value: number): string => (value < 10 ? `0${value}` : String(value))

/**
 * 严格匹配「无时区的本地时间」形式：
 *   2026-10-23T12:46:54.386540 / 2026-10-23 12:46:54 / 2026-10-23T12:46
 * 这类串必须按本地时区解析，并且必须截断微秒（不同浏览器对超长小数位的容错不一致）。
 */
const LOCAL_DATE_TIME_RE =
  /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.(\d{1,9}))?$/

/** 纯日期：2026-10-23（避免被原生解析成 UTC 零点导致跨时区偏移一天） */
const LOCAL_DATE_RE = /^(\d{4})-(\d{2})-(\d{2})$/

/** 解析成 Date；无法解析返回 null */
export function parseDateTime(value: DateInput): Date | null {
  if (value === null || value === undefined) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (typeof value === 'number') {
    const fromNumber = new Date(value)
    return Number.isNaN(fromNumber.getTime()) ? null : fromNumber
  }
  const text = String(value).trim()
  if (!text) return null

  const dateTimeMatch = LOCAL_DATE_TIME_RE.exec(text)
  if (dateTimeMatch) {
    const [, y, mo, d, h, mi, s, frac] = dateTimeMatch
    // 微秒级小数位截断到毫秒，避免部分引擎解析失败或四舍五入
    const ms = frac ? Number(frac.slice(0, 3).padEnd(3, '0')) : 0
    const parsed = new Date(
      Number(y),
      Number(mo) - 1,
      Number(d),
      Number(h),
      Number(mi),
      s ? Number(s) : 0,
      ms,
    )
    return Number.isNaN(parsed.getTime()) ? null : parsed
  }

  const dateMatch = LOCAL_DATE_RE.exec(text)
  if (dateMatch) {
    const [, y, mo, d] = dateMatch
    const parsed = new Date(Number(y), Number(mo) - 1, Number(d))
    return Number.isNaN(parsed.getTime()) ? null : parsed
  }

  // 带时区（Z / +08:00）或其它格式交给原生解析
  const native = new Date(text)
  return Number.isNaN(native.getTime()) ? null : native
}

/** YYYY-MM-DD HH:mm:ss */
export function formatDateTime(value: DateInput, fallback = '-'): string {
  const date = parseDateTime(value)
  if (!date) return fallback
  return (
    `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ` +
    `${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
  )
}

/** YYYY-MM-DD HH:mm */
export function formatDateTimeMinute(value: DateInput, fallback = '-'): string {
  const date = parseDateTime(value)
  if (!date) return fallback
  return `${formatDate(date)} ${formatTime(date)}`
}

/** YYYY-MM-DD */
export function formatDate(value: DateInput, fallback = '-'): string {
  const date = parseDateTime(value)
  if (!date) return fallback
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

/** HH:mm */
export function formatTime(value: DateInput, fallback = '-'): string {
  const date = parseDateTime(value)
  if (!date) return fallback
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

/** MM-DD HH:mm（POS 订单卡片等需要紧凑显示的场合） */
export function formatMonthDayTime(value: DateInput, fallback = '-'): string {
  const date = parseDateTime(value)
  if (!date) return fallback
  return `${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${formatTime(date)}`
}
