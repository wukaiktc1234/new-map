/**
 * 挂单状态管理
 * 管理挂起订单的列表状态和操作
 *
 * 说明：后端未提供挂单端点，挂单数据完全存储于本地 localStorage
 * 适用于单 POS 终端场景；多终端挂单需后端补建 /v1/pos/order/hang 等端点
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '@/utils/format'
import logger from '@/utils/logger'
import { holdsApi, parseCartData } from '@/api/holds'
import { fenToYuan } from '@/utils/shiftStatus'

/** 购物车商品项 */
export interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
  dishType: string
  isHot?: boolean
}

/** 挂单信息 */
export interface HeldOrder {
  /** 后端 id（离线暂存时是本地临时 id，pendingSync=true） */
  id: string
  items: CartItem[]
  /** 金额（**元**，展示用；提交后端时由 holdsApi 转分） */
  totalAmount: number
  holdTime: string
  remark?: string
  /** 后端挂单号（HOLDxxxx），离线暂存时为空 */
  orderNo?: string
  /** 后端过期时间 expireAt */
  expireAt?: string
  /** true = 仅存在本地、尚未同步到后端（弱网暂存） */
  pendingSync?: boolean
  /** 后端专用：cartData 原样（离线重放时回传） */
  cartData?: Record<string, unknown>
}

/**
 * 存储键（P0-1 越权修复）
 *
 * 历史实现用的是**全局键** 'heldOrders' —— localStorage 是浏览器级存储、不区分登录账号，
 * 于是「上一个收银员的挂单」会被下一个账号读到（Owner 报的「挂单越权」就是这个）。
 * 现在键里带 userId + terminalId：切换账号后读到的是**另一个键**，天然隔离。
 */
const LEGACY_STORAGE_KEY = 'heldOrders'
const STORAGE_KEY_PREFIX = 'heldOrders:'

/** 当前登录身份（pos-user 里的 employeeId / username + pos-terminal） */
function currentIdentity(): { userId: string; terminalId: string } {
  let userId = ''
  try {
    const rawUser = localStorage.getItem('pos-user')
    if (rawUser) {
      const user = JSON.parse(rawUser) as { employeeId?: string | number; username?: string } | null
      const id = user?.employeeId ?? user?.username ?? ''
      userId = String(id || '').trim()
    }
  } catch {
    userId = ''
  }
  const terminalId = (localStorage.getItem('pos-terminal') || '').trim()
  return { userId: userId || 'anon', terminalId: terminalId || 'noterm' }
}

/** 当前账号的存储键 */
function storageKey(): string {
  const { userId, terminalId } = currentIdentity()
  return STORAGE_KEY_PREFIX + userId + ':' + terminalId
}

/**
 * 清理其它账号 / 历史全局键（P0-1）
 * - 'heldOrders'（老的全局键）：内容来源不明，直接丢弃（这正是越权来源）
 * - 'heldOrders:<其它账号>:<任意终端>'：不属于当前登录身份，删掉，避免越攒越多
 */
function purgeForeignHoldKeys(): void {
  try {
    const current = storageKey()
    const drop: string[] = []
    for (let i = 0; i < localStorage.length; i += 1) {
      const key = localStorage.key(i)
      if (!key) continue
      if (key === LEGACY_STORAGE_KEY) { drop.push(key); continue }
      if (key.startsWith(STORAGE_KEY_PREFIX) && key !== current) drop.push(key)
    }
    drop.forEach((key) => localStorage.removeItem(key))
    if (drop.length > 0) {
      logger.logMessage('[HOLD-PURGE] 已清理非当前账号的挂单缓存 count=' + drop.length + ' keys=' + drop.join(','))
    }
  } catch (e) {
    logger.warnMessage('[HOLD-PURGE] 清理挂单缓存失败:', e)
  }
}

