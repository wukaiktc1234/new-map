<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { DataAnalysis, Timer, SuccessFilled, Warning, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer, FilterTabs, StatCard, SectionHeader, EmptyState } from '@/components/core'
import type { FilterTabOption } from '@/components/core'
import { UI_DELAY_SHORT } from '@/config/timing'

// ========== 时间范围筛选 ==========
type RangeKey = 'month' | 'quarter' | 'year' | 'custom'
const activeRange = ref<RangeKey>('month')
const dateRange = ref<[string, string]>([getMonthStart(), getMonthEnd()])

// 自定义模式：年/月起止选择
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)
const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1)
const customYear = ref(currentYear)
const customMonthStart = ref(new Date().getMonth() + 1)
const customMonthEnd = ref(new Date().getMonth() + 1)

/** 根据当前时间范围生成可读的日期提示文本 */
const dateHintText = computed(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  switch (activeRange.value) {
    case 'month': return `${y}年${String(m).padStart(2, '0')}月`
    case 'quarter': {
      const qStart = Math.floor((m - 1) / 3) * 3 + 1
      const qEnd = qStart + 2
      return `${y}年${String(qStart).padStart(2, '0')}月 —— ${String(qEnd).padStart(2, '0')}月`
    }
    case 'year': return `${y}年01月 —— 12月`
    case 'custom': {
      const ys = customYear.value
      const ms = String(customMonthStart.value).padStart(2, '0')
      const me = String(customMonthEnd.value).padStart(2, '0')
      return ms === me ? `${ys}年${ms}月` : `${ys}年${ms}月 —— ${me}月`
    }
  }
})

/** 自定义模式切换时同步更新 dateRange */
watch([customYear, customMonthStart, customMonthEnd], () => {
  if (activeRange.value !== 'custom') return
  const y = customYear.value
  const ms = customMonthStart.value
  const me = customMonthEnd.value
  const lastDay = new Date(y, me, 0).getDate()
  dateRange.value = [`${y}-${String(ms).padStart(2, '0')}-01`, `${y}-${String(me).padStart(2, '0')}-${lastDay}`]
})

const rangeOptions: { key: RangeKey; label: string }[] = [
  { key: 'month', label: '本月' }, { key: 'quarter', label: '本季' },
  { key: 'year', label: '本年' }, { key: 'custom', label: '自定义' },
]

/** FilterTabs 组件所需的选项格式 */
const periodOptions = computed<FilterTabOption[]>(() =>
  rangeOptions.map(opt => ({ value: opt.key, label: opt.label }))
)

const compareLabelMap: Record<RangeKey, string> = {
  month: 'vs 上月', quarter: 'vs 上季', year: 'vs 上年', custom: 'vs 上期',
}

function selectRange(key: RangeKey | string | number) {
  activeRange.value = key as RangeKey
  const now = new Date()
  switch (key) {
    case 'month': dateRange.value = [getMonthStart(), getMonthEnd()]; break
    case 'quarter': {
      const qStart = new Date(now.getFullYear(), Math.floor(now.getMonth() / 3) * 3, 1)
      dateRange.value = [formatDate(qStart), formatDate(new Date(now.getFullYear(), qStart.getMonth() + 3, 0))]
      break
    }
    case 'year': dateRange.value = [`${now.getFullYear()}-01-01`, `${now.getFullYear()}-12-31`]; break
    case 'custom': {
      // 切换到自定义时，默认为当前月
      const m = now.getMonth() + 1
      customYear.value = now.getFullYear()
      customMonthStart.value = m
      customMonthEnd.value = m
      dateRange.value = [getMonthStart(), getMonthEnd()]
      break
    }
  }
}

function getMonthStart(): string {
  const d = new Date()
  return formatDate(new Date(d.getFullYear(), d.getMonth(), 1))
}
function getMonthEnd(): string {
  const d = new Date()
  return formatDate(new Date(d.getFullYear(), d.getMonth() + 1, 0))
}
function formatDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// ========== 不同时间范围的指标数据 ==========
interface PeriodMetrics {
  attendance: string
  workHours: string
  punctuality: string
  overtime: string
  attendanceTrend: string
  workHoursTrend: string
  punctualityTrend: string
  overtimeTrend: string
}

const periodMetricsMap: Record<string, PeriodMetrics> = {
  month: {
    attendance: '22天', workHours: '176h', punctuality: '96.5%', overtime: '12h',
    attendanceTrend: '+2天', workHoursTrend: '+8h', punctualityTrend: '+1.2%', overtimeTrend: '+3h',
  },
  quarter: {
    attendance: '64天', workHours: '512h', punctuality: '94.2%', overtime: '38h',
    attendanceTrend: '+3天', workHoursTrend: '+24h', punctualityTrend: '-0.8%', overtimeTrend: '+12h',
  },
  year: {
    attendance: '250天', workHours: '2000h', punctuality: '93.8%', overtime: '156h',
    attendanceTrend: '+5天', workHoursTrend: '+40h', punctualityTrend: '-1.5%', overtimeTrend: '+28h',
  },
  custom: {
    attendance: '22天', workHours: '176h', punctuality: '96.5%', overtime: '12h',
    attendanceTrend: '+2天', workHoursTrend: '+8h', punctualityTrend: '+1.2%', overtimeTrend: '+3h',
  },
}

const currentMetrics = computed(() => periodMetricsMap[activeRange.value] || periodMetricsMap.month)

// ========== 核心指标卡片 ==========
interface StatCardItem {
  icon: typeof DataAnalysis
  theme: 'primary' | 'success' | 'warning'
  title: string
  value: string
  trend: string
  trendSemantic: 'good' | 'bad' | 'neutral'
}

