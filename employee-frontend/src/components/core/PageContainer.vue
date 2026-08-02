<script setup lang="ts">
/**
 * PageContainer - 统一页面容器组件（员工端UI架构地基）
 *
 * 设计理念：
 * - 参考飞书/MAC的页面结构规范
 * - 强制性三段式布局：Header + Body + Footer
 * - 自动处理移动端适配（安全区/返回按钮/标题栏）
 * - 完全使用CSS变量，零硬编码
 *
 * 使用示例：
 * <PageContainer title="审批中心" :loading="false">
 *   <template #headerActions>
 *     <el-button type="primary" @click="handleCreate">+ 发起申请</el-button>
 *   </template>
 *
 *   <!-- 页面具体内容 -->
 *   <div>...</div>
 *
 *   <template #footer>
 *     <el-button>取消</el-button>
 *     <el-button type="primary">提交</el-button>
 *   </template>
 * </PageContainer>
 */

import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

// ========== 滚动位置记忆 ==========
const SCROLL_KEY_PREFIX = 'fts_scroll_pos_'
const bodyRef = ref<HTMLElement | null>(null)

/** 获取滚动容器元素 */
function getScrollEl(): HTMLElement | null {
  return bodyRef.value || document.querySelector('.page-body')
}

/** 保存当前页面的滚动位置 */
function saveScrollPosition(path?: string) {
  const key = path || route.path
  const el = getScrollEl()
  if (el) {
    try {
      sessionStorage.setItem(SCROLL_KEY_PREFIX + key, JSON.stringify({
        x: el.scrollLeft,
        y: el.scrollTop,
        ts: Date.now(),
      }))
    } catch { /* ignore */ }
  }
}

/** 恢复指定路径的滚动位置（延迟到 DOM 完全渲染后） */
function restoreScrollPosition(path: string): boolean {
  try {
    const raw = sessionStorage.getItem(SCROLL_KEY_PREFIX + path)
    if (!raw) return false
    const data = JSON.parse(raw)
    // 超过5分钟的数据视为过期
    if (Date.now() - data.ts > 5 * 60 * 1000) {
      sessionStorage.removeItem(SCROLL_KEY_PREFIX + path)
      return false
    }
    // nextTick + 双 rAF 确保内容完全渲染后再恢复位置
    nextTick(() => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          const el = getScrollEl()
          if (el && (data.y > 0 || data.x > 0)) {
            el.scrollTo({ top: data.y, left: data.x })
          }
        })
      })
    })
    return true
  } catch {
    return false
  }
}

// ========== Props 定义 ==========
interface Props {
  /** 页面标题（优先级最高） */
  title?: string
  /** 副标题（显示在标题下方的小字） */
  subtitle?: string
  /** 是否显示返回按钮（移动端默认显示） */
  showBack?: boolean
  /** 是否显示加载状态 */
  loading?: boolean
  /** 内容区内边距 */
  padding?: string | number
  /** 是否约束最大宽度（桌面端限制阅读宽度） */
  constrained?: boolean
  /** 自定义最大宽度（仅constrained=true时生效） */
  maxWidth?: string
  /** Footer是否固定在底部（sticky定位） */
  stickyFooter?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  subtitle: '',
  showBack: false,
  loading: false,
  padding: undefined,
  constrained: true,
  maxWidth: '960px',
  stickyFooter: false,
})

// ========== 路由信息 ==========
const route = useRoute()
const router = useRouter()

/** 最终显示的标题（props > route.meta.title > ''） */
const displayTitle = computed(() => {
  return props.title || (route.meta?.title as string) || ''
})

/** 是否应该显示返回按钮（Tab首页不显示，子页面自动显示） */
const shouldShowBack = computed(() => {
  if (props.showBack) return true
  const isTabRoot = route.meta?.tab as string | undefined
  if (isTabRoot) return false
  return true
})

// ========== 方法 ==========
/**
 * 返回上一页（应用内导航，非浏览器后退）
 *
 * 策略优先级：
 * 1. 有 from 查询参数 → 跳转到指定页
 * 2. 学习模式 → 回到来源页（/training）
 * 3. 路由栈中有同域历史 → router.back()（Vue Router scrollBehavior自动恢复滚动位置）
 * 4. 兜底：根据当前路由推断最佳返回目标
 */
