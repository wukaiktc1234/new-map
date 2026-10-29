import { ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useHangingOrderStore } from '@/stores/hanging-order'
import type { HeldOrder } from '@/stores/hanging-order'

export function useHangingOrder(
  cart: Ref<any[]>,
  totalAmount: Ref<number>,
  clearCart: () => void
) {
  const hangingOrderStore = useHangingOrderStore()
  const showHoldOrder = ref(false)
  const showRetrieveOrder = ref(false)
  const showHangingPanel = ref(false)
  const holdOrderForm = ref({ remark: '' })
  const isSubmitting = ref(false)

  function showHoldOrderDialog() {
    if (cart.value.length === 0) {
      ElMessage.warning('购物车为空，无法挂单')
      return
    }
    holdOrderForm.value.remark = ''
    showHoldOrder.value = true
  }

  async function confirmHoldOrder() {
    if (isSubmitting.value) return
    isSubmitting.value = true
    const success = await hangingOrderStore.hangOrder(
      cart.value,
      totalAmount.value,
      holdOrderForm.value.remark || undefined
    )
    if (success) {
      clearCart()
      showHoldOrder.value = false
      ElMessage.success('挂单成功')
    } else {
      ElMessage.error(hangingOrderStore.error || '挂单失败')
    }
    isSubmitting.value = false
  }

  async function showRetrieveOrderDialog() {
    await hangingOrderStore.fetchHeldOrders()
    showRetrieveOrder.value = true
  }

  /**
   * 取单（从取单面板选中一张挂单恢复）。
   *
   * P0 修复：旧实现只确认「是否恢复该挂单」，随后直接 `cart.value = items` 覆盖当前购物车——
   * 收银员手上正点的菜会**静默消失**。现在：
   *   ① 当前购物车非空时，先明确告知"将被替换"并给出待替换件数，确认后才继续；
   *   ② 覆盖前把当前购物车自动挂起（转成一张新挂单），把"丢单"变成"可找回"；
   *   ③ 复用同文件既有的 `quickRestoreHeldOrder` 护栏，保持一处口径。
   */
  async function selectHeldOrder(order: HeldOrder) {
    const hasCurrentCart = cart.value.length > 0

    if (hasCurrentCart) {
      const currentCount = cart.value.reduce((sum: number, item: any) => sum + (item.quantity || 0), 0)
      try {
        await ElMessageBox.confirm(
          `恢复挂单会替换当前购物车。\n\n待恢复：¥${order.totalAmount.toFixed(2)}（${order.holdTime}）\n将被替换：当前 ${currentCount} 件商品\n\n当前购物车会先自动挂起，之后可从"取单"里找回。`,
          '替换购物车？',
          {
            confirmButtonText: '替换并挂起当前车',
            cancelButtonText: '取消',
            type: 'warning',
            distinguishCancelAndClose: true,
            closeOnClickModal: false
          }
        )
      } catch {
        return
      }

      // 先挂起当前购物车（失败则不覆盖，宁可让收银员重试也不丢单）
      const parked = await hangingOrderStore.hangOrder(
        cart.value,
        totalAmount.value,
        `取单自动挂起 ${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`
      )
      if (!parked) {
        ElMessage.error(hangingOrderStore.error || '当前购物车挂起失败，已取消取单')
        return
      }
    }

    const items = await hangingOrderStore.resumeOrder(order.id)
    if (items) {
      cart.value = items
      showRetrieveOrder.value = false
      ElMessage.success(hasCurrentCart ? '已恢复挂单，原购物车已挂起' : '已恢复挂单')
    } else {
      ElMessage.error(hangingOrderStore.error || '恢复失败')
    }
  }

  async function clearHeldOrders() {
    try {
      await ElMessageBox.confirm(
        '确定要清空所有挂单吗？此操作不可撤销。',
        '清空挂单',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
      await hangingOrderStore.clearAllOrders()
      ElMessage.success('已清空所有挂单')
    } catch {
      // 用户取消
    }
  }

  async function quickRestoreHeldOrder(order: HeldOrder) {
    if (cart.value.length > 0) {
      try {
        await ElMessageBox.confirm(
          '当前购物车已有商品，恢复挂单将清空当前购物车。是否继续？',
          '替换购物车',
          { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
        )
      } catch {
        return
      }
    }
    const items = await hangingOrderStore.resumeOrder(order.id)
    if (items) {
      cart.value = items
      ElMessage.success('已恢复挂单')
    } else {
      ElMessage.error(hangingOrderStore.error || '恢复失败')
    }
  }

  return {
    hangingOrderStore,
    showHoldOrder,
    showRetrieveOrder,
    showHangingPanel,
    holdOrderForm,
    isSubmitting,
    showHoldOrderDialog,
    confirmHoldOrder,
    showRetrieveOrderDialog,
    selectHeldOrder,
    clearHeldOrders,
    quickRestoreHeldOrder
  }
}
