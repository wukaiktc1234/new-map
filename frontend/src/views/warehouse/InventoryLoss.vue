<script setup lang="ts">
/**
 * 库存报损管理页面
 * 功能：报损单列表、新建报损单、审批报损单、处理报损、查看详情
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryLossApi, warehouseApi, inventoryLossConverter } from '@/api/warehouse'
import type { InventoryLossInfo, LossType, LossStatus, InventoryLossQueryForm, InventoryLossCreateForm } from '@/types/warehouse-loss'
import type { WarehouseInfo } from '@/types/warehouse'
import type { StatColorType } from '@/types/warehouse-stats'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 搜索表单 ===== */
const searchForm = ref<InventoryLossQueryForm & { keyword: string }>({
  lossType: undefined,
  status: undefined,
  warehouseId: undefined,
  keyword: '',
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
const tableData = ref<InventoryLossInfo[]>([])

const statistics = computed(() => {
  const all = tableData.value
  const total = all.length
  const pending = all.filter(r => r.status === 'pending').length
  const approved = all.filter(r => r.status === 'approved').length
  const totalAmount = all.reduce((sum, r) => sum + parseFloat(r.totalAmount || '0'), 0)
  return [
    { icon: 'DataLine', label: '报损单总数', value: total, colorType: 'primary' as StatColorType },
    { icon: 'Warning', label: '待审批', value: pending, colorType: 'warning' as StatColorType },
    { icon: 'Check', label: '已审批', value: approved, colorType: 'info' as StatColorType },
    { icon: 'Money', label: '报损金额', value: `${totalAmount.toFixed(2)}元`, colorType: 'error' as StatColorType },
  ]
})

/* ===== 表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'lossCode', label: '报损单号', minWidth: 145 },
  { prop: 'warehouseName', label: '仓库', minWidth: 120 },
  { prop: 'lossType', label: '报损类型', minWidth: 100, slot: 'lossType' },
  { prop: 'materialName', label: '物料名称', minWidth: 120 },
  { prop: 'quantity', label: '报损数量', minWidth: 90, align: 'center' },
  { prop: 'totalLossAmount', label: '报损金额', minWidth: 100, align: 'right' },
  { prop: 'reason', label: '报损原因', minWidth: 150, showOverflowTooltip: true },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const params: InventoryLossQueryForm = {
      lossType: searchForm.value.lossType || undefined,
      status: searchForm.value.status || undefined,
      warehouseId: searchForm.value.warehouseId || undefined,
      lossCode: searchForm.value.keyword || undefined,
      page: pagination.current,
      size: pagination.size,
    }
    const res = await inventoryLossApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载报损列表失败')
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */
function resetSearchForm() {
  searchForm.value = { lossType: undefined, status: undefined, warehouseId: undefined, keyword: '' }
}

/* ===== 报损类型选项 ===== */
const lossTypeOptions: { label: string; value: LossType }[] = [
  { label: '过期', value: 'expired' },
  { label: '损坏', value: 'damaged' },
  { label: '丢失', value: 'lost' },
  { label: '其他', value: 'other' },
]

/* ===== 报损状态选项 ===== */
const lossStatusOptions: { label: string; value: LossStatus }[] = [
  { label: '待审批', value: 'pending' },
  { label: '已审批', value: 'approved' },
  { label: '已处理', value: 'processed' },
]

/* ===== 新建报损单 ===== */
const createDialogVisible = ref(false)
const createForm = ref<InventoryLossCreateForm & { materialId: string; quantity: number; reason: string }>({
  warehouseId: '',
  lossType: 'expired',
  materialId: '',
  quantity: 1,
  reason: '',
  remark: '',
  items: [],
})
const createFormRef = ref()
const createFormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  lossType: [{ required: true, message: '请选择报损类型', trigger: 'change' }],
  reason: [{ required: true, message: '请输入报损原因', trigger: 'blur' }],
}

function handleOpenCreate() {
  createForm.value = {
    warehouseId: '',
    lossType: 'expired',
    materialId: '',
    quantity: 1,
    reason: '',
    remark: '',
    items: [],
  }
  createDialogVisible.value = true
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
    if (!createForm.value.reason?.trim()) {
      ElMessage.warning('请输入报损原因')
      return
    }
    const submitData: InventoryLossCreateForm = {
      warehouseId: createForm.value.warehouseId,
      lossType: createForm.value.lossType,
      remark: createForm.value.remark,
      items: [{
        inventoryId: createForm.value.materialId,
        quantity: createForm.value.quantity,
        reason: createForm.value.reason,
      }],
    }
    await inventoryLossApi.create(submitData)
    ElMessage.success('报损单创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '创建失败')
  }
}

/* ===== 审批报损单 ===== */
const approveDialogVisible = ref(false)
const approvingRow = ref<InventoryLossInfo | null>(null)
const approveForm = ref<{ approved: boolean; opinion: string }>({
  approved: true,
  opinion: '',
})

function handleOpenApprove(row: InventoryLossInfo) {
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
    await inventoryLossApi.approve(approvingRow.value.lossId, {
      approved: approveForm.value.approved,
      remark: approveForm.value.opinion,
    })
    ElMessage.success(approveForm.value.approved ? '审批通过' : '已驳回')
    approveDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  }
}

