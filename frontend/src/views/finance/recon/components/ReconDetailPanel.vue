<script setup lang="ts">
/**
 * 对账工作台 - 右侧详情面板（P3-1）
 *
 * 常驻展示：内部/外部 ref、两侧金额、差额、类型、resolution* 三个字段，
 * 以及**审计时间线**（由 claim/resolve 返回的 assignedAt / resolutionTime / resolutionBy / resolutionRemark 拼出）。
 * AC-47 依赖这里：认领后出现「认领于 …」，核销后出现「核销于 …，操作人 …，原因 …」。
 */
import { computed } from 'vue'
import {
  RECON_RESOLUTION_LABELS,
  RECON_STATUS_LABELS,
  RECON_TYPE_LABELS,
  formatFen,
} from '@/types/finance-recon'
import type { ReconDifferenceVO, ReconResolution } from '@/types/finance-recon'

interface Props {
  difference: ReconDifferenceVO | null
  claiming?: boolean
  resolving?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  claiming: false,
  resolving: false,
})

const emit = defineEmits<{
  (e: 'claim'): void
  (e: 'resolve'): void
}>()

const CLOSED_STATUSES = ['CLOSED', 'EXEMPT', 'PENDING_P2_REVERSE']

const statusText = computed<string>(() => {
  const status = String(props.difference?.status || '')
  return RECON_STATUS_LABELS[status] || status || '-'
})

const typeText = computed<string>(() => {
  const type = String(props.difference?.differenceType || '')
  return RECON_TYPE_LABELS[type] || type || '-'
})

const resolutionText = computed<string>(() => {
  const resolution = props.difference?.resolution as ReconResolution | null | undefined
  if (!resolution) return '-'
  return RECON_RESOLUTION_LABELS[resolution] || String(resolution)
})

/** 认领按钮：仅「待认领(OPEN)」可用 */
const canClaim = computed<boolean>(() => {
  const diff = props.difference
  if (!diff) return false
  return String(diff.status) === 'OPEN'
})

/** 核销按钮：已进入处理流程且尚未终结时可点 */
const canResolve = computed<boolean>(() => {
  const diff = props.difference
  if (!diff) return false
  if (CLOSED_STATUSES.includes(String(diff.status))) return false
  return true
})

/**
 * 审计时间线的结构化片段（AC-47）。
 *
 * 文案保持中文原样（「认领于 …」「核销于 …，操作人 …，原因 …」），
 * 同时给每个字段挂 `data-field`（assignedAt / resolutionTime / resolutionBy / resolutionRemark），
 * 便于 E2E 既读中文又能精确定位字段。
 */
interface AuditSegment {
  field: string
  text: string
}

interface AuditLine {
  field: string
  segments: AuditSegment[]
}

const auditLines = computed<AuditLine[]>(() => {
  const diff = props.difference
  if (!diff) return []
  const lines: AuditLine[] = []

  const claimedAt = diff.assignedAt
  if (claimedAt || diff.assigneeId != null) {
    lines.push({
      field: 'assignedAt',
      segments: [
        { field: 'assignedAt', text: `认领于 ${claimedAt || '（未记录时间）'}` },
        ...(diff.assigneeId != null
          ? [{ field: 'assigneeId', text: `，认领人 ${diff.assigneeId}` }]
          : []),
      ],
    })
  }

  const resolvedAt = diff.resolutionTime
  if (resolvedAt || diff.resolutionBy != null || diff.resolutionRemark) {
    const segments: AuditSegment[] = [
      { field: 'resolutionTime', text: `核销于 ${resolvedAt || '（未记录时间）'}` },
      { field: 'resolutionBy', text: `，操作人 ${diff.resolutionBy != null ? diff.resolutionBy : '（未记录）'}` },
      { field: 'resolutionRemark', text: `，原因 ${diff.resolutionRemark ? diff.resolutionRemark : '（空）'}` },
    ]
    if (diff.resolution) {
      segments.push({
        field: 'resolution',
        text: `，决议 ${RECON_RESOLUTION_LABELS[diff.resolution as ReconResolution] || diff.resolution}`,
      })
    }
    if (diff.reverseFlowNo) {
      segments.push({ field: 'reverseFlowNo', text: `，反向流水号 ${diff.reverseFlowNo}` })
    }
    lines.push({ field: 'resolutionTime', segments })
  }

  return lines
})
</script>

