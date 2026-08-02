<template>
  <el-dialog
    :model-value="modelValue"
    title="支付确认"
    width="520px"
    class="payment-dialog"
    :lock-scroll="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="payment-confirm-content">
      <div class="confirm-info">
        <div class="info-row amount-row">
          <span class="label">支付金额</span>
          <span class="value amount-large">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
        <div class="info-row">
          <span class="label">支付方式</span>
          <span class="value">{{ getPaymentMethodText(selectedPaymentMethod) }}</span>
        </div>
        <div class="info-row" v-if="selectedPaymentMethod === 'cash'">
          <span class="label">收取现金</span>
          <el-input-number
            ref="cashInputRef"
            v-model="cashReceived"
            :min="totalAmount"
            :precision="2"
            :step="10"
            size="large"
            style="width: 200px"
            @keyup.enter="handleConfirm"
          />
        </div>
        <div class="info-row change-row" v-if="selectedPaymentMethod === 'cash' && cashReceived >= totalAmount">
          <span class="label">找零</span>
          <span class="value change">¥{{ (cashReceived - totalAmount).toFixed(2) }}</span>
        </div>
      </div>

      <div v-if="selectedPaymentMethod !== 'cash'" class="scan-section">
        <el-divider content-position="left">扫码支付</el-divider>
        <div class="scan-instruction">
          <div class="scan-icon">📱</div>
          <div class="scan-text">
            <h3>请顾客出示付款码</h3>
            <p>使用扫码枪扫描顾客手机上的付款码</p>
          </div>
        </div>
        <div class="scan-input-section">
          <el-input
            ref="paymentCodeInputRef"
            v-model="paymentCode"
            placeholder="扫码枪会自动填充付款码，或手动输入"
            size="large"
            clearable
            @keyup.enter="handleConfirm"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <el-alert
        v-if="selectedPaymentMethod !== 'cash'"
        title="支付提示"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      >
        <template #default>
          <p style="margin: 0; font-size: 13px; color: var(--pos-text-secondary)">
            扫码枪扫描付款码后会自动填充并确认支付。<br />
            如需手动输入，请在输入完成后点击"确认支付"按钮。
          </p>
        </template>
      </el-alert>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)" size="large">取消</el-button>
      <el-button
        type="primary"
        @click="handleConfirm"
        :loading="paying"
        size="large"
        :disabled="selectedPaymentMethod !== 'cash' && !paymentCode"
      >
        确认支付
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'

interface Props {
  modelValue: boolean
  totalAmount: number
  selectedPaymentMethod: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm', data: { method: string; cashReceived?: number; paymentCode?: string }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const cashReceived = ref<number>(0)
const paymentCode = ref<string>('')
const paying = ref<boolean>(false)
const cashInputRef = ref()
const paymentCodeInputRef = ref()

watch(
  () => props.modelValue,
  async (newVal) => {
    if (newVal) {
      if (props.selectedPaymentMethod === 'cash') {
        cashReceived.value = Math.ceil(props.totalAmount / 10) * 10
      }
      await nextTick()
      setTimeout(() => {
        if (props.selectedPaymentMethod === 'cash') {
          try {
            const inputNumberComponent = cashInputRef.value as { $el: HTMLElement } | null
            const inputElement = inputNumberComponent?.$el?.querySelector('input') as HTMLInputElement | null
            if (inputElement) {
              inputElement.focus()
              inputElement.select()
            }
          } catch {
            // 聚焦失败时静默处理
          }
        } else {
          paymentCodeInputRef.value?.focus()
        }
      }, 100)
    }
  }
)

const getPaymentMethodText = (method: string): string => {
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

const handleConfirm = () => {
  emit('confirm', {
    method: props.selectedPaymentMethod,
    ...(props.selectedPaymentMethod === 'cash' ? { cashReceived: cashReceived.value } : { paymentCode: paymentCode.value })
  })
}
</script>

<style scoped>
.payment-dialog .payment-confirm-content {
  padding: 8px 0;
}

.confirm-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-row.amount-row {
  padding: 20px;
  background: var(--pos-bg-primary);
  border-radius: 12px;
  margin-bottom: 8px;
}

.info-row.change-row {
  padding: 16px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.info-row .label {
  font-size: 15px;
  color: var(--pos-text-muted);
  font-weight: 500;
}

.info-row .value {
  font-size: 16px;
  font-weight: 600;
  color: var(--pos-text-primary);
}

.info-row .value.amount-large {
  font-size: 32px;
  font-weight: 800;
  color: var(--pos-primary);
}

.info-row .value.change {
  font-size: 20px;
  font-weight: 700;
  color: var(--pos-success);
}

.scan-section {
  margin-top: 24px;
}

.scan-instruction {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--pos-bg-primary);
  border-radius: 12px;
  margin-bottom: 16px;
}

.scan-icon {
  font-size: 48px;
}

.scan-text h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--pos-text-primary);
}

.scan-text p {
  margin: 0;
  font-size: 14px;
  color: var(--pos-text-muted);
}

.scan-input-section {
  margin-bottom: 8px;
}

.payment-dialog {
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

  :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px var(--pos-border-color) inset !important;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px var(--pos-primary) inset !important;
  }

  :deep(.el-input__inner) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__placeholder) {
    color: var(--pos-text-muted) !important;
  }

  :deep(.el-divider__text) {
    color: var(--pos-text-primary) !important;
    background-color: var(--pos-bg-secondary) !important;
  }

  :deep(.el-alert__title) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-alert__content) {
    color: var(--pos-text-secondary) !important;
  }

  :deep(.el-input-number) {
    width: 200px;
  }

  :deep(.el-input-number .el-input__wrapper) {
    box-shadow: 0 0 0 1px var(--pos-border-color) inset !important;
  }

  :deep(.el-input-number .el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px var(--pos-primary) inset !important;
  }
}
</style>
