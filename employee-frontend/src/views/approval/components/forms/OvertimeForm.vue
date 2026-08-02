<script setup lang="ts">
/**
 * OvertimeForm - 加班申请表单（专业级重写版）
 *
 * 字段：加班日期 / 开始时间 / 结束时间 / 时长(自动计算+动画) / 加班原因
 *
 * 设计要点：
 * - 内置提交操作栏（正常流布局，不使用 sticky 避免与 TabBar 层级冲突）
 * - HR 工时参考信息卡片（含进度条和健康风险提示）
 * - 时间选择后自动计算加班时长（数字动画）
 * - 连续加班健康风险提示（纯文字，无 emoji）
 */
import { reactive, ref, computed, watch } from 'vue'
import { AlarmClock, Clock, EditPen, Switch as SwitchIcon, InfoFilled, WarningFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import FormSection from '../FormSection.vue'
import FormField from '../FormField.vue'
import type { OvertimeFormData } from '@/types/approval'
import { useScrollLock } from '@/composables/useScrollLock'

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'cancel'): void
}>()

const props = withDefaults(defineProps<{
  /** 默认加班日期（今天） */
  defaultDate?: string
  /** 是否正在提交中 */
  submitting?: boolean
}>(), {
  defaultDate: '',
  submitting: false,
})

const { lock, unlock } = useScrollLock()

// ========== 表单数据 ==========
const form = reactive<OvertimeFormData>({
  overtimeDate: props.defaultDate,
  startTime: '',
  endTime: '',
  duration: 0,
  reason: '',
  isCompensatoryLeave: false,
})

// ========== HR 工时参考数据（mock，后续对接 API） ==========
const overtimeContext = reactive({
  monthHours: 12,
  monthLimit: 36,
  quarterHours: 28,
  quarterLimit: 100,
  deptAvgHours: 8.5,
  get compensateType(): '调休' | '加班费' {
    return form.isCompensatoryLeave ? '调休' : '加班费'
  },
  recentRecords: [
    { date: '05-15', hours: 3, type: '工作日加班' },
    { date: '05-24', hours: 5, type: '周末加班' },
    { date: '05-28', hours: 4, type: '工作日加班' },
  ],
})

const monthUsagePercent = computed(() =>
  Math.round((overtimeContext.monthHours / overtimeContext.monthLimit) * 100),
)

// 风险等级判定
const riskLevel = computed<'normal' | 'caution' | 'warning' | 'danger'>(() => {
  const pct = monthUsagePercent.value
  if (pct >= 80) return 'danger'
  if (pct >= 55) return 'warning'
  if (pct >= 35) return 'caution'
  return 'normal'
})

// 健康风险提示
const healthWarning = computed<{ level: 'warning' | 'danger'; title: string; text: string } | null>(() => {
  const level = riskLevel.value
  if (level === 'danger') {
    return {
      level: 'danger',
      title: '加班时长已接近上限，存在健康风险',
      text: `本月已加班${overtimeContext.monthHours}小时（使用率${monthUsagePercent.value}%），近30天加班${overtimeContext.recentRecords.length}次。长时间高强度工作可能导致疲劳累积，请注意休息。`,
    }
  }
  if (level === 'warning') {
    return {
      level: 'warning',
      title: '加班频率偏高，请关注劳逸结合',
      text: `本月已加班${overtimeContext.monthHours}小时（使用率${monthUsagePercent.value}%），超过部门平均(${overtimeContext.deptAvgHours}h)。如确需加班，建议确保每次加班后有充足休息时间。`,
    }
  }
  return null
})

// ========== 字段级错误 ==========
const errors = reactive<Record<string, string>>({})

function clearError(field: string): void {
  delete errors[field]
}

function setError(field: string, message: string): void {
  errors[field] = message
}

// ========== 时长自动计算 ==========
function calcDuration(): void {
  if (!form.startTime || !form.endTime) {
    form.duration = 0
    return
  }

  const [sh, sm] = form.startTime.split(':').map(Number)
  const [eh, em] = form.endTime.split(':').map(Number)
  const startMinutes = sh * 60 + sm
  const endMinutes = eh * 60 + em

  if (endMinutes <= startMinutes) {
    form.duration = 0
    return
  }

  form.duration = Math.round((endMinutes - startMinutes) / 60 * 10) / 10
}

// ========== 动画数字显示 ==========
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

  if (!form.overtimeDate) {
    setError('overtimeDate', '请选择加班日期')
    isValid = false
  }
  if (!form.startTime || !form.endTime) {
    setError('timeRange', '请选择完整的加班时段')
    isValid = false
  } else if (form.duration <= 0) {
    setError('timeRange', '结束时间必须晚于开始时间')
    isValid = false
  }
  if (!form.reason.trim()) {
    setError('reason', '请填写加班原因')
    isValid = false
  }

  return isValid
}

