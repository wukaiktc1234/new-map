<template>
  <div class="modern-page">
    <PageHeader
      title="菜品成本分析"
      description="分析菜品成本构成、利润率和毛利率分布"
      icon-name="DataAnalysis"
    />

    <section class="stats-section">
      <StatCard icon="Box" label="菜品总数" :value="String(summary.totalDishes)" color-type="primary" variant="bordered" />
      <StatCard icon="TrendCharts" label="平均毛利率" :value="summary.avgMarginRate.toFixed(1) + '%'" color-type="success" variant="bordered" />
      <StatCard icon="CircleCheck" label="正常菜品" :value="String(summary.normalCount)" color-type="warning" variant="bordered" />
      <StatCard icon="Warning" label="低毛利预警" :value="String(summary.warningCount)" color-type="error" variant="bordered" />
      <StatCard icon="CircleClose" label="负利润" :value="String(summary.dangerCount)" color-type="error" variant="bordered" />
    </section>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width: 140px" size="default" @change="handleSearch">
            <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px" size="default" @change="handleSearch">
            <el-option label="在售" :value="1" />
            <el-option label="停售" :value="0" />
            <el-option label="售罄" :value="2" />
          </el-select>
          <el-select v-model="searchForm.statusFilter" placeholder="毛利率状态" clearable style="width: 150px" size="default" @change="handleStatusFilterChange">
            <el-option label="正常(>=35%)" value="normal" />
            <el-option label="低毛利(20-35%)" value="warning" />
            <el-option label="危险(<20%)" value="danger" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="菜品名称/编码" clearable style="width: 180px" size="default" @keyup.enter="fetchData" @clear="handleSearch" />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="fetchData">查询</el-button>
          <el-button size="default" @click="resetSearch">重置</el-button>
          <el-button size="default" v-permission="'product:cost:export'" @click="handleExport">导出报表</el-button>
        </div>
      </div>
    </div>

    <!-- Tab 分离：分析看板（2x3 图表）+ 数据明细（完整表格） -->
    <el-tabs v-model="activeTab" class="analysis-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="分析看板" name="dashboard">
        <!-- 2x3 图表布局：销售TOP20 | 低毛利预警TOP20 / 价格分布 | 毛利率分布 / 分类占比 | 状态分布 -->
        <section class="charts-section">
          <div class="chart-card">
            <div class="chart-title-row">
              <h3 class="chart-title">销售数据 TOP20</h3>
              <span v-if="salesData.length === 0" class="chart-hint">暂无销售数据</span>
            </div>
            <div v-if="salesData.length > 0" ref="salesChartRef" class="chart-box"></div>
            <el-empty v-else description="暂无销售数据，待订单生成后展示" :image-size="80" class="chart-empty" />
          </div>
          <div class="chart-card">
            <div class="chart-title-row">
              <h3 class="chart-title">低毛利预警 TOP20</h3>
              <span v-if="warningChartData.length === 0" class="chart-hint">暂无预警</span>
            </div>
            <div v-if="warningChartData.length > 0" ref="warningChartRef" class="chart-box"></div>
            <el-empty v-else description="暂无预警数据" :image-size="80" class="chart-empty" />
          </div>
          <div class="chart-card">
            <h3 class="chart-title">价格分布</h3>
            <div ref="priceChartRef" class="chart-box"></div>
          </div>
          <div class="chart-card">
            <h3 class="chart-title">毛利率分布</h3>
            <div ref="marginChartRef" class="chart-box"></div>
          </div>
          <div class="chart-card">
            <h3 class="chart-title">分类占比</h3>
            <div ref="categoryChartRef" class="chart-box"></div>
          </div>
          <div class="chart-card">
            <h3 class="chart-title">状态分布</h3>
            <div ref="statusChartRef" class="chart-box"></div>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="数据明细" name="detail">
        <div class="table-panel">
          <DataTable
            :columns="columns"
            :data="filteredData"
            :loading="loading"
            :pagination="paginationConfig"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            @row-click="handleRowSelect"
          >
            <template #productName="{ row }">
              <span class="product-name" :class="{ 'is-stopped': row.productStatus === 'inactive' }">{{ row.productName }}</span>
            </template>
            <template #costPrice="{ row }">
              <span class="cost-text">{{ row.costPrice || '0.00' }}</span>
            </template>
            <template #salePrice="{ row }">
              <span class="price-text">{{ row.salePrice || '0.00' }}</span>
            </template>
            <template #profit="{ row }">
              <span :class="Number(row.profit) < 0 ? 'profit-negative' : 'profit-positive'">{{ row.profit || '0.00' }}</span>
            </template>
            <template #profitRate="{ row }">
              <StatusTag :status="getMarginStatus(row.profitRate)" :label="Number(row.profitRate || 0).toFixed(1) + '%'" size="small" />
            </template>
            <template #weeklySales="{ row }">
              <span class="sales-text">{{ row.weeklySales || 0 }}</span>
            </template>
            <template #turnoverRate="{ row }">
              <span v-if="row.turnoverRate != null" :class="getTurnoverRateClass(row.turnoverRate)">{{ row.turnoverRate.toFixed(1) }}%</span>
              <span v-else class="text-tertiary">-</span>
            </template>
            <template #costStatus="{ row }">
              <StatusTag :status="row.costStatus" :label="getCostStatusLabel(row.costStatus)" size="small" />
            </template>
            <template #productStatus="{ row }">
              <StatusTag :status="getProductStatusTag(row.productStatus)" :label="getProductStatusLabel(row.productStatus)" size="small" />
            </template>
          </DataTable>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProductSalesAnalysis' })

