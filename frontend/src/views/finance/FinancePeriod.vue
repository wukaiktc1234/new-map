<script setup lang="ts">
/**
 * 会计期间管理页面
 * 数据来源：/v1/accounting-periods
 *
 * 功能：
 * - 分页查询会计期间列表（按期间类型/状态筛选）
 * - 新增会计期间（月度/季度/年度）
 * - 试算平衡（展示借贷平衡情况及科目明细）
 * - 结账检查清单（展示检查项完成情况）
 * - 损益结转、结账、反结账
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { usePermissionStore } from '@/stores/permission'
import { accountingPeriodApi } from '@/api/finance'
import type {
  AccountingPeriod,
  AccountingPeriodStatus,
  AccountingPeriodQueryForm,
  TrialBalanceResult,
  ClosingChecklistItem,
  ProfitTransferResult,
} from '@/types/finance'

defineOptions({ name: 'FinancePeriod' })

const { loading, pagination, handleSearch, handleReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/** 当前操作人ID（用于结账/反结账/损益结转接口的 operatorId 必填参数） */
const permissionStore = usePermissionStore()
const operatorId = String(permissionStore.userInfo?.userId ?? '1')

/* ===== 常量配置 ===== */

/** 期间类型标签映射：1=月度 2=季度 3=年度 */
const periodTypeLabelMap: Record<number, string> = {
  1: '月度',
  2: '季度',
  3: '年度',
}

/** 期间状态下拉选项 */
const statusOptions: Array<{ value: AccountingPeriodStatus; label: string }> = [
  { value: 'open', label: '开放' },
  { value: 'closing', label: '结账中' },
  { value: 'closed', label: '已结账' },
]

/** 期间类型下拉选项 */
const periodTypeOptions: Array<{ value: number; label: string }> = [
  { value: 1, label: '月度' },
  { value: 2, label: '季度' },
  { value: 3, label: '年度' },
]

/* ===== 数据状态 ===== */

const records = ref<AccountingPeriod[]>([])

const searchForm = ref({
  periodType: '' as '' | number,
  status: '' as '' | AccountingPeriodStatus,
})

/* ===== 工具函数 ===== */

