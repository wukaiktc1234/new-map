<script setup lang="ts">
/**
 * 页面头部组件 - 极简风格
 */
interface Props {
  title: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  description: ''
})
</script>

<template>
  <div class="page-header">
    <div class="page-header__content">
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <div class="page-header__actions">
      <slot name="extra" />
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-5) var(--fts-space-6);
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  position: relative;
  z-index: 1;

  /* 与页面容器圆角协调 - 顶部两个角 */
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

.page-header__title {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  margin: 0;
  line-height: var(--fts-line-height-tight);
}

.page-header__description {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin: var(--fts-space-1) 0 0 0;
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}
</style>
