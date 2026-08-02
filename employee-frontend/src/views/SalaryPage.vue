<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, ChatDotRound, Select, Warning, ArrowRight, CircleCheck } from '@element-plus/icons-vue'
import { PageContainer, StatusTag, StatCard, SectionHeader } from '@/components/core'
import ViewSwitcher from '@/components/core/ViewSwitcher.vue'
import { salaryApi } from '@/api'
import type { SalaryDeduction } from '@/types/salary'
import { useScreenSecurity } from '@/composables/useScreenSecurity'

interface SalaryRecord {
  id: string
  month: string
  monthShort: string
  baseSalary: number
  overtime: number
  bonus: number
  mealAllowance: number
  grossPay: number
  socialInsurance: number
  housingFund: number
  tax: number
  otherDeduction: number
  totalDeduction: number
  netPay: number
  status: 'available' | 'viewed' | 'confirmed' | 'disputed'
  confirmedAt?: string
  disputedReason?: string
  deductions: SalaryDeduction[]
}

const DISPUTE_TYPE_OPTIONS = [
  { value: 'salary_error', label: '工资金额有误' },
  { value: 'overtime_error', label: '加班费计算错误' },
  { value: 'deduction_error', label: '扣款项有误' },
  { value: 'missing_item', label: '遗漏项目' },
  { value: 'other', label: '其他' },
]

const payslips = ref<SalaryRecord[]>([])

async function fetchPayslips() {
  try {
    const response = await salaryApi.getPayslips({ page: 1, size: 20 })
    const records = response?.records || []
    payslips.value = records.map(item => {
      const detail = item.detail
      const deductions = detail?.deductions || []

      const socialInsurance = deductions
        .filter(d => d.category === 'social' && d.name !== '住房公积金')
        .reduce((sum, d) => sum + d.amount, 0)

      const housingFund = deductions
        .find(d => d.name === '住房公积金')?.amount || 0

      const tax = deductions
        .filter(d => d.category === 'tax')
        .reduce((sum, d) => sum + d.amount, 0)

      const otherDeduction = deductions
        .filter(d => d.category === 'other')
        .reduce((sum, d) => sum + d.amount, 0)

      const [year, monthNum] = item.yearMonth.split('-')

      return {
        id: item.id,
        month: `${year}年${parseInt(monthNum)}月`,
        monthShort: `${parseInt(monthNum)}月`,
        baseSalary: detail?.baseSalary || 0,
        overtime: detail?.overtimePay || 0,
        bonus: detail?.bonus || 0,
        mealAllowance: detail?.allowance || 0,
        grossPay: detail?.grossPay || 0,
        socialInsurance,
        housingFund,
        tax,
        otherDeduction,
        totalDeduction: detail?.totalDeduction || 0,
        netPay: detail?.netPay || 0,
        status: item.status === 'confirmed' ? 'confirmed'
          : item.status === 'draft' ? 'available'
          : item.status === 'disputed' ? 'disputed'
          : 'available',
        confirmedAt: item.confirmedAt,
        deductions,
      }
    })
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '获取工资条失败')
  }
}

onMounted(() => {
  // [M10] 工资页为敏感页面，启用截屏/录屏防护
  useScreenSecurity(true)
  fetchPayslips()
})

/** [M10] 页面卸载时移除截屏防护 */
onUnmounted(() => {
  useScreenSecurity(false)
})

const activeView = ref<'latest' | 'history' | 'annual'>('latest')
const showDisputeDialog = ref(false)
const showHistoryDetailDialog = ref(false)
const disputeTargetId = ref<string>('')
const disputeForm = ref({ type: '', description: '' })
const selectedHistorySlip = ref<SalaryRecord | null>(null)

const latestSlip = computed(() => payslips.value[0] || null)

const previousSlip = computed(() => payslips.value[1] || null)

const netPayChange = computed(() => {
  if (!latestSlip.value || !previousSlip.value) return null
  const diff = latestSlip.value.netPay - previousSlip.value.netPay
  return { diff, percent: ((diff / previousSlip.value.netPay) * 100).toFixed(1) }
})

