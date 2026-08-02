<script setup lang="ts">
/**
 * 预警指挥中心 - L4响应层升级版
 * 全局运营事件的统一接收、分级、分发与跟踪
 * 升级特性：SLA计时器、事件升级、批量操作、右侧抽屉工作台、自动刷新+声音提醒
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  ElMessage, ElMessageBox, ElSwitch, ElButton,
  ElSelect, ElOption, ElInput, ElDrawer, ElTimeline, ElTimelineItem
} from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { alertApi } from '@/api/operations/alert'
import type { AlertItem, AlertLevel, AlertStatus, AlertType } from '@/api/operations/alert'

const { pagination } = useStandardPage()
const loading = ref(false)

// 门店下拉选项（接入真实后端 API：/v1/store-operation/stores/active）
const { storeOptions, getStoreName } = useStoreOptions(true)

/** 从 CSS 变量获取颜色（避免硬编码颜色值） */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/* ===== Tab 状态（参考税务管理 el-tabs border-card 用法） ===== */
const activeTab = ref<'all' | 'escalated' | 'resolved'>('all')

/** Tab 切换回调（参考税务管理 handleTabChange） */
function handleTabChange(_tab: string | number): void {
  // 切换Tab时自动重置部分搜索条件，让对应Tab的内容更聚焦
  if (_tab === 'escalated') {
    searchForm.value.level = 'error'
  } else if (_tab === 'resolved') {
    searchForm.value.status = 'resolved'
  } else {
    // 全部预警：保留筛选条件
  }
}

/** 已升级为严重的预警（level=error 且未解决） */
const escalatedAlerts = computed(() =>
  alertList.value.filter(a => a.level === 'error' && a.status !== 'resolved')
)

/** 已解决的预警 */
const resolvedAlerts = computed(() =>
  alertList.value.filter(a => a.status === 'resolved')
)

// ==================== 自动刷新 & 声音提醒 ====================
const autoRefresh = ref(true)
const soundEnabled = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let slaTimer: ReturnType<typeof setInterval> | null = null
const slaClock = ref(0) // 触发SLA列的响应式更新

// ==================== 搜索筛选 ====================
interface SearchForm {
  type: string
  level: string
  store: string
  status: string
}
const searchForm = ref<SearchForm>({
  type: '',
  level: '',
  store: '',
  status: ''
})

// 筛选选项
const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '营收异常', value: 'revenue' },
  { label: '员工缺岗', value: 'staff' },
  { label: '设备故障', value: 'device' },
  { label: '库存预警', value: 'inventory' },
  { label: '证件临期', value: 'certificate' },
  { label: '食品安全', value: 'food_safety' },
  { label: '客诉激增', value: 'complaint' }
]

const levelOptions = [
  { label: '全部级别', value: '' },
  { label: '严重', value: 'error' },
  { label: '警告', value: 'warning' },
  { label: '提示', value: 'info' }
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已解决', value: 'resolved' },
  { label: '已忽略', value: 'ignored' }
]

// ==================== 数据列表 ====================
const alertList = ref<AlertItem[]>([])
const selectedRows = ref<AlertItem[]>([])

/** 过滤后的列表 */
const filteredAlerts = computed(() => {
  let result = alertList.value
  if (searchForm.value.type) result = result.filter(item => item.type === searchForm.value.type)
  if (searchForm.value.level) result = result.filter(item => item.level === searchForm.value.level)
  if (searchForm.value.store) {
    // searchForm.store 存储的是 storeId，需通过 composable 转换为门店名称进行匹配
    const storeName = getStoreName(searchForm.value.store)
    result = result.filter(item => item.storeName === storeName)
  }
  if (searchForm.value.status) result = result.filter(item => item.status === searchForm.value.status)
  return result
})

