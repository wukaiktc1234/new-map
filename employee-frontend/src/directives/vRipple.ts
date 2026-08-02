/**
 * v-ripple 全局指令
 *
 * 为任意元素添加 Material Design 风格的涟漪点击反馈。
 * 使用方式：v-ripple 或 v-ripple="{ color: '#xxx', duration: 500 }"
 *
 * 示例：
 * <button v-ripple>点击我</button>
 * <div v-ripple="{ color: 'rgba(0,122,255,0.2)' }">可点击区域</div>
 */

import type { Directive, DirectiveBinding } from 'vue'

interface RippleDirectiveValue {
  color?: string
  duration?: number
  disabled?: boolean
}

let nextId = 0

const directive: Directive<HTMLElement, RippleDirectiveValue | boolean> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<RippleDirectiveValue | boolean>) {
    const options = typeof binding.value === 'object' ? binding.value : {}
    const disabled = binding.value === false || options.disabled

    el.setAttribute('data-ripple', 'true')
    if (disabled) {
      el.setAttribute('data-ripple-disabled', 'true')
    }

    el.style.position = el.style.position || 'relative'
    el.style.overflow = el.style.overflow || 'hidden'
    // eslint-disable-next-line @typescript-eslint/no-explicit-any -- webkitTapHighlightColor 是标准属性但 TypeScript DOM 类型声明未收录
    ;(el.style as any).webkitTapHighlightColor = 'transparent'

    function handlePointerDown(event: PointerEvent) {
      if (disabled) return

      const rect = el.getBoundingClientRect()
      const size = Math.max(rect.width, rect.height) * 2.5
      const x = event.clientX - rect.left - size / 2
      const y = event.clientY - rect.top - size / 2

      const ripple = document.createElement('span')
      ripple.className = 'v-ripple-effect'
      ripple.setAttribute('data-ripple-id', String(++nextId))
      ripple.style.cssText = `
        position: absolute;
        left: ${x}px;
        top: ${y}px;
        width: ${size}px;
        height: ${size}px;
        border-radius: 50%;
        pointer-events: none;
        background-color: ${options.color || ''};
        transform: scale(0);
        z-index: 0;
      `

      // 应用CSS变量控制的颜色（如果未通过options指定）
      if (!options.color) {
        ripple.style.backgroundColor = getComputedStyle(document.documentElement)
          .getPropertyValue('--fts-ripple-color').trim() || 'rgba(0, 122, 255, 0.15)'
      }

      const duration = options.duration || parseInt(
        getComputedStyle(document.documentElement)
          .getPropertyValue('--fts-ripple-duration')
      ) || 600

      ripple.style.animation = `ripple-spread ${duration}ms cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards`
      ripple.style.willChange = 'transform, opacity'

      el.appendChild(ripple)

      setTimeout(() => {
        ripple.remove()
      }, duration + 50)
    }

    ;(el as any)._rippleHandler = handlePointerDown
    el.addEventListener('pointerdown', handlePointerDown as EventListener)
  },

  updated(el: HTMLElement, binding: DirectiveBinding<RippleDirectiveValue | boolean>) {
    const options = typeof binding.value === 'object' ? binding.value : {}
    const disabled = binding.value === false || options.disabled

    if (disabled) {
      el.setAttribute('data-ripple-disabled', 'true')
    } else {
      el.removeAttribute('data-ripple-disabled')
    }
  },

  unmounted(el: HTMLElement) {
    const handler = (el as any)._rippleHandler
    if (handler) {
      el.removeEventListener('pointerdown', handler as EventListener)
      delete (el as any)._rippleHandler
    }
    
    // 清除残留的涟漪元素
    const ripples = el.querySelectorAll('.v-ripple-effect')
    ripples.forEach(r => r.remove())
    el.removeAttribute('data-ripple')
    el.removeAttribute('data-ripple-disabled')
  },
}

export default directive
