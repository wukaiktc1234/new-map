<script setup lang="ts">
/**
 * ProgressTodoItem - 申请进展项
 *
 * 展示申请进展状态，包含标题、状态标签和时间。
 */
import type { ProgressTodo } from '@/types/todo'

interface Props {
  /** 进展数据 */
  item: ProgressTodo
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'navigate', url: string): void
}>()

function handleClick() {
  emit('navigate', props.item.targetUrl || '')
}
</script>

<template>
  <button class="progress-item" type="button" @click="handleClick">
    <!-- 内容区 -->
    <div class="item-content">
      <span class="item-title">{{ props.item.title }}</span>

      <div class="item-meta">
        <span v-if="props.item.statusText" class="progress-label">{{ props.item.statusText }}</span>
        <span class="item-time">{{ formatTime(props.item.createdAt ?? '') }}</span>
      </div>
    </div>

    <!-- 状态标签 -->
    <span class="status-text" :class="`status-text--${props.item.status || 'default'}`">
      {{ getStatusText(props.item.status ?? '') }}
    </span>
  </button>
</template>

<script lang="ts">
/** 格式化时间显示 */
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

/** 获取状态文字 */
function getStatusText(status: string): string {
  const map: Record<string, string> = {
    active: '进行中',
    completed: '已完成',
    rejected: '已驳回',
    pending: '待处理',
  }
  return map[status] || status || '未知'
}
</script>

<style scoped lang="scss">
.progress-item {
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

    .item-title { color: var(--fts-primary); }
  }

  &:active { transform: scale(0.98); }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: -2px;
  }
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

.item-meta {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.progress-label {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-success);
}

.item-time {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-quaternary);
}

.status-text {
  flex-shrink: 0;
  padding: 2px 10px;
  font-size: var(--fts-font-size-xs, 11px);
  font-weight: var(--fts-font-weight-semibold, 600);
  border-radius: var(--fts-radius-full, 20px);
  line-height: 20px;

  &--active {
    background-color: var(--fts-primary-lighter);
    color: var(--fts-primary);
  }

  &--completed {
    background-color: rgba(var(--fts-success-rgb), 0.08);
    color: var(--fts-success);
  }

  &--rejected {
    background-color: rgba(var(--fts-error-rgb), 0.08);
    color: var(--fts-error);
  }

  &--pending,
  &--default {
    background-color: var(--fts-bg-tertiary);
    color: var(--fts-text-tertiary);
  }
}
</style>
