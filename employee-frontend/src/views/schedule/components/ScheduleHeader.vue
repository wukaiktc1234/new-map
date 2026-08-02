<template>
  <div class="schedule-header">
    <!-- 导航 + 周次选择 -->
    <div class="schedule-header__nav">
      <button class="nav-btn" @click="$emit('prev-week')">
        <el-icon :size="18"><ArrowLeft /></el-icon>
      </button>
      <el-popover
        trigger="click"
        placement="bottom"
        :width="220"
        :show-arrow="false"
        popper-class="week-picker-popover"
      >
        <template #reference>
          <button class="week-trigger">
            <span class="week-number">第{{ weekRange.weekNumber }}周</span>
            <span class="week-date-range">{{ dateRangeText }}</span>
            <el-icon :size="12" class="week-chevron"><ArrowDown /></el-icon>
          </button>
        </template>
        <div class="week-picker">
          <div class="week-picker__title">选择周次</div>
          <div class="week-picker__list">
            <button
              v-for="w in weekOptions"
              :key="w.offset"
              :class="['week-option', { 'week-option--active': w.offset === weekOffset }]"
              @click="$emit('jump-week', w.offset)"
            >
              {{ w.label }}
              <span class="week-option__range">{{ w.range }}</span>
            </button>
          </div>
        </div>
      </el-popover>
      <button class="nav-btn" @click="$emit('next-week')">
        <el-icon :size="18"><ArrowRight /></el-icon>
      </button>
    </div>

    <!-- 今天按钮 -->
    <button
      v-if="showTodayButton"
      class="today-btn"
      @click="$emit('go-today')"
    >
      今天
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * ScheduleHeader - 排班表周选择器头部组件
 *
 * 提供周导航（上一周/下一周/回到今天）、快速周选择功能。
 */
import { computed } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { WeekRange, ScheduleLayer } from '@/types/schedule'

interface Props {
  /** 周范围信息 */
  weekRange: WeekRange
  /** 当前视图层级 */
  currentLayer: ScheduleLayer
  /** 周偏移量（用于判断是否显示"今天"按钮） */
  weekOffset?: number
}

const props = withDefaults(defineProps<Props>(), {
  weekOffset: 0,
})

const emit = defineEmits<{
  (e: 'prev-week'): void
  (e: 'next-week'): void
  (e: 'go-today'): void
  (e: 'toggle-layer', layer: ScheduleLayer): void
  (e: 'jump-week', offset: number): void
}>()

/** 是否显示"今天"按钮（非本周时显示） */
const showTodayButton = computed(() => props.weekOffset !== 0)

/** 日期范围格式化文本（如：6.1-6.7） */
const dateRangeText = computed(() => {
  const start = new Date(props.weekRange.start)
  const end = new Date(props.weekRange.end)
  const fmtStart = `${start.getMonth() + 1}.${start.getDate()}`
  const fmtEnd = `${end.getMonth() + 1}.${end.getDate()}`
  return `${fmtStart}-${fmtEnd}`
})

/** 快速周选择列表：本周 ±4 周 */
function fmtShort(start: string): string {
  const d = new Date(start)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

const weekOptions = computed(() => {
  const options: Array<{ offset: number; label: string; range: string }> = []
  const today = new Date()
  const currentMonday = new Date(today)
  const dayOfWeek = today.getDay()
  const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  currentMonday.setDate(today.getDate() + diff)

  for (let o = -4; o <= 4; o++) {
    const mon = new Date(currentMonday)
    mon.setDate(currentMonday.getDate() + o * 7)
    const sun = new Date(mon)
    sun.setDate(mon.getDate() + 6)
    let label = ''
    if (o === 0) label = '本周'
    else if (o === -1) label = '上周'
    else if (o === 1) label = '下周'
    else if (o < -1) label = `前${Math.abs(o)}周`
    else label = `后${o}周`

    options.push({
      offset: o,
      label,
      range: `${fmtShort(mon.toISOString())}-${fmtShort(sun.toISOString())}`,
    })
  }
  return options
})
</script>

<style scoped lang="scss">
/* ============================================================
 *  ScheduleHeader — 排班头部
 *
 *  布局: [ < 第N周 日期 ▼ > ]          [ 今天 ]
 * ============================================================ */
.schedule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  background-color: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  flex-shrink: 0;

  /* 左侧导航区 */
  &__nav {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
  }
}

/* ── 导航箭头按钮 ── */
.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--fts-radius-md);
  background: transparent;
  color: var(--fts-text-secondary);
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;

  &:hover {
    background-color: var(--fts-bg-hover);
    color: var(--fts-primary);
  }

  &:active {
    background-color: var(--fts-bg-tertiary);
  }
}

/* ── 周次触发器（可点击弹出选择器） ── */
.week-trigger {
  display: inline-flex;
  align-items: baseline;
  gap: var(--fts-space-1);
  padding: 6px var(--fts-space-2);
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:hover { background-color: var(--fts-bg-hover); }
  &:active { background-color: var(--fts-bg-tertiary); }
}

.week-number {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  white-space: nowrap;
}

.week-date-range {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  white-space: nowrap;
}

.week-chevron {
  color: var(--fts-text-quaternary);
  transition: transform 0.2s ease;
}

/* ── 今天按钮 ── */
.today-btn {
  padding: 6px var(--fts-space-3);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  background: transparent;
  color: var(--fts-primary);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
  flex-shrink: 0;

  &:hover {
    background-color: rgba(var(--fts-primary-rgb), 0.06);
    border-color: var(--fts-primary);
  }

  &:active {
    background-color: rgba(var(--fts-primary-rgb), 0.1);
  }
}
</style>

<!-- 非scoped：周选择器Popover内部样式（需穿透到body层） -->
<style lang="scss">
.el-popper.week-picker-popover {
  padding: 0;
  border-radius: var(--fts-radius-lg);
  border: 1px solid var(--fts-border-hover);
  box-shadow: var(--fts-shadow-lg);
}

.week-picker {
  padding: var(--fts-space-3);

  &__title {
    font-size: var(--fts-font-size-xs);
    font-weight: 600;
    color: var(--fts-text-tertiary);
    margin: 0 0 var(--fts-space-2);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 2px;
    max-height: 280px;
    overflow-y: auto;
  }
}

.week-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: var(--fts-space-2) var(--fts-space-3);
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
  cursor: pointer;
  transition: all 0.12s ease;
  text-align: left;

  &:hover:not(.week-option--active) {
    background-color: var(--fts-bg-hover);
  }

  &--active {
    background-color: rgba(var(--fts-primary-rgb), 0.08);
    color: var(--fts-primary);
    font-weight: 600;
  }

  &__range {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-quaternary);
    font-weight: 400;
  }

  &--active .week-option__range {
    color: var(--fts-text-tertiary);
  }
}
</style>
