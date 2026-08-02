<template>
  <div class="call-number-page" :data-theme="isDark ? 'dark' : 'light'">
    <div class="top-bar">
      <div class="page-info">
        <h1>取餐叫号系统</h1>
        <span class="subtitle">Pickup Call System</span>
      </div>
      <div class="quick-actions">
        <el-switch v-model="autoCallEnabled" active-text="自动叫号" inactive-text="手动叫号" class="auto-call-switch" />
        <el-button class="action-btn" @click="showHistoryDialog = true">
          <el-icon><Clock /></el-icon>
          <span>历史</span>
        </el-button>
        <el-button class="action-btn" @click="showSettingsDialog = true">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </el-button>
        <el-button class="action-btn" @click="syncFromKitchen" :loading="syncing">
          <el-icon><Refresh /></el-icon>
          <span>同步</span>
        </el-button>
        <el-button class="action-btn theme-btn" @click="toggleTheme">
          <el-icon v-if="isDark"><Sunny /></el-icon>
          <el-icon v-else><Moon /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="main-content" :class="{ 'display-mode': isDisplayMode }">
      <div class="left-panel" v-show="!isDisplayMode">
        <div class="current-calling-section">
          <div class="section-title">当前叫号</div>
          <div class="current-number-display" v-if="currentOrder">
            <div class="big-number">{{ currentOrder.orderNumber }}</div>
            <div class="order-tags">
              <span class="tag" :class="getOrderTypeClass(currentOrder.orderType)">{{ currentOrder.orderType || '堂食' }}</span>
              <span class="tag table" v-if="currentOrder.tableNumber">{{ currentOrder.tableNumber }}</span>
              <span class="tag count">{{ currentOrder.itemCount || 0 }}份</span>
            </div>
            <div class="call-info" v-if="currentOrder.callCount > 0">
              已叫号 {{ currentOrder.callCount }} 次
            </div>
          </div>
          <div class="no-current" v-else>
            <div class="empty-icon">🔔</div>
            <div class="empty-text">暂无叫号</div>
          </div>
        </div>

        <div class="call-actions" v-show="!isDisplayMode">
          <el-button type="primary" size="large" class="call-btn" @click="callNext" :disabled="pendingOrders.length === 0" :loading="calling">
            <el-icon><Bell /></el-icon>
            <span>叫下一号</span>
          </el-button>
          <el-button type="warning" size="large" class="call-btn" @click="callAgain" :disabled="!currentOrder" :loading="recalling">
            <el-icon><RefreshRight /></el-icon>
            <span>重叫</span>
          </el-button>
          <el-button type="success" size="large" class="call-btn" @click="markPicked" :disabled="!currentOrder" :loading="pickingUp">
            <el-icon><CircleCheck /></el-icon>
            <span>已取餐</span>
          </el-button>
        </div>

        <div class="stats-row" v-show="!isDisplayMode">
          <div class="stat-card pending">
            <div class="stat-num">{{ stats.pending }}</div>
            <div class="stat-label">待叫号</div>
          </div>
          <div class="stat-card called">
            <div class="stat-num">{{ stats.called }}</div>
            <div class="stat-label">已叫号</div>
          </div>
          <div class="stat-card picked">
            <div class="stat-num">{{ stats.picked }}</div>
            <div class="stat-label">已取餐</div>
          </div>
        </div>
      </div>

      <!-- 展示模式：全屏显示当前叫号信息 -->
      <div class="display-mode-content" v-if="isDisplayMode">
        <div class="display-current-number" v-if="currentOrder">
          <div class="display-big-number">{{ currentOrder.orderNumber }}</div>
          <div class="display-order-info">
            <span class="display-tag" :class="getOrderTypeClass(currentOrder.orderType)">{{ currentOrder.orderType || '堂食' }}</span>
            <span class="display-tag table" v-if="currentOrder.tableNumber">{{ currentOrder.tableNumber }}桌</span>
            <span class="display-tag count">{{ currentOrder.itemCount || 0 }}份</span>
          </div>
        </div>
        <div class="display-empty" v-else>
          <div class="display-empty-icon">🔔</div>
          <div class="display-empty-text">等待叫号中...</div>
        </div>
      </div>

      <div class="right-panel" v-show="!isDisplayMode">
        <div class="queue-section">
          <div class="section-header">
            <h2>待取餐队列</h2>
            <el-radio-group v-model="filterStatus" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="pending">待叫号</el-radio-button>
              <el-radio-button label="called">已叫号</el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="queue-grid" v-if="filteredOrders.length > 0">
            <div
              v-for="(order, index) in filteredOrders"
              :key="order.id"
              class="queue-item"
              :class="{ active: currentOrder?.orderId === order.orderId, [order.status]: true }"
              @click="selectOrder(order)"
            >
              <div class="queue-index">{{ index + 1 }}</div>
              <div class="queue-number">{{ order.orderNumber }}</div>
              <div class="queue-meta">
                <span class="type-tag" :class="getOrderTypeClass(order.orderType)">{{ order.orderType?.charAt(0) || '堂' }}</span>
                <span class="table-tag" v-if="order.tableNumber">{{ order.tableNumber }}</span>
              </div>
              <div class="queue-status" :class="order.status">
                {{ getStatusText(order.status) }}
              </div>
              <div class="queue-actions">
                <el-button v-if="order.status === 'pending'" type="primary" size="small" @click.stop="callOrder(order)" :loading="order.calling">
                  叫号
                </el-button>
                <el-button v-if="order.status === 'called'" type="success" size="small" @click.stop="markOrderPicked(order)" :loading="order.pickingUp">
                  已取
                </el-button>
              </div>
            </div>
          </div>
          
          <div class="empty-queue" v-else>
            <div class="empty-icon">✨</div>
            <div class="empty-text">暂无待取餐订单</div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showHistoryDialog" title="叫号历史" width="700px" :lock-scroll="false">
      <el-table :data="historyList" size="small" max-height="400" v-loading="loadingHistory">
        <el-table-column prop="orderNumber" label="订单号" width="100" />
        <el-table-column prop="orderType" label="类型" width="80" />
        <el-table-column prop="tableNumber" label="桌号" width="80" />
        <el-table-column prop="callCount" label="叫号次数" width="80" />
        <el-table-column prop="firstCallTime" label="首次叫号" width="120">
          <template #default="{ row }">{{ formatTime(row.firstCallTime) }}</template>
        </el-table-column>
        <el-table-column prop="pickTime" label="取餐时间" width="120">
          <template #default="{ row }">{{ row.pickTime ? formatTime(row.pickTime) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="waitSeconds" label="等待时长">
          <template #default="{ row }">{{ formatWaitTime(row.waitSeconds) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="showSettingsDialog" title="语音设置" width="500px" :lock-scroll="false">
      <el-form label-width="100px" size="small">
        <el-form-item label="启用语音">
          <el-switch v-model="voiceSettings.enabled" />
        </el-form-item>
        <el-form-item label="自动叫号">
          <el-switch v-model="autoCallEnabled" />
          <div class="form-tip">开启后，后厨完成制作时自动播报</div>
        </el-form-item>
        <el-form-item label="硬件状态">
          <div class="hardware-status">
            <span :class="['status-dot', hardwareSpeakerAvailable ? 'online' : 'offline']"></span>
            <span>{{ hardwareSpeakerAvailable ? '语音硬件已连接' : '语音硬件未连接（将使用浏览器TTS）' }}</span>
          </div>
          <div class="form-tip">串口音箱/显示屏连接状态</div>
        </el-form-item>
        <template v-if="voiceSettings.enabled">
          <el-form-item label="语音引擎">
            <el-select v-model="voiceSettings.voiceURI" style="width: 100%" placeholder="选择语音">
              <el-option label="系统默认" value="" />
              <el-option v-for="voice in availableVoices" :key="voice.voiceURI" :label="voice.name" :value="voice.voiceURI">
                <span>{{ voice.name }}</span>
                <span style="color: #999; font-size: 12px; margin-left: 8px;">{{ voice.lang }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="音量">
            <el-slider v-model="voiceSettings.volume" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="语速">
            <el-slider v-model="voiceSettings.rate" :min="0.5" :max="2" :step="0.1" />
          </el-form-item>
          <el-form-item label="播报次数">
            <el-input-number v-model="voiceSettings.repeatCount" :min="1" :max="5" />
          </el-form-item>
          <el-form-item label="播报间隔">
            <el-input-number v-model="voiceSettings.repeatInterval" :min="1" :max="10" />
            <span style="margin-left: 8px;">秒</span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="testVoice">测试语音</el-button>
        <el-button @click="showSettingsDialog = false">取消</el-button>
        <el-button type="primary" @click="saveVoiceSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Sunny, Moon, Bell, RefreshRight, CircleCheck, Clock, Setting, Refresh } from '@element-plus/icons-vue'
import SockJS from 'sockjs-client'
import { Client, IMessage } from '@stomp/stompjs'
import { ttsService } from '@/services/tts'
import request from '@/api/request'
import { callNumberHardwareApi, type HardwareVoiceResponse } from '@/api/callNumberApi'

interface CallRecord {
  id: number
  orderId: string
  orderNumber: string
  tableNumber?: string
  orderType?: string
  itemCount: number
  status: 'pending' | 'called' | 'picked'
  callCount: number
  firstCallTime?: string
  lastCallTime?: string
  pickTime?: string
  waitSeconds?: number
  createTime: string
  calling?: boolean
  pickingUp?: boolean
}

const isDark = ref(false)
const autoCallEnabled = ref(true)
// 显示模式：控制端（默认）或展示端
const isDisplayMode = ref(false)
const pendingOrders = ref<CallRecord[]>([])
const currentOrder = ref<CallRecord | null>(null)
const filterStatus = ref('all')
const showHistoryDialog = ref(false)
const showSettingsDialog = ref(false)
const historyList = ref<CallRecord[]>([])
const availableVoices = ref<SpeechSynthesisVoice[]>([])
const loadingHistory = ref(false)
const syncing = ref(false)
const calling = ref(false)
const recalling = ref(false)
const pickingUp = ref(false)

// 硬件状态
const hardwareAvailable = ref(false)
const hardwareSpeakerAvailable = ref(false)

const stats = ref({
  pending: 0,
  called: 0,
  picked: 0
})

const voiceSettings = ref({
  enabled: true,
  volume: 80,
  rate: 1,
  voiceURI: '',
  repeatCount: 2,
  repeatInterval: 3
})

let stompClient: Client | null = null
let pollingInterval: number | null = null

const filteredOrders = computed(() => {
  if (filterStatus.value === 'all') {
    return pendingOrders.value.filter(o => o.status !== 'picked')
  }
  return pendingOrders.value.filter(o => o.status === filterStatus.value)
})

watch(autoCallEnabled, (val) => {
  localStorage.setItem('call-number-auto-call', val ? 'true' : 'false')
})

const toggleTheme = () => {
  isDark.value = !isDark.value
  localStorage.setItem('call-number-theme', isDark.value ? 'dark' : 'light')
}

/**
 * 广播叫号事件到其他窗口（副屏、顾客屏等）
 * 使用 CustomEvent + localStorage 双重机制确保跨窗口通信
 * @param action - 动作类型：call（叫号）、recall（重叫）、pickup（取餐）
 * @param data - 叫号数据
 */
const broadcastCallEvent = (action: string, data: {
  orderNumber: string
  orderId: string
  tableNumber?: string
  orderType?: string
  itemCount: number
  status?: string
}): void => {
  const eventData = {
    action,
    data,
    timestamp: Date.now()
  }

  // 方式1：通过 CustomEvent 广播（同源窗口）
  const event = new CustomEvent('calling-broadcast', { detail: eventData })
  window.dispatchEvent(event)

  // 方式2：通过 localStorage 广播（跨窗口，包括 window.open 打开的窗口）
  try {
    localStorage.setItem('calling-event', JSON.stringify(eventData))
    // 触发 storage 事件（即使值相同也要触发，所以先清除再设置）
    localStorage.removeItem('calling-event')
    localStorage.setItem('calling-event', JSON.stringify(eventData))
  } catch (e) {
    console.warn('[广播] localStorage 写入失败:', e)
  }
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatWaitTime = (seconds: number) => {
  if (!seconds) return '-'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins > 0) {
    return `${mins}分${secs}秒`
  }
  return `${secs}秒`
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    pending: '待叫号',
    called: '已叫号',
    picked: '已取餐'
  }
  return statusMap[status] || status
}

