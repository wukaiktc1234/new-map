<script setup lang="ts">
/**
 * LeaveOverview - 请假管理总览视图
 * 包含：假期余额Hero、本周考勤联动、审批中的请假列表、快捷入口
 */
import { Plus, Clock, Calendar, TrendCharts } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { RefreshLeft } from '@element-plus/icons-vue'

import type { LeaveBalance, LeaveRecord, AttendanceDay } from '@/types/leave'
import { LEAVE_TYPE_LABEL_MAP } from '@/types'

const props = defineProps<{
  balances: LeaveBalance[]
  totalRemaining: number
  recentAttendance: AttendanceDay[]
  historyRecords: LeaveRecord[]
  pendingCount: number
}>()

const emit = defineEmits<{
  (e: 'createLeave'): void
  (e: 'goApprovalCenter'): void
  (e: 'withdraw', id: string): void
  (e: 'viewDetail', id: string): void
  (e: 'goSchedule'): void
}>()

function getBalanceColor(remaining: number, total: number): string {
  if (total === 0) return 'var(--fts-text-quaternary)'
  if (remaining <= 1) return 'var(--fts-error)'
  if (remaining <= total * 0.3) return 'var(--fts-warning)'
  return 'var(--fts-primary)'
}

function getAttendanceStatusClass(day: AttendanceDay): string {
  const map: Record<string, string> = {
    normal: 'att-cell--normal',
    leave: 'att-cell--leave',
    late: 'att-cell--late',
    absent: 'att-cell--absent',
    holiday: 'att-cell--holiday',
    rest: 'att-cell--rest',
  }
  return map[day.status] || ''
}
</script>

<template>
  <div class="leave-overview">
    <!-- 假期余额 Hero -->
    <section class="balance-hero">
      <div class="bh-main">
        <span class="bh-label">可用假期余额</span>
        <span class="bh-value">{{ totalRemaining }}</span>
        <span class="bh-unit">天</span>
      </div>
      <div class="bh-grid">
        <div v-for="b in balances" :key="b.type" class="bh-item">
          <div class="bhi-top">
            <span class="bhi-type">{{ LEAVE_TYPE_LABEL_MAP[b.type] }}</span>
            <span class="bhi-nums" :style="{ color: getBalanceColor(b.remaining, b.total) }">
              {{ b.remaining }}<span class="bhi-total">/{{ b.total }}</span>
            </span>
          </div>
          <div class="bhi-bar">
            <div
              class="bhi-bar-fill"
              :style="{
                width: b.total > 0 ? `${(b.used / b.total) * 100}%` : '0%',
                background: getBalanceColor(b.remaining, b.total),
              }"
            ></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 本周考勤联动 -->
    <section class="section-block">
      <div class="sb-header">
        <h3 class="sb-title">
          <el-icon :size="14"><Calendar /></el-icon>
          本周考勤
        </h3>
        <button class="sb-link" @click="$emit('goSchedule')">查看排班 →</button>
      </div>
      <div class="attendance-row">
        <div
          v-for="day in recentAttendance"
          :key="day.date"
          :class="['att-cell', getAttendanceStatusClass(day)]"
        >
          <span class="att-weekday">{{ day.weekday }}</span>
          <span class="att-date">{{ day.date.slice(3) }}</span>
          <span class="att-label">{{ day.label }}</span>
        </div>
      </div>
      <div class="att-legend">
        <span class="att-legend-item"><i class="att-dot att-dot--normal"></i>正常</span>
        <span class="att-legend-item"><i class="att-dot att-dot--leave"></i>请假</span>
        <span class="att-legend-item"><i class="att-dot att-dot--rest"></i>休息</span>
      </div>
    </section>

    <!-- 审批中的请假 -->
    <section v-if="pendingCount > 0" class="section-block">
      <div class="sb-header">
        <h3 class="sb-title">
          <el-icon :size="14"><Clock /></el-icon>
          审批中
          <span class="sb-count">{{ pendingCount }}</span>
        </h3>
      </div>
      <div class="pending-list">
        <div
          v-for="record in historyRecords.filter(r => r.status === 'pending')"
          :key="record.id"
          class="pending-card"
        >
          <div class="pc-row">
            <span class="pc-type">{{ LEAVE_TYPE_LABEL_MAP[record.form.leaveType] }}</span>
            <span class="pc-dates">{{ record.form.startDate.slice(5) }} ~ {{ record.form.endDate.slice(5) }}</span>
          </div>
          <div class="pc-row pc-row--sub">
            <span class="pc-days">{{ record.form.days }}天</span>
            <span class="pc-reason">{{ record.form.reason }}</span>
          </div>
          <div class="pc-actions">
            <button class="pc-withdraw" @click="$emit('withdraw', record.id)">
              <el-icon :size="12"><RefreshLeft /></el-icon>
              撤回
            </button>
            <button class="pc-detail" @click="$emit('viewDetail', record.id)">
              查看详情 →
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 快捷入口 -->
    <section class="quick-links">
      <button class="ql-btn" @click="$emit('createLeave')">
        <el-icon :size="16"><Plus /></el-icon>
        申请请假
      </button>
      <button class="ql-btn ql-btn--outline" @click="$emit('goApprovalCenter')">
        <el-icon :size="16"><TrendCharts /></el-icon>
        我的申请
      </button>
    </section>
  </div>
