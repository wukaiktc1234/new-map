<script setup lang="ts">
/**
 * 递归组织树节点组件
 * 支持多级组织架构嵌套展示
 *
 * 【功能特性】
 * - 多级组织架构递归渲染
 * - 节点折叠/展开交互
 * - 不同类型节点图标颜色区分（公司/部门）
 * - Hover 显示操作按钮
 * - 层级深度缩进
 */
import { computed } from 'vue'
import { OfficeBuilding, Plus, Edit, Delete, ArrowDown, ArrowRight, ArrowUp, VideoPause, VideoPlay, MoreFilled } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'
import type { OrgNode } from './org-types'

const permissionStore = usePermissionStore()

const props = defineProps<{
  node: OrgNode
  depth: number
  selectedId?: string
  expandedKeys: Set<string>
}>()

const emit = defineEmits<{
  select: [id: string]
  toggleExpand: [id: string]
  addChild: [id: string]
  edit: [node: OrgNode]
  delete: [node: OrgNode]
  toggleStatus: [node: OrgNode]
  moveUp: [node: OrgNode]
  moveDown: [node: OrgNode]
}>()

const isExpanded = computed(() => props.expandedKeys.has(props.node.id))
const isSelected = computed(() => props.selectedId === props.node.id)
const hasChildren = computed(() => !!(props.node.children && props.node.children.length > 0))
const headcount = computed(() => props.node.employees?.length || props.node.headcount || 0)

/** 是否有权限删除组织节点：超级管理员或拥有 department:delete 权限 */
const canDeleteNode = computed(() => {
  if (permissionStore.isAdmin) return true
  return permissionStore.userInfo?.permissions?.includes('department:delete') ?? false
})

function handleClick() { emit('select', props.node.id) }

function handleToggle(e: Event) {
  e.stopPropagation()
  emit('toggleExpand', props.node.id)
}

function handleMoreAction(cmd: string) {
  if (cmd === 'moveUp') emit('moveUp', props.node)
  else if (cmd === 'moveDown') emit('moveDown', props.node)
  else if (cmd === 'toggleStatus') emit('toggleStatus', props.node)
  else if (cmd === 'delete') emit('delete', props.node)
}

</script>

<template>
  <div
    :class="[
      'org-tree-node',
      `org-tree-node--${node.type}`,
      { 'is-selected': isSelected, 'is-inactive': node.status === 'inactive' }
    ]"
  >
    <div class="node-main" :style="{ '--depth-offset': `${depth * 20}px` }" @click="handleClick">
      <button
        v-if="hasChildren"
        class="expand-btn"
        :class="{ 'is-expanded': isExpanded }"
        @click="handleToggle"
      >
        <el-icon :size="12">
          <ArrowDown v-if="isExpanded" />
          <ArrowRight v-else />
        </el-icon>
      </button>
      <span v-else class="expand-placeholder" />

      <el-icon :size="18" class="node-icon">
        <OfficeBuilding />
      </el-icon>

      <div class="node-info">
        <span class="node-name">{{ node.name }}</span>
        <span class="node-meta">
          {{ node.manager || '' }}
          <template v-if="headcount > 0">
            · {{ headcount }}人
          </template>
        </span>
      </div>

      <span v-if="headcount > 0" class="node-badge">{{ headcount }}</span>
    </div>

    <div class="node-actions" @click.stop>
      <button
        v-if="node.type !== 'company' || depth === 0"
        class="action-btn action-btn--add"
        title="添加子节点"
        @click="$emit('addChild', node.id)"
      >
        <el-icon :size="12"><Plus /></el-icon>
      </button>
      <button class="action-btn action-btn--edit" title="编辑" @click="$emit('edit', node)">
        <el-icon :size="12"><Edit /></el-icon>
      </button>
      <el-dropdown trigger="click" @command="(cmd) => handleMoreAction(cmd)">
        <button class="action-btn action-btn--more" title="更多操作" @click.stop>
          <el-icon :size="12"><MoreFilled /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="moveUp">
              <el-icon><ArrowUp /></el-icon>上移
            </el-dropdown-item>
            <el-dropdown-item command="moveDown">
              <el-icon><ArrowDown /></el-icon>下移
            </el-dropdown-item>
            <el-dropdown-item :divided="true" command="toggleStatus">
              <el-icon><VideoPause v-if="node.status !== 'inactive'" /><VideoPlay v-else /></el-icon>
              {{ node.status === 'inactive' ? '启用' : '禁用' }}
            </el-dropdown-item>
            <el-dropdown-item v-if="canDeleteNode" command="delete" class="danger-item">
              <el-icon><Delete /></el-icon>删除
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <transition name="collapse">
      <div v-if="hasChildren && isExpanded" class="node-children">
        <OrgTreeNode
          v-for="child in node.children"
          :key="child.id"
          :node="child"
          :depth="depth + 1"
          :selected-id="selectedId"
          :expanded-keys="expandedKeys"
          @select="$emit('select', $event)"
          @toggle-expand="$emit('toggleExpand', $event)"
          @add-child="$emit('addChild', $event)"
          @edit="$emit('edit', $event)"
          @toggle-status="$emit('toggleStatus', $event)"
          @delete="$emit('delete', $event)"
          @move-up="$emit('moveUp', $event)"
          @move-down="$emit('moveDown', $event)"
        />
      </div>
    </transition>
  </div>
