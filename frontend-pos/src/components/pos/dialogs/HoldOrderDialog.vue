<template>
  <el-dialog
    :model-value="modelValue"
    width="560px"
    class="hold-dialog"
    :show-close="false"
    :lock-scroll="false"
    :append-to-body="true"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="hd-header">
        <div class="hd-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none"><path d="M5 8h14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </div>
        <span class="hd-title">挂单</span>
        <button class="hd-close" aria-label="关闭" @click="$emit('update:modelValue', false)">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </template>

    <div class="hd-body">
      <input
        v-model="holdOrderForm.remark"
        class="hd-input"
        placeholder="备注（选填，例如「客人先去取钱」）"
        maxlength="50"
      />

      <!-- 订单概览：件数常驻，长列表也不用滚回去看合计 -->
      <div class="hd-preview">
        <div class="hd-preview-head">
          <span class="hd-preview-label">当前订单</span>
          <span class="hd-count">{{ cart.length }} 项 · {{ totalQty }} 件</span>
        </div>

        <div v-if="cart.length > 0" class="hd-list-wrap">
          <div class="hd-preview-list">
            <div v-for="item in cart" :key="item.id" class="hd-item">
              <span class="hd-item-name" :title="item.name">{{ item.name }}</span>
              <span class="hd-item-qty">×{{ item.quantity }}</span>
              <span class="hd-item-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
          <!-- 列表底部渐隐：提示"下面还有"，而不是把一行生硬切断 -->
          <div class="hd-list-mask" aria-hidden="true"></div>
        </div>

        <div v-if="cart.length === 0" class="hd-empty-tip">购物车为空，无法挂单</div>

        <div class="hd-total-row">
          <span class="hd-total-label">合计</span>
          <span class="hd-total-val">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="hd-footer">
        <button class="hd-btn hd-btn-cancel" @click="$emit('update:modelValue', false)">取消</button>
        <button class="hd-btn hd-btn-confirm" @click="handleConfirm" :disabled="cart.length === 0 || isSubmitting">
          {{ isSubmitting ? '挂单中…' : '确认挂单' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { Close } from '@element-plus/icons-vue'

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

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', remark: string): void
}>()

const holdOrderForm = reactive({ remark: '' })

const totalQty = computed(() => props.cart.reduce((sum, item) => sum + (item.quantity || 0), 0))

const handleConfirm = () => {
  emit('confirm', holdOrderForm.remark)
}
</script>

<style scoped>
/* ==================== 挂单弹窗 ====================
 * 订单多时的原则：弹窗高度封顶、只有"商品清单"滚动、合计与按钮永远可见。
 * 旧版把列表塞进固定 200px 的卡片里，多单时既出现双滚动条，又把页脚顶出屏幕。
 */
/* 注意：class="hold-dialog" 直接落在 .el-dialog 根元素上，
 * 所以这里写 .hold-dialog 本身，而不是 .hold-dialog .el-dialog（后者永远匹配不到）。 */
.hold-dialog {
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background-color: var(--pos-bg-primary, #ffffff);
  border-radius: var(--pos-radius-md, 12px);
  overflow: hidden;
}

/* 头部/主体/页脚的结构性 padding 与 flex 在全局 dialog.css（.hold-dialog 段） */

.hd-header {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  padding: var(--pos-spacing-sm, 16px) var(--pos-spacing-md, 24px);
  border-bottom: 1px solid var(--pos-border-light, #f1f5f9);
}

.hd-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-primary, #e3651c);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hd-icon-wrap svg { width: 18px; height: 18px; }
.hd-title { font-size: 16px; font-weight: 700; color: var(--pos-text-primary, #1e293b); }

.hd-close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border-radius: var(--pos-radius-sm, 8px);
  border: none;
  background: transparent;
  color: var(--pos-text-muted, #94a3b8);
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    color var(--pos-transition-fast, 150ms ease);
}
.hd-close:hover { background: var(--pos-bg-hover, #e2e8f0); color: var(--pos-text-primary, #1e293b); }

.hd-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: var(--pos-spacing-sm, 16px);
  padding: var(--pos-spacing-md, 24px);
}

.hd-input {
  width: 100%;
  height: 44px;
  padding: 0 var(--pos-spacing-sm, 16px);
  border: 1px solid var(--pos-border-color, #e2e8f0);
  border-radius: var(--pos-radius-sm, 8px);
  font-size: 14px;
  color: var(--pos-text-primary, #1e293b);
  background: var(--pos-bg-secondary, #f8fafc);
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--pos-transition-fast, 150ms ease);
}
.hd-input:focus { border-color: var(--pos-primary, #e3651c); background: var(--pos-bg-primary, #ffffff); }
.hd-input::placeholder { color: var(--pos-text-muted, #94a3b8); }

/* 预览卡：占满剩余高度，内部只有清单滚动 */
.hd-preview {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: var(--pos-radius-md, 12px);
  background: var(--pos-bg-secondary, #f8fafc);
  box-shadow: inset 0 0 0 1px var(--pos-border-light, #f1f5f9);
  overflow: hidden;
}

.hd-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--pos-spacing-xs, 8px) var(--pos-spacing-sm, 16px);
  border-bottom: 1px solid var(--pos-border-light, #f1f5f9);
}

.hd-preview-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--pos-text-secondary, #64748b);
}

.hd-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--pos-primary, #e3651c);
  font-variant-numeric: tabular-nums;
}

.hd-list-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
}

.hd-preview-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--pos-spacing-xs, 8px) var(--pos-spacing-sm, 16px);
  scrollbar-width: thin;
  scrollbar-color: var(--pos-border-color, #e2e8f0) transparent;
}
.hd-preview-list::-webkit-scrollbar { width: 6px; }
.hd-preview-list::-webkit-scrollbar-thumb {
  background: var(--pos-border-color, #e2e8f0);
  border-radius: 9999px;
}

/* 底部渐隐：用视觉提示"还能往下滚"，避免把某一行从中间切断 */
.hd-list-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 24px;
  pointer-events: none;
  background: linear-gradient(to bottom, transparent, var(--pos-bg-secondary, #f8fafc));
}

.hd-item {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  min-height: 40px;
  font-size: 14px;
}

.hd-item-name {
  flex: 1;
  min-width: 0;
  color: var(--pos-text-primary, #1e293b);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hd-item-qty {
  flex-shrink: 0;
  min-width: 40px;
  text-align: right;
  font-size: 13px;
  color: var(--pos-text-secondary, #64748b);
  font-variant-numeric: tabular-nums;
}

.hd-item-price {
  flex-shrink: 0;
  min-width: 76px;
  text-align: right;
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
  font-size: 15px;
  font-weight: 700;
  color: var(--pos-text-primary, #1e293b);
}

/* 合计常驻在卡片底部，不随列表滚走 */
.hd-total-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: var(--pos-spacing-xs, 8px) var(--pos-spacing-sm, 16px) var(--pos-spacing-sm, 16px);
  border-top: 1px solid var(--pos-border-light, #f1f5f9);
  background: var(--pos-bg-primary, #ffffff);
}

.hd-total-label { font-size: 13.5px; font-weight: 600; color: var(--pos-text-secondary, #64748b); }

.hd-total-val {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
  font-size: 26px;
  font-weight: 800;
  color: var(--pos-primary, #e3651c);
}

.hd-empty-tip { padding: 32px 0; text-align: center; color: var(--pos-text-muted, #94a3b8); font-size: 14px; }

.hd-footer {
  display: flex;
  gap: var(--pos-spacing-xs, 8px);
  padding: var(--pos-spacing-sm, 16px) var(--pos-spacing-md, 24px);
  border-top: 1px solid var(--pos-border-light, #f1f5f9);
}

.hd-btn {
  flex: 1;
  height: 44px;
  border-radius: var(--pos-radius-sm, 8px);
  border: none;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    border-color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);
}

.hd-btn-cancel {
  background: var(--pos-bg-secondary, #f8fafc);
  color: var(--pos-text-secondary, #64748b);
  border: 1px solid var(--pos-border-color, #e2e8f0);
}
.hd-btn-cancel:hover { border-color: var(--pos-text-muted, #94a3b8); color: var(--pos-text-primary, #1e293b); }

.hd-btn-confirm {
  background: var(--pos-primary, #e3651c);
  color: #fff;
}
.hd-btn-confirm:hover:not(:disabled) { background: var(--pos-primary-dark, #c25515); }
.hd-btn-confirm:active:not(:disabled) { transform: scale(0.98); }
.hd-btn-confirm:disabled { opacity: 0.45; cursor: not-allowed; }
</style>
