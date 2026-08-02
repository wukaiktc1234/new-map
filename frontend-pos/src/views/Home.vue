<template>
  <div class="fastfood-pos" :data-theme="isDark ? 'dark' : 'light'">
    <PosTopBar
      :current-time="currentTime"
      :held-count="hangingOrderStore.heldCount"
      :call-number-pending-count="callNumberPendingCount"
      :is-dark="isDark"
      @hold="showHoldOrderDialog"
      @retrieve="showRetrieveOrderDialog"
      @order-query="showOrderQueryDialog"
      @coupon="showCouponDialog"
      @call-number="goToCallNumber"
      @tables="goToTables"
      @yolo="showYoloCheckoutDialog"
      @shift-handover="showShiftHandoverDialog"
      @toggle-theme="toggleTheme"
    />

    <div class="main-content">
      <!-- 购物车置于左侧，菜品网格在右侧 -->
      <CartSection
        :cart="cart"
        :current-order-number="currentOrderNumber"
        :total-quantity="totalQuantity"
        :original-total-amount="originalTotalAmount"
        :total-amount="totalAmount"
        :discount-amount="discountAmount"
        :applied-coupon="appliedCoupon"
        @increase="increaseItem"
        @decrease="decreaseItem"
        @delete="swipeDeleteItem"
        @clear="clearCart"
        @edit-qty="(idx) => startEditQuantity(idx, cart[idx].quantity)"
        @confirm-edit="confirmEditQuantity"
        @cancel-edit="cancelEditQuantity"
        @payment="quickPayment"
      />

      <MenuSection
        :categories="categories"
        :items="filteredItems"
        :active-category="activeCategory"
        :loading="loading"
        :load-error="loadError"
        @select-category="activeCategory = $event"
        @add-to-cart="addToCart"
        @retry="loadMenu"
      />
    </div>

    <!-- 支付确认对话框 -->
    <PaymentConfirmDialog
      v-model="showPaymentConfirm"
      :total-amount="totalAmount"
      :selected-payment-method="selectedPaymentMethod"
      @confirm="handlePaymentConfirm"
    />

    <!-- 支付成功对话框 -->
    <PaymentSuccessDialog
      v-model="showPaymentSuccess"
      :order-number="lastOrderNumber"
      :pickup-number="lastPickupNumber"
      @complete="showPaymentSuccess = false"
    />

    <!-- 挂单对话框 -->
    <HoldOrderDialog
      v-model="showHoldOrder"
      :cart="cart"
      :total-amount="totalAmount"
      :is-submitting="isSubmitting"
      @confirm="confirmHoldOrderWithRemark"
    />

    <!-- 取单对话框 -->
    <RetrieveOrderDialog
      v-model="showRetrieveOrder"
      @select="selectHeldOrder"
      @clear="clearHeldOrders"
    />

    <!-- 订单查询对话框 -->
    <OrderQueryDialog
      v-model="showOrderQuery"
      @view-detail="viewOrderDetail"
      @refund="refundOrder"
    />

    <!-- 券码核销对话框 -->
    <CouponDialog
      v-model="showCoupon"
      :applied-coupon="appliedCoupon"
      :original-total-amount="originalTotalAmount"
      @apply="applyCouponFromDialog"
      @remove="removeCoupon"
    />

    <!-- 订单详情对话框 -->
    <OrderDetailDialog
      v-model="showOrderDetail"
      :order="currentOrder"
      @refund="refundOrder"
    />

    <!-- 交班/日结对话框 -->
    <ShiftHandover
      v-model="showShiftHandover"
      @handoverCompleted="onHandoverCompleted"
    />

    <!-- 库存查询对话框 -->
    <StockQueryDialog
      v-model="showStockQuery"
      :low-stock-items="lowStockItems"
    />

    <!-- YOLO 辅助收银对话框 -->
    <YoloCheckoutDialog
      v-model="showYoloCheckout"
      @add-to-cart="handleYoloAddToCart"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { posApi } from '@/api/posApi'
