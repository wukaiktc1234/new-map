<script setup lang="ts">
/**
 * 订单统计页面 - 报表看板风格
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】订单数据分析和经营状况统计
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 功能：
 * - 顶部筛选栏（时间范围、门店选择、统计维度）
 * - 核心指标卡片（6个）
 * - 图表区域（订单趋势、来源分布、商品销量排行、时段销售分布）
 * - 标签页切换（销售趋势、商品分析、门店对比）
 * - 数据明细表 + 导出Excel
 */
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Download,
  Document,
  BankCard,
  DataLine,
  TrendCharts,
  Refund,
  Wallet,
  Money,
} from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { orderStatisticsApi, orderRefundApi } from '@/api/order'
import type { DailyStats, TopSellingFood, HourlyDistribution } from '@/types/order'
import { useLayoutStore } from '@/stores/layout'
import { useECharts } from '@/composables/useECharts'

const router = useRouter()
const layoutStore = useLayoutStore()
const stripe = computed(() => layoutStore.tableStriped)
const hover = computed(() => layoutStore.tableHover)

// ==================== 类型定义 ====================

interface StatisticsRow {
  id: string
  date: string
  storeName: string
  orderCount: number
  revenue: number
  cost: number
  profit: number
  profitRate: string
}

// 时间范围快捷选项
type TimeRangeType = 'today' | 'yesterday' | 'week' | 'lastWeek' | 'month' | 'lastMonth' | 'custom'

// 统计维度
type DimensionType = 'day' | 'week' | 'month'

// 标签页类型
type TabType = 'trend' | 'product' | 'store'

// ==================== 响应式数据 ====================

const loading = ref(false)
const activeTab = ref<TabType>('trend')

// 筛选表单
const filterForm = ref({
  timeRangeType: 'week' as TimeRangeType,
  dateRange: [] as string[],
  storeName: '',
  dimension: 'day' as DimensionType,
})

// 核心统计指标
const statistics = ref({
  totalOrders: 0,
  totalAmount: '0.00',
  avgOrderAmount: '0.00',
  refundOrders: 0,
  refundAmount: '0.00',
  actualAmount: '0.00',
})

// 每日统计列表
const dailyList = ref<StatisticsRow[]>([])

// 图表数据（真实 API 数据，替代原硬编码）
// 订单来源分布（堂食/外卖/自提/打包 → 百分比），来自 getTrend() 的 orderTypeDistribution
const sourceDistribution = ref<Record<string, number>>({})
// 热销菜品列表，来自 getProductStats()
const productList = ref<TopSellingFood[]>([])
// 时段分布数据，来自 getHourStats()
const hourlyData = ref<HourlyDistribution>({})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

// 图表容器引用
const trendChartRef = ref<HTMLElement>()
const sourceChartRef = ref<HTMLElement>()
const productChartRef = ref<HTMLElement>()
const hourChartRef = ref<HTMLElement>()

// ECharts 实例
const {
  init: initTrendChart,
  setOption: setTrendOption,
  showLoading: showTrendLoading,
  hideLoading: hideTrendLoading,
} = useECharts(trendChartRef, { autoInit: false })

const {
  init: initSourceChart,
  setOption: setSourceOption,
  showLoading: showSourceLoading,
  hideLoading: hideSourceLoading,
} = useECharts(sourceChartRef, { autoInit: false })

const {
  init: initProductChart,
  setOption: setProductOption,
  showLoading: showProductLoading,
  hideLoading: hideProductLoading,
} = useECharts(productChartRef, { autoInit: false })

const {
  init: initHourChart,
  setOption: setHourOption,
  showLoading: showHourLoading,
  hideLoading: hideHourLoading,
} = useECharts(hourChartRef, { autoInit: false })

// ==================== 表格列定义 ====================

const columns = computed(() => [
  { prop: 'date', label: '日期', minWidth: 120 },
  { prop: 'storeName', label: '门店', minWidth: 140 },
  { prop: 'orderCount', label: '订单数', minWidth: 85 },
  { prop: 'revenue', label: '营业额(元)', minWidth: 130 },
  { prop: 'cost', label: '成本(元)', minWidth: 120 },
  { prop: 'profit', label: '利润(元)', minWidth: 120 },
  { prop: 'profitRate', label: '利润率', minWidth: 85 },
])