import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { costAnalysisApi } from '@/api/product/cost-analysis'
import { categoryApi } from '@/api/product/category'
import { exportMultiSheetExcel, type ColumnHeader } from '@/utils/export'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { flattenTree } from '@/utils/tree'
import { useECharts } from '@/composables/useECharts'
import { useEChartsTheme } from '@/composables/useEChartsTheme'
import type { CostAnalysisItem, ProductStatus, SalesSummaryItem } from '@/types/product'
import { fenToYuanNumber } from '@/utils/money'

echarts.use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

interface SummaryData {
  totalDishes: number; normalCount: number; warningCount: number; dangerCount: number; avgMarginRate: number; totalCostChange: number
}

const layoutStore = useLayoutStore()
const loading = ref(true)
const tableData = ref<CostAnalysisItem[]>([])
/**
 * 图表数据（全量，不应用工具栏筛选条件）
 * 工具栏筛选只影响表格数据(tableData)，图表始终展示全量数据(chartData)
 * chartData 在 onMounted 时加载一次，查询/重置操作不会重新加载 chartData
 */
const chartData = ref<CostAnalysisItem[]>([])
const summary = ref<SummaryData>({ totalDishes: 0, normalCount: 0, warningCount: 0, dangerCount: 0, avgMarginRate: 0, totalCostChange: 0 })
const categoryOptions = ref<Array<{ value: string; label: string }>>([])
const selectedRow = ref<CostAnalysisItem | null>(null)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const marginChartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 当前激活的 Tab：dashboard=分析看板 / detail=数据明细
// 切换 Tab 时需触发图表 resize（隐藏 Tab 中的图表容器尺寸为 0，切换回来需重新计算）
const activeTab = ref<'dashboard' | 'detail'>('dashboard')

// status: '' 全部 / 1 在售 / 0 停售 / 2 售罄（value 为 number 类型，'' 表示全部）
const searchForm = reactive<{ categoryId: string; statusFilter: string; keyword: string; status: number | '' }>({ categoryId: '', statusFilter: '', keyword: '', status: '' })

const categoryChartRef = ref<HTMLElement>()
const priceChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()
const salesChartRef = ref<HTMLElement>()
const warningChartRef = ref<HTMLElement>()
// 销售数据（来源于 order_items 聚合，订单为空时为空数组）
const salesData = ref<SalesSummaryItem[]>([])
// 低毛利预警图表数据：从 chartData 中筛选 warning/danger 项，按毛利率升序取 TOP 20
const warningChartData = ref<CostAnalysisItem[]>([])

const { chartInstance: categoryChart, setOption: setCategoryOption } = useECharts(categoryChartRef, { autoInit: true })
const { chartInstance: priceChart, setOption: setPriceOption } = useECharts(priceChartRef, { autoInit: true })
const { chartInstance: statusChart, setOption: setStatusOption } = useECharts(statusChartRef, { autoInit: true })
const { chartInstance: salesChart, setOption: setSalesOption } = useECharts(salesChartRef, { autoInit: true })
const { chartInstance: warningChart, setOption: setWarningOption } = useECharts(warningChartRef, { autoInit: true })

// ECharts 主题适配：根据深色/浅色主题动态获取文字、边框、tooltip 背景色等
// ECharts 配置项不支持 CSS 变量，必须使用具体颜色值（已在 useEChartsTheme 中统一管理）
const {
  isDark: isDarkTheme,
  textColor: chartTextColor,
  textSecondaryColor: chartTextSecondaryColor,
  tooltipOption: chartTooltipOption,
  legendOption: chartLegendOption,
  xAxisBaseOption: chartXAxisBaseOption,
  yAxisBaseOption: chartYAxisBaseOption,
} = useEChartsTheme()

