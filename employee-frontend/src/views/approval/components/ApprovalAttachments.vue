<script setup lang="ts">
/**
 * ApprovalAttachments - 审批附件列表
 * 展示审批单关联的附件文件
 */
import type { Attachment } from '@/types/approval'

defineProps<{
  attachments: Attachment[]
}>()
</script>

<template>
  <section v-if="attachments && attachments.length > 0" class="detail-section">
    <h3 class="ds-title">附件</h3>
    <div class="attachments-list">
      <div v-for="(att, idx) in attachments" :key="idx" class="attachment-item">
        <span class="att-name">{{ att.name }}</span>
        <span class="att-size">{{ att.size }}</span>
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

// ========== 附件区域 ==========
.attachments-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.att-name {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  font-weight: 500;
}

.att-size {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
