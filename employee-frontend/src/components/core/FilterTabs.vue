<script setup lang="ts">
/**
 * FilterTabs - 统一筛选/标签切换组件
 *
 * 替代项目中 5+ 种自定义 Tab 实现（TaskPage/TrainingPage/KnowledgePage/AppealListPage/ReportsPage），
 * 提供四种视觉变体：
 * - line:    线条式（飞书/钉钉风格，下划线指示器，适用于主导航）
 * - pill:    胶囊样式（最常用，替代各页面自定义筛选 Tab）
 * - outline: 描边样式（适用于 ReportsPage）
 * - segment: iOS 分段控制器（替代 ViewSwitcher）
 *
 * 支持特性：
 * - v-model 双向绑定
 * - 可选计数徽章
 * - 深色模式自动适配
 * - 平滑过渡动画
 * - 触控反馈（scale 0.96）
 * - 可选横向滚动
 */
import { ref, computed, onMounted, watch, nextTick } from 'vue'

/** 单个选项定义 */
export interface FilterTabOption {
  /** 选项值 */
  value: string | number
  /** 显示文字 */
  label: string
  /** 可选计数徽章 */
  count?: number
  /** 可选图标名称 */
  icon?: string
}

interface Props {
  /** 当前激活值 (v-model) */
  modelValue: string | number
  /** 选项列表 */
  options: FilterTabOption[]
  /** 视觉变体: line(线条式) / pill(胶囊) / outline(描边) / segment(分段) */
  variant?: 'line' | 'pill' | 'outline' | 'segment'
  /** 尺寸 */
  size?: 'small' | 'medium' | 'large'
  /** 是否支持横向滚动（选项多时启用） */
  scrollable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'line',
  size: 'medium',
  scrollable: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
  (e: 'change', value: string | number): void
}>()

/** segment 指示器位置样式 */
const indicatorStyle = computed(() => ({
  '--ft-indicator-left': `${indicatorLeft.value}px`,
  '--ft-indicator-width': `${indicatorWidth.value}px`,
}))

const containerRef = ref<HTMLElement | null>(null)
const indicatorLeft = ref(0)
const indicatorWidth = ref(0)

/** 计算滑动指示器位置 */
function updateIndicator(): void {
  if (props.variant !== 'segment' || !containerRef.value) return
  const activeIndex = props.options.findIndex(o => o.value === props.modelValue)
  if (activeIndex < 0) return

  const items = containerRef.value.querySelectorAll('.ft__item')
  const targetItem = items[activeIndex] as HTMLElement | undefined
  if (!targetItem) return

  indicatorLeft.value = targetItem.offsetLeft
  indicatorWidth.value = targetItem.offsetWidth
}

function handleSelect(value: string | number): void {
  if (value !== props.modelValue) {
    emit('update:modelValue', value)
    emit('change', value)
  }
}

onMounted(() => {
  updateIndicator()
})

watch(
  () => [props.modelValue, props.options, props.variant],
  () => {
    nextTick(() => updateIndicator())
  },
)
</script>

<template>
  <div
    ref="containerRef"
    class="ft"
    :class="[
      `ft--${variant}`,
      `ft--${size}`,
      { 'ft--scrollable': scrollable },
    ]"
    role="tablist"
  >
    <!-- segment 变体的滑动指示器 -->
    <div
      v-if="variant === 'segment'"
      class="ft__indicator"
      :style="indicatorStyle"
    />

    <button
      v-for="opt in options"
      :key="opt.value"
      :class="[
        'ft__item',
        { 'ft__item--active': modelValue === opt.value },
      ]"
      role="tab"
      :aria-selected="modelValue === opt.value"
      @click="handleSelect(opt.value)"
    >
      <span v-if="opt.icon" class="ft__icon">{{ opt.icon }}</span>
      <span class="ft__label">{{ opt.label }}</span>
      <span v-if="opt.count !== undefined && opt.count !== null" class="ft__count">
        {{ opt.count > 99 ? '99+' : opt.count }}
      </span>
    </button>
  </div>
</template>

