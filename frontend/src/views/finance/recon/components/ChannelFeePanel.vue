<script setup lang="ts">
/**
 * 渠道手续费卡（P3-2 / P3-3 收尾修复）
 *
 * 【缺陷修复 2026-10-25 — AC-50b】此前固定传 `runId = currentRun.runId`（如 34/35），
 * 而 CHANNEL_FEE 差异属于 run 36 → 被 runId 过滤成 0 行、total 全 0。
 * **手续费是跨批次汇总维度**，默认不锁批次：
 *   - 默认：只带 7 天窗 from/to（全部批次）
 *   - 勾选「仅当前批次」后才传 runId
 *
 * 另：后端响应字段是 `rows`（不是文档里写的 groups），API 层已兼容两种命名。
 *
 * 数据口径（E2E 硬要求）：
 * - `data-fee-fen` / `data-diff-fen` / `data-count` **直接取接口 total.***，不前端重算；
 * - 未就绪（idle/loading）时三个属性输出**空串**（不写假 0），并暴露 `data-fee-state` 供等待。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { getChannelFeeSummary, type ChannelFeeGroupVO } from '@/api/finance-store-recon'
import { formatFen } from '@/types/finance-recon'

interface Props {
  /** 当前跑批ID：只有勾选「仅当前批次」时才会下发给后端 */
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
const loadedOk = ref(false)
const errorText = ref('')

/** 是否只看当前批次（默认 false：手续费跨批次汇总） */
const scopeCurrent = ref(false)

const effectiveRunId = computed<number | null>(() => {
  if (!scopeCurrent.value) return null
  const id = props.runId
  return typeof id === 'number' && Number.isFinite(id) && id > 0 ? id : null
})

const scopeHint = computed<string>(() => {
  if (!scopeCurrent.value) return '范围：近 7 天（全部批次）'
  return effectiveRunId.value
    ? '范围：当前批次 #' + effectiveRunId.value
    : '范围：当前批次（无可用批次ID，已回退全部批次）'
})

/** 状态机：loading → ready（成功拿到接口响应）/ idle（未开始或失败） */
const feeState = computed<'loading' | 'ready' | 'idle'>(() => {
  if (loading.value) return 'loading'
  return loadedOk.value ? 'ready' : 'idle'
})

const ready = computed<boolean>(() => feeState.value === 'ready')

/** 合计值：直接取接口 total.*（缺失即 null，不前端求和） */
const totalCount = computed<number | null>(() => total.value?.diffCount ?? null)
const totalFeeFen = computed<number | null>(() => total.value?.feeTotalFen ?? null)
const totalDiffFen = computed<number | null>(() => total.value?.diffAmountFen ?? null)

/** 未就绪 → 空串；就绪 → 接口原值（null 也输出空串，避免假 0） */
function attr(value: number | null): number | '' {
  if (!ready.value) return ''
  return value === null ? '' : value
}

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

async function load(): Promise<void> {
  loading.value = true
  loadedOk.value = false
  errorText.value = ''
  try {
    const data = await getChannelFeeSummary({
      runId: effectiveRunId.value,
      from: props.from,
      to: props.to,
    })
    rows.value = data.groups
    total.value = data.total
    loadedOk.value = true
  } catch (error) {
    rows.value = []
    total.value = null
    loadedOk.value = false
    errorText.value = error instanceof Error ? error.message : '渠道手续费加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => [scopeCurrent.value, props.runId, props.from, props.to], () => { void load() })

defineExpose({ reload: load })
</script>

<template>
  <section class="channel-fee" :data-fee-state="feeState" v-loading="loading">
    <header class="channel-fee__header">
      <h3 class="channel-fee__title">渠道手续费</h3>
      <el-checkbox v-model="scopeCurrent" data-test="fee-scope-current">仅当前批次</el-checkbox>
      <span class="channel-fee__hint" data-test="fee-scope-hint">{{ scopeHint }}</span>
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
          :data-fee-fen="row.feeTotalFen ?? ''"
          :data-diff-fen="row.diffAmountFen ?? ''"
          :data-count="row.diffCount ?? ''"
        >
          <td>{{ sourceTypeText(row.sourceType) }}</td>
          <td class="num">{{ row.diffCount ?? '-' }}</td>
          <td class="num">{{ row.feeTotalFen === null ? '-' : formatFen(row.feeTotalFen) }}</td>
          <td class="num" :class="{ 'is-negative': (row.diffAmountFen ?? 0) < 0 }">
            {{ row.diffAmountFen === null ? '-' : formatFen(row.diffAmountFen) }}
          </td>
        </tr>
        <tr v-if="ready && rows.length === 0">
          <td colspan="4" class="channel-fee__empty">当前范围内没有渠道手续费差异</td>
        </tr>
        <tr v-else-if="!ready">
          <td colspan="4" class="channel-fee__empty">加载中…</td>
        </tr>
      </tbody>
      <tfoot>
        <tr
          data-test="channel-fee-total"
          :data-fee-fen="attr(totalFeeFen)"
          :data-diff-fen="attr(totalDiffFen)"
          :data-count="attr(totalCount)"
          :data-fee-state="feeState"
          class="channel-fee__total"
        >
          <td>合计</td>
          <td class="num">{{ totalCount === null ? '-' : totalCount }}</td>
          <td class="num">{{ totalFeeFen === null ? '-' : formatFen(totalFeeFen) }}</td>
          <td class="num" :class="{ 'is-negative': (totalDiffFen ?? 0) < 0 }">
            {{ totalDiffFen === null ? '-' : formatFen(totalDiffFen) }}
          </td>
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
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
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
