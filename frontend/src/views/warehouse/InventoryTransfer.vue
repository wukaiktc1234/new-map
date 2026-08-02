<script setup lang="ts">
/**
 * 库存调拨管理页面
 * 功能：调拨单列表、新建调拨单、审批调拨单、查看详情
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryTransferApi, warehouseApi, inventoryApi, inventoryTransferConverter } from '@/api/warehouse'
import type { InventoryTransferInfo, TransferStatus, InventoryTransferQueryForm, InventoryTransferCreateForm, TransferApproveForm } from '@/types/warehouse-transfer'
import type { WarehouseInfo } from '@/types/warehouse'
import type { InventoryInfo } from '@/types/warehouse-inventory'
import type { StatColorType } from '@/types/warehouse-stats'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 搜索表单 ===== */
const searchForm = ref<InventoryTransferQueryForm & { keyword: string; dateRange: [string, string] | null }>({
  status: undefined,
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

/* ===== 调出仓库库存列表 ===== */
const fromWarehouseInventory = ref<InventoryInfo[]>([])

async function handleFromWarehouseChange(warehouseId: string) {
  if (!warehouseId) {
    fromWarehouseInventory.value = []
    return
  }
  try {
    const res = await inventoryApi.getList({ warehouseId, page: 1, size: 200 })
    fromWarehouseInventory.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载库存数据失败')
    fromWarehouseInventory.value = []
  }
}

/* ===== 统计卡片（从列表数据计算） ===== */
const tableData = ref<InventoryTransferInfo[]>([])

const statistics = computed(() => {
  const all = tableData.value
  const shipping = all.filter(r => r.status === 'shipped').length
  const completed = all.filter(r => r.status === 'completed').length
  const total = all.length
  const pending = all.filter(r => r.status === 'pending').length
  return [
    { icon: 'Loading', label: '调拨中', value: shipping, colorType: 'info' as StatColorType },
    { icon: 'Check', label: '已完成', value: completed, colorType: 'success' as StatColorType },
    { icon: 'DataLine', label: '本月调拨', value: total, colorType: 'primary' as StatColorType },
    { icon: 'Warning', label: '待审批', value: pending, colorType: 'warning' as StatColorType },
  ]
})

/* ===== 表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'transferCode', label: '调拨单号', minWidth: 145 },
  { prop: 'fromWarehouseName', label: '调出仓库', minWidth: 120 },
  { prop: 'toWarehouseName', label: '调入仓库', minWidth: 120 },
  { prop: 'materialName', label: '物料名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'quantity', label: '总数量', minWidth: 85, align: 'center' },
  { prop: 'transferReason', label: '调拨原因', minWidth: 100 },
  { prop: 'applyTime', label: '调拨日期', minWidth: 130 },
  { prop: 'applyUserName', label: '申请人', minWidth: 90 },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const params: InventoryTransferQueryForm = {
      status: searchForm.value.status || undefined,
      startDate: searchForm.value.startDate || undefined,
      endDate: searchForm.value.endDate || undefined,
      transferCode: searchForm.value.keyword || undefined,
      page: pagination.current,
      size: pagination.size,
    }
    const res = await inventoryTransferApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载调拨列表失败')
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */
function resetSearchForm() {
  searchForm.value = { status: undefined, startDate: '', endDate: '', keyword: '', dateRange: null }
}

/* ===== 日期范围变更 ===== */
function handleDateChange(val: [string, string] | null) {
  searchForm.value.startDate = val?.[0] || ''
  searchForm.value.endDate = val?.[1] || ''
}

/* ===== 调拨状态选项 ===== */
const transferStatusOptions: { label: string; value: TransferStatus }[] = [
  { label: '待调拨', value: 'pending' },
  { label: '已发出', value: 'shipped' },
  { label: '已接收', value: 'received' },
  { label: '已完成', value: 'completed' },
]

/* ===== 调拨原因选项 ===== */
const transferReasonOptions = [
  '门店补货',
  '库存调配',
  '紧急调拨',
  '退货调拨',
  '其他',
]

/* ===== 新建调拨单 ===== */
const createDialogVisible = ref(false)
const createForm = ref<InventoryTransferCreateForm>({
  fromWarehouseId: '',
  toWarehouseId: '',
  transferReason: '',
  remark: '',
  items: [{ materialId: '', quantity: 1, remark: '' }],
})
const createFormRef = ref()
const createFormRules = {
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }],
  transferReason: [{ required: true, message: '请选择调拨原因', trigger: 'change' }],
}

function handleOpenCreate() {
  createForm.value = {
    fromWarehouseId: '',
    toWarehouseId: '',
    transferReason: '',
    remark: '',
    items: [{ materialId: '', quantity: 1, remark: '' }],
  }
  fromWarehouseInventory.value = []
  createDialogVisible.value = true
}

function addTransferItem() {
  createForm.value.items.push({ materialId: '', quantity: 1, remark: '' })
}

function removeTransferItem(index: number) {
  if (createForm.value.items.length > 1) {
    createForm.value.items.splice(index, 1)
  }
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
    if (createForm.value.fromWarehouseId === createForm.value.toWarehouseId) {
      ElMessage.warning('调出仓库和调入仓库不能相同')
      return
    }
    const hasEmptyMaterial = createForm.value.items.some(item => !item.materialId)
    if (hasEmptyMaterial) {
      ElMessage.warning('请选择调拨物料')
      return
    }
    await inventoryTransferApi.create(createForm.value)
    ElMessage.success('调拨单创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '创建失败')
  }
}

