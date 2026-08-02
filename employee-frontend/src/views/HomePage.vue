<script setup lang="ts">
/**
 * HomePage - 首页编排器
 *
 * 纯编排层：组合6个子组件，负责数据准备和动画协调。
 * 所有业务UI逻辑已提取至 home/components/ 子组件。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getIconComponent, usePermissionStore } from '@/stores/permission'
import { PageContainer } from '@/components/core'
import { useStaggerAnimation } from '@/composables/useStaggerAnimation'
import { useCountUp } from '@/composables/useCountUp'
import { useQuickActions } from '@/composables/useQuickActions'
import { attendanceApi } from '@/api/attendance'
import type { TodayAttendance } from '@/api/attendance'

// 子组件
import GreetingSection from './home/components/GreetingSection.vue'
import QuickActionsGrid from './home/components/QuickActionsGrid.vue'
import TodoSummaryCard from './home/components/TodoSummaryCard.vue'
import TodayAttendanceCard from './home/components/TodayAttendanceCard.vue'
import AnnouncementList from './home/components/AnnouncementList.vue'
import OnboardingWizard from '@/components/business/OnboardingWizard.vue'
import CreateApprovalDialog from '@/components/business/CreateApprovalDialog.vue'
import { useOnboarding } from '@/composables/useOnboarding'

const router = useRouter()
const permission = usePermissionStore()
const { start: startOnboarding } = useOnboarding()

// 用户信息
const userName = computed(() => permission.userInfo.name || '员工')
const storeName = computed(() => permission.userInfo.storeName)

// 快捷操作（权限驱动 + 用户自定义）
const { selectedIds } = useQuickActions()
const quickActions = computed(() =>
  selectedIds.value
    .filter(id => permission.isModuleVisible(id))
    .map(id => {
      const mod = permission.getVisibleNavModules().find(m => m.id === id)
      return {
        id: mod?.id || id,
        icon: mod ? getIconComponent(mod.icon) : undefined,
        label: mod?.label || id,
        route: mod?.route || '/',
        badge: mod?.badge ? Number(mod.badge) || 0 : undefined,
      }
    }).filter(a => a.icon)
)

// 统计数据（带数字滚动）
const attendanceDisplay = useCountUp({ target: 22, suffix: '%', duration: 1000 })
const pendingDisplay = useCountUp({ target: 3, duration: 1000, initialDelay: 200 })
const alertsDisplay = useCountUp({ target: 1, duration: 1000, initialDelay: 400 })

// 公告数据
const announcements = computed(() => [
  { id: '1', title: '关于端午节放假安排的通知', isImportant: true, time: '今天 09:00', isRead: false },
  { id: '2', title: '6月食品安全培训考核通知', isImportant: false, time: '昨天 14:30', isRead: true },
])

// 今日排班（mock）
const todaySchedule = computed(() => ({
  isDayOff: false,
  shifts: [
    { type: 'morning' as const, name: '早班', startTime: '07:00', endTime: '15:00' },
  ],
}))

// 今日考勤
const attendanceData = ref<TodayAttendance | null>(null)
const attendanceLoading = ref(false)

async function fetchTodayAttendance() {
  attendanceLoading.value = true
  try {
    attendanceData.value = await attendanceApi.getTodayAttendance()
  } catch {
    // 静默失败，卡片显示空状态
    attendanceData.value = null
  } finally {
    attendanceLoading.value = false
  }
}

onMounted(() => {
  fetchTodayAttendance()
  // 新员工首次登录自动触发入职引导（仅未完成时）
  startOnboarding()
})

// 交错入场动画（三个独立区域）
const staggerGreeting = useStaggerAnimation({ staggerDelay: 0, duration: 350, type: 'fade-up' })
const staggerMain = useStaggerAnimation({ staggerDelay: 70, initialDelay: 100, duration: 400, type: 'fade-up' })
const staggerAnnounce = useStaggerAnimation({ staggerDelay: 80, initialDelay: 300, duration: 400, type: 'scale' })

// 权限判断
const hasStoreContext = computed(() => permission.hasStoreContext)
const isManager = computed(() => permission.isManager)

// 发起申请弹窗
const createApprovalVisible = ref(false)

/** 路由导航 */
function navigateTo(route: string) {
  if (route === '/approval?action=create') {
    createApprovalVisible.value = true
    return
  }
  router.push(route)
}
</script>

<template>
  <PageContainer :sticky-footer="false">
    <!-- 问候语区 -->
    <section ref="staggerGreeting.containerRef" class="greeting-zone">
      <GreetingSection
        :user-name="userName"
        :store-name="storeName"
        :schedule="hasStoreContext ? todaySchedule : null"
      />
    </section>

    <!-- 主内容区 -->
    <section ref="staggerMain.containerRef" class="main-cards">
      <QuickActionsGrid
        v-if="quickActions.length > 0"
        :actions="quickActions"
        @navigate="navigateTo"
      />

      <TodayAttendanceCard
        v-if="hasStoreContext"
        :data="attendanceData"
        :loading="attendanceLoading"
        @click="() => navigateTo('/attendance')"
      />

      <TodoSummaryCard
        :loading="false"
      />
    </section>

    <!-- 公告区 -->
    <section ref="staggerAnnounce.containerRef" class="announcement-zone">
      <AnnouncementList
        :announcements="announcements"
        :loading="false"
        @click-item="() => navigateTo('/messages?tab=notices')"
        @view-all="() => navigateTo('/messages?tab=notices')"
      />
    </section>
  </PageContainer>

  <!-- 新员工入职引导向导（Teleport to body） -->
  <OnboardingWizard />

  <!-- 发起申请弹窗（重构后的组件） -->
  <CreateApprovalDialog v-model="createApprovalVisible" />
</template>

<style scoped lang="scss">
.home-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

.greeting-zone {
}

.main-cards {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.announcement-zone {
}
</style>
