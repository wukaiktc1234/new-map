<script setup lang="ts">
/**
 * 主布局组件 - 支持三种侧边栏风格（统一宽度264px）
 * 风格1: 左侧菜单（图标+文字，点击展开二级菜单）
 * 风格2: 双列图标菜单（只有图标，双列排列，Tooltip提示）
 * 风格3: 双列混合菜单（左侧图标栏 + 右侧文字菜单栏）
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Check, Sunny, Moon, Monitor, Expand, Crop, MagicStick, Brush, Guide, CircleCheck, CircleClose, Minus, WarningFilled
} from '@element-plus/icons-vue'
import { useLayoutStore } from '@/stores/layout'
import { usePermissionStore } from '@/stores/permission'
import type { MenuGroupConfig } from '@/types/permission'
import { isMockMode, isMockFallbackTriggered } from '@/api/request'
import SidebarMenu from './SidebarMenu.vue'
import TopNavbar from './TopNavbar.vue'
import TabBar from './TabBar.vue'
import PageFooter from './PageFooter.vue'
import WatermarkOverlay from './WatermarkOverlay.vue'

const layoutStore = useLayoutStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const settingsDrawerVisible = ref(false)
const customRadius = ref(layoutStore.borderRadius)
const showCustomRadius = ref(false)

// ========== 演示模式横幅状态 ==========
/**
 * 跟踪 Mock fallback 状态（非响应式 → 响应式）
 *
 * request.ts 中的 mockFallbackTriggered 是模块级变量，
 * 此处通过定时轮询同步到 Vue ref，供 UI 感知"演示模式"。
 *
 * 触发条件（满足任一即显示横幅）：
 * 1. isMockMode()=true → VITE_USE_MOCK=true 启用的全局 Mock 模式
 * 2. isMockFallbackTriggered()=true → operations API catch 块静默回退
 *
 * 轮询策略：前 30s 内每 1s 检查一次（捕获页面加载时的 API fallback），
 * 检测到 mock 状态后立即停止；30s 后也停止以节省性能。
 * 注意：一旦 usingMockData=true，在当前会话内保持为 true（mock 状态不可逆）。
 */
const usingMockData = ref(false)
let mockCheckTimer: ReturnType<typeof setInterval> | null = null
let mockCheckStopTimer: ReturnType<typeof setTimeout> | null = null

function checkMockStatus(): void {
  if (isMockMode() || isMockFallbackTriggered()) {
    usingMockData.value = true
    if (mockCheckTimer) {
      clearInterval(mockCheckTimer)
      mockCheckTimer = null
    }
    if (mockCheckStopTimer) {
      clearTimeout(mockCheckStopTimer)
      mockCheckStopTimer = null
    }
  }
}

/** 是否显示演示模式横幅（仅运营模块路由显示） */
const showDemoModeBanner = computed((): boolean => {
  if (!route.path.startsWith('/operations')) return false
  return usingMockData.value
})

/** 演示模式横幅文案（根据触发原因分级） */
const demoModeText = computed((): string => {
  if (isMockMode()) return '演示模式：API 已自动切换到 Mock 数据'
  if (isMockFallbackTriggered()) return '演示模式：部分接口使用本地演示数据'
  return '演示模式'
})

// ========== 组件倒角选项（3个预设 + 自定义）==========
const radiusOptions = [
  { label: '直角', value: 0 },
  { label: '小圆角', value: 8 },
  { label: '大圆角', value: 16 }
]

function handleRadiusChange(val: number) {
  layoutStore.setBorderRadius(val)
  customRadius.value = val
  showCustomRadius.value = false
}

function handleCustomRadiusChange(val: number) {
  if (val >= 0 && val <= 32) {
    layoutStore.setBorderRadius(val)
  }
}


const menuItems = computed<MenuGroupConfig[]>(() => {
  if (!permissionStore.isLoggedIn) return []
  return permissionStore.getVisibleMenus()
})

/**
 * 路径前缀 → 模块名称映射表
 * 用于从扁平路由路径生成多级面包屑
 */
const PATH_MODULE_MAP: Record<string, string> = {
  'store-management': '门店管理',
  'product': '产品中心',
  'order': '订单管理',
  'operations': '运营中心',
  'purchase': '采购管理',
  'warehouse': '仓储管理',
  'hr': '人事管理',
  'schedule': '排班管理',
  'finance': '财务中心',
  'system-settings': '系统管理',
  'system': '系统管理',
  'device': '设备管理',
  'traceability': '食品追溯',
  'asset': '资产管理',
  'marketing': '会员管理',
}

