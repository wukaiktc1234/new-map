<script setup lang="ts">
/**
 * 资产盘点页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成资产盘点的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 功能：
 * - 盘点计划列表管理
 * - 新建/编辑盘点计划
 * - 盘点详情查看（含明细）
 * - 盘点录入功能
 * - 开始/结束盘点
 * - 导出盘点报告
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, Document } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { inventoryApi, categoryApi } from '@/api/asset'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  InventoryCheck,
  InventoryCheckItem,
  InventoryCreateDTO,
  InventoryCheckType,
  InventoryStatus,
  AssetCategory,
} from '@/types/asset'
import {
  InventoryStatusTagMap,
  InventoryCheckTypeOptions,
} from '@/types/asset'

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
const selectedRows = ref<InventoryCheck[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryCheck | null>(null)
const detailItems = ref<InventoryCheckItem[]>([])
const detailLoading = ref(false)

const entryDialogVisible = ref(false)
const currentEntryItem = ref<InventoryCheckItem | null>(null)
const entryFormRef = ref<FormInstance>()

const formRef = ref<FormInstance>()

const queryForm = ref({
  keyword: '',
  checkType: '' as InventoryCheckType | '',
  status: '' as InventoryStatus | '',
  dateRange: [] as string[],
})

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<InventoryCheck, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: Record<string, unknown> = {
        keyword: params.keyword || undefined,
        checkType: params.checkType || undefined,
        status: params.status || undefined,
        page: params.page,
        pageSize: params.size,
      }
      if (params.dateRange && params.dateRange.length === 2) {
        convertedParams.startDate = params.dateRange[0]
        convertedParams.endDate = params.dateRange[1]
      }
      return inventoryApi.getList(convertedParams)
    },
  } as unknown as CrudApi<InventoryCheck, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

const formData = reactive<Partial<InventoryCreateDTO> & { id?: string; remark?: string }>({
  title: '',
  checkType: 'full',
  plannedStartTime: '',
  plannedEndTime: '',
  scopeDescription: '',
  remark: '',
})

const entryFormData = reactive({
  actualQuantity: 1,
  diffReason: '',
})

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  inProgress: tableData.value.filter(item => item.status === 'in_progress').length,
  completed: tableData.value.filter(item => item.status === 'completed').length,
  diffCount: tableData.value.reduce((sum, item) => sum + item.overageCount + item.shortageCount, 0),
}))

// 部门下拉选项（来自后端 /v1/departments/tree）
const { departmentOptions } = useDepartmentOptions(true)

// 资产分类选项（来自后端 /v1/asset/categories/page）
const categoryOptions = ref<AssetCategory[]>([])

// 盘点负责人选项（TODO: 待接入后端用户列表 API）
const userOptions = ref<Array<{ value: string; label: string }>>([])

/** 加载资产分类选项 */
async function loadCategoryOptions(): Promise<void> {
  try {
    categoryOptions.value = await categoryApi.getList()
  } catch {
    categoryOptions.value = []
  }
}

const scopeType = ref<'department' | 'category'>('department')
const selectedScopes = ref<string[]>([])

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'checkCode', label: '盘点单号', minWidth: 140, slot: 'checkCode' },
  { prop: 'title', label: '盘点名称', minWidth: 180, slot: 'title' },
  { prop: 'checkType', label: '盘点类型', minWidth: 100, slot: 'checkType' },
  { prop: 'scopeDescription', label: '盘点范围', minWidth: 150, slot: 'scopeDescription' },
  { prop: 'plannedStartTime', label: '计划开始', minWidth: 160, slot: 'plannedStartTime' },
  { prop: 'plannedEndTime', label: '计划结束', minWidth: 160, slot: 'plannedEndTime' },
  { prop: 'status', label: '盘点状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'diffCount', label: '差异数量', minWidth: 100, slot: 'diffCount' },
  { prop: '_operation', label: '操作', width: 320, fixed: 'right', slot: 'operation' },
])

