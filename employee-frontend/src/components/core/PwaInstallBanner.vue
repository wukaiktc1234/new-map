<script setup lang="ts">
/**
 * PwaInstallBanner — PWA 安装提示横幅
 *
 * 在页面底部显示安装引导，支持：
 * - Android: "安装应用" 按钮 → 触发原生安装弹窗
 * - iOS: "添加到桌面" 按钮 → 显示操作步骤指引
 * - 可关闭，关闭后 24 小时内不再提示
 */
import { usePwaInstall } from '@/composables/usePwaInstall'

const { canInstall, isIOS, showInstallPrompt, dismissInstall } = usePwaInstall()
</script>

<template>
  <Transition name="pwa-banner">
    <div v-if="canInstall" class="pwa-install-banner">
      <div class="pwa-install-banner__content">
        <!-- 图标 -->
        <div class="pwa-install-banner__icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M12 3v12m0 0l-4-4m4 4l4-4"/>
          </svg>
        </div>

        <!-- 文字 -->
        <div class="pwa-install-banner__text">
          <span class="pwa-install-banner__title">安装到桌面</span>
          <span class="pwa-install-banner__desc">{{ isIOS ? '点击查看安装步骤' : '获得更好的使用体验' }}</span>
        </div>

        <!-- 操作按钮 -->
        <button class="pwa-install-banner__action" @click="showInstallPrompt">
          {{ isIOS ? '如何安装' : '立即安装' }}
        </button>

        <!-- 关闭 -->
        <button class="pwa-install-banner__close" @click="dismissInstall" aria-label="关闭">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
/* ================================================================
   PWA Install Banner — 底部安装提示横幅（移动端优先）
   ================================================================ */

.pwa-install-banner {
  position: fixed;
  bottom: calc(var(--fts-tabbar-height, 56px) + var(--fts-space-3));
  left: var(--fts-space-3);
  right: var(--fts-space-3);
  z-index: 900;

  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #e5e7eb);
  border-radius: var(--fts-radius-lg, 14px);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(0, 0, 0, 0.02);

  overflow: hidden;

  /* 仅移动端显示 */
  @media (min-width: 768px) {
    display: none;
  }
}

.pwa-install-banner__content {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
}

/* ---- 图标 ---- */
.pwa-install-banner__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--fts-radius-md, 10px);
  background: var(--fts-primary-bg, rgba(59, 130, 246, 0.1));
  color: var(--fts-primary, #2563eb);
}

/* ---- 文字 ---- */
.pwa-install-banner__text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1;
  min-width: 0;
}

.pwa-install-banner__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--fts-text-primary, #111827);
  line-height: 1.3;
}

.pwa-install-banner__desc {
  font-size: 11px;
  color: var(--fts-text-secondary, #6b7280);
  line-height: 1.3;
  margin-top: 1px;
}

/* ---- 操作按钮 ---- */
.pwa-install-banner__action {
  padding: 6px 14px;
  border: none;
  border-radius: var(--fts-radius-sm, 8px);
  background: var(--fts-primary, #2563eb);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;

  transition: opacity 0.15s ease;

  &:active {
    opacity: 0.85;
  }
}

/* ---- 关闭按钮 ---- */
.pwa-install-banner__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--fts-radius-sm, 8px);
  background: transparent;
  color: var(--fts-text-tertiary, #9ca3af);
  cursor: pointer;
  flex-shrink: 0;

  transition: background-color 0.15s ease, color 0.15s ease;

  &:active {
    background: var(--fts-bg-hover, #f3f4f6);
    color: var(--fts-text-secondary, #6b7280);
  }
}

/* ---- 动画 ---- */
.pwa-banner-enter-active {
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
              opacity 0.25s ease;
}
.pwa-banner-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.pwa-banner-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.pwa-banner-leave-to {
  transform: translateY(50%);
  opacity: 0;
}
</style>