</template>

<style scoped lang="scss">
.org-tree-node {
  width: 100%;
  display: flex;
  flex-direction: column;

  .node-main {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    padding: var(--fts-space-2) var(--fts-space-3);
    margin-bottom: 2px;
    border-radius: var(--fts-radius-md);
    cursor: pointer;
    transition: all 0.2s ease;
    padding-left: calc(var(--fts-space-3) + var(--depth-offset, 0px));
    padding-right: var(--fts-space-2);
    min-width: fit-content;

    &:hover {
      background: var(--fts-bg-hover);
    }
  }

  &.is-selected > .node-main {
    background: var(--fts-primary-light);
    border: 1px solid var(--fts-primary);

    .node-icon { color: var(--fts-primary); }
    .node-badge { background: var(--fts-primary); color: white; }
  }

  &--company > .node-main .node-icon {
    color: var(--fts-warning);
  }

  &--department > .node-main .node-icon {
    color: var(--fts-primary);
  }

  &--store > .node-main .node-icon {
    color: var(--fts-success);
  }

  &--warehouse > .node-main .node-icon {
    color: var(--fts-info);
  }

  &--team > .node-main .node-icon,
  &--group > .node-main .node-icon,
  &--office > .node-main .node-icon {
    color: var(--fts-info);
  }

  .expand-btn {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
    cursor: pointer;
    border-radius: var(--fts-radius-sm);
    color: var(--fts-text-tertiary);
    transition: all 0.2s ease;
    flex-shrink: 0;

    &:hover {
      background: var(--fts-bg-hover);
      color: var(--fts-text-primary);
    }
  }

  .expand-placeholder {
    width: 20px;
    flex-shrink: 0;
  }

  .node-icon {
    color: var(--fts-text-tertiary);
    flex-shrink: 0;
    transition: color 0.2s ease;
  }

  .node-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 1px;
    overflow: hidden;
  }

  .node-name {
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }

  .node-meta {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }

  .node-badge {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    background: var(--fts-bg-page);
    padding: 2px var(--fts-space-2);
    border-radius: var(--fts-radius-full);
    min-width: 24px;
    text-align: center;
    flex-shrink: 0;
    font-weight: var(--fts-font-weight-medium);
  }

  .node-actions {
    display: flex;
    gap: var(--fts-space-1);
    flex-shrink: 0;
    align-items: center;
    padding-left: var(--fts-space-2);
    opacity: 0;
    transition: opacity 0.2s ease;

    .action-btn {
      width: 22px;
      height: 22px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--fts-radius-sm);
      border: none;
      background: transparent;
      cursor: pointer;
      transition: all 0.15s ease;
      color: var(--fts-text-secondary);

      &:hover { background: var(--fts-bg-hover); }

      &--add { &:hover { color: var(--fts-success); background: var(--fts-success-light); } }
      &--edit { &:hover { color: var(--fts-primary); background: var(--fts-primary-light); } }
      &--more { &:hover { color: var(--fts-text-primary); background: var(--fts-bg-tertiary); } }
    }

    :deep(.el-dropdown) {
      display: inline-flex;
      line-height: 1;
    }
  }

  &:hover > .node-actions,
  &.is-selected > .node-actions,
  .node-actions:hover,
  .node-actions:focus-within {
    opacity: 1;
  }

  &.is-inactive > .node-main {
    opacity: 0.65;

    .node-name,
    .node-meta {
      color: var(--fts-text-disabled);
      text-decoration: line-through;
    }

    .node-icon {
      color: var(--fts-text-disabled);
    }
  }

  .node-children {
    overflow: hidden;
  }
}

.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  opacity: 1;
  max-height: 2000px;
}
</style>