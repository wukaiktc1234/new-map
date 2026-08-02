<template>
  <el-dialog
    v-model="visible"
    title=""
    width="880px"
    class="shift-handover-dialog"
    :close-on-click-modal="false"
    :lock-scroll="false"
  >
    <div class="shift-handover-content">
      <!-- 头部 -->
      <div class="sh-header">
        <div class="sh-brand">
          <span class="sh-icon">📋</span>
          <div class="sh-title-group">
            <h2>交班日结</h2>
            <span class="sh-subtitle">Shift Handover Report</span>
          </div>
        </div>
        <div class="sh-meta">
          <span class="sh-date">{{ currentDate }}</span>
          <span class="sh-time">{{ currentTime }}</span>
        </div>
      </div>

      <!-- 核心数据区 -->
      <div class="sh-main-stats">
        <div class="sh-total-card">
          <div class="sh-total-label">今日营业额</div>
          <div class="sh-total-value">¥{{ stats.totalAmount.toFixed(2) }}</div>
          <div class="sh-total-sub">共 {{ stats.totalOrders }} 笔订单 · 客单价 ¥{{ stats.avgAmount.toFixed(2) }}</div>
        </div>
        <div class="sh-stat-grid">
          <div class="sh-mini-stat">
            <span class="sh-ms-label">现金收入</span>
            <span class="sh-ms-value cash">¥{{ (stats.paymentMethodBreakdown.cash || 0).toFixed(2) }}</span>
          </div>
          <div class="sh-mini-stat">
            <span class="sh-ms-label">微信收款</span>
            <span class="sh-ms-value wechat">¥{{ (stats.paymentMethodBreakdown.wechat || 0).toFixed(2) }}</span>
          </div>
          <div class="sh-mini-stat">
            <span class="sh-ms-label">支付宝</span>
            <span class="sh-ms-value alipay">¥{{ (stats.paymentMethodBreakdown.alipay || 0).toFixed(2) }}</span>
          </div>
          <div class="sh-mini-stat" v-if="stats.refundCount > 0">
            <span class="sh-ms-label">退款</span>
            <span class="sh-ms-value refund">-¥{{ stats.refundAmount.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 支付分布 -->
      <div class="sh-section">
        <div class="sh-section-header">
          <span class="sh-section-title">支付方式分布</span>
        </div>
        <div class="sh-payment-bars">
          <div
            v-for="(amount, method) in stats.paymentMethodBreakdown"
            :key="method"
            class="sh-bar-row"
          >
            <span class="sh-bar-name">{{ getPaymentMethodName(method) }}</span>
            <div class="sh-bar-track">
              <div
                class="sh-bar-fill"
                :class="'bar-' + method"
                :style="{ width: getPaymentPercentage(amount) + '%' }"
              ></div>
            </div>
            <span class="sh-bar-val">¥{{ amount.toFixed(2) }}</span>
            <span class="sh-bar-pct">{{ getPaymentPercentage(amount).toFixed(1) }}%</span>
          </div>
        </div>
      </div>

      <!-- 订单明细 -->
      <div class="sh-section sh-orders-section">
        <div class="sh-section-header" @click="toggleOrders">
          <span class="sh-section-title">订单明细</span>
          <span class="sh-section-badge">{{ filteredOrders.length }} 笔</span>
          <span class="sh-toggle-icon">{{ showOrders ? '▲' : '▼' }}</span>
        </div>
        <div v-show="showOrders" class="sh-orders-body">
          <div class="sh-filter-row">
            <el-select
              v-model="filterPaymentMethod"
              placeholder="全部方式"
              clearable
              size="small"
              style="width: 140px"
            >
              <el-option
                v-for="method in paymentMethods"
                :key="method.value"
                :label="method.label"
                :value="method.value"
              />
            </el-select>
          </div>
          <div class="sh-order-list">
            <div
              v-for="(order, idx) in filteredOrders.slice(0, showAllOrders ? undefined : 8)"
              :key="idx"
              class="sh-order-item"
            >
              <span class="sh-o-time">{{ formatOrderTime(order.createTime) }}</span>
              <span class="sh-o-no">#{{ order.orderNumber?.slice(-6) }}</span>
              <span class="sh-o-amount">¥{{ (order.totalAmount || 0).toFixed(2) }}</span>
              <span class="sh-o-method" :class="'m-' + order.paymentMethod">{{ getPaymentMethodName(order.paymentMethod) }}</span>
              <span class="sh-o-status" :class="'s-' + order.status">{{ getStatusText(order.status) }}</span>
            </div>
            <div v-if="filteredOrders.length > 8 && !showAllOrders" class="sh-show-more" @click="showAllOrders = true">
              还有 {{ filteredOrders.length - 8 }} 笔，点击展开
            </div>
          </div>
        </div>
      </div>

      <!-- 操作区 -->
      <div class="sh-actions">
        <div class="sh-remark">
          <input
            v-model="handoverForm.remark"
            type="text"
            class="sh-remark-input"
            placeholder="交班备注（选填）"
            maxlength="100"
          />
        </div>
        <div class="sh-btn-row">
          <button class="sh-btn sh-btn-outline" @click="exportReport" :disabled="exporting">
            📥 导出报表
          </button>
          <button class="sh-btn sh-btn-outline" @click="printReport">
            🖨️ 打印小票
          </button>
          <button
            class="sh-btn sh-btn-confirm"
            @click="confirmHandover"
            :disabled="handovering"
          >
            {{ handovering ? '处理中...' : '确认交班' }}
          </button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface OrderItem {
  orderNumber: string
  createTime: string
  totalAmount: number
  paymentMethod: string
  status: string
}

interface ShiftStats {
  totalOrders: number
  totalAmount: number
  avgAmount: number
  paymentMethodBreakdown: Record<string, number>
  refundCount: number
  refundAmount: number
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'handoverCompleted', data: any): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const orders = ref<OrderItem[]>([])
const loading = ref(false)
const showOrders = ref(true)
const showAllOrders = ref(false)
const filterPaymentMethod = ref<string>('')
const exporting = ref(false)
const handovering = ref(false)
const handoverForm = ref({ remark: '' })

const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval> | null = null

const updateTime = (): void => {
  const now = new Date()
  currentTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
}

const currentDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
})

