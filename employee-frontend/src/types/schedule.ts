export type ShiftType = 'morning' | 'afternoon' | 'evening' | 'rest'

/** 视图层级：个人视图 / 团队视图 / 班次详情 */
export type ScheduleLayer = 'personal' | 'team' | 'detail'

/** 周范围信息（用于 Header 组件） */
export interface WeekRange {
  /** 周序号（年内第几周） */
  weekNumber: number
  /** 周起始日期 YYYY-MM-DD */
  start: string
  /** 周结束日期 YYYY-MM-DD */
  end: string
}

export const SHIFT_LABEL_MAP: Record<ShiftType, string> = {
  morning: '早班',
  afternoon: '中班',
  evening: '晚班',
  rest: '休息',
}

export const SHIFT_TIME_MAP: Record<ShiftType, string> = {
  morning: '07:00-15:00',
  afternoon: '11:00-19:00',
  evening: '16:00-24:00',
  rest: '--',
}

export interface ScheduleItem {
  id: string
  date: string
  dayOfWeek: string
  shiftType: ShiftType
  shiftLabel: string
  timeRange: string
  status: 'completed' | 'today' | 'upcoming' | 'rest'
  isWorkday: boolean
  note?: string
}

export interface WeekScheduleData {
  weekStart: string
  weekEnd: string
  days: ScheduleItem[]
  stats: {
    totalDays: number
    workDays: number
    restDays: number
    morningCount: number
    afternoonCount: number
    eveningCount: number
  }
}

// ============================================
// 排班视图增强类型（团队视图/班次详情）
// ============================================

/** 单日排班数据 */
export interface DaySchedule {
  date: string
  dayOfWeek: number
  dayOfWeekName: string
  isToday: boolean
  isRest: boolean
  shifts: ShiftItem[]
}

/** 单个班次 */
export interface ShiftItem {
  shiftId: string
  shiftType: ShiftType
  startTime: string
  endTime: string
  area?: string
  note?: string
}

/** 团队视图中的同事条目 */
export interface TeamColleagueItem {
  employeeId: string
  avatar: string
  name: string
  position: string
  isSelf: boolean
  positionGroup: string
  weekDots: WeekDot[]
}

/** 一周中某天的圆点状态 */
export interface WeekDot {
  date: string
  isWorking: boolean
  shiftType?: ShiftType
}

/** 班次详情(Layer 3) - 已扩展为任务派遣场景 */
export interface ShiftDetail {
  shiftId: string
  employeeId: string
  employeeName: string
  avatar: string
  position: string
  date: string
  dayOfWeekName: string
  shiftType: ShiftType
  startTime: string
  endTime: string
  area?: string
  note?: string
  storeName?: string
  // ---- 派遣相关字段（任务场景）----
  /** 派遣类型：借调/学习/临时支援 */
  dispatchType?: 'loan' | 'training' | 'support'
  /** 目标门店/部门名称 */
  targetStore?: string
  /** 派遣原因说明 */
  dispatchReason?: string
  /** 派遣时长（小时） */
  dispatchHours?: number
}

/** 换班申请DTO */
export interface SwapRequestDTO {
  targetShiftId: string
  targetEmployeeId: string
  targetDate: string
  reason: string
}
