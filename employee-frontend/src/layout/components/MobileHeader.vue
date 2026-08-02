<script setup lang="ts">
/**
 * MobileHeader — 移动端专用顶部导航栏
 *
 * 仅在视口宽度 <= 767px 时显示（与 PcTopbar 互补）
 * 解决问题：PcTopbar 在移动端 display:none 导致无任何头部
 *
 * 布局：[汉堡菜单] [页面标题] [右侧操作区]
 * 支持 iOS 安全区域（刘海屏适配）
 */
import { ArrowLeft } from '@element-plus/icons-vue'

interface Props {
  /** 页面标题 */
  title?: string
  /** 是否显示返回按钮 */
  showBack?: boolean
  /** 是否显示汉堡菜单 */
  showMenu?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  showBack: false,
  showMenu: true,
})

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'openMenu'): void
}>()
</script>

<template>
  <header class="mobile-header">
    <!-- 左侧操作区 -->
    <div class="mobile-header__left">
      <!-- 返回按钮 -->
      <button
        v-if="showBack"
        class="mobile-header__btn"
        @click="emit('back')"
        aria-label="返回"
      >
        <el-icon :size="18"><ArrowLeft /></el-icon>
      </button>

      <!-- 汉堡菜单 -->
      <button
        v-if="showMenu"
        class="mobile-header__btn mobile-header__menu-btn"
        @click="emit('openMenu')"
        aria-label="打开菜单"
      >
        <span class="mobile-header__hamburger-line"></span>
        <span class="mobile-header__hamburger-line"></span>
        <span class="mobile-header__hamburger-line"></span>
      </button>
    </div>

    <!-- 中间标题 -->
    <h1 class="mobile-header__title">{{ title || '员工门户' }}</h1>

    <!-- 右侧占位（保持标题居中） -->
    <div class="mobile-header__right"></div>
  </header>
</template>

<style scoped lang="scss">
/* ================================================================
   MobileHeader — 移动端顶部导航栏（仅移动端可见）
   ================================================================ */

.mobile-header {
  /* 默认隐藏，仅移动端显示 — 与 PcTopbar 互补 */
  display: none;

  position: sticky;
  top: 0;
  z-index: 200;
  align-items: center;
  justify-content: space-between;

  height: var(--fts-mobile-header-height, 48px);
  padding: 0 var(--fts-space-3);
  padding-top: env(safe-area-inset-top, 0);

  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);

  flex-shrink: 0;
  gap: var(--fts-space-2);

  /* 仅移动端显示 */
  @media (max-width: 767px) {
    display: flex;
  }
}

/* ---- 左侧按钮区 ---- */
.mobile-header__left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  flex-shrink: 0;
  min-width: 0;
}

/* 通用按钮样式 */
.mobile-header__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  color: var(--fts-text-primary);
  cursor: pointer;

  transition: background-color 0.15s ease;

  &:active {
    background: var(--fts-bg-hover);
  }
}

/* 汉堡菜单图标 */
.mobile-header__menu-btn {
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 6px;
}

.mobile-header__hamburger-line {
  display: block;
  width: 16px;
  height: 2px;
  background: var(--fts-text-primary);
  border-radius: 1px;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

/* ---- 标题区（居中，截断） ---- */
.mobile-header__title {
  font-size: var(--fts-font-size-base, 15px);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
  flex: 1;
  min-width: 0;
}

/* ---- 右侧占位（保持标题视觉居中） ---- */
.mobile-header__right {
  width: 32px;
  flex-shrink: 0;
}
</style>
