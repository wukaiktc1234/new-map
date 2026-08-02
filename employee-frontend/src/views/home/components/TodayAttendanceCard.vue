<template>
  <div
    class="attendance-card card-enhanced"
    :class="{
      'attendance-card--loading': loading,
      'attendance-card--active': data && data.status !== 'not_clocked_in' && data.expectedHours > 0,
    }"
    @click="handleClick"
  >
    <!-- Loading 骨架屏 -->
    <div v-if="loading" class="att-loading">
      <div class="att-skeleton att-skeleton--header"></div>
      <div class="att-skeleton att-skeleton--time"></div>
      <div class="att-skeleton att-skeleton--bar"></div>
    </div>

    <!-- 无数据 / 休息日 -->
    <div v-else-if="!data || data.expectedHours === 0" class="att-empty">
      <el-icon :size="28" color="var(--fts-text-tertiary)"><Timer /></el-icon>
      <p class="att-empty__text">{{ data?.shiftName === '休息' ? '今日休息，无考勤记录' : '暂无考勤数据' }}</p>
    </div>

    <!-- 考勤数据展示 -->
    <template v-else>
      <!-- 头部：标题 + 状态标签 -->
      <div class="att-header">
        <h3 class="att-title">
          <el-icon class="att-title__icon" :size="14"><Timer /></el-icon>
          今日考勤
        </h3>
        <StatusTag :status="statusTagValue" size="small" />
        <el-icon class="att-arrow" :size="14"><ArrowRight /></el-icon>
      </div>

      <!-- 打卡时间行 -->
      <div class="att-times">
        <div class="att-time-item">
          <span class="att-time__label">上班</span>
          <span class="att-time__value" :class="{ 'att-time__value--pending': !data.clockInTime }">
            {{ data.clockInTime || '--:--' }}
          </span>
        </div>

        <div class="att-time-divider"></div>

        <div class="att-time-item">
          <span class="att-time__label">下班</span>
          <span class="att-time__value" :class="{ 'att-time__value--pending': !data.clockOutTime }">
            {{ data.clockOutTime || (data.status === 'normal' ? '未打卡' : '--:--') }}
          </span>
        </div>

        <!-- 工时显示 -->
        <div class="att-hours">
          <span class="att-hours__current">{{ data.currentHours.toFixed(1) }}</span>
          <span class="att-hours__unit">/ {{ data.expectedHours }}h</span>
        </div>
      </div>

      <!-- 工时进度条 -->
      <div class="att-progress">
        <div
          class="att-progress__fill"
          :style="{ width: hoursPercent + '%' }"
          :class="`att-progress__fill--${progressVariant}`"
        ></div>
      </div>

      <!-- 异常提示条（仅异常状态显示） -->
      <div v-if="data.exceptionType" class="att-exception">
        <el-icon :size="13" color="var(--fts-warning)"><WarningFilled /></el-icon>
        <span class="att-exception__text">{{ data.exceptionType }}</span>
        <button class="att-exception__action" @click.stop="handleExceptionAction">
          去处理
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Timer, ArrowRight, WarningFilled } from '@element-plus/icons-vue'
import { StatusTag } from '@/components/core'
import type { TodayAttendance } from '@/api/attendance'

interface Props {
  /** 考勤数据 */
  data: TodayAttendance | null
  /** 是否加载中 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
})

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'exception-action'): void
}>()

/** 考勤状态 → StatusTag status 映射 */
const STATUS_TAG_MAP: Record<TodayAttendance['status'], string> = {
  normal: 'active',
  late: 'warning',
  early_leave: 'warning',
  absent: 'error',
  not_clocked_in: 'default',
  overtime: 'processing',
}

/** 状态标签值 */
const statusTagValue = computed(() => {
  if (!props.data) return 'default'
  return STATUS_TAG_MAP[props.data.status] || 'default'
})

/** 工时完成百分比（上限100%） */
const hoursPercent = computed(() => {
  if (!props.data || props.data.expectedHours <= 0) return 0
  return Math.min(100, Math.round((props.data.currentHours / props.data.expectedHours) * 100))
})

/** 进度条颜色变体 */
const progressVariant = computed<'success' | 'warning' | 'error' | 'default'>(() => {
  if (!props.data) return 'default'
  const s = props.data.status
  if (s === 'normal' || s === 'overtime') return 'success'
  if (s === 'late' || s === 'early_leave') return 'warning'
  if (s === 'absent') return 'error'
  return 'default'
})

function handleClick() {
  emit('click')
}

function handleExceptionAction() {
  emit('exception-action')
}
</script>

<style scoped lang="scss">
.attendance-card {
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  border: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-4);
  cursor: pointer;
  position: relative;
  min-height: 120px;
  transition: box-shadow 0.25s ease, border-color 0.25s ease;

