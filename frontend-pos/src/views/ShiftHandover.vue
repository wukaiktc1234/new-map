<template>
  <el-dialog
    v-model="visible"
    title=""
    width="960px"
    class="shift-handover-dialog"
    :close-on-click-modal="false"
    :show-close="false"
    :lock-scroll="false"
    :append-to-body="true"
    data-test="shift-handover-dialog"
  >
    <div
      class="shift-handover-content"
      v-loading="loading || summaryLoading"
      element-loading-text="正在加载班次数据..."
    >
      <!-- 头部 -->
      <div class="sh-header">
        <div class="sh-brand">
          <span class="sh-brand-icon"><el-icon><Files /></el-icon></span>
          <div class="sh-title-group">
            <h2>交班日结</h2>
            <span class="sh-subtitle">Shift Handover</span>
          </div>
        </div>
        <div class="sh-meta">
          <span class="sh-shift-chip" :class="{ active: shiftActive }">{{ shiftActive ? '班次进行中' : '今日汇总' }}</span>
          <span class="sh-date">{{ currentDate }}</span>
          <span class="sh-time">{{ currentTime }}</span>
          <button type="button" class="sh-close" title="关闭" @click="visible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>

      <!-- 异常隔离：只报数量，点开单独处理，绝不混进明细（配色与异常抽屉同一套 warning 令牌） -->
      <div v-if="anomalyOrders.length > 0" class="sh-alert" role="alert" data-test="shift-anomaly-bar">
        <el-icon class="sh-alert__icon"><WarningFilled /></el-icon>
        <span class="sh-alert__text">
          本班次有 <b>{{ anomalyOrders.length }}</b> 笔异常单（{{ anomalySummary }}），<b>未计入</b>营业额与上交额
        </span>
        <button type="button" class="sh-alert__btn" data-test="shift-anomaly-open" @click="anomalyDrawer = true">
          去处理
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <!-- 主结论：正常态说"应上交"，短款态直接说"需补足"（不能让 ¥0.00 掩盖欠款） -->
      <div class="sh-hero" :class="{ 'is-short': isCashShort }" data-test="shift-handover-cash">
        <div class="sh-hero__head">
          <span class="sh-hero__label" data-test="shift-hero-label">
            {{ isCashShort ? '需补足（已从备用金垫付）' : '应上交现金' }}
          </span>
          <span class="sh-hero__value" :class="{ 'is-danger': isCashShort }" data-test="shift-hero-amount">
            <AnimatedAmount :value="isCashShort ? floatGap : handoverCash" />
          </span>
        </div>

        <!-- 三个数字块代替一整句算式：用眼睛对数字，不用读句子 -->
        <div class="sh-tiles">
          <div class="sh-tile">
            <span class="sh-tile__label">现金收入</span>
            <b class="sh-tile__value" data-test="shift-cash-in">{{ cashIn.toFixed(2) }}</b>
          </div>
          <div class="sh-tile">
            <span class="sh-tile__label">现金退款</span>
            <b class="sh-tile__value is-refund" data-test="shift-cash-refund">{{ cashRefund.toFixed(2) }}</b>
          </div>
          <div class="sh-tile">
            <span class="sh-tile__label">应上交现金</span>
            <b class="sh-tile__value" data-test="shift-cash-handover">{{ handoverCash.toFixed(2) }}</b>
          </div>
        </div>

        <p v-if="isCashShort" class="sh-hero__hint" data-test="shift-cash-negative">
          <el-icon><WarningFilled /></el-icon>
          退款比收款多 ¥{{ floatGap.toFixed(2) }}，已从备用金支出；交班后请补回钱箱，否则下一班零钱不足。
        </p>
        <span class="is-sr-only" data-test="shift-cash-net">净现金 ¥{{ cashNet.toFixed(2) }}</span>
      </div>

      <!-- 钱箱核对：左"该有多少"，右"实际点出多少"，输入是明确的动手焦点 -->
      <div class="sh-cashcheck" data-test="shift-cash-check">
        <div class="sh-cashcheck__left">
          <span class="sh-cashcheck__label">钱箱应有现金</span>
          <span class="sh-cashcheck__value" data-test="shift-drawer-expected">
            <AnimatedAmount :value="drawerExpected" />
          </span>
          <span class="sh-cashcheck__sub">
            含备用金 <b data-test="shift-opening-cash">¥{{ openingCash.toFixed(2) }}</b>（留存钱箱）
            <span v-if="openingCashMissing" class="sh-hero__warn" data-test="shift-opening-missing">未读取到</span>
          </span>
          <span class="is-sr-only" data-test="shift-drawer-formula">备用金 + 现金收入 − 现金退款</span>
        </div>

        <div class="sh-cashcheck__right">
          <label class="sh-cashcheck__label" for="pos-cash-counted">实点现金</label>
          <el-input
            id="pos-cash-counted"
            v-model="cashCountedInput"
            class="sh-count-input"
            placeholder="点完钱填入"
            inputmode="decimal"
            data-test="shift-cash-counted"
            @blur="normalizeCounted"
          />
          <span class="sh-cc-diff" :class="cashDiffClass" data-test="shift-cash-diff">{{ cashDiffText }}</span>
        </div>
      </div>

      <!-- 非现金收入：单独成块，明确"已直连入账，无需上交" -->
      <div class="sh-noncash">
        <div class="sh-noncash__head">
          <span class="sh-noncash__title">非现金收入</span>
          <span class="sh-noncash__tag">
            <el-icon><CircleCheckFilled /></el-icon>
            已直连入账，无需上交
          </span>
        </div>
        <div class="sh-noncash__grid">
          <div
            v-for="card in nonCashCards"
            :key="card.key"
            class="sh-nc-card"
            :data-test="'shift-noncash-' + card.key"
          >
            <span class="sh-nc-card__head">
              <el-icon class="sh-nc-card__icon"><component :is="card.icon" /></el-icon>
              {{ card.name }}
            </span>
            <span class="sh-nc-card__amount" :class="{ 'is-refund': card.isNetRefund }">
              <em v-if="card.isNetRefund" class="sh-nc-card__flag">净退款</em>
              ¥{{ Math.abs(card.net).toFixed(2) }}
            </span>
            <span v-if="card.refund > 0" class="sh-nc-card__detail">
              收入 ¥{{ card.income.toFixed(2) }} · 退款 ¥{{ card.refund.toFixed(2) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 次要指标 -->
      <div class="sh-stat-grid">
        <div class="sh-mini-stat">
          <span class="sh-ms-label">净营业额</span>
          <span class="sh-ms-value" data-test="shift-net-revenue">¥{{ netRevenue.toFixed(2) }}</span>
          <span class="sh-ms-pct">
            已支付 {{ paidOrders.length }} 笔 − 退款 {{ refundedOrders.length }} 笔<template v-if="netRevenue < 0"> · 退款居多</template>
          </span>
        </div>
        <div class="sh-mini-stat">
          <span class="sh-ms-label">有效订单</span>
          <span class="sh-ms-value" data-test="shift-valid-count">{{ validOrders.length }}</span>
          <span class="sh-ms-pct">另有 {{ anomalyOrders.length }} 笔未计入</span>
        </div>
        <div class="sh-mini-stat">
          <span class="sh-ms-label">客单价</span>
          <span class="sh-ms-value">¥{{ avgTicket.toFixed(2) }}</span>
          <span class="sh-ms-pct">按已支付订单计</span>
        </div>
        <div class="sh-mini-stat">
          <span class="sh-ms-label">退款</span>
          <span class="sh-ms-value is-danger">¥{{ refundTotal.toFixed(2) }}</span>
          <span class="sh-ms-pct">{{ refundedOrders.length }} 笔（含现金 ¥{{ cashRefund.toFixed(2) }}）</span>
        </div>
      </div>

      <!-- 明细入口：主界面不铺列表，一律走抽屉 -->
      <button
        type="button"
        class="sh-detail-btn"
        data-test="shift-detail-button"
        @click="detailDrawer = true"
      >
        <el-icon class="sh-detail-btn__icon"><Tickets /></el-icon>
        <span class="sh-detail-btn__text">调阅本班次所有订单明细（共 {{ validOrders.length }} 笔）</span>
        <el-icon class="sh-detail-btn__arrow"><ArrowRight /></el-icon>
      </button>

      <!-- 操作区 -->
      <div class="sh-actions">
        <input
          v-model="handoverForm.remark"
          type="text"
          class="sh-remark-input"
          placeholder="交班备注（选填，如钱箱差异说明）"
          maxlength="100"
          data-test="shift-remark"
        />
        <div class="sh-btn-row">
          <button class="sh-btn sh-btn-ghost" :disabled="exporting" @click="exportReport">
            <el-icon><Download /></el-icon>
            导出报表
          </button>
          <button class="sh-btn sh-btn-ghost" @click="printReport">
            <el-icon><Printer /></el-icon>
            打印小票
          </button>
          <button class="sh-btn sh-btn-primary" :disabled="endingShift" data-test="shift-confirm" @click="confirmHandover">
            <el-icon v-if="endingShift" class="is-loading"><Loading /></el-icon>
            {{ endingShift ? '交班中…' : '确认交班' }}
          </button>
        </div>
        <div class="sh-shift-note" :class="{ active: shiftActive }">{{ shiftStatusText }}</div>
      </div>
    </div>

    <!-- 明细抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      title="本班次订单明细"
      size="560px"
      :append-to-body="true"
      data-test="shift-detail-drawer"
    >
      <div class="sh-drawer">
        <div class="sh-drawer-summary">
          共 <b>{{ validOrders.length }}</b> 笔有效订单 · 净营业额 <b>¥{{ netRevenue.toFixed(2) }}</b> ·
          应上交现金 <b>¥{{ handoverCash.toFixed(2) }}</b>
        </div>
        <div class="sh-section">
          <div class="sh-section-header">
            <span class="sh-section-title">支付方式分布（有效订单净额）</span>
          </div>
          <div class="sh-payment-bars">
            <div v-for="row in paymentRows" :key="row.method" class="sh-bar-row">
              <span class="sh-bar-name">{{ row.name }}</span>
              <div class="sh-bar-track">
                <div class="sh-bar-fill" :class="'bar-' + row.method" :style="{ width: row.percentage + '%' }"></div>
              </div>
              <span class="sh-bar-val">¥{{ row.amount.toFixed(2) }}</span>
              <span class="sh-bar-pct">{{ row.percentage.toFixed(1) }}%</span>
            </div>
            <div v-if="paymentRows.length === 0" class="sh-bar-empty">本班次暂无有效收款</div>
          </div>
        </div>

        <div class="sh-drawer-filter">
          <el-select v-model="filterPaymentMethod" placeholder="全部支付方式" clearable size="small" style="width: 170px">
            <el-option v-for="m in paymentMethods" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </div>
        <div class="sh-order-list">
          <div v-for="(order, idx) in filteredValidOrders" :key="order.orderNumber || idx" class="sh-order-item">
            <div class="sh-o-row sh-o-row--top">
              <span class="sh-o-time">{{ formatTime(order.createTime, '--:--') }}</span>
              <span class="sh-o-no">#{{ order.orderNumber?.slice(-6) }}</span>
              <span class="sh-o-status" :class="'s-' + order.statusKey">{{ order.statusText }}</span>
            </div>
            <div class="sh-o-row sh-o-row--bottom">
              <span class="sh-o-method" :class="'m-' + order.methodKey">{{ getPaymentMethodName(order.paymentMethod) }}</span>
              <span class="sh-o-amount">¥{{ order.amount.toFixed(2) }}</span>
            </div>
          </div>
          <div v-if="filteredValidOrders.length === 0" class="sh-orders-empty">暂无有效订单</div>
        </div>
      </div>
    </el-drawer>

    <!-- 异常单抽屉 -->
    <el-drawer
      v-model="anomalyDrawer"
      title="异常单待处理"
      size="560px"
      :append-to-body="true"
      data-test="shift-anomaly-drawer"
    >
      <div class="sh-drawer">
        <div class="sh-anomaly-tip">
          <el-icon><InfoFilled /></el-icon>
          以下订单未计入营业额与应上交现金。请逐单核实：待付单请补收款或作废，取消单请确认是否误操作，无流水单请联系店长核对支付渠道。
        </div>
        <div class="sh-order-list">
          <div v-for="(order, idx) in anomalyOrders" :key="order.orderNumber || idx" class="sh-order-item is-anomaly">
            <div class="sh-o-row sh-o-row--top">
              <span class="sh-o-time">{{ formatTime(order.createTime, '--:--') }}</span>
              <span class="sh-o-no">#{{ order.orderNumber?.slice(-6) }}</span>
              <span class="sh-o-reason">{{ order.anomalyReason }}</span>
            </div>
            <div class="sh-o-row sh-o-row--bottom">
              <span class="sh-o-method">{{ getPaymentMethodName(order.paymentMethod) }}</span>
              <span class="sh-o-amount">¥{{ order.amount.toFixed(2) }}</span>
            </div>
          </div>
          <div v-if="anomalyOrders.length === 0" class="sh-orders-empty">本班次无异常单</div>
        </div>
      </div>
    </el-drawer>
  </el-dialog>
</template>
<script setup lang="ts">
/**
 * 交班日结弹窗（第四轮 · 财务口径校正）
 *
 * ── 财务模型（对齐真实门店） ──────────────────────────────
 *   备用金          开班时放进钱箱的零钱，**留存不上交**
 *   应上交现金      现金收入 − 现金退款          ← 收银员唯一要交出去的钱（核心大字）
 *   钱箱理论应有    备用金 + 现金收入 − 现金退款  ← 收银员自己数钱核对用（小字）
 *   非现金收入      微信 / 支付宝 / 银行卡（含会员余额）→ 已直连入账，无需上交，单独成块
 *
 * ── 数据纪律 ─────────────────────────────────────────────
 *   只统计「已支付 / 已退款」且**有支付流水且金额 > 0** 的订单；
 *   待付、已取消、退款中、未记录流水、金额 ≤ 0 一律判为异常单，不进统计也不铺进明细。
 *
 * 数据源：/v1/pos/orders 明细（清洗唯一真相）→ /v1/pos/auth/shift/summary（班次状态）
 *        → /v1/pos/auth/shift/today（备用金兜底）→ POST /v1/pos/auth/shift/end
 */
import { computed, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Files,
  Tickets,
  WarningFilled,
  Download,
  Printer,
  Close,
  ArrowRight,
  CircleCheckFilled,
  InfoFilled,
  Loading,
  ChatDotRound,
  Wallet,
  CreditCard,
  Postcard
} from '@element-plus/icons-vue'
import request from '@/api/request'
import { posApi } from '@/api/posApi'
import { formatDate, formatDateTime, formatTime, parseDateTime } from '@/utils/format'
import AnimatedAmount from '@/components/pos/AnimatedAmount.vue'
import { clearShiftMark, readShiftMark } from '@/utils/shiftSession'
import logger from '@/utils/logger'

interface RawOrder {
  orderNumber?: string
  createTime?: string
  totalAmount?: number | string | null
  paymentMethod?: string | null
  status?: string | number | null
}

type OrderStatusKey = 'paid' | 'refunded' | 'anomaly'

interface CleanOrder {
  orderNumber: string
  createTime: string
  amount: number
  paymentMethod: string
  methodKey: string
  statusKey: OrderStatusKey
  statusText: string
  anomalyReason: string
}

interface ShiftOrderStats {
  total_orders?: number | string | null
  total_amount?: number | string | null
  avg_amount?: number | string | null
  payment_breakdown?: Record<string, number | string | null> | null
}

interface ShiftInfo {
  shiftId?: number | string
  terminalId?: string
  employeeId?: string | null
  employeeName?: string | null
  shiftType?: string
  startTime?: string
  endTime?: string | null
  status?: string
}

interface ShiftSummaryResponse {
  shiftInfo?: ShiftInfo | null
  shiftActive?: boolean
  summaryStartTime?: string
  orderStats?: ShiftOrderStats | null
}

interface PaymentRow {
  method: string
  name: string
  amount: number
  percentage: number
}

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

// ---- 状态 ----
const rawOrders = ref<RawOrder[]>([])
const loading = ref(false)
const exporting = ref(false)
const endingShift = ref(false)
const detailDrawer = ref(false)
const anomalyDrawer = ref(false)
const filterPaymentMethod = ref<string>('')
const handoverForm = ref({ remark: '' })

const terminalId = ref('')
const summaryLoading = ref(false)
const shiftActive = ref(false)
const shiftInfo = ref<ShiftInfo | null>(null)
const summaryStartTime = ref('')
const summaryOrderStats = ref<ShiftOrderStats | null>(null)

/** 备用金（元）：优先开班标记，其次今日班次接口，最后 0 并告警 */
/** 金额一律用"分"做整数运算，避免 0.1+0.2 这类浮点漂移 */
const toCents = (yuan: number): number => Math.round((Number.isFinite(yuan) ? yuan : 0) * 100)

const openingCash = ref(0)
const openingCashMissing = ref(false)

const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval> | null = null

const updateTime = (): void => {
  const now = new Date()
  const pad = (n: number): string => String(n).padStart(2, '0')
  currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}
const currentDate = computed(() => formatDate(new Date()))

// ---- 支付方式归一 ----
const paymentMethods = [
  { value: 'cash', label: '现金' },
  { value: 'wechat', label: '微信' },
  { value: 'alipay', label: '支付宝' },
  { value: 'card', label: '银行卡' },
  { value: 'balance', label: '余额' }
]

const PAYMENT_METHOD_NAMES: Record<string, string> = {
  cash: '现金',
  wechat: '微信',
  alipay: '支付宝',
  card: '银行卡',
  balance: '余额',
  other: '其他'
}

const PAYMENT_LABEL_TO_KEY: Record<string, string> = {
  现金: 'cash',
  微信: 'wechat',
  微信支付: 'wechat',
  支付宝: 'alipay',
  银行卡: 'card',
  余额: 'balance',
  余额支付: 'balance',
  积分支付: 'balance'
}

const paymentKey = (method?: string | null): string => {
  const raw = (method ?? '').trim()
  if (!raw) return ''
  if (PAYMENT_METHOD_NAMES[raw]) return raw
  return PAYMENT_LABEL_TO_KEY[raw] ?? 'other'
}

const getPaymentMethodName = (method?: string | null): string => {
  const key = paymentKey(method)
  if (!key) return '未记录'
  return PAYMENT_METHOD_NAMES[key] || (method ?? '未知')
}

const toNumber = (value: unknown): number => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

// ---- 状态清洗 ----
/** 已支付：1已确认 / 2已完成 / 4完成（历史码） */
const PAID_STATUS = new Set(['1', '2', '4', 'paid', 'completed', 'confirmed', '已确认', '已完成', '已支付', 'PAID'])
/** 已退款：5全额退款 / 7已退（历史码） */
const REFUNDED_STATUS = new Set(['5', '7', 'refunded', '已退', '已退款', 'REFUNDED'])

const ANOMALY_STATUS_TEXT: Record<string, string> = {
  '0': '待付（未收款）',
  '3': '已取消',
  '6': '退款中（待确认）',
  pending: '待付（未收款）',
  cancelled: '已取消',
  canceled: '已取消',
  making: '制作中（未收款）',
  delivering: '配送中（未收款）',
  refunding: '退款中（待确认）'
}

function cleanOrder(raw: RawOrder): CleanOrder {
  const amount = toNumber(raw.totalAmount)
  const methodRaw = (raw.paymentMethod ?? '').trim()
  const statusRaw = String(raw.status ?? '').trim()
  const statusLower = statusRaw.toLowerCase()

  let statusKey: OrderStatusKey = 'anomaly'
  let anomalyReason = ''
  let statusText = ''

  const isPaid = PAID_STATUS.has(statusRaw) || PAID_STATUS.has(statusLower)
  const isRefunded = REFUNDED_STATUS.has(statusRaw) || REFUNDED_STATUS.has(statusLower)

  if (!methodRaw) {
    statusKey = 'anomaly'
    anomalyReason = '未记录支付流水'
  } else if (amount <= 0) {
    statusKey = 'anomaly'
    anomalyReason = '金额异常（≤ 0）'
  } else if (isPaid) {
    statusKey = 'paid'
    statusText = '已支付'
  } else if (isRefunded) {
    statusKey = 'refunded'
    statusText = '已退款'
  } else {
    statusKey = 'anomaly'
    anomalyReason = ANOMALY_STATUS_TEXT[statusRaw] ?? ANOMALY_STATUS_TEXT[statusLower] ?? `未知状态（${statusRaw || '空'}）`
  }

  return {
    orderNumber: raw.orderNumber ?? '',
    createTime: raw.createTime ?? '',
    amount,
    paymentMethod: methodRaw,
    methodKey: paymentKey(methodRaw) || 'unknown',
    statusKey,
    statusText: statusText || anomalyReason,
    anomalyReason
  }
}

const cleanedOrders = computed<CleanOrder[]>(() => rawOrders.value.map(cleanOrder))
const validOrders = computed<CleanOrder[]>(() =>
  cleanedOrders.value.filter(o => o.statusKey === 'paid' || o.statusKey === 'refunded')
)
const paidOrders = computed<CleanOrder[]>(() => cleanedOrders.value.filter(o => o.statusKey === 'paid'))
const refundedOrders = computed<CleanOrder[]>(() => cleanedOrders.value.filter(o => o.statusKey === 'refunded'))
const anomalyOrders = computed<CleanOrder[]>(() => cleanedOrders.value.filter(o => o.statusKey === 'anomaly'))

const sumBy = (list: CleanOrder[], predicate?: (o: CleanOrder) => boolean): number =>
  list.reduce((sum, o) => (predicate && !predicate(o) ? sum : sum + o.amount), 0)

const isCash = (o: CleanOrder): boolean => o.methodKey === 'cash'

/** 现金收入（已支付中的现金） */
const cashIn = computed(() => sumBy(paidOrders.value, isCash))
/** 现金退款（已退款中的现金） */
const cashRefund = computed(() => sumBy(refundedOrders.value, isCash))
/** 现金净额 = 现金收入 − 现金退款（可为负：退款大于收款） */
const cashNet = computed(() => cashIn.value - cashRefund.value)
/**
 * 应上交现金：现金净额为正才上交，负数一律记 0。
 * 理由：退款大于收款时，缺口是从钱箱（备用金）垫出去的，
 * 收银员手里没有可交的现金，不能显示成"要交 -75 元"。
 */
const handoverCash = computed(() => Math.max(0, cashNet.value))
/** 备用金缺口：本班次垫付掉的钱，需要店长补回，避免下一班零钱不够 */
const floatGap = computed(() => Math.max(0, -cashNet.value))
/**
 * 短款态（现金净额为负）：主结论必须换成"需补足"。
 * 否则 ¥0.00 会让人误以为"不用交钱"，而实际欠公司一笔（退款从备用金里垫出去了）。
 */
const isCashShort = computed(() => cashNet.value < 0)

/**
 * 钱箱应有现金 = 备用金 + 现金收入 − 现金退款。
 * 这是"验算口径"：收银员/店长点完钱，拿实际金额跟它比，差值就是长短款。
 * 注意它 ≠ 应上交现金（应上交 = 钱箱应有 − 备用金，负数按 0 交）。
 */
const drawerExpectedCents = computed(() =>
  toCents(openingCash.value) + toCents(cashIn.value) - toCents(cashRefund.value)
)
/** 渲染用（元）：AnimatedAmount 接收的是元 */
const drawerExpected = computed(() => drawerExpectedCents.value / 100)

/** 实点现金输入（字符串，保持"用户正在输入"的状态；空 = 还没点） */
const cashCountedInput = ref('')
/** 实点现金（单位：分） */
const cashCounted = computed<number | null>(() => {
  const raw = cashCountedInput.value.trim()
  if (!raw) return null
  const value = Number(raw)
  return Number.isFinite(value) ? toCents(value) : null
})
/** 长短款（单位：分）：正数=长款（多钱），负数=短款（少钱） */
const cashDiff = computed<number | null>(() =>
  cashCounted.value === null ? null : cashCounted.value - drawerExpectedCents.value
)
const cashDiffClass = computed(() => {
  if (cashDiff.value === null) return 'is-empty'
  if (cashDiff.value === 0) return 'is-even'
  return cashDiff.value > 0 ? 'is-over' : 'is-short'
})
const cashDiffText = computed(() => {
  if (cashDiff.value === null) return '待点钞'
  if (cashDiff.value === 0) return '账实相符'
  const amount = Math.abs(cashDiff.value / 100).toFixed(2)
  return cashDiff.value > 0 ? `长款 ¥${amount}` : `短款 ¥${amount}`
})
/** 输入失焦后统一成两位小数，避免 '300' 与 '300.00' 两种显示 */
const normalizeCounted = (): void => {
  if (cashCounted.value === null) {
    cashCountedInput.value = ''
    return
  }
  cashCountedInput.value = (cashCounted.value / 100).toFixed(2)
}

/** 净营业额 = 已支付 − 已退款（全部支付方式） */
const netRevenue = computed(() => sumBy(paidOrders.value) - sumBy(refundedOrders.value))
/** 退款总额（所有支付方式） */
const refundTotal = computed(() => sumBy(refundedOrders.value))
/** 客单价：按已支付订单计 */
const avgTicket = computed(() =>
  paidOrders.value.length > 0 ? sumBy(paidOrders.value) / paidOrders.value.length : 0
)

/**
 * 非现金收入卡片：微信 / 支付宝 / 银行卡常显（0 也显示，收银员需要"确认没有"），
 * 会员余额只有发生额时才出现；全部标注「已直连入账，无需上交」。
 */
const NON_CASH_BASE = [
  { key: 'wechat', name: '微信', icon: ChatDotRound },
  { key: 'alipay', name: '支付宝', icon: Wallet },
  { key: 'card', name: '银行卡', icon: CreditCard }
]

const nonCashCards = computed(() => {
  const build = (c: { key: string; name: string; icon: unknown }) => {
    const income = sumBy(paidOrders.value, o => o.methodKey === c.key)
    const refund = sumBy(refundedOrders.value, o => o.methodKey === c.key)
    return { ...c, income, refund, net: income - refund, isNetRefund: income - refund < 0 }
  }
  const cards = NON_CASH_BASE.map(build)
  const balance = build({ key: 'balance', name: '会员余额', icon: Postcard })
  if (balance.income !== 0 || balance.refund !== 0) cards.push(balance)
  return cards
})

/** 支付方式分布：按已支付 − 已退款（净额） */
const paymentRows = computed<PaymentRow[]>(() => {
  const totals: Record<string, number> = {}
  paidOrders.value.forEach(o => {
    totals[o.methodKey] = (totals[o.methodKey] || 0) + o.amount
  })
  refundedOrders.value.forEach(o => {
    totals[o.methodKey] = (totals[o.methodKey] || 0) - o.amount
  })

  const baseKeys = ['cash', 'wechat', 'alipay']
  const keys = Array.from(new Set([...baseKeys, ...Object.keys(totals)]))
  const denominator = Math.max(1, Object.values(totals).reduce((sum, v) => sum + Math.max(0, v), 0))

  return keys
    .map(method => {
      const amount = totals[method] || 0
      return {
        method,
        name: getPaymentMethodName(method),
        amount,
        percentage: (Math.max(0, amount) / denominator) * 100
      }
    })
    .filter(row => baseKeys.includes(row.method) || row.amount !== 0)
})

const anomalySummary = computed<string>(() => {
  const counter: Record<string, number> = {}
  anomalyOrders.value.forEach(o => {
    const key = o.anomalyReason.replace(/（.*?）/g, '')
    counter[key] = (counter[key] || 0) + 1
  })
  return Object.entries(counter)
    .map(([reason, count]) => `${reason} ${count} 笔`)
    .join(' · ')
})

const filteredValidOrders = computed<CleanOrder[]>(() => {
  if (!filterPaymentMethod.value) return validOrders.value
  return validOrders.value.filter(o => o.methodKey === filterPaymentMethod.value)
})

const shiftStatusText = computed<string>(() => {
  if (summaryLoading.value && !shiftActive.value) return '正在读取班次状态…'
  if (shiftActive.value) {
    const operator = shiftInfo.value?.employeeName || shiftInfo.value?.employeeId || '当前收银员'
    return `班次进行中：${operator} · 开始于 ${formatTime(shiftInfo.value?.startTime || summaryStartTime.value, '--:--')}`
  }
  return `当前无进行中的班次，确认交班将按今日汇总（${formatTime(summaryStartTime.value, '--:--')} 起）留痕`
})

// ---- 数据加载 ----
const normalizeOrders = (payload: unknown): RawOrder[] => {
  if (Array.isArray(payload)) return payload as RawOrder[]
  if (payload && typeof payload === 'object') {
    const holder = payload as { records?: unknown; data?: unknown; list?: unknown }
    for (const candidate of [holder.records, holder.data, holder.list]) {
      if (Array.isArray(candidate)) return candidate as RawOrder[]
    }
  }
  return []
}

const resolveErrorMessage = (error: unknown, fallback: string): string => {
  if (typeof error === 'string' && error.trim()) return error
  if (error && typeof error === 'object') {
    const err = error as {
      message?: unknown
      originalError?: { response?: { data?: { message?: unknown } } }
    }
    const backendMessage = err.originalError?.response?.data?.message
    if (typeof backendMessage === 'string' && backendMessage.trim()) return backendMessage
    if (typeof err.message === 'string' && err.message.trim()) return err.message
  }
  return fallback
}

const TERMINAL_STORAGE_KEY = 'pos-terminal'

const resolveTerminalId = (): string => {
  const stored = (localStorage.getItem(TERMINAL_STORAGE_KEY) || '').trim()
  if (stored) return stored
  try {
    const rawUser = localStorage.getItem('pos-user')
    if (rawUser) {
      const userInfo = JSON.parse(rawUser) as { terminalId?: string } | null
      const fromUser = (userInfo?.terminalId || '').trim()
      if (fromUser) {
        localStorage.setItem(TERMINAL_STORAGE_KEY, fromUser)
        return fromUser
      }
    }
  } catch {
    // 解析失败走兜底
  }
  const fallback = 'pos-default'
  localStorage.setItem(TERMINAL_STORAGE_KEY, fallback)
  return fallback
}

/**
 * 终端标识硬校验：交班/班次统计全部依赖 terminalId，
 * 一旦为空或落到 'pos-default' 兜底值，后端会算到"另一个终端"的账上。
 * 这里显式拦住并提示重新登录（登录会用真实终端号覆盖）。
 */
const ensureTerminalId = (): boolean => {
  const value = (terminalId.value || '').trim()
  if (value && value !== 'pos-default') return true
  ElMessage.error('终端标识缺失或不合法，无法交班：请退出登录后重新登录本机')
  logger.warnMessage('[交班] terminalId 不可用：', value || '(empty)')
  return false
}

const loadShiftSummary = async (): Promise<void> => {
  if (!ensureTerminalId()) return
  summaryLoading.value = true
  try {
    const res = (await request.get('/v1/pos/auth/shift/summary', {
      params: { terminalId: terminalId.value }
    })) as unknown as ShiftSummaryResponse | null

    if (res && typeof res === 'object') {
      shiftActive.value = res.shiftActive === true
      shiftInfo.value = res.shiftInfo ?? null
      summaryStartTime.value = res.summaryStartTime || res.shiftInfo?.startTime || ''
      summaryOrderStats.value = res.orderStats ?? null
    }
  } catch (error) {
    logger.warnMessage('[交班] 班次统计加载失败：', resolveErrorMessage(error, '未知错误'))
  } finally {
    summaryLoading.value = false
  }
}

const loadOpeningCash = async (): Promise<void> => {
  const mark = readShiftMark()
  if (mark && typeof mark.openingCash === 'number' && Number.isFinite(mark.openingCash)) {
    openingCash.value = mark.openingCash
    openingCashMissing.value = false
    return
  }
  try {
    const shifts = await posApi.getTodayShifts(terminalId.value)
    const list = Array.isArray(shifts) ? shifts : []
    for (let i = list.length - 1; i >= 0; i -= 1) {
      const value = list[i]?.openingCash
      if (typeof value === 'number' && Number.isFinite(value)) {
        openingCash.value = value
        openingCashMissing.value = false
        return
      }
    }
  } catch (error) {
    logger.warnMessage('[交班] 备用金读取失败：', resolveErrorMessage(error, '未知错误'))
  }
  openingCash.value = 0
  openingCashMissing.value = true
}

const inShiftWindow = (order: RawOrder): boolean => {
  if (!shiftActive.value || !summaryStartTime.value) return true
  const orderTime = parseDateTime(order.createTime)
  const shiftStart = parseDateTime(summaryStartTime.value)
  if (!orderTime || !shiftStart) return true
  return orderTime.getTime() >= shiftStart.getTime()
}

const loadOrders = async (): Promise<void> => {
  loading.value = true
  try {
    const today = formatDate(new Date())
    const result = await request.get('/v1/pos/orders', { params: { startDate: today, endDate: today } })
    rawOrders.value = normalizeOrders(result).filter(inShiftWindow)
  } catch (error) {
    rawOrders.value = []
    logger.warnMessage('[交班] 订单明细加载失败：', resolveErrorMessage(error, '未知错误'))
    ElMessage.warning('订单明细加载失败，营业额请以班次汇总为准')
  } finally {
    loading.value = false
  }
}

const refreshAll = async (): Promise<void> => {
  terminalId.value = resolveTerminalId()
  await loadShiftSummary()
  await Promise.all([loadOrders(), loadOpeningCash()])
}

// ---- 操作 ----
const confirmHandover = async (): Promise<void> => {
  if (endingShift.value) return
  try {
    await ElMessageBox.confirm(
      `确认交班？\n\n应上交现金：¥${handoverCash.value.toFixed(2)}${floatGap.value > 0 ? `\n备用金待补足：¥${floatGap.value.toFixed(2)}` : ''}\n有效订单：${validOrders.value.length} 笔 · 异常单：${anomalyOrders.value.length} 笔\n\n交班后将生成本班次结算记录，需重新开班才能继续收银。`,
      '确认交班',
      {
        confirmButtonText: '确认交班',
        cancelButtonText: '再核对一下',
        type: 'warning',
        distinguishCancelAndClose: true,
        closeOnClickModal: false
      }
    )
  } catch {
    return
  }

  if (!ensureTerminalId()) return

  endingShift.value = true
  try {
    // 传参口径固定：terminalId 必传且与登录时写盘值一致，remark 只做备注
    const res = (await request.post('/v1/pos/auth/shift/end', {
      terminalId: terminalId.value.trim(),
      remark: handoverForm.value.remark.trim()
    })) as unknown as { orderStats?: ShiftOrderStats | null } | null

    const orderStats = res?.orderStats ?? null
    const totalOrders = Math.round(toNumber(orderStats?.total_orders))
    const totalAmount = toNumber(orderStats?.total_amount)

    clearShiftMark()
    ElMessage.success(`交班完成：本班次 ${totalOrders} 笔 / ¥${totalAmount.toFixed(2)}`)
    handoverForm.value.remark = ''
    detailDrawer.value = false
    anomalyDrawer.value = false
    await refreshAll()
    visible.value = false
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '交班失败，请稍后重试'))
  } finally {
    endingShift.value = false
  }
}