function handleBack() {
  // 保存当前页面的滚动位置，以便将来返回时恢复
  saveScrollPosition()

  // 学习模式：返回来源页面
  if (route.query.mode === 'learning') {
    const from = route.query.from as string | undefined
    router.push(from || '/training')
    return
  }

  // 有明确的来源页参数
  if (route.query.from) {
    router.push(route.query.from as string)
    return
  }

  // 尝试浏览器历史回退（scrollBehavior会自动恢复滚动位置）
  if (window.history.length > 2 && window.history.state) {
    router.back()
    return
  }

  // 兜底：根据当前路由路径推断最佳返回目标
  const fallbackMap: Record<string, string> = {
    '/settings/security/password': '/settings/security',
    '/settings/security/recovery': '/settings/security',
    '/settings/security/devices': '/settings/security',
    '/settings/security/lock': '/settings/security',
    '/settings/security/logs': '/settings/security',
    '/settings/security/email-bind': '/settings/security',
    '/settings/account': '/profile',
    '/settings/notifications': '/profile',
    '/help': '/profile',
    '/appeals/create': '/appeals',
    '/appeals/': '/appeals',
    '/approval/create': '/approval',
    '/approval/detail': '/approval',
    '/review/detail': '/review',
    '/tasks/publish': '/tasks',     // 发布任务返回列表
    '/tasks': '/office',           // 工作任务从办公中心进入
  }

  // 精确匹配或前缀匹配
  const path = route.path
  let fallback = '/home'
  for (const [prefix, target] of Object.entries(fallbackMap)) {
    if (path === prefix || path.startsWith(prefix + '/')) {
      fallback = target
      break
    }
  }

  router.push(fallback)
}

// ========== 生命周期：滚动位置恢复 ==========
onMounted(() => {
  // 尝试恢复之前保存的滚动位置
  restoreScrollPosition(route.path)
})

onBeforeUnmount(() => {
  // 离开页面时保存当前滚动位置
  saveScrollPosition()
})
</script>

<template>
  <div class="page-container" :class="{ 'page-container--loading': loading }">
    <!-- ====== Header：标题栏（固定或流式） ====== -->
    <header v-if="displayTitle || $slots.headerActions || $slots.headerPrefix" class="page-header">
      <div class="page-header__inner" :class="{ 'page-header__inner--constrained': constrained }">
        <!-- 左侧：返回按钮 + 标题 -->
        <div class="page-header__left">
          <!-- 前置插槽 -->
          <slot name="headerPrefix" />

          <!-- 返回按钮（移动端自动显示） -->
          <button
            v-if="shouldShowBack"
            class="page-header__back"
            @click="handleBack"
            title="返回"
          >
            <el-icon :size="18"><ArrowLeft /></el-icon>
          </button>

          <!-- 标题区域 -->
          <div class="page-header__title-group">
            <h1 class="page-header__title">{{ displayTitle }}</h1>
            <p v-if="subtitle || $slots.subtitle" class="page-header__subtitle">
              <slot name="subtitle">{{ subtitle }}</slot>
            </p>
          </div>
        </div>

        <!-- 右侧：操作按钮区域 -->
        <div v-if="$slots.headerActions" class="page-header__actions">
          <slot name="headerActions" />
        </div>
      </div>
    </header>

    <!-- ====== Body：内容区（flex:1，可滚动） ====== -->
    <main
      ref="bodyRef"
      class="page-body"
      :class="{
        'page-body--constrained': constrained,
        'page-body--loading': loading,
        'page-body--sticky-footer': stickyFooter
      }"
      :style="padding ? { padding: typeof padding === 'number' ? `${padding}px` : padding } : {}"
    >
      <!-- 加载状态 -->
      <div v-if="loading" class="page-body__skeleton">
        <slot name="loading">
          <el-skeleton :rows="5" animated />
        </slot>
      </div>

      <!-- 正常内容 + Footer（stickyFooter 模式下 footer 在滚动区域内） -->
      <div v-else class="page-body__content">
        <slot />
        <!-- [footer-fix] stickyFooter 时 footer 放入滚动区域内部，
             位于表单内容末尾，用户滚到底部才看到 -->
        <footer
          v-if="$slots.footer && stickyFooter"
          class="page-footer page-footer--inline"
        >
          <div class="page-footer__inner">
            <slot name="footer" />
          </div>
        </footer>
      </div>

      <!-- 非 stickyFooter 模式：footer 在 body 之外（容器级） -->
      <footer
        v-if="$slots.footer && !stickyFooter"
        class="page-footer"
        :class="{ 'page-footer--constrained': constrained }"
      >
        <div class="page-footer__inner">
          <slot name="footer" />
        </div>
      </footer>
    </main>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   PageContainer - 统一页面容器（macOS视觉风格）
   所有颜色/圆角/间距均通过 var(--fts-xxx) CSS变量引用
   ================================================================ */