// 饼图分隔线颜色：深色模式与卡片背景一致，浅色模式使用白色
const pieBorderColor = computed(() => isDarkTheme.value ? '#141414' : '#ffffff')
// 主题色板（与 --fts-primary/success/warning/error 视觉对应，深色模式提亮）
const chartPalette = computed(() => isDarkTheme.value
  ? ['#7aa8d9', '#6cc76c', '#e4b04a', '#d9645b', '#8babcc', '#a5d6a7', '#ffcc80']
  : ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#67C23A', '#E6A23C'])
// 毛利率分布图状态色：危险/警告/正常/优质（与 --fts-error/warning/success/primary 视觉对应）
const marginChartColors = computed(() => isDarkTheme.value
  ? { danger: '#d9645b', warning: '#e4b04a', success: '#6cc76c', primary: '#7aa8d9' }
  : { danger: '#ee6666', warning: '#fac858', success: '#91cc75', primary: '#5470c6' })

function updateCategoryChart(): void {
  const categoryCount: Record<string, number> = {}
  chartData.value.forEach((item) => {
    const name = item.categoryName || '未分类'
    categoryCount[name] = (categoryCount[name] || 0) + 1
  })
  const data = Object.entries(categoryCount).map(([name, value]) => ({ name, value }))
  setCategoryOption({
    tooltip: { ...chartTooltipOption.value, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { ...chartLegendOption.value, orient: 'vertical', left: 'left', top: 'middle' },
    series: [{ name: '菜品分类', type: 'pie', radius: ['40%', '70%'], center: ['60%', '50%'], avoidLabelOverlap: false, itemStyle: { borderRadius: 8, borderColor: pieBorderColor.value, borderWidth: 2 }, label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, color: chartTextColor.value }, emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: chartTextColor.value } }, data }],
    color: chartPalette.value,
  })
}

function updatePriceChart(): void {
  const ranges = [{ label: '0-20元', min: 0, max: 20 }, { label: '21-40元', min: 21, max: 40 }, { label: '41-60元', min: 41, max: 60 }, { label: '61-80元', min: 61, max: 80 }, { label: '80元以上', min: 81, max: Infinity }]
  const counts = ranges.map(range => chartData.value.filter(item => { const price = Number(item.salePrice) || 0; return price >= range.min && price <= range.max }).length)
  setPriceOption({
    tooltip: { ...chartTooltipOption.value, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c} 道菜' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { ...chartXAxisBaseOption.value, type: 'category', data: ranges.map(r => r.label), axisLabel: { ...chartXAxisBaseOption.value.axisLabel, fontSize: 11 } },
    yAxis: { ...chartYAxisBaseOption.value, type: 'value', name: '菜品数量', nameTextStyle: { color: chartTextSecondaryColor.value, fontSize: 11 }, axisLabel: { ...chartYAxisBaseOption.value.axisLabel, fontSize: 11 } },
    series: [{ name: '菜品数量', type: 'bar', data: counts, barWidth: '50%', itemStyle: { borderRadius: [4, 4, 0, 0], color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: chartPalette.value[0] }, { offset: 1, color: chartPalette.value[1] }] } }, emphasis: { itemStyle: { color: chartPalette.value[2] } } }],
  })
}

function updateStatusChart(): void {
  const activeCount = chartData.value.filter(f => f.profitRate >= 35).length
  const warningCount = chartData.value.filter(f => f.profitRate >= 20 && f.profitRate < 35).length
  const dangerCount = chartData.value.filter(f => f.profitRate < 20).length
  setStatusOption({
    tooltip: { ...chartTooltipOption.value, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { ...chartLegendOption.value, bottom: '0%', left: 'center' },
    series: [{ name: '成本状态', type: 'pie', radius: ['45%', '65%'], center: ['50%', '45%'], avoidLabelOverlap: true, itemStyle: { borderRadius: 8, borderColor: pieBorderColor.value, borderWidth: 2 }, label: { show: true, formatter: '{b}\n{c}道', fontSize: 11, color: chartTextColor.value }, emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: chartTextColor.value } }, data: [{ value: activeCount, name: '正常(>=35%)' }, { value: warningCount, name: '低毛利(20-35%)' }, { value: dangerCount, name: '危险(<20%)' }] }],
    color: [chartPalette.value[5], chartPalette.value[6], chartPalette.value[3]],
  })
}

