<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Fold, Search } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'

/* ========== 组件属性与事件 ========== */
interface Props {
  collapsed: boolean
}
const props = withDefaults(defineProps<Props>(), {
  collapsed: false,
})

const emit = defineEmits<{
  (e: 'update:collapsed', value: boolean): void
  (e: 'toggle'): void
  (e: 'create-approval'): void
}>()

/* ========== 路由实例（用于激活态判断） ========== */
const route = useRoute()

/* ========== 权限Store（导航数据源） ========== */
const permission = usePermissionStore()

/** 主导航模块（category=main，当前角色可见） */
const mainNav = computed(() => permission.getMainNav())

/** 次级导航模块（category=secondary，当前角色可见） */
const secondaryNav = computed(() => permission.getSecondaryNav())

/** 是否有次级导航（控制分隔线显示） */
const hasSecondaryNav = computed(() => secondaryNav.value.length > 0)

/* ========== 全局搜索 ========== */
const searchQuery = ref('')
const showSearchResults = ref(false)

function handleSearch() {
  if (searchQuery.value.trim()) {
    // TODO: 实现全局搜索逻辑
  }
}

/* ========== 折叠切换 ========== */
function toggleSidebar() {
  emit('toggle')
  emit('update:collapsed', !props.collapsed)
}

/* ========== 路由激活判断 ========== */
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
</script>

<template>
  <aside
    class="pc-sidebar"
    :class="{ 'pc-sidebar--collapsed': collapsed }"
  >
    <!-- 品牌区：含折叠按钮 -->
    <div class="pc-sidebar__brand">
      <div class="brand-logo">
        <el-icon :size="18" class="brand-logo__icon"><HomeFilled /></el-icon>
      </div>
      <transition name="sidebar-text">
        <span v-show="!collapsed" class="brand-text">员工自助门户</span>
      </transition>
      <!-- 折叠/展开按钮 -->
      <button
        class="pc-sidebar__toggle"
        :title="collapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="toggleSidebar"
      >
        <el-icon :size="14" :class="{ 'pc-sidebar__toggle--rotated': collapsed }">
          <Fold />
        </el-icon>
      </button>
    </div>

    <!-- 全局搜索框（MAC风格） -->
    <div v-show="!collapsed" class="pc-sidebar__search">
      <div class="search-box">
        <el-icon :size="14" class="search-icon"><Search /></el-icon>
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="搜索功能、文档..."
          @keyup.enter="handleSearch"
          @focus="showSearchResults = true"
          @blur="showSearchResults = false"
        />
        <kbd class="search-kbd">⌘K</kbd>
      </div>
    </div>

    <!-- 主导航区（动态渲染，由权限Store驱动） -->
    <nav class="pc-sidebar__nav">
      <!-- 主导航模块 -->
      <router-link
        v-for="item in mainNav"
        :key="item.id"
        :to="item.route"
        :class="['pc-nav-item', { 'pc-nav-item--active': isActive(item.route) }]"
        :title="collapsed ? item.label : undefined"
      >
        <span class="pc-nav-item__icon-wrap">
          <el-icon :size="20" class="pc-nav-item__icon"><component :is="item.icon" /></el-icon>
        </span>
        <transition name="sidebar-text">
          <span v-show="!collapsed" class="pc-nav-item__text">{{ item.label }}</span>
        </transition>
      </router-link>

      <!-- 分隔线：仅在存在次级导航时显示 -->
      <div v-if="hasSecondaryNav" class="pc-nav-divider" />

      <!-- 次级导航区（缩进显示，仅当有可见项时渲染） -->
      <template v-if="hasSecondaryNav">
        <component
          :is="item.id === 'create_approval' ? 'div' : 'router-link'"
          v-for="item in secondaryNav"
          :key="item.id"
          v-bind="item.id === 'create_approval' ? {} : { to: item.route }"
          :class="['pc-nav-item', 'pc-nav-item--secondary', { 'pc-nav-item--active': isActive(item.route) }]"
          :title="collapsed ? item.label : undefined"
          @click="item.id === 'create_approval' && emit('create-approval')"
        >
          <span class="pc-nav-item__icon-wrap">
            <el-icon :size="18" class="pc-nav-item__icon"><component :is="item.icon" /></el-icon>
          </span>
          <transition name="sidebar-text">
            <span v-show="!collapsed" class="pc-nav-item__text">{{ item.label }}</span>
          </transition>
        </component>
      </template>

      <!-- 底部分隔线 -->
      <div class="pc-nav-divider" />
    </nav>

    <!-- 底部版本信息 -->
    <div class="pc-sidebar__footer">
      <transition name="sidebar-text">
        <span v-show="!collapsed" class="version-text">v1.0.0</span>
      </transition>
    </div>
  </aside>
</template>

<style scoped lang="scss">
// ================================================================
//  PcSidebar — PC端MAC毛玻璃侧边栏（从 EmployeeLayout 拆分）
//  所有颜色通过 var(--fts-xxx) CSS 变量引用，自动适配浅色/深色主题。
//  导航内容由 permission Store 动态生成，实现千人千面。
// ================================================================

