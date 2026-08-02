<script setup lang="ts">
/**
 * EmptyState - 空状态差异化展示组件
 *
 * 【层级】L3-Core 基础组件层
 * 【职责】根据不同场景展示差异化的空状态提示
 * 【依赖】Element Plus el-empty（底层组件）
 * 【复用性】所有需要空状态展示的页面和列表
 *
 * 设计特点：
 * - 支持5种预设场景：default/no-data/no-search/no-permission/error
 * - 每种场景有独特的图标、标题和描述文案
 * - 可自定义文案覆盖默认值
 * - 支持操作按钮，触发action事件
 * - 使用 --fts-* CSS变量，自动适配深色模式
 *
 * @example
 * ```vue
 * <!-- 无数据 -->
 * <EmptyState type="no-data" @action="handleCreate" />
 *
 * <!-- 搜索无结果 -->
 * <EmptyState type="no-search" description="未找到匹配的员工" />
 *
 * <!-- 自定义完全 -->
 * <EmptyState type="error" title="加载失败" actionText="重试" @action="handleRetry" />
 * ```
 */

import { computed } from 'vue'

// ========== 类型定义 ==========

/** 空状态类型 */
export type EmptyStateType = 'default' | 'no-data' | 'no-search' | 'no-permission' | 'error'

// ========== 场景配置 ==========

interface SceneConfig {
  /** 图标名称或SVG */
  icon: string
  /** 默认标题 */
  defaultTitle: string
  /** 默认描述 */
  defaultDescription: string
}

/** 各场景的默认配置 */
const sceneConfigs: Record<EmptyStateType, SceneConfig> = {
  default: {
    icon: 'default',
    defaultTitle: '暂无内容',
    defaultDescription: '这里还没有任何内容'
  },
  'no-data': {
    icon: 'no-data',
    defaultTitle: '暂无数据',
    defaultDescription: '当前没有可显示的数据'
  },
  'no-search': {
    icon: 'search',
    defaultTitle: '未找到结果',
    defaultDescription: '没有找到符合条件的内容，请尝试其他搜索条件'
  },
  'no-permission': {
    icon: 'permission',
    defaultTitle: '无访问权限',
    defaultDescription: '您没有权限查看此内容，请联系管理员'
  },
  error: {
    icon: 'error',
    defaultTitle: '出错了',
    defaultDescription: '加载内容时发生错误，请稍后重试'
  }
}

// ========== Props定义 ==========

interface Props {
  /** 空状态类型 */
  type?: EmptyStateType
  /** 自定义标题 */
  title?: string
  /** 自定义描述 */
  description?: string
  /** 操作按钮文本 */
  actionText?: string
  /** 图片尺寸 */
  imageSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  title: '',
  description: '',
  actionText: '',
  imageSize: 160
})

// ========== Emits定义 ==========

const emit = defineEmits<{
  (e: 'action'): void
}>()

// ========== 计算属性 ==========

/** 当前场景配置 */
const currentConfig = computed<SceneConfig>(() => sceneConfigs[props.type])

/** 最终显示的标题 */
const displayTitle = computed<string>(() =>
  props.title || currentConfig.value.defaultTitle
)

/** 最终显示的描述 */
const displayDescription = computed<string>(() =>
  props.description || currentConfig.value.defaultDescription
)

/** 是否显示操作按钮 */
const showAction = computed<boolean>(() => !!props.actionText)

// ========== 方法 ==========

/** 处理操作按钮点击 */
const handleAction = (): void => {
  emit('action')
}
</script>

