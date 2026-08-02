<script setup lang="ts">
/**
 * ProfilePage - 个人中心页面
 * 展示用户信息卡片、常用服务列表、主题切换、退出登录等功能
 * @last-modified 2026-06-02
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, ArrowRight, Sunny, SwitchButton,
  CircleCheck, Check, Lock, Phone, Monitor, Key,
  Document, Warning, Bell, Setting, View, ChatDotSquare,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePermissionStore } from '@/stores/permission'
import { useEmployeeStore } from '@/stores/employee'
import { PageContainer, StatCard } from '@/components/core'
import { useProfileData } from '@/composables/useProfileData'
import { useTheme } from '@/composables/useTheme'
import { useOnboarding } from '@/composables/useOnboarding'
import { useEmployeeLifecycle, type LifecycleTask } from '@/composables/useEmployeeLifecycle'
import { UserLevel } from '@/types/permission'
import { useStaggerAnimation } from '@/composables/useStaggerAnimation'

const router = useRouter()
const permission = usePermissionStore()
const employeeStore = useEmployeeStore()
const { currentMode, setTheme, isDark, THEME_MODE_LABELS } = useTheme()
const { start: startOnboarding } = useOnboarding()
const {
  tasksByPhase,
  onboardingProgress,
  pendingCount: lifecyclePendingCount,
  currentPhase,
} = useEmployeeLifecycle()

// 交错入场动画
const stagger = useStaggerAnimation({ staggerDelay: 40, duration: 400, type: 'fade-up' })

const userName = computed(() => permission.userInfo.name || '员工')
const userRole = computed(() => permission.getCurrentRoleLabel())
const userAvatar = computed(() => permission.userInfo.avatar)
const userPhone = computed(() => permission.userInfo.phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') || '')
const storeName = computed(() => permission.userInfo.storeName)

const { serviceItems, otherItems, handleServiceClick } = useProfileData()

/** 当前用户角色标识 */
const currentRoleKey = computed(() => permission.currentRole || 'staff')

/** 是否为管理层（店长/主管/人事/财务）— 复用 store 语义化属性 */
const isManager = computed(() => permission.isManager)

/** 是否为新员工（入职30天内）
 *  注意：当前 userInfo 无 joinDate 字段，待后端接口支持后可启用
 *  暂时通过 userLevel 判断：STAFF 层级且非管理岗视为新员工提示目标群体
 */
const isNewEmployee = computed(() => {
  // TODO: 后端接口返回 joinDate 后改为日期计算逻辑
  // const joinDate = permission.userInfo.joinDate
  // if (!joinDate) return false
  // const daysSinceJoin = Math.floor((Date.now() - new Date(joinDate).getTime()) / (1000 * 60 * 60 * 24))
  // return daysSinceJoin <= 30
  return permission.userLevel === UserLevel.STAFF && !permission.isManager
})

// ========== 统计数据（待对接后端API）==========
// 从 localStorage 读取考勤打卡记录计算出勤天数（无数据时显示"--"）
function calcAttendanceDays(): number | string {
  try {
    const raw = localStorage.getItem('fts_punch_records')
    if (!raw) return '--'
    const records = JSON.parse(raw)
    if (!Array.isArray(records)) return '--'
    // 统计本月有打卡记录的不重复日期数
    const dates = new Set<string>()
    const now = new Date()
    const yearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    for (const r of records) {
      if (r.punchTime && r.punchTime.startsWith(yearMonth)) {
        dates.add(r.punchTime.split(' ')[0])
      }
    }
    return dates.size || '--'
  } catch { return '--' }
}

/** 年假余额：待对接假期API */
const annualLeaveDays = computed(() => '--' as string)

/** 待办数量：根据角色返回不同提示（审批类/个人类）*/
const pendingCount = computed(() => permission.canApprove ? '--' : '--')

const attendanceDays = computed(() => calcAttendanceDays())

function handleEditProfile() {
  // 跳转到账号设置页面（资料编辑功能待后端API支持）
  router.push('/settings/account')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
    employeeStore.logout()
    router.push('/login')
  } catch {
    // 用户取消
  }
}

// ========== 字体大小控制 ==========
type FontSizeLevel = 'small' | 'medium' | 'large'

const FONT_SIZE_LABELS: Record<FontSizeLevel, string> = {
  small: '小',
  medium: '中（默认）',
  large: '大',
}

