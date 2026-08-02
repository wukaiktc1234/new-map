<script setup lang="ts">
/**
 * 采购订单管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理所有采购订单的审批、跟踪和状态
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Document } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import ApprovalDialog from '@/components/business/ApprovalDialog.vue'
import ApprovalRecordCompact from '@/components/business/ApprovalRecordCompact.vue'
import RequestDetailDialog from './components/RequestDetailDialog.vue'
import ArrivalDetailDialog from './components/ArrivalDetailDialog.vue'
import PurchaseOrderFormDialog from './components/PurchaseOrderFormDialog.vue'
import { useLayoutStore } from '@/stores/layout'
import { usePermissionStore } from '@/stores/permission'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { purchaseOrderApi, purchaseRequestApi, supplierApi } from '@/api/purchase'
import { purchaseArrivalApi } from '@/api/purchase/arrival'
import { approvalWorkflowApi } from '@/api/approval'
import { purchaseOrderConverter, supplierConverter, purchaseRequestConverter } from '@/api/purchase/converters'
import type {
  PurchaseOrderInfo,
  PurchaseOrderQueryForm,
  PurchaseOrderStatus,
  PaymentStatus,
} from '@/types/purchase-order'
import type { PurchaseRequestInfo } from '@/types/purchase-request'
import type { SupplierInfo } from '@/types/purchase-supplier'
import type { PurchaseArrivalInfo } from '@/types/purchase-arrival'
import { PurchaseOrderStatusOptions, PaymentStatusOptions } from '@/types/purchase-order'

const layoutStore = useLayoutStore()
const permissionStore = usePermissionStore()
const router = useRouter()

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

// ==================== 响应式数据 ====================

const selectedRows = ref<PurchaseOrderInfo[]>([])

/** 详情对话框显隐 */
const detailDialogVisible = ref(false)
/** 当前查看的订单详情 */
const currentDetail = ref<PurchaseOrderInfo | null>(null)
/** 详情对话框当前 Tab */
const detailTab = ref('basic')

/** 采购申请详情弹窗（步骤条"采购申请"节点复用申请页原对话框组件） */
const requestRefVisible = ref(false)
const requestRefId = ref('')

/** 到货单详情弹窗（步骤条"到货登记"节点复用到货登记页原对话框组件） */
const arrivalRefVisible = ref(false)
const arrivalRefData = ref<PurchaseArrivalInfo | null>(null)

/** 审批对话框状态 */
const approvalDialogVisible = ref(false)
const approvalBusinessId = ref('')
const approvalBusinessNo = ref('')
const approvalBusinessType = ref('purchase_order')
const approvalNodeName = ref('')

/** 从采购申请生成对话框状态 */
const showRequestDialog = ref(false)
const approvedRequests = ref<PurchaseRequestInfo[]>([])
const selectedRequests = ref<PurchaseRequestInfo[]>([])

/** 采购订单表单对话框 ref */
const orderFormDialogRef = ref<InstanceType<typeof PurchaseOrderFormDialog>>()

// ==================== 查询表单 ====================

const queryForm = ref({
  orderNo: '',
  requestNo: '',
  status: '' as '' | PurchaseOrderStatus,
  paymentStatus: '' as '' | PaymentStatus,
  startDate: '',
  endDate: '',
  mineOnly: false,
})

/** 当前登录用户ID */
const currentUserId = computed(() => String(permissionStore.userInfo?.userId || ''))

/** 最近操作提示 */
const recentRecords = ref<{ orderId: string; orderNo: string; action: string; time: number }[]>([])
const recentTipVisible = ref(true)
const RECENT_KEY = 'purchase-order:recent'

/** 当前高亮的订单ID（用于操作后定位） */
const highlightId = ref('')
const highlightTimer = ref<number | null>(null)

// ==================== 使用 useCrudTable 管理表格数据 ====================

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<PurchaseOrderInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: PurchaseOrderQueryForm & { page?: number; size?: number; mineOnly?: boolean } = {
        ...params,
      }
      // 空字符串转为 undefined，避免传给后端
      if (convertedParams.orderNo === '') {
        delete convertedParams.orderNo
      }
      if (convertedParams.requestNo === '') {
        delete convertedParams.requestNo
      }
      if (convertedParams.status === '') {
        convertedParams.status = null
      }
      if (convertedParams.paymentStatus === '') {
        convertedParams.paymentStatus = null
      }
      if (convertedParams.startDate === '') {
        delete convertedParams.startDate
      }
      if (convertedParams.endDate === '') {
        delete convertedParams.endDate
      }
      // 我的单据：按当前登录用户ID过滤
      if (convertedParams.mineOnly && currentUserId.value) {
        convertedParams.createUserId = currentUserId.value
      } else {
        delete (convertedParams as { createUserId?: string }).createUserId
      }
      delete (convertedParams as { mineOnly?: boolean }).mineOnly
      return purchaseOrderApi.getList(convertedParams)
    },
  } as unknown as CrudApi<PurchaseOrderInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// ==================== 供应商选项 ====================

const supplierOptions = ref<SupplierInfo[]>([])

/** 加载供应商选项列表（仅查询正常状态的供应商） */
async function loadSupplierOptions(): Promise<void> {
  try {
    const res = await supplierApi.getList({ status: 'active', page: 1, size: 1000 })
    supplierOptions.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载供应商列表失败')
    }
  }
}

