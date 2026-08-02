<script setup lang="ts">
/**
 * 标准页面布局组件
 * 所有业务页面的根容器
 */
interface Props {
  title: string
  subtitle?: string
  loading?: boolean
}

withDefaults(defineProps<Props>(), {
  subtitle: '',
  loading: false
})
</script>

<template>
  <div class="standard-page">
    <!-- 页面标题区 -->
    <div class="standard-page__header">
      <div class="standard-page__title-section">
        <h1 class="standard-page__title">{{ title }}</h1>
        <p v-if="subtitle" class="standard-page__subtitle">{{ subtitle }}</p>
      </div>
      <div class="standard-page__actions">
        <slot name="headerActions" />
      </div>
    </div>

    <!-- 统计卡片区 -->
    <div v-if="$slots.stats" class="standard-page__stats">
      <slot name="stats" />
    </div>

    <!-- 搜索筛选区 -->
    <div v-if="$slots.filter" class="standard-page__filter">
      <slot name="filter" />
    </div>

    <!-- 工具栏 -->
    <div v-if="$slots.toolbar" class="standard-page__toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 内容区 -->
    <div class="standard-page__content" v-loading="loading">
      <slot />
    </div>

    <!-- 底部分页 -->
    <div v-if="$slots.footer" class="standard-page__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.standard-page {
  padding: var(--fts-space-6);
  min-height: 100vh;
  background-color: var(--fts-bg-page);
}

.standard-page__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--fts-space-6);
}

.standard-page__title-section {
  flex: 1;
}

.standard-page__title {
  font-size: var(--fts-font-size-2xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-1) 0;
  line-height: var(--fts-line-height-tight);
}

.standard-page__subtitle {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin: 0;
}

.standard-page__actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;
}

.standard-page__stats {
  margin-bottom: var(--fts-space-6);
}

.standard-page__filter {
  margin-bottom: var(--fts-space-5);
}

.standard-page__toolbar {
  margin-bottom: var(--fts-space-4);
}

.standard-page__content {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  box-shadow: var(--fts-shadow-sm);
  border: 1px solid var(--fts-border-secondary);
  overflow: hidden;
}

.standard-page__footer {
  margin-top: var(--fts-space-4);
  display: flex;
  justify-content: flex-end;
}
</style>
