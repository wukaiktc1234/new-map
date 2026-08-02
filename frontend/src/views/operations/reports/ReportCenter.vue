<script setup lang="ts">
/**
 * 经营报表中心页面（管理层视角）
 *
 * 功能：
 * - 6种报表Tab切换（日报/周报/月报/季报/年报/利润分析）
 * - 全局筛选器（日期/门店/渠道/对比）
 * - KPI首屏快速加载（管理层核心指标）
 * - 趋势图（营收+毛利率双Y轴）
 * - 门店排名条形图
 * - 门店对比数据表格
 * - 导出功能
 */
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataLine,
  TrendCharts,
  List,
  PieChart,
  Histogram,
  Money,
  Download,
  RefreshRight,
  FolderOpened
} from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import ReportKpiCards from './components/ReportKpiCards.vue'
import ReportChart from './components/ReportChart.vue'
import ReportDataTable from './components/ReportDataTable.vue'
import ExportTaskDrawer, { type ExportTaskItem } from './components/ExportTaskDrawer.vue'
import { operationsReportApi } from '@/api/operations/report'
import { useStoreOptions } from '@/composables/useStoreOptions'
import logger from '@/utils/logger'
import type { ReportTab, ReportType, KpiSummary, ChartData, ExportTaskStatus } from './types/report'

/** 图表视图模式（由用户切换） */
type ChartViewMode = 'revenue' | 'order'
const chartViewMode = ref<ChartViewMode>('revenue')

// ========== 状态管理 ==========

const activeTab = ref<ReportTab>('monthly')
const loading = ref(false)

// 获取默认日期范围（本月）
function getDefaultDateRange(): [string, string] {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  return [formatDate(start), formatDate(now)]
}

/** 格式化日期为 YYYY-MM-DD */
function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** 根据 Tab 类型自动计算对应的日期范围 */
function getDateRangeForTab(tab: ReportTab): [string, string] {
  const now = new Date()
  const today = formatDate(now)

  switch (tab) {
    case 'daily':
      // 日报：今天
      return [today, today]

    case 'weekly': {
      // 周报：本周一到今天
      const day = now.getDay() || 7 // 周日=7
      const monday = new Date(now)
      monday.setDate(now.getDate() - day + 1)
      return [formatDate(monday), today]
    }

    case 'monthly': {
      // 月报：本月1号到今天
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [formatDate(start), today]
    }

    case 'quarterly': {
      // 季报：本季第一天到今天
      const quarterStartMonth = Math.floor(now.getMonth() / 3) * 3
      const start = new Date(now.getFullYear(), quarterStartMonth, 1)
      return [formatDate(start), today]
    }

    case 'yearly':
    case 'profit': {
      // 年报/利润分析：本年1月1日到今天
      const start = new Date(now.getFullYear(), 0, 1)
      return [formatDate(start), today]
    }

    default:
      return getDefaultDateRange()
  }
}

// 筛选状态
const filterState = ref({
  dateRange: getDefaultDateRange(),
  selectedStores: [] as string[],
  channel: 'all' as string,
  compareType: 'mom' as string
})

// KPI数据
const kpiData = ref<KpiSummary | null>(null)
const kpiLoading = ref(false)

// 图表数据
const chartData = ref<ChartData | null>(null)
const chartLoading = ref(false)

// 门店下拉选项（接入真实后端 API：/v1/store-operation/stores/active）
const { storeOptions } = useStoreOptions(true)

// Tab配置
const tabs = [
  { key: 'daily' as ReportTab, label: '日报', icon: List, reportType: 1 as ReportType },
  { key: 'weekly' as ReportTab, label: '周报', icon: Histogram, reportType: 2 as ReportType },
  { key: 'monthly' as ReportTab, label: '月报', icon: DataLine, reportType: 3 as ReportType },
  { key: 'quarterly' as ReportTab, label: '季报', icon: PieChart, reportType: 4 as ReportType },
  { key: 'yearly' as ReportTab, label: '年报', icon: TrendCharts, reportType: 5 as ReportType },
  { key: 'profit' as ReportTab, label: '利润分析', icon: Money, reportType: 6 as ReportType }
]

const currentReportType = computed(() => {
  const tab = tabs.find(t => t.key === activeTab.value)
  return tab?.reportType || 1
})

// ========== 工具函数 ==========

function buildQueryParams(): Record<string, unknown> {
  return {
    startDate: filterState.value.dateRange[0],
    endDate: filterState.value.dateRange[1],
    storeIds: filterState.value.selectedStores,
    channel: filterState.value.channel,
    compareType: filterState.value.compareType,
    reportType: currentReportType.value
  }
}

// ========== 数据加载 ==========