// ==================== 统计卡片 ====================
const statsCards = computed(() => {
  const pending = alertList.value.filter(a => a.status === 'pending').length
  const processing = alertList.value.filter(a => a.status === 'processing').length
  const resolvedToday = alertList.value.filter(a => a.status === 'resolved').length
  // 计算平均响应时间（分钟）：取所有非resolved/ignored的alert的平均等待时间
  const activeAlerts = alertList.value.filter(a => !['resolved', 'ignored'].includes(a.status))
  const avgResponse = activeAlerts.length > 0
    ? Math.round(activeAlerts.reduce((sum, a) => sum + (Date.now() - new Date(a.triggerTime).getTime()), 0) / activeAlerts.length / 60000)
    : 0

  return [
    { icon: 'Warning', label: '待处理', value: pending, colorType: 'error' as const },
    { icon: 'Timer', label: '处理中', value: processing, colorType: 'warning' as const },
    { icon: 'CircleCheck', label: '今日已解决', value: resolvedToday, colorType: 'success' as const },
    { icon: 'Clock', label: '平均响应时间', value: `${avgResponse}min`, colorType: 'info' as const }
  ]
})

// ==================== SLA 计时器工具函数 ====================
/** 格式化SLA等待时长 */
function formatSlaDuration(triggerTimeStr: string): { text: string; isOverdue: boolean; isWarning: boolean } {
  const elapsed = Date.now() - new Date(triggerTimeStr).getTime()
  const totalSeconds = Math.floor(elapsed / 1000)
  if (totalSeconds < 0) return { text: '0秒', isOverdue: false, isWarning: false }

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  let text = ''
  if (hours > 0) text = `${hours}小时${minutes}分`
  else if (minutes > 0) text = `${minutes}分${seconds}秒`
  else text = `${seconds}秒`

  const totalMinutes = elapsed / 60000
  return {
    text,
    isOverdue: totalMinutes > 30,
    isWarning: totalMinutes >= 15 && totalMinutes <= 30
  }
}

/** 获取SLA颜色 */
function getSlaColor(triggerTimeStr: string): string {
  const { isOverdue, isWarning } = formatSlaDuration(triggerTimeStr)
  if (isOverdue) return 'var(--fts-error)'
  if (isWarning) return 'var(--fts-warning)'
  return 'var(--fts-success)'
}

// ==================== 类型配置映射 ====================
const typeLabelMap: Record<AlertType, string> = {
  revenue: '营收异常', staff: '员工缺岗', device: '设备故障',
  inventory: '库存预警', certificate: '证件临期', food_safety: '食品安全', complaint: '客诉激增'
}

const typeStatusMap: Record<AlertType, string> = {
  revenue: 'error', staff: 'warning', device: 'error',
  inventory: 'warning', certificate: 'warning', food_safety: 'error', complaint: 'warning'
}

const levelLabelMap: Record<AlertLevel, string> = {
  error: '严重', warning: '警告', info: '提示'
}

const levelDotColor = computed<Record<AlertLevel, string>>(() => ({
  error: getCssVar('--fts-error'),
  warning: getCssVar('--fts-warning'),
  info: getCssVar('--fts-info')
}))

const statusLabelMap: Record<AlertStatus, string> = {
  pending: '待处理', processing: '处理中', resolved: '已解决', ignored: '已忽略'
}

const statusTagMap: Record<AlertStatus, string> = {
  pending: 'pending', processing: 'in_progress', resolved: 'completed', ignored: 'inactive'
}

// ==================== 详情工作台抽屉 ====================
const drawerVisible = ref(false)
const currentAlert = ref<AlertItem | null>(null)
const solutionNote = ref('')
const resolveDialogVisible = ref(false)

/** 打开详情抽屉 */
function openDrawer(alert: AlertItem) {
  currentAlert.value = alert
  solutionNote.value = ''
  drawerVisible.value = true
}

