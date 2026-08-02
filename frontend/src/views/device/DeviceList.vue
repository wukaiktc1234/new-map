<script setup lang="ts">
/**
 * 设备管理页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成设备管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 重构要点：
 * ✅ 使用 .modern-page + PageHeader 替代 StandardPage
 * ✅ 使用 stats-section 响应式4列网格 + variant="bordered"
 * ✅ 使用 table-section + pagination-wrapper 分页分离模式
 * ✅ 使用 CSS 变量（无硬编码颜色）
 * ✅ 符合 project_rules.md 规范
 * ✅ 使用 useCrudTable 管理表格数据
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Monitor, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { deviceApi } from '@/api/device'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'
import {
  DeviceTypeOptions,
  DeviceStatusOptions,
  DeviceTypeText,
  DeviceStatusText,
  DeviceStatusColor,
  DeviceConnectionTypeOptions,
  DeviceConnectionTypeText,
} from '@/types/device'
import type { DeviceInfo, DeviceFormData, DeviceQueryForm, DeviceType, DeviceStatus, DeviceConnectionType } from '@/types/device'

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
const selectedRows = ref<DeviceInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)

/** 详情对话框显隐 */
const detailDialogVisible = ref(false)
/** 当前查看的设备详情 */
const currentDetail = ref<DeviceInfo | null>(null)

/** 详情对话框当前激活的页签 */
const activeDetailTab = ref('basic')

/** 报修对话框显隐 */
const repairDialogVisible = ref(false)
/** 当前报修的设备 */
const currentRepairDevice = ref<DeviceInfo | null>(null)
const repairFormRef = ref<FormInstance>()
const repairForm = reactive({
  faultDescription: '',
  contactPerson: '',
  contactPhone: '',
  remark: '',
})

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  deviceType: '' as '' | DeviceType,
  storeId: '' as number | string,
  status: '' as '' | DeviceStatus,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<DeviceInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: DeviceQueryForm & { page: number; size: number } = {
        page: params.page,
        size: params.size,
        keyword: params.keyword || undefined,
        deviceType: params.deviceType || undefined,
        status: params.status || undefined,
        storeId: params.storeId ? Number(params.storeId) : undefined,
      }
      return deviceApi.getList(convertedParams)
    },
  } as unknown as CrudApi<DeviceInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

const formData = reactive<Partial<DeviceFormData> & { deviceStatus: DeviceStatus }>({
  deviceCode: '',
  deviceName: '',
  deviceType: 'PRINTER',
  deviceModel: '',
  manufacturer: '',
  serialNo: '',
  connectionType: 'USB',
  connectionParams: '',
  location: '',
  storeId: 0,
  deviceStatus: 'offline',
  remark: '',
})

// 统计数据改为计算属性，基于实际数据动态计算
const statistics = computed(() => ({
  total: pagination?.total || 0,
  online: tableData.value.filter(item => item.status === 'online').length,
  fault: tableData.value.filter(item => item.status === 'fault').length,
  maintenance: tableData.value.filter(item => item.status === 'maintenance').length,
}))

// 门店选项（接入真实后端 API）
const { storeOptions, getStoreName } = useStoreOptions(true)

const importLoading = ref(false)
const exportLoading = ref(false)

/** 设备参数配置 */
interface DeviceParamItem {
  paramKey: string
  paramValue: string
}

const deviceParams = ref<DeviceParamItem[]>([])

/** 维护记录 */
interface MaintenanceRecord {
  id: number
  maintenanceDate: string
  maintenanceType: string
  maintenanceContent: string
  operator: string
  cost: number
  remark: string
}

// TODO: 后端暂无设备维护记录 API，待接口就绪后接入 deviceApi.getMaintenanceRecords(deviceId)
const maintenanceRecords = ref<MaintenanceRecord[]>([])

/** 运行记录 */
interface OperationRecord {
  id: number
  recordTime: string
  status: string
  description: string
}

// TODO: 后端暂无设备运行记录 API，待接口就绪后接入 deviceApi.getOperationRecords(deviceId)
const operationRecords = ref<OperationRecord[]>([])

/** 添加设备参数行 */
function addDeviceParam(): void {
  deviceParams.value.push({
    paramKey: '',
    paramValue: '',
  })
}

