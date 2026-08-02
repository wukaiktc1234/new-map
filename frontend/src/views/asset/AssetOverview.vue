<script setup lang="ts">
/**
 * 资产总览页面 - 看板/仪表盘风格
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】企业资产数据概览和统计分析
 * 【依赖】L3(PageHeader/StatCard/DataTable/StatusTag) + ECharts
 *
 * 功能模块：
 * 1. 页面标题栏
 * 2. 统计卡片（6个）
 * 3. 图表区域（4个图表）
 * 4. 快捷功能区
 * 5. 近期资产动态列表
 * 6. 预警提醒
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Warning,
  List,
  DataLine,
  PieChart,
  Histogram,
  CircleCheck,
  Plus,
  Upload,
  Download,
  Document,
  Delete,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'

import { useEChartsTheme } from '@/composables/useEChartsTheme'
import { assetApi } from '@/api/asset'

import type { AssetStatisticsVO, CategoryStatVO, MonthlyTrendItem } from '@/types/asset'
import { AssetStatus } from '@/types/asset'

const { chartColors, xAxisBaseOption, yAxisBaseOption, tooltipOption, legendOption, pieLabelStyle } = useEChartsTheme()

// ==================== 响应式数据 ====================

const loading = ref(false)

/** 统计概览数据 */
const statsOverview = ref<AssetStatisticsVO | null>(null)

/** 资产分类分布数据 */
const categoryStatsData = ref<CategoryStatVO[]>([])

/** 资产价值趋势数据（近12个月） */
const valueTrendData = ref<MonthlyTrendItem[]>([])

/** 部门资产占比数据 */
const departmentStatsData = ref<Array<{ deptId: string; deptName: string; assetCount: number; totalValue: number }>>([])

/** 资产状态分布数据 */
const statusStatsData = ref<Array<{ status: string; statusName: string; count: number }>>([])

/** 近期资产动态列表 */
const recentActivityData = ref<Array<{
  id: string
  assetCode: string
  assetName: string
  activityType: string
  activityTypeName: string
  operatorName: string
  createTime: string
}>>([])

/** 预警提醒列表 */
const warningListData = ref<Array<{
  id: string
  assetCode: string
  assetName: string
  warningType: string
  warningTypeName: string
  warningLevel: string
  createTime: string
}>>([])

// ==================== 图表引用 ====================

const categoryPieChartRef = ref<HTMLElement | null>(null)
const valueTrendChartRef = ref<HTMLElement | null>(null)
const deptBarChartRef = ref<HTMLElement | null>(null)
const statusRingChartRef = ref<HTMLElement | null>(null)

let categoryPieChartInstance: echarts.ECharts | null = null
let valueTrendChartInstance: echarts.ECharts | null = null
let deptBarChartInstance: echarts.ECharts | null = null
let statusRingChartInstance: echarts.ECharts | null = null

// ==================== 计算属性 ====================

/** 统计卡片数据（6个） */
const statCards = computed(() => {
  const s = statsOverview.value
  return [
    {
      icon: 'Grid',
      label: '资产总数',
      value: s?.totalAssets ?? 0,
      colorType: 'primary' as const,
    },
    {
      icon: 'Money',
      label: '资产总值',
      value: s ? `¥${s.totalCurrentValueYuan}` : '¥0.00',
      colorType: 'success' as const,
    },
    {
      icon: 'CircleCheck',
      label: '在用资产',
      value: s?.activeCount ?? 0,
      colorType: 'success' as const,
    },
    {
      icon: 'Timer',
      label: '闲置资产',
      value: s?.idleCount ?? 0,
      colorType: 'warning' as const,
    },
    {
      icon: 'Delete',
      label: '报废资产',
      value: s?.scrappedCount ?? 0,
      colorType: 'error' as const,
    },
    {
      icon: 'Plus',
      label: '本月新增',
      value: s?.thisMonthNewCount ?? 0,
      colorType: 'info' as const,
    },
  ]
})

/** 快捷功能列表 */
const quickActions = [
  { icon: Plus, label: '资产入库', type: 'primary' },
  { icon: Upload, label: '资产领用', type: 'success' },
  { icon: Download, label: '资产归还', type: 'warning' },
  { icon: Document, label: '资产盘点', type: 'info' },
  { icon: Delete, label: '资产报废', type: 'danger' },
]

