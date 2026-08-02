<script setup lang="ts">
/**
 * 应收账款页面
 * 数据来源：/v1/receivables
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { receivableApi } from '@/api/finance'
import ReceiptDialog from './components/ReceiptDialog.vue'
import ReceiptHistoryDialog from './components/ReceiptHistoryDialog.vue'
import type { FinanceReceivable, FinanceReceivableFormData, FinanceReceivableQueryForm, ReceivableStatus } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { loading, pagination, handleSearch, handleReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

const searchForm = ref({ status: '' as '' | ReceivableStatus, keyword: '' })

/** 原始数据（金额已被 ReceivableDataConverter.toFrontend 转为元） */
const rawRecords = ref<FinanceReceivable[]>([])

/** 格式化元金额（保留2位小数，带千分位）
 * 注：rawRecords 中的 amount/receivedAmount/remainAmount 已被 Converter 转为元，此处仅做展示格式化 */
function formatAmount(yuan: number | undefined | null): string {
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 格式化元金额（带¥前缀） */
function formatYuan(yuan: number | undefined | null): string {
  return `¥${formatAmount(yuan)}`
}

/** 应收账款状态 → StatusTag 配置（避免模板内三元映射） */
function getStatusTag(status: ReceivableStatus): { type: string; label: string } {
  const map: Record<ReceivableStatus, { type: string; label: string }> = {
    settled: { type: 'success', label: '已结清' },
    partial: { type: 'warning', label: '部分收' },
    overdue: { type: 'error', label: '已逾期' },
    unpaid: { type: 'info', label: '未收' },
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

/** 表格展示数据（金额已被 Converter 转为元，此处仅做展示格式化） */
const records = computed(() =>
  rawRecords.value.map(r => ({
    ...r,
    amount: formatAmount(r.amount),
    receivedAmount: formatAmount(r.receivedAmount),
    remainAmount: formatAmount(r.remainAmount),
  }))
)

/** 统计卡片（基于当前页数据计算） */
const statsCards = computed(() => {
  const all = rawRecords.value
  const totalAmount = all.reduce((s, r) => s + (r.amount || 0), 0)
  const received = all.reduce((s, r) => s + (r.receivedAmount || 0), 0)
  const overdue = all.filter(r => r.status === 'overdue').reduce((s, r) => s + (r.remainAmount || 0), 0)
  const pending = all.filter(r => r.status !== 'settled').reduce((s, r) => s + (r.remainAmount || 0), 0)
  return [
    { icon: 'Wallet', label: '应收总额', value: formatYuan(totalAmount), colorType: '' as StatColorType },
    { icon: 'Check', label: '已收款', value: formatYuan(received), colorType: '' as StatColorType },
    { icon: 'Warning', label: '逾期未收', value: formatYuan(overdue), colorType: '' as StatColorType },
    { icon: 'Clock', label: '待收款', value: formatYuan(pending), colorType: '' as StatColorType },
  ]
})

const columns = [
  { prop: 'receivableNo', label: '应收编号', minWidth: 140 },
  { prop: 'customerName', label: '客户名称', minWidth: 150 },
  { prop: 'amount', label: '应收金额(元)', minWidth: 130 },
  { prop: 'receivedAmount', label: '已收金额(元)', minWidth: 130 },
  { prop: 'remainAmount', label: '未收金额(元)', minWidth: 130 },
  { prop: 'dueDate', label: '到期日', minWidth: 110 },
  { prop: 'aging', label: '账龄', minWidth: 95 },
  { prop: 'status', label: '状态', minWidth: 95, slot: 'status' },
]

/** 加载应收账款列表 */
async function loadData() {
  loading.value = true
  try {
    const params: FinanceReceivableQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.status) params.status = searchForm.value.status
    if (searchForm.value.keyword) params.customerName = searchForm.value.keyword
    const response = await receivableApi.getList(params)
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
  searchForm.value = { status: '', keyword: '' }
}

onMounted(() => {
  loadData()
})

/** 详情对话框 */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

/** 收款对话框 */
const receiptDialogVisible = ref(false)
/** 当前选中的应收账款（用于收款对话框） */
const currentReceivable = ref<FinanceReceivable | null>(null)
/** 收款历史对话框 */
const historyDialogVisible = ref(false)
/** 收款历史查询的应收账款ID */
const historyReceivableId = ref<string | null>(null)

/** 打开详情对话框 */
function handleViewDetail(row: Record<string, unknown>): void {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}

/** 打开收款对话框 */
function handleReceipt(row: Record<string, unknown>): void {
  // 从原始数据中找到对应的应收账款记录
  const receivableId = String(row.id ?? '')
  const target = rawRecords.value.find(r => String(r.id) === receivableId) || null
  currentReceivable.value = target
  receiptDialogVisible.value = true
}

/** 打开收款历史对话框 */
function handleViewHistory(row: Record<string, unknown>): void {
  historyReceivableId.value = String(row.id ?? '')
  historyDialogVisible.value = true
}

/** 收款成功回调：关闭对话框并刷新列表 */
function handleReceiptSuccess(): void {
  receiptDialogVisible.value = false
  currentReceivable.value = null
  loadData()
}

/** 新增弹窗可见性 */
const createDialogVisible = ref(false)

/** 新增表单数据（金额单位：元） */
const createForm = ref({
  customerName: '',
  amount: undefined as number | undefined,
  dueDate: '',
  remark: '',
})

/** 打开新增弹窗，重置表单 */
function handleCreate(): void {
  createForm.value = { customerName: '', amount: undefined, dueDate: '', remark: '' }
  createDialogVisible.value = true
}

/** 提交新增应收账款 */
async function handleSubmit(): Promise<void> {
  const form = createForm.value
  if (!form.customerName) {
    ElMessage.error('请输入客户名称')
    return
  }
  if (form.amount === undefined || form.amount === null || form.amount <= 0) {
    ElMessage.error('请输入有效的应收金额')
    return
  }
  if (!form.dueDate) {
    ElMessage.error('请选择到期日')
    return
  }
  try {
    // 金额保持元单位，由 ReceivableDataConverter.toCreateDTO 统一转为分（避免双倍转换）
    await receivableApi.create({
      customerName: form.customerName,
      amount: form.amount,
      dueDate: form.dueDate,
      remark: form.remark || undefined,
    } as FinanceReceivableFormData)
    ElMessage.success('新增应收账款成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '创建失败')
    }
  }
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="应收账款" description="客户应收账款管理与催收">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增应收
      </el-button>
    </PageHeader>
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.status" placeholder="收款状态" clearable style="width:140px" size="default">
            <el-option label="未收" value="unpaid" />
            <el-option label="部分收" value="partial" />
            <el-option label="已结清" value="settled" />
            <el-option label="已逾期" value="overdue" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="搜索客户名称" clearable style="width:180px" size="default" @keyup.enter="handleSearch" />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>
    <div class="table-section">
      <DataTable :columns="columns" :data="records" :loading="loading" stripe :actions-width="220">
        <template #status="{ row }">
          <StatusTag :status="getStatusTag(row.status).type" :label="getStatusTag(row.status).label" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button v-if="row.status !== 'settled'" link type="primary" size="small" @click="handleReceipt(row)">收款</el-button>
          <el-button link type="primary" size="small" @click="handleViewHistory(row)">收款记录</el-button>
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
    <!-- 新增应收账款对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增应收账款" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="客户名称" required>
          <el-input v-model="createForm.customerName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="应收金额" required>
          <el-input-number v-model="createForm.amount" :min="0.01" :precision="2" placeholder="请输入应收金额" style="width:100%" />
        </el-form-item>
        <el-form-item label="到期日" required>
          <el-date-picker v-model="createForm.dueDate" type="date" placeholder="选择到期日" value-format="YYYY-MM-DD" style="width:100%" :teleported="false" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认创建</el-button>
      </template>
    </el-dialog>
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="应收账款详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="应收编号">{{ detailData.receivableNo as string }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detailData.customerName as string }}</el-descriptions-item>
        <el-descriptions-item label="应收金额(元)">¥{{ detailData.amount as string }}</el-descriptions-item>
        <el-descriptions-item label="已收金额(元)">¥{{ detailData.receivedAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="未收金额(元)">¥{{ detailData.remainAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="账龄">{{ detailData.aging as string }}</el-descriptions-item>
        <el-descriptions-item label="开票日期">{{ (detailData.invoiceDate as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期日">{{ detailData.dueDate as string }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusTag(detailData.status as ReceivableStatus).type" :label="getStatusTag(detailData.status as ReceivableStatus).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="发票号">{{ (detailData.invoiceNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 收款登记对话框 -->
    <ReceiptDialog
      v-model:visible="receiptDialogVisible"
      :receivable="currentReceivable"
      @success="handleReceiptSuccess"
    />
    <!-- 收款历史对话框 -->
    <ReceiptHistoryDialog
      v-model:visible="historyDialogVisible"
      :receivable-id="historyReceivableId"
    />
  </div>
</template>
