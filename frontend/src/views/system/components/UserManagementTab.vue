<script setup lang="ts">
/**
 * 用户管理 Tab 组件
 * 对应后端: /v1/users (UserController)
 *
 * 功能：用户列表、搜索筛选、新增/编辑、重置密码、角色分配、启用/禁用、删除
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, User as UserIcon, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { userApi, type UserItem, type UserQueryParams } from '@/api/system/user'
import { roleApi, type RoleVO } from '@/api/system/role'

// ==================== 类型定义 ====================

/** 用户表单数据 */
interface UserFormData {
  username: string
  password: string
  fullName: string
  phone: string
  email: string
  departmentId: string
  position: string
  roleIds: string[]
  status: number
}

/** 重置密码表单 */
interface ResetPasswordForm {
  newPassword: string
  confirmPassword: string
}

// ==================== 响应式数据 ====================

const loading = ref(false)
const total = ref(0)
const selectedRows = ref<UserItem[]>([])

/** 分页参数 */
const pagination = reactive({
  page: 1,
  pageSize: 20,
})

/** 搜索参数 */
const searchForm = reactive<UserQueryParams>({
  username: '',
  fullName: '',
  department: '',
  status: '',
})

/** 部门选项列表（从真实 API 加载，使用共享 composable，autoLoad 自动加载）
 *  el-tree-select 使用 departmentTree（树结构），el-select 使用 departmentOptions（扁平结构）
 */
const { departmentTree } = useDepartmentOptions(true)

/** 门店选项列表（从真实 API 加载，使用共享 composable，autoLoad 自动加载） */
const { storeOptions } = useStoreOptions(true)

/** 角色选项列表 */
const roleOptions = ref<RoleVO[]>([])

/** 用户列表 */
const users = ref<UserItem[]>([])

// ==================== 对话框状态 ====================

/** 新增/编辑对话框 */
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

/** 表单数据 */
const formData = reactive<UserFormData>({
  username: '',
  password: '',
  fullName: '',
  phone: '',
  email: '',
  departmentId: '',
  position: '',
  roleIds: [],
  status: 1,
})

/** 表单验证规则 */
const formRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为6-100个字符', trigger: 'blur' },
  ],
  fullName: [
    { required: true, message: '请输入用户姓名', trigger: 'blur' },
    { max: 30, message: '姓名长度不能超过30个字符', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
})

/** 重置密码对话框 */
const resetPasswordVisible = ref(false)
const resetPasswordUserId = ref<number | null>(null)
const resetPasswordUserName = ref('')
const resetPasswordForm = reactive<ResetPasswordForm>({
  newPassword: '',
  confirmPassword: '',
})
const resetPasswordFormRef = ref<FormInstance>()
const resetPasswordSubmitting = ref(false)

/** 重置密码表单验证规则 */
const resetPasswordRules = reactive<FormRules>({
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为6-100个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== resetPasswordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
})

/** 用户详情对话框 */
const detailVisible = ref(false)
const currentUser = ref<UserItem | null>(null)

// ==================== 工具函数 ====================

/**
 * 状态映射：数字状态转 StatusTag 语义
 * 1 → active, 0 → inactive
 */
function getStatusType(status: number): 'active' | 'inactive' {
  return status === 1 ? 'active' : 'inactive'
}

/**
 * 解析角色名称列表（后端返回 JSON 字符串）
 */
