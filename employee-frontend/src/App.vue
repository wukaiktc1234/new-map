<script setup lang="ts">
import { ref, onErrorCaptured, computed, onMounted } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import EmployeeLayout from './layout/EmployeeLayout.vue'
import { OfflineBanner } from './components/core'

const router = useRouter()
const route = useRoute()

const isPublicPage = computed(() => route.meta.public === true)

/**
 * 首次渲染标记：跳过首次渲染的页面过渡动画
 *
 * 原因：transition mode="out-in" 在首次渲染时，如果路由在挂载后
 * 发生重定向（如 / → /login），会先执行"离开"动画（从空白离开），
 * 再执行"进入"动画，导致首屏出现 200-250ms 的空白闪烁。
 * 首次渲染时无需过渡动画，直接显示即可。
 */
const isFirstRender = ref(true)

onMounted(() => {
  // 首次挂载完成后，后续路由切换才启用过渡动画
  // 使用 requestAnimationFrame 确保首帧渲染完成后再标记
  requestAnimationFrame(() => {
    isFirstRender.value = false
  })
})

const hasError = ref(false)
const errorMessage = ref('')
const errorDetail = ref('')
const errorType = ref<'network' | 'business' | 'system' | 'unknown'>('unknown')
const isDev = import.meta.env.DEV

onErrorCaptured((err: unknown, _instance, _info) => {
  hasError.value = true

  if (err instanceof Error) {
    errorMessage.value = err.message || '发生了未知错误'
    errorDetail.value = err.stack || ''

    if (err.message.includes('Network') || err.message.includes('fetch') || err.message.includes('timeout')) {
      errorType.value = 'network'
    } else if (err.message.includes('401') || err.message.includes('403') || err.message.includes('权限')) {
      errorType.value = 'business'
    } else {
      errorType.value = 'system'
    }
  } else {
    errorMessage.value = '发生了未知错误'
    errorDetail.value = String(err)
    errorType.value = 'unknown'
  }

  if (isDev) {
    console.error('[ErrorBoundary] 捕获到组件错误:', err, _info)
  }

  return false
})

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''
  errorDetail.value = ''
  errorType.value = 'unknown'
}

function goHome() {
  handleRetry()
  router.push('/')
}

// APP 打包模式：标记渲染阶段（仅开发模式）
onMounted(() => {
  if (import.meta.env.DEV) {
    // 延迟检查 #app 是否有实际内容
    setTimeout(() => {
      const appEl = document.getElementById('app')
      if (appEl) {
        const html = appEl.innerHTML
        const hasContent = html.length > 200
        if (!hasContent) {
          console.warn('[Emp] #app 内容仅', html.length, '字符，可能渲染异常')
        }
      }
    }, 500)
  }
})

function getErrorTitle(): string {
  switch (errorType.value) {
    case 'network': return '网络连接异常'
    case 'business': return '操作未授权'
    case 'system': return '系统繁忙'
    default: return '页面出了点问题'
  }
}

function getErrorDesc(): string {
  switch (errorType.value) {
    case 'network': return '请检查网络连接后重试'
    case 'business': return '您没有权限执行此操作'
    case 'system': return '请稍后再试，如持续出现请联系管理员'
    default: return '请返回首页或刷新重试'
  }
}
</script>

<template>
  <!-- 网络离线状态横幅 -->
  <OfflineBanner />

  <div v-if="hasError" class="error-boundary">
    <div class="error-boundary__container">
      <div class="error-boundary__icon-wrapper">
        <svg class="error-boundary__icon" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="32" cy="32" r="30" fill="var(--fts-error-light, #ffece8)" stroke="var(--fts-error, #f53f3f)" stroke-width="2"/>
          <path d="M32 18v20M32 46v2" stroke="var(--fts-error, #f53f3f)" stroke-width="4" stroke-linecap="round"/>
        </svg>
      </div>
      <h2 class="error-boundary__title">{{ getErrorTitle() }}</h2>
      <p class="error-boundary__desc">{{ getErrorDesc() }}</p>
      <div v-if="isDev && errorDetail" class="error-boundary__detail">
        <div class="error-boundary__detail-header">
          <span>开发模式 - 错误详情</span>
        </div>
        <pre class="error-boundary__detail-content">{{ errorDetail }}</pre>
      </div>
      <div class="error-boundary__actions">
        <button class="error-boundary__btn error-boundary__btn--primary" @click="goHome">
          返回首页
        </button>
        <button class="error-boundary__btn error-boundary__btn--secondary" @click="handleRetry">
          重试
        </button>
      </div>
    </div>
  </div>
  <template v-else>
    <EmployeeLayout v-if="!isPublicPage">
      <RouterView v-slot="{ Component }">
        <transition v-if="!isFirstRender" name="page-slide" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
        <component v-else :is="Component" :key="route.path" />
      </RouterView>
    </EmployeeLayout>
    <RouterView v-else v-slot="{ Component }">
      <transition v-if="!isFirstRender" name="page-slide" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
      <component v-else :is="Component" :key="route.path" />
    </RouterView>
  </template>
