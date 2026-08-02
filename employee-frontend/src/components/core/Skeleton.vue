<script setup lang="ts">
/**
 * Skeleton 骨架屏组件（增强版）
 *
 * 支持三种变体：
 * - default: shimmer 扫光效果（适用于列表/表格加载）
 * - pulse: 脉冲呼吸效果（更柔和，适用于卡片级骨架）
 * - avatar: 头像+文字组合骨架（适用于用户信息区）
 *
 * 使用示例：
 * <Skeleton :lines="3" />                    <!-- 默认shimmer -->
 * <Skeleton variant="pulse" :lines="4" />   <!-- 脉冲呼吸 -->
 * <Skeleton avatar :lines="3" />            <!-- 头像+文本 -->
 * <Skeleton type="card" />                  <!-- 卡片骨架 -->
 * <Skeleton type="stat" />                  <!-- 统计卡骨架 -->
 */

withDefaults(defineProps<{
  /** 骨架行数 */
  lines?: number
  /** 自定义宽度 */
  width?: string | number
  /** 自定义高度 */
  height?: string | number
  /** 圆角卡片模式 */
  rounded?: boolean
  /** 头像+文字模式 */
  avatar?: boolean
  /** 变体类型：shimmer(默认) / pulse / card / stat */
  variant?: 'shimmer' | 'pulse' | 'card' | 'stat'
}>(), {
  lines: 3,
  rounded: false,
  avatar: false,
  variant: 'shimmer',
})

/** 根据行索引计算宽度比例（模拟真实内容分布） */
function getLineWidth(index: number, total: number): string {
  if (index === 0) return '80%'   // 第一行较长（标题）
  if (index === total - 1) return '60%' // 最后一行较短
  return '100%'
}
</script>

<template>
  <div class="skeleton" :class="[`skeleton--${variant}`, { 'skeleton--rounded': rounded, 'skeleton--avatar': avatar }]">
    
    <!-- ========== avatar 模式：头像+文本行 ========== -->
    <div v-if="avatar" class="skeleton__avatar-group">
      <div class="skeleton__avatar skeleton-shimmer" />
      <div class="skeleton__avatar-lines">
        <div
          v-for="i in (lines || 3)"
          :key="i"
          class="skeleton__line skeleton-shimmer"
          :style="{
            width: i === 1 ? '72%' : i === lines ? '48%' : '92%',
            height: height || (i === 1 ? '16px' : '13px'),
            maxWidth: width || '100%',
          }"
        ></div>
      </div>
    </div>

    <!-- ========== card 模式：卡片级骨架（带header+body） ========== -->
    <div v-else-if="variant === 'card'" class="skeleton__card">
      <div class="skeleton__card-header">
        <div class="skeleton__card-title skeleton-shimmer" style="width: 40%; height: 16px;" />
      </div>
      <div class="skeleton__card-body">
        <div v-for="i in 3" :key="i" class="skeleton__card-row skeleton-pulse" 
             :style="{ width: `${85 - i * 15}%` }" />
      </div>
    </div>

    <!-- ========== stat 模式：统计卡片骨架 ========== -->
    <div v-else-if="variant === 'stat'" class="skeleton__stat">
      <div class="skeleton__stat-value skeleton-pulse" style="width: 60px; height: 28px; border-radius: var(--fts-radius-sm);" />
      <div class="skeleton__stat-label skeleton-pulse" style="width: 50px; height: 12px; border-radius: var(--fts-radius-xs);" />
    </div>

    <!-- ========== 默认/脉冲模式：纯文本行 ========== -->
    <div v-else class="skeleton__lines">
      <div
        v-for="i in (lines || 3)"
        :key="i"
        class="skeleton__line"
        :class="variant === 'pulse' ? 'skeleton-pulse' : 'skeleton-shimmer'"
        :style="{
          width: getLineWidth(i, lines || 3),
          height: height || (i === 1 ? '16px' : '13px'),
          maxWidth: width || '100%',
        }"
      ></div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.skeleton {
  padding: var(--fts-space-4);

  &--rounded {
    border-radius: var(--fts-radius-lg);
    background: var(--fts-bg-card);
    border: 1px solid var(--fts-border-secondary);
  }

  &--avatar {
    display: flex;
    gap: var(--fts-space-4);
    align-items: center;
    padding: var(--fts-space-4);
  }

  &--card,
  &--stat {
    background: var(--fts-bg-card);
    border: 1px solid var(--fts-border-secondary);
    border-radius: var(--fts-radius-lg);
    padding: var(--fts-space-5);
  }

  &__avatar-group {
    display: flex;
    gap: var(--fts-space-4);
    align-items: center;
  }

  &__avatar {
    width: 44px;
    height: 44px;
    border-radius: var(--fts-radius-md);
    flex-shrink: 0;
  }

  &__avatar-lines {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-3);
    flex: 1;
  }

  &__lines {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-3);
    flex: 1;
  }

  &__line {
    border-radius: var(--fts-radius-sm);
  }

  /* card 变体布局 */
  &__card {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-4);

    &-header {
      padding-bottom: var(--fts-space-3);
      border-bottom: 1px solid var(--fts-border-secondary);
    }

    &-title {
      border-radius: var(--fts-radius-sm);
    }

    &-body {
      display: flex;
      flex-direction: column;
      gap: var(--fts-space-3);
    }

    &-row {
      height: 13px;
      border-radius: var(--fts-radius-sm);
    }
  }

  /* stat 变体布局 */
  &__stat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--fts-space-2);

    &-value {
      flex-shrink: 0;
    }

    &-label {
      flex-shrink: 0;
    }
  }
}
</style>