const annualSummary = computed(() => {
  const records = payslips.value
  const totalGross = records.reduce((s, r) => s + r.grossPay, 0)
  const totalDeduction = records.reduce((s, r) => s + r.totalDeduction, 0)
  const totalNet = records.reduce((s, r) => s + r.netPay, 0)
  const avgNet = records.length > 0 ? Math.round(totalNet / records.length) : 0
  return { totalGross, totalDeduction, totalNet, avgNet, months: records.length }
})

const trendData = computed(() => {
  return [...payslips.value].reverse().map(s => ({
    month: s.monthShort,
    netPay: s.netPay,
    grossPay: s.grossPay,
  }))
})

const maxTrendPay = computed(() => {
  return Math.max(...trendData.value.map(d => d.grossPay), 1)
})

function getStatusInfo(record: SalaryRecord): { label: string; status: string } {
  switch (record.status) {
    case 'available': return { label: '待签收', status: 'pending' }
    case 'viewed': return { label: '待签收', status: 'pending' }
    case 'confirmed': return { label: '已签收', status: 'confirmed' }
    case 'disputed': return { label: '异议中', status: 'error' }
    default: return { label: record.status, status: 'info' }
  }
}

function getDeductionCategoryLabel(cat: string): string {
  switch (cat) {
    case 'social': return '社保'
    case 'housing': return '公积金'
    case 'tax': return '税费'
    default: return '其他'
  }
}

async function confirmReceipt(id: string) {
  try {
    await salaryApi.confirmPayslip(id)
    const rec = payslips.value.find(p => p.id === id)
    if (rec) {
      rec.status = 'confirmed'
      rec.confirmedAt = new Date().toISOString()
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '签收失败')
  }
}

function openDispute(id: string) {
  disputeTargetId.value = id
  disputeForm.value = { type: '', description: '' }
  showDisputeDialog.value = true
}

function openHistoryDetail(slip: SalaryRecord) {
  selectedHistorySlip.value = slip
  showHistoryDetailDialog.value = true
}

async function submitDispute() {
  if (!disputeForm.value.type || !disputeForm.value.description) return
  try {
    await salaryApi.disputePayslip(disputeTargetId.value, disputeForm.value.description)
    const rec = payslips.value.find(p => p.id === disputeTargetId.value)
    if (rec) {
      rec.status = 'disputed'
      rec.disputedReason = disputeForm.value.description
    }
    showDisputeDialog.value = false
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '提交异议失败')
  }
}

function downloadPDF(id: string) {
  const rec = payslips.value.find(p => p.id === id)
  if (!rec) return
  const link = document.createElement('a')
  link.href = '#'
  link.download = `工资条_${rec.month}.pdf`
  alert(`正在下载「${rec.month}」工资条PDF...（实际对接后端API生成）`)
}

function formatMoney(amount: number): string {
  return amount.toLocaleString('zh-CN')
}
</script>