// ==================== 辅助方法 ====================

/** 获取CSS变量颜色 */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#2563EB'
}

/** 格式化利润率 */
function formatProfitRate(rate: number): string {
  const percent = rate > 1 ? rate : rate * 100
  return `${percent.toFixed(2)}%`
}

/** DailyStats 映射为表格行 */
function mapDailyToRow(item: DailyStats): StatisticsRow {
  return {
    id: item.date + item.storeName,
    date: item.date,
    storeName: item.storeName || '',
    orderCount: item.orderCount,
    revenue: parseFloat(item.revenue) || 0,
    cost: parseFloat(item.cost) || 0,
    profit: parseFloat(item.profit) || 0,
    profitRate: formatProfitRate(item.profitRate),
  }
}

/** 计算时间范围 */
function computeDateRange(type: TimeRangeType): [string, string] {
  const now = new Date()
  const formatDate = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }

  const start = new Date(now)
  const end = new Date(now)

  switch (type) {
    case 'today':
      break
    case 'yesterday':
      start.setDate(start.getDate() - 1)
      end.setDate(end.getDate() - 1)
      break
    case 'week':
      const dayOfWeek = start.getDay() || 7
      start.setDate(start.getDate() - dayOfWeek + 1)
      break
    case 'lastWeek':
      const day = start.getDay() || 7
      start.setDate(start.getDate() - day - 6)
      end.setDate(end.getDate() - day)
      break
    case 'month':
      start.setDate(1)
      break
    case 'lastMonth':
      start.setDate(1)
      start.setMonth(start.getMonth() - 1)
      end.setDate(0)
      break
    case 'custom':
    default:
      return ['', '']
  }

  return [formatDate(start), formatDate(end)]
}

// ==================== 图表配置 ====================

/** 订单趋势图配置（双Y轴：订单数 + 金额） */
function getTrendChartOption(data: DailyStats[]): Record<string, any> {
  const dates = data.map(d => d.date)
  const orderCounts = data.map(d => d.orderCount)
  const revenues = data.map(d => parseFloat(d.revenue) || 0)

  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')
  const textColor = getCssVar('--fts-text-secondary')
  const borderColor = getCssVar('--fts-border-primary')

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['订单数', '营业额'],
      bottom: 0,
      textStyle: { color: textColor },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: textColor },
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        position: 'left',
        axisLine: { show: true, lineStyle: { color: primaryColor } },
        axisLabel: { color: primaryColor },
        splitLine: { lineStyle: { color: borderColor, type: 'dashed' } },
      },
      {
        type: 'value',
        name: '营业额（元）',
        position: 'right',
        axisLine: { show: true, lineStyle: { color: successColor } },
        axisLabel: { color: successColor },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        data: orderCounts,
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
      {
        name: '营业额',
        type: 'line',
        yAxisIndex: 1,
        data: revenues,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: successColor, type: 'dashed' },
        itemStyle: { color: successColor },
      },
    ],
  }
}

/**
 * 订单来源分布饼图配置
 *
 * 数据来源：getTrend() 返回的 orderTypeDistribution（堂食/外卖/自提/打包 → 百分比数字）
 * 后端 OrderNewServiceImpl.getOrderTrends 通过 countByOrderType 统计真实订单类型分布。
 *
 * @param distribution 订单类型分布（key 为中文类型名，value 为百分比数字）
 */
function getSourceChartOption(distribution: Record<string, number>): Record<string, any> {
  // 将后端返回的分布对象转换为饼图数据格式
  // 过滤掉 0 值避免空扇区；后端无数据时 data 为空数组，饼图显示空白
  const data = Object.entries(distribution || {})
    .map(([name, value]) => ({ name, value: Number(value) || 0 }))
    .filter(item => item.value > 0)

  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')
  const warningColor = getCssVar('--fts-warning')
  const errorColor = getCssVar('--fts-error')
  const infoColor = getCssVar('--fts-info')
  const textColor = getCssVar('--fts-text-secondary')

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}% ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: textColor },
    },
    color: [primaryColor, successColor, warningColor, infoColor, errorColor],
    series: [
      {
        name: '订单来源',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: getCssVar('--fts-bg-card'),
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold',
            color: textColor,
          },
        },
        labelLine: {
          show: false,
        },
        data: data,
      },
    ],
  }
}

