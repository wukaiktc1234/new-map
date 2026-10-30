/**
 * 挂单状态管理
 * 管理挂起订单的列表状态和操作
 *
 * 说明：后端未提供挂单端点，挂单数据完全存储于本地 localStorage
 * 适用于单 POS 终端场景；多终端挂单需后端补建 /v1/pos/order/hang 等端点
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { formatDateTime } from '@/utils/format'
import logger from '@/utils/logger'

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
  id: string
  items: CartItem[]
  totalAmount: number
  holdTime: string
  remark?: string
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
      // 后端未实现挂单 API，直接走本地存储
      const heldOrder: HeldOrder = {
        id: generateUniqueId(),
        items: JSON.parse(JSON.stringify(items)),
        totalAmount,
        holdTime: formatDateTime(new Date()),
        remark
      }

      heldOrders.value.unshift(heldOrder)
      saveToStorage()

      return true
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

      // 从列表中移除
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
    if (index !== -1) {
      heldOrders.value.splice(index, 1)
      saveToStorage()
    }
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
  const initialize = (): void => {
    // 顺序很重要：先清掉非当前账号的键（含老的全局键），再加载本账号的
    purgeForeignHoldKeys()
    loadFromStorage()
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
    initialize
  }
})