</template>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: var(--fts-bg-page);
  padding: var(--fts-space-6);
}

.error-boundary__container {
  max-width: 480px;
  width: 100%;
  text-align: center;
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-10) var(--fts-space-8);
  box-shadow: var(--fts-shadow-lg);
}

.error-boundary__icon-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: var(--fts-space-4);
}

.error-boundary__icon {
  width: 80px;
  height: 80px;
}

.error-boundary__title {
  font-size: var(--fts-font-size-xl);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2);
  line-height: 1.4;
}

.error-boundary__desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin: 0 0 var(--fts-space-6);
  line-height: 1.6;
}

.error-boundary__detail {
  text-align: left;
  background-color: var(--fts-bg-fill);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
  margin-bottom: var(--fts-space-6);
  overflow: auto;
  max-height: 200px;
}

.error-boundary__detail-header {
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  color: var(--fts-text-tertiary);
  padding: var(--fts-space-2) var(--fts-space-3);
  background-color: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  margin-bottom: var(--fts-space-2);
}

.error-boundary__detail-content {
  margin: 0;
  font-size: 11px;
  color: var(--fts-text-tertiary);
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  line-height: 1.5;
}

.error-boundary__actions {
  display: flex;
  gap: var(--fts-space-4);
  justify-content: center;
}

.error-boundary__btn {
  padding: var(--fts-space-2) var(--fts-space-6);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease,
    opacity 0.2s ease,
    transform 0.2s ease;
  border: 1px solid transparent;
  line-height: 1.5;
}

.error-boundary__btn--primary {
  background-color: var(--fts-primary);
  color: var(--fts-text-on-primary);
  border-color: var(--fts-primary);
}

.error-boundary__btn--primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.error-boundary__btn--primary:active {
  transform: translateY(0);
}

.error-boundary__btn--secondary {
  background-color: transparent;
  color: var(--fts-primary);
  border-color: var(--fts-primary);
}

.error-boundary__btn--secondary:hover {
  background-color: var(--fts-primary-light-1, #e8f3ff);
}

.error-boundary__btn--secondary:active {
  background-color: var(--fts-primary-light-2, #d0e8ff);
}
</style>

<style lang="scss">
/* ================================================================
   商用级页面过渡 — 飞书/钉钉丝滑切换（增强版）
   
   设计理念：
   - 进入：从下方淡入 + 轻微缩放回弹（spring物理感）
   - 离开：向上淡出 + 轻微缩小（让位感）
   - 所有参数通过 --fts-page-* CSS变量可配置
   - 支持深色模式自动适配
   ================================================================ */

.page-slide-enter-active {
  transition:
    opacity var(--fts-page-enter-duration, 250ms) var(--fts-page-easing-enter, cubic-bezier(0.16, 1, 0.3, 1)),
    transform var(--fts-page-enter-duration, 250ms) var(--fts-page-easing-enter, cubic-bezier(0.16, 1, 0.3, 1));
}

.page-slide-leave-active {
  transition:
    opacity var(--fts-page-leave-duration, 200ms) var(--fts-page-easing-leave, ease-in),
    transform var(--fts-page-leave-duration, 200ms) var(--fts-page-easing-leave, ease-in);
}

.page-slide-enter-from {
  opacity: var(--fts-page-enter-opacity-start, 0);
  transform: translateY(var(--fts-page-enter-translate-y, 12px)) scale(var(--fts-page-enter-scale, 1));
}

.page-slide-leave-to {
  opacity: var(--fts-page-leave-opacity-end, 0);
  transform: translateY(var(--fts-page-leave-translate-y, -6px)) scale(var(--fts-page-leave-scale, 0.98));
}
</style>