/** 标记处理中（调用后端 acknowledge 接口，ACTIVE → ACKNOWLEDGED） */
async function markProcessing() {
  if (!currentAlert.value) return
  const alertId = currentAlert.value.id
  try {
    const updated = await alertApi.acknowledgeAlert(alertId)
    // 同步本地列表（API 内部已处理 fallback，此处统一更新本地状态）
    const target = alertList.value.find(a => a.id === alertId)
    if (target) {
      target.status = 'processing'
      target.processHistory = target.processHistory || []
      target.processHistory.push({
        time: new Date().toLocaleString('zh-CN'),
        action: '标记为处理中',
        operator: '当前用户'
      })
    }
    // 同步抽屉内的当前预警
    if (updated) {
      currentAlert.value = { ...currentAlert.value, status: 'processing', processHistory: updated.processHistory || currentAlert.value.processHistory }
    }
    ElMessage.success('已标记为处理中')
  } catch {
    ElMessage.error('标记处理中失败')
  }
}

/** 标记已解决（调用后端 resolve 接口，ACKNOWLEDGED → RESOLVED） */
async function confirmResolve() {
  if (!currentAlert.value) return
  if (!solutionNote.value.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  const alertId = currentAlert.value.id
  const note = solutionNote.value
  try {
    await alertApi.resolveAlert(alertId, note)
    // 同步本地列表
    const target = alertList.value.find(a => a.id === alertId)
    if (target) {
      target.status = 'resolved'
      target.processHistory = target.processHistory || []
      target.processHistory.push({
        time: new Date().toLocaleString('zh-CN'),
        action: `已解决：${note}`,
        operator: '当前用户'
      })
    }
    resolveDialogVisible.value = false
    drawerVisible.value = false
    solutionNote.value = ''
    ElMessage.success('预警已标记为已解决')
  } catch {
    ElMessage.error('标记已解决失败')
  }
}

/** 升级为严重 */
async function escalateToError() {
  if (!currentAlert.value || currentAlert.value.level !== 'warning') return
  try {
    await ElMessageBox.confirm(
      `确认将此预警从「警告」升级为「严重」？\n\n升级后将提高优先级并通知管理层。`,
      '事件升级确认',
      { confirmButtonText: '确认升级', cancelButtonText: '取消', type: 'warning' }
    )
    const target = alertList.value.find(a => a.id === currentAlert.value!.id)
    if (target) {
      target.level = 'error'
      target.processHistory = target.processHistory || []
      target.processHistory.push({
        time: new Date().toLocaleString('zh-CN'),
        action: '事件级别由警告升级为严重',
        operator: '当前用户'
      })
      // 声音提醒
      if (soundEnabled.value) playNotificationSound()
      ElMessage.success('已升级为严重级别')
    }
  } catch {
    // 用户取消
  }
}

/** 忽略单条预警 */
async function dismissAlert(alert: AlertItem) {
  try {
    await ElMessageBox.confirm(`确认忽略此预警：「${alert.message.slice(0, 30)}...」？`, '忽略确认', {
      confirmButtonText: '确认忽略', cancelButtonText: '取消', type: 'info'
    })
    const target = alertList.value.find(a => a.id === alert.id)
    if (target) {
      target.status = 'ignored'
      ElMessage.success('已忽略该预警')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 批量操作 ====================
async function batchIgnore() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要忽略的预警')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认忽略选中的 ${selectedRows.value.length} 条预警？`,
      '批量忽略确认',
      { confirmButtonText: '确认忽略', cancelButtonText: '取消', type: 'warning' }
    )
    selectedRows.value.forEach(selected => {
      const target = alertList.value.find(a => a.id === selected.id)
      if (target) target.status = 'ignored'
    })
    ElMessage.success(`已忽略 ${selectedRows.value.length} 条预警`)
    selectedRows.value = []
  } catch {
    // 用户取消
  }
}

function handleSelectionChange(selection: AlertItem[]) {
  selectedRows.value = selection
}

// ==================== 声音提醒 ====================
function playNotificationSound() {
  try {
    const audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)()
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    oscillator.frequency.value = 800
    oscillator.type = 'sine'
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.001, audioContext.currentTime + 0.5)
    oscillator.start(audioContext.currentTime)
    oscillator.stop(audioContext.currentTime + 0.5)
  } catch {
    // 音频API不可用时静默失败
  }
}

// ==================== 搜索 & 刷新 ====================
function handleSearch() {
  // 响应式computed会自动过滤
}

