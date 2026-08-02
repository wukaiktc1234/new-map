<script setup lang="ts">
/**
 * 仓储总览页面 - 看板/仪表盘风格
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】仓储数据概览和仓储运营看板
 * 【依赖】L3(PageHeader/StatCard/DataTable/StatusTag) + ECharts
 *
 * 功能模块：
 * 1. 顶部筛选栏：仓库选择、时间范围
 * 2. 核心指标卡片（6个）
 * 3. 图表区域（4个图表）
 * 4. 快捷功能区
 * 5. 预警列表（TOP10）
 * 6. 近期动态
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Warning,
  Bottom,
  Top,
  List,
  DataLine,
  PieChart,
  Histogram,
  SwitchButton,
  Switch,
  Delete,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'

import { useEChartsTheme } from '@/composables/useEChartsTheme'
import {
  warehouseApi,
  inventoryStatsApi,
  inventoryWarningApi,
  inventoryLogApi,
} from '@/api/warehouse'

import type { WarehouseInfo } from '@/types/warehouse'
import type {
  InventoryStatsOverview,
  InventoryTrendItem,
  InventoryCategoryStatItem,
  StatColorType,
} from '@/types/warehouse-stats'
import type { InventoryWarningRecordInfo } from '@/types/warehouse-warning'
import type { InventoryLogInfo } from '@/types/warehouse-log'

const { chartColors, xAxisBaseOption, yAxisBaseOption, tooltipOption, legendOption, pieLabelStyle } = useEChartsTheme()

// ==================== 响应式数据 ====================

const loading = ref(false)

/** 仓库选项 */
const warehouseOptions = ref<WarehouseInfo[]>([])

/** 筛选表单 */
const filterForm = ref({
  warehouseId: '' as string,
  dateRange: [] as string[],
})

/** 统计概览数据 */
const statsOverview = ref<InventoryStatsOverview | null>(null)

/** 库存趋势数据（近30天） */
const inventoryTrendData = ref<InventoryTrendItem[]>([])

/** 分类统计数据 */
const categoryStatsData = ref<InventoryCategoryStatItem[]>([])

/** 预警列表 TOP10 */
const warningListData = ref<InventoryWarningRecordInfo[]>([])

/** 近期动态列表 */
const recentLogsData = ref<InventoryLogInfo[]>([])

// ==================== 图表引用 ====================

const trendChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
const ioChartRef = ref<HTMLElement | null>(null)

let trendChartInstance: echarts.ECharts | null = null
let pieChartInstance: echarts.ECharts | null = null
let barChartInstance: echarts.ECharts | null = null
let ioChartInstance: echarts.ECharts | null = null

// ==================== 计算属性 ====================

/** 核心指标卡片数据 */
const statCards = computed(() => {
  const s = statsOverview.value
  return [
    {
      icon: 'Money',
      label: '库存总金额',
      value: s ? `¥${s.totalValue}` : '¥0.00',
      colorType: 'primary' as StatColorType,
    },
    {
      icon: 'Goods',
      label: '库存商品种类',
      value: s?.totalItems ?? 0,
      colorType: 'success' as StatColorType,
    },
    {
      icon: 'Timer',
      label: '库存周转天数',
      value: s?.turnoverRate != null ? s.turnoverRate.toFixed(1) : '0',
      colorType: 'warning' as StatColorType,
    },
    {
      icon: 'DocumentAdd',
      label: '今日入库单',
      value: '0',
      colorType: 'info' as StatColorType,
    },
    {
      icon: 'Document',
      label: '今日出库单',
      value: '0',
      colorType: 'info' as StatColorType,
    },
    {
      icon: 'Warning',
      label: '预警商品数',
      value: s?.warningCount ?? 0,
      colorType: 'error' as StatColorType,
    },
  ]
})

/** 快捷功能列表 */
const quickActions = [
  { icon: Bottom, label: '入库管理', type: 'primary' },
  { icon: Top, label: '出库管理', type: 'success' },
  { icon: List, label: '库存盘点', type: 'warning' },
  { icon: Switch, label: '库存调拨', type: 'info' },
  { icon: Delete, label: '库存报损', type: 'danger' },
]

