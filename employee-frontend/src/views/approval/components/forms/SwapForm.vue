<script setup lang="ts">
/**
 * SwapForm - 调班换班申请表单（专业级重写版）
 *
 * 字段：调班类型 / 原班次 / 目标班次或交换对象 / 调班原因
 *
 * 设计要点：
 * - 内置提交操作栏（正常流布局，不使用 sticky 避免与 TabBar 层级冲突）
 * - 调班类型分段控件（与他人交换 / 申请调整）
 * - 类型切换动态显示不同字段组
 * - 班次信息卡片式展示
 */
import { reactive, computed } from 'vue'
import { RefreshRight, Calendar, EditPen, User, SetUp, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FormSection from '../FormSection.vue'
import FormField from '../FormField.vue'
import type { SwapFormData } from '@/types/approval'

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'cancel'): void
}>()

const props = withDefaults(defineProps<{
  /** 是否正在提交中 */
  submitting?: boolean
}>(), {
  submitting: false,
})

// ========== 表单数据 ==========
const form = reactive<SwapFormData>({
  swapType: 'exchange',
  originalShift: '',
  targetShift: '',
  exchangeWith: '',
  reason: '',
})

// ========== 字段级错误 ==========
const errors = reactive<Record<string, string>>({})

function clearError(field: string): void {
  delete errors[field]
}

function setError(field: string, message: string): void {
  errors[field] = message
}

/** 根据调班类型动态显示的字段标签 */
const targetFieldLabel = computed(() =>
  form.swapType === 'exchange' ? '交换对象' : '目标班次',
)

const targetFieldPlaceholder = computed(() =>
  form.swapType === 'exchange'
    ? '请输入交换对象的姓名'
    : '请输入期望调整的班次时间和时段',
)

const targetFieldValue = computed({
  get: () => form.swapType === 'exchange' ? form.exchangeWith : form.targetShift,
  set: (val: string) => {
    if (form.swapType === 'exchange') {
      form.exchangeWith = val
    } else {
      form.targetShift = val
    }
  },
})

// ========== 调班类型选项 ==========
const swapTypeOptions = [
  { label: '与他人交换', value: 'exchange' as const, icon: User },
  { label: '申请调整', value: 'adjust' as const, icon: SetUp },
]

// ========== 验证 ==========
function validate(): boolean {
  Object.keys(errors).forEach(key => delete errors[key])

  let isValid = true

  if (!form.originalShift.trim()) {
    setError('originalShift', '请填写原班次信息')
    isValid = false
  }
  if (!targetFieldValue.value.trim()) {
    setError('targetField', `请填写${targetFieldLabel.value}`)
    isValid = false
  }
  if (form.swapType === 'exchange' && !form.exchangeWith?.trim()) {
    setError('targetField', '请填写交换对象姓名')
    isValid = false
  }
  if (!form.reason.trim()) {
    setError('reason', '请填写调班原因')
    isValid = false
  }

  return isValid
}

// ========== 暴露方法 ==========
defineExpose({
  validate,
  getFormData: (): SwapFormData => ({ ...form }),
})

// ========== 提交/取消 ==========
function handleSubmit(): void {
  if (validate()) {
    emit('submit')
  } else {
    ElMessage.warning('请完善必填信息')
  }
}

function handleCancel(): void {
  emit('cancel')
}
</script>

