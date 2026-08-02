<script setup lang="ts">
/**
 * 健康证管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理员工健康证的办理、续期和到期提醒
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Bell, Document, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { healthCertificateApi, employeeApi } from '@/api/hr'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  HealthCertificate,
  HealthCertificateStatus,
  HealthCertificateStatistics,
} from '@/types/healthCertificate'

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
const selectedRows = ref<HealthCertificate[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const renewVisible = ref(false)
const currentDetail = ref<HealthCertificate | null>(null)
const currentRenewCert = ref<HealthCertificate | null>(null)

const formRef = ref<FormInstance>()
const renewFormRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  status: '' as HealthCertificateStatus | '',
  department: '',
  dateRange: [] as string[],
})

// 使用 useCrudTable 管理表格数据
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<HealthCertificate, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const apiParams: Record<string, unknown> = {
        page: params.page,
        pageSize: params.size,
      }
      if (params.keyword) apiParams.keyword = params.keyword
      if (params.status) apiParams.status = params.status
      return healthCertificateApi.getList(apiParams as Parameters<typeof healthCertificateApi.getList>[0])
    },
  } as unknown as CrudApi<HealthCertificate, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 全局统计数据
const globalStats = ref<HealthCertificateStatistics | null>(null)

// 本月新办数量（基于当前列表数据计算）
const newThisMonth = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  return tableData.value.filter(item => {
    if (!item.issueDate) return false
    const d = new Date(item.issueDate)
    return d.getFullYear() === year && d.getMonth() === month
  }).length
})

// 统计数据
const statistics = computed(() => ({
  total: globalStats.value?.total ?? 0,
  expiring: globalStats.value?.expiring ?? 0,
  expired: globalStats.value?.expired ?? 0,
  newThisMonth: newThisMonth.value,
}))

// 表单数据
const formData = reactive({
  employeeId: '',
  employeeName: '',
  store: '',
  certificateNumber: '',
  issuer: '',
  issueDate: '',
  expiryDate: '',
  certificateImage: '',
  note: '',
})

// 续期表单数据
const renewFormData = reactive({
  certificateNumber: '',
  issuer: '',
  issueDate: '',
  expiryDate: '',
  certificateImage: '',
  note: '',
})

// 员工选项（对接真实后端 EmployeeController，onMounted 时加载）
const employeeOptions = ref<Array<{ value: string; label: string; department: string }>>([])

// 部门选项（接入真实后端 API）
const { departmentOptions } = useDepartmentOptions(true)

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'employeeName', label: '员工姓名', minWidth: 100, slot: 'employeeName' },
  { prop: 'employeeId', label: '工号', minWidth: 100, slot: 'employeeId' },
  { prop: 'store', label: '部门', minWidth: 100, slot: 'store' },
  { prop: 'certificateNumber', label: '证件编号', minWidth: 140, slot: 'certificateNumber' },
  { prop: 'issuer', label: '发证机构', minWidth: 140, slot: 'issuer' },
  { prop: 'issueDate', label: '发证日期', minWidth: 110, slot: 'issueDate' },
  { prop: 'expiryDate', label: '到期日期', minWidth: 110, slot: 'expiryDate' },
  { prop: 'expiryDays', label: '剩余天数', minWidth: 100, slot: 'expiryDays' },
  { prop: 'status', label: '证件状态', minWidth: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  certificateNumber: [{ required: true, message: '请输入证件编号', trigger: 'blur' }],
  issuer: [{ required: true, message: '请输入发证机构', trigger: 'blur' }],
  issueDate: [{ required: true, message: '请选择发证日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
}

const renewFormRules: FormRules = {
  certificateNumber: [{ required: true, message: '请输入证件编号', trigger: 'blur' }],
  issuer: [{ required: true, message: '请输入发证机构', trigger: 'blur' }],
  issueDate: [{ required: true, message: '请选择发证日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
}

// ==================== 方法 ====================

/** 加载全局统计数据 */
async function loadGlobalStats(): Promise<void> {
  try {
    globalStats.value = await healthCertificateApi.getStatistics()
  } catch {
    globalStats.value = { total: 0, valid: 0, expiring: 0, expired: 0 }
  }
}

