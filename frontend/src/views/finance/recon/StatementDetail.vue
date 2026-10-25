<script setup lang="ts">
/**
 * 调节表详情（P3-3，S5）
 *
 * 关键约束：
 * - 四类下钻卡的**金额取接口原值**（unclearedInCredit/InDebit/OutCredit/OutDebit），
 *   条数取 items 的分组长度（这是"计数"，不是把金额前端求和）；
 * - 四眼门禁：提交人 = 当前用户 → 审核按钮禁用 + 提示；出纳 → 禁用 + 提示；
 *   **后端 409/错误仍是最终依据**，失败时把后端文案原样显示出来（不吞错）；
 * - 导出走 export 接口（带 token 的 Blob 下载）；打印打开独立打印视图。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import {
  approveStatement,
  exportStatementCsv,
  getStatement,
  resolveStatementAmbiguity,
  submitStatement,
  type ReconciliationItemVO,
  type ReconciliationStatementVO,
} from '@/api/finance-recon-statement'
import ResolveAmbiguityDialog from './components/ResolveAmbiguityDialog.vue'
import { usePermissionStore } from '@/stores/permission'
import { formatFen } from '@/types/finance-recon'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const statementId = computed<number>(() => Number(route.params.id))
const statement = ref<ReconciliationStatementVO | null>(null)
const loading = ref(false)
const errorText = ref('')
/** 动作错误（含后端 409 的四眼/出纳/V5 文案），必须显示出来，不能吞 */
const actionError = ref('')
const submitting = ref(false)
const approving = ref(false)
const exporting = ref(false)
/** 已展开的类别 */
const expanded = ref<string[]>([])

/** 当前登录用户ID（用于四眼判断） */
const currentUserId = computed<string>(() => {
  const id = permissionStore.userInfo?.userId
  return id === undefined || id === null ? '' : String(id)
})

/** 是否出纳（前端枚举暂无 TELLER，这里按角色原值模糊匹配，后端 ROLE_TELLER 才是最终依据） */
const isTeller = computed<boolean>(() => {
  const roles = permissionStore.userInfo?.roles ?? []
  return roles.some((role) => String(role).toLowerCase().includes('teller'))
})

/** 四类未达账项（金额取接口原值；label 与后端 categoryName 一致） */
interface CategoryMeta {
  key: 'A' | 'B' | 'C' | 'D'
  label: string
  /** statement 上对应的金额字段 */
  amountField: keyof ReconciliationStatementVO
}

const CATEGORIES: CategoryMeta[] = [
  { key: 'A', label: '企业已收、银行未收', amountField: 'unclearedInCredit' },
  { key: 'B', label: '企业已付、银行未付', amountField: 'unclearedInDebit' },
  { key: 'C', label: '银行已收、企业未收', amountField: 'unclearedOutCredit' },
  { key: 'D', label: '银行已付、企业未付', amountField: 'unclearedOutDebit' },
]

function itemsOf(key: string): ReconciliationItemVO[] {
  const items = statement.value?.items ?? []
  return items.filter((item) => String(item.category).toUpperCase() === key)
}

function amountOf(meta: CategoryMeta): number {
  const raw = statement.value?.[meta.amountField]
  return typeof raw === 'number' ? raw : 0
}

/** 条数 = 明细条数（计数），金额 = 接口原值 */
function countOf(meta: CategoryMeta): number {
  return itemsOf(meta.key).length
}

function toggle(key: string): void {
  const idx = expanded.value.indexOf(key)
  if (idx >= 0) expanded.value.splice(idx, 1)
  else expanded.value.push(key)
}

function isExpanded(key: string): boolean {
  return expanded.value.includes(key)
}

/** 引用侧：优先 flowId，其次 transactionId */
function refSide(item: ReconciliationItemVO): string {
  if (item.flowId != null) return 'flow#' + item.flowId
  if (item.transactionId != null) return 'txn#' + item.transactionId
  return '-'
}

const statusText = computed<string>(() => {
  const status = statement.value?.status || ''
  const map: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审核' }
  return map[status] || status || '-'
})

