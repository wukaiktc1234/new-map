/**
 * 顶部栏 — 含logo、导航、主题切换等
 */
<script setup lang="ts">
import { computed } from 'vue'
import { Sunny, Moon, Fold, Expand } from '@element-plus/icons-vue'
import { useLayoutStore } from '@/stores/layout'

const store = useLayoutStore()

/** 当前是否为深色主题 */
const isDark = computed(() => store.themeMode === 'dark')

/** 切换浅色/深色主题 */
function toggleTheme(): void {
  store.setThemeMode(isDark.value ? 'light' : 'dark')
}
</script>

<template>
  <header class="fts-topbar">
    <div class="fts-topbar__left">
      <!-- 收缩按钮（仅左侧菜单模式显示） -->
      <el-icon
        v-if="store.menuLayout === 'left'"
        :size="18"
        style="cursor:pointer;color:var(--fts-text-secondary);"
        @click="store.toggleSidebar()"
      >
        <Fold v-if="!store.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
    </div>

    <div class="fts-topbar__right">
      <!-- 主题切换 -->
      <el-button
        link
        :icon="isDark ? Sunny : Moon"
        @click="toggleTheme()"
        style="color:var(--fts-text-secondary);font-size:18px;"
      />

      <!-- 布局切换 -->
      <el-dropdown trigger="click" @command="(cmd: string) => store.setMenuLayout(cmd as any)">
        <el-button link style="color:var(--fts-text-secondary);font-size:13px;">
          布局 ▾
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="left">左侧菜单</el-dropdown-item>
            <el-dropdown-item command="icon">双列图标</el-dropdown-item>
            <el-dropdown-item command="mixed">混合菜单</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>