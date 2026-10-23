<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { InfoFilled, Search } from '@element-plus/icons-vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { bankAccountApi, fundFlowApi } from '@/api/finance'
import { getPosOrderTrace, type PosOrderTrace } from '@/api/order/order'
import { parsePosOrderRemark, type PosOrderRef } from '@/utils/pos-order-remark'
import { formatDateTime } from '@/utils/formatters'
import type { FinanceBankAccount, FundFlow, FundFlowStatistics } from '@/types/finance'
import type { StatColorType } from '@/types/stat'
import logger from '@/utils/logger'

const { pagination, resetPagination } = useStandardPage()
const searchForm = ref({
  /** 单号搜索：订单号 / 流水号（后端按 流水号/对方名称/备注/订单ID 模糊匹配） */
  keyword: '',
  bankAccountId: '',
  flowType: '' as '' | 'income' | 'expense',
  dateRange: null as [string, string] | null,
})

/** 当前生效的单号搜索词（用于空态文案，避免用户忘记自己开着筛选） */
const activeKeyword = ref('')
const loading = ref(false)

/** 银行账户列表（用于下拉选择和余额统计） */
const bankAccounts = ref<FinanceBankAccount[]>([])
/** 资金流水列表 */
const records = ref<FundFlow[]>([])
/** 资金流水统计 */
const statistics = ref<FundFlowStatistics | null>(null)
/** 统计区的局部提示（无账户/统计失败），只做弱提示，不弹全局错误 */
const statsHint = ref('')
/** 统计区加载态（不阻塞列表） */
const statsLoading = ref(false)

/** 收支类型 → StatusTag 配置（避免模板内三元映射） */
function getFlowTypeTag(flowType: string): { type: string; label: string } {
  return flowType === 'income'
    ? { type: 'success', label: '收入' }
    : { type: 'warning', label: '支出' }
}

/** 流水状态 → StatusTag 配置（避免模板内三元映射） */
function getFlowStatusTag(status: string): { type: string; label: string } {
  const map: Record<string, { type: string; label: string }> = {
    completed: { type: 'success', label: '已完成' },
    pending: { type: 'warning', label: '待处理' },
    cancelled: { type: 'info', label: '已取消' },
  }
  // 后端 status 已归一为 1..3（后端同时返回 statusName），这里不再出现「未知」兜底
  return map[status] ?? { type: 'warning', label: '待处理' }
}

/** 格式化元金额（保留2位小数，带千分位）
 * 注：bankAccounts.balance、fundFlow.amount/balance、statistics 金额均已被对应 Converter 转为元 */
function formatYuan(yuan: number | undefined | null): string {
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 统计卡片数据 */
const statsCards = ref<Array<{ icon: string; label: string; value: string; colorType: StatColorType }>>([
  { icon: 'Wallet', label: '银行余额', value: '¥0.00', colorType: 'primary' },
  { icon: 'TrendCharts', label: '本月收入', value: '¥0.00', colorType: 'success' },
  { icon: 'DataLine', label: '本月支出', value: '¥0.00', colorType: 'warning' },
  { icon: 'Star', label: '资金周转率', value: '0.00次', colorType: 'info' },
])

const columns: DataTableColumn[] = [
  { prop: 'transactionDate', label: '日期', minWidth: 110 },
  { prop: 'flowType', label: '收支类型', minWidth: 90, slot: 'flowType' },
  { prop: 'summary', label: '摘要', minWidth: 180, showOverflowTooltip: true },
  // 这三列必须声明 slot 才会渲染模板里的具名插槽（否则取 row.income/expense 不存在 → 空白）
  { prop: 'income', label: '收入(元)', minWidth: 120, align: 'right', slot: 'income' },
  { prop: 'expense', label: '支出(元)', minWidth: 120, align: 'right', slot: 'expense' },
  { prop: 'balance', label: '余额(元)', minWidth: 120, align: 'right', slot: 'balance' },
  { prop: 'bankAccountName', label: '银行账户', minWidth: 130 },
  { prop: 'counterparty', label: '对方单位', minWidth: 130 },
  { prop: 'status', label: '状态', minWidth: 95, slot: 'status' },
]

/** 构建查询参数 */
function buildQueryParams() {
  const params: Record<string, unknown> = {
    page: pagination.current,
    size: pagination.size,
  }
  // 单号搜索：trim 后非空才下发（空串由 fund-flow.ts 的 compactParams 再兜底一次）
  const keyword = searchForm.value.keyword.trim()
  if (keyword) {
    params.keyword = keyword
  }
  if (searchForm.value.bankAccountId) {
    params.bankAccountId = searchForm.value.bankAccountId
  }
  if (searchForm.value.flowType) {
    params.flowType = searchForm.value.flowType
  }
  if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
    params.startDate = searchForm.value.dateRange[0]
    params.endDate = searchForm.value.dateRange[1]
  }
  return params
}

