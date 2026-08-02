<script setup lang="ts">
/**
 * ReimbursementForm - 费用报销申请表单（专业级重写版）
 *
 * 字段：报销类型 / 费用明细(可增删卡片列表) / 总金额(自动汇总+动画) / 收款方式 / 报销说明
 *
 * 设计要点：
 * - 内置提交操作栏（正常流布局，不使用 sticky 避免与 TabBar 层级冲突）
 * - 费用明细使用移动端友好的卡片列表（替代 el-table）
 * - 金额输入自动格式化，总金额实时汇总动画
 * - 添加按钮带 hover 交互效果
 */
import { reactive, ref, watch } from 'vue'
import { Plus, Delete, Wallet, CreditCard, EditPen, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FormSection from '../FormSection.vue'
import FormField from '../FormField.vue'
import type { ReimbursementFormData, ExpenseItem } from '@/types/approval'
import { ExpenseTypeOptions, PaymentOptions } from '@/types/approval'
import { useScrollLock } from '@/composables/useScrollLock'

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

const { lock, unlock } = useScrollLock()

// ========== 表单数据 ==========
const form = reactive<ReimbursementFormData>({
  reimbursementType: '',
  expenses: [],
  totalAmount: 0,
  paymentMethod: '',
  bankAccount: '',
  description: '',
})

// ========== 字段级错误 ==========
const errors = reactive<Record<string, string>>({})

function clearError(field: string): void {
  delete errors[field]
}

function setError(field: string, message: string): void {
  errors[field] = message
}

// ========== 费用明细操作 ==========
function addExpense(): void {
  const newItem: ExpenseItem = {
    id: `exp_${Date.now()}`,
    expenseType: '',
    date: '',
    amount: 0,
    remark: '',
  }
  form.expenses.push(newItem)
  clearError('expenses')
}

function removeExpense(id: string): void {
  const idx = form.expenses.findIndex(e => e.id === id)
  if (idx >= 0) {
    form.expenses.splice(idx, 1)
    calcTotal()
  }
}

/** 计算总金额 */
function calcTotal(): void {
  form.totalAmount = form.expenses.reduce((sum, item) => sum + (item.amount || 0), 0)
}

// ========== 动画总金额显示 ==========
const displayTotal = ref(0)

watch(() => form.totalAmount, (newVal, oldVal) => {
  if (newVal === 0) {
    displayTotal.value = 0
    return
  }

  const startVal = oldVal || 0
  const diff = newVal - startVal
  const steps = Math.min(Math.abs(diff) * 2, 30)
  const stepValue = diff / steps
  let currentStep = 0

  const animate = () => {
    currentStep++
    if (currentStep <= steps) {
      displayTotal.value = Math.round((startVal + stepValue * currentStep) * 100) / 100
      requestAnimationFrame(animate)
    } else {
      displayTotal.value = newVal
    }
  }

  requestAnimationFrame(animate)
}, { immediate: true })

// ========== 验证 ==========
function validate(): boolean {
  Object.keys(errors).forEach(key => delete errors[key])

  let isValid = true

  if (!form.reimbursementType) {
    setError('reimbursementType', '请选择报销类型')
    isValid = false
  }

  if (form.expenses.length === 0) {
    setError('expenses', '请添加至少一条费用明细')
    isValid = false
  } else {
    for (let i = 0; i < form.expenses.length; i++) {
      const exp = form.expenses[i]
      if (!exp.expenseType) {
        setError(`expense_${i}_type`, `第${i + 1}项：请选择费用类型`)
        isValid = false
      }
      if (!exp.amount || exp.amount <= 0) {
        setError(`expense_${i}_amount`, `第${i + 1}项：金额必须大于0`)
        isValid = false
      }
    }
  }

  if (!form.paymentMethod) {
    setError('paymentMethod', '请选择收款方式')
    isValid = false
  }
  if (form.paymentMethod === 'bank' && !form.bankAccount?.trim()) {
    setError('bankAccount', '请填写银行账号')
    isValid = false
  }
  if (!form.description.trim()) {
    setError('description', '请填写报销说明')
    isValid = false
  }

  return isValid
}

// ========== 暴露方法 ==========
defineExpose({
  validate,
  getFormData: (): ReimbursementFormData => ({ ...form }),
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
  <div class="reimbursement-form">
    <!-- 基本信息 -->
    <FormSection icon="Wallet" title="基本信息" color="var(--fts-error)">
      <FormField label="报销类型" required :error="errors.reimbursementType">
        <el-select
          v-model="form.reimbursementType"
          placeholder="请选择报销类型"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          @change="clearError('reimbursementType')"
        >
          <el-option
            v-for="opt in ExpenseTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.label"
          />
        </el-select>
      </FormField>
    </FormSection>

    <!-- 费用明细（卡片列表） -->
    <FormSection icon="CreditCard" title="费用明细" subtitle="逐笔填写，支持多条" color="var(--fts-primary)">
      <!-- 区块级错误提示 -->
      <p v-if="errors.expenses" class="section-error">{{ errors.expenses }}</p>

      <!-- 卡片列表（带过渡动画） -->
      <TransitionGroup name="expense-list" tag="div" class="expense-list">
        <div
          v-for="(item, index) in form.expenses"
          :key="item.id"
          class="expense-card"
        >
          <!-- 卡片头部：类型 + 删除 -->
          <div class="expense-card__header">
            <el-select
              v-model="item.expenseType"
              placeholder="费用类型"
              popper-class="fts-popover"
              :teleported="false"
              size="small"
              style="flex: 1; min-width: 0"
              @change="clearError(`expense_${index}_type`)"
            >
              <el-option
                v-for="opt in ExpenseTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.label"
              />
            </el-select>
            <button
              type="button"
              class="expense-card__delete"
              title="删除此条"
              @click="removeExpense(item.id)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>

          <!-- 卡片主体：日期 + 金额 -->
          <div class="expense-card__body">
            <div class="expense-field">
              <span class="expense-label">日期</span>
              <el-date-picker
                v-model="item.date"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                popper-class="fts-popover"
                :teleported="false"
                size="small"
                style="width: 100%"
                :editable="false"
                @visible-change="(val: boolean) => val ? lock() : unlock()"
              />
            </div>
            <div class="expense-field">
              <span class="expense-label">金额</span>
              <el-input-number
                v-model="item.amount"
                :min="0.01"
                :precision="2"
                :controls="false"
                size="small"
                style="width: 100%"
                @change="calcTotal"
              />
            </div>
          </div>

          <!-- 备注 -->
          <el-input
            v-model="item.remark"
            placeholder="备注（选填）"
            size="small"
          />
        </div>
      </TransitionGroup>

      <!-- 添加按钮 -->
      <button type="button" class="add-btn" @click="addExpense">
        <el-icon><Plus /></el-icon>
        添加费用明细
      </button>

      <!-- 总计显示（带数字动画） -->
      <Transition name="fade-slide">
        <div v-if="form.totalAmount > 0" class="total-bar">
          <span class="total-bar__label">合计</span>
          <strong class="total-bar__amount">&yen;{{ displayTotal.toFixed(2) }}</strong>
        </div>
      </Transition>
    </FormSection>

    <!-- 收款信息 -->
    <FormSection icon="CreditCard" title="收款信息" color="var(--fts-success)">
      <FormField label="收款方式" required :error="errors.paymentMethod">
        <el-select
          v-model="form.paymentMethod"
          placeholder="请选择收款方式"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          @change="clearError('paymentMethod')"
        >
          <el-option
            v-for="opt in PaymentOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </FormField>

      <FormField
        v-if="form.paymentMethod === 'bank'"
        label="银行账号"
        required
        :error="errors.bankAccount"
      >
        <el-input
          v-model="form.bankAccount"
          placeholder="请输入银行账号"
          @input="clearError('bankAccount')"
        />
      </FormField>
    </FormSection>

    <!-- 报销说明 -->
    <FormSection icon="EditPen" title="报销说明" subtitle="最多300字" color="var(--fts-warning)">
      <FormField label="详细说明" required :error="errors.description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请填写报销说明..."
          maxlength="300"
          show-word-limit
          @input="clearError('description')"
        />
      </FormField>
    </FormSection>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   ReimbursementForm - 费用报销表单（专业级重写版）
   ================================================================ */

.reimbursement-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// ----- 区块级错误提示 -----
.section-error {
  margin: 0;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: rgba(var(--fts-error-rgb), 0.06);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-error);
}

// ----- 费用卡片列表 -----
.expense-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.expense-card {
  background: var(--fts-bg-page);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  transition:
    border-color var(--fts-duration-fast),
    box-shadow var(--fts-duration-fast),
    transform var(--fts-duration-fast);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-sm);
    transform: translateY(-2px);

    @media (max-width: 767px) {
      transform: none;
    }
  }
}