/**
 * 动态生成多级面包屑
 * 基于路径分段逐级匹配，解决扁平路由无父子关系的问题
 *
 * 示例：/store-management/pending-tasks
 * → [{ path: '/store-management', title: '门店管理' }, { path: '/store-management/pending-tasks', title: '待办事项' }]
 */
const breadcrumbItems = computed<{ path: string; title: string }[]>(() => {
  const path = route.path
  if (path === '/home' || path === '/') return []

  // 按斜杠分割路径段
  const segments = path.split('/').filter(Boolean)
  const items: { path: string; title: string }[] = []
  let currentPath = ''

  for (let i = 0; i < segments.length; i++) {
    currentPath += '/' + segments[i]
    // 最后一段使用路由 meta.title，中间段使用模块映射
    const isLast = i === segments.length - 1
    const title = isLast
      ? (route.meta?.title as string) || segments[i]
      : PATH_MODULE_MAP[segments[i]] || segments[i]

    items.push({ path: currentPath, title })
  }

  return items
})

function openSettings() {
  settingsDrawerVisible.value = true
}

function handleToggleSidebar() {
  layoutStore.toggleSidebar()
}

function handleLayoutChange(layout: 'left' | 'icon' | 'mixed') {
  layoutStore.setMenuLayout(layout)
}

// 初始化
onMounted(() => {
  layoutStore.init()

  // 初始化权限Store（从Token加载用户信息）
  if (!permissionStore.loaded && !permissionStore.loading) {
    permissionStore.initFromToken()
  }

  // 启动 Mock fallback 状态轮询
  checkMockStatus()
  mockCheckTimer = setInterval(checkMockStatus, 1000)
  // 30s 后自动停止轮询（节省性能；大多数 API fallback 在此之前已触发）
  mockCheckStopTimer = setTimeout(() => {
    if (mockCheckTimer) {
      clearInterval(mockCheckTimer)
      mockCheckTimer = null
    }
    mockCheckStopTimer = null
  }, 30000)
})

onUnmounted(() => {
  if (mockCheckTimer) {
    clearInterval(mockCheckTimer)
    mockCheckTimer = null
  }
  if (mockCheckStopTimer) {
    clearTimeout(mockCheckStopTimer)
    mockCheckStopTimer = null
  }
})
</script>

