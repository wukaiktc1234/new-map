<script setup lang="ts">
/**
 * 现金缴款单列表（N19-3b）
 *
 * 新增列：
 * - 退款构成 `refundComponentFen` → 单元格 `data-rcf-fen`
 * - 净值 `differenceNetFen` → 单元格 `data-dnet-fen` + `data-dnet-state`
 *   （<0 短款 text-danger / >0 长款 text-success / =0 neutral）
 *
 * 顶部提示条 chips：still_open / stale / legacy_null / missing-time，计数 0 不显示。
 * 详情抽屉内包含「待确认归属」区块（仅存在边界退款时显示）。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import DepositSlipDetail from './components/DepositSlipDetail.vue'
import {
  confirmAttribution,
  getDailySummary,
  getDepositSlip,
  getPendingAttribution,
  listDepositSlips,
  type DepositDailySummaryVO,
  type DepositSlipHints,
  type DepositSlipVO,
  type PendingAttributionPayload,
} from '@/api/finance-deposit-slip'
import { bankAccountApi } from '@/api/finance/bank-account'
import { usePermissionStore } from '@/stores/permission'
import { formatFen } from '@/types/finance-recon'

const permissionStore = usePermissionStore()

const rows = ref<DepositSlipVO[]>([])
/** 列表级四项计数（后端 summary；缺失时用逐单后端字段求和） */
const summary = ref<DepositSlipHints | null>(null)
const total = ref(0)
const current = ref(1)
const size = ref(20)
const loading = ref(false)
const listError = ref('')

const statusFilter = ref('')
const storeIdFilter = ref('')
const dateRange = ref<[string, string] | null>(null)

const drawerVisible = ref(false)
const detail = ref<DepositSlipVO | null>(null)
/** 后端 pending-refunds 整对象（pendingRefunds + candidateShifts + missingRefundTimeCount） */
const attribution = ref<PendingAttributionPayload | null>(null)
const detailLoading = ref(false)
const confirming = ref(false)

/**
 * 归属确认权限（契约 v2 / fund-500）：**只认** finance:deposit-slip:attribution:confirm。
 *
 * 曾经做过「新码 || 旧码」双检兼容；实测发现 qa_store_b 只剩旧码时仍会渲染按钮，
 * 导致权限收回失效（验收 ④ 不通过），故改回单码判定。
 * 旧码已列入下一版本移除计划，不再作为兜底。
 */
const canConfirm = computed<boolean>(
  () => permissionStore.hasPermission('finance:deposit-slip:attribution:confirm'),
)

/** 归属提交的业务错误（400 重复确认 / 403 无权限 等，由后端 code+message 给出） */
const attributionError = ref('')

function money(fen: number | null | undefined): string {
  return typeof fen === 'number' && Number.isFinite(fen) ? formatFen(fen) : '—'
}

function dnetState(fen: number | null | undefined): 'negative' | 'positive' | 'neutral' {
  if (typeof fen !== 'number' || !Number.isFinite(fen)) return 'neutral'
  if (fen < 0) return 'negative'
  if (fen > 0) return 'positive'
  return 'neutral'
}

function dnetClass(fen: number | null | undefined): string {
  const state = dnetState(fen)
  if (state === 'negative') return 'text-danger'
  if (state === 'positive') return 'text-success'
  return 'text-neutral'
}

/**
 * 顶部提示条：四项计数**全部来自后端字段**（A39 口径），前端不做任何状态/时间推断。
 * - stillOpenCount：班次窗口未结束
 * - staleCount：本单窗口内存在 refund_time > slip.create_time 的退款
 * - legacyNullCount：历史单据缺少 N19-2 新字段
 * - missingRefundTimeCount：退款 refund_time 为空
 * 列表级 summary 存在则优先，否则对当前页逐单字段求和；计数 0 不显示。
 */