const paymentMethods = [
  { value: 'cash', label: '现金' },
  { value: 'wechat', label: '微信' },
  { value: 'alipay', label: '支付宝' }
]

const stats = computed<ShiftStats>(() => {
  const todayOrders = orders.value
  const totalOrders = todayOrders.length
  const paidOrders = todayOrders.filter(o => o.status !== 'refunded')
  const totalAmount = paidOrders.reduce((sum, o) => sum + (o.totalAmount || 0), 0)
  const avgAmount = totalOrders > 0 ? totalAmount / totalOrders : 0
  const paymentMethodBreakdown: Record<string, number> = {}
  paidOrders.forEach(order => {
    const method = order.paymentMethod || 'unknown'
    paymentMethodBreakdown[method] = (paymentMethodBreakdown[method] || 0) + (order.totalAmount || 0)
  })
  const refundOrders = todayOrders.filter(o => o.status === 'refunded')
  return {
    totalOrders,
    totalAmount,
    avgAmount,
    paymentMethodBreakdown,
    refundCount: refundOrders.length,
    refundAmount: refundOrders.reduce((sum, o) => sum + (o.totalAmount || 0), 0)
  }
})

const filteredOrders = computed(() => {
  if (!filterPaymentMethod.value) return orders.value
  return orders.value.filter(o => o.paymentMethod === filterPaymentMethod.value)
})

const toggleOrders = (): void => { showOrders.value = !showOrders.value }

const getPaymentMethodName = (method: string): string => {
  const map: Record<string, string> = { cash: '现金', wechat: '微信', alipay: '支付宝' }
  return map[method] || method || '未知'
}

const getPaymentPercentage = (amount: number): number => {
  if (stats.value.totalAmount === 0) return 0
  return (amount / stats.value.totalAmount) * 100
}

const getStatusText = (status: string): string => {
  const map: Record<string, string> = { pending: '待付', paid: '已付', completed: '完成', refunded: '已退', cancelled: '取消' }
  return map[status] || status
}

const formatOrderTime = (time: string): string => {
  if (!time) return '--:--'
  return time.split(' ')[1]?.slice(0, 5) || time.slice(11, 16) || '--:--'
}