/**
 * 渲染销售数据图表（横向柱状图，按销售额降序展示 TOP N）
 * - 数据来源：order_items 表聚合，金额单位为分，需转为元展示
 * - 销售数据为空时不渲染图表（模板中显示 el-empty 占位）
 * - 配色按产品类型区分：菜品用主色、套餐用辅助色
 */
function updateSalesChart(): void {
  if (salesData.value.length === 0) return
  // 销售额由分转元，使用统一金额工具避免浮点精度问题
  const items = salesData.value.map(item => ({
    name: item.productName,
    amount: fenToYuanNumber(item.salesAmount),
    count: item.salesCount,
    productType: item.productType,
  }))
  // ECharts 横向柱状图：yAxis 为类目轴，从下往上；为保证 TOP1 显示在顶部，需反转数组
  const reversed = [...items].reverse()
  const colors = chartPalette.value
  setSalesOption({
    tooltip: {
      ...chartTooltipOption.value,
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      // 自定义 formatter：同时展示销售额和销量
      formatter: (params: Array<{ dataIndex: number }>) => {
        const idx = params[0]?.dataIndex
        if (idx == null || !reversed[idx]) return ''
        const it = reversed[idx]
        return `${it.name}<br/>销售额：￥${it.amount.toFixed(2)}<br/>销量：${it.count} 份`
      },
    },
    legend: { ...chartLegendOption.value, bottom: '0%', left: 'center' },
    grid: { left: '3%', right: '6%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: {
      ...chartXAxisBaseOption.value,
      type: 'value',
      name: '销售额(元)',
      nameTextStyle: { color: chartTextSecondaryColor.value, fontSize: 11 },
      axisLabel: { ...chartXAxisBaseOption.value.axisLabel, fontSize: 11 },
    },
    yAxis: {
      ...chartYAxisBaseOption.value,
      type: 'category',
      data: reversed.map(it => it.name),
      // 名称过长时截断，避免撑爆图表
      axisLabel: {
        ...chartYAxisBaseOption.value.axisLabel,
        fontSize: 11,
        width: 100,
        overflow: 'truncate',
        formatter: (val: string) => val.length > 8 ? val.slice(0, 8) + '…' : val,
      },
    },
    series: [{
      name: '销售额',
      type: 'bar',
      data: reversed.map(it => ({
        value: it.amount,
        // 菜品用主色，套餐用 success 色，便于区分
        itemStyle: { color: it.productType === 'COMBO' ? colors[1] : colors[0], borderRadius: [0, 4, 4, 0] },
      })),
      barWidth: '60%',
    }],
  })
}

/**
 * 渲染低毛利预警图表（横向柱状图，按毛利率升序展示 TOP 20）
 * - 数据来源：chartData 中 costStatus 为 warning/danger 的项
 * - 横向柱状图：yAxis 为类目轴（菜品名），xAxis 为毛利率(%)
 * - 颜色：danger 用 error 色，warning 用 warning 色，便于区分严重程度
 */
function updateWarningChart(): void {
  if (warningChartData.value.length === 0) return
  const items = warningChartData.value.map(item => ({
    name: item.productName,
    rate: Number(item.profitRate || 0),
    costStatus: item.costStatus,
  }))
  // 毛利率升序（最低的在顶部）：反转后 yAxis 从下往上绘制
  const reversed = [...items].reverse()
  const colors = marginChartColors.value
  setWarningOption({
    tooltip: {
      ...chartTooltipOption.value,
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ dataIndex: number }>) => {
        const idx = params[0]?.dataIndex
        if (idx == null || !reversed[idx]) return ''
        const it = reversed[idx]
        return `${it.name}<br/>毛利率：${it.rate.toFixed(2)}%<br/>状态：${it.costStatus === 'danger' ? '危险(<20%)' : '低毛利(20-35%)'}`
      },
    },
    legend: { ...chartLegendOption.value, bottom: '0%', left: 'center' },
    grid: { left: '3%', right: '6%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: {
      ...chartXAxisBaseOption.value,
      type: 'value',
      name: '毛利率(%)',
      nameTextStyle: { color: chartTextSecondaryColor.value, fontSize: 11 },
      axisLabel: { ...chartXAxisBaseOption.value.axisLabel, fontSize: 11, formatter: '{value}%' },
    },
    yAxis: {
      ...chartYAxisBaseOption.value,
      type: 'category',
      data: reversed.map(it => it.name),
      axisLabel: {
        ...chartYAxisBaseOption.value.axisLabel,
        fontSize: 11,
        width: 100,
        overflow: 'truncate',
        formatter: (val: string) => val.length > 8 ? val.slice(0, 8) + '…' : val,
      },
    },
    series: [{
      name: '毛利率',
      type: 'bar',
      data: reversed.map(it => ({
        value: it.rate,
        // 危险项用 error 色，警告项用 warning 色
        itemStyle: { color: it.costStatus === 'danger' ? colors.danger : colors.warning, borderRadius: [0, 4, 4, 0] },
      })),
      barWidth: '60%',
    }],
  })
}

