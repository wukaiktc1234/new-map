<script setup lang="ts">
/**
 * ApprovalDetailFields - 申请内容表单字段展示
 * 动态渲染表单字段的标签和值
 */
import type { FormField } from '@/types/approval'

defineProps<{
  fields: FormField[]
}>()
</script>

<template>
  <section class="detail-section">
    <h3 class="ds-title">申请内容</h3>
    <div class="ds-fields">
      <div v-for="(field, idx) in fields" :key="idx" class="df-row">
        <span class="df-label">{{ field.label }}</span>
        <span :class="['df-value', { 'df-value--long': field.type === 'textarea' }]">
          {{ field.value }}
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
// ========== 申请内容 / 通用详情区 ==========
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

.ds-fields {
  display: flex;
  flex-direction: column;
}

.df-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) 0;

  & + & {
    border-top: 1px solid var(--fts-border-secondary);
  }
}

.df-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  font-weight: 500;
  white-space: nowrap;
  min-width: 80px;
}

.df-value {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: 500;
  text-align: right;
  flex: 1;
  min-width: 0;
  word-break: break-all;

  &--long {
    text-align: left;
    white-space: pre-wrap;
    line-height: 1.6;
  }
}
</style>
