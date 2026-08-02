<script setup lang="ts">
/**
 * 侧边菜单栏组件 - 支持 ADPX 风格的所有菜单模式
 * 
 * 支持四种布局风格：
 * 1. left: 左侧菜单（图标+文字，可展开/收缩）- 复刻 ADPX
 * 2. top: 顶部菜单（水平排列）
 * 3. mixed: 混合菜单（左侧图标 + 右侧文字子菜单）
 * 4. icon: 双列图标菜单（左侧父级图标 + 右侧子级图标）
 */
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

interface MenuItem {
  title: string
  icon: string
  path?: string
  children?: MenuItem[]
}

interface LogoConfig {
  icon?: string
  title?: string
}

const props = defineProps<{
  menuItems: MenuItem[]
  collapsed?: boolean
  layout?: 'left' | 'top' | 'mixed' | 'icon'
  logo?: LogoConfig
  borderRadius?: number
}>()

// Logo 配置（使用默认值）
const logoConfig = computed(() => ({
  icon: props.logo?.icon || 'Grid',
  title: props.logo?.title || '管理系统'
}))

const emit = defineEmits<{
  'toggle': []
}>()

const route = useRoute()
const router = useRouter()
const activeMenu = ref(route.path)
const expandedMenus = ref<string[]>([])
const activeSubMenu = ref('')

function findParentMenuTitle(path: string): string | undefined {
  for (const item of props.menuItems) {
    if (item.children) {
      for (const child of item.children) {
        if (child.path === path) {
          return item.title
        }
      }
    }
  }
  return undefined
}

function initExpandedMenus() {
  const parentTitle = findParentMenuTitle(route.path)
  if (parentTitle && !expandedMenus.value.includes(parentTitle)) {
    expandedMenus.value.push(parentTitle)
  }
}

initExpandedMenus()

// 混合/双列模式：当前选中的一级菜单索引
const currentMainMenu = ref(0)

function findParentMenuIndex(path: string): number {
  for (let i = 0; i < props.menuItems.length; i++) {
    const item = props.menuItems[i]
    if (item?.children) {
      for (const child of item.children) {
        if (child.path === path) {
          return i
        }
      }
    }
  }
  return 0
}

currentMainMenu.value = findParentMenuIndex(route.path)

watch(() => route.path, (newPath) => {
  activeMenu.value = newPath
  const parentTitle = findParentMenuTitle(newPath)
  if (parentTitle && !expandedMenus.value.includes(parentTitle)) {
    expandedMenus.value.push(parentTitle)
  }
  currentMainMenu.value = findParentMenuIndex(newPath)
})

const isMixed = computed(() => props.layout === 'mixed')
const isIcon = computed(() => props.layout === 'icon')
const isTop = computed(() => props.layout === 'top')

function isActive(path: string): boolean {
  if (!path) return false
  return route.path === path || route.path.startsWith(path + '/')
}

/**
 * 判断父级菜单是否激活（用于有子菜单的一级菜单）
 * 规则：当前路径以该路径开头即激活（包含自身和所有子路径）
 */
function isActiveParent(path: string): boolean {
  if (!path) return false
  return route.path === path || route.path.startsWith(path + '/')
}

/**
 * 判断子级菜单是否激活
 * 规则：
 * 1. 精确匹配当前路径
 * 2. 或者是当前路径的子路径
 * 特殊处理：如果子级路径与父级路径相同，只有精确匹配时才激活（避免父子同时高亮）
 */
function isActiveChild(childPath: string, parentPath: string): boolean {
  if (!childPath) return false
  // 精确匹配
  if (route.path === childPath) return true
  // 子路径匹配（但排除与父级路径相同的情况）
  if (childPath !== parentPath && route.path.startsWith(childPath + '/')) return true
  return false
}

function isMenuExpanded(title: string): boolean {
  return expandedMenus.value.includes(title)
}

function toggleMenu(title: string) {
  const index = expandedMenus.value.indexOf(title)
  if (index > -1) {
    expandedMenus.value.splice(index, 1)
  } else {
    expandedMenus.value.push(title)
  }
}

function handleMenuClick(item: MenuItem) {
  if (item.children && item.children.length > 0) {
    toggleMenu(item.title)
  } else if (item.path) {
    activeMenu.value = item.path
    router.push(item.path)
  }
}

function handleSubMenuClick(path: string) {
  activeSubMenu.value = path
  activeMenu.value = path
  router.push(path)
}