function parseTrendDirection(trend: string): 'up' | 'down' | 'neutral' {
  if (trend.startsWith('+')) return 'up'
  if (trend.startsWith('-')) return 'down'
  return 'neutral'
}

const statCards = computed<StatCardItem[]>(() => {
  const m = currentMetrics.value
  return [
    { icon: DataAnalysis, theme: 'primary', title: '出勤天数', value: m.attendance, trend: m.attendanceTrend, trendSemantic: 'good' },
    { icon: Timer, theme: 'success', title: '总工作时长', value: m.workHours, trend: m.workHoursTrend, trendSemantic: 'neutral' },
    { icon: SuccessFilled, theme: 'primary', title: '出勤准时率', value: m.punctuality, trend: m.punctualityTrend, trendSemantic: 'good' },
    { icon: Warning, theme: 'warning', title: '累计加班', value: m.overtime, trend: m.overtimeTrend, trendSemantic: 'bad' },
  ]
})

// ========== 考勤热力图（固定Mock数据，按日期确定性生成） ==========
interface HeatmapDay { date: number; dayOfWeek: number; hours: number; shift: 'morning' | 'afternoon' | 'evening' | 'rest'; isToday: boolean }

const heatmapDays = computed<HeatmapDay[]>(() => {
  const now = new Date(), year = now.getFullYear(), month = now.getMonth(), todayDate = now.getDate()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const result: HeatmapDay[] = []
  const shiftCycle: HeatmapDay['shift'][] = ['morning', 'afternoon', 'evening', 'morning', 'afternoon', 'morning', 'evening']
  for (let d = 1; d <= daysInMonth; d++) {
    const dow = new Date(year, month, d).getDay()
    let shift: HeatmapDay['shift']; let hours: number
    if (dow === 0) {
      shift = 'rest'; hours = 0
    } else if (dow === 6) {
      const weekNum = Math.ceil(d / 7)
      if (weekNum % 2 === 0) { shift = 'morning'; hours = 5 }
      else { shift = 'rest'; hours = 0 }
    } else {
      shift = shiftCycle[(d - 1) % shiftCycle.length]
      hours = shift === 'evening' ? 9 : 8
    }
    result.push({ date: d, dayOfWeek: dow, hours, shift, isToday: d === todayDate })
  }
  return result
})

const heatmapWeeks = computed(() => {
  const weeks: HeatmapDay[][] = []; let currentWeek: HeatmapDay[] = []
  for (const day of heatmapDays.value) {
    if (day.dayOfWeek === 1 && currentWeek.length > 0) { weeks.push(currentWeek); currentWeek = [] }
    currentWeek.push(day)
  }
  if (currentWeek.length > 0) weeks.push(currentWeek)
  return weeks
})

function getHeatColor(hours: number): string {
  if (hours === 0) return 'var(--fts-bg-tertiary)'
  if (hours <= 5) return 'rgba(var(--fts-success-rgb), 0.20)'
  if (hours <= 7) return 'rgba(var(--fts-success-rgb), 0.45)'
  return 'rgba(var(--fts-success-rgb), 0.75)'
}

// ========== 考勤趋势折线图（替代色块热力图） ==========
interface LinePoint { x: number; y: number; hours: number; isToday: boolean }

/** SVG 坐标系：绘图区 x:[28,312] y:[12,108]，对应 hours:[0,10] */
const CHART = { left: 28, right: 312, top: 12, bottom: 108, maxH: 10 } as const

const lineChartPoints = computed<LinePoint[]>(() => {
  const days = heatmapDays.value
  const n = days.length
  if (n === 0) return []
  const xStep = (CHART.right - CHART.left) / Math.max(n - 1, 1)
  const yRange = CHART.bottom - CHART.top // 96px 对应 0-10h

  return days.map((d, i) => ({
    x: CHART.left + i * xStep,
    y: d.hours > 0 ? CHART.bottom - (d.hours / CHART.maxH) * yRange : CHART.bottom,
    hours: d.hours,
    isToday: d.isToday,
  }))
})

/** 折线 polyline 的 points 字符串 */
const linePointsStr = computed(() =>
  lineChartPoints.value.filter(p => p.hours > 0).map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
)

/** 面积填充 path（闭合到基线） */
const lineAreaPath = computed(() => {
  const pts = lineChartPoints.value.filter(p => p.hours > 0)
  if (pts.length < 2) return ''
  const linePart = pts.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
  return `M ${pts[0].x.toFixed(1)},${CHART.bottom} L ${linePart} L ${pts[pts.length - 1].x.toFixed(1)},${CHART.bottom} Z`
})

/** X轴标签（每周一显示日期） */
const xAxisLabels = computed(() => {
  const days = heatmapDays.value
  const labels: { x: number; text: string }[] = []
  const n = days.length
  const xStep = (CHART.right - CHART.left) / Math.max(n - 1, 1)
  // 每 7 天显示一次，加上最后一天
  for (let i = 0; i < n; i += 7) {
    labels.push({ x: CHART.left + i * xStep, text: `${days[i].date}` })
  }
  // 确保最后一天显示
  const last = days[n - 1]
  if (!labels.some(l => l.text === `${last.date}`)) {
    labels.push({ x: CHART.right, text: `${last.date}` })
  }
  return labels
})