function parseRoleNames(roleNames: string): string[] {
  if (!roleNames) return []
  try {
    const parsed = JSON.parse(roleNames)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// ==================== 表格列定义 ====================

const columns: DataTableColumn[] = [
  { prop: 'username', label: '用户账号', minWidth: 120, fixed: 'left' as const },
  { prop: 'fullName', label: '用户姓名', minWidth: 100 },
  { prop: 'phone', label: '手机号', minWidth: 130 },
  { prop: 'email', label: '邮箱', minWidth: 180, showOverflowTooltip: true },
  { prop: 'department', label: '所属部门', minWidth: 120, slot: 'department' as const },
  { prop: 'roleNames', label: '角色', minWidth: 160, slot: 'roleNames' as const, showOverflowTooltip: true },
  { prop: 'status', label: '状态', minWidth: 90, align: 'center', slot: 'status' as const },
  { prop: 'lastLoginTime', label: '最后登录时间', minWidth: 170 },
  { prop: 'createdTime', label: '创建时间', minWidth: 170 },
]

// ==================== 数据加载 ====================

/**
 * 加载用户列表
 */
async function loadUsers() {
  loading.value = true
  try {
    const params: UserQueryParams = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (searchForm.username?.trim()) params.username = searchForm.username.trim()
    if (searchForm.fullName?.trim()) params.fullName = searchForm.fullName.trim()
    if (searchForm.department?.trim()) params.department = searchForm.department.trim()
    if (searchForm.status !== '' && searchForm.status !== undefined) params.status = searchForm.status

    const result = await userApi.getList(params)
    users.value = result?.records || []
    total.value = result?.total || 0
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}

/**
 * 加载角色选项
 */
async function loadRoleOptions() {
  try {
    roleOptions.value = await roleApi.getActive() || []
  } catch {
    // 角色加载失败不阻塞主流程
  }
}

// ==================== 搜索操作 ====================

/**
 * 执行搜索
 */
function handleSearch() {
  pagination.page = 1
  loadUsers()
}

/**
 * 重置搜索条件
 */
function handleReset() {
  searchForm.username = ''
  searchForm.fullName = ''
  searchForm.department = ''
  searchForm.status = ''
  pagination.page = 1
  loadUsers()
}

/**
 * 刷新列表
 */
function handleRefresh() {
  loadUsers()
}

// ==================== 分页操作 ====================

/**
 * 翻页
 */
function handlePageChange(page: number) {
  pagination.page = page
  loadUsers()
}

/**
 * 每页条数变化
 */
function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  loadUsers()
}

/**
 * 多选变化
 */
function handleSelectionChange(selection: UserItem[]) {
  selectedRows.value = selection
}

// ==================== 新增/编辑操作 ====================

/**
 * 打开新增对话框
 */
function handleCreate() {
  dialogType.value = 'create'
  editingId.value = null
  Object.assign(formData, {
    username: '',
    password: '',
    fullName: '',
    phone: '',
    email: '',
    departmentId: '',
    position: '',
    roleIds: [],
    status: 1,
  })
  dialogVisible.value = true
}

/**
 * 打开编辑对话框
 */
function handleEdit(row: UserItem) {
  dialogType.value = 'edit'
  editingId.value = row.id
  Object.assign(formData, {
    username: row.username,
    password: '',
    fullName: row.fullName || '',
    phone: row.phone || '',
    email: row.email || '',
    departmentId: row.departmentId || '',
    position: '',
    roleIds: [],
    status: row.status ?? 1,
  })
  // 加载用户角色
  loadUserRoles(row.id)
  dialogVisible.value = true
}

/**
 * 加载用户角色
 */
async function loadUserRoles(userId: number) {
  try {
    const roles = await userApi.getUserRoles(userId)
    formData.roleIds = roles || []
  } catch {
    formData.roleIds = []
  }
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (dialogType.value === 'create') {
      await userApi.create({
        username: formData.username.trim(),
        password: formData.password,
        fullName: formData.fullName.trim(),
        email: formData.email || undefined,
        phone: formData.phone || undefined,
        status: formData.status,
        departmentId: formData.departmentId || undefined,
      })
      // 创建成功后分配角色
      if (formData.roleIds.length > 0) {
        // 角色分配需要用户ID，这里简化处理，实际项目中需要获取创建后的用户ID
      }
      ElMessage.success('用户创建成功')
    } else {
      if (editingId.value === null) return
      await userApi.update(editingId.value, {
        fullName: formData.fullName.trim(),
        email: formData.email || undefined,
        phone: formData.phone || undefined,
        status: formData.status,
        departmentId: formData.departmentId || undefined,
      })
      // 更新角色分配
      await userApi.assignRoles(editingId.value, formData.roleIds)
      ElMessage.success('用户更新成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

// ==================== 详情操作 ====================

/**
 * 查看详情
 */
function handleView(row: UserItem) {
  currentUser.value = row
  detailVisible.value = true
}

// ==================== 重置密码操作 ====================

/**
 * 打开重置密码对话框
 */
function handleResetPassword(row: UserItem) {
  resetPasswordUserId.value = row.id
  resetPasswordUserName.value = row.fullName || row.username
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordVisible.value = true
}

/**
 * 提交重置密码
 */
async function handleResetPasswordSubmit() {
  if (!resetPasswordFormRef.value) return

  try {
    await resetPasswordFormRef.value.validate()
  } catch {
    return
  }

  if (resetPasswordUserId.value === null) return

  resetPasswordSubmitting.value = true
  try {
    await userApi.resetPassword(resetPasswordUserId.value, resetPasswordForm.newPassword)
    ElMessage.success('密码重置成功')
    resetPasswordVisible.value = false
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    resetPasswordSubmitting.value = false
  }
}

// ==================== 启用/禁用操作 ====================

/**
 * 切换用户启用/禁用状态
 */
async function handleToggleStatus(row: UserItem) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户「${row.fullName || row.username}」吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const newStatus = row.status === 1 ? 0 : 1
    await userApi.toggleStatus(row.id, newStatus)
    ElMessage.success(`用户${action}成功`)
    loadUsers()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    // 请求错误已由拦截器统一提示
  }
}

// ==================== 删除操作 ====================

/**
 * 删除用户
 * TODO: 用户删除接口待后端实现（userApi 暂无 delete 方法）
 * 后端就绪后替换为真实 API 调用：await userApi.delete(row.id)
 */
async function handleDelete(row: UserItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.fullName || row.username}」吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
    // TODO: 用户删除接口待后端实现，禁止显示假成功
    // 后端就绪后替换为：await userApi.delete(row.id)
    ElMessage.warning('用户删除功能开发中，待后端接口实现后启用')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    // 请求错误已由拦截器统一提示
  }
}

/**
 * 批量删除
 * TODO: 批量删除接口待后端实现（userApi 暂无 batchDelete 方法）
 */
function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的用户')
    return
  }
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedRows.value.length} 个用户吗？此操作不可恢复。`,
    '确认批量删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    },
  ).then(() => {
    // TODO: 批量删除接口待后端实现，禁止显示假成功
    ElMessage.warning('批量删除功能开发中，待后端接口实现后启用')
  }).catch(() => {
    // 用户取消
  })
}

// ==================== 导入导出操作 ====================

/**
 * 批量导入
 */
function handleImport() {
  // TODO: 实现批量导入功能
  ElMessage.info('批量导入功能开发中')
}

/**
 * 批量导出
 */
function handleExport() {
  // TODO: 实现批量导出功能
  ElMessage.info('批量导出功能开发中')
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadUsers()
  loadRoleOptions()
})
</script>

<template>
  <div class="user-management-tab">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.username"
          placeholder="搜索用户账号/姓名"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-tree-select
          v-model="searchForm.department"
          :data="departmentTree"
          :props="{ label: 'name', value: 'id', children: 'children' }"
          placeholder="选择部门"
          clearable
          check-strictly
          style="width: 160px"
          :teleported="false"
        />

        <el-select
          v-model="searchForm.status"
          placeholder="状态筛选"
          clearable
          style="width: 120px"
          :teleported="false"
        >
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>

        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div class="toolbar-right">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
        <el-button @click="handleImport">
          <el-icon><Upload /></el-icon>
          批量导入
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          批量导出
        </el-button>
        <el-button
          v-if="selectedRows.length > 0"
          type="danger"
          @click="handleBatchDelete"
        >
          <el-icon><Delete /></el-icon>
          批量删除 ({{ selectedRows.length }})
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="users"
        :loading="loading"
        stripe
        :actions-width="300"
        @selection-change="handleSelectionChange"
      >
        <!-- 部门列 -->
        <template #department="{ row }">
          <StatusTag
            v-if="row.department"
            category="department"
            :label="row.department"
            size="small"
          />
          <span v-else class="text-muted">未分配</span>
        </template>

        <!-- 角色列 -->
        <template #roleNames="{ row }">
          <template v-if="parseRoleNames(row.roleNames).length > 0">
            <StatusTag
              v-for="name in parseRoleNames(row.roleNames).slice(0, 2)"
              :key="name"
              status="info"
              :label="name"
              size="small"
              class="role-tag"
            />
            <el-tooltip
              v-if="parseRoleNames(row.roleNames).length > 2"
              :content="parseRoleNames(row.roleNames).join('、')"
              placement="top"
            >
              <span class="more-roles">+{{ parseRoleNames(row.roleNames).length - 2 }}</span>
            </el-tooltip>
          </template>
          <span v-else class="text-muted">未分配</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusType(row.status)" size="small" />
        </template>

        <!-- 操作列 -->
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
          <el-button link type="warning" size="small" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增用户' : '编辑用户'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="dialogType === 'create' ? formRules : { fullName: formRules.fullName, email: formRules.email, phone: formRules.phone }"
        label-width="100px"
        label-position="right"
      >
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="formData.username"
                placeholder="请输入用户名"
                maxlength="50"
                show-word-limit
                :disabled="dialogType === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password" v-if="dialogType === 'create'">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="请输入密码"
                show-password
                maxlength="100"
              />
            </el-form-item>
            <el-form-item label="用户姓名" prop="fullName" v-else>
              <el-input
                v-model="formData.fullName"
                placeholder="请输入用户姓名"
                maxlength="30"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="dialogType === 'create'">
          <el-col :span="12">
            <el-form-item label="用户姓名" prop="fullName">
              <el-input
                v-model="formData.fullName"
                placeholder="请输入用户姓名"
                maxlength="30"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="formData.phone"
                placeholder="请输入手机号"
                maxlength="11"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="formData.email"
                placeholder="请输入邮箱"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">组织信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="formData.departmentId"
                :data="departmentTree"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择所属部门"
                clearable
                check-strictly
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input
                v-model="formData.position"
                placeholder="请输入职位"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">角色分配</el-divider>
        <el-form-item label="选择角色">
          <el-checkbox-group v-model="formData.roleIds" class="role-checkbox-group">
            <el-checkbox
              v-for="role in roleOptions"
              :key="role.id"
              :label="String(role.id)"
              class="role-checkbox-item"
            >
              <span class="role-name">{{ role.roleName }}</span>
              <span class="role-code">{{ role.roleCode }}</span>
            </el-checkbox>
          </el-checkbox-group>
          <el-empty
            v-if="roleOptions.length === 0"
            description="暂无可用角色"
            :image-size="60"
          />
        </el-form-item>

        <el-divider content-position="left">状态设置</el-divider>
        <el-form-item label="用户状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogType === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="resetPasswordVisible"
      title="重置密码"
      width="440px"
      :close-on-click-modal="false"
    >
      <div class="reset-password-hint">
        正在为用户 <strong>{{ resetPasswordUserName }}</strong> 重置密码
      </div>
      <el-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            maxlength="100"
            @keyup.enter="handleResetPasswordSubmit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPasswordSubmitting" @click="handleResetPasswordSubmit">
          确认重置
        </el-button>
      </template>
    </el-dialog>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="用户详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="currentUser" class="user-detail">
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户账号">{{ currentUser.username }}</el-descriptions-item>
            <el-descriptions-item label="用户姓名">{{ currentUser.fullName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ currentUser.department || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getStatusType(currentUser.status)" size="small" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="section-title">角色信息</div>
          <div class="role-list">
            <StatusTag
              v-for="name in parseRoleNames(currentUser.roleNames)"
              :key="name"
              status="info"
              :label="name"
              size="small"
              class="role-tag"
            />
            <span v-if="parseRoleNames(currentUser.roleNames).length === 0" class="text-muted">未分配角色</span>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">时间信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="最后登录时间">{{ currentUser.lastLoginTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最后登录IP">{{ currentUser.lastLoginIp || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ currentUser.createdTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ currentUser.updatedTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEdit(currentUser!); detailVisible = false">编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.user-management-tab {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

/* 顶部工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

/* 表格区域 */
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

/* 辅助文本 */
.text-muted {
  color: var(--fts-text-quaternary);
  font-size: var(--fts-font-size-sm);
}

/* 角色标签 */
.role-tag {
  margin-right: var(--fts-space-1);
  margin-bottom: var(--fts-space-1);
}

.more-roles {
  display: inline-block;
  padding: 2px 8px;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-sm);
}

/* 角色复选框组 */
.role-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  width: 100%;
}

.role-checkbox-item {
  display: flex;
  align-items: center;
  height: auto;
  padding: var(--fts-space-2) var(--fts-space-3);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-sm);
  margin: 0;

  .role-name {
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  .role-code {
    margin-left: var(--fts-space-2);
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    font-family: monospace;
  }
}

/* 重置密码提示 */
.reset-password-hint {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3);
  background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);

  strong {
    color: var(--fts-primary);
  }
}

/* 用户详情 */
.user-detail {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
  }
}

.role-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-1);
  padding: var(--fts-space-3);
  background: var(--fts-bg-primary);
  border-radius: var(--fts-radius-sm);
  border: 1px solid var(--fts-border-primary);
}
</style>
