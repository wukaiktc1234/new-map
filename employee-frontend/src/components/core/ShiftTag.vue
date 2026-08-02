<script setup lang="ts">
import { computed } from 'vue'
import type { ShiftType } from '@/types/schedule'
import { SHIFT_LABEL_MAP } from '@/types'

const props = withDefaults(defineProps<{
  shiftType: ShiftType
  size?: 'small' | 'medium' | 'large'
  showTime?: boolean
  timeRange?: string
}>(), {
  size: 'small',
  showTime: false,
})

const sizeMap = { small: '12px', medium: '14px', large: '16px' }

const label = computed(() => SHIFT_LABEL_MAP[props.shiftType])
</script>

<template>
  <span
    class="shift-tag"
    :class="[`shift-tag--${size}`, `shift-tag--${shiftType}`]"
  >
    <span class="shift-dot"></span>
    <span class="shift-name">{{ label }}</span>
    <span v-if="showTime && timeRange" class="shift-time">{{ timeRange }}</span>
  </span>
</template>

<style scoped lang="scss">
.shift-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: var(--fts-radius-sm);
  font-weight: 700; letter-spacing: 0.5px;
  line-height: 1.4;

  &--small { font-size: 12px; padding: 1px 6px; }
  &--medium { font-size: 14px; padding: 3px 10px; }
  &--large { font-size: 16px; padding: 4px 12px; }

  &--morning { background: var(--fts-shift-morning-bg); }
  &--afternoon { background: var(--fts-shift-afternoon-bg); }
  &--evening { background: var(--fts-shift-evening-bg); }
  &--rest { background: var(--fts-shift-rest-bg); }
}

.shift-dot {
  width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;

  .shift-tag--morning & { background: var(--fts-shift-morning-dot); }
  .shift-tag--afternoon & { background: var(--fts-shift-afternoon-dot); }
  .shift-tag--evening & { background: var(--fts-shift-evening-dot); }
  .shift-tag--rest & { background: var(--fts-shift-rest-dot); }
}

.shift-name {
  .shift-tag--morning & { color: var(--fts-shift-morning-text); }
  .shift-tag--afternoon & { color: var(--fts-shift-afternoon-text); }
  .shift-tag--evening & { color: var(--fts-shift-evening-text); }
  .shift-tag--rest & { color: var(--fts-shift-rest-text); }
}

.shift-time {
  font-size: 10px; font-weight: 400;
  color: var(--fts-text-quaternary); margin-left: 2px;
}
</style>
