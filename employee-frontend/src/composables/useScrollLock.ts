/**
 * useScrollLock - 滚动锁定 composable
 *
 * 用于在弹层（日期选择器、下拉菜单等）打开时阻止背景页面滚动，
 * 避免移动端滚动冲突和面板闪烁问题。
 */
import { onBeforeUnmount } from 'vue'

let lockCount = 0
let originalOverflow = ''
let originalPosition = ''
let originalWidth = ''

// [M5] 显式保存滚动位置变量，避免在多次 lock/unlock 或嵌套场景下位置丢失
// 比 body.style.top 解析更可靠（top 可能被其他逻辑覆盖）
let savedScrollY = 0

/** 锁定 body 滚动（支持嵌套调用，引用计数） */
export function lockBodyScroll(): void {
  if (lockCount === 0) {
    const body = document.body
    originalOverflow = body.style.overflow
    originalPosition = body.style.position
    originalWidth = body.style.width

    // [M5] 显式保存当前滚动位置到独立变量
    savedScrollY = window.scrollY

    // 锁定
    body.style.overflow = 'hidden'
    body.style.position = 'fixed'
    body.style.top = `-${savedScrollY}px`
    body.style.left = '0'
    body.style.right = '0'
    body.style.width = '100%'
  }
  lockCount++
}

/** 解锁 body 滚动（引用计数归零时才真正解锁） */
export function unlockBodyScroll(): void {
  lockCount = Math.max(0, lockCount - 1)
  if (lockCount === 0) {
    const body = document.body
    body.style.overflow = originalOverflow
    body.style.position = originalPosition
    body.style.top = ''
    body.style.left = ''
    body.style.right = ''
    body.style.width = originalWidth

    // [M5] 使用显式保存的滚动位置恢复，而非从 body.style.top 解析
    // 避免在嵌套锁定/其他样式覆盖场景下位置计算错误
    window.scrollTo(0, savedScrollY)
  }
}

/**
 * useScrollLock - 自动管理的滚动锁定 hook
 *
 * @returns { lock, unlock } 手动控制函数（通常配合 @visible-change 使用）
 *
 * @example
 * ```vue
 * <el-date-picker
 *   :teleported="true"
 *   @visible-change="(val) => val ? lock() : unlock()"
 * />
 * ```
 */
export function useScrollLock() {
  onBeforeUnmount(() => {
    // 组件卸载时确保解锁
    if (lockCount > 0) {
      lockCount = 1
      unlockBodyScroll()
    }
  })

  return {
    lock: lockBodyScroll,
    unlock: unlockBodyScroll,
  }
}
