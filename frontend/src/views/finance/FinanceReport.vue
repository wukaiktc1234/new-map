<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { reportApi, accountingPeriodApi, fenToYuan } from '@/api/finance'
import type {
  FinanceReportType,
  ProfitStatementData,
  BalanceSheetData,
  CashFlowStatementData,
} from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { pagination } = useStandardPage()
const loading = ref(false)

/** 当前报表类型 */
const reportType = ref<FinanceReportType>('profit')
/** 月份范围 */
const dateRange = ref<[string, string] | null>(null)

/** 报表数据 */
const profitData = ref<ProfitStatementData | null>(null)
const balanceData = ref<BalanceSheetData | null>(null)
const cashFlowData = ref<CashFlowStatementData | null>(null)

/** 报表类型选项 */
const reportTypeOptions: Array<{ label: string; value: FinanceReportType }> = [
  { label: '利润表', value: 'profit' },
  { label: '资产负债表', value: 'balance' },
  { label: '现金流量表', value: 'cash_flow' },
]

/** 统计卡片数据 */
const statsCards = ref<Array<{ icon: string; label: string; value: string; colorType: StatColorType }>>([
  { icon: 'TrendCharts', label: '营业收入', value: '¥0.00', colorType: 'primary' },
  { icon: 'DataLine', label: '毛利', value: '¥0.00', colorType: 'success' },
  { icon: 'Star', label: '毛利率', value: '0%', colorType: 'info' },
  { icon: 'Money', label: '净利润', value: '¥0.00', colorType: 'warning' },
])

/** 表格列定义（动态） */
const columns = computed(() => {
  if (reportType.value === 'profit') {
    return [
      { prop: 'item', label: '项目', minWidth: 160, fixed: 'left' as const },
      { prop: 'currentPeriod', label: '本期金额(元)', minWidth: 140 },
      { prop: 'previousPeriod', label: '上期金额(元)', minWidth: 140 },
      { prop: 'changeRate', label: '同比变动', minWidth: 100 },
    ]
  }
  if (reportType.value === 'balance') {
    return [
      { prop: 'category', label: '类别', minWidth: 100 },
      { prop: 'item', label: '项目', minWidth: 180 },
      { prop: 'amount', label: '金额(元)', minWidth: 140 },
    ]
  }
  // cash_flow
  return [
    { prop: 'activity', label: '活动类别', minWidth: 120 },
    { prop: 'item', label: '项目', minWidth: 160 },
    { prop: 'inflow', label: '流入(元)', minWidth: 130 },
    { prop: 'outflow', label: '流出(元)', minWidth: 130 },
    { prop: 'net', label: '净额(元)', minWidth: 130 },
  ]
})

/** 表格数据（动态转换） */
const allTableData = computed<Record<string, unknown>[]>(() => {
  if (reportType.value === 'profit' && profitData.value) {
    return profitData.value.details.map(d => ({
      item: d.item,
      currentPeriod: fenToYuan(d.currentPeriod),
      previousPeriod: fenToYuan(d.previousPeriod),
      changeRate: d.changeRate || '-',
    }))
  }
  if (reportType.value === 'balance' && balanceData.value) {
    const rows: Record<string, unknown>[] = []
    balanceData.value.assets.forEach(a => rows.push({ category: '资产', item: a.item, amount: fenToYuan(a.amount) }))
    balanceData.value.liabilities.forEach(l => rows.push({ category: '负债', item: l.item, amount: fenToYuan(l.amount) }))
    balanceData.value.equity.forEach(e => rows.push({ category: '权益', item: e.item, amount: fenToYuan(e.amount) }))
    return rows
  }
  if (reportType.value === 'cash_flow' && cashFlowData.value) {
    const d = cashFlowData.value
    return [
      { activity: '经营活动', item: '现金流入', inflow: fenToYuan(d.operatingInflow), outflow: '-', net: '-' },
      { activity: '经营活动', item: '现金流出', inflow: '-', outflow: fenToYuan(d.operatingOutflow), net: '-' },
      { activity: '经营活动', item: '净额', inflow: '-', outflow: '-', net: fenToYuan(d.operatingNet) },
      { activity: '投资活动', item: '现金流入', inflow: fenToYuan(d.investingInflow), outflow: '-', net: '-' },
      { activity: '投资活动', item: '现金流出', inflow: '-', outflow: fenToYuan(d.investingOutflow), net: '-' },
      { activity: '投资活动', item: '净额', inflow: '-', outflow: '-', net: fenToYuan(d.investingNet) },
      { activity: '筹资活动', item: '现金流入', inflow: fenToYuan(d.financingInflow), outflow: '-', net: '-' },
      { activity: '筹资活动', item: '现金流出', inflow: '-', outflow: fenToYuan(d.financingOutflow), net: '-' },
      { activity: '筹资活动', item: '净额', inflow: '-', outflow: '-', net: fenToYuan(d.financingNet) },
      { activity: '合计', item: '现金净增加额', inflow: '-', outflow: '-', net: fenToYuan(d.netIncrease) },
    ]
  }
  return []
})

/** 当前页表格数据（按分页切片） */
const tableData = computed<Record<string, unknown>[]>(() => {
  const start = (pagination.current - 1) * pagination.size
  return allTableData.value.slice(start, start + pagination.size)
})

