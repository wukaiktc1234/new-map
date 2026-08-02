<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Shop, Money, Document, User, TrendCharts, RefreshRight } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { useECharts } from '@/composables/useECharts'
import { liveMonitorApi } from '@/api/operations/live-monitor'
import type { StoreRecord, TrendDimension } from '@/api/operations/live-monitor'

/* ===== 类型定义 ===== */
// StoreRecord 类型从 @/api/operations/live-monitor 导入，保持与 API 层一致

/* ===== 状态管理 ===== */
const { pagination } = useStandardPage()
const loading = ref(false)
const autoRefreshEnabled = ref(true)
const timeDimension = ref<'24h' | '7d' | '30d'>('24h')
const refreshInterval = ref(30)

/* ===== Tab 状态（参考税务管理 el-tabs border-card 用法） ===== */
const activeTab = ref<'realtime' | 'trend' | 'abnormal'>('realtime')

/** Tab 切换回调（参考税务管理 handleTabChange） */
async function handleTabChange(_tab: string | number): Promise<void> {
  // 切换到趋势分析时，容器从隐藏变为可见，ECharts 需要重新计算尺寸
  if (_tab === 'trend') {
    await nextTick()
    if (!trendChart.chartInstance.value) {
      await trendChart.init()
      await initTrendChart()
    }
    trendChart.resize()
  }
}

// 搜索表单状态
const dateRange = ref<[string, string]>([
  new Date().toISOString().split('T')[0],
  new Date().toISOString().split('T')[0]
])
const selectedStore = ref('')
const shiftType = ref('')

// 图表容器引用
const revenueChartRef = ref<HTMLElement>()
const orderSourceChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()

// ECharts 实例
const revenueChart = useECharts(revenueChartRef)
const orderSourceChart = useECharts(orderSourceChartRef)
const trendChart = useECharts(trendChartRef)

// 自动刷新定时器
let refreshTimer: ReturnType<typeof setInterval> | null = null

// 数据状态
const storeRecords = ref<StoreRecord[]>([])
const statsCards = ref<Array<{ icon: string; label: string; value: string | number; colorType: 'primary' | 'success' | 'warning' | 'error' | 'info'; trend?: number }>>([])

// 首次加载标志（用于区分完整初始化 vs 增量更新）
let isFirstLoad = true

// 上一次的数据快照（用于变化检测）
let lastDataSnapshot: StoreRecord[] | null = null

/** 获取 CSS 变量值 */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/** 获取饼图边框颜色：深色模式与卡片背景一致，浅色模式使用白色 */
function getChartBorderColor(): string {
  const primary = getCssVar('--fts-text-primary')
  const isDark = primary === '#ffffff' || primary === '#fff'
    || document.documentElement.getAttribute('data-theme') === 'dark'
    || document.documentElement.classList.contains('dark')
  return isDark ? '#141414' : '#ffffff'
}

/* ===== 静态配置 ===== */

/** 门店名称列表（用于搜索下拉框） */
const STORE_NAMES = ['天府大道店', '春熙路店', '高新店', '双流店', '龙泉店', '锦江店', '武侯店']

// 注：Mock 数据已移至 @/api/operations/live-monitor.ts，由 API 层统一管理 fallback

/** 计算营收均值用于异常检测 */
const revenueAverage = computed(() => {
  if (storeRecords.value.length === 0) return 0
  const total = storeRecords.value.reduce((sum, s) => sum + s.todayRevenue, 0)
  return total / storeRecords.value.length
})

/** 判断门店是否异常（营收低于均值50%） */
function isAbnormalStore(revenue: number): boolean {
  return revenue < revenueAverage.value * 0.5
}

/* ===== 图表初始化 ===== */

/**
 * 初始化营收对比柱状图
 * @param stores - 门店数据
 * @param withAnimation - 是否启用初始动画（首次加载时启用）
 */
