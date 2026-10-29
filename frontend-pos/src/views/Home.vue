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

    <!-- 班次三态提示：no-shift / 未上线(404) / 加载失败 三种文案必须不同，且都不阻塞收银 -->
    <div
      v-if="shiftStateText"
      class="shift-state"
      :class="'shift-state--' + shiftState"
      :data-test="'shift-state-' + shiftState"
      role="status"
    >
      <span class="shift-state__title">{{ shiftStateText.title }}</span>
      <span class="shift-state__hint">{{ shiftStateText.hint }}</span>
      <span
        v-if="shiftStateDetail && (shiftState === 'error' || shiftState === 'bad-request')"
        class="shift-state__detail"
      >（{{ shiftStateDetail }}）</span>
      <el-button
        v-if="shiftState === 'no-shift'"
        type="primary"
        size="small"
        data-test="shift-state-open"
        @click="showStartShift = true"
      >去开班</el-button>
      <el-button
        v-else-if="shiftState === 'error' || shiftState === 'bad-request'"
        size="small"
        data-test="shift-state-retry"
        @click="retryShiftCheck"
      >重试</el-button>
    </div>

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
        :error-status="loadErrorStatus"
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
      :paying="paying"
      @confirm="handlePaymentConfirm"
    />

    <!-- 支付成功对话框 -->
    <PaymentSuccessDialog
      v-model="showPaymentSuccess"
      :order-number="lastOrderNumber"
      :call-number="lastPickupNumber"
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

    <!-- 交班/日结对话框（班次交接功能待后端接线，确认按钮禁用，见 ShiftHandover.vue） -->
    <ShiftHandover v-model="showShiftHandover" />

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

    <!-- 删除撤销条：删除立即生效，5 秒内可一键找回 -->
    <CartUndoBar :pending="pendingUndo" @undo="undoRemove" />

    <!-- 开班（备用金）对话框 -->
    <StartShiftDialog
      v-model="showStartShift"
      :terminal-id="terminalId"
      :employee-id="employeeId"
      :employee-name="employeeName"
      @success="handleShiftStarted"
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
import StartShiftDialog from '@/components/pos/dialogs/StartShiftDialog.vue'
import { SHIFT_STATE_TEXT, classifyShiftFailure, shiftFailureMessage, type ShiftStateKind } from '@/utils/shiftStatus'
import PosTopBar from '@/components/pos/PosTopBar.vue'
import MenuSection from '@/components/pos/MenuSection.vue'
import CartSection from '@/components/pos/CartSection.vue'
import CartUndoBar from '@/components/pos/CartUndoBar.vue'

// Composable 导入
import { useCart } from '@/composables/useCart'
import { usePayment } from '@/composables/usePayment'
import { useMenu } from '@/composables/useMenu'
import { useHangingOrder } from '@/composables/useHangingOrder'
import { useCustomerDisplayStore } from '@/stores/customer-display'
import { useTheme } from '@/composables/useTheme'
import { formatDate } from '@/utils/format'
import { hasActiveShiftMark, markShiftStarted } from '@/utils/shiftSession'
import { usePosPreferences } from '@/composables/usePosPreferences'
import { usePermission, PERM } from '@/composables/usePermission'
import logger from '@/utils/logger'

const router = useRouter()

// 权限（默认拒绝：无 order:refund 时退款入口不渲染、不请求）
const { can, markDenied } = usePermission()

// ========== 使用Composables ==========
const {
  cart, totalQuantity, originalTotalAmount, currentOrderNumber,
  startEditQuantity, confirmEditQuantity, cancelEditQuantity,
  addToCart, increaseItem, decreaseItem,
  swipeDeleteItem, clearCart, initFromStorage,
  pendingUndo, undoRemove
} = useCart()

const discountAmount = ref(0)
const totalAmount = computed(() => Math.max(0, originalTotalAmount.value - discountAmount.value))

