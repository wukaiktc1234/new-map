import type { Directive } from 'vue'

/**
 * 表格横向拖拽滚动指令
 *
 * 用法：<div v-table-drag-scroll class="data-table">...</div>
 *
 * 特性：
 * - 按住鼠标左键拖动，横向滚动表格内容（requestAnimationFrame 节流，性能好）
 * - 仅当内容超出容器（可横向滚动）时启用，不干扰正常滚动
 * - 拖动超过阈值时抑制随后的 click，避免误触行内按钮/下拉
 * - 表格内交互元素（输入框/按钮/下拉/分页等）上不启动拖动
 */
const INTERACTIVE_SELECTOR = [
  'input',
  'textarea',
  'select',
  'button',
  'a',
  '[contenteditable]',
  '.el-select__wrapper',
  '.el-input-number',
  '.el-switch',
  '.el-checkbox',
  '.el-radio',
  '.el-date-editor',
  '.el-pagination',
  '.el-table__fixed',
  '.el-table__fixed-right',
].join(', ')

export const vTableDragScroll: Directive<HTMLElement> = {
  mounted(el) {
    // el-table 的实际滚动容器：新版 element-plus 为 .el-scrollbar__wrap（内容超宽时 overflow-x:auto），
    // 旧的 .el-table__body-wrapper 不承担滚动（overflow hidden），仅作兼容回退
    const scrollEl =
      (el.querySelector('.el-scrollbar__wrap') as HTMLElement | null) ||
      (el.querySelector('.el-table__body-wrapper') as HTMLElement | null)
    if (!scrollEl) return

    const state = {
      down: false,
      startX: 0,
      startScroll: 0,
      moved: false,
      raf: 0,
    }

    const isInteractive = (target: EventTarget | null): boolean =>
      target instanceof Element && target.closest(INTERACTIVE_SELECTOR) !== null

    const onMouseDown = (e: MouseEvent): void => {
      if (e.button !== 0 || isInteractive(e.target)) return
      if (scrollEl.scrollWidth <= scrollEl.clientWidth + 1) return
      state.down = true
      state.moved = false
      state.startX = e.clientX
      state.startScroll = scrollEl.scrollLeft
      scrollEl.style.cursor = 'grabbing'
      document.body.style.userSelect = 'none'
    }

    const onMouseMove = (e: MouseEvent): void => {
      if (!state.down) return
      const dx = e.clientX - state.startX
      if (Math.abs(dx) > 4) state.moved = true
      if (state.raf) return
      state.raf = requestAnimationFrame(() => {
        state.raf = 0
        scrollEl.scrollLeft = state.startScroll - dx
      })
    }

    const onMouseUp = (): void => {
      if (!state.down) return
      state.down = false
      if (state.raf) {
        cancelAnimationFrame(state.raf)
        state.raf = 0
      }
      scrollEl.style.cursor = ''
      document.body.style.userSelect = ''
      resetMovedTimer()
    }

    // 拖动后抑制随后的 click（一次性：复位 moved，避免后续点击被永久阻止）
    const onClickCapture = (e: Event): void => {
      if (state.moved) {
        state.moved = false
        e.stopPropagation()
        e.preventDefault()
      }
    }

    // 兜底：鼠标抬起事件绑定在 window（含拖出窗口场景），并延时复位 moved，
    // 防止 mouseup 丢失导致 moved 残留、后续点击全部被抑制（"突然点击无反应"）
    const resetMovedTimer = (): void => {
      window.setTimeout(() => {
        state.moved = false
        state.down = false
      }, 0)
    }

    el.addEventListener('mousedown', onMouseDown)
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('mouseup', onMouseUp)
    el.addEventListener('click', onClickCapture, true)

    ;(el as unknown as { __dragScrollCleanup?: () => void }).__dragScrollCleanup = () => {
      el.removeEventListener('mousedown', onMouseDown)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
      el.removeEventListener('click', onClickCapture, true)
      resetMovedTimer()
    }
  },
  unmounted(el) {
    ;(el as unknown as { __dragScrollCleanup?: () => void }).__dragScrollCleanup?.()
  },
}
