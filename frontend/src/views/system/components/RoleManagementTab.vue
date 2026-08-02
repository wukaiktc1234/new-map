<script setup lang="ts">
/**
 * 角色管理 Tab 组件
 * 对应后端: /v1/roles (RoleController)
 *
 * 功能：角色列表、搜索、新增、编辑、删除、分配权限、分配用户、启用/禁用
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { roleApi, type RoleVO, type RoleQueryParams } from '@/api/system/role'
import { userApi, type UserItem } from '@/api/system/user'

/** 模拟用户数据（用于分配用户对话框演示） */
const mockUsers: UserItem[] = [
  { id: 1, username: 'admin', fullName: '系统管理员', status: 1 } as UserItem,
  { id: 2, username: 'manager', fullName: '门店经理', status: 1 } as UserItem,
  { id: 3, username: 'staff', fullName: '普通员工', status: 1 } as UserItem,
  { id: 4, username: 'chef', fullName: '厨师长', status: 1 } as UserItem,
  { id: 5, username: 'waiter', fullName: '服务员', status: 1 } as UserItem,
]

/** 角色列表 */
const roles = ref<RoleVO[]>([])
const loading = ref(false)
const total = ref(0)

/** 分页参数 */
const pagination = reactive({
  page: 1,
  pageSize: 20,
})

/** 搜索参数 */
const searchForm = reactive<RoleQueryParams>({
  roleName: '',
  status: '',
})

/** 新增/编辑对话框状态 */
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)

/** 权限分配对话框状态 */
const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSubmitting = ref(false)
const permRoleId = ref<number | null>(null)
const permRoleName = ref('')
const permTreeRef = ref()

/** 分配用户对话框状态 */
const userDialogVisible = ref(false)
const userLoading = ref(false)
const userSubmitting = ref(false)
const userRoleId = ref<number | null>(null)
const userRoleName = ref('')
const allUsers = ref<UserItem[]>([])
const selectedUserIds = ref<string[]>([])

/** 角色详情对话框状态 */
const detailDialogVisible = ref(false)
const detailRole = ref<RoleVO | null>(null)

/** 表单数据 */
const form = reactive({
  roleCode: '',
  roleName: '',
  roleDescription: '',
  status: 1 as number,
})

/** 状态映射：数字 -> 语义字符串（供 StatusTag 使用）*/
function getStatusText(status: number): 'active' | 'inactive' {
  return status === 1 ? 'active' : 'inactive'
}

/** 权限树节点 */
interface PermissionTreeNode {
  label: string
  value?: string
  children?: PermissionTreeNode[]
}

/** 权限树数据（基于常见权限码构建） */
const permissionTree: PermissionTreeNode[] = [
  {
    label: '系统管理',
    value: 'system',
    children: [
      {
        label: '角色管理',
        value: 'system:role',
        children: [
          { label: '查看', value: 'system:role:read' },
          { label: '创建', value: 'system:role:create' },
          { label: '更新', value: 'system:role:update' },
          { label: '删除', value: 'system:role:delete' },
          { label: '状态切换', value: 'system:role:update-status' },
          { label: '分配权限', value: 'system:role:assign-permissions' },
        ],
      },
      {
        label: '用户管理',
        value: 'system:user',
        children: [
          { label: '查看', value: 'system:user:read' },
          { label: '创建', value: 'system:user:create' },
          { label: '更新', value: 'system:user:update' },
          { label: '删除', value: 'system:user:delete' },
        ],
      },
      {
        label: '权限管理',
        value: 'system:permission',
        children: [
          { label: '查看', value: 'system:permission:read' },
          { label: '配置', value: 'system:permission:config' },
        ],
      },
    ],
  },
  {
    label: '食品溯源',
    value: 'food',
    children: [
      { label: '全部', value: 'food:*' },
      { label: '查看', value: 'food:read' },
      { label: '创建', value: 'food:create' },
      { label: '更新', value: 'food:update' },
    ],
  },
  {
    label: '溯源记录',
    value: 'trace',
    children: [
      { label: '全部', value: 'trace:*' },
      { label: '查看', value: 'trace:read' },
      { label: '创建', value: 'trace:create' },
    ],
  },
  {
    label: '审计日志',
    value: 'audit',
    children: [
      { label: '查看', value: 'audit:read' },
      { label: '导出', value: 'audit:export' },
    ],
  },
]

/** 表格列定义 */
const columns: DataTableColumn[] = [
  { prop: 'roleCode', label: '角色编码', minWidth: 140, fixed: 'left' as const },
  { prop: 'roleName', label: '角色名称', minWidth: 130 },
  { prop: 'roleDescription', label: '角色描述', minWidth: 200, showOverflowTooltip: true },
  { prop: 'roleType', label: '类型', minWidth: 100, align: 'center', slot: 'roleType' as const },
  { prop: 'status', label: '状态', minWidth: 85, align: 'center', slot: 'status' as const },
  { prop: 'createdTime', label: '创建时间', minWidth: 160 },
]

