import { onMounted, ref, nextTick } from 'vue'

export interface StaggerOptions {
  staggerDelay?: number
  initialDelay?: number
  duration?: number
  type?: 'fade-up' | 'fade-in' | 'slide-left' | 'scale'
  threshold?: number
}

const EASE_CUBIC = 'cubic-bezier(0.16,1,0.3,1)'

export function useStaggerAnimation(options: StaggerOptions = {}) {
  const {
    staggerDelay = 80,
    initialDelay = 0,
    duration = 400,
    type = 'fade-up',
    threshold = 0.1,
  } = options

  const containerRef = ref<HTMLElement | null>(null)
  const isVisible = ref(false)

  const getTransform = () => {
    switch (type) {
      case 'fade-up': return 'translateY(24px)'
      case 'slide-left': return 'translateX(-20px)'
      case 'scale': return 'scale(0.95)'
      default: return 'none'
    }
  }

  const applyAnimation = (el: HTMLElement) => {
    const children = Array.from(el.children)
    if (children.length === 0) return

    children.forEach((child) => {
      const c = child as HTMLElement
      c.style.opacity = '0'
      if (type !== 'fade-in') {
        c.style.transform = getTransform()
        c.style.transitionProperty = 'opacity,transform'
      } else {
        c.style.transitionProperty = 'opacity'
      }
      c.style.transitionDuration = duration + 'ms'
      c.style.transitionTimingFunction = EASE_CUBIC
      c.style.willChange = 'opacity,transform'
    })

    const baseDelay = Math.max(initialDelay, 16)

    children.forEach((child, index) => {
      const c = child as HTMLElement
      const delay = baseDelay + index * staggerDelay

      setTimeout(() => {
        c.style.opacity = '1'
        if (type !== 'fade-in') {
          c.style.transform = ''
        }
        c.style.transitionDelay = (delay - baseDelay) + 'ms'

        setTimeout(() => {
          c.style.willChange = 'auto'
        }, delay + duration + 50)
      }, delay)
    })
  }

  onMounted(async () => {
    await nextTick()

    if (!containerRef.value) return

    if (!('IntersectionObserver' in window)) {
      isVisible.value = true
      applyAnimation(containerRef.value)
      return
    }

    requestAnimationFrame(() => {
      if (!containerRef.value) return

      const observer = new IntersectionObserver(
        ([entry]) => {
          if (entry.isIntersecting) {
            observer.disconnect()
            isVisible.value = true
            applyAnimation(containerRef.value!)
          }
        },
        { threshold }
      )

      observer.observe(containerRef.value)
    })
  })

  return {
    containerRef,
    isVisible,
  }
}
