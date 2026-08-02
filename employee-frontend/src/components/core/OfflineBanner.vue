<script setup lang="ts">
/**
 * OfflineBanner 网络离线状态优雅降级组件
 *
 * 当用户网络断开时，在页面顶部显示一个非侵入式的横幅提示。
 * 网络恢复后自动消失，带成功动画。
 *
 * 特性：
 * - 自动监听 online/offline 事件
 * - 非侵入式设计（不遮挡主要内容）
 * - 深色模式自动适配
 * - 平滑的显隐过渡动画
 * - 可配置的超时自动关闭
 */

import { ref, onMounted, onBeforeUnmount } from 'vue'

const isVisible = ref(false)
const isRecovering = ref(false)

let hideTimer: ReturnType<typeof setTimeout> | null = null

function handleOffline() {
  if (hideTimer) clearTimeout(hideTimer)
  isRecovering.value = false
  isVisible.value = true
}

function handleOnline() {
  isRecovering.value = true
  // 显示"网络已恢复"后自动隐藏
  hideTimer = setTimeout(() => {
    isVisible.value = false
    isRecovering.value = false
  }, 3000)
}

onMounted(() => {
  window.addEventListener('offline', handleOffline)
  window.addEventListener('online', handleOnline)
  
  // 初始检查网络状态
  if (!navigator.onLine) {
    isVisible.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('offline', handleOffline)
  window.removeEventListener('online', handleOnline)
  if (hideTimer) clearTimeout(hideTimer)
})
</script>

<template>
  <Transition name="offline-banner">
    <div v-if="isVisible" class="offline-banner" :class="{ 'offline-banner--recovering': isRecovering }">
      <!-- 离线图标 -->
      <span class="offline-icon">{{ isRecovering ? '&#x2713;' : '&#x26A0;' }}</span>
      
      <span class="offline-text">
        {{ isRecovering ? '网络已恢复连接' : '当前网络不可用，部分功能可能受限' }}
      </span>

      <button 
        v-if="!isRecovering" 
        class="offline-close" 
        @click="isVisible = false"
        aria-label="关闭提示"
      >
        &#x2715;
      </button>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.offline-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--fts-z-notification);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-5);
  background: rgba(var(--fts-warning-rgb), 0.92);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  color: #fff;
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  &--recovering {
    background: rgba(var(--fts-success-rgb), 0.92);
  }
}

.offline-icon {
  font-size: 16px;
  line-height: 1;
  animation: icon-pulse 1.5s ease-in-out infinite;

  .offline-banner--recovering & {
    animation: checkmark-pop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  }
}

@keyframes icon-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes checkmark-pop {
  0% { transform: scale(0); opacity: 0; }
  70% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}

.offline-text {
  letter-spacing: 0.01em;
}

.offline-close {
  position: absolute;
  right: var(--fts-space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: var(--fts-radius-full);
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    background: rgba(255, 255, 255, 0.35);
  }

  &:active {
    transform: scale(0.9);
  }
}

/* 过渡动画 */
.offline-banner-enter-active {
  transition: transform var(--fts-duration-normal) var(--fts-easing-spring),
              opacity var(--fts-duration-normal) ease;
}

.offline-banner-leave-active {
  transition: transform var(--fts-duration-fast) var(--fts-ease-in),
              opacity var(--fts-duration-fast) ease;
}

.offline-banner-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}

.offline-banner-leave-to {
  transform: translateY(-20%);
  opacity: 0;
}
</style>