<template>
  <div class="main-layout">
    <!-- 侧边菜单 -->
    <SidebarMenu
      :key="permissionStore.menuVersion"
      :menu-items="menuItems"
      :collapsed="layoutStore.sidebarCollapsed"
      :layout="layoutStore.menuLayout"
      :border-radius="layoutStore.borderRadius"
      @toggle="handleToggleSidebar"
    />

    <!-- 主内容区 -->
    <div
      class="main-layout__content"
      :class="{ 'main-layout__content--fixed': layoutStore.contentWidth === 'fixed' }"
      :style="{ 'margin-left': layoutStore.sidebarWidthValue }"
    >
      <!-- 顶部导航 -->
      <TopNavbar
        :sidebar-width="layoutStore.sidebarWidth"
        :border-radius="layoutStore.borderRadius"
        @open-settings="openSettings"
      />

      <!-- 标签栏（根据导航设置显示） -->
      <TabBar
        v-show="layoutStore.navigationMode === 'tab'"
        :sidebar-width="layoutStore.sidebarWidth"
        :tab-style="layoutStore.tabStyle"
      />

      <!-- 面包屑导航（仅面包屑模式显示）-->
      <div
        v-show="layoutStore.navigationMode === 'breadcrumb'"
        class="breadcrumb-wrapper"
      >
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <template v-for="(item, index) in breadcrumbItems" :key="item.path">
            <el-breadcrumb-item
              v-if="index < breadcrumbItems.length - 1"
              :to="{ path: item.path }"
            >{{ item.title }}</el-breadcrumb-item>
            <el-breadcrumb-item v-else>{{ item.title }}</el-breadcrumb-item>
          </template>
        </el-breadcrumb>
      </div>

      <!-- 页面内容区域 -->
      <main
        class="main-layout__main"
        :class="{
          'main-layout__main--no-tab': layoutStore.navigationMode === 'breadcrumb'
        }"
      >
        <!-- 演示模式横幅（仅运营模块在 Mock fallback 或开发模式降级时显示） -->
        <div
          v-if="showDemoModeBanner"
          class="demo-mode-banner"
          role="status"
          aria-live="polite"
        >
          <el-icon class="demo-mode-banner__icon"><WarningFilled /></el-icon>
          <span class="demo-mode-banner__text">{{ demoModeText }}</span>
          <span class="demo-mode-banner__hint">（数据仅供演示，请勿用于真实决策）</span>
        </div>

        <div class="main-layout__page">
          <router-view />
        </div>
        <!-- 底部页脚 -->
        <PageFooter v-if="layoutStore.showFooter" />
      </main>
    </div>

    <!-- 页面水印 -->
    <WatermarkOverlay v-if="layoutStore.showWatermark" text="食品溯源系统" />

    <!-- 右侧设置抽屉 -->
    <el-drawer
      v-model="settingsDrawerVisible"
      title="界面设置"
      direction="rtl"
      size="360px"
      :with-header="true"
      :lock-scroll="false"
      class="settings-drawer"
    >
      <div class="settings-content">
        <!-- 主题设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">主题风格</h3>
          <p class="settings-section__desc">选择系统主题显示模式</p>
          <div class="theme-mode-options">
            <div
              class="theme-mode-option"
              :class="{ 'theme-mode-option--active': layoutStore.themeMode === 'light' }"
              @click="layoutStore.setThemeMode('light')"
            >
              <div class="theme-mode-icon">
                <el-icon :size="20"><Sunny /></el-icon>
              </div>
              <span class="theme-mode-label">浅色</span>
              <el-icon v-if="layoutStore.themeMode === 'light'" class="theme-mode-check"><Check /></el-icon>
            </div>
            <div
              class="theme-mode-option"
              :class="{ 'theme-mode-option--active': layoutStore.themeMode === 'dark' }"
              @click="layoutStore.setThemeMode('dark')"
            >
              <div class="theme-mode-icon">
                <el-icon :size="20"><Moon /></el-icon>
              </div>
              <span class="theme-mode-label">深色</span>
              <el-icon v-if="layoutStore.themeMode === 'dark'" class="theme-mode-check"><Check /></el-icon>
            </div>
            <div
              class="theme-mode-option"
              :class="{ 'theme-mode-option--active': layoutStore.themeMode === 'auto' }"
              @click="layoutStore.setThemeMode('auto')"
            >
              <div class="theme-mode-icon">
                <el-icon :size="20"><Monitor /></el-icon>
              </div>
              <span class="theme-mode-label">跟随系统</span>
              <el-icon v-if="layoutStore.themeMode === 'auto'" class="theme-mode-check"><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 菜单布局设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">菜单布局</h3>
          <p class="settings-section__desc">选择侧边栏的展示方式</p>

          <div class="layout-options">
            <div
              class="layout-option"
              :class="{ 'layout-option--active': layoutStore.menuLayout === 'left' }"
              @click="handleLayoutChange('left')"
            >
              <div class="layout-option__preview">
                <div class="preview-left-menu">
                  <div class="preview-left-sidebar">
                    <div class="preview-dot" />
                    <div class="preview-line" />
                    <div class="preview-line" />
                  </div>
                  <div class="preview-body" />
                </div>
              </div>
              <div class="layout-option__info">
                <span class="layout-option__label">左侧菜单</span>
                <span class="layout-option__desc">经典侧边栏布局</span>
              </div>
              <el-icon v-if="layoutStore.menuLayout === 'left'" class="layout-option__check"><Check /></el-icon>
            </div>

            <div
              class="layout-option"
              :class="{ 'layout-option--active': layoutStore.menuLayout === 'icon' }"
              @click="handleLayoutChange('icon')"
            >
              <div class="layout-option__preview">
                <div class="preview-icon-menu">
                  <div class="preview-icon-sidebar">
                    <div class="preview-icon-dot" />
                    <div class="preview-icon-dot" />
                  </div>
                  <div class="preview-icon-sidebar">
                    <div class="preview-icon-dot" />
                    <div class="preview-icon-dot" />
                  </div>
                  <div class="preview-body" />
                </div>
              </div>
              <div class="layout-option__info">
                <span class="layout-option__label">双列图标</span>
                <span class="layout-option__desc">紧凑双列图标</span>
              </div>
              <el-icon v-if="layoutStore.menuLayout === 'icon'" class="layout-option__check"><Check /></el-icon>
            </div>

            <div
              class="layout-option"
              :class="{ 'layout-option--active': layoutStore.menuLayout === 'mixed' }"
              @click="handleLayoutChange('mixed')"
            >
              <div class="layout-option__preview">
                <div class="preview-mixed-menu">
                  <div class="preview-mixed-iconbar">
                    <div class="preview-icon-dot" />
                    <div class="preview-icon-dot" />
                  </div>
                  <div class="preview-mixed-sidebar">
                    <div class="preview-line" />
                    <div class="preview-line" />
                  </div>
                  <div class="preview-body" />
                </div>
              </div>
              <div class="layout-option__info">
                <span class="layout-option__label">双列混合</span>
                <span class="layout-option__desc">图标+文字混合</span>
              </div>
              <el-icon v-if="layoutStore.menuLayout === 'mixed'" class="layout-option__check"><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 容器宽度 -->
        <div class="settings-section">
          <h3 class="settings-section__title">容器宽度</h3>
          <p class="settings-section__desc">调整内容区域显示宽度</p>
          <div class="width-options">
            <div
              class="width-option"
              :class="{ 'width-option--active': layoutStore.contentWidth === 'full' }"
              @click="layoutStore.setContentWidth('full')"
            >
              <div class="width-preview">
                <Expand />
              </div>
              <span class="width-label">铺满</span>
            </div>
            <div
              class="width-option"
              :class="{ 'width-option--active': layoutStore.contentWidth === 'fixed' }"
              @click="layoutStore.setContentWidth('fixed')"
            >
              <div class="width-preview">
                <Crop />
              </div>
              <span class="width-label">定宽</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 标签页风格 -->
        <div class="settings-section">
          <h3 class="settings-section__title">标签页风格</h3>
          <p class="settings-section__desc">选择标签页显示样式</p>
          <div class="tab-style-options">
            <div
              class="tab-style-option"
              :class="{ 'tab-style-option--active': layoutStore.tabStyle === 'default' }"
              @click="layoutStore.setTabStyle('default')"
            >
              <div class="tab-preview tab-preview--default">
                <div class="tab-item" />
                <div class="tab-item" />
              </div>
              <span class="tab-style-label">默认</span>
            </div>
            <div
              class="tab-style-option"
              :class="{ 'tab-style-option--active': layoutStore.tabStyle === 'chrome' }"
              @click="layoutStore.setTabStyle('chrome')"
            >
              <div class="tab-preview tab-preview--chrome">
                <div class="tab-item" />
                <div class="tab-item" />
              </div>
              <span class="tab-style-label">谷歌</span>
            </div>
            <div
              class="tab-style-option"
              :class="{ 'tab-style-option--active': layoutStore.tabStyle === 'card' }"
              @click="layoutStore.setTabStyle('card')"
            >
              <div class="tab-preview tab-preview--card">
                <div class="tab-item" />
                <div class="tab-item" />
              </div>
              <span class="tab-style-label">卡片</span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 组件倒角 -->
        <div class="settings-section">
          <h3 class="settings-section__title">组件倒角</h3>
          <p class="settings-section__desc">调整组件边框圆角大小</p>
          <div class="radius-options">
            <div
              v-for="option in radiusOptions"
              :key="option.value"
              class="radius-option"
              :class="{ 'radius-option--active': layoutStore.borderRadius === option.value && !showCustomRadius }"
              @click="handleRadiusChange(option.value)"
            >
              <div
                class="radius-preview"
                :style="{ borderRadius: option.value + 'px' }"
              />
              <span class="radius-label">{{ option.label }}</span>
            </div>
            <!-- 自定义选项 -->
            <div
              class="radius-option"
              :class="{ 'radius-option--active': showCustomRadius || (layoutStore.borderRadius !== 0 && layoutStore.borderRadius !== 8 && layoutStore.borderRadius !== 16) }"
              @click="showCustomRadius = true"
            >
              <div class="radius-preview radius-preview--custom">
                <span class="custom-text">{{ showCustomRadius || (layoutStore.borderRadius !== 0 && layoutStore.borderRadius !== 8 && layoutStore.borderRadius !== 16) ? layoutStore.borderRadius : '?' }}</span>
              </div>
              <span class="radius-label">自定义</span>
            </div>
          </div>
          <!-- 自定义滑块 -->
          <div v-if="showCustomRadius" class="custom-radius-slider">
            <el-slider
              v-model="customRadius"
              :min="0"
              :max="32"
              :step="1"
              show-input
              @change="handleCustomRadiusChange"
            />
          </div>
        </div>

        <el-divider />

        <!-- 动画设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">动画效果</h3>
          <p class="settings-section__desc">调整界面动画流畅度</p>
          <div class="animation-options">
            <div
              v-for="option in [
                { label: '全部动画', value: 'full', icon: MagicStick },
                { label: '简化动画', value: 'reduced', icon: Minus },
                { label: '关闭动画', value: 'none', icon: CircleClose }
              ]"
              :key="option.value"
              class="animation-option"
              :class="{ 'animation-option--active': layoutStore.animationLevel === option.value }"
              @click="layoutStore.setAnimationLevel(option.value as any)"
            >
              <div class="animation-icon">
                <el-icon :size="20"><component :is="option.icon" /></el-icon>
              </div>
              <span class="animation-label">{{ option.label }}</span>
              <el-icon v-if="layoutStore.animationLevel === option.value" class="animation-check"><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 字体设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">字体风格</h3>
          <p class="settings-section__desc">选择系统字体显示风格</p>
          <div class="font-options">
            <div
              v-for="option in [
                { label: '系统默认', value: 'system', sample: 'Aa' },
                { label: '宋体', value: 'serif', sample: 'Aa' },
                { label: '等宽', value: 'mono', sample: 'Aa' }
              ]"
              :key="option.value"
              class="font-option"
              :class="{ 'font-option--active': layoutStore.fontFamily === option.value }"
              @click="layoutStore.setFontFamily(option.value as any)"
            >
              <div class="font-sample" :style="{ fontFamily: option.value === 'system' ? '-apple-system, sans-serif' : option.value === 'serif' ? 'SimSun, serif' : 'monospace' }">
                {{ option.sample }}
              </div>
              <span class="font-label">{{ option.label }}</span>
              <el-icon v-if="layoutStore.fontFamily === option.value" class="font-check"><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 导航设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">导航显示</h3>
          <p class="settings-section__desc">选择页面导航展示方式</p>
          <div class="nav-options">
            <div
              v-for="option in [
                { label: '标签页', value: 'tab', desc: '多标签页切换' },
                { label: '面包屑', value: 'breadcrumb', desc: '层级路径导航' }
              ]"
              :key="option.value"
              class="nav-option"
              :class="{ 'nav-option--active': layoutStore.navigationMode === option.value }"
              @click="layoutStore.setNavigationMode(option.value as any)"
            >
              <div class="nav-info">
                <span class="nav-label">{{ option.label }}</span>
                <span class="nav-desc">{{ option.desc }}</span>
              </div>
              <el-icon v-if="layoutStore.navigationMode === option.value" class="nav-check"><CircleCheck /></el-icon>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 显示设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">显示设置</h3>
          <div class="display-options">
            <div class="display-item">
              <div class="display-info">
                <span class="display-label">紧凑模式</span>
                <span class="display-desc">减小间距，显示更多内容</span>
              </div>
              <el-switch v-model="layoutStore.compactMode" @change="layoutStore.setCompactMode" />
            </div>
            <div class="display-item">
              <div class="display-info">
                <span class="display-label">页面水印</span>
                <span class="display-desc">显示用户水印标识</span>
              </div>
              <el-switch v-model="layoutStore.showWatermark" @change="layoutStore.setShowWatermark" />
            </div>
            <div class="display-item">
              <div class="display-info">
                <span class="display-label">底部页脚</span>
                <span class="display-desc">显示页面底部版权信息</span>
              </div>
              <el-switch v-model="layoutStore.showFooter" @change="layoutStore.setShowFooter" />
            </div>
          </div>
        </div>

        <el-divider />

        <el-divider />

        <!-- 表格设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">表格设置</h3>
          <div class="table-settings">
            <!-- 表格密度 -->
            <div class="table-setting-item">
              <div class="table-setting-info">
                <span class="table-setting-label">行高</span>
                <span class="table-setting-desc">调整表格行间距</span>>
              </div>
              <div class="table-density-options">
                <el-radio-group v-model="layoutStore.tableDensity" @change="layoutStore.setTableDensity">
                  <el-radio-button label="compact">紧凑</el-radio-button>
                  <el-radio-button label="default">默认</el-radio-button>
                  <el-radio-button label="comfortable">宽松</el-radio-button>
                </el-radio-group>
              </div>
            </div>

            <!-- 边框样式 -->
            <div class="table-setting-item">
              <div class="table-setting-info">
                <span class="table-setting-label">边框样式</span>
                <span class="table-setting-desc">表格边框显示方式</span>
              </div>
              <div class="table-border-options">
                <el-radio-group v-model="layoutStore.tableBorderStyle" @change="layoutStore.setTableBorderStyle">
                  <el-radio-button label="none">无</el-radio-button>
                  <el-radio-button label="horizontal">横线</el-radio-button>
                  <el-radio-button label="full">全边框</el-radio-button>
                </el-radio-group>
              </div>
            </div>

            <!-- 斑马纹 -->
            <div class="table-setting-item">
              <div class="table-setting-info">
                <span class="table-setting-label">斑马纹</span>
                <span class="table-setting-desc">隔行背景色交替</span>>
              </div>
              <el-switch v-model="layoutStore.tableStriped" @change="layoutStore.setTableStriped" />
            </div>

            <!-- 悬停效果 -->
            <div class="table-setting-item">
              <div class="table-setting-info">
                <span class="table-setting-label">悬停高亮</span>
                <span class="table-setting-desc">鼠标悬停时高亮行</span>
              </div>
              <el-switch v-model="layoutStore.tableHover" @change="layoutStore.setTableHover" />
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 页面显示设置 -->
        <div class="settings-section">
          <h3 class="settings-section__title">页面显示</h3>
          <div class="page-display-settings">
            <div class="page-display-item">
              <div class="page-display-info">
                <span class="page-display-label">页面字号</span>
                <span class="page-display-desc">调整全局文字大小</span>
              </div>
              <el-radio-group v-model="layoutStore.pageFontSize" @change="layoutStore.setPageFontSize" size="small">
                <el-radio-button label="small">小</el-radio-button>
                <el-radio-button label="default">中</el-radio-button>
                <el-radio-button label="large">大</el-radio-button>
              </el-radio-group>
            </div>
            <div class="page-display-item">
              <div class="page-display-info">
                <span class="page-display-label">图标大小</span>
                <span class="page-display-desc">调整全局图标尺寸</span>
              </div>
              <el-radio-group v-model="layoutStore.pageIconSize" @change="layoutStore.setPageIconSize" size="small">
                <el-radio-button label="small">小</el-radio-button>
                <el-radio-button label="default">中</el-radio-button>
                <el-radio-button label="large">大</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 辅助功能 -->
        <div class="settings-section">
          <h3 class="settings-section__title">辅助功能</h3>
          <div class="accessibility-options">
            <div class="accessibility-item">
              <div class="accessibility-info">
                <span class="accessibility-label">色弱模式</span>
                <span class="accessibility-desc">增强色彩对比度</span>>
              </div>
              <el-switch v-model="layoutStore.colorWeakMode" @change="layoutStore.setColorWeakMode" />
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
// var() fallback为防御性编码，确保CSS变量未定义时有默认样式

