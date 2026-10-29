<template>
  <div class="cart-section">
    <!-- 头部 -->
    <div class="cart-header">
      <div v-if="currentOrderNumber" class="order-info">
        <el-tag type="primary" effect="dark" size="large" round>
          订单号: {{ currentOrderNumber }}
        </el-tag>
      </div>
      <el-button
        v-if="cart.length > 0"
        type="danger"
        text
        size="small"
        class="clear-btn"
        @click="handleClearCart"
      >
        清空
      </el-button>
    </div>

    <!-- 购物车列表 -->
    <div class="cart-list">
      <div v-if="cart.length === 0" class="empty-cart">
        <el-icon class="empty-icon"><ShoppingCart /></el-icon>
        <span>点击右侧菜品加入购物车</span>
      </div>

      <div v-else class="cart-items">
        <div
          v-for="(item, index) in cart"
          :key="item.id"
          ref="(el) => { if (el) cartItemRefs[index] = el as HTMLElement }"
          class="cart-item-wrapper"
        >
          <!-- 滑动区域 -->
          <div
            class="cart-item-swipe"
            :style="{ transform: `translateX(${item.swipeX || 0}px)` }"
          >
            <div class="cart-item">
              <div class="item-info">
                <div class="item-name">{{ item.name }}</div>
                <div class="item-price">&yen;{{ (item.price * item.quantity).toFixed(2) }}</div>
              </div>
              <div class="item-quantity">
                <button
                  class="qty-btn decrease-btn"
                  @click.stop="$emit('decrease', index)"
                >
                  <el-icon><Minus /></el-icon>
                </button>

                <span
                  v-if="editingIndex !== index"
                  class="qty-number"
                  @click.stop="startEditQuantity(index, item.quantity)"
                >
                  {{ item.quantity }}
                </span>

                <input
                  v-else
                  ref="(el) => { if (el) qtyInputRef = el as HTMLInputElement }"
                  v-model.number="editingQuantity"
                  type="number"
                  min="1"
                  class="qty-input"
                  @keyup.enter="confirmEditQuantity(index)"
                  @blur="confirmEditQuantity(index)"
                />

                <button
                  class="qty-btn increase-btn"
                  @click.stop="$emit('increase', index)"
                >
                  <el-icon><Plus /></el-icon>
                </button>

                <button
                  v-if="!isTouchDevice"
                  class="pc-delete-btn"
                  title="删除"
                  @click.stop="$emit('delete', index)"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
          </div>

          <!-- 触摸端滑动删除按钮 -->
          <div
            v-if="isTouchDevice"
            class="delete-action"
            @click.stop="$emit('delete', index)"
          >
            删除
          </div>
        </div>
      </div>
    </div>

    <!-- 底部金额与支付 -->
    <div class="cart-footer">
      <div class="total-section">
        <div class="total-row">
          <span class="label">共 {{ totalQuantity }} 份</span>
        </div>
        <div class="total-row">
          <span class="label">商品金额</span>
          <span class="value">
            <span class="currency">&yen;</span>
            {{ originalTotalAmount.toFixed(2) }}
          </span>
        </div>
        <div v-if="discountAmount > 0" class="total-row discount-row">
          <span class="label">优惠券</span>
          <span class="value">-&yen;{{ discountAmount.toFixed(2) }}</span>
        </div>
        <div class="total-row amount-row">
          <span class="label">应收金额</span>
          <span class="value amount">
            <span class="currency">&yen;</span>
            <span class="number">{{ totalAmount.toFixed(2) }}</span>
          </span>
        </div>
      </div>

      <div class="payment-section">
        <div class="payment-buttons">
          <button class="pay-btn pay-cash" @click="$emit('payment', 'cash')">
            <span>现金</span>
          </button>
          <div class="pay-secondary-row">
            <button class="pay-btn pay-wechat" @click="$emit('payment', 'wechat')">
              <span>微信</span>
            </button>
            <button class="pay-btn pay-alipay" @click="$emit('payment', 'alipay')">
              <span>支付宝</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Minus, Plus, Delete, ShoppingCart } from '@element-plus/icons-vue'

interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
  swipeX?: number
}

interface Props {
  cart: CartItem[]
  currentOrderNumber: string
  totalQuantity: number
  originalTotalAmount: number
  totalAmount: number
  discountAmount: number
  appliedCoupon: unknown | null
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'increase', index: number): void
  (e: 'decrease', index: number): void
  (e: 'delete', index: number): void
  (e: 'clear'): void
  (e: 'editQty', index: number): void
  (e: 'confirmEdit', index: number): void
  (e: 'cancelEdit'): void
  (e: 'payment', method: string): void
}>()

const editingIndex = ref<number | null>(null)
const editingQuantity = ref(1)
const qtyInputRef = ref<HTMLInputElement>()
const isTouchDevice = ref('ontouchstart' in window)

const startEditQuantity = async (index: number, quantity: number) => {
  editingIndex.value = index
  editingQuantity.value = quantity
  emit('editQty', index)
  await nextTick()
  qtyInputRef.value?.focus()
  qtyInputRef.value?.select()
}