// ==================== 统计卡片 ====================

const statistics = computed(() => ({
  total: pagination?.total || 0,
  pending: tableData.value.filter(item => item.status === 'pending').length,
  ordered: tableData.value.filter(item => item.status === 'ordered').length,
  completed: tableData.value.filter(item => item.status === 'completed').length,
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'orderNo', label: '订单编号', minWidth: 150, slot: 'orderNo' },
  { prop: 'requestNo', label: '来源采购申请', minWidth: 150, slot: 'requestNo' },
  { prop: 'supplierName', label: '供应商', minWidth: 150, slot: 'supplierName' },
  { prop: 'createByName', label: '创建人', minWidth: 100, slot: 'createByName' },
  { prop: 'status', label: '订单状态', minWidth: 100, slot: 'status' },
  { prop: 'paymentStatus', label: '付款状态', minWidth: 100, slot: 'paymentStatus' },
  { prop: 'totalAmount', label: '总金额(元)', minWidth: 110, align: 'right', slot: 'totalAmount' },
  { prop: 'orderDate', label: '下单日期', minWidth: 110, slot: 'orderDate' },
  { prop: 'expectedDate', label: '预计到货日期', minWidth: 120, slot: 'expectedDate' },
  { prop: 'createTime', label: '创建时间', minWidth: 160, slot: 'createTime' },
  { prop: '_operation', label: '操作', width: 280, fixed: 'right', slot: 'operation' },
])

// ==================== 搜索/重置方法 ====================

function handleSearch(): void {
  refresh()
}

function handleReset(): void {
  queryForm.value.orderNo = ''
  queryForm.value.status = ''
  queryForm.value.paymentStatus = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  queryForm.value.mineOnly = false
  refresh()
}

// ==================== 表格选择 ====================

function handleSelectionChange(rows: PurchaseOrderInfo[]): void {
  selectedRows.value = rows
}

// ==================== 新建/编辑 ====================

function handleCreate(): void {
  orderFormDialogRef.value?.create()
}

function handleEdit(row: PurchaseOrderInfo): void {
  orderFormDialogRef.value?.edit(row.purchaseOrderId)
}

// ==================== 查看详情 ====================

async function handleView(row: PurchaseOrderInfo): Promise<void> {
  try {
    const detail = await purchaseOrderApi.getById(row.purchaseOrderId)
    if (detail) {
      currentDetail.value = detail
      detailTab.value = 'basic'
      detailDialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

/** 订单表单保存成功：刷新列表；若详情打开则同步刷新详情数据 */
async function handleOrderFormSuccess(): Promise<void> {
  await refresh()
  await reloadDetail()
}

/** 详情打开时重新加载当前订单详情（详情内操作后状态同步） */
async function reloadDetail(): Promise<void> {
  if (detailDialogVisible.value && currentDetail.value) {
    const detail = await purchaseOrderApi.getById(currentDetail.value.purchaseOrderId).catch(() => null)
    if (detail) {
      currentDetail.value = detail
    }
  }
}

// ==================== 提交审批 ====================

async function handleSubmitApproval(row: PurchaseOrderInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要提交订单「${row.orderNo}」审批吗？`,
      '提交审批',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' },
    )
    // 先变更订单状态为待审核，再写入审批提交记录
    await purchaseOrderApi.submit(row.purchaseOrderId)
    await approvalWorkflowApi.submitApproval('purchase_order', row.purchaseOrderId)
    ElMessage.success('提交成功')
    pushRecentRecord(row, '提交审批')
    refresh()
    await reloadDetail()
  } catch {
    // 用户取消
  }
}

/** 跳转至采购收货页，自动带入当前订单 */
function handleStockin(row: PurchaseOrderInfo): void {
  pushRecentRecord(row, '跳转收货')
  router.push({ path: '/purchase/stockin', query: { orderId: row.purchaseOrderId } })
}

/** 单据链互动：来源采购申请 → 弹出关联单据详情（不跳转页面） */
function openRequestDetail(): void {
  if (!currentDetail.value?.requestId) return
  requestRefId.value = currentDetail.value.requestId
  requestRefVisible.value = true
}

/** 列表驳回：审批者发现问题直接驳回并附原因（通知申请者） */
async function handleRejectFromList(row: PurchaseOrderInfo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写驳回「${row.orderNo}」的原因`,
      '驳回订单',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '驳回原因将通知申请人',
      },
    )
    await purchaseOrderApi.reject(row.purchaseOrderId, value)
    ElMessage.success('已驳回')
    pushRecentRecord(row, '驳回')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '驳回失败')
    }
  }
}

// ==================== 审批 ====================

function handleApprove(row: PurchaseOrderInfo): void {
  approvalBusinessId.value = row.purchaseOrderId
  approvalBusinessNo.value = row.orderNo
  approvalBusinessType.value = 'purchase_order'
  approvalNodeName.value = '审批'
  approvalDialogVisible.value = true
}

/** 审批通过（真实业务 API，替代 mock 回退） */
async function handleApproveOrder(comment: string): Promise<void> {
  await purchaseOrderApi.approve(approvalBusinessId.value, comment)
}

/** 审批驳回（真实业务 API） */
async function handleRejectOrder(comment: string): Promise<void> {
  await purchaseOrderApi.reject(approvalBusinessId.value, comment)
}

