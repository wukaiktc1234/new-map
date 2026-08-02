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

  async function selectHeldOrder(order: HeldOrder) {
    try {
      await ElMessageBox.confirm(
        `确定要恢复该挂单吗？\n\n订单金额：¥${order.totalAmount.toFixed(2)}\n挂单时间：${order.holdTime}`,
        '恢复挂单',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
      )
      const items = await hangingOrderStore.resumeOrder(order.id)
      if (items) {
        cart.value = items
        showRetrieveOrder.value = false
        ElMessage.success('已恢复挂单')
      } else {
        ElMessage.error(hangingOrderStore.error || '恢复失败')
      }
    } catch {
      // 用户取消
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
