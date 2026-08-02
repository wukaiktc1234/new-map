<script setup lang="ts">
/**
 * 应付账款页面
 * 数据来源：/v1/payables
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { payableApi } from '@/api/finance'
import { supplierApi } from '@/api/purchase'
import type { FinancePayable, FinancePayableQueryForm, PayableStatus, FinancePayableFormData } from '@/types/finance'
import type { SupplierInfo } from '@/types/purchase-supplier'
import type { StatColorType } from '@/types/stat'
import PaymentDialog from './components/PaymentDialog.vue'
import PaymentHistoryDialog from './components/PaymentHistoryDialog.vue'

const { loading, pagination, handleSearch, handleReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

const searchForm = ref({ status: '' as '' | PayableStatus, keyword: '' })

/** 原始数据（金额已被 PayableDataConverter.toFrontend 转为元） */
const rawRecords = ref<FinancePayable[]>([])

/** 格式化元金额（保留2位小数，带千分位）
 * 注：rawRecords 中的 amount/paidAmount/remainAmount 已被 Converter 转为元，此处仅做展示格式化 */
function formatAmount(yuan: number | undefined | null): string {
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 格式化元金额（带¥前缀） */
function formatYuan(yuan: number | undefined | null): string {
  return `¥${formatAmount(yuan)}`
}

/** 应付账款状态 → StatusTag 配置（避免模板内三元映射） */
function getStatusTag(status: PayableStatus): { type: string; label: string } {
  const map: Record<PayableStatus, { type: string; label: string }> = {
    settled: { type: 'success', label: '已结清' },
    partial: { type: 'warning', label: '部分付' },
    overdue: { type: 'error', label: '已逾期' },
    unpaid: { type: 'info', label: '未付' },
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

/** 表格展示数据（金额已被 Converter 转为元，此处仅做展示格式化） */
const records = computed(() =>
  rawRecords.value.map(r => ({
    ...r,
    amount: formatAmount(r.amount),
    paidAmount: formatAmount(r.paidAmount),
    remainAmount: formatAmount(r.remainAmount),
  }))
)

/** 统计卡片（基于当前页数据计算） */
const statsCards = computed(() => {
  const all = rawRecords.value
  const totalAmount = all.reduce((s, r) => s + (r.amount || 0), 0)
  const paid = all.reduce((s, r) => s + (r.paidAmount || 0), 0)
  const overdue = all.filter(r => r.status === 'overdue').reduce((s, r) => s + (r.remainAmount || 0), 0)
  const pending = all.filter(r => r.status !== 'settled').reduce((s, r) => s + (r.remainAmount || 0), 0)
  return [
    { icon: 'Wallet', label: '应付总额', value: formatYuan(totalAmount), colorType: '' as StatColorType },
    { icon: 'Check', label: '已付款', value: formatYuan(paid), colorType: '' as StatColorType },
    { icon: 'Warning', label: '逾期未付', value: formatYuan(overdue), colorType: '' as StatColorType },
    { icon: 'Clock', label: '待付款', value: formatYuan(pending), colorType: '' as StatColorType },
  ]
})

const columns = [
  { prop: 'payableNo', label: '应付款编号', minWidth: 140 },
  { prop: 'supplierName', label: '供应商', minWidth: 140 },
  { prop: 'orderNo', label: '采购订单号', minWidth: 150 },
  { prop: 'stockinNo', label: '入库单号', minWidth: 150 },
  { prop: 'amount', label: '应付金额(元)', minWidth: 130 },
  { prop: 'paidAmount', label: '已付金额(元)', minWidth: 130 },
  { prop: 'remainAmount', label: '未付金额(元)', minWidth: 130 },
  { prop: 'dueDate', label: '到期日', minWidth: 110 },
  { prop: 'aging', label: '账龄', minWidth: 95 },
  { prop: 'status', label: '状态', minWidth: 95, slot: 'status' },
]

/** 加载应付账款列表 */
async function loadData() {
  loading.value = true
  try {
    const params: FinancePayableQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.status) params.status = searchForm.value.status
    if (searchForm.value.keyword) params.supplierName = searchForm.value.keyword
    const response = await payableApi.getList(params)
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

/** 付款对话框 */
const paymentDialogVisible = ref(false)
const currentPayable = ref<FinancePayable | null>(null)

/** 付款历史对话框 */
const historyDialogVisible = ref(false)
const historyPayableId = ref<string | null>(null)

/** 打开付款对话框（必须使用 rawRecords 中的原始数字金额，避免格式化字符串导致计算异常） */
function handlePay(row: FinancePayable): void {
  const raw = rawRecords.value.find(r => r.id === row.id)
  currentPayable.value = raw || row
  paymentDialogVisible.value = true
}

/** 查看付款记录 */
function handleViewHistory(row: FinancePayable): void {
  historyPayableId.value = row.id
  historyDialogVisible.value = true
}

/** 付款成功回调 */
function handlePaymentSuccess(): void {
  paymentDialogVisible.value = false
  loadData()
}

/** 详情对话框 */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

/** 打开详情对话框（展示层使用格式化后的字符串即可） */
function handleViewDetail(row: Record<string, unknown>): void {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}

/** 新增弹窗可见性 */
const createDialogVisible = ref(false)

/** 新增表单数据（金额单位：元） */
const createForm = ref({
  supplierName: '',
  amount: undefined as number | undefined,
  dueDate: '',
  remark: '',
})

/** 打开新增弹窗，重置表单 */
function handleCreate(): void {
  createForm.value = { supplierName: '', amount: undefined, dueDate: '', remark: '' }
  createDialogVisible.value = true
}

/** 供应商选项列表 */
const supplierOptions = ref<{ label: string; value: string }[]>([])

/** 加载供应商选项 */
async function loadSupplierOptions(): Promise<void> {
  try {
    const res = await supplierApi.getList({ page: 1, size: 200 })
    supplierOptions.value = (res?.records || []).map((s: SupplierInfo) => ({
      label: s.supplierName,
      value: s.supplierName,
    }))
  } catch {
    // 供应商API不可用时，允许用户手动输入
  }
}

/** 提交新增应付账款 */
async function handleSubmit(): Promise<void> {
  const form = createForm.value
  if (!form.supplierName) {
    ElMessage.error('请输入供应商名称')
    return
  }
  if (form.amount === undefined || form.amount === null || form.amount <= 0) {
    ElMessage.error('请输入有效的应付金额')
    return
  }
  if (!form.dueDate) {
    ElMessage.error('请选择到期日')
    return
  }
  try {
    // 金额保持元单位，由 PayableDataConverter.toCreateDTO 统一转为分（避免双倍转换）
    await payableApi.create({
      supplierName: form.supplierName,
      amount: form.amount,
      dueDate: form.dueDate,
      remark: form.remark || undefined,
    } as FinancePayableFormData)
    ElMessage.success('新增应付账款成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '创建失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadSupplierOptions()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="应付账款" description="供应商应付账款管理">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增应付
      </el-button>
    </PageHeader>
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.status" placeholder="付款状态" clearable style="width:140px" size="default">
            <el-option label="未付" value="unpaid" />
            <el-option label="部分付" value="partial" />
            <el-option label="已结清" value="settled" />
            <el-option label="已逾期" value="overdue" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="搜索供应商" clearable style="width:170px" size="default" @keyup.enter="handleSearch" />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>
    <div class="table-section">
      <DataTable :columns="columns" :data="records" :loading="loading" stripe :actions-width="280">
        <template #status="{ row }">
          <StatusTag :status="getStatusTag(row.status).type" :label="getStatusTag(row.status).label" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button v-if="row.status !== 'settled'" link type="primary" size="small" @click="handlePay(row)">付款</el-button>
          <el-button link type="primary" size="small" @click="handleViewHistory(row)">付款记录</el-button>
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
    <!-- 新增应付账款对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增应付账款" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="供应商名称" required>
          <el-select v-model="createForm.supplierName" placeholder="请选择或输入供应商" filterable allow-create clearable default-first-option style="width:100%" :teleported="false">
            <el-option v-for="s in supplierOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="应付金额" required>
          <el-input-number v-model="createForm.amount" :min="0.01" :precision="2" placeholder="请输入应付金额" style="width:100%" />
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
    <el-dialog v-model="detailDialogVisible" title="应付账款详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="应付编号">{{ detailData.payableNo as string }}</el-descriptions-item>
        <el-descriptions-item label="采购订单号">{{ (detailData.orderNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库单号">{{ (detailData.stockinNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="供应商名称">{{ detailData.supplierName as string }}</el-descriptions-item>
        <el-descriptions-item label="应付金额(元)">¥{{ detailData.amount as string }}</el-descriptions-item>
        <el-descriptions-item label="已付金额(元)">¥{{ detailData.paidAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="未付金额(元)">¥{{ detailData.remainAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="账龄">{{ detailData.aging as string }}</el-descriptions-item>
        <el-descriptions-item label="开票日期">{{ (detailData.invoiceDate as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期日">{{ detailData.dueDate as string }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusTag(detailData.status as PayableStatus).type" :label="getStatusTag(detailData.status as PayableStatus).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="发票号">{{ (detailData.invoiceNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 付款登记对话框 -->
    <PaymentDialog
      v-model:visible="paymentDialogVisible"
      :payable="currentPayable"
      @success="handlePaymentSuccess"
    />
    <!-- 付款历史对话框 -->
    <PaymentHistoryDialog
      v-model:visible="historyDialogVisible"
      :payable-id="historyPayableId"
      @void-success="loadData"
    />
  </div>
</template>