const {
  showPaymentConfirm, showPaymentSuccess, selectedPaymentMethod,
  lastOrderNumber, lastPickupNumber,
  cashInputRef, paying,
  quickPayment, confirmPayment, handlePaymentConfirm
} = usePayment(cart, totalAmount, originalTotalAmount, discountAmount, clearCart)

const {
  loading, loadError, loadErrorStatus, categories, activeCategory,
  filteredItems, lowStockItems, loadMenu
} = useMenu()

const {
  hangingOrderStore, showHoldOrder, showRetrieveOrder,
  holdOrderForm, isSubmitting, showHoldOrderDialog, confirmHoldOrder,
  showRetrieveOrderDialog, selectHeldOrder, clearHeldOrders
} = useHangingOrder(cart, totalAmount as any, clearCart)

const { isDark, currentTime, toggleTheme, initTime, cleanupTime } = useTheme()
/** 个人偏好（回车确认结账 / 界面缩放），由头像菜单的「偏好设置」写入 */
const { prefs } = usePosPreferences()
const customerDisplayStore = useCustomerDisplayStore()
const showOrderQuery = ref(false)

/* ===== 班次三态：no-shift（正常无班次）/ not-implemented（404）/ error（网络或 5xx） ===== */
/** 空串 = 加载中或已有在班班次，不提示 */
const shiftState = ref<'' | ShiftStateKind>('')
const shiftStateDetail = ref('')
const shiftStateText = computed(() => (shiftState.value ? SHIFT_STATE_TEXT[shiftState.value] : null))

function retryShiftCheck(): void {
  shiftState.value = ''
  shiftStateDetail.value = ''
  void maybeOpenStartShift()
}

/* ===== N19-4：开班（备用金） ===== */
const showStartShift = ref(false)
const terminalId = ref('')
const employeeId = ref('')
const employeeName = ref('')

/**
 * 登录后若无进行中的班次 → 弹出开班对话框（备用金默认值见 StartShiftDialog）
 *
 * 防重复弹窗（图一 状态持久化）：
 *   ① 先看本地开班标记（开班成功即写入，交班/登出时清除）——
 *      刷新页面、切页返回、重新进入首页都不再弹窗，也不额外打后端；
 *   ② 本地没有标记时才问后端 /current-shift，避免多终端共用同一终端号时状态不一致；
 *   ③ 后端说"有班次"时回写标记，下次直接走本地快路径。
 */
async function maybeOpenStartShift(): Promise<void> {
  try {
    const terminal = localStorage.getItem('pos-terminal') || ''
    const userRaw = localStorage.getItem('pos-user')
    terminalId.value = terminal
    if (userRaw) {
      const user = JSON.parse(userRaw) as { employeeId?: string | number; employeeName?: string; name?: string }
      employeeId.value = user?.employeeId != null ? String(user.employeeId) : ''
      employeeName.value = user?.employeeName || user?.name || ''
    }
    if (!terminal) return

    // ① 本终端当天已开班（且未交班）→ 静默跳过，刷新页面不再打扰收银员
    if (hasActiveShiftMark(terminal)) return

    const current = await posApi.getCurrentShift(terminal)
    // b) 200 + data=null → 真的没开班（正常业务态）：提示 + 开班入口，绝不当作故障
    if (!current) {
      shiftState.value = 'no-shift'
      shiftStateDetail.value = ''
      showStartShift.value = true
      return
    }
    shiftState.value = ''
    if (current) {
      // ③ 后端已有进行中的班次（例如换了一台终端登录同一账号）→ 回写标记
      markShiftStarted({
        terminalId: terminal,
        shiftId: current.shiftId,
        employeeId: employeeId.value,
        employeeName: employeeName.value,
        openingCash: typeof current.openingCash === 'number' ? current.openingCash : null,
        startedAt: current.startTime,
      })
      return
    }
  } catch (error) {
    // a) 404：接口不存在 → 「该功能未上线（V2.0）」，不阻塞收银
    // c) 其它：网络/5xx → 「班次状态加载失败，可重试」+ 重试
    shiftState.value = classifyShiftFailure(error)
    shiftStateDetail.value = shiftFailureMessage(error)
    logger.errorMessage('[Home] 检查当前班次失败（三态提示已展示，不阻塞收银）', error)
  }
}

