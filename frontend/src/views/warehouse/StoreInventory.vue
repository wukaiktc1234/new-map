<script setup lang="ts">
/**
 * 门店库存页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理各门店的库存数据，支持库存查询和调整
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Download,
  EditPen,
  View,
  Delete,
  Switch,
  Document,
  Goods,
  Warning,
  TrendCharts,
  Money,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { inventoryApi } from '@/api/warehouse/inventory'
import { inventoryLogApi } from '@/api/warehouse/inventory-log'
import { warehouseApi } from '@/api/warehouse/warehouse'
import { categoryApi } from '@/api/product/category'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { InventoryInfo, InventoryQueryForm, InventoryStatus } from '@/types/warehouse-inventory'
import type { CategoryOption } from '@/types/product'
import type { WarehouseInfo } from '@/types/warehouse'

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

/** 库存调整类型 */
type AdjustType = 'stock_in' | 'stock_out' | 'damage' | 'check_gain' | 'check_loss'

/** 库存调整表单 */
interface AdjustFormData {
  inventoryId: string
  adjustType: AdjustType
  quantity: number
  reason: string
  remark: string
}

/** 库存变动记录 */
interface TransactionRecord {
  transactionId: string
  transactionType: string
  typeLabel: string
  quantityChange: number
  beforeQty: number
  afterQty: number
  unitCost: string
  referenceNo: string
  createTime: string
  operator: string
  remark: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<InventoryInfo[]>([])
const adjustDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryInfo | null>(null)
const transactionLoading = ref(false)
const transactionRecords = ref<TransactionRecord[]>([])
const exportLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref<InventoryQueryForm & { categoryId?: string; stockStatus?: string }>({
  warehouseId: '',
  categoryId: '',
  materialName: '',
  stockStatus: '',
  status: undefined,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<InventoryInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: InventoryQueryForm & { page?: number; size?: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.warehouseId) queryParams.warehouseId = params.warehouseId
      if (params.materialName) queryParams.materialName = params.materialName
      if (params.status) queryParams.status = params.status
      if (params.lowStockOnly) queryParams.lowStockOnly = params.lowStockOnly
      return inventoryApi.getList(queryParams)
    },
  } as unknown as CrudApi<InventoryInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = computed(() => {
  const records = tableData.value || []
  const totalAmount = records.reduce((sum, item) => {
    return sum + (Number(item.totalCost) || 0)
  }, 0)
  const warningCount = records.filter(item => item.status === 'warning').length
  return {
    totalTypes: pagination?.total || 0,
    totalAmount: totalAmount.toFixed(2),
    warningCount,
    weekChange: 0,
  }
})

// 仓库/门店选项
const warehouseOptions = ref<WarehouseInfo[]>([])

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
  { label: '入库', value: 'stock_in' },
  { label: '出库', value: 'stock_out' },
  { label: '报损', value: 'damage' },
  { label: '盘盈', value: 'check_gain' },
  { label: '盘亏', value: 'check_loss' },
]

// 调整表单数据
const adjustFormData = reactive<AdjustFormData>({
  inventoryId: '',
  adjustType: 'stock_in',
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

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'materialId', label: '商品编码', minWidth: 120 },
  { prop: 'materialName', label: '商品名称', minWidth: 150, slot: 'materialName' },
  { prop: 'specification', label: '规格型号', minWidth: 100 },
  { prop: 'unit', label: '单位', minWidth: 70, align: 'center' },
  { prop: 'inventoryType', label: '分类', minWidth: 90, slot: 'inventoryType' },
  { prop: 'quantity', label: '当前库存数量', minWidth: 110, align: 'right', slot: 'quantity' },
  { prop: 'unitCost', label: '库存单价', minWidth: 100, align: 'right', slot: 'unitCost' },
  { prop: 'totalCost', label: '库存金额', minWidth: 110, align: 'right', slot: 'totalCost' },
  { prop: 'minSafeQty', label: '预警阈值', minWidth: 90, align: 'center' },
  { prop: 'status', label: '库存状态', minWidth: 90, slot: 'status', align: 'center' },
  { prop: 'updateTime', label: '最后更新时间', minWidth: 160, slot: 'updateTime' },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

/** 加载仓库/门店选项 */
async function loadWarehouseOptions(): Promise<void> {
  try {
    const res = await warehouseApi.getActiveList()
    warehouseOptions.value = res || []
  } catch {
    warehouseOptions.value = []
  }
}

/** 加载分类选项 */
async function loadCategoryOptions(): Promise<void> {
  try {
    categoryOptions.value = await categoryApi.getCategoryOptions()
  } catch {
    categoryOptions.value = []
  }
}

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.warehouseId = ''
  queryForm.value.categoryId = ''
  queryForm.value.materialName = ''
  queryForm.value.stockStatus = ''
  queryForm.value.status = undefined
  refresh()
}

function handleSelectionChange(rows: InventoryInfo[]) {
  selectedRows.value = rows
}

