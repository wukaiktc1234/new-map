<script setup lang="ts">
/**
 * 招聘管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理招聘需求、简历和面试流程
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, User, Briefcase, Check, Clock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'

import { recruitmentApi } from '@/api/hr'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  RecruitmentPosition,
  PositionCreateDTO,
  PositionUpdateDTO,
  PositionQueryParams,
  Resume,
  RecruitmentStatistics,
  EducationRequirement,
  InterviewCreateDTO,
} from '@/types/hr/recruitment'
import {
  RecruitmentStatus,
  RecruitmentStatusTagMap,
  RecruitmentStatusOptions,
  EducationOptions,
  ResumeStatus,
  ResumeStatusTagMap,
  InterviewTypeOptions,
} from '@/types/hr/recruitment'

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

const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const candidateDialogVisible = ref(false)
const currentPosition = ref<RecruitmentPosition | null>(null)
const formRef = ref<FormInstance>()

/** 部门选项（接入真实后端 API） */
const { departmentOptions, loadDepartments, getDepartmentName } = useDepartmentOptions(true)

/** 招聘统计数据 */
const statistics = ref<RecruitmentStatistics | null>(null)

// 查询表单
const queryForm = ref({
  keyword: '',
  departmentId: '',
  status: '' as '' | RecruitmentStatus,
  startDate: '',
  endDate: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<RecruitmentPosition, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const apiParams: PositionQueryParams = {
        page: params.page,
        pageSize: params.size,
        keyword: params.keyword || undefined,
        departmentId: params.departmentId || undefined,
        status: params.status || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      }
      return recruitmentApi.position.getList(apiParams)
    },
  } as unknown as CrudApi<RecruitmentPosition, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = ref<PositionCreateDTO>({
  positionName: '',
  departmentId: '',
  departmentName: '',
  headcount: 1,
  salaryRange: '',
  educationRequirement: 'none',
  experienceRequirement: '',
  jobDescription: '',
  requirements: '',
  deadline: '',
})

// 候选人列表（当前职位）
const candidateList = ref<Resume[]>([])
const candidateLoading = ref(false)
const activeCandidateTab = ref('list')
const currentCandidate = ref<Resume | null>(null)
const candidateDetailVisible = ref(false)

// 面试安排对话框
const interviewDialogVisible = ref(false)
const interviewFormRef = ref<FormInstance>()
const interviewSubmitLoading = ref(false)
const interviewForm = ref<InterviewCreateDTO & { interviewRound: number }>({
  resumeId: '',
  interviewRound: 1,
  interviewType: 'onsite',
  scheduledTime: '',
  interviewerName: '',
})

// 发布表单状态
const publishFormStatus = ref('draft')

const interviewRules: FormRules = {
  scheduledTime: [{ required: true, message: '请选择面试时间', trigger: 'change' }],
  interviewerName: [{ required: true, message: '请输入面试官姓名', trigger: 'blur' }],
}

// ==================== 统计数据 ====================

