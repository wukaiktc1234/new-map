<script setup lang="ts">
/**
 * 调节表打印视图（P3-3）
 *
 * 独立路由 /finance/recon/statements/:id/print，供 window.open 打开。
 * 内容：表头（公司/账户/期间/单据号）、四类明细表、
 *      两侧余额与**调节后余额（全部取接口原值，禁止前端重算）**、审核人签字栏。
 * 打印时通过 @media print 隐藏应用导航与操作按钮。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getStatement, type ReconciliationItemVO, type ReconciliationStatementVO } from '@/api/finance-recon-statement'
import { formatFen } from '@/types/finance-recon'

const route = useRoute()
const statementId = computed<number>(() => Number(route.params.id))
const statement = ref<ReconciliationStatementVO | null>(null)
const loading = ref(false)
const errorText = ref('')

const CATEGORIES: Array<{ key: 'A' | 'B' | 'C' | 'D'; label: string; amountField: keyof ReconciliationStatementVO }> = [
  { key: 'A', label: 'A 企业已收、银行未收', amountField: 'unclearedInCredit' },
  { key: 'B', label: 'B 企业已付、银行未付', amountField: 'unclearedInDebit' },
  { key: 'C', label: 'C 银行已收、企业未收', amountField: 'unclearedOutCredit' },
  { key: 'D', label: 'D 银行已付、企业未付', amountField: 'unclearedOutDebit' },
]

function itemsOf(key: string): ReconciliationItemVO[] {
  return (statement.value?.items ?? []).filter((item) => String(item.category).toUpperCase() === key)
}

function amountOf(field: keyof ReconciliationStatementVO): number {
  const raw = statement.value?.[field]
  return typeof raw === 'number' ? raw : 0
}

function refSide(item: ReconciliationItemVO): string {
  if (item.flowId != null) return 'flow#' + item.flowId
  if (item.transactionId != null) return 'txn#' + item.transactionId
  return '-'
}

const statusText = computed<string>(() => {
  const map: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审核' }
  const status = statement.value?.status || ''
  return map[status] || status || '-'
})

async function load(): Promise<void> {
  if (!Number.isFinite(statementId.value) || statementId.value <= 0) {
    errorText.value = '调节表ID无效'
    return
  }
  loading.value = true
  try {
    statement.value = await getStatement(statementId.value)
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '调节表加载失败'
  } finally {
    loading.value = false
  }
}

/** 触发浏览器打印（模板里不能直接访问 window） */
function doPrint(): void {
  window.print()
}

/** 关闭打印标签页（由 window.open 打开） */
function doClose(): void {
  window.close()
}

onMounted(load)
</script>