const hints = computed<Array<{ key: string; text: string; count: number }>>(() => {
  const list = rows.value
  const pick = (key: 'stillOpenCount' | 'staleCount' | 'legacyNullCount' | 'missingRefundTimeCount'): number => {
    const fromSummary = summary.value?.[key]
    if (typeof fromSummary === 'number') return fromSummary
    return list.reduce((acc, row) => acc + (row[key] ?? 0), 0)
  }

  const out: Array<{ key: string; text: string; count: number }> = []
  const stillOpen = pick('stillOpenCount')
  if (stillOpen > 0) out.push({ key: 'still-open', text: '有 ' + stillOpen + ' 个班次窗口未结束', count: stillOpen })

  const stale = pick('staleCount')
  if (stale > 0) {
    out.push({ key: 'stale', text: '有 ' + stale + ' 笔退款发生在单据生成之后（窗口外）', count: stale })
  }

  const legacyNull = pick('legacyNullCount')
  if (legacyNull > 0) out.push({ key: 'legacy-null', text: '有 ' + legacyNull + ' 笔历史单据无净值字段', count: legacyNull })

  const missing = pick('missingRefundTimeCount')
  if (missing > 0) {
    out.push({ key: 'missing-time', text: '有 ' + missing + ' 笔退款缺少发生时刻，未计入各单退款构成', count: missing })
  }

  return out
})

