<script setup lang="ts">
/**
 * 银行/现金账户主数据管理
 *
 * 对齐主干真实模型（@/types/finance → FinanceBankAccount）：
 * - 账户类型：basic-基本户 / general-一般户 / payroll-工资户 / reserve-专用户 / other-其他
 * - 账户状态：active-启用 / frozen-冻结 / closed-销户
 * - 金额字段 balance 后端为「分」，converter 已统一转「元」
 *
 * 说明：原 WIP 版本假设存在 scope/organizationId/storeId/derivedBalance/reconciliationStatus
 * 等字段，但主干后端 bank_accounts 表与 DTO 均无这些列，UI 已按真实模型收口，不展示假数据。
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { bankAccountApi } from '@/api/finance'
import { useStandardPage } from '@/composables/useStandardPage'
import type {
  FinanceBankAccount,
  FinanceBankAccountFormData,
  BankAccountType,
  BankAccountStatus,
} from '@/types/finance'

defineOptions({ name: 'BankAccountManage' })

/* ===== 常量配置 ===== */
const accountTypeOptions: { value: BankAccountType; label: string }[] = [
  { value: 'basic', label: '基本户' },
  { value: 'general', label: '一般户' },
  { value: 'payroll', label: '工资户' },
  { value: 'reserve', label: '专用户' },
  { value: 'other', label: '其他' },
]

const statusOptions: { value: BankAccountStatus; label: string }[] = [
  { value: 'active', label: '启用' },
  { value: 'frozen', label: '冻结' },
  { value: 'closed', label: '销户' },
]

/* ===== 数据状态 ===== */
const loading = ref(false)
const records = ref<FinanceBankAccount[]>([])
const { pagination, resetPagination } = useStandardPage()

const searchForm = ref({
  accountName: '',
  accountNo: '',
  accountType: '' as '' | BankAccountType,
  status: '' as '' | BankAccountStatus,
})

/* ===== 对话框状态 ===== */
const dialogVisible = ref(false)
const dialogTitle = ref('新增账户')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()

function emptyForm(): FinanceBankAccountFormData {
  return {
    accountName: '',
    bankName: '',
    accountNo: '',
    accountType: 'basic',
    openDate: '',
    remark: '',
  }
}
const formData = ref<FinanceBankAccountFormData>(emptyForm())

