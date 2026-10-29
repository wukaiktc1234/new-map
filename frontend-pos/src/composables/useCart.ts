import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import logger from '@/utils/logger'

export interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
  dishType: string
  isHot?: boolean
  swipeX?: number
  /** 库存上限（有值时购物车「+」不允许超过） */
  stock?: number
}

const MAX_QUANTITY = 999
const SWIPE_THRESHOLD = 60
const DELETE_WIDTH = 80
/** 删除类操作的撤销窗口（秒）：收银高峰不能用模态确认打断节奏，改用可撤销 */
const UNDO_SECONDS = 5

export function useCart() {
  const cart = ref<CartItem[]>([])
  const editingIndex = ref<number | null>(null)
  const editingQuantity = ref(1)
  const qtyInputRef = ref<HTMLInputElement | null>(null)
  const currentOrderNumber = ref('')

  const swipeState = ref({
    startX: 0,
    startY: 0,
    currentIndex: null as number | null,
    isSwiping: false
  })

  const cartItemRefs = ref<Record<number, HTMLElement>>({})

  const totalQuantity = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0))
  const originalTotalAmount = computed(() => cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0))
  const isTouchDevice = computed(() => {
    const hasTouchEvents = 'ontouchstart' in window
    const hasTouchPoints = navigator.maxTouchPoints > 0
    return hasTouchEvents && hasTouchPoints
  })

  /**
   * 商品不可售判定（前端防呆第一道闸，后端仍会再拦）。
   * 兼容后端字段差异：stock / soldOut / available / onSale / status / saleStatus。
   */
  const UNAVAILABLE_STATUS = ['0', 'soldout', 'sold_out', 'out_of_stock', 'off', 'offline', 'stopsale', 'stop', 'disabled', 'unavailable']

  const unavailableReason = (item: any): string => {
    if (!item) return '商品信息异常'
    if (item.soldOut === true) return '已售罄'
    if (item.available === false || item.onSale === false) return '已下架'
    if (typeof item.stock === 'number' && item.stock <= 0) return '已售罄'
    const rawStatus = item.saleStatus ?? item.status
    if (rawStatus !== undefined && rawStatus !== null && rawStatus !== '') {
      if (UNAVAILABLE_STATUS.includes(String(rawStatus).toLowerCase())) return '已下架'
    }
    return ''
  }

  const addToCart = (item: any) => {
    const blocked = unavailableReason(item)
    if (blocked) {
      ElMessage.warning(`${item?.name || '该商品'} ${blocked}，无法添加`)
      return
    }

    if (!item.price || item.price <= 0) {
      ElMessage.warning(`${item.name} 价格异常，无法添加`)
      return
    }

    const existing = cart.value.find(i => i.id === item.id)
    if (existing) {
      if (existing.quantity >= MAX_QUANTITY) {
        ElMessage.warning(`${item.name} 数量已达上限`)
        return
      }
      // 库存上限保护：有明确库存数字时不允许超过
      if (typeof item.stock === 'number' && item.stock > 0 && existing.quantity >= item.stock) {
        ElMessage.warning(`${item.name} 库存仅剩 ${item.stock} 件`)
        return
      }
      existing.quantity++
    } else {
      cart.value.push({
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: 1,
        dishType: item.dishType,
        isHot: item.isHot,
        // 记录库存上限：购物车「+」按钮据此拦截超卖
        stock: typeof item.stock === 'number' ? item.stock : undefined,
      })
    }
  }

  const increaseItem = (index: number) => {
    const line = cart.value[index]
    if (!line) return
    if (line.quantity >= MAX_QUANTITY) {
      ElMessage.warning('数量已达上限')
      return
    }
    // 购物车里的「+」同样受库存约束（否则会绕过加购时的校验把超卖数量塞进订单）
    const stock = (line as CartItem & { stock?: number }).stock
    if (typeof stock === 'number' && stock > 0 && line.quantity >= stock) {
      ElMessage.warning(`${line.name} 库存仅剩 ${stock} 件`)
      return
    }
    line.quantity++
  }

  /**
   * 待撤销的删除操作。
   * 设计取舍：删除立刻生效（不阻断节奏），但给 5 秒后悔窗口；
   * 不用 ElMessageBox 二次确认——高峰期「每删一行弹一次窗」会变成新的瓶颈。
   */
  const pendingUndo = ref<{ item: CartItem; index: number; secondsLeft: number } | null>(null)
  let undoTimer: number | null = null

  const clearPendingUndo = (): void => {
    if (undoTimer !== null) {
      clearInterval(undoTimer)
      undoTimer = null
    }
    pendingUndo.value = null
  }

  /** 删除一行并把操作放进撤销槽（同一时刻只保留最后一次删除） */
  const removeItemWithUndo = (index: number): void => {
    const item = cart.value[index]
    if (!item) return
    // 同一行连续删除时，先落定上一条（避免撤销错位）
    clearPendingUndo()
    cart.value.splice(index, 1)
    pendingUndo.value = { item: { ...item, swipeX: 0 }, index, secondsLeft: UNDO_SECONDS }
    undoTimer = window.setInterval(() => {
      const current = pendingUndo.value
      if (!current) {
        clearPendingUndo()
        return
      }
      current.secondsLeft -= 1
      if (current.secondsLeft <= 0) clearPendingUndo()
    }, 1000)
    // 不额外弹 toast：底部撤销条已经承载同样信息，再叠一个会盖住顶栏按钮
  }

  /** 撤销上一次删除：按原位置插回（位置越界时追加到末尾） */
  const undoRemove = (): void => {
    const pending = pendingUndo.value
    if (!pending) return
    const targetIndex = Math.min(Math.max(pending.index, 0), cart.value.length)
    cart.value.splice(targetIndex, 0, pending.item)
    clearPendingUndo()
    ElMessage.success('已撤销删除')
  }

  const decreaseItem = (index: number) => {
    const item = cart.value[index]
    if (!item) return
    if (item.quantity > 1) {
      item.quantity--
    } else {
      // 减到 0 等价于删除——但必须可撤销（旧实现是静默 splice，收银员一点就丢行）
      removeItemWithUndo(index)
    }
  }

  const startEditQuantity = (index: number, quantity: number) => {
    editingIndex.value = index
    editingQuantity.value = quantity
    nextTick(() => {
      const input = qtyInputRef.value
      if (input) {
        input.focus()
        input.select()
      }
    })
  }

  const confirmEditQuantity = (index: number) => {
    if (editingIndex.value === null) return

    const newQty = Math.min(MAX_QUANTITY, Math.max(1, Math.floor(editingQuantity.value) || 1))
    cart.value[index].quantity = newQty
    editingIndex.value = null
  }

  const cancelEditQuantity = () => {
    editingIndex.value = null
  }

  const onSwipeStart = (event: TouchEvent, index: number) => {
    const touch = event.touches[0]
    swipeState.value = {
      startX: touch.clientX,
      startY: touch.clientY,
      currentIndex: index,
      isSwiping: false
    }
  }

  const onSwipeMove = (event: TouchEvent, index: number) => {
    if (swipeState.value.currentIndex !== index) return

    const touch = event.touches[0]
    const deltaX = touch.clientX - swipeState.value.startX
    const deltaY = touch.clientY - swipeState.value.startY

    if (Math.abs(deltaY) > Math.abs(deltaX)) {
      return
    }

    if (Math.abs(deltaX) > 10) {
      event.preventDefault()
      swipeState.value.isSwiping = true
    }

    let newX = (cart.value[index].swipeX || 0) + deltaX * 0.5
    newX = Math.min(0, Math.max(-DELETE_WIDTH, newX))
    cart.value[index].swipeX = newX

    swipeState.value.startX = touch.clientX
  }

  const onSwipeEnd = (_event: TouchEvent, index: number) => {
    if (!swipeState.value.isSwiping) return

    const item = cart.value[index]
    const swipeX = item.swipeX || 0

    if (swipeX < -SWIPE_THRESHOLD) {
      item.swipeX = -DELETE_WIDTH
    } else {
      item.swipeX = 0
    }

    swipeState.value.isSwiping = false

    cart.value.forEach((item, i) => {
      if (i !== index && item.swipeX) {
        item.swipeX = 0
      }
    })
  }

  const bindSwipeEvents = () => {
    if (!isTouchDevice.value) return

    Object.keys(cartItemRefs.value).forEach((key) => {
      const index = parseInt(key)
      const el = cartItemRefs.value[index]
      if (!el || el.dataset.swipeBound) return

      el.dataset.swipeBound = 'true'

      const swipeEl = el.querySelector('.cart-item-swipe') as HTMLElement
      if (!swipeEl) return

      swipeEl.addEventListener('touchstart', (e: TouchEvent) => {
        onSwipeStart(e, index)
      }, { passive: true })

      swipeEl.addEventListener('touchmove', (e: TouchEvent) => {
        onSwipeMove(e, index)
      }, { passive: false })

      swipeEl.addEventListener('touchend', (e: TouchEvent) => {
        onSwipeEnd(e, index)
      }, { passive: true })
    })
  }

  const swipeDeleteItem = (index: number) => {
    // 垃圾桶删除 / 触摸左滑删除共用：同样进 5 秒撤销槽
    removeItemWithUndo(index)
  }

  const clearCart = () => {
    cart.value = []
    currentOrderNumber.value = ''
    clearPendingUndo()
    localStorage.removeItem('pos-cart')
    localStorage.removeItem('pos-current-order')
  }

  const initFromStorage = () => {
    try {
      const savedCart = localStorage.getItem('pos-cart')
      if (savedCart) {
        const parsedCart = JSON.parse(savedCart) as CartItem[]
        if (Array.isArray(parsedCart)) {
          cart.value = parsedCart
        }
      }
      const savedOrderNumber = localStorage.getItem('pos-current-order')
      if (savedOrderNumber) {
        currentOrderNumber.value = savedOrderNumber
      }
    } catch (e) {
      logger.errorMessage('从localStorage恢复购物车数据失败:', e)
    }
  }

  watch(cart, (newCart) => {
    localStorage.setItem('pos-cart', JSON.stringify(newCart))
    if (isTouchDevice.value) {
      nextTick(() => bindSwipeEvents())
    }
  }, { deep: true })

  watch(currentOrderNumber, (newOrderNumber) => {
    if (newOrderNumber) {
      localStorage.setItem('pos-current-order', newOrderNumber)
    } else {
      localStorage.removeItem('pos-current-order')
    }
  })

  return {
    cart,
    totalQuantity,
    originalTotalAmount,
    currentOrderNumber,
    editingIndex,
    editingQuantity,
    qtyInputRef,
    isTouchDevice,
    cartItemRefs,
    swipeState,
    addToCart,
    increaseItem,
    decreaseItem,
    startEditQuantity,
    confirmEditQuantity,
    cancelEditQuantity,
    onSwipeStart,
    onSwipeMove,
    onSwipeEnd,
    bindSwipeEvents,
    swipeDeleteItem,
    pendingUndo,
    undoRemove,
    clearPendingUndo,
    clearCart,
    initFromStorage
  }
}
