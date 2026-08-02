<template>
  <div
    :class="[
      'day-schedule-card',
      { 'day-schedule-card--today': dayData.isToday },
      { 'day-schedule-card--rest': dayData.isRest },
    ]"
    @click="handleClick"
  >
    <!-- 日期行 -->
    <div class="day-schedule-card__header">
      <span class="weekday">{{ weekdayText }}</span>
      <span class="month-day">{{ monthDayText }}</span>
      <span v-if="dayData.isToday" class="today-badge">今天</span>
    </div>

    <!-- 班次列表 / 休息状态 -->
    <div class="day-schedule-card__body">
      <template v-if="dayData.isRest || dayData.shifts.length === 0">
        <div class="rest-text">— 休息 —</div>
      </template>
      <template v-else>
        <div
          v-for="shift in dayData.shifts"
          :key="shift.shiftId"
          class="shift-row"
        >
          <span :class="['shift-type-dot', `shift-type-dot--${shift.shiftType}`]"></span>
          <span class="shift-name">{{ SHIFT_LABEL_MAP[shift.shiftType] || shift.shiftType }}</span>
          <span class="shift-time">{{ shift.startTime }}-{{ shift.endTime }}</span>
          <span v-if="shift.area" class="shift-area">{{ shift.area }}</span>
        </div>
        <!-- 班次描述文字 -->
        <div class="shift-description">
          {{ shiftDescription }}
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * DayScheduleCard - 单日排班卡片组件（个人视图 Layer 1）
 *
 * 展示某一天的班次信息，支持今天高亮、休息状态、点击交互。
 * 增强功能：完整日期显示、班次中文描述。
 */
import { computed } from 'vue'
import type { DaySchedule } from '@/types/schedule'
import { SHIFT_LABEL_MAP } from '@/types/schedule'

/** 班次类型中文描述映射 */
const SHIFT_DESCRIPTION_MAP: Record<string, string> = {
  morning: '早班 · 服务大厅 · A区',
  afternoon: '中班 · 后厨备餐区 · B区',
  evening: '晚班 · 收银台 · C区',
  night: '夜班 · 仓库值班 · D区',
}

interface Props {
  /** 单日排班数据 */
  dayData: DaySchedule
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', date: string): void
}>()

/** 从 dayOfWeekName 提取简短星期名称（如"周一"） */
const weekdayText = computed(() => {
  // dayOfWeekName 格式如 "周一"、"Tuesday" 等，取前2个中文字符或直接返回
  return props.dayData.dayOfWeekName || ''
})

/** 从 date 字段提取月日（如"6月2日"） */
const monthDayText = computed(() => {
  if (!props.dayData.date) return ''
  const date = new Date(props.dayData.date)
  return `${date.getMonth() + 1}月${date.getDate()}日`
})

/** 生成班次描述文字（合并所有班次的描述） */
const shiftDescription = computed(() => {
  if (!props.dayData.shifts || props.dayData.shifts.length === 0) return ''
  return props.dayData.shifts
    .map(shift => {
      // 优先使用映射表中的描述，否则根据班次类型和区域动态生成
      const baseDesc = SHIFT_DESCRIPTION_MAP[shift.shiftType] || ''
      if (baseDesc && !shift.area) return baseDesc
      // 动态生成：班次名称 + 区域
      const label = SHIFT_LABEL_MAP[shift.shiftType] || shift.shiftType
      const areaPart = shift.area ? ` · ${shift.area}` : ''
      return `${label}${areaPart}`
    })
    .join(' | ')
})

function handleClick(): void {
  if (!props.dayData.isRest && props.dayData.shifts.length > 0) {
    emit('click', props.dayData.date)
  }
}
</script>

<style scoped lang="scss">
.day-schedule-card {
  padding: var(--fts-space-3) var(--fts-space-4);
  background-color: var(--fts-bg-white);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 8px);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
    box-shadow: var(--fts-shadow-sm);
  }

  /* 今天高亮：左侧主色边框 */
  &--today {
    border-left: 4px solid var(--fts-primary);
    padding-left: calc(var(--fts-space-4) - 3px);
  }

  /* 休息状态：不可点击，轻微弱化 */
  &--rest {
    cursor: default;

    &:hover {
      border-color: var(--fts-border-secondary);
      box-shadow: none;
    }

    .day-schedule-card__body {
      opacity: 0.7;
    }
  }

  &__header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    margin-bottom: var(--fts-space-1);
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-1);
  }
}

.weekday {
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  min-width: 32px;
}

.month-day {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: 500;
}

.today-badge {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  font-size: var(--fts-font-size-xs);
  line-height: 18px;
  font-weight: 500;
  color: var(--fts-primary);
  background-color: var(--fts-primary-light);
  border-radius: var(--fts-radius-full, 10px);
}

.rest-text {
  text-align: center;
  padding: var(--fts-space-3) 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  letter-spacing: 2px;
  font-weight: 500;
}

/* ===== 班次行 ===== */
.shift-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: 2px 0;
  font-size: var(--fts-font-size-sm);
  line-height: 22px;
}

/* 班次类型颜色圆点 */
.shift-type-dot {
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

  &--night {
    background-color: var(--fts-info);
  }
}

.shift-name {
  font-weight: 500;
  color: var(--fts-text-primary);
  min-width: 36px;
}

.shift-time {
  color: var(--fts-text-primary);
  font-weight: 500;
  white-space: nowrap;
}

.shift-area {
  margin-left: auto;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  background-color: var(--fts-bg-tertiary);
  padding: 0 6px;
  border-radius: var(--fts-radius-sm, 4px);
  line-height: 18px;
}

/* 班次描述文字 */
.shift-description {
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-secondary);
  padding: var(--fts-space-1) 0;
  line-height: 1.5;
}
</style>