// ========== 主布局 ==========
.main-layout {
  display: flex;
  min-height: 100vh;
}

// ========== 演示模式横幅（运营模块 Mock fallback 时显示） ==========
.demo-mode-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-4);
  margin: 0 var(--fts-space-4) var(--fts-space-3);
  background: linear-gradient(90deg,
    color-mix(in srgb, var(--fts-warning) 12%, transparent),
    color-mix(in srgb, var(--fts-warning) 18%, transparent),
    color-mix(in srgb, var(--fts-warning) 12%, transparent)
  );
  border: 1px solid color-mix(in srgb, var(--fts-warning) 45%, transparent);
  border-radius: var(--fts-radius-md, 8px);
  color: var(--fts-warning-dark, var(--fts-warning));
  font-size: var(--fts-font-size-sm);
  font-weight: 600;

  &__icon {
    font-size: 16px;
    color: var(--fts-warning);
  }

  &__text {
    color: var(--fts-warning-dark, var(--fts-warning));
  }

  &__hint {
    color: var(--fts-text-secondary);
    font-weight: 400;
    font-size: var(--fts-font-size-xs);
  }

  // 响应式：小屏幕下隐藏 hint
  @media (max-width: 768px) {
    flex-wrap: wrap;
    gap: var(--fts-space-1);

    &__hint {
      width: 100%;
      text-align: center;
    }
  }
}

