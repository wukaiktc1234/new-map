<template>
  <el-dialog
    :model-value="modelValue"
    :title="null"
    width="460px"
    class="success-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :lock-scroll="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="success-content">
      <div class="success-icon-wrapper">
        <div class="success-icon-circle">
          <div class="success-icon">
            <CircleCheckFilled />
          </div>
        </div>
        <div class="success-ripple"></div>
      </div>
      <div class="success-message">
        <h3>支付成功</h3>
        <p class="success-desc">您的订单已支付完成</p>
      </div>

      <div class="pickup-cards-wrapper">
        <div class="pickup-card main-card">
          <div class="pickup-label">取餐号</div>
          <div class="pickup-number">{{ pickupNumber }}</div>
        </div>
      </div>

      <div class="success-tip">
        请凭取餐号到取餐区取餐
      </div>
    </div>
    <template #footer>
      <div class="success-footer">
        <el-button type="primary" size="large" class="success-btn" @click="$emit('complete')">
          完成
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CircleCheckFilled } from '@element-plus/icons-vue'

interface Props {
  modelValue: boolean
  orderNumber: string
  pickupNumber: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'complete'): void
}

defineProps<Props>()
defineEmits<Emits>()
</script>

<style scoped>
.success-dialog .success-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0 8px 0;
}

.success-icon-wrapper {
  position: relative;
  margin-bottom: 24px;
}

.success-icon-circle {
  width: 96px;
  height: 96px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.3);
  animation: successPopIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.success-icon {
  font-size: 52px;
  color: #fff;
}

.success-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 96px;
  height: 96px;
  background: rgba(16, 185, 129, 0.2);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: successRipple 1.5s ease-out infinite;
  z-index: 1;
}

@keyframes successPopIn {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes successRipple {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.5;
  }
  100% {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0;
  }
}

.success-message {
  text-align: center;
  margin-bottom: 24px;
}

.success-message h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--pos-text-primary);
}

.success-desc {
  margin: 0;
  font-size: 15px;
  color: var(--pos-text-muted);
}

.pickup-cards-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  margin-bottom: 16px;
}

.pickup-card {
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.pickup-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.pickup-card.main-card {
  background: linear-gradient(135deg, var(--pos-primary) 0%, #ea580c 100%);
  box-shadow: 0 8px 24px rgba(234, 88, 12, 0.25);
}

.pickup-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  margin-bottom: 8px;
  font-weight: 500;
}

.pickup-number {
  font-size: 48px;
  font-weight: 800;
  color: #fff;
  text-align: center;
  font-family: 'SF Mono', 'Monaco', monospace;
  letter-spacing: 4px;
}

.success-tip {
  font-size: 14px;
  color: var(--pos-text-muted);
  text-align: center;
  margin-bottom: 8px;
}

.success-footer {
  display: flex;
  justify-content: center;
  width: 100%;
}

.success-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.success-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.success-dialog {
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
}
</style>