const getOrderTypeClass = (orderType?: string) => {
  const classMap: Record<string, string> = {
    '堂食': 'dine-in',
    '外卖': 'takeout',
    '自提': 'pickup',
    '打包': 'pack'
  }
  return classMap[orderType || '堂食'] || 'dine-in'
}

const loadPendingOrders = async () => {
  try {
    const res = await request.get('/v1/call-number/pending')
    if (Array.isArray(res)) {
      pendingOrders.value = res
    } else if (res && typeof res === 'object') {
      pendingOrders.value = (res as any).data || []
    }
  } catch (e) {
    console.error('加载待取餐订单失败:', e)
  }
}

const loadStats = async () => {
  try {
    const res = await request.get('/v1/call-number/stats') as any
    if (res && typeof res === 'object') {
      stats.value = res
    }
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

/**
 * 检查硬件连接状态
 */
const checkHardwareStatus = async () => {
  try {
    const status = await callNumberHardwareApi.getStatus()
    hardwareAvailable.value = status?.anyHardwareAvailable || false
    hardwareSpeakerAvailable.value = status?.speakerAvailable || false
  } catch (e) {
    console.warn('检查硬件状态失败:', e)
    hardwareAvailable.value = false
    hardwareSpeakerAvailable.value = false
  }
}

/**
 * 通过硬件设备播报叫号语音（优先），不可用时回退到浏览器TTS
 * @param orderData - 叫号数据（订单号、桌号、类型、数量）
 */
const speakWithHardwareFallback = async (orderData: {
  orderNumber: string
  tableNumber: string
  orderType: string
  itemCount: number
}): Promise<void> => {
  // 如果硬件可用，优先使用硬件播报
  if (hardwareSpeakerAvailable.value) {
    try {
      const result: HardwareVoiceResponse = await callNumberHardwareApi.broadcastVoice({
        orderNumber: orderData.orderNumber,
        orderType: orderData.orderType
      })

      if (result?.hardwareAvailable) {
        return // 硬件播报成功，直接返回
      }
      // 硬件返回但不可用，继续fallback到浏览器TTS
      console.warn('[硬件] 设备不可用，回退到浏览器TTS')
    } catch (e) {
      console.warn('[硬件] 播报请求异常，回退到浏览器TTS:', e)
    }
  }

  // 回退：使用浏览器 Web Speech API
  await ttsService.speak(orderData)
}

/**
 * 向LED显示屏发送叫号信息
 * @param orderData - 叫号数据
 */
const sendToDisplay = async (orderData: {
  orderNumber: string
  tableNumber: string
  orderType: string
}): Promise<void> => {
  try {
    const result = await callNumberHardwareApi.sendToDisplay({
      orderNumber: orderData.orderNumber,
      orderType: orderData.orderType,
      tableNumber: orderData.tableNumber || undefined
    })
    if (result?.hardwareAvailable) {
      // 显示屏发送成功
    }
  } catch (e) {
    console.warn('[显示屏] 发送失败:', e)
    // 显示屏发送失败不阻断主流程
  }
}

// 加载叫号历史记录
const loadHistory = async () => {
  loadingHistory.value = true
  try {
    const res = await request.get('/v1/call-number/history')
    if (Array.isArray(res)) {
      historyList.value = res
    } else if (res && typeof res === 'object') {
      historyList.value = (res as any).data || []
    }
  } catch (e) {
    console.error('加载历史记录失败:', e)
  } finally {
    loadingHistory.value = false
  }
}

const syncFromKitchen = async () => {
  syncing.value = true
  try {
    await request.post('/v1/call-number/sync-from-kitchen')
    ElMessage.success('同步成功')
    await loadPendingOrders()
    await loadStats()
  } catch (e) {
    console.error('同步失败:', e)
    ElMessage.error('同步失败')
  } finally {
    syncing.value = false
  }
}

const callNext = async () => {
  const firstPending = pendingOrders.value.find(o => o.status === 'pending')
  if (!firstPending) {
    ElMessage.warning('没有待叫号订单')
    return
  }
  await callOrder(firstPending)
}

const callOrder = async (order: CallRecord, autoCall = false) => {
  order.calling = true
  try {
    const res = await request.post('/v1/call-number/call', {
      orderId: order.orderId,
      orderNumber: order.orderNumber,
      tableNumber: order.tableNumber,
      orderType: order.orderType,
      itemCount: order.itemCount
    })

    let updated = res
    if (res && typeof res === 'object' && !Array.isArray(res) && (res as any).data) {
      updated = (res as any).data
    }

    if (updated) {
      order.status = (updated as any).status || order.status
      order.callCount = (updated as any).callCount || 0
      order.firstCallTime = (updated as any).firstCallTime
      order.lastCallTime = (updated as any).lastCallTime
      currentOrder.value = order
    }

    if (voiceSettings.value.enabled) {
      // 使用硬件优先策略：先尝试硬件语音播报，不可用时回退到浏览器TTS
      await speakWithHardwareFallback({
        orderNumber: order.orderNumber,
        tableNumber: order.tableNumber || '',
        orderType: order.orderType || '堂食',
        itemCount: order.itemCount
      })
    }

    // 同时向LED显示屏发送叫号信息（非阻塞）
    await sendToDisplay({
      orderNumber: order.orderNumber,
      tableNumber: order.tableNumber || '',
      orderType: order.orderType || '堂食'
    })

    if (!autoCall) {
      ElMessage.success(`正在叫号：${order.orderNumber}`)
    }
    await loadStats()

    // 广播叫号事件到其他窗口（副屏、顾客屏）
    broadcastCallEvent('call', {
      orderNumber: order.orderNumber,
      orderId: order.orderId,
      tableNumber: order.tableNumber,
      orderType: order.orderType,
      itemCount: order.itemCount,
      status: order.status
    })
  } catch (e) {
    console.error('叫号失败:', e)
    ElMessage.error('叫号失败')
  } finally {
    order.calling = false
  }
}

const callAgain = async () => {
  if (!currentOrder.value) return

  recalling.value = true
  try {
    await request.post('/v1/call-number/recall', {
      orderId: currentOrder.value.orderId
    })

    currentOrder.value.callCount = (currentOrder.value.callCount || 0) + 1

    if (voiceSettings.value.enabled) {
      // 重叫也使用硬件优先策略
      await speakWithHardwareFallback({
        orderNumber: currentOrder.value.orderNumber,
        tableNumber: currentOrder.value.tableNumber || '',
        orderType: currentOrder.value.orderType || '堂食',
        itemCount: currentOrder.value.itemCount
      })
    }

    // 重叫时也更新显示屏
    await sendToDisplay({
      orderNumber: currentOrder.value.orderNumber,
      tableNumber: currentOrder.value.tableNumber || '',
      orderType: currentOrder.value.orderType || '堂食'
    })

    ElMessage.success(`重叫：${currentOrder.value.orderNumber}`)
    await loadStats()

    // 广播重叫事件到其他窗口
    broadcastCallEvent('recall', {
      orderNumber: currentOrder.value.orderNumber,
      orderId: currentOrder.value.orderId,
      tableNumber: currentOrder.value.tableNumber,
      orderType: currentOrder.value.orderType,
      itemCount: currentOrder.value.itemCount,
      status: currentOrder.value.status
    })
  } catch (e) {
    console.error('重叫失败:', e)
    ElMessage.error('重叫失败')
  } finally {
    recalling.value = false
  }
}

const markPicked = () => {
  if (!currentOrder.value) return
  markOrderPicked(currentOrder.value)
}

const markOrderPicked = async (order: CallRecord) => {
  order.pickingUp = true
  try {
    await request.post('/v1/call-number/pickup', {
      orderId: order.orderId
    })
    
    pendingOrders.value = pendingOrders.value.filter(o => o.orderId !== order.orderId)
    
    if (currentOrder.value?.orderId === order.orderId) {
      const nextPending = pendingOrders.value.find(o => o.status === 'pending') || 
                          pendingOrders.value.find(o => o.status === 'called')
      currentOrder.value = nextPending || null
    }
    
    ElMessage.success(`${order.orderNumber} 已取餐`)
    await loadStats()
  } catch (e) {
    console.error('标记取餐失败:', e)
    ElMessage.error('标记取餐失败')
  } finally {
    order.pickingUp = false
  }
}

const selectOrder = (order: CallRecord) => {
  currentOrder.value = order
}

const handleNewCallRecord = async (message: IMessage) => {
  try {
    const data = JSON.parse(message.body)

    const existing = pendingOrders.value.find(o => o.orderId === data.orderId)
    if (!existing) {
      // 添加到待叫号列表头部
      pendingOrders.value.unshift(data)

      // 自动叫号模式：只要开启自动叫号就执行叫号（不再依赖语音开关）
      if (autoCallEnabled.value) {
        ElMessage.success(`新订单 ${data.orderNumber}，自动叫号中...`)
        try {
          await callOrder(data, true)
        } catch (e) {
          console.error('自动叫号失败:', e)
          ElMessage.warning(`订单 ${data.orderNumber} 自动叫号失败，请手动处理`)
        }
      } else {
        ElMessage.success(`新订单 ${data.orderNumber} 待取餐`)
      }

      // 刷新统计数据
      await loadStats()
    }
  } catch (e) {
    console.error('解析消息失败:', e)
  }
}

const handleCalledRecord = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body)

    const order = pendingOrders.value.find(o => o.orderId === data.orderId)
    if (order) {
      order.status = data.status
      order.callCount = data.callCount
      order.lastCallTime = data.lastCallTime
    }

    // 更新当前叫号显示
    if (!currentOrder.value || currentOrder.value.orderId === data.orderId) {
      currentOrder.value = data
    }

    // 刷新统计
    loadStats()
  } catch (e) {
    console.error('解析消息失败:', e)
  }
}