/** 加载银行账户列表 */
async function loadBankAccounts(): Promise<void> {
  try {
    const response = await bankAccountApi.getList({ page: 1, size: 100 })
    bankAccounts.value = response?.records || []
  } catch (error) {
    logger.warnMessage('[FinanceFund] 加载银行账户列表失败', error)
    bankAccounts.value = []
  }
}

/**
 * 取银行账户ID（可安全返回空串）
 *
 * 后端 BankAccountVO 的字段名是 `accountId`，而前端 FinanceBankAccount 类型用的是 `id`，
 * 且 BankAccountDataConverter 未做该别名映射 → 运行时 `acc.id` 可能是 undefined。
 * 这里两种字段都兜底，拿不到就返回 ''，调用方据此跳过请求（否则 URL 会拼成
 * /account/undefined/statistics → 后端 400「参数类型不匹配：accountId」）。
 */
function getAccountId(acc: FinanceBankAccount | null | undefined): string {
  if (!acc) return ''
  const raw = (acc as unknown as Record<string, unknown>).id
    ?? (acc as unknown as Record<string, unknown>).accountId
  if (raw === undefined || raw === null) return ''
  const text = String(raw).trim()
  return text === 'undefined' || text === 'null' || text === 'NaN' ? '' : text
}

/** 当月日期区间（后端统计接口 startDate/endDate 必填，yyyy-MM-dd） */
function currentMonthRange(): { startDate: string; endDate: string } {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const monthText = String(month).padStart(2, '0')
  const lastDay = new Date(year, month, 0).getDate()
  return {
    startDate: `${year}-${monthText}-01`,
    endDate: `${year}-${monthText}-${String(lastDay).padStart(2, '0')}`,
  }
}

/** 首个「有有效ID」的银行账户ID（无则空串） */
function firstValidAccountId(): string {
  for (const acc of bankAccounts.value) {
    const id = getAccountId(acc)
    if (id) return id
  }
  return ''
}

/**
 * 加载资金流水统计
 * @param explicitAccountId 显式指定的账户ID（下拉切换时传入）；为空则回退到首个有效账户
 */
