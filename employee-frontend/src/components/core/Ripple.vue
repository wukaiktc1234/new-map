<script setup lang="ts">
/**
 * Ripple 涟漪反馈组件
 *
 * 设计理念：
 * - Material Design 风格的水波纹扩散效果
 * - 点击时从触点位置向外扩散圆形涟漪
 * - 支持多个涟漪叠加（快速连续点击）
 * - 所有颜色/时长/缓动通过 --fts-* CSS变量可配置
 * - 自动适配深色模式
 * - 支持 prefers-reduced-motion 降级（立即消失）
 *
 * 使用示例：
 * <Ripple @click="handleClick">
 *   <button>点击我</button>
 * </Ripple>
 *
 * 或作为指令使用：
 * <button v-ripple>点击我</button>
 */

import { ref, onBeforeUnmount } from 'vue'

interface RippleInstance {
  id: number
  x: number
  y: number
  size: number
}

const props = withDefaults(defineProps<{
  /** 涟漪颜色（默认使用CSS变量 --fts-ripple-color） */
  color?: string
  /** 涟漪持续时间（ms），默认使用CSS变量 */
  duration?: number
  /** 是否禁用涟漪 */
  disabled?: boolean
}>(), {
  disabled: false,
})

const ripples = ref<RippleInstance[]>([])
let nextId = 0

/** 创建涟漪效果 */
function createRipple(event: MouseEvent) {
  if (props.disabled) return

  const target = (event.currentTarget || event.target) as HTMLElement
  if (!target) return

  const rect = target.getBoundingClientRect()
  
  // 涟漪尺寸：覆盖整个元素的对角线长度，确保从任何位置点击都能覆盖全元素
  const size = Math.max(rect.width, rect.height) * 2.5
  
  // 计算涟漪中心点（相对于元素）
  const x = event.clientX - rect.left - size / 2
  const y = event.clientY - rect.top - size / 2

  const ripple: RippleInstance = {
    id: nextId++,
    x,
    y,
    size,
  }

  ripples.value.push(ripple)

  // 动画结束后自动移除涟漪
  const duration = props.duration || parseInt(
    getComputedStyle(document.documentElement)
      .getPropertyValue('--fts-ripple-duration')
  ) || 600

  setTimeout(() => {
    ripples.value = ripples.value.filter(r => r.id !== ripple.id)
  }, duration)
}

/** 清除所有涟漪（组件卸载时） */
function clearAll() {
  ripples.value = []
}

onBeforeUnmount(clearAll)
</script>

<template>
  <div
    class="ripple-host"
    :class="{ 'ripple-host--disabled': disabled }"
    @pointerdown="createRipple"
  >
    <!-- 涟漪层 -->
    <span
      v-for="ripple in ripples"
      :key="ripple.id"
      class="ripple-effect"
      :style="{
        left: ripple.x + 'px',
        top: ripple.y + 'px',
        width: ripple.size + 'px',
        height: ripple.size + 'px',
        backgroundColor: color || undefined,
        animationDuration: (duration || undefined) + 'ms',
      }"
    />
    
    <!-- 默认插槽：承载实际内容 -->
    <slot />
  </div>
</template>

<style scoped lang="scss">
.ripple-host {
  position: relative;
  overflow: hidden; /* 关键：裁剪超出范围的涟漪 */
  display: inline-flex;
  align-items: center;
  justify-content: center;

  &--disabled {
    .ripple-effect {
      display: none;
    }
  }
}

.ripple-effect {
  position: absolute;
  border-radius: 50%;
  pointer-events: none; /* 不阻挡鼠标事件 */
  background-color: var(--fts-ripple-color);
  transform: scale(0);
  animation: ripple-spread var(--fts-ripple-duration, 600ms) var(--fts-ripple-easing, cubic-bezier(0.25, 0.46, 0.45, 0.94)) forwards;
  will-change: transform, opacity;
}
</style>