<template>
  <div class="empty-state" :class="`empty-state--${type}`">
    <!-- 插图区域：使用Element Plus图标模拟不同场景 -->
    <div class="empty-state__image" :style="{ width: `${imageSize}px`, height: `${imageSize}px` }">
      <!-- 无数据场景 -->
      <svg v-if="type === 'no-data'" class="empty-state__icon empty-state__icon--no-data" viewBox="0 0 128 128" fill="none">
        <circle cx="64" cy="64" r="56" stroke="currentColor" stroke-width="4" opacity="0.3"/>
        <path d="M44 58h40M44 74h28M44 90h16" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.5"/>
        <circle cx="84" cy="48" r="12" fill="currentColor" opacity="0.2"/>
      </svg>

      <!-- 搜索无结果场景 -->
      <svg v-else-if="type === 'no-search'" class="empty-state__icon empty-state__icon--search" viewBox="0 0 128 128" fill="none">
        <circle cx="54" cy="54" r="32" stroke="currentColor" stroke-width="4" opacity="0.3"/>
        <line x1="78" y1="78" x2="104" y2="104" stroke="currentColor" stroke-width="6" stroke-linecap="round" opacity="0.4"/>
        <line x1="42" y1="54" x2="66" y2="54" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.5"/>
      </svg>

      <!-- 无权限场景 -->
      <svg v-else-if="type === 'no-permission'" class="empty-state__icon empty-state__icon--permission" viewBox="0 0 128 128" fill="none">
        <rect x="28" y="36" width="72" height="56" rx="8" stroke="currentColor" stroke-width="4" opacity="0.3"/>
        <path d="M50 60h28M50 76h16" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.5"/>
        <circle cx="88" cy="24" r="14" stroke="currentColor" stroke-width="4" opacity="0.4"/>
        <line x1="82" y1="30" x2="94" y2="18" stroke="currentColor" stroke-width="4" stroke-linecap="round" opacity="0.5"/>
      </svg>

      <!-- 错误场景 -->
      <svg v-else-if="type === 'error'" class="empty-state__icon empty-state__icon--error" viewBox="0 0 128 128" fill="none">
        <circle cx="64" cy="64" r="52" stroke="currentColor" stroke-width="4" opacity="0.25"/>
        <line x1="46" y1="46" x2="82" y2="82" stroke="currentColor" stroke-width="5" stroke-linecap="round" opacity="0.5"/>
        <line x1="82" y1="46" x2="46" y2="82" stroke="currentColor" stroke-width="5" stroke-linecap="round" opacity="0.5"/>
      </svg>

      <!-- 默认场景 -->
      <svg v-else class="empty-state__icon empty-state__icon--default" viewBox="0 0 128 128" fill="none">
        <rect x="24" y="28" width="80" height="68" rx="10" stroke="currentColor" stroke-width="4" opacity="0.25"/>
        <line x1="38" y1="48" x2="90" y2="48" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.35"/>
        <line x1="38" y1="64" x2="72" y2="64" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.3"/>
        <line x1="38" y1="80" x2="56" y2="80" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.25"/>
      </svg>
    </div>

    <!-- 文案区域 -->
    <div class="empty-state__content">
      <p class="empty-state__title">{{ displayTitle }}</p>
      <p v-if="displayDescription" class="empty-state__description">{{ displayDescription }}</p>
    </div>

    <!-- 操作区域 -->
    <div v-if="showAction" class="empty-state__action">
      <el-button type="primary" @click="handleAction">
        {{ actionText }}
      </el-button>
    </div>

    <!-- 插槽：允许自定义额外内容 -->
    <div v-if="$slots.default" class="empty-state__extra">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-10) var(--fts-space-6);
  min-height: 280px;
}

.empty-state__image {
  margin-bottom: var(--fts-space-5);
  flex-shrink: 0;
}

.empty-state__icon {
  width: 100%;
  height: 100%;
  color: var(--fts-text-tertiary);
  transition: color var(--fts-duration-normal) var(--fts-easing-default);
}

// 不同场景的图标颜色
.empty-state--no-data .empty-state__icon--no-data {
  color: var(--fts-info);
}

.empty-state--no-search .empty-state__icon--search {
  color: var(--fts-warning);
}

.empty-state--no-permission .empty-state__icon--permission {
  color: var(--fts-error);
}

.empty-state--error .empty-state__icon--error {
  color: var(--fts-error);
}

.empty-state__content {
  text-align: center;
  max-width: 320px;
}

.empty-state__title {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2) 0;
  line-height: var(--fts-line-height-normal);
}

.empty-state__description {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
  line-height: var(--fts-line-height-loose);
}

.empty-state__action {
  margin-top: var(--fts-space-5);
}

.empty-state__extra {
  margin-top: var(--fts-space-4);
}
</style>