async function loadKpiSummary(): Promise<void> {
  if (kpiLoading.value) return
  kpiLoading.value = true
  try {
    const params = buildQueryParams()
    const data = await operationsReportApi.getKpiSummary(params)
    kpiData.value = data
  } catch (error: unknown) {
    logger.error('ReportCenter', '加载KPI数据失败', error instanceof Error ? error : undefined)
    ElMessage.error('加载KPI数据失败')
  } finally {
    kpiLoading.value = false
  }
}

async function loadReportData(): Promise<void> {
  if (loading.value) return
  loading.value = true
  chartLoading.value = true
  try {
    const params = buildQueryParams()
    const data = await operationsReportApi.getReportData(activeTab.value, params)
    chartData.value = data
  } catch (error: unknown) {
    logger.error('ReportCenter', '加载报表数据失败', error instanceof Error ? error : undefined)
    ElMessage.error('加载报表数据失败')
  } finally {
    loading.value = false
    chartLoading.value = false
  }
}

async function refreshData(): Promise<void> {
  await Promise.all([
    loadKpiSummary(),
    loadReportData()
  ])
}

function handleFilterChange(): void {
  refreshData()
}

function handleTabChange(tab: ReportTab): void {
  activeTab.value = tab
  // Tab 切换时自动调整日期范围到对应周期
  filterState.value.dateRange = getDateRangeForTab(tab)
  refreshData()
}

// ========== 导出 ==========

const exporting = ref(false)

/** 导出任务列表（本地内存维护） */
const exportTasks = ref<ExportTaskItem[]>([])

/** 导出任务抽屉可见性 */
const exportDrawerVisible = ref(false)

async function handleExportClick(type: 'excel' | 'pdf'): Promise<void> {
  if (exporting.value) return
  exporting.value = true
  try {
    const taskType = type === 'excel' ? 1 : 2
    const params = buildQueryParams()
    const res = await operationsReportApi.createExportTask(taskType, currentReportType.value, params)
    // 加入任务列表
    exportTasks.value.unshift({
      ...res,
      taskKind: type,
      reportKind: activeTab.value,
      createdAt: new Date().toISOString(),
    })
    ElMessage.success(`导出任务已创建: ${res.taskNo}`)
    // 自动打开抽屉
    exportDrawerVisible.value = true
  } catch (error: unknown) {
    logger.error('ReportCenter', '导出失败', error instanceof Error ? error : undefined)
    if (error instanceof Error && error.message) {
      ElMessage.error(`导出失败：${error.message}`)
    } else {
      ElMessage.error('导出失败')
    }
  } finally {
    exporting.value = false
  }
}

/** 更新任务状态（由 ExportTaskDrawer 轮询触发） */
function handleUpdateTask(taskId: number, status: ExportTaskStatus): void {
  const task = exportTasks.value.find(t => t.taskId === taskId)
  if (!task) return
  task.status = status.status
  task.fileName = status.fileName
  task.fileSize = status.fileSize
  task.rowCount = status.rowCount
  task.errorMessage = status.errorMessage
  task.completedTime = status.completedTime
  // 任务完成时提示
  if (status.status === 3) {
    ElMessage.success(`导出任务 ${task.taskNo} 已完成`)
  } else if (status.status === 4) {
    ElMessage.error(`导出任务 ${task.taskNo} 失败：${status.errorMessage || '未知错误'}`)
  }
}

/** 移除单个任务 */
function handleRemoveTask(taskId: number): void {
  exportTasks.value = exportTasks.value.filter(t => t.taskId !== taskId)
  ElMessage.success('任务已移除')
}

/** 清空所有任务 */
function handleClearAllTasks(): void {
  exportTasks.value = []
  ElMessage.success('已清空所有任务')
}

// ========== 生命周期 ==========

