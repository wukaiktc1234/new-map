<script setup lang="ts">
/**
 * 库存出库管理页面
 * 功能：出库单列表、新建出库单、审批出库单、执行出库、查看详情
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Camera, User, Clock, Check, Money } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import DetailHeader from '@/components/core/DetailHeader.vue'
import DetailSection from '@/components/core/DetailSection.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryOutboundApi, inventoryApi, warehouseApi, inventoryOutboundConverter } from '@/api/warehouse'
import { storeInventoryApi } from '@/api/store-ops/store-inventory'
import type { InventoryOutboundInfo, OutboundStatus, OutboundType, OutboundQueryForm, OutboundCreateForm } from '@/types/warehouse-outbound'
import type { WarehouseInfo } from '@/types/warehouse'
import type { InventoryInfo } from '@/types/warehouse-inventory'
import type { StatColorType } from '@/types/warehouse-stats'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 搜索表单 ===== */
const searchForm = ref<OutboundQueryForm & { keyword: string; dateRange: [string, string] | null }>({
  outboundType: undefined,
  status: undefined,
  warehouseId: undefined,
  startDate: '',
  endDate: '',
  keyword: '',
  dateRange: null,
})

/* ===== 仓库选项 ===== */
const warehouseOptions = ref<WarehouseInfo[]>([])

async function loadWarehouseOptions() {
  try {
    const res = await warehouseApi.getActiveList()
    warehouseOptions.value = res || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载仓库列表失败')
  }
}

/* ===== 目标选项（门店/供应商） ===== */
const targetStoreOptions = ref<{ storeId: string; storeName: string }[]>([])
const targetSupplierOptions = ref<{ supplierId: string; supplierName: string }[]>([
  { supplierId: 'SUP001', supplierName: '双汇冷鲜肉直供' },
  { supplierId: 'SUP002', supplierName: '益海嘉里粮油' },
  { supplierId: 'SUP003', supplierName: '蒙牛乳业' },
])

async function loadTargetStoreOptions() {
  try {
    const res = await storeInventoryApi.getActiveStores()
    targetStoreOptions.value = (res || []).map(s => ({ storeId: s.storeId, storeName: s.storeName }))
  } catch {
    // 静默处理，使用空列表
  }
}

/** 目标选择变更处理 */
function handleTargetChange(val: string) {
  const storeOpt = targetStoreOptions.value.find(s => s.storeId === val)
  const supOpt = targetSupplierOptions.value.find(s => s.supplierId === val)
  if (storeOpt) {
    createForm.value.targetId = storeOpt.storeId
    createForm.value.targetName = storeOpt.storeName
  } else if (supOpt) {
    createForm.value.targetId = supOpt.supplierId
    createForm.value.targetName = supOpt.supplierName
  } else {
    // allow-create 手动输入时，targetId 和 targetName 都设为输入值
    createForm.value.targetId = val
    createForm.value.targetName = val
  }
}

/* ===== 统计卡片 ===== */
const tableData = ref<InventoryOutboundInfo[]>([])

const statistics = computed(() => {
  const all = tableData.value
  const pending = all.filter(r => r.status === 'pending').length
  const approved = all.filter(r => r.status === 'approved').length
  const completed = all.filter(r => r.status === 'completed').length
  const total = all.length
  return [
    { icon: 'Warning', label: '待审批', value: pending, colorType: 'warning' as StatColorType },
    { icon: 'Check', label: '已审批', value: approved, colorType: 'primary' as StatColorType },
    { icon: 'SuccessFilled', label: '已出库', value: completed, colorType: 'success' as StatColorType },
    { icon: 'DataLine', label: '本月出库', value: total, colorType: 'info' as StatColorType },
  ]
})

