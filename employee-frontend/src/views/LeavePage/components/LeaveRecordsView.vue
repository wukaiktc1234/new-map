<script setup lang="ts">
/**
 * LeaveRecordsView - 请假记录全部记录视图
 * 包含：日历查找、选中日期提示、操作工具栏、可折叠的请假记录列表
 */
import { RefreshLeft } from '@element-plus/icons-vue'
import EmptyState from '@/components/core/EmptyState.vue'
import StatusTag from '@/components/core/StatusTag.vue'

import type { LeaveRecord, CalendarDay } from '@/types/leave'
import { LEAVE_TYPE_LABEL_MAP } from '@/types'

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日']

interface Props {
  calendarYear: number
  calendarMonth: number
  calendarDays: CalendarDay[]
  calendarMonthLabel: string
  selectedDate: string | null
  selectedDateRecords: LeaveRecord[]
  historyRecords: LeaveRecord[]
  expandedTimelines: Set<string>
  expandedRecords: Set<string>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'prevMonth'): void
  (e: 'nextMonth'): void
  (e: 'selectDate', day: CalendarDay): void
  (e: 'clearSelection'): void
  (e: 'expandAllRecords'): void
  (e: 'collapseAllRecords'): void
  (e: 'toggleRecord', id: string): void
  (e: 'toggleTimeline', id: string): void
  (e: 'withdraw', id: string): void
  (e: 'viewDetail', id: string): void
}>()

const VISIBLE_NODE_COUNT = 2

function isRecordExpanded(recordId: string): boolean {
  return props.expandedRecords.has(recordId)
}

function isTimelineExpanded(recordId: string): boolean {
  return props.expandedTimelines.has(recordId)
}

function getVisibleNodes(record: LeaveRecord) {
  const nodes = record.approvalHistory
  if (isTimelineExpanded(record.id) || nodes.length <= VISIBLE_NODE_COUNT) {
    return nodes
  }
  return nodes.slice(0, VISIBLE_NODE_COUNT)
}

function hasMoreNodes(record: LeaveRecord): boolean {
  return record.approvalHistory.length > VISIBLE_NODE_COUNT
}

function getHiddenCount(record: LeaveRecord): number {
  return record.approvalHistory.length - VISIBLE_NODE_COUNT
}
</script>

