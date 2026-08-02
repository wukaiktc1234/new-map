<script setup lang="ts">
/**
 * AttendancePage - 我的考勤详情页（含打卡功能）
 *
 * 展示今日考勤状态 + 打卡按钮 + 本周出勤概览 + 异常处理入口。
 * 数据来源：attendanceApi（Mock阶段动态生成）
 *
 * 打卡状态机：
 *   not_clocked_in + canPunchIn  → 显示"上班打卡"按钮
 *   normal/late + canPunchOut   → 显示"下班打卡"按钮
 *   已完成(canPunchIn=false, canPunchOut=false) → 显示"今日完成"
 *   休息日                        → 按钮禁用
 */
import { ref, computed, onMounted } from 'vue'
import { Timer, WarningFilled, Calendar, ArrowRight, Location, Loading, Connection, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer, StatusTag, EmptyState, SectionHeader, StatCard } from '@/components/core'
import { attendanceApi } from '@/api/attendance'
import type { TodayAttendance, PunchResult, WiFiInfo, PunchConstraintMode } from '@/api/attendance'

// ==================== 数据 ====================
const loading = ref(false)
const todayData = ref<TodayAttendance | null>(null)

/** 打卡操作中 */
const isPunching = ref(false)
/** 定位状态 */
const locationStatus = ref<'idle' | 'locating' | 'success' | 'error'>('idle')
/** 距离门店距离（米） */
const distanceToStore = ref<number | null>(null)
/** 是否在围栏内 */
const withinFence = ref(true)
/** 最近一次打卡结果 */
const lastPunchResult = ref<PunchResult | null>(null)

/** 网络连接信息 */
const networkInfo = ref<WiFiInfo | null>(null)
/** 当前约束模式（后续从API获取） */
const constraintMode = ref<PunchConstraintMode>('location_only')
/** 是否显示定位引导卡片 */
const showLocationGuide = ref(false)

/** 本周模拟出勤数据（后续对接 scheduleApi） */
interface WeekDayRecord {
  date: string
  dayLabel: string
  status: 'normal' | 'late' | 'early_leave' | 'absent' | 'rest' | 'today'
  hours: number
}
const weekRecords = ref<WeekDayRecord[]>([])

async function fetchData() {
  loading.value = true
  try {
    todayData.value = await attendanceApi.getTodayAttendance()
    // 并行获取约束配置和网络状态
    const [config, network] = await Promise.all([
      attendanceApi.getPunchConfig(),
      attendanceApi.detectNetwork(),
    ])
    constraintMode.value = config.constraintMode
    networkInfo.value = network
    generateWeekRecords()
  } finally {
    loading.value = false
  }
}

function generateWeekRecords() {
  const now = new Date()
  const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const records: WeekDayRecord[] = []

  // 获取本周一的日期
  const dayOfWeek = now.getDay()
  const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  const monday = new Date(now)
  monday.setDate(now.getDate() + diffToMonday)

  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const isToday = d.toDateString() === now.toDateString()
    const isWeekend = d.getDay() === 0 || d.getDay() === 6

    let status: WeekDayRecord['status'] = 'normal'
    let hours = 8

    if (isWeekend) {
      status = 'rest'
      hours = 0
    } else if (isToday && todayData.value) {
      // 根据今日实际状态
      const s = todayData.value.status
      if (s === 'not_clocked_in' && todayData.value.expectedHours === 0) {
        status = 'rest'
        hours = 0
      } else if (s === 'not_clocked_in') {
        status = 'today' // 未上班
        hours = 0
      } else if (s === 'late') {
        status = 'late'
        hours = todayData.value.currentHours
      } else if (s === 'early_leave') {
        status = 'early_leave'
        hours = todayData.value.currentHours
      } else if (s === 'absent') {
        status = 'absent'
        hours = 0
      } else {
        status = 'today'
        hours = todayData.value.currentHours
      }
    } else if (d > now) {
      // 未来日期
      status = 'normal'
      hours = 0
    } else {
      // 过去工作日（模拟正常出勤）
      status = 'normal'
      hours = 7.5 + Math.random() * 2
    }

    records.push({
      date: `${d.getMonth() + 1}/${d.getDate()}`,
      dayLabel: dayNames[d.getDay()],
      status,
      hours: Math.round(hours * 10) / 10,
    })
  }

  weekRecords.value = records
}