onMounted(() => {
  refreshData()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader
      title="经营报表"
      subtitle="企业经营数据统一视图 · 数据每日 06:00 自动聚合"
      :icon="DataLine"
      icon-color="var(--fts-primary)"
    >
      <template #actions>
        <el-button
          :icon="RefreshRight"
          :loading="loading || kpiLoading"
          @click="refreshData"
        >
          刷新
        </el-button>
        <el-button
          :icon="FolderOpened"
          @click="exportDrawerVisible = true"
        >
          导出任务
          <el-badge
            v-if="exportTasks.length > 0"
            :value="exportTasks.length"
            :max="99"
            class="report-center__badge"
          />
        </el-button>
        <el-button
          type="primary"
          :icon="Download"
          :loading="exporting"
          @click="handleExportClick('excel')"
        >
          导出Excel
        </el-button>
      </template>
    </PageHeader>

    <!-- 顶部筛选栏（时间维度 Tab + 全局筛选器） -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-left">
          <!-- 时间维度 Tab -->
          <div class="rc-time-bar">
            <div
              v-for="tab in tabs"
              :key="tab.key"
              class="rc-time-bar__item"
              :class="{ 'is-active': activeTab === tab.key }"
              @click="handleTabChange(tab.key)"
            >
              <el-icon :size="14"><component :is="tab.icon" /></el-icon>
              <span>{{ tab.label }}</span>
            </div>
          </div>
          <!-- 日期范围 -->
          <el-date-picker
            v-model="filterState.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled="loading"
            :teleported="false"
            style="width: 220px"
            @change="handleFilterChange"
          />
          <!-- 门店选择 -->
          <el-select
            v-model="filterState.selectedStores"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择门店"
            :disabled="loading"
            :max-collapse-tags="2"
            :teleported="false"
            style="width: 180px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="String(store.storeId)"
            />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button :icon="RefreshRight" :loading="loading" @click="refreshData">刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 内容区：KPI + 图表统一水平对齐 -->
    <div class="content-wrapper">
      <!-- KPI 首屏 -->
      <section class="stats-section">
        <ReportKpiCards
          :data="kpiData"
          :loading="kpiLoading"
        />
      </section>

      <!-- 主内容区：图表 + 表格的两列布局 -->
      <section class="charts-section">
        <div class="charts-row">
        <!-- 经营趋势分析 -->
        <div class="chart-card chart-card--large">
          <div class="chart-card__header">
            <h4 class="chart-card__title">经营趋势分析</h4>
            <div class="chart-card__actions">
              <el-radio-group v-model="chartViewMode" size="small">
                <el-radio-button value="revenue">营收+毛利率</el-radio-button>
                <el-radio-button value="order">订单+客单</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div class="chart-card__body">
            <ReportChart
              :data="chartData"
              :loading="chartLoading"
              :report-type="currentReportType"
              :view-mode="chartViewMode"
            />
          </div>
        </div>

        <!-- 门店经营排名 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">门店经营排名</h4>
            <span class="chart-card__subtitle">TOP {{ chartData?.storeRanking?.length || 0 }}</span>
          </div>
          <div class="chart-card__body">
            <ReportDataTable
              :data="chartData"
              :loading="chartLoading"
              :compact="true"
            />
          </div>
        </div>
      </div>
    </section>
  </div>

    <!-- 导出任务抽屉 -->
    <ExportTaskDrawer
      v-model="exportDrawerVisible"
      :tasks="exportTasks"
      @update-task="handleUpdateTask"
      @remove="handleRemoveTask"
      @clear-all="handleClearAllTasks"
    />
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
  border-radius: var(--fts-page-radius);
}

// 头部 badge 修复
.report-center__badge {
  margin-left: var(--fts-space-1);
}

// 筛选栏
.filter-section {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  margin-top: var(--fts-space-4);
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 48px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;
}

// 时间维度 Tab
.rc-time-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-1);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-lg);
  border: 1px solid var(--fts-border-secondary);

  &__item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px var(--fts-space-3);
    border-radius: var(--fts-radius-md);
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-secondary);
    cursor: pointer;
    transition: all var(--fts-duration-fast);
    white-space: nowrap;

    &:hover {
      color: var(--fts-text-primary);
      background: var(--fts-bg-hover);
    }

    &.is-active {
      background: var(--fts-bg-card);
      color: var(--fts-primary);
      box-shadow: var(--fts-shadow-xs);
    }
  }
}

// 内容区无水平内边距，让 stats-section 与 charts-section 铺满 main 宽度
.content-wrapper {
  padding: 0;
}

// 统计卡片区域：内部为 ReportKpiCards 单个子组件，本身不启用 grid，由子组件的 kpi-grid 控制布局
.stats-section {
  display: block;
  padding: var(--fts-space-4) 0;

  .report-kpi-cards {
    width: 100%;
  }
}

// 图表区域
.charts-section {
  padding: 0 0 var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.charts-row {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: var(--fts-space-4);

  @media (max-width: 1100px) {
    grid-template-columns: minmax(0, 1fr);
  }
}

.chart-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
  flex-shrink: 0;
}

.chart-card__title {
  margin: 0;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.chart-card__subtitle {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: var(--fts-font-weight-medium);
}

.chart-card__actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
}

.chart-card__body {
  flex: 1;
  min-height: 340px;
}

// 响应式适配
@media (max-width: 768px) {
  .filter-section {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-left {
    flex-wrap: wrap;
  }

  .filter-right {
    justify-content: flex-end;
  }

  .content-wrapper {
    padding: 0;
  }

  .stats-section {
    padding: var(--fts-space-3) 0;
  }

  .charts-section {
    padding: 0 0 var(--fts-space-3);
  }

  .rc-time-bar {
    flex-wrap: wrap;
    width: 100%;
  }

  .chart-card__header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--fts-space-2);
  }
}
</style>
