<script setup lang="ts">
/**
 * PolicyStandardCard - 审批参考信息通用卡片
 *
 * 基于"差旅政策标准"设计模式抽取的通用组件，
 * 用于审批详情页的 contextData 区域展示各类政策/统计/标准信息。
 *
 * 支持特性：
 * - 可配置标题（如"差旅政策标准"、"假期使用概况"、"加班统计"等）
 * - 2列/3列自适应网格布局
 * - 每个 item 支持 label/value/highlight/tag
 * - 可选底部汇总区域
 * - 深色模式自动适配
 */
import { StatusTag } from '@/components/core'

/** 单个信息项 */
export interface PolicyItem {
  /** 标签文字 */
  label: string
  /** 值文字 */
  value: string
  /** 是否高亮显示（超限/警告场景） */
  highlight?: boolean
  /** 可选的状态标签 */
  tag?: { status: string; label: string }
}

/** 底部汇总行 */
export interface SummaryItem {
  /** 汇总前缀文字 */
  prefix: string
  /** 汇总强调值 */
  value: string | number
  /** 汇总后缀文字 */
  suffix?: string
  /** 子列表（如出差记录列表） */
  children?: { text: string }[]
}

interface Props {
  /** 卡片标题 */
  title: string
  /** 信息项列表 */
  items: PolicyItem[]
  /** 网格列数，默认3 */
  columns?: 2 | 3
  /** 可选底部汇总 */
  summary?: SummaryItem
}

withDefaults(defineProps<Props>(), {
  columns: 3,
})
</script>

<template>
  <div class="ps-card">
    <!-- 标题 -->
    <div class="ps-card__title">{{ title }}</div>

    <!-- 信息网格 -->
    <div class="ps-card__grid" :class="[`ps-card__grid--${columns}`]">
      <div
        v-for="(item, idx) in items"
        :key="idx"
        class="ps-card__item"
        :class="{ 'ps-card__item--highlight': item.highlight }"
      >
        <span class="ps-card__label">{{ item.label }}</span>
        <span class="ps-card__value">{{ item.value }}</span>
        <StatusTag
          v-if="item.tag"
          :status="item.tag.status"
          :label="item.tag.label"
          size="small"
          dot
        />
      </div>
    </div>

    <!-- 底部汇总区 -->
    <div v-if="summary" class="ps-card__summary">
      <span>{{ summary.prefix }}</span>
      <strong>{{ summary.value }}</strong>
      <span v-if="summary.suffix">{{ summary.suffix }}</span>
      <template v-if="summary.children?.length">
        <div v-for="(child, ci) in summary.children" :key="ci" class="ps-card__summary-child">
          {{ child.text }}
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ps-card {
  margin-bottom: var(--fts-space-3);
}

/* 标题 */
.ps-card__title {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-bottom: var(--fts-space-2);
  font-weight: 600;
}

/* 网格容器 */
.ps-card__grid {
  display: grid;
  gap: var(--fts-space-2);

  &--2 {
    grid-template-columns: repeat(2, 1fr);
  }

  &--3 {
    grid-template-columns: repeat(3, 1fr);

    @media (max-width: 480px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}

/* 单个信息项 */
.ps-card__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  transition: background-color 0.15s ease;

  &--highlight {
    background: rgba(var(--fts-warning-rgb), 0.06);

    .ps-card__value {
      color: var(--fts-warning);
    }
  }
}

/* 标签 */
.ps-card__label {
  font-size: 10px;
  color: var(--fts-text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

/* 值 */
.ps-card__value {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  line-height: 1.3;
}

/* 底部汇总 */
.ps-card__summary {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  margin-top: var(--fts-space-2);

  strong {
    color: var(--fts-primary);
  }
}

/* 汇总子项 */
.ps-card__summary-child {
  margin-top: var(--fts-space-1);
  padding-left: var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
