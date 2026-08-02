/**
 * 顾客显示屏状态管理
 * 通过 localStorage + storage 事件实现跨标签页实时同步
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface DisplayCartItem {
  id: string | number
  name: string
  price: number
  quantity: number
}

export interface DisplayOrderData {
  orderNumber: string
  items: DisplayCartItem[]
  totalAmount: number
}

const STORAGE_KEY = 'pos_customer_display'

function readFromStorage(): DisplayOrderData | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as DisplayOrderData
  } catch { return null }
}

function writeToStorage(data: DisplayOrderData | null): void {
  try {
    if (data) localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {}
}

export const useCustomerDisplayStore = defineStore('customerDisplay', () => {
  const currentOrder = ref<DisplayOrderData | null>(readFromStorage())
  const isProcessing = ref(false)
  const storeName = ref('快餐收银系统')
  const lastUpdateTime = ref(0)

  const totalQuantity = computed<number>(() => {
    if (!currentOrder.value?.items) return 0
    return currentOrder.value.items.reduce((sum, item) => sum + item.quantity, 0)
  })

  const hasOrder = computed(() => !!currentOrder.value && currentOrder.value.items.length > 0)

  function updateOrder(orderData: {
    orderNumber: string
    items: DisplayCartItem[]
    totalAmount: number
  }): void {
    currentOrder.value = { ...orderData }
    isProcessing.value = true
    lastUpdateTime.value = Date.now()
    writeToStorage(currentOrder.value)
  }

  function clearOrder(): void {
    currentOrder.value = null
    isProcessing.value = false
    lastUpdateTime.value = Date.now()
    writeToStorage(null)
  }

  function setProcessing(processing: boolean): void {
    isProcessing.value = processing
  }

  function setStoreName(name: string): void {
    storeName.value = name
  }

  function initStorageListener(): () => void {
    function handler(e: StorageEvent): void {
      if (e.key !== STORAGE_KEY) return
      if (e.newValue) {
        try { currentOrder.value = JSON.parse(e.newValue) as DisplayOrderData; isProcessing.value = true }
        catch { /* ignore */ }
      } else {
        currentOrder.value = null
        isProcessing.value = false
      }
      lastUpdateTime.value = Date.now()
    }
    window.addEventListener('storage', handler)
    return () => window.removeEventListener('storage', handler)
  }

  return {
    currentOrder,
    isProcessing,
    storeName,
    lastUpdateTime,
    totalQuantity,
    hasOrder,
    updateOrder,
    clearOrder,
    setProcessing,
    setStoreName,
    initStorageListener
  }
})