function handleReset() {
  searchForm.value = { type: '', level: '', store: '', status: '' }
}

async function handleRefresh() {
  loading.value = true
  try {
    // 调用真实 API（内部已实现 Mock fallback），拉取全量数据交由前端 filteredAlerts 过滤
    const result = await alertApi.getAlerts({ page: 1, size: 100 })
    alertList.value = result.records
    ElMessage.success('数据已刷新')
  } catch {
    // alertApi 内部已有 fallback，理论上不会抛出；此处兜底防御
    ElMessage.error('数据刷新失败')
  } finally {
    loading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await handleRefresh()

  // SLA计时器：每秒刷新一次SLA显示
  slaTimer = setInterval(() => {
    slaClock.value++
  }, 1000)

  // 自动刷新定时器
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      handleRefresh()
      if (soundEnabled.value && alertList.value.some(a => a.status === 'pending' && a.level === 'error')) {
        playNotificationSound()
      }
    }, 60000)
  }
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (slaTimer) clearInterval(slaTimer)
})
</script>

<template>
  <div class="modern-page">
    <!-- ====== 页面头部 ====== -->
    <PageHeader title="预警管理" description="全局运营事件的统一接收、分级、分发与跟踪">
      <template #extra>
        <div class="header-switches">
          <span class="switch-label">自动刷新</span>
          <ElSwitch v-model="autoRefresh" size="small" />
          <span class="switch-label">声音提醒</span>
          <ElSwitch v-model="soundEnabled" size="small" />
          <ElButton :icon="RefreshRight" circle size="small" :loading="loading" @click="handleRefresh" />
        </div>
      </template>
    </PageHeader>

    <!-- ====== 统计概览卡片（4列，参考税务管理 stats-section 规范，强制4列不响应式收缩） ====== -->
    <section class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
        :class="{ 'stat-card--pulse': stat.colorType === 'error' && stat.value > 0 }"
      />
    </section>

    <!-- ====== Tab 切换（参考税务管理 el-tabs border-card 结构） ====== -->
    <section class="tab-section">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- Tab 1: 全部预警（搜索+主表格） -->
        <el-tab-pane :label="`全部预警 (${alertList.length})`" name="all">
          <div class="advanced-search-panel">
            <div class="toolbar-row">
              <div class="toolbar-left">
                <ElSelect v-model="searchForm.type" placeholder="预警类型" clearable style="width: 140px">
                  <ElOption v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </ElSelect>
                <ElSelect v-model="searchForm.level" placeholder="级别" clearable style="width: 120px">
                  <ElOption v-for="opt in levelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </ElSelect>
                <ElSelect v-model="searchForm.store" placeholder="门店" clearable style="width: 130px">
                  <ElOption v-for="store in storeOptions" :key="store.storeId" :label="store.storeName" :value="String(store.storeId)" />
                </ElSelect>
                <ElSelect v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
                  <ElOption v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </ElSelect>
              </div>
              <div class="toolbar-right">
                <ElButton type="primary" @click="handleSearch">查询</ElButton>
                <ElButton @click="handleReset">重置</ElButton>
                <ElButton type="warning" :disabled="selectedRows.length === 0" @click="batchIgnore">
                  批量忽略 ({{ selectedRows.length }})
                </ElButton>
              </div>
            </div>
          </div>

          <div class="table-section">
            <DataTable
              :columns="[
                { prop: 'level', label: '级别', width: 80, align: 'center', slot: 'level' },
                { prop: 'storeName', label: '关联门店', width: 110 },
                { prop: 'type', label: '预警类型', width: 110, slot: 'typeSlot' },
                { prop: 'message', label: '预警描述', minWidth: 250, showOverflowTooltip: true },
                { prop: 'source', label: '来源', width: 100 },
                { prop: 'triggerTime', label: '触发时间', width: 160, slot: 'triggerTime' },
                { prop: 'slaTime', label: '等待时长', width: 110, align: 'center', slot: 'slaTime' },
                { prop: 'status', label: '状态', width: 90, slot: 'statusSlot' }
              ]"
              :data="filteredAlerts"
              :loading="loading"
              stripe
              hover
              :actions-width="200"
              @selection-change="handleSelectionChange"
              @row-click="openDrawer"
            >
              <template #level="{ row }">
                <span class="level-badge">
                  <span class="level-badge__dot" :style="{ backgroundColor: levelDotColor[row.level as AlertLevel] }" />
                  {{ levelLabelMap[row.level as AlertLevel] }}
                </span>
              </template>
              <template #typeSlot="{ row }">
                <StatusTag :status="typeStatusMap[row.type as AlertType]" :label="typeLabelMap[row.type as AlertType]" size="small" />
              </template>
              <template #triggerTime="{ row }">
                {{ new Date(row.triggerTime).toLocaleString('zh-CN') }}
              </template>
              <template #slaTime="{ row }">
                <span
                  class="sla-timer"
                  :class="{
                    'sla-timer--overdue': formatSlaDuration(row.triggerTime).isOverdue,
                    'sla-timer--warning': formatSlaDuration(row.triggerTime).isWarning
                  }"
                  :style="{ color: getSlaColor(row.triggerTime) }"
                >
                  {{ formatSlaDuration(row.triggerTime).text }}
                </span>
              </template>
              <template #statusSlot="{ row }">
                <StatusTag :status="statusTagMap[row.status as AlertStatus]" :label="statusLabelMap[row.status as AlertStatus]" size="small" />
              </template>
              <template #actions="{ row }">
                <ElButton link type="primary" size="small" @click.stop="openDrawer(row)">处理</ElButton>
                <ElButton
                  v-if="row.level === 'warning'"
                  link
                  type="warning"
                  size="small"
                  @click.stop="escalateToError(); openDrawer(row)"
                >升级</ElButton>
                <ElButton
                  v-if="row.status === 'pending' || row.status === 'processing'"
                  link
                  type="info"
                  size="small"
                  @click.stop="dismissAlert(row)"
                >忽略</ElButton>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 严重升级（仅显示 level=error 且未解决） -->
        <el-tab-pane :label="`严重升级 (${escalatedAlerts.length})`" name="escalated">
          <div class="table-section">
            <DataTable
              :columns="[
                { prop: 'level', label: '级别', width: 80, align: 'center', slot: 'level' },
                { prop: 'storeName', label: '关联门店', width: 110 },
                { prop: 'type', label: '预警类型', width: 110, slot: 'typeSlot' },
                { prop: 'message', label: '预警描述', minWidth: 250, showOverflowTooltip: true },
                { prop: 'triggerTime', label: '触发时间', width: 160, slot: 'triggerTime' },
                { prop: 'slaTime', label: '等待时长', width: 110, align: 'center', slot: 'slaTime' },
                { prop: 'status', label: '状态', width: 90, slot: 'statusSlot' }
              ]"
              :data="escalatedAlerts"
              :loading="loading"
              stripe
              hover
              :actions-width="200"
              @row-click="openDrawer"
            >
              <template #level="{ row }">
                <span class="level-badge">
                  <span class="level-badge__dot" :style="{ backgroundColor: levelDotColor[row.level as AlertLevel] }" />
                  {{ levelLabelMap[row.level as AlertLevel] }}
                </span>
              </template>
              <template #typeSlot="{ row }">
                <StatusTag :status="typeStatusMap[row.type as AlertType]" :label="typeLabelMap[row.type as AlertType]" size="small" />
              </template>
              <template #triggerTime="{ row }">
                {{ new Date(row.triggerTime).toLocaleString('zh-CN') }}
              </template>
              <template #slaTime="{ row }">
                <span
                  class="sla-timer"
                  :class="{ 'sla-timer--overdue': formatSlaDuration(row.triggerTime).isOverdue }"
                  :style="{ color: getSlaColor(row.triggerTime) }"
                >
                  {{ formatSlaDuration(row.triggerTime).text }}
                </span>
              </template>
              <template #statusSlot="{ row }">
                <StatusTag :status="statusTagMap[row.status as AlertStatus]" :label="statusLabelMap[row.status as AlertStatus]" size="small" />
              </template>
              <template #actions="{ row }">
                <ElButton link type="primary" size="small" @click.stop="openDrawer(row)">处理</ElButton>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- Tab 3: 已解决（仅显示 status=resolved） -->
        <el-tab-pane :label="`已解决 (${resolvedAlerts.length})`" name="resolved">
          <div class="table-section">
            <DataTable
              :columns="[
                { prop: 'level', label: '级别', width: 80, align: 'center', slot: 'level' },
                { prop: 'storeName', label: '关联门店', width: 110 },
                { prop: 'type', label: '预警类型', width: 110, slot: 'typeSlot' },
                { prop: 'message', label: '预警描述', minWidth: 250, showOverflowTooltip: true },
                { prop: 'triggerTime', label: '触发时间', width: 160, slot: 'triggerTime' },
                { prop: 'status', label: '状态', width: 90, slot: 'statusSlot' }
              ]"
              :data="resolvedAlerts"
              :loading="loading"
              stripe
              hover
              :actions-width="120"
              @row-click="openDrawer"
            >
              <template #level="{ row }">
                <span class="level-badge">
                  <span class="level-badge__dot" :style="{ backgroundColor: levelDotColor[row.level as AlertLevel] }" />
                  {{ levelLabelMap[row.level as AlertLevel] }}
                </span>
              </template>
              <template #typeSlot="{ row }">
                <StatusTag :status="typeStatusMap[row.type as AlertType]" :label="typeLabelMap[row.type as AlertType]" size="small" />
              </template>
              <template #triggerTime="{ row }">
                {{ new Date(row.triggerTime).toLocaleString('zh-CN') }}
              </template>
              <template #statusSlot="{ row }">
                <StatusTag :status="statusTagMap[row.status as AlertStatus]" :label="statusLabelMap[row.status as AlertStatus]" size="small" />
              </template>
              <template #actions="{ row }">
                <ElButton link type="primary" size="small" @click.stop="openDrawer(row)">查看</ElButton>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <!-- ====== 详情工作台抽屉 ====== -->
    <ElDrawer
      v-model="drawerVisible"
      title="预警详情工作台"
      direction="rtl"
      size="480px"
      :destroy-on-close="true"
      class="alert-drawer"
    >
      <template v-if="currentAlert">
        <!-- 抽屉头部信息 -->
        <div class="drawer-header-info">
          <span class="level-badge" :class="`level-badge--${currentAlert.level}`">
            <span class="level-badge__dot" :style="{ backgroundColor: levelDotColor[currentAlert.level] }" />
            {{ levelLabelMap[currentAlert.level] }}
          </span>
          <StatusTag :status="typeStatusMap[currentAlert.type]" :label="typeLabelMap[currentAlert.type]" size="medium" />
          <span class="drawer-store-name">{{ currentAlert.storeName }}</span>
        </div>

        <!-- 基本信息 -->
        <div class="drawer-section">
          <h4 class="drawer-section__title">基本信息</h4>
          <div class="drawer-info-grid">
            <div class="drawer-info-item">
              <span class="drawer-info-item__label">触发时间</span>
              <span class="drawer-info-item__value">{{ new Date(currentAlert.triggerTime).toLocaleString('zh-CN') }}</span>
            </div>
            <div class="drawer-info-item">
              <span class="drawer-info-item__label">来源</span>
              <span class="drawer-info-item__value">{{ currentAlert.source }}</span>
            </div>
            <div class="drawer-info-item">
              <span class="drawer-info-item__label">关联规则</span>
              <span class="drawer-info-item__value">{{ currentAlert.ruleName }}</span>
            </div>
            <div class="drawer-info-item">
              <span class="drawer-info-item__label">等待时长</span>
              <span class="drawer-info-item__value" :style="{ color: getSlaColor(currentAlert.triggerTime), fontWeight: formatSlaDuration(currentAlert.triggerTime).isOverdue ? 'bold' : 'normal' }">
                {{ formatSlaDuration(currentAlert.triggerTime).text }}
              </span>
            </div>
          </div>
        </div>

        <!-- 详细描述 -->
        <div class="drawer-section">
          <h4 class="drawer-section__title">详细描述</h4>
          <p class="drawer-description">{{ currentAlert.message }}</p>
        </div>

        <!-- 影响范围 -->
        <div class="drawer-section">
          <h4 class="drawer-section__title">影响范围</h4>
          <p class="drawer-scope">{{ currentAlert.affectedScope }}</p>
        </div>

        <!-- 处理记录 -->
        <div class="drawer-section">
          <h4 class="drawer-section__title">处理记录</h4>
          <div v-if="currentAlert.processHistory && currentAlert.processHistory.length > 0" class="drawer-timeline">
            <ElTimeline>
              <ElTimelineItem
                v-for="(record, idx) in currentAlert.processHistory"
                :key="idx"
                :timestamp="record.time"
                placement="top"
              >
                <p>{{ record.action }}</p>
                <p class="timeline-operator">操作人：{{ record.operator }}</p>
              </ElTimelineItem>
            </ElTimeline>
          </div>
          <div v-else class="drawer-empty-record">暂无处理记录</div>
        </div>
      </template>

      <!-- 底部操作按钮 -->
      <template #footer>
        <div class="drawer-footer">
          <ElButton @click="markProcessing" :disabled="currentAlert?.status === 'processing'">
            标记处理中
          </ElButton>
          <ElButton type="success" @click="resolveDialogVisible = true" :disabled="currentAlert?.status === 'resolved'">
            标记已解决
          </ElButton>
          <ElButton
            v-if="currentAlert?.level === 'warning'"
            type="warning"
            @click="escalateToError"
          >
            升级为严重
          </ElButton>
          <ElButton @click="drawerVisible = false">关闭</ElButton>
        </div>
      </template>
    </ElDrawer>

    <!-- 已解决对话框 -->
    <el-dialog
      v-model="resolveDialogVisible"
      title="标记为已解决"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-input
        v-model="solutionNote"
        type="textarea"
        :rows="3"
        placeholder="请输入处理说明..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="confirmResolve">确认解决</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ========== 头部开关组 ========== */
