<script setup lang="ts">
/**
 * 总部资金总览看板（P3-4）
 *
 * 数据源：GET /v1/finance/overview/daily?date=YYYY-MM-DD
 *
 * 反假 0 原则（沿用 P3-1 / AC-50b 教训）：
 * - 所有 data-* 属性在**未就绪**时输出空串，文本显示「—」；
 * - 根节点 `data-test="ov-state"` = loading | ready | idle，供 E2E 等就绪；
 * - 三卡数值 **全部取接口原值**，不做任何前端重算/求和。
 */
import { computed, onMounted, ref } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import { getFundOverviewDaily, OverviewApiError, type FundOverviewVO } from '@/api/finance-overview'
import { formatFen } from '@/types/finance-recon'

function todayString(): string {
  const now = new Date()
  const pad = (n: number) => (n < 10 ? '0' + n : String(n))
  return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
}

const date = ref<string>(todayString())
const data = ref<FundOverviewVO | null>(null)
const loading = ref(false)
const loadedOk = ref(false)
const errorText = ref('')
const forbidden = ref(false)

/** 根节点状态：loading | ready | idle（失败也回到 idle，避免 E2E 读到半成品） */
const ovState = computed<'loading' | 'ready' | 'idle'>(() => {
  if (loading.value) return 'loading'
  return loadedOk.value && data.value ? 'ready' : 'idle'
})
const ready = computed<boolean>(() => ovState.value === 'ready')

/** 属性绑定：未就绪 → 空串；就绪 → 接口原值（null 也空串，不写假 0） */
function attr(value: number | null | undefined): number | '' {
  if (!ready.value) return ''
  return typeof value === 'number' && Number.isFinite(value) ? value : ''
}

/** 人读金额：未就绪 → 「—」 */
function money(value: number | null | undefined): string {
  if (typeof value !== 'number' || !Number.isFinite(value)) return '—'
  return formatFen(value)
}

const stores = computed(() => data.value?.stores ?? [])
const channels = computed(() => data.value?.channels ?? null)
const statements = computed(() => data.value?.statements ?? null)

const oldestDays = computed<number | null>(() => statements.value?.oldestPendingDays ?? null)
const oldestDanger = computed<boolean>(() => (oldestDays.value ?? 0) > 7)

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  APPROVED: '已审核',
}
function statusText(status: string | null | undefined): string {
  if (!status) return '-'
  return STATUS_LABELS[status] || status
}

