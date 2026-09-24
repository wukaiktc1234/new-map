<template>
  <div class="kitchen-terminal" @keyup="handleKeyPress">
    <header class="terminal-header">
      <div class="header-left">
        <div class="logo">
          <span class="logo-icon">🍳</span>
          <div class="logo-text">
            <h1>后厨制作终端</h1>
            <span class="subtitle">Kitchen Terminal</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button class="scan-entry-btn" @click="goToScanPage">
          <span class="scan-icon">📷</span>
          <span>摄像头扫码</span>
        </el-button>
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

    <main class="terminal-main">
      <div class="orders-list-container">
        <div class="list-header">
          <h3>所有订单</h3>
          <div class="order-stats">
            <span class="stat-item pending">待制作: {{ pendingCount }}</span>
            <span class="stat-item making">制作中: {{ makingCount }}</span>
            <span class="stat-item completed">已完成: {{ completedCount }}</span>
          </div>
        </div>
        <div class="orders-list">
          <div v-if="allOrders.length === 0 && loadError" class="empty-state">
            <div class="empty-icon">⚠️</div>
            <p class="empty-error-text">订单加载失败：{{ loadError }}</p>
            <el-button type="primary" class="empty-retry-btn" @click="loadOrders">重试加载</el-button>
          </div>
          <div v-else-if="allOrders.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            <p>暂无订单</p>
          </div>
          <div v-for="order in allOrders" :key="order.kitchenOrderId" 
               class="order-card"
               :class="order.status">
            <div class="priority-stamp" :class="getPriorityClass(order.priority)" v-if="order.priority !== null && order.priority > 0">
              {{ getPriorityText(order.priority) }}
            </div>
            <div class="order-header">
              <div class="order-header-left">
                <div class="order-status-badge" :class="order.status">
                  <span class="status-icon">{{ getStatusIcon(order.status) }}</span>
                  <span class="status-text">{{ getStatusText(order.status) }}</span>
                </div>
              </div>
              <div class="order-id-section">
                <span class="order-number">{{ order.orderNumber }}</span>
                <span class="order-time">{{ formatTime(order.createTime) }}</span>
              </div>
            </div>
            <div class="order-info">
              <span class="table-number" v-if="order.tableNumber">桌号: {{ order.tableNumber }}</span>
              <span class="order-type" 
                    :class="getOrderTypeClass(order.orderType)" 
                    v-if="order.orderType !== null">
                {{ getOrderTypeText(order.orderType) }}
              </span>
              <span class="dish-count">{{ order.totalDishes }} 道菜</span>
              <span class="chef-name" v-if="order.chefName">厨师: {{ order.chefName }}</span>
            </div>
            <div class="dish-list" v-if="order.dishItems">
              <div class="dish-item" v-for="(dish, index) in parseDishItems(order.dishItems)" :key="index">
                <span>{{ dish.name }} × {{ dish.quantity }}</span>
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
              <span class="making-time" v-if="order.status === 'making' && order.makeStartTime">
                已制作: {{ formatDuration(order.makeStartTime) }}
              </span>
              <span class="complete-time" v-if="order.status === 'completed' && order.makeCompleteTime">
                完成时间: {{ formatTime(order.makeCompleteTime) }}
              </span>
              <span class="tray-info" v-if="order.trayCode">
                托盘: {{ order.trayCode }}
              </span>
              <div class="order-actions">
                <el-button v-if="!order.trayCode && (order.status === 'pending' || order.status === 'making')" 
                           type="primary" 
                           size="small" 
                           @click="showBindTrayDialog(order)">
                  绑定托盘
                </el-button>
                <el-button v-if="order.status === 'making'" 
                           type="success" 
                           size="small" 
                           @click="completeOrder(order)">
                  完成制作
                </el-button>
                <el-button v-if="order.trayCode && order.status === 'completed'" 
                           type="warning" 
                           size="small" 
                           @click="serveOrder(order)">
                  出餐
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

    <el-dialog
      v-model="bindTrayDialogVisible"
      title="绑定托盘"
      width="400px"
      :close-on-click-modal="false">
      <div class="bind-tray-content">
        <div class="order-info-box">
          <div class="info-row">
            <span class="label">订单号：</span>
            <span class="value">{{ currentBindOrder?.orderNumber }}</span>
          </div>
          <div class="info-row">
            <span class="label">菜品：</span>
            <span class="value">{{ parseDishItems(currentBindOrder?.dishItems || '[]').map((d: any) => d.name).join('、') }}</span>
          </div>
        </div>
        <div class="tray-input-section">
          <p class="tip">请扫描托盘底部二维码，或手动输入托盘编号</p>
          <el-input
            ref="trayInputRef"
            v-model="trayCodeInput"
            placeholder="扫描或输入托盘编号（如：TRAY001）"
            size="large"
            @keyup.enter="bindTray"
            clearable
          />
        </div>
        <div class="idle-trays" v-if="idleTrays.length > 0">
          <p class="idle-title">空闲托盘：</p>
          <div class="tray-tags">
            <el-tag 
              v-for="tray in idleTrays" 
              :key="tray.trayCode"
              :type="trayCodeInput === tray.trayCode ? 'primary' : 'info'"
              @click="trayCodeInput = tray.trayCode"
              style="cursor: pointer; margin: 4px;"
            >
              {{ tray.trayCode }}
            </el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="bindTrayDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="bindTray" :loading="binding">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { webSocketService } from '@/utils/websocket'
