<script setup lang="ts">
/**
 * 期末结账（P4-4 前端）
 *
 * 数据源（fund-500 并行实现，契约见 api/finance-period.ts）：
 *   GET  /v1/finance/periods              期间列表
 *   GET  /v1/finance/periods/{code}       逐账户明细（拿不到时回退列表行内联 accounts）
 *   POST /v1/finance/periods/{code}/close 结账
 *   POST /v1/finance/periods/{code}/reopen 反结账（reason）
 *
 * 约束：
 * - 金额一律「分」，展示用 formatFen，data-*-fen 属性写原值；
 * - 不提供批量结账/批量反结账；
 * - 失败时把后端 message **原样**写到 data-test="period-error"（含 OPEN 差异条数 / 账户差额等原因）。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import {
  closePeriod,
  getPeriodDetail,
  listPeriods,
  reopenPeriod,
  type PeriodAccountVO,
  type PeriodStatus,
  type PeriodVO,
} from '@/api/finance-period'
import { formatFen } from '@/types/finance-recon'

const periods = ref<PeriodVO[]>([])
const loading = ref(false)
/** 动作错误（后端 message 原文） */
const periodError = ref('')

const expandedCode = ref<string>('')
const detailLoading = ref(false)
const detailAccounts = ref<Record<string, PeriodAccountVO[]>>({})

const closingCode = ref('')

const reopenVisible = ref(false)
const reopenCode = ref('')
const reopenReason = ref('')
const reopening = ref(false)

const reopenReasonValid = computed<boolean>(() => reopenReason.value.trim().length >= 10)

function money(fen: number | null | undefined): string {
  return typeof fen === 'number' && Number.isFinite(fen) ? formatFen(fen) : '—'
}

/** 账户明细：优先用明细接口，拿不到则用列表行内联 accounts */
function accountsOf(period: PeriodVO): PeriodAccountVO[] {
  return detailAccounts.value[period.periodCode] ?? period.accounts ?? []
}

function isBadAccount(row: PeriodAccountVO): boolean {
  return row.trialBalancePassed === false || row.dirty === true
}

/** 三态徽标（P4-4 补：OPEN / CLOSED / REOPENED，不再只看 isClosed） */
const STATUS_LABELS: Record<PeriodStatus, string> = {
  OPEN: '未结账',
  CLOSED: '已结账',
  REOPENED: '已反结账',
}

function statusLabel(period: PeriodVO): string {
  return STATUS_LABELS[period.status] || period.status
}

function statusClass(period: PeriodVO): string {
  if (period.status === 'CLOSED') return 'pill--closed'
  if (period.status === 'REOPENED') return 'pill--reopened'
  return 'pill--open'
}

/** REOPENED 徽标 tooltip */
function reopenedTip(period: PeriodVO): string {
  return '曾于 ' + (period.closedAt || '-') + ' 结账，' + (period.reopenAt || '-')
    + ' 反结账：' + (period.reopenReason || '-')
}

/** 账户行校验失败原因 tooltip */
function accountIssue(row: PeriodAccountVO): string {
  if (row.dirty === true && row.trialBalancePassed === false) return '试算不平衡，且结账后仍有写入'
  if (row.dirty === true) return '结账后仍有写入'
  if (row.trialBalancePassed === false) return '试算不平衡（期初 + 收入 − 支出 ≠ 期末）'
  return ''
}

async function load(): Promise<void> {
  loading.value = true
  periodError.value = ''
  try {
    periods.value = await listPeriods()
  } catch (error) {
    periods.value = []
    periodError.value = error instanceof Error ? error.message : '期间列表加载失败'
  } finally {
    loading.value = false
  }
}

/** 行展开/收起：展开时拉逐账户明细 */
async function toggleRow(period: PeriodVO): Promise<void> {
  if (expandedCode.value === period.periodCode) {
    expandedCode.value = ''
    return
  }
  expandedCode.value = period.periodCode
  if (detailAccounts.value[period.periodCode]) return
  detailLoading.value = true
  try {
    const detail = await getPeriodDetail(period.periodCode)
    detailAccounts.value = { ...detailAccounts.value, [period.periodCode]: detail.accounts }
  } catch (error) {
    // 明细接口未就绪/失败：回退到列表内联 accounts（不打扰用户）
    console.warn('[PeriodClose] 明细接口不可用，回退列表内联 accounts', error)
    detailAccounts.value = { ...detailAccounts.value, [period.periodCode]: period.accounts ?? [] }
  } finally {
    detailLoading.value = false
  }
}

