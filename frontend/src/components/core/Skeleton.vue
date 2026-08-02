<script setup lang="ts">
/**
 * Skeleton - 骨架屏加载占位组件
 *
 * 【层级】L3-Core 基础组件层
 * 【职责】在数据加载时展示占位动画，提升用户体验
 * 【依赖】无外部依赖，纯CSS实现
 * 【复用性】所有需要异步加载数据的页面和组件
 *
 * 设计特点：
 * - 支持4种预设类型：table/card/list/detail
 * - Shimmer渐变动画效果（从左到右扫过）
 * - 使用 --fts-* CSS变量，自动适配深色模式
 * - 可配置显示行数和是否启用动画
 *
 * @example
 * ```vue
 * <!-- 表格骨架屏 -->
 * <Skeleton type="table" :rows="5" />
 *
 * <!-- 卡片骨架屏 -->
 * <Skeleton type="card" :rows="3" />
 *
 * <!-- 禁用动画 -->
 * <Skeleton type="list" :animated="false" />
 * ```
 */

// ========== 类型定义 ==========

/** 骨架屏类型 */
export type SkeletonType = 'table' | 'card' | 'list' | 'detail'

// ========== Props定义 ==========

interface Props {
  /** 骨架屏类型 */
  type?: SkeletonType
  /** 显示行数（默认3） */
  rows?: number
  /** 是否启用动画（默认true） */
  animated?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'table',
  rows: 3,
  animated: true
})

// ========== 辅助方法 ==========

/**
 * 生成指定长度的占位数组
 * 用于v-for循环渲染骨架行
 */
const generateRows = (count: number): number[] => {
  return Array.from({ length: count }, (_, i) => i)
}
</script>

<template>
  <div class="skeleton" :class="[`skeleton--${type}`, { 'skeleton--animated': animated }]">
    <!-- ========== 表格类型：模拟表格行 ========== -->
    <template v-if="type === 'table'">
      <!-- 表头行 -->
      <div class="skeleton__row skeleton__row--header">
        <div class="skeleton__avatar skeleton__shimmer" />
        <div class="skeleton__text skeleton__text--lg skeleton__shimmer" />
        <div class="skeleton__text skeleton__text--md skeleton__shimmer" />
        <div class="skeleton__text skeleton__text--sm skeleton__shimmer" />
      </div>
      <!-- 数据行 -->
      <div v-for="i in generateRows(rows)" :key="i" class="skeleton__row">
        <div class="skeleton__avatar skeleton__shimmer" />
        <div class="skeleton__text skeleton__text--lg skeleton__shimmer" />
        <div class="skeleton__text skeleton__text--md skeleton__shimmer" />
        <div class="skeleton__text skeleton__text--sm skeleton__shimmer" />
      </div>
    </template>

    <!-- ========== 卡片类型：模拟StatCard布局 ========== -->
    <template v-else-if="type === 'card'">
      <div class="skeleton-cards">
        <div v-for="i in generateRows(rows)" :key="i" class="skeleton-card">
          <div class="skeleton-card__icon skeleton__circle skeleton__shimmer" />
          <div class="skeleton-card__body">
            <div class="skeleton__text skeleton__text--xs skeleton__shimmer" />
            <div class="skeleton__text skeleton__text--lg skeleton__shimmer" style="width: 60%" />
          </div>
        </div>
      </div>
    </template>

    <!-- ========== 列表类型：模拟列表项 ========== -->
    <template v-else-if="type === 'list'">
      <div v-for="i in generateRows(rows)" :key="i" class="skeleton-list-item">
        <div class="skeleton-list-item__avatar skeleton__circle skeleton__shimmer" />
        <div class="skeleton-list-item__content">
          <div class="skeleton__text skeleton__text--md skeleton__shimmer" />
          <div class="skeleton__text skeleton__text--sm skeleton__shimmer" style="width: 80%" />
          <div class="skeleton__text skeleton__text--xs skeleton__shimmer" style="width: 50%" />
        </div>
      </div>
    </template>

    <!-- ========== 详情类型：模拟详情页 ========== -->
    <template v-else-if="type === 'detail'">
      <div class="skeleton-detail">
        <div class="skeleton-detail__header">
          <div class="skeleton-detail__avatar skeleton__circle--lg skeleton__shimmer" />
          <div class="skeleton-detail__title-block">
            <div class="skeleton__text skeleton__text--xl skeleton__shimmer" style="width: 40%" />
            <div class="skeleton__text skeleton__text--sm skeleton__shimmer" style="width: 25%" />
          </div>
        </div>
        <div class="skeleton-detail__body">
          <div v-for="i in generateRows(Math.min(rows, 4))" :key="i" class="skeleton-detail__row">
            <div class="skeleton__text skeleton__text--sm skeleton__shimmer" style="width: 15%" />
            <div class="skeleton__text skeleton__shimmer" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
// ========== 基础样式 ==========
.skeleton {
  width: 100%;
  padding: var(--fts-space-4);
}

// ========== Shimmer动画效果 ==========
.skeleton--animated {
  .skeleton__shimmer {
    background: linear-gradient(
      90deg,
      var(--fts-bg-secondary) 0%,
      var(--fts-bg-hover) 50%,
      var(--fts-bg-secondary) 100%
    );
    background-size: 200% 100%;
    animation: shimmer 1.5s ease-in-out infinite;
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

// ========== 骨架块基础样式 ==========
.skeleton__shimmer {
  border-radius: var(--fts-radius-sm);
}

.skeleton__avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.skeleton__circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton__circle--lg {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton__text {
  height: 14px;
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

// 文本宽度变体
.skeleton__text--xs { width: 20%; }
.skeleton__text--sm { width: 40%; }
.skeleton__text--md { width: 65%; }
.skeleton__text--lg { width: 85%; }
.skeleton__text--xl { width: 45%; }

// ========== 表格类型样式 ==========
.skeleton--table {
  .skeleton__row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4);
    padding: var(--fts-space-3) 0;
    border-bottom: 1px solid var(--fts-border-secondary);

    &:last-child {
      border-bottom: none;
    }
  }

  .skeleton__row--header {
    padding-bottom: var(--fts-space-2);
    margin-bottom: var(--fts-space-2);
    border-bottom: 2px solid var(--fts-border-primary);
  }
}

// ========== 卡片类型样式 ==========
.skeleton--card {
  .skeleton-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: var(--fts-space-4);
  }

  .skeleton-card {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    padding: var(--fts-space-4);
    background: var(--fts-bg-card);
    border-radius: var(--fts-radius-md);
    border: 1px solid var(--fts-border-secondary);
  }

  .skeleton-card__body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }
}

// ========== 列表类型样式 ==========
.skeleton--list {
  .skeleton-list-item {
    display: flex;
    align-items: flex-start;
    gap: var(--fts-space-3);
    padding: var(--fts-space-4) 0;
    border-bottom: 1px solid var(--fts-border-secondary);

    &:last-child {
      border-bottom: none;
    }
  }

  .skeleton-list-item__content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }
}

// ========== 详情类型样式 ==========
.skeleton--detail {
  .skeleton-detail {
    max-width: 600px;
  }

  .skeleton-detail__header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4);
    margin-bottom: var(--fts-space-6);
    padding-bottom: var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  .skeleton-detail__title-block {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }

  .skeleton-detail__body {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-3);
  }

  .skeleton-detail__row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }
}
</style>
