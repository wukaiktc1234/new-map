<script setup lang="ts">
/**
 * 印章管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成印章管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Download,
  Search,
  Refresh,
  Stamp,
  Edit,
  View,
  Delete,
  Upload,
  CircleCheck,
  CircleClose,
  Calendar,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { sealApi } from '@/api/seal'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import {
  type SealInfo,
  type SealQueryForm,
  type SealStatus,
  type SealType,
  SealTypeOptions,
  SealStatusOptions,
  SealTypeLabelMap,
  SealStatusLabelMap,
} from '@/types/seal'

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

/** 印章使用记录 */
interface SealUsageRecord {
  recordId: string
  sealId: string
  userName: string
  department: string
  reason: string
  useDate: string
  expectedReturnDate: string
  actualReturnDate?: string
  documentName: string
  status: 'using' | 'returned'
  remark?: string
}

/** 印章详情扩展数据（包含模拟字段） */
interface SealDetail extends SealInfo {
  sealCode: string
  department: string
  storageLocation: string
  lastUsedTime: string
}

/** 使用登记表单数据 */
interface UsageFormData {
  sealId: string
  userName: string
  department: string
  reason: string
  useDate: string
  expectedReturnDate: string
  documentName: string
  remark: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<SealInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)

/** 使用登记对话框显隐 */
const usageDialogVisible = ref(false)
/** 归还登记对话框显隐 */
const returnDialogVisible = ref(false)
/** 详情对话框显隐 */
const detailDialogVisible = ref(false)
/** 当前选中的印章 */
const currentSeal = ref<SealDetail | null>(null)

const formRef = ref<FormInstance>()
const usageFormRef = ref<FormInstance>()
const returnFormRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  sealType: '' as SealType | '',
  keeper: '',
  status: '' as SealStatus | '',
  dateRange: [] as string[],
})

// 印章详情扩展数据（本地缓存，存储表单中输入但后端未持久化的字段）
const sealDetailMap = reactive<Record<string, SealDetail>>({})

// 印章使用记录（本地缓存用于 UI 即时反馈，实际使用记录已通过 sealApi.recordUsage 持久化到后端）
const usageRecords = ref<SealUsageRecord[]>([])

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<SealInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: Record<string, unknown> = {}
      if (params.keyword) convertedParams.keyword = params.keyword
      if (params.sealType) convertedParams.sealType = params.sealType
      if (params.status) convertedParams.status = params.status
      if (params.keeper) convertedParams.keeper = params.keeper
      convertedParams.page = params.page
      convertedParams.size = params.size
      return sealApi.getList(convertedParams as unknown as SealQueryForm)
    },
  } as unknown as CrudApi<SealInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive({
  sealId: '',
  sealName: '',
  sealType: 'official' as SealType,
  sealCode: '',
  department: '',
  keeper: '',
  storageLocation: '',
  sealImage: '',
  remark: '',
})

// 使用登记表单数据
const usageFormData = reactive<UsageFormData>({
  sealId: '',
  userName: '',
  department: '',
  reason: '',
  useDate: '',
  expectedReturnDate: '',
  documentName: '',
  remark: '',
})

// 归还登记表单数据
const returnFormData = reactive({
  recordId: '',
  sealId: '',
  returnDate: '',
  remark: '',
})

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  inUse: usageRecords.value.filter(r => r.status === 'using').length,
  returned: usageRecords.value.filter(r => r.status === 'returned').length,
  todayUsed: usageRecords.value.filter(r => r.useDate === new Date().toISOString().slice(0, 10)).length,
}))

// 部门选项（从真实 API 加载，使用共享 composable，autoLoad 自动加载）
const { departmentOptions } = useDepartmentOptions(true)