// ==================== 计算属性 ====================

/** 状态中文描述 */
const statusText = computed(() => {
  if (!todayData.value) return ''
  const map: Record<string, string> = {
    normal: '正常出勤',
    late: '迟到',
    early_leave: '早退',
    absent: '缺勤',
    not_clocked_in: todayData.value.expectedHours === 0 ? '今日休息' : '未打卡',
    overtime: '加班中',
  }
  return map[todayData.value.status] || '未知'
})

/** 当前打卡按钮状态 */
interface PunchButtonState {
  visible: boolean
  label: string
  type: 'in' | 'out' | 'done' | 'disabled'
  disabled: boolean
}

const punchButtonState = computed<PunchButtonState>(() => {
  if (!todayData.value) return { visible: false, label: '', type: 'in' as const, disabled: true }

  const { canPunchIn, canPunchOut, expectedHours } = todayData.value

  // 休息日
  if (expectedHours === 0) {
    return { visible: true, label: '今日休息', type: 'disabled' as const, disabled: true }
  }

  // 检查约束条件是否满足
  const locationOk = locationStatus.value !== 'error'
  const allConstraintsOk = locationOk && networkStatusOk.value

  // 可以上班打卡
  if (canPunchIn) {
    if (!allConstraintsOk) {
      // 约束不满足：显示原因并禁用按钮
      const reasons: string[] = []
      if (!locationOk) reasons.push('定位服务未开启')
      if (!networkStatusOk.value) reasons.push(networkText.value || '网络不满足要求')
      return {
        visible: true,
        label: reasons.join(' / '),
        type: 'in',
        disabled: true,
      }
    }
    return { visible: true, label: '上班打卡', type: 'in', disabled: isPunching.value }
  }

  // 可以下班打卡
  if (canPunchOut) {
    if (!allConstraintsOk) {
      const reasons: string[] = []
      if (!locationOk) reasons.push('定位服务未开启')
      if (!networkStatusOk.value) reasons.push(networkText.value || '网络不满足要求')
      return {
        visible: true,
        label: reasons.join(' / '),
        type: 'out',
        disabled: true,
      }
    }
    return { visible: true, label: '下班打卡', type: 'out', disabled: isPunching.value }
  }

  // 今日完成
  return { visible: true, label: '今日已完成', type: 'done' as const, disabled: true }
})

/** 定位描述文本 */
const locationText = computed(() => {
  switch (locationStatus.value) {
    case 'locating': return '正在获取位置...'
    case 'success':
      if (distanceToStore.value !== null) {
        return withinFence.value
          ? `距门店 ${distanceToStore.value}m ✓`
          : `距门店 ${distanceToStore.value}m（超出范围）`
      }
      return '已定位'
    case 'error': return '定位不可用'
    default: return ''
  }
})

/** 网络连接描述文本 */
const networkText = computed(() => {
  if (!networkInfo.value) return ''

  const info = networkInfo.value
  if (!info.isConnected) {
    return '未连接网络'
  }

  // 网络类型中文映射
  const typeMap: Record<string, string> = {
    wifi: 'WiFi',
    cellular: '移动数据',
    ethernet: '有线网络',
    unknown: '未知网络',
    offline: '离线',
  }

  const typeName = typeMap[info.connectionType] || info.connectionType

  // 根据约束模式判断是否满足要求
  const needsWifi = constraintMode.value === 'wifi_only' || constraintMode.value === 'both'
  const isWifiAllowed = info.connectionType === 'wifi' || info.connectionType === 'ethernet'

  if (needsWifi && !isWifiAllowed) {
    return `${typeName}（非门店网络）`
  }

  return typeName
})