/** 分转元（带千分位，保留2位小数） */
function formatFen(fen: number | undefined | null): string {
  if (fen === undefined || fen === null || isNaN(fen)) return '0.00'
  return (fen / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 获取期间类型标签 */
function getPeriodTypeLabel(periodType: number | undefined): string {
  if (periodType === undefined || periodType === null) return '月度'
  return periodTypeLabelMap[periodType] || '未知'
}

/** 获取期间状态 StatusTag 配置 */
function getStatusTagConfig(status: AccountingPeriodStatus): { status: string; label: string } {
  const map: Record<AccountingPeriodStatus, { status: string; label: string }> = {
    open: { status: 'success', label: '开放' },
    closing: { status: 'warning', label: '结账中' },
    closed: { status: 'info', label: '已结账' },
  }
  return map[status] || { status: 'info', label: '未知' }
}

/* ===== 表格列定义 ===== */

const columns = [
  { prop: 'periodName', label: '期间名称', minWidth: 120 },
  { prop: 'periodType', label: '期间类型', minWidth: 90, slot: 'periodType' },
  { prop: 'startDate', label: '开始日期', minWidth: 110 },
  { prop: 'endDate', label: '结束日期', minWidth: 110 },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'closedBy', label: '结账人', minWidth: 90 },
  { prop: 'closedTime', label: '结账时间', minWidth: 160 },
]

/* ===== 数据加载 ===== */

/** 加载会计期间列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: AccountingPeriodQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.periodType !== '') {
      params.periodType = searchForm.value.periodType
    }
    if (searchForm.value.status) {
      params.status = searchForm.value.status
    }
    const response = await accountingPeriodApi.getList(params)
    records.value = response?.records || []
    pagination.total = response?.total || 0
  } catch {
    ElMessage.error('加载会计期间列表失败')
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 重置搜索表单 */
function resetSearchForm(): void {
  searchForm.value = { periodType: '', status: '' }
}

/** 分页大小变化 */
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

/* ===== 新增期间对话框 ===== */

const dialogVisible = ref(false)
const submitting = ref(false)

/** 新增表单数据 */
const formData = reactive({
  periodType: 1,
  year: new Date().getFullYear(),
  month: undefined as number | undefined,
  quarter: undefined as number | undefined,
  startDate: '',
  endDate: '',
})

/** 打开新增对话框 */
function handleCreate(): void {
  formData.periodType = 1
  formData.year = new Date().getFullYear()
  formData.month = new Date().getMonth() + 1
  formData.quarter = undefined
  formData.startDate = ''
  formData.endDate = ''
  dialogVisible.value = true
}

/** 提交新增表单 */
async function handleSubmit(): Promise<void> {
  if (!formData.startDate || !formData.endDate) {
    ElMessage.warning('请填写开始日期和结束日期')
    return
  }
  if (formData.startDate > formData.endDate) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }

  submitting.value = true
  try {
    const submitData: Partial<AccountingPeriod> = {
      periodType: formData.periodType,
      year: formData.year,
      startDate: formData.startDate,
      endDate: formData.endDate,
    }
    if (formData.periodType === 1 && formData.month) {
      submitData.month = formData.month
      submitData.periodName = `${formData.year}-${String(formData.month).padStart(2, '0')}`
    } else if (formData.periodType === 2 && formData.quarter) {
      submitData.quarter = formData.quarter
      submitData.periodName = `${formData.year}-Q${formData.quarter}`
    } else if (formData.periodType === 3) {
      submitData.periodName = `${formData.year}`
    }
    await accountingPeriodApi.create(submitData)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

/* ===== 试算平衡对话框 ===== */

const trialBalanceVisible = ref(false)
const trialBalanceLoading = ref(false)
const trialBalanceResult = ref<TrialBalanceResult | null>(null)

/** 打开试算平衡对话框 */
async function handleTrialBalance(row: AccountingPeriod): Promise<void> {
  trialBalanceVisible.value = true
  trialBalanceLoading.value = true
  trialBalanceResult.value = null
  try {
    const result = await accountingPeriodApi.getTrialBalance(row.id)
    trialBalanceResult.value = result
  } catch {
    ElMessage.error('获取试算平衡数据失败')
  } finally {
    trialBalanceLoading.value = false
  }
}

/* ===== 结账检查清单对话框 ===== */

const checklistVisible = ref(false)
const checklistLoading = ref(false)
const checklistItems = ref<ClosingChecklistItem[]>([])

/** 打开结账检查清单对话框 */
async function handleChecklist(row: AccountingPeriod): Promise<void> {
  checklistVisible.value = true
  checklistLoading.value = true
  checklistItems.value = []
  try {
    const result = await accountingPeriodApi.getClosingChecklist(row.id)
    checklistItems.value = result || []
  } catch {
    ElMessage.error('获取结账检查清单失败')
  } finally {
    checklistLoading.value = false
  }
}

/* ===== 损益结转 ===== */

/** 损益结转 */
async function handleProfitTransfer(row: AccountingPeriod): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要对期间"${row.periodName}"进行损益结转吗？`,
      '损益结转确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const result: ProfitTransferResult = await accountingPeriodApi.profitTransfer(row.id, operatorId)
    if (result.success) {
      ElMessage.success(result.message || `损益结转成功，结转金额：¥${formatFen(result.transferAmount)}`)
    } else {
      ElMessage.error(result.message || '损益结转失败')
    }
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('损益结转失败，请重试')
    }
  }
}

/* ===== 结账/反结账 ===== */

/** 结账 */
async function handleClose(row: AccountingPeriod): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要结账期间"${row.periodName}"吗？结账后该期间将无法再录入凭证。`,
      '结账确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await accountingPeriodApi.close(row.id, operatorId)
    ElMessage.success('结账成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('结账失败，请重试')
    }
  }
}