/**
 * 商品销量排行柱状图配置
 *
 * 数据来源：getProductStats() → GET /v1/analytics/sales/top-foods
 * 后端 SalesAnalysisServiceImpl.getTopSellingFoods 当前为 TODO，返回空数组，
 * 后端实现后图表自动显示真实数据。
 *
 * @param data 热销菜品列表（按销量降序）
 */
function getProductChartOption(data: TopSellingFood[]): Record<string, any> {
  // 将后端返回的菜品列表转换为图表数据格式
  // 字段名兼容：foodName/productName → name，totalQuantity/quantity → value
  const items = (data || [])
    .map(item => ({
      name: String(item.foodName ?? item.productName ?? '未知菜品'),
      value: Number(item.totalQuantity ?? item.quantity ?? 0),
    }))
    .filter(item => item.value > 0)

  // 按销量升序排列后反转，使最高销量在顶部（横向柱状图惯例）
  const names = items.map(d => d.name).reverse()
  const values = items.map(d => d.value).reverse()

  const primaryColor = getCssVar('--fts-primary')
  const textColor = getCssVar('--fts-text-secondary')
  const borderColor = getCssVar('--fts-border-primary')

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '8%',
      bottom: '3%',
      top: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: borderColor, type: 'dashed' } },
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: textColor },
    },
    series: [
      {
        type: 'bar',
        data: values,
        barWidth: '60%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: primaryColor + '99' },
              { offset: 1, color: primaryColor },
            ],
          },
          borderRadius: [0, 4, 4, 0],
        },
        label: {
          show: true,
          position: 'right',
          color: textColor,
          fontSize: 12,
        },
      },
    ],
  }
}

/**
 * 时段销售分布柱状图配置
 *
 * 数据来源：getHourStats() → GET /v1/analytics/sales/hourly-distribution
 * 后端返回 { hourlyData: [{ hour, orderCount, amount }], date }
 * 后端已实现基础结构（24 小时框架），当前 orderCount/amount 全为 0，待接入真实统计。
 *
 * @param data 时段分布数据（含 24 小时数据列表）
 */
function getHourChartOption(data: HourlyDistribution): Record<string, any> {
  // 将后端返回的小时数据转换为图表坐标轴数据
  // 后端返回 0-23 共 24 小时数据；格式化为 HH:00 标签
  const hourlyList = data?.hourlyData ?? []
  const hours = hourlyList.map(item => {
    const h = Number(item.hour) || 0
    return `${String(h).padStart(2, '0')}:00`
  })
  const orderCounts = hourlyList.map(item => Number(item.orderCount) || 0)

  const primaryColor = getCssVar('--fts-primary')
  const textColor = getCssVar('--fts-text-secondary')
  const borderColor = getCssVar('--fts-border-primary')

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: textColor },
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: borderColor } },
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: borderColor, type: 'dashed' } },
    },
    series: [
      {
        type: 'bar',
        data: orderCounts,
        barWidth: '50%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: primaryColor },
              { offset: 1, color: primaryColor + '66' },
            ],
          },
          borderRadius: [4, 4, 0, 0],
        },
      },
    ],
  }
}

// ==================== 数据加载 ====================

/** 加载每日统计数据 */
async function loadData() {
  loading.value = true
  try {
    let startDate = filterForm.value.dateRange?.[0]
    let endDate = filterForm.value.dateRange?.[1]

    if (filterForm.value.timeRangeType !== 'custom') {
      const range = computeDateRange(filterForm.value.timeRangeType)
      startDate = range[0]
      endDate = range[1]
    }

    // 并行获取每日统计和退款统计（退款统计为全量数据，暂未支持时间范围过滤）
    const [res, refundStats] = await Promise.all([
      orderStatisticsApi.getDailyStats({
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        storeName: filterForm.value.storeName || undefined,
      }),
      orderRefundApi.getStats().catch(() => null),
    ])

    dailyList.value = (res || []).map(mapDailyToRow)
    pagination.total = dailyList.value.length

    // 计算核心指标（使用真实退款数据）
    computeStatistics(dailyList.value, refundStats)

    // 更新图表（传入时间范围用于获取订单来源分布、热销菜品、时段分布数据）
    updateCharts(dailyList.value, startDate, endDate)
  } catch (error: unknown) {
    dailyList.value = []
    pagination.total = 0
    if (error instanceof Error) {
      ElMessage.error('加载统计数据失败')
    }
  } finally {
    loading.value = false
  }
}