const handlePickedRecord = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body)

    // 从待取餐列表中移除
    pendingOrders.value = pendingOrders.value.filter(o => o.orderId !== data.orderId)

    // 如果当前正在显示的订单被取餐，自动切换到下一个
    if (currentOrder.value?.orderId === data.orderId) {
      const nextPending = pendingOrders.value.find(o => o.status === 'pending') ||
                          pendingOrders.value.find(o => o.status === 'called')
      currentOrder.value = nextPending || null

      // 如果还有待叫号的订单且开启了自动叫号，自动叫下一个
      if (autoCallEnabled.value && nextPending && nextPending.status === 'pending') {
        callOrder(nextPending, true).catch((e: unknown) => {
          console.error('自动叫号下一个失败:', e)
        })
      }
    }

    // 实时刷新统计数据
    loadStats()
  } catch (e) {
    console.error('解析消息失败:', e)
  }
}

const initWebSocket = () => {
  const socket = new SockJS('/api/ws')
  
  stompClient = new Client({
    webSocketFactory: () => socket as any,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    
    onConnect: () => {
      stompClient?.subscribe('/topic/call-number/new', handleNewCallRecord)
      stompClient?.subscribe('/topic/call-number/called', handleCalledRecord)
      stompClient?.subscribe('/topic/call-number/picked', handlePickedRecord)
      stompClient?.subscribe('/topic/orders/ready', handleNewCallRecord)
    },

    onDisconnect: () => {
      console.warn('WebSocket连接断开，启动轮询')
      startPolling()
    },
    
    onStompError: (frame) => {
      console.error('❌ STOMP错误:', frame)
      startPolling()
    }
  })
  
  stompClient.activate()
}

