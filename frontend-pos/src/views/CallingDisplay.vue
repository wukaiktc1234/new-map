<template>
  <div class="calling-display">
    <!-- 顶部信息栏 -->
    <header class="display-header">
      <div class="store-name">{{ storeName }}</div>
      <div class="current-time">{{ currentTime }}</div>
    </header>

    <!-- 主区域：当前叫号 -->
    <main class="display-main">
      <div class="current-call-wrapper">
        <!-- 当前叫号号码 -->
        <div 
          class="current-number" 
          :class="{ 'call-animating': isAnimating }"
        >
          {{ currentNumber || '--' }}
        </div>
        
        <!-- 提示文字 -->
        <div class="call-hint" v-if="currentNumber">
          请 {{ currentNumber }} 号顾客取餐
        </div>
        <div class="call-hint waiting" v-else>
          等待叫号中...
        </div>
      </div>
    </main>

    <!-- 底部：待取餐队列 -->
    <footer class="display-footer">
      <div class="queue-section">
        <div class="queue-title">待取餐</div>
        <div class="queue-list" v-if="displayPendingNumbers.length > 0">
          <TransitionGroup name="queue-slide">
            <div 
              v-for="num in displayPendingNumbers" 
              :key="num"
              class="queue-item"
            >
              {{ num }}
            </div>
          </TransitionGroup>
        </div>
        <div class="queue-empty" v-else>
          暂无待取餐订单
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client, IMessage } from '@stomp/stompjs'
import request from '@/api/request'

/**
 * 叫号记录接口定义
 */
interface CallRecord {
  orderId: string
  orderNumber: string
  status: 'pending' | 'called' | 'picked'
}

// ==================== 响应式数据 ====================

/** 当前正在呼叫的号码 */
const currentNumber = ref<string>('')

/** 是否正在播放叫号动画 */
const isAnimating = ref(false)

/** 待取餐队列列表 */
const pendingOrders = ref<CallRecord[]>([])

/** 当前时间字符串 */
const currentTime = ref('')

/** 店铺名称 */
const storeName = ref('快餐收银系统')

// ==================== 定时器引用 ====================

let timeTimer: number | null = null
let stompClient: Client | null = null
let pollingTimer: number | null = null

// ==================== 计算属性 ====================

/**
 * 获取用于展示的待取餐号码列表（最多显示10个）
 */
const displayPendingNumbers = computed(() => {
  // 过滤出待叫号和已叫号的订单，排除当前正在呼叫的号码
  const numbers = pendingOrders.value
    .filter(order => order.status !== 'picked' && order.orderNumber !== currentNumber.value)
    .map(order => order.orderNumber)
    .slice(0, 10)
  
  return numbers
})

// ==================== 方法 ====================

/**
 * 更新当前时间显示
 */