import request from '@/api/request'

const router = useRouter()

const currentTime = ref('')
const currentDate = ref('')
const isConnected = ref(false)
const pendingOrders = ref<any[]>([])
const makingOrders = ref<any[]>([])
const completedOrders = ref<any[]>([])
/** 订单加载错误信息（失败透传：非空时展示错误态+重试，不伪装「暂无订单」空态） */
const loadError = ref<string | null>(null)
let timeInterval: number | null = null
let refreshInterval: number | null = null
let wsSubscriptionNew: string | null = null
let wsSubscriptionStatus: string | null = null
let wsSubscriptionRefund: string | null = null

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

const bindTrayDialogVisible = ref(false)
const currentBindOrder = ref<any>(null)
const trayCodeInput = ref('')
const idleTrays = ref<any[]>([])
const binding = ref(false)
const trayInputRef = ref<any>(null)

const allOrders = computed(() => {
  return [...pendingOrders.value, ...makingOrders.value, ...completedOrders.value]
})

const pendingCount = computed(() => pendingOrders.value.length)
const makingCount = computed(() => makingOrders.value.length)
const completedCount = computed(() => completedOrders.value.length)

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

const formatDuration = (startTimeStr: string) => {
  if (!startTimeStr) return ''
  const startTime = new Date(startTimeStr)
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

const getStatusIcon = (status: string) => {
  const icons: Record<string, string> = {
    pending: '⏳',
    making: '👨‍🍳',
    completed: '✅',
    served: '🍽️'
  }
  return icons[status] || '❓'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    pending: '待制作',
    making: '制作中',
    completed: '已完成',
    served: '已出餐'
  }
  return texts[status] || status
}

const getOrderTypeText = (orderType: number) => {
  const types: Record<number, string> = {
    0: '堂食',
    1: '外卖',
    2: '自提'
  }
  return types[orderType] || ''
}

