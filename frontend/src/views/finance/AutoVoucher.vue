<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { transferTemplateApi } from '@/api/finance'
import type { FinanceTransferTemplate, FinanceTransferTemplateQueryForm } from '@/types/finance'
import type { StatColorType } from '@/types/stat'

const { pagination } = useStandardPage()
const searchForm = ref({ ruleStatus: '', keyword: '' })
const loading = ref(false)
const records = ref<FinanceTransferTemplate[]>([])

/** 自动凭证规则展示数据 */
interface AutoVoucherRuleDisplay {
  id: string
  ruleName: string
  businessType: string
  accountTpl: string
  triggerEvent: string
  lastRunTime: string
  status: 'active' | 'inactive'
}

const displayRecords = ref<AutoVoucherRuleDisplay[]>([])

/** 构建凭证模板描述（借方科目 → 贷方科目） */
function buildAccountTpl(debitSubject?: string, creditSubject?: string): string {
  if (debitSubject && creditSubject) {
    return `借: ${debitSubject} 贷: ${creditSubject}`
  }
  if (debitSubject) return `借: ${debitSubject}`
  if (creditSubject) return `贷: ${creditSubject}`
  return '-'
}

/** 加载自动凭证规则列表 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: FinanceTransferTemplateQueryForm = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.keyword) {
      params.templateName = searchForm.value.keyword
    }
    if (searchForm.value.ruleStatus === 'active') {
      params.enabled = true
    } else if (searchForm.value.ruleStatus === 'inactive') {
      params.enabled = false
    }
    const response = await transferTemplateApi.getList(params)
    records.value = response?.records || []
    pagination.total = response?.total || 0
    // 转换为展示数据
    displayRecords.value = records.value.map((t): AutoVoucherRuleDisplay => ({
      id: t.id,
      ruleName: t.templateName,
      businessType: t.templateType || '-',
      accountTpl: buildAccountTpl(t.debitSubject, t.creditSubject),
      triggerEvent: t.frequency || '-',
      lastRunTime: t.updateTime || t.createTime || '-',
      status: t.enabled ? 'active' : 'inactive',
    }))
  } catch (error) {
    console.error('加载自动凭证规则列表失败:', error)
    records.value = []
    displayRecords.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 统计卡片数据（基于已加载列表数据计算） */
const statsCards = computed(() => {
  const enabledCount = records.value.filter(r => r.enabled).length
  const totalCount = records.value.length
  const enableRate = totalCount > 0
    ? ((enabledCount / totalCount) * 100).toFixed(0) + '%'
    : '-'
  return [
    { icon: 'Setting', label: '有效规则', value: enabledCount, colorType: '' as StatColorType },
    { icon: 'TrendCharts', label: '规则总数', value: totalCount, colorType: '' as StatColorType },
    { icon: 'DataLine', label: '启用率', value: enableRate, colorType: '' as StatColorType },
    { icon: 'Document', label: '凭证模板数', value: totalCount, colorType: '' as StatColorType },
  ]
})

const columns = [
  { prop: 'ruleName', label: '规则名称', minWidth: 180, showOverflowTooltip: true },
  { prop: 'businessType', label: '业务类型', minWidth: 100, slot: 'businessType' },
  { prop: 'accountTpl', label: '凭证模板', minWidth: 200, showOverflowTooltip: true },
  { prop: 'triggerEvent', label: '触发条件', minWidth: 160, showOverflowTooltip: true },
  { prop: 'lastRunTime', label: '最近执行', minWidth: 160 },
  { prop: 'status', label: '状态', minWidth: 80, slot: 'status' },
]

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

function handleReset() {
  searchForm.value = { ruleStatus: '', keyword: '' }
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

/** 详情对话框 */
const detailDialogVisible = ref(false)
const detailData = ref<Record<string, unknown>>({})

/** 打开详情对话框 */
function handleViewDetail(row: Record<string, unknown>): void {
  // 从 records 找到对应的原始记录，补充展示数据中未包含的字段
  const original = records.value.find(r => r.id === (row.id as string))
  detailData.value = { ...row, formula: original?.formula || '-', remark: original?.remark || '-' }
  detailDialogVisible.value = true
}

/** 切换启用状态 */
async function handleToggleEnabled(row: Record<string, unknown>): Promise<void> {
  try {
    // 后端 toggleEnabled 要求传 enabled 参数（必填），此处取反当前状态
    const currentEnabled = row.enabled === true || row.status === 'active'
    await transferTemplateApi.toggleEnabled(row.id as string, !currentEnabled)
    ElMessage.success(currentEnabled ? '已停用' : '已启用')
    loadData()
  } catch {
    ElMessage.error('操作失败，请重试')
  }
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="自动凭证管理" description="基于业务规则的自动会计凭证生成" />
    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>
    <div class="advanced-search-panel"><div class="toolbar-row"><div class="toolbar-left">
      <el-select v-model="searchForm.ruleStatus" placeholder="规则状态" clearable style="width:120px" size="default">
        <el-option label="启用" value="active" /><el-option label="停用" value="inactive" />
      </el-select>
      <el-input v-model="searchForm.keyword" placeholder="搜索规则名称" clearable style="width:180px" size="default" @keyup.enter="handleSearch" />
    </div><div class="toolbar-right">
      <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
      <el-button size="default" @click="handleReset">重置</el-button>
    </div></div></div>
    <div class="table-section">
      <DataTable :columns="columns" :data="displayRecords" :loading="loading" stripe>
        <template #businessType="{ row }"><StatusTag status="info" :label="row.businessType" size="small" /></template>
        <template #status="{ row }"><StatusTag :status="row.status === 'active' ? 'active' : 'inactive'" :label="row.status === 'active' ? '启用' : '停用'" size="small" /></template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="handleToggleEnabled(row)">{{ row.status === 'active' ? '停用' : '启用' }}</el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination :current-page="pagination.current" :page-size="pagination.size" :total="pagination.total" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="自动凭证规则详情" width="600px">
      <el-descriptions :column="2" border label-class-name="detail-label">
        <el-descriptions-item label="规则名称">{{ detailData.ruleName as string }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ detailData.businessType as string }}</el-descriptions-item>
        <el-descriptions-item label="凭证模板" :span="2">{{ detailData.accountTpl as string }}</el-descriptions-item>
        <el-descriptions-item label="触发条件">{{ detailData.triggerEvent as string }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="detailData.status === 'active' ? 'active' : 'inactive'" :label="detailData.status === 'active' ? '启用' : '停用'" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="计算公式" :span="2">{{ (detailData.formula as string) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近执行">{{ detailData.lastRunTime as string }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ (detailData.remark as string) || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