/** 计算核心统计指标 */
function computeStatistics(data: StatisticsRow[], refundStats: { pendingCount: number; refundedAmount: string } | null = null) {
  const totalOrders = data.reduce((sum, item) => sum + item.orderCount, 0)
  const totalRevenue = data.reduce((sum, item) => sum + item.revenue, 0)
  const avgOrder = totalOrders > 0 ? totalRevenue / totalOrders : 0

  // 使用真实退款数据（若获取失败则归零）
  const refundAmountNum = refundStats ? Number(refundStats.refundedAmount) : 0
  const actualAmount = Math.max(0, totalRevenue - refundAmountNum)

  statistics.value = {
    totalOrders,
    totalAmount: totalRevenue.toFixed(2),
    avgOrderAmount: avgOrder.toFixed(2),
    refundOrders: refundStats?.pendingCount ?? 0,
    refundAmount: refundAmountNum.toFixed(2),
    actualAmount: actualAmount.toFixed(2),
  }
}

/**
 * 更新所有图表
 *
 * 并行获取 3 个图表的真实 API 数据：
 * - 订单来源分布：getTrend() 返回的 orderTypeDistribution（堂食/外卖/自提/打包占比）
 * - 商品销量排行：getProductStats() → /v1/analytics/sales/top-foods
 * - 时段销售分布：getHourStats() → /v1/analytics/sales/hourly-distribution
 *
 * 任意 API 失败时对应图表显示空数据，不影响其他图表。
 */
async function updateCharts(data: DailyStats[], startDate?: string, endDate?: string) {
  try {
    showTrendLoading()
    showSourceLoading()
    showProductLoading()
    showHourLoading()

    await nextTick()

    // 并行获取 3 个图表数据（每个独立 catch，失败时返回空数据，不影响其他图表）
    const [trendRes, productRes, hourRes] = await Promise.all([
      orderStatisticsApi.getTrend({
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      }).catch(() => ({})),
      orderStatisticsApi.getProductStats({
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        limit: 10,
      }).catch(() => []),
      orderStatisticsApi.getHourStats(endDate || undefined).catch(() => ({})),
    ])

    // 存储到响应式变量（供未来扩展或调试使用）
    sourceDistribution.value = (trendRes?.orderTypeDistribution as Record<string, number>) ?? {}
    productList.value = productRes ?? []
    hourlyData.value = hourRes ?? {}

    await initTrendChart()
    await initSourceChart()
    await initProductChart()
    await initHourChart()

    setTrendOption(getTrendChartOption(data))
    setSourceOption(getSourceChartOption(sourceDistribution.value))
    setProductOption(getProductChartOption(productList.value))
    setHourOption(getHourChartOption(hourlyData.value))

    hideTrendLoading()
    hideSourceLoading()
    hideProductLoading()
    hideHourLoading()
  } catch (e) {
    console.error('图表初始化失败:', e)
  }
}

// ==================== 事件处理 ====================

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  filterForm.value.timeRangeType = 'week'
  filterForm.value.dateRange = []
  filterForm.value.storeName = ''
  filterForm.value.dimension = 'day'
  loadData()
}

function handleViewDetail(row: StatisticsRow) {
  router.push({
    path: '/order/query',
    query: { date: row.date, store: row.storeName },
  })
}