/** 预警列表表格列 */
const warningColumns = computed<DataTableColumn[]>(() => [
  { prop: 'materialName', label: '商品名称', minWidth: 160 },
  { prop: 'warehouseName', label: '所属仓库', minWidth: 120 },
  { prop: 'currentStock', label: '当前库存', minWidth: 100, align: 'center' },
  { prop: 'threshold', label: '预警阈值', minWidth: 100, align: 'center' },
  { prop: 'warningTypeName', label: '预警类型', minWidth: 100, slot: 'warningType' },
  { prop: 'createTime', label: '预警时间', minWidth: 160, slot: 'createTime' },
])

/** 近期动态表格列 */
const logColumns = computed<DataTableColumn[]>(() => [
  { prop: 'materialName', label: '商品名称', minWidth: 160 },
  { prop: 'warehouseName', label: '仓库', minWidth: 100 },
  { prop: 'operationTypeName', label: '操作类型', minWidth: 100, slot: 'operationType' },
  { prop: 'changeAmount', label: '变动数量', minWidth: 100, slot: 'changeAmount', align: 'center' },
  { prop: 'operatorName', label: '操作人', minWidth: 100 },
  { prop: 'createdAt', label: '操作时间', minWidth: 160, slot: 'createdAt' },
])

// ==================== 辅助方法 ====================

/** 从 CSS 变量获取颜色 */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || 'var(--fts-primary)'
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 获取预警类型对应的状态颜色 */
function getWarningTypeStatus(type: string): string {
  const map: Record<string, string> = {
    low_stock: 'warning',
    high_stock: 'info',
    expiring_soon: 'warning',
    expired: 'error',
  }
  return map[type] || 'info'
}

/** 获取操作类型对应的状态颜色 */
function getOperationTypeStatus(type: string): string {
  const inTypes = ['purchase_in', 'transfer_in', 'check_gain', 'return']
  const outTypes = ['sale_out', 'transfer_out', 'check_loss', 'damage']
  if (inTypes.includes(type)) return 'success'
  if (outTypes.includes(type)) return 'warning'
  return 'info'
}

// ==================== 数据加载 ====================

/** 加载仓库选项 */
async function loadWarehouseOptions(): Promise<void> {
  try {
    warehouseOptions.value = await warehouseApi.getActiveList()
  } catch {
    warehouseOptions.value = []
  }
}

/** 加载统计概览 */
async function loadStatsOverview(): Promise<void> {
  try {
    statsOverview.value = await inventoryStatsApi.getOverview()
  } catch {
    statsOverview.value = null
  }
}

/** 加载库存趋势数据 */
async function loadInventoryTrend(): Promise<void> {
  try {
    const params = {
      warehouseId: filterForm.value.warehouseId || undefined,
      granularity: 'day' as const,
    }
    inventoryTrendData.value = await inventoryStatsApi.getTrend(params)
  } catch {
    inventoryTrendData.value = []
  }
}

/** 加载分类统计数据 */
async function loadCategoryStats(): Promise<void> {
  try {
    categoryStatsData.value = await inventoryStatsApi.getCategoryStats(
      filterForm.value.warehouseId || undefined
    )
  } catch {
    categoryStatsData.value = []
  }
}

/** 加载预警列表 */
async function loadWarningList(): Promise<void> {
  try {
    const res = await inventoryWarningApi.getPage({
      warehouseId: filterForm.value.warehouseId || undefined,
      page: 1,
      size: 10,
    })
    warningListData.value = res.records || []
  } catch {
    warningListData.value = []
  }
}

/** 加载近期动态 */
async function loadRecentLogs(): Promise<void> {
  try {
    const res = await inventoryLogApi.getPage({
      warehouseId: filterForm.value.warehouseId || undefined,
      page: 1,
      size: 10,
    })
    recentLogsData.value = res.records || []
  } catch {
    recentLogsData.value = []
  }
}

/** 加载所有数据 */
async function loadAllData(): Promise<void> {
  loading.value = true
  try {
    await Promise.all([
      loadStatsOverview(),
      loadInventoryTrend(),
      loadCategoryStats(),
      loadWarningList(),
      loadRecentLogs(),
    ])
    await nextTick()
    initAllCharts()
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ==================== 图表配置 ====================

/** 库存趋势折线图配置 */
function getTrendChartOption(): EChartsOption {
  const data = inventoryTrendData.value
  const dates = data.map(item => item.date)
  const values = data.map(item => parseFloat(item.totalValue))

  const primaryColor = getCssVar('--fts-primary')

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'axis',
    },
    legend: {
      ...legendOption.value,
      data: ['库存金额'],
      top: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      ...xAxisBaseOption.value,
    },
    yAxis: {
      type: 'value',
      name: '金额（元）',
      ...yAxisBaseOption.value,
    },
    series: [
      {
        name: '库存金额',
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: primaryColor },
        itemStyle: { color: primaryColor },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: primaryColor + '33' },
              { offset: 1, color: primaryColor + '05' },
            ],
          },
        },
      },
    ],
  }
}