/** 结账（二次确认 → POST） */
async function handleClose(period: PeriodVO): Promise<void> {
  periodError.value = ''
  const baseText = '确认对期间 ' + period.periodCode + ' 结账？结账后该期间所有写入将被拦截'
  // REOPENED：曾被反结账的期间再次结账，追加一句明确提示反结账原因
  const extraText = period.status === 'REOPENED'
    ? '该期间曾被反结账（' + (period.reopenReason || '-') + '），确认再次结账？'
    : ''
  try {
    await ElMessageBox.confirm(
      extraText ? baseText + ' ' + extraText : baseText,
      '确认结账',
      { confirmButtonText: '确认结账', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  closingCode.value = period.periodCode
  try {
    await closePeriod(period.periodCode)
    ElMessage.success('期间 ' + period.periodCode + ' 已结账')
    await load()
    if (expandedCode.value === period.periodCode) {
      delete detailAccounts.value[period.periodCode]
    }
  } catch (error) {
    // 后端 message 原样展示（含 OPEN 差异条数 / 账户差额等具体原因）
    periodError.value = error instanceof Error ? error.message : '结账失败'
  } finally {
    closingCode.value = ''
  }
}

/** 打开反结账弹窗 */
function openReopen(period: PeriodVO): void {
  periodError.value = ''
  reopenCode.value = period.periodCode
  reopenReason.value = ''
  reopenVisible.value = true
}

/** 反结账（reason ≥10 字 + 二次确认 → POST） */
async function submitReopen(): Promise<void> {
  if (!reopenReasonValid.value) {
    ElMessage.warning('反结账原因至少 10 个字')
    return
  }
  try {
    await ElMessageBox.confirm(
      '反结账会解除写入拦截并写审计，确认？',
      '确认反结账',
      { confirmButtonText: '确认反结账', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  reopening.value = true
  periodError.value = ''
  try {
    await reopenPeriod(reopenCode.value, reopenReason.value.trim())
    ElMessage.success('期间 ' + reopenCode.value + ' 已反结账')
    reopenVisible.value = false
    await load()
    delete detailAccounts.value[reopenCode.value]
  } catch (error) {
    periodError.value = error instanceof Error ? error.message : '反结账失败'
  } finally {
    reopening.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="modern-page period-close">
    <PageHeader title="期末结账" description="会计期间结账 / 反结账 · 逐账户期初与期末合计校验">
      <el-button :loading="loading" @click="load">刷新</el-button>
    </PageHeader>

    <div v-if="periodError" class="period-close__error" data-test="period-error">{{ periodError }}</div>

    <section class="period-close__table-wrap" v-loading="loading">
      <table class="period-table">
        <thead>
          <tr>
            <th style="width: 120px">期间</th>
            <th style="width: 200px">起止</th>
            <th style="width: 100px">状态</th>
            <th style="width: 120px">结账人</th>
            <th style="width: 170px">结账时间</th>
            <th class="num" style="width: 90px">账户数</th>
            <th class="num" style="width: 130px">期初合计(元)</th>
            <th class="num" style="width: 130px">期末合计(元)</th>
            <th style="width: 210px">操作</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="period in periods" :key="period.periodCode">
            <tr
              data-test="period-row"
              :data-period-code="period.periodCode"
              :data-closed="period.closed ? 'true' : 'false'"
              class="period-row"
              :class="{ 'is-closed': period.closed }"
              @click="toggleRow(period)"
            >
              <td class="mono">{{ period.periodCode }}</td>
              <td class="mono">{{ period.startDate || '-' }} ~ {{ period.endDate || '-' }}</td>
              <td>
                <el-tooltip v-if="period.status === 'REOPENED'" :content="reopenedTip(period)" placement="top">
                  <span class="pill" :class="statusClass(period)" data-test="period-status" :data-status="period.status">
                    {{ statusLabel(period) }}
                  </span>
                </el-tooltip>
                <span
                  v-else
                  class="pill"
                  :class="statusClass(period)"
                  data-test="period-status"
                  :data-status="period.status"
                >{{ statusLabel(period) }}</span>
              </td>
              <td>{{ period.closedBy != null ? '#' + period.closedBy : '-' }}</td>
              <td class="mono">{{ period.closedAt || '-' }}</td>
              <td class="num">{{ period.accountCount ?? '-' }}</td>
              <td class="num">{{ money(period.openingTotalFen) }}</td>
              <td class="num" data-test="period-closing-fen" :data-fen="period.closingTotalFen ?? ''">
                {{ money(period.closingTotalFen) }}
              </td>
              <td>
                <!-- CLOSED → 反结账；OPEN / REOPENED → 结账 -->
                <el-button
                  v-if="period.status === 'CLOSED'"
                  type="warning"
                  link
                  data-test="btn-reopen-period"
                  @click.stop="openReopen(period)"
                >反结账</el-button>
                <el-button
                  v-else
                  type="primary"
                  link
                  data-test="btn-close-period"
                  :loading="closingCode === period.periodCode"
                  @click.stop="handleClose(period)"
                >结账</el-button>
              </td>
            </tr>

            <!-- 逐账户明细 -->
            <tr v-if="expandedCode === period.periodCode" class="detail-row">
              <td colspan="9">
                <div class="detail" v-loading="detailLoading">
                  <div class="detail__hint">逐账户明细（金额单位：元；判定值为后端原值）</div>
                  <table class="detail-table">
                    <thead>
                      <tr>
                        <th style="width: 90px">账户</th>
                        <th>账户名</th>
                        <th class="num">期初</th>
                        <th class="num">收入</th>
                        <th class="num">支出</th>
                        <th class="num">期末</th>
                        <th class="num" style="width: 80px">笔数</th>
                        <th style="width: 140px">校验</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="row in accountsOf(period)"
                        :key="String(row.accountId)"
                        data-test="period-account-row"
                        :data-account-id="row.accountId ?? ''"
                        :data-closing-fen="row.closingFen ?? ''"
                        :class="{ 'is-bad': isBadAccount(row) }"
                      >
                        <td class="mono">#{{ row.accountId ?? '-' }}</td>
                        <td>{{ row.accountName || '-' }}</td>
                        <td class="num">{{ money(row.openingFen) }}</td>
                        <td class="num">{{ money(row.incomeFen) }}</td>
                        <td class="num">{{ money(row.expenseFen) }}</td>
                        <td class="num" :data-closing-fen="row.closingFen ?? ''">{{ money(row.closingFen) }}</td>
                        <td class="num">{{ row.txnCount ?? '-' }}</td>
                        <td>
                          <el-tooltip v-if="isBadAccount(row)" :content="accountIssue(row)" placement="top">
                            <span class="bad">
                              <el-icon :size="13"><WarningFilled /></el-icon>
                              <template v-if="row.dirty">结账后仍有写入</template>
                              <template v-else>试算不平衡</template>
                            </span>
                          </el-tooltip>
                          <span v-else class="ok">通过</span>
                        </td>
                      </tr>
                      <tr v-if="accountsOf(period).length === 0">
                        <td colspan="8" class="detail__empty">该期间无账户明细</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </td>
            </tr>
          </template>

          <tr v-if="!loading && periods.length === 0">
            <td colspan="9" class="period-table__empty">暂无会计期间</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 反结账弹窗 -->
    <el-dialog v-model="reopenVisible" title="反结账" width="520px" :close-on-click-modal="false">
      <div class="reopen">
        <p class="reopen__hint">
          只允许对「最后一个已结账期间」反结账；反结账会解除写入拦截，并写入审计记录。
        </p>
        <p class="reopen__meta">期间：<span class="mono">{{ reopenCode }}</span></p>
        <el-form label-width="88px">
          <el-form-item label="反结账原因" required>
            <el-input
              v-model="reopenReason"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="请填写反结账原因（至少 10 个字）"
              data-test="reopen-reason"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="reopenVisible = false">取消</el-button>
        <el-button
          type="warning"
          data-test="reopen-confirm"
          :disabled="!reopenReasonValid"
          :loading="reopening"
          @click="submitReopen"
        >确认反结账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.period-close {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.period-close__error {
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
  white-space: pre-wrap;
}

.period-close__table-wrap {
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  padding: 8px 12px;
  overflow: auto;
}

.period-table,
.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.period-table th,
.period-table td,
.detail-table th,
.detail-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
  white-space: nowrap;
}

.period-table th,
.detail-table th {
  color: var(--fts-text-secondary, #606266);
  font-weight: 600;
  background: var(--fts-bg-subtle, #fafafa);
}

.period-row {
  cursor: pointer;
}

.period-row:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.period-row.is-closed {
  color: var(--fts-text-secondary, #606266);
}

.period-table__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 24px 0;
}

.pill {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.pill--open {
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
}

.pill--closed {
  background: var(--el-color-success-light-9, #f0f9eb);
  color: var(--el-color-success, #67c23a);
}

.pill--reopened {
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
  border: 1px dashed var(--el-color-warning-light-5, #f5dab1);
}

.detail-row > td {
  background: var(--fts-bg-subtle, #fafafa);
  padding: 10px 12px;
}

.detail__hint {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 6px;
}

.detail-table {
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 8px;
}

.detail-table tr.is-bad {
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
}

.bad {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--el-color-danger, #f56c6c);
}

.ok {
  color: var(--el-color-success, #67c23a);
}

.detail__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 16px 0;
}

.reopen__hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-color-warning-dark-2, #b88230);
}

.reopen__meta {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--fts-text-secondary, #909399);
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
</style>