<template>
  <div class="leave-records-view">
    <!-- 日历查找 -->
    <section class="calendar-section">
      <div class="cal-nav">
        <button class="cal-nav-btn" @click="$emit('prevMonth')">‹</button>
        <span class="cal-month-label">{{ calendarMonthLabel }}</span>
        <button class="cal-nav-btn" @click="$emit('nextMonth')">›</button>
      </div>
      <div class="cal-weekdays">
        <span v-for="w in WEEKDAYS" :key="w" class="cal-wd">{{ w }}</span>
      </div>
      <div class="cal-grid">
        <div
          v-for="(day, idx) in calendarDays"
          :key="idx"
          :class="[
            'cal-day',
            {
              'cal-day--other': !day.isCurrentMonth,
              'cal-day--today': day.isToday,
              'cal-day--has-record': day.hasRecord && day.isCurrentMonth,
              'cal-day--selected': selectedDate === day.fullDate,
              'cal-day--disabled': !day.hasRecord && day.isCurrentMonth,
            },
          ]"
          @click="$emit('selectDate', day)"
        >
          <span class="cal-day__num">{{ day.date }}</span>
          <span v-if="day.hasRecord && day.isCurrentMonth" class="cal-day__dot"></span>
        </div>
      </div>
      <div class="cal-hint">
        <span class="cal-hint-item"><i class="cal-hint-dot cal-hint-dot--active"></i>有请假记录</span>
        <span class="cal-hint-item"><i class="cal-hint-dot cal-hint-dot--empty"></i>无记录</span>
      </div>
    </section>

    <!-- 选中日期的记录 -->
    <section v-if="selectedDate && selectedDateRecords.length > 0" class="selected-date-section">
      <div class="sds-header">
        <h3 class="sds-title">{{ selectedDate.slice(5) }} 请假记录</h3>
        <button class="sds-clear" @click="$emit('clearSelection')">清除筛选</button>
      </div>
    </section>

    <!-- 操作栏 -->
    <div class="records-toolbar">
      <span class="rt-count">共 {{ selectedDate ? selectedDateRecords.length : historyRecords.length }} 条记录</span>
      <div class="rt-actions">
        <button class="rt-btn" @click="$emit('expandAllRecords')">全部展开</button>
        <button class="rt-btn" @click="$emit('collapseAllRecords')">全部折叠</button>
      </div>
    </div>

    <template v-if="(selectedDate ? selectedDateRecords : historyRecords).length > 0">
      <div class="record-list">
        <div
          v-for="record in (selectedDate ? selectedDateRecords : historyRecords)"
          :key="record.id"
          :class="['record-card', { 'record-card--expanded': isRecordExpanded(record.id) }]"
        >
          <!-- 折叠头部（始终可见） -->
          <div class="rc-summary" @click="$emit('toggleRecord', record.id)">
            <div class="rc-summary__left">
              <span class="rc-type">{{ LEAVE_TYPE_LABEL_MAP[record.form.leaveType] }}</span>
              <span class="rc-dates">{{ record.form.startDate.slice(5) }} ~ {{ record.form.endDate.slice(5) }}</span>
              <span class="rc-days-inline">{{ record.form.days }}天</span>
            </div>
            <div class="rc-summary__right">
              <StatusTag :status="record.status" variant="badge" size="small" />
              <span :class="['rc-chevron', { 'rc-chevron--open': isRecordExpanded(record.id) }]">›</span>
            </div>
          </div>

          <!-- 展开内容 -->
          <div v-if="isRecordExpanded(record.id)" class="rc-detail">
            <div class="rc-body">
              <span class="rc-reason">{{ record.form.reason }}</span>
            </div>

            <!-- 考勤联动标签 -->
            <div v-if="record.status === 'approved'" class="rc-attendance-link">
              <i class="rc-att-icon"></i>
              <span>考勤已同步，请假期间标记为「{{ LEAVE_TYPE_LABEL_MAP[record.form.leaveType] }}」</span>
            </div>

            <!-- 审批时间线 -->
            <div class="approval-timeline">
              <div class="tl-node tl-node--submit">
                <div class="tl-dot tl-dot--done"></div>
                <div class="tl-content">
                  <span class="tl-role">提交申请</span>
                  <span class="tl-time">{{ record.submittedAt.slice(5, 16).replace('T', ' ') }}</span>
                </div>
              </div>

              <div v-for="(node, idx) in getVisibleNodes(record)" :key="idx" :class="['tl-node', `tl-node--${node.status}`]">
                <div :class="['tl-dot', {
                  'tl-dot--done': node.status === 'approved',
                  'tl-dot--error': node.status === 'rejected',
                  'tl-dot--pending': node.status === 'pending',
                }]"></div>
                <div class="tl-content">
                  <span class="tl-role">{{ node.role }} · {{ node.approverName }}</span>
                  <span v-if="node.comment" class="tl-comment">"{{ node.comment }}"</span>
                  <span v-if="node.actionAt" class="tl-time">{{ node.actionAt.slice(5, 16).replace('T', ' ') }}</span>
                </div>
              </div>

              <button
                v-if="hasMoreNodes(record)"
                class="tl-toggle-btn"
                @click.stop="$emit('toggleTimeline', record.id)"
              >
                {{ isTimelineExpanded(record.id) ? '收起' : `展开更多(${getHiddenCount(record)})` }}
              </button>
            </div>

            <button v-if="record.canWithdraw" class="withdraw-btn" @click="$emit('withdraw', record.id)">
              <el-icon :size="14"><RefreshLeft /></el-icon>
              撤回申请
            </button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <EmptyState
        title="暂无请假记录"
        description="点击右上角「申请请假」按钮提交请假申请"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
// ========== 全部记录 ==========
.record-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.record-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: 12px 16px;
  transition: border-color var(--fts-duration-fast) var(--fts-easing-default);

  &:hover { border-color: var(--fts-border-hover); }

  &--expanded {
    border-color: var(--fts-border-hover);
  }

  &:hover { border-color: var(--fts-border-hover); }
}

.rc-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s ease;

  &:hover { background: var(--fts-bg-hover); }
  &:active { background: var(--fts-bg-tertiary); }

  &__left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    min-width: 0;
    flex: 1;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-shrink: 0;
  }
}

.rc-type { font-size: var(--fts-font-size-sm); font-weight: 600; color: var(--fts-text-primary); }

.rc-dates { font-size: 11px; color: var(--fts-text-tertiary); }

.rc-days-inline {
  font-size: 11px;
  font-weight: 600;
  color: var(--fts-primary);
  background: rgba(var(--fts-primary-rgb), 0.08);
  padding: 1px 6px;
  border-radius: var(--fts-radius-xs);
  flex-shrink: 0;
}

.rc-chevron {
  font-size: 16px;
  color: var(--fts-text-tertiary);
  transition: transform 0.2s ease;
  line-height: 1;

  &--open {
    transform: rotate(90deg);
  }
}

.rc-detail {
  padding: 0 16px 12px;
  border-top: 1px solid var(--fts-border-secondary);
}

.rc-body {
  display: flex;
  gap: var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin-bottom: var(--fts-space-2);
}

