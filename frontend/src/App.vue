<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MainLayout from '@/components/layout/MainLayout.vue'

const route = useRoute()

// 独立页面白名单：这些页面不使用MainLayout布局（全屏独立页面）
const standalonePages = ['/login', '/forgot-password']
const isStandalone = computed(() => {
  return standalonePages.some(p => route.path.startsWith(p))
})
</script>

<template>
  <div id="app">
    <!-- 独立页面（如登录页、忘记密码页）：直接渲染router-view，无侧边栏/顶栏 -->
    <template v-if="isStandalone">
      <router-view />
    </template>
    <!-- 其他页面：使用MainLayout布局 -->
    <template v-else>
      <MainLayout />
    </template>
  </div>
</template>

<style lang="scss">
// 全局样式已在 main.ts 中导入
</style>
