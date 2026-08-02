<script setup lang="ts">
/**
 * 抽检管理页面 - 管理食品安全抽检和监督检查记录
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成抽检管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, DocumentChecked, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { inspectionApi } from '@/api/traceability'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  InspectionRecord,
  InspectionFormData,
  InspectionQuery,
  InspectionStatistics,
  InspectionResult,
  InspectionType,
  InspectionItem,
} from '@/types/traceability'
import { InspectionResultMap, InspectionTypeMap } from '@/types/traceability'

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
const selectedRows = ref<InspectionRecord[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const currentDetail = ref<InspectionRecord | null>(null)
const detailLoading = ref(false)
const exportLoading = ref(false)
const statistics = ref<InspectionStatistics | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  inspectionType: '' as '' | InspectionType,
  inspectionResult: '' as '' | InspectionResult,
  inspectionTimeStart: '',
  inspectionTimeEnd: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<InspectionRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: InspectionQuery = {
        page: params.page,
        size: params.size,
        keyword: params.keyword || undefined,
        inspectionType: params.inspectionType || undefined,
        inspectionResult: params.inspectionResult || undefined,
        inspectionTimeStart: params.inspectionTimeStart || undefined,
        inspectionTimeEnd: params.inspectionTimeEnd || undefined,
      }
      return inspectionApi.getList(queryParams)
    },
  } as unknown as CrudApi<InspectionRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 抽检产品列表项
interface InspectionProductItem {
  materialId: string
  materialName: string
  batchNo: string
  quantity: number
  sampleQuantity: number
}

// 抽检项目项
interface InspectionItemForm {
  itemName: string
  standard: string
  value: string
  unit: string
  result: 'QUALIFIED' | 'UNQUALIFIED'
}

// 表单数据
const formData = reactive<Partial<InspectionFormData> & {
  products: InspectionProductItem[]
  inspectionItemsList: InspectionItemForm[]
}>({
  traceCodeId: 0,
  traceCode: '',
  batchNo: '',
  supplierId: undefined,
  supplierName: '',
  materialId: '',
  materialName: '',
  inspectionType: 'INCOMING',
  inspectionResult: 'QUALIFIED',
  inspectionItem: '',
  inspectionValue: '',
  standardValue: '',
  inspectionUnit: '',
  inspectorId: undefined,
  inspectorName: '',
  inspectionTime: '',
  inspectionLocation: '',
  reportUrl: '',
  remark: '',
  products: [],
  inspectionItemsList: [],
})

// 加载统计数据
async function loadStatistics(): Promise<void> {
  try {
    statistics.value = await inspectionApi.getStatistics()
  } catch {
    statistics.value = null
  }
}

// 统计卡片数据
const statsCards = computed(() => [
  {
    key: 'pending',
    icon: 'Clock',
    label: '待抽检',
    value: '0',
    colorType: 'warning' as const,
  },
  {
    key: 'inProgress',
    icon: 'Loading',
    label: '抽检中',
    value: '0',
    colorType: 'primary' as const,
  },
  {
    key: 'completed',
    icon: 'CircleCheck',
    label: '已完成',
    value: String(statistics.value?.total ?? 0),
    colorType: 'success' as const,
  },
  {
    key: 'unqualified',
    icon: 'CircleCloseFilled',
    label: '不合格数',
    value: String(statistics.value?.unqualifiedCount ?? 0),
    colorType: 'error' as const,
  },
])

// 抽检类型选项（用户要求：日常抽检、专项抽检、监督抽检）
// 映射到后端的 INCOMING/PROCESS/FINAL
const inspectionTypeOptions = [
  { value: 'INCOMING', label: '日常抽检' },
  { value: 'PROCESS', label: '专项抽检' },
  { value: 'FINAL', label: '监督抽检' },
]

// 抽检结果选项
const inspectionResultOptions = [
  { value: 'QUALIFIED', label: '合格' },
  { value: 'UNQUALIFIED', label: '不合格' },
  { value: 'CONDITIONAL', label: '有条件合格' },
]

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'inspectionNo', label: '抽检单号', minWidth: 160, slot: 'inspectionNo' },
  { prop: 'inspectionType', label: '抽检类型', minWidth: 110, slot: 'inspectionType', ellipsis: false },
  { prop: 'productBatch', label: '产品/批次', minWidth: 180, slot: 'productBatch' },
  { prop: 'supplierName', label: '抽检机构', minWidth: 140, slot: 'supplierName' },
  { prop: 'inspectionResult', label: '抽检结果', minWidth: 110, slot: 'inspectionResult', ellipsis: false },
  { prop: 'inspectionTime', label: '抽检日期', minWidth: 160, slot: 'inspectionTime' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  inspectionType: [{ required: true, message: '请选择抽检类型', trigger: 'change' }],
  inspectionResult: [{ required: true, message: '请选择抽检结果', trigger: 'change' }],
  inspectorName: [{ required: true, message: '请输入抽检人员姓名', trigger: 'blur' }],
  inspectionTime: [{ required: true, message: '请选择抽检日期', trigger: 'change' }],
}

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
  loadStatistics()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.inspectionType = ''
  queryForm.value.inspectionResult = ''
  queryForm.value.inspectionTimeStart = ''
  queryForm.value.inspectionTimeEnd = ''
  pagination.current = 1
  refresh()
  loadStatistics()
}

function handleSelectionChange(rows: InspectionRecord[]) {
  selectedRows.value = rows
}

/** 添加抽检产品行 */
function addProduct(): void {
  formData.products.push({
    materialId: '',
    materialName: '',
    batchNo: '',
    quantity: 0,
    sampleQuantity: 0,
  })
}

