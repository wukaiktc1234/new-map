<script setup lang="ts">
/**
 * 物资分类管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理采购物资的分类体系，支持分类树浏览、列表展示、新增/编辑/启用停用/删除
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 页面结构：
 * - PageHeader：页面标题 + 新增按钮
 * - 统计卡片：分类总数、一级分类、二级分类、三级分类
 * - 主体：左侧分类树 + 右侧列表（工具栏+表格+分页）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, FolderOpened, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, TreeNode } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { materialCategoryApi } from '@/api/purchase'
import type {
  MaterialCategoryInfo,
  MaterialCategoryFormData,
  MaterialCategoryQueryForm,
  MaterialCategoryStatus,
} from '@/types/purchase-category'
import { MaterialCategoryStatusOptions } from '@/types/purchase-category'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
  align?: string
}

/** 树形节点数据 */
interface CategoryTreeNode {
  categoryId: string
  categoryName: string
  categoryCode: string
  parentId: string
  children?: CategoryTreeNode[]
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<MaterialCategoryInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const treeRef = ref<InstanceType<typeof ElTree>>()
const formRef = ref<FormInstance>()

/** 当前选中的左侧树节点ID（空字符串表示全部） */
const selectedTreeId = ref<string>('')

// 查询表单
const queryForm = ref<MaterialCategoryQueryForm & { parentId?: string }>({
  keyword: '',
  status: null,
  parentId: '',
})

// 分页和加载状态
const loading = ref(false)
const tableData = ref<MaterialCategoryInfo[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

// 全部分类列表（用于构建树）
const allCategoryList = ref<MaterialCategoryInfo[]>([])

// 表单数据
const formData = reactive<Partial<MaterialCategoryFormData> & { status: MaterialCategoryStatus }>({
  categoryName: '',
  categoryCode: '',
  parentId: '',
  sortOrder: 0,
  status: 'active',
  remark: '',
})

// ==================== 计算属性 ====================

/**
 * 统计数据
 * 统计分类总数、各级分类数量
 */
const statistics = computed(() => {
  const list = allCategoryList.value
  const total = list.length
  const level1 = list.filter(c => !c.parentId).length
  const level2 = list.filter(c => {
    const parent = list.find(p => p.categoryId === c.parentId)
    return parent && !parent.parentId
  }).length
  const level3 = list.filter(c => {
    const parent = list.find(p => p.categoryId === c.parentId)
    if (!parent || !parent.parentId) return false
    const grandparent = list.find(p => p.categoryId === parent.parentId)
    return grandparent && !grandparent.parentId
  }).length
  return { total, level1, level2, level3 }
})

/**
 * 左侧分类树数据（扁平列表转树形结构）
 */
const categoryTreeData = computed<CategoryTreeNode[]>(() => {
  const list = allCategoryList.value
  const map = new Map<string, CategoryTreeNode>()
  const roots: CategoryTreeNode[] = []

  list.forEach(item => {
    map.set(item.categoryId, {
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      categoryCode: item.categoryCode,
      parentId: item.parentId,
      children: [],
    })
  })

  list.forEach(item => {
    const node = map.get(item.categoryId)!
    if (item.parentId && map.has(item.parentId)) {
      const parent = map.get(item.parentId)!
      if (!parent.children) parent.children = []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
})

/**
 * 上级分类树选项（用于对话框中的上级分类选择）
 * 编辑时排除自身及其子分类
 */
const parentTreeOptions = computed<CategoryTreeNode[]>(() => {
  if (!isEdit.value || !formData.categoryId) {
    return categoryTreeData.value
  }
  const excludeIds = new Set<string>()
  const collectChildren = (nodes: CategoryTreeNode[]) => {
    nodes.forEach(node => {
      excludeIds.add(node.categoryId)
      if (node.children) collectChildren(node.children)
    })
  }
  const currentNode = findNodeById(categoryTreeData.value, formData.categoryId)
  if (currentNode) {
    excludeIds.add(currentNode.categoryId)
    if (currentNode.children) collectChildren(currentNode.children)
  }
  const filterTree = (nodes: CategoryTreeNode[]): CategoryTreeNode[] => {
    return nodes
      .filter(n => !excludeIds.has(n.categoryId))
      .map(n => ({
        ...n,
        children: n.children ? filterTree(n.children) : [],
      }))
  }
  return filterTree(categoryTreeData.value)
})

/**
 * 表格列定义
 */
const columns = computed<ColumnDef[]>(() => [
  { prop: 'categoryCode', label: '分类编码', minWidth: 120 },
  { prop: 'categoryName', label: '分类名称', minWidth: 140 },
  { prop: 'level', label: '分类级别', minWidth: 90, align: 'center', slot: 'level' },
  { prop: 'parentName', label: '上级分类', minWidth: 120, slot: 'parentName' },
  { prop: 'sortOrder', label: '排序', minWidth: 70, align: 'center' },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

/**
 * 在树中查找指定ID的节点
 */
function findNodeById(nodes: CategoryTreeNode[], id: string): CategoryTreeNode | null {
  for (const node of nodes) {
    if (node.categoryId === id) return node
    if (node.children) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

/**
 * 计算分类的层级（1=顶级）
 */
function getCategoryLevel(categoryId: string): number {
  let level = 1
  let current = allCategoryList.value.find(c => c.categoryId === categoryId)
  while (current && current.parentId) {
    level++
    current = allCategoryList.value.find(c => c.categoryId === current!.parentId)
  }
  return level
}

/**
 * 获取层级标签文本
 */
function getLevelLabel(level: number): string {
  const labels: Record<number, string> = { 1: '一级', 2: '二级', 3: '三级', 4: '四级', 5: '五级' }
  return labels[level] || `${level}级`
}

/**
 * 加载表格数据
 */
async function loadTableData(): Promise<void> {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: queryForm.value.keyword || undefined,
      status: queryForm.value.status || undefined,
      parentId: queryForm.value.parentId || undefined,
    }
    const res = await materialCategoryApi.getList(params as MaterialCategoryQueryForm & { page?: number; size?: number })
    tableData.value = res.records
    pagination.total = res.total
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载数据失败')
    }
  } finally {
    loading.value = false
  }
}

/**
 * 加载全部分类列表（用于构建树）
 */
async function loadAllCategories(): Promise<void> {
  try {
    allCategoryList.value = await materialCategoryApi.getAllList()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载分类树失败')
    }
  }
}

/**
 * 刷新数据
 */
async function refresh(): Promise<void> {
  await Promise.all([loadTableData(), loadAllCategories()])
}

/**
 * 搜索
 */
function handleSearch(): void {
  pagination.current = 1
  loadTableData()
}

/**
 * 重置搜索
 */
function handleReset(): void {
  queryForm.value.keyword = ''
  queryForm.value.status = null
  queryForm.value.parentId = ''
  selectedTreeId.value = ''
  treeRef.value?.setCurrentKey(null)
  pagination.current = 1
  loadTableData()
}

/**
 * 左侧树节点点击
 */
function handleTreeNodeClick(data: CategoryTreeNode): void {
  selectedTreeId.value = data.categoryId
  queryForm.value.parentId = data.categoryId
  pagination.current = 1
  loadTableData()
}

/**
 * 表格选中变化
 */
function handleSelectionChange(rows: MaterialCategoryInfo[]): void {
  selectedRows.value = rows
}

/**
 * 打开新增对话框
 */
function handleCreate(): void {
  isEdit.value = false
  Object.assign(formData, {
    categoryName: '',
    categoryCode: '',
    parentId: selectedTreeId.value || '',
    sortOrder: 0,
    status: 'active' as MaterialCategoryStatus,
    remark: '',
  })
  dialogVisible.value = true
}

/**
 * 打开编辑对话框
 */
async function handleEdit(row: MaterialCategoryInfo): Promise<void> {
  isEdit.value = true
  try {
    const detail = await materialCategoryApi.getById(row.categoryId)
    if (detail) {
      Object.assign(formData, {
        ...detail,
        status: detail.status,
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载分类详情失败')
  }
}

/**
 * 表单校验规则
 */
const formRules: FormRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  categoryCode: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { min: 2, max: 30, message: '长度在2到30个字符之间', trigger: 'blur' },
  ],
}

/**
 * 提交表单
 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: MaterialCategoryFormData = {
      categoryName: formData.categoryName!,
      categoryCode: formData.categoryCode!,
      parentId: formData.parentId || undefined,
      sortOrder: formData.sortOrder,
      remark: formData.remark,
    }

    if (isEdit.value && formData.categoryId) {
      await materialCategoryApi.update(formData.categoryId, submitData)
      if (formData.status !== undefined) {
        await materialCategoryApi.updateStatus(formData.categoryId, formData.status)
      }
      ElMessage.success('更新成功')
    } else {
      await materialCategoryApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await refresh()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
    } else {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  } finally {
    submitLoading.value = false
  }
}

/**
 * 切换启用/停用状态
 */
async function handleStatusToggle(row: MaterialCategoryInfo): Promise<void> {
  const isActive = row.status === 'active'
  const text = isActive ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${text}分类「${row.categoryName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newStatus: MaterialCategoryStatus = isActive ? 'inactive' : 'active'
    await materialCategoryApi.updateStatus(row.categoryId, newStatus)
    ElMessage.success(`${text}成功`)
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/**
 * 删除分类
 */
async function handleDelete(row: MaterialCategoryInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${row.categoryName}」吗？删除后不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await materialCategoryApi.delete(row.categoryId)
    ElMessage.success('删除成功')
    if (selectedTreeId.value === row.categoryId) {
      selectedTreeId.value = ''
      queryForm.value.parentId = ''
    }
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 批量导入
 */
async function handleImport(): Promise<void> {
  ElMessage.info('批量导入功能开发中')
}

/**
 * 批量导出
 */
async function handleExport(): Promise<void> {
  ElMessage.info('批量导出功能开发中')
}

/**
 * 分页变化
 */
function handlePageChange(page: number): void {
  pagination.current = page
  loadTableData()
}

function handleSizeChange(size: number): void {
  pagination.pageSize = size
  pagination.current = 1
  loadTableData()
}

/**
 * 状态标签映射
 */
function getStatusColor(status: string): string {
  return status === 'active' ? 'active' : 'inactive'
}

function getStatusLabel(status: string): string {
  return status === 'active' ? '启用' : '停用'
}

// ==================== 生命周期 ====================

onMounted(() => {
  refresh()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="商品分类" description="管理采购物资的分类体系">
      <el-button v-permission="'purchase:material-category:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增分类
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="FolderOpened" label="分类总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Document" label="一级分类" :value="String(statistics.level1)" color-type="success" variant="bordered" />
      <StatCard icon="Files" label="二级分类" :value="String(statistics.level2)" color-type="warning" variant="bordered" />
      <StatCard icon="Collection" label="三级分类" :value="String(statistics.level3)" color-type="info" variant="bordered" />
    </section>

    <!-- 主体区域：左侧树 + 右侧列表 -->
    <div class="main-container">
      <!-- 左侧分类树 -->
      <aside class="tree-sidebar">
        <div class="tree-header">
          <span class="tree-title">分类树</span>
          <el-button link type="primary" size="small" @click="handleReset">全部</el-button>
        </div>
        <div class="tree-wrapper">
          <el-tree
            ref="treeRef"
            :data="categoryTreeData"
            :props="{ label: 'categoryName', children: 'children', id: 'categoryId' }"
            node-key="categoryId"
            default-expand-all
            highlight-current
            @node-click="handleTreeNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node-label">
                <el-icon class="tree-node-icon"><FolderOpened /></el-icon>
                {{ data.categoryName }}
              </span>
            </template>
          </el-tree>
        </div>
      </aside>

      <!-- 右侧列表区域 -->
      <div class="right-content">
        <!-- 工具栏面板 -->
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-input
                v-model="queryForm.keyword"
                placeholder="搜索分类名称或编码..."
                clearable
                style="width: 220px"
                size="default"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-select
                v-model="queryForm.status"
                placeholder="状态"
                clearable
                style="width: 110px"
                size="default"
                @change="handleSearch"
              >
                <el-option
                  v-for="opt in MaterialCategoryStatusOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
              <el-button size="default" @click="handleReset">
                <el-icon :size="14"><Refresh /></el-icon>重置
              </el-button>
              <el-button size="default" class="action-btn--import" @click="handleImport">
                <el-icon :size="14"><Upload /></el-icon>导入
              </el-button>
              <el-button size="default" class="action-btn--export" @click="handleExport">
                <el-icon :size="14"><Download /></el-icon>导出
              </el-button>
            </div>
          </div>
        </div>

        <!-- 数据表格区域 -->
        <section class="table-section">
          <DataTable
            :data="tableData"
            :columns="columns"
            :loading="loading"
            :selectable="true"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            @selection-change="handleSelectionChange"
          >
            <!-- 分类级别列 -->
            <template #level="{ row }">
              <span class="level-tag">
                {{ getLevelLabel(getCategoryLevel(row.categoryId)) }}
              </span>
            </template>

            <!-- 上级分类列 -->
            <template #parentName="{ row }">
              <span class="parent-text">{{ row.parentName || '顶级分类' }}</span>
            </template>

            <!-- 状态列 -->
            <template #status="{ row }">
              <StatusTag :status="getStatusColor(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
            </template>

            <!-- 操作列 -->
            <template #operation="{ row }">
              <div class="action-text">
                <el-button v-permission="'purchase:material-category:edit'" link type="primary" size="default" @click.stop="handleEdit(row)">
                  编辑
                </el-button>
                <el-button
                  v-permission="'purchase:material-category:edit'"
                  link
                  :type="row.status === 'active' ? 'warning' : 'primary'"
                  size="default"
                  @click.stop="handleStatusToggle(row)"
                >
                  {{ row.status === 'active' ? '停用' : '启用' }}
                </el-button>
                <el-button v-permission="'purchase:material-category:delete'" link type="danger" size="default" @click.stop="handleDelete(row)">
                  删除
                </el-button>
              </div>
            </template>
          </DataTable>

          <!-- 分页组件 -->
          <div class="pagination-wrapper">
            <el-pagination
              :current-page="pagination.current"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </section>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="formData.categoryName" placeholder="请输入分类名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码" prop="categoryCode">
              <el-input v-model="formData.categoryCode" placeholder="如：VGE、MEAT" :disabled="isEdit" maxlength="30" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="上级分类">
              <el-tree-select
                v-model="formData.parentId"
                :data="parentTreeOptions"
                :props="{ label: 'categoryName', children: 'children', value: 'categoryId' }"
                placeholder="无（顶级分类）"
                clearable
                check-strictly
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="formData.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="formData.status">
                <el-radio value="active">启用</el-radio>
                <el-radio value="inactive">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

// 主体容器（左侧树 + 右侧列表）
.main-container {
  display: flex;
  gap: var(--fts-space-4);
  align-items: flex-start;
}

// 左侧分类树边栏
.tree-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;

  @media (max-width: 992px) {
    display: none;
  }
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-primary);
}

.tree-title {
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
}

.tree-wrapper {
  padding: var(--fts-space-2);
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.tree-node-label {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

.tree-node-icon {
  color: var(--fts-primary);
}

// 右侧内容区
.right-content {
  flex: 1;
  min-width: 0;
}

// 工具栏面板
.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 48px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;

  .action-btn--import,
  .action-btn--export {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
}

// 表格区域
.table-section {
  padding: 0 var(--fts-space-6) var(--fts-space-6);
  overflow-x: auto;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);

  :deep(.el-table) {
    width: 100%;
  }

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 分类级别标签
.level-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: var(--fts-font-size-xs);
  border-radius: var(--fts-card-radius);
  background: var(--fts-primary-light-9);
  color: var(--fts-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 上级分类文本
.parent-text {
  color: var(--fts-text-secondary);
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}

@media (max-width: 576px) {
  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .table-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
