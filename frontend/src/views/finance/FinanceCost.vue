<script setup lang="ts">
/**
 * 成本管理页面
 * 数据来源：/v1/costs
 */
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { costApi } from '@/api/finance'
import type { FinanceCostRecord, FinanceCostQueryForm, FinanceCostType } from '@/types/finance'
import type { StatColorType } from '@/types/stat'
import CostFormDialog from './components/CostFormDialog.vue'

const { loading, pagination, handleSearch, handleReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

const searchForm = ref({
  costType: '' as '' | FinanceCostType,
  dateRange: null as [string, string] | null,
})

/** 原始数据（金额已被 CostDataConverter.toFrontend 转为元） */
const rawRecords = ref<FinanceCostRecord[]>([])

/** 格式化元金额（保留2位小数，带千分位）
 * 注：rawRecords 中的 amount/budgetAmount/variance 已被 Converter 转为元，此处仅做展示格式化 */
function formatAmount(yuan: number | undefined | null): string {
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 格式化元金额（带¥前缀，支持正负号） */
function formatYuanSigned(yuan: number | undefined | null): string {
  const value = yuan ?? 0
  const formatted = Math.abs(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return value < 0 ? `-¥${formatted}` : `¥${formatted}`
}

/** 表格展示数据（金额已被 Converter 转为元，此处仅做展示格式化） */
const records = computed(() =>
  rawRecords.value.map(r => ({
    ...r,
    budgetAmount: formatAmount(r.budgetAmount),
    amount: formatAmount(r.amount),
    variance: formatAmount(r.variance),
  }))
)

/** 统计卡片（基于当前页数据计算） */
const statsCards = computed(() => {
  const all = rawRecords.value
  const totalCost = all.reduce((s, r) => s + (r.amount || 0), 0)
  const materialCost = all.filter(r => r.costType === 'material').reduce((s, r) => s + (r.amount || 0), 0)
  const laborCost = all.filter(r => r.costType === 'labor').reduce((s, r) => s + (r.amount || 0), 0)
  const totalVariance = all.reduce((s, r) => s + (r.variance || 0), 0)
  const materialRate = totalCost > 0 ? ((materialCost / totalCost) * 100).toFixed(1) : '0.0'
  const laborRate = totalCost > 0 ? ((laborCost / totalCost) * 100).toFixed(1) : '0.0'
  return [
    { icon: 'Money', label: '本月总成本', value: formatYuanSigned(totalCost), colorType: '' as StatColorType },
    { icon: 'TrendCharts', label: '食材成本率', value: `${materialRate}%`, colorType: '' as StatColorType },
    { icon: 'User', label: '人工成本率', value: `${laborRate}%`, colorType: '' as StatColorType },
    { icon: 'DataLine', label: '成本偏差', value: formatYuanSigned(totalVariance), colorType: '' as StatColorType },
  ]
})

const columns = [
  { prop: 'costDate', label: '日期', minWidth: 110 },
  { prop: 'categoryName', label: '成本类别', minWidth: 120, slot: 'category' },
  { prop: 'description', label: '说明', minWidth: 200, ellipsis: true },
  { prop: 'budgetAmount', label: '预算金额(元)', minWidth: 130 },
  { prop: 'amount', label: '实际金额(元)', minWidth: 130 },
  { prop: 'variance', label: '偏差(元)', minWidth: 110 },
  { prop: 'varianceRate', label: '偏差率', minWidth: 90 },
]

/** 加载成本记录列表 */
async function loadData() {
  loading.value = true
  try {
    const params: FinanceCostQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.costType) params.costType = searchForm.value.costType
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = searchForm.value.dateRange[0]
      params.endDate = searchForm.value.dateRange[1]
    }
    const response = await costApi.getList(params)
    rawRecords.value = response?.records || []
    pagination.total = response?.total || 0
  } catch {
    rawRecords.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function resetSearchForm() {
  searchForm.value = { costType: '', dateRange: null }
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

/** 新增/编辑表单对话框 */
const formDialogVisible = ref(false)
const editingCostId = ref<string | null>(null)

/** 打开新增对话框 */
function handleCreate(): void {
  editingCostId.value = null
  formDialogVisible.value = true
}

/** 打开编辑对话框 */
function handleEdit(row: Record<string, unknown>): void {
  editingCostId.value = row.id as string
  formDialogVisible.value = true
}

/** 表单提交成功后刷新列表 */
function handleFormSuccess(): void {
  loadData()
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="成本管理" description="门店运营成本核算与分析">
      <template #extra>
        <el-button type="primary" size="default" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>新增成本
        </el-button>
      </template>
    </PageHeader>
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.costType" placeholder="成本类别" clearable style="width:140px" size="default">
            <el-option label="食材成本" value="material" />
            <el-option label="人工成本" value="labor" />
            <el-option label="租金成本" value="rent" />
            <el-option label="能耗成本" value="energy" />
            <el-option label="营销成本" value="marketing" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" placement="bottom-start" style="width:240px;max-width:240px;flex-shrink:0" size="default" />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>
    <div class="table-section">
      <DataTable :columns="columns" :data="records" :loading="loading" stripe :actions-width="160">
        <template #category="{ row }">
          <StatusTag status="info" :label="row.categoryName" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total,prev,pager,next,jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="成本记录详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="日期">{{ detailData.costDate as string }}</el-descriptions-item>
        <el-descriptions-item label="成本类别">{{ detailData.categoryName as string }}</el-descriptions-item>
        <el-descriptions-item label="预算金额(元)">¥{{ detailData.budgetAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="实际金额(元)">¥{{ detailData.amount as string }}</el-descriptions-item>
        <el-descriptions-item label="偏差(元)">{{ detailData.variance as string }}</el-descriptions-item>
        <el-descriptions-item label="偏差率">{{ detailData.varianceRate as string }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ (detailData.department as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ (detailData.description as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 新增/编辑对话框 -->
    <CostFormDialog v-model="formDialogVisible" :cost-id="editingCostId" @success="handleFormSuccess" />
  </div>
</template>
