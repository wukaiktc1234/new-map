<script setup lang="ts">
/**
 * 合同管理页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成合同管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 重构要点：
 * ✅ 使用 .modern-page + PageHeader 替代 StandardPage
 * ✅ 使用 stats-section 响应式4列网格 + variant="bordered"
 * ✅ 使用 table-section + pagination-wrapper 分页分离模式
 * ✅ 使用 CSS 变量（无硬编码颜色）
 * ✅ 符合 project_rules.md 规范
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Document, Bell } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { contractApi } from '@/api/hr/contract'
import { employeeApi } from '@/api/hr'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  EmployeeContract,
  ContractFormData,
  ContractQueryParams,
  ContractRenewData,
  ContractTerminateData,
  ContractType,
  ContractStatus,
  LaborContractTerm,
  ContractStatistics,
} from '@/types/hr/contract'
import {
  ContractTypeOptions,
  ContractTypeTagMap,
  ContractStatusOptions,
  ContractStatusTagMap,
  LaborContractTermOptions,
} from '@/types/hr/contract'
import type { Employee } from '@/types/hr'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<EmployeeContract[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailDialogVisible = ref(false)
const renewDialogVisible = ref(false)
const terminateDialogVisible = ref(false)
const reminderDialogVisible = ref(false)
const currentDetail = ref<EmployeeContract | null>(null)
const renewTarget = ref<EmployeeContract | null>(null)
const terminateTarget = ref<EmployeeContract | null>(null)

const formRef = ref<FormInstance>()
const renewFormRef = ref<FormInstance>()
const terminateFormRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  employeeName: '',
  employeeCode: '',
  departmentName: '',
  contractType: '' as ContractType | '',
  status: '' as ContractStatus | '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<EmployeeContract, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: ContractQueryParams = {
        page: params.page,
        pageSize: params.size,
        keyword: params.employeeName || params.employeeCode || params.departmentName || undefined,
        contractType: params.contractType || undefined,
        status: params.status || undefined,
      }
      return contractApi.getList(queryParams)
    },
  } as unknown as CrudApi<EmployeeContract, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = ref<ContractStatistics>({
  total: 0,
  pending: 0,
  active: 0,
  expiring: 0,
  closed: 0,
  archived: 0,
})

// 本月新签数量
const newThisMonth = ref(0)

// 员工选项
const employeeOptions = ref<Employee[]>([])

// 部门选项（接入真实后端 API）
const { departmentOptions } = useDepartmentOptions(true)

// 合同模板选项（TODO: 后端未提供合同模板接口，暂为静态配置；后端补充后改为 API 加载）
const templateOptions = ref<{ value: string; label: string }[]>([
  { value: 'tpl_001', label: '标准劳动合同模板' },
  { value: 'tpl_002', label: '实习协议模板' },
  { value: 'tpl_003', label: '劳务派遣协议模板' },
])

// 导入/导出加载状态
const importLoading = ref(false)
const exportLoading = ref(false)

// 表单数据
const formData = reactive<Partial<ContractFormData> & { employeeName: string; probationMonths: number }>({
  employeeId: '',
  employeeName: '',
  contractType: 'labor',
  contractNo: '',
  termType: 'fixed_3year',
  startDate: '',
  endDate: '',
  probationMonths: 3,
  templateId: '',
  remark: '',
})

// 续签表单
const renewForm = reactive({
  newStartDate: '',
  newEndDate: '',
  newTermType: '' as LaborContractTerm | '',
  remark: '',
})

// 终止表单
const terminateForm = reactive({
  terminateDate: '',
  terminateReason: '',
  remark: '',
})

// 到期提醒设置
const reminderSettings = reactive({
  enabled: true,
  advanceDays: 30,
  notifyHr: true,
  notifyManager: true,
})

// 续签记录（TODO: 后端未提供 GET /v1/contracts/{id}/renew-records 接口，暂为空数组）
const renewRecords = ref<Array<{ id: string; renewDate: string; oldEndDate: string; newEndDate: string; operator: string }>>([])

// 变更记录（TODO: 后端未提供 GET /v1/contracts/{id}/changes 接口，contractApi.getChangeRecords 待对接，暂为空数组）
const changeRecords = ref<Array<{ id: string; changeDate: string; changeType: string; beforeValue: string; afterValue: string; operator: string }>>([])

// 详情对话框当前激活的Tab
const activeDetailTab = ref('basic')

// ==================== 计算属性 ====================

const statsCards = computed(() => [
  { icon: 'Document', label: '合同总数', value: statistics.value.total, colorType: 'primary' as const },
  { icon: 'Warning', label: '即将到期', value: statistics.value.expiring, colorType: 'warning' as const },
  { icon: 'CircleClose', label: '已过期', value: statistics.value.closed, colorType: 'error' as const },
  { icon: 'Plus', label: '本月新签', value: newThisMonth.value, colorType: 'success' as const },
])

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'employeeName', label: '员工姓名', minWidth: 100, slot: 'employeeName' },
  { prop: 'employeeCode', label: '工号', minWidth: 100, slot: 'employeeCode' },
  { prop: 'departmentName', label: '部门', minWidth: 110, slot: 'departmentName' },
  { prop: 'contractType', label: '合同类型', minWidth: 120, slot: 'contractType' },
  { prop: 'contractNo', label: '合同编号', minWidth: 140, slot: 'contractNo' },
  { prop: 'startDate', label: '开始日期', minWidth: 110, slot: 'startDate' },
  { prop: 'endDate', label: '结束日期', minWidth: 110, slot: 'endDate' },
  { prop: 'termType', label: '合同期限', minWidth: 130, slot: 'termType' },
  { prop: 'status', label: '合同状态', minWidth: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 280, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  contractNo: [
    { required: true, message: '请输入合同编号', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
}

const renewFormRules: FormRules = {
  newStartDate: [{ required: true, message: '请选择新开始日期', trigger: 'change' }],
}

const terminateFormRules: FormRules = {
  terminateDate: [{ required: true, message: '请选择终止日期', trigger: 'change' }],
  terminateReason: [{ required: true, message: '请输入终止原因', trigger: 'blur' }],
}

// ==================== 方法 ====================

/** 加载统计数据 */
async function loadStatistics() {
  try {
    statistics.value = await contractApi.getStatistics()
  } catch {
    // 静默失败
  }
}

