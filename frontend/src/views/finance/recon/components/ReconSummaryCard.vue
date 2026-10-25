<script setup lang="ts">
/**
 * 对账工作台 - 顶部摘要卡（P3-1）
 *
 * AC-46a：differenceCount **必须直接取 R3 的 differenceCount**（父组件传入 run.differenceCount），
 *         不允许前端数列表长度。
 * AC-46b：differenceAmount = SUM(diffAmount)，由父组件用 R8 分页全量聚合后以「分」传入，
 *         与 46a 的计数值是两个独立数值。
 */
import { computed } from 'vue'
import { formatFen } from '@/types/finance-recon'
import type { ReconRunVO } from '@/types/finance-recon'

interface TrendInfo {
  /** 是否有可比基线（同一账户更早的 run） */
  hasBaseline: boolean
  /** 上一次跑批的差异金额合计（分） */
  previousFen: number
  /** 本次 - 上次（分） */
  deltaFen: number
  /** 基线批次号，便于追溯 */
  baselineRunId: number | null
}

interface Props {
  run: ReconRunVO | null
  /**
   * AC-46b：R8 全量聚合的 SUM(diffAmount)，单位分。
   *
   * 【缺陷修复 2026-10-25】用 `number | null` 而不是默认 0：
   * 聚合未完成时若渲染 0，会被 E2E/用户读成"差异金额 0.00"（假 0）。
   * null = 尚不知道，渲染「—」；只有聚合真正返回后才写入分值。
   */
  diffAmountFen: number | null
  /** 较上次（由父组件用 R2 历史 run 计算，禁用 localStorage） */
  trend: TrendInfo | null
  /** 摘要区加载态 */
  loading?: boolean
  /** 差异金额聚合是否仍在进行（多页拉取中） */
  amountLoading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  amountLoading: false,
})

/** AC-46a：直接取 R3 的 differenceCount */
const diffCount = computed<number>(() => {
  const value = props.run?.differenceCount
  return typeof value === 'number' ? value : 0
})

/** AC-46b：SUM(diffAmount)（分）；null 表示尚未聚合完成（不要当成 0） */
const diffAmountFen = computed<number | null>(() => {
  const value = props.diffAmountFen
  return typeof value === 'number' && Number.isFinite(value) ? Math.trunc(value) : null
})

/** 聚合状态：loading（摘要/聚集中）/ ready（已拿到分值）/ idle（无批次） */
const amountState = computed<'loading' | 'ready' | 'idle'>(() => {
  if (diffAmountFen.value !== null) return 'ready'
  if (props.amountLoading || props.loading) return 'loading'
  return 'idle'
})

/** 展示文案：未完成时显示「—」，绝不显示误导性的 0.00 */
const diffAmountText = computed<string>(() => {
  const fen = diffAmountFen.value
  return fen === null ? '—' : formatFen(fen)
})

const matchRateText = computed<string>(() => {
  const rate = props.run?.matchRate
  return typeof rate === 'number' ? `${rate.toFixed(2)}%` : '-'
})

/** 「较上次」文案：无历史时明确写「首次跑批（无可比基线）」 */
const trendText = computed<string>(() => {
  const trend = props.trend
  if (!trend || !trend.hasBaseline) return '首次跑批（无可比基线）'
  const deltaYuan = Math.abs(trend.deltaFen) / 100
  const arrow = trend.deltaFen > 0 ? '↑' : trend.deltaFen < 0 ? '↓' : '＝'
  const prev = `${(trend.previousFen / 100).toFixed(2)}`
  if (trend.deltaFen === 0) return `与上次持平（上次 ${prev} 元）`
  return `较上次 ${arrow} ${deltaYuan.toFixed(2)} 元（上次 ${prev} 元）`
})

const trendClass = computed<string>(() => {
  const trend = props.trend
  if (!trend || !trend.hasBaseline || trend.deltaFen === 0) return 'trend--flat'
  return trend.deltaFen > 0 ? 'trend--up' : 'trend--down'
})
</script>

<template>
  <section class="recon-summary" v-loading="loading">
    <div class="recon-summary__item">
      <div class="recon-summary__label">差异笔数</div>
      <!-- AC-46a：直接来自 R3.differenceCount -->
      <div
        class="recon-summary__value"
        data-test="summary-diff-count"
        :data-difference-count="diffCount"
      >{{ diffCount }}</div>
      <div class="recon-summary__hint">R3.differenceCount（后端口径）</div>
    </div>

    <div class="recon-summary__item">
      <div class="recon-summary__label">差异金额（元）</div>
      <!-- AC-46b：SUM(diffAmount)，由 R8 全量分页聚合，单位分 -->
      <div
        class="recon-summary__value"
        data-test="summary-diff-amount"
        :data-diff-amount-fen="diffAmountFen === null ? '' : diffAmountFen"
        :data-amount-state="amountState"
      >{{ diffAmountText }}</div>
      <div class="recon-summary__hint">
        <template v-if="diffAmountFen !== null">SUM(diffAmount) = {{ diffAmountFen }} 分</template>
        <template v-else>等待 R8 聚合完成…</template>
        <span v-if="amountLoading" class="recon-summary__loading">聚合中…</span>
      </div>
    </div>

    <div class="recon-summary__item">
      <div class="recon-summary__label">较上次</div>
      <div class="recon-summary__value recon-summary__value--sm" :class="trendClass" data-test="trend-text">
        {{ trendText }}
      </div>
      <div class="recon-summary__hint">
        基线批次：{{ trend?.baselineRunId ? '#' + trend.baselineRunId : '无' }}
      </div>
    </div>

    <div class="recon-summary__item">
      <div class="recon-summary__label">当前批次</div>
      <div class="recon-summary__value recon-summary__value--sm">
        {{ run ? (run.runKey || ('#' + run.runId)) : '-' }}
      </div>
      <div class="recon-summary__hint">
        {{ run?.accountName || '未知账户' }} · {{ run?.businessDate || '-' }} · 勾稽率 {{ matchRateText }}
      </div>
    </div>
  </section>
</template>

<style scoped>
.recon-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
  background: var(--fts-bg-card, #fff);
  border-radius: 12px;
  border: 1px solid var(--fts-border-secondary, #ebeef5);
}

.recon-summary__item {
  min-width: 0;
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--fts-bg-subtle, #fafafa);
}

.recon-summary__label {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 6px;
}

.recon-summary__value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--fts-text-primary, #303133);
  font-variant-numeric: tabular-nums;
}

.recon-summary__value--sm {
  font-size: 14px;
  font-weight: 500;
  white-space: normal;
  word-break: break-all;
}

.recon-summary__hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.recon-summary__loading {
  margin-left: 8px;
  color: var(--el-color-primary, #409eff);
}

.trend--up {
  color: var(--el-color-danger, #f56c6c);
}

.trend--down {
  color: var(--el-color-success, #67c23a);
}

.trend--flat {
  color: var(--fts-text-secondary, #909399);
}

@media (max-width: 1200px) {
  .recon-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
