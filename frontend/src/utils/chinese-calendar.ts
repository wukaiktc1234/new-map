/**
 * 中国农历（阴阳历）计算工具
 *
 * 基于 1900-2100 年天文数据表，提供公历 ↔ 农历互转、
 * 中国法定节假日动态计算等功能
 *
 * 数据来源: 香港天文台农历数据 (1900-2100)
 * 编码格式: 每一年用 2 字节（16位）表示：
 *   bit 15-4: 每月天数 (1=30天 大月, 0=29天 小月)，从正月到腊月共12位，
 *             如有闰月则在闰月位置多1位共13位
 *   bit 3-0:  闰月月份 (0=无闰月, 1-12=闰几月)
 */

/** 农历年份信息编码表 (1900-2100) */
const LUNAR_INFO: number[] = [
  0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
  0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
  0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
  0x06566, 0x0d4a0, 0x0ea50, 0x16a95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
  0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
  0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
  0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
  0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
  0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
  0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
  0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
  0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
  0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
  0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
  0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
  0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
  0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
  0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
  0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
  0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252,
  0x0d520, // 2100
]

/** 天干 */
const HEAVENLY_STEMS = ['甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸']
/** 地支 */
const EARTHLY_BRANCHES = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥']
/** 生肖 */
const ZODIAC = ['鼠', '牛', '虎', '兔', '龙', '蛇', '马', '羊', '猴', '鸡', '狗', '猪']
/** 农历月份名 */
const LUNAR_MONTH_NAMES = ['正', '二', '三', '四', '五', '六', '七', '八', '九', '十', '冬', '腊']
/** 农历日期名 */
const LUNAR_DAY_NAMES = [
  '', '初一', '初二', '初三', '初四', '初五', '初六', '初七', '初八', '初九', '初十',
  '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十',
  '廿一', '廿二', '廿三', '廿四', '廿五', '廿六', '廿七', '廿八', '廿九', '三十',
]

/** 公历转农历结果 */
export interface LunarDate {
  year: number
  month: number
  day: number
  isLeapMonth: boolean
  yearName: string
  monthName: string
  dayName: string
  zodiac: string
}

/** 节假日信息 */
export interface HolidayInfo {
  date: string
  name: string
  type: 'public' | 'traditional' | 'rest'
}

/** 1900年1月31日为农历庚子年正月初一（基准点） */
const BASE_YEAR = 1900
const BASE_LUNAR_YEAR = 1900
/** 1900年正月初一对应的公历日期偏移（从1900-01-01起的天数，1900-01-31是第30天即0-index为30） */
const BASE_DATE_OFFSET = 30

/** 农历年份信息缓存 */
interface LunarYearInfo {
  year: number
  /** 每月天数数组（含闰月），从正月开始 */
  monthDays: number[]
  /** 闰月月份 (0=无闰月) */
  leapMonth: number
  /** 该年总天数 */
  totalDays: number
  /** 该年正月初一对应的公历日期偏移（从BASE_YEAR-01-01起的天数） */
  firstDayOffset: number
}

const lunarYearCache = new Map<number, LunarYearInfo>()

