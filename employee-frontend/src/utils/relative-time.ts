/**
 * 相对时间格式化工具
 *
 * 将 ISO 时间戳转换为人类友好的相对时间格式：
 * - "刚刚" (0-59秒)
 * - "3分钟前" / "5分钟后"
 * - "2小时前" / "1小时后"
 * - "昨天 14:30" / "明天 09:00"
 * - "3天前" / "7天后"
 * - 超过7天显示具体日期 "2024-01-15 14:30"
 *
 * @param isoString - ISO 8601 格式的时间字符串
 * @returns 相对时间字符串
 *
 * @example
 * formatRelativeTime('2024-01-15T14:30:00') // 假设现在是 14:31 → "刚刚"
 * formatRelativeTime('2024-01-15T12:00:00') // 假设现在是 14:30 → "2小时前"
 */

const SECONDS = 1000
const MINUTES = 60 * SECONDS
const HOURS = 60 * MINUTES
const DAYS = 24 * HOURS

/** 中文数字单位映射 */
const UNITS = {
  second: '秒',
  minute: '分钟',
  hour: '小时',
  day: '天',
} as const

export function formatRelativeTime(isoString: string): string {
  if (!isoString) return ''

  const now = Date.now()
  const target = new Date(isoString).getTime()
  
  // 无效日期
  if (isNaN(target)) return isoString
  
  const diff = target - now
  const absDiff = Math.abs(diff)
  const isPast = diff < 0
  const suffix = isPast ? '前' : '后'

  // 刚刚（60秒内）
  if (absDiff < MINUTES) {
    return absDiff < 10 * SECONDS ? '刚刚' : `${Math.floor(absDiff / SECONDS)}${UNITS.second}${suffix}`
  }

  // 分钟级别
  if (absDiff < HOURS) {
    return `${Math.floor(absDiff / MINUTES)}${UNITS.minute}${suffix}`
  }

  // 小时级别
  if (absDiff < DAYS) {
    return `${Math.floor(absDiff / HOURS)}${UNITS.hour}${suffix}`
  }

  // 昨天/明天（1天内但超过24小时，处理跨日情况）
  if (absDiff < 2 * DAYS) {
    const targetDate = new Date(target)
    const nowDate = new Date(now)
    
    // 检查是否是昨天或明天的同一天
    const targetDayStart = new Date(targetDate.getFullYear(), targetDate.getMonth(), targetDate.getDate()).getTime()
    const nowDayStart = new Date(nowDate.getFullYear(), nowDate.getMonth(), nowDate.getDate()).getTime()
    const dayDiff = Math.round((targetDayStart - nowDayStart) / DAYS)
    
    if (dayDiff === -1) {
      return `昨天 ${formatTime(targetDate)}`
    }
    if (dayDiff === 1) {
      return `明天 ${formatTime(targetDate)}`
    }
  }

  // 天数级别（2-7天）
  if (absDiff <= 7 * DAYS) {
    return `${Math.floor(absDiff / DAYS)}${UNITS.day}${suffix}`
  }

  // 超过7天：显示具体日期
  return formatDate(new Date(target))
}

/** 格式化 HH:mm */
function formatTime(date: Date): string {
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

/** 格式化 YYYY-MM-DD HH:mm */
function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d} ${formatTime(date)}`
}
