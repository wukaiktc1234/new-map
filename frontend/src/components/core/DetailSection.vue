<script lang="ts">
import { computed, ref, watch } from 'vue'

export interface DetailSectionProps {
  title?: string
  description?: string
  badge?: string
  collapsible?: boolean
  defaultCollapsed?: boolean
  collapsed?: boolean
  accent?: boolean
  showDivider?: boolean
}

export default {
  name: 'DetailSection',
  props: {
    title: { type: String, default: undefined },
    description: { type: String, default: undefined },
    badge: { type: String, default: undefined },
    collapsible: { type: Boolean, default: false },
    defaultCollapsed: { type: Boolean, default: false },
    collapsed: { type: Boolean, default: undefined },
    accent: { type: Boolean, default: true },
    showDivider: { type: Boolean, default: true },
  },
  emits: ['update:collapsed'],
  setup(props: DetailSectionProps & { collapsed?: boolean }, { emit }) {
    const innerCollapsed = ref(props.defaultCollapsed ?? false)

    watch(
      () => props.collapsed,
      (v) => {
        if (v !== undefined) innerCollapsed.value = v
      },
      { immediate: true },
    )

    const collapsedProxy = computed({
      get: () => (props.collapsed !== undefined ? props.collapsed : innerCollapsed.value),
      set: (v: boolean) => {
        innerCollapsed.value = v
        emit('update:collapsed', v)
      },
    })

    const headerClass = computed(() => [
      'detail-section__header',
      { 'detail-section__header--accent': props.accent },
    ])

    return { collapsedProxy, headerClass }
  },
}
</script>

<script setup lang="ts">
import { Fold, Expand } from '@element-plus/icons-vue'
</script>

<template>
  <section class="detail-section">
    <div :class="headerClass">
      <div class="detail-section__title-wrap">
        <h3 v-if="title" class="detail-section__title">{{ title }}</h3>
        <span v-if="badge" class="detail-section__badge">{{ badge }}</span>
      </div>
      <div class="detail-section__actions">
        <slot name="actions" />
        <button
          v-if="collapsible"
          type="button"
          class="detail-section__toggle"
          @click="collapsedProxy = !collapsedProxy"
          :aria-label="collapsedProxy ? '展开' : '折叠'"
        >
          <el-icon :size="14"><component :is="collapsedProxy ? Expand : Fold" /></el-icon>
        </button>
      </div>
    </div>
    <p v-if="description && !collapsedProxy" class="detail-section__desc">{{ description }}</p>
    <div v-show="!collapsedProxy" class="detail-section__body">
      <slot />
    </div>
    <div v-if="showDivider" class="detail-section__divider" />
  </section>
</template>

<style scoped lang="scss">
.detail-section {
  width: 100%;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-3);
    padding: var(--fts-space-3) 0;
  }

  &__header--accent {
    padding-left: var(--fts-space-3);
    border-left: 3px solid var(--fts-primary);
    border-radius: var(--fts-radius-sm);
  }

  &__title-wrap {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    min-width: 0;
  }

  &__title {
    margin: 0;
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    line-height: 1.4;
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    padding: 2px var(--fts-space-2);
    background: var(--fts-bg-secondary);
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-xs);
    border-radius: var(--fts-radius-sm);
    border: 1px solid var(--fts-border-secondary);
    line-height: 1.2;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-shrink: 0;
  }

  &__toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    padding: 0;
    background: transparent;
    border: 1px solid var(--fts-border-secondary);
    border-radius: var(--fts-radius-sm);
    color: var(--fts-text-secondary);
    cursor: pointer;
    transition: all var(--fts-duration-fast);

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary);
      background: var(--fts-primary-bg);
    }
  }

  &__desc {
    margin: 0 0 var(--fts-space-3) var(--fts-space-3);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: 1.5;
  }

  &__body {
    padding-bottom: var(--fts-space-3);
  }

  &__divider {
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--fts-border-secondary) 20%, var(--fts-border-secondary) 80%, transparent);
    margin: 0 0 var(--fts-space-2);
  }
}
</style>
