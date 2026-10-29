import { ref, watch, nextTick, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { posApi, type OrderResult, type OrderRequest } from '@/api/posApi'
import logger from '@/utils/logger'

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

    // 幂等守卫必须在任何 await 之前同步置位：
    // 旧实现把 isConfirming = true 放在 await ElMessageBox 之后，
    // 于是「回车（捕获监听）+ 输入框 keyup」两条路径都会穿过守卫，
    // 叠出两个「确认支付」框 → 点两次 → 重复下单/重复扣款。
    if (isConfirming || paying.value) {
      return
    }
    isConfirming = true

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
      // 用户取消支付确认：释放守卫，允许重新发起
      isConfirming = false
      return
    }

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
      // 实收现金 / 找零 / 付款码必须真正上送后端（此前收集后被丢弃，导致无法对账）
      const isCash = selectedPaymentMethod.value === 'cash'
      const cashReceivedAmount = Number.isFinite(cashReceived.value) ? Number(cashReceived.value) : 0
      const changeAmount =
        isCash && cashReceivedAmount > totalAmount.value
          ? Number((cashReceivedAmount - totalAmount.value).toFixed(2))
          : 0
      const payResult = await posApi.payOrder({
        orderId: orderId!,
        paymentMethod: paymentMethodName,
        amount: totalAmount.value,
        ...(isCash
          ? {
              cashReceived: Number(cashReceivedAmount.toFixed(2)),
              changeAmount
            }
          : {}),
        ...(!isCash && paymentCode.value ? { paymentCode: paymentCode.value.trim() } : {})
      }) as unknown as OrderResult

      if (payResult?.orderId) {
        const orderNum = payResult?.orderNumber || createResult?.orderNumber || orderId
        // 顾客侧展示叫号号（A001）；旧接口只回 pickupNumber 时兼容
        const pickupNum = payResult?.callNumber || payResult?.pickupNumber
          || (createResult as any)?.callNumber || orderNum
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
          logger.errorMessage('聚焦失败:', e)
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