/** 加载角色列表 */
async function loadRoles() {
  loading.value = true
  try {
    const params: RoleQueryParams = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.roleName?.trim()) params.roleName = searchForm.roleName.trim()
    if (searchForm.status !== '' && searchForm.status !== undefined) params.status = searchForm.status

    const result = await roleApi.getPage(params)
    roles.value = result?.records || []
    total.value = result?.total || 0
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

/** 执行搜索 */
function handleSearch() {
  pagination.page = 1
  loadRoles()
}

/** 重置搜索条件 */
function handleReset() {
  searchForm.roleName = ''
  searchForm.status = ''
  pagination.page = 1
  loadRoles()
}

/** 翻页 */
function handlePageChange(page: number) {
  pagination.page = page
  loadRoles()
}

/** 每页条数变化 */
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  loadRoles()
}

/** 打开详情对话框 */
function handleDetail(row: RoleVO) {
  detailRole.value = row
  detailDialogVisible.value = true
}

/** 打开新建对话框 */
function handleCreate() {
  dialogType.value = 'create'
  editingId.value = null
  Object.assign(form, {
    roleCode: '',
    roleName: '',
    roleDescription: '',
    status: 1,
  })
  dialogVisible.value = true
}

/** 打开编辑对话框 */
function handleEdit(row: RoleVO) {
  dialogType.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    roleDescription: row.roleDescription || '',
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

/** 删除角色 */
async function handleDelete(row: RoleVO) {
  if (row.systemBuilt) {
    ElMessage.warning('系统内置角色不允许删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除角色「${row.roleName}」吗？删除后关联的用户将失去该角色权限。`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await roleApi.remove(row.id)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch {
    // 用户取消或请求失败
  }
}

/** 提交创建/编辑表单 */
async function handleSubmit() {
  if (!form.roleName.trim()) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (!form.roleCode.trim()) {
    ElMessage.warning('请输入角色编码')
    return
  }
  submitting.value = true
  try {
    if (dialogType.value === 'create') {
      await roleApi.create({
        roleCode: form.roleCode.trim(),
        roleName: form.roleName.trim(),
        roleDescription: form.roleDescription,
        status: form.status,
      })
      ElMessage.success('角色创建成功')
    } else {
      if (editingId.value === null) return
      await roleApi.update(editingId.value, {
        roleName: form.roleName.trim(),
        roleDescription: form.roleDescription,
        status: form.status,
      })
      ElMessage.success('角色更新成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

/** 切换角色启用/禁用状态 */
async function handleToggleStatus(row: RoleVO) {
  if (row.systemBuilt) {
    ElMessage.warning('系统内置角色不允许修改状态')
    return
  }
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await roleApi.toggleStatus(row.id, newStatus)
    ElMessage.success('状态切换成功')
    loadRoles()
  } catch {
    // 错误已由请求拦截器统一提示
  }
}

/** 打开权限分配对话框 */
async function handleAssignPermissions(row: RoleVO) {
  permRoleId.value = row.id
  permRoleName.value = row.roleName
  permDialogVisible.value = true
  permLoading.value = true
  try {
    const perms = await roleApi.getPermissions(row.id)
    // 等待 tree 渲染后设置选中节点
    setTimeout(() => {
      permTreeRef.value?.setCheckedKeys(perms || [])
    }, 50)
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    permLoading.value = false
  }
}

/** 提交权限分配 */
async function handlePermSubmit() {
  if (permRoleId.value === null) return
  permSubmitting.value = true
  try {
    const checked = (permTreeRef.value?.getCheckedKeys() || []) as unknown[]
    const allPerms = checked.filter((k): k is string => typeof k === 'string' && k.length > 0)
    await roleApi.assignPermissions(permRoleId.value, allPerms)
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    permSubmitting.value = false
  }
}

/** 角色类型映射 */
function getRoleTypeText(roleType: number): string {
  return roleType === 1 ? '系统角色' : '自定义角色'
}

/** 打开分配用户对话框 */
async function handleAssignUsers(row: RoleVO) {
  userRoleId.value = row.id
  userRoleName.value = row.roleName
  userDialogVisible.value = true
  userLoading.value = true
  selectedUserIds.value = []
  try {
    // 加载所有用户
    const result = await userApi.getList({ page: 1, pageSize: 1000 })
    allUsers.value = result?.records || []
    // 如果没有用户数据，使用模拟数据
    if (allUsers.value.length === 0) {
      allUsers.value = mockUsers
    }
  } catch {
    // API 调用失败时使用模拟数据
    allUsers.value = mockUsers
  } finally {
    userLoading.value = false
  }
}

/** 提交用户分配 */
async function handleUserSubmit() {
  if (userRoleId.value === null) return
  userSubmitting.value = true
  try {
    // 逐个为用户分配角色
    const promises = selectedUserIds.value.map(userId =>
      userApi.assignRoles(Number(userId), [String(userRoleId.value)])
    )
    await Promise.all(promises)
    ElMessage.success('用户分配成功')
    userDialogVisible.value = false
  } catch {
    ElMessage.warning('部分用户分配可能失败，请检查')
    userDialogVisible.value = false
  } finally {
    userSubmitting.value = false
  }
}

/** 初始化加载 */
onMounted(() => {
  loadRoles()
})
</script>

<template>
  <div class="role-management">
    <div class="section-header">
      <div class="section-desc">管理系统角色及其权限分配</div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.roleName as string"
        placeholder="角色名称"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchForm.status"
        placeholder="状态"
        clearable
        style="width: 120px"
        :teleported="false"
      >
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="roles"
        :loading="loading"
        stripe
        :actions-width="320"
      >
        <template #roleType="{ row }">
          <StatusTag
            :status="row.roleType === 1 ? 'success' : 'info'"
            :label="getRoleTypeText(row.roleType)"
            size="small"
          />
        </template>
        <template #status="{ row }">
          <StatusTag :status="getStatusText(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleAssignPermissions(row)">分配权限</el-button>
          <el-button link type="primary" size="small" @click="handleAssignUsers(row)">分配用户</el-button>
          <el-button link type="warning" size="small" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            link
            type="danger"
            size="small"
            :disabled="row.systemBuilt"
            @click="handleDelete(row)"
          >
            删除
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

    <!-- 角色详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="角色详情"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-descriptions :column="1" border v-if="detailRole">
        <el-descriptions-item label="角色编码">{{ detailRole.roleCode }}</el-descriptions-item>
        <el-descriptions-item label="角色名称">{{ detailRole.roleName }}</el-descriptions-item>
        <el-descriptions-item label="角色描述">{{ detailRole.roleDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="getStatusText(detailRole.status)" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="角色类型">
          {{ detailRole.roleType === 1 ? '系统角色' : '自定义角色' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否内置">
          {{ detailRole.systemBuilt ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRole.createdTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增角色' : '编辑角色'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" label-position="top">
        <el-form-item label="角色编码" required>
          <el-input
            v-model="form.roleCode"
            placeholder="请输入角色编码"
            maxlength="50"
            show-word-limit
            :disabled="dialogType === 'edit'"
          />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" placeholder="请输入角色名称" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input
            v-model="form.roleDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%" :teleported="false">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogType === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 权限分配对话框 -->
    <el-dialog
      v-model="permDialogVisible"
      :title="`分配权限 - ${permRoleName}`"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-loading="permLoading" class="perm-tree-wrapper">
        <div class="perm-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>勾选权限节点后保存即可。父节点勾选表示授予该模块的所有权限。</span>
        </div>
        <el-tree
          ref="permTreeRef"
          :data="permissionTree"
          show-checkbox
          node-key="value"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
          check-strictly
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="handlePermSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="`分配用户 - ${userRoleName}`"
      width="520px"
      :close-on-click-modal="false"
    >
      <div v-loading="userLoading" class="user-list-wrapper">
        <div class="perm-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>勾选用户后保存即可。用户将获得该角色的全部权限。</span>
        </div>
        <el-checkbox-group v-model="selectedUserIds" class="user-checkbox-group">
          <el-checkbox
            v-for="user in allUsers"
            :key="user.id"
            :label="String(user.id)"
            class="user-checkbox-item"
          >
            <span class="user-name">{{ user.fullName || user.username }}</span>
            <span class="user-code">{{ user.username }}</span>
          </el-checkbox>
        </el-checkbox-group>
        <el-empty v-if="allUsers.length === 0 && !userLoading" description="暂无可用用户" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSubmitting" @click="handleUserSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.role-management {
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
}

.section-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.search-bar {
  display: flex;
  gap: var(--fts-space-2);
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: var(--fts-space-3);
  padding: var(--fts-space-3);
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

.perm-tree-wrapper {
  min-height: 200px;
}

.perm-hint {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  margin-bottom: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);

  .el-icon {
    color: var(--fts-primary);
    flex-shrink: 0;
  }
}

.user-list-wrapper {
  min-height: 200px;
}

.user-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  max-height: 320px;
  overflow-y: auto;
  padding: var(--fts-space-2);
}

.user-checkbox-item {
  display: flex;
  align-items: center;
  height: auto;
  padding: var(--fts-space-2) var(--fts-space-3);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-sm);
  margin: 0;

  .user-name {
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  .user-code {
    margin-left: var(--fts-space-2);
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    font-family: monospace;
  }
}
</style>