// ========== 排班统计（Mock） ==========
interface ShiftDistItem { label: string; percent: number; count: number; dotClass: string }
const shiftDistribution = ref<ShiftDistItem[]>([
  { label: '早班', percent: 35, count: 11, dotClass: 'sd--m' }, { label: '中班', percent: 28, count: 9, dotClass: 'sd--a' },
  { label: '晚班', percent: 22, count: 7, dotClass: 'sd--e' }, { label: '休息', percent: 15, count: 5, dotClass: 'sd--r' },
])

interface WeekOverviewDay { day: string; dateStr: string; shift: string; status: 'completed' | 'today' | 'upcoming' | 'rest' }
const weekOverview = ref<WeekOverviewDay[]>([
  { day: '一', dateStr: '20', shift: '早班', status: 'completed' }, { day: '二', dateStr: '21', shift: '中班', status: 'completed' },
  { day: '三', dateStr: '22', shift: '休', status: 'rest' }, { day: '四', dateStr: '23', shift: '晚班', status: 'today' },
  { day: '五', dateStr: '24', shift: '早班', status: 'upcoming' }, { day: '六', dateStr: '25', shift: '中班', status: 'upcoming' },
  { day: '日', dateStr: '26', shift: '休', status: 'rest' },
])

/** 将 weekOverview status 映射为 LeavePage 一致的 att-cell 样式类 */
function weekStatusClass(day: WeekOverviewDay): string {
  switch (day.status) {
    case 'today': return 'att-cell--today'
    case 'rest': return 'att-cell--rest'
    default: return day.shift === '休' ? 'att-cell--rest' : 'att-cell--normal'
  }
}

// ========== 工资趋势（Mock近6个月） ==========
interface SalaryTrendItem { month: string; amount: number; isCurrent: boolean }
const salaryTrend = ref<SalaryTrendItem[]>([
  { month: '12月', amount: 5200, isCurrent: false }, { month: '1月', amount: 5580, isCurrent: false },
  { month: '2月', amount: 5400, isCurrent: false }, { month: '3月', amount: 6020, isCurrent: false },
  { month: '4月', amount: 5650, isCurrent: false }, { month: '5月', amount: 5850, isCurrent: true },
])
const maxSalaryAmount = computed(() => Math.max(...salaryTrend.value.map(i => i.amount)))

const tooltipData = ref<{ x: number; y: number; text: string } | null>(null)
function showTooltip(event: MouseEvent, item: SalaryTrendItem) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  tooltipData.value = { x: rect.left + rect.width / 2, y: rect.top - 8, text: `${item.month}: ¥${item.amount.toLocaleString()}` }
}
function hideTooltip() { tooltipData.value = null }

function findDayByDow(week: HeatmapDay[], dow: number): HeatmapDay | undefined {
  return week.find(d => d.dayOfWeek === dow)
}

// ========== 图表加载骨架屏 ==========
const chartLoading = ref(false)
watch(activeRange, () => {
  chartLoading.value = true
  setTimeout(() => { chartLoading.value = false }, UI_DELAY_SHORT)
})

// ========== 数据导出 ==========
function handleExport() {
  ElMessage.success('报表导出中，请稍后查看下载中心')
}

// ========== 审批概况（Mock） ==========
interface ApprovalSummaryItem { label: string; value: string; icon: string; theme: 'primary' | 'success' | 'warning' }
const approvalSummary = ref<ApprovalSummaryItem[]>([
  { label: '近期提交审批', value: '5', icon: 'document', theme: 'primary' },
  { label: '通过率', value: '80%', icon: 'circle-check', theme: 'success' },
  { label: '平均处理时长', value: '1.2天', icon: 'timer', theme: 'warning' },
])

// ========== 培训进度（Mock） ==========
const trainingProgress = ref({ completed: 8, total: 10, percent: 80 })
</script>

