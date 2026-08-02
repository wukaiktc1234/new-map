<script setup lang="ts">
/**
 * 设备监控页面
 * 功能：实时统计设备连接状态、查看最近状态变更、手动触发状态检查
 * 对接后端: /v1/device-alerts/monitor/* (DeviceAlertController 监控接口)
 *           /ws/device (DeviceWebSocketConfig WebSocket 端点)
 *
 * 说明：
 * - 优先使用 WebSocket 实时推送（订阅 /topic/device/status、/topic/device/connection）
 * - WebSocket 连接失败/断开时自动回退到 30 秒 HTTP 轮询（兜底）
 * - 页面顶部显示连接状态指示器：实时（绿）/ 轮询（黄）/ 离线（红）
 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { deviceMonitorApi } from '@/api/device/monitor'
import { deviceApi } from '@/api/device'
import type { DeviceInfo, DeviceStatus } from '@/types/device'
import { DeviceStatusText, DeviceStatusColor } from '@/types/device'
import { useDeviceWebSocket } from '@/composables/useDeviceWebSocket'
import type { DeviceStatusMessage } from '@/composables/useDeviceWebSocket'

type StatColorType = 'primary' | 'success' | 'warning' | 'error' | 'info'

const loading = ref(false)
const checkLoading = ref(false)
const statistics = ref<{ total: number; online: number; offline: number; fault: number; maintenance: number }>({
  total: 0, online: 0, offline: 0, fault: 0, maintenance: 0,
})
const recentChanges = ref<Array<{ deviceId: number; deviceName: string; oldStatus: string; newStatus: string; changeTime: string }>>([])
const onlineDevices = ref<DeviceInfo[]>([])
const autoRefresh = ref(true)
let timer: ReturnType<typeof setInterval> | null = null

/* ===== 统计卡片 ===== */
const stats = computed(() => [
  { key: 'total', icon: 'Monitor', label: '设备总数', value: statistics.value.total, colorType: 'primary' as StatColorType },
  { key: 'online', icon: 'CircleCheck', label: '在线', value: statistics.value.online, colorType: 'success' as StatColorType },
  { key: 'offline', icon: 'CircleClose', label: '离线', value: statistics.value.offline, colorType: 'info' as StatColorType },
  { key: 'fault', icon: 'Warning', label: '故障', value: statistics.value.fault, colorType: 'error' as StatColorType },
  { key: 'maintenance', icon: 'Tools', label: '维护中', value: statistics.value.maintenance, colorType: 'warning' as StatColorType },
])

/* ===== 最近变更表格列 ===== */
const changeColumns = [
  { prop: 'deviceName', label: '设备名称', minWidth: 140 },
  { prop: 'deviceId', label: '设备ID', minWidth: 90 },
  { prop: 'oldStatus', label: '原状态', minWidth: 100, slot: 'oldStatus' },
  { prop: 'newStatus', label: '新状态', minWidth: 100, slot: 'newStatus' },
  { prop: 'changeTime', label: '变更时间', minWidth: 160 },
]