/** 删除抽检产品行 */
function removeProduct(index: number): void {
  formData.products.splice(index, 1)
}

/** 添加抽检项目行 */
function addInspectionItem(): void {
  formData.inspectionItemsList.push({
    itemName: '',
    standard: '',
    value: '',
    unit: '',
    result: 'QUALIFIED',
  })
}

/** 删除抽检项目行 */
function removeInspectionItem(index: number): void {
  formData.inspectionItemsList.splice(index, 1)
}

/** 重置表单 */
function resetForm(): void {
  Object.assign(formData, {
    traceCodeId: 0,
    traceCode: '',
    batchNo: '',
    supplierId: undefined,
    supplierName: '',
    materialId: '',
    materialName: '',
    inspectionType: 'INCOMING',
    inspectionResult: 'QUALIFIED',
    inspectionItem: '',
    inspectionValue: '',
    standardValue: '',
    inspectionUnit: '',
    inspectorId: undefined,
    inspectorName: '',
    inspectionTime: '',
    inspectionLocation: '',
    reportUrl: '',
    remark: '',
    products: [],
    inspectionItemsList: [],
  })
}

function handleCreate() {
  isEdit.value = false
  resetForm()
  addProduct()
  addInspectionItem()
  dialogVisible.value = true
}

async function handleEdit(row: InspectionRecord) {
  isEdit.value = true
  resetForm()
  try {
    const detail = await inspectionApi.getById(row.inspectionId)
    Object.assign(formData, {
      ...detail,
      products: detail.materialName ? [{
        materialId: detail.materialId || '',
        materialName: detail.materialName || '',
        batchNo: detail.batchNo || '',
        quantity: 0,
        sampleQuantity: 0,
      }] : [],
      inspectionItemsList: parseInspectionItems(detail.inspectionItems),
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载抽检详情失败')
  }
}

/** 解析检验项目JSON字符串 */
function parseInspectionItems(jsonStr?: string): InspectionItemForm[] {
  if (!jsonStr) return []
  try {
    const items = JSON.parse(jsonStr) as InspectionItem[]
    return items.map(item => ({
      itemName: item.itemName || '',
      standard: item.standard || '',
      value: item.value || '',
      unit: item.unit || '',
      result: item.result || 'QUALIFIED',
    }))
  } catch {
    return []
  }
}

/** 序列化检验项目为JSON字符串 */
function serializeInspectionItems(items: InspectionItemForm[]): string {
  return JSON.stringify(items.map(item => ({
    itemName: item.itemName,
    standard: item.standard,
    value: item.value,
    unit: item.unit,
    result: item.result,
  })))
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: InspectionFormData = {
      traceCodeId: formData.traceCodeId || 0,
      traceCode: formData.traceCode,
      batchNo: formData.products[0]?.batchNo || formData.batchNo,
      supplierId: formData.supplierId,
      supplierName: formData.supplierName,
      materialId: formData.products[0]?.materialId || formData.materialId,
      materialName: formData.products[0]?.materialName || formData.materialName,
      inspectionType: formData.inspectionType as InspectionType,
      inspectionResult: formData.inspectionResult as InspectionResult,
      inspectionItem: formData.inspectionItem,
      inspectionValue: formData.inspectionValue,
      standardValue: formData.standardValue,
      inspectionUnit: formData.inspectionUnit,
      inspectorId: formData.inspectorId,
      inspectorName: formData.inspectorName,
      inspectionTime: formData.inspectionTime,
      inspectionLocation: formData.inspectionLocation,
      reportUrl: formData.reportUrl,
      inspectionItems: formData.inspectionItemsList.length > 0
        ? serializeInspectionItems(formData.inspectionItemsList)
        : undefined,
      remark: formData.remark,
    }

    if (isEdit.value && formData.inspectionId) {
      await inspectionApi.update(formData.inspectionId, submitData)
      ElMessage.success('更新成功')
    } else {
      await inspectionApi.create(submitData)
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

/** 删除抽检记录 */
async function handleDelete(row: InspectionRecord): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除抽检记录「${row.inspectionNo}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await inspectionApi.delete(row.inspectionId)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/** 查看详情 */
async function handleViewDetail(row: InspectionRecord): Promise<void> {
  detailLoading.value = true
  detailVisible.value = true
  currentDetail.value = null
  try {
    currentDetail.value = await inspectionApi.getById(row.inspectionId)
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

/** 导出报告 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出报告，请稍候...')

    const params: InspectionQuery = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: queryForm.value.keyword || undefined,
      inspectionType: queryForm.value.inspectionType || undefined,
      inspectionResult: queryForm.value.inspectionResult || undefined,
    }

    const res = await inspectionApi.getList(params)
    const records = res.records || []

    const csvContent = generateCsvContent(records)
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `抽检报告_${timestamp}.csv`

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

/** 生成CSV内容 */
function generateCsvContent(records: InspectionRecord[]): string {
  const headers = ['抽检单号', '抽检类型', '产品名称', '批次号', '供应商', '抽检结果', '抽检人员', '抽检时间', '备注']
  const rows = records.map(r => [
    r.inspectionNo,
    getInspectionTypeLabel(r.inspectionType),
    r.materialName,
    r.batchNo,
    r.supplierName,
    getResultLabel(r.inspectionResult),
    r.inspectorName,
    formatDateTime(r.inspectionTime),
    r.remark,
  ])
  return [headers, ...rows].map(row => row.map(cell => `"${cell || ''}"`).join(',')).join('\n')
}

/** 报告上传前验证 */
function beforeReportUpload(rawFile: UploadRawFile): boolean {
  const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png']
  const isAllowed = allowedTypes.includes(rawFile.type) ||
    rawFile.name.endsWith('.pdf') ||
    rawFile.name.endsWith('.jpg') ||
    rawFile.name.endsWith('.jpeg') ||
    rawFile.name.endsWith('.png')

  if (!isAllowed) {
    ElMessage.error('只支持 PDF、JPG、PNG 格式的文件')
    return false
  }

  const isLt10M = rawFile.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }

  return true
}

/** 处理报告上传 */
function handleReportUpload(uploadFile: UploadFile): void {
  if (uploadFile.raw) {
    formData.reportUrl = uploadFile.name
    ElMessage.success('报告上传成功')
  }
}

// ==================== 辅助方法 ====================

/** 获取抽检类型标签 */
function getInspectionTypeLabel(type?: InspectionType): string {
  if (!type) return '-'
  const map: Record<string, string> = {
    INCOMING: '日常抽检',
    PROCESS: '专项抽检',
    FINAL: '监督抽检',
  }
  return map[type] || type
}

/** 获取抽检类型StatusTag status */
function getInspectionTypeTagStatus(type?: InspectionType): string {
  if (!type) return 'info'
  const map: Record<string, string> = {
    INCOMING: 'info',
    PROCESS: 'warning',
    FINAL: 'primary',
  }
  return map[type] || 'info'
}

/** 获取抽检结果标签 */
function getResultLabel(result?: InspectionResult): string {
  if (!result) return '-'
  return InspectionResultMap[result]?.label || result
}

/** 获取抽检结果StatusTag status */
function getResultTagStatus(result?: InspectionResult): string {
  if (!result) return 'info'
  const map: Record<string, string> = {
    QUALIFIED: 'success',
    UNQUALIFIED: 'error',
    CONDITIONAL: 'warning',
  }
  return map[result] || 'info'
}

/** 获取单项结果标签 */
function getItemResultLabel(result: 'QUALIFIED' | 'UNQUALIFIED'): string {
  return result === 'QUALIFIED' ? '合格' : '不合格'
}

/** 获取单项结果标签状态 */
function getItemResultStatus(result: 'QUALIFIED' | 'UNQUALIFIED'): string {
  return result === 'QUALIFIED' ? 'success' : 'error'
}

/** 格式化日期时间 */
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const normalized = dateStr.replace('T', ' ')
  return normalized.split('.')[0].slice(0, 19)
}

/** 解析检验项目详情 */
function getDetailItems(jsonStr?: string): InspectionItem[] {
  if (!jsonStr) return []
  try {
    return JSON.parse(jsonStr) as InspectionItem[]
  } catch {
    return []
  }
}

// 页面挂载时加载统计数据
onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="检验记录" description="管理食品安全抽检和监督检查记录">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增抽检计划
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索抽检单号..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.inspectionType"
            placeholder="抽检类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in inspectionTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.inspectionResult"
            placeholder="抽检结果"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in inspectionResultOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.inspectionTimeStart"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
          <span class="date-range-separator">至</span>
          <el-date-picker
            v-model="queryForm.inspectionTimeEnd"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
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
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出报告
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
        <!-- 抽检单号列 -->
        <template #inspectionNo="{ row }">
          <span class="inspection-no">{{ row.inspectionNo }}</span>
        </template>

        <!-- 抽检类型列 -->
        <template #inspectionType="{ row }">
          <StatusTag
            :status="getInspectionTypeTagStatus(row.inspectionType)"
            :label="getInspectionTypeLabel(row.inspectionType)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 产品/批次列 -->
        <template #productBatch="{ row }">
          <div class="product-batch-cell">
            <div class="product-name">{{ row.materialName || '-' }}</div>
            <div class="batch-no">{{ row.batchNo || '-' }}</div>
          </div>
        </template>

        <!-- 抽检机构列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName || '-' }}</span>
        </template>

        <!-- 抽检结果列 -->
        <template #inspectionResult="{ row }">
          <StatusTag
            :status="getResultTagStatus(row.inspectionResult)"
            :label="getResultLabel(row.inspectionResult)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 抽检日期列 -->
        <template #inspectionTime="{ row }">
          <span class="time-text">{{ formatDateTime(row.inspectionTime) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
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
          @size-change="() => { pagination.current = 1; refresh() }"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑抽检计划' : '新增抽检计划'"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="抽检类型" prop="inspectionType">
              <el-select
                v-model="formData.inspectionType"
                placeholder="请选择抽检类型"
                style="width: 100%"
                :teleported="false"
              >
                <el-option
                  v-for="opt in inspectionTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="抽检计划名称">
              <el-input v-model="formData.inspectionItem" placeholder="请输入抽检计划名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="抽检机构">
              <el-input v-model="formData.supplierName" placeholder="请输入抽检机构" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="抽检人员" prop="inspectorName">
              <el-input v-model="formData.inspectorName" placeholder="请输入抽检人员姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="抽检日期" prop="inspectionTime">
              <el-date-picker
                v-model="formData.inspectionTime"
                type="datetime"
                placeholder="选择抽检日期"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="抽检结果" prop="inspectionResult">
              <el-select
                v-model="formData.inspectionResult"
                placeholder="请选择抽检结果"
                style="width: 100%"
                :teleported="false"
              >
                <el-option
                  v-for="opt in inspectionResultOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 抽检产品列表 -->
        <div class="form-section">
          <div class="section-title">
            <span>抽检产品列表</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addProduct">
              添加产品
            </el-button>
          </div>
          <el-table :data="formData.products" size="small" border style="width: 100%">
            <el-table-column label="产品名称" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.materialName" placeholder="产品名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="批次号" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.batchNo" placeholder="批次号" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="抽检数量" width="120" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.sampleQuantity"
                  :min="0"
                  controls-position="right"
                  size="small"
                  style="width: 100px"
                />
              </template>
            </el-table-column>
            <el-table-column label="" width="50" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeProduct($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!formData.products.length" class="empty-tip">
            <el-text type="info">暂未添加产品，点击上方"添加产品"按钮开始配置</el-text>
          </div>
        </div>

        <!-- 抽检项目 -->
        <div class="form-section">
          <div class="section-title">
            <span>抽检项目</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addInspectionItem">
              添加项目
            </el-button>
          </div>
          <el-table :data="formData.inspectionItemsList" size="small" border style="width: 100%">
            <el-table-column label="项目名称" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.itemName" placeholder="项目名称" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="标准值" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.standard" placeholder="标准值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="实测值" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.value" placeholder="实测值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80">
              <template #default="{ row }">
                <el-input v-model="row.unit" placeholder="单位" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="结果" width="100" align="center">
              <template #default="{ row }">
                <el-select
                  v-model="row.result"
                  size="small"
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option label="合格" value="QUALIFIED" />
                  <el-option label="不合格" value="UNQUALIFIED" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="" width="50" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeInspectionItem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!formData.inspectionItemsList.length" class="empty-tip">
            <el-text type="info">暂未添加抽检项目，点击上方"添加项目"按钮开始配置</el-text>
          </div>
        </div>

        <!-- 报告上传 -->
        <el-form-item label="报告上传">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeReportUpload"
            :http-request="handleReportUpload"
            accept=".pdf,.jpg,.jpeg,.png"
          >
            <el-button size="default">
              <el-icon :size="14"><DocumentChecked /></el-icon>上传报告
            </el-button>
          </el-upload>
          <span v-if="formData.reportUrl" class="report-name">{{ formData.reportUrl }}</span>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="抽检详情"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="detailLoading" element-loading-text="正在加载详情...">
        <div v-if="currentDetail">
          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="detail-section-title">基本信息</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="抽检单号">{{ currentDetail.inspectionNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抽检类型">
                <StatusTag
                  :status="getInspectionTypeTagStatus(currentDetail.inspectionType)"
                  :label="getInspectionTypeLabel(currentDetail.inspectionType)"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="产品名称">{{ currentDetail.materialName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="批次号">{{ currentDetail.batchNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抽检机构">{{ currentDetail.supplierName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抽检人员">{{ currentDetail.inspectorName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抽检日期">{{ formatDateTime(currentDetail.inspectionTime) }}</el-descriptions-item>
              <el-descriptions-item label="抽检结果">
                <StatusTag
                  :status="getResultTagStatus(currentDetail.inspectionResult)"
                  :label="getResultLabel(currentDetail.inspectionResult)"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="追溯码">{{ currentDetail.traceCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抽检地点">{{ currentDetail.inspectionLocation || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 抽检产品明细 -->
          <div class="detail-section">
            <div class="detail-section-title">抽检产品明细</div>
            <el-table :data="[{
              materialName: currentDetail.materialName,
              batchNo: currentDetail.batchNo,
            }]" size="small" border>
              <el-table-column prop="materialName" label="产品名称" />
              <el-table-column prop="batchNo" label="批次号" />
            </el-table>
          </div>

          <!-- 检测项目结果 -->
          <div class="detail-section">
            <div class="detail-section-title">检测项目结果</div>
            <el-table
              v-if="getDetailItems(currentDetail.inspectionItems).length > 0"
              :data="getDetailItems(currentDetail.inspectionItems)"
              size="small"
              border
            >
              <el-table-column prop="itemName" label="项目名称" min-width="120" />
              <el-table-column prop="standard" label="标准值" min-width="100" />
              <el-table-column prop="value" label="实测值" min-width="100" />
              <el-table-column prop="unit" label="单位" width="80" />
              <el-table-column label="结果" width="90" align="center">
                <template #default="{ row }">
                  <StatusTag
                    :status="getItemResultStatus(row.result)"
                    :label="getItemResultLabel(row.result)"
                    size="small"
                  />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无检测项目数据" :image-size="80" />
          </div>

          <!-- 抽检报告 -->
          <div class="detail-section">
            <div class="detail-section-title">抽检报告</div>
            <div v-if="currentDetail.reportUrl" class="report-link">
              <el-icon><DocumentChecked /></el-icon>
              <span>{{ currentDetail.reportUrl }}</span>
              <el-button link type="primary" size="small">下载</el-button>
            </div>
            <el-empty v-else description="暂无报告" :image-size="80" />
          </div>

          <!-- 处理措施/备注 -->
          <div class="detail-section">
            <div class="detail-section-title">处理措施 / 备注</div>
            <div class="remark-content">{{ currentDetail.remark || '暂无' }}</div>
          </div>
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

// 抽检单号
.inspection-no {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

// 产品/批次单元格
.product-batch-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .product-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  .batch-no {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

// 供应商文字
.supplier-text {
  color: var(--fts-text-primary);
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
}

// 表单区块
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

.report-name {
  margin-left: var(--fts-space-3);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

// 详情区块
.detail-section {
  margin-bottom: var(--fts-space-4);

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-section-title {
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
  line-height: 1.2;
}

.report-link {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  color: var(--fts-text-primary);

  .el-icon {
    color: var(--fts-primary);
  }
}

.remark-content {
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  color: var(--fts-text-primary);
  line-height: 1.6;
  white-space: pre-wrap;
  min-height: 60px;
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