/** 导出Excel */
async function handleExport() {
  try {
    ElMessage.info('正在导出数据，请稍候...')

    let csvContent = '\uFEFF'
    csvContent += '日期,门店,订单数,营业额(元),成本(元),利润(元),利润率\n'

    dailyList.value.forEach(row => {
      csvContent += `${row.date},${row.storeName},${row.orderCount},${row.revenue.toFixed(2)},${row.cost.toFixed(2)},${row.profit.toFixed(2)},${row.profitRate}\n`
    })

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `订单统计_${timestamp}.csv`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch (error) {
    ElMessage.error('导出失败，请稍后重试')
  }
}

/** 时间范围快捷选项切换 */
function handleTimeRangeChange(type: TimeRangeType) {
  filterForm.value.timeRangeType = type
  if (type !== 'custom') {
    filterForm.value.dateRange = []
    // 切换快捷日期范围后自动重新加载数据
    loadData()
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadData()
})

// 监听标签页切换，重新调整图表大小
watch(activeTab, () => {
  nextTick(() => {
    if (activeTab.value === 'trend') {
      // 触发图表 resize
      window.dispatchEvent(new Event('resize'))
    }
  })
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader
      title="订单统计"
      description="订单数据分析和经营状况统计"
    >
      <el-button size="default" @click="handleReset">
        <el-icon :size="14"><Refresh /></el-icon>重置
      </el-button>
      <el-button type="success" size="default" @click="handleExport">
        <el-icon :size="14"><Download /></el-icon>导出Excel
      </el-button>
    </PageHeader>

    <!-- 核心指标卡片区（6个） -->
    <section class="stats-section">
      <StatCard
        icon="Document"
        label="订单总数"
        :value="String(statistics.totalOrders)"
        color-type="primary"
        variant="bordered"
      />
      <StatCard
        icon="BankCard"
        label="订单总金额"
        :value="`¥${statistics.totalAmount}`"
        color-type="success"
        variant="bordered"
      />
      <StatCard
        icon="DataLine"
        label="客单价"
        :value="`¥${statistics.avgOrderAmount}`"
        color-type="info"
        variant="bordered"
      />
      <StatCard
        icon="Refund"
        label="退款订单数"
        :value="String(statistics.refundOrders)"
        color-type="warning"
        variant="bordered"
      />
      <StatCard
        icon="Money"
        label="退款金额"
        :value="`¥${statistics.refundAmount}`"
        color-type="error"
        variant="bordered"
      />
      <StatCard
        icon="Wallet"
        label="实收金额"
        :value="`¥${statistics.actualAmount}`"
        color-type="primary"
        variant="bordered"
      />
    </section>

    <!-- 筛选栏 -->
    <section class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <!-- 时间范围快捷选项 -->
          <div class="time-range-group">
            <el-radio-group v-model="filterForm.timeRangeType" size="default" @change="handleTimeRangeChange">
              <el-radio-button value="today">今日</el-radio-button>
              <el-radio-button value="yesterday">昨日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="lastWeek">上周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
              <el-radio-button value="lastMonth">上月</el-radio-button>
              <el-radio-button value="custom">自定义</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 自定义日期范围 -->
          <el-date-picker
            v-if="filterForm.timeRangeType === 'custom'"
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            placement="bottom-start"
            style="width: 240px"
          />

          <!-- 门店选择 -->
          <el-input
            v-model="filterForm.storeName"
            placeholder="门店名称"
            clearable
            style="width: 150px"
          />

          <!-- 统计维度 -->
          <el-select
            v-model="filterForm.dimension"
            placeholder="统计维度"
            style="width: 120px"
          >
            <el-option label="按天" value="day" />
            <el-option label="按周" value="week" />
            <el-option label="按月" value="month" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">
            <el-icon :size="14"><Search /></el-icon>查询
          </el-button>
        </div>
      </div>
    </section>

    <!-- 标签页切换 -->
    <section class="tabs-section">
      <el-tabs v-model="activeTab" class="statistics-tabs">
        <el-tab-pane label="销售趋势" name="trend">
          <!-- 图表区域 -->
          <div class="charts-grid">
            <!-- 订单趋势图 -->
            <div class="chart-card">
              <div class="chart-card__header">
                <span class="chart-card__title">订单趋势</span>
                <span class="chart-card__subtitle">订单数与营业额走势</span>
              </div>
              <div ref="trendChartRef" class="chart-container" />
            </div>

            <!-- 时段销售分布 -->
            <div class="chart-card">
              <div class="chart-card__header">
                <span class="chart-card__title">时段销售分布</span>
                <span class="chart-card__subtitle">按时段统计订单</span>
              </div>
              <div ref="hourChartRef" class="chart-container" />
            </div>

            <!-- 订单来源分布 -->
            <div class="chart-card">
              <div class="chart-card__header">
                <span class="chart-card__title">订单来源分布</span>
                <span class="chart-card__subtitle">各渠道占比</span>
              </div>
              <div ref="sourceChartRef" class="chart-container" />
            </div>

            <!-- 商品销量排行 -->
            <div class="chart-card">
              <div class="chart-card__header">
                <span class="chart-card__title">商品销量排行</span>
                <span class="chart-card__subtitle">TOP10热销商品</span>
              </div>
              <div ref="productChartRef" class="chart-container" />
            </div>
          </div>

          <!-- 数据明细表 -->
          <div class="detail-section">
            <div class="detail-section__header">
              <span class="detail-section__title">数据明细</span>
              <span class="detail-section__count">共 {{ pagination.total }} 条记录</span>
            </div>
            <div class="table-section">
              <DataTable
                :data="dailyList"
                :columns="columns"
                :stripe="stripe"
                :hover="hover"
                :loading="loading"
                :selectable="false"
                :actions-width="120"
                row-key="id"
              >
                <template #actions="{ row }">
                  <el-button link type="primary" size="small" @click="handleViewDetail(row)">
                    查看详情
                  </el-button>
                </template>
              </DataTable>
            </div>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                :page-sizes="[20, 50, 100]"
                layout="total, sizes, prev, pager, next"
              />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="商品分析" name="product">
          <div class="empty-tab">
            <el-empty description="商品分析功能开发中..." />
          </div>
        </el-tab-pane>

        <el-tab-pane label="门店对比" name="store">
          <div class="empty-tab">
            <el-empty description="门店对比功能开发中..." />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
  padding-bottom: var(--fts-space-6);
}

// 统计卡片区域 - 6个
.stats-section {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6);

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

// 工具栏面板
.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  margin: 0 var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;

  .time-range-group {
    :deep(.el-radio-button__inner) {
      padding: 8px 15px;
    }
  }
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

// 标签页区域
.tabs-section {
  margin: 0 var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);

  :deep(.el-tabs) {
    --el-tabs-header-height: 50px;
  }

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  :deep(.el-tabs__item) {
    height: 50px;
    line-height: 50px;
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-secondary);

    &.is-active {
      color: var(--fts-primary);
      font-weight: var(--fts-font-weight-semibold);
    }
  }

  :deep(.el-tabs__content) {
    padding: var(--fts-space-4);
  }
}

