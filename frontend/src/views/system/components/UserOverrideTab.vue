<script setup lang="ts">
/**
 * 用户权限覆盖 Tab 组件
 * 对应后端: /v1/user-permission-overrides (UserPermissionOverrideController)
 *
 * 功能：用户权限覆盖列表、搜索筛选、权限编辑、详情查看、重置权限
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, Edit, RefreshLeft, User as UserIcon } from '@element-plus/icons-vue'

import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { usePermissionStore } from '@/stores/permission'
import {
  userPermissionOverrideApi,
  type UserPermissionOverrideVO,
} from '@/api/system/user-permission-override'
import type { OverrideStatus } from '@/types/permission'

const permissionStore = usePermissionStore()

// ==================== 类型定义 ====================

/** 用户权限覆盖列表项（扩展自 UserPermissionOverrideVO） */
interface UserOverrideItem {
  id: string
  userId: string
  username: string
  fullName: string
  departmentName: string
  roleNames: string
  overrideCount: number
  status: OverrideStatus
  lastModifiedTime: string
}

/** 权限树节点 */
interface PermissionTreeNode {
  id: string
  label: string
  children?: PermissionTreeNode[]
  /** 是否为继承权限 */
  inherited?: boolean
  /** 是否为自定义权限 */
  custom?: boolean
}

// ==================== 响应式数据 ====================

const loading = ref(false)
const total = ref(0)

/** 分页参数 */
const pagination = reactive({
  page: 1,
  pageSize: 20,
})

/** 搜索参数 */
const searchForm = reactive({
  keyword: '',
  departmentId: '',
  status: '' as '' | OverrideStatus,
})

/** 部门选项列表（从真实 API 加载，使用共享 composable，autoLoad 自动加载） */
const { departmentOptions } = useDepartmentOptions(true)

/** 用户权限覆盖列表 */
const overrideList = ref<UserOverrideItem[]>([])

/** 状态选项 */
const statusOptions: { value: OverrideStatus | ''; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'ACTIVE', label: '生效中' },
  { value: 'PENDING', label: '待审批' },
  { value: 'EXPIRED', label: '已过期' },
  { value: 'REVOKED', label: '已撤销' },
  { value: 'REJECTED', label: '已拒绝' },
]

// ==================== 权限编辑对话框 ====================

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const currentEditUser = ref<UserOverrideItem | null>(null)

/** 权限树数据 */
const permissionTreeData = ref<PermissionTreeNode[]>([])
const permTreeRef = ref()

/** 继承的权限码（只读显示） */
const inheritedPermissions = ref<string[]>([])
/** 自定义添加的权限码 */
const customAddPermissions = ref<string[]>([])
/** 自定义移除的权限码 */
const customRemovePermissions = ref<string[]>([])

// ==================== 详情对话框 ====================

const detailDialogVisible = ref(false)
const currentDetailUser = ref<UserOverrideItem | null>(null)
const detailLoading = ref(false)
const userOverrideRecords = ref<UserPermissionOverrideVO[]>([])

// ==================== 计算属性 ====================

/** 权限树数据（带继承/自定义标记） */
const permissionTreeWithMarkers = computed(() => {
  return buildPermissionTreeWithMarkers(permissionTreeData.value)
})

// ==================== 表格列定义 ====================

const columns: DataTableColumn[] = [
  { prop: 'username', label: '用户账号', minWidth: 120, fixed: 'left' as const },
  { prop: 'fullName', label: '用户姓名', minWidth: 100 },
  { prop: 'departmentName', label: '所属部门', minWidth: 120 },
  { prop: 'roleNames', label: '角色', minWidth: 150, showOverflowTooltip: true },
  { prop: 'overrideCount', label: '权限覆盖数', minWidth: 110, align: 'center', slot: 'overrideCount' as const },
  { prop: 'status', label: '状态', minWidth: 100, align: 'center', slot: 'status' as const },
  { prop: 'lastModifiedTime', label: '最后修改时间', minWidth: 170 },
]

// ==================== 辅助方法 ====================

/** 状态映射到 StatusTag 类型 */
function getStatusType(status: OverrideStatus): string {
  const map: Record<OverrideStatus, string> = {
    ACTIVE: 'active',
    PENDING: 'pending',
    EXPIRED: 'expired',
    REVOKED: 'inactive',
    REJECTED: 'rejected',
  }
  return map[status] || 'info'
}