<template>
  <PageContainer title="我的收入">
    <!-- 视图切换 -->
    <ViewSwitcher
      v-model="activeView"
      :options="[
        { key: 'latest', label: '最新工资' },
        { key: 'history', label: '历史记录' },
        { key: 'annual', label: '年度汇总' },
      ]"
    />

    <!-- ====== 最新工资视图 ====== -->
    <template v-if="activeView === 'latest' && latestSlip">
      <!-- 实发金额 Hero -->
      <div class="hero-wrapper">
        <StatCard
          variant="hero"
          :value="'¥' + formatMoney(latestSlip.netPay)"
          :label="latestSlip.month + ' 实发工资'"
          :trend="netPayChange ? (netPayChange.diff >= 0 ? '+' : '') + '¥' + formatMoney(netPayChange.diff) + ' (' + (netPayChange.diff >= 0 ? '+' : '') + netPayChange.percent + '%)' : undefined"
          :trend-type="netPayChange ? (netPayChange.diff >= 0 ? 'up' : 'down') : undefined"
        />
        <div class="hero-status">
          <StatusTag :status="getStatusInfo(latestSlip).status" :label="getStatusInfo(latestSlip).label" variant="solid" size="small" />
        </div>
      </div>

      <!-- 收入/扣除速览 -->
      <section class="quick-stats">
        <StatCard variant="grid" :value="'¥' + formatMoney(latestSlip.grossPay)" label="应发合计" />
        <div class="qs-divider">
          <span class="qs-minus">−</span>
        </div>
        <StatCard variant="grid" :value="'¥' + formatMoney(latestSlip.totalDeduction)" label="扣除合计" />
      </section>

      <!-- 收入明细 -->
      <section class="detail-section">
        <SectionHeader title="收入明细" size="small">
          <template #suffix>
            <span class="ds-total">¥{{ formatMoney(latestSlip.grossPay) }}</span>
          </template>
        </SectionHeader>
        <div class="ds-rows">
          <div class="ds-row">
            <span class="ds-row__label">基本工资</span>
            <span class="ds-row__value">¥{{ formatMoney(latestSlip.baseSalary) }}</span>
          </div>
          <div class="ds-row">
            <span class="ds-row__label">加班费</span>
            <span class="ds-row__value ds-row__value--plus">+¥{{ formatMoney(latestSlip.overtime) }}</span>
          </div>
          <div class="ds-row">
            <span class="ds-row__label">奖金</span>
            <span class="ds-row__value ds-row__value--plus">+¥{{ formatMoney(latestSlip.bonus) }}</span>
          </div>
          <div class="ds-row">
            <span class="ds-row__label">餐补</span>
            <span class="ds-row__value ds-row__value--plus">+¥{{ formatMoney(latestSlip.mealAllowance) }}</span>
          </div>
        </div>
      </section>

      <!-- 扣除明细 -->
      <section class="detail-section">
        <SectionHeader title="扣除明细" size="small">
          <template #suffix>
            <span class="ds-total ds-total--deduct">-¥{{ formatMoney(latestSlip.totalDeduction) }}</span>
          </template>
        </SectionHeader>
        <div class="ds-rows">
          <template v-for="(d, idx) in latestSlip.deductions" :key="idx">
            <div class="ds-row">
              <span class="ds-row__label">
                <span class="ds-row__cat">{{ getDeductionCategoryLabel(d.category) }}</span>
                {{ d.name }}
              </span>
              <span class="ds-row__value ds-row__value--minus">-¥{{ formatMoney(d.amount) }}</span>
            </div>
          </template>
        </div>
      </section>

      <!-- 操作区 -->
      <section class="action-area">
        <el-popconfirm
          v-if="latestSlip.status !== 'confirmed' && latestSlip.status !== 'disputed'"
          title="确认签收此工资条？签收后即表示您已核对无误。"
          confirm-button-text="确认签收"
          cancel-button-text="再看看"
          @confirm="confirmReceipt(latestSlip.id)"
        >
          <template #reference>
            <button class="action-btn action-btn--primary" @click.stop>
              <el-icon :size="16"><CircleCheck /></el-icon>
              确认签收
            </button>
          </template>
        </el-popconfirm>
        <button v-if="latestSlip.status === 'confirmed'" class="action-btn action-btn--done" disabled>
          <el-icon :size="14"><Select /></el-icon>
          已签收
        </button>
        <button class="action-btn action-btn--outline" @click="downloadPDF(latestSlip.id)">
          <el-icon :size="14"><Download /></el-icon>
          下载PDF
        </button>
        <button
          v-if="latestSlip.status !== 'disputed'"
          class="action-btn action-btn--ghost"
          @click="openDispute(latestSlip.id)"
        >
          <el-icon :size="14"><ChatDotRound /></el-icon>
          异议反馈
        </button>
      </section>

      <!-- 签收信息 -->
      <div v-if="latestSlip.status === 'confirmed' && latestSlip.confirmedAt" class="confirm-info">
        <el-icon :size="12"><Select /></el-icon>
        已于 {{ latestSlip.confirmedAt.slice(0, 16).replace('T', ' ').replace(/-/g, '/') }} 签收
      </div>
      <div v-if="latestSlip.status === 'disputed'" class="confirm-info confirm-info--dispute">
        <el-icon :size="12"><Warning /></el-icon>
        异议处理中
      </div>
    </template>

    <!-- ====== 历史记录视图 ====== -->
    <template v-if="activeView === 'history'">
      <section class="history-list">
        <div
          v-for="slip in payslips"
          :key="slip.id"
          class="history-card"
          @click="openHistoryDetail(slip)"
        >
          <div class="hc-left">
            <span class="hc-month">{{ slip.month }}</span>
            <StatusTag :status="getStatusInfo(slip).status" :label="getStatusInfo(slip).label" size="small" />
          </div>
          <div class="hc-right">
            <span class="hc-net">¥{{ formatMoney(slip.netPay) }}</span>
            <el-icon class="hc-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </section>
    </template>

    <!-- ====== 年度汇总视图 ====== -->
    <template v-if="activeView === 'annual'">
      <!-- 汇总数据 -->
      <section class="annual-stats">
        <StatCard
          variant="hero"
          :value="'¥' + formatMoney(annualSummary.totalNet)"
          label="年度实发"
          :sub-text="'月均 ¥' + formatMoney(annualSummary.avgNet)"
        />
        <div class="as-row">
          <StatCard variant="grid" :value="'¥' + formatMoney(annualSummary.totalGross)" label="年度应发" />
          <StatCard variant="grid" :value="'¥' + formatMoney(annualSummary.totalDeduction)" label="年度扣除" />
        </div>
      </section>

      <!-- 收入趋势图 -->
      <section class="trend-section">
        <h3 class="trend-title">月度收入趋势</h3>
        <div class="trend-chart">
          <div
            v-for="(d, idx) in trendData"
            :key="idx"
            class="trend-bar-group"
          >
            <div class="trend-bars">
              <div
                class="trend-bar trend-bar--gross"
                :style="{ height: (d.grossPay / maxTrendPay * 100) + '%' }"
                :title="'应发 ¥' + d.grossPay"
              ></div>
              <div
                class="trend-bar trend-bar--net"
                :style="{ height: (d.netPay / maxTrendPay * 100) + '%' }"
                :title="'实发 ¥' + d.netPay"
              ></div>
            </div>
            <span class="trend-month">{{ d.month }}</span>
          </div>
        </div>
        <div class="trend-legend">
          <span class="tl-item"><i class="tl-dot tl-dot--gross"></i>应发</span>
          <span class="tl-item"><i class="tl-dot tl-dot--net"></i>实发</span>
        </div>
      </section>
    </template>

    <!-- 底部提示 -->
    <div class="footer-note">
      <el-icon><Warning /></el-icon>
      <span>工资条仅供参考，实际金额以银行到账为准。如有疑问请使用「异议反馈」。</span>
    </div>

    <!-- 异议反馈弹窗 -->
    <el-dialog
      v-model="showDisputeDialog"
      title="工资异议反馈"
      width="420px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="dispute-form">
        <div class="df-field">
          <label class="df-label">异议类型 <span class="required-mark">*</span></label>
          <el-select v-model="disputeForm.type" placeholder="请选择异议类型" style="width: 100%" popper-class="fts-popover" :teleported="false">
            <el-option
              v-for="opt in DISPUTE_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="df-field">
          <label class="df-label">详细描述 <span class="required-mark">*</span></label>
          <el-input
            v-model="disputeForm.description"
            type="textarea"
            placeholder="请详细说明异议原因..."
            :rows="4"
            maxlength="300"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="showDisputeDialog = false">取消</el-button>
        <el-button
          type="warning"
          :disabled="!disputeForm.type || !disputeForm.description"
          @click="submitDispute"
        >
          提交反馈
        </el-button>
      </template>
    </el-dialog>

    <!-- 历史工资详情弹窗 -->
    <el-dialog
      v-model="showHistoryDetailDialog"
      :title="selectedHistorySlip?.month + ' 工资详情'"
      width="460px"
      destroy-on-close
    >
      <template v-if="selectedHistorySlip">
        <div class="detail-summary">
          <div class="ds-row ds-row--highlight">
            <span class="ds-row__label">实发工资</span>
            <span class="ds-row__value ds-row__value--highlight">
              ¥{{ formatMoney(selectedHistorySlip.netPay) }}
            </span>
          </div>
        </div>
        <div class="detail-section dialog-detail-spacing">
          <SectionHeader title="收入明细" size="small">
            <template #suffix>
              <span class="ds-total">¥{{ formatMoney(selectedHistorySlip.grossPay) }}</span>
            </template>
          </SectionHeader>
          <div class="ds-rows">
            <div class="ds-row"><span class="ds-row__label">基本工资</span><span class="ds-row__value">¥{{ formatMoney(selectedHistorySlip.baseSalary) }}</span></div>
            <div class="ds-row"><span class="ds-row__label">加班费</span><span class="ds-row__value ds-row__value--plus">+¥{{ formatMoney(selectedHistorySlip.overtime) }}</span></div>
            <div class="ds-row"><span class="ds-row__label">奖金</span><span class="ds-row__value ds-row__value--plus">+¥{{ formatMoney(selectedHistorySlip.bonus) }}</span></div>
            <div class="ds-row"><span class="ds-row__label">餐补</span><span class="ds-row__value ds-row__value--plus">+¥{{ formatMoney(selectedHistorySlip.mealAllowance) }}</span></div>
          </div>
        </div>
        <div class="detail-section">
          <SectionHeader title="扣除明细" size="small">
            <template #suffix>
              <span class="ds-total ds-total--deduct">-¥{{ formatMoney(selectedHistorySlip.totalDeduction) }}</span>
            </template>
          </SectionHeader>
          <div class="ds-rows">
            <template v-for="(d, idx) in selectedHistorySlip.deductions" :key="idx">
              <div class="ds-row">
                <span class="ds-row__label"><span class="ds-row__cat">{{ getDeductionCategoryLabel(d.category) }}</span>{{ d.name }}</span>
                <span class="ds-row__value ds-row__value--minus">-¥{{ formatMoney(d.amount) }}</span>
              </div>
            </template>
          </div>
        </div>
        <div class="dialog-footer-info">
          <StatusTag :status="getStatusInfo(selectedHistorySlip).status" :label="getStatusInfo(selectedHistorySlip).label" />
          <span v-if="selectedHistorySlip.confirmedAt" class="confirm-date">
            签收于 {{ selectedHistorySlip.confirmedAt.slice(0, 10).replace(/-/g, '/') }}
          </span>
        </div>
      </template>
      <template #footer>
        <el-button @click="showHistoryDetailDialog = false">关闭</el-button>
        <el-button
          v-if="selectedHistorySlip && selectedHistorySlip.status !== 'disputed'"
          type="warning"
          plain
          @click="showHistoryDetailDialog = false; openDispute(selectedHistorySlip.id)"
        >
          异议反馈
        </el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped lang="scss">
