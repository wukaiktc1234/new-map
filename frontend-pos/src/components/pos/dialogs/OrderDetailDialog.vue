<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="订单详情"
    width="650px"
    class="order-detail-dialog"
    :lock-scroll="false"
  >
    <div v-if="order" class="order-detail-content">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="叫号号">
          <span class="call-number-text">{{ order.callNumber || order.pickupNumber || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单号">
          {{ order.orderNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="桌号" v-if="order.tableNumber">
          {{ order.tableNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ formatDateTime(order.createTime, '-') }}
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">
          {{ getPaymentMethodText(order.paymentMethodName || order.paymentMethod || '-') }}
        </el-descriptions-item>
        <el-descriptions-item label="交易流水号">
          {{ formatTransactionId(order.transactionId, order.paymentMethod) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <!-- 状态变化时做一次"翻一下"的过渡（key 变化触发 Transition），避免数字/文字硬切 -->
          <Transition name="status-pop" mode="out-in">
            <span
              :key="getStatusText(order.status)"
              class="status-badge"
              :class="`status-badge--${getStatusClass(order.status)}`"
              data-test="detail-pay-status"
            >
              <i class="status-badge__dot" aria-hidden="true"></i>
              {{ getStatusText(order.status) }}
            </span>
          </Transition>
        </el-descriptions-item>
        <el-descriptions-item label="后厨状态">
          <Transition name="status-pop" mode="out-in">
            <span
              :key="getKitchenStatusText(order)"
              class="status-badge"
              :class="[`status-badge--${getKitchenStatusClass(order)}`, { 'is-live': isKitchenLive(order) }]"
              data-test="detail-kitchen-status"
            >
              <i class="status-badge__dot" aria-hidden="true"></i>
              {{ getKitchenStatusText(order) }}
            </span>
          </Transition>
        </el-descriptions-item>
        <el-descriptions-item label="订单金额">
          <span class="amount-text">¥{{ order.totalAmount?.toFixed(2) || '0.00' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 菜品明细：使用原生 HTML 表格替代 el-table，避免首次渲染时的组件初始化开销 -->
      <div
        v-if="order.items && order.items.length > 0"
        class="order-items-section"
      >
        <div class="section-title">菜品明细</div>
        <table class="items-table">
          <thead>
            <tr>
              <th class="items-table__name">菜品名称</th>
              <th class="items-table__price">单价</th>
              <th class="items-table__qty">数量</th>
              <th class="items-table__subtotal">小计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in order.items" :key="idx">
              <td class="items-table__name">{{ row.name }}</td>
              <td class="items-table__price">¥{{ row.price?.toFixed(2) || '0.00' }}</td>
              <td class="items-table__qty">{{ row.quantity }}</td>
              <td class="items-table__subtotal">¥{{ (row.price * row.quantity)?.toFixed(2) || '0.00' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-else-if="order.dishItems"
        class="order-items-section"
      >
        <div class="section-title">菜品明细</div>
        <table class="items-table">
          <thead>
            <tr>
              <th class="items-table__name">菜品名称</th>
              <th class="items-table__price">单价</th>
              <th class="items-table__qty">数量</th>
              <th class="items-table__subtotal">小计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in parseDishItems(order.dishItems)" :key="idx">
              <td class="items-table__name">{{ row.name }}</td>
              <td class="items-table__price">¥{{ row.price?.toFixed(2) || '0.00' }}</td>
              <td class="items-table__qty">{{ row.quantity }}</td>
              <td class="items-table__subtotal">¥{{ (row.price * row.quantity)?.toFixed(2) || '0.00' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <!-- 退款需要 order:refund 权限：无权限时连入口都不出现 -->
      <el-button
        v-if="order && allowRefund && canRefund(order)"
        type="danger"
        data-test="detail-refund-btn"
        @click="emit('refund', order)"
      >申请退款</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { formatDateTime } from '@/utils/format'
import { getOrderStatus, resolveKitchenStatus } from '@/utils/orderStatus'
import { usePermission } from '@/composables/usePermission'
import logger from '@/utils/logger'

interface Props {
  modelValue: boolean
  order: any | null
}

const props = defineProps<Props>()

const { canRefund: allowRefund, refreshCapabilities } = usePermission()

// 打开弹窗时按当前登录信息重算一次权限（模块级快照可能是登录前初始化的）
watch(() => props.modelValue, (visible) => { if (visible) refreshCapabilities() }, { immediate: true })

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'refund', order: any): void
}>()

/**
 * 退款可用判定（放宽口径）：未支付 / 已取消 / 已退款之外都可发起退款，
 * 不再要求订单「已完成」——顾客刚下单或制作中退单是常态。
 */
function canRefund(order: any): boolean {
  if (!order) return false
  const status = String(order.status || '')
  return !['pending', 'unpaid', 'cancelled', 'refunded', 'partial_refunded'].includes(status)
}

/**
 * 支付方式文案：**直接使用后端给的中文**（POS 列表 / 财务详情同源），
 * 本函数只做"代码 → 中文"的兜底，不再把"现金"改写成"现金支付"。
 * （2026-10-24：三处文案漂移由这里造成，已按统一口径修正）
 */
const getPaymentMethodText = (method: string) => {
  const map: Record<string, string> = {
    cash: '现金',
    wechat: '微信支付',
    alipay: '支付宝',
    card: '银行卡',
    balance: '余额支付'
  }
  return map[method] || method
}

const formatTransactionId = (transactionId: string | undefined, paymentMethod: string | undefined) => {
  if (!transactionId) return '-'
  if (paymentMethod === 'cash' || paymentMethod === '现金' || paymentMethod === '现金支付') return '-'
  return transactionId.replace(/^[A-Z]+/, '')
}

/**
 * 订单状态 → 徽章样式类名
 * 使用原生 span + CSS 类替代 el-tag，避免组件初始化开销
 */
// 状态映射统一走 utils/orderStatus：数字规范码（0/1/2/3/5）也要翻成中文，
// 否则收银员会在「支付状态」里直接看到裸数字 "2"。
const getStatusClass = (status: string) => getOrderStatus(status).tone

const getStatusText = (status: string) => getOrderStatus(status).text

const getKitchenStatusClass = (order: any) => resolveKitchenStatus(order).tone

const getKitchenStatusText = (order: any) => resolveKitchenStatus(order).text

/** 制作中/待制作给一个呼吸点：让"还在后厨做"这件事在视觉上活起来 */
const isKitchenLive = (order: any): boolean => {
  const text = resolveKitchenStatus(order).text
  return text === '制作中' || text === '待制作'
}

const parseDishItems = (dishItems: any): any[] => {
  if (!dishItems) return []
  try {
    if (typeof dishItems === 'string') {
      return JSON.parse(dishItems)
    }
    if (Array.isArray(dishItems)) {
      return dishItems
    }
    return []
  } catch (error) {
    logger.errorMessage('解析菜品明细失败:', error)
    return []
  }
}
</script>

<style scoped>
.order-detail-content {
  padding: 8px 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--pos-text-primary);
  margin: 20px 0 12px 0;
  padding-left: 8px;
  border-left: 3px solid var(--pos-primary);
}

.amount-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--pos-primary);
}

.order-items-section {
  margin-top: 8px;
}

/* 状态徽章：原生 span 实现，替代 el-tag */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px;
  font-size: 12px;
  line-height: 20px;
  border-radius: var(--pos-radius-full, 9999px);
  border: 1px solid transparent;
  white-space: nowrap;
  font-weight: 600;
}

/* 状态点：静止时是纯色圆点；"制作中/待制作"再叠一层呼吸光环 */
.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.status-badge.is-live .status-badge__dot {
  position: relative;
  animation: status-breathe 1.8s ease-in-out infinite;
}

.status-badge.is-live .status-badge__dot::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid currentColor;
  opacity: 0.45;
  animation: status-ping 1.8s ease-out infinite;
}

@keyframes status-breathe {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(0.82); opacity: 0.7; }
}

@keyframes status-ping {
  0% { transform: scale(0.7); opacity: 0.5; }
  70% { transform: scale(1.6); opacity: 0; }
  100% { transform: scale(1.6); opacity: 0; }
}

/* 状态切换：轻微上浮 + 淡入，避免文字硬切 */
.status-pop-enter-active,
.status-pop-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.status-pop-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.status-pop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (prefers-reduced-motion: reduce) {
  .status-pop-enter-active,
  .status-pop-leave-active {
    transition: none;
  }

  .status-badge.is-live .status-badge__dot,
  .status-badge.is-live .status-badge__dot::after {
    animation: none;
  }
}

.status-badge--success {
  background-color: rgba(103, 194, 58, 0.1);
  border-color: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}

.status-badge--warning {
  background-color: rgba(230, 162, 60, 0.1);
  border-color: rgba(230, 162, 60, 0.2);
  color: #e6a23c;
}

.status-badge--danger {
  background-color: rgba(245, 108, 108, 0.1);
  border-color: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
}

.status-badge--primary {
  background-color: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.2);
  color: #409eff;
}

.status-badge--info {
  background-color: rgba(144, 147, 153, 0.1);
  border-color: rgba(144, 147, 153, 0.2);
  color: #909399;
}

/* 原生表格：替代 el-table，避免首次渲染时的组件初始化开销 */
.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  background-color: var(--pos-bg-secondary, #fff);
}

.items-table th,
.items-table td {
  padding: 8px 12px;
  border: 1px solid var(--pos-border-light, #ebeef5);
  text-align: left;
}

.items-table th {
  background-color: var(--pos-bg-hover, #f5f7fa);
  color: var(--pos-text-primary, #303133);
  font-weight: 600;
}

.items-table td {
  color: var(--pos-text-primary, #303133);
}

.items-table tbody tr:hover {
  background-color: var(--pos-bg-hover, #f5f7fa);
}

.items-table__name {
  min-width: 150px;
}

.items-table__price {
  width: 100px;
  text-align: right;
}

.items-table__qty {
  width: 80px;
  text-align: center;
}

.items-table__subtotal {
  width: 100px;
  text-align: right;
}

.order-detail-dialog {
  :deep(.el-dialog) {
    background-color: var(--pos-bg-secondary) !important;
  }

  :deep(.el-dialog__header) {
    border-bottom: 1px solid var(--pos-border-light);
  }

  :deep(.el-dialog__title) {
    color: var(--pos-text-primary) !important;
    font-weight: 600;
  }

  :deep(.el-dialog__body) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-button--primary) {
    color: #fff;
  }

  :deep(.el-button--text) {
    color: var(--pos-primary) !important;
  }

  :deep(.el-button) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__wrapper) {
    color: var(--pos-text-primary) !important;
    background-color: var(--pos-bg-secondary) !important;
    box-shadow: 0 0 0 1px var(--pos-border-color) inset !important;
  }

  :deep(.el-input__inner) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__placeholder) {
    color: var(--pos-text-muted) !important;
  }

  :deep(.el-form-item__label) {
    color: var(--pos-text-primary) !important;
  }
}
</style>