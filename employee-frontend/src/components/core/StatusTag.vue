<script setup lang="ts">
/**
 * StatusTag - 统一状态徽章组件
 *
 * 现代企业级状态展示，采用几何简约风格：
 * - light:   浅色底纹 + 左侧圆点 + 文字（默认）
 * - solid:   实心填充 + 文字
 * - outline: 描边 + 左侧圆点 + 文字
 * - badge:   左侧圆形色块 + 右侧文字（适合表格列）
 */
import { computed } from 'vue'

const STATUS_COLOR_MAP: Record<string, 'success' | 'warning' | 'error' | 'info' | 'primary' | 'default' | 'processing' | 'danger' | 'emergency'> = {
  active: 'success', approved: 'success', completed: 'success', confirmed: 'success',
  paid: 'success', online: 'success', published: 'success', enabled: 'success', appeal_resolved: 'success',
  pending: 'warning', processing: 'processing', reviewing: 'processing', waiting: 'warning',
  draft: 'warning', scheduled: 'warning', partial: 'warning', self_review: 'processing',
  manager_review: 'processing', hr_review: 'processing', in_progress: 'processing',
  appealing: 'processing', not_started: 'info',
  rejected: 'error', error: 'error', failed: 'error', expired: 'error',
  blocked: 'error', disabled: 'error', offline: 'error', withdrawn: 'info',
  inactive: 'info', cancelled: 'info', archived: 'info', unknown: 'default',
  urgent: 'danger', emergency: 'emergency', overdue: 'danger', unpaid: 'danger',
}

interface Props {
  status: string
  label?: string
  variant?: 'light' | 'solid' | 'outline' | 'badge'
  size?: 'small' | 'medium' | 'large'
  dot?: boolean
  pulse?: boolean
  animated?: boolean
  urgency?: 'normal' | 'urgent'
}

const props = withDefaults(defineProps<Props>(), {
  label: '',
  variant: 'light',
  size: 'small',
  dot: true,
  pulse: false,
  animated: false,
  urgency: undefined,
})

const colorCategory = computed(() => {
  if (!props.status) return 'default'
  return STATUS_COLOR_MAP[props.status.toLowerCase()] || 'default'
})

const isProcessing = computed(() => colorCategory.value === 'processing')

const displayLabel = computed(() => {
  if (props.label) return props.label
  const LABEL_MAP: Record<string, string> = {
    pending: '待审批', approved: '已通过', rejected: '已驳回',
    cancelled: '已撤回', processing: '处理中',
    draft: '待开启', self_review: '待自评', manager_review: '主管评定中',
    hr_review: 'HR审核中', completed: '已完成', appealing: '申诉中',
    appeal_resolved: '申诉已处理', withdrawn: '已撤回',
    not_started: '未开始', in_progress: '学习中', expired: '已过期',
    active: '启用', inactive: '停用', error: '异常', success: '成功',
    failed: '失败', confirmed: '已确认', reviewing: '审核中',
    disabled: '禁用', enabled: '启用', paid: '已支付', unpaid: '未支付',
    online: '在线', offline: '离线', published: '已发布', archived: '已归档',
    scheduled: '已排期', blocked: '已屏蔽', partial: '部分完成',
    waiting: '等待中', unknown: '未知', overdue: '逾期',
    urgent: '紧急', emergency: '特急',
  }
  return LABEL_MAP[props.status] || props.status || '未知'
})

const shouldPulse = computed(() => {
  if (props.pulse) return true
  return colorCategory.value === 'emergency' || (colorCategory.value === 'danger' && props.animated !== false)
})
</script>

<template>
  <span
    :class="[
      'st',
      `st--${variant}`,
      `st--${size}`,
      `st--${colorCategory}`,
      { 'st--pulse': shouldPulse },
      { 'st--animate': isProcessing && animated !== false },
      { 'st--urgency-urgent': urgency === 'urgent' },
      { 'st--urgency-emergency': (urgency as string) === 'emergency' },
    ]"
  >
    <span v-if="variant === 'badge'" class="st__indicator" />
    <span v-else-if="dot && variant !== 'solid'" class="st__dot" />
    <span class="st__text">{{ displayLabel }}</span>
  </span>
</template>