// 混合/双列模式专用
function handleMainMenuClick(index: number, item: MenuItem) {
  currentMainMenu.value = index
}

const currentSubMenuItems = computed(() => {
  const currentItem = props.menuItems[currentMainMenu.value]
  return currentItem?.children || []
})

/** 当前选中父级菜单的路径（用于子级菜单高亮判断） */
const currentParentPath = computed(() => {
  return props.menuItems[currentMainMenu.value]?.path || ''
})
</script>

<template>
  <div class="sidebar-wrapper">
    <!-- ========== 风格1: 左侧菜单（ADPX 风格） ========== -->
    <aside
      v-if="layout === 'left' || !layout"
      class="sidebar sidebar--left"
      :class="{ 'sidebar--collapsed': collapsed }"
    >
      <!-- Logo区域（可配置） -->
      <div class="sidebar__logo">
        <div class="logo-icon">
          <el-icon :size="24"><component :is="logoConfig.icon" /></el-icon>
        </div>
        <span v-if="!collapsed" class="logo-text">{{ logoConfig.title }}</span>
      </div>

      <!-- 菜单区域（ADPX 风格） -->
      <nav class="sidebar__menu" :class="{ 'is-collapsed': collapsed }">
        <div v-for="item in menuItems" :key="item.title" class="menu-wrapper">
          <!-- 一级菜单 -->
          <div
            class="menu-item"
            :class="{
              'menu-item--active': item.path && isActiveParent(item.path),
              'menu-item--has-children': item.children && item.children.length > 0,
              'menu-item--expanded': isMenuExpanded(item.title)
            }"
            @click="handleMenuClick(item)"
          >
            <!-- 图标容器（ADPX 风格） -->
            <div class="menu-icon flex-cc">
              <el-icon :size="20">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <!-- 文字（收缩时隐藏） -->
            <span v-if="!collapsed" class="menu-name">{{ item.title }}</span>
            <!-- 箭头（收缩时隐藏） -->
            <el-icon
              v-if="!collapsed && item.children && item.children.length > 0"
              class="menu-arrow"
              :class="{ 'is-expanded': isMenuExpanded(item.title) }"
            >
              <ArrowDown />
            </el-icon>
          </div>

          <!-- 二级菜单（展开状态） -->
          <div
            v-if="!collapsed && item.children && isMenuExpanded(item.title)"
            class="sub-menu"
          >
            <div
              v-for="child in item.children"
              :key="child.title"
              class="sub-menu-item"
              :class="{ 'sub-menu-item--active': child.path && isActiveChild(child.path, item.path || '') }"
              @click="child.path && handleSubMenuClick(child.path)"
            >
              <span class="sub-menu-item__dot" />
              <span class="sub-menu-item__title">{{ child.title }}</span>
            </div>
          </div>
        </div>
      </nav>

    </aside>

    <!-- ========== 风格2: 顶部菜单 ========== -->
    <nav
      v-else-if="isTop"
      class="top-menu"
    >
      <div class="top-menu__logo">
        <el-icon :size="24"><Grid /></el-icon>
        <span>管理系统</span>
      </div>
      <div class="top-menu__items">
        <div
          v-for="item in menuItems"
          :key="item.title"
          class="top-menu-item"
          :class="{ 'top-menu-item--active': item.path && isActiveParent(item.path) }"
          @click="handleMenuClick(item)"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </nav>

    <!-- ========== 风格3: 混合菜单（ADPX 风格） ========== -->
    <template v-else-if="isMixed">
      <!-- 左侧图标栏 -->
      <aside class="sidebar sidebar--mixed-icon">
        <div class="mixed-icon__logo">
          <el-icon :size="24"><component :is="logoConfig.icon" /></el-icon>
        </div>
        <nav class="mixed-icon__menu">
          <transition-group name="menu-fade">
            <el-tooltip
              v-for="(item, index) in menuItems"
              :key="item.title"
              :content="item.title"
              placement="right"
              :offset="8"
            >
              <div
                class="mixed-icon-item"
                :class="{ 'mixed-icon-item--active': currentMainMenu === index }"
                @click="handleMainMenuClick(index, item)"
              >
                <el-icon :size="20"><component :is="item.icon" /></el-icon>
              </div>
            </el-tooltip>
          </transition-group>
        </nav>
      </aside>

      <!-- 右侧内容区（水平子菜单） -->
      <div class="sidebar sidebar--mixed-content">
        <!-- 水平子菜单（图标+文字） -->
        <nav class="mixed-content__menu">
          <transition-group name="menu-fade">
            <div
              v-for="child in currentSubMenuItems"
              :key="child.title"
              class="mixed-content-item"
              :class="{ 'mixed-content-item--active': child.path && isActiveChild(child.path, currentParentPath) }"
              @click="child.path && handleSubMenuClick(child.path)"
            >
              <el-icon :size="20"><component :is="child.icon" /></el-icon>
              <span class="mixed-content-item__title">{{ child.title }}</span>
            </div>
          </transition-group>
        </nav>
      </div>
    </template>

    <!-- ========== 风格4: 双列图标菜单（左侧父级 + 右侧子级） ========== -->
    <template v-else-if="isIcon">
      <!-- 左侧父级图标栏 -->
      <aside class="sidebar sidebar--icon-parent">
        <div class="icon-parent__logo">
          <el-icon :size="24"><component :is="logoConfig.icon" /></el-icon>
        </div>
        <nav class="icon-parent__menu">
          <el-tooltip
            v-for="(item, index) in menuItems"
            :key="item.title"
            :content="item.title"
            placement="right"
            :offset="8"
          >
            <div
              class="icon-parent-item"
              :class="{ 'icon-parent-item--active': currentMainMenu === index }"
              @click="handleMainMenuClick(index, item)"
            >
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
          </el-tooltip>
        </nav>
      </aside>

      <!-- 右侧子级图标栏（和左侧完全一样的样式） -->
      <div class="sidebar sidebar--icon-child">
        <nav class="icon-child__menu">
          <el-tooltip
            v-for="child in currentSubMenuItems"
            :key="child.title"
            :content="child.title"
            placement="right"
            :offset="8"
          >
            <div
              class="icon-child-item"
              :class="{ 'icon-child-item--active': child.path && isActiveChild(child.path, currentParentPath) }"
              @click="child.path && handleSubMenuClick(child.path)"
            >
              <el-icon :size="20"><component :is="child.icon" /></el-icon>
            </div>
          </el-tooltip>
        </nav>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
