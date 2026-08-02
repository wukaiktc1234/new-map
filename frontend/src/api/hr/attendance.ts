/**
 * 考勤排班API
 * 对应后端: /v1/attendance (AttendanceController)
 *
 * 对接策略：
 * - attendanceApi.getList：对接真实后端 GET /v1/attendance/page
 * - attendanceApi.getStatistics：对接真实后端 GET /v1/attendance/statistics
 * - attendanceApi.getMonthlySummary：后端仅有单员工月度统计，无列表接口，暂用 Mock
 * - attendanceApi.confirmMonthlySummary：后端未实现，暂用 Mock
 * - scheduleApi 全部：后端未实现排班接口，暂用 Mock
 *
 * 字段映射（后端 AttendanceRecordVO ↔ 前端 AttendanceRecord）：
 * - recordId ↔ id
 * - employeeNo ↔ employeeCode
 * - status(Integer 1-6) ↔ status(string)
 *   1=normal 2=late 3=early_leave 4=absent 5=overtime 6=leave
 * - attendanceDate(LocalDate) + clockInTime(LocalTime) → clockInTime(string "YYYY-MM-DD HH:mm:ss")
 * - shiftType 后端无此字段，默认 'full_day'
 */
import { get, post } from '../request'
import type {
  AttendanceRecord,
  AttendanceMonthlySummary,
  AttendanceQueryParams,
  ScheduleRecord,
  ScheduleQueryParams,
  AttendanceSummaryQueryParams,
  StoreAttendanceSubmission,
  AttendanceStatistics,
} from '../../types/hr/attendance'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// ============================================================
// 后端 AttendanceRecordVO 类型（GET /v1/attendance/page 返回字段）
// ============================================================

interface AttendanceRecordBackend {
  recordId?: number
  employeeId?: string
  employeeName?: string
  employeeNo?: string
  departmentName?: string
  positionName?: string
  /** 考勤日期 YYYY-MM-DD */
  attendanceDate?: string
  /** 上班打卡时间 HH:mm:ss */
  clockInTime?: string
  /** 下班打卡时间 HH:mm:ss */
  clockOutTime?: string
  workHours?: number
  overtimeHours?: number
  leaveTypeName?: string
  leaveHours?: number
  lateMinutes?: number
  earlyLeaveMinutes?: number
  statusName?: string
  /** 1=normal 2=late 3=early_leave 4=absent 5=overtime 6=leave */
  status?: number
  remark?: string
}

/** 后端分页响应结构 */
interface AttendancePageBackend {
  records: AttendanceRecordBackend[]
  total: number
  current: number
  size: number
}

/* ===== 状态映射：后端 Integer ↔ 前端 string ===== */
function statusToFrontend(status?: number): AttendanceRecord['status'] {
  switch (status) {
    case 1: return 'normal'
    case 2: return 'late'
    case 3: return 'early_leave'
    case 4: return 'absent'
    case 5: return 'overtime'
    case 6: return 'leave'
    default: return 'normal'
  }
}

function statusToBackend(status?: string): number | undefined {
  if (!status) return undefined
  const map: Record<string, number> = {
    normal: 1, late: 2, early_leave: 3, absent: 4, overtime: 5, leave: 6,
  }
  return map[status]
}

/* ===== 后端 VO → 前端 AttendanceRecord ===== */
function toAttendanceRecord(item: AttendanceRecordBackend): AttendanceRecord {
  const date = item.attendanceDate || ''
  return {
    id: String(item.recordId ?? ''),
    employeeId: item.employeeId ?? '',
    employeeName: item.employeeName ?? '',
    employeeCode: item.employeeNo ?? '',
    departmentName: item.departmentName ?? '',
    positionName: item.positionName ?? '',
    attendanceDate: date,
    status: statusToFrontend(item.status),
    shiftType: 'full_day',
    clockInTime: item.clockInTime ? `${date} ${item.clockInTime}` : undefined,
    clockOutTime: item.clockOutTime ? `${date} ${item.clockOutTime}` : undefined,
    lateMinutes: item.lateMinutes ?? undefined,
    earlyLeaveMinutes: item.earlyLeaveMinutes ?? undefined,
    overtimeHours: item.overtimeHours ?? undefined,
    leaveHours: item.leaveHours ?? undefined,
    remark: item.remark ?? undefined,
  }
}

// ============================================================
// Mock 数据 - 月度汇总（后端未实现列表接口，暂用 Mock）
// ============================================================

