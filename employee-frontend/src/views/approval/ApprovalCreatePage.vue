<script setup lang="ts">
/**
 * ApprovalCreatePage - 发起申请页面
 *
 * 设计要点：
 * - 使用 PageContainer #footer slot 管理提交操作栏（Element Plus 原生按钮）
 * - Hero Banner 渐变背景 + 类型图标
 * - 紧急程度分段控件
 *
 * @last-modified 2026-06-06
 */
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, Tickets, AlarmClock, RefreshRight, Location, Wallet, ShoppingBag } from '@element-plus/icons-vue'
import { PageContainer } from '@/components/core'
import { useApprovalForm } from '@/composables/useApprovalForm'
import type { ApprovalType, AnyApprovalFormData } from '@/types/approval'
import { ApprovalTypeLabels, UrgencyOptions } from '@/types/approval'

// 同步导入6个表单组件
import LeaveForm from './components/forms/LeaveForm.vue'
import OvertimeForm from './components/forms/OvertimeForm.vue'
import SwapForm from './components/forms/SwapForm.vue'
import TravelForm from './components/forms/TravelForm.vue'
import ReimbursementForm from './components/forms/ReimbursementForm.vue'
import RequisitionForm from './components/forms/RequisitionForm.vue'

const route = useRoute()

// ========== 审批类型 ==========
const approvalType = computed<ApprovalType>(() =>
  (route.params.type as ApprovalType) || 'leave',
)
const typeLabel = computed(() => ApprovalTypeLabels[approvalType.value])

// ========== 类型图标映射 ==========
const typeIconMap: Record<ApprovalType, typeof Document> = {
  leave: Tickets,
  overtime: AlarmClock,
  swap: RefreshRight,
  travel: Location,
  reimbursement: Wallet,
  requisition: ShoppingBag,
}
const currentTypeIcon = computed(() => typeIconMap[approvalType.value] || Document)

// ========== 表单引用 ==========
const formRef = ref<InstanceType<typeof LeaveForm> | InstanceType<typeof OvertimeForm> | InstanceType<typeof SwapForm> | InstanceType<typeof TravelForm> | InstanceType<typeof ReimbursementForm> | InstanceType<typeof RequisitionForm>>()

const {
  urgency,
  submitting,
  defaultDates,
  handleSubmit,
  handleCancel,
} = useApprovalForm({
  type: approvalType.value,
  getFormData: () => formRef.value?.getFormData() ?? {} as AnyApprovalFormData,
  validate: () => formRef.value?.validate() ?? false,
})

// ========== 表单组件映射 ==========
const formComponentMap: Record<ApprovalType, typeof LeaveForm | typeof OvertimeForm | typeof SwapForm | typeof TravelForm | typeof ReimbursementForm | typeof RequisitionForm> = {
  leave: LeaveForm,
  overtime: OvertimeForm,
  swap: SwapForm,
  travel: TravelForm,
  reimbursement: ReimbursementForm,
  requisition: RequisitionForm,
}

const currentFormComponent = computed(() => formComponentMap[approvalType.value])
</script>

<template>
  <PageContainer :title="`发起${typeLabel}`" :sticky-footer="true">
    <!-- Hero 引导卡片 -->
    <section class="hero-card">
      <div class="hero-card__icon-wrap">
        <el-icon :size="22" class="hero-card__icon"><component :is="currentTypeIcon" /></el-icon>
      </div>
      <div class="hero-card__text">
        <h2 class="hero-card__title">发起{{ typeLabel }}</h2>
        <p class="hero-card__desc">填写以下信息，提交后将进入审批流程</p>
      </div>
    </section>

    <!-- 紧急程度选择器 -->
    <section class="urgency-section">
      <div class="urgency-selector">
        <button
          v-for="opt in UrgencyOptions"
          :key="opt.value"
          class="urgency-btn"
          :class="{ 'urgency-btn--active': urgency === opt.value, [`urgency-btn--${opt.value}`]: true }"
          @click="urgency = opt.value"
        >
          <span class="urgency-btn__dot" />
          {{ opt.label }}
        </button>
      </div>
    </section>

    <!-- 动态表单区域 -->
    <component
      :is="currentFormComponent"
      ref="formRef"
      :default-start-date="defaultDates.tomorrow"
      :default-date="defaultDates.today"
      :default-expected-date="defaultDates.nextWeek"
      :submitting="submitting"
      @submit="handleSubmit"
      @cancel="handleCancel"
    />

    <!-- 提交操作栏（Element Plus 原生按钮） -->
    <template #footer>
      <el-button size="large" @click="handleCancel">取消</el-button>
      <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">确认提交</el-button>
    </template>
  </PageContainer>
</template>

<style scoped lang="scss">
/* ================================================================
   ApprovalCreatePage - 发起申请页

   设计原则：
   - 使用 PageContainer #footer slot + stickyFooter 管理提交操作栏
   - 操作栏使用 Element Plus 原生 el-button（专业外观）
   - 所有颜色/间距/圆角均使用 --fts-* CSS 变量
   ================================================================ */

// ----- Hero 引导卡片 -----
.hero-card {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  padding: var(--fts-space-5) var(--fts-space-5);
  border-radius: var(--fts-radius-lg);
  background: linear-gradient(135deg, var(--fts-primary-lighter), var(--fts-bg-card));
  border: 1px solid var(--fts-border-primary);
  position: relative;
  overflow: hidden;

  /* 顶部高光线 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(var(--fts-primary-rgb), 0.5) 50%,
      transparent 100%
    );
  }
}

.hero-card__icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  min-width: 44px;
  border-radius: var(--fts-radius-md);
  background: linear-gradient(135deg, var(--fts-primary), var(--fts-primary-dark));
  color: var(--fts-text-on-primary);
  flex-shrink: 0;
}

.hero-card__text {
  min-width: 0;
}

.hero-card__title {
  margin: 0 0 var(--fts-space-1);
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  line-height: var(--fts-line-height-tight);
}

.hero-card__desc {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: var(--fts-line-height-normal);
}

// ----- 紧急程度选择器 -----
.urgency-section {
  margin-bottom: var(--fts-space-4);
}

.urgency-selector {
  display: flex;
  gap: var(--fts-space-2);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-1);
}

.urgency-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  border: none;
  border-radius: var(--fts-radius-md);
  background: transparent;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  cursor: pointer;
  transition: all var(--fts-duration-normal) var(--fts-easing-out);
  position: relative;
  min-height: 44px;

  &:hover {
    background: var(--fts-bg-secondary);
  }

  &--active {
    font-weight: var(--fts-font-weight-semibold);

    .urgency-btn__dot {
      opacity: 1;
      transform: scale(1);
    }
  }

  &--normal.urgency-btn--active {
    color: var(--fts-text-primary);
    background: var(--fts-bg-tertiary);
    box-shadow: var(--fts-shadow-sm);
  }

  &--urgent.urgency-btn--active {
    color: var(--fts-warning-dark, #92400e);
    background: rgba(var(--fts-warning-rgb), 0.10);
    box-shadow: var(--fts-shadow-sm);
  }
}

.urgency-btn__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  opacity: 0.5;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);
  transform: scale(0.8);
  flex-shrink: 0;
}

.urgency-btn--normal .urgency-btn__dot {
  background: var(--fts-text-tertiary);
}

.urgency-btn--urgent .urgency-btn__dot {
  background: var(--fts-warning);
}
</style>
