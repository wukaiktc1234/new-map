<script setup lang="ts">
/**
 * 采购报表页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】采购数据统计分析与报表展示
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 功能：
 * - 时间范围筛选（今日/本周/本月/本季/本年/自定义）
 * - 统计卡片展示核心指标
 * - ECharts 图表展示多维度分析
 * - 多维度明细数据表格
 * - 报表导出
 */
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, DatePicker } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useECharts } from '@/composables/useECharts'
import {
  getChartPalette,
  buildBaseOption,
  linearGradient,
  withBarRadius,
  formatWan,
} from '@/composables/useChartPalette'
import { purchaseReportApi, purchaseReportConverter } from '@/api/purchase'
import type {
  PurchaseReportSummary,
  PurchaseReportMonthlyItem,
  PurchaseReportSupplierItem,
  PurchaseReportCategoryItem,
  PurchaseReportQueryForm,
} from '@/types/purchase-report'

// ==================== 类型定义 ====================

/** 时间范围类型 */
type TimeRangeType = 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'

/** 报表标签页类型 */
type ReportTabType = 'amount' | 'supplier' | 'category'

// ==================== 响应式数据 ====================

const loading = ref(false)
const exportLoading = ref(false)

/** 时间范围类型 */
const timeRangeType = ref<TimeRangeType>('month')

/** 自定义日期范围 */
const customDateRange = ref<[string, string] | null>(null)

/** 当前激活的报表标签页 */
const activeTab = ref<ReportTabType>('amount')

/** 汇总数据 */
const summary = reactive<PurchaseReportSummary>({
  totalPurchaseAmount: 0,
  totalOrderCount: 0,
  totalSettledAmount: 0,
  totalUnsettledAmount: 0,
  avgOrderAmount: 0,
  supplierCount: 0,
  pendingOrders: 0,
  onTimeDeliveryRate: 0,
  qualifiedRate: 0,
})

/** 月度趋势数据 */
const monthlyData = ref<PurchaseReportMonthlyItem[]>([])

/** 供应商维度数据 */
const supplierData = ref<PurchaseReportSupplierItem[]>([])

/** 分类维度数据 */
const categoryData = ref<PurchaseReportCategoryItem[]>([])

// ==================== 图表相关 ====================

const trendChartRef = ref<HTMLElement>()
const supplierPieRef = ref<HTMLElement>()
const categoryBarRef = ref<HTMLElement>()
const orderTrendRef = ref<HTMLElement>()

const trendChart = useECharts(trendChartRef)
const supplierPieChart = useECharts(supplierPieRef)
const categoryBarChart = useECharts(categoryBarRef)
const orderTrendChart = useECharts(orderTrendRef)


