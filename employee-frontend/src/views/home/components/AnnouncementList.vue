<template>
  <!-- 仅在有公告数据时显示 -->
  <div v-if="shouldDisplay" class="announcement-list">
    <!-- 标题栏 -->
    <header class="announcement-header">
      <h3 class="announcement-title">
        <el-icon class="title-icon" :size="16"><Promotion /></el-icon>
        最新公告
      </h3>
    </header>

    <!-- Loading 状态：骨架屏 -->
    <div v-if="loading" class="announcement-loading">
      <div v-for="i in 2" :key="i" class="skeleton-item">
        <div v-if="i === 1" class="skeleton-important-mark"></div>
        <div class="skeleton-content">
          <div class="skeleton-line skeleton-line--title"></div>
          <div class="skeleton-line skeleton-line--time"></div>
        </div>
      </div>
    </div>

    <!-- 公告列表（最多2条） -->
    <template v-else>
      <ul class="announcement-items">
        <li
          v-for="item in displayAnnouncements"
          :key="item.id"
          class="announcement-item"
          :class="{ 'announcement-item--important': item.isImportant, 'announcement-item--read': item.isRead }"
          @click="handleClickItem(item.id)"
        >
          <!-- 重要公告：左侧红色竖线标记 -->
          <span v-if="item.isImportant" class="important-mark"></span>

          <!-- 公告内容 -->
          <div class="announcement-content">
            <span class="announcement-title-text" :class="{ 'text-bold': item.isImportant }">
              {{ item.title }}
            </span>
            <span class="announcement-time">{{ item.time }}</span>
          </div>

          <!-- 未读指示器 -->
          <span v-if="!item.isRead" class="unread-dot"></span>
        </li>
      </ul>

      <!-- 底部：查看全部链接 -->
      <footer v-if="announcements.length > 0" class="announcement-footer">
        <button
          type="button"
          class="view-all-btn"
          @click="handleViewAll"
        >
          查看全部公告
          <el-icon :size="14"><ArrowRight /></el-icon>
        </button>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowRight, Promotion } from '@element-plus/icons-vue'

/** 公告项接口 */
export interface AnnouncementItem {
  /** 唯一标识 */
  id: string
  /** 公告标题 */
  title: string
  /** 是否为重要公告 */
  isImportant: boolean
  /** 时间显示（相对时间格式） */
  time: string
  /** 是否已读 */
  isRead: boolean
}

/** 组件属性接口 */
interface Props {
  /** 公告列表 */
  announcements: AnnouncementItem[]
  /** 是否加载中 */
  loading: boolean
}

const props = withDefaults(defineProps<Props>(), {
  announcements: () => [],
  loading: false
})

/** 组件事件 */
const emit = defineEmits<{
  (e: 'click-item', id: string): void
  (e: 'view-all'): void
}>()

/** 是否应该显示整个区块（有数据或正在加载时显示，否则隐藏） */
const shouldDisplay = computed(() => {
  return props.loading || props.announcements.length > 0
})

/** 显示的公告列表（最多2条） */
const displayAnnouncements = computed(() => props.announcements.slice(0, 2))

/** 点击单个公告项 */
const handleClickItem = (id: string) => {
  emit('click-item', id)
}

/** 查看全部公告 */
const handleViewAll = () => {
  emit('view-all')
}
</script>

<style scoped lang="scss">
.announcement-list {
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg, 8px);
  border: 1px solid var(--fts-border-secondary);
  overflow: hidden;
  transition: box-shadow var(--fts-duration-fast, 150ms) ease;

  &:hover {
    box-shadow: var(--fts-shadow-sm);
  }
}

/* 标题栏 */
.announcement-header {
  padding: var(--fts-space-3, 12px) var(--fts-space-4, 16px);
  border-bottom: 1px solid var(--fts-border-secondary);
  background-color: var(--fts-bg-secondary);
}

.announcement-title {
  margin: 0;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
}

.title-icon {
  color: var(--fts-warning);
}

/* Loading 骨架屏 */
.announcement-loading {
  padding: var(--fts-space-4, 16px);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3, 12px);
}

.skeleton-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-2, 8px) 0;
}

.skeleton-important-mark {
  width: 3px;
  height: 32px;
  background-color: var(--fts-bg-tertiary);
  border-radius: 2px;
  flex-shrink: 0;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1, 4px);
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-secondary) 25%,
    var(--fts-bg-tertiary) 50%,
    var(--fts-bg-secondary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--fts-radius-sm, 4px);

  &--title {
    width: 70%;
  }

  &--time {
    width: 40%;
    height: 12px;
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 公告列表 */
.announcement-items {
  list-style: none;
  margin: 0;
  padding: var(--fts-space-3, 12px) var(--fts-space-4, 16px);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2, 8px);
}

.announcement-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-3, 12px);
  padding-left: var(--fts-space-4, 16px); /* 为重要标记留出空间 */
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  transition: all var(--fts-duration-fast, 150ms) ease;

  &:hover {
    background-color: var(--fts-bg-secondary);

    .announcement-title-text {
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

  /* 重要公告样式 */
  &--important {
    padding-left: var(--fts-space-5, 20px); /* 增加左侧内边距以容纳红色竖线 */
    background-color: var(--fts-error-lighter);

    &:hover {
      background-color: var(--fts-error-light);
    }
  }

  /* 已读状态 - 灰色文字 */
  &--read {
    .announcement-title-text {
      color: var(--fts-text-tertiary);
      font-weight: var(--fts-font-weight-normal, 400);
    }
  }
}

/* 重要公告：左侧红色竖线标记 */
.important-mark {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background-color: var(--fts-error);
  border-radius: 2px;
  flex-shrink: 0;
}

/* 公告内容 */
.announcement-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-2, 8px);
  min-width: 0;
}

.announcement-title-text {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
  transition: color var(--fts-duration-fast, 150ms) ease;

  &.text-bold {
    font-weight: var(--fts-font-weight-semibold, 600);
    color: var(--fts-text-primary);
  }
}

.announcement-time {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
  white-space: nowrap;
  flex-shrink: 0;
}

/* 未读圆点指示器 */
.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--fts-primary);
  flex-shrink: 0;
}

/* 底部操作区 */
.announcement-footer {
  padding: var(--fts-space-2, 8px) var(--fts-space-4, 16px);
  border-top: 1px solid var(--fts-border-secondary);
  background-color: var(--fts-bg-secondary);
}

.view-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1, 4px);
  width: 100%;
  padding: var(--fts-space-2, 8px);
  border: none;
  background: none;
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-primary);
  cursor: pointer;
  border-radius: var(--fts-radius-md, 6px);
  transition: all var(--fts-duration-fast, 150ms) ease;

  &:hover {
    background-color: var(--fts-primary-light);
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: 2px;
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .announcement-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .announcement-time {
    align-self: flex-end;
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .announcement-item,
  .skeleton-important-mark,
  .skeleton-line,
  .view-all-btn {
    animation: none;
    transition: none;
  }
}
</style>