// 抽屉样式
:deep(.settings-drawer) {
  .el-drawer__body {
    overflow-y: auto;
  }
}

.main-layout__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  transition: margin-left 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &--fixed {
    .main-layout__main {
      align-items: center;
    }

    .main-layout__page {
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
    }
  }
}

.main-layout__main {
  flex: 1;
  background: var(--fts-bg-page);
  margin-top: 104px;
  overflow-x: hidden;
  padding: var(--fts-space-5);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-width: 0;

  &--no-tab {
    margin-top: 64px;
  }

  &--with-breadcrumb {
    margin-top: 108px;
  }
}

// 面包屑导航容器
.breadcrumb-wrapper {
  position: fixed;
  top: 64px;
  left: v-bind('layoutStore.sidebarWidthValue');
  right: 0;
  z-index: 98;
  padding: var(--fts-space-2) var(--fts-space-6);
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  transition: left 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-secondary);

      &.is-link:hover {
        color: var(--fts-primary);
      }
    }

    &:last-child .el-breadcrumb__inner {
      color: var(--fts-text-primary);
      font-weight: var(--fts-font-weight-medium);
    }
  }
}

.main-layout__page {
  padding: 0;
  width: 100%;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

// ========== 设置抽屉样式 ==========
.settings-content {
  padding: var(--fts-space-2) 0;
}

.settings-section {
  margin-bottom: var(--fts-space-5);

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin: 0 0 var(--fts-space-1);
  }

  &__desc {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
    margin: 0 0 var(--fts-space-4);
  }
}

