<script setup lang="ts">
/**
 * PersonalWeekView - 个人周视图 (Layer 1)
 *
 * 展示当前周的7天排班卡片，每张卡片使用 DayScheduleCard 子组件。
 * 底部提供"查看全店排班"入口切换到团队视图。
 */
import { computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import DayScheduleCard from './DayScheduleCard.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import type { DaySchedule } from '@/types/schedule'

interface Props {
  /** 本周7天排班数据 */
  weekData: DaySchedule[]
  /** 加载状态 */
  loading: boolean
  /** 是否显示"查看团队排班"按钮（L3+ 可见） */
  showTeamButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  weekData: () => [],
  loading: false,
  showTeamButton: true,
})

const emit = defineEmits<{
  (e: 'view-team'): void
  (e: 'click-day', date: string): void
}>()

/** 是否有排班数据 */
const hasData = computed(() => props.weekData.length > 0)

/** 点击某一天 */
function handleClickDay(date: string) {
  emit('click-day', date)
}

/** 切换到团队视图 */
function handleViewTeam() {
  emit('view-team')
}
</script>

<template>
  <div class="personal-week-view">
    <!-- Loading 骨架屏 -->
    <div v-if="loading" class="week-loading">
      <div v-for="i in 7" :key="i" class="skeleton-card">
        <div class="skeleton-header">
          <div class="skeleton-dot" />
          <div class="skeleton-text skeleton-text--short" />
        </div>
        <div class="skeleton-body" />
      </div>
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!hasData"
      icon="calendar"
      title="本周暂无排班安排"
      description="本周没有找到排班数据，请查看其他周次"
    />

    <!-- 周视图列表 -->
    <template v-else>
      <div class="week-grid">
        <DayScheduleCard
          v-for="day in weekData"
          :key="day.date"
          :day-data="day"
          @click="handleClickDay"
        />
      </div>

      <!-- 查看全店排班（仅 L3+ 可见） -->
      <button v-if="showTeamButton" class="view-team-btn" @click="handleViewTeam">
        <span>查看全店排班</span>
        <el-icon :size="14"><ArrowDown /></el-icon>
      </button>
    </template>
  </div>
</template>

<style scoped lang="scss">
.personal-week-view {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

/* ===== Loading 骨架屏 ===== */
.week-loading {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--fts-space-2);

  @media (max-width: 767px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.skeleton-card {
  background-color: var(--fts-bg-white);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 8px);
  padding: var(--fts-space-3) var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.skeleton-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--fts-bg-tertiary);
  animation: pulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

.skeleton-text {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-tertiary) 25%,
    var(--fts-bg-secondary) 50%,
    var(--fts-bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;

  &--short {
    width: 36px;
    flex-shrink: 0;
  }

  &:not(&--short) {
    flex: 1;
  }
}

.skeleton-body {
  height: 32px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-tertiary) 25%,
    var(--fts-bg-secondary) 50%,
    var(--fts-bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ===== 周视图网格 ===== */
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--fts-space-2);

  @media (max-width: 767px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

/* ===== 查看全店按钮 ===== */
.view-team-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  width: 100%;
  padding: var(--fts-space-3) 0;
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-primary);
  background: none;
  border: 1px dashed var(--fts-border-hover);
  border-radius: var(--fts-radius-md, 8px);
  cursor: pointer;
  transition: all var(--fts-duration-fast, 150ms) ease;

  &:hover {
    background-color: rgba(var(--fts-primary-rgb), 0.06);
    border-style: solid;
    border-color: var(--fts-primary);
  }

  &:active {
    transform: scale(0.98);
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: 2px;
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .skeleton-dot,
  .skeleton-text,
  .skeleton-body {
    animation: none;
  }

  .view-team-btn {
    transition: none;
  }
}
</style>
