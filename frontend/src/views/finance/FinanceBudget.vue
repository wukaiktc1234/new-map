<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { budgetApi, BudgetStatusMap } from '@/api/finance'
import type { FinanceBudget, BudgetStatus, BudgetType, FinanceBudgetQueryForm } from '@/types/finance'
import type { StatColorType } from '@/types/stat'
import BudgetFormDialog from './components/BudgetFormDialog.vue'

const { pagination } = useStandardPage()
const searchForm = ref({ year: String(new Date().getFullYear()) })
const loading = ref(false)
const records = ref<FinanceBudget[]>([])

/** 年份下拉选项（当前年份 ± 2年） */
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  return [currentYear - 2, currentYear - 1, currentYear, currentYear + 1].map(y => String(y))
})

/** 预算展示数据（金额已转为格式化字符串） */
interface BudgetDisplay {
  id: string
  budgetName: string
  period: string
  budgetType: BudgetType
  budgetAmount: string
  actualAmount: string
  executionRate: string
  status: BudgetStatus
}

const displayRecords = ref<BudgetDisplay[]>([])

/** 格式化元金额（带千分位，保留2位小数）
 * 注：records 中的 budgetAmount/actualAmount 已被 BudgetDataConverter.toFrontend 转为元，此处仅做展示格式化 */
