<script setup lang="ts">
/**
 * 顶部导航栏组件
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserFilled, ArrowDown, Setting, Sunny, Moon, Search, Bell } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useLayoutStore } from '@/stores/layout'
import { usePermissionStore } from '@/stores/permission'
import { clearAllTokens } from '@/utils/auth'

interface Props {
  sidebarWidth?: number
  borderRadius?: number
}

const props = withDefaults(defineProps<Props>(), {
  sidebarWidth: 264,
  borderRadius: 8
})

const emit = defineEmits<{
  'open-settings': []
}>()

const route = useRoute()
const router = useRouter()
const layoutStore = useLayoutStore()
const permissionStore = usePermissionStore()

const navbarStyle = computed(() => ({
  left: `${props.sidebarWidth}px`,
  width: `calc(100% - ${props.sidebarWidth}px)`
}))

/** 路径前缀 → 模块名称映射
 * 用于将当前路由路径段翻译为面包屑层级名称。
 */
const PATH_MODULE_MAP: Record<string, string> = {
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
  'store-management': '门店管理',
}

/**
 * 动态面包屑：基于当前路由路径段生成
 * - 第一级固定为"首页"
 * - 中间段使用 PATH_MODULE_MAP 映射为模块名
 * - 最后一段使用 route.meta.title
 */
const breadcrumbItems = computed<{ path: string; title: string }[]>(() => {
  const path = route.path
  if (path === '/' || path === '/home') return []

  const segments = path.split('/').filter(Boolean)
  const items: { path: string; title: string }[] = []
  let currentPath = ''

  for (let i = 0; i < segments.length; i++) {
    currentPath += '/' + segments[i]
    const isLast = i === segments.length - 1
    const title = isLast
      ? (route.meta?.title as string) || segments[i]
      : PATH_MODULE_MAP[segments[i]] || segments[i]
    items.push({ path: currentPath, title })
  }

  return items
})

/** 当前路由对应的面包屑最后一级（用于显示当前页标题） */
const currentPageTitle = computed(() => {
  return (route.meta?.title as string) || ''
})

/** 显示的用户名 */
const displayName = computed(() => permissionStore.userInfo?.username || '未登录')

/** 用户名首字母（用于头像显示） */
const avatarText = computed(() => {
  const name = permissionStore.userInfo?.username
  if (!name) return ''
  return name.charAt(0).toUpperCase()
})

function toggleTheme() {
  const newMode = layoutStore.themeMode === 'dark' ? 'light' : 'dark'
  layoutStore.setThemeMode(newMode)
}

/** 导航到指定路径 */
function navigateTo(path: string) {
  router.push(path)
}

/** 全局搜索弹窗显示状态 */
const globalSearchVisible = ref(false)
/** 全局搜索关键词 */
const globalSearchKeyword = ref('')

/** 打开全局搜索弹窗 */
function openGlobalSearch() {
  globalSearchVisible.value = true
  globalSearchKeyword.value = ''
}

/** 执行全局搜索（按当前路由模块派发到对应搜索页） */
function performGlobalSearch() {
  const kw = globalSearchKeyword.value.trim()
  if (!kw) return
  globalSearchVisible.value = false
  // 根据当前路由模块派发到对应搜索结果页
  const top = route.path.split('/').filter(Boolean)[0] || 'home'
  const targetMap: Record<string, string> = {
    product: '/product/food',
    order: '/order/query',
    operations: '/operations',
    purchase: '/purchase',
    warehouse: '/warehouse',
    hr: '/hr',
    schedule: '/schedule',
    finance: '/finance',
    'system-settings': '/system',
    system: '/system',
    device: '/device',
    traceability: '/traceability',
    asset: '/asset',
    marketing: '/marketing',
    'store-management': '/store-management',
  }
  const target = targetMap[top] || '/'
  router.push({ path: target, query: { keyword: kw } })
  ElMessage.success(`正在搜索: ${kw}`)
}

/** 通知面板显示状态 */
const notificationPanelVisible = ref(false)
/** 通知列表 */
const notifications = ref<Array<{ id: number; title: string; time: string; read: boolean }>>([
  { id: 1, title: '系统欢迎使用食品溯源系统', time: '刚刚', read: false },
  { id: 2, title: '采购订单待审核 (3 条)', time: '5 分钟前', read: false },
  { id: 3, title: '库存预警: 大米库存低于阈值', time: '1 小时前', read: false },
])
/** 未读通知数 */
const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