function getLunarYearInfo(lunarYear: number): LunarYearInfo {
  const cached = lunarYearCache.get(lunarYear)
  if (cached) return cached

  const yearIndex = lunarYear - BASE_LUNAR_YEAR
  if (yearIndex < 0 || yearIndex >= LUNAR_INFO.length) {
    throw new Error(`农历年份 ${lunarYear} 超出支持范围 (${BASE_LUNAR_YEAR}-${BASE_LUNAR_YEAR + LUNAR_INFO.length - 1})`)
  }

  const info = LUNAR_INFO[yearIndex]
  const leapMonth = info & 0xf
  const monthDays: number[] = []

  // 解码每月天数
  let bits = info >> 4
  for (let i = 0; i < 12; i++) {
    monthDays.push(bits & 1 ? 30 : 29)
    bits >>= 1
  }
  // 如果有闰月，插入闰月天数
  if (leapMonth > 0) {
    monthDays.splice(leapMonth, 0, bits & 1 ? 30 : 29)
  }

  const totalDays = monthDays.reduce((s, d) => s + d, 0)

  const result: LunarYearInfo = {
    year: lunarYear,
    monthDays,
    leapMonth,
    totalDays,
    firstDayOffset: 0, // 稍后计算
  }

  // 计算正月初一的公历日期偏移
  if (lunarYear === BASE_LUNAR_YEAR) {
    result.firstDayOffset = BASE_DATE_OFFSET
  } else {
    const prevInfo = getLunarYearInfo(lunarYear - 1)
    result.firstDayOffset = prevInfo.firstDayOffset + prevInfo.totalDays
  }

  lunarYearCache.set(lunarYear, result)
  return result
}

/**
 * 获取指定公历年份中，各农历年的正月初一公历偏移
 * 返回 { lunarYear, offset } 数组
 */
function getLunarNewYearsInSolarYear(solarYear: number): Array<{ lunarYear: number; offset: number; month: number; day: number }> {
  const yearStartOffset = daysFromBase(solarYear, 1, 1)
  const yearEndOffset = daysFromBase(solarYear, 12, 31)

  const results: Array<{ lunarYear: number; offset: number; month: number; day: number }> = []

  // 农历年可能在公历年的前一年开始
  for (let ly = solarYear - 1; ly <= solarYear + 1; ly++) {
    try {
      const info = getLunarYearInfo(ly)
      if (info.firstDayOffset >= yearStartOffset && info.firstDayOffset <= yearEndOffset) {
        const date = offsetToDate(info.firstDayOffset)
        results.push({
          lunarYear: ly,
          offset: info.firstDayOffset,
          month: date.month,
          day: date.day,
        })
      }
    } catch {
      // 超出范围
    }
  }

  return results
}

// ==================== 节假日动态更新机制 ====================

/** 年度节假日安排（政府发布） */
export interface YearlyHolidaySchedule {
  year: number
  /** 该年度的节假日覆盖规则 */
  overrides: HolidayOverride[]
  /** 数据来源 */
  source: 'builtin' | 'government' | 'manual' | 'api'
  /** 最后更新时间 */
  updatedAt: string
  /** 版本号（用于冲突检测） */
  version: string
}

/** 单日节假日覆盖规则 */
export interface HolidayOverride {
  date: string
  name: string
  type: 'public' | 'traditional' | 'rest' | 'workday' | 'none'
  enabled: boolean
}

const OVERRIDE_STORAGE_KEY = 'fts_holiday_overrides'

class HolidayOverrideManager {
  private cache: Map<string, HolidayOverride> = new Map()
  private initialized = false

  init(): void {
    if (this.initialized) return
    this.loadFromStorage()
    this.initialized = true
  }

  private loadFromStorage(): void {
    try {
      const raw = localStorage.getItem(OVERRIDE_STORAGE_KEY)
      if (raw) {
        const data: YearlyHolidaySchedule[] = JSON.parse(raw)
        data.forEach((schedule) => {
          schedule.overrides.forEach((override) => {
            if (override.enabled) {
              this.cache.set(override.date, override)
            }
          })
        })
      }
    } catch {
      localStorage.removeItem(OVERRIDE_STORAGE_KEY)
    }
  }

  getOverride(date: string): HolidayOverride | null {
    if (!this.initialized) this.init()
    return this.cache.get(date) || null
  }

  setOverrides(schedule: YearlyHolidaySchedule): void {
    schedule.overrides.forEach((o) => {
      if (o.enabled) {
        this.cache.set(o.date, o)
      } else {
        this.cache.delete(o.date)
      }
    })
    this.persistToStorage()
  }

  removeOverride(date: string): void {
    this.cache.delete(date)
    this.persistToStorage()
  }

  clearAll(): void {
    this.cache.clear()
    localStorage.removeItem(OVERRIDE_STORAGE_KEY)
  }