const startPolling = () => {
  if (pollingInterval) return
  pollingInterval = window.setInterval(() => {
    loadPendingOrders()
    loadStats()
  }, 5000)
}

const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

const loadVoices = () => {
  if ('speechSynthesis' in window) {
    const voices = speechSynthesis.getVoices()
    availableVoices.value = voices.filter(v => v.lang.includes('zh') || v.lang.includes('CN'))
    if (availableVoices.value.length === 0) {
      availableVoices.value = voices.slice(0, 10)
    }
  }
}

const testVoice = async () => {
  if (!voiceSettings.value.enabled) {
    ElMessage.warning('请先启用语音')
    return
  }
  
  ttsService.updateConfig(voiceSettings.value)
  
  await ttsService.speak({
    orderNumber: 'A001',
    tableNumber: 'T01',
    orderType: '堂食',
    itemCount: 2
  })
  
  ElMessage.success('语音测试完成')
}

const saveVoiceSettings = () => {
  ttsService.updateConfig(voiceSettings.value)
  
  const settings = {
    voice: voiceSettings.value,
    autoCall: autoCallEnabled.value
  }
  localStorage.setItem('callNumberSettings', JSON.stringify(settings))
  
  showSettingsDialog.value = false
  ElMessage.success('设置已保存')
}