/** 四眼：提交人 == 当前用户 */
const fourEyesBlocked = computed<boolean>(() => {
  const submittedBy = statement.value?.submittedBy
  if (submittedBy === null || submittedBy === undefined) return false
  if (!currentUserId.value) return false
  return String(submittedBy) === currentUserId.value
})

/** 审核禁用原因（空串 = 可点） */
const approveBlockReason = computed<string>(() => {
  const stmt = statement.value
  if (!stmt) return ''
  if (fourEyesBlocked.value) return '提交人与审核人不得为同一人'
  if (isTeller.value) return '出纳不得审核调节表'
  if (String(stmt.status) !== 'SUBMITTED') return '仅「已提交」状态可审核'
  if (stmt.isBalanced === false) return '调节后两侧不相等，不可审核'
  if ((stmt.ambiguousItemCount ?? 0) > 0) return '存在未解决的同额歧义，不可审核'
  return ''
})
const approveDisabled = computed<boolean>(() => approveBlockReason.value !== '')

const submitDisabled = computed<boolean>(() => {
  const stmt = statement.value
  if (!stmt) return true
  if (String(stmt.status) !== 'DRAFT') return true
  if (stmt.isBalanced === false) return true
  return (stmt.ambiguousItemCount ?? 0) > 0
})

async function load(): Promise<void> {
  if (!Number.isFinite(statementId.value) || statementId.value <= 0) {
    errorText.value = '调节表ID无效'
    return
  }
  loading.value = true
  errorText.value = ''
  try {
    statement.value = await getStatement(statementId.value)
    expanded.value = ['A']
  } catch (error) {
    statement.value = null
    errorText.value = error instanceof Error ? error.message : '调节表详情加载失败'
  } finally {
    loading.value = false
  }
}

async function doSubmit(): Promise<void> {
  if (!statement.value) return
  actionError.value = ''
  submitting.value = true
  try {
    statement.value = await submitStatement(statement.value.statementId)
    ElMessage.success('提交成功')
  } catch (error) {
    // 后端门禁文案（V5 两侧不等 / 同额歧义 …）原样展示
    actionError.value = error instanceof Error ? error.message : '提交失败'
  } finally {
    submitting.value = false
  }
}

async function doApprove(): Promise<void> {
  if (!statement.value) return
  actionError.value = ''
  approving.value = true
  try {
    statement.value = await approveStatement(statement.value.statementId)
    ElMessage.success('审核通过')
  } catch (error) {
    // 409（四眼/出纳/V5 复核失败）→ 文案原样显示，绝不吞掉
    actionError.value = error instanceof Error ? error.message : '审核失败'
  } finally {
    approving.value = false
  }
}

