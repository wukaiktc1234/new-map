<script setup lang="ts">
/**
 * 组织详情面板
 *
 * 从 HROrganization.vue 中提取，展示当前选中组织的详情、下属单元卡片及操作按钮。
 */
import { OfficeBuilding, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import type { OrgNode } from './org-types'

interface Props {
  node: OrgNode | null
  nodes: OrgNode[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'add-child': [id: string]
  edit: [node: OrgNode]
  select: [id: string]
}>()

const ORG_TYPE_NAMES: Record<string, string> = {
  company: '公司',
  department: '部门',
  store: '门店',
  warehouse: '仓库',
  group: '小组',
  office: '办事处',
  team: '团队',
}

function getNodeTypeName(type: string): string {
  return ORG_TYPE_NAMES[type] || type
}

function hasChildren(node: OrgNode): boolean {
  return !!(node.children && node.children.length > 0)
}

function getNodePath(targetId: string): OrgNode[] {
  const path: OrgNode[] = []
  function search(nodes: OrgNode[], currentPath: OrgNode[]): boolean {
    for (const node of nodes) {
      currentPath.push(node)
      if (node.id === targetId) { path.push(...currentPath); return true }
      if (node.children && search(node.children, currentPath)) return true
      currentPath.pop()
    }
    return false
  }
  search(props.nodes, [])
  return path
}
</script>

<template>
  <div v-if="!node" class="empty-state">
    <el-icon :size="48" color="var(--fts-text-tertiary)"><OfficeBuilding /></el-icon>
    <span>请从左侧选择一个组织单元查看详情</span>
  </div>

  <template v-else>
    <nav class="breadcrumb-nav">
      <template v-for="(pathNode, index) in getNodePath(node.id)" :key="pathNode.id">
        <span
          :class="['breadcrumb-item', { 'is-current': index === getNodePath(node.id).length - 1 }]"
          @click="emit('select', pathNode.id)"
        >{{ pathNode.name }}</span>
        <span v-if="index < getNodePath(node.id).length - 1" class="breadcrumb-separator">/</span>
      </template>
    </nav>

    <section class="dept-detail-card">
      <div class="detail-header">
        <div class="detail-header__left">
          <div class="detail-title-row">
            <h2 class="detail-title">{{ node.name }}</h2>
            <StatusTag :status="node.type === 'company' ? 'warning' : (node.type === 'store' ? 'success' : 'active')" :label="getNodeTypeName(node.type)" size="small" variant="solid" />
            <StatusTag v-if="node.status" :status="node.status === 'active' ? 'active' : 'inactive'" :label="node.status === 'active' ? '正常运营' : '已禁用'" size="small" variant="light" />
          </div>
        </div>
        <div class="detail-header__actions">
          <el-button size="small" @click="emit('edit', node)">编辑</el-button>
          <el-button size="small" type="primary" @click="emit('add-child', node.id)">
            <el-icon :size="14"><Plus /></el-icon>添加子组织
          </el-button>
        </div>
      </div>

      <div class="detail-grid">
        <div class="info-block">
          <span class="info-label">负责人</span>
          <span class="info-value info-value--highlight">{{ node.manager || '未指定' }}</span>
        </div>
        <div class="info-block">
          <span class="info-label">人员规模</span>
          <span class="info-value info-value--highlight">{{ node.headcount || 0 }} 人</span>
        </div>
        <div v-if="node.code" class="info-block">
          <span class="info-label">组织编码</span>
          <span class="info-value">{{ node.code }}</span>
        </div>
        <div v-if="node.level != null" class="info-block">
          <span class="info-label">层级</span>
          <span class="info-value">L{{ node.level }}</span>
        </div>
      </div>

      <div v-if="node.description" class="dept-description">
        <span class="description-label">{{ node.type === 'company' ? '公司简介' : '组织职责' }}</span>
        <p class="description-text">{{ node.description }}</p>
      </div>
    </section>

    <section v-if="hasChildren(node)" class="children-overview">
      <div class="overview-header">
        <h3 class="overview-title">
          <el-icon><OfficeBuilding /></el-icon>
          下属单元
          <span class="overview-count">({{ node.children?.length || 0 }}个)</span>
        </h3>
      </div>
      <div class="children-grid">
        <div
          v-for="child in node.children"
          :key="child.id"
          class="child-card"
          @click="emit('select', child.id)"
        >
          <div class="child-card__header">
            <StatusTag
              :status="child.type === 'store' ? 'success' : (child.type === 'company' ? 'warning' : 'active')"
              :label="getNodeTypeName(child.type)"
              size="small"
              variant="light"
            />
            <span class="child-card__name">{{ child.name }}</span>
          </div>
          <div class="child-card__meta">
            {{ child.manager || '未指定负责人' }}
            <template v-if="child.headcount"> · {{ child.headcount }}人</template>
          </div>
        </div>
      </div>
    </section>
  </template>
</template>

<style scoped lang="scss">
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3);
  min-height: 400px;
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-sm);
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-md);
  flex-wrap: wrap;

  .breadcrumb-item {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    cursor: pointer;
    transition: color 0.15s ease;

    &:hover { color: var(--fts-primary); }

    &.is-current {
      color: var(--fts-text-primary);
      font-weight: var(--fts-font-weight-semibold);
      cursor: default;
    }
  }

  .breadcrumb-separator {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
  }
}

