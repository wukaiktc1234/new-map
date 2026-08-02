<script setup lang="ts">
/**
 * 供应商追溯页面
 *
 * 功能：
 * - 调用 supplierTraceApi.getSupplierTrace() 获取供应商追溯汇总
 * - 调用 supplierTraceApi.getBatches() 获取批次列表
 * - 调用 supplierTraceApi.getQualityRate() 获取合格率
 * - 调用 supplierTraceApi.getRecalls() 获取召回记录
 * - 调用 supplierTraceApi.getInspections() 获取检验记录
 * - 顶部选择供应商ID，加载汇总信息与三个 Tab 数据
 *
 * 对接策略：直接调用真实后端 API，无 Mock 降级
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { supplierTraceApi } from '@/api/traceability'
import type {
  SupplierTrace,
  SupplierBatch,
  InspectionRecord,
  InspectionResult,
  RecallRecord,
} from '@/types/traceability'
import type { StatColorType } from '@/types/stat'
import { InspectionResultMap, RecallStatusMap } from '@/types/traceability'

/** Tab 类型 */
type TraceTab = 'batch' | 'inspection' | 'recall'

/* ===== 状态 ===== */
const loading = ref(false)
const activeTab = ref<TraceTab>('batch')

/** 当前选中的供应商ID */
const selectedSupplierId = ref<number | undefined>(undefined)

/** 供应商追溯汇总 */
const supplierTrace = ref<SupplierTrace | null>(null)

/** 批次列表 */
const batchList = ref<SupplierBatch[]>([])

/** 检验记录列表 */
const inspectionList = ref<InspectionRecord[]>([])

/** 召回记录列表 */
const recallList = ref<RecallRecord[]>([])

/* ===== 加载数据 ===== */

/** 加载供应商追溯数据 */
async function loadSupplierTrace(): Promise<void> {
  if (!selectedSupplierId.value) {
    ElMessage.warning('请输入供应商ID')
    return
  }
  loading.value = true
  try {
    const data = await supplierTraceApi.getSupplierTrace(selectedSupplierId.value)
    supplierTrace.value = data
    batchList.value = data?.batches || []
    inspectionList.value = data?.inspections || []
    recallList.value = data?.recalls || []
  } catch (error) {
    ElMessage.error('加载供应商追溯数据失败')
    supplierTrace.value = null
    batchList.value = []
    inspectionList.value = []
    recallList.value = []
  } finally {
    loading.value = false
  }
}

/** 查询按钮 */
async function handleSearch(): Promise<void> {
  await loadSupplierTrace()
}

/** Tab 切换 */
function handleTabChange(tab: TraceTab): void {
  activeTab.value = tab
}

/* ===== 统计卡片 ===== */
const statsCards = computed(() => {
  const s = supplierTrace.value
  return [
    {
      key: 'batches',
      icon: 'Box',
      label: '总批次数',
      value: s?.totalBatches ?? 0,
      colorType: 'primary' as StatColorType,
    },
    {
      key: 'inspections',
      icon: 'DocumentChecked',
      label: '总检验次数',
      value: s?.totalInspections ?? 0,
      colorType: 'success' as StatColorType,
    },
    {
      key: 'rate',
      icon: 'DataAnalysis',
      label: '合格率',
      value: `${s?.qualificationRate ?? 0}%`,
      colorType: 'warning' as StatColorType,
    },
    {
      key: 'recalls',
      icon: 'CircleCloseFilled',
      label: '召回次数',
      value: s?.totalRecalls ?? 0,
      colorType: 'error' as StatColorType,
    },
  ]
})

/* ===== 表格列定义 ===== */

