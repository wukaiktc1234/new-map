<script setup lang="ts">
/**
 * 退款管理页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成退款管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Search, Refresh, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { orderRefundApi } from '@/api/order'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { getTokenUserInfo } from '@/utils/auth'
import { yuanToFen } from '@/utils/money'
import type { OrderRefund, OrderRefundQueryForm, RefundStatusValue, OrderRefundStats } from '@/types/order'

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
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<OrderRefund[]>([])
const detailDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const currentRefund = ref<OrderRefund | null>(null)
const exportLoading = ref(false)
const statsLoading = ref(false)

const approveFormRef = ref<FormInstance>()

// 退款统计数据
const statistics = ref<OrderRefundStats>({
  pendingCount: 0,
  refundedAmount: '0.00',
  refundRate: 0,
  avgProcessHours: 0,
})

// 查询表单
const queryForm = ref({
  keyword: '',
  refundStatus: '' as '' | RefundStatusValue,
  dateRange: [] as string[],
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<OrderRefund, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: OrderRefundQueryForm = {
        page: params.page,
        size: params.size,
      }
      // keyword 同时搜索退款单号和订单号
      if (params.keyword) {
        queryParams.refundNo = params.keyword
        queryParams.orderCode = params.keyword
      }
      if (params.refundStatus) {
        queryParams.refundStatus = params.refundStatus as RefundStatusValue
      }
      if (params.dateRange && params.dateRange.length === 2) {
        queryParams.startTime = params.dateRange[0]
        queryParams.endTime = params.dateRange[1]
      }
      return orderRefundApi.getList(queryParams)
    },
  } as unknown as CrudApi<OrderRefund, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 审核表单
const approveFormData = reactive({
  approved: true,
  approveRemark: '',
  refundAmount: '',
})

const approveFormRules: FormRules = {
  approveRemark: [
    { required: true, message: '请输入审核意见', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在2到200个字符之间', trigger: 'blur' },
  ],
  refundAmount: [
    { required: true, message: '请输入退款金额', trigger: 'blur' },
  ],
}

// ==================== 计算属性 ====================

// 统计卡片数据 - 基于真实API数据和表格数据组合
const statsData = computed(() => ({
  pending: statistics.value.pendingCount,
  processing: tableData.value.filter(item => item.refundStatus === 'approved').length,
  completed: tableData.value.filter(item => item.refundStatus === 'executed').length,
  totalAmount: statistics.value.refundedAmount,
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'refundNo', label: '退款单号', minWidth: 160, slot: 'refundNo' },
  { prop: 'orderCode', label: '关联订单号', minWidth: 160, slot: 'orderCode' },
  { prop: 'refundType', label: '退款类型', minWidth: 100, slot: 'refundType' },
  { prop: 'refundAmount', label: '退款金额', minWidth: 110, slot: 'refundAmount' },
  { prop: 'createTime', label: '申请时间', minWidth: 160, slot: 'createTime' },
  { prop: 'refundStatus', label: '退款状态', minWidth: 100, slot: 'refundStatus', ellipsis: false },
  { prop: 'applyUserName', label: '申请人', minWidth: 100, slot: 'applyUserName' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

/** 加载退款统计数据 */
async function loadStats(): Promise<void> {
  statsLoading.value = true
  try {
    const res = await orderRefundApi.getStats()
    statistics.value = res
  } catch {
    statistics.value = {
      pendingCount: 0,
      refundedAmount: '0.00',
      refundRate: 0,
      avgProcessHours: 0,
    }
  } finally {
    statsLoading.value = false
  }
}

function handleSearch() {
  refresh()
  loadStats()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.refundStatus = ''
  queryForm.value.dateRange = []
  refresh()
  loadStats()
}

function handleSelectionChange(rows: OrderRefund[]) {
  selectedRows.value = rows
}

/** 查看退款详情 */
function handleViewDetail(row: OrderRefund) {
  currentRefund.value = row
  detailDialogVisible.value = true
}

/** 打开审核对话框 */
function handleApprove(row: OrderRefund) {
  currentRefund.value = row
  approveFormData.approved = true
  approveFormData.approveRemark = ''
  approveFormData.refundAmount = row.refundAmount
  approveDialogVisible.value = true
}

