/**
 * 消息轮询 composable
 * 提供定时轮询消息的能力，支持页面可见性感知
 * 隐藏页面时自动暂停，显示时自动恢复
 */

import { ref, onMounted, onUnmounted } from 'vue'

/** 最大连续失败次数 */
const MAX_RETRY_COUNT = 3

/**
 * 消息轮询 hook
 * @param intervalMs - 轮询间隔（毫秒），默认30秒
 * @returns 轮询控制对象
 */
export function useMessagePolling(intervalMs: number = 30000) {
  // ============================================================
  // 状态
  // ============================================================

  /** 是否正在轮询 */
  const isPolling = ref(false)

  /** 连续失败计数 */
  let retryCount = 0

  /** 定时器引用 */
  let timer: ReturnType<typeof setInterval> | null = null

  /** 当前绑定的 fetch 函数引用 */
  let boundFetchFn: (() => Promise<void>) | null = null

  // ============================================================
  // 核心方法
  // ============================================================

  /**
   * 开始轮询
   * @param fetchFn - 数据获取函数（每次轮询执行）
   */
  async function startPolling(fetchFn: () => Promise<void>): Promise<void> {
    // 先停止已有的轮询
    stopPolling()

    boundFetchFn = fetchFn
    isPolling.value = true
    retryCount = 0

    // 立即执行一次
    try {
      await fetchFn()
    } catch {
      // 首次执行失败不阻止启动轮询
    }

    // 设置定时轮询
    timer = setInterval(async () => {
      // 页面不可见时不执行
      if (document.hidden) return

      try {
        await fetchFn()
        retryCount = 0
      } catch {
        retryCount++
        if (retryCount > MAX_RETRY_COUNT) {
          // 超过最大重试次数，暂停轮询并重置计数
          stopPolling()
          retryCount = 0
        }
      }
    }, intervalMs)
  }

  /**
   * 停止轮询
   */
  function stopPolling(): void {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isPolling.value = false
    boundFetchFn = null
  }

  /**
   * 手动触发一次刷新（不影响轮询周期）
   */
  async function refreshOnce(): Promise<void> {
    if (boundFetchFn) {
      try {
        await boundFetchFn()
      } catch {
        // 静默处理手动刷新失败
      }
    }
  }

  // ============================================================
  // 页面可见性感知
  // ============================================================

  /**
   * 处理页面可见性变化
   * hidden 时暂停轮询，visible 时恢复
   */
  function handleVisibilityChange(): void {
    if (document.hidden) {
      // 页面隐藏时不清除定时器，只在回调中跳过执行
      // 这样恢复时不需要重新设置定时器
      return
    }

    // 页面重新可见时，立即执行一次刷新
    if (isPolling.value && boundFetchFn) {
      boundFetchFn().catch(() => {
        // 恢复时的首次刷新失败不处理
      })
    }
  }

  // ============================================================
  // 生命周期
  // ============================================================

  onMounted(() => {
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onUnmounted(() => {
    stopPolling()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return {
    /** 是否正在轮询 */
    isPolling,
    /** 开始轮询 */
    startPolling,
    /** 停止轮询 */
    stopPolling,
    /** 手动刷新一次 */
    refreshOnce,
  }
}
