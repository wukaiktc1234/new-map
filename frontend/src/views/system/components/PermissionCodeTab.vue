<script setup lang="ts">
/**
 * 权限码管理 Tab 组件
 * 对应后端: /v1/permissions (PermissionController)
 *
 * 功能：
 * - 左侧权限树（菜单/权限树）+ 右侧列表布局
 * - 顶部工具栏：权限名称搜索、权限类型筛选、新增权限按钮
 * - 数据表格：权限编码、权限名称、权限类型、所属模块、排序、状态、操作
 * - 新增/编辑权限对话框：上级权限（树选择）、权限编码、权限名称、权限类型、路由路径/组件路径、图标、排序、状态
 * - 支持启用/禁用权限
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import {
  permissionApi,
  PERMISSION_TYPE_OPTIONS,
  PERMISSION_STATUS_OPTIONS,
  permissionStatusToTagStatus,
  permissionTypeToLabel,
  permissionTypeToTagStatus,
  type PermissionVO,
  type PermissionCreateDTO,
  type PermissionUpdateDTO,
  type PermissionTreeNodeVO,
  type PermissionQueryParams,
} from '@/api/system/permission'

// ========== 列表状态 ==========
const loading = ref(false)
const permissionList = ref<PermissionVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// ========== 左侧权限树 ==========
const treeLoading = ref(false)
const treeData = ref<PermissionTreeNodeVO[]>([])
const selectedNodeId = ref<string>('')

// ========== 搜索条件 ==========
const searchForm = reactive<PermissionQueryParams>({
  permissionName: '',
  permissionType: '',
  status: '',
})

// ========== 对话框 ==========
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const editingId = ref<string>('')
const parentTreeOptions = ref<PermissionTreeNodeVO[]>([])

/** 树形配置 */
const treeProps = { label: 'permissionName', children: 'children' }

/** 获取默认表单数据 */
function getDefaultForm(): PermissionCreateDTO {
  return {
    permissionCode: '',
    permissionName: '',
    permissionType: 1,
    module: '',
    parentId: selectedNodeId.value || null,
    sortOrder: 0,
    status: 1,
    path: '',
    component: '',
    icon: '',
  }
}

const form = reactive<PermissionCreateDTO>(getDefaultForm())

// ========== 列定义 ==========
const columns: DataTableColumn[] = [
  { prop: 'permissionCode', label: '权限编码', minWidth: 200, fixed: 'left' as const },
  { prop: 'permissionName', label: '权限名称', minWidth: 160 },
  { prop: 'permissionType', label: '权限类型', minWidth: 100, align: 'center', slot: 'type' as const },
  { prop: 'module', label: '所属模块', minWidth: 120 },
  { prop: 'sortOrder', label: '排序', minWidth: 80, align: 'center' },
  { prop: 'status', label: '状态', minWidth: 90, align: 'center', slot: 'status' as const },
]

// ========== 加载数据 ==========
/** 加载权限列表（条件分页查询） */
async function loadList(): Promise<void> {
  loading.value = true
  try {
    const params: PermissionQueryParams = {
      page: currentPage.value,
      size: pageSize.value,
      permissionName: searchForm.permissionName || undefined,
      permissionType: searchForm.permissionType || undefined,
      status: searchForm.status || undefined,
    }
    if (selectedNodeId.value) {
      params.module = selectedNodeId.value
    }
    const res = await permissionApi.search(params)
    permissionList.value = res.records || []
    total.value = res.total || 0
  } catch {
    permissionList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 加载权限树（左侧菜单/权限树） */
async function loadTree(): Promise<void> {
  treeLoading.value = true
  try {
    treeData.value = await permissionApi.getTreeNodes() || []
  } catch {
    treeData.value = []
  } finally {
    treeLoading.value = false
  }
}

/** 加载父级权限树选项（用于对话框） */
async function loadParentTreeOptions(): Promise<void> {
  try {
    parentTreeOptions.value = await permissionApi.getTreeNodes() || []
  } catch {
    parentTreeOptions.value = []
  }
}

// ========== 搜索/分页 ==========
function handleSearch(): void {
  currentPage.value = 1
  loadList()
}

function handleReset(): void {
  searchForm.permissionName = ''
  searchForm.permissionType = ''
  searchForm.status = ''
  selectedNodeId.value = ''
  currentPage.value = 1
  loadList()
}

function handlePageChange(page: number): void {
  currentPage.value = page
  loadList()
}

function handleSizeChange(size: number): void {
  pageSize.value = size
  currentPage.value = 1
  loadList()
}

// ========== 左侧树节点点击 ==========
function handleNodeClick(data: PermissionTreeNodeVO): void {
  selectedNodeId.value = data.id
  currentPage.value = 1
  loadList()
}

// ========== 新增/编辑 ==========
function handleCreate(): void {
  dialogType.value = 'create'
  Object.assign(form, getDefaultForm())
  editingId.value = ''
  loadParentTreeOptions()
  dialogVisible.value = true
}

function handleDetail(row: PermissionVO): void {
  ElMessage.info(`查看权限详情：${row.permissionName}`)
}

function handleEdit(row: PermissionVO): void {
  dialogType.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    permissionCode: row.permissionCode,
    permissionName: row.permissionName,
    permissionType: row.permissionType,
    module: row.module || '',
    parentId: row.parentId,
    sortOrder: row.sortOrder ?? 0,
    status: row.status,
    path: row.path || '',
    component: row.component || '',
    icon: row.icon || '',
  })
  loadParentTreeOptions()
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  if (!form.permissionCode.trim() || !form.permissionName.trim()) {
    ElMessage.warning('请填写完整的权限编码和名称')
    return
  }
  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await permissionApi.create(form)
      ElMessage.success('权限创建成功')
    } else {
      const updateData: PermissionUpdateDTO = { ...form, id: editingId.value }
      await permissionApi.update(editingId.value, updateData)
      ElMessage.success('权限更新成功')
    }
    dialogVisible.value = false
    loadList()
    loadTree()
  } catch {
    // 错误已由响应拦截器统一提示
  } finally {
    submitLoading.value = false
  }
}

