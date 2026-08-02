/**
 * v-lazy-load 图片懒加载指令
 *
 * 当图片进入可视区域时才加载真实 src，
 * 加载前显示占位背景，加载完成后渐显。
 *
 * 使用方式：
 * <img v-lazy-load="'https://example.com/photo.jpg'" alt="photo" />
 *
 * 或使用对象配置：
 * <img v-lazy-load="{ src: 'url.jpg', placeholder: '#f0f0f0', threshold: 0.1 }" />
 */

import type { Directive, DirectiveBinding } from 'vue'

interface LazyLoadValue {
  src: string
  /** 占位背景色或图片URL，默认使用CSS变量 */
  placeholder?: string
  /** IntersectionObserver 阈值（0-1），默认 0.05 */
  threshold?: number
  /** 根边距，默认 '50px' */
  rootMargin?: string
}

const observerMap = new WeakMap<HTMLElement, IntersectionObserver>()

function createObserver(el: HTMLElement, binding: DirectiveBinding<LazyLoadValue | string>): IntersectionObserver {
  const value = typeof binding.value === 'string' ? { src: binding.value } : binding.value
  const { threshold = 0.05, rootMargin = '50px' } = value

  // 设置初始占位样式
  el.style.transition = 'opacity var(--fts-duration-normal, 300ms) ease'
  el.style.opacity = '0'

  // 设置占位背景
  if (value.placeholder && value.placeholder.startsWith('http')) {
    el.setAttribute('data-src', value.src)
    ;(el as HTMLImageElement).src = value.placeholder
  } else {
    el.style.backgroundColor = value.placeholder || 'var(--fts-bg-tertiary, #f1f5f9)'
  }

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          loadImage(el, value.src)
          observer.unobserve(el)
          observerMap.delete(el)
        }
      })
    },
    { threshold, rootMargin }
  )

  return observer
}

function loadImage(el: HTMLElement, src: string) {
  const img = el as HTMLImageElement

  // 如果已经有data-src属性（说明用了placeholder图片方案）
  if (el.getAttribute('data-src')) {
    img.src = el.getAttribute('data-src')!
    el.removeAttribute('data-src')
  } else {
    img.src = src
  }

  img.addEventListener('load', () => {
    // 渐显动画
    requestAnimationFrame(() => {
      el.style.opacity = '1'
    })
  }, { once: true })

  img.addEventListener('error', () => {
    // 加载失败时显示半透明占位
    el.style.opacity = '0.5'
    el.style.background = 'var(--fts-bg-tertiary)'
  }, { once: true })
}

const directive: Directive<HTMLElement, LazyLoadValue | string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<LazyLoadValue | string>) {
    // 确保是 img 元素
    if (el.tagName !== 'IMG') {
      console.warn('[v-lazy-load] 指令只能应用于 img 元素')
      return
    }

    const observer = createObserver(el, binding)
    observer.observe(el)
    observerMap.set(el, observer)
  },

  updated(el: HTMLElement, binding: DirectiveBinding<LazyLoadValue | string>) {
    // src 变化时重新观察
    const observer = observerMap.get(el)
    if (observer) {
      observer.disconnect()
    }
    const newObserver = createObserver(el, binding)
    newObserver.observe(el)
    observerMap.set(el, newObserver)
  },

  unmounted(el: HTMLElement) {
    const observer = observerMap.get(el)
    if (observer) {
      observer.disconnect()
      observerMap.delete(el)
    }
  },
}

export default directive