/** 状态映射到显示文本 */
function getStatusLabel(status: OverrideStatus): string {
  const map: Record<OverrideStatus, string> = {
    ACTIVE: '生效中',
    PENDING: '待审批',
    EXPIRED: '已过期',
    REVOKED: '已撤销',
    REJECTED: '已拒绝',
  }
  return map[status] || status
}

/** 构建带标记的权限树 */
function buildPermissionTreeWithMarkers(nodes: PermissionTreeNode[]): PermissionTreeNode[] {
  return nodes.map(node => {
    const result: PermissionTreeNode = {
      ...node,
      inherited: inheritedPermissions.value.includes(node.id),
      custom: customAddPermissions.value.includes(node.id) || customRemovePermissions.value.includes(node.id),
    }
    if (node.children && node.children.length > 0) {
      result.children = buildPermissionTreeWithMarkers(node.children)
    }
    return result
  })
}

/** 初始化权限树数据（模拟数据） */
function initPermissionTree(): PermissionTreeNode[] {
  const groups = permissionStore.registeredMenus || []
  return groups.map((group: any) => ({
    id: `${group.id}:*`,
    label: group.title,
    children: (group.children || []).map((child: any) => ({
      id: `${group.id}:${child.path?.split('/').pop() || child.title}`,
      label: child.title,
    })),
  }))
}

// ==================== 列表加载 ====================

async function loadList() {
  loading.value = true
  try {
    const res = await userPermissionOverrideApi.getPage({
      page: pagination.page,
      size: pagination.pageSize,
      userName: searchForm.keyword || undefined,
      status: searchForm.status || undefined,
    })
    const records = res.records || []
    overrideList.value = records.map((item, index) => ({
      id: String(item.id),
      userId: item.userId,
      username: item.userName || `user_${item.userId}`,
      fullName: item.userName || '未知用户',
      departmentName: '未分配部门',
      roleNames: '普通用户',
      overrideCount: 1,
      status: item.status,
      lastModifiedTime: item.updatedAt,
    }))
    total.value = res.total || 0
  } catch {
    overrideList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.departmentId = ''
  searchForm.status = ''
  pagination.page = 1
  loadList()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadList()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  loadList()
}

// ==================== 详情 ====================

async function handleDetail(row: UserOverrideItem) {
  currentDetailUser.value = row
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const records = await userPermissionOverrideApi.getByUserId(row.userId)
    userOverrideRecords.value = records
  } catch {
    userOverrideRecords.value = []
  } finally {
    detailLoading.value = false
  }
}

// ==================== 编辑权限 ====================