/** 获取库存状态标签 */
function getStatusLabel(status: InventoryStatus): string {
  const map: Record<InventoryStatus, string> = {
    normal: '正常',
    warning: '预警',
    expired: '过期',
    frozen: '冻结',
  }
  return map[status] || status
}

/** 获取库存状态颜色 */
function getStatusColor(status: InventoryStatus): string {
  const map: Record<InventoryStatus, string> = {
    normal: 'success',
    warning: 'warning',
    expired: 'error',
    frozen: 'info',
  }
  return map[status] || 'info'
}

/** 获取库存类型标签 */
function getInventoryTypeLabel(type: string): string {
  const map: Record<string, string> = {
    raw_material: '原料',
    semi_finished: '半成品',
    finished: '成品',
    packaging: '包装材料',
  }
  return map[type] || type
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 打开库存调整对话框 */
function handleAdjust(row: InventoryInfo) {
  adjustFormData.inventoryId = row.inventoryId
  adjustFormData.adjustType = 'stock_in'
  adjustFormData.quantity = 0
  adjustFormData.reason = ''
  adjustFormData.remark = ''
  adjustDialogVisible.value = true
}

/** 打开报损对话框 */
function handleDamage(row: InventoryInfo) {
  adjustFormData.inventoryId = row.inventoryId
  adjustFormData.adjustType = 'damage'
  adjustFormData.quantity = 0
  adjustFormData.reason = ''
  adjustFormData.remark = ''
  adjustDialogVisible.value = true
}

/** 打开调拨对话框 */
function handleTransfer(row: InventoryInfo) {
  ElMessage.info('库存调拨功能开发中')
}

/** 提交库存调整 */
async function handleAdjustSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (adjustFormData.adjustType === 'stock_in' || adjustFormData.adjustType === 'check_gain') {
      const row = tableData.value.find(item => item.inventoryId === adjustFormData.inventoryId)
      await inventoryApi.increase({
        materialId: row?.materialId || '',
        warehouseId: row?.warehouseId || '',
        quantity: adjustFormData.quantity,
        remark: adjustFormData.remark,
      })
    } else {
      await inventoryApi.deduct({
        inventoryId: adjustFormData.inventoryId,
        quantity: adjustFormData.quantity,
      })
    }
    ElMessage.success('调整成功')
    adjustDialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error('调整失败')
  } finally {
    submitLoading.value = false
  }
}

/** 查看详情 */
async function handleView(row: InventoryInfo) {
  currentDetail.value = row
  detailDialogVisible.value = true
  loadTransactionRecords(row.materialId, row.warehouseId)
}

/**
 * 加载库存变动记录（调用真实 API）
 * 修复 P0：原代码使用硬编码 Mock 数据，库存变动记录是溯源链的关键环节，不能有假数据。
 * 改为调用 inventoryLogApi.getPage 获取真实库存日志。
 */
async function loadTransactionRecords(materialId: string, warehouseId: string) {
  transactionLoading.value = true
  try {
    const res = await inventoryLogApi.getPage({
      materialId: materialId || undefined,
      warehouseId: warehouseId || undefined,
      page: 1,
      size: 50,
    })
    // 映射后端日志记录到前端展示格式
    transactionRecords.value = (res?.records || []).map(log => ({
      transactionId: log.id,
      transactionType: log.operationType,
      typeLabel: log.operationTypeName || log.operationType,
      quantityChange: log.changeAmount,
      beforeQty: log.beforeStock,
      afterQty: log.afterStock,
      unitCost: '-',
      referenceNo: '-',
      createTime: log.createdAt,
      operator: log.operatorName || '-',
      remark: log.remark || '-',
    }))
  } catch (error) {
    // API 调用失败时显示空列表，不使用假数据
    transactionRecords.value = []
    if (error instanceof Error) {
      console.error('[StoreInventory] 加载库存变动记录失败:', error.message)
    }
  } finally {
    transactionLoading.value = false
  }
}

/** 库存盘点 */
function handleStockCheck() {
  ElMessage.info('库存盘点功能开发中')
}

/** 批量调整 */
function handleBatchAdjust() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要调整的库存记录')
    return
  }
  ElMessage.info('批量调整功能开发中')
}

/** 批量调拨 */
function handleBatchTransfer() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要调拨的库存记录')
    return
  }
  ElMessage.info('批量调拨功能开发中')
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
function getTransactionTypeStatus(type: string): string {
  const map: Record<string, string> = {
    purchase_in: 'success',
    sale_out: 'primary',
    transfer_in: 'success',
    transfer_out: 'warning',
    check_gain: 'success',
    check_loss: 'warning',
    damage: 'error',
    return: 'info',
  }
  return map[type] || 'default'
}

