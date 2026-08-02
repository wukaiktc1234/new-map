import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

export interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
  dishType: string
  isHot?: boolean
  swipeX?: number
}

const MAX_QUANTITY = 999
const SWIPE_THRESHOLD = 60
const DELETE_WIDTH = 80

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

  const addToCart = (item: any) => {
    if (item.stock === 0) {
      ElMessage.warning(`${item.name} 已售罄，无法添加`)
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
      existing.quantity++
    } else {
      cart.value.push({
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: 1,
        dishType: item.dishType,
        isHot: item.isHot
      })
    }
  }

  const increaseItem = (index: number) => {
    if (cart.value[index].quantity >= MAX_QUANTITY) {
      ElMessage.warning('数量已达上限')
      return
    }
    cart.value[index].quantity++
  }

  const decreaseItem = (index: number) => {
    if (cart.value[index].quantity > 1) {
      cart.value[index].quantity--
    } else {
      cart.value.splice(index, 1)
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
    cart.value.splice(index, 1)
  }

  const clearCart = () => {
    cart.value = []
    currentOrderNumber.value = ''
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
      console.error('从localStorage恢复购物车数据失败:', e)
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
    clearCart,
    initFromStorage
  }
}