function onApproved(): void {
  approvalDialogVisible.value = false
  const row = tableData.value.find(item => item.purchaseOrderId === approvalBusinessId.value)
  if (row) pushRecentRecord(row, '审批通过')
  refresh()
  // 详情打开时同步刷新详情状态（不再强制关闭）
  reloadDetail()
}

function onRejected(): void {
  approvalDialogVisible.value = false
  const row = tableData.value.find(item => item.purchaseOrderId === approvalBusinessId.value)
  if (row) pushRecentRecord(row, '审批驳回')
  refresh()
  if (detailDialogVisible.value) {
    reloadDetail()
  }
}

// ==================== 删除 ====================

async function handleDelete(row: PurchaseOrderInfo): Promise<void> {
  // 只有草稿状态可以删除
  if (row.status !== 'draft') {
    ElMessage.warning(`「${purchaseOrderConverter.toStatusLabel(row.status)}」状态的订单不可删除`)
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除草稿订单「${row.orderNo}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
    await purchaseOrderApi.delete(row.purchaseOrderId)
    ElMessage.success('删除成功')
    pushRecentRecord(row, '删除')
    refresh()
  } catch {
    // 用户取消
  }
}

// ==================== 终止订单 ====================

async function handleTerminate(row: PurchaseOrderInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要终止订单「${row.orderNo}」吗？终止后将取消未执行的采购内容，已收货部分仍需结算。`,
      '终止订单 - 风险提示',
      { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' },
    )
    const { value: reason } = await ElMessageBox.prompt('请输入终止原因', '终止原因', {
      confirmButtonText: '确定终止',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '终止原因不能为空',
    })
    await purchaseOrderApi.cancel(row.purchaseOrderId, reason)
    ElMessage.success('订单已终止')
    pushRecentRecord(row, '终止')
    refresh()
  } catch {
    // 用户取消
  }
}

// ==================== 详情页操作 ====================

function handleApproveFromDetail(): void {
  if (!currentDetail.value) return
  approvalBusinessId.value = currentDetail.value.purchaseOrderId
  approvalBusinessNo.value = currentDetail.value.orderNo
  approvalBusinessType.value = 'purchase_order'
  approvalNodeName.value = '审批'
  approvalDialogVisible.value = true
}

async function handleRejectFromDetail(): Promise<void> {
  if (!currentDetail.value) return
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回订单', {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '驳回原因不能为空',
    })
    await approvalWorkflowApi.reject({
      businessId: currentDetail.value.purchaseOrderId,
      businessType: 'purchase_order',
      comment: reason,
    })
    ElMessage.success('已驳回')
    pushRecentRecord(currentDetail.value, '审批驳回')
    detailDialogVisible.value = false
    refresh()
  } catch {
    // 用户取消
  }
}

async function handleConfirmOrder(): Promise<void> {
  if (!currentDetail.value) return
  try {
    await ElMessageBox.confirm(
      '确认下单后将通知供应商备货，确定继续？',
      '确认下单',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' },
    )
    await purchaseOrderApi.confirmOrder(currentDetail.value.purchaseOrderId)
    ElMessage.success('确认下单成功')
    pushRecentRecord(currentDetail.value, '确认下单')
    detailDialogVisible.value = false
    refresh()
  } catch {
    // 用户取消
  }
}

async function handleDeleteFromDetail(): Promise<void> {
  if (!currentDetail.value) return
  await handleDelete(currentDetail.value)
  detailDialogVisible.value = false
}

// ==================== 从采购申请生成 ====================

async function handleGenerateFromRequest(): Promise<void> {
  showRequestDialog.value = true
  try {
    const res = await purchaseRequestApi.getList({ status: 'approved', page: 1, size: 100 })
    approvedRequests.value = res.records
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载采购申请失败')
    }
  }
}

const selectedTotal = computed(() =>
  selectedRequests.value.reduce((sum, r) => sum + (Number(r.totalAmount) || 0), 0),)

function handleRequestSelectionChange(selections: PurchaseRequestInfo[]): void {
  selectedRequests.value = selections
}

async function confirmGenerateFromRequest(): Promise<void> {
  if (selectedRequests.value.length === 0) {
    ElMessage.warning('请至少选择一条采购申请')
    return
  }

  // 使用后端 generate-order 接口逐条转单，保持申请→订单的追溯关系
  // 并自动将采购申请状态置为 completed
  const successNos: string[] = []
  const failedNos: string[] = []
  for (const req of selectedRequests.value) {
    try {
      await purchaseRequestApi.generateOrder(req.requestId)
      successNos.push(req.requestNo)
    } catch (error: unknown) {
      failedNos.push(req.requestNo)
      if (error instanceof Error) {
        ElMessage.error(`申请 ${req.requestNo} 转单失败：${error.message}`)
      }
    }
  }

  // 保存最后一条成功转单的申请编号，用于刷新后定位
  const lastSuccessRequestNo = successNos.length > 0 ? successNos[successNos.length - 1] : ''
  showRequestDialog.value = false
  selectedRequests.value = []

  if (successNos.length > 0) {
    ElMessage.success(`已成功生成 ${successNos.length} 条采购订单：${successNos.join('、')}`)
    await refresh()
    // 刷新后根据申请编号定位生成的订单
    if (lastSuccessRequestNo) {
      const target = tableData.value.find(item => item.requestNo === lastSuccessRequestNo)
      if (target) pushRecentRecord(target, '从申请生成')
    }
  }
  if (failedNos.length > 0) {
    ElMessage.warning(`以下申请转单失败：${failedNos.join('、')}`)
  }
}

// ==================== 单号查找体验优化辅助方法 ====================

/** 加载最近操作记录 */
function loadRecentRecords(): void {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as { orderId: string; orderNo: string; action: string; time: number }[]
      recentRecords.value = parsed.slice(0, 3)
    }
  } catch {
    recentRecords.value = []
  }
}

/** 记录最近操作 */
function pushRecentRecord(row: PurchaseOrderInfo, action: string): void {
  if (!row?.purchaseOrderId) return
  const record = {
    orderId: row.purchaseOrderId,
    orderNo: row.orderNo,
    action,
    time: Date.now(),
  }
  const list = [record, ...recentRecords.value.filter(r => r.orderId !== record.orderId)]
  recentRecords.value = list.slice(0, 3)
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(recentRecords.value))
  } catch {
    // ignore storage error
  }
  setHighlight(row.purchaseOrderId)
}

/** 设置高亮行 */
function setHighlight(orderId: string): void {
  highlightId.value = orderId
  if (highlightTimer.value) {
    window.clearTimeout(highlightTimer.value)
  }
  highlightTimer.value = window.setTimeout(() => {
    highlightId.value = ''
  }, 5000)
}

/** 行类名：高亮最近操作的行 */
function rowClassName({ row }: { row: PurchaseOrderInfo; rowIndex: number }): string {
  if (row.purchaseOrderId === highlightId.value) {
    return 'purchase-order-row--highlight'
  }
  return ''
}

/** 关闭最近操作提示 */
function closeRecentTip(): void {
  recentTipVisible.value = false
}

/** 定位到最近操作的订单 */
function locateRecent(orderId: string): void {
  setHighlight(orderId)
  const row = tableData.value.find(item => item.purchaseOrderId === orderId)
  if (row) {
    handleView(row)
  } else {
    queryForm.value.orderNo = recentRecords.value.find(r => r.orderId === orderId)?.orderNo || ''
    refresh()
  }
}

// ==================== 辅助方法 ====================

/** 详情对话框中的供应商信息 */
const detailSupplier = computed(() => {
  if (!currentDetail.value?.supplierId) return null
  return supplierOptions.value.find(s => s.supplierId === currentDetail.value!.supplierId) || null
})

/** 采购流程步骤条数据 */
interface ProcessStep {
  key: string
  label: string
  completed: boolean
  active: boolean
  clickable: boolean
  badge?: string
  route?: { path: string; query?: Record<string, string> }
}

const processSteps = computed((): ProcessStep[] => {
  const detail = currentDetail.value
  if (!detail) return []
  const status = detail.status
  const requestNo = detail.requestNo
  const orderNo = detail.orderNo

  const completedStatuses = ['approved', 'ordered', 'shipped', 'received', 'partial_received', 'completed']
  const receivedStatuses = ['received', 'completed']
  const inTransitStatuses = ['ordered', 'shipped', 'partial_received']

  return [
    {
      key: 'request',
      label: '采购申请',
      completed: !!requestNo,
      active: false,
      clickable: !!requestNo,
      badge: requestNo || undefined,
      route: requestNo ? { path: '/purchase/request', query: { requestNo } } : undefined
    },
    {
      key: 'order',
      label: '采购订单',
      completed: completedStatuses.includes(status),
      active: true,
      clickable: false
    },
    {
      key: 'receive',
      label: '到货登记',
      completed: receivedStatuses.includes(status),
      active: inTransitStatuses.includes(status),
      // 只有进行到该环节（已下单/已收货）才可点击查看到货单
      clickable: inTransitStatuses.includes(status) || receivedStatuses.includes(status),
      route: { path: '/purchase/stockin', query: { orderNo } }
    },
    {
      key: 'stockin',
      label: '入库',
      completed: status === 'completed',
      active: ['received', 'partial_received'].includes(status),
      // 入库为库存汇总结果，无可点击的单对单单据，保持不可点
      clickable: false,
      route: { path: '/warehouse/overview', query: { orderNo } }
    },
    {
      key: 'settlement',
      label: '采购结算',
      completed: status === 'completed' && detail.paymentStatus === 'paid',
      active: (['received', 'partial_received'].includes(status) && detail.paymentStatus !== 'paid') || status === 'completed',
      // 结算单据详情弹窗暂未接入，保持不可点
      clickable: false,
      route: { path: '/purchase/settlement', query: { orderNo } }
    }
  ]
})

/** 点击流程步骤条：统一弹出关联单据详情（不跳转页面）；未进行到的步骤不可点击 */
async function handleStepClick(step: ProcessStep) {
  if (!step.clickable) return
  if (step.key === 'request') {
    openRequestDetail()
    return
  }
  if (step.key === 'receive') {
    // 到货登记节点：复用「到货登记页」的到货单详情对话框组件
    if (!currentDetail.value?.orderNo) return
    try {
      const list = await purchaseArrivalApi.getList({ orderNo: currentDetail.value.orderNo, page: 1, size: 1 })
      const first = list.records?.[0]
      if (first) {
        const res = await purchaseArrivalApi.getById(first.arrivalId)
        arrivalRefData.value = res
        arrivalRefVisible.value = true
      } else {
        ElMessage.info('该订单暂未生成到货单')
      }
    } catch {
      ElMessage.error('加载到货单详情失败')
    }
    return
  }
}

/** 格式化金额 */
function formatAmount(val: number): string {
  return val != null ? val.toFixed(2) : '0.00'
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 格式化日期 */
function formatDate(dateStr: string): string {
  return dateStr || '-'
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadSupplierOptions()
  loadRecentRecords()

  // 处理从其他页面跳转过来的 orderId：自动按订单号搜索并高亮
  const routeOrderId = router.currentRoute.value.query.orderId as string
  if (routeOrderId) {
    const recent = recentRecords.value.find(r => r.orderId === routeOrderId)
    if (recent) {
      queryForm.value.orderNo = recent.orderNo
      setHighlight(routeOrderId)
      refresh()
    } else {
      purchaseOrderApi.getById(routeOrderId).then((order) => {
        if (order) {
          queryForm.value.orderNo = order.orderNo
          setHighlight(routeOrderId)
          refresh()
        }
      })
    }
  }

  // 处理从申请/计划页跳转过来的 requestNo：按来源单号过滤并高亮第一条
  const routeRequestNo = router.currentRoute.value.query.requestNo as string
  if (routeRequestNo) {
    queryForm.value.requestNo = routeRequestNo
    queryForm.value.orderNo = ''
    refresh().then(() => {
      const target = tableData.value.find(item => item.requestNo === routeRequestNo)
      if (target) {
        setHighlight(target.purchaseOrderId)
        pushRecentRecord(target, '生成订单')
      }
    })
  }
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="采购订单" description="管理所有采购订单的审批、跟踪和状态">
      <el-button v-permission="'purchase:order:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新建订单
      </el-button>
      <el-button v-permission="'purchase:order:create'" size="default" @click="handleGenerateFromRequest">
        <el-icon :size="16"><Document /></el-icon>从申请生成
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Tickets" label="订单总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Clock" label="待审核" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="ShoppingCart" label="已下单" :value="String(statistics.ordered)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.orderNo"
            placeholder="搜索订单编号..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.status"
            placeholder="订单状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in PurchaseOrderStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.paymentStatus"
            placeholder="付款状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in PaymentStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
          />
          <el-checkbox v-model="queryForm.mineOnly" size="default" @change="handleSearch">我的单据</el-checkbox>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 最近操作提示条 -->
    <div v-if="recentRecords.length > 0 && recentTipVisible" class="recent-tip">
      <div class="recent-tip__content">
        <span class="recent-tip__label">最近操作：</span>
        <el-button
          v-for="record in recentRecords"
          :key="record.orderId"
          link
          type="primary"
          size="small"
          @click="locateRecent(record.orderId)"
        >
          {{ record.orderNo }}（{{ record.action }}）
        </el-button>
      </div>
      <el-button link type="info" size="small" @click="closeRecentTip">收起</el-button>
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
        :row-class-name="rowClassName"
        @selection-change="handleSelectionChange"
      >
        <!-- 订单编号列 -->
        <template #orderNo="{ row }">
          <span class="order-no-text">{{ row.orderNo }}</span>
        </template>

        <!-- 来源采购申请列 -->
        <template #requestNo="{ row }">
          <span class="request-no-text">{{ row.requestNo || '-' }}</span>
          <span v-if="row.sourceType === 'request'" class="source-badge">申请转单</span>
        </template>

        <!-- 供应商列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName || '-' }}</span>
        </template>

        <!-- 创建人列 -->
        <template #createByName="{ row }">
          <span class="creator-text">{{ row.createByName || '-' }}</span>
        </template>

        <!-- 订单状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="purchaseOrderConverter.toStatusTagStatus(row.status)"
            :label="purchaseOrderConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 付款状态列 -->
        <template #paymentStatus="{ row }">
          <StatusTag
            :status="purchaseOrderConverter.toPaymentStatusTagStatus(row.paymentStatus)"
            :label="purchaseOrderConverter.toPaymentStatusLabel(row.paymentStatus)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 总金额列 -->
        <template #totalAmount="{ row }">
          <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
        </template>

        <!-- 下单日期列 -->
        <template #orderDate="{ row }">
          <span class="date-text">{{ formatDate(row.orderDate) }}</span>
        </template>

        <!-- 预计到货日期列 -->
        <template #expectedDate="{ row }">
          <span class="date-text">{{ formatDate(row.expectedDate) }}</span>
        </template>

        <!-- 创建时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 操作列：查看 + 状态快捷操作（编辑/提交/审批/驳回/收货，按状态显示） -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button v-permission="'purchase:order:view'" link type="primary" size="default" @click.stop="handleView(row)">
              查看
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-permission="'purchase:order:edit'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-permission="'purchase:order:submit'"
              link
              type="success"
              size="default"
              @click.stop="handleSubmitApproval(row)"
            >
              {{ row.status === 'rejected' ? '重新提交' : '提交审批' }}
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'purchase:order:approve'"
              link
              type="primary"
              size="default"
              @click.stop="handleApprove(row)"
            >审批
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'purchase:order:approve'"
              link
              type="danger"
              size="default"
              @click.stop="handleRejectFromList(row)"
            >驳回
            </el-button>
            <el-button
              v-if="row.status === 'ordered'"
              v-permission="'purchase:stockin:create'"
              link
              type="primary"
              size="default"
              @click.stop="handleStockin(row)"
            >
              收货
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-permission="'purchase:order:delete'"
              link
              type="danger"
              size="default"
              @click.stop="handleDelete(row)"
            >
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
        />
      </div>
    </section>

    <!-- 新建/编辑对话框 -->
    <PurchaseOrderFormDialog
      ref="orderFormDialogRef"
      :supplier-options="supplierOptions"
      @success="handleOrderFormSuccess"
    />

    <!-- 详情对话框（摘要行 + 步骤条 + Tab 分层） -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="1200px"
      class="fts-dialog--xl"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <!-- 摘要行：状态徽章 + 单号 + 金额 + 当前状态主操作 -->
        <div class="detail-status-section">
          <div class="status-header" :class="`status-header--${currentDetail.status}`">
            <div class="status-header__left">
              <StatusTag
                :status="purchaseOrderConverter.toStatusTagStatus(currentDetail.status)"
                :label="purchaseOrderConverter.toStatusLabel(currentDetail.status)"
                size="large"
                variant="solid"
              />
              <span class="status-header__order-no">{{ currentDetail.orderNo }}</span>
              <span class="status-header__date">下单日期：{{ currentDetail.orderDate || '-' }}</span>
              <span class="status-header__amount">¥{{ formatAmount(currentDetail.totalAmount) }}</span>
            </div>
            <div class="status-header__actions">
              <template v-if="currentDetail.status === 'draft' || currentDetail.status === 'rejected'">
                <el-button v-permission="'purchase:order:edit'" @click="handleEdit(currentDetail)">编辑</el-button>
                <el-button v-permission="'purchase:order:submit'" type="primary" @click="handleSubmitApproval(currentDetail)">
                  {{ currentDetail.status === 'rejected' ? '重新提交' : '提交审批' }}
                </el-button>
                <el-button v-permission="'purchase:order:delete'" type="danger" @click="handleDeleteFromDetail">删除</el-button>
              </template>
              <template v-if="currentDetail.status === 'pending'">
                <el-button v-permission="'purchase:order:approve'" type="primary" @click="handleApproveFromDetail">审批通过</el-button>
                <el-button v-permission="'purchase:order:approve'" type="danger" plain @click="handleRejectFromDetail">驳回</el-button>
              </template>
              <template v-if="currentDetail.status === 'approved'">
                <el-button v-permission="'purchase:order:confirm'" type="primary" @click="handleConfirmOrder">确认下单</el-button>
              </template>
              <template v-if="currentDetail.status === 'ordered'">
                <el-button v-permission="'purchase:stockin:create'" type="primary" @click="handleStockin(currentDetail)">收货</el-button>
                <el-button
                  v-permission="'purchase:order:cancel'"
                  type="danger"
                  plain
                  @click="handleTerminate(currentDetail)"
                >终止</el-button>
              </template>
            </div>
          </div>

          <!-- 单据流转步骤条：申请→订单→到货→收货→入库，进行到的节点可点击弹出详情 -->
          <div class="process-steps">
            <div
              v-for="(step, index) in processSteps"
              :key="step.key"
              class="process-step"
              :class="{
                'is-completed': step.completed,
                'is-active': step.active,
                'is-clickable': step.clickable,
                'is-disabled': !step.clickable && !step.completed
              }"
              @click="handleStepClick(step)"
            >
              <span class="step-index">{{ index + 1 }}</span>
              <span class="step-label">{{ step.label }}</span>
              <span v-if="step.badge" class="step-badge">{{ step.badge }}</span>
            </div>
          </div>
        </div>

        <!-- Tab 分层：基本信息 / 采购明细 / 审批记录 -->
        <el-tabs v-model="detailTab" class="detail-tabs">
          <!-- Tab1 基本信息（精简，关键项标色互动） -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="供应商">
                <span class="link-text">{{ currentDetail.supplierName || '未指定' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="来源申请">
                <el-link v-if="currentDetail.requestNo" type="primary" :underline="false" @click="openRequestDetail">
                  {{ currentDetail.requestNo }}
                </el-link>
                <template v-else>{{ currentDetail.sourceType === 'request' ? '申请转单' : '手工创建' }}</template>
              </el-descriptions-item>
              <el-descriptions-item label="账期">
                {{ detailSupplier?.paymentTerms != null ? `${detailSupplier.paymentTerms}天` : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="结算方式">
                {{ detailSupplier ? supplierConverter.toSettlementMethodLabel(detailSupplier.settlementMethod) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="下单日期">{{ currentDetail.orderDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ currentDetail.createByName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="预计到货日期">{{ currentDetail.expectedDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="付款状态">
                <StatusTag
                  :status="purchaseOrderConverter.toPaymentStatusTagStatus(currentDetail.paymentStatus)"
                  :label="purchaseOrderConverter.toPaymentStatusLabel(currentDetail.paymentStatus)"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="优先级">
                {{ currentDetail.priority === 'urgent' ? '紧急' : '普通' }}
              </el-descriptions-item>
              <el-descriptions-item label="联系人">{{ detailSupplier?.contactPerson || currentDetail.contactPerson || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ detailSupplier?.contactPhone || currentDetail.contactPhone || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="currentDetail.rejectReason" label="拒绝原因" :span="2">
                {{ currentDetail.rejectReason }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- Tab2 采购明细（加大字号与行距） -->
          <el-tab-pane label="采购明细" name="items">
            <el-table :data="currentDetail.items || []" border class="detail-items-table">
              <el-table-column prop="materialName" label="物料名称" min-width="160" />
              <el-table-column prop="specification" label="规格" min-width="120" />
              <el-table-column prop="unit" label="单位" width="90" align="center" />
              <el-table-column prop="quantity" label="数量" width="100" align="center" />
              <el-table-column prop="unitPrice" label="单价(元)" width="130" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column prop="amount" label="金额(元)" width="130" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="receivedQuantity" label="已到货" width="100" align="center" />
              <el-table-column prop="remark" label="备注" min-width="130" />
            </el-table>
          </el-tab-pane>

          <!-- Tab3 审批记录（紧凑横排，节省纵向空间） -->
          <el-tab-pane label="审批记录" name="approval">
            <ApprovalRecordCompact :business-id="currentDetail.purchaseOrderId" business-type="purchase_order" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <!-- 采购申请详情（复用申请页原对话框组件） -->
    <RequestDetailDialog v-model="requestRefVisible" :biz-id="requestRefId" />

    <!-- 到货单详情（复用到货登记页原对话框组件） -->
    <ArrivalDetailDialog v-model="arrivalRefVisible" :data="arrivalRefData" />

    <!-- 选择采购申请对话框（生成订单） -->
    <el-dialog
      v-model="showRequestDialog"
      title="从采购申请生成订单"
      width="1100px"
      class="fts-dialog--xl"
      destroy-on-close
      lock-scroll="false"
    >
      <el-alert
        title="选择已审批通过的采购申请，确认后将按申请生成采购订单（草稿）"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-table
        :data="approvedRequests"
        border
        class="request-select-table"
        @selection-change="handleRequestSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="requestNo" label="申请编号" min-width="150">
          <template #default="{ row }">
            <span class="request-no-text">{{ row.requestNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="申请标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="departmentName" label="申请部门" min-width="110" />
        <el-table-column prop="createByName" label="申请人" min-width="100" />
        <el-table-column prop="createTime" label="申请时间" min-width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" min-width="110" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <StatusTag :status="purchaseRequestConverter.toStatusTagStatus(row.status)" :label="purchaseRequestConverter.toStatusLabel(row.status)" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <div v-if="selectedRequests.length" class="request-select-summary">
        已选 <strong>{{ selectedRequests.length }}</strong> 条，合计 <strong>¥{{ formatAmount(selectedTotal) }}</strong>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showRequestDialog = false">取消</el-button>
          <el-button
            v-permission="'purchase:order:create'"
            type="primary"
            :disabled="selectedRequests.length === 0"
            @click="confirmGenerateFromRequest"
          >
            确认生成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批流程对话框 -->
    <ApprovalDialog
      v-model:visible="approvalDialogVisible"
      :business-id="approvalBusinessId"
      :business-type="approvalBusinessType"
      :current-node-name="approvalNodeName"
      :business-no="approvalBusinessNo"
      :approve-handler="handleApproveOrder"
      :reject-handler="handleRejectOrder"
      @approved="onApproved"
      @rejected="onRejected"
    />
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

// 最近操作提示条
.recent-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-6);
  background: var(--fts-primary-bg);
  border: 1px solid var(--fts-primary-light);
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
:deep(.el-table__row.purchase-order-row--highlight.purchase-order-row--highlight) {
  td {
    background-color: var(--fts-primary-bg);
    animation: row-pulse 2s ease-in-out;
  }
}

@keyframes row-pulse {
  0% {
    background-color: var(--fts-primary-light);
  }
  100% {
    background-color: var(--fts-primary-bg);
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

// 订单编号
.order-no-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

// 来源采购申请
.request-no-text {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

// 申请选择表格：加大行距与字号
.request-select-table :deep(.el-table__cell) {
  padding: 10px 8px;
  font-size: 14px;
}

.request-select-summary {
  margin-top: var(--fts-space-3);
  text-align: right;
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
}

.source-badge {
  display: inline-block;
  margin-left: var(--fts-space-2);
  padding: 0 var(--fts-space-2);
  background: var(--fts-info-bg);
  color: var(--fts-info);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);
}

// 供应商
.supplier-text {
  color: var(--fts-text-primary);
}

// 金额
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
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

// 详情页样式
.detail-items {
  margin-top: var(--fts-space-4);

  h4 {
    margin: 0 0 var(--fts-space-3);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

// 详情状态区：Header 条 + 流程步骤条
.detail-status-section {
  margin-bottom: var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  overflow: hidden;
  border: 1px solid var(--fts-border-primary);
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-5);

  &__left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4);
    flex-wrap: wrap;
  }

  &__order-no {
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
  }

  &__amount {
    font-size: 22px;
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
    font-variant-numeric: tabular-nums;
  }

  &__date {
    margin-left: var(--fts-space-4);
    padding-left: var(--fts-space-4);
    border-left: 1px solid var(--fts-border-primary);
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__actions {
    display: flex;
    gap: var(--fts-space-2);
    flex-shrink: 0;
  }

  // 浅色模式状态背景
  &--draft {
    background: var(--fts-bg-card);
    border-bottom: 1px solid var(--fts-border-primary);
  }
  &--pending {
    background: var(--fts-warning-bg);
    border-bottom: 1px solid var(--fts-warning);
  }
  &--approved {
    background: var(--fts-info-bg);
    border-bottom: 1px solid var(--fts-info);
  }
  &--ordered,
  &--shipped {
    background: var(--fts-primary-bg);
    border-bottom: 1px solid var(--fts-primary);
  }
  &--received,
  &--partial_received {
    background: var(--fts-info-bg);
    border-bottom: 1px solid var(--fts-info);
  }
  &--rejected,
  &--terminated {
    background: var(--fts-error-bg);
    border-bottom: 1px solid var(--fts-error);
  }
  &--completed {
    background: var(--fts-success-bg);
    border-bottom: 1px solid var(--fts-success);
  }
  &--cancelled {
    background: var(--fts-info-bg);
    border-bottom: 1px solid var(--fts-info);
  }
}

// 深色模式：状态背景改用半透明主题色，避免灰黑一片
.dark .status-header,
[data-theme='dark'] .status-header {
  &--draft {
    background: rgba(255, 255, 255, 0.03);
    border-bottom-color: var(--fts-border-primary);
  }
  &--pending {
    background: rgba(255, 204, 77, 0.12);
    border-bottom-color: rgba(255, 204, 77, 0.4);
  }
  &--approved,
  &--received,
  &--partial_received,
  &--cancelled {
    background: rgba(74, 158, 255, 0.12);
    border-bottom-color: rgba(74, 158, 255, 0.4);
  }
  &--ordered,
  &--shipped {
    background: rgba(74, 158, 255, 0.15);
    border-bottom-color: rgba(74, 158, 255, 0.45);
  }
  &--rejected,
  &--terminated {
    background: rgba(255, 123, 107, 0.12);
    border-bottom-color: rgba(255, 123, 107, 0.4);
  }
  &--completed {
    background: rgba(91, 200, 122, 0.12);
    border-bottom-color: rgba(91, 200, 122, 0.4);
  }
}

// 采购流程步骤条：申请 → 订单 → 收货 → 入库 → 结算
.process-steps {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-3) var(--fts-space-5);
  background: var(--fts-bg-card);
}

.process-step {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: 6px 12px;
  border-radius: var(--fts-radius-full);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  background: var(--fts-bg-page);
  border: 1px solid var(--fts-border-primary);
  transition: all 0.2s ease;
  cursor: default;

  .step-index {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--fts-border-primary);
    color: var(--fts-text-tertiary);
    font-size: 11px;
    font-weight: var(--fts-font-weight-bold);
  }

  .step-label {
    font-weight: var(--fts-font-weight-medium);
  }

  .step-badge {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 2px 8px;
    border-radius: var(--fts-radius-full);
    background: var(--fts-border-primary);
    color: var(--fts-text-secondary);
    font-size: 11px;
  }

  &.is-completed {
    color: var(--fts-success);
    border-color: rgba(52, 168, 83, 0.35);
    background: rgba(52, 168, 83, 0.08);

    .step-index {
      background: var(--fts-success);
      color: white;
    }

    .step-badge {
      background: rgba(52, 168, 83, 0.15);
      color: var(--fts-success);
    }
  }

  &.is-active {
    color: var(--fts-primary);
    border-color: rgba(26, 95, 180, 0.4);
    background: rgba(26, 95, 180, 0.08);
    box-shadow: 0 0 0 2px rgba(26, 95, 180, 0.08);

    .step-index {
      background: var(--fts-primary);
      color: white;
    }
  }

  &.is-clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      border-color: var(--fts-primary);
      color: var(--fts-primary);
    }
  }

  /* 未进行到的步骤：弱化显示，不可点击 */
  &.is-disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

/* 采购明细表格：加大字号与行距（用户反馈过小） */
.detail-items-table {
  :deep(.el-table__cell) {
    padding: 10px 8px;
    font-size: 14px;
  }
}

.dark .process-step,
[data-theme='dark'] .process-step {
  background: rgba(255, 255, 255, 0.03);

  &.is-completed {
    background: rgba(91, 200, 122, 0.12);
    border-color: rgba(91, 200, 122, 0.4);

    .step-badge {
      background: rgba(91, 200, 122, 0.2);
    }
  }

  &.is-active {
    background: rgba(74, 158, 255, 0.12);
    border-color: rgba(74, 158, 255, 0.45);
    box-shadow: 0 0 0 2px rgba(74, 158, 255, 0.12);
  }

  &.is-clickable:hover {
    border-color: var(--fts-primary);
  }
}

.detail-actions {
  display: flex;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) 0;
  border-top: 1px solid var(--fts-border-primary);
  margin-top: var(--fts-space-4);
}

/* 详情"更多"Tab 操作区 */
.detail-more-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) 0;
}

.detail-tabs {
  margin-top: var(--fts-space-3);
}

/* 关键信息标色（供应商标色提示，无跳转） */
.link-text {
  color: var(--fts-primary);
  font-weight: var(--fts-font-weight-medium);
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