async function doExport(): Promise<void> {
  if (!statement.value) return
  exporting.value = true
  actionError.value = ''
  try {
    const blob = await exportStatementCsv(statement.value.statementId)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `statement-${statement.value.statementId}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : '导出失败'
  } finally {
    exporting.value = false
  }
}

function doPrint(): void {
  if (!statement.value) return
  const target = router.resolve({
    name: 'FinanceStatementPrint',
    params: { id: String(statement.value.statementId) },
  })
  window.open(target.href, '_blank')
}

/* ===== P4-2 同额歧义处置（逐笔，无批量） ===== */
const ambiguityVisible = ref(false)
const ambiguityItem = ref<ReconciliationItemVO | null>(null)
const ambiguitySubmitting = ref(false)

/** 打开处置弹窗（只针对当前行的这一条 item） */
function openAmbiguity(item: ReconciliationItemVO): void {
  ambiguityItem.value = item
  ambiguityVisible.value = true
}

/**
 * 提交处置：POST resolve-ambiguity。
 * 成功后**用响应里的新值**整体替换 statement（isBalanced / ambiguousItemCount / items 均由后端重算），
 * 四类卡的 data-count / data-amount-fen 与 stmt-balanced 自动刷新。
 * 失败（400/403/409）把后端 message 原样写进 data-test="action-error"，不吞错。
 */
async function submitAmbiguity(payload: { decision: string; targetRef: string; reason: string }): Promise<void> {
  const stmt = statement.value
  const item = ambiguityItem.value
  if (!stmt || !item) return
  if (payload.reason.trim().length < 5) {
    actionError.value = '处置原因至少 5 个字'
    return
  }
  actionError.value = ''
  ambiguitySubmitting.value = true
  try {
    // EXEMPT（排除）：请求体只发 { decision, reason }，不带 targetRef（P4-3）
    const updated = await resolveStatementAmbiguity(stmt.statementId, item.itemId, {
      decision: payload.decision,
      ...(payload.decision === 'EXEMPT' || !payload.targetRef ? {} : { targetRef: payload.targetRef }),
      reason: payload.reason.trim(),
    })
    statement.value = updated
    ambiguityVisible.value = false
    ambiguityItem.value = null
    ElMessage.success('歧义已处置')
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : '歧义处置失败'
  } finally {
    ambiguitySubmitting.value = false
  }
}

/**
 * 审计时间线（与 P3-1 同一渲染约定：li + span[data-field]）。
 * 详情接口未单独返回审计列表，这里用：
 * ① statement 的 submitted/approved 字段；
 * ② 处置后的 item.reason 前缀「已处置(AMBIGUITY->X:ref)：…」。
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
  const stmt = statement.value
  if (!stmt) return []
  const lines: AuditLine[] = []
  if (stmt.submittedAt || stmt.submittedBy != null) {
    lines.push({
      field: 'submittedAt',
      segments: [
        { field: 'submittedAt', text: '提交于 ' + (stmt.submittedAt || '（未记录时间）') },
        { field: 'submittedBy', text: '，提交人 ' + (stmt.submittedBy != null ? '#' + stmt.submittedBy : '（未记录）') },
      ],
    })
  }
  if (stmt.approvedAt || stmt.approvedBy != null) {
    lines.push({
      field: 'approvedAt',
      segments: [
        { field: 'approvedAt', text: '审核于 ' + (stmt.approvedAt || '（未记录时间）') },
        { field: 'approvedBy', text: '，审核人 ' + (stmt.approvedBy != null ? '#' + stmt.approvedBy : '（未记录）') },
      ],
    })
  }
  for (const item of stmt.items ?? []) {
    const reason = item.reason || ''
    const category = String(item.category || '').toUpperCase()
    // P4-3：排除（EXEMPT）与改类别（AMBIGUITY->X）分开渲染
    const isExempted = /EXEMPT/i.test(reason) || reason.startsWith('已排除')
    const isResolved = reason.startsWith('已处置(AMBIGUITY->') && !isExempted
    if (!isExempted && !isResolved) continue
    const head = reason.split('；')[0]
    lines.push({
      field: isExempted ? 'exempted' : 'ambiguityResolved',
      segments: [
        {
          field: isExempted ? 'exempted' : 'ambiguityResolved',
          text: 'item #' + item.itemId + '（' + category + ' 类）' + head,
        },
        { field: 'resolutionRemark', text: '；' + (reason.split('；').slice(1).join('；') || '') },
      ],
    })
  }
  return lines
})

onMounted(load)
</script>

<template>
  <div class="modern-page statement-detail">
    <PageHeader
      :title="statement ? '调节表 ' + statement.statementNo : '调节表详情'"
      :description="statement ? statement.periodStart + ' ~ ' + statement.periodEnd : '四类未达账项与四眼门禁'"
    >
      <el-button data-test="btn-submit" :disabled="submitDisabled" :loading="submitting" @click="doSubmit">提交</el-button>
      <el-button
        type="primary"
        data-test="btn-approve"
        :disabled="approveDisabled"
        :loading="approving"
        @click="doApprove"
      >审核</el-button>
      <el-button data-test="btn-export" :loading="exporting" @click="doExport">导出CSV</el-button>
      <el-button data-test="btn-print" @click="doPrint">打印</el-button>
    </PageHeader>

    <!-- 四眼/出纳提示：禁用时展示，文案与后端门禁语义一致 -->
    <div v-if="approveBlockReason" class="statement-detail__hint" data-test="four-eyes-hint">
      {{ approveBlockReason }}
    </div>

    <div v-if="errorText" class="statement-detail__error">{{ errorText }}</div>
    <div v-if="actionError" class="statement-detail__error" data-test="action-error">{{ actionError }}</div>

    <template v-if="statement">
      <!-- 余额与调节后余额（全部接口原值） -->
      <section class="statement-detail__balances" v-loading="loading">
        <div class="balance-card">
          <div class="balance-card__label">企业账面余额（元）</div>
          <div class="balance-card__value">{{ formatFen(statement.bookBalance) }}</div>
        </div>
        <div class="balance-card">
          <div class="balance-card__label">对账单余额（元）</div>
          <div class="balance-card__value">{{ formatFen(statement.bankBalance) }}</div>
        </div>
        <div class="balance-card">
          <div class="balance-card__label">调节后（企业侧，元）</div>
          <div class="balance-card__value" data-test="stmt-adj-book-fen" :data-fen="statement.adjustedBookBalance ?? 0">
            {{ formatFen(statement.adjustedBookBalance) }}
          </div>
        </div>
        <div class="balance-card">
          <div class="balance-card__label">调节后（银行侧，元）</div>
          <div class="balance-card__value" data-test="stmt-adj-bank-fen" :data-fen="statement.adjustedBankBalance ?? 0">
            {{ formatFen(statement.adjustedBankBalance) }}
          </div>
        </div>
        <div class="balance-card">
          <div class="balance-card__label">两侧平衡</div>
          <div
            class="balance-card__value"
            :class="statement.isBalanced ? 'is-ok' : 'is-bad'"
            data-test="stmt-balanced"
            :data-balanced="statement.isBalanced ? 'true' : 'false'"
          >{{ statement.isBalanced ? '已平' : '未平' }}</div>
        </div>
        <div class="balance-card">
          <div class="balance-card__label">状态 / 同额歧义</div>
          <div class="balance-card__value balance-card__value--sm">
            {{ statusText }} · 歧义 {{ statement.ambiguousItemCount ?? 0 }}
          </div>
        </div>
      </section>

      <!-- 四类下钻卡 -->
      <section class="statement-detail__cats">
        <div v-for="meta in CATEGORIES" :key="meta.key" class="cat-block">
          <div
            class="cat-card"
            :class="{ 'is-open': isExpanded(meta.key) }"
            :data-test="'cat-' + meta.key"
            :data-count="countOf(meta)"
            :data-amount-fen="amountOf(meta)"
            role="button"
            tabindex="0"
            :aria-expanded="isExpanded(meta.key)"
            @click="toggle(meta.key)"
            @keydown.enter.prevent="toggle(meta.key)"
            @keydown.space.prevent="toggle(meta.key)"
          >
            <div class="cat-card__head">
              <span class="cat-card__key">{{ meta.key }}</span>
              <span class="cat-card__label">{{ meta.label }}</span>
            </div>
            <div class="cat-card__metric">
              <span class="cat-card__count">{{ countOf(meta) }} 条</span>
              <span class="cat-card__amount">{{ formatFen(amountOf(meta)) }} 元</span>
            </div>
            <div class="cat-card__foot">{{ isExpanded(meta.key) ? '收起明细' : '展开明细' }}</div>
          </div>

          <div v-if="isExpanded(meta.key)" class="cat-items" :data-test="'cat-items-' + meta.key">
            <table class="cat-items__table">
              <thead>
                <tr>
                  <th>挂账人</th>
                  <th>原因</th>
                  <th>期间</th>
                  <th class="num">金额(元)</th>
                  <th>引用侧</th>
                  <th style="width: 92px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in itemsOf(meta.key)" :key="item.itemId" :data-item-id="item.itemId">
                  <td>{{ item.ownerName || (item.ownerUserId != null ? '#' + item.ownerUserId : '-') }}</td>
                  <td class="reason" :title="item.reason || ''">
                    {{ item.reason || '-' }}
                    <span v-if="item.ambiguous" class="tag-ambiguous">同额歧义</span>
                  </td>
                  <td class="mono nowrap">{{ item.periodStart || '-' }} ~ {{ item.periodEnd || '-' }}</td>
                  <td class="num" :data-amount-fen="item.amount ?? 0">{{ formatFen(item.amount) }}</td>
                  <td class="mono">{{ refSide(item) }}</td>
                  <td>
                    <el-button
                      v-if="item.ambiguous"
                      link
                      type="warning"
                      data-test="btn-resolve-ambiguity"
                      :data-item-id="item.itemId"
                      @click="openAmbiguity(item)"
                    >处置</el-button>
                    <span v-else class="dim">—</span>
                  </td>
                </tr>
                <tr v-if="itemsOf(meta.key).length === 0">
                  <td colspan="6" class="cat-items__empty">该类无未达账项</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 审计时间线（处置记录在此出现） -->
      <section class="statement-detail__timeline" data-test="audit-timeline">
        <h4 class="statement-detail__subtitle">审计时间线</h4>
        <ul v-if="auditLines.length" class="timeline">
          <li v-for="(line, idx) in auditLines" :key="idx" class="timeline__item" :data-field="line.field">
            <span v-for="(seg, segIdx) in line.segments" :key="segIdx" :data-field="seg.field">{{ seg.text }}</span>
          </li>
        </ul>
        <div v-else class="timeline__empty">暂无提交/审核/处置记录</div>
      </section>
    </template>

    <!-- 同额歧义处置（逐笔） -->
    <ResolveAmbiguityDialog
      v-model="ambiguityVisible"
      :statement="statement"
      :item="ambiguityItem"
      :submitting="ambiguitySubmitting"
      @submit="submitAmbiguity"
    />
  </div>
</template>

<style scoped>
.statement-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.statement-detail__hint {
  padding: 8px 14px;
  border-radius: 10px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
  border: 1px solid var(--el-color-warning-light-5, #f5dab1);
  font-size: 13px;
}

.statement-detail__error {
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
  white-space: pre-wrap;
}

.statement-detail__balances {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.balance-card {
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.balance-card__label {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 6px;
}

.balance-card__value {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary, #303133);
}

.balance-card__value--sm {
  font-size: 14px;
  font-weight: 500;
}

.balance-card__value.is-ok {
  color: var(--el-color-success, #67c23a);
}

.balance-card__value.is-bad {
  color: var(--el-color-danger, #f56c6c);
}

.statement-detail__cats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.cat-card {
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.cat-card:focus-visible {
  outline: 2px solid var(--el-color-primary, #409eff);
  outline-offset: 2px;
}

.cat-card:hover {
  border-color: var(--el-color-primary-light-5, #a0cfff);
}

.cat-card.is-open {
  border-color: var(--el-color-primary, #409eff);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.cat-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cat-card__key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary, #409eff);
  font-weight: 700;
  font-size: 12px;
}

.cat-card__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-primary, #303133);
}

.cat-card__metric {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 10px;
}

.cat-card__count {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.cat-card__amount {
  font-size: 13px;
  color: var(--fts-text-secondary, #606266);
  font-variant-numeric: tabular-nums;
}

.cat-card__foot {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-color-primary, #409eff);
}

.cat-items {
  margin-top: 8px;
  padding: 8px 10px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  overflow: auto;
}

.cat-items__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.cat-items__table th,
.cat-items__table td {
  padding: 6px 8px;
  border-bottom: 1px solid var(--fts-border-secondary, #f5f7fa);
  text-align: left;
  vertical-align: top;
}

.cat-items__table th {
  color: var(--fts-text-secondary, #606266);
  background: var(--fts-bg-subtle, #fafafa);
  white-space: nowrap;
}

.reason {
  max-width: 320px;
  white-space: normal;
  word-break: break-all;
}

.tag-ambiguous {
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 8px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
  font-size: 11px;
}

.cat-items__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 14px 0;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.nowrap {
  white-space: nowrap;
}

.dim {
  color: var(--fts-text-secondary, #909399);
}

.statement-detail__timeline {
  margin-top: 4px;
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.statement-detail__subtitle {
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

@media (max-width: 1400px) {
  .statement-detail__balances {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .statement-detail__cats {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