/** 初始化采购趋势图（折线+柱状组合图） */
function initTrendChart(): void {
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const months = monthlyData.value.map(m => m.month)
  const amountSeries = monthlyData.value.map(m => m.purchaseAmount)
  const countSeries = monthlyData.value.map(m => m.orderCount)

  trendChart.setOption({
    ...base,
    legend: {
      ...(base.legend as Record<string, unknown>),
      data: ['采购金额', '订单数量'],
      bottom: 0,
    },
    grid: { ...(base.grid as Record<string, unknown>), left: 64, right: 56, top: 36 },
    xAxis: {
      ...(base.xAxis as Record<string, unknown>),
      data: months,
    },
    yAxis: [
      {
        ...(base.yAxis as Record<string, unknown>),
        type: 'value',
        name: '金额(元)',
        axisLabel: {
          ...(base.yAxis as Record<string, unknown>).axisLabel,
          formatter: (v: number) => formatWan(v, '元'),
        },
      },
      {
        ...(base.yAxis as Record<string, unknown>),
        type: 'value',
        name: '订单数',
        position: 'right',
      },
    ],
    series: [
      {
        name: '采购金额',
        type: 'line',
        data: amountSeries,
        smooth: true,
        showSymbol: false,
        itemStyle: { color: palette.semantic.primary },
        lineStyle: { width: 2.5 },
        areaStyle: {
          color: linearGradient(palette.semantic.primary, 0.35, 0.02),
        },
      },
      {
        name: '订单数量',
        type: 'bar',
        yAxisIndex: 1,
        data: withBarRadius(countSeries.map(v => ({ value: v })) as any, [4, 4, 0, 0]),
        itemStyle: { color: palette.semantic.success, borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 24,
        barGap: '-30%',
      },
    ],
  })
}

/** 初始化供应商采购占比环形图 */
function initSupplierPieChart(): void {
  const palette = getChartPalette()
  const topSuppliers = supplierData.value.slice(0, 8)

  supplierPieChart.setOption({
    ...buildBaseOption(palette),
    tooltip: {
      ...(buildBaseOption(palette).tooltip as Record<string, unknown>),
      trigger: 'item',
      formatter(param: { name: string; value: number; percent: number }) {
        return `${param.name}<br/>采购金额: ¥${purchaseReportConverter.formatYuan(param.value)}<br/>占比: ${param.percent}%`
      },
    },
    legend: {
      ...(buildBaseOption(palette).legend as Record<string, unknown>),
      orient: 'vertical',
      right: 8,
      top: 'middle',
    },
    grid: undefined,
    xAxis: undefined,
    yAxis: undefined,
    series: [
      {
        type: 'pie',
        radius: ['40%', '68%'],
        center: ['32%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: palette.cardBg,
          borderWidth: 2,
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' },
          scale: true,
          scaleSize: 6,
        },
        data: topSuppliers.map((s, i) => ({
          name: s.supplierName,
          value: s.totalAmount,
          itemStyle: { color: palette.primary[i % palette.primary.length] },
        })),
      },
    ],
  })
}

/** 初始化分类采购排行柱状图（横向） */
function initCategoryBarChart(): void {
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const topCategories = categoryData.value.slice(0, 10).reverse()

  categoryBarChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>),
      axisPointer: { type: 'shadow' },
      formatter(params: { name: string; value: number }[]) {
        const item = params[0]
        return `${item.name}<br/>采购金额: ¥${purchaseReportConverter.formatYuan(item.value)}`
      },
    },
    grid: { ...(base.grid as Record<string, unknown>), left: 128, right: 24, top: 12, bottom: 12 },
    legend: undefined,
    xAxis: {
      ...(base.yAxis as Record<string, unknown>),
      type: 'value',
      name: '采购金额(元)',
      axisLabel: {
        ...(base.yAxis as Record<string, unknown>).axisLabel,
        formatter: (v: number) => formatWan(v),
      },
    },
    yAxis: {
      type: 'category',
      data: topCategories.map(c => c.categoryName),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        fontSize: 12,
        color: palette.textSecondary,
        width: 116,
        overflow: 'truncate',
      },
    },
    series: [
      {
        type: 'bar',
        data: topCategories.map(c => c.totalAmount),
        itemStyle: {
          color: linearGradient(palette.semantic.primary, 0.9, 0.4),
          borderRadius: [0, 4, 4, 0],
        },
        label: {
          show: true,
          position: 'right',
          color: palette.textSecondary,
          fontSize: 12,
          formatter: (p: { value: number }) => formatWan(p.value),
        },
        barMaxWidth: 18,
      },
    ],
  })
}

/** 初始化订单趋势图（柱状图） */
function initOrderTrendChart(): void {
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const months = monthlyData.value.map(m => m.month)

  orderTrendChart.setOption({
    ...base,
    legend: {
      ...(base.legend as Record<string, unknown>),
      data: ['订单数', '供应商数'],
      bottom: 0,
    },
    grid: { ...(base.grid as Record<string, unknown>), left: 56, right: 56, top: 36 },
    xAxis: {
      ...(base.xAxis as Record<string, unknown>),
      data: months,
    },
    yAxis: [
      {
        ...(base.yAxis as Record<string, unknown>),
        type: 'value',
        name: '订单数',
      },
      {
        ...(base.yAxis as Record<string, unknown>),
        type: 'value',
        name: '供应商数',
        position: 'right',
      },
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: monthlyData.value.map(m => m.orderCount),
        itemStyle: {
          color: linearGradient(palette.semantic.warning, 0.9, 0.4),
          borderRadius: [4, 4, 0, 0],
        },
        barMaxWidth: 20,
      },
      {
        name: '供应商数',
        type: 'line',
        yAxisIndex: 1,
        data: monthlyData.value.map(m => m.supplierCount),
        smooth: true,
        showSymbol: false,
        itemStyle: { color: palette.semantic.info },
        lineStyle: { width: 2.5 },
        areaStyle: {
          color: linearGradient(palette.semantic.info, 0.25, 0.02),
        },
      },
    ],
  })
}