import { webSocketService } from '@/utils/websocket'
import ShiftHandover from './ShiftHandover.vue'
import PaymentConfirmDialog from '@/components/pos/dialogs/PaymentConfirmDialog.vue'
import PaymentSuccessDialog from '@/components/pos/dialogs/PaymentSuccessDialog.vue'
import HoldOrderDialog from '@/components/pos/dialogs/HoldOrderDialog.vue'
import RetrieveOrderDialog from '@/components/pos/dialogs/RetrieveOrderDialog.vue'
import OrderQueryDialog from '@/components/pos/dialogs/OrderQueryDialog.vue'
import CouponDialog from '@/components/pos/dialogs/CouponDialog.vue'
import OrderDetailDialog from '@/components/pos/dialogs/OrderDetailDialog.vue'
import StockQueryDialog from '@/components/pos/dialogs/StockQueryDialog.vue'
import YoloCheckoutDialog from '@/components/pos/YoloCheckoutDialog.vue'
import PosTopBar from '@/components/pos/PosTopBar.vue'
import MenuSection from '@/components/pos/MenuSection.vue'
import CartSection from '@/components/pos/CartSection.vue'

// Composable 导入
import { useCart } from '@/composables/useCart'
import { usePayment } from '@/composables/usePayment'
import { useMenu } from '@/composables/useMenu'
import { useHangingOrder } from '@/composables/useHangingOrder'
import { useCustomerDisplayStore } from '@/stores/customer-display'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()

// ========== 使用Composables ==========
const {
  cart, totalQuantity, originalTotalAmount, currentOrderNumber,
  startEditQuantity, confirmEditQuantity, cancelEditQuantity,
  addToCart, increaseItem, decreaseItem,
  swipeDeleteItem, clearCart, initFromStorage
} = useCart()

const discountAmount = ref(0)
const totalAmount = computed(() => Math.max(0, originalTotalAmount.value - discountAmount.value))

const {
  showPaymentConfirm, showPaymentSuccess, selectedPaymentMethod,
  lastOrderNumber, lastPickupNumber,
  cashInputRef,
  quickPayment, confirmPayment, handlePaymentConfirm
} = usePayment(cart, totalAmount, originalTotalAmount, discountAmount, clearCart)

const {
  loading, loadError, categories, activeCategory,
  filteredItems, lowStockItems, loadMenu
} = useMenu()

const {
  hangingOrderStore, showHoldOrder, showRetrieveOrder,
  holdOrderForm, isSubmitting, showHoldOrderDialog, confirmHoldOrder,
  showRetrieveOrderDialog, selectHeldOrder, clearHeldOrders
} = useHangingOrder(cart, totalAmount as any, clearCart)

const { isDark, currentTime, toggleTheme, initTime, cleanupTime } = useTheme()
const customerDisplayStore = useCustomerDisplayStore()
const showOrderQuery = ref(false)
const showOrderDetail = ref(false)
const showCoupon = ref(false)
const showShiftHandover = ref(false)
const showStockQuery = ref(false)
const showYoloCheckout = ref(false)

const orderList = ref<any[]>([])
const queryDateRange = ref<[Date, Date] | null>(null)
const queryKeyword = ref('')
const currentOrder = ref<any>(null)
let orderQueryTimer: number | null = null

const couponForm = ref({
  code: ''
})
const couponInfo = ref<any>(null)
const appliedCoupon = ref<any>(null)
const couponInputRef = ref<any>(null)

// 叫号系统待取餐数量
const callNumberPendingCount = ref(0)
let callNumberStatsTimer: number | null = null

let wsMenuSubscription: string | null = null
let wsOrderStatusSubscription: string | null = null
let wsOrderRefundSubscription: string | null = null