/** 网络状态是否正常（根据约束模式判断） */
const networkStatusOk = computed(() => {
  if (!networkInfo.value) return true // 未检测到时默认通过

  const mode = constraintMode.value
  // 不需要网络约束的模式直接通过
  if (mode === 'location_only' || mode === 'none') return true

  const info = networkInfo.value
  if (!info.isConnected) return false

  // wifi_only / both 模式需要WiFi或有线网络
  if (mode === 'wifi_only' || mode === 'both') {
    return info.connectionType === 'wifi' || info.connectionType === 'ethernet'
  }

  return true
})

/** 本周累计工时 */
const weekTotalHours = computed(() =>
  weekRecords.value.reduce((sum, r) => sum + r.hours, 0),
)

/** 本周出勤天数 */
const weekWorkDays = computed(() =>
  weekRecords.value.filter(r => r.status !== 'rest' && r.hours > 0).length,
)

// ==================== 方法 ====================

/**
 * 执行打卡操作
 * 流程：约束预检 → 定位 → 调用API → 更新数据 → 反馈结果
 */
async function handlePunch(type: 'in' | 'out') {
  if (isPunching.value || !todayData.value) return

  isPunching.value = true
  locationStatus.value = 'locating'

  try {
    // 调用带约束检查的打卡方法
    const result = await attendanceApi.punch(type, constraintMode.value)

    // 处理约束拒绝
    if (!result.success) {
      locationStatus.value = result.locationSatisfied === false ? 'error' : 'success'

      // 如果是定位问题，显示引导卡片
      if (result.locationSatisfied === false) {
        showLocationGuide.value = true
      }

      ElMessage.warning(result.message || '打卡条件不满足')
      return
    }

    // 打卡成功：更新状态
    locationStatus.value = result.withinFence ? 'success' : 'error'
    distanceToStore.value = result.distanceToStore ?? null
    withinFence.value = result.withinFence
    lastPunchResult.value = result

    // 刷新考勤数据
    await fetchData()

    ElMessage.success(result.message)
  } catch (err) {
    locationStatus.value = 'error'
    const msg = err instanceof Error ? err.message : '打卡失败，请重试'
    ElMessage.error(msg)
  } finally {
    isPunching.value = false
  }
}

/** 跳转到系统定位设置（通过提示引导用户手动开启） */
function openLocationSettings() {
  showLocationGuide.value = true
}

