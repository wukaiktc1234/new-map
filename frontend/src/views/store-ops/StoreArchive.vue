<script setup lang="ts">
/**
 * 门店档案管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理门店的基础信息、配置和运营状态
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, OfficeBuilding } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { storeArchiveApi, storeArchiveConverter } from '@/api/store-ops/store-archive'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  StoreArchive,
  StoreArchiveCreateForm,
  StoreArchiveUpdateForm,
  StoreArchiveQuery,
  StoreArchiveStats,
  StoreStatus,
  StoreType,
} from '@/types/store-operation/store-archive'
import {
  STORE_TYPE_OPTIONS,
  STORE_STATUS_OPTIONS,
  getStoreTypeLabel,
  getStoreStatusLabel,
} from '@/types/store-operation/store-archive'

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
const selectedRows = ref<StoreArchive[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailData = ref<StoreArchive | null>(null)
const isEdit = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  storeType: '' as StoreType | '',
  status: '' as StoreStatus | '',
  area: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<StoreArchive, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: StoreArchiveQuery = {
        current: params.page,
        size: params.size,
        storeName: params.keyword || undefined,
        storeType: params.storeType || undefined,
        status: params.status || undefined,
      }
      return storeArchiveApi.getList(queryParams)
    },
  } as unknown as CrudApi<StoreArchive, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const stats = ref<StoreArchiveStats>({
  total: 0,
  byStatus: { running: 0, renovating: 0, paused: 0, closed: 0 },
  byType: { direct: 0, franchise: 0, cooperation: 0 },
  licenseExpiringSoon: 0,
})

// 表单数据
const formData = reactive<Partial<StoreArchiveCreateForm>>({
  storeCode: '',
  storeName: '',
  storeType: 'direct',
  address: '',
  phone: '',
  businessHoursStart: '09:00:00',
  businessHoursEnd: '22:00:00',
  areaSize: 0,
  area: '',
  managerId: null,
  openDate: '',
  status: 'running',
  licenseNo: '',
  licenseExpiry: '',
  configJson: '',
  remark: '',
})

// 区域选项
// 备注：当前内置 6 个一线城市，后续可改为从后端字典 API 动态加载
const areaOptions = ref([
  { value: 'beijing', label: '北京' },
  { value: 'shanghai', label: '上海' },
  { value: 'guangzhou', label: '广州' },
  { value: 'shenzhen', label: '深圳' },
  { value: 'hangzhou', label: '杭州' },
  { value: 'chengdu', label: '成都' },
])

const importLoading = ref(false)
const exportLoading = ref(false)

// ==================== 计算属性 ====================

const statistics = computed(() => ({
  total: stats.value.total,
  running: stats.value.byStatus.running,
  paused: stats.value.byStatus.paused,
  closed: stats.value.byStatus.closed,
}))

const columns = computed<ColumnDef[]>(() => [
  { prop: 'storeCode', label: '门店编码', minWidth: 120 },
  { prop: 'storeName', label: '门店名称', minWidth: 160 },
  { prop: 'storeType', label: '门店类型', minWidth: 100, slot: 'storeType' },
  { prop: 'area', label: '所属区域', minWidth: 110, slot: 'area' },
  { prop: 'address', label: '地址', minWidth: 200, ellipsis: true },
  { prop: 'phone', label: '联系电话', minWidth: 130 },
  { prop: 'managerId', label: '店长', minWidth: 100, slot: 'manager' },
  { prop: 'status', label: '营业状态', minWidth: 100, slot: 'status' },
  { prop: 'openDate', label: '开业日期', minWidth: 120 },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  storeCode: [
    { required: true, message: '请输入门店编码', trigger: 'blur' },
    { min: 2, max: 32, message: '编码长度 2-32 字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '仅允许字母、数字、下划线、连字符', trigger: 'blur' },
  ],
  storeName: [
    { required: true, message: '请输入门店名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度 2-100 字符', trigger: 'blur' },
  ],
  storeType: [{ required: true, message: '请选择门店类型', trigger: 'change' }],
  phone: [
    { pattern: /^$|^1[3-9]\d{9}$|^\d{3,4}-?\d{7,8}$/, message: '电话格式不正确', trigger: 'blur' },
  ],
  businessHoursStart: [
    { pattern: /^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$/, message: '时间格式 HH:mm 或 HH:mm:ss', trigger: 'blur' },
  ],
  businessHoursEnd: [
    { pattern: /^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$/, message: '时间格式', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

/** 加载统计数据 */
async function loadStats(): Promise<void> {
  try {
    stats.value = await storeArchiveApi.getStats()
  } catch {
    // 静默失败，统计卡片不影响主功能
  }
}