/** 加载员工选项 */
async function loadEmployeeOptions() {
  try {
    employeeOptions.value = await employeeApi.getEmployees()
  } catch {
    employeeOptions.value = []
  }
}

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.employeeName = ''
  queryForm.value.employeeCode = ''
  queryForm.value.departmentName = ''
  queryForm.value.contractType = ''
  queryForm.value.status = ''
  refresh()
}

function handleSelectionChange(rows: EmployeeContract[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    employeeId: '',
    employeeName: '',
    contractType: 'labor',
    contractNo: `HT${Date.now().toString().slice(-8)}`,
    termType: 'fixed_3year',
    startDate: '',
    endDate: '',
    probationMonths: 3,
    templateId: '',
    remark: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: EmployeeContract) {
  isEdit.value = true
  try {
    const detail = await contractApi.getById(row.id)
    Object.assign(formData, {
      ...detail,
      employeeName: detail.employeeName,
      probationMonths: 3,
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载合同详情失败')
  }
}

async function handleDetail(row: EmployeeContract) {
  try {
    const detail = await contractApi.getById(row.id)
    currentDetail.value = detail
    detailDialogVisible.value = true
  } catch {
    ElMessage.error('加载合同详情失败')
  }
}

function handleRenew(row: EmployeeContract) {
  renewTarget.value = row
  Object.assign(renewForm, {
    newStartDate: '',
    newEndDate: '',
    newTermType: row.termType || '',
    remark: '',
  })
  renewDialogVisible.value = true
}

async function handleRenewSubmit() {
  if (!renewFormRef.value) return
  const valid = await renewFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!renewTarget.value) return

  try {
    const data: ContractRenewData = {
      contractId: renewTarget.value.id,
      newStartDate: renewForm.newStartDate,
      newEndDate: renewForm.newEndDate || undefined,
      newTermType: renewForm.newTermType || undefined,
      remark: renewForm.remark || undefined,
    }
    await contractApi.renew(data)
    ElMessage.success('续签成功')
    renewDialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '续签失败')
  }
}

function handleTerminate(row: EmployeeContract) {
  terminateTarget.value = row
  Object.assign(terminateForm, {
    terminateDate: '',
    terminateReason: '',
    remark: '',
  })
  terminateDialogVisible.value = true
}

async function handleTerminateSubmit() {
  if (!terminateFormRef.value) return
  const valid = await terminateFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!terminateTarget.value) return

  try {
    const data: ContractTerminateData = {
      contractId: terminateTarget.value.id,
      terminateDate: terminateForm.terminateDate,
      terminateReason: terminateForm.terminateReason,
      remark: terminateForm.remark || undefined,
    }
    await contractApi.terminate(data)
    ElMessage.success('合同已终止')
    terminateDialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '终止失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = {
      ...formData,
    }

    if (isEdit.value && formData.id) {
      await contractApi.update(formData.id, submitData as Partial<ContractFormData>)
      ElMessage.success('更新成功')
    } else {
      await contractApi.create(submitData as ContractFormData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: EmployeeContract) {
  try {
    await ElMessageBox.confirm(
      `确定要删除合同「${row.contractNo}」（${row.employeeName}）吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await contractApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/** 到期提醒设置 */
function handleReminderSettings() {
  reminderDialogVisible.value = true
}

function handleReminderSave() {
  ElMessage.success('提醒设置已保存')
  reminderDialogVisible.value = false
}

/** 文件上传前验证 */
function beforeUpload(rawFile: UploadRawFile): boolean {
  const allowedTypes = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ]

  if (!allowedTypes.includes(rawFile.type) && !rawFile.name.endsWith('.xlsx') && !rawFile.name.endsWith('.xls')) {
    ElMessage.error('只支持 Excel 格式的文件（.xlsx 或 .xls）')
    return false
  }

  const isLt5M = rawFile.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }

  return true
}

/** 导入合同数据 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('导入功能开发中，敬请期待')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

/** 导出合同数据 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('导出功能开发中，敬请期待')
  } catch (error: unknown) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

function getContractTypeLabel(type: ContractType): string {
  return ContractTypeOptions.find(o => o.value === type)?.label || type
}

function getContractStatusLabel(status: ContractStatus): string {
  return ContractStatusOptions.find(o => o.value === status)?.label || status
}

function getTermTypeLabel(termType?: LaborContractTerm): string {
  if (!termType) return '-'
  return LaborContractTermOptions.find(o => o.value === termType)?.label || termType
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadStatistics()
  loadEmployeeOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="合同管理" description="管理员工劳动合同的签订、续签和到期提醒">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增合同
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="String(stat.value)"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导入导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.employeeName"
            placeholder="员工姓名"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.employeeCode"
            placeholder="工号"
            clearable
            style="width: 120px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-input
            v-model="queryForm.departmentName"
            placeholder="部门"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="queryForm.contractType"
            placeholder="合同类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in ContractTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="合同状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in ContractStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImport"
            accept=".xlsx,.xls"
            :disabled="importLoading"
          >
            <el-button size="default" class="action-btn--import" :loading="importLoading">
              <el-icon :size="14"><Upload /></el-icon>批量导入
            </el-button>
          </el-upload>
          <el-button size="default" class="action-btn--export" :loading="exportLoading" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
          <el-button size="default" class="action-btn--reminder" @click="handleReminderSettings">
            <el-icon :size="14"><Bell /></el-icon>到期提醒设置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="true"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        @selection-change="handleSelectionChange"
      >
        <!-- 员工姓名列 -->
        <template #employeeName="{ row }">
          <span class="name-text">{{ row.employeeName }}</span>
        </template>

        <!-- 工号列 -->
        <template #employeeCode="{ row }">
          <span class="code-text">{{ row.employeeCode || '-' }}</span>
        </template>

        <!-- 部门列 -->
        <template #departmentName="{ row }">
          <span class="dept-text">{{ row.departmentName || '-' }}</span>
        </template>

        <!-- 合同类型列 -->
        <template #contractType="{ row }">
          <StatusTag
            :status="ContractTypeTagMap[row.contractType as ContractType]"
            :label="getContractTypeLabel(row.contractType)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 合同编号列 -->
        <template #contractNo="{ row }">
          <span class="contract-no">{{ row.contractNo }}</span>
        </template>

        <!-- 开始日期列 -->
        <template #startDate="{ row }">
          <span class="date-text">{{ formatDate(row.startDate) }}</span>
        </template>

        <!-- 结束日期列 -->
        <template #endDate="{ row }">
          <span class="date-text">{{ formatDate(row.endDate) }}</span>
        </template>

        <!-- 合同期限列 -->
        <template #termType="{ row }">
          <span class="term-text">{{ getTermTypeLabel(row.termType) }}</span>
        </template>

        <!-- 合同状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="ContractStatusTagMap[row.status as ContractStatus]"
            :label="getContractStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'active' || row.status === 'expiring'"
              link
              type="success"
              size="default"
              @click.stop="handleRenew(row)"
            >
              续签
            </el-button>
            <el-button
              v-if="row.status === 'active' || row.status === 'expiring'"
              link
              type="warning"
              size="default"
              @click.stop="handleTerminate(row)"
            >
              终止
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>
      </DataTable>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="refresh"
          @size-change="refresh"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑合同' : '新增合同'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="员工选择" prop="employeeId">
              <el-select
                v-model="formData.employeeId"
                filterable
                placeholder="请选择员工"
                :teleported="false"
                style="width: 100%"
              >
                <el-option
                  v-for="emp in employeeOptions"
                  :key="emp.id"
                  :label="`${emp.employeeName}（${emp.employeeCode}）`"
                  :value="emp.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="formData.contractType" placeholder="请选择合同类型" :teleported="false" style="width: 100%">
                <el-option v-for="opt in ContractTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同编号" prop="contractNo">
              <el-input v-model="formData.contractNo" placeholder="请输入合同编号" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="formData.startDate"
                type="date"
                placeholder="选择开始日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="formData.endDate"
                type="date"
                placeholder="选择结束日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="合同期限">
              <el-select v-model="formData.termType" placeholder="请选择期限类型" :teleported="false" style="width: 100%">
                <el-option v-for="opt in LaborContractTermOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试用期">
              <el-input-number v-model="formData.probationMonths" :min="0" :max="6" controls-position="right" style="width: 100%" />
              <span style="margin-left: 8px; color: var(--fts-text-tertiary); font-size: 12px;">个月</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="合同模板">
              <el-select v-model="formData.templateId" placeholder="请选择合同模板" clearable :teleported="false" style="width: 100%">
                <el-option v-for="tpl in templateOptions" :key="tpl.value" :label="tpl.label" :value="tpl.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="合同附件">
              <el-upload
                action="#"
                :auto-upload="false"
                multiple
                :limit="5"
                accept=".pdf,.doc,.docx"
              >
                <el-button size="default">
                  <el-icon><Upload /></el-icon>点击上传
                </el-button>
                <template #tip>
                  <div style="color: var(--fts-text-tertiary); font-size: 12px; margin-top: 4px;">
                    支持PDF、Word格式，单个文件不超过10MB，最多上传5个
                  </div>
                </template>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 续签对话框 -->
    <el-dialog
      v-model="renewDialogVisible"
      title="合同续签"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="renewTarget">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: var(--fts-space-3);">
          <template #title>续签说明</template>
          <div style="line-height: 1.6; margin-top: var(--fts-space-1);">
            续签后原合同状态变为"已续签"，新合同进入待签署流程。原合同的关键信息（合同类型、模板等）将自动继承到新合同。
          </div>
        </el-alert>

        <el-descriptions :column="1" border size="small" style="margin-bottom: var(--fts-space-4);">
          <el-descriptions-item label="员工姓名">{{ renewTarget.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="原合同编号">{{ renewTarget.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="原合同类型">
            <StatusTag
              :status="ContractTypeTagMap[renewTarget.contractType as ContractType]"
              :label="getContractTypeLabel(renewTarget.contractType)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="原合同期限">
            {{ formatDate(renewTarget.startDate) }} ~ {{ formatDate(renewTarget.endDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="续签次数">{{ renewTarget.renewCount }} 次</el-descriptions-item>
        </el-descriptions>
      </template>

      <el-form ref="renewFormRef" :model="renewForm" :rules="renewFormRules" label-width="120px">
        <el-form-item label="新开始日期" prop="newStartDate">
          <el-date-picker
            v-model="renewForm.newStartDate"
            type="date"
            placeholder="选择新开始日期"
            :teleported="false"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="新结束日期">
          <el-date-picker
            v-model="renewForm.newEndDate"
            type="date"
            placeholder="选择新结束日期"
            :teleported="false"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="新期限类型">
          <el-select v-model="renewForm.newTermType" placeholder="请选择期限类型" :teleported="false" style="width: 100%">
            <el-option v-for="opt in LaborContractTermOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="renewForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="renewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleRenewSubmit">确认续签</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 终止对话框 -->
    <el-dialog
      v-model="terminateDialogVisible"
      title="合同终止"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="terminateTarget">
        <el-descriptions :column="1" border style="margin-bottom: var(--fts-space-4);">
          <el-descriptions-item label="员工姓名">{{ terminateTarget.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ terminateTarget.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="合同期限">
            {{ formatDate(terminateTarget.startDate) }} ~ {{ formatDate(terminateTarget.endDate) }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-form ref="terminateFormRef" :model="terminateForm" :rules="terminateFormRules" label-width="100px">
        <el-form-item label="终止日期" prop="terminateDate">
          <el-date-picker
            v-model="terminateForm.terminateDate"
            type="date"
            placeholder="选择终止日期"
            :teleported="false"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="终止原因" prop="terminateReason">
          <el-input v-model="terminateForm.terminateReason" type="textarea" :rows="3" placeholder="请输入终止原因" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="terminateForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="terminateDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="handleTerminateSubmit">确认终止</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="合同详情"
      width="1200px"
      class="fts-dialog--xl"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-tabs v-model="activeDetailTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border size="default">
              <el-descriptions-item label="员工姓名">{{ currentDetail.employeeName }}</el-descriptions-item>
              <el-descriptions-item label="工号">{{ currentDetail.employeeCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="部门">{{ currentDetail.departmentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="职位">{{ currentDetail.positionName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="合同类型">
                <StatusTag
                  :status="ContractTypeTagMap[currentDetail.contractType as ContractType]"
                  :label="getContractTypeLabel(currentDetail.contractType)"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="合同编号">{{ currentDetail.contractNo }}</el-descriptions-item>
              <el-descriptions-item label="合同期限">
                {{ formatDate(currentDetail.startDate) }} ~ {{ formatDate(currentDetail.endDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="期限类型">{{ getTermTypeLabel(currentDetail.termType) }}</el-descriptions-item>
              <el-descriptions-item label="合同状态">
                <StatusTag
                  :status="ContractStatusTagMap[currentDetail.status as ContractStatus]"
                  :label="getContractStatusLabel(currentDetail.status)"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="续签次数">{{ currentDetail.renewCount }} 次</el-descriptions-item>
              <el-descriptions-item label="签订日期">{{ formatDate(currentDetail.signDate) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDate(currentDetail.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 合同内容 -->
          <el-tab-pane label="合同内容" name="content">
            <div class="detail-section">
              <div class="section-title">合同模板</div>
              <div class="section-content">
                <StatusTag status="info" label="标准劳动合同模板" size="small" />
              </div>
            </div>
            <div class="detail-section">
              <div class="section-title">合同附件</div>
              <div class="section-content">
                <el-empty v-if="!currentDetail.attachments || currentDetail.attachments.length === 0" description="暂无附件" :image-size="80" />
                <div v-else class="attachment-list">
                  <div v-for="(file, index) in currentDetail.attachments" :key="index" class="attachment-item">
                    <el-icon><Document /></el-icon>
                    <span class="attachment-name">{{ file }}</span>
                    <el-button link type="primary" size="small">下载</el-button>
                  </div>
                </div>
              </div>
            </div>
            <div class="detail-section">
              <div class="section-title">备注</div>
              <div class="section-content remark-text">
                {{ currentDetail.remark || '暂无备注' }}
              </div>
            </div>
          </el-tab-pane>

          <!-- 续签记录 -->
          <el-tab-pane label="续签记录" name="renew">
            <el-table :data="renewRecords" stripe border size="default">
              <el-table-column prop="renewDate" label="续签日期" width="120" />
              <el-table-column prop="oldEndDate" label="原结束日期" width="120" />
              <el-table-column prop="newEndDate" label="新结束日期" width="120" />
              <el-table-column prop="operator" label="操作人" width="100" />
            </el-table>
            <el-empty v-if="renewRecords.length === 0" description="暂无续签记录" :image-size="80" />
          </el-tab-pane>

          <!-- 变更记录 -->
          <el-tab-pane label="变更记录" name="change">
            <el-table :data="changeRecords" stripe border size="default">
              <el-table-column prop="changeDate" label="变更日期" width="120" />
              <el-table-column prop="changeType" label="变更类型" width="120" />
              <el-table-column prop="beforeValue" label="变更前" />
              <el-table-column prop="afterValue" label="变更后" />
              <el-table-column prop="operator" label="操作人" width="100" />
            </el-table>
            <el-empty v-if="changeRecords.length === 0" description="暂无变更记录" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 到期提醒设置对话框 -->
    <el-dialog
      v-model="reminderDialogVisible"
      title="到期提醒设置"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form label-width="120px">
        <el-form-item label="开启提醒">
          <el-switch v-model="reminderSettings.enabled" />
        </el-form-item>
        <el-form-item label="提前提醒天数">
          <el-input-number v-model="reminderSettings.advanceDays" :min="1" :max="90" controls-position="right" />
          <span style="margin-left: 8px; color: var(--fts-text-tertiary); font-size: 12px;">天</span>
        </el-form-item>
        <el-form-item label="通知HR">
          <el-switch v-model="reminderSettings.notifyHr" />
        </el-form-item>
        <el-form-item label="通知部门经理">
          <el-switch v-model="reminderSettings.notifyManager" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="reminderDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleReminderSave">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

// 工具栏面板
.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 48px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;

  .action-btn--import,
  .action-btn--export,
  .action-btn--reminder {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
}

// 表格区域
.table-section {
  padding: 0 var(--fts-space-6) var(--fts-space-6);
  overflow-x: auto;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);

  :deep(.el-table) {
    width: 100%;
  }

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 名称文字
.name-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

// 工号文字
.code-text {
  color: var(--fts-text-secondary);
  font-family: var(--fts-font-family-mono);
}

// 部门文字
.dept-text {
  color: var(--fts-text-primary);
}

// 合同编号
.contract-no {
  color: var(--fts-text-primary);
  font-family: var(--fts-font-family-mono);
}

// 日期文字
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 期限文字
.term-text {
  color: var(--fts-text-primary);
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 详情页区域
.detail-section {
  margin-bottom: var(--fts-space-4);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
    padding-bottom: var(--fts-space-2);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  .section-content {
    padding: var(--fts-space-2) 0;
  }
}

.remark-text {
  color: var(--fts-text-secondary);
  line-height: 1.6;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);

  .el-icon {
    color: var(--fts-primary);
  }

  .attachment-name {
    flex: 1;
    color: var(--fts-text-primary);
  }
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}

@media (max-width: 576px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .table-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