.expense-card__header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.expense-card__delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  min-width: 28px;
  border: none;
  border-radius: var(--fts-radius-xs);
  background: transparent;
  color: var(--fts-text-tertiary);
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  min-height: 28px;

  &:hover {
    background: rgba(var(--fts-error-rgb), 0.1);
    color: var(--fts-error);
  }
}

.expense-card__body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.expense-field {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.expense-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

// ----- 添加按钮 -----
.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  width: 100%;
  padding: var(--fts-space-3);
  border: 1px dashed var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  background: transparent;
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-sm);
  cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);
  position: relative;
  overflow: hidden;
  min-height: 44px;

  &:hover {
    border-color: var(--fts-primary);
    color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.04);
  }

  .el-icon {
    font-size: 16px;
    transition: transform var(--fts-duration-fast);
  }

  &:hover .el-icon {
    transform: rotate(90deg);
  }
}

// ----- 总计栏 -----
.total-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-4) var(--fts-space-5);
  background: linear-gradient(
    135deg,
    rgba(var(--fts-error-rgb), 0.08) 0%,
    rgba(var(--fts-error-rgb), 0.04) 100%
  );
  border: 1px solid rgba(var(--fts-error-rgb), 0.15);
  border-radius: var(--fts-radius-md);
  margin-top: var(--fts-space-3);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.08),
      transparent
    );
    animation: totalShimmer 3s infinite;
  }

  > * { position: relative; z-index: 1; }
}

@keyframes totalShimmer {
  100% { transform: translateX(100%); }
}

.total-bar__label {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-secondary);
}

.total-bar__amount {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-error);
  letter-spacing: -0.5px;
}

// ----- TransitionGroup 动画 -----
.expense-list-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.expense-list-leave-active {
  transition: all 0.25s cubic-bezier(0.55, 0, 1, 1);
}
.expense-list-move {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.expense-list-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.97);
}
.expense-list-leave-to {
  opacity: 0;
  transform: translateX(-20px) scale(0.95);
}
.expense-list-leave-active {
  position: absolute;
  width: calc(100% - var(--fts-space-6));
}

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