/* ===== 在线设备表格列 ===== */
const onlineColumns = [
  { prop: 'deviceName', label: '设备名称', minWidth: 140 },
  { prop: 'deviceCode', label: '设备编号', minWidth: 130 },
  { prop: 'deviceType', label: '类型', minWidth: 100, slot: 'deviceType' },
  { prop: 'location', label: '安装位置', minWidth: 120 },
  { prop: 'lastHeartbeatTime', label: '最后心跳', minWidth: 160 },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 状态文本辅助（兼容后端返回的字符串状态） ===== */
function normalizeStatusKey(s: string): DeviceStatus | '' {
  const map: Record<string, DeviceStatus> = {
    online: 'online', ONLINE: 'online', Online: 'online', '1': 'online',
    offline: 'offline', OFFLINE: 'offline', Offline: 'offline', '0': 'offline',
    fault: 'fault', FAULT: 'fault', Fault: 'fault', '2': 'fault',
    maintenance: 'maintenance', MAINTENANCE: 'maintenance', '3': 'maintenance',
  }
  return map[s] ?? ''
}

/* ===== 加载统计数据 ===== */
async function loadStatistics() {
  try {
    statistics.value = await deviceMonitorApi.getStatistics()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载统计数据失败')
  }
}

/* ===== 加载最近变更 ===== */
async function loadRecentChanges() {
  try {
    recentChanges.value = await deviceMonitorApi.getRecentStatusChanges(60, 50)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载最近变更失败')
  }
}

/* ===== 加载在线设备列表 ===== */
async function loadOnlineDevices() {
  try {
    onlineDevices.value = await deviceApi.getOnline()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载在线设备失败')
  }
}

/* ===== 一次性加载所有数据 ===== */
async function loadAll() {
  loading.value = true
  await Promise.all([loadStatistics(), loadRecentChanges(), loadOnlineDevices()])
  loading.value = false
}

/* ===== 手动触发状态检查 ===== */
async function handleManualCheck() {
  checkLoading.value = true
  try {
    await deviceMonitorApi.checkStatus()
    ElMessage.success('状态检查已触发')
    await loadAll()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '状态检查失败')
  } finally {
    checkLoading.value = false
  }
}

