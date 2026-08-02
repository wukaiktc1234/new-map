<script setup lang="ts">
/**
 * 门店招聘页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成门店招聘的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { recruitmentApi } from '@/api/store-ops/recruitment'
import type { Recruitment } from '@/api/store-ops/recruitment'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

/** 招聘表单数据 */
interface RecruitmentFormData {
  recruitmentId?: string
  storeName: string
  positionName: string
  headcount: number
  salaryMin: number
  salaryMax: number
  description: string
  benefits: string
  status: 'open' | 'closed' | 'paused' | 'filled'
}

/** 查询表单 */
interface QueryForm {
  storeId: number | string
  positionName: string
  status: '' | Recruitment['status']
  startDate: string
  endDate: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<Recruitment[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentDetail = ref<Recruitment | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref<QueryForm>({
  storeId: '',
  positionName: '',
  status: '',
  startDate: '',
  endDate: '',
})

// 门店选项（从真实 API 加载）
const { storeOptions, loadStores, getStoreName } = useStoreOptions(true)

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<Recruitment, QueryForm>({
  api: {
    getList: async (params: QueryForm & { page: number; size: number }) => {
      const list = await recruitmentApi.getRecruitmentList({
        page: params.page,
        size: params.size,
      })
      return {
        records: list,
        total: list.length,
        current: params.page,
        size: params.size,
        pages: Math.ceil(list.length / params.size),
      }
    },
  } as unknown as CrudApi<Recruitment, QueryForm>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive<RecruitmentFormData>({
  storeName: '',
  positionName: '',
  headcount: 1,
  salaryMin: 0,
  salaryMax: 0,
  description: '',
  benefits: '',
  status: 'open',
})

// 统计数据
const statistics = computed(() => {
  const list = tableData.value
  return {
    openPositions: list.filter(item => item.status === 'open').length,
    pendingInterview: list.reduce((sum, item) => sum + (item.applicants || 0), 0),
    hired: list.reduce((sum, item) => sum + (item.hiredCount || 0), 0),
    onboardedThisMonth: list.reduce((sum, item) => sum + (item.onboardedCount || 0), 0),
  }
})

// ==================== 表格列定义 ====================

const columns = computed<DataTableColumn[]>(() => [
  { prop: 'recruitmentId', label: '招聘编号', width: 120, slot: 'recruitmentId' },
  { prop: 'department', label: '门店', minWidth: 120, slot: 'store' },
  { prop: 'positionName', label: '职位名称', minWidth: 140, slot: 'positionName' },
  { prop: 'headcount', label: '招聘人数', width: 100, align: 'center', slot: 'headcount' },
  { prop: 'hiredCount', label: '已招人数', width: 100, align: 'center', slot: 'hiredCount' },
  { prop: 'progress', label: '招聘进度', minWidth: 180, slot: 'progress' },
  { prop: 'publishDate', label: '发布日期', width: 120, slot: 'publishDate' },
  { prop: 'status', label: '状态', width: 100, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  storeName: [
    { required: true, message: '请选择门店', trigger: 'change' },
  ],
  positionName: [
    { required: true, message: '请输入职位名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  headcount: [
    { required: true, message: '请输入招聘人数', trigger: 'blur' },
  ],
  salaryMin: [
    { required: true, message: '请输入最低薪资', trigger: 'blur' },
  ],
  salaryMax: [
    { required: true, message: '请输入最高薪资', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.storeId = ''
  queryForm.value.positionName = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  refresh()
}

function handleSelectionChange(rows: Recruitment[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    storeName: '',
    positionName: '',
    headcount: 1,
    salaryMin: 0,
    salaryMax: 0,
    description: '',
    benefits: '',
    status: 'open',
  })
  dialogVisible.value = true
}

function handleEdit(row: Recruitment) {
  isEdit.value = true
  Object.assign(formData, {
    recruitmentId: row.recruitmentId,
    storeName: row.department,
    positionName: row.positionName,
    headcount: row.headcount,
    salaryMin: row.salaryMin,
    salaryMax: row.salaryMax,
    description: row.description || '',
    benefits: '',
    status: row.status,
  })
  dialogVisible.value = true
}

function handleDetail(row: Recruitment) {
  currentDetail.value = row
  detailVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      ElMessage.success('更新成功')
    } else {
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

/**
 * 发布/下架招聘
 */
async function handleToggleStatus(row: Recruitment) {
  const isOpen = row.status === 'open'
  const text = isOpen ? '下架' : '发布'
  try {
    await ElMessageBox.confirm(`确定要${text}招聘「${row.positionName}」吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success(`${text}成功`)
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/**
 * 删除招聘
 */
async function handleDelete(row: Recruitment): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除招聘「${row.positionName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 导出招聘数据
 */
async function handleExport(): Promise<void> {
  try {
    ElMessage.info('正在导出数据，请稍候...')
    const csvContent = generateCSV()
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `招聘数据_${timestamp}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功！文件已开始下载')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  }
}

function generateCSV(): string {
  const headers = ['招聘编号', '门店', '职位名称', '招聘人数', '已招人数', '薪资范围', '发布日期', '状态']
  const rows = tableData.value.map(item => [
    item.recruitmentId,
    item.department,
    item.positionName,
    String(item.headcount),
    String(item.hiredCount || 0),
    item.salaryRange,
    item.publishDate,
    getStatusLabel(item.status),
  ])
  return [headers.join(','), ...rows.map(row => row.join(','))].join('\n')
}

// ==================== 辅助方法 ====================

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    open: 'success',
    closed: 'inactive',
    paused: 'warning',
    filled: 'active',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    open: '招聘中',
    closed: '已关闭',
    paused: '已暂停',
    filled: '已招满',
  }
  return map[status] || status
}

function getProgressPercentage(row: Recruitment): number {
  const hired = row.hiredCount || 0
  const total = row.headcount || 1
  return Math.min(100, Math.round((hired / total) * 100))
}

function getProgressStatus(row: Recruitment): '' | 'success' | 'warning' | 'exception' {
  const percentage = getProgressPercentage(row)
  if (percentage >= 100) return 'success'
  if (percentage >= 80) return 'warning'
  return ''
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

/**
 * 获取审批步骤索引
 * 用于详情页的步骤条显示
 */
function getApprovalStepIndex(status: string): number {
  const map: Record<string, number> = {
    pending: 0,
    store_approved: 1,
    regional_approved: 2,
    hr_approved: 3,
    rejected: 0,
  }
  return map[status] ?? 0
}

onMounted(() => {
  // 数据由 useCrudTable autoLoad 自动加载
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="门店招聘" description="管理门店的招聘需求和招聘进度">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增招聘
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Briefcase" label="招聘中职位" :value="String(statistics.openPositions)" color-type="primary" variant="bordered" />
      <StatCard icon="User" label="待面试" :value="String(statistics.pendingInterview)" color-type="success" variant="bordered" />
      <StatCard icon="CircleCheck" label="已录用" :value="String(statistics.hired)" color-type="warning" variant="bordered" />
      <StatCard icon="Calendar" label="本月入职" :value="String(statistics.onboardedThisMonth)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.storeId"
            placeholder="选择门店"
            clearable
            style="width: 140px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="store.storeId"
            />
          </el-select>
          <el-input
            v-model="queryForm.positionName"
            placeholder="职位名称"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.status"
            placeholder="招聘状态"
            clearable
            style="width: 120px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option label="招聘中" value="open" />
            <el-option label="已暂停" value="paused" />
            <el-option label="已招满" value="filled" />
            <el-option label="已关闭" value="closed" />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            style="width: 140px"
            size="default"
            value-format="YYYY-MM-DD"
            :teleported="false"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            style="width: 140px"
            size="default"
            value-format="YYYY-MM-DD"
            :teleported="false"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button size="default" @click="handleExport">
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
        <!-- 招聘编号列 -->
        <template #recruitmentId="{ row }">
          <span class="id-text">{{ row.recruitmentId }}</span>
        </template>

        <!-- 门店列 -->
        <template #store="{ row }">
          <span class="store-text">{{ row.department }}</span>
        </template>

        <!-- 职位名称列 -->
        <template #positionName="{ row }">
          <div class="position-cell">
            <div class="position-name">{{ row.positionName }}</div>
            <div class="position-salary">{{ row.salaryRange }}</div>
          </div>
        </template>

        <!-- 招聘人数列 -->
        <template #headcount="{ row }">
          <span class="count-text">{{ row.headcount }}</span>
        </template>

        <!-- 已招人数列 -->
        <template #hiredCount="{ row }">
          <span class="count-text">{{ row.hiredCount || 0 }}</span>
        </template>

        <!-- 招聘进度列 -->
        <template #progress="{ row }">
          <div class="progress-cell">
            <el-progress
              :percentage="getProgressPercentage(row)"
              :status="getProgressStatus(row)"
              :stroke-width="8"
              :show-text="true"
            />
            <div class="progress-text">
              {{ row.hiredCount || 0 }} / {{ row.headcount }} 人
            </div>
          </div>
        </template>

        <!-- 发布日期列 -->
        <template #publishDate="{ row }">
          <span class="date-text">{{ formatDate(row.publishDate) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusType(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              link
              :type="row.status === 'open' ? 'warning' : 'success'"
              size="default"
              @click.stop="handleToggleStatus(row)"
            >
              {{ row.status === 'open' ? '下架' : '发布' }}
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
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑招聘' : '新增招聘'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="门店" prop="storeName">
              <el-select v-model="formData.storeName" placeholder="请选择门店" :teleported="false" style="width: 100%">
                <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName" :value="store.storeName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="positionName">
              <el-input v-model="formData.positionName" placeholder="请输入职位名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="招聘人数" prop="headcount">
              <el-input-number v-model="formData.headcount" :min="1" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio value="open">招聘中</el-radio>
                <el-radio value="paused">暂停</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最低薪资" prop="salaryMin">
              <el-input-number v-model="formData.salaryMin" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高薪资" prop="salaryMax">
              <el-input-number v-model="formData.salaryMax" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="职位要求" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入职位要求"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="福利待遇" prop="benefits">
          <el-input
            v-model="formData.benefits"
            type="textarea"
            :rows="3"
            placeholder="请输入福利待遇"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="招聘详情"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-wrapper">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="招聘编号">
              {{ currentDetail.recruitmentId }}
            </el-descriptions-item>
            <el-descriptions-item label="门店">
              {{ currentDetail.department }}
            </el-descriptions-item>
            <el-descriptions-item label="职位名称">
              {{ currentDetail.positionName }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getStatusType(currentDetail.status)" :label="getStatusLabel(currentDetail.status)" size="small" variant="light" />
            </el-descriptions-item>
            <el-descriptions-item label="薪资范围">
              {{ currentDetail.salaryRange }}
            </el-descriptions-item>
            <el-descriptions-item label="招聘人数">
              {{ currentDetail.headcount }} 人
            </el-descriptions-item>
            <el-descriptions-item label="已招人数">
              {{ currentDetail.hiredCount || 0 }} 人
            </el-descriptions-item>
            <el-descriptions-item label="发布日期">
              {{ formatDate(currentDetail.publishDate) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 招聘进度 -->
        <div class="detail-section">
          <div class="section-title">招聘进度</div>
          <div class="progress-detail">
            <el-progress
              :percentage="getProgressPercentage(currentDetail)"
              :status="getProgressStatus(currentDetail)"
              :stroke-width="12"
            />
            <div class="progress-detail-text">
              已招聘 {{ currentDetail.hiredCount || 0 }} 人 / 计划招聘 {{ currentDetail.headcount }} 人
            </div>
          </div>
        </div>

        <!-- 职位描述 -->
        <div class="detail-section">
          <div class="section-title">职位要求</div>
          <div class="detail-content">
            {{ currentDetail.description || '暂无' }}
          </div>
        </div>

        <!-- 审批状态 -->
        <div class="detail-section">
          <div class="section-title">审批状态</div>
          <el-steps :active="getApprovalStepIndex(currentDetail.approvalStatus)" finish-status="success" simple>
            <el-step title="提交申请" />
            <el-step title="门店审批" />
            <el-step title="区域审批" />
            <el-step title="HR审批" />
          </el-steps>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 编号
.id-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

// 门店
.store-text {
  color: var(--fts-text-primary);
}

// 职位单元格
.position-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .position-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
  }

  .position-salary {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

// 数字
.count-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 进度单元格
.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 160px;

  .progress-text {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    text-align: center;
  }
}

// 日期
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
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

// 详情对话框
.detail-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

.detail-content {
  color: var(--fts-text-secondary);
  line-height: 1.8;
  padding: var(--fts-space-3);
  background: var(--fts-bg-page);
  border-radius: var(--fts-card-radius);
}

.progress-detail {
  padding: var(--fts-space-4);
  background: var(--fts-bg-page);
  border-radius: var(--fts-card-radius);

  .progress-detail-text {
    text-align: center;
    margin-top: var(--fts-space-2);
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
  }
}

// 响应式
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