// ========== 基础样式 ==========
.sidebar-wrapper {
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  z-index: 100;
  display: flex;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar {
  height: 100vh;
  background: var(--fts-bg-card);
  border-right: 1px solid var(--fts-border-secondary);
  display: flex;
  flex-direction: column;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

// ========== 风格1: 左侧菜单（ADPX 风格） ==========
.sidebar--left {
  width: 264px;

  &.sidebar--collapsed {
    width: 64px;
  }
}

.sidebar__logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-secondary);
  gap: var(--fts-space-3);
  flex-shrink: 0;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: var(--fts-primary);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.logo-text {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  white-space: nowrap;
}

// 菜单区域（ADPX 风格）
.sidebar__menu {
  flex: 1;
  padding: var(--fts-space-3);
  overflow-y: auto;
}

.menu-wrapper {
  margin-bottom: var(--fts-space-1);
}

// ADPX 风格菜单项
.menu-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  color: var(--fts-text-secondary);
  position: relative;
  min-height: 44px;

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);
  }

  &--has-children {
    cursor: pointer;
  }

  &--expanded {
    background: var(--fts-bg-hover);
  }
}

// ADPX 风格图标容器
.menu-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .el-icon {
    font-size: 20px;
  }
}

// ADPX 风格文字
.menu-name {
  font-size: var(--fts-font-size-base);
  white-space: nowrap;
  flex: 1;
  transition: opacity var(--fts-duration-fast);
}

// ADPX 风格箭头
.menu-arrow {
  transition: transform var(--fts-duration-fast);
  font-size: 12px;
  flex-shrink: 0;

  &.is-expanded {
    transform: rotate(180deg);
  }
}

// 收缩状态（ADPX 风格）
.sidebar--collapsed {
  .menu-item {
    justify-content: center;
    padding: var(--fts-space-3);
  }

  .menu-icon {
    margin: 0;
  }

  .menu-name,
  .menu-arrow {
    display: none;
  }
}

// 二级菜单
.sub-menu {
  padding-left: var(--fts-space-8);
  padding-top: var(--fts-space-1);
  padding-bottom: var(--fts-space-1);
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sub-menu-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-4);
  border-radius: var(--fts-page-radius);  /* 受界面设置控制 */
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  color: var(--fts-text-secondary);
  margin-bottom: var(--fts-space-1);

  &:hover {
    color: var(--fts-text-primary);
    background: var(--fts-bg-hover);
  }

  &--active {
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);
    background: var(--fts-primary-bg);

    .sub-menu-item__dot {
      background: var(--fts-primary);
    }
  }
}