// ========== 主题模式选项 ==========
.theme-mode-options {
  display: flex;
  gap: var(--fts-space-3);
}

.theme-mode-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.theme-mode-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-primary);
}

.theme-mode-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

.theme-mode-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--fts-primary);
  font-size: 14px;
}

// ========== 菜单布局选项 ==========
.layout-options {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.layout-option {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  padding: var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }

  &__preview {
    width: 80px;
    height: 56px;
    background: var(--fts-bg-page);
    border-radius: var(--fts-radius-sm);
    border: 1px solid var(--fts-border-secondary);
    overflow: hidden;
    flex-shrink: 0;
    padding: 4px;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-medium);
  }

  &__desc {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__check {
    color: var(--fts-primary);
    font-size: 16px;
  }
}

// 布局预览 - 左侧菜单
.preview-left-menu {
  display: flex;
  height: 100%;
  gap: 2px;
}

.preview-left-sidebar {
  width: 20px;
  background: var(--fts-bg-card);
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  gap: 4px;
}

.preview-icon-menu {
  display: flex;
  height: 100%;
  gap: 2px;
}

.preview-icon-sidebar {
  width: 14px;
  background: var(--fts-bg-card);
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  gap: 4px;
}

.preview-mixed-menu {
  display: flex;
  height: 100%;
  gap: 2px;
}

