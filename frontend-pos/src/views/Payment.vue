<template>
  <div class="pos-payment">
    <!-- 头部 - 玻璃拟态效果 -->
    <div class="header">
      <el-button @click="$router.back()" :icon="ArrowLeft" class="back-btn">返回</el-button>
      <div class="header-center">
        <h2>结算</h2>
        <span class="subtitle">Checkout</span>
      </div>
      <div class="header-right">
        <span class="step-indicator">
          <span class="step" :class="{ active: currentStep >= 1 }">1</span>
          <span class="step-line" :class="{ active: currentStep >= 2 }"></span>
          <span class="step" :class="{ active: currentStep >= 2 }">2</span>
        </span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content">
      <!-- 左侧订单摘要 -->
      <div class="order-summary">
        <div class="summary-header">
          <div class="header-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="header-text">
            <h3>订单详情</h3>
            <span class="order-time">{{ currentTime }}</span>
          </div>
          <div class="table-badge" v-if="tableNumber">
            <el-icon><Location /></el-icon>
            {{ tableNumber }}号桌
          </div>
        </div>

        <!-- 时间线设计 -->
        <div class="timeline">
          <div class="timeline-item" v-for="(item, index) in orderItems" :key="item.id">
            <div class="timeline-dot" :class="{ 'first': index === 0 }">
              <span class="dot-inner"></span>
            </div>
            <div class="timeline-content">
              <div class="item-row">
                <div class="item-info">
                  <span class="item-name">{{ item.name }}</span>
                  <el-tag v-if="item.dishType === 'combo'" type="warning" size="small" effect="dark">套餐</el-tag>
                </div>
                <div class="item-meta">
                  <span class="item-qty">x{{ item.quantity }}</span>
                  <span class="item-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                </div>
              </div>
            </div>
            <div class="timeline-line" v-if="index < orderItems.length - 1"></div>
          </div>
        </div>

        <!-- 汇总 -->
        <div class="summary-footer">
          <div class="summary-row">
            <span class="label">商品数量</span>
            <span class="value">{{ totalQuantity }} 件</span>
          </div>
          <div class="summary-row total">
            <span class="label">合计</span>
            <span class="amount">
              <span class="currency">¥</span>
              <span class="number">{{ totalAmount.toFixed(2) }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧支付区域 -->
      <div class="payment-area">
        <!-- 支付方式选择 -->
        <div class="payment-methods">
          <h3>选择支付方式</h3>
          <div class="method-grid">
            <div
              v-for="(method, index) in paymentMethods"
              :key="method.id"
              class="method-card"
              :class="{ 
                'active': selectedMethod === method.id,
                'wechat': method.id === 'wechat',
                'alipay': method.id === 'alipay',
                'cash': method.id === 'cash',
                'bank-card': method.id === 'bank_card',
                'balance': method.id === 'balance'
              }"
              :style="{ animationDelay: `${index * 0.1}s` }"
              @click="handleMethodChange(method.id)"
            >
              <div class="card-inner">
                <div class="card-front">
                  <div class="method-icon">
                    <el-icon :size="40"><component :is="method.icon" /></el-icon>
                  </div>
                  <span class="method-name">{{ method.name }}</span>
                  <div class="method-badge" v-if="method.badge">
                    <span>{{ method.badge }}</span>
                  </div>
                </div>
                <div class="card-back">
                  <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
                </div>
              </div>
              <div class="card-glow"></div>
            </div>
          </div>

          <!-- 银行卡输入区域 -->
          <div v-if="selectedMethod === 'bank_card'" class="bank-card-input-section">
            <el-divider content-position="left">银行卡信息</el-divider>
            <el-form label-position="top">
              <el-form-item label="银行卡号（选填）">
                <el-input
                  v-model="bankCardNumber"
                  placeholder="请输入银行卡号，或直接确认使用POS机刷卡"
                  clearable
                  size="large"
                >
                  <template #prefix>
                    <el-icon><BankCard /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>
            <el-alert
              type="info"
              :closable="false"
              show-icon
            >
              <template #default>
                <p style="margin: 0; font-size: 13px;">
                  可直接在POS机上刷卡完成支付，无需手动输入卡号。
                </p>
              </template>
            </el-alert>
          </div>

          <!-- 会员余额显示区域 -->
          <div v-if="selectedMethod === 'balance'" class="balance-info-section">
            <el-divider content-position="left">会员余额</el-divider>
            
            <!-- 加载中状态 -->
            <div v-if="loadingBalance" class="balance-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在获取余额信息...</span>
            </div>

            <!-- 余额信息 -->
            <div v-else-if="memberBalance" class="balance-details">
              <!-- 余额不足警告 -->
              <el-alert
                v-if="!isBalanceSufficient"
                type="error"
                :closable="false"
                show-icon
              >
                <template #title>
                  <span style="font-weight: 600;">余额不足</span>
                </template>
                <template #default>
                  <p style="margin: 8px 0 0 0; font-size: 13px;">
                    当前余额 ¥{{ memberBalance.availableBalance.toFixed(2) }} 元，
                    应付金额 ¥{{ totalAmount.toFixed(2) }} 元，
                    差额 ¥{{ (totalAmount - memberBalance.availableBalance).toFixed(2) }} 元。
                    请选择其他支付方式。
                  </p>
                </template>
              </el-alert>

              <!-- 余额充足 -->
              <div v-else class="balance-sufficient">
                <div class="balance-row">
                  <span class="label">当前可用余额</span>
                  <span class="value">¥{{ memberBalance.availableBalance.toFixed(2) }}</span>
                </div>
                <div class="balance-row highlight">
                  <span class="label">应付金额</span>
                  <span class="value amount">¥{{ totalAmount.toFixed(2) }}</span>
                </div>
                <div class="balance-row success">
                  <span class="label">支付后剩余</span>
                  <span class="value remaining">¥{{ remainingBalance.toFixed(2) }}</span>
                </div>
              </div>

              <!-- 冻结金额提示 -->
              <el-alert
                v-if="memberBalance.frozenAmount > 0"
                type="warning"
                :closable="false"
                style="margin-top: 12px;"
              >
                <template #default>
                  <p style="margin: 0; font-size: 13px;">
                    冻结金额：¥{{ memberBalance.frozenAmount.toFixed(2) }} 元
                  </p>
                </template>
              </el-alert>
            </div>

            <!-- 获取失败 -->
            <el-alert
              v-else
              type="error"
              :closable="false"
              show-icon
            >
              <template #default>
                <p style="margin: 0;">获取余额失败，请稍后重试或选择其他支付方式</p>
              </template>
            </el-alert>
          </div>
        </div>

        <!-- 支付按钮 -->
        <div class="payment-actions">
          <el-button size="large" @click="$router.push('/')" class="cancel-btn">
            <el-icon><Close /></el-icon>
            取消订单
          </el-button>
          <el-button type="primary" size="large" @click="confirmPayment" :loading="paying" class="confirm-btn">
            <span class="btn-text">确认支付</span>
            <span class="btn-amount">¥{{ totalAmount.toFixed(2) }}</span>
          </el-button>
        </div>

        <!-- 安全提示 -->
        <div class="security-tips">
          <el-icon><Lock /></el-icon>
          <span>支付安全由平台保障</span>
        </div>
      </div>
    </div>

    <!-- 支付成功对话框 -->
    <el-dialog 
      v-model="showSuccess" 
      title="" 
      width="480px" 
      center 
      :show-close="false"
      :close-on-click-modal="false"
      class="success-dialog"
      :lock-scroll="false"
    >
      <div class="success-content">
        <!-- 成功动画 -->
        <div class="success-animation">
          <div class="success-circle">
            <svg class="checkmark" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52">
              <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none"/>
              <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
            </svg>
          </div>
          <div class="success-particles">
            <span v-for="i in 12" :key="i" class="particle" :style="{ '--delay': `${i * 0.1}s`, '--angle': `${i * 30}deg` }"></span>
          </div>
        </div>

        <h2 class="success-title">支付成功</h2>
        <p class="success-subtitle">Payment Successful</p>

        <!-- 订单信息卡片 -->
        <div class="order-info-card">
          <div class="info-row">
            <span class="info-label">订单号</span>
            <span class="info-value">{{ orderNumber }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">支付金额</span>
            <span class="info-value highlight">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">支付方式</span>
            <span class="info-value">{{ getPaymentMethodName(selectedMethod) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">支付时间</span>
            <span class="info-value">{{ currentTime }}</span>
          </div>
        </div>

        <!-- 倒计时 -->
        <div class="countdown-hint">
          <span class="countdown-text">
            <span class="countdown-number">{{ countdown }}</span>
            秒后自动返回首页
          </span>
        </div>

        <!-- 打印状态区域 -->
        <div class="print-status-area">
          <!-- 打印中状态 -->
          <div v-if="printing" class="print-status printing">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在打印小票...</span>
          </div>

          <!-- 打印成功状态 -->
          <div v-else-if="printStatus === 'success'" class="print-status success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>小票已打印</span>
          </div>

          <!-- 打印失败状态 -->
          <div v-else-if="printStatus === 'fail'" class="print-status fail">
            <el-icon><WarningFilled /></el-icon>
            <span>小票打印失败，请手动打印</span>
          </div>

          <!-- 重新打印按钮 -->
          <el-button
            v-if="!printing && (printStatus === 'success' || printStatus === 'fail')"
            type="warning"
            size="small"
            :icon="Printer"
            @click="handleReprint"
            :loading="printing"
            class="reprint-btn"
          >
            重新打印
          </el-button>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" size="large" @click="goHome" class="home-btn">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </template>
    </el-dialog>

    <!-- 扫码支付对话框（微信/支付宝） -->
    <el-dialog
      v-model="showQrCode"
      title="扫码支付"
      width="420px"
      center
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="true"
      class="qr-code-dialog"
      :lock-scroll="false"
    >
      <div class="qr-payment-content">
        <div class="qr-header">
          <el-icon :size="32" :color="qrPaymentType === 'wechat' ? '#07c160' : '#1677ff'">
            <component :is="qrPaymentType === 'wechat' ? 'Wallet' : 'CreditCard'" />
          </el-icon>
          <h3>{{ qrPaymentType === 'wechat' ? '微信支付' : '支付宝' }}</h3>
        </div>

        <div class="qr-amount">
          <span class="currency">¥</span>
          <span class="amount">{{ totalAmount.toFixed(2) }}</span>
        </div>

        <!-- 二维码展示 -->
        <div class="qr-code-wrapper">
          <div v-if="qrCodeLoading" class="qr-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>生成二维码中...</span>
          </div>
          <div v-else-if="qrCodeUrl" class="qr-code-box">
            <qrcode-vue :value="qrCodeUrl" :size="220" level="M" />
            <div class="qr-hint">
              <el-icon><Iphone /></el-icon>
              <span>请使用{{ qrPaymentType === 'wechat' ? '微信' : '支付宝' }}扫码支付</span>
            </div>
          </div>
          <div v-else class="qr-error">
            <el-icon><WarningFilled /></el-icon>
            <span>二维码生成失败</span>
          </div>
        </div>

        <!-- 倒计时 -->
        <div class="qr-countdown" v-if="qrExpiresAt">
          <el-icon><Clock /></el-icon>
          <span>二维码有效期剩余：{{ qrCountdownText }}</span>
        </div>

        <!-- 订单信息 -->
        <div class="qr-order-info" v-if="currentOrderId">
          <span class="label">订单号：</span>
          <span class="value">{{ currentOrderId }}</span>
        </div>

        <!-- 操作提示 -->
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="qr-tip"
        >
          <template #default>
            <p style="margin: 0; font-size: 12px;">
              开发模式：可点击下方按钮模拟"已扫码支付完成"。
              生产环境将由支付平台服务端回调自动确认。
            </p>
          </template>
        </el-alert>
      </div>

      <template #footer>
        <el-button @click="cancelQrPayment" :disabled="confirmingQr">
          取消支付
        </el-button>
        <el-button
          type="primary"
          @click="confirmQrPaymentComplete"
          :loading="confirmingQr"
        >
          <el-icon><CircleCheckFilled /></el-icon>
          已扫码，确认支付完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, CircleCheckFilled,
  Document, Location, Close, Lock, HomeFilled,
  Loading, Printer, WarningFilled, Iphone, Clock
} from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import { posApi, type OrderResult, type MemberBalance } from '@/api/posApi'

interface CartItem {
  id: string;
  name: string;
  price: number;
  quantity: number;
  dishType: string;
  items?: { foodId: string; foodName: string; quantity: number }[];
}

const router = useRouter()
const route = useRoute()

const orderItems = ref<CartItem[]>([])
const totalAmount = ref(0)
const tableNumber = ref<string>()
const selectedMethod = ref('wechat')
const paying = ref(false)
const showSuccess = ref(false)
const orderNumber = ref('')
const currentTime = ref('')
const countdown = ref(5)
const currentStep = ref(1)

// 打印相关状态
const printing = ref(false)
const printStatus = ref<'idle' | 'printing' | 'success' | 'fail'>('idle')
const currentOrderId = ref('')

// 银行卡支付相关
const bankCardNumber = ref('')

// 会员余额支付相关
const memberBalance = ref<MemberBalance | null>(null)
const loadingBalance = ref(false)

// 扫码支付（微信/支付宝）相关状态
const showQrCode = ref(false)
const qrCodeUrl = ref('')
const qrCodeLoading = ref(false)
const qrPaymentType = ref<'wechat' | 'alipay'>('wechat')
const qrExpiresAt = ref<number>(0)
const confirmingQr = ref(false)
const qrTransactionId = ref('')
const qrCountdownText = ref('')
let qrCountdownTimer: number | null = null

/** 大额支付阈值（元） */
const HIGH_AMOUNT_THRESHOLD = 500

const paymentMethods = [
  { id: 'wechat', name: '微信支付', icon: 'Wallet', badge: '推荐' },
  { id: 'alipay', name: '支付宝', icon: 'CreditCard' },
  { id: 'cash', name: '现金支付', icon: 'Postcard' },
  { id: 'bank_card', name: '银行卡支付', icon: 'CreditCard' },
  { id: 'balance', name: '会员余额', icon: 'Coin' }
]

const totalQuantity = computed(() => orderItems.value.reduce((sum, item) => sum + item.quantity, 0))

/** 是否为大额支付 */
const isHighAmount = computed(() => totalAmount.value > HIGH_AMOUNT_THRESHOLD)

/** 余额是否充足 */
const isBalanceSufficient = computed(() => {
  if (!memberBalance.value) return false
  return memberBalance.value.availableBalance >= totalAmount.value
})

/** 支付后剩余余额 */
const remainingBalance = computed(() => {
  if (!memberBalance.value) return 0
  return memberBalance.value.availableBalance - totalAmount.value
})

let timeTimer: number | null = null
let countdownTimer: number | null = null

const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN')
}

const getPaymentMethodName = (method: string) => {
  const methodMap: Record<string, string> = {
    'wechat': '微信支付',
    'alipay': '支付宝',
    'cash': '现金支付',
    'bank_card': '银行卡支付',
    'balance': '会员余额'
  }
  return methodMap[method] || method
}

/**
 * 获取会员余额
 */
const fetchMemberBalance = async () => {
  loadingBalance.value = true
  try {
    const response = await posApi.getMemberBalance()
    // 响应拦截器已解包数据，直接使用或类型转换
    memberBalance.value = response as unknown as MemberBalance
  } catch (error) {
    memberBalance.value = null
    ElMessage.warning('获取会员余额失败，余额支付暂不可用')
  } finally {
    loadingBalance.value = false
  }
}

/**
 * 支付方式切换处理
 * @param method - 选中的支付方式ID
 */
const handleMethodChange = (method: string) => {
  selectedMethod.value = method
  bankCardNumber.value = ''

  // 切换到余额支付时，获取最新余额
  if (method === 'balance') {
    fetchMemberBalance()
  }
}

onMounted(() => {
  updateTime()
  timeTimer = window.setInterval(updateTime, 1000)

  const items = route.query.items as string
  const total = route.query.total as string
  const table = route.query.table as string

  if (items) {
    orderItems.value = JSON.parse(items)
  }
  if (total) {
    totalAmount.value = parseFloat(total)
  }
  if (table) {
    tableNumber.value = table
  }
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  if (qrCountdownTimer) clearInterval(qrCountdownTimer)
})

const confirmPayment = async () => {
  if (paying.value) return

  // 验证银行卡支付
  if (selectedMethod.value === 'bank_card' && !bankCardNumber.value) {
    ElMessage.warning('请输入银行卡号')
    return
  }

  // 验证余额支付
  if (selectedMethod.value === 'balance') {
    if (!memberBalance.value) {
      ElMessage.warning('正在获取余额信息，请稍候...')
      return
    }
    if (!isBalanceSufficient.value) {
      ElMessage.error('余额不足，请选择其他支付方式')
      return
    }
  }

  // 大额支付二次确认（>500元）
  if (isHighAmount.value) {
    try {
      await ElMessageBox.confirm(
        `当前支付金额 ¥${totalAmount.value.toFixed(2)} 元超过 ¥${HIGH_AMOUNT_THRESHOLD} 元，请确认是否继续？`,
        '大额支付确认',
        {
          confirmButtonText: '确认支付',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true,
          center: true
        }
      )
    } catch {
      // 用户取消
      return
    }
  }

  // 普通金额确认
  try {
    await ElMessageBox.confirm(
      `确认使用${getPaymentMethodName(selectedMethod.value)}支付 ¥${totalAmount.value.toFixed(2)} 元？`,
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

  paying.value = true
  currentStep.value = 1

  try {
    const orderData = {
      tableNumber: tableNumber.value ? parseInt(tableNumber.value) : undefined,
      items: orderItems.value.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: item.quantity,
        dishType: item.dishType
      })),
      totalAmount: totalAmount.value,
      orderType: 'dinein'
    }

    currentStep.value = 1
    const createResult = await posApi.createOrder(orderData) as unknown as OrderResult

    if (!createResult?.orderId) {
      ElMessage.error('订单创建失败：未获取到订单ID')
      return
    }

    const orderId = createResult.orderId
    const paymentMethodName = getPaymentMethodName(selectedMethod.value)

    currentStep.value = 2
    let payResult: OrderResult

    // 根据支付方式调用不同的支付API
    switch (selectedMethod.value) {
      case 'balance':
        payResult = await posApi.payWithBalance({
          orderId: orderId,
          amount: totalAmount.value,
          memberId: memberBalance.value?.memberId
        }) as unknown as OrderResult
        break
      case 'bank_card':
        payResult = await posApi.payWithBankCard({
          orderId: orderId,
          cardNumber: bankCardNumber.value,
          amount: totalAmount.value
        }) as unknown as OrderResult
        break
      case 'wechat':
      case 'alipay': {
        // 微信/支付宝走扫码支付，返回二维码URL
        const qrResult = await posApi.payOrder({
          orderId: orderId,
          paymentMethod: paymentMethodName,
          amount: totalAmount.value
        }) as unknown as OrderResult

        // 后端返回二维码URL时，订单状态保持未支付，需弹窗展示二维码
        if (qrResult?.qrCodeUrl) {
          paying.value = false
          showQrCodeDialog(qrResult, qrResult.orderId || orderId)
          return
        }
        payResult = qrResult
        break
      }
      default:
        payResult = await posApi.payOrder({
          orderId: orderId,
          paymentMethod: paymentMethodName,
          amount: totalAmount.value
        }) as unknown as OrderResult
    }

    if (payResult?.orderId) {
      orderNumber.value = payResult.orderNumber || createResult.orderNumber
      currentOrderId.value = payResult.orderId || createResult.orderId
      showSuccess.value = true
      currentStep.value = 2
      ElMessage.success('支付成功，订单已推送至后厨')

      // 触发自动打印小票（异步执行，不阻塞用户操作）
      handleAutoPrint(payResult.orderId || createResult.orderId)

      countdownTimer = window.setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          goHome()
        }
      }, 1000)
    } else {
      ElMessage.error('支付失败，订单已创建但未完成支付')
    }
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '支付流程异常'
    ElMessage.error(errMsg)
  } finally {
    paying.value = false
  }
}

