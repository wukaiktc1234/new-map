<script setup lang="ts">
/**
 * 入职管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理新员工入职流程和手续办理
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, watch, onMounted, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Upload,
  Download,
  Search,
  Refresh,
  User,
  Edit,
  Promotion,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { onboardingApi } from '@/api/hr'
import { positionApi } from '@/api/hr/position'
import type { OnboardingArchive, PersonnelType } from '@/types/onboarding'

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

interface OnboardingRecord extends OnboardingArchive {
  id: number
  employeeNo: string
  currentStep: number
  totalSteps: number
  currentStepName: string
}

interface MaterialItem {
  id: string
  name: string
  required: boolean
  submitted: boolean
  submitTime?: string
}

interface ProcessStep {
  id: number
  name: string
  status: 'wait' | 'process' | 'finish' | 'error'
  operator?: string
  time?: string
  comment?: string
}

interface OperationLog {
  id: number
  action: string
  operator: string
  time: string
  comment?: string
}

// ==================== 静态配置数据 ====================

// 部门选项（接入真实后端 API）
const { departmentOptions, loadDepartments } = useDepartmentOptions(true)

// 职位选项（接入真实后端 /v1/hr/positions）
interface PositionOption { value: string; label: string; departmentId?: string }
const positionOptions = ref<PositionOption[]>([])
async function loadPositionOptions(): Promise<void> {
  try {
    const res = await positionApi.getList({ page: 1, size: 1000 })
    positionOptions.value = (res?.records ?? []).map((p: { positionId?: string | number; positionName?: string; departmentId?: string | number }) => ({
      value: String(p.positionName ?? ''),
      label: String(p.positionName ?? ''),
      departmentId: String(p.departmentId ?? ''),
    })).filter((p: PositionOption) => p.value)
  } catch {
    positionOptions.value = []
  }
}

/**
 * 部门下拉选项：去除树形前缀，并限制为部门级别（排除公司根节点）
 */
const departmentOptionsForSelect = computed(() => {
  return departmentOptions.value
    .filter((dept) => dept.type === 'department' || (dept.level && dept.level > 1))
    .map((dept) => ({
      ...dept,
      label: dept.name,
    }))
})

/**
 * 根据所选部门过滤职位选项
 */
const positionOptionsForSelect = computed(() => {
  if (!formData.departmentId) return []
  return positionOptions.value.filter((p) => p.departmentId === String(formData.departmentId))
})

/**
 * 部门变更时联动清空职位
 */
function handleDepartmentChange(): void {
  formData.position = ''
}

// 入职档案状态选项（与后端 OnboardingArchive.status 保持一致）
const statusOptions = [
  { value: 'CREATED', label: '已创建' },
  { value: 'PENDING_HR', label: '待HR审查' },
  { value: 'PENDING_SUBSTANTIVE', label: '待实质审查' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'CONTRACT_PENDING', label: '待签合同' },
  { value: 'CONTRACT_SIGNED', label: '合同已签' },
  { value: 'REGISTERED', label: '已注册' },
  { value: 'REJECTED', label: '已拒绝' },
]

const processSteps = [
  { id: 1, name: '入职登记' },
  { id: 2, name: '资料审核' },
  { id: 3, name: '合同签署' },
  { id: 4, name: '培训安排' },
  { id: 5, name: '岗位试用' },
  { id: 6, name: '转正评估' },
]

// 人员类型选项
const personnelTypeOptions: { value: PersonnelType; label: string }[] = [
  { value: 'social', label: '社会招聘' },
  { value: 'school', label: '校园招聘/应届生' },
  { value: 'intern', label: '实习生' },
  { value: 'returnee', label: '返聘' },
]

/**
 * 根据人员类型动态生成入职材料清单
 * 社招/返聘：离职证明为必传；校招/实习生：离职证明为选传
 */
