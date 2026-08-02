<template>
  <el-dialog
    :model-value="modelValue"
    width="540px"
    class="retrieve-dialog"
    :show-close="false"
    :lock-scroll="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="rd-header">
        <div class="rd-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <span class="rd-title">取单</span>
        <span v-if="hangingOrderStore.heldOrders.length > 0" class="rd-badge">{{ hangingOrderStore.heldOrders.length }}</span>
        <button class="rd-close" @click="$emit('update:modelValue', false)">✕</button>
      </div>
    </template>

    <div class="rd-body">
      <div v-if="hangingOrderStore.heldOrders.length === 0" class="rd-empty">
        <svg viewBox="0 0 24 24" fill="none" class="rd-empty-icon"><path d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.58a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.58 13H4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        <p>暂无挂单</p>
        <span class="rd-empty-sub">挂单后的订单会显示在这里</span>
      </div>

      <div v-else class="rd-list">
        <div
          v-for="order in hangingOrderStore.heldOrders"
          :key="order.id"
          class="rd-card"
          @click="$emit('select', order)"
        >
          <div class="rd-card-top">
            <span class="rd-time">{{ order.holdTime }}</span>
            <span class="rd-item-count">{{ order.items.length }}项</span>
          </div>
          <div class="rd-items-row">
            <span v-for="(item, idx) in order.items.slice(0, 3)" :key="idx" class="rd-item-tag">{{ item.name }}×{{ item.quantity }}</span>
            <span v-if="order.items.length > 3" class="rd-item-more">+{{ order.items.length - 3 }}</span>
          </div>
          <div class="rd-card-bottom">
            <span class="rd-total">¥{{ order.totalAmount.toFixed(2) }}</span>
            <span v-if="order.remark" class="rd-remark">{{ order.remark }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="rd-footer">
        <button class="rd-btn rd-btn-cancel" @click="$emit('update:modelValue', false)">取消</button>
        <button
          v-if="hangingOrderStore.heldOrders.length > 0"
          class="rd-btn rd-btn-clear"
          @click="$emit('clear')"
        >
          清空全部
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useHangingOrderStore, type HeldOrder } from '@/stores/hanging-order'

interface Props { modelValue: boolean }

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', order: HeldOrder): void
  (e: 'clear'): void
}>()

const props = defineProps<Props>()
const hangingOrderStore = useHangingOrderStore()

watch(() => props.modelValue, async (newVal) => {
  if (newVal) await hangingOrderStore.fetchHeldOrders()
})
</script>

<style scoped>
.retrieve-dialog :deep(.el-dialog) {
  background-color: var(--pos-bg-primary);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--pos-shadow-lg);
}
.retrieve-dialog :deep(.el-dialog__header) { padding: 0; margin: 0; }
.retrieve-dialog :deep(.el-dialog__body) { padding: 0; }
.retrieve-dialog :deep(.el-dialog__footer) { padding: 0; }

.rd-header {
  display: flex; align-items: center; gap: 10px;
  padding: 20px 26px 16px;
  background: linear-gradient(135deg, rgba(234, 88, 12, 0.07), transparent);
}

.rd-icon-wrap {
  width: 34px; height: 34px; border-radius: 9px;
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.rd-icon-wrap svg { width: 19px; height: 19px; }

.rd-title { font-size: 18px; font-weight: 700; color: var(--pos-text-primary); }

.rd-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 26px; height: 26px; padding: 0 8px; border-radius: 13px;
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff; font-size: 13px; font-weight: 700;
}

.rd-close {
  margin-left: auto; width: 30px; height: 30px; border-radius: 7px;
  border: none; background: transparent; color: var(--pos-text-muted);
  font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.rd-close:hover { background: var(--pos-bg-hover); color: var(--pos-text-primary); }

.rd-body { padding: 0 26px 20px; }

.rd-empty {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 52px 0 40px; color: var(--pos-text-muted);
}
.rd-empty-icon { width: 52px; height: 52px; opacity: 0.2; color: var(--pos-primary); }
.rd-empty p { font-size: 15px; margin: 0; color: var(--pos-text-secondary); }
.rd-empty-sub { font-size: 13px; color: var(--pos-text-muted); }

.rd-list {
  display: flex; flex-direction: column; gap: 12px;
  max-height: 400px; overflow-y: auto;
  scrollbar-width: thin;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--pos-border-light); border-radius: 2px; }
}

.rd-card {
  background: var(--pos-bg-secondary);
  border: 1.5px solid var(--pos-border-color);
  border-radius: 12px;
  padding: 14px 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.rd-card:hover {
  border-color: var(--pos-primary);
  box-shadow: 0 4px 16px rgba(234, 88, 12, 0.14);
  transform: translateY(-1px);
}

.rd-card-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;
}
.rd-time { font-size: 14px; color: var(--pos-text-secondary); font-family: 'SF Mono', 'Consolas', monospace; font-weight: 500; }
.rd-item-count {
  font-size: 13px; font-weight: 700; color: var(--pos-primary);
  background: rgba(234, 88, 12, 0.1); padding: 3px 10px; border-radius: 10px;
}

.rd-items-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
.rd-item-tag, .rd-item-more {
  font-size: 12px; padding: 4px 10px; border-radius: 6px;
  background: var(--pos-bg-primary); color: var(--pos-text-secondary);
}
.rd-item-more { background: rgba(234, 88, 12, 0.1); color: var(--pos-primary); font-weight: 600; }

.rd-card-bottom {
  display: flex; justify-content: space-between; align-items: baseline;
  padding-top: 10px; border-top: 1.5px solid var(--pos-border-light);
}

.rd-total {
  font-size: 22px; font-weight: 800; color: var(--pos-primary);
  font-family: 'SF Mono', 'Consolas', monospace;
  letter-spacing: -0.3px;
}
.rd-remark { font-size: 12px; color: var(--pos-text-muted); max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.rd-footer { display: flex; gap: 12px; padding: 16px 26px 20px; }

.rd-btn {
  flex: 1; height: 44px; border-radius: 10px; border: none;
  font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.rd-btn-cancel {
  background: var(--pos-bg-secondary); color: var(--pos-text-secondary);
  border: 1.5px solid var(--pos-border-color);
}
.rd-btn-cancel:hover { border-color: var(--pos-text-muted); color: var(--pos-text-primary); }

.rd-btn-clear {
  background: linear-gradient(135deg, #dc2626, #ef4444); color: #fff;
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.28);
}
.rd-btn-clear:hover:not(:disabled) { box-shadow: 0 6px 20px rgba(220, 38, 38, 0.38); transform: translateY(-1px); }
</style>