/** 批次表格列 */
const batchColumns: DataTableColumn[] = [
  { prop: 'traceCode', label: '追溯码', minWidth: 180, showOverflowTooltip: true },
  { prop: 'materialName', label: '物料名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'batchNo', label: '批次号', minWidth: 140, showOverflowTooltip: true },
  { prop: 'inboundTime', label: '入库时间', minWidth: 155, slot: 'inboundTime' },
  { prop: 'expiryDate', label: '到期日期', minWidth: 110 },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status', align: 'center' },
]

/** 检验记录表格列 */
const inspectionColumns: DataTableColumn[] = [
  { prop: 'inspectionNo', label: '检验编号', minWidth: 140, showOverflowTooltip: true },
  { prop: 'batchNo', label: '批次号', minWidth: 140, showOverflowTooltip: true },
  { prop: 'materialName', label: '物料名称', minWidth: 130, showOverflowTooltip: true },
  { prop: 'inspectionResult', label: '检验结果', minWidth: 110, slot: 'inspectionResult', align: 'center' },
  { prop: 'inspectorName', label: '检验员', minWidth: 90 },
  { prop: 'inspectionTime', label: '检验时间', minWidth: 155, slot: 'inspectionTime' },
]

/** 召回记录表格列 */
const recallColumns: DataTableColumn[] = [
  { prop: 'batchNo', label: '批次号', minWidth: 140, showOverflowTooltip: true },
  { prop: 'reason', label: '召回原因', minWidth: 200, showOverflowTooltip: true },
  { prop: 'operatorName', label: '操作人', minWidth: 100 },
  { prop: 'operateTime', label: '召回时间', minWidth: 155, slot: 'operateTime' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status', align: 'center' },
]

/* ===== 状态展示工具方法 ===== */

/**
 * 将映射表的 type 字段（success/danger/warning/primary/info）转换为 StatusTag 的 status
 */
function mapTypeToStatus(type?: string): string {
  if (!type) return 'info'
  const map: Record<string, string> = {
    success: 'success',
    danger: 'error',
    warning: 'warning',
    primary: 'info',
    info: 'info',
  }
  return map[type] || 'info'
}

/** 获取检验结果 StatusTag status */
function getResultTagStatus(result?: InspectionResult): string {
  if (!result) return 'info'
  return mapTypeToStatus(InspectionResultMap[result]?.type)
}

/** 获取检验结果标签文本 */
function getResultLabel(result?: InspectionResult): string {
  if (!result) return '-'
  return InspectionResultMap[result]?.label || result
}

/** 获取批次状态 StatusTag status */
function getBatchStatusTagStatus(status?: string): string {
  if (!status) return 'info'
  const map: Record<string, string> = {
    active: 'success',
    in_stock: 'success',
    expired: 'error',
    recalled: 'warning',
    used: 'info',
    pending: 'info',
  }
  return map[status] || 'info'
}

/** 获取召回状态 StatusTag status */
function getRecallStatusTagStatus(status?: string): string {
  if (!status) return 'info'
  return mapTypeToStatus(RecallStatusMap[status]?.type)
}

/** 获取召回状态标签文本 */
function getRecallStatusLabel(status?: string): string {
  if (!status) return '-'
  return RecallStatusMap[status]?.label || status
}

/** 格式化日期时间 */
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const normalized = dateStr.replace('T', ' ')
  return normalized.split('.')[0].slice(0, 19)
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="供应商追溯" description="从供应商视角聚合追溯信息，展示供应批次、检验与召回记录" />

    <!-- 供应商选择 -->
    <div class="supplier-select-section">
      <div class="select-label">供应商ID：</div>
      <el-input-number
        v-model="selectedSupplierId"
        :min="1"
        placeholder="请输入供应商ID"
        style="width: 220px"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <div v-if="supplierTrace" class="supplier-name">
        当前供应商：{{ supplierTrace.supplierName }}
      </div>
    </div>

    <!-- 统计卡片 -->
    <div v-if="supplierTrace" class="stats-section">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </div>

    <!-- Tab 切换 -->
    <div v-if="supplierTrace" class="tab-section">
      <el-radio-group v-model="activeTab" @change="handleTabChange">
        <el-radio-button value="batch">原料批次（{{ batchList.length }}）</el-radio-button>
        <el-radio-button value="inspection">检验记录（{{ inspectionList.length }}）</el-radio-button>
        <el-radio-button value="recall">召回记录（{{ recallList.length }}）</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 原料批次 Tab -->
    <div v-if="supplierTrace && activeTab === 'batch'" class="table-section">
      <DataTable :columns="batchColumns" :data="batchList" :loading="loading" stripe>
        <template #inboundTime="{ row }">
          {{ formatDateTime(row.inboundTime) }}
        </template>
        <template #status="{ row }">
          <StatusTag
            :status="getBatchStatusTagStatus(row.status)"
            :label="row.statusName || row.status || '-'"
            size="small"
          />
        </template>
      </DataTable>
      <EmptyState
        v-if="!loading && batchList.length === 0"
        type="no-data"
        title="暂无批次记录"
        description="该供应商当前没有批次记录"
      />
    </div>

    <!-- 检验记录 Tab -->
    <div v-if="supplierTrace && activeTab === 'inspection'" class="table-section">
      <DataTable :columns="inspectionColumns" :data="inspectionList" :loading="loading" stripe>
        <template #inspectionResult="{ row }">
          <StatusTag
            :status="getResultTagStatus(row.inspectionResult)"
            :label="getResultLabel(row.inspectionResult)"
            size="small"
          />
        </template>
        <template #inspectionTime="{ row }">
          {{ formatDateTime(row.inspectionTime) }}
        </template>
      </DataTable>
      <EmptyState
        v-if="!loading && inspectionList.length === 0"
        type="no-data"
        title="暂无检验记录"
        description="该供应商当前没有检验记录"
      />
    </div>

    <!-- 召回记录 Tab -->
    <div v-if="supplierTrace && activeTab === 'recall'" class="table-section">
      <DataTable :columns="recallColumns" :data="recallList" :loading="loading" stripe>
        <template #operateTime="{ row }">
          {{ formatDateTime(row.operateTime) }}
        </template>
        <template #status="{ row }">
          <StatusTag
            :status="getRecallStatusTagStatus(row.status)"
            :label="getRecallStatusLabel(row.status)"
            size="small"
          />
        </template>
      </DataTable>
      <EmptyState
        v-if="!loading && recallList.length === 0"
        type="no-data"
        title="暂无召回记录"
        description="该供应商当前没有召回记录"
      />
    </div>

    <!-- 未选择供应商时的空状态 -->
    <EmptyState
      v-if="!supplierTrace && !loading"
      type="no-data"
      title="请选择供应商"
      description="在上方输入供应商ID并点击查询，查看供应商追溯信息"
    />
  </div>
</template>

<style scoped lang="scss">
.supplier-select-section {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  margin-bottom: var(--fts-space-4);

  .select-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  .supplier-name {
    margin-left: var(--fts-space-4);
    font-size: 14px;
    color: var(--fts-text-secondary);
  }
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.tab-section {
  display: flex;
  align-items: center;
  padding: var(--fts-space-4) 0;
}

.table-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
}
</style>