function initRevenueChart(stores: StoreRecord[], withAnimation: boolean = false) {
  const primaryColor = getCssVar('--fts-primary')
  const errorColor = getCssVar('--fts-error')

  // 根据是否异常设置颜色
  const barData = stores.map(s => ({
    value: s.todayRevenue,
    itemStyle: {
      color: isAbnormalStore(s.todayRevenue) ? errorColor : primaryColor,
      borderRadius: [4, 4, 0, 0]
    }
  }))

  revenueChart.setOption({
    title: {
      text: '各门店营收对比',
      left: 'center',
      textStyle: { fontSize: 14, color: getCssVar('--fts-text-primary') }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const p = params as Array<{ name: string; value: number; marker: string }>
        if (!p || p.length === 0) return ''
        const item = p[0]
        const store = stores.find(s => s.storeName === item.name)
        const avg = revenueAverage.value
        const isLow = item.value < avg * 0.5
        return `${item.name}<br/>${item.marker} 今日营收: ¥${item.value.toLocaleString()}<br/>${store ? `昨日营收: ¥${store.yesterdayRevenue.toLocaleString()}` : ''}<br/>均值: ¥${avg.toFixed(0)}<br/>${isLow ? '<span style="color:' + errorColor + '">⚠ 低于均值50%</span>' : '✓ 正常'}`
      }
    },
    legend: { data: ['今日营收', '均值线'], top: 30 },
    grid: { left: '3%', right: '4%', bottom: '3%' },
    xAxis: {
      type: 'category',
      data: stores.map(s => s.storeName),
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: { type: 'value', name: '营收(元)' },
    animation: withAnimation,
    animationDuration: withAnimation ? 1200 : 0,
    animationEasing: withAnimation ? 'elasticOut' : undefined,
    series: [
      {
        name: '今日营收',
        type: 'bar',
        data: barData,
        barWidth: '50%',
        animation: withAnimation,
        animationDuration: withAnimation ? 1000 : 0,
        animationDelay: (idx: number) => idx * 50
      },
      {
        name: '均值线',
        type: 'line',
        data: stores.map(() => revenueAverage.value),
        lineStyle: { color: getCssVar('--fts-warning'), type: 'dashed', width: 2 },
        symbol: 'none',
        animation: withAnimation,
        animationDuration: withAnimation ? 1500 : 0
      }
    ]
  })
}

/**
 * 初始化订单来源饼图
 * @param stores - 门店数据
 * @param withAnimation - 是否启用初始动画（首次加载时启用）
 */
function initOrderSourceChart(stores: StoreRecord[], withAnimation: boolean = false) {
  const totalDineIn = stores.reduce((sum, s) => sum + s.dineIn, 0)
  const totalTakeout = stores.reduce((sum, s) => sum + s.takeaway, 0)
  const totalPickup = stores.reduce((sum, s) => sum + s.selfPickup, 0)

  orderSourceChart.setOption({
    title: {
      text: '订单来源分布',
      left: 'center',
      textStyle: { fontSize: 14, color: getCssVar('--fts-text-primary') }
    },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: '5%', top: 'center' },
    animation: withAnimation,
    animationDuration: withAnimation ? 1500 : 0,
    animationEasing: withAnimation ? 'elasticOut' : undefined,
    series: [{
      name: '订单来源',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: getChartBorderColor(), borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      animation: withAnimation,
      animationDuration: withAnimation ? 1000 : 0,
      animationDelay: (idx: number) => idx * 100,
      data: [
        { value: totalDineIn, name: '堂食', itemStyle: { color: getCssVar('--fts-primary') } },
        { value: totalTakeout, name: '外卖', itemStyle: { color: getCssVar('--fts-success') } },
        { value: totalPickup, name: '自提', itemStyle: { color: getCssVar('--fts-warning') } }
      ]
    }]
  })
}

/**
 * 初始化趋势图
 * 数据来源：调用 liveMonitorApi.getTrend()，失败时由 API 层回退到 Mock
 * @param withAnimation - 是否启用初始动画（首次加载时启用）
 */
