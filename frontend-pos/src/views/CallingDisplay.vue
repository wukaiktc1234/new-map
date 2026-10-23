<template>
  <div class="calling-display" data-theme="dark">
    <!-- 顶部信息栏 -->
    <header class="cd-header">
      <div class="cd-brand">
        <span class="cd-brand-mark">取餐叫号</span>
        <span class="cd-store">{{ storeName }}</span>
      </div>
      <div class="cd-header-right">
        <span class="cd-link" :class="{ 'is-live': wsConnected }">
          <span class="cd-link-dot"></span>
          {{ wsConnected ? '实时' : '轮询' }}
        </span>
        <span class="cd-clock">{{ clockText }}</span>
      </div>
    </header>

    <!-- 核心区：当前呼叫（3~5 米外可读） -->
    <section class="cd-hero" :class="{ 'is-idle': !heroRecord, 'is-fresh': isFresh }">
      <Transition name="hero-swap" mode="out-in">
        <div
          v-if="heroRecord"
          :key="heroKey"
          class="hero-card"
          :aria-label="`请 ${heroRecord.number} 取餐`"
          aria-live="polite"
        >
          <span class="hero-prefix">请</span>
          <span class="hero-number" :class="heroNumberClass">{{ heroRecord.number }}</span>
          <span class="hero-suffix">取餐</span>
        </div>
        <div v-else key="idle" class="hero-idle">
          <span class="hero-idle-text">等待叫号</span>
          <span class="hero-idle-sub">请留意屏幕，听到号码后到取餐口取餐</span>
        </div>
      </Transition>

      <div v-if="heroRecord" class="hero-caption">
        <span class="hero-tip">请核对号码，到取餐口取餐</span>
        <span class="hero-meta">
          <span v-if="heroRecord.callCount > 1" class="hero-repeat">
            第 {{ heroRecord.callCount }} 次呼叫
          </span>
          <span class="hero-time">
            呼叫时间 {{ formatTime(heroRecord.lastCallTime || heroRecord.firstCallTime) }}
          </span>
        </span>
      </div>
    </section>

    <!-- 两个区块：制作中 / 待取餐 -->
    <section class="cd-boards">
      <CallingQueuePanel
        title="制作中"
        tone="making"
        empty-text="暂无制作中订单"
        :items="makingTiles"
      />
      <CallingQueuePanel
        title="待取餐"
        tone="ready"
        empty-text="暂无待取餐号码"
        :items="readyTiles"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import SockJS from 'sockjs-client'
import { Client, type IMessage } from '@stomp/stompjs'
import request from '@/api/request'
import { formatDateTime, formatTime, parseDateTime } from '@/utils/format'
import CallingQueuePanel from '@/components/pos/CallingQueuePanel.vue'

/** 大屏只认两种活动状态；picked / passed 不上大屏，只留历史页 */
type DisplayStatus = 'pending' | 'called'

/** 后端 CallRecord（字段可能缺失/为 null，统一在 toDisplayRecord 里收敛） */
interface CallRecordPayload {
  orderId?: string | null
  orderNumber?: string | null
  callNumber?: string | null
  tableNumber?: string | null
  orderType?: string | null
  itemCount?: number | null
  status?: string | null
  callCount?: number | null
  firstCallTime?: string | null
  lastCallTime?: string | null
  pickTime?: string | null
  passTime?: string | null
  waitSeconds?: number | null
  createTime?: string | null
}

/** 大屏内部使用的规整记录 */
interface DisplayRecord {
  orderId: string
  /** 取餐号（A006）；缺失时回落订单号 */
  number: string
  status: DisplayStatus
  itemCount: number
  callCount: number
  orderType: string
  tableNumber: string
  createTime: string
  firstCallTime: string
  lastCallTime: string
}

/** 区块号牌 */
interface TileItem {
  key: string
  number: string
  time: string
  meta?: string
  current?: boolean
}

// ==================== 响应式数据 ====================

/** 活动记录（pending 制作中 + called 待取餐） */
const records = ref<DisplayRecord[]>([])

/** 顶部时钟：统一走 utils/format，不出现裸 ISO */
const clockText = ref(formatDateTime(new Date()))

/** 店铺名称 */
const storeName = ref('快餐收银系统')

/** WebSocket 是否在线（仅用于角标展示，不影响轮询） */
const wsConnected = ref(false)

/** 最近一次呼叫变化后的高亮过渡标记 */
const isFresh = ref(false)

// ==================== 定时器/连接引用 ====================