function handleExceptionAction() {
  // 后续可跳转到异常处理页面或弹出表单
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <PageContainer title="我的考勤" :loading="loading">
    <!-- 今日考勤卡片（大版 + 打卡按钮） -->
    <section v-if="todayData" class="page-section">
      <div class="att-today-card">
        <!-- 顶部：状态 + 班次 -->
        <div class="att-top-row">
          <div class="att-status-block">
            <StatusTag
              :status="todayData.status === 'not_clocked_in' && todayData.expectedHours === 0 ? 'default' : todayData.status === 'absent' ? 'error' : todayData.status === 'late' || todayData.status === 'early_leave' ? 'warning' : 'active'"
              size="small"
              variant="light"
            />
            <span class="att-status-text">{{ statusText }}</span>
          </div>
          <span class="att-shift-name">{{ todayData.shiftName }}</span>
        </div>

        <!-- 打卡按钮（核心交互区） -->
        <div v-if="punchButtonState.visible" class="att-punch-area">
          <!-- 约束状态指示区域 -->
          <div class="att-constraint-status">
            <!-- 定位状态 -->
            <div
              class="att-constraint-item"
              :class="{
                'att-constraint-item--ok': locationStatus === 'success',
                'att-constraint-item--error': locationStatus === 'error',
                'att-constraint-item--loading': locationStatus === 'locating',
              }"
              @click="locationStatus === 'error' && openLocationSettings()"
            >
              <el-icon :size="12"><Location /></el-icon>
              <span>{{ locationStatus === 'idle' ? '定位服务：待检测' : `定位服务：${locationText}` }}</span>
              <el-icon v-if="locationStatus === 'error'" :size="12" class="att-constraint-action"><Setting /></el-icon>
            </div>

            <!-- 网络状态（约束模式需要时显示） -->
            <div
              v-if="constraintMode !== 'location_only' && constraintMode !== 'none'"
              class="att-constraint-item"
              :class="{
                'att-constraint-item--ok': networkStatusOk,
                'att-constraint-item--error': !networkStatusOk && networkInfo !== null,
              }"
            >
              <el-icon :size="12"><Connection /></el-icon>
              <span>网络连接：{{ networkInfo ? networkText : '检测中...' }}</span>
            </div>
          </div>

          <!-- 定位引导卡片 -->
          <div v-if="showLocationGuide" class="att-location-guide">
            <div class="att-location-guide__icon"><el-icon :size="24"><Location /></el-icon></div>
            <div class="att-location-guide__content">
              <h4 class="att-location-guide__title">打卡需要使用定位服务</h4>
              <p class="att-location-guide__desc">请开启设备定位功能后重试。部分浏览器需要在设置中允许位置访问权限。</p>
            </div>
            <button class="att-location-guide__close" @click="showLocationGuide = false">知道了</button>
          </div>

          <!-- 打卡按钮 -->
          <button
            class="att-punch-btn"
            :class="{
              'att-punch-btn--in': punchButtonState.type === 'in',
              'att-punch-btn--out': punchButtonState.type === 'out',
              'att-punch-btn--done': punchButtonState.type === 'done',
              'att-punch-btn--disabled': punchButtonState.disabled,
              'att-punch-btn--loading': isPunching,
            }"
            :disabled="punchButtonState.disabled"
            @click="punchButtonState.type === 'in' || punchButtonState.type === 'out' ? handlePunch(punchButtonState.type) : undefined"
          >
            <template v-if="isPunching">
              <el-icon class="att-punch-loading"><Loading /></el-icon>
              <span>打卡中...</span>
            </template>
            <template v-else>
              <span class="att-punch-label">{{ punchButtonState.label }}</span>
            </template>
          </button>

          <!-- 最近打卡时间反馈 -->
          <p v-if="lastPunchResult" class="att-punch-feedback">
            {{ lastPunchResult.punchTime }} · {{ lastPunchResult.message }}
          </p>
        </div>

        <!-- 打卡时间 -->
        <div class="att-time-row">
          <div class="att-time-cell">
            <span class="att-time-cell__label">上班打卡</span>
            <span class="att-time-cell__value" :class="{ 'att-time-cell__value--muted': !todayData.clockInTime }">
              {{ todayData.clockInTime || '--:--' }}
            </span>
          </div>
          <div class="att-time-divider"></div>
          <div class="att-time-cell">
            <span class="att-time-cell__label">下班打卡</span>
            <span class="att-time-cell__value" :class="{ 'att-time-cell__value--muted': !todayData.clockOutTime }">
              {{ todayData.clockOutTime || (todayData.status === 'normal' ? '未下班' : '--:--') }}
            </span>
          </div>
          <div class="att-time-divider"></div>
          <div class="att-time-cell">
            <span class="att-time-cell__label">已工作</span>
            <span class="att-time-cell__value att-time-cell__value--highlight">
              {{ todayData.currentHours.toFixed(1) }}h
            </span>
          </div>
        </div>

        <!-- 工时进度条 -->
        <div v-if="todayData.expectedHours > 0" class="att-progress-row">
          <span class="att-progress-label">今日工时</span>
          <div class="att-progress-bar">
            <div
              class="att-progress-fill"
              :style="{ width: Math.min(100, (todayData.currentHours / todayData.expectedHours) * 100) + '%' }"
              :class="{
                'att-progress-fill--success': todayData.status === 'normal',
                'att-progress-fill--warning': todayData.status === 'late' || todayData.status === 'early_leave',
                'att-progress-fill--error': todayData.status === 'absent',
              }"
            ></div>
          </div>
          <span class="att-progress-text">{{ todayData.currentHours.toFixed(1) }} / {{ todayData.expectedHours }}h</span>
        </div>

        <!-- 异常提示 -->
        <div v-if="todayData.exceptionType" class="att-exception-bar">
          <el-icon :size="14" color="var(--fts-warning)"><WarningFilled /></el-icon>
          <span>{{ todayData.exceptionType }}</span>
          <button class="att-exception-btn" @click="handleExceptionAction">去处理</button>
        </div>
      </div>
    </section>

    <!-- 无数据状态 -->
    <section v-else-if="!loading" class="page-section">
      <EmptyState
        title="暂无考勤数据"
        description="今日可能为休息日，或数据尚未同步"
      />
    </section>

    <!-- 本周概览 -->
    <section class="page-section">
      <SectionHeader title="本周出勤概览" icon="📅" />

      <!-- 周统计 -->
      <div class="week-stats-grid">
        <StatCard variant="grid" compact :value="weekWorkDays" label="出勤天" />
        <StatCard variant="grid" compact :value="weekTotalHours.toFixed(1)" label="累计工时" />
        <StatCard variant="grid" compact :value="`${todayData?.expectedHours || 8}h`" label="日均目标" />
      </div>

      <!-- 每日格子 -->
      <div class="week-grid">
        <div
          v-for="(day, idx) in weekRecords"
          :key="idx"
          class="week-day"
          :class="`week-day--${day.status}`"
        >
          <span class="week-day__label">{{ day.dayLabel }}</span>
          <span class="week-day__date">{{ day.date }}</span>
          <span v-if="day.hours > 0" class="week-day__hours">{{ day.hours }}h</span>
          <span v-else-if="day.status === 'rest'" class="week-day__hours week-day__hours--rest">休息</span>
          <span v-else class="week-day__hours week-day__hours--empty">-</span>
        </div>
      </div>
    </section>

    <!-- 底部快捷入口 -->
    <section class="page-section">
      <router-link to="/schedule" class="att-link-item">
        <el-icon :size="18"><Calendar /></el-icon>
        <span>查看排班详情</span>
        <el-icon :size="12"><ArrowRight /></el-icon>
      </router-link>
      <router-link to="/leave" class="att-link-item">
        <span class="att-link-icon att-link-icon--leave">假</span>
        <span>请假 / 调休</span>
        <el-icon :size="12"><ArrowRight /></el-icon>
      </router-link>
    </section>
  </PageContainer>
