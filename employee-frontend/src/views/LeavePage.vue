<script setup lang="ts">
/**
 * LeavePage - 请假管理页面
 *
 * 已拆分为2个子组件：
 *   - LeaveOverview: 总览视图（假期余额/考勤联动/审批中列表/快捷入口）
 *   - LeaveRecordsView: 全部记录视图（日历查找/记录列表/工具栏）
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, RefreshLeft } from '@element-plus/icons-vue'
import { PageContainer, EmptyState, StatusTag } from '@/components/core'
import ViewSwitcher from '@/components/core/ViewSwitcher.vue'

// 子组件导入
import LeaveOverview from './LeavePage/components/LeaveOverview.vue'
import LeaveRecordsView from './LeavePage/components/LeaveRecordsView.vue'

import { leaveApi, approvalApi } from '@/api'
import { seedApprovalData, getApprovalsByTab, getApproval, getApprovalStats } from '@/api/mock'
import type {
  LeaveType, LeaveStatus, LeaveRecord,
  LeaveBalance, ApprovalNode,
} from '@/types/leave'
import { LEAVE_TYPE_LABEL_MAP } from '@/types'
import type { ApprovalItem } from '@/types/approval'

const router = useRouter()

const activeView = ref<'overview' | 'records'>('overview')

const VISIBLE_NODE_COUNT = 2
const expandedTimelines = ref<Set<string>>(new Set())
const expandedRecords = ref<Set<string>>(new Set())

function isTimelineExpanded(recordId: string): boolean {
  return expandedTimelines.value.has(recordId)
}

function toggleTimeline(recordId: string) {
  if (expandedTimelines.value.has(recordId)) {
    expandedTimelines.value.delete(recordId)
  } else {
    expandedTimelines.value.add(recordId)
  }
}

function isRecordExpanded(recordId: string): boolean {
  return expandedRecords.value.has(recordId)
}

function toggleRecord(recordId: string) {
  if (expandedRecords.value.has(recordId)) {
    expandedRecords.value.delete(recordId)
  } else {
    expandedRecords.value.add(recordId)
  }
}

function expandAllRecords() {
  historyRecords.value.forEach(r => expandedRecords.value.add(r.id))
}

function collapseAllRecords() {
  expandedRecords.value.clear()
}

function getVisibleNodes(record: LeaveRecord): ApprovalNode[] {
  const nodes = record.approvalHistory
  if (isTimelineExpanded(record.id) || nodes.length <= VISIBLE_NODE_COUNT) {
    return nodes
  }
  return nodes.slice(0, VISIBLE_NODE_COUNT)
}

function hasMoreNodes(record: LeaveRecord): boolean {
  return record.approvalHistory.length > VISIBLE_NODE_COUNT
}

function getHiddenCount(record: LeaveRecord): number {
  return record.approvalHistory.length - VISIBLE_NODE_COUNT
}

const balances = ref<LeaveBalance[]>([
  { type: 'annual', total: 10, used: 5, remaining: 5 },
  { type: 'sick', total: 15, used: 2, remaining: 13 },
  { type: 'personal', total: 0, used: 0, remaining: 0 },
  { type: 'compensatory', total: 3, used: 1, remaining: 2 },
])

const totalRemaining = computed(() =>
  balances.value.reduce((s, b) => s + b.remaining, 0)
)

interface AttendanceDay {
  date: string
  weekday: string
  status: 'normal' | 'leave' | 'late' | 'absent' | 'holiday' | 'rest'
  label: string
  leaveType?: string
}

const recentAttendance = ref<AttendanceDay[]>([
  { date: '05-26', weekday: '周一', status: 'normal', label: '正常' },
  { date: '05-27', weekday: '周二', status: 'normal', label: '正常' },
  { date: '05-28', weekday: '周三', status: 'leave', label: '年假', leaveType: 'annual' },
  { date: '05-29', weekday: '周四', status: 'leave', label: '年假', leaveType: 'annual' },
  { date: '05-30', weekday: '周五', status: 'normal', label: '正常' },
  { date: '05-31', weekday: '周六', status: 'rest', label: '休息' },
  { date: '06-01', weekday: '周日', status: 'rest', label: '休息' },
])

const historyRecords = ref<LeaveRecord[]>([])

function loadLeaveRecords() {
  seedApprovalData()
  const all = getApprovalsByTab('role', '')
  const leaveItems = all.filter(a => a.type === 'leave')

  historyRecords.value = leaveItems.map(a => {
    const detail = getApproval(a.id)
    const nodes: ApprovalNode[] = (detail?.flowNodes || []).map(n => ({
      role: n.role,
      approverName: n.userName || '',
      status: n.status === 'completed'
        ? (n.action === '驳回' ? 'rejected' : 'approved')
        : n.status === 'current' ? 'pending' : 'pending',
      actionAt: n.time || undefined,
      comment: n.action || undefined,
    }))

    return {
      id: a.id,
      form: {
        leaveType: 'annual',
        startDate: a.createdAt.slice(0, 10),
        endDate: a.createdAt.slice(0, 10),
        days: 1,
        reason: a.summary,
      },
      status: a.status === 'approved' ? 'approved' : a.status === 'rejected' ? 'rejected' : a.status === 'cancelled' ? 'withdrawn' : 'pending',
      submittedAt: a.createdAt,
      approvalHistory: nodes,
      canWithdraw: a.status === 'pending' || a.status === 'processing',
    }
  })
}

const pendingCount = computed(() =>
  historyRecords.value.filter(r => r.status === 'pending').length
)

async function withdrawRecord(id: string) {
  try {
    await approvalApi.withdraw(id)
    loadLeaveRecords()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  }
}

function goCreateLeave() {
  router.push('/approval/create/leave')
}

function goApprovalCenter() {
  router.push('/approval')
}

onMounted(() => {
  loadLeaveRecords()
})

// ========== 日历相关逻辑 ==========
interface CalendarDay {
  date: number
  fullDate: string
  isCurrentMonth: boolean
  hasRecord: boolean
  recordIds: string[]
  isToday: boolean
}

const calendarYear = ref(2025)
const calendarMonth = ref(5)

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日']

const calendarDays = computed(() => {
  const year = calendarYear.value
  const month = calendarMonth.value
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startWeekday = (firstDay.getDay() + 6) % 7
  const daysInMonth = lastDay.getDate()
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

  const recordDateMap = new Map<string, string[]>()
  historyRecords.value.forEach(r => {
    const start = r.form.startDate
    const end = r.form.endDate
    const cur = new Date(start)
    const endDate = new Date(end)
    while (cur <= endDate) {
      const dateStr = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}-${String(cur.getDate()).padStart(2, '0')}`
      if (!recordDateMap.has(dateStr)) recordDateMap.set(dateStr, [])
      recordDateMap.get(dateStr)!.push(r.id)
      cur.setDate(cur.getDate() + 1)
    }
  })

  const days: CalendarDay[] = []

  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = startWeekday - 1; i >= 0; i--) {
    const d = prevMonthLastDay - i
    const pm = month - 1
    const py = pm < 0 ? year - 1 : year
    const actualMonth = pm < 0 ? 11 : pm
    days.push({
      date: d,
      fullDate: `${py}-${String(actualMonth + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`,
      isCurrentMonth: false,
      hasRecord: false,
      recordIds: [],
      isToday: false,
    })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const recordIds = recordDateMap.get(dateStr) || []
    days.push({
      date: d,
      fullDate: dateStr,
      isCurrentMonth: true,
      hasRecord: recordIds.length > 0,
      recordIds,
      isToday: dateStr === todayStr,
    })
  }

  const remaining = 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const nm = month + 1
    const ny = nm > 11 ? year + 1 : year
    const actualMonth = nm > 11 ? 0 : nm
    days.push({
      date: d,
      fullDate: ny + '-' + String(actualMonth + 1).padStart(2, '0') + '-' + String(d).padStart(2, '0'),
      isCurrentMonth: false,
      hasRecord: false,
      recordIds: [],
      isToday: false,
    })
  }

  return days
})

const calendarMonthLabel = computed(() => `${calendarYear.value}年${calendarMonth.value + 1}月`)

function prevMonth() {
  if (calendarMonth.value === 0) {
    calendarMonth.value = 11
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
}

function nextMonth() {
  if (calendarMonth.value === 11) {
    calendarMonth.value = 0
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
}

const selectedDate = ref<string | null>(null)

const selectedDateRecords = computed(() => {
  if (!selectedDate.value) return []
  return historyRecords.value.filter(r => {
    const start = r.form.startDate
    const end = r.form.endDate
    return selectedDate.value! >= start && selectedDate.value! <= end
  })
})

function handleCalendarDayClick(day: CalendarDay) {
  if (!day.isCurrentMonth || !day.hasRecord) return
  selectedDate.value = day.fullDate
  day.recordIds.forEach(id => expandedRecords.value.add(id))
}
</script>

<template>
  <PageContainer title="请假管理">
    <template #headerActions>
      <el-button type="primary" :icon="Plus" size="small" @click="goCreateLeave">
        申请请假
      </el-button>
    </template>

    <!-- 视图切换 -->
    <ViewSwitcher
      v-model="activeView"
      :options="[
        { key: 'overview', label: '总览' },
        { key: 'records', label: '全部记录', badge: pendingCount || undefined },
      ]"
    />

    <!-- ====== 总览视图 ====== -->
    <template v-if="activeView === 'overview'">
      <LeaveOverview
        :balances="balances"
        :total-remaining="totalRemaining"
        :recent-attendance="recentAttendance"
        :history-records="historyRecords"
        :pending-count="pendingCount"
        @create-leave="goCreateLeave"
        @go-approval-center="goApprovalCenter"
        @withdraw="withdrawRecord($event)"
        @view-detail="router.push(`/approval/detail/${$event}`)"
        @go-schedule="router.push('/schedule')"
      />
    </template>

    <!-- ====== 全部记录视图 ====== -->
    <template v-if="activeView === 'records'">
      <LeaveRecordsView
        :calendar-year="calendarYear"
        :calendar-month="calendarMonth"
        :calendar-days="calendarDays"
        :calendar-month-label="calendarMonthLabel"
        :selected-date="selectedDate"
        :selected-date-records="selectedDateRecords"
        :history-records="historyRecords"
        :expanded-timelines="expandedTimelines"
        :expanded-records="expandedRecords"
        @prev-month="prevMonth"
        @next-month="nextMonth"
        @select-date="handleCalendarDayClick"
        @clear-selection="selectedDate = null"
        @expand-all-records="expandAllRecords"
        @collapse-all-records="collapseAllRecords"
        @toggle-record="toggleRecord($event)"
        @toggle-timeline="toggleTimeline($event)"
        @withdraw="withdrawRecord($event)"
        @view-detail="router.push(`/approval/detail/${$event}`)"
      />
    </template>
  </PageContainer>
</template>

<style scoped lang="scss">
/* 主页面仅保留最小化样式（如需要） */
/* 所有子组件的样式已在各自文件中定义 */
</style>