</template>

<style scoped lang="scss">
// ========== 假期余额 Hero ==========
.balance-hero {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5);
  margin-bottom: var(--fts-space-4);
}

.bh-main {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-4);
}

.bh-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  font-weight: 500;
}

.bh-value {
  font-size: 32px;
  font-weight: 800;
  color: var(--fts-primary);
  line-height: 1;
}

.bh-unit {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.bh-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-3);

  @media (min-width: 768px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

.bh-item {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3);
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-md);
}

.bhi-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bhi-type {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  font-weight: 500;
}

.bhi-nums {
  font-size: var(--fts-font-size-base);
  font-weight: 700;
  line-height: 1;
}

.bhi-total {
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  color: var(--fts-text-tertiary);
}

.bhi-bar {
  width: 100%;
  height: 4px;
  background: var(--fts-bg-page);
  border-radius: 2px;
  overflow: hidden;
}

.bhi-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}

// ========== 通用 Section Block ==========
.section-block {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.sb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
}

.sb-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.sb-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--fts-warning);
  color: var(--fts-text-on-primary);
  font-size: 10px;
  font-weight: 700;
}

.sb-link {
  border: none;
  background: transparent;
  color: var(--fts-primary);
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  cursor: pointer;
  padding: 2px 4px;

  &:hover { text-decoration: underline; }
}

// ========== 考勤联动 ==========
.attendance-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: var(--fts-space-3);
}

.att-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--fts-space-2) 2px;
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-secondary);
  transition: all 0.15s ease;

  &--normal {
    border-left: 3px solid var(--fts-success);
    .att-date { color: var(--fts-text-primary); }
    .att-label { color: var(--fts-success); }
  }

  &--leave {
    background: rgba(var(--fts-primary-rgb), 0.10);
    border-left: 3px solid var(--fts-primary);
    .att-date { color: var(--fts-primary); }
    .att-label { color: var(--fts-primary); }
  }

  &--late {
    background: rgba(var(--fts-warning-rgb), 0.08);
    border-left: 3px solid var(--fts-warning);
    .att-label { color: var(--fts-warning); }
  }

  &--absent {
    background: rgba(var(--fts-error-rgb), 0.08);
    border-left: 3px solid var(--fts-error);
    .att-date { color: var(--fts-error); }
    .att-label { color: var(--fts-error); }
  }

  &--rest {
    background: var(--fts-bg-tertiary);
    border-left: 3px solid transparent;
    .att-weekday { color: var(--fts-text-quaternary); }
    .att-date { color: var(--fts-text-tertiary); opacity: 0.6; }
    .att-label { color: var(--fts-text-quaternary); }
  }

  &--holiday {
    background: rgba(var(--fts-info-rgb), 0.06);
    border-left: 3px solid var(--fts-info);
    .att-label { color: var(--fts-info); }
  }
}

.att-weekday {
  font-size: 9px;
  color: var(--fts-text-tertiary);
  font-weight: 600;
}

.att-date {
  font-size: var(--fts-font-size-sm);
  font-weight: 700;
  color: var(--fts-text-primary);
  line-height: 1.3;
}

.att-label {
  font-size: 8px;
  font-weight: 600;
  color: var(--fts-text-tertiary);
  white-space: nowrap;
}

.att-legend {
  display: flex;
  gap: var(--fts-space-4);
  justify-content: center;
}

.att-legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.att-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;

  &--normal { background: var(--fts-success); }
  &--leave { background: var(--fts-primary); }
  &--rest { background: var(--fts-text-tertiary); opacity: 0.5; }
}

// ========== 审批中 ==========
.pending-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.pending-card {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-warning-rgb), 0.04);
  border: 1px solid rgba(var(--fts-warning-rgb), 0.15);
  border-radius: var(--fts-radius-md);
}

.pc-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);

  &--sub {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
    margin-bottom: var(--fts-space-2);
  }
}

.pc-type {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.pc-dates {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.pc-days {
  font-weight: 600;
  color: var(--fts-primary);
}

.pc-reason {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pc-withdraw {
  display: inline-flex;
  align-items: center;
  gap: 4px;
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
}

.pc-detail {
  border: none;
  background: transparent;
  color: var(--fts-primary);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;

  &:hover { text-decoration: underline; }
}

// ========== 快捷入口 ==========
.quick-links {
  display: flex;
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-2);
}

.ql-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1px solid var(--fts-primary);
  border-radius: var(--fts-radius-md);
  background: var(--fts-primary);
  color: var(--fts-text-on-primary);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.97); }

  &--outline {
    background: transparent;
    color: var(--fts-primary);
    border-color: var(--fts-border-primary);

    &:hover { border-color: var(--fts-primary); }
  }
}
</style>
