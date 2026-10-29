/**
 * 开班状态本地持久化（N19-4 增强）
 *
 * 目标：开班成功后把「本终端当天已开班」落到 localStorage，
 * 刷新页面 / 重新进入首页不再重复弹备用金弹窗；
 * 交班成功、退出登录、切换账号时清除标记，保证下一班次仍会正常弹窗。
 *
 * 设计要点：
 *   ① 标记与 terminalId + 业务日期绑定：换终端、隔天自动失效；
 *   ② 读取失败（脏数据 / 隐私模式）一律当作「未开班」，宁可多弹一次，
 *      也不会因为脏标记把收银员卡在「没有备用金记录」的状态；
 *   ③ 只做前端展示层状态，不替后端判定班次是否有效——后端有权威班次时以它为准。
 */

const SHIFT_MARK_KEY = 'pos-shift-active'

export interface ShiftSessionMark {
  /** 终端号（原始值，仅本地比对用，不展示） */
  terminalId: string
  shiftId?: string | number
  employeeId?: string
  employeeName?: string
  /** 开班备用金（元） */
  openingCash?: number | null
  /** 开班时间（YYYY-MM-DD HH:mm:ss） */
  startedAt: string
  /** 业务日期（YYYY-MM-DD），跨天自动失效 */
  businessDate: string
}

function todayKey(date: Date = new Date()): string {
  const pad = (n: number): string => (n < 10 ? `0${n}` : String(n))
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function nowStamp(date: Date = new Date()): string {
  const pad = (n: number): string => (n < 10 ? `0${n}` : String(n))
  return (
    `${todayKey(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/** 读取开班标记；脏数据 / 隔天 / localStorage 异常均返回 null */
export function readShiftMark(): ShiftSessionMark | null {
  try {
    const raw = localStorage.getItem(SHIFT_MARK_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<ShiftSessionMark> | null
    if (!parsed || typeof parsed !== 'object') return null
    const terminalId = typeof parsed.terminalId === 'string' ? parsed.terminalId : ''
    const businessDate = typeof parsed.businessDate === 'string' ? parsed.businessDate : ''
    if (!terminalId || !businessDate) return null
    // 隔天的标记不再有效：新一天必须重新开班
    if (businessDate !== todayKey()) return null
    return {
      terminalId,
      shiftId: parsed.shiftId,
      employeeId: parsed.employeeId,
      employeeName: parsed.employeeName,
      openingCash: typeof parsed.openingCash === 'number' ? parsed.openingCash : null,
      startedAt: typeof parsed.startedAt === 'string' ? parsed.startedAt : nowStamp(),
      businessDate,
    }
  } catch {
    return null
  }
}

/** 写入开班标记（开班成功后调用） */
export function markShiftStarted(mark: Omit<ShiftSessionMark, 'startedAt' | 'businessDate'> & {
  startedAt?: string
  businessDate?: string
}): ShiftSessionMark | null {
  const record: ShiftSessionMark = {
    terminalId: mark.terminalId,
    shiftId: mark.shiftId,
    employeeId: mark.employeeId,
    employeeName: mark.employeeName,
    openingCash: mark.openingCash ?? null,
    startedAt: mark.startedAt || nowStamp(),
    businessDate: mark.businessDate || todayKey(),
  }
  try {
    localStorage.setItem(SHIFT_MARK_KEY, JSON.stringify(record))
    return record
  } catch {
    return null
  }
}

/** 清除开班标记（交班成功 / 退出登录 / 切换账号） */
export function clearShiftMark(): void {
  try {
    localStorage.removeItem(SHIFT_MARK_KEY)
  } catch {
    // ignore
  }
}

/** 本终端当天是否已开班（标记存在且终端号一致） */
export function hasActiveShiftMark(terminalId: string): boolean {
  const mark = readShiftMark()
  if (!mark) return false
  const id = (terminalId || '').trim()
  if (!id) return false
  return mark.terminalId === id
}