.header-switches {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.switch-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  white-space: nowrap;
}

/* 待处理脉冲动画 */
.stat-card--pulse :deep(.stat-card__value) {
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.65; }
}

/* ========== Tab 内独立表格区域（覆盖全局 .table-section 套件样式） ========== */
.tab-section .table-section {
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  border-top: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-4);
  flex: 1;
  min-height: 400px;
}

/* ========== 级别标签 ========== */
.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
}

.level-badge__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ========== SLA 计时器 ========== */
.sla-timer {
  font-size: var(--fts-font-size-sm);
  font-family: var(--fts-font-mono, 'SF Mono', Consolas, monospace);
  font-variant-numeric: tabular-nums;
  transition: color var(--fts-duration-fast);
}

.sla-timer--overdue {
  font-weight: bold;
}

.sla-timer--warning {
  font-weight: var(--fts-font-weight-medium);
}

/* ========== 抽屉样式 ========== */
.drawer-header-info {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-5);
  padding-bottom: var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.drawer-store-name {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.drawer-section {
  margin-bottom: var(--fts-space-5);
}

.drawer-section__title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3) 0;
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
}

.drawer-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);
}

.drawer-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-info-item__label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
}

.drawer-info-item__value {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  word-break: break-all;
}

.drawer-description {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  line-height: var(--fts-line-height-relaxed);
  background: var(--fts-bg-secondary);
  padding: var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  margin: 0;
}

.drawer-scope {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: var(--fts-line-height-relaxed);
  margin: 0;
}

.drawer-empty-record {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-disabled);
  text-align: center;
  padding: var(--fts-space-6) 0;
}

.timeline-operator {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  margin: 0;
}

/* ========== 抽屉底部操作栏 ========== */
.drawer-footer {
  display: flex;
  gap: var(--fts-space-2);
  justify-content: flex-end;
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .drawer-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