/** 仓库库存占比饼图配置 */
function getPieChartOption(): EChartsOption {
  const data = categoryStatsData.value
  const pieData = data.map(item => ({
    name: item.categoryName,
    value: parseFloat(item.value),
  }))

  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')
  const warningColor = getCssVar('--fts-warning')
  const errorColor = getCssVar('--fts-error')
  const infoColor = getCssVar('--fts-info')

  const colors = [primaryColor, successColor, warningColor, infoColor, errorColor, getCssVar('--fts-chart-color-5'), getCssVar('--fts-chart-color-6')]

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    legend: {
      ...legendOption.value,
      orient: 'vertical',
      right: '5%',
      top: 'center',
    },
    color: colors,
    series: [
      {
        name: '库存占比',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: chartColors.value.backgroundColor,
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
            color: pieLabelStyle.value.color,
          },
        },
        labelLine: {
          show: false,
        },
        data: pieData,
      },
    ],
  }
}

/** 库存分类分布柱状图配置 */
function getBarChartOption(): EChartsOption {
  const data = categoryStatsData.value
  const categories = data.map(item => item.categoryName)
  const quantities = data.map(item => item.quantity)

  const successColor = getCssVar('--fts-success')

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories,
      ...xAxisBaseOption.value,
    },
    yAxis: {
      type: 'value',
      name: '数量',
      ...yAxisBaseOption.value,
    },
    series: [
      {
        name: '库存数量',
        type: 'bar',
        data: quantities,
        barWidth: '50%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: successColor },
              { offset: 1, color: successColor + '66' },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  }
}

/** 出入库趋势折线图配置 */
function getIOChartOption(): EChartsOption {
  const data = inventoryTrendData.value.slice(-7)
  const dates = data.map(item => item.date)
  const inQuantities = data.map(item => item.inQuantity)
  const outQuantities = data.map(item => item.outQuantity)

  const successColor = getCssVar('--fts-success')
  const warningColor = getCssVar('--fts-warning')

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'axis',
    },
    legend: {
      ...legendOption.value,
      data: ['入库数量', '出库数量'],
      top: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      ...xAxisBaseOption.value,
    },
    yAxis: {
      type: 'value',
      name: '数量',
      ...yAxisBaseOption.value,
    },
    series: [
      {
        name: '入库数量',
        type: 'line',
        data: inQuantities,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: successColor },
        itemStyle: { color: successColor },
      },
      {
        name: '出库数量',
        type: 'line',
        data: outQuantities,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: warningColor },
        itemStyle: { color: warningColor },
      },
    ],
  }
}

// ==================== 图表初始化 ====================

/** 初始化所有图表 */
function initAllCharts(): void {
  initTrendChart()
  initPieChart()
  initBarChart()
  initIOChart()
}

/** 初始化库存趋势图 */
function initTrendChart(): void {
  if (!trendChartRef.value) return
  if (trendChartInstance) {
    trendChartInstance.dispose()
  }
  trendChartInstance = echarts.init(trendChartRef.value)
  trendChartInstance.setOption(getTrendChartOption())
}

/** 初始化饼图 */
function initPieChart(): void {
  if (!pieChartRef.value) return
  if (pieChartInstance) {
    pieChartInstance.dispose()
  }
  pieChartInstance = echarts.init(pieChartRef.value)
  pieChartInstance.setOption(getPieChartOption())
}

/** 初始化柱状图 */
function initBarChart(): void {
  if (!barChartRef.value) return
  if (barChartInstance) {
    barChartInstance.dispose()
  }
  barChartInstance = echarts.init(barChartRef.value)
  barChartInstance.setOption(getBarChartOption())
}

