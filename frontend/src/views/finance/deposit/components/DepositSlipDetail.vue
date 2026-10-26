<script setup lang="ts">
/**
 * 缴款单详情（N19-3b）
 *
 * 新增展示：
 * - **退款构成** refundComponentFen → `data-rcf-fen`
 * - **净值** differenceNetFen → `data-dnet-fen`，<0 标红 text-danger / >0 标绿 text-success / =0 neutral
 *   （列头 title：「净值 = actual − (expected − 退款构成)，即扣除退款后的真实长短款」）
 * - 提示条 chips：still_open / stale / legacy_null / missing-time（计数 0 不显示）
 * - 「待确认归属」区块：仅在存在边界退款时显示；三选一班次 + 原因（≥4 字）→ confirm-attribution
 */
import { computed, ref, watch } from 'vue'
import { formatFen } from '@/types/finance-recon'
import type {
  DepositSlipVO,
  PendingAttributionPayload,
  PendingAttributionRefundVO,
  ShiftCandidateVO,
} from '@/api/finance-deposit-slip'

interface Props {
  slip: DepositSlipVO | null
  /** 后端 pending-refunds 整对象：{ pendingRefunds, candidateShifts, missingRefundTimeCount } */
  attribution?: PendingAttributionPayload | null
  /** 是否有 finance:deposit-slip:confirm-attribution 权限 */
  canConfirm?: boolean
  /** 提交中 */
  confirming?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  attribution: null,
  canConfirm: false,
  confirming: false,
})

const emit = defineEmits<{
  (e: 'confirm', payload: { refundRecordId: number; shiftId: number; reason: string }): void
}>()

/** 归属确认的临时状态：每行一个 reason / 选中班次 */
const selectedShift = ref<Record<number, number>>({})
const reasons = ref<Record<number, string>>({})
const localError = ref('')

function money(fen: number | null | undefined): string {
  return typeof fen === 'number' && Number.isFinite(fen) ? formatFen(fen) : '—'
}

/** 净值三态 */
const dnetState = computed<'negative' | 'positive' | 'neutral'>(() => {
  const value = props.slip?.differenceNetFen
  if (typeof value !== 'number') return 'neutral'
  if (value < 0) return 'negative'
  if (value > 0) return 'positive'
  return 'neutral'
})

const dnetClass = computed<string>(() => {
  if (dnetState.value === 'negative') return 'text-danger'
  if (dnetState.value === 'positive') return 'text-success'
  return 'text-neutral'
})

/**
 * 提示条计数：**全部取后端字段**（A39 口径），前端不做任何状态/时间推断。
 * stillOpenCount（班次窗口未结束）/ staleCount（窗口内 refund_time > slip.create_time 的退款数）
 * / legacyNullCount（历史单据缺新字段）/ missingRefundTimeCount（退款缺发生时刻）。0 不显示。
 */
const hints = computed<Array<{ key: string; text: string; count: number }>>(() => {
  const slip = props.slip
  if (!slip) return []
  const list: Array<{ key: string; text: string; count: number }> = []

  const stillOpen = slip.stillOpenCount ?? 0
  if (stillOpen > 0) {
    list.push({ key: 'still-open', text: '本单班次窗口尚未结束（' + stillOpen + '）', count: stillOpen })
  }

  const stale = slip.staleCount ?? 0
  if (stale > 0) {
    list.push({ key: 'stale', text: '窗口内有 ' + stale + ' 笔退款的 refund_time 晚于单据生成时间', count: stale })
  }

  const legacyNull = slip.legacyNullCount ?? 0
  if (legacyNull > 0) {
    list.push({ key: 'legacy-null', text: '历史单据：' + legacyNull + ' 处缺净值字段（按旧口径）', count: legacyNull })
  }

  const missing = slip.missingRefundTimeCount ?? 0
  if (missing > 0) {
    list.push({ key: 'missing-time', text: '有 ' + missing + ' 笔退款缺少发生时刻，未计入本单', count: missing })
  }

  return list
})

/** 待确认归属条目（由后端 pendingRefunds 驱动） */
const pending = computed<PendingAttributionRefundVO[]>(() => props.attribution?.pendingRefunds ?? [])

/** 整单共享候选班次（后端 candidateShifts）；行级若也带则优先行级 */
const sharedCandidates = computed<ShiftCandidateVO[]>(() => props.attribution?.candidateShifts ?? [])

function candidatesOf(row: PendingAttributionRefundVO): ShiftCandidateVO[] {
  const rowList = row.candidateShifts ?? []
  return rowList.length > 0 ? rowList : sharedCandidates.value
}