export const useHangingOrderStore = defineStore('hangingOrder', () => {
  // 状态定义
  const heldOrders = ref<HeldOrder[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  /** 挂单数量 */
  const heldCount = computed(() => heldOrders.value.length)

  /** 挂单总金额 */
  const heldTotalAmount = computed(() =>
    heldOrders.value.reduce((sum, order) => sum + order.totalAmount, 0)
  )

  /**
   * 校验挂单数据结构是否合法
   */
  function isValidHeldOrder(order: unknown): order is HeldOrder {
    if (!order || typeof order !== 'object') return false
    const o = order as Record<string, unknown>
    return (
      typeof o.id === 'string' &&
      Array.isArray(o.items) &&
      typeof o.totalAmount === 'number' &&
      typeof o.holdTime === 'string'
    )
  }

  /**
   * 取 holdTime 的业务日期（YYYY-MM-DD）
   * 兼容 'YYYY-MM-DD HH:mm:ss' 与 ISO 'YYYY-MM-DDTHH:mm:ss'；解析失败返回空串（视为过期）
   */
  function businessDateOf(holdTime: string): string {
    if (typeof holdTime !== 'string' || !holdTime.trim()) return ''
    const m = holdTime.trim().match(/^(\d{4}-\d{2}-\d{2})/)
    return m ? m[1] : ''
  }

  /** 今天的业务日期（本地时区，与收银员看到的日期一致） */
  function todayBusinessDate(): string {
    const now = new Date()
    const y = now.getFullYear()
    const mo = String(now.getMonth() + 1).padStart(2, '0')
    const d = String(now.getDate()).padStart(2, '0')
    return y + '-' + mo + '-' + d
  }

  /**
   * 挂单是否仍可用（P0-2 跨天失效 + P0-3 结构校验合并）
   * Owner 要求「凌晨强制失效」：holdTime 的业务日期不是今天就丢弃，避免挂单越攒越久。
   */
  function isUsableHeldOrder(order: unknown): order is HeldOrder {
    if (!isValidHeldOrder(order)) return false
    const date = businessDateOf(order.holdTime)
    if (!date) return false
    return date === todayBusinessDate()
  }

  /** 当前 terminalId（后端 GET/POST 必填） */
  function currentTerminalId(): string {
    return (localStorage.getItem('pos-terminal') || '').trim()
  }

  /** 当前门店 id（后端可选参数） */
  function currentStoreId(): number | undefined {
    try {
      const user = JSON.parse(localStorage.getItem('pos-user') || '{}') as { storeId?: number | string }
      const n = Number(user?.storeId)
      return Number.isFinite(n) && n > 0 ? n : undefined
    } catch {
      return undefined
    }
  }

  /**
   * 是否「网络类失败」（超时/断网/5xx 之外的无响应）
   * 只有这种情况才降级到本地暂存；业务错误（409/400 等）必须抛给用户，不能静默存本地。
   */
  function isNetworkFailure(err: unknown): boolean {
    if (!err || typeof err !== 'object') return true
    const e = err as { status?: unknown; code?: unknown; originalError?: { code?: string } }
    const status = Number(e.status ?? e.code)
    if (Number.isFinite(status) && status >= 400) return false
    const netCode = String(e.originalError?.code || '')
    if (netCode === 'ERR_NETWORK' || netCode === 'ECONNABORTED') return true
    // 完全拿不到 status → 视为网络问题（离线/超时）
    return !Number.isFinite(status)
  }

  /** 后端挂单 → 前端模型（分→元） */
  function toHeldOrder(raw: {
    id: number | string
    orderNo?: string
    cartData?: unknown
    totalAmountFen?: number
    createTime?: string
    expireAt?: string
  }): HeldOrder {
    const cart = parseCartData(raw.cartData as never)
    const items = Array.isArray((cart as { items?: unknown }).items)
      ? ((cart as { items: CartItem[] }).items)
      : []
    return {
      id: String(raw.id),
      items,
      totalAmount: fenToYuan(Number(raw.totalAmountFen ?? 0)),
      holdTime: raw.createTime || formatDateTime(new Date()),
      orderNo: raw.orderNo,
      expireAt: raw.expireAt,
      pendingSync: false,
      cartData: cart,
    }
  }

  /** 只在本地保存一份（弱网暂存），带 pendingSync 标记 */
  function saveLocalPending(order: HeldOrder): void {
    const next = heldOrders.value.filter((o) => o.id !== order.id)
    next.unshift({ ...order, pendingSync: true })
    heldOrders.value = next
    saveToStorage()
  }

  /** 从本地暂存里删掉某条（同步成功后调用） */
  function dropLocal(orderId: string): void {
    const next = heldOrders.value.filter((o) => o.id !== orderId)
    heldOrders.value = next
    saveToStorage()
  }

  /**
   * 把本地 pendingSync 的挂单补传到后端（弱网恢复后）
   * 成功 → 从本地删除；失败且非网络错误 → 也删除（业务错误说明后端已不接受，留着没意义）并记日志
   */
  async function syncPendingOrders(): Promise<{ synced: number; failed: number }> {
    const pending = heldOrders.value.filter((o) => o.pendingSync)
    if (pending.length === 0) return { synced: 0, failed: 0 }

    const terminalId = currentTerminalId()
    if (!terminalId) return { synced: 0, failed: 0 }

    let synced = 0
    let failed = 0
    for (const order of pending) {
      try {
        const res = await holdsApi.create({
          cartData: order.cartData ?? { items: order.items },
          totalAmountYuan: order.totalAmount,
          terminalId,
          storeId: currentStoreId(),
          clientHoldId: order.id,
        })
        dropLocal(order.id)
        synced += 1
        logger.logMessage('[HOLD-SYNC] 本地挂单已补传后端 id=' + order.id + ' → orderNo=' + res?.orderNo)
      } catch (err) {
        failed += 1
        if (!isNetworkFailure(err)) {
          dropLocal(order.id)
          logger.warnMessage('[HOLD-SYNC] 补传被后端拒绝，已丢弃本地副本 id=' + order.id, err)
        } else {
          logger.warnMessage('[HOLD-SYNC] 仍是网络问题，保留本地暂存 id=' + order.id, err)
        }
      }
    }
    return { synced, failed }
  }

  /**
   * 409（CAS 冲突：已被其它终端处理 / 已失效）统一处理：
   * 明确提示 + 立刻重新拉一次后端列表，避免用户对着旧数据反复点。
   * @returns true = 已处理完（调用方直接 return）
   */
  function handleCasConflict(err: unknown, actionLabel: string): boolean {
    const status = Number((err as { status?: unknown })?.status ?? (err as { code?: unknown })?.code)
    if (status !== 409) return false
    ElMessage.warning('该挂单已被其它终端处理或已失效，请刷新列表')
    logger.warnMessage('[HOLD-409] ' + actionLabel + ' 失败：挂单已被处理，已触发刷新', err)
    void fetchHeldOrders()
    return true
  }

  /**
   * 生成唯一ID
   */
  function generateUniqueId(): string {
    return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
  }

  /**
   * 从本地存储加载挂单列表
   */
  const loadFromStorage = (): void => {
    try {
      const key = storageKey()
      const saved = localStorage.getItem(key)
      if (!saved) {
        heldOrders.value = []
        return
      }
      const parsed = JSON.parse(saved)
      if (!Array.isArray(parsed)) {
        heldOrders.value = []
        return
      }
      const usable = parsed.filter(isUsableHeldOrder)
      const expiredCount = parsed.filter(isValidHeldOrder).length - usable.length
      heldOrders.value = usable
      if (expiredCount > 0) {
        // Owner 要求的可观测日志：跨天/无法解析的挂单被丢弃了多少
        logger.warnMessage('[HOLD-EXPIRED] 已丢弃跨天或非法的挂单 count=' + expiredCount + ' key=' + key)
      }
      if (usable.length < parsed.length) {
        saveToStorage()
      }
    } catch (e) {
      heldOrders.value = []
      logger.warnMessage('[HOLD-LOAD] 读取挂单缓存失败，已按空列表处理:', e)
    }
  }

  /**
   * 保存挂单列表到本地存储
   */
  const saveToStorage = (): void => {
    try {
      localStorage.setItem(storageKey(), JSON.stringify(heldOrders.value))
    } catch (e) {
      logger.errorMessage('保存挂单数据失败:', e)
    }
  }

  /**
   * 挂单操作（本地存储）
   * @param items - 当前购物车商品
   * @param totalAmount - 订单总金额
   * @param remark - 备注信息（可选）
   * @returns 挂单是否成功
   */
  const hangOrder = async (
    items: CartItem[],
    totalAmount: number,
    remark?: string
  ): Promise<boolean> => {
    if (items.length === 0) {
      error.value = '购物车为空，无法挂单'
      return false
    }

    loading.value = true
    error.value = null

    try {
      const terminalId = currentTerminalId()
      if (!terminalId) {
        error.value = '终端标识缺失，无法挂单：请退出登录后重新登录本机'
        return false
      }

      const clientHoldId = generateUniqueId()
      const cartData: Record<string, unknown> = { items: JSON.parse(JSON.stringify(items)), remark: remark ?? null }

      try {
        // 主路径：后端挂单（金额元→分由 holdsApi 内部换算）
        const res = await holdsApi.create({
          cartData,
          totalAmountYuan: totalAmount,
          terminalId,
          storeId: currentStoreId(),
          clientHoldId,
        })
        heldOrders.value.unshift({
          id: String(res?.id ?? clientHoldId),
          items: JSON.parse(JSON.stringify(items)),
          totalAmount,
          holdTime: res?.createTime || formatDateTime(new Date()),
          remark,
          orderNo: res?.orderNo,
          expireAt: res?.expireAt,
          pendingSync: false,
          cartData,
        })
        saveToStorage()
        return true
      } catch (err) {
        // 弱网降级：只有网络类失败才落本地暂存（业务错误必须让用户看到）
        if (!isNetworkFailure(err)) {
          error.value = (err as { message?: string })?.message || '挂单失败'
          throw err
        }
        const localOrder: HeldOrder = {
          id: clientHoldId,
          items: JSON.parse(JSON.stringify(items)),
          totalAmount,
          holdTime: formatDateTime(new Date()),
          remark,
          pendingSync: true,
          cartData,
        }
        saveLocalPending(localOrder)
        logger.warnMessage('[HOLD-OFFLINE] 网络不可用，已本地暂存（pendingSync=true）id=' + clientHoldId)
        ElMessage.warning('网络异常，挂单已暂存本地，恢复网络后会自动补传')
        return true
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取挂单列表（从本地存储）
   */
  const fetchHeldOrders = async (): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      // 任何读取入口都先清掉非当前账号的键：避免老全局键/别的账号键被读到（P0-1）
      purgeForeignHoldKeys()
      loadFromStorage()

      const terminalId = currentTerminalId()
      if (!terminalId) {
        error.value = '终端标识缺失，无法读取挂单：请退出登录后重新登录本机'
        return
      }

      // 先把上次弱网暂存的补传上去（成功后本地副本会被删除）
      await syncPendingOrders()

      try {
        const res = await holdsApi.list(terminalId)
        const remote = res.list.map(toHeldOrder)
        // 远端为准 + 仍然没同步成功的本地暂存（pendingSync）继续显示
        const stillPending = heldOrders.value.filter((o) => o.pendingSync)
        heldOrders.value = [...stillPending, ...remote]
        saveToStorage()
        logger.logMessage('[HOLD-LIST] 后端返回 ' + remote.length + ' 条（terminalId=' + terminalId + '），本地暂存 ' + stillPending.length + ' 条')
      } catch (err) {
        if (!isNetworkFailure(err)) throw err
        // 弱网：退回本地（已过滤跨天），并提示当前是离线视图
        logger.warnMessage('[HOLD-LIST] 后端不可用，展示本地暂存（离线视图）', err)
        ElMessage.warning('网络异常，挂单列表为本地暂存视图')
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 恢复挂单（本地操作）
   * @param orderId - 要恢复的挂单ID
   * @returns 恢复的购物车商品，如果失败返回null
   */
  const resumeOrder = async (orderId: string): Promise<CartItem[] | null> => {
    loading.value = true
    error.value = null

    try {
      const orderIndex = heldOrders.value.findIndex(o => o.id === orderId)
      if (orderIndex === -1) {
        error.value = '未找到该挂单'
        return null
      }

      const order = heldOrders.value[orderIndex]
      const cartItems = JSON.parse(JSON.stringify(order.items))

      // 离线暂存的单还没进后端：直接本地取出
      if (order.pendingSync) {
        heldOrders.value.splice(orderIndex, 1)
        saveToStorage()
        return cartItems
      }

      try {
        await holdsApi.checkout(order.id, currentTerminalId())
      } catch (err) {
        if (handleCasConflict(err, '取出')) return null
        throw err
      }

      heldOrders.value.splice(orderIndex, 1)
      saveToStorage()

      return cartItems
    } finally {
      loading.value = false
    }
  }

  /**
   * 删除单个挂单（本地操作）
   * @param orderId - 要删除的挂单ID
   */
  const removeOrder = async (orderId: string): Promise<void> => {
    const index = heldOrders.value.findIndex(o => o.id === orderId)
    if (index === -1) return

    const order = heldOrders.value[index]
    // 离线暂存单还没进后端，直接删本地
    if (order.pendingSync) {
      heldOrders.value.splice(index, 1)
      saveToStorage()
      return
    }

    try {
      await holdsApi.cancel(order.id, currentTerminalId())
    } catch (err) {
      if (handleCasConflict(err, '取消')) return
      throw err
    }

    heldOrders.value.splice(index, 1)
    saveToStorage()
  }

  /**
   * 清空所有挂单（本地操作）
   */
  const clearAllOrders = async (): Promise<void> => {
    heldOrders.value = []
    localStorage.removeItem(storageKey())
  }

  /**
   * 初始化：从本地存储加载数据
   */
  const initialize = async (): Promise<void> => {
    // 顺序很重要：先清掉非当前账号的键（含老的全局键），再加载本账号的
    purgeForeignHoldKeys()
    loadFromStorage()
    // 拉后端列表（同时会把上次弱网暂存补传上去）；失败不抛，避免阻断启动
    try {
      await fetchHeldOrders()
    } catch (err) {
      logger.warnMessage('[HOLD-INIT] 初始化拉取挂单失败（已退回本地视图）:', err)
    }
  }

  return {
    // 状态
    heldOrders,
    loading,
    error,

    // 计算属性
    heldCount,
    heldTotalAmount,

    // 方法
    hangOrder,
    fetchHeldOrders,
    resumeOrder,
    removeOrder,
    clearAllOrders,
    initialize,
    /** 弱网恢复后手动补传（可选调用；fetchHeldOrders / initialize 也会自动补传） */
    syncPendingOrders
  }
})
