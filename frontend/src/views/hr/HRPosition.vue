<script setup lang="ts">
/**
 * 岗位管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理组织架构中的岗位，提供岗位的增删改查
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Postcard, User, OfficeBuilding, Document, Brush } from '@element-plus/icons-vue'
import type { UploadRawFile, UploadRequestOptions } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { departmentApi } from '@/api/hr'
import { positionApi } from '@/api/hr/position'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { usePositionImportExport } from '@/composables/usePositionImportExport'
import type { PositionItem, PositionQueryForm } from '@/api/hr/position'
import type { DepartmentDTO } from '@/types/hr'
import PositionFormDialog from './components/PositionFormDialog.vue'
import PositionDetailDialog from './components/PositionDetailDialog.vue'

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

// ==================== 响应式数据 ====================

const selectedRows = ref<PositionItem[]>([])
const formDialogVisible = ref(false)
const formIsEdit = ref(false)
const formInitialData = ref<Partial<PositionItem>>({})
const detailDialogVisible = ref(false)
const currentDetail = ref<PositionItem | null>(null)

// 左侧部门树当前选中的节点
const selectedDeptId = ref<string>('')

// 查询表单
const queryForm = ref<PositionQueryForm>({
  keyword: '',
  departmentId: '',
  status: '' as '' | 'active' | 'inactive',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<PositionItem, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const { page, size, departmentId, status, keyword } = params
      return positionApi.getList({
        current: page,
        size,
        keyword: keyword || undefined,
        departmentId: departmentId || undefined,
        status: status || undefined,
      })
    },
  } as unknown as CrudApi<PositionItem, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 部门树数据
const departmentTree = ref<DepartmentDTO[]>([])

// 导入/导出
const {
  importLoading,
  exportLoading,
  handleDownloadTemplate,
  handleExport,
  handleImport,
} = usePositionImportExport(departmentTree, refresh)

// ==================== 统计卡片 ====================

const statistics = computed(() => {
  const all = tableData.value
  const total = all.length
  const activeCount = all.filter(r => r.status === 'active').length
  const inactiveCount = all.filter(r => r.status === 'inactive').length
  return { total, activeCount, inactiveCount }
})

// ==================== 数据加载 ====================

async function loadDepartmentTree(): Promise<void> {
  try {
    departmentTree.value = await departmentApi.getDepartmentTree()
  } catch {
    departmentTree.value = []
  }
}

onMounted(() => {
  loadDepartmentTree()
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'positionCode', label: '岗位编码', minWidth: 120 },
  { prop: 'positionName', label: '岗位名称', minWidth: 140 },
  { prop: 'departmentName', label: '所属部门', minWidth: 120 },
  { prop: 'maxCount', label: '编制人数', minWidth: 100, align: 'center', slot: 'maxCount' },
  { prop: 'currentCount', label: '在职人数', minWidth: 100, align: 'center', slot: 'currentCount' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.departmentId = ''
  queryForm.value.status = ''
  selectedDeptId.value = ''
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: PositionItem[]) {
  selectedRows.value = rows
}

// 左侧树节点点击
function handleDeptNodeClick(data: DepartmentDTO) {
  // 点击公司根节点（level=0）时显示全部岗位，点击部门时按部门筛选
  const isRoot = data.level === 0 || data.type === 'company'
  selectedDeptId.value = data.id || ''
  queryForm.value.departmentId = isRoot ? '' : (data.id || '')
  pagination.current = 1
  refresh()
}

// 新增岗位
function handleCreate() {
  formIsEdit.value = false
  formInitialData.value = {}
  formDialogVisible.value = true
}

// 编辑岗位
async function handleEdit(row: PositionItem) {
  formIsEdit.value = true
  try {
    const detail = await positionApi.getById(row.positionId)
    formInitialData.value = { ...detail, positionId: detail.positionId }
    formDialogVisible.value = true
  } catch {
    ElMessage.error('加载岗位详情失败')
  }
}

function handleFormSuccess() {
  formDialogVisible.value = false
  refresh()
}

// 查看详情
function handleView(row: PositionItem) {
  currentDetail.value = { ...row }
  detailDialogVisible.value = true
}

// 删除岗位
async function handleDelete(row: PositionItem): Promise<void> {
  if (row.currentCount > 0) {
    ElMessage.warning(`岗位「${row.positionName}」下有${row.currentCount}名在职员工，无法删除`)
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除岗位「${row.positionName}」吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await positionApi.delete(row.positionId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

// 启用/停用
async function handleStatusToggle(row: PositionItem): Promise<void> {
  const isActive = row.status === 'active'
  const text = isActive ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${text}岗位「${row.positionName}」吗？`,
      '操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const newStatus: 'active' | 'inactive' = isActive ? 'inactive' : 'active'
    await positionApi.updateStatus(row.positionId, newStatus)
    ElMessage.success(`${text}成功`)
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// 清理 POS_ 前缀岗位
async function handleCleanupPositions(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '确定要清理所有编码以 POS_ 开头且无员工关联的岗位吗？此操作会物理删除数据，不可恢复。',
      '清理 POS_ 岗位',
      {
        confirmButtonText: '确定清理',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const result = await positionApi.cleanupByPrefix('POS_')
    ElMessage.success(`已清理 ${result.cleanedCount} 个 POS_ 前缀岗位`)
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '清理失败')
    }
  }
}

// 导入前验证
function beforeUpload(rawFile: UploadRawFile): boolean {
  const isExcel = rawFile.name.endsWith('.xlsx') || rawFile.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('只支持 Excel 格式的文件（.xlsx 或 .xls）')
    return false
  }

  const isLt5M = rawFile.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }

  return true
}

// 适配 el-upload http-request 参数
function handleImportOptions(options: UploadRequestOptions): void {
  handleImport(options.file)
}
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="岗位管理" description="管理组织架构中的岗位">
      <el-button type="warning" size="default" @click="handleCleanupPositions">
        <el-icon :size="16"><Brush /></el-icon>清理 POS_ 岗位
      </el-button>
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增岗位
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Postcard" label="岗位总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="User" label="启用岗位" :value="String(statistics.activeCount)" color-type="success" variant="bordered" />
      <StatCard icon="OfficeBuilding" label="停用岗位" :value="String(statistics.inactiveCount)" color-type="info" variant="bordered" />
    </section>

    <!-- 主内容区：左侧部门树 + 右侧列表 -->
    <div class="main-content">
      <!-- 左侧部门树 -->
      <aside class="sidebar">
        <div class="sidebar-header">按部门筛选</div>
        <div class="sidebar-content">
          <el-tree
            :data="departmentTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleDeptNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 'group' || data.level === 0"><OfficeBuilding /></el-icon>
                <el-icon v-else><Postcard /></el-icon>
                <span class="tree-node-label">{{ data.name }}</span>
                <span class="tree-node-count" v-if="data.employeeCount != null">({{ data.employeeCount }})</span>
              </span>
            </template>
          </el-tree>
        </div>
      </aside>

      <!-- 右侧列表区 -->
      <div class="content-area">
        <!-- 工具栏面板 -->
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-input
                v-model="queryForm.keyword"
                placeholder="搜索岗位名称或编码..."
                clearable
                style="width: 200px"
                size="default"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
              <el-select
                v-model="queryForm.departmentId"
                placeholder="部门"
                clearable
                style="width: 140px"
                size="default"
                @change="handleSearch"
              >
                <el-option
                  v-for="dept in departmentTree.flatMap(d => [d, ...(d.children || [])])"
                  :key="dept.id"
                  :label="dept.name"
                  :value="dept.id"
                />
              </el-select>
              <el-select
                v-model="queryForm.status"
                placeholder="状态"
                clearable
                style="width: 100px"
                size="default"
                @change="handleSearch"
              >
                <el-option label="启用" value="active" />
                <el-option label="停用" value="inactive" />
              </el-select>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
              <el-button size="default" @click="handleReset">
                <el-icon :size="14"><Refresh /></el-icon>重置
              </el-button>
              <el-button size="default" @click="handleDownloadTemplate">
                <el-icon :size="14"><Document /></el-icon>模板下载
              </el-button>
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="handleImportOptions"
                accept=".xlsx,.xls"
                :disabled="importLoading"
              >
                <el-button size="default" :loading="importLoading">
                  <el-icon :size="14"><Upload /></el-icon>批量导入
                </el-button>
              </el-upload>
              <el-button size="default" :loading="exportLoading" @click="handleExport">
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
            <!-- 编制人数列 -->
            <template #maxCount="{ row }">
              <span>{{ row.maxCount || 0 }}</span>
            </template>

            <!-- 在职人数列 -->
            <template #currentCount="{ row }">
              <span :class="{ 'count-warning': row.currentCount > row.maxCount && row.maxCount > 0 }">
                {{ row.currentCount }}/{{ row.maxCount || 0 }}
              </span>
            </template>

            <!-- 状态列 -->
            <template #status="{ row }">
              <StatusTag
                :status="row.status === 'active' ? 'active' : 'inactive'"
                :label="row.status === 'active' ? '启用' : '停用'"
                size="small"
                variant="light"
              />
            </template>

            <!-- 操作列 -->
            <template #operation="{ row }">
              <div class="action-text">
                <el-button link type="primary" size="default" @click.stop="handleView(row)">
                  详情
                </el-button>
                <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
                  编辑
                </el-button>
                <el-button
                  link
                  :type="row.status === 'active' ? 'warning' : 'primary'"
                  size="default"
                  @click.stop="handleStatusToggle(row)"
                >
                  {{ row.status === 'active' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
                  删除
                </el-button>
              </div>
            </template>
          </DataTable>

          <!-- 分页组件 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.pageSize"
              :total="pagination.total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @size-change="refresh"
              @current-change="refresh"
            />
          </div>
        </section>
      </div>
    </div>

    <PositionFormDialog
      v-model="formDialogVisible"
      :is-edit="formIsEdit"
      :initial-data="formInitialData"
      :department-tree="departmentTree"
      @success="handleFormSuccess"
    />

    <PositionDetailDialog
      v-model="detailDialogVisible"
      :data="currentDetail"
    />
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

// 主内容区布局
.main-content {
  display: flex;
  gap: var(--fts-space-4);
  align-items: flex-start;
}

// 左侧边栏
.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
}

.sidebar-header {
  padding: var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  border-bottom: 1px solid var(--fts-border-primary);
}

.sidebar-content {
  max-height: 600px;
  overflow-y: auto;
  padding: var(--fts-space-2);
}

// 树节点样式
.tree-node {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);

  .tree-node-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tree-node-count {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
  }
}

// 右侧内容区
.content-area {
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 人数警告样式
.count-warning {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
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
  padding: var(--fts-space-4) 0 0;
  background: var(--fts-bg-card);
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }
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
  .stats-section {
    grid-template-columns: 1fr;
  }

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