/* ===== 表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'outboundCode', label: '出库单号', minWidth: 145 },
  { prop: 'outboundType', label: '出库类型', minWidth: 100, slot: 'outboundType' },
  { prop: 'warehouseName', label: '仓库', minWidth: 120 },
  { prop: 'targetName', label: '目标', minWidth: 120 },
  { prop: 'outboundDate', label: '出库日期', minWidth: 120 },
  { prop: 'totalQuantity', label: '总数量', minWidth: 85, align: 'center' },
  { prop: 'totalAmount', label: '总金额', minWidth: 100, align: 'right' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const params: OutboundQueryForm = {
      outboundType: searchForm.value.outboundType || undefined,
      status: searchForm.value.status || undefined,
      warehouseId: searchForm.value.warehouseId || undefined,
      startDate: searchForm.value.startDate || undefined,
      endDate: searchForm.value.endDate || undefined,
      keyword: searchForm.value.keyword || undefined,
      page: pagination.current,
      size: pagination.size,
    }
    const res = await inventoryOutboundApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载出库列表失败')
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */
function resetSearchForm() {
  searchForm.value = { outboundType: undefined, status: undefined, warehouseId: undefined, startDate: '', endDate: '', keyword: '', dateRange: null }
}

/* ===== 日期范围变更 ===== */
function handleDateChange(val: [string, string] | null) {
  searchForm.value.startDate = val?.[0] || ''
  searchForm.value.endDate = val?.[1] || ''
}

/* ===== 出库类型选项 ===== */
const outboundTypeOptions: { label: string; value: OutboundType }[] = [
  { label: '领料出库', value: 'requisition' },
  { label: '销售出库', value: 'sale' },
  { label: '退货出库', value: 'return' },
  { label: '其他出库', value: 'other' },
]

/* ===== 出库状态选项 ===== */
const outboundStatusOptions: { label: string; value: OutboundStatus }[] = [
  { label: '待审批', value: 'pending' },
  { label: '已审批', value: 'approved' },
  { label: '已出库', value: 'completed' },
  { label: '已驳回', value: 'rejected' },
]

/* ===== 新建出库单 ===== */
const createDialogVisible = ref(false)
const createForm = ref<OutboundCreateForm>({
  outboundType: 'requisition',
  warehouseId: '',
  targetId: '',
  targetName: '',
  referenceNo: '',
  outboundDate: '',
  items: [],
  remark: '',
})
const createFormRef = ref()
const createFormRules = {
  outboundType: [{ required: true, message: '请选择出库类型', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  targetId: [{ required: true, message: '请选择或输入目标', trigger: 'change' }],
  outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }],
}

/* ===== 仓库库存自动加载 ===== */
const warehouseInventory = ref<InventoryInfo[]>([])
const selectedInventoryIds = ref<string[]>([])
const inventoryLoading = ref(false)

async function handleWarehouseChange(warehouseId: string) {
  if (!warehouseId) {
    warehouseInventory.value = []
    selectedInventoryIds.value = []
    createForm.value.items = []
    return
  }
  inventoryLoading.value = true
  try {
    const res = await inventoryApi.getList({ warehouseId, page: 1, size: 200 })
    warehouseInventory.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载库存数据失败')
    warehouseInventory.value = []
  } finally {
    inventoryLoading.value = false
  }
  selectedInventoryIds.value = []
  createForm.value.items = []
}

/** 选中库存项变更时，同步到出库明细 */
function handleInventorySelectionChange() {
  const selectedItems = warehouseInventory.value
    .filter(inv => selectedInventoryIds.value.includes(inv.inventoryId))
    .map(inv => ({
      materialId: inv.materialId,
      requestQuantity: 1,
      remark: '',
    }))
  createForm.value.items = selectedItems
}

function handleOpenCreate() {
  createForm.value = { outboundType: 'requisition', warehouseId: '', targetId: '', targetName: '', referenceNo: '', outboundDate: '', items: [], remark: '' }
  warehouseInventory.value = []
  selectedInventoryIds.value = []
  createDialogVisible.value = true
}

