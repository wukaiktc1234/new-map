<script setup lang="ts">
/**
 * 到货登记页面
 * 负责采购到货单的列表展示、查询筛选、新增/编辑/关闭/详情操作
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { usePermissionStore } from '@/stores/permission'
import { purchaseArrivalApi, purchaseOrderApi, supplierApi } from '@/api/purchase'
import { useCrudTable } from '@/composables/useCrudTable'
import type { PurchaseArrivalInfo, PurchaseArrivalFormData, PurchaseArrivalUpdateForm, PurchaseArrivalCloseForm, PurchaseArrivalStatus, ReceiverType } from '@/types/purchase-arrival'
import { PurchaseArrivalStatusOptions, ReceiverTypeOptions, ShipmentStatusOptions, TransportModeOptions, VehicleTypeOptions } from '@/types/purchase-arrival'
import type { PurchaseOrderInfo } from '@/types/purchase-order'
import type { SupplierInfo } from '@/types/purchase-supplier'
import type { DataTableColumn } from '@/components/core/DataTable.vue'

import ArrivalFormDialog from './components/ArrivalFormDialog.vue'
import ArrivalCloseDialog from './components/ArrivalCloseDialog.vue'
import ArrivalDetailDialog from './components/ArrivalDetailDialog.vue'

const layoutStore = useLayoutStore()
const permissionStore = usePermissionStore()
const route = useRoute()

interface QueryForm {
  arrivalCode?: string
  orderCode?: string
  supplierId?: string
  status?: PurchaseArrivalStatus | ''
  receiverType?: ReceiverType | ''
  overdueOnly?: boolean
  estimatedStartDate?: string
  estimatedEndDate?: string
  mineOnly?: boolean
}

const queryForm = ref<QueryForm>({
  arrivalCode: '',
  orderCode: '',
  supplierId: '',
  status: '',
  receiverType: '',
  overdueOnly: false,
  estimatedStartDate: '',
  estimatedEndDate: '',
  mineOnly: false,
})

const estimatedDateRange = ref<[string, string] | null>(null)

/**
 * 到货单查询参数（传给 API 层）
 */
interface PurchaseArrivalQueryParams {
  arrivalCode?: string
  orderCode?: string
  status?: PurchaseArrivalStatus | ''
  receiverType?: ReceiverType | ''
  overdueOnly?: boolean
  estimatedStartDate?: string
  estimatedEndDate?: string
  createUserId?: string
  page?: number
  size?: number
}

const { tableData, loading, refresh, pagination } = useCrudTable<PurchaseArrivalInfo, QueryForm>({
  api: {
    getList: async (params) => {
      const query: PurchaseArrivalQueryParams = {
        arrivalCode: params.arrivalCode,
        orderCode: params.orderCode,
        supplierId: params.supplierId,
        status: params.status,
        receiverType: params.receiverType,
        overdueOnly: params.overdueOnly,
        page: params.page,
        size: params.size,
      }
      // 我的单据：按当前登录用户ID过滤
      const currentUserId = String(permissionStore.userInfo?.userId || '')
      if (params.mineOnly && currentUserId) {
        query.createUserId = currentUserId
      }
      if (estimatedDateRange.value) {
        query.estimatedStartDate = estimatedDateRange.value[0]
        query.estimatedEndDate = estimatedDateRange.value[1]
      }
      const res = await purchaseArrivalApi.getList(query)
      return {
        records: res.records,
        total: res.total,
        current: params.page,
        size: params.size,
        pages: Math.ceil(res.total / params.size) || 1,
      }
    },
  },
  queryForm,
  autoLoad: true,
})

// ==================== 统计数据 ====================