/** 初始化所有图表 */
function initAllCharts(): void {
  nextTick(() => {
    initTrendChart()
    initSupplierPieChart()
    initCategoryBarChart()
    initOrderTrendChart()
  })
}

// ==================== 统计卡片 ====================

const statCards = computed(() => [
  {
    icon: 'Wallet',
    label: '采购总金额',
    value: `¥${purchaseReportConverter.formatYuan(summary.totalPurchaseAmount)}`,
    colorType: 'primary' as const,
  },
  {
    icon: 'Document',
    label: '采购订单数',
    value: String(summary.totalOrderCount),
    colorType: 'info' as const,
  },
  {
    icon: 'User',
    label: '供应商数量',
    value: String(summary.supplierCount),
    colorType: 'success' as const,
  },
  {
    icon: 'TrendCharts',
    label: '平均订单金额',
    value: `¥${purchaseReportConverter.formatYuan(summary.avgOrderAmount)}`,
    colorType: 'warning' as const,
  },
  {
    icon: 'CircleCheck',
    label: '准时交付率',
    value: `${purchaseReportConverter.formatPercent(summary.onTimeDeliveryRate)}%`,
    colorType: 'success' as const,
  },
  {
    icon: 'Clock',
    label: '待处理单据',
    value: String(summary.pendingOrders),
    colorType: 'error' as const,
  },
])

// ==================== 表格列定义 ====================

/** 供应商报表列 */
const supplierColumns: DataTableColumn[] = [
  { prop: 'supplierName', label: '供应商名称', minWidth: 180 },
  { prop: 'orderCount', label: '订单数', minWidth: 100, align: 'right' },
  { prop: 'totalAmount', label: '采购总额(元)', minWidth: 140, align: 'right', slot: 'totalAmount' },
  { prop: 'onTimeRate', label: '准时交付率', minWidth: 120, align: 'right', slot: 'onTimeRate' },
  { prop: 'qualifiedRate', label: '质检合格率', minWidth: 120, align: 'right', slot: 'qualifiedRate' },
]

/** 分类报表列 */
const categoryColumns: DataTableColumn[] = [
  { prop: 'categoryName', label: '商品分类', minWidth: 180 },
  { prop: 'orderCount', label: '订单数', minWidth: 100, align: 'right' },
  { prop: 'totalQuantity', label: '总数量', minWidth: 120, align: 'right' },
  { prop: 'totalAmount', label: '采购总额(元)', minWidth: 140, align: 'right', slot: 'totalAmount' },
]

/** 月度明细列 */
const monthlyColumns: DataTableColumn[] = [
  { prop: 'month', label: '月份', minWidth: 120, align: 'center' },
  { prop: 'orderCount', label: '订单数', minWidth: 100, align: 'right' },
  { prop: 'purchaseAmount', label: '采购金额(元)', minWidth: 140, align: 'right', slot: 'purchaseAmount' },
  { prop: 'settledAmount', label: '已结算金额(元)', minWidth: 150, align: 'right', slot: 'settledAmount' },
  { prop: 'supplierCount', label: '供应商数', minWidth: 100, align: 'right' },
]

// ==================== 时间范围处理 ====================

