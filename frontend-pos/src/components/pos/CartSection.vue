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
  width: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--pos-bg-secondary, #f5f7fa);
  border-radius: 20px;
  overflow: hidden;
  /* 购物车在左侧，阴影朝向右侧菜品区域 */
  box-shadow: 4px 0 16px var(--pos-shadow-sm, rgba(0, 0, 0, 0.06));
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
  padding: 16px;
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
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
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
  color: var(--pos-primary, #ea580c);
  font-weight: 600;
}

/* 数量控制区 */
.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.qty-btn {
  width: 28px;
  height: 28px;
  min-width: 44px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
}

.decrease-btn {
  background-color: #fef2f2;
  color: var(--pos-danger, #f56c6c);

  &:hover {
    background-color: #fee2e2;
  }

  &:active {
    transform: scale(0.92);
  }
}

.increase-btn {
  background-color: #fff7ed;
  color: var(--pos-primary, #ea580c);

  &:hover {
    background-color: #ffedd5;
  }

  &:active {
    transform: scale(0.92);
  }
}

.qty-number {
  font-size: 16px;
  font-weight: 600;
  min-width: 24px;
  text-align: center;
  cursor: pointer;
  color: var(--pos-text-primary, #303133);
  transition: color 0.2s;

  &:hover {
    color: var(--pos-primary, #ea580c);
  }
}

.qty-input {
  width: 50px;
  height: 28px;
  text-align: center;
  border: 1px solid var(--pos-primary, #ea580c);
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--pos-text-primary, #303133);
  outline: none;
  transition: box-shadow 0.2s;

  &:focus {
    box-shadow: 0 0 0 2px rgba(234, 88, 12, 0.25);
  }

  /* 隐藏数字输入框箭头 */
  &::-webkit-inner-spin-button,
  &::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }
  -moz-appearance: textfield;
}

.pc-delete-btn {
  margin-left: 8px;
  color: var(--pos-danger, #f56c6c);
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.15s ease;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background-color: #fef2f2;
    transform: scale(1.1);
  }
}

/* 滑动删除按钮 */
.delete-action {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 60px;
  background-color: #ef4444;
  color: #ffffff;
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
  font-size: 14px;
  color: var(--pos-text-muted, #909399);
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
  font-size: 26px;
  font-weight: 800;
  color: var(--pos-primary, #ea580c);
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
  border-radius: 12px;
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
  background: linear-gradient(135deg, #374151, #1f2937);
  color: #ffffff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  box-shadow: 0 4px 14px rgba(55, 65, 81, 0.35);
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
  gap: 10px;
}

.pay-wechat,
.pay-alipay {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;

  &:active {
    transform: scale(0.97);
  }
}

.pay-wechat {
  background: linear-gradient(135deg, #07c160, #06ad56);

  &:hover {
    box-shadow: 0 4px 14px rgba(7, 193, 96, 0.4);
    transform: translateY(-1px);
  }
}

.pay-alipay {
  background: linear-gradient(135deg, #1677ff, #0958d9);

  &:hover {
    box-shadow: 0 4px 14px rgba(22, 119, 255, 0.4);
    transform: translateY(-1px);
  }
}
</style>
