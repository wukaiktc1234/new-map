import { ref, watch, nextTick, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { posApi, type OrderResult, type OrderRequest } from '@/api/posApi'

export function usePayment(
  cart: Ref<any[]>,
  totalAmount: Ref<number>,
  _originalTotalAmount: Ref<number>,
  _discountAmount: Ref<number>,
  clearCart: () => void
) {
  const showPaymentConfirm = ref(false)
  const showPaymentSuccess = ref(false)
  const selectedPaymentMethod = ref<string>('')
  const cashReceived = ref(0)
  const paying = ref(false)
  const lastOrderNumber = ref('')
  const lastPickupNumber = ref('')
  const lastPickupCode = ref('')
  let isConfirming = false
  const cashInputRef = ref()
  const paymentCode = ref('')
  const paymentCodeInputRef = ref()

  const getPaymentMethodName = (method: string): string => {
    const map: Record<string, string> = {
      wechat: '微信支付',
      alipay: '支付宝',
      cash: '现金支付'
    }
    return map[method] || method
  }

  const quickPayment = (method: string) => {
    if (cart.value.length === 0) {
      ElMessage.warning('请先选择菜品')
      return
    }

    if (totalAmount.value <= 0) {
      ElMessage.warning('订单金额异常，无法结算')
      return
    }

    selectedPaymentMethod.value = method
    paymentCode.value = ''

    if (method === 'cash') {
      cashReceived.value = Math.ceil(totalAmount.value / 10) * 10
    } else {
      cashReceived.value = 0
    }

    showPaymentConfirm.value = true
  }

  const confirmPayment = async () => {
    if (cart.value.length === 0) {
      ElMessage.warning('请先选择菜品')
      return
    }

    if (isConfirming || paying.value) {
      return
    }

    try {
      await ElMessageBox.confirm(
        `确认支付 ¥${totalAmount.value.toFixed(2)} 元？`,
        '支付确认',
        {
          confirmButtonText: '确认支付',
          cancelButtonText: '再想想',
          type: 'warning',
          distinguishCancelAndClose: true,
          center: true
        }
      )
    } catch {
      return
    }

    isConfirming = true
    paying.value = true

    try {
      const orderData: OrderRequest = {
        items: cart.value.map(item => ({
          id: item.id,
          name: item.name,
          price: item.price,
          quantity: item.quantity,
          dishType: item.dishType
        })),
        totalAmount: totalAmount.value,
        paymentMethod: selectedPaymentMethod.value,
        tableNumber: 1
      }

      // 调用真实后端 API：先创建订单，再发起支付
      const createResult = await posApi.createOrder(orderData) as unknown as OrderResult

      if (!createResult?.orderId) {
        ElMessage.error('订单创建失败：未获取到订单ID')
        return
      }

      const orderId = createResult.orderId

      const paymentMethodName = getPaymentMethodName(selectedPaymentMethod.value)
      const payResult = await posApi.payOrder({
        orderId: orderId!,
        paymentMethod: paymentMethodName,
        amount: totalAmount.value
      }) as unknown as OrderResult

      if (payResult?.orderId) {
        const orderNum = payResult?.orderNumber || createResult?.orderNumber || orderId
        const pickupNum = payResult?.pickupNumber || orderNum
        const pickupCd = payResult?.pickupCode

        lastOrderNumber.value = orderNum
        lastPickupNumber.value = pickupNum
        lastPickupCode.value = pickupCd

        showPaymentConfirm.value = false
        showPaymentSuccess.value = true
        clearCart()

        ElMessage.success('支付成功，订单已推送至后厨')
      } else {
        ElMessage.error('支付失败，订单已创建但未完成支付')
      }
    } catch (error: unknown) {
      // 支付流程异常时展示真实错误信息，便于排查
      const errMsg = error instanceof Error ? error.message
        : (error && typeof error === 'object' && 'message' in error
            ? String((error as { message: unknown }).message)
            : '支付流程异常')
      ElMessage.error(errMsg)
    } finally {
      paying.value = false
      isConfirming = false
    }
  }

  const handlePaymentConfirm = async (data: { method: string; cashReceived?: number; paymentCode?: string }) => {
    if (!data.method) {
      ElMessage.warning('请选择支付方式')
      return
    }

    selectedPaymentMethod.value = data.method

    if (data.method === 'cash' && data.cashReceived !== undefined) {
      cashReceived.value = data.cashReceived
    }

    if (data.paymentCode !== undefined) {
      paymentCode.value = data.paymentCode
    }

    await confirmPayment()
  }

  watch(showPaymentConfirm, async (newVal) => {
    if (newVal) {
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      if (selectedPaymentMethod.value === 'cash') {
        try {
          const inputNumberComponent = cashInputRef.value as { $el: HTMLElement } | null
          const inputElement = inputNumberComponent?.$el?.querySelector('input') as HTMLInputElement | null
          if (inputElement) {
            inputElement.focus()
            inputElement.select()
          }
        } catch (e) {
          console.error('聚焦失败:', e)
        }
      }
    }
  })

  watch(showPaymentSuccess, () => {})

  return {
    showPaymentConfirm,
    showPaymentSuccess,
    selectedPaymentMethod,
    cashReceived,
    paying,
    lastOrderNumber,
    lastPickupNumber,
    lastPickupCode,
    cashInputRef,
    paymentCode,
    paymentCodeInputRef,
    quickPayment,
    confirmPayment,
    handlePaymentConfirm
  }
}
