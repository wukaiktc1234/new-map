<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { bankAccountApi, fundFlowApi } from '@/api/finance'
import type { FinanceBankAccount, FundFlow, FundFlowStatistics } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { pagination, resetPagination } = useStandardPage()
const searchForm = ref({
  bankAccountId: '',
  flowType: '' as '' | 'income' | 'expense',
  dateRange: null as [string, string] | null,
})
const loading = ref(false)

/** 银行账户列表（用于下拉选择和余额统计） */
const bankAccounts = ref<FinanceBankAccount[]>([])
/** 资金流水列表 */
const records = ref<FundFlow[]>([])
/** 资金流水统计 */
const statistics = ref<FundFlowStatistics | null>(null)

/** 收支类型 → StatusTag 配置（避免模板内三元映射） */
function getFlowTypeTag(flowType: string): { type: string; label: string } {
  return flowType === 'income'
    ? { type: 'success', label: '收入' }
    : { type: 'warning', label: '支出' }
}

/** 流水状态 → StatusTag 配置（避免模板内三元映射） */
function getFlowStatusTag(status: string): { type: string; label: string } {
  const map: Record<string, { type: string; label: string }> = {
    completed: { type: 'success', label: '已完成' },
    pending: { type: 'warning', label: '待处理' },
    cancelled: { type: 'info', label: '已取消' },
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

/** 格式化元金额（保留2位小数，带千分位）
 * 注：bankAccounts.balance、fundFlow.amount/balance、statistics 金额均已被对应 Converter 转为元 */
function formatYuan(yuan: number | undefined | null): string {
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 统计卡片数据 */
const statsCards = ref<Array<{ icon: string; label: string; value: string; colorType: StatColorType }>>([
  { icon: 'Wallet', label: '银行余额', value: '¥0.00', colorType: 'primary' },
  { icon: 'TrendCharts', label: '本月收入', value: '¥0.00', colorType: 'success' },
  { icon: 'DataLine', label: '本月支出', value: '¥0.00', colorType: 'warning' },
  { icon: 'Star', label: '资金周转率', value: '0.00次', colorType: 'info' },
])

const columns: DataTableColumn[] = [
  { prop: 'transactionDate', label: '日期', minWidth: 110 },
  { prop: 'flowType', label: '收支类型', minWidth: 90, slot: 'flowType' },
  { prop: 'summary', label: '摘要', minWidth: 180, showOverflowTooltip: true },
  { prop: 'income', label: '收入(元)', minWidth: 120, align: 'right' },
  { prop: 'expense', label: '支出(元)', minWidth: 120, align: 'right' },
  { prop: 'balance', label: '余额(元)', minWidth: 120, align: 'right' },
  { prop: 'bankAccountName', label: '银行账户', minWidth: 130 },
  { prop: 'counterparty', label: '对方单位', minWidth: 130 },
]

/** 构建查询参数 */
function buildQueryParams() {
  const params: Record<string, unknown> = {
    page: pagination.current,
    size: pagination.size,
  }
  if (searchForm.value.bankAccountId) {
    params.bankAccountId = searchForm.value.bankAccountId
  }
  if (searchForm.value.flowType) {
    params.flowType = searchForm.value.flowType
  }
  if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
    params.startDate = searchForm.value.dateRange[0]
    params.endDate = searchForm.value.dateRange[1]
  }
  return params
}

/** 加载银行账户列表 */
async function loadBankAccounts(): Promise<void> {
  try {
    const response = await bankAccountApi.getList({ page: 1, size: 100 })
    bankAccounts.value = response?.records || []
  } catch {
    bankAccounts.value = []
  }
}

/** 加载资金流水统计 */
async function loadStatistics(): Promise<void> {
  // 优先使用第一个银行账户的统计数据
  if (bankAccounts.value.length === 0) {
    statistics.value = null
    return
  }
  try {
    const accountId = bankAccounts.value[0].id
    // 后端要求 startDate/endDate 必填（格式 yyyy-MM-dd），默认查当月数据
    const now = new Date()
    const startDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
    const endDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()).padStart(2, '0')}`
    statistics.value = await fundFlowApi.getStatisticsByAccount(accountId, startDate, endDate)
  } catch {
    statistics.value = null
  }
}

/** 更新统计卡片 */
function updateStatsCards(): void {
  // 银行余额：所有账户余额之和（acc.balance 已被 BankAccountDataConverter 转为元）
  const totalBalance = bankAccounts.value.reduce(
    (sum, acc) => sum + (acc.balance || 0),
    0,
  )
  statsCards.value[0].value = `¥${formatYuan(totalBalance)}`

  // 本月收入/支出：来自统计（statistics 已被 toFrontendStatistics 转为元）
  const income = statistics.value?.totalIncome || 0
  const expense = statistics.value?.totalExpense || 0
  statsCards.value[1].value = `¥${formatYuan(income)}`
  statsCards.value[2].value = `¥${formatYuan(expense)}`

  // 资金周转率：支出 / 余额（保留2位小数，两者均已是元）
  const turnoverRate = totalBalance > 0 ? (expense / totalBalance).toFixed(2) : '0.00'
  statsCards.value[3].value = `${turnoverRate}次`
}

/** 加载资金流水列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const response = await fundFlowApi.getList(buildQueryParams())
    records.value = response?.records || []
    pagination.total = response?.total || 0
  } catch {
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 查询 */
function handleSearch(): void {
  resetPagination()
  loadData()
}

/** 重置 */
function handleReset(): void {
  searchForm.value = {
    bankAccountId: '',
    flowType: '',
    dateRange: null,
  }
  resetPagination()
  loadData()
}

/** 每页条数变化 */
function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
  loadData()
}

/** 页码变化 */
function handleCurrentChange(current: number): void {
  pagination.current = current
  loadData()
}

/** 获取收入金额（元，row.amount 已被 FundFlowDataConverter 转为元） */
function getIncome(row: FundFlow): string {
  if (row.flowType === 'income') {
    return formatYuan(row.amount)
  }
  return '0.00'
}

/** 获取支出金额（元，row.amount 已被 FundFlowDataConverter 转为元） */
function getExpense(row: FundFlow): string {
  if (row.flowType === 'expense') {
    return formatYuan(row.amount)
  }
  return '0.00'
}

/** 获取余额（元，row.balance 已被 FundFlowDataConverter 转为元） */
function getBalance(row: FundFlow): string {
  return formatYuan(row.balance)
}

onMounted(async () => {
  await loadBankAccounts()
  await Promise.all([loadData(), loadStatistics()])
  updateStatsCards()
})

/** 详情对话框 */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

/** 打开详情对话框 */
function handleViewDetail(row: Record<string, unknown>): void {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="资金管理" description="银行账户与资金流水管理" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="searchForm.bankAccountId" placeholder="银行账户" clearable style="width:180px" size="default">
        <el-option v-for="acc in bankAccounts" :key="acc.id" :label="`${acc.bankName}-${acc.accountName}`" :value="acc.id" />
      </el-select>
      <el-select v-model="searchForm.flowType" placeholder="收支类型" clearable style="width:130px" size="default">
        <el-option label="收入" value="income" />
        <el-option label="支出" value="expense" />
      </el-select>
      <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" placement="bottom-start" style="width:240px;max-width:240px;flex-shrink:0" size="default" />
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="records" :loading="loading" stripe>
        <template #flowType="{ row }"><StatusTag :status="getFlowTypeTag(row.flowType).type" :label="getFlowTypeTag(row.flowType).label" size="small" /></template>
        <template #income="{ row }">{{ getIncome(row) }}</template>
        <template #expense="{ row }">{{ getExpense(row) }}</template>
        <template #balance="{ row }">{{ getBalance(row) }}</template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="资金流水详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="流水号">{{ (detailData.flowNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="交易日期">{{ detailData.transactionDate as string }}</el-descriptions-item>
        <el-descriptions-item label="收支类型">
          <StatusTag :status="getFlowTypeTag(detailData.flowType as string).type" :label="getFlowTypeTag(detailData.flowType as string).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="金额(元)">{{ detailData.flowType === 'income' ? getIncome(detailData as unknown as FundFlow) : getExpense(detailData as unknown as FundFlow) }}</el-descriptions-item>
        <el-descriptions-item label="银行账户">{{ (detailData.bankAccountName as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对方单位">{{ (detailData.counterparty as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="余额(元)">{{ getBalance(detailData as unknown as FundFlow) }}</el-descriptions-item>
        <el-descriptions-item label="凭证号">{{ (detailData.voucherNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getFlowStatusTag(detailData.status as string).type" :label="getFlowStatusTag(detailData.status as string).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ (detailData.summary as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
