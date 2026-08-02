<script setup lang="ts">
/**
 * 报表图表组件（管理层视角 - 精简版）
 *
 * 功能：
 * - 营收趋势折线图（双Y轴：左轴营收、右轴毛利率）
 * - 门店排名水平条形图
 * - ECharts失败时降级显示数据表格
 */
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import logger from '@/utils/logger'
import type { ChartData } from '../types/report'

interface Props {
  data: ChartData | null
  loading?: boolean
  reportType?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  reportType: 3
})

const chartRef = ref<HTMLElement | null>(null)
const chartInstance = ref<echarts.ECharts | null>(null)
const chartError = ref(false)

// 从CSS变量获取颜色
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || '#2563EB'
}

// 趋势图配置（双Y轴）
function getTrendOption(data: ChartData): EChartsOption {
  const trendData = data.trendData || []
  const dates = trendData.map(d => d.date)
  const revenues = trendData.map(d => d.revenue)
  const grossRates = trendData.map(d => d.grossRate)

  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['营收', '毛利率'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '10%',
     
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: getCssVar('--fts-border') } },
      axisLabel: { color: getCssVar('--fts-text-secondary') }
    },
    yAxis: [
      {
        type: 'value',
        name: '营收（元）',
        position: 'left',
        axisLine: { show: true, lineStyle: { color: primaryColor } },
        axisLabel: { color: primaryColor },
        splitLine: { lineStyle: { color: getCssVar('--fts-border'), type: 'dashed' } }
      },
      {
        type: 'value',
        name: '毛利率（%）',
        position: 'right',
        min: 0,
        max: 100,
        axisLine: { show: true, lineStyle: { color: successColor } },
        axisLabel: { color: successColor, formatter: '{value}%' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '营收',
        type: 'line',
        data: revenues,
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
              { offset: 1, color: primaryColor + '05' }
            ]
          }
        },
        markPoint: {
          data: [
            { type: 'max', name: '最高' },
            { type: 'min', name: '最低' }
          ]
        }
      },
      {
        name: '毛利率',
        type: 'line',
        yAxisIndex: 1,
        data: grossRates,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: successColor, type: 'dashed' },
        itemStyle: { color: successColor }
      }
    ]
  }
}

// 门店排名条形图
function getRankOption(data: ChartData): EChartsOption {
  const rankData = data.storeRank || []
  const names = rankData.map(d => d.name)
  const values = rankData.map(d => d.value)

  const primaryColor = getCssVar('--fts-primary')

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '3%',
     
    },
    xAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: getCssVar('--fts-border') } },
      axisLabel: { color: getCssVar('--fts-text-secondary') },
      splitLine: { lineStyle: { color: getCssVar('--fts-border'), type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: names.reverse(),
      axisLine: { lineStyle: { color: getCssVar('--fts-border') } },
      axisLabel: { color: getCssVar('--fts-text-secondary') }
    },
    series: [
      {
        type: 'bar',
        data: values.reverse(),
        itemStyle: {
          color: primaryColor,
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: unknown) => {
            const val = (params as { value: number }).value
            return '¥' + (val / 100).toLocaleString()
          }
        }
      }
    ]
  }
}

function initChart(): void {
  if (!chartRef.value || !props.data) return

  try {
    if (chartInstance.value) {
      chartInstance.value.dispose()
    }

    chartInstance.value = echarts.init(chartRef.value)
    const option = getTrendOption(props.data)
    chartInstance.value.setOption(option)
    chartError.value = false
  } catch (error: unknown) {
    logger.error('REPORT_CHART', `图表初始化失败: ${error instanceof Error ? error.message : String(error)}`)
    chartError.value = true
  }
}

function handleResize(): void {
  chartInstance.value?.resize()
}

watch(() => props.data, (newData) => {
  if (newData) {
    initChart()
  }
}, { deep: true })

onMounted(() => {
  if (props.data) {
    initChart()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance.value?.dispose()
})
</script>

<template>
  <div class="report-chart">
    <!-- 加载状态 -->
    <div v-if="loading" class="chart-loading">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="!data" class="chart-empty">
      <el-empty description="暂无数据" />
    </div>

    <!-- 图表错误降级 -->
    <div v-else-if="chartError" class="chart-fallback">
      <el-alert
        title="图表加载失败"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>图表组件加载失败，以下为数据概览：</p>
          <el-table v-if="data.trendData" :data="data.trendData.slice(0, 10)" size="small">
            <el-table-column prop="date" label="日期" />
            <el-table-column prop="revenue" label="营收">
              <template #default="{ row }">
                ¥{{ (row.revenue / 100).toLocaleString() }}
              </template>
            </el-table-column>
            <el-table-column prop="grossRate" label="毛利率">
              <template #default="{ row }">
                {{ row.grossRate }}%
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-alert>
    </div>

    <!-- 正常图表 -->
    <div v-else ref="chartRef" class="chart-container" />
  </div>
</template>

<style scoped lang="scss">
.report-chart {
  width: 100%;
  min-height: 350px;
}

.chart-loading {
  padding: var(--fts-space-4);
}

.chart-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 350px;
}

.chart-fallback {
  padding: var(--fts-space-4);
}

.chart-container {
  width: 100%;
  height: 350px;
}

@media (max-width: 768px) {
  .report-chart {
    min-height: 280px;
  }

  .chart-empty {
    min-height: 280px;
  }

  .chart-container {
    height: 280px;
  }
}
</style>
