<script setup lang="ts">
/**
 * 资产报表页面
 * 功能：资产总览、折旧分析、维修统计三个Tab
 */
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import { assetApi, categoryApi, depreciationApi, maintenanceApi, disposalApi } from '@/api/asset'
import type { AssetStatisticsVO, AssetCategory, DepreciationRecord, MaintenanceRecord } from '@/types/asset'
import { AssetStatusTagMap, DepreciationMethodOptions, MaintenanceStatusTagMap } from '@/types/asset'
import type { AssetStatus, DepreciationMethod, MaintenanceStatus } from '@/types/asset'
import { fenToYuan, formatFenToYuan } from '@/utils/money'

/* ===== 页面状态 ===== */
const loading = ref(false)
const activeTab = ref('overview')

/* ===== 资产总览数据 ===== */
const statistics = ref<AssetStatisticsVO | null>(null)
const categoryList = ref<AssetCategory[]>([])

/* ===== 折旧分析数据 ===== */
const depreciationList = ref<DepreciationRecord[]>([])

/* ===== 维修统计数据 ===== */
const maintenanceList = ref<MaintenanceRecord[]>([])

/* ===== 图表引用 ===== */
const statusPieRef = ref<HTMLElement>()
const categoryBarRef = ref<HTMLElement>()
const depreciationTrendRef = ref<HTMLElement>()
let statusPieChart: echarts.ECharts | null = null
let categoryBarChart: echarts.ECharts | null = null
let depreciationTrendChart: echarts.ECharts | null = null

/* ===== 工具函数 ===== */
// fenToYuan 已从 @/utils/money 导入，禁止在组件内直接做 /100 金额转换

function formatYuan(fen: number): string {
  return formatFenToYuan(fen).replace('¥', '')
}

function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function getChartTextColor(): string {
  const primary = getCssVar('--fts-text-primary')
  if (primary === '#ffffff' || primary === '#fff') return '#ffffff'
  return primary || '#333333'
}

/** 获取图表边框颜色（使用卡片背景色，使饼图边框与背景融合） */
function getChartBorderColor(): string {
  return getCssVar('--fts-bg-card') || getCssVar('--fts-bg-page') || '#ffffff'
}

function getMethodLabel(method: DepreciationMethod): string {
  return DepreciationMethodOptions.find(o => o.value === method)?.label || method
}

function getStatusInfo(status: AssetStatus) {
  return AssetStatusTagMap[status] || { status: 'info', label: status }
}

function getMaintenanceStatusInfo(status: MaintenanceStatus) {
  return MaintenanceStatusTagMap[status] || { status: 'info', label: status }
}

/* ===== 统计卡片 ===== */
const overviewStats = computed(() => {
  const s = statistics.value
  if (!s) return []
  return [
    { icon: 'Grid', label: '资产总数', value: String(s.totalAssets), colorType: 'primary' as const },
    { icon: 'TrendCharts', label: '资产总值(元)', value: formatYuan(s.totalOriginalValue), colorType: 'info' as const },
    { icon: 'Wallet', label: '净值合计(元)', value: formatYuan(s.totalCurrentValue), colorType: 'success' as const },
    { icon: 'DataLine', label: '月折旧额(元)', value: s.thisMonthNewValue ? formatYuan(s.thisMonthNewValue) : '--', colorType: 'warning' as const },
  ]
})

/** Tab2 折旧分析统计卡片（页面级展示） */
const depreciationStats = computed(() => {
  const list = depreciationList.value
  const totalDepreciation = list.reduce((sum, r) => sum + r.depreciationAmount, 0)
  const maxAccumulated = list.length > 0 ? Math.max(...list.map(r => r.accumulatedDepreciation)) : 0
  const assetCount = new Set(list.map(r => r.assetId)).size
  const periodCount = new Set(list.map(r => r.period)).size
  return [
    { icon: 'DataLine', label: '本期折旧额(元)', value: formatYuan(totalDepreciation), colorType: 'primary' as const },
    { icon: 'Wallet', label: '累计折旧(元)', value: formatYuan(maxAccumulated), colorType: 'warning' as const },
    { icon: 'Grid', label: '折旧资产数', value: String(assetCount), colorType: 'info' as const },
    { icon: 'Calendar', label: '折旧期间数', value: String(periodCount), colorType: 'success' as const },
  ]
})

