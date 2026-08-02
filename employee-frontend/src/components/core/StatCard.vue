<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  value: string | number
  label: string
  subText?: string
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info' | 'hero' | 'grid' | 'icon-left'
  icon?: string
  /** 紧凑模式（用于网格排列时减小内边距） */
  compact?: boolean
  /** 趋势文字（如 '+2天'） */
  trend?: string
  /** 趋势方向：up/down */
  trendType?: 'up' | 'down'
  /** 自定义渐变背景（仅 hero 变体生效） */
  gradient?: string
  /** 是否显示图标背景圆圈（仅 icon-left 变体，默认 true） */
  iconBg?: boolean
}>(), {
  variant: 'default',
  compact: false,
  iconBg: true,
})

const variantClass = computed(() => `stat-card--${props.variant}`)

/** hero 变体的渐变背景样式 */
const heroGradientStyle = computed(() => {
  if (props.variant !== 'hero') return undefined
  if (props.gradient) return { background: props.gradient }
  // 根据颜色变体选择对应渐变
  const gradientMap: Record<string, string> = {
    primary: 'var(--fts-gradient-primary)',
    success: 'var(--fts-gradient-success)',
    warning: 'var(--fts-gradient-warning)',
    error: 'var(--fts-gradient-error)',
    default: 'var(--fts-gradient-primary)',
  }
  return { background: gradientMap[props.variant] || gradientMap.default }
})
</script>

<template>
  <div
    class="stat-card"
    :class="[variantClass, { 'stat-card--compact': compact }]"
    :style="heroGradientStyle"
  >
    <!-- 图标 + 数值行 -->
    <div class="stat-top">
      <span v-if="icon" class="stat-icon" :class="[`stat-icon--${variant}`, { 'stat-icon--bg': iconBg && variant === 'icon-left' }]">{{ icon }}</span>
      <span class="stat-value">{{ value }}</span>
      <!-- 趋势标签 -->
      <span v-if="trend" class="stat-trend" :class="`stat-trend--${trendType || 'neutral'}`">
        {{ trend }}
      </span>
    </div>

    <!-- 标签 -->
    <span class="stat-label">{{ label }}</span>

    <!-- 副文本 -->
    <span v-if="subText" class="stat-sub">{{ subText }}</span>
  </div>
</template>

<style scoped lang="scss">
.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-4) var(--fts-space-3);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  transition: var(--fts-card-transition);
  position: relative;
  overflow: hidden;

  /* hover 增强：上浮 + 光晕边框 */
  &:hover {
    transform: translateY(var(--fts-card-hover-translate-y, -2px));
    box-shadow: var(--fts-card-hover-shadow, 0 8px 24px rgba(0, 0, 0, 0.08));
    border-color: var(--fts-card-hover-border-glow, rgba(var(--fts-primary-rgb), 0.2));
  }

  &:active {
    transform: scale(0.98);
  }

  /* 变体：左侧色带标识 */
  &--primary {
    border-left: 3px solid var(--fts-primary);
    .stat-value { color: var(--fts-primary); }
  }

  &--success {
    border-left: 3px solid var(--fts-success);
    .stat-value { color: var(--fts-success); }
  }

  &--warning {
    border-left: 3px solid var(--fts-warning);
    .stat-value { color: var(--fts-warning); }
  }

  &--error {
    border-left: 3px solid var(--fts-error);
    .stat-value { color: var(--fts-error); }
  }

  &--info {
    border-left: 3px solid var(--fts-info);
    .stat-value { color: var(--fts-info); }
  }

  /* 紧凑模式：减少内边距 */
  &--compact {
    padding: var(--fts-space-3) var(--fts-space-2);
    gap: 0;
  }

  /* ===== hero 变体：渐变背景大卡片 ===== */
  &--hero {
    padding: var(--fts-space-5) var(--fts-space-4);
    border: none;
    color: var(--fts-text-on-primary);
    align-items: center;
    text-align: center;

    .stat-top {
      flex-direction: column;
      align-items: center;
      gap: var(--fts-space-2);
      width: 100%;
    }

    .stat-value {
      font-size: var(--fts-font-size-2xl);
      color: var(--fts-text-on-primary);
      letter-spacing: -0.02em;
    }

    .stat-label {
      color: var(--fts-text-on-primary);
      opacity: 0.85;
      text-align: center;
    }

    .stat-sub {
      color: var(--fts-text-on-primary);
      opacity: 0.7;
    }

    .stat-icon {
      color: var(--fts-text-on-primary);
      opacity: 0.9;
    }

    .stat-trend {
      margin-top: var(--fts-space-1);

      &--up {
        background: rgba(255, 255, 255, 0.2);
        color: var(--fts-text-on-primary);
      }

      &--down {
        background: rgba(255, 255, 255, 0.2);
        color: var(--fts-text-on-primary);
      }

      &--neutral {
        background: rgba(255, 255, 255, 0.15);
        color: var(--fts-text-on-primary);
      }
    }

    &:hover {
      transform: none;
      box-shadow: none;
      border-color: transparent;
    }

    &:active {
      transform: none;
    }
  }

  /* ===== grid 变体：紧凑网格卡片 ===== */
  &--grid {
    border-left: none;
    padding: var(--fts-space-3);
    gap: var(--fts-space-1);
    align-items: center;
    text-align: center;
    border-color: var(--fts-border-secondary);

    .stat-value {
      font-size: var(--fts-font-size-lg);
      color: var(--fts-text-primary);
      font-weight: 700;
    }

    .stat-label {
      font-size: var(--fts-font-size-2xs);
      color: var(--fts-text-secondary);
    }

    .stat-sub {
      color: var(--fts-text-tertiary);
    }

    .stat-top {
      flex-direction: column;
      align-items: center;
      gap: var(--fts-space-1);
      width: 100%;
    }

    &:hover {
      transform: none;
      box-shadow: none;
      border-color: var(--fts-border-primary);
    }

    &:active {
      transform: none;
    }
  }

  /* ===== icon-left 变体：图标在左的横向布局 ===== */
  &--icon-left {
    border-left: none;
    flex-direction: row;
    align-items: center;
    gap: var(--fts-space-3);
    padding: var(--fts-space-3) var(--fts-space-4);

    .stat-top {
      flex-direction: column;
      align-items: flex-start;
      gap: var(--fts-space-1);
    }

    .stat-label {
      font-size: var(--fts-font-size-2xs);
    }

    .stat-value {
      font-size: var(--fts-font-size-lg);
    }

    .stat-icon {
      font-size: 18px;
      opacity: 1;
    }

    .stat-sub {
      font-size: var(--fts-font-size-2xs);
    }
  }
}

