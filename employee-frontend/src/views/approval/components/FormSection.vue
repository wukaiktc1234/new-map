<script setup lang="ts">
/**
 * FormSection - 表单分区容器组件
 *
 * 带图标标题的表单分组区块，用于将长表单按逻辑分组展示。
 * 设计对标飞书/钉钉的表单分区风格。
 */
import type { Component } from 'vue'

interface Props {
  /** 区块图标（Element Plus 图标组件名或组件对象） */
  icon?: string | Component
  /** 区块标题 */
  title: string
  /** 副标题说明文字 */
  subtitle?: string
  /** 主题色（用于图标背景），默认使用 --fts-primary */
  color?: string
}

withDefaults(defineProps<Props>(), {
  icon: undefined,
  subtitle: undefined,
  color: 'var(--fts-primary)',
})

/** 判断 icon 是否为组件对象（非字符串） */
function isComponentIcon(icon: string | Component | undefined): icon is Component {
  return typeof icon !== 'string' && icon !== undefined
}
</script>

<template>
  <div class="form-section">
    <!-- 左侧装饰线 -->
    <div class="form-section__accent" :style="{ background: color }" />

    <!-- 分区头部 -->
    <div class="form-section__header">
      <!-- 图标（仅组件对象类型才渲染） -->
      <span
        v-if="icon && isComponentIcon(icon)"
        class="form-section__icon"
        :style="{ color: color }"
      >
        <el-icon :size="18"><component :is="icon" /></el-icon>
      </span>
      <div class="form-section__title-group">
        <h4 class="form-section__title">{{ title }}</h4>
        <p v-if="subtitle" class="form-section__subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <!-- 分区内容插槽 -->
    <div class="form-section__body">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   FormSection - 表单分区容器（视觉增强版）
   特性：左侧彩色装饰线 + 微光效果 + Hover 上浮
   ================================================================ */

.form-section {
  position: relative;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5) var(--fts-space-6) var(--fts-space-5) calc(var(--fts-space-6) + 4px);
  transition:
    border-color var(--fts-duration-normal) var(--fts-easing-default),
    box-shadow var(--fts-duration-normal) var(--fts-easing-default),
    transform var(--fts-duration-normal) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow:
      0 4px 16px rgba(0, 0, 0, 0.06),
      0 0 0 1px var(--fts-border-hover);
    transform: translateY(-2px);

    .form-section__accent {
      box-shadow: 0 0 12px currentColor;
    }
  }

  /* 移动端收紧内边距 */
  @media (max-width: 767px) {
    padding: var(--fts-space-4) var(--fts-space-4) var(--fts-space-4) calc(var(--fts-space-4) + 3px);
    border-radius: var(--fts-radius-md);

    &:hover {
      transform: none;
    }
  }

  :global(html.dark) & {
    &:hover {
      box-shadow:
        0 4px 20px rgba(0, 0, 0, 0.12),
        0 0 0 1px rgba(255, 255, 255, 0.1);
    }
  }
}

// ----- 左侧彩色装饰线 -----
.form-section__accent {
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 0 2px 2px 0;
  transition: box-shadow var(--fts-duration-normal) var(--fts-easing-default);

  @media (max-width: 767px) {
    top: 10px;
    bottom: 10px;
    width: 2.5px;
  }
}

// ----- 头部区域 -----
.form-section__header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-5);
}

.form-section__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  line-height: 1;

  .el-icon {
    font-size: 18px;
    vertical-align: middle;
  }
}

.form-section__title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.form-section__title {
  margin: 0;
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  line-height: var(--fts-line-height-tight);
}

.form-section__subtitle {
  margin: 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  line-height: var(--fts-line-height-normal);
}

// ----- 内容区域 -----
.form-section__body {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}
</style>