// 导出加载状态
const exportLoading = ref(false)

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'sealCode', label: '印章编号', minWidth: 120, slot: 'sealCode' },
  { prop: 'sealName', label: '印章名称', minWidth: 140, slot: 'sealName' },
  { prop: 'sealType', label: '印章类型', minWidth: 110, slot: 'sealType' },
  { prop: 'department', label: '保管部门', minWidth: 110, slot: 'department' },
  { prop: 'keeper', label: '保管人', minWidth: 100 },
  { prop: 'status', label: '当前状态', minWidth: 90, slot: 'status', ellipsis: false },
  { prop: 'lastUsedTime', label: '最后使用时间', minWidth: 160, slot: 'lastUsedTime' },
  { prop: '_operation', label: '操作', width: 320, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  sealName: [
    { required: true, message: '请输入印章名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  sealType: [{ required: true, message: '请选择印章类型', trigger: 'change' }],
  sealCode: [{ required: true, message: '请输入印章编号', trigger: 'blur' }],
  department: [{ required: true, message: '请输入保管部门', trigger: 'blur' }],
  keeper: [{ required: true, message: '请输入保管人', trigger: 'blur' }],
  storageLocation: [{ required: true, message: '请输入存放位置', trigger: 'blur' }],
}

const usageFormRules: FormRules = {
  userName: [{ required: true, message: '请输入使用人', trigger: 'blur' }],
  department: [{ required: true, message: '请输入使用部门', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入使用事由', trigger: 'blur' }],
  useDate: [{ required: true, message: '请选择使用日期', trigger: 'change' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还日期', trigger: 'change' }],
}

const returnFormRules: FormRules = {
  returnDate: [{ required: true, message: '请选择归还日期', trigger: 'change' }],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.sealType = ''
  queryForm.value.keeper = ''
  queryForm.value.status = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: SealInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    sealId: '',
    sealName: '',
    sealType: 'official' as SealType,
    sealCode: `SEAL${Date.now().toString(36).toUpperCase()}`,
    department: '',
    keeper: '',
    storageLocation: '',
    sealImage: '',
    remark: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: SealInfo) {
  isEdit.value = true
  try {
    const detail = await sealApi.getById(row.sealId)
    if (detail) {
      const extra = sealDetailMap[row.sealId] || {
        ...row,
        sealCode: '',
        department: '',
        storageLocation: '',
        lastUsedTime: '',
      }
      Object.assign(formData, {
        ...detail,
        ...extra,
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载印章详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = {
      sealName: formData.sealName,
      sealType: formData.sealType,
      sealImage: formData.sealImage,
      keeper: formData.keeper,
      authorizedUsers: [],
      authorizedScenes: [],
      remark: formData.remark,
    }

    if (isEdit.value && formData.sealId) {
      await sealApi.update(formData.sealId, submitData)
      sealDetailMap[formData.sealId] = {
        ...(sealDetailMap[formData.sealId] || ({} as SealDetail)),
        sealCode: formData.sealCode,
        department: formData.department,
        storageLocation: formData.storageLocation,
      }
      ElMessage.success('更新成功')
    } else {
      const newSeal = await sealApi.create(submitData)
      sealDetailMap[newSeal.sealId] = {
        ...newSeal,
        sealCode: formData.sealCode,
        department: formData.department,
        storageLocation: formData.storageLocation,
        lastUsedTime: '',
      }
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

/** 使用登记 */
function handleUsageRegister(row: SealInfo) {
  Object.assign(usageFormData, {
    sealId: row.sealId,
    userName: '',
    department: '',
    reason: '',
    useDate: new Date().toISOString().slice(0, 10),
    expectedReturnDate: '',
    documentName: '',
    remark: '',
  })
  usageDialogVisible.value = true
}

/** 提交使用登记
 * 印章使用记录是法律合规要求，必须持久化到后端，禁止仅存内存
 * 调用 sealApi.recordUsage 创建后端使用记录
 */
async function handleUsageSubmit() {
  if (!usageFormRef.value) return

  const valid = await usageFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 调用后端 API 持久化印章使用记录（法律合规要求）
    // TODO: 后端 recordUsage 的 businessType 为 SealScene 类型（hr_contract/purchase_contract/electronic_contract），
    // 与本页通用印章借用场景不完全匹配，待后端调整字段后优化映射
    await sealApi.recordUsage(
      usageFormData.sealId,
      'hr_contract',
      usageFormData.documentName || usageFormData.sealId,
      usageFormData.documentName || '',
      usageFormData.userName,
    )

    // API 成功后才创建本地记录（用于 UI 即时反馈）
    const newRecord: SealUsageRecord = {
      recordId: String(Date.now()),
      sealId: usageFormData.sealId,
      userName: usageFormData.userName,
      department: usageFormData.department,
      reason: usageFormData.reason,
      useDate: usageFormData.useDate,
      expectedReturnDate: usageFormData.expectedReturnDate,
      documentName: usageFormData.documentName,
      status: 'using',
      remark: usageFormData.remark,
    }
    usageRecords.value.unshift(newRecord)
    ElMessage.success('使用登记成功')
    usageDialogVisible.value = false
    refresh()
  } catch {
    // 后端持久化失败时不创建本地记录，避免假成功
    ElMessage.error('使用登记失败：印章使用记录未持久化到后端')
  } finally {
    submitLoading.value = false
  }
}

/** 归还登记 */
function handleReturnRegister(row: SealInfo) {
  const usingRecord = usageRecords.value.find(
    r => r.sealId === row.sealId && r.status === 'using'
  )
  if (!usingRecord) {
    ElMessage.warning('该印章当前没有使用中的记录')
    return
  }
  Object.assign(returnFormData, {
    recordId: usingRecord.recordId,
    sealId: row.sealId,
    returnDate: new Date().toISOString().slice(0, 10),
    remark: '',
  })
  returnDialogVisible.value = true
}

/** 提交归还登记
 * 印章归还记录同样是法律合规要求，必须持久化到后端
 * TODO: 后端暂无专门的归还 API，当前通过 recordUsage 记录归还事件，
 * 待后端提供归还接口后替换为真实调用
 */
async function handleReturnSubmit() {
  if (!returnFormRef.value) return

  const valid = await returnFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const record = usageRecords.value.find(r => r.recordId === returnFormData.recordId)
    if (!record) {
      ElMessage.warning('未找到对应的使用记录')
      return
    }

    // 调用后端 API 持久化归还记录（法律合规要求）
    // TODO: 后端 recordUsage 的 businessType 与归还场景不完全匹配，待后端调整
    await sealApi.recordUsage(
      record.sealId,
      'hr_contract',
      record.recordId,
      record.documentName || '',
      record.userName,
    )

    // API 成功后才更新本地记录
    record.status = 'returned'
    record.actualReturnDate = returnFormData.returnDate
    record.remark = returnFormData.remark || record.remark
    ElMessage.success('归还登记成功')
    returnDialogVisible.value = false
    refresh()
  } catch {
    // 后端持久化失败时不更新本地记录，避免假成功
    ElMessage.error('归还登记失败：归还记录未持久化到后端')
  } finally {
    submitLoading.value = false
  }
}

/** 查看详情 */
async function handleViewDetail(row: SealInfo) {
  try {
    const detail = await sealApi.getById(row.sealId)
    if (detail) {
      const extra = sealDetailMap[row.sealId] || {
        ...row,
        sealCode: '',
        department: '',
        storageLocation: '',
        lastUsedTime: '',
      }
      currentSeal.value = {
        ...detail,
        ...extra,
      }
      detailDialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

/** 删除印章 */
async function handleDelete(row: SealInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要删除印章「${row.sealName}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await sealApi.delete(row.sealId)
    delete sealDetailMap[row.sealId]
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/** 导出数据 */
async function handleExport() {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    const filename = `印章数据_${timestamp}.xlsx`

    const csvContent = [
      ['印章编号', '印章名称', '印章类型', '保管部门', '保管人', '状态', '创建时间'].join(','),
      ...tableData.value.map(row => {
        const extra = sealDetailMap[row.sealId] || {}
        return [
          extra.sealCode || row.sealId,
          row.sealName,
          SealTypeLabelMap[row.sealType],
          extra.department || '-',
          row.keeper,
          SealStatusLabelMap[row.status],
          row.createTime,
        ].join(',')
      }),
    ].join('\n')

    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/** 印章图片上传处理 */
async function handleSealImageChange(file: UploadFile) {
  if (!file.raw) return
  const isImage = file.raw.type === 'image/png' || file.raw.type === 'image/jpeg'
  if (!isImage) {
    ElMessage.error('印章图片仅支持 PNG/JPG 格式')
    return
  }
  const isLt2M = file.raw.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('印章图片大小不能超过 2MB')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    formData.sealImage = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
}

/** 移除印章图片 */
function handleRemoveSealImage() {
  formData.sealImage = ''
}

// ==================== 辅助方法 ====================

function getSealTypeStatus(sealType: SealType): string {
  const map: Record<SealType, string> = {
    official: 'primary',
    finance: 'warning',
    contract: 'success',
    legal: 'info',
    custom: 'info',
  }
  return map[sealType] || 'info'
}

function getSealStatusColor(status: SealStatus): string {
  const map: Record<SealStatus, string> = {
    active: 'success',
    inactive: 'warning',
    revoked: 'error',
  }
  return map[status] || 'info'
}

function getSealDetail(row: SealInfo): SealDetail {
  return sealDetailMap[row.sealId] || {
    ...row,
    sealCode: '',
    department: '',
    storageLocation: '',
    lastUsedTime: '',
  }
}

function getSealUsageRecords(sealId: string): SealUsageRecord[] {
  return usageRecords.value.filter(r => r.sealId === sealId)
}

function formatTime(iso: string): string {
  if (!iso || iso === '-') return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

// 部门数据由 useDepartmentOptions(true) 自动加载，无需手动调用
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="印章管理" description="管理企业印章的使用和登记">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增印章
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Stamp" label="印章总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Upload" label="使用中" :value="String(statistics.inUse)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="已归还" :value="String(statistics.returned)" color-type="success" variant="bordered" />
      <StatCard icon="Calendar" label="今日使用" :value="String(statistics.todayUsed)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="印章名称"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.sealType"
            placeholder="印章类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in SealTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-input
            v-model="queryForm.keeper"
            placeholder="保管人"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in SealStatusOptions"
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
            style="width: 240px"
            size="default"
            :teleported="false"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button type="success" size="default" @click="handleCreate">
            <el-icon :size="14"><Plus /></el-icon>新增印章
          </el-button>
          <el-button type="warning" size="default" :disabled="selectedRows.length !== 1" @click="handleUsageRegister(selectedRows[0])">
            <el-icon :size="14"><Upload /></el-icon>使用登记
          </el-button>
          <el-button type="info" size="default" :disabled="selectedRows.length !== 1" @click="handleReturnRegister(selectedRows[0])">
            <el-icon :size="14"><CircleCheck /></el-icon>归还登记
          </el-button>
          <el-button size="default" class="action-btn--export" :loading="exportLoading" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出
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
        <!-- 印章编号列 -->
        <template #sealCode="{ row }">
          <span class="code-text">{{ getSealDetail(row).sealCode }}</span>
        </template>

        <!-- 印章名称列 -->
        <template #sealName="{ row }">
          <div class="seal-cell">
            <el-avatar :size="36" :src="row.sealImage" :icon="Stamp" shape="square" />
            <div class="seal-info">
              <div class="seal-name">{{ row.sealName }}</div>
            </div>
          </div>
        </template>

        <!-- 印章类型列 -->
        <template #sealType="{ row }">
          <StatusTag :status="getSealTypeStatus(row.sealType)" :label="SealTypeLabelMap[row.sealType]" size="small" variant="light" />
        </template>

        <!-- 保管部门列 -->
        <template #department="{ row }">
          <span class="dept-text">{{ getSealDetail(row).department }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getSealStatusColor(row.status)" :label="SealStatusLabelMap[row.status]" size="small" variant="light" />
        </template>

        <!-- 最后使用时间列 -->
        <template #lastUsedTime="{ row }">
          <span class="time-text">{{ formatTime(getSealDetail(row).lastUsedTime) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              link
              type="warning"
              size="default"
              :disabled="row.status === 'revoked'"
              @click.stop="handleUsageRegister(row)"
            >
              使用登记
            </el-button>
            <el-button
              link
              type="success"
              size="default"
              :disabled="row.status === 'revoked'"
              @click.stop="handleReturnRegister(row)"
            >
              归还
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
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑印章' : '新增印章'"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="印章名称" prop="sealName">
              <el-input v-model="formData.sealName" placeholder="请输入印章名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="印章编号" prop="sealCode">
              <el-input v-model="formData.sealCode" placeholder="请输入印章编号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="印章类型" prop="sealType">
              <el-select v-model="formData.sealType" placeholder="请选择印章类型" :teleported="false" style="width: 100%">
                <el-option v-for="opt in SealTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保管部门" prop="department">
              <el-select v-model="formData.department" placeholder="请选择保管部门" :teleported="false" style="width: 100%">
                <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="保管人" prop="keeper">
              <el-input v-model="formData.keeper" placeholder="请输入保管人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放位置" prop="storageLocation">
              <el-input v-model="formData.storageLocation" placeholder="请输入存放位置" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="印章图片">
          <div class="seal-upload-area">
            <div v-if="formData.sealImage" class="seal-preview">
              <img :src="formData.sealImage" alt="印章预览" class="seal-img" />
              <el-button link type="danger" size="small" @click="handleRemoveSealImage">移除</el-button>
            </div>
            <el-upload
              v-else
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/png,image/jpeg"
              :on-change="handleSealImageChange"
            >
              <div class="seal-upload-trigger">
                <el-icon :size="28"><Plus /></el-icon>
                <span>点击上传印章</span>
              </div>
            </el-upload>
            <div class="seal-upload-tip">支持 PNG/JPG，建议透明背景，不超过 2MB</div>
          </div>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 使用登记对话框 -->
    <el-dialog
      v-model="usageDialogVisible"
      title="使用登记"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="usageFormRef" :model="usageFormData" :rules="usageFormRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用人" prop="userName">
              <el-input v-model="usageFormData.userName" placeholder="请输入使用人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用部门" prop="department">
              <el-select v-model="usageFormData.department" placeholder="请选择部门" :teleported="false" style="width: 100%">
                <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="使用事由" prop="reason">
          <el-input v-model="usageFormData.reason" type="textarea" :rows="2" placeholder="请输入使用事由" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用日期" prop="useDate">
              <el-date-picker
                v-model="usageFormData.useDate"
                type="date"
                placeholder="选择使用日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计归还日期" prop="expectedReturnDate">
              <el-date-picker
                v-model="usageFormData.expectedReturnDate"
                type="date"
                placeholder="选择预计归还日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="使用文件">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            multiple
            :on-change="(file) => { usageFormData.documentName = file.name }"
          >
            <el-button size="default">
              <el-icon><Upload /></el-icon>上传文件
            </el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、Excel 等格式</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="usageFormData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="usageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleUsageSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 归还登记对话框 -->
    <el-dialog
      v-model="returnDialogVisible"
      title="归还登记"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="returnFormRef" :model="returnFormData" :rules="returnFormRules" label-width="100px">
        <el-form-item label="归还日期" prop="returnDate">
          <el-date-picker
            v-model="returnFormData.returnDate"
            type="date"
            placeholder="选择归还日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :teleported="false"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="returnFormData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleReturnSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="印章详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentSeal" class="detail-container">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="3" border size="default">
            <el-descriptions-item label="印章编号">
              <span class="code-text">{{ currentSeal.sealCode }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="印章名称">
              {{ currentSeal.sealName }}
            </el-descriptions-item>
            <el-descriptions-item label="印章类型">
              <StatusTag :status="getSealTypeStatus(currentSeal.sealType)" :label="SealTypeLabelMap[currentSeal.sealType]" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="保管部门">
              {{ currentSeal.department }}
            </el-descriptions-item>
            <el-descriptions-item label="保管人">
              {{ currentSeal.keeper }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getSealStatusColor(currentSeal.status)" :label="SealStatusLabelMap[currentSeal.status]" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="存放位置">
              {{ currentSeal.storageLocation }}
            </el-descriptions-item>
            <el-descriptions-item label="最后使用时间">
              {{ formatTime(currentSeal.lastUsedTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatTime(currentSeal.createTime) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 印章图片 -->
        <div class="detail-section" v-if="currentSeal.sealImage">
          <div class="section-title">印章图片</div>
          <div class="seal-image-wrapper">
            <img :src="currentSeal.sealImage" alt="印章图片" class="seal-large-img" />
          </div>
        </div>

        <!-- 备注 -->
        <div class="detail-section" v-if="currentSeal.remark">
          <div class="section-title">备注</div>
          <div class="remark-text">{{ currentSeal.remark }}</div>
        </div>

        <!-- 使用记录 -->
        <div class="detail-section">
          <div class="section-title">使用记录</div>
          <el-table :data="getSealUsageRecords(currentSeal.sealId)" stripe size="default" style="width: 100%">
            <el-table-column prop="useDate" label="使用日期" width="110" />
            <el-table-column prop="userName" label="使用人" width="100" />
            <el-table-column prop="department" label="使用部门" width="100" />
            <el-table-column prop="reason" label="使用事由" min-width="150" show-overflow-tooltip />
            <el-table-column prop="expectedReturnDate" label="预计归还" width="110" />
            <el-table-column prop="actualReturnDate" label="实际归还" width="110">
              <template #default="{ row }">
                {{ row.actualReturnDate || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <StatusTag :status="row.status === 'using' ? 'warning' : 'success'" :label="row.status === 'using' ? '使用中' : '已归还'" size="small" />
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无使用记录" :image-size="60" />
            </template>
          </el-table>
        </div>

        <!-- 归还记录 -->
        <div class="detail-section">
          <div class="section-title">归还记录</div>
          <el-table
            :data="getSealUsageRecords(currentSeal.sealId).filter(r => r.status === 'returned')"
            stripe
            size="default"
            style="width: 100%"
          >
            <el-table-column prop="actualReturnDate" label="归还日期" width="120" />
            <el-table-column prop="userName" label="使用人" width="100" />
            <el-table-column prop="reason" label="使用事由" min-width="150" show-overflow-tooltip />
            <el-table-column prop="documentName" label="使用文件" min-width="120" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <template #empty>
              <el-empty description="暂无归还记录" :image-size="60" />
            </template>
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 印章信息单元格
.seal-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.seal-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .seal-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 编号文字
.code-text {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

// 部门文字
.dept-text {
  color: var(--fts-text-primary);
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
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

// 印章上传区域
.seal-upload-area {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.seal-preview {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.seal-img {
  width: 96px;
  height: 96px;
  object-fit: contain;
  border: 1px dashed var(--fts-border-color);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-1);
  background: var(--fts-bg-secondary);
}

.seal-upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border: 1px dashed var(--fts-border-color);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  color: var(--fts-text-tertiary);
  gap: var(--fts-space-1);

  &:hover {
    border-color: var(--fts-primary);
    color: var(--fts-primary);
  }

  span {
    font-size: var(--fts-font-size-xs);
  }
}

.seal-upload-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.upload-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-top: var(--fts-space-1);
}

// 详情对话框
.detail-container {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

.seal-image-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
}

.seal-large-img {
  width: 160px;
  height: 160px;
  object-fit: contain;
}

.remark-text {
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
  color: var(--fts-text-primary);
  line-height: 1.6;
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