/** 初始化出入库趋势图 */
function initIOChart(): void {
  if (!ioChartRef.value) return
  if (ioChartInstance) {
    ioChartInstance.dispose()
  }
  ioChartInstance = echarts.init(ioChartRef.value)
  ioChartInstance.setOption(getIOChartOption())
}

/** 窗口大小变化时重绘图表 */
function handleResize(): void {
  trendChartInstance?.resize()
  pieChartInstance?.resize()
  barChartInstance?.resize()
  ioChartInstance?.resize()
}

// ==================== 事件处理 ====================

/** 刷新数据 */
function handleRefresh(): void {
  loadAllData()
}

/** 查询 */
function handleSearch(): void {
  loadAllData()
}

/** 重置 */
function handleReset(): void {
  filterForm.value.warehouseId = ''
  filterForm.value.dateRange = []
  loadAllData()
}

/** 快捷功能点击 */
function handleQuickAction(action: typeof quickActions[number]): void {
  ElMessage.info(`${action.label}功能开发中...`)
}

/** 查看预警详情 */
function handleViewWarning(row: InventoryWarningRecordInfo): void {
  ElMessage.info(`查看预警详情：${row.materialName}`)
}

/** 查看库存日志详情 */
function handleViewLog(row: InventoryLogInfo): void {
  ElMessage.info(`查看日志详情：${row.materialName || '-'}`)
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadWarehouseOptions()
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance?.dispose()
  pieChartInstance?.dispose()
  barChartInstance?.dispose()
  ioChartInstance?.dispose()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="库存概览" description="库存数据概览和仓储运营看板">
      <el-button type="primary" size="default" @click="handleRefresh">
        <el-icon :size="16"><Refresh /></el-icon>刷新数据
      </el-button>
    </PageHeader>

    <!-- 筛选栏 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="filterForm.warehouseId"
            placeholder="全部仓库"
            clearable
            style="width: 160px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="wh in warehouseOptions"
              :key="wh.warehouseId"
              :label="wh.warehouseName"
              :value="wh.warehouseId"
            />
          </el-select>
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            size="default"
            value-format="YYYY-MM-DD"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">
            <el-icon :size="14"><Search /></el-icon>查询
          </el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 核心指标卡片区 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in statCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- 图表区域 -->
    <section class="charts-section">
      <div class="chart-row">
        <div class="chart-card chart-card--large">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><DataLine /></el-icon>
              <span>库存趋势（近30天）</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="trendChartRef" class="chart-container" />
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><PieChart /></el-icon>
              <span>库存分类占比</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="pieChartRef" class="chart-container chart-container--pie" />
          </div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-card">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><Histogram /></el-icon>
              <span>库存分类分布</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="barChartRef" class="chart-container" />
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><SwitchButton /></el-icon>
              <span>出入库趋势（近7天）</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="ioChartRef" class="chart-container" />
          </div>
        </div>
      </div>
    </section>

    <!-- 快捷功能区 -->
    <section class="quick-actions-section">
      <div class="section-title">快捷功能</div>
      <div class="quick-actions-grid">
        <div
          v-for="action in quickActions"
          :key="action.label"
          class="quick-action-item"
          :class="`quick-action-item--${action.type}`"
          @click="handleQuickAction(action)"
        >
          <div class="quick-action-item__icon">
            <el-icon :size="28">
              <component :is="action.icon" />
            </el-icon>
          </div>
          <div class="quick-action-item__label">{{ action.label }}</div>
        </div>
      </div>
    </section>

    <!-- 预警列表和近期动态 -->
    <section class="bottom-section">
      <div class="bottom-card">
        <div class="bottom-card__header">
          <div class="bottom-card__title">
            <el-icon :size="16" class="bottom-card__title-icon bottom-card__title-icon--warning"><Warning /></el-icon>
            <span>库存预警 TOP10</span>
          </div>
          <el-button link type="primary" size="default">查看全部</el-button>
        </div>
        <div class="bottom-card__body">
          <DataTable
            :data="warningListData"
            :columns="warningColumns"
            :loading="loading"
            :selectable="false"
            :stripe="true"
            :hover="true"
            :border="false"
          >
            <template #warningType="{ row }">
              <StatusTag
                :status="getWarningTypeStatus(row.warningType)"
                :label="row.warningTypeName"
                size="small"
                variant="light"
              />
            </template>
            <template #createTime="{ row }">
              <span class="time-text">{{ formatTime(row.createTime) }}</span>
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="default" @click.stop="handleViewWarning(row)">
                查看
              </el-button>
            </template>
          </DataTable>
        </div>
      </div>

      <div class="bottom-card">
        <div class="bottom-card__header">
          <div class="bottom-card__title">
            <el-icon :size="16" class="bottom-card__title-icon bottom-card__title-icon--info"><List /></el-icon>
            <span>近期动态</span>
          </div>
          <el-button link type="primary" size="default">查看全部</el-button>
        </div>
        <div class="bottom-card__body">
          <DataTable
            :data="recentLogsData"
            :columns="logColumns"
            :loading="loading"
            :selectable="false"
            :stripe="true"
            :hover="true"
            :border="false"
          >
            <template #operationType="{ row }">
              <StatusTag
                :status="getOperationTypeStatus(row.operationType)"
                :label="row.operationTypeName"
                size="small"
                variant="light"
              />
            </template>
            <template #changeAmount="{ row }">
              <span :class="row.changeAmount > 0 ? 'amount-in' : 'amount-out'">
                {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
              </span>
            </template>
            <template #createdAt="{ row }">
              <span class="time-text">{{ formatTime(row.createdAt) }}</span>
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="default" @click.stop="handleViewLog(row)">
                详情
              </el-button>
            </template>
          </DataTable>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

// 统计卡片区域 - 6个卡片
.stats-section {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1600px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

// 图表区域
.charts-section {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
  padding-bottom: var(--fts-space-4);
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--fts-space-4);

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-md);
  }

  &__header {
    padding: var(--fts-space-4) var(--fts-space-5);
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &__title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__icon {
    color: var(--fts-primary);
  }

  &__body {
    padding: var(--fts-space-4);
  }
}

.chart-container {
  width: 100%;
  height: 320px;

  &--pie {
    height: 300px;
  }
}

// 快捷功能区
.quick-actions-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-5);
  margin-bottom: var(--fts-space-4);
}

