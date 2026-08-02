<script setup lang="ts">
/**
 * 知识库管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理企业知识库文档和培训资料
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Reading, View, Edit, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { knowledgeBaseApi } from '@/api/hr/knowledge-base'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  KnowledgeArticle,
  KnowledgeArticleFormData,
  KnowledgeArticleQueryParams,
  KnowledgeCategory,
  ArticlePublishStatus,
  KnowledgeCategoryStat,
} from '@/types/hr/knowledge-base'
import {
  KnowledgeCategoryOptions,
  ArticlePublishStatusOptions,
  ArticlePublishStatusTagMap,
} from '@/types/hr/knowledge-base'

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
}

interface TreeNode {
  id: string
  label: string
  count?: number
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<KnowledgeArticle[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentDetail = ref<KnowledgeArticle | null>(null)
const importLoading = ref(false)
const exportLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  category: '' as KnowledgeCategory | '',
  publishStatus: '' as ArticlePublishStatus | '',
})

// 当前选中的分类树节点
const selectedCategory = ref<string>('')

// 分类统计数据
const categoryStats = ref<KnowledgeCategoryStat[]>([])

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
  handleDelete,
} = useCrudTable<KnowledgeArticle, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: KnowledgeArticleQueryParams = {
        page: params.page,
        pageSize: params.size,
        keyword: params.keyword || undefined,
        category: params.category || undefined,
        publishStatus: params.publishStatus || undefined,
      }
      return knowledgeBaseApi.getList(convertedParams)
    },
    delete: async (id: string) => {
      await knowledgeBaseApi.delete(id)
    },
  } as unknown as CrudApi<KnowledgeArticle, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
  deleteConfirmText: '确定要删除这篇文档吗？此操作不可撤销！',
})

// 表单数据
const formData = reactive<Partial<KnowledgeArticleFormData>>({
  title: '',
  summary: '',
  category: 'safety',
  content: '',
  author: '',
  publishStatus: 'draft',
  tags: [],
  isImportant: false,
  authorDepartment: '',
})

// 附件列表
const attachmentList = ref<{ name: string; url: string; size: number }[]>([])

// 统计数据
const statistics = computed(() => {
  const total = pagination.total || 0
  const published = tableData.value.filter(item => item.publishStatus === 'published').length
  const draft = tableData.value.filter(item => item.publishStatus === 'draft').length
  const todayNew = tableData.value.filter(item => {
    if (!item.createTime) return false
    const today = new Date().toDateString()
    return new Date(item.createTime).toDateString() === today
  }).length
  return { total, published, draft, todayNew }
})

// 分类树数据
const categoryTree = computed<TreeNode[]>(() => {
  const allNode: TreeNode = { id: '', label: '全部分类', count: pagination.total || 0 }
  const categoryNodes: TreeNode[] = KnowledgeCategoryOptions.map(opt => {
    const stat = categoryStats.value.find(s => s.category === opt.value)
    return {
      id: opt.value,
      label: opt.label,
      count: stat?.count || 0,
    }
  })
  return [allNode, ...categoryNodes]
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'title', label: '文档标题', minWidth: 200, slot: 'title', ellipsis: true },
  { prop: 'category', label: '分类', minWidth: 100, slot: 'category' },
  { prop: 'author', label: '作者', minWidth: 100, slot: 'author' },
  { prop: 'viewCount', label: '阅读次数', minWidth: 90, slot: 'viewCount' },
  { prop: 'publishStatus', label: '状态', minWidth: 100, slot: 'publishStatus' },
  { prop: 'publishTime', label: '发布时间', minWidth: 160, slot: 'publishTime' },
  { prop: 'updateTime', label: '更新时间', minWidth: 160, slot: 'updateTime' },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入文档内容', trigger: 'blur' }],
}

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.category = ''
  queryForm.value.publishStatus = ''
  selectedCategory.value = ''
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: KnowledgeArticle[]) {
  selectedRows.value = rows
}

function handleCategoryClick(node: TreeNode) {
  selectedCategory.value = node.id
  queryForm.value.category = node.id as KnowledgeCategory | ''
  pagination.current = 1
  refresh()
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    title: '',
    summary: '',
    category: 'safety',
    content: '',
    author: '',
    publishStatus: 'draft',
    tags: [],
    isImportant: false,
    authorDepartment: '',
  })
  attachmentList.value = []
  dialogVisible.value = true
}

async function handleEdit(row: KnowledgeArticle) {
  isEdit.value = true
  try {
    const detail = await knowledgeBaseApi.getById(row.id)
    Object.assign(formData, {
      ...detail,
    })
    attachmentList.value = []
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载文档详情失败')
  }
}

async function handleView(row: KnowledgeArticle) {
  try {
    const detail = await knowledgeBaseApi.getById(row.id)
    currentDetail.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载文档详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await knowledgeBaseApi.update(formData.id, formData as KnowledgeArticleFormData)
      ElMessage.success('更新成功')
    } else {
      await knowledgeBaseApi.create(formData as KnowledgeArticleFormData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadCategoryStats()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handlePublish(row: KnowledgeArticle) {
  try {
    await ElMessageBox.confirm(
      `确定要发布文档「${row.title}」吗？发布后员工可查看。`,
      '发布确认',
      {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'info',
      }
    )
    await knowledgeBaseApi.publish(row.id)
    ElMessage.success('发布成功')
    refresh()
    loadCategoryStats()
  } catch {
    // 用户取消
  }
}

async function handleArchive(row: KnowledgeArticle) {
  try {
    await ElMessageBox.confirm(
      `确定要归档文档「${row.title}」吗？归档后员工将无法查看。`,
      '归档确认',
      {
        confirmButtonText: '确定归档',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await knowledgeBaseApi.archive(row.id)
    ElMessage.success('归档成功')
    refresh()
    loadCategoryStats()
  } catch {
    // 用户取消
  }
}

async function handleDeleteClick(row: KnowledgeArticle) {
  await handleDelete(row.id)
  loadCategoryStats()
}

function beforeUpload(rawFile: UploadRawFile): boolean {
  const isLt10M = rawFile.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

function handleImport(uploadFile: UploadFile): void {
  if (!uploadFile.raw) return
  importLoading.value = true
  setTimeout(() => {
    ElMessage.info('批量导入功能开发中')
    importLoading.value = false
  }, 500)
}

async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const params: KnowledgeArticleQueryParams = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: queryForm.value.keyword || undefined,
      category: queryForm.value.category || undefined,
      publishStatus: queryForm.value.publishStatus || undefined,
    }

    const res = await knowledgeBaseApi.getList(params)
    const data = res.records || []

    const csvContent = [
      ['文档标题', '分类', '作者', '阅读次数', '状态', '发布时间', '更新时间'].join(','),
      ...data.map(item => [
        `"${item.title}"`,
        getCategoryLabel(item.category),
        `"${item.author || ''}"`,
        item.viewCount,
        getPublishStatusLabel(item.publishStatus),
        item.publishTime || '',
        item.updateTime || '',
      ].join(','))
    ].join('\n')

    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `知识库文档_${timestamp}.csv`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

function getCategoryLabel(cat: KnowledgeCategory): string {
  return KnowledgeCategoryOptions.find(o => o.value === cat)?.label || cat
}

function getPublishStatusLabel(status: ArticlePublishStatus): string {
  return ArticlePublishStatusOptions.find(o => o.value === status)?.label || status
}

function getPublishStatusTag(status: ArticlePublishStatus): string {
  return ArticlePublishStatusTagMap[status] || 'info'
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadCategoryStats() {
  try {
    categoryStats.value = await knowledgeBaseApi.getCategoryStats()
  } catch {
    categoryStats.value = []
  }
}

onMounted(() => {
  loadCategoryStats()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="知识库" description="管理企业知识库文档和培训资料">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增文档
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Reading" label="文档总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已发布" :value="String(statistics.published)" color-type="success" variant="bordered" />
      <StatCard icon="EditPen" label="草稿" :value="String(statistics.draft)" color-type="info" variant="bordered" />
      <StatCard icon="Plus" label="今日新增" :value="String(statistics.todayNew)" color-type="warning" variant="bordered" />
    </section>

    <!-- 主内容区：左侧分类树 + 右侧内容列表 -->
    <div class="main-content">
      <!-- 左侧分类树 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">文档分类</span>
        </div>
        <div class="category-tree">
          <div
            v-for="node in categoryTree"
            :key="node.id"
            class="tree-node"
            :class="{ 'is-active': selectedCategory === node.id }"
            @click="handleCategoryClick(node)"
          >
            <span class="node-label">{{ node.label }}</span>
            <span class="node-count">{{ node.count }}</span>
          </div>
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <main class="content-area">
        <!-- 工具栏 -->
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-select
                v-model="queryForm.publishStatus"
                placeholder="状态筛选"
                clearable
                style="width: 140px"
                size="default"
                @change="handleSearch"
              >
                <el-option
                  v-for="opt in ArticlePublishStatusOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-input
                v-model="queryForm.keyword"
                placeholder="搜索文档标题..."
                clearable
                style="width: 240px"
                size="default"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
              <el-button size="default" @click="handleReset">
                <el-icon :size="14"><Refresh /></el-icon>重置
              </el-button>
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="handleImport"
                :disabled="importLoading"
              >
                <el-button size="default" class="action-btn--import" :loading="importLoading">
                  <el-icon :size="14"><Upload /></el-icon>批量导入
                </el-button>
              </el-upload>
              <el-button
                size="default"
                class="action-btn--export"
                :loading="exportLoading"
                @click="handleExport"
              >
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
            <!-- 文档标题列 -->
            <template #title="{ row }">
              <div class="title-cell">
                <el-icon class="title-icon"><Reading /></el-icon>
                <span class="title-text">{{ row.title }}</span>
              </div>
            </template>

            <!-- 分类列 -->
            <template #category="{ row }">
              <StatusTag :status="row.category === 'safety' ? 'error' : row.category === 'service' ? 'success' : row.category === 'manual' ? 'info' : 'warning'" :label="getCategoryLabel(row.category)" size="small" variant="light" />
            </template>

            <!-- 作者列 -->
            <template #author="{ row }">
              <span class="author-text">{{ row.author || '-' }}</span>
            </template>

            <!-- 阅读次数列 -->
            <template #viewCount="{ row }">
              <span class="view-count">
                <el-icon><View /></el-icon>
                {{ row.viewCount }}
              </span>
            </template>

            <!-- 状态列 -->
            <template #publishStatus="{ row }">
              <StatusTag :status="getPublishStatusTag(row.publishStatus)" :label="getPublishStatusLabel(row.publishStatus)" size="small" variant="light" />
            </template>

            <!-- 发布时间列 -->
            <template #publishTime="{ row }">
              <span class="time-text">{{ formatTime(row.publishTime || '') }}</span>
            </template>

            <!-- 更新时间列 -->
            <template #updateTime="{ row }">
              <span class="time-text">{{ formatTime(row.updateTime || '') }}</span>
            </template>

            <!-- 操作列 -->
            <template #operation="{ row }">
              <div class="action-text">
                <el-button link type="primary" size="default" @click.stop="handleView(row)">
                  查看
                </el-button>
                <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
                  编辑
                </el-button>
                <el-button
                  v-if="row.publishStatus === 'draft' || row.publishStatus === 'archived'"
                  link
                  type="success"
                  size="default"
                  @click.stop="handlePublish(row)"
                >
                  发布
                </el-button>
                <el-button
                  v-if="row.publishStatus === 'published'"
                  link
                  type="warning"
                  size="default"
                  @click.stop="handleArchive(row)"
                >
                  下架
                </el-button>
                <el-button link type="danger" size="default" @click.stop="handleDeleteClick(row)">
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
      </main>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑文档' : '新增文档'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="文档标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入文档标题" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" prop="category">
              <el-select v-model="formData.category" placeholder="请选择分类" :teleported="false" style="width: 100%">
                <el-option v-for="opt in KnowledgeCategoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="formData.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布状态">
              <el-radio-group v-model="formData.publishStatus">
                <el-radio value="draft">草稿</el-radio>
                <el-radio value="published">已发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="文档摘要">
          <el-input v-model="formData.summary" type="textarea" :rows="2" placeholder="请输入文档摘要" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="文档内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="12"
            placeholder="请输入文档内容..."
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="附件上传">
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            multiple
            :limit="5"
            :on-exceed="() => ElMessage.warning('最多上传5个附件')"
          >
            <el-button size="default" type="primary">
              <el-icon><Upload /></el-icon>选择文件
            </el-button>
            <template #tip>
              <div class="upload-tip">支持 PDF、Word、Excel 等格式，单个文件不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="formData.tags"
            multiple
            filterable
            allow-create
            default-first-option
            :teleported="false"
            placeholder="输入标签后按回车添加"
            style="width: 100%"
          />
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="文档详情"
      width="680px"
      class="fts-dialog--md"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文档标题" :span="2">{{ currentDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">
            <StatusTag :status="currentDetail.category === 'safety' ? 'error' : currentDetail.category === 'service' ? 'success' : currentDetail.category === 'manual' ? 'info' : 'warning'" :label="getCategoryLabel(currentDetail.category)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="getPublishStatusTag(currentDetail.publishStatus)" :label="getPublishStatusLabel(currentDetail.publishStatus)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="作者">{{ currentDetail.author || '-' }}</el-descriptions-item>
          <el-descriptions-item label="阅读次数">{{ currentDetail.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(currentDetail.publishTime || '') }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(currentDetail.updateTime || '') }}</el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.tags && currentDetail.tags.length > 0" label="标签" :span="2">
            <StatusTag v-for="tag in currentDetail.tags" :key="tag" status="info" :label="tag" size="small" class="tag-item" />
          </el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.summary" label="摘要" :span="2">{{ currentDetail.summary }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">文档内容</el-divider>
        <div class="detail-content">
          <pre>{{ currentDetail.content }}</pre>
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
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
  padding-bottom: var(--fts-space-6);
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
  width: 220px;
  flex-shrink: 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;

  .sidebar-header {
    padding: var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-primary);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
  }

  .category-tree {
    padding: var(--fts-space-2) 0;
  }

  .tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--fts-space-3) var(--fts-space-4);
    cursor: pointer;
    transition: background-color var(--fts-transition-base);

    &:hover {
      background-color: var(--fts-bg-hover);
    }

    &.is-active {
      background-color: var(--fts-primary-light-9);
      color: var(--fts-primary);

      .node-label {
        font-weight: var(--fts-font-weight-medium);
      }

      .node-count {
        background-color: var(--fts-primary);
        color: var(--fts-text-inverse);
      }
    }

    .node-label {
      font-size: var(--fts-font-size-base);
      color: var(--fts-text-primary);
    }

    .node-count {
      min-width: 24px;
      height: 20px;
      line-height: 20px;
      text-align: center;
      padding: 0 var(--fts-space-2);
      border-radius: 10px;
      background-color: var(--fts-bg-page);
      color: var(--fts-text-secondary);
      font-size: var(--fts-font-size-xs);
      font-variant-numeric: tabular-nums;
    }
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
  border: 1px solid var(--fts-border-primary);
  border-bottom: none;
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
  padding: 0;
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

// 标题单元格
.title-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  overflow: hidden;

  .title-icon {
    color: var(--fts-primary);
    flex-shrink: 0;
  }

  .title-text {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 作者文字
.author-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-base);
}

// 阅读次数
.view-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;

  .el-icon {
    font-size: 14px;
  }
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
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
  border-top: 1px solid var(--fts-border-primary);
}

// 上传提示
.upload-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-top: var(--fts-space-2);
}

// 详情内容
.detail-content {
  background: var(--fts-bg-page);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  max-height: 400px;
  overflow-y: auto;

  pre {
    margin: 0;
    font-family: var(--fts-font-family-mono, 'Courier New', Courier, monospace);
    font-size: var(--fts-font-size-sm);
    line-height: 1.6;
    color: var(--fts-text-primary);
    white-space: pre-wrap;
    word-break: break-all;
  }
}

// 标签项
.tag-item {
  margin-right: var(--fts-space-2);
  margin-bottom: var(--fts-space-1);
}

// 响应式
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .category-tree {
    display: flex;
    flex-wrap: wrap;
    gap: var(--fts-space-2);
    padding: var(--fts-space-3) var(--fts-space-4);

    .tree-node {
      border-radius: var(--fts-radius-md);
      padding: var(--fts-space-2) var(--fts-space-3);
      border: 1px solid var(--fts-border-primary);
    }
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
  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .table-section {
    padding: 0;
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