function formatAmount(yuan: number | undefined | null): string {
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 将后端状态（数字或字符串）映射为前端语义字符串 */
function mapBudgetStatus(status: number | string): BudgetStatus {
  if (typeof status === 'number') {
    return (BudgetStatusMap.toFrontend[status] ?? 'draft') as BudgetStatus
  }
  return status as BudgetStatus
}

/** 获取预算状态标签配置 */
function getStatusTag(status: BudgetStatus): { type: string; label: string } {
  const map: Record<BudgetStatus, { type: string; label: string }> = {
    draft: { type: 'warning', label: '草稿' },
    approved: { type: 'info', label: '已审批' },
    executing: { type: 'success', label: '执行中' },
    closed: { type: 'info', label: '已关闭' },
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

/** 获取预算类型标签 */
function getBudgetTypeLabel(type: BudgetType): string {
  const map: Record<BudgetType, string> = {
    operating: '运营预算',
    procurement: '采购预算',
    hr: '人力预算',
    marketing: '营销预算',
    capital: '资本预算',
  }
  return map[type] ?? type
}

/** 计算完成率 */
function calcExecutionRate(budgetAmount: number, actualAmount: number): string {
  if (!budgetAmount || budgetAmount === 0) return '-'
  const rate = (actualAmount / budgetAmount) * 100
  return rate.toFixed(1) + '%'
}

/** 格式化期间 */
function formatPeriod(year: number, month?: number): string {
  if (month) {
    return `${year}-${String(month).padStart(2, '0')}`
  }
  return String(year)
}

/** 加载预算列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: FinanceBudgetQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.year) {
      params.year = parseInt(searchForm.value.year, 10)
    }
    const response = await budgetApi.getList(params)
    records.value = response?.records || []
    pagination.total = response?.total || 0
    // 转换为展示数据：金额格式化、状态映射
    displayRecords.value = records.value.map((b): BudgetDisplay => {
      const status = mapBudgetStatus(b.status)
      // 优先使用后端返回的执行率，否则前端计算
      const executionRate = b.executionRate != null
        ? b.executionRate.toFixed(1) + '%'
        : calcExecutionRate(b.budgetAmount, b.actualAmount)
      return {
        id: b.id,
        budgetName: b.budgetName,
        period: formatPeriod(b.year, b.month),
        budgetType: b.budgetType,
        budgetAmount: formatAmount(b.budgetAmount),
        actualAmount: formatAmount(b.actualAmount),
        executionRate,
        status,
      }
    })
  } catch (error) {
    console.error('加载预算列表失败:', error)
    records.value = []
    displayRecords.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计卡片数据（基于已加载列表数据计算） */
const statsCards = computed(() => {
  const totalBudget = records.value.reduce((sum, b) => sum + (b.budgetAmount || 0), 0)
  const totalActual = records.value.reduce((sum, b) => sum + (b.actualAmount || 0), 0)
  const overBudgetCount = records.value.filter(b => b.actualAmount > b.budgetAmount).length
  const completionRate = totalBudget > 0
    ? ((totalActual / totalBudget) * 100).toFixed(1) + '%'
    : '-'
  return [
    { icon: 'TrendCharts', label: '年度预算总额', value: '¥' + formatAmount(totalBudget), colorType: '' as StatColorType },
    { icon: 'DataLine', label: '年度实际总额', value: '¥' + formatAmount(totalActual), colorType: '' as StatColorType },
    { icon: 'Star', label: '预算完成率', value: completionRate, colorType: '' as StatColorType },
    { icon: 'Warning', label: '超预算项数', value: overBudgetCount, colorType: '' as StatColorType },
  ]
})

const columns: DataTableColumn[] = [
  { prop: 'budgetName', label: '预算名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'period', label: '期间', minWidth: 100, align: 'center' },
  { prop: 'budgetType', label: '预算类型', minWidth: 110, slot: 'budgetType' },
  { prop: 'budgetAmount', label: '预算金额(元)', minWidth: 140, align: 'right' },
  { prop: 'actualAmount', label: '实际金额(元)', minWidth: 140, align: 'right' },
  { prop: 'executionRate', label: '完成率', minWidth: 90, align: 'center' },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status' },
]

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

function handleReset() {
  searchForm.value = { year: String(new Date().getFullYear()) }
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

function handleCurrentChange(current: number) {
  pagination.current = current
  loadData()
}

onMounted(() => {
  loadData()
})

/** 详情对话框 */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

/** 打开详情对话框 */
function handleViewDetail(row: Record<string, unknown>): void {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}

// ============================================================
// 新增/编辑对话框
// ============================================================

/** 表单对话框可见性 */
const formDialogVisible = ref(false)
/** 当前编辑的预算ID（null表示新增） */
const editingBudgetId = ref<string | null>(null)

/** 打开新增预算对话框 */
function handleCreate(): void {
  editingBudgetId.value = null
  formDialogVisible.value = true
}

/** 打开编辑预算对话框 */
function handleEdit(row: Record<string, unknown>): void {
  editingBudgetId.value = row.id as string
  formDialogVisible.value = true
}

/** 预算保存成功后刷新列表 */
function handleFormSuccess(): void {
  loadData()
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="预算管理" description="年度预算制定与执行分析">
      <template #extra>
        <el-button type="primary" size="default" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>新增预算
        </el-button>
      </template>
    </PageHeader>
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="searchForm.year" placeholder="选择年度" style="width:120px" size="default">
        <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
      </el-select>
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="displayRecords" :loading="loading" stripe :actions-width="160">
        <template #budgetType="{ row }"><StatusTag status="info" :label="getBudgetTypeLabel(row.budgetType)" size="small" /></template>
        <template #status="{ row }"><StatusTag :status="getStatusTag(row.status).type" :label="getStatusTag(row.status).label" size="small" /></template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="预算详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="预算名称" :span="2">{{ detailData.budgetName as string }}</el-descriptions-item>
        <el-descriptions-item label="预算类型">{{ getBudgetTypeLabel(detailData.budgetType as BudgetType) }}</el-descriptions-item>
        <el-descriptions-item label="期间">{{ detailData.period as string }}</el-descriptions-item>
        <el-descriptions-item label="预算金额(元)">¥{{ detailData.budgetAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="实际金额(元)">¥{{ detailData.actualAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="完成率">{{ detailData.executionRate as string }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusTag(detailData.status as BudgetStatus).type" :label="getStatusTag(detailData.status as BudgetStatus).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑预算对话框 -->
    <BudgetFormDialog v-model="formDialogVisible" :budget-id="editingBudgetId" @success="handleFormSuccess" />
  </div>
</template>
