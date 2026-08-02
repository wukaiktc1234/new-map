import { ref, onMounted, onUnmounted, type Ref } from 'vue'

/**
 * 快捷键配置接口
 */
export interface ShortcutConfig {
  key: string
  description: string
  handler: () => void
  ctrlKey?: boolean
  shiftKey?: boolean
  altKey?: boolean
}

/**
 * 快捷键状态接口
 */
interface ShortcutState {
  isActive: Ref<boolean>
  lastPressedKey: Ref<string | null>
  showHelp: Ref<boolean>
  highlightedButton: Ref<string | null>
}

/**
 * 检测是否为触摸设备
 */
const isTouchDevice = (): boolean => {
  if (typeof window === 'undefined') return false
  return (
    'ontouchstart' in window ||
    navigator.maxTouchPoints > 0 ||
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (navigator as any).msMaxTouchPoints > 0
  )
}

/**
 * 后厨快捷键Composable - 提供全局键盘快捷键支持
 * 仅在桌面端生效，触摸设备自动禁用
 */
export function useKeyboardShortcuts() {
  const state: ShortcutState = {
    isActive: ref(false),
    lastPressedKey: ref(null),
    showHelp: ref(false),
    highlightedButton: ref(null)
  }

  let shortcuts: ShortcutConfig[] = []
  let isRegistered = false

  /**
   * 默认快捷键映射表
   */
  const getDefaultShortcuts = (): ShortcutConfig[] => [
    {
      key: '1',
      description: '选中第一个待制作订单',
      handler: () => {}
    },
    {
      key: '2',
      description: '选中第二个待制作订单',
      handler: () => {}
    },
    {
      key: '3',
      description: '选中第三个待制作订单',
      handler: () => {}
    },
    {
      key: '4',
      description: '选中第四个待制作订单',
      handler: () => {}
    },
    {
      key: '5',
      description: '选中第五个待制作订单',
      handler: () => {}
    },
    {
      key: 'Enter',
      description: '开始制作选中订单',
      handler: () => {}
    },
    {
      key: ' ',
      description: '完成选中订单（空格键）',
      handler: () => {}
    },
    {
      key: 'Escape',
      description: '取消选择',
      handler: () => {}
    },
    {
      key: 'Tab',
      description: '切换到下一状态栏目',
      handler: () => {}
    },
    {
      key: 'F1',
      description: '刷新订单列表',
      handler: () => {}
    },
    {
      key: 'F5',
      description: '强制刷新页面',
      handler: () => {
        window.location.reload()
      }
    },
    {
      key: '?',
      description: '显示/隐藏快捷键帮助面板',
      handler: () => {
        state.showHelp.value = !state.showHelp.value
      }
    },
    {
      key: 'a',
      description: '全选/取消全选',
      ctrlKey: true,
      handler: () => {}
    }
  ]

  /**
   * 键盘事件处理器
   */
  const handleKeyDown = (event: KeyboardEvent): void => {
    // 如果在输入框中，不触发快捷键
    const target = event.target as HTMLElement
    const isInputFocused =
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.isContentEditable

    if (isInputFocused && !['Escape', 'F1', 'F5'].includes(event.key)) {
      return
    }

    // 查找匹配的快捷键
    const matchedShortcut = shortcuts.find((shortcut) => {
      const keyMatch =
        shortcut.key.toLowerCase() === event.key.toLowerCase() ||
        shortcut.key === event.code

      const ctrlMatch = shortcut.ctrlKey ? event.ctrlKey || event.metaKey : true
      const shiftMatch = shortcut.shiftKey ? event.shiftKey : !event.shiftKey
      const altMatch = shortcut.altKey ? event.altKey : !event.altKey

      return keyMatch && ctrlMatch && shiftMatch && altMatch
    })

    if (matchedShortcut) {
      event.preventDefault()

      // 更新状态
      state.lastPressedKey.value = matchedShortcut.key
      state.highlightedButton.value = matchedShortcut.key

      // 执行处理函数
      matchedShortcut.handler()

      // 短暂高亮后清除
      setTimeout(() => {
        state.highlightedButton.value = null
      }, 300)

      // 清除最后按键记录
      setTimeout(() => {
        state.lastPressedKey.value = null
      }, 1000)
    }
  }

  /**
   * 注册快捷键
   * @param customShortcuts - 自定义快捷键配置数组
   */
  const registerShortcuts = (customShortcuts?: ShortcutConfig[]): void => {
    if (isTouchDevice()) {
      return
    }

    if (isRegistered) {
      unregisterShortcuts()
    }

    shortcuts = customShortcuts || getDefaultShortcuts()
    isRegistered = true
    state.isActive.value = true

    document.addEventListener('keydown', handleKeyDown)
  }

  /**
   * 注销快捷键
   */
  const unregisterShortcuts = (): void => {
    if (!isRegistered) return

    document.removeEventListener('keydown', handleKeyDown)
    isRegistered = false
    state.isActive.value = false
    shortcuts = []
  }

  /**
   * 更新单个快捷键的处理函数
   * @param key - 快捷键标识
   * @param handler - 新的处理函数
   */
  const updateShortcutHandler = (key: string, handler: () => void): void => {
    const shortcut = shortcuts.find((s) => s.key === key)
    if (shortcut) {
      shortcut.handler = handler
    }
  }

  /**
   * 获取所有注册的快捷键列表（用于帮助面板显示）
   */
  const getShortcutList = (): ShortcutConfig[] => {
    return shortcuts.filter((s) => s.description)
  }

  /**
   * 切换帮助面板显示状态
   */
  const toggleHelp = (): void => {
    state.showHelp.value = !state.showHelp.value
  }

  /**
   * 组件挂载时自动注册
   */
  onMounted(() => {
    registerShortcuts()
  })

  /**
   * 组件卸载时自动注销
   */
  onUnmounted(() => {
    unregisterShortcuts()
  })

  return {
    // 状态
    isActive: state.isActive,
    lastPressedKey: state.lastPressedKey,
    showHelp: state.showHelp,
    highlightedButton: state.highlightedButton,

    // 方法
    registerShortcuts,
    unregisterShortcuts,
    updateShortcutHandler,
    getShortcutList,
    toggleHelp,

    // 工具方法
    getDefaultShortcuts
  }
}