.preview-mixed-iconbar {
  width: 14px;
  background: var(--fts-bg-card);
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  gap: 4px;
}

.preview-mixed-sidebar {
  width: 20px;
  background: var(--fts-bg-card);
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  gap: 4px;
}

.preview-body {
  flex: 1;
  background: var(--fts-bg-page);
  border-radius: 2px;
}

.preview-dot {
  width: 8px;
  height: 8px;
  background: var(--fts-border-hover);
  border-radius: 2px;
}

.preview-icon-dot {
  width: 6px;
  height: 6px;
  background: var(--fts-border-hover);
  border-radius: 1px;
}

.preview-line {
  width: 12px;
  height: 3px;
  background: var(--fts-border-hover);
  border-radius: 1px;
}

// ========== 容器宽度选项 ==========
.width-options {
  display: flex;
  gap: var(--fts-space-3);
}

.width-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.width-preview {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-primary);
  font-size: 20px;
}

.width-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

// ========== 标签页风格选项 ==========
.tab-style-options {
  display: flex;
  gap: var(--fts-space-3);
}

.tab-style-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.tab-preview {
  width: 56px;
  height: 32px;
  background: var(--fts-bg-page);
  border: 1px solid var(--fts-border-secondary);
  display: flex;
  align-items: flex-end;
  padding: 2px;
  gap: 2px;

  .tab-item {
    height: 20px;
    background: var(--fts-bg-card);
    border: 1px solid var(--fts-border-secondary);
    flex: 1;
  }

  &--default .tab-item {
    border-radius: 4px 4px 0 0;
    border-bottom: none;
  }

  &--chrome .tab-item {
    border-radius: 8px 8px 0 0;
    position: relative;

    &::before,
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      width: 8px;
      height: 8px;
    }

    &::before {
      left: -8px;
      border-bottom: 1px solid var(--fts-border-secondary);
      border-right: 1px solid var(--fts-border-secondary);
      border-bottom-right-radius: 8px;
    }

    &::after {
      right: -8px;
      border-bottom: 1px solid var(--fts-border-secondary);
      border-left: 1px solid var(--fts-border-secondary);
      border-bottom-left-radius: 8px;
    }
  }

  &--card .tab-item {
    border-radius: 4px;
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.tab-style-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
}

