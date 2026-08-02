<template>
  <div
    class="schedule-card"
    :class="{ 'schedule-card--loading': loading, 'schedule-card--today': schedule && !schedule.isDayOff }"
    @click="handleClick"
  >
    <!-- Loading 状态：骨架屏 -->
    <div v-if="loading" class="schedule-loading">
      <div class="skeleton-line skeleton-line--title"></div>
      <div class="skeleton-line skeleton-line--content"></div>
    </div>

    <!-- 无数据状态 -->
    <div v-else-if="!schedule" class="schedule-empty">
      <el-icon :size="32" color="var(--fts-text-tertiary)"><Calendar /></el-icon>
      <p class="empty-text">今日暂无排班安排</p>
    </div>

    <!-- 有排班数据 -->
    <template v-else>
      <!-- 休息日 -->
      <div v-if="schedule.isDayOff" class="schedule-day-off">
        <el-icon :size="24" color="var(--fts-text-tertiary)"><Coffee /></el-icon>
        <span class="day-off-text">今日休息</span>
      </div>

      <!-- 有班次 -->
      <div v-else class="schedule-content">
        <!-- 标题栏 -->
        <div class="schedule-header">
          <h3 class="schedule-title">
            <el-icon class="title-icon" :size="16"><Calendar /></el-icon>
            今日排班
          </h3>
          <el-icon class="arrow-icon" :size="16"><ArrowRight /></el-icon>
        </div>

        <!-- 班次列表 -->
        <div class="shift-list">
          <div
            v-for="(shift, index) in schedule.shifts"
            :key="index"
            class="shift-item"
          >
            <!-- 颜色圆点 -->
            <span
              class="shift-dot"
              :class="`shift-dot--${shift.type}`"
            ></span>

            <!-- 班次信息 -->
            <div class="shift-info">
              <span class="shift-name">{{ shift.name }}</span>
              <span class="shift-time">{{ shift.startTime }} - {{ shift.endTime }}</span>
            </div>

            <!-- 工作区域（可选） -->
            <span v-if="shift.area" class="shift-area">{{ shift.area }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { Calendar, Coffee, ArrowRight } from '@element-plus/icons-vue'

/** 班次类型 */
export interface ShiftItem {
  /** 班次类型: morning/afternoon/evening */
  type: 'morning' | 'afternoon' | 'evening'
  /** 班次名称 */
  name: string
  /** 开始时间 (HH:mm格式) */
  startTime: string
  /** 结束时间 (HH:mm格式) */
  endTime: string
  /** 工作区域（可选） */
  area?: string
}

/** 日排班数据 */
export interface DaySchedule {
  /** 是否休息日 */
  isDayOff: boolean
  /** 班次列表 */
  shifts: ShiftItem[]
}

/** 组件属性接口 */
interface Props {
  /** 排班数据 */
  schedule: DaySchedule | null
  /** 是否加载中 */
  loading: boolean
}

defineProps<Props>()

/** 组件事件 */
const emit = defineEmits<{
  (e: 'click'): void
}>()

/** 点击卡片 */
const handleClick = () => {
  emit('click')
}
</script>

<style scoped lang="scss">
.schedule-card {
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg, 8px);
  border: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-4, 16px);
  cursor: pointer;
  transition: all var(--fts-duration-fast, 150ms) ease;
  position: relative;
  min-height: 120px;

  &:hover {
    box-shadow: var(--fts-shadow-md);
    transform: translateY(-2px);
    border-color: var(--fts-primary-light);
  }

  &:active {
    transform: scale(0.98);
  }

  /* 今天高亮样式 */
  &--today {
    border-left: 3px solid var(--fts-primary);
    box-shadow: var(--fts-shadow-md);

    &:hover {
      box-shadow: var(--fts-shadow-md);
    }
  }
}

/* Loading 骨架屏 */
.schedule-loading {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-2, 8px) 0;
}

.skeleton-line {
  height: 16px;
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
    width: 40%;
  }

  &--content {
    width: 80%;
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 无数据状态 */
.schedule-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2, 8px);
  min-height: 120px;
  color: var(--fts-text-tertiary);
}

.empty-text {
  margin: 0;
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-tertiary);
}

/* 休息日样式 */
.schedule-day-off {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2, 8px);
  min-height: 120px;
  background-color: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md, 6px);
  color: var(--fts-text-tertiary);
}

.day-off-text {
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-medium, 500);
}

/* 有班次内容 */
.schedule-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3, 12px);
}

.schedule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.schedule-title {
  margin: 0;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
}

.title-icon {
  color: var(--fts-primary);
}

.arrow-icon {
  color: var(--fts-text-tertiary);
  transition: transform var(--fts-duration-fast, 150ms) ease;

  .schedule-card:hover & {
    transform: translateX(4px);
    color: var(--fts-primary);
  }
}

/* 班次列表 */
.shift-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2, 8px);
}

.shift-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-2, 8px) var(--fts-space-3, 12px);
  border-radius: var(--fts-radius-md, 6px);
  background-color: var(--fts-bg-secondary);
  transition: background-color var(--fts-duration-fast, 150ms) ease;

  &:hover {
    background-color: var(--fts-bg-tertiary);
  }
}

/* 班次颜色圆点 */
.shift-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &--morning {
    background-color: var(--fts-success);
  }

  &--afternoon {
    background-color: var(--fts-warning);
  }

  &--evening {
    background-color: var(--fts-error);
  }
}

.shift-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.shift-name {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
}

.shift-time {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
}

.shift-area {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-secondary);
  background-color: var(--fts-bg-tertiary);
  padding: 2px var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-sm, 4px);
  white-space: nowrap;
  flex-shrink: 0;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .schedule-card {
    padding: var(--fts-space-3, 12px);
    min-height: 100px;
  }

  .shift-item {
    flex-wrap: wrap;
  }

  .shift-area {
    margin-left: calc(8px + var(--fts-space-3, 12px)); /* 圆点宽度 + gap */
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .schedule-card,
  .arrow-icon,
  .skeleton-line {
    transition: none;
    animation: none;
  }
}
</style>
