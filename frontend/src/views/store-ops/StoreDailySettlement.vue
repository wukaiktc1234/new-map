<script setup lang="ts">
/**
 * 门店日结页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成门店每日结算对账的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 重构要点：
 * ✅ 使用 .modern-page + PageHeader 替代 StandardPage
 * ✅ 使用 stats-section 响应式4列网格 + variant="bordered"
 * ✅ 使用 table-section + pagination-wrapper 分页分离模式
 * ✅ 使用 CSS 变量（无硬编码颜色）
 * ✅ 符合 project_rules.md 规范
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, Money, Document, Warning, CircleCheck } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { settlementApi } from '@/api/store-ops/settlement'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { fenToYuan } from '@/utils/money'
import type { DailySettlement, SettlementQueryParams, SettlementStatus } from '@/types/store-operation'
import type { StoreOption } from '@/types/store-operation/store-archive'

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

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<DailySettlement[]>([])
const detailDialogVisible = ref(false)
const settlementDialogVisible = ref(false)
const currentDetailRow = ref<DailySettlement | null>(null)
const detailLoading = ref(false)
const exportLoading = ref(false)

// 门店选项（从真实 API 加载，使用共享 composable）
const { storeOptions, loadStores, getStoreName } = useStoreOptions(true)

// 查询表单
const queryForm = ref({
  storeId: '' as number | string,
  dateRange: null as [string, string] | null,
  status: '' as '' | SettlementStatus,
  keyword: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<DailySettlement, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: SettlementQueryParams = {
        page: params.page,
        size: params.size,
        storeId: params.storeId ? String(params.storeId) : undefined,
        dateRange: params.dateRange,
        status: params.status || undefined,
      }
      return settlementApi.getSettlementList(queryParams)
    },
  } as unknown as CrudApi<DailySettlement, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  const todayRecords = tableData.value.filter(r => r.settlementDate === today)

  const todayRevenue = todayRecords.reduce((sum, r) => sum + r.totalRevenue, 0)
  const todayOrders = todayRecords.reduce((sum, r) => sum + r.orderCount, 0)
  const todayRefund = todayRecords.reduce((sum, r) => sum + (r.refundAmount ? r.refundAmount / 100 : 0), 0)
  const pendingCount = tableData.value.filter(r => r.status === 'pending').length

  return {
    todayRevenue,
    todayOrders,
    todayRefund,
    pendingCount,
  }
})

// 日结表单数据
const settlementForm = reactive({
  storeId: '',
  settlementDate: '',
  remark: '',
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'settlementDate', label: '日结日期', minWidth: 120, slot: 'settlementDate' },
  { prop: 'storeName', label: '门店', minWidth: 120, slot: 'storeName' },
  { prop: 'totalRevenue', label: '营业额', minWidth: 110, align: 'right', slot: 'totalRevenue' },
  { prop: 'orderCount', label: '订单数', minWidth: 90, align: 'center', slot: 'orderCount' },
  { prop: 'refundAmount', label: '退款金额', minWidth: 110, align: 'right', slot: 'refundAmount' },
  { prop: 'netProfit', label: '实收金额', minWidth: 110, align: 'right', slot: 'netAmount' },
  { prop: 'auditorName', label: '收银员', minWidth: 100, slot: 'cashier' },
  { prop: 'status', label: '日结状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'confirmTime', label: '日结时间', minWidth: 160, slot: 'confirmTime' },
])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.storeId = ''
  queryForm.value.dateRange = null
  queryForm.value.status = ''
  queryForm.value.keyword = ''
  refresh()
}

function handleSelectionChange(rows: DailySettlement[]) {
  selectedRows.value = rows
}

/** 查看日结详情 */
async function handleViewDetail(row: DailySettlement) {
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    const detail = await settlementApi.getSettlementDetail(row.settlementId)
    currentDetailRow.value = detail
  } catch {
    ElMessage.error('加载日结详情失败')
  } finally {
    detailLoading.value = false
  }
}

/** 打开日结操作对话框 */
function handleSettlement(row?: DailySettlement) {
  if (row) {
    settlementForm.storeId = row.storeId
    settlementForm.settlementDate = row.settlementDate
  } else {
    settlementForm.storeId = ''
    settlementForm.settlementDate = new Date().toISOString().split('T')[0]
  }
  settlementForm.remark = ''
  settlementDialogVisible.value = true
}

