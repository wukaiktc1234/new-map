<script setup lang="ts">
/**
 * 组织架构页面（多级树形结构版）
 *
 * 【核心业务定位】
 * ✅ 主要功能：多级组织架构管理（公司→部门→子部门/门店→团队）
 * ✅ 数据来源：调用 /v1/departments/tree 真实后端 API
 * ✅ 辅助功能：查看部门概况、下级组织、成员分布
 * ❌ 不包含：员工的增删改操作（应在"员工管理"页面完成）
 *
 * 【重构历史】
 * - 2026-07: 移除硬编码 organizationData，全部接入真实后端 API
 * - 2026-07: 组织类型 type 新增 'store'（门店），原 5 种类型扩展为 6 种
 */
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  OfficeBuilding,
  UserFilled,
  Plus,
  Fold,
  Expand,
  Search,
} from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import { departmentApi } from '@/api/hr/department'
import OrgTreeNode from './components/OrgTreeNode.vue'
import OrgDetailPanel from './components/OrgDetailPanel.vue'
import OrgFormDialog from './components/OrgFormDialog.vue'
import type { OrgNode, OrgType } from './components/org-types'

const searchKeyword = ref('')
const selectedNodeId = ref<string | null>(null)
const expandedKeys = reactive<Set<string>>(new Set())
const loading = ref(false)

const EXPANDED_KEYS_STORAGE_KEY = 'hr-organization-expanded-keys'

function saveExpandedKeys() {
  try {
    localStorage.setItem(EXPANDED_KEYS_STORAGE_KEY, JSON.stringify([...expandedKeys]))
  } catch { /* ignore storage errors */ }
}