.page-container {
  display: flex;
  flex-direction: column;
  min-width: 0;
  width: 100%;
  max-width: 100vw;
  background: var(--fts-bg-page);
  position: relative;
  /* [footer-fix] 移除 overflow-y:auto — 父容器不应成为滚动容器
   * 滚动责任完全交给 .page-body (overflow-y:auto)
   * 否则 flex 子元素（footer）无法正确沉底 */
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  box-sizing: border-box;

  padding: 0 !important;
  margin: 0 !important;

  @media (min-width: 768px) {
    min-height: 100vh;
    min-height: 100dvh;
  }

  @media (max-width: 767px) {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100% !important;
    max-width: none !important;
  }

  &--loading {
    pointer-events: none;
    user-select: none;
  }
}

// ================================================================
// Header：标题栏
// ================================================================

.page-header {
  position: sticky;
  top: 0;
  z-index: var(--fts-z-sticky);
  background: var(--fts-bg-glass);
  flex-shrink: 0;

  border-bottom: 1px solid var(--fts-border-glass);

  /* MAC毛玻璃效果 */
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);

  &__inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-4);
    height: 56px;
    padding: 0 var(--fts-content-padding);
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;

    &--constrained {
      max-width: var(--fts-max-content-width, 960px);
      margin: 0 auto;
      padding: 0 var(--fts-space-6);

      /* 移动端：统一padding，不用margin制造偏移 */
      @media (max-width: 767px) {
        max-width: none;
        margin: 0 !important;
        padding: 0 var(--fts-space-3) !important;
      }
    }

    /* 移动端优化：商用级标题栏高度44-48px */
    @media (max-width: 767px) {
      height: 48px;
    }
  }

  /* 左侧区域 */
  &__left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    min-width: 0; /* 允许文字截断 */
    flex-shrink: 0;
  }

  /* 返回按钮 */
  &__back {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: var(--fts-radius-sm);
    background: transparent;
    color: var(--fts-text-secondary);
    cursor: pointer;
    transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default);
    flex-shrink: 0;

    &:hover {
      background: var(--fts-bg-hover);
      color: var(--fts-text-primary);
    }

    &:active {
      transform: scale(0.92);
    }
  }

  /* 标题组 */
  &__title-group {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    line-height: var(--fts-line-height-tight);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    @media (max-width: 767px) {
      font-size: var(--fts-font-size-md);
    }
  }

  &__subtitle {
    margin: 0;
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    line-height: var(--fts-line-height-normal);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  /* 右侧操作区 */
  &__actions {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-shrink: 0;
  }
}

// ================================================================
// Body：内容区
// ================================================================

.page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--fts-space-3) var(--fts-content-padding);
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  /* [footer-fix] 关键：overflow-y:auto 让超长内容在 body 内部滚动
   * 而不是溢出到 footer 区域造成视觉遮挡 */
  overflow-y: auto;

  /* 移动端收紧内边距 + 底部额外呼吸空间（为TabBar留出安全距离） */
  @media (max-width: 767px) {
    padding: var(--fts-space-3);
    padding-bottom: calc(var(--fts-tabbar-height) + var(--fts-space-4));
  }

  &--constrained {
    max-width: var(--fts-max-content-width, 960px);
    margin: 0 auto;
    width: 100%;
    padding-left: var(--fts-space-6);
    padding-right: var(--fts-space-6);

    /* 移动端：只用padding，不用margin */
    @media (max-width: 767px) {
      max-width: none;
      margin: 0 !important;
      padding-left: var(--fts-space-3) !important;
      padding-right: var(--fts-space-3) !important;
    }
  }

  &--loading {
    opacity: 0.6;
  }

  /* 内容容器 */
  &__content {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-5);
    min-width: 0;
    max-width: 100%;
    box-sizing: border-box;
  }

  /* 骨架屏加载态 */
  &__skeleton {
    padding: var(--fts-space-6) 0;
  }
}

