<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useEmployeeStore } from '@/stores/employee'
import { usePermissionStore, getIconComponent } from '@/stores/permission'
import PcSidebar from './components/PcSidebar.vue'
import PcTopbar from './components/PcTopbar.vue'
import MobileHeader from './components/MobileHeader.vue'
import MobileTabBar from './components/MobileTabBar.vue'
import PwaInstallBanner from '@/components/core/PwaInstallBanner.vue'
import MobileDrawer, { type NavItem } from './components/MobileDrawer.vue'
import FabTodoDrawer from '@/components/business/FabTodoDrawer.vue'
import CreateApprovalDialog from '@/components/business/CreateApprovalDialog.vue'

const route = useRoute()
const router = useRouter()
const employeeStore = useEmployeeStore()
const permission = usePermissionStore()

// ========== 侧边栏折叠状态 ==========
const sidebarCollapsed = ref<boolean>(
  localStorage.getItem('emp-sidebar-collapsed') === 'true'
)

const sidebarWidth = computed(() => sidebarCollapsed.value ? '64px' : '240px')

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('emp-sidebar-collapsed', String(sidebarCollapsed.value))
}

// ========== 移动端抽屉 ==========
const mobileSidebarOpen = ref(false)

function openMobileSidebar() {
  mobileSidebarOpen.value = true
  document.body.style.overflow = 'hidden'
}

function closeMobileSidebar() {
  mobileSidebarOpen.value = false
  document.body.style.overflow = ''
}

// ========== 导航数据（权限驱动） ==========
const mainNav = computed(() => permission.getMainNav())
const secondaryNav = computed(() => permission.getSecondaryNav())
const mobileTabs = computed(() => permission.getMobileTabs())

/** 合并主导航与次级导航为 Drawer 统一格式 */
const allMobileNavItems = computed<NavItem[]>(() => [
  ...mainNav.value.map(item => ({ id: item.id, label: item.label, icon: getIconComponent(item.icon), route: item.route, secondary: false })),
  ...secondaryNav.value.map(item => ({ id: item.id, label: item.label, icon: getIconComponent(item.icon), route: item.route, secondary: true as const })),
])

// ========== 面包屑 ==========
interface BreadcrumbItem {
  label: string
  path?: string
}

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const navMap: Record<string, string> = {}
  for (const item of [...mainNav.value, ...secondaryNav.value]) {
    navMap[item.route] = item.label
  }
  const staticMap: Record<string, BreadcrumbItem[]> = {
    '/profile': [{ label: '个人中心' }],
  }
  if (staticMap[route.path]) return staticMap[route.path]

  // 子页面面包屑映射（父页面 → 子页面标题）
  const childMap: Record<string, string> = {
    'approval-detail': '审批详情',
    'approval-create': '发起申请',
    'review-detail': '考核详情',
  }
  const routeName = route.name as string
  if (routeName && childMap[routeName]) {
    const parentNav = [...mainNav.value, ...secondaryNav.value].find(
      n => route.path.startsWith(n.route) && n.route !== route.path
    )
    if (parentNav) {
      return [
        { label: parentNav.label, path: parentNav.route },
        { label: childMap[routeName] },
      ]
    }
  }

  if (navMap[route.path]) return [{ label: navMap[route.path] }]
  return [{ label: '员工门户' }]
})

const pageTitle = computed(() => {
  const crumbs = breadcrumbs.value
  return crumbs[crumbs.length - 1]?.label || '员工门户'
})

// ========== 路由激活判断 ==========
function isActive(path: string): boolean {
  if (path === '/home') return route.path === '/' || route.path === '/home'
  const currentTab = route.meta?.tab as string | undefined
  if (currentTab) {
    const pathTabMap: Record<string, string> = {
      '/office': 'office',
      '/messages': 'messages',
      '/profile': 'profile',
    }
    return pathTabMap[path] === currentTab
  }
  return route.path.startsWith(path)
}

// ========== 用户操作回调 ==========
function handleUserAction(action: string) {
  if (action === 'logout') {
    employeeStore.logout()
    router.push('/login')
  } else if (action === 'profile') {
    router.push('/profile')
  }
}

function onMobileNavigate(navigateRoute: string) {
  router.push(navigateRoute)
}

// ========== 发起申请弹窗 ==========
const createApprovalVisible = ref(false)
</script>

<template>
  <div class="el-layout">
    <!-- PC 端侧边栏 -->
    <PcSidebar
      :collapsed="sidebarCollapsed"
      @update:collapsed="sidebarCollapsed = $event"
      @toggle="toggleSidebar"
      @create-approval="createApprovalVisible = true"
    />

    <!-- 主内容区域 -->
    <div class="el-main-wrap" :style="{ marginLeft: sidebarWidth }">
      <!-- PC 端顶栏 -->
      <PcTopbar
        :breadcrumbs="breadcrumbs"
        :page-title="pageTitle"
        @open-mobile-sidebar="openMobileSidebar"
        @user-action="handleUserAction"
      />

      <!-- 移动端顶栏（与 PcTopbar 互补：PC 隐藏时显示，移动端显示） -->
      <MobileHeader
        :title="pageTitle"
        :show-menu="true"
        @open-menu="openMobileSidebar"
      />

      <!-- 页面内容插槽 -->
      <main class="el-content">
        <slot />
      </main>

      <FabTodoDrawer />
    </div>

    <!-- 移动端底部 TabBar -->
    <MobileTabBar :tabs="mobileTabs" :is-active="isActive" />

    <!-- PWA 安装提示横幅（仅移动端显示） -->
    <PwaInstallBanner />

    <!-- 移动端抽屉式侧边栏 -->
    <MobileDrawer
      :visible="mobileSidebarOpen"
      :nav-items="allMobileNavItems"
      :active-path="route.path"
      @close="closeMobileSidebar"
      @navigate="onMobileNavigate"
    />

    <!-- 发起申请弹窗（全局） -->
    <CreateApprovalDialog v-model="createApprovalVisible" />
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   EmployeeLayout — 薄编排器（仅负责布局容器 + 子组件组合）
   所有业务子组件已提取至 layout/components/ 目录
   ================================================================ */

.el-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--fts-bg-page);
  overflow: hidden;

  @media (max-width: 767px) {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }
}

.el-main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  margin-left: 220px;
  transition: margin-left var(--fts-duration-slow, 500ms) var(--fts-easing-smooth, cubic-bezier(0.4, 0, 0.2, 1));

  @media (max-width: 767px) {
    margin-left: 0 !important;
    overflow-y: auto;
    overflow-x: hidden;
    /* 为 MobileHeader + fixed TabBar 预留空间 */
    padding-bottom: var(--fts-tabbar-height-safe);
  }
}

.el-content {
  flex: 1;
  min-width: 0;
  width: 100%;
  max-width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--fts-bg-page);
  position: relative;

  -webkit-overflow-scrolling: touch;
}
</style>
