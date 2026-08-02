<script setup lang="ts">
/**
 * ApprovalDetailTimeline - 审批流程时间线
 * 展示审批流程各节点状态（已完成/进行中/待处理/已跳过）
 */
import type { FlowNode } from '@/types/approval'

defineProps<{
  nodes: FlowNode[]
}>()
</script>

<template>
  <section class="detail-section">
    <h3 class="ds-title">审批流程</h3>
    <div class="flow-timeline">
      <div
        v-for="(node, idx) in nodes"
        :key="node.id"
        :class="['fn', `fn--${node.status}`]"
      >
        <div class="fn-line" v-if="idx > 0"></div>
        <div :class="['fn-dot', `fn-dot--${node.status}`]">
          <svg v-if="node.status === 'completed'" width="10" height="10" viewBox="0 0 10 10"><path d="M2 5.5L4 7.5L8 3" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <svg v-else-if="node.status === 'current'" width="6" height="6" viewBox="0 0 6 6"><circle cx="3" cy="3" r="2.5" fill="currentColor"/></svg>
        </div>
        <div class="fn-content">
          <div class="fn-top">
            <span class="fn-role">{{ node.role }}</span>
            <span v-if="node.userName" class="fn-user">{{ node.userName }}</span>
          </div>
          <div v-if="node.action" class="fn-action-row">
            <span :class="['fn-action', `fn-action--${node.action === '同意' || node.action === '自动通过' || node.action === '已备案' || node.action === '审核通过' || node.action === '批准' || node.action === '已归档' ? 'approve' : node.action === '驳回' ? 'reject' : 'neutral'}`]">
              {{ node.action }}
            </span>
            <span v-if="node.time" class="fn-time">{{ node.time }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
// ========== 通用详情区 ==========
.detail-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.ds-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-secondary);
}

// ========== 审批流程时间线 ==========
.flow-timeline {
  display: flex;
  flex-direction: column;
  padding-left: var(--fts-space-2);
}

.fn {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  position: relative;
  padding: var(--fts-space-3) 0;
  padding-left: var(--fts-space-5);
}

.fn-line {
  position: absolute;
  left: 7px;
  top: -4px;
  width: 2px;
  height: calc(100% + 8px);
  background: var(--fts-border-primary);

  .fn--completed & { background: var(--fts-success); }
  .fn--current & { background: linear-gradient(to bottom, var(--fts-primary), var(--fts-border-primary)); }
}

.fn-dot {
  position: absolute;
  left: 0;
  top: 10px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 1;

  &--completed {
    background: var(--fts-success);
    color: var(--fts-text-on-primary);
  }

  &--current {
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);
    box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.20);
    animation: pulse-dot 2s infinite;
  }

  &--pending {
    background: var(--fts-bg-card);
    border: 2px solid var(--fts-border-primary);
  }

  &--skipped {
    background: var(--fts-bg-tertiary);
    border: 2px dashed var(--fts-text-quaternary);
  }
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.20); }
  50% { box-shadow: 0 0 0 8px rgba(var(--fts-primary-rgb), 0.08); }
}

.fn-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.fn-top {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.fn-role {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.fn-user {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.fn-action-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.fn-action {
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  padding: 1px 6px;
  border-radius: var(--fts-radius-xs);

  &--approve { color: var(--fts-success); background: rgba(var(--fts-success-rgb), 0.08); }
  &--reject { color: var(--fts-error); background: rgba(var(--fts-error-rgb), 0.08); }
  &--neutral { color: var(--fts-text-quaternary); background: var(--fts-bg-tertiary); }
}

.fn-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
