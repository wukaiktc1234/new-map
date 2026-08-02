<script setup lang="ts">
/**
 * 召回管理页面
 *
 * 功能：
 * - 召回统计卡片（今日召回数、待处理召回、高风险项）
 * - 召回分析：按批次号/供应商/产品名分析影响范围（recallApi.analyzeRecall）
 * - 影响范围展示：受影响追溯码、订单、消费者数量
 * - 批量召回执行：选择追溯码 + 输入召回原因 + 执行（recallApi.batchRecall）
 *
 * 对接策略：真实后端优先 + Mock 降级
 */
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, WarningFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { usePermissionStore } from '@/stores/permission'
import { recallApi } from '@/api/traceability'
import type {
  RecallAnalyzeRequest,
  RecallQueryResultVO,
  RecallStatisticsVO,
  BatchRecallRequest,
  AffectedTraceCode,
} from '@/types/traceability'
import type { StatColorType } from '@/types/stat'

const permissionStore = usePermissionStore()

/** 获取当前操作人ID（数字类型） */
function getCurrentOperatorId(): number {
  const id = permissionStore.userInfo?.userId
  if (typeof id === 'number') return id
  if (typeof id === 'string') {
    const parsed = parseInt(id, 10)
    return isNaN(parsed) ? 0 : parsed
  }
  return 0
}

/** 获取当前操作人姓名 */
function getCurrentOperatorName(): string {
  return permissionStore.userInfo?.fullName || permissionStore.userInfo?.username || '未知用户'
}

/* ===== 召回统计 ===== */
const globalStats = ref<RecallStatisticsVO | null>(null)
const statsLoading = ref(false)

async function loadStatistics(): Promise<void> {
  statsLoading.value = true
  try {
    globalStats.value = await recallApi.getStatistics()
  } catch {
    // 静默失败，统计卡片显示 0
  } finally {
    statsLoading.value = false
  }
}

const statsCards = computed(() => {
  const s = globalStats.value
  return [
    {
      key: 'todayRecalled',
      icon: 'RefreshLeft',
      label: '今日召回数',
      value: s?.totalRecalledToday ?? 0,
      colorType: 'primary' as StatColorType,
    },
    {
      key: 'pending',
      icon: 'Clock',
      label: '待处理召回',
      value: s?.pendingRecall ?? 0,
      colorType: 'warning' as StatColorType,
    },
    {
      key: 'highRisk',
      icon: 'WarningFilled',
      label: '高风险项',
      value: s?.highRiskItems ?? 0,
      colorType: 'error' as StatColorType,
    },
    {
      key: 'analyzed',
      icon: 'DataAnalysis',
      label: '已分析批次',
      value: analyzeResult.value ? 1 : 0,
      colorType: 'info' as StatColorType,
    },
  ]
})

/* ===== 召回分析表单 ===== */
const analyzeForm = reactive<RecallAnalyzeRequest>({
  batchNo: '',
  supplierId: undefined,
  targetName: '',
})

const analyzing = ref(false)

/** 分析结果 */
const analyzeResult = ref<RecallQueryResultVO | null>(null)

/** 是否已执行过分析 */
const hasAnalyzed = ref(false)

/** 执行召回分析 */
async function handleAnalyze(): Promise<void> {
  if (!analyzeForm.batchNo && !analyzeForm.supplierId && !analyzeForm.targetName) {
    ElMessage.warning('请至少填写一个分析条件（批次号/供应商ID/产品名）')
    return
  }
  analyzing.value = true
  hasAnalyzed.value = true
  try {
    const params: RecallAnalyzeRequest = {}
    if (analyzeForm.batchNo) params.batchNo = analyzeForm.batchNo
    if (analyzeForm.supplierId) params.supplierId = analyzeForm.supplierId
    if (analyzeForm.targetName) params.targetName = analyzeForm.targetName
    const result = await recallApi.analyzeRecall(params)
    // 将 targetInfo 的 batchNo/supplierName 填充到每个 affectedTraceCode 行（后端 AffectedTraceCodeVO 不含这两个字段）
    if (result?.affectedTraceCodes?.length && result.targetInfo) {
      const batchNo = result.targetInfo.batchNo || ''
      const supplierName = result.targetInfo.supplierName || ''
      result.affectedTraceCodes = result.affectedTraceCodes.map((item) => ({
        ...item,
        batchNumber: item.batchNumber || batchNo,
        supplierName: item.supplierName || supplierName,
      }))
    }
    analyzeResult.value = result
    selectedTraceCodeIds.value = []
    if ((analyzeResult.value?.affectedTraceCodes?.length || 0) > 0) {
      ElMessage.success(`分析完成，共发现 ${analyzeResult.value?.affectedTraceCodes?.length || 0} 个受影响追溯码`)
    } else {
      ElMessage.info('分析完成，未发现受影响追溯码')
    }
  } catch (error) {
    ElMessage.error('召回分析失败')
    analyzeResult.value = null
  } finally {
    analyzing.value = false
  }
}