onMounted(async () => {
  // 检测显示模式（URL参数 mode=display 时进入纯展示模式）
  const urlParams = new URLSearchParams(window.location.search)
  isDisplayMode.value = urlParams.get('mode') === 'display' || urlParams.get('mode') === 'calling'

  if (isDisplayMode.value) {
    document.title = '叫号展示屏'
  }

  const savedTheme = localStorage.getItem('call-number-theme')
  if (savedTheme === 'dark') {
    isDark.value = true
  }

  const savedAutoCall = localStorage.getItem('call-number-auto-call')
  if (savedAutoCall !== null) {
    autoCallEnabled.value = savedAutoCall === 'true'
  }

  const savedSettings = localStorage.getItem('callNumberSettings')
  if (savedSettings) {
    try {
      const parsed = JSON.parse(savedSettings)
      if (parsed.voice) {
        voiceSettings.value = { ...voiceSettings.value, ...parsed.voice }
      }
      if (parsed.autoCall !== undefined) {
        autoCallEnabled.value = parsed.autoCall
      }
    } catch (e) {
      console.error('加载设置失败:', e)
    }
  }

  await loadPendingOrders()
  await loadStats()
  await loadHistory()

  // 检查硬件连接状态
  await checkHardwareStatus()

  initWebSocket()
  
  if ('speechSynthesis' in window) {
    loadVoices()
    speechSynthesis.onvoiceschanged = loadVoices
  }
  
  ttsService.loadConfig()

  // 监听来自其他窗口的叫号广播事件（CustomEvent）
  window.addEventListener('calling-broadcast', handleCallingBroadcast)

  // 监听 localStorage 变化（跨窗口通信）
  window.addEventListener('storage', handleStorageEvent)
})