  getAllSchedules(): YearlyHolidaySchedule[] {
    const grouped = new Map<number, HolidayOverride[]>()
    this.cache.forEach((override) => {
      const year = parseInt(override.date.substring(0, 4))
      if (!grouped.has(year)) grouped.set(year, [])
      grouped.get(year)!.push(override)
    })
    return Array.from(grouped.entries()).map(([year, overrides]) => ({
      year,
      overrides: overrides.sort((a, b) => a.date.localeCompare(b.date)),
      source: 'manual' as const,
      updatedAt: new Date().toISOString(),
      version: `v${Date.now()}`,
    }))
  }

  private persistToStorage(): void {
    const schedules = this.getAllSchedules()
    try {
      localStorage.setItem(OVERRIDE_STORAGE_KEY, JSON.stringify(schedules))
    } catch {
      console.warn('[Holiday] 存储节假日覆盖数据失败')
    }
  }
}

export const holidayOverrideManager = new HolidayOverrideManager()

/**
 * 获取节假日信息（含覆盖机制）
 *
 * 优先级：手动/API覆盖 > 内置计算
 * - type='workday' 表示该日原为假日但需补班（调休工作日）
 * - type='none'   表示该日原为假日但已取消休假
 */
export function getHolidayInfoWithOverride(year: number, month: number, day: number): HolidayInfo | null {
  const dateKey = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const override = holidayOverrideManager.getOverride(dateKey)

  if (override) {
    if (override.type === 'workday' || override.type === 'none') return null
    return { date: `${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`, name: override.name, type: override.type as HolidayInfo['type'] }
  }

  return getHolidayInfo(year, month, day)
}

/**
 * 导入政府发布的年度节假日安排
 *
 * @param schedule 政府发布的年度安排数据
 * @param mergeMode 是否与已有数据合并（false=完全替换该年）
 * @example
 * import { importGovernmentSchedule } from '@/utils/chinese-calendar'
 * // 2026年国务院办公厅发布的节假日安排
 * importGovernmentSchedule({
 *   year: 2026,
 *   version: '2026-gov-001',
 *   source: 'government',
 *   updatedAt: '2025-11-25',
 *   overrides: [
 *     { date: '2026-01-01', name: '元旦', type: 'public', enabled: true },
 *     { date: '2026-01-02', name: '元旦休', type: 'rest', enabled: true },
 *     { date: '2026-02-17', name: '春节', type: 'public', enabled: true },
 *     { date: '2026-04-06', name: '清明节', type: 'public', enabled: true },
 *     { date: '2026-05-01', name: '劳动节', type: 'public', enabled: true },
 *     { date: '2026-05-04', name: '劳动节休', type: 'rest', enabled: true },
 *     { date: '2026-05-09', name: '五一调休补班', type: 'workday', enabled: true },
 *     { date: '2026-10-01', name: '国庆节', type: 'public', enabled: true },
 *     { date: '2026-10-07', name: '国庆休', type: 'rest', enabled: true },
 *     { date: '2026-10-10', name: '国庆调休补班', type: 'workday', enabled: true },
 *   ]
 * })
 */
export function importGovernmentSchedule(schedule: YearlyHolidaySchedule): void {
  holidayOverrideManager.setOverrides(schedule)
}

/**
 * 生成示例：2026年国务院节假日安排（国办发明电〔2025〕7号）
 * 实际使用时应从国务院办公厅公告获取准确数据
 */
