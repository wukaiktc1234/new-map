<script setup lang="ts">
/**
 * EmptyState 空状态组件
 *
 * 设计理念：
 * - 简洁的纯文字空状态，无图标
 * - 渐入动画让空状态不那么突兀
 * - 支持深色模式自动适配
 * - 操作按钮带微交互反馈
 */

withDefaults(defineProps<{
  /** 标题文字 */
  title: string
  /** 描述文字 */
  description?: string
  /** 操作按钮文字 */
  actionText?: string
}>(), {
  actionText: '去操作',
})

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <div class="empty-state">
    <h3 class="empty-title">{{ title }}</h3>
    <p v-if="description" class="empty-desc">{{ description }}</p>

    <button v-if="actionText" class="empty-action" @click="emit('action')">
      {{ actionText }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-12) var(--fts-space-6);
  text-align: center;

  /* 入场动画：淡入+轻微上浮 */
  animation: empty-fade-in var(--fts-skeleton-fade-in-duration, 400ms) var(--fts-easing-smooth) both;
}

@keyframes empty-fade-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-title {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2);
  letter-spacing: -0.01em;
}

.empty-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-quaternary);
  line-height: 1.6;
  max-width: 280px;
  margin: 0 0 var(--fts-space-5);
}

.empty-action {
  padding: var(--fts-space-3) var(--fts-space-6);
  background: var(--fts-primary);
  color: #fff;
  border: none;
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color var(--fts-duration-normal) var(--fts-easing-spring),
    transform var(--fts-duration-normal) var(--fts-easing-spring),
    box-shadow var(--fts-duration-normal) var(--fts-easing-spring);

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(var(--fts-primary-rgb), 0.3);
  }

  &:active {
    transform: scale(0.97);
  }
}
</style>
