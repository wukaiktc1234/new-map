<template>
  <div class="modern-page">
    <PageHeader
      title="菜品分类"
      description="管理菜品分类层级结构，支持多级分类"
      icon-name="Folder"
    >
      <el-button type="primary" size="default" v-permission="'product:category:create'" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增分类
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard icon="FolderOpened" label="分类总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Document" label="一级分类" :value="String(statistics.level1)" color-type="success" variant="bordered" />
      <StatCard icon="Files" label="子分类" :value="String(statistics.children)" color-type="warning" variant="bordered" />
    </section>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="请输入分类名称"
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        row-key="categoryId"
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <template #categoryName="{ row }">
          <span class="category-name-text">{{ row.categoryName }}</span>
        </template>

        <template #status="{ row }">
          <StatusTag :status="row.categoryStatus === 1 || row.categoryStatus === 'active' ? 'active' : 'inactive'" :label="row.categoryStatus === 1 || row.categoryStatus === 'active' ? '启用' : '停用'" size="small" />
        </template>

        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="small" @click.stop="handleAddChild(row)">
              添加子类
            </el-button>
            <el-button link type="primary" size="small" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              link
              :type="row.categoryStatus === 1 || row.categoryStatus === 'active' ? 'warning' : 'primary'"
              size="small"
              @click.stop="handleStatusToggle(row)"
            >
              {{ (row.categoryStatus === 1 || row.categoryStatus === 'active') ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDeleteClick(row)">
              删除
            </el-button>
          </div>
        </template>
      </DataTable>
    </section>
  </div>

  <CategoryDialog ref="dialogRef" :category-tree="tableData" @submit="handleSubmit" />
</template>

<script setup lang="ts">
defineOptions({
  name: 'CategoryManagement'
})

import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import CategoryDialog from './components/CategoryDialog.vue'
import { useLayoutStore } from '@/stores/layout'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { categoryApi } from '@/api/product/category'
import type { FoodCategory, CategoryFormData, CategoryQueryForm, ProductStatus } from '@/types/product'

const layoutStore = useLayoutStore()
const dialogRef = ref<InstanceType<typeof CategoryDialog>>()
const queryForm = ref<CategoryQueryForm>({ keyword: '' })

const { tableData, loading, refresh, handleDelete } = useCrudTable<FoodCategory, CategoryQueryForm>({
  api: categoryApi as unknown as CrudApi<FoodCategory, CategoryQueryForm>,
  queryForm,
  autoLoad: true,
})

async function handleDeleteClick(row: FoodCategory): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除分类「${row.categoryName}」吗？删除后其下所有子分类也将被删除！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    // 直接调用 API 并 refresh，避免 useCrudTable.handleDelete 的二次确认弹窗
    await categoryApi.delete(Number(row.categoryId))
    ElMessage.success('删除成功')
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function handleStatusToggle(row: FoodCategory): Promise<void> {
  const isActive = String(row.categoryStatus) === '1' || row.categoryStatus === 'active'
  const text = isActive ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${text}分类「${row.categoryName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const newStatus: ProductStatus = isActive ? 'inactive' : 'active'
    // 直接调用 API，由后端处理状态变更；不本地修改 tableData，避免与后端数据不一致
    await categoryApi.updateStatus(Number(row.categoryId), newStatus)
    ElMessage.success(`${text}成功`)
    // 调用 refresh() 重新拉取数据，保证 UI 与后端数据一致
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

interface ColumnDef {
  prop: string; label: string; width?: number | string; minWidth?: number | string
  fixed?: 'left' | 'right'; slot?: string; ellipsis?: boolean
}

const columns: ColumnDef[] = [
  { prop: 'categoryName', label: '分类名称', minWidth: 100, slot: 'categoryName' },
  { prop: 'sortOrder', label: '排序', minWidth: 65 },
  { prop: 'foodCount', label: '菜品数', minWidth: 75 },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status' },
  { prop: '_operation', label: '操作', width: 250, fixed: 'right', slot: 'operation' },
]

const statistics = computed(() => ({
  total: countCategories(tableData.value),
  level1: tableData.value.length,
  children: countChildren(tableData.value),
}))

function countCategories(categories: FoodCategory[]): number {
  let count = 0
  for (const cat of categories) {
    count++
    if (cat.children?.length) count += countCategories(cat.children)
  }
  return count
}

function countChildren(categories: FoodCategory[]): number {
  let count = 0
  for (const cat of categories) {
    if (cat.children?.length) count += cat.children.length + countChildren(cat.children)
  }
  return count
}

function handleSearch(): void { refresh() }
function handleReset(): void { queryForm.value.keyword = ''; refresh() }
function handleCreate(): void { dialogRef.value?.openCreate() }
function handleAddChild(parent: FoodCategory): void { dialogRef.value?.openCreateChild(parent) }
function handleEdit(row: FoodCategory): void { dialogRef.value?.openEdit(row) }

/**
 * 处理 Dialog 提交：根据是否携带 categoryId 判断新增/编辑模式，
 * 调用对应的后端 API，成功后关闭弹窗并刷新表格。
 */
async function handleSubmit(data: CategoryFormData): Promise<void> {
  try {
    if (data.categoryId) {
      // 编辑模式
      await categoryApi.update(Number(data.categoryId), data)
      ElMessage.success('更新成功')
    } else {
      // 新增模式（含子分类，parentId 已在 data 中）
      await categoryApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogRef.value?.close()
    await refresh()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '保存失败')
    } else {
      ElMessage.error('保存失败')
    }
    // 失败时保持弹窗打开，让用户可以修改后重试
    dialogRef.value?.setLoading(false)
  }
}
</script>

<style scoped lang="scss">

.stats-section {
  grid-template-columns: repeat(3, 1fr);

  @media (max-width: 992px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

.action-text {
  display: flex;
  gap: var(--fts-space-2);
}

.category-name-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}
</style>