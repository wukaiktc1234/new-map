<script setup lang="ts">
import type { Component } from 'vue'
import { HomeFilled } from '@element-plus/icons-vue'

/** 导航项数据结构 */
export interface NavItem {
  id: string
  label: string
  icon: Component
  route: string
  secondary?: boolean
}

interface Props {
  /** 抽屉显隐状态 */
  visible: boolean
  /** 导航列表数据 */
  navItems: NavItem[]
  /** 当前激活的路由路径 */
  activePath: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'navigate', route: string): void
}>()

/** 判断导航项是否为激活状态 */
function isActive(route: string): boolean {
  if (route === '/home') {
    return props.activePath === '/' || props.activePath === '/home'
  }
  return props.activePath.startsWith(route)
}

/** 点击遮罩层关闭抽屉 */
function onOverlayClick() {
  emit('close')
}

/** 点击关闭按钮 */
function onCloseClick() {
  emit('close')
}

/** 点击导航项：通知父组件路由跳转并关闭抽屉 */
function onNavClick(item: NavItem) {
  emit('navigate', item.route)
  emit('close')
}
</script>

<template>
  <!-- 遮罩层 -->
  <transition name="fade">
    <div
      v-if="visible"
      class="mobile-drawer__overlay"
      @click="onOverlayClick"
    />
  </transition>

  <!-- 抽屉面板 -->
  <transition name="slide-right">
    <aside v-if="visible" class="mobile-drawer">
      <!-- 头部：品牌logo + 名称 + 关闭按钮 -->
      <div class="mobile-drawer__header">
        <div class="mobile-drawer__brand-logo">
          <el-icon :size="18" class="mobile-drawer__brand-icon"><HomeFilled /></el-icon>
        </div>
        <span class="mobile-drawer__brand-text">员工自助门户</span>
        <button
          class="mobile-drawer__close-btn"
          title="关闭"
          @click="onCloseClick"
        >✕</button>
      </div>

      <!-- 导航列表 -->
      <nav class="mobile-drawer__nav">
        <button
          v-for="item in navItems"
          :key="item.id"
          :class="[
            'mobile-drawer__nav-item',
            { 'mobile-drawer__nav-item--secondary': item.secondary },
            { 'mobile-drawer__nav-item--active': isActive(item.route) }
          ]"
          @click="onNavClick(item)"
        >
          <el-icon :size="item.secondary ? 18 : 20">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </aside>
  </transition>
</template>

<style scoped lang="scss">
// ---------- 遮罩层 ----------
.mobile-drawer__overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 298;
  background: var(--fts-overlay-backdrop, rgba(0, 0, 0, 0.3));
  backdrop-filter: blur(4px);

  @media (min-width: 768px) {
    display: none !important;
  }
}

// ---------- 抽屉面板 ----------
.mobile-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 299;
  width: 280px;
  display: flex;
  flex-direction: column;
  background: var(--fts-bg-card);
  box-shadow: var(--fts-shadow-xl);
  overflow-y: auto;

  @media (min-width: 768px) {
    display: none !important;
  }

  // ---- 头部 ----
  &__header {
    display: flex;
    align-items: center;
    gap: 10px;
    height: 56px;
    padding: 0 16px;
    border-bottom: 1px solid var(--fts-border-primary);
    flex-shrink: 0;
  }

  &__brand-logo {
    width: 28px;
    height: 28px;
    border-radius: var(--fts-radius-sm);
    background: var(--fts-primary-light);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__brand-icon {
    color: var(--fts-primary);
  }

  &__brand-text {
    font-size: 15px;
    font-weight: 600;
    color: var(--fts-text-primary);
    flex: 1;
  }

  // ---- 关闭按钮 ----
  &__close-btn {
    width: 28px;
    height: 28px;
    border: none;
    border-radius: var(--fts-radius-sm);
    background: transparent;
    color: var(--fts-text-tertiary);
    font-size: 18px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease, color 0.2s ease;

    &:hover {
      background: var(--fts-bg-page);
      color: var(--fts-text-primary);
    }

    &:active {
      transform: scale(0.92);
    }
  }

  // ---- 导航列表 ----
  &__nav {
    flex: 1;
    padding: 8px 0;
  }

  &__nav-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4, 12px);
    height: 48px;
    padding: 0 16px;
    border: none;
    width: 100%;
    text-align: left;
    text-decoration: none;
    color: inherit;
    transition: var(--fts-transition-all, all 0.2s ease);
    position: relative;
    -webkit-tap-highlight-color: transparent;
    cursor: pointer;

    &--active {
      background: var(--fts-bg-secondary, rgba(var(--fts-primary-rgb), 0.06));
      color: var(--fts-primary);

      &::after {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 20px;
        background: var(--fts-primary);
        border-radius: 0 4px 4px 0;
      }

      span {
        font-weight: 700;
      }
    }

    &:active:not(&--active) {
      background: var(--fts-bg-secondary, var(--fts-bg-page));
    }

    &--secondary {
      padding-left: 24px;
      font-size: 14px;
    }

    .el-icon {
      font-size: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
    }

    span {
      font-size: var(--fts-font-size-base, 15px);
      font-weight: 500;
      transition: var(--fts-transition-colors, color 0.2s ease);
    }
  }
}

// ---------- 过渡动画 ----------

/* 遮罩层淡入淡出（增强版：更平滑的缓动） */
.fade-enter-active {
  transition: opacity var(--fts-duration-normal, 300ms) var(--fts-easing-smooth, cubic-bezier(0.4, 0, 0.2, 1));
}

.fade-leave-active {
  transition: opacity var(--fts-duration-fast, 150ms) var(--fts-ease-in, cubic-bezier(0.4, 0, 1, 1));
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 抽屉从左侧滑入（增强版：spring弹性缓动） */
.slide-right-enter-active {
  transition: transform var(--fts-duration-slow, 500ms) var(--fts-easing-spring, cubic-bezier(0.32, 0.72, 0, 1));
}

.slide-right-leave-active {
  transition: transform var(--fts-duration-normal, 300ms) var(--fts-ease-in-out, cubic-bezier(0.4, 0, 0.2, 1));
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(-100%);
}
</style>
