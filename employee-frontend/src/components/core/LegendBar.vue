<script setup lang="ts">
interface LegendItem {
  dotColor: string
  label: string
  subLabel?: string
}

defineProps<{
  items: LegendItem[]
}>()
</script>

<template>
  <div class="legend-bar">
    <span v-for="(item, idx) in items" :key="idx" class="legend-entry">
      <i class="legend-dot" :style="{ background: item.dotColor }"></i>
      <span class="legend-label">{{ item.label }}</span>
      <span v-if="item.subLabel" class="legend-sub">{{ item.subLabel }}</span>
    </span>
  </div>
</template>

<style scoped lang="scss">
.legend-bar {
  display: flex; gap: var(--fts-space-4);
  padding: var(--fts-space-2) var(--fts-space-4);
  overflow-x: auto;

  @media (min-width: 768px) {
    justify-content: center; gap: var(--fts-space-6);
  }
}

.legend-entry {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; color: var(--fts-text-tertiary);
  white-space: nowrap; font-weight: 500;
  -webkit-user-select: none; user-select: none;
}

.legend-dot {
  width: 8px; height: 8px; border-radius: 50%;
  display: inline-block; flex-shrink: 0;
}

.legend-label { color: var(--fts-text-secondary); }

.legend-sub {
  color: var(--fts-text-quaternary); font-weight: 400;
}
</style>