// ================================================================
// Footer：操作区
// ================================================================

.page-footer {
  flex-shrink: 0;
  border-top: 1px solid var(--fts-border-glass);
  background: var(--fts-bg-glass);
  padding: var(--fts-space-4) var(--fts-content-padding);
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;

  /* 移动端安全区适配（仅容器级 footer 需要） */
  padding-bottom: calc(var(--fts-space-4) + env(safe-area-inset-bottom, 0px));

  /* [footer-fix] inline 模式：footer 在滚动区域内部（.page-body__content 的子元素）
   * 跟在表单内容末尾，用户滚到底部才看到。无需任何定位属性 */
  &--inline {
    position: static;
    margin-top: var(--fts-space-4);
    padding-bottom: var(--fts-space-4);

    @media (max-width: 767px) {
      padding-left: var(--fts-space-3);
      padding-right: var(--fts-space-3);
    }
  }

  &--constrained {
    max-width: var(--fts-max-content-width, 960px);
    margin: 0 auto;
    padding-left: var(--fts-space-6);
    padding-right: var(--fts-space-6);

    /* 移动端：统一取消宽度约束，padding与header/body一致 */
    @media (max-width: 767px) {
      max-width: none;
      padding-left: var(--fts-space-3);
      padding-right: var(--fts-space-3);
    }
  }

  &__inner {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: var(--fts-space-3);
    max-width: 100%;

    @media (max-width: 767px) {
      :deep(.el-button) {
        flex: 1;
        min-width: 0;
      }
    }
  }
}

// ================================================================
// 暗色模式适配（已移至非scoped样式块）
// ================================================================
</style>

<!-- 非scoped样式块：处理深色模式（需匹配html根元素）和全局按钮统一（需穿透el-button） -->
<style lang="scss">
/* 深色模式Header/Footer — 匹配 html.dark 下的 .page-header */
html.dark .page-container > .page-header {
  background: var(--fts-bg-glass) !important;
  border-bottom-color: var(--fts-border-glass) !important;
}

html.dark .page-container > .page-footer {
  background: var(--fts-bg-glass) !important;
  border-top-color: var(--fts-border-glass) !important;
}

/* 头部操作按钮统一样式 — 所有页面的 headerActions 按钮自动生效 */
.page-header__inner .el-button--primary {
  --el-button-bg-color: var(--fts-primary) !important;
  --el-button-border-color: var(--fts-primary) !important;
  --el-button-hover-bg-color: var(--fts-primary-hover, var(--fts-primary-dark)) !important;
  --el-button-hover-border-color: var(--fts-primary-hover, var(--fts-primary-dark)) !important;
  --el-button-text-color: var(--fts-text-on-primary) !important;
  --el-button-hover-text-color: var(--fts-text-on-primary) !important;
  --el-button-active-bg-color: var(--fts-primary-active, var(--fts-primary-dark)) !important;
  --el-button-active-border-color: var(--fts-primary-active, var(--fts-primary-dark)) !important;
  --el-button-active-text-color: var(--fts-text-on-primary) !important;
  border-radius: var(--fts-radius-md);
  font-weight: 600;
  font-size: var(--fts-font-size-sm);
  height: 32px;
  padding: 0 var(--fts-space-3);
  display: inline-flex;
  align-items: center;
  gap: 4px;

  @media (max-width: 767px) {
    height: 30px;
    padding: 0 var(--fts-space-2);
    font-size: var(--fts-font-size-xs);
  }
}

/* 深色模式TabBar */
html.dark .mobile-tab-bar {
  box-shadow: var(--fts-shadow-tabbar-dark) !important;
}
</style>
