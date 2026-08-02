<template>
  <div class="vs-switcher" :class="[`vs-switcher--${size}`]">
    <button
      v-for="opt in options"
      :key="opt.key"
      :class="['vs-sw-item', { 'vs-sw-item--active': modelValue === opt.key }]"
      @click="handleSelect(opt.key)"
    >
      <!-- 可选图标 -->
      <component v-if="opt.icon" :is="opt.icon" class="vs-sw-icon" />
      <span class="vs-sw-label">{{ opt.label }}</span>
      <!-- 可选徽章 -->
      <span v-if="opt.badge !== undefined && opt.badge !== null && opt.badge !== ''" class="vs-sw-badge">
        {{ opt.badge }}
      </span>
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * ViewSwitcher - 统一视图切换组件（胶囊分段控制器）
 *
 * iOS Segmented Control 风格的视图切换器，用于替代页面中分散的
 * el-tabs 和内联 view-switcher 实现，确保视觉统一。
 *
 * 支持特性：
 * - v-model 双向绑定当前激活项
 * - 可选图标组件
 * - 可选徽章数字/文字
 * - small / medium 两种尺寸
 * - 深色模式自动适配
 */
import type { Component } from 'vue'

/** 单个选项定义 */
export interface ViewSwitcherOption {
  /** 唯一标识 */
  key: string
  /** 显示文字 */
  label: string
  /** 可选图标组件 */
  icon?: Component
  /** 可选徽章数字或字符串 */
  badge?: number | string
}

interface Props {
  /** 选项列表 */
  options: ViewSwitcherOption[]
  /** 当前激活的 key (v-model) */
  modelValue: string
  /** 尺寸: small / medium */
  size?: 'small' | 'medium'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
})

const emit = defineEmits<{
  (e: 'update:modelValue', key: string): void
}>()

function handleSelect(key: string): void {
  if (key !== props.modelValue) {
    emit('update:modelValue', key)
  }
}
</script>

<style scoped lang="scss">
/* 外层容器：圆角背景容器 */
.vs-switcher {
  display: inline-flex;
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-lg);
  padding: 3px;
  margin-bottom: var(--fts-space-4);
  gap: 2px;

  /* 尺寸变体 */
  &--small {
    .vs-sw-item {
      padding: var(--fts-space-1) var(--fts-space-2);
      font-size: var(--fts-font-size-xs);
    }

    .vs-sw-icon {
      font-size: 12px;
    }
  }

  &--medium {
    .vs-sw-item {
      padding: var(--fts-space-2) var(--fts-space-3);
      font-size: var(--fts-font-size-sm);
    }
  }
}

/* 切换按钮 */
.vs-sw-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: var(--fts-space-2) var(--fts-space-3);
  border: none;
  border-radius: var(--fts-radius-md);
  background: transparent;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;

  /* 激活状态 */
  &--active {
    background: var(--fts-bg-card);
    color: var(--fts-primary);
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }

  /* 非激活态 hover */
  &:hover:not(&--active) {
    color: var(--fts-text-primary);
  }

  /* 焦点样式（无障碍） */
  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: 1px;
  }
}

/* 图标 */
.vs-sw-icon {
  display: inline-flex;
  align-items: center;
  font-size: 14px;
  flex-shrink: 0;
  transition: transform 0.15s ease, opacity 0.15s ease;

  /* 非激活态图标：大幅缩小 + 低透明度，退居次要位置 */
  .vs-sw-item:not(.vs-sw-item--active) & {
    transform: scale(0.72);
    opacity: 0.3;
  }
}

/* 文字标签 */
.vs-sw-label {
  line-height: 1;
}

/* 徽章 */
.vs-sw-badge {
  margin-left: 2px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--fts-error);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .vs-sw-item--active {
    /* 深色模式下增加微妙的边框以增强层次感 */
    border: 1px solid rgba(var(--fts-primary-rgb), 0.15);

    /* 调整阴影使其在深色背景下更自然 */
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  }
}
</style>