</template>

<style scoped lang="scss">
/* ── 标准页面区块（与 TrainingPage/NoticesPage 一致） ── */
.page-section {
  margin-bottom: var(--fts-space-section-gap, 20px);

  &:last-child {
    margin-bottom: 0;
  }
}

/* ── 今日考勤卡片（增强视觉层次） ── */

.att-today-card {
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  border: 1px solid var(--fts-border-primary);
  padding: var(--fts-space-5) var(--fts-space-5, 20px);

  /* 微妙渐变顶部高光 */
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg,
      transparent 0%,
      rgba(var(--fts-primary-rgb), 0.4) 30%,
      rgba(var(--fts-primary-rgb), 0.6) 50%,
      rgba(var(--fts-primary-rgb), 0.4) 70%,
      transparent 100%
    );
  }
}

.att-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fts-space-4);
}

.att-status-block {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.att-status-text {
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
}

.att-shift-name {
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-secondary);
  padding: var(--fts-space-1, 4px) var(--fts-space-2, 8px);
  background-color: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-full, 999px);
}

.att-time-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.att-time-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;

  &__label {
    font-size: var(--fts-font-size-xs, 12px);
    color: var(--fts-text-secondary);
  }

  &__value {
    font-size: var(--fts-font-size-xl, 20px);
    font-weight: 700;
    color: var(--fts-text-primary);
    font-variant-numeric: tabular-nums;

    &--muted {
      color: var(--fts-text-secondary);
      font-weight: var(--fts-font-weight-medium, 500);
    }

    &--highlight {
      color: var(--fts-primary);
    }
  }
}

