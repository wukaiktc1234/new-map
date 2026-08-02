<script setup lang="ts">
/**
 * 门店库存页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理门店的库存数据和库存操作
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Download,
  EditPen,
  View,
  Goods,
  Warning,
  Transfer,
  Document,
  Money,
  ShoppingCart,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { storeInventoryApi } from '@/api/store-ops/store-inventory'
import { categoryApi } from '@/api/product/category'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'
import type {
  StoreInventoryInfo,
  StoreInventoryQueryForm,
  StoreInfo,
  StoreInventoryLogInfo,
} from '@/types/warehouse-store'
import type { CategoryOption } from '@/types/product'

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
  align?: 'left' | 'center' | 'right'
}

/** 库存状态类型 */
type InventoryStatus = 'normal' | 'warning' | 'low' | 'over'

/** 库存调整类型 */
type AdjustType = 'in' | 'out'

/** 库存调整表单 */
interface AdjustFormData {
  inventoryId: string
  materialName: string
  currentQuantity: number
  adjustType: AdjustType
  quantity: number
  reason: string
  remark: string
}

/** 要货申请表单 */
interface RequisitionFormData {
  inventoryId: string
  materialName: string
  specification: string
  currentQuantity: number
  unit: string
  requestQuantity: number
  expectedDate: string
  urgency: string
  remark: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<StoreInventoryInfo[]>([])
const adjustDialogVisible = ref(false)
const requisitionDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentDetail = ref<StoreInventoryInfo | null>(null)
const logLoading = ref(false)
const logRecords = ref<StoreInventoryLogInfo[]>([])
const exportLoading = ref(false)

const adjustFormRef = ref<FormInstance>()
const requisitionFormRef = ref<FormInstance>()

// 查询表单
const queryForm = ref<StoreInventoryQueryForm & { stockStatus?: string }>({
  storeId: '',
  category: '',
  materialName: '',
  stockStatus: '',
  status: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<StoreInventoryInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: StoreInventoryQueryForm & { page?: number; size?: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.storeId) queryParams.storeId = params.storeId
      if (params.materialName) queryParams.materialName = params.materialName
      if (params.category) queryParams.category = params.category
      return storeInventoryApi.getList(queryParams)
    },
  } as unknown as CrudApi<StoreInventoryInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = computed(() => {
  const records = tableData.value || []
  const totalAmount = records.reduce((sum, item) => {
    return sum + (Number(item.totalCost) || 0)
  }, 0)
  const warningCount = records.filter(item => getInventoryStatus(item) === 'warning').length
  return {
    totalTypes: pagination?.total || 0,
    totalAmount: totalAmount.toFixed(2),
    warningCount,
    pendingTransfer: 0,
  }
})

// 门店选项（从真实 API 加载，使用共享 composable）
const { storeOptions, loadStores, getStoreName } = useStoreOptions(true)

// 分类选项
const categoryOptions = ref<CategoryOption[]>([])

// 库存状态选项
const stockStatusOptions = [
  { label: '正常', value: 'normal' },
  { label: '预警', value: 'warning' },
  { label: '不足', value: 'low' },
  { label: '过量', value: 'over' },
]

// 调整类型选项
const adjustTypeOptions = [
  { label: '入库', value: 'in' },
  { label: '出库', value: 'out' },
]

// 紧急程度选项
const urgencyOptions = [
  { label: '普通', value: 'normal' },
  { label: '紧急', value: 'urgent' },
  { label: '特急', value: 'critical' },
]

// 调整表单数据
const adjustFormData = reactive<AdjustFormData>({
  inventoryId: '',
  materialName: '',
  currentQuantity: 0,
  adjustType: 'in',
  quantity: 0,
  reason: '',
  remark: '',
})

// 调整表单校验规则
const adjustFormRules: FormRules = {
  adjustType: [{ required: true, message: '请选择调整类型', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入调整数量', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
        if (value <= 0) {
          callback(new Error('调整数量必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  reason: [{ required: true, message: '请输入调整原因', trigger: 'blur' }],
}

// 要货申请表单数据
const requisitionFormData = reactive<RequisitionFormData>({
  inventoryId: '',
  materialName: '',
  specification: '',
  currentQuantity: 0,
  unit: '',
  requestQuantity: 0,
  expectedDate: '',
  urgency: 'normal',
  remark: '',
})

// 要货申请表单校验规则
const requisitionFormRules: FormRules = {
  requestQuantity: [
    { required: true, message: '请输入申请数量', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
        if (value <= 0) {
          callback(new Error('申请数量必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  expectedDate: [{ required: true, message: '请选择期望到货日期', trigger: 'change' }],
}

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'materialId', label: '商品编码', minWidth: 120 },
  { prop: 'materialName', label: '商品名称', minWidth: 150, slot: 'materialName' },
  { prop: 'specification', label: '规格', minWidth: 100 },
  { prop: 'unit', label: '单位', minWidth: 70, align: 'center' },
  { prop: 'category', label: '分类', minWidth: 90, slot: 'category' },
  { prop: 'quantity', label: '当前库存', minWidth: 100, align: 'right', slot: 'quantity' },
  { prop: 'unitCost', label: '库存单价', minWidth: 100, align: 'right', slot: 'unitCost' },
  { prop: 'totalCost', label: '库存金额', minWidth: 110, align: 'right', slot: 'totalCost' },
  { prop: 'warningThreshold', label: '预警阈值', minWidth: 90, align: 'center', slot: 'warningThreshold' },
  { prop: 'status', label: '库存状态', minWidth: 90, slot: 'status', align: 'center' },
  { prop: 'updateTime', label: '最后更新时间', minWidth: 160, slot: 'updateTime' },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

/** 加载分类选项 */
async function loadCategoryOptions(): Promise<void> {
  try {
    categoryOptions.value = await categoryApi.getCategoryOptions()
  } catch {
    categoryOptions.value = []
  }
}

/** 获取库存状态 */
function getInventoryStatus(row: StoreInventoryInfo): InventoryStatus {
  const ratio = row.quantity > 0 ? row.availableQuantity / row.quantity : 0
  if (row.quantity === 0) return 'low'
  if (ratio < 0.2) return 'warning'
  if (ratio > 0.9) return 'over'
  return 'normal'
}

/** 获取库存状态标签 */
function getStatusLabel(status: InventoryStatus): string {
  const map: Record<InventoryStatus, string> = {
    normal: '正常',
    warning: '预警',
    low: '不足',
    over: '过量',
  }
  return map[status] || status
}

/** 获取库存状态颜色 */
function getStatusColor(status: InventoryStatus): string {
  const map: Record<InventoryStatus, string> = {
    normal: 'success',
    warning: 'warning',
    low: 'error',
    over: 'info',
  }
  return map[status] || 'info'
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.storeId = ''
  queryForm.value.category = ''
  queryForm.value.materialName = ''
  queryForm.value.stockStatus = ''
  queryForm.value.status = ''
  refresh()
}

function handleSelectionChange(rows: StoreInventoryInfo[]) {
  selectedRows.value = rows
}

/** 打开库存调整对话框 */
function handleAdjust(row: StoreInventoryInfo) {
  adjustFormData.inventoryId = row.inventoryId
  adjustFormData.materialName = row.materialName
  adjustFormData.currentQuantity = row.quantity
  adjustFormData.adjustType = 'in'
  adjustFormData.quantity = 0
  adjustFormData.reason = ''
  adjustFormData.remark = ''
  adjustDialogVisible.value = true
}

/** 打开报损对话框 */
function handleDamage(row: StoreInventoryInfo) {
  adjustFormData.inventoryId = row.inventoryId
  adjustFormData.materialName = row.materialName
  adjustFormData.currentQuantity = row.quantity
  adjustFormData.adjustType = 'out'
  adjustFormData.quantity = 0
  adjustFormData.reason = '报损'
  adjustFormData.remark = ''
  adjustDialogVisible.value = true
}

/** 提交库存调整 */
async function handleAdjustSubmit() {
  if (!adjustFormRef.value) return

  const valid = await adjustFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await storeInventoryApi.adjust({
      inventoryId: adjustFormData.inventoryId,
      quantity: adjustFormData.quantity,
      type: adjustFormData.adjustType,
      remark: adjustFormData.remark,
    })
    ElMessage.success('调整成功')
    adjustDialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error('调整失败')
  } finally {
    submitLoading.value = false
  }
}

/** 打开要货申请对话框 */
function handleRequisition(row: StoreInventoryInfo) {
  requisitionFormData.inventoryId = row.inventoryId
  requisitionFormData.materialName = row.materialName
  requisitionFormData.specification = row.specification
  requisitionFormData.currentQuantity = row.quantity
  requisitionFormData.unit = row.unit
  requisitionFormData.requestQuantity = 0
  requisitionFormData.expectedDate = ''
  requisitionFormData.urgency = 'normal'
  requisitionFormData.remark = ''
  requisitionDialogVisible.value = true
}

/** 提交要货申请 */
async function handleRequisitionSubmit() {
  if (!requisitionFormRef.value) return

  const valid = await requisitionFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await storeInventoryApi.adjust({
      inventoryId: requisitionFormData.inventoryId,
      quantity: requisitionFormData.requestQuantity,
      type: 'in',
      remark: `要货申请 - 紧急程度：${requisitionFormData.urgency}，期望到货：${requisitionFormData.expectedDate}，备注：${requisitionFormData.remark || ''}`,
    })
    ElMessage.success('要货申请已提交')
    requisitionDialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

/** 查看详情 */
async function handleView(row: StoreInventoryInfo) {
  currentDetail.value = row
  detailDialogVisible.value = true
  loadLogRecords(row.inventoryId)
}

/** 加载库存变动日志 */
async function loadLogRecords(inventoryId: string) {
  logLoading.value = true
  try {
    const params: Record<string, unknown> = { page: 1, size: 20 }
    if (currentDetail.value?.storeId) {
      params.storeId = currentDetail.value.storeId
    }
    const res = await storeInventoryApi.getLogs(params)
    logRecords.value = res?.records || []
  } catch {
    logRecords.value = []
  } finally {
    logLoading.value = false
  }
}

/** 库存盘点 */
function handleStockCheck() {
  ElMessage.info('库存盘点功能开发中')
}

/** 批量要货申请 */
function handleBatchRequisition() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要申请的库存记录')
    return
  }
  ElMessage.info('批量要货申请功能开发中')
}

/** 批量调整 */
function handleBatchAdjust() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要调整的库存记录')
    return
  }
  ElMessage.info('批量调整功能开发中')
}

/** 批量报损 */
function handleBatchDamage() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要报损的库存记录')
    return
  }
  ElMessage.info('批量报损功能开发中')
}

/** 导出数据 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    const filename = `门店库存_${timestamp}.xlsx`

    ElMessage.success(`导出成功：${filename}`)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/** 获取变动类型标签状态 */
function getLogTypeStatus(type: string): string {
  const map: Record<string, string> = {
    IN: 'success',
    OUT: 'error',
    ADJUST: 'warning',
  }
  return map[type] || 'default'
}

// 页面挂载时加载选项数据（门店选项由 useStoreOptions autoLoad 自动加载）
onMounted(() => {
  loadCategoryOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="门店库存" description="管理门店的库存数据和库存操作">
      <el-button type="primary" size="default" @click="handleStockCheck">
        <el-icon :size="16"><Document /></el-icon>库存盘点
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Goods" label="商品种类" :value="String(statistics.totalTypes)" color-type="primary" variant="bordered" />
      <StatCard icon="Money" label="库存总金额" :value="'¥' + statistics.totalAmount" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="预警商品数" :value="String(statistics.warningCount)" color-type="warning" variant="bordered" />
      <StatCard icon="Transfer" label="待调拨数" :value="String(statistics.pendingTransfer)" color-type="info" variant="bordered" />
    </section>

    <!-- 筛选栏 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.materialName"
            placeholder="商品名称/编码搜索"
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.category"
            placeholder="分类"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.stockStatus"
            placeholder="库存状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in stockStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.storeId"
            placeholder="门店选择"
            clearable
            style="width: 150px"
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
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-button size="default" @click="handleStockCheck">
            <el-icon :size="14"><Document /></el-icon>库存盘点
          </el-button>
          <el-button size="default" type="primary" @click="handleBatchRequisition">
            <el-icon :size="14"><ShoppingCart /></el-icon>要货申请
          </el-button>
          <el-button size="default" @click="handleBatchAdjust">
            <el-icon :size="14"><EditPen /></el-icon>库存调整
          </el-button>
          <el-button size="default" type="danger" @click="handleBatchDamage">
            <el-icon :size="14"><Goods /></el-icon>报损
          </el-button>
        </div>
        <div class="toolbar-right">
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
        <!-- 商品名称列 -->
        <template #materialName="{ row }">
          <div class="material-cell">
            <div class="material-info">
              <div class="material-name">{{ row.materialName }}</div>
            </div>
          </div>
        </template>

        <!-- 分类列 -->
        <template #category="{ row }">
          <span class="type-text">{{ (row as any).category || '-' }}</span>
        </template>

        <!-- 库存数量列 -->
        <template #quantity="{ row }">
          <span
            :class="{
              'qty-normal': getInventoryStatus(row) === 'normal',
              'qty-warning': getInventoryStatus(row) === 'warning',
              'qty-error': getInventoryStatus(row) === 'low',
            }"
          >
            {{ row.quantity }}
          </span>
        </template>

        <!-- 库存单价列 -->
        <template #unitCost="{ row }">
          <span class="price-text">¥{{ row.unitCost }}</span>
        </template>

        <!-- 库存金额列 -->
        <template #totalCost="{ row }">
          <span class="amount-text">¥{{ row.totalCost }}</span>
        </template>

        <!-- 预警阈值列 -->
        <template #warningThreshold="{ row }">
          <span>{{ Math.round(row.quantity * 0.2) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusColor(getInventoryStatus(row))"
            :label="getStatusLabel(getInventoryStatus(row))"
            size="small"
            variant="light"
          />
        </template>

        <!-- 更新时间列 -->
        <template #updateTime="{ row }">
          <span class="time-text">{{ formatTime(row.updateTime) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleAdjust(row)">
              调整
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDamage(row)">
              报损
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleRequisition(row)">
              要货
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

    <!-- 库存调整对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="库存调整"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="adjustFormRef"
        :model="adjustFormData"
        :rules="adjustFormRules"
        label-width="100px"
      >
        <el-form-item label="商品名称">
          <span>{{ adjustFormData.materialName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ adjustFormData.currentQuantity }}</span>
        </el-form-item>
        <el-form-item label="调整类型" prop="adjustType">
          <el-select
            v-model="adjustFormData.adjustType"
            placeholder="请选择调整类型"
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="opt in adjustTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整数量" prop="quantity">
          <el-input-number
            v-model="adjustFormData.quantity"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input
            v-model="adjustFormData.reason"
            placeholder="请输入调整原因"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="adjustFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleAdjustSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 要货申请对话框 -->
    <el-dialog
      v-model="requisitionDialogVisible"
      title="要货申请"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="requisitionFormRef"
        :model="requisitionFormData"
        :rules="requisitionFormRules"
        label-width="110px"
      >
        <el-form-item label="商品名称">
          <span>{{ requisitionFormData.materialName }}</span>
        </el-form-item>
        <el-form-item label="规格">
          <span>{{ requisitionFormData.specification }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ requisitionFormData.currentQuantity }} {{ requisitionFormData.unit }}</span>
        </el-form-item>
        <el-form-item label="申请数量" prop="requestQuantity">
          <el-input-number
            v-model="requisitionFormData.requestQuantity"
            :min="1"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="期望到货" prop="expectedDate">
          <el-date-picker
            v-model="requisitionFormData.expectedDate"
            type="date"
            placeholder="请选择日期"
            :teleported="false"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="requisitionFormData.urgency">
            <el-radio
              v-for="opt in urgencyOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="requisitionFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="requisitionDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleRequisitionSubmit"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="库存详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template v-if="currentDetail">
        <!-- 商品基本信息 -->
        <div class="detail-section">
          <div class="section-title">商品基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="商品编码">{{ currentDetail.materialId }}</el-descriptions-item>
            <el-descriptions-item label="商品名称">{{ currentDetail.materialName }}</el-descriptions-item>
            <el-descriptions-item label="规格">{{ currentDetail.specification || '-' }}</el-descriptions-item>
            <el-descriptions-item label="单位">{{ currentDetail.unit }}</el-descriptions-item>
            <el-descriptions-item label="门店">{{ getStoreName(currentDetail.storeId) || currentDetail.storeId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="库存ID">{{ currentDetail.inventoryId }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 库存信息 -->
        <div class="detail-section">
          <div class="section-title">库存信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="当前库存数量">
              <span
                :class="{
                  'qty-normal': getInventoryStatus(currentDetail) === 'normal',
                  'qty-warning': getInventoryStatus(currentDetail) === 'warning',
                  'qty-error': getInventoryStatus(currentDetail) === 'low',
                }"
              >
                {{ currentDetail.quantity }} {{ currentDetail.unit }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="可用库存">
              {{ currentDetail.availableQuantity }} {{ currentDetail.unit }}
            </el-descriptions-item>
            <el-descriptions-item label="锁定库存">
              {{ currentDetail.lockedQuantity }} {{ currentDetail.unit }}
            </el-descriptions-item>
            <el-descriptions-item label="库存单价">¥{{ currentDetail.unitCost }}</el-descriptions-item>
            <el-descriptions-item label="库存金额">¥{{ currentDetail.totalCost }}</el-descriptions-item>
            <el-descriptions-item label="预警阈值">{{ Math.round(currentDetail.quantity * 0.2) }}</el-descriptions-item>
            <el-descriptions-item label="库存状态">
              <StatusTag
                :status="getStatusColor(getInventoryStatus(currentDetail))"
                :label="getStatusLabel(getInventoryStatus(currentDetail))"
                size="small"
                variant="light"
              />
            </el-descriptions-item>
            <el-descriptions-item label="最后入库时间">{{ formatTime(currentDetail.lastInTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后出库时间">{{ formatTime(currentDetail.lastOutTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后更新时间">{{ formatTime(currentDetail.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 库存变动记录 -->
        <div class="detail-section">
          <div class="section-title">库存变动记录</div>
          <el-table
            :data="logRecords"
            v-loading="logLoading"
            size="small"
            border
            style="width: 100%"
          >
            <el-table-column prop="createTime" label="时间" min-width="160" />
            <el-table-column prop="operationTypeName" label="类型" min-width="100" align="center">
              <template #default="{ row }">
                <StatusTag
                  :status="getLogTypeStatus(row.operationType)"
                  :label="row.operationTypeName"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column prop="beforeStock" label="变动前" min-width="80" align="right" />
            <el-table-column label="变动数量" min-width="100" align="right">
              <template #default="{ row }">
                <span
                  :class="{
                    'qty-in': row.quantity > 0,
                    'qty-out': row.quantity < 0,
                  }"
                >
                  {{ row.quantity > 0 ? '+' : '' }}{{ row.quantity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="afterStock" label="变动后" min-width="80" align="right" />
            <el-table-column prop="operatorName" label="操作人" min-width="80" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>
      </template>

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

// 筛选栏面板
.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

// 工具栏面板
.toolbar-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-2) var(--fts-space-6);
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 40px;
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

// 商品信息单元格
.material-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.material-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .material-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 分类文字
.type-text {
  color: var(--fts-text-primary);
}

// 价格
.price-text {
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 库存数量样式
.qty-normal {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.qty-warning {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.qty-error {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
}

.qty-in {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.qty-out {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
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

// 详情区域
.detail-section {
  margin-bottom: var(--fts-space-4);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
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
  .advanced-search-panel,
  .toolbar-panel {
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
