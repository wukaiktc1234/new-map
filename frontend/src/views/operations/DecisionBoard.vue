<script setup lang="ts">
/**
 * 经营分析页面（重新设计 · 对齐项目设计语言）
 *
 * 设计原则：
 * 1. 统一使用项目标准结构：modern-page + PageHeader + filter-section + stats-section + charts-section
 * 2. KPI 使用 StatCard 组件（variant="bordered"），与采购报表保持一致
 * 3. 图表卡使用项目标准 chart-card 结构，移除自定义 accent 条
 * 4. 聚焦经营决策核心：4 个 KPI + 2 行 2 列图表（销售/门店/品类/成本）
 * 5. 删除冗余模块（会员增长、库存预警、KPI 对比表），保持决策看板的聚焦性
 */
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import { useECharts } from '@/composables/useECharts'
import {
  getChartPalette,
  buildBaseOption,
  linearGradient,
  withBarRadius,
  formatWan,
} from '@/composables/useChartPalette'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { decisionBoardApi } from '@/api/operations/decision-board'
import type { StoreRankItem, CategoryItem } from '@/api/operations/decision-board'

// ==================== 类型定义 ====================

type TimeRangeType = 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'

interface StatsOverview {
  totalRevenue: number
  totalOrders: number
  avgOrderValue: number
  memberConsumptionRatio: number
  purchaseCost: number
  grossProfit: number
  grossProfitMargin: number
  inventoryTurnoverDays: number
  totalRevenueTrend: number
  totalOrdersTrend: number
  avgOrderValueTrend: number
  memberConsumptionRatioTrend: number
  purchaseCostTrend: number
  grossProfitTrend: number
  grossProfitMarginTrend: number
  inventoryTurnoverDaysTrend: number
}

interface SalesTrendItem {
  date: string
  revenue: number
  orders: number
}

interface CostComponentItem {
  name: string
  value: number
}

// ==================== 响应式数据 ====================

const loading = ref(false)
const { storeOptions } = useStoreOptions(true)

const timeRangeType = ref<TimeRangeType>('month')
const customDateRange = ref<[string, string] | null>(null)
const storeId = ref<string>('')

const statsOverview = ref<StatsOverview | null>(null)
const salesTrendData = ref<SalesTrendItem[]>([])
const storeRankingData = ref<StoreRankItem[]>([])
const categorySalesData = ref<CategoryItem[]>([])
const costComponentData = ref<CostComponentItem[]>([])

// ==================== 图表引用 ====================

const trendChartRef = ref<HTMLElement>()
const storeRankRef = ref<HTMLElement>()
const categoryPieRef = ref<HTMLElement>()
const costRingRef = ref<HTMLElement>()

const trendChart = useECharts(trendChartRef)
const storeRankChart = useECharts(storeRankRef)
const categoryPieChart = useECharts(categoryPieRef)
const costRingChart = useECharts(costRingRef)

// ==================== 配置 ====================

const timeRangeOptions = [
  { label: '今日', value: 'today' as TimeRangeType },
  { label: '本周', value: 'week' as TimeRangeType },
  { label: '本月', value: 'month' as TimeRangeType },
  { label: '本季', value: 'quarter' as TimeRangeType },
  { label: '本年', value: 'year' as TimeRangeType },
  { label: '自定义', value: 'custom' as TimeRangeType },
]

// ==================== 计算属性 ====================

/** 4 个核心 KPI（精简到决策关键指标） */
const statCards = computed(() => {
  const s = statsOverview.value
  return [
    {
      icon: 'Money',
      label: '营业额',
      value: s ? `¥${formatNumber(s.totalRevenue)}` : '—',
      colorType: 'primary' as const,
      trend: s?.totalRevenueTrend ?? 0,
    },
    {
      icon: 'TrendCharts',
      label: '毛利润',
      value: s ? `¥${formatNumber(s.grossProfit)}` : '—',
      colorType: 'success' as const,
      trend: s?.grossProfitTrend ?? 0,
    },
    {
      icon: 'ShoppingCart',
      label: '订单数',
      value: s ? formatNumber(s.totalOrders) : '—',
      colorType: 'warning' as const,
      trend: s?.totalOrdersTrend ?? 0,
    },
    {
      icon: 'Coin',
      label: '客单价',
      value: s ? `¥${formatNumber(s.avgOrderValue)}` : '—',
      colorType: 'info' as const,
      trend: s?.avgOrderValueTrend ?? 0,
    },
  ]
})

// ==================== 辅助方法 ====================

