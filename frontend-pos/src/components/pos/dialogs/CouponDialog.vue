<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="券码核销"
    width="500px"
    class="coupon-dialog"
    :lock-scroll="false"
  >
    <div class="coupon-content">
      <el-form :model="couponForm" label-width="80px" @submit.prevent>
        <el-form-item label="券码">
          <el-input ref="couponInputRef" v-model="couponForm.code" placeholder="请输入或扫描券码" clearable @keydown.enter.stop.prevent="verifyCoupon">
            <template #append>
              <el-button @click="verifyCoupon">验证</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <div v-if="couponInfo?.valid" class="coupon-info">
        <el-alert type="success" :closable="false">
          <template #title>
            <div class="coupon-title">{{ couponInfo.name }}</div>
            <div class="coupon-desc">优惠：{{ couponInfo.discount }}</div>
            <div class="coupon-valid">有效期：{{ couponInfo.validPeriod }}</div>
          </template>
        </el-alert>
      </div>
      <div v-if="appliedCoupon" class="applied-coupon">
        <el-divider content-position="left">已应用优惠券</el-divider>
        <el-tag type="success" closable @close="emit('remove')">
          {{ appliedCoupon.name }} - {{ appliedCoupon.discount }}
        </el-tag>
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" @click="applyCoupon" :disabled="!couponInfo?.valid">
        应用优惠券
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { posApi, type CouponVerifyResult } from '@/api/posApi'

interface Props {
  modelValue: boolean
  appliedCoupon: any | null
  originalTotalAmount: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'verify'): void
  /** 应用优惠券时触发，携带券码信息传递给父组件 */
  (e: 'apply', couponInfo: CouponVerifyResult): void
  (e: 'remove'): void
}>()

const couponForm = reactive({ code: '' })
const couponInfo = ref<CouponVerifyResult | null>(null)
const isVerifying = ref(false)
const couponInputRef = ref()

const verifyCoupon = async () => {
  if (isVerifying.value) return

  couponInfo.value = null

  if (!couponForm.code) {
    ElMessage.warning('请输入券码')
    return
  }

  isVerifying.value = true
  const code = couponForm.code.trim()

  try {
    // 响应拦截器已提取 data.data，result 即为 CouponVerifyResult
    const result = await posApi.verifyCoupon(code) as unknown as CouponVerifyResult
    couponInfo.value = result

    if (result.valid) {
      ElMessage.success('券码验证成功！')
    } else {
      ElMessage.error(result.message || '券码无效')
    }
  } catch (error) {
    couponInfo.value = {
      valid: false,
      message: '验证失败，请检查网络后重试'
    }
    ElMessage.error('券码验证失败，请检查网络后重试')
  } finally {
    isVerifying.value = false
  }

  emit('verify')
}

const applyCoupon = () => {
  if (!couponInfo.value?.valid) {
    ElMessage.warning('请先验证有效的券码')
    return
  }

  if (couponInfo.value.minAmount && props.originalTotalAmount < couponInfo.value.minAmount) {
    ElMessage.warning(`订单金额需满 ¥${couponInfo.value.minAmount} 才能使用此优惠券`)
    return
  }

  // 携带券码信息传递给父组件，由父组件调用核销 API
  emit('apply', couponInfo.value)
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    nextTick(() => {
      setTimeout(() => {
        couponInputRef.value?.focus()
      }, 100)
    })
  }
})
</script>

<style scoped>
.coupon-content {
  padding: 8px 0;
}

.coupon-info {
  margin-top: 20px;

  :deep(.el-alert) {
    border: 1px solid transparent;

    &.el-alert--error.is-light {
      background-color: #450a0a !important;
      border-color: #7f1d1d;

      .el-alert__title,
      .el-alert__title * {
        color: #fca5a5 !important;
      }
    }

    &.el-alert--success.is-light {
      background-color: #052e16 !important;
      border-color: #166534;

      .el-alert__title,
      .el-alert__title * {
        color: #86efac !important;
      }
    }

    .el-alert__content {
      padding: 8px 12px;
    }
  }
}

.coupon-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.coupon-desc {
  font-size: 14px;
  margin-bottom: 4px;
}

.coupon-valid {
  font-size: 12px;
  opacity: 0.8;
}

.applied-coupon {
  margin-top: 20px;
}

.coupon-dialog {
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

  :deep(.el-input-group__append) {
    background-color: var(--pos-primary) !important;
    border-color: var(--pos-primary) !important;

    .el-button {
      color: #fff !important;
      background-color: transparent !important;
      border: none !important;
    }
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

  :deep(.el-form-item__label) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-table th) {
    background-color: var(--pos-bg-secondary);
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-table td) {
    color: var(--pos-text-secondary) !important;
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
}
</style>