.sub-menu-item__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--fts-border-hover);
  flex-shrink: 0;
}

.sub-menu-item__title {
  font-size: var(--fts-font-size-sm);
}

// ========== 风格2: 顶部菜单 ==========
.top-menu {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  display: flex;
  align-items: center;
  padding: 0 var(--fts-space-6);
  z-index: 100;
  gap: var(--fts-space-8);
}

.top-menu__logo {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);

  .el-icon {
    color: var(--fts-primary);
  }
}

.top-menu__items {
  display: flex;
  gap: var(--fts-space-2);
  flex: 1;
}

.top-menu-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-base);

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);
  }
}

// ========== 风格3: 混合菜单（左侧图标 + 右侧文字） ==========

// 左侧图标栏
.sidebar--mixed-icon {
  width: 64px;
  align-items: center;
  padding: var(--fts-space-4) 0;
  background: var(--fts-bg-card);
}

.mixed-icon__logo {
  width: 40px;
  height: 40px;
  background: var(--fts-primary);
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: var(--fts-space-6);
  flex-shrink: 0;
}

.mixed-icon__menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  width: 100%;
  align-items: center;
  padding-top: var(--fts-space-2);
  overflow-y: auto;
}

.mixed-icon-item {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--fts-text-secondary);
  position: relative;
  overflow: hidden;

  // 激活状态指示器
  &--active::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 20px;
    background: var(--fts-primary);
    border-radius: 0 2px 2px 0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
    transform: translateX(2px);
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
  }
}

// 右侧内容区（ADPX 风格）
.sidebar--mixed-content {
  width: 200px;
  background: var(--fts-bg-card);
  border-left: 1px solid var(--fts-border-secondary);
  position: relative;
  padding: 0 var(--fts-space-4);
  overflow-y: auto;
  scrollbar-width: none;  // Firefox 隐藏滚动条

  &::-webkit-scrollbar {
    display: none;  // Chrome/Safari 隐藏滚动条
  }
}

// Logo 行
.mixed-content__logo {
  height: 64px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.mixed-content__title {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
}

// 水平子菜单
.mixed-content__menu {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  padding-top: var(--fts-space-4);
}

// 子菜单项（图标+文字，水平排列）
.mixed-content-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-base);
  position: relative;
  overflow: hidden;

  // 图标容器（提升质感）
  .el-icon {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--fts-radius-sm);
    background: transparent;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
    transform: translateX(4px);

    .el-icon {
      background: rgba(59, 130, 246, 0.08);
      transform: scale(1.1);
    }
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);

    .el-icon {
      background: rgba(59, 130, 246, 0.12);
    }

    // 激活状态左侧指示条
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 16px;
      background: var(--fts-primary);
      border-radius: 0 2px 2px 0;
    }
  }
}

.mixed-content-item__title {
  font-size: var(--fts-font-size-base);
}

// 菜单切换动画（丝滑效果）
.menu-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  transition-delay: 0.05s;
}

.menu-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: absolute;
}

.menu-fade-enter-from {
  opacity: 0;
  transform: translateX(-20px) scale(0.95);
}

.menu-fade-leave-to {
  opacity: 0;
  transform: translateX(20px) scale(0.95);
}

.menu-fade-move {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

// ========== 风格4: 双列图标菜单（左侧父级 + 右侧子级） ==========

// 左侧父级图标栏
.sidebar--icon-parent {
  width: 64px;
  align-items: center;
  padding: var(--fts-space-4) 0;
  background: var(--fts-bg-card);
}

.icon-parent__logo {
  width: 40px;
  height: 40px;
  background: var(--fts-primary);
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: var(--fts-space-6);
  flex-shrink: 0;
}

.icon-parent__menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  width: 100%;
  align-items: center;
  padding-top: var(--fts-space-2);
  overflow-y: auto;
}

.icon-parent-item {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  color: var(--fts-text-secondary);

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
  }
}

// 右侧子级图标栏（和左侧完全一样的样式）
.sidebar--icon-child {
  width: 64px;
  align-items: center;
  padding: var(--fts-space-4) 0;
  background: var(--fts-bg-card);
  border-left: 1px solid var(--fts-border-secondary);
}

.icon-child__menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  width: 100%;
  align-items: center;
  padding-top: var(--fts-space-2);
  overflow-y: auto;
}

.icon-child-item {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  color: var(--fts-text-secondary);

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
  }
}
</style>
