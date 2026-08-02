<script setup lang="ts">
import type { Component } from 'vue'

interface TabItem {
  path: string
  label: string
  icon: Component
}

interface Props {
  tabs: TabItem[]
  /** 判断给定path是否为当前激活Tab */
  isActive: (path: string) => boolean
}

defineProps<Props>()
</script>

<template>
  <nav class="mobile-tab-bar">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="tab.path"
      :class="['mobile-tab-bar__tab', { 'mobile-tab-bar__tab--active': isActive(tab.path) }]"
    >
      <span class="mobile-tab-bar__icon-wrap">
        <el-icon :size="24"><component :is="tab.icon" /></el-icon>
      </span>
      <span class="mobile-tab-bar__label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped lang="scss">
/* ================================================================
   MobileTabBar — 移动端底部导航栏（iOS原生质感）
   纯展示组件，通过 props 驱动，零业务依赖。
   所有颜色、圆角、阴影均通过 var(--fts-xxx) CSS 变量引用，
   自动适配浅色/深色主题。
   ================================================================ */

.mobile-tab-bar {
  display: none;

  @media (max-width: 767px) {
    display: flex;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 200;
    justify-content: space-around;
    align-items: center;
    height: var(--fts-tabbar-height);
    height: var(--fts-tabbar-height-safe);
    padding-bottom: env(safe-area-inset-bottom);

    /* iOS风格毛玻璃背景 */
    background: var(--fts-bg-glass);
    backdrop-filter: blur(20px) saturate(180%);
    -webkit-backdrop-filter: blur(20px) saturate(180%);

    border-top: 0.5px solid var(--fts-border-glass);
    box-shadow: var(--fts-shadow-tabbar, 0 -1px 8px rgba(0, 0, 0, 0.04));
  }
}

/* 暗色模式TabBar已移至PageContainer全局样式块 */

/* ---------- Tab项 ---------- */
.mobile-tab-bar__tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  flex: 1;
  min-width: 56px;
  max-width: 80px;
  color: var(--fts-text-secondary);
  text-decoration: none;
  padding: var(--fts-space-2) var(--fts-space-2) var(--fts-space-1);
  -webkit-tap-highlight-color: transparent;
  transition: color var(--fts-tab-transition-duration) var(--fts-easing-default);
  position: relative;

  &__icon-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    width: var(--fts-tab-icon-size);
    height: var(--fts-tab-icon-size);
    border-radius: var(--fts-radius-md, 8px);
    transition: transform var(--fts-tab-transition-duration) var(--fts-tab-transition-easing),
                background-color var(--fts-tab-transition-duration) var(--fts-easing-default);
  }

  &__label {
    font-size: var(--fts-tab-label-size);
    font-weight: 500;
    line-height: 1.3;
    letter-spacing: 0.02em;
    transition: font-weight var(--fts-duration-fast) ease;
  }

  &:active {
    opacity: 0.7;

    .mobile-tab-bar__icon-wrap {
      transform: scale(0.88);
      transition-duration: var(--fts-touch-duration-press);
    }
  }

  /* 激活态：主色 + 背景胶囊 + 弹跳动画 */
  &--active {
    color: var(--fts-primary);

    &::before {
      content: '';
      position: absolute;
      top: 4px;
      left: 50%;
      transform: translateX(-50%);
      width: 24px;
      height: 3px;
      border-radius: 2px;
      background: var(--fts-primary);
      opacity: 0.9;
      transition: background-color var(--fts-duration-normal) var(--fts-easing-default), opacity var(--fts-duration-normal) var(--fts-easing-default);
    }

    .mobile-tab-bar__icon-wrap {
      transform: scale(var(--fts-tab-active-scale));
      background-color: rgba(var(--fts-primary-rgb), 0.08);
      animation: tabbar-icon-bounce 500ms var(--fts-easing-bounce);
    }

    .mobile-tab-bar__label {
      font-weight: 700;
    }
  }

  &:hover:not(&--active) {
    color: var(--fts-text-primary);

    .mobile-tab-bar__icon-wrap {
      background-color: var(--fts-bg-tertiary);
    }
  }
}

@keyframes tabbar-icon-bounce {
  0% {
    transform: scale(var(--fts-tab-active-scale)) translateY(0);
  }
  40% {
    transform: scale(var(--fts-tab-active-scale)) translateY(-6px);
  }
  60% {
    transform: scale(var(--fts-tab-active-scale)) translateY(2px);
  }
  80% {
    transform: scale(var(--fts-tab-active-scale)) translateY(-1px);
  }
  100% {
    transform: scale(var(--fts-tab-active-scale)) translateY(0);
  }
}
</style>
