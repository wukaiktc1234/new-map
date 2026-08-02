<script setup lang="ts">
/**
 * 排队历史页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】展示门店排队叫号历史记录，支持筛选、导出和详情查看
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search, Refresh, Clock, User, Phone } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { queueApi } from '@/api/store-ops/queue'
import { callNumberQueueDataConverter } from '@/api/store-ops/converters'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { exportToCSV } from '@/utils/export-csv'
import type {
  CallNumberQueue,
  CallNumberQueueQueryForm,
  CallNumberQueueStats,
  QueueStatus,
  QueueType,
} from '@/types/store-operation'
import type { StoreOption } from '@/types/store-operation/store-archive'

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
  align?: 'left' | 'center' | 'right'
}

/** 状态时间线项 */
interface StatusTimelineItem {
  status: string
  label: string
  time: string
  done: boolean
}

/** 操作记录项 */
interface OperationLogItem {
  operator: string
  action: string
  time: string
  remark?: string
}

// ==================== 响应式数据 ====================

const exportLoading = ref(false)
const detailVisible = ref(false)
const detailData = ref<CallNumberQueue | null>(null)

// 门店选项（从真实 API 加载，使用共享 composable）
const { storeOptions, loadStores, getStoreName } = useStoreOptions(true)

/** 桌型选项 */
const tableTypeOptions = [
  { value: 'small', label: '小桌(1-2人)' },
  { value: 'medium', label: '中桌(3-4人)' },
  { value: 'large', label: '大桌(5-8人)' },
  { value: 'xlarge', label: '超大桌(8人以上)' },
  { value: 'private', label: '包厢' },
]

// 查询表单
const queryForm = ref({
  storeId: '' as number | string,
  status: '' as QueueStatus | '',
  queueType: '' as QueueType | '',
  tableType: '',
  dateRange: null as [string, string] | null,
  keyword: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<CallNumberQueue, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: CallNumberQueueQueryForm = {
        page: params.page,
        size: params.size,
        status: params.status || undefined,
        queueType: params.queueType || undefined,
        keyword: params.keyword || undefined,
        dateRange: params.dateRange,
      }
      return queueApi.getRecords(queryParams)
    },
  } as unknown as CrudApi<CallNumberQueue, typeof queryForm.value>,
  queryForm,
  autoLoad: false,
})

// 统计数据
const stats = ref<CallNumberQueueStats>({
  waitingCount: 0,
  dinedCount: 0,
  avgWaitMinutes: 0,
  cancelledCount: 0,
})

// 统计卡片计算属性
const statistics = computed(() => ({
  todayQueue: pagination?.total || 0,
  avgWaitTime: Math.round(stats.value.avgWaitMinutes),
  calledCount: stats.value.dinedCount,
  cancelledCount: stats.value.cancelledCount,
}))