function getMaterialTemplates(personnelType: PersonnelType = 'social') {
  const baseMaterials = [
    { id: '1', name: '身份证复印件', required: true },
    { id: '2', name: '学历证书复印件', required: true },
    { id: '4', name: '体检报告', required: true },
    { id: '7', name: '照片（一寸）', required: true },
    { id: '5', name: '银行卡复印件', required: false },
    { id: '6', name: '社保转移单', required: false },
  ]

  const needResignationLetter = personnelType === 'social' || personnelType === 'returnee'
  return [
    ...baseMaterials.slice(0, 2),
    { id: '3', name: '离职证明', required: needResignationLetter },
    ...baseMaterials.slice(2),
  ]
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<OnboardingRecord[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const importLoading = ref(false)
const exportLoading = ref(false)
const currentDetail = ref<OnboardingRecord | null>(null)
const processDialogVisible = ref(false)
const processStepIndex = ref(0)
const processComment = ref('')
const processLoading = ref(false)

const formRef = ref<FormInstance>()

const queryForm = ref({
  keyword: '',
  departmentId: '' as string | number,
  status: '' as string,
  dateRange: [] as string[],
})

interface OnboardingQueryParams {
  keyword: string
  departmentId: string | number
  status: string
  dateRange: string[]
}

/**
 * 后端入职档案 → 页面展示记录转换
 * 页面统计/详情使用统一的后端状态语义，仅状态标签做中文映射
 */
function toOnboardingRecord(archive: OnboardingArchive): OnboardingRecord {
  return {
    ...archive,
    id: archive.id ?? 0,
    employeeNo: archive.employeeCode ?? '',
    currentStep: computeCurrentStep(archive.status),
    totalSteps: processSteps.length,
    currentStepName: processSteps[computeCurrentStep(archive.status) - 1]?.name ?? '-',
  }
}

/**
 * 根据后端状态计算当前流程步骤（用于进度展示）
 */
function computeCurrentStep(status?: string): number {
  switch (status) {
    case 'CREATED': return 1
    case 'PENDING_HR': return 2
    case 'PENDING_SUBSTANTIVE': return 3
    case 'APPROVED': return 4
    case 'CONTRACT_PENDING': return 5
    case 'CONTRACT_SIGNED': return 5
    case 'REGISTERED': return 6
    case 'REJECTED': return 1
    default: return 1
  }
}

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<OnboardingRecord, OnboardingQueryParams>({
  api: {
    getList: async (params: OnboardingQueryParams & { page: number; size: number }) => {
      try {
        const res = await onboardingApi.getList({
          page: params.page,
          size: params.size,
          status: params.status || undefined,
          keyword: params.keyword || undefined,
        })
        const records = (res.records ?? []).map(toOnboardingRecord)
        // 前端按部门和日期范围二次过滤（后端未支持这两个查询条件）
        const filtered = records.filter((row) => {
          if (params.departmentId && String(row.departmentId) !== String(params.departmentId)) {
            return false
          }
          if (params.dateRange && params.dateRange.length === 2) {
            const d = row.onboardDate
            if (d && (d < params.dateRange[0] || d > params.dateRange[1])) {
              return false
            }
          }
          return true
        })
        return {
          records: filtered,
          total: filtered.length,
          current: params.page,
          size: params.size,
          pages: Math.ceil(filtered.length / params.size) || 1,
        }
      } catch (error: unknown) {
        if (error instanceof Error) {
          ElMessage.error(error.message || '加载入职列表失败')
        }
        return { records: [], total: 0, current: params.page, size: params.size, pages: 1 }
      }
    },
  } as unknown as CrudApi<OnboardingRecord, OnboardingQueryParams>,
  queryForm: queryForm as Ref<OnboardingQueryParams>,
  autoLoad: true,
})

// 基于列表数据计算统计卡片（后端暂无独立统计接口）
const statistics = computed(() => {
  const allData = tableData.value
  const now = new Date()
  const currentMonth = now.getMonth()
  const currentYear = now.getFullYear()

  return {
    pending: allData.filter(item => ['CREATED', 'PENDING_HR', 'PENDING_SUBSTANTIVE'].includes(item.status || '')).length,
    inProgress: allData.filter(item => ['APPROVED', 'CONTRACT_PENDING', 'CONTRACT_SIGNED'].includes(item.status || '')).length,
    completed: allData.filter(item => item.status === 'REGISTERED').length,
    thisMonth: allData.filter(item => {
      if (!item.onboardDate) return false
      const date = new Date(item.onboardDate)
      return date.getFullYear() === currentYear && date.getMonth() === currentMonth
    }).length,
  }
})

const columns = computed<ColumnDef[]>(() => [
  { prop: 'candidateName', label: '员工姓名', minWidth: 100, slot: 'candidateName' },
  { prop: 'employeeNo', label: '工号', minWidth: 110, slot: 'employeeNo' },
  { prop: 'departmentName', label: '部门', minWidth: 110, slot: 'departmentName' },
  { prop: 'position', label: '职位', minWidth: 100, slot: 'position' },
  { prop: 'onboardDate', label: '入职日期', minWidth: 110, slot: 'onboardDate' },
  { prop: 'status', label: '入职状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'currentStepName', label: '当前流程节点', minWidth: 120, slot: 'currentStepName' },
  { prop: '_operation', label: '操作', width: 280, fixed: 'right', slot: 'operation' },
])

// ==================== 表单数据 ====================

const formData = reactive({
  candidateName: '',
  employeeNo: '',
  departmentId: '',
  position: '',
  positionLevel: '',
  onboardDate: '',
  phone: '',
  email: '',
  idCard: '',
  address: '',
  personnelType: 'social' as PersonnelType,
  materials: [] as MaterialItem[],
})

const formRules: FormRules = {
  candidateName: [
    { required: true, message: '请输入员工姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在2到20个字符之间', trigger: 'blur' },
  ],
  employeeNo: [
    { required: true, message: '请输入工号', trigger: 'blur' },
  ],
  departmentId: [
    { required: true, message: '请选择部门', trigger: 'change' },
  ],
  position: [
    { required: true, message: '请输入职位', trigger: 'blur' },
  ],
  onboardDate: [
    { required: true, message: '请选择入职日期', trigger: 'change' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

// ==================== 详情数据 ====================

const detailSteps = ref<ProcessStep[]>([])
const detailMaterials = ref<MaterialItem[]>([])
const detailLogs = ref<OperationLog[]>([])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.departmentId = ''
  queryForm.value.status = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: OnboardingRecord[]) {
  selectedRows.value = rows
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    CREATED: 'info',
    PENDING_HR: 'warning',
    PENDING_SUBSTANTIVE: 'warning',
    APPROVED: 'primary',
    CONTRACT_PENDING: 'primary',
    CONTRACT_SIGNED: 'primary',
    REGISTERED: 'success',
    REJECTED: 'inactive',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    CREATED: '已创建',
    PENDING_HR: '待HR审查',
    PENDING_SUBSTANTIVE: '待实质审查',
    APPROVED: '已通过',
    CONTRACT_PENDING: '待签合同',
    CONTRACT_SIGNED: '合同已签',
    REGISTERED: '已注册',
    REJECTED: '已拒绝',
  }
  return map[status] || status
}

function getPersonnelTypeLabel(type?: PersonnelType): string {
  const map: Record<string, string> = {
    social: '社会招聘',
    school: '校园招聘/应届生',
    intern: '实习生',
    returnee: '返聘',
  }
  return type ? (map[type] || type) : '社会招聘'
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    candidateName: '',
    employeeNo: `EMP${String(Date.now()).slice(-6)}`,
    departmentId: '',
    position: '',
    positionLevel: '',
    onboardDate: '',
    phone: '',
    email: '',
    idCard: '',
    address: '',
    personnelType: 'social',
    materials: getMaterialTemplates('social').map(m => ({
      id: m.id,
      name: m.name,
      required: m.required,
      submitted: false,
    })),
  })
  dialogVisible.value = true
}

function handleEdit(row: OnboardingRecord) {
  isEdit.value = true
  currentDetail.value = row
  const personnelType = row.personnelType || 'social'
  Object.assign(formData, {
    candidateName: row.candidateName,
    employeeNo: row.employeeNo,
    departmentId: row.departmentId,
    position: row.position,
    positionLevel: row.positionLevel,
    onboardDate: row.onboardDate || '',
    phone: row.phone || '',
    email: row.email,
    idCard: row.idCard || '',
    address: row.address || '',
    personnelType,
    materials: getMaterialTemplates(personnelType).map((m, idx) => ({
      id: m.id,
      name: m.name,
      required: m.required,
      submitted: idx < (row.currentStep || 0),
      submitTime: idx < (row.currentStep || 0) ? row.createTime : undefined,
    })),
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const selectedDept = departmentOptionsForSelect.value.find(d => String(d.id) === String(formData.departmentId))
    const payload = {
      candidateName: formData.candidateName,
      email: formData.email,
      phone: formData.phone,
      idCard: formData.idCard,
      position: formData.position,
      positionLevel: formData.positionLevel,
      departmentId: String(formData.departmentId),
      departmentName: selectedDept?.name,
      onboardDate: formData.onboardDate,
      address: formData.address,
      personnelType: formData.personnelType,
    }
    if (isEdit.value && currentDetail.value) {
      await onboardingApi.update(currentDetail.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await onboardingApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitLoading.value = false
  }
}

function handleDetail(row: OnboardingRecord) {
  currentDetail.value = row

  detailSteps.value = processSteps.map((step, idx) => {
    let status: 'wait' | 'process' | 'finish' | 'error' = 'wait'
    let operator = ''
    let time = ''

    if (row.status === 'REJECTED') {
      status = idx < row.currentStep ? 'finish' : idx === row.currentStep ? 'error' : 'wait'
    } else if (row.status === 'REGISTERED') {
      status = 'finish'
      operator = 'HR管理员'
      time = row.createTime
    } else if (['PENDING_HR', 'PENDING_SUBSTANTIVE', 'APPROVED', 'CONTRACT_PENDING', 'CONTRACT_SIGNED'].includes(row.status || '')) {
      if (idx < row.currentStep) {
        status = 'finish'
        operator = 'HR管理员'
        time = row.createTime
      } else if (idx === row.currentStep) {
        status = 'process'
      }
    }

    return {
      id: step.id,
      name: step.name,
      status,
      operator,
      time,
    }
  })

  detailMaterials.value = getMaterialTemplates(row.personnelType || 'social').map((m, idx) => ({
    id: m.id,
    name: m.name,
    required: m.required,
    submitted: idx < (row.currentStep || 0) + 1,
    submitTime: idx < (row.currentStep || 0) + 1 ? row.createTime : undefined,
  }))

  detailLogs.value = [
    { id: 1, action: '创建入职档案', operator: 'HR管理员', time: row.createTime || '', comment: '入职档案创建成功' },
    { id: 2, action: '提交资料', operator: row.candidateName, time: row.createTime || '', comment: '员工提交个人资料' },
    { id: 3, action: '资料审核通过', operator: 'HR管理员', time: row.createTime || '', comment: '资料审核通过，进入下一流程' },
  ]

  detailVisible.value = true
}

function handlePromote(row: OnboardingRecord) {
  if (row.status === 'REGISTERED' || row.status === 'REJECTED') {
    ElMessage.warning('当前状态无法推进流程')
    return
  }

  processStepIndex.value = row.currentStep
  processComment.value = ''
  currentDetail.value = row
  processDialogVisible.value = true
}

async function handleProcessSubmit() {
  if (!currentDetail.value) return

  processLoading.value = true
  try {
    // 后端当前仅支持提交审批（CREATED → PENDING_HR）
    // 后续可扩展为按 stepIndex 推进多节点审批
    await onboardingApi.submit(currentDetail.value.id)
    ElMessage.success('流程已推进')
    processDialogVisible.value = false
    refresh()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '推进失败')
    }
  } finally {
    processLoading.value = false
  }
}

async function handleDelete(row: OnboardingRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.candidateName}」的入职记录吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await onboardingApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

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

async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  // TODO: 后端入职批量导入接口未就绪（POST /v1/hr/onboarding/import）
  // 当前明确提示功能开发中，不显示假成功
  importLoading.value = true
  try {
    ElMessage.info('入职档案批量导入功能开发中，请稍后再试')
  } finally {
    importLoading.value = false
  }
}

async function handleExport(): Promise<void> {
  // TODO: 后端入职档案导出接口未就绪（GET /v1/hr/onboarding/export）
  // 当前明确提示功能开发中，不显示假成功
  exportLoading.value = true
  try {
    ElMessage.info('入职档案导出功能开发中，请稍后再试')
  } finally {
    exportLoading.value = false
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  return iso
}

/**
 * 人员类型变化时重新生成材料清单，保留已提交状态
 */
watch(() => formData.personnelType, (newType) => {
  const oldMaterials = formData.materials
  const newTemplates = getMaterialTemplates(newType)
  formData.materials = newTemplates.map((m) => {
    const old = oldMaterials.find((om) => om.id === m.id)
    return {
      ...m,
      submitted: old ? old.submitted : false,
    }
  })
})

onMounted(() => {
  loadPositionOptions()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="入职办理" description="管理新员工入职流程和手续办理">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>添加入职
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard icon="User" label="待入职" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Promotion" label="入职中" :value="String(statistics.inProgress)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Calendar" label="本月入职" :value="String(statistics.thisMonth)" color-type="info" variant="bordered" />
    </section>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索员工姓名或工号..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.departmentId"
            placeholder="部门"
            clearable
            style="width: 130px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptionsForSelect"
              :key="dept.id"
              :label="dept.label"
              :value="dept.id"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="入职状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
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
        </div>
      </div>
    </div>

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
        <template #candidateName="{ row }">
          <div class="employee-cell">
            <el-avatar :size="36" :icon="User" shape="circle" />
            <div class="employee-info">
              <div class="employee-name">{{ row.candidateName }}</div>
            </div>
          </div>
        </template>

        <template #employeeNo="{ row }">
          <span class="text-primary">{{ row.employeeNo }}</span>
        </template>

        <template #departmentName="{ row }">
          <span class="text-normal">{{ row.departmentName }}</span>
        </template>

        <template #position="{ row }">
          <span class="text-normal">{{ row.position }}</span>
        </template>

        <template #onboardDate="{ row }">
          <span class="text-secondary">{{ formatDate(row.onboardDate || '') }}</span>
        </template>

        <template #status="{ row }">
          <StatusTag :status="getStatusType(row.status || '')" :label="getStatusLabel(row.status || '')" size="small" variant="light" />
        </template>

        <template #currentStepName="{ row }">
          <div class="step-info">
            <span class="step-name">{{ row.currentStepName }}</span>
            <span class="step-progress">({{ row.currentStep }}/{{ row.totalSteps }})</span>
          </div>
        </template>

        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              link
              type="success"
              size="default"
              :disabled="row.status === 'COMPLETED' || row.status === 'CANCELLED'"
              @click.stop="handlePromote(row)"
            >
              推进流程
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
              删除
            </el-button>
          </div>
        </template>
      </DataTable>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑入职' : '添加入职'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="员工姓名" prop="candidateName">
                <el-input v-model="formData.candidateName" placeholder="请输入员工姓名" maxlength="20" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="工号" prop="employeeNo">
                <el-input v-model="formData.employeeNo" placeholder="请输入工号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="部门" prop="departmentId">
                <el-select v-model="formData.departmentId" placeholder="请选择部门" :teleported="false" style="width: 100%" @change="handleDepartmentChange">
                  <el-option v-for="dept in departmentOptionsForSelect" :key="dept.id" :label="dept.label" :value="dept.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职位" prop="position">
                <el-select v-model="formData.position" placeholder="请先选择部门再选择职位" filterable :teleported="false" style="width: 100%" :disabled="!formData.departmentId">
                  <el-option v-for="pos in positionOptionsForSelect" :key="pos.value" :label="pos.label" :value="pos.label" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="人员类型">
                <el-select v-model="formData.personnelType" placeholder="请选择人员类型" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in personnelTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="入职日期" prop="onboardDate">
                <el-date-picker
                  v-model="formData.onboardDate"
                  type="date"
                  placeholder="请选择入职日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="职级">
                <el-select v-model="formData.positionLevel" placeholder="请选择职级" :teleported="false" style="width: 100%">
                  <el-option label="初级" value="初级" />
                  <el-option label="中级" value="中级" />
                  <el-option label="高级" value="高级" />
                  <el-option label="资深" value="资深" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">个人信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="身份证号">
                <el-input v-model="formData.idCard" placeholder="请输入身份证号" maxlength="18" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="现住址">
                <el-input v-model="formData.address" placeholder="请输入现住址" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">入职材料清单</div>
          <el-table :data="formData.materials" size="small" border style="width: 100%">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="材料名称" min-width="200" />
            <el-table-column label="是否必需" width="100" align="center">
              <template #default="{ row }">
                <StatusTag v-if="row.required" status="error" label="必需" size="small" />
                <StatusTag v-else status="info" label="可选" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="提交状态" width="120" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.submitted">已提交</el-checkbox>
              </template>
            </el-table-column>
          </el-table>
        </div>
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

    <el-dialog
      v-model="detailVisible"
      title="入职详情"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <div v-if="currentDetail" class="detail-container">
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="员工姓名">{{ currentDetail.candidateName }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ currentDetail.employeeNo }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ currentDetail.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ currentDetail.position }}</el-descriptions-item>
            <el-descriptions-item label="职级">{{ currentDetail.positionLevel }}</el-descriptions-item>
            <el-descriptions-item label="人员类型">{{ getPersonnelTypeLabel(currentDetail.personnelType) }}</el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ formatDate(currentDetail.onboardDate || '') }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ currentDetail.phone }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentDetail.email }}</el-descriptions-item>
            <el-descriptions-item label="现住址">{{ currentDetail.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getStatusType(currentDetail.status || '')" :label="getStatusLabel(currentDetail.status || '')" size="small" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="section-title">入职流程进度</div>
          <el-steps :active="currentDetail.currentStep" finish-status="success" align-center>
            <el-step
              v-for="step in detailSteps"
              :key="step.id"
              :title="step.name"
              :status="step.status"
            />
          </el-steps>
        </div>

        <div class="detail-section">
          <div class="section-title">材料清单</div>
          <el-table :data="detailMaterials" size="small" border style="width: 100%">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="name" label="材料名称" min-width="200" />
            <el-table-column label="是否必需" width="100" align="center">
              <template #default="{ row }">
                <StatusTag v-if="row.required" status="error" label="必需" size="small" />
                <StatusTag v-else status="info" label="可选" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="提交状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag v-if="row.submitted" status="success" label="已提交" size="small" />
                <StatusTag v-else status="info" label="未提交" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="160" align="center">
              <template #default="{ row }">
                {{ row.submitTime || '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="detail-section">
          <div class="section-title">操作记录</div>
          <el-timeline>
            <el-timeline-item
              v-for="log in detailLogs"
              :key="log.id"
              :timestamp="log.time"
              placement="top"
            >
              <div class="log-item">
                <span class="log-action">{{ log.action }}</span>
                <span class="log-operator"> - {{ log.operator }}</span>
                <div v-if="log.comment" class="log-comment">{{ log.comment }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleEdit(currentDetail!)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="processDialogVisible"
      title="推进流程"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <div v-if="currentDetail" class="process-dialog">
        <div class="process-info">
          <p><strong>员工：</strong>{{ currentDetail.candidateName }}</p>
          <p><strong>当前节点：</strong>{{ currentDetail.currentStepName }} ({{ currentDetail.currentStep }}/{{ currentDetail.totalSteps }})</p>
          <p><strong>下一节点：</strong>{{ processSteps[processStepIndex + 1]?.name || '已完成' }}</p>
        </div>
        <el-form label-width="80px">
          <el-form-item label="推进意见">
            <el-input
              v-model="processComment"
              type="textarea"
              :rows="3"
              placeholder="请输入推进意见（选填）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="processDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="processLoading" @click="handleProcessSubmit">
            确认推进
          </el-button>
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
  .action-btn--export {
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

.employee-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.employee-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .employee-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.text-primary {
  color: var(--fts-primary);
  font-weight: var(--fts-font-weight-medium);
}

.text-normal {
  color: var(--fts-text-primary);
}

.text-secondary {
  color: var(--fts-text-secondary);
}

.step-info {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);

  .step-name {
    color: var(--fts-text-primary);
  }

  .step-progress {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
  }
}

.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.detail-container {
  .detail-section {
    margin-bottom: var(--fts-space-4);

    .section-title {
      margin-bottom: var(--fts-space-3);
      font-weight: var(--fts-font-weight-semibold);
      font-size: var(--fts-font-size-base);
      color: var(--fts-text-primary);
    }
  }
}

.log-item {
  .log-action {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  .log-operator {
    color: var(--fts-text-secondary);
  }

  .log-comment {
    margin-top: var(--fts-space-1);
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
  }
}

.process-dialog {
  .process-info {
    margin-bottom: var(--fts-space-4);
    padding: var(--fts-space-3);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);

    p {
      margin: var(--fts-space-1) 0;
      color: var(--fts-text-primary);
    }
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