/* 深色模式下 hero 变体使用更深的渐变 */
@media (prefers-color-scheme: dark) {
  .stat-card--hero:not([style*="background"]) {
    background: linear-gradient(
      135deg,
      var(--fts-gradient-primary-deep, #0040A0) 0%,
      var(--fts-primary) 100%
    );
  }
}

/* html.dark 类名方式的深色模式兼容 */
:global(html.dark) .stat-card--hero:not([style*="background"]) {
  background: linear-gradient(
    135deg,
    var(--fts-gradient-primary-deep, #0040A0) 0%,
    var(--fts-primary) 100%
  );
}

.stat-top {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-2);
}

.stat-icon {
  font-size: 14px;
  opacity: 0.7;
  transition: opacity var(--fts-duration-fast) ease;

  .stat-card:hover & {
    opacity: 1;
  }

  /* 各变体图标背景色 */
  &--primary { color: var(--fts-primary); }
  &--success { color: var(--fts-success); }
  &--warning { color: var(--fts-warning); }
  &--error { color: var(--fts-error); }
  &--info { color: var(--fts-info); }

  /* hero 变体图标 */
  &--hero {
    color: var(--fts-text-on-primary);
    opacity: 0.9;
  }

  /* grid 变体图标 */
  &--grid {
    color: var(--fts-text-tertiary);
  }

  /* icon-left 变体图标背景圆圈 */
  &--bg {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--fts-radius-full);
    font-size: 18px;
    flex-shrink: 0;
  }

  &--icon-left {
    opacity: 1;

    &.stat-icon--bg {
      background: rgba(var(--fts-primary-rgb), 0.1);
      color: var(--fts-primary);
    }

    &.stat-icon--primary.stat-icon--bg { background: rgba(var(--fts-primary-rgb), 0.1); color: var(--fts-primary); }
    &.stat-icon--success.stat-icon--bg { background: rgba(var(--fts-success-rgb), 0.1); color: var(--fts-success); }
    &.stat-icon--warning.stat-icon--bg { background: rgba(var(--fts-warning-rgb), 0.1); color: var(--fts-warning); }
    &.stat-icon--error.stat-icon--bg { background: rgba(var(--fts-error-rgb), 0.1); color: var(--fts-error); }
    &.stat-icon--info.stat-icon--bg { background: rgba(var(--fts-info-rgb), 0.1); color: var(--fts-info); }
  }
}

.stat-value {
  font-size: var(--fts-font-size-xl);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  white-space: nowrap;
  font-weight: 500;
}

.stat-sub {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  opacity: 0.7;
}

/* 趋势标签 */
.stat-trend {
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  padding: 1px 6px;
  border-radius: var(--fts-radius-full);

  &--up {
    background: rgba(var(--fts-success-rgb), 0.1);
    color: var(--fts-success);
  }

  &--down {
    background: rgba(var(--fts-error-rgb), 0.1);
    color: var(--fts-error);
  }

  &--neutral {
    background: var(--fts-bg-tertiary);
    color: var(--fts-text-quaternary);
  }
}
</style>