/**
 * 处理来自其他窗口的叫号广播事件
 * @param event - CustomEvent 事件对象
 */
const handleCallingBroadcast = (event: Event): void => {
  const customEvent = event as CustomEvent
  const eventData = customEvent.detail
  if (!eventData) return

  // 根据事件类型更新本地状态
  if (eventData.action === 'call' || eventData.action === 'recall') {
    const orderData = eventData.data
    // 更新当前叫号显示
    if (orderData) {
      currentOrder.value = {
        ...currentOrder.value,
        orderId: orderData.orderId,
        orderNumber: orderData.orderNumber,
        tableNumber: orderData.tableNumber,
        orderType: orderData.orderType,
        itemCount: orderData.itemCount,
        status: orderData.status || 'called',
        callCount: eventData.action === 'recall' ? ((currentOrder.value?.callCount || 0) + 1) : 1
      } as CallRecord
    }
  }
}

/**
 * 处理 localStorage 变化事件（跨窗口通信）
 * @param event - StorageEvent 事件对象
 */
const handleStorageEvent = (event: StorageEvent): void => {
  if (event.key !== 'calling-event') return

  try {
    const eventData = JSON.parse(event.newValue || '{}')

    // 转发给 CustomEvent 处理器
    handleCallingBroadcast({ detail: eventData } as unknown as Event)
  } catch (e) {
    console.warn('[广播接收] 解析事件数据失败:', e)
  }
}

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  stopPolling()

  // 清理广播事件监听器
  window.removeEventListener('calling-broadcast', handleCallingBroadcast)
  window.removeEventListener('storage', handleStorageEvent)
})
</script>

