import type { ScheduleItem, WeekScheduleData, ShiftType } from '@/types/schedule'
import { SHIFT_LABEL_MAP, SHIFT_TIME_MAP } from '@/types/schedule'
import { mockDelay } from './mock/delays'
import { useAuthContext } from '@/composables/useAuthContext'

const auth = useAuthContext()

function getMonday(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  return d
}

function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10)
}

function getDayOfWeekLabel(d: Date): string {
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return labels[d.getDay()]
}

function isWorkday(d: Date): boolean {
  const dow = d.getDay()
  return dow !== 0 && dow !== 6
}

const WEEKDAY_SHIFTS: ShiftType[] = ['morning', 'afternoon', 'evening', 'morning', 'afternoon']
const WEEKEND_SHIFT: ShiftType = 'rest'

function generateWeekSchedule(weekOffset: number): WeekScheduleData {
  const today = new Date()
  const monday = getMonday(today)
  monday.setDate(monday.getDate() + weekOffset * 7)

  const days: ScheduleItem[] = []
  let workDays = 0
  let restDays = 0
  let morningCount = 0
  let afternoonCount = 0
  let eveningCount = 0
  let workdayIndex = 0

  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const dateStr = formatDate(d)
    const isToday = dateStr === formatDate(today)

    const shiftType: ShiftType = isWorkday(d) ? WEEKDAY_SHIFTS[workdayIndex++] : WEEKEND_SHIFT

    let status: ScheduleItem['status']
    if (shiftType === 'rest') {
      status = 'rest'
    } else if (isToday) {
      status = 'today'
    } else {
      const now = new Date()
      status = d < now ? 'completed' : 'upcoming'
    }

    if (shiftType !== 'rest') {
      workDays++
      if (shiftType === 'morning') morningCount++
      else if (shiftType === 'afternoon') afternoonCount++
      else eveningCount++
    } else {
      restDays++
    }

    days.push({
      id: `sch-${dateStr}`,
      date: dateStr,
      dayOfWeek: getDayOfWeekLabel(d),
      shiftType,
      shiftLabel: SHIFT_LABEL_MAP[shiftType],
      timeRange: SHIFT_TIME_MAP[shiftType],
      status,
      isWorkday: shiftType !== 'rest',
      note: shiftType === 'evening' ? '晚班请注意安全' : undefined,
    })
  }

  return {
    weekStart: formatDate(monday),
    weekEnd: formatDate(new Date(monday.getTime() + 6 * 86400000)),
    days,
    stats: {
      totalDays: 7,
      workDays,
      restDays,
      morningCount,
      afternoonCount,
      eveningCount,
    },
  }
}

export const scheduleApi = {
  async getWeekSchedule(weekOffset: number = 0): Promise<WeekScheduleData> {
    await mockDelay(60, 200)
    return generateWeekSchedule(weekOffset)
  },

  /**
   * 获取今日排班详情（用于首页卡片和班次抽屉）
   * 后端 B-05 就绪后替换为: GET /v1/schedules/mine?date=
   */
  async getTodaySchedule(): Promise<{
    shiftType: ShiftType
    shiftName: string
    startTime: string
    endTime: string
  } | null> {
    await mockDelay(30, 80)
    const today = new Date()
    const dateStr = formatDate(today)
    const weekData = generateWeekSchedule(0)
    const day = weekData.days.find(d => d.date === dateStr)

    if (!day || day.shiftType === 'rest') return null

    const timeRange = SHIFT_TIME_MAP[day.shiftType] || ''
    const [startTime = '', endTime = ''] = timeRange.split(' - ')

    return {
      shiftType: day.shiftType,
      shiftName: day.shiftLabel,
      startTime,
      endTime,
    }
  },

  /**
   * 获取团队周排班数据（权限过滤：仅 L3+ 可调用）
   *
   * 后端 B-05 就绪后替换为: GET /v1/schedules/team?week_start=
   *
   * 当前 Mock 阶段：
   * - L1/L2 调用返回空数组（无权查看）
   * - L3+ 返回模拟团队数据
   */
  async getTeamWeekSchedule(weekOffset: number = 0): Promise<Array<{
    employeeId: string
    employeeName: string
    position: string
    department: string
    isSelf: boolean
    weekDots: Array<{ dayIndex: number; shiftType: ShiftType; shiftName: string }>
    workDaysCount: number
  }>> {
    await mockDelay(60, 200)

    // 权限门控：L1/L2 无团队数据访问权限
    if (!auth.isManager()) {
      return []
    }

    // 模拟团队数据：根据当前用户身份生成合理的同事列表
    const selfName = auth.userName.value
    const monday = getMonday(new Date())
    monday.setDate(monday.getDate() + weekOffset * 7)

    // 生成一周的班次模板（与个人视图保持一致）
    const weekShifts: ShiftType[] = []
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday)
      d.setDate(monday.getDate() + i)
      const isWorkDay = d.getDay() !== 0 && d.getDay() !== 6
      weekShifts.push(isWorkDay ? WEEKDAY_SHIFTS[i % WEEKDAY_SHIFTS.length] : 'rest')
    }

    // 团队成员（模拟同一门店的同事）
    const teamMembers = [
      { employeeId: 'self', employeeName: selfName, position: '服务员', department: '前厅部', isSelf: true },
      { employeeId: 'c2', employeeName: '李四', position: '厨师', department: '厨房部', isSelf: false },
      { employeeId: 'c3', employeeName: '王五', position: '服务员', department: '前厅部', isSelf: false },
      { employeeId: 'c4', employeeName: '赵敏', position: '收银员', department: '前厅部', isSelf: false },
      { employeeId: 'c5', employeeName: '钱七', position: '厨师长', department: '厨房部', isSelf: false },
    ]

    // 为每个成员生成 weekDots（错开班次，模拟真实排班）
    return teamMembers.map((member, memberIdx) => {
      const dots = weekShifts.map((baseShift, dayIdx) => {
        // 错开班次：不同成员有不同的偏移模式
        let shift: ShiftType = baseShift
        if (!member.isSelf && baseShift !== 'rest') {
          const offsetPatterns: Record<number, number[]> = {
            1: [0, 1, 2],   // 李四：早→中→晚 偏移
            2: [1, 2, 0],   // 王五：中→晚→早
            3: [0, 0, 1],   // 赵敏：多早班
            4: [2, 2, 1],   // 钱七：多晚班
          }
          const pattern = offsetPatterns[memberIdx] || [0]
          const shiftTypes: ShiftType[] = ['morning', 'afternoon', 'evening']
          const offset = pattern[dayIdx % pattern.length]
          shift = shiftTypes[offset]
        }

        return {
          dayIndex: dayIdx,
          shiftType: shift,
          shiftName: SHIFT_LABEL_MAP[shift],
        }
      })

      const workDaysCount = dots.filter(d => d.shiftType !== 'rest').length

      return {
        ...member,
        weekDots: dots,
        workDaysCount,
      }
    })
  },
}