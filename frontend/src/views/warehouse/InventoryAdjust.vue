<script setup lang="ts">
/**
 * 库存调整管理页面
 * 功能：调整单列表、新建调整单（自动加载库存）、审批调整单（多部门会签）、执行调整、查看详情
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Link } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryAdjustApi, inventoryApi, warehouseApi, inventoryAdjustConverter } from '@/api/warehouse'
import type { InventoryAdjustInfo, AdjustStatus, AdjustType, AdjustQueryForm, AdjustCreateForm, ApprovalHistoryItem } from '@/types/warehouse-adjust'
import type { WarehouseInfo } from '@/types/warehouse'
import type { InventoryInfo } from '@/types/warehouse-inventory'
import type { StatColorType } from '@/types/warehouse-stats'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 搜索表单 ===== */
const searchForm = ref<AdjustQueryForm & { keyword: string; dateRange: [string, string] | null }>({
  adjustType: undefined,
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

/* ===== 统计卡片 ===== */
const tableData = ref<InventoryAdjustInfo[]>([])

const statistics = computed(() => {
  const all = tableData.value
  const pending = all.filter(r => r.status === 'pending').length
  const approved = all.filter(r => r.status === 'approved').length
  const completed = all.filter(r => r.status === 'completed').length
  const totalAmount = all.reduce((sum, r) => sum + Math.abs(Number(r.totalAdjustAmount) || 0), 0)
  return [
    { icon: 'Warning', label: '待审批', value: pending, colorType: 'warning' as StatColorType },
    { icon: 'Check', label: '已审批', value: approved, colorType: 'primary' as StatColorType },
    { icon: 'SuccessFilled', label: '已完成', value: completed, colorType: 'success' as StatColorType },
    { icon: 'DataLine', label: '调整总额', value: `${totalAmount.toFixed(2)}元`, colorType: 'error' as StatColorType },
  ]
})

/* ===== 表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'adjustCode', label: '调整单号', minWidth: 145 },
  { prop: 'adjustType', label: '调整类型', minWidth: 100, slot: 'adjustType' },
  { prop: 'warehouseName', label: '仓库', minWidth: 120 },
  { prop: 'adjustReason', label: '调整原因', minWidth: 110 },
  { prop: 'totalAdjustQuantity', label: '调整总数量', minWidth: 100, align: 'center' },
  { prop: 'totalAdjustAmount', label: '调整总金额', minWidth: 110, align: 'right' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const params: AdjustQueryForm = {
      adjustType: searchForm.value.adjustType || undefined,
      status: searchForm.value.status || undefined,
      warehouseId: searchForm.value.warehouseId || undefined,
      startDate: searchForm.value.startDate || undefined,
      endDate: searchForm.value.endDate || undefined,
      keyword: searchForm.value.keyword || undefined,
      page: pagination.current,
      size: pagination.size,
    }
    const res = await inventoryAdjustApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载调整列表失败')
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */
function resetSearchForm() {
  searchForm.value = { adjustType: undefined, status: undefined, warehouseId: undefined, startDate: '', endDate: '', keyword: '', dateRange: null }
}

/* ===== 日期范围变更 ===== */
function handleDateChange(val: [string, string] | null) {
  searchForm.value.startDate = val?.[0] || ''
  searchForm.value.endDate = val?.[1] || ''
}

/* ===== 调整类型选项 ===== */
const adjustTypeOptions: { label: string; value: AdjustType }[] = [
  { label: '盘盈调整', value: 'gain' },
  { label: '盘亏调整', value: 'loss' },
  { label: '温度损耗', value: 'temp_loss' },
  { label: '称重差异', value: 'weight_diff' },
  { label: '其他调整', value: 'other' },
]

/* ===== 调整状态选项 ===== */
const adjustStatusOptions: { label: string; value: AdjustStatus }[] = [
  { label: '待审批', value: 'pending' },
  { label: '已审批', value: 'approved' },
  { label: '已完成', value: 'completed' },
  { label: '已驳回', value: 'rejected' },
]

/* ===== 调整原因选项（按调整类型分类） ===== */
const adjustReasonOptions: Record<AdjustType, string[]> = {
  gain: ['盘点盈余', '入库少记', '其他'],
  loss: ['自然损耗', '变质淘汰', '盘点短缺', '其他'],
  temp_loss: ['冷库故障', '运输温度异常', '其他'],
  weight_diff: ['实际称重偏少', '水分蒸发', '其他'],
  other: ['系统更正', '数据修正', '其他'],
}

/** 参与部门选项 */
const deptOptions = [
  { label: '仓储部', value: '仓储部' },
  { label: '财务部', value: '财务部' },
  { label: '采购部', value: '采购部' },
  { label: '运营部', value: '运营部' },
]

/** 根据调整类型获取原因选项 */
const currentReasonOptions = computed(() => {
  return adjustReasonOptions[createForm.value.adjustType] || ['其他']
})

/* ===== 新建调整单 ===== */
const createDialogVisible = ref(false)
const createForm = ref<AdjustCreateForm>({
  adjustType: 'loss',
  warehouseId: '',
  adjustReason: '',
  referenceCheckCode: '',
  referenceNo: '',
  participatingDepts: ['仓储部'],
  items: [],
  remark: '',
})
const createFormRef = ref()
const createFormRules = {
  adjustType: [{ required: true, message: '请选择调整类型', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  adjustReason: [{ required: true, message: '请选择调整原因', trigger: 'change' }],
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
  // 清空已选和明细
  selectedInventoryIds.value = []
  createForm.value.items = []
}

/** 选中库存项变更时，同步到调整明细 */
function handleInventorySelectionChange() {
  // 保留手动添加的项（没有inventoryId对应的）
  const manualItems = createForm.value.items.filter(
    item => !warehouseInventory.value.some(inv => inv.materialId === item.materialId)
  )
  // 从选中的库存项生成明细
  const selectedItems = warehouseInventory.value
    .filter(inv => selectedInventoryIds.value.includes(inv.inventoryId))
    .map(inv => ({
      materialId: inv.materialId,
      materialName: inv.materialName,
      specification: inv.specification,
      unit: inv.unit,
      beforeQuantity: inv.quantity,
      batchNo: inv.batchNo,
      unitCost: inv.unitCost,
      adjustQuantity: 0,
      reason: '',
    }))
  createForm.value.items = [...selectedItems, ...manualItems]
}

function handleOpenCreate() {
  createForm.value = {
    adjustType: 'loss',
    warehouseId: '',
    adjustReason: '',
    referenceCheckCode: '',
    referenceNo: '',
    participatingDepts: ['仓储部'],
    items: [],
    remark: '',
  }
  warehouseInventory.value = []
  selectedInventoryIds.value = []
  createDialogVisible.value = true
}

/** 调整类型变更时重置调整原因 */
function handleAdjustTypeChange() {
  createForm.value.adjustReason = ''
}

/** 手动添加调整明细行 */
function addManualItem() {
  createForm.value.items.push({
    materialId: '',
    adjustQuantity: 0,
    reason: '',
  })
}

/** 删除调整明细行 */
function removeAdjustItem(index: number) {
  const item = createForm.value.items[index]
  // 如果是库存自动加载的项，同时取消选中
  if (item.materialId) {
    const inv = warehouseInventory.value.find(i => i.materialId === item.materialId)
    if (inv) {
      selectedInventoryIds.value = selectedInventoryIds.value.filter(id => id !== inv.inventoryId)
    }
  }
  createForm.value.items.splice(index, 1)
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
    const hasEmptyMaterial = createForm.value.items.some(item => !item.materialId)
    if (hasEmptyMaterial) {
      ElMessage.warning('请填写调整明细中的物料')
      return
    }
    if (createForm.value.items.length === 0) {
      ElMessage.warning('请至少添加一条调整明细')
      return
    }
    const hasZeroQty = createForm.value.items.some(item => item.adjustQuantity === 0)
    if (hasZeroQty) {
      ElMessage.warning('调整数量不能为0')
      return
    }
    // 校验盘亏数量不超过当前库存（手动添加的明细 beforeQuantity 可能为 undefined，按 0 处理）
    for (const item of createForm.value.items) {
      const beforeQty = item.beforeQuantity ?? 0
      if (item.adjustQuantity < 0 && Math.abs(item.adjustQuantity) > beforeQty) {
        ElMessage.warning(`${item.materialName} 盘亏数量(${Math.abs(item.adjustQuantity)})超过当前库存(${beforeQty})`)
        return
      }
    }
    await inventoryAdjustApi.create(createForm.value)
    ElMessage.success('调整单创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '创建失败')
  }
}

/* ===== 审批调整单（自定义审批对话框） ===== */
const approveDialogVisible = ref(false)
const approvingRow = ref<InventoryAdjustInfo | null>(null)
const approveForm = ref<{ approved: boolean; opinion: string }>({
  approved: true,
  opinion: '',
})

function handleOpenApprove(row: InventoryAdjustInfo) {
  approvingRow.value = row
  approveForm.value = { approved: true, opinion: '' }
  approveDialogVisible.value = true
}

async function submitApprove() {
  if (!approveForm.value.opinion?.trim()) {
    ElMessage.warning('请输入审批意见')
    return
  }
  if (!approvingRow.value) return
  try {
    await inventoryAdjustApi.approve(approvingRow.value.adjustId, {
      approved: approveForm.value.approved,
      opinion: approveForm.value.opinion,
    })
    ElMessage.success(approveForm.value.approved ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  }
}

/* ===== 执行调整 ===== */
async function handleExecute(row: InventoryAdjustInfo) {
  try {
    await ElMessageBox.confirm('确认执行调整操作？执行后库存将更新。', '调整确认', {
      confirmButtonText: '确认执行',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await inventoryAdjustApi.execute(row.adjustId)
    ElMessage.success('调整执行成功')
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '调整执行失败')
    }
  }
}

/* ===== 查看详情 ===== */
const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryAdjustInfo | null>(null)

async function handleViewDetail(row: InventoryAdjustInfo) {
  try {
    const res = await inventoryAdjustApi.getById(row.adjustId)
    currentDetail.value = res
    detailDialogVisible.value = true
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载详情失败')
  }
}

/* ===== 审批历史步骤映射 ===== */
function getTimelineItemType(step: string): string {
  switch (step) {
    case 'approve': return 'primary'
    case 'reject': return 'danger'
    case 'execute': return 'success'
    default: return 'info'
  }
}

function getStepLabel(step: string): string {
  switch (step) {
    case 'submit': return '提交'
    case 'approve': return '审批通过'
    case 'reject': return '审批驳回'
    case 'execute': return '执行'
    default: return step
  }
}

/** 部门会签状态（mock） */
function getDeptSignStatus(dept: string, adjustInfo: InventoryAdjustInfo): string {
  if (adjustInfo.status === 'completed') return '已签'
  if (dept === '仓储部') return '已签'
  return '待签'
}

/* ===== CSS变量获取 ===== */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
const cssPrimary = computed(() => getCssVar('--fts-primary'))

/* ===== 初始化 ===== */
onMounted(() => {
  loadWarehouseOptions()
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存调整" description="库存盘盈盘亏与调整管理（调整执行后，库存数据将由后端服务自动更新）">
      <el-button type="primary" size="default" @click="handleOpenCreate">
        <el-icon :size="16"><Plus /></el-icon>新建调整
      </el-button>
    </PageHeader>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.adjustType" placeholder="调整类型" clearable style="width:130px" size="default">
            <el-option v-for="opt in adjustTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.status" placeholder="调整状态" clearable style="width:130px" size="default">
            <el-option v-for="opt in adjustStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
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
          <el-input v-model="searchForm.keyword" placeholder="搜索调整单号/物料" clearable style="width:200px" size="default" @keyup.enter="stdSearch">
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
        <template #adjustType="{ row }">
          <StatusTag :status="inventoryAdjustConverter.toTypeStatusTagStatus(row.adjustType)" :label="inventoryAdjustConverter.toTypeLabel(row.adjustType)" size="small" />
        </template>
        <template #status="{ row }">
          <StatusTag :status="inventoryAdjustConverter.toStatusTagStatus(row.status)" :label="inventoryAdjustConverter.toStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleOpenApprove(row)">审批</el-button>
          <el-button v-if="row.status === 'approved'" link type="primary" size="small" @click="handleExecute(row)">执行</el-button>
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

    <!-- 新建调整单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建调整单" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="100px">
        <el-form-item label="调整类型" prop="adjustType">
          <el-select v-model="createForm.adjustType" placeholder="请选择调整类型" :teleported="false" style="width:100%" @change="handleAdjustTypeChange">
            <el-option v-for="opt in adjustTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="createForm.warehouseId" placeholder="请选择仓库" :teleported="false" style="width:100%" @change="handleWarehouseChange">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整原因" prop="adjustReason">
          <el-select v-model="createForm.adjustReason" placeholder="请选择调整原因" :teleported="false" style="width:100%">
            <el-option v-for="reason in currentReasonOptions" :key="reason" :label="reason" :value="reason" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与部门">
          <el-checkbox-group v-model="createForm.participatingDepts">
            <el-checkbox v-for="dept in deptOptions" :key="dept.value" :label="dept.value" :value="dept.value" />
          </el-checkbox-group>
        </el-form-item>

        <!-- 关联盘点单号（盘盈/盘亏时突出显示） -->
        <el-form-item v-if="createForm.adjustType === 'gain' || createForm.adjustType === 'loss'" label="关联盘点单号">
          <el-input v-model="createForm.referenceCheckCode" placeholder="请输入关联盘点单号">
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
          <div class="check-hint">提示：盘盈/盘亏调整应基于盘点结果，请关联对应的盘点单号</div>
        </el-form-item>
        <el-form-item v-else label="关联单号">
          <el-input v-model="createForm.referenceNo" placeholder="请输入关联单号">
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
        </el-form-item>

        <!-- 仓库库存选择（选择仓库后自动加载） -->
        <el-form-item v-if="warehouseInventory.length > 0" label="选择库存">
          <div style="width:100%">
            <el-table
              ref="inventoryTableRef"
              :data="warehouseInventory"
              border
              size="small"
              style="width:100%"
              max-height="240"
              @selection-change="(selection: InventoryInfo[]) => { selectedInventoryIds = selection.map(s => s.inventoryId); handleInventorySelectionChange() }"
            >
              <el-table-column type="selection" width="45" />
              <el-table-column prop="materialName" label="物料名称" min-width="100" />
              <el-table-column prop="specification" label="规格" min-width="80" />
              <el-table-column prop="unit" label="单位" width="60" align="center" />
              <el-table-column prop="quantity" label="库存数量" width="80" align="center" />
              <el-table-column prop="batchNo" label="批次号" min-width="100" />
              <el-table-column prop="unitCost" label="单价(元)" width="80" align="right" />
            </el-table>
          </div>
        </el-form-item>
        <el-form-item v-else-if="createForm.warehouseId && !inventoryLoading" label="选择库存">
          <div class="empty-inventory-hint">该仓库暂无库存数据</div>
        </el-form-item>

        <!-- 调整明细 -->
        <el-form-item label="调整明细">
          <div style="width:100%">
            <el-table :data="createForm.items" border size="small" style="width:100%">
              <el-table-column label="物料" min-width="120">
                <template #default="{ row: item }">
                  <span v-if="item.materialName">{{ item.materialName }}</span>
                  <el-input v-else v-model="item.materialId" placeholder="物料ID" />
                </template>
              </el-table-column>
              <el-table-column label="规格" width="80">
                <template #default="{ row: item }">
                  <span v-if="item.specification">{{ item.specification }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="当前数量" width="80" align="center">
                <template #default="{ row: item }">
                  {{ item.beforeQuantity ?? '-' }}
                </template>
              </el-table-column>
              <el-table-column label="调整数量" width="130">
                <template #default="{ row: item }">
                  <el-input-number v-model="item.adjustQuantity" :min="createForm.adjustType === 'gain' ? 1 : undefined" :max="createForm.adjustType === 'loss' || createForm.adjustType === 'temp_loss' || createForm.adjustType === 'weight_diff' ? -1 : undefined" :step="1" style="width:100%" />
                </template>
              </el-table-column>
              <el-table-column label="原因" min-width="120">
                <template #default="{ row: item }">
                  <el-input v-model="item.reason" placeholder="调整原因" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="removeAdjustItem($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="adjust-items-actions">
              <el-button link type="primary" size="small" @click="addManualItem">+ 手动添加</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批调整单弹窗 -->
    <el-dialog v-model="approveDialogVisible" title="审批调整单" width="480px" class="fts-dialog--sm" destroy-on-close lock-scroll="false">
      <template v-if="approvingRow">
        <el-form label-width="90px">
          <el-form-item label="审批结果">
            <el-radio-group v-model="approveForm.approved">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审批意见">
            <el-input v-model="approveForm.opinion" type="textarea" :rows="3" placeholder="请输入审批意见" />
          </el-form-item>
          <!-- 部门会签 -->
          <el-form-item v-if="approvingRow.participatingDepts?.length" label="部门会签">
            <div v-for="dept in approvingRow.participatingDepts" :key="dept" class="dept-sign-item">
              <StatusTag :status="getDeptSignStatus(dept, approvingRow) === '已签' ? 'active' : 'pending'" :label="dept" size="small" />
              <span class="dept-sign-status">{{ getDeptSignStatus(dept, approvingRow) }}</span>
            </div>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="approveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApprove">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 调整详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="调整单详情" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="调整单号">{{ currentDetail.adjustCode }}</el-descriptions-item>
          <el-descriptions-item label="调整类型">
            <StatusTag :status="inventoryAdjustConverter.toTypeStatusTagStatus(currentDetail.adjustType)" :label="inventoryAdjustConverter.toTypeLabel(currentDetail.adjustType)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="仓库">{{ currentDetail.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="inventoryAdjustConverter.toStatusTagStatus(currentDetail.status)" :label="inventoryAdjustConverter.toStatusLabel(currentDetail.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="调整原因">{{ currentDetail.adjustReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联单号">{{ currentDetail.referenceNo || currentDetail.referenceCheckCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="调整总数量">{{ currentDetail.totalAdjustQuantity }}</el-descriptions-item>
          <el-descriptions-item label="调整总金额">{{ currentDetail.totalAdjustAmount }}元</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentDetail.applyUserName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentDetail.applyTime }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ currentDetail.approveUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ currentDetail.approveTime || '-' }}</el-descriptions-item>
          <!-- 参与部门 -->
          <el-descriptions-item v-if="currentDetail.participatingDepts?.length" label="参与部门" :span="2">
            <span v-for="(dept, idx) in currentDetail.participatingDepts" :key="dept">
              <StatusTag :status="getDeptSignStatus(dept, currentDetail) === '已签' ? 'active' : 'pending'" :label="dept" size="small" />
              <span class="dept-sign-status">{{ getDeptSignStatus(dept, currentDetail) }}</span>
              <span v-if="idx < currentDetail.participatingDepts.length - 1" style="margin-right: var(--fts-space-3)"></span>
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentDetail.items?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">调整明细</h4>
          <el-table :data="currentDetail.items" border size="small">
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column prop="specification" label="规格" min-width="80" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="beforeQuantity" label="调整前数量" width="100" align="center" />
            <el-table-column prop="adjustQuantity" label="调整数量" width="90" align="center" />
            <el-table-column prop="afterQuantity" label="调整后数量" width="100" align="center" />
            <el-table-column prop="adjustAmount" label="调整金额(元)" width="100" align="right" />
            <el-table-column prop="reason" label="调整原因" min-width="100" />
          </el-table>
        </div>
        <!-- 操作记录 -->
        <div v-if="currentDetail.approvalHistory?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">操作记录</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(record, index) in currentDetail.approvalHistory"
              :key="index"
              :timestamp="record.time"
              placement="top"
              :type="getTimelineItemType(record.step) as 'primary' | 'success' | 'warning' | 'danger' | 'info'"
              :color="index === 0 ? cssPrimary : undefined"
            >
              <div>{{ record.userName }} {{ getStepLabel(record.step) }}</div>
              <div v-if="record.comment" class="approval-comment">{{ record.comment }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stats-section { grid-template-columns: repeat(4, 1fr); }
.approval-comment {
  color: var(--fts-text-secondary);
  font-size: 12px;
  margin-top: 2px;
}
.dept-sign-item {
  display: flex;
  align-items: center;
  margin-bottom: var(--fts-space-2);
}
.dept-sign-status {
  margin-left: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: 13px;
}
.check-hint {
  color: var(--fts-text-secondary);
  font-size: 12px;
  margin-top: var(--fts-space-1);
  line-height: 1.4;
}
.adjust-items-actions {
  margin-top: var(--fts-space-2);
}
.empty-inventory-hint {
  color: var(--fts-text-secondary);
  font-size: 13px;
}
</style>
