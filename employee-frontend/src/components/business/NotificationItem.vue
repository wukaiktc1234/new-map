<script setup lang="ts">
/**
 * NotificationItem - 通知项
 *
 * 展示通知消息，包含标题、内容摘要、时间和未读蓝点。
 */
import type { TodoNotification } from '@/types/todo'

interface Props {
  /** 通知数据 */
  item: TodoNotification
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
  <button class="notification-item" :class="{ 'notification-item--unread': !props.item.isRead }" type="button" @click="handleClick">
    <!-- 未读蓝点 -->
    <span v-if="!props.item.isRead" class="unread-dot" />

    <!-- 内容区 -->
    <div class="item-content">
      <div class="item-header">
        <span class="item-title">{{ props.item.title }}</span>
        <span class="item-time">{{ formatTime(props.item.time ?? '') }}</span>
      </div>
      <p v-if="props.item.content" class="item-summary">{{ props.item.content }}</p>
    </div>
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
</script>

<style scoped lang="scss">
.notification-item {
  display: flex;
  align-items: flex-start;
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

  &--unread {
    background-color: rgba(var(--fts-primary-rgb), 0.03);

    .item-title { font-weight: var(--fts-font-weight-semibold, 600); }
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: -2px;
  }
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--fts-primary);
  flex-shrink: 0;
  margin-top: 6px;
}

.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-2);
}

.item-title {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: font-weight var(--fts-duration-fast, 150ms) ease,
              color var(--fts-duration-fast, 150ms) ease;
}

.item-time {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-quaternary);
  flex-shrink: 0;
}

.item-summary {
  margin: 0;
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