.pc-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  width: 240px;
  display: flex;
  flex-direction: column;

  /* MAC毛玻璃效果 */
  background: var(--fts-bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-right: 1px solid var(--fts-border-glass);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &--collapsed {
    width: 64px;
  }

  /* 移动端隐藏 */
  @media (max-width: 767px) {
    display: none !important;
  }
}

/* 品牌区：Logo + 名称 + 折叠按钮 */
.pc-sidebar__brand {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 12px 0 16px;
  border-bottom: 1px solid var(--fts-border-primary);
  flex-shrink: 0;
  gap: 10px;
  overflow: hidden;
}

.brand-logo {
  width: 28px;
  height: 28px;
  border-radius: var(--fts-radius-sm);
  background: var(--fts-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &__icon {
    color: var(--fts-primary);
  }
}

.brand-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-secondary);
  letter-spacing: 0.3px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 折叠按钮 */
.pc-sidebar__toggle {
  margin-left: auto;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  color: var(--fts-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &:active {
    transform: scale(0.9);
  }

  &--rotated {
    transform: rotate(180deg);
  }
}

// ---------- 全局搜索框（MAC风格） ----------
.pc-sidebar__search {
  padding: 8px 16px 12px;
  flex-shrink: 0;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px;
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-hover);
  border: 1px solid transparent;
  transition: all var(--fts-duration-normal) var(--fts-easing-default);

  &:hover,
  &:focus-within {
    background: var(--fts-bg-card);
    border-color: var(--fts-border-primary);
    box-shadow: var(--fts-shadow-sm);
  }

  /* 搜索框focus呼吸灯效果（柔和脉冲光晕） */
  &:focus-within {
    border-color: var(--fts-search-focus-border-color, var(--fts-primary));
    box-shadow: var(--fts-search-focus-glow, 0 0 0 3px rgba(var(--fts-primary-rgb), 0.1), 0 0 20px rgba(var(--fts-primary-rgb), 0.08));
  }
}

.search-icon {
  color: var(--fts-text-quaternary);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--fts-text-primary);
  min-width: 0;

  &::placeholder {
    color: var(--fts-text-quaternary);
  }
}

.search-kbd {
  font-size: 10px;
  font-weight: 600;
  color: var(--fts-text-quaternary);
  background: var(--fts-bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

/* 导航滚动区域 */
.pc-sidebar__nav {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
  overflow-x: hidden;

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb {
    background: var(--fts-text-quaternary);
    border-radius: 2px;
  }

  /* 当没有次级导航时，主导航自动填满空间 */
  &:has(.pc-nav-item:not(.pc-nav-item--secondary):last-child) {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
  }
}

/* ---------- 导航菜单项 ---------- */
.pc-nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 40px;
  padding: 0 16px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--fts-text-secondary);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 2px;
  overflow: hidden;
  border-radius: 0 var(--fts-radius-md) var(--fts-radius-md) 0;

  /* 图标包装器（用于背景高亮） */
  &__icon-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: var(--fts-radius-sm);
    flex-shrink: 0;
    transition: all 0.2s ease;
  }

  &__icon {
    color: var(--fts-text-tertiary);
    transition: all 0.2s ease;
  }

  &__text {
    white-space: nowrap;
    flex-shrink: 0;
  }

  &:hover {
    color: var(--fts-text-primary);
    background: var(--fts-bg-hover);

    .pc-nav-item__icon-wrap {
      background: rgba(var(--fts-primary-rgb), 0.08);
    }

    .pc-nav-item__icon {
      color: var(--fts-primary);
      transform: scale(1.05);
    }
  }

  /* 激活态：蓝色图标背景 + 主色文字 + 浅蓝背景 */
  &--active {
    color: var(--fts-primary);
    font-weight: 600;
    background: rgba(var(--fts-primary-rgb), 0.06);

    .pc-nav-item__icon-wrap {
      background: rgba(var(--fts-primary-rgb), 0.12);
    }

    .pc-nav-item__icon {
      color: var(--fts-primary);
    }
  }

  /* 次级导航：缩进 + 字号略小 */
  &--secondary {
    padding-left: 24px;
    font-size: 13px;

    .pc-nav-item__icon {
      font-size: 18px;
    }
  }

  &__icon {
    flex-shrink: 0;
    transition: transform 0.2s ease;
  }

  &__text {
    white-space: nowrap;
    flex-shrink: 0;
  }
}

/* 分隔线 */
.pc-nav-divider {
  height: 1px;
  background: var(--fts-border-primary);
  margin: 8px 16px;
}

/* 底部版本信息 */
.pc-sidebar__footer {
  padding: 12px 0;
  border-top: 1px solid var(--fts-border-primary);
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  overflow: hidden;
}

.version-text {
  font-size: 10px;
  color: var(--fts-text-quaternary);
  font-weight: 500;
  white-space: nowrap;
}

// ================================================================
//  过渡动画
// ================================================================

/* 侧边栏文字显隐过渡（折叠时淡出） */
.sidebar-text-enter-active,
.sidebar-text-leave-active {
  transition: opacity 0.15s ease;
}
.sidebar-text-enter-from,
.sidebar-text-leave-to {
  opacity: 0;
}
</style>
