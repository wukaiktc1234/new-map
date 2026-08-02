<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { taxRateApi } from '@/api/finance'
import { taxCalculationApi } from '@/api/finance/tax-calculation'
import type { TaxRateConfig, TaxType } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

/** 当前激活的Tab */
const activeTab = ref('records')

// ============================================================
// Tab1 - 税务记录
// ============================================================
const recordLoading = ref(false)
const recordSearchForm = reactive({
  taxType: '' as string,
  taxPeriod: '',
  taxStatus: '',
})

/**
 * 税务记录列表
 * TODO: 税务记录接口待后端实现，当前无对应 API（taxCalculationApi 仅提供申报表相关接口）。
 * 为避免偷漏税风险，禁止展示假税务数据，初始化为空列表。
 */
const taxRecords = ref<Record<string, unknown>[]>([])

const recordColumns = [
  { prop: 'taxTypeName', label: '税种', minWidth: 120 },
  { prop: 'taxPeriod', label: '纳税期间', minWidth: 100 },
  { prop: 'taxAmount', label: '应纳税额', minWidth: 120, slot: 'taxAmount' },
  { prop: 'paidAmount', label: '已缴税额', minWidth: 120, slot: 'paidAmount' },
  { prop: 'taxStatus', label: '状态', minWidth: 100, slot: 'taxStatus' },
  { prop: 'dueDate', label: '到期日', minWidth: 110 },
]

/** 税种选项（用于搜索和计算） */
const taxTypeOptions = [
  { label: '增值税', value: 'VAT' },
  { label: '企业所得税', value: 'INCOME_TAX' },
]

/** 税务状态选项 */
const taxStatusOptions = [
  { label: '未缴纳', value: 'UNPAID' },
  { label: '已缴纳', value: 'PAID' },
  { label: '逾期', value: 'OVERDUE' },
]