.att-time-divider {
  width: 1px;
  height: 36px;
  background-color: var(--fts-border-secondary);
  flex-shrink: 0;
}

.att-progress-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
}

.att-progress-label {
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-secondary);
  white-space: nowrap;
}

.att-progress-bar {
  flex: 1;
  height: 6px;
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full, 999px);
  overflow: hidden;
}

.att-progress-fill {
  height: 100%;
  border-radius: var(--fts-radius-full, 999px);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);

  &--success { background-color: var(--fts-success); }
  &--warning { background-color: var(--fts-warning); }
  &--error   { background-color: var(--fts-error); }
}

.att-progress-text {
  font-size: var(--fts-font-size-base, 14px);
  font-weight: 600;
  color: var(--fts-text-primary);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.att-exception-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-2, 8px) var(--fts-space-3, 12px);
  background-color: rgba(var(--fts-warning-rgb), 0.08);
  border-radius: var(--fts-radius-md, 6px);
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-warning);
  font-weight: 500;
}

.att-exception-btn {
  margin-left: auto;
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 600;
  color: var(--fts-warning);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-sm, 4px);

  &:hover { background-color: rgba(var(--fts-warning-rgb), 0.15); }
}

/* ── 打卡按钮区域（核心交互） ── */

.att-punch-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2, 8px);
  padding: var(--fts-space-4, 16px) 0;
}

/* ── 约束状态指示区域 ── */
.att-constraint-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-bottom: var(--fts-space-1, 4px);
}

.att-constraint-item {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  font-size: var(--fts-font-size-xs, 12px);
  padding: var(--fts-space-1, 4px) var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-full, 999px);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &--ok {
    color: var(--fts-success);
    background-color: rgba(var(--fts-success-rgb), 0.06);
  }

  &--error {
    color: var(--fts-error);
    background-color: rgba(var(--fts-error-rgb), 0.06);
    cursor: pointer;

    &:hover {
      background-color: rgba(var(--fts-error-rgb), 0.12);
    }
  }

  &--loading {
    color: var(--fts-text-secondary);
    background-color: var(--fts-bg-secondary);
  }
}

.att-constraint-action {
  margin-left: 2px;
  opacity: 0.7;
}

/* ── 定位引导卡片 ── */
.att-location-guide {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  width: 100%;
  padding: var(--fts-space-3, 12px) var(--fts-space-3, 12px);
  background-color: rgba(var(--fts-info-rgb), 0.06);
  border: 1px solid rgba(var(--fts-info-rgb), 0.2);
  border-radius: var(--fts-radius-md, 8px);
  margin-bottom: var(--fts-space-2, 8px);

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--fts-radius-md);
    background-color: rgba(var(--fts-info-rgb), 0.1);
    color: var(--fts-info);
    flex-shrink: 0;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: var(--fts-font-size-sm, 13px);
    font-weight: var(--fts-font-weight-semibold, 600);
    color: var(--fts-text-primary);
    margin: 0 0 4px;
  }

  &__desc {
    font-size: var(--fts-font-size-xs, 12px);
    color: var(--fts-text-secondary);
    margin: 0;
    line-height: 1.5;
  }

  &__close {
    flex-shrink: 0;
    font-size: var(--fts-font-size-xs, 12px);
    font-weight: 600;
    color: var(--fts-primary);
    background: none;
    border: none;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: var(--fts-radius-sm, 4px);
    white-space: nowrap;

    &:hover {
      background-color: rgba(var(--fts-primary-rgb), 0.08);
    }
  }
}

.att-location-hint {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-success);
  padding: var(--fts-space-1, 4px) var(--fts-space-2, 8px);
  background-color: rgba(var(--fts-success-rgb), 0.06);
  border-radius: var(--fts-radius-full, 999px);

  &--error {
    color: var(--fts-warning);
    background-color: rgba(var(--fts-warning-rgb), 0.08);
  }
}