/** 执行日结操作 */
async function handleConfirmSettlement() {
  if (!settlementForm.storeId) {
    ElMessage.warning('请选择门店')
    return
  }
  if (!settlementForm.settlementDate) {
    ElMessage.warning('请选择日结日期')
    return
  }

  submitLoading.value = true
  try {
    const result = await settlementApi.createSettlement({
      storeId: Number(settlementForm.storeId),
      settlementDate: settlementForm.settlementDate,
      remark: settlementForm.remark,
    })

    if (result.success) {
      ElMessage.success(result.message || '日结成功')
      settlementDialogVisible.value = false
      refresh()
    } else {
      ElMessage.error(result.message || '日结失败')
    }
  } catch {
    ElMessage.error('日结操作失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

/** 确认对账 */
async function handleConfirm(row: DailySettlement) {
  if (row.status === 'approved') {
    ElMessage.warning('该日结已确认，无需重复操作')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认 ${row.storeName} ${row.settlementDate} 的日结数据？确认后不可修改。`,
      '确认日结',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    const result = await settlementApi.confirmSettlement({
      settlementId: row.settlementId,
      auditorRemark: '',
    })

    if (result.success) {
      ElMessage.success(result.message || '确认成功')
      refresh()
    } else {
      ElMessage.error(result.message || '确认失败')
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败，请重试')
    }
  }
}

/** 打印日结单 */
function handlePrint(row: DailySettlement) {
  ElMessage.info(`正在打印 ${row.storeName} ${row.settlementDate} 的日结单...`)
}

/** 导出日结报表 */
async function handleExport() {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const params: SettlementQueryParams = {
      page: pagination.current,
      size: pagination.pageSize,
      storeId: queryForm.value.storeId ? String(queryForm.value.storeId) : undefined,
      dateRange: queryForm.value.dateRange,
      status: queryForm.value.status || undefined,
    }

    const success = await settlementApi.exportSettlements(params)

    if (success) {
      ElMessage.success('导出成功！文件已开始下载')
    } else {
      ElMessage.error('导出失败，请稍后重试')
    }
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'error',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待日结',
    approved: '已日结',
    rejected: '已驳回',
  }
  return map[status] || status
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '-'
  return `¥${amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 修复：移除本地 fenToYuan 函数，改用 @/utils/money 导入的全局 fenToYuan 工具
// 原本地函数包含 (fen / 100).toFixed(2) 金额转换，违反金额转换规范

// 门店选项由 useStoreOptions autoLoad 自动加载，表格由 useCrudTable autoLoad 自动加载
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="日结对账" description="管理门店每日结算对账">
      <el-button type="primary" size="default" @click="handleSettlement()">
        <el-icon :size="16"><Money /></el-icon>日结操作
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Money" label="今日营业额" :value="formatAmount(statistics.todayRevenue)" color-type="primary" variant="bordered" />
      <StatCard icon="Document" label="今日订单数" :value="`${statistics.todayOrders} 单`" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="今日退款" :value="formatAmount(statistics.todayRefund)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="待日结门店" :value="String(statistics.pendingCount)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 操作按钮） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.storeId"
            placeholder="门店选择"
            clearable
            style="width: 160px"
            size="default"
            filterable
            @change="handleSearch"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="store.storeId"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            placement="bottom-start"
            style="width: 240px"
            size="default"
            @change="handleSearch"
          />
          <el-select
            v-model="queryForm.status"
            placeholder="结算状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="待日结" value="pending" />
            <el-option label="已日结" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索门店名称..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="success"
            size="default"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出日结报表
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
        :actions-width="240"
        @selection-change="handleSelectionChange"
      >
        <!-- 日结日期列 -->
        <template #settlementDate="{ row }">
          <span class="date-text">{{ row.settlementDate }}</span>
        </template>

        <!-- 门店列 -->
        <template #storeName="{ row }">
          <span class="store-text">{{ row.storeName }}</span>
        </template>

        <!-- 营业额列 -->
        <template #totalRevenue="{ row }">
          <span class="amount-text revenue-text">{{ formatAmount(row.totalRevenue) }}</span>
        </template>

        <!-- 订单数列 -->
        <template #orderCount="{ row }">
          <span class="count-text">{{ row.orderCount }}</span>
        </template>

        <!-- 退款金额列 -->
        <template #refundAmount="{ row }">
          <span class="amount-text refund-text" v-if="row.refundAmount && row.refundAmount > 0">
            -¥{{ fenToYuan(row.refundAmount) }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>

        <!-- 实收金额列 -->
        <template #netAmount="{ row }">
          <span class="amount-text net-text">{{ formatAmount(row.netProfit) }}</span>
        </template>

        <!-- 收银员列 -->
        <template #cashier="{ row }">
          <span class="cashier-text">{{ row.auditorName || '-' }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusColor(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 日结时间列 -->
        <template #confirmTime="{ row }">
          <span class="time-text">{{ formatTime(row.confirmTime || row.createTime) }}</span>
        </template>

        <!-- 操作列（DataTable 自动追加） -->
        <template #actions="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status !== 'approved'"
              link
              type="success"
              size="default"
              @click.stop="handleConfirm(row)"
            >
              日结
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
          @size-change="refresh"
          @current-change="refresh"
        />
      </div>
    </section>

    <!-- 日结详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="日结详情"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
      v-loading="detailLoading"
    >
      <div v-if="currentDetailRow" class="detail-content">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="门店名称">{{ currentDetailRow.storeName }}</el-descriptions-item>
            <el-descriptions-item label="日结日期">{{ currentDetailRow.settlementDate }}</el-descriptions-item>
            <el-descriptions-item label="日结状态">
              <StatusTag :status="getStatusColor(currentDetailRow.status)" :label="getStatusLabel(currentDetailRow.status)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="收银员">{{ currentDetailRow.auditorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="日结时间">{{ formatTime(currentDetailRow.confirmTime) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(currentDetailRow.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 销售汇总 -->
        <div class="detail-section">
          <div class="section-title">销售汇总</div>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="summary-card">
                <div class="summary-label">总营业额</div>
                <div class="summary-value primary">{{ formatAmount(currentDetailRow.totalRevenue) }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-card">
                <div class="summary-label">订单数</div>
                <div class="summary-value success">{{ currentDetailRow.orderCount }} 单</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-card">
                <div class="summary-label">退款金额</div>
                <div class="summary-value warning">-¥{{ fenToYuan(currentDetailRow.refundAmount || 0) }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-card">
                <div class="summary-label">实收金额</div>
                <div class="summary-value error">{{ formatAmount(currentDetailRow.netProfit) }}</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 收款明细 -->
        <div class="detail-section">
          <div class="section-title">收款明细</div>
          <el-table :data="currentDetailRow.paymentBreakdown ? Object.entries(currentDetailRow.paymentBreakdown).map(([key, val]) => ({ method: key, ...val })) : []" size="small" border>
            <el-table-column label="支付方式" prop="method">
              <template #default="{ row }">
                {{ { cash: '现金', wechat: '微信支付', alipay: '支付宝', memberBalance: '会员卡' }[row.method] || row.method }}
              </template>
            </el-table-column>
            <el-table-column label="金额(元)" align="right">
              <template #default="{ row }">¥{{ fenToYuan(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="笔数" align="center" prop="count" />
          </el-table>
        </div>

        <!-- 退款明细 -->
        <div class="detail-section">
          <div class="section-title">退款明细</div>
          <el-table :data="[]" size="small" border empty-text="暂无退款记录">
            <el-table-column label="退款单号" min-width="160" />
            <el-table-column label="退款时间" min-width="160" />
            <el-table-column label="退款金额" align="right" width="120" />
            <el-table-column label="退款原因" min-width="150" />
            <el-table-column label="操作人" width="100" />
          </el-table>
        </div>

        <!-- 交接班记录 -->
        <div class="detail-section">
          <div class="section-title">交接班记录</div>
          <el-table :data="[]" size="small" border empty-text="暂无交接班记录">
            <el-table-column label="班次" width="100" align="center" />
            <el-table-column label="时间范围" width="180" />
            <el-table-column label="收银员" width="120" />
            <el-table-column label="营业额" align="right" width="120" />
            <el-table-column label="订单数" align="center" width="100" />
            <el-table-column label="交接时间" min-width="160" />
          </el-table>
        </div>

        <!-- 备注 -->
        <div class="detail-section" v-if="currentDetailRow.remark">
          <div class="section-title">备注</div>
          <div class="remark-text">{{ currentDetailRow.remark }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentDetailRow?.status !== 'approved'"
          type="primary"
          @click="handleConfirm(currentDetailRow!); detailDialogVisible = false"
        >
          确认日结
        </el-button>
      </template>
    </el-dialog>

    <!-- 日结操作对话框 -->
    <el-dialog
      v-model="settlementDialogVisible"
      title="日结操作"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="settlementForm" label-width="100px">
        <el-form-item label="门店" required>
          <el-select
            v-model="settlementForm.storeId"
            placeholder="请选择门店"
            filterable
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="String(store.storeId)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日结日期" required>
          <el-date-picker
            v-model="settlementForm.settlementDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            :teleported="false"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="settlementForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="settlementDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleConfirmSettlement">
          确认日结
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

// 金额样式
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
}

.revenue-text {
  color: var(--fts-text-primary);
}

.refund-text {
  color: var(--fts-warning);
}

.net-text {
  color: var(--fts-primary);
}

// 日期/文本样式
.date-text,
.store-text,
.cashier-text {
  color: var(--fts-text-primary);
}

.count-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.text-muted {
  color: var(--fts-text-tertiary);
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
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

.detail-section {
  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

// 销售汇总卡片
.summary-card {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-4);
  text-align: center;

  .summary-label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    margin-bottom: var(--fts-space-2);
  }

  .summary-value {
    font-size: var(--fts-font-size-xl);
    font-weight: var(--fts-font-weight-bold);
    font-variant-numeric: tabular-nums;

    &.primary {
      color: var(--fts-primary);
    }

    &.success {
      color: var(--fts-success);
    }

    &.warning {
      color: var(--fts-warning);
    }

    &.error {
      color: var(--fts-error);
    }
  }
}

.remark-text {
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
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
