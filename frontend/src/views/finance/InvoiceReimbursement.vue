<script setup lang="ts">
/**
 * 发票报销页面
 * 数据来源：/v1/invoices
 */
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { invoiceApi, fenToYuanNumber, yuanToFen } from '@/api/finance'
import type { FinanceInvoiceNew, FinanceInvoiceFormData, FinanceInvoiceQueryForm, FinanceInvoiceType, FinanceInvoiceStatus } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { loading, pagination, handleSearch, handleReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

const searchForm = ref({
  invoiceNo: '',
  invoiceType: '' as '' | FinanceInvoiceType,
  dateRange: null as [string, string] | null,
})

/** 原始数据（金额单位：分） */
const rawRecords = ref<FinanceInvoiceNew[]>([])

/** 发票类型中文映射 */
const invoiceTypeLabelMap: Record<FinanceInvoiceType, string> = {
  special: '专票',
  normal: '普票',
  electronic: '电子普票',
  electronic_special: '电子专票',
}

/** 分转元（保留2位小数，带千分位） */
function formatFen(fen: number | undefined | null): string {
  return fenToYuanNumber(fen).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 分转元（带¥前缀） */
function formatYuan(fen: number | undefined | null): string {
  return `¥${formatFen(fen)}`
}

/** 发票状态 → StatusTag 配置（避免模板内三元映射） */
function getStatusTag(status: FinanceInvoiceStatus): { type: string; label: string } {
  const map: Record<FinanceInvoiceStatus, { type: string; label: string }> = {
    issued: { type: 'success', label: '已开具' },
    draft: { type: 'warning', label: '草稿' },
    red_flushed: { type: 'error', label: '红冲' },
    cancelled: { type: 'info', label: '已作废' },
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

/** 表格展示数据（金额分→元格式化，发票类型转中文） */
const records = computed(() =>
  rawRecords.value.map(r => ({
    ...r,
    invoiceType: invoiceTypeLabelMap[r.invoiceType] || r.invoiceType,
    amountWithoutTax: formatFen(r.amountWithoutTax),
    taxAmount: formatFen(r.taxAmount),
    totalAmount: formatFen(r.totalAmount),
  }))
)

/** 统计卡片（基于当前页数据计算） */
const statsCards = computed(() => {
  const all = rawRecords.value
  const totalAmount = all.reduce((s, r) => s + (r.totalAmount || 0), 0)
  const issued = all.filter(r => r.status === 'issued').length
  const draft = all.filter(r => r.status === 'draft').length
  const totalCount = all.length
  return [
    { icon: 'Document', label: '发票总额', value: formatYuan(totalAmount), colorType: 'primary' as StatColorType },
    { icon: 'Check', label: '已开具', value: issued, colorType: 'success' as StatColorType },
    { icon: 'Clock', label: '草稿', value: draft, colorType: 'warning' as StatColorType },
    { icon: 'Star', label: '发票总数', value: totalCount, colorType: 'info' as StatColorType },
  ]
})

const columns = [
  { prop: 'invoiceNo', label: '发票编号', minWidth: 140 },
  { prop: 'invoiceType', label: '发票类型', minWidth: 100 },
  { prop: 'invoiceDate', label: '开票日期', minWidth: 110 },
  { prop: 'buyerName', label: '购方名称', minWidth: 140 },
  { prop: 'sellerName', label: '销方名称', minWidth: 140 },
  { prop: 'amountWithoutTax', label: '金额(元)', minWidth: 120 },
  { prop: 'taxAmount', label: '税额(元)', minWidth: 110 },
  { prop: 'totalAmount', label: '价税合计(元)', minWidth: 130 },
  { prop: 'status', label: '状态', minWidth: 95, slot: 'status' },
]

/** 加载发票列表 */
async function loadData() {
  loading.value = true
  try {
    const params: FinanceInvoiceQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.invoiceNo) params.invoiceNo = searchForm.value.invoiceNo
    if (searchForm.value.invoiceType) params.invoiceType = searchForm.value.invoiceType
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = searchForm.value.dateRange[0]
      params.endDate = searchForm.value.dateRange[1]
    }
    const response = await invoiceApi.getList(params)
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
  searchForm.value = { invoiceNo: '', invoiceType: '', dateRange: null }
}

/** 审批对话框 */
const approveDialogVisible = ref(false)
const approveForm = ref({ invoiceId: '', invoiceNo: '', buyerName: '', totalAmount: 0, action: 'approve' as 'approve' | 'reject', opinion: '' })
const approveSubmitting = ref(false)

/** 打开审批对话框 */
function handleApprove(row: FinanceInvoiceNew): void {
  approveForm.value = {
    invoiceId: row.id,
    invoiceNo: row.invoiceNo,
    buyerName: row.buyerName,
    totalAmount: row.totalAmount,
    action: 'approve',
    opinion: '',
  }
  approveDialogVisible.value = true
}

/** 确认审批（使用发票ID而非发票号调用更新接口） */
async function confirmApprove(): Promise<void> {
  if (approveForm.value.action === 'reject' && !approveForm.value.opinion) {
    ElMessage.warning('驳回时必须填写审批意见')
    return
  }
  approveSubmitting.value = true
  try {
    await invoiceApi.update(approveForm.value.invoiceId, {
      status: approveForm.value.action === 'approve' ? 'issued' : 'cancelled',
      remark: approveForm.value.opinion,
    } as unknown as FinanceInvoiceFormData)
    ElMessage.success(approveForm.value.action === 'approve' ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('操作失败，请重试')
  } finally {
    approveSubmitting.value = false
  }
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

/** 新增报销对话框 */
const createDialogVisible = ref(false)
const createSubmitting = ref(false)

/** 发票类型选项（复用已有 invoiceTypeLabelMap） */
const invoiceTypeOptions = computed(() =>
  Object.entries(invoiceTypeLabelMap).map(([value, label]) => ({ value: value as FinanceInvoiceType, label }))
)

/** 新增报销表单（金额单位：元，提交时转分） */
const createForm = reactive({
  invoiceNo: '',
  invoiceCode: '',
  invoiceType: 'normal' as FinanceInvoiceType,
  invoiceDate: '',
  buyerName: '',
  buyerTaxNo: '',
  sellerName: '',
  sellerTaxNo: '',
  amountWithoutTax: 0,
  taxRate: 0,
  remark: '',
})

/** 根据不含税金额和税率自动计算税额和价税合计（单位：元） */
const computedTaxAmount = computed(() => {
  const amount = Number(createForm.amountWithoutTax) || 0
  const rate = Number(createForm.taxRate) || 0
  return Math.round(amount * rate) / 100
})

const computedTotalAmount = computed(() => {
  const amount = Number(createForm.amountWithoutTax) || 0
  return Math.round((amount + computedTaxAmount.value) * 100) / 100
})

/** 重置新增表单 */
function resetCreateForm(): void {
  createForm.invoiceNo = ''
  createForm.invoiceCode = ''
  createForm.invoiceType = 'normal'
  createForm.invoiceDate = ''
  createForm.buyerName = ''
  createForm.buyerTaxNo = ''
  createForm.sellerName = ''
  createForm.sellerTaxNo = ''
  createForm.amountWithoutTax = 0
  createForm.taxRate = 0
  createForm.remark = ''
}

/** 打开新增报销对话框 */
function handleCreate(): void {
  resetCreateForm()
  // 默认开票日期为今天
  const today = new Date()
  createForm.invoiceDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  createDialogVisible.value = true
}

/** 提交新增报销申请 */
async function submitCreate(): Promise<void> {
  // 基础校验
  if (!createForm.invoiceNo.trim()) {
    ElMessage.warning('请输入发票号码')
    return
  }
  if (!createForm.invoiceDate) {
    ElMessage.warning('请选择开票日期')
    return
  }
  if (!createForm.buyerName.trim()) {
    ElMessage.warning('请输入购方名称')
    return
  }
  if (!createForm.sellerName.trim()) {
    ElMessage.warning('请输入销方名称')
    return
  }
  if (Number(createForm.amountWithoutTax) <= 0) {
    ElMessage.warning('不含税金额必须大于0')
    return
  }

  createSubmitting.value = true
  try {
    // 元转分后提交（后端金额单位为分）
    const payload: FinanceInvoiceFormData = {
      invoiceNo: createForm.invoiceNo.trim(),
      invoiceType: createForm.invoiceType,
      invoiceDate: createForm.invoiceDate,
      buyerName: createForm.buyerName.trim(),
      sellerName: createForm.sellerName.trim(),
      amountWithoutTax: yuanToFen(createForm.amountWithoutTax),
      taxAmount: yuanToFen(computedTaxAmount.value),
      totalAmount: yuanToFen(computedTotalAmount.value),
    }
    if (createForm.invoiceCode.trim()) payload.invoiceCode = createForm.invoiceCode.trim()
    if (createForm.buyerTaxNo.trim()) payload.buyerTaxNo = createForm.buyerTaxNo.trim()
    if (createForm.sellerTaxNo.trim()) payload.sellerTaxNo = createForm.sellerTaxNo.trim()
    if (Number(createForm.taxRate) > 0) payload.taxRate = Number(createForm.taxRate)
    if (createForm.remark.trim()) payload.remark = createForm.remark.trim()

    await invoiceApi.create(payload)
    ElMessage.success('报销申请已提交')
    createDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    createSubmitting.value = false
  }
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="发票报销" description="员工发票报销申请与审批">
      <template #extra>
        <el-button type="primary" size="default" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>新增报销
        </el-button>
      </template>
    </PageHeader>
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input v-model="searchForm.invoiceNo" placeholder="搜索发票号码" clearable style="width:170px" size="default" @keyup.enter="handleSearch" />
          <el-select v-model="searchForm.invoiceType" placeholder="发票类型" clearable style="width:130px" size="default">
            <el-option label="专票" value="special" />
            <el-option label="普票" value="normal" />
            <el-option label="电子普票" value="electronic" />
            <el-option label="电子专票" value="electronic_special" />
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
      <DataTable :columns="columns" :data="records" :loading="loading" stripe>
        <template #status="{ row }">
          <StatusTag :status="getStatusTag(row.status).type" :label="getStatusTag(row.status).label" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button v-if="row.status === 'draft'" link type="primary" size="small" @click="handleApprove(row)">审批</el-button>
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
    <el-dialog v-model="detailDialogVisible" title="发票详情" width="640px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="发票编号">{{ detailData.invoiceNo as string }}</el-descriptions-item>
        <el-descriptions-item label="发票代码">{{ (detailData.invoiceCode as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发票类型">{{ detailData.invoiceType as string }}</el-descriptions-item>
        <el-descriptions-item label="开票日期">{{ detailData.invoiceDate as string }}</el-descriptions-item>
        <el-descriptions-item label="购方名称">{{ detailData.buyerName as string }}</el-descriptions-item>
        <el-descriptions-item label="购方税号">{{ (detailData.buyerTaxNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="销方名称">{{ detailData.sellerName as string }}</el-descriptions-item>
        <el-descriptions-item label="销方税号">{{ (detailData.sellerTaxNo as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="不含税金额(元)">¥{{ detailData.amountWithoutTax as string }}</el-descriptions-item>
        <el-descriptions-item label="税额(元)">¥{{ detailData.taxAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="价税合计(元)">¥{{ detailData.totalAmount as string }}</el-descriptions-item>
        <el-descriptions-item label="税率">{{ (detailData.taxRate as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusTag(detailData.status as FinanceInvoiceStatus).type" :label="getStatusTag(detailData.status as FinanceInvoiceStatus).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 审批对话框 -->
    <el-dialog v-model="approveDialogVisible" title="发票审批" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="发票编号">{{ approveForm.invoiceNo }}</el-form-item>
        <el-form-item label="购方名称">{{ approveForm.buyerName }}</el-form-item>
        <el-form-item label="价税合计">¥{{ formatFen(approveForm.totalAmount) }}</el-form-item>
        <el-form-item label="审批操作">
          <el-radio-group v-model="approveForm.action">
            <el-radio value="approve">通过</el-radio>
            <el-radio value="reject">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button :type="approveForm.action === 'approve' ? 'primary' : 'danger'" :loading="approveSubmitting" @click="confirmApprove">
          {{ approveForm.action === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
    <!-- 新增报销对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增报销申请" width="640px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="发票号码" required>
          <el-input v-model="createForm.invoiceNo" placeholder="请输入发票号码" style="width: 220px" />
        </el-form-item>
        <el-form-item label="发票代码">
          <el-input v-model="createForm.invoiceCode" placeholder="选填" style="width: 220px" />
        </el-form-item>
        <el-form-item label="发票类型" required>
          <el-select v-model="createForm.invoiceType" placeholder="请选择" style="width: 220px" :teleported="false">
            <el-option v-for="opt in invoiceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开票日期" required>
          <el-date-picker
            v-model="createForm.invoiceDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 220px"
            :teleported="false"
          />
        </el-form-item>
        <el-form-item label="购方名称" required>
          <el-input v-model="createForm.buyerName" placeholder="请输入购方名称" style="width: 360px" />
        </el-form-item>
        <el-form-item label="购方税号">
          <el-input v-model="createForm.buyerTaxNo" placeholder="选填" style="width: 220px" />
        </el-form-item>
        <el-form-item label="销方名称" required>
          <el-input v-model="createForm.sellerName" placeholder="请输入销方名称" style="width: 360px" />
        </el-form-item>
        <el-form-item label="销方税号">
          <el-input v-model="createForm.sellerTaxNo" placeholder="选填" style="width: 220px" />
        </el-form-item>
        <el-form-item label="不含税金额(元)" required>
          <el-input-number
            v-model="createForm.amountWithoutTax"
            :min="0"
            :precision="2"
            :step="100"
            placeholder="请输入金额"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="税率(%)">
          <el-input-number
            v-model="createForm.taxRate"
            :min="0"
            :max="100"
            :precision="2"
            :step="1"
            placeholder="如 13"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="税额(元)">
          <span>{{ computedTaxAmount.toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="价税合计(元)">
          <span>{{ computedTotalAmount.toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="选填" style="width: 360px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>
