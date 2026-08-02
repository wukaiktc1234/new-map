<script setup lang="ts">
/**
 * SchedulePage - 排班页面编排器
 *
 * 状态机驱动的三层视图编排：
 * - personal (个人周视图) → team (团队视图) → detail (班次详情)
 * 使用 useScheduleView composable 管理所有视图状态。
 */
import { computed, ref, onMounted, watch } from 'vue'
import { PageContainer } from '@/components/core'
import { useScheduleView } from '@/composables/useScheduleView'
import { scheduleApi } from '@/api/schedule'
import { SHIFT_TIME_MAP } from '@/types/schedule'
import { useAuthContext } from '@/composables/useAuthContext'

const auth = useAuthContext()
import type { DaySchedule, ShiftDetail, TeamColleagueItem, ShiftItem, ScheduleLayer } from '@/types/schedule'

// 子组件
import ScheduleHeader from './schedule/components/ScheduleHeader.vue'
import PersonalWeekView from './schedule/components/PersonalWeekView.vue'
import TeamScheduleMobile from './schedule/components/TeamScheduleMobile.vue'

// 状态机
const {
  currentLayer,
  weekOffset,
  weekRange,
  searchKeyword,
  isPersonalView,
  isTeamView,
  isDetailView,
  goToTeamView,
  openShiftDetail,
  backToPersonal,
  prevWeek,
  nextWeek,
  goToday,
} = useScheduleView()

// ===== 数据加载 =====

/** 本周7天排班数据 */
const weekData = ref<DaySchedule[]>([])
/** 团队同事数据 */
const teamColleagues = ref<TeamColleagueItem[]>([])
/** 团队数据加载状态 */
const teamLoading = ref(false)

/** 是否有权限查看团队视图（L3+） */
const canViewTeam = computed(() => auth.isManager())

async function fetchWeekData() {
  const data = await scheduleApi.getWeekSchedule(weekOffset.value)
  const todayStr = new Date().toISOString().slice(0, 10)

  weekData.value = data.days.map((item): DaySchedule => {
    const dow = new Date(item.date).getDay()
    const isRest = item.shiftType === 'rest'

    let shifts: ShiftItem[] = []
    if (!isRest) {
      const timeRange = SHIFT_TIME_MAP[item.shiftType]
      const [startTime, endTime] = timeRange ? timeRange.split('-') : ['', '']
      shifts = [{
        shiftId: `${item.date}-1`,
        shiftType: item.shiftType,
        startTime,
        endTime,
      }]
    }

    return {
      date: item.date,
      dayOfWeek: dow,
      dayOfWeekName: item.dayOfWeek,
      isToday: item.date === todayStr,
      isRest,
      shifts,
    }
  })

  // 并行加载团队数据（仅 L3+ 有权限时）
  if (canViewTeam.value) {
    fetchTeamData()
  }
}

/** 加载团队排班数据（从 scheduleApi 获取，已内置权限门控） */
async function fetchTeamData() {
  teamLoading.value = true
  try {
    const rawTeam = await scheduleApi.getTeamWeekSchedule(weekOffset.value)
    // 将 API 返回格式转换为 TeamColleagueItem 格式
    teamColleagues.value = rawTeam.map(member => {
      // 将 API 的 weekDots（{ dayIndex, shiftType, shiftName }）转换为 WeekDot（{ date, isWorking, shiftType? }）
      const today = new Date()
      const dayOfWeek = today.getDay()
      const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
      const monday = new Date(today)
      monday.setDate(today.getDate() + diff + weekOffset.value * 7)
      const weekDots = member.weekDots.map(dot => {
        const dotDate = new Date(monday)
        dotDate.setDate(monday.getDate() + dot.dayIndex)
        return {
          date: dotDate.toISOString().slice(0, 10),
          isWorking: dot.shiftType !== 'rest',
          shiftType: dot.shiftType,
        }
      })
      return {
        employeeId: member.employeeId,
        avatar: '',
        name: member.employeeName,
        position: member.position,
        isSelf: member.isSelf,
        positionGroup: member.department || '',
        weekDots,
      }
    })
  } finally {
    teamLoading.value = false
  }
}

onMounted(() => {
  fetchWeekData()
})

watch(weekOffset, () => {
  fetchWeekData()
})

/** 团队同事列表（从 API 加载，权限门控在 scheduleApi 层） */
const colleagues = computed<TeamColleagueItem[]>(() => teamColleagues.value)

// ===== 适配 ScheduleHeader 的 WeekRange 接口 =====
const headerWeekRange = computed(() => ({
  weekNumber: Math.ceil(new Date(weekRange.value.startDate).getDate() / 7),
  start: weekRange.value.startDate,
  end: weekRange.value.endDate,
}))

// ===== 事件处理 =====

function handleToggleLayer(layer: ScheduleLayer) {
  if (layer === 'team') {
    goToTeamView()
  } else {
    backToPersonal()
  }
}

function handleClickDay(date: string) {
  // 点击日期暂无操作（详情抽屉已移除）
}

function handleViewTeam() {
  goToTeamView()
}

function goToTeamDetail() {
  goToTeamView()
}

function handleOpenShiftDetail(_shiftId: string) {
  // 点击班次暂无操作（详情抽屉已移除）
}

function jumpToWeek(offset: number) {
  weekOffset.value = offset
}
</script>

<template>
  <PageContainer title="我的排班" :sticky-footer="false">
    <!-- 个人视图 / 团队视图 共用 Header -->
    <template v-if="isPersonalView || isTeamView">
      <ScheduleHeader
        :week-range="headerWeekRange"
        :current-layer="currentLayer"
        :week-offset="weekOffset"
        @prev-week="prevWeek"
        @next-week="nextWeek"
        @go-today="goToday"
        @toggle-layer="handleToggleLayer"
        @jump-week="jumpToWeek"
      />
    </template>

    <!-- Layer 1: 个人周视图 -->
    <PersonalWeekView
      v-if="isPersonalView"
      :week-data="weekData"
      :loading="false"
      :show-team-button="canViewTeam"
      @view-team="handleViewTeam"
      @click-day="handleClickDay"
    />

    <!-- Layer 2: 团队视图（仅 L3+ 可见） -->
    <TeamScheduleMobile
      v-if="isTeamView && canViewTeam"
      :colleagues="colleagues"
      :loading="teamLoading"
      :search-keyword="searchKeyword"
      @search="(kw: string) => searchKeyword = kw"
      @click-colleague="handleOpenShiftDetail"
      @load-more="() => {}"
    />

    <!-- 无权限提示：L1/L2 误入团队视图时显示 -->
    <div v-else-if="isTeamView && !canViewTeam" class="schedule-no-permission">
      <el-empty description="暂无团队数据查看权限" />
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
/* 编排器无需自定义样式，全部由子组件负责 */

.schedule-no-permission {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}
</style>