let clockTimer: number | null = null
let pollTimer: number | null = null
let freshTimer: number | null = null
let loading = false
let stompClient: Client | null = null
// 应用层监督重连：stompjs v7 内部重连在重试以 ws-error（无 close）收场时会永久停摆
// （P1-STOMP-RECONNECT-001），关闭内部重连、由本页统一调度
let wsReconnectTimer: number | null = null

const clearWsReconnectTimer = () => {
  if (wsReconnectTimer !== null) {
    window.clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
}

const scheduleWsReconnect = () => {
  if (wsReconnectTimer !== null) {
    return
  }
  wsReconnectTimer = window.setTimeout(() => {
    wsReconnectTimer = null
    const oldClient = stompClient
    stompClient = null
    oldClient?.deactivate().catch(() => {})
    initWebSocket()
  }, 5000)
}

// ==================== 归一化 ====================

const timeValue = (value: string): number => {
  const parsed = parseDateTime(value)
  return parsed ? parsed.getTime() : 0
}

/** 呼叫时间排序权重：lastCallTime > firstCallTime > createTime */
const callTimeValue = (record: DisplayRecord): number =>
  timeValue(record.lastCallTime || record.firstCallTime || record.createTime)

/**
 * 后端记录 -> 大屏记录。
 * 返回 null 表示该记录不该出现在大屏上（picked / passed / 无号码）。
 */
const toDisplayRecord = (
  raw: CallRecordPayload,
  fallbackStatus: DisplayStatus
): DisplayRecord | null => {
  const statusText = String(raw.status ?? fallbackStatus).trim().toLowerCase()
  if (statusText !== 'pending' && statusText !== 'called') {
    return null
  }
  const number = String(raw.callNumber || raw.orderNumber || '').trim()
  if (!number) {
    return null
  }
  const orderId = String(raw.orderId || raw.orderNumber || number).trim()
  return {
    orderId,
    number,
    status: statusText,
    itemCount: typeof raw.itemCount === 'number' ? raw.itemCount : 0,
    callCount: typeof raw.callCount === 'number' ? raw.callCount : 0,
    orderType: raw.orderType ?? '',
    tableNumber: raw.tableNumber ?? '',
    createTime: raw.createTime ?? '',
    firstCallTime: raw.firstCallTime ?? '',
    lastCallTime: raw.lastCallTime ?? ''
  }
}

/** /pending 正常返回数组；兼容包装成 { records } / { list } 的情况 */
const listFromResponse = (res: unknown): CallRecordPayload[] => {
  if (Array.isArray(res)) {
    return res as CallRecordPayload[]
  }
  if (res && typeof res === 'object') {
    const maybeRecords = (res as { records?: unknown }).records
    if (Array.isArray(maybeRecords)) {
      return maybeRecords as CallRecordPayload[]
    }
    const maybeList = (res as { list?: unknown }).list
    if (Array.isArray(maybeList)) {
      return maybeList as CallRecordPayload[]
    }
  }
  return []
}

// ==================== 计算属性 ====================

/** 制作中（pending）：按先来后到排 */
const makingRecords = computed<DisplayRecord[]>(() =>
  records.value
    .filter((record) => record.status === 'pending')
    .sort((a, b) => timeValue(a.createTime) - timeValue(b.createTime))
)

/** 待取餐（called）：最近呼叫的排最前 */
const calledRecords = computed<DisplayRecord[]>(() =>
  records.value
    .filter((record) => record.status === 'called')
    .sort((a, b) => callTimeValue(b) - callTimeValue(a))
)

/** 顶部大字播报的对象：最近一次呼叫 */
const heroRecord = computed<DisplayRecord | null>(() => calledRecords.value[0] ?? null)

/** 播报内容签名：变化即视为「最近一次呼叫变了」，触发高亮过渡 */
const heroKey = computed(() =>
  heroRecord.value
    ? `${heroRecord.value.orderId}|${heroRecord.value.callCount}|${heroRecord.value.lastCallTime}`
    : 'idle'
)

/** 号码过长（无 POS 叫号号时回落订单号）时按长度降档字号，避免超出屏幕 */
const heroNumberClass = computed(() => {
  const length = heroRecord.value?.number.length ?? 0
  if (length > 10) {
    return 'is-xlong'
  }
  if (length > 6) {
    return 'is-long'
  }
  return ''
})

const makingTiles = computed<TileItem[]>(() =>
  makingRecords.value.map((record) => ({
    key: record.orderId,
    number: record.number,
    time: formatTime(record.createTime),
    meta: record.itemCount > 0 ? `${record.itemCount} 件` : '制作中'
  }))
)

const readyTiles = computed<TileItem[]>(() =>
  calledRecords.value.map((record) => ({
    key: record.orderId,
    number: record.number,
    time: formatTime(record.lastCallTime || record.firstCallTime || record.createTime),
    meta: record.callCount > 1 ? `第 ${record.callCount} 次` : '已呼叫',
    current: heroRecord.value?.orderId === record.orderId
  }))
)

// ==================== 数据加载 ====================

/** 拉取活动列表（pending + called），picked/passed 在此被过滤掉 */
const loadRecords = async () => {
  if (loading) {
    return
  }
  loading = true
  try {
    const res = await request.get('/v1/call-number/pending')
    const next: DisplayRecord[] = []
    for (const raw of listFromResponse(res)) {
      const record = toDisplayRecord(raw, 'pending')
      if (record) {
        next.push(record)
      }
    }
    records.value = next
  } catch (error) {
    console.error('[CallingDisplay] 拉取待取餐列表失败:', error)
  } finally {
    loading = false
  }
}

/** WebSocket / 轮询增量更新：同 orderId 覆盖，否则追加 */
const upsertRecord = (raw: CallRecordPayload, fallbackStatus: DisplayStatus) => {
  const record = toDisplayRecord(raw, fallbackStatus)
  if (!record) {
    return
  }
  const index = records.value.findIndex((item) => item.orderId === record.orderId)
  if (index >= 0) {
    records.value.splice(index, 1, record)
  } else {
    records.value.push(record)
  }
}

/** 从大屏移除（已取餐 / 已过号都不再展示） */
const removeRecord = (orderId: unknown, orderNumber: unknown) => {
  const byId = String(orderId ?? '').trim()
  const byNumber = String(orderNumber ?? '').trim()
  records.value = records.value.filter(
    (record) => record.orderId !== byId && record.number !== byNumber
  )
}

// ==================== WebSocket ====================

const handleCalledEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body) as CallRecordPayload
    upsertRecord(data, 'called')
  } catch (error) {
    console.error('[CallingDisplay] 解析叫号消息失败:', error)
  }
}

const handlePickedEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body) as CallRecordPayload
    removeRecord(data.orderId, data.orderNumber)
  } catch (error) {
    console.error('[CallingDisplay] 解析取餐消息失败:', error)
  }
}

/** 过号：大屏不展示 passed，收到后从活动列表移除即可 */
const handlePassedEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body) as CallRecordPayload
    removeRecord(data.orderId, data.orderNumber)
  } catch (error) {
    console.error('[CallingDisplay] 解析过号消息失败:', error)
  }
}

const handleNewOrderEvent = (message: IMessage) => {
  try {
    const data = JSON.parse(message.body) as CallRecordPayload
    upsertRecord(data, 'pending')
  } catch (error) {
    console.error('[CallingDisplay] 解析新订单消息失败:', error)
  }
}

const initWebSocket = () => {
  const socket = new SockJS('/api/ws')

  stompClient = new Client({
    webSocketFactory: () => socket as never,
    // 后端 STOMP CONNECT 帧强制 JWT 鉴权（拦截器只读帧头）；beforeConnect 保证每次重连取最新 token
    beforeConnect: (client) => {
      const token = localStorage.getItem('pos-token')
      if (token) {
        client.connectHeaders = { Authorization: `Bearer ${token}` }
      }
    },
    reconnectDelay: 0, // 内部重连已停用：由 scheduleWsReconnect 接管
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,

    onConnect: () => {
      clearWsReconnectTimer()
      wsConnected.value = true
      stompClient?.subscribe('/topic/call-number/called', handleCalledEvent)
      stompClient?.subscribe('/topic/call-number/picked', handlePickedEvent)
      stompClient?.subscribe('/topic/call-number/passed', handlePassedEvent)
      stompClient?.subscribe('/topic/call-number/new', handleNewOrderEvent)
      stompClient?.subscribe('/topic/orders/ready', handleNewOrderEvent)
    },

    onDisconnect: () => {
      wsConnected.value = false
      // 断线期间靠 5s 轮询兜底，无需额外动作
    },

    onWebSocketClose: () => {
      wsConnected.value = false
      scheduleWsReconnect()
    },

    onWebSocketError: () => {
      wsConnected.value = false
      scheduleWsReconnect()
    },

    onStompError: (frame) => {
      wsConnected.value = false
      console.error('[CallingDisplay] STOMP错误:', frame)
    }
  })

  stompClient.activate()
}

// ==================== 时钟 ====================

