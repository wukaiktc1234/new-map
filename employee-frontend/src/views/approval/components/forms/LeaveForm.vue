<script setup lang="ts">
/**
 * LeaveForm - 请假申请表单（专业级重写版）
 *
 * 字段：请假类型 / 开始时间 / 结束时间 / 时长(自动计算+动画) / 请假原因
 *
 * 设计要点：
 * - 内置提交操作栏（正常流布局，不使用 sticky 避免与 TabBar 层级冲突）
 * - 假期余额参考卡片（数据联动预览）
 * - 时长数字滚动动画
 * - 日期选择器限制不能选过去的日期
 * - 所有颜色使用 --fts-* CSS 变量
 */
import { reactive, computed, ref, watch } from 'vue'
import { Tickets, Clock, EditPen, InfoFilled, WarningFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FormSection from '../FormSection.vue'
import FormField from '../FormField.vue'
import type { LeaveFormData } from '@/types/approval'
import { LeaveTypeOptions } from '@/types/approval'
import { useScrollLock } from '@/composables/useScrollLock'

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'cancel'): void
}>()

const props = withDefaults(defineProps<{
  /** 智能默认日期（明天） */
  defaultStartDate?: string
  /** 是否正在提交中 */
  submitting?: boolean
}>(), {
  defaultStartDate: '',
  submitting: false,
})

const { lock, unlock } = useScrollLock()

// ========== 表单数据 ==========
const form = reactive<LeaveFormData>({
  leaveType: '',
  startDate: props.defaultStartDate,
  endDate: '',
  duration: 0,
  reason: '',
})

// ========== 假期余额（mock 数据，后续对接 API） ==========
const leaveBalance = reactive({
  annualTotal: 13,
  annualRemaining: 8,
  sickBalance: 10,
  personalBalance: 5,
  compensatoryBalance: 16,
  yearUsedCount: 2,
  lastLeaveDate: '2026-04-10',
})

// ========== 字段级错误 ==========
const errors = reactive<Record<string, string>>({})

function clearError(field: string): void {
  delete errors[field]
}

function setError(field: string, message: string): void {
  errors[field] = message
}

// ========== 时长自动计算（排除周末） ==========
function calcDuration(): void {
  if (!form.startDate || !form.endDate) {
    form.duration = 0
    return
  }

  const start = new Date(form.startDate)
  const end = new Date(form.endDate)

  if (end < start) {
    form.duration = 0
    return
  }

  let count = 0
  const cur = new Date(start)
  while (cur <= end) {
    const dayOfWeek = cur.getDay()
    if (dayOfWeek !== 0 && dayOfWeek !== 6) {
      count++
    }
    cur.setDate(cur.getDate() + 1)
  }
  form.duration = count
}

// ========== 动画数字显示（时长 count-up 效果） ==========
const displayDuration = ref(0)

watch(() => form.duration, (newVal, oldVal) => {
  if (newVal === 0) {
    displayDuration.value = 0
    return
  }

  const startVal = oldVal || 0
  const diff = newVal - startVal
  const steps = Math.min(Math.abs(diff) * 10, 20)
  const stepValue = diff / steps
  let currentStep = 0

  const animate = () => {
    currentStep++
    if (currentStep <= steps) {
      displayDuration.value = Math.round((startVal + stepValue * currentStep) * 100) / 100
      requestAnimationFrame(animate)
    } else {
      displayDuration.value = newVal
    }
  }

  requestAnimationFrame(animate)
}, { immediate: true })

// ========== 验证 ==========
function validate(): boolean {
  Object.keys(errors).forEach(key => delete errors[key])

  let isValid = true

  if (!form.leaveType) {
    setError('leaveType', '请选择请假类型')
    isValid = false
  }
  if (!form.startDate) {
    setError('startDate', '请选择开始日期')
    isValid = false
  }
  if (!form.endDate) {
    setError('endDate', '请选择结束日期')
    isValid = false
  } else if (form.duration <= 0) {
    setError('endDate', '结束日期必须晚于开始日期')
    isValid = false
  }
  if (!form.reason.trim()) {
    setError('reason', '请填写请假原因')
    isValid = false
  }

  return isValid
}