<template>
  <div class="swap-form">
    <!-- 调班类型选择 -->
    <FormSection icon="RefreshRight" title="调班类型" color="var(--fts-primary)">
      <FormField label="调班方式">
        <div class="swap-type-selector">
          <button
            v-for="opt in swapTypeOptions"
            :key="opt.value"
            class="swap-type-btn"
            :class="{ 'swap-type-btn--active': form.swapType === opt.value }"
            @click="form.swapType = opt.value; clearError('targetField')"
          >
            <el-icon><component :is="opt.icon" /></el-icon>
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </FormField>
    </FormSection>

    <!-- 班次信息 -->
    <FormSection icon="Calendar" title="班次信息" color="var(--fts-primary)">
      <FormField label="原班次" required :error="errors.originalShift">
        <el-input
          v-model="form.originalShift"
          placeholder="如：2026-06-15 早班（08:00-16:00）"
          @input="clearError('originalShift')"
        />
      </FormField>

      <FormField :label="targetFieldLabel" required :error="errors.targetField">
        <el-input
          v-model="targetFieldValue"
          :placeholder="targetFieldPlaceholder"
          @input="clearError('targetField')"
        />
      </FormField>

      <!-- 班次预览卡片（填写后展示） -->
      <div v-if="form.originalShift || targetFieldValue" class="shift-preview-card">
        <div class="shift-preview-row">
          <span class="shift-preview-label">当前班次</span>
          <span class="shift-preview-value">{{ form.originalShift || '--' }}</span>
        </div>
        <div class="shift-preview-divider" />
        <div class="shift-preview-row">
          <span class="shift-preview-label">{{ form.swapType === 'exchange' ? '交换对象' : '目标班次' }}</span>
          <span class="shift-preview-value shift-preview-value--highlight">
            {{ targetFieldValue || '--' }}
          </span>
        </div>
      </div>
    </FormSection>

    <!-- 详细说明 -->
    <FormSection icon="EditPen" title="详细说明" subtitle="最多200字" color="var(--fts-success)">
      <FormField label="调班原因" required :error="errors.reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          placeholder="请填写调班原因..."
          maxlength="200"
          show-word-limit
          @input="clearError('reason')"
        />
      </FormField>
    </FormSection>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   SwapForm - 调班换班表单（专业级重写版）
   ================================================================ */

.swap-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// ----- 调班类型分段控件 -----
.swap-type-selector {
  display: flex;
  gap: var(--fts-space-2);
}

.swap-type-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-page);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  cursor: pointer;
  transition: all var(--fts-duration-normal) var(--fts-ease-out);
  position: relative;
  overflow: hidden;
  min-height: 44px;

  /* Morphing 背景 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, var(--fts-primary), var(--fts-primary-light));
    opacity: 0;
    transition: opacity var(--fts-duration-normal) var(--fts-easing-default);
  }

  @media (max-width: 767px) {
    padding: var(--fts-space-3);
    font-size: var(--fts-font-size-xs);
  }

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-secondary);

    .el-icon { transform: scale(1.1); }
  }

  &--active {
    border-color: var(--fts-primary);
    color: var(--fts-text-on-primary);
    font-weight: var(--fts-font-weight-semibold);
    box-shadow:
      0 4px 16px rgba(var(--fts-primary-rgb), 0.3),
      0 0 0 1px rgba(var(--fts-primary-rgb), 0.2);

    &::before { opacity: 1; }

    .el-icon {
      color: var(--fts-text-on-primary);
      animation: iconBounce 0.6s ease;
    }
  }

  > span,
  .el-icon {
    position: relative;
    z-index: 1;
  }

  .el-icon {
    font-size: 16px;
    transition: all var(--fts-duration-fast) var(--fts-easing-default);
  }
}

@keyframes iconBounce {
  0% { transform: scale(1); }
  30% { transform: scale(1.3); }
  50% { transform: scale(0.9); }
  70% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

// ----- 班次预览卡片 -----
.shift-preview-card {
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: linear-gradient(
    135deg,
    rgba(var(--fts-primary-rgb), 0.04) 0%,
    rgba(var(--fts-primary-rgb), 0.01) 100%
  );
  border: 1px solid rgba(var(--fts-primary-rgb), 0.12);
  border-radius: var(--fts-radius-md);
}

.shift-preview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-2) 0;
}

.shift-preview-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.shift-preview-value {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);

  &--highlight {
    color: var(--fts-primary);
  }
}

.shift-preview-divider {
  height: 1px;
  background: var(--fts-border-secondary);
  margin: var(--fts-space-1) 0;
}

// ----- 样式结束 -----
</style>
