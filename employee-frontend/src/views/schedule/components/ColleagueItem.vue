<template>
  <div
    :class="['colleague-item', { 'colleague-item--self': colleague.isSelf }]"
    @click="$emit('click', colleague.employeeId)"
  >
    <!-- 头像 -->
    <div class="colleague-item__avatar">
      <img
        v-if="colleague.avatar"
        :src="colleague.avatar"
        :alt="colleague.name"
        class="avatar-img"
      />
      <div v-else class="avatar-placeholder">
        {{ colleague.name.charAt(0) }}
      </div>
    </div>

    <!-- 信息区 -->
    <div class="colleague-item__info">
      <div class="name-row">
        <span class="name">{{ colleague.name }}</span>
        <span v-if="colleague.isSelf" class="self-tag">⭐</span>
      </div>
      <span class="position">{{ colleague.position }}</span>
    </div>

    <!-- 一周出勤圆点 -->
    <div class="colleague-item__dots">
      <span
        v-for="(dot, index) in colleague.weekDots"
        :key="index"
        :class="['week-dot', dot.isWorking ? 'week-dot--working' : 'week-dot--rest']"
      ></span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * ColleagueItem - 同事条目组件（团队视图移动端 Layer 2）
 *
 * 展示单个同事的姓名、岗位、一周出勤状态。
 * 自己的条目有特殊高亮样式。
 */
import type { TeamColleagueItem } from '@/types/schedule'

interface Props {
  /** 同事数据 */
  colleague: TeamColleagueItem
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', colleagueId: string): void
}>()
</script>

<style scoped lang="scss">
.colleague-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background-color: var(--fts-bg-white);
  border-radius: var(--fts-radius-md, 8px);
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:hover {
    background-color: var(--fts-bg-tertiary);
  }

  /* 自己的高亮样式 */
  &--self {
    background-color: var(--fts-primary-light, rgba(37, 99, 235, 0.06));

    &:hover {
      background-color: var(--fts-primary-lighter, rgba(37, 99, 235, 0.12));
    }
  }

  &__avatar {
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__dots {
    display: flex;
    align-items: center;
    gap: 5px;
    flex-shrink: 0;
  }
}

.avatar-img,
.avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  background-color: var(--fts-bg-tertiary);
}

.name-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.name {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.self-tag {
  font-size: 11px;
  line-height: 1;
  flex-shrink: 0;
}

.position {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 一周出勤圆点 */
.week-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &--working {
    background-color: var(--fts-success);
  }

  &--rest {
    border: 1.5px solid var(--fts-border-primary);
    background-color: transparent;
  }
}
</style>
