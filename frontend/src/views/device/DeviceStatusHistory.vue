<script setup lang="ts">
/**
 * 设备状态历史页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】查看设备运行状态的历史记录
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search, Refresh, View } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { deviceHistoryApi } from '@/api/device/history'
import { DeviceTypeOptions, DeviceTypeText } from '@/types/device'
import type { DeviceStatusHistoryInfo } from '@/types/device'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
}

/** 状态类型枚举 */
type StatusType = 'online' | 'offline' | 'fault' | 'maintenance'

// ==================== 响应式数据 ====================

const detailVisible = ref(false)
const currentDetail = ref<DeviceStatusHistoryInfo | null>(null)
const exportLoading = ref(false)

/** 门店选项（接入真实后端 API） */
const { storeOptions, getStoreName } = useStoreOptions(true)

/** 状态类型选项 */
const statusTypeOptions = [
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' },
  { label: '故障', value: 'fault' },
  { label: '维护中', value: 'maintenance' },
] as const

// 查询表单
const queryForm = ref({
  keyword: '',
  deviceType: '' as string,
  storeId: '' as number | string,
  statusType: '' as '' | StatusType,
  timeRange: [] as string[],
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<DeviceStatusHistoryInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const startTime = params.timeRange?.[0]
      const endTime = params.timeRange?.[1]
      const query: Record<string, unknown> = {
        pageSize: params.size,
        pageNum: params.page,
      }
      if (params.deviceType) query.deviceType = params.deviceType
      if (params.storeId && params.storeId !== '') query.storeId = Number(params.storeId)
      if (startTime) query.startTime = startTime
      if (endTime) query.endTime = endTime
      return deviceHistoryApi.getByDeviceType(query as unknown as Parameters<typeof deviceHistoryApi.getByDeviceType>[0])
    },
  } as unknown as CrudApi<DeviceStatusHistoryInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  online: tableData.value.filter(item => item.online).length,
  offline: tableData.value.filter(item => !item.online).length,
  error: tableData.value.filter(item => item.errorMessage && item.errorMessage.trim() !== '').length,
}))