const FONT_SIZE_STORAGE_KEY = 'emp-font-size'
const currentFontSize = ref<FontSizeLevel>(
  (localStorage.getItem(FONT_SIZE_STORAGE_KEY) as FontSizeLevel) || 'medium'
)

function setFontSize(level: FontSizeLevel) {
  currentFontSize.value = level
  localStorage.setItem(FONT_SIZE_STORAGE_KEY, level)
  const root = document.documentElement
  root.classList.remove('font-size-small', 'font-size-medium', 'font-size-large')
  root.classList.add(`font-size-${level}`)
}

/** 点击行时循环切换字体大小 */
function cycleFontSize() {
  const order: FontSizeLevel[] = ['small', 'medium', 'large']
  const idx = order.indexOf(currentFontSize.value)
  setFontSize(order[(idx + 1) % order.length])
}

// 初始化字体大小
if (typeof window !== 'undefined') {
  document.documentElement.classList.add(`font-size-${currentFontSize.value}`)
}

// ========== 功能导航（点击跳转） ==========
/** 安全中心、申诉等统一导航到独立页面，此处仅做快捷入口 */
</script>

<template>
  <PageContainer title="我的">
    <div class="profile-page">
      <!-- 用户信息卡片 -->
      <section class="profile-card">
        <div class="profile-card__header">
          <div class="avatar-wrap">
            <img
              v-if="userAvatar"
              :src="userAvatar"
              :alt="userName"
              class="avatar-img"
            />
            <span v-else class="avatar-placeholder">
              <el-icon :size="28"><User /></el-icon>
            </span>
          </div>
          <div class="user-info">
            <h2 class="user-name">{{ userName }}</h2>
            <p class="user-role">{{ userRole }}</p>
            <p v-if="storeName" class="user-store">
              {{ storeName }}
            </p>
          </div>
        </div>
        <div class="profile-card__stats">
          <StatCard variant="grid" compact :value="attendanceDays" label="出勤(天)" />
          <StatCard variant="grid" compact :value="annualLeaveDays" label="年假(天)" />
          <StatCard variant="grid" compact :value="pendingCount" label="待办(项)" />
        </div>
        <div v-if="userPhone" class="profile-card__footer">
          <span>{{ userPhone }}</span>
          <a href="javascript:void(0)" class="edit-link" @click.prevent="handleEditProfile">编辑资料 →</a>
        </div>
      </section>

      <!-- 新员工引导提示 -->
      <section v-if="isNewEmployee" class="onboarding-banner">
        <div class="onboarding-banner__icon">
          <el-icon :size="22"><CircleCheck /></el-icon>
        </div>
        <div class="onboarding-banner__body">
          <span class="onboarding-banner__title">入职引导</span>
          <span class="onboarding-banner__desc">完成入职培训获取证书，解锁更多功能</span>
        </div>
        <button class="onboarding-banner__action" @click="startOnboarding(true)">去完成</button>
      </section>

      <!-- 入职/离职手续进度 -->
      <section v-if="currentPhase === 'onboarding' || currentPhase === 'offboarding'" class="lifecycle-section">
        <h3 class="section-title">
          {{ currentPhase === 'onboarding' ? '入职手续' : '离职交接' }}
        </h3>

        <!-- 进度条 -->
        <div class="lifecycle-progress">
          <div class="lifecycle-progress__bar">
            <div
              class="lifecycle-progress__fill"
              :style="{ width: `${onboardingProgress}%` }"
            />
          </div>
          <span class="lifecycle-progress__text">{{ onboardingProgress }}%</span>
        </div>

        <!-- 任务列表 -->
        <ul class="lifecycle-task-list">
          <li
            v-for="task in (currentPhase === 'onboarding' ? tasksByPhase.onboarding : tasksByPhase.offboarding)"
            :key="task.id"
            class="lifecycle-task"
            :class="[`lifecycle-task--${task.status}`]"
            @click="task.route && task.status !== 'completed' ? router.push(task.route) : undefined"
          >
            <span class="lifecycle-task__status">
              <el-icon v-if="task.status === 'completed'" :size="16"><Check /></el-icon>
              <span v-else-if="task.status === 'in_progress'" class="lifecycle-dot lifecycle-dot--pulse" />
              <span v-else class="lifecycle-dot" />
            </span>
            <div class="lifecycle-task__body">
              <span class="lifecycle-task__label">{{ task.title }}</span>
              <span class="lifecycle-task__desc">{{ task.description }}</span>
            </div>
            <el-icon v-if="task.route && task.status !== 'completed'" :size="14" class="lifecycle-task__arrow"><ArrowRight /></el-icon>
          </li>
        </ul>
      </section>

      <!-- 功能导航（点击即跳转到独立页面） -->
      <nav ref="stagger.containerRef" class="nav-section">
        <a href="javascript:void(0)" class="nav-item" @click.prevent="router.push('/appeals')">
          <span class="nav-icon nav-icon--appeal"><el-icon :size="18"><Warning /></el-icon></span>
          <span class="nav-body">
            <span class="nav-label">申诉与举报</span>
            <span class="nav-desc">处罚申诉 / 投诉举报</span>
          </span>
          <span class="nav-arrow">›</span>
        </a>
        <a
          v-for="item in serviceItems"
          :key="item.id"
          href="javascript:void(0)"
          class="nav-item"
          @click.prevent="handleServiceClick(item)"
        >
          <span class="nav-icon" :class="[`service-icon--${item.id}`]">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </span>
          <span class="nav-body">
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.desc" class="nav-desc">{{ item.desc }}</span>
          </span>
          <span class="nav-right">
            <span v-if="item.badge !== undefined" class="service-badge">{{ typeof item.badge === 'number' && item.badge > 99 ? '99+' : item.badge }}</span>
            <span class="nav-arrow">›</span>
          </span>
        </a>
        <a
          v-for="item in otherItems"
          :key="item.id"
          href="javascript:void(0)"
          class="nav-item"
          @click.prevent="handleServiceClick(item)"
        >
          <span class="nav-icon nav-icon--other"><el-icon :size="18"><component :is="item.icon" /></el-icon></span>
          <span class="nav-body">
            <span class="nav-label">{{ item.label }}</span>
          </span>
          <span class="nav-arrow">›</span>
        </a>

        <!-- 显示设置：外观主题 -->
        <a href="javascript:void(0)" class="nav-item" @click.prevent="setTheme(currentMode === 'light' ? 'dark' : currentMode === 'dark' ? 'light' : 'system')">
          <span class="nav-icon"><el-icon :size="18"><Sunny /></el-icon></span>
          <span class="nav-body">
            <span class="nav-label">外观主题</span>
            <span class="nav-desc">{{ THEME_MODE_LABELS[currentMode] }}</span>
          </span>
          <span class="nav-right">
            <div class="nav-inline-chips">
              <button
                v-for="mode in (['light', 'dark', 'system'] as const)"
                :key="mode"
                :class="['inline-chip', { 'inline-chip--active': currentMode === mode }]"
                @click.stop="setTheme(mode)"
              >{{ THEME_MODE_LABELS[mode] }}</button>
            </div>
            <span class="nav-arrow">›</span>
          </span>
        </a>

        <!-- 显示设置：字体大小 -->
        <a href="javascript:void(0)" class="nav-item" @click.prevent="cycleFontSize">
          <span class="nav-icon"><el-icon :size="18"><Setting /></el-icon></span>
          <span class="nav-body">
            <span class="nav-label">字体大小</span>
            <span class="nav-desc">{{ FONT_SIZE_LABELS[currentFontSize] }}</span>
          </span>
          <span class="nav-right">
            <div class="nav-inline-chips">
              <button
                v-for="level in (['small', 'medium', 'large'] as const)"
                :key="level"
                :class="['inline-chip', { 'inline-chip--active': currentFontSize === level }]"
                @click.stop="setFontSize(level)"
              >{{ FONT_SIZE_LABELS[level] }}</button>
            </div>
            <span class="nav-arrow">›</span>
          </span>
        </a>
      </nav>

      <!-- 退出登录 -->
      <section class="logout-section">
        <button class="logout-btn" type="button" @click="handleLogout">
          <el-icon :size="16"><SwitchButton /></el-icon>
          退出登录
        </button>
      </section>

      <!-- 版本信息 -->
      <footer class="profile-footer">
        <span>员工端 v1.0.0</span>
      </footer>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
