<script setup lang="ts">
/**
 * 审批记录 · 紧凑表格（替代横向节点，记录多时纵向滚动不挤压）
 *
 * 数据源与 ApprovalTimeline 一致（approval_workflow 审计日志）。
 */
import { ref, onMounted, watch } from 'vue'
import { Clock } from '@element-plus/icons-vue'
import { approvalWorkflowApi } from '@/api/approval'
import { approvalWorkflowConverter } from '@/api/approval/converters'
import type { ApprovalAction, ApprovalAuditLogDisplay } from '@/types/approval-workflow'
import StatusTag from '@/components/core/StatusTag.vue'

interface Props {
  businessId: string
  businessType?: string
}

const props = withDefaults(defineProps<Props>(), {
  businessType: '',
})

const loading = ref(false)
const logs = ref<ApprovalAuditLogDisplay[]>([])

async function fetchLogs(): Promise<void> {
  if (!props.businessId) return
  loading.value = true
  try {
    const data = await approvalWorkflowApi.getApprovalLogs(props.businessId)
    logs.value = approvalWorkflowConverter.convertAuditLogsToDisplay(data || [])
  } catch {
    logs.value = []
  } finally {
    loading.value = false
  }
}

const actionLabelMap: Record<string, string> = {
  submit: '提交审批',
  approve: '审批通过',
  reject: '驳回',
  withdraw: '撤回',
  delegate: '转办',
  timeout: '超时',
}

function getActionLabel(log: ApprovalAuditLogDisplay): string {
  return log.actionLabel || actionLabelMap[log.action] || log.action
}

function getActionStatus(log: ApprovalAuditLogDisplay): string {
  if (log.action === 'approve') return 'success'
  if (log.action === 'reject') return 'error'
  return 'info'
}

function formatTime(t?: string): string {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  fetchLogs()
})

watch(() => props.businessId, () => {
  fetchLogs()
})
</script>

<template>
  <div v-loading="loading" class="approval-table">
    <!-- 空状态 -->
    <div v-if="!loading && logs.length === 0" class="approval-table__empty">
      <el-icon :size="28" class="approval-table__empty-icon"><Clock /></el-icon>
      <div class="approval-table__empty-text">暂无审批记录</div>
      <div class="approval-table__empty-hint">提交审批后，审批流转（提交/通过/驳回）将在此展示</div>
    </div>

    <!-- 记录表格：与订单详情"采购明细"表格同规格（default 行高/字号） -->
    <el-table v-else :data="logs" border class="approval-table__list">
      <el-table-column type="index" label="#" width="55" align="center" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <StatusTag :status="getActionStatus(row)" :label="getActionLabel(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作人" min-width="130">
        <template #default="{ row }">{{ row.operatorName || '-' }}</template>
      </el-table-column>
      <el-table-column label="审批节点" min-width="130">
        <template #default="{ row }">{{ row.nodeName || '-' }}</template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.operateTime) }}</template>
      </el-table-column>
      <el-table-column label="意见" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.comment || '-' }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.approval-table {
  min-height: 60px;

  /* 与订单详情"采购明细"表格一致的行距与字号 */
  &__list :deep(.el-table__cell) {
    padding: 10px 8px;
    font-size: 14px;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--fts-space-2);
    padding: var(--fts-space-6) 0;
    border: 1px dashed var(--fts-border-primary);
    border-radius: var(--fts-radius-base);
    background: var(--fts-bg-secondary);

    &-icon {
      color: var(--fts-text-tertiary);
    }

    &-text {
      font-size: var(--fts-font-size-base);
      color: var(--fts-text-secondary);
    }

    &-hint {
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-tertiary);
    }
  }
}
</style>