async function initTrendChart(withAnimation: boolean = false) {
  // 调用 API 获取趋势数据（API 内部已处理 Mock 回退）
  const trendData = await liveMonitorApi.getTrend(timeDimension.value as TrendDimension)
  const dates = trendData.dates
  const revenueData = trendData.revenue
  const orderData = trendData.orders

  trendChart.setOption({
    title: {
      text: '营收趋势',
      left: 'center',
      textStyle: { fontSize: 14, color: getCssVar('--fts-text-primary') }
    },
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收(元)', '订单量'], top: 30 },
    grid: { left: '3%', right: '4%', bottom: '3%' },
    xAxis: { type: 'category', boundaryGap: false, data: dates },
    yAxis: [
      { type: 'value', name: '营收(元)', position: 'left' },
      { type: 'value', name: '订单量', position: 'right' }
    ],
    animation: withAnimation,
    animationDuration: withAnimation ? 2000 : 0,
    animationEasing: withAnimation ? 'cubicInOut' : undefined,
    series: [
      {
        name: '营收(元)',
        type: 'line',
        data: revenueData,
        smooth: true,
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: `${getCssVar('--fts-primary')}33` },
              { offset: 1, color: `${getCssVar('--fts-primary')}05` }
            ]
          }
        },
        lineStyle: { color: getCssVar('--fts-primary'), width: 2 },
        itemStyle: { color: getCssVar('--fts-primary') },
        animation: withAnimation,
        animationDuration: withAnimation ? 1800 : 0,
        animationDelay: (idx: number) => idx * 30
      },
      {
        name: '订单量',
        type: 'line',
        yAxisIndex: 1,
        data: orderData,
        smooth: true,
        lineStyle: { color: getCssVar('--fts-success'), width: 2, type: 'dashed' },
        itemStyle: { color: getCssVar('--fts-success') },
        animation: withAnimation,
        animationDuration: withAnimation ? 1900 : 0,
        animationDelay: (idx: number) => idx * 40
      }
    ]
  })
}

/* ===== 数据加载与刷新 ===== */

/**
 * 完整数据加载（首次加载或强制刷新）
 * - 初始化图表实例
 * - 设置完整的图表配置和数据
 * - 数据来源：调用 liveMonitorApi，失败时由 API 层回退到 Mock
 */
async function loadData() {
  loading.value = true
  try {
    // 调用 API 获取门店数据（API 内部已处理 Mock 回退）
    storeRecords.value = await liveMonitorApi.getStores()

    // 计算统计卡片数据
    updateStatsCards()

    if (isFirstLoad) {
      // 首次加载：完整初始化图表实例
      await Promise.all([
        revenueChart.init(),
        orderSourceChart.init(),
        trendChart.init()
      ])

      // 设置完整图表配置（包含动画）
      initRevenueChart(storeRecords.value, true)
      initOrderSourceChart(storeRecords.value, true)
      await initTrendChart(true)

      isFirstLoad = false
    } else {
      // 非首次：增量更新图表数据（带平滑过渡动画）
      await updateChartsData()
    }

    // 保存数据快照用于变化检测
    lastDataSnapshot = JSON.parse(JSON.stringify(storeRecords.value))
  } catch (error) {
    console.error('[LiveMonitor] 加载失败:', error)
    ElMessage.error('加载数据失败，请重试')
  } finally {
    loading.value = false
  }
}

/**
 * 增量更新图表数据（不重新初始化图表实例）
 * - 使用 ECharts 的 setOption 合并机制
 * - 自动播放平滑过渡动画
 * - 性能优于完全重建
 */
async function updateChartsData() {
  const stores = storeRecords.value

  // 更新营收对比柱状图（只更新 series 数据）
  if (revenueChart.chartInstance.value) {
    const primaryColor = getCssVar('--fts-primary')
    const errorColor = getCssVar('--fts-error')

    const barData = stores.map(s => ({
      value: s.todayRevenue,
      itemStyle: {
        color: isAbnormalStore(s.todayRevenue) ? errorColor : primaryColor,
        borderRadius: [4, 4, 0, 0]
      }
    }))

    revenueChart.setOption({
      xAxis: { data: stores.map(s => s.storeName) },
      series: [
        { data: barData },
        { data: stores.map(() => revenueAverage.value) }
      ],
      animation: true,  // 启用过渡动画
      animationDuration: 800,  // 动画持续时间
      animationEasing: 'cubicOut'  // 动画缓动函数
    })
  }

  // 更新订单来源饼图
  if (orderSourceChart.chartInstance.value) {
    const totalDineIn = stores.reduce((sum, s) => sum + s.dineIn, 0)
    const totalTakeout = stores.reduce((sum, s) => sum + s.takeaway, 0)
    const totalPickup = stores.reduce((sum, s) => sum + s.selfPickup, 0)

    orderSourceChart.setOption({
      series: [{
        data: [
          { value: totalDineIn, name: '堂食', itemStyle: { color: getCssVar('--fts-primary') } },
          { value: totalTakeout, name: '外卖', itemStyle: { color: getCssVar('--fts-success') } },
          { value: totalPickup, name: '自提', itemStyle: { color: getCssVar('--fts-warning') } }
        ]
      }],
      animation: true,
      animationDuration: 800,
      animationEasing: 'cubicOut'
    })
  }

  // 更新趋势图
  if (trendChart.chartInstance.value) {
    await updateTrendChartData()
  }
}

