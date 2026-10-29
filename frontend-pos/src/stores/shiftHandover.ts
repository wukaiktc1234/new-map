import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import shiftApi from '@/api/shiftExceptions'
import type { ExceptionOrder, ExceptionTab, ScanPayment, ShiftStats } from '@/api/shiftExceptions'
import logger from '@/utils/logger'
import { classifyShiftFailure } from '@/utils/shiftStatus'

/**
 * 交班日结 Store —— 账目数字的唯一真相在服务端
 *
 * 铁律（审计要求）：
 *   1) 前端不做任何金额自减：作废/补收款成功后一律 refreshStats() 用后端整体替换；
 *   2) pendingExceptionCount 优先取后端 shiftStats.pendingExceptionCount，
 *      后端没给时才退化为列表长度（列表同样来自后端，仍不是前端算账）；
 *   3) 交班按钮 disabled 只认 canEndShift，不认任何本地状态。
 */
export const useShiftHandoverStore = defineStore('shiftHandover', () => {
  // ---------- state ----------
  const stats = ref<ShiftStats | null>(null)
  const statsUpdatedAt = ref<number>(0)
  const terminalId = ref<string>('')
  const pending = ref<ExceptionOrder[]>([])
  const processed = ref<ExceptionOrder[]>([])
  const tab = ref<ExceptionTab>('pending')
  const loadingList = ref(false)
  const loadingStats = ref(false)
  const lastError = ref<string>('')
  /**
   * 班次统计接口的三态：ok / no-shift / not-implemented(404) / error(网络或 5xx)
   * 区分「接口不存在」与「真的没开班/网络失败」，避免把 404 当成「统计失败」而让交班按钮永久灰掉却说不清原因。
   */
  const statsState = ref<'ok' | 'no-shift' | 'not-implemented' | 'forbidden' | 'error'>('ok')
  /**
   * 403：本账号无权访问收银数据（不是网络故障，也不是"没开班"）。
   * 必须让 UI 明确提示并阻断交班，绝不允许静默 console.error 了事。
   */
  const statsForbidden = ref(false)
  /** 写操作进行中（作废/补收款/扫码下单），用于防连点 */
  const submitting = ref(false)

  // ---------- getters ----------
  const currentList = computed(() => (tab.value === 'pending' ? pending.value : processed.value))

  /** 待处理异常数：交班开关的唯一依据 */
  const pendingExceptionCount = computed<number>(() => {
    const fromServer = stats.value?.pendingExceptionCount
    if (typeof fromServer === 'number' && Number.isFinite(fromServer)) return fromServer
    return pending.value.length
  })

  const hasPendingException = computed(() => pendingExceptionCount.value > 0)

  /** 交班按钮是否可用：有异常单 / 统计未就绪 / 无权限 都不行 */
  const canEndShift = computed(() =>
    !hasPendingException.value && stats.value !== null && !statsForbidden.value
  )

  const handoverCash = computed(() => stats.value?.handoverCash ?? 0)
  const drawerExpected = computed(() => stats.value?.drawerExpected ?? 0)
  const cashIn = computed(() => stats.value?.cashIn ?? 0)
  const cashRefund = computed(() => stats.value?.cashRefund ?? 0)
  const openingCash = computed(() => stats.value?.openingCash ?? 0)
  const statsStale = computed(() => Date.now() - statsUpdatedAt.value > 60000)

  // ---------- actions ----------
  function setTerminal(id: string): void {
    terminalId.value = id || ''
  }

  /** 后端整体替换：任何字段都不在前端拼接 */
  function applyStats(next: ShiftStats | null): void {
    stats.value = next
    statsUpdatedAt.value = Date.now()
  }

  async function refreshStats(): Promise<ShiftStats | null> {
    if (!terminalId.value) return stats.value
    loadingStats.value = true
    try {
      const next = await shiftApi.getStats(terminalId.value)
      if (next) {
        applyStats(next)
        statsState.value = next ? 'ok' : 'no-shift'
      } else {
        statsState.value = 'no-shift'
      }
      lastError.value = ''
      statsForbidden.value = false
      return stats.value
    } catch (error) {
      const status = Number((error as { status?: number })?.status ?? (error as { code?: number })?.code ?? 0)
      if (status === 403) {
        // 越权：属于"要人授权"的业务状态，UI 必须说清楚，并阻断交班
        statsForbidden.value = true
        statsState.value = 'error'
        lastError.value = '当前账号无权限访问收银数据，请联系店长授权'
        logger.warnMessage('[交班] shiftStats 403：当前账号无收银数据权限', error)
        return null
      }
      const kind = classifyShiftFailure(error)
      statsState.value = kind === 'bad-request' ? 'error' : kind
      if (kind === 'forbidden') {
        statsForbidden.value = true
        lastError.value = '当前账号无权限访问收银数据，请联系店长授权'
      } else {
        lastError.value = kind === 'not-implemented'
          ? '班次统计接口未提供（该功能未上线）'
          : '统计刷新失败'
      }
      logger.warnMessage('[交班] shiftStats 拉取失败（' + kind + '）', error)
      return null
    } finally {
      loadingStats.value = false
    }
  }

  async function loadExceptions(nextTab: ExceptionTab = tab.value): Promise<void> {
    if (!terminalId.value) return
    tab.value = nextTab
    loadingList.value = true
    try {
      const list = (await shiftApi.listExceptions(terminalId.value, nextTab)) || []
      if (nextTab === 'pending') pending.value = list
      else processed.value = list
      lastError.value = ''
    } catch (error) {
      lastError.value = '异常单列表加载失败'
      logger.warnMessage('[交班] 异常单拉取失败', error)
    } finally {
      loadingList.value = false
    }
  }

  /** 抽屉打开 / 15s 轮询 / 操作完成后统一走这里：先列表，再统计 */
  async function refreshAll(): Promise<void> {
    await Promise.all([loadExceptions(tab.value), refreshStats()])
  }

  /**
   * 作废：成功后不做本地删除，直接重拉列表 + 统计。
   * 多终端并发时永远不会出现「我以为处理了，别人又看到它」。
   */
  async function voidOrder(orderId: string, payload: { reason: string; remark?: string }): Promise<boolean> {
    submitting.value = true
    try {
      // 后端契约：void 入参是 { reason, note }（UI 里的 remark 映射为 note）
      const res = await shiftApi.voidOrder(
        orderId,
        { reason: payload.reason, note: payload.remark },
        buildIdempotencyKey('void', orderId),
      )
      releaseIdempotencyKey('void', orderId)
      await refreshAll()
      return res?.replayed === true
    } finally {
      submitting.value = false
    }
  }

  /**
   * 补收款（异常单）
   * @param amountFen 应收（**分**）
   * @param cashReceivedFen 实收（**分**）
   * 单位由 UI 层换算（ResolveDialog 内 yuanToFen 一次），store 不再做任何金额换算。
   */
  async function resolveCash(orderId: string, amountFen: number, cashReceivedFen?: number): Promise<boolean> {
    submitting.value = true
    try {
      const res = await shiftApi.resolveCash(
        orderId,
        {
          payType: 'CASH',
          amountFen: Math.round(amountFen),
          cashReceivedFen: typeof cashReceivedFen === 'number' ? Math.round(cashReceivedFen) : undefined,
        },
        buildIdempotencyKey('cash', orderId),
      )
      releaseIdempotencyKey('cash', orderId)
      await refreshAll()
      return res?.replayed === true
    } finally {
      submitting.value = false
    }
  }

  /** 扫码下单：只建单，不改任何账目（严禁乐观更新） */
  async function createScanPayment(orderId: string): Promise<ScanPayment> {
    return await shiftApi.createScanPayment(orderId, buildIdempotencyKey('scan', orderId))
  }

  /** 轮询到支付成功后才允许刷新账目（由 useScanPaymentPolling 调用） */
  async function onScanPaid(): Promise<void> {
    releaseIdempotencyKey('scan', '')
    await refreshAll()
  }

  function reset(): void {
    stats.value = null
    statsForbidden.value = false
    statsUpdatedAt.value = 0
    pending.value = []
    processed.value = []
    tab.value = 'pending'
    lastError.value = ''
    submitting.value = false
  }

  return {
    stats, statsUpdatedAt, statsStale, terminalId,
    pending, processed, tab, currentList,
    loadingList, loadingStats, lastError, submitting, statsState, statsForbidden,
    pendingExceptionCount, hasPendingException, canEndShift,
    handoverCash, drawerExpected, cashIn, cashRefund, openingCash,
    setTerminal, applyStats, refreshStats, loadExceptions, refreshAll,
    voidOrder, resolveCash, createScanPayment, onScanPaid, reset
  }
})

/** 幂等键：同一订单同一动作只生成一次，重试复用 */
const keyCache = new Map<string, string>()

export function buildIdempotencyKey(action: string, orderId: string): string {
  const cacheKey = action + ':' + orderId
  const cached = keyCache.get(cacheKey)
  if (cached) return cached
  const key = [action, orderId, Date.now().toString(36), Math.random().toString(36).slice(2, 8)].join('-')
  keyCache.set(cacheKey, key)
  return key
}

/** 一次写操作成功后清掉幂等键，下一单重新生成 */
export function releaseIdempotencyKey(action: string, orderId: string): void {
  keyCache.delete(action + ':' + orderId)
}