/** 候选班次展示文案：优先后端 label（如 "morning #352"），缺失时按位置给 上一/本/下一班次；避免 #id 重复 */
function candidateLabel(cand: ShiftCandidateVO, idx: number): string {
  const fallback = idx === 0 ? '上一班次' : idx === 1 ? '本班次' : '下一班次'
  const base = (cand.label || '').trim() || fallback
  return base.includes('#' + cand.shiftId) ? base : base + ' #' + cand.shiftId
}

watch(pending, (list) => {
  // 默认选中「本班次」（中间那个候选）
  for (const item of list) {
    if (selectedShift.value[item.refundRecordId]) continue
    // 默认选中「本班次」= 候选中间那个（后端顺序：上一 / 本 / 下一）
    const candidates = candidatesOf(item)
    const fallback = candidates.length ? candidates[Math.min(1, candidates.length - 1)].shiftId : null
    if (fallback != null) selectedShift.value[item.refundRecordId] = fallback
  }
}, { immediate: true })

/**
 * 归属区渲染条件（N19-3b ⑤ 定案）：
 * 有 pending 退款 **或** 当前用户无权限（此时用占位骨架展示禁用态，便于验收/E2E）。
 * 无权限时列表页不请求 pending-refunds（避免 403 噪声），attribution 为空 → 走占位分支。
 */
const showAttribution = computed<boolean>(() => pending.value.length > 0 || !props.canConfirm)

function reasonValid(row: PendingAttributionRefundVO): boolean {
  return (reasons.value[row.refundRecordId] || '').trim().length >= 4
}

function canSubmit(row: PendingAttributionRefundVO): boolean {
  if (!props.canConfirm || props.confirming) return false
  if (!selectedShift.value[row.refundRecordId]) return false
  return reasonValid(row)
}

function submit(row: PendingAttributionRefundVO): void {
  localError.value = ''
  if (!props.canConfirm) {
    localError.value = '仅店长/财务可确认归属'
    return
  }
  if (!selectedShift.value[row.refundRecordId]) {
    localError.value = '请选择归属班次'
    return
  }
  if (!reasonValid(row)) {
    localError.value = '请填写确认原因（至少 4 个字）'
    return
  }
  emit('confirm', {
    refundRecordId: row.refundRecordId,
    shiftId: selectedShift.value[row.refundRecordId],
    reason: (reasons.value[row.refundRecordId] || '').trim(),
  })
}

defineExpose({ localError })
</script>