async function loadStatistics(explicitAccountId?: string): Promise<void> {
  const accountId = (explicitAccountId || '').trim() || firstValidAccountId()

  // 守卫 1：没有可用账户ID → 不发请求，统计归零并给局部提示
  if (!accountId) {
    statistics.value = null
    statsHint.value = bankAccounts.value.length === 0
      ? '暂无账户数据'
      : '银行账户缺少有效ID，暂无统计数据'
    updateStatsCards()
    return
  }

  statsLoading.value = true
  try {
    const { startDate, endDate } = currentMonthRange()
    if (explicitAccountId) {
      // 指定账户：只看该账户
      statistics.value = await fundFlowApi.getStatisticsByAccount(accountId, startDate, endDate)
      statsHint.value = ''
    } else {
      // 未指定账户（页面初始化）：跨全部账户求和，避免出现「列表里明明有 POS 收入，本月收入却显示 ¥0.00」
      const ids: string[] = bankAccounts.value
        .map((acc) => getAccountId(acc))
        .filter((id): id is string => !!id)
      const parts: (FundFlowStatistics | null)[] = await Promise.all(
        ids.map((id) => fundFlowApi.getStatisticsByAccount(id, startDate, endDate).catch(() => null)),
      )
      const sum: FundFlowStatistics = {
        totalIncome: 0,
        totalExpense: 0,
        netAmount: 0,
        count: 0,
      } as FundFlowStatistics
      for (const part of parts) {
        if (!part) continue
        sum.totalIncome += part.totalIncome || 0
        sum.totalExpense += part.totalExpense || 0
        sum.netAmount += part.netAmount || 0
        sum.count += part.count || 0
      }
      statistics.value = (sum.count > 0 || sum.totalIncome > 0 || sum.totalExpense > 0) ? sum : null
      statsHint.value = ids.length === 0 ? '暂无账户数据' : '以上为全部账户合计'
    }
  } catch (error) {
    // 守卫 2：失败只做弱提示 + 控制台告警，不弹全局红字、不打断整页
    logger.warnMessage('[FinanceFund] 加载资金流水统计失败', error)
    statistics.value = null
    statsHint.value = '统计数据加载失败，已按 0 显示'
  } finally {
    statsLoading.value = false
    updateStatsCards()
  }
}

/** 账户下拉切换：只在有值时才请求统计（清空则回退首个账户/归零） */
watch(
  () => searchForm.value.bankAccountId,
  (value) => {
    const accountId = typeof value === 'string' ? value.trim() : ''
    void loadStatistics(accountId || undefined)
  },
)

/** 更新统计卡片 */
function updateStatsCards(): void {
  // 银行余额：所有账户余额之和（acc.balance 已被 BankAccountDataConverter 转为元）
  const totalBalance = bankAccounts.value.reduce(
    (sum, acc) => sum + (acc.balance || 0),
    0,
  )
  statsCards.value[0].value = `¥${formatYuan(totalBalance)}`

  // 本月收入/支出：来自统计（statistics 已被 toFrontendStatistics 转为元）
  const income = statistics.value?.totalIncome || 0
  const expense = statistics.value?.totalExpense || 0
  statsCards.value[1].value = `¥${formatYuan(income)}`
  statsCards.value[2].value = `¥${formatYuan(expense)}`

  // 资金周转率：支出 / 余额（保留2位小数，两者均已是元）
  const turnoverRate = totalBalance > 0 ? (expense / totalBalance).toFixed(2) : '0.00'
  statsCards.value[3].value = `${turnoverRate}次`
}

/** 加载资金流水列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const response = await fundFlowApi.getList(buildQueryParams())
    records.value = response?.records || []
    pagination.total = response?.total || 0
  } catch (error) {
    logger.warnMessage('[FinanceFund] 加载资金流水列表失败', error)
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 查询（点击「查询」或单号搜索框回车都会走这里） */
function handleSearch(): void {
  activeKeyword.value = searchForm.value.keyword.trim()
  resetPagination()
  loadData()
}

/** 列表空态文案：带上当前搜索词，明确告知「是筛选导致的空」 */
const emptyText = computed<string>(() => {
  if (activeKeyword.value) {
    return `没有匹配「${activeKeyword.value}」的流水，请检查单号或点「重置」`
  }
  return '暂无资金流水数据'
})

/** 重置 */
function handleReset(): void {
  searchForm.value = {
    keyword: '',
    bankAccountId: '',
    flowType: '',
    dateRange: null,
  }
  activeKeyword.value = ''
  resetPagination()
  loadData()
}

/** 每页条数变化 */
function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
  loadData()
}

/** 页码变化 */
function handleCurrentChange(current: number): void {
  pagination.current = current
  loadData()
}

/** 获取收入金额（元，row.amount 已被 FundFlowDataConverter 转为元） */
function getIncome(row: FundFlow): string {
  if (row.flowType === 'income') {
    return formatYuan(row.amount)
  }
  return '0.00'
}