export function generateSample2026Schedule(): YearlyHolidaySchedule {
  return {
    year: 2026,
    version: '2026-gov-001',
    source: 'government' as const,
    updatedAt: '2025-11-04',
    overrides: [
      { date: '2026-01-01', name: '元旦', type: 'public', enabled: true },
      { date: '2026-01-02', name: '元旦休', type: 'rest', enabled: true },
      { date: '2026-01-03', name: '元旦休', type: 'rest', enabled: true },
      { date: '2026-01-04', name: '元旦补班', type: 'workday', enabled: true },
      { date: '2026-02-15', name: '春节(除夕)', type: 'traditional', enabled: true },
      { date: '2026-02-16', name: '春节', type: 'public', enabled: true },
      { date: '2026-02-17', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-18', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-19', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-20', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-21', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-22', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-23', name: '春节休', type: 'rest', enabled: true },
      { date: '2026-02-14', name: '春节补班', type: 'workday', enabled: true },
      { date: '2026-02-28', name: '春节补班', type: 'workday', enabled: true },
      { date: '2026-04-04', name: '清明节', type: 'traditional', enabled: true },
      { date: '2026-04-05', name: '清明休', type: 'rest', enabled: true },
      { date: '2026-04-06', name: '清明休', type: 'rest', enabled: true },
      { date: '2026-05-01', name: '劳动节', type: 'public', enabled: true },
      { date: '2026-05-02', name: '劳动节休', type: 'rest', enabled: true },
      { date: '2026-05-03', name: '劳动节休', type: 'rest', enabled: true },
      { date: '2026-05-04', name: '劳动节休', type: 'rest', enabled: true },
      { date: '2026-05-05', name: '劳动节休', type: 'rest', enabled: true },
      { date: '2026-05-09', name: '五一补班', type: 'workday', enabled: true },
      { date: '2026-06-19', name: '端午节', type: 'traditional', enabled: true },
      { date: '2026-06-20', name: '端午休', type: 'rest', enabled: true },
      { date: '2026-06-21', name: '端午休', type: 'rest', enabled: true },
      { date: '2026-09-25', name: '中秋节', type: 'traditional', enabled: true },
      { date: '2026-09-26', name: '中秋休', type: 'rest', enabled: true },
      { date: '2026-09-27', name: '中秋休', type: 'rest', enabled: true },
      { date: '2026-10-01', name: '国庆节', type: 'public', enabled: true },
      { date: '2026-10-02', name: '国庆休', type: 'rest', enabled: true },
      { date: '2026-10-03', name: '国庆休', type: 'rest', enabled: true },
      { date: '2026-10-04', name: '国庆休', type: 'rest', enabled: true },
      { date: '2026-10-05', name: '国庆休', type: 'rest', enabled: true },
      { date: '2026-10-06', name: '国庆休', type: 'rest', enabled: true },
      { date: '2026-10-07', name: '国庆休', type: 'rest', enabled: true },
      { date: '2026-09-20', name: '国庆补班', type: 'workday', enabled: true },
      { date: '2026-10-10', name: '国庆补班', type: 'workday', enabled: true },
    ],
  }
}

/** 计算从1900-01-01起的天数 */
function daysFromBase(year: number, month: number, day: number): number {
  let days = 0
  for (let y = BASE_YEAR; y < year; y++) {
    days += isLeapYear(y) ? 366 : 365
  }
  for (let m = 1; m < month; m++) {
    days += daysInMonth(year, m)
  }
  days += day - 1
  return days
}

function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0
}

function daysInMonth(year: number, month: number): number {
  const DAYS = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  if (month === 2 && isLeapYear(year)) return 29
  return DAYS[month - 1]
}

function offsetToDate(offset: number): { year: number; month: number; day: number } {
  let remaining = offset
  let year = BASE_YEAR
  while (true) {
    const yearDays = isLeapYear(year) ? 366 : 365
    if (remaining < yearDays) break
    remaining -= yearDays
    year++
  }
  let month = 1
  while (true) {
    const mDays = daysInMonth(year, month)
    if (remaining < mDays) break
    remaining -= mDays
    month++
  }
  return { year, month, day: remaining + 1 }
}

/**
 * 公历日期转农历日期
 * @param year - 公历年
 * @param month - 公历月 (1-12)
 * @param day - 公历日 (1-31)
 * @returns 农历日期对象，超出支持范围返回 null
 */