const goToCallNumber = () => {
  const screenLeft = window.screenLeft || window.screenX
  const screenWidth = window.screen.width
  window.open(
    '/call-number',
    '_blank',
    `width=900,height=700,left=${screenLeft + screenWidth + 10},top=0`
  )
}

/**
 * 加载叫号系统待取餐数量
 */
const loadCallNumberStats = async () => {
  try {
    const res = await request.get('/v1/call-number/stats') as { pending?: number; called?: number; picked?: number }
    if (res && typeof res.pending === 'number') {
      callNumberPendingCount.value = res.pending
    }
  } catch (error) {
  }
}

const goToTables = () => {
  router.push('/tables')
}

/** 显示 YOLO 辅助收银对话框 */
const showYoloCheckoutDialog = () => {
  showYoloCheckout.value = true
}

/**
 * 处理 YOLO 识别结果批量加入购物车
 * @param items YOLO 识别到的商品列表
 */
const handleYoloAddToCart = (items: Array<{
  id: string
  name: string
  price: number
  quantity: number
  dishType: string
}>) => {
  items.forEach(item => {
    // 按 quantity 逐个加入（复用 useCart 的库存/上限校验）
    for (let i = 0; i < item.quantity; i++) {
      addToCart({
        id: item.id,
        name: item.name,
        price: item.price,
        dishType: item.dishType
      })
    }
  })
}

/**
 * 显示交班对话框
 */
const showShiftHandoverDialog = () => {
  showShiftHandover.value = true
}

/**
 * 处理交班完成事件
 * 交班后清除当前账号凭证并跳转登录页，确保下一班次需要重新登录
 * @param data - 交班数据
 */
const onHandoverCompleted = (data: any) => {
  // 清空购物车和当前订单
  clearCart()

  ElMessage.success(`交班成功！今日共 ${data.orderCount} 笔订单`)

  // 清除当前账号凭证，跳转登录页让下一班次重新登录
  localStorage.removeItem('pos-token')
  localStorage.removeItem('pos-user')
  setTimeout(() => {
    router.push('/login')
  }, 800)
}

const showOrderQueryDialog = () => {
  showOrderQuery.value = true
  queryOrders()
  if (orderQueryTimer) clearInterval(orderQueryTimer)
  orderQueryTimer = window.setInterval(() => {
    if (showOrderQuery.value) {
      queryOrdersSilent()
    }
  }, 30000)  // 性能优化：30秒刷新一次，减少不必要的全量拉取
}

const queryOrdersSilent = async () => {
  try {
    const params: any = {}
    if (queryKeyword.value) {
      params.keyword = queryKeyword.value
    }
    if (queryDateRange.value && queryDateRange.value.length === 2) {
      params.startDate = queryDateRange.value[0].toISOString().split('T')[0]
      params.endDate = queryDateRange.value[1].toISOString().split('T')[0]
    }
    
    const result = await request.get('/v1/pos/orders', { params }) as any

    if (Array.isArray(result)) {
      // 性能优化：用浅比较替代 JSON.stringify 深比较
      // 仅比较长度和首条订单号（按时间倒序，首条变化即有新订单）
      const oldList = orderList.value
      const changed = oldList.length !== result.length
        || (result.length > 0 && oldList.length > 0
            && oldList[0].orderNumber !== result[0].orderNumber)
      if (changed) {
        orderList.value = result
      }
    }
  } catch (error) {
    console.error('静默查询订单失败:', error)
  }
}

const queryOrders = async () => {
  try {
    const params: any = {}
    if (queryKeyword.value) {
      params.keyword = queryKeyword.value
    }
    if (queryDateRange.value && queryDateRange.value.length === 2) {
      params.startDate = queryDateRange.value[0].toISOString().split('T')[0]
      params.endDate = queryDateRange.value[1].toISOString().split('T')[0]
    }

    const result = await request.get('/v1/pos/orders', { params }) as any

    if (Array.isArray(result)) {
      orderList.value = result
    } else {
      orderList.value = []
    }
  } catch (error) {
    // 订单查询失败时显示空列表，由用户决定是否重试
    console.error('查询订单失败:', error)
    orderList.value = []
  }
}

