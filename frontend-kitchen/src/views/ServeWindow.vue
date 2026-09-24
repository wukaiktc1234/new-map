<template>
  <div class="serve-window" @keyup="handleKeyPress">
    <header class="window-header">
      <div class="header-left">
        <div class="logo">
          <span class="logo-icon">🍽️</span>
          <div class="logo-text">
            <h1>出餐窗口</h1>
            <span class="subtitle">Serving Window</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <div class="scan-indicator" :class="{ scanning: isScanning }">
          <span class="scan-icon">📷</span>
          <span class="scan-status">{{ scanStatus }}</span>
        </div>
        <div class="time-display">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
        </div>
        <div class="connection-status" :class="{ connected: isConnected, disconnected: !isConnected }">
          <span class="status-dot" :class="{ online: isConnected, offline: !isConnected }"></span>
          <span>{{ isConnected ? '系统在线' : '连接断开' }}</span>
        </div>
      </div>
    </header>

    <main class="window-main">
      <div class="orders-container">
        <div class="list-header">
          <h3>待出餐订单</h3>
          <div class="order-stats">
            <span class="stat-item completed">已完成: {{ completedCount }}</span>
            <span class="stat-item serving">待出餐: {{ servingCount }}</span>
          </div>
        </div>
        <div class="orders-list">
          <div v-if="servingOrders.length === 0 && loadError" class="empty-state">
            <div class="empty-icon">⚠️</div>
            <p class="empty-error-text">待出餐列表加载失败：{{ loadError }}</p>
            <el-button type="primary" class="empty-retry-btn" @click="loadOrders">重试加载</el-button>
          </div>
          <div v-else-if="servingOrders.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            <p>暂无待出餐订单</p>
          </div>
          <div v-for="order in servingOrders" :key="order.kitchenOrderId" 
               class="order-card"
               :class="getOrderTypeClass(order.orderType)">
            <div class="order-header">
              <div class="order-header-left">
                <div class="order-type-badge" :class="getOrderTypeClass(order.orderType)">
                  <span class="type-icon">{{ getOrderTypeIcon(order.orderType) }}</span>
                  <span class="type-text">{{ getOrderTypeText(order.orderType) }}</span>
                </div>
                <div class="priority-badge" :class="getPriorityClass(order.priority)" v-if="order.priority !== null && order.priority > 0">
                  {{ getPriorityText(order.priority) }}
                </div>
              </div>
              <div class="order-id-section">
                <span class="order-number">{{ order.orderNumber }}</span>
                <span class="order-time">{{ formatTime(order.makeCompleteTime) }}</span>
              </div>
            </div>
            <div class="order-info">
              <span class="table-number" v-if="order.tableNumber">桌号: {{ order.tableNumber }}</span>
              <span class="dish-count">{{ order.totalDishes }} 道菜</span>
              <span class="wait-time">等待: {{ formatWaitTime(order.makeCompleteTime) }}</span>
            </div>
            <div class="dish-list" v-if="order.dishItems">
              <div class="dish-item" v-for="(dish, index) in parseDishItems(order.dishItems)" :key="index">
                <span>{{ dish.dishName || dish.name }} × {{ dish.quantity }}</span>
                <!-- P1-COMBO-ORDER-001: 套餐明细展开（单卡片） -->
                <ul v-if="dish.components && dish.components.length" class="combo-components">
                  <li v-for="(comp, ci) in dish.components" :key="ci">
                    {{ comp.name }} × {{ comp.quantity }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="order-remark" v-if="order.remark">
              <span class="remark-label">备注:</span>
              <span class="remark-text">{{ order.remark }}</span>
            </div>
            <div class="order-footer">
              <div class="order-actions">
                <el-button type="success" size="large" @click="serveOrder(order)">
                  ✅ 完成出餐
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <el-dialog
      v-model="scanResultDialogVisible"
      :title="scanResultTitle"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false">
      <div class="scan-result-content">
        <div class="scan-result-icon" :class="scanResultType">
          {{ scanResultIcon }}
        </div>
        <div class="scan-result-message">{{ scanResultMessage }}</div>
        <div v-if="scanResultDetails" class="scan-result-details">
          <div v-for="(value, key) in scanResultDetails" :key="key" class="detail-item">
            <span class="detail-label">{{ key }}:</span>
            <span class="detail-value">{{ value }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="scanResultDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { webSocketService } from '@/utils/websocket'
import request from '@/api/request'

const currentTime = ref('')
const currentDate = ref('')
const isConnected = ref(false)
const completedOrders = ref<any[]>([])
const servingOrders = ref<any[]>([])
/** 待出餐列表加载错误信息（失败透传：非空时展示错误态+重试，不伪装「暂无待出餐订单」空态） */
const loadError = ref<string | null>(null)
let timeInterval: number | null = null
let wsSubscriptionStatus: string | null = null

let scanBuffer = ''
let scanTimeout: number | null = null
const isScanning = ref(false)
const scanStatus = ref('等待扫码')

const scanResultDialogVisible = ref(false)
const scanResultTitle = ref('扫码结果')
const scanResultType = ref('success')
const scanResultIcon = ref('✅')
const scanResultMessage = ref('')
const scanResultDetails = ref<Record<string, any> | null>(null)

const completedCount = computed(() => completedOrders.value.length)
const servingCount = computed(() => servingOrders.value.length)

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatWaitTime = (timeStr: string) => {
  if (!timeStr) return ''
  const startTime = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - startTime.getTime()
  const minutes = Math.floor(diff / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)
  return `${minutes}分${seconds}秒`
}

const parseDishItems = (dishItemsStr: string) => {
  try {
    return JSON.parse(dishItemsStr)
  } catch (e) {
    return []
  }
}

const getOrderTypeIcon = (orderType: number) => {
  const icons: Record<number, string> = {
    0: '🍴',
    1: '🛵',
    2: '📦'
  }
  return icons[orderType] || '❓'
}

const getOrderTypeText = (orderType: number) => {
  const types: Record<number, string> = {
    0: '堂食',
    1: '外卖',
    2: '自提'
  }
  return types[orderType] || ''
}

const getPriorityText = (priority: number) => {
  const texts: Record<number, string> = {
    0: '',
    1: '加急',
    2: '特急'
  }
  return texts[priority] || ''
}

const getPriorityClass = (priority: number) => {
  const classes: Record<number, string> = {
    0: '',
    1: 'special',
    2: 'urgent'
  }
  return classes[priority] || ''
}

const getOrderTypeClass = (orderType: number) => {
  const classes: Record<number, string> = {
    0: 'dine-in',
    1: 'takeaway',
    2: 'pickup'
  }
  return classes[orderType] || ''
}

const loadOrders = async () => {
  try {
    // GET /v1/kitchen/completed-orders：后端无此端点（KIT-API-003，缺端点已立项归后端池）——
    // 契约未定时前端只做失败透传，路径不动、不发明路径（OIC2-B2-003）
    const res = await request.get('/v1/kitchen/completed-orders')
    
    if ((res as any).success) {
      const orders = (res as any).data || []
      servingOrders.value = orders
    }
    loadError.value = null
  } catch (error: any) {
    console.error('加载订单失败:', error)
    // 失败透传：明确提示 + 重试入口（错误态替代「暂无待出餐订单」伪装空数据）
    // 首次失败弹 toast，后续失败仅错误态常驻，避免 toast 轰炸
    const msg = error?.message || '加载待出餐订单失败'
    if (!loadError.value) {
      ElMessage.error(`待出餐列表加载失败：${msg}`)
    }
    loadError.value = msg
  }
}

const serveOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要完成订单 ${order.orderNumber} 的出餐吗？`,
      '确认出餐',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
    
    const result = await request.post(`/v1/kitchen/serve-order/${order.kitchenOrderId}`)
    
    if ((result as any).success) {
      ElMessage.success('出餐成功')
      await loadOrders()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const handleKeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    if (scanBuffer.length > 0) {
      processScan(scanBuffer)
      scanBuffer = ''
      isScanning.value = false
      scanStatus.value = '等待扫码'
    }
  } else {
    if (!isScanning.value) {
      isScanning.value = true
      scanStatus.value = '正在扫码...'
    }
    scanBuffer += event.key
    
    if (scanTimeout) {
      clearTimeout(scanTimeout)
    }
    scanTimeout = window.setTimeout(() => {
      if (scanBuffer.length > 0) {
        scanBuffer = ''
        isScanning.value = false
        scanStatus.value = '等待扫码'
      }
    }, 500)
  }
}

const processScan = async (traceCode: string) => {
  console.log('处理扫码:', traceCode)
  
  try {
    // 路径对齐后端实测 @RequestMapping（KitchenScanController:78）：/v1/kitchen/scan/scan-food-trace-code
    const result = await request.post('/v1/kitchen/scan/scan-food-trace-code', null, {
      params: { traceCode }
    })
    
    if ((result as any).success) {
      const data = (result as any).data
      showScanResult(
        'success',
        '✅',
        data.message || '出餐成功',
        {
          '订单号': data.kitchenOrder?.orderNumber,
          '菜品': data.foodTraceCode?.dishName,
          '操作': '已出餐'
        }
      )
      await loadOrders()
    } else {
      showScanResult(
        'error',
        '❌',
        (result as any).message || '扫码失败',
        { '追溯码': traceCode }
      )
    }
  } catch (error: any) {
    console.error('扫码处理失败:', error)
    showScanResult(
      'error',
      '❌',
      error.message || '扫码处理失败',
      { '追溯码': traceCode }
    )
  }
}

const showScanResult = (
  type: 'success' | 'error' | 'warning',
  icon: string,
  message: string,
  details?: Record<string, any>
) => {
  scanResultType.value = type
  scanResultIcon.value = icon
  scanResultMessage.value = message
  scanResultDetails.value = details || null
  scanResultTitle.value = type === 'success' ? '出餐成功' : '扫码失败'
  scanResultDialogVisible.value = true
}

const handleOrderStatusChange = (message: any) => {
  console.log('订单状态变更:', message)
  
  if (message.status === 'completed') {
    const existingIndex = servingOrders.value.findIndex(o => o.kitchenOrderId === message.kitchenOrderId)
    if (existingIndex === -1) {
      servingOrders.value.unshift(message)
    }
  } else if (message.status === 'served') {
    const index = servingOrders.value.findIndex(o => o.kitchenOrderId === message.kitchenOrderId)
    if (index > -1) {
      const order = servingOrders.value.splice(index, 1)[0]
      order.status = 'served'
      completedOrders.value.unshift(order)
    }
  }
}

onMounted(async () => {
  updateTime()
  timeInterval = window.setInterval(updateTime, 1000)
  
  await loadOrders()
  
  try {
    await webSocketService.connect()
    isConnected.value = true
    
    wsSubscriptionStatus = await webSocketService.subscribeToOrderStatusChanges(handleOrderStatusChange)
    
    console.log('WebSocket连接成功')
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    isConnected.value = false
  }
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (scanTimeout) clearTimeout(scanTimeout)
  
  if (wsSubscriptionStatus) {
    webSocketService.unsubscribe(wsSubscriptionStatus)
  }
  
  webSocketService.disconnect()
})
</script>

<style scoped>
.serve-window {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.window-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}

.header-left .logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #22c55e 0%, #4ade80 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.logo-text h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
}

.logo-text .subtitle {
  font-size: 11px;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.scan-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
  transition: all 0.3s ease;
}

.scan-indicator.scanning {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  animation: pulse 1s ease-in-out infinite;
}

.scan-icon {
  font-size: 16px;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.time-display {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.time-display .date {
  font-size: 12px;
  color: #64748b;
}

.time-display .time {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  font-variant-numeric: tabular-numeric;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
}

.connection-status.connected {
  background: rgba(34, 197, 94, 0.1);
  color: #4ade80;
}

.connection-status.disconnected {
  background: rgba(239, 68, 68, 0.1);
  color: #f87171;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #22c55e;
  animation: pulse 2s ease-in-out infinite;
}

.status-dot.offline {
  background: #ef4444;
}

.window-main {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.orders-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1e293b;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #334155;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #334155;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
}

.order-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.stat-item.completed {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.stat-item.serving {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.orders-list {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  align-content: start;
}

.orders-list::-webkit-scrollbar {
  width: 8px;
}

.orders-list::-webkit-scrollbar-track {
  background: #0f172a;
  border-radius: 4px;
}

.orders-list::-webkit-scrollbar-thumb {
  background: #475569;
  border-radius: 4px;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.empty-error-text {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #f87171;
  text-align: center;
}

.empty-retry-btn {
  margin-top: 4px;
}

.order-card {
  background: #0f172a;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.order-card.dine-in {
  border-color: #22c55e;
}

.order-card.takeaway {
  border-color: #3b82f6;
}

.order-card.pickup {
  border-color: #a855f7;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.order-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.order-type-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
}

.order-type-badge.dine-in {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.order-type-badge.takeaway {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.order-type-badge.pickup {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

.type-icon {
  font-size: 14px;
}

.priority-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.priority-badge.urgent {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  animation: pulse-red 1.5s ease-in-out infinite;
}

.priority-badge.special {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.order-id-section {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.order-number {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.order-time, .wait-time {
  font-size: 12px;
  color: #94a3b8;
}

.order-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.table-number, .dish-count, .wait-time {
  font-size: 13px;
  color: #94a3b8;
  padding: 2px 8px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 6px;
}

.dish-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dish-item {
  font-size: 13px;
  color: #cbd5e1;
  padding: 4px 10px;
  background: rgba(100, 116, 139, 0.15);
  border-radius: 6px;
}

.combo-components {
  margin: 4px 0 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
  padding-top: 4px;
}

.order-remark {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 8px 12px;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 6px;
}

.remark-label {
  color: #fbbf24;
  font-weight: 500;
}

.remark-text {
  color: #fbbf24;
}

.order-footer {
  display: flex;
  justify-content: center;
  padding-top: 8px;
  border-top: 1px solid #334155;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.scan-result-content {
  text-align: center;
  padding: 20px;
}

.scan-result-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.scan-result-icon.success {
  color: #22c55e;
}

.scan-result-icon.error {
  color: #ef4444;
}

.scan-result-message {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #1e293b;
}

.scan-result-details {
  text-align: left;
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e2e8f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-weight: 500;
  color: #64748b;
}

.detail-value {
  color: #1e293b;
}
</style>