/** 格式化金额 */
function formatAmount(val: unknown): string {
  const num = Number(val)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

/** 状态映射 */
function getStatusByTaxStatus(status: string): string {
  const map: Record<string, string> = {
    UNPAID: 'warning',
    PAID: 'success',
    OVERDUE: 'error',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    UNPAID: '未缴纳',
    PAID: '已缴纳',
    OVERDUE: '逾期',
  }
  return map[status] || status
}

/** 获取申报状态类型 */
function getReturnStatusType(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    SUBMITTED: 'warning',
    PAID: 'success',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function getReturnStatusLabel(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    PAID: '已缴款',
    CANCELLED: '已作废',
  }
  return map[status] || status
}

/** 查询税务记录 */
function handleRecordSearch(): void {
  // TODO: 税务记录接口待后端实现，当前 taxRecords 为空列表，查询操作仅刷新统计卡片
  recordLoading.value = true
  updateStatsCards()
  recordLoading.value = false
}

/** 查看税务记录详情 */
const recordDetailDialogVisible = ref(false)
const recordDetailData = ref<Record<string, unknown>>({})
function handleViewRecordDetail(row: Record<string, unknown>): void {
  recordDetailData.value = { ...row }
  recordDetailDialogVisible.value = true
}

/** 缴税操作 */
function handlePayTax(row: Record<string, unknown>): void {
  ElMessageBox.confirm(`确认缴纳 ${row.taxTypeName} ${row.taxPeriod} 期的税款 ${formatAmount(row.taxAmount)} 元？`, '缴税确认', {
    confirmButtonText: '确认缴税',
    cancelButtonText: '取消',
    type: 'info',
  }).then(() => {
    ElMessage.success('缴税操作已提交')
  }).catch(() => {
    // 取消操作
  })
}

// ============================================================
// Tab2 - 纳税申报
// ============================================================
const declarationLoading = ref(false)
const declarationForm = reactive({
  taxType: 'VAT',
  period: '202606',
})

/** 纳税申报表列表（从后端 API 加载） */
const declarationRecords = ref<Record<string, unknown>[]>([])

/** 加载纳税申报表列表（调用真实 API） */
async function loadDeclarationRecords(): Promise<void> {
  declarationLoading.value = true
  try {
    const res = await taxCalculationApi.getReturns({})
    const data = res as { records?: Record<string, unknown>[] } | Record<string, unknown>[]
    // 兼容分页响应和数组响应两种格式
    if (Array.isArray(data)) {
      declarationRecords.value = data
    } else if (data && Array.isArray(data.records)) {
      declarationRecords.value = data.records
    } else {
      declarationRecords.value = []
    }
  } catch (error: unknown) {
    declarationRecords.value = []
    if (error instanceof Error) {
      console.error('[FinanceTax] 加载申报表失败:', error.message)
    }
  } finally {
    declarationLoading.value = false
  }
}

const declarationColumns = [
  { prop: 'returnNo', label: '申报表编号', minWidth: 180 },
  { prop: 'taxTypeName', label: '税种', minWidth: 100 },
  { prop: 'period', label: '期间', minWidth: 90 },
  { prop: 'totalTaxPayable', label: '应纳税额', minWidth: 120, slot: 'totalTaxPayable' },
  { prop: 'generateDate', label: '生成日期', minWidth: 110 },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/** 计算税款 */
const calculationResult = ref<Record<string, unknown> | null>(null)
const calcDialogVisible = ref(false)
async function handleCalculate(): Promise<void> {
  declarationLoading.value = true
  try {
    const res = await taxCalculationApi.calculate(declarationForm.taxType, {
      year: declarationForm.period.substring(0, 4),
      month: declarationForm.period.substring(4, 6),
    })
    calculationResult.value = res as Record<string, unknown>
    calcDialogVisible.value = true
  } catch (error: unknown) {
    // 修复：移除 Mock 降级（原代码在 catch 中使用模拟税款数据，存在偷漏税风险）
    // 改为显示真实错误信息
    if (error instanceof Error) {
      ElMessage.error(error.message || '税款计算失败，请重试')
    } else {
      ElMessage.error('税款计算失败，请重试')
    }
  } finally {
    declarationLoading.value = false
  }
}

/** 生成申报表 */
async function handleGenerateReturn(): Promise<void> {
  // 修复 P0：原代码硬编码 totalTax（VAT?8905:10000），属于伪造税务数据（偷漏税风险）。
  // 现改为从 handleCalculate 的真实计算结果中提取应纳税额。
  // 若用户未先执行"计算税款"，则提示先计算，禁止使用假数据生成申报表。
  if (!calculationResult.value) {
    ElMessage.warning('请先点击"计算税款"获取真实应纳税额，再生成申报表')
    return
  }
  // 根据税种从计算结果中提取真实应纳税额
  const calcData = calculationResult.value
  const realTotalTax = declarationForm.taxType === 'VAT'
    ? Number(calcData.vatPayable)
    : Number(calcData.incomeTaxPayable)
  if (!realTotalTax || isNaN(realTotalTax)) {
    ElMessage.error('未能获取有效的应纳税额，请重新计算税款')
    return
  }
  declarationLoading.value = true
  try {
    await taxCalculationApi.generateReturn({
      taxType: declarationForm.taxType,
      period: declarationForm.period,
      totalTax: realTotalTax,
    })
    ElMessage.success('纳税申报表生成成功')
    // 生成成功后刷新申报表列表
    await loadDeclarationRecords()
  } catch (error: unknown) {
    // 修复 P0：原代码在 catch 块中显示"生成成功（模拟）"，错误处理显示成功属于合规风险
    // 改为 ElMessage.error 正确错误提示
    if (error instanceof Error) {
      ElMessage.error(error.message || '纳税申报表生成失败，请重试')
    } else {
      ElMessage.error('纳税申报表生成失败，请重试')
    }
  } finally {
    declarationLoading.value = false
  }
}

/** 查看申报表详情 */
const returnDetailDialogVisible = ref(false)
const returnDetailData = ref<Record<string, unknown>>({})
async function handleViewReturnDetail(row: Record<string, unknown>): Promise<void> {
  try {
    const res = await taxCalculationApi.getReturnDetail(String(row.returnId))
    returnDetailData.value = { ...row, ...(res as Record<string, unknown>) }
  } catch {
    returnDetailData.value = { ...row }
  }
  returnDetailDialogVisible.value = true
}

/** 提交到电子税务局 */
async function handleSubmitToBureau(row: Record<string, unknown>): Promise<void> {
  ElMessageBox.confirm(`确认将 ${row.returnNo} 提交到电子税务局？`, '提交确认', {
    confirmButtonText: '确认提交',
    cancelButtonText: '取消',
    type: 'info',
  }).then(async () => {
    try {
      const res = await taxCalculationApi.submitToBureau(row as Record<string, unknown>)
      const data = res as { message?: string }
      ElMessage.info(data?.message || '电子税务局对接功能需要配置税务系统授权后使用')
    } catch {
      // 电子税务局为外部系统对接，需配置授权证书后开通
      ElMessage.info('电子税务局对接需配置税务系统授权，请联系管理员开通')
    }
  }).catch(() => {
    // 取消操作
  })
}

// ============================================================
// Tab3 - 税率配置（保持原有功能）
// ============================================================
const { pagination, resetPagination } = useStandardPage()
const searchForm = ref({
  taxType: '' as TaxType | '',
  effective: '' as '' | 'true' | 'false',
})
const loading = ref(false)
const records = ref<TaxRateConfig[]>([])
const taxTypeOptionsForSearch: Array<{ label: string; value: TaxType }> = [
  { label: '增值税', value: 'vat' },
  { label: '企业所得税', value: 'corporate_income' },
  { label: '城市维护建设税', value: 'urban_construction' },
  { label: '教育费附加', value: 'education_surcharge' },
  { label: '地方教育附加', value: 'local_education' },
  { label: '印花税', value: 'stamp' },
  { label: '个人所得税', value: 'individual_income' },
]

const statsCards = ref<Array<{ icon: string; label: string; value: string; colorType: StatColorType }>>([
  { icon: 'Coin', label: '待缴税额', value: '¥0.00', colorType: 'warning' },
  { icon: 'SuccessFilled', label: '已缴税额', value: '¥0.00', colorType: 'success' },
  { icon: 'Timer', label: '即将到期', value: '0 笔', colorType: 'primary' },
  { icon: 'WarningFilled', label: '逾期未缴', value: '0 笔', colorType: 'error' },
])

const configColumns: DataTableColumn[] = [
  { prop: 'taxTypeName', label: '税种', minWidth: 140 },
  { prop: 'taxRate', label: '税率', minWidth: 90, align: 'center', slot: 'taxRate' },
  { prop: 'effectiveDate', label: '生效日期', minWidth: 120 },
  { prop: 'expiryDate', label: '失效日期', minWidth: 120 },
  { prop: 'effective', label: '状态', minWidth: 90, slot: 'effective' },
  { prop: 'remark', label: '备注', minWidth: 180, showOverflowTooltip: true },
  { prop: 'actions', label: '操作', width: 180, fixed: 'right', slot: 'actions' },
]

function buildQueryParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {
    page: pagination.current,
    size: pagination.size,
  }
  if (searchForm.value.taxType) {
    params.taxType = searchForm.value.taxType
  }
  if (searchForm.value.effective) {
    params.effective = searchForm.value.effective === 'true'
  }
  return params
}

function updateStatsCards(): void {
  const unpaid = taxRecords.value.filter(r => r.taxStatus === 'UNPAID').reduce((s, r) => s + Number(r.taxAmount), 0)
  const paid = taxRecords.value.filter(r => r.taxStatus === 'PAID').reduce((s, r) => s + Number(r.paidAmount), 0)
  const overdue = taxRecords.value.filter(r => r.taxStatus === 'OVERDUE')
  const dueCount = taxRecords.value.filter(r => r.taxStatus === 'UNPAID').length

  statsCards.value[0].value = `¥${unpaid.toFixed(2)}`
  statsCards.value[1].value = `¥${paid.toFixed(2)}`
  statsCards.value[2].value = `${dueCount} 笔`
  statsCards.value[3].value = `${overdue.length} 笔`
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const response = await taxRateApi.getList(buildQueryParams())
    records.value = response?.records || []
    pagination.total = response?.total || 0
  } catch {
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  resetPagination()
  loadData()
}

function handleReset(): void {
  searchForm.value = { taxType: '', effective: '' }
  resetPagination()
  loadData()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
  loadData()
}

function handleCurrentChange(current: number): void {
  pagination.current = current
  loadData()
}

function formatTaxRate(rate: number): string {
  return `${rate}%`
}

// 详情对话框
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

function handleViewDetail(row: Record<string, unknown>): void {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}

/** 税种名称映射 */
const taxTypeLabels: Record<string, string> = {
  vat: '增值税',
  corporate_income: '企业所得税',
  urban_construction: '城市维护建设税',
  education_surcharge: '教育费附加',
  local_education: '地方教育附加',
  stamp: '印花税',
  individual_income: '个人所得税',
}

// ============================================================
// 税率配置 - 新增/编辑/删除
// ============================================================
/** 新增/编辑税率配置弹窗 */
const configDialogVisible = ref(false)
const editingConfigId = ref<string | null>(null)
const configForm = ref({
  taxType: '' as TaxType | '',
  taxRate: 0,
  effectiveDate: '',
  expiryDate: '',
  remark: '',
})

/** 打开新增税率配置弹窗 */
function handleOpenAddConfig(): void {
  editingConfigId.value = null
  configForm.value = { taxType: '', taxRate: 0, effectiveDate: '', expiryDate: '', remark: '' }
  configDialogVisible.value = true
}

/** 打开编辑税率配置弹窗 */
function handleEditConfig(row: Record<string, unknown>): void {
  editingConfigId.value = row.id as string
  configForm.value = {
    taxType: (row.taxType as TaxType) || '',
    taxRate: (row.taxRate as number) || 0,
    effectiveDate: (row.effectiveDate as string) || '',
    expiryDate: (row.expiryDate as string) || '',
    remark: (row.remark as string) || '',
  }
  configDialogVisible.value = true
}

/** 提交税率配置（新增/编辑） */
async function handleSubmitConfig(): Promise<void> {
  const form = configForm.value
  if (!form.taxType) {
    ElMessage.error('请选择税种')
    return
  }
  if (!form.taxRate || form.taxRate <= 0) {
    ElMessage.error('请输入有效的税率')
    return
  }
  const submitData = {
    taxType: form.taxType as TaxType,
    taxTypeName: taxTypeLabels[form.taxType] || '',
    taxRate: form.taxRate,
    effectiveDate: form.effectiveDate,
    expiryDate: form.expiryDate || undefined,
    remark: form.remark || undefined,
  }
  try {
    if (editingConfigId.value) {
      await taxRateApi.update(editingConfigId.value, submitData)
      ElMessage.success('更新成功')
    } else {
      await taxRateApi.create(submitData)
      ElMessage.success('新增成功')
    }
    configDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

/** 删除税率配置 */
async function handleDeleteConfig(row: Record<string, unknown>): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除税种"${row.taxTypeName}"的税率配置？`, '确认删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await taxRateApi.delete(row.id as string)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

/** Tab切换时更新统计卡片 */
function handleTabChange(tab: string): void {
  if (tab === 'records' || tab === 'declaration') {
    updateStatsCards()
  }
}

onMounted(() => {
  loadData()
  updateStatsCards()
  // 加载纳税申报表列表（调用真实 API）
  loadDeclarationRecords()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="税务管理" description="税务计算、申报与缴纳管理" />

    <!-- 统计卡片 -->
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </div>

    <!-- Tab 切换 -->
    <div class="tab-section">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- Tab1 - 税务记录 -->
        <el-tab-pane label="税务记录" name="records">
          <div class="advanced-search-panel">
            <div class="toolbar-row">
              <div class="toolbar-left">
                <el-select v-model="recordSearchForm.taxType" placeholder="税种" clearable style="width: 160px">
                  <el-option v-for="opt in taxTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-select v-model="recordSearchForm.taxPeriod" placeholder="纳税期间" clearable style="width: 140px">
                  <el-option label="202606" value="202606" />
                  <el-option label="202605" value="202605" />
                  <el-option label="202604" value="202604" />
                  <el-option label="202603" value="202603" />
                </el-select>
                <el-select v-model="recordSearchForm.taxStatus" placeholder="状态" clearable style="width: 120px">
                  <el-option v-for="opt in taxStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </div>
              <div class="toolbar-right">
                <el-button type="primary" @click="handleRecordSearch">查询</el-button>
              </div>
            </div>
          </div>
          <div class="table-section">
            <DataTable :columns="recordColumns" :data="taxRecords" :loading="recordLoading" stripe>
              <template #taxAmount="{ row }">{{ formatAmount(row.taxAmount) }}</template>
              <template #paidAmount="{ row }">{{ formatAmount(row.paidAmount) }}</template>
              <template #taxStatus="{ row }">
                <StatusTag :status="getStatusByTaxStatus(row.taxStatus as string)" :label="getStatusLabel(row.taxStatus as string)" size="small" />
              </template>
              <template #actions="{ row }">
                <el-button link type="primary" size="small" @click="handleViewRecordDetail(row)">查看</el-button>
                <el-button v-if="row.taxStatus === 'UNPAID' || row.taxStatus === 'OVERDUE'" link type="primary" size="small" @click="handlePayTax(row)">缴税</el-button>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- Tab2 - 纳税申报 -->
        <el-tab-pane label="纳税申报" name="declaration">
          <div class="advanced-search-panel">
            <div class="toolbar-row">
              <div class="toolbar-left">
                <el-select v-model="declarationForm.taxType" placeholder="税种" style="width: 160px">
                  <el-option v-for="opt in taxTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-select v-model="declarationForm.period" placeholder="纳税期间" style="width: 140px">
                  <el-option label="2026年06月" value="202606" />
                  <el-option label="2026年05月" value="202605" />
                  <el-option label="2026年04月" value="202604" />
                  <el-option label="2026年03月" value="202603" />
                </el-select>
              </div>
              <div class="toolbar-right">
                <el-button type="primary" :loading="declarationLoading" @click="handleCalculate">计算税款</el-button>
                <el-button type="success" :loading="declarationLoading" @click="handleGenerateReturn">生成申报表</el-button>
              </div>
            </div>
          </div>
          <div class="table-section">
            <DataTable :columns="declarationColumns" :data="declarationRecords" :loading="declarationLoading" :actions-width="150" stripe>
              <template #totalTaxPayable="{ row }">{{ formatAmount(row.totalTaxPayable) }}</template>
              <template #status="{ row }">
                <StatusTag :status="getReturnStatusType(row.status as string)" :label="getReturnStatusLabel(row.status as string)" size="small" />
              </template>
              <template #actions="{ row }">
                <el-button link type="primary" size="small" @click="handleViewReturnDetail(row)">查看</el-button>
                <el-button v-if="row.status === 'DRAFT'" link type="primary" size="small" @click="handleSubmitToBureau(row)">提交电子税务局</el-button>
              </template>
            </DataTable>
          </div>
        </el-tab-pane>

        <!-- Tab3 - 税率配置 -->
        <el-tab-pane label="税率配置" name="config">
          <div class="advanced-search-panel">
            <div class="toolbar-row">
              <div class="toolbar-left">
                <el-select v-model="searchForm.taxType" placeholder="税种" clearable style="width: 160px">
                  <el-option v-for="opt in taxTypeOptionsForSearch" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-select v-model="searchForm.effective" placeholder="状态" clearable style="width: 120px">
                  <el-option label="有效" value="true" />
                  <el-option label="失效" value="false" />
                </el-select>
              </div>
              <div class="toolbar-right">
                <el-button type="primary" @click="handleOpenAddConfig">
                  <el-icon :size="16"><Plus /></el-icon>新增税率
                </el-button>
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
              </div>
            </div>
          </div>
          <div class="table-section">
            <DataTable :columns="configColumns" :data="records" :loading="loading" stripe>
              <template #taxRate="{ row }">{{ formatTaxRate(row.taxRate) }}</template>
              <template #effective="{ row }">
                <StatusTag :status="row.effective ? 'active' : 'inactive'" :label="row.effective ? '有效' : '失效'" size="small" />
              </template>
              <template #actions="{ row }">
                <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
                <el-button link type="primary" size="small" @click="handleEditConfig(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteConfig(row)">删除</el-button>
              </template>
            </DataTable>
          </div>
          <div class="pagination-wrapper">
            <el-pagination
              :current-page="pagination.current"
              :page-size="pagination.size"
              :total="pagination.total"
              layout="total,prev,pager,next,jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 税务记录详情对话框 -->
    <el-dialog v-model="recordDetailDialogVisible" title="税务记录详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="税种">{{ recordDetailData.taxTypeName as string }}</el-descriptions-item>
        <el-descriptions-item label="纳税期间">{{ recordDetailData.taxPeriod as string }}</el-descriptions-item>
        <el-descriptions-item label="应纳税额">{{ formatAmount(recordDetailData.taxAmount) }} 元</el-descriptions-item>
        <el-descriptions-item label="已缴税额">{{ formatAmount(recordDetailData.paidAmount) }} 元</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusByTaxStatus(recordDetailData.taxStatus as string)" :label="getStatusLabel(recordDetailData.taxStatus as string)" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="到期日">{{ (recordDetailData.dueDate as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ (recordDetailData.description as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="recordDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 税款计算结果对话框 -->
    <el-dialog v-model="calcDialogVisible" title="税款计算结果" width="600px">
      <template v-if="calculationResult">
        <el-descriptions :column="2" border label-class-name="detail-label">
          <el-descriptions-item label="税种">{{ calculationResult.taxTypeName as string }}</el-descriptions-item>
          <el-descriptions-item label="期间">{{ calculationResult.year }}-{{ calculationResult.month }}</el-descriptions-item>
          <template v-if="calculationResult.taxType === 'VAT'">
            <el-descriptions-item label="销售收入（含税）">{{ formatAmount(calculationResult.salesRevenue) }} 元</el-descriptions-item>
            <el-descriptions-item label="销项税额">{{ formatAmount(calculationResult.outputTax) }} 元</el-descriptions-item>
            <el-descriptions-item label="采购金额（含税）">{{ formatAmount(calculationResult.purchaseAmount) }} 元</el-descriptions-item>
            <el-descriptions-item label="进项税额">{{ formatAmount(calculationResult.inputTax) }} 元</el-descriptions-item>
            <el-descriptions-item label="应纳增值税" :span="2">
              <span style="font-weight: 700; color: var(--fts-primary)">{{ formatAmount(calculationResult.vatPayable) }} 元</span>
            </el-descriptions-item>
          </template>
          <template v-else>
            <el-descriptions-item label="收入总额">{{ formatAmount(calculationResult.totalRevenue) }} 元</el-descriptions-item>
            <el-descriptions-item label="成本总额">{{ formatAmount(calculationResult.totalCost) }} 元</el-descriptions-item>
            <el-descriptions-item label="营业费用">{{ formatAmount(calculationResult.operatingExpenses) }} 元</el-descriptions-item>
            <el-descriptions-item label="利润">{{ formatAmount(calculationResult.profit) }} 元</el-descriptions-item>
            <el-descriptions-item label="税率">{{ (calculationResult.taxRate as string) || '25%' }}</el-descriptions-item>
            <el-descriptions-item label="应纳所得税">
              <span style="font-weight: 700; color: var(--fts-primary)">{{ formatAmount(calculationResult.incomeTaxPayable) }} 元</span>
            </el-descriptions-item>
          </template>
          <el-descriptions-item label="计算时间" :span="2">{{ calculationResult.calculationTime as string }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button type="primary" @click="calcDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 申报表详情对话框 -->
    <el-dialog v-model="returnDetailDialogVisible" title="纳税申报表详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="申报表编号">{{ returnDetailData.returnNo as string }}</el-descriptions-item>
        <el-descriptions-item label="税种">{{ returnDetailData.taxTypeName as string }}</el-descriptions-item>
        <el-descriptions-item label="纳税期间">{{ returnDetailData.period as string }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getReturnStatusType(returnDetailData.status as string)" :label="getReturnStatusLabel(returnDetailData.status as string)" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="应纳税额" :span="2">
          <span style="font-weight: 700; color: var(--fts-primary)">{{ formatAmount(returnDetailData.totalTaxPayable) }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="生成日期">{{ returnDetailData.generateDate as string }}</el-descriptions-item>
        <el-descriptions-item label="提交状态">{{ (returnDetailData.submissionStatusName as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="returnDetailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 税率配置详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="税率配置详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="税种">{{ detailData.taxTypeName as string }}</el-descriptions-item>
        <el-descriptions-item label="税率">{{ formatTaxRate(detailData.taxRate as number) }}</el-descriptions-item>
        <el-descriptions-item label="生效日期">{{ detailData.effectiveDate as string }}</el-descriptions-item>
        <el-descriptions-item label="失效日期">{{ (detailData.expiryDate as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否有效">
          <StatusTag :status="detailData.effective ? 'active' : 'inactive'" :label="detailData.effective ? '有效' : '失效'" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑税率配置对话框 -->
    <el-dialog v-model="configDialogVisible" :title="editingConfigId ? '编辑税率配置' : '新增税率配置'" width="600px">
      <el-form :model="configForm" label-width="100px">
        <el-form-item label="税种" required>
          <el-select v-model="configForm.taxType" placeholder="请选择税种" style="width:100%" :teleported="false">
            <el-option v-for="opt in taxTypeOptionsForSearch" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="税率(%)" required>
          <el-input-number v-model="configForm.taxRate" :min="0.01" :max="100" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="生效日期" required>
          <el-date-picker v-model="configForm.effectiveDate" type="date" placeholder="选择生效日期" value-format="YYYY-MM-DD" style="width:100%" :teleported="false" />
        </el-form-item>
        <el-form-item label="失效日期">
          <el-date-picker v-model="configForm.expiryDate" type="date" placeholder="选择失效日期（可选）" value-format="YYYY-MM-DD" style="width:100%" :teleported="false" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="configForm.remark" type="textarea" :rows="3" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