const mockMonthlySummaryList: AttendanceMonthlySummary[] = [
  {
    employeeId: 'EMP001',
    employeeName: '张伟',
    employeeCode: 'EMP001',
    departmentName: '前厅部',
    yearMonth: '2026-05',
    scheduledDays: 26,
    actualDays: 25,
    lateCount: 1,
    earlyLeaveCount: 0,
    absentDays: 0,
    leaveDays: 1,
    overtimeHours: 4,
    missClockCount: 0,
    summaryStatus: 'confirmed',
    submitTime: '2026-06-01 10:00:00',
    confirmTime: '2026-06-03 09:00:00',
  },
  {
    employeeId: 'EMP002',
    employeeName: '李娜',
    employeeCode: 'EMP002',
    departmentName: '前厅部',
    yearMonth: '2026-05',
    scheduledDays: 26,
    actualDays: 24,
    lateCount: 3,
    earlyLeaveCount: 1,
    absentDays: 0,
    leaveDays: 2,
    overtimeHours: 0,
    missClockCount: 1,
    summaryStatus: 'confirmed',
    submitTime: '2026-06-01 10:00:00',
    confirmTime: '2026-06-03 09:00:00',
  },
  {
    employeeId: 'EMP003',
    employeeName: '王强',
    employeeCode: 'EMP003',
    departmentName: '后厨部',
    yearMonth: '2026-05',
    scheduledDays: 26,
    actualDays: 26,
    lateCount: 0,
    earlyLeaveCount: 0,
    absentDays: 0,
    leaveDays: 0,
    overtimeHours: 12,
    missClockCount: 0,
    summaryStatus: 'submitted',
    submitTime: '2026-06-01 10:00:00',
  },
]

// ============================================================
// Mock 数据 - 排班记录（后端未实现排班接口，暂用 Mock）
// ============================================================

let mockScheduleList: ScheduleRecord[] = [
  {
    id: 'SCH001',
    employeeId: 'EMP001',
    employeeName: '张伟',
    employeeCode: 'EMP001',
    departmentName: '前厅部',
    positionName: '前厅经理',
    scheduleDate: '2026-06-18',
    shiftType: 'full_day',
    startTime: '09:00',
    endTime: '21:00',
    source: 'hr',
    remark: '',
    planId: 'PLAN001',
    syncStatus: 'not_synced',
  },
  {
    id: 'SCH002',
    employeeId: 'EMP002',
    employeeName: '李娜',
    employeeCode: 'EMP002',
    departmentName: '前厅部',
    positionName: '服务员',
    scheduleDate: '2026-06-18',
    shiftType: 'morning',
    startTime: '06:00',
    endTime: '14:00',
    source: 'store',
    storeId: 'store-001',
    storeName: 'XX餐饮总店',
    remark: '',
  },
  {
    id: 'SCH003',
    employeeId: 'EMP003',
    employeeName: '王强',
    employeeCode: 'EMP003',
    departmentName: '后厨部',
    positionName: '厨师长',
    scheduleDate: '2026-06-18',
    shiftType: 'full_day',
    startTime: '09:00',
    endTime: '21:00',
    source: 'hr',
    remark: '',
  },
  {
    id: 'SCH004',
    employeeId: 'EMP004',
    employeeName: '刘洋',
    employeeCode: 'EMP004',
    departmentName: '后厨部',
    positionName: '炒锅师傅',
    scheduleDate: '2026-06-18',
    shiftType: 'evening',
    startTime: '14:00',
    endTime: '22:00',
    source: 'hr',
    remark: '',
  },
  {
    id: 'SCH005',
    employeeId: 'EMP010',
    employeeName: '郑浩',
    employeeCode: 'EMP010',
    departmentName: '前厅部',
    positionName: '服务员',
    scheduleDate: '2026-06-18',
    shiftType: 'afternoon',
    startTime: '10:00',
    endTime: '18:00',
    source: 'store',
    storeId: 'store-001',
    storeName: 'XX餐饮总店',
    remark: '试用期排班',
  },
]

// ============================================================
// 考勤API
// ============================================================

export const attendanceApi = {
  /**
   * 分页查询考勤记录（GET /v1/attendance/page）
   * 对接真实后端，字段映射在 toAttendanceRecord 中完成
   */
  async getList(params?: AttendanceQueryParams): Promise<{ records: AttendanceRecord[]; total: number }> {
    // yearMonth 转换为 startDate/endDate
    let startDate: string | undefined
    let endDate: string | undefined
    if (params?.yearMonth) {
      startDate = `${params.yearMonth}-01`
      // 取月份最后一天
      const [year, month] = params.yearMonth.split('-').map(Number)
      const lastDay = new Date(year, month, 0).getDate()
      endDate = `${params.yearMonth}-${String(lastDay).padStart(2, '0')}`
    }

    const res = await get<AttendancePageBackend>('/v1/attendance/page', {
      current: params?.page ?? 1,
      size: params?.pageSize ?? 10,
      employeeName: params?.keyword || undefined,
      status: statusToBackend(params?.status),
      departmentId: params?.departmentId || undefined,
      startDate,
      endDate,
    })
    return {
      records: (res?.records || []).map(toAttendanceRecord),
      total: res?.total ?? 0,
    }
  },

  /**
   * 获取月度考勤汇总
   * 注意：后端仅有 GET /v1/attendance/monthly-summary/{employeeId}/{yearMonth} 单员工接口，
   * 无列表接口，此处暂用 Mock。待后端补全列表接口后切换为真实调用。
   */
  async getMonthlySummary(params?: AttendanceSummaryQueryParams): Promise<{ records: AttendanceMonthlySummary[]; total: number }> {
    await delay()
    let records = [...mockMonthlySummaryList]
    if (params?.yearMonth) records = records.filter(r => r.yearMonth === params.yearMonth)
    if (params?.summaryStatus) records = records.filter(r => r.summaryStatus === params.summaryStatus)
    const total = records.length
    return { records, total }
  },

  /**
   * 确认月度考勤汇总
   * 注意：后端未实现此端点，暂用 Mock。待后端补全后切换为真实调用。
   */
  async confirmMonthlySummary(employeeId: string, yearMonth: string): Promise<void> {
    await delay()
    const summary = mockMonthlySummaryList.find(
      s => s.employeeId === employeeId && s.yearMonth === yearMonth
    )
    if (summary) {
      summary.summaryStatus = 'confirmed'
      summary.confirmTime = new Date().toISOString()
    }
  },

  /**
   * 获取考勤全局统计（GET /v1/attendance/statistics）
   * 真实后端调用，统计数据不受列表筛选条件影响，反映全量考勤状态
   */
  async getStatistics(): Promise<AttendanceStatistics> {
    const res = await get<Record<string, number>>('/v1/attendance/statistics')
    const statusDist = (res as Record<string, unknown>)?.statusDistribution as Record<string, number> | undefined
    return {
      total: res.total || 0,
      normal: statusDist?.normal || 0,
      late: statusDist?.late || 0,
      leave: statusDist?.onLeave || 0,
      absent: statusDist?.absent || 0,
      overtimeHours: res.overtimeHours || res.overtime_hours || 0,
      attendanceRate: res.attendanceRate || res.attendance_rate || 0,
    }
  },
}