<style scoped>
.call-number-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  overflow: hidden;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.page-info h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #fff;
}

.page-info .subtitle {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.quick-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-call-switch {
  --el-switch-on-color: #67C23A;
}

.action-btn {
  height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.current-calling-section {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 30px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.section-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 20px;
  text-transform: uppercase;
  letter-spacing: 2px;
}

.current-number-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.big-number {
  font-size: 80px;
  font-weight: 700;
  background: linear-gradient(135deg, #ea580c 0%, #f97316 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  text-shadow: 0 0 60px rgba(234, 88, 12, 0.5);
}

.order-tags {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.tag.dine-in { background: #409EFF; color: #fff; }
.tag.takeout { background: #E6A23C; color: #fff; }
.tag.pickup { background: #67C23A; color: #fff; }
.tag.pack { background: #909399; color: #fff; }
.tag.table { background: rgba(255, 255, 255, 0.2); color: #fff; }
.tag.count { background: rgba(255, 255, 255, 0.1); color: rgba(255, 255, 255, 0.8); }

.call-info {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.no-current {
  padding: 40px;
}

.no-current .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.no-current .empty-text {
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}

.call-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.call-btn {
  height: 56px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 14px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  padding: 16px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-card.pending { border-color: #E6A23C; }
.stat-card.called { border-color: #409EFF; }
.stat-card.picked { border-color: #67C23A; }

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
}

.stat-card.pending .stat-num { color: #E6A23C; }
.stat-card.called .stat-num { color: #409EFF; }
.stat-card.picked .stat-num { color: #67C23A; }

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.right-panel {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 20px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.queue-grid {
  flex: 1;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  align-content: start;
}

.queue-item {
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.queue-item:hover {
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
}

.queue-item.active {
  border-color: #ea580c;
  background: rgba(234, 88, 12, 0.1);
}

.queue-item.called {
  border-left: 4px solid #409EFF;
}

.queue-item.picked {
  opacity: 0.5;
}

.queue-index {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}

.queue-number {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8px;
}

.queue-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.type-tag {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 500;
}

.type-tag.dine-in { background: #409EFF; color: #fff; }
.type-tag.takeout { background: #E6A23C; color: #fff; }
.type-tag.pickup { background: #67C23A; color: #fff; }
.type-tag.pack { background: #909399; color: #fff; }

.table-tag {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.queue-status {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
}

.queue-status.pending {
  background: rgba(230, 162, 60, 0.2);
  color: #E6A23C;
}

.queue-status.called {
  background: rgba(64, 158, 255, 0.2);
  color: #409EFF;
}

.queue-actions {
  margin-top: 8px;
}

.empty-queue {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.empty-queue .empty-icon {
  font-size: 48px;
}

.empty-queue .empty-text {
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.hardware-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.online {
  background: #67C23A;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.6);
}

.status-dot.offline {
  background: #909399;
}

/* 展示模式样式 */
.main-content.display-mode {
  grid-template-columns: 1fr;
  justify-items: center;
  align-content: center;
}

.display-mode-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 40px;
}

.display-current-number {
  text-align: center;
  animation: displayPulse 2s ease-in-out infinite;
}

.display-big-number {
  font-size: 180px;
  font-weight: 800;
  background: linear-gradient(135deg, #ea580c 0%, #f97316 50%, #fb923c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  text-shadow: 0 0 80px rgba(102, 126, 234, 0.6);
  letter-spacing: 8px;
}

.display-order-info {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 24px;
}

.display-tag {
  padding: 10px 28px;
  border-radius: 30px;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.display-tag.dine-in { background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%); }
.display-tag.takeout { background: linear-gradient(135deg, #E6A23C 0%, #d4920e 100%); }
.display-tag.pickup { background: linear-gradient(135deg, #67C23A 0%, #529b2e 100%); }
.display-tag.pack { background: linear-gradient(135deg, #909399 0%, #7a7a7a 100%); }
.display-tag.table { background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.1) 100%); }
.display-tag.count { background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.08) 100%); }

.display-empty {
  text-align: center;
}

.display-empty-icon {
  font-size: 120px;
  margin-bottom: 24px;
  animation: displayBlink 3s ease-in-out infinite;
}

.display-empty-text {
  font-size: 36px;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 500;
}

@keyframes displayPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.02);
    opacity: 0.95;
  }
}

@keyframes displayBlink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}
</style>
