<script setup lang="ts">
/**
 * 采购数据分析页面（重新设计 · 对齐项目设计语言）
 *
 * 设计原则：
 * 1. 统一使用项目标准结构：modern-page + PageHeader + filter-section + stats-section + charts-section + detail-section
 * 2. KPI 使用 StatCard 组件（variant="bordered"），与采购报表保持一致
 * 3. 图表卡使用项目标准 chart-card 结构，移除自定义 accent 条 + title-wrap
 * 4. 业务导向布局：4 KPI + 趋势/供应商/品类/绩效 4 图表 + 供应商明细表
 * 5. 散点图（供应商绩效矩阵）替代单纯占比，提供更深洞察
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import { useECharts } from '@/composables/useECharts'
import {
  getChartPalette,
  buildBaseOption,
  linearGradient,
  withBarRadius,
  formatWan,
} from '@/composables/useChartPalette'
import { purchaseAnalysisApi } from '@/api/purchase'
import type {
  PurchaseAnalysisTrendItem,
  PurchaseAnalysisSupplierItem,
  PurchaseAnalysisCategoryItem,
  PurchaseAnalysisSummary,
} from '@/types/purchase-analysis'

const router = useRouter()

// ==================== 状态 ====================

const loading = ref(false)
const loadingError = ref('')

const filterForm = ref({
  dateRange: [] as string[],
})

const summaryData = ref<PurchaseAnalysisSummary | null>(null)
const trendData = ref<PurchaseAnalysisTrendItem[]>([])
const supplierData = ref<PurchaseAnalysisSupplierItem[]>([])
const categoryData = ref<PurchaseAnalysisCategoryItem[]>([])

// ==================== 图表 ====================

const trendChartRef = ref<HTMLElement>()
const supplierPieRef = ref<HTMLElement>()
const categoryBarRef = ref<HTMLElement>()
const supplierScatterRef = ref<HTMLElement>()

const trendChart = useECharts(trendChartRef)
const supplierPieChart = useECharts(supplierPieRef)
const categoryBarChart = useECharts(categoryBarRef)
const supplierScatterChart = useECharts(supplierScatterRef)

// ==================== 计算属性 ====================

/** 4 个核心 KPI */
const statCards = computed(() => {
  const s = summaryData.value
  return [
    {
      icon: 'Money',
      label: '采购总额',
      value: s ? `¥${formatNumber(s.totalPurchaseAmount)}` : '—',
      colorType: 'primary' as const,
    },
    {
      icon: 'ShoppingCart',
      label: '订单总数',
      value: s ? formatNumber(s.totalOrderCount) : '—',
      colorType: 'success' as const,
    },
    {
      icon: 'Coin',
      label: '平均订单金额',
      value: s ? `¥${formatNumber(s.avgOrderAmount)}` : '—',
      colorType: 'warning' as const,
    },
    {
      icon: 'Goods',
      label: '活跃供应商',
      value: s ? formatNumber(s.supplierCount) : '—',
      colorType: 'info' as const,
    },
  ]
})

/** 供应商明细表列定义 */
const supplierColumns: DataTableColumn[] = [
  { prop: 'supplierName', label: '供应商名称', minWidth: 180, showOverflowTooltip: true },
  { prop: 'totalAmount', label: '采购总额', minWidth: 130, align: 'right', slot: 'totalAmount' },
  { prop: 'orderCount', label: '订单数', minWidth: 90, align: 'center' },
  { prop: 'onTimeRate', label: '准时交付率', minWidth: 110, align: 'center', slot: 'onTimeRate' },
  { prop: 'qualifiedRate', label: '质检合格率', minWidth: 110, align: 'center', slot: 'qualifiedRate' },
]

// ==================== 辅助方法 ====================

function formatNumber(num: number): string {
  if (!Number.isFinite(num)) return '0'
  if (Math.abs(num) >= 10000) return (num / 10000).toFixed(2) + '万'
  return num.toLocaleString()
}

function buildQueryParams(): { startDate?: string; endDate?: string } {
  const range = filterForm.value.dateRange
  if (range && range.length === 2 && range[0] && range[1]) {
    return { startDate: range[0], endDate: range[1] }
  }
  return {}
}

/** 准时率状态颜色 */
function getRateColor(rate: number): string {
  if (rate >= 90) return 'var(--fts-success)'
  if (rate >= 70) return 'var(--fts-warning)'
  return 'var(--fts-error)'
}

// ==================== 数据加载 ====================