<template>
  <PageContainer title="我的数据">
    <template #headerActions>
      <button class="export-btn" @click="handleExport">
        <el-icon><Download /></el-icon> 导出报表
      </button>
    </template>

    <!-- 时间范围筛选器 -->
    <section class="rp-filter">
      <div class="filter-left">
        <FilterTabs v-model="activeRange" :options="periodOptions" variant="outline" size="small" @change="selectRange" />
      </div>
      <div class="filter-right">
        <!-- 非自定义模式：显示日期提示文本 -->
        <span v-if="activeRange !== 'custom'" class="date-hint">
          {{ dateHintText }}
        </span>
        <!-- 自定义模式：年/月起止下拉选择 -->
        <template v-else>
          <el-select v-model="customYear" size="small" class="custom-select" popper-class="rp-date-popper" :teleported="false">
            <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
          <span class="date-sep">—</span>
          <el-select v-model="customMonthStart" size="small" class="custom-select custom-select--sm" popper-class="rp-date-popper" :teleported="false">
            <el-option v-for="m in monthOptions" :key="m" :label="`${String(m).padStart(2, '0')}月`" :value="m" />
          </el-select>
          <span class="date-sep">至</span>
          <el-select v-model="customMonthEnd" size="small" class="custom-select custom-select--sm" popper-class="rp-date-popper" :teleported="false">
            <el-option v-for="m in monthOptions" :key="m" :label="`${String(m).padStart(2, '0')}月`" :value="m" />
          </el-select>
        </template>
      </div>
    </section>

    <!-- 核心数据卡片（4列Grid） -->
    <section class="rp-cards">
      <div class="cards-grid">
        <StatCard
          v-for="(card, idx) in statCards"
          :key="idx"
          variant="icon-left"
          :value="card.value"
          :label="card.title"
          :icon="card.theme === 'primary' ? '📊' : card.theme === 'success' ? '⏱' : card.theme === 'warning' ? '⚠' : '📈'"
          :trend="card.trend + ' ' + compareLabelMap[activeRange]"
          :trend-type="parseTrendDirection(card.trend) === 'up' ? 'up' : parseTrendDirection(card.trend) === 'down' ? 'down' : undefined"
        />
      </div>
    </section>

    <!-- 考勤趋势折线图（替代色块热力图，提升识别度） -->
    <section class="rp-section rp-section--heatmap">
      <SectionHeader title="考勤趋势" description="本月每日工作时长变化" />
      <div class="chart-content" :class="{ 'chart-content--loading': chartLoading }">
        <div v-if="chartLoading" class="chart-skeleton">
          <div v-for="i in 5" :key="i" class="skeleton-row shimmer"></div>
        </div>
        <template v-else>
          <div class="line-chart-wrapper">
            <svg class="line-chart" viewBox="0 0 320 140" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg">
              <!-- Y轴参考线 -->
              <line x1="28" y1="12" x2="312" y2="12" stroke="var(--fts-border-secondary)" stroke-width="0.5" stroke-dasharray="3,3" opacity="0.5"/>
              <line x1="28" y1="44" x2="312" y2="44" stroke="var(--fts-border-secondary)" stroke-width="0.5" stroke-dasharray="3,3" opacity="0.3"/>
              <line x1="28" y1="76" x2="312" y2="76" stroke="var(--fts-border-secondary)" stroke-width="0.5" stroke-dasharray="3,3" opacity="0.3"/>
              <line x1="28" y1="108" x2="312" y2="108" stroke="var(--fts-border-secondary)" stroke-width="0.5" stroke-dasharray="3,3" opacity="0.3"/>

              <!-- Y轴标签 -->
              <text x="24" y="15" text-anchor="end" fill="var(--fts-text-quaternary)" font-size="8">10h</text>
              <text x="24" y="47" text-anchor="end" fill="var(--fts-text-quaternary)" font-size="8">7h</text>
              <text x="24" y="79" text-anchor="end" fill="var(--fts-text-quaternary)" font-size="8">4h</text>
              <text x="24" y="111" text-anchor="end" fill="var(--fts-text-quaternary)" font-size="8">0h</text>

              <!-- 面积填充（渐变） -->
              <defs>
                <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="var(--fts-primary)" stop-opacity="0.18"/>
                  <stop offset="100%" stop-color="var(--fts-primary)" stop-opacity="0.02"/>
                </linearGradient>
              </defs>
              <path :d="lineAreaPath" fill="url(#areaGrad)"/>

              <!-- 折线 -->
              <polyline :points="linePointsStr" fill="none"
                stroke="var(--fts-primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>

              <!-- 数据点 + 今日高亮 -->
              <template v-for="(pt, i) in lineChartPoints" :key="i">
                <circle v-if="pt.hours > 0"
                  :cx="pt.x" :cy="pt.y" r="3"
                  :fill="pt.isToday ? 'var(--fts-primary)' : 'var(--fts-bg-card)'"
                  :stroke="pt.isToday ? 'none' : 'var(--fts-primary)'" stroke-width="1.2"
                  :class="{ 'lc-dot--today': pt.isToday }"/>
                <!-- 今日标签 -->
                <text v-if="pt.isToday && pt.hours > 0"
                  :x="pt.x" :y="pt.y - 9" text-anchor="middle"
                  fill="var(--fts-primary)" font-size="8" font-weight="600">{{ pt.hours }}h</text>
              </template>

              <!-- X轴基线 -->
              <line x1="28" y1="108" x2="312" y2="108" stroke="var(--fts-border-secondary)" stroke-width="0.8"/>

              <!-- X轴日期标签（每7天显示一次） -->
              <template v-for="(label, i) in xAxisLabels" :key="'xl'+i">
                <text :x="label.x" y="124" text-anchor="middle" fill="var(--fts-text-quaternary)" font-size="8">{{ label.text }}</text>
              </template>
            </svg>
          </div>
          <!-- 图例 -->
          <div class="lc-legend">
            <span class="lc-legend-item"><i class="lc-dot lc-dot--work"></i>工作日</span>
            <span class="lc-legend-item"><i class="lc-dot lc-dot--rest"></i>休息</span>
            <span class="lc-legend-item"><i class="lc-dot lc-dot--today"></i>今日</span>
          </div>
        </template>
      </div>
    </section>

    <!-- 排班统计区（双栏 + 培训进度） -->
    <section class="rp-dual">
      <div class="rp-section rp-dual-col">
        <SectionHeader title="班次分布" />
        <div class="shift-bars">
          <div v-for="(item, idx) in shiftDistribution" :key="idx" class="sb-row">
            <div class="sb-info"><i :class="['sb-dot', item.dotClass]"></i><span class="sb-label">{{ item.label }}</span></div>
            <div class="sb-track"><div :class="['sb-fill', `sb-fill--${item.dotClass.replace('sd--', '')}`]" :style="{ width: `${item.percent}%` }"></div></div>
            <span class="sb-pct">{{ item.percent }}%</span>
          </div>
        </div>
      </div>
      <div class="rp-section rp-dual-col">
        <SectionHeader title="本周概览" action-label="全部 →" @action="$router.push('/schedule')" />
        <!-- 统一使用与 LeavePage 一致的 attendance-row 样式 -->
        <div class="attendance-row">
          <div
            v-for="day in weekOverview"
            :key="day.day"
            :class="['att-cell', weekStatusClass(day)]"
          >
            <span class="att-weekday">{{ day.day }}</span>
            <span class="att-date">{{ day.dateStr }}</span>
            <span class="att-label">{{ day.shift === '休' ? '休息' : day.shift }}</span>
          </div>
        </div>
        <div class="att-legend">
          <span class="att-legend-item"><i class="att-dot att-dot--normal"></i>已上班</span>
          <span class="att-legend-item"><i class="att-dot att-dot--today"></i>今日</span>
          <span class="att-legend-item"><i class="att-dot att-dot--rest"></i>休息</span>
        </div>
      </div>
    </section>

    <!-- 培训进度 -->
    <section class="rp-section rp-section--training">
      <SectionHeader title="培训进度" :description="`已完成 ${trainingProgress.completed}/${trainingProgress.total} 门课程`" />
      <div class="training-body">
        <!-- 纯CSS圆环进度指示器 -->
        <div class="progress-ring" :style="{ '--ring-percent': `${trainingProgress.percent}%` }">
          <div class="progress-ring__track"></div>
          <div class="progress-ring__fill"></div>
          <div class="progress-ring__content">
            <span class="progress-ring__value">{{ trainingProgress.percent }}<small>%</small></span>
            <span class="progress-ring__label">完成度</span>
          </div>
        </div>
        <!-- 培训摘要信息 -->
        <div class="training-info">
          <div class="ti-item ti-item--completed">
            <span class="ti-value">{{ trainingProgress.completed }}</span>
            <span class="ti-label">已完成</span>
          </div>
          <div class="ti-divider"></div>
          <div class="ti-item ti-item--remaining">
            <span class="ti-value">{{ trainingProgress.total - trainingProgress.completed }}</span>
            <span class="ti-label">待学习</span>
          </div>
          <div class="ti-divider"></div>
          <div class="ti-item ti-item--total">
            <span class="ti-value">{{ trainingProgress.total }}</span>
            <span class="ti-label">总课程</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 工资趋势区 -->
    <section class="rp-section rp-section--salary">
      <SectionHeader title="工资趋势" description="近6个月实发工资对比" />
      <div class="chart-content" :class="{ 'chart-content--loading': chartLoading }">
        <div v-if="chartLoading" class="chart-skeleton chart-skeleton--bar">
          <div v-for="i in 6" :key="i" class="skeleton-bar shimmer"></div>
        </div>
        <div v-else class="chart-area">
          <div class="bar-chart">
            <div v-for="item in salaryTrend" :key="item.month"
              :class="['bar-col', { 'bar-col--active': item.isCurrent }]"
              @mouseenter="showTooltip($event, item)" @mouseleave="hideTooltip">
              <div class="bar-track"><div class="bar-fill" :style="{ height: `${(item.amount / maxSalaryAmount) * 100}%` }"></div></div>
              <span class="bar-label">{{ item.month }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 审批概况 -->
    <section class="rp-section rp-section--approval">
      <SectionHeader title="审批概况" description="近期审批数据摘要" />
      <div class="approval-grid">
        <div v-for="(item, idx) in approvalSummary" :key="idx"
          :class="['approval-card', `approval-card--${item.theme}`]">
          <div class="approval-card__icon">
            <el-icon :size="20">
              <component :is="item.icon === 'document' ? DataAnalysis : item.icon === 'circle-check' ? SuccessFilled : Timer" />
            </el-icon>
          </div>
          <div class="approval-card__body">
            <span class="approval-card__value">{{ item.value }}</span>
            <span class="approval-card__label">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 浮动Tooltip -->
    <Teleport to="body">
      <transition name="tooltip-fade">
        <div v-if="tooltipData" class="chart-tooltip"
          :style="{ left: `${tooltipData.x}px`, top: `${tooltipData.y}px` }">{{ tooltipData.text }}</div>
      </transition>
    </Teleport>
  </PageContainer>
</template>

<style scoped lang="scss">
/* ====== 页面容器 & 统一区块卡片 ====== */
.rp-section {
  background: var(--fts-bg-card); border-radius: var(--fts-radius-md); box-shadow: var(--fts-shadow-xs);
  padding: var(--fts-card-padding); margin-bottom: var(--fts-space-4);
  &:last-child { margin-bottom: 0; }
}

/* ====== 导出按钮 ====== */
.export-btn {
  display: inline-flex; align-items: center; gap: var(--fts-space-2);
  padding: var(--fts-space-1) var(--fts-space-3); border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-sm); background: transparent; color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm); cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);
  &:hover { border-color: var(--fts-primary); color: var(--fts-primary); }
  &:active { transform: scale(0.97); }
}

