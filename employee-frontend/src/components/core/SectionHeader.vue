<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  icon?: string
  description?: string
  actionLabel?: string
  size?: 'small' | 'medium' | 'large'
  variant?: 'default' | 'card'
}>(), {
  icon: '',
  description: '',
  actionLabel: '',
  size: 'medium',
  variant: 'default',
})

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <div
    class="sh"
    :class="[
      `sh--${size}`,
      { 'sh--card': variant === 'card' },
    ]"
  >
    <div class="sh__main">
      <div class="sh__header">
        <span v-if="icon" class="sh__icon">{{ icon }}</span>
        <h3 class="sh__title">{{ title }}</h3>
      </div>
      <div v-if="description || $slots.description" class="sh__description">
        <slot name="description">
          {{ description }}
        </slot>
      </div>
    </div>

    <div class="sh__aside">
      <slot name="suffix" />
      <button
        v-if="actionLabel && !$slots.suffix"
        class="sh__action"
        @click="emit('action')"
      >
        {{ actionLabel }}
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.sh {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);

  // ---- variant: card ----
  &--card {
    background-color: var(--fts-bg-card);
    border: 1px solid var(--fts-border-secondary);
    border-radius: var(--fts-radius-md);
    padding: var(--fts-space-4) var(--fts-space-5);
    box-shadow: var(--fts-shadow-xs);
  }

  // ---- size variants ----
  &--small {
    margin-bottom: var(--fts-space-2);

    .sh__title {
      font-size: var(--fts-font-size-sm);
      font-weight: var(--fts-font-weight-medium);
    }

    .sh__icon {
      font-size: var(--fts-font-size-sm);
    }

    .sh__description {
      font-size: var(--fts-font-size-2xs);
      margin-top: var(--fts-space-1);
    }

    .sh__action {
      font-size: var(--fts-font-size-2xs);
    }
  }

  &--medium {
    .sh__title {
      font-size: var(--fts-font-size-sm);
      font-weight: var(--fts-font-weight-semibold);
    }

    .sh__icon {
      font-size: var(--fts-font-size-base);
    }

    .sh__description {
      font-size: var(--fts-font-size-xs);
      margin-top: var(--fts-space-1);
    }
  }

  &--large {
    margin-bottom: var(--fts-space-5);

    .sh__title {
      font-size: var(--fts-font-size-lg);
      font-weight: var(--fts-font-weight-semibold);
    }

    .sh__icon {
      font-size: var(--fts-font-size-lg);
    }

    .sh__description {
      font-size: var(--fts-font-size-sm);
      margin-top: var(--fts-space-2);
    }
  }

  // ---- main content area ----
  &__main {
    display: flex;
    flex-direction: column;
    min-width: 0; // 防止flex子项溢出
  }

  &__header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__icon {
    flex-shrink: 0;
    line-height: 1;
  }

  &__title {
    color: var(--fts-text-primary);
    margin: 0;
    line-height: var(--fts-line-height-tight);
  }

  &__description {
    color: var(--fts-text-tertiary);
    line-height: var(--fts-line-height-normal);
  }

  // ---- aside (suffix / action) ----
  &__aside {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-left: auto;
  }

  &__action {
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-primary);
    background: none;
    border: none;
    cursor: pointer;
    padding: var(--fts-space-1) 0;
    transition: opacity var(--fts-duration-fast) var(--fts-easing-default);

    &:hover {
      opacity: 0.75;
    }

    &:active {
      transform: scale(var(--fts-touch-scale-active));
    }
  }
}
</style>