/** 重置分析表单 */
function handleResetAnalyze(): void {
  analyzeForm.batchNo = ''
  analyzeForm.supplierId = undefined
  analyzeForm.targetName = ''
  analyzeResult.value = null
  hasAnalyzed.value = false
  selectedTraceCodeIds.value = []
}

/* ===== 批量召回执行 ===== */
const selectedTraceCodeIds = ref<number[]>([])
const recallDialogVisible = ref(false)
const recallForm = reactive({
  reason: '',
})

/** 表格选择变更 */
function handleSelectionChange(rows: AffectedTraceCode[]): void {
  selectedTraceCodeIds.value = rows
    .map((r) => r.traceCodeId)
    .filter((id): id is number => id !== undefined && id !== null)
}

/** 打开召回确认弹窗 */
function openRecallDialog(): void {
  if (selectedTraceCodeIds.value.length === 0) {
    ElMessage.warning('请先选择需要召回的追溯码')
    return
  }
  recallForm.reason = ''
  recallDialogVisible.value = true
}

/** 执行批量召回 */
const recalling = ref(false)

async function handleExecuteRecall(): Promise<void> {
  if (!recallForm.reason.trim()) {
    ElMessage.warning('请填写召回原因')
    return
  }
  recalling.value = true
  try {
    const params: BatchRecallRequest = {
      traceCodeIds: selectedTraceCodeIds.value,
      reason: recallForm.reason,
      operatorId: getCurrentOperatorId(),
      operatorName: getCurrentOperatorName(),
    }
    const result = await recallApi.batchRecall(params)
    ElMessage.success(`召回执行完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
    recallDialogVisible.value = false
    // 重新分析以更新状态
    await handleAnalyze()
    await loadStatistics()
  } catch (error) {
    ElMessage.error('召回执行失败')
  } finally {
    recalling.value = false
  }
}

/* ===== 表格列定义 ===== */
const columns: DataTableColumn[] = [
  { prop: 'traceCode', label: '追溯码', minWidth: 180, showOverflowTooltip: true },
  { prop: 'targetName', label: '产品名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'batchNumber', label: '批次号', minWidth: 140, showOverflowTooltip: true },
  { prop: 'supplierName', label: '供应商', minWidth: 160, showOverflowTooltip: true },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status', align: 'center' },
  { prop: 'riskLevel', label: '风险等级', minWidth: 90, slot: 'riskLevel', align: 'center' },
]

/* ===== 工具方法 ===== */

/** 获取追溯码状态 StatusTag status
 * 后端返回 status: 1正常 2即将过期 3已过期 4已召回 5已消费
 */
function getStatusTagStatus(status?: number): string {
  const map: Record<number, string> = {
    1: 'success',    // 正常
    2: 'warning',    // 即将过期
    3: 'info',       // 已过期
    4: 'error',      // 已召回
    5: 'success',    // 已消费
  }
  return map[status ?? 0] || 'default'
}

/** 获取追溯码状态标签文本
 * 优先使用后端返回的 statusName，fallback 到映射
 */
function getStatusLabel(status?: number, statusName?: string): string {
  if (statusName) return statusName
  const map: Record<number, string> = {
    1: '正常',
    2: '即将过期',
    3: '已过期',
    4: '已召回',
    5: '已消费',
  }
  return map[status ?? 0] || '-'
}

/** 获取风险等级 StatusTag status */
function getRiskLevelTagStatus(riskLevel?: number): string {
  if (!riskLevel) return 'default'
  return riskLevel === 1 ? 'success' : riskLevel === 2 ? 'warning' : 'error'
}

/** 获取风险等级标签文本 */
function getRiskLevelLabel(riskLevel?: number): string {
  if (!riskLevel) return '-'
  return riskLevel === 1 ? '低' : riskLevel === 2 ? '中' : '高'
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="召回管理" description="问题批次反向查询、影响范围分析、批量召回执行" />

    <!-- 统计卡片 -->
    <div class="stats-section">
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

    <!-- 召回分析表单 -->
    <div class="analyze-section">
      <div class="section-header">
        <span class="section-title">召回影响分析</span>
        <span class="section-desc">输入问题批次信息，分析受影响的追溯码、订单和消费者范围</span>
      </div>
      <div class="analyze-form">
        <el-input
          v-model="analyzeForm.batchNo"
          placeholder="批次号"
          clearable
          style="width: 200px"
        />
        <el-input
          v-model.number="analyzeForm.supplierId"
          placeholder="供应商ID"
          clearable
          style="width: 140px"
          type="number"
        />
        <el-input
          v-model="analyzeForm.targetName"
          placeholder="产品名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleAnalyze"
        />
        <el-button type="primary" :loading="analyzing" @click="handleAnalyze">
          <el-icon><Search /></el-icon>
          分析影响范围
        </el-button>
        <el-button @click="handleResetAnalyze">重置</el-button>
      </div>
    </div>

    <!-- 分析结果 -->
    <div v-if="analyzeResult" class="result-section">
      <!-- 影响范围概览（使用后端 summary 字段） -->
      <div class="impact-overview">
        <div class="impact-card impact-card--primary">
          <div class="impact-card__value">{{ analyzeResult.summary?.totalTraceCodes ?? analyzeResult.affectedTraceCodes?.length ?? 0 }}</div>
          <div class="impact-card__label">受影响追溯码</div>
        </div>
        <div class="impact-card impact-card--warning">
          <div class="impact-card__value">{{ analyzeResult.summary?.totalOrders ?? analyzeResult.affectedOrders?.length ?? 0 }}</div>
          <div class="impact-card__label">受影响订单</div>
        </div>
        <div class="impact-card impact-card--error">
          <div class="impact-card__value">{{ analyzeResult.summary?.soldQuantity ?? 0 }}</div>
          <div class="impact-card__label">已售出数量</div>
        </div>
        <div class="impact-card impact-card--info">
          <div class="impact-card__value">{{ analyzeResult.summary?.stockQuantity ?? 0 }}</div>
          <div class="impact-card__label">库存中数量</div>
        </div>
      </div>

      <!-- 受影响追溯码列表 -->
      <div class="affected-list">
        <div class="list-header">
          <span class="list-title">受影响追溯码列表</span>
          <div class="list-actions">
            <el-button
              type="danger"
              :disabled="selectedTraceCodeIds.length === 0"
              @click="openRecallDialog"
            >
              <el-icon><WarningFilled /></el-icon>
              批量召回（{{ selectedTraceCodeIds.length }}）
            </el-button>
          </div>
        </div>
        <DataTable
          :columns="columns"
          :data="analyzeResult.affectedTraceCodes || []"
          :loading="analyzing"
          stripe
          @selection-change="handleSelectionChange"
        >
          <template #status="{ row }">
            <StatusTag
              :status="getStatusTagStatus(row.status)"
              :label="getStatusLabel(row.status, row.statusName)"
              size="small"
            />
          </template>
          <template #riskLevel="{ row }">
            <StatusTag
              :status="getRiskLevelTagStatus(row.riskLevel)"
              :label="getRiskLevelLabel(row.riskLevel)"
              size="small"
            />
          </template>
        </DataTable>
      </div>
    </div>

    <!-- 空状态：未分析或分析无结果 -->
    <div v-else-if="hasAnalyzed && !analyzing" class="empty-wrapper">
      <EmptyState
        type="no-data"
        title="未查询到受影响范围"
        description="请确认分析条件是否正确，或调整条件后重新分析"
      />
    </div>

    <!-- 初始状态提示 -->
    <div v-else-if="!analyzing" class="empty-wrapper">
      <EmptyState
        type="no-data"
        title="请输入分析条件"
        description="输入问题批次号、供应商ID或产品名称，分析召回影响范围"
      />
    </div>

    <!-- 批量召回确认弹窗 -->
    <el-dialog v-model="recallDialogVisible" title="批量召回确认" width="480px">
      <el-alert
        type="warning"
        :closable="false"
        title="召回操作将影响消费者权益，请确认信息无误后执行"
        style="margin-bottom: 16px"
      />
      <el-form label-width="100px">
        <el-form-item label="召回数量">
          <span>{{ selectedTraceCodeIds.length }} 个追溯码</span>
        </el-form-item>
        <el-form-item label="召回原因" required>
          <el-input
            v-model="recallForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请填写召回原因（如：原料检测不合格、供应商批次质量问题等）"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <span>{{ getCurrentOperatorName() }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recallDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="recalling" @click="handleExecuteRecall">
          确认召回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.analyze-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4) var(--fts-space-6);
  margin-bottom: var(--fts-space-4);

  .section-header {
    display: flex;
    align-items: baseline;
    gap: var(--fts-space-3);
    margin-bottom: var(--fts-space-4);
  }

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  .section-desc {
    font-size: 13px;
    color: var(--fts-text-secondary);
  }

  .analyze-form {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-wrap: wrap;
  }
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.impact-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
}

.impact-card {
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-left: 4px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  text-align: center;

  &--primary {
    border-left-color: var(--fts-primary);
  }

  &--success {
    border-left-color: var(--fts-success);
  }

  &--warning {
    border-left-color: var(--fts-warning);
  }

  &--error {
    border-left-color: var(--fts-error);
  }

  &--info {
    border-left-color: var(--fts-info);
  }

  &__value {
    font-size: 28px;
    font-weight: 700;
    color: var(--fts-text-primary);
    line-height: 1.2;
  }

  &__label {
    font-size: 13px;
    color: var(--fts-text-secondary);
    margin-top: var(--fts-space-1);
  }
}

.affected-list {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);

  .list-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: var(--fts-space-3);
    margin-bottom: var(--fts-space-3);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  .list-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  .list-actions {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }
}

.empty-wrapper {
  padding: var(--fts-space-8) 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
}
</style>
