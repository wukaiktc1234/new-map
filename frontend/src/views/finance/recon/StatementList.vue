<script setup lang="ts">
/**
 * 调节表列表（P3-3，S4）
 *
 * 列：单号 / 期间 / 账户 / 企业账面余额 / 对账单余额 / 调节后两侧 / is_balanced / AMBIGUOUS 数 / 状态。
 * 金额全部取接口原值（分），展示 /100；不做任何前端重算。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/core/PageHeader.vue'
import { listStatements, type ReconciliationStatementVO } from '@/api/finance-recon-statement'
import { formatFen } from '@/types/finance-recon'

const router = useRouter()

const rows = ref<ReconciliationStatementVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const loading = ref(false)
const errorText = ref('')

const statusFilter = ref('')
const accountIdFilter = ref('')
const periodRange = ref<[string, string] | null>(null)

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  APPROVED: '已审核',
}
function statusText(status: string | null | undefined): string {
  if (!status) return '-'
  return STATUS_LABELS[status] || status
}

/** 账户显示：接口只给 accountId，这里原样展示（不做额外请求） */
function accountText(row: ReconciliationStatementVO): string {
  return row.accountId != null ? '#' + row.accountId : '-'
}

async function load(): Promise<void> {
  loading.value = true
  errorText.value = ''
  try {
    const range = periodRange.value
    const page = await listStatements({
      status: statusFilter.value || undefined,
      accountId: accountIdFilter.value.trim() ? Number(accountIdFilter.value.trim()) : undefined,
      periodStart: range?.[0] || undefined,
      periodEnd: range?.[1] || undefined,
      current: current.value,
      size: size.value,
    })
    rows.value = page.records
    total.value = page.total
  } catch (error) {
    rows.value = []
    total.value = 0
    errorText.value = error instanceof Error ? error.message : '调节表列表加载失败'
  } finally {
    loading.value = false
  }
}

function openDetail(row: ReconciliationStatementVO): void {
  void router.push({ name: 'FinanceStatementDetail', params: { id: String(row.statementId) } })
}

function reset(): void {
  statusFilter.value = ''
  accountIdFilter.value = ''
  periodRange.value = null
  current.value = 1
  void load()
}

onMounted(load)

const pageCount = computed<number>(() => Math.max(1, Math.ceil((total.value || 0) / size.value)))
</script>

<template>
  <div class="modern-page statement-list">
    <PageHeader title="银行存款余额调节表" description="四类未达账项 · 两侧调节后余额勾稽 · 提交/审核门禁">
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </PageHeader>

    <section class="statement-list__filters">
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已提交" value="SUBMITTED" />
        <el-option label="已审核" value="APPROVED" />
      </el-select>
      <el-date-picker
        v-model="periodRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="期间开始"
        end-placeholder="期间结束"
        style="width: 260px"
      />
      <el-input v-model="accountIdFilter" placeholder="账户ID" clearable style="width: 140px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </section>

    <section v-if="errorText" class="statement-list__error">{{ errorText }}</section>

    <section class="statement-list__table-wrap" v-loading="loading">
      <table class="stmt-table">
        <thead>
          <tr>
            <th>单号</th>
            <th>期间</th>
            <th>账户</th>
            <th class="num">企业账面(元)</th>
            <th class="num">对账单(元)</th>
            <th class="num">调节后(账)</th>
            <th class="num">调节后(银)</th>
            <th>平衡</th>
            <th class="num">歧义</th>
            <th>状态</th>
            <th style="width: 90px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in rows"
            :key="row.statementId"
            data-test="stmt-row"
            :data-statement-id="row.statementId"
            :data-status="row.status || ''"
            @click="openDetail(row)"
          >
            <td class="mono">{{ row.statementNo }}</td>
            <td class="mono">{{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}</td>
            <td>{{ accountText(row) }}</td>
            <td class="num">{{ formatFen(row.bookBalance) }}</td>
            <td class="num">{{ formatFen(row.bankBalance) }}</td>
            <td class="num">{{ formatFen(row.adjustedBookBalance) }}</td>
            <td class="num">{{ formatFen(row.adjustedBankBalance) }}</td>
            <td>
              <span :class="row.isBalanced ? 'flag--ok' : 'flag--bad'">
                {{ row.isBalanced ? '已平' : '未平' }}
              </span>
            </td>
            <td class="num" :class="{ 'flag--bad': (row.ambiguousItemCount ?? 0) > 0 }">
              {{ row.ambiguousItemCount ?? 0 }}
            </td>
            <td><span class="status-pill" :data-status="row.status || ''">{{ statusText(row.status) }}</span></td>
            <td>
              <el-button link type="primary" @click.stop="openDetail(row)">查看</el-button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="11" class="stmt-table__empty">暂无调节表</td>
          </tr>
        </tbody>
      </table>
    </section>

    <div class="statement-list__pager">
      <el-pagination
        :current-page="current"
        :page-size="size"
        :total="total"
        layout="total,prev,pager,next"
        background
        @current-change="(p: number) => { current = p; load() }"
      />
      <span class="statement-list__count">共 {{ total }} 条 / {{ pageCount }} 页</span>
    </div>
  </div>
</template>

<style scoped>
.statement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.statement-list__filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.statement-list__error {
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
}

.statement-list__table-wrap {
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  padding: 8px 12px;
  overflow: auto;
}

.stmt-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.stmt-table th,
.stmt-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
  white-space: nowrap;
}

.stmt-table th {
  color: var(--fts-text-secondary, #606266);
  font-weight: 600;
  background: var(--fts-bg-subtle, #fafafa);
}

.stmt-table tbody tr {
  cursor: pointer;
}

.stmt-table tbody tr:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.stmt-table__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 24px 0;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.flag--ok {
  color: var(--el-color-success, #67c23a);
}

.flag--bad {
  color: var(--el-color-danger, #f56c6c);
}

.status-pill {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  background: var(--fts-bg-subtle, #f4f4f5);
  color: var(--fts-text-secondary, #606266);
}

.statement-list__pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.statement-list__count {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}
</style>