async function load(): Promise<void> {
  loading.value = true
  listError.value = ''
  try {
    const page = await listDepositSlips({
      status: statusFilter.value || undefined,
      storeId: storeIdFilter.value.trim() ? Number(storeIdFilter.value.trim()) : undefined,
      businessDateFrom: dateRange.value?.[0] || undefined,
      businessDateTo: dateRange.value?.[1] || undefined,
      current: current.value,
      size: size.value,
    })
    rows.value = page.records
    total.value = page.total
    summary.value = page.summary ?? null
  } catch (error) {
    rows.value = []
    total.value = 0
    listError.value = error instanceof Error ? error.message : '缴款单列表加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(row: DepositSlipVO): Promise<void> {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = row
  attribution.value = null
  try {
    const fresh = await getDepositSlip(row.slipId)
    detail.value = fresh
    attributionError.value = ''
    // N19-3b ⑤：无 confirm-attribution 权限时**不请求** pending-refunds（避免 403 噪声），
    // 给空载荷 → 详情用占位骨架渲染禁用态
    attribution.value = canConfirm.value
      ? await getPendingAttribution(row.slipId, fresh)
      : { pendingRefunds: [], candidateShifts: [], missingRefundTimeCount: 0 }
  } catch (error) {
    listError.value = error instanceof Error ? error.message : '缴款单详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

/** 确认归属：成功后用响应单据刷新详情并 toast */
async function handleConfirm(payload: { refundRecordId: number; shiftId: number; reason: string }): Promise<void> {
  if (!detail.value) return
  confirming.value = true
  try {
    const updated = await confirmAttribution(detail.value.slipId, payload)
    detail.value = updated
    // N19-3b ⑤：无 confirm-attribution 权限时不请求 pending-refunds（避免 403 噪声）
    attribution.value = canConfirm.value
      ? await getPendingAttribution(updated.slipId, updated)
      : { pendingRefunds: [], candidateShifts: [], missingRefundTimeCount: 0 }
    const idx = rows.value.findIndex((r) => r.slipId === updated.slipId)
    if (idx >= 0) rows.value[idx] = updated
    attributionError.value = ''
    ElMessage.success('已确认退款归属，单据退款构成与净值已重算')
  } catch (error) {
    // 后端统一 HTTP 200 + body.code（400 重复确认 / 403 无权限 / 其它）；
    // request 拦截器已按 code 弹出后端原文，这里只做面板内留痕，避免二次弹窗。
    attributionError.value = error instanceof Error ? error.message : '确认归属失败'
  } finally {
    confirming.value = false
  }
}

/* ===== 差异 3：本日存行汇总（人工汇总模式） ===== */
function todayString(): string {
  const now = new Date()
  const pad = (n: number) => (n < 10 ? '0' + n : String(n))
  return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
}

const summaryDate = ref<string>(todayString())
const dailySummary = ref<DepositDailySummaryVO | null>(null)
const summaryLoading = ref(false)
const summaryError = ref('')

/* ===== 多户歧义（bankInflowAccountAmbiguous） ===== */
/** 选中的银行账户（歧义时由用户选择；选定后请求带 accountId） */
const selectedAccountId = ref<number | null>(null)
/** 本店基本户列表（accountType=basic → 后端 1） */
const basicAccounts = ref<Array<{ id: number; label: string }>>([])
const accountsLoading = ref(false)
/** 账户列表取不到时的错误文案（此时下拉禁用，指标保持歧义提示） */
const accountsError = ref('')

const bankInflowAmbiguous = computed<boolean>(() => dailySummary.value?.bankInflowAccountAmbiguous === true)

async function loadBasicAccounts(): Promise<void> {
  accountsLoading.value = true
  accountsError.value = ''
  try {
    const page = await bankAccountApi.getList({
      accountType: 'basic',
      status: 'active',
      page: 1,
      size: 50,
    } as never)
    // 后端返回的是 accountId（没有 id 字段）；accountNo 由 accountNumber 兜底映射
    basicAccounts.value = (page.records || [])
      .map((acc) => {
        const raw = acc as unknown as Record<string, unknown>
        const idNum = Number(raw.accountId ?? raw.id)
        const name = String(raw.accountName ?? raw.bankName ?? '账户')
        const no = String(raw.accountNo ?? raw.accountNumber ?? '')
        return { id: idNum, label: name + (no ? ' …' + no.slice(-4) : '') }
      })
      .filter((acc) => Number.isFinite(acc.id) && acc.id > 0)
  } catch (error) {
    basicAccounts.value = []
    accountsError.value = '银行账户列表加载失败：'
      + (error instanceof Error ? error.message : '请检查 finance:bank:view 权限')
  } finally {
    accountsLoading.value = false
  }
}

/** 选择账户后重新请求（带 accountId） */
function handleSelectAccount(value: number | null): void {
  selectedAccountId.value = value
  void loadSummary()
}

const unmatchedCount = computed<number>(() => dailySummary.value?.unmatchedCount ?? 0)

/** 「人工确认一致」可用条件：unmatchedCount=0 且 合计应存现金 === 银行当日进账 */
const manualConfirmEnabled = computed<boolean>(() => {
  const s = dailySummary.value
  if (!s) return false
  // 多户歧义：银行进账未确定，禁止确认一致
  if (s.bankInflowAccountAmbiguous === true) return false
  // 空日守卫：当天没有任何存行记录时不允许「确认一致」（0 === 0 属于伪一致）
  if ((s.depositedSlips ?? []).length === 0) return false
  if ((s.unmatchedCount ?? 0) !== 0) return false
  if (typeof s.totalCashCountedFen !== 'number' || typeof s.bankInflowFen !== 'number') return false
  return s.totalCashCountedFen === s.bankInflowFen
})

/** 按钮禁用原因（tooltip） */
const manualConfirmHint = computed<string>(() => {
  const s = dailySummary.value
  if (!s) return '尚未加载本日存行汇总'
  if (s.bankInflowAccountAmbiguous === true) return '请先选择银行账户'
  if ((s.depositedSlips ?? []).length === 0) return '本日无存行记录，无需确认一致'
  if ((s.unmatchedCount ?? 0) > 0) {
    return '有 ' + s.unmatchedCount + ' 笔存行未匹配银行流水（合计 ' + money(s.unmatchedFen) + ' 元），无法确认一致'
  }
  if (typeof s.totalCashCountedFen !== 'number' || typeof s.bankInflowFen !== 'number') {
    return '汇总金额缺失，无法确认一致'
  }
  if (s.totalCashCountedFen !== s.bankInflowFen) {
    return '合计应存现金 ' + money(s.totalCashCountedFen) + ' 元 ≠ 银行当日进账 '
      + money(s.bankInflowFen) + ' 元，差额 ' + money(s.totalCashCountedFen - s.bankInflowFen) + ' 元'
  }
  return ''
})

async function loadSummary(): Promise<void> {
  summaryLoading.value = true
  summaryError.value = ''
  try {
    dailySummary.value = await getDailySummary(summaryDate.value, selectedAccountId.value)
  } catch (error) {
    dailySummary.value = null
    summaryError.value = error instanceof Error ? error.message : '本日存行汇总加载失败'
  } finally {
    summaryLoading.value = false
  }
}

/**
 * 人工确认一致。
 *
 * TODO(backend)：确认接口尚未就绪（fund-500 落盘中），前端只做 UI 可用性校验，
 * **不调用任何假接口**；接口上线后在此接入（预计 POST /v1/finance/deposit-slips/daily-summary/confirm）。
 */
function handleManualConfirm(): void {
  if (!manualConfirmEnabled.value) return
  ElMessage.info('人工确认接口尚未上线，本按钮当前仅做可用性校验')
}

function reset(): void {
  statusFilter.value = ''
  storeIdFilter.value = ''
  dateRange.value = null
  current.value = 1
  void load()
}

onMounted(() => {
  void load()
  void loadBasicAccounts()
  void loadSummary()
})

const pageCount = computed<number>(() => Math.max(1, Math.ceil((total.value || 0) / size.value)))
</script>

<template>
  <div class="modern-page slip-list">
    <PageHeader title="现金缴款单" description="班次缴款 · 退款构成与净值 · 待确认归属">
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </PageHeader>

    <!-- 差异 3：本日存行汇总（人工汇总模式） -->
    <section class="daily" v-loading="summaryLoading">
      <header class="daily__head">
        <h3 class="daily__title">本日存行汇总</h3>
        <!-- el-date-picker 不落地自定义属性，这里用 span 包裹以保证 E2E 能定位 -->
        <span data-test="daily-summary-date">
          <el-date-picker
            v-model="summaryDate"
            type="date"
            value-format="YYYY-MM-DD"
            :clearable="false"
            style="width: 160px"
            @change="loadSummary"
          />
        </span>
        <el-tooltip :disabled="manualConfirmEnabled" :content="manualConfirmHint" placement="top">
          <span>
            <el-button
              type="primary"
              data-test="btn-manual-confirm"
              :disabled="!manualConfirmEnabled"
              @click="handleManualConfirm"
            >人工确认一致</el-button>
          </span>
        </el-tooltip>
      </header>

      <div v-if="summaryError" class="daily__error">{{ summaryError }}</div>

      <div class="daily__metrics">
        <div class="daily__metric">
          <div class="daily__label">合计应存现金（元）</div>
          <div class="daily__value" data-test="daily-total-cash-fen" :data-fen="dailySummary?.totalCashCountedFen ?? ''">
            {{ money(dailySummary?.totalCashCountedFen) }}
          </div>
        </div>
        <div class="daily__metric">
          <div class="daily__label">已绑银行流水（元）</div>
          <div class="daily__value" data-test="daily-matched-fen" :data-fen="dailySummary?.matchedFen ?? ''">
            {{ money(dailySummary?.matchedFen) }}
          </div>
        </div>
        <div class="daily__metric">
          <div class="daily__label">未匹配银行流水（元）</div>
          <div
            class="daily__value"
            :class="{ 'text-danger': (dailySummary?.unmatchedFen ?? 0) !== 0 }"
            data-test="daily-unmatched-fen"
            :data-fen="dailySummary?.unmatchedFen ?? ''"
          >{{ money(dailySummary?.unmatchedFen) }}</div>
        </div>
        <div class="daily__metric">
          <div class="daily__label">银行当日进账（元）</div>
          <!-- 多户歧义：绝不允许把 null 渲染成 0.00 -->
          <div
            class="daily__value"
            data-test="daily-bank-inflow-fen"
            :data-fen="bankInflowAmbiguous ? '' : (dailySummary?.bankInflowFen ?? '')"
            :data-ambiguous="bankInflowAmbiguous ? 'true' : 'false'"
          >
            <span v-if="bankInflowAmbiguous" data-test="daily-bank-inflow-ambiguous">请选择银行账户</span>
            <template v-else>{{ money(dailySummary?.bankInflowFen) }}</template>
          </div>
          <div v-if="bankInflowAmbiguous" class="daily__account">
            <span
              data-test="daily-bank-inflow-account-select"
              :data-account-count="basicAccounts.length"
            >
              <el-select
                v-model="selectedAccountId"
                placeholder="选择银行账户"
                size="small"
                style="width: 100%"
                :disabled="accountsLoading || basicAccounts.length === 0"
                :loading="accountsLoading"
                @change="handleSelectAccount"
              >
                <el-option
                  v-for="acc in basicAccounts"
                  :key="acc.id"
                  :label="acc.label"
                  :value="acc.id"
                  :data-account-id="acc.id"
                />
              </el-select>
            </span>
            <div
              v-if="accountsError"
              class="daily__account-error"
              data-test="daily-bank-inflow-account-error"
            >{{ accountsError }}</div>
            <div
              v-else-if="!accountsLoading && basicAccounts.length === 0"
              class="daily__account-error"
              data-test="daily-bank-inflow-account-error"
            >本店没有可用的基本户（accountType=1），请先在账户管理维护</div>
          </div>
        </div>
      </div>

      <!-- 有未匹配存行时不可静默：红色横幅 -->
      <div v-if="unmatchedCount > 0" class="daily__banner" data-test="unmatched-banner">
        本日有 {{ unmatchedCount }} 笔存行未匹配银行流水（合计 {{ money(dailySummary?.unmatchedFen) }} 元）
      </div>

      <table v-if="(dailySummary?.depositedSlips ?? []).length" class="daily__table">
        <thead>
          <tr>
            <th style="width: 150px">单号</th>
            <th class="num" style="width: 70px">门店</th>
            <th class="num" style="width: 80px">班次</th>
            <th class="num" style="width: 120px">应存现金(元)</th>
            <th>银行流水号</th>
            <th class="num" style="width: 120px">银行进账(元)</th>
            <th style="width: 130px">状态</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in dailySummary?.depositedSlips ?? []"
            :key="String(row.slipId ?? row.slipNo)"
            :data-test="row.matched ? 'daily-slip-row' : 'unmatched-row'"
            :data-slip-id="row.slipId ?? ''"
            :data-matched="row.matched ? 'true' : 'false'"
            :class="{ 'is-unmatched': !row.matched }"
          >
            <td class="mono">{{ row.slipNo || '-' }}</td>
            <td class="num">{{ row.storeId ?? '-' }}</td>
            <td class="num">{{ row.shiftId ?? '-' }}</td>
            <td class="num">{{ money(row.cashCountedFen) }}</td>
            <td class="mono">{{ row.bankFlowNo || '—' }}</td>
            <td class="num">{{ money(row.bankInflowFen) }}</td>
            <td>
              <span v-if="!row.matched" class="text-danger">未匹配银行流水</span>
              <span v-else class="text-success">已匹配</span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="slip-list__filters">
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已提交" value="SUBMITTED" />
        <el-option label="已收款" value="RECEIVED" />
        <el-option label="已存行" value="DEPOSITED" />
        <el-option label="已复核" value="VERIFIED" />
        <el-option label="已关闭" value="CLOSED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-input v-model="storeIdFilter" placeholder="门店ID" clearable style="width: 130px" @keyup.enter="load" />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="业务日期起"
        end-placeholder="止"
        style="width: 260px"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </section>

    <!-- 顶部提示条 -->
    <div v-if="hints.length" class="slip-list__hints">
      <span
        v-for="hint in hints"
        :key="hint.key"
        class="slip-hint"
        :class="'slip-hint--' + hint.key"
        :data-hint="hint.key"
        :data-count="hint.count"
      >{{ hint.text }}</span>
    </div>

    <div v-if="listError" class="slip-list__error">{{ listError }}</div>

    <section class="slip-list__table-wrap" v-loading="loading">
      <table class="slip-table">
        <thead>
          <tr>
            <th style="width: 150px">单号</th>
            <th style="width: 110px">业务日期</th>
            <th class="num" style="width: 70px">门店</th>
            <th class="num" style="width: 90px">班次</th>
            <th class="num" style="width: 110px">应交(元)</th>
            <th class="num" style="width: 110px">实交(元)</th>
            <th class="num" style="width: 110px">差额(元)</th>
            <th
              class="num"
              style="width: 120px"
              title="本次存行中属于退款/冲回的金额"
            >退款构成(元)</th>
            <th
              class="num"
              style="width: 120px"
              title="净值 = actual − (expected − 退款构成)，即扣除退款后的真实长短款"
            >净值(元)</th>
            <th style="width: 100px">状态</th>
            <th style="width: 90px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in rows"
            :key="row.slipId"
            data-test="slip-row"
            :data-slip-id="row.slipId"
            class="slip-row"
          >
            <td class="mono">{{ row.slipNo }}</td>
            <td class="mono">{{ row.businessDate || '-' }}</td>
            <td class="num">{{ row.storeId ?? '-' }}</td>
            <td class="num">{{ row.shiftId ?? '-' }}</td>
            <td class="num">{{ money(row.expectedAmount) }}</td>
            <td class="num">{{ money(row.actualAmount) }}</td>
            <td class="num">{{ money(row.differenceAmount) }}</td>
            <td class="num" data-test="slip-rcf" :data-rcf-fen="row.refundComponentFen ?? ''">
              {{ money(row.refundComponentFen) }}
            </td>
            <td
              class="num"
              :class="dnetClass(row.differenceNetFen)"
              data-test="slip-dnet"
              :data-dnet-fen="row.differenceNetFen ?? ''"
              :data-dnet-state="dnetState(row.differenceNetFen)"
            >{{ money(row.differenceNetFen) }}</td>
            <td>{{ row.status || '-' }}</td>
            <td>
              <el-button link type="primary" data-test="slip-detail" @click="openDetail(row)">详情</el-button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="11" class="slip-table__empty">暂无缴款单</td>
          </tr>
        </tbody>
      </table>
    </section>

    <div class="slip-list__pager">
      <el-pagination
        :current-page="current"
        :page-size="size"
        :total="total"
        layout="total,prev,pager,next"
        background
        @current-change="(p: number) => { current = p; load() }"
      />
      <span class="slip-list__count">共 {{ total }} 条 / {{ pageCount }} 页</span>
    </div>

    <el-drawer v-model="drawerVisible" title="缴款单详情" size="62%" :destroy-on-close="false">
      <div v-loading="detailLoading">
        <DepositSlipDetail
          :slip="detail"
          :attribution="attribution"
          :can-confirm="canConfirm"
          :server-error="attributionError"
          :confirming="confirming"
          @confirm="handleConfirm"
        />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.slip-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

/* 本日存行汇总 */
.daily {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.daily__head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.daily__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-primary, #303133);
}

.daily__error {
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 12px;
}

.daily__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.daily__metric {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--fts-bg-subtle, #fafafa);
}

.daily__label {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 6px;
}

.daily__value {
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary, #303133);
}

.daily__ambiguous {
  color: var(--el-color-warning, #e6a23c);
  font-size: 14px;
  font-weight: 600;
}

.daily__account {
  margin-top: 8px;
}

.daily__account-error {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-danger, #f56c6c);
  line-height: 1.4;
}

.daily__banner {
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  border: 1px solid var(--el-color-danger-light-5, #fab6b6);
  font-size: 13px;
  font-weight: 500;
}

.daily__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.daily__table th,
.daily__table td {
  padding: 6px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
  white-space: nowrap;
}

.daily__table th {
  color: var(--fts-text-secondary, #606266);
  background: var(--fts-bg-subtle, #fafafa);
  font-weight: 600;
}

.daily__table tr.is-unmatched {
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
}

.text-danger {
  color: var(--el-color-danger, #f56c6c);
}

.text-success {
  color: var(--el-color-success, #67c23a);
}

.slip-list__filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.slip-list__hints {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

.slip-list__error {
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
}

.slip-list__table-wrap {
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  padding: 8px 12px;
  overflow: auto;
}

.slip-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.slip-table th,
.slip-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
  white-space: nowrap;
}

.slip-table th {
  color: var(--fts-text-secondary, #606266);
  font-weight: 600;
  background: var(--fts-bg-subtle, #fafafa);
}

.slip-row:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.slip-table__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 24px 0;
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

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.slip-list__pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.slip-list__count {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}
</style>