const updateClock = () => {
  clockText.value = formatDateTime(new Date())
}

// ==================== 高亮过渡 ====================

watch(heroKey, (next, prev) => {
  if (next === prev) {
    return
  }
  isFresh.value = true
  if (freshTimer !== null) {
    window.clearTimeout(freshTimer)
  }
  freshTimer = window.setTimeout(() => {
    freshTimer = null
    isFresh.value = false
  }, 3200)
})

// ==================== 生命周期 ====================

onMounted(async () => {
  updateClock()
  clockTimer = window.setInterval(updateClock, 1000)

  await loadRecords()
  initWebSocket()

  // 5 秒轮询常驻：WebSocket 只是低延迟补充通道
  pollTimer = window.setInterval(() => {
    void loadRecords()
  }, 5000)
})

onBeforeUnmount(() => {
  if (clockTimer !== null) {
    window.clearInterval(clockTimer)
    clockTimer = null
  }
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
  if (freshTimer !== null) {
    window.clearTimeout(freshTimer)
    freshTimer = null
  }
  clearWsReconnectTimer()

  if (stompClient) {
    stompClient.deactivate().catch(() => {})
    stompClient = null
  }
})
</script>

<style scoped>
/* ==================== 大屏主题变量（暗色，固定 100vh） ==================== */
.calling-display {
  --cd-bg-0: #070d1a;
  --cd-bg-1: #0f172a;
  --cd-bg-2: #131a2e;
  --cd-accent: #fbbf24;
  --cd-accent-soft: rgba(251, 191, 36, 0.5);
  --cd-text: #ffffff;
  --cd-text-dim: rgba(255, 255, 255, 0.62);
  --cd-text-muted: rgba(255, 255, 255, 0.36);

  position: fixed;
  inset: 0;
  box-sizing: border-box;
  padding: clamp(12px, 1.6vh, 26px) clamp(16px, 2vw, 44px) clamp(16px, 2.2vh, 32px);
  display: flex;
  flex-direction: column;
  gap: clamp(10px, 1.4vh, 22px);
  overflow: hidden;
  color: var(--cd-text);
  background:
    radial-gradient(120% 90% at 50% -10%, rgba(251, 191, 36, 0.12), transparent 58%),
    radial-gradient(90% 70% at 8% 110%, rgba(52, 211, 153, 0.1), transparent 60%),
    linear-gradient(160deg, var(--cd-bg-0) 0%, var(--cd-bg-1) 52%, var(--cd-bg-2) 100%);
  font-family: 'DIN Alternate', 'Roboto Mono', 'SF Mono', 'Monaco', 'PingFang SC',
    'Microsoft YaHei', monospace;
  user-select: none;
}

/* ==================== 顶部信息栏 ==================== */
.cd-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.cd-brand {
  display: flex;
  align-items: baseline;
  gap: clamp(10px, 1vw, 18px);
  min-width: 0;
}

.cd-brand-mark {
  font-size: clamp(18px, 1.5vw, 26px);
  font-weight: 800;
  letter-spacing: 0.22em;
  color: var(--cd-accent);
  text-shadow: 0 0 18px var(--cd-accent-soft);
  white-space: nowrap;
}

.cd-store {
  font-size: clamp(14px, 1.1vw, 20px);
  letter-spacing: 0.14em;
  color: var(--cd-text-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cd-header-right {
  display: flex;
  align-items: center;
  gap: clamp(12px, 1.4vw, 24px);
  flex: 0 0 auto;
}

.cd-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-size: clamp(12px, 0.95vw, 16px);
  letter-spacing: 0.12em;
  color: var(--cd-text-muted);
}

.cd-link-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
}

.cd-link.is-live {
  color: #6ee7b7;
  border-color: rgba(110, 231, 183, 0.4);
}

.cd-link.is-live .cd-link-dot {
  background: #34d399;
  box-shadow: 0 0 10px #34d399;
  animation: linkPulse 1.8s ease-in-out infinite;
}

@keyframes linkPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}

.cd-clock {
  font-size: clamp(16px, 1.4vw, 24px);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.1em;
  color: var(--cd-text-dim);
}

/* ==================== 核心区：当前呼叫 ==================== */
.cd-hero {
  position: relative;
  flex: 0 0 auto;
  min-height: clamp(190px, 30vh, 340px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: clamp(8px, 1.4vh, 20px);
  padding: clamp(10px, 1.6vh, 24px) 20px;
  border-radius: 26px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.02));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.cd-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(60% 120% at 50% 0%, rgba(251, 191, 36, 0.16), transparent 70%);
  pointer-events: none;
}