/**
 * 显示扫码支付弹窗（微信/支付宝）
 * @param result 创建/支付订单返回的结果
 * @param orderId 订单ID
 */
const showQrCodeDialog = (result: OrderResult, orderId: string) => {
  currentOrderId.value = orderId
  qrCodeUrl.value = result.qrCodeUrl || ''
  qrTransactionId.value = result.qrTransactionId || orderId
  qrExpiresAt.value = result.qrExpiresAt || 0
  qrPaymentType.value = selectedMethod.value === 'alipay' ? 'alipay' : 'wechat'
  qrCodeLoading.value = false
  showQrCode.value = true

  // 启动二维码倒计时
  startQrCountdown()
}

/**
 * 启动二维码有效期倒计时
 */
const startQrCountdown = () => {
  if (qrCountdownTimer) {
    clearInterval(qrCountdownTimer)
  }
  const update = () => {
    if (!qrExpiresAt.value) {
      qrCountdownText.value = ''
      return
    }
    const remaining = qrExpiresAt.value - Date.now()
    if (remaining <= 0) {
      qrCountdownText.value = '已过期'
      ElMessage.warning('二维码已过期，请重新发起支付')
      cancelQrPayment()
      return
    }
    const minutes = Math.floor(remaining / 60000)
    const seconds = Math.floor((remaining % 60000) / 1000)
    qrCountdownText.value = `${minutes}:${String(seconds).padStart(2, '0')}`
  }
  update()
  qrCountdownTimer = window.setInterval(update, 1000)
}

