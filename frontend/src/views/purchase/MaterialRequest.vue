<script setup lang="ts">
/**
 * 物资需求页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成物资需求管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Document, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { materialRequestApi, materialRequestConverter } from '@/api/purchase'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import {
  MaterialRequestStatusOptions,
  type MaterialRequestInfo,
  type MaterialRequestItem,
  type MaterialRequestStatus,
  type MaterialRequestQueryForm,
} from '@/types/material-request'

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
const selectedRows = ref<MaterialRequestInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const detailData = ref<MaterialRequestInfo | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  requestNo: '',
  createByName: '',
  storeName: '',
  status: '' as '' | MaterialRequestStatus,
  startDate: '',
  endDate: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<MaterialRequestInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: MaterialRequestQueryForm & { page?: number; size?: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.requestNo) queryParams.requestNo = params.requestNo
      if (params.status) queryParams.status = params.status
      if (params.storeName) queryParams.storeName = params.storeName
      if (params.startDate) queryParams.startDate = params.startDate
      if (params.endDate) queryParams.endDate = params.endDate
      if (params.createByName) queryParams.keyword = params.createByName
      return materialRequestApi.getList(queryParams)
    },
  } as unknown as CrudApi<MaterialRequestInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = ref({
  pending: 0,
  reviewing: 0,
  approved: 0,
  myRequest: 0,
})

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await materialRequestApi.getStatistics()
    statistics.value = {
      pending: res.pending,
      reviewing: res.pending,
      approved: res.approved,
      myRequest: res.total,
    }
  } catch {
    statistics.value = {
      pending: 0,
      reviewing: 0,
      approved: 0,
      myRequest: 0,
    }
  }
}

// 优先级选项
const priorityOptions = [
  { label: '普通', value: 'normal' },
  { label: '紧急', value: 'urgent' },
  { label: '特急', value: 'critical' },
]

// 部门选项（从真实 API 加载，使用共享 composable，autoLoad 自动加载）
const { departmentOptions } = useDepartmentOptions(true)

// 状态选项
const statusOptions = MaterialRequestStatusOptions

// ==================== 表单数据 ====================

interface MaterialRequestFormState {
  title: string
  storeName: string
  priority: string
  expectedDate: string
  remark: string
  items: MaterialRequestItem[]
}

const formData = reactive<MaterialRequestFormState>({
  title: '',
  storeName: '',
  priority: 'normal',
  expectedDate: '',
  remark: '',
  items: [],
})

// 计算明细小计金额
function calcSubtotal(item: MaterialRequestItem): number {
  return Number(((item.quantity || 0) * (item.estimatedPrice || 0)).toFixed(2))
}

// 计算所有明细总金额
const formTotalAmount = computed(() => {
  return Number(formData.items.reduce((sum, item) => sum + calcSubtotal(item), 0).toFixed(2))
})

// 添加明细行
function addItem() {
  formData.items.push({
    materialId: '',
    materialName: '',
    specification: '',
    quantity: 1,
    unit: '',
    estimatedPrice: 0,
    subtotalAmount: 0,
    remark: '',
  })
}

// 删除明细行
function removeItem(index: number) {
  formData.items.splice(index, 1)
}

// 重新计算小计
function recalcSubtotal(index: number) {
  const item = formData.items[index]
  item.subtotalAmount = calcSubtotal(item)
}

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'requestNo', label: '需求单号', minWidth: 150, slot: 'requestNo' },
  { prop: 'title', label: '需求标题', minWidth: 180, slot: 'title' },
  { prop: 'createByName', label: '申请人', minWidth: 100, slot: 'createByName' },
  { prop: 'storeName', label: '申请部门', minWidth: 120, slot: 'storeName' },
  { prop: 'expectedDate', label: '需求日期', minWidth: 120, slot: 'expectedDate' },
  { prop: 'status', label: '状态', minWidth: 110, slot: 'status', ellipsis: false },
  { prop: 'priority', label: '优先级', minWidth: 90, slot: 'priority' },
  { prop: 'itemCount', label: '物资种类数', minWidth: 100, slot: 'itemCount' },
  { prop: '_operation', label: '操作', width: 280, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' },
  ],
  storeName: [{ required: true, message: '请选择申请部门', trigger: 'change' }],
  expectedDate: [{ required: true, message: '请选择期望到货日期', trigger: 'change' }],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
  loadStatistics()
}

function handleReset() {
  queryForm.value.requestNo = ''
  queryForm.value.createByName = ''
  queryForm.value.storeName = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  refresh()
  loadStatistics()
}

function handleSelectionChange(rows: MaterialRequestInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    title: '',
    storeName: '',
    priority: 'normal',
    expectedDate: '',
    remark: '',
    items: [],
  })
  addItem()
  dialogVisible.value = true
}

async function handleEdit(row: MaterialRequestInfo) {
  isEdit.value = true
  try {
    const detail = await materialRequestApi.getById(row.requestId)
    if (detail) {
      Object.assign(formData, {
        title: detail.title,
        storeName: detail.storeName,
        priority: 'normal',
        expectedDate: detail.expectedDate,
        remark: detail.remark,
        items: detail.items || [],
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载需求详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (formData.items.length === 0) {
    ElMessage.warning('请至少添加一条需求明细')
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value && detailData.value?.requestId) {
      await materialRequestApi.update(detailData.value.requestId, {
        title: formData.title,
        storeName: formData.storeName,
        expectedDate: formData.expectedDate,
        remark: formData.remark,
        items: formData.items,
      })
      ElMessage.success('更新成功')
    } else {
      await materialRequestApi.create({
        title: formData.title,
        storeName: formData.storeName,
        expectedDate: formData.expectedDate,
        remark: formData.remark,
        items: formData.items,
      })
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

// 提交审核
async function handleSubmitApproval(row: MaterialRequestInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要提交需求「${row.title}」进行审核吗？`,
      '提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await materialRequestApi.submit(row.requestId)
    ElMessage.success('提交成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '提交失败')
    }
  }
}

// 批量提交
async function handleBatchSubmit() {
  if (selectedRows.value.length === 0) return
  const draftRows = selectedRows.value.filter(r => r.status === 'draft')
  if (draftRows.length === 0) {
    ElMessage.warning('选中的需求中没有草稿状态的记录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要批量提交选中的 ${draftRows.length} 条草稿需求吗？`,
      '批量提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await Promise.all(draftRows.map(row => materialRequestApi.submit(row.requestId)))
    ElMessage.success(`批量提交成功，共 ${draftRows.length} 条`)
    selectedRows.value = []
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '批量提交失败')
    }
  }
}

// 查看详情
async function handleView(row: MaterialRequestInfo) {
  try {
    const detail = await materialRequestApi.getById(row.requestId)
    detailData.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

// 审核通过
async function handleApprove(row: MaterialRequestInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要审核通过需求「${row.title}」吗？`,
      '审核确认',
      {
        confirmButtonText: '通过',
        cancelButtonText: '取消',
        type: 'success',
      },
    )
    await materialRequestApi.approve(row.requestId)
    ElMessage.success('审核通过')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// 审核驳回
async function handleReject(row: MaterialRequestInfo) {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入驳回原因`,
      '驳回确认',
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '请输入驳回原因',
        inputValidator: (value) => {
          if (!value || value.trim().length === 0) {
            return '请输入驳回原因'
          }
          return true
        },
      },
    )
    await materialRequestApi.reject(row.requestId, value)
    ElMessage.success('已驳回')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// 删除
async function handleDelete(row: MaterialRequestInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要删除需求「${row.title}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await materialRequestApi.delete(row.requestId)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

// 获取优先级标签
function getPriorityLabel(priority: string): string {
  const map: Record<string, string> = {
    normal: '普通',
    urgent: '紧急',
    critical: '特急',
  }
  return map[priority] || '普通'
}

// 获取优先级颜色
function getPriorityColor(priority: string): string {
  const map: Record<string, string> = {
    normal: 'info',
    urgent: 'warning',
    critical: 'error',
  }
  return map[priority] || 'info'
}

// 格式化时间
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 格式化日期
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="物资需求提报" description="管理各部门的物资需求申请">
      <el-button v-permission="'purchase:material-request:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增需求
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Clock" label="待审核" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Loading" label="审核中" :value="String(statistics.reviewing)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已审批" :value="String(statistics.approved)" color-type="success" variant="bordered" />
      <StatCard icon="Document" label="我的申请" :value="String(statistics.myRequest)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.requestNo"
            placeholder="需求单号"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Document /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.createByName"
            placeholder="申请人"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-input
            v-model="queryForm.storeName"
            placeholder="申请部门"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">
            <el-icon :size="14"><Search /></el-icon>查询
          </el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            v-permission="'purchase:material-request:edit'"
            type="success"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchSubmit"
          >
            批量提交
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
        <!-- 需求单号列 -->
        <template #requestNo="{ row }">
          <span class="request-no-text">{{ row.requestNo }}</span>
        </template>

        <!-- 需求标题列 -->
        <template #title="{ row }">
          <span class="title-text">{{ row.title }}</span>
        </template>

        <!-- 申请人列 -->
        <template #createByName="{ row }">
          <span class="applicant-text">{{ row.createByName || '-' }}</span>
        </template>

        <!-- 申请部门列 -->
        <template #storeName="{ row }">
          <span class="dept-text">{{ row.storeName }}</span>
        </template>

        <!-- 需求日期列 -->
        <template #expectedDate="{ row }">
          <span class="date-text">{{ formatDate(row.expectedDate) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="materialRequestConverter.toStatusTagStatus(row.status)"
            :label="materialRequestConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 优先级列 -->
        <template #priority="{ row }">
          <StatusTag
            :status="getPriorityColor('normal')"
            :label="getPriorityLabel('normal')"
            size="small"
            variant="light"
          />
        </template>

        <!-- 物资种类数列 -->
        <template #itemCount="{ row }">
          <span class="count-text">{{ row.items?.length || 0 }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button v-permission="'purchase:material-request:view'" link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:material-request:edit'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:material-request:edit'"
              link
              type="success"
              size="default"
              @click.stop="handleSubmitApproval(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'purchase:material-request:edit'"
              link
              type="success"
              size="default"
              @click.stop="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'purchase:material-request:edit'"
              link
              type="danger"
              size="default"
              @click.stop="handleReject(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:material-request:delete'"
              link
              type="danger"
              size="default"
              @click.stop="handleDelete(row)"
            >
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
      :title="isEdit ? '编辑物资需求' : '新增物资需求'"
      width="1200px"
      class="fts-dialog--xl"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="需求标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入需求标题" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请部门" prop="storeName">
              <el-select v-model="formData.storeName" placeholder="请选择申请部门" :teleported="false" style="width: 100%">
                <el-option v-for="dept in departmentOptions" :key="dept.id" :label="dept.name" :value="dept.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-radio-group v-model="formData.priority">
                <el-radio value="normal">普通</el-radio>
                <el-radio value="urgent">紧急</el-radio>
                <el-radio value="critical">特急</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望到货日期" prop="expectedDate">
              <el-date-picker
                v-model="formData.expectedDate"
                type="date"
                placeholder="请选择期望到货日期"
                value-format="YYYY-MM-DD"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 需求明细 -->
        <div class="form-section">
          <div class="section-title">
            <span>需求明细</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addItem">
              添加明细
            </el-button>
          </div>

          <el-table :data="formData.items" size="small" border style="width: 100%">
            <el-table-column label="物资名称" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.materialName" placeholder="请输入物资名称" />
              </template>
            </el-table-column>
            <el-table-column label="规格" width="120">
              <template #default="{ row }">
                <el-input v-model="row.specification" placeholder="规格" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80">
              <template #default="{ row }">
                <el-input v-model="row.unit" placeholder="单位" />
              </template>
            </el-table-column>
            <el-table-column label="需求数量" width="110" align="center">
              <template #default="{ row, $index }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="99999"
                  controls-position="right"
                  size="small"
                  style="width: 90px"
                  @change="() => recalcSubtotal($index)"
                />
              </template>
            </el-table-column>
            <el-table-column label="用途/备注" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="用途或备注" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeItem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!formData.items.length" class="empty-tip">
            <el-text type="info">暂未添加明细，点击上方"添加明细"按钮开始添加</el-text>
          </div>
        </div>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" show-word-limit />
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
      title="物资需求详情"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="需求单号">{{ detailData.requestNo }}</el-descriptions-item>
          <el-descriptions-item label="需求标题">{{ detailData.title }}</el-descriptions-item>
          <el-descriptions-item label="申请部门">{{ detailData.storeName }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detailData.createByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望到货日期">{{ formatDate(detailData.expectedDate) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="materialRequestConverter.toStatusTagStatus(detailData.status)"
              :label="materialRequestConverter.toStatusLabel(detailData.status)"
              size="small"
              variant="light"
            />
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detailData.updateTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.convertedRequestNo" label="采购申请单号">
            {{ detailData.convertedRequestNo }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">需求明细</el-divider>
        <el-table :data="detailData.items" stripe size="small">
          <el-table-column prop="materialName" label="物资名称" min-width="140" />
          <el-table-column prop="specification" label="规格" width="100" />
          <el-table-column prop="unit" label="单位" width="70" />
          <el-table-column prop="quantity" label="需求数量" width="90" align="center" />
          <el-table-column prop="estimatedPrice" label="预估单价(元)" width="110" align="center">
            <template #default="{ row }">
              ¥{{ (row.estimatedPrice ?? 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="subtotalAmount" label="小计(元)" width="110" align="center">
            <template #default="{ row }">
              ¥{{ ((row.quantity ?? 0) * (row.estimatedPrice ?? 0)).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="120" />
        </el-table>
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 需求单号
.request-no-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-primary);
}

// 需求标题
.title-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 申请人
.applicant-text {
  color: var(--fts-text-primary);
}

// 申请部门
.dept-text {
  color: var(--fts-text-primary);
}

// 日期
.date-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

// 数量
.count-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
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

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--fts-space-6) 0;
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
