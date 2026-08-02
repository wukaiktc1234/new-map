/**
 * 考勤排班类型定义
 * 支持排班管理、考勤记录、月度汇总、门店数据接收
 */

/** 班次类型 */
export type ShiftType =
  | 'morning'    // 早班
  | 'afternoon'  // 中班
  | 'evening'    // 晚班
  | 'full_day'   // 全天
  | 'night'      // 夜班
  | 'rest'       // 休息

/** 班次选项 */
export const ShiftTypeOptions: { value: ShiftType; label: string; timeRange: string }[] = [
  { value: 'morning', label: '早班', timeRange: '06:00-14:00' },
  { value: 'afternoon', label: '中班', timeRange: '10:00-18:00' },
  { value: 'evening', label: '晚班', timeRange: '14:00-22:00' },
  { value: 'full_day', label: '全天', timeRange: '09:00-21:00' },
  { value: 'night', label: '夜班', timeRange: '22:00-06:00' },
  { value: 'rest', label: '休息', timeRange: '' },
]

/** 考勤状态 */
export type AttendanceStatus =
  | 'normal'       // 正常
  | 'late'         // 迟到
  | 'early_leave'  // 早退
  | 'absent'       // 缺勤
  | 'leave'        // 请假
  | 'overtime'     // 加班
  | 'miss_clock'   // 漏打卡
  | 'holiday'      // 法定假日
  | 'rest_day'     // 休息日

/** 考勤状态选项 */
export const AttendanceStatusOptions: { value: AttendanceStatus; label: string }[] = [
  { value: 'normal', label: '正常' },
  { value: 'late', label: '迟到' },
  { value: 'early_leave', label: '早退' },
  { value: 'absent', label: '缺勤' },
  { value: 'leave', label: '请假' },
  { value: 'overtime', label: '加班' },
  { value: 'miss_clock', label: '漏打卡' },
  { value: 'holiday', label: '法定假日' },
  { value: 'rest_day', label: '休息日' },
]

/** 考勤状态到StatusTag映射 */
export const AttendanceStatusTagMap: Record<AttendanceStatus, string> = {
  normal: 'active',
  late: 'warning',
  early_leave: 'warning',
  absent: 'error',
  leave: 'info',
  overtime: 'success',
  miss_clock: 'warning',
  holiday: 'info',
  rest_day: 'default',
}

/** 请假类型 */
export type LeaveType =
  | 'annual'       // 年假
  | 'sick'         // 病假
  | 'personal'     // 事假
  | 'maternity'    // 产假
  | 'paternity'    // 陪产假
  | 'marriage'     // 婚假
  | 'bereavement'  // 丧假
  | 'work_injury'  // 工伤假

/** 请假类型选项 */
export const LeaveTypeOptions: { value: LeaveType; label: string }[] = [
  { value: 'annual', label: '年假' },
  { value: 'sick', label: '病假' },
  { value: 'personal', label: '事假' },
  { value: 'maternity', label: '产假' },
  { value: 'paternity', label: '陪产假' },
  { value: 'marriage', label: '婚假' },
  { value: 'bereavement', label: '丧假' },
  { value: 'work_injury', label: '工伤假' },
]

/** 排班记录 */
export interface ScheduleRecord {
  id: string
  employeeId: string
  employeeName: string
  employeeCode?: string
  departmentName?: string
  positionName?: string
  /** 排班日期 YYYY-MM-DD */
  scheduleDate: string
  /** 班次 */
  shiftType: ShiftType
  /** 上班时间 */
  startTime?: string
  /** 下班时间 */
  endTime?: string
  /** 数据来源：store=门店运营提交, hr=人事手动排班 */
  source: 'store' | 'hr'
  /** 门店ID（来自门店运营时） */
  storeId?: string
  storeName?: string
  /** 备注 */
  remark?: string
  /** 关联排班方案ID */
  planId?: string
  /** 考勤同步状态：not_synced=未同步, synced=已同步, failed=同步失败 */
  syncStatus?: 'not_synced' | 'synced' | 'failed'
}