// ========== 暴露方法 ==========
defineExpose({
  validate,
  getFormData: (): OvertimeFormData => ({ ...form }),
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
  <div class="overtime-form">
    <!-- HR 工时参考卡片 -->
    <section class="context-card" :class="`context-card--${riskLevel}`">
      <div class="context-card__header">
        <el-icon :size="14"><InfoFilled /></el-icon>
        加班工时参考
        <span v-if="riskLevel !== 'normal'" class="risk-badge" :class="`risk-badge--${riskLevel}`">
          {{ riskLevel === 'caution' ? '偏高' : riskLevel === 'warning' ? '注意' : '高风险' }}
        </span>
      </div>

      <!-- 主行：工时卡片 + 统计网格 -->
      <div class="context-main">
        <div class="hours-card">
          <span class="hours-label">本月已加班</span>
          <span class="hours-value" :class="{ 'hours-value--warn': monthUsagePercent > 60 }">
            {{ overtimeContext.monthHours }}<small>小时</small>
          </span>
          <div class="progress-wrap">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :class="{ 'progress-fill--warn': monthUsagePercent > 80, 'progress-fill--danger': monthUsagePercent > 95 }"
                :style="{ width: `${monthUsagePercent}%` }"
              />
            </div>
            <span class="progress-text">{{ monthUsagePercent }}% / 上限{{ overtimeContext.monthLimit }}h</span>
          </div>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-label">本季度</span>
            <span class="stat-value">{{ overtimeContext.quarterHours }}<small>h</small></span>
          </div>
          <div class="stat-item">
            <span class="stat-label">部门平均</span>
            <span class="stat-value">{{ overtimeContext.deptAvgHours }}<small>h</small></span>
          </div>
          <div class="stat-item">
            <span class="stat-label">补偿方式</span>
            <span class="stat-value stat-value--type">{{ overtimeContext.compensateType }}</span>
          </div>
        </div>
      </div>

      <!-- 近期记录 -->
      <div v-if="overtimeContext.recentRecords?.length" class="recent-section">
        <span class="recent-title">近期记录</span>
        <div class="recent-list">
          <div v-for="(rec, ri) in overtimeContext.recentRecords" :key="ri" class="recent-item">
            <span class="recent-date">{{ rec.date }}</span>
            <span class="recent-hours">{{ rec.hours }}h</span>
            <span class="recent-type">{{ rec.type }}</span>
          </div>
        </div>
      </div>

      <!-- 健康风险提示 -->
      <div v-if="healthWarning" class="health-alert" :class="`health-alert--${healthWarning.level}`">
        <el-icon :size="16"><WarningFilled /></el-icon>
        <div class="health-body">
          <span class="health-title">{{ healthWarning.title }}</span>
          <span class="health-text">{{ healthWarning.text }}</span>
        </div>
      </div>

      <p class="context-footer">HR系统自动检测 · 数据每4小时更新</p>
    </section>

    <!-- 基本信息 -->
    <FormSection icon="AlarmClock" title="加班信息" color="var(--fts-primary)">
      <FormField label="加班日期" required :error="errors.overtimeDate">
        <el-date-picker
          v-model="form.overtimeDate"
          type="date"
          placeholder="选择加班日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          popper-class="fts-popover"
          :teleported="false"
          style="width: 100%"
          :editable="false"
          @visible-change="(val: boolean) => val ? lock() : unlock()"
          @change="clearError('overtimeDate')"
        />
      </FormField>

      <FormField label="加班时段" required :error="errors.timeRange">
        <div class="time-range-picker">
          <el-time-picker
            v-model="form.startTime"
            placeholder="开始"
            popper-class="fts-popover"
            :teleported="false"
            format="HH:mm"
            value-format="HH:mm"
            style="flex: 1"
            @change="calcDuration"
          />
          <span class="time-sep" aria-hidden="true">~</span>
          <el-time-picker
            v-model="form.endTime"
            placeholder="结束"
            popper-class="fts-popover"
            :teleported="false"
            format="HH:mm"
            value-format="HH:mm"
            style="flex: 1"
            @change="calcDuration"
          />
        </div>
      </FormField>

      <!-- 时长标签 -->
      <Transition name="fade-slide">
        <div v-if="form.duration > 0" class="duration-badge">
          <div class="duration-badge__icon duration-badge__icon--spin">
            <el-icon><Clock /></el-icon>
          </div>
          <span>加班时长：<strong>{{ displayDuration }}</strong> 小时</span>
        </div>
      </Transition>
    </FormSection>

    <!-- 详细说明 -->
    <FormSection icon="EditPen" title="详细说明" subtitle="最多200字" color="var(--fts-success)">
      <FormField label="加班原因" required :error="errors.reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          placeholder="请填写加班原因..."
          maxlength="200"
          show-word-limit
          @input="clearError('reason')"
        />
      </FormField>
    </FormSection>

    <!-- 其他选项 -->
    <FormSection icon="Switch" title="其他选项" color="var(--fts-info)">
      <FormField label="是否申请调休">
        <div class="switch-wrapper">
          <el-switch
            v-model="form.isCompensatoryLeave"
            active-text="是，申请调休"
            inactive-text="否，领取加班费"
          />
        </div>
      </FormField>
    </FormSection>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   OvertimeForm - 加班申请表单（专业级重写版）
   ================================================================ */

.overtime-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// ----- HR 工时参考卡片 -----
.context-card {
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);
  transition: border-color 0.3s;

  &--caution { border-color: rgba(var(--fts-warning-rgb), 0.3); }
  &--warning { border-color: rgba(var(--fts-warning-rgb), 0.5); background: rgba(var(--fts-warning-rgb), 0.02); }
  &--danger { border-color: rgba(var(--fts-error-rgb), 0.4); background: rgba(var(--fts-error-rgb), 0.02); }
}

.context-card__header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-primary);
  margin-bottom: var(--fts-space-3);

  .el-icon { flex-shrink: 0; }
}

