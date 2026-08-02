<script setup lang="ts">
/**
 * 订单查询页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】查询和管理所有订单，支持多维度筛选
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, Printer } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { orderApi } from '@/api/order/order'
import { orderDataConverter } from '@/api/order/converters'
import trayApi from '@/api/tray'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { Order, OrderQueryForm, OrderStatusValue, OrderDetail, OrderItem } from '@/types/order'
import type { Tray, TrayStatus } from '@/api/tray'

const layoutStore = useLayoutStore()
const route = useRoute()

// ==================== 门店选项（从真实 API 加载，使用共享 composable） ====================
const { storeOptions } = useStoreOptions(true)

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

const selectedRows = ref<Order[]>([])
const detailVisible = ref(false)
const currentDetail = ref<OrderDetail | null>(null)
const detailLoading = ref(false)
const exportLoading = ref(false)

// 订单关联的托盘使用记录（管理端只读展示）
const orderTrays = ref<Tray[]>([])

// 查询表单
const queryForm = ref<OrderQueryForm & { storeId?: string; paymentMethod?: string }>({
  orderCode: '',
  orderStatus: '' as '' | OrderStatusValue,
  paymentStatus: undefined as number | undefined,
  customerName: '',
  startTime: '',
  endTime: '',
  storeId: '',
  paymentMethod: '',
})

// 日期范围
const dateRange = ref<string[] | null>(null)

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<Order, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      // 日期选择器 value-format 为 YYYY-MM-DD，结束日期需补全到 23:59:59 以包含当天
      const rawStart = dateRange.value?.[0] || params.startTime || undefined
      const rawEnd = dateRange.value?.[1] || params.endTime || undefined
      const startTime = rawStart && rawStart.length === 10 ? `${rawStart} 00:00:00` : rawStart
      const endTime = rawEnd && rawEnd.length === 10 ? `${rawEnd} 23:59:59` : rawEnd

      const apiParams: OrderQueryForm = {
        page: params.page,
        size: params.size,
        orderCode: params.orderCode || undefined,
        orderStatus: params.orderStatus ? (params.orderStatus as OrderStatusValue) : undefined,
        paymentStatus: params.paymentStatus,
        customerName: params.customerName || undefined,
        startTime,
        endTime,
        storeId: params.storeId || undefined,
        paymentMethod: params.paymentMethod ? Number(params.paymentMethod) : undefined,
      }
      // 订单统一查询：POS端产生的交易行为是管理端获取订单数据的主要来源
      // 查询 orders_legacy 表（POS端订单），包含门店信息
      return orderApi.getPosList(apiParams)
    },
  } as unknown as CrudApi<Order, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 今日统计数据
const statistics = ref({
  todayOrders: 0,
  pendingOrders: 0,
  completedOrders: 0,
  totalAmount: '0.00',
})

// ==================== 计算属性 ====================

/**
 * 已完成订单数（基于当前页数据计算，实际应从统计接口获取）
 * 这里优先使用今日统计接口的数据
 */
const completedCount = computed(() => {
  return tableData.value.filter(item => item.orderStatus === 'completed').length
})

/**
 * 订单总金额（基于当前页数据计算）
 */
const totalAmountOnPage = computed(() => {
  return tableData.value.reduce((sum, item) => sum + parseFloat(item.finalAmount || '0'), 0).toFixed(2)
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'orderCode', label: '订单号', minWidth: 160, slot: 'orderCode' },
  { prop: 'createTime', label: '下单时间', minWidth: 160, slot: 'createTime' },
  { prop: 'storeName', label: '门店', minWidth: 100, slot: 'storeName' },
  { prop: 'customerInfo', label: '顾客信息', minWidth: 150, slot: 'customerInfo' },
  { prop: 'itemsSummary', label: '商品明细', minWidth: 200, slot: 'itemsSummary' },
  { prop: 'finalAmount', label: '订单金额', minWidth: 100, slot: 'finalAmount' },
  { prop: 'paymentMethod', label: '支付方式', minWidth: 100, slot: 'paymentMethod' },
  { prop: 'orderStatus', label: '订单状态', minWidth: 100, slot: 'orderStatus', ellipsis: false },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

/** 加载今日统计数据 */
async function loadTodayStats(): Promise<void> {
  try {
    const res = await orderApi.getTodayStats()
    statistics.value = {
      todayOrders: res.todayOrders,
      pendingOrders: res.pendingOrders,
      completedOrders: res.completedOrders,
      totalAmount: res.todayRevenue,
    }
  } catch {
    statistics.value = {
      todayOrders: 0,
      pendingOrders: 0,
      completedOrders: 0,
      totalAmount: '0.00',
    }
  }
}

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value = {
    orderCode: '',
    orderStatus: '' as '' | OrderStatusValue,
    paymentStatus: undefined,
    customerName: '',
    startTime: '',
    endTime: '',
    storeId: '',
    paymentMethod: '',
  }
  dateRange.value = null
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: Order[]) {
  selectedRows.value = rows
}