<style scoped lang="scss">
// ============================================================
//  基础容器
// ============================================================
.ft {
  display: flex;
  align-items: center;
  position: relative;

  &--scrollable {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

// ============================================================
//  通用 Tab 项 — 仅设置跨变体共用的最小属性
//
//  设计原则：
//  - 基础样式只设 border/cursor/white-space 等不冲突属性
//  - 各变体自行定义 padding/font-size/background/color/line-height 等
//  - 避免在基础层设置需要被变体覆盖的属性，消除 !important 需求
// ============================================================
.ft__item {
  position: relative;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background-color var(--fts-duration-normal) var(--fts-easing-default),
    color var(--fts-duration-normal) var(--fts-easing-default),
    border-color var(--fts-duration-normal) var(--fts-easing-default),
    box-shadow var(--fts-duration-normal) var(--fts-easing-default),
    transform var(--fts-touch-duration-press) var(--fts-easing-spring);

  &:active:not(:disabled) {
    transform: scale(var(--fts-touch-scale-active));
  }

  &:focus-visible {
    outline: var(--fts-focus-ring-width) solid var(--fts-focus-ring-color);
    outline-offset: var(--fts-focus-ring-offset);
  }
}

.ft__icon {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}

.ft__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: var(--fts-badge-count-min-width);
  height: var(--fts-badge-count-height);
  padding: 0 4px;
  border-radius: var(--fts-radius-full);
  font-size: var(--fts-badge-font-size);
  font-weight: var(--fts-font-weight-semibold);
}

// ============================================================
//  line 变体 — 线条式（飞书/钉钉风格，适用于主导航）
//
//  设计参考：
//  - 飞书设计规范：一级标签页，选中态底部 3px 指示器线
//  - 钉钉/企业微信：同款线条式导航
//  - Linear/Notion：现代 SaaS 主导航标配
// ============================================================
.ft--line {
  gap: 0; // 紧凑排列
  border-bottom: 1px solid var(--fts-border-secondary);

  .ft__item {
    position: relative;
    flex: 1; // 钉钉风格：均分宽度，撑满容器
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 44px;
    color: var(--fts-text-secondary);
    background: transparent;
    border-radius: 0;
    transition: color var(--fts-duration-fast) ease;

    // 尺寸
    .ft--large & {
      height: 48px;
      font-size: var(--fts-font-size-md); // 16px
      font-weight: var(--fts-font-weight-medium);
      gap: var(--fts-space-2);
    }

    .ft--medium & {
      height: 40px;
      font-size: var(--fts-font-size-sm); // 14px
    }

    .ft--small & {
      height: 32px;
      font-size: var(--fts-font-size-xs); // 12px
    }
  }

  // 选中态：主色文字 + 底部指示器线（钉钉/飞书规范）
  .ft__item--active {
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-semibold);

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 16%;
      right: 16%; // 指示器比文字窄，更精致
      height: 3px;
      background: var(--fts-primary);
      border-radius: 2px 2px 0 0;
      transition: left var(--fts-duration-normal) var(--fts-easing-spring), opacity var(--fts-duration-normal) var(--fts-easing-spring);
    }
  }

  // Hover 态
  .ft__item:hover:not(.ft__item--active) {
    color: var(--fts-text-primary);
  }

  // count 徽章
  .ft__count {
    background: var(--fts-bg-tertiary);
    color: var(--fts-text-tertiary);
    margin-left: var(--fts-space-1);
  }

  .ft__item--active .ft__count {
    background: rgba(var(--fts-primary-rgb), 0.1);
    color: var(--fts-primary);
  }
}

// ============================================================
//  pill 变体 — 与 ViewSwitcher 完全一致的分段控制器
//
//  直接复用 ViewSwitcher（我的收入页切换器）的视觉规范：
//  - 外层圆角背景容器
//  - active 态：卡片色浮起 + 主色文字 + 微阴影
//  - inactive 态：透明融入容器
//  - 移动端溢出时自身作为滚动容器横向滑动
// ============================================================
.ft--pill {
  /* 外层容器 — 块级元素 + 自身滚动容器 */
  display: flex;
  width: 100%;
  min-width: 0;
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-lg);
  padding: 3px;
  gap: 2px;
  /* 自身作为横向滚动容器 */
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
  /* 不换行，保持单行横向排列 */
  flex-wrap: nowrap;

  /* 尺寸变体 — 与 ViewSwitcher --small/--medium 对应 */
  &--small {
    .ft__item {
      padding: var(--fts-space-1) var(--fts-space-2);
      font-size: var(--fts-font-size-xs);
    }
  }

  &--medium {
    .ft__item {
      padding: var(--fts-space-1) var(--fts-space-2);
      font-size: var(--fts-font-size-xs);
    }
  }

  &--large {
    .ft__item {
      padding: var(--fts-space-3) var(--fts-space-4);
      font-size: var(--fts-font-size-base);
    }
  }

  /* 切换按钮 — 与 .vs-sw-item 完全一致 */
  .ft__item {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: var(--fts-space-2) var(--fts-space-3);
    border: none;
    border-radius: var(--fts-radius-md);
    background: transparent;
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
    font-weight: 500;
    cursor: pointer;
    white-space: nowrap;
    /* 关键：禁止收缩，确保内容不被压缩，触发横向滚动 */
    flex-shrink: 0;
    /* 只过渡颜色属性，避免 box-shadow/font-weight 导致抖动 */
    transition:
      background-color 0.15s ease,
      color 0.15s ease;

    /* 激活状态 — 与 .vs-sw-item--active 一致 */
    &--active {
      background: var(--fts-bg-card);
      color: var(--fts-primary);
      font-weight: 600;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    }

    &:hover:not(&--active) {
      color: var(--fts-text-primary);
    }
  }

  /* 文字标签 — 与 .vs-sw-label 一致 */
  .ft__label {
    line-height: 1;
  }

  // count 徽章 — 加大尺寸
  .ft__count {
    margin-left: 2px;
    min-width: 20px;
    height: 20px;
    border-radius: 10px;
    background: var(--fts-error);
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    line-height: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 4px;
  }
}

