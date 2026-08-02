/**
 * 挂单状态管理
 * 管理挂起订单的列表状态和操作
 *
 * 说明：后端未提供挂单端点，挂单数据完全存储于本地 localStorage
 * 适用于单 POS 终端场景；多终端挂单需后端补建 /v1/pos/order/hang 等端点
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

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

const STORAGE_KEY = 'heldOrders'

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
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        const parsed = JSON.parse(saved)
        if (Array.isArray(parsed)) {
          heldOrders.value = parsed.filter(isValidHeldOrder)
          if (heldOrders.value.length < parsed.length) {
            saveToStorage()
          }
        }
      }
    } catch (e) {
      heldOrders.value = []
    }
  }

  /**
   * 保存挂单列表到本地存储
   */
  const saveToStorage = (): void => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(heldOrders.value))
    } catch (e) {
      console.error('保存挂单数据失败:', e)
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
        holdTime: new Date().toLocaleString('zh-CN'),
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
    localStorage.removeItem(STORAGE_KEY)
  }

  /**
   * 初始化：从本地存储加载数据
   */
  const initialize = (): void => {
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