/** 标记单条通知已读 */
function markAsRead(id: number) {
  const item = notifications.value.find(n => n.id === id)
  if (item) item.read = true
}

/** 全部标记为已读 */
function markAllAsRead() {
  notifications.value.forEach(n => (n.read = true))
  ElMessage.success('已全部标记为已读')
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      // 清除用户信息和权限
      permissionStore.logout()
      // 清除Token
      clearAllTokens()
      // 跳转登录页
      router.push('/login')
    } catch {
      // 用户取消，不做任何操作
    }
  }
}
</script>

<template>
  <header class="top-navbar" :style="navbarStyle">
    <!-- 左侧：侧边栏切换 + 面包屑 -->
    <div class="top-navbar__left">
      <!-- 面包屑导航（动态生成） -->
      <el-breadcrumb separator="/" class="breadcrumb-nav">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item
          v-for="(item, idx) in breadcrumbItems"
          :key="item.path"
          :to="idx === breadcrumbItems.length - 1 ? undefined : { path: item.path }"
        >
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧：工具组 -->
    <div class="top-navbar__right">
      <!-- 工具按钮组 -->
      <div class="navbar-tools__group">
        <!-- 全局搜索：点击打开搜索弹窗 -->
        <el-tooltip content="搜索" placement="bottom" :offset="4">
          <button class="tool-btn" @click="openGlobalSearch">
            <el-icon :size="18"><Search /></el-icon>
          </button>
        </el-tooltip>

        <!-- 消息通知：点击弹出通知面板 -->
        <el-popover
          v-model:visible="notificationPanelVisible"
          placement="bottom-end"
          :width="360"
          trigger="click"
          popper-class="notification-popover"
        >
          <template #reference>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-tooltip content="通知" placement="bottom" :offset="4">
                <button class="tool-btn">
                  <el-icon :size="18"><Bell /></el-icon>
                </button>
              </el-tooltip>
            </el-badge>
          </template>

          <div class="notification-panel">
            <div class="notification-panel__header">
              <span class="notification-panel__title">消息通知</span>
              <el-button link type="primary" size="small" @click="markAllAsRead">全部已读</el-button>
            </div>
            <div v-if="notifications.length === 0" class="notification-panel__empty">
              暂无通知
            </div>
            <ul v-else class="notification-panel__list">
              <li
                v-for="item in notifications"
                :key="item.id"
                :class="['notification-item', { 'is-read': item.read }]"
                @click="markAsRead(item.id)"
              >
                <div class="notification-item__dot" :class="{ 'is-read': item.read }" />
                <div class="notification-item__body">
                  <div class="notification-item__title">{{ item.title }}</div>
                  <div class="notification-item__time">{{ item.time }}</div>
                </div>
              </li>
            </ul>
          </div>
        </el-popover>
      </div>

      <!-- 分隔线 -->
      <div class="navbar-divider" />

      <!-- 主题切换按钮 -->
      <el-tooltip :content="layoutStore.themeMode === 'dark' ? '切换浅色' : '切换深色'" placement="bottom">
        <button class="navbar-action-btn theme-toggle-btn" @click="toggleTheme">
          <el-icon :size="18">
            <Sunny v-if="layoutStore.themeMode === 'dark'" />
            <Moon v-else />
          </el-icon>
        </button>
      </el-tooltip>

      <!-- 设置按钮（打开右侧抽屉） -->
      <el-tooltip content="系统设置" placement="bottom">
        <button class="navbar-action-btn" @click="emit('open-settings')">
          <el-icon :size="18"><Setting /></el-icon>
        </button>
      </el-tooltip>

      <!-- 用户信息（el-dropdown包裹整个用户区域作为触发器） -->
      <el-dropdown @command="handleCommand" trigger="click">
        <div class="user-profile">
          <el-avatar :size="32" class="user-avatar">
            <span v-if="avatarText" class="avatar-text">{{ avatarText }}</span>
            <el-icon v-else><UserFilled /></el-icon>
          </el-avatar>
          <span class="user-name">{{ displayName }}</span>
          <el-icon class="user-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="navigateTo('/personal-center')">个人中心</el-dropdown-item>
              <el-dropdown-item @click="navigateTo('/system/permission')">系统设置</el-dropdown-item>

              <el-dropdown-item divided command="logout">
                <span style="color: var(--fts-error)">退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
    </div>
  </header>

  <!-- 全局搜索弹窗（独立于导航栏 fixed 容器，避免布局干扰） -->
  <el-dialog
    v-model="globalSearchVisible"
    title="全局搜索"
    width="520px"
    align-center
    :close-on-click-modal="true"
    class="global-search-dialog"
  >
    <el-input
      v-model="globalSearchKeyword"
      placeholder="输入关键词搜索菜品、订单、库存等"
      size="large"
      clearable
      @keyup.enter="performGlobalSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <div class="global-search-hint">
      <span>提示：按 Enter 键搜索，搜索结果将跳转到当前模块的列表页</span>
    </div>
    <template #footer>
      <el-button @click="globalSearchVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!globalSearchKeyword.trim()" @click="performGlobalSearch">
        搜索
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.top-navbar {
  height: 64px;
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--fts-space-6);
  position: fixed;
  top: 0;
  z-index: 99;
  transition: left 0.4s cubic-bezier(0.4, 0, 0.2, 1),
              width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
  border-radius: 0 0 v-bind(borderRadius + 'px') v-bind(borderRadius + 'px');
}

