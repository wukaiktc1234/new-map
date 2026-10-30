<template>
  <div class="exc-card" :class="{ 'is-handled': handled }" :data-test="'exc-card-' + order.orderId">
    <div class="exc-card__head">
      <span class="exc-card__no" :data-test="'exc-no-' + order.orderId">#{{ shortNo }}</span>
      <span class="exc-badge" :class="'exc-badge--' + order.type" :data-test="'exc-type-' + order.orderId">
        {{ order.typeLabel }}
      </span>
      <!-- order.amount 是**分**（后端异常单口径）：展示统一走 fenToYuan，禁止裸 toFixed -->
      <span
        class="exc-card__amount"
        :data-test="'exc-amount-' + order.orderId"
        :data-amount-fen="order.amount"
      >¥{{ fenToYuan(order.amount).toFixed(2) }}</span>
    </div>

    <div class="exc-card__meta">
      <span class="exc-card__meta-item">{{ formatTime(order.createTime, '--:--') }}</span>
      <span class="exc-card__meta-item">开单员 {{ order.employeeName || '—' }}</span>
      <span v-if="order.paymentMethod" class="exc-card__meta-item">{{ order.paymentMethod }}</span>
    </div>

    <div v-if="!handled" class="exc-card__actions">
      <el-button
        class="exc-btn exc-btn--void"
        :data-test="'exc-void-' + order.orderId"
        :disabled="disabled"
        @click="$emit('void', order)"
      >
        作废
      </el-button>
      <el-button
        type="primary"
        class="exc-btn exc-btn--resolve"
        :data-test="'exc-resolve-' + order.orderId"
        :disabled="disabled"
        @click="$emit('resolve', order)"
      >
        补收款
      </el-button>
    </div>
    <div v-else class="exc-card__handled">
      <el-icon><CircleCheckFilled /></el-icon>
      <span data-test="'exc-handled-' + order.orderId">
        {{ handledLabel }}<template v-if="order.handledBy"> · {{ order.handledBy }}</template>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { formatTime } from '@/utils/format'
import { fenToYuan } from '@/utils/shiftStatus'
import type { ExceptionOrder } from '@/api/shiftExceptions'

const props = defineProps<{
  order: ExceptionOrder
  /** 已处理 Tab 里的卡片只读 */
  handled?: boolean
  /** 有写操作在飞时禁用按钮，防连点 */
  disabled?: boolean
}>()

defineEmits<{
  (e: 'void', order: ExceptionOrder): void
  (e: 'resolve', order: ExceptionOrder): void
}>()

const shortNo = computed(() => (props.order.orderNumber || '').slice(-6) || props.order.orderId)
const handledLabel = computed(() => {
  if (props.order.handledAs === 'voided') return '已作废'
  if (props.order.handledAs === 'resolved_scan') return '已扫码补收'
  return '已补收款'
})
</script>

<style scoped>
.exc-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--pos-radius-md, 12px);
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: inset 0 0 0 1px var(--pos-border-light, #f1f5f9);
  border-left: 3px solid var(--pos-warning, #c2861f);
}

.exc-card.is-handled {
  border-left-color: var(--pos-success, #059669);
  opacity: 0.9;
}

.exc-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.exc-card__no {
  font-family: var(--pos-font-number, monospace);
  font-size: 13px;
  font-weight: 700;
  color: var(--pos-text-primary, #1e293b);
}

.exc-card__amount {
  margin-left: auto;
  font-family: var(--pos-font-number, monospace);
  font-size: 16px;
  font-weight: 800;
  color: var(--pos-danger, #c94b4b);
  font-variant-numeric: tabular-nums;
}

.exc-badge {
  padding: 1px 8px;
  border-radius: var(--pos-radius-full, 9999px);
  font-size: 11px;
  font-weight: 600;
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
}

.exc-badge--cancelled {
  background: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
}

.exc-badge--amount_mismatch {
  background: var(--pos-primary-soft, #fdf1e7);
  color: var(--pos-primary-dark, #c25515);
}

.exc-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
}

.exc-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 触控基准 48px（R8 P1-5）：收银台湿手/戴手套也要点得中 */
.exc-btn {
  min-height: 48px;
  min-width: 104px;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
}

.exc-card__handled {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--pos-success, #059669);
}
</style>
