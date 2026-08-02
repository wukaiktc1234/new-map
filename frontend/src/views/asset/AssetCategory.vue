<script setup lang="ts">
/**
 * 资产分类页面
 * 功能：分类树/列表展示、新增/编辑/删除、树形/平铺切换
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { categoryApi } from '@/api/asset'
import type { AssetCategory } from '@/types/asset'
import { fenToYuan as fenToYuanFromUtils } from '@/utils/money'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 数据状态 ===== */
const tableData = ref<AssetCategory[]>([])
const categoryList = ref<AssetCategory[]>([])
const searchForm = ref({ keyword: '' })
const isTreeView = ref(true)

/* ===== 对话框状态 ===== */
const formDialogVisible = ref(false)
const editingId = ref<string | null>(null)

/* ===== 表单数据 ===== */
const categoryForm = ref({
  name: '',
  code: '',
  parentId: null as string | null,
  usefulLifeMonthsOverride: undefined as number | undefined,
  salvageRateOverride: undefined as number | undefined,
  sortOrder: 0,
})
const formRef = ref()
const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
}

/* ===== 表格列 ===== */
const columns = computed<DataTableColumn[]>(() => {
  const base: DataTableColumn[] = [
    { prop: 'code', label: '分类编码', minWidth: 120 },
    { prop: 'name', label: '分类名称', minWidth: 160 },
    { prop: 'usefulLifeMonthsOverride', label: '折旧年限(月)', minWidth: 120, align: 'center', slot: 'usefulLife' },
    { prop: 'salvageRateOverride', label: '残值率(%)', minWidth: 100, align: 'center', slot: 'salvageRate' },
    { prop: 'assetCount', label: '资产数', minWidth: 85, align: 'center' },
    { prop: 'totalValue', label: '分类总值(元)', minWidth: 130, align: 'right', slot: 'totalValue' },
    { prop: 'monthlyDepreciation', label: '月折旧额(元)', minWidth: 130, align: 'right', slot: 'monthlyDepreciation' },
    { prop: 'createTime', label: '创建时间', minWidth: 160 },
  ]
  return base
})

/* ===== 工具函数 ===== */
// 金额转换走 @/utils/money，对 undefined/null 显示 '-' 保持原语义
function fenToYuan(fen: number | undefined): string {
  if (fen === undefined || fen === null) return '-'
  return fenToYuanFromUtils(fen)
}

/** 扁平化分类列表用于父分类选择（排除当前编辑项及其子项）*/
const parentOptions = computed(() => {
  const flatList = flattenCategoryList(categoryList.value)
  if (!editingId.value) return flatList
  // 编辑时排除自身及子分类，防止循环引用
  const excludeIds = new Set<string>()
  const collectChildren = (id: string) => {
    excludeIds.add(id)
    const children = flatList.filter(c => c.parentId === id)
    children.forEach(c => collectChildren(c.id))
  }
  collectChildren(editingId.value)
  return flatList.filter(c => !excludeIds.has(c.id))
})

function flattenCategoryList(list: AssetCategory[]): AssetCategory[] {
  const result: AssetCategory[] = []
  const walk = (nodes: AssetCategory[], prefix = '') => {
    for (const node of nodes) {
      result.push({ ...node, name: prefix + node.name })
      if (node.children && node.children.length > 0) {
        walk(node.children, prefix + '　')
      }
    }
  }
  walk(list)
  return result
}

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    if (isTreeView.value) {
      const tree = await categoryApi.getTree()
      const keyword = searchForm.value.keyword.trim()
      tableData.value = keyword ? filterTree(tree, keyword) : tree
    } else {
      const list = await categoryApi.getList()
      const keyword = searchForm.value.keyword.trim()
      tableData.value = keyword ? list.filter(c => c.name.includes(keyword) || c.code.includes(keyword)) : list
    }
    // 分类列表始终加载完整树用于父分类选择
    categoryList.value = await categoryApi.getTree()
    pagination.total = isTreeView.value ? countTreeNodes(tableData.value) : tableData.value.length
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载数据失败')
    }
  } finally {
    loading.value = false
  }
}

function filterTree(nodes: AssetCategory[], keyword: string): AssetCategory[] {
  return nodes.reduce((acc, node) => {
    const children = node.children ? filterTree(node.children, keyword) : []
    if (node.name.includes(keyword) || node.code.includes(keyword) || children.length > 0) {
      acc.push({ ...node, children: children.length > 0 ? children : node.children })
    }
    return acc
  }, [])
}

function countTreeNodes(nodes: AssetCategory[]): number {
  let count = 0
  for (const node of nodes) {
    count += 1
    if (node.children) count += countTreeNodes(node.children)
  }
  return count
}

function resetSearchForm() {
  searchForm.value = { keyword: '' }
}

