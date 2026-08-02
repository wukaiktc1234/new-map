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
        <el-descriptions-item label="订单号">
          {{ order.orderNumber }}
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ order.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">
          {{ getPaymentMethodText(order.paymentMethod) }}
        </el-descriptions-item>
        <el-descriptions-item label="交易流水号">
          {{ formatTransactionId(order.transactionId, order.paymentMethod) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <span class="status-badge" :class="`status-badge--${getStatusClass(order.status)}`">
            {{ getStatusText(order.status) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="后厨状态">
          <span class="status-badge" :class="`status-badge--${getKitchenStatusClass(order.kitchenStatus)}`">
            {{ getKitchenStatusText(order.kitchenStatus) }}
          </span>
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
      <el-button
        v-if="order && (order.status === 'completed' || order.status === 'paid')"
        type="danger"
        @click="emit('refund', order)"
      >申请退款</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
interface Props {
  modelValue: boolean
  order: any | null
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'refund', order: any): void
}>()

const getPaymentMethodText = (method: string) => {
  const map: Record<string, string> = {
    cash: '现金支付',
    wechat: '微信支付',
    alipay: '支付宝',
    '现金': '现金支付',
    '微信支付': '微信支付',
    '支付宝': '支付宝'
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
const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    paid: 'success',
    cancelled: 'danger',
    making: 'primary',
    completed: 'success',
    served: 'success',
    refunded: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待制作',
    paid: '已支付',
    cancelled: '已取消',
    making: '制作中',
    completed: '制作完成',
    served: '已出餐',
    refunded: '已退款'
  }
  return map[status] || status
}

const getKitchenStatusClass = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    received: 'info',
    making: 'primary',
    completed: 'success',
    served: 'success',
    cancelled: 'danger'
  }
  return map[status] || 'info'
}

const getKitchenStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待制作',
    received: '已接单',
    making: '制作中',
    completed: '已完成',
    served: '已出餐',
    cancelled: '已取消'
  }
  return map[status] || '待处理'
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
    console.error('解析菜品明细失败:', error)
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
  padding: 2px 8px;
  font-size: 12px;
  line-height: 20px;
  border-radius: 4px;
  border: 1px solid transparent;
  white-space: nowrap;
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