/* ===== 自动刷新开关 ===== */
function toggleAutoRefresh(val: boolean) {
  autoRefresh.value = val
  if (val) {
    startTimer()
  } else {
    stopTimer()
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    loadStatistics()
    loadRecentChanges()
  }, 30000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

/* ===== WebSocket 实时推送（失败自动回退到上面的 HTTP 轮询） ===== */
const { isConnected: wsConnected, connect, disconnect, subscribeDeviceStatus, subscribeDeviceConnection } = useDeviceWebSocket()

/** 连接模式：实时（WebSocket 接管）/ 轮询（HTTP 兜底）/ 离线 */
const connectionMode = computed<'realtime' | 'polling' | 'offline'>(() => {
  if (wsConnected.value) return 'realtime'
  return autoRefresh.value ? 'polling' : 'offline'
})

/** 连接状态文案 */
const connectionLabel = computed(() => {
  switch (connectionMode.value) {
    case 'realtime': return '实时'
    case 'polling': return '轮询'
    default: return '离线'
  }
})

/** 当前时间格式化为 YYYY-MM-DD HH:mm:ss（用于补全推送记录的变更时间） */
function formatNow(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 设备状态变化推送处理：静默刷新统计、顶部插入变更记录、弹出通知 */
function handleDeviceStatus(message: DeviceStatusMessage): void {
  // 静默刷新统计与在线设备列表（不显示 loading）
  loadStatistics()
  loadOnlineDevices()
  // 在顶部插入新变更记录
  const newRecord = {
    deviceId: message.deviceId ?? 0,
    deviceName: message.deviceName ?? message.deviceType ?? '未知设备',
    oldStatus: '',
    newStatus: message.online != null ? (message.online ? 'online' : 'offline') : '',
    changeTime: message.lastCheckTime ?? formatNow(),
  }
  recentChanges.value = [newRecord, ...recentChanges.value].slice(0, 50)
  ElNotification({
    title: '设备状态已更新',
    message: `${newRecord.deviceName} 状态已变更`,
    type: 'info',
    duration: 3000,
  })
}

/** 设备连接状态推送处理：刷新统计与在线设备、弹出通知 */
function handleDeviceConnection(message: DeviceStatusMessage): void {
  loadStatistics()
  loadOnlineDevices()
  ElNotification({
    title: '设备连接状态已更新',
    message: `${message.deviceName ?? message.deviceType ?? '设备'} ${message.online ? '已连接' : '已断开'}`,
    type: 'info',
    duration: 3000,
  })
}

/**
 * 监听 WebSocket 连接状态，自动切换数据更新方式：
 * - 连接成功：暂停 HTTP 轮询（实时推送接管）
 * - 连接断开：自动回退到 30 秒 HTTP 轮询
 */
watch(wsConnected, (connected) => {
  if (connected) {
    stopTimer()
    autoRefresh.value = false
  } else {
    autoRefresh.value = true
    startTimer()
  }
})

onMounted(() => {
  loadAll()
  // 注册 WebSocket 订阅回调（连接成功后自动生效）
  subscribeDeviceStatus(handleDeviceStatus)
  subscribeDeviceConnection(handleDeviceConnection)
  // 尝试建立 WebSocket 连接（失败/断开时自动回退到轮询）
  connect()
  // 启动 HTTP 轮询作为兜底（WebSocket 连接成功后会自动暂停）
  if (autoRefresh.value) startTimer()
})

onBeforeUnmount(() => {
  stopTimer()
  disconnect()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="设备监控" description="门店外接设备实时连接状态监测">
      <div class="connection-indicator" :class="`connection-indicator--${connectionMode}`" :title="`数据更新方式：${connectionLabel}`">
        <span class="connection-indicator__dot"></span>
        <span class="connection-indicator__text">{{ connectionLabel }}</span>
      </div>
      <el-switch :model-value="autoRefresh" inline-prompt active-text="自动刷新" inactive-text="手动" :disabled="wsConnected" @change="toggleAutoRefresh" />
      <el-button type="primary" size="default" :loading="checkLoading" :icon="Refresh" @click="handleManualCheck">立即检查</el-button>
      <el-button size="default" @click="loadAll">刷新</el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard
        v-for="stat in stats"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <div class="content-grid">
      <div class="grid-item">
        <div class="section-header">
          <h3>最近状态变更（近60分钟）</h3>
          <span class="section-sub">最近 {{ recentChanges.length }} 条</span>
        </div>
        <DataTable :columns="changeColumns" :data="recentChanges" :loading="loading" stripe :actions-width="0">
          <template #oldStatus="{ row }">
            <StatusTag v-if="normalizeStatusKey(row.oldStatus)" :status="DeviceStatusColor[normalizeStatusKey(row.oldStatus) as DeviceStatus]" :label="DeviceStatusText[normalizeStatusKey(row.oldStatus) as DeviceStatus]" size="small" />
            <span v-else>{{ row.oldStatus || '-' }}</span>
          </template>
          <template #newStatus="{ row }">
            <StatusTag v-if="normalizeStatusKey(row.newStatus)" :status="DeviceStatusColor[normalizeStatusKey(row.newStatus) as DeviceStatus]" :label="DeviceStatusText[normalizeStatusKey(row.newStatus) as DeviceStatus]" size="small" />
            <span v-else>{{ row.newStatus || '-' }}</span>
          </template>
        </DataTable>
      </div>

      <div class="grid-item">
        <div class="section-header">
          <h3>当前在线设备</h3>
          <span class="section-sub">共 {{ onlineDevices.length }} 台</span>
        </div>
        <DataTable :columns="onlineColumns" :data="onlineDevices" :loading="loading" stripe :actions-width="0">
          <template #deviceType="{ row }">
            <StatusTag category="type" :label="row.deviceType" size="small" />
          </template>
          <template #status="{ row }">
            <StatusTag :status="DeviceStatusColor[row.status as DeviceStatus]" :label="DeviceStatusText[row.status as DeviceStatus]" size="small" />
          </template>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<style scoped>
.connection-indicator {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-1) var(--fts-space-2);
  border-radius: var(--fts-radius-md);
  font-size: 12px;
  color: var(--fts-text-secondary);
  background: var(--fts-bg-secondary);
}

.connection-indicator__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.connection-indicator--realtime .connection-indicator__dot {
  background: var(--fts-success);
}

.connection-indicator--polling .connection-indicator__dot {
  background: var(--fts-warning);
}

.connection-indicator--offline .connection-indicator__dot {
  background: var(--fts-error);
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-4);
  margin-top: var(--fts-space-4);
}

.grid-item {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg, 8px);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-light);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
}

.section-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--fts-text-primary);
}

.section-sub {
  font-size: 12px;
  color: var(--fts-text-secondary);
}

.stats-section {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
}

.stats-section :deep(.stat-card) {
  flex: 1;
  min-width: 140px;
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