const exportReport = async (): Promise<void> => {
  exporting.value = true
  try {
    const nonCash = netRevenue.value - handoverCash.value
    const lines: string[] = []
    lines.push(`交班报表,${currentDate.value} ${currentTime.value}`)
    lines.push(`应上交现金,${handoverCash.value.toFixed(2)}`)
    lines.push(`备用金待补足,${floatGap.value.toFixed(2)}`)
    lines.push(`备用金（留存钱箱）,${openingCash.value.toFixed(2)}`)
    lines.push(`现金收入,${cashIn.value.toFixed(2)}`)
    lines.push(`现金退款,${cashRefund.value.toFixed(2)}`)
    lines.push(`非现金收入（已直连入账无需上交）,${nonCash.toFixed(2)}`)
    lines.push(`净营业额,${netRevenue.value.toFixed(2)}`)
    lines.push(`有效订单,${validOrders.value.length}`)
    lines.push(`异常单,${anomalyOrders.value.length}`)
    lines.push('')
    lines.push('【有效订单】')
    lines.push('时间,订单号,金额,支付方式,状态')
    validOrders.value.forEach(o => {
      lines.push(
        [formatDateTime(o.createTime, ''), o.orderNumber, o.amount.toFixed(2), getPaymentMethodName(o.paymentMethod), o.statusText]
          .map(cell => `"${cell}"`)
          .join(',')
      )
    })
    if (anomalyOrders.value.length > 0) {
      lines.push('')
      lines.push('【异常单（未计入营业额）】')
      lines.push('时间,订单号,金额,支付方式,异常原因')
      anomalyOrders.value.forEach(o => {
        lines.push(
          [formatDateTime(o.createTime, ''), o.orderNumber, o.amount.toFixed(2), getPaymentMethodName(o.paymentMethod), o.anomalyReason]
            .map(cell => `"${cell}"`)
            .join(',')
        )
      })
    }

    const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `交班报表_${currentDate.value}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    logger.warnMessage('[交班] 导出失败：', resolveErrorMessage(error, '未知错误'))
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const printReport = (): void => {
  const w = window.open('', '_blank')
  if (!w) {
    ElMessage.error('无法打开打印窗口')
    return
  }
  const methodsHtml = paymentRows.value
    .filter(row => row.amount > 0)
    .map(row => `<div class="row"><span>${row.name}</span><span>${row.amount.toFixed(2)}</span></div>`)
    .join('')

  w.document.write(`<!DOCTYPE html><html><head><meta charset=utf-8><title>交班小票</title>
<style>body{font-family:'Courier New',monospace;font-size:13px;padding:24px;max-width:320px;margin:0 auto}
h1{text-align:center;font-size:18px;margin-bottom:4px}.sub{text-align:center;color:#888;font-size:11px;margin-bottom:16px}
.hr{border-top:1px dashed #ccc;margin:12px 0}.row{display:flex;justify-content:space-between;padding:3px 0}
.total{font-size:26px;font-weight:bold;text-align:center;color:#e3651c;margin:10px 0}
.small{font-size:11px;color:#666;text-align:center}
.footer{text-align:center;color:#aaa;font-size:11px;margin-top:20px}</style></head><body>
<h1>交班日结</h1><p class="sub">${currentDate.value} ${currentTime.value}</p>
<div class="hr"></div>
<div class="row"><span>应上交现金</span></div>
<div class="total">¥${handoverCash.value.toFixed(2)}</div>
<div class="small">= 现金收入 ${cashIn.value.toFixed(2)} − 现金退款 ${cashRefund.value.toFixed(2)}</div>
<div class="hr"></div>
<div class="row"><span>备用金待补足</span><span>${floatGap.value.toFixed(2)}</span></div>
<div class="row"><span>备用金（留存钱箱）</span><span>${openingCash.value.toFixed(2)}</span></div>
<div class="hr"></div>
<div class="row"><span>净营业额</span><span>${netRevenue.value.toFixed(2)}</span></div>
<div class="row"><span>有效订单</span><span>${validOrders.value.length} 笔</span></div>
<div class="row"><span>异常单</span><span>${anomalyOrders.value.length} 笔</span></div>
<div class="hr"></div>
<h3 style="font-size:14px;margin:8px 0 4px">收款明细（非现金已直连入账，无需上交）</h3>${methodsHtml}
<div class="hr"></div>
${handoverForm.value.remark ? `<div style="color:#666;font-size:12px">备注：${handoverForm.value.remark}</div><div class="hr"></div>` : ''}
<div class="footer"><p>--- 交班完成 ---</p><p>感谢您的辛勤工作！</p></div>
</body></html>`)
  w.document.close()
  w.print()
}

watch(visible, (val) => {
  if (val) {
    void refreshAll()
    updateTime()
    if (timeTimer) clearInterval(timeTimer)
    timeTimer = setInterval(updateTime, 1000)
  } else if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
})

onUnmounted(() => {
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
})
</script>
<style scoped>
/* ==================== 交班日结（第四轮 · 质感重做） ====================
 * 布局原则：一个主角（应上交现金）→ 一个辅助（钱箱核对）→ 一组并列（非现金）
 *           → 一排指标 → 一个入口 → 一条操作栏。
 * 视觉原则：区块用阴影与色阶分区，不用白描边；金额用 DIN/等宽数字；主色只用品牌橙。
 */

.shift-handover-content {
  padding: 0 0 4px;
}

/* 自绘头部，隐藏 el-dialog 的空标题条 */
.shift-handover-dialog :deep(.el-dialog__header) {
  display: none;
}

.shift-handover-dialog :deep(.el-dialog__body) {
  padding: 20px 24px 0;
  max-height: 78vh;
  /* 隐藏滚动条轨道（滚轮/触屏仍可滚动），避免右侧出现一条突兀的白条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.shift-handover-dialog :deep(.el-dialog__body)::-webkit-scrollbar {
  width: 0;
  height: 0;
}

/* ====== 头部 ====== */
.sh-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sh-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sh-brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--pos-primary-gradient, linear-gradient(135deg, #e3651c, #ef7f3a));
  color: #ffffff;
  font-size: 19px;
  box-shadow: 0 6px 16px rgba(227, 101, 28, 0.28);
}

.sh-title-group h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--pos-text-primary, #1e293b);
  line-height: 1.25;
}

.sh-subtitle {
  font-size: 10.5px;
  color: var(--pos-text-muted, #94a3b8);
  letter-spacing: 1.6px;
  text-transform: uppercase;
}

.sh-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--pos-text-secondary, #64748b);
  font-size: 12.5px;
}

.sh-shift-chip {
  padding: 4px 10px;
  border-radius: var(--pos-radius-full, 9999px);
  font-size: 11.5px;
  font-weight: 600;
  color: var(--pos-text-secondary, #64748b);
  background: var(--pos-bg-tertiary, #f1f5f9);
}

.sh-shift-chip.active {
  color: #047857;
  background: rgba(16, 185, 129, 0.14);
}

.sh-time {
  font-family: var(--pos-font-number, monospace);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.sh-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-left: 2px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--pos-text-muted, #94a3b8);
  font-size: 15px;
  cursor: pointer;
  transition: background var(--pos-transition-fast, 150ms ease);
}

.sh-close:hover {
  background: var(--pos-bg-hover, #e2e8f0);
  color: var(--pos-text-primary, #1e293b);
}

/* ====== 异常单提示条 ======
 * 和"异常单待处理"抽屉使用同一套 warning 令牌：
 * 同一个概念只有一种颜色，禁止"条是红的、抽屉是黄的"这种前后不一致。
 */
.sh-alert {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  margin-bottom: var(--pos-spacing-sm, 16px);
  padding: 10px 14px;
  border-radius: var(--pos-radius-md, 12px);
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  font-size: 12.5px;
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(194, 134, 31, 0.3));
}

.sh-alert__icon {
  font-size: 16px;
  flex-shrink: 0;
}

.sh-alert__text {
  flex: 1;
  min-width: 0;
}

.sh-alert__text b {
  font-variant-numeric: tabular-nums;
}

.sh-alert__btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--pos-radius-sm, 8px);
  border: 1px solid var(--pos-warning-border, rgba(194, 134, 31, 0.4));
  background: transparent;
  color: var(--pos-warning-text, #8a6114);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease);
}

.sh-alert__btn:hover {
  background: var(--pos-warning-border, rgba(194, 134, 31, 0.16));
}

/* ====== 主结论：应上交 / 需补足 ======
 * 两态：现金净额 ≥ 0 → 「应上交现金」；< 0 → 「需补足（已从备用金垫付）」。
 * 短款时大数字就是"欠公司的钱"，避免 ¥0.00 被读成"不用交"。
 */
.sh-hero {
  display: flex;
  flex-direction: column;
  gap: var(--pos-spacing-sm, 16px);
  margin-bottom: var(--pos-spacing-sm, 16px);
  padding: 14px 20px 16px;
  border-radius: var(--pos-radius-lg, 16px);
  background: var(--pos-bg-secondary, #f8fafc);
  border-left: 3px solid var(--pos-primary, #e3651c);
  box-shadow: inset 0 0 0 1px var(--pos-border-light, #f1f5f9);
}

/* 短款态只换"边条 + 金额颜色"，不刷满屏红底：财务界面要克制，红色留给真正的异常 */
.sh-hero.is-short {
  border-left-color: var(--pos-danger, #c94b4b);
  box-shadow: inset 0 0 0 1px rgba(201, 75, 75, 0.16);
}

.sh-hero__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--pos-spacing-sm, 16px);
}

.sh-hero__label {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--pos-text-secondary, #64748b);
}

.sh-hero__value {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', monospace);
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: var(--pos-primary, #e3651c);
}

.sh-hero__value.is-danger {
  color: var(--pos-danger, #c94b4b);
}

/* 三个数字块：标签 + 数字，不再让用户读一整句算式 */
.sh-tiles {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--pos-spacing-xs, 8px);
}

.sh-tile {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-bg-primary, #ffffff);
  box-shadow: inset 0 0 0 1px var(--pos-border-light, #f1f5f9);
}

.sh-tile__label {
  font-size: 11.5px;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-tile__value::before {
  content: '¥';
  margin-right: 2px;
  font-size: 0.78em;
  opacity: 0.8;
}

.sh-tile__value {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-size: 17px;
  font-weight: 700;
  color: var(--pos-text-primary, #1e293b);
  font-variant-numeric: tabular-nums;
}

.sh-tile__value.is-refund {
  color: var(--pos-danger, #c94b4b);
}

.sh-hero__hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 8px 12px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-bg-primary, #ffffff);
  box-shadow: inset 0 0 0 1px rgba(201, 75, 75, 0.18);
  color: var(--pos-danger, #c94b4b);
  font-size: 12.5px;
  line-height: 1.5;
}

/* ====== 钱箱核对面板：左"该有多少"，右"点出多少" ====== */
.sh-cashcheck {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--pos-spacing-md, 24px);
  margin-bottom: var(--pos-spacing-sm, 16px);
  padding: 14px 20px;
  border-radius: var(--pos-radius-lg, 16px);
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: inset 0 0 0 1px var(--pos-border-light, #f1f5f9);
}

.sh-cashcheck__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sh-cashcheck__label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--pos-text-secondary, #64748b);
}

.sh-cashcheck__value {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--pos-text-primary, #1e293b);
  font-variant-numeric: tabular-nums;
}

.sh-cashcheck__sub {
  font-size: 11.5px;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-cashcheck__right {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
}

/* 实点输入：给它"要动手"的视觉权重 */
.sh-count-input {
  width: 168px;
}

.sh-count-input :deep(.el-input__wrapper) {
  height: 44px;
  border-radius: var(--pos-radius-sm, 8px);
  box-shadow: inset 0 0 0 1.5px var(--pos-border-color, #e2e8f0);
  background: var(--pos-bg-primary, #ffffff);
}

.sh-count-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1.5px var(--pos-primary, #e3651c);
}

.sh-count-input :deep(.el-input__inner) {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-size: 19px;
  font-weight: 700;
  text-align: right;
  color: var(--pos-text-primary, #1e293b);
  font-variant-numeric: tabular-nums;
}

.is-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
  border: 0;
}

.sh-cc-diff {
  flex: 0 0 auto;
  padding: 3px 10px;
  border-radius: var(--pos-radius-full, 9999px);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.sh-cc-diff.is-empty {
  background: var(--pos-bg-tertiary, #f1f5f9);
  color: var(--pos-text-muted, #94a3b8);
}

.sh-cc-diff.is-even {
  background: var(--pos-success-soft, #ecf7f2);
  color: var(--pos-success, #059669);
  box-shadow: inset 0 0 0 1px rgba(5, 150, 105, 0.22);
}

.sh-cc-diff.is-over {
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(194, 134, 31, 0.3));
}

.sh-cc-diff.is-short {
  background: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
  box-shadow: inset 0 0 0 1px rgba(201, 75, 75, 0.24);
}

.sh-hero__op {
  margin: 0 4px;
  font-weight: 700;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-hero__warn {
  padding: 1px 8px;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  font-size: 11px;
  font-weight: 600;
}

/* 现金净流出：只说"谁来补"，不再重复金额（上面算式已经写了） */
.sh-hero__negative {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 8px 12px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
  font-size: 12.5px;
}

/* ====== 非现金收入 ====== */
.sh-noncash {
  margin-bottom: 14px;
}

.sh-noncash__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.sh-noncash__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--pos-text-primary, #1e293b);
}

.sh-noncash__tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: var(--pos-radius-full, 9999px);
  background: rgba(5, 150, 105, 0.12);
  color: #047857;
  font-size: 11.5px;
  font-weight: 600;
}

.sh-noncash__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.sh-nc-card {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 13px;
  border-radius: 14px;
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: var(--pos-shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.06));
  transition: transform var(--pos-transition-fast, 150ms ease), box-shadow var(--pos-transition-fast, 150ms ease);
}

.sh-nc-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--pos-shadow-md, 0 6px 18px rgba(0, 0, 0, 0.1));
}

.sh-nc-card__head {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--pos-text-secondary, #64748b);
}

.sh-nc-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  font-size: 14px;
  color: #ffffff;
  background: #16a34a;
}

.sh-nc-card[data-test='shift-noncash-alipay'] .sh-nc-card__icon {
  background: #2563eb;
}

.sh-nc-card[data-test='shift-noncash-card'] .sh-nc-card__icon {
  background: #7c3aed;
}

.sh-nc-card[data-test='shift-noncash-balance'] .sh-nc-card__icon {
  background: #0891b2;
}

.sh-nc-card__flag {
  margin-right: 6px;
  padding: 1px 7px;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  vertical-align: 2px;
}

.sh-nc-card__amount.is-refund {
  color: var(--pos-danger, #c94b4b);
}

.sh-nc-card__detail {
  font-size: 11.5px;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-nc-card__amount {
  font-family: var(--pos-font-number, 'DIN Alternate', monospace);
  font-size: 22px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--pos-text-primary, #1e293b);
}

.sh-nc-card__hint {
  font-size: 11px;
  color: var(--pos-text-muted, #94a3b8);
}

/* ====== 次要指标 ====== */
.sh-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.sh-mini-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 13px;
  border-radius: 12px;
  background: var(--pos-bg-primary, #ffffff);
  box-shadow: var(--pos-shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.06));
}

.sh-ms-label {
  font-size: 11.5px;
  color: var(--pos-text-secondary, #64748b);
}

.sh-ms-value {
  font-family: var(--pos-font-number, 'DIN Alternate', monospace);
  font-size: 20px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--pos-text-primary, #1e293b);
}

.sh-ms-value.is-danger {
  color: var(--pos-danger, #dc2626);
}

.sh-ms-pct {
  font-size: 10.5px;
  color: var(--pos-text-muted, #94a3b8);
}

/* ====== 明细入口 ====== */
.sh-detail-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 48px;
  margin-bottom: 14px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: inset 0 0 0 1px var(--pos-border-light, #f1f5f9);
  color: var(--pos-text-primary, #1e293b);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--pos-transition-fast, 150ms ease);
}

.sh-detail-btn:hover {
  background: #fff7ed;
  box-shadow: inset 0 0 0 1px rgba(227, 101, 28, 0.35);
  color: var(--pos-primary, #e3651c);
}

.sh-detail-btn__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(227, 101, 28, 0.12);
  color: var(--pos-primary, #e3651c);
  font-size: 16px;
}

.sh-detail-btn__text {
  flex: 1;
  text-align: left;
}

.sh-detail-btn__arrow {
  font-size: 14px;
  color: var(--pos-text-muted, #94a3b8);
}

/* ====== 操作栏 ====== */
.sh-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 0 12px;
  border-top: 1px solid var(--pos-border-light, #f1f5f9);
}

.sh-remark-input {
  width: 100%;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid var(--pos-border-color, #e2e8f0);
  background: var(--pos-bg-secondary, #f8fafc);
  color: var(--pos-text-primary, #1e293b);
  font-size: 13px;
  transition: border-color var(--pos-transition-fast, 150ms ease);
}

.sh-remark-input:focus {
  outline: none;
  border-color: var(--pos-primary, #e3651c);
  background: var(--pos-bg-primary, #ffffff);
}

.sh-btn-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.sh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--pos-transition-fast, 150ms ease);
}

.sh-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.sh-btn-ghost {
  border: none;
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: inset 0 0 0 1px var(--pos-border-color, #e2e8f0);
  color: var(--pos-text-primary, #1e293b);
}

.sh-btn-ghost:hover:not(:disabled) {
  color: var(--pos-primary, #e3651c);
  box-shadow: inset 0 0 0 1px rgba(227, 101, 28, 0.4);
}

.sh-btn-primary {
  min-width: 148px;
  border: none;
  background: var(--pos-primary-gradient, linear-gradient(135deg, #e3651c, #ef7f3a));
  color: #ffffff;
  box-shadow: 0 8px 20px rgba(227, 101, 28, 0.28);
}

.sh-btn-primary:hover:not(:disabled) {
  filter: brightness(1.05);
  box-shadow: 0 10px 24px rgba(227, 101, 28, 0.34);
}

.sh-shift-note {
  font-size: 11.5px;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-shift-note.active {
  color: var(--pos-success, #059669);
}

/* ====== 抽屉 ====== */
.sh-drawer {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
}

.sh-drawer-summary {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--pos-bg-tertiary, #f1f5f9);
  font-size: 12.5px;
  color: var(--pos-text-primary, #1e293b);
}

.sh-drawer-summary b {
  font-family: var(--pos-font-number, monospace);
  font-variant-numeric: tabular-nums;
  color: var(--pos-primary, #e3651c);
}

.sh-section {
  padding: 14px;
  border-radius: 12px;
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: var(--pos-shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.05));
}

.sh-section-header {
  margin-bottom: 10px;
}

.sh-section-title {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--pos-text-primary, #1e293b);
}

.sh-payment-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sh-bar-row {
  display: grid;
  grid-template-columns: 52px 1fr 86px 52px;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.sh-bar-name {
  color: var(--pos-text-secondary, #64748b);
}

.sh-bar-track {
  height: 8px;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-bg-tertiary, #f1f5f9);
  overflow: hidden;
}

.sh-bar-fill {
  height: 100%;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-primary, #e3651c);
  transition: width var(--pos-transition-normal, 250ms ease);
}

.sh-bar-fill.bar-wechat { background: #16a34a; }
.sh-bar-fill.bar-alipay { background: #2563eb; }
.sh-bar-fill.bar-card { background: #7c3aed; }
.sh-bar-fill.bar-balance { background: #0891b2; }

.sh-bar-val {
  text-align: right;
  font-family: var(--pos-font-number, monospace);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: var(--pos-text-primary, #1e293b);
}

.sh-bar-pct {
  text-align: right;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-bar-empty {
  padding: 8px 0;
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
}

.sh-drawer-filter {
  display: flex;
  justify-content: flex-end;
}

.sh-anomaly-tip {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--pos-radius-md, 12px);
  /* 统一走预警语义令牌：浅色深字、深色浅字，不再两套硬编码 */
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(194, 134, 31, 0.3));
  font-size: 12.5px;
  line-height: 1.65;
}

.sh-order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  flex: 1;
  padding-right: 2px;
  scrollbar-width: thin;
  scrollbar-color: var(--pos-border-color, #e2e8f0) transparent;
}

.sh-order-list::-webkit-scrollbar {
  width: 6px;
}

.sh-order-list::-webkit-scrollbar-thumb {
  background: var(--pos-border-color, #e2e8f0);
  border-radius: 9999px;
}

/* 每条订单一张卡片：上行「时间 / 单号 / 状态」，下行「支付方式 / 金额」 */
.sh-order-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: var(--pos-shadow-sm, 0 2px 8px rgba(0, 0, 0, 0.05));
  transition: transform var(--pos-transition-fast, 150ms ease), box-shadow var(--pos-transition-fast, 150ms ease);
}

.sh-order-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--pos-shadow-md, 0 6px 18px rgba(0, 0, 0, 0.1));
}

.sh-order-item.is-anomaly {
  background: var(--pos-warning-soft, #fbf5e8);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(194, 134, 31, 0.3));
}

.sh-o-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sh-o-row--top {
  font-size: 13px;
}

.sh-o-row--bottom {
  justify-content: space-between;
}

.sh-o-time {
  font-size: 14px;
  font-weight: 600;
  color: var(--pos-text-primary, #1e293b);
  font-family: var(--pos-font-number, monospace);
  font-variant-numeric: tabular-nums;
}

.sh-o-no {
  color: var(--pos-text-muted, #94a3b8);
  font-family: var(--pos-font-number, monospace);
  font-size: 12.5px;
}

.sh-o-amount {
  font-size: 19px;
  font-weight: 800;
  font-family: var(--pos-font-number, monospace);
  font-variant-numeric: tabular-nums;
  color: var(--pos-text-primary, #1e293b);
}

.sh-o-method {
  font-size: 13px;
  color: var(--pos-text-secondary, #64748b);
}

.sh-o-status {
  margin-left: auto;
  padding: 2px 10px;
  border-radius: var(--pos-radius-full, 9999px);
  font-size: 11.5px;
  font-weight: 600;
  background: rgba(5, 150, 105, 0.12);
  color: #047857;
}

.sh-o-status.s-refunded {
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

/* 异常原因与"待付"状态共用同一套预警令牌：同一个概念只有一种颜色 */
.sh-o-reason {
  margin-left: auto;
  padding: 2px 10px;
  border-radius: var(--pos-radius-full, 9999px);
  font-size: 11.5px;
  font-weight: 600;
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(194, 134, 31, 0.3));
}

.sh-o-status.s-anomaly,
.sh-o-status.s-pending {
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(194, 134, 31, 0.3));
}

.sh-orders-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 13px;
  color: var(--pos-text-muted, #94a3b8);
}

/* ==================== 深色模式：深灰 + 柔和灰白 + 去白框 ==================== */
[data-theme='dark'] .sh-hero {
  background: var(--pos-bg-secondary, #2c2c2e);
  border-left-color: var(--pos-primary, #ef7f3a);
  box-shadow: inset 0 0 0 1px var(--pos-border-color, #3a3a3e);
}

[data-theme='dark'] .sh-cc-diff.is-empty {
  background: var(--pos-bg-tertiary, #353538);
  color: var(--pos-text-muted, #85858c);
}

[data-theme='dark'] .sh-hero__value {
  color: var(--pos-primary, #ef7f3a);
  text-shadow: none;
}

/* 深色下短款金额必须是危险色（主题覆盖优先级要压过基础规则） */
[data-theme='dark'] .sh-hero__value.is-danger {
  color: var(--pos-danger, #d96b6b);
}

[data-theme='dark'] .sh-cc-item,
[data-theme='dark'] .sh-cc-count {
  color: var(--pos-text-secondary, #b0b0b5);
}

[data-theme='dark'] .sh-cc-item b {
  color: var(--pos-text-primary, #e5e5e5);
}

[data-theme='dark'] .sh-hero__negative {
  background: var(--pos-danger-soft, #332426);
  color: var(--pos-danger, #d96b6b);
}

[data-theme='dark'] .sh-order-item {
  background: var(--pos-bg-secondary, #26262a);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

[data-theme='dark'] .sh-order-item.is-anomaly {
  background: rgba(217, 119, 6, 0.14);
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, 0.25);
}

[data-theme='dark'] .sh-nc-card,
[data-theme='dark'] .sh-mini-stat,
[data-theme='dark'] .sh-section {
  background: var(--pos-bg-secondary, #26262a);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.35);
}

[data-theme='dark'] .sh-detail-btn {
  background: var(--pos-bg-secondary, #26262a);
  box-shadow: inset 0 0 0 1px var(--pos-border-color, #35353b);
  color: var(--pos-text-primary, #ececee);
}

[data-theme='dark'] .sh-detail-btn:hover {
  background: #2e2e33;
  color: #fb923c;
  box-shadow: inset 0 0 0 1px rgba(239, 127, 58, 0.4);
}

[data-theme='dark'] .sh-btn-ghost {
  background: var(--pos-bg-secondary, #26262a);
  box-shadow: inset 0 0 0 1px var(--pos-border-color, #35353b);
  color: var(--pos-text-primary, #ececee);
}

[data-theme='dark'] .sh-remark-input {
  background: var(--pos-bg-secondary, #26262a);
  border-color: var(--pos-border-color, #35353b);
  color: var(--pos-text-primary, #ececee);
}



[data-theme='dark'] .sh-anomaly-tip {
  background: var(--pos-warning-soft, #33291a);
  color: var(--pos-warning-text, #e7bd6b);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(231, 189, 107, 0.32));
}

[data-theme='dark'] .sh-order-item:hover {
  background: var(--pos-bg-tertiary, #2e2e33);
}

[data-theme='dark'] .sh-order-item.is-anomaly {
  background: var(--pos-warning-soft, #33291a);
  box-shadow: inset 0 0 0 1px var(--pos-warning-border, rgba(231, 189, 107, 0.32));
}

[data-theme='dark'] .sh-drawer-summary {
  background: var(--pos-bg-tertiary, #2e2e33);
  color: var(--pos-text-primary, #ececee);
}

/* ====== 窄屏 ====== */
@media (max-width: 1200px) {
  .sh-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sh-noncash__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sh-hero__value {
    font-size: 34px;
  }
}
</style>
