<script setup lang="ts">
/**
 * KPI卡片组件
 *
 * 功能：
 * - 展示KPI汇总数据
 * - 支持加载状态
 * - 支持空数据状态
 */
import { computed } from 'vue'
import { ArrowUp, ArrowDown, Minus } from '@element-plus/icons-vue'
import type { KpiSummary } from '../types/report'

interface Props {
  data: KpiSummary | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const kpiCards = computed(() => {
  return props.data?.kpiCards || []
})

function getTrendIcon(change: number | undefined) {
  if (change === undefined || change === 0) return Minus
  return change > 0 ? ArrowUp : ArrowDown
}

function getTrendClass(change: number | undefined) {
  if (change === undefined || change === 0) return 'trend-flat'
  return change > 0 ? 'trend-up' : 'trend-down'
}
</script>

<template>
  <div class="report-kpi-cards">
    <el-skeleton v-if="loading" :rows="2" animated />
    <div v-else-if="!data || kpiCards.length === 0" class="kpi-empty">
      <el-empty description="暂无KPI数据" />
    </div>
    <div v-else class="kpi-grid">
      <div
        v-for="card in kpiCards"
        :key="card.metricKey"
        class="kpi-card"
      >
        <div class="kpi-card__header">
          <span class="kpi-card__name">{{ card.metricName }}</span>
          <el-icon :size="14" :class="getTrendClass(card.yoyChange)">
            <component :is="getTrendIcon(card.yoyChange)" />
          </el-icon>
        </div>
        <div class="kpi-card__value">
          {{ card.currentValue }}
          <span class="kpi-card__unit">{{ card.unit }}</span>
        </div>
        <div class="kpi-card__trends">
          <span v-if="card.yoyChange !== undefined" :class="getTrendClass(card.yoyChange)">
            同比 {{ card.yoyChange > 0 ? '+' : '' }}{{ card.yoyChange }}%
          </span>
          <span v-if="card.momChange !== undefined" :class="getTrendClass(card.momChange)">
            环比 {{ card.momChange > 0 ? '+' : '' }}{{ card.momChange }}%
          </span>
        </div>
        <div v-if="card.targetAchievement !== undefined" class="kpi-card__target">
          目标达成 {{ card.targetAchievement }}%
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.report-kpi-cards {
  // 由父容器控制外间距
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--fts-space-3);
}

.kpi-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-2);
  }

  &__name {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  &__value {
    font-size: var(--fts-font-size-2xl);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
  }

  &__unit {
    font-size: var(--fts-font-size-sm);
    font-weight: normal;
    color: var(--fts-text-secondary);
    margin-left: var(--fts-space-1);
  }

  &__trends {
    display: flex;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-sm);
    margin-bottom: var(--fts-space-1);
  }

  &__target {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }
}

.trend-up {
  color: var(--fts-success);
}

.trend-down {
  color: var(--fts-error);
}

.trend-flat {
  color: var(--fts-text-tertiary);
}

.kpi-empty {
  padding: var(--fts-space-4);
}
</style>