/** 排班模板 */
export interface ScheduleTemplate {
  id: string
  name: string
  description?: string
  /** 模板行 */
  rows: ScheduleTemplateRow[]
  /** 适用门店 */
  storeIds?: string[]
  /** 创建时间 */
  createTime?: string
}

/** 排班模板行 */
export interface ScheduleTemplateRow {
  dayOfWeek: number  // 0=周日, 1=周一, ..., 6=周六
  shiftType: ShiftType
  startTime?: string
  endTime?: string
}

/** 考勤记录 */
export interface AttendanceRecord {
  id: string
  employeeId: string
  employeeName: string
  employeeCode?: string
  departmentName?: string
  positionName?: string
  /** 考勤日期 YYYY-MM-DD */
  attendanceDate: string
  /** 考勤状态 */
  status: AttendanceStatus
  /** 班次 */
  shiftType: ShiftType
  /** 上班打卡时间 */
  clockInTime?: string
  /** 下班打卡时间 */
  clockOutTime?: string
  /** 迟到分钟数 */
  lateMinutes?: number
  /** 早退分钟数 */
  earlyLeaveMinutes?: number
  /** 加班时长（小时） */
  overtimeHours?: number
  /** 请假类型 */
  leaveType?: LeaveType
  /** 请假时长（小时） */
  leaveHours?: number
  /** 备注 */
  remark?: string
}

/** 月度考勤汇总（用于薪资计算） */
export interface AttendanceMonthlySummary {
  employeeId: string
  employeeName: string
  employeeCode?: string
  departmentName?: string
  /** 年月 YYYY-MM */
  yearMonth: string
  /** 应出勤天数 */
  scheduledDays: number
  /** 实际出勤天数 */
  actualDays: number
  /** 迟到次数 */
  lateCount: number
  /** 早退次数 */
  earlyLeaveCount: number
  /** 缺勤天数 */
  absentDays: number
  /** 请假天数 */
  leaveDays: number
  /** 加班时长（小时） */
  overtimeHours: number
  /** 漏打卡次数 */
  missClockCount: number
  /** 汇总状态：draft=草稿, submitted=已提交, confirmed=已确认 */
  summaryStatus: 'draft' | 'submitted' | 'confirmed'
  /** 提交时间（门店提交时） */
  submitTime?: string
  /** 确认时间（人事确认时） */
  confirmTime?: string
}

/** 考勤查询参数 */
export interface AttendanceQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  yearMonth?: string
  status?: AttendanceStatus
  departmentId?: string
  storeId?: string
}

/** 排班查询参数 */
export interface ScheduleQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  startDate?: string
  endDate?: string
  departmentId?: string
  storeId?: string
  shiftType?: ShiftType
}

/** 月度考勤汇总查询参数 */
export interface AttendanceSummaryQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  yearMonth?: string
  summaryStatus?: 'draft' | 'submitted' | 'confirmed'
  departmentId?: string
}

/** 门店提交的考勤数据（跨模块接收） */
export interface StoreAttendanceSubmission {
  storeId: string
  storeName: string
  yearMonth: string
  submitTime: string
  submitBy: string
  employeeCount: number
  records: AttendanceRecord[]
}

/**
 * 考勤全局统计数据
 * 用于考勤排班页面顶部统计卡片展示（不受列表筛选条件影响）
 */
export interface AttendanceStatistics {
  /** 考勤记录总数 */
  total: number
  /** 正常考勤数 */
  normal: number
  /** 迟到数 */
  late: number
  /** 请假数 */
  leave: number
  /** 缺勤数 */
  absent: number
  /** 加班总时长（小时） */
  overtimeHours: number
  /** 出勤率（百分比，0-100） */
  attendanceRate: number
}
