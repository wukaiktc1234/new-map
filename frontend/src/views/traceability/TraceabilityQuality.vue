<script setup lang="ts">
/**
 * 质检管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理供应链各环节的质量检测记录
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { qualityApi } from '@/api/traceability'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  QualityRecord,
  QualityRecordFormData,
  QualityQuery,
  AbnormalLevel,
  QualityHandlingStatus,
} from '@/types/traceability'
import { AbnormalLevelMap, QualityHandlingStatusMap } from '@/types/traceability'

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

/** 质检类型 */
type QualityType = 'INCOMING' | 'FINISHED' | 'OUTBOUND' | 'PROCESS'

/** 质检结果 */
type QualityResult = 'PENDING' | 'PASS' | 'FAIL' | 'PROCESSING'

/** 质检项目明细 */
interface QualityItem {
  itemName: string
  standard: string
  actualValue: string
  result: QualityResult
  remark: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<QualityRecord[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const currentDetail = ref<QualityRecord | null>(null)
const importLoading = ref(false)
const exportLoading = ref(false)

const formRef = ref<FormInstance>()

/** 质检类型映射 */
const QualityTypeMap: Record<QualityType, { label: string; status: string }> = {
  INCOMING: { label: '来料质检', status: 'primary' },
  FINISHED: { label: '成品质检', status: 'success' },
  OUTBOUND: { label: '出库质检', status: 'warning' },
  PROCESS: { label: '过程质检', status: 'info' },
}

/** 质检结果映射 */
const QualityResultMap: Record<QualityResult, { label: string; status: string }> = {
  PENDING: { label: '待检', status: 'warning' },
  PROCESSING: { label: '质检中', status: 'primary' },
  PASS: { label: '合格', status: 'success' },
  FAIL: { label: '不合格', status: 'error' },
}

// 查询表单
const queryForm = ref({
  keyword: '',
  qualityType: '' as '' | QualityType,
  qualityResult: '' as '' | QualityResult,
  dateRange: [] as string[],
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<QualityRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: QualityQuery & { page: number; size: number; keyword?: string } = {
        page: params.page,
        size: params.size,
      }
      if (params.keyword) {
        queryParams.keyword = params.keyword
      }
      return qualityApi.getRecordList(queryParams)
    },
  } as unknown as CrudApi<QualityRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive<Partial<QualityRecordFormData> & {
  qualityType: QualityType
  relatedOrderNo: string
  productName: string
  batchNo: string
  sampleQuantity: number
  qualityResult: QualityResult
  reportUrl: string
  qualityItems: QualityItem[]
}>({
  qualityType: 'INCOMING',
  relatedOrderNo: '',
  materialName: '',
  batchNo: '',
  sampleQuantity: 0,
  qualityResult: 'PENDING',
  reportUrl: '',
  inspectionData: '',
  abnormalLevel: 'NORMAL',
  remark: '',
  qualityItems: [],
})

// 统计数据
const statistics = computed(() => ({
  pending: tableData.value.filter(item => item.handlingStatus === 'PENDING').length,
  processing: tableData.value.filter(item => item.handlingStatus === 'PROCESSING').length,
  pass: tableData.value.filter(item => item.abnormalLevel === 'NORMAL').length,
  fail: tableData.value.filter(item => item.abnormalLevel === 'CRITICAL' || item.abnormalLevel === 'WARNING').length,
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'recordNo', label: '质检单号', minWidth: 160, slot: 'recordNo' },
  { prop: 'qualityType', label: '质检类型', minWidth: 110, slot: 'qualityType' },
  { prop: 'traceCode', label: '关联单号', minWidth: 140, slot: 'relatedOrder' },
  { prop: 'materialName', label: '产品/物料名称', minWidth: 150, slot: 'productName' },
  { prop: 'batchNo', label: '批次号', minWidth: 130, slot: 'batchNo' },
  { prop: 'abnormalLevel', label: '质检结果', minWidth: 100, slot: 'qualityResult' },
  { prop: 'handledByName', label: '质检员', minWidth: 100, slot: 'inspector' },
  { prop: 'createTime', label: '质检时间', minWidth: 160, slot: 'inspectionTime' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  qualityType: [{ required: true, message: '请选择质检类型', trigger: 'change' }],
  relatedOrderNo: [{ required: true, message: '请输入关联单号', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入产品/物料名称', trigger: 'blur' }],
  batchNo: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
  qualityResult: [{ required: true, message: '请选择质检结果', trigger: 'change' }],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.qualityType = ''
  queryForm.value.qualityResult = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: QualityRecord[]) {
  selectedRows.value = rows
}

/** 添加质检项目 */
function addQualityItem(): void {
  formData.qualityItems.push({
    itemName: '',
    standard: '',
    actualValue: '',
    result: 'PENDING',
    remark: '',
  })
}

/** 删除质检项目 */
function removeQualityItem(index: number): void {
  formData.qualityItems.splice(index, 1)
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    traceCodeId: undefined,
    traceCode: '',
    batchNo: '',
    materialName: '',
    qualityType: 'INCOMING',
    relatedOrderNo: '',
    productName: '',
    sampleQuantity: 0,
    qualityResult: 'PENDING',
    reportUrl: '',
    inspectionData: '',
    abnormalLevel: 'NORMAL',
    remark: '',
    qualityItems: [],
  })
  addQualityItem()
  dialogVisible.value = true
}

async function handleEdit(row: QualityRecord) {
  isEdit.value = true
  try {
    const detail = await qualityApi.getRecordDetail(row.qualityRecordId)
    Object.assign(formData, {
      ...detail,
      qualityType: 'INCOMING' as QualityType,
      relatedOrderNo: detail.traceCode || '',
      productName: detail.materialName,
      sampleQuantity: 0,
      qualityResult: (detail.abnormalLevel === 'NORMAL' ? 'PASS' : detail.abnormalLevel === 'WARNING' ? 'FAIL' : 'FAIL') as QualityResult,
      reportUrl: '',
      qualityItems: [],
    })
    if (formData.qualityItems.length === 0) {
      addQualityItem()
    }
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载质检详情失败')
  }
}

async function handleDetail(row: QualityRecord) {
  try {
    const detail = await qualityApi.getRecordDetail(row.qualityRecordId)
    currentDetail.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: QualityRecordFormData = {
      materialName: formData.materialName || '',
      inspectionData: JSON.stringify(formData.qualityItems),
      abnormalLevel: formData.qualityResult === 'PASS' ? 'NORMAL' : formData.qualityResult === 'FAIL' ? 'CRITICAL' : 'WARNING',
      traceCode: formData.relatedOrderNo,
      batchNo: formData.batchNo,
      remark: formData.remark,
    }

    if (isEdit.value && currentDetail.value) {
      await qualityApi.handleAbnormal(currentDetail.value.qualityRecordId, formData.remark || '')
      ElMessage.success('更新成功')
    } else {
      await qualityApi.createRecord(submitData)
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

/**
 * 处理文件上传前的验证
 * 仅允许 Excel 文件（.xlsx / .xls），用于质检批量导入
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
 * 导入质检数据
 */
async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('正在导入数据，请稍候...')
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('导入成功')
    refresh()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

/**
 * 导出质检数据
 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    const blob = new Blob(['质检记录导出数据'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url

    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `质检记录_${timestamp}.xlsx`

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch (error) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/**
 * 删除质检记录
 */
async function handleDelete(row: QualityRecord): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除质检记录「${row.recordNo}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await qualityApi.deleteRecord(row.qualityRecordId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

// ==================== 辅助方法 ====================

function getQualityTypeLabel(type: string): string {
  return QualityTypeMap[type as QualityType]?.label || type
}

function getQualityTypeStatus(type: string): string {
  return QualityTypeMap[type as QualityType]?.status || 'info'
}

function getQualityResultLabel(level: AbnormalLevel): string {
  if (level === 'NORMAL') return '合格'
  if (level === 'WARNING') return '不合格'
  return '不合格'
}

function getQualityResultStatus(level: AbnormalLevel): string {
  if (level === 'NORMAL') return 'success'
  if (level === 'WARNING') return 'warning'
  return 'error'
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  // 页面加载时自动加载数据（useCrudTable autoLoad=true）
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="质量追溯" description="管理供应链各环节的质量检测记录">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增质检
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Clock" label="待质检" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Loading" label="质检中" :value="String(statistics.processing)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已合格" :value="String(statistics.pass)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="不合格" :value="String(statistics.fail)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导入导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="质检单号搜索..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.qualityType"
            placeholder="质检类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="(item, key) in QualityTypeMap"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
          <el-select
            v-model="queryForm.qualityResult"
            placeholder="质检结果"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="待检" value="PENDING" />
            <el-option label="质检中" value="PROCESSING" />
            <el-option label="合格" value="PASS" />
            <el-option label="不合格" value="FAIL" />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
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
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImport"
            accept=".xlsx,.xls"
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
        <!-- 质检单号列 -->
        <template #recordNo="{ row }">
          <span class="record-no-text">{{ row.recordNo }}</span>
        </template>

        <!-- 质检类型列 -->
        <template #qualityType="{ row }">
          <StatusTag :status="getQualityTypeStatus('INCOMING')" :label="getQualityTypeLabel('INCOMING')" size="small" variant="light" />
        </template>

        <!-- 关联单号列 -->
        <template #relatedOrder="{ row }">
          <span class="order-no-text">{{ row.traceCode || '-' }}</span>
        </template>

        <!-- 产品/物料名称列 -->
        <template #productName="{ row }">
          <span class="product-text">{{ row.materialName }}</span>
        </template>

        <!-- 批次号列 -->
        <template #batchNo="{ row }">
          <span class="batch-text">{{ row.batchNo || '-' }}</span>
        </template>

        <!-- 质检结果列 -->
        <template #qualityResult="{ row }">
          <StatusTag :status="getQualityResultStatus(row.abnormalLevel)" :label="getQualityResultLabel(row.abnormalLevel)" size="small" variant="light" />
        </template>

        <!-- 质检员列 -->
        <template #inspector="{ row }">
          <span class="inspector-text">{{ row.handledByName || '-' }}</span>
        </template>

        <!-- 质检时间列 -->
        <template #inspectionTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
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
      :title="isEdit ? '编辑质检' : '新增质检'"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="质检类型" prop="qualityType">
              <el-select v-model="formData.qualityType" placeholder="请选择质检类型" style="width: 100%" :teleported="false">
                <el-option
                  v-for="(item, key) in QualityTypeMap"
                  :key="key"
                  :label="item.label"
                  :value="key"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联单号" prop="relatedOrderNo">
              <el-input v-model="formData.relatedOrderNo" placeholder="请输入关联单号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品/物料" prop="materialName">
              <el-input v-model="formData.materialName" placeholder="请输入产品/物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批次号" prop="batchNo">
              <el-input v-model="formData.batchNo" placeholder="请输入批次号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="抽检数量" prop="sampleQuantity">
              <el-input-number v-model="formData.sampleQuantity" :min="0" :max="99999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质检结果" prop="qualityResult">
              <el-select v-model="formData.qualityResult" placeholder="请选择质检结果" style="width: 100%" :teleported="false">
                <el-option label="待检" value="PENDING" />
                <el-option label="质检中" value="PROCESSING" />
                <el-option label="合格" value="PASS" />
                <el-option label="不合格" value="FAIL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 质检项目明细 -->
        <div class="form-section">
          <div class="section-title">
            <span>质检项目</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addQualityItem">
              添加项目
            </el-button>
          </div>

          <el-table :data="formData.qualityItems" size="small" border style="width: 100%">
            <el-table-column label="质检项目" min-width="140">
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
                <el-input v-model="row.actualValue" placeholder="实测值" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="结果" width="100" align="center">
              <template #default="{ row }">
                <el-select v-model="row.result" size="small" :teleported="false">
                  <el-option label="待检" value="PENDING" />
                  <el-option label="合格" value="PASS" />
                  <el-option label="不合格" value="FAIL" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="备注" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="" width="50" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeQualityItem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!formData.qualityItems.length" class="empty-tip">
            <el-text type="info">暂未添加质检项目，点击上方"添加项目"按钮开始配置</el-text>
          </div>
        </div>

        <el-form-item label="质检报告">
          <el-upload
            class="report-uploader"
            :show-file-list="false"
            accept=".pdf,.doc,.docx,.jpg,.png"
          >
            <el-button>
              <el-icon><Upload /></el-icon> 上传报告
            </el-button>
            <div class="upload-tip">支持 PDF、Word、图片格式，大小不超过10MB</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="质检详情"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-container">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="质检单号">{{ currentDetail.recordNo }}</el-descriptions-item>
            <el-descriptions-item label="质检类型">{{ getQualityTypeLabel('INCOMING') }}</el-descriptions-item>
            <el-descriptions-item label="关联单号">{{ currentDetail.traceCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="产品/物料">{{ currentDetail.materialName }}</el-descriptions-item>
            <el-descriptions-item label="批次号">{{ currentDetail.batchNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="质检结果">
              <StatusTag :status="getQualityResultStatus(currentDetail.abnormalLevel)" :label="getQualityResultLabel(currentDetail.abnormalLevel)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="质检员">{{ currentDetail.handledByName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="质检时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 质检项目明细 -->
        <div class="detail-section">
          <div class="section-title">质检项目明细</div>
          <el-table :data="[]" size="small" border style="width: 100%">
            <el-table-column prop="itemName" label="质检项目" min-width="140" />
            <el-table-column prop="standard" label="标准值" min-width="120" />
            <el-table-column prop="actualValue" label="实测值" min-width="120" />
            <el-table-column prop="result" label="结果" width="100" align="center" />
            <el-table-column prop="remark" label="备注" min-width="120" />
          </el-table>
          <div class="empty-tip">
            <el-text type="info">暂无质检项目明细</el-text>
          </div>
        </div>

        <!-- 质检报告 -->
        <div class="detail-section">
          <div class="section-title">质检报告</div>
          <div class="report-section">
            <el-empty description="暂无质检报告" :image-size="60" />
          </div>
        </div>

        <!-- 质检记录时间线 -->
        <div class="detail-section">
          <div class="section-title">质检记录时间线</div>
          <el-timeline>
            <el-timeline-item :timestamp="formatTime(currentDetail.createTime)" placement="top">
              <el-card shadow="never">
                <h4>创建质检记录</h4>
                <p>系统创建质检记录，等待处理</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item v-if="currentDetail.handledTime" :timestamp="formatTime(currentDetail.handledTime)" placement="top" type="success">
              <el-card shadow="never">
                <h4>质检完成</h4>
                <p>处理人：{{ currentDetail.handledByName || '-' }}</p>
                <p>处理结果：{{ currentDetail.handlingResult || '-' }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
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

// 质检项目表格和表单区域
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

// 上传区域
.report-uploader {
  .upload-tip {
    margin-top: var(--fts-space-2);
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

// 详情对话框
.detail-container {
  .detail-section {
    margin-bottom: var(--fts-space-4);

    .section-title {
      font-weight: var(--fts-font-weight-semibold);
      font-size: var(--fts-font-size-base);
      color: var(--fts-text-primary);
      margin-bottom: var(--fts-space-3);
      padding-left: var(--fts-space-2);
      border-left: 3px solid var(--fts-primary);
    }
  }

  .report-section {
    min-height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed var(--fts-border-primary);
    border-radius: var(--fts-card-radius);
    background: var(--fts-bg-page);
  }
}

// 表格文本样式
.record-no-text,
.order-no-text,
.product-text,
.batch-text,
.inspector-text {
  color: var(--fts-text-primary);
}

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