/* ================================================================
 * ProfilePage 样式
 *
 * 响应式断点规范（项目统一）：
 *   768px  — 移动端通用布局切换（平板竖屏/手机横屏）
 *   480px  — 小屏手机网格/字体缩减
 * ================================================================ */

.profile-page {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);

  @media (max-width: 768px) {
    gap: var(--fts-space-section-gap-mobile);
  }
}

// ========== 用户卡片 ==========
.profile-card {
  background: var(--fts-gradient-primary);
  border-radius: var(--fts-radius-xl);
  padding: var(--fts-space-6) var(--fts-space-5);
  color: #fff; // 固定白色文字（渐变背景上）
  box-shadow: var(--fts-shadow-lg);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -30%;
    right: -10%;
    width: 220px;
    height: 220px;
    background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
  }

  @media (max-width: 768px) {
    padding: var(--fts-space-5) var(--fts-space-4);
    border-radius: var(--fts-radius-lg);
  }

  &__header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4);
    margin-bottom: var(--fts-space-5);
    position: relative;
    z-index: 1;
  }

  &__stats {
    display: flex;
    justify-content: space-around;
    padding-top: var(--fts-space-4);
    border-top: 1px solid rgba(255, 255, 255, 0.2); // 白色半透明分割线
    position: relative;
    z-index: 1;

    // 覆盖 StatCard 在渐变背景上的样式
    :deep(.stat-card--grid) {
      background: rgba(255, 255, 255, 0.12);
      border: 1px solid rgba(255, 255, 255, 0.15);

      .stat-value { color: #fff; }
      .stat-label { color: rgba(255, 255, 255, 0.8); }
      .stat-icon { color: rgba(255, 255, 255, 0.7); }
    }
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: var(--fts-font-size-xs);
    opacity: 0.8;
    border-top: 1px solid rgba(255, 255, 255, 0.2); // 白色半透明分割线
    padding-top: var(--fts-space-3);
    margin-top: var(--fts-space-3);
    position: relative;
    z-index: 1;
  }
}