/** 获取支出金额（元，row.amount 已被 FundFlowDataConverter 转为元） */
function getExpense(row: FundFlow): string {
  if (row.flowType === 'expense') {
    return formatYuan(row.amount)
  }
  return '0.00'
}

/** 获取余额（元，row.balance 已被 FundFlowDataConverter 转为元） */
function getBalance(row: FundFlow): string {
  return formatYuan(row.balance)
}

onMounted(async () => {
  await loadBankAccounts()
  await Promise.all([loadData(), loadStatistics()])
  updateStatsCards()
})

/** ==================== 详情对话框（流水详情 + POS 订单穿透） ==================== */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})
/** 详情接口加载态（仅弹窗内 loading，不阻塞整页） */
const detailLoading = ref(false)
/** 从 remark 解析出的关联 POS 订单 */
const posOrderRef = ref<PosOrderRef | null>(null)
/** 关联订单明细（统一视图，金额为元） */
const orderTrace = ref<PosOrderTrace | null>(null)
const orderLoading = ref(false)
/** 订单查询失败时的降级文案（无权限 / 不存在 / 暂不可用） */
const orderErrorText = ref('')

/**
 * 备注展示文案：正文优先（remark → summary），并去掉给机器用的关联 token `[orderId=xxx]`。
 * 内部 ID 对业务人员无意义（与「订单ID 不展示」同一口径），原始值仍保留在 detailData 里供解析使用。
 */
const detailRemarkText = computed<string>(() => {
  const remark = (detailData.value.remark as string) || (detailData.value.summary as string) || ''
  const cleaned = remark.replace(/\[orderId=[^\]]*\]/g, '').replace(/\s{2,}/g, ' ').trim()
  return cleaned || '-'
})

/** 操作人展示：优先姓名/用户名，其次审计列 createdBy/create_by，避免出现 '-' */
const detailOperatorText = computed<string>(() => {
  const d = detailData.value
  const text =
    (d.createUserName as string) ||
    (d.createBy as string) ||
    (d.createdBy as string) ||
    ''
  return String(text).trim() || '-'
})

/** 关联订单菜品小计合计（元） */
const orderItemsTotal = computed<number>(() => {
  const items = orderTrace.value?.items ?? []
  const total = items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
  return Math.round(total * 100) / 100
})

/** 订单状态 → StatusTag 颜色（与系统其它页面同源组件，避免裸文本） */
const orderStatusMeta = computed<{ tag: string; label: string }>(() => {
  const raw = String(orderTrace.value?.status ?? '').trim()
  const normalized = raw.toLowerCase()
  const map: Record<string, { tag: string; label: string }> = {
    pending: { tag: 'warning', label: '待确认' },
    confirmed: { tag: 'primary', label: '已确认' },
    paid: { tag: 'success', label: '已支付' },
    making: { tag: 'warning', label: '制作中' },
    completed: { tag: 'success', label: '已完成' },
    cancelled: { tag: 'info', label: '已取消' },
    partial_refunded: { tag: 'danger', label: '部分退款' },
    refunded: { tag: 'danger', label: '全额退款' },
  }
  if (map[normalized]) return map[normalized]
  // 后端已给中文（如「已完成」）时直接展示，颜色按关键字推断
  if (raw) {
    if (/完成|已支付|已确认/.test(raw)) return { tag: 'success', label: raw }
    if (/退/.test(raw)) return { tag: 'danger', label: raw }
    if (/取消/.test(raw)) return { tag: 'info', label: raw }
    return { tag: 'warning', label: raw }
  }
  return { tag: 'info', label: '-' }
})

/** 支付方式展示：优先后端中文名，其次代码映射，空值统一「未记录」而不是 '-' */
const paymentMethodText = computed<string>(() => {
  const trace = orderTrace.value as (PosOrderTrace & { paymentMethodName?: string }) | null
  const name = String(trace?.paymentMethodName ?? '').trim()
  if (name && name !== '-') return name
  const code = String(trace?.paymentMethod ?? '').trim()
  const codeMap: Record<string, string> = {
    '1': '现金', '2': '微信支付', '3': '支付宝', '4': '银行卡', '5': '余额支付', '0': '未记录',
    cash: '现金', wechat: '微信支付', alipay: '支付宝', card: '银行卡', balance: '余额支付',
  }
  if (codeMap[code]) return codeMap[code]
  if (code && code !== '-') return code
  return '未记录'
})