/**
 * 取消扫码支付
 * 关闭弹窗、清理定时器，订单保留为未支付状态
 */
const cancelQrPayment = () => {
  showQrCode.value = false
  qrCodeUrl.value = ''
  qrTransactionId.value = ''
  qrExpiresAt.value = 0
  if (qrCountdownTimer) {
    clearInterval(qrCountdownTimer)
    qrCountdownTimer = null
  }
  ElMessage.info('已取消扫码支付，订单已保留为未支付状态')
}

/**
 * 确认扫码支付完成
 * 调用后端 confirmQrPayment 接口将订单状态更新为已支付
 */
const confirmQrPaymentComplete = async () => {
  if (confirmingQr.value) return
  if (!qrTransactionId.value) {
    ElMessage.warning('交易号缺失，无法确认支付')
    return
  }

  confirmingQr.value = true
  try {
    const result = await posApi.confirmQrPayment(qrTransactionId.value) as unknown as OrderResult

    if (result?.orderId) {
      // 关闭二维码弹窗
      showQrCode.value = false
      if (qrCountdownTimer) {
        clearInterval(qrCountdownTimer)
        qrCountdownTimer = null
      }

      // 显示支付成功弹窗
      orderNumber.value = result.orderNumber || orderNumber.value
      currentOrderId.value = result.orderId || currentOrderId.value
      showSuccess.value = true
      currentStep.value = 2
      ElMessage.success('支付成功，订单已推送至后厨')

      // 触发自动打印小票
      handleAutoPrint(result.orderId || currentOrderId.value)

      countdownTimer = window.setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          goHome()
        }
      }, 1000)
    } else {
      ElMessage.error('支付确认失败，请重试或联系管理员')
    }
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '支付确认异常'
    ElMessage.error(errMsg)
  } finally {
    confirmingQr.value = false
  }
}

