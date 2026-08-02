<script setup lang="ts">
/**
 * ActionableTodoItem - 可操作待办项
 *
 * 展示需要用户操作的待办事项，左侧类型圆点 + 标题描述 + 右侧操作按钮。
 */
import type { ActionableTodo } from '@/types/todo'

interface Props {
  /** 待办数据 */
  item: ActionableTodo
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'handle', id: string): void
  (e: 'navigate', url: string): void
}>()

/** 根据优先级获取圆点颜色 */
function getDotColor(urgency: string): string {
  const map: Record<string, string> = {
    high: 'var(--fts-error)',
    medium: 'var(--fts-warning)',
    low: 'var(--fts-success)',
  }
  return map[urgency] || 'var(--fts-text-tertiary)'
}

/** 获取操作按钮文字 */
function getActionLabel(actionType: string): string {
  const map: Record<string, string> = {
    confirm: '确认',
    approve: '查看',
    complete: '完成',
  }
  return map[actionType] || '处理'
}

function handlePrimary() {
  emit('handle', props.item.id)
}
</script>

<template>
  <button class="actionable-item" type="button" @click="handlePrimary">
    <!-- 左侧类型圆点 -->
    <span
      class="type-dot"
      :style="{ backgroundColor: getDotColor(props.item.priority ?? '') }"
    />

    <!-- 内容区 -->
    <div class="item-content">
      <span class="item-title">{{ item.title }}</span>
      <span v-if="item.description" class="item-desc">{{ item.description }}</span>
    </div>

    <!-- 操作按钮 -->
    <span class="action-label">{{ getActionLabel(props.item.actionType ?? '') }}</span>
  </button>
</template>

<style scoped lang="scss">
.actionable-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  width: 100%;
  padding: var(--fts-space-3) var(--fts-space-4);
  border: none;
  border-radius: var(--fts-radius-md, 8px);
  background-color: var(--fts-bg-secondary);
  cursor: pointer;
  text-align: left;
  transition: all var(--fts-duration-fast, 150ms) ease;

  &:hover {
    background-color: var(--fts-bg-tertiary);

    .item-title {
      color: var(--fts-primary);
    }
  }

  &:active {
    transform: scale(0.98);
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: -2px;
  }
}

.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-title {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--fts-duration-fast, 150ms) ease;
}

.item-desc {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-label {
  flex-shrink: 0;
  padding: 2px 12px;
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-primary);
  background-color: var(--fts-primary-lighter);
  border-radius: var(--fts-radius-full, 20px);
  line-height: 22px;
}
</style>