/** 快捷扫码出库：打开新建对话框后自动打开扫码 */
function handleQuickScan() {
  handleOpenCreate()
  nextTick(() => {
    handleScanBarcode()
  })
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
    // 校验出库数量不超过可用库存
    for (const item of createForm.value.items) {
      if (item.requestQuantity <= 0) {
        ElMessage.warning('出库数量必须大于0')
        return
      }
      const inv = warehouseInventory.value.find(i => i.materialId === item.materialId)
      if (inv && item.requestQuantity > inv.availableQuantity) {
        ElMessage.warning(`${inv.materialName} 出库数量(${item.requestQuantity})超过可用库存(${inv.availableQuantity})`)
        return
      }
    }
    await inventoryOutboundApi.create(createForm.value)
    ElMessage.success('出库单创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '创建失败')
  }
}

/* ===== 审批出库单 ===== */
async function handleApprove(row: InventoryOutboundInfo) {
  try {
    await ElMessageBox.confirm('确认审批通过该出库单？', '审批确认', {
      confirmButtonText: '通过',
      cancelButtonText: '驳回',
      distinguishCancelAndClose: true,
      type: 'warning',
    })
    await inventoryOutboundApi.approve(row.outboundId, { approved: true })
    ElMessage.success('审批通过')
    loadData()
  } catch (action: unknown) {
    if (action === 'cancel') {
      try {
        await inventoryOutboundApi.approve(row.outboundId, { approved: false, opinion: '驳回' })
        ElMessage.success('已驳回')
        loadData()
      } catch (error: unknown) {
        if (error instanceof Error) ElMessage.error(error.message || '操作失败')
      }
    }
  }
}

/* ===== 执行出库 ===== */
async function handleExecute(row: InventoryOutboundInfo) {
  try {
    await ElMessageBox.confirm('确认执行出库操作？出库后库存将扣减。', '出库确认', {
      confirmButtonText: '确认出库',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await inventoryOutboundApi.execute(row.outboundId)
    ElMessage.success('出库成功')
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '出库失败')
    }
  }
}

/* ===== 查看详情 ===== */
const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryOutboundInfo | null>(null)
const detailSectionCollapsed = ref<Record<string, boolean>>({
  items: false,
  approvals: true,
})

const detailStatusTagStatus = computed(() =>
  currentDetail.value ? inventoryOutboundConverter.toStatusTagStatus(currentDetail.value.status) : 'pending'
)
const detailStatusLabel = computed(() =>
  currentDetail.value ? inventoryOutboundConverter.toStatusLabel(currentDetail.value.status) : ''
)
const detailMetaItems = computed(() => {
  if (!currentDetail.value) return []
  const d = currentDetail.value
  return [
    { label: '申请人', value: d.applyUserName || '-', icon: User },
    { label: '申请时间', value: d.applyTime || '-', icon: Clock },
    { label: '审批人', value: d.approveUserName || '待审核', icon: Check },
    { label: '审批时间', value: d.approveTime || '-', icon: Clock },
  ]
})

async function handleViewDetail(row: InventoryOutboundInfo) {
  try {
    const res = await inventoryOutboundApi.getById(row.outboundId)
    currentDetail.value = res
    detailDialogVisible.value = true
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载详情失败')
  }
}
async function handleDetailApprove() {
  if (!currentDetail.value) return
  try {
    await ElMessageBox.confirm('确认审批通过该出库单？', '审批确认', {
      confirmButtonText: '通过',
      cancelButtonText: '驳回',
      distinguishCancelAndClose: true,
      type: 'warning',
    })
    await inventoryOutboundApi.approve(currentDetail.value.outboundId, { approved: true })
    ElMessage.success('审批通过')
    detailDialogVisible.value = false
    loadData()
  } catch (action: unknown) {
    if (action === 'cancel' && currentDetail.value) {
      try {
        await inventoryOutboundApi.approve(currentDetail.value.outboundId, { approved: false, opinion: '驳回' })
        ElMessage.success('已驳回')
        detailDialogVisible.value = false
        loadData()
      } catch (error: unknown) {
        if (error instanceof Error) ElMessage.error(error.message || '操作失败')
      }
    }
  }
}
async function handleDetailExecute() {
  if (!currentDetail.value) return
  try {
    await ElMessageBox.confirm('确认执行出库操作？出库后库存将扣减。', '出库确认', {
      confirmButtonText: '确认出库',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await inventoryOutboundApi.execute(currentDetail.value.outboundId)
    ElMessage.success('出库成功')
    detailDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '出库失败')
    }
  }
}