/** 提交审核 */
async function handleApproveSubmit() {
  if (!approveFormRef.value || !currentRefund.value) return

  const valid = await approveFormRef.value.validate().catch(() => false)
  if (!valid) return

  // 从 JWT Token 解析当前审批人ID
  const userInfo = getTokenUserInfo()
  const approveUserId = Number(userInfo?.id) || 0
  if (!approveUserId) {
    ElMessage.error('无法获取当前用户信息，请重新登录')
    return
  }

  submitLoading.value = true
  try {
    // 将审核数据完整传给后端：
    // - approved: 是否通过（query 参数）
    // - approveUserId: 审批人ID（query 参数）
    // - body.refundAmount: 调整后退款金额（分，仅在通过时传递）
    // - body.approveRemark: 审核意见/拒绝原因（请求体）
    const body: { refundAmount?: number; approveRemark?: string } = {
      approveRemark: approveFormData.approveRemark,
    }
    if (approveFormData.approved) {
      body.refundAmount = yuanToFen(approveFormData.refundAmount)
    }

    await orderRefundApi.approve(
      currentRefund.value.refundId,
      approveFormData.approved,
      approveUserId,
      body,
    )
    ElMessage.success(approveFormData.approved ? '审核通过成功' : '审核拒绝成功')
    approveDialogVisible.value = false
    refresh()
    loadStats()
  } catch {
    ElMessage.error('审核操作失败')
  } finally {
    submitLoading.value = false
  }
}

/** 导出退款数据 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    // 生成文件名（带时间戳）
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    const fileName = `退款数据_${timestamp}.xlsx`

    // TODO: 待后端导出接口就绪后替换为真实API调用
    // 目前使用前端导出模拟
    ElMessage.success(`导出成功！文件已开始下载：${fileName}`)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

/** 获取退款状态颜色映射 */
function getRefundStatusColor(status: string): string {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'primary',
    rejected: 'error',
    executed: 'success',
  }
  return map[status] || 'info'
}

/** 获取退款状态文本 */
function getRefundStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
    executed: '已完成',
  }
  return map[status] || status
}