async function loadAllData(): Promise<void> {
  loading.value = true
  loadingError.value = ''
  try {
    const params = buildQueryParams()
    const [summary, trend, supplier, category] = await Promise.all([
      purchaseAnalysisApi.getSummary(params),
      purchaseAnalysisApi.getTrend(params),
      purchaseAnalysisApi.getSupplier(params),
      purchaseAnalysisApi.getCategory(params),
    ])
    summaryData.value = summary
    trendData.value = trend
    supplierData.value = supplier
    categoryData.value = category
  } catch (error: unknown) {
    if (error instanceof Error) {
      loadingError.value = error.message || '加载分析数据失败'
      ElMessage.error(loadingError.value)
    } else {
      loadingError.value = '加载分析数据失败'
      ElMessage.error(loadingError.value)
    }
  } finally {
    loading.value = false
  }
}

// ==================== 图表配置 ====================

/** 采购趋势：柱状（金额） + 折线（订单数）双轴 */
function initTrendChart(): void {
  if (trendData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const months = trendData.value.map(d => d.month || '')
  const amounts = trendData.value.map(d => d.purchaseAmount || 0)
  const counts = trendData.value.map(d => d.orderCount || 0)

  trendChart.setOption({
    ...base,
    tooltip: { ...(base.tooltip as Record<string, unknown>), trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { ...(base.legend as Record<string, unknown>), data: ['采购金额', '订单数量'], top: 0 },
    grid: { left: 56, right: 56, top: 48, bottom: 32 },
    xAxis: { ...(base.xAxis as Record<string, unknown>), type: 'category', data: months },
    yAxis: [
      {
        type: 'value', name: '金额(元)',
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: palette.textSecondary, formatter: (v: number) => formatWan(v, '') },
        splitLine: { lineStyle: { color: palette.splitColor, type: 'dashed' } },
      },
      {
        type: 'value', name: '数量(单)', position: 'right',
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: palette.textSecondary },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '采购金额', type: 'bar', data: withBarRadius(amounts.map(v => ({ value: v }))), barMaxWidth: 28,
        itemStyle: { color: linearGradient(palette.semantic.primary, 0.95, 0.55) },
      },
      {
        name: '订单数量', type: 'line', yAxisIndex: 1, data: counts, smooth: true, showSymbol: false,
        itemStyle: { color: palette.semantic.success }, lineStyle: { width: 2.5 },
        areaStyle: { color: linearGradient(palette.semantic.success, 0.3, 0.02) },
      },
    ],
  })
}

/** 供应商采购占比：环形图 TOP8 */
function initSupplierPieChart(): void {
  if (supplierData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  // 取 TOP8，其余合并为"其他"
  const sorted = [...supplierData.value].sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0))
  const top = sorted.slice(0, 8)
  const others = sorted.slice(8)
  const pieData = top.map(d => ({ name: d.supplierName || '—', value: d.totalAmount || 0 }))
  if (others.length > 0) {
    const othersTotal = others.reduce((sum, d) => sum + (d.totalAmount || 0), 0)
    pieData.push({ name: '其他', value: othersTotal })
  }

  supplierPieChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>), trigger: 'item',
      formatter: (param: { name: string; value: number; percent: number }) =>
        `${param.name}<br/>采购金额: ¥${formatNumber(param.value)}<br/>占比: ${param.percent}%`,
    },
    legend: { ...(base.legend as Record<string, unknown>), orient: 'vertical', right: 8, top: 'middle' },
    grid: undefined, xAxis: undefined, yAxis: undefined,
    series: [{
      name: '供应商采购', type: 'pie', radius: ['42%', '70%'], center: ['35%', '50%'],
      itemStyle: { borderRadius: 6, borderColor: palette.cardBg, borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, scale: true, scaleSize: 6 },
      data: pieData.map((d, i) => ({ ...d, itemStyle: { color: palette.primary[i % palette.primary.length] } })),
    }],
  })
}

