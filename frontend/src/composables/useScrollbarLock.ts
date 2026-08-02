/**
 * 滚动条锁定Composable v4 - 保留滚动条版本
 *
 * 【核心原理】不隐藏滚动条，只阻止滚动行为
 * 【与v3的区别】v3 设置 overflow:hidden（滚动条消失→页面变宽）
 *               v4 使用 position:fixed + scroll 事件拦截（滚动条保留→页面不变）
 * 【效果】弹窗打开时滚动条仍然可见，但无法滚动页面
 */

import { onUnmounted } from 'vue'

/** 已锁定的计数器 */
let lockCount = 0

/** 原始的滚动位置 */
let savedScrollTop = 0
let savedScrollLeft = 0

/** MutationObserver 实例 */
let observer: MutationObserver | null = null

/** scroll 事件处理器引用 */
let scrollHandler: ((e: Event) => void) | null = null
let wheelHandler: ((e: WheelEvent) => void) | null = null
let touchMoveHandler: ((e: TouchEvent) => void) | null = null

/**
 * 锁定滚动（保留滚动条，阻止滚动行为）
 *
 * 实现方式：
 * 1. 记录当前滚动位置
 * 2. 给 body 添加 position: fixed + 对应的 top/left 偏移
 *    → body 脱离文档流，固定在当前位置
 *    → 滚动条保留（因为内容高度不变）
 *    → 用户无法滚动（因为 body 已固定）
 * 3. 监听 scroll/wheel/touch 事件强制回滚
 */
function lockWithScrollbar(): void {
  if (lockCount === 0) {
    // 1. 记录当前滚动位置
    savedScrollTop = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop
    savedScrollLeft = window.scrollX || document.documentElement.scrollLeft || document.body.scrollLeft

    // 2. 计算 body 的偏移量（抵消 position: fixed 导致的跳动）
    const body = document.body
    const computedStyle = getComputedStyle(body)

    // 3. 固定 body 到当前位置
    body.style.position = 'fixed'
    body.style.top = `-${savedScrollTop}px`
    body.style.left = `-${savedScrollLeft}px`
    body.style.width = '100%'
    body.style.overflowY = 'scroll'  // 保持滚动条可见！

    // 4. 绑定事件监听器，防止任何形式的滚动
    scrollHandler = (e: Event) => {
      e.preventDefault()
      window.scrollTo(savedScrollLeft, savedScrollTop)
    }

    wheelHandler = (e: WheelEvent) => {
      e.preventDefault()
    }

    touchMoveHandler = (e: TouchEvent) => {
      if (lockCount > 0) {
        e.preventDefault()
      }
    }

    // 使用 passive: false 才能 preventDefault
    window.addEventListener('scroll', scrollHandler, { passive: false })
    window.addEventListener('wheel', wheelHandler, { passive: false })
    document.addEventListener('touchmove', touchMoveHandler, { passive: false })
  }
  lockCount++
}

/**
 * 解锁滚动（恢复原始状态）
 */
function unlockWithScrollbar(): void {
  lockCount = Math.max(0, lockCount - 1)

  if (lockCount === 0) {
    const body = document.body

    // 1. 移除事件监听器
    if (scrollHandler) {
      window.removeEventListener('scroll', scrollHandler)
      scrollHandler = null
    }
    if (wheelHandler) {
      window.removeEventListener('wheel', wheelHandler)
      wheelHandler = null
    }
    if (touchMoveHandler) {
      document.removeEventListener('touchmove', touchMoveHandler)
      touchMoveHandler = null
    }

    // 2. 恢复 body 样式
    body.style.position = ''
    body.style.top = ''
    body.style.left = ''
    body.style.width = ''
    body.style.overflowY = ''

    // 3. 恢复滚动位置
    window.scrollTo(savedScrollLeft, savedScrollTop)
  }
}

/**
 * 创建 MutationObserver 防御外部代码修改 body 的 position/overflow
 */
function createObserver(): void {
  if (observer) return

  observer = new MutationObserver((mutations) => {
    if (lockCount === 0) return

    for (const mutation of mutations) {
      if (mutation.type !== 'attributes') continue
      if (mutation.attributeName !== 'style') continue

      const body = document.body

      // 如果外部代码试图移除我们的 position:fixed，立即恢复
      if (body.style.position !== 'fixed' && lockCount > 0) {
        body.style.position = 'fixed'
        body.style.top = `-${savedScrollTop}px`
        body.style.left = `-${savedScrollLeft}px`
        body.style.width = '100%'
        body.style.overflowY = 'scroll'
      }
    }
  })

  observer.observe(document.body, { attributes: true, attributeFilter: ['style'] })
}

function destroyObserver(): void {
  if (observer) {
    observer.disconnect()
    observer = null
  }
}

/**
 * 滚动条锁定Composable v4
 *
 * @param visible 控制锁定的响应式变量
 */
export function useScrollbarLock(visible: { value: boolean }) {
  createObserver()

  let lastValue = visible.value

  // 高频检测
  const intervalId = setInterval(() => {
    if (visible.value !== lastValue) {
      lastValue = visible.value
      if (visible.value) {
        lockWithScrollbar()
      } else {
        unlockWithScrollbar()
      }
    }
  }, 16)

  // 立即检查初始状态
  if (visible.value) {
    lockWithScrollbar()
  }

  onUnmounted(() => {
    clearInterval(intervalId)
    if (visible.value) {
      unlockWithScrollbar()
    }
    if (lockCount === 0) {
      destroyObserver()
    }
  })

  return {
    lockBodyScroll: lockWithScrollbar,
    unlockBodyScroll: unlockWithScrollbar,
  }
}