<template>
  <aside class="recon-detail" data-test="detail-panel">
    <header class="recon-detail__header">
      <h3 class="recon-detail__title">差异详情</h3>
      <span v-if="difference" class="recon-detail__id">#{{ difference.differenceId }}</span>
    </header>

    <div v-if="!difference" class="recon-detail__empty">点击左侧队列中的一行查看详情</div>

    <template v-else>
      <dl class="recon-detail__grid">
        <div class="row"><dt>批次</dt><dd class="mono">#{{ difference.runId }}</dd></div>
        <div class="row"><dt>类型</dt><dd>{{ typeText }}</dd></div>
        <div class="row"><dt>状态</dt><dd>{{ statusText }}</dd></div>
        <div class="row"><dt>内部凭证</dt><dd class="mono" data-test="detail-internal-ref">{{ difference.internalRef || '-' }}</dd></div>
        <div class="row"><dt>外部凭证</dt><dd class="mono" data-test="detail-external-ref">{{ difference.externalRef || '-' }}</dd></div>
        <div class="row"><dt>内部金额(元)</dt><dd class="num">{{ formatFen(difference.amountInternal) }}</dd></div>
        <div class="row"><dt>外部金额(元)</dt><dd class="num">{{ formatFen(difference.amountExternal) }}</dd></div>
        <div class="row">
          <dt>差额(元)</dt>
          <dd class="num strong" :data-diff-amount-fen="difference.diffAmount ?? 0">
            {{ formatFen(difference.diffAmount) }}
          </dd>
        </div>
        <div class="row"><dt>决议</dt><dd data-test="detail-resolution">{{ resolutionText }}</dd></div>
        <div class="row"><dt>决议备注</dt><dd data-test="detail-resolution-remark">{{ difference.resolutionRemark || '-' }}</dd></div>
        <div class="row"><dt>反向流水号</dt><dd class="mono">{{ difference.reverseFlowNo || '-' }}</dd></div>
        <div class="row"><dt>创建时间</dt><dd class="mono">{{ difference.createTime || '-' }}</dd></div>
      </dl>

      <!-- 审计时间线（AC-47） -->
      <section class="recon-detail__timeline" data-test="audit-timeline">
        <h4 class="recon-detail__subtitle">审计时间线</h4>
        <ul v-if="auditLines.length" class="timeline">
          <li
            v-for="(line, idx) in auditLines"
            :key="idx"
            class="timeline__item"
            :data-field="line.field"
          >
            <span
              v-for="(seg, segIdx) in line.segments"
              :key="segIdx"
              :data-field="seg.field"
            >{{ seg.text }}</span>
          </li>
        </ul>
        <div v-else class="timeline__empty">尚无认领/核销记录</div>
      </section>

      <footer class="recon-detail__actions">
        <el-button
          type="primary"
          data-test="btn-claim"
          :disabled="!canClaim"
          :loading="claiming"
          @click="emit('claim')"
        >认领</el-button>
        <el-button
          type="success"
          data-test="btn-resolve"
          :disabled="!canResolve"
          :loading="resolving"
          @click="emit('resolve')"
        >核销</el-button>
      </footer>
    </template>
  </aside>
</template>

<style scoped>
.recon-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 320px;
  max-width: 420px;
  padding: 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  overflow: auto;
}

.recon-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recon-detail__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.recon-detail__id {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.recon-detail__empty {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--fts-text-secondary, #909399);
}

.recon-detail__grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
}

.recon-detail__grid .row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 20px;
}

.recon-detail__grid dt {
  flex: 0 0 92px;
  color: var(--fts-text-secondary, #909399);
}

.recon-detail__grid dd {
  flex: 1 1 auto;
  margin: 0;
  min-width: 0;
  word-break: break-all;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.strong {
  font-weight: 600;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.recon-detail__timeline {
  border-top: 1px dashed var(--fts-border-secondary, #ebeef5);
  padding-top: 10px;
}

.recon-detail__subtitle {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--fts-text-secondary, #606266);
}

.timeline {
  margin: 0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  line-height: 18px;
  color: var(--fts-text-primary, #303133);
}

.timeline__empty {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.recon-detail__actions {
  display: flex;
  gap: 10px;
  padding-top: 4px;
}
</style>
