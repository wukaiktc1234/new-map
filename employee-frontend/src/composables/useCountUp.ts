import { ref, watch, onMounted, computed, nextTick } from 'vue'

export interface CountUpOptions {
  target: number | (() => number)
  start?: number
  duration?: number
  decimals?: number
  prefix?: string
  suffix?: string
  separator?: string
  immediate?: boolean
  /** 动画延迟启动时间（毫秒），用于交错入场动画场景 */
  initialDelay?: number
}

export function useCountUp(options: CountUpOptions) {
  const {
    start = 0,
    duration = 1200,
    decimals = 0,
    prefix = '',
    suffix = '',
    separator = ',',
    immediate = true,
  } = options

  const current = ref(start)
  const isAnimating = ref(false)

  const targetValue = computed(() =>
    typeof options.target === 'function' ? options.target() : options.target
  )

  const formatNumber = (num: number): string => {
    const fixed = num.toFixed(decimals)
    if (!separator || decimals > 0) return fixed

    const [intPart, decPart] = fixed.split('.')
    const formatted = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, separator)
    return decPart ? formatted + '.' + decPart : formatted
  }

  const displayValue = computed(() => prefix + formatNumber(current.value) + suffix)

  let startTime: number | null = null
  let rafId: number | null = null
  let animationTimer: ReturnType<typeof setTimeout> | null = null

  const easeOutExpo = (t: number): number => t === 1 ? 1 : 1 - Math.pow(2, -10 * t)

  const animate = (timestamp: number) => {
    if (!startTime) startTime = timestamp
    const elapsed = timestamp - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easedProgress = easeOutExpo(progress)

    current.value = start + (targetValue.value - start) * easedProgress

    if (progress < 1) {
      rafId = requestAnimationFrame(animate)
    } else {
      current.value = targetValue.value
      isAnimating.value = false
      rafId = null
      startTime = null
    }
  }

  const startAnimation = () => {
    if (rafId !== null) cancelAnimationFrame(rafId)
    if (animationTimer !== null) clearTimeout(animationTimer)
    isAnimating.value = true
    current.value = start
    startTime = null

    animationTimer = setTimeout(() => {
      rafId = requestAnimationFrame(animate)
    }, 50)
  }

  watch(targetValue, () => {
    startAnimation()
  })

  onMounted(async () => {
    await nextTick()
    if (immediate) {
      startAnimation()
    }
  })

  return {
    displayValue,
    current,
    isAnimating,
    startAnimation,
  }
}