<template>
  <div v-if="slip" class="slip-detail">
    <!-- 提示条 -->
    <div v-if="hints.length" class="slip-hints">
      <span
        v-for="hint in hints"
        :key="hint.key"
        class="slip-hint"
        :class="'slip-hint--' + hint.key"
        :data-hint="hint.key"
        :data-count="hint.count"
      >{{ hint.text }}</span>
    </div>

    <header class="slip-detail__head">
      <div class="slip-detail__no">{{ slip.slipNo }}</div>
      <div class="slip-detail__meta">
        业务日期 {{ slip.businessDate || '-' }} · 门店 {{ slip.storeId ?? '-' }} · 班次 {{ slip.shiftId ?? '-' }}
        · 收银员 {{ slip.cashierName || '-' }} · 状态 {{ slip.status || '-' }}
      </div>
    </header>

    <!-- 金额区（含 N19-3b 两列） -->
    <section class="slip-amounts">
      <div class="slip-amount">
        <div class="slip-amount__label">应交（元）</div>
        <div class="slip-amount__value">{{ money(slip.expectedAmount) }}</div>
      </div>
      <div class="slip-amount">
        <div class="slip-amount__label">实交（元）</div>
        <div class="slip-amount__value">{{ money(slip.actualAmount) }}</div>
      </div>
      <div class="slip-amount">
        <div class="slip-amount__label">差额（元）</div>
        <div class="slip-amount__value">{{ money(slip.differenceAmount) }}</div>
      </div>
      <div class="slip-amount">
        <div class="slip-amount__label" title="本次存行中属于退款/冲回的金额">退款构成（元）</div>
        <div class="slip-amount__value" data-test="slip-rcf" :data-rcf-fen="slip.refundComponentFen ?? ''">
          {{ money(slip.refundComponentFen) }}
        </div>
      </div>
      <div class="slip-amount">
        <div
          class="slip-amount__label"
          title="净值 = actual − (expected − 退款构成)，即扣除退款后的真实长短款"
        >净值（元）</div>
        <div
          class="slip-amount__value"
          :class="dnetClass"
          data-test="slip-dnet"
          :data-dnet-fen="slip.differenceNetFen ?? ''"
          :data-dnet-state="dnetState"
        >{{ money(slip.differenceNetFen) }}</div>
      </div>
    </section>

    <!-- 待确认归属 -->
    <section v-if="showAttribution" class="slip-attribution" data-test="attribution-block">
      <header class="slip-attribution__head">
        <h4>待确认归属</h4>
        <span v-if="!canConfirm" class="slip-attribution__lock">无权限确认退款归属（需店长/财务）</span>
      </header>
      <div v-if="localError" class="slip-attribution__error">{{ localError }}</div>
      <table class="slip-attribution__table">
        <thead>
          <tr>
            <th>退款单号</th>
            <th class="num">金额(元)</th>
            <th>refund_time</th>
            <th>候选班次</th>
            <th>原因</th>
            <th style="width: 96px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in pending"
            :key="row.refundRecordId"
            data-test="attribution-row"
            :data-refund-id="row.refundRecordId"
          >
            <td class="mono">{{ row.refundNo || row.orderNumber || ('#' + row.refundRecordId) }}</td>
            <td class="num">{{ money(row.amountFen) }}</td>
            <td class="mono">{{ row.refundTime || '（空）' }}</td>
            <td>
              <el-radio-group
                v-model="selectedShift[row.refundRecordId]"
                :disabled="!canConfirm"
                size="small"
                data-test="attribution-shift"
              >
                <el-radio-button
                  v-for="(cand, idx) in candidatesOf(row)"
                  :key="cand.shiftId"
                  :value="cand.shiftId"
                  :data-shift-id="cand.shiftId"
                  :title="(cand.startTime || '') + ' ~ ' + (cand.endTime || '')"
                >{{ candidateLabel(cand, idx) }}</el-radio-button>
              </el-radio-group>
            </td>
            <td>
              <el-input
                v-model="reasons[row.refundRecordId]"
                placeholder="至少 4 个字"
                maxlength="100"
                :disabled="!canConfirm"
                data-test="attribution-reason"
              />
            </td>
            <td>
              <el-tooltip :disabled="canConfirm" content="仅店长/财务可确认归属" placement="top">
                <span>
                  <el-button
                    type="primary"
                    link
                    data-test="attribution-confirm"
                    :disabled="!canSubmit(row)"
                    :loading="confirming"
                    :title="canConfirm ? '' : '无权限'"
                    @click="submit(row)"
                  >确认归属</el-button>
                </span>
              </el-tooltip>
            </td>
          </tr>

          <!-- 无权限占位行：不发起 pending-refunds 请求也能展示禁用态 -->
          <tr v-if="!canConfirm && pending.length === 0" data-test="attribution-placeholder">
            <td colspan="5" class="slip-attribution__empty">
              无可确认的退款归属数据（当前账号无查看权限，已跳过取数）
            </td>
            <td>
              <el-button
                type="primary"
                link
                data-test="attribution-confirm"
                disabled
                title="无权限"
              >确认归属</el-button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.slip-hints {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.slip-hint {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  line-height: 20px;
  background: var(--fts-bg-subtle, #f4f4f5);
  color: var(--fts-text-secondary, #606266);
}

.slip-hint--still-open {
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary, #409eff);
}

.slip-hint--stale {
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
}

.slip-hint--legacy-null {
  background: var(--el-color-info-light-9, #f4f4f5);
  color: var(--el-color-info, #909399);
}

.slip-hint--missing-time {
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
}

.slip-detail__head {
  margin-bottom: 12px;
}

.slip-detail__no {
  font-size: 16px;
  font-weight: 600;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.slip-detail__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.slip-amounts {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.slip-amount {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--fts-bg-subtle, #fafafa);
}

.slip-amount__label {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 6px;
  cursor: help;
}

.slip-amount__value {
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary, #303133);
}

.text-danger {
  color: var(--el-color-danger, #f56c6c);
}

.text-success {
  color: var(--el-color-success, #67c23a);
}

.text-neutral {
  color: var(--fts-text-primary, #303133);
}

.slip-attribution {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--fts-border-secondary, #ebeef5);
}

.slip-attribution__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.slip-attribution__head h4 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}

.slip-attribution__lock {
  font-size: 12px;
  color: var(--el-color-warning-dark-2, #b88230);
}

.slip-attribution__empty {
  color: var(--fts-text-secondary, #909399);
  font-size: 12px;
}

.slip-attribution__error {
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 12px;
}

.slip-attribution__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.slip-attribution__table th,
.slip-attribution__table td {
  padding: 6px 8px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
  vertical-align: middle;
}

.slip-attribution__table th {
  color: var(--fts-text-secondary, #606266);
  background: var(--fts-bg-subtle, #fafafa);
  font-weight: 600;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.dim {
  color: var(--fts-text-secondary, #909399);
  font-size: 11px;
}

@media (max-width: 1200px) {
  .slip-amounts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
