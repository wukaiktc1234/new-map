<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  Sunny, Moon, Monitor,
  CaretBottom, User, HomeFilled,
} from '@element-plus/icons-vue'
import { useEmployeeStore } from '@/stores/employee'
import { usePermissionStore } from '@/stores/permission'
import { useTheme } from '@/composables/useTheme'

interface BreadcrumbItem {
  label: string
  path?: string
}

interface Props {
  breadcrumbs: BreadcrumbItem[]
  pageTitle: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'openMobileSidebar'): void
  (e: 'userAction', action: string): void
}>()

const router = useRouter()
const employeeStore = useEmployeeStore()
const permission = usePermissionStore()

// ========== 主题切换（三选一：system / light / dark）—— 使用统一的 useTheme composable ==========
const { currentMode: themeMode, setTheme, isDark } = useTheme()
const showThemePopover = ref(false)
const themeSelectorRef = ref<HTMLElement | null>(null)

function selectTheme(mode: 'system' | 'light' | 'dark') {
  setTheme(mode)
  showThemePopover.value = false
}

/* 点击外部关闭主题面板 */
function onDocumentClick(e: Event) {
  if (showThemePopover.value && themeSelectorRef.value && !themeSelectorRef.value.contains(e.target as Node)) {
    showThemePopover.value = false
  }
}

onMounted(() => { document.addEventListener('click', onDocumentClick) })
onBeforeUnmount(() => { document.removeEventListener('click', onDocumentClick) })

const themeOptions = [
  { value: 'system' as const, label: '跟随系统', icon: Monitor },
  { value: 'light' as const, label: '浅色模式', icon: Sunny },
  { value: 'dark' as const, label: '深色模式', icon: Moon },
]

// ========== 字体大小控制 ==========
type FontSizeLevel = 'small' | 'medium' | 'large' | 'xlarge'
const fontSizeLevel = ref<FontSizeLevel>((localStorage.getItem('emp-font-size') as FontSizeLevel) || 'medium')

const fontScaleMap: Record<FontSizeLevel, number> = {
  small: 0.875,
  medium: 1,
  large: 1.125,
  xlarge: 1.25,
}

function setFontSize(level: FontSizeLevel) {
  fontSizeLevel.value = level
  localStorage.setItem('emp-font-size', level)
  document.documentElement.style.fontSize = `${fontScaleMap[level] * 16}px`
}

setFontSize(fontSizeLevel.value)

// ========== 用户下拉菜单 ==========
const showUserDropdown = ref(false)
const userDropdownRef = ref<HTMLElement | null>(null)

function toggleUserDropdown() {
  showUserDropdown.value = !showUserDropdown.value
  if (showUserDropdown.value) showThemePopover.value = false
}

function handleUserAction(action: string) {
  showUserDropdown.value = false
  emit('userAction', action)
}

/**
 * 开发环境：切换角色（调试用）
 * 切换后刷新页面以重新初始化路由和权限
 */
function switchRole(role: import('@/stores/permission').EmployeeRole) {
  permission.setRole(role)
  showUserDropdown.value = false
  window.location.reload()
}

function onDocumentClickUser(e: Event) {
  if (showUserDropdown.value && userDropdownRef.value && !userDropdownRef.value.contains(e.target as Node)) {
    showUserDropdown.value = false
  }
}

onMounted(() => { document.addEventListener('click', onDocumentClickUser) })
onBeforeUnmount(() => { document.removeEventListener('click', onDocumentClickUser) })

// ========== 角色与权限数据（从权限Store获取） ==========
const isDev = import.meta.env.DEV
const allRoles = computed(() => permission.getAllRoles())
</script>