/** Tab3 维修统计卡片（页面级展示） */
const maintenanceStats = computed(() => {
  const summary = maintenanceSummary.value
  return [
    { icon: 'SetUp', label: '维修总数', value: String(summary.totalCount), colorType: 'primary' as const },
    { icon: 'Wallet', label: '维修总费用(元)', value: formatYuan(summary.totalCost), colorType: 'warning' as const },
    { icon: 'DataLine', label: '平均费用(元)', value: formatYuan(summary.avgCost), colorType: 'info' as const },
  ]
})

/** 当前激活Tab对应的统计卡片数据（页面级展示） */
const currentPageStats = computed(() => {
  if (activeTab.value === 'overview') return overviewStats.value
  if (activeTab.value === 'depreciation') return depreciationStats.value
  if (activeTab.value === 'maintenance') return maintenanceStats.value
  return []
})

/* ===== 分类分布表格数据 ===== */
const categoryTableData = computed(() => {
  const totalValue = categoryList.value.reduce((sum, c) => sum + (c.totalValue || 0), 0)
  return categoryList.value
    .filter(c => !c.parentId || c.parentId === '0')
    .map(c => ({
      id: c.id,
      name: c.name,
      assetCount: c.assetCount || 0,
      totalValue: c.totalValue || 0,
      percentage: totalValue > 0 ? Number(((c.totalValue || 0) / totalValue * 100).toFixed(1)) : 0,
    }))
})

const categoryColumns: DataTableColumn[] = [
  { prop: 'name', label: '分类名称', minWidth: 140 },
  { prop: 'assetCount', label: '资产数量', minWidth: 100, align: 'right' },
  { prop: 'totalValue', label: '原值合计(元)', minWidth: 140, align: 'right', slot: 'totalValue' },
  { prop: 'percentage', label: '占比(%)', minWidth: 100, align: 'right', slot: 'percentage' },
]

/* ===== 状态分布饼图数据 ===== */
const statusPieData = computed(() => {
  const s = statistics.value
  if (!s) return []
  return [
    { name: '在用', value: s.activeCount },
    { name: '闲置', value: s.idleCount },
    { name: '维修中', value: s.maintenanceCount },
    { name: '待处置', value: s.toBeDisposedCount },
    { name: '已处置', value: s.disposedCount },
    { name: '已报废', value: s.scrappedCount },
    { name: '已调拨', value: s.transferredCount },
  ].filter(d => d.value > 0)
})

/* ===== 分类价值柱状图数据 ===== */
const categoryBarData = computed(() =>
  categoryList.value
    .filter(c => !c.parentId || c.parentId === '0')
    .map(c => ({ name: c.name, value: c.totalValue || 0 }))
    .filter(d => d.value > 0),
)

/* ===== 折旧分析 - 按期间汇总 ===== */
const depreciationByPeriod = computed(() => {
  const periodMap = new Map<string, { period: string; assetCount: number; depreciation: number; accumulated: number }>()
  for (const record of depreciationList.value) {
    const existing = periodMap.get(record.period)
    if (existing) {
      existing.assetCount += 1
      existing.depreciation += record.depreciationAmount
      existing.accumulated = Math.max(existing.accumulated, record.accumulatedDepreciation)
    } else {
      periodMap.set(record.period, {
        period: record.period,
        assetCount: 1,
        depreciation: record.depreciationAmount,
        accumulated: record.accumulatedDepreciation,
      })
    }
  }
  return Array.from(periodMap.values()).sort((a, b) => a.period.localeCompare(b.period))
})

const depreciationColumns: DataTableColumn[] = [
  { prop: 'period', label: '期间', minWidth: 120, align: 'center' },
  { prop: 'assetCount', label: '折旧资产数', minWidth: 120, align: 'right' },
  { prop: 'depreciation', label: '本期折旧额(元)', minWidth: 150, align: 'right', slot: 'depreciation' },
  { prop: 'accumulated', label: '累计折旧(元)', minWidth: 150, align: 'right', slot: 'accumulated' },
]

/* ===== 折旧方法分布 ===== */
const depreciationMethodDist = computed(() => {
  const methodMap = new Map<string, number>()
  for (const record of depreciationList.value) {
    const label = getMethodLabel(record.method)
    methodMap.set(label, (methodMap.get(label) || 0) + 1)
  }
  return Array.from(methodMap.entries()).map(([name, count]) => ({ name, count }))
})

/* ===== 月度折旧趋势（最近6个月） ===== */
const depreciationTrendData = computed(() => {
  return depreciationByPeriod.value.slice(-6)
})

/* ===== 维修统计汇总 ===== */
const maintenanceSummary = computed(() => {
  const list = maintenanceList.value
  const totalCost = list.reduce((sum, m) => sum + m.repairCost, 0)
  return {
    totalCount: list.length,
    totalCost,
    avgCost: list.length > 0 ? Math.round(totalCost / list.length) : 0,
  }
})