/** 获取指定时间范围的开始和结束日期 */
function getDateRange(type: TimeRangeType): { startDate: string; endDate: string } {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const date = now.getDate()

  const formatDate = (d: Date): string => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }

  let start: Date
  let end: Date = new Date(year, month, date)

  switch (type) {
    case 'today':
      start = new Date(year, month, date)
      break
    case 'week': {
      const dayOfWeek = now.getDay() || 7
      start = new Date(year, month, date - dayOfWeek + 1)
      break
    }
    case 'month':
      start = new Date(year, month, 1)
      break
    case 'quarter': {
      const quarterMonth = Math.floor(month / 3) * 3
      start = new Date(year, quarterMonth, 1)
      break
    }
    case 'year':
      start = new Date(year, 0, 1)
      break
    case 'custom':
      if (customDateRange.value?.[0] && customDateRange.value?.[1]) {
        return {
          startDate: customDateRange.value[0],
          endDate: customDateRange.value[1],
        }
      }
      start = new Date(year, month, 1)
      break
    default:
      start = new Date(year, month, 1)
  }

  return {
    startDate: formatDate(start),
    endDate: formatDate(end),
  }
}

/** 时间范围选项 */
const timeRangeOptions = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本季', value: 'quarter' },
  { label: '本年', value: 'year' },
  { label: '自定义', value: 'custom' },
]

// ==================== 数据加载 ====================

/** 构建查询参数 */
function buildQueryParams(): PurchaseReportQueryForm {
  const { startDate, endDate } = getDateRange(timeRangeType.value)
  return { startDate, endDate }
}

/** 加载所有报表数据 */
async function loadAllData(): Promise<void> {
  loading.value = true
  try {
    const params = buildQueryParams()

    const [summaryRes, monthlyRes, supplierRes, categoryRes] = await Promise.all([
      purchaseReportApi.getSummary(params),
      purchaseReportApi.getMonthlyTrend(params),
      purchaseReportApi.getSupplierReport(params),
      purchaseReportApi.getCategoryReport(params),
    ])

    Object.assign(summary, summaryRes)
    monthlyData.value = monthlyRes
    supplierData.value = supplierRes
    categoryData.value = categoryRes

    initAllCharts()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载报表数据失败')
    } else {
      ElMessage.error('加载报表数据失败')
    }
  } finally {
    loading.value = false
  }
}

/** 刷新数据 */
function handleRefresh(): void {
  loadAllData()
}

/** 时间范围变化时重新加载 */
watch(timeRangeType, () => {
  if (timeRangeType.value !== 'custom') {
    loadAllData()
  }
})

/** 自定义日期变化时重新加载 */
watch(customDateRange, () => {
  if (timeRangeType.value === 'custom' && customDateRange.value?.[0] && customDateRange.value?.[1]) {
    loadAllData()
  }
})

// ==================== 导出功能 ====================

/** 导出报表 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    const params = buildQueryParams()
    const blob = await purchaseReportApi.exportReport({
      ...params,
      format: 'csv',
    })

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toISOString().slice(0, 10)
    link.download = `采购报表_${timestamp}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '导出失败')
    } else {
      ElMessage.error('导出失败')
    }
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

/** 格式化金额 */
function formatAmount(amount: number): string {
  return purchaseReportConverter.formatYuan(amount)
}

/** 格式化百分比 */
function formatPercent(percent: number): string {
  return purchaseReportConverter.formatPercent(percent)
}