<template>
  <header class="el-topbar">
    <!-- 左侧：移动端菜单按钮 + 品牌区 -->
    <div class="el-topbar__left">
      <!-- 移动端汉堡菜单 -->
      <button
        class="el-mobile-menu-btn"
        @click="emit('openMobileSidebar')"
        title="打开菜单"
      >
        <span class="hamburger-line"></span>
        <span class="hamburger-line"></span>
        <span class="hamburger-line"></span>
      </button>

      <div class="el-topbar__brand">
        <el-icon :size="16" class="el-topbar__brand-icon"><HomeFilled /></el-icon>
        <span class="el-topbar__brand-text">员工自助门户</span>
      </div>
    </div>

    <!-- 中间面包屑导航 -->
    <div class="el-topbar__center">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="(crumb, index) in breadcrumbs"
          :key="index"
        >{{ crumb.label }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧工具区 -->
    <div class="el-topbar__right">


      <!-- 字体大小切换 -->
      <div class="el-font-control">
        <button
          class="el-font-btn"
          :class="{ 'el-font-btn--active': fontSizeLevel === 'small' }"
          @click="setFontSize('small')"
          title="小号字体"
        >A-</button>
        <button
          class="el-font-btn"
          :class="{ 'el-font-btn--active': fontSizeLevel === 'medium' }"
          @click="setFontSize('medium')"
          title="中号字体"
        >A</button>
        <button
          class="el-font-btn"
          :class="{ 'el-font-btn--active': fontSizeLevel === 'large' }"
          @click="setFontSize('large')"
          title="大号字体"
        >A+</button>
      </div>

      <!-- 主题切换触发器 -->
      <div class="el-theme-selector" ref="themeSelectorRef">
        <button class="el-theme-trigger" @click.stop="showThemePopover = !showThemePopover">
          <el-icon :size="16">
            <Monitor v-if="themeMode === 'system'" />
            <Sunny v-else-if="themeMode === 'light'" />
            <Moon v-else />
          </el-icon>
        </button>

        <transition name="popover-fade">
          <div v-if="showThemePopover" class="el-theme-popover" @click.stop>
            <div class="el-popover-title">外观模式</div>
            <button
              v-for="opt in themeOptions"
              :key="opt.value"
              :class="['el-theme-option', { 'el-theme-option--active': themeMode === opt.value }]"
              @click="selectTheme(opt.value)"
            >
              <el-icon :size="18"><component :is="opt.icon" /></el-icon>
              <span class="el-option-label">{{ opt.label }}</span>
              <span v-if="themeMode === opt.value" class="el-option-check">&#10003;</span>
            </button>
          </div>
        </transition>
      </div>

      <!-- 用户头像 + 下拉菜单 -->
      <div class="el-user-area" ref="userDropdownRef">
        <button class="el-avatar-trigger" @click.stop="toggleUserDropdown">
          <div class="el-avatar-circle">
            <el-icon :size="16"><User /></el-icon>
          </div>
          <el-icon
            :size="10"
            class="el-avatar-arrow"
            :class="{ 'el-avatar-arrow--open': showUserDropdown }"
          ><CaretBottom /></el-icon>
        </button>

        <transition name="popover-fade">
          <div v-if="showUserDropdown" class="el-user-dropdown" @click.stop>
            <!-- 用户信息：从权限Store获取 -->
            <div class="el-dropdown-header">
              <span class="el-dropdown-name">{{ permission.userInfo.name || '员工' }}</span>
              <span class="el-dropdown-dept">{{ permission.userInfo.storeName }}</span>
            </div>
            <div class="el-dropdown-divider" />
            <button class="el-dropdown-item" @click="handleUserAction('profile')">
              <el-icon :size="16"><User /></el-icon>
              <span>个人中心</span>
            </button>
            <button class="el-dropdown-item" @click="handleUserAction('settings')">
              <el-icon :size="16"><Monitor /></el-icon>
              <span>账户设置</span>
            </button>

            <!-- 开发环境：角色切换器（千人千面调试工具） -->
            <template v-if="isDev">
              <div class="el-dropdown-divider" />
              <div class="el-dropdown-section-title">切换视角</div>
              <button
                v-for="role in allRoles"
                :key="role.value"
                :class="[
                  'el-dropdown-item',
                  'el-dropdown-item--role',
                  { 'el-dropdown-item--role-active': permission.currentRole === role.value }
                ]"
                @click="switchRole(role.value)"
              >
                <span class="el-role-dot" :style="{ background: role.value === permission.currentRole ? 'var(--fts-primary)' : 'var(--fts-border-primary)' }" />
                <span>{{ role.label }}</span>
                <span v-if="permission.currentRole === role.value" class="el-role-current-tag">当前</span>
              </button>
            </template>

            <div class="el-dropdown-divider" />
            <button class="el-dropdown-item el-dropdown-item--danger" @click="handleUserAction('logout')">
              <span class="el-logout-icon">&#8634;</span>
              <span>退出登录</span>
            </button>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
