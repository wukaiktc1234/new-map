<script setup lang="ts">
/**
 * TravelForm - 出差申请表单
 *
 * 字段：出差地点 / 出差时间(范围) / 交通方式 / 预算金额 / 出差事由
 */
import { reactive, computed } from 'vue'
import { Location, Timer, MapLocation, EditPen } from '@element-plus/icons-vue'
import FormSection from '../FormSection.vue'
import FormField from '../FormField.vue'
import type { TravelFormData } from '@/types/approval'
import { TransportOptions } from '@/types/approval'
import { useScrollLock } from '@/composables/useScrollLock'

const { lock, unlock } = useScrollLock()

const props = withDefaults(defineProps<{
  /** 默认开始日期（后天） */
  defaultStartDate?: string
}>(), {
  defaultStartDate: '',
})

// ========== 表单数据 ==========
const form = reactive<TravelFormData>({
  destination: '',
  startDate: props.defaultStartDate,
  endDate: '',
  transportMethod: '',
  budgetAmount: 0,
  reason: '',
})

/** 出差日期范围（双向绑定 el-date-picker daterange） */
const dateRange = computed({
  get: () => [form.startDate, form.endDate],
  set: (val: [string, string]) => {
    form.startDate = val?.[0] || ''
    form.endDate = val?.[1] || ''
  },
})

// ========== 字段级错误 ==========
const errors = reactive<Record<string, string>>({})

function clearError(field: string): void {
  delete errors[field]
}

function setError(field: string, message: string): void {
  errors[field] = message
}

// ========== 验证 ==========
function validate(): boolean {
  Object.keys(errors).forEach(key => delete errors[key])

  let isValid = true

  if (!form.destination.trim()) {
    setError('destination', '请填写出差地点')
    isValid = false
  }
  if (!form.startDate || !form.endDate) {
    setError('dateRange', '请选择完整的出差时间范围')
    isValid = false
  }
  if (!form.transportMethod) {
    setError('transportMethod', '请选择交通方式')
    isValid = false
  }
  if (form.budgetAmount <= 0) {
    setError('budgetAmount', '预算金额必须大于0')
    isValid = false
  }
  if (!form.reason.trim()) {
    setError('reason', '请填写出差事由')
    isValid = false
  }

  return isValid
}

// ========== 暴露方法 ==========
defineExpose({
  validate,
  getFormData: (): TravelFormData => ({ ...form }),
})
</script>

<template>
  <div class="travel-form">
    <!-- 基本信息 -->
    <FormSection icon="Location" title="基本信息" color="var(--fts-success)">
      <FormField label="出差地点" required :error="errors.destination">
        <el-input
          v-model="form.destination"
          placeholder="请输入出差目的地城市"
          @input="clearError('destination')"
        >
          <template #prefix>
            <el-icon><MapLocation /></el-icon>
          </template>
        </el-input>
      </FormField>
    </FormSection>

    <!-- 时间与交通 -->
    <FormSection icon="Timer" title="时间与交通" color="var(--fts-primary)">
      <FormField label="出差时间" required :error="errors.dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          :editable="false"
          @visible-change="(val: boolean) => val ? lock() : unlock()"
        />
      </FormField>

      <FormField label="交通方式" required :error="errors.transportMethod">
        <el-select
          v-model="form.transportMethod"
          placeholder="请选择交通方式"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          @change="clearError('transportMethod')"
        >
          <el-option
            v-for="opt in TransportOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </FormField>

      <FormField label="预算金额（元）" required :error="errors.budgetAmount">
        <el-input-number
          v-model="form.budgetAmount"
          :min="1"
          :precision="2"
          :controls="false"
          controls-position="right"
          style="width: 100%"
          @change="clearError('budgetAmount')"
        />
      </FormField>
    </FormSection>

    <!-- 出差事由 -->
    <FormSection icon="EditPen" title="出差事由" :subtitle="'最多300字'" color="var(--fts-warning)">
      <FormField label="事由说明" required :error="errors.reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          placeholder="请详细说明出差事由..."
          maxlength="300"
          show-word-limit
          @input="clearError('reason')"
        />
      </FormField>
    </FormSection>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   TravelForm - 出差申请表单（视觉增强版）
   特性：位置图标动画 + 预算金额视觉增强
   ================================================================ */

.travel-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

/* 全局样式增强：输入框聚焦时的位置图标动画 */
.travel-form :deep(.el-input__prefix) {
  .el-icon {
    transition: all var(--fts-duration-fast) var(--fts-easing-default);
  }

  &:focus-within .el-icon {
    color: var(--fts-primary);
    animation: locationBounce 0.6s ease;
  }
}

@keyframes locationBounce {
  0%, 100% { transform: translateY(0); }
  25% { transform: translateY(-3px); }
  50% { transform: translateY(0); }
  75% { transform: translateY(-1px); }
}
</style>