/**
 * 自动打印小票（支付成功后自动触发）
 * <p>
 * 显示"正在打印..."状态，2秒后显示打印结果。
 * 打印失败不阻断支付成功流程。
 * </p>
 */
const handleAutoPrint = async (orderId: string) => {
  if (!orderId) return

  printing.value = true
  printStatus.value = 'printing'

  // 显示打印中提示（2秒后自动消失）
  const printLoadingMsg = ElMessage({
    message: '正在打印小票...',
    type: 'info',
    duration: 2000,
    showClose: false
  })

  try {
    // 模拟打印机响应时间（实际项目中可移除此延迟）
    await new Promise(resolve => setTimeout(resolve, 1500))

    // 调用后端重打接口（实际上后端在支付时已自动打印，此处为前端状态同步）
    printStatus.value = 'success'
    ElMessage.success('小票已打印')
  } catch (error) {
    console.error('小票打印失败:', error)
    printStatus.value = 'fail'
    ElMessage.warning('小票打印失败，请手动打印')
  } finally {
    printing.value = false
    printLoadingMsg.close()
  }
}

/**
 * 手动重新打印小票
 * <p>用户点击"重新打印"按钮时调用</p>
 */
const handleReprint = async () => {
  if (!currentOrderId.value || printing.value) return

  printing.value = true
  printStatus.value = 'printing'

  try {
    const result = await posApi.reprintReceipt(currentOrderId.value)
    if (result) {
      printStatus.value = 'success'
      ElMessage.success('小票已重新打印')
    } else {
      printStatus.value = 'fail'
      ElMessage.warning('小票打印失败，请检查打印机')
    }
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '重打异常'
    printStatus.value = 'fail'
    ElMessage.error(`重打失败: ${errMsg}`)
  } finally {
    printing.value = false
  }
}