/* ===== 维修按类型统计 ===== */
const maintenanceByType = computed(() => {
  const typeMap = new Map<string, { type: string; count: number; cost: number }>()
  for (const m of maintenanceList.value) {
    const label = m.repairType === 'internal' ? '内部维修' : '外部送修'
    const existing = typeMap.get(label)
    if (existing) {
      existing.count += 1
      existing.cost += m.repairCost
    } else {
      typeMap.set(label, { type: label, count: 1, cost: m.repairCost })
    }
  }
  return Array.from(typeMap.values())
})

const maintenanceTypeColumns: DataTableColumn[] = [
  { prop: 'type', label: '维修类型', minWidth: 120 },
  { prop: 'count', label: '维修次数', minWidth: 100, align: 'right' },
  { prop: 'cost', label: '维修费用(元)', minWidth: 140, align: 'right', slot: 'cost' },
]

/* ===== 维修状态分布 ===== */
const maintenanceStatusDist = computed(() => {
  const statusMap = new Map<MaintenanceStatus, number>()
  for (const m of maintenanceList.value) {
    statusMap.set(m.status, (statusMap.get(m.status) || 0) + 1)
  }
  return Array.from(statusMap.entries()).map(([statusKey, count]) => ({
    statusKey,
    ...getMaintenanceStatusInfo(statusKey),
    count,
  }))
})

/* ===== 最近维修记录（前10条） ===== */
const recentMaintenance = computed(() => {
  return [...maintenanceList.value]
    .sort((a, b) => b.createTime.localeCompare(a.createTime))
    .slice(0, 10)
})

const recentMaintenanceColumns: DataTableColumn[] = [
  { prop: 'assetName', label: '资产名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'faultDescription', label: '故障描述', minWidth: 200, showOverflowTooltip: true },
  { prop: 'repairType', label: '维修类型', minWidth: 100, slot: 'repairType' },
  { prop: 'repairCost', label: '维修费用(元)', minWidth: 120, align: 'right', slot: 'repairCost' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'createTime', label: '报修时间', minWidth: 160 },
]

/* ===== 图表初始化 ===== */
function initStatusPieChart() {
  if (!statusPieRef.value) return
  const rect = statusPieRef.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    setTimeout(() => initStatusPieChart(), 200)
    return
  }
  if (statusPieChart) statusPieChart.dispose()
  statusPieChart = echarts.init(statusPieRef.value)

  const palette = [
    getCssVar('--fts-success') || '#008000',
    getCssVar('--fts-warning') || '#cc7a00',
    getCssVar('--fts-info') || '#0052cc',
    getCssVar('--fts-error') || '#cc0000',
    getCssVar('--fts-primary') || '#0052cc',
    '#8b5cf6',
    '#14b8a6',
  ]

  statusPieChart.setOption({
    tooltip: {
      trigger: 'item',
      transitionDuration: 0,
      confine: true,
      showDelay: 0,
      extraCssText: 'transition: none !important;',
      formatter(param: { name: string; value: number; percent: number }) {
        return `${param.name}<br/>数量: ${param.value}<br/>占比: ${param.percent}%`
      },
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { fontSize: 12, color: getChartTextColor() },
    },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: getChartBorderColor(), borderWidth: 2 },
        label: { show: false, color: getChartTextColor() },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: getChartTextColor() } },
        data: statusPieData.value.map((d, i) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: palette[i % palette.length] },
        })),
      },
    ],
  })
}

function initCategoryBarChart() {
  if (!categoryBarRef.value) return
  const rect = categoryBarRef.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    setTimeout(() => initCategoryBarChart(), 200)
    return
  }
  if (categoryBarChart) categoryBarChart.dispose()
  categoryBarChart = echarts.init(categoryBarRef.value)

  const primaryColor = getCssVar('--fts-primary') || '#0052cc'

  categoryBarChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      transitionDuration: 0,
      confine: true,
      showDelay: 0,
      extraCssText: 'transition: none !important;',
      formatter(params: { name: string; value: number }[]) {
        const item = params[0]
        return `${item.name}<br/>原值: ¥${formatYuan(item.value)}`
      },
    },
    grid: { left: 140, right: 40, top: 20, bottom: 20 },
    xAxis: {
      type: 'value',
      name: '原值(元)',
      axisLabel: { fontSize: 12, color: getChartTextColor() },
    },
    yAxis: {
      type: 'category',
      data: categoryBarData.value.map(d => d.name).reverse(),
      axisLabel: { fontSize: 12, color: getChartTextColor(), width: 120, overflow: 'truncate' },
    },
    series: [
      {
        type: 'bar',
        data: categoryBarData.value.map(d => d.value).reverse(),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: primaryColor },
            { offset: 1, color: getCssVar('--fts-primary-hover') || '#0066ff' },
          ]),
          borderRadius: [0, 4, 4, 0],
        },
        barWidth: '50%',
      },
    ],
  })
}

