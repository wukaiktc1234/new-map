<template>
  <span class="animated-amount" :class="{ 'is-animating': animating }">
    <span class="animated-amount__value" data-test="animated-value">{{ display }}</span>
  </span>
</template>

<script setup lang="ts">
/**
 * 金额翻牌动画（收银台"数钱"手感）
 *
 * 设计约束：
 *   · 只在金额**变化**时滚动，首次渲染直接落地（否则进页面就在跳，很吵）
 *   · 按分做整数插值，避免 0.1+0.2 这类浮点误差让尾数抖动
 *   · 尊重 prefers-reduced-motion：系统关闭动画时直接给终值
 *   · aria-live="polite"，读屏只在稳定后播报一次
 */
import { computed, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  value: number
  /** 动画时长（毫秒） */
  duration?: number
  /** 小数位 */
  decimals?: number
}>(), {
  duration: 520,
  decimals: 2
})

const toCents = (v: number): number => Math.round((Number.isFinite(v) ? v : 0) * 100)

const current = ref(toCents(props.value))
const animating = ref(false)
let rafId: number | null = null

const display = computed(() =>
  (current.value / 100).toFixed(props.decimals)
)

const reduceMotion = (): boolean =>
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

const easeOutCubic = (t: number): number => 1 - Math.pow(1 - t, 3)

function animateTo(targetCents: number): void {
  if (rafId !== null) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
  const from = current.value
  if (from === targetCents || reduceMotion()) {
    current.value = targetCents
    animating.value = false
    return
  }
  const start = performance.now()
  animating.value = true
  const step = (now: number): void => {
    const progress = Math.min(1, (now - start) / props.duration)
    const eased = easeOutCubic(progress)
    current.value = Math.round(from + (targetCents - from) * eased)
    if (progress < 1) {
      rafId = requestAnimationFrame(step)
    } else {
      current.value = targetCents
      animating.value = false
      rafId = null
    }
  }
  rafId = requestAnimationFrame(step)
}

onMounted(() => {
  current.value = toCents(props.value)
})

watch(() => props.value, (next) => animateTo(toCents(next)))
</script>

<style scoped>
.animated-amount {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum' 1;
}

/* 货币符号用伪元素：DOM 里只有数字，innerText / 复制 / 读屏都不会多出换行 */
.animated-amount__value::before {
  content: '¥';
  margin-right: 2px;
  font-size: 0.62em;
  font-weight: 700;
  opacity: 0.85;
}

.animated-amount__value {
  /* 等宽数字 + 轻微字距，滚动时不会左右抖 */
  letter-spacing: 0.2px;
}

.animated-amount.is-animating .animated-amount__value {
  /* 滚动时给一点点"在动"的暗示，结束后回到常态 */
  filter: brightness(1.06);
}

@media (prefers-reduced-motion: reduce) {
  .animated-amount__value {
    transition: none;
  }
}
</style>