/** 后端 LocalDateTime.toString() 形如 2026-10-23T22:30:42.064207 → 统一 YYYY-MM-DD HH:mm:ss */
function formatOrderTime(value: unknown): string {
  const text = String(value ?? '').trim()
  if (!text) return '-'
  const normalized = text.replace('T', ' ').replace(/\.\d+$/, '')
  const formatted = formatDateTime(normalized)
  return formatted || normalized
}

/** 查询关联 POS 订单明细（失败只做局部提示，不抛错、不弹全局红字） */
async function loadPosOrderTrace(refInfo: PosOrderRef | null): Promise<void> {
  if (!refInfo) return
  orderLoading.value = true
  try {
    const result = await getPosOrderTrace(refInfo.orderNumber, refInfo.orderId)
    if (result.ok) {
      orderTrace.value = result.trace
      orderErrorText.value = ''
    } else {
      orderTrace.value = null
      orderErrorText.value = result.message
    }
  } catch (error) {
    logger.warnMessage('[FinanceFund] 关联 POS 订单查询异常', error)
    orderTrace.value = null
    orderErrorText.value = '订单明细暂不可用，请稍后重试'
  } finally {
    orderLoading.value = false
  }
}

/** 打开详情对话框：先用列表行渲染，再补拉详情接口 + 联查关联订单 */
async function handleViewDetail(row: Record<string, unknown>): Promise<void> {
  detailData.value = { ...row }
  detailLoading.value = false
  orderTrace.value = null
  orderErrorText.value = ''
  orderLoading.value = false
  // 列表行经 FundFlowDataConverter 转换后 remark 同时存在于 summary/remark
  posOrderRef.value = parsePosOrderRemark(
    (row.summary as string) || (row.remark as string) || '',
  )
  detailDialogVisible.value = true

  const flowId = String(row.id ?? '').trim()
  if (flowId) {
    detailLoading.value = true
    try {
      const detail = (await fundFlowApi.getById(flowId)) as unknown as Record<string, unknown>
      if (detail) {
        detailData.value = { ...row, ...detail }
        const parsed = parsePosOrderRemark(
          (detail.remark as string) || (detail.summary as string) || '',
        )
        if (parsed) posOrderRef.value = parsed
      }
    } catch (error) {
      // 详情接口失败（无权限/已删除等）→ 回落列表行数据，不打断弹窗
      logger.warnMessage('[FinanceFund] 加载流水详情失败，已回落到列表行数据', error)
    } finally {
      detailLoading.value = false
    }
  }

  // 后端已把订单摘要塞进流水详情（posOrder）时直接用，避免再打 3 个端点、也避免权限降级噪音；
  // 字段：orderNumber/orderTime/paymentMethodName/orderStatusName/orderStatusCode/orderAmount/items
  const backendOrder = detailData.value.posOrder as Record<string, unknown> | undefined | null
  if (backendOrder && typeof backendOrder === 'object') {
    const rawItems = (backendOrder.items as Array<Record<string, unknown>> | undefined) ?? []
    orderTrace.value = {
      orderNumber: String(backendOrder.orderNumber ?? posOrderRef.value?.orderNumber ?? ''),
      orderId: '',
      createTime: String(backendOrder.orderTime ?? ''),
      paymentMethod: String(backendOrder.paymentMethodName ?? ''),
      paymentMethodName: String(backendOrder.paymentMethodName ?? ''),
      status: String(backendOrder.orderStatusName ?? backendOrder.orderStatusCode ?? ''),
      totalAmount: Number(backendOrder.orderAmount ?? 0),
      items: rawItems.map((it) => ({
        name: String(it.name ?? ''),
        price: Number(it.price ?? 0),
        quantity: Number(it.quantity ?? 0),
        amount: Number(it.amount ?? 0),
      })),
    } as unknown as PosOrderTrace
    orderErrorText.value = ''
    orderLoading.value = false
    return
  }

  if (posOrderRef.value) {
    await loadPosOrderTrace(posOrderRef.value)
  }
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="资金管理" description="银行账户与资金流水管理" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div v-if="statsHint" class="stats-hint">
      <el-icon class="stats-hint__icon"><InfoFilled /></el-icon>
      <span>{{ statsHint }}</span>
      <span v-if="statsLoading" class="stats-hint__loading">统计加载中...</span>
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-input
        v-model="searchForm.keyword"
        placeholder="订单号 / 流水号，模糊搜索"
        clearable
        size="default"
        style="width:230px"
        class="keyword-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchForm.bankAccountId" placeholder="银行账户" clearable style="width:180px" size="default">
        <el-option v-for="acc in bankAccounts" :key="getAccountId(acc)" :label="`${acc.bankName}-${acc.accountName}`" :value="getAccountId(acc)" />
      </el-select>
      <el-select v-model="searchForm.flowType" placeholder="收支类型" clearable style="width:130px" size="default">
        <el-option label="收入" value="income" />
        <el-option label="支出" value="expense" />
      </el-select>
      <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" placement="bottom-start" style="width:240px;max-width:240px;flex-shrink:0" size="default" />
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="records" :loading="loading" :empty-text="emptyText" stripe>
        <template #flowType="{ row }"><StatusTag :status="getFlowTypeTag(row.flowType).type" :label="getFlowTypeTag(row.flowType).label" size="small" /></template>
        <template #income="{ row }">{{ getIncome(row) }}</template>
        <template #expense="{ row }">{{ getExpense(row) }}</template>
        <template #balance="{ row }">{{ getBalance(row) }}</template>
        <template #status="{ row }"><StatusTag :status="getFlowStatusTag(row.status).type" :label="row.statusName || getFlowStatusTag(row.status).label" size="small" /></template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
    <!-- 详情对话框：流水详情 + 关联 POS 订单穿透 -->
    <el-dialog v-model="detailDialogVisible" title="资金流水详情" width="760px" class="fund-detail-dialog">
      <div v-loading="detailLoading" class="fund-detail">
        <el-descriptions :column="2" border label-class-name="detail-label">
          <el-descriptions-item label="流水号">{{ (detailData.flowNo as string) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易日期">{{ (detailData.transactionDate as string) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收支类型">
            <StatusTag :status="getFlowTypeTag(detailData.flowType as string).type" :label="getFlowTypeTag(detailData.flowType as string).label" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="流水状态">
            <StatusTag :status="getFlowStatusTag(detailData.status as string).type" :label="(detailData.statusName as string) || getFlowStatusTag(detailData.status as string).label" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="金额(元)">{{ formatYuan(detailData.amount as number) }}</el-descriptions-item>
          <el-descriptions-item label="交易后余额(元)">{{ getBalance(detailData as unknown as FundFlow) }}</el-descriptions-item>
          <el-descriptions-item label="银行账户">{{ (detailData.bankAccountName as string) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对方单位">{{ (detailData.counterparty as string) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="凭证号">{{ (detailData.voucherNo as string) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detailOperatorText }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            <span class="fund-detail__remark">{{ detailRemarkText }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="order-section">
          <div class="order-section__title">关联 POS 订单</div>

          <template v-if="posOrderRef">
            <div v-if="orderLoading" class="order-loading">正在查询订单明细...</div>

            <template v-else-if="orderTrace">
              <!-- 统一网格：2 列、带边框、标签列固定宽度，保证「订单号/下单时间/支付方式/订单状态/订单金额」严格对齐。
                   内部订单ID 属系统内部标识，对业务人员无意义，界面上不展示。 -->
              <el-descriptions
                :column="2"
                border
                size="default"
                class="order-meta"
                label-class-name="detail-label"
                content-class-name="detail-content"
              >
                <el-descriptions-item label="订单号">
                  <span class="mono">{{ posOrderRef.orderNumber || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="下单时间">{{ formatOrderTime(orderTrace.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="支付方式">{{ paymentMethodText }}</el-descriptions-item>
                <el-descriptions-item label="订单状态">
                  <StatusTag :status="orderStatusMeta.tag" :label="orderStatusMeta.label" size="small" />
                </el-descriptions-item>
                <el-descriptions-item label="订单金额(元)" :span="2">
                  <span class="amount-strong">{{ formatYuan(orderTrace.totalAmount) }}</span>
                </el-descriptions-item>
              </el-descriptions>

              <table v-if="orderTrace.items.length" class="order-items-table">
                <thead>
                  <tr><th>菜品</th><th>单价(元)</th><th>数量</th><th>小计(元)</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in orderTrace.items" :key="idx">
                    <td>{{ item.name }}</td>
                    <td class="num">{{ formatYuan(item.price) }}</td>
                    <td class="num">{{ item.quantity }}</td>
                    <td class="num">{{ formatYuan(item.amount) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr>
                    <td colspan="3" class="num">菜品合计</td>
                    <td class="num">{{ formatYuan(orderItemsTotal) }}</td>
                  </tr>
                </tfoot>
              </table>
              <div v-else class="order-empty">该订单无菜品明细</div>
            </template>

            <el-alert
              v-else-if="orderErrorText"
              :title="orderErrorText"
              type="info"
              :closable="false"
              show-icon
            />
          </template>

          <div v-else class="order-empty">
            该流水未关联 POS 订单（非 POS 销售入账，或备注缺少关联信息）
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stats-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -4px 0 8px;
  padding: 6px 12px;
  border-radius: 8px;
  background: var(--el-color-info-light-9, #f4f4f5);
  color: var(--el-color-info, #909399);
  font-size: 12px;
  line-height: 1.5;
}

.stats-hint__icon {
  font-size: 14px;
}

.stats-hint__loading {
  margin-left: 4px;
  opacity: 0.8;
}

/* 空态文案：默认 el-table__empty-text 宽度只有 50%，长文案会被折成两行 */
:deep(.el-table__empty-text) {
  width: 100% !important;
  max-width: none;
  line-height: 1.8;
  padding: 0 24px;
  white-space: normal;
}

/* ====== 详情弹窗：流水 + 关联订单 ====== */
.fund-detail {
  min-height: 80px;
}

.fund-detail__remark {
  word-break: break-all;
}

.order-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color, #dcdfe6);
}

.order-section__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
  margin-bottom: 10px;
}

.order-meta {
  margin-top: 10px;
}

/* 统一网格：标签列固定宽度（12em），内容列等分，保证上下两个 descriptions 与明细表左边界对齐 */
.order-meta :deep(.detail-label) {
  width: 12em;
  min-width: 12em;
  color: var(--el-text-color-regular, #606266);
  background: var(--el-fill-color-light, #f5f7fa);
  font-weight: 500;
  white-space: nowrap;
}

.order-meta :deep(.detail-content) {
  color: var(--el-text-color-primary, #303133);
}

.order-meta :deep(.el-descriptions__table) {
  table-layout: fixed;
  width: 100%;
}

.order-meta :deep(.el-descriptions__cell) {
  padding: 8px 12px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  letter-spacing: 0.02em;
}

.amount-strong {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.order-loading {
  padding: 12px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}

.order-empty {
  padding: 12px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}

.order-items-table {
  width: 100%;
  /* 与上方 descriptions 同一边界与间距 */
  margin: 12px 0 0;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
}

.order-items-table th,
.order-items-table td {
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  padding: 6px 10px;
  text-align: left;
}

.order-items-table th {
  background: var(--el-fill-color-light, #f5f7fa);
  font-weight: 600;
  color: var(--el-text-color-regular, #606266);
}

.order-items-table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.order-items-table tfoot td {
  font-weight: 600;
  background: var(--el-fill-color-lighter, #fafafa);
}
</style>