// ========== 状态切换 / 删除 ==========
async function handleToggleStatus(row: PermissionVO): Promise<void> {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}权限「${row.permissionName}」吗？`,
      '提示',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
    await permissionApi.updateStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadList()
    loadTree()
  } catch {
    // 用户取消或请求失败
  }
}

async function handleDelete(row: PermissionVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除权限「${row.permissionName}」吗？此操作不可恢复。`,
      '危险操作',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await permissionApi.delete(row.id)
    ElMessage.success('删除成功')
    loadList()
    loadTree()
  } catch {
    // 用户取消或请求失败
  }
}

// ========== 初始化 ==========
onMounted(() => {
  loadTree()
  loadList()
})
</script>

<template>
  <div class="permission-code-tab">
    <!-- 主体布局：左侧树 + 右侧列表 -->
    <div class="main-layout">
      <!-- 左侧权限树 -->
      <div class="tree-panel">
        <div class="tree-panel__header">
          <span class="tree-panel__title">权限树</span>
          <el-button
            link
            type="primary"
            size="small"
            @click="loadTree"
          >
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        <div class="tree-panel__content" v-loading="treeLoading">
          <el-empty v-if="treeData.length === 0 && !treeLoading" description="暂无权限数据" :image-size="80" />
          <el-tree
            v-else
            :data="treeData"
            :props="treeProps"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            :highlight-current="true"
            @node-click="handleNodeClick"
          >
            <template #default="{ data: node }">
              <div class="tree-node">
                <span class="tree-node__name">{{ node.permissionName }}</span>
                <StatusTag
                  v-if="node.permissionType"
                  :status="permissionTypeToTagStatus(node.permissionType)"
                  :label="permissionTypeToLabel(node.permissionType)"
                  size="small"
                />
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧列表区域 -->
      <div class="table-panel">
        <!-- 顶部工具栏 -->
        <div class="toolbar">
          <div class="toolbar__left">
            <el-input
              v-model="searchForm.permissionName"
              placeholder="搜索权限名称"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select
              v-model="searchForm.permissionType"
              placeholder="权限类型"
              clearable
              :teleported="false"
              style="width: 130px"
              @change="handleSearch"
            >
              <el-option
                v-for="opt in PERMISSION_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="String(opt.value)"
              />
            </el-select>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
          <div class="toolbar__right">
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新增权限
            </el-button>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="table-section">
          <DataTable
            :columns="columns"
            :data="permissionList"
            :loading="loading"
            :selectable="false"
            stripe
            :actions-width="220"
          >
            <template #type="{ row }">
              <StatusTag
                :status="permissionTypeToTagStatus(row.permissionType)"
                :label="permissionTypeToLabel(row.permissionType)"
                size="small"
              />
            </template>
            <template #status="{ row }">
              <StatusTag :status="permissionStatusToTagStatus(row.status)" size="small" />
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                link
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </DataTable>

          <!-- 分页 -->
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑权限对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增权限' : '编辑权限'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" label-position="right">
        <el-form-item label="上级权限">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTreeOptions"
            :props="treeProps"
            node-key="id"
            check-strictly
            clearable
            :render-after-expand="false"
            :teleported="false"
            placeholder="留空为顶级权限"
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="权限编码" required>
              <el-input v-model="form.permissionCode" placeholder="请输入权限编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限名称" required>
              <el-input v-model="form.permissionName" placeholder="请输入权限名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="权限类型">
              <el-select v-model="form.permissionType" :teleported="false" style="width: 100%">
                <el-option
                  v-for="opt in PERMISSION_TYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属模块">
              <el-input v-model="form.module" placeholder="如：system" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="路由路径">
              <el-input v-model="form.path" placeholder="前端路由路径" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="组件路径">
              <el-input v-model="form.component" placeholder="前端组件路径" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="图标">
              <el-input v-model="form.icon" placeholder="图标名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ dialogType === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.permission-code-tab {
  width: 100%;
  height: 100%;
}

// ========== 主体布局：左侧树 + 右侧列表 ==========
.main-layout {
  display: flex;
  gap: var(--fts-space-4);
  height: 100%;
}

// ========== 左侧树面板 ==========
.tree-panel {
  width: 280px;
  flex-shrink: 0;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--fts-space-3) var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-light);
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  &__content {
    flex: 1;
    overflow-y: auto;
    padding: var(--fts-space-2);
  }
}

// ========== 树节点样式 ==========
.tree-node {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex: 1;
  padding-right: var(--fts-space-2);

  &__name {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-primary);
  }
}

// ========== 右侧列表面板 ==========
.table-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

// ========== 工具栏 ==========
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
  flex-wrap: wrap;
  gap: var(--fts-space-3);

  &__left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-wrap: wrap;
  }

  &__right {
    display: flex;
    gap: var(--fts-space-2);
  }
}

// ========== 表格区域 ==========
.table-section {
  flex: 1;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

// ========== 分页栏 ==========
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-3) var(--fts-space-2) 0;
}
</style>