const confirmEditQuantity = (index: number) => {
  if (editingQuantity.value >= 1) {
    emit('confirmEdit', index)
  }
  editingIndex.value = null
}

const handleClearCart = async () => {
  try {
    await ElMessageBox.confirm('确定清空购物车？此操作不可撤销', '清空确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    emit('clear')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.cart-section {
  /* 弹性宽度：随视口伸缩，绝不写死绝对宽度（缩放 110% 时也不溢出） */
  flex: 0 1 clamp(340px, 30%, 460px);
  min-width: 300px;
  display: flex;
  flex-direction: column;
  background-color: var(--pos-bg-primary, #ffffff);
  border: 1px solid var(--pos-border-light, #f1f5f9);
  border-radius: var(--pos-radius-lg, 16px);
  overflow: hidden;
  /* 面板级阴影：浅灰画布上能明显"浮起来" */
  box-shadow: var(--pos-shadow-md, 0 6px 18px rgba(0, 0, 0, 0.08));
}

/* 小屏幕下购物车宽度自适应，作为底部面板展示 */
@media (max-width: 1000px) {
  .cart-section {
    width: 100%;
    max-height: 50vh;
    box-shadow: 0 -4px 16px var(--pos-shadow-sm, rgba(0, 0, 0, 0.06));
  }
}

/* 头部 */
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--pos-spacing-sm, 16px);
  background-color: var(--pos-bg-primary, #ffffff);
  border-bottom: 1px solid var(--pos-border-color, #e4e7ed);
}

.order-info .order-tag {
  font-weight: 600;
  letter-spacing: 0.5px;
}

.clear-btn {
  color: var(--pos-danger, #f56c6c);
  font-weight: 500;

  &:hover {
    color: #e63946;
  }
}

/* 购物车列表 */
.cart-list {
  /* min-height:0 是关键：否则 flex 子项按内容撑开，短屏/110% 缩放下会把支付区顶出屏幕 */
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: var(--pos-spacing-xs, 8px) var(--pos-spacing-sm, 16px);
  scrollbar-width: thin;

  &::-webkit-scrollbar {
    width: 5px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.12);
    border-radius: 3px;

    &:hover {
      background-color: rgba(0, 0, 0, 0.2);
    }
  }
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;
  color: var(--pos-text-muted, #909399);
}

.empty-icon {
  font-size: 48px;
  opacity: 0.6;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 购物车项容器（滑动支持） */
.cart-item-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
}

.cart-item-swipe {
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* 购物车项 */
.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: var(--pos-bg-primary, #ffffff);
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

  &:hover .pc-delete-btn {
    opacity: 1;
  }
}

.item-info {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--pos-text-primary, #303133);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 14px;
  color: var(--pos-primary, #c9622a);
  font-weight: 600;
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
}

/* 数量控制区 */
.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 加减按钮：圆形，44px 触控热区；底色走令牌，深色模式自动变暗（原写死 #fef2f2/#fff7ed → 深色下一块白） */
.qty-btn {
  width: 34px;
  height: 34px;
  min-width: 44px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--pos-radius-full, 9999px);
  border: none;
  cursor: pointer;
  font-size: 15px;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);
}

.decrease-btn {
  background-color: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);

  &:hover {
    background-color: var(--pos-danger-soft-hover, #f6dede);
  }

  &:active {
    transform: scale(0.92);
  }
}

.increase-btn {
  background-color: var(--pos-primary-soft, #fbf1ea);
  color: var(--pos-primary, #c9622a);

  &:hover {
    background-color: var(--pos-primary-soft-hover, #f6e3d5);
  }

  &:active {
    transform: scale(0.92);
  }
}

.qty-number {
  font-size: 17px;
  font-weight: 700;
  min-width: 32px;
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
  text-align: center;
  cursor: pointer;
  color: var(--pos-text-primary, #303133);
  transition: color 0.2s;

  &:hover {
    color: var(--pos-primary, #e3651c);
  }
}

.qty-input {
  width: 50px;
  height: 28px;
  text-align: center;
  border: 1px solid var(--pos-primary, #e3651c);
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--pos-text-primary, #303133);
  outline: none;
  transition: box-shadow 0.2s;

  &:focus {
    box-shadow: 0 0 0 2px rgba(227, 101, 28, 0.25);
  }

  /* 隐藏数字输入框箭头 */
  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }
  -moz-appearance: textfield;
}

/* 深色：加减按钮的软底与图标再提亮一档，避免"看不见的深红/深棕圆圈" */
[data-theme='dark'] .increase-btn {
  background-color: #3a2c21;
  color: #e8935f;
}

[data-theme='dark'] .increase-btn:hover {
  background-color: #46352a;
}

[data-theme='dark'] .decrease-btn {
  background-color: #3a2a2c;
  color: #e08a8a;
}

[data-theme='dark'] .decrease-btn:hover {
  background-color: #46333a;
}

/* 深色：画布用最深一档，条目卡片提亮一档，形成"卡片浮在画布上"的层次 */
[data-theme='dark'] .cart-section {
  background-color: var(--pos-bg-primary, #1e1e1f);
}

[data-theme='dark'] .cart-item {
  background-color: var(--pos-bg-secondary, #2c2c2e);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

[data-theme='dark'] .cart-item:hover {
  background-color: var(--pos-bg-tertiary, #353538);
}

/* 删除按钮：去掉原生按钮的方块边框与白底，改为圆形幽灵按钮 */
.pc-delete-btn {
  margin-left: 4px;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--pos-text-muted, #94a3b8);
  opacity: 0;
  cursor: pointer;
  border-radius: var(--pos-radius-full, 9999px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity var(--pos-transition-fast, 150ms ease),
    background-color var(--pos-transition-fast, 150ms ease),
    color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);

  &:hover {
    background-color: var(--pos-danger-soft-hover, #f6dede);
    color: var(--pos-danger, #c94b4b);
    transform: scale(1.06);
  }

  &:active {
    transform: scale(0.94);
  }
}

/* 滑动删除按钮 */
.delete-action {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 64px;
  background-color: var(--pos-danger, #c94b4b);
  color: #ffffff;
  border-radius: 0 var(--pos-radius-md, 12px) var(--pos-radius-md, 12px) 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-left: 60px;
  user-select: none;
}

/* 底部 */
.cart-footer {
  padding: 16px;
  border-top: 1px solid var(--pos-border-color, #e4e7ed);
  background-color: var(--pos-bg-primary, #ffffff);
  flex-shrink: 0;
}

.total-section {
  margin-bottom: 16px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}

.total-row:last-child {
  margin-bottom: 0;
}

.label {
  font-size: 13.5px;
  color: var(--pos-text-secondary, #64748b);
}

.value {
  font-size: 15px;
  font-weight: 600;
  color: var(--pos-text-primary, #303133);
}

.amount-row {
  padding-top: 10px;
  border-top: 1px dashed var(--pos-border-color, #e4e7ed);
  margin-top: 8px;
}

.value.amount {
  display: flex;
  align-items: baseline;
  gap: 1px;
}

.value.amount .currency {
  font-size: 16px;
  font-weight: 600;
  color: var(--pos-text-primary, #303133);
}

.value.amount .number {
  font-size: 30px;
  font-weight: 800;
  color: var(--pos-primary, #c9622a);
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

.discount-row {
  color: var(--pos-success, #67c23a);
}

.discount-row .value {
  color: var(--pos-success, #67c23a);
}

/* 支付按钮区 */
.payment-section .payment-buttons {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.pay-btn {
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--pos-radius-md, 12px);
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
  letter-spacing: 1px;

  &:active {
    transform: scale(0.97);
  }
}

.pay-cash {
  width: 100%;
  height: 52px;
  font-size: 17px;
  font-weight: 700;
  /* 中性石墨：避免深色模式下偏蓝的"黑客感" */
  background: linear-gradient(135deg, #4a4a50, #34343a);
  color: #ffffff;
  border-radius: var(--pos-radius-md, 12px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--pos-spacing-xs, 8px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  letter-spacing: 8px;

  &:hover {
    box-shadow: 0 6px 20px rgba(55, 65, 81, 0.5);
    transform: translateY(-1px);

    &:active {
      transform: translateY(0) scale(0.98);
    }
  }
}

.pay-secondary-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--pos-spacing-xs, 8px);
}

.pay-wechat,
.pay-alipay {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  border-radius: var(--pos-radius-md, 12px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;

  &:active {
    transform: scale(0.97);
  }
}

.pay-wechat {
  background: linear-gradient(135deg, #46a97a, #3d9870);

  &:hover {
    background: linear-gradient(135deg, #3f9d6d, #368a63);
    box-shadow: 0 4px 14px rgba(63, 157, 109, 0.28);
    transform: translateY(-1px);
  }
}

.pay-alipay {
  background: linear-gradient(135deg, #4a7fb5, #3d6c9c);

  &:hover {
    background: linear-gradient(135deg, #4477aa, #38648f);
    box-shadow: 0 4px 14px rgba(74, 127, 181, 0.28);
    transform: translateY(-1px);
  }
}

/* ==================== 短屏自适应 ====================
 * 1280×720 或 110% 缩放下，可用高度只剩 ~590px：
 * 若支付区保持 52+44 的固定高度，底部按钮会被裁掉（图三的整改点之一）。
 * 这里按"视口高度"降档，保证一屏内看到：清单 + 汇总 + 三个支付按钮。
 */
@media (max-height: 820px) {
  .pay-cash {
    height: 46px;
    font-size: 16px;
    margin-bottom: 6px;
  }

  .pay-wechat,
  .pay-alipay {
    height: 40px;
    font-size: 14px;
  }

  .cart-list {
    padding: 6px 12px;
  }
}

@media (max-height: 700px) {
  .pay-cash {
    height: 42px;
    margin-bottom: 4px;
  }

  .pay-wechat,
  .pay-alipay {
    height: 36px;
  }
}
</style>