async function updateCharts(): Promise<void> {
  await nextTick()
  // 等待图表实例初始化完成（处理 ECharts 动态加载延迟导致的首次渲染空白）
  let retries = 0
  while ((!categoryChart.value || !priceChart.value || !statusChart.value) && retries < 30) {
    await new Promise(resolve => setTimeout(resolve, 100))
    retries++
  }
  updateCategoryChart()
  updatePriceChart()
  updateStatusChart()
  // 销售数据图表单独等待其实例（数据为空时不渲染，跳过实例检查）
  updateSalesChart()
  // 低毛利预警图表（数据为空时不渲染）
  updateWarningChart()
}

const columns = computed<DataTableColumn[]>(() => [
  { prop: 'productName', label: '菜品名称', minWidth: 160, fixed: 'left' },
  { prop: 'categoryName', label: '分类', minWidth: 110 },
  { prop: 'costPrice', label: '成本(元)', minWidth: 105, align: 'right' },
  { prop: 'salePrice', label: '售价(元)', minWidth: 105, align: 'right' },
  { prop: 'profit', label: '毛利(元)', minWidth: 105, align: 'right' },
  { prop: 'profitRate', label: '毛利率(%)', minWidth: 105, align: 'center' },
  { prop: 'weeklySales', label: '周销售量', minWidth: 95, align: 'right', slot: 'weeklySales' },
  { prop: 'turnoverRate', label: '周转率', minWidth: 95, align: 'right', slot: 'turnoverRate' },
  { prop: 'costStatus', label: '成本状态', minWidth: 95, align: 'center', slot: 'costStatus' },
  { prop: 'productStatus', label: '售卖状态', minWidth: 95, align: 'center', slot: 'productStatus' },
])

const paginationConfig = computed(() => ({ total: total.value, current: currentPage.value, pageSize }))

const filteredData = computed(() => {
  // keyword 已改为后端过滤，前端仅保留按毛利率状态（costStatus）的过滤
  let data = tableData.value
  if (searchForm.statusFilter) data = data.filter(item => item.costStatus === searchForm.statusFilter)
  return data
})

onMounted(async () => {
  // 图表全量数据与表格筛选数据并行加载
  await Promise.all([fetchChartData(), fetchData()])
  fetchCategories(); fetchSalesData(); initChart(); window.addEventListener('resize', handleResize)
})
onUnmounted(() => { window.removeEventListener('resize', handleResize); chartInstance?.dispose() })

// 图表数据变化时刷新图表（与 tableData 解耦，筛选不影响图表）
watch(() => chartData.value.length, () => {
  // 同步更新低毛利预警图表数据（TOP 20，按毛利率升序）
  warningChartData.value = chartData.value
    .filter(item => item.costStatus === 'warning' || item.costStatus === 'danger')
    .sort((a, b) => a.profitRate - b.profitRate)
    .slice(0, 20)
  updateCharts()
})

// 销售数据变化时单独刷新销售图表（与 tableData 解耦）
watch(() => salesData.value.length, () => updateSalesChart())

// 主题切换时重新渲染所有图表，确保文字/配色适配深色模式
watch(isDarkTheme, () => {
  updateCategoryChart()
  updatePriceChart()
  updateStatusChart()
  updateSalesChart()
  updateWarningChart()
  updateChart()
})

async function fetchData(): Promise<void> {
  loading.value = true
  try {
    // 表格数据应用筛选条件（categoryId/status/keyword）
    const res = await costAnalysisApi.getReport({ categoryId: searchForm.categoryId ? Number(searchForm.categoryId) : undefined, keyword: searchForm.keyword || undefined, status: searchForm.status === '' ? undefined : Number(searchForm.status), page: currentPage.value, size: pageSize })
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error: unknown) { ElMessage.error(error instanceof Error ? error.message : '获取数据失败') }
  finally { loading.value = false }
}

/**
 * 加载图表全量数据（不应用工具栏筛选条件）
 * 图表、统计卡片、预警列表始终展示全量数据，与表格筛选解耦
 * 失败时静默降级，不影响表格功能
 */