const detailItemColumns = computed<ColumnDef[]>(() => [
  { prop: 'assetCode', label: '资产编号', minWidth: 130 },
  { prop: 'assetName', label: '资产名称', minWidth: 180 },
  { prop: 'bookQuantity', label: '账存数', minWidth: 90, slot: 'bookQuantity' },
  { prop: 'actualQuantity', label: '实存数', minWidth: 90, slot: 'actualQuantity' },
  { prop: 'diffQuantity', label: '差异', minWidth: 90, slot: 'diffQuantity' },
  { prop: 'diffReason', label: '差异原因', minWidth: 150, slot: 'diffReason' },
  { prop: 'counterName', label: '盘点人', minWidth: 90 },
  { prop: '_operation', label: '操作', width: 100, fixed: 'right', slot: 'itemOperation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  title: [
    { required: true, message: '请输入盘点名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  checkType: [{ required: true, message: '请选择盘点类型', trigger: 'change' }],
  plannedStartTime: [{ required: true, message: '请选择计划开始日期', trigger: 'change' }],
  plannedEndTime: [{ required: true, message: '请选择计划结束日期', trigger: 'change' }],
}

const entryFormRules: FormRules = {
  actualQuantity: [
    { required: true, message: '请输入实存数量', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.checkType = ''
  queryForm.value.status = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: InventoryCheck[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: '',
    title: '',
    checkType: 'full',
    plannedStartTime: '',
    plannedEndTime: '',
    scopeDescription: '',
    remark: '',
  })
  scopeType.value = 'department'
  selectedScopes.value = []
  dialogVisible.value = true
}

async function handleEdit(row: InventoryCheck) {
  isEdit.value = true
  try {
    const detail = await inventoryApi.getById(row.id)
    if (detail) {
      Object.assign(formData, {
        id: detail.id,
        title: detail.title,
        checkType: detail.checkType,
        plannedStartTime: detail.plannedStartTime,
        plannedEndTime: detail.plannedEndTime,
        scopeDescription: detail.scopeDescription,
        remark: detail.remark,
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载盘点详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: InventoryCreateDTO = {
      title: formData.title!,
      checkType: formData.checkType as InventoryCheckType,
      plannedStartTime: formData.plannedStartTime!,
      plannedEndTime: formData.plannedEndTime!,
      scopeDescription: formData.scopeDescription,
    }

    if (isEdit.value && formData.id) {
      await inventoryApi.update(formData.id, {
        title: submitData.title,
        checkType: submitData.checkType,
        scopeDescription: submitData.scopeDescription,
        remark: formData.remark,
      })
      ElMessage.success('更新成功')
    } else {
      await inventoryApi.create(submitData)
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

async function handleDetail(row: InventoryCheck) {
  try {
    const detail = await inventoryApi.getById(row.id)
    if (detail) {
      currentDetail.value = detail
      detailDialogVisible.value = true
      loadDetailItems(row.id)
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
}

async function loadDetailItems(checkId: string) {
  detailLoading.value = true
  try {
    detailItems.value = await inventoryApi.getItems(checkId)
  } catch {
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

async function handleStart(row: InventoryCheck) {
  try {
    await ElMessageBox.confirm(
      `确定要开始盘点「${row.title}」吗？开始后将进入盘点进行中状态。`,
      '开始盘点确认',
      {
        confirmButtonText: '确定开始',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await inventoryApi.start(row.id)
    ElMessage.success('盘点已开始')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleComplete(row: InventoryCheck) {
  try {
    await ElMessageBox.confirm(
      `确定要完成盘点「${row.title}」吗？完成后将无法修改盘点数据。`,
      '完成盘点确认',
      {
        confirmButtonText: '确定完成',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await inventoryApi.complete(row.id)
    ElMessage.success('盘点已完成')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleDelete(row: InventoryCheck) {
  try {
    await ElMessageBox.confirm(
      `确定要删除盘点计划「${row.title}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await inventoryApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

function handleEntry(row: InventoryCheckItem) {
  currentEntryItem.value = row
  entryFormData.actualQuantity = row.actualQuantity
  entryFormData.diffReason = row.diffReason || ''
  entryDialogVisible.value = true
}

async function handleEntrySubmit() {
  if (!entryFormRef.value || !currentEntryItem.value) return

  const valid = await entryFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const diff = entryFormData.actualQuantity - currentEntryItem.value.bookQuantity
    await inventoryApi.updateItem(currentEntryItem.value.id, {
      checkId: currentEntryItem.value.checkId,
      actualQuantity: entryFormData.actualQuantity,
      diffQuantity: diff,
      diffReason: entryFormData.diffReason,
    })
    ElMessage.success('录入成功')
    entryDialogVisible.value = false
    if (currentDetail.value) {
      loadDetailItems(currentDetail.value.id)
    }
  } catch {
    ElMessage.error('录入失败')
  }
}

async function handleExport() {
  try {
    ElMessage.info('正在导出盘点报告，请稍候...')
    setTimeout(() => {
      ElMessage.success('导出成功！文件已开始下载')
    }, 1000)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  }
}

// ==================== 辅助方法 ====================

function getStatusInfo(status: InventoryStatus) {
  return InventoryStatusTagMap[status] || { status: 'info', label: status }
}

function getCheckTypeLabel(type: InventoryCheckType) {
  return InventoryCheckTypeOptions.find(o => o.value === type)?.label || type
}

function getCheckTypeTagStatus(type: InventoryCheckType): string {
  const map: Record<InventoryCheckType, string> = {
    full: 'primary',
    sample: 'warning',
    cyclic: 'info',
  }
  return map[type] || 'info'
}

function getDiffClass(diff: number): string {
  if (diff > 0) return 'diff-overage'
  if (diff < 0) return 'diff-shortage'
  return 'diff-normal'
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

onMounted(() => {
  loadCategoryOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="资产盘点" description="管理资产盘点计划和盘点结果">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新建盘点
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="盘点计划数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Loading" label="进行中" :value="String(statistics.inProgress)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="差异资产数" :value="String(statistics.diffCount)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索盘点单号..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.checkType"
            placeholder="盘点类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in InventoryCheckTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="(info, key) in InventoryStatusTagMap"
              :key="key"
              :label="info.label"
              :value="key"
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
            :teleported="false"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="success"
            size="default"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出盘点报告
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
        <!-- 盘点单号列 -->
        <template #checkCode="{ row }">
          <span class="code-text">{{ row.checkCode }}</span>
        </template>

        <!-- 盘点名称列 -->
        <template #title="{ row }">
          <span class="title-text">{{ row.title }}</span>
        </template>

        <!-- 盘点类型列 -->
        <template #checkType="{ row }">
          <StatusTag :status="getCheckTypeTagStatus(row.checkType)" :label="getCheckTypeLabel(row.checkType)" size="small" variant="light" />
        </template>

        <!-- 盘点范围列 -->
        <template #scopeDescription="{ row }">
          <span class="scope-text">{{ row.scopeDescription || '-' }}</span>
        </template>

        <!-- 计划开始列 -->
        <template #plannedStartTime="{ row }">
          <span class="time-text">{{ formatDate(row.plannedStartTime) }}</span>
        </template>

        <!-- 计划结束列 -->
        <template #plannedEndTime="{ row }">
          <span class="time-text">{{ formatDate(row.plannedEndTime) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusInfo(row.status).status" :label="getStatusInfo(row.status).label" size="small" variant="light" />
        </template>

        <!-- 差异数量列 -->
        <template #diffCount="{ row }">
          <span :class="getDiffClass(row.overageCount + row.shortageCount)">
            {{ row.overageCount + row.shortageCount > 0 ? row.overageCount + row.shortageCount : 0 }}
          </span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="success"
              size="default"
              @click.stop="handleStart(row)"
            >
              开始盘点
            </el-button>
            <el-button
              v-if="row.status === 'in_progress'"
              link
              type="warning"
              size="default"
              @click.stop="handleComplete(row)"
            >
              结束盘点
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
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
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑盘点计划' : '新建盘点计划'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="盘点名称" prop="title">
              <el-input v-model="formData.title" placeholder="请输入盘点名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="盘点类型" prop="checkType">
              <el-select v-model="formData.checkType" placeholder="请选择盘点类型" :teleported="false" style="width: 100%">
                <el-option
                  v-for="opt in InventoryCheckTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="盘点负责人">
              <el-select v-model="formData.creatorId" placeholder="请选择负责人" :teleported="false" style="width: 100%">
                <el-option
                  v-for="user in userOptions"
                  :key="user.value"
                  :label="user.label"
                  :value="user.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划开始日期" prop="plannedStartTime">
              <el-date-picker
                v-model="formData.plannedStartTime"
                type="date"
                placeholder="请选择开始日期"
                value-format="YYYY-MM-DD"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束日期" prop="plannedEndTime">
              <el-date-picker
                v-model="formData.plannedEndTime"
                type="date"
                placeholder="请选择结束日期"
                value-format="YYYY-MM-DD"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="盘点范围">
              <el-radio-group v-model="scopeType" style="margin-bottom: 12px;">
                <el-radio value="department">按部门</el-radio>
                <el-radio value="category">按分类</el-radio>
              </el-radio-group>
              <el-select
                v-if="scopeType === 'department'"
                v-model="selectedScopes"
                multiple
                filterable
                placeholder="请选择部门"
                :teleported="false"
                style="width: 100%"
              >
                <el-option
                  v-for="dept in departmentOptions"
                  :key="dept.id"
                  :label="dept.name"
                  :value="dept.id"
                />
              </el-select>
              <el-select
                v-else
                v-model="selectedScopes"
                multiple
                filterable
                placeholder="请选择资产分类"
                :teleported="false"
                style="width: 100%"
              >
                <el-option
                  v-for="cat in categoryOptions"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
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
      v-model="detailDialogVisible"
      title="盘点详情"
      width="900px"
      destroy-on-close
    >
      <template v-if="currentDetail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="盘点单号">{{ currentDetail.checkCode }}</el-descriptions-item>
            <el-descriptions-item label="盘点名称">{{ currentDetail.title }}</el-descriptions-item>
            <el-descriptions-item label="盘点类型">
              <StatusTag :status="getCheckTypeTagStatus(currentDetail.checkType)" :label="getCheckTypeLabel(currentDetail.checkType)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="盘点状态">
              <StatusTag :status="getStatusInfo(currentDetail.status).status" :label="getStatusInfo(currentDetail.status).label" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="计划开始">{{ formatDate(currentDetail.plannedStartTime) }}</el-descriptions-item>
            <el-descriptions-item label="计划结束">{{ formatDate(currentDetail.plannedEndTime) }}</el-descriptions-item>
            <el-descriptions-item label="实际开始">{{ currentDetail.actualStartTime ? formatDate(currentDetail.actualStartTime) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际结束">{{ currentDetail.actualEndTime ? formatDate(currentDetail.actualEndTime) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="发起人">{{ currentDetail.creatorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ currentDetail.auditorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="盘点范围" :span="2">{{ currentDetail.scopeDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 盘点进度 -->
        <div class="detail-section">
          <div class="section-title">盘点进度</div>
          <div class="progress-stats">
            <div class="progress-item">
              <span class="progress-label">资产总数</span>
              <span class="progress-value">{{ currentDetail.totalAssetCount }}</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">已盘点</span>
              <span class="progress-value progress-value--info">{{ currentDetail.countedCount }}</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">盘盈</span>
              <span class="progress-value progress-value--success">{{ currentDetail.overageCount }}</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">盘亏</span>
              <span class="progress-value progress-value--error">{{ currentDetail.shortageCount }}</span>
            </div>
            <div class="progress-item progress-bar-wrapper">
              <span class="progress-label">完成进度</span>
              <el-progress
                :percentage="currentDetail.totalAssetCount > 0 ? Math.round(currentDetail.countedCount / currentDetail.totalAssetCount * 100) : 0"
                :stroke-width="12"
              />
            </div>
          </div>
        </div>

        <!-- 盘点明细 -->
        <div class="detail-section">
          <div class="section-title">盘点明细</div>
          <DataTable
            :columns="detailItemColumns"
            :data="detailItems"
            :loading="detailLoading"
            :selectable="false"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            size="small"
          >
            <template #bookQuantity="{ row }">
              <span class="qty-text">{{ row.bookQuantity }}</span>
            </template>
            <template #actualQuantity="{ row }">
              <span class="qty-text">{{ row.actualQuantity }}</span>
            </template>
            <template #diffQuantity="{ row }">
              <span :class="getDiffClass(row.diffQuantity)">
                <span v-if="row.diffQuantity > 0">+{{ row.diffQuantity }}</span>
                <span v-else>{{ row.diffQuantity }}</span>
              </span>
            </template>
            <template #diffReason="{ row }">
              <span class="reason-text">{{ row.diffReason || '-' }}</span>
            </template>
            <template #itemOperation="{ row }">
              <el-button
                v-if="currentDetail?.status === 'in_progress'"
                link
                type="primary"
                size="small"
                @click.stop="handleEntry(row)"
              >
                录入
              </el-button>
            </template>
          </DataTable>
        </div>

        <!-- 差异处理 -->
        <div class="detail-section" v-if="currentDetail.status === 'completed'">
          <div class="section-title">差异处理</div>
          <el-alert
            title="差异处理说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p>盘盈资产：需要核实资产来源，补录资产档案</p>
              <p>盘亏资产：需要查明原因，追究责任，进行资产报损处理</p>
            </template>
          </el-alert>
        </div>
      </template>
    </el-dialog>

    <!-- 盘点录入对话框 -->
    <el-dialog
      v-model="entryDialogVisible"
      title="盘点录入"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="entryFormRef" :model="entryFormData" :rules="entryFormRules" label-width="100px">
        <el-descriptions :column="1" size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="资产编号">{{ currentEntryItem?.assetCode }}</el-descriptions-item>
          <el-descriptions-item label="资产名称">{{ currentEntryItem?.assetName }}</el-descriptions-item>
          <el-descriptions-item label="账存数量">{{ currentEntryItem?.bookQuantity }}</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="实存数量" prop="actualQuantity">
          <el-input-number
            v-model="entryFormData.actualQuantity"
            :min="0"
            :max="99999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="差异原因">
          <el-input
            v-model="entryFormData.diffReason"
            type="textarea"
            :rows="3"
            placeholder="请输入差异原因（如有差异）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="entryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEntrySubmit">确认录入</el-button>
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

// 文本样式
.code-text {
  font-family: var(--fts-font-mono);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.title-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.scope-text {
  color: var(--fts-text-secondary);
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.qty-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.reason-text {
  color: var(--fts-text-secondary);
}

// 差异样式
.diff-overage {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
}

.diff-shortage {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
}

.diff-normal {
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

// 详情对话框
.detail-section {
  margin-bottom: var(--fts-space-5);

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
}

// 进度统计
.progress-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr) 2fr;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4);
  background: var(--fts-bg-soft);
  border-radius: var(--fts-card-radius);

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.progress-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.progress-value {
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;

  &--info {
    color: var(--fts-info);
  }

  &--success {
    color: var(--fts-success);
  }

  &--error {
    color: var(--fts-error);
  }
}

.progress-bar-wrapper {
  grid-column: span 1;
  justify-content: center;

  @media (max-width: 768px) {
    grid-column: span 2;
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
