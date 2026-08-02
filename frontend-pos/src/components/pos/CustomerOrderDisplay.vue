<template>
  <div class="customer-display" :data-theme="isDark ? 'dark' : 'light'">
    <div class="display-header">
      <div class="brand-section">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12H15V22" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="brand-info">
          <h1 class="brand-name">{{ displayStore.storeName }}</h1>
          <p class="brand-slogan">感谢您的光临</p>
        </div>
      </div>
      <div class="header-status">
        <div class="status-badge" :class="{ 'is-processing': displayStore.isProcessing }">
          <span class="status-dot"></span>
          {{ displayStore.isProcessing ? '正在处理' : '等待中' }}
        </div>
      </div>
    </div>

    <div class="display-body">
      <!-- 左侧：订单详情 -->
      <div class="order-panel">
        <div v-if="displayStore.hasOrder && displayStore.currentOrder" class="order-content">
          <div class="order-title-row">
            <h2>当前订单</h2>
            <span class="order-number">#{{ displayStore.currentOrder.orderNumber }}</span>
          </div>

          <div class="order-items-table">
            <div class="table-header">
              <span class="col-name">品名</span>
              <span class="col-qty">数量</span>
              <span class="col-price">小计</span>
            </div>
            <div class="table-body">
              <div v-for="(item, index) in displayStore.currentOrder.items" :key="index" class="table-row">
                <span class="col-name item-name">{{ item.name }}</span>
                <span class="col-qty">x{{ item.quantity }}</span>
                <span class="col-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <div class="order-summary">
            <div class="summary-left">
              <span class="total-quantity">共 {{ displayStore.totalQuantity }} 件商品</span>
            </div>
            <div class="summary-right">
              <span class="amount-label">合计</span>
              <span class="amount-value">¥{{ displayStore.currentOrder.totalAmount.toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M16 11V7A4 4 0 008 7v4M5 9H19L18 21H6L5 9Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <p>等待新订单...</p>
        </div>
      </div>

      <!-- 右侧：叫号信息 + 广告区 -->
      <div class="side-panel">
        <div class="call-info-card" v-if="latestCallNumber">
          <div class="call-label">请取餐</div>
          <div class="call-number">{{ latestCallNumber.number }}</div>
          <div class="call-hint">请凭号码到窗口取餐</div>
        </div>

        <div class="promo-section">
          <div class="promo-item" v-for="(promo, index) in promoList" :key="index">
            <div class="promo-icon">{{ promo.icon }}</div>
            <span>{{ promo.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="display-footer">
      <div class="footer-time">{{ currentTime }}</div>
      <div class="footer-info">
        <span>本店支持微信/支付宝支付</span>
        <span class="divider">|</span>
        <span>如有问题请联系服务员</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useCustomerDisplayStore } from '@/stores/customer-display'

const displayStore = useCustomerDisplayStore()

const isDark = ref(document.documentElement.getAttribute('data-theme') === 'dark')

const currentTime = ref('')
let timer: ReturnType<typeof setInterval> | null = null
let cleanupStorageListener: (() => void) | null = null

const latestCallNumber = ref<{ number: string } | null>(null)

const promoList = [
  { icon: '🎉', text: '新用户立减5元' },
  { icon: '📱', text: '扫码关注享优惠' },
  { icon: '⭐', text: '好评返现活动进行中' }
]

const updateTime = (): void => {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)

  // 注册 localStorage 事件监听器，实现跨标签页数据同步
  cleanupStorageListener = displayStore.initStorageListener()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)

  // 移除 localStorage 事件监听器，防止内存泄漏
  if (cleanupStorageListener) {
    cleanupStorageListener()
    cleanupStorageListener = null
  }
})
</script>

<style scoped>
.customer-display {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #ea580c 0%, #f97316 50%, #fb923c 100%);
  color: #ffffff;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

[data-theme="dark"] .customer-display {
  background: linear-gradient(135deg, #1c1917 0%, #292524 50%, #1c1917 100%);
}

.display-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 32px; background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px); border-bottom: 1px solid rgba(255, 255, 255, 0.15); flex-shrink: 0;
}

.brand-section { display: flex; align-items: center; gap: 14px; }

.brand-logo {
  width: 44px; height: 44px; background: rgba(255, 255, 255, 0.2);
  border-radius: 10px; display: flex; align-items: center; justify-content: center;
}
.brand-logo svg { width: 26px; height: 26px; }

.brand-name { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 1px; }
.brand-slogan { margin: 2px 0 0; font-size: 13px; opacity: 0.8; }