.rc-reason { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.rc-attendance-link {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
  background: rgba(var(--fts-success-rgb), 0.06);
  border-radius: var(--fts-radius-sm);
  font-size: 11px;
  color: var(--fts-success);
  font-weight: 500;
}

.rc-att-icon {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--fts-success);
  flex-shrink: 0;
}

.approval-timeline {
  padding-left: var(--fts-space-4);
  border-left: 2px solid var(--fts-border-primary);
  margin-bottom: var(--fts-space-4);
}

.tl-node {
  display: flex;
  gap: var(--fts-space-3);
  position: relative;
  padding: var(--fts-space-3) 0;
  padding-left: var(--fts-space-4);

  &::before {
    content: '';
    position: absolute;
    left: -17px;
    top: 8px;
    width: 10px;
    height: 2px;
    background: var(--fts-border-primary);
  }
}

.tl-dot {
  position: absolute;
  left: -21px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid;

  &--done { background: var(--fts-success); border-color: var(--fts-success); }
  &--error { background: var(--fts-error); border-color: var(--fts-error); }
  &--pending { background: var(--fts-bg-card); border-color: var(--fts-border-primary); }
}

.tl-content {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.tl-role { font-size: 12px; font-weight: 600; color: var(--fts-text-primary); }

.tl-comment {
  font-size: 11px;
  color: var(--fts-text-tertiary);
  font-style: italic;
}

.tl-time { font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary); }

.tl-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--fts-space-2);
  margin-left: var(--fts-space-4);
  padding: var(--fts-space-1) var(--fts-space-3);
  border: none;
  border-radius: var(--fts-radius-sm);
  background: transparent;
  color: var(--fts-primary);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover { background: rgba(var(--fts-primary-rgb), 0.06); }
  &:active { transform: scale(0.96); }
}

.withdraw-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-1) var(--fts-space-3);
  border: 1px dashed var(--fts-warning);
  border-radius: var(--fts-radius-sm);
  background: transparent;
  color: var(--fts-warning);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover { background: rgba(var(--fts-warning-rgb), 0.06); }
  &:active { transform: scale(0.96); }
}

// ========== 日历查找 ==========
.calendar-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fts-space-3);
}

.cal-nav-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  background: transparent;
  color: var(--fts-text-secondary);
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;

  &:hover { border-color: var(--fts-primary); color: var(--fts-primary); }
  &:active { transform: scale(0.95); }
}

.cal-month-label {
  font-size: var(--fts-font-size-base);
  font-weight: 700;
  color: var(--fts-text-primary);
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: var(--fts-space-1);
}

.cal-wd {
  text-align: center;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-text-tertiary);
  padding: var(--fts-space-1) 0;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.cal-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: var(--fts-space-2) 0;
  border-radius: var(--fts-radius-md);
  cursor: default;
  transition: all 0.15s ease;
  position: relative;

  &--other { opacity: 0.25; }

  &--today {
    .cal-day__num {
      background: var(--fts-primary);
      color: var(--fts-text-on-primary);
      border-radius: 50%;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &--has-record {
    cursor: pointer;
    background: rgba(var(--fts-primary-rgb), 0.06);

    &:hover { background: rgba(var(--fts-primary-rgb), 0.12); }
    &:active { transform: scale(0.95); }
  }

  &--selected {
    background: var(--fts-primary);

    .cal-day__num {
      color: var(--fts-text-on-primary);
      font-weight: 700;
    }

    .cal-day__dot { background: var(--fts-text-on-primary); }
  }

  &--disabled { opacity: 0.4; cursor: not-allowed; }
}

.cal-day__num {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
  line-height: 1;
}

.cal-day__dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--fts-primary);
}

.cal-hint {
  display: flex;
  gap: var(--fts-space-4);
  justify-content: center;
  margin-top: var(--fts-space-3);
}

.cal-hint-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.cal-hint-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &--active { background: var(--fts-primary); }
  &--empty { background: var(--fts-border-primary); }
}

// ========== 选中日期提示 ==========
.selected-date-section {
  margin-bottom: var(--fts-space-3);
}

.sds-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sds-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-primary);
  margin: 0;
}

.sds-clear {
  border: none;
  background: transparent;
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-xs);
  cursor: pointer;

  &:hover { color: var(--fts-error); }
}

// ========== 记录工具栏 ==========
.records-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
}

.rt-count {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: 500;
}

.rt-actions {
  display: flex;
  gap: var(--fts-space-2);
}

.rt-btn {
  border: none;
  background: transparent;
  color: var(--fts-primary);
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: var(--fts-radius-xs);

  &:hover { background: rgba(var(--fts-primary-rgb), 0.06); }
}
</style>