// 图表网格布局
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

// 图表卡片
.chart-card {
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-4);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  &--wide {
    grid-column: span 1;

    @media (min-width: 1201px) {
      grid-column: span 2;
    }
  }

  &__header {
    display: flex;
    align-items: baseline;
    gap: var(--fts-space-2);
    margin-bottom: var(--fts-space-3);
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__subtitle {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

.chart-container {
  width: 100%;
  height: 320px;

  @media (max-width: 768px) {
    height: 260px;
  }
}

// 数据明细区域
.detail-section {
  margin-top: var(--fts-space-4);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    padding: 0 var(--fts-space-2);
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__count {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }
}

// 表格区域
.table-section {
  overflow-x: auto;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius) var(--fts-card-radius) 0 0;

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

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-card-radius) var(--fts-card-radius);
}

// 空标签页
.empty-tab {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  padding: var(--fts-space-8);
}

// 响应式调整
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

  .stats-section {
    padding: var(--fts-space-3) var(--fts-space-4);
  }

  .advanced-search-panel {
    margin: 0 var(--fts-space-4);
    padding: var(--fts-space-2) var(--fts-space-3);
  }

  .tabs-section {
    margin: 0 var(--fts-space-4);
  }
}

@media (max-width: 576px) {
  .pagination-wrapper {
    padding: var(--fts-space-3);
    justify-content: center;
  }
}
</style>
