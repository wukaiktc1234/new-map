<script setup lang="ts">
/**
 * BatchActionBar - 批量操作栏
 *
 * 固定在底部的操作栏，显示已选数量和批量操作按钮。
 * 仅在有选中项时可见。
 */
interface Props {
  /** 已选中的数量 */
  selectedCount: number
  /** 是否可见 */
  visible: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedCount: 0,
  visible: false,
})

const emit = defineEmits<{
  (e: 'approve-all'): void
  (e: 'reject-all'): void
}>()

function handleApproveAll() {
  emit('approve-all')
}

function handleRejectAll() {
  emit('reject-all')
}
</script>

<template>
  <Transition name="slide-up">
    <div v-if="visible && selectedCount > 0" class="batch-action-bar">
      <div class="bar-content">
        <span class="selected-text">已选 {{ selectedCount }} 项</span>
        <div class="bar-actions">
          <button class="action-btn action-btn--success" @click="handleApproveAll">
            全部通过
          </button>
          <button class="action-btn action-btn--danger" @click="handleRejectAll">
            全部驳回
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.batch-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: var(--fts-z-index-sticky, 100);
  padding: var(--fts-space-3) var(--fts-space-4);
  padding-bottom: calc(var(--fts-space-3) + env(safe-area-inset-bottom, 0px));
  background-color: var(--fts-bg-card);
  border-top: 1px solid var(--fts-border-secondary);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 640px;
  margin: 0 auto;
}

.selected-text {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
}

.bar-actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.action-btn {
  padding: var(--fts-space-2) var(--fts-space-4);
  border: none;
  border-radius: var(--fts-radius-md, 8px);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-semibold, 600);
  cursor: pointer;
  transition: all var(--fts-duration-fast, 150ms) ease;

  &--success {
    background-color: var(--fts-success-lighter, #ecfdf5);
    color: var(--fts-success, #10b981);

    &:hover {
      background-color: var(--fts-success-light, #d1fae5);
    }

    &:active {
      transform: scale(0.97);
    }
  }

  &--danger {
    background-color: var(--fts-error-lighter, #fef2f2);
    color: var(--fts-error, #ef4444);

    &:hover {
      background-color: var(--fts-error-light, #fee2e2);
    }

    &:active {
      transform: scale(0.97);
    }
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: 2px;
  }
}

/* 入场/退场动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
              opacity 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .slide-up-enter-active,
  .slide-up-leave-active {
    transition: none;
  }

  .action-btn {
    transition: none;
  }
}
</style>
