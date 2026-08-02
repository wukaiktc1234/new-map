<script setup lang="ts">
/**
 * 菜单实时预览面板
 *
 * 职责：
 * - 显示当前选中角色的导航菜单预览
 * - 统计可见/只读/完全访问/隐藏菜单数
 * - 每个菜单项显示图标、标题、访问级别标签
 */
import { Monitor } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import type { DomainAccessLevel, DomainAccessLevelMetaMap, MenuPreviewItem } from '@/types/domain-permission'

interface Props {
  /** 菜单预览项列表 */
  menuPreview: MenuPreviewItem[]
  /** 可见菜单数 */
  visibleMenuCount: number
  /** 完全访问菜单数 */
  fullAccessMenuCount: number
  /** 只读菜单数 */
  readOnlyMenuCount: number
  /** 总菜单数 */
  totalMenuCount: number
  /** 当前选中角色名 */
  selectedRoleName: string
  /** 访问级别元数据 */
  levelMeta: DomainAccessLevelMetaMap
  /** 解析图标名为组件 */
  resolveIcon: (iconName: string | undefined) => Component
}

const props = defineProps<Props>()

/** 隐藏菜单数 */
function getHiddenCount(): number {
  return props.totalMenuCount - props.visibleMenuCount
}

/** 将访问级别映射为 StatusTag 的 status 值 */
function levelToTagStatus(level: DomainAccessLevel): string {
  if (level === 'FULL') return 'primary'
  if (level === 'READ_ONLY') return 'success'
  if (level === 'LIMITED') return 'warning'
  return 'inactive'
}
</script>

<template>
  <div class="menu-preview-panel">
    <div class="preview-header">
      <h4 class="preview-title">
        <el-icon><Monitor /></el-icon>
        导航菜单实时预览
      </h4>
      <span class="preview-role-badge">
        当前角色: {{ selectedRoleName }}
      </span>
    </div>

    <div class="preview-stats">
      <span class="stat"><b>{{ visibleMenuCount }}</b> 可见菜单</span>
      <span class="stat stat-full"><b>{{ fullAccessMenuCount }}</b> 完全访问</span>
      <span class="stat stat-readonly"><b>{{ readOnlyMenuCount }}</b> 只读监控</span>
      <span class="stat stat-hidden"><b>{{ getHiddenCount() }}</b> 隐藏</span>
    </div>

    <div class="preview-menu-list">
      <div
        v-for="menu in menuPreview"
        :key="menu.path"
        :class="['menu-item', {
          'menu-item--visible': menu.visible,
          'menu-item--hidden': !menu.visible,
          [`menu-item--${menu.accessLevel.toLowerCase()}`]: true,
        }]"
      >
        <div class="menu-icon">
          <el-icon :size="16"><component :is="resolveIcon(menu.icon)" /></el-icon>
        </div>
        <span class="menu-title">{{ menu.title }}</span>
        <StatusTag
          v-if="menu.visible"
          :status="levelToTagStatus(menu.accessLevel)"
          :label="levelMeta[menu.accessLevel].label"
          size="small"
        />
        <span v-else class="menu-hidden-label">不可见</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.menu-preview-panel {
  margin-top: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  background: var(--fts-bg-card);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-4) var(--fts-space-5);
  background: linear-gradient(135deg, rgba(var(--fts-primary-rgb, 64, 158, 255), 0.04), transparent);
  border-bottom: 1px solid var(--fts-border-primary);
}

.preview-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin: 0;
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.preview-role-badge {
  font-size: var(--fts-font-size-xs);
  padding: var(--fts-space-1) var(--fts-space-3);
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full);
  color: var(--fts-text-secondary);
}

.preview-stats {
  display: flex;
  gap: var(--fts-space-6);
  padding: var(--fts-space-3) var(--fts-space-5);
  background: var(--fts-bg-secondary);
  border-bottom: 1px solid var(--fts-border-primary);
  flex-wrap: wrap;

  .stat {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);

    b {
      font-size: var(--fts-font-size-lg);
      font-weight: 600;
      color: var(--fts-text-primary);
      margin-right: var(--fts-space-1);
    }

    &-full b { color: var(--fts-primary); }
    &-readonly b { color: var(--fts-success); }
    &-hidden b { color: var(--fts-text-quaternary); }
  }
}

.preview-menu-list {
  padding: var(--fts-space-3) var(--fts-space-5);
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--fts-space-2);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  transition: all 0.15s ease;

  .menu-icon {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--fts-radius-sm);
    background: var(--fts-bg-tertiary);
    flex-shrink: 0;
  }

  .menu-title {
    font-size: var(--fts-font-size-sm);
    font-weight: 500;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .menu-hidden-label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-quaternary);
  }

  &--visible {
    background: var(--fts-bg-secondary);

    .menu-icon {
      color: var(--fts-text-primary);
    }

    .menu-title {
      color: var(--fts-text-primary);
    }
  }

  &--hidden {
    opacity: 0.45;
    background: transparent;

    .menu-icon {
      color: var(--fts-text-quaternary);
    }

    .menu-title {
      color: var(--fts-text-quaternary);
      text-decoration: line-through;
    }
  }

  &--full { border-left: 3px solid var(--fts-primary); }
  &--read_only { border-left: 3px solid var(--fts-success); }
  &--limited { border-left: 3px solid var(--fts-warning); }
}
</style>