async function load(): Promise<void> {
  loading.value = true
  loadedOk.value = false
  errorText.value = ''
  forbidden.value = false
  try {
    data.value = await getFundOverviewDaily(date.value)
    loadedOk.value = true
  } catch (error) {
    data.value = null
    if (error instanceof OverviewApiError && error.forbidden) {
      forbidden.value = true
    } else {
      errorText.value = error instanceof Error ? error.message : '资金总览加载失败'
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="modern-page fund-overview" :data-test="'ov-state'" :data-state="ovState">
    <PageHeader title="总部资金总览" description="门店现金户余额 · 渠道到账与 SALE 差异 · 调节表闭环">
      <el-date-picker
        v-model="date"
        type="date"
        value-format="YYYY-MM-DD"
        :clearable="false"
        placeholder="业务日期"
        style="width: 180px"
        @change="load"
      />
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </PageHeader>

    <!-- 403：店长无归属门店 -->
    <section v-if="forbidden" class="fund-overview__forbidden" data-test="ov-forbidden">
      <el-icon :size="18"><WarningFilled /></el-icon>
      <span>无权查看资金总览</span>
    </section>

    <section v-else-if="errorText" class="fund-overview__error">{{ errorText }}</section>

    <template v-else>
      <!-- 卡1：门店现金户 -->
      <section class="ov-card" v-loading="loading">
        <header class="ov-card__header">
          <h3 class="ov-card__title">门店现金户</h3>
          <span class="ov-card__hint">
            范围：{{ data?.dataScope || '—' }}<template v-if="data?.storeId != null"> · 门店 #{{ data.storeId }}</template>
          </span>
        </header>
        <table class="ov-table">
          <thead>
            <tr>
              <th>门店</th>
              <th class="num">当前余额(元)</th>
              <th class="num">当日净变动(元)</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in stores"
              :key="String(row.storeId ?? row.storeName)"
              data-test="ov-store-row"
              :data-store-id="row.storeId ?? ''"
              :data-balance-fen="attr(row.balanceFen)"
              :data-net-fen="attr(row.netChangeFen)"
              :class="{ 'is-negative': row.negative }"
            >
              <td>
                <span class="store-name">{{ row.storeName || (row.storeId != null ? '门店 #' + row.storeId : '-') }}</span>
                <el-tooltip v-if="row.abnormal" content="该门店资金异常，请联系财务核查" placement="top">
                  <el-icon class="warn-icon" :size="14"><WarningFilled /></el-icon>
                </el-tooltip>
                <span v-if="row.negative" class="tag-negative">余额为负</span>
              </td>
              <td class="num" :class="{ 'is-negative': row.negative }">{{ money(row.balanceFen) }}</td>
              <td class="num" :class="{ 'is-negative': (row.netChangeFen ?? 0) < 0 }">{{ money(row.netChangeFen) }}</td>
            </tr>
            <tr v-if="stores.length === 0">
              <td colspan="3" class="ov-table__empty">{{ ready ? '当日无门店现金户数据' : '加载中…' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <div class="fund-overview__row">
        <!-- 卡2：渠道到账 vs SALE -->
        <section class="ov-card ov-card--half" v-loading="loading">
          <header class="ov-card__header">
            <h3 class="ov-card__title">渠道到账 vs SALE</h3>
            <span class="ov-card__hint">口径：接口原值，前端不重算</span>
          </header>
          <div class="ov-metrics">
            <div class="ov-metric">
              <div class="ov-metric__label">内部 SALE（元）</div>
              <div class="ov-metric__value" data-test="ov-channel-internal-fen" :data-fen="attr(channels?.internalSaleFen)">
                {{ money(channels?.internalSaleFen) }}
              </div>
            </div>
            <div class="ov-metric">
              <div class="ov-metric__label">外部实收（元）</div>
              <div class="ov-metric__value" data-test="ov-channel-external-fen" :data-fen="attr(channels?.externalReceivedFen)">
                {{ money(channels?.externalReceivedFen) }}
              </div>
            </div>
            <div class="ov-metric">
              <div class="ov-metric__label">差异（元）</div>
              <div
                class="ov-metric__value"
                :class="{ 'is-negative': (channels?.diffFen ?? 0) < 0, 'is-positive': (channels?.diffFen ?? 0) > 0 }"
                data-test="ov-channel-diff-fen"
                :data-fen="attr(channels?.diffFen)"
              >{{ money(channels?.diffFen) }}</div>
            </div>
            <div class="ov-metric">
              <div class="ov-metric__label">无法解释笔数</div>
              <div class="ov-metric__value" data-test="ov-channel-unexplained" :data-count="attr(channels?.unexplainedCount)">
                {{ ready ? (channels?.unexplainedCount ?? '—') : '—' }}
              </div>
            </div>
          </div>

          <table class="ov-table ov-table--compact">
            <thead>
              <tr>
                <th>批次</th>
                <th class="num">内部金额(元)</th>
                <th class="num">外部金额(元)</th>
                <th class="num">差异笔数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="run in channels?.runs ?? []" :key="String(run.runId)" data-test="ov-channel-run-row" :data-run-id="run.runId ?? ''">
                <td class="mono">#{{ run.runId ?? '-' }}</td>
                <td class="num">{{ money(run.internalAmount) }}</td>
                <td class="num">{{ money(run.externalAmount) }}</td>
                <td class="num">{{ run.differenceCount ?? '-' }}</td>
              </tr>
              <tr v-if="(channels?.runs ?? []).length === 0">
                <td colspan="4" class="ov-table__empty">{{ ready ? '当日无渠道跑批' : '加载中…' }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <!-- 卡3：未关闭调节表 -->
        <section class="ov-card ov-card--half" v-loading="loading">
          <header class="ov-card__header">
            <h3 class="ov-card__title">未关闭调节表</h3>
            <span class="ov-card__hint">状态机：DRAFT → SUBMITTED → APPROVED</span>
          </header>
          <div class="ov-metrics">
            <div class="ov-metric">
              <div class="ov-metric__label">未关闭数量</div>
              <div class="ov-metric__value" data-test="ov-open-count" :data-count="attr(statements?.openCount)">
                {{ ready ? (statements?.openCount ?? '—') : '—' }}
              </div>
            </div>
            <div class="ov-metric">
              <div class="ov-metric__label">最久待处理（天）</div>
              <div
                class="ov-metric__value"
                :class="{ 'is-negative': oldestDanger }"
                data-test="ov-oldest-days"
                :data-days="attr(oldestDays)"
              >{{ ready ? (oldestDays ?? '—') : '—' }}</div>
            </div>
          </div>

          <div class="ov-status-list" v-if="(statements?.byStatus ?? []).length">
            <span v-for="row in statements?.byStatus ?? []" :key="String(row.status)" class="ov-status-pill">
              {{ statusText(row.status) }}：{{ row.count ?? '-' }}
            </span>
          </div>

          <div v-if="statements?.oldest" class="ov-oldest">
            <div class="ov-oldest__title">最久未关闭</div>
            <div class="ov-oldest__body">
              单据 <span class="mono">#{{ statements.oldest.statementId ?? '-' }}</span> ·
              {{ statusText(statements.oldest.status) }} ·
              提交人 {{ statements.oldest.submittedBy != null ? '#' + statements.oldest.submittedBy : '-' }} ·
              {{ statements.oldest.submittedAt || '-' }} ·
              已挂 <span :class="{ 'is-negative': (statements.oldest.daysPending ?? 0) > 7 }">{{ statements.oldest.daysPending ?? '-' }}</span> 天
            </div>
          </div>
          <div v-else-if="ready" class="ov-oldest ov-oldest--empty">暂无未关闭调节表</div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.fund-overview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.fund-overview__forbidden {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 16px;
  border-radius: 12px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
  border: 1px solid var(--el-color-warning-light-5, #f5dab1);
  font-size: 14px;
  font-weight: 500;
}

.fund-overview__error {
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
}

.ov-card {
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  padding: 12px 14px;
}

.fund-overview__row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ov-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.ov-card__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-primary, #303133);
}

.ov-card__hint {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.ov-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.ov-table th,
.ov-table td {
  padding: 7px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
}

.ov-table th {
  color: var(--fts-text-secondary, #606266);
  font-weight: 600;
  background: var(--fts-bg-subtle, #fafafa);
}

.ov-table--compact {
  margin-top: 10px;
  font-size: 12px;
}

.ov-table__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 16px 0;
}

.ov-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.ov-metric {
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--fts-bg-subtle, #fafafa);
}

.ov-metric__label {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 6px;
}

.ov-metric__value {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary, #303133);
}

.ov-status-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.ov-status-pill {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  background: var(--fts-bg-subtle, #f4f4f5);
  color: var(--fts-text-secondary, #606266);
}

.ov-oldest {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  border: 1px solid var(--el-color-warning-light-5, #f5dab1);
  font-size: 12px;
  color: var(--el-color-warning-dark-2, #b88230);
}

.ov-oldest--empty {
  background: var(--fts-bg-subtle, #fafafa);
  border-color: var(--fts-border-secondary, #ebeef5);
  color: var(--fts-text-secondary, #909399);
}

.ov-oldest__title {
  font-weight: 600;
  margin-bottom: 4px;
}

.store-name {
  margin-right: 6px;
}

.warn-icon {
  color: var(--el-color-warning, #e6a23c);
  vertical-align: middle;
  margin-right: 6px;
}

.tag-negative {
  padding: 0 6px;
  border-radius: 8px;
  font-size: 11px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
}

.is-negative {
  color: var(--el-color-danger, #f56c6c);
}

.is-positive {
  color: var(--el-color-success, #67c23a);
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

@media (max-width: 1280px) {
  .fund-overview__row {
    grid-template-columns: minmax(0, 1fr);
  }

  .ov-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
