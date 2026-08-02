<script setup lang="ts">
/**
 * 培训管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理员工培训计划、课程和记录的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Reading, UserFilled, CircleCheck, Calendar } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { trainingApi } from '@/api/hr'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  TrainingCourse,
  TrainingCourseFormData,
  TrainingCourseQueryParams,
  CourseType,
  CoursePublishStatus,
  StudyRecord,
} from '@/types/hr/training'
import {
  CourseTypeOptions,
  CoursePublishStatusOptions,
  CoursePublishStatusTagMap,
} from '@/types/hr/training'

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

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<TrainingCourse[]>([])
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const enrollDialogVisible = ref(false)
const isEdit = ref(false)
const currentCourse = ref<TrainingCourse | null>(null)
const detailTab = ref('basic')
const importLoading = ref(false)
const exportLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  courseType: '' as CourseType | '',
  publishStatus: '' as CoursePublishStatus | '',
  dateRange: [] as string[],
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<TrainingCourse, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: TrainingCourseQueryParams = {
        page: params.page,
        pageSize: params.size,
        keyword: params.keyword || undefined,
        courseType: params.courseType || undefined,
        publishStatus: params.publishStatus || undefined,
      }
      return trainingApi.getList(convertedParams)
    },
  } as unknown as CrudApi<TrainingCourse, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive<TrainingCourseFormData>({
  title: '',
  description: '',
  courseType: 'required',
  relatedArticleId: '',
  instructor: '',
  totalLessons: 1,
  duration: 60,
  deadline: '',
  certificateEligible: false,
  certificateValidityDays: 0,
  publishStatus: 'draft',
})

// 参训人员列表（详情对话框用）
const studyRecords = ref<StudyRecord[]>([])
const studyRecordsLoading = ref(false)

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  inProgress: tableData.value.filter(item => item.publishStatus === 'published').length,
  completed: tableData.value.filter(item => item.completedCount && item.assignedCount && item.completedCount >= item.assignedCount).length,
  thisMonth: tableData.value.reduce((sum, item) => sum + (item.assignedCount || 0), 0),
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'title', label: '培训名称', minWidth: 200, slot: 'title' },
  { prop: 'courseType', label: '培训类型', minWidth: 100, slot: 'courseType', ellipsis: false },
  { prop: 'totalLessons', label: '培训方式', minWidth: 100, slot: 'trainingMethod' },
  { prop: 'instructor', label: '培训讲师', minWidth: 120, slot: 'instructor' },
  { prop: 'createTime', label: '开始日期', minWidth: 120, slot: 'startDate' },
  { prop: 'deadline', label: '结束日期', minWidth: 120, slot: 'endDate' },
  { prop: 'assignedCount', label: '参训人数', minWidth: 100, slot: 'assignedCount' },
  { prop: 'publishStatus', label: '状态', minWidth: 100, slot: 'publishStatus', ellipsis: false },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  title: [
    { required: true, message: '请输入培训名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' },
  ],
  courseType: [{ required: true, message: '请选择培训类型', trigger: 'change' }],
  instructor: [{ required: true, message: '请输入培训讲师', trigger: 'blur' }],
  duration: [{ required: true, message: '请输入培训时长', trigger: 'blur' }],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.courseType = ''
  queryForm.value.publishStatus = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: TrainingCourse[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    title: '',
    description: '',
    courseType: 'required',
    relatedArticleId: '',
    instructor: '',
    totalLessons: 1,
    duration: 60,
    deadline: '',
    certificateEligible: false,
    certificateValidityDays: 0,
    publishStatus: 'draft',
  })
  dialogVisible.value = true
}

async function handleEdit(row: TrainingCourse) {
  isEdit.value = true
  try {
    const detail = await trainingApi.getById(row.id)
    currentCourse.value = detail
    Object.assign(formData, detail)
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载培训详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && currentCourse.value?.id) {
      await trainingApi.update(currentCourse.value.id, formData)
      ElMessage.success('更新成功')
    } else {
      await trainingApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDetail(row: TrainingCourse) {
  try {
    const detail = await trainingApi.getById(row.id)
    currentCourse.value = detail
    // 加载参训人员列表
    loadStudyRecords(row.id)
    detailDialogVisible.value = true
  } catch {
    ElMessage.error('加载培训详情失败')
  }
}

async function loadStudyRecords(courseId: string) {
  studyRecordsLoading.value = true
  try {
    const res = await trainingApi.getStudyRecords({ courseId, pageSize: 100 })
    studyRecords.value = res.records || []
  } catch {
    studyRecords.value = []
  } finally {
    studyRecordsLoading.value = false
  }
}

function handleEnroll(row: TrainingCourse) {
  currentCourse.value = row
  enrollDialogVisible.value = true
}

async function handleDelete(row: TrainingCourse) {
  try {
    await ElMessageBox.confirm(
      `确定要删除培训「${row.title}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await trainingApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 处理文件上传前的验证
 * 仅允许 Excel 文件（.xlsx / .xls），用于培训记录批量导入
 */
