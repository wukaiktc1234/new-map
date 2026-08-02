<script setup lang="ts">
/**
 * RequisitionForm - 物品领用申请表单
 *
 * 字段：物品类别 / 物品名称 / 领用数量 / 用途说明 / 期望领取时间
 */
import { reactive, computed } from 'vue'
import { ShoppingBag, Clock, EditPen, Box } from '@element-plus/icons-vue'
import FormSection from '../FormSection.vue'
import FormField from '../FormField.vue'
import type { RequisitionFormData } from '@/types/approval'
import { CategoryOptions } from '@/types/approval'
import { useScrollLock } from '@/composables/useScrollLock'

const { lock, unlock } = useScrollLock()

const props = withDefaults(defineProps<{
  /** 默认期望领取日期（下周） */
  defaultExpectedDate?: string
}>(), {
  defaultExpectedDate: '',
})

// ========== 表单数据 ==========
const form = reactive<RequisitionFormData>({
  category: '',
  itemName: '',
  quantity: 1,
  purpose: '',
  expectedDate: props.defaultExpectedDate,
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

  if (!form.category) {
    setError('category', '请选择物品类别')
    isValid = false
  }
  if (!form.itemName.trim()) {
    setError('itemName', '请填写物品名称')
    isValid = false
  }
  if (form.quantity < 1) {
    setError('quantity', '领用数量不能小于1')
    isValid = false
  }
  if (!form.purpose.trim()) {
    setError('purpose', '请填写用途说明')
    isValid = false
  }

  return isValid
}

// ========== 暴露方法 ==========
defineExpose({
  validate,
  getFormData: (): RequisitionFormData => ({ ...form }),
})
</script>

<template>
  <div class="requisition-form">
    <!-- 物品信息 -->
    <FormSection icon="ShoppingBag" title="物品信息" color="var(--fts-info)">
      <FormField label="物品类别" required :error="errors.category">
        <el-select
          v-model="form.category"
          placeholder="请选择物品类别"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          @change="clearError('category')"
        >
          <el-option
            v-for="opt in CategoryOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </FormField>

      <FormField label="物品名称" required :error="errors.itemName">
        <el-input
          v-model="form.itemName"
          placeholder="请输入需要领取的物品名称"
          @input="clearError('itemName')"
        >
          <template #prefix>
            <el-icon><Box /></el-icon>
          </template>
        </el-input>
      </FormField>

      <FormField label="领用数量" required :error="errors.quantity">
        <el-input-number
          v-model="form.quantity"
          :min="1"
          controls-position="right"
          style="width: 160px"
          @change="clearError('quantity')"
        />
      </FormField>
    </FormSection>

    <!-- 用途与时间 -->
    <FormSection icon="EditPen" title="用途说明" :subtitle="'最多200字'" color="var(--fts-warning)">
      <FormField label="用途说明" required :error="errors.purpose">
        <el-input
          v-model="form.purpose"
          type="textarea"
          :rows="3"
          placeholder="请说明领用用途..."
          maxlength="200"
          show-word-limit
          @input="clearError('purpose')"
        />
      </FormField>
    </FormSection>

    <!-- 领取时间（可选） -->
    <FormSection icon="Clock" title="期望时间" subtitle="可选，不填则按正常流程处理" color="var(--fts-primary)">
      <FormField label="期望领取日期">
        <el-date-picker
          v-model="form.expectedDate"
          type="date"
          placeholder="选择期望领取日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          :editable="false"
          @visible-change="(val: boolean) => val ? lock() : unlock()"
        />
      </FormField>
    </FormSection>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   RequisitionForm - 物品领用申请表单（视觉增强版）
   特性：数量选择器弹跳动画 + 物品类视觉增强
   ================================================================ */

.requisition-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

/* 数量选择器增强：按钮点击弹跳效果 */
.requisition-form :deep(.el-input-number) {
  .el-input-number__increase,
  .el-input-number__decrease {
    transition: transform var(--fts-duration-fast);

    &:active {
      transform: scale(0.9);
    }
  }

  &:focus-within .el-input__inner {
    animation: quantityPulse 0.4s ease;
  }
}

@keyframes quantityPulse {
  0%, 100% { box-shadow: 0 0 0 0 transparent; }
  50% { box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.15); }
}

</style>