export function solarToLunar(year: number, month: number, day: number): LunarDate | null {
  try {
    const offset = daysFromBase(year, month, day)

    // 查找该公历日期属于哪个农历年
    let lunarYear: number | null = null
    let dayInLunarYear = 0

    for (let ly = year - 1; ly <= year + 1; ly++) {
      try {
        const info = getLunarYearInfo(ly)
        if (offset >= info.firstDayOffset && offset < info.firstDayOffset + info.totalDays) {
          lunarYear = ly
          dayInLunarYear = offset - info.firstDayOffset
          break
        }
      } catch {
        continue
      }
    }

    if (lunarYear === null) return null

    const info = getLunarYearInfo(lunarYear)
    let remaining = dayInLunarYear
    let lunarMonth = 0
    let isLeap = false

    for (let i = 0; i < info.monthDays.length; i++) {
      if (remaining < info.monthDays[i]) {
        lunarMonth = i + 1
        break
      }
      remaining -= info.monthDays[i]
    }

    if (lunarMonth === 0) return null

    // 判断是否闰月
    let displayMonth = lunarMonth
    if (info.leapMonth > 0 && lunarMonth > info.leapMonth) {
      displayMonth = lunarMonth - 1
    }
    isLeap = info.leapMonth > 0 && lunarMonth === info.leapMonth + 1
    if (isLeap) {
      displayMonth = info.leapMonth
    }

    const yearIndex = (lunarYear - 4) % 10
    const branchIndex = (lunarYear - 4) % 12
    const yearName = `${HEAVENLY_STEMS[yearIndex >= 0 ? yearIndex : yearIndex + 10]}${EARTHLY_BRANCHES[branchIndex >= 0 ? branchIndex : branchIndex + 12]}年`

    return {
      year: lunarYear,
      month: displayMonth,
      day: remaining + 1,
      isLeapMonth: isLeap,
      yearName,
      monthName: (isLeap ? '闰' : '') + LUNAR_MONTH_NAMES[displayMonth - 1] + '月',
      dayName: LUNAR_DAY_NAMES[remaining + 1],
      zodiac: ZODIAC[branchIndex >= 0 ? branchIndex : branchIndex + 12],
    }
  } catch {
    return null
  }
}

/** 农历数据支持的年份范围 */
const LUNAR_YEAR_MIN = 1900
const LUNAR_YEAR_MAX = 2100

/**
 * 获取农历计算支持的年份范围
 * 固定节假日（元旦/清明/劳动节/国庆节）不受此范围限制
 */
export function getLunarYearRange(): { min: number; max: number } {
  return { min: LUNAR_YEAR_MIN, max: LUNAR_YEAR_MAX }
}

// ==================== 政府公布的节假日日期表 ====================
// 优先级：政府公布日期 > 农历动态计算
// 数据来源：国务院办公厅关于XX年部分节假日安排的通知
// 每年更新后应同步更新此表

interface GovHoliday {
  name: string
  type: 'public' | 'traditional' | 'rest' | 'workday'
}

/**
 * 政府公布的精确节假日日期
 * key格式: YYYY-MM-DD，value为节假日信息
 * 包含：法定假日、调休假日、调休工作日
 */
