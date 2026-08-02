<script setup lang="ts">
/**
 * AppBadge 徽标组件（增强版）
 *
 * 支持未读数量角标，带脉冲动画提醒。
 * 所有颜色通过 --fts-* CSS变量控制。
 */

withDefaults(defineProps<{
  value?: number | string
  max?: number
  /** 是否显示脉冲动画（默认有数字时自动启用） */
  dot?: boolean
  /** 是否隐藏（值为0时） */
  hidden?: boolean
  showPulse?: boolean
}>(), {
  max: 99,
  dot: false,
  hidden: true,
  showPulse: undefined, // undefined = 自动根据value判断
})

/** 格式化显示值 */
function formatValue(val: number | string, maxNum: number): string {
  const num = typeof val === 'string' ? parseInt(val) || 0 : val
  return num > maxNum ? `${maxNum}+` : String(num)
}

/** 是否应该显示脉冲 */
function shouldPulse(value: number | string | undefined, explicit: boolean | undefined): boolean {
  if (explicit !== undefined) return explicit
  if (value === undefined || value === null) return false
  const num = typeof value === 'string' ? parseInt(value) || 0 : value
  return num > 0
}
</script>

<template>
  <span
    v-if="!(hidden && !value)"
    class="app-badge"
    :class="{
      'app-badge--dot': dot,
      'app-badge--pulse': shouldPulse(value, showPulse),
      'app-badge--hidden': hidden && (!value || value === 0)
    }"
  >
    <slot>{{ dot ? '' : formatValue(value ?? 0, max) }}</slot>
  </span>
</template>

<style scoped lang="scss">
.app-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--fts-radius-full);
  background: var(--fts-error);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: -0.02em;
  white-space: nowrap;
  transform-origin: center;
  
  /* 基础过渡 */
  transition: transform var(--fts-duration-fast) var(--fts-easing-spring);

  &--dot {
    min-width: 8px;
    height: 8px;
    padding: 0;
    border-radius: 50%;
  }

  /* 脉冲动画：未读数量提示 */
  &--pulse {
    animation: badge-pulse 2s ease-in-out infinite;
  }

  &--hidden {
    display: none;
  }
}
</style>