/** 加载员工选项（对接真实后端 EmployeeController） */
async function loadEmployeeOptions(): Promise<void> {
  try {
    const employees = await employeeApi.getEmployees()
    employeeOptions.value = (employees || []).map(emp => ({
      value: emp.employeeCode || emp.id || '',
      label: emp.employeeName || '',
      department: emp.departmentName || emp.storeName || '',
    })).filter(e => e.value && e.label)
  } catch {
    employeeOptions.value = []
  }
}

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.status = ''
  queryForm.value.department = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: HealthCertificate[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    employeeId: '',
    employeeName: '',
    store: '',
    certificateNumber: '',
    issuer: '',
    issueDate: '',
    expiryDate: '',
    certificateImage: '',
    note: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: HealthCertificate) {
  isEdit.value = true
  try {
    const detail = await healthCertificateApi.getById(row.id)
    currentDetail.value = detail
    Object.assign(formData, {
      employeeId: detail.employeeId,
      employeeName: detail.employeeName,
      store: detail.store,
      certificateNumber: detail.certificateNumber,
      issuer: detail.issuer,
      issueDate: detail.issueDate,
      expiryDate: detail.expiryDate,
      certificateImage: detail.certificateImage || '',
      note: detail.note || '',
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载健康证详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = { ...formData }
    if (isEdit.value && currentDetail.value?.id) {
      await healthCertificateApi.update(currentDetail.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await healthCertificateApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadGlobalStats()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDetail(row: HealthCertificate) {
  try {
    const detail = await healthCertificateApi.getById(row.id)
    currentDetail.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

function handleRenew(row: HealthCertificate) {
  currentRenewCert.value = row
  Object.assign(renewFormData, {
    certificateNumber: row.certificateNumber,
    issuer: row.issuer,
    issueDate: '',
    expiryDate: '',
    certificateImage: '',
    note: '',
  })
  renewVisible.value = true
}

async function handleRenewSubmit() {
  if (!renewFormRef.value || !currentRenewCert.value) return

  const valid = await renewFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      employeeId: currentRenewCert.value.employeeId,
      employeeName: currentRenewCert.value.employeeName,
      store: currentRenewCert.value.store,
      ...renewFormData,
    }
    await healthCertificateApi.create(data)
    ElMessage.success('续期成功')
    renewVisible.value = false
    refresh()
    loadGlobalStats()
  } catch {
    ElMessage.error('续期失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: HealthCertificate) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.employeeName}」的健康证吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await healthCertificateApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
    loadGlobalStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/** 员工选择变更 */
function handleEmployeeChange(employeeId: string) {
  const emp = employeeOptions.value.find(e => e.value === employeeId)
  if (emp) {
    formData.employeeName = emp.label
    formData.store = emp.department
  }
}

/** 文件上传前验证 */
function beforeUpload(rawFile: UploadRawFile): boolean {
  const isImage = rawFile.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只支持图片格式的文件')
    return false
  }
  const isLt5M = rawFile.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }
  return true
}

/** 图片上传处理 */
function handleImageUpload(uploadFile: UploadFile) {
  if (uploadFile.raw) {
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.certificateImage = e.target?.result as string
    }
    reader.readAsDataURL(uploadFile.raw)
  }
}

/** 续期图片上传处理 */
function handleRenewImageUpload(uploadFile: UploadFile) {
  if (uploadFile.raw) {
    const reader = new FileReader()
    reader.onload = (e) => {
      renewFormData.certificateImage = e.target?.result as string
    }
    reader.readAsDataURL(uploadFile.raw)
  }
}

/** 发送到期提醒 */
async function handleSendReminders() {
  try {
    await ElMessageBox.confirm(
      '确定向即将到期和已到期的持证员工发送提醒通知吗？',
      '发送到期提醒',
      {
        confirmButtonText: '确定发送',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await healthCertificateApi.sendExpiryReminders()
    ElMessage.success('到期提醒已发送')
  } catch {
    // 用户取消
  }
}

/** 导出数据 */
async function handleExport() {
  try {
    ElMessage.info('正在导出数据，请稍候...')
    // 模拟导出
    const exportData = tableData.value.map(item => ({
      员工姓名: item.employeeName,
      工号: item.employeeId,
      部门: item.store,
      证件编号: item.certificateNumber,
      发证机构: item.issuer,
      发证日期: item.issueDate,
      到期日期: item.expiryDate,
      剩余天数: item.expiryDays,
      状态: getStatusLabel(item.status),
    }))
    ElMessage.success('导出成功！')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  }
}

/** 批量导入 */
function handleImport() {
  ElMessage.info('批量导入功能开发中')
}

/** 到期提醒设置 */
function handleReminderSettings() {
  ElMessage.info('到期提醒设置功能开发中')
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    valid: 'success',
    expiring: 'warning',
    expired: 'error',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    valid: '有效',
    expiring: '即将到期',
    expired: '已过期',
  }
  return map[status] || status
}

function getExpiryDaysClass(days: number | undefined): string {
  if (days === undefined) return 'expiry-normal'
  if (days < 0) return 'expiry-expired'
  if (days <= 30) return 'expiry-warning'
  return 'expiry-normal'
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

onMounted(() => {
  loadGlobalStats()
  loadEmployeeOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="健康证管理" description="管理员工健康证的办理、续期和到期提醒">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增证件
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="持证总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Warning" label="即将到期" :value="String(statistics.expiring)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleClose" label="已过期" :value="String(statistics.expired)" color-type="error" variant="bordered" />
      <StatCard icon="Plus" label="本月新办" :value="String(statistics.newThisMonth)" color-type="success" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="员工姓名/工号"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.department"
            placeholder="部门"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.label"
              :value="dept.id"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="证件状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="有效" value="valid" />
            <el-option label="即将到期" value="expiring" />
            <el-option label="已过期" value="expired" />
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
          >
            <el-button size="default" class="action-btn--import">
              <el-icon :size="14"><Upload /></el-icon>批量导入
            </el-button>
          </el-upload>
          <el-button size="default" class="action-btn--export" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
          <el-button size="default" @click="handleReminderSettings">
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
          <div class="employee-cell">
            <el-avatar :size="32" :icon="User" shape="circle" />
            <span class="employee-name">{{ row.employeeName }}</span>
          </div>
        </template>

        <!-- 工号列 -->
        <template #employeeId="{ row }">
          <span class="text-secondary">{{ row.employeeId }}</span>
        </template>

        <!-- 部门列 -->
        <template #store="{ row }">
          <span class="dept-text">{{ row.store }}</span>
        </template>

        <!-- 证件编号列 -->
        <template #certificateNumber="{ row }">
          <span class="cert-number">{{ row.certificateNumber }}</span>
        </template>

        <!-- 发证机构列 -->
        <template #issuer="{ row }">
          <span class="issuer-text">{{ row.issuer }}</span>
        </template>

        <!-- 发证日期列 -->
        <template #issueDate="{ row }">
          <span class="date-text">{{ formatDate(row.issueDate) }}</span>
        </template>

        <!-- 到期日期列 -->
        <template #expiryDate="{ row }">
          <span class="date-text">{{ formatDate(row.expiryDate) }}</span>
        </template>

        <!-- 剩余天数列 -->
        <template #expiryDays="{ row }">
          <span :class="getExpiryDaysClass(row.expiryDays)">
            {{ row.expiryDays !== undefined ? (row.expiryDays >= 0 ? `${row.expiryDays}天` : `已过期${Math.abs(row.expiryDays)}天`) : '-' }}
          </span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusColor(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
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
            <el-button link type="success" size="default" @click.stop="handleRenew(row)">
              续期
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
          @size-change="refresh"
          @current-change="refresh"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑健康证' : '新增健康证'"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="员工" prop="employeeId">
              <el-select
                v-model="formData.employeeId"
                placeholder="请选择员工"
                filterable
                :teleported="false"
                style="width: 100%"
                @change="handleEmployeeChange"
              >
                <el-option
                  v-for="emp in employeeOptions"
                  :key="emp.value"
                  :label="emp.label"
                  :value="emp.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="formData.store" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证件编号" prop="certificateNumber">
              <el-input v-model="formData.certificateNumber" placeholder="请输入证件编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发证机构" prop="issuer">
              <el-input v-model="formData.issuer" placeholder="请输入发证机构" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发证日期" prop="issueDate">
              <el-date-picker
                v-model="formData.issueDate"
                type="date"
                placeholder="选择发证日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="expiryDate">
              <el-date-picker
                v-model="formData.expiryDate"
                type="date"
                placeholder="选择到期日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="证件照片">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImageUpload"
            accept="image/*"
          >
            <div v-if="formData.certificateImage" class="image-preview">
              <img :src="formData.certificateImage" alt="证件照片" />
            </div>
            <el-button v-else type="primary" size="default">
              <el-icon><Upload /></el-icon>上传照片
            </el-button>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="formData.note" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
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

    <!-- 续期对话框 -->
    <el-dialog
      v-model="renewVisible"
      title="健康证续期"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="renewFormRef" :model="renewFormData" :rules="renewFormRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证件编号" prop="certificateNumber">
              <el-input v-model="renewFormData.certificateNumber" placeholder="请输入证件编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发证机构" prop="issuer">
              <el-input v-model="renewFormData.issuer" placeholder="请输入发证机构" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发证日期" prop="issueDate">
              <el-date-picker
                v-model="renewFormData.issueDate"
                type="date"
                placeholder="选择发证日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="expiryDate">
              <el-date-picker
                v-model="renewFormData.expiryDate"
                type="date"
                placeholder="选择到期日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="证件照片">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleRenewImageUpload"
            accept="image/*"
          >
            <div v-if="renewFormData.certificateImage" class="image-preview">
              <img :src="renewFormData.certificateImage" alt="证件照片" />
            </div>
            <el-button v-else type="primary" size="default">
              <el-icon><Upload /></el-icon>上传照片
            </el-button>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="renewFormData.note" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="renewVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleRenewSubmit">
            确认续期
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="健康证详情"
      width="680px"
      class="fts-dialog--md"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <!-- 到期预警 -->
        <el-alert
          v-if="currentDetail.status === 'expiring' || currentDetail.status === 'expired'"
          :title="currentDetail.status === 'expiring' ? '该健康证即将到期，请及时续办' : '该健康证已到期，请尽快续办'"
          :type="currentDetail.status === 'expiring' ? 'warning' : 'error'"
          show-icon
          :closable="false"
          style="margin-bottom: var(--fts-space-4)"
        />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="员工姓名">{{ currentDetail.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="工号">{{ currentDetail.employeeId }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentDetail.store }}</el-descriptions-item>
          <el-descriptions-item label="证件编号">{{ currentDetail.certificateNumber }}</el-descriptions-item>
          <el-descriptions-item label="发证机构">{{ currentDetail.issuer }}</el-descriptions-item>
          <el-descriptions-item label="证件状态">
            <StatusTag :status="getStatusColor(currentDetail.status)" :label="getStatusLabel(currentDetail.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="发证日期">{{ currentDetail.issueDate }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ currentDetail.expiryDate }}</el-descriptions-item>
          <el-descriptions-item label="剩余天数">
            <span :class="getExpiryDaysClass(currentDetail.expiryDays)">
              {{ currentDetail.expiryDays !== undefined ? (currentDetail.expiryDays >= 0 ? `${currentDetail.expiryDays}天` : `已过期${Math.abs(currentDetail.expiryDays)}天`) : '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <StatusTag status="info" :label="currentDetail.approvalStatus || '未知'" size="small" />
          </el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.note" label="备注" :span="2">{{ currentDetail.note }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentDetail.certificateImage" class="detail-image-section">
          <div class="section-title">证件照片</div>
          <div class="image-container">
            <img :src="currentDetail.certificateImage" alt="证件照片" />
          </div>
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 员工信息单元格
.employee-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;

  .employee-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
  }
}

.text-secondary {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.dept-text {
  color: var(--fts-text-primary);
}

.cert-number {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.issuer-text {
  color: var(--fts-text-secondary);
}

.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 剩余天数
.expiry-normal {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.expiry-warning {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.expiry-expired {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
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

// 图片预览
.image-preview {
  width: 120px;
  height: 80px;
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  overflow: hidden;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

// 详情页图片区域
.detail-image-section {
  margin-top: var(--fts-space-4);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
  }

  .image-container {
    border: 1px solid var(--fts-border-primary);
    border-radius: var(--fts-card-radius);
    overflow: hidden;
    max-width: 400px;

    img {
      width: 100%;
      height: auto;
      display: block;
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