// ========== Hero Wrapper ==========
.hero-wrapper {
  position: relative;
  margin-bottom: var(--fts-space-4);
}

.hero-status {
  position: absolute;
  top: var(--fts-space-3);
  right: var(--fts-space-3);
}

// ========== Quick Stats ==========
.quick-stats {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-4);
}

.qs-divider {
  flex-shrink: 0;
}

.qs-minus {
  font-size: var(--fts-font-size-lg);
  font-weight: 700;
  color: var(--fts-text-tertiary);
}

// ========== Detail Section ==========
.detail-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-3);
  overflow: hidden;

  :deep(.sh) {
    margin-bottom: 0;
    padding: var(--fts-space-3) var(--fts-space-4);
    background: var(--fts-bg-tertiary);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.ds-total {
  font-size: var(--fts-font-size-base);
  font-weight: 800;
  color: var(--fts-success);

  &--deduct { color: var(--fts-error); }
}

.ds-rows {
  padding: var(--fts-space-2) var(--fts-space-4);
}

.ds-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-2) 0;

  & + & {
    border-top: 1px solid var(--fts-border-secondary);
  }

  &__label {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-secondary);
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__cat {
    font-size: 10px;
    font-weight: 600;
    padding: 1px 4px;
    border-radius: var(--fts-radius-xs);
    background: var(--fts-bg-tertiary);
    color: var(--fts-text-secondary);
  }

  &__value {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);

    &--plus { color: var(--fts-success); }
    &--minus { color: var(--fts-error); }
    &--highlight {
      color: var(--fts-primary);
      font-size: var(--fts-font-size-xl);
      font-weight: 800;
    }
  }
}