// 表格列定义
const columns = computed<ColumnDef[]>(() => [
  { prop: 'id', label: '记录编号', minWidth: 100 },
  { prop: 'deviceName', label: '设备名称', minWidth: 140, slot: 'deviceName' },
  { prop: 'deviceId', label: '设备编号', minWidth: 100 },
  { prop: 'deviceType', label: '设备类型', minWidth: 100, slot: 'deviceType' },
  { prop: 'storeId', label: '所属门店', minWidth: 120, slot: 'storeName' },
  { prop: 'online', label: '状态类型', minWidth: 100, slot: 'statusType' },
  { prop: 'responseTime', label: '状态值', minWidth: 100, slot: 'statusValue' },
  { prop: 'checkTime', label: '开始时间', minWidth: 160, slot: 'startTime' },
  { prop: 'checkTime', label: '结束时间', minWidth: 160, slot: 'endTime' },
  { prop: 'responseTime', label: '持续时长', minWidth: 100, slot: 'duration' },
  { prop: '_operation', label: '操作', width: 100, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.deviceType = ''
  queryForm.value.storeId = ''
  queryForm.value.statusType = ''
  queryForm.value.timeRange = []
  refresh()
}

/**
 * 查看详情
 */
function handleViewDetail(row: DeviceStatusHistoryInfo) {
  currentDetail.value = row
  detailVisible.value = true
}

/**
 * 导出历史记录
 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const startTime = queryForm.value.timeRange?.[0]
    const endTime = queryForm.value.timeRange?.[1]

    const params = {
      deviceType: queryForm.value.deviceType || undefined,
      storeId: queryForm.value.storeId ? Number(queryForm.value.storeId) : undefined,
      startTime,
      endTime,
    }

    const allRecords: DeviceStatusHistoryInfo[] = []
    let pageNum = 1
    const pageSize = 1000

    while (true) {
      const res = await deviceHistoryApi.getByDeviceType({
        ...params,
        pageNum,
        pageSize,
      })
      allRecords.push(...(res.records || []))
      if (allRecords.length >= res.total || !res.records || res.records.length < pageSize) {
        break
      }
      pageNum++
    }

    const csvContent = generateCSV(allRecords)
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `设备状态历史_${timestamp}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/**
 * 生成CSV内容
 */
function generateCSV(records: DeviceStatusHistoryInfo[]): string {
  const headers = ['记录编号', '设备名称', '设备编号', '设备类型', '所属门店', '状态类型', '响应时间(ms)', 'IP地址', '检查时间', '错误信息']
  const rows = records.map(item => [
    item.id,
    item.deviceName,
    item.deviceId,
    getDeviceTypeLabel(item.deviceType),
    getStoreName(item.storeId),
    item.online ? '在线' : '离线',
    item.responseTime,
    item.ipAddress,
    item.checkTime,
    item.errorMessage,
  ])
  return [headers, ...rows].map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
}

/** 获取设备类型文本 */
function getDeviceTypeLabel(type: string): string {
  return (DeviceTypeText as Record<string, string>)[type] || type
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 状态类型颜色映射 */
function getStatusTypeColor(online: boolean): string {
  return online ? 'online' : 'offline'
}

/** 状态类型标签 */
function getStatusTypeLabel(online: boolean): string {
  return online ? '在线' : '离线'
}

/** 状态趋势数据项 */
interface TrendDataItem {
  time: string
  online: boolean
  responseTime: number
}

// TODO: 后端暂无设备状态趋势 API，待接口就绪后接入 deviceHistoryApi.getTrend(deviceId)
// 当前移除了基于 Math.random() 生成的 mock 趋势数据，避免展示虚假数据
const trendData = ref<TrendDataItem[]>([])

onMounted(() => {
  // 门店选项由 useStoreOptions(true) 自动加载，无需手动调用
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="设备状态历史" description="查看设备运行状态的历史记录">
      <el-button type="primary" size="default" :loading="exportLoading" @click="handleExport">
        <el-icon :size="16"><Download /></el-icon>导出历史记录
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="记录总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="在线记录" :value="String(statistics.online)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="离线记录" :value="String(statistics.offline)" color-type="warning" variant="bordered" />
      <StatCard icon="Warning" label="异常记录" :value="String(statistics.error)" color-type="error" variant="bordered" />
    </section>

    <!-- 搜索栏 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索设备名称或编号..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.deviceType"
            placeholder="设备类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in DeviceTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.storeId"
            placeholder="所属门店"
            clearable
            filterable
            style="width: 160px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="store.storeId"
            />
          </el-select>
          <el-select
            v-model="queryForm.statusType"
            placeholder="状态类型"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="false"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
      >
        <!-- 设备名称列 -->
        <template #deviceName="{ row }">
          <span class="device-name">{{ row.deviceName }}</span>
        </template>

        <!-- 设备类型列 -->
        <template #deviceType="{ row }">
          <span class="type-text">{{ getDeviceTypeLabel(row.deviceType) }}</span>
        </template>

        <!-- 所属门店列 -->
        <template #storeName="{ row }">
          <span class="store-text">{{ getStoreName(row.storeId) }}</span>
        </template>

        <!-- 状态类型列 -->
        <template #statusType="{ row }">
          <StatusTag :status="getStatusTypeColor(row.online)" :label="getStatusTypeLabel(row.online)" size="small" variant="light" />
        </template>

        <!-- 状态值列 -->
        <template #statusValue="{ row }">
          <span class="response-time">{{ row.responseTime }} ms</span>
        </template>

        <!-- 开始时间列 -->
        <template #startTime="{ row }">
          <span class="time-text">{{ formatTime(row.checkTime) }}</span>
        </template>

        <!-- 结束时间列（简化：同开始时间，实际应计算状态持续结束时间） -->
        <template #endTime="{ row }">
          <span class="time-text">{{ formatTime(row.checkTime) }}</span>
        </template>

        <!-- 持续时长列（简化：显示响应时间） -->
        <template #duration="{ row }">
          <span class="duration-text">{{ row.responseTime }} ms</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
          </div>
        </template>
      </DataTable>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </section>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="状态历史详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-content">
        <!-- 设备基本信息 -->
        <div class="detail-section">
          <div class="section-title">设备基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="设备名称">{{ currentDetail.deviceName }}</el-descriptions-item>
            <el-descriptions-item label="设备编号">{{ currentDetail.deviceId }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ getDeviceTypeLabel(currentDetail.deviceType) }}</el-descriptions-item>
            <el-descriptions-item label="设备型号">{{ currentDetail.deviceModel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属门店">{{ getStoreName(currentDetail.storeId) }}</el-descriptions-item>
            <el-descriptions-item label="IP地址">{{ currentDetail.ipAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="连接类型">{{ currentDetail.connectionType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="固件版本">{{ currentDetail.firmwareVersion || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 状态变化记录 -->
        <div class="detail-section">
          <div class="section-title">状态变化记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="记录编号">{{ currentDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="检查时间">{{ formatTime(currentDetail.checkTime) }}</el-descriptions-item>
            <el-descriptions-item label="状态类型">
              <StatusTag :status="getStatusTypeColor(currentDetail.online)" :label="getStatusTypeLabel(currentDetail.online)" size="small" variant="light" />
            </el-descriptions-item>
            <el-descriptions-item label="响应时间">{{ currentDetail.responseTime }} ms</el-descriptions-item>
            <el-descriptions-item label="错误信息" :span="2">
              <span :class="currentDetail.errorMessage ? 'error-text' : 'text-muted'">
                {{ currentDetail.errorMessage || '无' }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="详细信息" :span="2">
              <span class="text-muted">{{ currentDetail.details || '无' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 状态趋势图（简化版） -->
        <div class="detail-section">
          <div class="section-title">状态趋势图（最近12小时）</div>
          <div class="trend-chart">
            <div class="trend-header">
              <div class="trend-legend">
                <span class="legend-item">
                  <span class="legend-dot legend-online"></span>
                  在线
                </span>
                <span class="legend-item">
                  <span class="legend-dot legend-offline"></span>
                  离线
                </span>
              </div>
            </div>
            <div class="trend-bars">
              <div
                v-for="(item, index) in trendData"
                :key="index"
                class="trend-bar-wrapper"
              >
                <div
                  class="trend-bar"
                  :class="item.online ? 'bar-online' : 'bar-offline'"
                  :style="{ height: `${Math.min(item.responseTime / 2, 100)}px` }"
                  :title="`${formatTime(item.time)} - ${item.online ? '在线' : '离线'} - ${item.responseTime}ms`"
                ></div>
                <div class="trend-label">{{ new Date(item.time).getHours() }}:00</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

// 工具栏面板
.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 48px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;
}

// 表格区域
.table-section {
  padding: 0 var(--fts-space-6) var(--fts-space-6);
  overflow-x: auto;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);

  :deep(.el-table) {
    width: 100%;
  }

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 设备名称
.device-name {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

// 类型文本
.type-text {
  color: var(--fts-text-secondary);
}

// 门店文本
.store-text {
  color: var(--fts-text-secondary);
}

// 响应时间
.response-time {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 持续时长
.duration-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 详情内容
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

.error-text {
  color: var(--fts-error);
}

.text-muted {
  color: var(--fts-text-tertiary);
}

// 趋势图
.trend-chart {
  background: var(--fts-bg-page);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-4);
}

.trend-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--fts-space-4);
}

.trend-legend {
  display: flex;
  gap: var(--fts-space-4);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-online {
  background: var(--fts-success);
}

.legend-offline {
  background: var(--fts-warning);
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 120px;
  gap: var(--fts-space-2);
  padding: 0 var(--fts-space-2);
}

.trend-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-1);
}

.trend-bar {
  width: 100%;
  min-height: 4px;
  border-radius: 2px 2px 0 0;
  transition: height 0.3s ease;
  cursor: pointer;
}

.bar-online {
  background: var(--fts-success);
}

.bar-offline {
  background: var(--fts-warning);
}

.trend-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  white-space: nowrap;
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}

@media (max-width: 576px) {
  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .table-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