.dept-detail-card {
  background: linear-gradient(135deg, var(--fts-primary-light) 0%, var(--fts-bg-card) 50%);
  border: 1px solid var(--fts-primary-light);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5);

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: var(--fts-space-4);
    margin-bottom: var(--fts-space-4);
    flex-wrap: wrap;

    &__left { flex: 1; min-width: 200px; }

    .detail-title-row {
      display: flex;
      align-items: center;
      gap: var(--fts-space-3);
      flex-wrap: wrap;
    }

    .detail-title {
      font-size: var(--fts-font-size-xl);
      font-weight: var(--fts-font-weight-bold);
      color: var(--fts-text-primary);
      margin: 0;
    }

    &__actions {
      display: flex;
      gap: var(--fts-space-2);
      flex-shrink: 0;
    }
  }

  .detail-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: var(--fts-space-4);
    margin-bottom: var(--fts-space-4);
  }

  .info-block {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-1);
    padding: var(--fts-space-3);
    background: var(--fts-bg-card);
    border-radius: var(--fts-radius-md);
    border: 1px solid var(--fts-border-secondary);
  }

  .info-label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  .info-value {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);

    &--highlight {
      color: var(--fts-primary-active);
      font-weight: var(--fts-font-weight-semibold);
      font-size: var(--fts-font-size-lg);
    }
  }

  .dept-description {
    padding: var(--fts-space-3);
    background: var(--fts-bg-tertiary);
    border-radius: var(--fts-radius-md);
    border-left: 3px solid var(--fts-primary);

    .description-label {
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-tertiary);
      font-weight: var(--fts-font-weight-medium);
    }

    .description-text {
      margin: var(--fts-space-2) 0 0;
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-secondary);
      line-height: 1.8;
    }
  }
}

.children-overview {
  background: var(--fts-bg-tertiary);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);

  .overview-header {
    margin-bottom: var(--fts-space-3);

    .overview-title {
      display: flex;
      align-items: center;
      gap: var(--fts-space-2);
      font-size: var(--fts-font-size-base);
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-text-primary);
      margin: 0;

      .el-icon { color: var(--fts-text-secondary); }

      .overview-count {
        font-size: var(--fts-font-size-xs);
        color: var(--fts-text-tertiary);
        font-weight: var(--fts-font-weight-normal);
      }
    }
  }

  .children-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: var(--fts-space-3);
  }

  .child-card {
    padding: var(--fts-space-3);
    background: var(--fts-bg-card);
    border: 1px solid var(--fts-border-primary);
    border-radius: var(--fts-radius-md);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: var(--fts-primary);
      box-shadow: var(--fts-shadow-md);
      transform: translateY(-2px);
    }

    &__header {
      display: flex;
      align-items: center;
      gap: var(--fts-space-2);
      margin-bottom: var(--fts-space-2);
    }

    &__name {
      font-size: var(--fts-font-size-sm);
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-text-primary);
    }

    &__meta {
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-tertiary);
    }
  }
}
</style>