/* ====== 时间范围筛选器 ====== */
.rp-filter {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: var(--fts-space-3);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  box-shadow: var(--fts-shadow-xs);
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-bottom: var(--fts-space-4);

  /* 创建层叠上下文：约束内部 popper 的 z-index 不穿透标题栏 */
  position: relative;

  @media (max-width: 639px) {
    flex-direction: column;
    align-items: stretch;
  }
}

.filter-left {
  display: flex;
  gap: var(--fts-space-2);
  flex-wrap: wrap;

  @media (max-width: 639px) {
    justify-content: center;
  }
}

.filter-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
}

/* 日期提示文本（本月/本季/本年） */
.date-hint {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-primary);
  white-space: nowrap;
  padding: var(--fts-space-1) var(--fts-space-3);
  border-radius: var(--fts-radius-full);
  background: rgba(var(--fts-primary-rgb), 0.06);
}

/* 自定义模式下拉选择器 */
.custom-select {
  width: 90px;

  &--sm {
    width: 72px;
  }

  // 深浅主题适配：使用CSS变量
  :deep(.el-input__wrapper) {
    border-radius: var(--fts-radius-md);
    transition: all var(--fts-duration-fast) var(--fts-easing-default);

    &:hover {
      box-shadow: 0 0 0 1px var(--fts-primary-light, rgba(var(--fts-primary-rgb), 0.4)) inset;
    }
  }
}