function handleShiftStarted(): void {
  ElMessage.success('班次已开始')
  showStartShift.value = false
}
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
      params.startDate = formatDate(queryDateRange.value[0])
      params.endDate = formatDate(queryDateRange.value[1])
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
    logger.errorMessage('静默查询订单失败:', error)
  }
}

const queryOrders = async () => {
  try {
    const params: any = {}
    if (queryKeyword.value) {
      params.keyword = queryKeyword.value
    }
    if (queryDateRange.value && queryDateRange.value.length === 2) {
      params.startDate = formatDate(queryDateRange.value[0])
      params.endDate = formatDate(queryDateRange.value[1])
    }

    const result = await request.get('/v1/pos/orders', { params }) as any

    if (Array.isArray(result)) {
      orderList.value = result
    } else {
      orderList.value = []
    }
  } catch (error) {
    // 订单查询失败时显示空列表，由用户决定是否重试
    logger.errorMessage('查询订单失败:', error)
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
    logger.errorMessage('获取订单详情失败:', error)
  }
}

const refundOrder = async (order: any) => {
  // 权限闸门：无 order:refund 直接提示，不发请求（后端也会 403，这里避免无谓的失败弹窗）
  if (!can(PERM.REFUND)) {
    ElMessage.warning('当前账号无退款权限，请联系店长授权')
    return
  }

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
    if (error === 'cancel') return
    const status = error?.status ?? error?.response?.status
    if (status === 403) {
      // 后端判定越权：记住这次拒绝，本次会话不再显示退款入口
      markDenied(PERM.REFUND)
      ElMessage.error('退款被拒绝（403）：当前账号无 order:refund 权限，已隐藏退款入口')
      return
    }
    ElMessage.error(error?.message || '退款失败')
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

/** 焦点是否落在可编辑控件内（输入框 / 文本域 / 下拉 / contenteditable） */
function isEditableTarget(target: EventTarget | null): boolean {
  const el = target as HTMLElement | null
  if (!el || typeof el.tagName !== 'string') return false
  const tag = el.tagName.toUpperCase()
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
  return el.isContentEditable === true
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
  
  // 开班检查（无进行中班次时弹出备用金输入）
  void maybeOpenStartShift()

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
    logger.errorMessage('WebSocket连接失败:', error)
    // WebSocket连接失败时，设置一个较长的备用刷新间隔（30分钟）
    menuRefreshTimer = window.setInterval(() => {
      loadMenu(true)
    }, 1800000)
  }
  
  handleKeyDown = (event: KeyboardEvent) => {
    if (event.key !== 'Enter' || !showPaymentConfirm.value) return
    // 偏好设置里可关闭「回车直接确认」，避免误触结账（默认开启）
    if (!prefs.enterConfirmCheckout) return
    // 焦点在输入框内时交给弹窗自己的 @keyup.enter 处理：
    // 否则「捕获阶段的全局监听」与「输入框局部监听」会对同一次回车各触发一遍，
    // 叠加出两个确认框 → 点两次 → 重复下单（后厨端 useKeyboardShortcuts 有同样的排除，收银端此前缺失）。
    if (isEditableTarget(event.target)) return
    event.preventDefault()
    event.stopPropagation()
    confirmPayment()
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
/* 班次三态提示条 */
.shift-state {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed);
}

.shift-state__title {
  font-weight: 600;
}

.shift-state__hint {
  color: var(--pos-text-secondary, #909399);
}

.shift-state--no-shift {
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary-dark-2, #337ecc);
}

.shift-state--not-implemented,
.shift-state--bad-request {
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
}

.shift-state--error {
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger-dark-2, #c45656);
}


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
  gap: var(--pos-spacing-sm, 16px);
  padding: var(--pos-spacing-sm, 16px);
  overflow: hidden;
  background: var(--pos-bg-canvas, #f2f3f5);
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