// ============================================================
// 排班API（后端未实现排班接口，全部暂用 Mock）
// ============================================================

export const scheduleApi = {
  /**
   * 分页查询排班记录
   * 注意：后端未实现，暂用 Mock。待后端补全排班接口后切换为真实调用。
   */
  async getList(params?: ScheduleQueryParams): Promise<{ records: ScheduleRecord[]; total: number }> {
    await delay()
    let records = [...mockScheduleList]
    if (params?.shiftType) records = records.filter(r => r.shiftType === params.shiftType)
    if (params?.keyword) {
      const kw = params.keyword.toLowerCase()
      records = records.filter(r =>
        r.employeeName.toLowerCase().includes(kw) ||
        r.employeeCode?.toLowerCase().includes(kw)
      )
    }
    const total = records.length
    const page = params?.page || 1
    const size = params?.pageSize || 10
    const start = (page - 1) * size
    return { records: records.slice(start, start + size), total }
  },

  /**
   * 创建排班记录
   * 注意：后端未实现，暂用 Mock。待后端补全排班接口后切换为真实调用。
   */
  async create(data: Omit<ScheduleRecord, 'id'>): Promise<ScheduleRecord> {
    await delay()
    const newRecord: ScheduleRecord = {
      ...data,
      id: `SCH${Date.now()}`,
    }
    mockScheduleList.push(newRecord)
    return newRecord
  },

  /**
   * 更新排班记录
   * 注意：后端未实现，暂用 Mock。待后端补全排班接口后切换为真实调用。
   */
  async update(id: string, data: Partial<ScheduleRecord>): Promise<ScheduleRecord> {
    await delay()
    const index = mockScheduleList.findIndex(s => s.id === id)
    if (index === -1) throw new Error('排班记录不存在')
    Object.assign(mockScheduleList[index], data)
    return { ...mockScheduleList[index] }
  },

  /**
   * 批量创建排班记录
   * 注意：后端未实现，暂用 Mock。待后端补全排班接口后切换为真实调用。
   */
  async batchCreate(records: Omit<ScheduleRecord, 'id'>[]): Promise<ScheduleRecord[]> {
    await delay()
    const newRecords: ScheduleRecord[] = records.map(r => ({
      ...r,
      id: `SCH${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    }))
    mockScheduleList.push(...newRecords)
    return newRecords
  },

  /**
   * 获取门店提交的考勤数据
   * 注意：后端未实现，暂用 Mock。待后端补全后切换为真实调用。
   */
  async getStoreSubmissions(yearMonth: string): Promise<StoreAttendanceSubmission[]> {
    await delay()
    return [
      {
        storeId: 'store-001',
        storeName: 'XX餐饮总店',
        yearMonth,
        submitTime: `${yearMonth}-01 10:00:00`,
        submitBy: '张伟',
        employeeCount: 8,
        records: [],
      },
    ]
  },

  /**
   * 同步排班到考勤系统
   * POST /v1/schedule/attendance/{planId}/sync
   */
  async syncToAttendance(planId: string): Promise<{ syncStatus: string; syncedEntries: number; failedEntries: number }> {
    const res = await post<{ syncStatus: string; syncedEntries: number; failedEntries: number }>(
      `/v1/schedule/attendance/${planId}/sync`
    )
    return {
      syncStatus: res?.syncStatus ?? 'synced',
      syncedEntries: res?.syncedEntries ?? 0,
      failedEntries: res?.failedEntries ?? 0,
    }
  },
}
