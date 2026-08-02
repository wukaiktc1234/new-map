<template>
  <el-dialog
    :model-value="modelValue"
    width="520px"
    class="hold-dialog"
    :show-close="false"
    :lock-scroll="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="hd-header">
        <div class="hd-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none"><path d="M5 8h14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </div>
        <span class="hd-title">挂单</span>
        <button class="hd-close" @click="$emit('update:modelValue', false)">✕</button>
      </div>
    </template>

    <div class="hd-body">
      <div class="hd-remark-row">
        <input
          v-model="holdOrderForm.remark"
          class="hd-input"
          placeholder="备注（选填）"
          maxlength="50"
        />
      </div>

      <div v-if="cart.length > 0" class="hd-preview">
        <div class="hd-preview-label">当前订单</div>
        <div class="hd-preview-list">
          <div v-for="item in cart" :key="item.id" class="hd-item">
            <span class="hd-item-name">{{ item.name }}</span>
            <span class="hd-item-qty">×{{ item.quantity }}</span>
            <span class="hd-item-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
          </div>
        </div>
        <div class="hd-total-row">
          <span class="hd-total-label">合计</span>
          <span class="hd-total-val">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
      </div>

      <div v-if="cart.length === 0" class="hd-empty-tip">购物车为空，无法挂单</div>
    </div>

    <template #footer>
      <div class="hd-footer">
        <button class="hd-btn hd-btn-cancel" @click="$emit('update:modelValue', false)">取消</button>
        <button class="hd-btn hd-btn-confirm" @click="handleConfirm" :disabled="cart.length === 0 || isSubmitting">
          {{ isSubmitting ? '挂单中...' : '确认挂单' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
}

interface Props {
  modelValue: boolean
  cart: CartItem[]
  totalAmount: number
  isSubmitting?: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', remark: string): void
}>()

const holdOrderForm = reactive({ remark: '' })

const handleConfirm = () => {
  emit('confirm', holdOrderForm.remark)
}
</script>

<style scoped>
.hold-dialog :deep(.el-dialog) {
  background-color: var(--pos-bg-primary);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--pos-shadow-lg);
}

.hold-dialog :deep(.el-dialog__header) { padding: 0; margin: 0; }
.hold-dialog :deep(.el-dialog__body) { padding: 0; }
.hold-dialog :deep(.el-dialog__footer) { padding: 0; }

.hd-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 26px 16px;
  background: linear-gradient(135deg, rgba(234, 88, 12, 0.07), transparent);
}

.hd-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #ea580c, #f97316);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hd-icon-wrap svg { width: 19px; height: 19px; }

.hd-title { font-size: 18px; font-weight: 700; color: var(--pos-text-primary); }

.hd-close {
  margin-left: auto; width: 30px; height: 30px; border-radius: 7px;
  border: none; background: transparent; color: var(--pos-text-muted);
  font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.hd-close:hover { background: var(--pos-bg-hover); color: var(--pos-text-primary); }

.hd-body { padding: 0 26px 20px; }

.hd-remark-row { margin-bottom: 16px; }

.hd-input {
  width: 100%; height: 40px; padding: 0 14px;
  border: 1.5px solid var(--pos-border-color); border-radius: 9px;
  font-size: 14px; color: var(--pos-text-primary); background: var(--pos-bg-secondary);
  outline: none; transition: border-color 0.2s; box-sizing: border-box;
}
.hd-input:focus { border-color: #ea580c; }
.hd-input::placeholder { color: var(--pos-text-muted); }

.hd-preview {
  background: var(--pos-bg-secondary);
  border-radius: 12px;
  padding: 18px 20px;
}

.hd-preview-label {
  font-size: 12px; font-weight: 600; color: var(--pos-text-muted);
  text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 14px;
}

.hd-preview-list {
  display: flex; flex-direction: column; gap: 8px;
  max-height: 200px; overflow-y: auto;
  scrollbar-width: thin;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--pos-border-light); border-radius: 2px; }
}

.hd-item {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 0; font-size: 14px;
}

.hd-item-name {
  flex: 1; color: var(--pos-text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  font-weight: 500;
}

.hd-item-qty {
  color: var(--pos-text-secondary);
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 15px;
  font-weight: 600;
  min-width: 44px;
  text-align: right;
}

.hd-item-price {
  color: var(--pos-text-primary);
  font-weight: 700;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 16px;
  min-width: 72px;
  text-align: right;
}

.hd-total-row {
  display: flex; justify-content: space-between; align-items: baseline;
  padding-top: 14px; margin-top: 10px;
  border-top: 1.5px solid var(--pos-border-light);
}

.hd-total-label {
  font-size: 15px; font-weight: 600; color: var(--pos-text-secondary);
}

.hd-total-val {
  font-size: 26px; font-weight: 800; color: #ea580c;
  font-family: 'SF Mono', 'Consolas', monospace;
  letter-spacing: -0.5px;
}

.hd-empty-tip {
  text-align: center; padding: 32px 0;
  color: var(--pos-text-muted); font-size: 14px;
}

.hd-footer { display: flex; gap: 12px; padding: 16px 26px 20px; }

.hd-btn {
  flex: 1; height: 44px; border-radius: 10px; border: none;
  font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}

.hd-btn-cancel {
  background: var(--pos-bg-secondary); color: var(--pos-text-secondary);
  border: 1.5px solid var(--pos-border-color);
}
.hd-btn-cancel:hover { border-color: var(--pos-text-muted); color: var(--pos-text-primary); }

.hd-btn-confirm {
  background: linear-gradient(135deg, #ea580c, #f97316); color: #fff;
  box-shadow: 0 4px 14px rgba(234, 88, 12, 0.28);
}
.hd-btn-confirm:hover:not(:disabled) { box-shadow: 0 6px 20px rgba(234, 88, 12, 0.38); transform: translateY(-1px); }
.hd-btn-confirm:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