const statistics = computed(() => {
  const now = new Date()
  const monthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`

  const pendingCount = tableData.value.filter((item) => item.status === 'pending').length
  const receivingCount = tableData.value.filter((item) => item.status === 'receiving').length
  const overdueCount = tableData.value.filter((item) => isOverdue(item)).length
  const monthCount = tableData.value.filter((item) =>
    item.estimatedArrivalDate ? item.estimatedArrivalDate >= monthStart : false,
  ).length

  return {
    pending: pendingCount,
    receiving: receivingCount,
    overdue: overdueCount,
    month: monthCount,
  }
})

// ==================== 采购订单选项 ====================

const orderOptions = ref<PurchaseOrderInfo[]>([])
const orderOptionsLoading = ref(false)
const supplierOptions = ref<SupplierInfo[]>([])

async function loadOrderOptions(): Promise<void> {
  orderOptionsLoading.value = true
  try {
    const [approvedRes, orderedRes] = await Promise.all([
      purchaseOrderApi.getList({ status: 'approved', page: 1, size: 1000 }),
      purchaseOrderApi.getList({ status: 'ordered', page: 1, size: 1000 }),
    ])
    const list = [...(approvedRes?.records || []), ...(orderedRes?.records || [])]
    const map = new Map<string, PurchaseOrderInfo>()
    for (const order of list) {
      map.set(order.purchaseOrderId, order)
    }
    orderOptions.value = Array.from(map.values())
  } catch {
    orderOptions.value = []
  } finally {
    orderOptionsLoading.value = false
  }
}

async function loadSupplierOptions(): Promise<void> {
  try {
    const res = await supplierApi.getList({ page: 1, size: 1000 })
    supplierOptions.value = res?.records || []
  } catch {
    supplierOptions.value = []
  }
}

// ==================== 表格列定义 ====================

const columns = computed<DataTableColumn[]>(() => [
  { prop: 'arrivalCode', label: '到货单号', minWidth: 160, slot: 'arrivalCode' },
  { prop: 'orderCode', label: '采购订单号', minWidth: 150, slot: 'orderCode' },
  { prop: 'supplierName', label: '供应商', minWidth: 130, slot: 'supplierName' },
  { prop: 'receiverType', label: '收货方类型', minWidth: 140, slot: 'receiverType' },
  { prop: 'shipmentStatus', label: '发货状态', minWidth: 100, slot: 'shipmentStatus' },
  { prop: 'logisticsNo', label: '物流单号', minWidth: 130, slot: 'logisticsNo' },
  { prop: 'logisticsCompany', label: '物流公司', minWidth: 130, slot: 'logisticsCompany' },
  { prop: 'transportMode', label: '运输方式', minWidth: 100, slot: 'transportMode' },
  { prop: 'vehicleInfo', label: '车辆信息', minWidth: 160, slot: 'vehicleInfo' },
  { prop: 'estimatedArrivalDate', label: '预计到货日期', minWidth: 130, slot: 'estimatedArrivalDate' },
  { prop: 'quantity', label: '到货数量/已收数量', minWidth: 150, slot: 'quantity', align: 'right' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
])

// ==================== 对话框状态 ====================

const formDialogVisible = ref(false)
const formDialogMode = ref<'create' | 'edit'>('create')
const editingArrival = ref<PurchaseArrivalInfo | null>(null)
const initialOrderId = ref('')
const submitLoading = ref(false)

const closeDialogVisible = ref(false)
const closingArrival = ref<PurchaseArrivalInfo | null>(null)

const detailDialogVisible = ref(false)
const detailData = ref<PurchaseArrivalInfo | null>(null)

/** 最近操作提示 */
const recentRecords = ref<{ arrivalId: string; arrivalCode: string; action: string; time: number }[]>([])
const recentTipVisible = ref(true)
const RECENT_KEY = 'purchase-arrival:recent'

/** 当前高亮的到货单ID */
const highlightId = ref('')
const highlightTimer = ref<number | null>(null)

// ==================== 辅助方法 ====================

/** 加载最近操作记录 */
function loadRecentRecords(): void {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as { arrivalId: string; arrivalCode: string; action: string; time: number }[]
      recentRecords.value = parsed.slice(0, 3)
    }
  } catch {
    recentRecords.value = []
  }
}

/** 记录最近操作 */
function pushRecentRecord(row: PurchaseArrivalInfo, action: string): void {
  if (!row?.arrivalId) return
  const record = {
    arrivalId: row.arrivalId,
    arrivalCode: row.arrivalCode,
    action,
    time: Date.now(),
  }
  const list = [record, ...recentRecords.value.filter(r => r.arrivalId !== record.arrivalId)]
  recentRecords.value = list.slice(0, 3)
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(recentRecords.value))
  } catch {
    // ignore storage error
  }
  setHighlight(row.arrivalId)
}

/** 设置高亮行 */
function setHighlight(arrivalId: string): void {
  highlightId.value = arrivalId
  if (highlightTimer.value) {
    window.clearTimeout(highlightTimer.value)
  }
  highlightTimer.value = window.setTimeout(() => {
    highlightId.value = ''
  }, 5000)
}

/** 行类名：高亮最近操作的行 */
function rowClassName({ row }: { row: PurchaseArrivalInfo; rowIndex: number }): string {
  if (row.arrivalId === highlightId.value) {
    return 'purchase-arrival-row--highlight'
  }
  return ''
}

/** 关闭最近操作提示 */
function closeRecentTip(): void {
  recentTipVisible.value = false
}

/** 定位到最近操作的到货单 */
function locateRecent(arrivalId: string): void {
  setHighlight(arrivalId)
  const row = tableData.value.find(item => item.arrivalId === arrivalId)
  if (row) {
    handleView(row)
  } else {
    queryForm.value.arrivalCode = recentRecords.value.find(r => r.arrivalId === arrivalId)?.arrivalCode || ''
    refresh()
  }
}

function isOverdue(row: PurchaseArrivalInfo): boolean {
  if (row.overdue) return true
  if (!row.estimatedArrivalDate) return false
  if (row.status === 'received' || row.status === 'closed') return false
  return row.estimatedArrivalDate < new Date().toISOString().slice(0, 10)
}

function getStatusConfig(status: PurchaseArrivalStatus): { status: string; label: string } {
  const map: Record<string, { status: string; label: string }> = {
    pending: { status: 'pending', label: '待收货' },
    receiving: { status: 'pending', label: '收货中' },
    partial_received: { status: 'warning', label: '部分收货' },
    received: { status: 'success', label: '已收货' },
    closed: { status: 'inactive', label: '已关闭' },
  }
  return map[status] ?? { status: 'info', label: status }
}

function getReceiverTypeLabel(type: ReceiverType): string {
  return ReceiverTypeOptions.find((o) => o.value === type)?.label || type
}

function getShipmentStatusLabel(status?: string): string {
  if (!status) return '-'
  return ShipmentStatusOptions.find((o) => o.value === status)?.label || status
}

function getTransportModeLabel(mode?: string): string {
  if (!mode) return '-'
  return TransportModeOptions.find((o) => o.value === mode)?.label || mode
}

function getVehicleTypeLabel(type?: string): string {
  if (!type) return '-'
  return VehicleTypeOptions.find((o) => o.value === type)?.label || type
}

function getReceiverName(row: PurchaseArrivalInfo): string {
  const typeLabel = getReceiverTypeLabel(row.receiverType)
  const name = row.storeId || row.warehouseId || ''
  return name ? `${typeLabel}(${name})` : typeLabel
}

function formatDate(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function canEditLogistics(row: PurchaseArrivalInfo): boolean {
  return row.status === 'pending' || row.status === 'receiving' || row.status === 'partial_received'
}

function canClose(row: PurchaseArrivalInfo): boolean {
  return row.status === 'pending'
}

// ==================== 搜索与重置 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value = {
    arrivalCode: '',
    orderCode: '',
    supplierId: '',
    status: '',
    receiverType: '',
    overdueOnly: false,
    estimatedStartDate: '',
    estimatedEndDate: '',
    mineOnly: false,
  }
  estimatedDateRange.value = null
  handleSearch()
}

// ==================== 新增/编辑/关闭/详情 ====================

function handleCreate() {
  formDialogMode.value = 'create'
  editingArrival.value = null
  initialOrderId.value = ''
  formDialogVisible.value = true
}

function handleEditLogistics(row: PurchaseArrivalInfo) {
  formDialogMode.value = 'edit'
  editingArrival.value = row
  formDialogVisible.value = true
}

function handleCloseDialog(row: PurchaseArrivalInfo) {
  closingArrival.value = row
  closeDialogVisible.value = true
}

async function handleView(row: PurchaseArrivalInfo) {
  try {
    const res = await purchaseArrivalApi.getById(row.arrivalId)
    if (res) {
      detailData.value = res
      detailDialogVisible.value = true
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
}

/** 详情内质检/确认入库后刷新详情与列表 */
async function handleDetailRefresh(): Promise<void> {
  if (detailData.value) {
    const res = await purchaseArrivalApi.getById(detailData.value.arrivalId).catch(() => null)
    if (res) {
      detailData.value = res
    }
  }
  refresh()
}

async function handleCreateSubmit(data: PurchaseArrivalFormData) {
  submitLoading.value = true
  try {
    const created = await purchaseArrivalApi.createFromOrder(data)
    ElMessage.success('到货单创建成功')
    formDialogVisible.value = false
    await refresh()
    if (created.length > 0) {
      pushRecentRecord(created[0], '新增到货单')
    }
  } catch {
    ElMessage.error('创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleUpdateSubmit(data: PurchaseArrivalUpdateForm) {
  if (!editingArrival.value) return
  submitLoading.value = true
  try {
    await purchaseArrivalApi.update(editingArrival.value.arrivalId, data)
    ElMessage.success('物流信息更新成功')
    formDialogVisible.value = false
    pushRecentRecord(editingArrival.value, '编辑物流')
    refresh()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleCloseSubmit(data: PurchaseArrivalCloseForm) {
  if (!closingArrival.value) return
  submitLoading.value = true
  try {
    await purchaseArrivalApi.close(closingArrival.value.arrivalId, data)
    ElMessage.success('到货单已关闭')
    closeDialogVisible.value = false
    pushRecentRecord(closingArrival.value, '关闭')
    refresh()
  } catch {
    ElMessage.error('关闭失败')
  } finally {
    submitLoading.value = false
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadOrderOptions()
  loadSupplierOptions()
  loadRecentRecords()

  // 处理从采购订单页跳转过来的 orderId：自动打开新增到货单并选中该订单
  const routeOrderId = route.query.orderId as string
  if (routeOrderId) {
    initialOrderId.value = routeOrderId
    // 等待订单选项加载完成后再打开对话框
    const unwatch = watch(orderOptionsLoading, (loading) => {
      if (!loading) {
        const order = orderOptions.value.find(o => o.purchaseOrderId === routeOrderId)
        if (order) {
          formDialogMode.value = 'create'
          formDialogVisible.value = true
        } else {
          ElMessage.warning('未找到可到货登记的采购订单')
        }
        unwatch()
      }
    }, { immediate: true })
  }
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="到货登记" description="管理采购到货单的登记、物流跟踪与状态维护">
      <el-button v-permission="'purchase:stockin:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增到货单
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard icon="Clock" label="待收货" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Box" label="收货中" :value="String(statistics.receiving)" color-type="primary" variant="bordered" />
      <StatCard icon="Warning" label="超期到货" :value="String(statistics.overdue)" color-type="error" variant="bordered" />
      <StatCard icon="Calendar" label="本月到货" :value="String(statistics.month)" color-type="success" variant="bordered" />
    </section>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.arrivalCode"
            placeholder="到货单号"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.orderCode"
            placeholder="采购订单号"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.supplierId"
            placeholder="供应商"
            clearable
            filterable
            style="width: 150px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="s in supplierOptions"
              :key="s.supplierId"
              :label="s.supplierName"
              :value="s.supplierId"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in PurchaseArrivalStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.receiverType"
            placeholder="收货方类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in ReceiverTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-checkbox v-model="queryForm.overdueOnly" @change="handleSearch">仅看超期</el-checkbox>
          <el-checkbox v-model="queryForm.mineOnly" size="default" @change="handleSearch">我的单据</el-checkbox>
          <el-date-picker
            v-model="estimatedDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            placement="bottom-start"
            style="width: 240px"
            size="default"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="recentRecords.length > 0 && recentTipVisible" class="recent-tip">
      <div class="recent-tip__content">
        <span class="recent-tip__label">最近操作：</span>
        <el-button
          v-for="record in recentRecords"
          :key="record.arrivalId"
          link
          type="primary"
          size="small"
          @click="locateRecent(record.arrivalId)"
        >
          {{ record.arrivalCode }}（{{ record.action }}）
        </el-button>
      </div>
      <el-button link type="info" size="small" @click="closeRecentTip">收起</el-button>
    </div>

    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="true"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        :actions-width="200"
        :row-class-name="rowClassName"
      >
        <template #arrivalCode="{ row }">
          <span class="arrival-code-text">{{ row.arrivalCode || '-' }}</span>
        </template>

        <template #orderCode="{ row }">
          <span class="order-code-text">{{ row.orderCode || '-' }}</span>
        </template>

        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName || '-' }}</span>
        </template>

        <template #receiverType="{ row }">
          <span class="receiver-text">{{ getReceiverName(row) }}</span>
        </template>

        <template #shipmentStatus="{ row }">
          <span class="shipment-text">{{ getShipmentStatusLabel(row.shipmentStatus) }}</span>
        </template>

        <template #logisticsNo="{ row }">
          <span class="logistics-text">{{ row.logisticsNo || '-' }}</span>
        </template>

        <template #logisticsCompany="{ row }">
          <span class="logistics-text">{{ row.logisticsCompany || '-' }}</span>
        </template>

        <template #transportMode="{ row }">
          <span class="logistics-text">{{ getTransportModeLabel(row.transportMode) }}</span>
        </template>

        <template #vehicleInfo="{ row }">
          <span class="logistics-text">
            {{ row.vehiclePlateNo || getVehicleTypeLabel(row.vehicleType) || '-' }}
            <template v-if="row.vehiclePlateNo && row.vehicleType">
              （{{ getVehicleTypeLabel(row.vehicleType) }}）
            </template>
          </span>
        </template>

        <template #estimatedArrivalDate="{ row }">
          <span :class="['date-text', { 'date-text--overdue': isOverdue(row) }]">
            {{ formatDate(row.estimatedArrivalDate) }}
          </span>
        </template>

        <template #quantity="{ row }">
          <span class="quantity-text">{{ row.totalQuantity }} / {{ row.receivedQuantity }}</span>
        </template>

        <template #status="{ row }">
          <StatusTag
            :status="getStatusConfig(row.status).status"
            :label="getStatusConfig(row.status).label"
            size="small"
          />
        </template>

        <template #actions="{ row }">
          <div class="action-text">
            <el-button v-permission="'purchase:stockin:view'" link type="primary" size="small" @click.stop="handleView(row)">详情</el-button>
            <el-button
              v-if="canEditLogistics(row)"
              v-permission="'purchase:stockin:edit'"
              link
              type="primary"
              size="small"
              @click.stop="handleEditLogistics(row)"
            >
              编辑物流
            </el-button>
            <el-button
              v-if="canClose(row)"
              v-permission="'purchase:stockin:confirm'"
              link
              type="danger"
              size="small"
              @click.stop="handleCloseDialog(row)"
            >
              关闭
            </el-button>
          </div>
        </template>
      </DataTable>

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

    <ArrivalFormDialog
      v-model="formDialogVisible"
      :mode="formDialogMode"
      :initial-data="editingArrival"
      :initial-order-id="initialOrderId"
      :order-options="orderOptions"
      :order-options-loading="orderOptionsLoading"
      @create="handleCreateSubmit"
      @update="handleUpdateSubmit"
    />

    <ArrivalCloseDialog
      v-model="closeDialogVisible"
      :arrival-id="closingArrival?.arrivalId || ''"
      :arrival-code="closingArrival?.arrivalCode || ''"
      @submit="handleCloseSubmit"
    />

      <ArrivalDetailDialog
        v-model="detailDialogVisible"
        :data="detailData"
        @refresh="handleDetailRefresh"
      />
  </div>
</template>

<style scoped lang="scss">
// 最近操作提示条
.recent-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-6);
  background: var(--fts-primary-bg, #ecf5ff);
  border: 1px solid var(--fts-primary-light, #d9ecff);
  border-top: none;

  &__content {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-wrap: wrap;
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }
}

// 表格高亮行（重复类名提升特异性，避免使用 !important）
:deep(.el-table__row.purchase-arrival-row--highlight.purchase-arrival-row--highlight) {
  td {
    background-color: var(--fts-primary-bg, #ecf5ff);
    animation: row-pulse 2s ease-in-out;
  }
}

@keyframes row-pulse {
  0% {
    background-color: var(--fts-primary-light, #d9ecff);
  }
  100% {
    background-color: var(--fts-primary-bg, #ecf5ff);
  }
}

// 文本样式
.arrival-code-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.order-code-text,
.supplier-text,
.receiver-text,
.shipment-text,
.logistics-text {
  color: var(--fts-text-primary);
}

.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);

  &--overdue {
    color: var(--fts-error);
    font-weight: var(--fts-font-weight-semibold);
  }
}

.quantity-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}
</style>
