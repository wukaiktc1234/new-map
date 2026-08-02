<template>
  <div class="hanging-order-panel" v-if="visible" :class="{ expanded }">
    <div class="panel-header" @click="handleToggle">
      <div class="panel-title">
        <el-icon><Ticket /></el-icon>
        <span>挂单列表</span>
        <el-tag size="small" type="info" round>{{ orders.length }} 单</el-tag>
      </div>
      <div class="panel-actions">
        <span class="total-amount">合计: ¥{{ totalAmount.toFixed(2) }}</span>
        <el-icon class="toggle-icon" :class="{ rotated: expanded }"><ArrowDown /></el-icon>
      </div>
    </div>

    <div class="panel-body" v-show="expanded">
      <div class="held-orders-grid">
        <div
          v-for="order in orders"
          :key="order.id"
          class="held-order-card"
          @click="$emit('restore', order)"
        >
          <div class="card-header">
            <span class="hold-time">{{ order.holdTime }}</span>
            <el-tag size="small" type="primary" round>{{ order.items.length }} 项</el-tag>
          </div>

          <div class="card-items">
            <span
              v-for="(item, index) in getDisplayItems(order.items)"
              :key="index"
              class="item-name"
            >
              {{ item.name }}x{{ item.quantity }}
            </span>
            <span v-if="order.items.length > 3" class="item-name more-items">
              +{{ order.items.length - 3 }}项
            </span>
          </div>

          <div class="card-footer">
            <span class="order-amount">¥{{ order.totalAmount.toFixed(2) }}</span>
            <el-button type="primary" size="small" round @click.stop="$emit('restore', order)">
              取回
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Ticket, ArrowDown } from '@element-plus/icons-vue'

interface OrderItem {
  name: string
  quantity: number
}

interface HeldOrder {
  id: string
  holdTime: string
  items: OrderItem[]
  totalAmount: number
}

interface Props {
  visible: boolean
  orders: HeldOrder[]
  totalAmount: number
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'restore', order: HeldOrder): void
  (e: 'toggleExpand'): void
}>()

const expanded = ref<boolean>(false)

const handleToggle = (): void => {
  expanded.value = !expanded.value
  emit('toggleExpand')
}

const getDisplayItems = (items: OrderItem[]): OrderItem[] => {
  return items.slice(0, 3)
}
</script>

<style scoped>
.hanging-order-panel {
  border-top: 2px solid var(--pos-border-color, #e5e7eb);
  background: var(--pos-glass-bg, rgba(255, 255, 255, 0.9));
  backdrop-filter: blur(10px);
  transition: all 0.35s ease;
  overflow: hidden;
}

.hanging-order-panel.expanded {
  max-height: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}

.panel-header:hover {
  background: var(--pos-hover-bg, rgba(99, 102, 241, 0.05));
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--pos-text-primary, #1f2937);
}

.panel-title .el-icon {
  font-size: 20px;
  color: var(--pos-primary, #ea580c);
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-amount {
  font-size: 15px;
  font-weight: 700;
  color: var(--pos-primary, #ea580c);
}

.toggle-icon {
  font-size: 18px;
  color: var(--pos-text-muted, #6b7280);
  transition: transform 0.3s ease;
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.panel-body {
  overflow-y: auto;
  max-height: 320px;
  padding: 0 24px 16px;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.held-orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.held-order-card {
  background: var(--pos-bg-secondary, #f9fafb);
  border: 2px solid var(--pos-border-color, #e5e7eb);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}

.held-order-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(234, 88, 12, 0.15);
  border-color: var(--pos-primary, #ea580c);
}

.held-order-card:active {
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.hold-time {
  font-size: 14px;
  color: var(--pos-text-muted, #6b7280);
  font-family: 'Courier New', monospace;
}

.card-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  min-height: 32px;
}

.item-name {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--pos-primary-light, rgba(234, 88, 12, 0.1));
  color: var(--pos-primary, #ea580c);
  white-space: nowrap;
}

.more-items {
  background: var(--pos-primary, #ea580c);
  color: #ffffff;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid var(--pos-border-color, #e5e7eb);
}

.order-amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--pos-primary, #ea580c);
}

@media (max-width: 768px) {
  .held-orders-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }

  .held-order-card {
    padding: 12px;
  }

  .panel-body {
    max-height: 280px;
  }
}
</style>