.hero-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(10px, 1.6vw, 30px);
  padding: 0 clamp(10px, 1.4vw, 26px);
  border-radius: 24px;
}

.hero-prefix,
.hero-suffix {
  font-size: clamp(26px, 2.4vw, 46px);
  font-weight: 700;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.8);
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.6);
  white-space: nowrap;
}

/* 号码：96~140px，3~5 米外可读 */
.hero-number {
  font-size: clamp(96px, 13vw, 140px);
  font-weight: 900;
  line-height: 0.95;
  letter-spacing: 0.05em;
  font-variant-numeric: tabular-nums;
  color: var(--cd-accent);
  white-space: nowrap;
  animation: heroBreath 3.2s ease-in-out infinite;
}

.hero-number.is-long {
  font-size: clamp(56px, 7.5vw, 88px);
  letter-spacing: 0.02em;
}

.hero-number.is-xlong {
  font-size: clamp(34px, 4.6vw, 56px);
  letter-spacing: 0.01em;
}

@keyframes heroBreath {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
    text-shadow:
      0 0 22px rgba(251, 191, 36, 0.5),
      0 0 64px rgba(251, 191, 36, 0.24),
      0 6px 18px rgba(0, 0, 0, 0.55);
  }
  50% {
    opacity: 0.93;
    transform: scale(1.018);
    text-shadow:
      0 0 38px rgba(251, 191, 36, 0.78),
      0 0 110px rgba(251, 191, 36, 0.4),
      0 6px 18px rgba(0, 0, 0, 0.55);
  }
}

/* 最近一次呼叫变化：高亮过渡 */
.cd-hero.is-fresh .hero-card {
  animation: heroFlash 1.7s ease-out 1;
}

@keyframes heroFlash {
  0% { box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.55); }
  70% { box-shadow: 0 0 0 22px rgba(251, 191, 36, 0); }
  100% { box-shadow: 0 0 0 0 rgba(251, 191, 36, 0); }
}

.hero-swap-enter-active {
  transition:
    opacity 0.42s ease,
    transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1),
    filter 0.42s ease;
}

.hero-swap-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s ease,
    filter 0.24s ease;
}

.hero-swap-enter-from {
  opacity: 0;
  transform: scale(0.84) translateY(20px);
  filter: blur(6px);
}

.hero-swap-leave-to {
  opacity: 0;
  transform: scale(1.06) translateY(-18px);
  filter: blur(4px);
}

/* 空闲态 */
.hero-idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(8px, 1.2vh, 16px);
  animation: fadeIn 0.5s ease both;
}

.hero-idle-text {
  font-size: clamp(48px, 6vw, 96px);
  font-weight: 900;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.32);
  animation: idlePulse 3.6s ease-in-out infinite;
}

@keyframes idlePulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.82; }
}

.hero-idle-sub {
  font-size: clamp(14px, 1.2vw, 22px);
  letter-spacing: 0.12em;
  color: var(--cd-text-muted);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

.hero-caption {
  position: relative;
  display: flex;
  align-items: center;
  gap: clamp(14px, 1.6vw, 30px);
  flex-wrap: wrap;
  justify-content: center;
  font-size: clamp(14px, 1.2vw, 22px);
  letter-spacing: 0.1em;
  color: var(--cd-text-dim);
}

.hero-tip {
  color: rgba(255, 255, 255, 0.72);
}

.hero-meta {
  display: inline-flex;
  align-items: center;
  gap: clamp(10px, 1vw, 18px);
  font-variant-numeric: tabular-nums;
  color: var(--cd-text-muted);
}

.hero-repeat {
  padding: 2px 12px;
  border-radius: 999px;
  color: #fde68a;
  background: rgba(251, 191, 36, 0.16);
  border: 1px solid rgba(251, 191, 36, 0.35);
  letter-spacing: 0.08em;
}

/* ==================== 两个区块 ==================== */
.cd-boards {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  gap: clamp(12px, 1.4vw, 26px);
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 1024px) {
  .cd-boards {
    flex-direction: column;
  }

  .cd-hero {
    min-height: clamp(150px, 24vh, 240px);
  }
}

@media (max-height: 620px) {
  .hero-number {
    font-size: clamp(72px, 11vw, 96px);
  }

  .cd-hero {
    min-height: 150px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-number,
  .cd-hero.is-fresh .hero-card,
  .hero-idle-text,
  .cd-link.is-live .cd-link-dot {
    animation: none;
  }
}
</style>