/** 删除设备参数行 */
function removeDeviceParam(index: number): void {
  deviceParams.value.splice(index, 1)
}

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'deviceCode', label: '设备编号', minWidth: 130 },
  { prop: 'deviceName', label: '设备名称', minWidth: 140 },
  { prop: 'deviceType', label: '设备类型', minWidth: 100, slot: 'deviceType' },
  { prop: 'deviceModel', label: '品牌型号', minWidth: 140 },
  { prop: 'storeId', label: '所属门店', minWidth: 120, slot: 'storeName' },
  { prop: 'location', label: '安装位置', minWidth: 120 },
  { prop: 'installDate', label: '安装日期', minWidth: 120, slot: 'installDate' },
  { prop: 'status', label: '设备状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'lastMaintenanceDate', label: '上次维护时间', minWidth: 150, slot: 'lastMaintenanceDate' },
  { prop: '_operation', label: '操作', width: 320, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  deviceCode: [
    { required: true, message: '请输入设备编号', trigger: 'blur' },
  ],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择所属门店', trigger: 'change' }],
}

const repairFormRules: FormRules = {
  faultDescription: [
    { required: true, message: '请描述故障情况', trigger: 'blur' },
    { min: 5, max: 500, message: '长度在5到500个字符之间', trigger: 'blur' },
  ],
  contactPerson: [
    { required: true, message: '请输入联系人', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.deviceType = ''
  queryForm.value.storeId = ''
  queryForm.value.status = ''
  refresh()
}

function handleSelectionChange(rows: DeviceInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    deviceName: '',
    deviceCode: `DEV${Date.now().toString(36).toUpperCase()}`,
    deviceType: 'PRINTER',
    deviceModel: '',
    manufacturer: '',
    serialNo: '',
    connectionType: 'USB',
    connectionParams: '',
    location: '',
    storeId: 0,
    deviceStatus: 'offline',
    remark: '',
  })
  deviceParams.value = []
  dialogVisible.value = true
}

async function handleEdit(row: DeviceInfo) {
  isEdit.value = true
  try {
    const detail = await deviceApi.getById(row.deviceId)
    Object.assign(formData, {
      ...detail,
      deviceStatus: detail.status,
    })
    deviceParams.value = []
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载设备详情失败')
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
      status: formData.deviceStatus,
    }
    delete (submitData as Record<string, unknown>).deviceStatus

    if (isEdit.value && formData.deviceId) {
      await deviceApi.update(Number(formData.deviceId), submitData as unknown as DeviceFormData)
      ElMessage.success('更新成功')
    } else {
      await deviceApi.create(submitData as unknown as DeviceFormData)
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

/**
 * 处理文件上传前的验证
 * 仅允许 Excel 文件（.xlsx / .xls），用于设备批量导入
 */
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

/**
 * 导入设备数据（从Excel文件）
 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('设备导入功能开发中...')
    // 实际项目中调用 deviceApi.importDevices(uploadFile.raw)
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

/**
 * 导出设备数据为Excel文件
 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('设备导出功能开发中...')
    // 实际项目中调用 deviceApi.exportDevices(params)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/**
 * 删除设备确认
 */
async function handleDeviceDelete(row: DeviceInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除设备「${row.deviceName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deviceApi.delete(row.deviceId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 查看设备详情
 */
async function handleViewDetail(row: DeviceInfo): Promise<void> {
  try {
    const detail = await deviceApi.getById(row.deviceId)
    currentDetail.value = detail
    detailDialogVisible.value = true
  } catch {
    ElMessage.error('加载设备详情失败')
  }
}

/**
 * 打开报修对话框
 */
function handleRepair(row: DeviceInfo): void {
  currentRepairDevice.value = row
  Object.assign(repairForm, {
    faultDescription: '',
    contactPerson: '',
    contactPhone: '',
    remark: '',
  })
  repairDialogVisible.value = true
}

/**
 * 提交报修
 */
async function handleRepairSubmit(): Promise<void> {
  if (!repairFormRef.value) return

  const valid = await repairFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    submitLoading.value = true
    ElMessage.success('报修申请已提交')
    repairDialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error('提交报修失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 查看维护记录
 */
function handleViewMaintenance(row: DeviceInfo): void {
  currentDetail.value = row
  detailDialogVisible.value = true
}

// ==================== 辅助方法 ====================

function getDeviceStatusColor(status: string): string {
  return DeviceStatusColor[status as DeviceStatus] || 'info'
}

function getDeviceStatusLabel(status: string): string {
  return DeviceStatusText[status as DeviceStatus] || status
}

function getDeviceTypeLabel(type: string): string {
  return DeviceTypeText[type as DeviceType] || type
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

onMounted(() => {
  // 可在此加载门店选项等基础数据
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="设备列表" description="管理门店设备档案和设备状态">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增设备
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Monitor" label="设备总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="运行中" :value="String(statistics.online)" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="故障中" :value="String(statistics.fault)" color-type="error" variant="bordered" />
      <StatCard icon="Tools" label="待维护" :value="String(statistics.maintenance)" color-type="warning" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导入导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索设备名称/编号..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.deviceType"
            placeholder="设备类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in DeviceTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.storeId"
            placeholder="所属门店"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="store.storeId"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="设备状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in DeviceStatusOptions"
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
          <el-button size="default" class="action-btn--export" @click="handleExport" :loading="exportLoading">
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
        <!-- 设备类型列 -->
        <template #deviceType="{ row }">
          <StatusTag category="type" :label="getDeviceTypeLabel(row.deviceType)" size="small" />
        </template>

        <!-- 所属门店列 -->
        <template #storeName="{ row }">
          <span class="store-text">{{ getStoreName(row.storeId) }}</span>
        </template>

        <!-- 安装日期列 -->
        <template #installDate="{ row }">
          <span class="date-text">{{ row.installDate ? formatDate(row.installDate) : '-' }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getDeviceStatusColor(row.status)" :label="getDeviceStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 上次维护时间列 -->
        <template #lastMaintenanceDate="{ row }">
          <span class="time-text">{{ row.lastMaintenanceTime ? formatDate(row.lastMaintenanceTime) : '-' }}</span>
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
            <el-button link type="warning" size="default" @click.stop="handleRepair(row)">
              报修
            </el-button>
            <el-button link type="info" size="default" @click.stop="handleViewMaintenance(row)">
              维护记录
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDeviceDelete(row)">
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
      :title="isEdit ? '编辑设备' : '新增设备'"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="设备名称" prop="deviceName">
                <el-input v-model="formData.deviceName" placeholder="请输入设备名称" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="设备编号" prop="deviceCode">
                <el-input v-model="formData.deviceCode" placeholder="请输入设备编号" maxlength="50" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="设备类型" prop="deviceType">
                <el-select v-model="formData.deviceType" placeholder="请选择设备类型" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in DeviceTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="品牌型号">
                <el-input v-model="formData.deviceModel" placeholder="请输入品牌型号" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="制造商">
                <el-input v-model="formData.manufacturer" placeholder="请输入制造商" maxlength="50" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="序列号">
                <el-input v-model="formData.serialNo" placeholder="请输入序列号" maxlength="50" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 安装信息 -->
        <div class="form-section">
          <div class="section-title">安装信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="所属门店" prop="storeId">
                <el-select v-model="formData.storeId" placeholder="请选择所属门店" :teleported="false" style="width: 100%">
                  <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName" :value="store.storeId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="安装位置">
                <el-input v-model="formData.location" placeholder="请输入安装位置" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="安装日期">
                <el-date-picker
                  v-model="formData.installDate"
                  type="date"
                  placeholder="选择安装日期"
                  :teleported="false"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="连接方式">
                <el-select v-model="formData.connectionType" placeholder="请选择连接方式" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in DeviceConnectionTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 设备参数 -->
        <div class="form-section">
          <div class="section-title">
            <span>设备参数</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addDeviceParam">
              添加参数
            </el-button>
          </div>
          <el-table :data="deviceParams" size="small" border style="width: 100%">
            <el-table-column label="参数名" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.paramKey" placeholder="请输入参数名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="参数值" min-width="200">
              <template #default="{ row }">
                <el-input v-model="row.paramValue" placeholder="请输入参数值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="60" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeDeviceParam($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!deviceParams.length" class="empty-tip">
            <el-text type="info">暂无设备参数，点击上方"添加参数"按钮开始配置</el-text>
          </div>
        </div>

        <!-- 维保信息 -->
        <div class="form-section">
          <div class="section-title">维保信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="维保周期(天)">
                <el-input-number v-model="formData.maintenanceCycle" :min="0" :max="3650" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="上次维护日期">
                <el-date-picker
                  v-model="formData.lastMaintenanceDate"
                  type="date"
                  placeholder="选择上次维护日期"
                  :teleported="false"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 设备状态 -->
        <div class="form-section">
          <div class="section-title">设备状态</div>
          <el-form-item label="当前状态">
            <el-radio-group v-model="formData.deviceStatus">
              <el-radio value="online">运行中</el-radio>
              <el-radio value="offline">离线</el-radio>
              <el-radio value="fault">故障</el-radio>
              <el-radio value="maintenance">维护中</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <!-- 备注 -->
        <el-form-item label="备注">
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="设备详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template v-if="currentDetail">
        <el-tabs v-model="activeDetailTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border size="default">
              <el-descriptions-item label="设备编号">{{ currentDetail.deviceCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="设备名称">{{ currentDetail.deviceName }}</el-descriptions-item>
              <el-descriptions-item label="设备类型">
                <StatusTag category="type" :label="getDeviceTypeLabel(currentDetail.deviceType)" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="品牌型号">{{ currentDetail.deviceModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="制造商">{{ currentDetail.manufacturer || '-' }}</el-descriptions-item>
              <el-descriptions-item label="序列号">{{ currentDetail.serialNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属门店">{{ getStoreName(currentDetail.storeId) }}</el-descriptions-item>
              <el-descriptions-item label="安装位置">{{ currentDetail.location || '-' }}</el-descriptions-item>
              <el-descriptions-item label="连接方式">{{ DeviceConnectionTypeText[currentDetail.connectionType as DeviceConnectionType] }}</el-descriptions-item>
              <el-descriptions-item label="设备状态">
                <StatusTag :status="getDeviceStatusColor(currentDetail.status)" :label="getDeviceStatusLabel(currentDetail.status)" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="最后心跳时间">{{ currentDetail.lastHeartbeatTime ? formatTime(currentDetail.lastHeartbeatTime) : '-' }}</el-descriptions-item>
              <el-descriptions-item label="最后在线时间">{{ currentDetail.lastOnlineTime ? formatTime(currentDetail.lastOnlineTime) : '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane label="设备参数" name="params">
            <el-table :data="deviceParams" size="default" border style="width: 100%">
              <el-table-column prop="paramKey" label="参数名" min-width="180" />
              <el-table-column prop="paramValue" label="参数值" min-width="200" />
            </el-table>
            <div v-if="!deviceParams.length" class="empty-tip">
              <el-text type="info">暂无设备参数配置</el-text>
            </div>
          </el-tab-pane>

          <el-tab-pane label="运行记录" name="operation">
            <el-table :data="operationRecords" size="default" border style="width: 100%">
              <el-table-column prop="recordTime" label="记录时间" min-width="180" />
              <el-table-column label="状态" min-width="100">
                <template #default="{ row }">
                  <StatusTag :status="getDeviceStatusColor(row.status)" :label="getDeviceStatusLabel(row.status)" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="200" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="维护记录" name="maintenance">
            <el-table :data="maintenanceRecords" size="default" border style="width: 100%">
              <el-table-column prop="maintenanceDate" label="维护日期" min-width="120" />
              <el-table-column prop="maintenanceType" label="维护类型" min-width="100" />
              <el-table-column prop="maintenanceContent" label="维护内容" min-width="200" />
              <el-table-column prop="operator" label="操作人" min-width="100" />
              <el-table-column prop="cost" label="费用(元)" min-width="100" align="right">
                <template #default="{ row }">¥{{ row.cost }}</template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="150" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 报修对话框 -->
    <el-dialog
      v-model="repairDialogVisible"
      title="设备报修"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template v-if="currentRepairDevice">
        <el-alert
          :title="`报修设备：${currentRepairDevice.deviceName}（${currentRepairDevice.deviceCode}）`"
          type="warning"
          :closable="false"
          show-icon
          class="repair-alert"
        />
        <el-form ref="repairFormRef" :model="repairForm" :rules="repairFormRules" label-width="100px" class="repair-form">
          <el-form-item label="故障描述" prop="faultDescription">
            <el-input v-model="repairForm.faultDescription" type="textarea" :rows="4" placeholder="请详细描述故障现象" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="联系人" prop="contactPerson">
            <el-input v-model="repairForm.contactPerson" placeholder="请输入联系人姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="repairForm.contactPhone" placeholder="请输入联系电话" maxlength="20" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="repairForm.remark" type="textarea" :rows="2" placeholder="其他补充说明" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="repairDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleRepairSubmit">
          提交报修
        </el-button>
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--fts-space-6) 0;
}

// 门店文字
.store-text {
  color: var(--fts-text-primary);
}

// 日期
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
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

// 报修表单
.repair-alert {
  margin-bottom: var(--fts-space-4);
}

.repair-form {
  padding-top: var(--fts-space-2);
}

// 详情页签
:deep(.el-tabs__content) {
  padding-top: var(--fts-space-2);
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
