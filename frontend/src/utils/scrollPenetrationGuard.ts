/**
 * 滚动穿透守卫
 *
 * 弹层出现 → overflow:hidden（锁定滚动）；全部关闭 → 恢复。
 * 宽度稳定性由 html { scrollbar-gutter: stable } 保证，无需 padding-right 补偿。
 */
/** 带遮罩的弹层 */
const MODAL_SELECTOR = '.el-overlay, .el-drawer, .el-message-box'

let registered = false
let locked = false
let savedOverflow = ''

function hasLiveModal(): boolean {
  const nodes = document.querySelectorAll(MODAL_SELECTOR)
  for (const n of nodes) {
    const el = n as HTMLElement
    if (el.style.display === 'none') continue
    if (el.classList.contains('el-overlay-leave-active')) continue
    const rect = el.getBoundingClientRect()
    if (rect.width === 0 && rect.height === 0) continue
    return true
  }
  return false
}

function syncLock(): void {
  const hasModal = hasLiveModal()
  if (hasModal) {
    if (!locked) {
      locked = true
      savedOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      if (import.meta.env.DEV) {
        console.log('[scroll-guard] 弹层出现 → 锁定滚动')
      }
    }
  } else if (locked) {
    locked = false
    document.body.style.overflow = savedOverflow
    if (import.meta.env.DEV) {
      console.log('[scroll-guard] 弹层全部关闭 → 已恢复')
    }
  }
}

export function installScrollPenetrationGuard(): void {
  if (registered || typeof document === 'undefined') return
  registered = true

  const observer = new MutationObserver(() => syncLock())
  observer.observe(document.body, { childList: true, subtree: true })
  syncLock()
}
