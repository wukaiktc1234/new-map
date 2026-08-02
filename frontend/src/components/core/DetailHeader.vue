<script setup lang="ts">
/**
 * 详情对话框头部
 *
 * <p>包含标题、副标题（编号/单号）、状态、元信息条（创建/审批人/时间）与右侧操作区。
 * 统一大内容对话框头部视觉与结构，避免每个页面重复拼装 header。</p>
 */
import { computed } from 'vue'
import type { Component } from 'vue'

interface HeaderMetaItem {
  label: string
  value: string | number
  icon?: Component
}

interface Props {
  title: string
  subtitle?: string
  /** StatusTag 或其他标签组件外部通过 #status 插槽传入更佳；仅当传 string 时使用 el-tag 默认展示 */
  statusText?: string
  statusType?: 'primary' | 'success' | 'warning' | 'info' | 'danger'
  /** 分隔展示的元信息（创建人/时间/审批人等），最多 4 项保持一行视觉舒适 */
  metaItems?: HeaderMetaItem[]
  /** 头部右侧主操作按钮文字（如「导出PDF」「审批」） */
  primaryActionText?: string
  primaryActionDisabled?: boolean
  primaryActionLoading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  statusType: 'info',
})

const emit = defineEmits<{
  primaryAction: []
}>()

const displayMetaItems = computed(() => (props.metaItems ?? []).slice(0, 4))
</script>

<template>
  <div class="detail-header">
    <div class="detail-header__main">
      <div class="detail-header__titles">
        <div class="detail-header__title-row">
          <h2 class="detail-header__title">{{ title }}</h2>
          <slot name="status">
            <el-tag
              v-if="statusText"
              :type="statusType"
              size="small"
              effect="light"
              round
            >{{ statusText }}</el-tag>
          </slot>
        </div>
        <p v-if="subtitle" class="detail-header__subtitle">{{ subtitle }}</p>
      </div>
      <div class="detail-header__actions">
        <slot name="actions" />
        <el-button
          v-if="primaryActionText"
          type="primary"
          size="small"
          :disabled="primaryActionDisabled"
          :loading="primaryActionLoading"
          @click="emit('primaryAction')"
        >
          {{ primaryActionText }}
        </el-button>
      </div>
    </div>
    <div v-if="displayMetaItems.length" class="detail-header__meta">
      <div
        v-for="(item, idx) in displayMetaItems"
        :key="idx"
        class="detail-header__meta-item"
      >
        <span class="detail-header__meta-label">
          <el-icon v-if="item.icon" :size="12"><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </span>
        <span class="detail-header__meta-value">{{ item.value }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.detail-header {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) var(--fts-space-5);
  margin: calc(var(--fts-space-4) * -1) calc(var(--fts-space-5) * -1) var(--fts-space-4);
  background:
    linear-gradient(135deg,
      color-mix(in srgb, var(--fts-primary) 10%, transparent),
      transparent 60%);
  border-bottom: 1px solid var(--fts-border-secondary);

  &__main {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--fts-space-4);
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-1);
    min-width: 0;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }

  &__title {
    margin: 0;
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
    line-height: 1.3;
  }

  &__subtitle {
    margin: 0;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    font-family: var(--fts-font-family-mono, ui-monospace, SFMono-Regular, Menlo, Monaco, monospace);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-shrink: 0;
  }

  &__meta {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: var(--fts-space-2) var(--fts-space-4);
    padding-top: var(--fts-space-3);
    border-top: 1px dashed var(--fts-border-secondary);
  }

  &__meta-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    min-width: 0;
  }

  &__meta-label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
    line-height: 1.4;
  }

  &__meta-value {
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