// 左侧区域
.top-navbar__left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  min-width: 0;
  overflow: hidden;
  height: 100%;
}

// 导航栏操作按钮
.navbar-action-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--fts-text-secondary);
  cursor: pointer;
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  transition: all var(--fts-duration-fast);

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &:active {
    transform: scale(0.95);
  }
}

// 面包屑导航
.breadcrumb-nav {
  flex-shrink: 0;

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

// 右侧区域
.top-navbar__right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
  height: 100%;
}

// 工具组
.navbar-tools__group {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
}

// 工具按钮
.tool-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--fts-text-secondary);
  cursor: pointer;
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  transition: all var(--fts-duration-fast);

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &:active {
    transform: scale(0.95);
  }
}

// 通知徽章
.notification-badge {
  :deep(.el-badge__content) {
    top: 6px;
    right: 6px;
  }
}

// 分隔线
.navbar-divider {
  width: 1px;
  height: 24px;
  background: var(--fts-border-secondary);
  margin: 0 var(--fts-space-3);
  flex-shrink: 0;
}

// 用户资料
.user-profile {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  cursor: pointer;
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  transition: background var(--fts-duration-fast);
  position: relative;

  &:hover {
    background: var(--fts-bg-hover);
  }
}

.user-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--fts-primary), var(--fts-primary-hover));
}

.avatar-text {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-inverse);
  line-height: 1;
}

.user-name {
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: var(--fts-text-tertiary);
  transition: transform var(--fts-duration-fast);
}
</style>

<!-- 通知面板样式：scoped 不会作用于 popover 内部，需使用非 scoped 块 -->
<style lang="scss">
.notification-popover {
  padding: 0 !important;
  border-radius: var(--fts-radius-md) !important;
  box-shadow: var(--fts-shadow-lg) !important;
  border: 1px solid var(--fts-border-secondary) !important;
  background: var(--fts-bg-card) !important;
}

.notification-panel {
  width: 360px;
  max-height: 480px;
  display: flex;
  flex-direction: column;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--fts-space-3) var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__empty {
    padding: var(--fts-space-12) var(--fts-space-4);
    text-align: center;
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
    max-height: 400px;
    overflow-y: auto;
  }
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  cursor: pointer;
  transition: background var(--fts-duration-fast);
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--fts-bg-hover);
  }

  &.is-read {
    .notification-item__title {
      color: var(--fts-text-tertiary);
    }
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--fts-primary);
    flex-shrink: 0;
    margin-top: 6px;

    &.is-read {
      background: transparent;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-primary);
    line-height: 1.5;
    word-break: break-all;
  }

  &__time {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 4px;
  }
}

.global-search-hint {
  margin-top: var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