function initDepreciationTrendChart() {
  if (!depreciationTrendRef.value) return
  const rect = depreciationTrendRef.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) {
    setTimeout(() => initDepreciationTrendChart(), 200)
    return
  }
  if (depreciationTrendChart) depreciationTrendChart.dispose()
  depreciationTrendChart = echarts.init(depreciationTrendRef.value)

  const primaryColor = getCssVar('--fts-primary') || '#0052cc'

  depreciationTrendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      transitionDuration: 0,
      confine: true,
      showDelay: 0,
      extraCssText: 'transition: none !important;',
      formatter(params: { name: string; value: number }[]) {
        const item = params[0]
        return `${item.name}<br/>折旧额: ¥${formatYuan(item.value)}`
      },
    },
    grid: { left: 80, right: 30, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: depreciationTrendData.value.map(d => d.period),
      axisLabel: { fontSize: 12, color: getChartTextColor() },
    },
    yAxis: {
      type: 'value',
      name: '折旧额(元)',
      axisLabel: {
        fontSize: 12,
        color: getChartTextColor(),
        formatter(val: number) {
          return val >= 10000 ? `${(val / 10000).toFixed(0)}万` : String(val)
        },
      },
    },
    series: [
      {
        type: 'line',
        data: depreciationTrendData.value.map(d => d.depreciation),
        smooth: true,
        itemStyle: { color: primaryColor },
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.1 },
      },
    ],
  })
}

function initOverviewCharts() {
  nextTick(() => {
    setTimeout(() => {
      initStatusPieChart()
      initCategoryBarChart()
    }, 300)
  })
}

function initDepreciationCharts() {
  nextTick(() => {
    setTimeout(() => {
      initDepreciationTrendChart()
    }, 300)
  })
}

function disposeAllCharts() {
  statusPieChart?.dispose()
  categoryBarChart?.dispose()
  depreciationTrendChart?.dispose()
  statusPieChart = null
  categoryBarChart = null
  depreciationTrendChart = null
}

function handleResize() {
  statusPieChart?.resize()
  categoryBarChart?.resize()
  depreciationTrendChart?.resize()
}

/* ===== Tab切换处理 ===== */
function handleTabChange(tabName: string | number) {
  const name = String(tabName)
  if (name === 'overview') {
    initOverviewCharts()
  } else if (name === 'depreciation') {
    initDepreciationCharts()
  }
}

/* ===== 数据加载 ===== */
async function loadOverviewData() {
  loading.value = true
  try {
    const [statsRes, categories] = await Promise.all([
      assetApi.getStatisticsVO(),
      categoryApi.getList(),
    ])
    statistics.value = statsRes
    categoryList.value = categories
    initOverviewCharts()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载资产总览数据失败')
    }
  } finally {
    loading.value = false
  }
}

async function loadDepreciationData() {
  loading.value = true
  try {
    const res = await depreciationApi.getList()
    depreciationList.value = res.records
    initDepreciationCharts()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载折旧数据失败')
    }
  } finally {
    loading.value = false
  }
}

async function loadMaintenanceData() {
  loading.value = true
  try {
    const res = await maintenanceApi.getList()
    maintenanceList.value = res.records
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载维修数据失败')
    }
  } finally {
    loading.value = false
  }
}