const GOVERNMENT_HOLIDAY_DATES: Record<string, GovHoliday> = {
  // ==================== 2025年 ====================
  '2025-01-01': { name: '元旦', type: 'public' },

  '2025-01-28': { name: '春节(除夕)', type: 'traditional' },
  '2025-01-29': { name: '春节', type: 'public' },
  '2025-01-30': { name: '春节休', type: 'rest' },
  '2025-01-31': { name: '春节休', type: 'rest' },
  '2025-02-01': { name: '春节休', type: 'rest' },
  '2025-02-02': { name: '春节休', type: 'rest' },
  '2025-02-03': { name: '春节休', type: 'rest' },
  '2025-02-04': { name: '春节休', type: 'rest' },
  '2025-01-26': { name: '春节补班', type: 'workday' },
  '2025-02-08': { name: '春节补班', type: 'workday' },

  '2025-04-04': { name: '清明节', type: 'traditional' },
  '2025-04-05': { name: '清明休', type: 'rest' },
  '2025-04-06': { name: '清明休', type: 'rest' },

  '2025-05-01': { name: '劳动节', type: 'public' },
  '2025-05-02': { name: '劳动节休', type: 'rest' },
  '2025-05-03': { name: '劳动节休', type: 'rest' },
  '2025-05-04': { name: '劳动节休', type: 'rest' },
  '2025-05-05': { name: '劳动节休', type: 'rest' },
  '2025-04-27': { name: '五一补班', type: 'workday' },

  '2025-05-31': { name: '端午节', type: 'traditional' },
  '2025-06-01': { name: '端午休', type: 'rest' },
  '2025-06-02': { name: '端午休', type: 'rest' },

  '2025-10-01': { name: '国庆节', type: 'public' },
  '2025-10-02': { name: '国庆休', type: 'rest' },
  '2025-10-03': { name: '国庆休', type: 'rest' },
  '2025-10-04': { name: '中秋', type: 'traditional' },
  '2025-10-05': { name: '国庆休', type: 'rest' },
  '2025-10-06': { name: '国庆休', type: 'rest' },
  '2025-10-07': { name: '国庆休', type: 'rest' },
  '2025-09-28': { name: '国庆补班', type: 'workday' },
  '2025-10-11': { name: '国庆补班', type: 'workday' },

  // ==================== 2026年（国办发明电〔2025〕7号） ====================
  '2026-01-01': { name: '元旦', type: 'public' },
  '2026-01-02': { name: '元旦休', type: 'rest' },
  '2026-01-03': { name: '元旦休', type: 'rest' },
  '2026-01-04': { name: '元旦补班', type: 'workday' },

  '2026-02-15': { name: '春节(除夕)', type: 'traditional' },
  '2026-02-16': { name: '春节', type: 'public' },
  '2026-02-17': { name: '春节休', type: 'rest' },
  '2026-02-18': { name: '春节休', type: 'rest' },
  '2026-02-19': { name: '春节休', type: 'rest' },
  '2026-02-20': { name: '春节休', type: 'rest' },
  '2026-02-21': { name: '春节休', type: 'rest' },
  '2026-02-22': { name: '春节休', type: 'rest' },
  '2026-02-23': { name: '春节休', type: 'rest' },
  '2026-02-14': { name: '春节补班', type: 'workday' },
  '2026-02-28': { name: '春节补班', type: 'workday' },

  '2026-04-04': { name: '清明节', type: 'traditional' },
  '2026-04-05': { name: '清明休', type: 'rest' },
  '2026-04-06': { name: '清明休', type: 'rest' },

  '2026-05-01': { name: '劳动节', type: 'public' },
  '2026-05-02': { name: '劳动节休', type: 'rest' },
  '2026-05-03': { name: '劳动节休', type: 'rest' },
  '2026-05-04': { name: '劳动节休', type: 'rest' },
  '2026-05-05': { name: '劳动节休', type: 'rest' },
  '2026-05-09': { name: '五一补班', type: 'workday' },

  '2026-06-19': { name: '端午节', type: 'traditional' },
  '2026-06-20': { name: '端午休', type: 'rest' },
  '2026-06-21': { name: '端午休', type: 'rest' },

  '2026-09-25': { name: '中秋节', type: 'traditional' },
  '2026-09-26': { name: '中秋休', type: 'rest' },
  '2026-09-27': { name: '中秋休', type: 'rest' },

  '2026-10-01': { name: '国庆节', type: 'public' },
  '2026-10-02': { name: '国庆休', type: 'rest' },
  '2026-10-03': { name: '国庆休', type: 'rest' },
  '2026-10-04': { name: '国庆休', type: 'rest' },
  '2026-10-05': { name: '国庆休', type: 'rest' },
  '2026-10-06': { name: '国庆休', type: 'rest' },
  '2026-10-07': { name: '国庆休', type: 'rest' },
  '2026-09-20': { name: '国庆补班', type: 'workday' },
  '2026-10-10': { name: '国庆补班', type: 'workday' },

  // ==================== 2027年（待政府公布，暂按农历推算） ====================
  '2027-01-01': { name: '元旦', type: 'public' },
}

