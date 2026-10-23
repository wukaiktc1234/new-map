<script setup lang="ts">
/**
 * 银行/现金账户主数据管理（P1-A：账户主数据受控启动）
 *
 * 功能：
 * - 账户 CRUD（创建/编辑/删除/查看）
 * - 账户类型：bank-银行 / cash-现金
 * - 数据范围：GLOBAL-全局 / ORGANIZATION-组织 / STORE-门店
 * - 启停控制：status=1(启用)/0(停用)
 * - 数据范围过滤：根据用户权限自动过滤
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
  BankAccountScope,
} from '@/types/finance'

defineOptions({ name: 'BankAccountManage' })

/* ===== 常量配置 ===== */
const accountTypeOptions = [
  { value: 'bank' as BankAccountType, label: '银行账户' },
  { value: 'cash' as BankAccountType, label: '现金账户' },
]

const scopeOptions = [
  { value: 'GLOBAL' as BankAccountScope, label: '全局' },
  { value: 'ORGANIZATION' as BankAccountScope, label: '组织' },
  { value: 'STORE' as BankAccountScope, label: '门店' },
]

/* ===== 数据状态 ===== */
const loading = ref(false)
const records = ref<FinanceBankAccount[]>([])
const { pagination, resetPagination } = useStandardPage()
const searchForm = ref({
  keyword: '',
  accountType: '' as '' | BankAccountType,
  scope: '' as '' | BankAccountScope,
})

/* ===== 对话框状态 ===== */
const dialogVisible = ref(false)
const dialogTitle = ref('新增账户')
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref()

const formData = ref<FinanceBankAccountFormData>({
  accountName: '',
  bankName: '',
  accountNumber: '',
  accountType: 'bank',
  scope: 'GLOBAL',
  organizationId: '',
  storeId: '',
  openDate: '',
  remark: '',
})

/* ===== 表单校验规则 ===== */
const formRules = {
  accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
  scope: [{ required: true, message: '请选择数据范围', trigger: 'change' }],
  bankName: [{
    validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
      if (formData.value.accountType === 'bank' && !value) {
        callback(new Error('银行账户必须填写开户银行'))
      } else {
        callback()
      }
    },
    trigger: 'blur',
  }],
}

/* ===== 表格列定义 ===== */
const columns: DataTableColumn[] = [
  { prop: 'accountName', label: '账户名称', minWidth: 140 },
  { prop: 'accountType', label: '账户类型', width: 100, slot: 'accountType' },
  { prop: 'bankName', label: '开户银行', minWidth: 140 },
  { prop: 'accountNumberMasked', label: '账号', width: 120 },
  { prop: 'scope', label: '数据范围', width: 100, slot: 'scope' },
  { prop: 'balance', label: '记账余额(元)', width: 120, align: 'right', slot: 'balance' },
  { prop: 'derivedBalance', label: '派生余额(元)', width: 120, align: 'right', slot: 'derivedBalance' },
  { prop: 'reconciliationStatus', label: '对账状态', width: 100, slot: 'reconciliationStatus' },
  { prop: 'status', label: '状态', width: 80, slot: 'status' },
]

/* ===== 方法 ===== */

