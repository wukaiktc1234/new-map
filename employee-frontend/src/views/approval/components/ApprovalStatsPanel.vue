<script setup lang="ts">
/**
 * ApprovalStatsPanel - 个人审批数据统计面板
 *
 * 双行布局：Hero 行展示核心指标（申请数/通过率/待处理），
 * Detail 行展示明细数据与状态分布
 *
 * @last-modified 2026-06-06
 */
import { computed } from 'vue'

interface StatsData {
  totalApplications: number
  approvedCount: number
  rejectedCount: number
  cancelledCount: number
  pendingCount: number
  passRate: number
  leaveDaysUsed: number
  leaveDaysTotal: number
  reimbursementTotal: number
  overtimeHours: number
  travelCount: number
  swapCount: number
}

interface Props {
  stats: StatsData
}

const props = defineProps<Props>()

// 通过率等级：用于颜色映射
const passRateLevel = computed(() => {
  if (props.stats.passRate >= 90) return 'success'
  if (props.stats.passRate >= 70) return 'warning'
  return 'error'
})

// 报销金额格式化（分 → 元，千分位）
function formatReimbursement(fen: number): string {
  return Math.round(fen / 100).toLocaleString()
}
</script>

<template>
  <section class="stats-panel" aria-label="审批数据统计">
    <!-- Hero Row: 核心指标 -->
    <div class="stats-hero">
      <div class="hero-stat hero-stat--primary">
        <span class="hero-stat__value">{{ stats.totalApplications }}</span>
        <span class="hero-stat__label">本年申请</span>
        <span class="hero-stat__unit">笔</span>
      </div>
      <div class="hero-divider" />
      <div class="hero-stat" :class="'hero-stat--' + passRateLevel">
        <span class="hero-stat__value">{{ stats.passRate }}%</span>
        <span class="hero-stat__label">通过率</span>
      </div>
      <div class="hero-divider" />
      <div class="hero-stat hero-stat--warning" v-if="stats.pendingCount > 0">
        <span class="hero-stat__value">{{ stats.pendingCount }}</span>
        <span class="hero-stat__label">待处理</span>
        <span class="hero-stat__unit">项</span>
      </div>
      <div class="hero-stat hero-stat--muted" v-else>
        <span class="hero-stat__value">0</span>
        <span class="hero-stat__label">待处理</span>
      </div>
    </div>

    <!-- Secondary Row: 明细信息 -->
    <div class="stats-detail">
      <div class="detail-group">
        <div class="detail-item">
          <span class="detail-value">{{ stats.leaveDaysUsed }}/{{ stats.leaveDaysTotal }}</span>
          <span class="detail-label">年假(已用/总额)</span>
        </div>
        <div class="detail-sep" />
        <div class="detail-item">
          <span class="detail-value">¥{{ formatReimbursement(stats.reimbursementTotal) }}</span>
          <span class="detail-label">报销总额</span>
        </div>
        <div class="detail-sep" />
        <div class="detail-item">
          <span class="detail-value">{{ stats.overtimeHours }}h</span>
          <span class="detail-label">加班时长</span>
        </div>
      </div>
      <div class="breakdown">
        <span class="bd-item">
          <span class="bd-dot bd-dot--ok" />
          <span class="bd-text">已通过</span>
          <span class="bd-val">{{ stats.approvedCount }}</span>
        </span>
        <span class="bd-item">
          <span class="bd-dot bd-dot--fail" />
          <span class="bd-text">已驳回</span>
          <span class="bd-val">{{ stats.rejectedCount }}</span>
        </span>
        <span class="bd-item">
          <span class="bd-dot bd-dot--cancel" />
          <span class="bd-text">已撤回</span>
          <span class="bd-val">{{ stats.cancelledCount }}</span>
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
/* ================================================================
 * ApprovalStatsPanel - 双行统计面板样式
 *
 * 布局结构：
 *   Hero Row: [申请数] | [通过率] | [待处理]
 *   Detail Row: [年假 | 报销 | 加班] ... [●已通过 ●已驳回 ●已撤回]
 *
 * 设计原则：
 *   - Hero 行突出核心数字，通过率带颜色语义
 *   - Detail 行紧凑展示辅助信息
 *   - 全部颜色通过 --fts-* CSS 变量控制
 * ================================================================ */

.stats-panel {
  margin-bottom: var(--fts-space-5);
}

// ===== Hero Row =====
.stats-hero {
  display: flex;
  align-items: stretch;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-xl);
  padding: var(--fts-space-5) var(--fts-space-6);
  gap: 0;

  @media (max-width: 640px) {
    padding: var(--fts-space-4);
  }
}

.hero-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  min-width: 0;

  &--primary {
    .hero-stat__value {
      font-size: var(--fts-font-size-2xl);
      color: var(--fts-text-primary);
    }
  }

  &--success .hero-stat__value { color: var(--fts-success); }
  &--warning .hero-stat__value { color: var(--fts-warning); }
  &--error .hero-stat__value { color: var(--fts-error); }
  &--muted .hero-stat__value { color: var(--fts-text-quaternary); }
}

.hero-stat__value {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.hero-stat__label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  font-weight: var(--fts-font-weight-medium);
}

.hero-stat__unit {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.hero-divider {
  width: 1px;
  background: var(--fts-border-secondary);
  align-self: stretch;
  margin: var(--fts-space-3) 0;
}

// ===== Detail Row =====
.stats-detail {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-lg);

  @media (max-width: 640px) {
    flex-direction: column;
    align-items: stretch;
    gap: var(--fts-space-3);
  }
}

.detail-group {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);

  @media (max-width: 640px) {
    justify-content: space-around;
  }
}

.detail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.detail-value {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.detail-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  white-space: nowrap;
}

.detail-sep {
  width: 1px;
  height: 24px;
  background: var(--fts-border-secondary);
}

// Breakdown dots
.breakdown {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);

  @media (max-width: 640px) {
    justify-content: center;
    padding-top: var(--fts-space-3);
    border-top: 1px solid var(--fts-border-secondary);
    gap: var(--fts-space-3);
  }
}

.bd-item {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
}

.bd-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;

  &--ok { background: var(--fts-success); }
  &--fail { background: var(--fts-error); }
  &--cancel { background: var(--fts-text-quaternary); }
}

.bd-text {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
}

.bd-val {
  font-size: var(--fts-font-size-xs);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}
</style>