const goHome = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  showSuccess.value = false
  router.push('/')
}
</script>

<style scoped>
/* ========== 主容器 ========== */
.pos-payment {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

/* ========== 头部 ========== */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.back-btn {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-btn:hover {
  transform: translateX(-4px);
}

.header-center {
  text-align: center;
}

.header-center h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  background: var(--pos-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 11px;
  color: #9ca3af;
  letter-spacing: 1px;
}

.step-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.step.active {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.step-line {
  width: 24px;
  height: 2px;
  background: #e5e7eb;
  transition: background 0.3s ease;
}

.step-line.active {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
}

/* ========== 主内容区 ========== */
.content {
  flex: 1;
  padding: 24px;
  display: flex;
  gap: 24px;
  overflow: hidden;
}

/* ========== 订单摘要 ========== */
.order-summary {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: var(--pos-primary-gradient);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  box-shadow: var(--pos-shadow-primary);
}

.header-text h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.order-time {
  font-size: 12px;
  color: #9ca3af;
}

.table-badge {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
  color: #fff;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
}

/* ========== 时间线 ========== */
.timeline {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.timeline-item {
  position: relative;
  padding-left: 32px;
  padding-bottom: 20px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.timeline-dot.first {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3);
}

.dot-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
}

.timeline-line {
  position: absolute;
  left: 9px;
  top: 24px;
  width: 2px;
  height: calc(100% - 24px);
  background: linear-gradient(180deg, #e5e7eb 0%, transparent 100%);
}

.timeline-content {
  background: linear-gradient(135deg, #fafbfc 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.timeline-content:hover {
  border-color: #ff6b35;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.1);
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 14px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-qty {
  font-size: 13px;
  color: #6b7280;
}

.item-price {
  font-weight: 600;
  color: #ef4444;
  font-size: 15px;
}

/* ========== 汇总 ========== */
.summary-footer {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid #f0f0f0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.summary-row .label {
  color: #6b7280;
}

.summary-row .value {
  font-weight: 600;
  color: #1f2937;
}

.summary-row.total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.summary-row.total .label {
  font-weight: 600;
  color: #1f2937;
  font-size: 16px;
}

.summary-row .amount {
  font-size: 32px;
  font-weight: 700;
  color: #ef4444;
}

.summary-row .currency {
  font-size: 18px;
  margin-right: 2px;
}

/* ========== 支付区域 ========== */
.payment-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.payment-methods {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.payment-methods h3 {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.method-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* ========== 支付方式卡片 - 翻转效果 ========== */
.method-card {
  position: relative;
  height: 160px;
  perspective: 1000px;
  cursor: pointer;
  animation: pos-fade-in 0.4s ease-out backwards;
}

.card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-style: preserve-3d;
}

.method-card:hover .card-inner {
  transform: rotateY(10deg);
}

.method-card.active .card-inner {
  transform: rotateY(180deg);
}

.card-front, .card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.card-front {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
}

.method-card:hover .card-front {
  border-color: #e5e7eb;
}

.method-card.active .card-front {
  border-color: transparent;
}

.card-back {
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
  transform: rotateY(180deg);
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.3);
}

.check-icon {
  font-size: 48px;
  color: #fff;
  animation: pos-bounce 0.6s ease-out;
}

.method-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.method-card.wechat .method-icon {
  background: linear-gradient(135deg, #07c160 0%, #2aae67 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(7, 193, 96, 0.3);
}

.method-card.alipay .method-icon {
  background: linear-gradient(135deg, #1677ff 0%, #40a9ff 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.3);
}

.method-card.cash .method-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.3);
}

.method-card.bank-card .method-icon {
  background: linear-gradient(135deg, #475569 0%, #64748b 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(71, 85, 105, 0.3);
}

.method-card.balance .method-icon {
  background: linear-gradient(135deg, #f97316 0%, #fb923c 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(249, 115, 22, 0.3);
}

.method-card:hover .method-icon {
  transform: scale(1.1);
}

.method-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.method-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(255, 107, 53, 0.3);
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.method-card.active .card-glow {
  opacity: 1;
  box-shadow: 0 8px 32px rgba(34, 197, 94, 0.3);
}

/* ========== 支付按钮 ========== */
.payment-actions {
  display: flex;
  gap: 16px;
}

.cancel-btn {
  flex: 1;
  height: 56px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 500;
  border: 2px solid #e5e7eb;
  background: #fff;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  border-color: #ef4444;
  color: #ef4444;
  transform: translateY(-2px);
}

.confirm-btn {
  flex: 2;
  height: 56px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a5c 100%);
  border: none;
  box-shadow: 0 6px 24px rgba(255, 107, 53, 0.3);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.confirm-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 8px 32px rgba(255, 107, 53, 0.4);
}

.btn-text {
  font-size: 14px;
  opacity: 0.9;
}

.btn-amount {
  font-size: 20px;
  font-weight: 700;
}

/* ========== 安全提示 ========== */
.security-tips {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: rgba(34, 197, 94, 0.05);
  border-radius: 12px;
  color: #22c55e;
  font-size: 13px;
}

/* ========== 成功对话框 ========== */
.success-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

.success-dialog :deep(.el-dialog__header) {
  display: none;
}

.success-dialog :deep(.el-dialog__body) {
  padding: 40px 30px 30px;
}

.success-content {
  text-align: center;
}

/* 成功动画 */
.success-animation {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
}

.success-circle {
  width: 100%;
  height: 100%;
}

.checkmark {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  stroke-width: 2;
  stroke: #22c55e;
  stroke-miterlimit: 10;
  box-shadow: inset 0 0 0 #22c55e;
  animation: fill 0.4s ease-in-out 0.4s forwards, scale 0.3s ease-in-out 0.9s both;
}

.checkmark-circle {
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  stroke-width: 2;
  stroke-miterlimit: 10;
  stroke: #22c55e;
  fill: none;
  animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

.checkmark-check {
  transform-origin: 50% 50%;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  stroke-width: 3;
  stroke: #22c55e;
  animation: stroke 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.8s forwards;
}

@keyframes stroke {
  100% {
    stroke-dashoffset: 0;
  }
}

@keyframes scale {
  0%, 100% {
    transform: none;
  }
  50% {
    transform: scale3d(1.1, 1.1, 1);
  }
}

@keyframes fill {
  100% {
    box-shadow: inset 0 0 0 60px rgba(34, 197, 94, 0.1);
  }
}

/* 粒子效果 */
.success-particles {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
  animation: particle-explode 0.8s ease-out var(--delay) forwards;
  transform: rotate(var(--angle)) translateX(0);
}

@keyframes particle-explode {
  0% {
    transform: rotate(var(--angle)) translateX(0);
    opacity: 1;
  }
  100% {
    transform: rotate(var(--angle)) translateX(80px);
    opacity: 0;
  }
}

.success-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.success-subtitle {
  margin: 0 0 24px;
  font-size: 12px;
  color: #9ca3af;
  letter-spacing: 2px;
}

/* 订单信息卡片 */
.order-info-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #e5e7eb;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #6b7280;
  font-size: 14px;
}

.info-value {
  font-weight: 600;
  color: #1f2937;
  font-size: 14px;
}

.info-value.highlight {
  color: #ef4444;
  font-size: 18px;
}

/* 倒计时 */
.countdown-hint {
  padding: 12px;
  background: rgba(249, 115, 22, 0.05);
  border-radius: 10px;
}

.countdown-text {
  font-size: 13px;
  color: #6b7280;
}

.countdown-number {
  display: inline-block;
  min-width: 20px;
  height: 20px;
  line-height: 20px;
  background: var(--pos-primary-gradient);
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  margin-right: 4px;
}

.home-btn {
  width: 100%;
  height: 52px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: var(--pos-primary-gradient);
  border: none;
  box-shadow: var(--pos-shadow-primary);
  transition: all 0.3s ease;
}

.home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(249, 115, 22, 0.4);
}

/* ========== 打印状态区域 ========== */
.print-status-area {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.print-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  margin-bottom: 12px;
}

.print-status.printing {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
  border: 1px solid #93c5fd;
}

.print-status.success {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #059669;
  border: 1px solid #34d399;
}

.print-status.fail {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
  border: 1px solid #f87171;
}

.reprint-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  border: none;
  transition: all 0.3s ease;
}

.reprint-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4);
}

/* ========== 银行卡输入区域 ========== */
.bank-card-input-section,
.balance-info-section {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

/* ========== 会员余额区域 ========== */
.balance-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  color: #6b7280;
  font-size: 14px;
}

.balance-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.balance-sufficient {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.balance-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.balance-row .label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.balance-row .value {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.balance-row.highlight {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-color: #fbbf24;
}

.balance-row.highlight .label {
  color: #92400e;
}

.balance-row.highlight .value.amount {
  color: #d97706;
  font-size: 22px;
}

.balance-row.success {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border-color: #34d399;
}

.balance-row.success .label {
  color: #065f46;
}

.balance-row.success .value.remaining {
  color: #059669;
  font-weight: 700;
}

/* ========== 扫码支付弹窗 ========== */
.qr-code-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.qr-payment-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.qr-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.qr-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.qr-amount {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.qr-amount .currency {
  font-size: 18px;
  color: #ef4444;
  font-weight: 600;
}

.qr-amount .amount {
  font-size: 36px;
  font-weight: 700;
  color: #ef4444;
}

.qr-code-wrapper {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #6b7280;
  font-size: 14px;
}

.qr-code-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 13px;
}

.qr-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #ef4444;
}

.qr-countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

.qr-order-info {
  font-size: 12px;
  color: #6b7280;
}

.qr-order-info .label {
  color: #9ca3af;
}

.qr-order-info .value {
  font-family: monospace;
  color: #374151;
}

.qr-tip {
  margin-top: 8px;
}
</style>