.date-sep {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-quaternary);
  user-select: none;
}

/* ====== 核心数据卡片（4列Grid） ====== */
.cards-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
  @media (max-width: 767px) { grid-template-columns: repeat(2, 1fr); }
}
/* ====== 图表加载骨架屏 ====== */
.chart-content { position: relative; min-height: 120px; }
.chart-content--loading { min-height: 200px; }
.chart-skeleton {
  display: flex; flex-direction: column; gap: var(--fts-space-2); padding: var(--fts-space-3) 0;
  &--bar { flex-direction: row; align-items: flex-end; justify-content: space-around; height: 180px; padding: 0; }
}
.skeleton-row {
  height: 32px; border-radius: var(--fts-radius-xs); background: var(--fts-bg-tertiary);
}
.skeleton-bar {
  width: 48px; border-radius: 4px 4px 0 0; background: var(--fts-bg-tertiary);
  &:nth-child(1) { height: 60%; } &:nth-child(2) { height: 80%; } &:nth-child(3) { height: 45%; }
  &:nth-child(4) { height: 90%; } &:nth-child(5) { height: 70%; } &:nth-child(6) { height: 55%; }
}
.shimmer { animation: shimmer 1.5s infinite; background: linear-gradient(90deg, var(--fts-bg-tertiary) 25%, var(--fts-bg-secondary) 50%, var(--fts-bg-tertiary) 75%); background-size: 200% 100%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* ====== 考勤趋势折线图（替代色块热力图） ====== */
.rp-section--heatmap {
  overflow: hidden;

  @media (max-width: 639px) {
    overflow-x: auto;
    &::-webkit-scrollbar { display: none; }
    scrollbar-width: none;
  }
}

.line-chart-wrapper {
  width: 100%;
  min-width: 300px;
}

.line-chart {
  display: block;
  width: 100%;
  height: auto;
  text { font-family: inherit; }
}

.lc-dot--today {
  r: 5;
  filter: drop-shadow(0 0 3px rgba(var(--fts-primary-rgb), 0.5));
}

.lc-legend {
  display: flex; align-items: center; gap: var(--fts-space-4);
  margin-top: var(--fts-space-3); padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
}

.lc-legend-item {
  display: flex; align-items: center; gap: 4px;
  font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary);
}

.lc-dot {
  width: 8px; height: 8px; border-radius: 50%; display: inline-block;
  &--work  { background: var(--fts-primary); }
  &--rest  { background: var(--fts-bg-tertiary); border: 1px solid var(--fts-border-secondary); }
  &--today { background: var(--fts-primary); box-shadow: 0 0 4px rgba(var(--fts-primary-rgb), 0.4); }
}

/* ====== 排班统计双栏 ====== */
.rp-dual { display: grid; grid-template-columns: 1fr 1fr; gap: var(--fts-space-4); @media (max-width: 767px) { grid-template-columns: 1fr; } }
.rp-dual-col { margin-bottom: 0; }
.shift-bars { display: flex; flex-direction: column; gap: var(--fts-space-3); }
.sb-row { display: flex; align-items: center; gap: var(--fts-space-2); }
.sb-info { display: flex; align-items: center; gap: 6px; width: 52px; flex-shrink: 0; }
.sb-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block;
  &--m { background: var(--fts-shift-morning-dot); } &--a { background: var(--fts-shift-afternoon-dot); } &--e { background: var(--fts-shift-evening-dot); } &--r { background: var(--fts-shift-rest-dot); }
}
.sb-label { font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); white-space: nowrap; }
.sb-track { flex: 1; height: 10px; background: var(--fts-bg-tertiary); border-radius: var(--fts-radius-full); overflow: hidden; }
.sb-fill { height: 100%; border-radius: var(--fts-radius-full); transition: width 0.6s var(--fts-easing-default);
  &--m { background: var(--fts-shift-morning-dot); } &--a { background: var(--fts-shift-afternoon-dot); } &--e { background: var(--fts-shift-evening-dot); } &--r { background: var(--fts-shift-rest-dot); }
}
.sb-pct { width: 36px; text-align: right; font-size: var(--fts-font-size-sm); font-weight: var(--fts-font-weight-semibold); color: var(--fts-text-secondary); flex-shrink: 0; }

/* ====== 本周概览（与 LeavePage 本周考勤统一样式） ====== */
.attendance-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: var(--fts-space-3);
}

.att-cell {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: var(--fts-space-2) 2px; border-radius: var(--fts-radius-md);
  background: var(--fts-bg-secondary);
  transition: all 0.15s ease;

  /* 已上班：绿色左边框 */
  &--normal {
    border-left: 3px solid var(--fts-success);
    .att-date { color: var(--fts-text-primary); }
    .att-label { color: var(--fts-success); }
  }

  /* 今日：主色高亮 */
  &--today {
    border-left: 3px solid var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.08);
    .att-date { color: var(--fts-primary); }
    .att-label { color: var(--fts-primary); font-weight: 600; }
  }

  /* 休息：降低透明度 */
  &--rest { opacity: 0.45; }
}

.att-weekday { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }
.att-date { font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); font-weight: 500; }
.att-label { font-size: 9px; font-weight: 600; }