const viewOrderDetail = async (order: any) => {
  currentOrder.value = order
  showOrderDetail.value = true

  try {
    const result = await posApi.getOrderDetail(order.orderNumber || order.orderId) as any
    if (result) {
      currentOrder.value = { ...order, ...result }
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
  }
}

const refundOrder = async (order: any) => {
  try {
    const { value: refundReason } = await ElMessageBox.prompt(
      `订单号: ${order.orderNumber}\n退款金额: ¥${order.totalAmount?.toFixed(2) || '0.00'}`,
      '退款确认',
      {
        confirmButtonText: '确认退款',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入退款原因（可选）',
        inputValue: '用户申请退款',
        type: 'warning'
      }
    ) as { value: string; action: string }

    // 调用后端退款 API
    await posApi.refundOrder(order.orderNumber || order.orderId, refundReason || '用户申请退款')

    ElMessage.success('退款成功')
    showOrderDetail.value = false
    await queryOrders()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '退款失败')
    }
  }
}

const showCouponDialog = () => {
  couponForm.value.code = ''
  couponInfo.value = null
  showCoupon.value = true
}

const applyCoupon = async () => {
  if (!couponInfo.value?.valid) {
    ElMessage.warning('请先验证有效的券码')
    return
  }

  if (couponInfo.value.minAmount && originalTotalAmount.value < couponInfo.value.minAmount) {
    ElMessage.warning(`订单金额需满 ¥${couponInfo.value.minAmount} 才能使用此优惠券`)
    return
  }

  // 调用后端核销 API（应用优惠券计算折扣）
  try {
    const result = await posApi.applyCoupon(
      couponInfo.value.code || couponForm.value.code,
      originalTotalAmount.value
    ) as unknown as {
      success: boolean
      discountAmount?: number
      finalAmount?: number
      message?: string
    }

    if (!result.success) {
      ElMessage.error(result.message || '优惠券应用失败')
      return
    }

    appliedCoupon.value = couponInfo.value
    // 以后端返回的折扣金额为准
    discountAmount.value = result.discountAmount ?? couponInfo.value.discountAmount ?? 0
    showCoupon.value = false
    ElMessage.success('优惠券应用成功！')
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '优惠券应用失败'
    ElMessage.error(errMsg)
  }
}

const removeCoupon = () => {
  appliedCoupon.value = null
  discountAmount.value = 0
  ElMessage.success('已移除优惠券')
}

const confirmHoldOrderWithRemark = (remark: string) => {
  holdOrderForm.value.remark = remark
  confirmHoldOrder()
}

const applyCouponFromDialog = (couponData: any) => {
  // 接收对话框传递的券码信息，同步到父组件状态后应用
  couponInfo.value = couponData
  applyCoupon()
}

watch(showPaymentConfirm, async (newVal) => {
  if (newVal) {
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 100))
    if (selectedPaymentMethod.value === 'cash') {
      try {
        // 使用组件引用获取内部输入元素
        const inputNumberComponent = cashInputRef.value as { $el: HTMLElement } | null
        const inputElement = inputNumberComponent?.$el?.querySelector('input') as HTMLInputElement | null
        if (inputElement) {
          inputElement.focus()
          inputElement.select()
        }
      } catch (e) {
        // 聚焦失败时静默处理
      }
    }
  }
})

const handleMenuUpdate = (_message: any) => {
  loadMenu(true)
}

const handleOrderStatusChange = (_message: any) => {
  if (showOrderQuery.value) {
    queryOrdersSilent()
  }
}

const handleOrderRefund = (message: any) => {
  ElMessage.warning(`订单 ${message.orderNumber} 已退款`)
  if (showOrderQuery.value) {
    queryOrdersSilent()
  }
}

