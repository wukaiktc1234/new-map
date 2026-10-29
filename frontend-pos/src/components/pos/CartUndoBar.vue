<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
/**
 * 购物车删除撤销条
 *
 * 收银高峰不能用模态确认打断节奏（「每删一行弹一次窗」），
 * 改成「删除立刻生效 + 底部 5 秒撤销条」：
 *   ① 不阻断下一动作，误删也能一键找回；
 *   ② 倒计时可见，避免收银员误以为还来得及；
 *   ③ 只保留最近一次删除，连续删除时以最后一次为准。
 */
interface PendingUndo {
  item: { id: string; name: string; price: number; quantity: number }
  index: number
  secondsLeft: number
}

const props = defineProps<{
  pending: PendingUndo | null
}>()

const emit = defineEmits<{
  (e: 'undo'): void
}>()
</script>

<template>
  <transition name="undo-bar">
    <div v-if="pending && pending.secondsLeft > 0" class="cart-undo-bar" data-test="cart-undo-bar" role="status">
      <el-icon class="undo-icon"><Delete /></el-icon>
      <span class="undo-text">
        已移除 <b>{{ pending.item.name }}</b> ×{{ pending.item.quantity }}
      </span>
      <button type="button" class="undo-btn" data-test="cart-undo-button" @click="emit('undo')">
        撤销
        <span class="undo-count">{{ pending.secondsLeft }}s</span>
      </button>
    </div>
  </transition>
</template>

<style scoped>
.cart-undo-bar {
  position: fixed;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  z-index: 2200;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  max-width: 520px;
  padding: 8px 12px 8px 16px;
  border-radius: var(--pos-radius-full, 9999px);
  border: 1px solid var(--pos-border-color, #e2e8f0);
  background: rgba(30, 41, 59, 0.96);
  color: #ffffff;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.28);
  font-size: 14px;
}

.undo-icon {
  font-size: 16px;
}

.undo-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.undo-text b {
  font-weight: 700;
  color: #fed7aa;
}

.undo-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  min-height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-primary, #e3651c);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background var(--pos-transition-fast, 150ms ease);
}

.undo-btn:hover {
  background: var(--pos-primary-dark, #c25515);
}

.undo-count {
  font-family: var(--pos-font-mono, 'Courier New', monospace);
  font-size: 12px;
  opacity: 0.85;
}

.undo-bar-enter-active,
.undo-bar-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.undo-bar-enter-from,
.undo-bar-leave-to {
  opacity: 0;
  transform: translate(-50%, 12px);
}
</style>
