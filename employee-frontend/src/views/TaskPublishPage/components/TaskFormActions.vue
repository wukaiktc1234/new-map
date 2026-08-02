<script setup lang="ts">
/**
 * TaskFormActions - 任务发布表单操作按钮
 * 包含：取消按钮、发布按钮
 */
defineProps<{
  submitting: boolean
  category: string
}>()

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <div class="form-actions">
    <button type="button" class="btn btn--secondary" @click="$emit('cancel')">
      取消
    </button>
    <button type="submit" class="btn btn--primary" :disabled="submitting || !category" @click="$emit('submit')">
      {{ submitting ? '发布中...' : '发布任务' }}
    </button>
  </div>
</template>

<style scoped lang="scss">
// ====== 按钮 ======
.form-actions {
  display: flex;
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);

  @media (max-width: 480px) {
    flex-direction: column-reverse;
  }
}

.btn {
  flex: 1;
  padding: var(--fts-space-3) 0;
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;
  text-align: center;

  &:disabled { opacity: 0.5; cursor: not-allowed; }

  &--primary {
    color: #fff;
    background: var(--fts-primary);
    border: none;

    &:hover:not(:disabled) { opacity: 0.9; }
  }

  &--secondary {
    color: var(--fts-text-secondary);
    background: transparent;
    border: 1px solid var(--fts-border-secondary);

    &:hover:not(:disabled) { background: var(--fts-bg-tertiary); }
  }
}
</style>