  &:hover {
    border-color: var(--fts-primary-light);
    box-shadow: var(--fts-shadow-md);
    /* 覆盖 .card-enhanced 的 translateY 上浮动画 */
    transform: none !important;
  }

  /* 状态指示器改为右上角的小点或背景色块 */
  &--active {
    // 移除传统的 border-left
  }
}

/* ── Loading 骨架屏 ── */
.att-loading {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-1, 4px) 0;
}

.att-skeleton {
  height: 14px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-secondary) 25%,
    var(--fts-bg-tertiary) 50%,
    var(--fts-bg-secondary) 75%
  );
  background-size: 200% 100%;
  animation: att-shimmer 1.5s infinite;
  border-radius: var(--fts-radius-sm, 4px);

  &--header { width: 35%; }
  &--time { width: 60%; }
  &--bar { width: 100%; height: 8px; }
}

@keyframes att-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* ── 空状态 ── */
.att-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2, 8px);
  min-height: 100px;
  color: var(--fts-text-tertiary);
}

.att-empty__text {
  margin: 0;
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-tertiary);
}

/* ── 头部 ── */
.att-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
  margin-bottom: var(--fts-space-3, 12px);
}

.att-title {
  margin: 0;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  display: flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  flex: 1;
}

.att-title__icon {
  color: var(--fts-primary);
}

.att-arrow {
  color: var(--fts-text-tertiary);
}

/* ── 打卡时间行 ── */
.att-times {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
  margin-bottom: var(--fts-space-2, 8px);
}

.att-time-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.att-time__label {
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-quaternary);
  font-weight: 500;
}

.att-time__value {
  font-size: var(--fts-font-size-lg);
  font-weight: 700;
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5px;

  &--pending {
    color: var(--fts-text-quaternary);
    font-weight: 500;
  }
}

.att-time-divider {
  width: 1px;
  height: 28px;
  background-color: var(--fts-border-secondary);
  flex-shrink: 0;
}

.att-hours {
  margin-left: auto;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.att-hours__current {
  font-size: var(--fts-font-size-lg, 17px);
  font-weight: 700;
  color: var(--fts-primary);
  font-variant-numeric: tabular-nums;
}

.att-hours__unit {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
}

/* ── 工时进度条 ── */
.att-progress {
  height: 6px;
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full, 999px);
  overflow: hidden;
}

.att-progress__fill {
  height: 100%;
  border-radius: var(--fts-radius-full, 999px);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);

  &--success { background-color: var(--fts-success); }
  &--warning { background-color: var(--fts-warning); }
  &--error   { background-color: var(--fts-error); }
  &--default { background-color: var(--fts-text-quaternary); opacity: 0.3; }
}

/* ── 异常提示条 ── */
.att-exception {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  margin-top: var(--fts-space-2, 8px);
  padding: var(--fts-space-1, 4px) var(--fts-space-2, 8px);
  background-color: rgba(var(--fts-warning-rgb), 0.08);
  border-radius: var(--fts-radius-sm, 4px);
}

.att-exception__text {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-warning);
  font-weight: 500;
  flex: 1;
}

.att-exception__action {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-warning);
  font-weight: 600;
  background: none;
  border: none;
  padding: 2px var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-sm, 4px);
  cursor: pointer;
  white-space: nowrap;
}

/* ── 响应式适配 ── */
@media (max-width: 768px) {
  .attendance-card {
    padding: var(--fts-space-3, 12px);
    min-height: 100px;
  }

  .att-time__value {
    font-size: var(--fts-font-size-base, 14px);
  }

  .att-hours__current {
    font-size: var(--fts-font-size-base, 14px);
  }
}

/* ── 减少动画偏好 ── */
@media (prefers-reduced-motion: reduce) {
  .attendance-card,
  .att-arrow,
  .att-skeleton,
  .att-progress__fill {
    transition: none;
    animation: none;
  }
}
</style>