async function fetchChartData(): Promise<void> {
  try {
    const res = await costAnalysisApi.getReport({ page: 1, size: 10000 })
    chartData.value = res.records || []
    // 统计卡片基于全量数据计算
    summary.value.totalDishes = chartData.value.length
    summary.value.normalCount = chartData.value.filter(i => i.costStatus === 'normal').length
    summary.value.warningCount = chartData.value.filter(i => i.costStatus === 'warning').length
    summary.value.dangerCount = chartData.value.filter(i => i.costStatus === 'danger').length
    const rates = chartData.value.map(i => i.profitRate).filter(r => r != null)
    summary.value.avgMarginRate = rates.length ? rates.reduce((a, b) => a + b, 0) / rates.length : 0
    await nextTick(); updateChart(); await updateCharts()
  } catch {
    // 图表数据加载失败时静默处理，不影响表格主功能
    chartData.value = []
  }
}

async function fetchCategories(): Promise<void> {
  try { const categories = await categoryApi.getTree(); categoryOptions.value = flattenTree(categories as unknown as Record<string, unknown>[], 'categoryId', 'categoryName') } catch { categoryOptions.value = [] }
}

/**
 * 获取销售数据汇总（TOP20 销售额菜品/套餐）
 * 数据来源：后端 /sales-summary 接口，聚合自 order_items 表
 * 订单数据为空时返回空数组，前端显示"暂无销售数据"占位
 * 失败时不弹错误提示，仅静默降级（避免影响主成本分析功能）
 */
async function fetchSalesData(): Promise<void> {
  try {
    const res = await costAnalysisApi.getSalesSummary(20)
    salesData.value = Array.isArray(res) ? res : []
    await nextTick()
    updateSalesChart()
  } catch {
    salesData.value = []
  }
}

function resetSearch(): void { searchForm.categoryId = ''; searchForm.statusFilter = ''; searchForm.keyword = ''; searchForm.status = ''; currentPage.value = 1; fetchData() }
// 分类/状态下拉变化时触发查询（重置到第 1 页，仅影响表格数据，不影响图表）
function handleSearch(): void { currentPage.value = 1; fetchData() }
/**
 * Tab 切换处理：切到"分析看板"时触发所有图表 resize
 * 隐藏 Tab 中的图表容器尺寸为 0，切回来需重新计算尺寸才能正常显示
 */
function handleTabChange(tabName: string | number): void {
  if (tabName === 'dashboard') {
    nextTick(() => {
      categoryChart.value?.resize()
      priceChart.value?.resize()
      statusChart.value?.resize()
      salesChart.value?.resize()
      warningChart.value?.resize()
      chartInstance?.resize()
    })
  }
}
/**
 * 周转率样式：高周转(>200%)用 success 色，中周转(50-200%)用 primary 色，低周转(<50%)用 warning 色
 */
function getTurnoverRateClass(rate: number | null): string {
  if (rate == null) return 'text-tertiary'
  if (rate >= 200) return 'turnover-high'
  if (rate >= 50) return 'turnover-normal'
  return 'turnover-low'
}
/**
 * 毛利率状态筛选变化处理
 * statusFilter 是前端过滤（按 costStatus 过滤），无需调用 API
 * filteredData 计算属性会自动重新计算，表格数据自动更新
 */
function handleStatusFilterChange(): void { currentPage.value = 1 }
function getMarginStatus(rate: number): string { if (rate >= 60) return 'success'; if (rate >= 35) return 'active'; if (rate >= 20) return 'warning'; return 'error' }
function getCostStatusLabel(status: string): string { const map: Record<string, string> = { normal: '正常', warning: '低毛利', danger: '危险' }; return map[status] || status }
// 售卖状态 → StatusTag 颜色映射：在售=success绿/停售=inactive灰/售罄=warning橙
function getProductStatusTag(status: ProductStatus): string {
  const map: Record<ProductStatus, string> = { active: 'success', inactive: 'inactive', soldout: 'warning' }
  return map[status] || 'info'
}
// 售卖状态 → 中文标签
function getProductStatusLabel(status: ProductStatus): string {
  const map: Record<ProductStatus, string> = { active: '在售', inactive: '停售', soldout: '售罄' }
  return map[status] || status
}
function handleRowSelect(row: CostAnalysisItem): void { selectedRow.value = row }