/** 反结账 */
async function handleReopen(row: AccountingPeriod): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要反结账期间"${row.periodName}"吗？反结账后该期间可重新录入凭证。`,
      '反结账确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await accountingPeriodApi.reopen(row.id, operatorId)
    ElMessage.success('反结账成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('反结账失败，请重试')
    }
  }
}

/* ===== 生命周期 ===== */

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="会计期间管理" description="管理会计期间，支持结账、反结账、试算平衡、损益结转">
      <template #extra>
        <el-button type="primary" size="default" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>新增期间
        </el-button>
      </template>
    </PageHeader>

    <!-- 工具栏：期间类型筛选 + 状态筛选 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="searchForm.periodType"
            placeholder="期间类型"
            clearable
            style="width: 140px"
            size="default"
          >
            <el-option
              v-for="opt in periodTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="searchForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 期间列表表格 -->
    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="records"
        :loading="loading"
        :selectable="false"
        stripe
        :actions-width="260"
      >
        <template #periodType="{ row }">
          <StatusTag status="info" :label="getPeriodTypeLabel(row.periodType)" size="small" />
        </template>
        <template #status="{ row }">
          <StatusTag
            :status="getStatusTagConfig(row.status as AccountingPeriodStatus).status"
            :label="getStatusTagConfig(row.status as AccountingPeriodStatus).label"
            size="small"
          />
        </template>
        <template #actions="{ row }">
          <!-- open（开放）：试算平衡、结账检查、损益结转、结账 -->
          <template v-if="row.status === 'open'">
            <el-button link type="primary" size="small" @click="handleTrialBalance(row)">试算平衡</el-button>
            <el-button link type="primary" size="small" @click="handleChecklist(row)">结账检查</el-button>
            <el-button link type="primary" size="small" @click="handleProfitTransfer(row)">损益结转</el-button>
            <el-button link type="danger" size="small" @click="handleClose(row)">结账</el-button>
          </template>
          <!-- closing（结账中）：试算平衡、结账检查 -->
          <template v-else-if="row.status === 'closing'">
            <el-button link type="primary" size="small" @click="handleTrialBalance(row)">试算平衡</el-button>
            <el-button link type="primary" size="small" @click="handleChecklist(row)">结账检查</el-button>
          </template>
          <!-- closed（已结账）：反结账 -->
          <template v-else>
            <el-button link type="primary" size="small" @click="handleReopen(row)">反结账</el-button>
          </template>
        </template>
      </DataTable>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total,prev,pager,next,jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增会计期间对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增会计期间"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="期间类型" required>
          <el-select v-model="formData.periodType" :teleported="false" style="width: 100%">
            <el-option
              v-for="opt in periodTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年份" required>
          <el-input-number v-model="formData.year" :min="2020" :max="2099" />
        </el-form-item>
        <el-form-item v-if="formData.periodType === 1" label="月份">
          <el-input-number v-model="formData.month" :min="1" :max="12" />
        </el-form-item>
        <el-form-item v-if="formData.periodType === 2" label="季度">
          <el-input-number v-model="formData.quarter" :min="1" :max="4" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="formData.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            :teleported="false"
            placeholder="选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="formData.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            :teleported="false"
            placeholder="选择结束日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 试算平衡表对话框 -->
    <el-dialog v-model="trialBalanceVisible" title="试算平衡表" width="700px">
      <div v-loading="trialBalanceLoading">
        <template v-if="trialBalanceResult">
          <el-alert
            :type="trialBalanceResult.balanced ? 'success' : 'error'"
            :closable="false"
            :title="trialBalanceResult.balanced ? '借贷平衡' : '借贷不平衡'"
            :description="`借方合计: ¥${formatFen(trialBalanceResult.totalDebit)}，贷方合计: ¥${formatFen(trialBalanceResult.totalCredit)}，差额: ¥${formatFen(trialBalanceResult.difference)}`"
            show-icon
            style="margin-bottom: 16px;"
          />
          <el-table :data="trialBalanceResult.details" border>
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="科目名称" min-width="200" />
            <el-table-column prop="debitAmount" label="借方金额(元)" width="140">
              <template #default="{ row }">{{ formatFen(row.debitAmount) }}</template>
            </el-table-column>
            <el-table-column prop="creditAmount" label="贷方金额(元)" width="140">
              <template #default="{ row }">{{ formatFen(row.creditAmount) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
      <template #footer>
        <el-button @click="trialBalanceVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 结账检查清单对话框 -->
    <el-dialog v-model="checklistVisible" title="结账检查清单" width="600px">
      <div v-loading="checklistLoading">
        <div
          v-for="item in checklistItems"
          :key="item.itemName"
          class="checklist-item"
        >
          <el-icon :size="20" class="checklist-item__icon">
            <CircleCheck v-if="item.completed" class="checklist-item__icon--success" />
            <CircleClose v-else class="checklist-item__icon--error" />
          </el-icon>
          <div class="checklist-item__content">
            <div class="checklist-item__name">{{ item.itemName }}</div>
            <div v-if="item.description" class="checklist-item__desc">{{ item.description }}</div>
          </div>
        </div>
        <el-empty v-if="!checklistLoading && checklistItems.length === 0" description="暂无检查项" />
      </div>
      <template #footer>
        <el-button @click="checklistVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.checklist-item {
  display: flex;
  align-items: center;
  padding: var(--fts-space-3) 0;
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child {
    border-bottom: none;
  }

  &__icon {
    margin-right: var(--fts-space-3);
    flex-shrink: 0;

    /* 通过项图标：成功色 */
    &--success {
      color: var(--fts-success);
    }

    /* 未通过项图标：错误色 */
    &--error {
      color: var(--fts-error);
    }
  }

  &__content {
    flex: 1;
  }

  &__name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  &__desc {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    margin-top: var(--fts-space-1);
  }
}
</style>
