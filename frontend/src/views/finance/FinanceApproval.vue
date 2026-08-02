<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { approvalFlowApi } from '@/api/finance'
import type { ApprovalFlowConfig, ApprovalDocumentType } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { pagination, resetPagination } = useStandardPage()
const searchForm = ref({
  documentType: '' as ApprovalDocumentType | '',
  enabled: '' as '' | 'true' | 'false',
  keyword: '',
})
const loading = ref(false)

/** 审批流配置列表 */
const records = ref<ApprovalFlowConfig[]>([])

/** 单据类型下拉选项 */
const documentTypeOptions: Array<{ label: string; value: ApprovalDocumentType }> = [
  { label: '凭证', value: 'voucher' },
  { label: '费用', value: 'expense' },
  { label: '发票', value: 'invoice' },
  { label: '报表', value: 'report' },
  { label: '预算', value: 'budget' },
  { label: '付款', value: 'payment' },
]

/** 单据类型显示名称映射 */
const documentTypeLabelMap: Record<ApprovalDocumentType, string> = {
  voucher: '凭证',
  expense: '费用',
  invoice: '发票',
  report: '报表',
  budget: '预算',
  payment: '付款',
}

/** 统计卡片数据 */
const statsCards = ref<Array<{ icon: string; label: string; value: string; colorType: StatColorType }>>([
  { icon: 'Document', label: '配置总数', value: '0', colorType: 'primary' },
  { icon: 'Check', label: '已启用', value: '0', colorType: 'success' },
  { icon: 'Close', label: '已禁用', value: '0', colorType: 'error' },
  { icon: 'Connection', label: '审批节点总数', value: '0', colorType: 'info' },
])

const columns = [
  { prop: 'configName', label: '配置名称', minWidth: 160 },
  { prop: 'documentType', label: '单据类型', minWidth: 110, slot: 'documentType' },
  { prop: 'nodes', label: '审批节点', minWidth: 220, slot: 'nodes', showOverflowTooltip: true },
  { prop: 'enabled', label: '状态', minWidth: 90, slot: 'enabled' },
  { prop: 'remark', label: '备注', minWidth: 180, showOverflowTooltip: true },
]

/** 构建查询参数 */
function buildQueryParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {
    page: pagination.current,
    size: pagination.size,
  }
  if (searchForm.value.documentType) {
    params.documentType = searchForm.value.documentType
  }
  if (searchForm.value.enabled) {
    params.enabled = searchForm.value.enabled === 'true'
  }
  if (searchForm.value.keyword) {
    params.configName = searchForm.value.keyword
  }
  return params
}

/** 更新统计卡片 */
function updateStatsCards(): void {
  const total = records.value.length
  const enabled = records.value.filter(r => r.enabled).length
  const disabled = total - enabled
  const totalNodes = records.value.reduce((sum, r) => sum + (r.nodes?.length || 0), 0)

  statsCards.value[0].value = String(total)
  statsCards.value[1].value = String(enabled)
  statsCards.value[2].value = String(disabled)
  statsCards.value[3].value = String(totalNodes)
}

/** 加载审批流配置列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const response = await approvalFlowApi.getList(buildQueryParams())
    records.value = response?.records || []
    pagination.total = response?.total || 0
    updateStatsCards()
  } catch {
    records.value = []
    pagination.total = 0
    updateStatsCards()
  } finally {
    loading.value = false
  }
}

/** 查询 */
function handleSearch(): void {
  resetPagination()
  loadData()
}

/** 重置 */
function handleReset(): void {
  searchForm.value = { documentType: '', enabled: '', keyword: '' }
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

/** 格式化审批节点显示 */
function formatNodes(nodes: ApprovalFlowConfig['nodes']): string {
  if (!nodes || nodes.length === 0) return '-'
  return nodes
    .sort((a, b) => a.order - b.order)
    .map(n => `${n.nodeName}(${n.approverNames?.join('、') || '-'})`)
    .join(' → ')
}

/** 获取单据类型显示名称 */
function getDocumentTypeLabel(type: ApprovalDocumentType): string {
  return documentTypeLabelMap[type] || type
}

onMounted(() => {
  loadData()
})

/** 详情对话框 */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

/** 打开详情对话框 */
function handleViewDetail(row: Record<string, unknown>): void {
  detailData.value = { ...row }
  detailDialogVisible.value = true
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="财务审批" description="财务审批流程配置管理" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="searchForm.documentType" placeholder="单据类型" clearable style="width:130px" size="default">
        <el-option v-for="opt in documentTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="searchForm.enabled" placeholder="启用状态" clearable style="width:120px" size="default">
        <el-option label="已启用" value="true" />
        <el-option label="已禁用" value="false" />
      </el-select>
      <el-input v-model="searchForm.keyword" placeholder="搜索配置名称" clearable style="width:180px" size="default" @keyup.enter="handleSearch" />
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="records" :loading="loading" stripe>
        <template #documentType="{ row }"><StatusTag status="info" :label="getDocumentTypeLabel(row.documentType)" size="small" /></template>
        <template #nodes="{ row }">{{ formatNodes(row.nodes) }}</template>
        <template #enabled="{ row }"><StatusTag :status="row.enabled ? 'active' : 'inactive'" :label="row.enabled ? '已启用' : '已禁用'" size="small" /></template>
        <template #actions="{ row }"><el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button></template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="审批流配置详情" width="640px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="配置名称">{{ detailData.configName as string }}</el-descriptions-item>
        <el-descriptions-item label="单据类型">{{ getDocumentTypeLabel(detailData.documentType as ApprovalDocumentType) }}</el-descriptions-item>
        <el-descriptions-item label="是否启用">
          <StatusTag :status="detailData.enabled ? 'active' : 'inactive'" :label="detailData.enabled ? '已启用' : '已禁用'" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ (detailData.createTime as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-nodes-title">审批节点</div>
      <el-table :data="(detailData.nodes as ApprovalFlowConfig['nodes']) || []" border size="small">
        <el-table-column prop="order" label="顺序" width="80" align="center" />
        <el-table-column prop="nodeName" label="节点名称" min-width="140" />
        <el-table-column label="审批人" min-width="180">
          <template #default="{ row }">{{ row.approverNames?.join('、') || '-' }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-nodes-title {
  margin-top: var(--fts-space-4);
  margin-bottom: var(--fts-space-2);
  font-weight: 600;
  color: var(--fts-text-primary);
}
</style>