// ========== Action Area ==========
.action-area {
  display: flex;
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-4);
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: var(--fts-space-3) var(--fts-space-5);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  flex: 1;
  min-width: 0;

  &--primary {
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);
    border-color: var(--fts-primary);

    &:hover { opacity: 0.9; }
    &:active { transform: scale(0.97); }
  }

  &--done {
    background: rgba(var(--fts-success-rgb), 0.08);
    color: var(--fts-success);
    border-color: rgba(var(--fts-success-rgb), 0.20);
    cursor: default;
  }

  &--outline {
    background: transparent;
    color: var(--fts-text-secondary);
    border-color: var(--fts-border-primary);

    &:hover { border-color: var(--fts-primary); color: var(--fts-primary); }
    &:active { transform: scale(0.97); }
  }

  &--ghost {
    background: transparent;
    color: var(--fts-warning);
    border-color: rgba(var(--fts-warning-rgb), 0.30);

    &:hover { background: rgba(var(--fts-warning-rgb), 0.06); }
    &:active { transform: scale(0.97); }
  }
}

.confirm-info {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-success);
  font-weight: 500;

  &--dispute { color: var(--fts-error); }
}

// ========== History View ==========
.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.history-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
  }

  &:active { transform: scale(0.99); }
}

.hc-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.hc-month {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.hc-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.hc-net {
  font-size: var(--fts-font-size-lg);
  font-weight: 700;
  color: var(--fts-success);
}

.hc-arrow {
  color: var(--fts-text-quaternary);
  font-size: 14px;
}

// ========== Annual View ==========
.annual-stats {
  margin-bottom: var(--fts-space-5);
}

.as-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);
}