function handleSearch() {
  refresh()
  loadStats()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.storeType = ''
  queryForm.value.status = ''
  queryForm.value.area = ''
  refresh()
  loadStats()
}

function handleSelectionChange(rows: StoreArchive[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    storeCode: `STORE${Date.now().toString(36).toUpperCase()}`,
    storeName: '',
    storeType: 'direct',
    address: '',
    phone: '',
    businessHoursStart: '09:00:00',
    businessHoursEnd: '22:00:00',
    areaSize: 0,
    area: '',
    managerId: null,
    openDate: '',
    status: 'running',
    licenseNo: '',
    licenseExpiry: '',
    configJson: '',
    remark: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: StoreArchive) {
  isEdit.value = true
  try {
    const detail = await storeArchiveApi.getById(row.storeId)
    Object.assign(formData, detail)
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载门店详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && formData.storeId) {
      const updateForm: StoreArchiveUpdateForm = {
        storeName: formData.storeName,
        storeType: formData.storeType as StoreType,
        address: formData.address,
        phone: formData.phone,
        businessHoursStart: formData.businessHoursStart,
        businessHoursEnd: formData.businessHoursEnd,
        areaSize: formData.areaSize,
        area: formData.area,
        managerId: formData.managerId,
        openDate: formData.openDate,
        status: formData.status as StoreStatus,
        licenseNo: formData.licenseNo,
        licenseExpiry: formData.licenseExpiry,
        configJson: formData.configJson,
        remark: formData.remark,
      }
      await storeArchiveApi.update(formData.storeId, updateForm)
      ElMessage.success('更新成功')
    } else {
      // 创建前校验编码唯一性
      if (formData.storeCode) {
        const check = await storeArchiveApi.checkCodeAvailable(formData.storeCode)
        if (!check.available) {
          ElMessage.error(`门店编码已存在：${formData.storeCode}`)
          return
        }
      }
      await storeArchiveApi.create(formData as StoreArchiveCreateForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadStats()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 处理文件上传前的验证
 * 仅允许 Excel 文件（.xlsx / .xls），用于门店批量导入
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
 * 导入门店数据（从Excel文件）
 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('导入功能开发中...')
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

/**
 * 导出门店数据为Excel文件
 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('导出功能开发中...')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/**
 * 删除门店确认
 */
async function handleDelete(row: StoreArchive): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除门店「${row.storeName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await storeArchiveApi.delete(row.storeId)
    ElMessage.success('删除成功')
    refresh()
    loadStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 切换门店营业状态
 */
async function handleStatusToggle(row: StoreArchive): Promise<void> {
  const isRunning = row.status === 'running'
  const newStatus: StoreStatus = isRunning ? 'paused' : 'running'
  const text = isRunning ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${text}门店「${row.storeName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await storeArchiveApi.updateStatus(row.storeId, newStatus)
    ElMessage.success(`${text}成功`)
    await refresh()
    loadStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/** 查看详情 */
async function handleViewDetail(row: StoreArchive): Promise<void> {
  try {
    const detail = await storeArchiveApi.getById(row.storeId)
    detailData.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载门店详情失败')
  }
}

/** 格式化营业时间显示 */
function formatBusinessHours(row: StoreArchive): string {
  if (!row.businessHoursStart && !row.businessHoursEnd) return '-'
  return `${row.businessHoursStart || '--'} ~ ${row.businessHoursEnd || '--'}`
}

/** 获取区域显示标签：匹配内置选项则返回中文标签，否则原样返回自定义值 */
function getAreaLabel(area: string | null | undefined): string {
  if (!area) return '-'
  const matched = areaOptions.value.find((opt) => opt.value === area)
  return matched ? matched.label : area
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 页面挂载时加载统计数据
onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="门店档案" description="管理门店的基础信息、配置和运营状态">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增门店
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="OfficeBuilding" label="门店总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="营业中" :value="String(statistics.running)" color-type="success" variant="bordered" />
      <StatCard icon="Clock" label="休息中" :value="String(statistics.paused)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleClose" label="已关闭" :value="String(statistics.closed)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导入导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索门店名称或编码..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.storeType"
            placeholder="门店类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in STORE_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="营业状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in STORE_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.area"
            placeholder="区域"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="area in areaOptions"
              :key="area.value"
              :label="area.label"
              :value="area.value"
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
              <el-icon :size="14"><Upload /></el-icon>导入
            </el-button>
          </el-upload>
          <el-button
            size="default"
            class="action-btn--export"
            :loading="exportLoading"
            @click="handleExport"
          >
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
        <!-- 门店类型列 -->
        <template #storeType="{ row }">
          <StatusTag
            :status="storeArchiveConverter.toTypeTagStatus(row.storeType as StoreType)"
            :label="getStoreTypeLabel(row.storeType as StoreType)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 所属区域列 -->
        <template #area="{ row }">
          <span>{{ getAreaLabel(row.area) }}</span>
        </template>

        <!-- 店长列 -->
        <template #manager="{ row }">
          <span>{{ row.managerId || '-' }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="storeArchiveConverter.toStatusTagStatus(row.status as StoreStatus)"
            :label="getStoreStatusLabel(row.status as StoreStatus)"
            size="small"
            variant="light"
          />
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
              :type="row.status === 'running' ? 'warning' : 'primary'"
              size="default"
              @click.stop="handleStatusToggle(row)"
            >
              {{ row.status === 'running' ? '停用' : '启用' }}
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
      :title="isEdit ? '编辑门店' : '新增门店'"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="门店名称" prop="storeName">
                <el-input v-model="formData.storeName" placeholder="请输入门店名称" maxlength="100" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="门店编码" prop="storeCode">
                <el-input v-model="formData.storeCode" :disabled="isEdit" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="门店类型" prop="storeType">
                <el-select v-model="formData.storeType" placeholder="请选择门店类型" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in STORE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属区域">
                <el-select
                  v-model="formData.area"
                  placeholder="请选择或输入区域"
                  :teleported="false"
                  filterable
                  allow-create
                  default-first-option
                  clearable
                  style="width: 100%"
                >
                  <el-option v-for="area in areaOptions" :key="area.value" :label="area.label" :value="area.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="地址" prop="address">
            <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="请输入详细地址" maxlength="500" show-word-limit />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系电话" prop="phone">
                <el-input v-model="formData.phone" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="店长">
                <el-input-number
                  v-model="formData.managerId"
                  :min="1"
                  :step="1"
                  controls-position="right"
                  placeholder="店长员工ID"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 营业信息 -->
        <div class="form-section">
          <div class="section-title">营业信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开业日期" prop="openDate">
                <el-date-picker
                  v-model="formData.openDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择开业日期"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="营业状态" prop="status">
                <el-select v-model="formData.status" placeholder="请选择营业状态" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in STORE_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="营业开始" prop="businessHoursStart">
                <el-time-picker
                  v-model="formData.businessHoursStart"
                  value-format="HH:mm:ss"
                  format="HH:mm"
                  placeholder="选择开始时间"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="营业结束" prop="businessHoursEnd">
                <el-time-picker
                  v-model="formData.businessHoursEnd"
                  value-format="HH:mm:ss"
                  format="HH:mm"
                  placeholder="选择结束时间"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="面积(㎡)" prop="areaSize">
                <el-input-number
                  v-model="formData.areaSize"
                  :min="0"
                  :max="999999.99"
                  :precision="2"
                  :step="10"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 设备配置 -->
        <div class="form-section">
          <div class="section-title">设备配置</div>
          <el-form-item label="配置信息">
            <el-input
              v-model="formData.configJson"
              type="textarea"
              :rows="4"
              placeholder="请输入设备配置信息（JSON格式）"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 备注 -->
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" show-word-limit />
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
      v-model="detailVisible"
      title="门店详情"
      width="720px"
      destroy-on-close
    >
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="门店编码">{{ detailData.storeCode }}</el-descriptions-item>
        <el-descriptions-item label="门店名称">{{ detailData.storeName }}</el-descriptions-item>
        <el-descriptions-item label="门店类型">
          <StatusTag
            :status="storeArchiveConverter.toTypeTagStatus(detailData.storeType as StoreType)"
            :label="getStoreTypeLabel(detailData.storeType as StoreType)"
            size="small"
          />
        </el-descriptions-item>
        <el-descriptions-item label="营业状态">
          <StatusTag
            :status="storeArchiveConverter.toStatusTagStatus(detailData.status as StoreStatus)"
            :label="getStoreStatusLabel(detailData.status as StoreStatus)"
            size="small"
          />
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="店长">{{ detailData.managerId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属区域">{{ getAreaLabel(detailData.area) }}</el-descriptions-item>
        <el-descriptions-item label="开业日期">{{ detailData.openDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="面积(㎡)">{{ detailData.areaSize || '-' }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ formatBusinessHours(detailData) }}</el-descriptions-item>
        <el-descriptions-item label="许可证号">{{ detailData.licenseNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="许可证到期">{{ detailData.licenseExpiry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detailData.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备配置" :span="2">{{ detailData.configJson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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

// 表单分区
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