function formatNumber(num: number): string {
  if (!Number.isFinite(num)) return '0'
  if (Math.abs(num) >= 10000) return (num / 10000).toFixed(2) + '万'
  return num.toLocaleString()
}

// ==================== 数据加载 ====================

async function loadStatsOverview(): Promise<void> {
  // 后端接口待接入，暂保持空状态
  statsOverview.value = null
}

async function loadSalesTrend(): Promise<void> {
  salesTrendData.value = []
}

async function loadStoreRanking(): Promise<void> {
  try {
    const data = await decisionBoardApi.getStoreRanking()
    storeRankingData.value = data.slice(0, 10)
  } catch {
    storeRankingData.value = []
  }
}

async function loadCategorySales(): Promise<void> {
  try {
    const data = await decisionBoardApi.getCategorySales()
    categorySalesData.value = data
  } catch {
    categorySalesData.value = []
  }
}

async function loadCostComponent(): Promise<void> {
  costComponentData.value = []
}

async function loadAllData(): Promise<void> {
  loading.value = true
  try {
    await Promise.all([
      loadStatsOverview(),
      loadSalesTrend(),
      loadStoreRanking(),
      loadCategorySales(),
      loadCostComponent(),
    ])
    await nextTick()
    initAllCharts()
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ==================== 图表配置 ====================

/** 销售趋势：折线（营业额） + 柱状（订单数）双轴 */
function initTrendChart(): void {
  if (salesTrendData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const dates = salesTrendData.value.map(d => d.date)
  const revenues = salesTrendData.value.map(d => d.revenue)
  const orders = salesTrendData.value.map(d => d.orders)

  trendChart.setOption({
    ...base,
    tooltip: { ...(base.tooltip as Record<string, unknown>), trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { ...(base.legend as Record<string, unknown>), data: ['营业额', '订单数'], top: 0 },
    grid: { left: 56, right: 56, top: 48, bottom: 32 },
    xAxis: { ...(base.xAxis as Record<string, unknown>), type: 'category', data: dates },
    yAxis: [
      {
        type: 'value', name: '营业额(元)',
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: palette.textSecondary, formatter: (v: number) => formatWan(v, '') } ,
        splitLine: { lineStyle: { color: palette.splitColor, type: 'dashed' } },
      },
      {
        type: 'value', name: '订单数', position: 'right',
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: palette.textSecondary },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '营业额', type: 'line', data: revenues, smooth: true, showSymbol: false,
        lineStyle: { width: 2.5 }, itemStyle: { color: palette.semantic.primary },
        areaStyle: { color: linearGradient(palette.semantic.primary, 0.35, 0.02) },
      },
      {
        name: '订单数', type: 'line', yAxisIndex: 1, data: orders, smooth: true, showSymbol: false,
        lineStyle: { width: 2, type: 'dashed' }, itemStyle: { color: palette.semantic.success },
      },
    ],
  })
}

/** 门店排行：横向条形图（TOP10） */
function initStoreRankChart(): void {
  if (storeRankingData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  // 倒序排列，使 TOP1 显示在最上方
  const sorted = [...storeRankingData.value].sort((a, b) => a.revenue - b.revenue)
  const names = sorted.map(item => item.storeName)
  const revenues = sorted.map(item => item.revenue)

  storeRankChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>), trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ name: string; value: number }>) => {
        const p = params[0]
        return `${p.name}<br/>营业额: ¥${formatNumber(p.value)}`
      },
    },
    legend: undefined,
    grid: { left: 8, right: 48, top: 16, bottom: 16 },
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
      name: '营业额', type: 'bar', data: withBarRadius(revenues.map(v => ({ value: v })), [0, 6, 6, 0]),
      barMaxWidth: 18,
      itemStyle: { color: linearGradient(palette.semantic.primary, 0.95, 0.55) },
      label: { show: true, position: 'right', color: palette.textSecondary, fontSize: 11, formatter: (p: { value: number }) => formatWan(p.value, '元') },
    }],
  })
}

/** 品类销售占比：环形图 */
function initCategoryPieChart(): void {
  if (categorySalesData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const pieData = categorySalesData.value.map(item => ({ name: item.name, value: item.value }))

  categoryPieChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>), trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    legend: { ...(base.legend as Record<string, unknown>), orient: 'vertical', right: 8, top: 'middle' },
    grid: undefined, xAxis: undefined, yAxis: undefined,
    series: [{
      name: '品类销售', type: 'pie', radius: ['42%', '70%'], center: ['35%', '50%'],
      itemStyle: { borderRadius: 6, borderColor: palette.cardBg, borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, scale: true, scaleSize: 6 },
      data: pieData.map((d, i) => ({ ...d, itemStyle: { color: palette.primary[i % palette.primary.length] } })),
    }],
  })
}