const updateTime = () => {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`
}

/**
 * 触发叫号动画效果
 * @param number - 叫号的号码
 */
const triggerCallAnimation = (number: string) => {
  currentNumber.value = number
  isAnimating.value = true
  
  // 动画持续时间后移除动画类
  setTimeout(() => {
    isAnimating.value = false
  }, 800)
}

/**
 * 从服务器加载待取餐列表
 */
const loadPendingOrders = async () => {
  try {
    const res = await request.get('/api/v1/call-number/pending') as CallRecord[]
    
    if (Array.isArray(res) && res.length > 0) {
      pendingOrders.value = res
      
      // 如果没有当前叫号，自动设置第一个已叫号的订单为当前叫号
      if (!currentNumber.value) {
        const calledOrder = res.find(order => order.status === 'called')
        if (calledOrder) {
          currentNumber.value = calledOrder.orderNumber
        }
      }
    } else {
      pendingOrders.value = []
    }
  } catch (error) {
    console.error('加载待取餐列表失败:', error)
  }
}

/**
 * 处理WebSocket接收到的叫号事件
 * @param message - STOMP消息对象
 */
const handleCalledEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body)

    if (data?.orderNumber) {
      // 触发叫号动画
      triggerCallAnimation(data.orderNumber)
      
      // 更新待取餐列表中的状态
      const existingOrder = pendingOrders.value.find(
        order => order.orderId === data.orderId
      )
      
      if (existingOrder) {
        existingOrder.status = data.status || 'called'
      } else {
        // 如果不存在，添加到列表
        pendingOrders.value.unshift({
          orderId: data.orderId,
          orderNumber: data.orderNumber,
          status: data.status || 'called'
        })
      }
    }
  } catch (error) {
    console.error('解析叫号消息失败:', error)
  }
}

/**
 * 处理WebSocket接收到的取餐事件
 * @param message - STOMP消息对象
 */
const handlePickedEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body)

    if (data?.orderId) {
      // 从待取餐列表中移除已取餐的订单
      pendingOrders.value = pendingOrders.value.filter(
        order => order.orderId !== data.orderId
      )

      // 如果当前正在显示的号码被取餐，清除当前号码或切换到下一个
      if (currentNumber.value === data.orderNumber) {
        // 查找下一个已叫号的订单
        const nextCalled = pendingOrders.value.find(order => order.status === 'called')
        
        if (nextCalled) {
          triggerCallAnimation(nextCalled.orderNumber)
        } else {
          currentNumber.value = ''
        }
      }
    }
  } catch (error) {
    console.error('解析取餐消息失败:', error)
  }
}

/**
 * 处理新的待取餐订单事件
 * @param message - STOMP消息对象
 */
const handleNewOrderEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body)

    if (data?.orderNumber && data?.orderId) {
      // 检查是否已存在
      const exists = pendingOrders.value.some(
        order => order.orderId === data.orderId
      )

      if (!exists) {
        // 添加到待取餐列表头部
        pendingOrders.value.unshift({
          orderId: data.orderId,
          orderNumber: data.orderNumber,
          status: data.status || 'pending'
        })
      }
    }
  } catch (error) {
    console.error('解析新订单消息失败:', error)
  }
}

/**
 * 初始化WebSocket连接
 */
const initWebSocket = () => {
  const socket = new SockJS('/api/ws')
  
  stompClient = new Client({
    webSocketFactory: () => socket as any,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,

    onConnect: () => {
      // 订阅叫号相关主题
      stompClient?.subscribe('/topic/call-number/called', handleCalledEvent)
      stompClient?.subscribe('/topic/call-number/picked', handlePickedEvent)
      stompClient?.subscribe('/topic/call-number/new', handleNewOrderEvent)
      stompClient?.subscribe('/topic/orders/ready', handleNewOrderEvent)

      // 连接成功后停止轮询
      stopPolling()
    },

    onDisconnect: () => {
      console.warn('[CallingDisplay] WebSocket断开，启动轮询备用方案')
      startPolling()
    },

    onStompError: (frame) => {
      console.error('[CallingDisplay] STOMP错误:', frame)
      startPolling()
    }
  })

  stompClient.activate()
}

/**
 * 启动轮询备用方案（WebSocket断开时使用）
 */
const startPolling = () => {
  if (pollingTimer) return

  pollingTimer = window.setInterval(() => {
    loadPendingOrders()
  }, 5000)
}

/**
 * 停止轮询
 */
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// ==================== 生命周期钩子 ====================

onMounted(async () => {
  // 启动时钟更新
  updateTime()
  timeTimer = window.setInterval(updateTime, 1000)
  
  // 加载初始数据
  await loadPendingOrders()
  
  // 初始化WebSocket连接
  initWebSocket()
})

onUnmounted(() => {
  // 清理定时器
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
  
  stopPolling()
  
  // 断开WebSocket连接
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
})
</script>

<style scoped>
/* ==================== CSS变量定义 ==================== */
.calling-display {
  /* 颜色变量 */
  --cd-bg-primary: #0f172a;
  --cd-bg-secondary: #1e293b;
  --cd-text-primary: #ffffff;
  --cd-text-secondary: rgba(255, 255, 255, 0.7);
  --cd-text-muted: rgba(255, 255, 255, 0.4);
  --cd-accent-color: #fbbf24;
  --cd-accent-glow: rgba(251, 191, 36, 0.5);
  --cd-queue-bg: rgba(255, 255, 255, 0.08);
  --cd-queue-border: rgba(255, 255, 255, 0.15);

  /* 字体大小变量 */
  --cd-font-number: min(20vw, 180px);
  --cd-font-hint: clamp(24px, 3vw, 40px);
  --cd-font-time: clamp(18px, 2vw, 28px);
  --cd-font-store: clamp(16px, 1.5vw, 24px);
  --cd-font-queue-item: clamp(20px, 2.5vw, 36px);

  /* 间距变量 */
  --cd-header-height: 80px;
  --cd-footer-height: 200px;
}

/* ==================== 主容器样式 ==================== */
.calling-display {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, var(--cd-bg-primary) 0%, #1a1a2e 50%, var(--cd-bg-secondary) 100%);
  color: var(--cd-text-primary);
  font-family: 'DIN Alternate', 'Roboto Mono', 'SF Mono', 'Monaco', monospace;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 20px 40px;
  box-sizing: border-box;
}

/* ==================== 顶部信息栏 ==================== */
.display-header {
  width: 100%;
  height: var(--cd-header-height);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-sizing: border-box;
}

.store-name {
  font-size: var(--cd-font-store);
  font-weight: 600;
  color: var(--cd-text-secondary);
  letter-spacing: 0.05em;
}

.current-time {
  font-size: var(--cd-font-time);
  font-weight: 500;
  color: var(--cd-text-muted);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.1em;
}

/* ==================== 主内容区 ==================== */
.display-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.current-call-wrapper {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
}

/* 当前叫号号码 - 超大字体 */
.current-number {
  font-size: var(--cd-font-number);
  font-weight: 900;
  letter-spacing: 0.15em;
  color: var(--cd-accent-color);
  text-shadow: 
    0 0 40px var(--cd-accent-glow),
    0 0 80px var(--cd-accent-glow),
    0 4px 12px rgba(0, 0, 0, 0.5);
  line-height: 1;
  user-select: none;
  transition: transform 0.1s ease;
}

/* 叫号弹跳动画 */
.current-number.call-animating {
  animation: callBounce 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes callBounce {
  0% {
    transform: scale(1);
  }
  30% {
    transform: scale(1.15);
  }
  50% {
    transform: scale(0.95);
  }
  70% {
    transform: scale(1.08);
  }
  100% {
    transform: scale(1);
  }
}

/* 提示文字 */
.call-hint {
  font-size: var(--cd-font-hint);
  font-weight: 500;
  color: var(--cd-text-secondary);
  letter-spacing: 0.08em;
  opacity: 0;
  animation: fadeInUp 0.5s ease forwards;
  animation-delay: 0.3s;
}

.call-hint.waiting {
  color: var(--cd-text-muted);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.7;
  }
}

/* ==================== 底部队列区 ==================== */
.display-footer {
  width: 100%;
  height: var(--cd-footer-height);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.queue-section {
  width: 100%;
  max-width: 1200px;
  text-align: center;
}

.queue-title {
  font-size: clamp(14px, 1.2vw, 18px);
  font-weight: 600;
  color: var(--cd-text-muted);
  letter-spacing: 0.15em;
  text-transform: uppercase;
  margin-bottom: 16px;
}

.queue-list {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: clamp(12px, 2vw, 24px);
  flex-wrap: wrap;
  min-height: 60px;
}

/* 队列项样式 */
.queue-item {
  font-size: var(--cd-font-queue-item);
  font-weight: 700;
  color: var(--cd-text-primary);
  background: var(--cd-queue-bg);
  border: 2px solid var(--cd-queue-border);
  border-radius: 16px;
  padding: clamp(8px, 1.5vw, 16px) clamp(20px, 3vw, 36px);
  letter-spacing: 0.1em;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  user-select: none;
  transition: all 0.3s ease;
}

.queue-item:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

/* 队列为空提示 */
.queue-empty {
  font-size: clamp(16px, 1.5vw, 22px);
  color: var(--cd-text-muted);
  letter-spacing: 0.05em;
}

/* ==================== 队列滑动动画 ==================== */

/* 进入动画 */
.queue-slide-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.queue-slide-leave-active {
  transition: all 0.3s ease-in;
}

.queue-slide-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.8);
}

.queue-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px) scale(0.8);
}

.queue-slide-move {
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* ==================== 响应式适配 ==================== */

/* 小屏幕适配 */
@media (max-width: 768px) {
  .calling-display {
    padding: 15px 20px;
  }

  :root {
    --cd-header-height: 60px;
    --cd-footer-height: 160px;
  }

  .queue-list {
    gap: 8px;
  }

  .queue-item {
    padding: 6px 16px;
    border-radius: 12px;
  }
}

/* 超宽屏幕适配 */
@media (min-width: 1920px) {
  .calling-display {
    padding: 30px 60px;
  }

  :root {
    --cd-font-number: 200px;
    --cd-font-hint: 48px;
    --cd-font-queue-item: 42px;
  }
}
</style>