/** 获取交付率状态颜色 */
function getRateStatus(rate: number): string {
  if (rate >= 90) return 'success'
  if (rate >= 70) return 'warning'
  return 'error'
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadAllData()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="采购报表" description="采购数据分析和报表展示">
      <el-button type="primary" :icon="Download" :loading="exportLoading" @click="handleExport">
        导出Excel
      </el-button>
      <el-button :icon="Refresh" @click="handleRefresh">
        刷新
      </el-button>
    </PageHeader>

    <!-- 顶部筛选栏 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-left">
          <span class="filter-label">时间范围：</span>
          <el-radio-group v-model="timeRangeType" size="default" @change="loadAllData">
            <el-radio-button
              v-for="opt in timeRangeOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="timeRangeType === 'custom'"
            v-model="customDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            :teleported="false"
            @change="loadAllData"
          />
        </div>
        <div class="filter-right">
          <el-button type="primary" :icon="Search" @click="loadAllData">查询</el-button>
        </div>
      </div>
    </div>

    <!-- 内容区：统计卡片 + 图表 + 明细统一水平对齐 -->
    <div class="content-wrapper">
      <!-- 统计卡片区域 -->
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
        <div class="charts-row">
        <!-- 采购趋势图 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">采购趋势分析</h4>
          </div>
          <div ref="trendChartRef" class="chart-area"></div>
        </div>

        <!-- 供应商采购占比 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">供应商采购占比 TOP8</h4>
          </div>
          <div ref="supplierPieRef" class="chart-area"></div>
        </div>
      </div>

      <div class="charts-row">
        <!-- 商品分类采购排行 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">商品分类采购排行 TOP10</h4>
          </div>
          <div ref="categoryBarRef" class="chart-area"></div>
        </div>

        <!-- 订单趋势分析 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">订单趋势分析</h4>
          </div>
          <div ref="orderTrendRef" class="chart-area"></div>
        </div>
      </div>
    </section>

    <!-- 明细数据区域（标签页切换） -->
    <section class="detail-section">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- 采购金额报表 -->
        <el-tab-pane label="采购金额报表" name="amount">
          <div v-loading="loading" class="tab-content">
            <DataTable
              :columns="monthlyColumns"
              :data="monthlyData"
              :loading="loading"
              :selectable="false"
              stripe
            >
              <template #purchaseAmount="{ row }">
                <span class="amount-text">¥{{ formatAmount(row.purchaseAmount) }}</span>
              </template>
              <template #settledAmount="{ row }">
                <span class="amount-text">¥{{ formatAmount(row.settledAmount) }}</span>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- 供应商报表 -->
        <el-tab-pane label="供应商报表" name="supplier">
          <div v-loading="loading" class="tab-content">
            <DataTable
              :columns="supplierColumns"
              :data="supplierData"
              :loading="loading"
              :selectable="false"
              stripe
            >
              <template #totalAmount="{ row }">
                <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
              </template>
              <template #onTimeRate="{ row }">
                <StatusTag
                  :status="getRateStatus(row.onTimeRate)"
                  :label="`${formatPercent(row.onTimeRate)}%`"
                  size="small"
                  variant="light"
                />
              </template>
              <template #qualifiedRate="{ row }">
                <StatusTag
                  :status="getRateStatus(row.qualifiedRate)"
                  :label="`${formatPercent(row.qualifiedRate)}%`"
                  size="small"
                  variant="light"
                />
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- 商品分类报表 -->
        <el-tab-pane label="商品分类报表" name="category">
          <div v-loading="loading" class="tab-content">
            <DataTable
              :columns="categoryColumns"
              :data="categoryData"
              :loading="loading"
              :selectable="false"
              stripe
            >
              <template #totalAmount="{ row }">
                <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
  border-radius: var(--fts-page-radius);
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

.filter-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  font-weight: var(--fts-font-weight-medium);
}

// 内容区无水平内边距，让 stats-section/charts-section 铺满 main 宽度
.content-wrapper {
  padding: 0;
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1600px) {
    grid-template-columns: repeat(4, 1fr);
  }

  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
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
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-4);

  @media (max-width: 992px) {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
  min-width: 0;
  overflow: hidden;
}

.chart-card__header {
  margin-bottom: var(--fts-space-3);
}

.chart-card__title {
  margin: 0;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.chart-area {
  width: 100%;
  height: 320px;
  min-width: 0;
}

// 明细数据区域（无水平内边距，铺满 main 宽度）
.detail-section {
  padding: 0 0 var(--fts-space-6);
}

.detail-tabs {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: 0 var(--fts-space-4);

  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 var(--fts-space-2);
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-medium);
  }
}

.tab-content {
  padding: var(--fts-space-4) 0;
  min-height: 300px;
}

// 金额文字
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
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

  .detail-section {
    padding: 0 0 var(--fts-space-4);
  }

  .chart-area {
    height: 280px;
  }
}

@media (max-width: 576px) {
  .filter-label {
    display: none;
  }

  .chart-area {
    height: 240px;
  }
}
</style>