/* ===== 扫码添加 ===== */
const scanDialogVisible = ref(false)
const scanBarcode = ref('')
const scannedItems = ref<{ barcode: string; materialName: string; quantity: number }[]>([])
const scanInputRef = ref<HTMLInputElement | null>(null)

function handleScanBarcode() {
  scanDialogVisible.value = true
  scanBarcode.value = ''
  scannedItems.value = []
  nextTick(() => {
    scanInputRef.value?.focus()
  })
}

function handleScanSubmit() {
  if (!scanBarcode.value) return
  scannedItems.value.push({
    barcode: scanBarcode.value,
    materialName: `物料-${scanBarcode.value.slice(-4)}`,
    quantity: 1,
  })
  scanBarcode.value = ''
  nextTick(() => {
    scanInputRef.value?.focus()
  })
}

/** 确认添加扫码物料到出库明细 */
function handleScanConfirm() {
  for (const item of scannedItems.value) {
    createForm.value.items.push({
      materialId: item.barcode,
      requestQuantity: item.quantity,
      remark: '',
    })
  }
  ElMessage.success(`已添加 ${scannedItems.value.length} 项扫码物料`)
  scanDialogVisible.value = false
}

/** 手动添加空行 */
function handleManualAdd() {
  createForm.value.items.push({
    materialId: '',
    requestQuantity: 1,
    remark: '',
  })
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadWarehouseOptions()
  loadTargetStoreOptions()
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存出库" description="库存出库管理与审批（出库执行后，目标门店库存将由后端服务自动同步）">
      <el-button type="primary" size="default" @click="handleOpenCreate">
        <el-icon :size="16"><Plus /></el-icon>新建出库
      </el-button>
      <el-button size="default" @click="handleQuickScan">
        <el-icon :size="16"><Camera /></el-icon>
        扫码出库
      </el-button>
    </PageHeader>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.outboundType" placeholder="出库类型" clearable style="width:130px" size="default">
            <el-option v-for="opt in outboundTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.status" placeholder="出库状态" clearable style="width:130px" size="default">
            <el-option v-for="opt in outboundStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.warehouseId" placeholder="仓库" clearable style="width:130px" size="default">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            placement="bottom-start"
            style="width:240px;max-width:240px;flex-shrink:0"
            size="default"
            @change="handleDateChange"
          />
          <el-input v-model="searchForm.keyword" placeholder="搜索出库单号/物料" clearable style="width:200px" size="default" @keyup.enter="stdSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="stdSearch">查询</el-button>
          <el-button size="default" @click="stdReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
        <template #outboundType="{ row }">
          <StatusTag :status="inventoryOutboundConverter.toTypeStatusTagStatus(row.outboundType)" :label="inventoryOutboundConverter.toTypeLabel(row.outboundType)" size="small" />
        </template>
        <template #status="{ row }">
          <StatusTag :status="inventoryOutboundConverter.toStatusTagStatus(row.status)" :label="inventoryOutboundConverter.toStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleApprove(row)">审批</el-button>
          <el-button v-if="row.status === 'approved'" link type="primary" size="small" @click="handleExecute(row)">出库</el-button>
        </template>
      </DataTable>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total,prev,pager,next,jumper"
        @size-change="(val: number) => { pagination.size = val; loadData() }"
        @current-change="(val: number) => { pagination.current = val; loadData() }"
      />
    </div>

    <!-- 新建出库单弹窗 -->
    <el-dialog v-model="createDialogVisible" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <DetailHeader
        title="新建出库单"
        subtitle="填写出库单信息并选择需要出库的物料"
        :meta-items="[
          { label: '创建人', value: '当前用户', icon: User },
          { label: '创建时间', value: new Date().toLocaleString('zh-CN'), icon: Clock },
        ]"
        primary-action-text="提交出库单"
        @primary-action="handleCreateSubmit"
      />
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="90px">
        <DetailSection title="基础信息" description="出库类型、仓库、目标门店/供应商等基础字段" :show-divider="true">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="出库类型" prop="outboundType">
                <el-select v-model="createForm.outboundType" placeholder="请选择出库类型" :teleported="false" style="width:100%">
                  <el-option v-for="opt in outboundTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="仓库" prop="warehouseId">
                <el-select v-model="createForm.warehouseId" placeholder="请选择仓库" :teleported="false" style="width:100%" @change="handleWarehouseChange">
                  <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="目标" prop="targetId">
                <el-select
                  v-model="createForm.targetId"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="搜索或输入目标门店/部门"
                  :teleported="false"
                  style="width: 100%"
                  @change="handleTargetChange"
                >
                  <el-option-group v-if="targetStoreOptions.length" label="门店">
                    <el-option v-for="store in targetStoreOptions" :key="store.storeId" :label="store.storeName" :value="store.storeId" />
                  </el-option-group>
                  <el-option-group v-if="targetSupplierOptions.length && (createForm.outboundType === 'return' || createForm.outboundType === 'other')" label="供应商">
                    <el-option v-for="sup in targetSupplierOptions" :key="sup.supplierId" :label="sup.supplierName" :value="sup.supplierId" />
                  </el-option-group>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="出库日期" prop="outboundDate">
                <el-date-picker v-model="createForm.outboundDate" type="date" placeholder="请选择日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="关联单号">
                <el-input v-model="createForm.referenceNo" placeholder="采购/调拨单号（选填）" />
              </el-form-item>
            </el-col>
          </el-row>
        </DetailSection>

        <DetailSection
          title="选择库存物料"
          description="选择仓库后可在下表中勾选需要出库的物料；也可通过「扫码添加」快速录入。"
          :badge="selectedInventoryIds.length > 0 ? `已选${selectedInventoryIds.length}项` : undefined"
          :collapsible="true"
          :default-collapsed="warehouseInventory.length === 0"
          :show-divider="true"
        >
          <div v-if="warehouseInventory.length > 0" class="inventory-section">
            <div class="inventory-section__toolbar">
              <span class="inventory-section__title">当前仓库可用库存</span>
              <div class="inventory-section__actions">
                <el-button size="small" type="success" @click="handleScanBarcode">
                  <el-icon :size="14"><Camera /></el-icon>
                  扫码添加
                </el-button>
                <el-button size="small" type="primary" @click="handleManualAdd">
                  <el-icon :size="14"><Plus /></el-icon>
                  手动添加
                </el-button>
              </div>
            </div>
            <el-table
              :data="warehouseInventory"
              border
              size="small"
              style="width:100%"
              max-height="260"
              @selection-change="(selection: InventoryInfo[]) => { selectedInventoryIds = selection.map(s => s.inventoryId); handleInventorySelectionChange() }"
            >
              <el-table-column type="selection" width="45" />
              <el-table-column prop="materialName" label="物料名称" min-width="120" />
              <el-table-column prop="specification" label="规格" min-width="90" />
              <el-table-column prop="unit" label="单位" width="60" align="center" />
              <el-table-column prop="quantity" label="库存数量" width="80" align="center" />
              <el-table-column prop="availableQuantity" label="可用数量" width="80" align="center" />
              <el-table-column prop="batchNo" label="批次号" min-width="110" />
            </el-table>
          </div>
          <div v-else-if="createForm.warehouseId && !inventoryLoading" class="empty-inventory-hint">该仓库暂无库存数据</div>
          <el-empty v-else-if="!createForm.warehouseId" description="请先选择出库仓库" :image-size="100" />
        </DetailSection>

        <DetailSection
          title="出库明细"
          :badge="createForm.items.length > 0 ? `共${createForm.items.length}项` : undefined"
          description="核对物料数量，确保不超过可用库存；单价与小计由后端按最新成本计算。"
          :collapsible="true"
          :show-divider="true"
        >
          <el-table v-if="createForm.items.length > 0" :data="createForm.items" border size="small" style="width:100%">
            <el-table-column label="物料ID" min-width="140">
              <template #default="{ row: item }">{{ item.materialId }}</template>
            </el-table-column>
            <el-table-column label="申请数量" width="140">
              <template #default="{ row: item }">
                <el-input-number v-model="item.requestQuantity" :min="1" :max="warehouseInventory.find(inv => inv.materialId === item.materialId)?.availableQuantity" :step="1" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="160">
              <template #default="{ row: item }">
                <el-input v-model="item.remark" placeholder="备注（选填）" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂未选择出库物料，可在上方勾选或扫码添加" :image-size="100" />
        </DetailSection>

        <DetailSection title="备注信息" :collapsible="true" :default-collapsed="true" :show-divider="false">
          <el-form-item label="备注">
            <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息（选填）" />
          </el-form-item>
        </DetailSection>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 出库详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" width="1040px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <template v-if="currentDetail">
        <DetailHeader
          :title="currentDetail.outboundCode"
          :subtitle="`出库单 #${currentDetail.outboundId}`"
          :meta-items="detailMetaItems"
          :primary-action-text="currentDetail.status === 'pending' ? '审批通过' : currentDetail.status === 'approved' ? '执行出库' : undefined"
          :primary-action-loading="false"
          @primary-action="currentDetail.status === 'pending' ? handleDetailApprove : handleDetailExecute"
        >
          <template #status>
            <StatusTag :status="detailStatusTagStatus" :label="detailStatusLabel" size="medium" variant="solid" />
          </template>
        </DetailHeader>

        <DetailSection title="基础概览" description="出库单关键基础信息与金额汇总" :show-divider="true">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="出库类型">
              <StatusTag :status="inventoryOutboundConverter.toTypeStatusTagStatus(currentDetail.outboundType)" :label="inventoryOutboundConverter.toTypeLabel(currentDetail.outboundType)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="出库日期">{{ currentDetail.outboundDate }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ currentDetail.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="目标">{{ currentDetail.targetName }}</el-descriptions-item>
            <el-descriptions-item label="关联单号">{{ currentDetail.referenceNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="总数量">{{ currentDetail.totalQuantity }}</el-descriptions-item>
            <el-descriptions-item label="总金额">
              <span style="display: inline-flex; align-items: center; gap: 4px; color: var(--fts-error); font-weight: var(--fts-font-weight-semibold)">
                <el-icon :size="14"><Money /></el-icon>
                ¥ {{ currentDetail.totalAmount }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ currentDetail.completeTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </DetailSection>

        <DetailSection
          title="出库明细"
          :badge="currentDetail.items?.length ? `共${currentDetail.items.length}项` : undefined"
          description="物料出库数量与成本明细"
          :collapsible="true"
          v-model:collapsed="detailSectionCollapsed.items"
          :show-divider="true"
        >
          <el-table v-if="currentDetail.items?.length" :data="currentDetail.items" border size="small">
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column prop="specification" label="规格" min-width="90" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="requestQuantity" label="申请数量" width="90" align="center" />
            <el-table-column prop="actualQuantity" label="实际数量" width="90" align="center" />
            <el-table-column prop="batchNo" label="批次号" min-width="120" />
            <el-table-column prop="unitCost" label="单价(元)" width="95" align="right" />
            <el-table-column prop="totalCost" label="小计(元)" width="105" align="right" />
          </el-table>
          <el-empty v-else description="暂无明细" :image-size="80" />
        </DetailSection>

        <DetailSection
          title="审批流转"
          description="出库单的申请与审批时间线"
          :collapsible="true"
          v-model:collapsed="detailSectionCollapsed.approvals"
          :show-divider="true"
        >
          <el-timeline>
            <el-timeline-item
              :timestamp="currentDetail.applyTime"
              placement="top"
              type="primary"
              :hollow="true"
            >
              <div class="timeline-content">
                <div class="timeline-content__title">提交申请</div>
                <div class="timeline-content__desc">申请人：{{ currentDetail.applyUserName }}</div>
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.approveTime"
              :timestamp="currentDetail.approveTime"
              placement="top"
              :type="currentDetail.status === 'rejected' ? 'danger' : 'success'"
              :hollow="true"
            >
              <div class="timeline-content">
                <div class="timeline-content__title">
                  {{ currentDetail.status === 'rejected' ? '审批驳回' : '审批通过' }}
                </div>
                <div class="timeline-content__desc">审批人：{{ currentDetail.approveUserName }}</div>
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.completeTime"
              :timestamp="currentDetail.completeTime"
              placement="top"
              type="success"
              :hollow="true"
            >
              <div class="timeline-content">
                <div class="timeline-content__title">出库完成</div>
                <div class="timeline-content__desc">库存扣减与门店同步由后端自动完成</div>
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="!currentDetail.approveTime && currentDetail.status === 'pending'"
              timestamp="待处理"
              placement="top"
              type="warning"
              :hollow="true"
            >
              <div class="timeline-content">
                <div class="timeline-content__title">等待审批</div>
                <div class="timeline-content__desc">请相关负责人尽快处理</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </DetailSection>

        <DetailSection title="备注说明" :collapsible="true" :default-collapsed="true" :show-divider="false">
          <p v-if="currentDetail.remark" class="remark-text">{{ currentDetail.remark }}</p>
          <span v-else style="color: var(--fts-text-tertiary); font-size: var(--fts-font-size-sm)">暂无备注</span>
        </DetailSection>
      </template>
    </el-dialog>

    <!-- 扫码添加弹窗 -->
    <el-dialog v-model="scanDialogVisible" title="扫码添加物料" width="680px" class="fts-dialog--md" destroy-on-close lock-scroll="false">
      <div class="scan-instructions">请使用扫码枪扫描物料条码，或手动输入条码后按回车</div>
      <el-input
        ref="scanInputRef"
        v-model="scanBarcode"
        placeholder="扫描或输入条码"
        size="large"
        @keyup.enter="handleScanSubmit"
      >
        <template #prefix><el-icon><Camera /></el-icon></template>
      </el-input>
      <div v-if="scannedItems.length > 0" style="margin-top: var(--fts-space-4)">
        <h4 style="margin-bottom: var(--fts-space-3)">已扫描物料（{{ scannedItems.length }}项）</h4>
        <el-table :data="scannedItems" border size="small">
          <el-table-column prop="barcode" label="条码" min-width="120" />
          <el-table-column prop="materialName" label="物料名称" min-width="120" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="scannedItems.splice($index, 1)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="scanDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="scannedItems.length === 0" @click="handleScanConfirm">确认添加</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section { grid-template-columns: repeat(4, 1fr); }
.empty-inventory-hint {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  padding: var(--fts-space-4) 0;
  text-align: center;
}
.scan-instructions {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  margin-bottom: var(--fts-space-3);
}

.inventory-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);

  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: var(--fts-space-2);

  &__title {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
  }

  &__desc {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-xs);
  }
}

.remark-text {
  margin: 0;
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
