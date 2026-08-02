<script setup lang="ts">
/**
 * 财务总账页面
 * 数据来源：/v1/vouchers
 *
 * 功能：
 * - 凭证列表展示与查询
 * - 手工凭证录入/编辑（通过 VoucherFormDialog 子组件）
 * - 凭证状态流转：草稿 → 已审核 → 已过账；草稿/已审核 → 已作废
 * - 凭证详情查看（含分录明细）
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import VoucherFormDialog from './components/VoucherFormDialog.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { voucherApi, fenToYuanNumber, VoucherStatusMap } from '@/api/finance'
import type { FinanceVoucherNew, VoucherStatus, VoucherQueryForm } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { pagination } = useStandardPage()
const searchForm = ref({ dateRange: null as [string, string] | null })
const loading = ref(false)
const records = ref<FinanceVoucherNew[]>([])

/** 凭证展示数据（金额已转为格式化字符串） */
interface VoucherDisplay {
  id: string
  voucherNo: string
  voucherDate: string
  summary: string
  debitTotal: string
  creditTotal: string
  subjectName: string
  creatorName: string
  status: VoucherStatus
}

const displayRecords = ref<VoucherDisplay[]>([])

/** 格式化金额：分 → 元（带千分位，保留2位小数） */
function formatAmount(fen: number | undefined | null): string {
  return fenToYuanNumber(fen).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 将后端状态（数字或字符串）映射为前端语义字符串 */
function mapVoucherStatus(status: number | string): VoucherStatus {
  if (typeof status === 'number') {
    return (VoucherStatusMap.toFrontend[status] ?? 'draft') as VoucherStatus
  }
  return status as VoucherStatus
}

/** 获取凭证状态标签配置 */
function getStatusTag(status: VoucherStatus): { type: string; label: string } {
  const map: Record<VoucherStatus, { type: string; label: string }> = {
    draft: { type: 'warning', label: '草稿' },
    posted: { type: 'success', label: '已过账' },
    audited: { type: 'info', label: '已审核' },
    cancelled: { type: 'info', label: '已作废' },
  }
  return map[status] ?? { type: 'info', label: '未知' }
}

/** 加载凭证列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: VoucherQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = searchForm.value.dateRange[0]
      params.endDate = searchForm.value.dateRange[1]
    }
    const response = await voucherApi.getList(params)
    records.value = response?.records || []
    pagination.total = response?.total || 0
    // 转换为展示数据：金额格式化、状态映射
    displayRecords.value = records.value.map((v): VoucherDisplay => ({
      id: v.id,
      voucherNo: v.voucherNo,
      voucherDate: v.voucherDate,
      summary: v.summary,
      debitTotal: formatAmount(v.debitTotal),
      creditTotal: formatAmount(v.creditTotal),
      subjectName: v.entries?.[0]?.subjectName || '-',
      creatorName: v.creatorName,
      status: mapVoucherStatus(v.status),
    }))
  } catch (error) {
    console.error('加载凭证列表失败:', error)
    records.value = []
    displayRecords.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计卡片数据（基于已加载列表数据计算） */
const statsCards = computed(() => {
  const posted = records.value.filter(r => mapVoucherStatus(r.status) === 'posted').length
  const draft = records.value.filter(r => mapVoucherStatus(r.status) === 'draft').length
  const audited = records.value.filter(r => mapVoucherStatus(r.status) === 'audited').length
  return [
    { icon: 'TrendCharts', label: '本月凭证数', value: pagination.total, colorType: '' as StatColorType },
    { icon: 'Document', label: '已过账', value: posted, colorType: '' as StatColorType },
    { icon: 'EditPen', label: '草稿', value: draft, colorType: '' as StatColorType },
    { icon: 'CircleClose', label: '待审核', value: audited, colorType: '' as StatColorType },
  ]
})

const columns: DataTableColumn[] = [
  { prop: 'voucherNo', label: '凭证编号', minWidth: 145 },
  { prop: 'voucherDate', label: '日期', minWidth: 110 },
  { prop: 'summary', label: '摘要', minWidth: 200, showOverflowTooltip: true },
  { prop: 'debitTotal', label: '借方金额(元)', minWidth: 130, align: 'right' },
  { prop: 'creditTotal', label: '贷方金额(元)', minWidth: 130, align: 'right' },
  { prop: 'subjectName', label: '会计科目', minWidth: 140 },
  { prop: 'creatorName', label: '制单人', minWidth: 90 },
  { prop: 'status', label: '状态', minWidth: 95, slot: 'status' },
]

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

function handleReset() {
  searchForm.value = { dateRange: null }
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.current = 1
  loadData()
}

function handleCurrentChange(current: number) {
  pagination.current = current
  loadData()
}

onMounted(() => {
  loadData()
})

// ============================================================
// 凭证录入/编辑对话框
// ============================================================

/** 录入/编辑对话框可见性 */
const formDialogVisible = ref(false)
/** 当前编辑的凭证ID（null表示新增） */
const editingVoucherId = ref<string | null>(null)

/** 打开新增凭证对话框 */
function handleCreate(): void {
  editingVoucherId.value = null
  formDialogVisible.value = true
}

/** 打开编辑凭证对话框 */
function handleEdit(row: Record<string, unknown>): void {
  editingVoucherId.value = row.id as string
  formDialogVisible.value = true
}

/** 凭证保存成功后刷新列表 */
function handleFormSuccess(): void {
  loadData()
}

// ============================================================
// 凭证状态流转
// ============================================================

/** 审核凭证（草稿 → 已审核） */
async function handleApprove(row: Record<string, unknown>): Promise<void> {
  try {
    await voucherApi.approve(row.id as string)
    ElMessage.success('凭证审核成功')
    loadData()
  } catch {
    ElMessage.error('审核失败，请重试')
  }
}

/** 过账凭证（已审核 → 已过账） */
async function handlePost(row: Record<string, unknown>): Promise<void> {
  try {
    await voucherApi.post(row.id as string)
    ElMessage.success('凭证过账成功')
    loadData()
  } catch {
    ElMessage.error('过账失败，请重试')
  }
}

/** 反审核凭证（已审核 → 草稿） */
async function handleUnapprove(row: Record<string, unknown>): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要反审核凭证 ${row.voucherNo} 吗？反审核后凭证将退回草稿状态。`,
      '反审核确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await voucherApi.unapprove(row.id as string)
    ElMessage.success('凭证已反审核')
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('反审核失败，请重试')
    }
  }
}

/** 反过账凭证（已过账 → 已审核） */
async function handleUnpost(row: Record<string, unknown>): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要反过账凭证 ${row.voucherNo} 吗？反过账后凭证将退回已审核状态。`,
      '反过账确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await voucherApi.unpost(row.id as string)
    ElMessage.success('凭证已反过账')
    loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('反过账失败，请重试')
    }
  }
}