.att-legend {
  display: flex; align-items: center; gap: var(--fts-space-3);
}
.att-legend-item {
  display: flex; align-items: center; gap: 4px;
  font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary);
}
.att-dot {
  width: 8px; height: 8px; border-radius: 50%; display: inline-block;
  &--normal { background: var(--fts-success); }
  &--today  { background: var(--fts-primary); box-shadow: 0 0 4px rgba(var(--fts-primary-rgb), 0.4); }
  &--rest   { background: var(--fts-bg-tertiary); border: 1px solid var(--fts-border-secondary); }
}

/* ====== 工资趋势柱状图 ====== */
.chart-area { position: relative; }
.bar-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 180px; padding: 0 var(--fts-space-2); gap: var(--fts-space-3); }
.bar-col { display: flex; flex-direction: column; align-items: center; flex: 1; max-width: 64px; cursor: pointer; }
.bar-track { width: 100%; height: 140px; display: flex; align-items: flex-end; justify-content: center; }
.bar-fill { width: 70%; min-height: 4px; border-radius: 4px 4px 0 0;
  /* 深色模式：--fts-info-light 太暗，使用带透明度的主色确保可见 */
  background: rgba(var(--fts-primary-rgb), 0.30);
  transition: height 0.5s var(--fts-easing-default);
  .bar-col:hover & { opacity: 0.85; }
  .bar-col--active & { background: var(--fts-primary); }
}
.bar-label { margin-top: var(--fts-space-2); font-size: var(--fts-font-size-sm); color: var(--fts-text-quaternary);
  .bar-col--active & { color: var(--fts-primary); font-weight: var(--fts-font-weight-semibold); }
}
.chart-tooltip {
  position: fixed; transform: translateX(-50%) translateY(-100%); padding: var(--fts-space-1) var(--fts-space-3);
  /* 深色模式：深色背景 + 浅色文字（之前用 --fts-text-primary 做背景，暗模式下文字不可见） */
  background: var(--fts-bg-elevated); color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm); font-weight: var(--fts-font-weight-medium);
  border-radius: var(--fts-radius-sm); white-space: nowrap; pointer-events: none; z-index: 9999;
  border: 1px solid var(--fts-border-primary);
  box-shadow: var(--fts-shadow-md);
}
.tooltip-fade-enter-active,.tooltip-fade-leave-active { transition: opacity var(--fts-duration-fast) var(--fts-easing-default); }
.tooltip-fade-enter-from,.tooltip-fade-leave-to { opacity: 0; }

/* ====== 培训进度 - 纯CSS圆环进度指示器 ====== */
.rp-section--training .training-body {
  display: flex; align-items: center; gap: var(--fts-space-6);
  padding: var(--fts-space-4) 0;
  @media (max-width: 639px) { flex-direction: column; text-align: center; }
}

/* 圆环进度 - 使用 conic-gradient 实现纯CSS圆环 */
.progress-ring {
  position: relative; width: 120px; height: 120px; flex-shrink: 0;
  /* 外层容器尺寸 */
  &__track, &__fill {
    position: absolute; inset: 0; border-radius: 50%;
  }
  /* 底层轨道 */
  &__track {
    background: var(--fts-bg-tertiary);
  }
  /* 进度填充层 - conic-gradient 实现圆弧效果 */
  &__fill {
    background: conic-gradient(
      var(--fts-primary) 0%,
      var(--fts-primary) var(--ring-percent),
      transparent var(--ring-percent),
      transparent 100%
    );
    /* 使用 mask 镂空中心，形成圆环效果 */
    -webkit-mask: radial-gradient(circle at center, transparent 60%, black 61%);
    mask: radial-gradient(circle at center, transparent 60%, black 61%);
  }
  /* 中心内容区 */
  &__content {
    position: absolute; inset: 0;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    z-index: 1;
  }
  &__value {
    font-size: 26px; font-weight: var(--fts-font-weight-bold); color: var(--fts-primary); line-height: 1;
    small { font-size: 14px; font-weight: var(--fts-font-weight-normal); margin-left: 1px; }
  }
  &__label {
    font-size: var(--fts-font-size-caption); color: var(--fts-text-quaternary); margin-top: 2px;
  }
}

/* 培训摘要信息 */
.training-info {
  display: flex; align-items: center; gap: var(--fts-space-4);
  flex: 1;
  @media (max-width: 639px) { justify-content: center; }
}
.ti-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  &--completed .ti-value { color: var(--fts-success); }
  &--remaining .ti-value { color: var(--fts-warning); }
  &--total .ti-value { color: var(--fts-text-secondary); }
}
.ti-value {
  font-size: 22px; font-weight: var(--fts-font-weight-bold); line-height: 1.2;
}
.ti-label {
  font-size: var(--fts-font-size-sm); color: var(--fts-text-quaternary);
}
.ti-divider {
  width: 1px; height: 36px; background: var(--fts-border-secondary);
}