.avatar-wrap {
  width: 64px;
  height: 64px;
  border-radius: var(--fts-radius-full);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--fts-overlay-medium);
  border: 2px solid var(--fts-overlay-heavy);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-overlay-heavy);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  line-height: 1.3;
  margin: 0 0 2px;
}

.user-role {
  font-size: var(--fts-font-size-sm);
  opacity: 0.85;
  margin: 0 0 2px;
}

.user-store {
  font-size: var(--fts-font-size-xs);
  opacity: 0.7;
  margin: 0;
}

.edit-link {
  color: var(--fts-text-on-primary);
  text-decoration: none;
  font-weight: 500;

  &:hover { opacity: 0.85; }
  &:active { opacity: 0.7; }
}

// ========== 通用区块标题 ==========
.section-title {
  font-size: var(--fts-font-size-md);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
}

// ========== 管理层专属区域 ==========
.manager-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);

  @media (max-width: 768px) {
    padding: var(--fts-space-3) var(--fts-space-4);
  }
}

.manager-grid {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.manager-card {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  width: 100%;
  padding: var(--fts-space-3) var(--fts-space-4);
  border: none;
  border-radius: var(--fts-radius-md);
  background: transparent;
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) ease;
  -webkit-tap-highlight-color: transparent;

  &:active {
    background-color: var(--fts-bg-page);
  }

  &:hover {
    background: var(--fts-bg-hover);
  }

  &__icon {
    width: 40px;
    height: 40px;
    border-radius: var(--fts-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__label {
    font-size: var(--fts-font-size-base);
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__desc {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__arrow {
    color: var(--fts-text-quaternary);
    flex-shrink: 0;
  }
}

// ========== 新员工入职引导 ==========
.onboarding-banner {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) var(--fts-space-5);
  border-radius: var(--fts-radius-lg);
  background: linear-gradient(135deg, rgba(var(--fts-warning-rgb), 0.08) 0%, rgba(var(--fts-primary-rgb), 0.06) 100%);
  border: 1px solid rgba(var(--fts-warning-rgb), 0.15);

  @media (max-width: 768px) {
    padding: var(--fts-space-3) var(--fts-space-4);
    gap: var(--fts-space-2);
  }

  &__icon {
    font-size: 24px;
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__title {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  &__desc {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__action {
    flex-shrink: 0;
    padding: var(--fts-space-2) var(--fts-space-4);
    border: none;
    border-radius: var(--fts-radius-full);
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);
    font-size: var(--fts-font-size-xs);
    font-weight: 600;
    cursor: pointer;
    transition: opacity var(--fts-duration-fast) ease;

    &:hover { opacity: 0.9; }
    &:active { opacity: 0.8; }
  }
}

// ========== 入职/离职手续进度 ==========
.lifecycle-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

.lifecycle-progress {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);

  &__bar {
    flex: 1;
    height: 6px;
    border-radius: 3px;
    background: var(--fts-bg-secondary);
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    border-radius: 3px;
    background: linear-gradient(90deg, var(--fts-primary), var(--fts-gradient-primary-deep));
    transition: width 0.4s ease;
  }

  &__text {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-primary);
    min-width: 36px;
    text-align: right;
  }
}

.lifecycle-task-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.lifecycle-task {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) 0;
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;
  -webkit-tap-highlight-color: transparent;

  &:not(:last-child) {
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &:hover { opacity: 0.85; }
  &:active { opacity: 0.7; }

  &--completed {
    .lifecycle-task__label,
    .lifecycle-task__desc { color: var(--fts-text-quaternary); }
    .lifecycle-dot { background: var(--fts-success); }
  }

  &--in_progress .lifecycle-dot {
    background: var(--fts-primary);
  }

  &__status {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: var(--fts-success);
  }

  &__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__label {
    font-size: var(--fts-font-size-base);
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__desc {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-tertiary);
  }

  &__arrow {
    color: var(--fts-text-quaternary);
    flex-shrink: 0;
  }
}

.lifecycle-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--fts-bg-tertiary);

  &--pulse {
    animation: lifecycle-pulse 1.5s infinite;
  }
}

@keyframes lifecycle-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}
// ========== 功能导航（点击跳转式） ==========
.nav-section {
  display: flex;
  flex-direction: column;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  overflow: hidden;
  box-shadow: var(--fts-shadow-xs);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  text-decoration: none;
  color: inherit;
  border-bottom: 1px solid var(--fts-border-secondary);
  transition: background-color var(--fts-duration-fast) ease;
  -webkit-tap-highlight-color: transparent;

  &:last-child { border-bottom: none; }
  &:hover { background-color: var(--fts-bg-tertiary); }
  &:active { background-color: var(--fts-bg-secondary); }
}