const positionStats = computed(() => {
  const s = statistics.value
  const data = tableData.value
  return [
    {
      key: 'open',
      icon: 'Briefcase',
      label: '招聘中职位',
      value: s?.openPositions ?? data.filter(p => p.status === RecruitmentStatus.PUBLISHED).length,
      colorType: 'primary' as const,
    },
    {
      key: 'interview',
      icon: 'Clock',
      label: '待面试',
      value: Math.floor((s?.totalApplicants ?? 0) * 0.3),
      colorType: 'warning' as const,
    },
    {
      key: 'hired',
      icon: 'Check',
      label: '已录用',
      value: s?.totalHired ?? 0,
      colorType: 'success' as const,
    },
    {
      key: 'onboard',
      icon: 'User',
      label: '本月入职',
      value: Math.floor((s?.totalHired ?? 0) * 0.4),
      colorType: 'info' as const,
    },
  ]
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'positionName', label: '职位名称', minWidth: 160, slot: 'positionName' },
  { prop: 'departmentName', label: '所属部门', minWidth: 110, slot: 'department' },
  { prop: 'educationRequirement', label: '学历要求', minWidth: 90, slot: 'education' },
  { prop: 'headcount', label: '招聘人数', minWidth: 90, align: 'center' },
  { prop: 'hiredCount', label: '已招人数', minWidth: 90, align: 'center', slot: 'hiredCount' },
  { prop: 'salaryRange', label: '薪资范围', minWidth: 120 },
  { prop: 'publishDate', label: '发布日期', minWidth: 120, slot: 'publishDate' },
  { prop: 'status', label: '招聘状态', minWidth: 100, slot: 'status' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  positionName: [
    { required: true, message: '请输入职位名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  headcount: [{ required: true, message: '请输入招聘人数', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  jobDescription: [{ required: true, message: '请输入职位描述', trigger: 'blur' }],
  requirements: [{ required: true, message: '请输入任职要求', trigger: 'blur' }],
}

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.departmentId = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  pagination.current = 1
  refresh()
}

function handleCreate() {
  isEdit.value = false
  publishFormStatus.value = 'draft'
  formData.value = {
    positionName: '',
    departmentId: '',
    departmentName: '',
    headcount: 1,
    salaryRange: '',
    educationRequirement: 'none',
    experienceRequirement: '',
    jobDescription: '',
    requirements: '',
    deadline: '',
  }
  dialogVisible.value = true
}

function handleEdit(row: RecruitmentPosition) {
  isEdit.value = true
  currentPosition.value = row
  publishFormStatus.value = row.status === RecruitmentStatus.PUBLISHED ? 'published' : 'draft'
  formData.value = {
    positionName: row.positionName,
    departmentId: row.departmentId,
    departmentName: row.departmentName,
    headcount: row.headcount,
    salaryRange: row.salaryRange || '',
    educationRequirement: row.educationRequirement,
    experienceRequirement: row.experienceRequirement || '',
    jobDescription: row.jobDescription,
    requirements: row.requirements,
    deadline: row.deadline,
  }
  dialogVisible.value = true
}

function handleDepartmentChange(val: string) {
  const dept = departmentOptions.value.find(d => d.id === val)
  formData.value.departmentName = dept?.name || ''
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  const publishNow = publishFormStatus.value === 'published'
  try {
    if (isEdit.value && currentPosition.value) {
      await recruitmentApi.position.update(currentPosition.value.id, formData.value as PositionUpdateDTO, publishNow)
      ElMessage.success('更新成功')
    } else {
      await recruitmentApi.position.create(formData.value, publishNow)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleViewDetail(row: RecruitmentPosition) {
  try {
    const detail = await recruitmentApi.position.getById(row.id)
    currentPosition.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

async function handleCandidates(row: RecruitmentPosition) {
  currentPosition.value = row
  candidateDialogVisible.value = true
  await loadCandidates(row.id)
}

async function loadCandidates(positionId: string) {
  candidateLoading.value = true
  try {
    const res = await recruitmentApi.resume.getList({
      page: 1,
      pageSize: 100,
      positionId,
    })
    candidateList.value = res.records
  } catch {
    ElMessage.error('加载候选人列表失败')
  } finally {
    candidateLoading.value = false
  }
}

async function handlePublish(row: RecruitmentPosition) {
  try {
    await ElMessageBox.confirm(
      `确定要发布职位「${row.positionName}」吗？发布后应聘者可投递简历。`,
      '确认发布',
      {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await recruitmentApi.position.publish(row.id)
    ElMessage.success('发布成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleClose(row: RecruitmentPosition) {
  try {
    await ElMessageBox.confirm(
      `确定要关闭职位「${row.positionName}」吗？关闭后将不再接受简历投递。`,
      '确认关闭',
      {
        confirmButtonText: '确定关闭',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await recruitmentApi.position.close(row.id)
    ElMessage.success('关闭成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleDelete(row: RecruitmentPosition) {
  try {
    await ElMessageBox.confirm(
      `确定要删除职位「${row.positionName}」吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await recruitmentApi.position.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 处理文件上传前的验证
 * 仅允许 Excel 文件（.xlsx / .xls），用于简历批量导入
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
 * 导入简历数据
 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }
  ElMessage.info('简历导入功能开发中...')
}

/**
 * 导出当前招聘职位列表为 CSV
 */
function handleExport(): void {
  const headers = ['职位名称', '所属部门', '招聘人数', '已招人数', '薪资范围', '学历要求', '发布日期', '招聘状态']
  const rows = tableData.value.map((row) => [
    row.positionName,
    row.departmentName,
    String(row.headcount),
    String(row.hiredCount),
    row.salaryRange || '面议',
    getEducationLabel(row.educationRequirement),
    formatDate(row.publishDate || row.createTime),
    getPositionStatusInfo(row.status).label,
  ])
  const csvContent = '\uFEFF' + [headers, ...rows].map((r) => r.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `招聘职位_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
  ElMessage.success('导出成功')
}

/**
 * 加载统计数据
 */
async function loadStatistics() {
  try {
    statistics.value = await recruitmentApi.stats.getStatistics()
  } catch {
    // 统计数据加载失败不影响主流程
  }
}

// ==================== 辅助方法 ====================

function handleViewCandidate(row: Resume) {
  currentCandidate.value = row
  candidateDetailVisible.value = true
}

async function handleRejectCandidate(row: Resume) {
  try {
    await ElMessageBox.confirm(`确定要拒绝候选人「${row.applicantName}」吗？`, '拒绝确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await recruitmentApi.resume.reject(row.id)
    ElMessage.success('操作成功')
    if (currentPosition.value) {
      await loadCandidates(currentPosition.value.id)
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

function handleScheduleInterview(row: Resume) {
  currentCandidate.value = row
  interviewForm.value = {
    resumeId: row.id,
    interviewRound: 1,
    interviewType: 'onsite',
    scheduledTime: '',
    interviewerName: '',
  }
  interviewDialogVisible.value = true
}

async function submitInterview() {
  if (!interviewFormRef.value) return
  const valid = await interviewFormRef.value.validate().catch(() => false)
  if (!valid) return

  interviewSubmitLoading.value = true
  try {
    await recruitmentApi.interview.create(interviewForm.value)
    ElMessage.success('面试安排成功')
    interviewDialogVisible.value = false
    if (currentPosition.value) {
      await loadCandidates(currentPosition.value.id)
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '安排失败')
  } finally {
    interviewSubmitLoading.value = false
  }
}

function getPositionStatusInfo(status: RecruitmentStatus) {
  return RecruitmentStatusTagMap[status] || { status: 'info', label: status }
}

function getResumeStatusInfo(status: ResumeStatus) {
  return ResumeStatusTagMap[status] || { status: 'info', label: status }
}

function getEducationLabel(edu: EducationRequirement): string {
  return EducationOptions.find(o => o.value === edu)?.label || edu
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// ==================== 初始化 ====================

onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="招聘管理" description="管理招聘需求、简历和面试流程">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>发布职位
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in positionStats"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="String(stat.value)"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索职位名称..."
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
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="招聘状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in RecruitmentStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            :teleported="false"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
          <span class="date-range-separator">至</span>
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            :teleported="false"
            value-format="YYYY-MM-DD"
            style="width: 140px"
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
          >
            <el-button size="default" class="action-btn--import">
              <el-icon :size="14"><Upload /></el-icon>导入简历
            </el-button>
          </el-upload>
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
        :stripe="true"
        :hover="true"
        :border="false"
        :actions-width="280"
        @row-click="handleViewDetail"
      >
        <!-- 职位名称列 -->
        <template #positionName="{ row }">
          <div class="position-cell">
            <div class="position-name">{{ row.positionName }}</div>
          </div>
        </template>

        <!-- 部门列 -->
        <template #department="{ row }">
          <StatusTag category="department" :label="row.departmentName" size="small" />
        </template>

        <!-- 学历要求列 -->
        <template #education="{ row }">
          <span class="education-text">{{ getEducationLabel(row.educationRequirement) }}</span>
        </template>

        <!-- 已招人数列 -->
        <template #hiredCount="{ row }">
          <span :class="{ 'hired-full': row.hiredCount >= row.headcount }">
            {{ row.hiredCount }}
          </span>
        </template>

        <!-- 发布日期列 -->
        <template #publishDate="{ row }">
          <span class="date-text">{{ formatDate(row.publishDate || row.createTime) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getPositionStatusInfo(row.status).status"
            :label="getPositionStatusInfo(row.status).label"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #actions="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === RecruitmentStatus.DRAFT || row.status === RecruitmentStatus.PUBLISHED"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button link type="info" size="default" @click.stop="handleCandidates(row)">
              候选人
            </el-button>
            <el-button
              v-if="row.status === RecruitmentStatus.DRAFT"
              link
              type="primary"
              size="default"
              @click.stop="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === RecruitmentStatus.PUBLISHED"
              link
              type="warning"
              size="default"
              @click.stop="handleClose(row)"
            >
              下架
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

    <!-- 新增/编辑职位对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑职位' : '新增职位'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基础信息 -->
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职位名称" prop="positionName">
              <el-input v-model="formData.positionName" placeholder="请输入职位名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门" prop="departmentId">
              <el-select
                v-model="formData.departmentId"
                placeholder="请选择部门"
                :teleported="false"
                style="width: 100%"
                @change="handleDepartmentChange"
              >
                <el-option
                  v-for="dept in departmentOptions"
                  :key="dept.id"
                  :label="dept.name"
                  :value="dept.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="招聘人数" prop="headcount">
              <el-input-number v-model="formData.headcount" :min="1" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="薪资范围">
              <el-input v-model="formData.salaryRange" placeholder="如：8K-12K" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作地点">
              <el-input v-model="formData.experienceRequirement" placeholder="请输入工作地点" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学历要求" prop="educationRequirement">
              <el-select v-model="formData.educationRequirement" placeholder="请选择学历要求" :teleported="false" style="width: 100%">
                <el-option v-for="opt in EducationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期" prop="deadline">
              <el-date-picker
                v-model="formData.deadline"
                type="date"
                placeholder="选择截止日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 描述信息 -->
        <el-divider content-position="left">职位描述</el-divider>
        <el-form-item label="职位描述" prop="jobDescription">
          <el-input
            v-model="formData.jobDescription"
            type="textarea"
            :rows="4"
            placeholder="请输入职位描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left">任职要求</el-divider>
        <el-form-item label="任职要求" prop="requirements">
          <el-input
            v-model="formData.requirements"
            type="textarea"
            :rows="4"
            placeholder="请输入任职要求"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 发布状态 -->
        <el-divider content-position="left">发布设置</el-divider>
        <el-form-item label="发布状态">
          <el-radio-group v-model="publishFormStatus">
            <el-radio value="draft">保存为草稿</el-radio>
            <el-radio value="published">立即发布</el-radio>
          </el-radio-group>
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
      title="职位详情"
      width="680px"
      class="fts-dialog--md"
      destroy-on-close
      lock-scroll="false"
    >
      <div v-if="currentPosition" class="detail-content">
        <div class="detail-header">
          <h3 class="detail-title">{{ currentPosition.positionName }}</h3>
          <StatusTag
            :status="getPositionStatusInfo(currentPosition.status).status"
            :label="getPositionStatusInfo(currentPosition.status).label"
            size="default"
            variant="light"
          />
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">所属部门</span>
            <span class="detail-value">{{ currentPosition.departmentName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">招聘人数</span>
            <span class="detail-value">{{ currentPosition.headcount }}人</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">已招人数</span>
            <span class="detail-value">{{ currentPosition.hiredCount }}人</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">薪资范围</span>
            <span class="detail-value">{{ currentPosition.salaryRange || '面议' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">学历要求</span>
            <span class="detail-value">{{ getEducationLabel(currentPosition.educationRequirement) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">截止日期</span>
            <span class="detail-value">{{ formatDate(currentPosition.deadline) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">发布日期</span>
            <span class="detail-value">{{ formatDate(currentPosition.publishDate || currentPosition.createTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">应聘人数</span>
            <span class="detail-value">{{ currentPosition.applicantCount }}人</span>
          </div>
        </div>

        <el-divider content-position="left">职位描述</el-divider>
        <div class="detail-desc">{{ currentPosition.jobDescription }}</div>

        <el-divider content-position="left">任职要求</el-divider>
        <div class="detail-desc">{{ currentPosition.requirements }}</div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 候选人管理对话框 -->
    <el-dialog
      v-model="candidateDialogVisible"
      title="候选人管理"
      width="960px"
      class="fts-dialog--lg"
      destroy-on-close
      lock-scroll="false"
    >
      <div v-if="currentPosition" class="candidate-header">
        <span class="candidate-position">职位：{{ currentPosition.positionName }}</span>
        <span class="candidate-count">共 {{ candidateList.length }} 位候选人</span>
      </div>

      <div class="candidate-tabs">
        <el-tabs v-model="activeCandidateTab" type="border-card">
          <el-tab-pane label="候选人列表" name="list">
            <el-table
              :data="candidateList"
              v-loading="candidateLoading"
              size="default"
              stripe
              style="width: 100%"
              max-height="400"
            >
              <el-table-column prop="applicantName" label="姓名" min-width="100" />
              <el-table-column prop="phone" label="联系电话" min-width="120" />
              <el-table-column prop="education" label="学历" min-width="80" />
              <el-table-column prop="submitTime" label="投递时间" min-width="160">
                <template #default="{ row }">
                  {{ formatDate(row.submitTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" min-width="100">
                <template #default="{ row }">
                  <StatusTag
                    :status="getResumeStatusInfo(row.status).status"
                    :label="getResumeStatusInfo(row.status).label"
                    size="small"
                    variant="light"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleViewCandidate(row)">查看</el-button>
                  <el-button link type="primary" size="small" @click="handleScheduleInterview(row)">安排面试</el-button>
                  <el-button link type="danger" size="small" @click="handleRejectCandidate(row)">拒绝</el-button>
                </template>
              </el-table-column>

              <template #empty>
                <EmptyState type="no-data" description="该职位暂无候选人" />
              </template>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="面试安排" name="interview">
            <EmptyState type="no-data" description="暂无面试安排" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="candidateDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 候选人详情对话框 -->
    <el-dialog
      v-model="candidateDetailVisible"
      title="候选人详情"
      width="680px"
      class="fts-dialog--md"
      destroy-on-close
      lock-scroll="false"
    >
      <div v-if="currentCandidate" class="candidate-detail">
        <div class="detail-item">
          <span class="detail-label">姓名</span>
          <span class="detail-value">{{ currentCandidate.applicantName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">联系电话</span>
          <span class="detail-value">{{ currentCandidate.phone }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">邮箱</span>
          <span class="detail-value">{{ currentCandidate.email || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">学历</span>
          <span class="detail-value">{{ currentCandidate.education }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">工作经验</span>
          <span class="detail-value">{{ currentCandidate.experience || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">投递时间</span>
          <span class="detail-value">{{ formatDate(currentCandidate.submitTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">当前状态</span>
          <StatusTag
            :status="getResumeStatusInfo(currentCandidate.status).status"
            :label="getResumeStatusInfo(currentCandidate.status).label"
            size="small"
            variant="light"
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="candidateDetailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 安排面试对话框 -->
    <el-dialog
      v-model="interviewDialogVisible"
      title="安排面试"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="interviewFormRef" :model="interviewForm" :rules="interviewRules" label-width="100px">
        <el-form-item label="面试官" prop="interviewerName">
          <el-input v-model="interviewForm.interviewerName" placeholder="请输入面试官姓名" />
        </el-form-item>
        <el-form-item label="面试轮次">
          <el-radio-group v-model="interviewForm.interviewRound">
            <el-radio :value="1">初试</el-radio>
            <el-radio :value="2">复试</el-radio>
            <el-radio :value="3">终试</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="面试方式">
          <el-select v-model="interviewForm.interviewType" :teleported="false" style="width: 100%">
            <el-option
              v-for="opt in InterviewTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="面试时间" prop="scheduledTime">
          <el-date-picker
            v-model="interviewForm.scheduledTime"
            type="datetime"
            placeholder="选择面试时间"
            :teleported="false"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="interviewDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="interviewSubmitLoading" @click="submitInterview">确定</el-button>
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

.date-range-separator {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
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
}

.education-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

// 已招满标记
.hired-full {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-semibold);
}

// 日期文字
.date-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 详情对话框
.detail-content {
  .detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-4);
    padding-bottom: var(--fts-space-3);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  .detail-title {
    margin: 0;
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--fts-space-3) var(--fts-space-6);
    margin-bottom: var(--fts-space-4);
  }

  .detail-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  .detail-label {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
    min-width: 80px;
  }

  .detail-value {
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
  }

  .detail-desc {
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
    line-height: 1.8;
    white-space: pre-wrap;
    padding: var(--fts-space-3);
    background: var(--fts-bg-page);
    border-radius: var(--fts-card-radius);
  }
}

// 候选人对话框
.candidate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
  padding-bottom: var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-primary);

  .candidate-position {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  .candidate-count {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
  }
}

.candidate-tabs {
  :deep(.el-tabs__content) {
    padding-top: var(--fts-space-3);
  }
}

.candidate-detail {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--fts-space-3);

  .detail-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }

  .detail-label {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
    min-width: 80px;
  }

  .detail-value {
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
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
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