async function handleExport(): Promise<void> {
  if (tableData.value.length === 0) { ElMessage.warning('当前没有可导出的数据'); return }
  try {
    loading.value = true
    const res = await costAnalysisApi.getReport({ categoryId: searchForm.categoryId ? Number(searchForm.categoryId) : undefined, keyword: searchForm.keyword || undefined, status: searchForm.status === '' ? undefined : Number(searchForm.status), page: 1, size: 10000 })
    const allRecords = res.records || []
    if (allRecords.length === 0) { ElMessage.warning('没有可导出的数据'); return }
    const costStatusMap: Record<string, string> = { normal: '正常', warning: '低毛利', danger: '危险' }
    // 详细数据：包含每个菜品的成本、售价、毛利率、销量、周转率等明细
    const detailData = allRecords.map(item => ({
      productName: item.productName,
      productCode: item.productCode,
      categoryName: item.categoryName,
      costPrice: item.costPrice,
      salePrice: item.salePrice,
      profit: item.profit,
      profitRate: Number(item.profitRate || 0).toFixed(2) + '%',
      weeklySales: String(item.weeklySales ?? 0),
      turnoverRate: item.turnoverRate != null ? item.turnoverRate.toFixed(1) + '%' : '-',
      costStatus: costStatusMap[item.costStatus] || item.costStatus,
    }))
    const warningRecords = allRecords.filter(item => item.costStatus === 'warning' || item.costStatus === 'danger')
    const warningData = warningRecords.map((item, index) => ({ rank: index + 1, productName: item.productName, categoryName: item.categoryName, profitRate: Number(item.profitRate || 0).toFixed(2) + '%', salePrice: item.salePrice, costPrice: item.costPrice, profit: item.profit, weeklySales: String(item.weeklySales ?? 0), turnoverRate: item.turnoverRate != null ? item.turnoverRate.toFixed(1) + '%' : '-', costStatus: costStatusMap[item.costStatus] || item.costStatus, riskLevel: item.profitRate < 20 ? '高风险' : item.profitRate < 35 ? '中风险' : '低风险' }))
    const summaryData = [{ metric: '菜品总数', value: String(summary.value.totalDishes), unit: '个' }, { metric: '平均毛利率', value: summary.value.avgMarginRate.toFixed(2), unit: '%' }, { metric: '正常菜品(>=35%)', value: String(summary.value.normalCount), unit: '个' }, { metric: '低毛利预警(20-35%)', value: String(summary.value.warningCount), unit: '个' }, { metric: '危险(<20%)', value: String(summary.value.dangerCount), unit: '个' }, { metric: '导出时间', value: new Date().toLocaleString('zh-CN'), unit: '' }]
    const sheets = [
      { data: summaryData as unknown as Record<string, unknown>[], headers: [{ key: 'metric', label: '指标', width: 25 }, { key: 'value', label: '数值', width: 18 }, { key: 'unit', label: '单位', width: 10 }] as ColumnHeader[], sheetName: '统计汇总' },
      { data: detailData as unknown as Record<string, unknown>[], headers: [{ key: 'productName', label: '菜品名称', width: 18 }, { key: 'productCode', label: '编码', width: 12 }, { key: 'categoryName', label: '分类', width: 12 }, { key: 'costPrice', label: '成本(元)', width: 14 }, { key: 'salePrice', label: '售价(元)', width: 13 }, { key: 'profit', label: '毛利(元)', width: 13 }, { key: 'profitRate', label: '毛利率', width: 13 }, { key: 'weeklySales', label: '周销售量', width: 12 }, { key: 'turnoverRate', label: '周转率', width: 12 }, { key: 'costStatus', label: '成本状态', width: 10 }] as ColumnHeader[], sheetName: '成本明细' },
      { data: warningData as unknown as Record<string, unknown>[], headers: [{ key: 'rank', label: '排名', width: 8 }, { key: 'productName', label: '菜品名称', width: 18 }, { key: 'categoryName', label: '分类', width: 12 }, { key: 'profitRate', label: '毛利率', width: 12 }, { key: 'salePrice', label: '售价(元)', width: 13 }, { key: 'costPrice', label: '成本(元)', width: 14 }, { key: 'profit', label: '毛利(元)', width: 13 }, { key: 'weeklySales', label: '周销售量', width: 12 }, { key: 'turnoverRate', label: '周转率', width: 12 }, { key: 'costStatus', label: '成本状态', width: 10 }, { key: 'riskLevel', label: '风险等级', width: 10 }] as ColumnHeader[], sheetName: '预警列表' },
    ]
    exportMultiSheetExcel(sheets, '成本分析报表')
    ElMessage.success(`成功导出 ${allRecords.length} 条成本分析数据`)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '导出失败') }
  finally { loading.value = false }
}

function initChart(): void { if (!marginChartRef.value) return; chartInstance = echarts.init(marginChartRef.value); updateChart() }