.nav-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--fts-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--fts-bg-secondary);
  color: var(--fts-text-secondary);

  &--security { background: rgba(var(--fts-warning-rgb), 0.10); color: var(--fts-warning); }
  &--appeal   { background: rgba(var(--fts-error-rgb), 0.08); color: var(--fts-error); }
  &--other    { background: rgba(var(--fts-info-rgb), 0.08); color: var(--fts-info); }
}

.nav-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-label {
  font-size: var(--fts-font-size-base);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.nav-desc {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
}

.nav-arrow {
  font-size: 18px;
  color: var(--fts-text-quaternary);
  line-height: 1;
}

// ========== 退出登录 ==========
.logout-section {
  padding: var(--fts-space-4) 0;
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1.5px solid var(--fts-error);
  border-radius: var(--fts-radius-md);
  background: transparent;
  color: var(--fts-error);
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;
  letter-spacing: 0.5px;

  &:hover {
    background: rgba(var(--fts-error-rgb), 0.06);
    border-color: var(--fts-error);
    box-shadow: 0 0 0 3px rgba(var(--fts-error-rgb), 0.12);
  }

  &:active {
    transform: scale(0.98);
    background: rgba(var(--fts-error-rgb), 0.10);
  }
}

// ========== 页脚 ==========
.profile-footer {
  text-align: center;
  padding: var(--fts-space-6) 0 var(--fts-space-10);
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-2xs);
}

// ========== 导航行内联操作芯片（主题切换/字体大小） ==========
.nav-inline-chips {
  display: flex;
  gap: var(--fts-space-1);
  flex-shrink: 0;
}

.inline-chip {
  padding: 2px 8px;
  font-size: var(--fts-font-size-2xs);
  font-weight: 500;
  color: var(--fts-text-tertiary);
  background: transparent;
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--active) {
    border-color: var(--fts-border-hover);
    color: var(--fts-text-secondary);
  }

  &--active {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.10);
    color: var(--fts-primary);
    font-weight: 600;
  }
}
</style>
