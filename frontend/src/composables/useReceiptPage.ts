/**
 * 收货/入库页面通用逻辑
 * 供门店收货、库存入库共用，根据 receiverType 区分数据来源。
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { purchaseArrivalApi } from '@/api/purchase/arrival'
import { receiptConfirmationApi } from '@/api/receipt-confirmation'
import { useCrudTable } from './useCrudTable'
import type {
  PurchaseArrivalInfo,
  PurchaseArrivalStatus,
  ReceiptConfirmationInfo,
  ReceiverType,
} from '@/types/purchase-arrival'
import { VehicleTypeOptions } from '@/types/purchase-arrival'
import type { DataTableColumn } from '@/components/core/DataTable.vue'

/** 待收货状态集合 */
const PENDING_RECEIPT_STATUSES: PurchaseArrivalStatus[] = ['pending', 'receiving', 'partial_received']

/**
 * @param receiverType 收货方类型
 */
export function useReceiptPage(receiverType: ReceiverType) {
  const activeTab = ref<'pending' | 'confirmed'>('pending')

  // 到货单查询
  const arrivalQuery = ref({
    arrivalCode: '',
    orderCode: '',
    status: '' as PurchaseArrivalStatus | '',
    receiverType,
  })

  const {
    tableData: arrivalList,
    loading: arrivalLoading,
    refresh: refreshArrivals,
    pagination: arrivalPagination,
  } = useCrudTable<PurchaseArrivalInfo, typeof arrivalQuery.value>({
    api: {
      getList: async (params) => {
        const res = await purchaseArrivalApi.getList(params)
        return {
          records: res.records,
          total: res.total,
          current: params.page,
          size: params.size,
          pages: Math.ceil(res.total / params.size) || 1,
        }
      },
    },
    queryForm: arrivalQuery,
    autoLoad: true,
  })

  // 确认单查询
  const confirmationQuery = ref({
    confirmationCode: '',
    receiverType,
  })

  const {
    tableData: confirmationList,
    loading: confirmationLoading,
    refresh: refreshConfirmations,
    pagination: confirmationPagination,
  } = useCrudTable<ReceiptConfirmationInfo, typeof confirmationQuery.value>({
    api: {
      getList: async (params) => {
        const res = await receiptConfirmationApi.getList(params)
        return {
          records: res.records,
          total: res.total,
          current: params.page,
          size: params.size,
          pages: Math.ceil(res.total / params.size) || 1,
        }
      },
    },
    queryForm: confirmationQuery,
    autoLoad: true,
  })

  // 统计数据
  const statistics = computed(() => {
    const now = new Date()
    const todayStr = now.toISOString().slice(0, 10)
    const monthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`

    const pendingCount = arrivalList.value.filter((item) => PENDING_RECEIPT_STATUSES.includes(item.status)).length
    const todayCount = confirmationList.value.filter(
      (item) => item.confirmTime && item.confirmTime.slice(0, 10) === todayStr,
    ).length
    const monthCount = confirmationList.value.filter(
      (item) => item.confirmTime && item.confirmTime.slice(0, 10) >= monthStart,
    ).length
    const overdueCount = arrivalList.value.filter((item) => isOverdue(item)).length

    return {
      pending: pendingCount,
      today: todayCount,
      month: monthCount,
      overdue: overdueCount,
    }
  })

  // 表格列定义
  const arrivalColumns = computed<DataTableColumn[]>(() => [
    { prop: 'arrivalCode', label: '到货单号', minWidth: 160, slot: 'arrivalCode' },
    { prop: 'orderCode', label: '采购订单号', minWidth: 150, slot: 'orderCode' },
    { prop: 'supplierName', label: '供应商', minWidth: 130, slot: 'supplierName' },
    { prop: 'shipmentStatus', label: '发货状态', minWidth: 100, slot: 'shipmentStatus' },
    { prop: 'logisticsNo', label: '物流单号', minWidth: 130, slot: 'logisticsNo' },
    { prop: 'logisticsCompany', label: '物流公司', minWidth: 130, slot: 'logisticsCompany' },
    { prop: 'vehicleInfo', label: '车辆信息', minWidth: 160, slot: 'vehicleInfo' },
    { prop: 'estimatedArrivalDate', label: '预计到货日期', minWidth: 130, slot: 'estimatedArrivalDate' },
    { prop: 'quantity', label: '到货数量', minWidth: 120, slot: 'quantity', align: 'right' },
    { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
  ])

  const confirmationColumns = computed<DataTableColumn[]>(() => [
    { prop: 'confirmationCode', label: '确认单号', minWidth: 160, slot: 'confirmationCode' },
    { prop: 'arrivalId', label: '关联到货单', minWidth: 130, slot: 'arrivalId' },
    { prop: 'confirmTime', label: '确认时间', minWidth: 150, slot: 'confirmTime' },
    { prop: 'totalQuantity', label: '确认数量', minWidth: 100, align: 'right' },
    { prop: 'totalAmount', label: '确认金额', minWidth: 120, slot: 'totalAmount', align: 'right' },
    { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
  ])

  // 对话框状态
  const formDialogVisible = ref(false)
  const confirmingArrival = ref<PurchaseArrivalInfo | null>(null)
  const confirmDialogLoading = ref(false)

  const detailDialogVisible = ref(false)
  const detailMode = ref<'arrival' | 'confirmation'>('arrival')
  const detailArrival = ref<PurchaseArrivalInfo | null>(null)
  const detailConfirmation = ref<ReceiptConfirmationInfo | null>(null)

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

  function getConfirmationStatusConfig(status: number): { status: string; label: string } {
    if (status === 1) return { status: 'success', label: '已确认' }
    if (status === 2) return { status: 'error', label: '已取消' }
    return { status: 'info', label: String(status) }
  }

  function getShipmentStatusLabel(status?: string): string {
    if (!status) return '-'
    const map: Record<string, string> = { shipped: '已发货', in_transit: '运输中', delivered: '已送达' }
    return map[status] || status
  }

  function getVehicleTypeLabel(type?: string): string {
    if (!type) return '-'
    return VehicleTypeOptions.find((o) => o.value === type)?.label || type
  }

  function formatVehicleInfo(row: PurchaseArrivalInfo): string {
    if (row.vehiclePlateNo && row.vehicleType) {
      return `${row.vehiclePlateNo}（${getVehicleTypeLabel(row.vehicleType)}）`
    }
    return row.vehiclePlateNo || getVehicleTypeLabel(row.vehicleType) || '-'
  }

  function canConfirm(row: PurchaseArrivalInfo): boolean {
    return PENDING_RECEIPT_STATUSES.includes(row.status)
  }

  /**
   * 打开收货确认对话框
   * 必须先从后端加载包含明细的到货单详情，避免列表数据缺少 items 导致提交失败
   */
  async function openConfirmDialog(row: PurchaseArrivalInfo): Promise<void> {
    confirmDialogLoading.value = true
    try {
      const detail = await purchaseArrivalApi.getById(row.arrivalId)
      if (!detail.items || detail.items.length === 0) {
        ElMessage.warning('该到货单无明细数据，无法进行收货确认')
        return
      }
      confirmingArrival.value = detail
      formDialogVisible.value = true
    } catch {
      ElMessage.error('加载到货单详情失败，请重试')
    } finally {
      confirmDialogLoading.value = false
    }
  }

  async function openArrivalDetail(row: PurchaseArrivalInfo): Promise<void> {
    try {
      const res = await purchaseArrivalApi.getById(row.arrivalId)
      detailArrival.value = res
      detailMode.value = 'arrival'
      detailDialogVisible.value = true
    } catch {
      ElMessage.error('获取到货单详情失败')
    }
  }

  async function openConfirmationDetail(row: ReceiptConfirmationInfo): Promise<void> {
    try {
      const res = await receiptConfirmationApi.getById(row.confirmationId)
      detailConfirmation.value = res
      detailMode.value = 'confirmation'
      detailDialogVisible.value = true
    } catch {
      ElMessage.error('获取确认单详情失败')
    }
  }

  async function handleConfirmationSuccess(): Promise<void> {
    await Promise.all([refreshArrivals(), refreshConfirmations()])
  }

  function formatDate(iso?: string): string {
    if (!iso) return '-'
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }

  return {
    activeTab,
    arrivalQuery,
    arrivalList,
    arrivalLoading,
    refreshArrivals,
    arrivalPagination,
    confirmationQuery,
    confirmationList,
    confirmationLoading,
    refreshConfirmations,
    confirmationPagination,
    statistics,
    arrivalColumns,
    confirmationColumns,
    formDialogVisible,
    confirmingArrival,
    confirmDialogLoading,
    detailDialogVisible,
    detailMode,
    detailArrival,
    detailConfirmation,
    isOverdue,
    getStatusConfig,
    getConfirmationStatusConfig,
    getShipmentStatusLabel,
    getVehicleTypeLabel,
    formatVehicleInfo,
    canConfirm,
    openConfirmDialog,
    openArrivalDetail,
    openConfirmationDetail,
    handleConfirmationSuccess,
    formatDate,
  }
}