// ========== Trend Chart ==========
.trend-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
}

.trend-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-4);
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: var(--fts-space-4);
  height: 120px;
  padding: 0 var(--fts-space-2);
}

.trend-bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-1);
  height: 100%;
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  flex: 1;
  width: 100%;
}

.trend-bar {
  flex: 1;
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  transition: height 0.4s ease;

  &--gross {
    background: rgba(var(--fts-primary-rgb), 0.30);
  }

  &--net {
    background: var(--fts-primary);
  }
}

.trend-month {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: 500;
  white-space: nowrap;
}

.trend-legend {
  display: flex;
  justify-content: center;
  gap: var(--fts-space-5);
  margin-top: var(--fts-space-3);
}

.tl-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: var(--fts-text-tertiary);
  font-weight: 500;
}

.tl-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;

  &--gross { background: rgba(var(--fts-primary-rgb), 0.30); }
  &--net { background: var(--fts-primary); }
}

// ========== Footer ==========
.footer-note {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-5);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-warning-rgb), 0.06);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  line-height: 1.6;
  border: 1px solid rgba(var(--fts-warning-rgb), 0.12);
}

// ========== Dispute Dialog ==========
.dispute-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.df-field {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.df-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);

  .required-mark {
    color: var(--fts-error);
    margin-left: 2px;
    font-weight: 700;
  }
}

// ========== History Detail Dialog ==========
.detail-summary {
  background: rgba(var(--fts-primary-rgb), 0.06);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3) var(--fts-space-4);

  .ds-row--highlight {
    padding: 0;
    background: none;
    border: none;
  }
}

.dialog-detail-spacing {
  margin-top: var(--fts-space-3);
}

.dialog-footer-info {
  margin-top: var(--fts-space-3);
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.confirm-date {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
  margin-left: auto;
}
</style>
