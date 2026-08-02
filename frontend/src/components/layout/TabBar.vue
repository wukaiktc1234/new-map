<script setup lang="ts">
/**
 * 全局标签栏组件
 *
 * 标签状态由 useTabStore 集中管理，支持持久化、右键关闭菜单。
 */
import { ref, watch, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { useTabStore, type Tab } from '@/stores/tab'

interface Props {
  sidebarWidth?: number
  tabStyle?: 'default' | 'chrome' | 'card'
}

const props = withDefaults(defineProps<Props>(), {
  sidebarWidth: 264,
  tabStyle: 'default'
})

const route = useRoute()
const router = useRouter()
const tabStore = useTabStore()

const tabBarStyle = computed(() => ({
  left: `${props.sidebarWidth}px`,
  width: `calc(100% - ${props.sidebarWidth}px)`
}))

/** 根据路由变化自动打开标签 */
watch(() => route.path, (newPath) => {
  if (route.meta.title && !tabStore.tabs.find(t => t.path === newPath)) {
    tabStore.openTab({
      name: route.name as string,
      title: route.meta.title as string,
      path: newPath
    })
  }
  tabStore.setActive(newPath)
}, { immediate: true })

function handleTabClick(tab: Tab) {
  router.push(tab.path)
}

function handleTabClose(index: number) {
  const tab = tabStore.tabs[index]
  if (!tab) return
  const fallback = tabStore.closeTab(tab.path)
  if (fallback && fallback !== route.path) {
    router.push(fallback)
  }
}

function handleCloseOthers(index: number) {
  const tab = tabStore.tabs[index]
  if (!tab) return
  tabStore.closeOthers(tab.path)
  if (tab.path !== route.path) {
    router.push(tab.path)
  }
}

function handleCloseAll() {
  tabStore.closeAll()
  if (route.path !== '/home') {
    router.push('/home')
  }
}

const contextMenu = ref({ visible: false, x: 0, y: 0, index: 0 })

function handleContextMenu(event: MouseEvent, index: number) {
  event.preventDefault()
  contextMenu.value = { visible: true, x: event.clientX, y: event.clientY, index }
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

const tabBarRef = ref<HTMLElement>()

function handleWheel(event: WheelEvent) {
  if (!tabBarRef.value) return
  event.preventDefault()
  tabBarRef.value.scrollLeft += event.deltaY
}

onMounted(() => {
  // 初始化时同步一次当前路由
  if (route.meta.title) {
    tabStore.openTab({
      name: route.name as string,
      title: route.meta.title as string,
      path: route.path
    })
  }
})
</script>

<template>
  <div ref="tabBarRef" class="tab-bar" :style="tabBarStyle" @wheel="handleWheel">
    <div
      v-for="(tab, index) in tabStore.tabs"
      :key="tab.path"
      class="tab-item"
      :class="[
        { 'tab-item--active': tabStore.activePath === tab.path },
        `tab-item--${tabStyle}`
      ]"
      @click="handleTabClick(tab)"
      @contextmenu="handleContextMenu($event, index)"
    >
      <span class="tab-item__title">{{ tab.title }}</span>
      <el-icon
        v-if="tabStore.tabs.length > 1"
        class="tab-item__close"
        :size="12"
        @click.stop="handleTabClose(index)"
      >
        <Close />
      </el-icon>
    </div>

    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="tab-context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <div class="tab-context-menu__item" @click="handleTabClose(contextMenu.index); closeContextMenu()">
          <span>关闭</span>
        </div>
        <div class="tab-context-menu__item" @click="handleCloseOthers(contextMenu.index); closeContextMenu()">
          <span>关闭其他</span>
        </div>
        <div class="tab-context-menu__divider" />
        <div class="tab-context-menu__item tab-context-menu__item--danger" @click="handleCloseAll(); closeContextMenu()">
          <span>关闭所有</span>
        </div>
      </div>
    </Teleport>

    <div v-if="contextMenu.visible" class="tab-context-overlay" @click="closeContextMenu" />
  </div>
</template>

<style scoped lang="scss">
.tab-bar {
  height: 40px;
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  display: flex;
  align-items: center;
  padding: 0 var(--fts-space-4);
  gap: var(--fts-space-1);
  position: fixed;
  top: 64px;
  z-index: 99;
  transition: left 0.4s cubic-bezier(0.4, 0, 0.2, 1),
              width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: hidden;

  &::-webkit-scrollbar {
    display: none;
  }
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-page-radius);
  cursor: pointer;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  transition: all var(--fts-duration-fast);
  border: 1px solid transparent;
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    background: var(--fts-bg-hover);
    color: var(--fts-text-primary);
  }

  &--active {
    background: var(--fts-primary-bg);
    color: var(--fts-primary);
    border-color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);
  }

  &--default {
    border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
    border-bottom: none;
  }

  &--chrome {
    border-radius: 12px 12px 0 0;
    position: relative;

    &::before,
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      width: 12px;
      height: 12px;
    }

    &::before {
      left: -12px;
      border-bottom-right-radius: 12px;
      box-shadow: 2px 2px 0 var(--fts-bg-card);
    }

    &::after {
      right: -12px;
      border-bottom-left-radius: 12px;
      box-shadow: -2px 2px 0 var(--fts-bg-card);
    }
  }

  &--card {
    border-radius: var(--fts-page-radius);
    border: 1px solid var(--fts-border-secondary);

    &.tab-item--active {
      border-color: var(--fts-primary);
    }
  }
}

.tab-item__title {
  white-space: nowrap;
}

.tab-item__close {
  opacity: 0.6;
  transition: opacity var(--fts-duration-fast);

  &:hover {
    opacity: 1;
    color: var(--fts-error);
  }
}

.tab-context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  box-shadow: var(--fts-shadow-lg);
  padding: var(--fts-space-1) 0;
}

.tab-context-menu__item {
  padding: var(--fts-space-2) var(--fts-space-4);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  cursor: pointer;
  transition: background var(--fts-duration-fast);

  &:hover {
    background: var(--fts-bg-hover);
  }

  &--danger {
    color: var(--fts-error);

    &:hover {
      background: var(--fts-error-bg);
    }
  }
}

.tab-context-menu__divider {
  height: 1px;
  background: var(--fts-border-secondary);
  margin: var(--fts-space-1) 0;
}

.tab-context-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
}
</style>