/**
 * 更新趋势图数据
 * 数据来源：调用 liveMonitorApi.getTrend()，失败时由 API 层回退到 Mock
 */
async function updateTrendChartData() {
  // 调用 API 获取趋势数据（API 内部已处理 Mock 回退）
  const trendData = await liveMonitorApi.getTrend(timeDimension.value as TrendDimension)

  trendChart.setOption({
    xAxis: { data: trendData.dates },
    series: [
      { data: trendData.revenue },
      { data: trendData.orders }
    ],
    animation: true,
    animationDuration: 1000,
    animationEasing: 'cubicInOut'
  })
}

/**
 * 更新统计数据卡片
 */
function updateStatsCards() {
  const activeStores = storeRecords.value.filter(s => s.status === 'active').length
  const totalRevenue = storeRecords.value.reduce((sum, s) => sum + s.todayRevenue, 0)
  const totalOrders = storeRecords.value.reduce((sum, s) => sum + s.orderCount, 0)
  const avgTurnover = storeRecords.value.length > 0
    ? (storeRecords.value.reduce((sum, s) => sum + s.turnoverRate, 0) / storeRecords.value.length).toFixed(1)
    : '0'

  // 4列卡片（参考税务管理 stats-section 规范）
  statsCards.value = [
    { icon: 'Shop', label: '在营门店', value: activeStores, colorType: 'primary', trend: 2 },
    { icon: 'Money', label: '今日总营收', value: `¥${totalRevenue.toLocaleString()}`, colorType: 'success', trend: 12.5 },
    { icon: 'Document', label: '今日总订单', value: totalOrders, colorType: 'warning', trend: 8.3 },
    { icon: 'TrendCharts', label: '平均翻台率', value: avgTurnover, colorType: 'info', trend: 3.2 }
  ]
}

/** 异常门店列表（用于 Tab3） */
const abnormalStores = computed(() => storeRecords.value.filter(s => isAbnormalStore(s.todayRevenue)))

/** 检测数据是否有实质性变化 */
function hasDataChanged(newData: StoreRecord[]): boolean {
  if (!lastDataSnapshot || lastDataSnapshot.length !== newData.length) return true

  return newData.some((store, index) => {
    const oldStore = lastDataSnapshot![index]
    return (
      store.todayRevenue !== oldStore.todayRevenue ||
      store.orderCount !== oldStore.orderCount ||
      store.dineIn !== oldStore.dineIn ||
      store.takeaway !== oldStore.takeaway ||
      store.selfPickup !== oldStore.selfPickup
    )
  })
}

/** 手动刷新（强制完整刷新） */
function handleRefresh() {
  // 手动刷新时重置首次加载标志，强制完整初始化
  isFirstLoad = true
  loadData()
}

/**
 * 智能自动刷新（带变化检测）
 * - 只在数据有实质性变化时才更新UI
 * - 使用增量更新而非完全重建
 * - 数据来源：调用 liveMonitorApi，失败时由 API 层回退到 Mock
 */
async function smartRefresh() {
  try {
    // 调用 API 获取最新门店数据（API 内部已处理 Mock 回退）
    const newData = await liveMonitorApi.getStores()

    // 检测数据是否有实质性变化
    if (!hasDataChanged(newData)) {
      return
    }

    // 更新数据状态
    storeRecords.value = newData
    updateStatsCards()

    // 增量更新图表（带平滑过渡动画）
    await updateChartsData()

    // 更新数据快照
    lastDataSnapshot = JSON.parse(JSON.stringify(newData))
  } catch (error) {
    console.error('[LiveMonitor] 自动刷新失败:', error)
  }
}