/* ===== 处理报损 ===== */
async function handleProcess(row: InventoryLossInfo) {
  try {
    await ElMessageBox.confirm('确认处理该报损单？处理后库存将扣减。', '处理确认', {
      confirmButtonText: '确认处理',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await inventoryLossApi.process(row.lossId)
    ElMessage.success('报损处理成功')
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '处理失败')
    }
  }
}

/* ===== 查看详情 ===== */
const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryLossInfo | null>(null)

async function handleViewDetail(row: InventoryLossInfo) {
  try {
    const res = await inventoryLossApi.getById(row.lossId)
    currentDetail.value = res
    detailDialogVisible.value = true
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载详情失败')
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
    <PageHeader title="库存报损" description="食材过期、损坏、丢失等报损管理与审批">
      <el-button type="primary" size="default" @click="handleOpenCreate">
        <el-icon :size="16"><Plus /></el-icon>新建报损
      </el-button>
    </PageHeader>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.lossType" placeholder="报损类型" clearable style="width:130px" size="default">
            <el-option v-for="opt in lossTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.status" placeholder="报损状态" clearable style="width:130px" size="default">
            <el-option v-for="opt in lossStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.warehouseId" placeholder="仓库" clearable style="width:130px" size="default">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="搜索报损单号" clearable style="width:200px" size="default" @keyup.enter="stdSearch">
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
        <template #lossType="{ row }">
          <StatusTag :status="inventoryLossConverter.toTypeStatusTagStatus(row.lossType)" :label="inventoryLossConverter.toTypeLabel(row.lossType)" size="small" />
        </template>
        <template #status="{ row }">
          <StatusTag :status="inventoryLossConverter.toStatusTagStatus(row.status)" :label="inventoryLossConverter.toStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">查看</el-button>
          <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleOpenApprove(row)">审批</el-button>
          <el-button v-if="row.status === 'approved'" link type="primary" size="small" @click="handleProcess(row)">处理</el-button>
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

    <!-- 新建报损单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建报损单" width="680px" class="fts-dialog--md" destroy-on-close lock-scroll="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="90px">
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="createForm.warehouseId" placeholder="请选择仓库" :teleported="false" style="width:100%">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="报损类型" prop="lossType">
          <el-select v-model="createForm.lossType" placeholder="请选择报损类型" :teleported="false" style="width:100%">
            <el-option v-for="opt in lossTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="物料" prop="materialId">
          <el-select v-model="createForm.materialId" filterable allow-create default-first-option placeholder="搜索或输入物料" :teleported="false" style="width:100%">
            <el-option label="手动输入" value="" disabled />
          </el-select>
        </el-form-item>
        <el-form-item label="报损数量">
          <el-input-number v-model="createForm.quantity" :min="1" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="报损原因" prop="reason">
          <el-input v-model="createForm.reason" type="textarea" :rows="3" placeholder="请输入报损原因" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批报损单弹窗 -->
    <el-dialog v-model="approveDialogVisible" title="审批报损单" width="480px" class="fts-dialog--sm" destroy-on-close lock-scroll="false">
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

    <!-- 报损详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="报损单详情" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报损单号">{{ currentDetail.lossCode }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ currentDetail.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="报损类型">
            <StatusTag :status="inventoryLossConverter.toTypeStatusTagStatus(currentDetail.lossType)" :label="inventoryLossConverter.toTypeLabel(currentDetail.lossType)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="inventoryLossConverter.toStatusTagStatus(currentDetail.status)" :label="inventoryLossConverter.toStatusLabel(currentDetail.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="报损金额">{{ currentDetail.totalAmount }}元</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentDetail.applyUserName }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentDetail.applyTime }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ currentDetail.approveUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ currentDetail.approveTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ currentDetail.processTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <!-- 报损明细 -->
        <div v-if="currentDetail.items?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">报损明细</h4>
          <el-table :data="currentDetail.items" border size="small">
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column prop="specification" label="规格" min-width="80" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column prop="unitCost" label="单价(元)" width="90" align="right" />
            <el-table-column prop="totalCost" label="小计(元)" width="90" align="right" />
            <el-table-column prop="reason" label="原因" min-width="100" />
          </el-table>
        </div>
        <!-- 操作记录 -->
        <div style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">操作记录</h4>
          <el-timeline>
            <el-timeline-item
              timestamp="提交报损"
              placement="top"
              type="primary"
              :color="cssPrimary"
            >
              <div>{{ currentDetail.applyUserName }} 提交报损申请</div>
              <div class="record-time">{{ currentDetail.applyTime }}</div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.approveUserName"
              :timestamp="currentDetail.approveTime || ''"
              placement="top"
              :type="currentDetail.status === 'pending' ? 'warning' : 'primary'"
            >
              <div>{{ currentDetail.approveUserName }} 审批{{ currentDetail.status === 'pending' ? '' : '通过' }}</div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.processTime"
              :timestamp="currentDetail.processTime"
              placement="top"
              type="success"
            >
              <div>报损处理完成，库存已扣减</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stats-section { grid-template-columns: repeat(4, 1fr); }
.record-time {
  color: var(--fts-text-secondary);
  font-size: 12px;
  margin-top: 2px;
}
</style>