// ============================================================
//  outline 变体 — 描边样式
// ============================================================
.ft--outline {
  flex-wrap: wrap;

  .ft__item {
    border: 1px solid var(--fts-border-secondary);
    border-radius: var(--fts-radius-lg);
    background: transparent;
    color: var(--fts-text-secondary);

    // 尺寸
    .ft--small & {
      padding: var(--fts-space-1) var(--fts-space-2);
      font-size: var(--fts-font-size-xs);
    }

    .ft--medium & {
      padding: var(--fts-space-2) var(--fts-space-3);
      font-size: var(--fts-font-size-sm);
    }

    .ft--large & {
      padding: var(--fts-space-2) var(--fts-space-4);
      font-size: var(--fts-font-size-base);
    }
  }

  .ft__item--active {
    border-color: var(--fts-primary);
    color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
  }

  .ft__item:hover:not(.ft__item--active) {
    border-color: var(--fts-border-hover);
    color: var(--fts-text-primary);
  }

  // outline 变体的 count 徽章
  .ft__count {
    background: rgba(var(--fts-primary-rgb), 0.08);
    color: var(--fts-primary);
  }

  .ft__item--active .ft__count {
    background: rgba(var(--fts-primary-rgb), 0.12);
  }
}

// ============================================================
//  segment 变体 — 与 pill 一致的 ViewSwitcher 分段控制器
//
//  统一为 ViewSwitcher 视觉规范（圆角背景 + active 卡片浮起）
//  用于页面级主导航（如 消息/通知公告 切换）
// ============================================================
.ft--segment {
  display: flex;
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-lg);
  padding: 3px;
  gap: 2px;

  .ft__item {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: var(--fts-space-3) var(--fts-space-4);
    border: none;
    border-radius: var(--fts-radius-md);
    background: transparent;
    color: var(--fts-text-secondary);
    font-weight: 500;
    cursor: pointer;
    white-space: nowrap;
    flex: 1; /* 均分宽度 */
    flex-shrink: 0;
    transition:
      background-color 0.15s ease,
      color 0.15s ease;

    /* 尺寸 */
    .ft--small & {
      padding: var(--fts-space-1) var(--fts-space-2);
      font-size: var(--fts-font-size-xs);
    }

    .ft--medium & {
      padding: var(--fts-space-2) var(--fts-space-3);
      font-size: var(--fts-font-size-sm);
    }

    .ft--large & {
      padding: var(--fts-space-3) var(--fts-space-5);
      font-size: var(--fts-font-size-base);
      font-weight: 500;
    }
  }

  /* active 态 — 卡片浮起 */
  .ft__item--active {
    background: var(--fts-bg-card);
    color: var(--fts-primary);
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }

  .ft__item:hover:not(.ft__item--active) {
    color: var(--fts-text-primary);
  }

  .ft__label {
    line-height: 1;
  }

  /* count 徽章 */
  .ft__count {
    margin-left: 2px;
    min-width: 20px;
    height: 20px;
    border-radius: 10px;
    background: var(--fts-error);
    color: #fff;
    font-size: 11px;
    font-weight: 600;
    line-height: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 4px;
  }

  .ft__item--active .ft__count {
    background: var(--fts-primary);
  }
}

// ============================================================
//  segment 滑动指示器（segment 变体已改用 border-bottom，此处保留兼容）
// ============================================================
.ft__indicator {
  // segment 变体下隐藏（使用 border-bottom 代替）
  .ft--segment & {
    display: none;
  }

  position: absolute;
  top: 3px;
  bottom: 3px;
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-card);
  box-shadow: var(--fts-shadow-sm);
  transition:
    left var(--fts-duration-normal) var(--fts-easing-spring),
    width var(--fts-duration-normal) var(--fts-easing-spring);
  z-index: 0;
  left: var(--ft-indicator-left, 0);
  width: var(--ft-indicator-width, 0);
}

// ============================================================
//  深色模式适配
// ============================================================
@media (prefers-color-scheme: dark) {
  // pill + segment 变体 — 与 ViewSwitcher 深色模式一致
  .ft--pill .ft__item--active,
  .ft--segment .ft__item--active {
    border: 1px solid rgba(var(--fts-primary-rgb), 0.15);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  }

  // outline 变体深色模式
  .ft--outline .ft__item--active {
    border-color: var(--fts-primary);
  }
}

// 手动切换深色模式
:root.dark .ft--pill .ft__item--active,
:root.dark .ft--segment .ft__item--active,
[data-theme='dark'] .ft--pill .ft__item--active,
[data-theme='dark'] .ft--segment .ft__item--active {
  border: 1px solid rgba(var(--fts-primary-rgb), 0.15);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}
</style>
