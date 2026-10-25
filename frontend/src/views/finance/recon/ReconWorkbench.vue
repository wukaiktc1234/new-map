<script setup lang="ts">
/**
 * 财务对账工作台（P3-1）
 *
 * 三区布局：顶部摘要卡（AC-46a/46b/趋势） / 中部差异队列（R8 + 7 天窗 + 筛选 + 分页） / 右侧详情面板（AC-47）。
 *
 * 数据流与口径（硬约束）：
 * - AC-46a：摘要卡差异笔数 **只取 R3 的 differenceCount**（不信列表长度）。
 * - AC-46b：摘要卡差异金额 = **SUM(diffAmount)**，由 R8 `size=200` 逐页拉到末页聚合，单位分。
 * - 「较上次」不用 localStorage：R2 拉同账户历史批次 → 取 createTime 更早的最近一条 → 再聚合其 SUM(diffAmount)。
 * - 队列默认时间窗 from = 今天-7 天 00:00:00、to = 明天 00:00:00，**直接传给 R8**，前端不做过滤。
 * - 所有金额断言/比较使用分（fen），展示才 /100 toFixed(2)。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import ReconSummaryCard from './components/ReconSummaryCard.vue'
import ReconQueueTable from './components/ReconQueueTable.vue'
import ReconDetailPanel from './components/ReconDetailPanel.vue'
import ReconResolveDialog from './components/ReconResolveDialog.vue'
import {
  claimReconDifference,
  createReconRun,
  getReconRun,
  listAllReconDifferences,
  listReconDifferences,
  listReconRuns,
  resolveReconDifference,
} from '@/api/finance-recon'
import { sumFen } from '@/types/finance-recon'
import type {
  ReconDifferenceVO,
  ReconResolution,
  ReconRunVO,
} from '@/types/finance-recon'

/** 「较上次」载荷（与 ReconSummaryCard 的 TrendInfo 结构一致） */
interface TrendInfo {
  hasBaseline: boolean
  previousFen: number
  deltaFen: number
  baselineRunId: number | null
}

const currentRun = ref<ReconRunVO | null>(null)
/**
 * AC-46b：R8 全量聚合的 SUM(diffAmount)（分）。
 *
 * 【缺陷修复 2026-10-25】初值必须是 `null`（未知）而不是 0：
 * 之前初值 0 会在 R8 聚合返回前把摘要卡渲染成 "0.00"，E2E/用户会读成"差异金额为 0"（假 0）。
 * 现在只有聚合真正完成后才写入分值；失败/未完成时保持 null，卡片显示「—」。
 */
const diffAmountFen = ref<number | null>(null)
const amountLoading = ref(false)
const trend = ref<TrendInfo | null>(null)
const summaryLoading = ref(false)
const pageError = ref('')

const rows = ref<ReconDifferenceVO[]>([])
const queueLoading = ref(false)
const total = ref(0)
const current = ref(1)
const size = ref(20)

const statusFilter = ref('')
const typeFilter = ref('')
const runIdFilter = ref('')

const selected = ref<ReconDifferenceVO | null>(null)
const claiming = ref(false)
const resolveDialogVisible = ref(false)
const resolving = ref(false)
const rerunning = ref(false)