.header-status .status-badge {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 8px 20px; border-radius: 20px; font-size: 14px; font-weight: 600;
  background: rgba(255, 255, 255, 0.15); letter-spacing: 1px;
}
.status-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #fbbf24;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
.status-badge.is-processing .status-dot { background: #34d399; animation: pulse-dot 0.8s ease-in-out infinite; }
@keyframes pulse-dot { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.3); } }

.display-body {
  flex: 1; display: flex; padding: 20px 28px; gap: 24px; overflow: hidden;
}

.order-panel {
  flex: 1; background: rgba(255, 255, 255, 0.95); border-radius: 16px;
  padding: 20px 24px; display: flex; flex-direction: column; color: #1f2937; overflow: hidden;
}
[data-theme="dark"] .order-panel { background: rgba(30, 27, 43, 0.95); color: #e5e7eb; }

.order-title-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 12px; border-bottom: 2px solid #f3f4f6;
}
[data-theme="dark"] .order-title-row { border-bottom-color: #374151; }
.order-title-row h2 { margin: 0; font-size: 18px; font-weight: 700; color: #ea580c; }
.order-number { font-size: 28px; font-weight: 800; color: #111827; font-family: 'Courier New', monospace; letter-spacing: 2px; }
[data-theme="dark"] .order-number { color: #f3f4f6; }

.order-items-table { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.table-header {
  display: grid; grid-template-columns: 1fr 80px 120px; padding: 8px 12px;
  background: #f8fafc; border-radius: 8px; font-size: 13px; font-weight: 600;
  color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px;
}
[data-theme="dark"] .table-header { background: #374151; color: #9ca3af; }

.table-body { flex: 1; overflow-y: auto; }
.table-row {
  display: grid; grid-template-columns: 1fr 80px 120px; padding: 12px;
  align-items: center; border-bottom: 1px solid #f3f4f6; transition: background 0.2s;
}
.table-row:hover { background: #fafafa; }
[data-theme="dark"] .table-row { border-bottom-color: #374151; }
[data-theme="dark"] .table-row:hover { background: rgba(234, 88, 12, 0.08); }

.item-name { font-size: 15px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.col-qty { text-align: center; font-size: 14px; color: #6b7280; font-family: 'SF Mono', Monaco, monospace; }
.col-price { text-align: right; font-size: 15px; font-weight: 600; color: #111827; font-family: 'SF Mono', Monaco, monospace; }

.order-summary {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 14px; margin-top: auto; border-top: 2px solid #e5e7eb;
}
[data-theme="dark"] .order-summary { border-top-color: #4b5563; }
.total-quantity { font-size: 14px; color: #6b7280; }
.amount-label { font-size: 14px; color: #6b7280; margin-right: 10px; }
.amount-value { font-size: 28px; font-weight: 800; color: #dc2626; font-family: 'SF Mono', Monaco, monospace; }

.empty-state {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #9ca3af; gap: 12px;
}
.empty-icon { width: 64px; height: 64px; opacity: 0.4; }
.empty-icon svg { width: 64px; height: 64px; }
.empty-state p { font-size: 18px; font-weight: 500; opacity: 0.7; }

.side-panel { width: 280px; display: flex; flex-direction: column; gap: 16px; flex-shrink: 0; }

.call-info-card {
  background: linear-gradient(135deg, #fbbf24, #f59e0b); border-radius: 16px;
  padding: 24px 20px; text-align: center; box-shadow: 0 8px 24px rgba(245, 158, 11, 0.3);
}
.call-label { font-size: 14px; font-weight: 600; color: #78350f; letter-spacing: 3px; margin-bottom: 6px; }
.call-number { font-size: 56px; font-weight: 900; color: #ffffff; line-height: 1.1; font-family: 'Courier New', monospace; text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15); }
.call-hint { font-size: 13px; color: #92400e; margin-top: 8px; font-weight: 500; }

.promo-section {
  flex: 1; background: rgba(255, 255, 255, 0.12); border-radius: 16px;
  padding: 18px; display: flex; flex-direction: column; gap: 12px;
}
.promo-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  background: rgba(255, 255, 255, 0.1); border-radius: 10px; font-size: 14px; transition: transform 0.2s;
}
.promo-item:hover { transform: translateX(4px); background: rgba(255, 255, 255, 0.18); }
.promo-icon { font-size: 22px; flex-shrink: 0; }

.display-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 32px; background: rgba(0, 0, 0, 0.15); font-size: 13px; flex-shrink: 0;
}
.footer-time { font-family: 'Courier New', monospace; font-size: 18px; font-weight: 600; letter-spacing: 1px; }
.footer-info { display: flex; align-items: center; gap: 8px; opacity: 0.75; font-size: 12px; }
.divider { opacity: 0.5; }
</style>