// ================================================================
//  PC 端顶栏（飞书式 + MAC毛玻璃）
// ================================================================

.el-topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: none;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 24px;

  /* MAC毛玻璃效果 */
  background: var(--fts-bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--fts-border-glass);
  flex-shrink: 0;
  gap: 16px;

  /* 仅 PC 端显示 */
  @media (min-width: 768px) {
    display: flex;
  }
}

/* ---- 左侧区域（移动端菜单 + 品牌） ---- */
.el-topbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 移动端汉堡菜单按钮 */
.el-mobile-menu-btn {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 32px;
  height: 32px;
  padding: 6px;
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;

  @media (max-width: 767px) {
    display: flex;
  }

  &:hover {
    background: var(--fts-bg-hover);
  }
}

.hamburger-line {
  width: 100%;
  height: 2px;
  background: var(--fts-text-secondary);
  border-radius: 1px;
  transition: all 0.3s ease;
}

/* ---- 左侧品牌区 ---- */
.el-topbar__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;

  &-icon {
    color: var(--fts-primary);
  }

  &-text {
    font-size: 14px;
    font-weight: 600;
    color: var(--fts-text-secondary);
    letter-spacing: 0.3px;
    white-space: nowrap;
  }
}

/* ---- 中间面包屑（弹性占据剩余空间） ---- */
.el-topbar__center {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;

  /* 使用 :deep() 覆盖 el-breadcrumb 样式以适配项目风格 */
  :deep(.el-breadcrumb) {
    font-size: 13px;

    .el-breadcrumb__inner {
      color: var(--fts-text-tertiary);
      font-weight: 400;

      &.is-link:hover {
        color: var(--fts-primary);
      }
    }

    .el-breadcrumb__separator {
      color: var(--fts-text-quaternary);
      margin: 0 6px;
    }

    /* 最后一级加粗 */
    .el-breadcrumb__inner:last-child {
      color: var(--fts-text-primary);
      font-weight: 600;
    }
  }
}

/* ---- 右侧工具区 ---- */
.el-topbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

// ---------- 字体大小控制按钮组 ----------
.el-font-control {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-primary);
  padding: 2px;
}

.el-font-btn {
  width: 28px;
  height: 26px;
  border: none;
  border-radius: calc(var(--fts-radius-md) - 2px);
  background: transparent;
  color: var(--fts-text-secondary);
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  transition: all 0.15s ease;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover:not(.el-font-btn--active) {
    color: var(--fts-text-primary);
    background: var(--fts-bg-card);
  }

  &:active:not(.el-font-btn--active) {
    transform: scale(0.92);
  }

  &--active {
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);
  }
}

// ---------- 主题选择器 ----------
.el-theme-selector {
  position: relative;
}

.el-theme-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-primary);
  background: var(--fts-bg-card);
  color: var(--fts-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
    color: var(--fts-primary);
  }

  &:active {
    transform: scale(0.96);
  }
}

.el-theme-popover {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 160px;
  padding: 8px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  box-shadow: var(--fts-shadow-md);
  z-index: 300;
}

.el-popover-title {
  font-size: 10px;
  font-weight: 600;
  color: var(--fts-text-quaternary);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 0 8px 8px;
  margin-bottom: 2px;
}

.el-theme-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  color: var(--fts-text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.12s ease;

  &:hover {
    background: var(--fts-bg-page);
    color: var(--fts-text-primary);
  }

  &--active {
    background: var(--fts-primary-light);
    color: var(--fts-primary);
    font-weight: 600;
  }
}

.el-option-label {
  flex: 1;
}

.el-option-check {
  font-size: 12px;
  color: var(--fts-primary);
  font-weight: 700;
}

// ---------- 用户头像区域 ----------
.el-user-area {
  position: relative;
}

.el-avatar-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px 2px 2px;
  border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-primary);
  background: var(--fts-bg-card);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
  }

  &:active {
    transform: scale(0.96);
  }
}

.el-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    var(--fts-primary-light),
    rgba(var(--fts-primary-rgb), 0.08)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-primary);
  flex-shrink: 0;
}