function loadExpandedKeys(): string[] {
  try {
    const raw = localStorage.getItem(EXPANDED_KEYS_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

function restoreExpandedState(nodes: OrgNode[]) {
  const stored = loadExpandedKeys()
  const validIds = new Set<string>()
  function collect(nodeList: OrgNode[]) {
    for (const node of nodeList) {
      validIds.add(node.id)
      if (node.children) collect(node.children)
    }
  }
  collect(nodes)
  expandedKeys.clear()
  stored.forEach(id => { if (validIds.has(id)) expandedKeys.add(id) })
}

/** 组织树数据（来自后端） */
const organizationData = ref<OrgNode[]>([])

/**
 * 将后端 DepartmentDTO 转换为前端 OrgNode
 * 兼容字段：employeeCount → headcount
 * @param dto 后端返回的 DepartmentDTO（类型来自后端，与前端 DepartmentDTO 接口存在差异）
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- 后端 DepartmentDTO 字段较灵活，统一用 any 接收简化映射逻辑
function toOrgNode(dto: any): OrgNode {
  return {
    id: String(dto.id ?? ''),
    name: dto.name ?? '',
    type: (dto.type as OrgNode['type']) || 'department',
    manager: dto.manager,
    description: dto.remark,
    headcount: dto.employeeCount ?? 0,
    level: dto.level,
    code: dto.code,
    parentId: dto.parentId != null ? String(dto.parentId) : null,
    sort: dto.sort,
    status: dto.status,
    children: Array.isArray(dto.children) ? dto.children.map(toOrgNode) : undefined,
  }
}

function initExpandedState(nodes: OrgNode[], depth: number = 0) {
  nodes.forEach((node) => {
    if (depth < 2 && node.children && node.children.length > 0) {
      expandedKeys.add(node.id)
      initExpandedState(node.children, depth + 1)
    }
  })
}

function toggleExpand(nodeId: string) {
  if (expandedKeys.has(nodeId)) expandedKeys.delete(nodeId)
  else expandedKeys.add(nodeId)
}

function hasChildren(node: OrgNode): boolean {
  return !!(node.children && node.children.length > 0)
}

function collectAllExpandableIds(nodes: OrgNode[]): string[] {
  const ids: string[] = []
  function traverse(nodeList: OrgNode[]) {
    for (const node of nodeList) {
      if (hasChildren(node)) {
        ids.push(node.id)
        if (node.children) traverse(node.children)
      }
    }
  }
  traverse(nodes)
  return ids
}

function expandAll() {
  const allIds = collectAllExpandableIds(filteredData.value)
  allIds.forEach(id => expandedKeys.add(id))
  ElMessage.success(`已展开 ${allIds.length} 个节点`)
}

function collapseAll() {
  expandedKeys.clear()
  ElMessage.info('已全部折叠')
}

const filteredData = computed(() => {
  const kw = searchKeyword.value.toLowerCase().trim()
  if (!kw) return organizationData.value

  function filterNodes(nodes: OrgNode[]): OrgNode[] {
    return nodes.reduce<OrgNode[]>((acc, node) => {
      const matchName = node.name.toLowerCase().includes(kw)
      const matchManager = node.manager?.toLowerCase().includes(kw) || false
      let filteredChildren: OrgNode[] | undefined
      if (node.children) filteredChildren = filterNodes(node.children)

      if (matchName || matchManager || (filteredChildren && filteredChildren.length > 0)) {
        acc.push({ ...node, children: filteredChildren ?? node.children })
      }
      return acc
    }, [])
  }
  return filterNodes(organizationData.value)
})

const currentNode = computed((): OrgNode | null => {
  if (!selectedNodeId.value) return null
  return findNodeById(filteredData.value, selectedNodeId.value)
})

const statistics = computed(() => {
  let totalNodes = 0
  let totalDepts = 0
  let totalStores = 0
  let totalEmps = 0

  function countNodes(nodes: OrgNode[]) {
    nodes.forEach((node) => {
      totalNodes++
      if (node.type === 'department') totalDepts++
      if (node.type === 'store') totalStores++
      if (node.headcount) totalEmps += node.headcount
      if (node.children) countNodes(node.children)
    })
  }
  countNodes(filteredData.value)

  return [
    { key: 'total', icon: 'OfficeBuilding', label: '组织总数', value: totalNodes, colorType: 'primary' as const },
    { key: 'depts', icon: 'OfficeBuilding', label: '部门数', value: totalDepts, colorType: 'success' as const },
    { key: 'stores', icon: 'OfficeBuilding', label: '门店数', value: totalStores, colorType: 'info' as const },
    { key: 'emps', icon: 'UserFilled', label: '在职员工', value: totalEmps, colorType: 'info' as const },
  ]
})

function findNodeById(nodes: OrgNode[], id: string): OrgNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

function getFirstLeafNode(nodes: OrgNode[]): OrgNode | null {
  for (const node of nodes) {
    if (!hasChildren(node)) return node
    if (node.children) {
      const leaf = getFirstLeafNode(node.children)
      if (leaf) return leaf
    }
  }
  return nodes[0] || null
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
  search(filteredData.value, [])
  return path
}

function selectNode(id: string) {
  selectedNodeId.value = id
  const path = getNodePath(id)
  path.slice(0, -1).forEach((node) => expandedKeys.add(node.id))
}

/** 组织类型名称映射 */
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

function handleSearch() {
  if (!searchKeyword.value.trim()) { handleReset(); return }
  ElMessage.success('搜索完成')
}

function handleReset() {
  searchKeyword.value = ''
  const firstNode = getFirstLeafNode(filteredData.value)
  if (firstNode) selectedNodeId.value = firstNode.id
}

/* ===== 数据加载（真实后端） ===== */
/**
 * 从后端 /v1/departments/tree 加载组织架构树
 */
async function loadOrganizationData(): Promise<void> {
  loading.value = true
  try {
    const tree = await departmentApi.getDepartmentTree()
    if (Array.isArray(tree) && tree.length > 0) {
      organizationData.value = tree.map(toOrgNode)
      const stored = loadExpandedKeys()
      if (stored.length > 0) restoreExpandedState(organizationData.value)
      else initExpandedState(organizationData.value)
      // 自动选中第一个叶子节点（仅在未选中时）
      if (!selectedNodeId.value) {
        const firstNode = getFirstLeafNode(organizationData.value)
        if (firstNode) selectedNodeId.value = firstNode.id
      }
    } else {
      organizationData.value = []
      ElMessage.info('暂无组织数据，请先创建公司')
    }
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载组织架构失败'
    ElMessage.error(msg)
    organizationData.value = []
  } finally {
    loading.value = false
  }
}

/* ===== 对话框状态 ===== */
const dialogVisible = ref(false)
const dialogMode = ref<'add-top' | 'add-child' | 'edit'>('add-top')
const dialogTitle = ref('新增组织')
const editingNodeId = ref<string | null>(null)
const dialogInitialForm = ref<Partial<{ name: string; type: OrgType; parentId: string | null; manager: string; phone: string; location: string; description: string }>>({})

/* ===== 新增/编辑/删除处理 ===== */
function handleAddTopLevel() {
  dialogMode.value = 'add-top'
  dialogTitle.value = '新增顶级组织'
  editingNodeId.value = null
  dialogInitialForm.value = { type: 'company', parentId: null }
  dialogVisible.value = true
}

function handleAddChild(parentId: string) {
  const parent = findNodeById(organizationData.value, parentId)
  if (!parent) {
    ElMessage.error('未找到父节点')
    return
  }
  dialogMode.value = 'add-child'
  dialogTitle.value = `在「${parent.name}」下新增子组织`
  editingNodeId.value = null
  let childType: OrgType = 'department'
  if (parent.type === 'company') childType = 'department'
  else if (parent.type === 'department') childType = 'team'
  else if (parent.type === 'store') childType = 'team'
  else childType = 'team'
  dialogInitialForm.value = { type: childType, parentId }
  dialogVisible.value = true
}

function handleEditNode(node: OrgNode) {
  dialogMode.value = 'edit'
  dialogTitle.value = `编辑：${node.name}`
  editingNodeId.value = node.id
  dialogInitialForm.value = {
    name: node.name,
    type: node.type,
    parentId: node.parentId || null,
    manager: node.manager || '',
    phone: node.phone || '',
    location: node.location || '',
    description: node.description || '',
  }
  dialogVisible.value = true
}

async function handleDeleteNode(node: OrgNode) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${node.name}」吗？${hasChildren(node) ? `\n该组织下包含 ${node.children?.length || 0} 个子节点，删除前请先迁移或删除子节点。` : ''}`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await departmentApi.deleteDepartment(node.id)
    ElMessage.success(`已删除: ${node.name}`)
    if (selectedNodeId.value === node.id) {
      selectedNodeId.value = null
    }
    await loadOrganizationData()
  } catch (err: unknown) {
    // 用户取消时 err 为 'cancel'，仅提示非取消错误
    if (err !== 'cancel' && err instanceof Error) {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

/**
 * 找到节点在树中的父节点（根节点返回 null）
 */
function findParentNode(nodes: OrgNode[], targetId: string): OrgNode | null {
  for (const node of nodes) {
    if (node.children) {
      if (node.children.some(child => child.id === targetId)) return node
      const found = findParentNode(node.children, targetId)
      if (found) return found
    }
  }
  return null
}

/**
 * 获取节点的兄弟列表引用（根节点返回顶层数组）
 */
function getSiblingList(nodeId: string): OrgNode[] | null {
  const parent = findParentNode(organizationData.value, nodeId)
  return parent ? (parent.children ?? []) : organizationData.value
}

async function handleMoveNode(node: OrgNode, direction: 'up' | 'down') {
  const siblings = getSiblingList(node.id)
  if (!siblings || siblings.length < 2) return
  const index = siblings.findIndex(n => n.id === node.id)
  if (index === -1) return
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex < 0 || targetIndex >= siblings.length) return

  const currentSort = node.sort ?? index
  const targetNode = siblings[targetIndex]
  const targetSort = targetNode.sort ?? targetIndex

  try {
    await Promise.all([
      departmentApi.updateSortOrder(node.id, targetSort),
      departmentApi.updateSortOrder(targetNode.id, currentSort),
    ])
    // 本地交换顺序，避免重新加载树后展开状态丢失
    const tempSort = node.sort
    node.sort = targetNode.sort
    targetNode.sort = tempSort
    siblings.splice(index, 1)
    siblings.splice(targetIndex, 0, node)
    ElMessage.success(direction === 'up' ? '已上移' : '已下移')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '排序调整失败'
    ElMessage.error(msg)
    await loadOrganizationData()
  }
}

async function handleToggleStatus(node: OrgNode) {
  const isInactive = node.status === 'inactive'
  const actionText = isInactive ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}「${node.name}」吗？${!isInactive && hasChildren(node) ? '\n该操作将级联禁用其所有子组织。' : ''}`,
      `确认${actionText}`,
      { confirmButtonText: actionText, cancelButtonText: '取消', type: isInactive ? 'primary' : 'warning' }
    )
    const nextStatus = isInactive ? 'active' : 'inactive'
    await departmentApi.updateDepartmentStatus(node.id, nextStatus)
    ElMessage.success(`${actionText}成功: ${node.name}`)
    await loadOrganizationData()
  } catch (err: unknown) {
    if (err !== 'cancel' && err instanceof Error) {
      ElMessage.error(err.message || `${actionText}失败`)
    }
  }
}