const loadTodayOrders = async () => {
  loading.value = true
  try {
    const today = new Date().toISOString().split('T')[0]
    const result = await request.get('/v1/pos/orders', {
      params: { page: 1, size: 100, startDate: today, endDate: today }
    }) as { records: OrderItem[] }
    orders.value = result?.records && Array.isArray(result.records) ? result.records : []
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

const exportReport = async () => {
  exporting.value = true
  try {
    const headers = ['时间', '订单号', '金额', '支付方式', '状态']
    const rows = orders.value.map(o => [
      o.createTime, o.orderNumber, (o.totalAmount || 0).toFixed(2),
      getPaymentMethodName(o.paymentMethod), getStatusText(o.status)
    ])
    const csvContent = [headers.join(','), ...rows.map(row => row.map(cell => `"${cell}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `交班报表_${currentDate.value}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const printReport = () => {
  const w = window.open('', '_blank')
  if (!w) { ElMessage.error('无法打开打印窗口'); return }

  const methodsHtml = Object.entries(stats.value.paymentMethodBreakdown).map(([m, a]) =>
    `<div style="display:flex;justify-content:space-between;padding:4px 0"><span>${getPaymentMethodName(m)}</span><span>${a.toFixed(2)}</span></div>`
  ).join('')

  w.document.write(`<!DOCTYPE html><html><head><meta charset=utf-8><title>交班小票</title>
<style>body{font-family:'Courier New',monospace;font-size:13px;padding:24px;max-width:320px;margin:0 auto}
h1{text-align:center;font-size:18px;margin-bottom:4px}.sub{text-align:center;color:#888;font-size:11px;margin-bottom:16px}
.hr{border-top:1px dashed #ccc;margin:12px 0}.row{display:flex;justify-content:space-between;padding:3px 0}
.total{font-size:22px;font-weight:bold;text-align:center;color:#ea580c;margin:12px 0}
.footer{text-align:center;color:#aaa;font-size:11px;margin-top:20px}</style></head><body>
<h1>交班日结</h1><p class="sub">${currentDate.value} ${currentTime.value}</p>
<div class="hr"></div>
<div class="row"><span>订单总数</span><span><b>${stats.value.totalOrders}</b> 笔</span></div>
<div class="row"><span>总营业额</span></div>
<div class="total">¥${stats.value.totalAmount.toFixed(2)}</div>
<div class="row"><span>平均客单价</span><span>¥${stats.value.avgAmount.toFixed(2)}</span></div>
<div class="hr"></div>
<h3 style="font-size:14px;margin:8px 0 4px">支付明细</h3>${methodsHtml}
${stats.value.refundCount > 0 ? `<div class="hr"></div><div class="row"><span>退款</span><span style="color:red">-${stats.value.refundAmount.toFixed(2)}</span></div>` : ''}
<div class="hr"></div>
${handoverForm.value.remark ? `<div style="color:#666;font-size:12px">备注：${handoverForm.value.remark}</div><div class="hr"></div>` : ''}
<div class="footer"><p>--- 交班完成 ---</p><p>感谢您的辛勤工作！</p></div>
</body></html>`)
  w.document.close()
  w.print()
}

const confirmHandover = async () => {
  try {
    await ElMessageBox.confirm(
      `今日共 ${stats.value.totalOrders} 笔订单\n营业额: ¥${stats.value.totalAmount.toFixed(2)}\n\n确认交班？`,
      '交班确认',
      { confirmButtonText: '确认交班', cancelButtonText: '取消', type: 'warning', distinguishCancelAndClose: true }
    )
    handovering.value = true
    emit('handoverCompleted', {
      date: currentDate.value,
      time: new Date().toLocaleTimeString('zh-CN'),
      stats: stats.value,
      remark: handoverForm.value.remark,
      orderCount: orders.value.length
    })
    ElMessage.success('交班成功！辛苦了！')
    visible.value = false
    handoverForm.value.remark = ''
  } catch (e: unknown) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error('交班失败')
  } finally {
    handovering.value = false
  }
}

watch(visible, (val) => {
  if (val) {
    loadTodayOrders()
    updateTime()
    timeTimer = setInterval(updateTime, 1000)
  } else if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
})
</script>

<style scoped>
.shift-handover-content {
  padding: 4px 0;
}

/* ====== 头部 ====== */
.sh-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.sh-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sh-icon {
  font-size: 28px;
}

.sh-title-group h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}

.sh-subtitle {
  font-size: 11px;
  color: #9ca3af;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.sh-meta {
  text-align: right;
}

.sh-date {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.sh-time {
  font-size: 12px;
  color: #9ca3af;
  font-family: 'Courier New', monospace;
}

/* ====== 核心数据 ====== */
.sh-main-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.sh-total-card {
  flex: 1;
  background: linear-gradient(135deg, #ea580c 0%, #f97316 100%);
  border-radius: 14px;
  padding: 20px 24px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.sh-total-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.sh-total-value {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.sh-total-sub {
  font-size: 12px;
  opacity: 0.75;
  margin-top: 8px;
}

.sh-stat-grid {
  flex: 0 0 260px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.sh-mini-stat {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sh-ms-label {
  font-size: 11px;
  color: #6b7280;
}

.sh-ms-value {
  font-size: 17px;
  font-weight: 700;
}

.sh-ms-value.cash { color: #059669; }
.sh-ms-value.wechat { color: #07c160; }
.sh-ms-value.alipay { color: #1677ff; }
.sh-ms-value.refund { color: #dc2626; }

/* ====== 支付分布 ====== */
.sh-section {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 16px;
}

.sh-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.sh-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.sh-section-badge {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: #ea580c;
  padding: 1px 8px;
  border-radius: 10px;
}

.sh-toggle-icon {
  font-size: 10px;
  color: #9ca3af;
  margin-left: auto;
}

.sh-payment-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.sh-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sh-bar-name {
  font-size: 12px;
  font-weight: 500;
  color: #4b5563;
  min-width: 48px;
  flex-shrink: 0;
}

.sh-bar-track {
  flex: 1;
  height: 18px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.sh-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
  min-width: 4px;
}

.bar-cash { background: linear-gradient(90deg, #059669, #10b981); }
.bar-wechat { background: linear-gradient(90deg, #07c160, #34d399); }
.bar-alipay { background: linear-gradient(90deg, #1677ff, #60a5fa); }

.sh-bar-val {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  min-width: 72px;
  text-align: right;
  font-family: 'SF Mono', Monaco, monospace;
}

.sh-bar-pct {
  font-size: 11px;
  color: #9ca3af;
  min-width: 36px;
  text-align: right;
}

/* ====== 订单明细 ====== */
.sh-orders-body {
  margin-top: 12px;
}

.sh-filter-row {
  margin-bottom: 10px;
}

.sh-order-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sh-order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  transition: background 0.15s;
}

.sh-order-item:hover { background: #f0fdf4; }

.sh-o-time {
  font-family: 'SF Mono', monospace;
  color: #6b7280;
  min-width: 52px;
  flex-shrink: 0;
}

.sh-o-no {
  color: #9ca3af;
  min-width: 56px;
  flex-shrink: 0;
}

.sh-o-amount {
  font-weight: 600;
  color: #1f2937;
  min-width: 64px;
  text-align: right;
  font-family: 'SF Mono', monospace;
}

.sh-o-method {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  min-width: 40px;
  text-align: center;
  flex-shrink: 0;
}

.m-cash { background: #d1fae5; color: #065f46; }
.m-wechat { background: #d1fae5; color: #065f46; }
.m-alipay { background: #dbeafe; color: #1e40af; }

.sh-o-status {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  min-width: 44px;
  text-align: center;
  flex-shrink: 0;
  margin-left: auto;
}

.s-completed, .s-paid { background: #d1fae5; color: #065f46; }
.s-pending { background: #fef3c7; color: #92400e; }
.s-refunded { background: #fee2e2; color: #991b1b; }
.s-cancelled { background: #f3f4f6; color: #6b7280; }

.sh-show-more {
  text-align: center;
  padding: 8px;
  font-size: 12px;
  color: #ea580c;
  cursor: pointer;
  font-weight: 500;
}

/* ====== 操作区 ====== */
.sh-actions {
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.sh-remark {
  margin-bottom: 14px;
}

.sh-remark-input {
  width: 100%;
  height: 38px;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.sh-remark-input:focus {
  border-color: #ea580c;
  box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.08);
}

.sh-remark-input::placeholder { color: #9ca3af; }

.sh-btn-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.sh-btn {
  height: 42px;
  padding: 0 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.sh-btn-outline {
  background: #fff;
  border: 1.5px solid #d1d5db;
  color: #4b5563;
}

.sh-btn-outline:hover:not(:disabled) {
  border-color: #ea580c;
  color: #ea580c;
}

.sh-btn-confirm {
  background: linear-gradient(135deg, #dc2626, #ef4444);
  color: #fff;
  min-width: 120px;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.sh-btn-confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(220, 38, 38, 0.35);
}

.sh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ====== 暗色模式 ====== */
[data-theme='dark'] .sh-header { border-bottom-color: #374151; }
[data-theme='dark'] .sh-title-group h2 { color: #f1f5f9; }
[data-theme='dark'] .sh-date { color: #e2e8f0; }
[data-theme='dark'] .sh-total-card { background: linear-gradient(135deg, #c2410c, #ea580c); }
[data-theme='dark'] .sh-mini-stat { background: #1e293b; }
[data-theme='dark'] .sh-section { background: #1e293b; }
[data-theme='dark'] .sh-section-title { color: #e2e8f0; }
[data-theme='dark'] .sh-bar-track { background: #334155; }
[data-theme='dark'] .sh-bar-val { color: #f1f5f9; }
[data-theme='dark'] .sh-order-item { background: #0f172a; }
[data-theme='dark'] .sh-order-item:hover { background: #1e293b; }
[data-theme='dark'] .sh-o-amount { color: #f1f5f9; }
[data-theme='dark'] .sh-actions { border-top-color: #334155; }
[data-theme='dark'] .sh-remark-input { background: #0f172a; border-color: #334155; color: #e2e8f0; }
[data-theme='dark'] .sh-btn-outline { background: transparent; border-color: #475569; color: #94a3b8; }
</style>