// ==================== 计算属性 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'ticketNumber', label: '排队号', width: 100, align: 'center' },
  { prop: 'customerName', label: '顾客姓名', minWidth: 100, slot: 'customerName' },
  { prop: 'customerPhone', label: '联系电话', minWidth: 120, slot: 'customerPhone' },
  { prop: 'tableType', label: '桌型', minWidth: 100, slot: 'tableType' },
  { prop: 'peopleCount', label: '人数', width: 80, align: 'center' },
  { prop: 'createTime', label: '取号时间', minWidth: 160, slot: 'createTime' },
  { prop: 'callTime', label: '叫号时间', minWidth: 160, slot: 'callTime' },
  { prop: 'waitMinutes', label: '等待时长', minWidth: 100, align: 'center', slot: 'waitMinutes' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'storeId', label: '门店', minWidth: 120, slot: 'store' },
  { prop: '_operation', label: '操作', width: 100, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

/** 加载统计数据 */
async function loadStats(): Promise<void> {
  try {
    const result = await queueApi.getStats()
    stats.value = result
  } catch {
    console.error('[StoreQueueHistory] 加载统计数据失败')
  }
}

/** 搜索 */
function handleSearch(): void {
  pagination.current = 1
  refresh()
}

/** 重置 */
function handleReset(): void {
  queryForm.value = {
    storeId: '',
    status: '',
    queueType: '',
    tableType: '',
    dateRange: null,
    keyword: '',
  }
  pagination.current = 1
  refresh()
}

/** 查看详情 */
function handleViewDetail(row: CallNumberQueue): void {
  detailData.value = row
  detailVisible.value = true
}

/** 格式化时间 */
function formatTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 获取桌型标签 */
function getTableTypeLabel(tablePreference?: string): string {
  if (!tablePreference) return '-'
  const opt = tableTypeOptions.find(o => o.value === tablePreference)
  return opt?.label || tablePreference
}

/** 获取状态时间线 */
function getStatusTimeline(row: CallNumberQueue): StatusTimelineItem[] {
  const items: StatusTimelineItem[] = [
    { status: 'waiting', label: '取号排队', time: row.createTime, done: true },
  ]

  if (row.callTime) {
    items.push({ status: 'called', label: '叫号通知', time: row.callTime, done: true })
  }

  if (row.status === 'dined') {
    items.push({ status: 'dined', label: '已入座用餐', time: row.callTime || '', done: true })
  } else if (row.status === 'cancelled') {
    items.push({ status: 'cancelled', label: '已取消', time: row.createTime, done: true })
  } else if (row.status === 'expired') {
    items.push({ status: 'expired', label: '已过号', time: row.createTime, done: true })
  }

  return items
}

/** 获取操作记录（模拟数据） */
function getOperationLogs(_row: CallNumberQueue): OperationLogItem[] {
  return [
    { operator: '系统', action: '顾客取号', time: _row.createTime, remark: '微信小程序取号' },
    { operator: '张经理', action: '叫号', time: _row.callTime || '-', remark: `第${_row.calledCount}次叫号` },
  ]
}

/** 排队类型显示映射 */
function getQueueTypeLabel(type: QueueType): string {
  const map: Record<QueueType, string> = {
    dine_in: '堂食',
    takeout: '外卖',
    pickup: '自提',
  }
  return map[type] || type
}

// TODO: 后端 CallNumberQueue 类型暂无 customerName/customerPhone 字段，待接口补充后替换 '-' 占位
/** 导出排队数据 */
async function handleExport(): Promise<void> {
  if (tableData.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const exportColumns = [
      { prop: 'ticketNumber' as const, label: '排队号' },
      { prop: 'customerName' as const, label: '顾客姓名' },
      { prop: 'customerPhone' as const, label: '联系电话' },
      { prop: 'tableType' as const, label: '桌型' },
      { prop: 'peopleCount' as const, label: '人数' },
      { prop: 'createTime' as const, label: '取号时间' },
      { prop: 'callTime' as const, label: '叫号时间' },
      { prop: 'waitMinutes' as const, label: '等待时长(分钟)' },
      { prop: 'status' as const, label: '状态' },
      { prop: 'storeName' as const, label: '门店' },
    ]

    const exportData = tableData.value.map(row => ({
      ticketNumber: row.ticketNumber,
      customerName: '-',
      customerPhone: '-',
      tableType: getTableTypeLabel(row.tablePreference),
      peopleCount: row.peopleCount,
      createTime: formatTime(row.createTime),
      callTime: formatTime(row.callTime),
      waitMinutes: row.waitMinutes ?? '-',
      status: callNumberQueueDataConverter.toStatusLabel(row.status),
      storeName: getStoreName(row.storeId),
    }))

    exportToCSV(exportData, exportColumns, '排队历史记录')
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/** 刷新数据 */
async function handleRefresh(): Promise<void> {
  await Promise.all([refresh(), loadStats()])
  ElMessage.success('刷新成功')
}

// 页面挂载时加载数据（门店选项由 useStoreOptions autoLoad 自动加载）
onMounted(async () => {
  await Promise.all([refresh(), loadStats()])
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="叫号记录" description="查看门店排队叫号历史记录">
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Tickets" label="今日排队数" :value="String(statistics.todayQueue)" color-type="primary" variant="bordered" />
      <StatCard icon="Clock" label="平均等待时间(分)" :value="String(statistics.avgWaitTime)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="已叫号" :value="String(statistics.calledCount)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="已取消" :value="String(statistics.cancelledCount)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.storeId"
            placeholder="选择门店"
            clearable
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
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            placement="bottom-start"
            style="width: 240px"
            size="default"
            @change="handleSearch"
          />
          <el-select
            v-model="queryForm.status"
            placeholder="排队状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="等待中" value="waiting" />
            <el-option label="已叫号" value="called" />
            <el-option label="已过号" value="expired" />
            <el-option label="已用餐" value="dined" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
          <el-select
            v-model="queryForm.tableType"
            placeholder="桌型"
            clearable
            style="width: 140px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in tableTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索排队号或顾客..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            size="default"
            class="action-btn--export"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
          <el-button size="default" @click="handleRefresh">
            <el-icon :size="14"><Refresh /></el-icon>刷新
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
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
      >
        <!-- 顾客姓名列 -->
        <template #customerName="{ row }">
          <span class="name-text">-</span>
        </template>

        <!-- 联系电话列 -->
        <template #customerPhone="{ row }">
          <span class="phone-text">-</span>
        </template>

        <!-- 桌型列 -->
        <template #tableType="{ row }">
          <span class="table-type-text">{{ getTableTypeLabel(row.tablePreference) }}</span>
        </template>

        <!-- 取号时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 叫号时间列 -->
        <template #callTime="{ row }">
          <span class="time-text">{{ formatTime(row.callTime) }}</span>
        </template>

        <!-- 等待时长列 -->
        <template #waitMinutes="{ row }">
          <span class="wait-text">{{ row.waitMinutes != null ? row.waitMinutes + ' 分钟' : '-' }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="callNumberQueueDataConverter.toStatusTagStatus(row.status)"
            :label="callNumberQueueDataConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 门店列 -->
        <template #store="{ row }">
          <span class="store-text">{{ getStoreName(row.storeId) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
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
      title="排队详情"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="detailData" class="detail-container">
        <!-- 排队基本信息 -->
        <div class="detail-section">
          <div class="section-title">排队基本信息</div>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="排队号">
              <span class="ticket-number">{{ detailData.ticketNumber }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="排队类型">
              <StatusTag
                :status="callNumberQueueDataConverter.toStatusTagStatus(detailData.status)"
                :label="getQueueTypeLabel(detailData.queueType)"
                size="small"
                variant="light"
              />
            </el-descriptions-item>
            <el-descriptions-item label="当前状态">
              <StatusTag
                :status="callNumberQueueDataConverter.toStatusTagStatus(detailData.status)"
                :label="callNumberQueueDataConverter.toStatusLabel(detailData.status)"
                size="default"
                variant="light"
              />
            </el-descriptions-item>
            <el-descriptions-item label="叫号次数">
              {{ detailData.calledCount }} 次
            </el-descriptions-item>
            <el-descriptions-item label="取号时间">
              {{ formatTime(detailData.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="叫号时间">
              {{ formatTime(detailData.callTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="等待时长">
              {{ detailData.waitMinutes != null ? detailData.waitMinutes + ' 分钟' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="所属门店">
              {{ getStoreName(detailData.storeId) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 顾客信息 -->
        <div class="detail-section">
          <div class="section-title">顾客信息</div>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="顾客姓名">
              <el-icon><User /></el-icon>
              <span style="margin-left: 4px">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              <el-icon><Phone /></el-icon>
              <span style="margin-left: 4px">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="用餐人数">
              {{ detailData.peopleCount }} 人
            </el-descriptions-item>
            <el-descriptions-item label="桌型偏好">
              {{ getTableTypeLabel(detailData.tablePreference) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 状态时间线 -->
        <div class="detail-section">
          <div class="section-title">状态时间线</div>
          <el-timeline>
            <el-timeline-item
              v-for="(item, index) in getStatusTimeline(detailData)"
              :key="index"
              :timestamp="item.time"
              :type="item.done ? 'primary' : 'info'"
            >
              <div class="timeline-content">
                <span class="timeline-label">{{ item.label }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- 操作记录 -->
        <div class="detail-section">
          <div class="section-title">操作记录</div>
          <el-table :data="getOperationLogs(detailData)" size="small" border>
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="action" label="操作内容" min-width="150" />
            <el-table-column prop="time" label="操作时间" width="160" />
            <el-table-column prop="remark" label="备注" min-width="150" />
          </el-table>
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

  .action-btn--export {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
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

// 文字样式
.name-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.phone-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.table-type-text {
  color: var(--fts-text-primary);
}

.store-text {
  color: var(--fts-text-primary);
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.wait-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.ticket-number {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-primary);
  font-size: var(--fts-font-size-lg);
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

// 详情对话框
.detail-container {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
    line-height: 1.2;
  }
}

.timeline-content {
  .timeline-label {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
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
