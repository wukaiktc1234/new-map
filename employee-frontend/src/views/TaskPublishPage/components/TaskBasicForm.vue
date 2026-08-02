<script setup lang="ts">
/**
 * TaskBasicForm - 任务基础表单
 * 包含：任务标题、任务说明、类别选择、优先级、截止时间
 */
import { Location, Box } from '@element-plus/icons-vue'

type CategoryType = '' | 'daily' | 'training' | 'business_trip' | 'inventory'

const props = defineProps<{
  title: string
  description: string
  category: CategoryType
  priority: 'high' | 'medium' | 'low'
  deadline: string
  destination: string
  expenseBudget: string
  tripStartDate: string
  tripEndDate: string
  inventoryScope: '' | 'full_store' | 'kitchen' | 'dining_area' | 'warehouse'
}>()

const emit = defineEmits<{
  (e: 'update:title', value: string): void,
  (e: 'update:description', value: string): void,
  (e: 'update:category', value: CategoryType): void,
  (e: 'update:priority', value: 'high' | 'medium' | 'low'): void,
  (e: 'update:deadline', value: string): void,
  (e: 'update:destination', value: string): void,
  (e: 'update:expenseBudget', value: string): void,
  (e: 'update:tripStartDate', value: string): void,
  (e: 'update:tripEndDate', value: string): void,
  (e: 'update:inventoryScope', value: typeof props.inventoryScope): void,
  (e: 'categoryChange'): void
}>()

const categoryOptions = [
  { value: 'daily', label: '日常指派', desc: '指派日常工作给同事' },
  { value: 'training', label: '培训考核', desc: '部门级培训学习任务' },
  { value: 'business_trip', label: '出差任务', desc: '指定人员外出工作' },
  { value: 'inventory', label: '盘点任务', desc: '部门/门店库存盘点' },
]

const priorityOptions = [
  { value: 'high', label: '高优先级' },
  { value: 'medium', label: '普通' },
  { value: 'low', label: '低优先级' },
]

const inventoryScopeOptions = [
  { value: 'full_store', label: '全店盘点' },
  { value: 'kitchen', label: '后厨区域' },
  { value: 'dining_area', label: '前厅区域' },
  { value: 'warehouse', label: '仓储区域' },
]

// 滚动锁定（用于日期选择器）
let lockScroll: (() => void) | null = null
let unlockScroll: (() => void) | null = null

// 动态导入 useScrollLock 避免循环依赖
async function initScrollLock() {
  if (!lockScroll) {
    const { useScrollLock } = await import('@/composables/useScrollLock')
    const { lock, unlock } = useScrollLock()
    lockScroll = lock
    unlockScroll = unlock
  }
}

function handleDateVisible(val: boolean) {
  if (val) {
    initScrollLock().then(() => lockScroll?.())
  } else {
    unlockScroll?.()
  }
}
</script>