.el-avatar-arrow {
  color: var(--fts-text-tertiary);
  transition: transform 0.2s ease;

  &--open {
    transform: rotate(180deg);
  }
}

/* 用户下拉面板 */
.el-user-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 200px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  box-shadow: var(--fts-shadow-md);
  z-index: 300;
  overflow: hidden;
}

.el-dropdown-header {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--fts-border-primary);
  background: var(--fts-bg-page);
}

.el-dropdown-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-primary);
}

.el-dropdown-dept {
  font-size: 11px;
  color: var(--fts-text-tertiary);
}

.el-dropdown-divider {
  height: 1px;
  background: var(--fts-border-primary);
}

/* 角色切换器区域标题 */
.el-dropdown-section-title {
  font-size: 10px;
  font-weight: 600;
  color: var(--fts-text-quaternary);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 8px 16px 4px;
}

.el-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: transparent;
  color: var(--fts-text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.12s ease;
  text-align: left;

  &:hover {
    background: var(--fts-bg-page);
    color: var(--fts-text-primary);
  }

  &--danger:hover {
    background: rgba(var(--fts-error-rgb), 0.06);
    color: var(--fts-error);
  }

  /* 角色选项样式 */
  &--role {
    padding: 8px 16px;
    font-size: 12px;

    &:hover {
      background: var(--fts-bg-page);
    }

    &-active {
      color: var(--fts-primary);
      font-weight: 600;
    }
  }
}

/* 角色选项中的圆点指示器 */
.el-role-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.15s ease;
}

/* 当前角色标签 */
.el-role-current-tag {
  margin-left: auto;
  font-size: 10px;
  font-weight: 600;
  color: var(--fts-primary);
  background: var(--fts-primary-light);
  padding: 1px 6px;
  border-radius: var(--fts-radius-sm);
  flex-shrink: 0;
}

.el-logout-icon {
  font-size: 14px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
}

// ================================================================
//  深色模式适配
//  双重匹配：html.dark（类名切换）+ :root[data-theme='dark']（属性切换）
// ================================================================

:global(html.dark),
:root.dark,
:root[data-theme='dark'] {
  .el-theme-trigger {
    background: var(--fts-bg-secondary);
    border-color: var(--fts-border-primary);
    color: var(--fts-text-tertiary);

    &:hover {
      border-color: var(--fts-primary);
      color: var(--fts-text-primary);
    }
  }

  .el-theme-popover {
    background: var(--fts-bg-card);
    border-color: var(--fts-border-primary);
    box-shadow: var(--fts-shadow-lg);
  }

  .el-popover-title { color: var(--fts-text-tertiary); }

  .el-theme-option {
    color: var(--fts-text-secondary);

    &:hover {
      background: var(--fts-bg-hover);
      color: var(--fts-text-primary);
    }

    &--active {
      background: rgba(var(--fts-primary-rgb), 0.12);
      color: var(--fts-primary);
    }

    .el-icon { color: var(--fts-text-secondary); }
    &--active .el-icon { color: var(--fts-primary); }
  }

  .el-option-check { color: var(--fts-primary); }

  .el-avatar-trigger {
    background: var(--fts-bg-secondary);
    border-color: var(--fts-border-primary);

    &:hover {
      background: var(--fts-bg-tertiary);
      border-color: var(--fts-primary);
    }
  }

  .el-user-dropdown {
    background: var(--fts-bg-card);
    border-color: var(--fts-border-primary);
    box-shadow: var(--fts-shadow-lg);
  }

  .el-dropdown-header {
    background: var(--fts-bg-page);
    border-bottom-color: var(--fts-border-primary);
  }
  .el-dropdown-name { color: var(--fts-text-primary); }
  .el-dropdown-dept { color: var(--fts-text-tertiary); }
  .el-dropdown-divider { background: var(--fts-border-primary); }
  .el-dropdown-section-title { color: var(--fts-text-tertiary); }
  .el-dropdown-item { color: var(--fts-text-secondary); }
}

// ================================================================
//  过渡动画
// ================================================================

/* 下拉面板淡入淡出 */
.popover-fade-enter-active { transition: all 0.15s ease; }
.popover-fade-leave-active { transition: all 0.1s ease; }
.popover-fade-enter-from { opacity: 0; transform: translateY(-6px); }
.popover-fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