/** 构建报表查询参数 */
function buildQueryParams(): { reportType: FinanceReportType; startDate: string; endDate: string; period?: 'monthly' } {
  const today = new Date()
  // 后端 ReportController 使用 @DateTimeFormat(pattern = "yyyy-MM-dd") 解析日期
  // 必须传 YYYY-MM-DD 格式，不能只传 YYYY-MM
  const defaultStart = `${today.getFullYear()}-01-01`
  const defaultEnd = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  let startDate = dateRange.value?.[0] || defaultStart
  let endDate = dateRange.value?.[1] || defaultEnd
  // 兼容日期选择器返回 YYYY-MM 格式的情况，补全为 YYYY-MM-DD
  if (/^\d{4}-\d{2}$/.test(startDate)) startDate = `${startDate}-01`
  if (/^\d{4}-\d{2}$/.test(endDate)) {
    // 结束日期取该月最后一天
    const [year, month] = endDate.split('-').map(Number)
    const lastDay = new Date(year, month, 0).getDate()
    endDate = `${endDate}-${String(lastDay).padStart(2, '0')}`
  }
  return {
    reportType: reportType.value,
    startDate,
    endDate,
    period: 'monthly',
  }
}

/** 更新统计卡片 */
function updateStatsCards(): void {
  if (reportType.value === 'profit' && profitData.value) {
    const d = profitData.value
    statsCards.value = [
      { icon: 'TrendCharts', label: '营业收入', value: `¥${fenToYuan(d.operatingIncome)}`, colorType: 'primary' },
      { icon: 'DataLine', label: '毛利', value: `¥${fenToYuan(d.grossProfit)}`, colorType: 'success' },
      { icon: 'Star', label: '毛利率', value: d.grossProfitRate || '0%', colorType: 'info' },
      { icon: 'Money', label: '净利润', value: `¥${fenToYuan(d.netProfit)}`, colorType: 'warning' },
    ]
  } else if (reportType.value === 'balance' && balanceData.value) {
    const d = balanceData.value
    const debtRatio = d.totalAssets > 0
      ? ((d.totalLiabilities / d.totalAssets) * 100).toFixed(2) + '%'
      : '0%'
    statsCards.value = [
      { icon: 'TrendCharts', label: '资产总计', value: `¥${fenToYuan(d.totalAssets)}`, colorType: 'primary' },
      { icon: 'DataLine', label: '负债总计', value: `¥${fenToYuan(d.totalLiabilities)}`, colorType: 'error' },
      { icon: 'Star', label: '所有者权益', value: `¥${fenToYuan(d.totalEquity)}`, colorType: 'success' },
      { icon: 'Coin', label: '资产负债率', value: debtRatio, colorType: 'warning' },
    ]
  } else if (reportType.value === 'cash_flow' && cashFlowData.value) {
    const d = cashFlowData.value
    statsCards.value = [
      { icon: 'TrendCharts', label: '经营净额', value: `¥${fenToYuan(d.operatingNet)}`, colorType: 'primary' },
      { icon: 'DataLine', label: '投资净额', value: `¥${fenToYuan(d.investingNet)}`, colorType: 'info' },
      { icon: 'Star', label: '筹资净额', value: `¥${fenToYuan(d.financingNet)}`, colorType: 'warning' },
      { icon: 'Money', label: '现金净增加', value: `¥${fenToYuan(d.netIncrease)}`, colorType: 'success' },
    ]
  }
}

/** 加载报表数据 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params = buildQueryParams()
    if (reportType.value === 'profit') {
      profitData.value = await reportApi.getProfitStatement(params)
      balanceData.value = null
      cashFlowData.value = null
    } else if (reportType.value === 'balance') {
      balanceData.value = await reportApi.getBalanceSheet(params)
      profitData.value = null
      cashFlowData.value = null
    } else {
      cashFlowData.value = await reportApi.getCashFlowStatement(params)
      profitData.value = null
      balanceData.value = null
    }
    pagination.total = allTableData.value.length
    // 数据变化时回到第一页
    pagination.current = 1
    updateStatsCards()
  } catch {
    profitData.value = null
    balanceData.value = null
    cashFlowData.value = null
    pagination.total = 0
    updateStatsCards()
  } finally {
    loading.value = false
  }
}

/** 报表类型切换 */
function handleReportTypeChange(): void {
  loadData()
}

/** 查询 */
function handleSearch(): void {
  loadData()
}

/** 重置 */
function handleReset(): void {
  dateRange.value = null
  loadData()
}

/** 每页条数变化 */
function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
}

/** 页码变化 */
function handleCurrentChange(current: number): void {
  pagination.current = current
}

/** 初始化默认日期范围 */
async function initDefaultDateRange(): Promise<void> {
  try {
    const currentPeriod = await accountingPeriodApi.getCurrent()
    if (currentPeriod?.periodName) {
      // 使用当前期间作为默认结束月份，同年1月作为开始月份
      const year = currentPeriod.year || new Date().getFullYear()
      dateRange.value = [`${year}-01`, currentPeriod.periodName]
    }
  } catch {
    // 静默失败，使用默认日期范围
  }
}

onMounted(async () => {
  await initDefaultDateRange()
  await loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="财务报表" description="损益表、资产负债表、现金流量表等财务综合分析" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="reportType" placeholder="报表类型" style="width:140px" size="default" @change="handleReportTypeChange">
        <el-option v-for="opt in reportTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-date-picker v-model="dateRange" type="monthrange" range-separator="至" start-placeholder="开始月份" end-placeholder="结束月份" placement="bottom-start" style="width:240px;max-width:240px;flex-shrink:0" size="default" />
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe />
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
  </div>
</template>