<style scoped lang="scss">
// ============================================================
//  基础结构
//  注意：不使用 SCSS 嵌套的 &--xxx 来组合多类名选择器，
//  因为 Vue scoped 会给每个选择器加 [data-v-xxx]，
//  导致 .st--light--success 无法匹配 .st.st--light.st--success
// ============================================================
.st {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  font-weight: var(--fts-font-weight-medium);
  line-height: 1;
  transition: background-color var(--fts-duration-fast) ease, color var(--fts-duration-fast) ease;
}

.st--small  { height: 20px; padding: 0 5px 0 1px; font-size: var(--fts-font-size-xs); border-radius: var(--fts-radius-sm); }
.st--medium { height: 24px; padding: 0 7px 0 2px; font-size: var(--fts-font-size-sm); border-radius: var(--fts-radius-tag); }
.st--large  { height: 28px; padding: 0 9px 0 2px; font-size: var(--fts-font-size-base); border-radius: var(--fts-radius-md); }

.st--solid.st--small  { padding: 0 6px; text-align: center; }
.st--solid.st--medium { padding: 0 8px; text-align: center; }
.st--solid.st--large  { padding: 0 10px; text-align: center; }

.st__dot {
  display: inline-block;
  border-radius: 50%;
  flex-shrink: 0;
}

.st--small .st__dot  { width: 6px; height: 6px; }
.st--medium .st__dot { width: 7px; height: 7px; }
.st--large .st__dot  { width: 8px; height: 8px; }

.st__text { white-space: nowrap; }

// ============================================================
//  变体：light（浅色底纹 + 圆点 + 文字）— 默认
// ============================================================
.st--light { border: none; backdrop-filter: blur(8px); }

/* light 变体在渐变背景上需要更大的尺寸和内边距以保持可见性 */
.st--light.st--small  { height: 22px; padding: 0 7px 0 2px; font-size: var(--fts-font-size-sm); }
.st--light.st--medium { height: 26px; padding: 0 9px 0 3px; font-size: var(--fts-font-size-base); }
.st--light.st--large  { height: 30px; padding: 0 11px 0 3px; font-size: var(--fts-font-size-md); }

/* light 变体圆点略大，增强在渐变背景上的辨识度 */
.st--light.st--small .st__dot  { width: 7px; height: 7px; }
.st--light.st--medium .st__dot { width: 8px; height: 8px; }
.st--light.st--large .st__dot  { width: 9px; height: 9px; }