/* ====== 审批概况 - 3列小卡片 ====== */
.rp-section--approval .approval-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--fts-space-3);
  @media (max-width: 639px) { grid-template-columns: 1fr; }
}
.approval-card {
  display: flex; align-items: center; gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-page); border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-secondary);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);
  &:hover { transform: translateY(-2px); box-shadow: var(--fts-shadow-sm); border-color: transparent; }

  /* 主题色变体 */
  &--primary {
    .approval-card__icon { background: rgba(var(--fts-primary-rgb), 0.08); color: var(--fts-primary); }
    .approval-card__value { color: var(--fts-primary); }
  }
  &--success {
    .approval-card__icon { background: rgba(var(--fts-success-rgb), 0.08); color: var(--fts-success); }
    .approval-card__value { color: var(--fts-success); }
  }
  &--warning {
    .approval-card__icon { background: rgba(var(--fts-warning-rgb), 0.08); color: var(--fts-warning); }
    .approval-card__value { color: var(--fts-warning); }
  }

  &__icon {
    width: 44px; height: 44px; border-radius: var(--fts-radius-sm);
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  }
  &__body {
    display: flex; flex-direction: column; gap: 4px;
  }
  &__value {
    font-size: 22px; font-weight: var(--fts-font-weight-bold); line-height: 1.2;
  }
  &__label {
    font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary);
  }
}
</style>

<style lang="scss">
/*
 * 报表页日期选择器下拉浮层 — 全局样式（非 scoped）
 *
 * 系统性修复三个问题：
 *   1. 深色模式下拉菜单白色背景（EP 内层元素默认白色）
 *   2. 快速滑动时中间出现白色闪烁（scrollbar 层暴露）
 *   3. 选中项在深色背景上不可见（无对比背景）
 *
 * 使用 teleported=false 让 popper 留在组件 DOM 树内，
 * 配合此全局样式块实现完整覆盖。
 */

/* ====== 深色模式：三种激活方式全覆盖 ====== */
:root.dark .rp-date-popper,
[data-theme='dark'] .rp-date-popper {

  /* 容器层 */
  background-color: var(--fts-bg-elevated);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  box-shadow: var(--fts-shadow-lg);

  /* 层级约束：限制在页面内容区内，不穿透标题栏（EP 默认 z-index:2000+） */
  z-index: 100;

  /* 滚动容器 — 快速滑动时此层最容易暴露白色 */
  .el-scrollbar,
  .el-scrollbar__wrap,
  .el-scrollbar__view {
    background-color: transparent;
  }

  /* 下拉面板 + 列表容器 */
  .el-select-dropdown,
  .el-select-dropdown__wrap,
  .el-select-dropdown__list {
    background-color: transparent;
  }

  .el-select-dropdown__list {
    list-style: none;
    margin: 0;
    padding: 4px 0;
  }

  /* 列表项 */
  .el-select-dropdown__item {
    color: var(--fts-text-secondary);
    background-color: transparent;
    padding: 10px 16px;
    font-size: 14px;
    line-height: 1.4;
    transition: background-color 0.15s ease;

    &:hover {
      /* 深色模式：hover 需要足够明显的亮度差异 */
      background-color: rgba(255, 255, 255, 0.12);
    }

    /* 选中项：深色模式高对比度设计 */
    &.is-selected {
      color: var(--fts-text-on-primary);
      font-weight: 600;
      background: linear-gradient(90deg, rgba(var(--fts-primary-rgb), 0.38) 0%, rgba(var(--fts-primary-rgb), 0.18) 100%);
      border-left: 3px solid var(--fts-primary);
    }

    &.is-disabled {
      color: var(--fts-text-quaternary);
      cursor: not-allowed;
    }
  }

  /* 隐藏滚动条，hover 时显示 */
  .el-scrollbar__bar.is-horizontal,
  .el-scrollbar__bar.is-vertical {
    opacity: 0;
    transition: opacity 0.2s;
  }
  &:hover .el-scrollbar__bar.is-vertical {
    opacity: 1;
  }
}

/* 跟随系统深色模式（未手动设置时） */
@media (prefers-color-scheme: dark) {
  :root:not([data-theme='light']) .rp-date-popper {
    background-color: var(--fts-bg-elevated);
    border: 1px solid var(--fts-border-primary);
    border-radius: var(--fts-radius-md);
    box-shadow: var(--fts-shadow-lg);

    /* 层级约束 */
    z-index: 100;

    .el-scrollbar,
    .el-scrollbar__wrap,
    .el-scrollbar__view,
    .el-select-dropdown,
    .el-select-dropdown__wrap,
    .el-select-dropdown__list {
      background-color: transparent;
    }

    .el-select-dropdown__list {
      list-style: none; margin: 0; padding: 4px 0;
    }

    .el-select-dropdown__item {
      color: var(--fts-text-secondary);
      background-color: transparent;
      padding: 10px 16px;
      font-size: 14px;
      line-height: 1.4;
      transition: background-color 0.15s ease;

      &:hover {
        background-color: rgba(255, 255, 255, 0.12);
      }
      &.is-selected {
        color: var(--fts-text-on-primary);
        font-weight: 600;
        background: linear-gradient(90deg, rgba(var(--fts-primary-rgb), 0.38) 0%, rgba(var(--fts-primary-rgb), 0.18) 100%);
        border-left: 3px solid var(--fts-primary);
      }
      &.is-disabled {
        color: var(--fts-text-quaternary);
        cursor: not-allowed;
      }
    }

    .el-scrollbar__bar.is-horizontal,
    .el-scrollbar__bar.is-vertical {
      opacity: 0; transition: opacity 0.2s;
    }
    &:hover .el-scrollbar__bar.is-vertical { opacity: 1; }
  }
}

/* 浅色模式：选中项也要有可见背景 + 层级约束 */
.rp-date-popper {
  z-index: 100;
}

.rp-date-popper .el-select-dropdown__item.is-selected {
  background: linear-gradient(90deg, rgba(var(--fts-primary-rgb), 0.12) 0%, rgba(var(--fts-primary-rgb), 0.05) 100%);
  font-weight: 600;
  border-left: 3px solid var(--fts-primary);
}
</style>