.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-4);
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--fts-space-4);

  @media (max-width: 992px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-5) var(--fts-space-3);
  border-radius: var(--fts-page-radius);
  background: var(--fts-bg-secondary);
  cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--fts-shadow-lg);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: var(--fts-page-radius);
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  &--primary {
    .quick-action-item__icon {
      background: var(--fts-primary-bg);
      color: var(--fts-primary);
    }
    &:hover {
      border-color: var(--fts-primary-light-5);
    }
  }

  &--success {
    .quick-action-item__icon {
      background: var(--fts-success-bg);
      color: var(--fts-success);
    }
    &:hover {
      border-color: var(--fts-success-light-5);
    }
  }

  &--warning {
    .quick-action-item__icon {
      background: var(--fts-warning-bg);
      color: var(--fts-warning);
    }
    &:hover {
      border-color: var(--fts-warning-light-5);
    }
  }

  &--info {
    .quick-action-item__icon {
      background: var(--fts-info-bg);
      color: var(--fts-info);
    }
    &:hover {
      border-color: var(--fts-info-light-5);
    }
  }

  &--danger {
    .quick-action-item__icon {
      background: var(--fts-error-bg);
      color: var(--fts-error);
    }
    &:hover {
      border-color: var(--fts-error-light-5);
    }
  }
}

// 底部区域（预警列表 + 近期动态）
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-4);

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.bottom-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--fts-space-4) var(--fts-space-5);
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &__title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);

    &-icon {
      &--warning {
        color: var(--fts-warning);
      }
      &--info {
        color: var(--fts-primary);
      }
    }
  }

  &__body {
    padding: var(--fts-space-2);
  }
}

// 数量变动样式
.amount-in {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.amount-out {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

// 时间文字
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 工具栏面板样式（与 FoodManagement 保持一致）
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

// 响应式适配
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

  .chart-container {
    height: 260px;

    &--pie {
      height: 240px;
    }
  }
}

@media (max-width: 576px) {
  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .stats-section {
    padding: var(--fts-space-3) 0;
  }

  .chart-card__header {
    padding: var(--fts-space-3) var(--fts-space-4);
  }

  .chart-card__body {
    padding: var(--fts-space-3);
  }

  .quick-actions-section {
    padding: var(--fts-space-4);
  }

  .bottom-card__header {
    padding: var(--fts-space-3) var(--fts-space-4);
  }
}
</style>