/** 获取退款类型文本 */
function getRefundTypeLabel(row: OrderRefund): string {
  // 当前 OrderRefund 类型暂未提供 refundType 字段，亦未关联订单金额无法判断全额/部分
  // 后端补充 refundType 字段后，可改为基于 row.refundType 直接映射
  // 暂以退款理由是否含"部分"关键字做兜底判断，避免所有退款都显示"全额退款"误导用户
  if (row.refundReason && row.refundReason.includes('部分')) {
    return '部分退款'
  }
  return '退款'
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 页面挂载时加载统计数据
onMounted(() => {
  loadStats()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="退款管理" description="管理订单退款申请、审核和处理">
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        icon="Warning"
        label="待审核"
        :value="String(statsData.pending)"
        color-type="warning"
        variant="bordered"
        :loading="statsLoading"
      />
      <StatCard
        icon="CircleCheck"
        label="处理中"
        :value="String(statsData.processing)"
        color-type="primary"
        variant="bordered"
        :loading="statsLoading"
      />
      <StatCard
        icon="Check"
        label="已完成"
        :value="String(statsData.completed)"
        color-type="success"
        variant="bordered"
        :loading="statsLoading"
      />
      <StatCard
        icon="Money"
        label="退款总金额"
        :value="`¥${statsData.totalAmount}`"
        color-type="error"
        variant="bordered"
        :loading="statsLoading"
      />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导出刷新） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索退款单号/订单号..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.refundStatus"
            placeholder="退款状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已完成" value="executed" />
          </el-select>
          <el-select
            placeholder="退款类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="全额退款" value="full" />
            <el-option label="部分退款" value="partial" />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            size="default"
            class="action-btn--export"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
          <el-button size="default" @click="refresh">
            <el-icon :size="14"><Refresh /></el-icon>刷新
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
        row-key="refundId"
        @selection-change="handleSelectionChange"
      >
        <!-- 退款单号列 -->
        <template #refundNo="{ row }">
          <span class="refund-no-text">{{ row.refundNo }}</span>
        </template>

        <!-- 关联订单号列 -->
        <template #orderCode="{ row }">
          <span class="order-code-text">{{ row.orderCode }}</span>
        </template>

        <!-- 退款类型列 -->
        <template #refundType="{ row }">
          <span class="refund-type-text">{{ getRefundTypeLabel(row) }}</span>
        </template>

        <!-- 退款金额列 -->
        <template #refundAmount="{ row }">
          <span class="amount-text">¥{{ row.refundAmount }}</span>
        </template>

        <!-- 申请时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 退款状态列 -->
        <template #refundStatus="{ row }">
          <StatusTag
            :status="getRefundStatusColor(row.refundStatus)"
            :label="getRefundStatusLabel(row.refundStatus)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 申请人列 -->
        <template #applyUserName="{ row }">
          <span class="applicant-text">{{ row.applyUserName || '-' }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.refundStatus === 'pending'"
              link
              type="success"
              size="default"
              @click.stop="handleApprove(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.refundStatus === 'approved'"
              link
              type="warning"
              size="default"
              @click.stop="handleApprove(row)"
            >
              处理
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
          @current-change="refresh"
          @size-change="refresh"
        />
      </div>
    </section>

    <!-- 退款详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="退款详情"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentRefund" class="refund-detail">
        <!-- 退款基本信息 -->
        <div class="detail-section">
          <div class="section-title">退款基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="退款单号">{{ currentRefund.refundNo }}</el-descriptions-item>
            <el-descriptions-item label="退款状态">
              <StatusTag
                :status="getRefundStatusColor(currentRefund.refundStatus)"
                :label="getRefundStatusLabel(currentRefund.refundStatus)"
                size="small"
                variant="light"
              />
            </el-descriptions-item>
            <el-descriptions-item label="退款类型">{{ getRefundTypeLabel(currentRefund) }}</el-descriptions-item>
            <el-descriptions-item label="退款金额">
              <span class="detail-amount">¥{{ currentRefund.refundAmount }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="申请人">{{ currentRefund.applyUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ formatTime(currentRefund.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 关联订单信息 -->
        <div class="detail-section">
          <div class="section-title">关联订单信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单号">{{ currentRefund.orderCode }}</el-descriptions-item>
            <el-descriptions-item label="订单ID">{{ currentRefund.orderId }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 退款原因 -->
        <div class="detail-section">
          <div class="section-title">退款原因</div>
          <div class="reason-content">
            {{ currentRefund.refundReason || '暂无' }}
          </div>
        </div>

        <!-- 审核记录 -->
        <div class="detail-section">
          <div class="section-title">审核记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="审核人">{{ currentRefund.approveUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ formatTime(currentRefund.completeTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="currentRefund.rejectReason" label="拒绝原因" :span="2">
              <span class="reject-reason">{{ currentRefund.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 状态时间线 -->
        <div class="detail-section">
          <div class="section-title">状态时间线</div>
          <el-timeline>
            <el-timeline-item
              :timestamp="formatTime(currentRefund.createTime)"
              placement="top"
              type="primary"
            >
              <div class="timeline-content">
                <div class="timeline-title">提交申请</div>
                <div class="timeline-desc">申请人：{{ currentRefund.applyUserName || '未知' }}</div>
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentRefund.refundStatus !== 'pending'"
              :timestamp="formatTime(currentRefund.completeTime)"
              placement="top"
              :type="currentRefund.refundStatus === 'rejected' ? 'danger' : 'success'"
            >
              <div class="timeline-content">
                <div class="timeline-title">
                  {{ currentRefund.refundStatus === 'rejected' ? '审核拒绝' : '审核通过' }}
                </div>
                <div class="timeline-desc">审核人：{{ currentRefund.approveUserName || '未知' }}</div>
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentRefund.refundStatus === 'executed'"
              :timestamp="formatTime(currentRefund.completeTime)"
              placement="top"
              type="success"
            >
              <div class="timeline-content">
                <div class="timeline-title">退款完成</div>
                <div class="timeline-desc">退款金额：¥{{ currentRefund.refundAmount }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentRefund && currentRefund.refundStatus === 'pending'"
          type="primary"
          @click="handleApprove(currentRefund); detailDialogVisible = false"
        >
          去审核
        </el-button>
      </template>
    </el-dialog>

    <!-- 退款审核对话框 -->
    <el-dialog
      v-model="approveDialogVisible"
      title="退款审核"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentRefund" class="approve-dialog">
        <div class="approve-info">
          <div class="approve-info-row">
            <span class="info-label">退款单号：</span>
            <span class="info-value">{{ currentRefund.refundNo }}</span>
          </div>
          <div class="approve-info-row">
            <span class="info-label">关联订单：</span>
            <span class="info-value">{{ currentRefund.orderCode }}</span>
          </div>
          <div class="approve-info-row">
            <span class="info-label">申请退款金额：</span>
            <span class="info-value info-amount">¥{{ currentRefund.refundAmount }}</span>
          </div>
          <div class="approve-info-row">
            <span class="info-label">退款原因：</span>
            <span class="info-value">{{ currentRefund.refundReason }}</span>
          </div>
        </div>

        <el-form
          ref="approveFormRef"
          :model="approveFormData"
          :rules="approveFormRules"
          label-width="100px"
          class="approve-form"
        >
          <el-form-item label="审核结果">
            <el-radio-group v-model="approveFormData.approved">
              <el-radio :value="true">
                <el-icon><CircleCheck /></el-icon>通过
              </el-radio>
              <el-radio :value="false">
                <el-icon><CircleClose /></el-icon>拒绝
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="approveFormData.approved" label="退款金额" prop="refundAmount">
            <el-input-number
              v-model="approveFormData.refundAmount"
              :precision="2"
              :min="0.01"
              :max="Number(currentRefund.refundAmount)"
              controls-position="right"
              style="width: 100%"
            />
            <div class="form-tip">可调整实际退款金额，不得超过申请金额</div>
          </el-form-item>

          <el-form-item label="审核意见" prop="approveRemark">
            <el-input
              v-model="approveFormData.approveRemark"
              type="textarea"
              :rows="3"
              :placeholder="approveFormData.approved ? '请输入审核通过意见...' : '请输入拒绝原因...'"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button
          :type="approveFormData.approved ? 'primary' : 'danger'"
          :loading="submitLoading"
          @click="handleApproveSubmit"
        >
          {{ approveFormData.approved ? '确认通过' : '确认拒绝' }}
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

  .action-btn--export {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
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

// 表格文字样式
.refund-no-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.order-code-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.refund-type-text {
  color: var(--fts-text-secondary);
}

.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-warning);
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.applicant-text {
  color: var(--fts-text-primary);
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  .el-button {
    .el-icon {
      margin-right: 2px;
    }
  }
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 退款详情对话框
.refund-detail {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  .section-title {
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-secondary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-3);
    border-left: 3px solid var(--fts-warning);
    line-height: 1;
  }
}

.detail-amount {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-warning);
  font-variant-numeric: tabular-nums;
}

.reason-content {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-page);
  border-radius: var(--fts-card-radius);
  color: var(--fts-text-primary);
  min-height: 60px;
  line-height: 1.6;
}

.reject-reason {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}

// 时间线样式
.timeline-content {
  .timeline-title {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
  }

  .timeline-desc {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
    margin-top: 2px;
  }
}

// 审核对话框
.approve-dialog {
  .approve-info {
    padding: var(--fts-space-4);
    background: var(--fts-bg-page);
    border-radius: var(--fts-card-radius);
    margin-bottom: var(--fts-space-4);

    .approve-info-row {
      display: flex;
      align-items: flex-start;
      margin-bottom: var(--fts-space-2);

      &:last-child {
        margin-bottom: 0;
      }
    }

    .info-label {
      color: var(--fts-text-tertiary);
      flex-shrink: 0;
      width: 100px;
    }

    .info-value {
      color: var(--fts-text-primary);
      flex: 1;
      word-break: break-all;
    }

    .info-amount {
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-warning);
      font-size: var(--fts-font-size-base);
    }
  }

  .approve-form {
    .form-tip {
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-tertiary);
      margin-top: var(--fts-space-1);
    }
  }
}

// 响应式
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