const getOrderTypeClass = (orderType: number) => {
  const classes: Record<number, string> = {
    0: 'dine-in',
    1: 'takeaway',
    2: 'pickup'
  }
  return classes[orderType] || ''
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

const loadOrders = async () => {
  try {
    console.log('后厨端开始加载订单...')
    // 路径对齐后端实测 @RequestMapping（KitchenScanController: /v1/kitchen/scan）：
    // /v1/kitchen/scan/pending-orders、/v1/kitchen/scan/making-orders（契约核对见 OIC2-B2-003）
    const [pendingRes, makingRes] = await Promise.all([
      request.get('/v1/kitchen/scan/pending-orders'),
      request.get('/v1/kitchen/scan/making-orders')
    ])
    
    if (Array.isArray(pendingRes)) {
      pendingOrders.value = pendingRes
    }
    
    if (Array.isArray(makingRes)) {
      makingOrders.value = makingRes
    }
    loadError.value = null
  } catch (error: any) {
    console.error('加载订单失败:', error)
    // 失败透传：明确提示 + 重试入口（错误态替代「暂无订单」伪装空数据）
    // 首次失败弹 toast，自动刷新轮询期间失败仅错误态常驻，避免 toast 轰炸
    const msg = error?.message || '加载订单失败'
    if (!loadError.value) {
      ElMessage.error(`订单加载失败：${msg}`)
    }
    loadError.value = msg
  }
}

const goToScanPage = () => {
  router.push('/scan')
}

const showBindTrayDialog = async (order: any) => {
  currentBindOrder.value = order
  trayCodeInput.value = ''
  bindTrayDialogVisible.value = true
  
  try {
    const res = await request.get('/v1/tray/idle', { params: { limit: 10 } })
    if (Array.isArray(res)) {
      idleTrays.value = res
    }
  } catch (e) {
    console.error('获取空闲托盘失败:', e)
    ElMessage.warning('获取空闲托盘列表失败，可手动输入托盘编号')
  }
  
  setTimeout(() => {
    trayInputRef.value?.focus()
  }, 100)
}

const bindTray = async () => {
  if (!trayCodeInput.value) {
    ElMessage.warning('请输入或扫描托盘编号')
    return
  }
  
  binding.value = true
  try {
    const res = await request.post('/v1/tray/bind-order', {
      trayCode: trayCodeInput.value,
      orderId: currentBindOrder.value.orderId,
      kitchenOrderId: currentBindOrder.value.id
    })
    
    if ((res as any).success !== false) {
      ElMessage.success(`托盘 ${trayCodeInput.value} 已绑定订单 ${currentBindOrder.value.orderNumber}`)
      bindTrayDialogVisible.value = false
      await loadOrders()
    } else {
      ElMessage.error((res as any).message || '绑定失败')
    }
  } catch (e: any) {
    console.error('绑定托盘失败:', e)
    ElMessage.error(e.message || '绑定失败')
  } finally {
    binding.value = false
  }
}

const serveOrder = async (order: any) => {
  if (!order.trayCode) {
    ElMessage.warning('订单未绑定托盘')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要出餐订单 ${order.orderNumber} 吗？`,
      '确认出餐',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    const res = await request.post('/v1/tray/scan-serve', null, {
      params: { trayCode: order.trayCode }
    })
    
    if ((res as any).success !== false) {
      ElMessage.success(`订单 ${order.orderNumber} 已出餐`)
      await loadOrders()
    } else {
      ElMessage.error((res as any).message || '出餐失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('出餐失败:', e)
      ElMessage.error(e.message || '出餐失败')
    }
  }
}

const completeOrder = async (order: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要完成订单 ${order.orderNumber} 的制作吗？`,
      '确认完成',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const kitchenOrderId = order.kitchenOrderId
    console.log('[后厨端] 完成订单:', kitchenOrderId, '当前状态:', order.status)
    
    if (order.status === 'pending') {
      // 路径对齐后端实测 @RequestMapping（KitchenOrderController: /v1/kitchen/orders）：
      // POST /v1/kitchen/orders/{id}/receive（chefId/chefName 参数沿用既有调用，chefId=1 硬编码属 KIT-DATA-003 只登记不修）
      const receiveResult = await request.post(`/v1/kitchen/orders/${kitchenOrderId}/receive?chefId=1&chefName=后厨`)
      console.log('[后厨端] 自动接单结果:', receiveResult)
      if (receiveResult !== true) {
        ElMessage.error('接单失败')
        return
      }
    }
    
    if (order.status === 'pending' || order.status === 'received') {
      const startResult = await request.post(`/v1/kitchen/orders/${kitchenOrderId}/start-make`)
      console.log('[后厨端] 自动开始制作结果:', startResult)
      if (startResult !== true) {
        ElMessage.error('开始制作失败')
        return
      }
    }
    
    const result = await request.post(`/v1/kitchen/orders/${kitchenOrderId}/complete-make`)
    console.log('[后厨端] 完成订单结果:', result)
    
    if (result === true) {
      ElMessage.success(`订单 ${order.orderNumber} 已完成，已发送取餐通知`)
      await loadOrders()
    } else {
      ElMessage.error((result as any).message || '完成订单失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('[后厨端] 完成订单失败:', error)
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
    let result: any
    
    const isTrayCode = traceCode.toUpperCase().startsWith('TRAY')
    const isFoodTraceCode = traceCode.startsWith('FTC') || traceCode.length > 20
    
    if (isTrayCode) {
      if (currentBindOrder.value && bindTrayDialogVisible.value) {
        trayCodeInput.value = traceCode.toUpperCase()
        await bindTray()
        return
      }
      
      result = await request.post('/v1/tray/scan-serve', null, {
        params: { trayCode: traceCode.toUpperCase() }
      })
      
      if ((result as any).success !== false) {
        const data = (result as any).data
        showScanResult(
          'success',
          '✅',
          '出餐成功',
          {
            '托盘码': traceCode.toUpperCase(),
            '订单号': data?.orderNumber,
            '出餐时间': data?.serveTime ? new Date(data.serveTime).toLocaleTimeString('zh-CN') : '-'
          }
        )
        await loadOrders()
      } else {
        showScanResult(
          'error',
          '❌',
          (result as any).message || '出餐失败',
          { '托盘码': traceCode }
        )
      }
    } else if (isFoodTraceCode) {
      result = await request.post('/v1/kitchen/pre-make/scan-serve', null, {
        params: { traceCode }
      })
      
      if ((result as any).success) {
        const data = (result as any).data
        if (data.action === 'no_order') {
          showScanResult(
            'warning',
            '⚠️',
            data.message || '食品码有效，但无匹配订单',
            {
              '食品追溯码': traceCode,
              '菜品': data.foodTraceCode?.dishName,
              '状态': '等待订单'
            }
          )
        } else {
          showScanResult(
            'success',
            '✅',
            data.message || '出餐成功',
            {
              '订单号': data.kitchenOrder?.orderNumber,
              '菜品': data.foodTraceCode?.dishName,
              '出餐时间': data.serveTime ? new Date(data.serveTime).toLocaleTimeString('zh-CN') : '-'
            }
          )
          await loadOrders()
        }
      } else {
        showScanResult(
          'error',
          '❌',
          (result as any).message || '扫码失败',
          { '追溯码': traceCode }
        )
      }
    } else {
      result = await request.post('/v1/kitchen/scan', null, {
        params: { traceCode }
      })
      
      if ((result as any).success) {
        const data = (result as any).data
        let message = '原料使用记录成功'
        if (data.triggeredComplete) {
          message = '🎉 所有原料已扫描，订单自动完成制作！'
        } else if (data.triggeredMaking) {
          message = '原料已记录，订单开始制作'
        }
        
        showScanResult(
          'success',
          data.triggeredComplete ? '🎉' : '✅',
          message,
          {
            '原料': data.materialName,
            '使用数量': `${data.usedQuantity} ${data.unit}`,
            '匹配订单': data.orderNumber,
            '菜品': data.dishName,
            '订单状态': data.triggeredComplete ? '已自动完成制作' : (data.triggeredMaking ? '已自动开始制作' : '等待更多原料')
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
  scanResultTitle.value = type === 'success' ? '扫码成功' : '扫码失败'
  scanResultDialogVisible.value = true
}

const handleNewOrder = (message: any) => {
  console.log('收到新订单:', message)
  ElMessage.success(`收到新订单: ${message.orderNumber}`)
  loadOrders()
}

const handleOrderStatusChange = (message: any) => {
  console.log('订单状态变更:', message)
  
  if (message.status === 'making') {
    const index = pendingOrders.value.findIndex(o => o.kitchenOrderId === message.kitchenOrderId)
    if (index > -1) {
      const order = pendingOrders.value.splice(index, 1)[0]
      order.status = 'making'
      order.makeStartTime = message.notificationTime
      order.chefName = message.chefName
      makingOrders.value.unshift(order)
    }
  } else if (message.status === 'completed') {
    const index = makingOrders.value.findIndex(o => o.kitchenOrderId === message.kitchenOrderId)
    if (index > -1) {
      const order = makingOrders.value.splice(index, 1)[0]
      order.status = 'completed'
      order.makeCompleteTime = message.notificationTime
      completedOrders.value.unshift(order)
    }
  }
}

const handleOrderRefund = (message: any) => {
  console.log('收到退款通知:', message)
  
  ElMessage.warning(`订单 ${message.orderNumber} 已退款`)
  
  // 从待制作列表移除
  const pendingIndex = pendingOrders.value.findIndex(o => o.kitchenOrderId === message.kitchenOrderId)
  if (pendingIndex > -1) {
    pendingOrders.value.splice(pendingIndex, 1)
  }
  
  // 从制作中列表移除
  const makingIndex = makingOrders.value.findIndex(o => o.kitchenOrderId === message.kitchenOrderId)
  if (makingIndex > -1) {
    makingOrders.value.splice(makingIndex, 1)
  }
}

onMounted(async () => {
  updateTime()
  timeInterval = window.setInterval(updateTime, 1000)
  
  await loadOrders()
  
  // 设置WebSocket连接状态回调
  webSocketService.setOnConnectionChange((connected) => {
    isConnected.value = connected
    if (connected) {
      console.log('WebSocket已连接，切换到低频刷新')
      startRefreshTimer(60000)  // 1分钟备用刷新
    } else {
      console.log('WebSocket已断开，切换到高频刷新')
      startRefreshTimer(5000)   // 5秒轮询
    }
  })
  
  try {
    await webSocketService.connect()
    isConnected.value = true
    
    wsSubscriptionNew = await webSocketService.subscribeToNewOrders(handleNewOrder)
    wsSubscriptionStatus = await webSocketService.subscribeToOrderStatusChanges(handleOrderStatusChange)
    wsSubscriptionRefund = await webSocketService.subscribeToOrderRefund(handleOrderRefund)
    
    console.log('WebSocket连接成功')
    startRefreshTimer(60000)  // WebSocket正常时，1分钟备用刷新
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    isConnected.value = false
    startRefreshTimer(5000)   // WebSocket失败时，5秒轮询
  }
})

const startRefreshTimer = (interval: number) => {
  if (refreshInterval) clearInterval(refreshInterval)
  refreshInterval = window.setInterval(async () => {
    if (!isConnected.value) {
      console.log('WebSocket断开，自动刷新订单...')
    }
    await loadOrders()
  }, interval)
}

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  if (refreshInterval) clearInterval(refreshInterval)
  if (scanTimeout) clearTimeout(scanTimeout)
  
  if (wsSubscriptionNew) {
    webSocketService.unsubscribe(wsSubscriptionNew)
  }
  if (wsSubscriptionStatus) {
    webSocketService.unsubscribe(wsSubscriptionStatus)
  }
  if (wsSubscriptionRefund) {
    webSocketService.unsubscribe(wsSubscriptionRefund)
  }
  
  webSocketService.disconnect()
})
</script>

<style scoped>
.kitchen-terminal {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.terminal-header {
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
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
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

.scan-entry-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  border: none;
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.scan-entry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.scan-entry-btn .scan-icon {
  font-size: 18px;
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
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
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

.terminal-main {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.orders-list-container {
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

.stat-item.pending {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

.stat-item.making {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.stat-item.completed {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.orders-list {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
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
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.order-card.pending {
  border-color: #f59e0b;
}

.order-card.making {
  border-color: #3b82f6;
}

.order-card.completed {
  border-color: #22c55e;
}

.priority-stamp {
  position: absolute;
  top: 15px;
  right: 5px;
  width: 75px;
  height: 75px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 900;
  transform: rotate(12deg);
  z-index: 10;
  border-radius: 50%;
  border: 2.5px solid;
  text-align: center;
  line-height: 1.3;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  background: transparent;
}

.priority-stamp.urgent {
  color: #ef4444;
  border-color: #ef4444;
  animation: pulse-stamp 1.5s ease-in-out infinite;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.4);
}

.priority-stamp.special {
  color: #f59e0b;
  border-color: #f59e0b;
  animation: pulse-stamp-special 2s ease-in-out infinite;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4);
}

@keyframes pulse-stamp {
  0%, 100% { 
    opacity: 1;
    transform: rotate(12deg) scale(1);
    box-shadow: 0 4px 16px rgba(239, 68, 68, 0.5);
  }
  50% { 
    opacity: 0.95;
    transform: rotate(12deg) scale(1.1);
    box-shadow: 0 6px 24px rgba(239, 68, 68, 0.7);
  }
}

@keyframes pulse-stamp-special {
  0%, 100% { 
    opacity: 1;
    transform: rotate(12deg) scale(1);
    box-shadow: 0 4px 16px rgba(245, 158, 11, 0.5);
  }
  50% { 
    opacity: 0.95;
    transform: rotate(12deg) scale(1.08);
    box-shadow: 0 6px 24px rgba(245, 158, 11, 0.7);
  }
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

.order-status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
}

.order-status-badge.pending {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

.order-status-badge.making {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.order-status-badge.completed {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.status-icon {
  font-size: 14px;
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

.pickup-number {
  font-size: 20px;
  font-weight: 700;
  color: #fbbf24;
  padding: 2px 10px;
  background: rgba(251, 191, 36, 0.15);
  border-radius: 6px;
  font-family: 'SF Mono', 'Monaco', monospace;
}

.order-time, .complete-time, .making-time {
  font-size: 12px;
  color: #94a3b8;
}

.order-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.table-number, .order-type, .dish-count, .chef-name {
  font-size: 13px;
  color: #94a3b8;
  padding: 2px 8px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 6px;
}

.order-type.dine-in {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}

.order-type.takeaway {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

.order-type.pickup {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
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
  justify-content: space-between;
  align-items: center;
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

.scan-result-icon.warning {
  color: #f59e0b;
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

.bind-tray-content {
  padding: 10px 0;
}

.order-info-box {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.order-info-box .info-row {
  display: flex;
  margin-bottom: 8px;
}

.order-info-box .info-row:last-child {
  margin-bottom: 0;
}

.order-info-box .label {
  color: #909399;
  width: 70px;
}

.order-info-box .value {
  color: #303133;
  flex: 1;
}

.tray-input-section .tip {
  color: #909399;
  font-size: 13px;
  margin-bottom: 10px;
}

.idle-trays {
  margin-top: 16px;
}

.idle-trays .idle-title {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.tray-tags {
  display: flex;
  flex-wrap: wrap;
}

.tray-info {
  font-size: 12px;
  color: #60a5fa;
  padding: 2px 8px;
  background: rgba(59, 130, 246, 0.15);
  border-radius: 6px;
}
</style>