/* ===== 生命周期 ===== */
onMounted(() => {
  loadOverviewData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeAllCharts()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="资产报表" description="固定资产数据统计与分析" />

    <!-- 页面级统计卡片（依据当前Tab动态切换） -->
    <section class="stats-section">
      <StatCard
        v-for="stat in currentPageStats"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <el-tabs v-model="activeTab" class="page-tabs" @tab-change="handleTabChange">
      <!-- ===== Tab 1: 资产总览 ===== -->
      <el-tab-pane label="资产总览" name="overview">
        <div v-loading="loading" class="tab-content">
          <!-- 分类分布表格 -->
          <div class="section-block">
            <h3 class="section-block__title">分类分布</h3>
            <DataTable :columns="categoryColumns" :data="categoryTableData" stripe>
              <template #totalValue="{ row }">
                {{ formatYuan(row.totalValue) }}
              </template>
              <template #percentage="{ row }">
                {{ row.percentage }}%
              </template>
            </DataTable>
          </div>

          <!-- 图表区域 -->
          <div class="charts-row">
            <!-- 状态分布饼图 -->
            <div class="chart-card">
              <h4 class="chart-card__title">状态分布</h4>
              <div ref="statusPieRef" class="chart-area"></div>
            </div>
            <!-- 分类价值柱状图 -->
            <div class="chart-card">
              <h4 class="chart-card__title">分类原值分布</h4>
              <div ref="categoryBarRef" class="chart-area"></div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== Tab 2: 折旧分析 ===== -->
      <el-tab-pane label="折旧分析" name="depreciation">
        <div v-loading="loading" class="tab-content">
          <!-- 折旧方法分布 -->
          <div v-if="depreciationMethodDist.length" class="method-dist">
            <span class="method-dist__label">折旧方法分布：</span>
            <span v-for="item in depreciationMethodDist" :key="item.name" class="method-dist__item">
              {{ item.name }} <strong>{{ item.count }}</strong> 条
            </span>
          </div>

          <!-- 折旧趋势图 -->
          <div class="section-block">
            <h3 class="section-block__title">月度折旧趋势（近6期）</h3>
            <div ref="depreciationTrendRef" class="chart-area chart-area--wide"></div>
          </div>

          <!-- 折旧汇总表格 -->
          <div class="section-block">
            <h3 class="section-block__title">折旧期间汇总</h3>
            <DataTable :columns="depreciationColumns" :data="depreciationByPeriod" stripe>
              <template #depreciation="{ row }">
                {{ formatYuan(row.depreciation) }}
              </template>
              <template #accumulated="{ row }">
                {{ formatYuan(row.accumulated) }}
              </template>
            </DataTable>
          </div>
        </div>
      </el-tab-pane>

      <!-- ===== Tab 3: 维修统计 ===== -->
      <el-tab-pane label="维修统计" name="maintenance">
        <div v-loading="loading" class="tab-content">
          <!-- 维修类型与状态分布 -->
          <div class="charts-row">
            <!-- 维修类型表格 -->
            <div class="chart-card chart-card--table">
              <h4 class="chart-card__title">维修类型统计</h4>
              <DataTable :columns="maintenanceTypeColumns" :data="maintenanceByType" stripe size="small">
                <template #cost="{ row }">
                  {{ formatYuan(row.cost) }}
                </template>
              </DataTable>
            </div>
            <!-- 维修状态分布 -->
            <div class="chart-card">
              <h4 class="chart-card__title">维修状态分布</h4>
              <div class="status-dist-list">
                <div v-for="item in maintenanceStatusDist" :key="item.status" class="status-dist-item">
                  <StatusTag :status="item.status" :label="item.label" size="small" />
                  <span class="status-dist-item__count">{{ item.count }} 次</span>
                </div>
                <el-empty v-if="!maintenanceStatusDist.length" description="暂无数据" :image-size="60" />
              </div>
            </div>
          </div>

          <!-- 最近维修记录 -->
          <div class="section-block">
            <h3 class="section-block__title">最近维修记录</h3>
            <DataTable :columns="recentMaintenanceColumns" :data="recentMaintenance" stripe>
              <template #repairType="{ row }">
                {{ row.repairType === 'internal' ? '内部维修' : '外部送修' }}
              </template>
              <template #repairCost="{ row }">
                {{ formatYuan(row.repairCost) }}
              </template>
              <template #status="{ row }">
                <StatusTag :status="getMaintenanceStatusInfo(row.status).status" :label="getMaintenanceStatusInfo(row.status).label" size="small" />
              </template>
            </DataTable>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.tab-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
}

.section-block {
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
}

.section-block__title {
  margin: 0 0 var(--fts-space-3);
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-4);
}

.chart-card {
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  min-width: 0;
  overflow: hidden;
}

.chart-card--table {
  :deep(.el-table) {
    width: 100%;
  }
}

.chart-card__title {
  margin: 0 0 var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-secondary);
}

.chart-area {
  width: 100%;
  height: 320px;
  min-width: 0;
}

.chart-area--wide {
  height: 360px;
}

.method-dist {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
}

.method-dist__label {
  color: var(--fts-text-secondary);
}

.method-dist__item {
  color: var(--fts-text-primary);

  strong {
    color: var(--fts-primary);
  }
}

.status-dist-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) 0;
}

.status-dist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-sm);
}

.status-dist-item__count {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}
</style>