async function handleDialogSuccess() {
  dialogVisible.value = false
  await loadOrganizationData()
}

watch(
  () => [...expandedKeys],
  () => saveExpandedKeys(),
  { deep: true },
)

onMounted(() => {
  loadOrganizationData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="组织架构" description="管理多级组织结构、维护层级关系、查看人员分布">
      <el-button type="primary" size="default" :loading="loading" @click="loadOrganizationData">
        <el-icon :size="16"><Search /></el-icon>刷新
      </el-button>
      <el-button type="primary" size="default" @click="handleAddTopLevel">
        <el-icon :size="16"><Plus /></el-icon>新增组织
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.key" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </section>

    <div class="search-bar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索组织名称或负责人..."
          clearable
          :prefix-icon="Search"
          style="width: 280px"
          @keyup.enter="handleSearch"
          @clear="handleReset"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <span class="node-count-badge">{{ statistics[1]?.value || 0 }} 个部门</span>
      </div>
    </div>

    <section class="org-content">
      <div v-loading="loading" class="org-layout">
        <aside class="dept-sidebar">
          <div class="sidebar-header">
            <h3 class="sidebar-title">组织架构</h3>
            <div class="sidebar-actions">
              <span class="sidebar-count">{{ statistics[0]?.value || 0 }} 个节点</span>
              <el-tooltip content="全部展开" placement="bottom">
                <el-button circle size="small" @click="expandAll">
                  <el-icon :size="14"><Expand /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="全部折叠" placement="bottom">
                <el-button circle size="small" @click="collapseAll">
                  <el-icon :size="14"><Fold /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>

          <nav class="dept-tree" role="navigation">
            <template v-for="node in filteredData" :key="node.id">
              <OrgTreeNode
                :node="node"
                :depth="0"
                :selected-id="selectedNodeId ?? undefined"
                :expanded-keys="expandedKeys"
                @select="selectNode"
                @toggle-expand="toggleExpand"
                @add-child="handleAddChild"
                @edit="handleEditNode"
                @toggle-status="handleToggleStatus"
                @delete="handleDeleteNode"
                @move-up="(n) => handleMoveNode(n, 'up')"
                @move-down="(n) => handleMoveNode(n, 'down')"
              />
            </template>

            <div v-if="!loading && filteredData.length === 0" class="no-depts">
              <p>暂无组织数据</p>
              <el-button link type="primary" size="small" @click="handleAddTopLevel">立即创建</el-button>
            </div>
          </nav>
        </aside>

        <main class="dept-detail-main">
          <OrgDetailPanel
            :node="currentNode"
            :nodes="filteredData"
            @select="selectNode"
            @edit="handleEditNode"
            @add-child="handleAddChild"
          />
        </main>
      </div>

      <footer class="footer-stats">
        <span class="stat-item">{{ statistics[0]?.value || 0 }} 个组织单元</span>
        <span class="stat-divider">|</span>
        <span class="stat-item">{{ statistics[1]?.value || 0 }} 个部门</span>
        <span class="stat-divider">|</span>
        <span class="stat-item">{{ statistics[2]?.value || 0 }} 个门店</span>
        <span class="stat-divider">|</span>
        <span class="stat-item">{{ statistics[3]?.value || 0 }} 名人员</span>
        <span class="stat-divider">|</span>
        <span class="stat-item">当前: {{ currentNode?.name || '未选择' }}</span>
      </footer>
    </section>

    <!-- 新增/编辑组织对话框 -->
    <OrgFormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :mode="dialogMode"
      :nodes="organizationData"
      :editing-node-id="editingNodeId"
      :initial-form="dialogInitialForm"
      @success="handleDialogSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }

  .toolbar-right {
    display: flex;
    align-items: center;
  }

  .node-count-badge {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    background: var(--fts-bg-tertiary);
    padding: 2px var(--fts-space-3);
    border-radius: var(--fts-radius-full);
  }
}

.org-content {
  padding: var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
  margin-bottom: var(--fts-space-5);
}

.org-layout {
  display: flex;
  gap: var(--fts-space-6);
  min-height: 580px;

  @media (max-width: 1024px) {
    flex-direction: column;
  }
}

.dept-sidebar {
  width: 340px;
  min-width: 300px;
  max-width: 420px;
  background: var(--fts-bg-tertiary);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  resize: horizontal;

  @media (max-width: 1024px) {
    width: 100%;
    max-width: 100%;
    max-height: 400px;
    resize: none;
  }

  .sidebar-header {
    padding: var(--fts-space-4) var(--fts-space-5);
    border-bottom: 1px solid var(--fts-border-primary);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
    border-radius: var(--fts-radius-lg) var(--fts-radius-lg) 0 0;

    .sidebar-title {
      font-size: var(--fts-font-size-base);
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-text-primary);
      margin: 0;
    }

    .sidebar-count {
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-secondary);
      background: var(--fts-bg-card);
      padding: 2px var(--fts-space-2);
      border-radius: var(--fts-radius-full);
    }

    .sidebar-actions {
      display: flex;
      align-items: center;
      gap: var(--fts-space-2);

      .el-button {
        border-color: transparent;

        &:hover {
          background: var(--fts-bg-hover);
          border-color: var(--fts-border-primary);
        }
      }
    }
  }

  .dept-tree {
    flex: 1;
    overflow: auto;
    padding: var(--fts-space-2);

    &::-webkit-scrollbar { width: 6px; height: 6px; }
    &::-webkit-scrollbar-thumb {
      background: var(--fts-border-primary);
      border-radius: 3px;
    }
  }

  .no-depts {
    text-align: center;
    padding: var(--fts-space-8) var(--fts-space-4);
    color: var(--fts-text-tertiary);

    p { margin-bottom: var(--fts-space-3); font-size: var(--fts-font-size-sm); }
  }
}

.dept-detail-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
  overflow-y: auto;
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  background: var(--fts-bg-card);
  padding: var(--fts-space-5);

  @media (max-width: 1024px) {
    min-height: 500px;
  }
}

.footer-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-4);
  margin-top: var(--fts-space-6);
  padding: var(--fts-space-4) var(--fts-space-6);
  background: linear-gradient(180deg, transparent 0%, var(--fts-bg-tertiary) 100%);
  border-top: 1px solid var(--fts-border-primary);
  border-radius: 0 0 var(--fts-radius-lg) var(--fts-radius-lg);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);

  .stat-divider { color: var(--fts-border-primary); }
  .stat-item { font-weight: var(--fts-font-weight-medium); }
}

@media (max-width: 768px) {
  .org-layout { gap: var(--fts-space-4); }

  .dept-sidebar {
    width: 100%;
    max-height: 350px;
  }

  .dept-detail-main { padding: var(--fts-space-3); }

  .footer-stats {
    flex-direction: column;
    gap: var(--fts-space-2);
  }
}
</style>