<template>
  <div class="task-basic-form">
    <!-- 任务标题 -->
    <div class="form-group">
      <label class="form-label">任务标题 <span class="required">*</span></label>
      <input
        :value="title"
        class="form-input"
        type="text"
        placeholder="请输入任务标题（如：完成月度库存盘点）"
        maxlength="50"
        @input="emit('update:title', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <!-- 任务描述 -->
    <div class="form-group">
      <label class="form-label">任务说明</label>
      <textarea
        :value="description"
        class="form-textarea"
        placeholder="请输入任务详细描述、要求等（选填）"
        rows="4"
        maxlength="500"
        @input="emit('update:description', ($event.target as HTMLTextAreaElement).value)"
      />
    </div>

    <!-- 任务类别 -->
    <div class="form-group">
      <label class="form-label">任务类别 <span class="required">*</span></label>
      <div class="category-grid">
        <button
          v-for="opt in categoryOptions"
          :key="opt.value"
          type="button"
          :class="['category-chip', { 'category-chip--active': category === opt.value }]"
          @click="emit('update:category', opt.value as CategoryType); emit('categoryChange')"
        >
          <span class="category-chip__label">{{ opt.label }}</span>
          <span class="category-chip__desc">{{ opt.desc }}</span>
        </button>
      </div>
    </div>

    <!-- 优先级 + 截止时间（同行两列） -->
    <div class="form-row">
      <div class="form-group form-group--flex1">
        <label class="form-label">优先级</label>
        <div class="priority-chips">
          <button
            v-for="p in priorityOptions"
            :key="p.value"
            type="button"
            :class="['priority-chip', { [`priority-chip--${p.value}`]: priority === p.value }]"
            @click="emit('update:priority', p.value as typeof priority)"
          >{{ p.label }}</button>
        </div>
      </div>
      <div class="form-group form-group--flex1">
        <label class="form-label">截止时间 <span class="required">*</span></label>
        <el-date-picker
          :model-value="deadline"
          type="datetime"
          placeholder="选择日期和时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          :clearable="true"
          :editable="false"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          @update:model-value="(val: string) => emit('update:deadline', val)"
          @visible-change="handleDateVisible"
        />
      </div>
    </div>

    <!-- ====== 类别专属字段（根据category动态显示）====== -->
    <section v-if="category === 'business_trip'" class="category-detail-section">
      <h4 class="detail-title"><el-icon :size="16"><Location /></el-icon> 出差详情</h4>
      <div class="form-row">
        <div class="form-group form-group--flex1">
          <label class="form-label">出差目的地</label>
          <input
            :value="destination"
            class="form-input"
            type="text"
            placeholder="如：总部、XX分店、客户现场"
            maxlength="50"
            @input="emit('update:destination', ($event.target as HTMLInputElement).value)"
          />
        </div>
        <div class="form-group form-group--flex1">
          <label class="form-label">差旅预算（元）</label>
          <input
            :value="expenseBudget"
            class="form-input"
            type="number"
            placeholder="预算金额"
            min="0"
            step="100"
            @input="emit('update:expenseBudget', ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>
      <div class="form-row">
        <div class="form-group form-group--flex1">
          <label class="form-label">出发日期</label>
          <el-date-picker
            :model-value="tripStartDate"
            type="date"
            placeholder="选择出发日期"
            value-format="YYYY-MM-DD"
            :clearable="true"
            :editable="false"
            popper-class="fts-popover"
            :teleported="false"
            style="width: 100%"
            @update:model-value="(val: string) => emit('update:tripStartDate', val)"
            @visible-change="handleDateVisible"
          />
        </div>
        <div class="form-group form-group--flex1">
          <label class="form-label">返回日期</label>
          <el-date-picker
            :model-value="tripEndDate"
            type="date"
            placeholder="选择返回日期"
            value-format="YYYY-MM-DD"
            :clearable="true"
            :editable="false"
            popper-class="fts-popover"
            :teleported="false"
            style="width: 100%"
            @update:model-value="(val: string) => emit('update:tripEndDate', val)"
            @visible-change="handleDateVisible"
          />
        </div>
      </div>
    </section>

    <section v-if="category === 'inventory'" class="category-detail-section">
      <h4 class="detail-title"><el-icon :size="16"><Box /></el-icon> 盘点范围</h4>
      <div class="scope-options">
        <button
          v-for="opt in inventoryScopeOptions"
          :key="opt.value"
          type="button"
          :class="['scope-card', { 'scope-card--active': inventoryScope === opt.value }]"
          @click="emit('update:inventoryScope', opt.value as typeof inventoryScope)"
        >
          {{ opt.label }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.task-basic-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  &--flex1 {
    flex: 1;
    min-width: 0;
  }
}

.form-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);

  .required {
    color: var(--fts-error);
    margin-left: 2px;
  }
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: var(--fts-space-3) var(--fts-space-3);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  outline: none;
  transition: border-color var(--fts-duration-fast) ease;
  box-sizing: border-box;

  &::placeholder { color: var(--fts-text-quaternary); }

  &:focus {
    border-color: var(--fts-primary);
    box-shadow: 0 0 0 3px rgba(var(--fts-primary-rgb), 0.10);
  }
}

.form-textarea {
  resize: vertical;
  min-height: 88px;
  line-height: 1.6;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23999' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

// ====== 优先级 Chip ======
.priority-chips {
  display: flex;
  gap: var(--fts-space-2);
}

.priority-chip {
  flex: 1;
  padding: var(--fts-space-2) var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
  background: transparent;
  border: 1.5px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--high):not(&--medium):not(&--low) {
    border-color: var(--fts-border-hover);
  }

  &--high {
    border-color: var(--fts-error);
    background: rgba(245, 108, 108, 0.06);
    color: var(--fts-error);
    font-weight: 600;
  }

  &--medium {
    border-color: var(--fts-warning);
    background: rgba(230, 162, 60, 0.06);
    color: var(--fts-warning);
    font-weight: 600;
  }

  &--low {
    border-color: var(--fts-success);
    background: rgba(103, 194, 58, 0.06);
    color: var(--fts-success);
    font-weight: 600;
  }
}

// ====== 类别专属字段区 ======
.category-detail-section {
  margin-top: var(--fts-space-2);
  padding: var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
}

.detail-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-secondary);
}

// ====== 盘点范围选择 ======
.scope-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-2);

  @media (max-width: 480px) {
    grid-template-columns: 1fr 1fr;
  }
}

.scope-card {
  padding: var(--fts-space-3) var(--fts-space-4);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
  background: transparent;
  border: 1.5px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(&--active) { border-color: var(--fts-border-hover); }

  &--active {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.06);
    color: var(--fts-primary);
    font-weight: 600;
  }
}

// ====== 类别选择 Chip ======
.category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-2);
}

.category-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: var(--fts-space-3) var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover { border-color: var(--fts-border-hover); }
  &:active { transform: scale(0.97); }

  &--active {
    background: rgba(var(--fts-primary-rgb), 0.06);
    border-color: var(--fts-primary);
    color: var(--fts-primary);

    .category-chip__label { font-weight: 600; }
  }

  &__desc {
    font-size: var(--fts-font-size-2xs);
    opacity: 0.7;
  }
}

// ====== 双列布局 ======
.form-row {
  display: flex;
  gap: var(--fts-space-3);

  @media (max-width: 480px) {
    flex-direction: column;
  }
}
</style>