/* ===== 审批调拨单（自定义审批对话框） ===== */
const approveDialogVisible = ref(false)
const approvingRow = ref<InventoryTransferInfo | null>(null)
const approveForm = ref<{ approved: boolean; opinion: string }>({
  approved: true,
  opinion: '',
})

function handleOpenApprove(row: InventoryTransferInfo) {
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
    const data: TransferApproveForm = {
      approved: approveForm.value.approved,
      remark: approveForm.value.opinion,
    }
    await inventoryTransferApi.approve(approvingRow.value.transferId, data)
    ElMessage.success(approveForm.value.approved ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  }
}

/* ===== 查看详情 ===== */
const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryTransferInfo | null>(null)

async function handleViewDetail(row: InventoryTransferInfo) {
  try {
    const res = await inventoryTransferApi.getById(row.transferId)
    currentDetail.value = res
    detailDialogVisible.value = true
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载详情失败')
  }
}

/* ===== 确认收货 ===== */
async function handleConfirmReceipt(row: InventoryTransferInfo) {
  try {
    await ElMessageBox.confirm(
      '确认已收到调拨物料？确认后调入仓库库存将增加。',
      '确认收货',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
    await inventoryTransferApi.execute(row.transferId)
    ElMessage.success('收货确认成功，库存已更新')
    loadData()
  } catch {
    // 用户取消或操作失败
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
    <PageHeader title="库存调拨" description="门店/仓库间库存调配管理（调拨完成后，双方仓库库存将由后端服务自动更新）">
      <el-button type="primary" size="default" @click="handleOpenCreate">
        <el-icon :size="16"><Plus /></el-icon>新建调拨
      </el-button>
    </PageHeader>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.status" placeholder="调拨状态" clearable style="width:130px" size="default">
            <el-option v-for="opt in transferStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
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
          <el-input v-model="searchForm.keyword" placeholder="搜索调拨单号" clearable style="width:200px" size="default" @keyup.enter="stdSearch">
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
        <template #status="{ row }">
          <StatusTag :status="inventoryTransferConverter.toStatusTagStatus(row.status)" :label="inventoryTransferConverter.toStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleOpenApprove(row)">审批</el-button>
          <el-button v-if="row.status === 'shipped'" link type="primary" size="small" @click="handleConfirmReceipt(row)">确认收货</el-button>
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

    <!-- 新建调拨单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建调拨单" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="90px">
        <el-form-item label="调出仓库" prop="fromWarehouseId">
          <el-select v-model="createForm.fromWarehouseId" placeholder="请选择调出仓库" :teleported="false" style="width:100%" @change="handleFromWarehouseChange">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="调入仓库" prop="toWarehouseId">
          <el-select v-model="createForm.toWarehouseId" placeholder="请选择调入仓库" :teleported="false" style="width:100%">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="调拨原因" prop="transferReason">
          <el-select v-model="createForm.transferReason" placeholder="请选择调拨原因" :teleported="false" style="width:100%">
            <el-option v-for="reason in transferReasonOptions" :key="reason" :label="reason" :value="reason" />
          </el-select>
        </el-form-item>
        <el-form-item label="调拨明细">
          <div style="width:100%">
            <el-table :data="createForm.items" border size="small" style="width:100%">
              <el-table-column label="物料" min-width="140">
                <template #default="{ row: item }">
                  <el-select v-model="item.materialId" filterable allow-create default-first-option placeholder="搜索或输入物料" :teleported="false" style="width: 100%">
                    <el-option v-for="mat in fromWarehouseInventory" :key="mat.materialId" :label="`${mat.materialName} (${mat.materialId})`" :value="mat.materialId" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" width="130">
                <template #default="{ row: item }">
                  <el-input-number v-model="item.quantity" :min="1" style="width:100%" />
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="120">
                <template #default="{ row: item }">
                  <el-input v-model="item.remark" placeholder="备注" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ $index }">
                  <el-button v-if="createForm.items.length > 1" link type="danger" size="small" @click="removeTransferItem($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button link type="primary" size="small" style="margin-top:var(--fts-space-2)" @click="addTransferItem">+ 添加物料</el-button>
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

    <!-- 审批调拨单弹窗 -->
    <el-dialog v-model="approveDialogVisible" title="审批调拨单" width="480px" class="fts-dialog--sm" destroy-on-close lock-scroll="false">
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
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="approveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitApprove">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 调拨详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="调拨单详情" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="调拨单号">{{ currentDetail.transferCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="inventoryTransferConverter.toStatusTagStatus(currentDetail.status)" :label="inventoryTransferConverter.toStatusLabel(currentDetail.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="调出仓库">{{ currentDetail.fromWarehouseName }}</el-descriptions-item>
          <el-descriptions-item label="调入仓库">{{ currentDetail.toWarehouseName }}</el-descriptions-item>
          <el-descriptions-item label="调拨原因">{{ currentDetail.transferReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentDetail.applyUserName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentDetail.applyTime }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ currentDetail.approveUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ currentDetail.approveTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentDetail.items?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">调拨明细</h4>
          <el-table :data="currentDetail.items" border size="small">
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column prop="specification" label="规格" min-width="80" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column prop="remark" label="备注" min-width="100" />
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
</style>
