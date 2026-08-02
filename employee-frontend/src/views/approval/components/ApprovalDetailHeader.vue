<script setup lang="ts">
/**
 * ApprovalDetailHeader - 审批详情状态头部
 * 展示审批类型、紧急程度、状态标签、申请人信息和单号
 */
import { StatusTag } from '@/components/core'
import type { ApprovalDetail, ApprovalType } from '@/types/approval'
import { ApprovalTypeLabels } from '@/types/approval'

defineProps<{
  detail: ApprovalDetail
}>()

const typeColorMap: Record<ApprovalType, string> = {
  leave: 'var(--fts-warning)',
  overtime: 'var(--fts-primary)',
  swap: 'var(--fts-info)',
  travel: 'var(--fts-success)',
  reimbursement: 'var(--fts-error)',
  requisition: 'var(--fts-text-quaternary)',
}
</script>

<template>
  <section class="status-hero" :style="{ borderLeftColor: typeColorMap[detail.type] }">
    <div class="sh-top">
      <div class="sh-left">
        <span class="sh-type-badge" :style="{ background: typeColorMap[detail.type], color: 'var(--fts-text-on-primary)' }">
          {{ ApprovalTypeLabels[detail.type] }}
        </span>
        <StatusTag
          v-if="detail.urgency !== 'normal'"
          :status="detail.urgency"
          :urgency="detail.urgency"
          variant="solid"
          size="small"
          dot
        />
      </div>
      <StatusTag
        :status="detail.status"
        variant="light"
        size="medium"
        dot
      />
    </div>
    <div class="sh-info">
      <span class="sh-applicant">{{ detail.applicantName }}</span>
      <span class="sh-time">{{ detail.createdAt.slice(5, 16).replace('T', ' ') }}</span>
    </div>
    <div class="sh-no">单号：{{ detail.approvalNo }}</div>
  </section>
</template>

<style scoped lang="scss">
// ========== 状态头部 ==========
.status-hero {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-left: 4px solid var(--fts-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.sh-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
}

.sh-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.sh-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
}

.sh-info {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin-bottom: var(--fts-space-2);
}

.sh-applicant {
  font-weight: 600;
  color: var(--fts-text-primary);
}

.sh-time {
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-xs);
}

.sh-no {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