/**
 * 获取指定公历日期的中国节假日信息
 *
 * 优先级：政府公布日期表 > 农历动态计算
 * - 政府公布日期表（GOVERNMENT_HOLIDAY_DATES）包含国务院办公厅正式发布的
 *   法定假日、调休假日和调休工作日，是权威数据源
 * - 农历动态计算作为兜底，覆盖政府表未收录的年份
 *
 * @param year - 公历年
 * @param month - 公历月 (1-12)
 * @param day - 公历日 (1-31)
 * @returns 节假日信息，非节假日返回 null
 */
export function getHolidayInfo(year: number, month: number, day: number): HolidayInfo | null {
  const dateKey = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  const dateShortKey = `${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`

  // 优先级1：政府公布的精确日期表
  const govHoliday = GOVERNMENT_HOLIDAY_DATES[dateKey]
  if (govHoliday) {
    // 调休工作日不是假日
    if (govHoliday.type === 'workday') return null
    return { date: dateShortKey, name: govHoliday.name, type: govHoliday.type as HolidayInfo['type'] }
  }

  // 优先级2：农历动态计算（兜底，覆盖政府表未收录的年份）
  const lunar = getLunarForYear(year, month, day)
  if (!lunar) return null

  if (lunar.month === 1 && lunar.day === 1 && !lunar.isLeapMonth) {
    return { date: dateShortKey, name: '春节', type: 'public' }
  }
  if (lunar.month === 12 && lunar.day === 30) {
    return { date: dateShortKey, name: '除夕', type: 'traditional' }
  }
  if (lunar.month === 12 && lunar.day === 29) {
    const nextDayOffset = daysFromBase(year, month, day) + 1
    const nextLunar = solarToLunar(...dateFromOffset(nextDayOffset))
    if (nextLunar && nextLunar.month === 1 && nextLunar.day === 1) {
      return { date: dateShortKey, name: '除夕', type: 'traditional' }
    }
  }
  if (lunar.month === 5 && lunar.day === 5 && !lunar.isLeapMonth) {
    return { date: dateShortKey, name: '端午节', type: 'traditional' }
  }
  if (lunar.month === 8 && lunar.day === 15 && !lunar.isLeapMonth) {
    return { date: dateShortKey, name: '中秋节', type: 'traditional' }
  }

  return null
}

/** 获取农历信息（带年份范围检查，仅内部使用避免递归） */
function getLunarForYear(year: number, month: number, day: number): LunarDate | null {
  if (year < LUNAR_YEAR_MIN || year > LUNAR_YEAR_MAX) return null
  return solarToLunar(year, month, day)
}

function dateFromOffset(offset: number): [number, number, number] {
  const d = offsetToDate(offset)
  return [d.year, d.month, d.day]
}

/**
 * 获取两个公历日期之间的所有节假日
 */
export function getHolidaysInRange(startYear: number, startMonth: number, startDay: number, endYear: number, endMonth: number, endDay: number): Array<HolidayInfo & { fullDate: string }> {
  const results: Array<HolidayInfo & { fullDate: string }> = []
  const startOffset = daysFromBase(startYear, startMonth, startDay)
  const endOffset = daysFromBase(endYear, endMonth, endDay)

  for (let offset = startOffset; offset <= endOffset; offset++) {
    const [y, m, d] = dateFromOffset(offset)
    const holiday = getHolidayInfo(y, m, d)
    if (holiday) {
      results.push({
        ...holiday,
        fullDate: `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`,
      })
    }
  }

  return results
}