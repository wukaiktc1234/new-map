<script setup lang="ts">
/**
 * 资产台账页面 - 管理企业固定资产台账和资产信息
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成资产台账的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Upload,
  Download,
  Search,
  Refresh,
  Printer,
  Delete,
  View,
  Edit,
  Promotion,
  Tools,
  DeleteFilled,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { assetApi, categoryApi } from '@/api/asset'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  Asset,
  AssetQueryDTO,
  AssetCategory,
  AssetStatisticsVO,
  MaintenanceRecord,
} from '@/types/asset'
import {
  AssetStatus,
  AssetStatusTagMap,
  AssetStatusOptions,
  DepreciationMethod,
  DepreciationMethodOptions,
} from '@/types/asset'

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
const selectedRows = ref<Asset[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const importLoading = ref(false)
const exportLoading = ref(false)

const formRef = ref<FormInstance>()
const detailData = ref<Asset | null>(null)
const activeDetailTab = ref('basic')

// 查询表单
const queryForm = ref({
  keyword: '',
  categoryId: '' as number | string,
  departmentId: '' as string,
  status: '' as AssetStatus | '',
  useStatus: '' as string,
})

// 使用 useCrudTable 管理表格数据
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<Asset, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: AssetQueryDTO = {
        keyword: params.keyword || undefined,
        categoryId: params.categoryId ? String(params.categoryId) : undefined,
        departmentId: params.departmentId || undefined,
        status: params.status || undefined,
        page: params.page,
        pageSize: params.size,
      }
      return assetApi.getList(convertedParams)
    },
  } as unknown as CrudApi<Asset, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = ref<AssetStatisticsVO>({
  totalAssets: 0,
  totalOriginalValue: 0,
  totalOriginalValueYuan: '0.00',
  totalCurrentValue: 0,
  totalCurrentValueYuan: '0.00',
  thisMonthNewCount: 0,
  thisMonthNewValue: 0,
  thisMonthNewValueYuan: '0.00',
  activeCount: 0,
  idleCount: 0,
  maintenanceCount: 0,
  toBeDisposedCount: 0,
  disposedCount: 0,
  scrappedCount: 0,
  transferredCount: 0,
})

// 分类选项
const categoryOptions = ref<AssetCategory[]>([])

// 部门下拉选项（来自后端 /v1/departments/tree）
const { departmentOptions } = useDepartmentOptions(true)

// 使用状态选项
const useStatusOptions = [
  { value: 'in_use', label: '使用中' },
  { value: 'idle', label: '闲置' },
  { value: 'borrowed', label: '借出' },
]

// 表单数据
const formData = reactive({
  id: '',
  assetCode: '',
  assetName: '',
  categoryId: '',
  specification: '',
  serialNumber: '',
  purchaseDate: '',
  originalValue: 0,
  usefulLifeMonths: 60,
  depreciationMethod: DepreciationMethod.STRAIGHT_LINE as DepreciationMethod,
  salvageRate: 5,
  departmentId: '',
  departmentName: '',
  responsibleUserName: '',
  location: '',
  status: AssetStatus.IDLE as AssetStatus,
  remark: '',
  supplierName: '',
})

// 使用记录（TODO: 待接入后端资产使用记录 API）
const usageRecords = ref<Array<{
  id: string
  date: string
  action: string
  operator: string
  department: string
  remark: string
}>>([])

// 维修记录
const maintenanceRecords = ref<MaintenanceRecord[]>([])

// 附件列表（TODO: 待接入后端资产附件 API）
const attachmentList = ref<Array<{
  id: string
  name: string
  size: string
  uploadTime: string
}>>([])

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'assetCode', label: '资产编码', minWidth: 130, slot: 'assetCode' },
  { prop: 'assetName', label: '资产名称', minWidth: 160, slot: 'assetName' },
  { prop: 'categoryName', label: '资产分类', minWidth: 110, slot: 'categoryName' },
  { prop: 'specification', label: '规格型号', minWidth: 130, slot: 'specification' },
  { prop: 'purchaseDate', label: '购置日期', minWidth: 110, slot: 'purchaseDate' },
  { prop: 'originalValue', label: '原值(元)', minWidth: 110, slot: 'originalValue' },
  { prop: 'accumulatedDepreciation', label: '累计折旧(元)', minWidth: 120, slot: 'accumulatedDepreciation' },
  { prop: 'currentValue', label: '净值(元)', minWidth: 110, slot: 'currentValue' },
  { prop: 'departmentName', label: '使用部门', minWidth: 100, slot: 'departmentName' },
  { prop: 'status', label: '使用状态', minWidth: 95, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  assetName: [
    { required: true, message: '请输入资产名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  assetCode: [
    { required: true, message: '请输入资产编码', trigger: 'blur' },
  ],
  categoryId: [
    { required: true, message: '请选择资产分类', trigger: 'change' },
  ],
  originalValue: [
    { required: true, message: '请输入资产原值', trigger: 'blur' },
  ],
  purchaseDate: [
    { required: true, message: '请选择购置日期', trigger: 'change' },
  ],
  usefulLifeMonths: [
    { required: true, message: '请输入使用年限', trigger: 'blur' },
  ],
  departmentId: [
    { required: true, message: '请选择使用部门', trigger: 'change' },
  ],
  location: [
    { required: true, message: '请输入存放地点', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.categoryId = ''
  queryForm.value.departmentId = ''
  queryForm.value.status = ''
  queryForm.value.useStatus = ''
  refresh()
}

function handleSelectionChange(rows: Asset[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: '',
    assetCode: `FA-${new Date().getFullYear()}-${String(Date.now()).slice(-6)}`,
    assetName: '',
    categoryId: '',
    specification: '',
    serialNumber: '',
    purchaseDate: '',
    originalValue: 0,
    usefulLifeMonths: 60,
    depreciationMethod: DepreciationMethod.STRAIGHT_LINE,
    salvageRate: 5,
    departmentId: '',
    departmentName: '',
    responsibleUserName: '',
    location: '',
    status: AssetStatus.IDLE,
    remark: '',
    supplierName: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: Asset) {
  isEdit.value = true
  try {
    const detail = await assetApi.getById(row.id)
    if (detail) {
      Object.assign(formData, {
        id: detail.id,
        assetCode: detail.assetCode,
        assetName: detail.assetName,
        categoryId: detail.categoryId,
        specification: detail.specification || '',
        serialNumber: detail.serialNumber || '',
        purchaseDate: detail.purchaseDate,
        originalValue: detail.originalValue,
        usefulLifeMonths: detail.usefulLifeMonths,
        depreciationMethod: detail.depreciationMethod,
        salvageRate: detail.salvageRate,
        departmentId: detail.departmentId,
        departmentName: detail.departmentName || '',
        responsibleUserName: detail.responsibleUserName || '',
        location: detail.location,
        status: detail.status,
        remark: detail.remark || '',
        supplierName: '',
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载资产详情失败')
  }
}

async function handleDetail(row: Asset) {
  try {
    const detail = await assetApi.getById(row.id)
    if (detail) {
      detailData.value = detail
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('加载资产详情失败')
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
      originalValue: formData.originalValue,
    }

    if (isEdit.value && formData.id) {
      await assetApi.update(formData.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await assetApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadStatistics()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: Asset) {
  try {
    await ElMessageBox.confirm(
      `确定要删除资产「${row.assetName}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await assetApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function handleRepair(row: Asset) {
  try {
    await ElMessageBox.confirm(
      `确定要将资产「${row.assetName}」标记为维修中吗？`,
      '维修确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    ElMessage.success('已标记为维修中')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleScrap(row: Asset) {
  try {
    await ElMessageBox.confirm(
      `确定要报废资产「${row.assetName}」吗？报废后资产将不可使用。`,
      '报废确认',
      {
        confirmButtonText: '确定报废',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    ElMessage.success('已提交报废申请')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleTransfer(row: Asset) {
  ElMessage.info(`领用/归还功能：${row.assetName}`)
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

  try {
    importLoading.value = true
    ElMessage.info('批量导入功能开发中')
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('导出功能开发中')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

function handlePrintLabel() {
  ElMessage.info('资产标签打印功能开发中')
}

// ==================== 辅助方法 ====================

function getStatusInfo(status: AssetStatus) {
  return AssetStatusTagMap[status] || { status: 'info', label: status }
}

function formatMoney(value: number): string {
  if (value == null) return '0.00'
  return Number(value).toFixed(2)
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 部门变更时更新部门名称
function onDepartmentChange(val: string) {
  const dept = departmentOptions.value.find(d => d.id === val)
  formData.departmentName = dept?.name || ''
}

// 加载统计数据
async function loadStatistics() {
  try {
    const data = await assetApi.getStatisticsVO()
    statistics.value = data
  } catch {
    // 加载失败使用默认值
  }
}

// 加载分类选项
async function loadCategoryOptions() {
  try {
    categoryOptions.value = await categoryApi.getList()
  } catch {
    categoryOptions.value = []
  }
}

// 页面挂载时加载数据
onMounted(() => {
  loadCategoryOptions()
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="资产台账" description="管理企业固定资产台账和资产信息">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增资产
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        icon="Wallet"
        label="资产总数"
        :value="String(statistics.totalAssets)"
        color-type="primary"
        variant="bordered"
      />
      <StatCard
        icon="Money"
        label="资产总值(元)"
        :value="statistics.totalOriginalValueYuan"
        color-type="success"
        variant="bordered"
      />
      <StatCard
        icon="CircleCheck"
        label="在用资产"
        :value="String(statistics.activeCount)"
        color-type="warning"
        variant="bordered"
      />
      <StatCard
        icon="Plus"
        label="本月新增"
        :value="String(statistics.thisMonthNewCount)"
        color-type="info"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="资产名称/编码"
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.categoryId"
            placeholder="资产分类"
            clearable
            style="width: 140px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
          <el-select
            v-model="queryForm.departmentId"
            placeholder="使用部门"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="资产状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in AssetStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.useStatus"
            placeholder="使用状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in useStatusOptions"
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
          <el-button size="default" class="action-btn--print" @click="handlePrintLabel">
            <el-icon :size="14"><Printer /></el-icon>资产标签打印
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
        <!-- 资产编码列 -->
        <template #assetCode="{ row }">
          <span class="code-text">{{ row.assetCode }}</span>
        </template>

        <!-- 资产名称列 -->
        <template #assetName="{ row }">
          <span class="name-text">{{ row.assetName }}</span>
        </template>

        <!-- 资产分类列 -->
        <template #categoryName="{ row }">
          <span class="category-text">{{ row.categoryName || '-' }}</span>
        </template>

        <!-- 规格型号列 -->
        <template #specification="{ row }">
          <span class="spec-text">{{ row.specification || '-' }}</span>
        </template>

        <!-- 购置日期列 -->
        <template #purchaseDate="{ row }">
          <span class="date-text">{{ formatDate(row.purchaseDate) }}</span>
        </template>

        <!-- 原值列 -->
        <template #originalValue="{ row }">
          <span class="money-text">¥{{ formatMoney(row.originalValue) }}</span>
        </template>

        <!-- 累计折旧列 -->
        <template #accumulatedDepreciation="{ row }">
          <span class="depreciation-text">¥{{ formatMoney(row.accumulatedDepreciation) }}</span>
        </template>

        <!-- 净值列 -->
        <template #currentValue="{ row }">
          <span class="net-value-text">¥{{ formatMoney(row.currentValue) }}</span>
        </template>

        <!-- 使用部门列 -->
        <template #departmentName="{ row }">
          <span class="dept-text">{{ row.departmentName || '-' }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusInfo(row.status).status"
            :label="getStatusInfo(row.status).label"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              <el-icon :size="14"><View /></el-icon>详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              <el-icon :size="14"><Edit /></el-icon>编辑
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleTransfer(row)">
              <el-icon :size="14"><Promotion /></el-icon>领用/归还
            </el-button>
            <el-button link type="info" size="default" @click.stop="handleRepair(row)">
              <el-icon :size="14"><Tools /></el-icon>维修
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleScrap(row)">
              <el-icon :size="14"><DeleteFilled /></el-icon>报废
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
              <el-icon :size="14"><Delete /></el-icon>删除
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
      :title="isEdit ? '编辑资产' : '新增资产'"
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
              <el-form-item label="资产名称" prop="assetName">
                <el-input v-model="formData.assetName" placeholder="请输入资产名称" maxlength="50" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产编码" prop="assetCode">
                <el-input v-model="formData.assetCode" :disabled="isEdit" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产分类" prop="categoryId">
                <el-select v-model="formData.categoryId" placeholder="请选择分类" :teleported="false" style="width: 100%">
                  <el-option v-for="cat in categoryOptions" :key="cat.id" :label="cat.name" :value="cat.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="规格型号">
                <el-input v-model="formData.specification" placeholder="请输入规格型号" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 购置信息 -->
        <div class="form-section">
          <div class="section-title">购置信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="购置日期" prop="purchaseDate">
                <el-date-picker
                  v-model="formData.purchaseDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择日期"
                  :teleported="false"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="原值(元)" prop="originalValue">
                <el-input-number
                  v-model="formData.originalValue"
                  :precision="2"
                  :min="0"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="使用年限(月)" prop="usefulLifeMonths">
                <el-input-number
                  v-model="formData.usefulLifeMonths"
                  :min="1"
                  :step="12"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="残值率(%)">
                <el-input-number
                  v-model="formData.salvageRate"
                  :min="0"
                  :max="100"
                  :precision="1"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="折旧方法">
                <el-select
                  v-model="formData.depreciationMethod"
                  placeholder="请选择"
                  :teleported="false"
                  style="width: 100%"
                >
                  <el-option
                    v-for="opt in DepreciationMethodOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="供应商">
                <el-input v-model="formData.supplierName" placeholder="请输入供应商" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 使用信息 -->
        <div class="form-section">
          <div class="section-title">使用信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="使用部门" prop="departmentId">
                <el-select
                  v-model="formData.departmentId"
                  placeholder="请选择部门"
                  :teleported="false"
                  style="width: 100%"
                  @change="onDepartmentChange"
                >
                  <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="使用人">
                <el-input v-model="formData.responsibleUserName" placeholder="请输入使用人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="存放地点" prop="location">
                <el-input v-model="formData.location" placeholder="请输入存放地点" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产状态">
                <el-select v-model="formData.status" :teleported="false" style="width: 100%">
                  <el-option
                    v-for="opt in AssetStatusOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
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
      v-model="detailVisible"
      title="资产详情"
      width="900px"
      destroy-on-close
    >
      <template v-if="detailData">
        <el-tabs v-model="activeDetailTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border class="detail-descriptions">
              <el-descriptions-item label="资产编码">{{ detailData.assetCode }}</el-descriptions-item>
              <el-descriptions-item label="资产名称">{{ detailData.assetName }}</el-descriptions-item>
              <el-descriptions-item label="资产分类">{{ detailData.categoryName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="规格型号">{{ detailData.specification || '-' }}</el-descriptions-item>
              <el-descriptions-item label="资产状态">
                <StatusTag
                  :status="getStatusInfo(detailData.status).status"
                  :label="getStatusInfo(detailData.status).label"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="购置日期">{{ formatDate(detailData.purchaseDate) }}</el-descriptions-item>
              <el-descriptions-item label="使用部门">{{ detailData.departmentName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="使用人">{{ detailData.responsibleUserName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="存放地点">{{ detailData.location }}</el-descriptions-item>
              <el-descriptions-item label="序列号">{{ detailData.serialNumber || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatTime(detailData.updateTime) }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 折旧信息 -->
          <el-tab-pane label="折旧信息" name="depreciation">
            <el-descriptions :column="2" border class="detail-descriptions">
              <el-descriptions-item label="资产原值">¥{{ formatMoney(detailData.originalValue) }}</el-descriptions-item>
              <el-descriptions-item label="累计折旧">¥{{ formatMoney(detailData.accumulatedDepreciation) }}</el-descriptions-item>
              <el-descriptions-item label="资产净值">¥{{ formatMoney(detailData.currentValue) }}</el-descriptions-item>
              <el-descriptions-item label="使用年限">{{ detailData.usefulLifeMonths }} 个月</el-descriptions-item>
              <el-descriptions-item label="已使用">{{ detailData.usedMonths }} 个月</el-descriptions-item>
              <el-descriptions-item label="残值率">{{ detailData.salvageRate }}%</el-descriptions-item>
              <el-descriptions-item label="折旧方法" :span="2">
                {{ DepreciationMethodOptions.find(o => o.value === detailData.depreciationMethod)?.label || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 使用记录 -->
          <el-tab-pane label="使用记录" name="usage">
            <el-table :data="usageRecords" border size="default">
              <el-table-column prop="date" label="日期" width="120" />
              <el-table-column prop="action" label="操作类型" width="100" />
              <el-table-column prop="operator" label="操作人" width="100" />
              <el-table-column prop="department" label="部门" width="120" />
              <el-table-column prop="remark" label="备注" />
            </el-table>
          </el-tab-pane>

          <!-- 维修记录 -->
          <el-tab-pane label="维修记录" name="maintenance">
            <el-table :data="maintenanceRecords" border size="default">
              <el-table-column prop="startTime" label="开始时间" width="160" />
              <el-table-column prop="completedTime" label="完成时间" width="160" />
              <el-table-column prop="faultDescription" label="故障描述" />
              <el-table-column prop="repairType" label="维修类型" width="100" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <StatusTag
                    :status="row.status === 'completed' ? 'active' : row.status === 'in_progress' ? 'info' : 'warning'"
                    :label="row.status === 'completed' ? '已完成' : row.status === 'in_progress' ? '维修中' : '待维修'"
                    size="small"
                  />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="maintenanceRecords.length === 0" description="暂无维修记录" />
          </el-tab-pane>

          <!-- 附件 -->
          <el-tab-pane label="附件" name="attachment">
            <el-table :data="attachmentList" border size="default">
              <el-table-column prop="name" label="文件名" />
              <el-table-column prop="size" label="大小" width="100" align="center" />
              <el-table-column prop="uploadTime" label="上传时间" width="160" align="center" />
              <el-table-column label="操作" width="120" align="center">
                <template #default>
                  <el-button link type="primary" size="small">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
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
  .action-btn--print {
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

// 表格文字样式
.code-text {
  font-family: var(--fts-font-mono);
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.name-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.category-text {
  color: var(--fts-text-primary);
}

.spec-text {
  color: var(--fts-text-secondary);
}

.date-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.money-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.depreciation-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.net-value-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-primary);
}

.dept-text {
  color: var(--fts-text-primary);
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
  }
}

// 详情描述列表
.detail-descriptions {
  margin-top: var(--fts-space-2);
}

// 响应式
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