.risk-badge {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: var(--fts-radius-xs);
  font-size: 10px;
  font-weight: 700;

  &--caution { background: rgba(var(--fts-warning-rgb), 0.12); color: var(--fts-warning-dark, #92400e); }
  &--warning { background: rgba(var(--fts-warning-rgb), 0.18); color: var(--fts-warning-dark, #b45309); }
  &--danger { background: rgba(var(--fts-error-rgb), 0.15); color: var(--fts-error); animation: riskPulse 2s ease-in-out infinite; }
}

@keyframes riskPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.context-main {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);

  @media (max-width: 480px) {
    flex-direction: column;
  }
}

.hours-card {
  flex: 1;
  min-width: 180px;
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-sm);
  border: 1px solid var(--fts-border-secondary);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.hours-label {
  font-size: 10px;
  color: var(--fts-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.hours-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--fts-text-primary);
  line-height: 1.1;

  small { font-size: var(--fts-font-size-sm); font-weight: 400; color: var(--fts-text-tertiary); }

  &--warn { color: var(--fts-warning); }
}

.progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.progress-bar {
  height: 6px;
  background: var(--fts-bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--fts-primary);
  border-radius: 3px;
  transition: width 0.5s ease;

  &--warn { background: var(--fts-warning); }
  &--danger { background: var(--fts-error); }
}

.progress-text {
  font-size: 10px;
  color: var(--fts-text-tertiary);
}

.stats-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-2);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-xs);
  border: 1px solid var(--fts-border-secondary);
}

.stat-label {
  font-size: 10px;
  color: var(--fts-text-tertiary);
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--fts-text-primary);

  small { font-size: var(--fts-font-size-xs); font-weight: 400; color: var(--fts-text-tertiary); }

  &--type { font-size: var(--fts-font-size-xs); color: var(--fts-primary); font-weight: 500; }
}

// 近期记录
.recent-section { margin-bottom: var(--fts-space-3); }

.recent-title {
  font-size: 10px;
  color: var(--fts-text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--fts-space-2);
  display: block;
}

.recent-list {
  display: flex;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

.recent-item {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: 3px 8px;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-xs);
  font-size: 11px;
  border: 1px solid var(--fts-border-secondary);
}

.recent-date { color: var(--fts-text-secondary); font-weight: 500; }
.recent-hours { color: var(--fts-primary); font-weight: 700; }
.recent-type { color: var(--fts-text-secondary); }

// 健康风险提示
.health-alert {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-sm);
  margin-bottom: var(--fts-space-2);

  &--warning {
    background: rgba(var(--fts-warning-rgb), 0.05);
    border: 1px solid rgba(var(--fts-warning-rgb), 0.15);

    .el-icon { color: var(--fts-warning); }
    .health-title { color: var(--fts-warning-dark, #92400e); }
  }

  &--danger {
    background: rgba(var(--fts-error-rgb), 0.05);
    border: 1px solid rgba(var(--fts-error-rgb), 0.18);

    .el-icon { color: var(--fts-error); }
    .health-title { color: var(--fts-error); }
  }
}

.health-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.health-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  line-height: 1.4;
}

.health-text {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  line-height: 1.5;
}

.context-footer {
  font-size: 10px;
  color: var(--fts-text-tertiary);
  text-align: center;
  margin: 0;
}

// ----- 时间范围选择器 -----
.time-range-picker {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  @media (max-width: 480px) {
    flex-direction: column;
  }
}

.time-sep {
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-tertiary);
  white-space: nowrap;
  flex-shrink: 0;

  @media (max-width: 480px) {
    transform: rotate(90deg);
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
  margin-top: var(--fts-space-2);

  strong {
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-bold);
  }
}

.duration-badge__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--fts-radius-sm);
  background: var(--fts-primary);
  color: var(--fts-text-on-primary);

  .el-icon { font-size: 14px; }

  &--spin {
    animation: clockSpin 4s linear infinite;

    .el-icon { animation: clockSpinReverse 4s linear infinite; }
  }
}

@keyframes clockSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes clockSpinReverse {
  from { transform: rotate(360deg); }
  to { transform: rotate(0deg); }
}

// ----- 开关包装器 -----
.switch-wrapper {
  display: flex;
  align-items: center;
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