<template>
  <div class="print-page">
    <div class="print-actions no-print">
      <button type="button" data-test="btn-print" class="print-actions__btn" @click="doPrint">打印</button>
      <button type="button" class="print-actions__btn" @click="doClose">关闭</button>
    </div>

    <div v-if="errorText" class="print-error">{{ errorText }}</div>

    <div v-else-if="statement" class="print-sheet" data-test="print-sheet" v-loading="loading">
      <h1 class="print-sheet__title">银行存款余额调节表</h1>

      <table class="print-head">
        <tbody>
          <tr>
            <th>公司</th><td>食品溯源系统（财务中心）</td>
            <th>单据号</th><td class="mono">{{ statement.statementNo }}</td>
          </tr>
          <tr>
            <th>账户</th><td>#{{ statement.accountId ?? '-' }}</td>
            <th>期间</th><td class="mono">{{ statement.periodStart }} ~ {{ statement.periodEnd }}</td>
          </tr>
          <tr>
            <th>状态</th><td>{{ statusText }}</td>
            <th>打印时间</th><td>{{ new Date().toLocaleString('zh-CN') }}</td>
          </tr>
        </tbody>
      </table>

      <h2 class="print-sheet__subtitle">一、企业账面余额与调节</h2>
      <table class="print-balance">
        <tbody>
          <tr>
            <th>企业账面余额</th><td class="num">{{ formatFen(statement.bookBalance) }}</td>
            <th>加：银行已收企业未收（C）</th><td class="num">{{ formatFen(statement.unclearedOutCredit) }}</td>
          </tr>
          <tr>
            <th>减：银行已付企业未付（D）</th><td class="num">{{ formatFen(statement.unclearedOutDebit) }}</td>
            <th>调节后（企业侧）</th>
            <td class="num strong" data-test="stmt-adj-book-fen" :data-fen="statement.adjustedBookBalance ?? 0">
              {{ formatFen(statement.adjustedBookBalance) }}
            </td>
          </tr>
        </tbody>
      </table>

      <h2 class="print-sheet__subtitle">二、对账单余额与调节</h2>
      <table class="print-balance">
        <tbody>
          <tr>
            <th>对账单余额</th><td class="num">{{ formatFen(statement.bankBalance) }}</td>
            <th>加：企业已收银行未收（A）</th><td class="num">{{ formatFen(statement.unclearedInCredit) }}</td>
          </tr>
          <tr>
            <th>减：企业已付银行未付（B）</th><td class="num">{{ formatFen(statement.unclearedInDebit) }}</td>
            <th>调节后（银行侧）</th>
            <td class="num strong" data-test="stmt-adj-bank-fen" :data-fen="statement.adjustedBankBalance ?? 0">
              {{ formatFen(statement.adjustedBankBalance) }}
            </td>
          </tr>
        </tbody>
      </table>

      <p class="print-sheet__balanced" data-test="stmt-balanced" :data-balanced="statement.isBalanced ? 'true' : 'false'">
        两侧调节后余额{{ statement.isBalanced ? '相等（已平）' : '不相等（未平，禁止提交/审核）' }}；
        同额歧义 {{ statement.ambiguousItemCount ?? 0 }} 条。
      </p>

      <h2 class="print-sheet__subtitle">三、未达账项明细</h2>
      <section v-for="meta in CATEGORIES" :key="meta.key" class="print-cat">
        <h3 class="print-cat__title">
          {{ meta.label }}（{{ itemsOf(meta.key).length }} 条 / 合计 {{ formatFen(amountOf(meta.amountField)) }} 元）
        </h3>
        <table class="print-items">
          <thead>
            <tr><th style="width: 90px">挂账人</th><th>原因</th><th style="width: 170px">期间</th><th style="width: 100px">金额(元)</th><th style="width: 110px">引用侧</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in itemsOf(meta.key)" :key="item.itemId">
              <td>{{ item.ownerName || (item.ownerUserId != null ? '#' + item.ownerUserId : '-') }}</td>
              <td>{{ item.reason || '-' }}</td>
              <td class="mono">{{ item.periodStart || '-' }} ~ {{ item.periodEnd || '-' }}</td>
              <td class="num">{{ formatFen(item.amount) }}</td>
              <td class="mono">{{ refSide(item) }}</td>
            </tr>
            <tr v-if="itemsOf(meta.key).length === 0"><td colspan="5" class="center">无</td></tr>
          </tbody>
        </table>
      </section>

      <table class="print-sign">
        <tbody>
          <tr>
            <th>编制人</th><td>{{ statement.createUserId != null ? '#' + statement.createUserId : '' }}</td>
            <th>提交人</th><td>{{ statement.submittedBy != null ? '#' + statement.submittedBy : '' }}</td>
            <th>审核人签字</th><td class="sign-cell">{{ statement.approvedBy != null ? '#' + statement.approvedBy : '' }}</td>
            <th>日期</th><td class="sign-cell">{{ statement.approvedAt || '' }}</td>
          </tr>
        </tbody>
      </table>

      <p v-if="statement.remark" class="print-sheet__remark">备注：{{ statement.remark }}</p>
    </div>
  </div>
</template>

<style scoped>
.print-page {
  padding: 16px;
  background: #fff;
  color: #1f2329;
  font-size: 13px;
}

.print-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.print-actions__btn {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}

.print-error {
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef0f0;
  color: #f56c6c;
}

.print-sheet {
  max-width: 980px;
  margin: 0 auto;
}

.print-sheet__title {
  text-align: center;
  font-size: 20px;
  margin: 6px 0 16px;
}

.print-sheet__subtitle {
  font-size: 14px;
  margin: 16px 0 6px;
  border-left: 3px solid #303133;
  padding-left: 8px;
}

.print-head,
.print-balance,
.print-items,
.print-sign {
  width: 100%;
  border-collapse: collapse;
}

.print-head th,
.print-head td,
.print-balance th,
.print-balance td,
.print-items th,
.print-items td,
.print-sign th,
.print-sign td {
  border: 1px solid #999;
  padding: 6px 8px;
  text-align: left;
  vertical-align: top;
}

.print-head th,
.print-balance th,
.print-items th,
.print-sign th {
  background: #f2f3f5;
  font-weight: 600;
  white-space: nowrap;
}

.print-sheet__balanced {
  margin: 10px 0 0;
  font-weight: 600;
}

.print-cat__title {
  font-size: 13px;
  margin: 10px 0 4px;
  font-weight: 600;
}

.center {
  text-align: center;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.strong {
  font-weight: 700;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.sign-cell {
  min-width: 110px;
}

.print-sheet__remark {
  margin-top: 12px;
  font-size: 12px;
  color: #4e5969;
}
</style>

<style>
/* 打印时隐藏应用导航、页签条与所有按钮（本页操作区也隐藏） */
@media print {
  .app-sidebar,
  .app-header,
  .sidebar,
  .header,
  .tab-bar,
  .tab-bar-tabs,
  .no-print,
  .el-button,
  .el-pagination {
    display: none !important;
  }

  .print-page {
    padding: 0;
  }

  @page {
    margin: 12mm;
  }
}
</style>
