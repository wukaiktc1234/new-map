<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import RoleManagementTab from './components/RoleManagementTab.vue'
import UserManagementTab from './components/UserManagementTab.vue'
import DomainPermissionTab from './components/DomainPermissionTab.vue'
import PermissionCodeTab from './components/PermissionCodeTab.vue'
import TemplateManagementTab from './components/TemplateManagementTab.vue'
import UserOverrideTab from './components/UserOverrideTab.vue'
import ApprovalWorkflowTab from './components/ApprovalWorkflowTab.vue'
import AuditLogTab from './components/AuditLogTab.vue'
import { usePermissionStore } from '@/stores/permission'
import { useRoute } from 'vue-router'

const permissionStore = usePermissionStore()

const PC_TAB_STORAGE_KEY = 'permission-center-active-tab'
// 支持 URL 查询参数 ?tab=users 深度链接到指定 Tab（菜单"用户管理"入口使用）
const route = useRoute()

const urlTab = route.query.tab
const activeTab = ref((typeof urlTab === 'string' ? urlTab : null) || localStorage.getItem(PC_TAB_STORAGE_KEY) || 'users')

watch(activeTab, (val) => {
  localStorage.setItem(PC_TAB_STORAGE_KEY, val)
})

/** 监听路由 query.tab 变化（支持从侧边栏"用户管理"菜单点击切换 Tab） */
watch(() => route.query.tab, (val) => {
  if (typeof val === 'string' && tabs.some(t => t.key === val)) {
    activeTab.value = val
  }
})

// watch for tab switch suggestions from child components
watch(() => permissionStore.suggestedTab, (tab) => {
  if (tab && tab !== activeTab.value) {
    activeTab.value = tab
    // cleanup suggestion
    permissionStore.suggestedTab = ''
  }
})

interface TabItem {
  key: string
  label: string
  icon: string
}

const tabs: TabItem[] = [
  { key: 'users', label: '用户管理', icon: 'User' },
  { key: 'roles', label: '角色管理', icon: 'UserFilled' },
  { key: 'domains', label: '域权限配置', icon: 'Grid' },
  { key: 'permissions', label: '权限码管理', icon: 'Key' },
  { key: 'templates', label: '模板管理', icon: 'Document' },
  { key: 'overrides', label: '用户覆盖', icon: 'User' },
  { key: 'approvals', label: '审批流程', icon: 'Finished' },
  { key: 'audit-logs', label: '操作日志', icon: 'Notebook' },
]

const statsCards = computed(() => {
  const registeredCount = permissionStore.registeredMenus.length
  const subMenuCount = permissionStore.registeredMenus.reduce((sum, g) => sum + (g.children?.length || 0), 0)
  const overrideCount = permissionStore.userMenuOverrides.length
  return [
    { icon: 'UserFilled', label: '菜单组', value: registeredCount, colorType: 'primary' as const },
    { icon: 'Grid', label: '业务域', value: permissionStore.registeredMenus.length, colorType: 'success' as const },
    { icon: 'Key', label: '子菜单', value: subMenuCount, colorType: 'info' as const },
    { icon: 'WarningFilled', label: '覆盖规则', value: overrideCount, colorType: 'warning' as const },
    ]
})
</script>

<template>
  <div class="permission-center">
    <PageHeader title="权限中心" description="灵活配置角色、域权限、权限码和审批流程" />

    <div class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="permission-tabs">
      <div class="tabs-header">
        <div class="tabs-scroll">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-item', { 'tab-item--active': activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            <el-icon :size="16"><component :is="tab.icon" /></el-icon>
            {{ tab.label }}
          </div>
        </div>
      </div>

      <div class="tab-content">
        <UserManagementTab v-show="activeTab === 'users'" />
        <RoleManagementTab v-show="activeTab === 'roles'" />
        <DomainPermissionTab v-show="activeTab === 'domains'" />
        <PermissionCodeTab v-show="activeTab === 'permissions'" />
        <TemplateManagementTab v-show="activeTab === 'templates'" />
        <UserOverrideTab v-show="activeTab === 'overrides'" />
        <ApprovalWorkflowTab v-show="activeTab === 'approvals'" />
        <AuditLogTab v-show="activeTab === 'audit-logs'" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.permission-center {
  padding: var(--fts-space-0);
}

.stats-section {
  display: grid;
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-4);
}

.permission-tabs {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  margin-top: var(--fts-space-4);
  overflow: hidden;
}

.tabs-header {
  border-bottom: 1px solid var(--fts-border-primary);
  background: var(--fts-bg-secondary);
  overflow-x: auto;

  &::-webkit-scrollbar {
    height: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--fts-border-primary);
    border-radius: 2px;
  }
}

.tabs-scroll {
  display: flex;
  min-width: max-content;
  padding: 0 var(--fts-space-2);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  font-size: var(--fts-font-size-md);
  color: var(--fts-text-tertiary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all var(--fts-transition-fast);
  user-select: none;
  white-space: nowrap;

  &:hover {
    color: var(--fts-text-primary);
    background: var(--fts-bg-tertiary);
  }

  &--active {
    color: var(--fts-primary);
    border-bottom-color: var(--fts-primary);
    font-weight: 500;
    background: var(--fts-bg-card);

    &:hover {
      color: var(--fts-primary);
      background: var(--fts-bg-card);
    }
  }
}

.tab-content {
  padding: var(--fts-space-4);
}
</style>

\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n    // powered by \u79d2\u7ea7