/* ===== 新增/编辑 ===== */
function handleCreate() {
  editingId.value = null
  categoryForm.value = {
    name: '',
    code: '',
    parentId: null,
    usefulLifeMonthsOverride: undefined,
    salvageRateOverride: undefined,
    sortOrder: 0,
  }
  formDialogVisible.value = true
}

function handleEdit(row: AssetCategory) {
  editingId.value = row.id
  categoryForm.value = {
    name: row.name,
    code: row.code,
    parentId: row.parentId,
    usefulLifeMonthsOverride: row.usefulLifeMonthsOverride,
    salvageRateOverride: row.salvageRateOverride,
    sortOrder: row.sortOrder,
  }
  formDialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  try {
    const submitData = {
      name: categoryForm.value.name,
      code: categoryForm.value.code,
      parentId: categoryForm.value.parentId,
      usefulLifeMonthsOverride: categoryForm.value.usefulLifeMonthsOverride,
      salvageRateOverride: categoryForm.value.salvageRateOverride,
      sortOrder: categoryForm.value.sortOrder,
    }
    if (editingId.value) {
      await categoryApi.update(editingId.value, submitData)
      ElMessage.success('更新成功')
    } else {
      await categoryApi.create(submitData)
      ElMessage.success('创建成功')
    }
    formDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

/* ===== 删除 ===== */
async function handleDelete(row: AssetCategory) {
  try {
    await ElMessageBox.confirm(`确认删除分类"${row.name}"？该操作不可恢复。`, '删除确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await categoryApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 用户取消
  }
}

/* ===== 视图切换 ===== */
function handleViewToggle() {
  isTreeView.value = !isTreeView.value
  loadData()
}

/* ===== 生命周期 ===== */
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="资产分类" description="资产类别与折旧政策配置">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增分类
      </el-button>
    </PageHeader>

    <div class="search-bar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.keyword" placeholder="搜索分类名称/编码" clearable :prefix-icon="Search" style="width:200px" size="default" @keyup.enter="stdSearch" />
        <el-button-group style="margin-left: 8px">
          <el-button :type="isTreeView ? 'primary' : 'default'" size="default" @click="!isTreeView && handleViewToggle()">树形</el-button>
          <el-button :type="!isTreeView ? 'primary' : 'default'" size="default" @click="isTreeView && handleViewToggle()">列表</el-button>
        </el-button-group>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" size="default" @click="stdSearch">查询</el-button>
        <el-button size="default" @click="stdReset">重置</el-button>
      </div>
    </div>

    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        stripe
        :row-key="'id'"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :default-expand-all="isTreeView"
        :actions-width="160"
      >
        <template #usefulLife="{ row }">
          {{ row.usefulLifeMonthsOverride != null ? row.usefulLifeMonthsOverride : '-' }}
        </template>
        <template #salvageRate="{ row }">
          {{ row.salvageRateOverride != null ? row.salvageRateOverride : '-' }}
        </template>
        <template #totalValue="{ row }">
          {{ fenToYuan(row.totalValue) }}
        </template>
        <template #monthlyDepreciation="{ row }">
          {{ fenToYuan(row.monthlyDepreciation) }}
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </DataTable>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="categoryForm" :rules="formRules" label-width="110px" label-position="right">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类名称" prop="name">
              <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码" prop="code">
              <el-input v-model="categoryForm.code" placeholder="如 CAT-KT" :disabled="!!editingId" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="父分类">
              <el-select v-model="categoryForm.parentId" placeholder="无（顶级分类）" clearable :teleported="false" style="width:100%">
                <el-option v-for="cat in parentOptions" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序序号">
              <el-input-number v-model="categoryForm.sortOrder" :min="0" :step="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="折旧年限(月)">
              <el-input-number v-model="categoryForm.usefulLifeMonthsOverride" :min="1" :step="12" placeholder="覆盖默认值" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="残值率(%)">
              <el-input-number v-model="categoryForm.salvageRateOverride" :min="0" :max="100" :precision="1" placeholder="覆盖默认值" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page { min-height: 100vh; background: var(--fts-bg-page); }
.search-bar {
  display: flex; align-items: center; justify-content: space-between; gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6); background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary); border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
  .toolbar-left { display: flex; align-items: center; gap: var(--fts-space-3); flex-wrap: wrap; }
  .toolbar-right { display: flex; align-items: center; gap: var(--fts-space-3); }
}
.table-section { padding: 0 var(--fts-space-6) var(--fts-space-6); background: var(--fts-bg-card); border: 1px solid var(--fts-border-primary); border-top: none; overflow-x: auto; }
.pagination-wrapper { display: flex; justify-content: flex-end; padding: var(--fts-space-4) var(--fts-space-6); background: var(--fts-bg-card); border: 1px solid var(--fts-border-primary); border-top: none; border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius); }
:deep(.el-dialog__body) { padding-top: var(--fts-space-4); }
:deep(.el-form-item__label) { white-space: nowrap; }
</style>