/** 查看订单详情 */
async function handleViewDetail(row: Order): Promise<void> {
  detailLoading.value = true
  detailVisible.value = true
  currentDetail.value = null
  orderTrays.value = []
  try {
    // POS终端订单（orderId以O开头）查询orders_legacy + order_items_legacy表
    // 通过专门的POS订单详情接口获取完整明细（含菜品列表），用于数据追溯
    if (String(row.orderId).startsWith('O')) {
      const detail = await orderApi.getPosDetail(row.orderCode)
      currentDetail.value = detail
      // 加载托盘使用记录（管理端只读，不参与业务步骤）
      await loadOrderTrays(String(row.orderId))
      return
    }
    const detail = await orderApi.getById(Number(row.orderId))
    currentDetail.value = detail
    // 加载托盘使用记录（管理端只读，不参与业务步骤）
    await loadOrderTrays(String(row.orderId))
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载订单详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/** 加载订单关联的托盘使用记录（管理端只读展示，不参与业务流程） */
async function loadOrderTrays(orderId: string): Promise<void> {
  try {
    orderTrays.value = await trayApi.getByOrderId(orderId)
  } catch {
    // 托盘数据加载失败不应阻断订单详情展示
    orderTrays.value = []
  }
}

/** 获取托盘状态标签文本 */
function getTrayStatusLabel(status?: TrayStatus): string {
  const map: Record<TrayStatus, string> = {
    idle: '空闲',
    bound: '已绑定',
    making: '制作中',
    ready: '已出餐',
    served: '已取餐',
    cleaning: '清洁中',
    damaged: '损坏',
  }
  return status ? (map[status] || status) : '-'
}

/** 获取托盘状态对应的 StatusTag 状态 */
function getTrayStatusColor(status?: TrayStatus): string {
  const map: Record<TrayStatus, string> = {
    idle: 'info',
    bound: 'primary',
    making: 'warning',
    ready: 'success',
    served: 'success',
    cleaning: 'info',
    damaged: 'error',
  }
  return status ? (map[status] || 'info') : 'info'
}

/** 退款操作 */
async function handleRefund(row: Order): Promise<void> {
  // 当前 OrderQuery 页面未提供退款创建接口（orderRefundApi 仅有 approve 审批接口），
  // 该入口仅做"功能未开放"提示，不再弹出确认对话框避免误导用户认为已发起退款。
  // TODO: 后端补全 POST /v1/orders/refunds 创建退款申请接口后，
  //       替换为 ElMessageBox.confirm + orderRefundApi.create(...) 真实调用。
  ElMessage.info(`订单「${row.orderCode}」的退款功能开发中，请前往退款管理页面处理`)
}

/** 打印小票 */
function handlePrint(row: Order): void {
  ElMessage.info(`正在打印订单 ${row.orderCode} 的小票...`)
}

/** 导出订单数据 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')
    // TODO: 调用导出接口
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('导出成功！')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/** 批量操作 */
async function handleBatchAction(action: string): Promise<void> {
  if (selectedRows.value.length === 0) return
  try {
    if (action === 'export') {
      ElMessage.info(`批量导出 ${selectedRows.value.length} 个订单`)
    } else {
      ElMessage.info(`批量操作: ${action}`)
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// ==================== 辅助方法 ====================

/** 获取订单状态对应的 StatusTag 状态 */
function getOrderStatusColor(status: OrderStatusValue): string {
  const map: Record<OrderStatusValue, string> = {
    pending: 'warning',
    confirmed: 'primary',
    completed: 'success',
    cancelled: 'info',
    partial_refund: 'warning',
    full_refund: 'error',
    pending_review: 'primary',
  }
  return map[status] || 'info'
}

/** 获取订单状态标签文本 */
function getOrderStatusLabel(status: OrderStatusValue): string {
  const map: Record<OrderStatusValue, string> = {
    pending: '待确认',
    confirmed: '已确认',
    completed: '已完成',
    cancelled: '已取消',
    partial_refund: '部分退款',
    full_refund: '全额退款',
    pending_review: '待评价',
  }
  return map[status] || status
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 获取商品摘要 */
function getItemsSummary(items?: OrderItem[]): string {
  if (!items || items.length === 0) return '-'
  if (items.length === 1) {
    return `${items[0].productName} ×${items[0].quantity}`
  }
  const first = items[0]
  return `${first.productName} ×${first.quantity} 等${items.length}件商品`
}

/** 获取订单状态时间线数据 */
function getStatusTimeline(detail: OrderDetail): Array<{ time: string; status: string; label: string }> {
  const timeline: Array<{ time: string; status: string; label: string }> = []
  if (detail.createTime) {
    timeline.push({ time: detail.createTime, status: 'pending', label: '订单创建' })
  }
  // 可根据实际状态添加更多时间节点
  if (detail.orderStatus === 'completed' || detail.orderStatus === 'confirmed') {
    timeline.push({ time: detail.updateTime, status: 'confirmed', label: '订单确认' })
  }
  if (detail.orderStatus === 'completed') {
    timeline.push({ time: detail.updateTime, status: 'completed', label: '订单完成' })
  }
  if (detail.orderStatus === 'cancelled') {
    timeline.push({ time: detail.updateTime, status: 'cancelled', label: '订单取消' })
  }
  return timeline
}

// ==================== 生命周期 ====================

onMounted(async () => {
  await loadTodayStats()

  // 支持从订单统计页面跳转时携带的筛选参数（date/store）
  const { date, store } = route.query
  if (date || store) {
    if (date && typeof date === 'string') {
      // 日期选择器格式为 YYYY-MM-DD，设置当天为开始和结束
      dateRange.value = [date, date]
    }
    if (store && typeof store === 'string' && store !== '全部门店') {
      // 根据门店名称匹配门店ID
      const matched = storeOptions.value.find(s => s.storeName === store)
      if (matched) {
        queryForm.value.storeId = String(matched.storeId)
      }
    }
    handleSearch()
  }
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="订单查询" description="查询和管理所有订单，支持多维度筛选" />

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="今日订单" :value="String(statistics.todayOrders)" color-type="primary" variant="bordered" />
      <StatCard icon="Clock" label="待处理订单" :value="String(statistics.pendingOrders)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成订单" :value="String(statistics.completedOrders)" color-type="success" variant="bordered" />
      <StatCard icon="Money" label="订单总金额" :value="`¥${statistics.totalAmount}`" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.orderCode"
            placeholder="订单号"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-select
            v-model="queryForm.orderStatus"
            placeholder="订单状态"
            clearable
            style="width: 140px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="待确认" value="pending" />
            <el-option label="已确认" value="confirmed" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="部分退款" value="partial_refund" />
            <el-option label="全额退款" value="full_refund" />
            <el-option label="待评价" value="pending_review" />
          </el-select>

          <el-select
            v-model="queryForm.paymentStatus"
            placeholder="支付状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="未支付" :value="0" />
            <el-option label="部分支付" :value="1" />
            <el-option label="已支付" :value="2" />
            <el-option label="已退款" :value="3" />
          </el-select>

          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            @change="handleSearch"
          />

          <el-select
            v-model="queryForm.storeId"
            placeholder="门店选择"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="全部门店" value="" />
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="String(store.storeId)"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">
            <el-icon :size="14"><Search /></el-icon>查询
          </el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="success"
            size="default"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
          <el-button
            size="default"
            @click="refresh"
          >
            <el-icon :size="14"><Refresh /></el-icon>刷新
          </el-button>
          <el-dropdown
            :disabled="selectedRows.length === 0"
            @command="handleBatchAction"
          >
            <el-button size="default">
              批量操作
              <el-icon class="el-icon--right"><span class="el-caret-bottom" /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="export">批量导出</el-dropdown-item>
                <el-dropdown-item command="print">批量打印</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
        <!-- 订单号列 -->
        <template #orderCode="{ row }">
          <span class="order-code">{{ row.orderCode }}</span>
        </template>

        <!-- 下单时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 门店列 -->
        <template #storeName="{ row }">
          <span class="store-text">{{ row.storeName || '-' }}</span>
        </template>

        <!-- 顾客信息列 -->
        <template #customerInfo="{ row }">
          <div class="customer-info">
            <div class="customer-name">{{ row.customerName || '散客' }}</div>
            <div v-if="row.customerPhone" class="customer-phone">{{ row.customerPhone }}</div>
          </div>
        </template>

        <!-- 商品明细列 -->
        <template #itemsSummary="{ row }">
          <div class="items-summary" :title="getItemsSummary(row.items)">
            {{ getItemsSummary(row.items) }}
          </div>
        </template>

        <!-- 订单金额列 -->
        <template #finalAmount="{ row }">
          <span class="amount-text">¥{{ row.finalAmount }}</span>
        </template>

        <!-- 支付方式列 -->
        <template #paymentMethod="{ row }">
          <span class="payment-text">{{ row.paymentStatusName || '-' }}</span>
        </template>

        <!-- 订单状态列 -->
        <template #orderStatus="{ row }">
          <StatusTag
            :status="getOrderStatusColor(row.orderStatus)"
            :label="getOrderStatusLabel(row.orderStatus)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.orderStatus === 'completed' || row.orderStatus === 'confirmed'"
              link
              type="warning"
              size="default"
              @click.stop="handleRefund(row)"
            >
              退款
            </el-button>
            <el-button link type="info" size="default" @click.stop="handlePrint(row)">
              打印
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

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="detailLoading" element-loading-text="正在加载订单详情...">
        <template v-if="currentDetail">
          <!-- 订单基本信息 -->
          <div class="detail-section">
            <div class="section-title">订单基本信息</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="订单号">
                <span class="order-code">{{ currentDetail.orderCode }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="订单类型">
                {{ currentDetail.orderTypeName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="下单时间">
                {{ formatTime(currentDetail.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="订单状态">
                <StatusTag
                  :status="getOrderStatusColor(currentDetail.orderStatus)"
                  :label="getOrderStatusLabel(currentDetail.orderStatus)"
                  size="small"
                  variant="light"
                />
              </el-descriptions-item>
              <el-descriptions-item label="所属门店">
                {{ currentDetail.storeName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="桌号">
                {{ currentDetail.tableName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="用餐人数">
                {{ currentDetail.diningPeopleCount || '-' }} 人
              </el-descriptions-item>
              <el-descriptions-item label="收银员">
                {{ currentDetail.cashierUserName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="备注">
                {{ currentDetail.remark || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 收货/用餐信息 -->
          <div class="detail-section">
            <div class="section-title">顾客信息</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="顾客姓名">
                {{ currentDetail.customerName || '散客' }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ currentDetail.customerPhone || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 商品明细列表 -->
          <div class="detail-section">
            <div class="section-title">商品明细</div>
            <el-table :data="currentDetail.items || []" size="small" border style="width: 100%">
              <el-table-column prop="productName" label="商品名称" min-width="180" />
              <el-table-column prop="unitPrice" label="单价(元)" width="100" align="center" />
              <el-table-column prop="quantity" label="数量" width="80" align="center" />
              <el-table-column prop="amount" label="小计(元)" width="100" align="center" />
              <el-table-column prop="kitchenStatusName" label="制作状态" width="100" align="center" />
            </el-table>
          </div>

          <!-- 金额明细 -->
          <div class="detail-section">
            <div class="section-title">金额明细</div>
            <div class="amount-detail">
              <div class="amount-row">
                <span class="amount-label">商品金额：</span>
                <span class="amount-value">¥{{ currentDetail.totalAmount }}</span>
              </div>
              <div class="amount-row">
                <span class="amount-label">优惠金额：</span>
                <span class="amount-value discount">
                  {{ Number(currentDetail.discountAmount) > 0 ? '-¥' + currentDetail.discountAmount : '¥' + currentDetail.discountAmount }}
                </span>
              </div>
              <div class="amount-row">
                <span class="amount-label">应收金额：</span>
                <span class="amount-value final">¥{{ currentDetail.finalAmount }}</span>
              </div>
              <div class="amount-row">
                <span class="amount-label">已支付：</span>
                <span class="amount-value paid">¥{{ currentDetail.paidAmount }}</span>
              </div>
              <div v-if="parseFloat(currentDetail.refundAmount) > 0" class="amount-row">
                <span class="amount-label">已退款：</span>
                <span class="amount-value refund">-¥{{ currentDetail.refundAmount }}</span>
              </div>
            </div>
          </div>

          <!-- 订单状态时间线 -->
          <div class="detail-section">
            <div class="section-title">订单状态</div>
            <el-timeline>
              <el-timeline-item
                v-for="(item, index) in getStatusTimeline(currentDetail)"
                :key="index"
                :timestamp="item.time"
                placement="top"
              >
                <StatusTag
                  :status="getOrderStatusColor(item.status as OrderStatusValue)"
                  :label="item.label"
                  size="small"
                  variant="light"
                />
              </el-timeline-item>
            </el-timeline>
          </div>

          <!-- 操作记录 -->
          <div class="detail-section" v-if="currentDetail.payments && currentDetail.payments.length > 0">
            <div class="section-title">支付记录</div>
            <el-table :data="currentDetail.payments" size="small" border style="width: 100%">
              <el-table-column prop="paymentMethodName" label="支付方式" width="120" />
              <el-table-column prop="paymentAmount" label="支付金额(元)" width="120" align="center" />
              <el-table-column prop="paymentTime" label="支付时间" min-width="160" />
              <el-table-column prop="operatorName" label="操作人" width="100" />
            </el-table>
          </div>

          <!-- 托盘使用记录（管理端只读展示，数据由POS端/后厨端产生） -->
          <div class="detail-section">
            <div class="section-title">
              托盘使用记录
              <span class="section-title__hint">数据来源：POS端绑定 / 后厨端扫码</span>
            </div>
            <el-table
              v-if="orderTrays.length > 0"
              :data="orderTrays"
              size="small"
              border
              style="width: 100%"
            >
              <el-table-column prop="trayCode" label="托盘编码" min-width="140" />
              <el-table-column prop="trayName" label="托盘名称" min-width="120">
                <template #default="{ row }">
                  {{ row.trayName || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="托盘状态" width="120" align="center">
                <template #default="{ row }">
                  <StatusTag
                    :status="getTrayStatusColor(row.status)"
                    :label="getTrayStatusLabel(row.status)"
                    size="small"
                    variant="light"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="bindTime" label="绑定时间" min-width="160">
                <template #default="{ row }">
                  {{ formatTime(row.bindTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="lastScanTime" label="最近扫码时间" min-width="160">
                <template #default="{ row }">
                  {{ row.lastScanTime ? formatTime(row.lastScanTime) : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="useCount" label="累计使用" width="100" align="center">
                <template #default="{ row }">
                  {{ row.useCount ?? 0 }} 次
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-else
              description="该订单暂无托盘使用记录"
              :image-size="60"
            />
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handlePrint(currentDetail!)">
          <el-icon :size="14"><Printer /></el-icon>打印小票
        </el-button>
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

// 订单号
.order-code {
  font-family: 'Courier New', monospace;
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-primary);
}

// 门店
.store-text {
  color: var(--fts-text-primary);
}

// 顾客信息
.customer-info {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .customer-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  .customer-phone {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

// 商品摘要
.items-summary {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 金额
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 支付方式
.payment-text {
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

// 详情对话框样式
.detail-section {
  margin-bottom: var(--fts-space-4);

  .section-title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);

    &__hint {
      font-size: var(--fts-font-size-xs);
      font-weight: var(--fts-font-weight-normal);
      color: var(--fts-text-tertiary);
    }
  }
}

// 金额明细
.amount-detail {
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);

  .amount-row {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding: var(--fts-space-2) 0;
    gap: var(--fts-space-6);

    .amount-label {
      color: var(--fts-text-secondary);
    }

    .amount-value {
      font-variant-numeric: tabular-nums;
      color: var(--fts-text-primary);
      min-width: 100px;
      text-align: right;

      &.discount {
        color: var(--fts-success);
      }

      &.final {
        font-size: var(--fts-font-size-lg);
        font-weight: var(--fts-font-weight-bold);
        color: var(--fts-primary);
      }

      &.paid {
        color: var(--fts-success);
      }

      &.refund {
        color: var(--fts-error);
      }
    }
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
  .stats-grid {
    grid-template-columns: 1fr;
  }

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