/** 品类采购分布：横向条形图 TOP10 */
function initCategoryBarChart(): void {
  if (categoryData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  // 取 TOP10，按金额升序排列（最大值在顶部）
  const sorted = [...categoryData.value]
    .sort((a, b) => (b.totalAmount || 0) - (a.totalAmount || 0))
    .slice(0, 10)
    .reverse()
  const names = sorted.map(d => d.categoryName || '—')
  const amounts = sorted.map(d => d.totalAmount || 0)

  categoryBarChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>), trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ name: string; value: number }>) => {
        const p = params[0]
        return `${p.name}<br/>采购金额: ¥${formatNumber(p.value)}`
      },
    },
    legend: undefined,
    grid: { left: 8, right: 56, top: 16, bottom: 16 },
    xAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: palette.textSecondary, formatter: (v: number) => formatWan(v, '') },
      splitLine: { lineStyle: { color: palette.splitColor, type: 'dashed' } },
    },
    yAxis: {
      type: 'category', data: names,
      axisLine: { lineStyle: { color: palette.splitColor } },
      axisTick: { show: false },
      axisLabel: { color: palette.textColor, fontSize: 12, width: 100, overflow: 'truncate' },
    },
    series: [{
      name: '采购金额', type: 'bar',
      data: withBarRadius(amounts.map(v => ({ value: v })), [0, 6, 6, 0]),
      barMaxWidth: 18,
      itemStyle: { color: linearGradient(palette.semantic.warning, 0.95, 0.55) },
      label: { show: true, position: 'right', color: palette.textSecondary, fontSize: 11, formatter: (p: { value: number }) => formatWan(p.value, '元') },
    }],
  })
}

/** 供应商绩效矩阵：散点图（X=采购金额，Y=准时率，气泡大小=订单数） */
function initSupplierScatterChart(): void {
  if (supplierData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  // 数据点：[金额, 准时率, 订单数, 供应商名]
  const scatterData = supplierData.value.map(d => ({
    value: [d.totalAmount || 0, d.onTimeRate ?? 0, d.orderCount || 1],
    name: d.supplierName || '—',
  }))

  supplierScatterChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>), trigger: 'item',
      formatter: (param: { data: { name: string; value: number[] } }) => {
        const [amount, rate, orders] = param.data.value
        return `${param.data.name}<br/>采购金额: ¥${formatNumber(amount)}<br/>准时率: ${rate.toFixed(1)}%<br/>订单数: ${orders}`
      },
    },
    legend: undefined,
    grid: { left: 56, right: 24, top: 24, bottom: 40 },
    xAxis: {
      type: 'value', name: '采购金额(元)',
      nameLocation: 'middle', nameGap: 28,
      axisLine: { lineStyle: { color: palette.splitColor } },
      axisTick: { show: false },
      axisLabel: { color: palette.textSecondary, formatter: (v: number) => formatWan(v, '') },
      splitLine: { lineStyle: { color: palette.splitColor, type: 'dashed' } },
    },
    yAxis: {
      type: 'value', name: '准时交付率(%)', min: 0, max: 100,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: palette.textSecondary, formatter: '{value}%' },
      splitLine: { lineStyle: { color: palette.splitColor, type: 'dashed' } },
    },
    series: [{
      name: '供应商绩效', type: 'scatter',
      data: scatterData,
      symbolSize: (val: number[]) => Math.max(8, Math.min(40, Math.sqrt(val[2]) * 2.5)),
      itemStyle: {
        color: palette.semantic.primary,
        opacity: 0.75,
        borderColor: palette.cardBg,
        borderWidth: 1.5,
      },
      emphasis: {
        itemStyle: { opacity: 1, borderColor: palette.semantic.primary, borderWidth: 2 },
        label: { show: true, position: 'top', color: palette.textColor, fontSize: 11, formatter: (p: { data: { name: string } }) => p.data.name },
      },
      // 90% 警戒线
      markLine: {
        silent: true, symbol: 'none',
        lineStyle: { color: palette.semantic.success, type: 'dashed', opacity: 0.5 },
        label: { color: palette.semantic.success, formatter: '优秀 90%', position: 'insideStartTop' },
        data: [{ yAxis: 90 }],
      },
    }],
  })
}

async function initAllCharts(): Promise<void> {
  await nextTick()
  // useECharts 已监听容器引用，v-if 渲染后会自动补初始化，此处直接 setOption
  initTrendChart()
  initSupplierPieChart()
  initCategoryBarChart()
  initSupplierScatterChart()
}

// ==================== 事件 ====================

function handleSearch(): void {
  loadAllData().then(initAllCharts)
}

function handleRefresh(): void {
  loadAllData().then(initAllCharts)
}

function handleReset(): void {
  filterForm.value.dateRange = []
  loadAllData().then(initAllCharts)
}

function goBack(): void {
  router.push('/purchase/report')
}