/** 两位补零 */
function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/** 本地时间 → yyyy-MM-dd */
function toDateString(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

/** 本地时间 → yyyy-MM-ddTHH:mm:ss（后端 LocalDateTime 接受 ISO-8601） */
function toIsoLocal(d: Date): string {
  return `${toDateString(d)}T${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/**
 * 队列默认 7 天窗：from = 今天-7 天 00:00:00，to = 明天 00:00:00（半开区间）。
 * 该窗口直接作为 R8 的 from/to 参数下发，前端不再二次过滤。
 */
function defaultWindow(): { from: string; to: string } {
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 7, 0, 0, 0, 0)
  const to = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 0, 0)
  return { from: toIsoLocal(from), to: toIsoLocal(to) }
}

/** 把接口返回的最新差异合并进列表与详情面板 */
function mergeDifference(updated: ReconDifferenceVO): void {
  const idx = rows.value.findIndex((row) => row.differenceId === updated.differenceId)
  if (idx >= 0) rows.value[idx] = { ...rows.value[idx], ...updated }
  if (selected.value && selected.value.differenceId === updated.differenceId) {
    selected.value = { ...selected.value, ...updated }
  }
}

/** 打开页面：先定批次，再并行拉摘要与队列 */
onMounted(async () => {
  await loadCurrentRun()
  await Promise.all([loadSummary(), loadQueue()])
})

/**
 * 选定「当前批次」：
 * - 队列筛选里填了 runId → 用该批次（R3）；
 * - 否则取 R2 最新一条（后端按 create_time 倒序）。
 */
async function loadCurrentRun(): Promise<void> {
  const explicitRunId = Number(runIdFilter.value)
  if (Number.isFinite(explicitRunId) && explicitRunId > 0) {
    try {
      currentRun.value = await getReconRun(explicitRunId)
      return
    } catch (error) {
      console.warn('[ReconWorkbench] 指定批次加载失败', error)
    }
  }
  try {
    const page = await listReconRuns({ current: 1, size: 20 })
    const records = page.records ?? []
    currentRun.value = records.length ? records[0] : null
  } catch (error) {
    console.warn('[ReconWorkbench] 跑批列表加载失败', error)
    currentRun.value = null
  }
}

/**
 * 摘要卡：
 * - AC-46a：R3.differenceCount（直接取，不数列表）；
 * - AC-46b：R8 全量分页（size=200）SUM(diffAmount)；
 * - 趋势：R2 同账户历史 → 上一条批次 → 其 SUM(diffAmount)。
 */
async function loadSummary(): Promise<void> {
  pageError.value = ''
  // 重新加载时先回到「未知」态，避免旧值/假 0 残留
  diffAmountFen.value = null
  const run = currentRun.value
  if (!run) {
    trend.value = { hasBaseline: false, previousFen: 0, deltaFen: 0, baselineRunId: null }
    return
  }

  summaryLoading.value = true
  try {
    // AC-46a：以 R3 为唯一口径刷新批次对象（差异笔数只认这个字段）
    const fresh = await getReconRun(run.runId)
    currentRun.value = fresh

    // AC-46b：R8 分页全量聚合（只按 runId，不加时间窗，确保是该批次全量）
    amountLoading.value = true
    const allDiffs = await listAllReconDifferences({ runId: fresh.runId }, 200)
    const totalFen = sumFen(allDiffs)
    // 拿到真实聚合值后才写入（null → 分值）；同时留一条可审计日志
    diffAmountFen.value = totalFen
    if (typeof fresh.differenceCount === 'number' && fresh.differenceCount > 0 && allDiffs.length === 0) {
      console.warn('[ReconWorkbench] R8 聚合返回 0 行但 R3.differenceCount>0，请检查 runId 过滤与分页参数', {
        runId: fresh.runId,
        differenceCount: fresh.differenceCount,
      })
    }

    // 「较上次」：R2 同账户历史批次（createTime 更早的最近一条）
    trend.value = await computeTrend(fresh, totalFen)
  } catch (error) {
    console.warn('[ReconWorkbench] 摘要加载失败', error)
    // 失败时保持 null（卡片显示「—」），不再落成误导性的 0.00
    diffAmountFen.value = null
    pageError.value = '摘要数据加载失败，请稍后重试'
  } finally {
    amountLoading.value = false
    summaryLoading.value = false
  }
}

/**
 * 「较上次」计算（禁用 localStorage，全部来自后端）：
 * 取 R2?accountId=当前批次.accountId&size=20 → 过滤 createTime < 当前批次.createTime 的最近一条
 * → 聚合该历史批次的 SUM(diffAmount) → 与当前批次比较 |金额|。
 */
async function computeTrend(run: ReconRunVO, currentFen: number): Promise<TrendInfo> {
  try {
    const page = await listReconRuns({ accountId: run.accountId, current: 1, size: 20 })
    const candidates = (page.records ?? []).filter((item) => {
      if (item.runId === run.runId) return false
      const itemTime = item.createTime ? Date.parse(item.createTime.replace(' ', 'T')) : NaN
      const runTime = run.createTime ? Date.parse(run.createTime.replace(' ', 'T')) : NaN
      if (Number.isNaN(itemTime) || Number.isNaN(runTime)) return false
      return itemTime < runTime
    })
    if (candidates.length === 0) {
      return { hasBaseline: false, previousFen: 0, deltaFen: 0, baselineRunId: null }
    }
    // 取 createTime 最大（即最近一次）的历史批次
    const baseline = candidates.reduce((prev, next) => {
      const prevTime = prev.createTime ? Date.parse(prev.createTime.replace(' ', 'T')) : 0
      const nextTime = next.createTime ? Date.parse(next.createTime.replace(' ', 'T')) : 0
      return nextTime > prevTime ? next : prev
    })
    const baselineDiffs = await listAllReconDifferences({ runId: baseline.runId }, 200)
    const previousFen = sumFen(baselineDiffs)
    return {
      hasBaseline: true,
      previousFen,
      deltaFen: currentFen - previousFen,
      baselineRunId: baseline.runId,
    }
  } catch (error) {
    console.warn('[ReconWorkbench] 趋势基线计算失败', error)
    return { hasBaseline: false, previousFen: 0, deltaFen: 0, baselineRunId: null }
  }
}

/** 队列：R8 + 默认 7 天窗 + 常显筛选（全部服务端过滤） */
async function loadQueue(): Promise<void> {
  queueLoading.value = true
  try {
    const { from, to } = defaultWindow()
    const runIdNum = Number(runIdFilter.value)
    const page = await listReconDifferences({
      status: statusFilter.value || undefined,
      type: typeFilter.value || undefined,
      runId: Number.isFinite(runIdNum) && runIdNum > 0 ? runIdNum : undefined,
      from,
      to,
      current: current.value,
      size: size.value,
    })
    rows.value = page.records ?? []
    total.value = page.total ?? 0
    if (rows.value.length && (!selected.value || !rows.value.some((r) => r.differenceId === selected.value?.differenceId))) {
      selected.value = rows.value[0]
    }
  } catch (error) {
    console.warn('[ReconWorkbench] 差异队列加载失败', error)
    rows.value = []
    total.value = 0
    pageError.value = '差异队列加载失败，请稍后重试'
  } finally {
    queueLoading.value = false
  }
}

async function handleSearch(): Promise<void> {
  current.value = 1
  await loadCurrentRun()
  await Promise.all([loadSummary(), loadQueue()])
}

async function handleReset(): Promise<void> {
  statusFilter.value = ''
  typeFilter.value = ''
  runIdFilter.value = ''
  current.value = 1
  await loadCurrentRun()
  await Promise.all([loadSummary(), loadQueue()])
}

async function handlePageChange(page: number): Promise<void> {
  current.value = page
  await loadQueue()
}

async function handleRefresh(): Promise<void> {
  await loadCurrentRun()
  await Promise.all([loadSummary(), loadQueue()])
}

/** 重跑当前批次（R1，force=false：幂等复用已有批次） */
async function handleRerun(): Promise<void> {
  const run = currentRun.value
  if (!run) return
  rerunning.value = true
  try {
    await createReconRun({ accountId: run.accountId, businessDate: run.businessDate, force: false })
    ElMessage.success('已提交跑批请求')
    await handleRefresh()
  } catch (error) {
    console.warn('[ReconWorkbench] 重跑失败', error)
    ElMessage.error('重跑失败，请稍后重试')
  } finally {
    rerunning.value = false
  }
}

/** AC-47 ①：认领（R9）→ 状态变 ASSIGNED，写入 assignedAt */
async function handleClaim(): Promise<void> {
  const diff = selected.value
  if (!diff) return
  claiming.value = true
  try {
    const updated = await claimReconDifference(diff.differenceId)
    mergeDifference(updated)
    ElMessage.success('认领成功')
  } catch (error) {
    console.warn('[ReconWorkbench] 认领失败', error)
    ElMessage.error('认领失败，请稍后重试')
  } finally {
    claiming.value = false
  }
}

/** 打开核销对话框（AC-48 的空原因禁用在对话框内部保证） */
function openResolveDialog(): void {
  if (!selected.value) return
  resolveDialogVisible.value = true
}

/** AC-47 ②：核销（R10）→ 状态变 CLOSED/EXEMPT，写入 resolutionTime/resolutionBy/resolutionRemark */
async function handleResolveSubmit(payload: { resolution: ReconResolution; remark: string }): Promise<void> {
  const diff = selected.value
  if (!diff) return
  if (!payload.remark.trim()) {
    // 双保险：即使绕过 UI 也绝不发请求（AC-48）
    return
  }
  resolving.value = true
  try {
    const updated = await resolveReconDifference(diff.differenceId, {
      resolution: payload.resolution,
      remark: payload.remark.trim(),
    })
    mergeDifference(updated)
    resolveDialogVisible.value = false
    ElMessage.success('处理成功')
    // 状态变化会影响队列筛选结果与摘要金额口径，刷新一次
    await loadQueue()
    await loadSummary()
  } catch (error) {
    console.warn('[ReconWorkbench] 处理失败', error)
    ElMessage.error('处理失败，请稍后重试')
  } finally {
    resolving.value = false
  }
}

const summaryRun = computed<ReconRunVO | null>(() => currentRun.value)
</script>

<template>
  <div class="modern-page recon-workbench">
    <PageHeader title="对账工作台" description="银行流水与业务单据勾稽 · 差异池认领与核销">
      <el-button :loading="rerunning" @click="handleRerun">重跑当前批次</el-button>
      <el-button type="primary" @click="handleRefresh">刷新</el-button>
    </PageHeader>

    <div v-if="pageError" class="recon-workbench__error">{{ pageError }}</div>

    <!-- ① 顶部摘要卡 -->
    <ReconSummaryCard
      :run="summaryRun"
      :diff-amount-fen="diffAmountFen"
      :trend="trend"
      :loading="summaryLoading"
      :amount-loading="amountLoading"
    />

    <!-- ② 差异队列 + ③ 右侧详情（三区布局的下半部分） -->
    <div class="recon-workbench__body">
      <ReconQueueTable
        :rows="rows"
        :total="total"
        :current="current"
        :size="size"
        :loading="queueLoading"
        :status-filter="statusFilter"
        :type-filter="typeFilter"
        :run-id-filter="runIdFilter"
        :selected-id="selected?.differenceId ?? null"
        @update:status-filter="(v: string) => (statusFilter = v)"
        @update:type-filter="(v: string) => (typeFilter = v)"
        @update:run-id-filter="(v: string) => (runIdFilter = v)"
        @search="handleSearch"
        @reset="handleReset"
        @page-change="handlePageChange"
        @select="(row: ReconDifferenceVO) => (selected = row)"
      />

      <ReconDetailPanel
        :difference="selected"
        :claiming="claiming"
        :resolving="resolving"
        @claim="handleClaim"
        @resolve="openResolveDialog"
      />
    </div>

    <!-- 核销对话框（R10，AC-48） -->
    <ReconResolveDialog
      v-model="resolveDialogVisible"
      :difference="selected"
      :submitting="resolving"
      @submit="handleResolveSubmit"
    />
  </div>
</template>

<style scoped>
.recon-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.recon-workbench__error {
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
}

.recon-workbench__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 12px;
  align-items: start;
}

@media (max-width: 1280px) {
  .recon-workbench__body {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