// ========== 倒角选项 ==========
.radius-options {
  display: flex;
  gap: var(--fts-space-3);
}

.radius-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  flex: 1;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.radius-preview {
  width: 40px;
  height: 40px;
  background: var(--fts-primary);
  border: 2px solid var(--fts-primary);

  &--custom {
    background: var(--fts-bg-secondary);
    border-color: var(--fts-border-hover);
    display: flex;
    align-items: center;
    justify-content: center;

    .custom-text {
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-secondary);
      font-weight: var(--fts-font-weight-semibold);
    }
  }
}

.custom-radius-slider {
  margin-top: var(--fts-space-3);
  padding: 0 var(--fts-space-2);
}

.radius-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
}

// ========== 动画选项 ==========
.animation-options {
  display: flex;
  gap: var(--fts-space-3);
}

.animation-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.animation-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-primary);
}

.animation-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

.animation-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--fts-primary);
  font-size: 14px;
}

// ========== 字体选项 ==========
.font-options {
  display: flex;
  gap: var(--fts-space-3);
}

.font-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.font-sample {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.font-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

.font-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--fts-primary);
  font-size: 14px;
}

// ========== 导航选项 ==========
.nav-options {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.nav-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 2px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-hover);
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-bg);

    &:hover {
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }
}

.nav-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.nav-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.nav-check {
  color: var(--fts-primary);
  font-size: 16px;
}

// ========== 显示设置 ==========
.display-options {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.display-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 1px solid var(--fts-border-secondary);
  background: var(--fts-bg-page);
}

.display-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.display-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.display-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

// ========== 辅助功能 ==========
.accessibility-options {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.accessibility-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 1px solid var(--fts-border-secondary);
  background: var(--fts-bg-page);
}

.accessibility-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.accessibility-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.accessibility-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

// ========== 表格设置 ==========
.table-settings {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.table-setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  border: 1px solid var(--fts-border-secondary);
  background: var(--fts-bg-page);
}

.table-setting-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.table-setting-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.table-setting-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.table-density-options,
.table-border-options {
  :deep(.el-radio-button__inner) {
    background: var(--fts-bg-card);
    border-color: var(--fts-border-primary);
    color: var(--fts-text-primary);

    &:hover {
      color: var(--fts-primary);
    }
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: var(--fts-primary);
    border-color: var(--fts-primary);
    color: var(--fts-text-inverse);
    box-shadow: none;
  }
}

// ========== 页面显示设置 ==========
.page-display-settings {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.page-display-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-page-radius);
  border: 1px solid var(--fts-border-secondary);
  background: var(--fts-bg-page);
}

.page-display-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-display-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.page-display-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.page-display-item {
  :deep(.el-radio-button__inner) {
    background: var(--fts-bg-card);
    border-color: var(--fts-border-primary);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-xs);

    &:hover {
      color: var(--fts-primary);
    }
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: var(--fts-primary);
    border-color: var(--fts-primary);
    color: var(--fts-text-inverse);
    box-shadow: none;
  }
}
</style>