// ========== 暴露方法 ==========
defineExpose({
  validate,
  getFormData: (): LeaveFormData => ({ ...form }),
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
  <div class="leave-form">
    <!-- 假期余额参考卡片 -->
    <section class="balance-card">
      <div class="balance-card__header">
        <el-icon :size="14"><InfoFilled /></el-icon>
        假期余额参考
      </div>
      <div class="balance-grid">
        <div class="balance-item">
          <span class="balance-item__label">年假</span>
          <span class="balance-item__value">
            {{ leaveBalance.annualRemaining }}<small>/{{ leaveBalance.annualTotal }}</small>
          </span>
          <span class="balance-item__unit">天</span>
        </div>
        <div class="balance-item">
          <span class="balance-item__label">病假</span>
          <span class="balance-item__value">{{ leaveBalance.sickBalance }}<small>天</small></span>
        </div>
        <div class="balance-item">
          <span class="balance-item__label">事假</span>
          <span class="balance-item__value">{{ leaveBalance.personalBalance }}<small>天</small></span>
        </div>
        <div class="balance-item">
          <span class="balance-item__label">调休</span>
          <span class="balance-item__value">{{ leaveBalance.compensatoryBalance }}<small>小时</small></span>
        </div>
      </div>

      <!-- 年假余额预警 -->
      <div
        v-if="form.duration > 0 && form.leaveType === '年假' && leaveBalance.annualRemaining - form.duration <= 2"
        class="balance-warning"
      >
        <el-icon :size="13"><WarningFilled /></el-icon>
        年假余额紧张，本次申请后仅剩 {{ Math.max(0, leaveBalance.annualRemaining - form.duration) }} 天
      </div>

      <p class="balance-footer">
        本年度已请 {{ leaveBalance.yearUsedCount }} 次 · 上次请假：{{ leaveBalance.lastLeaveDate || '无记录' }}
      </p>
    </section>

    <!-- 基本信息 -->
    <FormSection icon="Tickets" title="基本信息" color="var(--fts-warning)">
      <FormField label="请假类型" required :error="errors.leaveType">
        <el-select
          v-model="form.leaveType"
          placeholder="请选择请假类型"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          @change="clearError('leaveType')"
        >
          <el-option
            v-for="opt in LeaveTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.label"
          />
        </el-select>
      </FormField>
    </FormSection>

    <!-- 时间信息 -->
    <FormSection icon="Clock" title="时间信息" color="var(--fts-primary)">
      <!-- 紧凑型日期范围行 -->
      <div class="date-range-row">
        <div class="date-field">
          <label class="date-label">
            开始日期<span class="date-required">*</span>
          </label>
          <el-date-picker
            v-model="form.startDate"
            type="date"
            placeholder="开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            popper-class="fts-popover"
            :teleported="false"
            style="width: 100%"
            :editable="false"
            :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000"
            @change="calcDuration(); clearError('startDate')"
            @visible-change="(val: boolean) => val ? lock() : unlock()"
          />
          <p v-if="errors.startDate" class="field-error">{{ errors.startDate }}</p>
        </div>

        <div class="date-separator" aria-hidden="true">至</div>

        <div class="date-field">
          <label class="date-label">
            结束日期<span class="date-required">*</span>
          </label>
          <el-date-picker
            v-model="form.endDate"
            type="date"
            placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            popper-class="fts-popover"
            :teleported="false"
            style="width: 100%"
            :editable="false"
            :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000"
            @change="calcDuration(); clearError('endDate')"
            @visible-change="(val: boolean) => val ? lock() : unlock()"
          />
          <p v-if="errors.endDate" class="field-error">{{ errors.endDate }}</p>
        </div>
      </div>

      <!-- 时长标签（带数字动画） -->
      <Transition name="fade-slide">
        <div v-if="form.duration > 0" class="duration-badge">
          <div class="duration-badge__icon">
            <el-icon><Clock /></el-icon>
          </div>
          <span class="duration-badge__text">
            请假时长：<strong>{{ displayDuration }}</strong> 天
            <span class="duration-badge__note">（已排除周末）</span>
          </span>
        </div>
      </Transition>
    </FormSection>

    <!-- 详细说明 -->
    <FormSection icon="EditPen" title="详细说明" subtitle="最多200字" color="var(--fts-success)">
      <FormField label="请假原因" required :error="errors.reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          placeholder="请详细说明请假原因..."
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
   LeaveForm - 请假申请表单（专业级重写版）
   ================================================================ */

.leave-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// ----- 假期余额参考卡片 -----
.balance-card {
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);
}

.balance-card__header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-primary);
  margin-bottom: var(--fts-space-3);

  .el-icon { flex-shrink: 0; }
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);

  @media (max-width: 480px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.balance-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-sm);
  border: 1px solid var(--fts-border-secondary);
}

.balance-item__label {
  font-size: 10px;
  color: var(--fts-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.balance-item__value {
  font-size: 20px;
  font-weight: 700;
  color: var(--fts-text-primary);
  line-height: 1.2;

  small {
    font-size: var(--fts-font-size-xs);
    font-weight: 400;
    color: var(--fts-text-tertiary);
  }
}

.balance-item__unit {
  font-size: 10px;
  color: var(--fts-text-secondary);
}

.balance-warning {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: rgba(var(--fts-warning-rgb), 0.06);
  border: 1px solid rgba(var(--fts-warning-rgb), 0.15);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-warning-dark, #92400e);
  margin-bottom: var(--fts-space-2);

  .el-icon { flex-shrink: 0; }
}

.balance-footer {
  font-size: 11px;
  color: var(--fts-text-tertiary);
  text-align: center;
  margin: 0;
}

// ----- 日期范围行 -----
.date-range-row {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-2);
}

.date-field {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.date-label {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-tertiary);
  line-height: var(--fts-line-height-normal);
  margin: 0;
  white-space: nowrap;
}

.date-required {
  color: var(--fts-error);
  margin-left: 2px;
}

.field-error {
  margin: 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-error);
  line-height: var(--fts-line-height-normal);
}

.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  min-width: 28px;
  padding-top: 22px;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  user-select: none;
  flex-shrink: 0;

  @media (max-width: 480px) {
    min-width: 20px;
    padding-top: 20px;
    font-size: var(--fts-font-size-xs);
  }
}

// ----- 时长标签 -----
.duration-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: linear-gradient(
    135deg,
    rgba(var(--fts-primary-rgb), 0.08) 0%,
    rgba(var(--fts-primary-rgb), 0.03) 100%
  );
  border: 1px solid rgba(var(--fts-primary-rgb), 0.12);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  line-height: var(--fts-line-height-normal);
  margin-top: var(--fts-space-2);
}

.duration-badge__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--fts-radius-sm);
  background: var(--fts-primary);
  color: #fff;

  .el-icon { font-size: 14px; }
}

.duration-badge__text strong {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  margin: 0 2px;
}

.duration-badge__note {
  font-size: var(--fts-font-size-xs);
  opacity: 0.7;
}

// ----- 过渡动画 -----
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all var(--fts-duration-normal) var(--fts-easing-default);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

// ----- 样式结束 -----
</style>