function beforeUpload(rawFile: UploadRawFile): boolean {
  const allowedTypes = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ]

  if (!allowedTypes.includes(rawFile.type) && !rawFile.name.endsWith('.xlsx') && !rawFile.name.endsWith('.xls')) {
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

/**
 * 导入培训记录
 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('正在导入数据，请稍候...')
    // 模拟导入成功
    setTimeout(() => {
      ElMessage.success('导入成功')
      refresh()
      importLoading.value = false
    }, 1500)
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败，请检查文件格式')
    importLoading.value = false
  }
}

/**
 * 导出培训数据
 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    // 模拟导出
    setTimeout(() => {
      ElMessage.success('导出成功！文件已开始下载')
      exportLoading.value = false
    }, 1500)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
    exportLoading.value = false
  }
}

/**
 * 确认报名
 */
async function handleConfirmEnroll() {
  try {
    ElMessage.success('报名成功')
    enrollDialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error('报名失败')
  }
}

// ==================== 辅助方法 ====================

function getCourseTypeLabel(type: CourseType): string {
  const opt = CourseTypeOptions.find(o => o.value === type)
  return opt?.label || type
}

function getCourseTypeColor(type: CourseType): string {
  return type === 'required' ? 'warning' : 'info'
}

function getPublishStatusLabel(status: CoursePublishStatus): string {
  const opt = CoursePublishStatusOptions.find(o => o.value === status)
  return opt?.label || status
}

function getPublishStatusTag(status: CoursePublishStatus): string {
  return CoursePublishStatusTagMap[status] || 'default'
}

function getTrainingMethodLabel(totalLessons?: number): string {
  if (!totalLessons || totalLessons <= 1) return '线下集中'
  return `线上课程（${totalLessons}课时）`
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes > 0 ? minutes + '分钟' : ''}`
  return `${minutes}分钟`
}

onMounted(() => {
  // 页面加载时的初始化操作
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="培训发展" description="管理员工培训计划、课程和记录">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增培训
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Reading" label="培训课程" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="进行中" :value="String(statistics.inProgress)" color-type="success" variant="bordered" />
      <StatCard icon="Reading" label="已完成" :value="String(statistics.completed)" color-type="warning" variant="bordered" />
      <StatCard icon="UserFilled" label="本月参训" :value="String(statistics.thisMonth)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索培训名称..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.courseType"
            placeholder="培训类型"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in CourseTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.publishStatus"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in CoursePublishStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            @change="handleSearch"
          />
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
            accept=".xlsx,.xls"
            :disabled="importLoading"
          >
            <el-button size="default" class="action-btn--import" :loading="importLoading">
              <el-icon :size="14"><Upload /></el-icon>导入培训记录
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
        <!-- 培训名称列 -->
        <template #title="{ row }">
          <div class="training-cell">
            <div class="training-info">
              <div class="training-name">{{ row.title }}</div>
              <div class="training-desc">{{ row.description ? row.description.slice(0, 30) + (row.description.length > 30 ? '...' : '') : '-' }}</div>
            </div>
          </div>
        </template>

        <!-- 培训类型列 -->
        <template #courseType="{ row }">
          <StatusTag :status="getCourseTypeColor(row.courseType)" :label="getCourseTypeLabel(row.courseType)" size="small" variant="light" />
        </template>

        <!-- 培训方式列 -->
        <template #trainingMethod="{ row }">
          <span class="method-text">{{ getTrainingMethodLabel(row.totalLessons) }}</span>
        </template>

        <!-- 培训讲师列 -->
        <template #instructor="{ row }">
          <span class="instructor-text">{{ row.instructor || '-' }}</span>
        </template>

        <!-- 开始日期列 -->
        <template #startDate="{ row }">
          <span class="date-text">{{ formatTime(row.createTime || '') }}</span>
        </template>

        <!-- 结束日期列 -->
        <template #endDate="{ row }">
          <span class="date-text">{{ formatDate(row.deadline || '') }}</span>
        </template>

        <!-- 参训人数列 -->
        <template #assignedCount="{ row }">
          <span class="count-text">{{ row.assignedCount || 0 }}人</span>
        </template>

        <!-- 状态列 -->
        <template #publishStatus="{ row }">
          <StatusTag :status="getPublishStatusTag(row.publishStatus)" :label="getPublishStatusLabel(row.publishStatus)" size="small" variant="light" />
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
            <el-button link type="success" size="default" @click.stop="handleEnroll(row)">
              报名
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
          @current-change="refresh"
          @size-change="refresh"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑培训' : '新增培训'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="培训名称" prop="title">
              <el-input v-model="formData.title" placeholder="请输入培训名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="培训类型" prop="courseType">
              <el-select v-model="formData.courseType" placeholder="请选择培训类型" :teleported="false" style="width: 100%">
                <el-option v-for="opt in CourseTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="培训方式">
              <el-select v-model="formData.totalLessons" placeholder="请选择培训方式" :teleported="false" style="width: 100%">
                <el-option label="线下集中" :value="1" />
                <el-option label="线上课程（4课时）" :value="4" />
                <el-option label="线上课程（6课时）" :value="6" />
                <el-option label="线上课程（8课时）" :value="8" />
                <el-option label="线上课程（12课时）" :value="12" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="培训讲师" prop="instructor">
              <el-input v-model="formData.instructor" placeholder="请输入培训讲师" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="培训地点">
              <el-input v-model="(formData as any).location" placeholder="请输入培训地点" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="培训时长(分钟)" prop="duration">
              <el-input-number v-model="formData.duration" :min="1" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="(formData as any).startDate" type="date" placeholder="选择开始日期" :teleported="false" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker v-model="formData.deadline" type="date" placeholder="选择结束日期" :teleported="false" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="培训内容">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="请输入培训内容/大纲" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="参训人员范围">
          <el-select v-model="(formData as any).scope" placeholder="请选择参训人员范围" multiple :teleported="false" style="width: 100%">
            <el-option label="全体员工" value="all" />
            <el-option label="前厅部" value="front_desk" />
            <el-option label="后厨部" value="kitchen" />
            <el-option label="财务部" value="finance" />
            <el-option label="人力资源部" value="hr" />
            <el-option label="行政部" value="admin" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布状态">
              <el-radio-group v-model="formData.publishStatus">
                <el-radio value="draft">草稿</el-radio>
                <el-radio value="published">已发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书资格">
              <el-switch v-model="formData.certificateEligible" active-text="是" inactive-text="否" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="formData.certificateEligible" label="证书有效期(天)">
          <el-input-number v-model="formData.certificateValidityDays" :min="1" :step="30" controls-position="right" style="width: 100%" />
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
      v-model="detailDialogVisible"
      title="培训详情"
      width="960px"
      class="fts-dialog--lg"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentCourse">
        <el-tabs v-model="detailTab" class="detail-tabs">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="培训名称">{{ currentCourse.title }}</el-descriptions-item>
              <el-descriptions-item label="培训类型">
                <StatusTag :status="getCourseTypeColor(currentCourse.courseType)" :label="getCourseTypeLabel(currentCourse.courseType)" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="培训方式">{{ getTrainingMethodLabel(currentCourse.totalLessons) }}</el-descriptions-item>
              <el-descriptions-item label="培训讲师">{{ currentCourse.instructor || '-' }}</el-descriptions-item>
              <el-descriptions-item label="开始日期">{{ formatTime(currentCourse.createTime || '') }}</el-descriptions-item>
              <el-descriptions-item label="结束日期">{{ formatDate(currentCourse.deadline || '') }}</el-descriptions-item>
              <el-descriptions-item label="培训时长">{{ currentCourse.duration }}分钟</el-descriptions-item>
              <el-descriptions-item label="发布状态">
                <StatusTag :status="getPublishStatusTag(currentCourse.publishStatus)" :label="getPublishStatusLabel(currentCourse.publishStatus)" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="已分配人数">{{ currentCourse.assignedCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="已完成人数">{{ currentCourse.completedCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="平均得分">{{ currentCourse.averageScore ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="证书资格">{{ currentCourse.certificateEligible ? '是' : '否' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 培训内容 -->
          <el-tab-pane label="培训内容" name="content">
            <div class="content-section">
              <p v-if="currentCourse.description" class="content-text">{{ currentCourse.description }}</p>
              <el-empty v-else description="暂无培训内容" />
            </div>
          </el-tab-pane>

          <!-- 参训人员列表 -->
          <el-tab-pane label="参训人员" name="participants">
            <div v-loading="studyRecordsLoading" class="participants-section">
              <el-table :data="studyRecords" size="default" border stripe style="width: 100%">
                <el-table-column prop="employeeName" label="员工姓名" min-width="100" />
                <el-table-column prop="employeeCode" label="工号" min-width="100" />
                <el-table-column prop="departmentName" label="部门" min-width="100" />
                <el-table-column prop="completed" label="完成状态" min-width="100">
                  <template #default="{ row }">
                    <StatusTag :status="row.completed ? 'success' : 'warning'" :label="row.completed ? '已完成' : '进行中'" size="small" />
                  </template>
                </el-table-column>
                <el-table-column prop="score" label="得分" min-width="80" />
                <el-table-column prop="startTime" label="开始时间" min-width="160" />
              </el-table>
              <el-empty v-if="studyRecords.length === 0 && !studyRecordsLoading" description="暂无参训人员" />
            </div>
          </el-tab-pane>

          <!-- 培训资料 -->
          <el-tab-pane label="培训资料" name="materials">
            <div class="materials-section">
              <el-empty description="暂无培训资料" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-dialog>

    <!-- 报名对话框 -->
    <el-dialog
      v-model="enrollDialogVisible"
      title="培训报名"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentCourse">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="培训名称">{{ currentCourse.title }}</el-descriptions-item>
          <el-descriptions-item label="培训类型">{{ getCourseTypeLabel(currentCourse.courseType) }}</el-descriptions-item>
          <el-descriptions-item label="培训讲师">{{ currentCourse.instructor || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ formatDate(currentCourse.deadline || '') }}</el-descriptions-item>
        </el-descriptions>
        <el-form style="margin-top: 20px" label-width="100px">
          <el-form-item label="选择员工">
            <el-select placeholder="请选择要报名的员工" multiple filterable :teleported="false" style="width: 100%">
              <el-option label="张伟" value="EMP001" />
              <el-option label="李娜" value="EMP002" />
              <el-option label="王强" value="EMP003" />
              <el-option label="刘洋" value="EMP004" />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="enrollDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmEnroll">确认报名</el-button>
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 培训信息单元格
.training-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.training-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .training-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .training-desc {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.method-text,
.instructor-text,
.date-text,
.count-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-base);
}

.count-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
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
.detail-tabs {
  margin-top: var(--fts-space-4);
}

.content-section {
  padding: var(--fts-space-4);
  min-height: 200px;
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);

  .content-text {
    line-height: 1.8;
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    margin: 0;
  }
}

.participants-section,
.materials-section {
  min-height: 200px;
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