.st--light.st--success    { background: rgba(var(--fts-success-rgb), 0.08); color: var(--fts-success); }
.st--light.st--warning    { background: rgba(var(--fts-warning-rgb), 0.08); color: var(--fts-warning-dark, #b45309); }
.st--light.st--error      { background: rgba(var(--fts-error-rgb), 0.08);   color: var(--fts-error); }
.st--light.st--info       { background: rgba(var(--fts-info-rgb), 0.06);    color: var(--fts-text-secondary); }
.st--light.st--primary    { background: rgba(var(--fts-primary-rgb), 0.08); color: var(--fts-primary); }
.st--light.st--default    { background: rgba(var(--fts-gray-rgb), 0.06);    color: var(--fts-text-tertiary); }
.st--light.st--processing { background: rgba(var(--fts-primary-rgb), 0.06); color: var(--fts-primary); }
.st--light.st--danger     { background: rgba(var(--fts-error-rgb), 0.08);   color: var(--fts-error); }
.st--light.st--emergency  { background: rgba(var(--fts-error-rgb), 0.10);   color: var(--fts-error); }

.st--light.st--success .st__dot    { background: var(--fts-success); }
.st--light.st--warning .st__dot    { background: var(--fts-warning); }
.st--light.st--error .st__dot      { background: var(--fts-error); }
.st--light.st--info .st__dot       { background: var(--fts-text-tertiary); }
.st--light.st--primary .st__dot    { background: var(--fts-primary); }
.st--light.st--default .st__dot    { background: var(--fts-text-quaternary); }
.st--light.st--processing .st__dot { background: var(--fts-primary); }
.st--light.st--danger .st__dot     { background: var(--fts-error); }
.st--light.st--emergency .st__dot  { background: var(--fts-error); }

// ============================================================
//  变体：solid（实心填充）
// ============================================================
.st--solid { color: var(--fts-text-on-primary); }

.st--solid.st--success    { background: var(--fts-success); }
.st--solid.st--warning    { background: var(--fts-warning); }
.st--solid.st--error      { background: var(--fts-error); }
.st--solid.st--info       { background: var(--fts-info); }
.st--solid.st--primary    { background: var(--fts-primary); }
.st--solid.st--default    { background: var(--fts-text-quaternary); }
.st--solid.st--processing { background: var(--fts-primary); }
.st--solid.st--danger     { background: var(--fts-error); }
.st--solid.st--emergency  { background: var(--fts-gradient-error); font-weight: 700; }

// ============================================================
//  变体：outline（描边 + 圆点）
// ============================================================
.st--outline { border-width: 1px; border-style: solid; background: transparent; }

.st--outline.st--success    { border-color: var(--fts-success);    color: var(--fts-success); }
.st--outline.st--warning    { border-color: var(--fts-warning);    color: var(--fts-warning-dark, #b45309); }
.st--outline.st--error      { border-color: var(--fts-error);      color: var(--fts-error); }
.st--outline.st--info       { border-color: var(--fts-border-primary); color: var(--fts-text-secondary); }
.st--outline.st--primary    { border-color: var(--fts-primary);    color: var(--fts-primary); }
.st--outline.st--default    { border-color: var(--fts-border-secondary); color: var(--fts-text-tertiary); }
.st--outline.st--processing { border-color: var(--fts-primary);    color: var(--fts-primary); }
.st--outline.st--danger     { border-color: var(--fts-error);      color: var(--fts-error); }
.st--outline.st--emergency  { border-color: var(--fts-error);      color: var(--fts-error); }

.st--outline.st--success .st__dot    { background: var(--fts-success); }
.st--outline.st--warning .st__dot    { background: var(--fts-warning); }
.st--outline.st--error .st__dot      { background: var(--fts-error); }
.st--outline.st--info .st__dot       { background: var(--fts-text-tertiary); }
.st--outline.st--primary .st__dot    { background: var(--fts-primary); }
.st--outline.st--default .st__dot    { background: var(--fts-text-quaternary); }
.st--outline.st--processing .st__dot { background: var(--fts-primary); }
.st--outline.st--danger .st__dot     { background: var(--fts-error); }
.st--outline.st--emergency .st__dot  { background: var(--fts-error); }

// ============================================================
//  变体：badge（左侧圆形指示器 + 右侧文字）
// ============================================================
.st--badge {
  gap: 3px;
  padding: 1px 5px 1px 3px;
  border: 1px solid transparent;
  border-radius: var(--fts-radius-full);
}

.st__indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 50%;
}

.st--badge.st--small .st__indicator  { width: 8px; height: 8px; }
.st--badge.st--medium .st__indicator { width: 9px; height: 9px; }
.st--badge.st--large .st__indicator  { width: 10px; height: 10px; }

.st--badge.st--success {
  background: rgba(var(--fts-success-rgb), 0.07);
  border-color: rgba(var(--fts-success-rgb), 0.16);
  color: var(--fts-success);
  .st__indicator { background: var(--fts-success); }
}

.st--badge.st--warning {
  background: rgba(var(--fts-warning-rgb), 0.07);
  border-color: rgba(var(--fts-warning-rgb), 0.16);
  color: var(--fts-warning-dark, #b45309);
  .st__indicator { background: var(--fts-warning); }
}

.st--badge.st--error {
  background: rgba(var(--fts-error-rgb), 0.07);
  border-color: rgba(var(--fts-error-rgb), 0.16);
  color: var(--fts-error);
  .st__indicator { background: var(--fts-error); }
}

.st--badge.st--info {
  background: rgba(var(--fts-info-rgb), 0.05);
  border-color: rgba(var(--fts-info-rgb), 0.12);
  color: var(--fts-text-secondary);
  .st__indicator { background: var(--fts-text-tertiary); }
}

.st--badge.st--primary {
  background: rgba(var(--fts-primary-rgb), 0.07);
  border-color: rgba(var(--fts-primary-rgb), 0.16);
  color: var(--fts-primary);
  .st__indicator { background: var(--fts-primary); }
}

.st--badge.st--default {
  background: rgba(var(--fts-gray-rgb), 0.05);
  border-color: rgba(var(--fts-gray-rgb), 0.12);
  color: var(--fts-text-tertiary);
  .st__indicator { background: var(--fts-text-quaternary); }
}

.st--badge.st--processing {
  background: rgba(var(--fts-primary-rgb), 0.06);
  border-color: rgba(var(--fts-primary-rgb), 0.18);
  color: var(--fts-primary);
  .st__indicator { background: var(--fts-primary); }
}

.st--badge.st--danger {
  background: rgba(var(--fts-error-rgb), 0.07);
  border-color: rgba(var(--fts-error-rgb), 0.20);
  color: var(--fts-error);
  .st__indicator { background: var(--fts-error); }
}

.st--badge.st--emergency {
  background: rgba(var(--fts-error-rgb), 0.09);
  border-color: rgba(var(--fts-error-rgb), 0.28);
  color: var(--fts-error);
  font-weight: 700;
  .st__indicator { background: var(--fts-error); }
}

// ============================================================
//  紧急特急强视觉
// ============================================================
.st--urgency-urgent { box-shadow: 0 0 0 2px rgba(var(--fts-warning-rgb), 0.25); }
.st--urgency-emergency { box-shadow: 0 0 0 2px rgba(var(--fts-error-rgb), 0.35); font-weight: 700; }

// ============================================================
//  动画
// ============================================================
.st--animate .st__dot,
.st--animate .st__indicator {
  animation: st-breathe 2s ease-in-out infinite;
}

.st--pulse {
  animation: st-pulse-ring 2s ease-in-out infinite;
}

@keyframes st-breathe {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(0.85); }
}

@keyframes st-pulse-ring {
  0%, 100% { box-shadow: 0 0 0 0 rgba(var(--fts-error-rgb), 0.20); }
  50%      { box-shadow: 0 0 0 5px rgba(var(--fts-error-rgb), 0.03); }
}

// ============================================================
//  深色模式适配（light变体 - 主要用于列表/详情页状态标签）
//  原则：提升背景可见度 + 文字适度提亮（不过曝）
// ============================================================
@media (prefers-color-scheme: dark) {
  :root.dark &,
  html.dark & {
    // light + warning（待审批/等待中）— 橙色系
    .st--light.st--warning {
      background: rgba(245, 158, 11, 0.14);
      color: #F59E20;
    }
    .st--light.st--warning .st__dot { background: #F59E20; }

    // light + processing（处理中/审核中）— 蓝色系
    .st--light.st--processing {
      background: rgba(59, 130, 246, 0.13);
      color: #60A5FA;
    }
    .st--light.st--processing .st__dot { background: #60A5FA; }

    // light + success（已通过/已确认）
    .st--light.st--success {
      background: rgba(34, 197, 94, 0.13);
      color: #4ADE80;
    }
    .st--light.st--success .st__dot { background: #4ADE80; }

    // light + error/rejected（已驳回）
    .st--light.st--error,
    .st--light.st--danger {
      background: rgba(239, 68, 68, 0.14);
      color: #F87171;
    }
    .st--light.st--error .st__dot,
    .st--light.st--danger .st__dot { background: #F87171; }

    // light + info/cancelled（已撤回/已归档）
    .st--light.st--info,
    .st--light.st--default {
      background: rgba(148, 163, 184, 0.10);
      color: #94A3B8;
    }
    .st--light.st--info .st__dot,
    .st--light.st--default .st__dot { background: #94A3B8; }

    // solid 变体深色微调（紧急等实心标签）
    .st--solid.st--emergency {
      background: linear-gradient(135deg, #DC2626, #B91C1C);
    }

    // badge 变体深色模式
    .st--badge.st--warning {
      background: rgba(245, 158, 11, 0.10);
      border-color: rgba(245, 158, 11, 0.22);
      color: #F59E20;
    }
    .st--badge.st--warning .st__indicator { background: #F59E20; }

    .st--badge.st--processing {
      background: rgba(59, 130, 246, 0.10);
      border-color: rgba(59, 130, 246, 0.22);
      color: #60A5FA;
    }
    .st--badge.st--processing .st__indicator { background: #60A5FA; }

    .st--badge.st--success {
      background: rgba(34, 197, 94, 0.10);
      border-color: rgba(34, 197, 94, 0.22);
      color: #4ADE80;
    }
    .st--badge.st--success .st__indicator { background: #4ADE80; }
  }
}
</style>
