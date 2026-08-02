<script setup lang="ts">
/**
 * 状态标签组件 - 现代化设计，支持深色模式自动适配
 *
 * 设计原则：
 * - 浅色主题：深色文字 + 浅色背景 + 微妙边框 = WCAG AA级对比度
 * - 深色主题：明亮文字 + 暗色背景 + 发光边框 = 舒适可读
 * - 类别化标签：图标+文字+品牌色 = 现代化实体设计
 *
 * 使用方式：
 * <StatusTag status="active" />
 * <StatusTag status="unpaid" label="待支付" />
 * <StatusTag status="success" variant="solid" />
 * <StatusTag status="warning" variant="outline" size="medium" />
 * <StatusTag category="department" label="技术部" />
 * <StatusTag category="position" label="工程师" />
 * <StatusTag category="type" label="打印机" />
 */
import { computed } from 'vue'
import {
  OfficeBuilding,
  User,
  Monitor,
  Connection,
  PriceTag,
  Location,
  Box,
  Document,
  Setting,
  Flag,
  KnifeFork,
  IceCream,
  Food,
  Dish,
  IceDrink
} from '@element-plus/icons-vue'
import { useLayoutStore } from '@/stores/layout'

type StatusColorType = 'active' | 'inactive' | 'probation' | 'pending' | 'success' | 'error' | 'warning' | 'info'
type CategoryType = 'department' | 'position' | 'type' | 'location' | 'device' | 'connection' | 'document' | 'tag' | 'config'
  | 'food-hot' | 'food-cold' | 'food-staple' | 'food-soup' | 'food-drink'

interface Props {
  status?: string
  label?: string
  size?: 'small' | 'medium' | 'large'
  variant?: 'solid' | 'light' | 'outline'
  category?: CategoryType
  icon?: string
}

const props = withDefaults(defineProps<Props>(), {
  status: 'info',
  size: 'small',
  variant: 'light'
})

const statusConfig: Record<string, { label: string; colorType: StatusColorType }> = {
  active: { label: '在职', colorType: 'active' },
  inactive: { label: '离职', colorType: 'inactive' },
  probation: { label: '试用期', colorType: 'probation' },
  pending: { label: '待审核', colorType: 'pending' },
  success: { label: '成功', colorType: 'success' },
  error: { label: '失败', colorType: 'error' },
  warning: { label: '警告', colorType: 'warning' },
  info: { label: '信息', colorType: 'info' },

  // 通用色彩类型（业务映射直接使用）
  primary: { label: '主色', colorType: 'primary' },
  orange: { label: '橙色', colorType: 'orange' },
  danger: { label: '危险', colorType: 'danger' },
  default: { label: '默认', colorType: 'default' },

  // 招聘流程状态
  applied: { label: '待处理', colorType: 'probation' },
  interviewing: { label: '面试中', colorType: 'warning' },
  offered: { label: '已发Offer', colorType: 'success' },
  hired: { label: '已录用', colorType: 'active' },

  // 面试结果状态（专用）
  passed: { label: '通过', colorType: 'success' },
  failed: { label: '未通过', colorType: 'error' },

  draft: { label: '草稿', colorType: 'info' },
  submitted: { label: '已提交', colorType: 'pending' },
  approved: { label: '已审核', colorType: 'success' },
  rejected: { label: '已驳回', colorType: 'error' },
  cancelled: { label: '已取消', colorType: 'inactive' },

  in_progress: { label: '进行中', colorType: 'pending' },
  completed: { label: '已完成', colorType: 'success' },
  paused: { label: '已暂停', colorType: 'warning' },

  paid: { label: '已支付', colorType: 'success' },
  unpaid: { label: '待支付', colorType: 'error' },
  partial_paid: { label: '部分支付', colorType: 'warning' },
  refunded: { label: '已退款', colorType: 'inactive' },

  online: { label: '在线', colorType: 'active' },
  offline: { label: '离线', colorType: 'inactive' },
  maintenance: { label: '维护中', colorType: 'warning' },

  confirmed: { label: '已确认', colorType: 'success' },
  received: { label: '已收货', colorType: 'success' },
  shipped: { label: '已发货', colorType: 'pending' },

  normal: { label: '正常', colorType: 'active' },
  disabled: { label: '已停用', colorType: 'info' },
  expired: { label: '已过期', colorType: 'error' },

  // 通用启用/停用（用于模板、配置等非人员场景）
  enabled: { label: '已启用', colorType: 'success' },
}

