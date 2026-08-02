<template>
  <div class="content-card" :class="{ 'no-padding': noPadding }">
    <header v-if="title || $slots.header" class="card-header">
      <div class="header-left">
        <h3 v-if="title" class="card-title">{{ title }}</h3>
        <slot name="header" />
      </div>
      <div class="card-actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="card-body">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
  noPadding?: boolean;
}>();
</script>

<style scoped>
.content-card {
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  box-shadow: var(--fts-shadow-sm);
  border: 1px solid var(--fts-border-primary);
  overflow: visible;
  transition: box-shadow var(--fts-transition-fast);
}

.content-card:hover {
  box-shadow: var(--fts-shadow-md);
}

.content-card.no-padding .card-body {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-2) var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-primary);
  min-height: 44px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  min-width: 0;
}

.card-title {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  position: relative;
  padding-left: var(--fts-space-2);
}

.card-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  background: var(--fts-primary);
  border-radius: 2px;
}

.card-actions {
  display: flex;
  gap: var(--fts-space-1);
  flex-shrink: 0;
}

.card-body {
  padding: var(--fts-space-4);
}

.card-footer {
  padding: var(--fts-space-2) var(--fts-space-4);
  border-top: 1px solid var(--fts-border-primary);
  background-color: var(--fts-bg-secondary);
}
</style>