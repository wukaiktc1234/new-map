<script setup lang="ts">
defineProps<{
  title: string
  icon?: object
  subtitle?: string
}>()
</script>

<template>
  <header class="page-header">
    <div class="header-left">
      <h1 v-if="icon" class="page-title">
        <el-icon><component :is="icon" /></el-icon>
        {{ title }}
      </h1>
      <h1 v-else class="page-title">{{ title }}</h1>
      <span v-if="subtitle" class="header-subtitle">{{ subtitle }}</span>
    </div>
    <div class="header-right">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-5);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-3);
}

.page-title {
  display: flex; align-items: center; gap: var(--fts-space-2);
  font-size: var(--fts-font-size-lg); font-weight: 700;
  color: var(--fts-text-primary); margin: 0;

  @media (min-width: 768px) { font-size: var(--fts-font-size-xl); }
}

.header-subtitle {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  font-weight: 500;
}

.header-right {
  display: flex; align-items: center; gap: var(--fts-space-2);
}
</style>