/** 作废凭证（草稿/已审核 → 已作废） */
async function handleVoid(row: Record<string, unknown>): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要作废凭证 ${row.voucherNo} 吗？作废后不可恢复。`,
      '作废确认',
      {
        confirmButtonText: '确定作废',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await voucherApi.void(row.id as string)
    ElMessage.success('凭证已作废')
    loadData()
  } catch (error) {
    // 用户取消操作时不提示错误
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('作废失败，请重试')
    }
  }
}

// ============================================================
// 凭证详情对话框
// ============================================================

/** 详情对话框可见性 */
const detailDialogVisible = ref(false)
/** 详情数据（原始凭证数据，金额单位：分） */
const detailVoucher = ref<FinanceVoucherNew | null>(null)
/** 详情分录展示数据（金额已转为格式化字符串） */
const detailEntries = ref<Array<Record<string, string>>>([])
/** 详情借方合计（元，格式化字符串） */
const detailDebitTotal = ref('0.00')
/** 详情贷方合计（元，格式化字符串） */
const detailCreditTotal = ref('0.00')

/** 打开详情对话框（从列表行） */
function handleViewDetail(row: Record<string, unknown>): void {
  const original = records.value.find(r => r.id === (row.id as string))
  if (original) {
    showDetail(original)
  } else {
    // 列表中找不到时从后端拉取
    fetchAndShowDetail(row.id as string)
  }
}

/** 从后端获取凭证详情并展示 */
async function fetchAndShowDetail(id: string): Promise<void> {
  try {
    const detail = await voucherApi.getById(id)
    if (detail) {
      showDetail(detail)
    }
  } catch {
    ElMessage.error('加载凭证详情失败')
  }
}

/** 展示详情对话框 */
function showDetail(voucher: FinanceVoucherNew): void {
  detailVoucher.value = voucher
  detailEntries.value = (voucher.entries || []).map(e => ({
    subjectCode: e.subjectCode,
    subjectName: e.subjectName,
    summary: e.summary || '-',
    debitAmount: formatAmount(e.debitAmount),
    creditAmount: formatAmount(e.creditAmount),
  }))
  detailDebitTotal.value = formatAmount(voucher.debitTotal)
  detailCreditTotal.value = formatAmount(voucher.creditTotal)
  detailDialogVisible.value = true
}

/** 详情分录表格列定义 */
const detailEntryColumns = [
  { prop: 'subjectCode', label: '科目编码', minWidth: 100 },
  { prop: 'subjectName', label: '科目名称', minWidth: 140 },
  { prop: 'summary', label: '摘要', minWidth: 160, showOverflowTooltip: true },
  { prop: 'debitAmount', label: '借方(元)', minWidth: 120, align: 'right' },
  { prop: 'creditAmount', label: '贷方(元)', minWidth: 120, align: 'right' },
]
</script>

<template>
  <div class="modern-page">
    <PageHeader title="财务总账" description="会计凭证与总账科目管理">
      <template #extra>
        <el-button type="primary" size="default" @click="handleCreate">
          <el-icon :size="16"><Plus /></el-icon>新增凭证
        </el-button>
      </template>
    </PageHeader>
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" placement="bottom-start" style="width:240px;max-width:240px;flex-shrink:0" size="default" />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>
    <div class="table-section">
      <DataTable :columns="columns" :data="displayRecords" :loading="loading" stripe :actions-width="240">
        <template #status="{ row }"><StatusTag :status="getStatusTag(row.status).type" :label="getStatusTag(row.status).label" size="small" /></template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">查看</el-button>
          <template v-if="row.status === 'draft'">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="handleApprove(row)">审核</el-button>
            <el-button link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
          </template>
          <template v-else-if="row.status === 'audited'">
            <el-button link type="primary" size="small" @click="handlePost(row)">过账</el-button>
            <el-button link type="warning" size="small" @click="handleUnapprove(row)">反审核</el-button>
            <el-button link type="danger" size="small" @click="handleVoid(row)">作废</el-button>
          </template>
          <template v-else-if="row.status === 'posted'">
            <el-button link type="warning" size="small" @click="handleUnpost(row)">反过账</el-button>
          </template>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <!-- 凭证录入/编辑对话框 -->
    <VoucherFormDialog
      v-model="formDialogVisible"
      :voucher-id="editingVoucherId"
      @success="handleFormSuccess"
    />

    <!-- 凭证详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="凭证详情" width="780px">
      <el-descriptions v-if="detailVoucher" :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="凭证编号">{{ detailVoucher.voucherNo }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ detailVoucher.voucherDate }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ detailVoucher.summary }}</el-descriptions-item>
        <el-descriptions-item label="借方金额(元)">¥{{ detailDebitTotal }}</el-descriptions-item>
        <el-descriptions-item label="贷方金额(元)">¥{{ detailCreditTotal }}</el-descriptions-item>
        <el-descriptions-item label="制单人">{{ detailVoucher.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detailVoucher.auditorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusTag(mapVoucherStatus(detailVoucher.status)).type" :label="getStatusTag(mapVoucherStatus(detailVoucher.status)).label" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="附件张数">{{ detailVoucher.attachmentCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailVoucher.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 分录明细表格 -->
      <div class="detail-entries">
        <div class="detail-entries__title">分录明细</div>
        <el-table :data="detailEntries" border size="small" style="width: 100%">
          <el-table-column
            v-for="col in detailEntryColumns"
            :key="col.prop"
            :prop="col.prop"
            :label="col.label"
            :min-width="col.minWidth"
            :align="col.align || 'left'"
            :show-overflow-tooltip="col.showOverflowTooltip"
          />
        </el-table>
        <div class="detail-entries__total">
          <div class="total-item">
            <span class="total-label">借方合计：</span>
            <span class="total-value">¥{{ detailDebitTotal }}</span>
          </div>
          <div class="total-item">
            <span class="total-label">贷方合计：</span>
            <span class="total-value">¥{{ detailCreditTotal }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.detail-entries {
  margin-top: var(--fts-space-4);

  &__title {
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    margin-bottom: var(--fts-space-3);
  }

  &__total {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: var(--fts-space-6);
    padding: var(--fts-space-3) var(--fts-space-4);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);
    margin-top: var(--fts-space-3);

    .total-item {
      display: flex;
      align-items: center;
      gap: var(--fts-space-2);

      .total-label {
        color: var(--fts-text-secondary);
        font-size: var(--fts-font-size-sm);
      }

      .total-value {
        color: var(--fts-text-primary);
        font-weight: var(--fts-font-weight-semibold);
        font-size: var(--fts-font-size-base);
      }
    }
  }
}
</style>