/** 加载账户列表 */
async function loadList() {
  loading.value = true
  try {
    const res = await bankAccountApi.getList({
      page: pagination.current,
      size: pagination.size,
      keyword: searchForm.value.keyword || undefined,
      accountType: searchForm.value.accountType || undefined,
      scope: searchForm.value.scope || undefined,
    })
    records.value = res.records
    pagination.total = res.total
  } catch (e: unknown) {
    // 拦截器已通过 ElMessage 弹出具体错误（403→"没有权限"、500→"服务器内部错误"等）
    // 此处仅记录日志，不再覆盖为通用提示
    console.error('[BankAccountManage] loadList error:', e)
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  resetPagination()
  loadList()
}

/** 重置 */
function handleReset() {
  searchForm.value = { keyword: '', accountType: '', scope: '' }
  handleSearch()
}

/** 分页切换 */
function handleSizeChange(size: number) {
  pagination.size = size
  loadList()
}

function handleCurrentChange(current: number) {
  pagination.current = current
  loadList()
}

/** 打开新增对话框 */
function openCreateDialog() {
  editingId.value = null
  dialogTitle.value = '新增账户'
  formData.value = {
    accountName: '',
    bankName: '',
    accountNumber: '',
    accountType: 'bank',
    scope: 'GLOBAL',
    organizationId: '',
    storeId: '',
    openDate: '',
    remark: '',
  }
  dialogVisible.value = true
}

/** 打开编辑对话框 */
function openEditDialog(row: FinanceBankAccount) {
  editingId.value = row.id
  dialogTitle.value = '编辑账户'
  formData.value = {
    id: row.id,
    accountName: row.accountName,
    bankName: row.bankName || '',
    accountNumber: '', // 编辑时不回填账号（脱敏）
    accountType: row.accountType,
    scope: row.scope,
    organizationId: row.organizationId || '',
    storeId: row.storeId || '',
    openDate: row.openDate || '',
    remark: row.remark || '',
  }
  dialogVisible.value = true
}

/** 提交表单 */
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
    const msg = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

/** 停用账户 */
async function handleDisable(row: FinanceBankAccount) {
  try {
    await ElMessageBox.confirm(`确认停用账户「${row.accountName}」？停用后将不在下拉选择中显示。`, '停用确认', {
      confirmButtonText: '确认停用',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await bankAccountApi.update(row.id, { ...row, status: 'inactive', scope: row.scope } as never)
    ElMessage.success('账户已停用')
    loadList()
  } catch (e: unknown) {
    if (e !== 'cancel' && e instanceof Error) {
      ElMessage.error(e.message)
    }
  }
}

/** 启用账户 */
async function handleEnable(row: FinanceBankAccount) {
  try {
    await bankAccountApi.update(row.id, { ...row, status: 'active', scope: row.scope } as never)
    ElMessage.success('账户已启用')
    loadList()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '启用失败'
    ElMessage.error(msg)
  }
}

/** 删除账户（软删除） */
async function handleDelete(row: FinanceBankAccount) {
  try {
    await ElMessageBox.confirm(`确认删除账户「${row.accountName}」？删除后不可恢复。`, '删除确认', {
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

/** 格式化金额显示 */
function formatAmount(amount: number | undefined): string {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 账户类型 → 显示文本 */
function getAccountTypeLabel(type: BankAccountType): string {
  return type === 'bank' ? '银行账户' : '现金账户'
}

/** 范围 → 显示文本 */
function getScopeLabel(scope: BankAccountScope): string {
  return scopeOptions.find(o => o.value === scope)?.label ?? scope
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="银行/现金账户管理" description="管理银行账户与现金账户，支持组织/门店归属与启停控制">
      <template #extra>
        <el-button type="primary" size="default" @click="openCreateDialog">
          <el-icon><Plus /></el-icon> 新增账户
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选区 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索账户名称/银行名称"
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="searchForm.accountType" placeholder="账户类型" clearable style="width: 130px" size="default">
            <el-option v-for="opt in accountTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.scope" placeholder="数据范围" clearable style="width: 130px" size="default">
            <el-option v-for="opt in scopeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 表格区 -->
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
        :actions-width="160"
        stripe
        density="auto"
      >
        <template #accountType="{ row }">
          <StatusTag :status="row.accountType === 'bank' ? 'info' : 'warning'" :label="getAccountTypeLabel(row.accountType)" size="small" />
        </template>
        <template #scope="{ row }">
          <StatusTag status="info" :label="getScopeLabel(row.scope)" size="small" />
        </template>
        <template #balance="{ row }">
          <span>{{ formatAmount(row.balance) }}</span>
        </template>
        <template #derivedBalance="{ row }">
          <span>{{ formatAmount(row.derivedBalance) }}</span>
        </template>
        <template #reconciliationStatus="{ row }">
          <StatusTag
            :status="row.reconciliationStatus === 'MATCH' ? 'success' : row.reconciliationStatus === 'UNMATCH' ? 'danger' : 'info'"
            :label="row.reconciliationStatus === 'MATCH' ? '一致' : row.reconciliationStatus === 'UNMATCH' ? '不一致' : '未核对'"
            size="small"
          />
        </template>
        <template #status="{ row }">
          <StatusTag
            :status="row.status === 'active' ? 'success' : 'danger'"
            :label="row.status === 'active' ? '启用' : '停用'"
            size="small"
          />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'active'"
            link type="warning" size="small"
            @click="handleDisable(row)"
          >停用</el-button>
          <el-button
            v-else
            link type="success" size="small"
            @click="handleEnable(row)"
          >启用</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </DataTable>
    </div>

    <!-- 分页 -->
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

    <!-- 新增/编辑对话框 -->
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
          <el-input v-model="formData.bankName" placeholder="银行账户必填" maxlength="100" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="账号" prop="accountNumber">
          <el-input v-model="formData.accountNumber" placeholder="银行账户请输入账号" maxlength="50" />
        </el-form-item>
        <el-form-item label="数据范围" prop="scope">
          <el-select v-model="formData.scope" style="width: 100%">
            <el-option v-for="opt in scopeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.scope === 'ORGANIZATION'" label="组织ID">
          <el-input v-model="formData.organizationId" placeholder="请输入组织ID" />
        </el-form-item>
        <el-form-item v-if="formData.scope === 'STORE'" label="门店ID">
          <el-input v-model="formData.storeId" placeholder="请输入门店ID" />
        </el-form-item>
        <el-form-item label="开户日期">
          <el-date-picker v-model="formData.openDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
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