async function handleEditPermission(row: UserOverrideItem) {
  currentEditUser.value = row
  editDialogVisible.value = true
  permissionTreeData.value = initPermissionTree()

  inheritedPermissions.value = [
    'product:*',
    'order:*',
  ]
  customAddPermissions.value = []
  customRemovePermissions.value = []

  try {
    const records = await userPermissionOverrideApi.getByUserId(row.userId)
    records.forEach(record => {
      if (record.overrideType === 'ADD') {
        customAddPermissions.value.push(record.permissionCode)
      } else if (record.overrideType === 'REMOVE') {
        customRemovePermissions.value.push(record.permissionCode)
      }
    })
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

async function handleEditSubmit() {
  if (!currentEditUser.value) return
  editSubmitting.value = true
  try {
    const userId = currentEditUser.value.userId
    const userName = currentEditUser.value.fullName

    // 调用真实 API 创建权限覆盖（ADD/REMOVE）
    const addPromises = customAddPermissions.value.map(permissionCode =>
      userPermissionOverrideApi.create({
        userId,
        userName,
        permissionCode,
        overrideType: 'ADD',
      })
    )
    const removePromises = customRemovePermissions.value.map(permissionCode =>
      userPermissionOverrideApi.create({
        userId,
        userName,
        permissionCode,
        overrideType: 'REMOVE',
      })
    )

    const results = await Promise.allSettled([...addPromises, ...removePromises])
    const failedCount = results.filter(r => r.status === 'rejected').length

    if (failedCount > 0) {
      ElMessage.warning(`权限覆盖部分更新失败（${failedCount}/${results.length}）`)
    } else if (results.length > 0) {
      ElMessage.success('权限覆盖更新成功')
    } else {
      ElMessage.info('未发生权限变更')
    }
    editDialogVisible.value = false
    loadList()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    editSubmitting.value = false
  }
}

// ==================== 重置权限 ====================

async function handleResetPermission(row: UserOverrideItem) {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户「${row.fullName}」的所有权限覆盖吗？重置后该用户的权限将完全由角色决定。`,
      '确认重置',
      { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' }
    )
    // 调用真实 API 删除该用户的所有权限覆盖
    const records = await userPermissionOverrideApi.getByUserId(row.userId)
    const results = await Promise.allSettled(
      records.map(record => userPermissionOverrideApi.remove(record.id))
    )
    const failedCount = results.filter(r => r.status === 'rejected').length
    if (failedCount > 0) {
      ElMessage.warning(`权限覆盖部分重置失败（${failedCount}/${results.length}）`)
    } else {
      ElMessage.success('权限覆盖已重置')
    }
    loadList()
  } catch {
    // 用户取消或请求失败
  }
}

// ==================== 权限树节点 class 渲染 ====================

/** 权限树节点 class 名称 */
function getNodeClass(node: PermissionTreeNode): string {
  const classes: string[] = []
  if (node.inherited) classes.push('perm-inherited')
  if (node.custom) classes.push('perm-custom')
  return classes.join(' ')
}

// ==================== 初始化 ====================

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="user-override-tab">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="用户账号/姓名搜索"
        clearable
        style="width: 220px"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchForm.departmentId"
        placeholder="部门筛选"
        clearable
        style="width: 160px"
        :teleported="false"
      >
        <el-option
          v-for="dept in departmentOptions"
          :key="dept.id"
          :label="dept.name"
          :value="dept.id"
        />
      </el-select>
      <el-select
        v-model="searchForm.status"
        placeholder="状态筛选"
        clearable
        style="width: 140px"
        :teleported="false"
      >
        <el-option
          v-for="s in statusOptions"
          :key="s.value"
          :label="s.label"
          :value="s.value"
        />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="overrideList"
        :loading="loading"
        stripe
        :actions-width="260"
      >
        <template #overrideCount="{ row }">
          <StatusTag
            size="small"
            :status="row.overrideCount > 0 ? 'warning' : 'info'"
            :label="`${row.overrideCount} 项`"
          />
        </template>
        <template #status="{ row }">
          <StatusTag :status="getStatusType(row.status)" :label="getStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" :icon="View" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" :icon="Edit" @click="handleEditPermission(row)">编辑权限</el-button>
          <el-button
            link
            type="warning"
            size="small"
            :icon="RefreshLeft"
            :disabled="row.overrideCount === 0"
            @click="handleResetPermission(row)"
          >
            重置权限
          </el-button>
        </template>
      </DataTable>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 权限编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑用户权限"
      width="720px"
      :close-on-click-modal="false"
      class="permission-edit-dialog"
    >
      <div v-if="currentEditUser" class="edit-content">
        <!-- 用户基本信息 -->
        <div class="user-info-card">
          <div class="user-avatar">
            <el-icon :size="32"><UserIcon /></el-icon>
          </div>
          <div class="user-info">
            <div class="user-name">{{ currentEditUser.fullName }}</div>
            <div class="user-meta">
              <span>{{ currentEditUser.username }}</span>
              <span class="meta-divider">|</span>
              <span>{{ currentEditUser.departmentName }}</span>
              <span class="meta-divider">|</span>
              <span>{{ currentEditUser.roleNames }}</span>
            </div>
          </div>
        </div>

        <!-- 图例说明 -->
        <div class="perm-legend">
          <div class="legend-item">
            <span class="legend-dot inherited"></span>
            <span>继承权限（来自角色）</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot custom"></span>
            <span>自定义覆盖</span>
          </div>
        </div>

        <!-- 权限树 -->
        <div class="permission-tree-wrapper">
          <div class="tree-header">
            <span class="tree-title">权限列表</span>
            <span class="tree-tip">勾选添加权限，取消勾选移除权限</span>
          </div>
          <el-tree
            ref="permTreeRef"
            :data="permissionTreeWithMarkers"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ label: 'label', children: 'children' }"
          >
            <template #default="{ node, data }">
              <span class="perm-tree-node" :class="getNodeClass(data)">
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.inherited" class="node-tag inherited-tag">继承</span>
                <span v-if="data.custom" class="node-tag custom-tag">自定义</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="用户权限详情"
      width="720px"
      :close-on-click-modal="false"
    >
      <div v-if="currentDetailUser" class="detail-content">
        <!-- 用户基本信息 -->
        <div class="detail-section">
          <div class="section-title">用户信息</div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="用户账号">{{ currentDetailUser.username }}</el-descriptions-item>
            <el-descriptions-item label="用户姓名">{{ currentDetailUser.fullName }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ currentDetailUser.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ currentDetailUser.roleNames }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getStatusType(currentDetailUser.status)" :label="getStatusLabel(currentDetailUser.status)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="最后修改时间">{{ currentDetailUser.lastModifiedTime }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 权限覆盖记录 -->
        <div class="detail-section">
          <div class="section-title">
            <span>权限覆盖记录</span>
            <span class="section-count">共 {{ userOverrideRecords.length }} 条</span>
          </div>
          <div v-loading="detailLoading" class="override-records">
            <el-empty v-if="!detailLoading && userOverrideRecords.length === 0" description="暂无权限覆盖记录" />
            <div v-else class="record-list">
              <div
                v-for="record in userOverrideRecords"
                :key="record.id"
                class="record-item"
              >
                <div class="record-header">
                  <StatusTag
                    :status="record.overrideType === 'ADD' ? 'success' : 'error'"
                    :label="record.overrideType === 'ADD' ? '添加权限' : '移除权限'"
                    size="small"
                  />
                  <StatusTag
                    :status="getStatusType(record.status)"
                    :label="getStatusLabel(record.status)"
                    size="small"
                  />
                </div>
                <div class="record-perm">
                  <span class="perm-name">{{ record.permissionName || record.permissionCode }}</span>
                  <code class="perm-code">{{ record.permissionCode }}</code>
                </div>
                <div v-if="record.reason" class="record-reason">
                  <span class="reason-label">原因：</span>
                  <span>{{ record.reason }}</span>
                </div>
                <div class="record-meta">
                  <span>有效期：{{ record.expireTime ? record.expireTime : '永久' }}</span>
                  <span>创建时间：{{ record.createdAt }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEditPermission(currentDetailUser!)">编辑权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.user-override-tab {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.table-section {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fts-space-3);
}

// ========== 权限编辑对话框 ==========

.permission-edit-dialog {
  :deep(.el-dialog__body) {
    padding-top: var(--fts-space-3);
  }
}

.edit-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.user-info-card {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-primary);
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--fts-primary-light, rgba(64, 158, 255, 0.1));
  color: var(--fts-primary, #409eff);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-1);
}

.user-meta {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.meta-divider {
  color: var(--fts-border-primary);
}

.perm-legend {
  display: flex;
  gap: var(--fts-space-6);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-sm);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;

  &.inherited {
    background: var(--fts-info, #909399);
  }

  &.custom {
    background: var(--fts-warning, #e6a23c);
  }
}

.permission-tree-wrapper {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  overflow: hidden;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-tertiary);
  border-bottom: 1px solid var(--fts-border-primary);
}

.tree-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.tree-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.perm-tree-node {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  width: 100%;

  &.perm-inherited {
    .node-label {
      color: var(--fts-text-secondary);
    }
  }

  &.perm-custom {
    .node-label {
      color: var(--fts-warning, #e6a23c);
      font-weight: 500;
    }
  }
}

.node-label {
  flex: 1;
  font-size: var(--fts-font-size-sm);
}

.node-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--fts-radius-xs, 2px);
  line-height: 1.4;

  &.inherited-tag {
    background: var(--fts-info-bg, rgba(144, 147, 153, 0.1));
    color: var(--fts-info, #909399);
  }

  &.custom-tag {
    background: var(--fts-warning-bg, rgba(230, 162, 60, 0.1));
    color: var(--fts-warning, #e6a23c);
  }
}

// ========== 详情对话框 ==========

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.section-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-count {
  font-size: var(--fts-font-size-xs);
  font-weight: normal;
  color: var(--fts-text-tertiary);
}

.override-records {
  min-height: 120px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.record-item {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.record-header {
  display: flex;
  gap: var(--fts-space-2);
}

.record-perm {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.perm-name {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  font-weight: 500;
}

.perm-code {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  background: var(--fts-bg-tertiary);
  padding: 2px 8px;
  border-radius: var(--fts-radius-sm);
  display: inline-block;
}

.record-reason {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.reason-label {
  color: var(--fts-text-tertiary);
}

.record-meta {
  display: flex;
  gap: var(--fts-space-6);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}
</style>
