<script setup lang="ts">
/**
 * 渠道手续费卡（P3-2）
 *
 * 数据源：GET /v1/finance/recon/channel-fee-summary?runId&from&to
 * 展示：按 sourceType 分组的表格（笔数 / 手续费合计 / 差异金额）+ 合计行。
 * 金额单位统一为「分」，展示时 /100 保留 2 位；比较/断言用分。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { getChannelFeeSummary, type ChannelFeeGroupVO } from '@/api/finance-store-recon'
import { formatFen } from '@/types/finance-recon'

interface Props {
  /** 当前跑批ID（为空时按 from/to 时间窗汇总） */
  runId?: number | null
  /** ISO-8601 本地时间，起 */
  from?: string | null
  /** ISO-8601 本地时间，止（半开区间上界） */
  to?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  runId: null,
  from: null,
  to: null,
})

const rows = ref<ChannelFeeGroupVO[]>([])
const total = ref<ChannelFeeGroupVO | null>(null)
const loading = ref(false)
const errorText = ref('')

/** 渠道类型中文名（未知类型回退原值） */
const SOURCE_TYPE_LABELS: Record<string, string> = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  UNIONPAY: '银联',
  BANK_CARD: '银行卡',
  MEITUAN: '美团',
  ELEME: '饿了么',
  DOUYIN: '抖音',
  CASH: '现金',
}
function sourceTypeText(type: string | null): string {
  if (!type) return '未分类'
  return SOURCE_TYPE_LABELS[type] || type
}

const totalCount = computed<number>(() => {
  if (total.value && typeof total.value.diffCount === 'number') return total.value.diffCount
  return rows.value.reduce((sum, row) => sum + (row.diffCount ?? 0), 0)
})
const totalFeeFen = computed<number>(() => {
  if (total.value && typeof total.value.feeTotalFen === 'number') return total.value.feeTotalFen
  return rows.value.reduce((sum, row) => sum + (row.feeTotalFen ?? 0), 0)
})
const totalDiffFen = computed<number>(() => {
  if (total.value && typeof total.value.diffAmountFen === 'number') return total.value.diffAmountFen
  return rows.value.reduce((sum, row) => sum + (row.diffAmountFen ?? 0), 0)
})

async function load(): Promise<void> {
  loading.value = true
  errorText.value = ''
  try {
    const data = await getChannelFeeSummary({
      runId: props.runId,
      from: props.from,
      to: props.to,
    })
    rows.value = data.groups
    total.value = data.total
  } catch (error) {
    rows.value = []
    total.value = null
    errorText.value = error instanceof Error ? error.message : '渠道手续费加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => [props.runId, props.from, props.to], () => { void load() })

defineExpose({ reload: load })
</script>

<template>
  <section class="channel-fee" v-loading="loading">
    <header class="channel-fee__header">
      <h3 class="channel-fee__title">渠道手续费</h3>
      <span class="channel-fee__hint">
        {{ runId ? '当前批次 #' + runId : '按时间窗统计' }} · 金额单位：元（原始分值见 data 属性）
      </span>
    </header>

    <div v-if="errorText" class="channel-fee__error">{{ errorText }}</div>

    <table v-else class="channel-fee__table">
      <thead>
        <tr>
          <th>渠道类型</th>
          <th class="num">差异笔数</th>
          <th class="num">手续费合计</th>
          <th class="num">差异金额</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in rows"
          :key="row.sourceType || 'unknown'"
          data-test="channel-fee-row"
          :data-source-type="row.sourceType || ''"
          :data-fee-fen="row.feeTotalFen ?? 0"
          :data-diff-fen="row.diffAmountFen ?? 0"
          :data-count="row.diffCount ?? 0"
        >
          <td>{{ sourceTypeText(row.sourceType) }}</td>
          <td class="num">{{ row.diffCount ?? 0 }}</td>
          <td class="num">{{ formatFen(row.feeTotalFen) }}</td>
          <td class="num" :class="{ 'is-negative': (row.diffAmountFen ?? 0) < 0 }">
            {{ formatFen(row.diffAmountFen) }}
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td colspan="4" class="channel-fee__empty">当前批次没有渠道手续费差异</td>
        </tr>
      </tbody>
      <tfoot>
        <tr
          data-test="channel-fee-total"
          :data-fee-fen="totalFeeFen"
          :data-diff-fen="totalDiffFen"
          :data-count="totalCount"
          class="channel-fee__total"
        >
          <td>合计</td>
          <td class="num">{{ totalCount }}</td>
          <td class="num">{{ formatFen(totalFeeFen) }}</td>
          <td class="num" :class="{ 'is-negative': totalDiffFen < 0 }">{{ formatFen(totalDiffFen) }}</td>
        </tr>
      </tfoot>
    </table>
  </section>
</template>

<style scoped>
.channel-fee {
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  padding: 12px 14px;
}

.channel-fee__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.channel-fee__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-primary, #303133);
}

.channel-fee__hint {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.channel-fee__error {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
}

.channel-fee__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.channel-fee__table th,
.channel-fee__table td {
  padding: 7px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
}

.channel-fee__table th {
  color: var(--fts-text-secondary, #606266);
  font-weight: 600;
  background: var(--fts-bg-subtle, #fafafa);
}

.channel-fee__table tfoot td {
  border-bottom: none;
  font-weight: 600;
  background: var(--fts-bg-subtle, #fafafa);
}

.channel-fee__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 16px 0;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.is-negative {
  color: var(--el-color-danger, #f56c6c);
}
</style>