const categoryConfig: Record<CategoryType, { icon: typeof OfficeBuilding; colorVar: string }> = {
  department: { icon: OfficeBuilding, colorVar: 'primary' },
  position: { icon: User, colorVar: 'primary' },
  type: { icon: PriceTag, colorVar: 'info' },
  location: { icon: Location, colorVar: 'warning' },
  device: { icon: Monitor, colorVar: 'info' },
  connection: { icon: Connection, colorVar: 'success' },
  document: { icon: Document, colorVar: 'primary' },
  tag: { icon: Flag, colorVar: 'info' },
  config: { icon: Setting, colorVar: 'warning' },

  'food-hot': { icon: KnifeFork, colorVar: 'primary' },
  'food-cold': { icon: IceCream, colorVar: 'primary' },
  'food-staple': { icon: Food, colorVar: 'primary' },
  'food-soup': { icon: Dish, colorVar: 'primary' },
  'food-drink': { icon: IceDrink, colorVar: 'primary' },
}

const current = computed(() => statusConfig[props.status] || statusConfig.info)
const displayLabel = computed(() => props.label || current.value?.label || '未知')
const colorType = computed(() => current.value?.colorType || 'info')

/** 颜色通过 CSS 变量内联绑定（scoped 样式无法匹配动态 class，改用变量保证深浅主题生效） */
const colorStyle = computed(() => {
  const t = colorType.value
  return {
    color: `var(--fts-status-${t}-color)`,
    backgroundColor: `var(--fts-status-${t}-bg)`,
    borderColor: `var(--fts-status-${t}-border, transparent)`,
  }
})

const dotStyle = computed(() => {
  const t = colorType.value
  return { backgroundColor: `var(--fts-status-${t}-color)` }
})

const isCategoryMode = computed(() => !!props.category)
const categoryInfo = computed(() => props.category ? categoryConfig[props.category] : null)

const layoutStore = useLayoutStore()

const sizeMap = {
  small: { padding: '3px 10px', fontSize: '12px', dot: '6px', icon: '12px', gap: '4px' },
  medium: { padding: '5px 14px', fontSize: '13px', dot: '7px', icon: '14px', gap: '5px' },
  large: { padding: '7px 18px', fontSize: '14px', dot: '8px', icon: '16px', gap: '6px' }
}

const iconScaleMap: Record<string, number> = { small: 0.875, default: 1, large: 1.25 }

const sizeStyle = computed(() => {
  const base = sizeMap[props.size]
  const iconScale = iconScaleMap[layoutStore.pageIconSize]
  return {
    padding: base.padding,
    fontSize: base.fontSize,
    dot: `${Math.round(parseFloat(base.dot) * iconScale)}px`,
    icon: Math.round(parseFloat(base.icon) * iconScale),
    gap: base.gap
  }
})


/** dot 尺寸（颜色已由 dotStyle 绑定） */
const sizeStyleDot = computed(() => {
  const base = sizeMap[props.size]
  const iconScale = iconScaleMap[layoutStore.pageIconSize]
  return {
    width: `${Math.round(parseFloat(base.dot) * iconScale)}px`,
    height: `${Math.round(parseFloat(base.dot) * iconScale)}px`,
  }
})</script>

<template>
  <span
    v-if="isCategoryMode"
    class="category-tag"
    :class="[`category-tag--${category}`, `category-tag--${size}`]"
    :style="{
      padding: sizeStyle?.padding,
      fontSize: sizeStyle?.fontSize,
      gap: sizeStyle?.gap
    }"
  >
    <el-icon v-if="categoryInfo" :size="sizeStyle?.icon" class="category-tag__icon">
      <component :is="categoryInfo.icon" />
    </el-icon>
    <span class="category-tag__text">{{ displayLabel }}</span>
  </span>

  <span
    v-else
    class="status-tag"
    :class="[`status-tag--${variant}`, `status-tag--${colorType}`]"
    :style="{
      ...sizeStyle,
      ...colorStyle
    }"
  >
    <span
      v-if="variant !== 'outline'"
      class="status-tag__dot"
      :style="{
        ...sizeStyleDot,
        ...dotStyle
      }"
    />
    <span class="status-tag__text">{{ displayLabel }}</span>
  </span>