/** 切换自动刷新 */
function toggleAutoRefresh() {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
    ElMessage.success(`已开启自动刷新，每 ${refreshInterval.value} 秒更新一次`)
  } else {
    stopAutoRefresh()
    ElMessage.info('已关闭自动刷新')
  }
}

/** 启动自动刷新定时器（使用智能刷新） */
function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    smartRefresh()
  }, refreshInterval.value * 1000)
}

/** 停止自动刷新定时器 */
function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

/** 查询操作 */
function handleSearch() {
  loadData()
}

/** 重置搜索条件 */
function handleReset() {
  dateRange.value = [
    new Date().toISOString().split('T')[0],
    new Date().toISOString().split('T')[0]
  ]
  selectedStore.value = ''
  shiftType.value = ''
  loadData()
}

/** 时间维度切换 */
function handleTimeDimensionChange(dim: '24h' | '7d' | '30d') {
  timeDimension.value = dim
  if (trendChart.chartInstance.value) {
    trendChart.chartInstance.value?.clear()
    initTrendChart()
  }
}

/** 查看门店详情 */
function handleViewDetail(row: StoreRecord) {
  ElMessage.info(`查看 ${row.storeName} 详情`)
}

// 监听时间维度变化重新渲染图表
watch(timeDimension, () => {
  if (trendChart.chartInstance.value) {
    trendChart.chartInstance.value?.clear()
    initTrendChart()
  }
})

