<script setup lang="ts">
/**
 * Breadcrumb - 面包屑导航组件
 *
 * 【层级】L3-Core 基础组件层
 * 【职责】展示页面导航路径，支持点击跳转和当前页高亮
 * 【依赖】Vue Router（可选，用于路由跳转）
 * 【复用性】所有需要展示导航路径的页面
 *
 * 设计特点：
 * - 使用 --fts-* CSS变量，自动适配深色模式
 * - 支持自定义分隔符
 * - 当前项高亮显示，历史项可点击跳转
 * - 遵循Element Plus el-breadcrumb的交互行为
 *
 * @example
 * ```vue
 * <Breadcrumb :items="[
 *   { label: '首页', path: '/' },
 *   { label: '员工管理', path: '/employees' },
 *   { label: '员工详情' }
 * ]" />
 * ```
 */

import { useRouter } from 'vue-router'

// ========== 类型定义 ==========

/** 面包屑单项 */
export interface BreadcrumbItem {
  /** 显示文本 */
  label: string
  /** 跳转路径（可选，无path则为当前页） */
  path?: string
}

// ========== Props定义 ==========

interface Props {
  /** 面包屑数据项 */
  items: BreadcrumbItem[]
  /** 分隔符（默认 '/'） */
  separator?: string
}

const props = withDefaults(defineProps<Props>(), {
  separator: '/'
})

// ========== 逻辑实现 ==========

const router = useRouter()

/**
 * 处理面包屑项点击
 * 有path的项执行路由跳转，最后一项不响应点击
 */
const handleClick = (item: BreadcrumbItem, index: number): void => {
  // 最后一项（当前页）不可点击
  if (index === props.items.length - 1 || !item.path) {
    return
  }
  router.push(item.path)
}
</script>

<template>
  <nav class="breadcrumb" aria-label="面包屑导航">
    <ol class="breadcrumb__list">
      <li
        v-for="(item, index) in items"
        :key="index"
        class="breadcrumb__item"
        :class="{ 'breadcrumb__item--current': index === items.length - 1 }"
      >
        <!-- 可点击的历史项 -->
        <a
          v-if="index < items.length - 1 && item.path"
          class="breadcrumb__link"
          @click.prevent="handleClick(item, index)"
        >
          {{ item.label }}
        </a>
        <!-- 当前页（不可点击） -->
        <span v-else class="breadcrumb__current">
          {{ item.label }}
        </span>
        <!-- 分隔符（最后一项后不显示） -->
        <span v-if="index < items.length - 1" class="breadcrumb__separator" aria-hidden="true">
          {{ separator }}
        </span>
      </li>
    </ol>
  </nav>
</template>

<style scoped lang="scss">
.breadcrumb {
  display: flex;
  align-items: center;
  font-size: var(--fts-font-size-sm);
  height: 40px;              // 与 Element Plus el-tabs header 高度一致(默认尺寸)
  line-height: 40px;          // 垂直居中
}

.breadcrumb__list {
  display: flex;
  align-items: center;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 0;
}

.breadcrumb__item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.breadcrumb__link {
  color: var(--fts-text-secondary);
  text-decoration: none;
  cursor: pointer;
  transition: color var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    color: var(--fts-primary);
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: 2px;
    border-radius: var(--fts-radius-sm);
  }
}

.breadcrumb__current {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.breadcrumb__item--current .breadcrumb__current {
  color: var(--fts-primary);
}

.breadcrumb__separator {
  color: var(--fts-text-tertiary);
  user-select: none;
  margin: 0 var(--fts-space-1);
}
</style>