/** 近期动态表格列 */
const activityColumns = computed<DataTableColumn[]>(() => [
  { prop: 'assetCode', label: '资产编号', minWidth: 120 },
  { prop: 'assetName', label: '资产名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'activityTypeName', label: '操作类型', minWidth: 100, slot: 'activityType' },
  { prop: 'operatorName', label: '操作人', minWidth: 100 },
  { prop: 'createTime', label: '操作时间', minWidth: 160, slot: 'createTime' },
])

/** 预警列表表格列 */
const warningColumns = computed<DataTableColumn[]>(() => [
  { prop: 'assetCode', label: '资产编号', minWidth: 120 },
  { prop: 'assetName', label: '资产名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'warningTypeName', label: '预警类型', minWidth: 120, slot: 'warningType' },
  { prop: 'warningLevel', label: '级别', minWidth: 80, slot: 'warningLevel' },
  { prop: 'createTime', label: '预警时间', minWidth: 160, slot: 'createTime' },
])

// ==================== 辅助方法 ====================

/** 从 CSS 变量获取颜色 */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#2563EB'
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 获取操作类型对应的状态颜色 */
function getActivityTypeStatus(type: string): string {
  const map: Record<string, string> = {
    stock_in: 'success',
    receive: 'primary',
    return: 'warning',
    inventory: 'info',
    scrap: 'error',
    maintenance: 'info',
    transfer: 'warning',
  }
  return map[type] || 'info'
}

/** 获取预警类型对应的状态颜色 */
function getWarningTypeStatus(type: string): string {
  const map: Record<string, string> = {
    depreciation_complete: 'warning',
    pending_inventory: 'info',
    pending_maintenance: 'error',
    overdue_maintenance: 'error',
    idle_long: 'warning',
  }
  return map[type] || 'info'
}

/** 获取预警级别标签 */
function getWarningLevelTag(level: string): { status: string; label: string } {
  const map: Record<string, { status: string; label: string }> = {
    high: { status: 'error', label: '高' },
    medium: { status: 'warning', label: '中' },
    low: { status: 'info', label: '低' },
  }
  return map[level] || { status: 'info', label: '低' }
}

// ==================== 数据加载 ====================

/** 加载统计概览（真实 API：/v1/asset/overview） */
async function loadStatsOverview(): Promise<void> {
  try {
    const data = await assetApi.getStatisticsVO()
    statsOverview.value = data
  } catch (err) {
    statsOverview.value = null
    // eslint-disable-next-line no-console
    console.error('[AssetOverview] 加载统计概览失败:', err)
  }
}

/** 加载分类统计数据（TODO: 待接入后端分类统计 API） */
async function loadCategoryStats(): Promise<void> {
  try {
    // TODO: 后端暂无分类统计接口，待 API 就绪后替换
    categoryStatsData.value = []
  } catch {
    categoryStatsData.value = []
  }
}

/** 加载价值趋势数据（TODO: 待接入后端月度趋势 API） */
async function loadValueTrend(): Promise<void> {
  try {
    // TODO: 后端暂无月度趋势接口，待 API 就绪后替换
    valueTrendData.value = []
  } catch {
    valueTrendData.value = []
  }
}

/** 加载部门资产数据（TODO: 待接入后端部门资产统计 API） */
async function loadDeptStats(): Promise<void> {
  try {
    // TODO: 后端暂无部门资产统计接口，待 API 就绪后替换
    departmentStatsData.value = []
  } catch {
    departmentStatsData.value = []
  }
}

/** 加载状态分布数据（基于统计概览派生） */
async function loadStatusStats(): Promise<void> {
  try {
    const s = statsOverview.value
    if (s) {
      statusStatsData.value = [
        { status: AssetStatus.ACTIVE, statusName: '在用', count: s.activeCount },
        { status: AssetStatus.IDLE, statusName: '闲置', count: s.idleCount },
        { status: AssetStatus.MAINTENANCE, statusName: '维修中', count: s.maintenanceCount },
        { status: AssetStatus.TO_BE_DISPOSED, statusName: '待处置', count: s.toBeDisposedCount },
        { status: AssetStatus.SCRAPPED, statusName: '已报废', count: s.scrappedCount },
        { status: AssetStatus.DISPOSED, statusName: '已处置', count: s.disposedCount },
      ]
    } else {
      statusStatsData.value = []
    }
  } catch {
    statusStatsData.value = []
  }
}

/** 加载近期动态（TODO: 待接入后端资产动态 API） */
async function loadRecentActivity(): Promise<void> {
  try {
    // TODO: 后端暂无资产动态接口，待 API 就绪后替换
    recentActivityData.value = []
  } catch {
    recentActivityData.value = []
  }
}

/** 加载预警列表（TODO: 待接入后端资产预警 API） */
async function loadWarningList(): Promise<void> {
  try {
    // TODO: 后端暂无资产预警接口，待 API 就绪后替换
    warningListData.value = []
  } catch {
    warningListData.value = []
  }
}

/** 加载所有数据 */
async function loadAllData(): Promise<void> {
  loading.value = true
  try {
    await loadStatsOverview()
    await Promise.all([
      loadCategoryStats(),
      loadValueTrend(),
      loadDeptStats(),
      loadStatusStats(),
      loadRecentActivity(),
      loadWarningList(),
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

/** 资产分类分布饼图配置 */
function getCategoryPieChartOption(): EChartsOption {
  const data = categoryStatsData.value
  const pieData = data.map(item => ({
    name: item.categoryName,
    value: item.assetCount,
  }))

  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')
  const warningColor = getCssVar('--fts-warning')
  const errorColor = getCssVar('--fts-error')
  const infoColor = getCssVar('--fts-info')

  const colors = [primaryColor, successColor, warningColor, infoColor, errorColor, '#8B5CF6', '#EC4899']

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'item',
      formatter: '{b}: {c}台 ({d}%)',
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
        name: '资产分类',
        type: 'pie',
        radius: ['0%', '70%'],
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

/** 资产价值趋势折线图配置 */
function getValueTrendChartOption(): EChartsOption {
  const data = valueTrendData.value
  const months = data.map(item => item.month)
  const values = data.map(item => item.totalValue / 10000)

  const primaryColor = getCssVar('--fts-primary')

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'axis',
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        return `${p.name}<br/>资产总值：¥${p.value.toFixed(2)}万`
      },
    },
    legend: {
      ...legendOption.value,
      data: ['资产总值'],
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
      data: months,
      ...xAxisBaseOption.value,
    },
    yAxis: {
      type: 'value',
      name: '金额（万元）',
      ...yAxisBaseOption.value,
    },
    series: [
      {
        name: '资产总值',
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

/** 部门资产占比柱状图配置 */
function getDeptBarChartOption(): EChartsOption {
  const data = departmentStatsData.value
  const depts = data.map(item => item.deptName)
  const counts = data.map(item => item.assetCount)

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
      data: depts,
      axisLabel: {
        ...xAxisBaseOption.value.axisLabel,
        rotate: 30,
      },
      axisLine: xAxisBaseOption.value.axisLine,
      splitLine: xAxisBaseOption.value.splitLine,
    },
    yAxis: {
      type: 'value',
      name: '数量（台）',
      ...yAxisBaseOption.value,
    },
    series: [
      {
        name: '资产数量',
        type: 'bar',
        data: counts,
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

/** 资产状态分布环形图配置 */
function getStatusRingChartOption(): EChartsOption {
  const data = statusStatsData.value
  const ringData = data.map(item => ({
    name: item.statusName,
    value: item.count,
  }))

  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')
  const warningColor = getCssVar('--fts-warning')
  const errorColor = getCssVar('--fts-error')
  const infoColor = getCssVar('--fts-info')

  const colors = [successColor, warningColor, infoColor, errorColor, '#8B5CF6', '#6B7280']

  return {
    ...tooltipOption.value,
    tooltip: {
      ...tooltipOption.value.tooltip,
      trigger: 'item',
      formatter: '{b}: {c}台 ({d}%)',
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
        name: '资产状态',
        type: 'pie',
        radius: ['50%', '75%'],
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
        data: ringData,
      },
    ],
  }
}

// ==================== 图表初始化 ====================

/** 初始化所有图表 */
function initAllCharts(): void {
  initCategoryPieChart()
  initValueTrendChart()
  initDeptBarChart()
  initStatusRingChart()
}

/** 初始化资产分类饼图 */
function initCategoryPieChart(): void {
  if (!categoryPieChartRef.value) return
  if (categoryPieChartInstance) {
    categoryPieChartInstance.dispose()
  }
  categoryPieChartInstance = echarts.init(categoryPieChartRef.value)
  categoryPieChartInstance.setOption(getCategoryPieChartOption())
}

/** 初始化资产价值趋势图 */
function initValueTrendChart(): void {
  if (!valueTrendChartRef.value) return
  if (valueTrendChartInstance) {
    valueTrendChartInstance.dispose()
  }
  valueTrendChartInstance = echarts.init(valueTrendChartRef.value)
  valueTrendChartInstance.setOption(getValueTrendChartOption())
}

/** 初始化部门资产柱状图 */
function initDeptBarChart(): void {
  if (!deptBarChartRef.value) return
  if (deptBarChartInstance) {
    deptBarChartInstance.dispose()
  }
  deptBarChartInstance = echarts.init(deptBarChartRef.value)
  deptBarChartInstance.setOption(getDeptBarChartOption())
}

/** 初始化资产状态环形图 */
function initStatusRingChart(): void {
  if (!statusRingChartRef.value) return
  if (statusRingChartInstance) {
    statusRingChartInstance.dispose()
  }
  statusRingChartInstance = echarts.init(statusRingChartRef.value)
  statusRingChartInstance.setOption(getStatusRingChartOption())
}

/** 窗口大小变化时重绘图表 */
function handleResize(): void {
  categoryPieChartInstance?.resize()
  valueTrendChartInstance?.resize()
  deptBarChartInstance?.resize()
  statusRingChartInstance?.resize()
}

// ==================== 事件处理 ====================

/** 刷新数据 */
function handleRefresh(): void {
  loadAllData()
}

/** 快捷功能点击 */
function handleQuickAction(action: typeof quickActions[number]): void {
  ElMessage.info(`${action.label}功能开发中...`)
}

/** 查看动态详情 */
function handleViewActivity(row: typeof recentActivityData.value[number]): void {
  ElMessage.info(`查看动态详情：${row.assetName}`)
}

/** 查看预警详情 */
function handleViewWarning(row: typeof warningListData.value[number]): void {
  ElMessage.info(`查看预警详情：${row.assetName}`)
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  categoryPieChartInstance?.dispose()
  valueTrendChartInstance?.dispose()
  deptBarChartInstance?.dispose()
  statusRingChartInstance?.dispose()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="资产概览" description="企业资产数据概览和统计分析">
      <el-button type="primary" size="default" @click="handleRefresh">
        <el-icon :size="16"><Refresh /></el-icon>刷新数据
      </el-button>
    </PageHeader>

    <!-- 统计卡片区域 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in statCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="String(stat.value)"
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
              <span>资产价值趋势（近12个月）</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="valueTrendChartRef" class="chart-container" />
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><PieChart /></el-icon>
              <span>资产分类分布</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="categoryPieChartRef" class="chart-container chart-container--pie" />
          </div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-card">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><Histogram /></el-icon>
              <span>部门资产占比</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="deptBarChartRef" class="chart-container" />
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-card__header">
            <div class="chart-card__title">
              <el-icon :size="16" class="chart-card__icon"><CircleCheck /></el-icon>
              <span>资产状态分布</span>
            </div>
          </div>
          <div class="chart-card__body">
            <div ref="statusRingChartRef" class="chart-container chart-container--pie" />
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

    <!-- 底部区域：近期动态 + 预警提醒 -->
    <section class="bottom-section">
      <div class="bottom-card">
        <div class="bottom-card__header">
          <div class="bottom-card__title">
            <el-icon :size="16" class="bottom-card__title-icon bottom-card__title-icon--info"><List /></el-icon>
            <span>近期资产动态</span>
          </div>
          <el-button link type="primary" size="default">查看全部</el-button>
        </div>
        <div class="bottom-card__body">
          <DataTable
            :data="recentActivityData"
            :columns="activityColumns"
            :loading="loading"
            :selectable="false"
            :stripe="true"
            :hover="true"
            :border="false"
          >
            <template #activityType="{ row }">
              <StatusTag
                :status="getActivityTypeStatus(row.activityType)"
                :label="row.activityTypeName"
                size="small"
                variant="light"
              />
            </template>
            <template #createTime="{ row }">
              <span class="time-text">{{ formatTime(row.createTime) }}</span>
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="default" @click.stop="handleViewActivity(row)">
                详情
              </el-button>
            </template>
          </DataTable>
        </div>
      </div>

      <div class="bottom-card">
        <div class="bottom-card__header">
          <div class="bottom-card__title">
            <el-icon :size="16" class="bottom-card__title-icon bottom-card__title-icon--warning"><Warning /></el-icon>
            <span>预警提醒</span>
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
            <template #warningLevel="{ row }">
              <StatusTag
                :status="getWarningLevelTag(row.warningLevel).status"
                :label="getWarningLevelTag(row.warningLevel).label"
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
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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

// 底部区域（近期动态 + 预警提醒）
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

// 时间文字
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 响应式适配
@media (max-width: 768px) {
  .chart-container {
    height: 260px;

    &--pie {
      height: 240px;
    }
  }
}

@media (max-width: 576px) {
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