</template>

<style scoped lang="scss">
/* var() fallback为防御性编码，确保CSS变量未定义时有默认样式 */

/* ========================================
   状态标签 - 现代化设计
   浅色：深色文字 + 浅色背景 + 微妙边框
   深色：明亮文字 + 暗色背景 + 发光边框
   
   ✨ 全局省略号修复（2026-05-06）：
   - 使用 width: max-content 确保内容不被截断
   - 在表格 .cell 中也能完整显示，不会出现 "..."
   - 无需在每个列配置 ellipsis: false
   ======================================== */
.status-tag {
  display: inline-flex;
  align-items: center;
  border-radius: var(--fts-radius-sm, 4px);
  font-weight: var(--fts-font-weight-medium, 500);
  line-height: 1.4;
  white-space: nowrap;
  border: 1px solid transparent;
  transition: all var(--fts-duration-fast, 150ms) var(--fts-easing-default, ease-in-out);

  // 🔥 核心修复：突破父容器 text-overflow: ellipsis 限制
  // 让 StatusTag 始终根据内容自适应宽度，不出现 "..."
  width: max-content;
  max-width: none;
  min-width: fit-content;

  &--active {
    color: var(--fts-status-active-color);
    background-color: var(--fts-status-active-bg);
    border-color: var(--fts-status-active-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-active-color); }
  }

  &--inactive {
    color: var(--fts-status-inactive-color);
    background-color: var(--fts-status-inactive-bg);
    border-color: var(--fts-status-inactive-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-inactive-color); }
  }

  &--probation {
    color: var(--fts-status-probation-color);
    background-color: var(--fts-status-probation-bg);
    border-color: var(--fts-status-probation-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-probation-color); }
  }

  &--pending {
    color: var(--fts-status-pending-color);
    background-color: var(--fts-status-pending-bg);
    border-color: var(--fts-status-pending-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-pending-color); }
  }

  &--success {
    color: var(--fts-status-success-color);
    background-color: var(--fts-status-success-bg);
    border-color: var(--fts-status-success-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-success-color); }
  }

  &--error {
    color: var(--fts-status-error-color);
    background-color: var(--fts-status-error-bg);
    border-color: var(--fts-status-error-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-error-color); }
  }

  &--warning {
    color: var(--fts-status-warning-color);
    background-color: var(--fts-status-warning-bg);
    border-color: var(--fts-status-warning-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-warning-color); }
  }

  &--info {
    color: var(--fts-status-info-color);
    background-color: var(--fts-status-info-bg);
    border-color: var(--fts-status-info-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-info-color); }
  }

  /* 别名类：兼容业务映射中的 default/primary/danger 等类型
     （default→中性灰、primary→主题色、danger→错误红），与深浅主题变量联动 */
  &--default {
    color: var(--fts-status-inactive-color);
    background-color: var(--fts-status-inactive-bg);
    border-color: var(--fts-status-inactive-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-inactive-color); }
  }

  &--primary {
    color: var(--fts-status-active-color);
    background-color: var(--fts-status-active-bg);
    border-color: var(--fts-status-active-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-active-color); }
  }

  &--danger {
    color: var(--fts-status-error-color);
    background-color: var(--fts-status-error-bg);
    border-color: var(--fts-status-error-border, transparent);
    .status-tag__dot { background-color: var(--fts-status-error-color); }
  }

  &--solid {
    color: var(--fts-text-inverse, #ffffff);
    border-color: transparent;
    .status-tag__dot { background-color: var(--fts-text-inverse, #ffffff); opacity: 0.8; }

    &.status-tag--active { background-color: var(--fts-status-active-color); }
    &.status-tag--inactive { background-color: var(--fts-status-inactive-color); }
    &.status-tag--probation { background-color: var(--fts-status-probation-color); }
    &.status-tag--pending { background-color: var(--fts-status-pending-color); }
    &.status-tag--success { background-color: var(--fts-status-success-color); }
    &.status-tag--error { background-color: var(--fts-status-error-color); }
    &.status-tag--warning { background-color: var(--fts-status-warning-color); }
    &.status-tag--info { background-color: var(--fts-status-info-color); }
  }

  &--outline {
    background-color: transparent;
    border: 1px solid currentColor;
  }
}

.status-tag__dot {
  display: inline-block;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 1px currentColor;
  opacity: 0.6;
}

.status-tag__text {
  display: inline-block;
}

/* ========================================
   类别标签 - 现代化实体设计
   图标 + 品牌色 + 文字 = 信息密度与视觉美感
   
   ✨ 全局省略号修复（2026-05-06）：
   - 与 status-tag 相同的修复逻辑
   - 确保部门/职位等标签在表格中完整显示
   ======================================== */
.category-tag {
  display: inline-flex;
  align-items: center;
  border-radius: var(--fts-radius-sm, 4px);
  font-weight: var(--fts-font-weight-medium, 500);
  line-height: 1.4;
  white-space: nowrap;
  border: 1px solid transparent;
  transition: all var(--fts-duration-fast, 150ms) var(--fts-easing-default, ease-in-out);
  box-shadow: var(--fts-category-tag-shadow);

  // 🔥 核心修复
  width: max-content;
  max-width: none;
  min-width: fit-content;

  &--department {
    color: var(--fts-category-department-color, var(--fts-primary));
    background-color: var(--fts-category-department-bg, var(--fts-primary-light));
    border-color: var(--fts-category-department-border, var(--fts-primary-bg));
  }

  &--position {
    color: var(--fts-category-position-color, var(--fts-primary));
    background-color: var(--fts-category-position-bg, var(--fts-primary-light));
    border-color: var(--fts-category-position-border, var(--fts-primary-bg));
  }

  &--type {
    color: var(--fts-category-type-color, var(--fts-info));
    background-color: var(--fts-category-type-bg, var(--fts-info-light));
    border-color: var(--fts-category-type-border, var(--fts-info-bg));
  }

  &--location {
    color: var(--fts-category-location-color, var(--fts-warning));
    background-color: var(--fts-category-location-bg, var(--fts-warning-light));
    border-color: var(--fts-category-location-border, var(--fts-warning-bg));
  }

  &--device {
    color: var(--fts-category-device-color, var(--fts-info));
    background-color: var(--fts-category-device-bg, var(--fts-info-light));
    border-color: var(--fts-category-device-border, var(--fts-info-bg));
  }

  &--connection {
    color: var(--fts-category-connection-color, var(--fts-success));
    background-color: var(--fts-category-connection-bg, var(--fts-success-light));
    border-color: var(--fts-category-connection-border, var(--fts-success-bg));
  }

  &--document {
    color: var(--fts-category-document-color, var(--fts-primary));
    background-color: var(--fts-category-document-bg, var(--fts-primary-light));
    border-color: var(--fts-category-document-border, var(--fts-primary-bg));
  }

  &--tag {
    color: var(--fts-category-tag-color, var(--fts-info));
    background-color: var(--fts-category-tag-bg, var(--fts-info-light));
    border-color: var(--fts-category-tag-border, var(--fts-info-bg));
  }

  &--config {
    color: var(--fts-category-config-color, var(--fts-warning));
    background-color: var(--fts-category-config-bg, var(--fts-warning-light));
    border-color: var(--fts-category-config-border, var(--fts-warning-bg));
  }

  &--food-hot,
  &--food-cold,
  &--food-staple,
  &--food-soup,
  &--food-drink {
    color: var(--fts-category-food-color, var(--fts-primary));
    background-color: var(--fts-category-food-bg, var(--fts-primary-light));
    border-color: var(--fts-category-food-border, var(--fts-primary-bg));
  }

  // category tag 图标增强
  &__icon {
    flex-shrink: 0;
    opacity: 0.85;
    background: var(--fts-category-tag-icon-bg);
    border: 1px solid var(--fts-category-tag-icon-border);
    border-radius: var(--fts-radius-sm, 4px);
    padding: 2px;
    box-shadow: var(--fts-category-tag-icon-shadow);
  }

  &__text {
    display: inline-block;
    text-shadow: var(--fts-category-tag-text-shadow);
  }
}
</style>