function updateChart(): void {
  if (!chartInstance) return
  // 注意：ECharts 不支持 CSS 变量字符串（如 'var(--fts-error)'）作为 color，
  // 必须使用具体颜色值，这里通过主题适配 composable 动态获取
  const colors = marginChartColors.value
  const ranges = [
    { label: '<0%', count: 0, color: colors.danger }, { label: '0-20%', count: 0, color: colors.danger },
    { label: '20-35%', count: 0, color: colors.warning }, { label: '35-60%', count: 0, color: colors.success }, { label: '>60%', count: 0, color: colors.primary },
  ]
  // 图表使用全量数据 chartData，不受工具栏筛选影响
  for (const item of chartData.value) {
    const rate = Number(item.profitRate || 0)
    if (rate < 0) ranges[0].count++; else if (rate < 20) ranges[1].count++; else if (rate < 35) ranges[2].count++; else if (rate < 60) ranges[3].count++; else ranges[4].count++
  }
  const option = {
    tooltip: { ...chartTooltipOption.value, trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}: {c} 个菜品' },
    grid: { left: '12%', right: '8%', bottom: '18%', top: '18%' },
    xAxis: { ...chartXAxisBaseOption.value, type: 'category', data: ranges.map(r => r.label), axisLabel: { ...chartXAxisBaseOption.value.axisLabel, fontSize: 11 } },
    yAxis: { ...chartYAxisBaseOption.value, type: 'value', name: '菜品数量', nameTextStyle: { color: chartTextSecondaryColor.value, fontSize: 11 }, axisLabel: { ...chartYAxisBaseOption.value.axisLabel, fontSize: 11 } },
    series: [{ type: 'bar', data: ranges.map(r => ({ value: r.count, itemStyle: { color: r.color, borderRadius: [4, 4, 0, 0] } })), barWidth: '50%' }],
  }
  chartInstance.setOption(option, true)
}

function handleResize(): void { chartInstance?.resize() }
</script>

<style scoped lang="scss">
.stats-section { grid-template-columns: repeat(5, 1fr);
  @media (max-width: 1400px) { grid-template-columns: repeat(3, 1fr); }
}

/* Tab 容器：与上方工具栏保持间距 */
.analysis-tabs {
  margin-top: var(--fts-space-4);
  :deep(.el-tabs__header) { margin-bottom: var(--fts-space-4); }
}

/* 数据明细 Tab 中的表格容器 */
.table-panel {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  background: var(--fts-bg-card);
  overflow: hidden;
  :deep(.el-table__header-wrapper th) { border-bottom: 2px solid var(--fts-border-primary); }
  :deep(.el-table__row td) { border-bottom: 1px solid var(--fts-border-primary); }
}

.profit-negative { color: var(--fts-error); font-weight: var(--fts-font-weight-semibold); }
.profit-positive { color: var(--fts-success); font-weight: var(--fts-font-weight-medium); }
.product-name { font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary);
  /* 停售菜品视觉标识：使用更淡的文字色，与 StatusTag 配合区分 */
  &.is-stopped { color: var(--fts-text-tertiary); font-style: italic; }
}
.cost-text { color: var(--fts-text-secondary); font-variant-numeric: tabular-nums; }
.price-text { font-weight: var(--fts-font-weight-semibold); color: var(--fts-success); font-variant-numeric: tabular-nums; }
.sales-text { color: var(--fts-text-primary); font-variant-numeric: tabular-nums; }

/* 周转率颜色：高周转=success / 中周转=primary / 低周转=warning / 不适用=tertiary */
.turnover-high { color: var(--fts-success); font-weight: var(--fts-font-weight-semibold); font-variant-numeric: tabular-nums; }
.turnover-normal { color: var(--fts-primary); font-weight: var(--fts-font-weight-medium); font-variant-numeric: tabular-nums; }
.turnover-low { color: var(--fts-warning); font-variant-numeric: tabular-nums; }
.text-tertiary { color: var(--fts-text-tertiary); }

/* 图表区：2x3 网格布局（销售TOP20、低毛利预警TOP20、价格分布、毛利率分布、分类占比、状态分布） */
.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: auto auto auto;
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}
.chart-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
  display: flex;
  flex-direction: column;
}
.chart-title { margin: 0 0 var(--fts-space-3); font-size: var(--fts-font-size-base); font-weight: var(--fts-font-weight-semibold); color: var(--fts-text-primary); }
.chart-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 var(--fts-space-3);
  .chart-title { margin: 0; }
}
.chart-hint {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  padding: 2px var(--fts-space-2);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}
.chart-box { height: 300px; min-height: 280px; width: 100%; }
.chart-empty {
  height: 300px;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 中等屏幕保持 2 列；小屏幕降级为 1 列，保证可读性 */
@media (max-width: 768px) { .charts-section { grid-template-columns: 1fr; } }
</style>