onMounted(() => {
  loadData()
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  stopAutoRefresh()
  revenueChart.dispose()
  orderSourceChart.dispose()
  trendChart.dispose()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="实时监控" description="各门店实时运营数据逐项深度查看">
      <template #extra>
        <el-button
          :type="autoRefreshEnabled ? 'primary' : 'default'"
          size="small"
          @click="toggleAutoRefresh"
        >
          <el-icon class="refresh-icon" :class="{ spinning: loading }">
            <RefreshRight />
          </el-icon>
          {{ autoRefreshEnabled ? '自动刷新中' : '已暂停' }}
        </el-button>
      </template>
    </PageHeader>

    <div class="content-wrapper">
    <!-- 统计卡片区域（4列，参考税务管理 stats-section 规范，强制4列不响应式收缩） -->
    <section class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        :trend="stat.trend"
        variant="bordered"
      />
    </section>

    <!-- Tab 切换（参考税务管理 el-tabs border-card 结构） -->
    <section class="tab-section">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- Tab 1: 实时门店监控（搜索+图表+表格） -->
        <el-tab-pane label="实时门店监控" name="realtime">
          <div class="advanced-search-panel">
            <div class="toolbar-row">
              <div class="toolbar-left">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  size="default"
                  style="width: 180px"
                />
                <el-select v-model="selectedStore" placeholder="选择门店" clearable style="width: 160px">
                  <el-option
                    v-for="name in STORE_NAMES"
                    :key="name"
                    :label="name"
                    :value="name"
                  />
                </el-select>
                <el-select v-model="shiftType" placeholder="班次类型" clearable style="width: 140px">
                  <el-option label="早班" value="morning" />
                  <el-option label="中班" value="afternoon" />
                  <el-option label="晚班" value="evening" />
                  <el-option label="全天" value="all" />
                </el-select>
              </div>
              <div class="toolbar-right">
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
              </div>
            </div>
          </div>

          <div class="charts-section">
            <div class="chart-card">
              <div ref="revenueChartRef" class="chart-box" />
            </div>
            <div class="chart-card">
              <div ref="orderSourceChartRef" class="chart-box" />
            </div>
          </div>

          <div class="table-section">
            <DataTable
              :columns="[
                { prop: 'storeName', label: '门店名', minWidth: 130 },
                { prop: 'todayRevenue', label: '今日营收', minWidth: 120, align: 'right', slot: 'revenue' },
                { prop: 'orderCount', label: '订单数', minWidth: 90, align: 'center' },
                { prop: 'dineIn', label: '堂食', minWidth: 75, align: 'center' },
                { prop: 'takeaway', label: '外卖', minWidth: 75, align: 'center' },
                { prop: 'selfPickup', label: '自提', minWidth: 75, align: 'center' },
                { prop: 'turnoverRate', label: '翻台率', minWidth: 90, align: 'center' },
                { prop: 'staffOnDuty', label: '在岗人数', minWidth: 100, align: 'center' },
                { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
                { prop: 'actions', label: '操作', width: 100, fixed: 'right', slot: 'actions' }
              ]"
              :data="storeRecords"
              :loading="loading"
              stripe
            >
              <template #revenue="{ row }">
                <span :class="{ 'revenue-abnormal': isAbnormalStore(row.todayRevenue) }">
                  ¥{{ row.todayRevenue.toLocaleString() }}
                </span>
              </template>
              <template #status="{ row }">
                <StatusTag
                  :status="row.status"
                  :label="row.status === 'active' ? '营业中' : '停业'"
                  size="small"
                />
              </template>
              <template #actions="{ row }">
                <el-button link type="primary" size="small" @click="handleViewDetail(row)">
                  查看详情
                </el-button>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 趋势分析（全宽趋势图） -->
        <el-tab-pane label="趋势分析" name="trend">
          <div class="chart-card chart-full-width">
            <div class="dimension-switcher">
              <el-radio-group
                v-model="timeDimension"
                size="small"
                @change="(val: string) => handleTimeDimensionChange(val as '24h' | '7d' | '30d')"
              >
                <el-radio-button value="24h">24小时</el-radio-button>
                <el-radio-button value="7d">7天</el-radio-button>
                <el-radio-button value="30d">30天</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="trendChartRef" class="chart-box chart-box--trend" />
          </div>
        </el-tab-pane>

        <!-- Tab 3: 异常门店（仅显示营收低于均值50%的门店） -->
        <el-tab-pane :label="`异常门店 (${abnormalStores.length})`" name="abnormal">
          <div class="table-section">
            <DataTable
              :columns="[
                { prop: 'storeName', label: '门店名', minWidth: 130 },
                { prop: 'todayRevenue', label: '今日营收', minWidth: 120, align: 'right', slot: 'revenue' },
                { prop: 'yesterdayRevenue', label: '昨日营收', minWidth: 120, align: 'right' },
                { prop: 'orderCount', label: '订单数', minWidth: 90, align: 'center' },
                { prop: 'turnoverRate', label: '翻台率', minWidth: 90, align: 'center' },
                { prop: 'staffOnDuty', label: '在岗人数', minWidth: 100, align: 'center' },
                { prop: 'status', label: '状态', minWidth: 90, slot: 'status' }
              ]"
              :data="abnormalStores"
              :loading="loading"
              stripe
            >
              <template #revenue="{ row }">
                <span class="revenue-abnormal">
                  ¥{{ row.todayRevenue.toLocaleString() }}
                </span>
              </template>
              <template #status="{ row }">
                <StatusTag
                  :status="row.status"
                  :label="row.status === 'active' ? '营业中' : '停业'"
                  size="small"
                />
              </template>
            </DataTable>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
    </div>
  </div>
</template>

<style scoped>
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
  border-radius: var(--fts-page-radius);
}

/* 内容区无水平内边距，让 stats-section 与 tab-section 铺满 main 宽度 */
.content-wrapper {
  padding: 0;
}

/* Tab 内容区底部内边距（左右由 content-wrapper 统一提供） */
.tab-section {
  padding: 0 0 var(--fts-space-6);
}

/* 图表区域布局（Tab1 内部 2 列） */
.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.chart-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.chart-full-width {
  width: 100%;
}

.dimension-switcher {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--fts-space-2);
}

.chart-box {
  width: 100%;
  height: 280px;
}

.chart-box--trend {
  height: 420px;
}

/* 营收异常高亮样式 */
.revenue-abnormal {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}

/* 刷新按钮动画 */
.refresh-icon {
  transition: transform var(--fts-duration-normal) var(--fts-ease-out);
}

.refresh-icon.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .tab-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }
}
</style>