/** 成本构成：玫瑰图 */
function initCostRingChart(): void {
  if (costComponentData.value.length === 0) return
  const palette = getChartPalette()
  const base = buildBaseOption(palette)
  const ringData = costComponentData.value.map(item => ({ name: item.name, value: item.value }))

  costRingChart.setOption({
    ...base,
    tooltip: {
      ...(base.tooltip as Record<string, unknown>), trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    legend: { ...(base.legend as Record<string, unknown>), orient: 'vertical', right: 8, top: 'middle' },
    grid: undefined, xAxis: undefined, yAxis: undefined,
    series: [{
      name: '成本构成', type: 'pie', radius: ['30%', '70%'], center: ['35%', '50%'], roseType: 'radius',
      itemStyle: { borderRadius: 6, borderColor: palette.cardBg, borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, scale: true, scaleSize: 6 },
      data: ringData.map((d, i) => ({ ...d, itemStyle: { color: palette.primary[i % palette.primary.length] } })),
    }],
  })
}

function initAllCharts(): void {
  initTrendChart()
  initStoreRankChart()
  initCategoryPieChart()
  initCostRingChart()
}

// ==================== 事件处理 ====================

function handleRefresh(): void {
  loadAllData()
}

function handleSearch(): void {
  loadAllData()
}

// 时间范围变化时重新加载
watch(timeRangeType, (v) => {
  if (v !== 'custom') loadAllData()
})

watch(customDateRange, () => {
  if (timeRangeType.value === 'custom' && customDateRange.value?.[0] && customDateRange.value?.[1]) {
    loadAllData()
  }
})

onMounted(() => {
  loadAllData()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="经营分析" description="企业经营数据看板 · 关键指标一目了然">
      <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
    </PageHeader>

    <!-- 顶部筛选栏 -->
    <div class="filter-section">
      <div class="filter-row">
        <div class="filter-left">
          <span class="filter-label">时间范围：</span>
          <el-radio-group v-model="timeRangeType" size="default">
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
            value-format="YYYY-MM-DD"
            :teleported="false"
          />
          <span class="filter-label">门店：</span>
          <el-select
            v-model="storeId"
            placeholder="全部门店"
            clearable
            style="width: 180px"
            :teleported="false"
            @change="handleSearch"
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
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">查询</el-button>
        </div>
      </div>
    </div>

    <!-- 内容区：统计卡片 + 图表统一水平对齐 -->
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
          :trend="stat.trend"
          variant="bordered"
        />
      </section>

      <!-- 图表区域 -->
      <section class="charts-section">
        <div class="charts-row">
        <!-- 销售趋势分析 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">销售趋势分析</h4>
            <span class="chart-card__subtitle">营业额 + 订单数双轴</span>
          </div>
          <div v-if="salesTrendData.length > 0" ref="trendChartRef" class="chart-area" />
          <el-empty v-else description="暂无销售趋势数据 · 待后端接口接入" :image-size="80" class="chart-empty" />
        </div>

        <!-- 门店营业额排行 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">门店营业额排行</h4>
            <span class="chart-card__subtitle">TOP {{ storeRankingData.length || 10 }}</span>
          </div>
          <div v-if="storeRankingData.length > 0" ref="storeRankRef" class="chart-area" />
          <el-empty v-else description="暂无门店排名数据" :image-size="80" class="chart-empty" />
        </div>
      </div>

      <div class="charts-row">
        <!-- 品类销售占比 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">品类销售占比</h4>
            <span class="chart-card__subtitle">环形图</span>
          </div>
          <div v-if="categorySalesData.length > 0" ref="categoryPieRef" class="chart-area" />
          <el-empty v-else description="暂无品类数据" :image-size="80" class="chart-empty" />
        </div>

        <!-- 成本构成分析 -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h4 class="chart-card__title">成本构成分析</h4>
            <span class="chart-card__subtitle">玫瑰图</span>
          </div>
          <div v-if="costComponentData.length > 0" ref="costRingRef" class="chart-area" />
          <el-empty v-else description="暂无成本数据 · 待后端接口接入" :image-size="80" class="chart-empty" />
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

// 内容区：统一水平内边距，保证与标题栏/filter-section 内容区对齐
.content-wrapper {
  padding: 0 var(--fts-space-6);
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
    padding: 0 var(--fts-space-4);
  }

  .stats-section {
    padding: var(--fts-space-3) 0;
  }

  .charts-section {
    padding: 0 0 var(--fts-space-3);
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