/* ===== 表单校验规则 ===== */
const formRules = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
  accountNo: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  bankName: [
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        // 基本户/一般户必须填写开户银行；其它类型可空
        const needBank = formData.value.accountType === 'basic' || formData.value.accountType === 'general'
        if (needBank && !value) {
          callback(new Error('基本户/一般户必须填写开户银行'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

/* ===== 表格列定义 ===== */
const columns: DataTableColumn[] = [
  { prop: 'accountName', label: '账户名称', minWidth: 140 },
  { prop: 'accountType', label: '账户类型', width: 100, slot: 'accountType' },
  { prop: 'bankName', label: '开户银行', minWidth: 140 },
  { prop: 'accountNo', label: '账号', minWidth: 150, slot: 'accountNo' },
  { prop: 'balance', label: '余额(元)', width: 130, align: 'right', slot: 'balance' },
  { prop: 'openDate', label: '开户日期', width: 120 },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
]

/* ===== 方法 ===== */

async function loadList() {
  loading.value = true
  try {
    const res = await bankAccountApi.getList({
      page: pagination.current,
      size: pagination.size,
      accountName: searchForm.value.accountName || undefined,
      accountNo: searchForm.value.accountNo || undefined,
      accountType: searchForm.value.accountType || undefined,
      status: searchForm.value.status || undefined,
    })
    records.value = res.records
    pagination.total = res.total
  } catch (e: unknown) {
    // 拦截器已统一弹错，这里只留日志，避免重复提示
    console.error('[BankAccountManage] loadList error:', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  resetPagination()
  loadList()
}

function handleReset() {
  searchForm.value = { accountName: '', accountNo: '', accountType: '', status: '' }
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.size = size
  loadList()
}

function handleCurrentChange(current: number) {
  pagination.current = current
  loadList()
}

function openCreateDialog() {
  editingId.value = null
  dialogTitle.value = '新增账户'
  formData.value = emptyForm()
  dialogVisible.value = true
}

function openEditDialog(row: FinanceBankAccount) {
  editingId.value = row.id
  dialogTitle.value = '编辑账户'
  formData.value = {
    id: row.id,
    accountName: row.accountName,
    bankName: row.bankName || '',
    accountNo: row.accountNo || '',
    accountType: row.accountType,
    openDate: row.openDate || '',
    remark: row.remark || '',
    status: row.status,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await bankAccountApi.update(editingId.value, formData.value)
      ElMessage.success('账户更新成功')
    } else {
      await bankAccountApi.create(formData.value)
      ElMessage.success('账户创建成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    submitting.value = false
  }
}

/** 启停切换：后端无独立状态接口，走 update（updateDTO 支持 status） */
async function toggleStatus(row: FinanceBankAccount, next: BankAccountStatus, confirmText?: string) {
  try {
    if (confirmText) {
      await ElMessageBox.confirm(confirmText, '状态变更', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      })
    }
    await bankAccountApi.update(row.id, {
      id: row.id,
      accountName: row.accountName,
      bankName: row.bankName || '',
      accountNo: row.accountNo || '',
      accountType: row.accountType,
      openDate: row.openDate || '',
      remark: row.remark || '',
      status: next,
    })
    ElMessage.success(next === 'active' ? '账户已启用' : '账户已停用')
    loadList()
  } catch (e: unknown) {
    if (e !== 'cancel' && e instanceof Error) {
      ElMessage.error(e.message)
    }
  }
}

function handleDisable(row: FinanceBankAccount) {
  const text = '确认停用账户「' + row.accountName + '」？停用后将不在下拉选择中显示。'
  return toggleStatus(row, 'closed', text)
}

function handleEnable(row: FinanceBankAccount) {
  return toggleStatus(row, 'active')
}

async function handleDelete(row: FinanceBankAccount) {
  const text = '确认删除账户「' + row.accountName + '」？删除后不可恢复。'
  try {
    await ElMessageBox.confirm(text, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await bankAccountApi.delete(row.id)
    ElMessage.success('账户已删除')
    loadList()
  } catch (e: unknown) {
    if (e !== 'cancel' && e instanceof Error) {
      ElMessage.error(e.message)
    }
  }
}

function formatAmount(amount: number | undefined): string {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getAccountTypeLabel(type: BankAccountType): string {
  return accountTypeOptions.find((o) => o.value === type)?.label ?? type
}

function getStatusLabel(status: BankAccountStatus): string {
  return statusOptions.find((o) => o.value === status)?.label ?? status
}

function getStatusTagType(status: BankAccountStatus): 'success' | 'danger' | 'info' {
  if (status === 'active') return 'success'
  if (status === 'closed') return 'danger'
  return 'info'
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="银行/现金账户管理" description="管理银行账户与现金账户，支持类型、状态与余额维护">
      <template #extra>
        <el-button type="primary" size="default" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 新增账户
        </el-button>
      </template>
    </PageHeader>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="searchForm.accountName"
            placeholder="账户名称"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="searchForm.accountNo"
            placeholder="账号"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="searchForm.accountType" placeholder="账户类型" clearable style="width: 130px" size="default">
            <el-option v-for="opt in accountTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px" size="default">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="table-section">
      <EmptyState
        v-if="records.length === 0 && !loading"
        type="no-data"
        title="暂无账户数据"
        description="可通过「新增账户」创建第一个银行或现金账户"
        action-text="新增账户"
        @action="openCreateDialog"
      />
      <DataTable
        v-else
        :columns="columns"
        :data="records"
        :loading="loading"
        :actions-width="170"
        stripe
        density="auto"
      >
        <template #accountType="{ row }">
          <StatusTag status="info" :label="getAccountTypeLabel(row.accountType)" size="small" />
        </template>
        <template #accountNo="{ row }">
          <span>{{ row.accountNo || '-' }}</span>
        </template>
        <template #balance="{ row }">
          <span>{{ formatAmount(row.balance) }}</span>
        </template>
        <template #status="{ row }">
          <StatusTag :status="getStatusTagType(row.status)" :label="getStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'active'"
            link
            type="warning"
            size="small"
            @click="handleDisable(row)"
          >停用</el-button>
          <el-button v-else link type="success" size="small" @click="handleEnable(row)">启用</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="账户名称" prop="accountName">
          <el-input v-model="formData.accountName" placeholder="请输入账户名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="账户类型" prop="accountType">
          <el-select v-model="formData.accountType" style="width: 100%">
            <el-option v-for="opt in accountTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开户银行" prop="bankName">
          <el-input v-model="formData.bankName" placeholder="基本户/一般户必填" maxlength="100" />
        </el-form-item>
        <el-form-item label="账号" prop="accountNo">
          <el-input v-model="formData.accountNo" placeholder="请输入银行账号" maxlength="50" />
        </el-form-item>
        <el-form-item label="开户日期">
          <el-date-picker
            v-model="formData.openDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注（选填）" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
/* 页面布局与财务模块其他页一致 */
</style>