let timer: number | null = null
let menuRefreshTimer: number | null = null
let handleKeyDown: ((event: KeyboardEvent) => void) | null = null

onMounted(async () => {
  // 主题初始化
  initTime()
  
  // 购物车恢复
  initFromStorage()
  
  // 菜单加载
  loadMenu()
  
  // 叫号统计
  loadCallNumberStats()
  callNumberStatsTimer = window.setInterval(loadCallNumberStats, 30000)

  // 设置WebSocket连接状态回调
  webSocketService.setOnConnectionChange((connected) => {
    if (connected) {
      if (menuRefreshTimer) {
        clearInterval(menuRefreshTimer)
        menuRefreshTimer = null
      }
    } else {
      if (!menuRefreshTimer) {
        menuRefreshTimer = window.setInterval(() => {
          loadMenu(true)
        }, 1800000)  // 30分钟
      }
    }
  })
  
  try {
    await webSocketService.connect()
    wsMenuSubscription = await webSocketService.subscribe('/topic/menu/update', handleMenuUpdate)
    wsOrderStatusSubscription = await webSocketService.subscribe('/topic/orders/status', handleOrderStatusChange)
    wsOrderRefundSubscription = await webSocketService.subscribe('/topic/orders/refund', handleOrderRefund)
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    // WebSocket连接失败时，设置一个较长的备用刷新间隔（30分钟）
    menuRefreshTimer = window.setInterval(() => {
      loadMenu(true)
    }, 1800000)
  }
  
  handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Enter' && showPaymentConfirm.value) {
      event.preventDefault()
      event.stopPropagation()
      confirmPayment()
    }
  }
  window.addEventListener('keydown', handleKeyDown, true)
})

onUnmounted(() => {
  cleanupTime()
  if (timer) clearInterval(timer)
  if (menuRefreshTimer) clearInterval(menuRefreshTimer)
  if (orderQueryTimer) clearInterval(orderQueryTimer)
  if (callNumberStatsTimer) clearInterval(callNumberStatsTimer)
  if (wsMenuSubscription) {
    webSocketService.unsubscribe(wsMenuSubscription)
  }
  if (wsOrderStatusSubscription) {
    webSocketService.unsubscribe(wsOrderStatusSubscription)
  }
  if (wsOrderRefundSubscription) {
    webSocketService.unsubscribe(wsOrderRefundSubscription)
  }
  if (handleKeyDown) {
    window.removeEventListener('keydown', handleKeyDown)
  }
  webSocketService.disconnect()
})

watch(showOrderQuery, (newVal) => {
  if (!newVal && orderQueryTimer) {
    clearInterval(orderQueryTimer)
    orderQueryTimer = null
  }
})

watch(showCoupon, (newVal) => {
  if (newVal) {
    nextTick(() => {
      setTimeout(() => {
        couponInputRef.value?.focus()
      }, 100)
    })
  }
})

watch([cart, totalAmount], ([newCart, newTotal]) => {
  if (newCart.length > 0) {
    customerDisplayStore.updateOrder({
      orderNumber: currentOrderNumber.value || `POS-${Date.now().toString(36).toUpperCase()}`,
      items: newCart.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: item.quantity
      })),
      totalAmount: newTotal
    })
  } else {
    customerDisplayStore.clearOrder()
  }
}, { deep: true })

</script>

<style scoped>
.fastfood-pos {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--pos-bg-primary);
  overflow: hidden;
  transition: background-color 0.3s ease;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

@media (max-width: 1400px) {
  .main-content {
    gap: 12px;
    padding: 12px;
  }
}

@media (max-width: 1200px) {
  .main-content {
    gap: 8px;
    padding: 8px;
  }
}

/* 小屏幕下菜品网格在上方，购物车在下方（column-reverse 让 CartSection 显示在底部） */
@media (max-width: 1000px) {
  .main-content {
    flex-direction: column-reverse;
    gap: 8px;
    padding: 8px;
    overflow-y: auto;
  }
}
</style>