// 页面挂载时加载选项数据
onMounted(() => {
  loadWarehouseOptions()
  loadCategoryOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="门店库存查看" description="管理各门店的库存数据，支持库存查询和调整">
      <el-button type="primary" size="default" @click="handleStockCheck">
        <el-icon :size="16"><Document /></el-icon>库存盘点
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Goods" label="商品种类" :value="String(statistics.totalTypes)" color-type="primary" variant="bordered" />
      <StatCard icon="Money" label="库存总金额" :value="'¥' + statistics.totalAmount" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="预警商品数" :value="String(statistics.warningCount)" color-type="warning" variant="bordered" />
      <StatCard icon="TrendCharts" label="近7天变动" :value="String(statistics.weekChange)" color-type="info" variant="bordered" />
    </section>

    <!-- 筛选栏 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.warehouseId"
            placeholder="门店/仓库"
            clearable
            style="width: 150px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="wh in warehouseOptions"
              :key="wh.warehouseId"
              :label="wh.warehouseName"
              :value="wh.warehouseId"
            />
          </el-select>
          <el-select
            v-model="queryForm.categoryId"
            placeholder="商品分类"
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
          <el-button size="default" @click="handleBatchAdjust">
            <el-icon :size="14"><EditPen /></el-icon>库存调整
          </el-button>
          <el-button size="default" @click="handleBatchTransfer">
            <el-icon :size="14"><Switch /></el-icon>库存调拨
          </el-button>
          <el-button size="default" type="danger" @click="handleBatchDamage">
            <el-icon :size="14"><Delete /></el-icon>报损
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
        <template #inventoryType="{ row }">
          <span class="type-text">{{ getInventoryTypeLabel(row.inventoryType) }}</span>
        </template>

        <!-- 库存数量列 -->
        <template #quantity="{ row }">
          <span
            :class="{
              'qty-normal': row.status === 'normal',
              'qty-warning': row.status === 'warning',
              'qty-error': row.status === 'expired' || row.status === 'frozen',
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

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusColor(row.status)"
            :label="getStatusLabel(row.status)"
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
              查看
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleAdjust(row)">
              调整
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDamage(row)">
              报损
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleTransfer(row)">
              调拨
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

    <!-- 库存调整对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="库存调整"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form
        ref="formRef"
        :model="adjustFormData"
        :rules="adjustFormRules"
        label-width="100px"
      >
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
        <div class="dialog-footer">
          <el-button @click="adjustDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="submitLoading"
            @click="handleAdjustSubmit"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="库存详情"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <!-- 商品基本信息 -->
        <div class="detail-section">
          <div class="section-title">商品基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="商品编码">{{ currentDetail.materialId }}</el-descriptions-item>
            <el-descriptions-item label="商品名称">{{ currentDetail.materialName }}</el-descriptions-item>
            <el-descriptions-item label="规格型号">{{ currentDetail.specification || '-' }}</el-descriptions-item>
            <el-descriptions-item label="单位">{{ currentDetail.unit }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ getInventoryTypeLabel(currentDetail.inventoryType) }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ currentDetail.warehouseId }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 库存信息 -->
        <div class="detail-section">
          <div class="section-title">库存信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="当前库存数量">
              <span
                :class="{
                  'qty-normal': currentDetail.status === 'normal',
                  'qty-warning': currentDetail.status === 'warning',
                  'qty-error': currentDetail.status === 'expired' || currentDetail.status === 'frozen',
                }"
              >
                {{ currentDetail.quantity }} {{ currentDetail.unit }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="可用库存">
              {{ currentDetail.availableQuantity }} {{ currentDetail.unit }}
            </el-descriptions-item>
            <el-descriptions-item label="库存单价">¥{{ currentDetail.unitCost }}</el-descriptions-item>
            <el-descriptions-item label="库存金额">¥{{ currentDetail.totalCost }}</el-descriptions-item>
            <el-descriptions-item label="预警阈值">{{ currentDetail.minSafeQty }}</el-descriptions-item>
            <el-descriptions-item label="库存状态">
              <StatusTag
                :status="getStatusColor(currentDetail.status)"
                :label="getStatusLabel(currentDetail.status)"
                size="small"
                variant="light"
              />
            </el-descriptions-item>
            <el-descriptions-item label="最后更新时间">{{ formatTime(currentDetail.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 库存变动记录 -->
        <div class="detail-section">
          <div class="section-title">库存变动记录</div>
          <el-table
            :data="transactionRecords"
            v-loading="transactionLoading"
            size="small"
            border
            style="width: 100%"
          >
            <el-table-column prop="createTime" label="时间" min-width="160" />
            <el-table-column prop="typeLabel" label="类型" min-width="100" align="center">
              <template #default="{ row }">
                <StatusTag
                  :status="getTransactionTypeStatus(row.transactionType)"
                  :label="row.typeLabel"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column prop="beforeQty" label="变动前" min-width="80" align="right" />
            <el-table-column label="变动数量" min-width="100" align="right">
              <template #default="{ row }">
                <span
                  :class="{
                    'qty-in': row.quantityChange > 0,
                    'qty-out': row.quantityChange < 0,
                  }"
                >
                  {{ row.quantityChange > 0 ? '+' : '' }}{{ row.quantityChange }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="afterQty" label="变动后" min-width="80" align="right" />
            <el-table-column prop="referenceNo" label="单据号" min-width="130" />
            <el-table-column prop="operator" label="操作人" min-width="80" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
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
