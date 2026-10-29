import { onUnmounted, ref } from 'vue'
import shiftApi from '@/api/shiftExceptions'
import type { ScanStatus } from '@/api/shiftExceptions'
import logger from '@/utils/logger'

export interface ScanPollingOptions {
  /** 轮询间隔，默认 2s */
  intervalMs?: number
  /** 总超时，默认 120s */
  timeoutMs?: number
  /** 支付成功回调（只在这里才允许刷新账目） */
  onPaid: (status: ScanStatus) => void | Promise<void>
  /** 超时 / 关闭 */
  onTimeout?: () => void
  /** 查询失败（网络抖动不终止，连续 3 次才报错） */
  onError?: (error: unknown) => void
}

/**
 * 扫码支付轮询
 *
 * 约束（审计要求）：
 *   1) 轮询到 paid 之前**绝不**乐观更新任何账目 —— 本模块没有任何「先加钱」的动作；
 *   2) 定时器与请求都可在任意时刻 stop()，组件卸载自动清理；
 *   3) 页面隐藏时暂停（避免后台标签页疯狂打接口），恢复可见后继续；
 *   4) 连续 3 次查询异常才判定失败，单次抖动只记日志。
 */
export function useScanPaymentPolling() {
  const polling = ref(false)
  const remainMs = ref(0)

  let timer: ReturnType<typeof setTimeout> | null = null
  let deadline = 0
  let stopped = false
  let consecutiveErrors = 0
  let currentPayNo = ''

  const clearTimer = (): void => {
    if (timer !== null) {
      clearTimeout(timer)
      timer = null
    }
  }

  const stop = (): void => {
    stopped = true
    polling.value = false
    clearTimer()
  }

  function schedule(fn: () => void, ms: number): void {
    clearTimer()
    timer = setTimeout(fn, ms)
  }

  async function tick(options: Required<Pick<ScanPollingOptions, 'intervalMs' | 'timeoutMs' | 'onPaid'>> & ScanPollingOptions): Promise<void> {
    if (stopped || !currentPayNo) return
    const left = deadline - Date.now()
    remainMs.value = Math.max(0, left)
    if (left <= 0) {
      stop()
      options.onTimeout?.()
      return
    }

    try {
      const status = await shiftApi.queryScanStatus(currentPayNo)
      consecutiveErrors = 0
      if (stopped) return
      if (status?.status === 'paid') {
        stop()
        // 只有这一刻才允许刷新账目（由调用方的 onPaid 决定怎么刷）
        await options.onPaid(status)
        return
      }
      if (status?.status === 'closed' || status?.status === 'expired') {
        stop()
        options.onTimeout?.()
        return
      }
    } catch (error) {
      consecutiveErrors += 1
      logger.warnMessage('[扫码支付] 状态查询失败', error)
      if (consecutiveErrors >= 3) {
        stop()
        options.onError?.(error)
        return
      }
    }

    if (stopped) return
    schedule(() => { void tick(options) }, options.intervalMs)
  }

  function start(payNo: string, options: ScanPollingOptions): void {
    stop()
    stopped = false
    consecutiveErrors = 0
    currentPayNo = payNo
    const intervalMs = options.intervalMs ?? 2000
    const timeoutMs = options.timeoutMs ?? 120000
    deadline = Date.now() + timeoutMs
    remainMs.value = timeoutMs
    polling.value = true
    schedule(() => {
      void tick({ intervalMs, timeoutMs, onPaid: options.onPaid, onTimeout: options.onTimeout, onError: options.onError })
    }, intervalMs)
  }

  onUnmounted(() => stop())

  return { start, stop, polling, remainMs }
}