.att-punch-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2, 8px);
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  /* 外圈脉冲动画 */
  &::before {
    content: '';
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 2px solid transparent;
    opacity: 0;
    transition: opacity 0.3s;
  }

  /* 上班打卡：主色 */
  &--in {
    background: linear-gradient(135deg, var(--fts-primary), color-mix(in srgb, var(--fts-primary) 85%, white));
    color: var(--fts-text-on-primary);

    &::before {
      border-color: rgba(var(--fts-primary-rgb), 0.3);
      animation: punch-pulse 2s ease-out infinite;
    }

    &:active:not(:disabled) {
      transform: scale(0.95);
    }
  }

  /* 下班打卡：成功色 */
  &--out {
    background: linear-gradient(135deg, var(--fts-success), color-mix(in srgb, var(--fts-success) 85%, white));
    color: var(--fts-text-on-primary);

    &::before {
      border-color: rgba(var(--fts-success-rgb), 0.3);
      animation: punch-pulse 2s ease-out infinite;
    }

    &:active:not(:disabled) {
      transform: scale(0.95);
    }
  }

  /* 已完成：灰色 */
  &--done {
    background-color: var(--fts-bg-secondary);
    color: var(--fts-text-tertiary);
    cursor: default;
  }

  /* 禁用状态（休息日等） */
  &--disabled {
    background-color: var(--fts-bg-tertiary);
    color: var(--fts-text-quaternary);
    cursor: not-allowed;
  }

  /* 加载中 */
  &--loading {
    cursor: wait;
    opacity: 0.9;
  }
}

.att-punch-label {
  letter-spacing: 1px;
}

.att-punch-loading {
  animation: spin 1s linear infinite;
}

@keyframes punch-pulse {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.25); opacity: 0; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.att-punch-feedback {
  margin: 0;
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
  text-align: center;
}

.week-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
}

.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--fts-space-2);
}

.week-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: var(--fts-space-2, 8px) var(--fts-space-1, 4px);
  border-radius: var(--fts-radius-md);
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &--today {
    border-color: var(--fts-primary);
    background-color: rgba(var(--fts-primary-rgb), 0.06);
    box-shadow: 0 0 0 1px rgba(var(--fts-primary-rgb), 0.15);
  }

  &--rest {
    opacity: 0.7;
  }

  &--late, &--early_leave {
    background-color: rgba(var(--fts-warning-rgb), 0.06);
    border-color: var(--fts-warning-light);
  }

  &--absent {
    background-color: rgba(var(--fts-error-rgb), 0.06);
    border-color: var(--fts-error-light);
  }
}

.week-day__label {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-secondary);
}

.week-day__date {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
}

.week-day__hours {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-success);
  font-weight: 600;

  &--rest {
    color: var(--fts-text-secondary);
    font-weight: 500;
  }

  &--empty {
    color: var(--fts-bg-tertiary);
  }
}

/* ── 底部链接 ── */
.att-link-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3, 12px) var(--fts-space-4, 16px);
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  text-decoration: none;
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);

  .el-icon:last-child {
    margin-left: auto;
    color: var(--fts-text-quaternary);
  }
}

.att-link-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--fts-radius-md, 6px);
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 700;

  &--leave {
    background-color: rgba(var(--fts-info-rgb), 0.1);
    color: var(--fts-info);
  }
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .att-time-cell__value {
    font-size: var(--fts-font-size-lg, 17px);
  }

  .week-grid {
    gap: var(--fts-space-1);
  }

  .week-day {
    padding: var(--fts-space-1, 4px) 2px;
  }

  .week-day__label { font-size: var(--fts-font-size-xs); }
  .week-day__date { font-size: var(--fts-font-size-sm); }
  .week-day__hours { font-size: var(--fts-font-size-xs); }
}
</style>