onMounted(async () => {
  await loadAllData()
  initAllCharts()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="采购数据分析" description="多维度采购数据洞察 · 趋势/供应商/品类/绩效分析">
      <el-button :icon="ArrowLeft" @click="goBack">返回报表</el-button>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
    </PageHeader>

    <!-- 顶部筛选栏 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-left">
          <span class="filter-label">时间范围：</span>
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :teleported="false"
            :disabled="loading"
            style="width: 130px"
          />
        </div>
        <div class="filter-right">
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="loadingError" class="error-section">
      <el-alert :title="loadingError" type="error" :closable="true" show-icon />
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
        <!-- 采购趋势分析 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">采购趋势分析</h4>
            <span class="chart-card__subtitle">金额 + 订单双轴</span>
          </div>
          <div v-if="trendData.length > 0" ref="trendChartRef" class="chart-area" />
          <el-empty v-else description="暂无趋势分析数据" :image-size="80" class="chart-empty" />
        </div>

        <!-- 供应商采购占比 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">供应商采购占比</h4>
            <span class="chart-card__subtitle">TOP 8 环形图</span>
          </div>
          <div v-if="supplierData.length > 0" ref="supplierPieRef" class="chart-area" />
          <el-empty v-else description="暂无供应商数据" :image-size="80" class="chart-empty" />
        </div>
      </div>

      <div class="charts-row">
        <!-- 品类采购分布 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">品类采购分布</h4>
            <span class="chart-card__subtitle">TOP 10 横向条形</span>
          </div>
          <div v-if="categoryData.length > 0" ref="categoryBarRef" class="chart-area" />
          <el-empty v-else description="暂无品类数据" :image-size="80" class="chart-empty" />
        </div>

        <!-- 供应商绩效矩阵 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">供应商绩效矩阵</h4>
            <span class="chart-card__subtitle">金额 × 准时率 × 订单数</span>
          </div>
          <div v-if="supplierData.length > 0" ref="supplierScatterRef" class="chart-area" />
          <el-empty v-else description="暂无绩效数据" :image-size="80" class="chart-empty" />
        </div>
      </div>
    </section>

    <!-- 供应商明细数据区域 -->
    <section class="detail-section">
      <div class="detail-card">
        <div class="detail-card__header">
          <h4 class="detail-card__title">供应商采购明细</h4>
          <span class="detail-card__subtitle">共 {{ supplierData.length }} 家</span>
        </div>
        <div class="detail-card__body">
          <DataTable
            v-if="supplierData.length > 0"
            :data="supplierData"
            :columns="supplierColumns"
            :loading="loading"
            :selectable="false"
            :stripe="true"
            :border="false"
          >
            <template #totalAmount="{ row }">
              <span class="amount-text">¥ {{ formatNumber(row.totalAmount) }}</span>
            </template>
            <template #onTimeRate="{ row }">
              <span :style="{ color: getRateColor(row.onTimeRate), fontWeight: 'var(--fts-font-weight-medium)' }">
                {{ row.onTimeRate?.toFixed(1) ?? '—' }}%
              </span>
            </template>
            <template #qualifiedRate="{ row }">
              <span :style="{ color: getRateColor(row.qualifiedRate), fontWeight: 'var(--fts-font-weight-medium)' }">
                {{ row.qualifiedRate?.toFixed(1) ?? '—' }}%
              </span>
            </template>
          </DataTable>
          <el-empty v-else description="暂无供应商明细数据" :image-size="80" class="chart-empty" />
        </div>
      </div>
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

// 错误提示
.error-section {
  padding: var(--fts-space-3) var(--fts-space-6) 0;
}

// 内容区无水平内边距，让 stats-section/charts-section 铺满 main 宽度
.content-wrapper {
  padding: 0;
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1200px) {
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
  transition: box-shadow var(--fts-duration-fast);

  &:hover {
    box-shadow: var(--fts-shadow-card-hover);
  }
}

.chart-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
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

.chart-area {
  width: 100%;
  height: 320px;
  min-width: 0;
}

.chart-empty {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 明细数据区域（无水平内边距，铺满 main 宽度）
.detail-section {
  padding: 0 0 var(--fts-space-6);
}

.detail-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
}

.detail-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) var(--fts-space-4) var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.detail-card__title {
  margin: 0;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.detail-card__subtitle {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: var(--fts-font-weight-medium);
}

.detail-card__body {
  padding: var(--fts-space-3) var(--fts-space-4);
  min-height: 200px;
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

  .chart-area,
  .chart-empty {
    height: 280px;
  }
}

@media (max-width: 576px) {
  .filter-label {
    display: none;
  }

  .chart-area,
  .chart-empty {
    height: 240px;
  }
}
</style>
